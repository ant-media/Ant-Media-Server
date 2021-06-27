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

import java.beans.ConstructorProperties;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.lang3.RandomStringUtils;
import org.red5.server.api.IClient;
import org.red5.server.api.IConnection;
import org.red5.server.api.event.IEvent;
import org.red5.server.api.listeners.IConnectionListener;
import org.red5.server.api.scope.IBasicScope;
import org.red5.server.api.scope.IBroadcastScope;
import org.red5.server.api.scope.IScope;
import org.red5.server.scope.Scope;
import org.red5.server.so.SharedObjectScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base abstract class for connections. Adds connection specific functionality like work with clients to AttributeStore.
 */
public abstract class BaseConnection extends AttributeStore implements IConnection {

    private static final Logger log = LoggerFactory.getLogger(BaseConnection.class);

    /**
     * Connection type
     */
    protected final String type;

    /**
     * Connection host
     */
    protected volatile String host;

    /**
     * Connection remote address
     */
    protected volatile String remoteAddress;

    /**
     * Connection remote addresses
     */
    protected volatile List<String> remoteAddresses;

    /**
     * Remote port
     */
    protected volatile int remotePort;

    /**
     * Path of scope client connected to
     */
    protected volatile String path;

    /**
     * Connection session identifier
     */
    protected final String sessionId;

    /**
     * Number of read messages
     */
    protected AtomicLong readMessages = new AtomicLong(0);

    /**
     * Number of written messages
     */
    protected AtomicLong writtenMessages = new AtomicLong(0);

    /**
     * Number of dropped messages
     */
    protected AtomicLong droppedMessages = new AtomicLong(0);

    /**
     * Connection params passed from client with NetConnection.connect call
     */
    @SuppressWarnings("all")
    protected volatile Map<String, Object> params = null;

    /**
     * Client bound to connection
     */
    protected volatile IClient client;

    /**
     * Scope to which this connection belongs
     */
    protected transient volatile Scope scope;

    /**
     * Set of basic scopes. The scopes may be of shared object or broadcast stream type.
     */
    protected transient CopyOnWriteArraySet<IBasicScope> basicScopes = new CopyOnWriteArraySet<IBasicScope>();

    /**
     * Is the connection closed?
     */
    private volatile boolean closed;

    /**
     * Listeners
     */
    protected transient CopyOnWriteArrayList<IConnectionListener> connectionListeners = new CopyOnWriteArrayList<IConnectionListener>();

    /**
     * Used to protect mulit-threaded operations on write
     */
    private final transient Semaphore writeLock = new Semaphore(1, true);

    // Support for stream ids
    private transient ThreadLocal<Number> streamLocal = new ThreadLocal<Number>();

    /** {@inheritDoc} */
    public Number getStreamId() {
        return streamLocal.get();
    }

    /** {@inheritDoc} */
    public void setStreamId(Number id) {
        streamLocal.set(id);
    }

    /**
     * Creates a new persistent base connection
     */
    @ConstructorProperties(value = { "persistent" })
    public BaseConnection() {
        this(PERSISTENT);
    }

    /**
     * Creates a new base connection with the given type.
     *
     * @param type
     *            Connection type
     */
    @ConstructorProperties({ "type" })
    public BaseConnection(String type) {
        log.debug("New BaseConnection - type: {}", type);
        this.type = type;
        this.sessionId = RandomStringUtils.randomAlphanumeric(13).toUpperCase();
        log.debug("Generated session id: {}", sessionId);
    }

