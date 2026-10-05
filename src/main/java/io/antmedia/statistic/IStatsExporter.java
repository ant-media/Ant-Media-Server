package io.antmedia.statistic;

import java.io.IOException;

import com.google.gson.JsonObject;

public interface IStatsExporter {

	String INSTANCE_STATS = "ams-instance-stats";
	String WEBRTC_CLIENT_STATS = "ams-webrtc-stats";

	void start() throws IOException;

	void sendStats(JsonObject jsonObject, String type);

	void stop();
}
