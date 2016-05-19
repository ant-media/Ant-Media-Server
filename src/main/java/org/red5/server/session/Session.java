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

package org.red5.server.session;

import org.red5.server.api.session.ISession;

/**
 * Represents the most basic type of "Session", loosely modeled after the HTTP Session used in J2EE applications.
 *
 *
 * @author The Red5 Project
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class Session implements ISession {

    private static final long serialVersionUID = 2893666721L;

    //time at which this session instance was created
    protected long created;

    //whether or not this session is in an active state
    protected boolean active;

    //unique identifier for this session
    protected String sessionId;

    //location where resources may be stored for this instance
    protected String destinationDirectory;

    //flash client identifier
    protected String clientId;

    {
        //set current time as created time
        created = System.currentTimeMillis();
        //set as active
        active = true;
    }

    public Session() {
    }

    public Session(String sessionId) {
        this.sessionId = sessionId;
    }

    public long getCreated() {
        return created;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void reset() {
        clientId = null;
    }

    public boolean isActive() {
        return active;
    }

    public void end() {
        active = false;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public void setDestinationDirectory(String destinationDirectory) {
        this.destinationDirectory = destinationDirectory;
    }

    public String getDestinationDirectory() {
        return destinationDirectory;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((sessionId == null) ? 0 : sessionId.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final Session other = (Session) obj;
        if (sessionId == null) {
            if (other.sessionId != null)
                return false;
        } else if (!sessionId.equals(other.sessionId))
            return false;
        return true;
    }

}
