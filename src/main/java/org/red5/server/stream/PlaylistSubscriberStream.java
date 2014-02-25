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

package org.red5.server.stream;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.red5.logging.Red5LoggerFactory;
import org.red5.server.api.IConnection;
import org.red5.server.api.IContext;
import org.red5.server.api.Red5;
import org.red5.server.api.scheduling.IScheduledJob;
import org.red5.server.api.scheduling.ISchedulingService;
import org.red5.server.api.scope.IScope;
import org.red5.server.api.statistics.IPlaylistSubscriberStreamStatistics;
import org.red5.server.api.stream.IPlayItem;
import org.red5.server.api.stream.IPlaylistController;
import org.red5.server.api.stream.IPlaylistSubscriberStream;
import org.red5.server.api.stream.IStreamAwareScopeHandler;
import org.red5.server.api.stream.OperationNotSupportedException;
import org.red5.server.api.stream.StreamState;
import org.slf4j.Logger;

/**
 * Stream of playlist subscriber
 */
public class PlaylistSubscriberStream extends AbstractClientStream implements IPlaylistSubscriberStream, IPlaylistSubscriberStreamStatistics {

	private static final Logger log = Red5LoggerFactory.getLogger(PlaylistSubscriberStream.class);

	private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();

	private final Lock read = readWriteLock.readLock();

	private final Lock write = readWriteLock.writeLock();

	/**
	 * Playlist controller
	 */
	private IPlaylistController controller;

	/**
	 * Default playlist controller
	 */
	private IPlaylistController defaultController;

	/**
	 * Playlist items
	 */
	private final LinkedList<IPlayItem> items;

	/**
	 * Current item index
	 */
	private int currentItemIndex = 0;

	/**
	 * Plays items back
	 */
	protected PlayEngine engine;

	/**
	 * Rewind mode state
	 */
	protected boolean rewind;

	/**
	 * Random mode state
	 */
	protected boolean random;

	/**
	 * Repeat mode state
	 */
	protected boolean repeat;

	/**
	 * Service used to provide notifications, keep client buffer filled, clean up, etc...
	 */
	protected ISchedulingService schedulingService;
	
	/** 
	 * Scheduled job names
	 */
	protected Set<String> jobs = new HashSet<String>(1);

	/**
	 * Interval in ms to check for buffer underruns in VOD streams.
	 */
	protected int bufferCheckInterval = 0;

	/**
	 * Number of pending messages at which a <code>NetStream.Play.InsufficientBW</code>
	 * message is generated for VOD streams.
	 */
	protected int underrunTrigger = 10;

	/**
	 * Timestamp this stream was created.
	 */
	protected long creationTime = System.currentTimeMillis();

	/**
	 * Number of bytes sent.
	 */
	protected long bytesSent = 0;

	/** Constructs a new PlaylistSubscriberStream. */
	public PlaylistSubscriberStream() {
		defaultController = new SimplePlaylistController();
		items = new LinkedList<IPlayItem>();
	}

	/**
	 * Creates a play engine based on current services (scheduling service, consumer service, and provider service).
	 * This method is useful during unit testing.
	 */
	PlayEngine createEngine(ISchedulingService schedulingService, IConsumerService consumerService, IProviderService providerService) {
		engine = new PlayEngine.Builder(this, schedulingService, consumerService, providerService).build();
		return engine;
	}

	/**
	 * Set interval to check for buffer underruns. Set to <code>0</code> to
	 * disable.
	 * 
	 * @param bufferCheckInterval interval in ms
	 */
	public void setBufferCheckInterval(int bufferCheckInterval) {
		this.bufferCheckInterval = bufferCheckInterval;
	}

	/**
	 * Set maximum number of pending messages at which a
	 * <code>NetStream.Play.InsufficientBW</code> message will be
	 * generated for VOD streams
	 * 
	 * @param underrunTrigger the maximum number of pending messages
	 */
	public void setUnderrunTrigger(int underrunTrigger) {
		this.underrunTrigger = underrunTrigger;
	}

