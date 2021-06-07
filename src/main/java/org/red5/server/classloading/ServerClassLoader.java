package org.red5.server.classloading;

import java.io.File;
import java.io.FilenameFilter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

public class ServerClassLoader extends URLClassLoader {

	private static String PLATFORM;

	static {
		String jvmName = System.getProperty("java.vm.name", "").toLowerCase();
		String osName  = System.getProperty("os.name", "").toLowerCase();
		String osArch  = System.getProperty("os.arch", "").toLowerCase();
		String abiType = System.getProperty("sun.arch.abi", "").toLowerCase();
		String libPath = System.getProperty("sun.boot.library.path", "").toLowerCase();
		if (jvmName.startsWith("dalvik") && osName.startsWith("linux")) {
			osName = "android";
		} else if (jvmName.startsWith("robovm") && osName.startsWith("darwin")) {
			osName = "ios";
			osArch = "arm";
		} else if (osName.startsWith("mac os x") || osName.startsWith("darwin")) {
			osName = "macosx";
		} else {
			int spaceIndex = osName.indexOf(' ');
			if (spaceIndex > 0) {
				osName = osName.substring(0, spaceIndex);
			}
		}
		if (osArch.equals("i386") || osArch.equals("i486") || osArch.equals("i586") || osArch.equals("i686")) {
			osArch = "x86";
		} else if (osArch.equals("amd64") || osArch.equals("x86-64") || osArch.equals("x64")) {
			osArch = "x86_64";
		} else if (osArch.startsWith("aarch64") || osArch.startsWith("armv8") || osArch.startsWith("arm64")) {
			osArch = "arm64";
		} else if ((osArch.startsWith("arm")) && ((abiType.equals("gnueabihf")) || (libPath.contains("openjdk-armhf")))) {
			osArch = "armhf";
		} else if (osArch.startsWith("arm")) {
			osArch = "arm";
		}
		PLATFORM = osName + "-" + osArch;
	}

	/**
	 * Filters jar files
	 */
	public final static class JarFileFilter implements FilenameFilter {
		/**
		 * Check whether file matches filter rules
		 * 
		 * @param dir
		 *            Directory
		 * @param name
		 *            File name
		 * @return true If file does match filter rules, false otherwise
		 */
		public boolean accept(File dir, String name) {
			return name.endsWith(".jar");
		}
	}


	public ServerClassLoader(java.lang.ClassLoader parent) {
		super(getJars(), parent);
		
	}
	
	public static URL[] getJars() {
		List<URL> urlList = new ArrayList<>();
		JarFileFilter jarFileFilter = new JarFileFilter();

		String home = System.getProperty("red5.root");
				
		if (home == null || ".".equals(home)) {
			// if home is still null look it up via this classes loader
			String classLocation = ServerClassLoader.class.getProtectionDomain().getCodeSource().getLocation().toString();
			// System.out.printf("Classloader location: %s\n",
			// classLocation);
			// snip off anything beyond the last slash
			home = classLocation.substring(0, classLocation.lastIndexOf('/'));
			
			if (home.startsWith("file:")) {
				home = home.substring("file:".length());
			}
		}

		//Add jars in the lib directory
		String libPath = home + File.separator +"lib";
		File libDir = new File(libPath);
		
		File[] libFiles = libDir.listFiles(jarFileFilter);
		for (File lib : libFiles) {
			try {
				urlList.add(lib.toURI().toURL());
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
		}

		// get config dir
		String conf = home + File.separator  + "conf";

		try {
			URL confUrl = new File(conf).toURI().toURL();
			if (!urlList.contains(confUrl)) {
				urlList.add(confUrl);
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}


		try {
			String serverJar = home + File.separator + "ant-media-server.jar";
			URL serverJarURL = new File(serverJar).toURI().toURL();
			urlList.add(serverJarURL);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		
		// create the directory if it doesnt exist
		String pluginsPath = home + File.separator +"plugins";
		File pluginsDir = new File(pluginsPath);
		
		// add the plugin directory to the path so that configs
		// will be resolved and not have to be copied to conf
		try {
			URL pluginsUrl = pluginsDir.toURI().toURL();
			if (!urlList.contains(pluginsUrl)) {
				urlList.add(pluginsUrl);
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}

		// get all the plugin jars
		File[] pluginsFiles = pluginsDir.listFiles(jarFileFilter);
		// this can be null if the dir doesnt exist

		loadPlugins(pluginsFiles, urlList, PLATFORM);


		URL[] urls = urlList.toArray(new URL[0]);
		System.out.println("Selected libraries: (" + urls.length + " items)");
		

		return urls;
	}
	
	public static void loadPlugins(File[] pluginsFiles, List<URL> urlList, String platform) {
		if (pluginsFiles != null) {
			for (File plugin : pluginsFiles) {
				try {
					String parseUrl = parseUrl(plugin.toURI().toURL());
					if (parseUrl.endsWith("x86") || parseUrl.endsWith("x86-gpl")  || 
						 parseUrl.endsWith("x86_64") || parseUrl.endsWith("x86_64-gpl") ||
						 parseUrl.endsWith("arm64") || parseUrl.endsWith("arm64-gpl") || 
						 parseUrl.endsWith("armhf") || parseUrl.endsWith("armhf-gpl") ||
						 parseUrl.endsWith("ppc64le") || parseUrl.endsWith("ppc64le-gpl") ||  
						 parseUrl.endsWith("arm") || parseUrl.endsWith("arm-gpl")) 
					{
						if (parseUrl.contains(platform)) 
						{
							urlList.add(plugin.toURI().toURL());
						}
					}
					else {
						urlList.add(plugin.toURI().toURL());
					}
				} catch (MalformedURLException e) {
					e.printStackTrace();
				}
			}
		}

	}
	
	/**
	 * Parses url and returns the jar filename stripped of the ending .jar
	 * 
	 * @param url
	 * @return
	 */
	private static String parseUrl(URL url) {
		String external = url.toExternalForm().toLowerCase();
		// get everything after the last slash
		String[] parts = external.split("/");
		// last part
		String libName = parts[parts.length - 1];
		// strip .jar
		libName = libName.substring(0, libName.length() - 4);
		return libName;
	}




}
