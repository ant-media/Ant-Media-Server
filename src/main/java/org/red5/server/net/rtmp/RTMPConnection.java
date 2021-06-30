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

package org.red5.server.net.rtmp;

import java.beans.ConstructorProperties;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.red5.server.BaseConnection;
import org.red5.server.api.Red5;
import org.red5.server.api.event.IEvent;
import org.red5.server.api.scope.IScope;
import org.red5.server.api.service.IPendingServiceCall;
import org.red5.server.api.service.IPendingServiceCallback;
import org.red5.server.api.service.IServiceCall;
import org.red5.server.api.service.IServiceCapableConnection;
import org.red5.server.api.stream.IClientBroadcastStream;
import org.red5.server.api.stream.IClientStream;
import org.red5.server.api.stream.IPlaylistSubscriberStream;
import org.red5.server.api.stream.ISingleItemSubscriberStream;
import org.red5.server.api.stream.IStreamCapableConnection;
import org.red5.server.api.stream.IStreamService;
import org.red5.server.exception.ClientRejectedException;
import org.red5.server.net.protocol.RTMPDecodeState;
import org.red5.server.net.rtmp.codec.RTMP;
import org.red5.server.net.rtmp.event.BytesRead;
import org.red5.server.net.rtmp.event.ClientBW;
import org.red5.server.net.rtmp.event.ClientInvokeEvent;
import org.red5.server.net.rtmp.event.ClientNotifyEvent;
import org.red5.server.net.rtmp.event.Invoke;
import org.red5.server.net.rtmp.event.Notify;
import org.red5.server.net.rtmp.event.Ping;
import org.red5.server.net.rtmp.event.ServerBW;
import org.red5.server.net.rtmp.event.VideoData;
import org.red5.server.net.rtmp.message.Constants;
import org.red5.server.net.rtmp.message.Header;
import org.red5.server.net.rtmp.message.Packet;
import org.red5.server.net.rtmp.status.Status;
import org.red5.server.service.Call;
import org.red5.server.service.PendingCall;
import org.red5.server.so.FlexSharedObjectMessage;
import org.red5.server.so.ISharedObjectEvent;
import org.red5.server.so.SharedObjectMessage;
import org.red5.server.stream.AbstractClientStream;
import org.red5.server.stream.ClientBroadcastStream;
import org.red5.server.stream.OutputStream;
import org.red5.server.stream.PlaylistSubscriberStream;
import org.red5.server.stream.SingleItemSubscriberStream;
import org.red5.server.stream.StreamService;
import org.red5.server.util.ScopeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.util.concurrent.ListenableFutureTask;

/**
 * RTMP connection. Stores information about client streams, data transfer channels, pending RPC calls, bandwidth configuration, AMF
 * encoding type (AMF0/AMF3), connection state (is alive, last ping time and ping result) and session.
 */
public abstract class RTMPConnection extends BaseConnection implements IStreamCapableConnection, IServiceCapableConnection, IReceivedMessageTaskQueueListener {

    private static Logger log = LoggerFactory.getLogger(RTMPConnection.class);

    public static final String RTMP_SESSION_ID = "rtmp.sessionid";

    public static final String RTMP_HANDSHAKE = "rtmp.handshake";

    public static final String RTMP_CONN_MANAGER = "rtmp.connection.manager";

    public static final Object RTMP_HANDLER = "rtmp.handler";

    /**
     * Marker byte for standard or non-encrypted RTMP data.
     */
    public static final byte RTMP_NON_ENCRYPTED = (byte) 0x03;

    /**
     * Marker byte for encrypted RTMP data.
     */
    public static final byte RTMP_ENCRYPTED = (byte) 0x06;

    /**
     * Marker byte for encrypted RTMP data XTEA. http://en.wikipedia.org/wiki/XTEA
     */
    public static final byte RTMP_ENCRYPTED_XTEA = (byte) 0x08;

    /**
     * Marker byte for encrypted RTMP data using Blowfish. http://en.wikipedia.org/wiki/Blowfish_(cipher)
     */
    public static final byte RTMP_ENCRYPTED_BLOWFISH = (byte) 0x09;

    /**
     * Unknown type 0x0a, seen on youtube
     */
    public static final byte RTMP_ENCRYPTED_UNK = (byte) 0x0a;

    /**
     * Cipher for RTMPE input
     */
    public static final String RTMPE_CIPHER_IN = "rtmpe.cipher.in";

    /**
     * Cipher for RTMPE output
     */
    public static final String RTMPE_CIPHER_OUT = "rtmpe.cipher.out";

    // ~320 streams seems like a sufficient max amount of streams for a single connection
    public static final double MAX_RESERVED_STREAMS = 320;

    /**
     * Initial channel capacity
     */
    private int channelsInitalCapacity = 3;

    /**
     * Concurrency level for channels collection
     */
    private int channelsConcurrencyLevel = 1;

    /**
     * Initial streams capacity
     */
    private int streamsInitalCapacity = 1;

    /**
     * Concurrency level for streams collection
     */
    private int streamsConcurrencyLevel = 1;

    /**
     * Initial pending calls capacity
     */
    private int pendingCallsInitalCapacity = 3;

    /**
     * Concurrency level for pending calls collection
     */
    private int pendingCallsConcurrencyLevel = 1;

    /**
     * Initial reserved streams capacity
     */
    private int reservedStreamsInitalCapacity = 1;

    /**
     * Concurrency level for reserved streams collection
     */
    private int reservedStreamsConcurrencyLevel = 1;

    /**
     * Connection channels
     * 
     * @see org.red5.server.net.rtmp.Channel
     */
    private transient ConcurrentMap<Integer, Channel> channels = new ConcurrentHashMap<Integer, Channel>(channelsInitalCapacity, 0.9f, channelsConcurrencyLevel);

    /**
     * Queues of tasks for every channel
     *
     * @see org.red5.server.net.rtmp.ReceivedMessageTaskQueue
     */
    private final transient ConcurrentMap<Integer, ReceivedMessageTaskQueue> tasksByStreams = new ConcurrentHashMap<Integer, ReceivedMessageTaskQueue>(streamsInitalCapacity, 0.9f, streamsConcurrencyLevel);

    /**
     * Client streams
     * 
     * @see org.red5.server.api.stream.IClientStream
     */
    private transient ConcurrentMap<Number, IClientStream> streams = new ConcurrentHashMap<Number, IClientStream>(streamsInitalCapacity, 0.9f, streamsConcurrencyLevel);

    /**
     * Reserved stream ids. Stream id's directly relate to individual NetStream instances.
     */
    private transient Set<Number> reservedStreams = Collections.newSetFromMap(new ConcurrentHashMap<Number, Boolean>(reservedStreamsInitalCapacity, 0.9f, reservedStreamsConcurrencyLevel));

    /**
     * Transaction identifier for remote commands.
     */
    private AtomicInteger transactionId = new AtomicInteger(1);

    /**
     * Hash map that stores pending calls and ids as pairs.
     */
    private transient ConcurrentMap<Integer, IPendingServiceCall> pendingCalls = new ConcurrentHashMap<Integer, IPendingServiceCall>(pendingCallsInitalCapacity, 0.75f, pendingCallsConcurrencyLevel);

    /**
     * Deferred results set.
     * 
     * @see org.red5.server.net.rtmp.DeferredResult
     */
    private transient CopyOnWriteArraySet<DeferredResult> deferredResults = new CopyOnWriteArraySet<DeferredResult>();

    /**
     * Last ping round trip time
     */
    private AtomicInteger lastPingRoundTripTime = new AtomicInteger(-1);

