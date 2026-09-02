package io.antmedia.muxer;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import io.lindstrom.m3u8.model.MediaSegment;
import io.lindstrom.m3u8.parser.MediaPlaylistParser;
import io.lindstrom.m3u8.parser.PlaylistParserException;

/**
 * Writes one WebVTT HLS rendition by mirroring the segment boundaries of the
 * primary media playlist.
 */
class WebVttHlsPlaylist {

	private final WebVttTrack track;
	private final File directory;
	private final String baseName;
	private final File playlistFile;
	private final List<WebVttCue> cues = new ArrayList<>();
	private final Map<Long, Segment> knownSegments = new HashMap<>();
	private final Set<Long> writtenSequences = new HashSet<>();
	private TimelinePlaylist mediaPlaylist;

	WebVttHlsPlaylist(WebVttTrack track, File directory, String baseName) {
		this.track = track;
		this.directory = directory;
		this.baseName = baseName;
		playlistFile = new File(directory, playlistName(track, baseName));
	}

	WebVttTrack track() {
		return track;
	}

	String playlistName() {
		return playlistFile.getName();
	}

	synchronized void addCue(WebVttCue cue) throws IOException {
		cues.add(cue);
		if (mediaPlaylist != null) {
			writeFiles();
		}
	}

	synchronized void update(String mediaPlaylistContent) throws IOException {
		TimelinePlaylist parsed;
		try {
			parsed = parseMediaPlaylist(mediaPlaylistContent);
		}
		catch (PlaylistParserException e) {
			throw new IOException("Cannot parse media playlist", e);
		}
		if (parsed.segments().isEmpty()) {
			return;
		}
		mediaPlaylist = addTimeline(parsed);
		writeFiles();
	}

	private TimelinePlaylist addTimeline(TimelinePlaylist parsed) {
		List<Segment> segments = new ArrayList<>();
		long startMs = findStartTime(parsed);
		for (Segment segment : parsed.segments()) {
			Segment timedSegment = new Segment(segment.sequence(), startMs, segment.durationMs());
			knownSegments.put(timedSegment.sequence(), timedSegment);
			segments.add(timedSegment);
			startMs += timedSegment.durationMs();
		}
		return new TimelinePlaylist(parsed.version(), parsed.targetDuration(), parsed.mediaSequence(), segments,
				parsed.endList());
	}

	private long findStartTime(TimelinePlaylist parsed) {
		Segment first = knownSegments.get(parsed.mediaSequence());
		if (first != null) {
			return first.startMs();
		}
		Segment previous = knownSegments.get(parsed.mediaSequence() - 1);
		if (previous != null) {
			return previous.startMs() + previous.durationMs();
		}
		return knownSegments.isEmpty() ? 0 : knownSegments.values().stream()
				.max((left, right) -> Long.compare(left.sequence(), right.sequence()))
				.map(segment -> segment.startMs() + segment.durationMs()).orElse(0L);
	}

	private void writeFiles() throws IOException {
		for (Segment segment : mediaPlaylist.segments()) {
			writeAtomically(new File(directory, segmentName(segment.sequence())), createSegmentContent(segment));
			writtenSequences.add(segment.sequence());
		}
		writeAtomically(playlistFile, createPlaylistContent());
		removeExpiredFiles();
		long earliestCueTime = mediaPlaylist.segments().get(0).startMs();
		cues.removeIf(cue -> cue.endTimeMs() < earliestCueTime);
	}

	private String createSegmentContent(Segment segment) {
		StringBuilder content = new StringBuilder("WEBVTT\n");
		long segmentEnd = segment.startMs() + segment.durationMs();
		for (WebVttCue cue : cues) {
			if (cue.startTimeMs() >= segment.startMs() && cue.startTimeMs() < segmentEnd) {
				content.append('\n').append(formatTimestamp(cue.startTimeMs())).append(" --> ")
						.append(formatTimestamp(cue.endTimeMs())).append('\n').append(cue.text()).append('\n');
			}
		}
		return content.toString();
	}

	private String createPlaylistContent() {
		io.lindstrom.m3u8.model.MediaPlaylist.Builder builder = io.lindstrom.m3u8.model.MediaPlaylist.builder()
				.version(mediaPlaylist.version())
				.targetDuration(mediaPlaylist.targetDuration())
				.mediaSequence(mediaPlaylist.mediaSequence())
				.ongoing(!mediaPlaylist.endList());
		for (Segment segment : mediaPlaylist.segments()) {
			builder.addMediaSegments(MediaSegment.builder()
					.duration(segment.durationMs() / 1000D)
					.uri(segmentName(segment.sequence()))
					.build());
		}
		return new MediaPlaylistParser().writePlaylistAsString(builder.build());
	}

	private void removeExpiredFiles() throws IOException {
		long firstSequenceToKeep = Math.max(0, mediaPlaylist.mediaSequence() - 1);
		for (Long sequence : new HashSet<>(writtenSequences)) {
			if (sequence < firstSequenceToKeep) {
				Files.deleteIfExists(new File(directory, segmentName(sequence)).toPath());
				writtenSequences.remove(sequence);
				knownSegments.remove(sequence);
			}
		}
	}

	private String segmentName(long sequence) {
		return baseName + "_subtitles_" + track.inputStreamIndex() + "_" + sequence + ".vtt";
	}

	static String playlistName(WebVttTrack track, String baseName) {
		return baseName + "_subtitles_" + track.inputStreamIndex() + ".m3u8";
	}

	static TimelinePlaylist parseMediaPlaylist(String content) throws PlaylistParserException {
		io.lindstrom.m3u8.model.MediaPlaylist source = new MediaPlaylistParser().readPlaylist(content);
		long sequence = source.mediaSequence();
		List<Segment> segments = new ArrayList<>();
		for (MediaSegment segment : source.mediaSegments()) {
			segments.add(new Segment(sequence++, 0, Math.round(segment.duration() * 1000)));
		}
		return new TimelinePlaylist(source.version().orElse(3), source.targetDuration(), source.mediaSequence(),
				segments, !source.ongoing());
	}

	private static String formatTimestamp(long timestampMs) {
		long hours = timestampMs / 3_600_000;
		long minutes = timestampMs % 3_600_000 / 60_000;
		long seconds = timestampMs % 60_000 / 1_000;
		long milliseconds = timestampMs % 1_000;
		return String.format(Locale.ROOT, "%02d:%02d:%02d.%03d", hours, minutes, seconds, milliseconds);
	}

	private static void writeAtomically(File target, String content) throws IOException {
		File temporary = new File(target.getParentFile(), target.getName() + ".tmp");
		Files.writeString(temporary.toPath(), content, StandardCharsets.UTF_8);
		try {
			Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING,
					StandardCopyOption.ATOMIC_MOVE);
		}
		catch (AtomicMoveNotSupportedException e) {
			Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
		}
	}

	record Segment(long sequence, long startMs, long durationMs) { }

	record TimelinePlaylist(int version, int targetDuration, long mediaSequence, List<Segment> segments,
			boolean endList) { }
}
