/*
 * RED5 Open Source Media Server - https://github.com/Red5/ Copyright 2006-2016 by respective authors (see below). All rights reserved. Licensed under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless
 * required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package org.red5.io.utils;

import java.nio.ByteBuffer;
import java.util.UUID;

public class RandomGUID extends Object {

    private static final String hexChars = "0123456789ABCDEF";

    private RandomGUID() {
    }

    /**
     * Returns a byte array for the given uuid or guid.
     * 
     * @param uid
     *            unique id
     * @return array of bytes containing the id
     */
    public final static byte[] toByteArray(String uid) {
        byte[] result = new byte[16];
        char[] chars = uid.toCharArray();
        int r = 0;
        for (int i = 0; i < chars.length; ++i) {
            if (chars[i] == '-') {
                continue;
            }
            int h1 = Character.digit(chars[i], 16);
            ++i;
            int h2 = Character.digit(chars[i], 16);
            result[(r++)] = (byte) ((h1 << 4 | h2) & 0xFF);
        }
        return result;
    }

    /**
     * Returns a uuid / guid for a given byte array.
     * 
     * @param ba
     *            array of bytes containing the id
     * @return id
     */
    public static String fromByteArray(byte[] ba) {
        if ((ba != null) && (ba.length == 16)) {
            StringBuilder result = new StringBuilder(36);
            for (int i = 0; i < 16; ++i) {
                if ((i == 4) || (i == 6) || (i == 8) || (i == 10)) {
                    result.append('-');
                }
                result.append(hexChars.charAt(((ba[i] & 0xF0) >>> 4)));
                result.append(hexChars.charAt((ba[i] & 0xF)));
            }
            return result.toString();
        }
        return null;
    }

    /**
     * Returns a nice neat formatted string.
     * 
     * @param str
     *            unformatted string
     * @return formatted string
     */
    public static String getPrettyFormatted(String str) {
        return String.format("%s-%s-%s-%s-%s", new Object[] { str.substring(0, 8), str.substring(8, 12), str.substring(12, 16), str.substring(16, 20), str.substring(20) });
    }

    public static String create() {
        UUID id = UUID.randomUUID();

        byte[] bytes = ByteBuffer.allocate(16).putLong(id.getLeastSignificantBits()).putLong(id.getMostSignificantBits()).array();

        return fromByteArray(bytes);
    }
}
