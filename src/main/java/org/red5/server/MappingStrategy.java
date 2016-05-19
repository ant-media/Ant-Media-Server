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

package org.red5.server;

import org.red5.server.api.IMappingStrategy;

/**
 * Basic mapping strategy implementation. This one uses slash as filesystem path separator, '.service' postfix for services naming, '.handler' for handlers naming and 'default' string as default application name.
 */
public class MappingStrategy implements IMappingStrategy {
    /**
     * Root constant
     */
    private static final String ROOT = "";

    /**
     * Handler extension constant
     */
    private static final String HANDLER = ".handler";

    /**
     * Dir separator constant
     */
    private static final String DIR = "/";

    /**
     * Service extension constant
     */
    private static final String SERVICE = ".service";

    /**
     * Default application name
     */
    private String defaultApp = "default";

    /**
     * Setter for default application name ('default' by default).
     * 
     * @param defaultApp
     *            Default application
     */
    public void setDefaultApp(String defaultApp) {
        this.defaultApp = defaultApp;
    }

    /**
     * Resolves resource prefix from path. Default application used as root when path is specified.
     * 
     * @param path
     *            Path
     * @return Resource prefix according to this naming strategy
     */
    public String mapResourcePrefix(String path) {
        if (path == null || path.equals(ROOT)) {
            return defaultApp + DIR;
        } else {
            return path + DIR;
        }
    }

    /**
     * Resolves scope handler name for path. Default application used as root when path is specified.
     * 
     * @param path
     *            Path
     * @return Scope handler name according to this naming strategy
     */
    public String mapScopeHandlerName(String path) {
        if (path == null || path.equals(ROOT)) {
            return defaultApp + HANDLER;
        } else {
            return path + HANDLER;
        }
    }

    /**
     * Resolves service filename name from name
     * 
     * @param name
     *            Service name
     * @return Service filename according to this naming strategy
     */
    public String mapServiceName(String name) {
        return name + SERVICE;
    }

}
