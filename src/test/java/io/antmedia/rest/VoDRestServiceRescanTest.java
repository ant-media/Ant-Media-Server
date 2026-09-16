package io.antmedia.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.red5.server.api.scope.IScope;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.AppSettings;
import io.antmedia.cluster.IClusterNotifier;
import io.antmedia.cluster.IClusterStore;
import io.antmedia.datastore.db.InMemoryDataStore;
import io.antmedia.datastore.db.types.VoD;
import io.antmedia.storage.StorageClient;
import io.antmedia.rest.model.Result;
import io.antmedia.test.UnitTestBase;

@Tag("fast")
class VoDRestServiceRescanTest extends UnitTestBase<VoDRestService> {

	@TempDir
	Path temporaryDirectory;
	private String previousRed5Root;
	private MockedStatic<AntMediaApplicationAdapter> settingsPersistence;

	@BeforeEach
	void setServerRoot() throws Exception {
		previousRed5Root = System.getProperty("red5.root");
		System.setProperty("red5.root", temporaryDirectory.toString());
		Files.createDirectories(temporaryDirectory.resolve("webapps/test-app/streams"));
		settingsPersistence = mockStatic(AntMediaApplicationAdapter.class, invocation ->
				"updateAppSettingsFile".equals(invocation.getMethod().getName()) ? true : invocation.callRealMethod());
	}

	@AfterEach
	void restoreServerRoot() {
		settingsPersistence.close();
		if (previousRed5Root == null) {
			System.clearProperty("red5.root");
		}
		else {
			System.setProperty("red5.root", previousRed5Root);
		}
	}

	private AntMediaApplicationAdapter createApplication() {
		AntMediaApplicationAdapter application = spy(new AntMediaApplicationAdapter());
		application.setAppSettings(new AppSettings());
		application.setDataStore(new InMemoryDataStore("vod-directory"));
		application.setStorageClient(mock(StorageClient.class));
		IScope scope = mock(IScope.class);
		when(scope.getName()).thenReturn("test-app");
		application.setScope(scope);
		return application;
	}

	private VoDRestService createService(AntMediaApplicationAdapter application) {
		VoDRestService service = new VoDRestService();
		service.setApplication(application);
		return service;
	}

	@Test
	void testImportUpdatesSettingsAndScansDirectory() throws Exception {
		Path directory = Files.createDirectory(temporaryDirectory.resolve("videos"));
		Files.writeString(directory.resolve("asset.mp4"), "asset");
		AntMediaApplicationAdapter application = createApplication();
		AppSettings settings = application.getAppSettings();
		settings.setHlsTime("6");
		settings.setS3SecretKey("existing-secret");
		IClusterNotifier notifier = mock(IClusterNotifier.class);
		IClusterStore clusterStore = mock(IClusterStore.class);
		when(notifier.getClusterStore()).thenReturn(clusterStore);
		application.setClusterNotifier(notifier);
		VoDRestService service = createService(application);

		assertThat(service.importVoDs(directory.toString()).isSuccess()).isTrue();
		assertThat(settings.getVodFolder()).isEqualTo(directory.toString());
		assertThat(settings.getHlsTime()).isEqualTo("6");
		assertThat(settings.getS3SecretKey()).isEqualTo("existing-secret");
		assertThat(settings.getUpdateTime()).isPositive();
		ArgumentCaptor<AppSettings> saved = ArgumentCaptor.forClass(AppSettings.class);
		settingsPersistence.verify(() -> AntMediaApplicationAdapter.updateAppSettingsFile(eq("test-app"), saved.capture()));
		assertThat(saved.getValue()).isNotSameAs(settings);
		assertThat(saved.getValue().getVodFolder()).isEqualTo(directory.toString());
		verify(clusterStore).saveSettings(settings);
		verify(application).notifySettingsUpdateListeners(settings);
		assertThat(application.getDataStore().getVodList(0, 50, null, null, null, null))
				.extracting(VoD::getFilePath).containsExactly("streams/asset.mp4");
		assertThat(temporaryDirectory.resolve("webapps/test-app/streams/videos")).doesNotExist();

		Files.writeString(directory.resolve("new.mp4"), "asset");
		assertThat(service.importVoDs(directory.toString()).isSuccess()).isTrue();
		assertThat(application.getDataStore().getTotalVodNumber()).isEqualTo(2);

		Path replacement = Files.createDirectory(temporaryDirectory.resolve("replacement"));
		Files.writeString(replacement.resolve("replacement.mp4"), "asset");
		assertThat(service.importVoDs(replacement.toString()).isSuccess()).isTrue();
		assertThat(application.getDataStore().getVodList(0, 50, null, null, null, null))
				.extracting(VoD::getFilePath).containsExactly("streams/replacement.mp4");
		assertThat(directory.resolve("asset.mp4")).exists();
	}

