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

import org.apache.mina.core.buffer.IoBuffer;
import org.red5.server.api.scope.IScope;
import org.red5.server.api.stream.IClientStream;
import org.red5.server.api.stream.IRtmpSampleAccess;
import org.red5.server.net.rtmp.event.IRTMPEvent;
import org.red5.server.net.rtmp.event.Invoke;
import org.red5.server.net.rtmp.event.Notify;
import org.red5.server.net.rtmp.message.Header;
import org.red5.server.net.rtmp.message.Packet;
import org.red5.server.net.rtmp.status.Status;
import org.red5.server.net.rtmp.status.StatusCodes;
import org.red5.server.service.Call;
import org.red5.server.service.PendingCall;
import org.red5.server.stream.IStreamData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Identified connection that transfers packets.
 */
public class Channel {

    protected static Logger log = LoggerFactory.getLogger(Channel.class);

    private final static String CALL_ON_STATUS = "onStatus";

    /**
     * RTMP connection used to transfer packets.
     */
    private final RTMPConnection connection;

    /**
     * Channel id
     */
    private final int id;

    /**
     * Creates channel from connection and channel id
     * 
     * @param conn
     *            Connection
     * @param channelId
     *            Channel id
     */
    public Channel(RTMPConnection conn, int channelId) {
        assert (conn != null);
        connection = conn;
        id = channelId;
    }

    /**
     * Closes channel with this id on RTMP connection.
     */
    public void close() {
        log.debug("Closing channel: {}", id);
        connection.closeChannel(id);
    }

    /**
     * Getter for id.
     *
     * @return Channel ID
     */
    public int getId() {
        return id;
    }

    /**
     * Getter for RTMP connection.
     *
     * @return RTMP connection
     */
    protected RTMPConnection getConnection() {
        return connection;
    }

    /**
     * Writes packet from event data to RTMP connection.
     *
     * @param event
     *            Event data
     */
    public void write(IRTMPEvent event) {
        if (!connection.isClosed()) {
            final IClientStream stream = connection.getStreamByChannelId(id);
            if (id > 3 && stream == null) {
                log.warn("Non-existant stream for channel id: {}, session: {} discarding: {}", id, connection.getSessionId(), event);
            }
            // if the stream is non-existant, the event will go out with stream id == 0
            final Number streamId = (stream == null) ? 0 : stream.getStreamId();
            write(event, streamId);
        } else {
            log.debug("Connection {} is closed, cannot write to channel: {}", connection.getSessionId(), id);
        }
    }

    /**
     * Writes packet from event data to RTMP connection and stream id.
     *
     * @param event
     *            Event data
     * @param streamId
     *            Stream id
     */
    private void write(IRTMPEvent event, Number streamId) {
        log.trace("write to stream id: {} channel: {}", streamId, id);
        final Header header = new Header();
        final Packet packet = new Packet(header, event);
        // set the channel id
        header.setChannelId(id);
        int ts = event.getTimestamp();
        if (ts != 0) {
            header.setTimer(event.getTimestamp());
        }
        header.setStreamId(streamId);
        header.setDataType(event.getDataType());
        // should use RTMPConnection specific method.. 
        //log.trace("Connection type for write: {}", connection.getClass().getName());
        connection.write(packet);
    }

    /**
     * Discard an event routed to this channel.
     * 
     * @param event
     */
    @SuppressWarnings("unused")
    private void discard(IRTMPEvent event) {
        if (event instanceof IStreamData<?>) {
            log.debug("Discarding: {}", ((IStreamData<?>) event).toString());
            IoBuffer data = ((IStreamData<?>) event).getData();
            if (data != null) {
                log.trace("Freeing discarded event data");
                data.free();
                data = null;
            }
        }
        event.setHeader(null);
    }

    /**
     * Sends status notification.
     *
     * @param status
     *            Status
     */
    public void sendStatus(Status status) {
        if (connection != null) {
            final boolean andReturn = !status.getCode().equals(StatusCodes.NS_DATA_START);
            final Invoke event = new Invoke();
            if (andReturn) {
                final PendingCall call = new PendingCall(null, CALL_ON_STATUS, new Object[] { status });
                if (status.getCode().equals(StatusCodes.NS_PLAY_START)) {
                    IScope scope = connection.getScope();
                    if (scope.getContext().getApplicationContext().containsBean(IRtmpSampleAccess.BEAN_NAME)) {
                        IRtmpSampleAccess sampleAccess = (IRtmpSampleAccess) scope.getContext().getApplicationContext().getBean(IRtmpSampleAccess.BEAN_NAME);
                        boolean videoAccess = sampleAccess.isVideoAllowed(scope);
                        boolean audioAccess = sampleAccess.isAudioAllowed(scope);
                        if (videoAccess || audioAccess) {
                            final Call call2 = new Call(null, "|RtmpSampleAccess", null);
                            Notify notify = new Notify();
                            notify.setCall(call2);
                            notify.setData(IoBuffer.wrap(new byte[] { 0x01, (byte) (audioAccess ? 0x01 : 0x00), 0x01, (byte) (videoAccess ? 0x01 : 0x00) }));
                            write(notify, connection.getStreamIdForChannelId(id));
                        }
                    }
                }
                event.setCall(call);
            } else {
                final Call call = new Call(null, CALL_ON_STATUS, new Object[] { status });
                event.setCall(call);
            }
            // send directly to the corresponding stream as for some status codes, no stream has been created  and thus "getStreamByChannelId" will fail
            write(event, connection.getStreamIdForChannelId(id));
        }
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        if (connection != null) {
            return "Channel [id=" + id + ", stream id=" + connection.getStreamIdForChannelId(id) + ", session=" + connection.getSessionId() + "]";
        }
        return "Channel [id=" + id + "]";
    }

}
