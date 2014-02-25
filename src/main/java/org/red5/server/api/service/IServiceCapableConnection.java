/*
 * RED5 Open Source Flash Server - http://code.google.com/p/red5/
 * 
 * Copyright 2006-2013 by respective authors (see below). All rights reserved.
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
	 * Invokes service using remoting call object
	 * @param call       Service call object
	 */
	void invoke(IServiceCall call);

	/**
	 * Invoke service using call and channel
	 * @param call       Service call
	 * @param channel    Channel used
	 */
	void invoke(IServiceCall call, int channel);

	/**
	 * Invoke method by name
	 * @param method     Called method name
	 */
	void invoke(String method);

	/**
	 * Invoke method by name with callback
	 * @param method     Called method name
	 * @param callback   Callback
	 */
	void invoke(String method, IPendingServiceCallback callback);

	/**
	 * Invoke method with parameters
	 * @param method     Method name
	 * @param params     Invocation parameters passed to method
	 */
	void invoke(String method, Object[] params);

	/**
	 *
	 * @param method
	 * @param params
	 * @param callback
	 */
	void invoke(String method, Object[] params, IPendingServiceCallback callback);

	/**
	 *
	 * @param call
	 */
	void notify(IServiceCall call);

	/**
	 *
	 * @param call
	 * @param channel
	 */
	void notify(IServiceCall call, int channel);

	/**
	 *
	 * @param method
	 */
	void notify(String method);

	/**
	 * 
	 * @param method
	 * @param params
	 */
	void notify(String method, Object[] params);

	/**
	 * Sends a status object to the connection
	 * @param status
	 */
	void status(Status status);
	
	/**
	 * Sends a status object to the connection on a given channel
	 * @param status
	 * @param channel
	 */
	void status(Status status, int channel);
}
