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

package org.red5.server.net.servlet;

import java.io.IOException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.mina.core.buffer.IoBuffer;
import org.red5.logging.Red5LoggerFactory;
import org.red5.server.api.IContext;
import org.red5.server.api.IServer;
import org.red5.server.api.Red5;
import org.red5.server.api.remoting.IRemotingConnection;
import org.red5.server.api.scope.IGlobalScope;
import org.red5.server.api.scope.IScope;
import org.red5.server.api.service.IServiceInvoker;
import org.red5.server.net.remoting.RemotingConnection;
import org.red5.server.net.remoting.codec.RemotingCodecFactory;
import org.red5.server.net.remoting.message.RemotingCall;
import org.red5.server.net.remoting.message.RemotingPacket;
import org.slf4j.Logger;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * Servlet that handles remoting requests.
 * 
 * @author The Red5 Project
 * @author Luke Hubbard (luke@codegent.com)
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class AMFGatewayServlet extends HttpServlet {

    private static final long serialVersionUID = 7174018823796785619L;

    /**
     * Logger
     */
    protected Logger log = Red5LoggerFactory.getLogger(AMFGatewayServlet.class);

    /**
     * AMF MIME type
     */
    public static final String APPLICATION_AMF = "application/x-amf";

    /**
     * Web app context
     */
    protected transient WebApplicationContext webAppCtx;

    /**
     * Red5 server instance
     */
    protected transient IServer server;

    /**
     * Remoting codec factory
     */
    protected transient RemotingCodecFactory codecFactory;

    /**
     * Request attribute holding the Red5 connection object
     */
    private static final String CONNECTION = "red5.remotingConnection";

    /** {@inheritDoc} */
    @Override
    public void init() throws ServletException {
    }

    /** {@inheritDoc} */
    @Override
    public void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        log.debug("Servicing Request");
        if (codecFactory == null) {
            ServletContext ctx = getServletContext();
            log.debug("Context path: {}", ctx.getContextPath());
            //attempt to lookup the webapp context		
            webAppCtx = WebApplicationContextUtils.getRequiredWebApplicationContext(ctx);
            //now try to look it up as an attribute
            if (webAppCtx == null) {
                log.debug("Webapp context was null, trying lookup as attr.");
                webAppCtx = (WebApplicationContext) ctx.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
            }
            //lookup the server and codec factory
            if (webAppCtx != null) {
                server = (IServer) webAppCtx.getBean("red5.server");
                codecFactory = (RemotingCodecFactory) webAppCtx.getBean("remotingCodecFactory");
            } else {
                log.debug("No web context");
            }
        }
        log.debug("Remoting request {} {}", req.getContextPath(), req.getServletPath());
        if (APPLICATION_AMF.equals(req.getContentType())) {
            serviceAMF(req, resp);
        } else {
            resp.getWriter().write("Red5 : Remoting Gateway");
        }
    }

    /**
     * Return the global scope to use for the given request.
     * 
     * @param req
     *            http request
     * @return scope
     */
    protected IGlobalScope getGlobalScope(HttpServletRequest req) {
        String path = req.getContextPath() + req.getServletPath();
        log.debug("getGlobalScope path: {}", path);
        if (path.startsWith("/")) {
            path = path.substring(1);
        } else {
            log.debug("Path length: {} Servlet name length: {}", path.length(), getServletName().length());
            path = path.substring(0, path.length() - getServletName().length() - 1);
        }
        IGlobalScope global = server.lookupGlobal(req.getServerName(), path);
        if (global == null) {
            global = server.lookupGlobal(req.getLocalName(), path);
            if (global == null) {
                global = server.lookupGlobal(req.getLocalAddr(), path);
            }
        }
        return global;
    }

    /**
     * Works out AMF request
     * 
     * @param req
     *            Request
     * @param resp
     *            Response
     * @throws ServletException
     *             Servlet exception
     * @throws IOException
     *             I/O exception
     */
    protected void serviceAMF(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        log.debug("Servicing AMF");
        IRemotingConnection conn = null;
        try {
            RemotingPacket packet = decodeRequest(req);
            if (packet == null) {
                log.error("Packet should not be null");
                return;
            }
            // Provide a valid IConnection in the Red5 object
            final IGlobalScope global = getGlobalScope(req);
            final IContext context = global.getContext();
            final IScope scope = context.resolveScope(global, packet.getScopePath());
            conn = new RemotingConnection(req, scope, packet);
            // Make sure the connection object isn't garbage collected
            req.setAttribute(CONNECTION, conn);
            // set thread local reference
            Red5.setConnectionLocal(conn);
            //fixed so that true is not returned for calls that have failed
            boolean passed = handleRemotingPacket(req, context, scope, packet);
            if (passed) {
                resp.setStatus(HttpServletResponse.SC_OK);
            } else {
                log.warn("At least one invocation failed to execute");
                resp.setStatus(HttpServletResponse.SC_EXPECTATION_FAILED);
            }
            //send our response
            resp.setContentType(APPLICATION_AMF);
            sendResponse(resp, packet);
        } catch (Exception e) {
            log.error("Error handling remoting call", e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } finally {
            //ensure the conn attr gets removed
            req.removeAttribute(CONNECTION);
            //unregister the remote connection client
            if (conn != null) {
                ((RemotingConnection) conn).cleanup();
            }
            // clear thread local reference
            Red5.setConnectionLocal(null);
        }
    }

    /**
     * Decode request
     * 
     * @param req
     *            Request
     * @return Remoting packet
     * @throws Exception
     *             General exception
     */
    protected RemotingPacket decodeRequest(HttpServletRequest req) throws Exception {
        log.debug("Decoding request");
        IoBuffer reqBuffer = IoBuffer.allocate(req.getContentLength());
        ServletUtils.copy(req, reqBuffer.asOutputStream());
        reqBuffer.flip();
        RemotingPacket packet = (RemotingPacket) codecFactory.getRemotingDecoder().decode(reqBuffer);
        String path = req.getContextPath();
        if (path == null) {
            path = "";
        }
        if (req.getPathInfo() != null) {
            path += req.getPathInfo();
        }
        // check for header path, this is used by the AMF tunnel servlet
        String headerPath = req.getHeader("Tunnel-request");
        // it is only used if the path is set to root
        if (headerPath != null && path.length() < 1) {
            path = headerPath;
        }
        if (path.length() > 0 && path.charAt(0) == '/') {
            path = path.substring(1);
        }
        log.debug("Path: {} Scope path: {}", path, packet.getScopePath());
        packet.setScopePath(path);
        reqBuffer.free();
        reqBuffer = null;
        return packet;
    }

    /**
     * Handles AMF request by making calls
     * 
     * @param req
     *            Request
     * @param context
     *            context
     * @param scope
     *            scope
     * @param message
     *            Remoting packet
     * @return <pre>
     * true
     * </pre>
     * 
     *         on success
     */
    protected boolean handleRemotingPacket(HttpServletRequest req, IContext context, IScope scope, RemotingPacket message) {
        log.debug("Handling remoting packet");
        boolean result = true;
        final IServiceInvoker invoker = context.getServiceInvoker();
        for (RemotingCall call : message.getCalls()) {
            result = invoker.invoke(call, scope);
            //if we encounter a failure break out
            if (!result) {
                break;
            }
        }
        return result;
    }

    /**
     * Sends response to client
     * 
     * @param resp
     *            Response
     * @param packet
     *            Remoting packet
     * @throws Exception
     *             General exception
     */
    protected void sendResponse(HttpServletResponse resp, RemotingPacket packet) throws Exception {
        log.debug("Sending response");
        IoBuffer respBuffer = codecFactory.getRemotingEncoder().encode(packet);
        if (respBuffer != null) {
            final ServletOutputStream out = resp.getOutputStream();
            resp.setContentLength(respBuffer.limit());
            ServletUtils.copy(respBuffer.asInputStream(), out);
            out.flush();
            out.close();
            respBuffer.free();
            respBuffer = null;
        } else {
            log.info("Response buffer was null after encoding");
        }
    }

}