	/** {@inheritDoc} */
	public void start() {
		//ensure the play engine exists
		if (engine == null) {
			IScope scope = getScope();
			if (scope != null) {
				IContext ctx = scope.getContext();
				if (ctx.hasBean(ISchedulingService.BEAN_NAME)) {
					schedulingService = (ISchedulingService) ctx.getBean(ISchedulingService.BEAN_NAME);
				} else {
					//try the parent
					schedulingService = (ISchedulingService) scope.getParent().getContext().getBean(ISchedulingService.BEAN_NAME);
				}
				IConsumerService consumerService = null;
				if (ctx.hasBean(IConsumerService.KEY)) {
					consumerService = (IConsumerService) ctx.getBean(IConsumerService.KEY);
				} else {
					//try the parent
					consumerService = (IConsumerService) scope.getParent().getContext().getBean(IConsumerService.KEY);
				}
				IProviderService providerService = null;
				if (ctx.hasBean(IProviderService.BEAN_NAME)) {
					providerService = (IProviderService) ctx.getBean(IProviderService.BEAN_NAME);
				} else {
					//try the parent
					providerService = (IProviderService) scope.getParent().getContext().getBean(IProviderService.BEAN_NAME);
				}

				engine = new PlayEngine.Builder(this, schedulingService, consumerService, providerService).build();
			} else {
				log.info("Scope was null on start");
			}
		}
		//set buffer check interval
		engine.setBufferCheckInterval(bufferCheckInterval);
		//set underrun trigger
		engine.setUnderrunTrigger(underrunTrigger);
		// Start playback engine
		engine.start();
		// Notify subscribers on start
		onChange(StreamState.STARTED);
	}

	/** {@inheritDoc} */
	public void play() throws IOException {
		// Check how many is yet to play...
		int count = items.size();
		// Return if playlist is empty
		if (count == 0) {
			return;
		}
		// Move to next if current item is set to -1
		if (currentItemIndex == -1) {
			moveToNext();
		}
		// If there's some more items on list then play current item
		while (count-- > 0) {
			IPlayItem item = null;
			read.lock();
			try {
				// Get playlist item
				item = items.get(currentItemIndex);
				engine.play(item);
				break;
			} catch (StreamNotFoundException e) {
				// go for next item
				moveToNext();
				if (currentItemIndex == -1) {
					// we reaches the end.
					break;
				}
				item = items.get(currentItemIndex);
			} catch (IllegalStateException e) {
				// an stream is already playing
				break;
			} finally {
				read.unlock();
			}
		}
	}

	/** {@inheritDoc} */
	public void pause(int position) {
		try {
			engine.pause(position);
		} catch (IllegalStateException e) {
			log.debug("pause caught an IllegalStateException");
		}
	}

	/** {@inheritDoc} */
	public void resume(int position) {
		try {
			engine.resume(position);
		} catch (IllegalStateException e) {
			log.debug("resume caught an IllegalStateException");
		}
	}

	/** {@inheritDoc} */
	public void stop() {
		try {
			engine.stop();
		} catch (IllegalStateException e) {
			log.debug("stop caught an IllegalStateException");
		}
	}

	/** {@inheritDoc} */
	public void seek(int position) throws OperationNotSupportedException {
		try {
			engine.seek(position);
		} catch (IllegalStateException e) {
			log.debug("seek caught an IllegalStateException");
		}
	}

	/** {@inheritDoc} */
	public void close() {
		if (engine != null) {
    		engine.close();
    		onChange(StreamState.CLOSED);
    		items.clear();
    		// clear jobs
    		if (schedulingService != null && !jobs.isEmpty()) {
    			for (String jobName : jobs) {
    				schedulingService.removeScheduledJob(jobName);
    			}
    			jobs.clear();
    		}
		}
	}

	/** {@inheritDoc} */
	public boolean isPaused() {
		return state == StreamState.PAUSED;
	}

	/** {@inheritDoc} */
	public void addItem(IPlayItem item) {
		write.lock();
		try {
			items.add(item);
		} finally {
			write.unlock();
		}
	}

	/** {@inheritDoc} */
	public void addItem(IPlayItem item, int index) {
		write.lock();
		try {
			items.add(index, item);
		} finally {
			write.unlock();
		}
	}

	/** {@inheritDoc} */
	public void removeItem(int index) {
		if (index < 0 || index >= items.size()) {
			return;
		}
		int originSize = items.size();
		write.lock();
		try {
			items.remove(index);
		} finally {
			write.unlock();
		}
		if (currentItemIndex == index) {
			// set the next item.
			if (index == originSize - 1) {
				currentItemIndex = index - 1;
			}
		}
	}

	/** {@inheritDoc} */
	public void removeAllItems() {
		// we try to stop the engine first
		stop();
		write.lock();
		try {
			items.clear();
		} finally {
			write.unlock();
		}
	}

