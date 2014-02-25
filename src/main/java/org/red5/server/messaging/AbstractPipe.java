/*
 * RED5 Open Source Flash Server - http://code.google.com/p/red5/
 * 
 * Copyright 2006-2013 by respective authors (see below). All rights reserved.
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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;

/**
 * Abstract pipe that books providers/consumers and listeners.
 * Aim to ease the implementation of concrete pipes. For more
 * information on what pipe is, see IPipe interface documentation.
 * 
 * @author The Red5 Project
 * @author Steven Gong (steven.gong@gmail.com)
 *
 * @see     org.red5.server.messaging.IPipe
 */
public abstract class AbstractPipe implements IPipe {
	/**
	 * Logger
	 */
	private static final Logger log = LoggerFactory.getLogger(AbstractPipe.class);

	/**
	 * Pipe consumers list
	 */
	protected volatile CopyOnWriteArrayList<IConsumer> consumers = new CopyOnWriteArrayList<IConsumer>();

	/**
	 * Pipe providers list
	 */
	protected volatile CopyOnWriteArrayList<IProvider> providers = new CopyOnWriteArrayList<IProvider>();

	/**
	 * Event listeners
	 */
	protected volatile CopyOnWriteArrayList<IPipeConnectionListener> listeners = new CopyOnWriteArrayList<IPipeConnectionListener>();

	/**
	 * Executor service used to run pipe tasks.
	 */
	private static ExecutorService taskExecutor;

	/**
	 * Connect consumer to this pipe. Doesn't allow to connect one consumer twice.
	 * Does register event listeners if instance of IPipeConnectionListener is given.
	 *
	 * @param consumer        Consumer
	 * @param paramMap        Parameters passed with connection, used in concrete pipe implementations
	 * @return                <code>true</code> if consumer was added, <code>false</code> otherwise
	 */
	public boolean subscribe(IConsumer consumer, Map<String, Object> paramMap) {
		// if consumer is listener object register it as listener
		if (consumer instanceof IPipeConnectionListener) {
			listeners.addIfAbsent((IPipeConnectionListener) consumer);
		}
		// pipe is possibly used by dozens of threads at once (like many subscribers for one server stream)
		return consumers.addIfAbsent(consumer);
	}

	/**
	 * Connect provider to this pipe. Doesn't allow to connect one provider twice.
	 * Does register event listeners if instance of IPipeConnectionListener is given.
	 *
	 * @param provider        Provider
	 * @param paramMap        Parameters passed with connection, used in concrete pipe implementations
	 * @return                <code>true</code> if provider was added, <code>false</code> otherwise
	 */
	public boolean subscribe(IProvider provider, Map<String, Object> paramMap) {
		// register event listener if given
		if (provider instanceof IPipeConnectionListener) {
			listeners.add((IPipeConnectionListener) provider);
		}
		return providers.addIfAbsent(provider);
	}

