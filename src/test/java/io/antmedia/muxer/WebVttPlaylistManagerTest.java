package io.antmedia.muxer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.antmedia.test.UnitTestBase;
import io.lindstrom.m3u8.model.AlternativeRendition;
import io.lindstrom.m3u8.parser.MasterPlaylistParser;

@Tag("fast")
class WebVttPlaylistManagerTest extends UnitTestBase<WebVttPlaylistManager> {

	@TempDir
	Path tempDirectory;

	@BeforeEach
	void setUp() {
		classUnderTest = new WebVttPlaylistManager();
	}

	@Test
	void testCreateMultiTrackWebVttMasterPlaylistContent() {
		WebVttTrack german = new WebVttTrack(2, "deu\n", "DVB \"TTML\"");
		WebVttTrack french = new WebVttTrack(3, "fra", "Hard of hearing");

		String playlist = WebVttPlaylistManager.createWebVttMasterPlaylistContent(List.of(french, german), "test", "test.m3u8");

		assertThat(playlist)
				.startsWith("#EXTM3U\n")
				.contains("LANGUAGE=\"deu \"", "NAME=\"DVB 'TTML'\"", "DEFAULT=YES")
				.contains("URI=\"test_subtitles_2.m3u8\"")
				.contains("LANGUAGE=\"fra\"", "NAME=\"Hard of hearing\"", "DEFAULT=NO")
				.contains("URI=\"test_subtitles_3.m3u8\"")
				.containsOnlyOnce("DEFAULT=YES")
				.endsWith("test.m3u8\n");
	}

	@Test
	void testSubtitleNamesPreferMetadataLabels() throws IOException {
		String playlist = WebVttPlaylistManager.createWebVttMasterPlaylistContent(List.of(
				new WebVttTrack(3, "ara", "العربية"),
				new WebVttTrack(4, "rus", "Russian captions")), "test", "test.m3u8");
		assertThat(new MasterPlaylistParser().readPlaylist(playlist).alternativeRenditions())
				.extracting(AlternativeRendition::name).containsExactly("العربية", "Russian captions");
	}

	@Test
	void testGenericSubtitleNamesUseLanguageMetadata() throws IOException {
		String playlist = WebVttPlaylistManager.createWebVttMasterPlaylistContent(List.of(
				new WebVttTrack(4, "eng", "DVB-TTML"),
				new WebVttTrack(5, "ara", "DVB-TTML"),
				new WebVttTrack(6, "rus", "DVB-TTML")), "syncwords", "syncwords_0.m3u8");
		var renditions = new MasterPlaylistParser().readPlaylist(playlist).alternativeRenditions();
		assertThat(renditions).extracting(AlternativeRendition::name).containsExactly("English", "Arabic", "Russian");
		assertThat(playlist).contains("LANGUAGE=\"eng\"", "LANGUAGE=\"ara\"", "LANGUAGE=\"rus\"")
				.contains("syncwords_subtitles_4.m3u8", "syncwords_subtitles_5.m3u8", "syncwords_subtitles_6.m3u8")
				.containsOnlyOnce("DEFAULT=YES").doesNotContain("DVB-TTML");
	}

	@Test
	void testDuplicateSubtitleNamesDoNotTakeOtherMetadataLabels() throws IOException {
		List<WebVttTrack> tracks = List.of(new WebVttTrack(5, "eng", "English (2)"),
				new WebVttTrack(4, "eng", "DVB-TTML"), new WebVttTrack(3, "eng", "DVB-TTML"));
		String playlist = WebVttPlaylistManager.createWebVttMasterPlaylistContent(tracks, "test", "test.m3u8");
		assertThat(new MasterPlaylistParser().readPlaylist(playlist).alternativeRenditions())
				.extracting(AlternativeRendition::name).containsExactly("English", "English (3)", "English (2)");
		assertThat(WebVttPlaylistManager.createWebVttMasterPlaylistContent(List.of(tracks.get(2), tracks.get(0), tracks.get(1)),
				"test", "test.m3u8")).isEqualTo(playlist);
	}

	@Test
	void testSubtitleNamesAreUniqueAfterSanitizingMetadata() throws IOException {
		String playlist = WebVttPlaylistManager.createWebVttMasterPlaylistContent(List.of(
				new WebVttTrack(3, "eng", "News \"captions\""),
				new WebVttTrack(4, "eng", "News 'captions'")), "test", "test.m3u8");
		assertThat(new MasterPlaylistParser().readPlaylist(playlist).alternativeRenditions())
				.extracting(AlternativeRendition::name).containsExactly("News 'captions'", "News 'captions' (2)");
	}

