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

package org.red5.server.api.event;

/**
 * IEvent interfaces is the essential interface every Event should implement
 */
public interface IEvent {
	
	/**
	 * Returns even type
	 * 
	 * @return Event type enumeration
	 */
	public Type getType();
	
	/**
	 * Returns event context object
	 * 
	 * @return Event context object
	 */
	public Object getObject();
	
	/**
	 * Whether event has source (event listener(s))
	 * @return	<code>true</code> if so, <code>false</code> otherwise
	 */
	public boolean hasSource();
	
	/**
	 * Returns event listener
	 * @return	Event listener object
	 */
	public IEventListener getSource();

	enum Type {
		SYSTEM, STATUS, SERVICE_CALL, SHARED_OBJECT, STREAM_ACTION, STREAM_CONTROL, STREAM_DATA, CLIENT, CLIENT_INVOKE, CLIENT_NOTIFY, SERVER
	}

}
