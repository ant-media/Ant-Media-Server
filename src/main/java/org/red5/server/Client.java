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
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.management.openmbean.CompositeData;

import org.red5.server.api.IClient;
import org.red5.server.api.IConnection;
import org.red5.server.api.Red5;
import org.red5.server.api.persistence.IPersistable;
import org.red5.server.api.scope.IScope;
import org.red5.server.stream.bandwidth.ClientServerDetection;
import org.red5.server.stream.bandwidth.ServerClientDetection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client is an abstraction representing user connected to Red5 application. Clients are tied to connections and registered in ClientRegistry
 */
public class Client extends AttributeStore implements IClient {

    protected static Logger log = LoggerFactory.getLogger(Client.class);

    /**
     * Name of connection attribute holding the permissions.
     */
    protected static final String PERMISSIONS = IPersistable.TRANSIENT_PREFIX + "_red5_permissions";

    /**
     * Client registry where Client is registered
     */
    protected transient WeakReference<ClientRegistry> registry;

    /**
     * Connections this client is associated with.
     */
    protected transient CopyOnWriteArraySet<IConnection> connections = new CopyOnWriteArraySet<IConnection>();

    /**
     * Creation time as Timestamp
     */
    protected final long creationTime;

    /**
     * Clients identifier
     */
    protected final String id;

    /**
     * Whether or not the bandwidth has been checked.
     */
    protected boolean bandwidthChecked;

    /**
     * Disconnected state.
     */
    protected AtomicBoolean disconnected = new AtomicBoolean(false);

    /**
     * Creates client, sets creation time and registers it in ClientRegistry.
     *
     * @param id
     *            Client id
     * @param registry
     *            ClientRegistry
     */
    @ConstructorProperties({ "id", "registry" })
    public Client(String id, ClientRegistry registry) {
        super();
        if (id != null) {
            this.id = id;
        } else {
            this.id = registry.nextId();
        }
        this.creationTime = System.currentTimeMillis();
        // use a weak reference to prevent any hard-links to the registry
        this.registry = new WeakReference<ClientRegistry>(registry);
    }

    /**
     * Creates client, sets creation time and registers it in ClientRegistry.
     *
     * @param id
     *            Client id
     * @param creationTime
     *            Creation time
     * @param registry
     *            ClientRegistry
     */
    @ConstructorProperties({ "id", "creationTime", "registry" })
    public Client(String id, Long creationTime, ClientRegistry registry) {
        super();
        if (id != null) {
            this.id = id;
        } else {
            this.id = registry.nextId();
        }
        if (creationTime != null) {
            this.creationTime = creationTime;
        } else {
            this.creationTime = System.currentTimeMillis();
        }
        // use a weak reference to prevent any hard-links to the registry
        this.registry = new WeakReference<ClientRegistry>(registry);
    }

    /**
     * Disconnects client from Red5 application
     */
    public void disconnect() {
        if (disconnected.compareAndSet(false, true)) {
            log.debug("Disconnect - id: {}", id);
            if (connections != null && !connections.isEmpty()) {
                log.debug("Closing {} scope connections", connections.size());
                // close all connections held to Red5 by client
                for (IConnection con : getConnections()) {
                    try {
                        con.close();
                    } catch (Exception e) {
                        // closing a connection calls into application code, so exception possible
                        log.error("Unexpected exception closing connection {}", e);
                    }
                }
            } else {
                log.debug("Connection map is empty or null");
            }
            // unregister client
            removeInstance();
        }
    }

    /**
     * Return set of connections for this client
     *
     * @return Set of connections
     */
    public Set<IConnection> getConnections() {
        return Collections.unmodifiableSet(connections);
    }

    /**
     * Return client connections to given scope
     *
     * @param scope
     *            Scope
     * @return Set of connections for that scope
     */
    public Set<IConnection> getConnections(IScope scope) {
        if (scope == null) {
            return getConnections();
        }
        Set<IClient> scopeClients = scope.getClients();
        if (scopeClients.contains(this)) {
            for (IClient cli : scopeClients) {
                if (this.equals(cli)) {
                    return cli.getConnections();
                }
            }
        }
        return Collections.emptySet();
    }

    /**
     * Returns the time at which the client was created.
     * 
     * @return creation time
     */
    public long getCreationTime() {
        return creationTime;
    }

    /**
     * Returns the client id.
     * 
     * @return client id
     */
    public String getId() {
        return id;
    }

    /**
     *
     * @return scopes on this client
     */
    public Collection<IScope> getScopes() {
        Set<IScope> scopes = new HashSet<IScope>();
        for (IConnection conn : connections) {
            scopes.add(conn.getScope());
        }
        return scopes;
    }

    /**
     * Iterate through the scopes and their attributes. Used by JMX
     *
     * @return list of scope attributes
     */
    public List<String> iterateScopeNameList() {
        log.debug("iterateScopeNameList called");
        Collection<IScope> scopes = getScopes();
        log.debug("Scopes: {}", scopes.size());
        List<String> scopeNames = new ArrayList<String>(scopes.size());
        for (IScope scope : scopes) {
            log.debug("Client scope: {}", scope);
            scopeNames.add(scope.getName());
            if (log.isDebugEnabled()) {
                for (Map.Entry<String, Object> entry : scope.getAttributes().entrySet()) {
                    log.debug("Client scope attr: {} = {}", entry.getKey(), entry.getValue());
                }
            }
        }
        return scopeNames;
    }

