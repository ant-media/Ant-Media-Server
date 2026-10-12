package io.antmedia.muxer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.antmedia.storage.StorageClient;
import io.antmedia.test.UnitTestBase;

@Tag("fast")
class HLSMuxerTest extends UnitTestBase<HLSMuxer> {

	@TempDir
	Path tempDirectory;

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
}
