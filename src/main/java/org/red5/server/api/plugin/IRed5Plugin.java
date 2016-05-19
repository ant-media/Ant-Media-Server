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

package org.red5.server.api.plugin;

import org.red5.server.Server;
import org.springframework.context.ApplicationContext;

/**
 * Base interface for a Red5 server Plug-in.
 * 
 * @author Paul Gregoire (mondain@gmail.com)
 */
public interface IRed5Plugin {

    /**
     * Returns a name / identifier for the plug-in.
     * 
     * @return plug-in's name
     */
    String getName();

    /**
     * Sets the top-most ApplicationContext within Red5.
     * 
     * @param context
     *            application context
     */
    void setApplicationContext(ApplicationContext context);

    /**
     * Sets a reference to the server.
     * 
     * @param server
     *            server
     */
    void setServer(Server server);

    /**
     * Lifecycle method called when the plug-in is started.
     * 
     * @throws Exception
     *             on start error
     */
    void doStart() throws Exception;

    /**
     * Lifecycle method called when the plug-in is stopped.
     * 
     * @throws Exception
     *             on stop error
     */
    void doStop() throws Exception;

}