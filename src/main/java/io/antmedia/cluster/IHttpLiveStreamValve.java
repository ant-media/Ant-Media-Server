package io.antmedia.cluster;

import io.antmedia.AppSettings;
import io.antmedia.servlet.IChunkedCacheManager;
import io.vertx.core.Vertx;

public interface IHttpLiveStreamValve {
	static final String M4S_EXTENSION = ".m4s";

	static final String M3U8_EXTENSION = ".m3u8";

	static final String TS_EXTENSION = ".ts";

	static final String MPD_EXTENSION = ".mpd";


	void downloadRequestedFileFromUrl(String url, AppSettings appSettings, String hostName, Vertx vertx, IChunkedCacheManager iChunkedCacheManager, String jwtSecretKey);
}
