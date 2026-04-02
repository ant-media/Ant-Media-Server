package org.red5.server.plugin;

import io.antmedia.test.plugin.MinimalAmsPlugin;
import io.antmedia.test.plugin.MinimalPluginRest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Builds minimal V2 plugin JARs and ZIPs for use in PluginDeployerTest.
 */
public class TestPluginJarBuilder {

	/**
	 * Builds a JAR with @AmsPlugin + @Listener class and valid V2 manifest.
	 */
	public static File buildPluginJar(String pluginName) throws Exception {
		return buildPluginJar(pluginName, pluginName, "1.0.0", "Test Author", null);
	}

	/**
	 * Builds a JAR with @AmsPlugin + @Listener + @Rest classes and valid V2 manifest.
	 */
	public static File buildPluginWithRestJar(String pluginName) throws Exception {
		File jar = tempFile(pluginName + ".jar");
		Manifest manifest = pluginManifest(pluginName, "1.0.0", "Test Author", null);
		Set<String> addedDirs = new HashSet<>();
		try (JarOutputStream out = new JarOutputStream(new FileOutputStream(jar), manifest)) {
			addClass(out, MinimalAmsPlugin.class, addedDirs);
			addClass(out, MinimalPluginRest.class, addedDirs);
		}
		return jar;
	}

	/**
	 * Full control builder.
	 */
	public static File buildPluginJar(String jarName, String pluginName, String version,
			String author, String requiresVersion) throws Exception {
		File jar = tempFile(jarName + ".jar");
		Manifest manifest = pluginManifest(pluginName, version, author, requiresVersion);
		Set<String> addedDirs = new HashSet<>();
		try (JarOutputStream out = new JarOutputStream(new FileOutputStream(jar), manifest)) {
			addClass(out, MinimalAmsPlugin.class, addedDirs);
		}
		return jar;
	}

	/**
	 * Builds a JAR with no AMS-Plugin-Name (missing required attribute).
	 */
	public static File buildJarMissingName() throws Exception {
		File jar = tempFile("missing-name.jar");
		Manifest m = new Manifest();
		m.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
		m.getMainAttributes().putValue("AMS-Plugin-Version", "1.0.0");
		m.getMainAttributes().putValue("AMS-Plugin-Author", "Test");
		Set<String> addedDirs = new HashSet<>();
		try (JarOutputStream out = new JarOutputStream(new FileOutputStream(jar), m)) {
			addClass(out, MinimalAmsPlugin.class, addedDirs);
		}
		return jar;
	}

	/**
	 * Builds a JAR with no AMS-Plugin-Version.
	 */
	public static File buildJarMissingVersion() throws Exception {
		File jar = tempFile("missing-version.jar");
		Manifest m = new Manifest();
		m.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
		m.getMainAttributes().putValue("AMS-Plugin-Name", "Test Plugin");
		m.getMainAttributes().putValue("AMS-Plugin-Author", "Test");
		Set<String> addedDirs = new HashSet<>();
		try (JarOutputStream out = new JarOutputStream(new FileOutputStream(jar), m)) {
			addClass(out, MinimalAmsPlugin.class, addedDirs);
		}
		return jar;
	}

	/**
	 * Builds a JAR with no AMS-Plugin-Author.
	 */
	public static File buildJarMissingAuthor() throws Exception {
		File jar = tempFile("missing-author.jar");
		Manifest m = new Manifest();
		m.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
		m.getMainAttributes().putValue("AMS-Plugin-Name", "Test Plugin");
		m.getMainAttributes().putValue("AMS-Plugin-Version", "1.0.0");
		Set<String> addedDirs = new HashSet<>();
		try (JarOutputStream out = new JarOutputStream(new FileOutputStream(jar), m)) {
			addClass(out, MinimalAmsPlugin.class, addedDirs);
		}
		return jar;
	}

	/**
	 * Builds a JAR with an incompatible version requirement (very high).
	 */
	public static File buildJarIncompatibleVersion() throws Exception {
		return buildPluginJar("incompatible", "Incompat Plugin", "1.0.0", "Test", "99.0.0");
	}

