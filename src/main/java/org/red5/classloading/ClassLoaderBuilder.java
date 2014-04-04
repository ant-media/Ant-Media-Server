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

package org.red5.classloading;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.regex.Pattern;

/**
 * Class used to get the Servlet Class loader. The class loader returned is a
 * child first class loader. 
 * 
 * <br />
 * <i>This class is based on original code from the XINS project, by 
 * Anthony Goubard (anthony.goubard@japplis.com)</i>
 * 
 * @author Paul Gregoire (mondain@gmail.com)
 */
public final class ClassLoaderBuilder {

	/*
	 http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6500212
	 http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6516909
	 http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4976356
	 */
	
	/**
	 * Load the Servlet code from the WAR file and use the current classpath for
	 * the libraries.
	 */
	public static final int USE_CLASSPATH_LIB = 1;

	/**
	 * Load the servlet code from the WAR file and try to find the libraries in
	 * the common red5 lib directory.
	 */
	public static final int USE_RED5_LIB = 2;

	/**
	 * Load the servlet code and the libraries from the WAR file. This may take
	 * some time as the libraries need to be extracted from the WAR file.
	 */
	public static final int USE_WAR_LIB = 3;

	/**
	 * Filters jar files
	 */
	public final static class JarFileFilter implements FilenameFilter {
		/**
		 * Check whether file matches filter rules
		 * 
		 * @param dir Directory
		 * @param name File name
		 * @return true If file does match filter rules, false otherwise
		 */
		public boolean accept(File dir, String name) {
			return name.endsWith(".jar");
		}
	}

	/**
	 * Default build uses Red5 common lib without a parent classloader.
	 * 
	 * @return the class loader
	 */
	public static ClassLoader build() {
		return ClassLoaderBuilder.build(null, USE_RED5_LIB, null);
	}

