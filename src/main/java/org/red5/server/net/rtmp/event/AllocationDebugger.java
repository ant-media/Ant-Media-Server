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

package org.red5.server.net.rtmp.event;


import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple allocation debugger for Event reference counting.
 * 
 * @author The Red5 Project
 * @author Steven Gong (steven.gong@gmail.com) on behalf of
 *         (ce@publishing-etc.de)
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class AllocationDebugger {
	
	/**
	 * Information on references count
	 */
	private static class Info {
		/**
		 * References count
		 */
		public AtomicInteger refcount = new AtomicInteger(1);

		/** Constructs a new Info. */
		public Info() {
		}

	}

	/**
	 * Allocation debugger istance
	 */
	private static AllocationDebugger instance = new AllocationDebugger();

	/**
	 * Logger
	 */
	private Logger log;

	/**
	 * Events-to-information objects map
	 */
	private ConcurrentMap<BaseEvent, Info> events;
	
	/**
	 * Getter for instance
	 * 
	 * @return Allocation debugger instance
	 */
	public static AllocationDebugger getInstance() {
		return instance;
	}

	/** Do not instantiate AllocationDebugger. */
	private AllocationDebugger() {
		log = LoggerFactory.getLogger(getClass());
		events = new ConcurrentHashMap<BaseEvent, Info>();
	}

	/**
	 * Add event to map
	 * 
	 * @param event
	 *            Event
	 */
	protected void create(BaseEvent event) {
		events.put(event, new Info());
	}

	/**
	 * Retain event
	 * 
	 * @param event
	 *            Event
	 */
	protected void retain(BaseEvent event) {
		Info info = events.get(event);
		if (info != null) {
			info.refcount.incrementAndGet();
		} else {
			log.warn("Retain called on already released event.");
		}
	}

	/**
	 * Release event if there's no more references to it
	 * 
	 * @param event
	 *            Event
	 */
	protected void release(BaseEvent event) {
		Info info = events.get(event);
		if (info != null) {
			if (info.refcount.decrementAndGet() == 0) {
				events.remove(event);
			}
		} else {
			log.warn("Release called on already released event.");
		}
	}

	/**
	 * Dumps allocations
	 */
	public synchronized void dump() {
		if (log.isDebugEnabled()) {
			log.debug("dumping allocations {}", events.size());
			for (Entry<BaseEvent, Info> entry : events.entrySet()) {
				log.debug("{} {}", entry.getKey(), entry.getValue().refcount);
			}
		}
	}

}
