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

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.filterchain.IoFilter;
import org.apache.mina.core.filterchain.IoFilterAdapter;
import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.session.AttributeKey;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.session.IoSessionConfig;
import org.apache.mina.core.write.WriteRequest;
import org.apache.mina.filter.executor.IoEventSizeEstimator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An {@link IoFilter} that limits bandwidth (bytes per second) related with
 * read and write operations on a per-session basis.
 * <p>
 * It is always recommended to add this filter in the first place of the
 * {@link IoFilterChain}.
 * 
 * <br />
 * This originated from the Mina sandbox.
 */
public class TrafficShapingFilter extends IoFilterAdapter {

	protected static Logger log = LoggerFactory.getLogger(TrafficShapingFilter.class);

	private final AttributeKey STATE = new AttributeKey(getClass(), "state");

	private final ScheduledExecutorService scheduledExecutor;

	private final DefaultMessageSizeEstimator messageSizeEstimator;

	private volatile int maxReadThroughput;

	private volatile int maxWriteThroughput;

	private volatile int poolSize = 1;

	public TrafficShapingFilter(int maxReadThroughput, int maxWriteThroughput) {
		this(null, null, maxReadThroughput, maxWriteThroughput);
	}

	public TrafficShapingFilter(ScheduledExecutorService scheduledExecutor, int maxReadThroughput, int maxWriteThroughput) {
		this(scheduledExecutor, null, maxReadThroughput, maxWriteThroughput);
	}

	public TrafficShapingFilter(ScheduledExecutorService scheduledExecutor, DefaultMessageSizeEstimator messageSizeEstimator, int maxReadThroughput, int maxWriteThroughput) {

		log.debug("ctor - executor: {} estimator: {} max read: {} max write: {}", new Object[] { scheduledExecutor, messageSizeEstimator, maxReadThroughput, maxWriteThroughput });

		if (scheduledExecutor == null) {
			scheduledExecutor = new ScheduledThreadPoolExecutor(poolSize);
			//throw new NullPointerException("scheduledExecutor");
		}

		if (messageSizeEstimator == null) {
			messageSizeEstimator = new DefaultMessageSizeEstimator() {
				@Override
				public int estimateSize(Object message) {
					if (message instanceof IoBuffer) {
						return ((IoBuffer) message).remaining();
					}
					return super.estimateSize(message);
				}
			};
		}

		this.scheduledExecutor = scheduledExecutor;
		this.messageSizeEstimator = messageSizeEstimator;
		setMaxReadThroughput(maxReadThroughput);
		setMaxWriteThroughput(maxWriteThroughput);
	}

	public ScheduledExecutorService getScheduledExecutor() {
		return scheduledExecutor;
	}

	public IoEventSizeEstimator getMessageSizeEstimator() {
		return messageSizeEstimator;
	}

	public int getMaxReadThroughput() {
		return maxReadThroughput;
	}

	public void setMaxReadThroughput(int maxReadThroughput) {
		if (maxReadThroughput < 0) {
			maxReadThroughput = 0;
		}
		this.maxReadThroughput = maxReadThroughput;
	}

	public int getMaxWriteThroughput() {
		return maxWriteThroughput;
	}

	public void setMaxWriteThroughput(int maxWriteThroughput) {
		if (maxWriteThroughput < 0) {
			maxWriteThroughput = 0;
		}
		this.maxWriteThroughput = maxWriteThroughput;
	}

	public int getPoolSize() {
		return poolSize;
	}

	public void setPoolSize(int poolSize) {
		if (poolSize < 1) {
			poolSize = 1;
		}
		this.poolSize = poolSize;
	}

	@Override
	public void onPreAdd(IoFilterChain parent, String name, NextFilter nextFilter) throws Exception {
		if (parent.contains(this)) {
			throw new IllegalArgumentException("You can't add the same filter instance more than once.  Create another instance and add it.");
		}
		parent.getSession().setAttribute(STATE, new State());
		adjustReadBufferSize(parent.getSession());
	}

