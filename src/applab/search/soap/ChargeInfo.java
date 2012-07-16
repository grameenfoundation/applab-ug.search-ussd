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
 * @author Francis Serializable charge information for a chargeable USSD Request
 * 
 */
public class ChargeInfo implements Serializable {

    // Required UID for serialization
    private static final long serialVersionUID = 8574127985370335316L;

    // The charge amount of a chargeable USSD request
    public double amount;

    // The content type of the chargeable USSD request
    public String contentType;

}
