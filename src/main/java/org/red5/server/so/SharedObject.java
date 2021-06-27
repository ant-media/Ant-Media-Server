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

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.red5.io.object.Deserializer;
import org.red5.io.object.Input;
import org.red5.io.object.Output;
import org.red5.io.object.Serializer;
import org.red5.server.AttributeStore;
import org.red5.server.api.IAttributeStore;
import org.red5.server.api.Red5;
import org.red5.server.api.event.IEventListener;
import org.red5.server.api.persistence.IPersistable;
import org.red5.server.api.persistence.IPersistenceStore;
import org.red5.server.api.scope.ScopeType;
import org.red5.server.api.statistics.ISharedObjectStatistics;
import org.red5.server.api.statistics.support.StatisticsCounter;
import org.red5.server.net.rtmp.RTMPConnection;
import org.red5.server.net.rtmp.codec.RTMP;
import org.red5.server.net.rtmp.message.Constants;
import org.red5.server.so.ISharedObjectEvent.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents shared object on server-side. Shared Objects in Flash are like cookies that are stored on client side. In Red5 and Flash Media Server there's one more special type of SOs : remote Shared Objects.
 *
 * These are shared by multiple clients and synchronized between them automatically on each data change. This is done asynchronously, used as events handling and is widely used in multiplayer Flash online games.
 *
 * Shared object can be persistent or transient. The difference is that first are saved to the disk and can be accessed later on next connection, transient objects are not saved and get lost each time they last client disconnects from it.
 *
 * Shared Objects has name identifiers and path on server's HD (if persistent). On deeper level server-side Shared Object in this implementation actually uses IPersistenceStore to delegate all (de)serialization work.
 *
 * SOs store data as simple map, that is, "name-value" pairs. Each value in turn can be complex object or map.
 * 
 * All access to methods that change properties in the SO must be properly synchronized for multi-threaded access.
 */
public class SharedObject extends AttributeStore implements ISharedObjectStatistics, IPersistable, Constants {

    protected static Logger log = LoggerFactory.getLogger(SharedObject.class);

    /**
     * Shared Object name (identifier)
     */
    protected String name = "";

    /**
     * SO path
     */
    protected String path = "";

    /**
     * true if the SharedObject was stored by the persistence framework and can be used later on reconnection
     */
    protected boolean persistent;

    /**
     * Object that is delegated with all storage work for persistent SOs
     */
    protected IPersistenceStore storage;

    /**
     * Version. Used on synchronization purposes.
     */
    protected AtomicInteger version = new AtomicInteger(1);

    /**
     * Number of pending update operations
     */
    protected AtomicInteger updateCounter = new AtomicInteger();

    /**
     * Has changes? flag
     */
    protected AtomicBoolean modified = new AtomicBoolean();

    /**
     * Last modified timestamp
     */
    protected long lastModified = -1;

    /**
     * Owner event
     */
    protected SharedObjectMessage ownerMessage;

    /**
     * Synchronization events
     */
    protected transient volatile ConcurrentLinkedQueue<ISharedObjectEvent> syncEvents = new ConcurrentLinkedQueue<ISharedObjectEvent>();

    /**
     * Listeners
     */
    protected transient volatile CopyOnWriteArraySet<IEventListener> listeners = new CopyOnWriteArraySet<IEventListener>();

    /**
     * Event listener, actually RTMP connection
     */
    protected IEventListener source;

    /**
     * Number of times the SO has been acquired
     */
    protected AtomicInteger acquireCount = new AtomicInteger();

    /**
     * Timestamp the scope was created.
     */
    private long creationTime;

    /**
     * Manages listener statistics.
     */
    protected transient StatisticsCounter listenerStats = new StatisticsCounter();

    /**
     * Counts number of "change" events.
     */
    protected AtomicInteger changeStats = new AtomicInteger();

    /**
     * Counts number of "delete" events.
     */
    protected AtomicInteger deleteStats = new AtomicInteger();

    /**
     * Counts number of "send message" events.
     */
    protected AtomicInteger sendStats = new AtomicInteger();

    /**
     * Whether or not this shared object is closed
     */
    protected AtomicBoolean closed = new AtomicBoolean(false);

    /** Constructs a new SharedObject. */
    public SharedObject() {
        // This is used by the persistence framework
        super();
        ownerMessage = new SharedObjectMessage(null, null, -1, false);
        creationTime = System.currentTimeMillis();
    }

    /**
     * Constructs new SO from Input object
     * 
     * @param input
     *            Input source
     * @throws IOException
     *             I/O exception
     *
     * @see org.red5.io.object.Input
     */
    public SharedObject(Input input) throws IOException {
        this();
        deserialize(input);
    }