	@Test
	void testImportRejectsInvalidDirectoriesWithoutChangingSettings() throws Exception {
		AntMediaApplicationAdapter application = createApplication();
		VoDRestService service = createService(application);
		Path regularFile = Files.writeString(temporaryDirectory.resolve("file.mp4"), "asset");
		for (String directory : new String[] {null, "", "  ", temporaryDirectory.resolve("missing").toString(), regularFile.toString()}) {
			assertThat(service.importVoDs(directory).isSuccess()).isFalse();
		}
		assertThat(application.getAppSettings().getVodFolder()).isEqualTo("streams");
		verify(application, never()).updateSettings(any(), eq(true), eq(false));
		verify(application, never()).rescanVodAssets();
	}

	@Test
	void testImportResolvesLegacyRelativePaths() {
		AntMediaApplicationAdapter application = createApplication();
		doReturn(new Result(true)).when(application).rescanVodAssets();
		assertThat(createService(application).importVoDs("src/test").isSuccess()).isTrue();
		assertThat(application.getAppSettings().getVodFolder())
				.isEqualTo(Path.of("src/test").toAbsolutePath().normalize().toString());
	}

	@Test
	void testImportReportsSettingsSaveFailure() {
		AntMediaApplicationAdapter application = createApplication();
		settingsPersistence.when(() -> AntMediaApplicationAdapter.updateAppSettingsFile(eq("test-app"), any(AppSettings.class)))
				.thenReturn(false);
		assertThat(createService(application).importVoDs(temporaryDirectory.toString()).isSuccess()).isFalse();
	}

	@Test
	void testUnlinkResetsFolderAndPreservesFilesAndRecordings() throws Exception {
		Path directory = Files.createDirectory(temporaryDirectory.resolve("videos"));
		Path asset = Files.writeString(directory.resolve("asset.mp4"), "asset");
		Path defaultAsset = Files.writeString(temporaryDirectory.resolve("webapps/test-app/streams/default.mp4"), "asset");
		AntMediaApplicationAdapter application = createApplication();
		VoDRestService service = createService(application);
		assertThat(service.importVoDs(directory.toString()).isSuccess()).isTrue();
		application.getDataStore().addVod(new VoD("recording", "stream", "streams/recording.mp4", "recording.mp4", 1, 0, 0, 1,
				VoD.STREAM_VOD, "recorded-vod", null));

		assertThat(service.unlinksVoD(directory.resolve(".").toString()).isSuccess()).isTrue();
		assertThat(application.getAppSettings().getVodFolder()).isEqualTo("streams");
		assertThat(application.getDataStore().getVodList(0, 50, null, null, null, null))
				.extracting(VoD::getFilePath).containsExactlyInAnyOrder("streams/default.mp4", "streams/recording.mp4");
		assertThat(asset).exists();
		assertThat(defaultAsset).exists();
	}

	@Test
	void testUnlinkRejectsEmptyOrUnrelatedDirectory() {
		AntMediaApplicationAdapter application = createApplication();
		application.getAppSettings().setVodFolder(temporaryDirectory.resolve("videos").toString());
		VoDRestService service = createService(application);
		for (String directory : new String[] {null, "", "  ", temporaryDirectory.toString()}) {
			assertThat(service.unlinksVoD(directory).isSuccess()).isFalse();
		}
		assertThat(application.getAppSettings().getVodFolder()).isEqualTo(temporaryDirectory.resolve("videos").toString());
		verify(application, never()).updateSettings(any(), eq(true), eq(false));
	}

	@Test
	void testDirectoryEndpointsKeepTheirRestContract() throws Exception {
		var importMethod = VoDRestService.class.getMethod("importVoDs", String.class);
		var unlinkMethod = VoDRestService.class.getMethod("unlinksVoD", String.class);
		assertThat(importMethod.isAnnotationPresent(jakarta.ws.rs.POST.class)).isTrue();
		assertThat(unlinkMethod.isAnnotationPresent(jakarta.ws.rs.DELETE.class)).isTrue();
		for (var method : new java.lang.reflect.Method[] {importMethod, unlinkMethod}) {
			assertThat(method.getAnnotation(jakarta.ws.rs.Path.class).value()).isEqualTo("/directory");
			assertThat(method.getParameters()[0].getAnnotation(jakarta.ws.rs.QueryParam.class).value()).isEqualTo("directory");
		}
	}

	@Test
	void testRescanEndpointDelegatesToApplication() {
		AntMediaApplicationAdapter application = mock(AntMediaApplicationAdapter.class);
		Result expected = new Result(true, "scan complete");
		when(application.rescanVodAssets()).thenReturn(expected);

		VoDRestService service = new VoDRestService();
		service.setApplication(application);

		assertThat(service.rescanVodAssets()).isSameAs(expected);
		verify(application).rescanVodAssets();
	}
}
