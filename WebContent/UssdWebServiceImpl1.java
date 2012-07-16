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

import java.sql.SQLException;

import javax.jws.WebService;
import java.util.logging.Logger;

/**
 * 
 * Service Endpoint implementation class
 * 
 */

@WebService(endpointInterface = "applab.search.soap.UssdWebServiceInterface")
public class UssdWebServiceImpl1 implements UssdWebServiceInterface {
    private final static String ROOT_USSD_CODE = "*178#";
    private final static Logger logger = Logger.getLogger(UssdWebServiceImpl1.class.getName());

    @Override
    public UssdResponse handleUSSDRequest(UssdRequest request) {

        DatabaseHandler1 db = new DatabaseHandler1("localhost", "ycppquiz",
                "root", "g5*vTys-D2");
        return getResponse(request, db);
    }

    /**
     * @param request
     * @param appResp
     * @param db
     */
    public UssdResponse getResponse(UssdRequest request, DatabaseHandler1 db) {
        UssdResponse appResp = new UssdResponse();

        try {
            // Check if the request is for the first level of the menu
            if (request.userInput.equals(ROOT_USSD_CODE)) {
                appResp = getRootMenu(request, db);
            }
            else {
                appResp = getSelectedMenu(request, db);
            }

            // Decide whether we need the back option
            if (appResp.isMenu && (!appResp.isFirst)) {
                appResp.responseToSubscriber += "\r\n0. Previous Page";
            }
        }
        catch (Exception e) {
            logger.warning(e.getMessage());
            appResp.responseToSubscriber = "Sorry, unable to service your request at this time.";
            appResp.isMenu = false;
        }

        return appResp;
    }

    /**
     * Get first menu
     * 
     * @param request
     * @param db
     * @param appResp
     * @param responseToSubscriber
     * @param contentToSave
     * @param moreMenuToSave
     * @throws SQLException
     */
    public UssdResponse getRootMenu(UssdRequest request, DatabaseHandler1 db) throws SQLException {

        // Mark response as a menu
        UssdResponse appResp = new UssdResponse();
        appResp.isFirst = true;

        // Obtain the active categories from the database
        UssdMenu rootMenu = db.createRootMenu();
        rootMenu.setTitle("Farmer Services");

        appResp.responseToSubscriber = rootMenu.getMenuStringForDisplay();

        db.logRequest(request, rootMenu);

        return appResp;
    }

    /**
     * @param request
     * @param db
     * @param appResp
     * @param responseToSubscriber
     * @param contentToSave
     * @param menuToSave
     * @param moreMenuToSave
     * @throws SQLException
     * @throws NumberFormatException
     */
    public UssdResponse getSelectedMenu(UssdRequest request, DatabaseHandler1 db)
            throws SQLException,
            NumberFormatException {
        UssdResponse appResp = new UssdResponse();

        // Get displayed menu
        UssdMenu displayedMenu = db.getDisplayedMenu(request);

        // If the input was 9, just show the next page of the previous menu
        if (request.userInput.equals("9")) {
            appResp = getNextPage(request, db, displayedMenu);
        }
        else if (request.userInput.equals("0")) {

            // The input was 0, so we need to go back
            if (displayedMenu.getPage() == 1) {

                // Go to the menu before this one (the parent menu)
                appResp = getPreviousMenu(request, db, displayedMenu);
            }
            else {

                // Just go to the previous page of this menu
                appResp = getPreviousPage(request, db, displayedMenu);
            }
        }
        else {
            appResp = getNextMenu(request, db, displayedMenu);
        }

        return appResp;
    }

