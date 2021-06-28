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

package org.red5.server.api.so;

import java.util.List;
import java.util.Map;

import org.red5.server.api.ICastingAttributeStore;
import org.red5.server.api.event.IEventListener;

/**
 * Base interface for shared objects. Changes to the shared objects are propagated to all subscribed clients.
 * 
 * If you want to modify multiple attributes and notify the clients about all changes at once, you can use code like this:
 * 
 * <pre>
 * SharedObject.beginUpdate();
 * SharedObject.setAttribute(&quot;One&quot;, '1');
 * SharedObject.setAttribute(&quot;Two&quot;, '2');
 * SharedObject.removeAttribute(&quot;Three&quot;);
 * SharedObject.endUpdate();
 * </pre>
 * 
 * All changes between "beginUpdate" and "endUpdate" will be sent to the clients using one notification event.
 * 
 * @author The Red5 Project
 * @author Joachim Bauch (jojo@struktur.de)
 */
public interface ISharedObjectBase extends ISharedObjectHandlerProvider, ICastingAttributeStore {

    /**
     * Returns the version of the shared object. The version is incremented automatically on each modification.
     * 
     * @return the version of the shared object
     */
    public int getVersion();

    /**
     * Check if the object has been created as persistent shared object by the client.
     * 
     * @return true if the shared object is persistent, false otherwise
     */
    public boolean isPersistent();

    /**
     * Return a map containing all attributes of the shared object. <br>
     * NOTE: The returned map will be read-only.
     * 
     * @return a map containing all attributes of the shared object
     */
    public Map<String, Object> getData();

    /**
     * Send a message to a handler of the shared object.
     * 
     * @param handler
     *            the name of the handler to call
     * @param arguments
     *            a list of objects that should be passed as arguments to the handler
     */
    public void sendMessage(String handler, List<?> arguments);

    /**
     * Start performing multiple updates to the shared object from serverside code.
     */
    public void beginUpdate();

    /**
     * Start performing multiple updates to the shared object from a connected client.
     * 
     * @param source
     *            Update events listener
     */
    public void beginUpdate(IEventListener source);

    /**
     * The multiple updates are complete, notify clients about all changes at once.
     */
    public void endUpdate();

    /**
     * Register object that will be notified about update events.
     * 
     * @param listener
     *            the object to notify
     */
    public void addSharedObjectListener(ISharedObjectListener listener);

    /**
     * Unregister object to not longer receive update events.
     * 
     * @param listener
     *            the object to unregister
     */
    public void removeSharedObjectListener(ISharedObjectListener listener);

    /**
     * Locks the shared object instance. Prevents any changes to this object by clients until the SharedObject.unlock() method is called.
     */
    public void lock();

    /**
     * Unlocks a shared object instance that was locked with SharedObject.lock().
     */
    public void unlock();

    /**
     * Returns the locked state of this SharedObject.
     * 
     * @return true if in a locked state; false otherwise
     */
    public boolean isLocked();

    /**
     * Deletes all the attributes and sends a clear event to all listeners. The persistent data object is also removed from a persistent shared object.
     * 
     * @return true if successful; false otherwise
     */
    public boolean clear();

    /**
     * Detaches a reference from this shared object, this will destroy the reference immediately. This is useful when you don't want to proxy a shared object any longer.
     */
    public void close();

}
