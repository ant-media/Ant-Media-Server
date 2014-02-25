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

package org.red5.server.api.scope;

import org.red5.server.api.ICoreObject;
import org.red5.server.api.event.IEventObservable;
import org.red5.server.api.persistence.IPersistenceStore;

/**
 * Base interface for all scope objects, including SharedObjects.
 * 
 * @author The Red5 Project
 * @author Luke Hubbard (luke@codegent.com)
 */
public interface IBasicScope extends ICoreObject, IEventObservable {

	/**
	 * Does this scope have a parent? You can think of scopes as of tree items
	 * where scope may have a parent and children (child).
	 * 
	 * @return <code>true</code> if this scope has a parent, otherwise
	 *         <code>false</code>
	 */
	public boolean hasParent();

	/**
	 * Get this scopes parent.
	 * 
	 * @return parent scope, or <code>null</code> if this scope doesn't have a
	 *         parent
	 */
	public IScope getParent();

	/**
	 * Get the scopes depth, how far down the scope tree is it. The lowest depth
	 * is 0x00, the depth of Global scope. Application scope depth is 0x01. Room
	 * depth is 0x02, 0x03 and so forth.
	 * 
	 * @return the depth
	 */
	public int getDepth();

	/**
	 * Get the name of this scope. Eg. <code>someroom</code>.
	 * 
	 * @return the name
	 */
	public String getName();

	/**
	 * Get the persistable store
	 * @return the store
	 */
	public IPersistenceStore getStore();
	
	/**
	 * Get the full absolute path. Eg. <code>host/myapp/someroom</code>.
	 * 
	 * @return Absolute scope path
	 */
	public String getPath();

	/**
	 * Get the type of the scope.
	 * 
	 * @return Type of scope
	 */
	public ScopeType getType();

	/**
	 * Sets the amount of time to keep the scope available after the
	 * last disconnect.
	 * 
	 * @param keepDelay delay
	 */
	public void setKeepDelay(int keepDelay);
	
	/**
	 * Validates a scope based on its name and type
	 * 
	 * @return true if both name and type are valid, false otherwise
	 */
	public boolean isValid();

}
