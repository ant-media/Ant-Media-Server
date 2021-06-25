/*
 * RED5 Open Source Media Server - https://github.com/Red5/
 * 
 * Copyright 2006-2016 by respective authors (see below). All rights reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.red5.server.so;

/**
 * One update event for a shared object received through a connection.
 */
public interface ISharedObjectEvent {

    enum Type {
        SERVER_CONNECT, SERVER_DISCONNECT, SERVER_SET_ATTRIBUTE, SERVER_DELETE_ATTRIBUTE, SERVER_SEND_MESSAGE, CLIENT_CLEAR_DATA, CLIENT_DELETE_ATTRIBUTE, CLIENT_DELETE_DATA, CLIENT_INITIAL_DATA, CLIENT_STATUS, CLIENT_UPDATE_DATA, CLIENT_UPDATE_ATTRIBUTE, CLIENT_SEND_MESSAGE
    };

    /**
     * Returns the type of the event.
     * 
     * @return the type of the event.
     */
    public Type getType();

    /**
     * Returns the key of the event.
     * 
     * Depending on the type this contains:
     * <ul>
     * <li>the attribute name to set for SET_ATTRIBUTE</li>
     * <li>the attribute name to delete for DELETE_ATTRIBUTE</li>
     * <li>the handler name to call for SEND_MESSAGE</li>
     * </ul>
     * In all other cases the key is
     * 
     * <pre>
     * null
     * </pre>
     * 
     * .
     * 
     * @return the key of the event
     */
    public String getKey();

    /**
     * Returns the value of the event.
     * 
     * Depending on the type this contains:
     * <ul>
     * <li>the attribute value to set for SET_ATTRIBUTE</li>
     * <li>a list of parameters to pass to the handler for SEND_MESSAGE</li>
     * </ul>
     * In all other cases the value is
     * 
     * <pre>
     * null
     * </pre>
     * 
     * .
     * 
     * @return the value of the event
     */
    public Object getValue();

}
