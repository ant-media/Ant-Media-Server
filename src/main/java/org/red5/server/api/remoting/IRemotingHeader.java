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

package org.red5.server.api.remoting;

/**
 * A Remoting header.
 * 
 * @author The Red5 Project
 * @author Joachim Bauch (jojo@struktur.de)
 */
public interface IRemotingHeader {

    /** Name of header specifying string to add to gateway url. */
    public static final String APPEND_TO_GATEWAY_URL = "AppendToGatewayUrl";

    /** Name of header specifying new gateway url to use. */
    public static final String REPLACE_GATEWAY_URL = "ReplaceGatewayUrl";

    /** Name of header specifying new header to send. */
    public static final String PERSISTENT_HEADER = "RequestPersistentHeader";

    /** Name of header containing authentication data. */
    public static final String CREDENTIALS = "Credentials";

    /** Name of header to request debug informations from the server. */
    public static final String DEBUG_SERVER = "amf_server_debug";

    /**
     * Return name of header.
     * 
     * @return name of header
     */
    public String getName();

    /**
     * Return value of header.
     * 
     * @return value of header
     */
    public Object getValue();

    /**
     * Return boolean flag if receiver must process header before handling other headers or messages.
     * 
     * @return must understand
     */
    public boolean getMustUnderstand();

}
