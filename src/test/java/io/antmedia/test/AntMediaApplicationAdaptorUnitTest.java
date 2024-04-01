package io.antmedia.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import io.vertx.core.*;
import org.apache.commons.lang3.RandomUtils;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.awaitility.Awaitility;
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
import org.red5.server.stream.ClientBroadcastStream;
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
import io.antmedia.AppSettings;
import io.antmedia.cluster.ClusterNode;
import io.antmedia.cluster.IClusterNotifier;
import io.antmedia.cluster.IClusterStore;
import io.antmedia.datastore.db.DataStore;
import io.antmedia.datastore.db.DataStoreFactory;
import io.antmedia.datastore.db.IDataStoreFactory;
import io.antmedia.datastore.db.InMemoryDataStore;
import io.antmedia.datastore.db.MapDBStore;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.datastore.db.types.VoD;
import io.antmedia.integration.AppFunctionalV2Test;
import io.antmedia.licence.ILicenceService;
import io.antmedia.muxer.IAntMediaStreamHandler;
import io.antmedia.muxer.MuxAdaptor;
import io.antmedia.plugin.api.IClusterStreamFetcher;
import io.antmedia.plugin.api.IPacketListener;
import io.antmedia.rest.model.Result;
import io.antmedia.security.AcceptOnlyStreamsInDataStore;
import io.antmedia.settings.ServerSettings;
import io.antmedia.statistic.type.WebRTCAudioReceiveStats;
import io.antmedia.statistic.type.WebRTCAudioSendStats;
import io.antmedia.statistic.type.WebRTCVideoReceiveStats;
import io.antmedia.statistic.type.WebRTCVideoSendStats;
import io.antmedia.storage.StorageClient;
import io.antmedia.streamsource.StreamFetcher;
import io.antmedia.streamsource.StreamFetcherManager;
import io.antmedia.track.ISubtrackPoller;
import io.vertx.core.impl.VertxImpl;
import io.vertx.ext.dropwizard.DropwizardMetricsOptions;


public class AntMediaApplicationAdaptorUnitTest {

	AntMediaApplicationAdapter adapter;
	String streamsFolderPath = "webapps/test/streams";

	Vertx vertx = Vertx.vertx();

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

		assertFalse(spyAdapter.isIncomingTimeValid(newSettings));

		newSettings.setUpdateTime(1000);

		assertFalse(spyAdapter.isIncomingTimeValid(newSettings));

		appSettings.setUpdateTime(900);

		assertTrue(spyAdapter.isIncomingTimeValid(newSettings));

		appSettings.setUpdateTime(2000);

		assertFalse(spyAdapter.isIncomingTimeValid(newSettings));

		newSettings.setUpdateTime(3000);
		assertTrue(spyAdapter.isIncomingTimeValid(newSettings));



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

		spyAdapter.updateSettings(newSettings, false, false);
		//it should not change times(1) because we don't want it to update the datastore
		verify(clusterNotifier, times(1)).getClusterStore();
		verify(clusterStore, times(1)).saveSettings(settings);

		settings.setUpdateTime(1000);
		newSettings.setUpdateTime(900);
		assertFalse(spyAdapter.updateSettings(newSettings, false, true));


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


		// Add 1. Broadcast
		Broadcast broadcast = new Broadcast();
		broadcast.setZombi(true);		


		// Add 2. Broadcast
		Broadcast broadcast2 = new Broadcast();

		broadcast2.setWebRTCViewerCount(100);
		broadcast2.setRtmpViewerCount(10);
		broadcast2.setHlsViewerCount(1000);

		broadcast2.setStatus(spyAdapter.BROADCAST_STATUS_BROADCASTING);


		// Add 3. Broadcast
		Broadcast broadcast3 = new Broadcast();
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
		assertEquals(7, vodList.size());

		for (VoD voD : vodList) {
			assertEquals("streams/resources/" + voD.getVodName(), voD.getFilePath());
		}

