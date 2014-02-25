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

package org.red5.server.api.persistence;

import java.io.IOException;

import org.red5.io.object.Input;
import org.red5.io.object.Output;

/**
 * Base interface for objects that can be made persistent.
 * 
 * Every object that complies to this interface must provide either a
 * constructor that takes an input stream as only parameter or an empty
 * constructor so it can be loaded from the persistence store.
 * 
 * However this is not required for objects that are created by the application
 * and initialized afterwards.
 * 
 * @see org.red5.io.object.Input
 * @see IPersistenceStore#load(String)
 * 
 * @author The Red5 Project
 * @author Luke Hubbard (luke@codegent.com)
 * @author Joachim Bauch (jojo@struktur.de)
 */

public interface IPersistable {

	/**
	 * Prefix for attribute names that should not be made persistent.
	 */
	public static final String TRANSIENT_PREFIX = "_transient";

	/**
	 * Returns <code>true</code> if the object is persistent,
	 * <code>false</code> otherwise.
	 * 
	 * @return <code>true</code> if object is persistent, <code>false</code> otherwise
	 */
	public boolean isPersistent();

	/**
	 * Set the persistent flag of the object.
	 * 
	 * @param persistent <code>true</code> if object is persistent, <code>false</code> otherwise
	 */
	public void setPersistent(boolean persistent);

	/**
	 * Returns the name of the persistent object.
	 * 
	 * @return Object name
	 */
	public String getName();

	/**
	 * Set the name of the persistent object.
	 * 
	 * @param name New object name
	 */
	public void setName(String name);

	/**
	 * Returns the type of the persistent object.
	 * 
	 * @return Object type
	 */
	public String getType();

	/**
	 * Returns the path of the persistent object.
	 * 
	 * @return Persisted object path
	 */
	public String getPath();

	/**
	 * Set the path of the persistent object.
	 * 
	 * @param path New persisted object path
	 */
	public void setPath(String path);

	/**
	 * Returns the timestamp when the object was last modified.
	 * 
	 * @return      Last modification date in milliseconds
	 */
	public long getLastModified();

	/**
	 * Returns the persistence store this object is stored in
	 * 
	 * @return      This object's persistence store
	 */
	public IPersistenceStore getStore();

	/**
	 * Store a reference to the persistence store in the object.
	 * 
	 * @param store
	 * 		Store the object is saved in
	 */
	void setStore(IPersistenceStore store);

	/**
	 * Write the object to the passed output stream.
	 * 
	 * @param output
	 * 		Output stream to write to
     * @throws java.io.IOException     Any I/O exception
	 */
	void serialize(Output output) throws IOException;

	/**
	 * Load the object from the passed input stream.
	 * 
	 * @param input
	 * 		Input stream to load from
     * @throws java.io.IOException      Any I/O exception
	 */
	void deserialize(Input input) throws IOException;

}
