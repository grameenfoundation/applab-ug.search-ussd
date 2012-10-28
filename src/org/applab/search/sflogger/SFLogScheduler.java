package org.applab.search.sflogger;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.xml.rpc.ServiceException;

import org.applab.search.soap.DatabaseHandler;
import org.applab.search.soap.UssdWebServiceImpl;

import applab.server.SalesforceProxy;

import com.sforce.soap.enterprise.LoginResult;
import com.sforce.soap.enterprise.SessionHeader;
import com.sforce.soap.enterprise.SforceServiceLocator;
import com.sforce.soap.enterprise.SoapBindingStub;
import com.sforce.soap.schemas._class.CreateSearchLogEntry.CreateSearchLogEntryBindingStub;
import com.sforce.soap.schemas._class.CreateSearchLogEntry.CreateSearchLogEntryServiceLocator;
import com.sforce.soap.schemas._class.CreateSearchLogEntry.SearchLogEntry;

public class SFLogScheduler {

	private static Connection connection = null;
	private final static Logger logger = Logger
			.getLogger(UssdWebServiceImpl.class.getName());

	public static void main(String[] args) throws ServiceException, IOException {
		try {
			logToSalesforce();
			logger.info("Running USSD Searches to Salesforce Logger..");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Build queries and db connection and returns prepared statements
	 * 
	 * @param commandText
	 * @return
	 * @throws ClassNotFoundException
	 *             , SQLException
	 * 
	 */

	static PreparedStatement buildSelectQuery(StringBuilder commandText)
			throws ClassNotFoundException, SQLException {
		PreparedStatement selectStatement = null;
		try {

			Configuration.getConfig().parseConfig();
			connection = DatabaseHelpers.createConnection(
					Configuration.getConfig().getConfiguration("databaseURL",
							""),
					Configuration.getConfig().getConfiguration(
							"databaseUsername", ""), Configuration.getConfig()
							.getConfiguration("databasePassword", ""));

			System.out.println("Database connection established");

			// prepare select statement
			selectStatement = connection.prepareStatement(commandText
					.toString());
		} catch (Exception e) {

			System.err.println("Cannot connect to database server");
			e.printStackTrace();
		}
		return selectStatement;
	}

	@SuppressWarnings("unused")
	public static void logToSalesforce() throws Exception {
		
		CreateSearchLogEntryServiceLocator serviceLocator = new CreateSearchLogEntryServiceLocator();
		CreateSearchLogEntryBindingStub serviceStub = (CreateSearchLogEntryBindingStub) serviceLocator
				.getCreateSearchLogEntry();

		// Use soap api to login and get session info
		SforceServiceLocator soapServiceLocator = new SforceServiceLocator();
		soapServiceLocator.setSoapEndpointAddress(Configuration.getConfig()
				.getConfiguration("salesforceAddress", ""));

		SoapBindingStub binding = (SoapBindingStub) soapServiceLocator
				.getSoap();
		LoginResult loginResult = binding.login(
				Configuration.getConfig().getConfiguration(
						"salesforceUsername", ""),
				Configuration.getConfig().getConfiguration(
						"salesforcePassword", "")
						+ Configuration.getConfig().getConfiguration(
								"salesforceToken", ""));
		SessionHeader sessionHeader = new SessionHeader(
				loginResult.getSessionId());
		logger.info("Share the session info with our webservice");

		// Share the session info with our webservice
		serviceStub.setHeader(
				"http://soap.sforce.com/schemas/class/CreateSearchLogEntry",
				"SessionHeader", sessionHeader);

		Set<String> s = new HashSet<String>();
		Iterator<String> iter = s.iterator();
		String transactionId;
		SearchLogEntry searchLogEntry = new SearchLogEntry();
		int transactionIdCount = 0;

		// Prepare select statement to pick transaction_ids from table
		StringBuilder CommandText = new StringBuilder();
		CommandText.append("SELECT transaction_id");
		CommandText.append(", msisdn, bread_crumb");
		CommandText.append(", category_id, menu_content");
		CommandText.append(", created_date, iscompleted");
		CommandText.append(" FROM ussd");
		CommandText
				.append(" WHERE DATE_ADD(created_date, INTERVAL 120 second) < NOW()");
		CommandText.append(" AND sf_logged = ?");
		CommandText.append(" ORDER BY created_date DESC");

		PreparedStatement selectStatement = buildSelectQuery(CommandText);
		selectStatement.setInt(1, 0);
		ResultSet result = selectStatement.executeQuery();
		List<SearchLogEntry> searchLogEntriesList = new ArrayList<SearchLogEntry>();
		Map<String, SearchLogEntry> transactionIdSearchLogEntryMap = new HashMap<String, SearchLogEntry>();

		SearchLogEntry searchLogEntries[] = null;

		boolean sfLogged = false;

		if (result.next()) {

			// Guarantees the topmost pick which is the most recent entry for
			// any transaction
			result.first();
			while (result.next()) {

				transactionId = result.getString(1);

				while (!s.contains(transactionId)) {
					transactionIdCount++;

					// Prepare to Log to SF
					String msisdn = result.getString(2);
					msisdn = msisdn.replace("256", "0");
					String content = null;
					content = DatabaseHandler.getContent(result.getString(3),
							result.getInt(4));
					String category = null;
					category = DatabaseHandler.getCategoryNameFromId(result
							.getInt(4));
					String submissionTime = null;
					submissionTime = result.getString(6);
					String keyword = null;
					keyword = result.getString(3);

					// Save record to SF
					searchLogEntry.setMsisdn(msisdn);
					searchLogEntry.setCategory(category);
					searchLogEntry.setContent(content);
					searchLogEntry.setSubmissionTime(submissionTime);
					Calendar calendar = Calendar.getInstance();
					String serverEntryTime = SalesforceProxy
							.formatDateTime(calendar.getTime());
					searchLogEntry.setServerEntryTime(serverEntryTime);
					searchLogEntry.setQuery(keyword);
					searchLogEntry.setIsUssd(true);

					// If process was completed set iscompleted to true if not
					// then false
					if (result.getInt(7) == 1) {
						searchLogEntry.setIsCompleted(true);
					} else {
						searchLogEntry.setIsCompleted(false);
					}
					try {

						// Add searchLogEntry to List;
						searchLogEntriesList.add(searchLogEntry);

						// Add transactionId to set
						s.add(transactionId);

						transactionIdSearchLogEntryMap.put(transactionId,
								searchLogEntry);

						// Bundle 20 Entries per SF call
						if (searchLogEntriesList.size() >= 20) {
							searchLogEntries = new SearchLogEntry[searchLogEntriesList
									.size()];
							searchLogEntriesList.toArray(searchLogEntries);

							// Make SF entry
							logger.info("Making a batch entry to SF..");
							sfLogged = serviceStub
									.createNewSearchLogEntries(searchLogEntries);

							// Mark all successfully logged entries as logged
							if (sfLogged == true) {
								StringBuilder updateCommandText = new StringBuilder();
								updateCommandText.append("UPDATE ussd ");
								updateCommandText.append("SET sf_logged = ?");
								updateCommandText
										.append("WHERE transaction_id = ?");

								PreparedStatement updateStatement = buildSelectQuery(updateCommandText);
								updateStatement.setInt(1, 1);

								for (String transactionIdToUpdate : transactionIdSearchLogEntryMap
										.keySet()) {
									updateStatement.setString(2,
											transactionIdToUpdate);
									updateStatement.executeUpdate();
								}
							}
							searchLogEntriesList.clear();
							transactionIdSearchLogEntryMap.clear();
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
			logger.info(transactionIdCount + " Distict transaction ids logged");
			connection.close();
		}
		connection.close();
	}
}