	/** {@inheritDoc} */
	public void previousItem() {
		stop();
		moveToPrevious();
		if (currentItemIndex == -1) {
			return;
		}
		IPlayItem item = null;
		int count = items.size();
		while (count-- > 0) {
			read.lock();
			try {
				item = items.get(currentItemIndex);
				engine.play(item);
				break;
			} catch (IOException err) {
				log.error("Error while starting to play item, moving to previous.", err);
				// go for next item
				moveToPrevious();
				if (currentItemIndex == -1) {
					// we reaches the end.
					break;
				}
			} catch (StreamNotFoundException e) {
				// go for next item
				moveToPrevious();
				if (currentItemIndex == -1) {
					// we reaches the end.
					break;
				}
			} catch (IllegalStateException e) {
				// an stream is already playing
				break;
			} finally {
				read.unlock();
			}
		}
	}

	/** {@inheritDoc} */
	public boolean hasMoreItems() {
		int nextItem = currentItemIndex + 1;
		if (nextItem >= items.size() && !repeat) {
			return false;
		} else {
			return true;
		}
	}

	/** {@inheritDoc} */
	public void nextItem() {
		moveToNext();
		if (currentItemIndex == -1) {
			return;
		}
		IPlayItem item = null;
		int count = items.size();
		while (count-- > 0) {
			read.lock();
			try {
				item = items.get(currentItemIndex);
				engine.play(item, false);
				break;
			} catch (IOException err) {
				log.error("Error while starting to play item, moving to next", err);
				// go for next item
				moveToNext();
				if (currentItemIndex == -1) {
					// we reaches the end.
					break;
				}
			} catch (StreamNotFoundException e) {
				// go for next item
				moveToNext();
				if (currentItemIndex == -1) {
					// we reaches the end.
					break;
				}
			} catch (IllegalStateException e) {
				// an stream is already playing
				break;
			} finally {
				read.unlock();
			}
		}
	}

	/** {@inheritDoc} */
	public void setItem(int index) {
		if (index < 0 || index >= items.size()) {
			return;
		}
		stop();
		currentItemIndex = index;
		read.lock();
		try {
			IPlayItem item = items.get(currentItemIndex);
			engine.play(item);
		} catch (IOException e) {
			log.error("setItem caught a IOException", e);
		} catch (StreamNotFoundException e) {
			// let the engine retain the STOPPED state
			// and wait for control from outside
			log.debug("setItem caught a StreamNotFoundException");
		} catch (IllegalStateException e) {
			log.error("Illegal state exception on playlist item setup", e);
		} finally {
			read.unlock();
		}
	}

	/** {@inheritDoc} */
	public boolean isRandom() {
		return random;
	}

	/** {@inheritDoc} */
	public void setRandom(boolean random) {
		this.random = random;
	}

	/** {@inheritDoc} */
	public boolean isRewind() {
		return rewind;
	}

	/** {@inheritDoc} */
	public void setRewind(boolean rewind) {
		this.rewind = rewind;
	}

	/** {@inheritDoc} */
	public boolean isRepeat() {
		return repeat;
	}

	/** {@inheritDoc} */
	public void setRepeat(boolean repeat) {
		this.repeat = repeat;
	}

	/**
	 * Seek to current position to restart playback with audio and/or video.
	 */
	private void seekToCurrentPlayback() {
		if (engine.isPullMode()) {
			try {
				// TODO: figure out if this is the correct position to seek to
				final long delta = System.currentTimeMillis() - engine.getPlaybackStart();
				engine.seek((int) delta);
			} catch (OperationNotSupportedException err) {
				// Ignore error, should not happen for pullMode engines
			}
		}
	}

	/** {@inheritDoc} */
	public void receiveVideo(boolean receive) {
		boolean receiveVideo = engine.receiveVideo(receive);
		if (!receiveVideo && receive) {
			//video has been re-enabled
			seekToCurrentPlayback();
		}
	}

	/** {@inheritDoc} */
	public void receiveAudio(boolean receive) {
		//check if engine currently receives audio, returns previous value
		boolean receiveAudio = engine.receiveAudio(receive);
		if (receiveAudio && !receive) {
			//send a blank audio packet to reset the player
			engine.sendBlankAudio(true);
		} else if (!receiveAudio && receive) {
			//do a seek	
			seekToCurrentPlayback();
		}
	}

	/** {@inheritDoc} */
	public void setPlaylistController(IPlaylistController controller) {
		this.controller = controller;
	}

