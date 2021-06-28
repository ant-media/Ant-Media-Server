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

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Set;

import org.apache.mina.core.session.IoSession;
import org.red5.io.object.StreamAction;
import org.red5.server.api.event.IEventDispatcher;
import org.red5.server.api.service.IPendingServiceCall;
import org.red5.server.api.service.IPendingServiceCallback;
import org.red5.server.api.service.IServiceCall;
import org.red5.server.api.stream.IClientStream;
import org.red5.server.net.ICommand;
import org.red5.server.net.IConnectionManager;
import org.red5.server.net.rtmp.codec.RTMP;
import org.red5.server.net.rtmp.event.BytesRead;
import org.red5.server.net.rtmp.event.ChunkSize;
import org.red5.server.net.rtmp.event.ClientBW;
import org.red5.server.net.rtmp.event.IRTMPEvent;
import org.red5.server.net.rtmp.event.Invoke;
import org.red5.server.net.rtmp.event.Notify;
import org.red5.server.net.rtmp.event.Ping;
import org.red5.server.net.rtmp.event.ServerBW;
import org.red5.server.net.rtmp.event.Unknown;
import org.red5.server.net.rtmp.message.Constants;
import org.red5.server.net.rtmp.message.Header;
import org.red5.server.net.rtmp.message.Packet;
import org.red5.server.net.rtmp.status.StatusCodes;
import org.red5.server.so.SharedObjectMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for all RTMP handlers.
 * 
 * @author The Red5 Project
 */
public abstract class BaseRTMPHandler implements IRTMPHandler, Constants, StatusCodes {

    private static Logger log = LoggerFactory.getLogger(BaseRTMPHandler.class);

    /** {@inheritDoc} */
    public void connectionOpened(RTMPConnection conn) {
        if (log.isTraceEnabled()) {
            log.trace("connectionOpened - conn: {} state: {}", conn, conn.getState());
        }
        conn.open();
        // start the wait for handshake
        conn.startWaitForHandshake();
    }

    /** {@inheritDoc} */
    public void messageReceived(RTMPConnection conn, Packet packet) throws Exception {
        log.trace("messageReceived connection: {}", conn.getSessionId());
        if (conn != null) {
            IRTMPEvent message = null;
            try {
                message = packet.getMessage();
                final Header header = packet.getHeader();
                final Number streamId = header.getStreamId();
                final Channel channel = conn.getChannel(header.getChannelId());
                final IClientStream stream = conn.getStreamById(streamId);
                if (log.isTraceEnabled()) {
                    log.trace("Message received - header: {}", header);
                }
                // set stream id on the connection
                conn.setStreamId(streamId);
                // increase number of received messages
                conn.messageReceived();
                // set the source of the message
                message.setSource(conn);
                // process based on data type
                final byte headerDataType = header.getDataType();
                if (log.isTraceEnabled()) {
                    log.trace("Header / message data type: {}", headerDataType);
                }
                switch (headerDataType) {
                    case TYPE_AGGREGATE:
                        log.debug("Aggregate type data - header timer: {} size: {}", header.getTimer(), header.getSize());
                    case TYPE_AUDIO_DATA:
                    case TYPE_VIDEO_DATA:
                        // mark the event as from a live source
                        // log.trace("Marking message as originating from a Live source");
                        message.setSourceType(Constants.SOURCE_TYPE_LIVE);
                        // NOTE: If we respond to "publish" with "NetStream.Publish.BadName",
                        // the client sends a few stream packets before stopping. We need to ignore them
                        if (stream != null) {
                            ((IEventDispatcher) stream).dispatchEvent(message);
                        }
                        break;
                    case TYPE_FLEX_SHARED_OBJECT:
                    case TYPE_SHARED_OBJECT:
                        onSharedObject(conn, channel, header, (SharedObjectMessage) message);
                        break;
                    case TYPE_INVOKE:
                    case TYPE_FLEX_MESSAGE:
                        onCommand(conn, channel, header, (Invoke) message);
                        IPendingServiceCall call = ((Invoke) message).getCall();
                        if (message.getHeader().getStreamId().intValue() != 0 && call.getServiceName() == null && StreamAction.PUBLISH.equals(call.getServiceMethodName())) {
                            if (stream != null) {
                                // Only dispatch if stream really was created
                                ((IEventDispatcher) stream).dispatchEvent(message);
                            }
                        }
                        break;
                    case TYPE_NOTIFY: 
                        // like an invoke, but does not return anything and has a invoke / transaction id of 0
                    case TYPE_FLEX_STREAM_SEND:
                        if (((Notify) message).getData() != null && stream != null) {
                            // Stream metadata
                            ((IEventDispatcher) stream).dispatchEvent(message);
                        } else {
                            onCommand(conn, channel, header, (Notify) message);
                        }
                        break;
                    case TYPE_PING:
                        onPing(conn, channel, header, (Ping) message);
                        break;
                    case TYPE_BYTES_READ:
                        onStreamBytesRead(conn, channel, header, (BytesRead) message);
                        break;
                    case TYPE_CHUNK_SIZE:
                        onChunkSize(conn, channel, header, (ChunkSize) message);
                        break;
                    case Constants.TYPE_CLIENT_BANDWIDTH: // onBWDone / peer bw
                        log.debug("Client bandwidth: {}", message);
                        onClientBandwidth(conn, channel, (ClientBW) message);
                        break;
                    case Constants.TYPE_SERVER_BANDWIDTH: // window ack size
                        log.debug("Server bandwidth: {}", message);
                        onServerBandwidth(conn, channel, (ServerBW) message);
                        break;
                    default:
                        log.debug("Unknown type: {}", header.getDataType());
                }
                if (message instanceof Unknown) {
                    log.info("Message type unknown: {}", message);
                }
            } catch (Throwable t) {
                log.error("Exception", t);
            }
            // XXX this may be causing 'missing' data if previous methods are
            // not making copies before buffering etc..
            if (message != null) {
                message.release();
            }
        }
    }

