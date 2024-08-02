/*
 * RED5 Open Source Media Server - https://github.com/Red5/ Copyright 2006-2016 by respective authors (see below). All rights reserved. Licensed under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless
 * required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package org.red5.io.object;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Random;

/**
 * The UnsignedLong class wraps a value of an unsigned 64 bits number.
 * 
 * @author Matteo Merli (matteo.merli@gmail.com)
 */
public final class UnsignedLong extends UnsignedNumber {

    private static final long serialVersionUID = 1L;

    private byte[] value = new byte[8];

    public UnsignedLong(byte c) {
        Arrays.fill(value, (byte) 0);
        value[7] = c;
    }

    public UnsignedLong(short c) {
        Arrays.fill(value, (byte) 0);
        value[6] = (byte) ((c >> 8) & 0xFF);
        value[7] = (byte) (c & 0xFF);
    }

    public UnsignedLong(int c) {
        Arrays.fill(value, (byte) 0);
        value[4] = (byte) ((c >> 24) & 0xFF);
        value[5] = (byte) ((c >> 16) & 0xFF);
        value[6] = (byte) ((c >> 8) & 0xFF);
        value[7] = (byte) (c & 0xFF);
    }

    public UnsignedLong(long c) {
        value[0] = (byte) ((c >> 56) & 0xFF);
        value[1] = (byte) ((c >> 48) & 0xFF);
        value[2] = (byte) ((c >> 40) & 0xFF);
        value[3] = (byte) ((c >> 32) & 0xFF);
        value[4] = (byte) ((c >> 24) & 0xFF);
        value[5] = (byte) ((c >> 16) & 0xFF);
        value[6] = (byte) ((c >> 8) & 0xFF);
        value[7] = (byte) (c & 0xFF);
    }

    /**
     * Construct a new random UnsignedLong.
     * 
     * @param random
     *            a Random handler
     */
    public UnsignedLong(Random random) {
        random.nextBytes(value);
    }

    private UnsignedLong() {
        Arrays.fill(value, (byte) 0);
    }

    public static UnsignedLong fromBytes(byte[] c) {
        return fromBytes(c, 0);
    }

    public static UnsignedLong fromBytes(byte[] c, int offset) {
        UnsignedLong number = new UnsignedLong();
        if ((c.length - offset) < 8)
            throw new IllegalArgumentException("An UnsignedLong number is composed of 8 bytes.");

        for (int i = 7; i >= 0; i--)
            number.value[i] = c[offset + i];
        return number;
    }

    public static UnsignedLong fromString(String c) {
        return fromString(c, 10);
    }

    public static UnsignedLong fromString(String c, int radix) {
        UnsignedLong number = new UnsignedLong();

        BigInteger n = new BigInteger(c, radix);
        byte[] bytes = n.toByteArray();

        int len = Math.min(8, bytes.length);
        for (int i = 0; i < len; i++)
            number.value[7 - i] = bytes[bytes.length - 1 - i];
        return number;
    }

    @Override
    public byte[] getBytes() {
        return value;
    }

    @Override
    public String toString() {
        if ((byte) ((value[0] >> 7) & 0x01) == 1) {
            value[0] = (byte) (value[0] & 0x7F);
            BigInteger n = new BigInteger(value);
            n = n.setBit(63);
            value[0] = (byte) (value[0] | 0x80);
            return n.toString();
        } else {
            BigInteger n = new BigInteger(value);
            return n.toString();
        }
    }

    @Override
    public int intValue() {
        return ((value[4] << 24) & 0xFF000000 | (value[5] << 16) & 0xFF0000 | (value[6] << 8) & 0xFF00 | (value[7] & 0xFF));
    }

    @Override
    public long longValue() {
        return (((long) value[0] << 56) & 0xFF00000000000000L | ((long) value[1] << 48) & 0xFF000000000000L | ((long) value[2] << 40) & 0xFF0000000000L | ((long) value[3] << 32) & 0xFF00000000L | ((long) value[4] << 24) & 0xFF000000L | ((long) value[5] << 16) & 0xFF0000L | ((long) value[6] << 8) & 0xFF00L | ((value[7]) & 0xFFL));
    }

    @Override
    public float floatValue() {
        return longValue();
    }

    @Override
    public double doubleValue() {
        return longValue();
    }

    @Override
    public int compareTo(UnsignedNumber other) {
        byte[] otherValue = other.getBytes();
        if (otherValue.length > 8)
            throw new IllegalArgumentException("The number is more than 8 bytes.");

        byte[] normalValue = new byte[8];
        Arrays.fill(normalValue, (byte) 0);
        for (int i = 1; i <= otherValue.length; i++) {
            normalValue[8 - i] = otherValue[otherValue.length - i];
        }

        for (int i = 0; i < 8; i++)
            if ((char) value[i] > (char) normalValue[i])
                return +1;
            else if ((char) value[i] < (char) normalValue[i])
                return -1;
        return 0;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof UnsignedLong) {
            // this is a special case
            byte[] bytes = ((UnsignedLong) other).getBytes();
            for (int i = 7; i >= 0; i--)
                if (value[i] != bytes[i])
                    return false;
            return true;
        } else if (other instanceof Number)
            return longValue() == ((Number) other).longValue();
        else
            return false;
    }

    @Override
    public int hashCode() {
        int hashCode = 0;

        for (int i = 0; i < value.length; i++)
            hashCode = (int) (31 * hashCode + (value[i] & 0xFFFFFFFFL));

        return hashCode;
    }

    @Override
    public void shiftRight(int nBits) {
        if (nBits > 64 || nBits < 0)
            throw new IllegalArgumentException("Cannot right shift " + nBits + " an UnsignedLong.");
        if (nBits % 8 != 0)
            throw new IllegalArgumentException("nBits must be a multiple of 8.");

        int nBytes = nBits / 8;
        for (int i = 7; i >= nBytes; i--)
            value[i] = value[i - nBytes];
        for (int i = nBytes - 1; i >= 0; i--)
            value[i] = 0;
    }

    @Override
    public void shiftLeft(int nBits) {
        if (nBits > 64 || nBits < 0)
            throw new IllegalArgumentException("Cannot left shift " + nBits + " an UnsignedLong.");
        if (nBits % 8 != 0)
            throw new IllegalArgumentException("nBits must be a multiple of 8.");

        int nBytes = nBits / 8;
        for (int i = 0; i <= 7 - nBytes; i++) {
            value[i] = value[i + nBytes];
        }
        for (int i = 8 - nBytes; i < 8; i++) {
            value[i] = 0;
        }
    }
}
