package io.antmedia.muxer;

import java.util.concurrent.atomic.AtomicLong;

import org.bytedeco.ffmpeg.global.avutil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Per-endpoint analytics stuff
 * {@link #recordDrop} is producer-thread (atomic+volatile);
 * {@link #recordWrite} is called only from the drain job, which runs one-at-a-time
 * (guarded by {@code drainScheduled}), so its plain fields need no synchronization.
 */
public class EndpointMuxerAnalytics {
	/** Deliberately the EndpointMuxer category, so operators keep one logger to filter on. */
	private static final Logger logger = LoggerFactory.getLogger(EndpointMuxer.class);

	// --- Drop counter (producer thread). ---
	private static final long DROP_LOG_INTERVAL_MS = 5_000L;
	private final AtomicLong dropCount = new AtomicLong();
	private volatile long lastDropLogMs = 0L;

	// --- Write-latency analytics (worker thread only). ---
	private static final long WRITE_STATS_LOG_INTERVAL_MS = 10_000L;
	/** A write is flagged only if it exceeds both this floor AND {@link #WRITE_SPIKE_RATIO}× baseline. */
	private static final long WRITE_SPIKE_FLOOR_NANOS = 100_000_000L;
	private static final long WRITE_SPIKE_RATIO = 5L;
	private long writeAccumNanos = 0L;
	private long writeMaxNanos = 0L;
	private int writeCount = 0;
	/** EWMA of per-window averages, a multi-window baseline for spike detection. */
	private long writeEwmaNanos = 0L;
	private long lastWriteStatsLogMs = 0L;

	// --- Burst-flush detection (worker thread only). ---
	/** Two consecutive writes are "back-to-back" if their inter-gap is below this. */
	private static final long BURST_GAP_NANOS = 2_000_000L;
	private static final int BURST_THRESHOLD = 3;
	/**
	 * DTS span (ms in FLV time base) the burst must cover. Set well above
	 * natural AV-interleave (~42ms) and routine source batching (~150ms),
	 * so anything firing here is real pathology.
	 */
	private static final long BURST_DTS_SPAN_THRESHOLD_MS = 500L;
	private long lastWriteEndNanos = 0L;
	private int burstCount = 0;
	private long burstStartMaxDts = Long.MIN_VALUE;
	private long currentMaxDts = Long.MIN_VALUE;
	private boolean burstWarned = false;

	private final String url;
	private final int queueCapacity;

	EndpointMuxerAnalytics(String url, int queueCapacity) {
		this.url = url;
		this.queueCapacity = queueCapacity;
	}

	/** One warn per {@link #DROP_LOG_INTERVAL_MS} regardless of drop rate. */
	public void recordDrop(int droppedPackets) {
		long count = dropCount.incrementAndGet();
		long now = System.currentTimeMillis();
		if (now - lastDropLogMs >= DROP_LOG_INTERVAL_MS) {
			lastDropLogMs = now;
			logger.warn("Endpoint backlog dropped {} packets for {}, {} drops total (capacity {})",
					droppedPackets, url, count, queueCapacity);
		}
	}

	/**
	 * Records per-write timing, detects burst-flushes, and emits periodic stats.
	 * Worker-thread only, so fields are unsynchronized by design.
	 */
	public void recordWrite(long durNanos, long pktDts, int queueDepth) {
		writeAccumNanos += durNanos;
		if (durNanos > writeMaxNanos) {
			writeMaxNanos = durNanos;
		}
		writeCount++;

		// Cross-stream max DTS, skipping AV_NOPTS_VALUE and non-monotonic regressions.
		if (pktDts != avutil.AV_NOPTS_VALUE
				&& (currentMaxDts == Long.MIN_VALUE || pktDts > currentMaxDts)) {
			currentMaxDts = pktDts;
		}

		// Flag a real backlog drain: many sub-gap writes covering a meaningful
		// chunk of DTS. One warn per burst event.
		long writeEndNanos = System.nanoTime();
		if (lastWriteEndNanos != 0L
				&& (writeEndNanos - lastWriteEndNanos) - durNanos < BURST_GAP_NANOS) {
			burstCount++;
			// Lazy init: capture the burst's DTS baseline at the first valid
			// sample to avoid Long.MIN_VALUE overflow in the span subtraction.
			if (burstStartMaxDts == Long.MIN_VALUE) {
				burstStartMaxDts = currentMaxDts;
			}
			if (burstStartMaxDts != Long.MIN_VALUE) {
				long dtsSpan = currentMaxDts - burstStartMaxDts;
				if (!burstWarned
						&& burstCount > BURST_THRESHOLD
						&& dtsSpan > BURST_DTS_SPAN_THRESHOLD_MS) {
					logger.warn("Worker burst-flush: {} packets back-to-back covering {}ms DTS for {} (qDepth={}/{})",
							burstCount, dtsSpan, url, queueDepth, queueCapacity);
					burstWarned = true;
				}
			}
		} else {
			burstCount = 1;
			burstStartMaxDts = currentMaxDts;
			burstWarned = false;
		}
		lastWriteEndNanos = writeEndNanos;

		if (durNanos > WRITE_SPIKE_FLOOR_NANOS
				&& writeEwmaNanos > 0L
				&& durNanos > writeEwmaNanos * WRITE_SPIKE_RATIO) {
			logger.warn("Write latency spike for {}: {}ms (baseline {}ms)",
					url, durNanos / 1_000_000L, writeEwmaNanos / 1_000_000L);
		}

		long now = System.currentTimeMillis();
		if (now - lastWriteStatsLogMs >= WRITE_STATS_LOG_INTERVAL_MS && writeCount > 0) {
			long avgNanos = writeAccumNanos / writeCount;
			// EWMA alpha = 0.2 (~5-window memory).
			writeEwmaNanos = (writeEwmaNanos == 0L)
					? avgNanos
					: (avgNanos / 5L) + (writeEwmaNanos * 4L / 5L);

			logger.info("Write timing for {}: n={} avg={}us max={}ms qDepth={}/{}",
					url, writeCount, avgNanos / 1000L, writeMaxNanos / 1000000L,
					queueDepth, queueCapacity);

			writeAccumNanos = 0L;
			writeMaxNanos = 0L;
			writeCount = 0;
			lastWriteStatsLogMs = now;
		}
	}
}
