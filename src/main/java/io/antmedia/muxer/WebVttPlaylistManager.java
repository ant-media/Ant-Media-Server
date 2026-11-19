package io.antmedia.muxer;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.lindstrom.m3u8.model.AlternativeRendition;
import io.lindstrom.m3u8.model.MasterPlaylist;
import io.lindstrom.m3u8.model.MediaType;
import io.lindstrom.m3u8.model.Variant;
import io.lindstrom.m3u8.parser.MasterPlaylistParser;

/** Owns WebVTT playlists and keeps them aligned with FFmpeg's HLS output. */
class WebVttPlaylistManager {

	private static final Logger logger = LoggerFactory.getLogger(WebVttPlaylistManager.class);
	private static final String SUBTITLE_GROUP = "subs";
	private File file;
	private String initialResourceNameWithoutExtension;
	private String streamId;
	private final Map<Integer, WebVttTrack> webVttTracks = new ConcurrentHashMap<>();
	private final Map<Integer, WebVttHlsPlaylist> webVttPlaylists = new ConcurrentHashMap<>();
	private File webVttMasterPlaylist;
	private File webVttMediaPlaylist;
	private WebVttMasterPlaylistSynchronizer webVttMasterPlaylistSynchronizer;
	private long lastMediaPlaylistModified = -1;
	private long lastMediaPlaylistSize = -1;

	void configureOutput(File file, String baseName, String streamId) {
		this.file = file;
		this.initialResourceNameWithoutExtension = baseName;
		this.streamId = streamId;
	}

	void initializePlaylists() {
		if (hasTracks()) {
			webVttMasterPlaylist = new File(file.getParentFile(), initialResourceNameWithoutExtension + "_master.m3u8");
			webVttMediaPlaylist = file;
			webVttTracks.values().forEach(this::createWebVttPlaylist);
		}
	}

	boolean hasTracks() {
		return !webVttTracks.isEmpty();
	}

	boolean containsTrack(int inputStreamIndex) {
		return webVttTracks.containsKey(inputStreamIndex);
	}

	void attachToVariantMasterPlaylist() {
		webVttMasterPlaylist = file;
		webVttMediaPlaylist = null;
		webVttMasterPlaylistSynchronizer = new WebVttMasterPlaylistSynchronizer(file,
				webVttTracks.values(), initialResourceNameWithoutExtension);
		syncWebVttMasterPlaylist(false);
	}

	/** Updates playlists only when their source files have changed. */
	void synchronizeChangedPlaylists() {
		syncWebVttMasterPlaylist(false);
		syncWebVttPlaylists(false);
	}

	/** Re-reads all source playlists after the muxer writes its trailer, including ENDLIST. */
	void synchronizeFinalPlaylists() {
		syncWebVttMasterPlaylist(true);
		syncWebVttPlaylists(true);
	}

	void setWebVttTracks(Collection<WebVttTrack> tracks) {
		webVttTracks.clear();
		tracks.forEach(this::addWebVttTrack);
	}

	void addWebVttTrack(WebVttTrack track) {
		webVttTracks.put(track.inputStreamIndex(), track);
		if (file != null) {
			createWebVttPlaylist(track);
		}
	}

	void writeWebVttCue(int inputStreamIndex, WebVttCue cue) {
		WebVttHlsPlaylist playlist = webVttPlaylists.get(inputStreamIndex);
		if (playlist == null) {
			return;
		}
		try {
			playlist.addCue(cue);
		}
		catch (IOException e) {
			logger.error("Cannot write WebVTT cue for stream:{} inputIndex:{}", streamId, inputStreamIndex, e);
		}
	}

	void writeWebVttMasterPlaylist() {
		String content = createWebVttMasterPlaylistContent(webVttTracks.values(), initialResourceNameWithoutExtension,
				file.getName());
		try {
			File temporaryFile = new File(webVttMasterPlaylist.getParentFile(), webVttMasterPlaylist.getName() + ".tmp");
			Files.writeString(temporaryFile.toPath(), content, StandardCharsets.UTF_8);
			Files.move(temporaryFile.toPath(), webVttMasterPlaylist.toPath(), StandardCopyOption.REPLACE_EXISTING,
					StandardCopyOption.ATOMIC_MOVE);
			logger.info("WebVTT master playlist is written for stream:{} path:{}", streamId, webVttMasterPlaylist);
		}
		catch (IOException | RuntimeException e) {
			logger.error("Cannot write WebVTT master playlist for stream:{}", streamId, e);
		}
	}

	private void syncWebVttMasterPlaylist(boolean force) {
		if (webVttMasterPlaylistSynchronizer == null) {
			return;
		}
		try {
			Optional<String> mediaPlaylistName = force ? webVttMasterPlaylistSynchronizer.synchronizeRegardlessOfFileChanges()
					: webVttMasterPlaylistSynchronizer.synchronizeIfChanged();
			if (mediaPlaylistName.isPresent()) {
				webVttMediaPlaylist = new File(file.getParentFile(), mediaPlaylistName.get());
				File obsoleteStandaloneMaster = new File(file.getParentFile(),
						initialResourceNameWithoutExtension + "_master.m3u8");
				Files.deleteIfExists(obsoleteStandaloneMaster.toPath());
			}
		}
		catch (IOException | RuntimeException e) {
			logger.warn("Cannot synchronize WebVTT master playlist for stream:{} path:{}",
					streamId, webVttMasterPlaylist, e);
		}
	}

