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

import java.util.Set;

/**
 * IEventObservable hold functionality of the well-known Observer pattern, that is
 * it has a list of objects that listen to events.
 */
public interface IEventObservable {
	
    /**
     * Add event listener to this observable
     * 
     * @param listener      Event listener
	 * @return true if listener is added and false otherwise
     */
	public boolean addEventListener(IEventListener listener);

    /**
     * Remove event listener from this observable
     * 
     * @param listener      Event listener
	 * @return true if listener is removed and false otherwise
     */
    public boolean removeEventListener(IEventListener listener);

	/**
     * Returns event listeners
     *
     * @return  Event listeners iterator
     */
    public Set<IEventListener> getEventListeners();

}
