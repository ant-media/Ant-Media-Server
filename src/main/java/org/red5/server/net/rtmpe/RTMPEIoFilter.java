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

package org.red5.server.net.rtmpe;

import javax.crypto.Cipher;

import org.apache.commons.codec.binary.Hex;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.filterchain.IoFilterAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;
import org.apache.mina.core.write.WriteRequestWrapper;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.red5.server.net.rtmp.InboundHandshake;
import org.red5.server.net.rtmp.RTMPConnManager;
import org.red5.server.net.rtmp.RTMPConnection;
import org.red5.server.net.rtmp.RTMPMinaConnection;
import org.red5.server.net.rtmp.codec.RTMP;
import org.red5.server.net.rtmp.codec.RTMPMinaCodecFactory;
import org.red5.server.net.rtmp.message.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RTMPE IO filter - Server version.
 * 
 * @author Peter Thomas (ptrthomas@gmail.com)
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class RTMPEIoFilter extends IoFilterAdapter {

    private static final Logger log = LoggerFactory.getLogger(RTMPEIoFilter.class);

    @Override
    public void messageReceived(NextFilter nextFilter, IoSession session, Object obj) throws Exception {
        log.trace("messageReceived nextFilter: {} session: {} message: {}", nextFilter, session, obj);
        RTMP rtmp = null;
        String sessionId = (String) session.getAttribute(RTMPConnection.RTMP_SESSION_ID);
        if (sessionId != null) {
            log.trace("Session id: {}", sessionId);
            RTMPMinaConnection conn = (RTMPMinaConnection) RTMPConnManager.getInstance().getConnectionBySessionId(sessionId);
            // filter based on current connection state
            rtmp = conn.getState();
            final byte connectionState = conn.getStateCode();
            // assume message is an IoBuffer
            IoBuffer message = (IoBuffer) obj;
            // client handshake handling
            InboundHandshake handshake = null;
            int remaining = 0;
            switch (connectionState) {
                case RTMP.STATE_CONNECTED:
                    Cipher cipher = (Cipher) session.getAttribute(RTMPConnection.RTMPE_CIPHER_IN);
                    if (cipher != null) {
                        if (log.isDebugEnabled()) {
                            log.debug("Decrypting message: {}", message);
                        }
                        byte[] encrypted = new byte[message.remaining()];
                        message.get(encrypted);
                        message.clear();
                        message.free();
                        byte[] plain = cipher.update(encrypted);
                        IoBuffer messageDecrypted = IoBuffer.wrap(plain);
                        if (log.isDebugEnabled()) {
                            log.debug("Decrypted buffer: {}", messageDecrypted);
                        }
                        nextFilter.messageReceived(session, messageDecrypted);
                    } else {
                        log.trace("Not decrypting message: {}", obj);
                        nextFilter.messageReceived(session, obj);
                    }
                    break;
                case RTMP.STATE_CONNECT:
                    // we're expecting C0+C1 here
                    //log.trace("C0C1 byte order: {}", message.order());
                    // get the handshake from the session
                    handshake = (InboundHandshake) session.getAttribute(RTMPConnection.RTMP_HANDSHAKE);
                    // set handshake to match client requested type
                    message.mark();
                    handshake.setHandshakeType(message.get());
                    message.reset();
                    // set encryption flag the rtmp state
                    rtmp.setEncrypted(handshake.useEncryption());
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
                        conn.getState().setState(RTMP.STATE_HANDSHAKE);
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
                            if (handshake.useEncryption()) {
                                // set encryption flag the rtmp state
                                rtmp.setEncrypted(true);
                                log.debug("Using encrypted communications, adding ciphers to the session");
                                session.setAttribute(RTMPConnection.RTMPE_CIPHER_IN, handshake.getCipherIn());
                                session.setAttribute(RTMPConnection.RTMPE_CIPHER_OUT, handshake.getCipherOut());
                            }
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
                    if (session.containsAttribute("buffer")) {
                        remainder = (byte[]) session.getAttribute("buffer");
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
                            session.removeAttribute("buffer");
                        } else {
                            // copy
                            message.get(dst);
                            log.trace("Copied {}", Hex.encodeHexString(dst));
                        }
                        log.trace("Message - pos: {} {}", message.position(), message);
                        int index = message.indexOf(handshake.getHandshakeType());
                        if (index != -1) {
                            log.trace("Connection type index in message: {}", index);
                            message.position(index);
                        }
                        log.trace("Message - pos: {} {}", message.position(), message);
                        //if (log.isTraceEnabled()) {
                        //    log.trace("C2 - buffer: {}", Hex.encodeHexString(dst));
                        //}
                        if (handshake.decodeClientRequest2(IoBuffer.wrap(dst))) {
                            log.debug("Connected, removing handshake data and adding rtmp protocol filter");
                            // set state to indicate we're connected
                            conn.getState().setState(RTMP.STATE_CONNECTED);
                            // remove handshake from session now that we are connected
                            session.removeAttribute(RTMPConnection.RTMP_HANDSHAKE);
                            // add protocol filter as the last one in the chain
                            log.debug("Adding RTMP protocol filter");
                            session.getFilterChain().addAfter("rtmpeFilter", "protocolFilter", new ProtocolCodecFilter(new RTMPMinaCodecFactory()));
                            // pass the now empty message to the next filter
                            nextFilter.messageReceived(session, message);
                        } else {
                            log.warn("Client was rejected due to invalid handshake");
                            conn.close();
                        }
                    }
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

    @Override
    public void filterWrite(NextFilter nextFilter, IoSession session, WriteRequest request) throws Exception {
        RTMPMinaConnection conn = (RTMPMinaConnection) RTMPConnManager.getInstance().getConnectionBySessionId((String) session.getAttribute(RTMPConnection.RTMP_SESSION_ID));
        // filter based on current connection state
        if (conn.getState().getState() == RTMP.STATE_CONNECTED && session.containsAttribute(RTMPConnection.RTMPE_CIPHER_OUT)) {
            Cipher cipher = (Cipher) session.getAttribute(RTMPConnection.RTMPE_CIPHER_OUT);
            IoBuffer message = (IoBuffer) request.getMessage();
            if (!message.hasRemaining()) {
                if (log.isTraceEnabled()) {
                    log.trace("Ignoring empty message");
                }
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Encrypting message: {}", message);
                }
                byte[] plain = new byte[message.remaining()];
                message.get(plain);
                message.clear();
                message.free();
                //encrypt and write
                byte[] encrypted = cipher.update(plain);
                IoBuffer messageEncrypted = IoBuffer.wrap(encrypted);
                if (log.isDebugEnabled()) {
                    log.debug("Encrypted message: {}", messageEncrypted);
                }
                nextFilter.filterWrite(session, new EncryptedWriteRequest(request, messageEncrypted));
            }
        } else {
            log.trace("Non-encrypted message");
            nextFilter.filterWrite(session, request);
        }
    }

    private static class EncryptedWriteRequest extends WriteRequestWrapper {
        private final IoBuffer encryptedMessage;

        private EncryptedWriteRequest(WriteRequest writeRequest, IoBuffer encryptedMessage) {
            super(writeRequest);
            this.encryptedMessage = encryptedMessage;
        }

        @Override
        public Object getMessage() {
            return encryptedMessage;
        }
    }

}
