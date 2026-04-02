package io.antmedia.test.plugin;

import io.antmedia.plugin.api.AmsPlugin;
import io.antmedia.plugin.api.IServerListener;
import io.antmedia.plugin.api.Inject;
import io.antmedia.plugin.api.Listener;
import io.antmedia.plugin.api.Scope;
import io.antmedia.plugin.api.IStreamListener;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.muxer.IAntMediaStreamHandler;
import io.vertx.core.Vertx;

/**
 * Minimal V2 plugin used in unit tests.
 * Combines @AmsPlugin + @Listener on the same class (common pattern).
 */
@AmsPlugin
@Listener(scope = Scope.APPLICATION)
public class MinimalAmsPlugin implements IStreamListener, IServerListener {

	@Inject
	private IAntMediaStreamHandler app;

	@Inject
	private Vertx vertx;

	private boolean pluginEnabled = false;
	private boolean pluginDisabled = false;
	private int streamStartedCount = 0;

	@Override
	public void onPluginEnabled(String pluginName) {
		pluginEnabled = true;
	}

	@Override
	public void onPluginDisabled(String pluginName) {
		pluginDisabled = true;
	}

	@Override
	public void streamStarted(Broadcast broadcast) {
		streamStartedCount++;
	}

	@Override
	public void streamFinished(Broadcast broadcast) {
	}

	@Override
	public void joinedTheRoom(String roomId, String streamId) {
	}

	@Override
	public void leftTheRoom(String roomId, String streamId) {
	}

	public boolean isPluginEnabled() { return pluginEnabled; }
	public boolean isPluginDisabled() { return pluginDisabled; }
	public int getStreamStartedCount() { return streamStartedCount; }
	public IAntMediaStreamHandler getApp() { return app; }
	public Vertx getVertx() { return vertx; }
}
