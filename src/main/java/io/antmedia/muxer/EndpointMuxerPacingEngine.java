package io.antmedia.muxer;

import static org.bytedeco.ffmpeg.global.avcodec.av_packet_clone;
import static org.bytedeco.ffmpeg.global.avcodec.av_packet_free;
import static org.bytedeco.ffmpeg.global.avutil.av_rescale_q;

import java.util.concurrent.LinkedBlockingQueue;

import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avutil.AVRational;
import org.bytedeco.ffmpeg.global.avutil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.antmedia.muxer.EndpointMuxerPacingPolicy.Action;
import io.antmedia.muxer.EndpointMuxerPacingPolicy.PacingDecision;

/**
 * Sits between the packet producer and the drain. Producer submits, policy decides, drain takes
 * what survived.
 *
 * Never sees an AVFormatContext, only time bases the muxer copied out of one, so nothing here
 * can read a freed context.
 */
public class EndpointMuxerPacingEngine {

	/** EndpointMuxer category, so endpoint logs filter as one. */
	private static final Logger logger = LoggerFactory.getLogger(EndpointMuxer.class);
	private static final long ISSUE_LOG_INTERVAL_MS = 5_000L;

	private final LinkedBlockingQueue<AVPacket> packetQueue;
	private final Object queueLock = new Object();
	private final EndpointMuxerAnalytics analytics;
	private final EndpointMuxerPacingPolicy policy;

	/** Muxer owned copies, not pointers into the context. Indexed by output stream index. */
	private final AVRational[] timeBases;
	/** -1 when the endpoint carries no video. */
	private final int videoStreamIndex;

	private long lastIssueLogMs = 0;

	EndpointMuxerPacingEngine(EndpointMuxerPacingPolicy policy, EndpointMuxerAnalytics analytics,
			AVRational[] timeBases, int videoStreamIndex) {
		this.policy = policy;
		this.analytics = analytics;
		this.timeBases = timeBases;
		this.videoStreamIndex = videoStreamIndex;
		this.packetQueue = new LinkedBlockingQueue<>(policy.queueCapacity());
	}

	/** Queues a clone already shifted onto the output timeline, so the drain writes it as is. */
	void submit(AVPacket src) {
		// Rejected here, not in shift(), which would queue it unshifted onto a shifted timeline.
		if (timeBaseOf(src) == null) {
			return;
		}
		PacingDecision decision = policy.onPacket(src, this);
		if (decision.action() == Action.DISCARD) {
			return;
		}
		AVPacket clone = av_packet_clone(src);
		if (clone == null) {
			logPacketIssue("Cannot clone packet for the endpoint queue");
			return;
		}
		shift(clone, decision.shiftMs());
		// Defensive. Only the producer fills the queue, so isFull can't go stale.
		if (!packetQueue.offer(clone)) {
			av_packet_free(clone);
		}
	}

	/** @return the next packet, or null when empty. The caller owns it and must free it. */
	AVPacket drainNext() {
		synchronized (queueLock) {
			return packetQueue.poll();
		}
	}

	void close() {
		synchronized (queueLock) {
			AVPacket pkt;
			while ((pkt = packetQueue.poll()) != null) {
				av_packet_free(pkt);
			}
		}
	}

	int size() {
		return packetQueue.size();
	}

	// ---- policy facing ----

	public boolean isFull() {
		return packetQueue.remainingCapacity() == 0;
	}

	public int videoStreamIndex() {
		return videoStreamIndex;
	}

	public int streamCount() {
		return timeBases.length;
	}

	/** @return dts in ms, or AV_NOPTS_VALUE when absent or the stream index is out of range. */
	public long toMs(AVPacket pkt) {
		AVRational timeBase = timeBaseOf(pkt);
		if (timeBase == null || pkt.dts() == avutil.AV_NOPTS_VALUE) {
			return avutil.AV_NOPTS_VALUE;
		}
		return av_rescale_q(pkt.dts(), timeBase, MuxAdaptor.TIME_BASE_FOR_MS);
	}

	/**
	 * Empties the queue and reports where the drain would have carried on. Exact, not a guess:
	 * the queue holds what the drain has not written yet, in order.
	 *
	 * @return oldest queued video dts in ms, else oldest of any stream, else AV_NOPTS_VALUE.
	 */
	public long flush() {
		long videoMs = avutil.AV_NOPTS_VALUE;
		long anyMs = avutil.AV_NOPTS_VALUE;
		int dropped;
		synchronized (queueLock) {
			dropped = packetQueue.size();
			AVPacket pkt;
			while ((pkt = packetQueue.poll()) != null) {
				try {
					long ms = toMs(pkt);
					if (ms == avutil.AV_NOPTS_VALUE) {
						continue;
					}
					if (anyMs == avutil.AV_NOPTS_VALUE) {
						anyMs = ms;
					}
					if (videoMs == avutil.AV_NOPTS_VALUE && pkt.stream_index() == videoStreamIndex) {
						videoMs = ms;
					}
				}
				finally {
					// In a finally so a throw mid-scan can't leak a queue full of native packets.
					av_packet_free(pkt);
				}
			}
		}
		long anchorMs = videoMs != avutil.AV_NOPTS_VALUE ? videoMs : anyMs;
		if (anchorMs != avutil.AV_NOPTS_VALUE) {
			analytics.recordDrop(dropped);
		}
		else if (dropped > 0) {
			logPacketIssue("Flushed {} endpoint packets with no usable dts", dropped);
		}
		return anchorMs;
	}

	// ---- internal ----

	private void shift(AVPacket pkt, long offsetMs) {
		if (offsetMs == 0) {
			return;
		}
		AVRational timeBase = timeBaseOf(pkt);
		if (timeBase == null) {
			return;
		}
		long offset = av_rescale_q(offsetMs, MuxAdaptor.TIME_BASE_FOR_MS, timeBase);
		// AV_NOPTS_VALUE is Long.MIN_VALUE, so subtracting overflows it into a real timestamp.
		if (pkt.pts() != avutil.AV_NOPTS_VALUE) {
			pkt.pts(pkt.pts() - offset);
		}
		if (pkt.dts() != avutil.AV_NOPTS_VALUE) {
			pkt.dts(pkt.dts() - offset);
		}
	}

	/** Backstop, so a bad index is a Java bounds check instead of a native out of bounds read. */
	private AVRational timeBaseOf(AVPacket pkt) {
		int index = pkt.stream_index();
		if (index < 0 || index >= timeBases.length) {
			logPacketIssue("Packet stream index:{} is out of range for the endpoint, {} streams configured",
					index, timeBases.length);
			return null;
		}
		return timeBases[index];
	}

	/** Throttled, because these fire at packet rate. */
	private void logPacketIssue(String format, Object... arguments) {
		long now = System.currentTimeMillis();
		if (now - lastIssueLogMs >= ISSUE_LOG_INTERVAL_MS) {
			lastIssueLogMs = now;
			logger.warn(format, arguments);
		}
	}
}
