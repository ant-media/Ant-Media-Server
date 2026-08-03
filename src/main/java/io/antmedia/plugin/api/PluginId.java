package io.antmedia.plugin.api;

import java.util.regex.Pattern;

/**
 * The rule for a plugin's machine identity, declared by {@code AMS-Plugin-Id} in the plugin
 * jar's manifest. Kept in one place so the REST, service and deployer layers cannot drift on
 * what a valid id is.
 */
public final class PluginId {

	/** Human-readable form of the rule, for error messages. */
	public static final String RULE = "[a-z0-9][a-z0-9._-]{0,63}";

	private static final Pattern PATTERN = Pattern.compile("^" + RULE + "$");

	private static final String NAME_SUFFIX = "-plugin";

	private PluginId() {
	}

	/**
	 * The id is used as a filename, a directory name and a URL segment, so the charset is
	 * restrictive. Dots are allowed for version-like ids, hence the explicit {@code ..} reject.
	 */
	public static boolean isValid(String id) {
		return id != null && PATTERN.matcher(id).matches() && !id.contains("..");
	}

	/**
	 * Derives an id from a display name, for jars packaged before {@code AMS-Plugin-Id} existed.
	 * Drops a trailing "-plugin" because plugins are named "&lt;Something&gt; Plugin" while their
	 * catalog id is just "&lt;something&gt;".
	 */
	public static String fromName(String name) {
		if (name == null) {
			return "";
		}
		String slug = name.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
		return slug.endsWith(NAME_SUFFIX) ? slug.substring(0, slug.length() - NAME_SUFFIX.length()) : slug;
	}
}
