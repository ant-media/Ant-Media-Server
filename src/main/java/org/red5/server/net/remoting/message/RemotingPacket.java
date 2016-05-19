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

package org.red5.server.net.remoting.message;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.red5.server.api.IConnection.Encoding;

/**
 * Packet of remote calls. Used by RemoteProtocolDecoder.
 */
public class RemotingPacket {
    /**
     * HTTP request object
     */
    protected HttpServletRequest request;

    /**
     * Byte buffer data
     */
    protected ByteBuffer data;

    /**
     * Headers sent with request.
     */
    protected Map<String, Object> headers;

    /**
     * List of calls
     */
    protected List<RemotingCall> calls;

    /**
     * Scope path
     */
    protected String scopePath;

    /**
     * Create remoting packet from list of pending calls
     * 
     * @param headers
     *            headers
     * @param calls
     *            List of call objects
     */
    public RemotingPacket(Map<String, Object> headers, List<RemotingCall> calls) {
        this.headers = headers;
        this.calls = calls;
    }

    /**
     * Get the headers sent with the request.
     * 
     * @return headers
     */
    public Map<String, Object> getHeaders() {
        return headers;
    }

    /**
     * Getter for calls.
     *
     * @return List of remote calls
     */
    public List<RemotingCall> getCalls() {
        return calls;
    }

    /**
     * Setter for scope path.
     *
     * @param path
     *            Value to set for property 'scopePath'.
     */
    public void setScopePath(String path) {
        scopePath = path;
    }

    /**
     * Getter for property scope path.
     *
     * @return Scope path to set
     */
    public String getScopePath() {
        return scopePath;
    }

    /**
     * Return the encoding of the included calls.
     * 
     * @return encoding
     */
    public Encoding getEncoding() {
        List<RemotingCall> calls = getCalls();
        if (calls == null || calls.isEmpty()) {
            return Encoding.AMF0;
        }
        RemotingCall call = calls.get(0);
        return call.isAMF3 ? Encoding.AMF3 : Encoding.AMF0;
    }

}
