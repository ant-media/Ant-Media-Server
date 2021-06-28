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

package org.red5.server.net.rtmp.message;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.apache.mina.core.buffer.IoBuffer;
import org.red5.server.net.protocol.ProtocolException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RTMP chunk header 
 * <pre>
 * rtmp_specification_1.0.pdf (5.3.1.1 page 12)
 * </pre>
 */
public class ChunkHeader implements Constants, Cloneable, Externalizable {
    protected static final Logger log = LoggerFactory.getLogger(ChunkHeader.class);

    /**
     * Chunk format
     */
    private byte format;

    /**
     * Chunk size
     */
    private byte size;

    /**
     * Channel
     */
    private int channelId;

    /**
     * Getter for format
     *
     * @return chunk format
     */
    public byte getFormat() {
        return format;
    }

    /**
     * Setter for format
     *
     * @param format
     *            format
     */
    public void setFormat(byte format) {
        this.format = format;
    }

    /**
     * Getter for channel id
     *
     * @return Channel id
     */
    public int getChannelId() {
        return channelId;
    }

    /**
     * Setter for channel id
     *
     * @param channelId
     *            Header channel id
     */
    public void setChannelId(int channelId) {
        this.channelId = channelId;
    }

    /**
     * Getter for size
     *
     * @return size
     */
    public byte getSize() {
        return size;
    }

    /**
     * Setter for size
     *
     * @param size
     *            Header size
     */
    public void setSize(byte size) {
        this.size = size;
    }

    /**
     * Read chunk header from the buffer.
     * 
     * @param in buffer
     * @return ChunkHeader instance
     */
    public static ChunkHeader read(IoBuffer in) {
        int remaining = in.remaining();
        if (remaining > 0) {
            byte headerByte = in.get();
            ChunkHeader h = new ChunkHeader();
            // going to check highest 2 bits
            h.format = (byte) ((0b11000000 & headerByte) >> 6);
            int fmt = headerByte & 0x3f;
            switch (fmt) {
                case 0:
                    // two byte header
                    h.size = 2;
                    if (remaining < 2) {
                        throw new ProtocolException("Bad chunk header, at least 2 bytes are expected");
                    }
                    h.channelId = 64 + (in.get() & 0xff);
                    break;
                case 1:
                    // three byte header
                    h.size = 3;
                    if (remaining < 3) {
                         throw new ProtocolException("Bad chunk header, at least 3 bytes are expected");
                    }
                    byte b1 = in.get();
                    byte b2 = in.get();
                    h.channelId = 64 + ((b2 & 0xff) << 8 | (b1 & 0xff));
                    break;
                default:
                    // single byte header
                    h.size = 1;
                    h.channelId = 0x3f & headerByte;
                    break;
            }
            // check channel id is valid
            if (h.channelId < 0) {
                throw new ProtocolException("Bad channel id: " + h.channelId);
            }
            log.trace("CHUNK header:: byte {}, count {}, header {}, channel {}", String.format("%02x", headerByte), h.size, 0, h.channelId);
            return h;
        } else {
            // at least one byte for valid decode
            throw new ProtocolException("Bad chunk header, at least 1 byte is expected");
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object other) {
        if (other instanceof ChunkHeader) {
            final ChunkHeader header = (ChunkHeader) other;
            return (header.getChannelId() == channelId && header.getFormat() == format);
        }
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public ChunkHeader clone() {
        final ChunkHeader header = new ChunkHeader();
        header.setChannelId(channelId);
        header.setSize(size);
        header.setFormat(format);
        return header;
    }

    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        format = in.readByte();
        channelId = in.readInt();
        size = (byte) (channelId > 319 ? 3 : (channelId > 63 ? 2 : 1));
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeByte(format);
        out.writeInt(channelId);
    }

    @Override
    public String toString() {
        // if its new and props are un-set, just return that message
        if ((channelId + format) > 0d) {
            return "ChunkHeader [type=" + format + ", channelId=" + channelId + ", size=" + size + "]";
        } else {
            return "empty";
        }
    }

}
