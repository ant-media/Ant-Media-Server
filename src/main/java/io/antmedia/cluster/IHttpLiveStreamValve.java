package io.antmedia.cluster;

import io.antmedia.servlet.IChunkedCacheManager;
import io.vertx.core.Vertx;

public interface IHttpLiveStreamValve {
	public static final String M4S_EXTENSION = ".m4s";

	public static final String M3U8_EXTENSION = ".m3u8";

	public static final String TS_EXTENSION = ".ts";

	public static final String MPD_EXTENSION = ".mpd";


	void downloadRequestedFile(RequestInfo reqInfo, String hostName, Vertx vertx, IChunkedCacheManager iChunkedCacheManager, String jwtSecretKey);
}
