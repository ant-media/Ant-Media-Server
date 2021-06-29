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

package org.red5.server.exception;

/**
 * Exception class than contains additional parameters to return to the client.
 * 
 * @author The Red5 Project
 * @author Joachim Bauch (jojo@struktur.de)
 */
public class ClientDetailsException extends RuntimeException {

    private static final long serialVersionUID = -1908769505547253205L;

    /**
     * Parameters to return to the client.
     */
    private Object parameters;

    /**
     * Also return stacktrace to client?
     */
    private boolean stacktrace;

    /**
     * Create new exception object from message and parameters. By default, no stacktrace is returned to the client.
     * 
     * @param message
     *            message
     * @param params
     *            parameters for message
     */
    public ClientDetailsException(String message, Object params) {
        this(message, params, false);
    }

    /**
     * Create new exception object from message and parameters with optional stacktrace.
     * 
     * @param message
     *            message
     * @param params
     *            parameters
     * @param includeStacktrace
     *            whether or not to include a stack trace
     */
    public ClientDetailsException(String message, Object params, boolean includeStacktrace) {
        super(message);
        this.parameters = params;
        this.stacktrace = includeStacktrace;
    }

    /**
     * Get parameters to return to the client.
     * 
     * @return parameters
     */
    public Object getParameters() {
        return parameters;
    }

    /**
     * Should the stacktrace returned to the client?
     * 
     * @return stacktrace
     */
    public boolean includeStacktrace() {
        return stacktrace;
    }

}
