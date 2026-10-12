package io.antmedia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doReturn;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.red5.server.api.scope.IScope;

import io.antmedia.datastore.db.InMemoryDataStore;
import io.antmedia.datastore.db.types.VoD;
import io.antmedia.rest.model.Result;
import io.antmedia.test.UnitTestBase;

@Tag("fast")
class AntMediaApplicationAdapterVodTest extends UnitTestBase<AntMediaApplicationAdapter> {

	@TempDir
	Path temporaryDirectory;

	@Test
	void testRescanVodAssetsAddsMissingAndRemovesOnlyStaleUserVods() throws Exception {
		Path nestedDirectory = Files.createDirectories(temporaryDirectory.resolve("nested"));
		Files.writeString(temporaryDirectory.resolve("asset.mp4"), "asset");
		Files.writeString(nestedDirectory.resolve("playlist.m3u8"), "#EXTM3U");
		Files.writeString(temporaryDirectory.resolve("ignored.txt"), "ignored");

		InMemoryDataStore datastore = new InMemoryDataStore("vod-rescan");
		datastore.addVod(new VoD("stale", "old", "streams/stale.mp4", "stale.mp4", 1, 0, 0, 1,
				VoD.USER_VOD, "stale-user-vod", null));
		datastore.addVod(new VoD("recording", "stream", "streams/recording.mp4", "recording.mp4", 1, 0, 0, 1,
				VoD.STREAM_VOD, "recorded-vod", null));

		AntMediaApplicationAdapter adapter = new AntMediaApplicationAdapter();
		adapter.setDataStore(datastore);
		AppSettings settings = new AppSettings();
		settings.setVodFolder(temporaryDirectory.toString());
		adapter.setAppSettings(settings);
		IScope scope = mock(IScope.class);
		when(scope.getName()).thenReturn("test-app");
		adapter.setScope(scope);

		Result firstScan = adapter.rescanVodAssets();
		assertThat(firstScan.isSuccess()).isTrue();
		assertThat(firstScan.getMessage()).contains("2 VoD assets discovered", "2 added", "1 removed");

		List<VoD> vods = datastore.getVodList(0, 50, null, null, null, null);
		assertThat(vods).extracting(VoD::getFilePath)
				.containsExactlyInAnyOrder("streams/asset.mp4", "streams/nested/playlist.m3u8",
						"streams/recording.mp4");
		assertThat(datastore.getVoD("recorded-vod")).isNotNull();
		assertThat(datastore.getVoD("stale-user-vod")).isNull();

		Result secondScan = adapter.rescanVodAssets();
		assertThat(secondScan.getMessage()).contains("0 added", "0 removed");
		assertThat(datastore.getTotalVodNumber()).isEqualTo(3);

		Files.delete(temporaryDirectory.resolve("asset.mp4"));
		Result thirdScan = adapter.rescanVodAssets();
		assertThat(thirdScan.getMessage()).contains("1 removed");
		assertThat(datastore.getVodList(0, 50, null, null, null, null))
				.extracting(VoD::getFilePath)
				.containsExactlyInAnyOrder("streams/nested/playlist.m3u8", "streams/recording.mp4");
	}

	@Test
	void testRescanRejectsRelativeVodFolderWithoutRemovingRecords() {
		InMemoryDataStore datastore = new InMemoryDataStore("invalid-vod-rescan");
		datastore.addVod(new VoD("existing", "old", "streams/existing.mp4", "existing.mp4", 1, 0, 0, 1,
				VoD.USER_VOD, "existing-user-vod", null));

		AntMediaApplicationAdapter adapter = new AntMediaApplicationAdapter();
		adapter.setDataStore(datastore);
		AppSettings settings = new AppSettings();
		settings.setVodFolder("relative/videos");
		adapter.setAppSettings(settings);

		assertThat(adapter.rescanVodAssets().isSuccess()).isFalse();
		assertThat(datastore.getVoD("existing-user-vod")).isNotNull();
	}

	@Test
	void testRescanUsesDefaultApplicationStreamsFolder() throws Exception {
		AntMediaApplicationAdapter adapter = new AntMediaApplicationAdapter();
		adapter.setDataStore(new InMemoryDataStore("default-vod-rescan"));
		AppSettings settings = new AppSettings();
		settings.setVodFolder(AntMediaApplicationAdapter.STREAMS);
		adapter.setAppSettings(settings);

		assertThat(adapter.rescanVodAssets().isSuccess()).isFalse();

		IScope scope = mock(IScope.class);
		when(scope.getName()).thenReturn("LiveApp");
		adapter.setScope(scope);
		Path streamsDirectory = Files.createDirectories(temporaryDirectory.resolve("webapps/LiveApp/streams"));
		Files.writeString(streamsDirectory.resolve("default.mp4"), "asset");
		String previousRed5Root = System.getProperty("red5.root");
		try {
			System.setProperty("red5.root", temporaryDirectory.toString());
			assertThat(adapter.rescanVodAssets().isSuccess()).isTrue();
			assertThat(adapter.getDataStore().getVodList(0, 50, null, null, null, null))
					.extracting(VoD::getFilePath).containsExactly("streams/default.mp4");
		}
		finally {
			if (previousRed5Root == null) {
				System.clearProperty("red5.root");
			}
			else {
				System.setProperty("red5.root", previousRed5Root);
			}
		}
	}

	@Test
	void testVodFolderSettingChangeTriggersRescan() {
		AntMediaApplicationAdapter adapter = spy(new AntMediaApplicationAdapter());
		doReturn(new Result(true)).when(adapter).rescanVodAssets();

		adapter.rescanVodAssetsIfFolderChanged(false);
		verify(adapter, never()).rescanVodAssets();

		adapter.rescanVodAssetsIfFolderChanged(true);
		verify(adapter).rescanVodAssets();

		doReturn(new Result(false, "scan failed")).when(adapter).rescanVodAssets();
		adapter.rescanVodAssetsIfFolderChanged(true);
		verify(adapter, times(2)).rescanVodAssets();
	}
}
