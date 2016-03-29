/*
 * RED5 Open Source Flash Server - https://github.com/Red5/
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

import org.apache.commons.codec.binary.Hex;
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
                log.trace("Session id: {}", sessionId);
                RTMPMinaConnection conn = (RTMPMinaConnection) RTMPConnManager.getInstance().getConnectionBySessionId(sessionId);
                // filter based on current connection state
                RTMP rtmp = conn.getState();
                final byte connectionState = conn.getStateCode();
                // assume message is an IoBuffer
                IoBuffer message = (IoBuffer) obj;
                // client handshake handling
                InboundHandshake handshake = null;
                int remaining = 0;
                switch (connectionState) {
                    case RTMP.STATE_CONNECT:
                        // we're expecting C0+C1 here
                        //log.trace("C0C1 byte order: {}", message.order());
                        if (message.indexOf("P".getBytes()[0]) == 0) {
                            // indicates that the FP sent "POST" for a non-native rtmps connection, reject it
                            throw new Exception("Client requested non-native RTMPS connection");
                        }
                        // get the handshake from the session
                        handshake = (InboundHandshake) session.getAttribute(RTMPConnection.RTMP_HANDSHAKE);
                        log.debug("decodeHandshakeC0C1 - buffer: {}", message);
                        // we want 1537 bytes for C0C1
                        remaining = message.remaining();
                        log.trace("Incoming C0C1 size: {}", remaining);
                        if (remaining >= (Constants.HANDSHAKE_SIZE + 1)) {
                            // get the connection type byte, may want to set this on the conn in the future
                            byte connectionType = message.get();
                            log.trace("Incoming C0 connection type: {}", connectionType);
                            // create array for decode
                            byte[] dst = new byte[Constants.HANDSHAKE_SIZE];
                            // copy out 1536 bytes
                            message.get(dst);
                            //log.debug("C1 - buffer: {}", Hex.encodeHexString(dst));
                            // set state to indicate we're waiting for C2
                            rtmp.setState(RTMP.STATE_HANDSHAKE);
                            // buffer any extra bytes
                            remaining = message.remaining();
                            if (log.isTraceEnabled()) {
                                log.trace("Incoming C1 remaining size: {}", remaining);
                            }
                            if (remaining > 0) {
                                // store the remaining bytes in a thread local for use by C2 decoding
                                byte[] remainder = new byte[remaining];
                                message.get(remainder);
                                session.setAttribute("handshake.buffer", remainder);
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
                        // we're expecting C2 here
                        //log.trace("C2 byte order: {}", message.order());
                        // get the handshake from the session
                        handshake = (InboundHandshake) session.getAttribute(RTMPConnection.RTMP_HANDSHAKE);
                        log.debug("decodeHandshakeC2 - buffer: {}", message);
                        remaining = message.remaining();
                        // check for remaining stored bytes left over from C0C1
                        byte[] remainder = null;
                        if (session.containsAttribute("handshake.buffer")) {
                            remainder = (byte[]) session.getAttribute("handshake.buffer");
                            remaining += remainder.length;
                            log.trace("Remainder: {}", Hex.encodeHexString(remainder));
                        }
                        // no connection type byte is supposed to be in C2 data
                        log.trace("Incoming C2 size: {}", remaining);
                        if (remaining >= Constants.HANDSHAKE_SIZE) {
                            // create array for decode
                            byte[] dst = new byte[Constants.HANDSHAKE_SIZE];
                            // check for remaining stored bytes left over from C0C1 and prepend to the dst array
                            if (remainder != null) {
                                // copy into dst
                                System.arraycopy(remainder, 0, dst, 0, remainder.length);
                                log.trace("Copied {} from buffer {}", remainder.length, Hex.encodeHexString(dst));
                                // copy
                                message.get(dst, remainder.length, (Constants.HANDSHAKE_SIZE - remainder.length));
                                log.trace("Copied {} from message {}", (Constants.HANDSHAKE_SIZE - remainder.length), Hex.encodeHexString(dst));
                                // remove buffer
                                session.removeAttribute("handshake.buffer");
                            } else {
                                // copy
                                message.get(dst);
                                log.trace("Copied {}", Hex.encodeHexString(dst));
                            }
                            //if (log.isTraceEnabled()) {
                            //    log.trace("C2 - buffer: {}", Hex.encodeHexString(dst));
                            //}
                            if (handshake.decodeClientRequest2(IoBuffer.wrap(dst))) {
                                log.debug("Connected, removing handshake data and adding rtmp protocol filter");
                                // set state to indicate we're connected
                                rtmp.setState(RTMP.STATE_CONNECTED);
                                // remove handshake from session now that we are connected
                                session.removeAttribute(RTMPConnection.RTMP_HANDSHAKE);
                                // add protocol filter as the last one in the chain
                                log.debug("Adding RTMP protocol filter");
                                session.getFilterChain().addAfter("rtmpsFilter", "protocolFilter", new ProtocolCodecFilter(new RTMPMinaCodecFactory()));
                            } else {
                                log.warn("Client was rejected due to invalid handshake");
                                conn.close();
                            }
                        }
                        // no break here to all the message to flow into connected case
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
