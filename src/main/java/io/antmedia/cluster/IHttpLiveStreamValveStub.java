package io.antmedia.cluster;

import io.antmedia.AppSettings;
import io.antmedia.servlet.IChunkedCacheManager;
import io.vertx.core.Vertx;

// Just a dummy class to make JVM not fail when loading plugins, if there is no enterprise package.
// This will never be used if enterprise packet is available.
public class IHttpLiveStreamValveStub implements IHttpLiveStreamValve {
	@Override
	public void downloadRequestedFileFromUrl(String url, AppSettings appSettings, String hostName, Vertx vertx, IChunkedCacheManager iChunkedCacheManager, String jwtSecretKey) {
	}
}
