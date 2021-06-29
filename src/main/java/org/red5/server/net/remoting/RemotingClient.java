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

package org.red5.server.net.remoting;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.util.EntityUtils;
import org.apache.mina.core.buffer.IoBuffer;
import org.red5.io.amf.Input;
import org.red5.io.amf.Output;
import org.red5.io.client.IRemotingClient;
import org.red5.io.object.Deserializer;
import org.red5.io.object.RecordSet;
import org.red5.io.object.Serializer;
import org.red5.server.util.HttpConnectionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client interface for remoting calls.
 * 
 * @author The Red5 Project
 * @author Joachim Bauch (jojo@struktur.de)
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class RemotingClient implements IRemotingClient {

    protected static Logger log = LoggerFactory.getLogger(RemotingClient.class);

    /** Default timeout to use. */
    public static final int DEFAULT_TIMEOUT = 30000;

    /** Content MIME type for HTTP requests. */
    protected static final String CONTENT_TYPE = "application/x-amf";

    /** HTTP client for remoting calls. */
    protected HttpClient client;

    /** Url to connect to. */
    protected String url;

    /** Additional string to use while connecting. */
    protected String appendToUrl = "";

    /** Headers to send to the server. */
    protected Map<String, RemotingHeader> headers = new ConcurrentHashMap<String, RemotingHeader>();

    /** Thread pool to use for asynchronous requests. */
    protected static ExecutorService executor;

    /** Maximum pool threads */
    protected int poolSize = 1;

    /**
     * Dummy constructor used by the spring configuration.
     */
    public RemotingClient() {
        log.debug("RemotingClient created");
    }

    /**
     * Create new remoting client for the given url.
     * 
     * @param url
     *            URL to connect to
     */
    public RemotingClient(String url) {
        this(url, DEFAULT_TIMEOUT);
        log.debug("RemotingClient created  - url: {}", url);
    }

    /**
     * Create new remoting client for the given url and given timeout.
     * 
     * @param url
     *            URL to connect to
     * @param timeout
     *            Timeout for one request in milliseconds
     */
    public RemotingClient(String url, int timeout) {
        client = HttpConnectionUtil.getClient(timeout);
        this.url = url;
        log.debug("RemotingClient created  - url: {} timeout: {}", url, timeout);
    }

    public int getPoolSize() {
        return poolSize;
    }

    public void setPoolSize(int poolSize) {
        this.poolSize = poolSize;
        executor = Executors.newFixedThreadPool(poolSize);
    }

    /**
     * Encode the method call.
     * 
     * @param method
     *            Remote method being called
     * @param params
     *            Method parameters
     * @return Byte buffer with data to perform remoting call
     */
    private IoBuffer encodeInvoke(String method, Object[] params) {
        log.debug("RemotingClient encodeInvoke - method: {} params: {}", method, params);
        IoBuffer result = IoBuffer.allocate(1024);
        result.setAutoExpand(true);
        // XXX: which is the correct version?
        result.putShort((short) 0);
        // Headers
        Collection<RemotingHeader> hdr = headers.values();
        result.putShort((short) hdr.size());
        for (RemotingHeader header : hdr) {
            Output.putString(result, header.name);
            result.put(header.required ? (byte) 0x01 : (byte) 0x00);
            IoBuffer tmp = IoBuffer.allocate(1024);
            tmp.setAutoExpand(true);
            Output tmpOut = new Output(tmp);
            Serializer.serialize(tmpOut, header.data);
            tmp.flip();
            // Size of header data
            result.putInt(tmp.limit());
            // Header data
            result.put(tmp);
            tmp.free();
            tmp = null;
        }
        // One body
        result.putShort((short) 1);

        // Method name
        Output.putString(result, method);

        // Client callback for response
        Output.putString(result, "");

        // Serialize parameters
        IoBuffer tmp = IoBuffer.allocate(1024);
        tmp.setAutoExpand(true);
        Output tmpOut = new Output(tmp);
        //if the params are null send the NULL AMF type
        //this should fix APPSERVER-296
        if (params == null) {
            tmpOut.writeNull();
        } else {
            tmpOut.writeArray(params);
        }
        tmp.flip();
        // Store size and parameters
        result.putInt(tmp.limit());
        result.put(tmp);
        tmp.free();
        tmp = null;

        result.flip();
        return result;
    }

    /**
     * Process any headers sent in the response.
     * 
     * @param in
     *            Byte buffer with response data
     */
    protected void processHeaders(IoBuffer in) {
        log.debug("RemotingClient processHeaders - buffer limit: {}", (in != null ? in.limit() : 0));
        int version = in.getUnsignedShort(); // skip
        log.debug("Version: {}", version);
        // the version by now, AMF3 is not yet implemented
        int count = in.getUnsignedShort();
        log.debug("Count: {}", count);
        Input input = new Input(in);
        for (int i = 0; i < count; i++) {
            String name = input.getString();
            //String name = deserializer.deserialize(input, String.class);
            log.debug("Name: {}", name);
            boolean required = (in.get() == 0x01);
            log.debug("Required: {}", required);
            int len = in.getInt();
            log.debug("Length: {}", len);
            Object value = Deserializer.deserialize(input, Object.class);
            log.debug("Value: {}", value);

            // XXX: this is pretty much untested!!!
            if (RemotingHeader.APPEND_TO_GATEWAY_URL.equals(name)) {
                // Append string to gateway url
                appendToUrl = (String) value;
            } else if (RemotingHeader.REPLACE_GATEWAY_URL.equals(name)) {
                // Replace complete gateway url
                url = (String) value;
                // XXX: reset the <appendToUrl< here?
            } else if (RemotingHeader.PERSISTENT_HEADER.equals(name)) {
                // Send a new header with each following request
                if (value instanceof Map<?, ?>) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> valueMap = (Map<String, Object>) value;
                    RemotingHeader header = new RemotingHeader((String) valueMap.get("name"), (Boolean) valueMap.get("mustUnderstand"), valueMap.get("data"));
                    headers.put(header.name, header);
                } else {
                    log.error("Expected Map but received {}", value);
                }
            } else {
                log.warn("Unsupported remoting header \"{}\" received with value \"{}\"", name, value);
            }
        }
    }

    /**
     * Decode response received from remoting server.
     * 
     * @param data
     *            Result data to decode
     * @return Object deserialized from byte buffer data
     */
    private Object decodeResult(IoBuffer data) {
        log.debug("decodeResult - data limit: {}", (data != null ? data.limit() : 0));
        processHeaders(data);
        int count = data.getUnsignedShort();
        if (count != 1) {
            throw new RuntimeException("Expected exactly one result but got " + count);
        }
        Input input = new Input(data);
        String target = input.getString(); // expect "/onResult"
        log.debug("Target: {}", target);
        String nullString = input.getString(); // expect "null"
        log.debug("Null string: {}", nullString);
        // Read return value
        return Deserializer.deserialize(input, Object.class);
    }

    /**
     * Send authentication data with each remoting request.
     * 
     * @param userid
     *            User identifier
     * @param password
     *            Password
     */
    public void setCredentials(String userid, String password) {
        Map<String, String> data = new HashMap<String, String>();
        data.put("userid", userid);
        data.put("password", password);
        RemotingHeader header = new RemotingHeader(RemotingHeader.CREDENTIALS, true, data);
        headers.put(RemotingHeader.CREDENTIALS, header);
    }

    /**
     * Stop sending authentication data.
     */
    public void resetCredentials() {
        removeHeader(RemotingHeader.CREDENTIALS);
    }

    /**
     * Send an additional header to the server.
     * 
     * @param name
     *            Header name
     * @param required
     *            Header required?
     * @param value
     *            Header body
     */
    public void addHeader(String name, boolean required, Object value) {
        RemotingHeader header = new RemotingHeader(name, required, value);
        headers.put(name, header);
    }

    /**
     * Stop sending a given header.
     * 
     * @param name
     *            Header name
     */
    public void removeHeader(String name) {
        headers.remove(name);
    }

    /**
     * Invoke a method synchronously on the remoting server.
     * 
     * @param method
     *            Method name
     * @param params
     *            Parameters passed to method
     * @return the result of the method call
     */
    public Object invokeMethod(String method, Object[] params) {
        log.debug("invokeMethod url: {}", (url + appendToUrl));
        IoBuffer resultBuffer = null;
        IoBuffer data = encodeInvoke(method, params);
        //setup POST
        HttpPost post = null;
        try {
            post = new HttpPost(url + appendToUrl);
            post.addHeader("Content-Type", CONTENT_TYPE);
            post.setEntity(new InputStreamEntity(data.asInputStream(), data.limit()));
            // execute the method
            HttpResponse response = client.execute(post);
            int code = response.getStatusLine().getStatusCode();
            log.debug("HTTP response code: {}", code);
            if (code / 100 != 2) {
                throw new RuntimeException("Didn't receive success from remoting server");
            } else {
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    //fix for Trac #676
                    int contentLength = (int) entity.getContentLength();
                    //default the content length to 16 if post doesn't contain a good value
                    if (contentLength < 1) {
                        contentLength = 16;
                    }
                    // get the response as bytes
                    byte[] bytes = EntityUtils.toByteArray(entity);
                    resultBuffer = IoBuffer.wrap(bytes);
                    Object result = decodeResult(resultBuffer);
                    if (result instanceof RecordSet) {
                        // Make sure we can retrieve paged results
                        ((RecordSet) result).setRemotingClient(this);
                    }
                    return result;
                }
            }
        } catch (Exception ex) {
            log.error("Error while invoking remoting method: {}", method, ex);
            post.abort();
        } finally {
            if (resultBuffer != null) {
                resultBuffer.free();
                resultBuffer = null;
            }
            data.free();
            data = null;
        }
        return null;
    }

    /**
     * Invoke a method asynchronously on the remoting server.
     * 
     * @param method
     *            Method name
     * @param methodParams
     *            Parameters passed to method
     * @param callback
     *            Callback
     */
    public void invokeMethod(String method, Object[] methodParams, IRemotingCallback callback) {
        try {
            RemotingWorker worker = new RemotingWorker(this, method, methodParams, callback);
            executor.execute(worker);
        } catch (Exception err) {
            log.warn("Exception invoking method: {}", method, err);
        }
    }

    /**
     * Worker class that is used for asynchronous remoting calls.
     */
    public final static class RemotingWorker implements Runnable {

        private final RemotingClient client;

        private final String method;

        private final Object[] params;

        private final IRemotingCallback callback;

        /**
         * Execute task.
         * 
         * @param client
         *            Remoting client
         * @param method
         *            Method name
         * @param params
         *            Parameters to pass to method on call
         * @param callback
         *            Callback
         */
        public RemotingWorker(RemotingClient client, String method, Object[] params, IRemotingCallback callback) {
            this.client = client;
            this.method = method;
            this.params = params;
            this.callback = callback;
        }

        public void run() {
            try {
                Object result = client.invokeMethod(method, params);
                callback.resultReceived(client, method, params, result);
            } catch (Exception err) {
                callback.errorReceived(client, method, params, err);
            }
        }
    }

}
