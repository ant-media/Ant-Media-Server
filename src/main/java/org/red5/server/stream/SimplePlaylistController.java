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

package org.red5.server.stream;

import java.util.Random;

import org.red5.server.api.stream.IPlaylist;
import org.red5.server.api.stream.IPlaylistController;

/**
 * Simple playlist controller implementation
 */
public class SimplePlaylistController implements IPlaylistController {

    /** {@inheritDoc} */
    public int nextItem(IPlaylist playlist, int itemIndex) {
        if (itemIndex < 0) {
            itemIndex = -1;
        }

        if (playlist.isRepeat()) {
            return itemIndex;
        }

        if (playlist.isRandom()) {
            int lastIndex = itemIndex;
            if (playlist.getItemSize() > 1) {
                // continuously generate a random number
                // until you get one that was not the last...
                Random rand = new Random();
                while (itemIndex == lastIndex) {
                    itemIndex = rand.nextInt(playlist.getItemSize());
                }
            }
            return itemIndex;
        }

        int nextIndex = itemIndex + 1;

        if (nextIndex < playlist.getItemSize()) {
            return nextIndex;
        } else if (playlist.isRewind()) {
            return playlist.getItemSize() > 0 ? 0 : -1;
        } else {
            return -1;
        }
    }

    /** {@inheritDoc} */
    public int previousItem(IPlaylist playlist, int itemIndex) {

        if (itemIndex > playlist.getItemSize()) {
            return playlist.getItemSize() - 1;
        }

        if (playlist.isRepeat()) {
            return itemIndex;
        }

        if (playlist.isRandom()) {
            Random rand = new Random();
            int lastIndex = itemIndex;
            // continuously generate a random number
            // until you get one that was not the last...
            while (itemIndex == lastIndex) {
                itemIndex = rand.nextInt(playlist.getItemSize());
            }
            lastIndex = itemIndex;
            return itemIndex;
        }

        int prevIndex = itemIndex - 1;

        if (prevIndex >= 0) {
            return prevIndex;
        } else if (playlist.isRewind()) {
            return playlist.getItemSize() - 1;
        } else {
            return -1;
        }
    }

}