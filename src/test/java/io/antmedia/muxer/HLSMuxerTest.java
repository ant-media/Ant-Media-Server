package io.antmedia.muxer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.bytedeco.ffmpeg.global.avutil.AVMEDIA_TYPE_DATA;
import static org.bytedeco.ffmpeg.global.avutil.AVMEDIA_TYPE_SUBTITLE;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import io.antmedia.storage.StorageClient;
import io.antmedia.test.UnitTestBase;

@Tag("fast")
class HLSMuxerTest extends UnitTestBase<HLSMuxer> {

	@TempDir
	Path tempDirectory;

	@BeforeEach
	void setUp() {
		classUnderTest = new HLSMuxer(null, null, "streams", 0, null, false);
	}

	@Test
	void shouldOnlyFinalizeFilesModifiedBeforeCleanupWasScheduled() throws Exception {
		HLSMuxer hlsMuxer = new HLSMuxer(null, mock(StorageClient.class), "streams", 0, null, false);
		FileTime cleanupCutoff = FileTime.from(Instant.parse("2026-08-21T12:00:00Z"));
		Path oldManifest = createFileWithModificationTime("old.m3u8", cleanupCutoff.toInstant().minusSeconds(1));
		Path newManifest = createFileWithModificationTime("new.m3u8", cleanupCutoff.toInstant().plusSeconds(1));
		Path sameTimeManifest = createFileWithModificationTime("same-time.m3u8", cleanupCutoff.toInstant());

		hlsMuxer.handleFinalization(oldManifest.toFile(), cleanupCutoff);
		hlsMuxer.handleFinalization(newManifest.toFile(), cleanupCutoff);
		hlsMuxer.handleFinalization(sameTimeManifest.toFile(), cleanupCutoff);

		assertThat(oldManifest).doesNotExist();
		assertThat(newManifest).exists();
		assertThat(sameTimeManifest).exists();
	}

	private Path createFileWithModificationTime(String fileName, Instant modificationTime) throws Exception {
		Path file = Files.createFile(tempDirectory.resolve(fileName));
		Files.setLastModifiedTime(file, FileTime.from(modificationTime));
		return file;
	}

	@Test
	void testCreateMultiTrackWebVttMasterPlaylistContent() {
		WebVttTrack german = new WebVttTrack(2, "deu\n", "DVB \"TTML\"");
		WebVttTrack french = new WebVttTrack(3, "fra", "Hard of hearing");

		String playlist = HLSMuxer.createWebVttMasterPlaylistContent(List.of(french, german), "test", "test.m3u8");

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

		String combined = HLSMuxer.addWebVttToMasterPlaylistContent(
				List.of(new WebVttTrack(4, "eng", "English captions"),
						new WebVttTrack(5, "fra", "French captions")), "test", audioMaster);

		assertThat(combined)
				.contains("TYPE=AUDIO", "URI=\"test_audio_0.m3u8\"", "URI=\"test_fra.m3u8\"")
				.contains("TYPE=SUBTITLES", "URI=\"test_subtitles_4.m3u8\"",
						"URI=\"test_subtitles_5.m3u8\"", "GROUP-ID=\"other\"", "URI=\"external.m3u8\"")
				.contains("AUDIO=\"group_audio\"", "SUBTITLES=\"subs\"")
				.endsWith("test_0.m3u8\n")
				.doesNotContain("\ntest.m3u8\n", "URI=\"old.m3u8\"");
		assertThat(HLSMuxer.getPrimaryVariantUri(combined)).isEqualTo("test_0.m3u8");
		assertThatIllegalArgumentException()
				.isThrownBy(() -> HLSMuxer.getPrimaryVariantUri("#EXTM3U\n#EXT-X-VERSION:3\n"));
	}

	@Test
	void testDropsOriginalDataPacketForConvertedTrack() {
		try (AVPacket packet = new AVPacket()) {
			packet.stream_index(2);
			assertThat(classUnderTest.checkToDropPacket(packet, AVMEDIA_TYPE_DATA)).isFalse();

			classUnderTest.setWebVttTracks(List.of(
					new WebVttTrack(2, "de", "German"), new WebVttTrack(3, "fr", "French")));

			assertThat(classUnderTest.checkToDropPacket(packet, AVMEDIA_TYPE_DATA)).isTrue();
			assertThat(classUnderTest.checkToDropPacket(packet, AVMEDIA_TYPE_SUBTITLE)).isFalse();

			packet.stream_index(3);
			assertThat(classUnderTest.checkToDropPacket(packet, AVMEDIA_TYPE_DATA)).isTrue();

			packet.stream_index(4);
			assertThat(classUnderTest.checkToDropPacket(packet, AVMEDIA_TYPE_DATA)).isFalse();
		}
	}

	@Test
	void testBuildVariantStreamMap() {
		assertThat(classUnderTest.buildVariantStreamMap(1, 2)).isEqualTo(
				"v:0,agroup:audio a:0,agroup:audio,name:audio_0,language:und,default:yes a:1,agroup:audio,name:audio_1,language:und");
		assertThat(classUnderTest.buildVariantStreamMap(0,
				List.of(Optional.of("eng"), Optional.empty()))).isEqualTo(
						"a:0,agroup:audio,name:eng,language:eng,default:yes a:1,agroup:audio,name:audio_1,language:und");
	}

	@Test
	void testInsertVariantSpecifierBeforeExtension() {
		assertThat(classUnderTest.insertVariantSpecifierBeforeExtension("stream.m3u8"))
				.isEqualTo("stream_%v.m3u8");
		assertThat(classUnderTest.insertVariantSpecifierBeforeExtension("stream_%v.m3u8"))
				.isEqualTo("stream_%v.m3u8");
		assertThat(classUnderTest.insertVariantSpecifierBeforeExtension("stream.ts"))
				.isEqualTo("stream_%v.ts");
		assertThat(classUnderTest.insertVariantSpecifierBeforeExtension("stream"))
				.isEqualTo("stream_%v");
	}

	@Test
	void testGetVariantSegmentFilename() {
		ReflectionTestUtils.setField(classUnderTest, "segmentFileNameSuffix", "%09d");
		ReflectionTestUtils.setField(classUnderTest, "segmentFilename", "streams/stream%09d.ts");
		assertThat(classUnderTest.getVariantSegmentFilename()).isEqualTo("streams/stream_%v%09d.ts");

		ReflectionTestUtils.setField(classUnderTest, "segmentFilename", "streams/stream.ts");
		assertThat(classUnderTest.getVariantSegmentFilename()).isEqualTo("streams/stream_%v.ts");

		ReflectionTestUtils.setField(classUnderTest, "segmentFilename", "streams/stream_%v%09d.ts");
		assertThat(classUnderTest.getVariantSegmentFilename()).isEqualTo("streams/stream_%v%09d.ts");
	}

	@Test
	void testVariantHlsFilePattern() {
		String segmentFilename = "streams/stream_%v%09d.ts";
		ReflectionTestUtils.setField(classUnderTest, "segmentFilename", segmentFilename);
		ReflectionTestUtils.setField(classUnderTest, "variantStreamMappingEnabled", true);

		String pattern = classUnderTest.getHLSFilesRegularExpression(segmentFilename.indexOf("%09d"));

		assertThat("stream_0.m3u8").matches(pattern);
		assertThat("stream_audio_0.m3u8").matches(pattern);
		assertThat("stream_audio_000000001.vtt").matches(pattern);
		assertThat("other_0.m3u8").doesNotMatch(pattern);
	}
}
