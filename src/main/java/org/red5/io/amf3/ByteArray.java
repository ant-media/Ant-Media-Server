/*
 * RED5 Open Source Media Server - https://github.com/Red5/ Copyright 2006-2016 by respective authors (see below). All rights reserved. Licensed under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless
 * required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package org.red5.io.amf3;

import java.io.IOException;
import java.nio.ByteOrder;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

import org.apache.mina.core.buffer.IoBuffer;

/**
 * Red5 version of the Flex ByteArray class.
 * 
 * @author The Red5 Project
 * @author Joachim Bauch (jojo@struktur.de)
 */
public class ByteArray implements IDataInput, IDataOutput {

    /** Internal storage for array contents. */
    protected IoBuffer data;

    /** Object used to read from array. */
    protected IDataInput dataInput;

    /** Object used to write to array. */
    protected IDataOutput dataOutput;

    /**
     * Internal constructor used to create ByteArray during deserialization.
     * 
     * @param buffer
     *            io buffer
     * @param length
     *            length
     */
    protected ByteArray(IoBuffer buffer, int length) {
        data = IoBuffer.allocate(length);
        data.setAutoExpand(true);
        byte[] tmp = new byte[length];
        buffer.get(tmp);
        data.put(tmp);
        data.flip();
        prepareIO();
    }

    /**
     * Public constructor. Creates new empty ByteArray.
     */
    public ByteArray() {
        data = IoBuffer.allocate(0);
        data.setAutoExpand(true);
        prepareIO();
    }

    /**
     * Create internal objects used for reading and writing.
     */
    protected void prepareIO() {
        // we assume that everything in ByteArray is in AMF3
        Input input = new Input(data);
        input.enforceAMF3();
        dataInput = new DataInput(input);
        Output output = new Output(data);
        output.enforceAMF3();
        dataOutput = new DataOutput(output);
    }

    /**
     * Get internal data.
     * 
     * @return byte buffer
     */
    protected IoBuffer getData() {
        return data;
    }

    /**
     * Get the current position in the data.
     * 
     * @return current position
     */
    public int position() {
        return data.position();
    }

    /**
     * Set the current position in the data.
     * 
     * @param position
     *            position to set
     */
    public void position(int position) {
        data.position(position);
    }

    /**
     * Return number of bytes available for reading.
     * 
     * @return bytes available
     */
    public int bytesAvailable() {
        return length() - position();
    }

    /**
     * Return total number of bytes in array.
     * 
     * @return number of bytes in array
     */
    public int length() {
        return data.limit();
    }

    /**
     * Compress contents using zlib.
     */
    public void compress() {
        IoBuffer tmp = IoBuffer.allocate(0);
        tmp.setAutoExpand(true);
        byte[] tmpData = new byte[data.limit()];
        data.position(0);
        data.get(tmpData);
        try (DeflaterOutputStream deflater = new DeflaterOutputStream(tmp.asOutputStream(), new Deflater(Deflater.BEST_COMPRESSION))) {
            deflater.write(tmpData);
            deflater.finish();
        } catch (IOException e) {
            //docs state that free is optional
            tmp.free();
            throw new RuntimeException("could not compress data", e);
        }
        data.free();
        data = tmp;
        data.flip();
        prepareIO();
    }

    /**
     * Decompress contents using zlib.
     */
    public void uncompress() {
        data.position(0);
        byte[] buffer = new byte[8192];
        IoBuffer tmp = IoBuffer.allocate(0);
        tmp.setAutoExpand(true);
        try (InflaterInputStream inflater = new InflaterInputStream(data.asInputStream())) {
            while (inflater.available() > 0) {
                int decompressed = inflater.read(buffer);
                if (decompressed <= 0) {
                    // Finished decompression
                    break;
                }
                tmp.put(buffer, 0, decompressed);
            }
        } catch (IOException e) {
            tmp.free();
            throw new RuntimeException("could not uncompress data", e);
        }
        data.free();
        data = tmp;
        data.flip();
        prepareIO();
    }

    /** {@inheritDoc} */
    @Override
    public ByteOrder getEndian() {
        return dataInput.getEndian();
    }

    /** {@inheritDoc} */
    @Override
    public boolean readBoolean() {
        return dataInput.readBoolean();
    }

    /** {@inheritDoc} */
    @Override
    public byte readByte() {
        return dataInput.readByte();
    }

    /** {@inheritDoc} */
    @Override
    public void readBytes(byte[] bytes) {
        dataInput.readBytes(bytes);
    }

