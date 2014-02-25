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

package org.red5.server.api.stream.support;

import org.red5.server.api.stream.IPlayItem;
import org.red5.server.messaging.IMessageInput;

/**
 * Simple playlist item implementation
 */
public class SimplePlayItem implements IPlayItem, Comparable<SimplePlayItem> {

	private long created = System.nanoTime();
	
	/**
	 * Playlist item name
	 */
	protected final String name;

	/**
	 * Start mark
	 */
	protected final long start;

	/**
	 * Length - amount to play
	 */
	protected final long length;
	
	/**
	 * Message source
	 */
	protected IMessageInput msgInput;
	
	private SimplePlayItem(String name) {
		this.name = name;
		this.start = -2L;
		this.length = -1L;
	}
	
	private SimplePlayItem(String name, long start, long length) {
		this.name = name;
		this.start = start;
		this.length = length;
	}

	/**
	 * Returns play item length in milliseconds
	 * 
	 * @return	Play item length in milliseconds
	 */
	public long getLength() {
		return length;
	}

	/**
	 * Returns IMessageInput object. IMessageInput is an endpoint for a consumer
	 * to connect.
	 * 
	 * @return	IMessageInput object
	 */
	public IMessageInput getMessageInput() {
		return msgInput;
	}

	/**
	 * Returns item name
	 * 
	 * @return	item name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns boolean value that specifies whether item can be played
	 */
	public long getStart() {
		return start;
	}

	/**
	 * Alias for getMessageInput
	 * 
	 * @return      Message input source
	 */
	public IMessageInput getMsgInput() {
		return msgInput;
	}

	/**
	 * Setter for message input
	 *
	 * @param msgInput Message input
	 */
	public void setMsgInput(IMessageInput msgInput) {
		this.msgInput = msgInput;
	}

	/**
	 * @return the created
	 */
	public long getCreated() {
		return created;
	}

	/**
	 * @param created the created to set
	 */
	public void setCreated(long created) {
		this.created = created;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + (int) (start ^ (start >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SimplePlayItem other = (SimplePlayItem) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (start != other.start)
			return false;
		return true;
	}

	@Override
	public int compareTo(SimplePlayItem that) {
		if (created > that.getCreated()) {
			return -1;
		} else if (created < that.getCreated()) {
			return 1;
		}
		return 0;
	}	
	
	/**
	 * Builder for SimplePlayItem
	 * 
	 * @param name
	 * @return play item instance
	 */
	public static SimplePlayItem build(String name) {
		SimplePlayItem playItem = new SimplePlayItem(name);
		return playItem;
	}	
	
	/**
	 * Builder for SimplePlayItem
	 * 
	 * @param name
	 * @param start
	 * @param length
	 * @return play item instance
	 */
	public static SimplePlayItem build(String name, long start, long length) {
		SimplePlayItem playItem = new SimplePlayItem(name, start, length);
		return playItem;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "SimplePlayItem [created=" + created + ", name=" + name + ", start=" + start + ", length=" + length + "]";
	}
	
}
