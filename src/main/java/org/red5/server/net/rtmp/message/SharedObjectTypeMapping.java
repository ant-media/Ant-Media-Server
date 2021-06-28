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

import org.red5.server.so.ISharedObjectEvent.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SO event types mapping
 */
public class SharedObjectTypeMapping {

    protected static Logger log = LoggerFactory.getLogger(SharedObjectTypeMapping.class);

    /**
     * Types map
     */
    public static final Type[] typeMap = new Type[] { null, Type.SERVER_CONNECT, // 01
            Type.SERVER_DISCONNECT, // 02
            Type.SERVER_SET_ATTRIBUTE, // 03
            Type.CLIENT_UPDATE_DATA, // 04 
            Type.CLIENT_UPDATE_ATTRIBUTE, // 05
            Type.SERVER_SEND_MESSAGE, // 06
            Type.CLIENT_STATUS, // 07
            Type.CLIENT_CLEAR_DATA, // 08
            Type.CLIENT_DELETE_DATA, // 09
            Type.SERVER_DELETE_ATTRIBUTE, // 0A
            Type.CLIENT_INITIAL_DATA, // 0B
    };

    /**
     * Convert byte value of RTMP marker to event type
     * 
     * @param rtmpType
     *            RTMP marker value
     * @return Corresponding Shared Object event type
     */
    public static Type toType(byte rtmpType) {
        return typeMap[rtmpType];
    }

    /**
     * Convert SO event type to byte representation that RTMP uses
     * 
     * @param type
     *            Event type
     * @return Byte representation of given event type
     */
    public static byte toByte(Type type) {
        switch (type) {
            case SERVER_CONNECT:
                return 0x01;
            case SERVER_DISCONNECT:
                return 0x02;
            case SERVER_SET_ATTRIBUTE:
                return 0x03;
            case CLIENT_UPDATE_DATA:
                return 0x04;
            case CLIENT_UPDATE_ATTRIBUTE:
                return 0x05;
            case SERVER_SEND_MESSAGE:
                return 0x06;
            case CLIENT_SEND_MESSAGE:
                return 0x06;
            case CLIENT_STATUS:
                return 0x07;
            case CLIENT_CLEAR_DATA:
                return 0x08;
            case CLIENT_DELETE_DATA:
                return 0x09;
            case SERVER_DELETE_ATTRIBUTE:
                return 0x0A;
            case CLIENT_INITIAL_DATA:
                return 0x0B;
            default:
                log.error("Unknown type " + type);
                return 0x00;
        }
    }

    /**
     * String representation of type
     * 
     * @param type
     *            Type
     * @return String representation of type
     */
    public static String toString(Type type) {
        switch (type) {
            case SERVER_CONNECT:
                return "server connect";
            case SERVER_DISCONNECT:
                return "server disconnect";
            case SERVER_SET_ATTRIBUTE:
                return "server_set_attribute";
            case CLIENT_UPDATE_DATA:
                return "client_update_data";
            case CLIENT_UPDATE_ATTRIBUTE:
                return "client_update_attribute";
            case SERVER_SEND_MESSAGE:
                return "server_send_message";
            case CLIENT_SEND_MESSAGE:
                return "client_send_message";
            case CLIENT_STATUS:
                return "client_status";
            case CLIENT_CLEAR_DATA:
                return "client_clear_data";
            case CLIENT_DELETE_DATA:
                return "client_delete_data";
            case SERVER_DELETE_ATTRIBUTE:
                return "server_delete_attribute";
            case CLIENT_INITIAL_DATA:
                return "client_initial_data";
            default:
                log.error("Unknown type " + type);
                return "unknown";
        }
    }

}