    /**
     * Timestamp when last ping command was sent.
     */
    private AtomicLong lastPingSentOn = new AtomicLong(0);

    /**
     * Timestamp when last ping result was received.
     */
    private AtomicLong lastPongReceivedOn = new AtomicLong(0);

    /**
     * RTMP events handler
     */
    protected transient IRTMPHandler handler;

    /**
     * Ping interval in ms to detect dead clients.
     */
    private volatile int pingInterval = 5000;

    /**
     * Maximum time in ms after which a client is disconnected because of inactivity.
     */
    protected volatile int maxInactivity = 60000;

    /**
     * Data read interval
     */
    protected long bytesReadInterval = 1024 * 1024;

    /**
     * Number of bytes to read next.
     */
    protected long nextBytesRead = 1024 * 1024;

    /**
     * Number of bytes the client reported to have received.
     */
    private AtomicLong clientBytesRead = new AtomicLong(0L);

    /**
     * Map for pending video packets keyed by stream id.
     */
    private transient ConcurrentMap<Number, AtomicInteger> pendingVideos = new ConcurrentHashMap<Number, AtomicInteger>(1, 0.9f, 1);

    /**
     * Number of (NetStream) streams used.
     */
    private AtomicInteger usedStreams = new AtomicInteger(0);

    /**
     * Remembered stream buffer durations.
     */
    private transient ConcurrentMap<Number, Integer> streamBuffers = new ConcurrentHashMap<Number, Integer>(1, 0.9f, 1);

    /**
     * Maximum time in milliseconds to wait for a valid handshake.
     */
    private int maxHandshakeTimeout = 10000;

    /**
     * Maximum time in milliseconds allowed to process received message
     */
    protected long maxHandlingTimeout = 500L;

    /**
     * Bandwidth limit type / enforcement. (0=hard,1=soft,2=dynamic)
     */
    protected int limitType = 0;

    /**
     * Protocol state
     */
    protected RTMP state = new RTMP();

    // protection for the decoder when using multiple threads per connection
    protected transient Semaphore decoderLock = new Semaphore(1, true);

    // protection for the encoder when using multiple threads per connection
    protected transient Semaphore encoderLock = new Semaphore(1, true);

    // keeps track of the decode state
    protected transient RTMPDecodeState decoderState;

    /**
     * Scheduling service
     */
    protected transient ThreadPoolTaskScheduler scheduler;

    /**
     * Thread pool for message handling.
     */
    protected transient ThreadPoolTaskExecutor executor;

    /**
     * Thread pool for guarding deadlocks.
     */
    protected transient ThreadPoolTaskScheduler deadlockGuardScheduler;

    /**
     * Keep-alive worker flag
     */
    protected final AtomicBoolean running;

    /**
     * Timestamp generator
     */
    private final AtomicInteger timer = new AtomicInteger(0);

    /**
     * Closing flag
     */
    private final AtomicBoolean closing = new AtomicBoolean(false);

    /**
     * Packet sequence number
     */
    private final AtomicLong packetSequence = new AtomicLong();

    /**
     * Specify the size of queue that will trigger audio packet dropping, disabled if it's 0
     * */
    private Integer executorQueueSizeToDropAudioPackets = 0;

    /**
     * Keep track of current queue size
     * */
    private final AtomicInteger currentQueueSize = new AtomicInteger();

    /**
     * Wait for handshake task.
     */
    private ScheduledFuture<?> waitForHandshakeTask;

    /**
     * Keep alive task.
     */
    private ScheduledFuture<?> keepAliveTask;

    /**
     * Creates anonymous RTMP connection without scope.
     * 
     * @param type
     *            Connection type
     */
    @ConstructorProperties({ "type" })
    public RTMPConnection(String type) {
        // We start with an anonymous connection without a scope.
        // These parameters will be set during the call of "connect" later.
        super(type);
        // create a decoder state
        decoderState = new RTMPDecodeState(getSessionId());
        // set running flag
        running = new AtomicBoolean(false);
    }

    public int getId() {
        // handle the fact that a client id is a String
        return client != null ? client.getId().hashCode() : -1;
    }

    @Deprecated
    public void setId(int clientId) {
        log.warn("Setting of a client id is deprecated, use IClient to manipulate the id", new Exception("RTMPConnection.setId is deprecated"));
    }

    public void setHandler(IRTMPHandler handler) {
        this.handler = handler;
    }

    public IRTMPHandler getHandler() {
        return handler;
    }

    public RTMP getState() {
        return state;
    }

    public byte getStateCode() {
        return state.getState();
    }

    public void setStateCode(byte code) {
        if (log.isTraceEnabled()) {
            log.trace("setStateCode: {} - {}", code, RTMP.states[code]);
        }
        state.setState(code);
    }

    public IoSession getIoSession() {
        return null;
    }

    /**
     * @return the decoderLock
     */
    public Semaphore getDecoderLock() {
        return decoderLock;
    }

    /**
     * @return the decoderLock
     */
    public Semaphore getEncoderLock() {
        return encoderLock;
    }

    /**
     * @return the decoderState
     */
    public RTMPDecodeState getDecoderState() {
        return decoderState;
    }

    /** {@inheritDoc} */
    public void setBandwidth(int mbits) {
        // tell the flash player how fast we want data and how fast we shall send it
        getChannel(2).write(new ServerBW(mbits));
        // second param is the limit type (0=hard,1=soft,2=dynamic)
        getChannel(2).write(new ClientBW(mbits, (byte) limitType));
    }

    /**
     * Returns a usable timestamp for written packets.
     * 
     * @return timestamp
     */
    public int getTimer() {
        return timer.incrementAndGet();
    }

    /**
     * Opens the connection.
     */
    public void open() {
        if (log.isTraceEnabled()) {
            // dump memory stats
            log.trace("Memory at open - free: {}K total: {}K", Runtime.getRuntime().freeMemory() / 1024, Runtime.getRuntime().totalMemory() / 1024);
        }
    }

    @Override
    public boolean connect(IScope newScope, Object[] params) {
        if (log.isDebugEnabled()) {
            log.debug("Connect scope: {}", newScope);
        }
        try {
            boolean success = super.connect(newScope, params);
            if (success) {
                stopWaitForHandshake();
                // once the handshake has completed, start needed jobs start the ping / pong keep-alive
                startRoundTripMeasurement();
            } else if (log.isDebugEnabled()) {
                log.debug("Connect failed");
            }
            return success;
        } catch (ClientRejectedException e) {
            String reason = (String) e.getReason();
            log.info("Client rejected, reason: " + ((reason != null) ? reason : "None"));
            stopWaitForHandshake();
            throw e;
        }
    }

    /**
     * Start waiting for a valid handshake.
     */
    public void startWaitForHandshake() {
        if (log.isDebugEnabled()) {
            log.debug("startWaitForHandshake - {}", sessionId);
        }
        // start the handshake checker after maxHandshakeTimeout milliseconds
        try {
            waitForHandshakeTask = scheduler.schedule(new WaitForHandshakeTask(), new Date(System.currentTimeMillis() + maxHandshakeTimeout));
        } catch (TaskRejectedException e) {
            log.error("WaitForHandshake task was rejected for {}", sessionId, e);
        }
    }

    /**
     * Cancels wait for handshake task.
     */
    private void stopWaitForHandshake() {
        if (waitForHandshakeTask != null) {
            boolean cancelled = waitForHandshakeTask.cancel(true);
            waitForHandshakeTask = null;
            if (cancelled && log.isDebugEnabled()) {
                log.debug("waitForHandshake was cancelled for {}", sessionId);
            }
        }
    }

