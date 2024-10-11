/*
 * RED5 Open Source Media Server - https://github.com/Red5/ Copyright 2006-2016 by respective authors (see below). All rights reserved. Licensed under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless
 * required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package org.red5.io.flv.meta;

/**
 * FLV MetaData interface
 * 
 * @author The Red5 Project
 * @author Dominick Accattato (daccattato@gmail.com)
 * 
 *         Sample Data: private boolean canSeekToEnd = true; private int videocodecid = 4; private int framerate = 15; private int videodatarate = 400; private int height = 215; private int width = 320; private int duration = 7.347;
 * @param <K>
 *            key type
 * @param <V>
 *            value type
 */
public interface IMetaData<K, V> extends IMeta {

    /**
     * Returns a boolean depending on whether the video can seek to end
     * 
     * @return <code>true</code> if file is seekable to the end, <code>false</code> otherwise
     */
    public boolean getCanSeekToEnd();

    /**
     * Sets whether a video can seek to end
     * 
     * @param b
     *            <code>true</code> if file is seekable to the end, <code>false</code> otherwise
     */
    public void setCanSeekToEnd(boolean b);

    /**
     * Returns the video codec id
     * 
     * @return Video codec id
     */
    public int getVideoCodecId();

    /**
     * Sets the video codec id
     * 
     * @param id
     *            Video codec id
     */
    public void setVideoCodecId(int id);

    public int getAudioCodecId();

    public void setAudioCodecId(int id);

    /**
     * Returns the framerate.
     * 
     * @return FLV framerate in frames per second
     */
    public double getFrameRate();

    /**
     * Sets the framerate.
     * 
     * @param rate
     *            FLV framerate in frames per second
     */
    public void setFrameRate(double rate);

    /**
     * Returns the videodatarate
     * 
     * @return Video data rate
     */
    public int getVideoDataRate();

    /**
     * Sets the videodatarate
     * 
     * @param rate
     *            Video data rate
     */
    public void setVideoDataRate(int rate);

    /**
     * Returns the height
     * 
     * @return height Video height
     */
    public int getHeight();

    /**
     * Sets the height
     * 
     * @param h
     *            Video height
     */
    public void setHeight(int h);

    /**
     * Returns the width Video width
     * 
     * @return width
     */
    public int getWidth();

    /**
     * Sets the width
     * 
     * @param w
     *            Video width
     */
    public void setWidth(int w);

    /**
     * Returns the duration.
     * 
     * @return duration Video duration in seconds
     */
    public double getDuration();

    /**
     * Sets the duration.
     * 
     * @param d
     *            Video duration in seconds
     */
    public void setDuration(double d);

    /**
     * Sets the cue points
     * 
     * @param metaCue
     *            Cue points
     */
    public void setMetaCue(IMetaCue[] metaCue);

    /**
     * Gets the cue points
     * 
     * @return Cue points
     */
    public IMetaCue[] getMetaCue();
}
