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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.mina.core.buffer.IoBuffer;
import org.red5.compatibility.flex.messaging.messages.AbstractMessage;
import org.red5.compatibility.flex.messaging.messages.ErrorMessage;
import org.red5.io.amf.Output;
import org.red5.io.object.Serializer;
import org.red5.server.api.IConnection.Encoding;
import org.red5.server.api.Red5;
import org.red5.server.api.remoting.IRemotingConnection;
import org.red5.server.api.remoting.IRemotingHeader;
import org.red5.server.exception.ClientDetailsException;
import org.red5.server.net.remoting.FlexMessagingService;
import org.red5.server.net.remoting.message.RemotingCall;
import org.red5.server.net.remoting.message.RemotingPacket;
import org.red5.server.net.rtmp.status.StatusCodes;
import org.red5.server.net.rtmp.status.StatusObject;
import org.red5.server.service.ServiceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Remoting protocol encoder.
 */
public class RemotingProtocolEncoder {

    protected static Logger log = LoggerFactory.getLogger(RemotingProtocolEncoder.class);

    /**
     * Encodes the given buffer.
     * 
     * @param message
     *            message
     * @return buffer
     * @throws Exception
     *             on exception
     */
    public IoBuffer encode(Object message) throws Exception {
        RemotingPacket resp = (RemotingPacket) message;
        IoBuffer buf = IoBuffer.allocate(1024);
        buf.setAutoExpand(true);
        Output output;
        if (resp.getEncoding() == Encoding.AMF0) {
            buf.putShort((short) 0); // encoded using AMF0
        } else {
            buf.putShort((short) 3); // encoded using AMF3
        }

        IRemotingConnection conn = (IRemotingConnection) Red5.getConnectionLocal();
        Collection<IRemotingHeader> headers = conn.getHeaders();
        buf.putShort((short) headers.size()); // write the header count
        if (resp.getEncoding() == Encoding.AMF0) {
            output = new Output(buf);
        } else {
            output = new org.red5.io.amf3.Output(buf);
        }
        for (IRemotingHeader header : headers) {
            Output.putString(buf, IRemotingHeader.PERSISTENT_HEADER);
            output.writeBoolean(false);
            Map<String, Object> param = new HashMap<String, Object>();
            param.put("name", header.getName());
            param.put("mustUnderstand", header.getMustUnderstand() ? Boolean.TRUE : Boolean.FALSE);
            param.put("data", header.getValue());
            Serializer.serialize(output, param);
        }
        headers.clear();

        buf.putShort((short) resp.getCalls().size()); // write the number of bodies
        for (RemotingCall call : resp.getCalls()) {
            log.debug("Call");
            Output.putString(buf, call.getClientResponse());
            if (!call.isMessaging) {
                Output.putString(buf, "null");
            } else {
                Output.putString(buf, "");
            }
            buf.putInt(-1);
            log.info("result: {}", call.getResult());
            if (call.isAMF3) {
                output = new org.red5.io.amf3.Output(buf);
            } else {
                output = new Output(buf);
            }
            Object result = call.getClientResult();
            if (!call.isSuccess()) {
                if (call.isMessaging && !(result instanceof ErrorMessage)) {
                    // Generate proper error result for the Flex messaging client
                    AbstractMessage request = (AbstractMessage) call.getArguments()[0];
                    if (result instanceof ServiceNotFoundException) {
                        ServiceNotFoundException ex = (ServiceNotFoundException) result;
                        result = FlexMessagingService.returnError(request, "serviceNotAvailable", "Flex messaging not activated", ex.getMessage());
                    } else if (result instanceof Throwable) {
                        result = FlexMessagingService.returnError(request, "Server.Invoke.Error", ((Throwable) result).getMessage(), (Throwable) result);
                    } else {
                        result = FlexMessagingService.returnError(request, "Server.Invoke.Error", result.toString(), "");
                    }
                } else if (!call.isMessaging) {
                    // Generate proper error object to return
                    result = generateErrorResult(StatusCodes.NC_CALL_FAILED, call.getException());
                }
            }
            Serializer.serialize(output, result);
        }
        buf.flip();
        if (log.isDebugEnabled()) {
            log.debug(">>{}", buf.getHexDump());
        }
        return buf;

    }

    /**
     * Generate error object to return for given exception.
     * 
     * @param code
     *            call
     * @param error
     *            error
     * @return status object
     */
    protected StatusObject generateErrorResult(String code, Throwable error) {
        // Construct error object to return
        String message = "";
        while (error != null && error.getCause() != null) {
            error = error.getCause();
        }
        if (error != null && error.getMessage() != null) {
            message = error.getMessage();
        }
        StatusObject status = new StatusObject(code, "error", message);
        if (error instanceof ClientDetailsException) {
            // Return exception details to client
            status.setApplication(((ClientDetailsException) error).getParameters());
            if (((ClientDetailsException) error).includeStacktrace()) {
                List<String> stack = new ArrayList<String>();
                for (StackTraceElement element : error.getStackTrace()) {
                    stack.add(element.toString());
                }
                status.setAdditional("stacktrace", stack);
            }
        } else if (error != null) {
            status.setApplication(error.getClass().getCanonicalName());
        }
        return status;
    }

}
