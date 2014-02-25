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

package org.red5.classloading;

import java.io.InputStream;
import java.net.URL;

import org.quartz.spi.ClassLoadHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A <code>ClassLoadHelper</code> that determines the correct class loader to
 * use for a scheduler.
 * 
 * @see org.quartz.spi.ClassLoadHelper
 * 
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class QuartzClassLoadHelper implements ClassLoadHelper {

	private static Logger log = LoggerFactory.getLogger(QuartzClassLoadHelper.class);

	private ClassLoader initClassLoader;

	/*
	ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
	 if (classLoader == null) {
	  	classLoader = this.getClass().getClassLoader();
	  	result= classLoader.getResourceAsStream( name );
	 } else {
	  	result= classLoader.getResourceAsStream( name );
	  	if (result == null) {
	  	 classLoader = this.getClass().getClassLoader();
	  	 result= classLoader.getResourceAsStream( name );
	  	}
	 }
	*/

	/**
	 * Called to give the ClassLoadHelper a chance to initialize itself,
	 * including the opportunity to "steal" the class loader off of the calling
	 * thread, which is the thread that is initializing Quartz.
	 */
	public void initialize() {
		initClassLoader = Thread.currentThread().getContextClassLoader();
		log.debug("Initialized with classloader: {}", initClassLoader);
	}

	/**
	 * Return the class with the given name.
	 * 
	 * @param name
	 * @return class
	 */
	public Class<?> loadClass(String name) throws ClassNotFoundException {
		return getClassLoader().loadClass(name);
	}

	/**
	 * Return the class with the given name.
	 * 
	 * @param name
	 * @param clazz
	 * @return class
	 */
	@SuppressWarnings("unchecked")
	public <T> Class<? extends T> loadClass(String name, Class<T> clazz) throws ClassNotFoundException {
		return (Class<? extends T>) getClassLoader().loadClass(name);
	}

	/**
	 * Finds a resource with a given name. This method returns null if no
	 * resource with this name is found.
	 * @param name name of the desired resource
	 * @return a java.net.URL object
	 */
	public URL getResource(String name) {
		return getClassLoader().getResource(name);
	}

	/**
	 * Finds a resource with a given name. This method returns null if no
	 * resource with this name is found.
	 * @param name name of the desired resource
	 * @return a java.io.InputStream object
	 */
	public InputStream getResourceAsStream(String name) {
		return getClassLoader().getResourceAsStream(name);
	}

	/**
	 * Enable sharing of the class-loader with 3rd party (e.g. digester).
	 *
	 * @return the class-loader user be the helper.
	 */
	public ClassLoader getClassLoader() {
		log.debug("Class classloader: {} Thread classloader: {}", this.getClass().getClassLoader(), Thread.currentThread().getContextClassLoader());
		return Thread.currentThread().getContextClassLoader();
	}

}