	/**
	 * Builds a JAR with no @AmsPlugin class (empty, just manifest).
	 */
	public static File buildJarNoAmsPlugin() throws Exception {
		File jar = tempFile("no-ams-plugin.jar");
		Manifest manifest = pluginManifest("No AmsPlugin", "1.0.0", "Test", null);
		try (JarOutputStream out = new JarOutputStream(new FileOutputStream(jar), manifest)) {
			// no class entries
		}
		return jar;
	}

	/**
	 * Wraps a JAR file into a V2 ZIP (plugin.jar inside).
	 */
	public static File wrapJarAsZip(File jarFile, String zipName) throws Exception {
		File zip = tempFile(zipName + ".zip");
		try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zip))) {
			zos.putNextEntry(new ZipEntry("plugin.jar"));
			try (FileInputStream fis = new FileInputStream(jarFile)) {
				byte[] buf = new byte[4096];
				int len;
				while ((len = fis.read(buf)) > 0) {
					zos.write(buf, 0, len);
				}
			}
			zos.closeEntry();
		}
		return zip;
	}

	/**
	 * Builds a ZIP with plugin.jar + install.sh
	 */
	public static File buildZipWithInstallScript(String pluginName, String scriptContent) throws Exception {
		File jar = buildPluginJar(pluginName);
		File zip = tempFile(pluginName + ".zip");
		try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zip))) {
			// plugin.jar
			zos.putNextEntry(new ZipEntry("plugin.jar"));
			try (FileInputStream fis = new FileInputStream(jar)) {
				byte[] buf = new byte[4096];
				int len;
				while ((len = fis.read(buf)) > 0) {
					zos.write(buf, 0, len);
				}
			}
			zos.closeEntry();

			// install.sh
			zos.putNextEntry(new ZipEntry("install.sh"));
			zos.write(scriptContent.getBytes());
			zos.closeEntry();

			// uninstall.sh (required when install.sh exists)
			zos.putNextEntry(new ZipEntry("uninstall.sh"));
			zos.write("#!/bin/bash\nexit 0\n".getBytes());
			zos.closeEntry();
		}
		return zip;
	}

	/**
	 * Builds a ZIP with no plugin.jar inside.
	 */
	public static File buildZipNoPluginJar() throws Exception {
		File zip = tempFile("no-plugin-jar.zip");
		try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zip))) {
			zos.putNextEntry(new ZipEntry("readme.txt"));
			zos.write("no plugin.jar here".getBytes());
			zos.closeEntry();
		}
		return zip;
	}

	// --- internals ---

	private static Manifest pluginManifest(String pluginName, String version, String author, String requiresVersion) {
		Manifest m = new Manifest();
		Attributes attrs = m.getMainAttributes();
		attrs.put(Attributes.Name.MANIFEST_VERSION, "1.0");
		attrs.putValue("AMS-Plugin-Name", pluginName);
		attrs.putValue("AMS-Plugin-Version", version);
		attrs.putValue("AMS-Plugin-Author", author);
		if (requiresVersion != null) {
			attrs.putValue("AMS-Plugin-Requires-Version", requiresVersion);
		}
		return m;
	}

	private static void addClass(JarOutputStream out, Class<?> clazz, Set<String> addedDirs) throws Exception {
		String entryPath = clazz.getName().replace('.', '/') + ".class";

		String[] segments = entryPath.split("/");
		StringBuilder dir = new StringBuilder();
		for (int i = 0; i < segments.length - 1; i++) {
			dir.append(segments[i]).append('/');
			String dirName = dir.toString();
			if (addedDirs.add(dirName)) {
				out.putNextEntry(new JarEntry(dirName));
				out.closeEntry();
			}
		}

		URL classUrl = clazz.getClassLoader().getResource(entryPath);
		if (classUrl == null) return;
		File classFile;
		try {
			classFile = new File(classUrl.toURI());
		} catch (URISyntaxException e) {
			return;
		}
		if (!classFile.exists()) return;

		out.putNextEntry(new JarEntry(entryPath));
		try (FileInputStream in = new FileInputStream(classFile)) {
			byte[] buf = new byte[2048];
			int read;
			while ((read = in.read(buf)) != -1) {
				out.write(buf, 0, read);
			}
		}
		out.closeEntry();
	}

	private static File tempFile(String name) throws IOException {
		File dir = new File(System.getProperty("java.io.tmpdir"), "ams-v2-plugin-test");
		dir.mkdirs();
		return new File(dir, name);
	}
}
