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
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.jws.WebService;
import javax.naming.NamingException;
import javax.servlet.ServletContext;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;

import applab.server.ApplabConfiguration;

/**
 * 
 * Service Endpoint implementation class
 * 
 */

@WebService(endpointInterface = "applab.search.soap.UssdWebServiceInterface")
public class UssdWebServiceImpl implements UssdWebServiceInterface {

	private final static Logger logger = Logger
			.getLogger(UssdWebServiceImpl.class.getName());

	private final static String ROOT_USSD_MENU = "*178#";

	@Resource
	private WebServiceContext context;
	public int isCompleted = 0;
	public int isTopLevel = 0;
	public int BSICategoryId = 77;

	/**
	 * @param request
	 * @param appResp
	 * @param db
	 */
	public UssdResponse getResponse(UssdRequest request) {
		UssdResponse appResp = new UssdResponse();
		try {

			// Check for valid user input
			if (request.userInput.matches("[^0-9]")) {
				appResp.responseToSubscriber = "Wrong input!!";
				appResp.isMenu = false;
				appResp.isFirst = true;
			}

			// Check if the request is for the first level of the menu
			else if (request.userInput.equals(ROOT_USSD_MENU)) {
				appResp = getTopLevelMenu(request);
			} else {
				appResp = getSelectedMenu(request);
			}

			// Decide whether we need the back option
			if (appResp.isMenu && (!appResp.isFirst) && (isTopLevel != 1)) {
				appResp.responseToSubscriber += "\r\n0. Previous Page";
			}
		} catch (Exception e) {
			logger.warning(e.getMessage());
			appResp.responseToSubscriber = "Sorry, unable to service your request at this time.";
			appResp.isMenu = false;
		}

		return appResp;
	}

	// Obtain content to format and send as SMS
	private UssdResponse getContent(UssdRequest request, String breadCrumb,
			Integer categoryId) throws Exception {
		UssdResponse response = new UssdResponse();
		response.isMenu = false;
		String responseText = DatabaseHandler
				.getContent(breadCrumb, categoryId);

		// response text exceeds 160 character limit send SMS
		if (responseText.length() > 160) {
			response.responseToSubscriber = "Request has been received. Please wait for SMS with response to your query.";
			sendSMS("Grameen", "+" + request.msisdn, responseText);
		} else {
			response.responseToSubscriber = responseText;
		}
		return response;
	}

