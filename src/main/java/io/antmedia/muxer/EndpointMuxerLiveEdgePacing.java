package io.antmedia.muxer;

import static org.bytedeco.ffmpeg.global.avcodec.AV_PKT_FLAG_KEY;

import java.util.Arrays;

import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.global.avutil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Opt-in pacing that holds the endpoint at the live edge. Everything dropped is charged to
 * {@link #offsetMs} and subtracted from what follows, so the endpoint never sees a hole.
 *
 * The cost: output media time stops matching wall clock, so a recording at the destination comes
 * out short with no sign anything was cut. {@link EndpointMuxerBacklogPacing} is the default.
 *
 * A cycle opens on a full queue, on a dts jump (GAP, we discarded nothing), or on arrivals
 * falling behind wall clock with continuous timestamps (LAG, only wall clock exposes it).
 */
public class EndpointMuxerLiveEdgePacing implements EndpointMuxerPacingPolicy {

	/** EndpointMuxer category on purpose */
	private static final Logger logger = LoggerFactory.getLogger(EndpointMuxer.class);

	/** Keep this low. */
	private static final int QUEUE_CAPACITY = 100;
	private static final long GRACE_PERIOD_MS = 1000L;
	/** When we are dropping packets, every packet is dropped until we get a keyframe.
	 * If keyframe never arrives for this amount of time, we will just resume with any next packet. */
	private static final long RESUME_WAIT_LIMIT_MS = 10_000L;

	/**
	 * A dts jump this big on one stream means the publisher dropped frames. 
	 * Must stay above the source frame interval.
	 * WARNING: Sources below <= 1 fps every frame, reads as a gap and will be a problem
	 */
	private static final long GAP_THRESHOLD_MS = 1000L;
	/** A source persistently slower than realtime gets a jump cut every time it burns this. */
	private static final long LAG_THRESHOLD_MS = 3000L;
	/** Where a lag skip lands, under the trigger so it does not re-fire. */
	private static final long LAG_RESUME_TARGET_MS = 1000L;
	private static final long MAX_LEARNED_STEP_MS = 1000L;
	private static final long DEFAULT_STEP_MS = 33L;

	private final String url;

	/** total media time removed so far. Only changes on resume, which only happens on an empty queue. */
	private long offsetMs = 0;
	/** Output time the open cycle cut from. AV_NOPTS_VALUE means no cycle is open. */
	private long dropAnchorOutputMs = avutil.AV_NOPTS_VALUE;
	private long cycleStartNanos = 0;

	private boolean graceStarted = false;
	private long graceStartNanos = 0;

	private long[] lastInputDtsMs;
	/** Lag measures from here, not the current packet, so skew cannot read as lateness. */
	private long maxInputDtsMs = avutil.AV_NOPTS_VALUE;
	/** Cushions where a gap was resumed */
	private long inputStepMs = DEFAULT_STEP_MS;

	private long wallStartNanos = 0;
	private long dtsStartMs = avutil.AV_NOPTS_VALUE;
	/** Resume must reach this dts, so one cycle skips the whole stale backlog. */
	private long skipToDtsMs = avutil.AV_NOPTS_VALUE;
	/** Gap cycles: we discarded nothing, decoder is whole, so any video packet resumes. */
	private boolean resumeOnAnyVideo = false;

	public EndpointMuxerLiveEdgePacing(String url) {
		this.url = url;
	}

	@Override
	public int queueCapacity() {
		return QUEUE_CAPACITY;
	}

	@Override
	public PacingDecision onPacket(AVPacket pkt, EndpointMuxerPacingEngine engine) {
		detectStall(pkt, engine);
		if (inGracePeriod()) {
			return PacingDecision.discard();
		}

		if (!isDropCycleOpen() && engine.isFull()) {
			beginDropCycle(engine.flush());
		}
		
		// Retried on this same packet, so a fresh cycle doesn't cost another GOP.
		if (isDropCycleOpen() && !tryResume(pkt, engine)) {
			return PacingDecision.discard();
		}
		
		return PacingDecision.pass(offsetMs);
	}

	/** Anchored at 0, not the packet dts: the header write already pushed that past the origin. */
	private boolean inGracePeriod() {
		if (!graceStarted) {
			graceStarted = true;
			graceStartNanos = System.nanoTime();
			beginDropCycle(0);
			logger.info("Startup grace period ({} ms) started for {}", GRACE_PERIOD_MS, url);
		}
		return (System.nanoTime() - graceStartNanos) / 1_000_000L < GRACE_PERIOD_MS;
	}

	private boolean isDropCycleOpen() {
		return dropAnchorOutputMs != avutil.AV_NOPTS_VALUE;
	}

	/** AV_NOPTS_VALUE means the drain emptied the queue first, so nothing was lost to charge. */
	private void beginDropCycle(long anchorOutputMs) {
		if (anchorOutputMs == avutil.AV_NOPTS_VALUE) {
			return;
		}
		cycleStartNanos = System.nanoTime();
		dropAnchorOutputMs = anchorOutputMs;
	}

	private boolean tryResume(AVPacket pkt, EndpointMuxerPacingEngine engine) {
		long dtsMs = engine.toMs(pkt);
		if (dtsMs == avutil.AV_NOPTS_VALUE) {
			return false;
		}
		if (!isResumePoint(pkt, engine) && !waitedOut()) {
			return false;
		}
		long previousOffsetMs = offsetMs;

		// Assignment, not accumulation. The anchor came off the queue already carrying offsetMs.
		offsetMs = dtsMs - dropAnchorOutputMs;
		dropAnchorOutputMs = avutil.AV_NOPTS_VALUE;

		// Charge covered everything up to here, so a stale dts must not read as a second gap.
		if (lastInputDtsMs != null) {
			Arrays.fill(lastInputDtsMs, dtsMs);
		}
		maxInputDtsMs = dtsMs;
		wallStartNanos = System.nanoTime();
		dtsStartMs = dtsMs;
		skipToDtsMs = avutil.AV_NOPTS_VALUE;
		resumeOnAnyVideo = false;

		logger.info("Endpoint resumed for {}: dropped {}ms, offset now {}ms", url, offsetMs - previousOffsetMs, offsetMs);
		return true;
	}

	/**
	 * Video resumes on a keyframe, since the drop threw away frames the decoder needs. Audio-only
	 * takes anything, or it would wait forever. The index check is not redundant: writeAudioBuffer
	 * flags every audio packet as a keyframe.
	 */
	private boolean isResumePoint(AVPacket pkt, EndpointMuxerPacingEngine engine) {
		if (skipToDtsMs != avutil.AV_NOPTS_VALUE && engine.toMs(pkt) < skipToDtsMs) {
			return false;
		}
		int videoIndex = engine.videoStreamIndex();
		if (videoIndex < 0) {
			return true;
		}
		if (pkt.stream_index() != videoIndex) {
			return false;
		}
		return resumeOnAnyVideo || (pkt.flags() & AV_PKT_FLAG_KEY) != 0;
	}

	private boolean waitedOut() {
		return (System.nanoTime() - cycleStartNanos) / 1_000_000L > RESUME_WAIT_LIMIT_MS;
	}

	private void detectStall(AVPacket pkt, EndpointMuxerPacingEngine engine) {
		if (isDropCycleOpen()) {
			return;
		}

		long dtsMs = engine.toMs(pkt);
		if (dtsMs == avutil.AV_NOPTS_VALUE) {
			return;
		}

		if (lastInputDtsMs == null) {
			lastInputDtsMs = new long[engine.streamCount()];
			Arrays.fill(lastInputDtsMs, avutil.AV_NOPTS_VALUE);
		}

		int index = pkt.stream_index();
		if (index < 0 || index >= lastInputDtsMs.length) {
			return;
		}

		long nowNanos = System.nanoTime();
		long previous = lastInputDtsMs[index];
		lastInputDtsMs[index] = dtsMs;
		// Seed once, off whichever stream arrives first. Re-seeding drags maxInputDtsMs back.
		if (dtsStartMs == avutil.AV_NOPTS_VALUE) {
			wallStartNanos = nowNanos;
			dtsStartMs = dtsMs;
			maxInputDtsMs = dtsMs;
		}

		if (previous == avutil.AV_NOPTS_VALUE) {
			return;
		}

		// Before maxInputDtsMs moves: this dts sits past the jump, anchoring there charges nothing.
		long deltaMs = dtsMs - previous;
		if (deltaMs > GAP_THRESHOLD_MS) {
			logger.info("Detected {}ms publisher gap for {}", deltaMs, url);
			openStallCycle(engine, true);
			return;
		}

		if (dtsMs > maxInputDtsMs) {
			maxInputDtsMs = dtsMs;
		}

		// One packet interval only, so a gap must not teach it.
		if (deltaMs > 0 && deltaMs <= MAX_LEARNED_STEP_MS) {
			inputStepMs = deltaMs;
		}
		detectLag(engine, nowNanos);
	}

	private void detectLag(EndpointMuxerPacingEngine engine, long nowNanos) {
		long lagMs = ((nowNanos - wallStartNanos) / 1_000_000L) - (maxInputDtsMs - dtsStartMs);
		// Re-anchor when arrivals run ahead, so a fast source can't bank margin that masks a stall.
		if (lagMs < 0) {
			wallStartNanos = nowNanos;
			dtsStartMs = maxInputDtsMs;
			return;
		}
		if (lagMs > LAG_THRESHOLD_MS) {
			logger.info("Arrivals lag live by {}ms for {}, skipping forward", lagMs, url);
			skipToDtsMs = maxInputDtsMs + (lagMs - LAG_RESUME_TARGET_MS);
			openStallCycle(engine, false);
		}
	}

	private void openStallCycle(EndpointMuxerPacingEngine engine, boolean nothingDiscarded) {
		long flushedMs = engine.flush();
		// Anything flushed was undelivered, so that cycle owes the decoder a keyframe.
		resumeOnAnyVideo = nothingDiscarded && flushedMs == avutil.AV_NOPTS_VALUE;
		// flush() returns queue time, already shifted. maxInputDtsMs is source time, so shift it.
		beginDropCycle(flushedMs != avutil.AV_NOPTS_VALUE ? flushedMs : maxInputDtsMs + inputStepMs - offsetMs);
	}
}
