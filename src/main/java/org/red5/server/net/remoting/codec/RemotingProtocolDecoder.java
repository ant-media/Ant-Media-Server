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

package org.red5.server.net.remoting.codec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.mina.core.buffer.IoBuffer;
import org.red5.io.amf.AMF;
import org.red5.io.amf3.Input.RefStorage;
import org.red5.io.object.Deserializer;
import org.red5.io.object.Input;
import org.red5.server.net.remoting.FlexMessagingService;
import org.red5.server.net.remoting.message.RemotingCall;
import org.red5.server.net.remoting.message.RemotingPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemotingProtocolDecoder {

    protected static Logger log = LoggerFactory.getLogger(RemotingProtocolDecoder.class);

    /**
     * Decodes the given buffer.
     * 
     * @param buffer
     *            input buffer
     * @return a List of {@link RemotingPacket} objects.
     */
    public List<Object> decodeBuffer(IoBuffer buffer) {
        List<Object> list = new LinkedList<Object>();
        Object packet = null;
        try {
            packet = decode(buffer);
        } catch (Exception e) {
            log.error("Decoding error", e);
            packet = null;
        }
        if (packet != null) {
            list.add(packet);
        }
        return list;
    }

    /**
     * Decodes the buffer and returns a remoting packet.
     * 
     * @param in
     *            input buffer
     * @return A {@link RemotingPacket}
     * @throws Exception
     *             on exception
     */
    public Object decode(IoBuffer in) throws Exception {
        Map<String, Object> headers = readHeaders(in);
        List<RemotingCall> calls = decodeCalls(in);
        return new RemotingPacket(headers, calls);
    }

    /**
     * Read remoting headers.
     * 
     * @param in
     *            Input data as byte buffer
     * @return header map
     */
    @SuppressWarnings("unchecked")
    protected Map<String, Object> readHeaders(IoBuffer in) {
        int version = in.getUnsignedShort(); // skip the version
        int count = in.getUnsignedShort();
        log.debug("Read headers - version: {} count: {}", version, count);
        if (count == 0) {
            // No headers present
            return Collections.EMPTY_MAP;
        }
        Input input;
        if (version == 3) {
            input = new org.red5.io.amf3.Input(in);
        } else {
            input = new org.red5.io.amf.Input(in);
        }
        Map<String, Object> result = new HashMap<String, Object>();
        for (int i = 0; i < count; i++) {
            String name = input.getString();
            boolean required = in.get() == 0x01;
            int size = in.getInt();
            Object value = Deserializer.deserialize(input, Object.class);
            log.debug("Header: {} Required: {} Size: {} Value: {}", new Object[] { name, required, size, value });
            result.put(name, value);
        }
        return result;
    }

    /**
     * Decode calls.
     *
     * @param in
     *            Input data as byte buffer
     * @return List of pending calls
     */
    protected List<RemotingCall> decodeCalls(IoBuffer in) {
        log.debug("Decode calls");
        //in.getInt();
        List<RemotingCall> calls = new LinkedList<RemotingCall>();
        org.red5.io.amf.Input input = new org.red5.io.amf.Input(in);
        int count = in.getUnsignedShort();
        log.debug("Calls: {}", count);
        int limit = in.limit();
        // Loop over all the body elements
        for (int i = 0; i < count; i++) {
            in.limit(limit);
            String serviceString = input.getString();
            String clientCallback = input.getString();
            log.debug("callback: {}", clientCallback);
            Object[] args = null;
            boolean isAMF3 = false;
            @SuppressWarnings("unused")
            int length = in.getInt();
            // Set the limit and deserialize
            // NOTE: disabled because the FP sends wrong values here
            /*
             * if (length != -1) in.limit(in.position()+length);
             */
            byte type = in.get();
            if (type == AMF.TYPE_ARRAY) {
                int elements = in.getInt();
                List<Object> values = new ArrayList<Object>();
                RefStorage refStorage = null;
                for (int j = 0; j < elements; j++) {
                    byte amf3Check = in.get();
                    in.position(in.position() - 1);
                    isAMF3 = (amf3Check == AMF.TYPE_AMF3_OBJECT);
                    if (isAMF3) {
                        if (refStorage == null) {
                            input = new org.red5.io.amf3.Input(in);
                        } else {
                            input = new org.red5.io.amf3.Input(in, refStorage);
                        }
                    } else {
                        input = new org.red5.io.amf.Input(in);
                    }
                    // prepare remoting mode
                    input.reset();
                    // add deserialized object to the value list
                    values.add(Deserializer.deserialize(input, Object.class));
                    if (isAMF3) {
                        refStorage = ((org.red5.io.amf3.Input) input).getRefStorage();
                    }
                }
                args = values.toArray(new Object[values.size()]);
                if (log.isDebugEnabled()) {
                    for (Object element : args) {
                        log.debug("> " + element);
                    }
                }
            } else if (type == AMF.TYPE_NULL) {
                log.debug("Got null amf type");
            } else if (type != AMF.TYPE_ARRAY) {
                throw new RuntimeException("AMF0 array type expected but found " + type);
            }
            String serviceName;
            String serviceMethod;
            int dotPos = serviceString.lastIndexOf('.');
            if (dotPos != -1) {
                serviceName = serviceString.substring(0, dotPos);
                serviceMethod = serviceString.substring(dotPos + 1, serviceString.length());
            } else {
                serviceName = "";
                serviceMethod = serviceString;
            }
            boolean isMessaging = false;
            if ("".equals(serviceName) && "null".equals(serviceMethod)) {
                // Use fixed service and method name for Flex messaging requests,
                // this probably will change in the future.
                serviceName = FlexMessagingService.SERVICE_NAME;
                serviceMethod = "handleRequest";
                isMessaging = true;
            }
            log.debug("Service: {} Method: {}", serviceName, serviceMethod);
            // Add the call to the list
            calls.add(new RemotingCall(serviceName, serviceMethod, args, clientCallback, isAMF3, isMessaging));
        }
        return calls;
    }

}
