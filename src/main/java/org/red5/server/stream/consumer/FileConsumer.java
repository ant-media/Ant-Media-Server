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

package org.red5.server.stream.consumer;

import java.io.File;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.mina.core.buffer.IoBuffer;
import org.red5.io.IStreamableFile;
import org.red5.io.ITag;
import org.red5.io.ITagWriter;
import org.red5.io.flv.impl.Tag;
import org.red5.server.api.scope.IScope;
import org.red5.server.api.service.IStreamableFileService;
import org.red5.server.api.stream.IClientStream;
import org.red5.server.api.stream.IStreamableFileFactory;
import org.red5.server.messaging.IMessage;
import org.red5.server.messaging.IMessageComponent;
import org.red5.server.messaging.IPipe;
import org.red5.server.messaging.IPipeConnectionListener;
import org.red5.server.messaging.IPushableConsumer;
import org.red5.server.messaging.OOBControlMessage;
import org.red5.server.messaging.PipeConnectionEvent;
import org.red5.server.net.rtmp.event.FlexStreamSend;
import org.red5.server.net.rtmp.event.IRTMPEvent;
import org.red5.server.net.rtmp.event.VideoData;
import org.red5.server.net.rtmp.event.VideoData.FrameType;
import org.red5.server.net.rtmp.message.Constants;
import org.red5.server.stream.IStreamData;
import org.red5.server.stream.StreamableFileFactory;
import org.red5.server.stream.message.RTMPMessage;
import org.red5.server.stream.message.ResetMessage;
import org.red5.server.util.ScopeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;

/**
 * Consumer that pushes messages to file. Used when recording live streams.
 * 
 * @author The Red5 Project
 * @author Paul Gregoire (mondain@gmail.com)
 * @author Vladimir Hmelyoff (vlhm@splitmedialabs.com)
 */
public class FileConsumer implements Constants, IPushableConsumer, IPipeConnectionListener {

	private static final Logger log = LoggerFactory.getLogger(FileConsumer.class);

	/**
	 * Executor for all writer jobs
	 */
	private static ScheduledExecutorService scheduledExecutorService;

	/**
	 * Queue writer thread count
	 */
	private int schedulerThreadSize = 1;

	/**
	 * Queue to hold data for delayed writing
	 */
	private PriorityQueue<QueuedData> queue;

	/**
	 * Reentrant lock
	 */
	private ReentrantReadWriteLock reentrantLock;

	/**
	 * Write lock
	 */
	private volatile Lock writeLock;

	/**
	 * Read lock
	 */
	private volatile Lock readLock;

	/**
	 * Scope
	 */
	private IScope scope;

	/**
	 * File
	 */
	private File file;

	/**
	 * Tag writer
	 */
	private ITagWriter writer;

	/**
	 * Operation mode
	 */
	private String mode;

	/**
	 * Start timestamp
	 */
	private int startTimestamp = -1;

	/**
	 * Last write timestamp
	 */
	@SuppressWarnings("unused")
	private int lastTimestamp;

	/**
	 * Video decoder configuration
	 */
	private ITag videoConfigurationTag;

	/**
	 * Audio decoder configuration
	 */
	private ITag audioConfigurationTag;

	/**
	 * Number of queued items needed before writes are initiated
	 */
	private int queueThreshold = -1;

	/**
	 * Percentage of the queue which is sliced for writing
	 */
	private int percentage = 25;

	/**
	 * Whether or not to use a queue for delaying file writes. The queue is useful
	 * for keeping Tag items in their expected order based on their time stamp.
	 */
	private boolean delayWrite = false;

	/**
	 * Tracks the last timestamp written to prevent backwards time stamped data.
	 */
	private volatile int lastWrittenTs = -1;

	/**
	 * Keeps track of the last spawned write worker.
	 */
	private volatile Future<?> writerFuture;

	private volatile boolean gotVideoKeyFrame;

	/**
	 * Default ctor
	 */
	public FileConsumer() {
		if (scheduledExecutorService == null) {
			scheduledExecutorService = Executors.newScheduledThreadPool(schedulerThreadSize, new CustomizableThreadFactory("FileConsumerExecutor-"));
		}
	}

	/**
	 * Creates file consumer
	 * @param scope        Scope of consumer
	 * @param file         File
	 */
	public FileConsumer(IScope scope, File file) {
		this();
		this.scope = scope;
		this.file = file;
	}

