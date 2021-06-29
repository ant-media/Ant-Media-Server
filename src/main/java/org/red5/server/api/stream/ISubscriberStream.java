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

import java.io.IOException;

import org.red5.server.api.scheduling.IScheduledJob;

/**
 * ISubscriberStream is a stream from subscriber's point of view. That is, it provides methods for common stream operations like play, pause or seek.
 */
public interface ISubscriberStream extends IClientStream {
    /**
     * Start playing.
     * 
     * @throws IOException
     *             if an IO error occurred while starting to play the stream
     */
    void play() throws IOException;

    /**
     * Pause at a position for current playing item.
     * 
     * @param position
     *            Position for pause in millisecond.
     */
    void pause(int position);

    /**
     * Resume from a position for current playing item.
     * 
     * @param position
     *            Position for resume in millisecond.
     */
    void resume(int position);

    /**
     * Stop playing.
     */
    void stop();

    /**
     * Seek into a position for current playing item.
     * 
     * @param position
     *            Position for seek in millisecond.
     * @throws OperationNotSupportedException
     *             if the stream doesn't support seeking.
     */
    void seek(int position) throws OperationNotSupportedException;

    /**
     * Check if the stream is currently paused.
     * 
     * @return stream is paused
     */
    boolean isPaused();

    /**
     * Should the stream send video to the client?
     * 
     * @param receive
     *            toggle
     */
    void receiveVideo(boolean receive);

    /**
     * Should the stream send audio to the client?
     * 
     * @param receive
     *            toggle
     */
    void receiveAudio(boolean receive);

    /**
     * Return the streams state enum.
     * 
     * @return current state
     */
    public StreamState getState();

    /**
     * Sets the streams state enum.
     * 
     * @param state
     *            sets current state
     */
    public void setState(StreamState state);

    /**
     * Notification of state change and associated parameters.
     * 
     * @param state
     *            new state
     * @param changed
     *            parameters associated with the change
     */
    public void onChange(final StreamState state, final Object... changed);

    /**
     * Schedule a job to be executed only once after a 10ms delay.
     * 
     * @param job
     *            scheduled job
     * @return jobName
     */
    public String scheduleOnceJob(IScheduledJob job);

    /**
     * Schedule a job to be executed regularly at the given interval.
     * 
     * @param job
     *            scheduled job
     * @param interval
     *            interval
     * @return jobName
     */
    public String scheduleWithFixedDelay(IScheduledJob job, int interval);

    /**
     * Cancels a scheduled job by name.
     * 
     * @param jobName
     *            job name
     */
    public void cancelJob(String jobName);

}