    // Use the previous menu to build the content for the previous menu
    private UssdResponse getPreviousMenu(UssdRequest request, DatabaseHandler1 db, UssdMenu displayedMenu) throws SQLException {
        UssdResponse appResp = new UssdResponse();

        // Get the breadcrumb, and knock off a portion
        String breadCrumb = displayedMenu.getBreadCrumb();
        String[] parts = breadCrumb.split(" ");

        if (breadCrumb == "") {

            // Just return the root menu
            return getRootMenu(request, db);
        }

        String previousBreadCrumb = "";

        // Join but leave last portion off
        for (int counter = 0; counter < parts.length - 1; counter++) {
            if (counter > 0) {
                previousBreadCrumb += " ";
            }
            previousBreadCrumb += parts[counter];
        }

        UssdMenu previousMenu = db.getRequestedMenu(previousBreadCrumb, displayedMenu.getCategoryId());

        // Set the title
        if (parts.length > 1) {
            previousMenu.setTitle(parts[parts.length - 1].replace("_", " "));
        }
        else {
            previousMenu.setTitle(db.getCategoryNameFromId(displayedMenu.getCategoryId()));
        }

        appResp.responseToSubscriber = previousMenu.getMenuStringForDisplay();
        db.logRequest(request, previousMenu);
        return appResp;
    }

    /**
     * Use the previous menu to build the content for the previously displayed page
     * 
     * @param request
     * @param db
     * @param appResp
     * @param previousMenu
     * @throws SQLException
     */
    public UssdResponse getPreviousPage(UssdRequest request, DatabaseHandler1 db, UssdMenu previousMenu) throws SQLException {
        UssdResponse appResp = new UssdResponse();
        previousMenu.setPage(previousMenu.getPage() - 1);
        appResp.responseToSubscriber = previousMenu.getMenuStringForDisplay();
        db.logRequest(request, previousMenu);
        return appResp;
    }

    /**
     * Use the previous menu to build the content for the next page
     * 
     * @param request
     * @param db
     * @param appResp
     * @param previousMenu
     * @throws SQLException
     */
    public UssdResponse getNextPage(UssdRequest request, DatabaseHandler1 db, UssdMenu previousMenu) throws SQLException {
        UssdResponse appResp = new UssdResponse();
        previousMenu.setPage(previousMenu.getPage() + 1);
        appResp.responseToSubscriber = previousMenu.getMenuStringForDisplay();
        db.logRequest(request, previousMenu);
        return appResp;
    }

    /**
     * Use the previous menu content to build the next menu when userinput is 9(more) on the last or only page of a
     * menu.
     * 
     * @param request
     * @param db
     * @param appResp
     * @param previousMenu
     * @return
     * @throws NumberFormatException
     * @throws SQLException
     */
    public UssdResponse getNextMenu(UssdRequest request, DatabaseHandler1 db, UssdMenu previousMenu)
            throws NumberFormatException, SQLException {

        UssdResponse appResp = new UssdResponse();

        // Get the meaning of the user's input & the categoryId
        String selectedItem = previousMenu.getItem(Integer.parseInt(request.userInput) - 1);
        Integer categoryId = previousMenu.getCategoryId();
        if (categoryId == 0) {

            // This was the first menu, so we get the categoryId from the user's input
            categoryId = db.getCategoryIdFromName(selectedItem);
        }
        else {

            // Get the new breadcrumb
            previousMenu.addPathToBreadCrumb(selectedItem);
        }

        UssdMenu currentMenu = db.getRequestedMenu(previousMenu.getBreadCrumb(), categoryId);

        // Set the title
        currentMenu.setTitle(selectedItem);

        if (currentMenu.getItemCount() == 0) {

            // Then end of keyword is implied and we return content instead
            appResp = getContent(request, currentMenu.getBreadCrumb(), categoryId, db);
        }
        else {
            appResp.responseToSubscriber = currentMenu.getMenuStringForDisplay();
        }

        db.logRequest(request, currentMenu);
        return appResp;
    }

    // Obtain content to format and send as SMS
    private UssdResponse getContent(UssdRequest request, String breadCrumb, Integer categoryId, DatabaseHandler1 db) throws SQLException {
        UssdResponse response = new UssdResponse();
        response.isMenu = false;
        String responseText = db.getContent(request, breadCrumb, categoryId);
        if (responseText.length() > 160) {
            response.responseToSubscriber = "Request has been received. Please wait for SMS with response to your query.";
            sendSMS("Grameen", "+" + request.msisdn, responseText);
        }
        else {
            response.responseToSubscriber = responseText;
        }
        return response;
    }

    public void sendSMS(String sender, String recipient, String content) {
        Message message = new Message("http://ckwapps.applab.org:8888/services/sendSms");
        message.setSender(sender);
        message.setRecipient(recipient);
        message.setBody(content);
        message.Send();
    }

}