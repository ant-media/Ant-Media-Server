/*
 * RED5 Open Source Media Server - https://github.com/Red5/ Copyright 2006-2016 by respective authors (see below). All rights reserved. Licensed under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless
 * required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package org.red5.io.flv.impl;

import org.apache.mina.core.buffer.IoBuffer;
import org.red5.io.ITag;

/**
 * A Tag represents the contents or payload of a FLV file.
 * 
 * @see <a href="https://code.google.com/p/red5/wiki/FLV#FLV_Tag">FLV Tag</a>
 *
 * @author The Red5 Project
 * @author Dominick Accattato (daccattato@gmail.com)
 * @author Luke Hubbard, Codegent Ltd (luke@codegent.com)
 */
public class Tag implements ITag {

    /**
     * Tag type
     */
    private byte type;

    /**
     * Tag data type
     */
    private byte dataType;

    /**
     * Timestamp
     */
    private int timestamp;

    /**
     * Tag body size
     */
    private int bodySize;

    /**
     * Tag body as byte buffer
     */
    private IoBuffer body;

    /**
     * Previous tag size
     */
    private int previousTagSize;

    /**
     * Bit flags
     */
    private byte bitflags;

    /**
     * TagImpl Constructor
     * 
     * @param dataType
     *            Tag data type
     * @param timestamp
     *            Timestamp
     * @param bodySize
     *            Tag body size
     * @param body
     *            Tag body
     * @param previousTagSize
     *            Previous tag size information
     */
    public Tag(byte dataType, int timestamp, int bodySize, IoBuffer body, int previousTagSize) {
        this.dataType = dataType;
        this.timestamp = timestamp;
        this.bodySize = bodySize;
        this.body = body;
        this.previousTagSize = previousTagSize;
    }

    /** Constructs a new Tag. */
    public Tag() {

    }

    /**
     * Getter for bit flags
     *
     * @return Value for bit flags
     */
    public byte getBitflags() {
        return bitflags;
    }

    /**
     * Setter for bit flags
     *
     * @param bitflags
     *            Bit flags
     */
    public void setBitflags(byte bitflags) {
        this.bitflags = bitflags;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IoBuffer getData() {
        return null;
    }

    /**
     * Return the body IoBuffer
     * 
     * @return Tag body
     */
    @Override
    public IoBuffer getBody() {
        return body;
    }

    /**
     * Return the size of the body
     * 
     * @return Tag body size
     */
    @Override
    public int getBodySize() {
        return bodySize;
    }

    /**
     * Get the data type
     * 
     * @return Tag data type
     */
    @Override
    public byte getDataType() {
        return dataType;
    }

    /**
     * Return the timestamp
     * 
     * @return Tag timestamp
     */
    @Override
    public int getTimestamp() {
        return timestamp;
    }

    /**
     * Return previous tag size
     * 
     * @return Previous tag size
     */
    @Override
    public int getPreviousTagSize() {
        return previousTagSize;
    }

    /**
     * Prints out the contents of the tag
     * 
     * @return Tag contents
     */
    @Override
    public String toString() {
        String ret = "Data Type\t=" + dataType + "\n";
        ret += "Prev. Tag Size\t=" + previousTagSize + "\n";
        ret += "Body size\t=" + bodySize + "\n";
        ret += "timestamp\t=" + timestamp + "\n";
        ret += "Body Data\t=" + body + "\n";
        return ret;
    }

    /**
     * Getter for tag type
     *
     * @return Tag type
     */
    public byte getType() {
        return type;
    }

    /**
     * Setter for tag type
     *
     * @param type
     *            Tag type
     */
    public void setType(byte type) {
        this.type = type;
    }

    /** {@inheritDoc} */
    @Override
    public void setBody(IoBuffer body) {
        this.body = body;
    }

    /** {@inheritDoc} */
    @Override
    public void setBodySize(int bodySize) {
        this.bodySize = bodySize;
    }

    /** {@inheritDoc} */
    @Override
    public void setDataType(byte dataType) {
        this.dataType = dataType;
    }

    /** {@inheritDoc} */
    @Override
    public void setTimestamp(int timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Setter for tag data. Empty method.
     */
    public void setData() {
    }

    /** {@inheritDoc} */
    @Override
    public void setPreviousTagSize(int size) {
        this.previousTagSize = size;
    }

}