    /**
     * Starts measurement.
     */
    private void startRoundTripMeasurement() {
        if (scheduler != null) {
            if (pingInterval > 0) {
                if (log.isDebugEnabled()) {
                    log.debug("startRoundTripMeasurement - {}", sessionId);
                }
                try {
                    // schedule with an initial delay of now + 2s to prevent ping messages during connect post processes
                    keepAliveTask = scheduler.scheduleWithFixedDelay(new KeepAliveTask(), new Date(System.currentTimeMillis() + 2000L), pingInterval);
                    if (log.isDebugEnabled()) {
                        log.debug("Keep alive scheduled for {}", sessionId);
                    }
                } catch (Exception e) {
                    log.error("Error creating keep alive job for {}", sessionId, e);
                }
            }
        } else {
            log.error("startRoundTripMeasurement cannot be executed due to missing scheduler. This can happen if a connection drops before handshake is complete");
        }
    }

    /**
     * Stops measurement.
     */
    private void stopRoundTripMeasurement() {
        if (keepAliveTask != null) {
            boolean cancelled = keepAliveTask.cancel(true);
            keepAliveTask = null;
            if (cancelled && log.isDebugEnabled()) {
                log.debug("Keep alive was cancelled for {}", sessionId);
            }
        }
    }

    /**
     * Initialize connection.
     * 
     * @param host
     *            Connection host
     * @param path
     *            Connection path
     * @param params
     *            Params passed from client
     */
    public void setup(String host, String path, Map<String, Object> params) {
        this.host = host;
        this.path = path;
        this.params = params;
        if (Integer.valueOf(3).equals(params.get("objectEncoding"))) {
            if (log.isDebugEnabled()) {
                log.debug("Setting object encoding to AMF3");
            }
            state.setEncoding(Encoding.AMF3);
        }
    }

    /**
     * Return AMF protocol encoding used by this connection.
     * 
     * @return AMF encoding used by connection
     */
    public Encoding getEncoding() {
        return state.getEncoding();
    }

    /**
     * Getter for next available channel id.
     * 
     * @return Next available channel id
     */
    public int getNextAvailableChannelId() {
        int result = 4;
        while (isChannelUsed(result)) {
            result++;
        }
        return result;
    }

    /**
     * Checks whether channel is used.
     * 
     * @param channelId
     *            Channel id
     * @return true if channel is in use, false otherwise
     */
    public boolean isChannelUsed(int channelId) {
        return channels.get(channelId) != null;
    }

    /**
     * Return channel by id.
     * 
     * @param channelId
     *            Channel id
     * @return Channel by id
     */
    public Channel getChannel(int channelId) {
        Channel channel = channels.putIfAbsent(channelId, new Channel(this, channelId));
        if (channel == null) {
            channel = channels.get(channelId);
        }
        return channel;
    }

    /**
     * Closes channel.
     * 
     * @param channelId
     *            Channel id
     */
    public void closeChannel(int channelId) {
        if (log.isTraceEnabled()) {
            log.trace("closeChannel: {}", channelId);
        }
        Channel chan = channels.remove(channelId);
        if (log.isTraceEnabled()) {
            log.trace("channel: {} for id: {}", chan, channelId);
            if (chan == null) {
                log.trace("Channels: {}", channels);
            }
        }
        /*
        ReceivedMessageTaskQueue queue = tasksByChannels.remove(channelId);
        if (queue != null) {
            if (isConnected()) {
                // if connected, drain and process the tasks queued-up
                log.debug("Processing remaining tasks at close for channel: {}", channelId);
                processTasksQueue(queue);
            }
            queue.removeAllTasks();
        } else if (log.isTraceEnabled()) {
            log.trace("No task queue for id: {}", channelId);
        }
        */
        chan = null;
    }

    /**
     * Getter for client streams.
     * 
     * @return Client streams as array
     */
    protected Collection<IClientStream> getStreams() {
        return streams.values();
    }

    public Map<Number, IClientStream> getStreamsMap() {
        return Collections.unmodifiableMap(streams);
    }

    /** {@inheritDoc} */
    public Number reserveStreamId() {
        double d = 1.0d;
        for (; d < MAX_RESERVED_STREAMS; d++) {
            if (reservedStreams.add(d)) {
                break;
            }
        }
        if (d == MAX_RESERVED_STREAMS) {
            throw new IndexOutOfBoundsException("Unable to reserve new stream");
        }
        return d;
    }

    /** {@inheritDoc} */
    public Number reserveStreamId(Number streamId) {
        if (log.isTraceEnabled()) {
            log.trace("Reserve stream id: {}", streamId);
        }
        if (reservedStreams.add(streamId.doubleValue())) {
            return streamId;
        }
        return reserveStreamId();
    }

    /**
     * Returns whether or not a given stream id is valid.
     * 
     * @param streamId
     *            stream id
     * @return true if its valid, false if its invalid
     */
    public boolean isValidStreamId(Number streamId) {
        double d = streamId.doubleValue();
        if (log.isTraceEnabled()) {
            log.trace("Checking validation for streamId {}; reservedStreams: {}; streams: {}, connection: {}", new Object[] { d, reservedStreams, streams, sessionId });
        }
        if (d <= 0 || !reservedStreams.contains(d)) {
            log.warn("Stream id: {} was not reserved in connection {}", d, sessionId);
            // stream id has not been reserved before
            return false;
        }
        if (streams.get(d) != null) {
            // another stream already exists with this id
            log.warn("Another stream already exists with this id in streams {} in connection: {}", streams, sessionId);
            return false;
        }
        if (log.isTraceEnabled()) {
            log.trace("Stream id: {} is valid for connection: {}", d, sessionId);
        }
        return true;
    }

    /**
     * Returns whether or not the connection has been idle for a maximum period.
     * 
     * @return true if max idle period has been exceeded, false otherwise
     */
    public boolean isIdle() {
        long lastPingTime = lastPingSentOn.get();
        long lastPongTime = lastPongReceivedOn.get();
        boolean idle = (lastPongTime > 0 && (lastPingTime - lastPongTime > maxInactivity));
        if (log.isTraceEnabled()) {
            log.trace("Connection {} {} idle", getSessionId(), (idle ? "is" : "is not"));
        }
        return idle;
    }

    /**
     * Returns whether or not the connection is disconnected.
     * 
     * @return true if connection state is RTMP.STATE_DISCONNECTED, false otherwise
     */
    public boolean isDisconnected() {
        return state.getState() == RTMP.STATE_DISCONNECTED;
    }

    /** {@inheritDoc} */
    public IClientBroadcastStream newBroadcastStream(Number streamId) {
        if (isValidStreamId(streamId)) {
            // get ClientBroadcastStream defined as a prototype in red5-common.xml
            ClientBroadcastStream cbs = (ClientBroadcastStream) scope.getContext().getBean("clientBroadcastStream");
            customizeStream(streamId, cbs);
            if (!registerStream(cbs)) {
                cbs = null;
            }
            return cbs;
        }
        return null;
    }

    /** {@inheritDoc} */
    public ISingleItemSubscriberStream newSingleItemSubscriberStream(Number streamId) {
        if (isValidStreamId(streamId)) {
            // get SingleItemSubscriberStream defined as a prototype in red5-common.xml
            SingleItemSubscriberStream siss = (SingleItemSubscriberStream) scope.getContext().getBean("singleItemSubscriberStream");
            customizeStream(streamId, siss);
            if (!registerStream(siss)) {
                siss = null;
            }
            return siss;
        }
        return null;
    }

