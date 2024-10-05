/*
 * RED5 Open Source Media Server - https://github.com/Red5/ Copyright 2006-2016 by respective authors (see below). All rights reserved. Licensed under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless
 * required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package org.red5.io.flv.meta;

import java.util.HashMap;
import java.util.Map;

/**
 * MetaData Implementation
 *
 * @author The Red5 Project
 * @author Dominick Accattato (daccattato@gmail.com)
 *
 *         Example:
 *
 *         // private boolean canSeekToEnd = true; // private int videocodecid = 4; // private int framerate = 15; // private int videodatarate = 600; // private int height; // private int width = 320; // private double duration = 7.347;
 * @param <K>
 *            key type
 * @param <V>
 *            value type
 */
public class MetaData<K, V> extends HashMap<String, Object> implements IMetaData<Object, Object> {

    private static final long serialVersionUID = -5681069577717669925L;

    /**
     * Cue points array. Cue points can be injected on fly like any other data even on client-side.
     */
    IMetaCue[] cuePoints; //CuePoint array

    /** MetaData constructor */
    public MetaData() {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean getCanSeekToEnd() {
        return (Boolean) this.get("canSeekToEnd");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setCanSeekToEnd(boolean b) {
        this.put("canSeekToEnd", b);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getVideoCodecId() {
        return (Integer) this.get("videocodecid");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setVideoCodecId(int id) {
        this.put("videocodecid", id);
    }

    @Override
    public int getAudioCodecId() {
        return (Integer) this.get("audiocodecid");
    }

    @Override
    public void setAudioCodecId(int id) {
        this.put("audiocodecid", id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getFrameRate() {
        return (Double) this.get("framerate");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setFrameRate(double rate) {
        this.put("framerate", Double.valueOf(rate));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getVideoDataRate() {
        return (Integer) this.get("videodatarate");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setVideoDataRate(int rate) {
        this.put("videodatarate", rate);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getWidth() {
        return (Integer) this.get("width");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setWidth(int w) {
        this.put("width", w);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getDuration() {
        return (Double) this.get("duration");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setDuration(double d) {
        this.put("duration", d);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getHeight() {
        return (Integer) this.get("height");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setHeight(int h) {
        this.put("height", h);
    }

    /**
     * Sets the Meta Cue Points
     *
     * @param cuePoints
     *            The cuePoints to set.
     */
    @Override
    public void setMetaCue(IMetaCue[] cuePoints) {
        Map<String, Object> cues = new HashMap<String, Object>();
        this.cuePoints = cuePoints;

        int j = 0;
        for (j = 0; j < this.cuePoints.length; j++) {
            cues.put(String.valueOf(j), this.cuePoints[j]);
        }

        //		"CuePoints", cuePointData
        //					'0',	MetaCue
        //							name, "test"
        //							type, "event"
        //							time, "0.1"
        //					'1',	MetaCue
        //							name, "test1"
        //							type, "event1"
        //							time, "0.5"

        this.put("cuePoints", cues);
    }

    /**
     * Return array of cue points
     *
     * @return Array of cue points
     */
    @Override
    public IMetaCue[] getMetaCue() {
        return this.cuePoints;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "MetaData{" + "cuePoints=" + (cuePoints == null ? null : this.get("cuePoints")) + '}';
    }
}
