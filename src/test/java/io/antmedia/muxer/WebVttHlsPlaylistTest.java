package io.antmedia.muxer;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.antmedia.test.UnitTestBase;

@Tag("fast")
class WebVttHlsPlaylistTest extends UnitTestBase<WebVttHlsPlaylist> {

	@TempDir
	Path temporaryDirectory;

	@Test
	void testMirrorMediaSegmentsAndWriteCues() throws Exception {
		classUnderTest = new WebVttHlsPlaylist(new WebVttTrack(3, "fra", "French"),
				temporaryDirectory.toFile(), "test");
		classUnderTest.addCue(new WebVttCue(500, 2500, "Bonjour"));
		classUnderTest.addCue(new WebVttCue(2200, 3100, "Deuxième"));

		classUnderTest.update("""
				#EXTM3U
				#EXT-X-VERSION:3
				#EXT-X-TARGETDURATION:2
				#EXT-X-MEDIA-SEQUENCE:10
				#EXTINF:2.000000,
				test10.ts
				#EXTINF:2.000000,
				test11.ts
				""");

		assertThat(Files.readString(temporaryDirectory.resolve("test_subtitles_3.m3u8")))
				.contains("#EXT-X-MEDIA-SEQUENCE:10", "test_subtitles_3_10.vtt", "test_subtitles_3_11.vtt");
		assertThat(Files.readString(temporaryDirectory.resolve("test_subtitles_3_10.vtt")))
				.contains("00:00:00.500 --> 00:00:02.500", "Bonjour")
				.doesNotContain("Deuxième");
		assertThat(Files.readString(temporaryDirectory.resolve("test_subtitles_3_11.vtt")))
				.contains("00:00:02.200 --> 00:00:03.100", "Deuxième");
	}
}
