package io.antmedia.statistic.type;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Per-stream metric history, projected oldest -> newest as parallel arrays (one entry per sample).
 * Served by GET /broadcasts/{id}/metrics-history; backed by an in-memory ring in StatsCollector.
 */
@Schema(description = "Per-stream metric history as parallel, oldest-to-newest arrays")
public class StreamMetricsHistory {

	@Schema(description = "Instantaneous ingest bitrate per sample, bits/sec")
	private long[] bitrate;

	@Schema(description = "Total viewers per sample (WebRTC + HLS + DASH)")
	private int[] viewers;

	@Schema(description = "Ingest speed per sample; 1.0 means real-time")
	private double[] speed;

	@Schema(description = "Encoder input queue size per sample; should stay low")
	private int[] encoderQueueSize;

	@Schema(description = "Cumulative dropped packets at ingest per sample")
	private int[] droppedPackets;

	@Schema(description = "Cumulative dropped frames at encoding per sample")
	private int[] droppedFrames;

	@Schema(description = "WebRTC ingest packet-loss ratio per sample (0.01 = 1%)")
	private double[] packetLostRatio;

	public StreamMetricsHistory(long[] bitrate, int[] viewers, double[] speed, int[] encoderQueueSize,
			int[] droppedPackets, int[] droppedFrames, double[] packetLostRatio) {
		this.bitrate = bitrate;
		this.viewers = viewers;
		this.speed = speed;
		this.encoderQueueSize = encoderQueueSize;
		this.droppedPackets = droppedPackets;
		this.droppedFrames = droppedFrames;
		this.packetLostRatio = packetLostRatio;
	}

	public long[] getBitrate() {
		return bitrate;
	}

	public int[] getViewers() {
		return viewers;
	}

	public double[] getSpeed() {
		return speed;
	}

	public int[] getEncoderQueueSize() {
		return encoderQueueSize;
	}

	public int[] getDroppedPackets() {
		return droppedPackets;
	}

	public int[] getDroppedFrames() {
		return droppedFrames;
	}

	public double[] getPacketLostRatio() {
		return packetLostRatio;
	}
}