	/**
	 * Gets a class loader based on mode.
	 * 
	 * @param path the directory or file containing classes
	 * 
	 * @param mode the mode in which the servlet should be loaded. The possible
	 *            values are <code>USE_CURRENT_CLASSPATH</code>,
	 *            <code>USE_CLASSPATH_LIB</code>, <code>USE_XINS_LIB</code>,
	 *            <code>USE_WAR_LIB</code>.
	 * 
	 * @param parent the parent class loader or null if you want the current threads
	 * 			class loader
	 * 
	 * @return the Class loader to use to load the required class(es).
	 * 
	 */
	@SuppressWarnings("unused")
	public static ClassLoader build(File path, int mode, ClassLoader parent) {
		JarFileFilter jarFileFilter = new JarFileFilter();
		List<URL> urlList = new ArrayList<URL>(31);
		//the class loader to return
		ClassLoader loader = null;
		//urls to load resources / classes from
		URL[] urls = null;

		if (mode == USE_RED5_LIB) {
			//get red5 home
			// look for red5 home as a system property
			String home = System.getProperty("red5.root");
			// if home is null check environmental
			if (home == null) {
				//check for env variable
				home = System.getenv("RED5_HOME");
			}
			//if home is null or equal to "current" directory
			if (home == null || ".".equals(home)) {
				//if home is still null look it up via this classes loader
				String classLocation = ClassLoaderBuilder.class.getProtectionDomain().getCodeSource().getLocation().toString();
				//System.out.printf("Classloader location: %s\n", classLocation);
				//snip off anything beyond the last slash
				home = classLocation.substring(0, classLocation.lastIndexOf('/'));
			}

			try {
				//add red5.jar to the classpath
				File red5jar = new File(home, "red5-server.jar");
				if (!red5jar.exists()) {
					System.out.println("Red5 server jar was not found, using fallback.");
					red5jar = new File(home, "red5.jar");
				} else {
					System.out.println("Red5 server jar was found");
				}
				urlList.add(red5jar.toURI().toURL());
			} catch (MalformedURLException e1) {
				e1.printStackTrace();
			}
			System.out.printf("URL list: %s\n", urlList);	

			//get red5 lib system property, if not found build it	
			String libPath = System.getProperty("red5.lib_root");
			if (libPath == null) {
				//construct the lib path
				libPath = home + "/lib";
			}
			//System.out.printf("Library path: %s\n", libPath);	

			//grab the urls for all the jars in "lib"
			File libDir = new File(libPath);
			//if we are on osx with spaces in our path this may occur
			if (libDir == null) {
				libDir = new File(home, "lib");
			}
			File[] libFiles = libDir.listFiles(jarFileFilter);
			for (File lib : libFiles) {
				try {
					urlList.add(lib.toURI().toURL());
				} catch (MalformedURLException e) {
					System.err.printf("Exception %s\n", e);
				}
			}

			//look over the libraries and remove the old versions
			scrubURLList(urlList);

			//get config dir
			String conf = System.getProperty("red5.config_root");
			if (conf == null) {
				conf = home + "/conf";
			}
			// add config dir
			try {
				URL confUrl = new File(conf).toURI().toURL();
				if (!urlList.contains(confUrl)) {
					urlList.add(confUrl);
				}
			} catch (MalformedURLException e) {
				System.err.printf("Exception %s\n", e);
			}

			//add the plugins 

			//get red5 lib system property, if not found build it	
			String pluginsPath = System.getProperty("red5.plugins_root");
			if (pluginsPath == null) {
				//construct the plugins path
				pluginsPath = home + "/plugins";
				//update the property
				System.setProperty("red5.plugins_root", pluginsPath);
			}
			// create the directory if it doesnt exist
			File pluginsDir = new File(pluginsPath);
			//if we are on osx with spaces in our path this may occur
			if (pluginsDir == null) {
				pluginsDir = new File(home, "plugins");
				//create the dir
				pluginsDir.mkdirs();
			}			
			// add the plugin directory to the path so that configs
			// will be resolved and not have to be copied to conf
			try {
				URL pluginsUrl = pluginsDir.toURI().toURL();
				if (!urlList.contains(pluginsUrl)) {
					urlList.add(pluginsUrl);
				}
			} catch (MalformedURLException e) {
				System.err.printf("Exception %s\n", e);
			}	
			//get all the plugin jars
			File[] pluginsFiles = pluginsDir.listFiles(jarFileFilter);
			//this can be null if the dir doesnt exist
			if (pluginsFiles != null) {
				for (File plugin : pluginsFiles) {
					try {
						urlList.add(plugin.toURI().toURL());
					} catch (MalformedURLException e) {
						System.err.printf("Exception %s\n", e);
					}
				}
			}

			//create the url array that the classloader wants
			urls = urlList.toArray(new URL[0]);
			System.out.printf("Selected libraries: (%s items)\n", urls.length);
			for (URL url : urls) {
				System.out.println(url);
			}		
			System.out.println();
			// instance a url classloader using the selected jars
			if (parent == null) {
				loader = new URLClassLoader(urls);
			} else {
				loader = new URLClassLoader(urls, parent);
			}

		} else {
			List<String> standardLibs = new ArrayList<String>(7);

			try {
				urlList.add(path.toURI().toURL());
				URL classesURL = new URL("jar:file:" + path.getAbsolutePath().replace(File.separatorChar, '/') + "!/WEB-INF/classes/");
				urlList.add(classesURL);
			} catch (MalformedURLException e1) {
				e1.printStackTrace();
			}

			if (mode == USE_CLASSPATH_LIB) {
				String classPath = System.getProperty("java.class.path");
				StringTokenizer stClassPath = new StringTokenizer(classPath, File.pathSeparator);
				while (stClassPath.hasMoreTokens()) {
					String nextPath = stClassPath.nextToken();
					if (nextPath.toLowerCase().endsWith(".jar")) {
						standardLibs.add(nextPath.substring(nextPath.lastIndexOf(File.separatorChar) + 1));
					}
					try {
						urlList.add(new File(nextPath).toURI().toURL());
					} catch (MalformedURLException e) {
						System.err.printf("Exception %s\n", e);
					}
				}
			}
			if (mode == USE_WAR_LIB) {
				if (path.isDirectory()) {
					File libDir = new File(path, "WEB-INF/lib");
					//this should not be null but it can happen
					if (libDir != null && libDir.canRead()) {
						File[] libs = libDir.listFiles(jarFileFilter);
						//System.out.printf("Webapp lib count: %s\n", libs.length);
						for (File lib : libs) {
							try {
								urlList.add(lib.toURI().toURL());
							} catch (MalformedURLException e) {
								System.err.printf("Exception %s\n", e);
							}
						}
					}
				} else {
					try {
						JarInputStream jarStream = new JarInputStream(new FileInputStream(path));
						JarEntry entry = jarStream.getNextJarEntry();
						while (entry != null) {
							String entryName = entry.getName();
							if (entryName.startsWith("WEB-INF/lib/") && entryName.endsWith(".jar") && !standardLibs.contains(entryName.substring(12))) {
								File tempJarFile = unpack(jarStream, entryName);
								urlList.add(tempJarFile.toURI().toURL());
							}
							entry = jarStream.getNextJarEntry();
						}
						jarStream.close();
					} catch (IOException e) {
						System.err.printf("Exception %s\n", e);
					}
				}
			}
			urls = urlList.toArray(new URL[0]);
			loader = new ChildFirstClassLoader(urls, parent);
		}
		Thread.currentThread().setContextClassLoader(loader);
		//loop thru all the current urls
		//System.out.printf("Classpath for %s:\n", loader);
		//for (URL url : urls) {
		//System.out.println(url.toExternalForm());
		//}
		return loader;
	}

