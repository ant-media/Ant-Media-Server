package io.antmedia.plugin.api;

import io.antmedia.RecordType;

/**
 * Server and application lifecycle listener for V2 plugins.
 * All methods have default empty implementations — override only what you need.
 */
public interface IServerListener {

	default void onServerStarted() { }

	default void onServerStopped() { }

	default void onApplicationStarted(String appName) { }

	default void onApplicationStopped(String appName) { }

	default void onApplicationCreated(String appName) { }

	default void onApplicationDeleted(String appName) { }

	default void onPluginEnabled(String pluginName) { }

	default void onPluginDisabled(String pluginName) { }

	default void onRecordingStarted(String streamId, RecordType type) { }

	default void onRecordingSegmentFinished(String streamId, String segmentPath) { }

	default void onRecordingFinished(String streamId, String vodId) { }

	default void onVodPlaybackStarted(String vodId, String subscriberId) { }

	default void onDataReceived(String streamId, String data) { }
}
