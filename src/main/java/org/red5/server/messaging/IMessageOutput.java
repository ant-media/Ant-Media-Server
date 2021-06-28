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

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Output Endpoint for a provider to connect.
 * 
 * @author The Red5 Project
 * @author Steven Gong (steven.gong@gmail.com)
 */
public interface IMessageOutput {
    /**
     * Push a message to this output endpoint. May block the pusher when output can't handle the message at the time.
     * 
     * @param message
     *            Message to be pushed.
     * @throws IOException
     *             If message could not be written.
     */
    void pushMessage(IMessage message) throws IOException;

    /**
     * Connect to a provider. Note that params passed has nothing to deal with NetConnection.connect in client-side Flex/Flash RIA.
     * 
     * @param provider
     *            Provider
     * @param paramMap
     *            Parameters passed with connection
     * @return <tt>true</tt> when successfully subscribed, <tt>false</tt> otherwise.
     */
    boolean subscribe(IProvider provider, Map<String, Object> paramMap);

    /**
     * Disconnect from a provider.
     * 
     * @param provider
     *            Provider
     * @return <tt>true</tt> when successfully unsubscribed, <tt>false</tt> otherwise.
     */
    boolean unsubscribe(IProvider provider);

    /**
     * Getter for providers
     *
     * @return Providers
     */
    List<IProvider> getProviders();

    /**
     * Send OOB Control Message to all consumers on the other side of pipe.
     * 
     * @param provider
     *            The provider that sends the message
     * @param oobCtrlMsg
     *            Out-of-band control message
     */
    void sendOOBControlMessage(IProvider provider, OOBControlMessage oobCtrlMsg);
}
