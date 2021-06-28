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

package org.red5.server.scope;

import java.beans.ConstructorProperties;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.red5.server.api.IConnection;
import org.red5.server.api.event.IEvent;
import org.red5.server.api.event.IEventListener;
import org.red5.server.api.persistence.IPersistenceStore;
import org.red5.server.api.scheduling.IScheduledJob;
import org.red5.server.api.scheduling.ISchedulingService;
import org.red5.server.api.scope.IBasicScope;
import org.red5.server.api.scope.IScope;
import org.red5.server.api.scope.IScopeSecurityHandler;
import org.red5.server.api.scope.ScopeType;
import org.red5.server.util.ScopeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generalizations of one of main Red5 object types, Scope.
 *
 * @see org.red5.server.api.scope.IScope
 * @see org.red5.server.scope.Scope
 */
public abstract class BasicScope implements IBasicScope, Comparable<BasicScope> {

    protected static Logger log = LoggerFactory.getLogger(BasicScope.class);

    /**
     * Scheduled job name for keep alive check
     */
    private String keepAliveJobName;

    /**
     * Parent scope. Scopes can be nested.
     *
     * @see org.red5.server.api.scope.IScope
     */
    protected IScope parent;

    /**
     * Scope type.
     *
     * @see org.red5.server.api.scope.ScopeType
     */
    protected ScopeType type = ScopeType.UNDEFINED;

    /**
     * String identifier for this scope
     */
    protected String name;

    /**
     * Creation timestamp
     */
    protected long creation;

    /**
     * Whether or not to persist attributes
     */
    protected boolean persistent;

    /**
     * Storage for persistable attributes
     */
    protected IPersistenceStore store;

    /**
     * Scope persistence storage type
     */
    protected String persistenceClass;

    /**
     * Set to true to prevent the scope from being freed upon disconnect.
     */
    protected boolean keepOnDisconnect;

    /**
     * Set to amount of time (in seconds) the scope will be kept before being freed, after the last disconnect.
     */
    protected int keepDelay = 0;

    /**
     * List of security handlers
     */
    protected transient CopyOnWriteArraySet<IScopeSecurityHandler> securityHandlers;

    /**
     * List of event listeners
     */
    protected transient CopyOnWriteArraySet<IEventListener> listeners;

    /**
     * Creates unnamed scope
     */
    @ConstructorProperties(value = { "" })
    public BasicScope() {
        this.creation = System.nanoTime();
    }