	/**
	 * Use the previous menu content to build the next menu when userinput is
	 * 9(more) on the last or only page of a menu.
	 * 
	 * @param request
	 * @param db
	 * @param appResp
	 * @param previousMenu
	 * @return
	 * @throws NumberFormatException
	 * @throws SQLException
	 */
	public UssdResponse getNextMenu(UssdRequest request, UssdMenu previousMenu)
			throws Exception {
		isCompleted = 0;
		UssdResponse appResp = new UssdResponse();

		// Get the meaning of the user's input & the categoryId
		String selectedItem = previousMenu.getItem(Integer
				.parseInt(request.userInput) - 1);
		Integer categoryId = previousMenu.getCategoryId();
		if (categoryId == 0) {
			if (isTopLevel == 1) {
				UssdMenu rootMenu = new UssdMenu();
				if (selectedItem.equalsIgnoreCase("Farmer Services")) {
					rootMenu = DatabaseHandler.createRootMenu();
					rootMenu.setTitle("Farmer Services");
					isTopLevel = 0;
				} else if (selectedItem.equalsIgnoreCase("Budget Services")) {
					rootMenu = DatabaseHandler
							.createOtherRootMenu(BSICategoryId);
					rootMenu.setTitle("Budget Services");
					isTopLevel = 0;
				}

				appResp.responseToSubscriber = rootMenu
						.getMenuStringForDisplay();
				isCompleted = 0;
				DatabaseHandler.logRequest(request, rootMenu, isCompleted);
				return appResp;
			} else {

				// This was the first menu, so we get the categoryId from the
				// user's
				// input
				selectedItem = selectedItem.replace(" ", "_");
				categoryId = DatabaseHandler
						.getCategoryIdFromName(selectedItem);
				selectedItem = selectedItem.replace("_", " ");

			}
		} else {

			// Get the new breadcrumb
			previousMenu.addPathToBreadCrumb(selectedItem);
		}
		UssdMenu currentMenu = DatabaseHandler.getRequestedMenu(
				previousMenu.getBreadCrumb(), categoryId);

		// Set the title
		currentMenu.setTitle(selectedItem);
		if (currentMenu.getItemCount() == 0) {

			// Then end of keyword is implied and we return content instead
			appResp = getContent(request, currentMenu.getBreadCrumb(),
					categoryId);

			// Indicate end of ussd request process
			isCompleted = 1;
		} else {
			appResp.responseToSubscriber = currentMenu
					.getMenuStringForDisplay();
		}
		DatabaseHandler.logRequest(request, currentMenu, isCompleted);
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
	public UssdResponse getNextPage(UssdRequest request, UssdMenu previousMenu)
			throws Exception {
		isCompleted = 0;
		UssdResponse appResp = new UssdResponse();
		previousMenu.setPage(previousMenu.getPage() + 1);
		appResp.responseToSubscriber = previousMenu.getMenuStringForDisplay();
		DatabaseHandler.logRequest(request, previousMenu, isCompleted);
		return appResp;
	}

	// Use the previous menu to build the content for the previous menu
	private UssdResponse getPreviousMenu(UssdRequest request,
			UssdMenu displayedMenu) throws Exception {
		UssdResponse appResp = new UssdResponse();
		isCompleted = 0;
		UssdMenu previousMenu = new UssdMenu();

		// Get the breadcrumb, and knock off a portion
		String breadCrumb = displayedMenu.getBreadCrumb();
		String[] parts = breadCrumb.split(" ");

		if (breadCrumb == "") {

			if (displayedMenu.getCategoryId() != 0) {
				if (displayedMenu.getCategoryId() == BSICategoryId) {
					previousMenu = DatabaseHandler
							.createOtherRootMenu(BSICategoryId);
					previousMenu.setTitle("Budget Services");
					isTopLevel = 0;
					appResp.responseToSubscriber = previousMenu
							.getMenuStringForDisplay();
				} else {
					previousMenu = DatabaseHandler.createRootMenu();
					previousMenu.setTitle("Farmer Services");
					isTopLevel = 0;
					appResp.responseToSubscriber = previousMenu
							.getMenuStringForDisplay();
				}

			} else {
				request.userInput = ROOT_USSD_MENU;
				return getTopLevelMenu(request);
			}
		} else {
			String previousBreadCrumb = "";

			// Join but leave last portion off
			for (int counter = 0; counter < parts.length - 1; counter++) {
				if (counter > 0) {
					previousBreadCrumb += " ";
				}
				previousBreadCrumb += parts[counter];
			}

			previousMenu = DatabaseHandler.getRequestedMenu(previousBreadCrumb,
					displayedMenu.getCategoryId());

			// Set the title
			if (parts.length > 1) {
				previousMenu
						.setTitle(parts[parts.length - 1].replace("_", " "));
			} else {
				previousMenu.setTitle(DatabaseHandler.getCategoryNameFromId(
						displayedMenu.getCategoryId()).replace("_", " "));
			}

			appResp.responseToSubscriber = previousMenu
					.getMenuStringForDisplay();
		}
		DatabaseHandler.logRequest(request, previousMenu, isCompleted);
		return appResp;
	}

	/**
	 * Use the previous menu to build the content for the previously displayed
	 * page
	 * 
	 * @param request
	 * @param db
	 * @param appResp
	 * @param previousMenu
	 * @throws SQLException
	 */
	public UssdResponse getPreviousPage(UssdRequest request,
			UssdMenu previousMenu) throws Exception {
		isCompleted = 0;
		UssdResponse appResp = new UssdResponse();
		previousMenu.setPage(previousMenu.getPage() - 1);
		appResp.responseToSubscriber = previousMenu.getMenuStringForDisplay();
		DatabaseHandler.logRequest(request, previousMenu, isCompleted);
		return appResp;
	}

	public UssdResponse getTopLevelMenu(UssdRequest request) throws Exception {

		// Mark response as a menu
		UssdResponse appResp = new UssdResponse();
		appResp.isFirst = true;
		isTopLevel = 1;
		UssdMenu rootMenu = new UssdMenu();

		// Display top level categories
		if (request.userInput.equals(ROOT_USSD_MENU)) {
			rootMenu.addItem("Farmer Services");
			rootMenu.addItem("Budget Services");
			rootMenu.setTitle("Grameen Services");
		}

		appResp.responseToSubscriber = rootMenu.getMenuStringForDisplay();
		isCompleted = 0;
		DatabaseHandler.logRequest(request, rootMenu, isCompleted);
		return appResp;
	}

	/**
	 * Get other non-first menu
	 * 
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
	public UssdResponse getSelectedMenu(UssdRequest request) throws Exception {
		UssdResponse appResp = new UssdResponse();

		// Get displayed menu
		UssdMenu displayedMenu = DatabaseHandler.getDisplayedMenu(request);

		// If the input was 9, just show the next page of the previous menu
		if (request.userInput.equals("9")) {
			appResp = getNextPage(request, displayedMenu);
		} else if (request.userInput.equals("0")) {

			// The input was 0, so we need to go back
			if (displayedMenu.getPage() == 1) {

				// Go to the menu before this one (the parent menu)
				appResp = getPreviousMenu(request, displayedMenu);
			} else {

				// Just go to the previous page of this menu
				appResp = getPreviousPage(request, displayedMenu);
			}
		} else {
			appResp = getNextMenu(request, displayedMenu);
		}
		return appResp;
	}

	@Override
	public UssdResponse handleUSSDRequest(UssdRequest request) {

		// Pick AppLabWebService context
		ServletContext servletContext = (ServletContext) context
				.getMessageContext().get(MessageContext.SERVLET_CONTEXT);
		try {
			ApplabConfiguration.initConfiguration(servletContext);
		} catch (NamingException e) {
			e.printStackTrace();
		}
		return getResponse(request);
	}

	public void sendSMS(String sender, String recipient, String content) {
		Message message = new Message(
				"http://ckwapps.applab.org:8888/services/sendSms");
		message.setSender(sender);
		message.setRecipient(recipient);
		message.setBody(content);
		message.Send();
	}
}