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

package org.red5.server.net.remoting;

import org.red5.server.api.remoting.IRemotingHeader;

/**
 * Remoting header to be sent to a server.
 * 
 * @author The Red5 Project
 * @author Joachim Bauch (jojo@struktur.de)
 */
public class RemotingHeader implements IRemotingHeader {

    /**
     * The name of the header.
     */
    protected String name;

    /**
     * Is this header required?
     */
    protected boolean required;

    /**
     * The actual data of the header.
     */
    protected Object data;

    /**
     * Create a new header to be sent through remoting.
     * 
     * @param name
     *            Header name
     * @param required
     *            Header required?
     * @param data
     *            Header data
     */
    public RemotingHeader(String name, boolean required, Object data) {
        this.name = name;
        this.required = required;
        this.data = data;
    }

    /** {@inheritDoc} */
    public boolean getMustUnderstand() {
        return required;
    }

    /** {@inheritDoc} */
    public String getName() {
        return name;
    }

    /** {@inheritDoc} */
    public Object getValue() {
        return data;
    }
}
