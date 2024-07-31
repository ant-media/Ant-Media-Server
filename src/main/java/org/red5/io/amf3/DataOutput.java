/*
 * RED5 Open Source Media Server - https://github.com/Red5/ Copyright 2006-2016 by respective authors (see below). All rights reserved. Licensed under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless
 * required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package org.red5.io.amf3;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;

import org.apache.mina.core.buffer.IoBuffer;
import org.red5.io.amf.AMF;
import org.red5.io.object.Serializer;

/**
 * Implementation of the IDataOutput interface. Can be used to store an IExternalizable object.
 * 
 * @author The Red5 Project
 * @author Joachim Bauch (jojo@struktur.de)
 * 
 */
public class DataOutput implements IDataOutput {

    /** The output stream */
    private Output output;

    /** Raw data of output destination */
    private IoBuffer buffer;

    /**
     * Create a new DataOutput.
     * 
     * @param output
     *            destination to write to
     */
    protected DataOutput(Output output) {
        this.output = output;
        buffer = output.getBuffer();
    }

    /** {@inheritDoc} */
    @Override
    public ByteOrder getEndian() {
        return buffer.order();
    }

    /** {@inheritDoc} */
    @Override
    public void setEndian(ByteOrder endian) {
        buffer.order(endian);
    }

    /** {@inheritDoc} */
    @Override
    public void writeBoolean(boolean value) {
        buffer.put((byte) (value ? 1 : 0));
    }

    /** {@inheritDoc} */
    @Override
    public void writeByte(byte value) {
        buffer.put(value);
    }

    /** {@inheritDoc} */
    @Override
    public void writeBytes(byte[] bytes) {
        buffer.put(bytes);
    }

    /** {@inheritDoc} */
    @Override
    public void writeBytes(byte[] bytes, int offset) {
        buffer.put(bytes, offset, bytes.length - offset);
    }

    /** {@inheritDoc} */
    @Override
    public void writeBytes(byte[] bytes, int offset, int length) {
        buffer.put(bytes, offset, length);
    }

    /** {@inheritDoc} */
    @Override
    public void writeDouble(double value) {
        buffer.putDouble(value);
    }

    /** {@inheritDoc} */
    @Override
    public void writeFloat(float value) {
        buffer.putFloat(value);
    }

    /** {@inheritDoc} */
    @Override
    public void writeInt(int value) {
        buffer.putInt(value);
    }

    /** {@inheritDoc} */
    @Override
    public void writeMultiByte(String value, String encoding) {
        final Charset cs = Charset.forName(encoding);
        final ByteBuffer strBuf = cs.encode(value);
        buffer.put(strBuf);
    }

    /** {@inheritDoc} */
    @Override
    public void writeObject(Object value) {
        Serializer.serialize(output, value);
    }

    /** {@inheritDoc} */
    @Override
    public void writeShort(short value) {
        buffer.putShort(value);
    }

    /** {@inheritDoc} */
    @Override
    public void writeUnsignedInt(long value) {
        buffer.putInt((int) value);
    }

    /** {@inheritDoc} */
    @Override
    public void writeUTF(String value) {
        // fix from issue #97
        try {
            byte[] strBuf = value.getBytes(AMF.CHARSET.name());
            buffer.putShort((short) strBuf.length);
            buffer.put(strBuf);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void writeUTFBytes(String value) {
        final java.nio.ByteBuffer strBuf = AMF.CHARSET.encode(value);
        buffer.put(strBuf);
    }

}