	/** {@inheritDoc} */
	public int getItemSize() {
		return items.size();
	}

	/** {@inheritDoc} */
	public int getCurrentItemIndex() {
		return currentItemIndex;
	}

	/**
	 * {@inheritDoc}
	 */
	public IPlayItem getCurrentItem() {
		return getItem(getCurrentItemIndex());
	}

	/** {@inheritDoc} */
	public IPlayItem getItem(int index) {
		read.lock();
		try {
			return items.get(index);
		} catch (IndexOutOfBoundsException e) {
			return null;
		} finally {
			read.unlock();
		}
	}

	/** {@inheritDoc} */
	public boolean replace(IPlayItem oldItem, IPlayItem newItem) {
		boolean result = false;
		read.lock();
		try {
			int index = items.indexOf(oldItem);
			items.remove(index);
			items.set(index, newItem);
			result = true;
		} catch (Exception e) {

		} finally {
			read.unlock();
		}
		return result;
	}

	/**
	 * Move the current item to the next in list.
	 */
	private void moveToNext() {
		if (controller != null) {
			currentItemIndex = controller.nextItem(this, currentItemIndex);
		} else {
			currentItemIndex = defaultController.nextItem(this, currentItemIndex);
		}
	}

	/**
	 * Move the current item to the previous in list.
	 */
	private void moveToPrevious() {
		if (controller != null) {
			currentItemIndex = controller.previousItem(this, currentItemIndex);
		} else {
			currentItemIndex = defaultController.previousItem(this, currentItemIndex);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void onChange(final StreamState state, final Object... changed) {
		Notifier notifier = null;
		IStreamAwareScopeHandler handler = getStreamAwareHandler();
		switch (state) {
			case SEEK:
				//notifies subscribers on seek
				if (handler != null) {
					notifier = new Notifier(this, handler) {
						public void execute(ISchedulingService service) {
							//make sure those notified have the correct connection
							Red5.setConnectionLocal(conn);
							//get item being played
							IPlayItem item = (IPlayItem) changed[0];
							//seek position
							int position = (Integer) changed[1];
							try {
								handler.streamPlayItemSeek(stream, item, position);
							} catch (Throwable t) {
								log.error("error notify streamPlayItemSeek", t);
							}
							// clear thread local reference
							Red5.setConnectionLocal(null);
						}
					};
				}
				break;
			case PAUSED:
				//set the paused state
				this.setState(StreamState.PAUSED);
				//notifies subscribers on pause
				if (handler != null) {
					notifier = new Notifier(this, handler) {
						public void execute(ISchedulingService service) {
							//make sure those notified have the correct connection
							Red5.setConnectionLocal(conn);
							//get item being played
							IPlayItem item = (IPlayItem) changed[0];
							//playback position
							int position = (Integer) changed[1];
							try {
								handler.streamPlayItemPause(stream, item, position);
							} catch (Throwable t) {
								log.error("error notify streamPlayItemPause", t);
							}
							// clear thread local reference
							Red5.setConnectionLocal(null);
						}
					};
				}
				break;
			case RESUMED:
				//resume playing
				this.setState(StreamState.PLAYING);
				//notifies subscribers on resume
				if (handler != null) {
					notifier = new Notifier(this, handler) {
						public void execute(ISchedulingService service) {
							//make sure those notified have the correct connection
							Red5.setConnectionLocal(conn);
							//get item being played
							IPlayItem item = (IPlayItem) changed[0];
							//playback position
							int position = (Integer) changed[1];
							try {
								handler.streamPlayItemResume(stream, item, position);
							} catch (Throwable t) {
								log.error("error notify streamPlayItemResume", t);
							}
							// clear thread local reference
							Red5.setConnectionLocal(null);
						}
					};
				}
				break;
			case PLAYING:
				//notifies subscribers on play
				if (handler != null) {
					notifier = new Notifier(this, handler) {
						public void execute(ISchedulingService service) {
							//make sure those notified have the correct connection
							Red5.setConnectionLocal(conn);
							//get item being played
							IPlayItem item = (IPlayItem) changed[0];
							//is it a live broadcast
							boolean isLive = (Boolean) changed[1];
							try {
								handler.streamPlayItemPlay(stream, item, isLive);
							} catch (Throwable t) {
								log.error("error notify streamPlayItemPlay", t);
							}
							// clear thread local reference
							Red5.setConnectionLocal(null);
						}
					};
				}
				break;
			case CLOSED:
				//notifies subscribers on close
				if (handler != null) {
					notifier = new Notifier(this, handler) {
						public void execute(ISchedulingService service) {
							//make sure those notified have the correct connection
							Red5.setConnectionLocal(conn);
							try {
								handler.streamSubscriberClose(stream);
							} catch (Throwable t) {
								log.error("error notify streamSubscriberClose", t);
							}
							// clear thread local reference
							Red5.setConnectionLocal(null);
						}
					};
				}
				break;
			case STARTED:
				//notifies subscribers on start
				if (handler != null) {
					notifier = new Notifier(this, handler) {
						public void execute(ISchedulingService service) {
							//make sure those notified have the correct connection
							Red5.setConnectionLocal(conn);
							try {
								handler.streamSubscriberStart(stream);
							} catch (Throwable t) {
								log.error("error notify streamSubscriberStart", t);
							}
							// clear thread local reference
							Red5.setConnectionLocal(null);
						}
					};
				}
				break;
			case STOPPED:
				//set the stopped state
				this.setState(StreamState.STOPPED);
				//notifies subscribers on stop
				if (handler != null) {
					notifier = new Notifier(this, handler) {
						public void execute(ISchedulingService service) {
							//make sure those notified have the correct connection
							Red5.setConnectionLocal(conn);
							//get the item that was stopped
							IPlayItem item = (IPlayItem) changed[0];
							try {
								handler.streamPlayItemStop(stream, item);
							} catch (Throwable t) {
								log.error("error notify streamPlaylistItemStop", t);
							}
							// clear thread local reference
							Red5.setConnectionLocal(null);
						}
					};
				}
				break;
			case END:
				//notified by the play engine when the current item reaches the end
				nextItem();
				break;
			default:
				//there is no "default" handling
				log.warn("Unhandled change: {}", state);
		}
		if (notifier != null) {
			IConnection conn = Red5.getConnectionLocal();
			notifier.setConnection(conn);
			scheduleOnceJob(notifier);
		}
	}

	/** {@inheritDoc} */
	public IPlaylistSubscriberStreamStatistics getStatistics() {
		return this;
	}

	/** {@inheritDoc} */
	public long getCreationTime() {
		return creationTime;
	}

	/** {@inheritDoc} */
	public int getCurrentTimestamp() {
		int lastMessageTs = engine.getLastMessageTimestamp();
		if (lastMessageTs >= 0) {
			return 0;
		}
		return lastMessageTs;
	}

	/** {@inheritDoc} */
	public long getBytesSent() {
		return bytesSent;
	}

	/** {@inheritDoc} */
	public double getEstimatedBufferFill() {
		// check to see if any messages have been sent
		int lastMessageTs = engine.getLastMessageTimestamp();
		if (lastMessageTs < 0) {
			// nothing has been sent yet
			return 0.0;
		}
		// buffer size as requested by the client
		final long buffer = getClientBufferDuration();
		if (buffer == 0) {
			return 100.0;
		}
		// duration the stream is playing
		final long delta = System.currentTimeMillis() - engine.getPlaybackStart();
		// expected amount of data present in client buffer
		final long buffered = lastMessageTs - delta;
		return (buffered * 100.0) / buffer;
	}

	/** {@inheritDoc} */
	public String scheduleOnceJob(IScheduledJob job) {
		String jobName = schedulingService.addScheduledOnceJob(10, job);
		return jobName;
	}

	/** {@inheritDoc} */
	public String scheduleWithFixedDelay(IScheduledJob job, int interval) {
		String jobName = schedulingService.addScheduledJob(interval, job);
		jobs.add(jobName);
		return jobName;
	}	

	/** {@inheritDoc} */
	public void cancelJob(String jobName) {
		schedulingService.removeScheduledJob(jobName);
	}	
	
	/**
	 * Handles notifications in a separate thread.
	 */
	public class Notifier implements IScheduledJob {

		IPlaylistSubscriberStream stream;

		IStreamAwareScopeHandler handler;

		IConnection conn;

		public Notifier(IPlaylistSubscriberStream stream, IStreamAwareScopeHandler handler) {
			log.trace("Notifier - stream: {} handler: {}", stream, handler);
			this.stream = stream;
			this.handler = handler;
		}

		public void setConnection(IConnection conn) {
			this.conn = conn;
		}

		public void execute(ISchedulingService service) {
		}

	}

}
