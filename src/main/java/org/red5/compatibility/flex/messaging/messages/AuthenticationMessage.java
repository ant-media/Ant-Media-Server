/*
 * RED5 Open Source Media Server - https://github.com/Red5/ Copyright 2006-2016 by respective authors (see below). All rights reserved. Licensed under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless
 * required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package org.red5.compatibility.flex.messaging.messages;

/**
 * Added to support flex.messaging.messages.AuthenticationMessage as noted in http://jira.red5.org/browse/APPSERVER-176
 * 
 * @author The Red5 Project
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class AuthenticationMessage extends CommandMessage {

    private static final long serialVersionUID = -9135142173898013075L;

    //TODO: we need to decode the body to actually make this viable
    //body=cXdlcXdldzpxd2Vxd2Vxd2Vxd2U

}
