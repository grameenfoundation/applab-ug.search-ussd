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
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

/**
 * 
 * Handles all database access logic
 * 
 */
public class DatabaseHandler {

    private static final String USSD_LOG_TABLE_NAME = "ussd";
    Connection conn = null;

    /**
     * Constructor - Creates connection to MySQL DB
     * 
     * @param hostName
     * @param databaseName
     * @param username
     * @param password
     */
    public DatabaseHandler(String hostName, String databaseName,
            String username, String password) {
        try {
            String url = "jdbc:mysql://" + hostName + ":3306/" + databaseName;
            Class.forName("com.mysql.jdbc.Driver").newInstance();
            conn = DriverManager.getConnection(url, username, password);
            System.out.println("Database connection established");
        }
        catch (Exception e) {
            System.err.println("Cannot connect to database server");
        }
    }

    /**
     * Executes a select query that returns a ResultSet
     * 
     * @param query
     * @return
     * @throws SQLException
     */
    public ResultSet executeSelectQuery(String query) throws SQLException {
        Statement selectStatement = conn.createStatement();
        selectStatement.executeQuery(query);
        return selectStatement.getResultSet();
    }

    /**
     * Executes a non select query such as insert, delete, update
     * 
     * @param query
     * @return
     * @throws SQLException
     */
    public void executeNonSelectQuery(String query) throws SQLException {
        conn.createStatement().executeUpdate(query);
    }

    // Use the previously displayed menu to get the breadCrumb, categoryId, and last page for the previous transaction
    public UssdMenu getDisplayedMenu(UssdRequest request) throws SQLException {
        String query = "SELECT bread_crumb, category_id, menu_content, page FROM " + USSD_LOG_TABLE_NAME + " WHERE transaction_id = "
                + request.transactionId + " ORDER BY id DESC LIMIT 1";
        ResultSet resultSet = this.executeSelectQuery(query);
        resultSet.first();
        UssdMenu menu = new UssdMenu();
        menu.unserialize(resultSet.getString("menu_content"));
        menu.setBreadCrumb(resultSet.getString("bread_crumb"));
        menu.setCategoryId(resultSet.getInt("category_id"));
        menu.setPage(resultSet.getInt("page"));
        return menu;
    }

    // Get requested content using the previous breadCrumb and the categoryId as per the previous request
    public UssdMenu getRequestedMenu(String breadCrumb, Integer categoryId) throws SQLException {
        // Build the query string
        String query = "SELECT DISTINCT(SUBSTRING_INDEX(TRIM(REPLACE(keyword, '" + breadCrumb + "', '')), ' ', 1)) " +
                " AS menuItem FROM keyword WHERE keyword LIKE '" + breadCrumb + "%' AND categoryId = "
                + categoryId + " AND isDeleted = 0 ORDER BY keyword ASC";
        ResultSet resource = this.executeSelectQuery(query);
        UssdMenu menu = new UssdMenu();

        while (resource.next()) {
            menu.addItem(resource.getString("menuItem").replace("_", " "));
        }

        menu.setCategoryId(categoryId);
        menu.setBreadCrumb(breadCrumb);
        return menu;
    }

    public String getContent(UssdRequest request, String breadCrumb, Integer categoryId) throws SQLException {
        String query = "SELECT content FROM keyword WHERE categoryId='"
                + categoryId
                + "' AND isdeleted='0' AND keyword = '"
                + breadCrumb
                + "' ORDER BY content";
        ResultSet result = this.executeSelectQuery(query);
        result.first();
        return result.getString("content");
    }

    // Get content for root menu
    public UssdMenu createRootMenu() throws SQLException {
        ResultSet rootMenuResource = this
                .executeSelectQuery("SELECT DISTINCT(keyword.categoryId), category.name AS name FROM category INNER JOIN keyword ON keyword.categoryId=category.id WHERE category.ckwsearch='1' AND category.isdeleted='0' ORDER BY category.name ASC");

        UssdMenu rootMenu = new UssdMenu();

        while (rootMenuResource.next()) {
            rootMenu.addItem(rootMenuResource.getString("name"));
        }
        return rootMenu;
    }

    public Integer getCategoryIdFromName(String userInput) throws SQLException {
        String query = "SELECT id FROM category WHERE name = '" + userInput
                + "' AND category.ckwsearch='1' AND category.isdeleted='0' LIMIT 1";
        ResultSet result = this.executeSelectQuery(query);
        result.first();
        return result.getInt("id");
    }

    public void logRequest(UssdRequest request, UssdMenu currentMenu) throws SQLException {
        this.executeNonSelectQuery("INSERT INTO " + USSD_LOG_TABLE_NAME + " (" +
                "transaction_id, menu_content, page, category_id, bread_crumb, msisdn, created_date) VALUES ('"
                + request.transactionId
                + "', '"
                + currentMenu.getMenuString("##", false, currentMenu.getItemCount() + 1, 1)
                + "', "
                + currentMenu.getPage()
                + ", "
                + ((null == currentMenu.getCategoryId()) ? 0 : currentMenu.getCategoryId())
                + ", '"
                + ((null == currentMenu.getBreadCrumb()) ? "" : currentMenu.getBreadCrumb())
                + "', '"
                + request.msisdn
                + "', NOW());");
        
    }

    public String getCategoryNameFromId(Integer categoryId) throws SQLException {
        String query = "SELECT name FROM category WHERE id = " + categoryId;
        ResultSet result = this.executeSelectQuery(query);
        result.first();
        return result.getString("name");
    }
}