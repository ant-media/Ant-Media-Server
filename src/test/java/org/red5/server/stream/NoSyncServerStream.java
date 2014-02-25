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

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

import org.red5.server.api.scheduling.IScheduledJob;
import org.red5.server.api.scheduling.ISchedulingService;
import org.red5.server.api.scope.IScope;
import org.red5.server.api.stream.IPlayItem;
import org.red5.server.api.stream.IPlaylistController;
import org.red5.server.api.stream.IServerStream;
import org.red5.server.api.stream.IStreamFilenameGenerator;
import org.red5.server.api.stream.IStreamFilenameGenerator.GenerationType;
import org.red5.server.api.stream.IStreamListener;
import org.red5.server.api.stream.IStreamPacket;
import org.red5.server.api.stream.ResourceExistException;
import org.red5.server.api.stream.ResourceNotFoundException;
import org.red5.server.messaging.IFilter;
import org.red5.server.messaging.IMessage;
import org.red5.server.messaging.IMessageComponent;
import org.red5.server.messaging.IMessageInput;
import org.red5.server.messaging.IMessageOutput;
import org.red5.server.messaging.IPassive;
import org.red5.server.messaging.IPipe;
import org.red5.server.messaging.IPipeConnectionListener;
import org.red5.server.messaging.IProvider;
import org.red5.server.messaging.IPushableConsumer;
import org.red5.server.messaging.InMemoryPushPushPipe;
import org.red5.server.messaging.OOBControlMessage;
import org.red5.server.messaging.PipeConnectionEvent;
import org.red5.server.net.rtmp.event.AudioData;
import org.red5.server.net.rtmp.event.IRTMPEvent;
import org.red5.server.net.rtmp.event.VideoData;
import org.red5.server.stream.consumer.FileConsumer;
import org.red5.server.stream.message.RTMPMessage;
import org.red5.server.stream.message.ResetMessage;
import org.red5.server.util.ScopeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;

/**
 * An implementation for server side stream.
 * 
 * @author The Red5 Project
 */
public class NoSyncServerStream extends AbstractStream implements IServerStream, IFilter, IPushableConsumer, IPipeConnectionListener {
	/**
	 * Enumeration for states
	 */
	private enum State {
		CLOSED, PLAYING, STOPPED, UNINIT, PAUSED
	}

	/**
	 * Logger
	 */
	private static final Logger log = LoggerFactory.getLogger(NoSyncServerStream.class);

	/**
	 * Actual playlist controller
	 */
	private IPlaylistController controller;

	/**
	 * Current item
	 */
	private IPlayItem currentItem;

	/**
	 * Current item index
	 */
	private int currentItemIndex;

	/**
	 * Default playlist controller
	 */
	private IPlaylistController defaultController;

	/**
	 * Random flag state
	 */
	private boolean isRandom;

	/**
	 * Repeat flag state
	 */
	private boolean isRepeat;

	/**
	 * Rewind flag state
	 */
	private boolean isRewind;

	/**
	 * List of items in this playlist
	 */
	private List<IPlayItem> items;

	/**
	 * Live broadcasting scheduled job name
	 */
	private String liveJobName;

	/**
	 * Message input
	 */
	private IMessageInput msgIn;

	/**
	 * Message output
	 */
	private IMessageOutput msgOut;

	/**
	 * Next msg's audio timestamp
	 */
	private long nextAudioTS;

	/**
	 * Next msg's data timestamp
	 */
	private long nextDataTS;

	/**
	 * Next RTMP message
	 */
	private RTMPMessage nextRTMPMessage;

	/**
	 * Next msg's timestamp
	 */
	private long nextTS;

	/**
	 * Next msg's video timestamp
	 */
	private long nextVideoTS;

	/**
	 * Stream published name
	 */
	private String publishedName;

	/**
	 * The filename we are recording to.
	 */
	private String recordingFilename;

	/**
	 * Pipe for recording
	 */
	private IPipe recordPipe;

	/**
	 * Scheduling service
	 */
	private ISchedulingService scheduler;

	/**
	 * Server start timestamp
	 */
	private long serverStartTS;

	/**
	 * Current state
	 */
	private State state;

	/**
	 * VOD scheduled job name
	 */
	private String vodJobName;

	/**
	 * VOD start timestamp
	 */
	private long vodStartTS;

	/** Listeners to get notified about received packets. */
	private Set<IStreamListener> listeners = new CopyOnWriteArraySet<IStreamListener>();

	/** Constructs a new ServerStream. */
	public NoSyncServerStream() {
		defaultController = new SimplePlaylistController();
		items = new CopyOnWriteArrayList<IPlayItem>();
		state = State.UNINIT;
	}

