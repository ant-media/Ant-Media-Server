/*
 * RED5 Open Source Media Server - https://github.com/Red5/ Copyright 2006-2016 by respective authors (see below). All rights reserved. Licensed under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless
 * required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package org.red5.codec;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.mina.core.buffer.IoBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Red5 video codec for the AVC (h264) video format. Stores DecoderConfigurationRecord and last keyframe.
 *
 * @author Tiago Jacobs (tiago@imdt.com.br)
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class HEVCVideo extends AVCVideo {

    private static Logger log = LoggerFactory.getLogger(HEVCVideo.class);

    /**
     * AVC video codec constant
     */
    static final String CODEC_NAME = "HEVC";


    /** {@inheritDoc} */
    @Override
    public String getName() {
        return CODEC_NAME;
    }

    /** {@inheritDoc} */
    @Override
    public boolean canDropFrames() {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean canHandleData(IoBuffer data) {
        boolean result = false;
        if (data.limit() > 0) {
            // read the first byte and ensure its HEVC / h.265 type
            result = ((data.get() & 0x0f) == VideoCodec.HEVC.getId());
            data.rewind();
        }
        return result;
    }



    /** {@inheritDoc} */
    @Override
    public boolean addData(IoBuffer data, int timestamp) {
        //log.trace("addData timestamp: {} remaining: {}", timestamp, data.remaining());
        if (data.hasRemaining()) {
            // mark
            int start = data.position();
            // get frame type
            byte frameType = data.get();
            byte hevcType = data.get();
            if ((frameType & 0x0f) == VideoCodec.HEVC.getId()) {
                // check for keyframe
                if ((frameType & 0xf0) == FLV_FRAME_KEY) {
                    //log.trace("Key frame");
                    if (log.isDebugEnabled()) {
                        log.debug("Keyframe - HEVC type: {}", hevcType);
                    }
                    // rewind
                    data.rewind();
                    switch (hevcType) {
                        case 1: // keyframe
                            if (timestamp != keyframeTimestamp) {
                                // new keyframe
                                keyframeTimestamp = timestamp;
                                // if its a new keyframe, clear keyframe and interframe collections
                                softReset();
                            }
                            // store keyframe
                            keyframes.add(new FrameData(data));
                            break;
                        case 0: // configuration
                            // Store AVCDecoderConfigurationRecord data
                            decoderConfiguration.setData(data);
                            softReset();
                            break;
                    }
                } else if (bufferInterframes) {
                    if (log.isDebugEnabled()) {
                        log.debug("Interframe - HEVC type: {}", hevcType);
                    }
                    // rewind
                    data.rewind();
                    try {
                        int lastInterframe = numInterframes.getAndIncrement();
                        if (lastInterframe < interframes.size()) {
                            interframes.get(lastInterframe).setData(data);
                        } else {
                            interframes.add(new FrameData(data));
                        }
                    } catch (Throwable e) {
                        log.error("Failed to buffer interframe", e);
                    }
                }
            } else {
                // not AVC data
                log.debug("Non-hevc data, rejecting");
                // go back to where we started
                data.position(start);
                return false;
            }
            // go back to where we started
            data.position(start);
        }
        return true;
    }

}
