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

import org.red5.server.api.IClient;
import org.red5.server.api.IClientRegistry;
import org.red5.server.api.IConnection;
import org.red5.server.api.IContext;
import org.red5.server.api.event.IEvent;
import org.red5.server.api.scope.IBasicScope;
import org.red5.server.api.scope.IScope;
import org.red5.server.api.scope.IScopeHandler;
import org.red5.server.api.service.IServiceCall;
import org.red5.server.jmx.mxbeans.CoreHandlerMXBean;
import org.red5.server.net.IConnectionManager;
import org.red5.server.net.rtmp.RTMPConnManager;
import org.red5.server.net.rtmp.RTMPConnection;
import org.red5.server.net.rtmpt.RTMPTConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base IScopeHandler implementation
 */
public class CoreHandler implements IScopeHandler, CoreHandlerMXBean {

    protected static Logger log = LoggerFactory.getLogger(CoreHandler.class);

    /** {@inheritDoc} */
    public boolean addChildScope(IBasicScope scope) {
        return true;
    }

    /**
     * Connects client to the scope
     *
     * @param conn
     *            Client connection
     * @param scope
     *            Scope
     * @return true if client was registered within scope, false otherwise
     */
    public boolean connect(IConnection conn, IScope scope) {
        return connect(conn, scope, null);
    }

    /**
     * Connects client to the scope
     *
     * @param conn
     *            Client connection
     * @param scope
     *            Scope
     * @param params
     *            Parameters passed from client side with connect call
     * @return true if client was registered within scope, false otherwise
     */
    public boolean connect(IConnection conn, IScope scope, Object[] params) {
        log.debug("connect - conn: {} scope: {}", conn, scope);
        // this is where we create the Client object that consolidates connections from a single client/FP. 
        // Now for more strangeness, I've only been looking at RTMPConnection derivatives, but it's setup() method
        // seems the only way that the session id is passed in to the newly established connection and this is currently *always* passed in
        // as null. I'm guessing that either the Flash Player passes some kind of unique id to us that is not being used, or that the idea
        // originally was to make our own session id, for example by combining client information with the IP address or something like that.
        boolean connect = false;
        // Get session id
        String id = conn.getSessionId();
        log.trace("Session id: {}", id);
        // Use client registry from scope the client connected to
        IScope connectionScope = conn.getScope();
        log.debug("Connection scope: {}", (connectionScope == null ? "is null" : "not null"));
        // when the scope is null bad things seem to happen, if a null scope is OK then
        // this block will need to be removed - Paul
        if (connectionScope != null) {
            // Get client registry for connection scope
            IClientRegistry clientRegistry = connectionScope.getContext().getClientRegistry();
            log.debug("Client registry: {}", (clientRegistry == null ? "is null" : "not null"));
            if (clientRegistry != null) {
                IClient client = conn.getClient();
                if (client == null) {
                    if (!clientRegistry.hasClient(id)) {
                        if (conn instanceof RTMPTConnection) {
                            log.debug("Creating new client for RTMPT connection");
                            // create a new client using the session id as the client's id
                            client = new Client(id, (ClientRegistry) clientRegistry);
                            clientRegistry.addClient(client);
                            // set the client on the connection
                            conn.setClient(client);
                        } else if (conn instanceof RTMPConnection) {
                            log.debug("Creating new client for RTMP connection");
                            // this is a new connection, create a new client to hold it
                            client = clientRegistry.newClient(params);
                            // set the client on the connection
                            conn.setClient(client);
                        }
                    } else {
                        client = clientRegistry.lookupClient(id);
                        conn.setClient(client);
                    }
                } else {
                    // set the client on the connection
                    conn.setClient(client);
                }
                // add any rtmp connections to the manager
                IConnectionManager<RTMPConnection> connManager = RTMPConnManager.getInstance();
                if (conn instanceof RTMPTConnection) {
                    connManager.setConnection((RTMPTConnection) conn);
                } else if (conn instanceof RTMPConnection) {
                    connManager.setConnection((RTMPConnection) conn);
                } else {
                    log.warn("Connection was not added to manager: {}", conn);
                }
                // assign connection to client
                conn.initialize(client);
                // we could checked for banned clients here
                connect = true;
            } else {
                log.error("No client registry was found, clients cannot be looked-up or created");
            }
        } else {
            log.error("No connection scope was found");
        }
        return connect;
    }

    /** {@inheritDoc} */
    public void disconnect(IConnection conn, IScope scope) {
        // do nothing here
    }

    /** {@inheritDoc} */
    public boolean join(IClient client, IScope scope) {
        return true;
    }

    /** {@inheritDoc} */
    public void leave(IClient client, IScope scope) {
        // do nothing here
    }

    /** {@inheritDoc} */
    public void removeChildScope(IBasicScope scope) {
        // do nothing here
    }

    /**
     * Remote method invocation
     *
     * @param conn
     *            Connection to invoke method on
     * @param call
     *            Service call context
     * @return true on success
     */
    public boolean serviceCall(IConnection conn, IServiceCall call) {
        final IContext context = conn.getScope().getContext();
        if (call.getServiceName() != null) {
            context.getServiceInvoker().invoke(call, context);
        } else {
            context.getServiceInvoker().invoke(call, conn.getScope().getHandler());
        }
        return true;
    }

    /** {@inheritDoc} */
    public boolean start(IScope scope) {
        return true;
    }

    /** {@inheritDoc} */
    public void stop(IScope scope) {
        // do nothing here
    }

    /** {@inheritDoc} */
    public boolean handleEvent(IEvent event) {
        return false;
    }

}
