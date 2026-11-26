package io.antmedia.muxer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import io.antmedia.AppSettings;
import io.antmedia.EncoderSettings;
import io.antmedia.storage.StorageClient;
import io.antmedia.test.UnitTestBase;

@Tag("fast")
class HLSMuxerTest extends UnitTestBase<HLSMuxer> {

	@TempDir
	Path tempDirectory;

	@ParameterizedTest
	@ValueSource(strings = {"camera.v1", "red5.properties|x", ".*|x", "camera\\E[1]+"})
	void shouldOnlySelectLiteralStreamFilenamesForCleanup(String name) throws Exception {
		Path manifest = Files.createFile(tempDirectory.resolve("manifest.m3u8"));
		Path segment = Files.createFile(tempDirectory.resolve(name + "000000001.ts"));
		Path fmp4 = Files.createFile(tempDirectory.resolve(name + "000000001.fmp4"));
		Files.createFile(tempDirectory.resolve("red5.properties"));
		Files.createFile(tempDirectory.resolve("server.db"));
		Files.createFile(tempDirectory.resolve("cameraXv1000000001.ts"));
		Files.createFile(tempDirectory.resolve("unrelated000000001.ts"));
		String segmentFilename = tempDirectory.resolve(name + "%09d.ts").toString();
		String regex = HLSMuxer.buildHLSFilesRegularExpression(segmentFilename, segmentFilename.indexOf("%09d"), false);

		File[] selected = HLSMuxer.getHLSFilesInDirectory(manifest.toFile(), regex);

		assertThat(selected).extracting(File::getName).containsExactlyInAnyOrder(
				manifest.getFileName().toString(), segment.getFileName().toString(), fmp4.getFileName().toString());
	}

	@Test
	void shouldPreserveVariantPlaylistAndSegmentCleanup() throws Exception {
		Path manifest = Files.createFile(tempDirectory.resolve("camera.v1.m3u8"));
		Files.createFile(tempDirectory.resolve("camera.v1_english.m3u8"));
		Files.createFile(tempDirectory.resolve("camera.v1_english000000001.ts"));
		Files.createFile(tempDirectory.resolve("camera.v1_french000000001.fmp4"));
		Files.createFile(tempDirectory.resolve("cameraXv1_english000000001.ts"));
		Files.createFile(tempDirectory.resolve("server.db"));
		String segmentFilename = tempDirectory.resolve("camera.v1_%v%09d.ts").toString();
		String regex = HLSMuxer.buildHLSFilesRegularExpression(segmentFilename, segmentFilename.indexOf("%09d"), true);

		File[] selected = HLSMuxer.getHLSFilesInDirectory(manifest.toFile(), regex);

		assertThat(selected).extracting(File::getName).containsExactlyInAnyOrder(
				"camera.v1.m3u8", "camera.v1_english.m3u8",
				"camera.v1_english000000001.ts", "camera.v1_french000000001.fmp4");
	}

	@ParameterizedTest
	@CsvSource(value = {"%09d|000000001", "%Y%m%d-%s|20261124-1795520000", "%s-%%09d|1795520000-000000001"}, delimiter = '|')
	void shouldPreserveConfiguredSegmentSuffixCleanup(String format, String suffix) throws Exception {
		Path manifest = Files.createFile(tempDirectory.resolve("camera.m3u8"));
		Path segment = Files.createFile(tempDirectory.resolve("camera" + suffix + ".ts"));
		Files.createFile(tempDirectory.resolve("other" + suffix + ".ts"));
		String segmentFilename = tempDirectory.resolve("camera" + format + ".ts").toString();
		String regex = HLSMuxer.buildHLSFilesRegularExpression(segmentFilename, segmentFilename.indexOf(format), false);

		File[] selected = HLSMuxer.getHLSFilesInDirectory(manifest.toFile(), regex);

		assertThat(selected).extracting(File::getName).containsExactlyInAnyOrder(
				manifest.getFileName().toString(), segment.getFileName().toString());
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
}
