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

import org.red5.server.api.scope.IScope;

/**
 * Interface for handlers that control access to stream playback.
 * 
 * @author The Red5 Project
 * @author Joachim Bauch (jojo@struktur.de)
 */
public interface IStreamPlaybackSecurity {

    /**
     * Check if playback of a stream with the given name is allowed.
     * 
     * @param scope
     *            Scope the stream is about to be played back from.
     * @param name
     *            Name of the stream to play.
     * @param start
     *            Position to start playback from (in milliseconds).
     * @param length
     *            Duration to play (in milliseconds).
     * @param flushPlaylist
     *            Flush playlist?
     * @return <pre>
     * True
     * </pre>
     * 
     *         if playback is allowed, otherwise
     * 
     *         <pre>
     * False
     * </pre>
     */
    public boolean isPlaybackAllowed(IScope scope, String name, int start, int length, boolean flushPlaylist);

}