	/**
	 * Push message through pipe
	 * @param pipe         Pipe
	 * @param message      Message to push
	 * @throws IOException if message could not be written
	 */
	@SuppressWarnings("rawtypes")
	public void pushMessage(IPipe pipe, IMessage message) throws IOException {
		if (message instanceof RTMPMessage) {
			final IRTMPEvent msg = ((RTMPMessage) message).getBody();
			// get the type
			byte dataType = msg.getDataType();
			// get the timestamp
			int timestamp = msg.getTimestamp();
			log.debug("Data type: {} timestamp: {}", dataType, timestamp);
			// if we're dealing with a FlexStreamSend IRTMPEvent, this avoids relative timestamp calculations
			if (!(msg instanceof FlexStreamSend)) {
				log.trace("Not FlexStreamSend type");
				lastTimestamp = timestamp;
			}
			// ensure that our first video frame written is a key frame
			if (msg instanceof VideoData) {
				if (!gotVideoKeyFrame) {
					VideoData video = (VideoData) msg;
					if (video.getFrameType() == FrameType.KEYFRAME) {
						log.debug("Got our first keyframe");
						gotVideoKeyFrame = true;
					} else {
						// skip this frame bail out
						log.debug("Skipping video data since keyframe has not been written yet");
						return;
					}
				}
			}
			// initialize a writer
			if (writer == null) {
				init();
			}
			// if writes are delayed, queue the data and sort it by time
			if (!delayWrite) {
				write(timestamp, msg);
			} else {
				QueuedData queued = null;
				if (msg instanceof IStreamData) {
					log.debug("Stream data, body saved. Data type: {} class type: {}", dataType, msg.getClass().getName());
					try {
						queued = new QueuedData(timestamp, dataType, ((IStreamData) msg).duplicate());
					} catch (ClassNotFoundException e) {
						log.warn("Exception queueing stream data", e);
					}
				} else {
					//XXX what type of message are we saving that has no body data??
					log.debug("Non-stream data, body not saved. Data type: {} class type: {}", dataType, msg.getClass().getName());
					queued = new QueuedData(timestamp, dataType);
				}
				writeLock.lock();
				try {
					//add to the queue
					queue.add(queued);
				} finally {
					writeLock.unlock();
				}
				int queueSize = 0;
				readLock.lock();
				try {
					queueSize = queue.size();
				} finally {
					readLock.unlock();
				}
				if (msg instanceof VideoData) {
					writeQueuedDataSlice(createTimestampLimitedSlice(msg.getTimestamp()));
				} else if (queueThreshold >= 0 && queueSize >= queueThreshold) {
					writeQueuedDataSlice(createFixedLengthSlice(queueThreshold / (100 / percentage)));
				}
			}
		} else if (message instanceof ResetMessage) {
			startTimestamp = -1;
		}
	}

	private void writeQueuedDataSlice(final QueuedData[] slice) {
		if (acquireWriteFuture(slice.length)) {
			// spawn a writer
			writerFuture = scheduledExecutorService.submit(new Runnable() {
				public void run() {
					log.trace("Spawning queue writer thread");
					doWrites(slice);
				}
			});
		}
	}

	private QueuedData[] createFixedLengthSlice(int sliceLength) {
		log.debug("Creating data slice to write of length {}.", sliceLength);
		// get the slice
		final QueuedData[] slice = new QueuedData[sliceLength];
		log.trace("Slice length: {}", slice.length);
		writeLock.lock();
		try {
			// sort the queue
			log.trace("Queue length: {}", queue.size());
			for (int q = 0; q < sliceLength; q++) {
				slice[q] = queue.remove();
			}
			log.trace("Queue length (after removal): {}", queue.size());
		} finally {
			writeLock.unlock();
		}
		return slice;
	}

	private QueuedData[] createTimestampLimitedSlice(int timestamp) {
		log.debug("Creating data slice up until timestamp {}.", timestamp);
		// get the slice
		final ArrayList<QueuedData> slice = new ArrayList<QueuedData>();
		writeLock.lock();
		try {
			// sort the queue
			log.trace("Queue length: {}", queue.size());
			
			if(!queue.isEmpty()) {
    			QueuedData data;
    			
    			do {
    				data = queue.remove();
    				slice.add(data);
    			} while (!queue.isEmpty() && data.getTimestamp() <= timestamp);
    			log.trace("Queue length (after removal): {}", queue.size());
			}
		} finally {
			writeLock.unlock();
		}
		return slice.toArray(new QueuedData[slice.size()]);
	}

