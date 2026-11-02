package io.antmedia.plugin;

import java.io.File;
import java.nio.file.Path;

import io.antmedia.plugin.api.PluginId;

/**
 * Builds every on-disk path derived from a plugin id, in one auditable place.
 *
 * <p>A plugin id arrives as a REST path parameter, so it is untrusted until proven otherwise.
 * {@link PluginId#isValid(String)} already rejects anything outside a strict charset, but a
 * charset check alone is easy to bypass by refactoring and impossible for a static analyser to
 * recognise as a sanitiser. Every path is therefore also resolved, normalised and required to
 * land <em>directly</em> inside the base directory — no subdirectories, no traversal.</p>
 */
public final class PluginPaths {

	private PluginPaths() {
		// utility
	}

	/**
	 * Resolves {@code {baseDir}/{pluginId}{suffix}}, refusing anything that is not a direct
	 * child of {@code baseDir}.
	 *
	 * @param suffix file extension such as {@code ".jar"}, or {@code ""} for a directory
	 * @throws IllegalArgumentException if the id is invalid or the result escapes the base
	 */
	public static File resolve(File baseDir, String pluginId, String suffix) {
		if (!PluginId.isValid(pluginId)) {
			throw new IllegalArgumentException("Invalid plugin id: " + forLog(pluginId));
		}

		Path base = baseDir.toPath().toAbsolutePath().normalize();
		Path resolved = base.resolve(pluginId + suffix).normalize();

		// Direct child only. startsWith() alone would still allow a nested path if the id ever
		// grew a separator, so the parent has to be the base directory itself.
		if (!base.equals(resolved.getParent())) {
			throw new IllegalArgumentException("Plugin path escapes the plugins directory: " + forLog(pluginId));
		}
		return resolved.toFile();
	}

	/**
	 * Strips line breaks and tabs so a value that reached us from a request cannot forge or
	 * split log records.
	 */
	public static String forLog(String value) {
		return value == null ? "null" : value.replaceAll("[\\r\\n\\t]", "_");
	}
}
