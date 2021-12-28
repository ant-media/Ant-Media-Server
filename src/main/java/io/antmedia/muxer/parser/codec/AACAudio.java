/*
 * RED5 Open Source Media Server - https://github.com/Red5/ Copyright 2006-2016 by respective authors (see below). All rights reserved. Licensed under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless
 * required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package io.antmedia.muxer.parser.codec;

import org.apache.mina.core.buffer.IoBuffer;
import org.red5.codec.AudioCodec;
import org.red5.codec.IAudioStreamCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Red5 audio codec for the AAC audio format.
 *
 * Stores the decoder configuration
 * 
 * @author Paul Gregoire (mondain@gmail.com)
 * @author Wittawas Nakkasem (vittee@hotmail.com)
 * @author Vladimir Hmelyoff (vlhm@splitmedialabs.com)
 */
public class AACAudio implements IAudioStreamCodec {

    private static Logger log = LoggerFactory.getLogger(AACAudio.class);

    public static final int[] AAC_SAMPLERATES = { 96000, 88200, 64000, 48000, 44100, 32000, 24000, 22050, 16000, 12000, 11025, 8000, 7350 };

    /**
     * AAC audio codec constant
     */
    static final String CODEC_NAME = "AAC";

    /**
     * Block of data (AAC DecoderConfigurationRecord)
     */
    private byte[] blockDataAACDCR;

    /** Constructs a new AACAudio */
    public AACAudio() {
        this.reset();
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return CODEC_NAME;
    }

    /** {@inheritDoc} */
    @Override
    public void reset() {
        blockDataAACDCR = null;
    }

    /** {@inheritDoc} */
    @Override
    public boolean canHandleData(IoBuffer data) {
        if (data.limit() == 0) {
            // Empty buffer
            return false;
        }
        byte first = data.get();
        boolean result = (((first & 0xf0) >> 4) == AudioCodec.AAC.getId());
        data.rewind();
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public boolean addData(IoBuffer data) {
        if (data.hasRemaining()) {
            // mark
            int start = data.position();
            // ensure we are at the beginning
            data.rewind();
            byte frameType = data.get();
            log.trace("Frame type: {}", frameType);
            byte header = data.get();
            // if we have header == 0, it means this is Decoder config
            if (header == 0) {
            	log.debug("AACDecoderConfigurationRecord is received");
                if ((((frameType & 0xf0) >> 4) == AudioCodec.AAC.getId())) {
                    // back to the beginning
                    data.rewind();
                    blockDataAACDCR = new byte[data.remaining()];
                    data.get(blockDataAACDCR);
                }
            }
            data.position(start);
        }
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public IoBuffer getDecoderConfiguration() {
        if (blockDataAACDCR == null) {
            return null;
        }
        IoBuffer result = IoBuffer.allocate(4);
        result.setAutoExpand(true);
        result.put(blockDataAACDCR);
        result.rewind();
        return result;
    }
}
