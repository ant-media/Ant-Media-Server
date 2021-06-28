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

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;

import org.red5.server.api.IConnection;
import org.red5.server.api.IServer;
import org.red5.server.api.listeners.IConnectionListener;
import org.red5.server.api.listeners.IScopeListener;
import org.red5.server.api.scheduling.IScheduledJob;
import org.red5.server.api.scheduling.ISchedulingService;
import org.red5.server.api.scope.IGlobalScope;
import org.red5.server.api.scope.IScope;
import org.red5.server.scheduling.QuartzSchedulingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.style.ToStringCreator;

/**
 * Red5 server core class implementation.
 */
public class Server implements IServer, ApplicationContextAware, InitializingBean, DisposableBean {

    protected static Logger log = LoggerFactory.getLogger(Server.class);

    /**
     * Service used to provide notifications.
     */
    private static QuartzSchedulingService schedulingService;

    /**
     * List of global scopes
     */
    protected ConcurrentMap<String, IGlobalScope> globals = new ConcurrentHashMap<String, IGlobalScope>(1, 0.9f, 1);

    /**
     * Mappings
     */
    protected ConcurrentMap<String, String> mapping = new ConcurrentHashMap<String, String>(32, 0.9f, 8);

    /**
     * Spring application context
     */
    protected ApplicationContext applicationContext;

    /**
     * Constant for slash
     */
    protected static final String SLASH = "/";

    /**
     * Constant for empty string
     */
    protected static final String EMPTY = "";

    public Set<IScopeListener> scopeListeners = new CopyOnWriteArraySet<IScopeListener>();

    public Set<IConnectionListener> connectionListeners = new CopyOnWriteArraySet<IConnectionListener>();

    /**
     * Setter for Spring application context
     * 
     * @param applicationContext
     *            Application context
     */
    public void setApplicationContext(ApplicationContext applicationContext) {
        log.debug("Setting application context");
        this.applicationContext = applicationContext;
    }

    /**
     * Initialization section.
     */
    public void afterPropertiesSet() throws Exception {
        Server.schedulingService = (QuartzSchedulingService) applicationContext.getBean("schedulingService");
    }

    /**
     * Destruction section.
     */
    public void destroy() throws Exception {
    }

    /**
     * Return scope key. Scope key consists of host name concatenated with context path by slash symbol
     * 
     * @param hostName
     *            Host name
     * @param contextPath
     *            Context path
     * @return Scope key as string
     */
    protected String getKey(String hostName, String contextPath) {
        return String.format("%s/%s", (hostName == null ? EMPTY : hostName), (contextPath == null ? EMPTY : contextPath));
    }

    /**
     * Does global scope lookup for host name and context path
     * 
     * @param hostName
     *            Host name
     * @param contextPath
     *            Context path
     * @return Global scope
     */
    public IGlobalScope lookupGlobal(String hostName, String contextPath) {
        log.trace("{}", this);
        log.debug("Lookup global scope - host name: {} context path: {}", hostName, contextPath);
        // Init mappings key
        String key = getKey(hostName, contextPath);
        // If context path contains slashes get complex key and look for it in mappings
        while (contextPath.indexOf(SLASH) != -1) {
            key = getKey(hostName, contextPath);
            log.trace("Check: {}", key);
            String globalName = mapping.get(key);
            if (globalName != null) {
                return getGlobal(globalName);
            }
            final int slashIndex = contextPath.lastIndexOf(SLASH);
            // Context path is substring from the beginning and till last slash index
            contextPath = contextPath.substring(0, slashIndex);
        }
        // Get global scope key
        key = getKey(hostName, contextPath);
        log.trace("Check host and path: {}", key);
        // Look up for global scope switching keys if still not found
        String globalName = mapping.get(key);
        if (globalName != null) {
            return getGlobal(globalName);
        }
        key = getKey(EMPTY, contextPath);
        log.trace("Check wildcard host with path: {}", key);
        globalName = mapping.get(key);
        if (globalName != null) {
            return getGlobal(globalName);
        }
        key = getKey(hostName, EMPTY);
        log.trace("Check host with no path: {}", key);
        globalName = mapping.get(key);
        if (globalName != null) {
            return getGlobal(globalName);
        }
        key = getKey(EMPTY, EMPTY);
        log.trace("Check default host, default path: {}", key);
        return getGlobal(mapping.get(key));
    }

    /**
     * Return global scope by name
     * 
     * @param name
     *            Global scope name
     * @return Global scope
     */
    public IGlobalScope getGlobal(String name) {
        if (name == null) {
            return null;
        }
        return globals.get(name);
    }

    /**
     * Register global scope
     * 
     * @param scope
     *            Global scope to register
     */
    public void registerGlobal(IGlobalScope scope) {
        log.trace("Registering global scope: {}", scope.getName(), scope);
        globals.put(scope.getName(), scope);
    }

