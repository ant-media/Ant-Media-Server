package io.antmedia.test;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import io.antmedia.filter.TokenFilterManager;
import io.antmedia.statistic.IStatsCollector;
import io.antmedia.statistic.StatsCollector;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.red5.server.api.IContext;
import org.red5.server.api.scope.IScope;
import org.red5.server.api.stream.IPlayItem;
import org.red5.server.api.stream.ISubscriberStream;
import org.red5.server.messaging.IConsumer;
import org.red5.server.scope.BroadcastScope;
import org.red5.server.stream.ClientBroadcastStream;
import org.red5.server.stream.PlaylistSubscriberStream;
import org.red5.server.stream.SingleItemSubscriberStream;
import org.slf4j.Logger;
import org.springframework.context.ApplicationContext;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.SendResponse;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.AntMediaApplicationAdapter.RTMPClusterStreamFetcherListener;
import io.antmedia.AppSettings;
import io.antmedia.IAppSettingsUpdateListener;
import io.antmedia.cluster.ClusterNode;
import io.antmedia.cluster.IClusterNotifier;
import io.antmedia.cluster.IClusterStore;
import io.antmedia.datastore.db.DataStore;
import io.antmedia.datastore.db.DataStoreFactory;
import io.antmedia.datastore.db.IDataStoreFactory;
import io.antmedia.datastore.db.InMemoryDataStore;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.datastore.db.types.BroadcastUpdate;
import io.antmedia.datastore.db.types.VoD;
import io.antmedia.integration.AppFunctionalV2Test;
import io.antmedia.licence.ILicenceService;
import io.antmedia.muxer.IAntMediaStreamHandler;
import io.antmedia.muxer.RtmpProvider;
import io.antmedia.muxer.MuxAdaptor;
import io.antmedia.plugin.api.IClusterStreamFetcher;
import io.antmedia.plugin.api.IPacketListener;
import io.antmedia.plugin.api.IStreamListener;
import io.antmedia.rest.model.Result;
import io.antmedia.security.AcceptOnlyStreamsInDataStore;
import io.antmedia.settings.ServerSettings;
import io.antmedia.statistic.type.WebRTCAudioReceiveStats;
import io.antmedia.statistic.type.WebRTCAudioSendStats;
import io.antmedia.statistic.type.WebRTCVideoReceiveStats;
import io.antmedia.statistic.type.WebRTCVideoSendStats;
import io.antmedia.storage.StorageClient;
import io.antmedia.streamsource.RTMPClusterStreamFetcher;
import io.antmedia.streamsource.StreamFetcher;
import io.antmedia.streamsource.StreamFetcherManager;
import io.antmedia.track.ISubtrackPoller;
import io.antmedia.websocket.WebSocketConstants;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.impl.VertxImpl;
import io.vertx.ext.dropwizard.DropwizardMetricsOptions;


public class AntMediaApplicationAdaptorUnitTest {

	AntMediaApplicationAdapter adapter;
	String streamsFolderPath = "webapps/test/streams";

	Vertx vertx = Vertx.vertx();
	
	Logger logger = org.slf4j.LoggerFactory.getLogger(AntMediaApplicationAdaptorUnitTest.class);

	@Rule
	public TestRule watcher = new TestWatcher() {
		protected void starting(Description description) {
			System.out.println("Starting test: " + description.getMethodName());
		}

		protected void failed(Throwable e, Description description) {
			System.out.println("Failed test: " + description.getMethodName());
		};
		protected void finished(Description description) {
			System.out.println("Finishing test: " + description.getMethodName());
		};
	};

	@Before
	public void before() {
		adapter = new AntMediaApplicationAdapter();
		adapter.setVertx(vertx);
		IScope scope = Mockito.mock(IScope.class);
		Mockito.when(scope.getName()).thenReturn("junit");
		adapter.setScope(scope);
		File f = new File(streamsFolderPath);
		try {
			AppFunctionalV2Test.delete(f);
		} catch (IOException e) {
			e.printStackTrace();
		}

		File webApps = new File("webapps");
		if (!webApps.exists()) {
			webApps.mkdirs();
		}
		File junit = new File(webApps, "junit");
		if (!junit.exists()) {
			junit.mkdirs();
		}

		File webinf = new File(junit, "WEB-INF");
		if (!webinf.exists()) {
			webinf.mkdirs();
		}

	}

