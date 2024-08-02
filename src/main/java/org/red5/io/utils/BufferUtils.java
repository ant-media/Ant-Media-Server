/*
 * RED5 Open Source Media Server - https://github.com/Red5/ Copyright 2006-2016 by respective authors (see below). All rights reserved. Licensed under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless
 * required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package org.red5.io.utils;

import org.apache.mina.core.buffer.IoBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Buffer Utility class which reads/writes integers to the input/output buffer
 * 
 * @author The Red5 Project
 * @author Luke Hubbard, Codegent Ltd (luke@codegent.com)
 * @author Andy Shaules (bowljoman@hotmail.com)
 */
public class BufferUtils {

    private static Logger log = LoggerFactory.getLogger(BufferUtils.class);

    /**
     * Writes a Medium Int to the output buffer
     * 
     * @param out
     *            Container to write to
     * @param value
     *            Integer to write
     */
    public static void writeMediumInt(IoBuffer out, int value) {
        byte[] bytes = new byte[3];
        bytes[0] = (byte) ((value >>> 16) & 0x000000FF);
        bytes[1] = (byte) ((value >>> 8) & 0x000000FF);
        bytes[2] = (byte) (value & 0x00FF);
        out.put(bytes);
    }

    /**
     * Reads an unsigned Medium Int from the in buffer
     * 
     * @param in
     *            Source
     * @return int Integer value
     */
    public static int readUnsignedMediumInt(IoBuffer in) {
        byte[] bytes = new byte[3];
        in.get(bytes);
        int val = 0;
        val += (bytes[0] & 0xFF) * 256 * 256;
        val += (bytes[1] & 0xFF) * 256;
        val += (bytes[2] & 0xFF);
        return val;
    }

    /**
     * Reads a Medium Int to the in buffer
     * 
     * @param in
     *            Source
     * @return int Medium int
     */
    public static int readMediumInt(IoBuffer in) {
        byte[] bytes = new byte[3];
        in.get(bytes);
        int val = 0;
        val += bytes[0] * 256 * 256;
        val += bytes[1] * 256;
        val += bytes[2];
        if (val < 0) {
            val += 256;
        }
        return val;
    }

    /**
     * Puts an input buffer in an output buffer and returns number of bytes written.
     * 
     * @param out
     *            Output buffer
     * @param in
     *            Input buffer
     * @param numBytesMax
     *            Number of bytes max
     * @return int Number of bytes written
     */
    public final static int put(IoBuffer out, IoBuffer in, int numBytesMax) {
        if (log.isTraceEnabled()) {
            log.trace("Put\nin buffer: {}\nout buffer: {}\nmax bytes: {}", new Object[] { out, in, numBytesMax });
        }
        int numBytesRead = 0;
        if (in != null) {
            int limit = Math.min(in.limit(), numBytesMax);
            byte[] inBuf = new byte[limit];
            log.trace("Bulk get size: {}", limit);
            in.get(inBuf);
            byte[] outBuf = consumeBytes(inBuf, numBytesMax);
            out.put(outBuf);
            numBytesRead = outBuf.length;
            log.trace("In pos: {}", in.position());
        }
        log.trace("Bytes put: {}", numBytesRead);
        return numBytesRead;
    }

    /**
     * Consumes bytes from an input buffer and returns them in an output buffer.
     * 
     * @param in
     *            Input byte array
     * @param numBytesMax
     *            Number of bytes max
     * @return out Output byte array
     */
    public final static byte[] consumeBytes(byte[] in, int numBytesMax) {
        int limit = Math.min(in.length, numBytesMax);
        byte[] out = new byte[limit];
        System.arraycopy(in, 0, out, 0, limit);
        return out;
    }

}
