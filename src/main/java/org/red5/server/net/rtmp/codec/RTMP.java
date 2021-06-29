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

package org.red5.server.net.rtmp.codec;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.red5.server.api.IConnection.Encoding;
import org.red5.server.net.rtmp.message.Header;
import org.red5.server.net.rtmp.message.Packet;

/**
 * RTMP is the RTMP protocol state representation.
 */
public class RTMP {

    public static final String[] states = { "connect", "handshake", "connected", "error", "disconnecting", "disconnected" };

    /**
     * Connect state
     */
    public static final byte STATE_CONNECT = 0x00;

    /**
     * Handshake state. Server sends handshake request to client right after connection established.
     */
    public static final byte STATE_HANDSHAKE = 0x01;

    /**
     * Connected
     */
    public static final byte STATE_CONNECTED = 0x02;

    /**
     * Error
     */
    public static final byte STATE_ERROR = 0x03;

    /**
     * In the processing of disconnecting
     */
    public static final byte STATE_DISCONNECTING = 0x04;

    /**
     * Disconnected
     */
    public static final byte STATE_DISCONNECTED = 0x05;

    /**
     * Sent the connect message to origin.
     */
    public static final byte STATE_EDGE_CONNECT_ORIGIN_SENT = 0x11;

    /**
     * Forwarded client's connect call to origin.
     */
    public static final byte STATE_ORIGIN_CONNECT_FORWARDED = 0x12;

    /**
     * Edge is disconnecting, waiting Origin close connection.
     */
    public static final byte STATE_EDGE_DISCONNECTING = 0x13;

    /**
     * RTMP state.
     */
    private volatile byte state = STATE_CONNECT;

    /**
     * Encryption flag.
     */
    private boolean encrypted = false;

    /**
     * Map for channels, keyed by channel id.
     */
    private final transient ConcurrentMap<Integer, ChannelInfo> channels = new ConcurrentHashMap<Integer, ChannelInfo>(3, 0.9f, 1);

    /**
     * Read chunk size. Packets are read and written chunk-by-chunk.
     */
    private int readChunkSize = 128;

    /**
     * Write chunk size. Packets are read and written chunk-by-chunk.
     */
    private int writeChunkSize = 128;

    /**
     * Encoding type for objects.
     */
    private Encoding encoding = Encoding.AMF0;

    /**
     * Creates RTMP object; essentially for storing session information.
     */
    public RTMP() {
    }

    /**
     * Returns channel information for a given channel id.
     * 
     * @param channelId
     * @return channel info
     */
    private ChannelInfo getChannelInfo(int channelId) {
        ChannelInfo info = channels.putIfAbsent(channelId, new ChannelInfo());
        if (info == null) {
            info = channels.get(channelId);
        }
        return info;
    }

    /**
     * @return the encrypted
     */
    public boolean isEncrypted() {
        return encrypted;
    }

    /**
     * @param encrypted
     *            the encrypted to set
     */
    public void setEncrypted(boolean encrypted) {
        this.encrypted = encrypted;
    }

    /**
     * Return current state.
     *
     * @return State
     */
    public byte getState() {
        return state;
    }

    /**
     * Releases a packet.
     *
     * @param packet
     *            Packet to release
     */
    private void freePacket(Packet packet) {
        if (packet != null && packet.getData() != null) {
            packet.clearData();
        }
    }

    /**
     * Releases the channels.
     */
    private void freeChannels() {
        for (ChannelInfo info : channels.values()) {
            freePacket(info.getReadPacket());
            freePacket(info.getWritePacket());
        }
        channels.clear();
    }

    /**
     * Setter for state.
     *
     * @param state
     *            New state
     */
    public void setState(byte state) {
        this.state = state;
        if (state == STATE_DISCONNECTED) {
            freeChannels();
        }
    }

    /**
     * Setter for last read header.
     *
     * @param channelId
     *            Channel id
     * @param header
     *            Header
     */
    public void setLastReadHeader(int channelId, Header header) {
        getChannelInfo(channelId).setReadHeader(header);
    }

    /**
     * Return last read header for channel.
     *
     * @param channelId
     *            Channel id
     * @return Last read header
     */
    public Header getLastReadHeader(int channelId) {
        return getChannelInfo(channelId).getReadHeader();
    }

