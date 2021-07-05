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

package org.red5.server.so;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.locks.ReentrantLock;

import org.red5.server.BaseConnection;
import org.red5.server.api.IAttributeStore;
import org.red5.server.api.IContext;
import org.red5.server.api.event.IEvent;
import org.red5.server.api.event.IEventListener;
import org.red5.server.api.persistence.IPersistenceStore;
import org.red5.server.api.scheduling.IScheduledJob;
import org.red5.server.api.scheduling.ISchedulingService;
import org.red5.server.api.scope.IScope;
import org.red5.server.api.scope.ScopeType;
import org.red5.server.api.so.ISharedObject;
import org.red5.server.api.so.ISharedObjectListener;
import org.red5.server.api.so.ISharedObjectSecurity;
import org.red5.server.api.so.ISharedObjectSecurityService;
import org.red5.server.api.statistics.ISharedObjectStatistics;
import org.red5.server.net.rtmp.status.StatusCodes;
import org.red5.server.scheduling.QuartzSchedulingService;
import org.red5.server.scope.BasicScope;
import org.red5.server.service.ReflectionUtils;
import org.red5.server.util.ScopeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Special scope for shared objects
 */
public class SharedObjectScope extends BasicScope implements ISharedObject, StatusCodes {

    private Logger log = LoggerFactory.getLogger(SharedObjectScope.class);

    /**
     * Lock to synchronize shared object updates from multiple threads
     */
    private final transient ReentrantLock lock = new ReentrantLock();

    /**
     * Server-side listeners
     */
    private transient CopyOnWriteArraySet<ISharedObjectListener> serverListeners = new CopyOnWriteArraySet<ISharedObjectListener>();

    /**
     * Event handlers
     */
    private transient ConcurrentMap<String, Object> handlers = new ConcurrentHashMap<String, Object>(1, 0.9f, 1);

    /**
     * Security handlers
     */
    private transient CopyOnWriteArraySet<ISharedObjectSecurity> securityHandlers = new CopyOnWriteArraySet<ISharedObjectSecurity>();

    /**
     * Scoped shared object
     */
    protected volatile SharedObject so;

    /**
     * Time to linger before checking for disposal
     */
    private long lingerPeriod = 5000L;

    /**
     * Linger job name
     */
    private String lingerJobName;

    /**
     * Creates shared object with given parent scope, name, persistence flag state and store object
     * 
     * @param parent
     *            Parent scope
     * @param name
     *            Name
     * @param persistent
     *            Persistence flag state
     * @param store
     *            Persistence store
     */
    public SharedObjectScope(IScope parent, String name, boolean persistent, IPersistenceStore store) {
        super(parent, ScopeType.SHARED_OBJECT, name, persistent);
        // create shared object wrapper around the attributes
        String path = parent.getContextPath();
        if ("".equals(path) || path.charAt(0) != '/') {
            path = '/' + path;
        }
        log.trace("Path+name: {}/{}", path, name);
        // Load SO
        so = (SharedObject) store.load(ScopeType.SHARED_OBJECT + path + '/' + name);
        // Create if it doesn't exist
        if (so == null) {
            so = new SharedObject(name, path, persistent, store);
            // Save
            store.save(so);
        } else {
            // set path
            so.setPath(path);
        }
    }

    /** {@inheritDoc} */
    public void registerSharedObjectSecurity(ISharedObjectSecurity handler) {
        securityHandlers.add(handler);
    }

    /** {@inheritDoc} */
    public void unregisterSharedObjectSecurity(ISharedObjectSecurity handler) {
        securityHandlers.remove(handler);
    }

    /** {@inheritDoc} */
    public Set<ISharedObjectSecurity> getSharedObjectSecurity() {
        return Collections.unmodifiableSet(securityHandlers);
    }

    /** {@inheritDoc} */
    @Override
    public IPersistenceStore getStore() {
        return so.getStore();
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return so.getName();
    }

    /** {@inheritDoc} */
    @Override
    public String getPath() {
        return so.getPath();
    }

    public void setPath(String path) {
        so.setPath(path);
    }

    /** {@inheritDoc} */
    public boolean isPersistent() {
        return so.isPersistent();
    }

    /** {@inheritDoc} */
    public void beginUpdate() {
        if (log.isTraceEnabled()) {
            log.trace("beginUpdate - locked: {} owner: {}", lock.isLocked(), lock.isHeldByCurrentThread());
        }
        // make sure only one thread can update the SO
        lock.lock();
        so.beginUpdate();
    }

