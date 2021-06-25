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

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Ping event, actually combination of different events. This is also known as a user control message.
 */
public class Ping extends BaseEvent {

    private static final long serialVersionUID = -6478248060425544923L;

    /**
     * Stream begin / clear event
     */
    public static final short STREAM_BEGIN = 0;

    /**
     * Stream EOF, playback of requested stream is completed.
     */
    public static final short STREAM_PLAYBUFFER_CLEAR = 1;

    /**
     * Stream is empty
     */
    public static final short STREAM_DRY = 2;

    /**
     * Client buffer. Sent by client to indicate its buffer time in milliseconds.
     */
    public static final short CLIENT_BUFFER = 3;

    /**
     * Recorded stream. Sent by server to indicate a recorded stream.
     */
    public static final short RECORDED_STREAM = 4;

    /**
     * One more unknown event
     */
    public static final short UNKNOWN_5 = 5;

    /**
     * Client ping event. Sent by server to test if client is reachable.
     */
    public static final short PING_CLIENT = 6;

    /**
     * Server response event. A clients ping response.
     */
    public static final short PONG_SERVER = 7;

    /**
     * One more unknown event
     */
    public static final short UNKNOWN_8 = 8;

    /**
     * SWF verification ping 0x001a
     */
    public static final short PING_SWF_VERIFY = 26;

    /**
     * SWF verification pong 0x001b
     */
    public static final short PONG_SWF_VERIFY = 27;

    /**
     * Buffer empty.
     */
    public static final short BUFFER_EMPTY = 31;

    /**
     * Buffer full.
     */
    public static final short BUFFER_FULL = 32;

    /**
     * Event type is undefined
     */
    public static final int UNDEFINED = -1;

    /**
     * The sub-type
     */
    protected short eventType;

    /**
     * Represents the stream id in all cases except PING_CLIENT and PONG_SERVER where it represents the local server timestamp.
     */
    private Number value2;

    private int value3 = UNDEFINED;

    private int value4 = UNDEFINED;

    /**
     * Debug string
     */
    private String debug = "";

    /** Constructs a new Ping. */
    public Ping() {
        super(Type.SYSTEM);
    }

    public Ping(short eventType) {
        super(Type.SYSTEM);
        this.eventType = eventType;
    }

    public Ping(short eventType, int value2) {
        super(Type.SYSTEM);
        this.eventType = eventType;
        this.value2 = value2;
    }

    public Ping(short eventType, Number value2) {
        super(Type.SYSTEM);
        this.eventType = eventType;
        this.value2 = value2;
    }

    public Ping(short eventType, int value2, int value3) {
        super(Type.SYSTEM);
        this.eventType = eventType;
        this.value2 = value2;
        this.value3 = value3;
    }

    public Ping(short eventType, Number value2, int value3) {
        super(Type.SYSTEM);
        this.eventType = eventType;
        this.value2 = value2;
        this.value3 = value3;
    }

    public Ping(short eventType, int value2, int value3, int value4) {
        super(Type.SYSTEM);
        this.eventType = eventType;
        this.value2 = value2;
        this.value3 = value3;
        this.value4 = value4;
    }

    public Ping(short eventType, Number value2, int value3, int value4) {
        super(Type.SYSTEM);
        this.eventType = eventType;
        this.value2 = value2;
        this.value3 = value3;
        this.value4 = value4;
    }

    public Ping(Ping in) {
        super(Type.SYSTEM);
        this.eventType = in.getEventType();
        this.value2 = in.getValue2();
        this.value3 = in.getValue3();
        this.value4 = in.getValue4();
    }

    /** {@inheritDoc} */
    @Override
    public byte getDataType() {
        return TYPE_PING;
    }

    /**
     * Returns the events sub-type
     * 
     * @return the event type
     */
    public short getEventType() {
        return eventType;
    }

    /**
     * Sets the events sub-type
     * 
     * @param eventType
     *            event type
     */
    public void setEventType(short eventType) {
        this.eventType = eventType;
    }

    /**
     * Getter for property 'value2'.
     *
     * @return Value for property 'value2'.
     */
    public Number getValue2() {
        return value2;
    }

    /**
     * Setter for property 'value2'.
     *
     * @param value2
     *            Value to set for property 'value2'.
     */
    public void setValue2(Number value2) {
        this.value2 = value2;
    }

    /**
     * Getter for property 'value3'.
     *
     * @return Value for property 'value3'.
     */
    public int getValue3() {
        return value3;
    }

    /**
     * Setter for property 'value3'.
     *
     * @param value3
     *            Value to set for property 'value3'.
     */
    public void setValue3(int value3) {
        this.value3 = value3;
    }

    /**
     * Getter for property 'value4'.
     *
     * @return Value for property 'value4'.
     */
    public int getValue4() {
        return value4;
    }

    /**
     * Setter for property 'value4'.
     *
     * @param value4
     *            Value to set for property 'value4'.
     */
    public void setValue4(int value4) {
        this.value4 = value4;
    }

    /**
     * Getter for property 'debug'.
     *
     * @return Value for property 'debug'.
     */
    public String getDebug() {
        return debug;
    }

    /**
     * Setter for property 'debug'.
     *
     * @param debug
     *            Value to set for property 'debug'.
     */
    public void setDebug(String debug) {
        this.debug = debug;
    }

    protected void doRelease() {
        eventType = 0;
        value2 = 0;
        value3 = UNDEFINED;
        value4 = UNDEFINED;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return String.format("Ping: %d, %f, %d, %d", eventType, value2.doubleValue(), value3, value4);
    }

    /** {@inheritDoc} */
    @Override
    protected void releaseInternal() {

    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);
        eventType = in.readShort();
        switch (eventType) {
            case PING_CLIENT:
            case PONG_SERVER:
                value2 = (Number) in.readInt();
                break;
            default:
                value2 = (Number) in.readDouble();
        }
        value3 = in.readInt();
        value4 = in.readInt();
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        super.writeExternal(out);
        out.writeShort(eventType);
        switch (eventType) {
            case PING_CLIENT:
            case PONG_SERVER:
                out.writeInt(value2.intValue());
                break;
            default:
                out.writeDouble(value2.doubleValue());
        }
        out.writeInt(value3);
        out.writeInt(value4);
    }
}
