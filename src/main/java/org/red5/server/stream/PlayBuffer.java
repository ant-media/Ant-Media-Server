/*
 * RED5 Open Source Flash Server - https://github.com/Red5/
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

import java.util.LinkedList;
import java.util.Queue;

import org.red5.server.net.rtmp.event.IRTMPEvent;
import org.red5.server.stream.message.RTMPMessage;

/**
 * A Play buffer for sending VOD. The implementation is not synchronized.
 * 
 * @author The Red5 Project
 * @author Steven Gong (steven.gong@gmail.com)
 */
public class PlayBuffer {

    /**
     * Buffer capacity
     */
    private long capacity;

    /**
     * Message size
     */
    private long messageSize;

    /**
     * Queue of RTMP messages
     */
    private Queue<RTMPMessage> messageQueue = new LinkedList<>();

    /**
     * Create play buffer with given capacity
     * 
     * @param capacity
     *            Capacity of buffer
     */
    public PlayBuffer(long capacity) {
        this.capacity = capacity;
    }

    /**
     * Buffer capacity in bytes.
     * 
     * @return Buffer capacity in bytes
     */
    public long getCapacity() {
        return capacity;
    }

    /**
     * Setter for capacity
     *
     * @param capacity
     *            New capacity
     */
    public void setCapacity(long capacity) {
        this.capacity = capacity;
    }

    /**
     * Number of messages in buffer.
     * 
     * @return Number of messages in buffer
     */
    public int getMessageCount() {
        return messageQueue.size();
    }

    /**
     * Total message size in bytes.
     * 
     * @return Total message size in bytes
     */
    public long getMessageSize() {
        return messageSize;
    }

    /**
     * Put a message into this buffer.
     * 
     * @param message
     *            RTMP message
     * @return <tt>true</tt> indicates success and <tt>false</tt> indicates buffer is full.
     */
    public boolean putMessage(RTMPMessage message) {
        IRTMPEvent body = message.getBody();
        if (!(body instanceof IStreamData)) {
            throw new RuntimeException("Expected IStreamData but got " + body);
        }
        int size = ((IStreamData<?>) body).getData().limit();
        if (messageSize + size > capacity) {
            return false;
        }
        messageSize += size;
        messageQueue.offer(message);
        return true;
    }

    /**
     * Take a message from this buffer. The message count decreases.
     * 
     * @return <tt>null</tt> if buffer is empty.
     */
    public RTMPMessage takeMessage() {
        RTMPMessage message = messageQueue.poll();
        if (message != null) {
            IRTMPEvent body = message.getBody();
            if (!(body instanceof IStreamData)) {
                throw new RuntimeException("Expected IStreamData but got " + body);
            }
            messageSize -= ((IStreamData<?>) body).getData().limit();
        }
        return message;
    }

    /**
     * Peek a message but not take it from the buffer. The message count doesn't change.
     * 
     * @return <tt>null</tt> if buffer is empty.
     */
    public RTMPMessage peekMessage() {
        return messageQueue.peek();
    }

    /**
     * Empty this buffer.
     */
    public void clear() {
        messageQueue.clear();
        messageSize = 0;
    }
}
