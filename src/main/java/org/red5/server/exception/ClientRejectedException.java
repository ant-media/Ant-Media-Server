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
 * The client is not allowed to connect. Reason that provided with this exception is sent to client-side status event description.
 *
 */
public class ClientRejectedException extends RuntimeException {

    private static final long serialVersionUID = 9204597649465357898L;

    @SuppressWarnings("all")
    private Object reason;

    /** Constructs a new ClientRejectedException. */
    public ClientRejectedException() {
        this("Client rejected");
    }

    /**
     * Create new exception with given rejection reason
     * 
     * @param reason
     *            Rejection reason
     */
    public ClientRejectedException(Object reason) {
        super("Client rejected");
        this.reason = reason;
    }

    /**
     * Getter for reason
     *
     * @return Rejection reason
     */
    public Object getReason() {
        return reason;
    }

}
