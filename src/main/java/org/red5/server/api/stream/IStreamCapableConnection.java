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

import org.red5.server.api.IConnection;

/**
 * A connection that supports streaming.
 * 
 * @author The Red5 Project
 * @author Luke Hubbard (luke@codegent.com)
 * @author Steven Gong (steven.gong@gmail.com)
 */
public interface IStreamCapableConnection extends IConnection {

	/**
	 * Return a reserved stream id for use.
	 * According to FCS/FMS regulation, the base is 1.
	 * @return              Reserved stream id
	 */
	int reserveStreamId();

	int reserveStreamId(int id);

	/**
	 * Unreserve this id for future use.
	 * 
	 * @param streamId      ID of stream to unreserve
	 */
	void unreserveStreamId(int streamId);

	/**
	 * Deletes the stream with the given id.
	 * 
	 * @param streamId      ID of stream to delete
	 */
	void deleteStreamById(int streamId);

	/**
	 * Get a stream by its id.
	 * 
	 * @param streamId      Stream id
	 * @return              Stream with given id
	 */
	IClientStream getStreamById(int streamId);

	/**
	 * Create a stream that can play only one item.
	 * 
	 * @param streamId      Stream id
	 * @return              New subscriber stream that can play only one item
	 */
	ISingleItemSubscriberStream newSingleItemSubscriberStream(int streamId);

	/**
	 * Create a stream that can play a list.
	 * 
	 * @param streamId      Stream id
	 * @return              New stream that can play sequence of items
	 */
	IPlaylistSubscriberStream newPlaylistSubscriberStream(int streamId);

	/**
	 * Create a broadcast stream.
	 * 
	 * @param streamId      Stream id
	 * @return              New broadcast stream
	 */
	IClientBroadcastStream newBroadcastStream(int streamId);

	/**
	 * Total number of video messages that are pending to be sent to a stream.
	 *
	 * @param streamId       Stream id
	 * @return               Number of pending video messages
	 */
	long getPendingVideoMessages(int streamId);

}
