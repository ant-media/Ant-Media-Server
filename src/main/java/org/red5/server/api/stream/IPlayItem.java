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

import org.red5.server.messaging.IMessageInput;

/**
 * Playlist item. Each playlist item has name, start time, length in milliseconds and message input source.
 */
public interface IPlayItem {

    /**
     * Get name of item. The VOD or Live stream provider is found according to this name.
     * 
     * @return the name
     */
    String getName();

    /**
     * Start time in milliseconds.
     * 
     * @return start time
     */
    long getStart();

    /**
     * Play length in milliseconds.
     * 
     * @return length in milliseconds
     */
    long getLength();

    /**
     * Get a message input for play. This object overrides the default algorithm for finding the appropriate VOD or Live stream provider according to the item name.
     * 
     * @return message input
     */
    IMessageInput getMessageInput();
}
