/*
 * RED5 Open Source Media Server - https://github.com/Red5/ Copyright 2006-2016 by respective authors (see below). All rights reserved. Licensed under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless
 * required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package org.red5.compatibility.flex.messaging.messages;

/**
 * Constants for the flex compatibility messages.
 * 
 * @author The Red5 Project
 * @author Joachim Bauch (jojo@struktur.de)
 */
public class Constants {

    /** Operation id of subscribe command. */
    public static final int SUBSCRIBE_OPERATION = 0;

    /** Operation id of unsubscribe command. */
    public static final int UNSUBSCRIBE_OPERATION = 1;

    /** Operation id of poll command. */
    public static final int POLL_OPERATION = 2;

    /** Update given attributes from a data message. */
    public static final int DATA_OPERATION_UPDATE_ATTRIBUTES = 3;

    public static final int CLIENT_SYNC_OPERATION = 4;

    /** Operation id of ping commands. */
    public static final int CLIENT_PING_OPERATION = 5;

    /** Update destination based on nested DataMessage packet. */
    public static final int DATA_OPERATION_UPDATE = 7;

    public static final int CLUSTER_REQUEST_OPERATION = 7;

    /** Operation id of authentication commands. */
    public static final int LOGIN_OPERATION = 8;

    public static final int LOGOUT_OPERATION = 9;

    /** Set all attributes from a data message. */
    public static final int DATA_OPERATION_SET = 10;

    public static final int SUBSCRIPTION_INVALIDATE_OPERATION = 10;

    public static final int MULTI_SUBSCRIBE_OPERATION = 11;

    public static final int UNKNOWN_OPERATION = 10000;

}
