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

/**
 * Class for AMF and RTMP marker values constants
 */
public interface Constants {

    /**
     * Data originated from a file.
     */
    public static final byte SOURCE_TYPE_VOD = 0x0;

    /**
     * Data originated from a live encoder or stream.
     */
    public static final byte SOURCE_TYPE_LIVE = 0x01;

    /**
     * Medium integer max value
     */
    public static final int MEDIUM_INT_MAX = 16777215;

    /**
     * RTMP chunk size constant
     */
    public static final byte TYPE_CHUNK_SIZE = 0x01;

    /**
     * Abort message
     */
    public static final byte TYPE_ABORT = 0x02;

    /**
     * Acknowledgment. Send every x bytes read by both sides.
     */
    public static final byte TYPE_BYTES_READ = 0x03;

    /**
     * Ping is a stream control message, it has sub-types
     */
    public static final byte TYPE_PING = 0x04;

    /**
     * Server (downstream) bandwidth marker
     */
    public static final byte TYPE_SERVER_BANDWIDTH = 0x05;

    /**
     * Client (upstream) bandwidth marker
     */
    public static final byte TYPE_CLIENT_BANDWIDTH = 0x06;

    /**
     * Edge / Origin message.
     */
    public static final byte TYPE_EDGE_ORIGIN = 0x07;

    /**
     * Audio data marker
     */
    public static final byte TYPE_AUDIO_DATA = 0x08;

    /**
     * Video data marker
     */
    public static final byte TYPE_VIDEO_DATA = 0x09;

    // Unknown: 0x0A ...  0x0E

    /**
     * AMF3 stream send
     */
    public static final byte TYPE_FLEX_STREAM_SEND = 0x0F;

    /**
     * AMF3 shared object
     */
    public static final byte TYPE_FLEX_SHARED_OBJECT = 0x10;

    /**
     * AMF3 message
     */
    public static final byte TYPE_FLEX_MESSAGE = 0x11;

    /**
     * Notification is invocation without response
     */
    public static final byte TYPE_NOTIFY = 0x12;

    /**
     * Stream metadata
     */
    public static final byte TYPE_STREAM_METADATA = 0x12;

    /**
     * Shared Object marker
     */
    public static final byte TYPE_SHARED_OBJECT = 0x13;

    /**
     * Invoke operation (remoting call but also used for streaming) marker
     */
    public static final byte TYPE_INVOKE = 0x14;

    /**
     * Aggregate data marker
     */
    public static final byte TYPE_AGGREGATE = 0x16;

    /**
     * New header marker
     */
    public static final byte HEADER_NEW = 0x00;

    /**
     * Same source marker
     */
    public static final byte HEADER_SAME_SOURCE = 0x01;

    /**
     * Timer change marker
     */
    public static final byte HEADER_TIMER_CHANGE = 0x02;

    /**
     * There's more to encode
     */
    public static final byte HEADER_CONTINUE = 0x03;

    /**
     * Size of initial handshake between client and server
     */
    public static final int HANDSHAKE_SIZE = 1536;

    /**
     * Client Shared Object data update
     */
    public static final byte SO_CLIENT_UPDATE_DATA = 0x04; //update data

    /**
     * Client Shared Object attribute update
     */
    public static final byte SO_CLIENT_UPDATE_ATTRIBUTE = 0x05; //5: update attribute

    /**
     * Send SO message flag
     */
    public static final byte SO_CLIENT_SEND_MESSAGE = 0x06; // 6: send message

    /**
     * Shared Object status marker
     */
    public static final byte SO_CLIENT_STATUS = 0x07; // 7: status (usually returned with error messages)

    /**
     * Clear event for Shared Object
     */
    public static final byte SO_CLIENT_CLEAR_DATA = 0x08; // 8: clear data

    /**
     * Delete data for Shared Object
     */
    public static final byte SO_CLIENT_DELETE_DATA = 0x09; // 9: delete data

    /**
     * Initial SO data flag
     */
    public static final byte SO_CLIENT_INITIAL_DATA = 0x0B; // 11: initial data

    /**
     * Shared Object connection
     */
    public static final byte SO_CONNECT = 0x01;

    /**
     * Shared Object disconnection
     */
    public static final byte SO_DISCONNECT = 0x02;

    /**
     * Set Shared Object attribute flag
     */
    public static final byte SO_SET_ATTRIBUTE = 0x03;

    /**
     * Send message flag
     */
    public static final byte SO_SEND_MESSAGE = 0x06;

    /**
     * Shared Object attribute deletion flag
     */
    public static final byte SO_DELETE_ATTRIBUTE = 0x0A;

}
