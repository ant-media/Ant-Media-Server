/*
 * RED5 Open Source Media Server - https://github.com/Red5/ Copyright 2006-2016 by respective authors (see below). All rights reserved. Licensed under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless
 * required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package org.red5.cache;

import java.io.Serializable;

import org.apache.mina.core.buffer.IoBuffer;

/**
 * Base interface for objects that can be made cacheable.
 * 
 * @see ICacheStore
 * 
 * @author The Red5 Project
 * @author Paul Gregoire (mondain@gmail.com)
 */
public interface ICacheable extends Serializable {

    /**
     * Returns <code>true</code> if the object is cached, <code>false</code> otherwise.
     * 
     * @return <code>true</code> if object is cached, <code>false</code> otherwise
     */
    public boolean isCached();

    /**
     * Sets a flag to represent the cached status of a cacheable object.
     * 
     * @param cached
     *            <code>true</code> if object is cached, <code>false</code> otherwise
     */
    public void setCached(boolean cached);

    /**
     * Returns the name of the cached object.
     * 
     * @return Object name
     */
    public String getName();

    /**
     * Set the name of the cached object.
     * 
     * @param name
     *            New object name
     */
    public void setName(String name);

    /**
     * Returns the object contained within the cacheable reference.
     * 
     * @return Cached representation of object
     */
    public byte[] getBytes();

    /**
     * Returns a readonly byte buffer.
     * 
     * @return Read-only IoBuffer with cached data
     */
    public IoBuffer getByteBuffer();

}