	@After
	public void after() {
		adapter = null;

		try {
			AppFunctionalV2Test.delete(new File("webapps"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	public void testFirebase() throws IOException, FirebaseMessagingException {
		FileInputStream serviceAccount =
				new FileInputStream("path/to/serviceAccountKey.json");

		FirebaseOptions options = new FirebaseOptions.Builder()
				.setCredentials(GoogleCredentials.fromStream(serviceAccount))
				.build();

		FirebaseApp.initializeApp(options);

		{
			String registrationToken = "YOUR_REGISTRATION_TOKEN";

			// See documentation on defining a message payload.
			Message message = Message.builder()
					.putData("score", "850")
					.putData("time", "2:45")
					.setToken(registrationToken)
					.build();

			// Send a message to the device corresponding to the provided
			// registration token.
			String response = FirebaseMessaging.getInstance().send(message);
			// Response is a message ID string.
			System.out.println("Successfully sent message: " + response);
		}

		{


			List<String> registrationTokens = Arrays.asList(
					"YOUR_REGISTRATION_TOKEN_1",
					// ...
					"YOUR_REGISTRATION_TOKEN_n"
					);

			MulticastMessage message = MulticastMessage.builder()
					.putData("score", "850")
					.putData("time", "2:45")
					.addAllTokens(registrationTokens)
					.build();
			BatchResponse response = FirebaseMessaging.getInstance().sendMulticast(message);

			if (response.getFailureCount() > 0) {
				List<SendResponse> responses = response.getResponses();
				List<String> failedTokens = new ArrayList<>();
				for (int i = 0; i < responses.size(); i++) {
					if (!responses.get(i).isSuccessful()) {
						// The order of responses corresponds to the order of the registration tokens.
						failedTokens.add(registrationTokens.get(i));
					}
				}

				System.out.println("List of tokens that caused failures: " + failedTokens);
			}
		}

	}

	@Test
	public void testEndpointReachable() {
		boolean endpointReachable = AntMediaApplicationAdapter.isEndpointReachable("http://antmedia.io/not_exist");
		//it should be true because we're just checking if it's reachable
		assertTrue(endpointReachable);

		endpointReachable = AntMediaApplicationAdapter.isEndpointReachable("http://antmedia.io:45454/not_exist");
		assertFalse(endpointReachable);

		boolean instanceAlive = AntMediaApplicationAdapter.isInstanceAlive("antmedia.io", null, 80, "");
		assertTrue(instanceAlive);

		instanceAlive = AntMediaApplicationAdapter.isInstanceAlive("antmedia.io", null, 4545, "");
		assertFalse(instanceAlive);

		instanceAlive = AntMediaApplicationAdapter.isInstanceAlive("", null, 4545, "");
		assertTrue(instanceAlive);

		instanceAlive = AntMediaApplicationAdapter.isInstanceAlive("localhost", "localhost", 4545, "");
		assertTrue(instanceAlive);

	}

	@Test
	public void testIsIncomingTimeValid() {
		AppSettings newSettings = new AppSettings();

		IScope scope = mock(IScope.class);

		when(scope.getName()).thenReturn("junit");

		AntMediaApplicationAdapter spyAdapter = Mockito.spy(adapter);
		AppSettings appSettings = new AppSettings();
		spyAdapter.setAppSettings(appSettings);
		IContext context = mock(IContext.class);
		when(context.getBean(any())).thenReturn(mock(AcceptOnlyStreamsInDataStore.class));
		when(scope.getContext()).thenReturn(context);
		Mockito.doReturn(mock(DataStore.class)).when(spyAdapter).getDataStore();


		newSettings = new AppSettings();

		assertFalse(spyAdapter.isIncomingSettingsDifferent(newSettings));

		newSettings.setUpdateTime(1000);
		assertTrue(spyAdapter.isIncomingSettingsDifferent(newSettings));

		appSettings.setUpdateTime(900);
		assertTrue(spyAdapter.isIncomingSettingsDifferent(newSettings));

		appSettings.setUpdateTime(2000);
		assertTrue(spyAdapter.isIncomingSettingsDifferent(newSettings));

		newSettings.setUpdateTime(3000);
		assertTrue(spyAdapter.isIncomingSettingsDifferent(newSettings));

		appSettings.setUpdateTime(3000);
		assertFalse(spyAdapter.isIncomingSettingsDifferent(newSettings));




	}

	public static class AppSettingsChild extends AppSettings {

		public String field = "field";
		public static String fieldStatic = "fieldStatic";

		public static final String fieldFinalStatic = "fieldFinalStatic";
	}

	@Test
	public void testFieldValue() {

		AppSettingsChild settings = new AppSettingsChild();
		AppSettingsChild newSettings = new AppSettingsChild();

		settings.field = "field";
		newSettings.field = "field2";

		Field field;
		try {
			field = settings.getClass().getDeclaredField("field");
			boolean result = AntMediaApplicationAdapter.setAppSettingsFieldValue(settings, newSettings, field);
			assertTrue(result);
			assertEquals(settings.field, newSettings.field);


			field = settings.getClass().getDeclaredField("fieldStatic");
			result = AntMediaApplicationAdapter.setAppSettingsFieldValue(settings, newSettings, field);
			assertFalse(result);

			field = settings.getClass().getDeclaredField("fieldFinalStatic");
			result = AntMediaApplicationAdapter.setAppSettingsFieldValue(settings, newSettings, field);
			assertFalse(result);


			AppSettings settings2 = new AppSettings();
			AppSettings newSettings2 = new AppSettings();
			settings2.setAcceptOnlyStreamsInDataStore(true);
			newSettings2.setAcceptOnlyStreamsInDataStore(false);

			field = settings2.getClass().getDeclaredField("acceptOnlyRoomsInDataStore");
			result = AntMediaApplicationAdapter.setAppSettingsFieldValue(settings2, newSettings2, field);
			assertTrue(result);
			assertEquals(settings2.isAcceptOnlyRoomsInDataStore(), newSettings2.isAcceptOnlyRoomsInDataStore());






		} catch (NoSuchFieldException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (SecurityException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}



	}


	@Test
	public void testAppSettings() 
	{
		AppSettings settings = new AppSettings();

		AppSettings newSettings = Mockito.spy(new AppSettings());
		newSettings.setVodFolder("");
		newSettings.setListenerHookURL("");
		newSettings.setHlsflags("delete_segments");
		newSettings.setTokenHashSecret("");
		newSettings.setDataChannelPlayerDistribution("");
		newSettings.setDashSegDuration("");

		IScope scope = mock(IScope.class);

		when(scope.getName()).thenReturn("junit");

		AntMediaApplicationAdapter spyAdapter = Mockito.spy(adapter);
		IContext context = mock(IContext.class);
		when(context.getBean(any())).thenReturn(mock(AcceptOnlyStreamsInDataStore.class));
		when(scope.getContext()).thenReturn(context);
		Mockito.doReturn(mock(DataStore.class)).when(spyAdapter).getDataStore();

		StorageClient storageClient = Mockito.mock(StorageClient.class);
		spyAdapter.setStorageClient(storageClient);

		assertEquals(storageClient, spyAdapter.getStorageClient());

		spyAdapter.setAppSettings(settings);
		spyAdapter.setScope(scope);
		assertEquals("", settings.getHlsPlayListType());
		spyAdapter.updateSettings(newSettings, true, false);

		assertEquals("", settings.getHlsPlayListType());
		assertEquals(newSettings.getHlsPlayListType(), settings.getHlsPlayListType());

		assertEquals("", settings.getDashSegDuration());
		assertEquals(newSettings.getDashSegDuration(), settings.getDashSegDuration());

		assertEquals("delete_segments", settings.getHlsflags());
		assertEquals(newSettings.getHlsflags(), settings.getHlsflags());


		IClusterNotifier clusterNotifier = mock(IClusterNotifier.class);

		IClusterStore clusterStore = mock(IClusterStore.class);

		when(clusterNotifier.getClusterStore()).thenReturn(clusterStore);
		spyAdapter.setClusterNotifier(clusterNotifier);

		newSettings.setVodFolder(null);
		newSettings.setHlsPlayListType(null);
		newSettings.setHlsflags(null);
		spyAdapter.updateSettings(newSettings, true, false);

		assertEquals("", settings.getVodUploadFinishScript());
		assertEquals(null, settings.getHlsPlayListType());
		assertEquals(null, settings.getHlsflags());
		assertEquals(newSettings.getHlsflags(), settings.getHlsflags());

		verify(clusterNotifier, times(1)).getClusterStore();
		verify(clusterStore, times(1)).saveSettings(settings);

		IAppSettingsUpdateListener settingsListener = mock(IAppSettingsUpdateListener.class);
		spyAdapter.addSettingsUpdateListener(settingsListener);

		spyAdapter.updateSettings(newSettings, false, false);
		//it should not change times(1) because we don't want it to update the datastore
		verify(clusterNotifier, times(1)).getClusterStore();
		verify(clusterStore, times(1)).saveSettings(settings);

		//make sure settingsUpdated is called
		verify(settingsListener, times(1)).settingsUpdated(settings);

		settings.setUpdateTime(900);
		newSettings.setUpdateTime(900);
		assertFalse(spyAdapter.updateSettings(newSettings, false, true));

		newSettings.setUpdateTime(1000);
		assertTrue(spyAdapter.updateSettings(newSettings, false, true));


		newSettings.setPlayJwtControlEnabled(true);
		newSettings.setPlayTokenControlEnabled(true);
		newSettings.setEnableTimeTokenForPlay(true);

		assertFalse(spyAdapter.updateSettings(newSettings, false, false));

		newSettings.setPlayJwtControlEnabled(false);
		newSettings.setPlayTokenControlEnabled(false);


		assertTrue(spyAdapter.updateSettings(newSettings, false, false));

		newSettings.setEnableTimeTokenForPublish(true);
		newSettings.setPublishTokenControlEnabled(true);
		newSettings.setPublishJwtControlEnabled(true);

		assertFalse(spyAdapter.updateSettings(newSettings, false, false));
		newSettings.setEnableTimeTokenForPublish(false);
		newSettings.setPublishJwtControlEnabled(false);

		assertTrue(spyAdapter.updateSettings(newSettings, false, false));

	}

	@Test
	public void testResetBroadcasts() 
	{
		IScope scope = mock(IScope.class);
		when(scope.getName()).thenReturn("junit");

		DataStore dataStore = new InMemoryDataStore("dbname");
		DataStoreFactory dsf = Mockito.mock(DataStoreFactory.class);
		Mockito.when(dsf.getDataStore()).thenReturn(dataStore);

		AntMediaApplicationAdapter spyAdapter = Mockito.spy(adapter);
		IContext context = mock(IContext.class);
		when(context.getBean(spyAdapter.VERTX_BEAN_NAME)).thenReturn(vertx);
		StorageClient storageClient = Mockito.mock(StorageClient.class);
		when(context.getBean(StorageClient.BEAN_NAME)).thenReturn(storageClient);

		when(scope.getContext()).thenReturn(context);
		spyAdapter.setDataStoreFactory(dsf);

		Mockito.doReturn(dataStore).when(spyAdapter).getDataStore();
		spyAdapter.setScope(scope);
		spyAdapter.setAppSettings(new AppSettings());
		spyAdapter.setStreamPublishSecurityList(new ArrayList<>());
		spyAdapter.setStreamPlaySecurityList(new ArrayList<>());

		// Add 1. Broadcast
		Broadcast broadcast = new Broadcast();
		broadcast.setZombi(true);		


		// Add 2. Broadcast
		Broadcast broadcast2 = new Broadcast();

		broadcast2.setWebRTCViewerCount(100);
		broadcast2.setRtmpViewerCount(10);
		broadcast2.setHlsViewerCount(1000);
		broadcast2.setUpdateTime(System.currentTimeMillis());
		broadcast2.setStatus(spyAdapter.BROADCAST_STATUS_BROADCASTING);


		// Add 3. Broadcast
		Broadcast broadcast3 = new Broadcast();
		broadcast3.setUpdateTime(System.currentTimeMillis());
		broadcast3.setStatus(spyAdapter.BROADCAST_STATUS_PREPARING);

		dataStore.save(broadcast);
		dataStore.save(broadcast2);
		dataStore.save(broadcast3);

		// Should 3 broadcast in DB
		assertEquals(3, dataStore.getBroadcastCount());

		Result result = new Result(false);
		Mockito.when(spyAdapter.createInitializationProcess(Mockito.anyString())).thenReturn(result);
		//When createInitializationProcess(scope.getName());

		spyAdapter.setDataStore(dataStore);

		spyAdapter.setServerSettings(new ServerSettings());

		spyAdapter.appStart(scope);

		// Should 2 broadcast in DB, because delete zombie stream
		assertEquals(2, dataStore.getBroadcastCount());

		List<Broadcast> broadcastList = dataStore.getBroadcastList(0, 10, null, null, null, null);
		for (Broadcast testBroadcast : broadcastList) 
		{
			assertEquals(0, testBroadcast.getWebRTCViewerCount());
			assertEquals(0, testBroadcast.getHlsViewerCount());
			assertEquals(0, testBroadcast.getRtmpViewerCount());

			assertEquals(spyAdapter.BROADCAST_STATUS_FINISHED, testBroadcast.getStatus());
		}	
	}

	/**
	 * Test code for https://github.com/ant-media/Ant-Media-Server/issues/4748
	 */
	@Test
	public void testSyncUserVoDBug() {
		File streamsFolder = new File("webapps/junit/streams");
		assertFalse(streamsFolder.exists());	

		//any target
		Path target = new File("/usr").toPath();
		try {
			Files.createSymbolicLink(streamsFolder.toPath(), target);
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}


		assertTrue(streamsFolder.exists());	
		assertTrue(Files.isSymbolicLink(streamsFolder.toPath()));


		IScope scope = Mockito.mock(IScope.class);
		Mockito.when(scope.getName()).thenReturn("junit");

		AntMediaApplicationAdapter spyAdapter = Mockito.spy(adapter);
		Mockito.doReturn(scope).when(spyAdapter).getScope();

		DataStore dataStore = new InMemoryDataStore("dbname");
		DataStoreFactory dsf = Mockito.mock(DataStoreFactory.class);
		Mockito.when(dsf.getDataStore()).thenReturn(dataStore);
		spyAdapter.setDataStoreFactory(dsf);


		spyAdapter.synchUserVoDFolder(null, null);
		Mockito.verify(spyAdapter, Mockito.never()).deleteSymbolicLink(any(), any());
		Mockito.verify(spyAdapter, Mockito.never()).createSymbolicLink(any(), any());

		assertTrue(streamsFolder.exists());

		//Don't delete the file if they are the same files
		spyAdapter.deleteSymbolicLink(new File(""), streamsFolder);

		assertTrue(streamsFolder.exists());


	}

	@Test
	public void testSynchUserVoD() {
		File streamsFolder = new File(streamsFolderPath);
		if (!streamsFolder.exists()) {
			assertTrue(streamsFolder.mkdirs());
		}
		DataStore dataStore = new InMemoryDataStore("dbname");
		DataStoreFactory dsf = Mockito.mock(DataStoreFactory.class);
		Mockito.when(dsf.getDataStore()).thenReturn(dataStore);
		adapter.setDataStoreFactory(dsf);

		IScope scope = Mockito.mock(IScope.class);
		Mockito.when(scope.getName()).thenReturn("test");

		AntMediaApplicationAdapter spyAdapter = Mockito.spy(adapter);
		Mockito.doReturn(scope).when(spyAdapter).getScope();



		File realPath = new File("src/test/resources");
		assertTrue(realPath.exists());

		File[] listFiles = realPath.listFiles();
		int numberOfFiles = 0;
		for (File file : listFiles) {
			String extension = FilenameUtils.getExtension(file.getName());
			if (file.isFile() && ("mp4".equals(extension) || "flv".equals(extension) || "mkv".equals(extension))) {
				numberOfFiles++;
			}
		}

		String linkFilePath = streamsFolder.getAbsolutePath() + "/resources";
		File linkFile = new File(linkFilePath);
		//Files.isSymbolicLink(linkFile.toPath());
		try {
			Files.deleteIfExists(linkFile.toPath());
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}


		boolean result = spyAdapter.synchUserVoDFolder(null, realPath.getAbsolutePath());
		assertTrue(result);

		//we know there are files in src/test/resources
		//test_short.flv
		//test_video_360p_subtitle.flv
		//test_Video_360p.flv
		//test.flv
		//sample_MP4_480.mp4
		//high_profile_delayed_video.flv
		//test_video_360p_pcm_audio.mkv
		List<VoD> vodList = dataStore.getVodList(0, 50, null, null, null, null);
		assertEquals(numberOfFiles, vodList.size());

		for (VoD voD : vodList) {
			assertEquals("streams/resources/" + voD.getVodName(), voD.getFilePath());
		}

		linkFile = new File(streamsFolder, "resources");
		assertTrue(linkFile.exists());

	}

	@Test
	public void testMuxingFinishedWithPreview() throws Exception{
		AppSettings appSettings = new AppSettings();
		appSettings.setGeneratePreview(true);
		appSettings.setMuxerFinishScript("src/test/resources/echo.sh");

		adapter.setAppSettings(appSettings);
		File f = new File ("src/test/resources/hello_script");

		DataStore dataStore = new InMemoryDataStore("dbname");
		DataStoreFactory dsf = Mockito.mock(DataStoreFactory.class);
		Mockito.when(dsf.getDataStore()).thenReturn(dataStore);
		adapter.setDataStoreFactory(dsf);
		Broadcast broadcast = new Broadcast();
		broadcast.setStreamId("streamId");
		dataStore.save(broadcast);

		adapter.setVertx(vertx);

		File anyFile = new File("src/test/resources/sample_MP4_480.mp4");

		File preview = new File("src/test/resources/preview.png");
		if (f.exists()) { //if it exists delete it due to cache
			Files.delete(f.toPath());
		}

		assertFalse(f.exists());


		adapter.muxingFinished(broadcast, "streamId", anyFile, 0, 100, 480, "src/test/resources/preview.png", null);

		await().atMost(5, TimeUnit.SECONDS).until(()-> f.exists());

		try {
			Files.delete(f.toPath());
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	@Test
	public void testMuxingFinished() throws Exception {

		AppSettings appSettings = new AppSettings();
		appSettings.setMuxerFinishScript("src/test/resources/echo.sh");

		adapter.setAppSettings(appSettings);
		File f = new File ("src/test/resources/hello_script");

		DataStore dataStore = new InMemoryDataStore("dbname");
		DataStoreFactory dsf = Mockito.mock(DataStoreFactory.class);
		Mockito.when(dsf.getDataStore()).thenReturn(dataStore);
		adapter.setDataStoreFactory(dsf);

		Broadcast broadcast = new Broadcast();
		broadcast.setStreamId("streamId");
		dataStore.save(broadcast);

		adapter.setVertx(vertx);

		File anyFile = new File("src/test/resources/sample_MP4_480.mp4");

		{
			if (f.exists()) { //if it exists delete it due to cache
				Files.delete(f.toPath());
			}
			assertFalse(f.exists());

			adapter.muxingFinished(broadcast, broadcast.getStreamId(), anyFile, 0, 100, 480, null, null);

			await().atMost(5, TimeUnit.SECONDS).until(()-> f.exists());

			try {
				Files.delete(f.toPath());
			} catch (IOException e) {
				e.printStackTrace();
				fail(e.getMessage());
			}
		}

		{
			appSettings.setMuxerFinishScript("");
			adapter.setAppSettings(appSettings);

			assertFalse(f.exists());

			adapter.muxingFinished(broadcast, broadcast.getStreamId(), anyFile, 0, 100, 480, "", null);

			await().pollDelay(3, TimeUnit.SECONDS).atMost(4, TimeUnit.SECONDS).until(()-> !f.exists());
		}

	}

	@Test
	public void testRunMuxerScript() throws IOException {
		File f = new File ("src/test/resources/hello_script");
		if (f.exists()) { // if it exists delete it due to cache
			Files.delete(f.toPath());
		}
		assertFalse(f.exists());

		adapter.setVertx(vertx);
		adapter.runScript("src/test/resources/echo.sh");

		await().atMost(5, TimeUnit.SECONDS).until(()-> f.exists());

		try {
			Files.delete(f.toPath());
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}


	@Test
	public void testSendPost() {
		try {
			AntMediaApplicationAdapter spyAdaptor = Mockito.spy(adapter);
			AppSettings appSettings = new AppSettings();
			spyAdaptor.setAppSettings(appSettings);

			CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);
			Mockito.doReturn(httpClient).when(spyAdaptor).getHttpClient();

			CloseableHttpResponse httpResponse = Mockito.mock(CloseableHttpResponse.class);
			Mockito.when(httpClient.execute(any())).thenReturn(httpResponse);
			Mockito.when(httpResponse.getStatusLine()).thenReturn(Mockito.mock(StatusLine.class));

			Mockito.when(httpResponse.getEntity()).thenReturn(null);

			spyAdaptor.sendPOST("http://any_url", new JSONObject(), appSettings.getWebhookRetryCount(), null);

			Mockito.verify(spyAdaptor, Mockito.times(0)).retrySendPostWithDelay(any(), any(), anyInt(), anyString());


			HttpEntity entity = Mockito.mock(HttpEntity.class);
			InputStream is = new ByteArrayInputStream(ByteBuffer.allocate(10).array());
			Mockito.when(entity.getContent()).thenReturn(is);
			Mockito.when(httpResponse.getEntity()).thenReturn(entity);
			JSONObject jsonPayload = new JSONObject();
			jsonPayload.put("action", "action_any");
			spyAdaptor.sendPOST("http://any_url", jsonPayload, appSettings.getWebhookRetryCount(), null);

			Mockito.verify(spyAdaptor, Mockito.times(0)).retrySendPostWithDelay(any(), any(), anyInt(), anyString());

			appSettings.setWebhookRetryCount(1);

			HttpEntity entity2 = Mockito.mock(HttpEntity.class);
			InputStream is2 = new ByteArrayInputStream(ByteBuffer.allocate(10).array());
			Mockito.when(entity2.getContent()).thenReturn(is2);
			Mockito.when(httpResponse.getEntity()).thenReturn(entity2);

			StatusLine statusLine = Mockito.mock(StatusLine.class);

			Mockito.when(httpResponse.getStatusLine()).thenReturn(statusLine);
			Mockito.when(statusLine.getStatusCode()).thenReturn(404);

			spyAdaptor.sendPOST("http://any_url", jsonPayload, appSettings.getWebhookRetryCount(), null);

			verify(spyAdaptor).retrySendPostWithDelay(
					ArgumentMatchers.eq("http://any_url"),
					ArgumentMatchers.eq(jsonPayload),
					ArgumentMatchers.eq(appSettings.getWebhookRetryCount() - 1), 
					isNull(String.class)
					);

			Mockito.when(statusLine.getStatusCode()).thenReturn(200);
			spyAdaptor.sendPOST("http://any_url", jsonPayload, appSettings.getWebhookRetryCount(), null);

			when(httpClient.execute(any())).thenThrow(new IOException("Simulated IOException"));
			spyAdaptor.sendPOST("http://any_url", jsonPayload, appSettings.getWebhookRetryCount(), null);

			appSettings.setWebhookRetryCount(0);
			spyAdaptor.sendPOST("http://any_url", jsonPayload, appSettings.getWebhookRetryCount(), null);
			Mockito.verify(spyAdaptor, Mockito.times(2)).retrySendPostWithDelay(any(), any(), anyInt(), any());
			ArgumentCaptor<HttpPost> httpPostCaptor = ArgumentCaptor.forClass(HttpPost.class);
			Mockito.verify(httpClient, times(6)).execute(httpPostCaptor.capture());
			assertTrue(httpPostCaptor.getValue().getEntity() instanceof StringEntity);

			appSettings.setWebhookRetryCount(1);
			spyAdaptor.sendPOST("http://any_url", jsonPayload, appSettings.getWebhookRetryCount(), ContentType.APPLICATION_FORM_URLENCODED.getMimeType());
			Mockito.verify(spyAdaptor, Mockito.times(3)).retrySendPostWithDelay(any(), any(), anyInt(), any());

			Mockito.verify(httpClient, times(7)).execute(httpPostCaptor.capture());

			assertTrue(httpPostCaptor.getValue().getEntity() instanceof UrlEncodedFormEntity);


		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testStartStopPublishWithSubscriberId() throws Exception {
		AntMediaApplicationAdapter spyAdaptor = Mockito.spy(adapter);
		spyAdaptor.setDataStore(new InMemoryDataStore("testStartStopPublishWithSubscriberId"));
		spyAdaptor.setServerSettings(new ServerSettings());
		AppSettings appSettings = new AppSettings();
		spyAdaptor.setAppSettings(appSettings);

		Broadcast broadcast = new Broadcast();
		broadcast.setStreamId("streamId");


		String hookURL = "listener_hook_url";
		appSettings.setListenerHookURL(hookURL);

		IStreamListener streamListener = Mockito.mock(IStreamListener.class);
		spyAdaptor.addStreamListener(streamListener);

		String subscriberId = "subscriberId";
		spyAdaptor.startPublish(broadcast.getStreamId(), 0, IAntMediaStreamHandler.PUBLISH_TYPE_RTMP, subscriberId, null);
		verify(spyAdaptor, Mockito.timeout(2000).times(1)).notifyHook(hookURL, broadcast.getStreamId(), "", 
				AntMediaApplicationAdapter.HOOK_ACTION_START_LIVE_STREAM, null, null, null, null, "", subscriberId, null);


		ArgumentCaptor<Broadcast> broadcastCaptor = ArgumentCaptor.forClass(Broadcast.class);
		verify(streamListener, Mockito.timeout(2000).times(1)).streamStarted(broadcastCaptor.capture());
		assertEquals(broadcast.getStreamId(), broadcastCaptor.getValue().getStreamId());


		spyAdaptor.closeBroadcast(broadcast.getStreamId(), subscriberId, null);

		verify(spyAdaptor, Mockito.timeout(2000).times(1)).notifyHook(hookURL, broadcast.getStreamId(), "", 
				AntMediaApplicationAdapter.HOOK_ACTION_END_LIVE_STREAM, null, null, null, null, "", subscriberId, null);
		broadcastCaptor = ArgumentCaptor.forClass(Broadcast.class);
		verify(streamListener, Mockito.timeout(2000).times(1)).streamFinished(broadcastCaptor.capture());
		assertEquals(broadcast.getStreamId(), broadcastCaptor.getValue().getStreamId());

	}

	@Test
	public void testHookAfterDefined() 
	{
		AntMediaApplicationAdapter spyAdaptor = Mockito.spy(adapter);
		AppSettings appSettings = new AppSettings();
		spyAdaptor.setAppSettings(appSettings);

		Broadcast broadcast = new Broadcast();
		assertEquals("", spyAdaptor.getListenerHookURL(broadcast));

		String hookURL = "listener_hook_url";
		appSettings.setListenerHookURL(hookURL);

		assertEquals(hookURL, spyAdaptor.getListenerHookURL(broadcast));

		spyAdaptor = Mockito.spy(adapter);
		Mockito.doNothing().when(spyAdaptor).resetHLSStats(Mockito.anyString());
		Mockito.doNothing().when(spyAdaptor).resetDASHStats(Mockito.anyString());
		appSettings = new AppSettings();
		spyAdaptor.setServerSettings(new ServerSettings());
		spyAdaptor.setAppSettings(appSettings);
		DataStore dataStore = new InMemoryDataStore("testHook");
		DataStoreFactory dsf = Mockito.mock(DataStoreFactory.class);
		Mockito.when(dsf.getDataStore()).thenReturn(dataStore);
		spyAdaptor.setDataStoreFactory(dsf);
		spyAdaptor.setDataStore(dataStore);

		dataStore.save(broadcast);

		String subscriberId = "subscriberId";
		spyAdaptor.startPublish(broadcast.getStreamId(), 0, IAntMediaStreamHandler.PUBLISH_TYPE_RTMP);

		broadcast = dataStore.get(broadcast.getStreamId());
		Mockito.verify(spyAdaptor, Mockito.timeout(2000).times(1)).getListenerHookURL(broadcast);


		spyAdaptor.closeBroadcast(broadcast.getStreamId(), subscriberId, null);
		Mockito.verify(spyAdaptor, Mockito.timeout(2000).times(2)).getListenerHookURL(broadcast);


		spyAdaptor.publishTimeoutError(broadcast.getStreamId(), "");
		Mockito.verify(spyAdaptor, Mockito.timeout(2000).times(3)).getListenerHookURL(broadcast);

		spyAdaptor.incrementEncoderNotOpenedError(broadcast.getStreamId());
		Mockito.verify(spyAdaptor, Mockito.timeout(2000).times(4)).getListenerHookURL(broadcast);

		spyAdaptor.endpointFailedUpdate(broadcast.getStreamId(), "rtmp_url");
		Mockito.verify(spyAdaptor, Mockito.timeout(2000).times(5)).getListenerHookURL(broadcast);

	}


	@Test
	public void testNotifyHookJSON() {
		AntMediaApplicationAdapter spyAdaptor = Mockito.spy(adapter);
		AppSettings appSettings = new AppSettings();
		appSettings.setWebhookRetryCount(2);
		spyAdaptor.setAppSettings(appSettings);

		String webhookUrl = "https://webhook.site/f7056013-4d98-450c-8141-9d792138ead1";

		String streamId = "stream123";

		{
			JSONObject metadata = new JSONObject();
			metadata.put("key1", "value1");

			spyAdaptor.notifyHook(webhookUrl, streamId, null, "action", null, null, null, null, metadata.toString(), null, null);

			ArgumentCaptor<Map> variables = ArgumentCaptor.forClass(Map.class);

			Mockito.verify(spyAdaptor, timeout(5000).times(1)).sendPOST(Mockito.any(), variables.capture(), Mockito.anyInt(), Mockito.anyString());

			assertTrue(variables.getValue().get("metadata") instanceof JSONObject);
		}

		{
			String metadata = "metadata";
			spyAdaptor.notifyHook(webhookUrl, streamId, null, "action", null, null, null, null, metadata, null, null);
			ArgumentCaptor<Map> variables = ArgumentCaptor.forClass(Map.class);

			Mockito.verify(spyAdaptor, timeout(5000).times(2)).sendPOST(Mockito.any(), variables.capture(), Mockito.anyInt(), Mockito.anyString());

			assertTrue(variables.getValue().get("metadata") instanceof String);
		}

	}


	@Test
	public void testNotifyHook() {

		AntMediaApplicationAdapter spyAdaptor = Mockito.spy(adapter);
		AppSettings appSettings = new AppSettings();
		appSettings.setWebhookRetryCount(2);
		spyAdaptor.setAppSettings(appSettings);

		spyAdaptor.notifyHook(null, null, null, null, null, null, null, null, null, null, null);
		Mockito.verify(spyAdaptor, timeout(5000).times(1)).sendPOST(Mockito.any(), Mockito.any(), Mockito.anyInt(), Mockito.anyString());

		spyAdaptor.notifyHook("", null, null, null, null, null, null, null, null, null, null);
		Mockito.verify(spyAdaptor, timeout(5000).times(2)).sendPOST(Mockito.any(), Mockito.any(), Mockito.anyInt(), Mockito.anyString());


		String id = String.valueOf((Math.random() * 10000));
		String action = "any_action";
		String streamName = String.valueOf((Math.random() * 10000));
		String category = "category";

		String vodName = "vod name" + String.valueOf((Math.random() * 10000));
		String vodId = String.valueOf((Math.random() * 10000));

		String url = "this is url";
		spyAdaptor.notifyHook(url, id, null, action, streamName, category, vodName, vodId, null, null, null);
		Mockito.verify(spyAdaptor, timeout(5000).times(3)).sendPOST(Mockito.any(), Mockito.any(), Mockito.anyInt(), Mockito.anyString());

		ArgumentCaptor<String> captureUrl = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<Map> variables = ArgumentCaptor.forClass(Map.class);
		ArgumentCaptor<Integer> retryAttempts = ArgumentCaptor.forClass(Integer.class);
		ArgumentCaptor<String> contentType = ArgumentCaptor.forClass(String.class);

		Mockito.verify(spyAdaptor,  timeout(5000).times(3)).sendPOST(captureUrl.capture(), variables.capture(), retryAttempts.capture(), contentType.capture());
		assertEquals(url, captureUrl.getValue());

		Map<String, String> map = variables.getValue();
		assertEquals(id, map.get("id"));
		assertEquals(action, map.get("action"));
		assertEquals(streamName, map.get("streamName"));
		assertEquals(category, map.get("category"));
		assertEquals(vodName, map.get("vodName"));
		assertEquals(vodId, map.get("vodId"));
		assertNotNull(map.get("timestamp"));

		url = "this is second  url";
		Map<String, String> parameters = new HashMap<>();
		parameters.put("key1", "value1");
		parameters.put("key2", "value2");
		spyAdaptor.notifyHook(url, id, null, null, null, null, null, null, null, null, parameters);


		ArgumentCaptor<String> captureUrl2 = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<Map> variables2 = ArgumentCaptor.forClass(Map.class);

		ArgumentCaptor<Integer> retryAttempts2 = ArgumentCaptor.forClass(Integer.class);

		Mockito.verify(spyAdaptor, timeout(5000).times(4)).sendPOST(captureUrl2.capture(), variables2.capture(), retryAttempts2.capture(), contentType.capture());
		assertEquals(url, captureUrl2.getValue());

		map = variables2.getValue();
		assertEquals(id, map.get("id"));
		assertNull(map.get("action"));
		assertNull(map.get("streamName"));
		assertNull(map.get("category"));
		assertNull(map.get("vodName"));
		assertNull(map.get("vodId"));
		assertNotNull(map.get("timestamp"));
		assertEquals("value1", map.get("key1"));
		assertEquals("value2", map.get("key2"));



	}
	@Test
	public void testNotifyHookErrors(){
		AntMediaApplicationAdapter spyAdaptor = Mockito.spy(adapter);
		AppSettings appSettings = new AppSettings();
		spyAdaptor.setAppSettings(appSettings);

		DataStore dataStore = new InMemoryDataStore("testHook");
		DataStoreFactory dsf = Mockito.mock(DataStoreFactory.class);
		Mockito.when(dsf.getDataStore()).thenReturn(dataStore);
		spyAdaptor.setDataStoreFactory(dsf);

		//get sample mp4 file from test resources
		File anyFile = new File("src/test/resources/sample_MP4_480.mp4");

		//create new broadcast
		Broadcast broadcast = new Broadcast();

		broadcast.setListenerHookURL("listenerHookURL");
		broadcast.setName("name");

		//save this broadcast to db
		String streamId = dataStore.save(broadcast);

		ArgumentCaptor<String> captureUrl = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> captureId = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> captureMainTrackId = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> captureAction = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> captureStreamName = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> captureCategory = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> captureVodName = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> captureVodId = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> captureMetadata = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> subscriberId = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<Map<String, String>> parameters = ArgumentCaptor.forClass(Map.class);



		/*
		 * PUBLISH TIMEOUT ERROR
		 */

		spyAdaptor.publishTimeoutError(broadcast.getStreamId(), "");

		await().atMost(10, TimeUnit.SECONDS).until(()-> {
			boolean called = false;
			try {

				//verify that notifyHook is called 1 time
				verify(spyAdaptor, times(1)).notifyHook(captureUrl.capture(), captureId.capture(), captureMainTrackId.capture(), captureAction.capture(),
						captureStreamName.capture(), captureCategory.capture(),captureVodName.capture(), captureVodId.capture(), captureMetadata.capture(), 
						subscriberId.capture(), parameters.capture());

				assertEquals(captureUrl.getValue(), broadcast.getListenerHookURL());
				assertEquals(captureId.getValue(), broadcast.getStreamId());
				assertEquals(captureAction.getValue(), "publishTimeoutError");

				called = true;
			} catch (Exception e) {
				e.printStackTrace();

			}
			return called;
		});

		/*
		 * ENCODER NOT OPENED ERROR
		 */

		spyAdaptor.incrementEncoderNotOpenedError(broadcast.getStreamId());

		await().atMost(10, TimeUnit.SECONDS).until(()-> {
			boolean called = false;
			try {

				//verify that notifyHook is called 1 time
				verify(spyAdaptor, times(2)).notifyHook(captureUrl.capture(), captureId.capture(), captureMainTrackId.capture(), captureAction.capture(),
						captureStreamName.capture(), captureCategory.capture(),captureVodName.capture(), captureVodId.capture(), 
						captureMetadata.capture(), subscriberId.capture(), parameters.capture());

				assertEquals(captureUrl.getValue(), broadcast.getListenerHookURL());
				assertEquals(captureId.getValue(), broadcast.getStreamId());

				called = true;
			} catch (Exception e) {
				e.printStackTrace();

			}
			return called;
		});

		/*
		 * ENDPOINT FAILED
		 */
		String rtmpUrl = "rtmp://localhost/test/stream123";
		spyAdaptor.endpointFailedUpdate(broadcast.getStreamId(), rtmpUrl);

		await().atMost(10, TimeUnit.SECONDS).until(()-> {
			boolean called = false;
			try {

				//verify that notifyHook is called 1 time
				verify(spyAdaptor, times(3)).notifyHook(captureUrl.capture(), captureId.capture(), captureMainTrackId.capture(), captureAction.capture(),
						captureStreamName.capture(), captureCategory.capture(),captureVodName.capture(), 
						captureVodId.capture(), captureMetadata.capture(), subscriberId.capture(), parameters.capture());

				assertEquals(captureUrl.getValue(), broadcast.getListenerHookURL());
				assertEquals(captureId.getValue(), broadcast.getStreamId());
				JSONObject jsObject = (JSONObject) new JSONParser().parse(captureMetadata.getValue());
				assertTrue(jsObject.containsKey("rtmp-url"));
				assertEquals(rtmpUrl, jsObject.get("rtmp-url"));

				called = true;
			} catch (Exception e) {
				e.printStackTrace();

			}
			return called;
		});

		assertEquals(captureAction.getAllValues().get(3), "publishTimeoutError");
		assertEquals(captureAction.getAllValues().get(4), "encoderNotOpenedError");
		assertEquals(captureAction.getAllValues().get(5), "endpointFailed");
	}

	@Test
	public void testNotifyHookFromMuxingFinished() {

		AntMediaApplicationAdapter spyAdaptor = Mockito.spy(adapter);
		AppSettings appSettings = new AppSettings();
		spyAdaptor.setAppSettings(appSettings);

		DataStore dataStore = new InMemoryDataStore("testHook");
		DataStoreFactory dsf = Mockito.mock(DataStoreFactory.class);
		Mockito.when(dsf.getDataStore()).thenReturn(dataStore);
		spyAdaptor.setDataStoreFactory(dsf);

		//get sample mp4 file from test resources
		File anyFile = new File("src/test/resources/sample_MP4_480.mp4");

		//create new broadcast
		Broadcast broadcast = new Broadcast();

		//save this broadcast to db
		String streamId = dataStore.save(broadcast);

		/*
		 * Scenario 1; Stream is saved to DB, but no Hook URL is defined either for stream and in AppSettings
		 * So, no hook is posted
		 */

		ArgumentCaptor<String> captureUrl = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> captureId = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> captureMainTrackId = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> captureAction = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> captureStreamName = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> captureCategory = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> captureVodName = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> captureVodId = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> captureMetadata = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> subscriberId = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<Map<String, String>> parameters = ArgumentCaptor.forClass(Map.class);




		//call muxingFinished function
		spyAdaptor.muxingFinished(broadcast, broadcast.getStreamId(), anyFile, 0, 100, 480, null, null);

		//verify that notifyHook is never called
		verify(spyAdaptor, never()).notifyHook(captureUrl.capture(), captureId.capture(), captureMainTrackId.capture(), captureAction.capture(),
				captureStreamName.capture(), captureCategory.capture(), captureVodName.capture(), captureVodId.capture(), 
				captureMetadata.capture(), subscriberId.capture(), parameters.capture());


		/*
		 * Scenario 2; hook URL is defined for stream and stream is in DB
		 * So hook is posted
		 */

		//define hook URL for stream specific

		broadcast.setListenerHookURL("listenerHookURL");
		BroadcastUpdate update = new BroadcastUpdate();
		update.setListenerHookURL(broadcast.getListenerHookURL());
		//(Changed due to a bug) In this scenario broadcast name should be irrelevant for the hook to work so setting it to null tests if it is dependent or not.
		broadcast.setName(null);
		update.setName(null);

		//update broadcast
		dataStore.updateBroadcastFields(streamId, update);

		//call muxingFinished function
		spyAdaptor.muxingFinished(broadcast, broadcast.getStreamId(), anyFile, 0, 100, 480, null, null);

		await().atMost(10, TimeUnit.SECONDS).until(()-> {
			boolean called = false;
			try {

				//verify that notifyHook is called 1 time
				verify(spyAdaptor, times(1)).notifyHook(captureUrl.capture(), captureId.capture(), captureMainTrackId.capture(), captureAction.capture(),
						captureStreamName.capture(), captureCategory.capture(), captureVodName.capture(), 
						captureVodId.capture(), captureMetadata.capture(), subscriberId.capture(), parameters.capture());

				assertEquals(captureUrl.getValue(), broadcast.getListenerHookURL());
				assertEquals(captureId.getValue(), broadcast.getStreamId());
				assertEquals(captureVodName.getValue()+".mp4", anyFile.getName());
				assertNull(captureStreamName.capture());

				Map<String,String> value = parameters.getValue();
				assertEquals(100, Integer.parseInt(value.get("duration")));


				called = true;
			}
			catch (Exception e) {
				e.printStackTrace();

			}
			return called;
		});

		/*
		 * Scenario 3; Stream is deleted from DB (zombi stream)
		 * also no HookURL is defined in AppSettins
		 * so no hook is posted
		 */

		//delete broadcast from db
		dataStore.delete(streamId);

		//call muxingFinished function
		spyAdaptor.muxingFinished("streamId", anyFile, 0, 100, 480, null, null);

		await().atMost(10, TimeUnit.SECONDS).until(()-> {
			boolean called = false;
			try {

				//verify that no new notifyHook is called 
				verify(spyAdaptor, times(1)).notifyHook(captureUrl.capture(), captureId.capture(), captureMainTrackId.capture(), captureAction.capture(),
						captureStreamName.capture(), captureCategory.capture(), captureVodName.capture(), captureVodId.capture(), 
						captureMetadata.capture(), subscriberId.capture(), parameters.capture());

				called = true;
			}
			catch (Exception e) {
				e.printStackTrace();

			}
			return called;
		});

		/*
		 * Scenario 4; Stream is deleted from DB (zombi stream)
		 * but HookURL is defined in AppSettins
		 * so new hook is posted
		 */

		//set common hook URL
		appSettings.setListenerHookURL("listenerHookURL");

		//call muxingFinished function
		spyAdaptor.muxingFinished(broadcast, broadcast.getStreamId(), anyFile, 0, 100, 480, null, null);

		await().atMost(10, TimeUnit.SECONDS).until(()-> {
			boolean called = false;
			try {

				//verify that notifyHook is called 2 times
				verify(spyAdaptor, times(2)).notifyHook(captureUrl.capture(), captureId.capture(), captureMainTrackId.capture(), captureAction.capture(),
						captureStreamName.capture(), captureCategory.capture(), captureVodName.capture(), captureVodId.capture(), 
						captureMetadata.capture(), subscriberId.capture(), parameters.capture());

				assertEquals(captureUrl.getValue(), broadcast.getListenerHookURL());
				assertEquals(captureId.getValue(), broadcast.getStreamId());
				assertEquals(captureVodName.getValue()+".mp4", anyFile.getName());

				called = true;
			}
			catch (Exception e) {
				e.printStackTrace();

			}
			return called;
		});

	}


	@Test
	public void testSynchUserVodThrowException() {
		File f = new File(streamsFolderPath);
		assertTrue(f.mkdirs());

		File emptyFile = new File(streamsFolderPath, "emptyfile");
		emptyFile.deleteOnExit();
		try {
			assertTrue(emptyFile.createNewFile());
			boolean synchUserVoDFolder = adapter.deleteSymbolicLink(new File("any_file_not_exist"), f);
			assertFalse(synchUserVoDFolder);

			synchUserVoDFolder = adapter.deleteSymbolicLink(null, f);
			assertFalse(synchUserVoDFolder);


			File oldDir = new File (streamsFolderPath, "dir");
			oldDir.mkdirs();
			Files.deleteIfExists(oldDir.toPath());
			Files.createSymbolicLink(oldDir.toPath(), emptyFile.toPath());
			oldDir.deleteOnExit();

			synchUserVoDFolder = adapter.deleteSymbolicLink(oldDir, f);
			assertTrue(synchUserVoDFolder);

		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testWaitUntilLiveStreamsStopped() {
		IScope scope = mock(IScope.class);
		when(scope.getName()).thenReturn("test");
		adapter.setScope(scope);

		IContext context = mock(IContext.class);
		when(scope.getContext()).thenReturn(context);
		when(context.getBean(AppSettings.BEAN_NAME)).thenReturn(new AppSettings());


		adapter.setServerSettings(Mockito.spy(new ServerSettings()));

		adapter.setDataStore(new InMemoryDataStore("testWaitUntilLiveStreamsStopped"));

		int numberOfCall = (int)(Math.random()*999);

		for (int i=0; i < numberOfCall; i++) 
		{
			Broadcast stream = new Broadcast();
			stream.setUpdateTime(System.currentTimeMillis());
			stream.setStatus(IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING);

			adapter.getDataStore().save(stream);
		}

		assertEquals(numberOfCall, adapter.getDataStore().getLocalLiveBroadcastCount(ServerSettings.getLocalHostAddress()));

		adapter.waitUntilLiveStreamsStopped();


		assertEquals(0, adapter.getDataStore().getLocalLiveBroadcastCount(ServerSettings.getLocalHostAddress()));

	}

	@Test
	public void testShutDown() {
		IScope scope = mock(IScope.class);
		when(scope.getName()).thenReturn("test");
		adapter.setScope(scope);

		IContext context = mock(IContext.class);
		when(scope.getContext()).thenReturn(context);
		when(context.getBean(AppSettings.BEAN_NAME)).thenReturn(new AppSettings());


		adapter.setServerSettings(Mockito.spy(new ServerSettings()));

		DataStore dataStore = mock(DataStore.class);
		DataStoreFactory dataStoreFactory = mock(DataStoreFactory.class);
		when(dataStoreFactory.getDataStore()).thenReturn(dataStore);

		adapter.setDataStoreFactory(dataStoreFactory);

		//Add first broadcast with wrong URL
		Broadcast stream = new Broadcast();
		try {
			stream.setStreamId(String.valueOf((Math.random() * 100000)));
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		stream.setStreamUrl("anyurl");
		dataStore.save(stream);

		//Add second broadcast with correct URL
		Broadcast stream2 = new Broadcast();
		try {
			stream2.setStreamId(String.valueOf((Math.random() * 100000)));
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		stream2.setStreamUrl("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerMeltdowns.mp4");		
		dataStore.save(stream2);

		StreamFetcherManager sfm = new StreamFetcherManager(vertx, dataStore, scope);
		StreamFetcherManager fetcherManager = Mockito.spy(sfm);

		StreamFetcher streamFetcher = mock(StreamFetcher.class);
		StreamFetcher streamFetcher2 = mock(StreamFetcher.class);


		Mockito.doReturn(streamFetcher).when(fetcherManager).make(stream, scope, vertx);
		Mockito.doReturn(streamFetcher2).when(fetcherManager).make(stream2, scope, vertx);


		Map<String, StreamFetcher> sfQueue = new ConcurrentHashMap<>();
		sfQueue.put(stream.getStreamId(), streamFetcher);
		sfQueue.put(stream2.getStreamId(), streamFetcher2);

		fetcherManager.setStreamFetcherList(sfQueue);
		adapter.setStreamFetcherManager(fetcherManager);


		MuxAdaptor muxerAdaptor = mock(MuxAdaptor.class);
		Mockito.when(muxerAdaptor.getStreamId()).thenReturn("stream1");
		adapter.muxAdaptorAdded(muxerAdaptor);

		Broadcast broadcast = mock(Broadcast.class);
		when(broadcast.getType()).thenReturn(AntMediaApplicationAdapter.LIVE_STREAM);
		ClientBroadcastStream cbs = mock(ClientBroadcastStream.class);
		when(muxerAdaptor.getBroadcastStream()).thenReturn(cbs);
		when(muxerAdaptor.getBroadcast()).thenReturn(broadcast);

		when(dataStore.getLocalLiveBroadcastCount(any())).thenReturn(1L);

		new Thread() {
			public void run() {
				try {
					sleep(3000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				when(dataStore.getLocalLiveBroadcastCount(any())).thenReturn(0L);
			};
		}.start();

		assertEquals(2, fetcherManager.getStreamFetcherList().size());
		assertEquals(2, sfQueue.size());


		assertFalse(adapter.isServerShuttingDown());

		adapter.serverShuttingdown();

		assertTrue(adapter.isServerShuttingDown());

		verify(streamFetcher, times(1)).stopStream();
		verify(streamFetcher2, times(1)).stopStream();

		assertEquals(0, fetcherManager.getStreamFetcherList().size());
		assertEquals(0, sfQueue.size());

		verify(cbs, times(1)).stop();
		verify(muxerAdaptor, times(1)).stop(true);
	}

	@Test
	public void testCloseStreamFetchers() {

		Map<String, StreamFetcher> streamFetcherList= new ConcurrentHashMap<>();

		StreamFetcher streamFetcher = mock(StreamFetcher.class);
		StreamFetcher streamFetcher2 = mock(StreamFetcher.class);
		StreamFetcher streamFetcher3 = mock(StreamFetcher.class);
		StreamFetcher streamFetcher4 = mock(StreamFetcher.class);

		streamFetcherList.put("streamFetcher", streamFetcher);
		streamFetcherList.put("streamFetcher2", streamFetcher2);
		streamFetcherList.put("streamFetcher3", streamFetcher3);
		streamFetcherList.put("streamFetcher4", streamFetcher4);

		StreamFetcherManager fetcherManager = mock(StreamFetcherManager.class);
		when(fetcherManager.getStreamFetcherList()).thenReturn(streamFetcherList);
		adapter.setStreamFetcherManager(fetcherManager);

		assertEquals(4, streamFetcherList.size());

		adapter.closeStreamFetchers();

		assertEquals(0, streamFetcherList.size());
	}

	@Test
	public void testEncoderBlocked() {
		DataStore dataStore = mock(DataStore.class);
		DataStoreFactory dataStoreFactory = mock(DataStoreFactory.class);
		when(dataStoreFactory.getDataStore()).thenReturn(dataStore);

		adapter.setDataStoreFactory(dataStoreFactory);

		assertEquals(0, adapter.getNumberOfEncodersBlocked());
		assertEquals(0, adapter.getNumberOfEncoderNotOpenedErrors());

		adapter.incrementEncoderNotOpenedError("");
		adapter.incrementEncoderNotOpenedError("");
		adapter.incrementEncoderNotOpenedError("");

		assertEquals(3, adapter.getNumberOfEncoderNotOpenedErrors());
	}

	@Test
	public void testPublishTimeout() {
		assertEquals(0, adapter.getNumberOfPublishTimeoutError());

		DataStore dataStore = mock(DataStore.class);
		DataStoreFactory dataStoreFactory = mock(DataStoreFactory.class);
		when(dataStoreFactory.getDataStore()).thenReturn(dataStore);

		adapter.setDataStoreFactory(dataStoreFactory);

		adapter.publishTimeoutError("streamId", "");

		assertEquals(1, adapter.getNumberOfPublishTimeoutError());
	}

	@Test
	public void testStats() {
		WebRTCVideoReceiveStats receiveStats = new WebRTCVideoReceiveStats();
		assertNotNull(receiveStats.getVideoBytesReceivedPerSecond());
		assertEquals(BigInteger.ZERO, receiveStats.getVideoBytesReceivedPerSecond());

		assertNotNull(receiveStats.getVideoBytesReceived());
		assertEquals(BigInteger.ZERO, receiveStats.getVideoBytesReceived());

		WebRTCAudioReceiveStats audioReceiveStats = new WebRTCAudioReceiveStats();
		assertNotNull(audioReceiveStats.getAudioBytesReceivedPerSecond());
		assertEquals(BigInteger.ZERO, audioReceiveStats.getAudioBytesReceivedPerSecond());


		assertNotNull(audioReceiveStats.getAudioBytesReceived());
		assertEquals(BigInteger.ZERO, audioReceiveStats.getAudioBytesReceived());


		WebRTCVideoSendStats videoSendStats = new WebRTCVideoSendStats();
		assertNotNull(videoSendStats.getVideoBytesSentPerSecond());
		assertEquals(BigInteger.ZERO, videoSendStats.getVideoBytesSentPerSecond());

		assertNotNull(videoSendStats.getVideoBytesSent());
		assertEquals(BigInteger.ZERO, videoSendStats.getVideoBytesSent());


		WebRTCAudioSendStats audioSendStats = new WebRTCAudioSendStats();
		assertEquals(BigInteger.ZERO, audioSendStats.getAudioBytesSent());
		assertEquals(BigInteger.ZERO, audioSendStats.getAudioBytesSentPerSecond());


	}

	@Test
	public void testEncoderBlockedList() {

		assertEquals(0, adapter.getNumberOfEncodersBlocked());

		adapter.encoderBlocked("stream1", false);

		assertEquals(0, adapter.getNumberOfEncodersBlocked());

		adapter.encoderBlocked("stream1", true);

		assertEquals(1, adapter.getNumberOfEncodersBlocked());

		adapter.encoderBlocked("stream2", true);
		adapter.encoderBlocked("stream3", true);

		assertEquals(3, adapter.getNumberOfEncodersBlocked());

		adapter.encoderBlocked("stream2", false);
		adapter.encoderBlocked("stream3", false);
		adapter.encoderBlocked("stream1", false);

		assertEquals(0, adapter.getNumberOfEncodersBlocked());
	}


	@Test
	public void testCreateShutdownFile() {

		IScope scope = mock(IScope.class);
		when(scope.getName()).thenReturn("junit");

		String closedFilePath = "webapps/"+scope.getName()+"/.closed";
		File closedFile = new File(closedFilePath);

		// First stop
		adapter.createShutdownFile(scope.getName());

		assertEquals(true, closedFile.exists());

		adapter.createShutdownFile(scope.getName());

	}

	@Test
	public void testCloseBroadcast() {

		DataStore db = new InMemoryDataStore("db");
		Broadcast broadcast = new Broadcast();
		broadcast.setListenerHookURL("url");
		broadcast.setWebRTCViewerCount(10);
		broadcast.setDashViewerCount(11);
		broadcast.setHlsViewerCount(12);
		db.save(broadcast);

		Vertx vertx = Mockito.mock(VertxImpl.class);
		adapter.setDataStore(db);

		IScope scope = mock(IScope.class);
		when(scope.getName()).thenReturn("junit");
		IContext context = Mockito.mock(IContext.class);
		when(context.getApplicationContext()).thenReturn(Mockito.mock(org.springframework.context.ApplicationContext.class));
		when(scope.getContext()).thenReturn(context);

		adapter.setScope(scope);
		adapter.setVertx(vertx);

		adapter.closeBroadcast(broadcast.getStreamId());

		broadcast = db.get(broadcast.getStreamId());
		assertEquals(AntMediaApplicationAdapter.BROADCAST_STATUS_FINISHED, broadcast.getStatus());
		assertEquals(0, broadcast.getWebRTCViewerCount());
		assertEquals(0, broadcast.getHlsViewerCount());
		assertEquals(0, broadcast.getDashViewerCount());

		broadcast = new Broadcast();
		broadcast.setOriginAdress("testing");
		db.save(broadcast);


	}



	@SuppressWarnings("java:S1874")
	@Test
	public void testCloseBroadcastOverload() {

		DataStore db = new InMemoryDataStore("db");
		Broadcast broadcast = new Broadcast();
		broadcast.setListenerHookURL("url");
		broadcast.setWebRTCViewerCount(10);
		broadcast.setDashViewerCount(11);
		broadcast.setHlsViewerCount(12);
		db.save(broadcast);

		Vertx vertxLocal = Mockito.mock(VertxImpl.class);
		adapter.setDataStore(db);
		adapter.setAppSettings(new AppSettings());

		IScope scope = mock(IScope.class);
		when(scope.getName()).thenReturn("junit");
		IContext context = Mockito.mock(IContext.class);
		when(context.getApplicationContext()).thenReturn(Mockito.mock(org.springframework.context.ApplicationContext.class));
		when(scope.getContext()).thenReturn(context);

		adapter.setScope(scope);
		adapter.setVertx(vertxLocal);

		adapter.closeBroadcast(broadcast.getStreamId(), "subscriberId");

		broadcast = db.get(broadcast.getStreamId());
		assertEquals(AntMediaApplicationAdapter.BROADCAST_STATUS_FINISHED, broadcast.getStatus());
		assertEquals(0, broadcast.getWebRTCViewerCount());
		assertEquals(0, broadcast.getHlsViewerCount());
		assertEquals(0, broadcast.getDashViewerCount());

	}

	@Test
	public void testiSSubscriberIdMatching() {
		AntMediaApplicationAdapter spyAdaptor = Mockito.spy(adapter);
		ClientBroadcastStream clientBroadcastStream = Mockito.mock(ClientBroadcastStream.class);
		boolean result = spyAdaptor.isSubscriberIdMatching("subscriberId", null);
		assertFalse(result);

		result = spyAdaptor.isSubscriberIdMatching("sub1", "sub1");
		assertTrue(result);

	}


	@Test
	public void testInitializationFile() {

		IScope scope = mock(IScope.class);
		when(scope.getName()).thenReturn("junit");

		String initializedFilePath = "webapps/"+scope.getName()+"/.initialized";
		File initializedFile = new File(initializedFilePath);

		String closedFilePath = "webapps/"+scope.getName()+"/.closed";
		File closedFile = new File(closedFilePath);

		Result result = new Result(false); 

		// After the upgrade First initialization
		//initialization file not exist
		//closed file not exist
		result = adapter.createInitializationProcess(scope.getName());

		assertEquals(false, closedFile.exists());
		assertEquals(true, initializedFile.exists());
		assertEquals(true, result.isSuccess());
		assertEquals("Initialized file created in "+ scope.getName(), result.getMessage());


		//After the upgrade repeated initialization
		//initialization file exist
		//closed file not exist
		try {
			initializedFile.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
		}
		result = adapter.createInitializationProcess(scope.getName());

		assertEquals(false, closedFile.exists());
		assertEquals(true, initializedFile.exists());
		assertEquals(false, result.isSuccess());
		assertEquals("Something wrong in "+ scope.getName(), result.getMessage());


		//After the upgrade repeated initialization
		//initialization file exist
		//closed file exist

		try {
			initializedFile.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			closedFile.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
		}
		result = adapter.createInitializationProcess(scope.getName());

		assertEquals(false, closedFile.exists());
		assertEquals(true, initializedFile.exists());
		assertEquals(true, result.isSuccess());
		assertEquals("System works, deleted closed file in "+ scope.getName(), result.getMessage());


		//initiiazed file does not exist but closed file exists
		initializedFile.delete();


		try {
			closedFile.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
		}
		result = adapter.createInitializationProcess(scope.getName());

		assertEquals(false, closedFile.exists());
		assertEquals(true, initializedFile.exists());
		assertEquals(true, result.isSuccess());



		//run create initialization file for odd case
		result = new Result(true);
		initializedFile = Mockito.mock(File.class);
		try {
			Mockito.when(initializedFile.createNewFile()).thenReturn(false);
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			adapter.createInitializationFile("app", result, initializedFile);
			assertFalse(result.isSuccess());
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}



	}

	boolean threadStarted = false; 
	@Test
	public void testVertexThreadWait() {

		Vertx tempVertx = Vertx.vertx(new VertxOptions()
				.setMetricsOptions(new DropwizardMetricsOptions().setEnabled(true)));
		AntMediaApplicationAdapter antMediaApplicationAdapter = new AntMediaApplicationAdapter();
		antMediaApplicationAdapter.setVertx(tempVertx);
		IScope scope = mock(IScope.class);
		when(scope.getName()).thenReturn("junit");

		antMediaApplicationAdapter.setScope(scope);
		int sleepTime = 5000;

		tempVertx.executeBlocking(l->{
			try {
				threadStarted = true;
				Thread.sleep(sleepTime);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}, r->{});


		await().atMost(10, TimeUnit.SECONDS).until(() -> threadStarted);

		long t0 = System.currentTimeMillis();
		antMediaApplicationAdapter.waitUntilThreadsStop();
		long t1 = System.currentTimeMillis();
		long dt = t1 - t0;
		assertTrue(Math.abs(dt - sleepTime) < 100);

		tempVertx.close();
	}

	@Test
	public void testStreamFetcherStartAutomatically()
	{
		IScope scope = mock(IScope.class);
		when(scope.getName()).thenReturn("junit");

		DataStore dataStore = new InMemoryDataStore("dbname");
		DataStoreFactory dsf = Mockito.mock(DataStoreFactory.class);
		Mockito.when(dsf.getDataStore()).thenReturn(dataStore);

		AntMediaApplicationAdapter spyAdapter = Mockito.spy(adapter);
		IContext context = mock(IContext.class);
		when(context.getBean(spyAdapter.VERTX_BEAN_NAME)).thenReturn(vertx);
		StorageClient storageClient = Mockito.mock(StorageClient.class);
		when(context.getBean(StorageClient.BEAN_NAME)).thenReturn(storageClient);

		when(scope.getContext()).thenReturn(context);
		spyAdapter.setDataStoreFactory(dsf);

		Mockito.doReturn(dataStore).when(spyAdapter).getDataStore();
		spyAdapter.setScope(scope);


		Broadcast broadcast = new Broadcast();
		broadcast.setType(AntMediaApplicationAdapter.STREAM_SOURCE);
		dataStore.save(broadcast);

		Result result = new Result(false);
		Mockito.when(spyAdapter.createInitializationProcess(Mockito.anyString())).thenReturn(result);
		//When createInitializationProcess(scope.getName());

		StreamFetcherManager streamFetcherManager = mock(StreamFetcherManager.class);

		spyAdapter.setStreamFetcherManager(streamFetcherManager);
		AppSettings settings = new AppSettings();
		settings.setStartStreamFetcherAutomatically(true);
		spyAdapter.setAppSettings(settings);
		spyAdapter.setServerSettings(new ServerSettings());
		spyAdapter.setStreamPublishSecurityList(new ArrayList<>());
		spyAdapter.setStreamPlaySecurityList(new ArrayList<>());

		spyAdapter.appStart(scope);

		await().pollInterval(2,TimeUnit.SECONDS).atMost(3, TimeUnit.SECONDS).until(()-> true);

		ArgumentCaptor<Broadcast> broadcastListCaptor = ArgumentCaptor.forClass(Broadcast.class);
		verify(streamFetcherManager, times(1)).startStreaming(broadcastListCaptor.capture(), anyBoolean());

		broadcast = dataStore.get(broadcast.getStreamId());
		assertNotNull(broadcastListCaptor.getValue());
		assertEquals(broadcast.getStreamId(),  broadcastListCaptor.getValue().getStreamId());
		assertEquals(broadcast.getStatus(),  broadcastListCaptor.getValue().getStatus());
	}

	@Test
	public void testStartStreaming() {
		IScope scope = mock(IScope.class);
		IStatsCollector statsCollector = mock(IStatsCollector.class);

		when(scope.getName()).thenReturn("junit");

		DataStore dataStore = new InMemoryDataStore("dbname");
		DataStoreFactory dsf = Mockito.mock(DataStoreFactory.class);
		Mockito.when(dsf.getDataStore()).thenReturn(dataStore);

		AntMediaApplicationAdapter spyAdapter = Mockito.spy(adapter);
		IContext context = mock(IContext.class);
		when(scope.getContext()).thenReturn(context);
		when(context.getBean(spyAdapter.VERTX_BEAN_NAME)).thenReturn(vertx);


		when(context.getBean(IDataStoreFactory.BEAN_NAME)).thenReturn(dsf);


		ApplicationContext appContext = Mockito.mock(ApplicationContext.class);
		when(context.getApplicationContext()).thenReturn(appContext);
		when(context.getResource(Mockito.anyString())).thenReturn(Mockito.mock(org.springframework.core.io.Resource.class));

		AntMediaApplicationAdapter appAdaptor = Mockito.mock(AntMediaApplicationAdapter.class);
		spyAdapter.setServerSettings(new ServerSettings());
		spyAdapter.setAppSettings(new AppSettings());
		spyAdapter.setDataStore(dataStore);

		when(appContext.getBean(AntMediaApplicationAdapter.BEAN_NAME)).thenReturn(appAdaptor);

		when(appContext.containsBean(AppSettings.BEAN_NAME)).thenReturn(true);
		when(appContext.containsBean(IAntMediaStreamHandler.VERTX_BEAN_NAME)).thenReturn(true);
		when(appContext.getBean(IAntMediaStreamHandler.VERTX_BEAN_NAME)).thenReturn(vertx);
		when(appContext.getBean(AppSettings.BEAN_NAME)).thenReturn(new AppSettings());

		when(scope.getContext()).thenReturn(context);
		spyAdapter.setDataStoreFactory(dsf);

		Mockito.doReturn(dataStore).when(spyAdapter).getDataStore();
		spyAdapter.setScope(scope);

		ILicenceService licenseService = Mockito.mock(ILicenceService.class);
		Mockito.when(context.getBean(ILicenceService.BeanName.LICENCE_SERVICE.toString())).thenReturn(licenseService);
		when(licenseService.isLicenceSuspended()).thenReturn(false);

		when(appContext.getBean(StatsCollector.BEAN_NAME)).thenReturn(statsCollector);
		when(statsCollector.enoughResource()).thenReturn(true);


		Broadcast broadcast = new Broadcast();
		broadcast.setType(AntMediaApplicationAdapter.STREAM_SOURCE);
		broadcast.setStreamUrl("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerMeltdowns.mp4");
		dataStore.save(broadcast);

		boolean startStreaming = spyAdapter.startStreaming(broadcast).isSuccess();
		assertTrue(startStreaming);
		assertTrue(spyAdapter.getStreamFetcherManager().isStreamRunning(broadcast));

		StreamFetcher streamFetcher = spyAdapter.getStreamFetcherManager().getStreamFetcher(broadcast.getStreamId());
		await().atMost(5, TimeUnit.SECONDS).until(() -> streamFetcher.isThreadActive());

		spyAdapter.getStreamFetcherManager().stopStreaming(broadcast.getStreamId(), false);
		await().atMost(5, TimeUnit.SECONDS).until(() -> !streamFetcher.isThreadActive());



		when(licenseService.isLicenceSuspended()).thenReturn(true);
		startStreaming = spyAdapter.startStreaming(broadcast).isSuccess();
		assertFalse(startStreaming);

	}

	@Test
	public void testStartStreamingForwardToOrigin() throws Exception {
		IScope scope = mock(IScope.class);
		IStatsCollector statsCollector = mock(IStatsCollector.class);
		when(scope.getName()).thenReturn("junit");

		DataStore dataStore = new InMemoryDataStore("dbname");
		DataStoreFactory dsf = Mockito.mock(DataStoreFactory.class);
		Mockito.when(dsf.getDataStore()).thenReturn(dataStore);

		AntMediaApplicationAdapter spyAdapter = Mockito.spy(adapter);
		IContext context = mock(IContext.class);
		when(context.getBean(IAntMediaStreamHandler.VERTX_BEAN_NAME)).thenReturn(vertx);
		when(context.getBean(IDataStoreFactory.BEAN_NAME)).thenReturn(dsf);

		ApplicationContext appContext = Mockito.mock(ApplicationContext.class);
		when(context.getApplicationContext()).thenReturn(appContext);
		when(context.getResource(Mockito.anyString())).thenReturn(Mockito.mock(org.springframework.core.io.Resource.class));

		AntMediaApplicationAdapter appAdaptor = Mockito.mock(AntMediaApplicationAdapter.class);
		ServerSettings serverSettings = Mockito.mock(ServerSettings.class);
		when(spyAdapter.getScope()).thenReturn(scope);
		when(scope.getContext()).thenReturn(context);
		when(spyAdapter.getServerSettings()).thenReturn(serverSettings);

		AppSettings appSettings = new AppSettings();
		appSettings.setAppName("WebRTCAppEE");
		appSettings.setWebhookRetryDelay(1000);
		appSettings.setClusterCommunicationKey("test-key");
		spyAdapter.setAppSettings(appSettings);
		spyAdapter.setDataStore(dataStore);

		when(appContext.getBean(AntMediaApplicationAdapter.BEAN_NAME)).thenReturn(appAdaptor);
		when(appContext.containsBean(AppSettings.BEAN_NAME)).thenReturn(true);
		when(appContext.containsBean(IAntMediaStreamHandler.VERTX_BEAN_NAME)).thenReturn(true);
		when(appContext.getBean(IAntMediaStreamHandler.VERTX_BEAN_NAME)).thenReturn(vertx);
		when(appContext.getBean(AppSettings.BEAN_NAME)).thenReturn(appSettings);
		when(scope.getContext()).thenReturn(context);

		spyAdapter.setDataStoreFactory(dsf);
		Mockito.doReturn(dataStore).when(spyAdapter).getDataStore();
		spyAdapter.setScope(scope);

		ILicenceService licenseService = Mockito.mock(ILicenceService.class);
		Mockito.when(context.getBean(ILicenceService.BeanName.LICENCE_SERVICE.toString())).thenReturn(licenseService);
		when(licenseService.isLicenceSuspended()).thenReturn(false);

		when(appContext.getBean(StatsCollector.BEAN_NAME)).thenReturn(statsCollector);
		when(statsCollector.enoughResource()).thenReturn(true);

		StreamFetcherManager streamFetcherManager = mock(StreamFetcherManager.class);
		when(spyAdapter.getStreamFetcherManager()).thenReturn(streamFetcherManager);
		when(spyAdapter.isClusterMode()).thenReturn(true);
		when(serverSettings.getDefaultHttpPort()).thenReturn(5080);

		// Test Case 1: Null origin address in cluster mode
		Broadcast broadcast1 = new Broadcast();
		broadcast1.setType(AntMediaApplicationAdapter.STREAM_SOURCE);
		broadcast1.setStreamUrl("rtsp");
		broadcast1.setOriginAdress(null);
		dataStore.save(broadcast1);

		when(serverSettings.getHostAddress()).thenReturn("1.1.1.2");
		when(streamFetcherManager.startStreaming(broadcast1)).thenReturn(new Result(true));

		Result result1 = spyAdapter.startStreaming(broadcast1);
		assertTrue(result1.isSuccess());
		assertEquals("Broadcasts origin address is not set. 1.1.1.2 will fetch the stream.", result1.getMessage());
		verify(streamFetcherManager, times(1)).startStreaming(broadcast1);

		// Test Case 2: Local server streaming (same origin address)
		Broadcast broadcast2 = new Broadcast();
		broadcast2.setType(AntMediaApplicationAdapter.STREAM_SOURCE);
		broadcast2.setOriginAdress("1.1.1.2");
		broadcast2.setStreamUrl("rtsp");
		dataStore.save(broadcast2);

		when(streamFetcherManager.startStreaming(broadcast2)).thenReturn(new Result(true));

		Result result2 = spyAdapter.startStreaming(broadcast2);
		assertTrue(result2.isSuccess());
		verify(streamFetcherManager, times(1)).startStreaming(broadcast2);

		// Test Case 3: Forward to different origin
		Broadcast broadcast3 = new Broadcast();
		broadcast3.setType(AntMediaApplicationAdapter.STREAM_SOURCE);
		broadcast3.setOriginAdress("1.1.1.3");
		broadcast3.setStreamUrl("rtsp");
		broadcast3.setStreamId("test-stream-id");
		dataStore.save(broadcast3);

		when(serverSettings.getHostAddress()).thenReturn("1.1.1.2");

		String expectedRestRoute = "http://1.1.1.3:5080/WebRTCAppEE/rest/v2/broadcasts/test-stream-id/start";
		Mockito.doReturn(true).when(spyAdapter).sendClusterPost(
				eq(expectedRestRoute),
				anyString(), 
				any()
				);

		Result result3 = spyAdapter.startStreaming(broadcast3);
		assertTrue(result3.isSuccess());
		assertEquals("Request forwarded to origin server for fetching. Check broadcast status for final confirmation.",
				result3.getMessage());

		// Test Case 4: Forward fails, fallback to local streaming
		Broadcast broadcast4 = new Broadcast();
		broadcast4.setType(AntMediaApplicationAdapter.STREAM_SOURCE);
		broadcast4.setOriginAdress("1.1.1.4");
		broadcast4.setStreamUrl("rtsp");
		broadcast4.setStreamId("test-stream-id-2");
		dataStore.save(broadcast4);

		CompletableFuture<Boolean> failureFuture = CompletableFuture.completedFuture(false);
		String expectedRestRoute2 = "http://1.1.1.4:5080/WebRTCAppEE/rest/v2/broadcasts/test-stream-id-2/start";
		Mockito.doReturn(false).when(spyAdapter).sendClusterPost(
				eq(expectedRestRoute2),
				anyString(),
				any()
				);
		when(streamFetcherManager.startStreaming(broadcast4)).thenReturn(new Result(true));

		Result result4 = spyAdapter.startStreaming(broadcast4);
		assertTrue(result4.isSuccess());

		// Verify the async callback executed
		await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
			verify(streamFetcherManager, times(1)).startStreaming(broadcast4);
		});

		// Test Case 5: Non-cluster mode streaming
		when(spyAdapter.isClusterMode()).thenReturn(false);
		Broadcast broadcast5 = new Broadcast();
		broadcast5.setType(AntMediaApplicationAdapter.STREAM_SOURCE);
		broadcast5.setStreamUrl("https://example.com/stream5.mp4");
		dataStore.save(broadcast5);

		when(streamFetcherManager.startStreaming(broadcast5)).thenReturn(new Result(true));

		Result result5 = spyAdapter.startStreaming(broadcast5);
		assertTrue(result5.isSuccess());
		verify(streamFetcherManager, times(1)).startStreaming(broadcast5);
	}

	@Test
	public void testForwardStartStreaming() throws Exception {
		IScope scope = mock(IScope.class);
		when(scope.getName()).thenReturn("junit");

		DataStore dataStore = new InMemoryDataStore("dbname");
		DataStoreFactory dsf = Mockito.mock(DataStoreFactory.class);
		Mockito.when(dsf.getDataStore()).thenReturn(dataStore);

		AntMediaApplicationAdapter spyAdapter = Mockito.spy(adapter);
		IContext context = mock(IContext.class);
		when(context.getBean(spyAdapter.VERTX_BEAN_NAME)).thenReturn(vertx);
		when(context.getBean(IDataStoreFactory.BEAN_NAME)).thenReturn(dsf);

		ApplicationContext appContext = Mockito.mock(ApplicationContext.class);
		when(context.getApplicationContext()).thenReturn(appContext);
		when(context.getResource(Mockito.anyString())).thenReturn(Mockito.mock(org.springframework.core.io.Resource.class));

		ServerSettings serverSettings = Mockito.mock(ServerSettings.class);
		when(spyAdapter.getScope()).thenReturn(scope);
		when(scope.getContext()).thenReturn(context);
		when(spyAdapter.getServerSettings()).thenReturn(serverSettings);

		AppSettings appSettings = new AppSettings();
		appSettings.setAppName("WebRTCAppEE");
		appSettings.setWebhookRetryDelay(100); // Small delay for testing
		appSettings.setClusterCommunicationKey("test-key");
		spyAdapter.setAppSettings(appSettings);
		spyAdapter.setDataStore(dataStore);

		when(serverSettings.getDefaultHttpPort()).thenReturn(5080);
		StreamFetcherManager streamFetcherManager = mock(StreamFetcherManager.class);

		when(spyAdapter.getStreamFetcherManager()).thenReturn(streamFetcherManager);

		// Test Case 1: Successful forward
		Broadcast broadcast1 = new Broadcast();
		broadcast1.setOriginAdress("127.0.0.1");
		broadcast1.setStreamId("test-stream-1");

		String expectedRestRoute1 = "http://127.0.0.1:5080" +
				File.separator + appSettings.getAppName() + File.separator + "rest" +
				File.separator + "v2" + File.separator + "broadcasts" +
				File.separator + broadcast1.getStreamId() + File.separator + "start";

		Mockito.doReturn(true).when(spyAdapter).sendClusterPost(
				eq(expectedRestRoute1),
				anyString(),
				any()
				);

		spyAdapter.forwardStartStreaming(broadcast1);

		// Verify successful case
		await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> {
			verify(spyAdapter, times(1)).sendClusterPost(
					eq(expectedRestRoute1),
					anyString(),
					any()
					);
			verify(streamFetcherManager, never()).startStreaming(broadcast1);
		});

		// Test Case 2: Failed forward with retry
		Broadcast broadcast2 = new Broadcast();
		broadcast2.setOriginAdress("127.0.0.2");
		broadcast2.setStreamId("test-stream-2");

		String expectedRestRoute2 = "http://127.0.0.2:5080" +
				File.separator + appSettings.getAppName() + File.separator + "rest" +
				File.separator + "v2" + File.separator + "broadcasts" +
				File.separator + broadcast2.getStreamId() + File.separator + "start";

		Mockito.doReturn(false).when(spyAdapter).sendClusterPost(
				eq(expectedRestRoute2),
				anyString(),
				any()
				);
		when(streamFetcherManager.startStreaming(broadcast2)).thenReturn(new Result(true));

		spyAdapter.forwardStartStreaming(broadcast2);

		// Verify failure case with local fallback
		await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> {
			verify(spyAdapter, times(4)).sendClusterPost(
					eq(expectedRestRoute2),
					anyString(),
					any()
					);
			verify(streamFetcherManager, times(1)).startStreaming(broadcast2);
		});

		// Test Case 3: Exception during execution
		Broadcast broadcast3 = new Broadcast();
		broadcast3.setOriginAdress("127.0.0.3");
		broadcast3.setStreamId("test-stream-3");

		String expectedRestRoute3 = "http://127.0.0.3:5080" +
				File.separator + appSettings.getAppName() + File.separator + "rest" +
				File.separator + "v2" + File.separator + "broadcasts" +
				File.separator + broadcast3.getStreamId() + File.separator + "start";

		Mockito.doReturn(false).when(spyAdapter).sendClusterPost(
				eq(expectedRestRoute3),
				anyString(),
				any()
				);
		when(streamFetcherManager.startStreaming(broadcast3)).thenReturn(new Result(true));

		spyAdapter.forwardStartStreaming(broadcast3);

		// Verify exception case with local fallback
		await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> {
			verify(spyAdapter, times(4)).sendClusterPost(
					eq(expectedRestRoute3),
					anyString(),
					any()
					);
			verify(streamFetcherManager, times(1)).startStreaming(broadcast3);
		});

		// Test Case 4: Verify JWT token format
		ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
		verify(spyAdapter, atLeast(3)).sendClusterPost(anyString(), tokenCaptor.capture(), any());

		for (String capturedToken : tokenCaptor.getAllValues()) {
			assertNotNull(capturedToken);
			assertFalse(capturedToken.isEmpty());
		}
	}

	@Test
	public void testOctetStreamSendClusterPost() throws ClientProtocolException, IOException {
		IScope scope = mock(IScope.class);
		when(scope.getName()).thenReturn("junit");

		AntMediaApplicationAdapter spyAdapter = Mockito.spy(adapter);
		IContext context = mock(IContext.class);
		when(context.getBean(IAntMediaStreamHandler.VERTX_BEAN_NAME)).thenReturn(vertx);

		ApplicationContext appContext = Mockito.mock(ApplicationContext.class);
		when(context.getApplicationContext()).thenReturn(appContext);
		when(appContext.containsBean(IAntMediaStreamHandler.VERTX_BEAN_NAME)).thenReturn(true);
		when(appContext.getBean(IAntMediaStreamHandler.VERTX_BEAN_NAME)).thenReturn(vertx);

		CloseableHttpClient httpClient = mock(CloseableHttpClient.class);

		CloseableHttpResponse httpResponse = mock(CloseableHttpResponse.class);
		StatusLine statusLine = mock(StatusLine.class);
		when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_OK);
		when(httpResponse.getStatusLine()).thenReturn(statusLine);

		Mockito.doReturn(httpClient).when(spyAdapter).getHttpClient();
		when(httpClient.execute(any(HttpPost.class)))
		.thenReturn(httpResponse);

		String testUrl = "http://localhost:5080/test";
		String testToken = "test-token";

		boolean result = spyAdapter.sendClusterPost(testUrl, testToken, testUrl.getBytes());

		ArgumentCaptor<HttpPost> httpPostCaptor = ArgumentCaptor.forClass(HttpPost.class);

		verify(httpClient, times(1)).execute(httpPostCaptor.capture());

		HttpPost value = httpPostCaptor.getValue();

		assertEquals(testUrl, value.getURI().toString());
		assertEquals(testToken, value.getFirstHeader(TokenFilterManager.TOKEN_HEADER_FOR_NODE_COMMUNICATION).getValue());
		assertEquals("application/octet-stream", value.getFirstHeader("Content-Type").getValue());
		ByteArrayEntity entity = (ByteArrayEntity) value.getEntity();

		assertEquals(entity.getContent().readAllBytes().length, testUrl.getBytes().length);
	}

	@Test
	public void testSendClusterPost() throws Exception {
		IScope scope = mock(IScope.class);
		when(scope.getName()).thenReturn("junit");

		AntMediaApplicationAdapter spyAdapter = Mockito.spy(adapter);
		IContext context = mock(IContext.class);
		when(context.getBean(IAntMediaStreamHandler.VERTX_BEAN_NAME)).thenReturn(vertx);

		ApplicationContext appContext = Mockito.mock(ApplicationContext.class);
		when(context.getApplicationContext()).thenReturn(appContext);
		when(appContext.containsBean(IAntMediaStreamHandler.VERTX_BEAN_NAME)).thenReturn(true);
		when(appContext.getBean(IAntMediaStreamHandler.VERTX_BEAN_NAME)).thenReturn(vertx);

		AppSettings appSettings = new AppSettings();
		appSettings.setWebhookRetryDelay(100); // Small delay for testing
		spyAdapter.setAppSettings(appSettings);

		CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
		Mockito.doReturn(httpClient).when(spyAdapter).getHttpClient();

		String testUrl = "http://localhost:5080/test";
		String testToken = "test-token";

		// Mock responses
		CloseableHttpResponse successResponse = mock(CloseableHttpResponse.class);
		StatusLine successStatusLine = mock(StatusLine.class);
		when(successStatusLine.getStatusCode()).thenReturn(HttpStatus.SC_OK);
		when(successResponse.getStatusLine()).thenReturn(successStatusLine);

		CloseableHttpResponse failResponse = mock(CloseableHttpResponse.class);
		StatusLine failStatusLine = mock(StatusLine.class);
		when(failStatusLine.getStatusCode()).thenReturn(HttpStatus.SC_INTERNAL_SERVER_ERROR);
		when(failResponse.getStatusLine()).thenReturn(failStatusLine);

		// First request fails, second succeeds
		when(httpClient.execute(any(HttpPost.class)))
		.thenReturn(failResponse)
		.thenReturn(successResponse);

		// Test asynchronous behavior
		long startTime = System.currentTimeMillis();
		boolean result = spyAdapter.sendClusterPost(testUrl, testToken, null);

		long endTime = System.currentTimeMillis();
		// Wait for the CompletableFuture to complete
		assertTrue((endTime - startTime) < AntMediaApplicationAdapter.CLUSTER_POST_TIMEOUT_MS);

		// Verify request configuration
		ArgumentCaptor<HttpPost> httpPostCaptor = ArgumentCaptor.forClass(HttpPost.class);
		verify(httpClient, times(1)).execute(httpPostCaptor.capture());

		List<HttpPost> capturedPosts = httpPostCaptor.getAllValues();
		assertEquals(testUrl, capturedPosts.get(0).getURI().toString());
		assertEquals(testToken, capturedPosts.get(0)
				.getFirstHeader(TokenFilterManager.TOKEN_HEADER_FOR_NODE_COMMUNICATION).getValue());

		RequestConfig capturedConfig = capturedPosts.get(0).getConfig();
		assertEquals(AntMediaApplicationAdapter.CLUSTER_POST_TIMEOUT_MS, capturedConfig.getConnectTimeout());
		assertEquals(AntMediaApplicationAdapter.CLUSTER_POST_TIMEOUT_MS, capturedConfig.getConnectionRequestTimeout());
		assertEquals(AntMediaApplicationAdapter.CLUSTER_POST_TIMEOUT_MS, capturedConfig.getSocketTimeout());

		// Test no retries left after failure
		when(httpClient.execute(any(HttpPost.class))).thenReturn(failResponse);
		result = spyAdapter.sendClusterPost(testUrl, testToken, null);
		assertFalse(result);

		// Test IOException handling with retries
		when(httpClient.execute(any(HttpPost.class)))
		.thenThrow(new IOException("Test exception"))
		.thenReturn(successResponse);

		result = spyAdapter.sendClusterPost(testUrl, testToken, null);
		assertFalse(result);

		// Verify the retry logic was triggered
		//verify(spyAdapter, times(2)).trySendClusterPostWithDelay(eq(testUrl), eq(testToken), eq(0));
	}


	@Test
	public void testStreamFetcherNotStartAutomatically()
	{
		IScope scope = mock(IScope.class);
		when(scope.getName()).thenReturn("junit");

		DataStore dataStore = new InMemoryDataStore("dbname");
		DataStoreFactory dsf = Mockito.mock(DataStoreFactory.class);
		Mockito.when(dsf.getDataStore()).thenReturn(dataStore);

		AntMediaApplicationAdapter spyAdapter = Mockito.spy(adapter);
		IContext context = mock(IContext.class);
		when(context.getBean(spyAdapter.VERTX_BEAN_NAME)).thenReturn(vertx);

		StorageClient storageClient = Mockito.mock(StorageClient.class);
		when(context.getBean(StorageClient.BEAN_NAME)).thenReturn(storageClient);

		when(scope.getContext()).thenReturn(context);
		spyAdapter.setDataStoreFactory(dsf);

		Mockito.doReturn(dataStore).when(spyAdapter).getDataStore();
		spyAdapter.setScope(scope);


		Broadcast broadcast = new Broadcast();
		broadcast.setType(AntMediaApplicationAdapter.STREAM_SOURCE);
		dataStore.save(broadcast);

		Result result = new Result(false);
		Mockito.when(spyAdapter.createInitializationProcess(Mockito.anyString())).thenReturn(result);
		//When createInitializationProcess(scope.getName());

		StreamFetcherManager streamFetcherManager = mock(StreamFetcherManager.class);

		spyAdapter.setStreamFetcherManager(streamFetcherManager);
		AppSettings settings = new AppSettings();
		settings.setStartStreamFetcherAutomatically(false);
		spyAdapter.setAppSettings(settings);
		spyAdapter.setServerSettings(new ServerSettings());
		spyAdapter.setStreamPublishSecurityList(new ArrayList<>());
		spyAdapter.setStreamPlaySecurityList(new ArrayList<>());

		spyAdapter.appStart(scope);

		await().pollInterval(2,TimeUnit.SECONDS).atMost(3, TimeUnit.SECONDS).until(()-> true);

		ArgumentCaptor<Broadcast> broadcastListCaptor = ArgumentCaptor.forClass(Broadcast.class);
		verify(streamFetcherManager, never()).startStreaming(broadcastListCaptor.capture());
	}


	@Test
	public void testCloseDB() {
		AntMediaApplicationAdapter spyAdapter = Mockito.spy(adapter);



		IContext context = mock(IContext.class);
		when(context.getBean(spyAdapter.VERTX_BEAN_NAME)).thenReturn(vertx);
		IScope scope = mock(IScope.class);
		when(scope.getContext()).thenReturn(context);
		spyAdapter.setScope(scope);

		DataStore dataStore = Mockito.mock(DataStore.class);
		DataStoreFactory dsf = Mockito.mock(DataStoreFactory.class);
		Mockito.when(dsf.getDataStore()).thenReturn(dataStore);
		spyAdapter.setDataStoreFactory(dsf);

		spyAdapter.closeDB(true);
		Mockito.verify(dataStore).close(true);

		spyAdapter.closeDB(false);
		Mockito.verify(dataStore, Mockito.times(1)).close(false);



		when(context.hasBean(IClusterNotifier.BEAN_NAME)).thenReturn(true);
		spyAdapter.closeDB(true);
		Mockito.verify(dataStore).close(true);

		Mockito.verify(dataStore, Mockito.timeout(ClusterNode.NODE_UPDATE_PERIOD + 2000).times(2)).close(true);


	}

	@Test
	public void testClusterUpdateSettings() {
		IScope scope = mock(IScope.class);
		when(scope.getName()).thenReturn("junit");

		DataStore dataStore = new InMemoryDataStore("dbname");
		DataStoreFactory dsf = Mockito.mock(DataStoreFactory.class);
		Mockito.when(dsf.getDataStore()).thenReturn(dataStore);

		AntMediaApplicationAdapter spyAdapter = Mockito.spy(adapter);
		IContext context = mock(IContext.class);
		when(context.getBean(spyAdapter.VERTX_BEAN_NAME)).thenReturn(vertx);
		when(context.hasBean(IClusterNotifier.BEAN_NAME)).thenReturn(true);
		StorageClient storageClient = Mockito.mock(StorageClient.class);
		when(context.getBean(StorageClient.BEAN_NAME)).thenReturn(storageClient);
		ServerSettings serverSettings = new ServerSettings();
		Mockito.doReturn(serverSettings).when(spyAdapter).getServerSettings();


		IClusterNotifier clusterNotifier = Mockito.mock(IClusterNotifier.class);
		when(context.getBean(IClusterNotifier.BEAN_NAME)).thenReturn(clusterNotifier);


		when(scope.getContext()).thenReturn(context);
		spyAdapter.setDataStoreFactory(dsf);

		Mockito.doReturn(dataStore).when(spyAdapter).getDataStore();
		spyAdapter.setScope(scope);

		AppSettings settings = new AppSettings();
		spyAdapter.setAppSettings(settings);

		IClusterStore clusterStore = Mockito.mock(IClusterStore.class);
		when(clusterNotifier.getClusterStore()).thenReturn(clusterStore);

		when(clusterStore.getSettings(any())).thenReturn(null);
		when(context.getBean(AcceptOnlyStreamsInDataStore.BEAN_NAME)).thenReturn(Mockito.mock(AcceptOnlyStreamsInDataStore.class));
		spyAdapter.setServerSettings(new ServerSettings());
		spyAdapter.setStreamPublishSecurityList(new ArrayList<>());
		spyAdapter.setStreamPlaySecurityList(new ArrayList<>());

		spyAdapter.appStart(scope);

		verify(clusterNotifier).registerSettingUpdateListener(any(), any());
		verify(spyAdapter).updateSettings(settings, true, false);


		AppSettings clusterStoreSettings = new AppSettings();
		when(clusterStore.getSettings(any())).thenReturn(clusterStoreSettings);
		spyAdapter.appStart(scope);
		verify(clusterNotifier, times(2)).registerSettingUpdateListener(any(), any());
		verify(spyAdapter).updateSettings(clusterStoreSettings, false, false);


		clusterStoreSettings.setToBeDeleted(true);
		clusterStoreSettings.setUpdateTime(System.currentTimeMillis());
		spyAdapter.appStart(scope);
		verify(clusterNotifier, times(3)).registerSettingUpdateListener(any(), any());
		verify(spyAdapter, times(2)).updateSettings(clusterStoreSettings, false, false);


		clusterStoreSettings.setUpdateTime(System.currentTimeMillis()-80000);
		spyAdapter.appStart(scope);
		verify(clusterNotifier, times(4)).registerSettingUpdateListener(any(), any());
		verify(spyAdapter, times(1)).updateSettings(settings, true, false);
		verify(spyAdapter, times(3)).updateSettings(clusterStoreSettings, false, false);


		clusterStoreSettings.setWarFileOriginServerAddress(serverSettings.getHostAddress());
		clusterStoreSettings.setUpdateTime(System.currentTimeMillis()+80000);
		clusterStoreSettings.setPullWarFile(true);
		spyAdapter.appStart(scope);
		verify(spyAdapter, times(2)).updateSettings(settings, true, false);
		assertTrue(settings.isPullWarFile());


		clusterStoreSettings.setWarFileOriginServerAddress("other address");
		clusterStoreSettings.setUpdateTime(System.currentTimeMillis()+80000);
		clusterStoreSettings.setPullWarFile(true);
		spyAdapter.appStart(scope);
		verify(spyAdapter, times(4)).updateSettings(clusterStoreSettings, false, false);
		assertTrue(settings.isPullWarFile());


		clusterStoreSettings.setWarFileOriginServerAddress(serverSettings.getHostAddress());
		clusterStoreSettings.setUpdateTime(System.currentTimeMillis()+80000);
		clusterStoreSettings.setPullWarFile(false);
		spyAdapter.appStart(scope);
		verify(spyAdapter, times(5)).updateSettings(clusterStoreSettings, false, false);
		assertFalse(settings.isPullWarFile());


	}

	@Test
	public void testUpdateMainBroadcast() {
		AntMediaApplicationAdapter spyAdapter = Mockito.spy(adapter);
		DataStore dataStore = new InMemoryDataStore("dbname");
		spyAdapter.setDataStore(dataStore);


		Broadcast subTrack1 = new Broadcast();
		try {
			subTrack1.setStreamId("subtrack1");
		} catch (Exception e) {
			e.printStackTrace();
		}


		Broadcast subTrack2 = new Broadcast();
		try {
			subTrack2.setStreamId("subtrack2");
		} catch (Exception e) {
			e.printStackTrace();
		}

		Broadcast mainTrack = new Broadcast();
		mainTrack.setListenerHookURL("url");
		try {
			mainTrack.setStreamId("maintrack");
		} catch (Exception e) {
			e.printStackTrace();
		}
		mainTrack.setZombi(true);

		subTrack1.setMainTrackStreamId(mainTrack.getStreamId());
		subTrack1.setStatus(IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING);
		subTrack1.setUpdateTime(System.currentTimeMillis());

		subTrack2.setMainTrackStreamId(mainTrack.getStreamId());
		subTrack2.setStatus(IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING);
		subTrack2.setUpdateTime(System.currentTimeMillis());

		mainTrack.getSubTrackStreamIds().add(subTrack1.getStreamId());
		mainTrack.getSubTrackStreamIds().add(subTrack2.getStreamId());


		dataStore.save(subTrack1);
		dataStore.save(subTrack2);
		dataStore.save(mainTrack);

		subTrack1.setStatus(IAntMediaStreamHandler.BROADCAST_STATUS_FINISHED);
		spyAdapter.updateMainTrackWithRecentlyFinishedBroadcast(subTrack1);
		assertNotNull(dataStore.get(mainTrack.getStreamId()));

		subTrack2.setStatus(IAntMediaStreamHandler.BROADCAST_STATUS_FINISHED);
		spyAdapter.updateMainTrackWithRecentlyFinishedBroadcast(subTrack2);
		assertNull(dataStore.get(mainTrack.getStreamId()));

	}

	@Test
	public void testAddRemovePacketListener() {

		AntMediaApplicationAdapter spyAdapter = Mockito.spy(adapter);
		MuxAdaptor mockAdaptor = mock(MuxAdaptor.class);
		String streamId = "stream_"+RandomUtils.nextInt(0, 1000);


		MuxAdaptor mockAdaptor2 = mock(MuxAdaptor.class);


		List<MuxAdaptor>  muxAdaptors = new ArrayList<MuxAdaptor>();
		muxAdaptors.add(mockAdaptor);
		muxAdaptors.add(mockAdaptor2);


		when(mockAdaptor2.getStreamId()).thenReturn("dummy");
		when(mockAdaptor.getStreamId()).thenReturn(streamId);

		IClusterStreamFetcher clusterStreamFetcher = mock(IClusterStreamFetcher.class);
		doReturn(muxAdaptors).when(spyAdapter).getMuxAdaptors();
		doReturn(mockAdaptor).when(spyAdapter).getMuxAdaptor(streamId);
		doReturn(mockAdaptor2).when(spyAdapter).getMuxAdaptor("dummy");

		doReturn(clusterStreamFetcher).when(spyAdapter).createClusterStreamFetcher();


		IPacketListener listener = mock(IPacketListener.class);
		spyAdapter.addPacketListener(streamId, listener);

		verify(mockAdaptor, times(1)).addPacketListener(listener);

		spyAdapter.removePacketListener(streamId, listener);
		verify(mockAdaptor, times(1)).removePacketListener(listener);

		String nonExistingStreamId = "stream_"+RandomUtils.nextInt(0, 1000);
		spyAdapter.addPacketListener(nonExistingStreamId, listener);
		verify(clusterStreamFetcher, times(1)).register(nonExistingStreamId, listener);


		spyAdapter.removePacketListener(nonExistingStreamId, listener);
		verify(clusterStreamFetcher, times(1)).remove(nonExistingStreamId, listener);


	}

	@Test
	public void testSaveBroadcast() throws Exception {
		DataStore dataStore = new InMemoryDataStore("test");

		adapter.setDataStore(dataStore);

		IScope scope = mock(IScope.class);
		when(scope.getName()).thenReturn("junit");

		IContext context = mock(IContext.class);
		ApplicationContext appContext = mock(ApplicationContext.class);
		when(context.getApplicationContext()).thenReturn(appContext);
		when(appContext.getBean(ServerSettings.BEAN_NAME)).thenReturn(new ServerSettings());

		when(scope.getContext()).thenReturn(context);

		adapter.setAppSettings(new AppSettings());

		adapter.setScope(scope);

		Broadcast broadcast = new Broadcast();
		broadcast.setStreamId("test123");

		assertNull(dataStore.get(broadcast.getStreamId()));
		AntMediaApplicationAdapter.saveBroadcast(broadcast, adapter);

		assertNotNull(dataStore.get(broadcast.getStreamId()));


	}


	@Test
	public void testSaveMainBroadcast() 
	{
		DataStore dataStore = new InMemoryDataStore("test");
		Broadcast mainTrack = AntMediaApplicationAdapter.saveMainBroadcast("streamId", "mainTrackId", dataStore);
		assertNotNull(mainTrack);
		//origin address must be null because main track is not a real stream
		assertNull(mainTrack.getOriginAdress());
	}

	@Test
	public void testAppDeletion() 
	{
		DataStore dataStore = Mockito.spy(new InMemoryDataStore("test"));
		adapter.setDataStore(dataStore);

		IScope scope = mock(IScope.class);
		when(scope.getName()).thenReturn("junit");

		IContext context = mock(IContext.class);
		ApplicationContext appContext = mock(ApplicationContext.class);
		when(context.getApplicationContext()).thenReturn(appContext);
		when(appContext.getBean(ServerSettings.BEAN_NAME)).thenReturn(new ServerSettings());

		when(scope.getContext()).thenReturn(context);


		adapter.setScope(scope);

		adapter.stopApplication(true);
		verify(dataStore, timeout(ClusterNode.NODE_UPDATE_PERIOD+1000)).close(true);
	}


	@Test
	public void testGetWebRTCClientMap() {

		assertNotNull(adapter.getWebRTCClientsMap());

		assertTrue(adapter.getWebRTCClientsMap().isEmpty());

	}


	@Test
	public void testSetAndGetSubtrackPoller() {

		assertNull(adapter.getSubtrackPoller());

		// Set the mockSubtrackPoller using the setter

		ISubtrackPoller mockSubtrackPoller = Mockito.mock(ISubtrackPoller.class);
		adapter.setSubtrackPoller(mockSubtrackPoller);

		// Get the subtrackPoller using the getter and verify it's the same as the mock
		ISubtrackPoller retrievedSubtrackPoller = adapter.getSubtrackPoller();
		assertEquals("The retrieved subtrackPoller should match the mock instance.", mockSubtrackPoller, retrievedSubtrackPoller);
	}

	@Test
	public void testSchedulePlayList() throws Exception {

		Broadcast broadcast = new Broadcast();
		broadcast.setStreamId("streamId");
		broadcast.setType(AntMediaApplicationAdapter.PLAY_LIST);
		adapter.schedulePlayList(System.currentTimeMillis(), broadcast);


		assertTrue(adapter.getPlayListSchedulerTimer().isEmpty());


		broadcast.setPlannedStartDate(100);
		adapter.schedulePlayList(System.currentTimeMillis(), broadcast);
		assertTrue(adapter.getPlayListSchedulerTimer().isEmpty());


		long now = System.currentTimeMillis();
		broadcast.setPlannedStartDate((now + 3000) / 1000);
		adapter.setDataStore(new InMemoryDataStore("testdb"));
		adapter.getDataStore().save(broadcast);
		StreamFetcherManager fetcherManager = Mockito.mock(StreamFetcherManager.class);
		adapter.setStreamFetcherManager(fetcherManager);

		adapter.schedulePlayList(now, broadcast);
		assertFalse(adapter.getPlayListSchedulerTimer().isEmpty());

		//it can take up 8 secs to start because of randomness about 5 seconds and 3 seconds 
		Mockito.verify(fetcherManager, Mockito.timeout(9000).times(1)).startPlaylist(broadcast);

		assertTrue(adapter.getPlayListSchedulerTimer().isEmpty());




		adapter.schedulePlayList(now, broadcast);
		assertFalse(adapter.getPlayListSchedulerTimer().isEmpty());
		adapter.cancelPlaylistSchedule(broadcast.getStreamId());
		assertTrue(adapter.getPlayListSchedulerTimer().isEmpty());

		//it should be still 1 because we cancel the timer 
		Mockito.verify(fetcherManager, Mockito.timeout(9000).times(1)).startPlaylist(broadcast);



		adapter.cancelPlaylistSchedule("anyId");
	}

	@Test
	public void testNotifyRoomEndedHook() {
		try{
			String mainTrackId = "stream_"+RandomUtils.nextInt(0, 1000);
			String subTrackId = "stream_"+RandomUtils.nextInt(0, 1000);
			DataStore db = new InMemoryDataStore("db");

			Broadcast roomBroadcast = new Broadcast();
			Broadcast subTrackBroadcast = new Broadcast();

			roomBroadcast.setStreamId(mainTrackId);
			roomBroadcast.setZombi(true);
			roomBroadcast.setListenerHookURL("url1");

			subTrackBroadcast.setZombi(true);
			subTrackBroadcast.setListenerHookURL("url2");
			subTrackBroadcast.setStreamId(subTrackId);
			subTrackBroadcast.setMainTrackStreamId(mainTrackId);
			db.save(subTrackBroadcast);
			db.save(roomBroadcast);

			AntMediaApplicationAdapter spyAdapter = spy(adapter);
			Vertx vertxSpy = spy(vertx);

			spyAdapter.setDataStore(db);

			IScope scope = mock(IScope.class);
			when(scope.getName()).thenReturn("junit");
			IContext context = Mockito.mock(IContext.class);
			when(context.getApplicationContext()).thenReturn(Mockito.mock(org.springframework.context.ApplicationContext.class));
			when(scope.getContext()).thenReturn(context);

			spyAdapter.setScope(scope);
			spyAdapter.setVertx(vertxSpy);

			AppSettings appSettings = new AppSettings();
			spyAdapter.setAppSettings(appSettings);

			doNothing().when(spyAdapter).sendPOST(anyString(), any(), anyInt(), any());

			spyAdapter.closeBroadcast(subTrackBroadcast.getStreamId(), null, null);

			verify(spyAdapter, timeout(5000)).updateMainTrackWithRecentlyFinishedBroadcast(subTrackBroadcast);


			verify(spyAdapter, timeout(5000)).notifyHook(subTrackBroadcast.getListenerHookURL(), subTrackId, mainTrackId, AntMediaApplicationAdapter.HOOK_ACTION_END_LIVE_STREAM, null, null, null, null, null, null, null);
			verify(spyAdapter, timeout(5000)).notifyNoActiveSubtracksLeftInMainTrack(roomBroadcast);
			verify(spyAdapter).notifyPublishStopped(mainTrackId, null, mainTrackId);

		}catch (Exception e){
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	@Test
	public void testNotifyRoomHook(){
		AntMediaApplicationAdapter spyAdapter = spy(adapter);
		DataStore db = new InMemoryDataStore("db");
		String mainTrackId = "mainTrack_"+RandomUtils.nextInt(0, 1000);
		String streamId = "stream_"+RandomUtils.nextInt(0, 1000);
		Vertx vertxSpy = spy(vertx);
		Broadcast broadcast = new Broadcast();
		try{
			broadcast.setStreamId(streamId);
		}catch (Exception e){
			e.printStackTrace();
			fail();
		}

		broadcast.setMetaData("metaData");
		db.save(broadcast);
		spyAdapter.setDataStore(db);


		Broadcast mainTrackBroadcast = new Broadcast();
		try{
			mainTrackBroadcast.setStreamId(mainTrackId);
		}catch (Exception e){
			e.printStackTrace();
			fail();
		}

		IScope scope = mock(IScope.class);
		when(scope.getName()).thenReturn("junit");
		IContext context = Mockito.mock(IContext.class);
		when(context.getApplicationContext()).thenReturn(Mockito.mock(org.springframework.context.ApplicationContext.class));
		when(scope.getContext()).thenReturn(context);

		spyAdapter.setScope(scope);
		spyAdapter.setVertx(vertxSpy);

		String webhookUrl = "url";

		AppSettings appSettings = new AppSettings();
		appSettings.setWebhookRetryCount(1);

		appSettings.setListenerHookURL(webhookUrl);
		spyAdapter.setAppSettings(appSettings);

		doNothing().when(spyAdapter).sendPOST(anyString(), any(), anyInt(), any());

		spyAdapter.notifyFirstActiveSubtrackInMainTrack(mainTrackBroadcast, broadcast.getStreamId());


		verify(spyAdapter,timeout(5000)).notifyHook(webhookUrl, broadcast.getStreamId(), mainTrackId, AntMediaApplicationAdapter.HOOK_ACTION_FIRST_ACTIVE_SUBTRACK_ADDED_IN_THE_MAINTRACK, null,null,null,null, null, null, null);

		verify(spyAdapter).notifyPublishStarted(mainTrackId, null, mainTrackId);

		appSettings.setListenerHookURL(null);
		spyAdapter.notifyFirstActiveSubtrackInMainTrack(mainTrackBroadcast, broadcast.getStreamId());

		verify(spyAdapter,times(1)).notifyHook(webhookUrl, broadcast.getStreamId(), mainTrackId, AntMediaApplicationAdapter.HOOK_ACTION_FIRST_ACTIVE_SUBTRACK_ADDED_IN_THE_MAINTRACK, null,null,null,null, null, null, null);
		broadcast.setMetaData(null);
		db.save(broadcast);

		appSettings.setListenerHookURL(webhookUrl);
		spyAdapter.notifyFirstActiveSubtrackInMainTrack(mainTrackBroadcast, broadcast.getStreamId());

		verify(spyAdapter,times(2)).notifyHook(webhookUrl, broadcast.getStreamId(), mainTrackId, AntMediaApplicationAdapter.HOOK_ACTION_FIRST_ACTIVE_SUBTRACK_ADDED_IN_THE_MAINTRACK, null,null,null,null, null, null, null);

	}

	@Test
	public void testUpdateBroadcastStatus() {
		AntMediaApplicationAdapter spyAdapter = spy(adapter);
		DataStore db = new InMemoryDataStore("db");
		spyAdapter.setDataStore(db);

		spyAdapter.setServerSettings(new ServerSettings());
		IScope scope = mock(IScope.class);
		when(scope.getName()).thenReturn("junit");
		IContext context = Mockito.mock(IContext.class);
		when(context.getApplicationContext()).thenReturn(Mockito.mock(org.springframework.context.ApplicationContext.class));
		when(scope.getContext()).thenReturn(context);

		spyAdapter.setScope(scope);

		spyAdapter.setAppSettings(new AppSettings());

		String streamId = "stream1";
		spyAdapter.updateBroadcastStatus(streamId, 0, IAntMediaStreamHandler.PUBLISH_TYPE_WEBRTC, null);

		assertNotNull(db.get(streamId));

		assertEquals(IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING, db.get(streamId).getStatus());
		spyAdapter.updateBroadcastStatus(streamId, 0, IAntMediaStreamHandler.PUBLISH_TYPE_WEBRTC, db.get(streamId), null, IAntMediaStreamHandler.BROADCAST_STATUS_PREPARING);
		Broadcast broadcast = db.get(streamId);
		assertNotNull(broadcast);
		assertFalse(broadcast.isVirtual());
		assertEquals(IAntMediaStreamHandler.BROADCAST_STATUS_PREPARING, db.get(streamId).getStatus());


		BroadcastUpdate broadcastUpdateForStatus = spyAdapter.getFreshBroadcastUpdateForStatus(IAntMediaStreamHandler.PUBLISH_TYPE_WEBRTC, IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING);
		broadcastUpdateForStatus.setVirtual(true);
		spyAdapter.updateBroadcastStatus(streamId, 0, IAntMediaStreamHandler.PUBLISH_TYPE_WEBRTC, db.get(streamId), broadcastUpdateForStatus, IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING);
		broadcast = db.get(streamId);
		assertNotNull(broadcast);
		assertTrue(broadcast.isVirtual());
		assertEquals(IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING, db.get(streamId).getStatus());

		Broadcast updateBroadcast = spyAdapter.updateBroadcastStatus(streamId, 0, IAntMediaStreamHandler.PUBLISH_TYPE_WEBRTC, null);
		assertNotNull(updateBroadcast);
	}

	@Test
	public void testGetBroadcastUpdateForStatus() {
		AntMediaApplicationAdapter spyAdapter = spy(adapter);
		DataStore db = new InMemoryDataStore("db");
		spyAdapter.setDataStore(db);

		spyAdapter.setServerSettings(new ServerSettings());

		BroadcastUpdate broadcastUpdateForStatus = spyAdapter.getFreshBroadcastUpdateForStatus(IAntMediaStreamHandler.PUBLISH_TYPE_WEBRTC, IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING);

		//we don't set virtual in the method above so getVirtual is null
		assertNull(broadcastUpdateForStatus.getVirtual());

		assertEquals(IAntMediaStreamHandler.PUBLISH_TYPE_WEBRTC, broadcastUpdateForStatus.getPublishType());
		assertEquals(IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING, broadcastUpdateForStatus.getStatus());
		assertNotNull(broadcastUpdateForStatus.getStartTime());
		assertNotNull(broadcastUpdateForStatus.getUpdateTime());
		assertEquals(spyAdapter.getServerSettings().getHostAddress(), broadcastUpdateForStatus.getOriginAdress());
		assertEquals(0, broadcastUpdateForStatus.getWebRTCViewerCount().intValue());
		assertEquals(0, broadcastUpdateForStatus.getHlsViewerCount().intValue());
		assertEquals(0, broadcastUpdateForStatus.getDashViewerCount().intValue());
	}
	@Test
	public void testFetchRtmpFromOriginIfExist() throws Exception {
		AntMediaApplicationAdapter spyAdapter = spy(adapter);
		DataStore db = new InMemoryDataStore("db");
		spyAdapter.setDataStore(db);
		String streamId = "test";

		spyAdapter.setServerSettings(new ServerSettings());
		spyAdapter.setAppSettings(new AppSettings());
		IScope scope = mock(IScope.class);
		spyAdapter.setScope(scope);
		when(scope.getName()).thenReturn("junit");
		IContext context = Mockito.mock(IContext.class);
		ApplicationContext applicationContext = Mockito.mock(org.springframework.context.ApplicationContext.class);
		doReturn(new AppSettings()).when(applicationContext).getBean(AppSettings.BEAN_NAME);
		doReturn(applicationContext).when(context).getApplicationContext();
		doReturn(context).when(scope).getContext();

		//broadcast is null
		boolean result = spyAdapter.fetchRtmpFromOriginIfExist("test");
		assertFalse(result);

		result = spyAdapter.fetchRtmpFromOriginIfExist(streamId);
		assertFalse(result);

		// broadcast exist on same server no need to fetch the stream

		Broadcast broadcast = Mockito.spy(new Broadcast());
		broadcast.setStreamId(streamId);
		broadcast.setOriginAdress(spyAdapter.getServerSettings().getHostAddress());
		broadcast.setRtmpURL("rtmp_url_test");
		broadcast.setUpdateTime(System.currentTimeMillis());
		broadcast.setStatus(IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING);
		db.save(broadcast);

		result = spyAdapter.fetchRtmpFromOriginIfExist(streamId);
		assertFalse(result);

		//stream exist on another node fetch the stream


		broadcast.setOriginAdress("1234");
		result = spyAdapter.fetchRtmpFromOriginIfExist(streamId);
		assertTrue(result);


	}


	@Test
	public void testBroadcastTriggersIdleHook() {
		AntMediaApplicationAdapter spyAdapter = spy(adapter);

		AppSettings appSettings = new AppSettings();
		String listenerHookURL = "something";
		appSettings.setListenerHookURL(listenerHookURL);
		spyAdapter.setAppSettings(appSettings);

		doNothing().when(spyAdapter).resetHLSStats(anyString());
		doNothing().when(spyAdapter).resetDASHStats(anyString());

		doReturn(null).when(spyAdapter).getBroadcastStream(any(), anyString());
		doNothing().when(spyAdapter).notifyHook(
				anyString(), // url
				anyString(), // id
				anyString(), // mainTrackId
				anyString(), // action
				anyString(), // streamName
				anyString(), // category
				nullable(String.class), // vodName
				nullable(String.class), // vodId
				nullable(String.class), // metadata
				nullable(String.class), // subscriberId
				nullable(Map.class)     // parameters
				);

		String streamId = "stream1";
		Broadcast broadcast = new Broadcast();
		try {
			broadcast.setStreamId(streamId);
		} catch (Exception e) {
			e.printStackTrace();
		}
		broadcast.setMaxIdleTime(5);

		DataStore db = mock(DataStore.class);
		when(db.get(broadcast.getStreamId())).thenReturn(broadcast);
		spyAdapter.setDataStore(db);

		spyAdapter.closeBroadcast(broadcast.getStreamId(), null, null);

		verify(spyAdapter, Mockito.after(4000).never()).notifyHook(listenerHookURL, broadcast.getStreamId(), broadcast.getMainTrackStreamId(), AntMediaApplicationAdapter.HOOK_IDLE_TIME_EXPIRED, null,null,null,null, null, null, null);

		verify(spyAdapter, Mockito.after(2000).times(1)).notifyHook(listenerHookURL, broadcast.getStreamId(), broadcast.getMainTrackStreamId(), AntMediaApplicationAdapter.HOOK_IDLE_TIME_EXPIRED, null,null,null,null, null, null, null);

	}



	@Test
	public void testBroadcastDoesntTriggerIdleHook() {
		AntMediaApplicationAdapter spyAdapter = spy(adapter);

		AppSettings appSettings = new AppSettings();
		String listenerHookURL = "something";
		appSettings.setListenerHookURL(listenerHookURL);
		spyAdapter.setAppSettings(appSettings);

		doNothing().when(spyAdapter).resetHLSStats(anyString());
		doNothing().when(spyAdapter).resetDASHStats(anyString());

		doReturn(null).when(spyAdapter).getBroadcastStream(any(), anyString());
		doNothing().when(spyAdapter).notifyHook(
				anyString(), // url
				anyString(), // id
				anyString(), // mainTrackId
				anyString(), // action
				anyString(), // streamName
				anyString(), // category
				nullable(String.class), // vodName
				nullable(String.class), // vodId
				nullable(String.class), // metadata
				nullable(String.class), // subscriberId
				nullable(Map.class)     // parameters
				);

		String streamId = "stream1";
		Broadcast broadcast = new Broadcast();
		try {
			broadcast.setStreamId(streamId);
		} catch (Exception e) {
			e.printStackTrace();
		}
		broadcast.setMaxIdleTime(5);

		DataStore db = mock(DataStore.class);
		when(db.get(broadcast.getStreamId())).thenReturn(broadcast);
		spyAdapter.setDataStore(db);


		spyAdapter.closeBroadcast(broadcast.getStreamId(), null, null);

		verify(spyAdapter,Mockito.after(4000).never()).notifyHook(listenerHookURL, broadcast.getStreamId(), broadcast.getMainTrackStreamId(), AntMediaApplicationAdapter.HOOK_IDLE_TIME_EXPIRED, null,null,null,null, null, null, null);

		broadcast.setUpdateTime(System.currentTimeMillis());

		verify(spyAdapter,Mockito.after(2000).never()).notifyHook(listenerHookURL, broadcast.getStreamId(), broadcast.getMainTrackStreamId(), AntMediaApplicationAdapter.HOOK_IDLE_TIME_EXPIRED, null,null,null,null, null, null, null);

	}

	@Test
	public void testRoomTriggersIdleHook() {
		AntMediaApplicationAdapter spyAdapter = spy(adapter);

		AppSettings appSettings = new AppSettings();
		String listenerHookURL = "something";
		appSettings.setListenerHookURL(listenerHookURL);
		spyAdapter.setAppSettings(appSettings);

		doNothing().when(spyAdapter).resetHLSStats(anyString());
		doNothing().when(spyAdapter).resetDASHStats(anyString());

		doReturn(null).when(spyAdapter).getBroadcastStream(any(), anyString());
		doNothing().when(spyAdapter).notifyHook(
				anyString(), // url
				anyString(), // id
				anyString(), // mainTrackId
				anyString(), // action
				anyString(), // streamName
				anyString(), // category
				nullable(String.class), // vodName
				nullable(String.class), // vodId
				nullable(String.class), // metadata
				nullable(String.class), // subscriberId
				nullable(Map.class)     // parameters
				);

		String streamId = "stream1";
		String mainTrackId = "room1";

		Broadcast broadcast = new Broadcast();
		try {
			broadcast.setStreamId(streamId);
		} catch (Exception e) {
			e.printStackTrace();
		}
		broadcast.setMainTrackStreamId(mainTrackId);

		Broadcast mainTrack = new Broadcast();
		try {
			mainTrack.setStreamId(mainTrackId);
		} catch (Exception e) {
			e.printStackTrace();
		}
		mainTrack.setMaxIdleTime(5);


		DataStore db = mock(DataStore.class);
		when(db.get(broadcast.getStreamId())).thenReturn(broadcast);
		when(db.get(mainTrack.getStreamId())).thenReturn(mainTrack);

		spyAdapter.setDataStore(db);

		when(db.getActiveSubtracksCount(anyString(), nullable(String.class))).thenReturn(0L);

		spyAdapter.updateMainTrackWithRecentlyFinishedBroadcast(broadcast);

		verify(spyAdapter,Mockito.after(4000).never()).notifyHook(listenerHookURL, mainTrack.getStreamId(), mainTrack.getMainTrackStreamId(), AntMediaApplicationAdapter.HOOK_IDLE_TIME_EXPIRED, null,null,null,null, null, null, null);

		verify(spyAdapter, Mockito.after(2000).times(1)).notifyHook(listenerHookURL, mainTrack.getStreamId(), mainTrack.getMainTrackStreamId(), AntMediaApplicationAdapter.HOOK_IDLE_TIME_EXPIRED, null,null,null,null, null, null, null);

	}

	@Test
	public void testRoomDoesntTriggerIdleHook() {
		AntMediaApplicationAdapter spyAdapter = spy(adapter);

		AppSettings appSettings = new AppSettings();
		String listenerHookURL = "something";
		appSettings.setListenerHookURL(listenerHookURL);
		spyAdapter.setAppSettings(appSettings);

		doNothing().when(spyAdapter).resetHLSStats(anyString());
		doNothing().when(spyAdapter).resetDASHStats(anyString());

		doReturn(null).when(spyAdapter).getBroadcastStream(any(), anyString());
		doNothing().when(spyAdapter).notifyHook(
				anyString(), // url
				anyString(), // id
				anyString(), // mainTrackId
				anyString(), // action
				anyString(), // streamName
				anyString(), // category
				nullable(String.class), // vodName
				nullable(String.class), // vodId
				nullable(String.class), // metadata
				nullable(String.class), // subscriberId
				nullable(Map.class)     // parameters
				);

		String streamId = "stream1";
		String mainTrackId = "room1";

		Broadcast broadcast = new Broadcast();
		try {
			broadcast.setStreamId(streamId);
		} catch (Exception e) {
			e.printStackTrace();
		}
		broadcast.setMainTrackStreamId(mainTrackId);

		Broadcast mainTrack = new Broadcast();
		try {
			mainTrack.setStreamId(mainTrackId);
		} catch (Exception e) {
			e.printStackTrace();
		}
		mainTrack.setMaxIdleTime(5);


		DataStore db = mock(DataStore.class);
		when(db.get(broadcast.getStreamId())).thenReturn(broadcast);
		when(db.get(mainTrack.getStreamId())).thenReturn(mainTrack);

		spyAdapter.setDataStore(db);

		when(db.getActiveSubtracksCount(anyString(), nullable(String.class))).thenReturn(0L);

		spyAdapter.updateMainTrackWithRecentlyFinishedBroadcast(broadcast);

		verify(spyAdapter,Mockito.after(4000).never()).notifyHook(listenerHookURL, mainTrack.getStreamId(), mainTrack.getMainTrackStreamId(), AntMediaApplicationAdapter.HOOK_IDLE_TIME_EXPIRED, null,null,null,null, null, null, null);

		verify(spyAdapter, Mockito.after(2000).times(1)).notifyHook(listenerHookURL, mainTrack.getStreamId(), mainTrack.getMainTrackStreamId(), AntMediaApplicationAdapter.HOOK_IDLE_TIME_EXPIRED, null,null,null,null, null, null, null);

	}


	@Test
	public void testSendWebHook() {
		AntMediaApplicationAdapter spyAdapter = spy(adapter);

		AppSettings appSettings = new AppSettings();
		String listenerHookURL = "something";
		appSettings.setListenerHookURL(listenerHookURL);
		spyAdapter.setAppSettings(appSettings);

		String id = "stream123";
		String mainTrackId = "mainTrack";
		String action = "publish";
		String streamName = "testStream";
		String category = "category";
		String vodName = "vod.mp4";
		String vodId = "vod123";
		String subscriberId = "subscriber1";
		Map<String, String> parameters = new HashMap<>();
		parameters.put("key", "value");

		Broadcast broadcast = new Broadcast(streamName);
		try {
			broadcast.setStreamId(id);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		broadcast.setMetaData("metaFromBroadcast");


		DataStore dataStore = mock(DataStore.class);
		Mockito.doReturn(dataStore).when(spyAdapter).getDataStore();

		when(dataStore.get(id)).thenReturn(broadcast);

		doNothing().when(spyAdapter).notifyHook(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());

		spyAdapter.sendWebHook(id, mainTrackId, action, streamName, category, vodName, vodId, "", subscriberId, parameters);

		verify(spyAdapter).notifyHook(
				eq(listenerHookURL), eq(id), eq(mainTrackId), eq(action),
				eq(streamName), eq(category), eq(vodName), eq(vodId),
				eq("metaFromBroadcast"), eq(subscriberId), eq(parameters)
				);


		spyAdapter.sendWebHook(id, mainTrackId, action, streamName, category, vodName, vodId, "meta from params", subscriberId, parameters);

		verify(spyAdapter).notifyHook(
				eq(listenerHookURL), eq(id), eq(mainTrackId), eq(action),
				eq(streamName), eq(category), eq(vodName), eq(vodId),
				eq("meta from params"), eq(subscriberId), eq(parameters)
				);

	}

	@Test
	public void testRTMPClusterStreamFetcherListener() throws Exception {

		AntMediaApplicationAdapter spyAdapter = spy(adapter);

		spyAdapter.setDataStore(new InMemoryDataStore("test"));

		RTMPClusterStreamFetcher rtmpClusterStreamFetcher = Mockito.mock(RTMPClusterStreamFetcher.class);
		String rtmpUrl = "rtmp://localhost/live/stream";
		String streamId = "streamId";
		RTMPClusterStreamFetcherListener listener = spyAdapter.new RTMPClusterStreamFetcherListener(rtmpClusterStreamFetcher, rtmpUrl, streamId);

		RtmpProvider rtmpProvider = Mockito.mock(RtmpProvider.class);
		when(rtmpClusterStreamFetcher.getRtmpProvider()).thenReturn(rtmpProvider);
		listener.streamFinished(listener);

		Mockito.verify(rtmpProvider, times(1)).detachRtmpPublisher(streamId);


		Broadcast broadcast = new Broadcast();
		broadcast.setStreamId(streamId);

		spyAdapter.getDataStore().save(broadcast);

		listener.streamFinished(listener);
		//it detaches because stream is not broadcasting
		Mockito.verify(rtmpProvider, times(2)).detachRtmpPublisher(streamId);

		//update time and status
		broadcast.setUpdateTime(System.currentTimeMillis());
		broadcast.setStatus(IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING);

		Mockito.doReturn(true).when(spyAdapter).isBroadcastOnThisServer(Mockito.any());

		listener.streamFinished(listener);
		//it enters another if statement
		Mockito.verify(rtmpProvider, times(3)).detachRtmpPublisher(streamId);


		Mockito.doReturn(false).when(spyAdapter).isBroadcastOnThisServer(Mockito.any());

		listener.streamFinished(listener);
		//it enters another if statement
		Mockito.verify(rtmpProvider, times(4)).detachRtmpPublisher(streamId);

		BroadcastScope broadcastScope = Mockito.mock(BroadcastScope.class);
		when(rtmpProvider.getBroadcastScope()).thenReturn(broadcastScope);
		when(broadcastScope.getConsumers()).thenReturn(Arrays.asList(Mockito.mock(IConsumer.class)));
		listener.streamFinished(listener);
		//verify does not increase
		Mockito.verify(rtmpProvider, times(4)).detachRtmpPublisher(streamId);


		Mockito.verify(rtmpClusterStreamFetcher, timeout(6000)).startStream();

	}

	@Test
	public void testStreamPlayItemPlay() {
		AntMediaApplicationAdapter spyAdapter = spy(adapter);
		InMemoryDataStore dtStore = new InMemoryDataStore("test");
		spyAdapter.setDataStore(dtStore);
		AppSettings appSettings = new AppSettings();
		dtStore.setAppSettings(appSettings);
		spyAdapter.setAppSettings(appSettings);

		spyAdapter.setServerSettings(new ServerSettings());

		IPlayItem playItem = mock(IPlayItem.class);
		when(playItem.getName()).thenReturn("streamId");
		ISubscriberStream stream = mock(ISubscriberStream.class);

		logger.info("First streamPlayItemPlay");
		spyAdapter.streamPlayItemPlay(stream, playItem, true);

		Mockito.verify(spyAdapter, timeout(5000)).sendWebHook(Mockito.eq("streamId"), Mockito.any(), Mockito.eq(AntMediaApplicationAdapter.HOOK_ACTION_PLAY_STARTED), Mockito.any(), Mockito.any(), 
				Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());

		Mockito.verify(spyAdapter, timeout(5000).times(0)).registerSubscriberToNode(Mockito.any(), Mockito.any(), Mockito.any());

		Map<String, String> params = new HashMap<>();
		params.put(WebSocketConstants.SUBSCRIBER_ID, "subid");
		stream = mock(ISubscriberStream.class);
		when(stream.getParams()).thenReturn(params);
		
		logger.info("Second streamPlayItemPlay");
		spyAdapter.streamPlayItemPlay(stream, playItem, true);

		Mockito.verify(spyAdapter, timeout(5000).times(2)).sendWebHook(Mockito.eq("streamId"), Mockito.any(), Mockito.eq(AntMediaApplicationAdapter.HOOK_ACTION_PLAY_STARTED), Mockito.any(), Mockito.any(), 
				Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());

		Mockito.verify(spyAdapter, timeout(5000).times(1)).registerSubscriberToNode(Mockito.eq("streamId"), Mockito.eq("subid"), Mockito.any());


	}

	@Test
	public void testStreamPlayItemStop() {
		AntMediaApplicationAdapter spyAdapter = spy(adapter);
		InMemoryDataStore dtStore = new InMemoryDataStore("test");
		spyAdapter.setDataStore(dtStore);
		AppSettings appSettings = new AppSettings();
		dtStore.setAppSettings(appSettings);
		spyAdapter.setAppSettings(appSettings);

		spyAdapter.setServerSettings(new ServerSettings());

		IPlayItem playItem = mock(IPlayItem.class);
		when(playItem.getName()).thenReturn("streamId");
		ISubscriberStream stream = mock(ISubscriberStream.class);


		spyAdapter.streamPlayItemStop(stream, playItem);

		Mockito.verify(spyAdapter, timeout(5000)).sendWebHook(Mockito.eq("streamId"), Mockito.any(), Mockito.eq(AntMediaApplicationAdapter.HOOK_ACTION_PLAY_STOPPED), Mockito.any(), Mockito.any(), 
				Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());


	}

	@Test
	public void testStreamSubscriberClose() {
		AntMediaApplicationAdapter spyAdapter = spy(adapter);
		InMemoryDataStore dtStore = new InMemoryDataStore("test");
		spyAdapter.setDataStore(dtStore);
		AppSettings appSettings = new AppSettings();
		dtStore.setAppSettings(appSettings);
		spyAdapter.setAppSettings(appSettings);

		spyAdapter.setServerSettings(new ServerSettings());

		ISubscriberStream stream = mock(ISubscriberStream.class);
		when(stream.getBroadcastStreamPublishName()).thenReturn("streamId");

		spyAdapter.streamSubscriberClose(stream);

		Mockito.verify(spyAdapter, timeout(5000)).sendWebHook(Mockito.eq("streamId"), Mockito.any(), Mockito.eq(AntMediaApplicationAdapter.HOOK_ACTION_PLAY_STOPPED), Mockito.any(), Mockito.any(), 
				Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());


	}

	@Test
	public void testSubscriberStreams() {

		{
			SingleItemSubscriberStream stream = new SingleItemSubscriberStream();

			assertTrue(stream.getParams().isEmpty());
			Map<String, String> params = new HashMap<>();
			stream.setParams(params);

			assertEquals(params, stream.getParams());
		}

		{
			PlaylistSubscriberStream pStream = new PlaylistSubscriberStream();
			assertTrue(pStream.getParams().isEmpty());
			Map<String, String> params = new HashMap<>();
			pStream.setParams(params);

			assertEquals(params, pStream.getParams());
		}

	}
	
	
	long t1Start = 0;
    long t1End = 0;
    long t2Start = 0;
    long t2End = 0;
    long t3Start = 0;
    long t3End = 0;
	@Test
	public void testPerKeyLockingBehavior() throws Exception {
        AntMediaApplicationAdapter adaptor = new AntMediaApplicationAdapter();
        
        DataStore inMemoryDatastore = new InMemoryDataStore("") {
			@Override
        	public Broadcast get(String id) {
        		Broadcast b = super.get(id);
        		try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
        		return b;
        	};
        };
        
		adaptor.setDataStore(inMemoryDatastore );
		
		adaptor.setAppSettings(new AppSettings());

        Broadcast b1 = new Broadcast(); 
        b1.setMainTrackStreamId("main1");
        Broadcast b2 = new Broadcast(); 
        b2.setMainTrackStreamId("main1");
        Broadcast b3 = new Broadcast(); 
        b3.setMainTrackStreamId("main2");
        
        Broadcast m1 = new Broadcast(); 
        m1.setStreamId("main1");
        
        Broadcast m2 = new Broadcast(); 
        m2.setStreamId("main2");

        
        inMemoryDatastore.save(b1);
        inMemoryDatastore.save(b2);
        inMemoryDatastore.save(b3);
        inMemoryDatastore.save(m1);
        inMemoryDatastore.save(m2);

        Thread th1 = new Thread(() -> {
            t1Start = System.currentTimeMillis();
            adaptor.updateMainTrackWithRecentlyFinishedBroadcast(b1);
            t1End = System.currentTimeMillis();
        }, "T1-main");

        Thread th2 = new Thread(() -> {
            t2Start = System.currentTimeMillis();
            adaptor.updateMainTrackWithRecentlyFinishedBroadcast(b2);
            t2End = System.currentTimeMillis();
        }, "T2-main");

        Thread th3 = new Thread(() -> {
            t3Start = System.currentTimeMillis();
            adaptor.updateMainTrackWithRecentlyFinishedBroadcast(b3);
            t3End = System.currentTimeMillis();
        }, "T3-main2");

        // Start all threads
        th1.start();
        Thread.sleep(10);
        th2.start();
        Thread.sleep(10);
        th3.start();

        // Wait for all threads to finish
        th1.join();
        th2.join();
        th3.join();

        long dt1 = (t1End - t1Start);
        long dt2 = (t2End - t2Start);
        long dt3 = (t3End - t3Start);

        System.out.println("Delta t1:" + dt1);
        System.out.println("Delta t2:" + dt2);
        System.out.println("Delta t3:" + dt3);
        
        assertTrue(100 > Math.abs(dt1*2 - dt2));
        assertTrue(100 > Math.abs(dt1 - dt3));

    }

	@Test
	public void testExceptionInStreamListener() throws Exception {
		AtomicBoolean listener1Invoked = new AtomicBoolean(false);
		AtomicBoolean listener2Invoked = new AtomicBoolean(false);

		AntMediaApplicationAdapter spyAdaptor = Mockito.spy(adapter);
		spyAdaptor.setDataStore(new InMemoryDataStore("testStartStopPublishWithSubscriberId"));
		spyAdaptor.setServerSettings(new ServerSettings());
		AppSettings appSettings = new AppSettings();
		spyAdaptor.setAppSettings(appSettings);

		Broadcast broadcast = new Broadcast();
		broadcast.setStreamId("streamId");


		String hookURL = "listener_hook_url";
		appSettings.setListenerHookURL(hookURL);

		IStreamListener streamListener = Mockito.mock(IStreamListener.class);
		spyAdaptor.addStreamListener(streamListener);

		String subscriberId = "subscriberId";

		spyAdaptor.addStreamListener(new IStreamListener() {
			@Override
			public void joinedTheRoom(String roomId, String streamId) {
			}
			@Override
			public void leftTheRoom(String roomId, String streamId) {
			}

			@Override
			public void streamStarted(Broadcast broadcast) {
				listener1Invoked.set(true);
				throw new NoClassDefFoundError("Plugin misses a dependency");
			}
		});

		spyAdaptor.addStreamListener(new IStreamListener() {
			@Override
			public void joinedTheRoom(String roomId, String streamId) {
			}
			@Override
			public void leftTheRoom(String roomId, String streamId) {
			}

			@Override
			public void streamStarted(Broadcast broadcast) {
				listener2Invoked.set(true);
			}
		});

		spyAdaptor.startPublish(broadcast.getStreamId(), 0, IAntMediaStreamHandler.PUBLISH_TYPE_RTMP, subscriberId, null);

		await("The first listener should have been invoked")
				.atMost(Duration.ofMillis(200))
				.untilAtomic(listener1Invoked, is(true));

		await("The second listener should have been invoked even if the first one threw an error")
				.atMost(Duration.ofMillis(200))
				.untilAtomic(listener2Invoked, is(true));

	}

}
