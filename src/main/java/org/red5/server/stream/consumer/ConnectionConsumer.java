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

package org.red5.server.stream.consumer;

import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.mina.core.buffer.IoBuffer;
import org.red5.server.api.stream.IClientStream;
import org.red5.server.messaging.IMessage;
import org.red5.server.messaging.IMessageComponent;
import org.red5.server.messaging.IPipe;
import org.red5.server.messaging.IPipeConnectionListener;
import org.red5.server.messaging.IPushableConsumer;
import org.red5.server.messaging.OOBControlMessage;
import org.red5.server.messaging.PipeConnectionEvent;
import org.red5.server.net.rtmp.Channel;
import org.red5.server.net.rtmp.RTMPConnection;
import org.red5.server.net.rtmp.event.AudioData;
import org.red5.server.net.rtmp.event.BytesRead;
import org.red5.server.net.rtmp.event.ChunkSize;
import org.red5.server.net.rtmp.event.FlexStreamSend;
import org.red5.server.net.rtmp.event.IRTMPEvent;
import org.red5.server.net.rtmp.event.Notify;
import org.red5.server.net.rtmp.event.Ping;
import org.red5.server.net.rtmp.event.VideoData;
import org.red5.server.net.rtmp.message.Constants;
import org.red5.server.net.rtmp.message.Header;
import org.red5.server.stream.message.RTMPMessage;
import org.red5.server.stream.message.ResetMessage;
import org.red5.server.stream.message.StatusMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RTMP connection consumer.
 */
public class ConnectionConsumer implements IPushableConsumer, IPipeConnectionListener {

    private static final Logger log = LoggerFactory.getLogger(ConnectionConsumer.class);

    /**
     * Connection consumer class name
     */
    public static final String KEY = ConnectionConsumer.class.getName();

    /**
     * Connection object
     */
    private RTMPConnection conn;

    /**
     * Video channel
     */
    private Channel video;

    /**
     * Audio channel
     */
    private Channel audio;

    /**
     * Data channel
     */
    private Channel data;

    /**
     * Chunk size. Packets are sent chunk-by-chunk.
     */
    private int chunkSize = 1024; //TODO: Not sure of the best value here

    /**
     * Whether or not the chunk size has been sent. This seems to be required for h264.
     */
    private AtomicBoolean chunkSizeSent = new AtomicBoolean(false);

    /**
     * Create RTMP connection consumer for given connection and channels.
     * 
     * @param conn
     *            RTMP connection
     * @param videoChannel
     *            Video channel
     * @param audioChannel
     *            Audio channel
     * @param dataChannel
     *            Data channel
     */
    public ConnectionConsumer(RTMPConnection conn, Channel videoChannel, Channel audioChannel, Channel dataChannel) {
        log.debug("Channel ids - video: {} audio: {} data: {}", new Object[] { videoChannel, audioChannel, dataChannel });
        this.conn = conn;
        this.video = videoChannel;
        this.audio = audioChannel;
        this.data = dataChannel;
    }

    /**
     * Create connection consumer without an RTMP connection.
     * 
     * @param videoChannel
     *            video channel
     * @param audioChannel
     *            audio channel
     * @param dataChannel
     *            data channel
     */
    public ConnectionConsumer(Channel videoChannel, Channel audioChannel, Channel dataChannel) {
        this.video = videoChannel;
        this.audio = audioChannel;
        this.data = dataChannel;
    }

