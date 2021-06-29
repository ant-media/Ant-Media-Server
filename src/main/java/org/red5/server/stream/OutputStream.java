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

import org.red5.server.net.rtmp.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Output stream that consists of audio, video and data channels
 *
 * @see org.red5.server.net.rtmp.Channel
 */
public class OutputStream {
    /**
     * Logger
     */
    protected static Logger log = LoggerFactory.getLogger(OutputStream.class);

    /**
     * Video channel
     */
    private Channel video;

    /**
     * Audio channel
     */
    private Channel audio;

    /**
     * Data channel
     */
    private Channel data;

    /**
     * Creates output stream from channels
     *
     * @param video
     *            Video channel
     * @param audio
     *            Audio channel
     * @param data
     *            Data channel
     */
    public OutputStream(Channel video, Channel audio, Channel data) {
        this.video = video;
        this.audio = audio;
        this.data = data;
    }

    /**
     * Closes audion, video and data channels
     */
    public void close() {
        video.close();
        audio.close();
        data.close();
    }

    /**
     * Getter for audio channel
     *
     * @return Audio channel
     */
    public Channel getAudio() {
        return audio;
    }

    /**
     * Getter for data channel
     *
     * @return Data channel
     */
    public Channel getData() {
        return data;
    }

    /**
     * Getter for video channel
     *
     * @return Video channel
     */
    public Channel getVideo() {
        return video;
    }
}