		linkFile = new File(streamsFolder, "resources");
		assertTrue(linkFile.exists());

	}

	@Test
	public void testMuxingFinishedWithPreview(){
		AppSettings appSettings = new AppSettings();
		appSettings.setGeneratePreview(true);
		appSettings.setMuxerFinishScript("src/test/resources/echo.sh");

		adapter.setAppSettings(appSettings);
		File f = new File ("src/test/resources/hello_script");

		DataStore dataStore = new InMemoryDataStore("dbname");
		DataStoreFactory dsf = Mockito.mock(DataStoreFactory.class);
		Mockito.when(dsf.getDataStore()).thenReturn(dataStore);
		adapter.setDataStoreFactory(dsf);

		adapter.setVertx(vertx);

		File anyFile = new File("src/test/resources/sample_MP4_480.mp4");

		File preview = new File("src/test/resources/preview.png");

		assertFalse(f.exists());

		adapter.muxingFinished("streamId", anyFile, 0, 100, 480, "src/test/resources/preview.png", null);

		Awaitility.await().atMost(5, TimeUnit.SECONDS).until(()-> f.exists());

		try {
			Files.delete(f.toPath());
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	@Test
	public void testMuxingFinished() {

		AppSettings appSettings = new AppSettings();
		appSettings.setMuxerFinishScript("src/test/resources/echo.sh");

		adapter.setAppSettings(appSettings);
		File f = new File ("src/test/resources/hello_script");

		DataStore dataStore = new InMemoryDataStore("dbname");
		DataStoreFactory dsf = Mockito.mock(DataStoreFactory.class);
		Mockito.when(dsf.getDataStore()).thenReturn(dataStore);
		adapter.setDataStoreFactory(dsf);

		adapter.setVertx(vertx);

		File anyFile = new File("src/test/resources/sample_MP4_480.mp4");

		{

			assertFalse(f.exists());

			adapter.muxingFinished("streamId", anyFile, 0, 100, 480, null, null);

			Awaitility.await().atMost(5, TimeUnit.SECONDS).until(()-> f.exists());

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

			adapter.muxingFinished("streamId", anyFile, 0, 100, 480, "", null);

			Awaitility.await().pollDelay(3, TimeUnit.SECONDS).atMost(4, TimeUnit.SECONDS).until(()-> !f.exists());
		}

	}

	@Test
	public void testRunMuxerScript() {
		File f = new File ("src/test/resources/hello_script");
		assertFalse(f.exists());

		adapter.setVertx(vertx);
		adapter.runScript("src/test/resources/echo.sh");

		Awaitility.await().atMost(5, TimeUnit.SECONDS).until(()-> f.exists());

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

			spyAdaptor.sendPOST("http://any_url", new HashMap(), appSettings.getWebhookRetryCount() );

			Mockito.verify(spyAdaptor, Mockito.times(0)).retrySendPostWithDelay(any(), any(), anyInt());


			HttpEntity entity = Mockito.mock(HttpEntity.class);
			InputStream is = new ByteArrayInputStream(ByteBuffer.allocate(10).array());
			Mockito.when(entity.getContent()).thenReturn(is);
			Mockito.when(httpResponse.getEntity()).thenReturn(entity);
			HashMap map = new HashMap();
			map.put("action", "action_any");
			spyAdaptor.sendPOST("http://any_url", map, appSettings.getWebhookRetryCount());

			Mockito.verify(spyAdaptor, Mockito.times(0)).retrySendPostWithDelay(any(), any(), anyInt());

			appSettings.setWebhookRetryCount(1);

			HttpEntity entity2 = Mockito.mock(HttpEntity.class);
			InputStream is2 = new ByteArrayInputStream(ByteBuffer.allocate(10).array());
			Mockito.when(entity2.getContent()).thenReturn(is2);
			Mockito.when(httpResponse.getEntity()).thenReturn(entity2);

			StatusLine statusLine = Mockito.mock(StatusLine.class);

			Mockito.when(httpResponse.getStatusLine()).thenReturn(statusLine);
			Mockito.when(statusLine.getStatusCode()).thenReturn(404);

			spyAdaptor.sendPOST("http://any_url", map, appSettings.getWebhookRetryCount());

			verify(spyAdaptor).retrySendPostWithDelay(
					ArgumentMatchers.eq("http://any_url"),
					ArgumentMatchers.eq(map),
					ArgumentMatchers.eq(appSettings.getWebhookRetryCount() - 1)
			);

			Mockito.when(statusLine.getStatusCode()).thenReturn(200);
			spyAdaptor.sendPOST("http://any_url", map, appSettings.getWebhookRetryCount());

			when(httpClient.execute(any())).thenThrow(new IOException("Simulated IOException"));
			spyAdaptor.sendPOST("http://any_url", map, appSettings.getWebhookRetryCount());

			appSettings.setWebhookRetryCount(0);
			spyAdaptor.sendPOST("http://any_url", map, appSettings.getWebhookRetryCount());

			Mockito.verify(spyAdaptor, Mockito.times(2)).retrySendPostWithDelay(any(), any(), anyInt());

		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
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
		appSettings = new AppSettings();
		spyAdaptor.setServerSettings(new ServerSettings());
		spyAdaptor.setAppSettings(appSettings);
		DataStore dataStore = new InMemoryDataStore("testHook");
		DataStoreFactory dsf = Mockito.mock(DataStoreFactory.class);
		Mockito.when(dsf.getDataStore()).thenReturn(dataStore);
		spyAdaptor.setDataStoreFactory(dsf);
		spyAdaptor.setDataStore(dataStore);

		dataStore.save(broadcast);

		spyAdaptor.startPublish(broadcast.getStreamId(), 0, IAntMediaStreamHandler.PUBLISH_TYPE_RTMP);

		broadcast = dataStore.get(broadcast.getStreamId());
		Mockito.verify(spyAdaptor, Mockito.timeout(2000).times(1)).getListenerHookURL(broadcast);


		spyAdaptor.closeBroadcast(broadcast.getStreamId());
		Mockito.verify(spyAdaptor, Mockito.timeout(2000).times(2)).getListenerHookURL(broadcast);


		spyAdaptor.publishTimeoutError(broadcast.getStreamId(), "");
		Mockito.verify(spyAdaptor, Mockito.timeout(2000).times(3)).getListenerHookURL(broadcast);

		spyAdaptor.incrementEncoderNotOpenedError(broadcast.getStreamId());
		Mockito.verify(spyAdaptor, Mockito.timeout(2000).times(4)).getListenerHookURL(broadcast);

		spyAdaptor.endpointFailedUpdate(broadcast.getStreamId(), "rtmp_url");
		Mockito.verify(spyAdaptor, Mockito.timeout(2000).times(5)).getListenerHookURL(broadcast);

	}

	@Test
	public void testNotifyHook() {

		AntMediaApplicationAdapter spyAdaptor = Mockito.spy(adapter);
		AppSettings appSettings = new AppSettings();
		appSettings.setWebhookRetryCount(2);
		spyAdaptor.setAppSettings(appSettings);

		spyAdaptor.notifyHook(null, null, null, null, null, null, null, null);
		Mockito.verify(spyAdaptor, never()).sendPOST(Mockito.any(), Mockito.any(), Mockito.anyInt());

		spyAdaptor.notifyHook("", null, null, null, null, null, null, null);
		Mockito.verify(spyAdaptor, never()).sendPOST(Mockito.any(), Mockito.any(), Mockito.anyInt());


		String id = String.valueOf((Math.random() * 10000));
		String action = "any_action";
		String streamName = String.valueOf((Math.random() * 10000));
		String category = "category";

		String vodName = "vod name" + String.valueOf((Math.random() * 10000));
		String vodId = String.valueOf((Math.random() * 10000));

		String url = "this is url";
		spyAdaptor.notifyHook(url, id, action, streamName, category, vodName, vodId, null);
		Mockito.verify(spyAdaptor, times(1)).sendPOST(Mockito.any(), Mockito.any(), Mockito.anyInt());

		ArgumentCaptor<String> captureUrl = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<Map> variables = ArgumentCaptor.forClass(Map.class);
		ArgumentCaptor<Integer> retryAttempts = ArgumentCaptor.forClass(Integer.class);
		Mockito.verify(spyAdaptor).sendPOST(captureUrl.capture(), variables.capture(), retryAttempts.capture());
		assertEquals(url, captureUrl.getValue());

		Map variablesMap = variables.getValue();
		assertEquals(id, variablesMap.get("id"));
		assertEquals(action, variablesMap.get("action"));
		assertEquals(streamName, variablesMap.get("streamName"));
		assertEquals(category, variablesMap.get("category"));
		assertEquals(vodName, variablesMap.get("vodName"));
		assertEquals(vodId, variablesMap.get("vodId"));
		assertNotNull(variablesMap.get("timestamp"));

		url = "this is second  url";
		spyAdaptor.notifyHook(url, id, null, null, null, null, null, null);


		ArgumentCaptor<String> captureUrl2 = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<Map> variables2 = ArgumentCaptor.forClass(Map.class);
		ArgumentCaptor<Integer> retryAttempts2 = ArgumentCaptor.forClass(Integer.class);

		Mockito.verify(spyAdaptor, Mockito.times(2)).sendPOST(captureUrl2.capture(), variables2.capture(), retryAttempts2.capture());
		assertEquals(url, captureUrl2.getValue());

		Map variablesMap2 = variables2.getValue();
		assertEquals(id, variablesMap2.get("id"));
		assertNull(variablesMap2.get("action"));
		assertNull(variablesMap2.get("streamName"));
		assertNull(variablesMap2.get("category"));
		assertNull(variablesMap2.get("vodName"));
		assertNull(variablesMap2.get("vodId"));
		assertNotNull(variablesMap2.get("timestamp"));


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
		ArgumentCaptor<String> captureAction = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> captureStreamName = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> captureCategory = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> captureVodName = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> captureVodId = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> captureMetadata = ArgumentCaptor.forClass(String.class);

		/*
		 * PUBLISH TIMEOUT ERROR
		 */

		spyAdaptor.publishTimeoutError(broadcast.getStreamId(), "");

		Awaitility.await().atMost(10, TimeUnit.SECONDS).until(()-> {
			boolean called = false;
			try {

				//verify that notifyHook is called 1 time
				verify(spyAdaptor, times(1)).notifyHook(captureUrl.capture(), captureId.capture(), captureAction.capture(),
						captureStreamName.capture(), captureCategory.capture(),captureVodName.capture(), captureVodId.capture(), captureMetadata.capture());

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

		Awaitility.await().atMost(10, TimeUnit.SECONDS).until(()-> {
			boolean called = false;
			try {

				//verify that notifyHook is called 1 time
				verify(spyAdaptor, times(2)).notifyHook(captureUrl.capture(), captureId.capture(), captureAction.capture(),
						captureStreamName.capture(), captureCategory.capture(),captureVodName.capture(), captureVodId.capture(), captureMetadata.capture());

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

		Awaitility.await().atMost(10, TimeUnit.SECONDS).until(()-> {
			boolean called = false;
			try {

				//verify that notifyHook is called 1 time
				verify(spyAdaptor, times(3)).notifyHook(captureUrl.capture(), captureId.capture(), captureAction.capture(),
						captureStreamName.capture(), captureCategory.capture(),captureVodName.capture(), captureVodId.capture(), captureMetadata.capture());

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
		ArgumentCaptor<String> captureAction = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> captureStreamName = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> captureCategory = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> captureVodName = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> captureVodId = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> captureMetadata = ArgumentCaptor.forClass(String.class);



		//call muxingFinished function
		spyAdaptor.muxingFinished(streamId, anyFile, 0, 100, 480, null, null);

		//verify that notifyHook is never called
		verify(spyAdaptor, never()).notifyHook(captureUrl.capture(), captureId.capture(), captureAction.capture(), 
				captureStreamName.capture(), captureCategory.capture(), captureVodName.capture(), captureVodId.capture(), captureMetadata.capture());


		/*
		 * Scenario 2; hook URL is defined for stream and stream is in DB
		 * So hook is posted
		 */

		//define hook URL for stream specific
		broadcast.setListenerHookURL("listenerHookURL");

		//(Changed due to a bug) In this scenario broadcast name should be irrelevant for the hook to work so setting it to null tests if it is dependent or not.
		broadcast.setName(null);

		//update broadcast
		dataStore.updateBroadcastFields(streamId, broadcast);

		//call muxingFinished function
		spyAdaptor.muxingFinished(streamId, anyFile, 0, 100, 480, null, null);

		Awaitility.await().atMost(10, TimeUnit.SECONDS).until(()-> {
			boolean called = false;
			try {

				//verify that notifyHook is called 1 time
				verify(spyAdaptor, times(1)).notifyHook(captureUrl.capture(), captureId.capture(), captureAction.capture(), 
						captureStreamName.capture(), captureCategory.capture(), captureVodName.capture(), captureVodId.capture(), captureMetadata.capture());

				assertEquals(captureUrl.getValue(), broadcast.getListenerHookURL());
				assertEquals(captureId.getValue(), broadcast.getStreamId());
				assertEquals(captureVodName.getValue()+".mp4", anyFile.getName());
				assertNull(captureStreamName.capture());

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
		spyAdaptor.muxingFinished(streamId, anyFile, 0, 100, 480, null, null);

		Awaitility.await().atMost(10, TimeUnit.SECONDS).until(()-> {
			boolean called = false;
			try {

				//verify that no new notifyHook is called 
				verify(spyAdaptor, times(1)).notifyHook(captureUrl.capture(), captureId.capture(), captureAction.capture(), 
						captureStreamName.capture(), captureCategory.capture(), captureVodName.capture(), captureVodId.capture(), captureMetadata.capture());

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
		spyAdaptor.muxingFinished(streamId, anyFile, 0, 100, 480, null, null);

		Awaitility.await().atMost(10, TimeUnit.SECONDS).until(()-> {
			boolean called = false;
			try {

				//verify that notifyHook is called 2 times
				verify(spyAdaptor, times(2)).notifyHook(captureUrl.capture(), captureId.capture(), captureAction.capture(), 
						captureStreamName.capture(), captureCategory.capture(), captureVodName.capture(), captureVodId.capture(), captureMetadata.capture());

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


		Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> threadStarted);

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

		spyAdapter.appStart(scope);

		Awaitility.await().pollInterval(2,TimeUnit.SECONDS).atMost(3, TimeUnit.SECONDS).until(()-> true);

		ArgumentCaptor<Broadcast> broadcastListCaptor = ArgumentCaptor.forClass(Broadcast.class);
		verify(streamFetcherManager, times(1)).startStreaming(broadcastListCaptor.capture());

		broadcast = dataStore.get(broadcast.getStreamId());
		assertNotNull(broadcastListCaptor.getValue());
		assertEquals(broadcast.getStreamId(),  broadcastListCaptor.getValue().getStreamId());
		assertEquals(broadcast.getStatus(),  broadcastListCaptor.getValue().getStatus());
	}

	@Test
	public void testStartStreaming() {
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



		Broadcast broadcast = new Broadcast();
		broadcast.setType(AntMediaApplicationAdapter.STREAM_SOURCE);
		broadcast.setStreamUrl("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerMeltdowns.mp4");
		dataStore.save(broadcast);

		boolean startStreaming = spyAdapter.startStreaming(broadcast).isSuccess();
		assertTrue(startStreaming);
		assertTrue(spyAdapter.getStreamFetcherManager().isStreamRunning(broadcast));

		StreamFetcher streamFetcher = spyAdapter.getStreamFetcherManager().getStreamFetcher(broadcast.getStreamId());
		Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> streamFetcher.isThreadActive());

		spyAdapter.getStreamFetcherManager().stopStreaming(broadcast.getStreamId());
		Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> !streamFetcher.isThreadActive());



		when(licenseService.isLicenceSuspended()).thenReturn(true);
		startStreaming = spyAdapter.startStreaming(broadcast).isSuccess();
		assertFalse(startStreaming);



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

		spyAdapter.appStart(scope);

		Awaitility.await().pollInterval(2,TimeUnit.SECONDS).atMost(3, TimeUnit.SECONDS).until(()-> true);

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
		try {
			mainTrack.setStreamId("maintrack");
		} catch (Exception e) {
			e.printStackTrace();
		}
		mainTrack.setZombi(true);

		subTrack1.setMainTrackStreamId(mainTrack.getStreamId());
		subTrack2.setMainTrackStreamId(mainTrack.getStreamId());

		mainTrack.getSubTrackStreamIds().add(subTrack1.getStreamId());
		mainTrack.getSubTrackStreamIds().add(subTrack2.getStreamId());


		dataStore.save(subTrack1);
		dataStore.save(subTrack1);
		dataStore.save(mainTrack);

		spyAdapter.updateMainBroadcast(subTrack1);
		assertNotNull(dataStore.get(mainTrack.getStreamId()));

		spyAdapter.updateMainBroadcast(subTrack2);
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
		
		Mockito.verify(fetcherManager, Mockito.timeout(7000).times(1)).startPlaylist(broadcast);

		assertTrue(adapter.getPlayListSchedulerTimer().isEmpty());
		
		
		
		
		adapter.schedulePlayList(now, broadcast);
		assertFalse(adapter.getPlayListSchedulerTimer().isEmpty());
		adapter.cancelPlaylistSchedule(broadcast.getStreamId());
		assertTrue(adapter.getPlayListSchedulerTimer().isEmpty());

		//it should be still 1 because we cancel the timer 
		Mockito.verify(fetcherManager, Mockito.timeout(7000).times(1)).startPlaylist(broadcast);


		
		adapter.cancelPlaylistSchedule("anyId");
	}

}
