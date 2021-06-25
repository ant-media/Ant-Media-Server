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

package org.red5.server.api;

/**
 * This interface encapsulates the mapping strategy used by the context.
 */
public interface IMappingStrategy {

    /**
     * Map a name to the name of a service.
     * 
     * @param name
     *            name to map
     * @return The name of the service with the passed name
     */
    public String mapServiceName(String name);

    /**
     * Map a context path to the name of a scope handler.
     * 
     * @param contextPath
     *            context path to map
     * @return The name of a scope handler
     */
    public String mapScopeHandlerName(String contextPath);

    /**
     * Map a context path to a path prefix for resources.
     * 
     * @param contextPath
     *            context path to map
     * @return The path prefix for resources with the given name
     */
    public String mapResourcePrefix(String contextPath);

}