	/**
	 * Get the WriteFuture with a timeout based on the length of the slice to write.
	 * 
	 * @param sliceLength
	 * @return true if successful and false otherwise
	 */
	private boolean acquireWriteFuture(int sliceLength) {
		if (sliceLength > 0) {
			Object writeResult = null;
			// determine a good timeout value based on the slice length to write
			int timeout = sliceLength * 500;
			// check for existing future
			if (writerFuture != null) {
				try {
					//wait n seconds for a result from the last writer
					writeResult = writerFuture.get(timeout, TimeUnit.MILLISECONDS);
				} catch (Exception e) {
					log.warn("Exception waiting for write result", e);
					return false;
				}
			}
			log.debug("Write future result (expect null): {}", writeResult);
			return true;
		}
		return false;
	}

	/**
	 * Out-of-band control message handler
	 *
	 * @param source            Source of message
	 * @param pipe              Pipe that is used to transmit OOB message
	 * @param oobCtrlMsg        OOB control message
	 */
	public void onOOBControlMessage(IMessageComponent source, IPipe pipe, OOBControlMessage oobCtrlMsg) {
	}

	/**
	 * Pipe connection event handler
	 * @param event       Pipe connection event
	 */
	public void onPipeConnectionEvent(PipeConnectionEvent event) {
		switch (event.getType()) {
			case PipeConnectionEvent.CONSUMER_CONNECT_PUSH:
				if (event.getConsumer() == this) {
					Map<String, Object> paramMap = event.getParamMap();
					if (paramMap != null) {
						mode = (String) paramMap.get("mode");
					}
				}
				break;
			case PipeConnectionEvent.CONSUMER_DISCONNECT:
				if (event.getConsumer() != this) {
					break;
				}
			case PipeConnectionEvent.PROVIDER_DISCONNECT:
				// we only support one provider at a time so releasing when provider disconnects
				//uninit();
				break;
			default:
				break;
		}
	}

	/**
	 * Initialization
	 *
	 * @throws IOException          I/O exception
	 */
	private void init() throws IOException {
		log.debug("Init");
		// if the "file" is null, the consumer has been uninitialized
		if (file != null) {
			// if we plan to use a queue, create one
			if (delayWrite) {
				queue = new PriorityQueue<QueuedData>(queueThreshold <= 0 ? 11 : queueThreshold);
				// add associated locks
				reentrantLock = new ReentrantReadWriteLock();
				writeLock = reentrantLock.writeLock();
				readLock = reentrantLock.readLock();
			}
			IStreamableFileFactory factory = (IStreamableFileFactory) ScopeUtils.getScopeService(scope, IStreamableFileFactory.class, StreamableFileFactory.class);
			File folder = file.getParentFile();
			if (!folder.exists()) {
				if (!folder.mkdirs()) {
					throw new IOException("Could not create parent folder");
				}
			}
			if (!file.isFile()) {
				// Maybe the (previously existing) file has been deleted
				file.createNewFile();
			} else if (!file.canWrite()) {
				throw new IOException("The file is read-only");
			}
			IStreamableFileService service = factory.getService(file);
			IStreamableFile flv = service.getStreamableFile(file);
			if (mode == null || mode.equals(IClientStream.MODE_RECORD)) {
				writer = flv.getWriter();
				//write the decoder config tag if it exists
				if (videoConfigurationTag != null) {
					writer.writeTag(videoConfigurationTag);
					videoConfigurationTag = null;
				}
				if (audioConfigurationTag != null) {
					writer.writeTag(audioConfigurationTag);
					audioConfigurationTag = null;
				}
			} else if (mode.equals(IClientStream.MODE_APPEND)) {
				writer = flv.getAppendWriter();
			} else {
				throw new IllegalStateException(String.format("Illegal mode type: %s", mode));
			}
		} else {
			log.warn("Consumer is uninitialized");
		}
	}

	/**
	 * Reset or uninitialize
	 */
	public void uninit() {
		log.debug("Uninit");
		if (writer != null) {
			if (writerFuture != null) {
				try {
					writerFuture.get();
				} catch (Exception e) {
					log.warn("Exception waiting for write result on uninit", e);
				}
				if (writerFuture.cancel(false)) {
					log.debug("Future completed");
				}
			}
			writerFuture = null;
			if (delayWrite) {
				// write all the queued items
				doWrites();
				// clear the queue
				queue.clear();
				queue = null;
			}
			//close the writer
			writer.close();
			writer = null;
		}
		// clear file ref
		file = null;
	}