    /** {@inheritDoc} */
    public void messageSent(RTMPConnection conn, Packet packet) {
        log.trace("Message sent");
        // increase number of sent messages
        conn.messageSent(packet);
    }

    /** {@inheritDoc} */
    public void connectionClosed(RTMPConnection conn) {
        log.debug("connectionClosed: {}", conn.getSessionId());
        if (conn.getStateCode() != RTMP.STATE_DISCONNECTED) {
            // inform any callbacks for pending calls that the connection is closed
            conn.sendPendingServiceCallsCloseError();
            // close the connection
            if (conn.getStateCode() != RTMP.STATE_DISCONNECTING) {
                conn.close();
            }
            // set as disconnected
            conn.setStateCode(RTMP.STATE_DISCONNECTED);
        }
        IoSession session = conn.getIoSession();
        if (session != null && session.containsAttribute(RTMPConnection.RTMP_CONN_MANAGER)) {
            @SuppressWarnings("unchecked")
            IConnectionManager<RTMPConnection> connManager = (IConnectionManager<RTMPConnection>) ((WeakReference<?>) session.getAttribute(RTMPConnection.RTMP_CONN_MANAGER)).get();
            if (connManager != null) {
                connManager.removeConnection(conn.getSessionId());
            } else {
                log.debug("Connection manager was not found in the session");
            }
        }
        log.trace("connectionClosed: {}", conn);
    }

    /**
     * Return hostname for URL.
     * 
     * @param url
     *            URL
     * @return Hostname from that URL
     */
    protected String getHostname(String url) {
        if (log.isDebugEnabled()) {
            log.debug("getHostname - url: {}", url);
        }
        String[] parts = url.split("/");
        if (parts.length == 2) {
            return "";
        } else {
            String host = parts[2];
            // strip out default port in case the client added the port explicitly
            if (host.endsWith(":1935")) {
                // remove default port from connection string
                return host.substring(0, host.length() - 5);
            }
            return host;
        }
    }

    /**
     * Handler for pending call result. Dispatches results to all pending call handlers.
     * 
     * @param conn
     *            Connection
     * @param invoke
     *            Pending call result event context
     */
    protected void handlePendingCallResult(RTMPConnection conn, Invoke invoke) {
        final IServiceCall call = invoke.getCall();
        final IPendingServiceCall pendingCall = conn.retrievePendingCall(invoke.getTransactionId());
        if (pendingCall != null) {
            // The client sent a response to a previously made call.
            Object[] args = call.getArguments();
            if (args != null && args.length > 0) {
                // TODO: can a client return multiple results?
                pendingCall.setResult(args[0]);
            }
            Set<IPendingServiceCallback> callbacks = pendingCall.getCallbacks();
            if (!callbacks.isEmpty()) {
                HashSet<IPendingServiceCallback> tmp = new HashSet<>();
                tmp.addAll(callbacks);
                for (IPendingServiceCallback callback : tmp) {
                    try {
                        callback.resultReceived(pendingCall);
                    } catch (Exception e) {
                        log.error("Error while executing callback {}", callback, e);
                    }
                }
            }
        }
    }

    /**
     * Chunk size change event handler. Abstract, to be implemented in subclasses.
     * 
     * @param conn
     *            Connection
     * @param channel
     *            Channel
     * @param source
     *            Header
     * @param chunkSize
     *            New chunk size
     */
    protected abstract void onChunkSize(RTMPConnection conn, Channel channel, Header source, ChunkSize chunkSize);

    /**
     * Command event handler, which current consists of an Invoke or Notify type object.
     * 
     * @param conn
     *            Connection
     * @param channel
     *            Channel
     * @param source
     *            Header
     * @param command
     *            event context
     */
    protected abstract void onCommand(RTMPConnection conn, Channel channel, Header source, ICommand command);

    /**
     * Ping event handler.
     * 
     * @param conn
     *            Connection
     * @param channel
     *            Channel
     * @param source
     *            Header
     * @param ping
     *            Ping event context
     */
    protected abstract void onPing(RTMPConnection conn, Channel channel, Header source, Ping ping);

    /**
     * Server bandwidth / Window ACK size event handler.
     * 
     * @param conn
     *            Connection
     * @param channel
     *            Channel
     * @param message
     *            ServerBW
     */
    protected void onServerBandwidth(RTMPConnection conn, Channel channel, ServerBW message) {

    }

    /**
     * Client bandwidth / Peer bandwidth set event handler.
     * 
     * @param conn
     *            Connection
     * @param channel
     *            Channel
     * @param message
     *            ClientBW
     */
    protected void onClientBandwidth(RTMPConnection conn, Channel channel, ClientBW message) {

    }

    /**
     * Stream bytes read event handler.
     * 
     * @param conn
     *            Connection
     * @param channel
     *            Channel
     * @param source
     *            Header
     * @param streamBytesRead
     *            Bytes read event context
     */
    protected void onStreamBytesRead(RTMPConnection conn, Channel channel, Header source, BytesRead streamBytesRead) {
        conn.receivedBytesRead(streamBytesRead.getBytesRead());
    }

    /**
     * Shared object event handler.
     * 
     * @param conn
     *            Connection
     * @param channel
     *            Channel
     * @param source
     *            Header
     * @param message
     *            Shared object message
     */
    protected abstract void onSharedObject(RTMPConnection conn, Channel channel, Header source, SharedObjectMessage message);

}