    /** {@inheritDoc} */
    public IPlaylistSubscriberStream newPlaylistSubscriberStream(Number streamId) {
        if (isValidStreamId(streamId)) {
            // get PlaylistSubscriberStream defined as a prototype in red5-common.xml
            PlaylistSubscriberStream pss = (PlaylistSubscriberStream) scope.getContext().getBean("playlistSubscriberStream");
            customizeStream(streamId, pss);
            if (!registerStream(pss)) {
                log.trace("Stream: {} for stream id: {} failed to register", streamId);
                pss = null;
            }
            return pss;
        }
        return null;
    }

    public void addClientStream(IClientStream stream) {
        if (reservedStreams.add(stream.getStreamId().doubleValue())) {
            registerStream(stream);
        } else {
            // stream not added to registered? what to do with it?
            log.warn("Failed adding stream: {} to reserved: {}", stream, reservedStreams);
        }
    }

    public void removeClientStream(Number streamId) {
        unreserveStreamId(streamId);
    }

    /**
     * Getter for used stream count.
     * 
     * @return Value for property 'usedStreamCount'.
     */
    protected int getUsedStreamCount() {
        return usedStreams.get();
    }

    /** {@inheritDoc} */
    public IClientStream getStreamById(Number streamId) {
        return streams.get(streamId.doubleValue());
    }

    /**
     * Return stream id for given channel id.
     * 
     * @param channelId
     *            Channel id
     * @return ID of stream that channel belongs to
     */
    public Number getStreamIdForChannelId(int channelId) {
        if (channelId < 4) {
            return 0;
        }
        Number streamId = Math.floor(((channelId - 4) / 5.0d) + 1);
        if (log.isTraceEnabled()) {
            log.trace("Stream id: {} requested for channel id: {}", streamId, channelId);
        }
        return streamId;
    }

    /**
     * Return stream by given channel id.
     * 
     * @param channelId
     *            Channel id
     * @return Stream that channel belongs to
     */
    public IClientStream getStreamByChannelId(int channelId) {
        // channels 2 and 3 are "special" and don't have an IClientStream associated
        if (channelId < 4) {
            return null;
        }
        Number streamId = getStreamIdForChannelId(channelId);
        if (log.isTraceEnabled()) {
            log.trace("Stream requested for channel id: {} stream id: {} streams: {}", channelId, streamId, streams);
        }
        return getStreamById(streamId);
    }

    /**
     * Return channel id for given stream id.
     * 
     * @param streamId
     *            Stream id
     * @return ID of channel that belongs to the stream
     */
    public int getChannelIdForStreamId(Number streamId) {
        int channelId = (int) (streamId.doubleValue() * 5) - 1;
        if (log.isTraceEnabled()) {
            log.trace("Channel id: {} requested for stream id: {}", channelId, streamId);
        }
        return channelId;
    }

    /**
     * Creates output stream object from stream id. Output stream consists of audio, video, and data channels.
     * 
     * @see org.red5.server.stream.OutputStream
     * @param streamId
     *            Stream id
     * @return Output stream object
     */
    public OutputStream createOutputStream(Number streamId) {
        int channelId = getChannelIdForStreamId(streamId);
        if (log.isTraceEnabled()) {
            log.trace("Create output - stream id: {} channel id: {}", streamId, channelId);
        }
        final Channel data = getChannel(channelId++);
        final Channel video = getChannel(channelId++);
        final Channel audio = getChannel(channelId++);
        if (log.isTraceEnabled()) {
            log.trace("Output stream - data: {} video: {} audio: {}", data, video, audio);
        }
        return new OutputStream(video, audio, data);
    }

    /**
     * Specify name, connection, scope and etc for stream
     *
     * @param streamId
     *            Stream id
     * @param stream
     *            Stream
     */
    private void customizeStream(Number streamId, AbstractClientStream stream) {
        Integer buffer = streamBuffers.get(streamId.doubleValue());
        if (buffer != null) {
            stream.setClientBufferDuration(buffer);
        }
        stream.setName(createStreamName());
        stream.setConnection(this);
        stream.setScope(this.getScope());
        stream.setStreamId(streamId);
    }

    /**
     * Store a stream in the connection.
     * 
     * @param stream
     */
    private boolean registerStream(IClientStream stream) {
        if (streams.putIfAbsent(stream.getStreamId().doubleValue(), stream) == null) {
            usedStreams.incrementAndGet();
            return true;
        }
        log.error("Unable to register stream {}, stream with id {} was already added", stream, stream.getStreamId());
        return false;
    }

    /**
     * Remove a stream from the connection.
     * 
     * @param stream
     */
    @SuppressWarnings("unused")
    private void unregisterStream(IClientStream stream) {
        if (stream != null) {
            deleteStreamById(stream.getStreamId());
        }
    }

