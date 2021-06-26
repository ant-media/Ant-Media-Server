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
 * Client not found
 */
public class ClientNotFoundException extends RuntimeException {

    /**
	 * 
	 */
    private static final long serialVersionUID = 3135070223941800751L;

    /**
     * Create exception from given string message
     * 
     * @param id
     *            id
     */
    public ClientNotFoundException(String id) {
        super("Client \"" + id + "\" not found.");
    }

}
