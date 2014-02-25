/*
 * RED5 Open Source Flash Server - http://code.google.com/p/red5/
 * 
 * Copyright 2006-2013 by respective authors (see below). All rights reserved.
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

import org.red5.server.api.statistics.IPlaylistSubscriberStreamStatistics;

/**
 * IPlaylistSubscriberStream has methods of both ISubscriberStream and IPlaylist
 * but adds nothing new
 */
public interface IPlaylistSubscriberStream extends ISubscriberStream, IPlaylist {

	/**
	 * Return statistics about this stream.
	 * 
	 * @return statistics
	 */
	public IPlaylistSubscriberStreamStatistics getStatistics();
		
	/**
	 * Handles a change occurring on the stream.
	 * 
	 * @param state stream state that we are changing to or notifying of
	 * @param changed changed items
	 */	
	public void onChange(StreamState state, Object... changed);

	/**
	 * Replaces an item in the list with another item.
	 * 
	 * @param oldItem
	 * @param newItem
	 * @return true if successful and false otherwise
	 */
	public boolean replace(IPlayItem oldItem, IPlayItem newItem);	
	
}