	/** {@inheritDoc} */
	public void addItem(IPlayItem item) {
		items.add(item);
	}

	/** {@inheritDoc} */
	public void addItem(IPlayItem item, int index) {
		items.add(index, item);
	}

	/** {@inheritDoc} */
	public void close() {
		if (state == State.PLAYING || state == State.PAUSED) {
			stop();
		}
		if (msgOut != null) {
			msgOut.unsubscribe(this);
		}
		recordPipe.unsubscribe((IProvider) this);
		state = State.CLOSED;
	}

	/** {@inheritDoc} */
	public IPlayItem getCurrentItem() {
		return currentItem;
	}

	/** {@inheritDoc} */
	public int getCurrentItemIndex() {
		return currentItemIndex;
	}

	/** {@inheritDoc} */
	public IPlayItem getItem(int index) {
		try {
			return items.get(index);
		} catch (IndexOutOfBoundsException e) {
			return null;
		}
	}

	/** {@inheritDoc} */
	public int getItemSize() {
		return items.size();
	}

	/**
	 * Getter for next RTMP message.
	 * 
	 * @return Next RTMP message
	 */
	private RTMPMessage getNextRTMPMessage() {
		IMessage message;
		do {
			// Pull message from message input object...
			try {
				message = msgIn.pullMessage();
			} catch (IOException err) {
				log.error("Error while pulling message.", err);
				message = null;
			}
			// If message is null then return null
			if (message == null) {
				return null;
			}
		} while (!(message instanceof RTMPMessage));
		// Cast and return
		return (RTMPMessage) message;
	}

	/** {@inheritDoc} */
	public IProvider getProvider() {
		return this;
	}

	/** {@inheritDoc} */
	public String getPublishedName() {
		return publishedName;
	}

	/** {@inheritDoc} */
	public String getSaveFilename() {
		return recordingFilename;
	}

	/** {@inheritDoc} */
	public boolean hasMoreItems() {
		int nextItem = currentItemIndex + 1;
		if (nextItem >= items.size() && !isRepeat) {
			return false;
		} else {
			return true;
		}
	}

	/** {@inheritDoc} */
	public boolean isRandom() {
		return isRandom;
	}

	/** {@inheritDoc} */
	public boolean isRepeat() {
		return isRepeat;
	}

	/** {@inheritDoc} */
	public boolean isRewind() {
		return isRewind;
	}

	/**
	 * Move to the next item updating the currentItemIndex. Should be called
	 * in context.
	 */
	private void moveToNext() {
		if (currentItemIndex >= items.size()) {
			currentItemIndex = items.size() - 1;
		}
		if (controller != null) {
			currentItemIndex = controller.nextItem(this, currentItemIndex);
		} else {
			currentItemIndex = defaultController.nextItem(this, currentItemIndex);
		}
	}

	/**
	 * Move to the previous item updating the currentItemIndex. Should be
	 * called in context.
	 */
	private void moveToPrevious() {
		if (currentItemIndex >= items.size()) {
			currentItemIndex = items.size() - 1;
		}
		if (controller != null) {
			currentItemIndex = controller.previousItem(this, currentItemIndex);
		} else {
			currentItemIndex = defaultController.previousItem(this, currentItemIndex);
		}
	}

	/** {@inheritDoc} */
	public void nextItem() {
		stop();
		moveToNext();
		if (currentItemIndex == -1) {
			return;
		}
		IPlayItem item = items.get(currentItemIndex);
		play(item);
	}

	/**
	 * Play next item on item end
	 */
	private void onItemEnd() {
		nextItem();
	}

	/** {@inheritDoc} */
	public void onOOBControlMessage(IMessageComponent source, IPipe pipe, OOBControlMessage oobCtrlMsg) {
	}

	/**
	 * Pipe connection event handler. There are two types of pipe connection
	 * events so far, provider push connection event and provider
	 * disconnection event.
	 * 
	 * Pipe events handling is the most common way of working with pipes.
	 * 
	 * @param event Pipe connection event context
	 */
	public void onPipeConnectionEvent(PipeConnectionEvent event) {
		switch (event.getType()) {
			case PipeConnectionEvent.PROVIDER_CONNECT_PUSH:
				if (event.getProvider() == this && (event.getParamMap() == null || !event.getParamMap().containsKey("record"))) {
					this.msgOut = (IMessageOutput) event.getSource();
				}
				break;
			case PipeConnectionEvent.PROVIDER_DISCONNECT:
				if (this.msgOut == event.getSource()) {
					this.msgOut = null;
				}
				break;
			default:
				break;
		}
	}

