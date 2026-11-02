package io.antmedia.plugin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Test;

public class PluginPathsTest {

	private static final File BASE = new File("/tmp/ams-plugins");

	@Test
	public void testResolve_directChild() {
		File jar = PluginPaths.resolve(BASE, "clip-creator", ".jar");

		assertEquals("clip-creator.jar", jar.getName());
		assertEquals(BASE.getAbsolutePath(), jar.getParentFile().getAbsolutePath());
	}

	@Test
	public void testResolve_emptySuffixGivesDirectory() {
		File dir = PluginPaths.resolve(BASE, "clip-creator", "");

		assertEquals("clip-creator", dir.getName());
		assertEquals(BASE.getAbsolutePath(), dir.getParentFile().getAbsolutePath());
	}

	@Test
	public void testResolve_rejectsInvalidId() {
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
				() -> PluginPaths.resolve(BASE, "Not A Valid Id", ".jar"));

		assertTrue(e.getMessage().contains("Invalid plugin id"));
	}

	@Test
	public void testResolve_rejectsTraversal() {
		assertThrows(IllegalArgumentException.class, () -> PluginPaths.resolve(BASE, "../etc/passwd", ".jar"));
		assertThrows(IllegalArgumentException.class, () -> PluginPaths.resolve(BASE, "..", ".jar"));
		assertThrows(IllegalArgumentException.class, () -> PluginPaths.resolve(BASE, "a/b", ".jar"));
		assertThrows(IllegalArgumentException.class, () -> PluginPaths.resolve(BASE, null, ".jar"));
	}

	/**
	 * The charset rule permits dots so version-like ids work, which is exactly why a segment
	 * that is only dots has to be refused separately.
	 */
	@Test
	public void testResolve_allowsDottedIdButNotDotDot() {
		assertEquals("my.plugin.v2.jar", PluginPaths.resolve(BASE, "my.plugin.v2", ".jar").getName());
		assertThrows(IllegalArgumentException.class, () -> PluginPaths.resolve(BASE, "my..plugin", ".jar"));
	}

	@Test
	public void testForLog_stripsLineBreaks() {
		assertEquals("clip-creator", PluginPaths.forLog("clip-creator"));
		assertEquals("evil_INFO fake log entry", PluginPaths.forLog("evil\nINFO fake log entry"));
		assertEquals("a_b_c", PluginPaths.forLog("a\rb\tc"));
		assertEquals("null", PluginPaths.forLog(null));
	}
}
