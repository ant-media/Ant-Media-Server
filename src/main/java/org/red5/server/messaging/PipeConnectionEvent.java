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

package org.red5.server.messaging;

import java.util.EventObject;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Event object corresponds to the connect/disconnect events among providers/consumers on pipes. 
 * This object is immutable except for the parameter map and tasks.
 * 
 * @author The Red5 Project
 * @author Steven Gong (steven.gong@gmail.com)
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class PipeConnectionEvent extends EventObject {

    private static final long serialVersionUID = 9078843765378168072L;

    /** Pipe connection event type */
    public enum EventType {
        /** Provider connects in pull mode */
        PROVIDER_CONNECT_PULL,
        /** Provider connects in push mode */
        PROVIDER_CONNECT_PUSH,
        /** Provider disconnects */
        PROVIDER_DISCONNECT,
        /** Consumer connects in pull mode */
        CONSUMER_CONNECT_PULL,
        /** Consumer connects in push mode */
        CONSUMER_CONNECT_PUSH,
        /** Consumer disconnects */
        CONSUMER_DISCONNECT
    };

    /**
     * Provider
     */
    private final transient IProvider provider;

    /**
     * Consumer
     */
    private final transient IConsumer consumer;

    /**
     * Event type
     */
    private final EventType type;

    /**
     * Parameters map
     */
    private final ConcurrentMap<String, Object> paramMap = new ConcurrentHashMap<>();

    /**
     * List of tasks to be executed for the event
     */
    private final LinkedList<Runnable> taskList = new LinkedList<>();

    /**
     * Construct an object with the specific pipe as the <tt>source</tt>
     * 
     * @param source pipe that triggers this event
     * @param type event type
     * @param consumer the consumer
     * @param paramMap parameters map
     */
    private PipeConnectionEvent(AbstractPipe source, EventType type, IConsumer consumer, Map<String, Object> paramMap) {
        super(source);
        this.type = type;
        this.consumer = consumer;
        this.provider = null;
        setParamMap(paramMap);
    }

    /**
     * Construct an object with the specific pipe as the <tt>source</tt>
     * 
     * @param source pipe that triggers this event
     * @param type event type
     * @param provider the provider
     * @param paramMap parameters map
     */
    private PipeConnectionEvent(AbstractPipe source, EventType type, IProvider provider, Map<String, Object> paramMap) {
        super(source);
        this.type = type;
        this.consumer = null;
        this.provider = provider;
        setParamMap(paramMap);
    }

    /**
     * Return pipe connection provider
     * 
     * @return Provider
     */
    public IProvider getProvider() {
        return provider;
    }

    /**
     * Return pipe connection consumer
     * 
     * @return Consumer
     */
    public IConsumer getConsumer() {
        return consumer;
    }

    /**
     * Return event type
     * 
     * @return Event type
     */
    public EventType getType() {
        return type;
    }

    /**
     * Return event parameters as Map
     * 
     * @return Event parameters as Map
     */
    public Map<String, Object> getParamMap() {
        return paramMap;
    }

    /**
     * Setter for event parameters map
     * 
     * @param paramMap
     *            Event parameters as Map
     */
    public void setParamMap(Map<String, Object> paramMap) {
        if (paramMap != null && !paramMap.isEmpty()) {
            this.paramMap.putAll(paramMap);
        }
    }

    /**
     * Add task to list
     * 
     * @param task
     *            Task to add
     */
    public void addTask(Runnable task) {
        taskList.add(task);
    }

    /**
     * Return list of tasks
     * 
     * @return List of tasks
     */
    List<Runnable> getTaskList() {
        return taskList;
    }

    /**
     * Builds a PipeConnectionEvent with a source pipe and consumer.
     * 
     * @param source pipe that triggers this event
     * @param type event type
     * @param consumer the consumer
     * @param paramMap parameters map
     * @return event
     */
    public final static PipeConnectionEvent build(AbstractPipe source, EventType type, IConsumer consumer, Map<String, Object> paramMap) {
        return new PipeConnectionEvent(source, type, consumer, paramMap);
    }

    /**
     * Builds a PipeConnectionEvent with a source pipe and provider.
     * 
     * @param source pipe that triggers this event
     * @param type event type
     * @param provider the provider
     * @param paramMap parameters map
     * @return event
     */
    public final static PipeConnectionEvent build(AbstractPipe source, EventType type, IProvider provider, Map<String, Object> paramMap) {
        return new PipeConnectionEvent(source, type, provider, paramMap);
    }

}