	/**
	 * Disconnects provider from this pipe. Fires pipe connection event.
	 * @param provider        Provider that should be removed
	 * @return                 <code>true</code> on success, <code>false</code> otherwise
	 */
	public boolean unsubscribe(IProvider provider) {
		if (providers.remove(provider)) {
			fireProviderConnectionEvent(provider, PipeConnectionEvent.PROVIDER_DISCONNECT, null);
			listeners.remove(provider);
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Disconnects consumer from this pipe. Fires pipe connection event.
	 * @param   consumer       Consumer that should be removed
	 * @return                 <code>true</code> on success, <code>false</code> otherwise
	 */
	public boolean unsubscribe(IConsumer consumer) {
		if (consumers.remove(consumer)) {
			fireConsumerConnectionEvent(consumer, PipeConnectionEvent.CONSUMER_DISCONNECT, null);
			listeners.remove(consumer);
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Registers pipe connect events listener
	 * @param listener      Listener
	 */
	public void addPipeConnectionListener(IPipeConnectionListener listener) {
		listeners.add(listener);
	}

	/**
	 * Removes pipe connection listener
	 * @param listener      Listener
	 */
	public void removePipeConnectionListener(IPipeConnectionListener listener) {
		listeners.remove(listener);
	}

	/**
	 * Send out-of-band ("special") control message to all consumers
	 *
	 * @param provider           Provider, may be used in concrete implementations
	 * @param oobCtrlMsg         Out-of-band control message
	 */
	public void sendOOBControlMessage(IProvider provider, OOBControlMessage oobCtrlMsg) {
		for (IConsumer consumer : consumers) {
			try {
				consumer.onOOBControlMessage(provider, this, oobCtrlMsg);
			} catch (Throwable t) {
				log.error("exception when passing OOBCM from provider to consumers", t);
			}
		}
	}

	/**
	 * Send out-of-band ("special") control message to all providers
	 *
	 * @param consumer          Consumer, may be used in concrete implementations
	 * @param oobCtrlMsg        Out-of-band control message
	 */
	public void sendOOBControlMessage(IConsumer consumer, OOBControlMessage oobCtrlMsg) {
		for (IProvider provider : providers) {
			try {
				provider.onOOBControlMessage(consumer, this, oobCtrlMsg);
			} catch (Throwable t) {
				log.error("exception when passing OOBCM from consumer to providers", t);
			}
		}
	}

	/**
	 * Getter for pipe connection events listeners
	 *
	 * @return  Listeners
	 */
	public List<IPipeConnectionListener> getListeners() {
		return Collections.unmodifiableList(listeners);
	}

	/**
	 * Setter for pipe connection events listeners
	 *
	 * @param newListeners  Listeners
	 */
	public void setListeners(List<IPipeConnectionListener> newListeners) {
		listeners.clear();
		listeners.addAll(newListeners);
	}

	/**
	 * Getter for providers
	 *
	 * @return  Providers list
	 */
	public List<IProvider> getProviders() {
		return Collections.unmodifiableList(providers);
	}

	/**
	 * Getter for consumers
	 *
	 * @return  consumers list
	 */
	public List<IConsumer> getConsumers() {
		return Collections.unmodifiableList(consumers);
	}

	/**
	 * Broadcast consumer connection event
	 *
	 * @param consumer        Consumer that has connected
	 * @param type            Event type
	 * @param paramMap        Parameters passed with connection
	 */
	protected void fireConsumerConnectionEvent(IConsumer consumer, int type, Map<String, Object> paramMap) {
		// Create event object
		PipeConnectionEvent event = new PipeConnectionEvent(this);
		// Fill it up
		event.setConsumer(consumer);
		event.setType(type);
		event.setParamMap(paramMap);
		// Fire it
		firePipeConnectionEvent(event);
	}

	/**
	 * Broadcast provider connection event
	 * @param provider        Provider that has connected
	 * @param type            Event type
	 * @param paramMap        Parameters passed with connection
	 */
	protected void fireProviderConnectionEvent(IProvider provider, int type, Map<String, Object> paramMap) {
		PipeConnectionEvent event = new PipeConnectionEvent(this);
		event.setProvider(provider);
		event.setType(type);
		event.setParamMap(paramMap);
		firePipeConnectionEvent(event);
	}

	/**
	 * Fire any pipe connection event and run all it's tasks
	 * @param event            Pipe connection event
	 */
	protected void firePipeConnectionEvent(PipeConnectionEvent event) {
		for (IPipeConnectionListener element : listeners) {
			try {
				element.onPipeConnectionEvent(event);
			} catch (Throwable t) {
				log.error("Exception when handling pipe connection event", t);
			}
		}
		if (taskExecutor == null) {
			taskExecutor = Executors.newCachedThreadPool(new CustomizableThreadFactory("Pipe-"));
		}
		// Run all of event's tasks
		for (Runnable task : event.getTaskList()) {
			try {
				taskExecutor.execute(task);
			} catch (Throwable t) {
				log.warn("Exception executing pipe task {}", t);
			}
		}
		// Clear event's tasks list
		event.getTaskList().clear();
	}

	/**
	 * Close the pipe
	 */
	public void close() {
		//clean up collections
		if (consumers != null) {
			consumers.clear();
			consumers = null;
		}
		if (providers != null) {
			providers.clear();
			providers = null;
		}
		if (listeners != null) {
			listeners.clear();
			listeners = null;
		}
	}

}
