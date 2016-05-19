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

package org.red5.server.api.stream;

/**
 * Extends stream to add methods for on demand access.
 * 
 * @author The Red5 Project
 * @author Luke Hubbard (luke@codegent.com)
 */
public interface IOnDemandStream extends IStream {

    /**
     * Start playback
     */
    public void play();

    /**
     * Start playback with a given maximum duration.
     * 
     * @param length
     *            maximum duration in milliseconds
     */
    public void play(int length);

    /**
     * Seek to the keyframe nearest to position
     * 
     * @param position
     *            position in milliseconds
     */
    public void seek(int position);

    /**
     * Pause the stream
     */
    public void pause();

    /**
     * Resume a paused stream
     */
    public void resume();

    /**
     * Stop the stream, this resets the position to the start
     */
    public void stop();

    /**
     * Is the stream paused
     * 
     * @return true if the stream is paused
     */
    public boolean isPaused();

    /**
     * Is the stream stopped
     * 
     * @return true if the stream is stopped
     */
    public boolean isStopped();

    /**
     * Is the stream playing
     * 
     * @return true if the stream is playing
     */
    public boolean isPlaying();

}