package io.antmedia.muxer;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.antmedia.test.UnitTestBase;

@Tag("fast")
class WebVttMasterPlaylistSynchronizerTest extends UnitTestBase<WebVttMasterPlaylistSynchronizer> {

	private static final String FFMPEG_MASTER = "#EXTM3U\n#EXT-X-VERSION:3\n"
			+ "#EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID=\"group_audio\",NAME=\"English\",DEFAULT=YES,"
			+ "LANGUAGE=\"eng\",URI=\"test_audio_0.m3u8\"\n"
			+ "#EXT-X-STREAM-INF:BANDWIDTH=1000000,AUDIO=\"group_audio\"\n"
			+ "test_0.m3u8\n";

	@TempDir
	Path tempDirectory;

	@Test
	void testSynchronizeAfterDelayedCreationAndFfmpegRewrite() throws Exception {
		Path master = tempDirectory.resolve("test.m3u8");
		classUnderTest = new WebVttMasterPlaylistSynchronizer(master.toFile(),
				List.of(new WebVttTrack(2, "eng", "English captions")), "test");

		assertThat(classUnderTest.synchronize(false)).isEmpty();

		Files.writeString(master, FFMPEG_MASTER, StandardCharsets.UTF_8);
		assertThat(classUnderTest.synchronize(false)).contains("test_0.m3u8");
		assertThat(Files.readString(master)).contains("TYPE=AUDIO", "TYPE=SUBTITLES", "SUBTITLES=\"subs\"");

		FileTime mergedModificationTime = Files.getLastModifiedTime(master);
		assertThat(classUnderTest.synchronize(false)).contains("test_0.m3u8");
		assertThat(Files.getLastModifiedTime(master)).isEqualTo(mergedModificationTime);

		Files.writeString(master, FFMPEG_MASTER, StandardCharsets.UTF_8);
		Files.setLastModifiedTime(master, FileTime.fromMillis(mergedModificationTime.toMillis() + 1_000));
		assertThat(classUnderTest.synchronize(false)).contains("test_0.m3u8");
		assertThat(Files.readString(master)).contains("TYPE=SUBTITLES", "SUBTITLES=\"subs\"");
	}
}