    /**
     * Creates new SO from given data map, name, path and persistence option
     *
     * @param name
     *            SO name
     * @param path
     *            SO path
     * @param persistent
     *            SO persistence
     */
    public SharedObject(String name, String path, boolean persistent) {
        super();
        this.name = name;
        this.path = path;
        this.persistent = persistent;
        ownerMessage = new SharedObjectMessage(null, name, 0, persistent);
        creationTime = System.currentTimeMillis();
    }

    /**
     * Creates new SO from given data map, name, path, storage object and persistence option
     * 
     * @param name
     *            SO name
     * @param path
     *            SO path
     * @param persistent
     *            SO persistence
     * @param storage
     *            Persistence storage
     */
    public SharedObject(String name, String path, boolean persistent, IPersistenceStore storage) {
        this(name, path, persistent);
        setStore(storage);
    }

    /**
     * Creates new SO from given data map, name, path and persistence option
     *
     * @param data
     *            Data
     * @param name
     *            SO name
     * @param path
     *            SO path
     * @param persistent
     *            SO persistence
     */
    public SharedObject(Map<String, Object> data, String name, String path, boolean persistent) {
        this(name, path, persistent);
        attributes.putAll(data);
    }

    /**
     * Creates new SO from given data map, name, path, storage object and persistence option
     * 
     * @param data
     *            Data
     * @param name
     *            SO name
     * @param path
     *            SO path
     * @param persistent
     *            SO persistence
     * @param storage
     *            Persistence storage
     */
    public SharedObject(Map<String, Object> data, String name, String path, boolean persistent, IPersistenceStore storage) {
        this(data, name, path, persistent);
        setStore(storage);
    }

    /** {@inheritDoc} */
    public String getName() {
        return name;
    }

    /** {@inheritDoc} */
    public void setName(String name) {
        throw new UnsupportedOperationException(String.format("Name change not supported; current name: %s", getName()));
    }

    /** {@inheritDoc} */
    public String getPath() {
        return path;
    }

    /** {@inheritDoc} */
    public void setPath(String path) {
        this.path = path;
    }

    /** {@inheritDoc} */
    public String getType() {
        return ScopeType.SHARED_OBJECT.toString();
    }

    /** {@inheritDoc} */
    public long getLastModified() {
        return lastModified;
    }

    /** {@inheritDoc} */
    public boolean isPersistent() {
        return persistent;
    }

    /** {@inheritDoc} */
    public void setPersistent(boolean persistent) {
        log.debug("setPersistent: {}", persistent);
        this.persistent = persistent;
    }

    /**
     * Send update notification over data channel of RTMP connection
     */
    protected void sendUpdates() {
        log.debug("sendUpdates");
        // get the current version
        final int currentVersion = getVersion();
        log.debug("Current version: {}", currentVersion);
        // get the name
        final String name = getName();
        //get owner events
        ConcurrentLinkedQueue<ISharedObjectEvent> ownerEvents = ownerMessage.getEvents();
        if (!ownerEvents.isEmpty()) {
            // get all current owner events
            final ConcurrentLinkedQueue<ISharedObjectEvent> events = new ConcurrentLinkedQueue<ISharedObjectEvent>();
            if (ownerEvents.size() > SharedObjectService.MAXIMUM_EVENTS_PER_UPDATE) {
                log.debug("Owner events exceed max: {}", ownerEvents.size());
                for (int i = 0; i < SharedObjectService.MAXIMUM_EVENTS_PER_UPDATE; i++) {
                    events.add(ownerEvents.poll());
                }
            } else {
                events.addAll(ownerEvents);
                ownerEvents.removeAll(events);
            }
            // send update to "owner" of this update request
            if (source != null) {
                final RTMPConnection con = (RTMPConnection) source;
                // create a worker
                SharedObjectService.submitTask(new Runnable() {
                    public void run() {
                        Red5.setConnectionLocal(con);
                        con.sendSharedObjectMessage(name, currentVersion, persistent, events);
                        Red5.setConnectionLocal(null);
                    }
                });
            }
        } else if (log.isTraceEnabled()) {
            log.trace("No owner events to send");
        }
        // tell all the listeners
        if (!syncEvents.isEmpty()) {
            // get all current sync events 
            final ConcurrentLinkedQueue<ISharedObjectEvent> events = new ConcurrentLinkedQueue<ISharedObjectEvent>();
            if (syncEvents.size() > SharedObjectService.MAXIMUM_EVENTS_PER_UPDATE) {
                log.debug("Sync events exceed max: {}", syncEvents.size());
                for (int i = 0; i < SharedObjectService.MAXIMUM_EVENTS_PER_UPDATE; i++) {
                    events.add(syncEvents.poll());
                }
            } else {
                events.addAll(syncEvents);
                syncEvents.removeAll(events);
            }
            // get the listeners
            Set<IEventListener> listeners = getListeners();
            if (log.isDebugEnabled()) {
                log.debug("Listeners: {}", listeners);
            }
            // updates all registered clients of this shared object
            for (IEventListener listener : listeners) {
                if (listener != source) {
                    if (listener instanceof RTMPConnection) {
                        final RTMPConnection con = (RTMPConnection) listener;
                        if (con.getStateCode() == RTMP.STATE_CONNECTED) {
                            // create a worker
                            SharedObjectService.submitTask(new Runnable() {
                                public void run() {
                                    Red5.setConnectionLocal(con);
                                    con.sendSharedObjectMessage(name, currentVersion, persistent, events);
                                    Red5.setConnectionLocal(null);
                                }
                            });
                        } else {
                            log.debug("Skipping unconnected connection");
                        }
                    } else {
                        log.warn("Can't send sync message to unknown connection {}", listener);
                    }
                } else {
                    // don't re-send update to active client
                    log.debug("Skipped {}", source);
                }
            }
        } else if (log.isTraceEnabled()) {
            log.trace("No sync events to send");
        }
    }

