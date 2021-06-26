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

import org.red5.server.api.IConnection;
import org.red5.server.api.scope.IScopeService;

/**
 * This interface represents the stream methods that can be called throug RTMP.
 */
public interface IStreamService extends IScopeService {

    public static String BEAN_NAME = "streamService";

    /**
     * Create a stream and return a corresponding id.
     * 
     * @return ID of created stream
     */
    public Number createStream();

    /**
     * Create a stream and return a corresponding id.
     * 
     * @param streamId
     *            Stream id
     * @return ID of created stream
     */
    public Number createStream(Number streamId);

    /**
     * Close the stream but not deallocate the resources.
     * 
     * @param connection
     *            Connection
     * @param streamId
     *            Stream id
     */
    public void closeStream(IConnection connection, Number streamId);

    /**
     * Close the stream if not been closed. Deallocate the related resources.
     * 
     * @param streamId
     *            Stream id
     */
    public void deleteStream(Number streamId);

    /**
     * Called by FMS.
     * 
     * @param streamId
     *            Stream id
     */
    public void initStream(Number streamId);

    /**
     * Called by FMS.
     * 
     * @param streamId
     *            Stream id
     * @param idk
     *            I dont know what this is yet
     */
    public void initStream(Number streamId, Object idk);

    /**
     * Called by FME.
     * 
     * @param streamName
     *            stream name
     */
    public void releaseStream(String streamName);

    /**
     * Delete stream
     * 
     * @param conn
     *            Stream capable connection
     * @param streamId
     *            Stream id
     */
    public void deleteStream(IStreamCapableConnection conn, Number streamId);

    /**
     * Play stream without initial stop
     * 
     * @param dontStop
     *            Stoppage flag
     */
    public void play(Boolean dontStop);

    /**
     * Play stream with name
     * 
     * @param name
     *            Stream name
     */
    public void play(String name);

    /**
     * Play stream with name from start position
     * 
     * @param name
     *            Stream name
     * @param start
     *            Start position
     */
    public void play(String name, int start);

    /**
     * Play stream with name from start position and for given amount if time
     * 
     * @param name
     *            Stream name
     * @param start
     *            Start position
     * @param length
     *            Playback length
     */
    public void play(String name, int start, int length);

    /**
     * Publishes stream from given position for given amount of time
     * 
     * @param name
     *            Stream published name
     * @param start
     *            Start position
     * @param length
     *            Playback length
     * @param flushPlaylist
     *            Flush playlist?
     */
    public void play(String name, int start, int length, boolean flushPlaylist);

    /**
     * Publishes stream with given name
     * 
     * @param name
     *            Stream published name
     */
    public void publish(String name);

    /**
     * Publishes stream with given name and mode
     * 
     * @param name
     *            Stream published name
     * @param mode
     *            Stream publishing mode
     */
    public void publish(String name, String mode);

    /**
     * Publish
     * 
     * @param dontStop
     *            Whether need to stop first
     */
    public void publish(Boolean dontStop);

    /**
     * Seek to position
     * 
     * @param position
     *            Seek position
     */
    public void seek(int position);

    /**
     * Pauses playback
     * 
     * @param pausePlayback
     *            Pause or resume flash
     * @param position
     *            Pause position
     */
    public void pause(Boolean pausePlayback, int position);

    /**
     * Undocumented Flash Plugin 10 call, assuming to be the alias to pause(boolean, int)
     * 
     * @param pausePlayback
     *            Pause or resume flash
     * @param position
     *            Pause position
     */
    public void pauseRaw(Boolean pausePlayback, int position);

    /**
     * Can recieve video?
     * 
     * @param receive
     *            Boolean flag
     */
    public void receiveVideo(boolean receive);

    /**
     * Can recieve audio?
     * 
     * @param receive
     *            Boolean flag
     */
    public void receiveAudio(boolean receive);

}