	@Test
	void testMissingSubtitleMetadataFallsBackToUniqueNames() throws IOException {
		String playlist = WebVttPlaylistManager.createWebVttMasterPlaylistContent(List.of(
				new WebVttTrack(3, null, null), new WebVttTrack(4, "und", "DVB-TTML"),
				new WebVttTrack(5, "ara", null)), "test", "test.m3u8");
		assertThat(new MasterPlaylistParser().readPlaylist(playlist).alternativeRenditions())
				.extracting(AlternativeRendition::name).containsExactly("Subtitles", "Subtitles (2)", "Arabic");
	}

	@Test
	void testInvalidLanguageMetadataFallsBackToUniqueNames() throws IOException {
		String playlist = WebVttPlaylistManager.createWebVttMasterPlaylistContent(List.of(
				new WebVttTrack(3, "_", "DVB-TTML"),
				new WebVttTrack(4, "_", "Subtitles")), "test", "test.m3u8");
		assertThat(new MasterPlaylistParser().readPlaylist(playlist).alternativeRenditions())
				.extracting(AlternativeRendition::name).containsExactly("Subtitles", "Subtitles (2)");
	}

	@Test
	void testSubtitleLabelsNormalizeWhitespaceAndGenericNameCase() throws IOException {
		String playlist = WebVttPlaylistManager.createWebVttMasterPlaylistContent(List.of(
				new WebVttTrack(3, " eng ", " dvb-ttml "),
				new WebVttTrack(4, "ara", " SUBTITLES "),
				new WebVttTrack(5, " UND ", "\t\n"),
				new WebVttTrack(6, "rus", " Russian captions ")), "test", "test.m3u8");
		assertThat(new MasterPlaylistParser().readPlaylist(playlist).alternativeRenditions())
				.extracting(AlternativeRendition::name).containsExactly("English", "Arabic", "Subtitles", "Russian captions");
	}

	@Test
	void testMergedMasterUsesUniqueSubtitleNames() throws IOException {
		String master = "#EXTM3U\n#EXT-X-STREAM-INF:BANDWIDTH=1000000\ntest_0.m3u8\n";
		String playlist = WebVttPlaylistManager.addWebVttToMasterPlaylistContent(List.of(
				new WebVttTrack(3, "eng", "DVB-TTML"), new WebVttTrack(4, "ara", "DVB-TTML")), "test", master);
		assertThat(new MasterPlaylistParser().readPlaylist(playlist).alternativeRenditions())
				.extracting(AlternativeRendition::name).containsExactly("English", "Arabic");
	}

	@Test
	void testAddWebVttTracksToMultiTrackAudioMasterPlaylist() throws IOException {
		String audioMaster = """
				#EXTM3U
				#EXT-X-VERSION:3
				#EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID="group_audio",NAME="English",DEFAULT=YES,LANGUAGE="eng",URI="test_audio_0.m3u8"
				#EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID="group_audio",NAME="French",DEFAULT=NO,LANGUAGE="fra",URI="test_fra.m3u8"
				#EXT-X-MEDIA:TYPE=SUBTITLES,GROUP-ID="subs",NAME="Obsolete",DEFAULT=YES,URI="old.m3u8"
				#EXT-X-MEDIA:TYPE=SUBTITLES,GROUP-ID="other",NAME="External",DEFAULT=NO,URI="external.m3u8"
				#EXT-X-STREAM-INF:BANDWIDTH=1000000,AUDIO="group_audio"
				test_0.m3u8
				""";

		String combined = WebVttPlaylistManager.addWebVttToMasterPlaylistContent(
				List.of(new WebVttTrack(4, "eng", "English captions"),
						new WebVttTrack(5, "fra", "French captions")), "test", audioMaster);

		assertThat(combined)
				.contains("TYPE=AUDIO", "URI=\"test_audio_0.m3u8\"", "URI=\"test_fra.m3u8\"")
				.contains("TYPE=SUBTITLES", "URI=\"test_subtitles_4.m3u8\"",
						"URI=\"test_subtitles_5.m3u8\"", "GROUP-ID=\"other\"", "URI=\"external.m3u8\"")
				.contains("AUDIO=\"group_audio\"", "SUBTITLES=\"subs\"")
				.endsWith("test_0.m3u8\n")
				.doesNotContain("\ntest.m3u8\n", "URI=\"old.m3u8\"");
		assertThat(WebVttPlaylistManager.getPrimaryVariantUri(combined)).isEqualTo("test_0.m3u8");
		assertThatIllegalArgumentException()
				.isThrownBy(() -> WebVttPlaylistManager.getPrimaryVariantUri("#EXTM3U\n#EXT-X-VERSION:3\n"));
	}