	/**
	 * Play a specific IPlayItem. The strategy for now is VOD first, Live
	 * second. Should be called in a context.
	 * 
	 * @param item
	 *                Item to play
	 */
	private void play(IPlayItem item) {
		// Return if already playing
		if (state != State.STOPPED) {
			return;
		}
		// Assume this is not live stream
		boolean isLive = false;
		// Get provider service from Spring bean factory
		IProviderService providerService = (IProviderService) getScope().getContext().getBean(IProviderService.BEAN_NAME);
		msgIn = providerService.getVODProviderInput(getScope(), item.getName());
		if (msgIn == null) {
			msgIn = providerService.getLiveProviderInput(getScope(), item.getName(), true);
			isLive = true;
		}
		if (msgIn == null) {
			log.warn("ABNORMAL Can't get both VOD and Live input from providerService");
			return;
		}
		state = State.PLAYING;
		currentItem = item;
		sendResetMessage();
		msgIn.subscribe(this, null);
		if (isLive) {
			if (item.getLength() >= 0) {
				liveJobName = scheduler.addScheduledOnceJob(item.getLength(), new IScheduledJob() {
					/** {@inheritDoc} */
					public void execute(ISchedulingService service) {
						if (liveJobName == null) {
							return;
						}
						liveJobName = null;
						onItemEnd();
					}
				});
			}
		} else {
			long start = item.getStart();
			if (start < 0) {
				start = 0;
			}
			sendVODInitCM(msgIn, (int) start);
			startBroadcastVOD();
		}
	}

	/** {@inheritDoc} */
	public void previousItem() {
		stop();
		moveToPrevious();
		if (currentItemIndex == -1) {
			return;
		}
		IPlayItem item = items.get(currentItemIndex);
		play(item);
	}

	/**
	 * Push message
	 * 
	 * @param message
	 *                Message
	 */
	private void pushMessage(IMessage message) throws IOException {
		msgOut.pushMessage(message);
		recordPipe.pushMessage(message);

		// Notify listeners about received packet
		if (message instanceof RTMPMessage) {
			final IRTMPEvent rtmpEvent = ((RTMPMessage) message).getBody();
			if (rtmpEvent instanceof IStreamPacket) {
				for (IStreamListener listener : getStreamListeners()) {
					try {
						listener.packetReceived(this, (IStreamPacket) rtmpEvent);
					} catch (Exception e) {
						log.error("Error while notifying listener " + listener, e);
					}
				}
			}
		}
	}

	/** {@inheritDoc} */
	public void pushMessage(IPipe pipe, IMessage message) throws IOException {
		pushMessage(message);
	}

	/** {@inheritDoc} */
	public void removeAllItems() {
		items.clear();
	}

	/** {@inheritDoc} */
	public void removeItem(int index) {
		if (index < 0 || index >= items.size()) {
			return;
		}
		items.remove(index);
	}

	/** {@inheritDoc} */
	public void saveAs(String name, boolean isAppend) throws IOException, ResourceNotFoundException, ResourceExistException {
		try {
			IScope scope = getScope();
			IStreamFilenameGenerator generator = (IStreamFilenameGenerator) ScopeUtils.getScopeService(scope, IStreamFilenameGenerator.class, DefaultStreamFilenameGenerator.class);

			String filename = generator.generateFilename(scope, name, ".flv", GenerationType.RECORD);
			Resource res = scope.getContext().getResource(filename);
			if (!isAppend) {
				if (res.exists()) {
					// Per livedoc of FCS/FMS:
					// When "live" or "record" is used,
					// any previously recorded stream with the same stream
					// URI is deleted.
					if (!res.getFile().delete())
						throw new IOException("file could not be deleted");
				}
			} else {
				if (!res.exists()) {
					// Per livedoc of FCS/FMS:
					// If a recorded stream at the same URI does not already
					// exist,
					// "append" creates the stream as though "record" was
					// passed.
					isAppend = false;
				}
			}

			if (!res.exists()) {
				// Make sure the destination directory exists
				try {
					String path = res.getFile().getAbsolutePath();
					int slashPos = path.lastIndexOf(File.separator);
					if (slashPos != -1) {
						path = path.substring(0, slashPos);
					}
					File tmp = new File(path);
					if (!tmp.isDirectory()) {
						tmp.mkdirs();
					}
				} catch (IOException err) {
					log.error("Could not create destination directory.", err);
				}
				res = scope.getResource(filename);
			}

			if (!res.exists()) {
				if (!res.getFile().canWrite()) {
					log.warn("File cannot be written to " + res.getFile().getCanonicalPath());
				}
				res.getFile().createNewFile();
			}
			FileConsumer fc = new FileConsumer(scope, res.getFile());
			Map<String, Object> paramMap = new HashMap<String, Object>();
			if (isAppend) {
				paramMap.put("mode", "append");
			} else {
				paramMap.put("mode", "record");
			}
			if (null == recordPipe) {
				recordPipe = new InMemoryPushPushPipe();
			}
			recordPipe.subscribe(fc, paramMap);
			recordingFilename = filename;
		} catch (IOException e) {
			log.warn("Save as exception", e);
		}
	}

