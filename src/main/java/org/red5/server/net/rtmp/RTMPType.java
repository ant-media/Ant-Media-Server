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

/**
 * Enum for RTMP types.
 * 
 * @author Paul Gregoire (mondain@gmail.com)
 */
public enum RTMPType {

    /**
     * The specification refers to the following types by different names: 0x03 = Acknowledgement 0x04 = User control message 0x05 = Window Acknowledgement Size 0x06 = Set Peer Bandwidth 0x0f = AMF3 Data message 0x10 = AMF3 Shared object message 0x11 = AMF3 Command message 0x12 = AMF0 Data message 0x13 = AMF0 Shared object message 0x14 = AMF0 Command message
     * 
     * ------------------------------------------------------------------- RTMFP related (here for reference)
     * 
     * 0x30 Initiator hello 0x70 Responder hello 0x38 Initiator initial keying 0x78 Responder initial keying 0x0f Forwarded initiator hello 0x71 Forwarded hello response 0x10 Normal user data 0x11 Next user data 0x0c Session failed on client side 0x4c Session died 0x01 Causes response with 0x41, reset keep alive 0x41 Reset times keep alive 0x5e Negative ack 0x51 Some ack
     */
    TYPE_CHUNK_SIZE(0x01), TYPE_ABORT(0x02), TYPE_BYTES_READ(0x03), TYPE_PING(0x04), TYPE_SERVER_BANDWIDTH(0x05), TYPE_CLIENT_BANDWIDTH(0x06), TYPE_EDGE_ORIGIN(0x07), TYPE_AUDIO_DATA(0x08), TYPE_VIDEO_DATA(0x09), TYPE_UNK1(0x0a), TYPE_UNK2(0x0b), TYPE_UNK3(0x0c), TYPE_UNK4(0x0d), TYPE_UNK5(0x0e), TYPE_FLEX_STREAM_SEND(0x0f), TYPE_FLEX_SHARED_OBJECT(0x10), TYPE_FLEX_MESSAGE(0x11), TYPE_NOTIFY(0x12), TYPE_SHARED_OBJECT(
            0x13), TYPE_INVOKE(0x14), TYPE_UNK6(0x15), TYPE_AGGREGATE(0x16), TYPE_UNK7(0x17), TYPE_UNK8(0x18);

    private final byte type;

    RTMPType(byte type) {
        this.type = type;
    }

    RTMPType(int type) {
        this.type = (byte) type;
    }

    public byte getType() {
        return type;
    }

    public static String valueOf(byte dataType) {
        int idx = (int) dataType - 1;
        if (idx < RTMPType.values().length) {
            return RTMPType.values()[idx].name();
        } else {
            return "TYPE_UNKNOWN:" + dataType;
        }
    }

}
