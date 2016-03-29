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

package org.red5.server.net.rtmpt;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Hex;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.red5.logging.Red5LoggerFactory;
import org.red5.server.api.Red5;
import org.red5.server.net.IConnectionManager;
import org.red5.server.net.rtmp.InboundHandshake;
import org.red5.server.net.rtmp.RTMPConnManager;
import org.red5.server.net.rtmp.RTMPConnection;
import org.red5.server.net.rtmp.codec.RTMP;
import org.red5.server.net.rtmp.codec.RTMPProtocolEncoder;
import org.red5.server.net.rtmp.event.Invoke;
import org.red5.server.net.rtmp.message.Constants;
import org.red5.server.net.rtmp.message.Header;
import org.red5.server.net.rtmp.message.Packet;
import org.red5.server.net.rtmp.status.Status;
import org.red5.server.net.rtmp.status.StatusCodes;
import org.red5.server.net.servlet.ServletUtils;
import org.red5.server.service.PendingCall;
import org.slf4j.Logger;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * Servlet that handles all RTMPT requests.
 * 
 * @author The Red5 Project
 * @author Joachim Bauch (jojo@struktur.de)
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class RTMPTServlet extends HttpServlet {

    private static final long serialVersionUID = 5925399677454936613L;

    protected static Logger log = Red5LoggerFactory.getLogger(RTMPTServlet.class);

    /**
     * HTTP request method to use for RTMPT calls.
     */
    private static final String REQUEST_METHOD = "POST";

    /**
     * Content-Type to use for RTMPT requests / responses.
     */
    private static final String CONTENT_TYPE = "application/x-fcs";

    /**
     * Connection manager.
     */
    private static IConnectionManager<RTMPConnection> manager;

    /**
     * Try to generate responses that contain at least 32768 bytes data. Increasing this value results in better stream performance, but
     * also increases the latency.
     */
    private static int targetResponseSize = Short.MAX_VALUE + 1;

    /**
     * Reference to RTMPT handler;
     */
    private static RTMPTHandler handler;

    /**
     * Response sent for ident2 requests. If this is null a 404 will be returned
     */
    private static String ident2;

    // Whether or not to enforce content type checking for requests
    private boolean enforceContentTypeCheck;

    /**
     * Thread local for request info storage
     */
    protected ThreadLocal<RequestInfo> requestInfo = new ThreadLocal<RequestInfo>();

    /**
     * Web app context
     */
    protected transient WebApplicationContext applicationContext;

    /**
     * Return an error message to the client.
     * 
     * @param message
     *            Message
     * @param resp
     *            Servlet response
     * @throws IOException
     *             on IO error
     */
    protected void handleBadRequest(String message, HttpServletResponse resp) throws IOException {
        log.debug("handleBadRequest {}", message);
        //		resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        //		resp.setHeader("Connection", "Keep-Alive");
        //		resp.setHeader("Cache-Control", "no-cache");
        //		resp.setContentType("text/plain");
        //		resp.setContentLength(message.length());
        //		resp.getWriter().write(message);
        //		resp.flushBuffer();

        // create and send a rejected status
        Status status = new Status(StatusCodes.NC_CONNECT_REJECTED, Status.ERROR, message);
        PendingCall call = new PendingCall(null, "onStatus", new Object[] { status });
        Invoke event = new Invoke();
        event.setCall(call);
        Header header = new Header();
        Packet packet = new Packet(header, event);
        header.setDataType(event.getDataType());
        // create dummy connection if local is empty
        RTMPConnection conn = (RTMPConnection) Red5.getConnectionLocal();
        if (conn == null) {
            try {
                conn = ((RTMPConnManager) manager).createConnectionInstance(RTMPTConnection.class);
                Red5.setConnectionLocal(conn);
            } catch (Exception e) {
            }
        }
        // encode the data
        RTMPProtocolEncoder encoder = new RTMPProtocolEncoder();
        IoBuffer out = encoder.encodePacket(packet);
        // send the response
        returnMessage(null, out, resp);
        // clear local
        Red5.setConnectionLocal(null);
    }

    /**
     * Return a single byte to the client.
     * 
     * @param message
     *            Message
     * @param resp
     *            Servlet response
     * @throws IOException
     *             I/O exception
     */
    protected void returnMessage(byte message, HttpServletResponse resp) throws IOException {
        log.debug("returnMessage {}", message);
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setHeader("Connection", "Keep-Alive");
        resp.setHeader("Cache-Control", "no-cache");
        resp.setContentType(CONTENT_TYPE);
        resp.setContentLength(1);
        resp.getWriter().write(message);
        resp.flushBuffer();
    }

    /**
     * Return a message to the client.
     * 
     * @param message
     *            Message
     * @param resp
     *            Servlet response
     * @throws IOException
     *             I/O exception
     */
    protected void returnMessage(String message, HttpServletResponse resp) throws IOException {
        log.debug("returnMessage {}", message);
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setHeader("Connection", "Keep-Alive");
        resp.setHeader("Cache-Control", "no-cache");
        resp.setContentType(CONTENT_TYPE);
        resp.setContentLength(message.length());
        resp.getWriter().write(message);
        resp.flushBuffer();
    }

    /**
     * Return raw data to the client.
     * 
     * @param conn
     *            RTMP connection
     * @param buffer
     *            Raw data as byte buffer
     * @param resp
     *            Servlet response
     * @throws IOException
     *             I/O exception
     */
    protected void returnMessage(RTMPTConnection conn, IoBuffer buffer, HttpServletResponse resp) throws IOException {
        log.trace("returnMessage {}", buffer);
        if (conn != null) {
            resp.setStatus(HttpServletResponse.SC_OK);
        } else {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }
        resp.setHeader("Connection", "Keep-Alive");
        resp.setHeader("Cache-Control", "no-cache");
        resp.setContentType(CONTENT_TYPE);
        int contentLength = buffer.limit() + 1;
        resp.setContentLength(contentLength);
        ServletOutputStream output = resp.getOutputStream();
        if (conn != null) {
            byte pollingDelay = conn.getPollingDelay();
            log.debug("Sending {} bytes; polling delay: {}", buffer.limit(), pollingDelay);
            output.write(pollingDelay);
        } else {
            output.write((byte) 0);
        }
        ServletUtils.copy(buffer.asInputStream(), output);
        if (conn != null) {
            conn.updateWrittenBytes(contentLength);
        }
        buffer.free();
        buffer = null;
    }

    /**
     * Sets the request info for the current request. Request info contains the session id and request number gathered from the incoming
     * request. The URI is in this form /[method]/[session id]/[request number] ie. /send/CAFEBEEF01/7
     *
     * @param req
     *            Servlet request
     */
    protected void setRequestInfo(HttpServletRequest req) {
        String[] arr = req.getRequestURI().trim().split("/");
        log.trace("Request parts: {}", Arrays.toString(arr));
        RequestInfo info = new RequestInfo(arr[2], Integer.valueOf(arr[3]));
        requestInfo.set(info);
    }

    /**
     * Skip data sent by the client.
     * 
     * @param req
     *            Servlet request
     * @throws IOException
     *             I/O exception
     */
    protected void skipData(HttpServletRequest req) throws IOException {
        log.trace("skipData {}", req);
        int length = req.getContentLength();
        log.trace("Skipping {} bytes", length);
        IoBuffer data = IoBuffer.allocate(length);
        ServletUtils.copy(req, data.asOutputStream());
        data.flip();
        data.free();
        data = null;
        log.trace("Skipped {} bytes", length);
    }

    /**
     * Send pending messages to client.
     * 
     * @param conn
     *            RTMP connection
     * @param resp
     *            Servlet response
     */
    protected void returnPendingMessages(RTMPTConnection conn, HttpServletResponse resp) {
        log.debug("returnPendingMessages {}", conn);
        // grab any pending outgoing data
        IoBuffer data = conn.getPendingMessages(targetResponseSize);
        if (data != null) {
            try {
                returnMessage(conn, data, resp);
            } catch (Exception ex) {
                // using "Exception" is meant to catch any exception that would occur when doing a write
                // this can be an IOException or a container specific one like ClientAbortException from catalina
                log.warn("Exception returning outgoing data", ex);
                conn.close();
            }
        } else {
            log.debug("No messages to send");
            if (conn.isClosing()) {
                log.debug("Client is closing, send close notification");
                try {
                    // tell client to close connection
                    returnMessage((byte) 0, resp);
                } catch (IOException ex) {
                    log.warn("Exception returning outgoing data - close notification", ex);
                }
            } else {
                try {
                    returnMessage(conn.getPollingDelay(), resp);
                } catch (IOException ex) {
                    log.warn("Exception returning outgoing data - polling delay", ex);
                }
            }
        }
    }

    /**
     * Start a new RTMPT session.
     * 
     * @param req
     *            Servlet request
     * @param resp
     *            Servlet response
     * @throws IOException
     *             I/O exception
     */
    protected void handleOpen(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        log.debug("handleOpen");
        // skip sent data
        skipData(req);
        // TODO: should we evaluate the pathinfo?
        RTMPTConnection conn = (RTMPTConnection) manager.createConnection(RTMPTConnection.class);
        log.trace("{}", conn);
        if (conn != null) {
            // set properties
            conn.setServlet(this);
            conn.setServletRequest(req);
            // add the connection to the manager
            manager.setConnection(conn);
            // set handler 
            conn.setHandler(handler);
            conn.setDecoder(handler.getCodecFactory().getRTMPDecoder());
            conn.setEncoder(handler.getCodecFactory().getRTMPEncoder());
            handler.connectionOpened(conn);
            conn.dataReceived();
            conn.updateReadBytes(req.getContentLength());
            // set thread local reference
            Red5.setConnectionLocal(conn);
            if (conn.getId() != 0) {
                // return session id to client
                returnMessage(String.format("%s\n", conn.getSessionId()), resp);
            } else {
                // no more clients are available for serving
                returnMessage((byte) 0, resp);
            }
        } else {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.setHeader("Connection", "Keep-Alive");
            resp.setHeader("Cache-Control", "no-cache");
            resp.flushBuffer();
        }
    }

    /**
     * Close a RTMPT session.
     * 
     * @param req
     *            Servlet request
     * @param resp
     *            Servlet response
     * @throws IOException
     *             I/O exception
     */
    protected void handleClose(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        log.debug("handleClose");
        // skip sent data
        skipData(req);
        // get the associated connection
        RTMPTConnection connection = getConnection();
        if (connection != null) {
            log.debug("Pending messges on close: {}", connection.getPendingMessages());
            returnMessage((byte) 0, resp);
            connection.close();
        } else {
            handleBadRequest(String.format("Close: unknown client session: %s", requestInfo.get().getSessionId()), resp);
        }
    }

    /**
     * Add data for an established session.
     * 
     * @param req
     *            Servlet request
     * @param resp
     *            Servlet response
     * @throws IOException
     *             I/O exception
     */
    protected void handleSend(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        log.debug("handleSend");
        final RTMPTConnection conn = getConnection();
        if (conn != null) {
            IoSession session = conn.getIoSession();
            // get the handshake from the session
            InboundHandshake handshake = null;
            // put the received data in a ByteBuffer
            int length = req.getContentLength();
            log.trace("Request content length: {}", length);
            final IoBuffer message = IoBuffer.allocate(length);
            ServletUtils.copy(req, message.asOutputStream());
            message.flip();
            RTMP rtmp = conn.getState();
            int connectionState = rtmp.getState();
            switch (connectionState) {
                case RTMP.STATE_CONNECT:
                    // we're expecting C0+C1 here
                    //log.trace("C0C1 byte order: {}", message.order());
                    log.debug("decodeHandshakeC0C1 - buffer: {}", message);
                    // we want 1537 bytes for C0C1
                    if (message.remaining() >= (Constants.HANDSHAKE_SIZE + 1)) {
                        // get the connection type byte, may want to set this on the conn in the future
                        byte connectionType = message.get();
                        log.trace("Incoming C0 connection type: {}", connectionType);
                        // add the in-bound handshake, defaults to non-encrypted mode
                        handshake = new InboundHandshake(connectionType);
                        handshake.setUnvalidatedConnectionAllowed(handler.isUnvalidatedConnectionAllowed());
                        session.setAttribute(RTMPConnection.RTMP_HANDSHAKE, handshake);
                        // create array for decode
                        byte[] dst = new byte[Constants.HANDSHAKE_SIZE];
                        // copy out 1536 bytes
                        message.get(dst);
                        //log.debug("C1 - buffer: {}", Hex.encodeHexString(dst));
                        // set state to indicate we're waiting for C2
                        rtmp.setState(RTMP.STATE_HANDSHAKE);
                        IoBuffer s1 = handshake.decodeClientRequest1(IoBuffer.wrap(dst));
                        if (s1 != null) {
                            //log.trace("S1 byte order: {}", s1.order());
                            conn.writeRaw(s1);
                        } else {
                            log.warn("Client was rejected due to invalid handshake");
                            conn.close();
                        }
                    }
                    break;
                case RTMP.STATE_HANDSHAKE:
                    // we're expecting C2 here
                    //log.trace("C2 byte order: {}", message.order());
                    log.debug("decodeHandshakeC2 - buffer: {}", message);
                    // no connection type byte is supposed to be in C2 data
                    if (message.remaining() >= Constants.HANDSHAKE_SIZE) {
                        // get the handshake
                        handshake = (InboundHandshake) session.getAttribute(RTMPConnection.RTMP_HANDSHAKE);
                        // create array for decode
                        byte[] dst = new byte[Constants.HANDSHAKE_SIZE];
                        // copy
                        message.get(dst);
                        log.trace("Copied {}", Hex.encodeHexString(dst));
                        //if (log.isTraceEnabled()) {
                        //    log.trace("C2 - buffer: {}", Hex.encodeHexString(dst));
                        //}
                        if (handshake.decodeClientRequest2(IoBuffer.wrap(dst))) {
                            log.debug("Connected, removing handshake data and adding rtmp protocol filter");
                            // set state to indicate we're connected
                            rtmp.setState(RTMP.STATE_CONNECTED);
                            // remove handshake from session now that we are connected
                            session.removeAttribute(RTMPConnection.RTMP_HANDSHAKE);
                        } else {
                            log.warn("Client was rejected due to invalid handshake");
                            conn.close();
                        }
                    }
                    // let the logic flow into connected to catch the remaining bytes that probably contain
                    // the connect call
                case RTMP.STATE_CONNECTED:
                    // decode the objects and pass to received; messages should all be Packet type
                    for (Object obj : conn.decode(message)) {
                        conn.handleMessageReceived(obj);
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
            conn.dataReceived();
            conn.updateReadBytes(length);
            message.clear();
            message.free();
            // return pending messages
            returnPendingMessages(conn, resp);
        } else {
            handleBadRequest(String.format("Send: unknown client session: %s", requestInfo.get().getSessionId()), resp);
        }
    }

    /**
     * Poll RTMPT session for updates.
     * 
     * @param req
     *            Servlet request
     * @param resp
     *            Servlet response
     * @throws IOException
     *             I/O exception
     */
    protected void handleIdle(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        log.debug("handleIdle");
        // skip sent data
        skipData(req);
        // get associated connection
        RTMPTConnection conn = getConnection();
        if (conn != null) {
            conn.dataReceived();
            conn.updateReadBytes(req.getContentLength());
            // return pending
            returnPendingMessages(conn, resp);
        } else {
            handleBadRequest(String.format("Idle: unknown client session: %s", requestInfo.get().getSessionId()), resp);
        }
    }

    /**
     * Main entry point for the servlet.
     * 
     * @param req
     *            Request object
     * @param resp
     *            Response object
     * @throws IOException
     *             I/O exception
     */
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (applicationContext == null) {
            ServletContext ctx = getServletContext();
            applicationContext = WebApplicationContextUtils.getWebApplicationContext(ctx);
            if (applicationContext == null) {
                applicationContext = (WebApplicationContext) ctx.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
            }
            log.debug("Application context: {}", applicationContext);
            // ensure we have a connection manager
            if (manager == null) {
                log.warn("Class instance connection manager was null, looking up in application context");
                manager = (RTMPConnManager) applicationContext.getBean("rtmpConnManager");
                if (manager == null) {
                    log.warn("Connection manager was null in context, getting class instance");
                    manager = RTMPConnManager.getInstance();
                    if (manager == null) {
                        log.error("Connection manager is still null, this is bad");
                    }
                }
            }
        }
        log.debug("Request - method: {} content type: {} path: {}", new Object[] { req.getMethod(), req.getContentType(), req.getServletPath() });
        // allow only POST requests with valid content length
        if (!REQUEST_METHOD.equals(req.getMethod()) || req.getContentLength() == 0) {
            // Bad request - return simple error page
            handleBadRequest("Bad request, only RTMPT supported.", resp);
            return;
        }
        // decide whether or not to enforce request content checks
        if (enforceContentTypeCheck && !CONTENT_TYPE.equals(req.getContentType())) {
            handleBadRequest(String.format("Bad request, unsupported content type: %s.", req.getContentType()), resp);
            return;
        }
        // get the uri
        String uri = req.getRequestURI().trim();
        log.debug("URI: {}", uri);
        // get the path
        String path = req.getServletPath();
        // since the only current difference in the type of request that we are interested in is the 'second' character, we can double
        // the speed of this entry point by using a switch on the second character.
        char p = path.charAt(1);
        switch (p) {
            case 'o': // OPEN_REQUEST
                handleOpen(req, resp);
                break;
            case 'c': // CLOSE_REQUEST
                setRequestInfo(req);
                handleClose(req, resp);
                requestInfo.remove();
                break;
            case 's': // SEND_REQUEST
                setRequestInfo(req);
                handleSend(req, resp);
                requestInfo.remove();
                break;
            case 'i': // IDLE_REQUEST
                setRequestInfo(req);
                handleIdle(req, resp);
                requestInfo.remove();
                break;
            case 'f': // HTTPIdent request (ident and ident2)
                //if HTTPIdent is requested send back some Red5 info
                //http://livedocs.adobe.com/flashmediaserver/3.0/docs/help.html?content=08_xmlref_011.html			
                String ident = "<fcs><Company>Red5</Company><Team>Red5 Server</Team></fcs>";
                // handle ident2 slightly different to appease osx clients
                if (uri.charAt(uri.length() - 1) == '2') {
                    // check for pre-configured ident2 value
                    if (ident2 != null) {
                        ident = ident2;
                    } else {
                        // just send 404 back if no ident2 value is set
                        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                        resp.setHeader("Connection", "Keep-Alive");
                        resp.setHeader("Cache-Control", "no-cache");
                        resp.flushBuffer();
                        break;
                    }
                }
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.setHeader("Connection", "Keep-Alive");
                resp.setHeader("Cache-Control", "no-cache");
                resp.setContentType(CONTENT_TYPE);
                resp.setContentLength(ident.length());
                resp.getWriter().write(ident);
                resp.flushBuffer();
                break;
            default:
                handleBadRequest(String.format("RTMPT command %s is not supported.", path), resp);
        }
        // clear thread local reference
        Red5.setConnectionLocal(null);
    }

    /** {@inheritDoc} */
    @Override
    public void destroy() {
        // Cleanup connections
        Collection<RTMPConnection> conns = manager.getAllConnections();
        for (RTMPConnection conn : conns) {
            if (conn instanceof RTMPTConnection) {
                log.debug("Connection scope on destroy: {}", conn.getScope());
                conn.close();
            }
        }
        super.destroy();
    }

    /**
     * Returns a connection based on the current client session id.
     * 
     * @return RTMPTConnection
     */
    protected RTMPTConnection getConnection() {
        String sessionId = requestInfo.get().getSessionId();
        RTMPTConnection conn = (RTMPTConnection) manager.getConnectionBySessionId(sessionId);
        if (conn != null) {
            // check for non-connected state
            if (!conn.isDisconnected()) {
                // clear thread local reference
                Red5.setConnectionLocal(conn);
            } else {
                removeConnection(sessionId);
            }
        } else {
            log.warn("Null connection for session id: {}", sessionId);
        }
        return conn;
    }

    /**
     * Removes a connection matching the given session id from the connection manager.
     * 
     * @param sessionId
     *            session id
     */
    protected void removeConnection(String sessionId) {
        log.debug("Removing connection for session id: {}", sessionId);
        RTMPTConnection conn = (RTMPTConnection) manager.getConnectionBySessionId(sessionId);
        if (conn != null) {
            manager.removeConnection(conn.getSessionId());
        } else {
            log.warn("Remove failed, null connection for session id: {}", sessionId);
        }
    }

    /**
     * @param manager
     *            the manager to set
     */
    public void setManager(IConnectionManager<RTMPConnection> manager) {
        log.trace("Set connection manager: {}", manager);
        RTMPTServlet.manager = manager;
    }

    /**
     * Set the RTMPTHandler to use in this servlet.
     * 
     * @param handler
     *            handler
     */
    public void setHandler(RTMPTHandler handler) {
        log.trace("Set handler: {}", handler);
        RTMPTServlet.handler = handler;
    }

    /**
     * Set the fcs/ident2 string
     * 
     * @param ident2
     *            ident2 string
     */
    public void setIdent2(String ident2) {
        RTMPTServlet.ident2 = ident2;
    }

    /**
     * Sets the target size for responses
     * 
     * @param targetResponseSize
     *            the targetResponseSize to set
     */
    public void setTargetResponseSize(int targetResponseSize) {
        RTMPTServlet.targetResponseSize = targetResponseSize;
    }

    /**
     * @return the enforceContentTypeCheck
     */
    public boolean isEnforceContentTypeCheck() {
        return enforceContentTypeCheck;
    }

    /**
     * @param enforceContentTypeCheck
     *            the enforceContentTypeCheck to set
     */
    public void setEnforceContentTypeCheck(boolean enforceContentTypeCheck) {
        this.enforceContentTypeCheck = enforceContentTypeCheck;
    }

    /**
     * Used to store request information per thread.
     */
    protected final class RequestInfo {

        private String sessionId;

        private Integer requestNumber;

        RequestInfo(String sessionId, Integer requestNumber) {
            this.sessionId = sessionId;
            this.requestNumber = requestNumber;
        }

        /**
         * @return the sessionId
         */
        public String getSessionId() {
            return sessionId;
        }

        /**
         * @return the requestNumber
         */
        public Integer getRequestNumber() {
            return requestNumber;
        }

    }

}
