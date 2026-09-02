package io.antmedia.muxer;

/**
 * A WebVTT cue on the media timeline.
 *
 * @param startTimeMs cue start in milliseconds
 * @param endTimeMs cue end in milliseconds
 * @param text cue payload
 */
public record WebVttCue(long startTimeMs, long endTimeMs, String text) {

	public WebVttCue {
		if (startTimeMs < 0) {
			throw new IllegalArgumentException("startTimeMs cannot be negative");
		}
		if (endTimeMs <= startTimeMs) {
			throw new IllegalArgumentException("endTimeMs must be greater than startTimeMs");
		}
		if (text == null || text.isBlank()) {
			throw new IllegalArgumentException("text cannot be blank");
		}
	}
}
