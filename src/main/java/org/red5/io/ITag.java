/*
 * RED5 Open Source Media Server - https://github.com/Red5/ Copyright 2006-2016 by respective authors (see below). All rights reserved. Licensed under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless
 * required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package org.red5.io;

import org.apache.mina.core.buffer.IoBuffer;

/**
 * A Tag represents the contents or payload of a streamable file.
 *
 * @author The Red5 Project
 * @author Dominick Accattato (daccattato@gmail.com)
 * @author Luke Hubbard, Codegent Ltd (luke@codegent.com)
 */
public interface ITag extends IoConstants {

    /**
     * Return the body ByteBuffer
     *
     * @return ByteBuffer Body as byte buffer
     */
    public IoBuffer getBody();

    /**
     * Return the size of the body
     *
     * @return int Body size
     */
    public int getBodySize();

    /**
     * Returns the data as a ByteBuffer
     *
     * @return ByteBuffer Data as byte buffer
     */
    public IoBuffer getData();

    /**
     * Get the data type
     *
     * @return byte Data type as byte
     */
    public byte getDataType();

    /**
     * Returns previous tag size
     *
     * @return int Previous tag size
     */
    public int getPreviousTagSize();

    /**
     * Return the timestamp
     *
     * @return int Timestamp
     */
    public int getTimestamp();

    /**
     * Set the body ByteBuffer.
     *
     * @param body
     *            Body as ByteBuffer
     */
    public void setBody(IoBuffer body);

    /**
     * Set the size of the body.
     *
     * @param size
     *            Body size
     */
    public void setBodySize(int size);

    /**
     * Set the data type.
     *
     * @param datatype
     *            Data type
     */
    public void setDataType(byte datatype);

    /**
     * Set the size of the previous tag.
     *
     * @param size
     *            Previous tag size
     */
    public void setPreviousTagSize(int size);

    /**
     * Set the timestamp.
     *
     * @param timestamp
     *            Timestamp
     */
    public void setTimestamp(int timestamp);

}
