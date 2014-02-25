/*
 * RED5 Open Source Flash Server - http://code.google.com/p/red5/
 * 
 * Copyright 2006-2013 by respective authors (see below). All rights reserved.
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

package org.red5.server.api.stream;

/**
 * Stream codec information
 */
public interface IStreamCodecInfo {
    /**
     * Has audio support?
     * @return           <code>true</code> if stream codec has audio support, <code>false</code> otherwise
     */
    boolean hasAudio();

    /**
     * Has video support?
     * @return           <code>true</code> if stream codec has video support, <code>false</code> otherwise
     */
	boolean hasVideo();

	/**
     * Getter for audio codec name
     *
     * @return Audio codec name
     */
    String getAudioCodecName();

	/**
     * Getter for video codec name
     *
     * @return Video codec name
     */
    String getVideoCodecName();

	/**
     * Return video codec
     *
     * @return Video codec used by stream codec
     */
    IVideoStreamCodec getVideoCodec();
    
	/**
     * Return audio codec
     *
     * @return Audio codec used by stream codec
     */
    IAudioStreamCodec getAudioCodec();    
    
}
