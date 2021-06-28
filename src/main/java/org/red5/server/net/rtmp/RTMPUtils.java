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

import org.apache.mina.core.buffer.IoBuffer;
import org.red5.server.net.rtmp.message.Constants;

/**
 * RTMP utilities class.
 *
 * @author The Red5 Project
 * @author Luke Hubbard, Codegent Ltd (luke@codegent.com)
 * @author Art Clarke (aclarke@xuggle.com)
 */
public class RTMPUtils implements Constants {

    /**
     * Writes reversed integer to buffer.
     *
     * @param out
     *            Buffer
     * @param value
     *            Integer to write
     */
    public static void writeReverseIntOld(IoBuffer out, int value) {
        byte[] bytes = new byte[4];
        IoBuffer rev = IoBuffer.allocate(4);
        rev.putInt(value);
        rev.flip();
        bytes[3] = rev.get();
        bytes[2] = rev.get();
        bytes[1] = rev.get();
        bytes[0] = rev.get();
        out.put(bytes);
        rev.free();
        rev = null;
    }

    /**
     * Writes reversed integer to buffer.
     *
     * @param out
     *            Buffer
     * @param value
     *            Integer to write
     */
    public static void writeReverseInt(IoBuffer out, int value) {
        out.put((byte) (0xFF & value));
        out.put((byte) (0xFF & (value >> 8)));
        out.put((byte) (0xFF & (value >> 16)));
        out.put((byte) (0xFF & (value >> 24)));
    }

    /**
     *
     * @param out
     *            output buffer
     * @param value
     *            value to write
     */
    public static void writeMediumInt(IoBuffer out, int value) {
        out.put((byte) (0xFF & (value >> 16)));
        out.put((byte) (0xFF & (value >> 8)));
        out.put((byte) (0xFF & (value >> 0)));
    }

    /**
     *
     * @param in
     *            input
     * @return unsigned int
     */
    public static int readUnsignedMediumInt(IoBuffer in) {
        final byte a = in.get();
        final byte b = in.get();
        final byte c = in.get();
        int val = 0;
        val += (a & 0xff) << 16;
        val += (b & 0xff) << 8;
        val += (c & 0xff);
        return val;
    }

    /**
     *
     * @param in
     *            input
     * @return unsigned medium (3 byte) int.
     */
    public static int readUnsignedMediumIntOld(IoBuffer in) {
        byte[] bytes = new byte[3];
        in.get(bytes);
        int val = 0;
        val += (bytes[0] & 0xFF) * 256 * 256;
        val += (bytes[1] & 0xFF) * 256;
        val += (bytes[2] & 0xFF);
        return val;
    }

    /**
     *
     * @param in
     *            input
     * @return signed 3-byte int
     */
    public static int readMediumIntOld(IoBuffer in) {
        IoBuffer buf = IoBuffer.allocate(4);
        buf.put((byte) 0x00);
        buf.put(in.get());
        buf.put(in.get());
        buf.put(in.get());
        buf.flip();
        int value = buf.getInt();
        buf.free();
        buf = null;
        return value;
    }

    /**
     *
     * @param in
     *            input
     * @return signed 3 byte int
     */
    public static int readMediumInt(IoBuffer in) {
        final byte a = in.get();
        final byte b = in.get();
        final byte c = in.get();
        // Fix unsigned values
        int val = 0;
        val += (a & 0xff) << 16;
        val += (b & 0xff) << 8;
        val += (c & 0xff);
        return val;
    }

    /**
     * Read integer in reversed order.
     *
     * @param in
     *            Input buffer
     * @return Integer
     */
    public static int readReverseInt(IoBuffer in) {
        final byte a = in.get();
        final byte b = in.get();
        final byte c = in.get();
        final byte d = in.get();
        int val = 0;
        val += (d & 0xff) << 24;
        val += (c & 0xff) << 16;
        val += (b & 0xff) << 8;
        val += (a & 0xff);
        return val;
    }

    /**
     * Encodes header size marker and channel id into header marker.
     * 
     * @param out
     *            output buffer
     *
     * @param headerSize
     *            Header size marker
     * @param channelId
     *            Channel used
     */
    public static void encodeHeaderByte(IoBuffer out, byte headerSize, int channelId) {
        if (channelId <= 63) {
            out.put((byte) ((headerSize << 6) + channelId));
        } else if (channelId <= 319) {
            out.put((byte) (headerSize << 6));
            out.put((byte) (channelId - 64));
        } else {
            out.put((byte) ((headerSize << 6) | 1));
            channelId -= 64;
            out.put((byte) (channelId & 0xff));
            out.put((byte) (channelId >> 8));
        }
    }

    /**
     * Decode channel id.
     *
     * @param header
     *            Header
     * @param byteCount
     *            byte count
     * @return Channel id
     */
    public static int decodeChannelId(int header, int byteCount) {
        if (byteCount == 1) {
            return (header & 0x3f);
        } else if (byteCount == 2) {
            return 64 + (header & 0xff);
        } else {
            return 64 + ((header >> 8) & 0xff) + ((header & 0xff) << 8);
        }
    }

    /**
     * Decode header size.
     *
     * @param header
     *            Header byte
     * @param byteCount
     *            byte count
     * @return Header size byte
     */
    public static byte decodeHeaderSize(int header, int byteCount) {
        if (byteCount == 1) {
            return (byte) (header >> 6);
        } else if (byteCount == 2) {
            return (byte) (header >> 14);
        } else {
            return (byte) (header >> 22);
        }
    }

    /**
     * Return header length from marker value.
     *
     * @param headerSize
     *            Header size marker value
     * @return Header length
     */
    public static int getHeaderLength(byte headerSize) {
        switch (headerSize) {
            case HEADER_NEW:
                return 12;
            case HEADER_SAME_SOURCE:
                return 8;
            case HEADER_TIMER_CHANGE:
                return 4;
            case HEADER_CONTINUE:
                return 1;
            default:
                return -1;
        }
    }

    /**
     * Compares two RTMP time stamps, accounting for time stamp wrapping.
     * 
     * @param a
     *            First time stamp
     * @param b
     *            Second time stamp
     * @return -1 if a &lt; b, 1 if a &gt; b, or 0 if a == b
     */
    public static int compareTimestamps(final int a, final int b) {
        long diff = diffTimestamps(a, b);
        return diff < 0 ? -1 : (diff > 0 ? 1 : 0);
    }

    /**
     * Calculates the delta between two time stamps, adjusting for time stamp wrapping.
     * 
     * @param a
     *            First time stamp
     * @param b
     *            Second time stamp
     * @return the distance between a and b, which will be negative if a is less than b.
     */
    public static long diffTimestamps(final int a, final int b) {
        // first convert each to unsigned integers
        final long unsignedA = a & 0xFFFFFFFFL;
        final long unsignedB = b & 0xFFFFFFFFL;
        // then find the delta
        final long delta = unsignedA - unsignedB;
        return delta;
    }

}