	/**
	 * Pull the next message from IMessageInput and schedule it for push
	 * according to the timestamp.
	 */
	private void scheduleNextMessage() {
		boolean first = nextRTMPMessage == null;

		nextRTMPMessage = getNextRTMPMessage();
		if (nextRTMPMessage == null) {
			onItemEnd();
			return;
		}

		IRTMPEvent rtmpEvent = null;

		if (first) {
			rtmpEvent = nextRTMPMessage.getBody();
			// FIXME hack the first Metadata Tag from FLVReader
			// the FLVReader will issue a metadata tag of ts 0
			// even if it is seeked to somewhere in the middle
			// which will cause the stream to wait too long.
			// Is this an FLVReader's bug?
			if (!(rtmpEvent instanceof VideoData) && !(rtmpEvent instanceof AudioData) && rtmpEvent.getTimestamp() == 0) {
				rtmpEvent.release();
				nextRTMPMessage = getNextRTMPMessage();
				if (nextRTMPMessage == null) {
					onItemEnd();
					return;
				}
			}
		}

		rtmpEvent = nextRTMPMessage.getBody();
		if (rtmpEvent instanceof VideoData) {
			nextVideoTS = rtmpEvent.getTimestamp();
			nextTS = nextVideoTS;
		} else if (rtmpEvent instanceof AudioData) {
			nextAudioTS = rtmpEvent.getTimestamp();
			nextTS = nextAudioTS;
		} else {
			nextDataTS = rtmpEvent.getTimestamp();
			nextTS = nextDataTS;
		}
		if (first) {
			vodStartTS = nextTS;
		}
		long delta = nextTS - vodStartTS - (System.currentTimeMillis() - serverStartTS);

		vodJobName = scheduler.addScheduledOnceJob(delta, new IScheduledJob() {
			/** {@inheritDoc} */
			public void execute(ISchedulingService service) {
				if (vodJobName == null) {
					return;
				}
				vodJobName = null;
				try {
					pushMessage(nextRTMPMessage);
				} catch (IOException err) {
					log.error("Error while sending message.", err);
				}
				nextRTMPMessage.getBody().release();
				long start = currentItem.getStart();
				if (start < 0) {
					start = 0;
				}
				if (currentItem.getLength() >= 0 && nextTS - currentItem.getStart() > currentItem.getLength()) {
					onItemEnd();
					return;
				}
				scheduleNextMessage();
			}
		});
	}

	/**
	 * Send reset message
	 */
	private void sendResetMessage() {
		// Send new reset message
		try {
			pushMessage(new ResetMessage());
		} catch (IOException err) {
			log.error("Error while sending reset message.", err);
		}
	}

	/**
	 * Send VOD initialization control message
	 * 
	 * @param msgIn
	 *                Message input
	 * @param start
	 *                Start timestamp
	 */
	private void sendVODInitCM(IMessageInput msgIn, int start) {
		// Create new out-of-band control message
		OOBControlMessage oobCtrlMsg = new OOBControlMessage();
		// Set passive type
		oobCtrlMsg.setTarget(IPassive.KEY);
		// Set service name of init
		oobCtrlMsg.setServiceName("init");
		// Create map for parameters
		Map<String, Object> paramMap = new HashMap<String, Object>();
		// Put start timestamp into Map of params
		paramMap.put("startTS", start);
		// Attach to OOB control message and send it
		oobCtrlMsg.setServiceParamMap(paramMap);
		msgIn.sendOOBControlMessage(this, oobCtrlMsg);
	}

	/** {@inheritDoc} */
	public void setItem(int index) {
		if (index < 0 || index >= items.size()) {
			return;
		}
		stop();
		currentItemIndex = index;
		IPlayItem item = items.get(currentItemIndex);
		play(item);
	}

	/** {@inheritDoc} */
	public void setPlaylistController(IPlaylistController controller) {
		this.controller = controller;
	}

