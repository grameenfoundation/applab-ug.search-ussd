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

import java.io.Serializable;

/**
 * 
 * Serializable USSDRequest that is received by the web service
 * 
 */
public class UssdRequest implements Serializable {

    // Required UID for serialization
    private static final long serialVersionUID = -2761065451328878881L;

    // The transaction ID for the request
    public String transactionId;

    // The requesting MSISDN
    public String msisdn;

    // The input from the user
    public String userInput;

}