    /**
     * Setter for last written header.
     *
     * @param channelId
     *            Channel id
     * @param header
     *            Header
     */
    public void setLastWriteHeader(int channelId, Header header) {
        getChannelInfo(channelId).setWriteHeader(header);
    }

    /**
     * Return last written header for channel.
     *
     * @param channelId
     *            Channel id
     * @return Last written header
     */
    public Header getLastWriteHeader(int channelId) {
        return getChannelInfo(channelId).getWriteHeader();
    }

    /**
     * Setter for last read packet.
     *
     * @param channelId
     *            Channel id
     * @param packet
     *            Packet
     */
    public void setLastReadPacket(int channelId, Packet packet) {
        final ChannelInfo info = getChannelInfo(channelId);
        // grab last packet
        Packet prevPacket = info.getReadPacket();
        // set new one
        info.setReadPacket(packet);
        // free the previous packet
        freePacket(prevPacket);
    }

    /**
     * Return last read packet for channel.
     *
     * @param channelId
     *            Channel id
     * @return Last read packet for that channel
     */
    public Packet getLastReadPacket(int channelId) {
        return getChannelInfo(channelId).getReadPacket();
    }

    /**
     * Setter for last written packet.
     *
     * @param channelId
     *            Channel id
     * @param packet
     *            Last written packet
     */
    public void setLastWritePacket(int channelId, Packet packet) {
        // Disabled to help GC because we currently don't use the write packets
        /*
        Packet prevPacket = writePackets.put(channelId, packet);
        if (prevPacket != null && prevPacket.getData() != null) {
        	prevPacket.getData().release();
        	prevPacket.setData(null);
        }
        */
    }

    /**
     * Return packet that has been written last.
     *
     * @param channelId
     *            Channel id
     * @return Packet that has been written last
     */
    public Packet getLastWritePacket(int channelId) {
        return getChannelInfo(channelId).getWritePacket();
    }

    /**
     * Getter for write chunk size. Data is being read chunk-by-chunk.
     *
     * @return Read chunk size
     */
    public int getReadChunkSize() {
        return readChunkSize;
    }

    /**
     * Setter for read chunk size. Data is being read chunk-by-chunk.
     *
     * @param readChunkSize
     *            Value to set for property 'readChunkSize'.
     */
    public void setReadChunkSize(int readChunkSize) {
        this.readChunkSize = readChunkSize;
    }

    /**
     * Getter for write chunk size. Data is being written chunk-by-chunk.
     *
     * @return Write chunk size
     */
    public int getWriteChunkSize() {
        return writeChunkSize;
    }

    /**
     * Setter for write chunk size.
     *
     * @param writeChunkSize
     *            Write chunk size
     */
    public void setWriteChunkSize(int writeChunkSize) {
        this.writeChunkSize = writeChunkSize;
    }

    /**
     * Getter for encoding version.
     * 
     * @return Encoding version
     */
    public Encoding getEncoding() {
        return encoding;
    }

    /**
     * Setter for encoding version.
     * 
     * @param encoding
     *            Encoding version
     */
    public void setEncoding(Encoding encoding) {
        this.encoding = encoding;
    }

    public void setLastFullTimestampWritten(int channelId, int timer) {
        getChannelInfo(channelId).setWriteTimestamp(timer);
    }

    public int getLastFullTimestampWritten(int channelId) {
        return getChannelInfo(channelId).getWriteTimestamp();
    }

    /**
     * Sets the last "read" packet header for the given channel.
     * 
     * @param channelId channel id
     * @param header header
     */
    public void setLastReadPacketHeader(int channelId, Header header) {
        getChannelInfo(channelId).setReadPacketHeader(header);
    }

    /**
     * Returns the last "read" packet header for the given channel.
     * 
     * @param channelId channel id
     * @return Header
     */
    public Header getLastReadPacketHeader(int channelId) {
        return getChannelInfo(channelId).getReadPacketHeader();
    }

    LiveTimestampMapping getLastTimestampMapping(int channelId) {
        return getChannelInfo(channelId).getLiveTimestamp();
    }

