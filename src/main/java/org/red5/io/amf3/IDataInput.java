/*
 * RED5 Open Source Media Server - https://github.com/Red5/ Copyright 2006-2016 by respective authors (see below). All rights reserved. Licensed under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless
 * required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package org.red5.io.amf3;

import java.nio.ByteOrder;

/**
 * Interface implemented by classes that provide a way to load custom objects.
 * 
 * @see IExternalizable#readExternal(IDataInput)
 * @see <a href="http://livedocs.adobe.com/flex/2/langref/flash/utils/IDataInput.html">Adobe Livedocs (external)</a>
 */
public interface IDataInput {

    /**
     * Return the byteorder used when loading values.
     * 
     * @return the byteorder
     */
    public ByteOrder getEndian();

    /**
     * Set the byteorder to use when loading values.
     * 
     * @param endian
     *            the byteorder to use
     */
    public void setEndian(ByteOrder endian);

    /**
     * Read boolean value.
     * 
     * @return the value
     */
    public boolean readBoolean();

    /**
     * Read signed single byte value.
     * 
     * @return the value
     */
    public byte readByte();

    /**
     * Read list of bytes.
     * 
     * @param bytes
     *            destination for read bytes
     */
    public void readBytes(byte[] bytes);

    /**
     * Read list of bytes to given offset.
     * 
     * @param bytes
     *            destination for read bytes
     * @param offset
     *            offset in destination to write to
     */
    public void readBytes(byte[] bytes, int offset);

    /**
     * Read given number of bytes to given offset.
     * 
     * @param bytes
     *            destination for read bytes
     * @param offset
     *            offset in destination to write to
     * @param length
     *            number of bytes to read
     */
    public void readBytes(byte[] bytes, int offset, int length);

    /**
     * Read double-precision floating point value.
     * 
     * @return the value
     */
    public double readDouble();

    /**
     * Read single-precision floating point value.
     * 
     * @return the value
     */
    public float readFloat();

    /**
     * Read signed integer value.
     * 
     * @return the value
     */
    public int readInt();

    /**
     * Read multibyte string.
     * 
     * @param length
     *            length of string to read
     * @param charSet
     *            character set of string to read
     * @return the string
     */
    public String readMultiByte(int length, String charSet);

    /**
     * Read arbitrary object.
     * 
     * @return the object
     */
    public Object readObject();

    /**
     * Read signed short value.
     * 
     * @return the value
     */
    public short readShort();

    /**
     * Read unsigned single byte value.
     * 
     * @return the value
     */
    public int readUnsignedByte();

    /**
     * Read unsigned integer value.
     * 
     * @return the value
     */
    public long readUnsignedInt();

    /**
     * Read unsigned short value.
     * 
     * @return the value
     */
    public int readUnsignedShort();

    /**
     * Read UTF-8 encoded string.
     * 
     * @return the string
     */
    public String readUTF();

    /**
     * Read UTF-8 encoded string with given length.
     * 
     * @param length
     *            the length of the string
     * @return the string
     */
    public String readUTFBytes(int length);

}