    /** {@inheritDoc} */
    @Override
    public void readBytes(byte[] bytes, int offset) {
        dataInput.readBytes(bytes, offset);
    }

    /** {@inheritDoc} */
    @Override
    public void readBytes(byte[] bytes, int offset, int length) {
        dataInput.readBytes(bytes, offset, length);
    }

    /** {@inheritDoc} */
    @Override
    public double readDouble() {
        return dataInput.readDouble();
    }

    /** {@inheritDoc} */
    @Override
    public float readFloat() {
        return dataInput.readFloat();
    }

    /** {@inheritDoc} */
    @Override
    public int readInt() {
        return dataInput.readInt();
    }

    /** {@inheritDoc} */
    @Override
    public String readMultiByte(int length, String charSet) {
        return dataInput.readMultiByte(length, charSet);
    }

    /** {@inheritDoc} */
    @Override
    public Object readObject() {
        // according to AMF3 spec, each object should have its own "reference" tables,
        // so we must recreate Input object before reading each object 
        prepareIO();
        return dataInput.readObject();
    }

    /** {@inheritDoc} */
    @Override
    public short readShort() {
        return dataInput.readShort();
    }

    /** {@inheritDoc} */
    @Override
    public String readUTF() {
        return dataInput.readUTF();
    }

    /** {@inheritDoc} */
    @Override
    public String readUTFBytes(int length) {
        return dataInput.readUTFBytes(length);
    }

    /** {@inheritDoc} */
    @Override
    public int readUnsignedByte() {
        return dataInput.readUnsignedByte();
    }

    /** {@inheritDoc} */
    @Override
    public long readUnsignedInt() {
        return dataInput.readUnsignedInt();
    }

    /** {@inheritDoc} */
    @Override
    public int readUnsignedShort() {
        return dataInput.readUnsignedShort();
    }

    /** {@inheritDoc} */
    @Override
    public void setEndian(ByteOrder endian) {
        dataInput.setEndian(endian);
        dataOutput.setEndian(endian);
    }

    /** {@inheritDoc} */
    @Override
    public void writeBoolean(boolean value) {
        dataOutput.writeBoolean(value);
    }

    /** {@inheritDoc} */
    @Override
    public void writeByte(byte value) {
        dataOutput.writeByte(value);
    }

    /** {@inheritDoc} */
    @Override
    public void writeBytes(byte[] bytes) {
        dataOutput.writeBytes(bytes);
    }

    /** {@inheritDoc} */
    @Override
    public void writeBytes(byte[] bytes, int offset) {
        dataOutput.writeBytes(bytes, offset);
    }

    /** {@inheritDoc} */
    @Override
    public void writeBytes(byte[] bytes, int offset, int length) {
        dataOutput.writeBytes(bytes, offset, length);
    }

    /** {@inheritDoc} */
    @Override
    public void writeDouble(double value) {
        dataOutput.writeDouble(value);
    }

    /** {@inheritDoc} */
    @Override
    public void writeFloat(float value) {
        dataOutput.writeFloat(value);
    }

    /** {@inheritDoc} */
    @Override
    public void writeInt(int value) {
        dataOutput.writeInt(value);
    }

    /** {@inheritDoc} */
    @Override
    public void writeMultiByte(String value, String encoding) {
        dataOutput.writeMultiByte(value, encoding);
    }

    /** {@inheritDoc} */
    @Override
    public void writeObject(Object value) {
        // according to AMF3 spec, each object should have its own "reference" tables,
        // so we must recreate Input object before writing each object 
        prepareIO();
        dataOutput.writeObject(value);
    }

    /** {@inheritDoc} */
    @Override
    public void writeShort(short value) {
        dataOutput.writeShort(value);
    }

    /** {@inheritDoc} */
    @Override
    public void writeUTF(String value) {
        dataOutput.writeUTF(value);
    }

    /** {@inheritDoc} */
    @Override
    public void writeUTFBytes(String value) {
        dataOutput.writeUTFBytes(value);
    }

    /** {@inheritDoc} */
    @Override
    public void writeUnsignedInt(long value) {
        dataOutput.writeUnsignedInt(value);
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ByteArray other = (ByteArray) obj;
        if (!toString().equals(other.toString())) {
            return false;
        }
        return true;
    }

    /**
     * Return string representation of the array's contents.
     * 
     * @return string representation of array's contents.
     */
    @Override
    public String toString() {
        if (data != null) {
            int old = data.position();
            try {
                data.position(0);
                return data.asCharBuffer().toString();
            } finally {
                data.position(old);
            }
        } else {
            return "";
        }
    }
}
