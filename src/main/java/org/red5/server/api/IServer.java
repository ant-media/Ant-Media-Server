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

import java.util.Iterator;
import java.util.Map;

import org.red5.server.api.listeners.IConnectionListener;
import org.red5.server.api.listeners.IScopeListener;
import org.red5.server.api.scope.IGlobalScope;

/**
 * The interface that represents the Red5 server.
 * 
 * @author The Red5 Project
 * @author Luke Hubbard (luke@codegent.com)
 * 
 */
public interface IServer {
    /**
     * Server ID
     */
    public static final String ID = "red5.server";

    /**
     * Get the global scope with given name.
     * 
     * @param name
     *            Name of the global scope
     * @return the global scope
     */
    public IGlobalScope getGlobal(String name);

    /**
     * Register a global scope.
     * 
     * @param scope
     *            The global scope to register
     */
    public void registerGlobal(IGlobalScope scope);

    /**
     * Lookup the global scope for a host.
     * 
     * @param hostName
     *            The name of the host
     * @param contextPath
     *            The path in the host
     * @return The found global scope or
     * 
     *         <pre>
     * null
     * </pre>
     */
    public IGlobalScope lookupGlobal(String hostName, String contextPath);

    /**
     * Map a virtual hostname and a path to the name of a global scope.
     * 
     * @param hostName
     *            The name of the host to map
     * @param contextPath
     *            The path to map
     * @param globalName
     *            The name of the global scope to map to
     * @return <pre>
     * true
     * </pre>
     * 
     *         if the name was mapped, otherwise
     * 
     *         <pre>
     * false
     * </pre>
     */
    public boolean addMapping(String hostName, String contextPath, String globalName);

    /**
     * Unregister a previously mapped global scope.
     * 
     * @param hostName
     *            The name of the host to unmap
     * @param contextPath
     *            The path for this host to unmap
     * @return <pre>
     * true
     * </pre>
     * 
     *         if the global scope was unmapped, otherwise
     * 
     *         <pre>
     * false
     * </pre>
     */
    public boolean removeMapping(String hostName, String contextPath);

    /**
     * Query informations about the global scope mappings.
     * 
     * @return Map containing informations about the mappings
     */
    public Map<String, String> getMappingTable();

    /**
     * Get list of global scope names.
     * 
     * @return Iterator for names of global scopes
     */
    public Iterator<String> getGlobalNames();

    /**
     * Get list of global scopes.
     * 
     * @return Iterator for global scopes objects
     */
    public Iterator<IGlobalScope> getGlobalScopes();

    /**
     * Add listener to get notified about scope events.
     * 
     * @param listener
     *            the listener to add
     */
    public void addListener(IScopeListener listener);

    /**
     * Add listener to get notified about connection events.
     * 
     * @param listener
     *            the listener to add
     */
    public void addListener(IConnectionListener listener);

    /**
     * Remove listener that got notified about scope events.
     * 
     * @param listener
     *            the listener to remove
     */
    public void removeListener(IScopeListener listener);

    /**
     * Remove listener that got notified about connection events.
     * 
     * @param listener
     *            the listener to remove
     */
    public void removeListener(IConnectionListener listener);

}