	@Test
	void testWebVttPlaylistLifecycle() throws Exception {
		Path mediaPlaylist = tempDirectory.resolve("test.m3u8");
		classUnderTest.synchronizeChangedPlaylists();
		classUnderTest.configureOutput(mediaPlaylist.toFile(), "test", "test");
		classUnderTest.initializePlaylists();

		WebVttTrack track = new WebVttTrack(2, "de", "German");
		classUnderTest.addWebVttTrack(track);
		classUnderTest.initializePlaylists();
		classUnderTest.writeWebVttCue(2, new WebVttCue(500, 1_500, "Hallo"));
		classUnderTest.writeWebVttCue(3, new WebVttCue(500, 1_500, "Ignored"));
		Path masterPlaylist = tempDirectory.resolve("test_master.m3u8");
		classUnderTest.writeWebVttMasterPlaylist();
		assertThat(Files.readString(masterPlaylist))
				.contains("TYPE=SUBTITLES", "URI=\"test_subtitles_2.m3u8\"", "test.m3u8");

		String endList = "#EXT-X-ENDLIST\n";
		String placeholder = "#" + " ".repeat(endList.length() - 2) + "\n";
		Files.writeString(mediaPlaylist, """
				#EXTM3U
				#EXT-X-VERSION:3
				#EXT-X-TARGETDURATION:2
				#EXT-X-MEDIA-SEQUENCE:10
				#EXTINF:2.000000,
				test000000010.ts
				""" + placeholder);

		classUnderTest.synchronizeChangedPlaylists();
		classUnderTest.synchronizeChangedPlaylists();

		assertThat(Files.readString(tempDirectory.resolve("test_subtitles_2.m3u8")))
				.contains("#EXT-X-MEDIA-SEQUENCE:10", "test_subtitles_2_10.vtt");
		assertThat(Files.readString(tempDirectory.resolve("test_subtitles_2_10.vtt")))
				.contains("00:00:00.500 --> 00:00:01.500", "Hallo")
				.doesNotContain("Ignored");

		// Final synchronization must re-read even when size and modification time are unchanged.
		var modified = Files.getLastModifiedTime(mediaPlaylist);
		Files.writeString(mediaPlaylist, Files.readString(mediaPlaylist).replace(placeholder, endList));
		Files.setLastModifiedTime(mediaPlaylist, modified);
		classUnderTest.synchronizeChangedPlaylists();
		assertThat(Files.readString(tempDirectory.resolve("test_subtitles_2.m3u8")))
				.doesNotContain("#EXT-X-ENDLIST");
		classUnderTest.synchronizeFinalPlaylists();
		assertThat(Files.readString(tempDirectory.resolve("test_subtitles_2.m3u8")))
				.contains("#EXT-X-ENDLIST");
	}

	@Test
	void testVariantMasterCreatedAfterHeaderSelectsMediaPlaylist() throws Exception {
		Path master = tempDirectory.resolve("test.m3u8");
		classUnderTest.setWebVttTracks(List.of(new WebVttTrack(2, "eng", "English")));
		classUnderTest.configureOutput(master.toFile(), "test", "test");
		classUnderTest.initializePlaylists();
		classUnderTest.writeWebVttMasterPlaylist();
		classUnderTest.attachToVariantMasterPlaylist();
		classUnderTest.synchronizeChangedPlaylists();

		Files.writeString(master, "#EXTM3U\n#EXT-X-STREAM-INF:BANDWIDTH=1000000\ntest_0.m3u8\n");
		Files.writeString(tempDirectory.resolve("test_0.m3u8"), """
				#EXTM3U
				#EXT-X-TARGETDURATION:2
				#EXTINF:2.0,
				test_0000000000.ts
				#EXT-X-ENDLIST
				""");
		classUnderTest.writeWebVttCue(2, new WebVttCue(500, 1_500, "Hello"));
		classUnderTest.synchronizeFinalPlaylists();

		assertThat(Files.readString(master)).contains("SUBTITLES=\"subs\"", "test_0.m3u8");
		assertThat(tempDirectory.resolve("test_master.m3u8")).doesNotExist();
		assertThat(Files.readString(tempDirectory.resolve("test_subtitles_2.m3u8")))
				.contains("test_subtitles_2_0.vtt", "#EXT-X-ENDLIST");
		assertThat(Files.readString(tempDirectory.resolve("test_subtitles_2_0.vtt"))).contains("Hello");
	}

}
