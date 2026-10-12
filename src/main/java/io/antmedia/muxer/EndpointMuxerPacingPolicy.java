package io.antmedia.muxer;

import org.bytedeco.ffmpeg.avcodec.AVPacket;

/**
 * Decides what happens to each packet on its way to an endpoint: send it, or throw it away to
 * buy back latency. A policy that hides its drops charges them to {@code shiftMs}, so the
 * endpoint sees an unbroken timeline instead of a hole it would buffer through.
 */
public interface EndpointMuxerPacingPolicy {

	enum Action { PASS, DISCARD }

	record PacingDecision(Action action, long shiftMs) {

		private static final PacingDecision DISCARD = new PacingDecision(Action.DISCARD, 0);
		private static final PacingDecision PASS = new PacingDecision(Action.PASS, 0);

		public static PacingDecision discard() {
			return DISCARD;
		}

		/** Cached at shiftMs 0, so a policy that never shifts allocates nothing per packet. */
		public static PacingDecision pass(long shiftMs) {
			return shiftMs == 0 ? PASS : new PacingDecision(Action.PASS, shiftMs);
		}
	}

	/** Packets this policy buffers before a full queue starts a drop cycle. */
	int queueCapacity();

	/**
	 * Producer thread, under the EndpointMuxer monitor, so policy fields need no synchronization.
	 * Must not touch {@code pkt}, it is the muxer's tmpPacket. The engine shifts its own clone.
	 */
	PacingDecision onPacket(AVPacket pkt, EndpointMuxerPacingEngine engine);
}
