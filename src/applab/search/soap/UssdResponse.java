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
 * Serializable response for a USSD Request
 * 
 */
public class UssdResponse implements Serializable {

    // Required UID for serialization
    private static final long serialVersionUID = 1058864762423086756L;

    // Flag for whether the response is a menu or not.
    public boolean isMenu = true;

    // Flag for whether this is the first menu or not
    public boolean isFirst = false;

    // The details of the response to be sent to the subscriber
    public String responseToSubscriber;

    // The Charging information if the request is chargeable.
    public ChargeInfo chargeableInfo;

}