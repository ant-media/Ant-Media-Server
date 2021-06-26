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

package org.red5.server.net.rtmp.event;

import org.red5.server.api.event.IEvent;
import org.red5.server.api.event.IEventListener;
import org.red5.server.net.rtmp.message.Header;

public interface IRTMPEvent extends IEvent {

    /**
     * Getter for data type
     *
     * @return Data type
     */
    public byte getDataType();

    /**
     * Setter for source
     *
     * @param source
     *            Source
     */
    public void setSource(IEventListener source);

    /**
     * Getter for header
     *
     * @return RTMP packet header
     */
    public Header getHeader();

    /**
     * Setter for header
     *
     * @param header
     *            RTMP packet header
     */
    public void setHeader(Header header);

    /**
     * Getter for timestamp
     *
     * @return Event timestamp
     */
    public int getTimestamp();

    /**
     * Setter for timestamp
     *
     * @param timestamp
     *            New event timestamp
     */
    public void setTimestamp(int timestamp);

    /**
     * Getter for source type
     *
     * @return Source type
     */
    public byte getSourceType();

    /**
     * Setter for source type
     *
     * @param sourceType
     *            source type
     */
    public void setSourceType(byte sourceType);

    /**
     * Retain event
     */
    public void retain();

    /**
     * Hook to free buffers allocated by the event.
     */
    public void release();

}