    /**
     * Map key (host + / + context path) and global scope name
     * 
     * @param hostName
     *            Host name
     * @param contextPath
     *            Context path
     * @param globalName
     *            Global scope name
     * @return true if mapping was added, false if already exist
     */
    public boolean addMapping(String hostName, String contextPath, String globalName) {
        log.info("Add mapping global: {} host: {} context: {}", new Object[] { globalName, hostName, contextPath });
        final String key = getKey(hostName, contextPath);
        log.debug("Add mapping: {} => {}", key, globalName);
        return (mapping.putIfAbsent(key, globalName) == null);
    }

    /**
     * Remove mapping with given key
     * 
     * @param hostName
     *            Host name
     * @param contextPath
     *            Context path
     * @return true if mapping was removed, false if key doesn't exist
     */
    public boolean removeMapping(String hostName, String contextPath) {
        log.info("Remove mapping host: {} context: {}", hostName, contextPath);
        final String key = getKey(hostName, contextPath);
        log.debug("Remove mapping: {}", key);
        return (mapping.remove(key) != null);
    }

    /**
     * Remove all mappings with given context path
     * 
     * @param contextPath
     *            Context path
     * @return true if mapping was removed, false if key doesn't exist
     */
    public boolean removeMapping(String contextPath) {
        log.info("Remove mapping context: {}", contextPath);
        final String key = getKey("", contextPath);
        log.debug("Remove mapping: {}", key);
        return (mapping.remove(key) != null);
    }

    /**
     * Return mapping
     * 
     * @return Map of "scope key / scope name" pairs
     */
    public Map<String, String> getMappingTable() {
        return mapping;
    }

    /**
     * Return global scope names set iterator
     * 
     * @return Iterator
     */
    public Iterator<String> getGlobalNames() {
        return globals.keySet().iterator();
    }

    /**
     * Return global scopes set iterator
     * 
     * @return Iterator
     */
    public Iterator<IGlobalScope> getGlobalScopes() {
        return globals.values().iterator();
    }

    /**
     * String representation of server
     * 
     * @return String representation of server
     */
    @Override
    public String toString() {
        return new ToStringCreator(this).append(mapping).toString();
    }

    /** {@inheritDoc} */
    public void addListener(IScopeListener listener) {
        scopeListeners.add(listener);
    }

    /** {@inheritDoc} */
    public void addListener(IConnectionListener listener) {
        connectionListeners.add(listener);
    }

    /** {@inheritDoc} */
    public void removeListener(IScopeListener listener) {
        scopeListeners.remove(listener);
    }

    /** {@inheritDoc} */
    public void removeListener(IConnectionListener listener) {
        connectionListeners.remove(listener);
    }

    /**
     * Notify listeners about a newly created scope.
     * 
     * @param scope
     *            the scope that was created
     */
    public void notifyScopeCreated(IScope scope) {
        schedulingService.addScheduledOnceJob(10, new ScopeCreatedJob(scope));
    }

    /**
     * Notify listeners that a scope was removed.
     * 
     * @param scope
     *            the scope that was removed
     */
    public void notifyScopeRemoved(IScope scope) {
        schedulingService.addScheduledOnceJob(10, new ScopeRemovedJob(scope));
    }

    /**
     * Notify listeners that a new connection was established.
     * 
     * @param conn
     *            the new connection
     */
    public void notifyConnected(IConnection conn) {
        schedulingService.addScheduledOnceJob(10, new ConnectedJob(conn));
    }

    /**
     * Notify listeners that a connection was disconnected.
     * 
     * @param conn
     *            the disconnected connection
     */
    public void notifyDisconnected(final IConnection conn) {
        schedulingService.addScheduledOnceJob(10, new DisconnectedJob(conn));
    }

    /**
     * Used to indicate a scope was created.
     */
    private final class ScopeCreatedJob implements IScheduledJob {

        private IScope scope;

        ScopeCreatedJob(IScope scope) {
            this.scope = scope;
        }

        public void execute(ISchedulingService service) {
            for (IScopeListener listener : scopeListeners) {
                listener.notifyScopeCreated(scope);
            }
        }

    }

    /**
     * Used to indicate a scope was removed.
     */
    private final class ScopeRemovedJob implements IScheduledJob {

        private IScope scope;

        ScopeRemovedJob(IScope scope) {
            this.scope = scope;
        }

        public void execute(ISchedulingService service) {
            for (IScopeListener listener : scopeListeners) {
                listener.notifyScopeRemoved(scope);
            }
        }

    }

    private final class ConnectedJob implements IScheduledJob {

        private IConnection conn;

        ConnectedJob(IConnection conn) {
            this.conn = conn;
        }

        public void execute(ISchedulingService service) {
            for (IConnectionListener listener : connectionListeners) {
                listener.notifyConnected(conn);
            }
        }

    }

    private final class DisconnectedJob implements IScheduledJob {

        private IConnection conn;

        DisconnectedJob(IConnection conn) {
            this.conn = conn;
        }

        public void execute(ISchedulingService service) {
            for (IConnectionListener listener : connectionListeners) {
                listener.notifyDisconnected(conn);
            }
        }

    }

}
