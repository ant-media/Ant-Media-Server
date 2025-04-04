/*
 * RED5 Open Source Media Server - https://github.com/Red5/ Copyright 2006-2016 by respective authors (see below). All rights reserved. Licensed under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless
 * required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package org.red5.codec;

import org.apache.mina.core.buffer.IoBuffer;

/**
 * Represents a Video codec and its associated decoder configuration.
 */
public interface IVideoStreamCodec {

    /**
     * FLV frame marker constant
     */
    static final byte FLV_FRAME_KEY = 0x10;

    /**
     * @return the name of the video codec.
     */
    public String getName();

    /**
     * Reset the codec to its initial state.
     */
    public void reset();

    /**
     * Check if the codec supports frame dropping.
     * 
     * @return if the codec supports frame dropping.
     */
    public boolean canDropFrames();

    /**
     * Returns true if the codec knows how to handle the passed stream data.
     * 
     * @param data
     *            some sample data to see if this codec can handle it
     * @return can this code handle the data.
     */
    public boolean canHandleData(IoBuffer data);

    /**
     * Update the state of the codec with the passed data.
     * 
     * @param data
     *            data to tell the codec we're adding
     * @return true for success. false for error
     */
    public boolean addData(IoBuffer data);

    /**
     * Update the state of the codec with the passed data.
     * 
     * @param data
     *            data to tell the codec we're adding
     * @param timestamp time associated with the data
     * @return true for success. false for error
     */
    public boolean addData(IoBuffer data, int timestamp);

    /**
     * Returns keyframe data.
     * 
     * @return the data for a keyframe
     */
    public IoBuffer getKeyframe();

    /**
     * Returns all the keyframe data.
     * 
     * @return array of keyframe data
     */
    public FrameData[] getKeyframes();

    /**
     * Returns information used to configure the decoder.
     * 
     * @return the data for decoder setup
     */
    public IoBuffer getDecoderConfiguration();

    /**
     * Returns the number of interframes collected from last keyframe.
     * 
     * @return number of interframes
     */
    public int getNumInterframes();

    /**
     * Gets data of interframe with the specified index.
     *
     * @param index
     *            of interframe
     * @return data of the interframe or null if index is not valid
     */
    public FrameData getInterframe(int index);

    /**
     * Holder for video frame data.
     */
    public final static class FrameData {

        private byte[] frame;

        public FrameData() {
        }

        public FrameData(IoBuffer data) {
            setData(data);
        }

        /**
         * Makes a copy of the incoming bytes and places them in an IoBuffer. No flip or rewind is performed on the source data.
         * 
         * @param data
         *            data
         */
        public void setData(IoBuffer data) {
            if (frame != null) {
                frame = null;
            }
            frame = new byte[data.limit()];
            data.get(frame);
        }

        public IoBuffer getFrame() {
            return frame == null ? null : IoBuffer.wrap(frame).asReadOnlyBuffer();
        }

    }
}
