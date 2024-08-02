/*
 * RED5 Open Source Media Server - https://github.com/Red5/ Copyright 2006-2016 by respective authors (see below). All rights reserved. Licensed under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless
 * required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package org.red5.io;

import java.io.File;

import org.red5.io.flv.IKeyFrameDataAnalyzer.KeyFrameMeta;

/**
 * Interface defining a cache for keyframe metadata informations.
 *
 * @author The Red5 Project
 * @author Joachim Bauch (jojo@struktur.de)
 */
public interface IKeyFrameMetaCache {

    /**
     * Load keyframe informations for the given file.
     *
     * @param file
     *            File to load informations for.
     * @return The keyframe informations or <code>null</code> if none exist.
     */
    public KeyFrameMeta loadKeyFrameMeta(File file);

    /**
     * Remove keyframe information for given file. Need to update keyframe cache when re-writing file.
     *
     * @param file
     *            File to remove information for.
     */
    public void removeKeyFrameMeta(File file);

    /**
     * Store keyframe informations for the given file.
     *
     * @param file
     *            File to save informations for.
     * @param meta
     *            Keyframe informations for this file.
     */
    public void saveKeyFrameMeta(File file, KeyFrameMeta meta);

}
