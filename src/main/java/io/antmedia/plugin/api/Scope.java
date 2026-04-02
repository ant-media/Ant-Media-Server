package io.antmedia.plugin.api;

/**
 * Defines whether a plugin component is per-application or server-wide.
 */
public enum Scope {
	/**
	 * One instance per streaming application (LiveApp, WebRTCAppEE, etc.)
	 */
	APPLICATION,

	/**
	 * Single instance for the whole server
	 */
	SERVER
}
