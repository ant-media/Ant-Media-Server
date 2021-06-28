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

package org.red5.server.api.service;

import org.red5.server.api.IConnection;
import org.red5.server.net.rtmp.status.Status;

/**
 * Connection that has options to invoke and handle remote calls
 */
// TODO: this should really extend IServiceInvoker
public interface IServiceCapableConnection extends IConnection {
    /**
     * Invokes service using remoting call object.
     * 
     * @param call
     *            Service call object
     */
    void invoke(IServiceCall call);

    /**
     * Invoke service using call and channel.
     * 
     * @param call
     *            Service call
     * @param channel
     *            Channel used
     */
    void invoke(IServiceCall call, int channel);

    /**
     * Invoke method by name.
     * 
     * @param method
     *            Called method name
     */
    void invoke(String method);

    /**
     * Invoke method by name with callback.
     * 
     * @param method
     *            Called method name
     * @param callback
     *            Callback
     */
    void invoke(String method, IPendingServiceCallback callback);

    /**
     * Invoke method with parameters.
     * 
     * @param method
     *            Method name
     * @param params
     *            Invocation parameters passed to method
     */
    void invoke(String method, Object[] params);

    /**
     * Invoke method with parameters.
     *
     * @param method
     *            by name
     * @param params
     *            method params
     * @param callback
     *            callback
     */
    void invoke(String method, Object[] params, IPendingServiceCallback callback);

    /**
     * Notify method.
     *
     * @param call
     *            service call
     */
    void notify(IServiceCall call);

    /**
     * Notify method with channel id.
     *
     * @param call
     *            service call
     * @param channel
     *            channel id
     */
    void notify(IServiceCall call, int channel);

    /**
     * Notify method.
     *
     * @param method
     *            by name
     */
    void notify(String method);

    /**
     * Notify method with parameters.
     * 
     * @param method
     *            by name
     * @param params
     *            method params
     */
    void notify(String method, Object[] params);

    /**
     * Sends a status object to the connection.
     * 
     * @param status
     *            Status
     */
    void status(Status status);

    /**
     * Sends a status object to the connection on a given channel.
     * 
     * @param status
     *            Status
     * @param channel
     *            channel id
     */
    void status(Status status, int channel);

}
