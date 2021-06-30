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

import org.red5.server.exception.ClientNotFoundException;
import org.red5.server.exception.ClientRejectedException;

/**
 * Provides a registry of client objects. You can lookup a client by its client id / session id using lookupClient method. This interface implementations also create new client objects from given params, usually passed from client-side Flex/Flash application upon initial connection.
 * 
 * @author The Red5 Project
 * @author Luke Hubbard (luke@codegent.com)
 */
public interface IClientRegistry {

    /**
     * Check if a client with a given id exists.
     * 
     * @param id
     *            the id of the client to check for
     * @return <pre>
     * true
     * </pre>
     * 
     *         if the client exists,
     * 
     *         <pre>
     * false
     * </pre>
     * 
     *         otherwise
     */
    public boolean hasClient(String id);

    /**
     * Create a new client client object from connection params.
     * 
     * @param params
     *            the parameters the client passed during connection
     * @return the new client
     * @throws ClientNotFoundException
     *             no client could be created from the passed parameters
     * @throws ClientRejectedException
     *             the client is not allowed to connect
     */
    public IClient newClient(Object[] params) throws ClientNotFoundException, ClientRejectedException;

    /**
     * Return an existing client from a client id.
     * 
     * @param id
     *            the id of the client to return
     * @return the client object
     * @throws ClientNotFoundException
     *             no client with the passed id exists
     */
    public IClient lookupClient(String id) throws ClientNotFoundException;

    /**
     * Adds a client to the registry.
     * 
     * @param client
     *            client
     */
    public void addClient(IClient client);

}