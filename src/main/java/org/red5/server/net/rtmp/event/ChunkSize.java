/*
 * RED5 Open Source Media Server - https://github.com/Red5/
 * 
 * Copyright 2006-2016 by respective authors (see below). All rights reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.red5.server.net.rtmp.event;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Chunk size event
 */
public class ChunkSize extends BaseEvent {

    private static final long serialVersionUID = -7680099175881755879L;

    /**
     * Chunk size
     */
    private int size;

    public ChunkSize() {
        super(Type.SYSTEM);
    }

    /**
     * Create chunk size event with given size
     * 
     * @param size
     *            Chunk size
     */
    public ChunkSize(int size) {
        this();
        this.size = size;
    }

    /** {@inheritDoc} */
    @Override
    public byte getDataType() {
        return TYPE_CHUNK_SIZE;
    }

    /**
     * Getter for size.
     *
     * @return Chunk size
     */
    public int getSize() {
        return size;
    }

    /**
     * Setter for size.
     *
     * @param size
     *            Chunk size
     */
    public void setSize(int size) {
        this.size = size;
    }

    /**
     * Releases chunk (set size to zero)
     */
    protected void doRelease() {
        size = 0;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "ChunkSize: " + size;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ChunkSize)) {
            return false;
        }
        final ChunkSize other = (ChunkSize) obj;
        return getSize() == other.getSize();
    }

    /** {@inheritDoc} */
    @Override
    protected void releaseInternal() {

    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        // XXX Paul: use timestamp as the hash instead of Object.hashCode()
        return timestamp;
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);
        size = in.readInt();
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        super.writeExternal(out);
        out.writeInt(size);
    }
}