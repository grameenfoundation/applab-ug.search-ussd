/*
 * Copyright (C) 2011 Grameen Foundation
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package applab.search.soap;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import applab.server.ApplabConfiguration;
import applab.server.DatabaseHelpers;
import applab.server.SalesforceProxy;
import applab.server.WebAppId;

import com.sforce.soap.enterprise.LoginResult;
import com.sforce.soap.enterprise.SessionHeader;
import com.sforce.soap.enterprise.SforceServiceLocator;
import com.sforce.soap.enterprise.SoapBindingStub;
import com.sforce.soap.schemas._class.CreateSearchLogEntry.CreateSearchLogEntryBindingStub;
import com.sforce.soap.schemas._class.CreateSearchLogEntry.CreateSearchLogEntryServiceLocator;
import com.sforce.soap.schemas._class.CreateSearchLogEntry.SearchLogEntry;

/**
 * 
 * Handles all database access logic and logging searches to Salesforce
 * 
 */

public class DatabaseHandler {

	private static Connection connection = null;
	private final static Logger logger = Logger.getLogger(DatabaseHandler.class
			.getName());

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
			connection = DatabaseHelpers
					.createReaderConnection(WebAppId.AppLabWebService);
			System.out.println("Database connection established");

			// prepare select statement
			selectStatement = connection.prepareStatement(commandText
					.toString());
		} catch (Exception e) {

			System.err.println("Cannot connect to database server");
			logger.warning(e.getMessage());
			e.printStackTrace();
		}
		return selectStatement;
	}

	private static PreparedStatement buildNonSelectQuery(
			StringBuilder commandText) throws ClassNotFoundException,
			SQLException {
		PreparedStatement insertStatement = null;
		try {
			connection = DatabaseHelpers
					.createConnection(WebAppId.AppLabWebService);
			System.out.println("Database connection established");

			// prepare non-select statement
			insertStatement = connection.prepareStatement(commandText
					.toString());
		} catch (Exception e) {
			System.err.println("Cannot connect to database server");
			logger.warning(e.getMessage());
			e.printStackTrace();
		}
		return insertStatement;
	}

	// Use the previously displayed menu to get the breadCrumb, categoryId, and
	// last page for the previous transaction
	public static UssdMenu getDisplayedMenu(UssdRequest request)
			throws Exception {
		StringBuilder commandText = new StringBuilder();
		commandText.append("SELECT bread_crumb");
		commandText.append(", category_id, menu_content ");
		commandText.append(", page ");
		commandText.append(" FROM ussd ");
		commandText.append(" WHERE transaction_id = ?");
		commandText.append(" ORDER BY id");
		commandText.append(" DESC LIMIT 1");

		PreparedStatement selectStatement = buildSelectQuery(commandText);

		// Pass the variables to the prepared statement
		selectStatement.setString(1, request.transactionId);

		ResultSet result = selectStatement.executeQuery();

		UssdMenu menu = new UssdMenu();
		try {
			while (result.next()) {

				// Populate Menu
				menu.setBreadCrumb(result.getString(1));
				menu.setCategoryId(result.getInt(2));
				menu.unserialize(result.getString(3));
				menu.setPage(result.getInt(4));
			}
		} catch (Exception e) {
			logger.warning(e.getMessage());
			e.printStackTrace();
		}
		connection.close();
		return menu;
	}

	// Get requested content using the previous breadCrumb and the categoryId as
	// per the previous request
	public static UssdMenu getRequestedMenu(String breadCrumb,
			Integer categoryId) throws Exception {

		// Build select command to obtain content from table
		StringBuilder commandText = new StringBuilder();
		commandText.append("SELECT DISTINCT(SUBSTRING_INDEX(");
		commandText.append(" TRIM(REPLACE(");
		commandText.append(" keyword, ?");
		commandText.append(",' ')), ' ', 1))");
		commandText.append(" AS menuItem ");
		commandText.append(" FROM keyword ");
		commandText.append(" WHERE");
		commandText.append(" keyword LIKE ?");
		commandText.append(" AND categoryId = ? ");
		commandText.append(" AND isdeleted = ? ");
		commandText.append(" ORDER BY keyword ASC");

		PreparedStatement selectStatement = buildSelectQuery(commandText);

		// Pass the variables to the prepared statement
		selectStatement.setString(1, breadCrumb);
		selectStatement.setString(2, breadCrumb + "%");
		selectStatement.setInt(3, categoryId);
		selectStatement.setInt(4, 0);

		ResultSet resource = selectStatement.executeQuery();
		UssdMenu menu = new UssdMenu();
		try {
			while (resource.next()) {
				menu.addItem(resource.getString(1).replace("_", " "));
			}
		} catch (Exception e) {
			logger.warning(e.getMessage());
			e.printStackTrace();
		}
		menu.setCategoryId(categoryId);
		menu.setBreadCrumb(breadCrumb);
		connection.close();
		return menu;
	}

	public static String getContent(String breadCrumb, Integer categoryId)
			throws Exception {
		String content = "";

		// Build select command to obtain content from table
		StringBuilder commandText = new StringBuilder();
		commandText.append("SELECT content");
		commandText.append(" FROM keyword ");
		commandText.append(" WHERE categoryId = ? ");
		commandText.append(" AND isdeleted = ? ");
		commandText.append(" AND keyword = ? ");
		commandText.append(" ORDER BY content LIMIT 1");

		PreparedStatement selectStatement = buildSelectQuery(commandText);

		// Pass the variables to the prepared statement
		selectStatement.setInt(1, categoryId);
		selectStatement.setInt(2, 0);
		selectStatement.setString(3, breadCrumb);

		ResultSet result = selectStatement.executeQuery();
		try {
			while (result.next()) {
				content = result.getString(1);
			}
		} catch (Exception e) {
			logger.warning(e.getMessage());
			e.printStackTrace();
		}
		connection.close();
		return content;
	}

	// Get content for root menu
	public static UssdMenu createRootMenu() throws Exception {
		StringBuilder commandText = new StringBuilder();
		commandText.append("SELECT DISTINCT(keyword.categoryId)");
		commandText.append(", category.name AS name");
		commandText.append(" FROM category ");
		commandText
				.append(" INNER JOIN keyword ON keyword.categoryId=category.id ");
		commandText.append(" WHERE category.ckwsearch= ? ");
		commandText.append(" AND category.isdeleted = ? ");
		commandText.append(" AND category.isussd = ? ");
		commandText.append(" ORDER BY category.name ASC");

		PreparedStatement selectStatement = buildSelectQuery(commandText);

		// Pass the variables to the prepared statement
		selectStatement.setInt(1, 1);
		selectStatement.setInt(2, 0);
		selectStatement.setInt(3, 1);

		ResultSet rootMenuResource = selectStatement.executeQuery();
		UssdMenu rootMenu = new UssdMenu();
		try {

			// Populate the root menu
			while (rootMenuResource.next()) {
				if (rootMenuResource.getInt(1) != 77) {
					rootMenu.addItem(rootMenuResource.getString(2));
				}
			}
		} catch (Exception e) {
			logger.warning(e.getMessage());
			e.printStackTrace();
		}
		connection.close();
		return rootMenu;
	}

	public static UssdMenu createOtherRootMenu(Integer categoryId)
			throws Exception {
		StringBuilder commandText = new StringBuilder();
		commandText.append("SELECT keyword AS name");
		commandText.append(" FROM keyword ");
		commandText
				.append(" INNER JOIN category ON keyword.categoryId=category.id ");
		commandText.append(" WHERE category.isdeleted = ? ");
		commandText.append(" AND category.isussd = ? ");
		commandText.append(" AND category.id = ? ");
		commandText.append(" ORDER BY keyword ASC");

		PreparedStatement selectStatement = buildSelectQuery(commandText);

		// Pass the variables to the prepared statement
		selectStatement.setInt(1, 0);
		selectStatement.setInt(2, 1);
		selectStatement.setInt(3, categoryId);

		ResultSet rootMenuResource = selectStatement.executeQuery();
		UssdMenu rootMenu = new UssdMenu();

		Set<String> checkUniqueSet = new HashSet<String>();

		try {
			while (rootMenuResource.next()) {
				String[] keywordParts = rootMenuResource.getString(1)
						.split(" ");
				String keyword = keywordParts[0].trim().replace("_", " ");
				
				//Make sure we get a unique entry to the menu
				if (checkUniqueSet.add(keyword) != false) {
					rootMenu.addItem(keyword);
				}
			}
		} catch (Exception e) {
			logger.warning(e.getMessage());
			e.printStackTrace();
		}
		rootMenu.setCategoryId(categoryId);
		connection.close();
		return rootMenu;
	}

	// Use category selected by user to get categoryId
	public static Integer getCategoryIdFromName(String userInput)
			throws Exception {
		int categoryId = 0;

		// Prepare select statement to pick categoryid for category from table
		StringBuilder commandText = new StringBuilder();
		commandText.append("SELECT id");
		commandText.append(" FROM category");
		commandText.append(" WHERE name = ? ");
		commandText.append(" AND category.isdeleted = ? ");
		commandText.append(" AND category.isussd = ? ");
		commandText.append(" LIMIT 1");

		PreparedStatement selectStatement = buildSelectQuery(commandText);

		// Pass the variables to the prepared statement
		selectStatement.setString(1, userInput);
		selectStatement.setInt(2, 0);
		selectStatement.setInt(3, 1);

		ResultSet result = selectStatement.executeQuery();
		try {

			// Pick categoryId
			while (result.next()) {
				categoryId = result.getInt(1);
			}
		} catch (Exception e) {
			logger.warning(e.getMessage());
			e.printStackTrace();
		}
		connection.close();
		return categoryId;
	}

	public static void logRequest(UssdRequest request, UssdMenu currentMenu,
			int complete) throws Exception {

		StringBuilder commandText = new StringBuilder();
		commandText.append("INSERT INTO");
		commandText.append(" ussd (");
		commandText.append(" transaction_id, menu_content");
		commandText.append(", page, category_id");
		commandText.append(", bread_crumb, msisdn");
		commandText.append(", iscompleted, sf_logged, created_date");
		commandText.append(") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");

		PreparedStatement insertStatement = buildNonSelectQuery(commandText);

		// Add query parameters
		insertStatement.setString(1, request.transactionId);
		insertStatement.setString(
				2,
				currentMenu.getMenuString("##", false,
						currentMenu.getItemCount() + 1, 1));
		insertStatement.setInt(3, currentMenu.getPage());
		insertStatement.setInt(4, ((null == currentMenu.getCategoryId()) ? 0
				: currentMenu.getCategoryId()));
		insertStatement.setString(
				5,
				((null == currentMenu.getBreadCrumb()) ? "" : currentMenu
						.getBreadCrumb()));
		insertStatement.setString(6, request.msisdn);
		insertStatement.setInt(7, complete);
		insertStatement.setInt(8, 0);
		Date date = new Date();
		Timestamp sqlDatetime = new Timestamp(date.getTime());
		insertStatement.setTimestamp(9, sqlDatetime);

		try {
			insertStatement.executeUpdate();
		} catch (Exception e) {
			e.printStackTrace();
		}
		complete = 0;
		connection.close();

		// Prepare select statement to pick transaction_ids from table
		StringBuilder tIdCommandText = new StringBuilder();
		tIdCommandText.append("SELECT DISTINCT(transaction_id)");
		tIdCommandText.append(" FROM ussd ");
		tIdCommandText.append(" WHERE sf_logged = ? ");
		tIdCommandText.append(" AND NOT category_id = ? ");
		tIdCommandText
				.append(" AND DATE_ADD(created_date, INTERVAL 120 second) < NOW()");

		PreparedStatement tIdSelectStatement = buildSelectQuery(tIdCommandText);
		tIdSelectStatement.setInt(1, 0);

		// Don't log to SF if Budget Services
		tIdSelectStatement.setInt(2, 77);

		ResultSet transactionIdResult = tIdSelectStatement.executeQuery();
		if (transactionIdResult.next()) {
			connection.close();
			// logToSalesforce();
		}
	}

	public static void logToSalesforce() throws Exception {
		CreateSearchLogEntryServiceLocator serviceLocator = new CreateSearchLogEntryServiceLocator();
		CreateSearchLogEntryBindingStub serviceStub = (CreateSearchLogEntryBindingStub) serviceLocator
				.getCreateSearchLogEntry();

		// Use soap api to login and get session info2
		SforceServiceLocator soapServiceLocator = new SforceServiceLocator();
		soapServiceLocator.setSoapEndpointAddress((String) ApplabConfiguration
				.getConfigParameter(WebAppId.global, "salesforceAddress", ""));
		SoapBindingStub binding = (SoapBindingStub) soapServiceLocator
				.getSoap();
		LoginResult loginResult = binding.login(
				(String) ApplabConfiguration.getConfigParameter(
						WebAppId.global, "salesforceUsername", ""),
				(String) ApplabConfiguration.getConfigParameter(
						WebAppId.global, "salesforcePassword", "")
						+ (String) ApplabConfiguration.getConfigParameter(
								WebAppId.global, "salesforceToken", ""));
		SessionHeader sessionHeader = new SessionHeader(
				loginResult.getSessionId());

		// Share the session info with our webservice
		serviceStub.setHeader(
				"http://soap.sforce.com/schemas/class/CreateSearchLogEntry",
				"SessionHeader", sessionHeader);

		Set<String> s = new HashSet<String>();
		String transactionId;
		SearchLogEntry searchLogEntry = new SearchLogEntry();

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

		if (result.next()) {

			// Guarantees the topmost pick which is the most recent entry for
			// any transaction
			result.first();
			while (result.next()) {
				transactionId = result.getString(1);

				if (!s.contains(transactionId)) {

					// Prepare to Log to SF
					String msisdn = result.getString(2);
					msisdn = msisdn.replace("256", "0");
					String content = null;
					content = getContent(result.getString(3), result.getInt(4));
					String category = null;
					category = getCategoryNameFromId(result.getInt(4));
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

						// Make SF entry
						SearchLogEntry resultSearchLogEntry = serviceStub
								.createNewSearchLogEntry(searchLogEntry);

						// Add transactionId to set
						s.add(transactionId);
					} catch (Exception e) {
						logger.warning(e.getMessage());
						e.printStackTrace();
					}
				}
			}

			connection.close();

			// Mark all logged entries as logged
			Iterator<String> iter = s.iterator();
			StringBuilder updateCommandText = new StringBuilder();
			updateCommandText.append("UPDATE ussd ");
			updateCommandText.append("SET sf_logged = ? ");
			updateCommandText.append("WHERE transaction_id = ?");

			while (iter.hasNext()) {
				PreparedStatement updateStatement = buildSelectQuery(updateCommandText);
				updateStatement.setInt(1, 1);
				transactionId = iter.next();
				updateStatement.setString(2, transactionId);
				updateStatement.executeUpdate();
			}
		}
		connection.close();
	}

	// Use Category Id to obtain category name
	public static String getCategoryNameFromId(Integer categoryId)
			throws Exception {

		String category = "";
		StringBuilder commandText = new StringBuilder();

		commandText.append("SELECT name");
		commandText.append(" FROM category");
		commandText.append(" WHERE id = ?");

		PreparedStatement selectStatement = buildSelectQuery(commandText);

		// Pass the variables to the prepared statement
		selectStatement.setInt(1, categoryId);

		ResultSet result = selectStatement.executeQuery();
		try {
			while (result.next()) {
				category = result.getString(1);
			}
		} catch (Exception e) {
			logger.warning(e.getMessage());
			e.printStackTrace();
		}
		connection.close();
		return category;
	}
}