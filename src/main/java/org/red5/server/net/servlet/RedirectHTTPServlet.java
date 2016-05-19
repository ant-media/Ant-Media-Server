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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet to redirect to HTTP port of Red5.
 * 
 * @author The Red5 Project
 * @author Joachim Bauch (jojo@struktur.de)
 */
public class RedirectHTTPServlet extends HttpServlet {

    private static final long serialVersionUID = -3543614516289102090L;

    /**
     * Redirect to HTTP port.
     */
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String host = System.getProperty("http.host");
        String port = System.getProperty("http.port");
        if ("0.0.0.0".equals(host)) {
            host = "127.0.0.1";
        }
        resp.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
        resp.addHeader("Location", "http://" + host + ":" + port);
        resp.setContentType("text/plain");
        String message = "Relocated to http://" + host + ":" + port;
        resp.setContentLength(message.length());
        resp.getWriter().write(message);
        resp.flushBuffer();
    }

}