    void setLastTimestampMapping(int channelId, LiveTimestampMapping mapping) {
        getChannelInfo(channelId).setLiveTimestamp(mapping);
    }

    void clearLastTimestampMapping(int... channelIds) {
        for (int channelId : channelIds) {
            getChannelInfo(channelId).setLiveTimestamp(null);
        }
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "RTMP [state=" + states[state] + ", encrypted=" + encrypted + ", readChunkSize=" + readChunkSize + ", writeChunkSize=" + writeChunkSize + ", encoding=" + encoding + "]";
    }

    /**
     * Class for mapping between clock time and stream time for live streams
     */
    final class LiveTimestampMapping {

        private final long clockStartTime;

        private final long streamStartTime;

        private boolean keyFrameNeeded;

        private long lastStreamTime;

        public LiveTimestampMapping(long clockStartTime, long streamStartTime) {
            this.clockStartTime = clockStartTime;
            this.streamStartTime = streamStartTime;
            this.keyFrameNeeded = true; // Always start with a key frame
            this.lastStreamTime = streamStartTime;
        }

        public long getStreamStartTime() {
            return streamStartTime;
        }

        public long getClockStartTime() {
            return clockStartTime;
        }

        public void setKeyFrameNeeded(boolean keyFrameNeeded) {
            this.keyFrameNeeded = keyFrameNeeded;
        }

        public boolean isKeyFrameNeeded() {
            return keyFrameNeeded;
        }

        public long getLastStreamTime() {
            return lastStreamTime;
        }

        public void setLastStreamTime(long lastStreamTime) {
            this.lastStreamTime = lastStreamTime;
        }
    }

    /**
     * Channel details
     */
    private final class ChannelInfo {

        // read header
        private Header readHeader;

        // write header
        private Header writeHeader;

        // packet header
        private Header readPacketHeader;

        // read packet
        private Packet readPacket;

        // written packet
        private Packet writePacket;

        // written timestamp
        private int writeTimestamp;

        // used for live streams
        private LiveTimestampMapping liveTimestamp;

        /**
         * @return the readHeader
         */
        public Header getReadHeader() {
            return readHeader;
        }

        /**
         * @param readHeader
         *            the readHeader to set
         */
        public void setReadHeader(Header readHeader) {
            this.readHeader = readHeader;
        }

        /**
         * @return the writeHeader
         */
        public Header getWriteHeader() {
            return writeHeader;
        }

        /**
         * @param writeHeader
         *            the writeHeader to set
         */
        public void setWriteHeader(Header writeHeader) {
            this.writeHeader = writeHeader;
        }

        /**
         * @return the readPacketHeader
         */
        public Header getReadPacketHeader() {
            return readPacketHeader;
        }

        /**
         * @param readPacketHeader
         *            the readPacketHeader to set
         */
        public void setReadPacketHeader(Header readPacketHeader) {
            this.readPacketHeader = readPacketHeader;
        }

        /**
         * @return the readPacket
         */
        public Packet getReadPacket() {
            return readPacket;
        }

        /**
         * @param readPacket
         *            the readPacket to set
         */
        public void setReadPacket(Packet readPacket) {
            this.readPacket = readPacket;
        }

        /**
         * @return the writePacket
         */
        public Packet getWritePacket() {
            return writePacket;
        }

        /**
         * @param writePacket
         *            the writePacket to set
         */
        @SuppressWarnings("unused")
        public void setWritePacket(Packet writePacket) {
            this.writePacket = writePacket;
        }

        /**
         * @return the writeTimestamp
         */
        public int getWriteTimestamp() {
            return writeTimestamp;
        }

        /**
         * @param writeTimestamp
         *            the writeTimestamp to set
         */
        public void setWriteTimestamp(int writeTimestamp) {
            this.writeTimestamp = writeTimestamp;
        }

        /**
         * @return the liveTimestamp
         */
        public LiveTimestampMapping getLiveTimestamp() {
            return liveTimestamp;
        }

        /**
         * @param liveTimestamp
         *            the liveTimestamp to set
         */
        public void setLiveTimestamp(LiveTimestampMapping liveTimestamp) {
            this.liveTimestamp = liveTimestamp;
        }
    }

}
