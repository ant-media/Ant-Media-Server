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

package org.red5.server.stream;

import java.util.ArrayList;
import java.util.List;

import org.apache.mina.core.buffer.IoBuffer;
import org.red5.codec.IAudioStreamCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for audio codecs. Creates and returns audio codecs
 * 
 * @author The Red5 Project
 * @author Vladimir Hmelyoff (vlhm@splitmedialabs.com)
 */
public class AudioCodecFactory {
    /**
     * Object key
     */
    public static final String KEY = "audioCodecFactory";

    /**
     * Logger for audio factory
     */
    private static Logger log = LoggerFactory.getLogger(AudioCodecFactory.class);

    /**
     * List of available codecs
     */
    private static List<IAudioStreamCodec> codecs = new ArrayList<IAudioStreamCodec>(1);

    /**
     * Setter for codecs
     *
     * @param codecs
     *            List of codecs
     */
    public void setCodecs(List<IAudioStreamCodec> codecs) {
        AudioCodecFactory.codecs = codecs;
    }

    /**
     * Create and return new audio codec applicable for byte buffer data
     * 
     * @param data
     *            Byte buffer data
     * @return audio codec
     */
    public static IAudioStreamCodec getAudioCodec(IoBuffer data) {
        IAudioStreamCodec result = null;
        try {
            //get the codec identifying byte
            int codecId = (data.get() & 0xf0) >> 4;
            switch (codecId) {
                case 10: //aac 
                    result = (IAudioStreamCodec) Class.forName("org.red5.codec.AACAudio").newInstance();
                    break;
            // TODO add SPEEX support?
            }
            data.rewind();
        } catch (Exception ex) {
            log.error("Error creating codec instance", ex);
        }
        //if codec is not found do the old-style loop
        if (result == null) {
            for (IAudioStreamCodec storedCodec : codecs) {
                IAudioStreamCodec codec;
                // XXX: this is a bit of a hack to create new instances of the
                // configured audio codec for each stream
                try {
                    codec = storedCodec.getClass().newInstance();
                } catch (Exception e) {
                    log.error("Could not create audio codec instance", e);
                    continue;
                }
                if (codec.canHandleData(data)) {
                    result = codec;
                    break;
                }
            }
        }
        return result;
    }

}
