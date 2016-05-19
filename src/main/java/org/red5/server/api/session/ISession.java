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

package org.red5.server.api.session;

import java.io.Serializable;

/**
 * Represents the most basic type of "Session", loosely modeled after the HTTP Session used in J2EE applications.
 *
 *
 * @author The Red5 Project
 * @author Paul Gregoire (mondain@gmail.com)
 */
public interface ISession extends Serializable {

    /**
     * Returns creation time in milliseconds.
     * 
     * @return creation time
     */
    public long getCreated();

    /**
     * Returns the session's identifier.
     * 
     * @return session id
     */
    public String getSessionId();

    /**
     * Resets a specified set of internal parameters.
     */
    public void reset();

    /**
     * Returns the active state of the session.
     * 
     * @return is active
     */
    public boolean isActive();

    /**
     * Ends the session, no further modifications should be allowed.
     */
    public void end();

    /**
     * Sets the associated client id.
     * 
     * @param clientId
     *            client id
     */
    public void setClientId(String clientId);

    /**
     * Returns the client id associated with this session.
     * 
     * @return client id
     */
    public String getClientId();

    /**
     * Sets where session resources will be located if persisted to disk.
     * 
     * @param destinationDirectory
     *            destination directory
     */
    public void setDestinationDirectory(String destinationDirectory);

    /**
     * Returns the directory used to store session resources.
     *
     * @return destination directory
     */
    public String getDestinationDirectory();

}
