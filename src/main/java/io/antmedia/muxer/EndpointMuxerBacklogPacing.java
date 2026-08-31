package io.antmedia.muxer;

import static org.bytedeco.ffmpeg.global.avcodec.AV_PKT_FLAG_KEY;

import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default pacing policy. Drops the backlog when our queue fills and restarts on the next keyframe.
 *
 * Timestamps are never rewritten, so the endpoint sees the hole. That is the point: a recording
 * at the destination still lines up with wall clock. {@link EndpointMuxerLiveEdgePacing} trades
 * that away for realtime stream.
 */
public class EndpointMuxerBacklogPacing implements EndpointMuxerPacingPolicy {

	/** EndpointMuxer category on purpose */
	private static final Logger logger = LoggerFactory.getLogger(EndpointMuxer.class);

	private static final int QUEUE_CAPACITY = 250;
	private static final long GRACE_PERIOD_MS = 1000L;
	/**A failsafe...  When we are dropping packets, every packet is dropped until we get a keyframe.
	 * If keyframe never arrives for this amount of time, we will just resume with any next packet. */
	private static final long RESUME_WAIT_LIMIT_MS = 10_000L;

	private final String url;

	private boolean graceStarted = false;
	private long graceStartNanos = 0;

	private boolean waitingForResume = false;
	private long cycleStartNanos = 0;

	public EndpointMuxerBacklogPacing(String url) {
		this.url = url;
	}

	@Override
	public int queueCapacity() {
		return QUEUE_CAPACITY;
	}

	@Override
	public PacingDecision onPacket(AVPacket pkt, EndpointMuxerPacingEngine engine) {
		if (inGracePeriod()) {
			return PacingDecision.discard();
		}
		if (!waitingForResume && engine.isFull()) {
			engine.flush();
			openCycle();
		}
		// Retried on this same packet, so a fresh cycle doesn't cost another GOP.
		if (waitingForResume && !tryResume(pkt, engine)) {
			return PacingDecision.discard();
		}
		return PacingDecision.pass(0);
	}

	/** Also holds for a keyframe, or we'd open mid-GOP on whatever arrives when the window ends. */
	private boolean inGracePeriod() {
		if (!graceStarted) {
			graceStarted = true;
			graceStartNanos = System.nanoTime();
			openCycle();
			logger.info("Startup grace period ({} ms) started for {}", GRACE_PERIOD_MS, url);
		}
		return (System.nanoTime() - graceStartNanos) / 1_000_000L < GRACE_PERIOD_MS;
	}

	private void openCycle() {
		waitingForResume = true;
		cycleStartNanos = System.nanoTime();
	}

	private boolean tryResume(AVPacket pkt, EndpointMuxerPacingEngine engine) {
		if (!isResumePoint(pkt, engine) && !waitedOut()) {
			return false;
		}
		waitingForResume = false;
		logger.info("Endpoint resumed for {} after {}ms", url, (System.nanoTime() - cycleStartNanos) / 1_000_000L);
		return true;
	}

	private boolean isResumePoint(AVPacket pkt, EndpointMuxerPacingEngine engine) {
		int videoIndex = engine.videoStreamIndex();
		if (videoIndex < 0) {
			return true;
		}
		return pkt.stream_index() == videoIndex && (pkt.flags() & AV_PKT_FLAG_KEY) != 0;
	}

	private boolean waitedOut() {
		return (System.nanoTime() - cycleStartNanos) / 1_000_000L > RESUME_WAIT_LIMIT_MS;
	}
}