	static String createWebVttMasterPlaylistContent(Collection<WebVttTrack> tracks, String baseName,
			String mediaPlaylistName) {
		MasterPlaylist.Builder playlist = MasterPlaylist.builder().version(3);
		playlist.alternativeRenditions(createSubtitleRenditions(tracks, baseName));
		playlist.addVariants(Variant.builder().bandwidth(1_000_000).subtitles(SUBTITLE_GROUP)
				.uri(mediaPlaylistName).build());
		return new MasterPlaylistParser().writePlaylistAsString(playlist.build());
	}

	static String addWebVttToMasterPlaylistContent(Collection<WebVttTrack> tracks, String baseName,
			String masterPlaylistContent) throws IOException {
		MasterPlaylistParser parser = new MasterPlaylistParser();
		MasterPlaylist source = parser.readPlaylist(masterPlaylistContent);
		List<AlternativeRendition> renditions = new ArrayList<>(source.alternativeRenditions());
		renditions.removeIf(rendition -> rendition.type() == MediaType.SUBTITLES
				&& SUBTITLE_GROUP.equals(rendition.groupId()));
		renditions.addAll(createSubtitleRenditions(tracks, baseName));
		List<Variant> variants = source.variants().stream()
				.map(variant -> Variant.builder().from(variant).subtitles(SUBTITLE_GROUP).build())
				.toList();
		MasterPlaylist merged = MasterPlaylist.builder().from(source)
				.alternativeRenditions(renditions)
				.variants(variants)
				.build();
		return parser.writePlaylistAsString(merged);
	}

	private static List<AlternativeRendition> createSubtitleRenditions(Collection<WebVttTrack> tracks,
			String baseName) {
		List<AlternativeRendition> renditions = new ArrayList<>();
		List<WebVttTrack> sortedTracks = tracks.stream()
				.sorted(Comparator.comparingInt(WebVttTrack::inputStreamIndex)).toList();
		List<String> preferredNames = sortedTracks.stream().map(WebVttPlaylistManager::subtitleName).toList();
		Set<String> reservedNames = new HashSet<>(preferredNames);
		Set<String> usedNames = new HashSet<>();
		int trackNumber = 0;
		for (WebVttTrack track : sortedTracks) {
			String preferredName = preferredNames.get(trackNumber);
			String name = preferredName;
			int suffix = 2;
			// HLS rendition names must be unique within the group. Reserve metadata
			// labels so an auto-generated suffix cannot take another track's name.
			while (usedNames.contains(name) || (!name.equals(preferredName) && reservedNames.contains(name))) {
				name = preferredName + " (" + suffix++ + ")";
			}
			usedNames.add(name);
			renditions.add(AlternativeRendition.builder()
					.type(MediaType.SUBTITLES)
					.groupId(SUBTITLE_GROUP)
					.name(name)
					.language(sanitizePlaylistAttribute(track.language()))
					.defaultRendition(trackNumber++ == 0)
					.autoSelect(true)
					.uri(WebVttHlsPlaylist.playlistName(track, baseName))
					.build());
		}
		return renditions;
	}

	private static String subtitleName(WebVttTrack track) {
		String name = sanitizePlaylistAttribute(track.name()).strip();
		if (!"DVB-TTML".equalsIgnoreCase(name) && !"Subtitles".equalsIgnoreCase(name)) {
			return name;
		}
		String language = track.language().strip();
		if (!"und".equalsIgnoreCase(language)) {
			String languageName = Locale.forLanguageTag(language).getDisplayLanguage(Locale.ENGLISH);
			if (!languageName.isBlank()) {
				return sanitizePlaylistAttribute(languageName);
			}
		}
		return "Subtitles";
	}

	static String getPrimaryVariantUri(String masterPlaylistContent) throws IOException {
		List<Variant> variants = new MasterPlaylistParser().readPlaylist(masterPlaylistContent).variants();
		if (variants.isEmpty()) {
			throw new IllegalArgumentException("HLS master playlist does not contain a media variant");
		}
		return variants.get(0).uri();
	}

	private static String sanitizePlaylistAttribute(String value) {
		return value.replace('"', '\'').replace('\r', ' ').replace('\n', ' ').replace('\t', ' ');
	}

	private void createWebVttPlaylist(WebVttTrack track) {
		webVttPlaylists.computeIfAbsent(track.inputStreamIndex(), ignored ->
				new WebVttHlsPlaylist(track, file.getParentFile(), initialResourceNameWithoutExtension));
	}

	private void syncWebVttPlaylists(boolean force) {
		if (webVttMediaPlaylist == null || !webVttMediaPlaylist.isFile()) {
			return;
		}
		long modified = webVttMediaPlaylist.lastModified();
		long size = webVttMediaPlaylist.length();
		if (!force && modified == lastMediaPlaylistModified && size == lastMediaPlaylistSize) {
			return;
		}
		lastMediaPlaylistModified = modified;
		lastMediaPlaylistSize = size;
		try {
			String mediaPlaylistContent = Files.readString(webVttMediaPlaylist.toPath(), StandardCharsets.UTF_8);
			for (WebVttHlsPlaylist playlist : webVttPlaylists.values()) {
				playlist.update(mediaPlaylistContent);
			}
		}
		catch (IOException | RuntimeException e) {
			logger.warn("Cannot synchronize WebVTT playlists for stream:{} mediaPlaylist:{}",
					streamId, webVttMediaPlaylist, e);
		}
	}
}
