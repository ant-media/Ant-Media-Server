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

import org.apache.mina.core.buffer.IoBuffer;
import org.red5.codec.VideoCodec;
import org.red5.io.ITag;
import org.red5.io.IoConstants;
import org.red5.server.api.stream.IStreamPacket;
import org.red5.server.stream.IStreamData;

/**
 * Video data event
 */
public class VideoData extends BaseEvent implements IoConstants, IStreamData<VideoData>, IStreamPacket {

    private static final long serialVersionUID = 5538859593815804830L;

    /**
     * Videoframe type
     */
    public static enum FrameType {
        UNKNOWN, KEYFRAME, INTERFRAME, DISPOSABLE_INTERFRAME,
    }

    /**
     * Video data
     */
    protected IoBuffer data;

    /**
     * Data type
     */
    private byte dataType = TYPE_VIDEO_DATA;

    /**
     * Frame type, unknown by default
     */
    protected FrameType frameType = FrameType.UNKNOWN;

    /**
     * The codec id
     */
    protected int codecId = -1;

    /**
     * True if this is configuration data and false otherwise
     */
    protected boolean config;

    /** Constructs a new VideoData. */
    public VideoData() {
        this(IoBuffer.allocate(0).flip());
    }

    /**
     * Create video data event with given data buffer
     * 
     * @param data
     *            Video data
     */
    public VideoData(IoBuffer data) {
        super(Type.STREAM_DATA);
        setData(data);
    }

    /**
     * Create video data event with given data buffer
     * 
     * @param data
     *            Video data
     * @param copy
     *            true to use a copy of the data or false to use reference
     */
    public VideoData(IoBuffer data, boolean copy) {
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
        if (data != null && data.limit() > 0) {
            data.mark();
            int firstByte = data.get(0) & 0xff;
            codecId = firstByte & ITag.MASK_VIDEO_CODEC;
            if (codecId == VideoCodec.AVC.getId()) {
                config = (data.get() == 0);
            }
            data.reset();
            int frameType = (firstByte & MASK_VIDEO_FRAMETYPE) >> 4;
            if (frameType == FLAG_FRAMETYPE_KEYFRAME) {
                this.frameType = FrameType.KEYFRAME;
            } else if (frameType == FLAG_FRAMETYPE_INTERFRAME) {
                this.frameType = FrameType.INTERFRAME;
            } else if (frameType == FLAG_FRAMETYPE_DISPOSABLE) {
                this.frameType = FrameType.DISPOSABLE_INTERFRAME;
            } else {
                this.frameType = FrameType.UNKNOWN;
            }
        }
    }

    public void setData(byte[] data) {
        this.data = IoBuffer.allocate(data.length);
        this.data.put(data).flip();
    }

    /**
     * Getter for frame type
     *
     * @return Type of video frame
     */
    public FrameType getFrameType() {
        return frameType;
    }

    public int getCodecId() {
        return codecId;
    }

    public boolean isConfig() {
        return config;
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
        frameType = (FrameType) in.readObject();
        byte[] byteBuf = (byte[]) in.readObject();
        if (byteBuf != null) {
            setData(IoBuffer.wrap(byteBuf));
        }
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        super.writeExternal(out);
        out.writeObject(frameType);
        if (data != null) {
            out.writeObject(data.array());
        } else {
            out.writeObject(null);
        }
    }

    /**
     * Duplicate this message / event.
     * 
     * @return duplicated event
     */
    public VideoData duplicate() throws IOException, ClassNotFoundException {
        VideoData result = new VideoData();
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

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return String.format("Video - ts: %s length: %s", getTimestamp(), (data != null ? data.limit() : '0'));
    }

}