	/**
	 * Write all the queued items to the writer.
	 */
	public final void doWrites() {
		QueuedData[] slice = null;
		writeLock.lock();
		try {
			slice = queue.toArray(new QueuedData[0]);
			if (queue.removeAll(Arrays.asList(slice))) {
				log.debug("Queued writes transfered, count: {}", slice.length);
			}
		} finally {
			writeLock.unlock();
		}
		doWrites(slice);
	}

	/**
	 * Write a slice of the queued items to the writer.
	 */
	public final void doWrites(QueuedData[] slice) {
		//empty the queue
		for (QueuedData queued : slice) {
			int tmpTs = queued.getTimestamp();
			if (lastWrittenTs <= tmpTs) {
				write(queued);
				lastWrittenTs = tmpTs;
			}
		}
		//clear and null-out
		slice = null;
	}

	/**
	 * Write incoming data to the file.
	 * 
	 * @param timestamp adjusted timestamp
	 * @param msg stream data
	 */
	private final void write(int timestamp, IRTMPEvent msg) {
		byte dataType = msg.getDataType();
		log.debug("Write - timestamp: {} type: {}", timestamp, dataType);
		//if the last message was a reset or we just started, use the header timer
		if (startTimestamp == -1) {
			startTimestamp = timestamp;
			timestamp = 0;
		} else {
			timestamp -= startTimestamp;
		}
		// create a tag
		ITag tag = new Tag();
		tag.setDataType(dataType);
		tag.setTimestamp(timestamp);
		// get data bytes
		IoBuffer data = ((IStreamData<?>) msg).getData().duplicate();
		if (data != null) {
			tag.setBodySize(data.limit());
			tag.setBody(data);
		}
		// only allow blank tags if they are of audio type
		if (tag.getBodySize() > 0 || dataType == ITag.TYPE_AUDIO) {
			try {
				if (timestamp >= 0) {
					if (!writer.writeTag(tag)) {
						log.warn("Tag was not written");
					}
				} else {
					log.warn("Skipping message with negative timestamp.");
				}
			} catch (IOException e) {
				log.error("Error writing tag", e);
			} finally {
				if (data != null) {
					data.clear();
					data.free();
				}
			}
		}
		data = null;
	}

	/**
	 * Adjust timestamp and write to the file.
	 * 
	 * @param queued queued data for write
	 */
	private final void write(QueuedData queued) {
		if (queued != null) {
			//get timestamp
			int timestamp = queued.getTimestamp();
			log.debug("Write - timestamp: {} type: {}", timestamp, queued.getDataType());
			//if the last message was a reset or we just started, use the header timer
			if (startTimestamp == -1) {
				startTimestamp = timestamp;
				timestamp = 0;
			} else {
				timestamp -= startTimestamp;
			}
			// get the type
			byte dataType = queued.getDataType();
			// create a tag
			ITag tag = new Tag();
			tag.setDataType(dataType);
			tag.setTimestamp(timestamp);
			// get queued
			IoBuffer data = queued.getData();
			if (data != null) {
				tag.setBodySize(data.limit());
				tag.setBody(data);
			}
			// only allow blank tags if they are of audio type
			if (tag.getBodySize() > 0 || dataType == ITag.TYPE_AUDIO) {
				try {
					if (timestamp >= 0) {
						if (!writer.writeTag(tag)) {
							log.warn("Tag was not written");
						}
					} else {
						log.warn("Skipping message with negative timestamp.");
					}
				} catch (ClosedChannelException cce) {
					// the channel we tried to write to is closed, we should not try again on that writer
					log.error("The writer is no longer able to write to the file: {} writable: {}", file.getName(), file.canWrite());
				} catch (IOException e) {
					log.warn("Error writing tag", e);
					if (e.getCause() instanceof ClosedChannelException) {
						// the channel we tried to write to is closed, we should not try again on that writer
						log.error("The writer is no longer able to write to the file: {} writable: {}", file.getName(), file.canWrite());
					}
				} finally {
					if (data != null) {
						data.clear();
						data.free();
					}
				}
			}
			data = null;
			queued.dispose();
			queued = null;
		} else {
			log.warn("Queued data was null");
		}
	}

