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

package org.red5.server.api.statistics;

/**
 * Statistical informations about a scope.
 * 
 * @author The Red5 Project
 * @author Joachim Bauch (jojo@struktur.de)
 */
public interface IScopeStatistics extends IStatisticsBase {

    /**
     * Get the name of this scope. Eg.
     * 
     * <pre>
     * someroom
     * </pre>
     * 
     * .
     * 
     * @return the name
     */
    public String getName();

    /**
     * Get the full absolute path. Eg.
     * 
     * <pre>
     * host / myapp / someroom
     * </pre>
     * 
     * .
     * 
     * @return Absolute scope path
     */
    public String getPath();

    /**
     * Get the scopes depth, how far down the scope tree is it. The lowest depth is 0x00, the depth of Global scope. Application scope depth is 0x01. Room depth is 0x02, 0x03 and so forth.
     * 
     * @return the depth
     */
    public int getDepth();

    /**
     * Return total number of connections to the scope.
     * 
     * @return number of connections
     */
    public int getTotalConnections();

    /**
     * Return maximum number of concurrent connections to the scope.
     * 
     * @return number of connections
     */
    public int getMaxConnections();

    /**
     * Return current number of connections to the scope.
     * 
     * @return number of connections
     */
    public int getActiveConnections();

    /**
     * Return total number of clients connected to the scope.
     * 
     * @return number of clients
     */
    public int getTotalClients();

    /**
     * Return maximum number of clients concurrently connected to the scope.
     * 
     * @return number of clients
     */
    public int getMaxClients();

    /**
     * Return current number of clients connected to the scope.
     * 
     * @return number of clients
     */
    public int getActiveClients();

    /**
     * Return total number of subscopes created.
     * 
     * @return number of subscopes created
     */
    public int getTotalSubscopes();

    /**
     * Return maximum number of concurrently existing subscopes.
     * 
     * @return number of subscopes
     */
    public int getMaxSubscopes();

    /**
     * Return number of currently existing subscopes.
     * 
     * @return number of subscopes
     */
    public int getActiveSubscopes();

}
