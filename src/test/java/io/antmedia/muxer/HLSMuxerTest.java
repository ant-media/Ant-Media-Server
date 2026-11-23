package io.antmedia.muxer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.bytedeco.ffmpeg.global.avutil.AVMEDIA_TYPE_DATA;
import static org.bytedeco.ffmpeg.global.avutil.AVMEDIA_TYPE_SUBTITLE;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import io.antmedia.AppSettings;
import io.antmedia.EncoderSettings;
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

	@Test
	void shouldTakeBandwidthOfAbrRenditionFromItsEncoderSettings() {
		HLSMuxer hlsMuxer = muxerWithSettings(360, Arrays.asList(
				new EncoderSettings(240, 300000, 32000, true),
				new EncoderSettings(360, 800000, 64000, true),
				new EncoderSettings(720, 2000000, 128000, true)));
		doReturn(9000000L).when(hlsMuxer).getAverageBitrate();

		//the video plus audio bitrate of the 360p rendition. The average is ignored.
		assertThat(hlsMuxer.getStableBandwidthBps()).isEqualTo(expectedBandwidth(800000 + 64000));
	}

	@Test
	void shouldRaiseButNeverLowerBandwidthWhenThereIsNoMatchingAbrRendition() {
		HLSMuxer hlsMuxer = muxerWithSettings(0, Collections.singletonList(
				new EncoderSettings(360, 800000, 64000, true)));

		doReturn(1000000L).when(hlsMuxer).getAverageBitrate();
		assertThat(hlsMuxer.getStableBandwidthBps()).isEqualTo(expectedBandwidth(1000000));

		doReturn(100000L).when(hlsMuxer).getAverageBitrate();
		assertThat(hlsMuxer.getStableBandwidthBps()).isEqualTo(expectedBandwidth(1000000));

		doReturn(3000000L).when(hlsMuxer).getAverageBitrate();
		assertThat(hlsMuxer.getStableBandwidthBps()).isEqualTo(expectedBandwidth(3000000));
	}

	@Test
	void shouldNeverReportZeroBandwidth() {
		HLSMuxer hlsMuxer = muxerWithSettings(0, Collections.emptyList());

		doReturn(0L).when(hlsMuxer).getAverageBitrate();
		assertThat(hlsMuxer.getStableBandwidthBps()).isEqualTo(HLSMuxer.BANDWIDTH_STEP);
	}

	private static long expectedBandwidth(long bitrate) {
		long withHeadroom = bitrate + bitrate * HLSMuxer.BANDWIDTH_HEADROOM_PERCENT / 100;
		return (withHeadroom / HLSMuxer.BANDWIDTH_STEP + 1) * HLSMuxer.BANDWIDTH_STEP;
	}

	private HLSMuxer muxerWithSettings(int resolution, List<EncoderSettings> encoderSettings) {
		AppSettings appSettings = new AppSettings();
		appSettings.setEncoderSettings(encoderSettings);

		HLSMuxer hlsMuxer = spy(new HLSMuxer(null, mock(StorageClient.class), "streams", 0, null, false));
		doReturn(appSettings).when(hlsMuxer).getAppSettings();
		doReturn(resolution).when(hlsMuxer).getResolution();
		return hlsMuxer;
	}

	private Path createFileWithModificationTime(String fileName, Instant modificationTime) throws Exception {
		Path file = Files.createFile(tempDirectory.resolve(fileName));
		Files.setLastModifiedTime(file, FileTime.from(modificationTime));
		return file;
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
	void testWebVttCuesAreOnlyForwardedWhileRunning() {
		WebVttPlaylistManager manager = mock(WebVttPlaylistManager.class);
		ReflectionTestUtils.setField(classUnderTest, "webVttPlaylistManager", manager);
		WebVttCue cue = new WebVttCue(500, 1_500, "Hallo");
		classUnderTest.setIsRunning(new AtomicBoolean(true));
		classUnderTest.writeWebVttCue(2, cue);
		verify(manager).writeWebVttCue(2, cue);

		classUnderTest.setIsRunning(new AtomicBoolean(false));
		classUnderTest.writeWebVttCue(2, new WebVttCue(1_600, 1_900, "Too late"));
		verifyNoMoreInteractions(manager);
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
