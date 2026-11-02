package org.red5.server.classloading;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.util.Arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Covers {@link ServerClassLoader#addPluginJar(URL)} — the hook the plugin deployer uses to put
 * a freshly installed jar on the classpath of a running server.
 */
public class ServerClassLoaderTest {

	private String previousRed5Root;
	private File root;

	@Before
	public void setUp() throws Exception {
		previousRed5Root = System.getProperty("red5.root");

		// getJars() iterates {red5.root}/lib without a null check, so the directory has to exist
		// before the classloader can be constructed at all.
		root = Files.createTempDirectory("ams-classloader-test").toFile();
		Files.createDirectory(new File(root, "lib").toPath());
		Files.createDirectory(new File(root, "plugins").toPath());
		System.setProperty("red5.root", root.getAbsolutePath());
	}

	@After
	public void tearDown() {
		if (previousRed5Root != null) {
			System.setProperty("red5.root", previousRed5Root);
		}
		else {
			System.clearProperty("red5.root");
		}
		deleteRecursively(root);
	}

	@Test
	public void testAddPluginJar_appendsToSearchPath() throws Exception {
		try (ServerClassLoader loader = new ServerClassLoader(getClass().getClassLoader())) {
			URL jar = new File(root, "plugins/clip-creator.jar").toURI().toURL();
			assertFalse("precondition: the jar must not already be on the path",
					Arrays.asList(loader.getURLs()).contains(jar));

			loader.addPluginJar(jar);

			assertTrue("addPluginJar must expose the jar to the classloader",
					Arrays.asList(loader.getURLs()).contains(jar));
		}
	}

	private static void deleteRecursively(File file) {
		if (file == null || !file.exists()) {
			return;
		}
		File[] children = file.listFiles();
		if (children != null) {
			for (File child : children) {
				deleteRecursively(child);
			}
		}
		file.delete();
	}
}
