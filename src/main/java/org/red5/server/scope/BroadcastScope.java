/*
 * RED5 Open Source Flash Server - https://github.com/Red5/
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

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.red5.server.api.scope.IBroadcastScope;
import org.red5.server.api.scope.IScope;
import org.red5.server.api.scope.ScopeType;
import org.red5.server.api.stream.IClientBroadcastStream;
import org.red5.server.messaging.IConsumer;
import org.red5.server.messaging.IMessage;
import org.red5.server.messaging.IPipeConnectionListener;
import org.red5.server.messaging.IProvider;
import org.red5.server.messaging.InMemoryPushPushPipe;
import org.red5.server.messaging.OOBControlMessage;
import org.red5.server.messaging.PipeConnectionEvent;
import org.red5.server.stream.IProviderService;

/**
 * Scope type for publishing that deals with pipe connection events, like async message listening in JMS
 */
public class BroadcastScope extends BasicScope implements IBroadcastScope, IPipeConnectionListener {

    /**
     * Broadcasting stream associated with this scope
     */
    private transient IClientBroadcastStream clientBroadcastStream;

    /**
     * Simple in memory push pipe, triggered by an active provider to push messages to consumer
     */
    private final transient InMemoryPushPushPipe pipe;

    /**
     * Number of components.
     */
    private AtomicInteger compCounter = new AtomicInteger(0);

    /**
     * Whether or not this "scope" has been removed
     */
    private volatile boolean removed;

    /**
     * Creates broadcast scope
     * 
     * @param parent
     *            Parent scope
     * @param name
     *            Scope name
     */
    public BroadcastScope(IScope parent, String name) {
        super(parent, ScopeType.BROADCAST, name, false);
        pipe = new InMemoryPushPushPipe(this);
        keepOnDisconnect = true;
    }

    /**
     * Register pipe connection event listener with this scope's pipe. A listener that wants to listen to events when provider/consumer connects to or disconnects from a specific pipe.
     * 
     * @param listener
     *            Pipe connection event listener
     * @see org.red5.server.messaging.IPipeConnectionListener
     */
    public void addPipeConnectionListener(IPipeConnectionListener listener) {
        pipe.addPipeConnectionListener(listener);
    }

    /**
     * Unregisters pipe connection event listener with this scope's pipe
     * 
     * @param listener
     *            Pipe connection event listener
     * @see org.red5.server.messaging.IPipeConnectionListener
     */
    public void removePipeConnectionListener(IPipeConnectionListener listener) {
        pipe.removePipeConnectionListener(listener);
    }

    /**
     * Pull message from pipe
     * 
     * @return Message object
     * @see org.red5.server.messaging.IMessage
     */
    public IMessage pullMessage() {
        return pipe.pullMessage();
    }

    /**
     * Pull message with timeout
     * 
     * @param wait
     *            Timeout
     * @return Message object
     * @see org.red5.server.messaging.IMessage
     */
    public IMessage pullMessage(long wait) {
        return pipe.pullMessage(wait);
    }

    /**
     * Connect scope's pipe to given consumer
     *
     * @param consumer
     *            Consumer
     * @param paramMap
     *            Parameters passed with connection
     * @return true on success, false otherwise
     */
    public boolean subscribe(IConsumer consumer, Map<String, Object> paramMap) {
        return !removed && pipe.subscribe(consumer, paramMap);
    }

    /**
     * Disconnects scope's pipe from given consumer
     * 
     * @param consumer
     *            Consumer
     * @return true on success, false otherwise
     */
    public boolean unsubscribe(IConsumer consumer) {
        return pipe.unsubscribe(consumer);
    }

    /**
     * Getter for pipe consumers
     * 
     * @return Pipe consumers
     */
    public List<IConsumer> getConsumers() {
        return pipe.getConsumers();
    }

    /**
     * Send out-of-band ("special") control message
     *
     * @param consumer
     *            Consumer, may be used in concrete implementations
     * @param oobCtrlMsg
     *            Out-of-band control message
     */
    public void sendOOBControlMessage(IConsumer consumer, OOBControlMessage oobCtrlMsg) {
        pipe.sendOOBControlMessage(consumer, oobCtrlMsg);
    }

    /**
     * Push a message to this output endpoint. May block the pusher when output can't handle the message at the time.
     * 
     * @param message
     *            Message to be pushed
     * @throws IOException
     *             If message could not be pushed
     */
    public void pushMessage(IMessage message) throws IOException {
        pipe.pushMessage(message);
    }

    /**
     * Connect scope's pipe with given provider
     * 
     * @param provider
     *            Provider
     * @param paramMap
     *            Parameters passed on connection
     * @return true on success, false otherwise
     */
    public boolean subscribe(IProvider provider, Map<String, Object> paramMap) {
        return !removed && pipe.subscribe(provider, paramMap);
    }

    /**
     * Disconnects scope's pipe from given provider
     * 
     * @param provider
     *            Provider
     * @return true on success, false otherwise
     */
    public boolean unsubscribe(IProvider provider) {
        return pipe.unsubscribe(provider);
    }

    /**
     * Getter for providers list
     * 
     * @return List of providers
     */
    public List<IProvider> getProviders() {
        return pipe.getProviders();
    }

    /**
     * Send out-of-band ("special") control message
     *
     * @param provider
     *            Provider, may be used in concrete implementations
     * @param oobCtrlMsg
     *            Out-of-band control message
     */
    public void sendOOBControlMessage(IProvider provider, OOBControlMessage oobCtrlMsg) {
        pipe.sendOOBControlMessage(provider, oobCtrlMsg);
    }

    /**
     * Pipe connection event handler
     * 
     * @param event
     *            Pipe connection event
     */
    public void onPipeConnectionEvent(PipeConnectionEvent event) {
        // Switch event type
        switch (event.getType()) {
            case PipeConnectionEvent.CONSUMER_CONNECT_PULL:
            case PipeConnectionEvent.CONSUMER_CONNECT_PUSH:
            case PipeConnectionEvent.PROVIDER_CONNECT_PULL:
            case PipeConnectionEvent.PROVIDER_CONNECT_PUSH:
                compCounter.incrementAndGet();
                break;
            case PipeConnectionEvent.CONSUMER_DISCONNECT:
            case PipeConnectionEvent.PROVIDER_DISCONNECT:
                if (compCounter.decrementAndGet() <= 0) {
                    // XXX should we synchronize parent before removing?
                    if (hasParent()) {
                        IScope parent = getParent();
                        IProviderService providerService = (IProviderService) parent.getContext().getBean(IProviderService.BEAN_NAME);
                        removed = providerService.unregisterBroadcastStream(parent, getName());
                    } else {
                        removed = true;
                    }
                }
                break;
            default:
                throw new UnsupportedOperationException("Event type not supported: " + event.getType());
        }
    }

    /**
     * Returns the client broadcast stream
     */
    public IClientBroadcastStream getClientBroadcastStream() {
        return clientBroadcastStream;
    }

    /**
     * Sets the client broadcast stream
     * 
     * @param clientBroadcastStream
     *            stream
     */
    public void setClientBroadcastStream(IClientBroadcastStream clientBroadcastStream) {
        if (this.clientBroadcastStream != null) {
            log.info("ClientBroadcastStream already exists: {} new: {}", this.clientBroadcastStream, clientBroadcastStream);
        }
        this.clientBroadcastStream = clientBroadcastStream;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "BroadcastScope [clientBroadcastStream=" + clientBroadcastStream + ", compCounter=" + compCounter + ", removed=" + removed + "]";
    }

}