    /**
     * Returns registration status of given connection.
     * 
     * @param conn
     *            connection
     * @return true if registered and false otherwise
     */
    public boolean isRegistered(IConnection conn) {
        return connections.contains(conn);
    }

    /**
     * Associate connection with client
     * 
     * @param conn
     *            Connection object
     */
    protected void register(IConnection conn) {
        if (log.isDebugEnabled()) {
            if (conn == null) {
                log.debug("Register null connection, client id: {}", id);
            } else {
                log.debug("Register connection ({}:{}) client id: {}", conn.getRemoteAddress(), conn.getRemotePort(), id);
            }
        }
        if (conn != null) {
            IScope scope = conn.getScope();
            if (scope != null) {
                log.debug("Registering for scope: {}", scope);
                connections.add(conn);
            } else {
                log.warn("Clients scope is null. Id: {}", id);
            }
        } else {
            log.warn("Clients connection is null. Id: {}", id);
        }
    }

    /**
     * Removes client-connection association for given connection
     * 
     * @param conn
     *            Connection object
     */
    protected void unregister(IConnection conn) {
        unregister(conn, true);
    }

    /**
     * Removes client-connection association for given connection
     * 
     * @param conn
     *            Connection object
     * @param deleteIfNoConns
     *            Whether to delete this client if it no longer has any connections
     */
    protected void unregister(IConnection conn, boolean deleteIfNoConns) {
        log.debug("Unregister connection ({}:{}) client id: {}", conn.getRemoteAddress(), conn.getRemotePort(), id);
        // remove connection from connected scopes list
        connections.remove(conn);
        // If client is not connected to any scope any longer then remove
        if (deleteIfNoConns && connections.isEmpty()) {
            // TODO DW dangerous the way this is called from BaseConnection.initialize(). Could we unexpectedly pop a Client out of the registry?
            removeInstance();
        }
    }

    /** {@inheritDoc} */
    public boolean isBandwidthChecked() {
        return bandwidthChecked;
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    public Collection<String> getPermissions(IConnection conn) {
        Collection<String> result = (Collection<String>) conn.getAttribute(PERMISSIONS);
        if (result == null) {
            result = Collections.emptySet();
        }
        return result;
    }

    /** {@inheritDoc} */
    public boolean hasPermission(IConnection conn, String permissionName) {
        final Collection<String> permissions = getPermissions(conn);
        return permissions.contains(permissionName);
    }

    /** {@inheritDoc} */
    public void setPermissions(IConnection conn, Collection<String> permissions) {
        if (permissions == null) {
            conn.removeAttribute(PERMISSIONS);
        } else {
            conn.setAttribute(PERMISSIONS, permissions);
        }
    }

    /** {@inheritDoc} */
    public void checkBandwidth() {
        log.debug("Check bandwidth");
        bandwidthChecked = true;
        //do something to check the bandwidth, Dan what do you think?
        ServerClientDetection detection = new ServerClientDetection();
        detection.checkBandwidth(Red5.getConnectionLocal());
    }

    /** {@inheritDoc} */
    public Map<String, Object> checkBandwidthUp(Object[] params) {
        if (log.isDebugEnabled()) {
            log.debug("Check bandwidth: {}", Arrays.toString(params));
        }
        bandwidthChecked = true;
        //do something to check the bandwidth, Dan what do you think?
        ClientServerDetection detection = new ClientServerDetection();
        // if dynamic bw is turned on, we switch to a higher or lower
        return detection.checkBandwidth(params);
    }

    /**
     * Allows for reconstruction via CompositeData.
     *
     * @param cd
     *            composite data
     * @return Client class instance
     */
    public static Client from(CompositeData cd) {
        Client instance = null;
        if (cd.containsKey("id")) {
            String id = (String) cd.get("id");
            instance = new Client(id, (Long) cd.get("creationTime"), null);
            instance.setAttribute(PERMISSIONS, cd.get(PERMISSIONS));
        }
        if (cd.containsKey("attributes")) {
            AttributeStore attrs = (AttributeStore) cd.get("attributes");
            instance.setAttributes(attrs);
        }
        return instance;
    }

    /**
     * Removes this instance from the client registry.
     */
    private void removeInstance() {
        // unregister client
        ClientRegistry ref = registry.get();
        if (ref != null) {
            ref.removeClient(this);
        } else {
            log.warn("Client registry reference was not accessable, removal failed");
            // TODO: attempt to lookup the registry via the global.clientRegistry
        }
    }

    @Override
    public int hashCode() {
        if (id == null) {
            return -1;
        }
        return id.hashCode();
    }

    /**
     * Check clients equality by id
     *
     * @param obj
     *            Object to check against
     * @return true if clients ids are the same, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Client) {
            return ((Client) obj).getId().equals(id);
        }
        return false;
    }

    /**
     *
     * @return string representation of client
     */
    @Override
    public String toString() {
        return "Client: " + id;
    }

}
