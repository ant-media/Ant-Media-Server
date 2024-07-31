/*
 * RED5 Open Source Media Server - https://github.com/Red5/ Copyright 2006-2016 by respective authors (see below). All rights reserved. Licensed under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless
 * required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package org.red5.cache.impl;

import java.lang.ref.SoftReference;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.mina.core.buffer.IoBuffer;
import org.red5.cache.ICacheStore;
import org.red5.cache.ICacheable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Provides an implementation of an object cache.
 * 
 * @author The Red5 Project
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class CacheImpl implements ICacheStore, ApplicationContextAware {

    protected static Logger log = LoggerFactory.getLogger(CacheImpl.class);

    private static volatile CacheImpl instance;

    private static final Map<String, SoftReference<? extends ICacheable>> CACHE;

    // cache registry - keeps hard references to objects in the cache
    private static Map<String, Integer> registry;

    private static int capacity = 5;

    private static volatile long cacheHit;

    private static volatile long cacheMiss;

    static {
        // create an instance
        instance = new CacheImpl();
        // instance a static map with an initial small (prime) size
        CACHE = new HashMap<String, SoftReference<? extends ICacheable>>(3);
        // instance a hard-ref registry
        registry = new HashMap<String, Integer>(3);
    }

    /** Do not instantiate CacheImpl. */

    /**
     * This constructor helps to ensure that we are singleton.
     */
    private CacheImpl() {
    }

    // We store the application context in a ThreadLocal so we can access it
    // later.
    private static ApplicationContext applicationContext = null;

    /** {@inheritDoc} */
    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        CacheImpl.applicationContext = context;
    }

    /**
     * Getter for property 'applicationContext'.
     *
     * @return Value for property 'applicationContext'.
     */
    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    /**
     * Returns the instance of this class.
     * 
     * @return instance of this class
     */
    public static CacheImpl getInstance() {
        return instance;
    }

    public void init() {
        log.info("Loading generic object cache");
        log.debug("Appcontext: {}", applicationContext.toString());
    }

    /** {@inheritDoc} */
    @Override
    public Iterator<String> getObjectNames() {
        return Collections.unmodifiableSet(CACHE.keySet()).iterator();
    }

    /** {@inheritDoc} */
    @Override
    public Iterator<SoftReference<? extends ICacheable>> getObjects() {
        return Collections.unmodifiableCollection(CACHE.values()).iterator();
    }

    public boolean offer(String key, IoBuffer obj) {
        return offer(key, new CacheableImpl(obj));
    }

    /** {@inheritDoc} */
    @Override
    public boolean offer(String name, Object obj) {
        boolean accepted = false;
        // check map size
        if (CACHE.size() < capacity) {
            SoftReference<?> tmp = CACHE.get(name);
            // because soft references can be garbage collected when a system is
            // in need of memory, we will check that the cacheable object is
            // valid
            // log.debug("Softreference: " + (null == tmp));
            // if (null != tmp) {
            // log.debug("Softreference value: " + (null == tmp.get()));
            // }
            if (null == tmp || null == tmp.get()) {
                ICacheable cacheable = null;
                if (obj instanceof ICacheable) {
                    cacheable = (ICacheable) obj;
                } else {
                    cacheable = new CacheableImpl(obj);
                }
                // set the objects name
                cacheable.setName(name);
                // set a registry entry
                registry.put(name, 1);
                // create a soft reference
                SoftReference<ICacheable> value = new SoftReference<ICacheable>(cacheable);
                CACHE.put(name, value);
                // set acceptance
                accepted = true;
                log.info("{} has been added to the cache. Current size: {}", name, CACHE.size());
            }
        } else {
            log.warn("Cache has reached max element size: " + capacity);
        }
        return accepted;
    }

    /** {@inheritDoc} */
    @Override
    public void put(String name, Object obj) {
        if (obj instanceof ICacheable) {
            put(name, (ICacheable) obj);
        } else {
            put(name, new CacheableImpl(obj));
        }
    }

    protected void put(String name, ICacheable obj) {
        // set the objects name
        obj.setName(name);
        // set a registry entry
        registry.put(name, 1);
        // create a soft reference
        SoftReference<ICacheable> value = new SoftReference<ICacheable>(obj);
        // put an object into the cache
        CACHE.put(name, value);
        log.info(name + " has been added to the cache. Current size: " + CACHE.size());
    }

    /** {@inheritDoc} */
    @Override
    public ICacheable get(String name) {
        if (log.isDebugEnabled()) {
            log.debug("Looking up " + name + " in the cache. Current size: " + CACHE.size());
        }
        ICacheable ic = null;
        SoftReference<?> sr = null;
        if (!CACHE.isEmpty() && null != (sr = CACHE.get(name))) {
            ic = (ICacheable) sr.get();
            // add a request count to the registry
            int requestCount = registry.get(name);
            registry.put(name, (requestCount += 1));
            // increment cache hits
            cacheHit += 1;
        } else {
            // add a request count to the registry
            registry.put(name, 1);
            // increment cache misses
            cacheMiss += 1;
        }
        log.debug("Registry on get: {}", registry.toString());
        return ic;
    }

    /** {@inheritDoc} */
    @Override
    public boolean remove(ICacheable obj) {
        log.debug("Looking up {} in the cache. Current size: {}", obj.getName(), CACHE.size());
        return remove(obj.getName());
    }

    /** {@inheritDoc} */
    @Override
    public boolean remove(String name) {
        return CACHE.remove(name) != null ? true : false;
    }

    /**
     * Getter for property 'cacheHit'.
     *
     * @return Value for property 'cacheHit'.
     */
    public static long getCacheHit() {
        return cacheHit;
    }

    /**
     * Getter for property 'cacheMiss'.
     *
     * @return Value for property 'cacheMiss'.
     */
    public static long getCacheMiss() {
        return cacheMiss;
    }

    /** {@inheritDoc} */
    @Override
    public void setMaxEntries(int max) {
        log.info("Setting max entries for this cache to {}", max);
        CacheImpl.capacity = max;
    }

    /** {@inheritDoc} */
    @Override
    public void destroy() {
        // Shut down the cache manager
        try {
            registry.clear();
            registry = null;
            CACHE.clear();
        } catch (Exception e) {
            log.warn("Error on cache shutdown", e);
        }
    }
}
