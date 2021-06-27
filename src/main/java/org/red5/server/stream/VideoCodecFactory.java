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
import org.red5.codec.IVideoStreamCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for video codecs. Creates and returns video codecs
 * 
 * @author The Red5 Project
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class VideoCodecFactory {
    /**
     * Object key
     */
    public static final String KEY = "videoCodecFactory";

    /**
     * Logger for video factory
     */
    private static Logger log = LoggerFactory.getLogger(VideoCodecFactory.class);

    /**
     * List of available codecs
     */
    private static List<IVideoStreamCodec> codecs = new ArrayList<IVideoStreamCodec>(3);

    /**
     * Setter for codecs
     *
     * @param codecs
     *            List of codecs
     */
    public void setCodecs(List<IVideoStreamCodec> codecs) {
        VideoCodecFactory.codecs = codecs;
    }

    /**
     * Create and return new video codec applicable for byte buffer data
     * 
     * @param data
     *            Byte buffer data
     * @return Video codec
     */
    public static IVideoStreamCodec getVideoCodec(IoBuffer data) {
        IVideoStreamCodec result = null;
        //get the codec identifying byte
        int codecId = data.get() & 0x0f;
        try {
            switch (codecId) {
                case 2: //sorenson 
                    result = (IVideoStreamCodec) Class.forName("org.red5.codec.SorensonVideo").newInstance();
                    break;
                case 3: //screen video
                    result = (IVideoStreamCodec) Class.forName("org.red5.codec.ScreenVideo").newInstance();
                    break;
                case 6: //screen video 2
                    result = (IVideoStreamCodec) Class.forName("org.red5.codec.ScreenVideo2").newInstance();
                    break;
                case 7: //avc/h.264 video
                    result = (IVideoStreamCodec) Class.forName("org.red5.codec.AVCVideo").newInstance();
                    break;
            }
        } catch (Exception ex) {
            log.error("Error creating codec instance", ex);
        }
        data.rewind();
        //if codec is not found do the old-style loop
        if (result == null) {
            for (IVideoStreamCodec storedCodec : codecs) {
                IVideoStreamCodec codec;
                // XXX: this is a bit of a hack to create new instances of the
                // configured video codec for each stream
                try {
                    codec = storedCodec.getClass().newInstance();
                } catch (Exception e) {
                    log.error("Could not create video codec instance", e);
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

    //	private boolean isScreenVideo(byte first) {
    //    	log.debug("Trying ScreenVideo");
    //		boolean result = ((first & 0x0f) == 3);
    //		return result;
    //	}
    //	
    //	private boolean isSorenson(byte first) {
    //    	log.debug("Trying Sorenson");
    //		boolean result = ((first & 0x0f) == 2);
    //		return result;
    //	}

}
