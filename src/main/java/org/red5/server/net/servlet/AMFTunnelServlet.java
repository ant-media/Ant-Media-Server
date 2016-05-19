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

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.util.EntityUtils;
import org.apache.mina.core.buffer.IoBuffer;
import org.red5.logging.Red5LoggerFactory;
import org.red5.server.util.HttpConnectionUtil;
import org.slf4j.Logger;

/**
 * Servlet to tunnel to the AMF gateway servlet.
 * 
 * @author The Red5 Project
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class AMFTunnelServlet extends HttpServlet {

    private static final long serialVersionUID = -35436145164322090L;

    protected Logger log = Red5LoggerFactory.getLogger(AMFTunnelServlet.class);

    private static final String REQUEST_TYPE = "application/x-amf";

    private static String postAcceptorURL = "http://localhost:8080/gateway";

    private static int connectionTimeout = 30000;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        //get the url for posting
        if (config.getInitParameter("tunnel.acceptor.url") != null) {
            postAcceptorURL = config.getInitParameter("tunnel.acceptor.url");
        }
        log.debug("POST acceptor URL: {}", postAcceptorURL);
        //get the connection timeout
        if (config.getInitParameter("tunnel.timeout") != null) {
            connectionTimeout = Integer.valueOf(config.getInitParameter("tunnel.timeout"));
        }
        log.debug("POST connection timeout: {}", postAcceptorURL);
    }

    /**
     * Redirect to HTTP port.
     */
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpClient client = HttpConnectionUtil.getClient(connectionTimeout);
        //setup POST
        HttpPost post = null;
        try {
            post = new HttpPost(postAcceptorURL);
            String path = req.getContextPath();
            if (path == null) {
                path = "";
            }
            log.debug("Path: {}", path);
            if (req.getPathInfo() != null) {
                path += req.getPathInfo();
            }
            log.debug("Path 2: {}", path);
            int reqContentLength = req.getContentLength();
            if (reqContentLength > 0) {
                log.debug("Request content length: {}", reqContentLength);
                IoBuffer reqBuffer = IoBuffer.allocate(reqContentLength);
                ServletUtils.copy(req, reqBuffer.asOutputStream());
                reqBuffer.flip();
                post.setEntity(new InputStreamEntity(reqBuffer.asInputStream(), reqContentLength));
                post.addHeader("Content-Type", REQUEST_TYPE);
                // get.setPath(path);
                post.addHeader("Tunnel-request", path);
                // execute the method
                HttpResponse response = client.execute(post);
                int code = response.getStatusLine().getStatusCode();
                log.debug("HTTP response code: {}", code);
                if (code == HttpStatus.SC_OK) {
                    HttpEntity entity = response.getEntity();
                    if (entity != null) {
                        resp.setContentType(REQUEST_TYPE);
                        // get the response as bytes
                        byte[] bytes = EntityUtils.toByteArray(entity);
                        IoBuffer resultBuffer = IoBuffer.wrap(bytes);
                        resultBuffer.flip();
                        ServletUtils.copy(resultBuffer.asInputStream(), resp.getOutputStream());
                        resp.flushBuffer();
                    }
                } else {
                    resp.sendError(code);
                }
            } else {
                resp.sendError(HttpStatus.SC_BAD_REQUEST);
            }
        } catch (Exception ex) {
            log.error("", ex);
            if (post != null) {
                post.abort();
            }
        }
    }
}