    /**
     * Creates a new base connection with the given parameters.
     *
     * @param type
     *            Connection type
     * @param host
     *            Host
     * @param remoteAddress
     *            Remote address
     * @param remotePort
     *            Remote port
     * @param path
     *            Scope path on server
     * @param sessionId
     *            Session id
     * @param params
     *            Params passed from client
     */
    @ConstructorProperties({ "type", "host", "remoteAddress", "remotePort", "path", "sessionId" })
    public BaseConnection(String type, String host, String remoteAddress, int remotePort, String path, String sessionId, Map<String, Object> params) {
        log.debug("New BaseConnection - type: {} host: {} remoteAddress: {} remotePort: {} path: {} sessionId: {}", new Object[] { type, host, remoteAddress, remotePort, path, sessionId });
        log.debug("Params: {}", params);
        this.type = type;
        this.host = host;
        this.remoteAddress = remoteAddress;
        this.remoteAddresses = new ArrayList<String>(1);
        this.remoteAddresses.add(remoteAddress);
        this.remoteAddresses = Collections.unmodifiableList(this.remoteAddresses);
        this.remotePort = remotePort;
        this.path = path;
        this.sessionId = sessionId;
        this.params = params;
        log.debug("Generated session id: {}", sessionId);
    }

    /** {@inheritDoc} */
    public void addListener(IConnectionListener listener) {
        this.connectionListeners.add(listener);
    }

    /** {@inheritDoc} */
    public void removeListener(IConnectionListener listener) {
        this.connectionListeners.remove(listener);
    }

    /**
     * @return lock for changing state operations
     */
    public Semaphore getLock() {
        return writeLock;
    }

    /**
     * Initializes client
     * 
     * @param client
     *            Client bound to connection
     */
    public void initialize(IClient client) {
        if (log.isDebugEnabled()) {
            log.debug("initialize - client: {}", client);
        }
        if (this.client != null && this.client instanceof Client && !this.client.equals(client)) {
            // unregister old client
            if (log.isTraceEnabled()) {
                log.trace("Unregistering previous client: {}", this.client);
            }
            ((Client) this.client).unregister(this, false);
        }
        this.client = client;
        if (this.client instanceof Client && !((Client) this.client).isRegistered(this)) {
            // register new client
            if (log.isTraceEnabled()) {
                log.trace("Registering client: {}", this.client);
            }
            ((Client) this.client).register(this);
        }
    }

    /**
     *
     * @return type
     */
    public String getType() {
        return type;
    }

    /**
     *
     * @return host
     */
    public String getHost() {
        return host;
    }

    /**
     *
     * @return remote address
     */
    public String getRemoteAddress() {
        return remoteAddress;
    }

    /**
     * @return remote address
     */
    public List<String> getRemoteAddresses() {
        return remoteAddresses;
    }

    /**
     *
     * @return remote port
     */
    public int getRemotePort() {
        return remotePort;
    }

    /**
     *
     * @return path
     */
    public String getPath() {
        return path;
    }

    /**
     *
     * @return session id
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * Return connection parameters
     * 
     * @return connection parameters
     */
    public Map<String, Object> getConnectParams() {
        return Collections.unmodifiableMap(params);
    }

    /** {@inheritDoc} */
    public void setClient(IClient client) {
        this.client = client;
    }

    /** {@inheritDoc} */
    public IClient getClient() {
        return client;
    }

    /**
     * Check whether connection is alive
     * 
     * @return true if connection is bound to scope, false otherwise
     */
    public boolean isConnected() {
        //log.debug("Connected: {}", (scope != null));
        return scope != null;
    }

    /**
     * Connect to another scope on server
     * 
     * @param newScope
     *            New scope
     * @return true on success, false otherwise
     */
    public boolean connect(IScope newScope) {
        return connect(newScope, null);
    }

    /**
     * Connect to another scope on server with given parameters
     * 
     * @param newScope
     *            New scope
     * @param params
     *            Parameters to connect with
     * @return true on success, false otherwise
     */
    public boolean connect(IScope newScope, Object[] params) {
        if (log.isDebugEnabled()) {
            log.debug("Connect Params: {}", params);
            if (params != null) {
                for (Object e : params) {
                    log.debug("Param: {}", e);
                }
            }
        }
        scope = (Scope) newScope;
        return scope.connect(this, params);
    }

    /**
     * Return the current scope.
     * 
     * @return scope
     */
    public IScope getScope() {
        return scope;
    }

