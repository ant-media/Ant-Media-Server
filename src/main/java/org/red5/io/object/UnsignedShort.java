/*
 * RED5 Open Source Media Server - https://github.com/Red5/ Copyright 2006-2016 by respective authors (see below). All rights reserved. Licensed under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless
 * required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package org.red5.io.object;

import java.util.Arrays;

/**
 * The UnsignedByte class wraps a value of an unsigned 16 bits number.
 * 
 * @author Matteo Merli (matteo.merli@gmail.com)
 */
public final class UnsignedShort extends UnsignedNumber {
    static final long serialVersionUID = 1L;

    private int value;

    public UnsignedShort(byte c) {
        value = c;
    }

    public UnsignedShort(short c) {
        value = c;
    }

    public UnsignedShort(int c) {
        value = c & 0xFFFF;
    }

    public UnsignedShort(long c) {
        value = (int) (c & 0xFFFFL);
    }

    private UnsignedShort() {
        value = 0;
    }

    public static UnsignedShort fromBytes(byte[] c) {
        return fromBytes(c, 0);
    }

    public static UnsignedShort fromBytes(byte[] c, int idx) {
        UnsignedShort number = new UnsignedShort();
        if ((c.length - idx) < 2) {
            throw new IllegalArgumentException("An UnsignedShort number is composed of 2 bytes.");
        }
        number.value = ((c[0] << 8) | (c[1] & 0xFFFF));
        return number;
    }

    public static UnsignedShort fromString(String c) {
        return fromString(c, 10);
    }

    public static UnsignedShort fromString(String c, int radix) {
        UnsignedShort number = new UnsignedShort();
        long v = Integer.parseInt(c, radix);
        number.value = (int) (v & 0xFFFF);
        return number;
    }

    @Override
    public double doubleValue() {
        return value;
    }

    @Override
    public float floatValue() {
        return value;
    }

    @Override
    public short shortValue() {
        return (short) (value & 0xFFFF);
    }

    @Override
    public int intValue() {
        return value & 0xFFFF;
    }

    @Override
    public long longValue() {
        return value & 0xFFFFL;
    }

    @Override
    public byte[] getBytes() {
        return new byte[] { (byte) ((value >> 8) & 0xFF), (byte) (value & 0xFF) };
    }

    @Override
    public int compareTo(UnsignedNumber other) {
        int otherValue = other.intValue();
        if (value > otherValue) {
            return 1;
        } else if (value < otherValue) {
            return -1;
        }
        return 0;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof Number) {
            return Arrays.equals(getBytes(), ((UnsignedNumber) other).getBytes());
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return value;
    }

    @Override
    public String toString() {
        return Integer.toString(value);
    }

    @Override
    public void shiftRight(int nBits) {
        if (Math.abs(nBits) > 16) {
            throw new IllegalArgumentException("Cannot right shift " + nBits + " an UnsignedShort.");
        }
        value >>>= nBits;
    }

    @Override
    public void shiftLeft(int nBits) {
        if (Math.abs(nBits) > 16) {
            throw new IllegalArgumentException("Cannot left shift " + nBits + " an UnsignedShort.");
        }
        value <<= nBits;
    }

}
