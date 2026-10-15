package io.antmedia.muxer;

/**
 * Describes a WebVTT subtitle track created from an ingest stream.
 *
 * @param inputStreamIndex source stream index used to route cues
 * @param language BCP 47 language code, or {@code und}
 * @param name human-readable track name
 */
public record WebVttTrack(int inputStreamIndex, String language, String name) {

	public WebVttTrack {
		if (inputStreamIndex < 0) {
			throw new IllegalArgumentException("inputStreamIndex cannot be negative");
		}
		language = language == null || language.isBlank() ? "und" : language;
		name = name == null || name.isBlank() ? "Subtitles" : name;
	}
}
