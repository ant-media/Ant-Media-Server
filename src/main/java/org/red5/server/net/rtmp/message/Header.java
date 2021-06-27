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

package org.red5.server.net.rtmp.message;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * RTMP packet header
 */
public class Header implements Constants, Cloneable, Externalizable {

    private static final long serialVersionUID = 8982665579411495024L;

    public enum HeaderType {
        HEADER_NEW, HEADER_SAME_SOURCE, HEADER_TIMER_CHANGE, HEADER_CONTINUE;
    }

    /**
     * Channel
     */
    private int channelId;

    /**
     * Timer
     */
    private int timerBase;

    /**
     * Delta
     */
    private int timerDelta;

    /**
     * Header size
     */
    private int size;

    /**
     * Type of data
     */
    private byte dataType;

    /**
     * Stream id
     */
    private Number streamId = 0.0d;

    /**
     * Extended Timestamp
     */
    private int extendedTimestamp;

    /**
     * Getter for channel id
     *
     * @return Channel id
     */
    public int getChannelId() {
        return channelId;
    }

    /**
     * Setter for channel id
     *
     * @param channelId
     *            Header channel id
     */
    public void setChannelId(int channelId) {
        this.channelId = channelId;
    }

    /**
     * Getter for data type
     *
     * @return Data type
     */
    public byte getDataType() {
        return dataType;
    }

    /**
     * Setter for data type
     *
     * @param dataType
     *            Data type
     */
    public void setDataType(byte dataType) {
        this.dataType = dataType;
    }

    /**
     * Getter for size.
     *
     * @return Header size
     */
    public int getSize() {
        return size;
    }

    /**
     * Setter for size
     *
     * @param size
     *            Header size
     */
    public void setSize(int size) {
        this.size = size;
    }

    /**
     * Getter for stream id
     *
     * @return Stream id
     */
    public Number getStreamId() {
        return streamId;
    }

    /**
     * Setter for stream id
     *
     * @param streamId
     *            Stream id
     */
    public void setStreamId(Number streamId) {
        this.streamId = streamId;
    }

    /**
     * Getter for Extended Timestamp
     *
     * @return Extended Timestamp
     */
    public int getExtendedTimestamp() {
        return extendedTimestamp;
    }

    /**
     * Setter for Extended Timestamp
     *
     * @param extendedTimestamp
     *            Extended Timestamp
     */
    public void setExtendedTimestamp(int extendedTimestamp) {
        this.extendedTimestamp = extendedTimestamp;
    }

    /**
     * Getter for timer
     *
     * @return Timer
     */
    public int getTimer() {
        return timerBase + timerDelta;
    }

    /**
     * Setter for timer
     *
     * @param timer
     *            Timer
     */
    public void setTimer(int timer) {
        this.timerBase = timer;
        this.timerDelta = 0;
    }

    public void setTimerDelta(int timerDelta) {
        this.timerDelta = timerDelta;
    }

    public int getTimerDelta() {
        return timerDelta;
    }

    public void setTimerBase(int timerBase) {
        this.timerBase = timerBase;
    }

    public int getTimerBase() {
        return timerBase;
    }

    public boolean isEmpty() {
        return !((channelId + dataType + size + streamId.doubleValue()) > 0d);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + channelId;
        result = prime * result + dataType;
        result = prime * result + size;
        result = prime * result + streamId.intValue();
        result = prime * result + getTimer();
        result = prime * result + extendedTimestamp;
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Header)) {
            return false;
        }
        final Header header = (Header) other;
        return (header.getChannelId() == channelId && header.getDataType() == dataType && header.getSize() == size && header.getTimer() == this.getTimer() && header.getStreamId() == streamId && header.getExtendedTimestamp() == extendedTimestamp);
    }

    /** {@inheritDoc} */
    @Override
    public Header clone() {
        final Header header = new Header();
        header.setChannelId(channelId);
        header.setTimerBase(timerBase);
        header.setTimerDelta(timerDelta);
        header.setSize(size);
        header.setDataType(dataType);
        header.setStreamId(streamId);
        header.setExtendedTimestamp(extendedTimestamp);
        return header;
    }

    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        dataType = in.readByte();
        channelId = in.readInt();
        size = in.readInt();
        streamId = (Number) in.readDouble();
        timerBase = in.readInt();
        timerDelta = in.readInt();
        extendedTimestamp = in.readInt();
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeByte(dataType);
        out.writeInt(channelId);
        out.writeInt(size);
        out.writeDouble(streamId.doubleValue());
        out.writeInt(timerBase);
        out.writeInt(timerDelta);
        out.writeInt(extendedTimestamp);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        // if its new and props are un-set, just return that message
        if (isEmpty()) {
            return "empty";
        } else {
            return "Header [streamId=" + streamId + ", channelId=" + channelId + ", dataType=" + dataType + ", timerBase=" + timerBase + ", timerDelta=" + timerDelta + ", size=" + size + ", extendedTimestamp=" + extendedTimestamp + "]";
        }
    }

}
