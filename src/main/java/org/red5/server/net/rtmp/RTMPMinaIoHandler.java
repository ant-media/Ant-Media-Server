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

package org.red5.server.net.rtmp;

import java.io.IOException;
import java.lang.ref.WeakReference;

import org.apache.commons.codec.binary.Hex;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.future.CloseFuture;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.service.IoProcessor;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequestQueue;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.red5.server.net.IConnectionManager;
import org.red5.server.net.rtmp.codec.RTMP;
import org.red5.server.net.rtmp.message.Packet;
import org.red5.server.net.rtmpe.RTMPEIoFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles all RTMP protocol events fired by the MINA framework.
 */
public class RTMPMinaIoHandler extends IoHandlerAdapter {

    private static Logger log = LoggerFactory.getLogger(RTMPMinaIoHandler.class);

    /**
     * RTMP events handler
     */
    protected IRTMPHandler handler;

    /** {@inheritDoc} */
    @Override
    public void sessionCreated(IoSession session) throws Exception {
        log.debug("Session created");
        // add rtmpe filter, rtmp protocol filter is added upon successful handshake
        session.getFilterChain().addFirst("rtmpeFilter", new RTMPEIoFilter());
        // create a connection
        RTMPMinaConnection conn = createRTMPMinaConnection();
        // add session to the connection
        conn.setIoSession(session);
        // add the handler
        conn.setHandler(handler);
        // add the connections session id for look up using the connection manager
        session.setAttribute(RTMPConnection.RTMP_SESSION_ID, conn.getSessionId());
        // create an inbound handshake
        InboundHandshake handshake = new InboundHandshake();
        // set whether or not unverified will be allowed
        handshake.setUnvalidatedConnectionAllowed(((RTMPHandler) handler).isUnvalidatedConnectionAllowed()); 
        // add the in-bound handshake, defaults to non-encrypted mode
        session.setAttribute(RTMPConnection.RTMP_HANDSHAKE, handshake);
    }

    /** {@inheritDoc} */
    @Override
    public void sessionOpened(IoSession session) throws Exception {
        String sessionId = (String) session.getAttribute(RTMPConnection.RTMP_SESSION_ID);
        log.debug("Session opened: {} id: {}", session.getId(), sessionId);
        RTMPConnManager connManager = (RTMPConnManager) RTMPConnManager.getInstance();
        session.setAttribute(RTMPConnection.RTMP_CONN_MANAGER, new WeakReference<IConnectionManager<RTMPConnection>>(connManager));
        RTMPMinaConnection conn = (RTMPMinaConnection) connManager.getConnectionBySessionId(sessionId);
        handler.connectionOpened(conn);
    }

    /** {@inheritDoc} */
    @Override
    public void sessionClosed(IoSession session) throws Exception {
        String sessionId = (String) session.getAttribute(RTMPConnection.RTMP_SESSION_ID);
        log.debug("Session closed: {} id: {}", session.getId(), sessionId);
        if (log.isTraceEnabled()) {
            log.trace("Session attributes: {}", session.getAttributeKeys());
        }
        if (sessionId != null) {
            RTMPMinaConnection conn = (RTMPMinaConnection) RTMPConnManager.getInstance().getConnectionBySessionId(sessionId);
            if (conn != null) {
                // fire-off closed event
                handler.connectionClosed(conn);
                // clear any session attributes we may have previously set
                // TODO: verify this cleanup code is necessary. The session is over and will be garbage collected surely?
                if (session.containsAttribute(RTMPConnection.RTMP_HANDSHAKE)) {
                    session.removeAttribute(RTMPConnection.RTMP_HANDSHAKE);
                }
                if (session.containsAttribute(RTMPConnection.RTMPE_CIPHER_IN)) {
                    session.removeAttribute(RTMPConnection.RTMPE_CIPHER_IN);
                    session.removeAttribute(RTMPConnection.RTMPE_CIPHER_OUT);
                }
            } else {
                log.warn("Connection was not found for {}", sessionId);
            }
            cleanSession(session, false);
        } else {
            log.debug("Connections session id was null in session, may already be closed");
        }
    }

