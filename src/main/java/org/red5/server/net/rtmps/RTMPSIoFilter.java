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

package org.red5.server.net.rtmps;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.ssl.SslFilter.SslFilterMessage;
import org.red5.server.net.rtmp.InboundHandshake;
import org.red5.server.net.rtmp.RTMPConnManager;
import org.red5.server.net.rtmp.RTMPConnection;
import org.red5.server.net.rtmp.RTMPMinaConnection;
import org.red5.server.net.rtmp.codec.RTMP;
import org.red5.server.net.rtmp.codec.RTMPMinaCodecFactory;
import org.red5.server.net.rtmp.message.Constants;
import org.red5.server.net.rtmpe.RTMPEIoFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RTMPS IO filter - Server version.
 * 
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class RTMPSIoFilter extends RTMPEIoFilter {

    private static final Logger log = LoggerFactory.getLogger(RTMPSIoFilter.class);

    @Override
    public void messageReceived(NextFilter nextFilter, IoSession session, Object obj) throws Exception {
        log.trace("messageReceived nextFilter: {} session: {} message: {}", nextFilter, session, obj);
        if (obj instanceof SslFilterMessage || !session.isSecured()) {
            log.trace("Either ssl message or un-secured session: {}", session.isSecured());
            nextFilter.messageReceived(session, obj);
        } else {
            String sessionId = (String) session.getAttribute(RTMPConnection.RTMP_SESSION_ID);
            if (sessionId != null) {
                log.debug("RTMPS Session id: {}", sessionId);
                RTMPMinaConnection conn = (RTMPMinaConnection) RTMPConnManager.getInstance().getConnectionBySessionId(sessionId);
                // filter based on current connection state
                RTMP rtmp = conn.getState();
                final byte connectionState = conn.getStateCode();
                // assume message is an IoBuffer
                IoBuffer message = (IoBuffer) obj;
                // client handshake handling
                InboundHandshake handshake = null;
                switch (connectionState) {
                    case RTMP.STATE_CONNECT:
                        if (message.indexOf("P".getBytes()[0]) == 0) {
                            log.info("Non-native RTMPS connection requested for: {}", sessionId);
                            // indicates that the FP sent "POST" for a non-native rtmps connection
                            break;
                        }
                        // get the handshake from the session and process C0+C1 if we have enough bytes
                        handshake = (InboundHandshake) session.getAttribute(RTMPConnection.RTMP_HANDSHAKE);
                        // buffer the incoming message or part of message
                        handshake.addBuffer(message);
                        // check the size, we want 1537 bytes for C0C1
                        int c0c1Size = handshake.getBufferSize();
                        log.trace("Incoming C0C1 size: {}", c0c1Size);
                        if (c0c1Size >= (Constants.HANDSHAKE_SIZE + 1)) {
                            log.debug("decodeHandshakeC0C1");
                            // get the buffered bytes
                            IoBuffer buf = handshake.getBufferAsIoBuffer();
                            // set handshake to match client requested type
                            byte connectionType = buf.get();
                            handshake.setHandshakeType(connectionType);
                            log.trace("Incoming C0 connection type: {}", connectionType);
                            // create array for decode
                            byte[] dst = new byte[Constants.HANDSHAKE_SIZE];
                            // copy out 1536 bytes
                            buf.get(dst);
                            //log.debug("C1 - buffer: {}", Hex.encodeHexString(dst));
                            // set state to indicate we're waiting for C2
                            rtmp.setState(RTMP.STATE_HANDSHAKE);
                            // buffer any extra bytes
                            int remaining = buf.remaining();
                            if (remaining > 0) {
                                // store the remaining bytes in a thread local for use by C2 decoding
                                handshake.addBuffer(buf);
                                log.trace("Stored {} bytes for later decoding", remaining);
                            }
                            IoBuffer s1 = handshake.decodeClientRequest1(IoBuffer.wrap(dst));
                            if (s1 != null) {
                                //log.trace("S1 byte order: {}", s1.order());
                                session.write(s1);
                            } else {
                                log.warn("Client was rejected due to invalid handshake");
                                conn.close();
                            }
                        }
                        break;
                    case RTMP.STATE_HANDSHAKE:
                        // get the handshake from the session and process C2 if we have enough bytes
                        handshake = (InboundHandshake) session.getAttribute(RTMPConnection.RTMP_HANDSHAKE);
                        // buffer the incoming message
                        handshake.addBuffer(message);
                        // no connection type byte is supposed to be in C2 data
                        int c2Size = handshake.getBufferSize();
                        log.trace("Incoming C2 size: {}", c2Size);
                        if (c2Size >= Constants.HANDSHAKE_SIZE) {
                            log.debug("decodeHandshakeC2");
                            // get the buffered bytes
                            IoBuffer buf = handshake.getBufferAsIoBuffer();
                            // create array for decode
                            byte[] dst = new byte[Constants.HANDSHAKE_SIZE];
                            // get C2 out
                            buf.get(dst);
                            if (handshake.decodeClientRequest2(IoBuffer.wrap(dst))) {
                                log.debug("Connected, removing handshake data and adding rtmp protocol filter");
                                // set state to indicate we're connected
                                rtmp.setState(RTMP.STATE_CONNECTED);
                                // remove handshake from session now that we are connected
                                session.removeAttribute(RTMPConnection.RTMP_HANDSHAKE);
                                // add protocol filter as the last one in the chain
                                log.debug("Adding RTMP protocol filter");
                                session.getFilterChain().addAfter("rtmpsFilter", "protocolFilter", new ProtocolCodecFilter(new RTMPMinaCodecFactory()));
                                // check for remaining stored bytes left over from C0C1 and prepend to the dst array
                                if (buf.hasRemaining()) {
                                    log.trace("Receiving message: {}", buf);
                                    nextFilter.messageReceived(session, buf);
                                }
                            } else {
                                log.warn("Client was rejected due to invalid handshake");
                                conn.close();
                            }
                        }
                    case RTMP.STATE_CONNECTED:
                        log.trace("Receiving message: {}", message);
                        nextFilter.messageReceived(session, message);
                        break;
                    case RTMP.STATE_ERROR:
                    case RTMP.STATE_DISCONNECTING:
                    case RTMP.STATE_DISCONNECTED:
                        // do nothing, really
                        log.debug("Nothing to do, connection state: {}", RTMP.states[connectionState]);
                        break;
                    default:
                        throw new IllegalStateException("Invalid RTMP state: " + connectionState);
                }
            }
        }
    }

}