	/**
	 * Sets a video decoder configuration; some codecs require this, such as AVC.
	 * 
	 * @param decoderConfig video codec configuration
	 */
	public void setVideoDecoderConfiguration(IRTMPEvent decoderConfig) {
		videoConfigurationTag = new Tag();
		videoConfigurationTag.setDataType(decoderConfig.getDataType());
		videoConfigurationTag.setTimestamp(0);
		if (decoderConfig instanceof IStreamData) {
			IoBuffer data = ((IStreamData<?>) decoderConfig).getData().asReadOnlyBuffer();
			videoConfigurationTag.setBodySize(data.limit());
			videoConfigurationTag.setBody(data);
		}
	}

	/**
	 * Sets a audio decoder configuration; some codecs require this, such as AAC.
	 * 
	 * @param decoderConfig audio codec configuration
	 */
	public void setAudioDecoderConfiguration(IRTMPEvent decoderConfig) {
		audioConfigurationTag = new Tag();
		audioConfigurationTag.setDataType(decoderConfig.getDataType());
		audioConfigurationTag.setTimestamp(0);
		if (decoderConfig instanceof IStreamData) {
			IoBuffer data = ((IStreamData<?>) decoderConfig).getData().asReadOnlyBuffer();
			audioConfigurationTag.setBodySize(data.limit());
			audioConfigurationTag.setBody(data);
		}
	}

	/**
	 * Sets the scope for this consumer.
	 * 
	 * @param scope
	 */
	public void setScope(IScope scope) {
		this.scope = scope;
	}

	/**
	 * Sets the file we're writing to.
	 * 
	 * @param file
	 */
	public void setFile(File file) {
		this.file = file;
	}

	/**
	 * Returns the file.
	 * 
	 * @return file
	 */
	public File getFile() {
		return file;
	}

	/**
	 * Sets the threshold for the queue. When the threshold is met a worker is spawned
	 * to empty the sorted queue to the writer.
	 * 
	 * @param queueThreshold number of items to queue before spawning worker
	 */
	public void setQueueThreshold(int queueThreshold) {
		this.queueThreshold = queueThreshold;
	}

	/**
	 * Returns the size of the delayed writing queue.
	 * 
	 * @return queue length
	 */
	public int getQueueThreshold() {
		return queueThreshold;
	}

	/**
	 * Whether or not the queue should be utilized.
	 * 
	 * @return true if using the queue, false if sending directly to the writer
	 */
	public boolean isDelayWrite() {
		return delayWrite;
	}

	/**
	 * Sets whether or not to use the queue.
	 * 
	 * @param delayWrite true to use the queue, false if not
	 */
	public void setDelayWrite(boolean delayWrite) {
		this.delayWrite = delayWrite;
	}

	/**
	 * @return the schedulerThreadSize
	 */
	public int getSchedulerThreadSize() {
		return schedulerThreadSize;
	}

	/**
	 * @param schedulerThreadSize the schedulerThreadSize to set
	 */
	public void setSchedulerThreadSize(int schedulerThreadSize) {
		this.schedulerThreadSize = schedulerThreadSize;
	}

	/**
	 * Sets the recording mode.
	 * 
	 * @param mode either "record" or "append" depending on the type of action to perform
	 */
	public void setMode(String mode) {
		this.mode = mode;
	}

	/**
	 * Queued data wrapper.
	 */
	private final static class QueuedData implements Comparable<QueuedData> {
		final int timestamp;

		final byte dataType;

		@SuppressWarnings("rawtypes")
		final IStreamData streamData;

		QueuedData(int timestamp, byte dataType) {
			this.timestamp = timestamp;
			this.dataType = dataType;
			this.streamData = null;
		}

		@SuppressWarnings("rawtypes")
		QueuedData(int timestamp, byte dataType, IStreamData streamData) {
			this.timestamp = timestamp;
			this.dataType = dataType;
			this.streamData = streamData;
		}

		public int getTimestamp() {
			return timestamp;
		}

		public byte getDataType() {
			return dataType;
		}

		public IoBuffer getData() {
			return streamData.getData().asReadOnlyBuffer();
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + dataType;
			result = prime * result + timestamp;
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null || getClass() != obj.getClass()) {
				return false;
			}
			QueuedData other = (QueuedData) obj;
			if (dataType != other.dataType) {
				return false;
			}
			if (timestamp != other.timestamp) {
				return false;
			}
			return true;
		}

		@Override
		public int compareTo(QueuedData other) {
			if (timestamp > other.timestamp) {
				return 1;
			} else if (timestamp < other.timestamp) {
				return -1;
			}
			return 0;
		}

		public void dispose() {
			streamData.getData().free();
		}

	}

}
