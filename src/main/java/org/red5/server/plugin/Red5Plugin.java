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

package org.red5.server.plugin;

import org.red5.server.Server;
import org.red5.server.adapter.MultiThreadedApplicationAdapter;
import org.red5.server.api.plugin.IRed5Plugin;
import org.springframework.context.ApplicationContext;

/**
 * Provides more features to the plug-in system.
 * 
 * @author Paul Gregoire (mondain@gmail.com)
 */
public abstract class Red5Plugin implements IRed5Plugin {

    protected ApplicationContext context;

    protected Server server;

    /** {@inheritDoc} */
    public void doStart() throws Exception {
    }

    /** {@inheritDoc} */
    public void doStop() throws Exception {
    }

    /**
     * Initialize the plug-in
     */
    public void init() {
    }

    /** {@inheritDoc} */
    public String getName() {
        return null;
    }

    /** {@inheritDoc} */
    public void setApplicationContext(ApplicationContext context) {
        this.context = context;
    }

    /**
     * Return the server reference.
     * 
     * @return server
     */
    public Server getServer() {
        return server;
    }

    /** {@inheritDoc} */
    public void setServer(Server server) {
        this.server = server;
    }

    /**
     * Set the application making use of this plug-in.
     * 
     * @param application
     *            application
     */
    public void setApplication(MultiThreadedApplicationAdapter application) {
    }

}
