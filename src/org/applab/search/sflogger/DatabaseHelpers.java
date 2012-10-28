package org.applab.search.sflogger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Class to deal with database interactions
 * 
 * Copyright (C) 2012 Grameen Foundation
 */
public class DatabaseHelpers {

	final static String JDBC_DRIVER = "com.mysql.jdbc.Driver";
	private static Connection connection;

	/**
	 * Constructor will create a connection to the DB. This will need to be
	 * improved to use connection pooling and non-persistent connections
	 * 
	 * @param databaseName
	 *            - The name of the database being used
	 * @param username
	 *            - The username for the database
	 * @param password
	 *            - The password for the database
	 * @return 
	 */
	public static Connection createConnection(String url, String username,
			String password) throws ClassNotFoundException, SQLException {

		// Make sure the JDBC driver is loaded into memory
		Class.forName(JDBC_DRIVER);
		try {
			connection = DriverManager.getConnection(url, username, password);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return connection;
	}

	public static void closeConnection() throws SQLException {
		connection.close();
	}

	public static Boolean checkConnection() throws SQLException {
		return connection.isClosed();
	}

	public static PreparedStatement getPreparedStatement(String query)
			throws SQLException {
		return connection.prepareStatement(query);
	}

	/**
	 * Execute a select statement.
	 * 
	 * @param query
	 *            - The query string for the select statement
	 * 
	 * @return - The result set
	 */
	public static ResultSet executeSelectQuery(PreparedStatement statement)
			throws SQLException {
		statement.executeQuery();
		return statement.getResultSet();
	}

	/**
	 * Executes a select query that returns a ResultSet
	 * 
	 * @param query
	 * @return
	 * @throws SQLException
	 */
	public static ResultSet executeSelectQuery(String query)
			throws SQLException {
		Statement selectStatement = connection.createStatement();
		selectStatement.executeQuery(query);
		return selectStatement.getResultSet();
	}

	/**
	 * Get the number of rows that are in the result set. Returns the result set
	 * with the cursor on the first row.
	 * 
	 * @param rs
	 *            - The result set
	 * 
	 * @return - The number of rows in the result set
	 */
	public static int getNumberOfRows(ResultSet resultSet) {

		int totalRow = 0;
		try {
			boolean hasRows = resultSet.last();

			// Do we have any rows in the result set
			if (hasRows) {
				totalRow = resultSet.getRow();
				resultSet.first();
			}
		} catch (Exception e) {
			return totalRow;
		}
		return totalRow;
	}

}