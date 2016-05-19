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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.beanutils.ConversionException;
import org.apache.commons.lang3.StringUtils;
import org.red5.compatibility.flex.data.messages.DataMessage;
import org.red5.compatibility.flex.data.messages.SequencedMessage;
import org.red5.compatibility.flex.messaging.messages.AbstractMessage;
import org.red5.compatibility.flex.messaging.messages.AcknowledgeMessage;
import org.red5.compatibility.flex.messaging.messages.AcknowledgeMessageExt;
import org.red5.compatibility.flex.messaging.messages.AsyncMessage;
import org.red5.compatibility.flex.messaging.messages.AsyncMessageExt;
import org.red5.compatibility.flex.messaging.messages.CommandMessage;
import org.red5.compatibility.flex.messaging.messages.CommandMessageExt;
import org.red5.compatibility.flex.messaging.messages.Constants;
import org.red5.compatibility.flex.messaging.messages.ErrorMessage;
import org.red5.compatibility.flex.messaging.messages.Message;
import org.red5.compatibility.flex.messaging.messages.RemotingMessage;
import org.red5.io.utils.ConversionUtils;
import org.red5.io.utils.RandomGUID;
import org.red5.server.api.IClient;
import org.red5.server.api.Red5;
import org.red5.server.api.service.IPendingServiceCall;
import org.red5.server.api.service.IServiceInvoker;
import org.red5.server.exception.ClientDetailsException;
import org.red5.server.messaging.ServiceAdapter;
import org.red5.server.service.PendingCall;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service that can execute compatibility Flex messages.
 * 
 * @author The Red5 Project
 * @author Joachim Bauch (jojo@struktur.de)
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class FlexMessagingService {

    /** Name of the service. */
    public static final String SERVICE_NAME = "flexMessaging";

    /**
     * Logger
     */
    protected static Logger log = LoggerFactory.getLogger(FlexMessagingService.class);

    /** Service invoker to use. */
    protected IServiceInvoker serviceInvoker;

    /** Configured endpoints. */
    @SuppressWarnings("unchecked")
    protected Map<String, Object> endpoints = Collections.EMPTY_MAP;

    /** Registered clients. */
    protected ConcurrentMap<String, ServiceAdapter> registrations = null;

    /**
     * Setup available end points.
     * 
     * @param endpoints
     *            end points
     */
    public void setEndpoints(Map<String, Object> endpoints) {
        this.endpoints = endpoints;
        log.info("Configured endpoints: {}", endpoints);
    }

    /**
     * Set the service invoker to use.
     * 
     * @param serviceInvoker
     *            service invoker
     */
    public void setServiceInvoker(IServiceInvoker serviceInvoker) {
        this.serviceInvoker = serviceInvoker;
    }

    /**
     * Construct error message.
     * 
     * @param request
     *            request
     * @param faultCode
     *            fault code
     * @param faultString
     *            fault string
     * @param faultDetail
     *            fault detail
     * @return error message
     */
    public static ErrorMessage returnError(AbstractMessage request, String faultCode, String faultString, String faultDetail) {
        ErrorMessage result = new ErrorMessage();
        result.timestamp = System.currentTimeMillis();
        result.headers = request.headers;
        result.destination = request.destination;
        result.correlationId = request.messageId;
        result.faultCode = faultCode;
        result.faultString = faultString;
        result.faultDetail = faultDetail;
        return result;
    }

    /**
     * Construct error message from exception.
     * 
     * @param request
     *            request
     * @param faultCode
     *            fault code
     * @param faultString
     *            fautl string
     * @param error
     *            error
     * @return message
     */
    public static ErrorMessage returnError(AbstractMessage request, String faultCode, String faultString, Throwable error) {
        ErrorMessage result = returnError(request, faultCode, faultString, "");
        if (error instanceof ClientDetailsException) {
            result.extendedData = ((ClientDetailsException) error).getParameters();
            if (((ClientDetailsException) error).includeStacktrace()) {
                StringBuilder stack = new StringBuilder();
                for (StackTraceElement element : error.getStackTrace()) {
                    stack.append(element.toString()).append('\n');
                }
                result.faultDetail = stack.toString();
            }
        }
        result.rootCause = error;
        return result;
    }

    /**
     * Handle request coming from
     * 
     * <pre>
     * mx:RemoteObject
     * </pre>
     * 
     * tags.
     * 
     * @see <a href="http://livedocs.adobe.com/flex/2/langref/mx/rpc/remoting/mxml/RemoteObject.html">Adobe Livedocs (external)</a>
     *
     * @param msg
     *            message
     * @return aynsc message
     */
    public AsyncMessage handleRequest(RemotingMessage msg) {
        log.debug("Handle RemotingMessage request");
        log.trace("{}", msg);
        setClientId(msg);
        if (serviceInvoker == null) {
            log.error("No service invoker configured: {}", msg);
            return returnError(msg, "Server.Invoke.Error", "No service invoker configured.", "No service invoker configured.");
        }

        Object endpoint = endpoints.get(msg.destination);
        log.debug("End point / destination: {}", endpoint);
        if (endpoint == null) {
            String errMsg = String.format("Endpoint %s doesn't exist.", msg.destination);
            log.debug("{} ({})", errMsg, msg);
            return returnError(msg, "Server.Invoke.Error", errMsg, errMsg);
        }

        //prepare an ack message
        AcknowledgeMessage result = new AcknowledgeMessage();
        result.setClientId(msg.getClientId());
        result.setCorrelationId(msg.getMessageId());

        //grab any headers
        Map<String, Object> headers = msg.getHeaders();
        log.debug("Headers: {}", headers);
        //if (headers.containsKey(Message.FLEX_CLIENT_ID_HEADER)) {
        //	headers.put(Message.FLEX_CLIENT_ID_HEADER, msg.getClientId());
        //}
        //result.setHeaders(headers);

        //get the operation
        String operation = msg.operation;
        log.debug("Operation: {}", operation);

        if (endpoint instanceof ServiceAdapter) {
            log.debug("Endpoint is a ServiceAdapter so message will be invoked");
            ServiceAdapter adapter = (ServiceAdapter) endpoint;
            //the result of the invocation will make up the message body
            result.body = adapter.invoke(msg);
        } else {
            //get arguments
            Object[] args = null;
            try {
                log.debug("Body: {} type: {}", msg.body, msg.body.getClass().getName());
                args = (Object[]) ConversionUtils.convert(msg.body, Object[].class);
            } catch (ConversionException cex) {
                //if the conversion fails and the endpoint is not a ServiceAdapter
                //just drop the object directly into an array
                args = new Object[] { msg.body };
            }

            IPendingServiceCall call = new PendingCall(operation, args);
            try {
                if (!serviceInvoker.invoke(call, endpoint)) {
                    if (call.getException() != null) {
                        // Use regular exception handling
                        Throwable err = call.getException();
                        return returnError(msg, "Server.Invoke.Error", err.getMessage(), err);
                    }
                    return returnError(msg, "Server.Invoke.Error", "Can't invoke method.", "");
                }
            } catch (Throwable err) {
                log.error("Error while invoking method.", err);
                return returnError(msg, "Server.Invoke.Error", err.getMessage(), err);
            }

            //we got a valid result from the method call.
            result.body = call.getResult();
        }

        return result;
    }

    /**
     * Handle command message (external) request.
     * 
     * @param msg
     *            message
     * @return message
     */
    public Message handleRequest(CommandMessageExt msg) {
        log.debug("Handle CommandMessageExt request");
        log.trace("{}", msg);
        setClientId(msg);
        String clientId = msg.getClientId();

        //process messages to non-service adapter end-points
        switch (msg.operation) {
            case Constants.POLL_OPERATION: //2
                //send back modifications
                log.debug("Poll: {}", clientId);
                //send back stored updates for this client
                if (registrations.containsKey(clientId)) {
                    ServiceAdapter adapter = registrations.get(clientId);
                    if (adapter != null) {
                        CommandMessage result = new CommandMessage();
                        result.setOperation(Constants.CLIENT_SYNC_OPERATION);
                        //result.setCorrelationId(msg.getMessageId());
                        //this will be the body of the responding command message
                        AsyncMessageExt ext = new AsyncMessageExt();
                        ext.setClientId(clientId);
                        ext.setCorrelationId(msg.getMessageId());
                        ext.setDestination("Red5Chat");
                        ext.setBody(adapter.manage(msg));
                        /*
                        //grab any headers
                        Map<String, Object> headers = msg.getHeaders();
                        log.debug("Headers: {}", headers);
                        if (headers.containsKey(Message.FLEX_CLIENT_ID_HEADER)) {
                        	headers.put(Message.FLEX_CLIENT_ID_HEADER, msg.getClientId());
                        }
                        ext.setHeaders(headers);
                        */
                        //add as a child (body) of the command message
                        result.setBody(new Object[] { ext });

                        return result;
                    } else {
                        log.warn("Adapter was not available");
                    }
                }
                break;
            default:
                log.error("Unhandled CommandMessageExt request: {}", msg);
                String errMsg = String.format("Don't know how to handle %s", msg);
                return returnError(msg, "notImplemented", errMsg, errMsg);
        }

        AcknowledgeMessageExt result = new AcknowledgeMessageExt();
        result.setClientId(clientId);
        result.setCorrelationId(msg.getMessageId());

        return result;
    }

    /**
     * Handle command message request.
     * 
     * @param msg
     *            message
     * @return message
     */
    public Message handleRequest(CommandMessage msg) {
        log.debug("Handle CommandMessage request");
        log.trace("{}", msg);
        setClientId(msg);
        String clientId = msg.getClientId();

        //grab any headers
        Map<String, Object> headers = msg.getHeaders();
        log.debug("Headers: {}", headers);
        if (headers.containsKey(Message.FLEX_CLIENT_ID_HEADER)) {
            headers.put(Message.FLEX_CLIENT_ID_HEADER, msg.getClientId());
        }

        String destination = msg.getDestination();
        log.debug("Destination: {}", destination);

        //process messages to non-service adapter end-points
        switch (msg.operation) {
            case Constants.CLIENT_PING_OPERATION: //5
                //send back pong message
                break;

            case Constants.POLL_OPERATION: //2
                //send back modifications
                log.debug("Poll: {}", clientId);
                //send back stored updates for this client
                if (registrations.containsKey(clientId)) {
                    ServiceAdapter adapter = registrations.get(clientId);
                    if (adapter != null) {
                        CommandMessage result = new CommandMessage();
                        result.setOperation(Constants.CLIENT_SYNC_OPERATION);
                        //result.setCorrelationId(msg.getMessageId());
                        //this will be the body of the responding command message
                        AsyncMessageExt ext = new AsyncMessageExt();
                        ext.setClientId(clientId);
                        ext.setCorrelationId(msg.getMessageId());
                        ext.setDestination("Red5Chat");
                        ext.setBody(adapter.manage(msg));
                        //add as a child (body) of the command message
                        result.setBody(new Object[] { ext });

                        return result;
                    } else {
                        log.debug("Adapter was not available");
                    }
                }
                break;

            case Constants.SUBSCRIBE_OPERATION: //0
                log.debug("Subscribe: {}", clientId);
                //if there is a destination check for an adapter
                if (StringUtils.isNotBlank(destination)) {
                    //look-up end-point and register
                    if (endpoints.containsKey(destination)) {
                        Object endpoint = endpoints.get(destination);
                        //if the endpoint is an adapter, try to subscribe
                        if (endpoint instanceof ServiceAdapter) {
                            ServiceAdapter adapter = (ServiceAdapter) endpoint;
                            boolean subscribed = ((Boolean) adapter.manage(msg));
                            if (subscribed) {
                                log.debug("Client was subscribed");
                                registerClientToAdapter(clientId, adapter);
                            } else {
                                log.debug("Client was not subscribed");
                            }
                        }
                    }
                }
                // Send back registration ok
                break;

            case Constants.UNSUBSCRIBE_OPERATION: //1
                log.trace("Unsubscribe: {}", clientId);
                if (registrations.containsKey(clientId)) {
                    ServiceAdapter adapter = registrations.get(clientId);
                    boolean unsubscribed = ((Boolean) adapter.manage(msg));
                    if (unsubscribed) {
                        log.debug("Client was unsubscribed");
                        unregisterClientFromAdapter(clientId);
                    } else {
                        log.debug("Client was not unsubscribed");
                    }
                } else {
                    log.debug("Client was not subscribed");
                }
                // Send back unregistration ok
                break;

            default:
                log.error("Unknown CommandMessage request: {}", msg);
                String errMsg = String.format("Don't know how to handle %s", msg);
                return returnError(msg, "notImplemented", errMsg, errMsg);
        }

        AcknowledgeMessage result = new AcknowledgeMessage();
        result.setBody(msg.getBody());
        result.setClientId(clientId);
        result.setCorrelationId(msg.getMessageId());
        result.setHeaders(headers);

        // put destination in ack if it exists
        if (StringUtils.isNotBlank(destination)) {
            result.setDestination(destination);
        }

        return result;
    }

    /**
     * Evaluate update requests sent by a client.
     * 
     * @param msg
     * @param event
     */
    @SuppressWarnings("unchecked")
    private void evaluateDataUpdate(DataMessage msg, DataMessage event) {
        switch (event.operation) {
            case Constants.DATA_OPERATION_UPDATE_ATTRIBUTES:
                List<Object> contents = (List<Object>) event.body;
                @SuppressWarnings("unused")
                List<String> attributeNames = (List<String>) contents.get(0);
                @SuppressWarnings("unused")
                Map<String, Object> oldValues = (Map<String, Object>) contents.get(1);
                @SuppressWarnings("unused")
                Map<String, Object> newValues = (Map<String, Object>) contents.get(2);
                /*
                // Commented out as it triggeres a crash in the compiler on Java 1.5
                for (@SuppressWarnings("unused") String name: attributeNames) {
                	// TODO: store attribute change for registered clients
                }
                */
                break;

            default:
                log.error("Unknown data update request: {}", event);
        }
    }

    /**
     * Handle messages related to shared objects.
     * 
     * @param msg
     *            message
     * @return async message
     */
    @SuppressWarnings("unchecked")
    public AsyncMessage handleRequest(DataMessage msg) {
        log.debug("Handle DataMessage request");
        log.trace("{}", msg);
        setClientId(msg);
        SequencedMessage result = new SequencedMessage();
        result.clientId = msg.clientId;
        result.destination = msg.destination;
        result.correlationId = msg.messageId;
        switch (msg.operation) {
            case Constants.DATA_OPERATION_SET:
                result.body = new Object[] { msg.body };
                result.sequenceId = 0;
                result.sequenceSize = 1;
                // TODO: store initial version of object
                break;

            case Constants.DATA_OPERATION_UPDATE:
                for (DataMessage event : (List<DataMessage>) msg.body) {
                    evaluateDataUpdate(msg, event);
                }
                AcknowledgeMessage res = new AcknowledgeMessage();
                res.clientId = msg.clientId;
                res.destination = msg.destination;
                res.correlationId = msg.messageId;
                res.body = msg.body;
                return res;

            default:
                log.error("Unknown DataMessage request: {}", msg);
                String errMsg = String.format("Don't know how to handle %s", msg);
                return returnError(msg, "notImplemented", errMsg, errMsg);

        }
        return result;
    }

    /**
     * Fallback method to handle arbitrary messages.
     * 
     * @param msg
     *            message
     * @return error message
     */
    public Message handleRequest(AbstractMessage msg) {
        log.debug("Handle AbstractMessage request");
        log.trace("{}", msg);
        setClientId(msg);

        Object endpoint = endpoints.get(msg.getDestination());
        log.debug("End point / destination: {}", endpoint);
        if (endpoint == null) {
            String errMsg = String.format("Endpoint %s doesn't exist.", msg.getDestination());
            log.debug("{} ({})", errMsg, msg);
            return returnError(msg, "Server.Invoke.Error", errMsg, errMsg);
        }

        //grab any headers
        Map<String, Object> headers = msg.getHeaders();
        log.debug("Headers: {}", headers);
        if (headers.containsKey(Message.FLEX_CLIENT_ID_HEADER)) {
            headers.remove(Message.FLEX_CLIENT_ID_HEADER);
        }
        if (headers.containsKey(Message.ENDPOINT_HEADER)) {
            headers.remove(Message.ENDPOINT_HEADER);
        }

        if (endpoint instanceof ServiceAdapter) {
            log.debug("Endpoint is a ServiceAdapter so message will be invoked");
            //prepare an ack message
            AcknowledgeMessage result = new AcknowledgeMessage();
            result.setClientId(msg.getClientId());
            result.setCorrelationId(msg.getMessageId());
            result.setDestination(msg.getDestination());
            result.setHeaders(headers);
            //get the adapter
            ServiceAdapter adapter = (ServiceAdapter) endpoint;
            //log.debug("Invoke: {}", adapter.invoke(msg));
            Object o = adapter.invoke(msg);
            //the result of the invocation will make up the message body		
            //AsyncMessage ext = new AsyncMessage();
            //ext.setClientId(msg.getClientId());
            //ext.setCorrelationId(result.getMessageId());
            //ext.setBody(o);

            result.setBody(new Object[] { o });
            return result;
        } else {
            log.error("Unknown Flex compatibility request: {}", msg);
            String errMsg = String.format("Don't know how to handle %s", msg);
            return returnError(msg, "notImplemented", errMsg, errMsg);
        }
    }

    /**
     * This is mandatory for client built from Flex 3 or later, or client will hang with concurrent accesses.
     * 
     * @param msg
     */
    private void setClientId(AbstractMessage msg) {
        String clientId = msg.getClientId();
        if (clientId == null || "null".equals(clientId)) {
            log.trace("Dump: {}", msg);
            //use the connection client id before creating a new one
            IClient client = Red5.getConnectionLocal().getClient();
            if (client != null) {
                //should we format it?
                clientId = client.getId();
                msg.setClientId(RandomGUID.getPrettyFormatted(clientId));
            } else {
                msg.setClientId(UUID.randomUUID().toString());
            }
        }
    }

    /**
     * Maps a client to an adapter for lookups on messages that do not contain a destination.
     * 
     * @param clientId
     *            a subscribed client id
     * @param adapter
     *            service adapter to register for
     */
    private final void registerClientToAdapter(String clientId, ServiceAdapter adapter) {
        if (registrations == null) {
            registrations = new ConcurrentHashMap<String, ServiceAdapter>();
        }
        registrations.put(clientId, adapter);
    }

    /**
     * Removes a mapping for a client with an adapter.
     * 
     * @param clientId
     *            a subscribed client id
     */
    private final void unregisterClientFromAdapter(String clientId) {
        registrations.remove(clientId);
    }

}
