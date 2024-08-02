/*
 * RED5 Open Source Media Server - https://github.com/Red5/ Copyright 2006-2016 by respective authors (see below). All rights reserved. Licensed under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless
 * required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package org.red5.cache.impl;

import java.lang.ref.SoftReference;
import java.util.Iterator;

import org.apache.mina.core.buffer.IoBuffer;
import org.red5.cache.ICacheStore;
import org.red5.cache.ICacheable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Provides an implementation of an object cache which actually does not provide a cache.
 * 
 * @author The Red5 Project
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class NoCacheImpl implements ICacheStore, ApplicationContextAware {

    protected static Logger log = LoggerFactory.getLogger(NoCacheImpl.class);

    private static NoCacheImpl instance = new NoCacheImpl();

    /** Do not instantiate NoCacheImpl. */
    /*
     * This constructor helps to ensure that we are singleton.
     */
    private NoCacheImpl() {
    }

    /**
     * Returns the instance of this class.
     * 
     * @return class instance
     */
    public static NoCacheImpl getInstance() {
        return instance;
    }

    // We store the application context in a ThreadLocal so we can access it
    // later.
    private static ApplicationContext applicationContext = null;

    /** {@inheritDoc} */
    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        NoCacheImpl.applicationContext = context;
    }

    /**
     * Getter for property 'applicationContext'.
     *
     * @return Value for property 'applicationContext'.
     */
    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    /** {@inheritDoc} */
    @Override
    public Iterator<String> getObjectNames() {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public Iterator<SoftReference<? extends ICacheable>> getObjects() {
        return null;
    }

    public boolean offer(String key, IoBuffer obj) {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean offer(String name, Object obj) {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public void put(String name, Object obj) {
    }

    /** {@inheritDoc} */
    @Override
    public ICacheable get(String name) {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public boolean remove(ICacheable obj) {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean remove(String name) {
        return false;
    }

    /**
     * Getter for property 'cacheHit'.
     *
     * @return Value for property 'cacheHit'.
     */
    public static long getCacheHit() {
        return 0;
    }

    /**
     * Getter for property 'cacheMiss'.
     *
     * @return Value for property 'cacheMiss'.
     */
    public static long getCacheMiss() {
        return 0;
    }

    /** {@inheritDoc} */
    @Override
    public void setMaxEntries(int max) {
    }

    /** {@inheritDoc} */
    @Override
    public void destroy() {
    }
}
