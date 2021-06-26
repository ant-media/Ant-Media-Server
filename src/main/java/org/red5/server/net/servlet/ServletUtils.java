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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.red5.logging.Red5LoggerFactory;
import org.slf4j.Logger;

public class ServletUtils {

    private static Logger log = Red5LoggerFactory.getLogger(ServletUtils.class);

    /**
     * Default value is 2048.
     */
    public static final int DEFAULT_BUFFER_SIZE = 2048;

    /**
     * Copies information from the input stream to the output stream using a default buffer size of 2048 bytes.
     * 
     * @param input
     *            input
     * @param output
     *            output
     * 
     * @throws java.io.IOException
     *             on error
     */
    public static void copy(InputStream input, OutputStream output) throws IOException {
        copy(input, output, DEFAULT_BUFFER_SIZE);
    }

    /**
     * Copies information from the input stream to the output stream using the specified buffer size
     * 
     * @param input
     *            input
     * @param bufferSize
     *            buffer size
     * @param output
     *            output
     * @throws java.io.IOException
     *             on error
     */
    public static void copy(InputStream input, OutputStream output, int bufferSize) throws IOException {
        int availableBytes = input.available();
        log.debug("copy - bufferSize: {} available: {}", bufferSize, availableBytes);
        byte[] buf = null;
        if (availableBytes > 0) {
            if (availableBytes >= bufferSize) {
                buf = new byte[bufferSize];
            } else {
                buf = new byte[availableBytes];
            }
            int bytesRead = input.read(buf);
            while (bytesRead != -1) {
                output.write(buf, 0, bytesRead);
                bytesRead = input.read(buf);
                log.trace("Bytes read: {}", bytesRead);
            }
            output.flush();
        } else {
            log.debug("Available is 0, attempting to read anyway");
            buf = new byte[bufferSize];
            int bytesRead = input.read(buf);
            while (bytesRead != -1) {
                output.write(buf, 0, bytesRead);
                bytesRead = input.read(buf);
                log.trace("Bytes read: {}", bytesRead);
            }
            output.flush();
        }
    }

    /**
     * Copies information from the http request to the output stream using the specified content length.
     * 
     * @param req
     *            Request
     * @param output
     *            Output stream
     * @throws java.io.IOException
     *             on error
     */
    public static void copy(HttpServletRequest req, OutputStream output) throws IOException {
        InputStream input = req.getInputStream();
        int availableBytes = req.getContentLength();
        log.debug("copy - available: {}", availableBytes);
        if (availableBytes > 0) {
            byte[] buf = new byte[availableBytes];
            int bytesRead = input.read(buf);
            while (bytesRead != -1) {
                output.write(buf, 0, bytesRead);
                bytesRead = input.read(buf);
                log.trace("Bytes read: {}", bytesRead);
            }
            output.flush();
        } else {
            log.debug("Nothing to available to copy");
        }
    }

    /**
     * Copies information between specified streams and then closes both of the streams.
     * 
     * @param output
     *            output
     * @param input
     *            input
     * @throws java.io.IOException
     *             on error
     */
    public static void copyThenClose(InputStream input, OutputStream output) throws IOException {
        copy(input, output);
        input.close();
        output.close();
    }

    /**
     * @param input
     *            input stream
     * @return a byte[] containing the information contained in the specified InputStream.
     * @throws java.io.IOException
     *             on error
     */
    public static byte[] getBytes(InputStream input) throws IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        copy(input, result);
        result.close();
        return result.toByteArray();
    }

    /**
     * Return all remote addresses that were involved in the passed request.
     * 
     * @param request
     *            request
     * @return remote addresses
     */
    public static List<String> getRemoteAddresses(HttpServletRequest request) {
        List<String> addresses = new ArrayList<String>();
        addresses.add(request.getRemoteHost());
        if (!request.getRemoteAddr().equals(request.getRemoteHost())) {
            // Store both remote host and remote address 
            addresses.add(request.getRemoteAddr());
        }
        final String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null) {
            // Also store address this request was forwarded for.
            final String[] parts = forwardedFor.split(",");
            for (String part : parts) {
                addresses.add(part);
            }
        }
        final String httpVia = request.getHeader("Via");
        if (httpVia != null) {
            // Also store address this request was forwarded for.
            final String[] parts = httpVia.split(",");
            for (String part : parts) {
                addresses.add(part);
            }
        }
        return Collections.unmodifiableList(addresses);
    }

}
