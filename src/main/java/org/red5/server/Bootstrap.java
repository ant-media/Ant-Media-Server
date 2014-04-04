/*
 * RED5 Open Source Flash Server - http://code.google.com/p/red5/
 * 
 * Copyright 2006-2014 by respective authors (see below). All rights reserved.
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

package org.red5.server;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.red5.classloading.ClassLoaderBuilder;

/**
 * Boot-straps Red5 using the latest available jars found in <i>red5.home/lib</i> directory.
 *
 * @author The Red5 Project
 * @author Paul Gregoire (mondain@gmail.com)
 * @author Dominick Accattato (daccattato@gmail.com)
 */
public class Bootstrap {

	/**
	 * BootStrapping entry point
	 * 
	 * @param args command line arguments
	 * @throws Exception if error occurs
	 */
	public static void main(String[] args) throws Exception {
		try {
    		String root = getRed5Root();
    		getConfigurationRoot(root);
    		//bootstrap dependencies and start red5
    		bootStrap();
    		System.out.println("Bootstrap complete");
		} catch (Throwable t) {
    		System.out.printf("Bootstrap exception: %s\n", t.getMessage());
    		t.printStackTrace();
		} finally {
    		System.out.println("Bootstrap exit");
		}
	}

	/**
	 * Loads classloader with dependencies
	 * 
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws ClassNotFoundException
	 * @throws NoSuchMethodException
	 * @throws InvocationTargetException
	 */
	private static void bootStrap() throws InstantiationException, IllegalAccessException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException {
		// print the classpath
		//String classPath = System.getProperty("java.class.path");
		//System.out.printf("JVM classpath: %s\n", classPath);		
		System.setProperty("red5.deployment.type", "bootstrap");
		System.setProperty("sun.lang.ClassLoader.allowArraySyntax", "true");
		//check system property before forcing out selector
		if (System.getProperty("logback.ContextSelector") == null) {
			//set to use our logger
			System.setProperty("logback.ContextSelector", "org.red5.logging.LoggingContextSelector");
		}
		String policyFile = System.getProperty("java.security.policy");
		if (policyFile == null) {
			System.setProperty("java.security.debug", "all");
			System.setProperty("java.security.policy", "conf/red5.policy");
		}
		//set the temp directory if we're vista or later
		String os = System.getProperty("os.name").toLowerCase();
		//String arch = System.getProperty("os.arch").toLowerCase();
		//System.out.printf("OS: %s Arch: %s\n", os, arch);
		if (os.contains("vista") || os.contains("windows 7")) {
			String dir = System.getProperty("user.home");
			//detect base drive (c:\ etc)
			if (dir.length() == 3) {
				//use default
				dir += "Users\\Default\\AppData\\Red5";
				//make sure the directory exists
				File f = new File(dir);
				if (!f.exists()) {
					f.mkdir();
				}
				f = null;
			} else {
				dir += "\\AppData\\localLow";
			}
			System.setProperty("java.io.tmpdir", dir);
			System.out.printf("Setting temp directory to %s\n", System.getProperty("java.io.tmpdir"));
		}
		/*
		try {
		    // Enable the security manager
		    SecurityManager sm = new SecurityManager();
		    System.setSecurityManager(sm);
		} catch (SecurityException se) {
			System.err.println("Security manager already set");
		}
		*/
		//get current loader
		ClassLoader baseLoader = Thread.currentThread().getContextClassLoader();
		// build a ClassLoader
		ClassLoader loader = ClassLoaderBuilder.build();
		//set new loader as the loader for this thread
		Thread.currentThread().setContextClassLoader(loader);
		// create a new instance of this class using new classloader
		Object boot = Class.forName("org.red5.server.Launcher", true, loader).newInstance();
		Method m1 = boot.getClass().getMethod("launch", (Class[]) null);
		m1.invoke(boot, (Object[]) null);
		//not that it matters, but set it back to the original loader
		Thread.currentThread().setContextClassLoader(baseLoader);
	}

	/**
	 * Gets the configuration root
	 * 
	 * @param root
	 * @return
	 */
	private static String getConfigurationRoot(String root) {
		// look for config dir
		String conf = System.getProperty("red5.config_root");
		// if root is not null and conf is null then default it
		if (root != null && conf == null) {
			conf = root + "/conf";
		}
		//flip slashes only if windows based os
		if (File.separatorChar != '/') {
			conf = conf.replaceAll("\\\\", "/");
		}
		//set conf sysprop
		System.setProperty("red5.config_root", conf);
		System.out.printf("Configuation root: %s\n", conf);
		return conf;
	}

	/**
	 * Gets the Red5 root
	 * 
	 * @return
	 * @throws IOException
	 */
	private static String getRed5Root() throws IOException {
		// look for red5 root first as a system property
		String root = System.getProperty("red5.root");
		// if root is null check environmental
		if (root == null) {
			//check for env variable
			root = System.getenv("RED5_HOME");
		}
		// if root is null find out current directory and use it as root
		if (root == null || ".".equals(root)) {
			root = System.getProperty("user.dir");
			//System.out.printf("Current directory: %s\n", root);
		}
		//if were on a windows based os flip the slashes
		if (File.separatorChar != '/') {
			root = root.replaceAll("\\\\", "/");
		}
		//drop last slash if exists
		if (root.charAt(root.length() - 1) == '/') {
			root = root.substring(0, root.length() - 1);
		}
		//set/reset property
		System.setProperty("red5.root", root);
		System.out.printf("Red5 root: %s\n", root);
		return root;
	}

}