	/** {@inheritDoc} */
	public void setPublishedName(String name) {
		publishedName = name;
	}

	/** {@inheritDoc} */
	public void setRandom(boolean random) {
		isRandom = random;
	}

	/** {@inheritDoc} */
	public void setRepeat(boolean repeat) {
		isRepeat = repeat;
	}

	/** {@inheritDoc} */
	public void setRewind(boolean rewind) {
		isRewind = rewind;
	}

	/**
	 * Start this server-side stream
	 */
	public void start() {
		if (state != State.UNINIT) {
			throw new IllegalStateException("State " + state + " not valid to start");
		}
		if (items.size() == 0) {
			throw new IllegalStateException("At least one item should be specified to start");
		}
		if (publishedName == null) {
			throw new IllegalStateException("A published name is needed to start");
		}
		// publish this server-side stream
		IProviderService providerService = (IProviderService) getScope().getContext().getBean(IProviderService.BEAN_NAME);
		providerService.registerBroadcastStream(getScope(), publishedName, this);
		Map<String, Object> recordParamMap = new HashMap<String, Object>();
		recordPipe = new InMemoryPushPushPipe();
		recordParamMap.put("record", null);
		recordPipe.subscribe((IProvider) this, recordParamMap);
		recordingFilename = null;
		scheduler = (ISchedulingService) getScope().getContext().getBean(ISchedulingService.BEAN_NAME);
		state = State.STOPPED;
		currentItemIndex = -1;
		nextItem();
	}

	/**
	 * Begin VOD broadcasting
	 */
	private void startBroadcastVOD() {
		nextVideoTS = nextAudioTS = nextDataTS = 0;
		nextRTMPMessage = null;
		vodStartTS = 0;
		serverStartTS = System.currentTimeMillis();
		scheduleNextMessage();
	}

	/**
	 * Stop this server-side stream
	 */
	public void stop() {
		if (state != State.PLAYING && state != State.PAUSED) {
			return;
		}
		if (liveJobName != null) {
			scheduler.removeScheduledJob(liveJobName);
			liveJobName = null;
		}
		if (vodJobName != null) {
			scheduler.removeScheduledJob(vodJobName);
			vodJobName = null;
		}
		if (msgIn != null) {
			msgIn.unsubscribe(this);
			msgIn = null;
		}
		if (nextRTMPMessage != null) {
			nextRTMPMessage.getBody().release();
		}
		state = State.STOPPED;
	}

	/** {@inheritDoc} */
	public void pause() {
		if (state == State.PLAYING) {
			state = State.PAUSED;
		} else if (state == State.PAUSED) {
			state = State.PLAYING;
			vodStartTS = 0;
			serverStartTS = System.currentTimeMillis();
			scheduleNextMessage();
		}
	}

	/** {@inheritDoc} */
	public void seek(int position) {
		if (state != State.PLAYING && state != State.PAUSED)
			// Can't seek when stopped/closed
			return;

		sendVODSeekCM(msgIn, position);
	}

	/**
	 * Send VOD seek control message
	 * 
	 * @param msgIn				Message input
	 * @param position			New timestamp to play from
	 */
	private void sendVODSeekCM(IMessageInput msgIn, int position) {
		OOBControlMessage oobCtrlMsg = new OOBControlMessage();
		oobCtrlMsg.setTarget(ISeekableProvider.KEY);
		oobCtrlMsg.setServiceName("seek");
		Map<String, Object> paramMap = new HashMap<String, Object>();
		paramMap.put("position", new Integer(position));
		oobCtrlMsg.setServiceParamMap(paramMap);
		msgIn.sendOOBControlMessage(this, oobCtrlMsg);
		synchronized (this) {
			// Reset properties
			vodStartTS = 0;
			serverStartTS = System.currentTimeMillis();
			if (nextRTMPMessage != null) {
				try {
					pushMessage(nextRTMPMessage);
				} catch (IOException err) {
					log.error("Error while sending message.", err);
				}
				nextRTMPMessage.getBody().release();
				nextRTMPMessage = null;
			}
			ResetMessage reset = new ResetMessage();
			try {
				pushMessage(reset);
			} catch (IOException err) {
				log.error("Error while sending message.", err);
			}
			scheduleNextMessage();
		}
	}

	/** {@inheritDoc} */
	public void addStreamListener(IStreamListener listener) {
		listeners.add(listener);
	}

	/** {@inheritDoc} */
	public Collection<IStreamListener> getStreamListeners() {
		return listeners;
	}

	/** {@inheritDoc} */
	public void removeStreamListener(IStreamListener listener) {
		listeners.remove(listener);
	}

}