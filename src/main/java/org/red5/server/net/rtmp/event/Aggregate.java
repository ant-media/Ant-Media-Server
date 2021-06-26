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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.LinkedList;

import org.apache.mina.core.buffer.IoBuffer;
import org.red5.io.IoConstants;
import org.red5.io.utils.IOUtils;
import org.red5.server.api.stream.IStreamPacket;
import org.red5.server.net.rtmp.message.Header;
import org.red5.server.stream.IStreamData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Aggregate data event
 */
public class Aggregate extends BaseEvent implements IoConstants, IStreamData<Aggregate>, IStreamPacket {

    private static final long serialVersionUID = 5538859593815804830L;

    private static Logger log = LoggerFactory.getLogger(Aggregate.class);

    /**
     * Data
     */
    protected IoBuffer data;

    /**
     * Data type
     */
    private byte dataType = TYPE_AGGREGATE;

    /** Constructs a new Aggregate. */
    public Aggregate() {
        this(IoBuffer.allocate(0).flip());
    }

    /**
     * Create aggregate data event with given data buffer.
     * 
     * @param data
     *            data
     */
    public Aggregate(IoBuffer data) {
        super(Type.STREAM_DATA);
        setData(data);
    }

    /**
     * Create aggregate data event with given data buffer.
     * 
     * @param data
     *            aggregate data
     * @param copy
     *            true to use a copy of the data or false to use reference
     */
    public Aggregate(IoBuffer data, boolean copy) {
        super(Type.STREAM_DATA);
        if (copy) {
            byte[] array = new byte[data.limit()];
            data.mark();
            data.get(array);
            data.reset();
            setData(array);
        } else {
            setData(data);
        }
    }

    /** {@inheritDoc} */
    @Override
    public byte getDataType() {
        return dataType;
    }

    public void setDataType(byte dataType) {
        this.dataType = dataType;
    }

    /** {@inheritDoc} */
    public IoBuffer getData() {
        return data;
    }

    public void setData(IoBuffer data) {
        this.data = data;
    }

    public void setData(byte[] data) {
        this.data = IoBuffer.allocate(data.length);
        this.data.put(data).flip();
    }

    /**
     * Breaks-up the aggregate into its individual parts and returns them as a list. The parts are returned based on the ordering of the aggregate itself.
     * 
     * @return list of IRTMPEvent objects
     */
    public LinkedList<IRTMPEvent> getParts() {
        LinkedList<IRTMPEvent> parts = new LinkedList<IRTMPEvent>();
        log.trace("Aggregate data length: {}", data.limit());
        int position = data.position();
        do {
            try {
                // read the header
                //log.trace("Hex: {}", data.getHexDump());
                byte subType = data.get();
                // when we run into subtype 0 break out of here
                if (subType == 0) {
                    log.debug("Subtype 0 encountered within this aggregate, processing with exit");
                    break;
                }
                int size = IOUtils.readUnsignedMediumInt(data);
                log.debug("Data subtype: {} size: {}", subType, size);
                // TODO ensure the data contains all the bytes to support the specified size
                int timestamp = IOUtils.readExtendedMediumInt(data);
                /*timestamp = ntohap((GETIBPOINTER(buffer) + 4)); 0x12345678 == 34 56 78 12*/
                int streamId = IOUtils.readUnsignedMediumInt(data);
                log.debug("Data timestamp: {} stream id: {}", timestamp, streamId);
                Header partHeader = new Header();
                partHeader.setChannelId(header.getChannelId());
                partHeader.setDataType(subType);
                partHeader.setSize(size);
                // use the stream id from the aggregate's header
                partHeader.setStreamId(header.getStreamId());
                partHeader.setTimer(timestamp);
                // timer delta == time stamp - timer base
                // the back pointer may be used to verify the size of the individual part
                // it will be equal to the data size + header size
                int backPointer = 0;
                switch (subType) {
                    case TYPE_AUDIO_DATA:
                        AudioData audio = new AudioData(data.getSlice(size));
                        audio.setTimestamp(timestamp);
                        audio.setHeader(partHeader);
                        log.debug("Audio header: {}", audio.getHeader());
                        parts.add(audio);
                        //log.trace("Hex: {}", data.getHexDump());
                        // ensure 4 bytes left to read an int
                        if (data.position() < data.limit() - 4) {
                            backPointer = data.getInt();
                            //log.trace("Back pointer: {}", backPointer);
                            if (backPointer != (size + 11)) {
                                log.debug("Data size ({}) and back pointer ({}) did not match", size, backPointer);
                            }
                        }
                        break;
                    case TYPE_VIDEO_DATA:
                        VideoData video = new VideoData(data.getSlice(size));
                        video.setTimestamp(timestamp);
                        video.setHeader(partHeader);
                        log.debug("Video header: {}", video.getHeader());
                        parts.add(video);
                        //log.trace("Hex: {}", data.getHexDump());
                        // ensure 4 bytes left to read an int
                        if (data.position() < data.limit() - 4) {
                            backPointer = data.getInt();
                            //log.trace("Back pointer: {}", backPointer);
                            if (backPointer != (size + 11)) {
                                log.debug("Data size ({}) and back pointer ({}) did not match", size, backPointer);
                            }
                        }
                        break;
                    default:
                        log.debug("Non-A/V subtype: {}", subType);
                        Unknown unk = new Unknown(subType, data.getSlice(size));
                        unk.setTimestamp(timestamp);
                        unk.setHeader(partHeader);
                        parts.add(unk);
                        // ensure 4 bytes left to read an int
                        if (data.position() < data.limit() - 4) {
                            backPointer = data.getInt();
                        }
                }
                position = data.position();
            } catch (Exception e) {
                log.error("Exception decoding aggregate parts", e);
                break;
            }
            log.trace("Data position: {}", position);
        } while (position < data.limit());
        log.trace("Aggregate processing complete, {} parts extracted", parts.size());
        return parts;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return String.format("Aggregate - ts: %s length: %s", getTimestamp(), (data != null ? data.limit() : '0'));
    }

    /** {@inheritDoc} */
    @Override
    protected void releaseInternal() {
        if (data != null) {
            final IoBuffer localData = data;
            // null out the data first so we don't accidentally
            // return a valid reference first
            data = null;
            localData.clear();
            localData.free();
        }
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);
        byte[] byteBuf = (byte[]) in.readObject();
        if (byteBuf != null) {
            data = IoBuffer.allocate(byteBuf.length);
            data.setAutoExpand(true);
            SerializeUtils.ByteArrayToByteBuffer(byteBuf, data);
        }
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        super.writeExternal(out);
        if (data != null) {
            out.writeObject(SerializeUtils.ByteBufferToByteArray(data));
        } else {
            out.writeObject(null);
        }
    }

    /**
     * Duplicate this message / event.
     * 
     * @return duplicated event
     */
    public Aggregate duplicate() throws IOException, ClassNotFoundException {
        Aggregate result = new Aggregate();
        // serialize
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        writeExternal(oos);
        oos.close();
        // convert to byte array
        byte[] buf = baos.toByteArray();
        baos.close();
        // create input streams
        ByteArrayInputStream bais = new ByteArrayInputStream(buf);
        ObjectInputStream ois = new ObjectInputStream(bais);
        // deserialize
        result.readExternal(ois);
        ois.close();
        bais.close();
        // clone the header if there is one
        if (header != null) {
            result.setHeader(header.clone());
        }
        return result;
    }

}
