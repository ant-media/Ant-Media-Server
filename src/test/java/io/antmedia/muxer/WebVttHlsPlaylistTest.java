package io.antmedia.muxer;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.antmedia.test.UnitTestBase;

@Tag("fast")
class WebVttHlsPlaylistTest extends UnitTestBase<WebVttHlsPlaylist> {

	@TempDir
	Path temporaryDirectory;

	@Test
	void testUnchangedSegmentsAndPlaylistAreNotRewritten() throws Exception {
		classUnderTest = new WebVttHlsPlaylist(new WebVttTrack(3, "fra", "French"),
				temporaryDirectory.toFile(), "test");
		String playlist = playlist(10, false);
		classUnderTest.update(playlist);
		Path first = temporaryDirectory.resolve("test_subtitles_3_10.vtt");
		Path second = temporaryDirectory.resolve("test_subtitles_3_11.vtt");
		Path manifest = temporaryDirectory.resolve("test_subtitles_3.m3u8");
		FileTime marker = FileTime.fromMillis(1_000);
		for (Path file : new Path[] {first, second, manifest}) {
			Files.setLastModifiedTime(file, marker);
		}

		classUnderTest.update(playlist);
		for (Path file : new Path[] {first, second, manifest}) {
			assertThat(Files.getLastModifiedTime(file)).isEqualTo(marker);
		}

		classUnderTest.addCue(new WebVttCue(2200, 3100, "Late cue"));
		assertThat(Files.readString(second)).contains("Late cue");
		assertThat(Files.getLastModifiedTime(first)).isEqualTo(marker);
		assertThat(Files.getLastModifiedTime(manifest)).isEqualTo(marker);
		Files.setLastModifiedTime(second, marker);
		classUnderTest.update(playlist);
		assertThat(Files.getLastModifiedTime(second)).isEqualTo(marker);
	}

	@Test
	void testSlidingPlaylistCreatesNewSegmentsAndRemovesExpiredFiles() throws Exception {
		classUnderTest = new WebVttHlsPlaylist(new WebVttTrack(3, "fra", "French"),
				temporaryDirectory.toFile(), "test");
		classUnderTest.update(playlist(10, false));
		classUnderTest.addCue(new WebVttCue(4200, 5100, "Future cue"));
		classUnderTest.update(playlist(11, false));
		Path overlap = temporaryDirectory.resolve("test_subtitles_3_12.vtt");
		assertThat(Files.readString(overlap)).contains("Future cue");
		FileTime marker = FileTime.fromMillis(1_000);
		Files.setLastModifiedTime(overlap, marker);
		classUnderTest.update(playlist(12, true));
		assertThat(Files.getLastModifiedTime(overlap)).isEqualTo(marker);
		assertThat(temporaryDirectory.resolve("test_subtitles_3_10.vtt")).doesNotExist();
		assertThat(temporaryDirectory.resolve("test_subtitles_3_11.vtt")).exists();
		assertThat(temporaryDirectory.resolve("test_subtitles_3_13.vtt")).exists();
		assertThat(Files.readString(temporaryDirectory.resolve("test_subtitles_3.m3u8")))
				.contains("#EXT-X-MEDIA-SEQUENCE:12", "#EXT-X-ENDLIST");
	}

	private String playlist(long sequence, boolean endList) {
		return """
				#EXTM3U
				#EXT-X-VERSION:3
				#EXT-X-TARGETDURATION:2
				#EXT-X-MEDIA-SEQUENCE:%d
				#EXTINF:2.000000,
				test%d.ts
				#EXTINF:2.000000,
				test%d.ts
				""".formatted(sequence, sequence, sequence + 1) + (endList ? "#EXT-X-ENDLIST\n" : "");
	}

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
