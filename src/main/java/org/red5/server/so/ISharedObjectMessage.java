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

import java.util.Queue;

import org.red5.server.net.rtmp.event.IRTMPEvent;

/**
 * Shared object message
 */
public interface ISharedObjectMessage extends IRTMPEvent {

    /**
     * Returns the name of the shared object this message belongs to.
     * 
     * @return name of the shared object
     */
    public String getName();

    /**
     * Returns the version to modify.
     * 
     * @return version to modify
     */
    public int getVersion();

    /**
     * Does the message affect a persistent shared object?
     * 
     * @return true if a persistent shared object should be updated otherwise false
     */
    public boolean isPersistent();

    /**
     * Returns a set of ISharedObjectEvent objects containing informations what to change.
     * 
     * @return set of ISharedObjectEvents
     */
    public Queue<ISharedObjectEvent> getEvents();

    /**
     * Addition event handler
     * 
     * @param type
     *            Event type
     * @param key
     *            Handler key
     * @param value
     *            Event value (like arguments)
     * @return true if event is added and false if it is not added
     */
    public boolean addEvent(ISharedObjectEvent.Type type, String key, Object value);

    /**
     * Add event handler
     * 
     * @param event
     *            SO event
     */
    public void addEvent(ISharedObjectEvent event);

    /**
     * Clear shared object
     */
    public void clear();

    /**
     * Is empty?
     *
     * @return <pre>
     * true
     * </pre>
     * 
     *         if shared object is empty,
     * 
     *         <pre>
     * false
     * </pre>
     * 
     *         otherwise
     */
    public boolean isEmpty();

}
