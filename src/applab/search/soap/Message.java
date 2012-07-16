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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

/**
 * SMS message class for creating, building and sending SMS
 * 
 */
public class Message {

    private String sender;
    private String body;
    private String recipient;
    private String endPointURL;

    public Message(String endPointURL) {
        this.endPointURL = endPointURL;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    /**
     * Build the SMS Request to send
     * 
     * @return
     */
    private String buildSmsRequest() {
        String request = "<?xml version=\"1.0\"?>" +
                "<SendSmsRequest xmlns=\"http://schemas.applab.org/2010/08\" from=\"" + this.sender + "\">" +
                "<Message>" + this.body + "</Message>" +
                "<Recipients>";
        request += "<Recipient>" + this.recipient + "</Recipient>";

        request += "</Recipients></SendSmsRequest>";

        System.out.print("REQUEST XML: " + request);
        return request;
    }

    /**
     * Sends the message to the specified recipient;
     * 
     * @return True or false depending on whether the message is sent successfully
     */
    public boolean Send() {
        String smsRequest = buildSmsRequest();
        boolean success = false;
        URL connURL;
        try {
            connURL = new URL(this.endPointURL);
            URLConnection httpConn = connURL.openConnection();
            httpConn.addRequestProperty("content-type", "text/xml");
            httpConn.setDoOutput(true);
            OutputStreamWriter out = new OutputStreamWriter(httpConn.getOutputStream());
            out.write(smsRequest);
            out.flush();

            BufferedReader rd = new BufferedReader(new InputStreamReader(httpConn.getInputStream()));
            String line;
            while ((line = rd.readLine()) != null) {

                // Process line...
            }
            System.out.print(line);
            out.close();
            rd.close();
            success = true;
        }
        catch (MalformedURLException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return success;
    }
}
