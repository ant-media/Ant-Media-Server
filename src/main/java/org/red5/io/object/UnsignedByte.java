/*
 * RED5 Open Source Media Server - https://github.com/Red5/ Copyright 2006-2016 by respective authors (see below). All rights reserved. Licensed under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless
 * required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package org.red5.io.object;

/**
 * The UnsignedByte class wraps a value of and unsigned 8 bits number.
 * 
 * @author Matteo Merli (matteo.merli@gmail.com)
 */
public final class UnsignedByte extends UnsignedNumber {
    static final long serialVersionUID = 1L;

    private short value;

    public UnsignedByte(byte c) {
        value = c;
    }

    public UnsignedByte(short c) {
        value = (short) (c & 0xFF);
    }

    public UnsignedByte(int c) {
        value = (short) (c & 0xFF);
    }

    public UnsignedByte(long c) {
        value = (short) (c & 0xFFL);
    }

    private UnsignedByte() {
        value = 0;
    }

    public static UnsignedByte fromBytes(byte[] c) {
        return fromBytes(c, 0);
    }

    public static UnsignedByte fromBytes(byte[] c, int idx) {
        UnsignedByte number = new UnsignedByte();
        if ((c.length - idx) < 1)
            throw new IllegalArgumentException("An UnsignedByte number is composed of 1 byte.");

        number.value = (short) (c[0] & 0xFF);
        return number;
    }

    public static UnsignedByte fromString(String c) {
        return fromString(c, 10);
    }

    public static UnsignedByte fromString(String c, int radix) {
        UnsignedByte number = new UnsignedByte();

        short v = Short.parseShort(c, radix);
        number.value = (short) (v & 0xFF);
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
        return (short) (value & 0xFF);
    }

    @Override
    public int intValue() {
        return value & 0xFF;
    }

    @Override
    public long longValue() {
        return value & 0xFFL;
    }

    @Override
    public byte[] getBytes() {
        byte[] c = { (byte) (value & 0xFF) };
        return c;
    }

    @Override
    public int compareTo(UnsignedNumber other) {
        short otherValue = other.shortValue();
        if (value > otherValue) {
            return +1;
        } else if (value < otherValue) {
            return -1;
        }
        return 0;
    }

    @Override
    public boolean equals(Object other) {
        if (other != null && other instanceof Number) {
            return value == ((Number) other).shortValue();
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
        return Short.toString(value);
    }

    @Override
    public void shiftRight(int nBits) {
        if (Math.abs(nBits) > 8) {
            throw new IllegalArgumentException("Cannot right shift " + nBits + " an UnsignedByte.");
        }
        value >>>= nBits;
    }

    @Override
    public void shiftLeft(int nBits) {
        if (Math.abs(nBits) > 8) {
            throw new IllegalArgumentException("Cannot left shift " + nBits + " an UnsignedByte.");
        }
        value <<= nBits;
    }
}
