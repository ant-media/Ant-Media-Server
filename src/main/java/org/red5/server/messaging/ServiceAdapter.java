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

package org.red5.server.messaging;

import org.red5.compatibility.flex.messaging.messages.CommandMessage;
import org.red5.compatibility.flex.messaging.messages.Message;

/**
 * The ServiceAdapter class is the base definition of a service adapter.
 * 
 * @author Paul Gregoire
 */
public abstract class ServiceAdapter {

    /**
     * Starts the adapter if its associated Destination is started and if the adapter is not already running. If subclasses override, they must call super.start().
     */
    public void start() {

    }

    /**
     * Stops the ServiceAdapter. If subclasses override, they must call super.start().
     */
    public void stop() {

    }

    /**
     * Handle a data message intended for this adapter. This method is responsible for handling the message and returning a result (if any). The return value of this message is used as the body of the acknowledge message returned to the client. It may be null if there is no data being returned for this message. Typically the data content for the message is stored in the body property of the message. The headers of
     * the message are used to store fields which relate to the transport of the message. The type of operation is stored as the operation property of the message.
     * 
     * @param message
     *            the message as sent by the client intended for this adapter
     * @return the body of the acknowledge message (or null if there is no body)
     */
    public abstract Object invoke(Message message);

    /**
     * Accept a command from the adapter's service and perform some internal action based upon it. CommandMessages are used for messages which control the state of the connection between the client and the server. For example, this handles subscribe, unsubscribe, and ping operations. The messageRefType property of the CommandMessage is used to associate a command message with a particular service. Services are
     * configured to handle messages of a particular concrete type. For example, the MessageService is typically invoked to handle messages of type flex.messaging.messages.AsyncMessage. To ensure a given CommandMessage is routed to the right service, its MessageRefType is set to the string name of the message type for messages handled by that service.
     * 
     * @param commandMessage
     *            message
     * @return Exception if not implemented
     */
    public Object manage(CommandMessage commandMessage) {
        throw new UnsupportedOperationException("This adapter does not support the manage call");
    }

    /**
     * Returns true if the adapter performs custom subscription management. The default return value is false, and subclasses should override this method as necessary.
     * 
     * @return true if subscriptions are handled
     */
    public boolean handlesSubscriptions() {
        return true;
    }

}