	@Override
	public void onPostRemove(IoFilterChain parent, String name, NextFilter nextFilter) throws Exception {
		parent.getSession().removeAttribute(STATE);
	}

	@Override
	public void messageReceived(NextFilter nextFilter, final IoSession session, Object message) throws Exception {

		int maxReadThroughput = this.maxReadThroughput;
		//process the request if our max is greater than zero
		if (maxReadThroughput > 0) {
			final State state = (State) session.getAttribute(STATE);
			long currentTime = System.currentTimeMillis();

			long suspendTime = 0;
			boolean firstRead = false;
			synchronized (state) {
				state.readBytes += messageSizeEstimator.estimateSize(message);

				if (!state.suspendedRead) {
					if (state.readStartTime == 0) {
						firstRead = true;
						state.readStartTime = currentTime - 1000;
					}

					long throughput = (state.readBytes * 1000 / (currentTime - state.readStartTime));
					if (throughput >= maxReadThroughput) {
						suspendTime = Math.max(0, state.readBytes * 1000 / maxReadThroughput - (firstRead ? 0 : currentTime - state.readStartTime));

						state.readBytes = 0;
						state.readStartTime = 0;
						state.suspendedRead = suspendTime != 0;

						adjustReadBufferSize(session);
					}
				}
			}

			if (suspendTime != 0) {
				session.suspendRead();
				scheduledExecutor.schedule(new Runnable() {
					public void run() {
						synchronized (state) {
							state.suspendedRead = false;
						}
						session.resumeRead();
					}
				}, suspendTime, TimeUnit.MILLISECONDS);
			}
		}

		nextFilter.messageReceived(session, message);

	}

	private void adjustReadBufferSize(IoSession session) {
		int maxReadThroughput = this.maxReadThroughput;
		if (maxReadThroughput == 0) {
			return;
		}
		IoSessionConfig config = session.getConfig();
		if (config.getReadBufferSize() > maxReadThroughput) {
			config.setReadBufferSize(maxReadThroughput);
		}
		if (config.getMaxReadBufferSize() > maxReadThroughput) {
			config.setMaxReadBufferSize(maxReadThroughput);
		}
	}

	@Override
	public void messageSent(NextFilter nextFilter, final IoSession session, WriteRequest writeRequest) throws Exception {

		int maxWriteThroughput = this.maxWriteThroughput;
		//process the request if our max is greater than zero
		if (maxWriteThroughput > 0) {
			final State state = (State) session.getAttribute(STATE);
			long currentTime = System.currentTimeMillis();

			long suspendTime = 0;
			boolean firstWrite = false;
			synchronized (state) {
				state.writtenBytes += messageSizeEstimator.estimateSize(writeRequest.getMessage());
				if (!state.suspendedWrite) {
					if (state.writeStartTime == 0) {
						firstWrite = true;
						state.writeStartTime = currentTime - 1000;
					}

					long throughput = (state.writtenBytes * 1000 / (currentTime - state.writeStartTime));
					if (throughput >= maxWriteThroughput) {
						suspendTime = Math.max(0, state.writtenBytes * 1000 / maxWriteThroughput - (firstWrite ? 0 : currentTime - state.writeStartTime));

						state.writtenBytes = 0;
						state.writeStartTime = 0;
						state.suspendedWrite = suspendTime != 0;
					}
				}
			}

			if (suspendTime != 0) {
				log.trace("Suspending write");
				session.suspendWrite();
				scheduledExecutor.schedule(new Runnable() {
					public void run() {
						synchronized (state) {
							state.suspendedWrite = false;
						}
						session.resumeWrite();
						log.trace("Resuming write");
					}
				}, suspendTime, TimeUnit.MILLISECONDS);
			}
		}

		nextFilter.messageSent(session, writeRequest);

	}

	private static class State {
		private long readStartTime;

		private long writeStartTime;

		private boolean suspendedRead;

		private boolean suspendedWrite;

		private long readBytes;

		private long writtenBytes;
	}
}
