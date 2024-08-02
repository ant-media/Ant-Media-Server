/*
 * RED5 Open Source Media Server - https://github.com/Red5/ Copyright 2006-2016 by respective authors (see below). All rights reserved. Licensed under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless
 * required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package org.red5.io.amf3;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;

import org.apache.mina.core.buffer.IoBuffer;
import org.red5.io.amf.AMF;
import org.red5.io.object.Deserializer;

/**
 * Implementation of the IDataInput interface. Can be used to load an IExternalizable object.
 * 
 * @author The Red5 Project
 * @author Joachim Bauch (jojo@struktur.de)
 * 
 */
public class DataInput implements IDataInput {

    /** The input stream. */
    private Input input;

    /** Raw data of input source. */
    private IoBuffer buffer;

    /**
     * Create a new DataInput.
     * 
     * @param input
     *            input to use
     */
    protected DataInput(Input input) {
        this.input = input;
        buffer = input.getBuffer();
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
    public boolean readBoolean() {
        return (buffer.get() != 0);
    }

    /** {@inheritDoc} */
    @Override
    public byte readByte() {
        return buffer.get();
    }

    /** {@inheritDoc} */
    @Override
    public void readBytes(byte[] bytes) {
        buffer.get(bytes);
    }

    /** {@inheritDoc} */
    @Override
    public void readBytes(byte[] bytes, int offset) {
        buffer.get(bytes, offset, bytes.length - offset);
    }

    /** {@inheritDoc} */
    @Override
    public void readBytes(byte[] bytes, int offset, int length) {
        buffer.get(bytes, offset, length);
    }

    /** {@inheritDoc} */
    @Override
    public double readDouble() {
        return buffer.getDouble();
    }

    /** {@inheritDoc} */
    @Override
    public float readFloat() {
        return buffer.getFloat();
    }

    /** {@inheritDoc} */
    @Override
    public int readInt() {
        return buffer.getInt();
    }

    /** {@inheritDoc} */
    @Override
    public String readMultiByte(int length, String charSet) {
        final Charset cs = Charset.forName(charSet);
        int limit = buffer.limit();
        final ByteBuffer strBuf = buffer.buf();
        strBuf.limit(strBuf.position() + length);
        final String string = cs.decode(strBuf).toString();
        buffer.limit(limit); // Reset the limit
        return string;
    }

    /** {@inheritDoc} */
    @Override
    public Object readObject() {
        return Deserializer.deserialize(input, Object.class);
    }

    /** {@inheritDoc} */
    @Override
    public short readShort() {
        return buffer.getShort();
    }

    /** {@inheritDoc} */
    @Override
    public int readUnsignedByte() {
        return buffer.getUnsigned();
    }

    /** {@inheritDoc} */
    @Override
    public long readUnsignedInt() {
        return buffer.getUnsignedInt();
    }

    /** {@inheritDoc} */
    @Override
    public int readUnsignedShort() {
        return buffer.getShort() & 0xffff; //buffer.getUnsignedShort();
    }

    /** {@inheritDoc} */
    @Override
    public String readUTF() {
        int length = buffer.getShort() & 0xffff; //buffer.getUnsignedShort();
        return readUTFBytes(length);
    }

    /** {@inheritDoc} */
    @Override
    public String readUTFBytes(int length) {
        int limit = buffer.limit();
        final ByteBuffer strBuf = buffer.buf();
        strBuf.limit(strBuf.position() + length);
        final String string = AMF.CHARSET.decode(strBuf).toString();
        buffer.limit(limit); // Reset the limit
        return string;
    }

}
