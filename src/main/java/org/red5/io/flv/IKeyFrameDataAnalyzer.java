/*
 * RED5 Open Source Media Server - https://github.com/Red5/ Copyright 2006-2016 by respective authors (see below). All rights reserved. Licensed under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless
 * required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package org.red5.io.flv;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Analyzes key frame data.
 */
public interface IKeyFrameDataAnalyzer {

    /**
     * Analyze and return keyframe metadata.
     *
     * @return Metadata object
     */
    public KeyFrameMeta analyzeKeyFrames();

    /**
     * Keyframe metadata.
     */
    public static class KeyFrameMeta implements Serializable {

        private static final long serialVersionUID = 5436632873705625365L;

        /**
         * Video codec id.
         */
        public int videoCodecId = -1;

        /**
         * Audio codec id.
         */
        public int audioCodecId = -1;

        /**
         * Duration in milliseconds
         */
        public long duration;

        /**
         * Only audio frames?
         */
        public boolean audioOnly;

        /**
         * Keyframe timestamps
         */
        public int timestamps[];

        /**
         * Keyframe positions
         */
        public long positions[];

        /*
         * (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return "KeyFrameMeta [videoCodecId=" + videoCodecId + ", audioCodecId=" + audioCodecId + ", duration=" + duration + ", audioOnly=" + audioOnly + ", timestamps=" + Arrays.toString(timestamps) + ", positions=" + Arrays.toString(positions) + "]";
        }
    }
}