    /**
     * Send notification about modification of SO
     */
    protected void notifyModified() {
        log.debug("notifyModified - modified: {} update counter: {}", modified.get(), updateCounter.get());
        if (updateCounter.get() == 0) {
            if (modified.get()) {
                // client sent at least one update -> increase version of SO
                updateVersion();
                lastModified = System.currentTimeMillis();
                if (storage == null || !storage.save(this)) {
                    log.warn("Could not store shared object");
                }
            }
            sendUpdates();
            modified.compareAndSet(true, false);
        }
    }

    /**
     * Return an error message to the client.
     * 
     * @param message
     *            message
     */
    protected void returnError(String message) {
        ownerMessage.addEvent(Type.CLIENT_STATUS, "error", message);
    }

    /**
     * Return an attribute value to the owner.
     * 
     * @param name
     *            name
     */
    protected void returnAttributeValue(String name) {
        ownerMessage.addEvent(Type.CLIENT_UPDATE_DATA, name, getAttribute(name));
    }

    /**
     * Return attribute by name and set if it doesn't exist yet.
     * 
     * @param name
     *            Attribute name
     * @param value
     *            Value to set if attribute doesn't exist
     * @return Attribute value
     */
    @Override
    public Object getAttribute(String name, Object value) {
        log.debug("getAttribute - name: {} value: {}", name, value);
        Object result = null;
        if (name != null) {
            result = attributes.putIfAbsent(name, value);
            if (result == null) {
                // no previous value
                modified.set(true);
                ownerMessage.addEvent(Type.CLIENT_UPDATE_DATA, name, value);
                syncEvents.add(new SharedObjectEvent(Type.CLIENT_UPDATE_DATA, name, value));
                notifyModified();
                changeStats.incrementAndGet();
                result = value;
            }
        }
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public boolean setAttribute(String name, Object value) {
        log.debug("setAttribute - name: {} value: {}", name, value);
        boolean result = true;
        ownerMessage.addEvent(Type.CLIENT_UPDATE_ATTRIBUTE, name, null);
        if (value == null && super.removeAttribute(name)) {
            // Setting a null value removes the attribute
            modified.set(true);
            syncEvents.add(new SharedObjectEvent(Type.CLIENT_DELETE_DATA, name, null));
            deleteStats.incrementAndGet();
        } else if (value != null) {
            boolean setAttr = super.setAttribute(name, value);
            log.debug("Set attribute?: {} modified: {}", setAttr, modified.get());
            // only sync if the attribute changed
            modified.set(true);
            syncEvents.add(new SharedObjectEvent(Type.CLIENT_UPDATE_DATA, name, value));
            changeStats.incrementAndGet();
        } else {
            result = false;
        }
        notifyModified();
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public boolean setAttributes(Map<String, Object> values) {
        int successes = 0;
        if (values != null) {
            beginUpdate();
            try {
                for (Map.Entry<String, Object> entry : values.entrySet()) {
                    if (setAttribute(entry.getKey(), entry.getValue())) {
                        successes++;
                    }
                }
            } finally {
                endUpdate();
            }
        }
        // expect every value to have been added
        return (successes == values.size());
    }

    /** {@inheritDoc} */
    @Override
    public boolean setAttributes(IAttributeStore values) {
        if (values != null) {
            return setAttributes(values.getAttributes());
        }
        return false;
    }

    /**
     * Removes attribute with given name
     * 
     * @param name
     *            Attribute
     * @return <pre>
     * true
     * </pre>
     * 
     *         if there's such an attribute and it was removed,
     * 
     *         <pre>
     * false
     * </pre>
     * 
     *         otherwise
     */
    @Override
    public boolean removeAttribute(String name) {
        boolean result = true;
        // Send confirmation to client
        ownerMessage.addEvent(Type.CLIENT_DELETE_DATA, name, null);
        if (super.removeAttribute(name)) {
            modified.set(true);
            syncEvents.add(new SharedObjectEvent(Type.CLIENT_DELETE_DATA, name, null));
            deleteStats.incrementAndGet();
        } else {
            result = false;
        }
        notifyModified();
        return result;
    }

    /**
     * Broadcast event to event handler
     * 
     * @param handler
     *            Event handler
     * @param arguments
     *            Arguments
     */
    protected void sendMessage(String handler, List<?> arguments) {
        if (ownerMessage.addEvent(Type.CLIENT_SEND_MESSAGE, handler, arguments)) {
            syncEvents.add(new SharedObjectEvent(Type.CLIENT_SEND_MESSAGE, handler, arguments));
            sendStats.incrementAndGet();
            if (log.isTraceEnabled()) {
                log.trace("Send message: {}", arguments);
            }
        }
    }

    /**
     * Getter for data.
     *
     * @return SO data as unmodifiable map
     */
    public Map<String, Object> getData() {
        return getAttributes();
    }

    /**
     * Getter for version.
     *
     * @return SO version.
     */
    public int getVersion() {
        return version.get();
    }

    /**
     * Increases version by one
     */
    private void updateVersion() {
        version.incrementAndGet();
    }

    /**
     * Remove all attributes (clear Shared Object)
     */
    @Override
    public void removeAttributes() {
        // TODO: there must be a direct way to clear the SO on the client side...
        Set<String> names = getAttributeNames();
        for (String key : names) {
            ownerMessage.addEvent(Type.CLIENT_DELETE_DATA, key, null);
            syncEvents.add(new SharedObjectEvent(Type.CLIENT_DELETE_DATA, key, null));
        }
        deleteStats.addAndGet(names.size());
        // clear data
        super.removeAttributes();
        // mark as modified
        modified.set(true);
        // broadcast 'modified' event
        notifyModified();
    }

    /**
     * Register event listener
     * 
     * @param listener
     *            Event listener
     * @return true if listener was added
     */
    protected boolean register(IEventListener listener) {
        log.debug("register - listener: {}", listener);
        boolean registered = listeners.add(listener);
        if (registered) {
            listenerStats.increment();
            // prepare response for new client
            ownerMessage.addEvent(Type.CLIENT_INITIAL_DATA, null, null);
            if (!isPersistent()) {
                ownerMessage.addEvent(Type.CLIENT_CLEAR_DATA, null, null);
            }
            if (!attributes.isEmpty()) {
                ownerMessage.addEvent(new SharedObjectEvent(Type.CLIENT_UPDATE_DATA, null, getAttributes()));
            }
            // we call notifyModified here to send response if we're not in a beginUpdate block
            notifyModified();
        }
        return registered;
    }

    /**
     * Unregister event listener
     * 
     * @param listener
     *            Event listener
     */
    protected void unregister(IEventListener listener) {
        log.debug("unregister - listener: {}", listener);
        listeners.remove(listener);
        listenerStats.decrement();
    }

    /**
     * Check if shared object must be released.
     */
    protected void checkRelease() {
        if (!isPersistent() && listeners.isEmpty() && !isAcquired()) {
            log.info("Deleting shared object {} because all clients disconnected and it is no longer acquired.", name);
            if (storage != null) {
                if (!storage.remove(this)) {
                    log.error("Could not remove shared object");
                }
            }
            close();
        }
    }

    /**
     * Get event listeners.
     *
     * @return Value for property 'listeners'.
     */
    public Set<IEventListener> getListeners() {
        return Collections.unmodifiableSet(listeners);
    }

    /**
     * Begin update of this Shared Object. Increases number of pending update operations
     */
    protected void beginUpdate() {
        log.debug("beginUpdate");
        beginUpdate(source);
    }

    /**
     * Begin update of this Shared Object and setting listener
     * 
     * @param listener
     *            Update with listener
     */
    protected void beginUpdate(IEventListener listener) {
        log.debug("beginUpdate - listener: {}", listener);
        source = listener;
        // increase number of pending updates
        updateCounter.incrementAndGet();
    }

    /**
     * End update of this Shared Object. Decreases number of pending update operations and broadcasts modified event if it is equal to zero (i.e. no more pending update operations).
     */
    protected void endUpdate() {
        log.debug("endUpdate");
        // decrease number of pending updates
        if (updateCounter.decrementAndGet() == 0) {
            notifyModified();
            source = null;
        }
    }

    /** {@inheritDoc} */
    public void serialize(Output output) throws IOException {
        log.debug("serialize - name: {}", name);
        Serializer.serialize(output, getName());
        Map<String, Object> map = getAttributes();
        if (log.isTraceEnabled()) {
            log.trace("Attributes: {}", map);
        }
        Serializer.serialize(output, map);
    }

    /** {@inheritDoc} */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void deserialize(Input input) throws IOException {
        log.debug("deserialize");
        name = Deserializer.deserialize(input, String.class);
        log.trace("Name: {}", name);
        persistent = true;
        Map<String, Object> map = Deserializer.<Map> deserialize(input, Map.class);
        if (log.isTraceEnabled()) {
            log.trace("Attributes: {}", map);
        }
        super.setAttributes(map);
        ownerMessage.setName(name);
        ownerMessage.setPersistent(persistent);
    }

    /** {@inheritDoc} */
    public void setStore(IPersistenceStore store) {
        this.storage = store;
    }

    /** {@inheritDoc} */
    public IPersistenceStore getStore() {
        return storage;
    }

    /**
     * Deletes all the attributes and sends a clear event to all listeners. The persistent data object is also removed from a persistent shared object.
     * 
     * @return <pre>
     * true
     * </pre>
     * 
     *         on success,
     * 
     *         <pre>
     * false
     * </pre>
     * 
     *         otherwise
     */
    protected boolean clear() {
        log.debug("clear");
        super.removeAttributes();
        // send confirmation to client
        ownerMessage.addEvent(Type.CLIENT_CLEAR_DATA, name, null);
        notifyModified();
        changeStats.incrementAndGet();
        return true;
    }

    /**
     * Detaches a reference from this shared object, reset it's state, this will destroy the reference immediately. This is useful when you don't want to proxy a shared object any longer.
     */
    protected void close() {
        log.debug("close");
        closed.compareAndSet(false, true);
        // clear collections
        super.removeAttributes();
        listeners.clear();
        syncEvents.clear();
        ownerMessage.getEvents().clear();
    }

    /**
     * Prevent shared object from being released. Each call to
     * 
     * <pre>
     * acquire
     * </pre>
     * 
     * must be paired with a call to
     * 
     * <pre>
     * release
     * </pre>
     * 
     * so the SO isn't held forever. This is only valid for non-persistent SOs.
     */
    public void acquire() {
        log.debug("acquire");
        acquireCount.incrementAndGet();
    }

    /**
     * Check if shared object currently is acquired.
     * 
     * @return <pre>
     * true
     * </pre>
     * 
     *         if the SO is acquired, otherwise
     * 
     *         <pre>
     * false
     * </pre>
     */
    public boolean isAcquired() {
        return acquireCount.get() > 0;
    }

    /**
     * Release previously acquired shared object. If the SO is non-persistent, no more clients are connected the SO isn't acquired any more, the data is released.
     */
    public void release() {
        log.debug("release");
        if (acquireCount.get() == 0) {
            throw new RuntimeException("The shared object was not acquired before.");
        }
        if (acquireCount.decrementAndGet() == 0) {
            checkRelease();
        }
    }

    public boolean isClosed() {
        return closed.get();
    }

    /** {@inheritDoc} */
    public long getCreationTime() {
        return creationTime;
    }

    /** {@inheritDoc} */
    public int getTotalListeners() {
        return listenerStats.getTotal();
    }

    /** {@inheritDoc} */
    public int getMaxListeners() {
        return listenerStats.getMax();
    }

    /** {@inheritDoc} */
    public int getActiveListeners() {
        return listenerStats.getCurrent();
    }

    /** {@inheritDoc} */
    public int getTotalChanges() {
        return changeStats.intValue();
    }

    /** {@inheritDoc} */
    public int getTotalDeletes() {
        return deleteStats.intValue();
    }

    /** {@inheritDoc} */
    public int getTotalSends() {
        return sendStats.intValue();
    }

    /**
     * Sets a modified or dirty property on this object to indicate whether or not a modification has been made.
     * 
     * @param dirty
     *            true if modified and false otherwise
     */
    public void setDirty(boolean dirty) {
        log.trace("setDirty: {}", dirty);
        modified.set(dirty);
    }

}
