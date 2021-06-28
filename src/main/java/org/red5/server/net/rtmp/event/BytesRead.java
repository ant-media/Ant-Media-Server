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
 * Bytes read event
 */
public class BytesRead extends BaseEvent {

    private static final long serialVersionUID = -127649312402709338L;

    /**
     * Bytes read
     */
    private int bytesRead;

    public BytesRead() {
        super(Type.STREAM_CONTROL);
    }

    /**
     * Creates new event with given bytes number
     * 
     * @param bytesRead
     *            Number of bytes read
     */
    public BytesRead(int bytesRead) {
        this();
        this.bytesRead = bytesRead;
    }

    /** {@inheritDoc} */
    @Override
    public byte getDataType() {
        return TYPE_BYTES_READ;
    }

    /**
     * Return number of bytes read
     *
     * @return Number of bytes
     */
    public int getBytesRead() {
        return bytesRead;
    }

    /**
     * Setter for bytes read
     *
     * @param bytesRead
     *            Number of bytes read
     */
    public void setBytesRead(int bytesRead) {
        this.bytesRead = bytesRead;
    }

    /**
     * Release event (set bytes read to zero)
     */
    protected void doRelease() {
        bytesRead = 0;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "StreamBytesRead: " + bytesRead;
    }

    /** {@inheritDoc} */
    @Override
    protected void releaseInternal() {

    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);
        bytesRead = in.readInt();
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        super.writeExternal(out);
        out.writeInt(bytesRead);
    }
}