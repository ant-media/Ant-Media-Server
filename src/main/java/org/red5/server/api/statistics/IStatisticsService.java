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

import java.util.Set;

import org.red5.server.exception.ScopeNotFoundException;
import org.red5.server.exception.SharedObjectException;

/**
 * Statistics methods for Red5. They can be used to poll for updates of given elements inside the server. Statistics data will be stored as properties of different shared objects.
 * 
 * Use
 * 
 * <pre>
 * getScopeStatisticsSO
 * </pre>
 * 
 * and
 * 
 * <pre>
 * getSharedObjectStatisticsSO
 * </pre>
 * 
 * to get these shared objects. The property names are
 * 
 * <pre>
 * scopeName
 * </pre>
 * 
 * for scope attributes and
 * 
 * <pre>
 * scopeName | sharedObjectName
 * </pre>
 * 
 * for shared object attributes. Each property holds a Map containing key/value mappings of the corresponding attributes.
 * 
 * Sometime in the future, the updates on the shared objects will be done automatically so a client doesn't need to poll for them.
 * 
 * @author The Red5 Project
 * @author Joachim Bauch (jojo@struktur.de)
 */
public interface IStatisticsService {


    /**
     * Return a list of all scopes that currently exist on the server.
     * 
     * @return list of scope names
     */
    public Set<String> getScopes();

    /**
     * Return a list of all scopes that currently exist on the server below a current path.
     * 
     * @param path
     *            Path to start looking for scopes.
     * @return list of scope names
     * @throws ScopeNotFoundException
     *             if the path on the server doesn't exist
     */
    public Set<String> getScopes(String path) throws ScopeNotFoundException;

  

   
}