    /** {@inheritDoc} */
    public void beginUpdate(IEventListener listener) {
        if (log.isTraceEnabled()) {
            log.trace("beginUpdate with listener - locked: {} owner: {} listener: {}", lock.isLocked(), lock.isHeldByCurrentThread(), listener);
        }
        // Make sure only one thread can update the SO
        lock.lock();
        // start updates
        so.beginUpdate(listener);
    }

    /** {@inheritDoc} */
    public void endUpdate() {
        if (log.isTraceEnabled()) {
            log.trace("endUpdate - locked: {} owner: {}", lock.isLocked(), lock.isHeldByCurrentThread());
        }
        // end update of SO
        try {
            so.endUpdate();
        } catch (Exception ex) {
            log.warn("Exception on so.endUpdate", ex);
        } finally {
            lock.unlock();
        }
    }

    /** {@inheritDoc} */
    public int getVersion() {
        return so.getVersion();
    }

    /** {@inheritDoc} */
    public void sendMessage(String handler, List<?> arguments) {
        if (so != null) {
            beginUpdate();
            try {
                so.sendMessage(handler, arguments);
            } catch (Exception ex) {
                log.warn("Exception on so.sendMessage", ex);
            } finally {
                endUpdate();
            }
            // Invoke method on registered handler
            String serviceName, serviceMethod;
            // Find out last dot position
            int dotPos = handler.lastIndexOf('.');
            // If any, split service name and service method name
            if (dotPos != -1) {
                serviceName = handler.substring(0, dotPos);
                serviceMethod = handler.substring(dotPos + 1);
            } else {
                // Otherwise only service method name is available
                serviceName = "";
                serviceMethod = handler;
            }
            // Get previously registered handler for service
            Object soHandler = getServiceHandler(serviceName);
            if (soHandler == null && hasParent()) {
                // No custom handler, check for service defined in the scope's context
                IContext context = getParent().getContext();
                String serviceId = null;
                try {
                    // The bean must have a name of
                    // "<SharedObjectName>.<DottedServiceName>.soservice"
                    serviceId = so.getName() + '.' + serviceName + ".soservice";
                    if (context.hasBean(serviceId)) {
                        soHandler = context.getBean(serviceId);
                    }
                } catch (Exception err) {
                    log.debug("No such bean: {}", serviceId);
                }
            }
            // Once handler is found, find matching method
            if (soHandler != null) {
                // With exact params...
                Object[] methodResult = ReflectionUtils.findMethodWithExactParameters(soHandler, serviceMethod, arguments);
                // Or at least with suitable list params
                if (methodResult.length == 0 || methodResult[0] == null) {
                    methodResult = ReflectionUtils.findMethodWithListParameters(soHandler, serviceMethod, arguments);
                }
                // If method is found...
                if (methodResult.length > 0 && methodResult[0] != null) {
                    Method method = (Method) methodResult[0];
                    Object[] params = (Object[]) methodResult[1];
                    // ...try to invoke it and handle exceptions
                    try {
                        method.invoke(soHandler, params);
                    } catch (Exception err) {
                        log.error("Error while invoking method {} on shared object handler {}", new Object[] { serviceMethod, handler }, err);
                    }
                }
            }
            // notify server listeners
            for (ISharedObjectListener listener : serverListeners) {
                listener.onSharedObjectSend(this, handler, arguments);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean removeAttribute(String name) {
        boolean success = false;
        // begin update of shared object
        beginUpdate();
        try {
            // try to remove attribute
            success = so.removeAttribute(name);
        } catch (Exception ex) {
            log.warn("Exception on so.removeAttribute", ex);
        } finally {
            // end update of SO
            endUpdate();
        }
        // notify listeners on success and return true
        if (success) {
            for (ISharedObjectListener listener : serverListeners) {
                listener.onSharedObjectDelete(this, name);
            }
        }
        return success;
    }

    /** {@inheritDoc} */
    @Override
    public void removeAttributes() {
        beginUpdate();
        try {
            // remove all attributes
            so.removeAttributes();
        } catch (Exception ex) {
            log.warn("Exception on so.removeAttributes", ex);
        } finally {
            endUpdate();
        }
        // notify listeners on attributes clear
        for (ISharedObjectListener listener : serverListeners) {
            listener.onSharedObjectClear(this);
        }
    }

    /** {@inheritDoc} */
    public int size() {
        return so != null ? so.getAttributeNames().size() : 0;
    }

    /** {@inheritDoc} */
    @Override
    public boolean addEventListener(IEventListener listener) {
        boolean result = super.addEventListener(listener) && so.register(listener);
        for (ISharedObjectListener soListener : serverListeners) {
            soListener.onSharedObjectConnect(this);
        }
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public boolean removeEventListener(IEventListener listener) {
        // remove the listener from the so
        so.unregister(listener);
        // if we have not been released by all that acquired then keep on
        // disconnection of the last listener
        if (so.isAcquired()) {
            log.debug("Shared object has been aquired so setting keep on disconnect");
            keepOnDisconnect = true;
        }
        // remove the listener
        boolean result = super.removeEventListener(listener);
        // notify other listeners that someone has stopped listening
        for (ISharedObjectListener soListener : serverListeners) {
            soListener.onSharedObjectDisconnect(this);
        }
        // check that linger job has be set
        if (lingerJobName == null) {
            // start a job to allow the so to linger for just a few ticks
            QuartzSchedulingService scheduler = (QuartzSchedulingService) getParent().getContext().getBean(QuartzSchedulingService.BEAN_NAME);
            IScheduledJob job = new IScheduledJob() {
                public void execute(ISchedulingService service) {
                    if (so != null && !so.isClosed()) {
                        so.checkRelease();
                    }
                }
            };
            lingerJobName = scheduler.addScheduledOnceJob(lingerPeriod, job);
        }
        // check acquire
        if (so.isClosed()) {
            log.debug("Removing scope: {}", this);
            getParent().removeChildScope(this);
        }
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasAttribute(String name) {
        return so.hasAttribute(name);
    }

    /** {@inheritDoc} */
    @Override
    public Object getAttribute(String name) {
        return so.getAttribute(name);
    }

    /** {@inheritDoc} */
    @Override
    public Object getAttribute(String name, Object value) {
        beginUpdate();
        try {
            return so.getAttribute(name, value);
        } catch (Exception ex) {
            log.warn("Exception on so.getAttribute", ex);
        } finally {
            endUpdate();
        }
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public Map<String, Object> getAttributes() {
        return so.getAttributes();
    }

    /** {@inheritDoc} */
    @Override
    public Set<String> getAttributeNames() {
        return so.getAttributeNames();
    }

    /** {@inheritDoc} */
    @Override
    public Boolean getBoolAttribute(String name) {
        return so.getBoolAttribute(name);
    }

    /** {@inheritDoc} */
    @Override
    public Byte getByteAttribute(String name) {
        return so.getByteAttribute(name);
    }

    /** {@inheritDoc} */
    @Override
    public Double getDoubleAttribute(String name) {
        return so.getDoubleAttribute(name);
    }

    /** {@inheritDoc} */
    @Override
    public Integer getIntAttribute(String name) {
        return so.getIntAttribute(name);
    }

    /** {@inheritDoc} */
    @Override
    public List<?> getListAttribute(String name) {
        return so.getListAttribute(name);
    }

    /** {@inheritDoc} */
    @Override
    public Long getLongAttribute(String name) {
        return so.getLongAttribute(name);
    }

    /** {@inheritDoc} */
    @Override
    public Map<?, ?> getMapAttribute(String name) {
        return so.getMapAttribute(name);
    }

    /** {@inheritDoc} */
    @Override
    public Set<?> getSetAttribute(String name) {
        return so.getSetAttribute(name);
    }

    /** {@inheritDoc} */
    @Override
    public Short getShortAttribute(String name) {
        return so.getShortAttribute(name);
    }

    /** {@inheritDoc} */
    @Override
    public String getStringAttribute(String name) {
        return so.getStringAttribute(name);
    }

    /** {@inheritDoc} */
    public Map<String, Object> getData() {
        return so.getData();
    }

    /**
     * Return security handlers for this shared object or
     * 
     * <pre>
     * null
     * </pre>
     * 
     * if none are found.
     * 
     * @return set of security handlers
     */
    private Set<ISharedObjectSecurity> getSecurityHandlers() {
        ISharedObjectSecurityService security = (ISharedObjectSecurityService) ScopeUtils.getScopeService(getParent(), ISharedObjectSecurityService.class);
        if (security == null) {
            return null;
        }
        return security.getSharedObjectSecurity();
    }

    /**
     * Call handlers and check if connection to the existing SO is allowed.
     * 
     * @return is connection allowed
     */
    protected boolean isConnectionAllowed() {
        // Check internal handlers first
        for (ISharedObjectSecurity handler : securityHandlers) {
            if (!handler.isConnectionAllowed(this)) {
                return false;
            }
        }
        // Check global SO handlers next
        final Set<ISharedObjectSecurity> handlers = getSecurityHandlers();
        if (handlers == null) {
            return true;
        }
        for (ISharedObjectSecurity handler : handlers) {
            if (!handler.isConnectionAllowed(this)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Call handlers and check if writing to the SO is allowed.
     * 
     * @param key
     *            key
     * @param value
     *            value
     * @return is write allowed
     */
    protected boolean isWriteAllowed(String key, Object value) {
        // check internal handlers first
        for (ISharedObjectSecurity handler : securityHandlers) {
            if (!handler.isWriteAllowed(this, key, value)) {
                return false;
            }
        }
        // check global SO handlers next
        final Set<ISharedObjectSecurity> handlers = getSecurityHandlers();
        if (handlers == null) {
            return true;
        }
        for (ISharedObjectSecurity handler : handlers) {
            if (!handler.isWriteAllowed(this, key, value)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Call handlers and check if deleting a property from the SO is allowed.
     * 
     * @param key
     *            key
     * @return is delete allowed
     */
    protected boolean isDeleteAllowed(String key) {
        // check internal handlers first
        for (ISharedObjectSecurity handler : securityHandlers) {
            if (!handler.isDeleteAllowed(this, key)) {
                return false;
            }
        }
        // check global SO handlers next
        final Set<ISharedObjectSecurity> handlers = getSecurityHandlers();
        if (handlers == null) {
            return true;
        }
        for (ISharedObjectSecurity handler : handlers) {
            if (!handler.isDeleteAllowed(this, key)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Call handlers and check if sending a message to the clients connected to the SO is allowed.
     * 
     * @param message
     *            message
     * @param arguments
     *            arguments
     * @return is send allowed
     */
    protected boolean isSendAllowed(String message, List<?> arguments) {
        // check internal handlers first
        for (ISharedObjectSecurity handler : securityHandlers) {
            if (!handler.isSendAllowed(this, message, arguments)) {
                return false;
            }
        }
        // check global SO handlers next
        final Set<ISharedObjectSecurity> handlers = getSecurityHandlers();
        if (handlers == null) {
            return true;
        }
        for (ISharedObjectSecurity handler : handlers) {
            if (!handler.isSendAllowed(this, message, arguments)) {
                return false;
            }
        }
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public void dispatchEvent(IEvent e) {
        if (e instanceof ISharedObjectMessage || e.getType() == IEvent.Type.SHARED_OBJECT) {
            ISharedObjectMessage msg = (ISharedObjectMessage) e;
            if (msg.hasSource()) {
                beginUpdate(msg.getSource());
            } else {
                beginUpdate();
            }
            try {
                for (ISharedObjectEvent event : msg.getEvents()) {
                    final String key = event.getKey();
                    switch (event.getType()) {
                        case SERVER_CONNECT:
                            if (!isConnectionAllowed()) {
                                so.returnError(SO_NO_READ_ACCESS);
                            } else if (msg.hasSource()) {
                                IEventListener source = msg.getSource();
                                if (source instanceof BaseConnection) {
                                    ((BaseConnection) source).registerBasicScope(this);
                                } else {
                                    addEventListener(source);
                                }
                            }
                            break;
                        case SERVER_DISCONNECT:
                            if (msg.hasSource()) {
                                IEventListener source = msg.getSource();
                                if (source instanceof BaseConnection) {
                                    ((BaseConnection) source).unregisterBasicScope(this);
                                } else {
                                    removeEventListener(source);
                                }
                            }
                            break;
                        case SERVER_SET_ATTRIBUTE:
                            final Object value = event.getValue();
                            if (!isWriteAllowed(key, value)) {
                                so.returnAttributeValue(key);
                                so.returnError(SO_NO_WRITE_ACCESS);
                            } else {
                                setAttribute(key, value);
                            }
                            break;
                        case SERVER_DELETE_ATTRIBUTE:
                            if (!isDeleteAllowed(key)) {
                                so.returnAttributeValue(key);
                                so.returnError(SO_NO_WRITE_ACCESS);
                            } else {
                                removeAttribute(key);
                            }
                            break;
                        case SERVER_SEND_MESSAGE:
                            final List<?> arguments = (List<?>) event.getValue();
                            // Ignore request silently if not allowed
                            if (isSendAllowed(key, arguments)) {
                                sendMessage(key, arguments);
                            } else {
                                log.debug("Send is not allowed for {}", key);
                            }
                            break;
                        default:
                            log.warn("Unknown SO event: {}", event.getType());
                    }
                }
            } catch (Exception ex) {
                log.warn("Exception on dispatchEvent", ex);
            } finally {
                endUpdate();
            }
        } else {
            // don't know how to handle this event.
            super.dispatchEvent(e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean setAttribute(String name, Object value) {
        boolean success = false;
        beginUpdate();
        try {
            success = so.setAttribute(name, value);
        } catch (Exception ex) {
            log.warn("Exception on so.setAttribute", ex);
        } finally {
            endUpdate();
        }
        if (success) {
            for (ISharedObjectListener listener : serverListeners) {
                listener.onSharedObjectUpdate(this, name, value);
            }
        }
        return success;
    }

    /** {@inheritDoc} */
    @Override
    public boolean setAttributes(IAttributeStore values) {
        boolean success = false;
        beginUpdate();
        try {
            success = so.setAttributes(values);
        } catch (Exception ex) {
            log.warn("Exception on so.setAttributes", ex);
        } finally {
            endUpdate();
        }
        if (success) {
            for (ISharedObjectListener listener : serverListeners) {
                listener.onSharedObjectUpdate(this, values);
            }
        }
        return success;
    }

    /** {@inheritDoc} */
    @Override
    public boolean setAttributes(Map<String, Object> values) {
        boolean success = false;
        beginUpdate();
        try {
            success = so.setAttributes(values);
        } catch (Exception ex) {
            log.warn("Exception on so.setAttributes", ex);
        } finally {
            endUpdate();
        }
        if (success) {
            for (ISharedObjectListener listener : serverListeners) {
                listener.onSharedObjectUpdate(this, values);
            }
        }
        return success;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "SharedObjectScope: " + getName();
    }

    /** {@inheritDoc} */
    public void addSharedObjectListener(ISharedObjectListener listener) {
        serverListeners.add(listener);
    }

    /** {@inheritDoc} */
    public void removeSharedObjectListener(ISharedObjectListener listener) {
        serverListeners.remove(listener);
    }

    /** {@inheritDoc} */
    public void registerServiceHandler(Object handler) {
        registerServiceHandler("", handler);
    }

    /** {@inheritDoc} */
    public void registerServiceHandler(String name, Object handler) {
        if (name == null) {
            name = "";
        }
        handlers.put(name, handler);
    }

    public void unregisterServiceHandler() {
        unregisterServiceHandler("");
    }

    /** {@inheritDoc} */
    public void unregisterServiceHandler(String name) {
        if (name == null) {
            name = "";
        }
        handlers.remove(name);
    }

    /** {@inheritDoc} */
    public Object getServiceHandler(String name) {
        if (name == null) {
            name = "";
        }
        return handlers.get(name);
    }

    /** {@inheritDoc} */
    public Set<String> getServiceHandlerNames() {
        return Collections.unmodifiableSet(handlers.keySet());
    }

    /**
     * Locks the shared object instance. Prevents any changes to this object by clients until the SharedObject.unlock() method is called.
     */
    public void lock() {
        lock.lock();
    }

    /**
     * Unlocks a shared object instance that was locked with SharedObject.lock().
     */
    public void unlock() {
        lock.unlock();
    }

    /**
     * Returns the locked state of this SharedObject.
     * 
     * @return true if in a locked state; false otherwise
     */
    public boolean isLocked() {
        return lock.isLocked();
    }

    /** {@inheritDoc} */
    public boolean clear() {
        boolean success = false;
        beginUpdate();
        try {
            success = so.clear();
        } catch (Exception ex) {
            log.warn("Exception on so.clear", ex);
        } finally {
            endUpdate();
        }
        if (success) {
            for (ISharedObjectListener listener : serverListeners) {
                listener.onSharedObjectClear(this);
            }
        }
        return success;
    }

    /** {@inheritDoc} */
    public void close() {
        // close the internal SO
        so.close();
        // remove from the parent
        parent.removeChildScope(this);
        // clear the reference
        so = null;
    }

    /** {@inheritDoc} */
    public void acquire() {
        so.acquire();
    }

    /** {@inheritDoc} */
    public boolean isAcquired() {
        return so.isAcquired();
    }

    /** {@inheritDoc} */
    public void release() {
        so.release();
    }

    /** {@inheritDoc} */
    public ISharedObjectStatistics getStatistics() {
        return so;
    }

    /** {@inheritDoc} */
    public void setDirty(boolean dirty) {
        so.setDirty(dirty);
    }

}