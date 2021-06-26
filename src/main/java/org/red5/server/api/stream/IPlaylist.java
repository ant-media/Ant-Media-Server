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
 * Playlist
 */
public interface IPlaylist {
    /**
     * Add an item to the list.
     * 
     * @param item
     *            Playlist item
     */
    void addItem(IPlayItem item);

    /**
     * Add an item to specific index.
     * 
     * @param item
     *            Playlist item
     * @param index
     *            Index in list
     */
    void addItem(IPlayItem item, int index);

    /**
     * Remove an item from list.
     * 
     * @param index
     *            Index in list
     */
    void removeItem(int index);

    /**
     * Remove all items.
     */
    void removeAllItems();

    /**
     * Return number of items in list
     *
     * @return Number of items in list
     */
    int getItemSize();

    /**
     * Get currently playing item index.
     * 
     * @return Currently playing item index.
     */
    int getCurrentItemIndex();

    /**
     * Get currently playing item
     * 
     * @return Item
     */
    IPlayItem getCurrentItem();

    /**
     * Get the item according to the index.
     * 
     * @param index
     *            Item index
     * @return Item at that index in list
     */
    IPlayItem getItem(int index);

    /**
     * Check if the playlist has more items after the currently playing one.
     * 
     * @return <pre>
     * true
     * </pre>
     * 
     *         if more items are available,
     * 
     *         <pre>
     * false
     * </pre>
     * 
     *         otherwise
     */
    boolean hasMoreItems();

    /**
     * Go for the previous played item.
     */
    void previousItem();

    /**
     * Go for next item decided by controller logic.
     */
    void nextItem();

    /**
     * Set the current item for playing.
     * 
     * @param index
     *            Position in list
     */
    void setItem(int index);

    /**
     * Whether items are randomly played.
     * 
     * @return <pre>
     * true
     * </pre>
     * 
     *         if shuffle is on for this list,
     * 
     *         <pre>
     * false
     * </pre>
     * 
     *         otherwise
     */
    boolean isRandom();

    /**
     * Set whether items should be randomly played.
     * 
     * @param random
     *            Shuffle flag
     */
    void setRandom(boolean random);

    /**
     * Whether rewind the list.
     * 
     * @return <pre>
     * true
     * </pre>
     * 
     *         if playlist is rewind on end,
     * 
     *         <pre>
     * false
     * </pre>
     * 
     *         otherwise
     */
    boolean isRewind();

    /**
     * Set whether rewind the list.
     * 
     * @param rewind
     *            New vallue for rewind flag
     */
    void setRewind(boolean rewind);

    /**
     * Whether repeat playing an item.
     * 
     * @return <pre>
     * true
     * </pre>
     * 
     *         if repeat mode is on for this playlist,
     * 
     *         <pre>
     * false
     * </pre>
     * 
     *         otherwise
     */
    boolean isRepeat();

    /**
     * Set whether repeat playing an item.
     * 
     * @param repeat
     *            New value for item playback repeat flag
     */
    void setRepeat(boolean repeat);

    /**
     * Set list controller.
     * 
     * @param controller
     *            Playlist controller
     */
    void setPlaylistController(IPlaylistController controller);
}
