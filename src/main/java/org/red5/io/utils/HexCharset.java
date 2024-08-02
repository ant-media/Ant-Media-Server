/*
 * RED5 Open Source Media Server - https://github.com/Red5/ Copyright 2006-2016 by respective authors (see below). All rights reserved. Licensed under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless
 * required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package org.red5.io.utils;

import java.nio.Buffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;

/**
 * This was borrowed from the Soupdragon base64 library.
 *
 * <p>
 * Codec to translate between hex coding and byte string.
 * </p>
 * <p>
 * Hex output is capital if the char set name is given in capitals.
 * </p>
 * <p>
 * hex:nn used as a charset name inserts \n after every nnth character.
 * </p>
 * 
 * @author Malcolm McMahon
 */
public class HexCharset extends Charset {

    private final static String codeHEX = "0123456789ABCDEF";

    private final static String codehex = "0123456789abcdef";

    private String codes;

    private Integer measure;

    /**
     * Creates a new instance of HexCharset
     * 
     * @param caps
     *            true for A-F, false for a-f
     */
    public HexCharset(boolean caps) {
        super(caps ? "HEX" : "hex", new String[] { "HEX" });
        codes = caps ? codeHEX : codehex;
    }

    /**
     * Construct the charset
     * 
     * @param caps
     *            true for A-F, false for a-f
     * @param measure
     *            Line width for decoding
     */
    public HexCharset(boolean caps, int measure) {
        super((caps ? "HEX" : "hex") + ":" + measure, new String[] { "HEX" });
        codes = caps ? codeHEX : codehex;
        this.measure = measure;
    }

    /**
     * Constructs a new encoder for this charset.
     * 
     * @return A new encoder for this charset
     */
    @Override
    public CharsetEncoder newEncoder() {
        return new Encoder();
    }

    /**
     * Constructs a new decoder for this charset.
     * 
     * @return A new decoder for this charset
     */
    @Override
    public CharsetDecoder newDecoder() {
        return new Decoder();
    }

    /**
     * Tells whether or not this charset contains the given charset.
     * 
     * <p>
     * A charset <i>C</i> is said to <i>contain</i> a charset <i>D</i> if, and only if, every character representable in <i>D</i> is also representable in <i>C</i>. If this relationship holds then it is guaranteed that every string that can be encoded in <i>D</i> can also be encoded in <i>C</i> without performing any replacements.
     * 
     * <p>
     * That <i>C</i> contains <i>D</i> does not imply that each character representable in <i>C</i> by a particular byte sequence is represented in <i>D</i> by the same byte sequence, although sometimes this is the case.
     * 
     * <p>
     * Every charset contains itself.
     * 
     * <p>
     * This method computes an approximation of the containment relation: If it returns true then the given charset is known to be contained by this charset; if it returns false, however, then it is not necessarily the case that the given charset is not contained in this charset.
     * 
     * @return true if, and only if, the given charset is contained in this charset
     */
    @Override
    public boolean contains(Charset cs) {
        return cs instanceof HexCharset;
    }

    private class Encoder extends CharsetEncoder {
        private boolean unpaired;

        private int nyble;

        private Encoder() {
            super(HexCharset.this, 0.49f, 1f);
        }

        /**
         * Flushes this encoder.
         * 
         * <p>
         * The default implementation of this method does nothing, and always returns {@link CoderResult#UNDERFLOW}. This method should be overridden by encoders that may need to write final bytes to the output buffer once the entire input sequence has been read.
         * </p>
         * 
         * @param out
         *            The output byte buffer
         * 
         * @return A coder-result object, either {@link CoderResult#UNDERFLOW} or {@link CoderResult#OVERFLOW}
         */
        @Override
        protected java.nio.charset.CoderResult implFlush(java.nio.ByteBuffer out) {
            if (!unpaired) {
                implReset();
                return CoderResult.UNDERFLOW;
            } else
                throw new IllegalArgumentException("Hex string must be an even number of digits");
        }

