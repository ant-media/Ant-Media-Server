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

package org.red5.server.net.rtmp;

import org.red5.server.net.rtmp.message.Packet;

/**
 * RTMP events handler
 */
public interface IRTMPHandler {

    /**
     * Connection open event.
     * 
     * @param conn
     *            Connection
     */
    public void connectionOpened(RTMPConnection conn);

    /**
     * Message received.
     * 
     * @param conn
     *            Connection
     * @param packet
     *            Packet containing an RTMP message
     * @throws Exception
     *             on exception
     */
    public void messageReceived(RTMPConnection conn, Packet packet) throws Exception;

    /**
     * Message sent.
     * 
     * @param conn
     *            Connection
     * @param packet
     *            RTMP message
     */
    public void messageSent(RTMPConnection conn, Packet packet);

    /**
     * Connection closed.
     * 
     * @param conn
     *            Connection
     */
    public void connectionClosed(RTMPConnection conn);

}
