package io.antmedia.statistic;

import com.google.gson.JsonObject;

import io.antmedia.analytic.model.PublishStatsEvent;
import io.antmedia.statistic.type.StreamMetricsHistory;

public interface IStatsCollector {
	public static final String BEAN_NAME = "statsCollector";

	/**
	 * It calculates the average CPU usage for a specified time.
	 * @return the current CPU usage
	 */
	public int getCpuLoad();

	/** 
	 * It's configurable and it's based on percentage. 
	 * Max value is 100.
	 * @return the CPU limit that server does not exceed.
	 */
	public int getCpuLimit();
	
	/**
	 * It's configurable
	 * In MB
	 * @return the free RAM size that server should have all the time
	 */
	public int getMinFreeRamSize();
	
	/**
	 * In MB
	 * @return the free RAM that server can use
	 */
	public int getFreeRam();
	
	/**
	 * Check if cpu usage and ram usage does not exceed the limit
	 * @return true if not exceeding the limit, false if exceeding limit
	 */
	public boolean enoughResource();

	/**
	 * Recent system resource usage history (cpu/mem/disk/heap/db/live), oldest first.
	 * @return metric key to value-array JSON
	 */
	public JsonObject getSystemResourcesHistory();

	/**
	 * Recent per-app metric history (viewers, live streams), oldest first.
	 * @param appName application name
	 * @return metric key to value-array JSON; empty arrays when the app is unknown
	 */
	public JsonObject getAppMetricsHistory(String appName);

	/**
	 * Current network throughput across physical NICs.
	 * @return {outboundMbps, inboundMbps, uplinkMbps} JSON
	 */
	public JsonObject getNetworkStatus();

	/**
	 * Append one per-stream metric sample. Push-fed from the stream's quality update; bitrate is derived
	 * from the {@code totalByteReceived} delta against the previous sample's timestamp. No-op when disabled.
	 * @param appName application the stream belongs to
	 * @param streamId stream id
	 * @param stats latest publish stats for the stream
	 * @param viewers total viewers (WebRTC + HLS + DASH)
	 * @param timestampMs sample wall-clock time, ms
	 */
	public void addStreamSample(String appName, String streamId, PublishStatsEvent stats, int viewers, long timestampMs);

	/**
	 * Drop a stream's metric history, called when the stream ends.
	 * @param appName application the stream belonged to
	 * @param streamId stream id
	 */
	public void removeStreamHistory(String appName, String streamId);

	/**
	 * Recent per-stream metric history (bitrate/viewers/speed/queue/drops/loss), oldest first.
	 * @param appName application the stream belongs to
	 * @param streamId stream id
	 * @return parallel value arrays; empty arrays when the stream is unknown
	 */
	public StreamMetricsHistory getStreamMetricsHistory(String appName, String streamId);

}
