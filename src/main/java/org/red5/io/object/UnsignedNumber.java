/*
 * RED5 Open Source Media Server - https://github.com/Red5/ Copyright 2006-2016 by respective authors (see below). All rights reserved. Licensed under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless
 * required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package org.red5.io.object;

/**
 * @author Matteo Merli (matteo.merli@gmail.com)
 */
public abstract class UnsignedNumber extends Number {

    private static final long serialVersionUID = -6404256963187584919L;

    /**
     * Get a byte array representation of the number. The order will be MSB first (Big Endian).
     * 
     * @return the serialized number
     */
    public abstract byte[] getBytes();

    /**
     * Perform a bit right shift of the value.
     * 
     * @param nBits
     *            the number of positions to shift
     */
    public abstract void shiftRight(int nBits);

    /**
     * Perform a bit left shift of the value.
     * 
     * @param nBits
     *            the number of positions to shift
     */
    public abstract void shiftLeft(int nBits);

    @Override
    public abstract String toString();

    public abstract int compareTo(UnsignedNumber other);

    @Override
    public abstract boolean equals(Object other);

    @Override
    public abstract int hashCode();

    public String toHexString() {
        return toHexString(false);
    }

    public String toHexString(boolean pad) {
        StringBuilder sb = new StringBuilder();
        boolean started = false;
        for (byte b : getBytes()) {
            if (!started && b == 0) {
                if (pad) {
                    sb.append("00");
                }
            } else {
                sb.append(hexLetters[(byte) ((b >> 4) & 0x0F)]).append(hexLetters[b & 0x0F]);
                started = true;
            }
        }
        if (sb.length() == 0) {
            return "0";
        }
        return sb.toString();
    }

    protected static final char[] hexLetters = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };
}
