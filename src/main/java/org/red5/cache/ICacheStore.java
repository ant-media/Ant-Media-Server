/*
 * RED5 Open Source Media Server - https://github.com/Red5/ Copyright 2006-2016 by respective authors (see below). All rights reserved. Licensed under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless
 * required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package org.red5.cache;

import java.lang.ref.SoftReference;
import java.util.Iterator;

/**
 * Storage for cacheable objects. Selected cache engines must implement this interface.
 * 
 * @see <a href="http://www-128.ibm.com/developerworks/java/library/j-jtp01246.html">Soft references provide for quick-and-dirty caching</a>
 * @see <a href="http://java.sun.com/developer/technicalArticles/ALT/RefObj/">Reference Objects and Garbage Collection</a>
 * @see <a href="http://www.onjava.com/pub/a/onjava/2002/10/02/javanio.html?page=3">Top Ten New Things You Can Do with NIO</a>
 * @see <a href="http://csci.csusb.edu/turner/archive/courses/aiit2004/proxy_cache_solution.html">Proxy Cache Solution</a>
 * 
 * @author The Red5 Project
 * @author Paul Gregoire (mondain@gmail.com)
 */
public interface ICacheStore {

    /**
     * Offer an object to the cache with an associated key. If the named object exists in cache, it will not be accepted.
     * 
     * @param name
     *            string name representing the object
     * @param obj
     *            cacheable object
     * @return true if accepted, false otherwise
     */
    public boolean offer(String name, Object obj);

    /**
     * Puts an object in the cache with the associated key.
     * 
     * @param name
     *            string name representing the object
     * @param obj
     *            cacheable object
     */
    public void put(String name, Object obj);

    /**
     * Return a cached object with the given name.
     * 
     * @param name
     *            the name of the object to return
     * @return the object or <code>null</code> if no such object was found
     */
    public ICacheable get(String name);

    /**
     * Delete the passed cached object.
     * 
     * @param obj
     *            the object to delete
     * @return true if was removed; false it wasn't in cache to begin with
     */
    public boolean remove(ICacheable obj);

    /**
     * Delete the cached object with the given name.
     * 
     * @param name
     *            the name of the object to delete
     * @return true if was removed; false it wasn't in cache to begin with
     */
    public boolean remove(String name);

    /**
     * Return iterator over the names of all already loaded objects in the storage.
     * 
     * @return iterator over all objects names
     */
    public Iterator<String> getObjectNames();

    /**
     * Return iterator over the already loaded objects in the storage.
     * 
     * @return iterator over all objects
     */
    public Iterator<SoftReference<? extends ICacheable>> getObjects();

    /**
     * Sets the maximum number of entries for the cache.
     * 
     * @param max
     *            upper-limit of the cache
     */
    public void setMaxEntries(int max);

    /**
     * Allows for cleanup of a cache implementation.
     */
    public void destroy();

}
