/*
 * RED5 Open Source Media Server - https://github.com/Red5/ Copyright 2006-2016 by respective authors (see below). All rights reserved. Licensed under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless
 * required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package org.red5.io.flv;

import java.nio.ByteBuffer;
import org.apache.mina.core.buffer.IoBuffer;

/**
 * FLVHeader parses out the contents of a FLV video file and returns the Header data
 *
 * @see <a href="https://code.google.com/p/red5/wiki/FLV#FLV_Header">FLV Header</a>
 *
 * @author The Red5 Project
 * @author Dominick Accattato (daccattato@gmail.com)
 * @author Luke Hubbard, Codegent Ltd (luke@codegent.com)
 * @author Tiago Jacobs (tiago@imdt.com.br)
 */
public class FLVHeader {

    static final int FLV_HEADER_FLAG_HAS_AUDIO = 4;

    static final int FLV_HEADER_FLAG_HAS_VIDEO = 1;

    /**
     * Signature
     */
    public final static byte[] signature = new byte[] { (byte) (0x46 & 0xff), (byte) (0x4C & 0xff), (byte) (0x56 & 0xff) };

    /**
     * FLV version
     */
    public final static byte version = 0x01 & 0xff; //version 1

    // TYPES

    /**
     * Reserved flag, one
     */
    public static byte flagReserved01 = 0x00 & 0xff;

    /**
     * Audio flag
     */
    public boolean flagAudio;

    /**
     * Reserved flag, two
     */
    public static byte flagReserved02 = 0x00 & 0xff;

    /**
     * Video flag
     */
    public boolean flagVideo;

    // DATA OFFSET
    /**
     * reserved for data up to 4,294,967,295
     */
    public int dataOffset = 0x00 & 0xff;

    /**
     * Returns the data offset bytes
     *
     * @return int Data offset
     */
    public int getDataOffset() {
        return dataOffset;
    }

    /**
     * Sets the data offset bytes
     *
     * @param data_offset
     *            Data offset
     */
    public void setDataOffset(int data_offset) {
        dataOffset = data_offset;
    }

    /**
     * Returns the signature bytes
     *
     * @return byte[] Signature
     */
    public byte[] getSignature() {
        return signature;
    }

    /**
     * Overrides the toString method so that a FLVHeader can be represented by its datatypes
     *
     * @return String String representation
     */
    @Override
    public String toString() {
        String ret = "";
        //ret += "SIGNATURE: \t" + getSIGNATURE() + "\n";
        //ret += "SIGNATURE: \t\t" + new String(signature) + "\n";
        ret += "VERSION: \t\t" + getVersion() + "\n";
        ret += "TYPE FLAGS VIDEO: \t" + getFlagVideo() + "\n";
        ret += "TYPE FLAGS AUDIO: \t" + getFlagAudio() + "\n";
        ret += "DATA OFFSET: \t\t" + getDataOffset() + "\n";
        return ret;
    }

    /**
     * Returns a boolean on whether this data contains audio
     *
     * @return boolean <code>true</code> if this FLV header contains audio data, <code>false</code> otherwise
     */
    public boolean getFlagAudio() {
        return flagAudio;
    }

    /**
     * Sets the audioflag on whether this data contains audio
     *
     * @param flagAudio
     *            <code>true</code> if this FLV header contains audio data, <code>false</code> otherwise
     */
    public void setFlagAudio(boolean flagAudio) {
        this.flagAudio = flagAudio;
    }

    /**
     * Sets the type flags on whether this data is audio or video
     *
     * @param typeFlags
     *            Type flags determining data types (audio or video)
     */
    public void setTypeFlags(byte typeFlags) {
        flagVideo = (((byte) (((typeFlags << 0x7) >>> 0x7) & 0x01)) > 0x00);
        flagAudio = (((byte) (((typeFlags << 0x5) >>> 0x7) & 0x01)) > 0x00);
    }

    /**
     * Gets the FlagReserved01 which is a datatype specified in the Flash Specification
     *
     * @return byte Flag reserved, first
     */
    public byte getFlagReserved01() {
        return flagReserved01;
    }

    /**
     * Sets the FlagReserved01 which is a datatype specified in the Flash Specification
     *
     * @param flagReserved01
     *            Flag reserved, first
     */
    @SuppressWarnings("static-access")
    public void setFlagReserved01(byte flagReserved01) {
        this.flagReserved01 = flagReserved01;
    }

    /**
     * Gets the FlagReserved02 which is a datatype specified in the Flash Specification
     *
     * @return byte FlagReserved02
     */
    public byte getFlagReserved02() {
        return flagReserved02;
    }

    /**
     * Sets the Flag Reserved02 which is a datatype specified in the Flash Specification
     *
     * @param flagReserved02
     *            FlagReserved02
     */
    @SuppressWarnings("static-access")
    public void setFlagReserved02(byte flagReserved02) {
        this.flagReserved02 = flagReserved02;
    }

    /**
     * Returns a boolean on whether this data contains video
     *
     * @return boolean <code>true</code> if this FLV header contains vide data, <code>false</code> otherwise
     */
    public boolean getFlagVideo() {
        return flagVideo;
    }

    /**
     * Sets the audioflag on whether this data contains audio
     *
     * @param type_flags_video
     *            <code>true</code> if this FLV header contains video data, <code>false</code> otherwise
     */
    public void setFlagVideo(boolean type_flags_video) {
        flagVideo = type_flags_video;
    }

    /**
     * Gets the version byte
     *
     * @return byte FLV version byte
     */
    public byte getVersion() {
        return version;
    }

    /**
     * Writes the FLVHeader to IoBuffer.
     *
     * @param buffer
     *            IoBuffer to write
     */
    public void write(IoBuffer buffer) {
        // FLV
        buffer.put(signature);
        // version
        buffer.put(version);
        // flags
        buffer.put((byte) (FLV_HEADER_FLAG_HAS_AUDIO * (flagAudio ? 1 : 0) + FLV_HEADER_FLAG_HAS_VIDEO * (flagVideo ? 1 : 0)));
        // data offset
        buffer.putInt(9);
        // previous tag size 0 (this is the "first" tag)
        buffer.putInt(0);
        buffer.flip();
    }

    public void write(ByteBuffer buffer) {
        // FLV
        buffer.put(signature);
        // version
        buffer.put(version);
        // flags
        buffer.put((byte) (FLV_HEADER_FLAG_HAS_AUDIO * (flagAudio ? 1 : 0) + FLV_HEADER_FLAG_HAS_VIDEO * (flagVideo ? 1 : 0)));
        // data offset
        buffer.putInt(9);
        // previous tag size 0 (this is the "first" tag)
        buffer.putInt(0);
        buffer.flip();
    }

}
