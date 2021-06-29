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

package org.red5.server.stream;

import org.red5.server.stream.message.RTMPMessage;

/**
 * Interface for classes that implement logic to drop frames.
 * 
 * @author The Red5 Project
 * @author Joachim Bauch (jojo@struktur.de)
 */
public interface IFrameDropper {

    /** Send keyframes, interframes and disposable interframes. */
    public static final int SEND_ALL = 0;

    /** Send keyframes and interframes. */
    public static final int SEND_INTERFRAMES = 1;

    /** Send keyframes only. */
    public static final int SEND_KEYFRAMES = 2;

    /** Send keyframes only and switch to SEND_INTERFRAMES later. */
    public static final int SEND_KEYFRAMES_CHECK = 3;

    /**
     * Checks if a message may be sent to the subscriber.
     * 
     * @param message
     *            the message to check
     * @param pending
     *            the number of pending messages
     * @return <pre>
     * true
     * </pre>
     * 
     *         if the packet may be sent, otherwise
     * 
     *         <pre>
     * false
     * </pre>
     */
    boolean canSendPacket(RTMPMessage message, long pending);

    /**
     * Notify that a packet has been dropped.
     * 
     * @param message
     *            the message that was dropped
     */
    void dropPacket(RTMPMessage message);

    /**
     * Notify that a message has been sent.
     * 
     * @param message
     *            the message that was sent
     */
    void sendPacket(RTMPMessage message);

    /** Reset the frame dropper. */
    void reset();

    /**
     * Reset the frame dropper to a given state.
     * 
     * @param state
     *            the state to reset the frame dropper to
     */
    void reset(int state);

}