    /** {@inheritDoc} */
    @Override
    public void close() {
        if (closing.compareAndSet(false, true)) {
            if (log.isDebugEnabled()) {
                log.debug("close: {}", sessionId);
            }
            stopWaitForHandshake();
            stopRoundTripMeasurement();
            // update our state
            if (state != null) {
                final byte s = getStateCode();
                switch (s) {
                    case RTMP.STATE_DISCONNECTED:
                        if (log.isDebugEnabled()) {
                            log.debug("Already disconnected");
                        }
                        return;
                    default:
                        if (log.isDebugEnabled()) {
                            log.debug("State: {}", RTMP.states[s]);
                        }
                        setStateCode(RTMP.STATE_DISCONNECTING);
                }
            }
            Red5.setConnectionLocal(this);
            IStreamService streamService = (IStreamService) ScopeUtils.getScopeService(scope, IStreamService.class, StreamService.class);
            if (streamService != null) {
                //in the end of call streamService.deleteStream we do streams.remove
                for (Iterator<IClientStream> it = streams.values().iterator(); it.hasNext();) {
                    IClientStream stream = it.next();
                    if (log.isDebugEnabled()) {
                        log.debug("Closing stream: {}", stream.getStreamId());
                    }
                    streamService.deleteStream(this, stream.getStreamId());
                }
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Stream service was not found for scope: {}", (scope != null ? scope.getName() : "null or non-existant"));
                }
            }
            // close the base connection - disconnect scopes and unregister client
            super.close();
            // kill all the collections etc
            channels.clear();
            streams.clear();
            pendingCalls.clear();
            deferredResults.clear();
            pendingVideos.clear();
            streamBuffers.clear();
            if (log.isTraceEnabled()) {
                // dump memory stats
                log.trace("Memory at close - free: {}K total: {}K", Runtime.getRuntime().freeMemory() / 1024, Runtime.getRuntime().totalMemory() / 1024);
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug("Already closing..");
            }
        }
    }

    /**
     * Dispatches event
     * 
     * @param event
     *            Event
     */
    @Override
    public void dispatchEvent(IEvent event) {
        if (log.isDebugEnabled()) {
            log.debug("Event notify: {}", event);
        }
        // determine if its an outgoing invoke or notify
        switch (event.getType()) {
            case CLIENT_INVOKE:
                ClientInvokeEvent cie = (ClientInvokeEvent) event;
                invoke(cie.getMethod(), cie.getParams(), cie.getCallback());
                break;
            case CLIENT_NOTIFY:
                ClientNotifyEvent cne = (ClientNotifyEvent) event;
                notify(cne.getMethod(), cne.getParams());
                break;
            default:
                log.warn("Unhandled event: {}", event);
        }
    }

    /**
     * When the connection has been closed, notify any remaining pending service calls that they have failed because the connection is
     * broken. Implementors of IPendingServiceCallback may only deduce from this notification that it was not possible to read a result for
     * this service call. It is possible that (1) the service call was never written to the service, or (2) the service call was written to
     * the service and although the remote method was invoked, the connection failed before the result could be read, or (3) although the
     * remote method was invoked on the service, the service implementor detected the failure of the connection and performed only partial
     * processing. The caller only knows that it cannot be confirmed that the callee has invoked the service call and returned a result.
     */
    public void sendPendingServiceCallsCloseError() {
        if (pendingCalls != null && !pendingCalls.isEmpty()) {
            if (log.isDebugEnabled()) {
                log.debug("Connection calls pending: {}", pendingCalls.size());
            }
            for (IPendingServiceCall call : pendingCalls.values()) {
                call.setStatus(Call.STATUS_NOT_CONNECTED);
                for (IPendingServiceCallback callback : call.getCallbacks()) {
                    callback.resultReceived(call);
                }
            }
        }
    }

    /** {@inheritDoc} */
    public void unreserveStreamId(Number streamId) {
        if (log.isTraceEnabled()) {
            log.trace("Unreserve streamId: {}", streamId);
        }
        double d = streamId.doubleValue();
        if (d > 0.0d) {
            if (reservedStreams.remove(d)) {
                deleteStreamById(d);
            } else {
                if (log.isTraceEnabled()) {
                    log.trace("Failed to unreserve stream id: {} streams: {}", d, streams);
                }
            }
        }
    }

    /** {@inheritDoc} */
    public void deleteStreamById(Number streamId) {
        if (log.isTraceEnabled()) {
            log.trace("Delete streamId: {}", streamId);
        }
        double d = streamId.doubleValue();
        if (d > 0.0d) {
            if (streams.remove(d) != null) {
                usedStreams.decrementAndGet();
                pendingVideos.remove(d);
                streamBuffers.remove(d);
            } else {
                if (log.isTraceEnabled()) {
                    log.trace("Failed to remove stream id: {} streams: {}", d, streams);
                }
            }
        }
    }

    /**
     * Handler for ping event.
     * 
     * @param ping
     *            Ping event context
     */
    public void ping(Ping ping) {
        getChannel(2).write(ping);
    }

    /**
     * Write packet.
     * 
     * @param out
     *            Packet
     */
    public abstract void write(Packet out);

    /**
     * Write raw byte buffer.
     * 
     * @param out
     *            IoBuffer
     */
    public abstract void writeRaw(IoBuffer out);

    /**
     * Update number of bytes to read next value.
     */
    protected void updateBytesRead() {
        if (log.isTraceEnabled()) {
            log.trace("updateBytesRead");
        }
        long bytesRead = getReadBytes();
        if (bytesRead >= nextBytesRead) {
            BytesRead sbr = new BytesRead((int) (bytesRead % Integer.MAX_VALUE));
            getChannel(2).write(sbr);
            nextBytesRead += bytesReadInterval;
        }
    }

    /**
     * Read number of received bytes.
     * 
     * @param bytes
     *            Number of bytes
     */
    public void receivedBytesRead(int bytes) {
        if (log.isDebugEnabled()) {
            log.debug("Client received {} bytes, written {} bytes, {} messages pending", new Object[] { bytes, getWrittenBytes(), getPendingMessages() });
        }
        clientBytesRead.addAndGet(bytes);
    }

    /**
     * Get number of bytes the client reported to have received.
     * 
     * @return Number of bytes
     */
    public long getClientBytesRead() {
        return clientBytesRead.get();
    }

    /** {@inheritDoc} */
    public void invoke(IServiceCall call) {
        invoke(call, 3);
    }

    /**
     * Generate next invoke id.
     * 
     * @return Next invoke id for RPC
     */
    public int getTransactionId() {
        return transactionId.incrementAndGet();
    }

    /**
     * Register pending call (remote function call that is yet to finish).
     * 
     * @param invokeId
     *            Deferred operation id
     * @param call
     *            Call service
     */
    public void registerPendingCall(int invokeId, IPendingServiceCall call) {
        pendingCalls.put(invokeId, call);
    }

    /** {@inheritDoc} */
    public void invoke(IServiceCall call, int channel) {
        // We need to use Invoke for all calls to the client
        Invoke invoke = new Invoke();
        invoke.setCall(call);
        invoke.setTransactionId(getTransactionId());
        if (call instanceof IPendingServiceCall) {
            registerPendingCall(invoke.getTransactionId(), (IPendingServiceCall) call);
        }
        getChannel(channel).write(invoke);
    }

    /** {@inheritDoc} */
    public void invoke(String method) {
        invoke(method, null, null);
    }

    /** {@inheritDoc} */
    public void invoke(String method, Object[] params) {
        invoke(method, params, null);
    }

    /** {@inheritDoc} */
    public void invoke(String method, IPendingServiceCallback callback) {
        invoke(method, null, callback);
    }

    /** {@inheritDoc} */
    public void invoke(String method, Object[] params, IPendingServiceCallback callback) {
        IPendingServiceCall call = new PendingCall(method, params);
        if (callback != null) {
            call.registerCallback(callback);
        }
        invoke(call);
    }

    /** {@inheritDoc} */
    public void notify(IServiceCall call) {
        notify(call, 3);
    }

    /** {@inheritDoc} */
    public void notify(IServiceCall call, int channel) {
        Notify notify = new Notify();
        notify.setCall(call);
        getChannel(channel).write(notify);
    }

    /** {@inheritDoc} */
    public void notify(String method) {
        notify(method, null);
    }

    /** {@inheritDoc} */
    public void notify(String method, Object[] params) {
        IServiceCall call = new Call(method, params);
        notify(call);
    }

    /** {@inheritDoc} */
    public void status(Status status) {
        status(status, 3);
    }

    /** {@inheritDoc} */
    public void status(Status status, int channel) {
        if (status != null) {
            getChannel(channel).sendStatus(status);
        }
    }

    /** {@inheritDoc} */
    @Override
    public long getReadBytes() {
        return 0;
    }

    /** {@inheritDoc} */
    @Override
    public long getWrittenBytes() {
        return 0;
    }

    /**
     * Get pending call service by id.
     * 
     * @param invokeId
     *            Pending call service id
     * @return Pending call service object
     */
    public IPendingServiceCall getPendingCall(int invokeId) {
        return pendingCalls.get(invokeId);
    }

    /**
     * Retrieves and removes the pending call service by id.
     * 
     * @param invokeId
     *            Pending call service id
     * @return Pending call service object
     */
    public IPendingServiceCall retrievePendingCall(int invokeId) {
        return pendingCalls.remove(invokeId);
    }

    /**
     * Generates new stream name.
     * 
     * @return New stream name
     */
    protected String createStreamName() {
        return UUID.randomUUID().toString();
    }

    /**
     * Mark message as being written.
     * 
     * @param message
     *            Message to mark
     */
    protected void writingMessage(Packet message) {
        if (message.getMessage() instanceof VideoData) {
            Number streamId = message.getHeader().getStreamId();
            final AtomicInteger value = new AtomicInteger();
            AtomicInteger old = pendingVideos.putIfAbsent(streamId.doubleValue(), value);
            if (old == null) {
                old = value;
            }
            old.incrementAndGet();
        }
    }

    /**
     * Increases number of read messages by one. Updates number of bytes read.
     */
    public void messageReceived() {
        if (log.isTraceEnabled()) {
            log.trace("messageReceived");
        }
        readMessages.incrementAndGet();
        // trigger generation of BytesRead messages
        updateBytesRead();
    }

    private String getMessageType(Packet packet) {
        final Header header = packet.getHeader();
        final byte headerDataType = header.getDataType();
        return messageTypeToName(headerDataType);
    }

    public String messageTypeToName(byte headerDataType) {
        switch (headerDataType) {
            case Constants.TYPE_AGGREGATE:
                return "TYPE_AGGREGATE";
            case Constants.TYPE_AUDIO_DATA:
                return "TYPE_AUDIO_DATA";
            case Constants.TYPE_VIDEO_DATA:
                return "TYPE_VIDEO_DATA";
            case Constants.TYPE_FLEX_SHARED_OBJECT:
                return "TYPE_FLEX_SHARED_OBJECT";
            case Constants.TYPE_SHARED_OBJECT:
                return "TYPE_SHARED_OBJECT";
            case Constants.TYPE_INVOKE:
                return "TYPE_INVOKE";
            case Constants.TYPE_FLEX_MESSAGE:
                return "TYPE_FLEX_MESSAGE";
            case Constants.TYPE_NOTIFY:
                return "TYPE_NOTIFY";
            case Constants.TYPE_FLEX_STREAM_SEND:
                return "TYPE_FLEX_STREAM_SEND";
            case Constants.TYPE_PING:
                return "TYPE_PING";
            case Constants.TYPE_BYTES_READ:
                return "TYPE_BYTES_READ";
            case Constants.TYPE_CHUNK_SIZE:
                return "TYPE_CHUNK_SIZE";
            case Constants.TYPE_CLIENT_BANDWIDTH:
                return "TYPE_CLIENT_BANDWIDTH";
            case Constants.TYPE_SERVER_BANDWIDTH:
                return "TYPE_SERVER_BANDWIDTH";
            default:
                return "UNKNOWN [" + headerDataType + "]";

        }
    }

    /**
     * Handle the incoming message.
     * 
     * @param message
     *            message
     */
    public void handleMessageReceived(Packet message) {
        if (log.isTraceEnabled()) {
            log.trace("handleMessageReceived - {}", sessionId);
        }
        final byte dataType = message.getHeader().getDataType();
        // route these types outside the executor
        switch (dataType) {
            case Constants.TYPE_PING:
            case Constants.TYPE_ABORT:
            case Constants.TYPE_BYTES_READ:
            case Constants.TYPE_CHUNK_SIZE:
            case Constants.TYPE_CLIENT_BANDWIDTH:
            case Constants.TYPE_SERVER_BANDWIDTH:
                // pass message to the handler
                try {
                    handler.messageReceived(this, message);
                } catch (Exception e) {
                    log.error("Error processing received message {}", sessionId, e);
                }
                break;
            default:
                if (executor != null) {
                    final String messageType = getMessageType(message);
                    try {
                        // increment the packet number
                        final long packetNumber = packetSequence.incrementAndGet();
                        if (executorQueueSizeToDropAudioPackets > 0 && currentQueueSize.get() >= executorQueueSizeToDropAudioPackets) {
                            if (message.getHeader().getDataType() == Constants.TYPE_AUDIO_DATA) {
                                // if there's a backlog of messages in the queue. Flash might have sent a burst of messages after a network congestion. Throw away packets that we are able to discard.
                                log.info("Queue threshold reached. Discarding packet: session=[{}], msgType=[{}], packetNum=[{}]", sessionId, messageType, packetNumber);
                                return;
                            }
                        }
                        // set the packet expiration time if maxHandlingTimeout is not disabled (set to 0)
                        if (maxHandlingTimeout > 0) {
                            message.setExpirationTime(System.currentTimeMillis() + maxHandlingTimeout);
                        }
                        int streamId = message.getHeader().getStreamId().intValue();
                        if (log.isTraceEnabled()) {
                            log.trace("Handling message for streamId: {}, channelId: {} Channels: {}", streamId, message.getHeader().getChannelId(), channels);
                        }
                        // create a task to setProcessing the message
                        ReceivedMessageTask task = new ReceivedMessageTask(sessionId, message, handler, this);
                        task.setPacketNumber(packetNumber);
                        // create a task queue
                        ReceivedMessageTaskQueue newStreamTasks = new ReceivedMessageTaskQueue(streamId, this);
                        // put the queue in the task by stream map
                        ReceivedMessageTaskQueue currentStreamTasks = tasksByStreams.putIfAbsent(streamId, newStreamTasks);
                        if (currentStreamTasks != null) {
                            // add the task to the existing queue
                            currentStreamTasks.addTask(task);
                        } else {
                            // add the task to the newly created and just added queue
                            newStreamTasks.addTask(task);
                        }
                    } catch (Exception e) {
                        log.error("Incoming message handling failed on session=[" + sessionId + "], messageType=[" + messageType + "]", e);
                        if (log.isDebugEnabled()) {
                            log.debug("Execution rejected on {} - {}", getSessionId(), RTMP.states[getStateCode()]);
                            log.debug("Lock permits - decode: {} encode: {}", decoderLock.availablePermits(), encoderLock.availablePermits());
                        }
                    }
                } else {
                    log.warn("Executor is null on {} state: {}", getSessionId(), RTMP.states[getStateCode()]);
                }
        }
    }

    @Override
    public void onTaskAdded(ReceivedMessageTaskQueue queue) {
        currentQueueSize.incrementAndGet();
        processTasksQueue(queue);
    }

    @Override
    public void onTaskRemoved(ReceivedMessageTaskQueue queue) {
        currentQueueSize.decrementAndGet();
        processTasksQueue(queue);
    }

    @SuppressWarnings("unchecked")
    private void processTasksQueue(final ReceivedMessageTaskQueue currentStreamTasks) {
        int streamId = currentStreamTasks.getStreamId();
        if (log.isTraceEnabled()) {
           log.trace("Process tasks for streamId {}", streamId);
        }
        final ReceivedMessageTask task = currentStreamTasks.getTaskToProcess();
        if (task != null) {
            Packet packet = task.getPacket();
            try {
                final String messageType = getMessageType(packet);
                ListenableFuture<Packet> future = (ListenableFuture<Packet>) executor.submitListenable(new ListenableFutureTask<Packet>(task));
                future.addCallback(new ListenableFutureCallback<Packet>() {

                    final long startTime = System.currentTimeMillis();

                    int getProcessingTime() {
                        return (int) (System.currentTimeMillis() - startTime);
                    }

                    public void onFailure(Throwable t) {
                        log.debug("ReceivedMessageTask failure: {}", t);
                        if (log.isWarnEnabled()) {
                            log.warn("onFailure - session: {}, msgtype: {}, processingTime: {}, packetNum: {}", sessionId, messageType, getProcessingTime(), task.getPacketNumber());
                        }
                        currentStreamTasks.removeTask(task);
                    }

                    public void onSuccess(Packet packet) {
                        log.debug("ReceivedMessageTask success");
                        if (log.isDebugEnabled()) {
                            log.debug("onSuccess - session: {}, msgType: {}, processingTime: {}, packetNum: {}", sessionId, messageType, getProcessingTime(), task.getPacketNumber());
                        }
                        currentStreamTasks.removeTask(task);
                    }

                });
            } catch (TaskRejectedException tre) {
                Throwable[] suppressed = tre.getSuppressed();
                for (Throwable t : suppressed) {
                    log.warn("Suppressed exception on {}", sessionId, t);
                }
                log.info("Rejected message: {} on {}", packet, sessionId);
                currentStreamTasks.removeTask(task);
            } catch (Throwable e) {
                log.error("Incoming message handling failed on session=[" + sessionId + "]", e);
                if (log.isDebugEnabled()) {
                    log.debug("Execution rejected on {} - {}", getSessionId(), RTMP.states[getStateCode()]);
                    log.debug("Lock permits - decode: {} encode: {}", decoderLock.availablePermits(), encoderLock.availablePermits());
                }
                currentStreamTasks.removeTask(task);
            }
        } else {
            if (log.isTraceEnabled()) {
                log.trace("Channel {} task queue is empty", streamId);
            }
        }
    }

    /**
     * Mark message as sent.
     * 
     * @param message
     *            Message to mark
     */
    public void messageSent(Packet message) {
        if (message.getMessage() instanceof VideoData) {
            Number streamId = message.getHeader().getStreamId();
            AtomicInteger pending = pendingVideos.get(streamId.doubleValue());
            if (log.isTraceEnabled()) {
                log.trace("Stream id: {} pending: {} total pending videos: {}", streamId, pending, pendingVideos.size());
            }
            if (pending != null) {
                pending.decrementAndGet();
            }
        }
        writtenMessages.incrementAndGet();
    }

    /**
     * Increases number of dropped messages.
     */
    protected void messageDropped() {
        droppedMessages.incrementAndGet();
    }

    /**
     * Returns the current received message queue size.
     * 
     * @return current message queue size
     */
    protected int currentQueueSize() {
        return currentQueueSize.get();
    }

    /** {@inheritDoc} */
    @Override
    public long getPendingVideoMessages(Number streamId) {
        AtomicInteger pendingCount = pendingVideos.get(streamId.doubleValue());
        if (log.isTraceEnabled()) {
            log.trace("Stream id: {} pendingCount: {} total pending videos: {}", streamId, pendingCount, pendingVideos.size());
        }
        return pendingCount != null ? pendingCount.intValue() : 0;
    }

    /**
     * Send a shared object message.
     * 
     * @param name
     *            shared object name
     * @param currentVersion
     *            the current version
     * @param persistent
     *            toggle
     * @param events
     *            shared object events
     */
    public void sendSharedObjectMessage(String name, int currentVersion, boolean persistent, ConcurrentLinkedQueue<ISharedObjectEvent> events) {
        // create a new sync message for every client to avoid concurrent access through multiple threads
        SharedObjectMessage syncMessage = state.getEncoding() == Encoding.AMF3 ? new FlexSharedObjectMessage(null, name, currentVersion, persistent) : new SharedObjectMessage(null, name, currentVersion, persistent);
        syncMessage.addEvents(events);
        try {
            // get the channel for so updates
            Channel channel = getChannel(3);
            if (log.isTraceEnabled()) {
                log.trace("Send to channel: {}", channel);
            }
            channel.write(syncMessage);
        } catch (Exception e) {
            log.warn("Exception sending shared object", e);
        }
    }

    /** {@inheritDoc} */
    public void ping() {
        long newPingTime = System.currentTimeMillis();
        if (log.isDebugEnabled()) {
            log.debug("Send Ping: session=[{}], currentTime=[{}], lastPingTime=[{}]", new Object[] { getSessionId(), newPingTime, lastPingSentOn.get() });
        }
        if (lastPingSentOn.get() == 0) {
            lastPongReceivedOn.set(newPingTime);
        }
        Ping pingRequest = new Ping();
        pingRequest.setEventType(Ping.PING_CLIENT);
        lastPingSentOn.set(newPingTime);
        int now = (int) (newPingTime & 0xffffffffL);
        pingRequest.setValue2(now);
        ping(pingRequest);
    }

    /**
     * Marks that ping back was received.
     * 
     * @param pong
     *            Ping object
     */
    public void pingReceived(Ping pong) {
        long now = System.currentTimeMillis();
        long previousPingTime = lastPingSentOn.get();
        int previousPingValue = (int) (previousPingTime & 0xffffffffL);
        int pongValue = pong.getValue2().intValue();
        if (log.isDebugEnabled()) {
            log.debug("Pong received: session=[{}] at {} with value {}, previous received at {}", new Object[] { getSessionId(), now, pongValue, previousPingValue });
        }
        if (pongValue == previousPingValue) {
            lastPingRoundTripTime.set((int) ((now - previousPingTime) & 0xffffffffL));
            if (log.isDebugEnabled()) {
                log.debug("Ping response session=[{}], RTT=[{} ms]", new Object[] { getSessionId(), lastPingRoundTripTime.get() });
            }
        } else {
            // don't log the congestion entry unless there are more than X messages waiting
            if (getPendingMessages() > 4) {
                int pingRtt = (int) ((now & 0xffffffffL)) - pongValue;
                log.info("Pong delayed: session=[{}], ping response took [{} ms] to arrive. Connection may be congested, or loopback", new Object[] { getSessionId(), pingRtt });
            }
        }
        lastPongReceivedOn.set(now);
    }

    /**
     * Difference between when the last ping was sent and when the last pong was received.
     * 
     * @return last interval of ping minus pong
     */
    public int getLastPingSentAndLastPongReceivedInterval() {
        return (int) (lastPingSentOn.get() - lastPongReceivedOn.get());
    }

    /** {@inheritDoc} */
    public int getLastPingTime() {
        return lastPingRoundTripTime.get();
    }

    /**
     * Setter for ping interval.
     * 
     * @param pingInterval
     *            Interval in ms to ping clients. Set to 0 to disable ghost detection code.
     */
    public void setPingInterval(int pingInterval) {
        this.pingInterval = pingInterval;
    }

    /**
     * Setter for maximum inactivity.
     * 
     * @param maxInactivity
     *            Maximum time in ms after which a client is disconnected in case of inactivity.
     */
    public void setMaxInactivity(int maxInactivity) {
        this.maxInactivity = maxInactivity;
    }

    /**
     * Inactive state event handler.
     */
    protected abstract void onInactive();

    /**
     * Sets the scheduler.
     * 
     * @param scheduler
     *            scheduling service / thread executor
     */
    public void setScheduler(ThreadPoolTaskScheduler scheduler) {
        this.scheduler = scheduler;
    }

    /**
     * @return the scheduler
     */
    public ThreadPoolTaskScheduler getScheduler() {
        return scheduler;
    }

    public ThreadPoolTaskExecutor getExecutor() {
        return executor;
    }

    public void setExecutor(ThreadPoolTaskExecutor executor) {
        this.executor = executor;
    }

    /**
     * Thread pool for guarding deadlocks
     *
     * @return the deadlockGuardScheduler
     */
    public ThreadPoolTaskScheduler getDeadlockGuardScheduler() {
        return deadlockGuardScheduler;
    }

    /**
     * Thread pool for guarding deadlocks
     * 
     * @param deadlockGuardScheduler
     *            the deadlockGuardScheduler to set
     */
    public void setDeadlockGuardScheduler(ThreadPoolTaskScheduler deadlockGuardScheduler) {
        this.deadlockGuardScheduler = deadlockGuardScheduler;
    }

    /**
     * Registers deferred result.
     * 
     * @param result
     *            Result to register
     */
    public void registerDeferredResult(DeferredResult result) {
        deferredResults.add(result);
    }

    /**
     * Unregister deferred result
     * 
     * @param result
     *            Result to unregister
     */
    public void unregisterDeferredResult(DeferredResult result) {
        deferredResults.remove(result);
    }

    public void rememberStreamBufferDuration(int streamId, int bufferDuration) {
        streamBuffers.put(streamId, bufferDuration);
    }

    /**
     * Set maximum time to wait for valid handshake in milliseconds.
     * 
     * @param maxHandshakeTimeout
     *            Maximum time in milliseconds
     */
    public void setMaxHandshakeTimeout(int maxHandshakeTimeout) {
        this.maxHandshakeTimeout = maxHandshakeTimeout;
    }

    public long getMaxHandlingTimeout() {
        return maxHandlingTimeout;
    }

    public void setMaxHandlingTimeout(long maxHandlingTimeout) {
        this.maxHandlingTimeout = maxHandlingTimeout;
    }

    public int getChannelsInitalCapacity() {
        return channelsInitalCapacity;
    }

    public void setChannelsInitalCapacity(int channelsInitalCapacity) {
        this.channelsInitalCapacity = channelsInitalCapacity;
    }

    public int getChannelsConcurrencyLevel() {
        return channelsConcurrencyLevel;
    }

    public void setChannelsConcurrencyLevel(int channelsConcurrencyLevel) {
        this.channelsConcurrencyLevel = channelsConcurrencyLevel;
    }

    public int getStreamsInitalCapacity() {
        return streamsInitalCapacity;
    }

    public void setStreamsInitalCapacity(int streamsInitalCapacity) {
        this.streamsInitalCapacity = streamsInitalCapacity;
    }

    public int getStreamsConcurrencyLevel() {
        return streamsConcurrencyLevel;
    }

    public void setStreamsConcurrencyLevel(int streamsConcurrencyLevel) {
        this.streamsConcurrencyLevel = streamsConcurrencyLevel;
    }

    public int getPendingCallsInitalCapacity() {
        return pendingCallsInitalCapacity;
    }

    public void setPendingCallsInitalCapacity(int pendingCallsInitalCapacity) {
        this.pendingCallsInitalCapacity = pendingCallsInitalCapacity;
    }

    public int getPendingCallsConcurrencyLevel() {
        return pendingCallsConcurrencyLevel;
    }

    public void setPendingCallsConcurrencyLevel(int pendingCallsConcurrencyLevel) {
        this.pendingCallsConcurrencyLevel = pendingCallsConcurrencyLevel;
    }

    public int getReservedStreamsInitalCapacity() {
        return reservedStreamsInitalCapacity;
    }

    public void setReservedStreamsInitalCapacity(int reservedStreamsInitalCapacity) {
        this.reservedStreamsInitalCapacity = reservedStreamsInitalCapacity;
    }

    public int getReservedStreamsConcurrencyLevel() {
        return reservedStreamsConcurrencyLevel;
    }

    public void setReservedStreamsConcurrencyLevel(int reservedStreamsConcurrencyLevel) {
        this.reservedStreamsConcurrencyLevel = reservedStreamsConcurrencyLevel;
    }

    /**
     * Specify the size of queue that will trigger audio packet dropping, disabled if it's 0
     * 
     * @param executorQueueSizeToDropAudioPackets
     *            queue size
     */
    public void setExecutorQueueSizeToDropAudioPackets(Integer executorQueueSizeToDropAudioPackets) {
        this.executorQueueSizeToDropAudioPackets = executorQueueSizeToDropAudioPackets;
    }

    @Override
    public String getProtocol() {
        return "rtmp";
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        if (log.isDebugEnabled()) {
            String id = getClient() != null ? getClient().getId() : null;
            return String.format("%1$s %2$s:%3$s to %4$s client: %5$s session: %6$s state: %7$s", new Object[] { getClass().getSimpleName(), getRemoteAddress(), getRemotePort(), getHost(), id, getSessionId(), RTMP.states[getStateCode()] });
        } else {
            Object[] args = new Object[] { getClass().getSimpleName(), getRemoteAddress(), getReadBytes(), getWrittenBytes(), getSessionId(), RTMP.states[getStateCode()] };
            return String.format("%1$s from %2$s (in: %3$s out: %4$s) session: %5$s state: %6$s", args);
        }
    }

    /**
     * Task that keeps connection alive and disconnects if client is dead.
     */
    private class KeepAliveTask implements Runnable {

        private final AtomicLong lastBytesRead = new AtomicLong(0);

        private volatile long lastBytesReadTime = 0;

        public void run() {
            // we dont ping until in connected state
            if (state.getState() == RTMP.STATE_CONNECTED) {
                // ensure the job is not already running
                if (running.compareAndSet(false, true)) {
                    if (log.isTraceEnabled()) {
                        log.trace("Running keep-alive for {}", getSessionId());
                    }
                    try {
                        // first check connected
                        if (isConnected()) {
                            // get now
                            long now = System.currentTimeMillis();
                            // get the current bytes read count on the connection
                            long currentReadBytes = getReadBytes();
                            // get our last bytes read count
                            long previousReadBytes = lastBytesRead.get();
                            if (log.isTraceEnabled()) {
                                log.trace("Time now: {} current read count: {} last read count: {}", new Object[] { now, currentReadBytes, previousReadBytes });
                            }
                            if (currentReadBytes > previousReadBytes) {
                                if (log.isTraceEnabled()) {
                                    log.trace("Client is still alive, no ping needed");
                                }
                                // client has sent data since last check and thus is not dead. No need to ping
                                if (lastBytesRead.compareAndSet(previousReadBytes, currentReadBytes)) {
                                    // update the timestamp to match our update
                                    lastBytesReadTime = now;
                                }
                                // check idle
                                if (isIdle()) {
                                    onInactive();
                                }
                            } else {
                                // client didn't send response to ping command and didn't sent data for too long, disconnect
                                long lastPingTime = lastPingSentOn.get();
                                long lastPongTime = lastPongReceivedOn.get();
                                if (lastPongTime > 0 && (lastPingTime - lastPongTime > maxInactivity) && (now - lastBytesReadTime > maxInactivity)) {
                                    log.warn("Closing connection - inactivity timeout: session=[{}], lastPongReceived=[{} ms ago], lastPingSent=[{} ms ago], lastDataRx=[{} ms ago]", new Object[] { getSessionId(), (lastPingTime - lastPongTime), (now - lastPingTime), (now - lastBytesReadTime) });
                                    // the following line deals with a very common support request
                                    log.warn("Client on session=[{}] has not responded to our ping for [{} ms] and we haven't received data for [{} ms]", new Object[] { getSessionId(), (lastPingTime - lastPongTime), (now - lastBytesReadTime) });
                                    onInactive();
                                } else {
                                    // send ping command to client to trigger sending of data
                                    ping();
                                }
                            }
                        } else {
                            if (log.isDebugEnabled()) {
                                log.debug("No longer connected, clean up connection. Connection state: {}", RTMP.states[state.getState()]);
                            }
                            onInactive();
                        }
                    } catch (Exception e) {
                        log.warn("Exception in keepalive for {}", getSessionId(), e);
                    } finally {
                        // reset running flag
                        running.compareAndSet(true, false);
                    }
                }
            }
        }
    }

    /**
     * Task that waits for a valid handshake and disconnects the client if none is received.
     */
    private class WaitForHandshakeTask implements Runnable {

        public WaitForHandshakeTask() {
            if (log.isTraceEnabled()) {
                log.trace("WaitForHandshakeTask created on scheduler: {} for session: {}", scheduler, getSessionId());
            }
        }

        public void run() {
            if (log.isTraceEnabled()) {
                log.trace("WaitForHandshakeTask started for {}", getSessionId());
            }
            // check for connected state before disconnecting
            if (state.getState() != RTMP.STATE_CONNECTED) {
                // Client didn't send a valid handshake, disconnect
                log.warn("Closing {}, due to long handshake. State: {}", getSessionId(), RTMP.states[getStateCode()]);
                onInactive();
            }
        }

    }

}
