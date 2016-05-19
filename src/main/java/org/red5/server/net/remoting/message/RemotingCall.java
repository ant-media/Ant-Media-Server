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

import org.red5.compatibility.flex.messaging.messages.ErrorMessage;
import org.red5.server.service.PendingCall;

/**
 * Remoting method call, specific pending call.
 *
 * @author The Red5 Project
 * @author Luke Hubbard, Codegent Ltd (luke@codegent.com)
 */
public class RemotingCall extends PendingCall {
    /**
     * Handler success posfix constant
     */
    public static final String HANDLER_SUCCESS = "/onResult";

    /**
     * Handler error posfix constant
     */
    public static final String HANDLER_ERROR = "/onStatus";

    /**
     * Client callback name
     */
    public String clientCallback;

    public boolean isAMF3;

    public boolean isMessaging;

    /**
     * Default / void constructor to prevent runtime exception.
     */
    public RemotingCall() {
    }

    /**
     * Create remoting call from service name, method name, list of arguments and callback name.
     *
     * @param serviceName
     *            Service name
     * @param serviceMethod
     *            Service method name
     * @param args
     *            Parameters passed to method
     * @param callback
     *            Name of client callback
     * @param isAMF3
     *            Does the client support AMF3?
     * @param isMessaging
     *            Is this a Flex messaging request?
     */
    public RemotingCall(String serviceName, String serviceMethod, Object[] args, String callback, boolean isAMF3, boolean isMessaging) {
        super(serviceName, serviceMethod, args);
        setClientCallback(callback);
        this.isAMF3 = isAMF3;
        this.isMessaging = isMessaging;
    }

    /**
     * Setter for client callback.
     *
     * @param clientCallback
     *            Client callback
     */
    public void setClientCallback(String clientCallback) {
        this.clientCallback = clientCallback;
    }

    /**
     * Getter for client response.
     *
     * @return Client response
     */
    public String getClientResponse() {
        if (clientCallback != null) {
            return clientCallback + (isSuccess() && !(getClientResult() instanceof ErrorMessage) ? HANDLER_SUCCESS : HANDLER_ERROR);
        } else {
            return null;
        }
    }

    /**
     * Getter for client result.
     *
     * @return Client result
     */
    public Object getClientResult() {
        return isSuccess() ? getResult() : getException();
    }

}
