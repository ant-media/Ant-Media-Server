package io.antmedia.muxer;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.Optional;

/**
 * Adds AMS-generated WebVTT renditions after FFmpeg creates or rewrites an HLS master playlist.
 */
class WebVttMasterPlaylistSynchronizer {

	private final File masterPlaylist;
	private final Collection<WebVttTrack> tracks;
	private final String baseName;
	private long lastModified = -1;
	private long lastSize = -1;
	private String mediaPlaylistName;

	WebVttMasterPlaylistSynchronizer(File masterPlaylist, Collection<WebVttTrack> tracks, String baseName) {
		this.masterPlaylist = masterPlaylist;
		this.tracks = tracks;
		this.baseName = baseName;
	}

	Optional<String> synchronize(boolean force) throws IOException {
		if (!masterPlaylist.isFile()) {
			return Optional.empty();
		}
		long modified = masterPlaylist.lastModified();
		long size = masterPlaylist.length();
		if (!force && modified == lastModified && size == lastSize) {
			return Optional.ofNullable(mediaPlaylistName);
		}

		// Remember failures as well. FFmpeg's next completed rewrite changes these values and retries the merge,
		// while a permanently malformed file does not cause one warning for every video packet.
		lastModified = modified;
		lastSize = size;
		String source = Files.readString(masterPlaylist.toPath(), StandardCharsets.UTF_8);
		mediaPlaylistName = HLSMuxer.getPrimaryVariantUri(source);
		String merged = HLSMuxer.addWebVttToMasterPlaylistContent(tracks, baseName, source);
		if (!merged.equals(source)) {
			File temporaryFile = new File(masterPlaylist.getParentFile(), masterPlaylist.getName() + ".tmp");
			Files.writeString(temporaryFile.toPath(), merged, StandardCharsets.UTF_8);
			Files.move(temporaryFile.toPath(), masterPlaylist.toPath(), StandardCopyOption.REPLACE_EXISTING,
					StandardCopyOption.ATOMIC_MOVE);
			lastModified = masterPlaylist.lastModified();
			lastSize = masterPlaylist.length();
		}
		return Optional.of(mediaPlaylistName);
	}
}