    /** {@inheritDoc} */
    public void pushMessage(IPipe pipe, IMessage message) {
        //log.trace("pushMessage - type: {}", message.getMessageType());
        if (message instanceof ResetMessage) {
            //ignore
        } else if (message instanceof StatusMessage) {
            StatusMessage statusMsg = (StatusMessage) message;
            data.sendStatus(statusMsg.getBody());
        } else if (message instanceof RTMPMessage) {
            // make sure chunk size has been sent
            sendChunkSize();
            // cast to rtmp message
            RTMPMessage rtmpMsg = (RTMPMessage) message;
            IRTMPEvent msg = rtmpMsg.getBody();
            // get timestamp
            int eventTime = msg.getTimestamp();
            log.debug("Message timestamp: {}", eventTime);
            if (eventTime < 0) {
                log.debug("Message has negative timestamp: {}", eventTime);
                return;
            }
            // get the data type
            byte dataType = msg.getDataType();
            log.trace("Data type: {}", dataType);
            // create a new header for the consumer
            final Header header = new Header();
            header.setTimerBase(eventTime);
            // data buffer
            IoBuffer buf = null;
            switch (dataType) {
                case Constants.TYPE_AGGREGATE:
                    log.trace("Aggregate data");
                    data.write(msg);
                    break;
                case Constants.TYPE_AUDIO_DATA:
                    log.trace("Audio data");
                    buf = ((AudioData) msg).getData();
                    if (buf != null) {
                        AudioData audioData = new AudioData(buf.asReadOnlyBuffer());
                        audioData.setHeader(header);
                        audioData.setTimestamp(header.getTimer());
                        log.trace("Source type: {}", ((AudioData) msg).getSourceType());
                        audioData.setSourceType(((AudioData) msg).getSourceType());
                        audio.write(audioData);
                    } else {
                        log.warn("Audio data was not found");
                    }
                    break;
                case Constants.TYPE_VIDEO_DATA:
                    log.trace("Video data");
                    buf = ((VideoData) msg).getData();
                    if (buf != null) {
                        VideoData videoData = new VideoData(buf.asReadOnlyBuffer());
                        videoData.setHeader(header);
                        videoData.setTimestamp(header.getTimer());
                        log.trace("Source type: {}", ((VideoData) msg).getSourceType());
                        videoData.setSourceType(((VideoData) msg).getSourceType());
                        video.write(videoData);
                    } else {
                        log.warn("Video data was not found");
                    }
                    break;
                case Constants.TYPE_PING:
                    log.trace("Ping");
                    Ping ping = (Ping) msg;
                    ping.setHeader(header);
                    conn.ping(ping);
                    break;
                case Constants.TYPE_STREAM_METADATA:
                    if (log.isTraceEnabled()) {
                        log.trace("Meta data: {}", (Notify) msg);
                    }
                    //Notify notify = new Notify(((Notify) msg).getData().asReadOnlyBuffer());
                    Notify notify = (Notify) msg;
                    notify.setHeader(header);
                    notify.setTimestamp(header.getTimer());
                    data.write(notify);
                    break;
                case Constants.TYPE_FLEX_STREAM_SEND:
                    if (log.isTraceEnabled()) {
                        log.trace("Flex stream send: {}", (Notify) msg);
                    }
                    FlexStreamSend send = null;
                    if (msg instanceof FlexStreamSend) {
                        send = (FlexStreamSend) msg;
                    } else {
                        send = new FlexStreamSend(((Notify) msg).getData().asReadOnlyBuffer());
                    }
                    send.setHeader(header);
                    send.setTimestamp(header.getTimer());
                    data.write(send);
                    break;
                case Constants.TYPE_BYTES_READ:
                    log.trace("Bytes read");
                    BytesRead bytesRead = (BytesRead) msg;
                    bytesRead.setHeader(header);
                    bytesRead.setTimestamp(header.getTimer());
                    conn.getChannel((byte) 2).write(bytesRead);
                    break;
                default:
                    log.trace("Default: {}", dataType);
                    data.write(msg);
            }
        } else {
            log.debug("Unhandled push message: {}", message);
            if (log.isTraceEnabled()) {
                Class<? extends IMessage> clazz = message.getClass();
                log.trace("Class info - name: {} declaring: {} enclosing: {}", new Object[] { clazz.getName(), clazz.getDeclaringClass(), clazz.getEnclosingClass() });
            }
        }
    }

    /** {@inheritDoc} */
    public void onPipeConnectionEvent(PipeConnectionEvent event) {
        if (event.getType().equals(PipeConnectionEvent.EventType.PROVIDER_DISCONNECT)) {
            closeChannels();
        }
    }

    /** {@inheritDoc} */
    public void onOOBControlMessage(IMessageComponent source, IPipe pipe, OOBControlMessage oobCtrlMsg) {
        if ("ConnectionConsumer".equals(oobCtrlMsg.getTarget())) {
            String serviceName = oobCtrlMsg.getServiceName();
            log.trace("Service name: {}", serviceName);
            if ("pendingCount".equals(serviceName)) {
                oobCtrlMsg.setResult(conn.getPendingMessages());
            } else if ("pendingVideoCount".equals(serviceName)) {
                IClientStream stream = conn.getStreamByChannelId(video.getId());
                if (stream != null) {
                    oobCtrlMsg.setResult(conn.getPendingVideoMessages(stream.getStreamId()));
                } else {
                    oobCtrlMsg.setResult(0L);
                }
            } else if ("writeDelta".equals(serviceName)) {
                //TODO: Revisit the max stream value later
                long maxStream = 120 * 1024;
                // Return the current delta between sent bytes and bytes the client
                // reported to have received, and the interval the client should use
                // for generating BytesRead messages (half of the allowed bandwidth).
                oobCtrlMsg.setResult(new Long[] { conn.getWrittenBytes() - conn.getClientBytesRead(), maxStream / 2 });
            } else if ("chunkSize".equals(serviceName)) {
                int newSize = (Integer) oobCtrlMsg.getServiceParamMap().get("chunkSize");
                if (newSize != chunkSize) {
                    chunkSize = newSize;
                    chunkSizeSent.set(false);
                    sendChunkSize();
                }
            }
        }
    }

    /**
     * Send the chunk size
     */
    private void sendChunkSize() {
        if (chunkSizeSent.compareAndSet(false, true)) {
            log.debug("Sending chunk size: {}", chunkSize);
            ChunkSize chunkSizeMsg = new ChunkSize(chunkSize);
            conn.getChannel((byte) 2).write(chunkSizeMsg);
        }
    }

    /**
     * Close all the channels
     */
    private void closeChannels() {
        conn.closeChannel(video.getId());
        conn.closeChannel(audio.getId());
        conn.closeChannel(data.getId());
    }

}
