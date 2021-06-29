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
 * A play list controller that controls the order of play items.
 * 
 * @author The Red5 Project
 * @author Steven Gong (steven.gong@gmail.com)
 */
public interface IPlaylistController {
    /**
     * Get next item to play.
     * 
     * @param playlist
     *            The related play list.
     * @param itemIndex
     *            The current item index. <tt>-1</tt> indicates to retrieve the first item for play.
     * @return The next item index to play. <tt>-1</tt> reaches the end.
     */
    int nextItem(IPlaylist playlist, int itemIndex);

    /**
     * Get previous item to play.
     * 
     * @param playlist
     *            The related play list.
     * @param itemIndex
     *            The current item index. <tt>IPlaylist.itemSize</tt> indicated to retrieve the last item for play.
     * @return The previous item index to play. <tt>-1</tt> reaches the beginning.
     */
    int previousItem(IPlaylist playlist, int itemIndex);
}
