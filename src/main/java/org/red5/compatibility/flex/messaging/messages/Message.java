/*
 * RED5 Open Source Media Server - https://github.com/Red5/ Copyright 2006-2016 by respective authors (see below). All rights reserved. Licensed under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless
 * required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package org.red5.compatibility.flex.messaging.messages;

import java.util.Map;

public interface Message {

    public static final String NEEDS_CONFIG_HEADER = "DSNeedsConfig";

    public static final String POLL_WAIT_HEADER = "DSPollWait";

    public static final String PRESERVE_DURABLE_HEADER = "DSPreserveDurable";

    public static final String REMOVE_SUBSCRIPTIONS = "DSRemSub";

    public static final String SELECTOR_HEADER = "DSSelector";

    public static final String SUBSCRIPTION_INVALIDATED_HEADER = "DSSubscriptionInvalidated";

    public static final String SUBTOPIC_SEPARATOR = "_;_";

    public static final String MESSAGING_VERSION = "DSMessagingVersion";

    public static final String DESTINATION_CLIENT_ID_HEADER = "DSDstClientId";

    public static final String ENDPOINT_HEADER = "DSEndpoint";

    public static final String FLEX_CLIENT_ID_HEADER = "DSId";

    public static final String REMOTE_CREDENTIALS_HEADER = "DSRemoteCredentials";

    public static final String SYNC_HEADER = "sync";

    /**
     * Returns the body of the message.
     * 
     * @return message body
     */
    Object getBody();

    /**
     * Returns the client id indicating the client that sent the message.
     * 
     * @return client id
     */
    String getClientId();

    /**
     * Returns the destination that the message targets.
     * 
     * @return destination
     */
    String getDestination();

    /**
     * Returns a header value corresponding to the passed header name.
     * 
     * @param name
     *            header name
     * @return header value
     */
    Object getHeader(String name);

    /**
     * Returns the headers for the message.
     * 
     * @return headers
     */
    Map<String, Object> getHeaders();

    /**
     * Returns the unique message id.
     * 
     * @return message id
     */
    String getMessageId();

    /**
     * Returns the timestamp for the message.
     * 
     * @return timestamp
     */
    long getTimestamp();

    /**
     * Returns the time to live for the message.
     * 
     * @return time to live
     */
    long getTimeToLive();

    /**
     * Tests whether a header with the passed name exists.
     * 
     * @param name
     *            header name
     * @return true if header exists, false otherwise
     */
    boolean headerExists(String name);

    /**
     * Sets the body of the message.
     * 
     * @param value
     *            body
     */
    void setBody(Object value);

    /**
     * Sets the client id indicating the client that sent the message.
     * 
     * @param value
     *            client id
     */
    void setClientId(String value);

    /**
     * Sets the destination that the message targets.
     * 
     * @param value
     *            destination
     */
    void setDestination(String value);

    /**
     * Sets a header on the message.
     * 
     * @param name
     *            header name
     * @param value
     *            header value
     */
    void setHeader(String name, Object value);

    /**
     * Sets the headers for the message.
     * 
     * @param values
     *            header values
     */
    void setHeaders(Map<String, Object> values);

    /**
     * Sets the unique message id.
     * 
     * @param value
     *            message id
     */
    void setMessageId(String value);

    /**
     * Sets the timestamp for the message.
     * 
     * @param value
     *            timestamp
     */
    void setTimestamp(long value);

    /**
     * Sets the time to live for the message.
     * 
     * @param value
     *            ttl
     */
    void setTimeToLive(long value);

}