    /**
     * Closes connection
     */
    public void close() {
        if (closed) {
            log.debug("Already closed, nothing to do");
            return;
        }
        closed = true;
        if (scope != null) {
            log.debug("Close, disconnect from scope, and children");
            try {
                // unregister all child scopes first
                for (IBasicScope basicScope : basicScopes) {
                    unregisterBasicScope(basicScope);
                }
            } catch (Exception err) {
                log.error("Error while unregistering basic scopes", err);
            }
            // disconnect
            if (scope != null) {
                try {
                    scope.disconnect(this);
                } catch (Exception err) {
                    log.error("Error while disconnecting from scope: {}. {}", scope, err);
                }
                scope = null;
            }
        }
        // unregister client
        if (client != null && client instanceof Client) {
            ((Client) client).unregister(this);
        }
        // alert our listeners
        if (connectionListeners != null) {
            for (IConnectionListener listener : connectionListeners) {
                listener.notifyDisconnected(this);
            }
            connectionListeners.clear();
            connectionListeners = null;
        }
    }

    /**
     * Notified on event
     * 
     * @param event
     *            Event
     */
    public void notifyEvent(IEvent event) {
        log.debug("Event notify was not handled: {}", event);
    }

    /**
     * Dispatches event
     * 
     * @param event
     *            Event
     */
    public void dispatchEvent(IEvent event) {
        log.debug("Event notify was not dispatched: {}", event);
    }

    /**
     * Handles event
     * 
     * @param event
     *            Event
     * @return true if associated scope was able to handle event, false otherwise
     */
    public boolean handleEvent(IEvent event) {
        return getScope().handleEvent(event);
    }

    /**
     *
     * @return basic scopes
     */
    public Iterator<IBasicScope> getBasicScopes() {
        return basicScopes.iterator();
    }

    /**
     * Registers basic scope
     * 
     * @param basicScope
     *            Basic scope to register
     */
    public void registerBasicScope(IBroadcastScope basicScope) {
        basicScopes.add(basicScope);
        basicScope.addEventListener(this);
    }

    /**
     * Registers basic scope
     * 
     * @param basicScope
     *            Basic scope to register
     */
    public void registerBasicScope(SharedObjectScope basicScope) {
        basicScopes.add(basicScope);
        basicScope.addEventListener(this);
    }

    /**
     * Unregister basic scope
     *
     * @param basicScope
     *            Unregister basic scope
     */
    public void unregisterBasicScope(IBasicScope basicScope) {
        if (basicScope instanceof IBroadcastScope || basicScope instanceof SharedObjectScope) {
            basicScopes.remove(basicScope);
            basicScope.removeEventListener(this);
        }
    }

    /**
     *
     * @return bytes read
     */
    public abstract long getReadBytes();

    /**
     *
     * @return bytes written
     */
    public abstract long getWrittenBytes();

    /**
     *
     * @return messages read
     */
    public long getReadMessages() {
        return readMessages.get();
    }

    /**
     *
     * @return messages written
     */
    public long getWrittenMessages() {
        return writtenMessages.get();
    }

    /**
     *
     * @return dropped messages
     */
    public long getDroppedMessages() {
        return droppedMessages.get();
    }

    /**
     * Returns whether or not the reader is idle.
     * 
     * @return queued messages
     */
    public boolean isReaderIdle() {
        return false;
    }

    /**
     * Returns whether or not the writer is idle.
     * 
     * @return queued messages
     */
    public boolean isWriterIdle() {
        return false;
    }

    /**
     * Returns whether or not a connection is closed.
     * 
     * @return true if closed
     */
    public boolean isClosed() {
        return closed;
    }

    /**
     * Count of outgoing messages not yet written.
     * 
     * @return pending messages
     */
    public long getPendingMessages() {
        return 0;
    }

    /**
     * Count of outgoing video messages not yet written.
     * 
     * @param streamId
     *            the id you want to know about
     * @return pending messages for this streamId
     */
    public long getPendingVideoMessages(Number streamId) {
        return 0;
    }

    /** {@inheritDoc} */
    public long getClientBytesRead() {
        return 0;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = prime * sessionId.hashCode();
        return result;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        return sessionId.equals(((BaseConnection) obj).getSessionId());
    }

}
