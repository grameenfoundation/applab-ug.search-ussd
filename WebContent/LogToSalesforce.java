package applab.search.soap;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import applab.server.ApplabConfiguration;
import applab.server.SalesforceProxy;
import applab.server.WebAppId;

import com.sforce.soap.enterprise.LoginResult;
import com.sforce.soap.enterprise.SessionHeader;
import com.sforce.soap.enterprise.SforceServiceLocator;
import com.sforce.soap.enterprise.SoapBindingStub;
import com.sforce.soap.schemas._class.CreateSearchLogEntry.CreateSearchLogEntryBindingStub;
import com.sforce.soap.schemas._class.CreateSearchLogEntry.CreateSearchLogEntryServiceLocator;
import com.sforce.soap.schemas._class.CreateSearchLogEntry.SearchLogEntry;

public class LogToSalesforce {

    public static void main(String [] args) {
        // public static void prep2Log() {
        // Prepare select statement to pick transaction_ids from table
        StringBuilder tIdCommandText = new StringBuilder();
        tIdCommandText.append("SELECT DISTINCT(transaction_id)");
        tIdCommandText.append(" FROM ussd ");
        tIdCommandText.append(" WHERE sf_logged = ? ");
        tIdCommandText.append(" AND DATE_ADD(created_date, INTERVAL 120 second) < NOW()");

        try {
            prepToLog(tIdCommandText);
        }
        catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    public static void prepToLog(StringBuilder tIdCommandText) throws Exception {

        PreparedStatement tIdSelectStatement = DatabaseHandler.buildSelectQuery(tIdCommandText);
        tIdSelectStatement.setInt(1, 0);

        ResultSet transactionIdResult = tIdSelectStatement.executeQuery();
        if (transactionIdResult.next()) {
            
            logToSalesforce();
        }

    }

    public static void logToSalesforce() throws Exception {
        CreateSearchLogEntryServiceLocator serviceLocator = new CreateSearchLogEntryServiceLocator();
        CreateSearchLogEntryBindingStub serviceStub = (CreateSearchLogEntryBindingStub)serviceLocator.getCreateSearchLogEntry();

        // Use soap api to login and get session info2
        SforceServiceLocator soapServiceLocator = new SforceServiceLocator();
        soapServiceLocator.setSoapEndpointAddress((String)ApplabConfiguration.getConfigParameter(WebAppId.global, "salesforceAddress", ""));
        SoapBindingStub binding = (SoapBindingStub)soapServiceLocator.getSoap();
        LoginResult loginResult = binding.login((String)ApplabConfiguration.getConfigParameter(WebAppId.global, "salesforceUsername",
                ""), (String)ApplabConfiguration.getConfigParameter(WebAppId.global, "salesforcePassword", "") +
                (String)ApplabConfiguration.getConfigParameter(WebAppId.global, "salesforceToken", ""));
        SessionHeader sessionHeader = new SessionHeader(loginResult.getSessionId());

        // Share the session info with our webservice
        serviceStub.setHeader("http://soap.sforce.com/schemas/class/CreateSearchLogEntry", "SessionHeader", sessionHeader);

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
        CommandText.append(" WHERE DATE_ADD(created_date, INTERVAL 120 second) < NOW()");
        CommandText.append(" AND sf_logged = ?");
        CommandText.append(" ORDER BY created_date DESC");

        PreparedStatement selectStatement = DatabaseHandler.buildSelectQuery(CommandText);
        selectStatement.setInt(1, 0);

        ResultSet result = selectStatement.executeQuery();

        if (result.next()) {

            // Guarantees the topmost pick which is the most recent entry for any transaction
            result.first();
            while (result.next()) {
                transactionId = result.getString(1);

                if (!s.contains(transactionId)) {

                    // Prepare to Log to SF
                    String msisdn = result.getString(2);
                    msisdn = msisdn.replace("256", "0");
                    String content = null;
                    content = DatabaseHandler.getContent(result.getString(3), result.getInt(4));
                    String category = null;
                    category = DatabaseHandler.getCategoryNameFromId(result.getInt(4));
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
                    String serverEntryTime = SalesforceProxy.formatDateTime(calendar.getTime());
                    searchLogEntry.setServerEntryTime(serverEntryTime);
                    searchLogEntry.setQuery(keyword);
                    searchLogEntry.setIsUssd(true);

                    // If process was completed set iscompleted to true if not then false
                    if (result.getInt(7) == 1) {
                        searchLogEntry.setIsCompleted(true);
                    }
                    else if (result.getInt(7) == 0) {
                        searchLogEntry.setIsCompleted(false);
                    }
                    try {

                        // Make SF entry
                        System.out.print("logging");
                        SearchLogEntry resultSearchLogEntry = serviceStub.createNewSearchLogEntry(searchLogEntry);

                        // Add transactionId to set
                        s.add(transactionId);
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            // Mark all logged entries as logged
            Iterator<String> iter = s.iterator();
            StringBuilder updateCommandText = new StringBuilder();
            updateCommandText.append("UPDATE ussd ");
            updateCommandText.append("SET sf_logged = ? ");
            updateCommandText.append("WHERE transaction_id = ?");

            while (iter.hasNext()) {
                PreparedStatement updateStatement = DatabaseHandler.buildSelectQuery(updateCommandText);
                updateStatement.setInt(1, 1);
                transactionId = iter.next();
                updateStatement.setString(2, transactionId);
                updateStatement.executeUpdate();
            }
        }
    }

}