    /**
     * Constructor for basic scope
     *
     * @param parent
     *            Parent scope
     * @param type
     *            Scope type
     * @param name
     *            Scope name. Used to identify scopes in application, must be unique among scopes of one level
     * @param persistent
     *            Whether scope is persistent
     */
    @ConstructorProperties({ "parent", "type", "name", "persistent" })
    public BasicScope(IScope parent, ScopeType type, String name, boolean persistent) {
        this.parent = parent;
        this.type = type;
        this.name = name;
        this.persistent = persistent;
        this.listeners = new CopyOnWriteArraySet<IEventListener>();
        this.creation = System.nanoTime();
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasParent() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public IScope getParent() {
        return parent;
    }

    /**
     * @return the type
     */
    public ScopeType getType() {
        return type;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the store
     */
    public IPersistenceStore getStore() {
        return store;
    }

    /**
     * {@inheritDoc}
     */
    public int getDepth() {
        return parent.getDepth() + 1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getPath() {
        return parent.getPath() + '/' + parent.getName();
    }

    /**
     * Sets the amount of time to keep the scope available after the last disconnect.
     * 
     * @param keepDelay delay
     */
    public void setKeepDelay(int keepDelay) {
        this.keepDelay = keepDelay;
    }

    /**
     * Validates a scope based on its name and type
     * 
     * @return true if both name and type are valid, false otherwise
     */
    public boolean isValid() {
        // to be valid a scope must have a type set other than undefined and its name will be set
        return (type != null && !type.equals(ScopeType.UNDEFINED) && (name != null && !("").equals(name)));
    }

    /**
     * {@inheritDoc}
     */
    public boolean isConnectionAllowed(IConnection conn) {
        if (log.isDebugEnabled()) {
            log.debug("isConnectionAllowed: {}", conn);
        }
        if (securityHandlers != null) {
            if (log.isDebugEnabled()) {
                log.debug("securityHandlers: {}", securityHandlers);
            }
            // loop through the handlers
            for (IScopeSecurityHandler handler : securityHandlers) {
                // if allowed continue to the next handler
                if (handler.allowed(conn)) {
                    continue;
                } else {
                    // if any handlers deny we return false
                    return false;
                }
            }
        }
        // default is to allow
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isScopeAllowed(IScope scope) {
        if (log.isDebugEnabled()) {
            log.debug("isScopeAllowed: {}", scope);
        }
        if (securityHandlers != null) {
            // loop through the handlers
            for (IScopeSecurityHandler handler : securityHandlers) {
                // if allowed continue to the next handler
                if (handler.allowed(scope)) {
                    continue;
                } else {
                    // if any handlers deny we return false
                    return false;
                }
            }
        }
        // default is to allow
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public void setSecurityHandlers(Set<IScopeSecurityHandler> handlers) {
        if (securityHandlers == null) {
            securityHandlers = new CopyOnWriteArraySet<>();
        }
        // add the specified set of security handlers
        securityHandlers.addAll(handlers);
        if (log.isDebugEnabled()) {
            log.debug("securityHandlers: {}", securityHandlers);
        }
    }

    /**
     * Add event listener to list of notified objects
     * 
     * @param listener Listening object
     * @return true if listener is added and false otherwise
     */
    public boolean addEventListener(IEventListener listener) {
        log.debug("addEventListener - scope: {} {}", getName(), listener);
        return listeners.add(listener);
    }

    /**
     * Remove event listener from list of listeners
     * 
     * @param listener
     *            Listener to remove
     * @return true if listener is removed and false otherwise
     */
    public boolean removeEventListener(IEventListener listener) {
        log.debug("removeEventListener - scope: {} {}", getName(), listener);
        if (log.isTraceEnabled()) {
            log.trace("Listeners - check #1: {}", listeners);
        }
        boolean removed = listeners.remove(listener);
        if (!keepOnDisconnect) {
            if (removed && keepAliveJobName == null) {
                if (ScopeUtils.isRoom(this) && listeners.isEmpty()) {
                    // create job to kill the scope off if no listeners join within the delay
                    ISchedulingService schedulingService = (ISchedulingService) parent.getContext().getBean(ISchedulingService.BEAN_NAME);
                    // by default keep a scope around for a fraction of a second
                    keepAliveJobName = schedulingService.addScheduledOnceJob((keepDelay > 0 ? keepDelay * 1000 : 100), new KeepAliveJob(this));
                }
            }
        } else {
            log.trace("Scope: {} is exempt from removal when empty", getName());
        }
        if (log.isTraceEnabled()) {
            log.trace("Listeners - check #2: {}", listeners);
        }
        return removed;
    }

    /**
     * Return listeners list iterator
     *
     * @return Listeners list iterator
     */
    public Set<IEventListener> getEventListeners() {
        return Collections.unmodifiableSet(listeners);
    }

    /**
     * Returns true if there are event listeners attached to this scope.
     * 
     * @return true if it has listeners; else false.
     */
    public boolean hasEventListeners() {
        return !listeners.isEmpty();
    }

    /**
     * Handles event. To be implemented in subclass realization
     *
     * @param event Event context
     * @return Event handling result
     */
    public boolean handleEvent(IEvent event) {
        return false;
    }

    /**
     * Notifies listeners on event. Current implementation is empty. To be implemented in subclass realization
     * 
     * @param event Event to broadcast
     */
    public void notifyEvent(IEvent event) {

    }

    /**
     * Dispatches event (notifies all listeners)
     *
     * @param event Event to dispatch
     */
    public void dispatchEvent(IEvent event) {
        for (IEventListener listener : listeners) {
            if (event.getSource() == null || event.getSource() != listener) {
                listener.notifyEvent(event);
            }
        }
    }

    /**
     * Hash code is based on the scope's name and type
     * 
     * @return hash code
     */
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        return result;
    }

    /**
     * Equality is based on the scope's name and type.
     * 
     * @param obj
     *            object
     */
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
        BasicScope other = (BasicScope) obj;
        if (hashCode() != other.hashCode()) {
            return false;
        }
        return true;
    }

    public int compareTo(BasicScope that) {
        if (this.equals(that)) {
            return 0;
        }
        return name.compareTo(that.getName());
    }

    /**
     * Keeps the scope alive for a set number of seconds.
     */
    private class KeepAliveJob implements IScheduledJob {

        private IBasicScope scope = null;

        KeepAliveJob(IBasicScope scope) {
            this.scope = scope;
        }

        public void execute(ISchedulingService service) {
            if (listeners.isEmpty()) {
                // delete empty rooms
                log.trace("Removing {} from {}", scope.getName(), parent.getName());
                parent.removeChildScope(scope);
            }
            keepAliveJobName = null;
        }

    }

}
