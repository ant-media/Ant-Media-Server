/*
 * RED5 Open Source Flash Server - https://github.com/Red5/
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

package org.red5.server.security.jaas;

import java.io.Serializable;
import java.security.Principal;

/**
 * Represents a user. <br />
 * Principals may be associated with a particular <code>Subject</code> to augment it with an additional identity. Authorization decisions can be based upon the Principals associated with a <code>Subject</code>.
 * 
 * @see java.security.Principal
 * @see javax.security.auth.Subject
 */
public class SimplePrincipal implements Principal, Serializable {

    private static final long serialVersionUID = -5845179654012035528L;

    /**
     * @serial
     */
    private String name;

    private String passwd;

    /**
     * Create a Principal with the given name.
     * 
     * @param name
     *            the username for this user
     * @param password
     *            the password for this user
     * @exception NullPointerException
     *                if the name is null.
     */
    public SimplePrincipal(String name, String password) {
        if (name == null) {
            throw new NullPointerException("Name cannot be null");
        }
        this.name = name;
        this.passwd = password;
    }

    /** {@inheritDoc} */
    public String getName() {
        return name;
    }

    /**
     * @return the passwd
     */
    public String getPassword() {
        return passwd;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        SimplePrincipal other = (SimplePrincipal) obj;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        return true;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "SimplePrincipal [name=" + name + ", password=" + passwd + "]";
    }

}
