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

package org.red5.server.net.filter;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.mina.core.session.IoEvent;
import org.apache.mina.filter.executor.DefaultIoEventSizeEstimator;
import org.apache.mina.filter.executor.IoEventQueueHandler;
import org.apache.mina.filter.executor.IoEventSizeEstimator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Throttles incoming or outgoing events. The basis for this version originated in the Apache MINA Project.
 * 
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class IoEventQueueThrottler implements IoEventQueueHandler {

	/** A logger for this class */
	private final static Logger logger = LoggerFactory.getLogger(IoEventQueueThrottler.class);

	/** The event size estimator instance */
	private final IoEventSizeEstimator eventSizeEstimator;

	private volatile int threshold;

	private final Semaphore lock;

	private final AtomicInteger counter;

	public IoEventQueueThrottler(int threshold, int permits) {
		this(new DefaultIoEventSizeEstimator(), threshold, permits);
		logger.info("IoEventQueueThrottle created");
	}

	public IoEventQueueThrottler(IoEventSizeEstimator eventSizeEstimator, int threshold, int permits) {
		if (eventSizeEstimator == null) {
			throw new IllegalArgumentException("eventSizeEstimator");
		}
		this.eventSizeEstimator = eventSizeEstimator;
		setThreshold(threshold);
		lock = new Semaphore(permits, true);
		counter = new AtomicInteger();
	}

	public IoEventSizeEstimator getEventSizeEstimator() {
		return eventSizeEstimator;
	}

	public int getThreshold() {
		return threshold;
	}

	public int getCounter() {
		return counter.get();
	}

	public void setThreshold(int threshold) {
		if (threshold <= 0) {
			throw new IllegalArgumentException("threshold: " + threshold);
		}
		this.threshold = threshold;
	}

	public boolean accept(Object source, IoEvent event) {
		return true;
	}

	public void offered(Object source, IoEvent event) {
		logger.debug("offered: {}", source.getClass().getName());	
		int eventSize = estimateSize(event);
		int currentCounter = counter.addAndGet(eventSize);
		logState();
		if (currentCounter >= threshold) {
			try {
				lock.acquire();
				Thread.sleep(1000L);
			} catch (InterruptedException e) {
			} finally {
				lock.release();				
			}
		}
	}

	public void polled(Object source, IoEvent event) {
		logger.debug("polled: {}", source.getClass().getName());	
		int eventSize = estimateSize(event);
		@SuppressWarnings("unused")
		int currentCounter = counter.addAndGet(-eventSize);
		logState();
//		if (currentCounter < threshold) {
//			lock.release();
//		}
	}

	private int estimateSize(IoEvent event) {
		int size = getEventSizeEstimator().estimateSize(event);
		if (size < 0) {
			throw new IllegalStateException(IoEventSizeEstimator.class.getSimpleName() + " returned a negative value (" + size + "): " + event);
		}
		return size;
	}

	private void logState() {
		logger.debug("Available permits: {} thread queue: {} state: {} threshold: {}", new Object[] {lock.availablePermits(), lock.getQueueLength(), counter.get(), getThreshold()});
	}

}