        /**
         * Encodes one or more characters into one or more bytes.
         * 
         * <p>
         * This method encapsulates the basic encoding loop, encoding as many characters as possible until it either runs out of input, runs out of room in the output buffer, or encounters an encoding error. This method is invoked by the {@link #encode encode} method, which handles result interpretation and error recovery.
         * 
         * <p>
         * The buffers are read from, and written to, starting at their current positions. At most {@link Buffer#remaining in.remaining()} characters will be read, and at most {@link Buffer#remaining out.remaining()} bytes will be written. The buffers' positions will be advanced to reflect the characters read and the bytes written, but their marks and limits will not be modified.
         * 
         * <p>
         * This method returns a {@link CoderResult} object to describe its reason for termination, in the same manner as the {@link #encode encode} method. Most implementations of this method will handle encoding errors by returning an appropriate result object for interpretation by the {@link #encode encode} method. An optimized implementation may instead examine the relevant error action and implement that action
         * itself.
         * 
         * <p>
         * An implementation of this method may perform arbitrary lookahead by returning {@link CoderResult#UNDERFLOW} until it receives sufficient input.
         * </p>
         * 
         * @param in
         *            The input character buffer
         * 
         * @param out
         *            The output byte buffer
         * 
         * @return A coder-result object describing the reason for termination
         */
        @Override
        public java.nio.charset.CoderResult encodeLoop(java.nio.CharBuffer in, java.nio.ByteBuffer out) {
            while (in.remaining() > 0) {
                if (out.remaining() <= 0)
                    return CoderResult.OVERFLOW;
                char inch = in.get();
                if (!Character.isWhitespace(inch)) {
                    int d = Character.digit(inch, 16);
                    if (d < 0)
                        throw new IllegalArgumentException("Bad hex character " + inch);
                    if (unpaired)
                        out.put((byte) (nyble | d));
                    else
                        nyble = d << 4;
                    unpaired = !unpaired;
                }
            }
            return CoderResult.UNDERFLOW;
        }

        /**
         * Clear state
         */

        @Override
        protected void implReset() {
            unpaired = false;
            nyble = 0;
        }

    }

    private class Decoder extends CharsetDecoder {
        private int charCount;

        private Decoder() {
            super(HexCharset.this, 2f, measure == null ? 2f : 2f + (2f / (float) measure));
        }

        /**
         * Decodes one or more bytes into one or more characters.
         * 
         * <p>
         * This method encapsulates the basic decoding loop, decoding as many bytes as possible until it either runs out of input, runs out of room in the output buffer, or encounters a decoding error. This method is invoked by the {@link #decode decode} method, which handles result interpretation and error recovery.
         * 
         * <p>
         * The buffers are read from, and written to, starting at their current positions. At most {@link Buffer#remaining in.remaining()} bytes will be read, and at most {@link Buffer#remaining out.remaining()} characters will be written. The buffers' positions will be advanced to reflect the bytes read and the characters written, but their marks and limits will not be modified.
         * 
         * <p>
         * This method returns a {@link CoderResult} object to describe its reason for termination, in the same manner as the {@link #decode decode} method. Most implementations of this method will handle decoding errors by returning an appropriate result object for interpretation by the {@link #decode decode} method. An optimized implementation may instead examine the relevant error action and implement that action
         * itself.
         * 
         * <p>
         * An implementation of this method may perform arbitrary lookahead by returning {@link CoderResult#UNDERFLOW} until it receives sufficient input.
         * </p>
         * 
         * @param in
         *            The input byte buffer
         * 
         * @param out
         *            The output character buffer
         * 
         * @return A coder-result object describing the reason for termination
         */
        @Override
        public java.nio.charset.CoderResult decodeLoop(java.nio.ByteBuffer in, java.nio.CharBuffer out) {
            while (in.remaining() > 0) {
                if (measure != null && charCount >= measure) {
                    if (out.remaining() == 0)
                        return CoderResult.OVERFLOW;
                    out.put('\n');
                    charCount = 0;
                }
                if (out.remaining() < 2)
                    return CoderResult.OVERFLOW;
                int b = in.get() & 0xff;
                out.put(codes.charAt(b >>> 4));
                out.put(codes.charAt(b & 0x0f));
                charCount += 2;
            }
            return CoderResult.UNDERFLOW;
        }

        /**
         * Resets this decoder, clearing any charset-specific internal state.
         * 
         * <p>
         * The default implementation of this method does nothing. This method should be overridden by decoders that maintain internal state.
         * </p>
         */
        @Override
        protected void implReset() {
            charCount = 0;
        }

    }
}
