/*
 * RED5 Open Source Media Server - https://github.com/Red5/ Copyright 2006-2016 by respective authors (see below). All rights reserved. Licensed under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless
 * required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package org.red5.io;

import java.io.IOException;

import org.apache.mina.core.buffer.IoBuffer;
import org.red5.media.processor.IPostProcessor;

/**
 * Writes tags to a file
 */
public interface ITagWriter {

    /**
     * Closes a Writer
     */
    public void close();

    /**
     * Return the bytes written
     *
     * @return Number of bytes written
     */
    public long getBytesWritten();

    /**
     * Return the file that is written.
     *
     * @return the File to be written
     */
    public IStreamableFile getFile();

    /**
     * Return the offset
     *
     * @return Offset value
     */
    public int getOffset();

    /**
     * Writes the header bytes
     *
     * @throws IOException
     *             I/O exception
     */
    public void writeHeader() throws IOException;

    /**
     * Write a Stream to disk using bytes
     *
     * @param b
     *            Array of bytes to write
     * @return <code>true</code> on success, <code>false</code> otherwise
     */
    public boolean writeStream(byte[] b);

    /**
     * Write a Tag using bytes
     *
     * @param type
     *            Tag type
     * @param data
     *            Byte data
     * @return <code>true</code> on success, <code>false</code> otherwise
     * @throws IOException
     *             I/O exception
     */
    public boolean writeTag(byte type, IoBuffer data) throws IOException;

    /**
     * Writes a Tag object
     *
     * @param tag
     *            Tag to write
     * @return <code>true</code> on success, <code>false</code> otherwise
     * @throws IOException
     *             I/O exception
     */
    public boolean writeTag(ITag tag) throws IOException;

    /**
     * Adds a post-process for execution once the instance completes.
     * 
     * @param postProcessor an implementation instance of IPostProcessor
     */
    public void addPostProcessor(IPostProcessor postProcessor);

}