    /** {@inheritDoc} */
    @Override
    public void messageReceived(IoSession session, Object message) throws Exception {
        log.trace("messageReceived session: {} message: {}", session, message);
        log.trace("Filter chain: {}", session.getFilterChain());
        String sessionId = (String) session.getAttribute(RTMPConnection.RTMP_SESSION_ID);
        log.trace("Message received on session: {} id: {}", session.getId(), sessionId);
        RTMPMinaConnection conn = (RTMPMinaConnection) RTMPConnManager.getInstance().getConnectionBySessionId(sessionId);
        if (conn != null) {
            if (message != null) {
                if (message instanceof Packet) {
                    byte state = conn.getStateCode();
                    // checking the state before allowing a task to be created will hopefully prevent rejected task exceptions
                    if (state != RTMP.STATE_DISCONNECTING && state != RTMP.STATE_DISCONNECTED) {
                        conn.handleMessageReceived((Packet) message);
                    } else {
                        log.info("Ignoring received message on {} due to state: {}", sessionId, RTMP.states[state]);
                    }
                }
            }
        } else {
            log.warn("Connection was not found for {}, force closing", sessionId);
            forceClose(session);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void messageSent(IoSession session, Object message) throws Exception {
        log.trace("messageSent session: {} message: {}", session, message);
        String sessionId = (String) session.getAttribute(RTMPConnection.RTMP_SESSION_ID);
        if (log.isTraceEnabled()) {
            log.trace("Message sent on session: {} id: {}", session.getId(), sessionId);
        }
        if (sessionId != null) {
            RTMPMinaConnection conn = (RTMPMinaConnection) RTMPConnManager.getInstance().getConnectionBySessionId(sessionId);
            if (conn != null) {
                final byte state = conn.getStateCode();
                switch (state) {
                    case RTMP.STATE_CONNECTED:
                        if (message instanceof Packet) {
                            handler.messageSent(conn, (Packet) message);
                        } else if (log.isDebugEnabled()) {
                            log.debug("Message was not of Packet type; its type: {}", message != null ? message.getClass().getName() : "null");
                        }
                        break;
                    case RTMP.STATE_CONNECT:
                    case RTMP.STATE_HANDSHAKE:
                        if (log.isTraceEnabled()) {
                            log.trace("messageSent: {}", Hex.encodeHexString(((IoBuffer) message).array()));
                        }
                        break;
                    case RTMP.STATE_DISCONNECTING:
                    case RTMP.STATE_DISCONNECTED:
                    default:
                }
            } else {
                log.warn("Destination connection was null, it is already disposed. Session id: {}", sessionId);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
        log.debug("Filter chain: {}", session.getFilterChain());
        String sessionId = (String) session.getAttribute(RTMPConnection.RTMP_SESSION_ID);
        if (log.isDebugEnabled()) {
            log.warn("Exception caught on session: {} id: {}", session.getId(), sessionId, cause);
        }
        if (cause instanceof IOException) {
            // Mina states that the connection will be automatically closed when an IOException is caught
            log.debug("IOException caught on {}", sessionId);
        } else {
            log.debug("Non-IOException caught on {}", sessionId);
            forceClose(session);
        }
    }

    /**
     * Force the NioSession to be released and cleaned up.
     * 
     * @param session
     */
    private void forceClose(final IoSession session) {
        log.warn("Force close - session: {}", session.getId());
        if (session.containsAttribute("FORCED_CLOSE")) {
            log.info("Close already forced on this session: {}", session.getId());
        } else {
            // set flag
            session.setAttribute("FORCED_CLOSE", Boolean.TRUE);
            session.suspendRead();
            cleanSession(session, true);
        }
    }

    /**
     * Close and clean-up the IoSession.
     * 
     * @param session
     * @param immediately close without waiting for the write queue to flush
     */
    private void cleanSession(final IoSession session, boolean immediately) {
        // clean up
        final String sessionId = (String) session.getAttribute(RTMPConnection.RTMP_SESSION_ID);
        if (log.isDebugEnabled()) {
            log.debug("Forcing close on session: {} id: {}", session.getId(), sessionId);
            log.debug("Session closing: {}", session.isClosing());
        }
        // get the write request queue
        final WriteRequestQueue writeQueue = session.getWriteRequestQueue();
        if (writeQueue != null && !writeQueue.isEmpty(session)) {
            log.debug("Clearing write queue");
            try {
                writeQueue.clear(session);
            } catch (Exception ex) {
                // clear seems to cause a write to closed session ex in some cases
                log.warn("Exception clearing write queue for {}", sessionId, ex);
            }
        }
        // force close the session
        final CloseFuture future = immediately ? session.closeNow() : session.closeOnFlush();
        IoFutureListener<CloseFuture> listener = new IoFutureListener<CloseFuture>() {
            @SuppressWarnings({ "unchecked", "rawtypes" })
            public void operationComplete(CloseFuture future) {
                // now connection should be closed
                log.debug("Close operation completed {}: {}", sessionId, future.isClosed());
                future.removeListener(this);
                for (Object key : session.getAttributeKeys()) {
                    Object obj = session.getAttribute(key);
                    log.debug("{}: {}", key, obj);
                    if (obj != null) {
                        if (log.isTraceEnabled()) {
                            log.trace("Attribute: {}", obj.getClass().getName());
                        }
                        if (obj instanceof IoProcessor) {
                            log.debug("Flushing session in processor");
                            ((IoProcessor) obj).flush(session);
                            log.debug("Removing session from processor");
                            ((IoProcessor) obj).remove(session);
                        } else if (obj instanceof IoBuffer) {
                            log.debug("Clearing session buffer");
                            ((IoBuffer) obj).clear();
                            ((IoBuffer) obj).free();
                        }
                    }
                }
            }
        };
        future.addListener(listener);
    }

    /**
     * Setter for handler.
     *
     * @param handler RTMP events handler
     */
    public void setHandler(IRTMPHandler handler) {
        this.handler = handler;
    }

    /**
     * @param codecFactory the codecFactory to set
     */
    @Deprecated
    public void setCodecFactory(ProtocolCodecFactory codecFactory) {
        log.warn("This option is deprecated, the codec factory is now contained within the RTMPEIoFilter");
    }

    protected RTMPMinaConnection createRTMPMinaConnection() {
        return (RTMPMinaConnection) RTMPConnManager.getInstance().createConnection(RTMPMinaConnection.class);
    }
}