	/**
	 * Unpack the specified entry from the JAR file.
	 * 
	 * @param jarStream
	 *            The input stream of the JAR file positioned at the entry.
	 * @param entryName
	 *            The name of the entry to extract.
	 * 
	 * @return The extracted file. The created file is a temporary file in the
	 *         temporary directory.
	 * 
	 * @throws IOException
	 *             if the JAR file cannot be read or is incorrect.
	 */
	private static File unpack(JarInputStream jarStream, String entryName) throws IOException {
		String libName = entryName.substring(entryName.lastIndexOf('/') + 1, entryName.length() - 4);
		File tempJarFile = File.createTempFile("tmp_" + libName, ".jar");
		tempJarFile.deleteOnExit();
		FileOutputStream out = new FileOutputStream(tempJarFile);
		// Transfer bytes from the JAR file to the output file
		byte[] buf = new byte[8192];
		int len;
		while ((len = jarStream.read(buf)) > 0) {
			out.write(buf, 0, len);
		}
		out.close();
		return tempJarFile;
	}

	/**
	 * Removes older versions of libraries from a given list based
	 * on their version numbers.
	 * 
	 * @param list
	 */
	private final static void scrubURLList(List<URL> list) {
		String ALPHABET = "abcdefghijklmnopqrstuvwxyz";
		Pattern punct = Pattern.compile("\\p{Punct}");
		Set<URL> removalList = new HashSet<URL>(list.size());
		String topName = null;
		String checkName = null;
		URL[] urls = list.toArray(new URL[0]);
//		System.out.printf("Library list: (%s items)\n", urls.length);
//		for (URL url : urls) {
//			System.out.println(url);
//		}		
//		System.out.println();
		for (URL top : urls) {
			if (removalList.contains(top)) {
				continue;
			}
			topName = parseUrl(top);
			//empty name - this happens inside eclipse
			if ("".equals(topName)) {
				removalList.add(top);
				continue;
			}
			//skip red5
			if (topName.startsWith("red5")) {
				continue;
			}
			//skip version-less libraries
			if (topName.endsWith("-")) {
				removalList.add(top);
				continue;
			}
			//by default we will get rid of testing libraries
			if (topName.startsWith("grobo") || topName.startsWith("junit") || topName.startsWith("ivy")) {
				removalList.add(top);
				continue;
			}
			//by default we will get rid of "javadoc" and "sources" jars
			if (topName.contains("javadoc") || topName.contains("sources")) {
				removalList.add(top);
				continue;
			}
			int topFirstDash = topName.indexOf('-');
			//if theres no dash then just grab the first 3 chars // FIXME: why just grab the first 3 characters?
			String prefix = topName.substring(0, topFirstDash != -1 ? topFirstDash : 3);
			int topSecondDash = topName.indexOf('-', topFirstDash + 1);
			for (URL check : list) {
				if (removalList.contains(check)) {
					continue;
				}
				checkName = parseUrl(check);
				//if its the same lib just continue with the next
				if (checkName.equals(topName)) {
					continue;
				}
				//if the last character is a dash then skip it
				if (checkName.endsWith("-")) {
					continue;
				}
				//check starts with to see if we should do version check
				if (!checkName.startsWith(prefix)) {
					continue;
				}
				//check for next dash
				if (topSecondDash > 0) {
					if (checkName.length() <= topSecondDash) {
						continue;
					}
					//check for second dash in check lib at same position
					if (checkName.charAt(topSecondDash) != '-') {
						continue;
					}
					//split the names
					String[] topSubs = topName.split("-");
					String[] checkSubs = checkName.split("-");
					//check lib type "spring-aop" vs "spring-orm"
					if (!topSubs[1].equals(checkSubs[1])) {
						continue;
					}
					//see if next entry is a number
					if (!Character.isDigit(topSubs[2].charAt(0)) && !Character.isDigit(checkSubs[2].charAt(0))) {
						//check next lib name section for a match
						if (!topSubs[2].equals(checkSubs[2])) {
							continue;
						}
					}
				}
				//do the version check

				//read from end to get version info
				String checkVers = checkName.substring(topSecondDash != -1 ? (topSecondDash + 1) : (topFirstDash + 1));
				if (checkVers.startsWith("-")) {
					continue;
				}

				//get top libs version info
				String topVers = topName.substring(topSecondDash != -1 ? (topSecondDash + 1) : (topFirstDash + 1));
				int topThirdDash = -1;
				String topThirdName = null;
				if (topVers.length() > 0 && !Character.isDigit(topVers.charAt(0))) {
					//check if third level lib name matches
					topThirdDash = topVers.indexOf('-');
					//no version most likely exists
					if (topThirdDash == -1) {
						continue;
					}
					topThirdName = topVers.substring(0, topThirdDash);
					topVers = topVers.substring(topThirdDash + 1);
				}

				//if check version starts with a non-number skip it
				int checkThirdDash = -1;
				String checkThirdName = null;
				if (!Character.isDigit(checkVers.charAt(0))) {
					//check if third level lib name matches
					checkThirdDash = checkVers.indexOf('-');
					//no version most likely exists
					if (checkThirdDash == -1) {
						continue;
					}
					checkThirdName = checkVers.substring(0, checkThirdDash);
					if (topThirdName == null || !topThirdName.equals(checkThirdName)) {
						continue;
					}
					checkVers = checkVers.substring(checkThirdDash + 1);
					//if not
					if (!Character.isDigit(checkVers.charAt(0))) {
						continue;
					}
				}

				if (topThirdName != null && checkThirdName == null) {
					continue;
				}
				//check major
				String[] topVersion = punct.split(topVers);
				//System.out.println("topVersion (" + topVers + "): " + topVersion[0] + " length: " + topVersion.length);
				if (!topVersion[0].matches("[\\d].*")) {
					continue;
				}

				//check 3rd part of version for letters
				if (topVersion.length > 2) {
					String v = topVersion[2].toLowerCase();
					if (v.length() > 1) {
						topVersion[2] = deleteAny(v, ALPHABET);
					}
					// after alpha removal, string is any digits or single char
					if (topVersion[2].length() == 1) {
						//if is a only a letter use its index as a version
						char ch = v.charAt(0);
						if (!Character.isDigit(ch)) {
							topVersion[2] = ALPHABET.indexOf(ch) + "";
						}
					}
				}
				//System.out.println("AOB " + checkVers + " | " + topVersion[0] + " length: " + topVersion.length);
				int topVersionNumber;
				try{
					topVersionNumber = topVersion.length == 1 ? Integer.valueOf(topVersion[0]) : Integer.valueOf(
						topVersion[0] + topVersion[1] + (topVersion.length > 2 ? topVersion[2] : '0')).intValue();
				} catch (NumberFormatException nfe){
					topVersionNumber = 0;
					System.err.println("Error parsing topVers:"+ topVers);
				}

				String[] checkVersion = punct.split(checkVers);
				//System.out.println("checkVersion (" + checkVers + "): " + checkVersion[0] + " length: " + checkVersion.length);

				//check 3rd part of version for letters
				if (checkVersion.length > 2) {
					String v = checkVersion[2].toLowerCase();
					if (v.length() > 1) {
						checkVersion[2] = deleteAny(v, ALPHABET);
					} 
					// after alpha removal, string is any digits or single char
					if (checkVersion[2].length() == 1) {
						//if is a only a letter use its index as a version
						char ch = v.charAt(0);
						if (!Character.isDigit(ch)) {
							checkVersion[2] = ALPHABET.indexOf(ch) + "";
						}
					}
				}
				int checkVersionNumber;
				try{
					checkVersionNumber = checkVersion.length == 1 ? Integer.valueOf(checkVersion[0]) : Integer.valueOf(
						checkVersion[0] + checkVersion[1] + (checkVersion.length > 2 ? checkVersion[2] : '0')).intValue();
				} catch (NumberFormatException nfe){
					checkVersionNumber=0;
					System.err.println("Error parsing checkVers:"+ checkVers);
				}

				// Check version numbers
				if (topVersionNumber >= checkVersionNumber) {
					//remove it
					removalList.add(check);
				} else {
					removalList.add(top);
					break;
				}
			}
		}
		//remove the old libs
//		System.out.println("Removal list:");
//		for (URL url : removalList) {
//			System.out.println(url);
//		}
		list.removeAll(removalList);
	}

	/**
	 * Parses url and returns the jar filename stripped of the ending .jar
	 * @param url
	 * @return
	 */
	private static String parseUrl(URL url) {
		String external = url.toExternalForm().toLowerCase();
		//get everything after the last slash
		String[] parts = external.split("/");
		//last part
		String libName = parts[parts.length - 1];
		//strip .jar
		libName = libName.substring(0, libName.length() - 4);
		return libName;
	}

	private static String deleteAny(String str, String removalChars) {
		StringBuilder sb = new StringBuilder(str);
		//System.out.println("Before alpha delete: " + sb.toString());
		String[] chars = removalChars.split("");
		//System.out.println("Chars length: " + chars.length);
		for (String c : chars) {
			int index = -1;
			while ((index = sb.indexOf(c)) > 0) {
				sb.deleteCharAt(index);
			}
		}
		//System.out.println("After alpha delete: " + sb.toString());
		return sb.toString();
	}
}
