package io.antmedia.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.awaitility.Awaitility;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.red5.server.api.IContext;
import org.red5.server.api.scope.IScope;
import org.red5.server.stream.ClientBroadcastStream;
import org.springframework.context.ApplicationContext;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.AppSettings;
import io.antmedia.IApplicationAdaptorFactory;
import io.antmedia.cluster.IClusterNotifier;
import io.antmedia.cluster.IClusterStore;
import io.antmedia.datastore.db.DataStore;
import io.antmedia.datastore.db.DataStoreFactory;
import io.antmedia.datastore.db.IDataStoreFactory;
import io.antmedia.datastore.db.InMemoryDataStore;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.datastore.db.types.VoD;
import io.antmedia.integration.AppFunctionalV2Test;
import io.antmedia.licence.ILicenceService;
import io.antmedia.muxer.MuxAdaptor;
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
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
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

	@Test
	public void testIsIncomingTimeValid() {
		AppSettings settings = new AppSettings();
		
		IScope scope = mock(IScope.class);

		when(scope.getName()).thenReturn("junit");
		
		AntMediaApplicationAdapter spyAdapter = Mockito.spy(adapter);
		AppSettings appSettings = new AppSettings();
		spyAdapter.setAppSettings(appSettings);
		IContext context = mock(IContext.class);
		when(context.getBean(Mockito.any())).thenReturn(mock(AcceptOnlyStreamsInDataStore.class));
		when(scope.getContext()).thenReturn(context);
		Mockito.doReturn(mock(DataStore.class)).when(spyAdapter).getDataStore();
		
		
		settings = new AppSettings();
		
		assertTrue(spyAdapter.isIncomingTimeValid(settings, false));
		
		assertTrue(spyAdapter.isIncomingTimeValid(settings, true));
		
		settings.setUpdateTime(1000);
		
		assertTrue(spyAdapter.isIncomingTimeValid(settings, true));
		
		appSettings.setUpdateTime(900);
		
		assertTrue(spyAdapter.isIncomingTimeValid(settings, true));
		
		appSettings.setUpdateTime(2000);
		
		assertFalse(spyAdapter.isIncomingTimeValid(settings, true));
		assertTrue(spyAdapter.isIncomingTimeValid(settings, false));
		
		settings.setUpdateTime(3000);
		assertTrue(spyAdapter.isIncomingTimeValid(settings, false));
		
		
		
	}

	@Test
	public void testAppSettings() 
	{
		AppSettings settings = new AppSettings();

		AppSettings newSettings = Mockito.spy(new AppSettings());
		newSettings.setVodFolder("");
		newSettings.setListenerHookURL("");
		newSettings.setHlsPlayListType("");
		newSettings.setTokenHashSecret("");
		newSettings.setDataChannelPlayerDistribution("");

		IScope scope = mock(IScope.class);

		when(scope.getName()).thenReturn("junit");

		AntMediaApplicationAdapter spyAdapter = Mockito.spy(adapter);
		IContext context = mock(IContext.class);
		when(context.getBean(Mockito.any())).thenReturn(mock(AcceptOnlyStreamsInDataStore.class));
		when(scope.getContext()).thenReturn(context);
		Mockito.doReturn(mock(DataStore.class)).when(spyAdapter).getDataStore();
		
		StorageClient storageClient = Mockito.mock(StorageClient.class);
		spyAdapter.setStorageClient(storageClient);
		
		spyAdapter.setAppSettings(settings);
		spyAdapter.setScope(scope);
		spyAdapter.updateSettings(newSettings, true, false);
		
		

		IClusterNotifier clusterNotifier = mock(IClusterNotifier.class);

		IClusterStore clusterStore = mock(IClusterStore.class);

		when(clusterNotifier.getClusterStore()).thenReturn(clusterStore);
		spyAdapter.setClusterNotifier(clusterNotifier);

		spyAdapter.updateSettings(newSettings, true, false);

		verify(clusterNotifier, times(1)).getClusterStore();
		verify(clusterStore, times(1)).saveSettings(settings);
		
		spyAdapter.updateSettings(newSettings, false, false);
		//it should not change times(1) because we don't want it to update the datastore
		verify(clusterNotifier, times(1)).getClusterStore();
		verify(clusterStore, times(1)).saveSettings(settings);
		
		settings.setUpdateTime(1000);
		newSettings.setUpdateTime(900);
		assertFalse(spyAdapter.updateSettings(newSettings, false, true));
		
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
		List<VoD> vodList = dataStore.getVodList(0, 50, null, null, null, null);
		assertEquals(6, vodList.size());

		for (VoD voD : vodList) {
			assertEquals("streams/resources/" + voD.getVodName(), voD.getFilePath());
		}

		linkFile = new File(streamsFolder, "resources");
		assertTrue(linkFile.exists());

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

			adapter.muxingFinished("streamId", anyFile, 100, 480);

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

			adapter.muxingFinished("streamId", anyFile, 100, 480);

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

			CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);
			Mockito.doReturn(httpClient).when(spyAdaptor).getHttpClient();

			CloseableHttpResponse httpResponse = Mockito.mock(CloseableHttpResponse.class);
			Mockito.when(httpClient.execute(Mockito.any())).thenReturn(httpResponse);
			Mockito.when(httpResponse.getStatusLine()).thenReturn(Mockito.mock(StatusLine.class));

			Mockito.when(httpResponse.getEntity()).thenReturn(null);
			StringBuilder response = spyAdaptor.sendPOST("http://any_url", new HashMap());
			assertNull(response);

			HttpEntity entity = Mockito.mock(HttpEntity.class);
			InputStream is = new ByteArrayInputStream(ByteBuffer.allocate(10).array());
			Mockito.when(entity.getContent()).thenReturn(is);
			Mockito.when(httpResponse.getEntity()).thenReturn(entity);
			HashMap map = new HashMap();
			map.put("action", "action_any");
			response = spyAdaptor.sendPOST("http://any_url", map);
			assertNotNull(response);
			assertEquals(10, response.length());


		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}


	@Test
	public void testNotifyHook() {

		AntMediaApplicationAdapter spyAdaptor = Mockito.spy(adapter);

		StringBuilder notifyHook = spyAdaptor.notifyHook(null, null, null, null, null, null, null);
		assertNull(notifyHook);

		notifyHook = spyAdaptor.notifyHook("", null, null, null, null, null, null);
		assertNull(notifyHook);


		String id = String.valueOf((Math.random() * 10000));
		String action = "any_action";
		String streamName = String.valueOf((Math.random() * 10000));
		String category = "category";

		String vodName = "vod name" + String.valueOf((Math.random() * 10000));
		String vodId = String.valueOf((Math.random() * 10000));

		String url = "this is url";
		notifyHook = spyAdaptor.notifyHook(url, id, action, streamName, category, vodName, vodId);
		assertNull(notifyHook);

		try {
			ArgumentCaptor<String> captureUrl = ArgumentCaptor.forClass(String.class);
			ArgumentCaptor<Map> variables = ArgumentCaptor.forClass(Map.class);
			Mockito.verify(spyAdaptor).sendPOST(captureUrl.capture(), variables.capture());
			assertEquals(url, captureUrl.getValue());

			Map variablesMap = variables.getValue();
			assertEquals(id, variablesMap.get("id"));
			assertEquals(action, variablesMap.get("action"));
			assertEquals(streamName, variablesMap.get("streamName"));
			assertEquals(category, variablesMap.get("category"));
			assertEquals(vodName, variablesMap.get("vodName"));
			assertEquals(vodId, variablesMap.get("vodId"));

		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}


		url = "this is second  url";
		notifyHook = spyAdaptor.notifyHook(url, id, null, null, null, null, null);
		assertNull(notifyHook);

		try {
			ArgumentCaptor<String> captureUrl = ArgumentCaptor.forClass(String.class);
			ArgumentCaptor<Map> variables = ArgumentCaptor.forClass(Map.class);
			Mockito.verify(spyAdaptor, Mockito.times(2)).sendPOST(captureUrl.capture(), variables.capture());
			assertEquals(url, captureUrl.getValue());

			Map variablesMap = variables.getValue();
			assertEquals(id, variablesMap.get("id"));
			assertNull(variablesMap.get("action"));
			assertNull(variablesMap.get("streamName"));
			assertNull(variablesMap.get("category"));
			assertNull(variablesMap.get("vodName"));
			assertNull(variablesMap.get("vodId"));

		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

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

		//call muxingFinished function
		spyAdaptor.muxingFinished(streamId, anyFile, 100, 480);

		//verify that notifyHook is never called
		verify(spyAdaptor, never()).notifyHook(captureUrl.capture(), captureId.capture(), captureAction.capture(), 
				captureStreamName.capture(), captureCategory.capture(), captureVodName.capture(), captureVodId.capture());


		/*
		 * Scenario 2; hook URL is defined for stream and stream is in DB
		 * So hook is posted
		 */

		//define hook URL for stream specific
		broadcast.setListenerHookURL("listenerHookURL");
		broadcast.setName("name");

		//update broadcast
		dataStore.updateBroadcastFields(streamId, broadcast);

		//call muxingFinished function
		spyAdaptor.muxingFinished(streamId, anyFile, 100, 480);	

		Awaitility.await().atMost(10, TimeUnit.SECONDS).until(()-> {
			boolean called = false;
			try {

				//verify that notifyHook is called 1 time
				verify(spyAdaptor, times(1)).notifyHook(captureUrl.capture(), captureId.capture(), captureAction.capture(), 
						captureStreamName.capture(), captureCategory.capture(), captureVodName.capture(), captureVodId.capture());

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

		/*
		 * Scenario 3; Stream is deleted from DB (zombi stream)
		 * also no HookURL is defined in AppSettins
		 * so no hook is posted
		 */

		//delete broadcast from db
		dataStore.delete(streamId);

		//call muxingFinished function
		spyAdaptor.muxingFinished(streamId, anyFile, 100, 480);	

		Awaitility.await().atMost(10, TimeUnit.SECONDS).until(()-> {
			boolean called = false;
			try {

				//verify that no new notifyHook is called 
				verify(spyAdaptor, times(1)).notifyHook(captureUrl.capture(), captureId.capture(), captureAction.capture(), 
						captureStreamName.capture(), captureCategory.capture(), captureVodName.capture(), captureVodId.capture());

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
		spyAdaptor.muxingFinished(streamId, anyFile, 100, 480);	

		Awaitility.await().atMost(10, TimeUnit.SECONDS).until(()-> {
			boolean called = false;
			try {

				//verify that notifyHook is called 2 times
				verify(spyAdaptor, times(2)).notifyHook(captureUrl.capture(), captureId.capture(), captureAction.capture(), 
						captureStreamName.capture(), captureCategory.capture(), captureVodName.capture(), captureVodId.capture());

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
			boolean synchUserVoDFolder = adapter.deleteOldFolderPath("", f);
			assertFalse(synchUserVoDFolder);

			synchUserVoDFolder = adapter.deleteOldFolderPath(null, f);
			assertFalse(synchUserVoDFolder);

			synchUserVoDFolder = adapter.deleteOldFolderPath("anyfile", null);
			assertFalse(synchUserVoDFolder);


			synchUserVoDFolder = adapter.deleteOldFolderPath("notexist", f);
			assertFalse(synchUserVoDFolder);

			synchUserVoDFolder = adapter.deleteOldFolderPath(emptyFile.getName(), f);
			assertFalse(synchUserVoDFolder);

			File oldDir = new File (streamsFolderPath, "dir");
			oldDir.mkdirs();
			oldDir.deleteOnExit();

			synchUserVoDFolder = adapter.deleteOldFolderPath(oldDir.getName(), f);
			assertTrue(synchUserVoDFolder);

		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
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

		
		Queue<StreamFetcher> sfQueue = new ConcurrentLinkedQueue<StreamFetcher>();
		sfQueue.add(streamFetcher);
		sfQueue.add(streamFetcher2);
		
		fetcherManager.setStreamFetcherList(sfQueue);
		adapter.setStreamFetcherManager(fetcherManager);


		MuxAdaptor muxerAdaptor = mock(MuxAdaptor.class);
		adapter.muxAdaptorAdded(muxerAdaptor);

		Broadcast broadcast = mock(Broadcast.class);
		when(broadcast.getType()).thenReturn(AntMediaApplicationAdapter.LIVE_STREAM);
		ClientBroadcastStream cbs = mock(ClientBroadcastStream.class);
		when(muxerAdaptor.getBroadcastStream()).thenReturn(cbs);
		when(muxerAdaptor.getBroadcast()).thenReturn(broadcast);

		when(dataStore.getLocalLiveBroadcastCount(Mockito.any())).thenReturn(1L);

		new Thread() {
			public void run() {
				try {
					sleep(3000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				when(dataStore.getLocalLiveBroadcastCount(Mockito.any())).thenReturn(0L);
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
		
		Queue<StreamFetcher> streamFetcherList= new ConcurrentLinkedQueue<>();
		
		StreamFetcher streamFetcher = mock(StreamFetcher.class);
		StreamFetcher streamFetcher2 = mock(StreamFetcher.class);
		StreamFetcher streamFetcher3 = mock(StreamFetcher.class);
		StreamFetcher streamFetcher4 = mock(StreamFetcher.class);
		
		streamFetcherList.add(streamFetcher);
		streamFetcherList.add(streamFetcher2);
		streamFetcherList.add(streamFetcher3);
		streamFetcherList.add(streamFetcher4);
		
		StreamFetcherManager fetcherManager = mock(StreamFetcherManager.class);
		when(fetcherManager.getStreamFetcherList()).thenReturn(streamFetcherList);
		adapter.setStreamFetcherManager(fetcherManager);
		
		assertEquals(4, streamFetcherList.size());
		
		adapter.closeStreamFetchers();

		assertEquals(0, streamFetcherList.size());
	}

	@Test
	public void testEncoderBlocked() {
		assertEquals(0, adapter.getNumberOfEncodersBlocked());
		assertEquals(0, adapter.getNumberOfEncoderNotOpenedErrors());

		adapter.incrementEncoderNotOpenedError();
		adapter.incrementEncoderNotOpenedError();
		adapter.incrementEncoderNotOpenedError();

		assertEquals(3, adapter.getNumberOfEncoderNotOpenedErrors());
	}

	@Test
	public void testPublishTimeout() {
		assertEquals(0, adapter.getNumberOfPublishTimeoutError());

		adapter.publishTimeoutError("streamId");

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
		spyAdapter.appStart(scope);

		Awaitility.await().pollInterval(2,TimeUnit.SECONDS).atMost(3, TimeUnit.SECONDS).until(()-> true);

		ArgumentCaptor<List<Broadcast>> broadcastListCaptor = ArgumentCaptor.forClass(List.class);
		verify(streamFetcherManager, times(1)).startStreams(broadcastListCaptor.capture());
		
		assertEquals(1,  broadcastListCaptor.getValue().size());
		assertEquals(broadcast,  broadcastListCaptor.getValue().get(0));
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
		
		
		Application appFactor = Mockito.mock(Application.class);
		when(appFactor.getAppAdaptor()).thenReturn(spyAdapter);
		spyAdapter.setServerSettings(new ServerSettings());
		spyAdapter.setAppSettings(new AppSettings());
		spyAdapter.setDataStore(dataStore);
		
		when(appContext.getBean(AntMediaApplicationAdapter.BEAN_NAME)).thenReturn(appFactor);
		
		when(appContext.containsBean(AppSettings.BEAN_NAME)).thenReturn(true);
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
		assertTrue(spyAdapter.getStreamFetcherManager().isStreamRunning(broadcast.getStreamId()));
		
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
		spyAdapter.appStart(scope);

		Awaitility.await().pollInterval(2,TimeUnit.SECONDS).atMost(3, TimeUnit.SECONDS).until(()-> true);

		ArgumentCaptor<List<Broadcast>> broadcastListCaptor = ArgumentCaptor.forClass(List.class);
		verify(streamFetcherManager, never()).startStreams(broadcastListCaptor.capture());
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
		
		when(clusterStore.getSettings(Mockito.any())).thenReturn(null);
		when(context.getBean(AcceptOnlyStreamsInDataStore.BEAN_NAME)).thenReturn(Mockito.mock(AcceptOnlyStreamsInDataStore.class));
		spyAdapter.setServerSettings(new ServerSettings());
		
		
		spyAdapter.appStart(scope);
		
		verify(clusterNotifier).registerSettingUpdateListener(Mockito.any(), Mockito.any());
		verify(spyAdapter).updateSettings(settings, true, false);
		

		AppSettings clusterStoreSettings = new AppSettings();
		when(clusterStore.getSettings(Mockito.any())).thenReturn(clusterStoreSettings);
		spyAdapter.appStart(scope);
		verify(clusterNotifier, times(2)).registerSettingUpdateListener(Mockito.any(), Mockito.any());
		verify(spyAdapter).updateSettings(clusterStoreSettings, false, false);
		
		
		clusterStoreSettings.setToBeDeleted(true);
		clusterStoreSettings.setUpdateTime(System.currentTimeMillis());
		spyAdapter.appStart(scope);
		verify(clusterNotifier, times(3)).registerSettingUpdateListener(Mockito.any(), Mockito.any());
		verify(spyAdapter, times(2)).updateSettings(clusterStoreSettings, false, false);
		
		
		clusterStoreSettings.setUpdateTime(System.currentTimeMillis()-80000);
		spyAdapter.appStart(scope);
		verify(clusterNotifier, times(4)).registerSettingUpdateListener(Mockito.any(), Mockito.any());
		verify(spyAdapter, times(2)).updateSettings(settings, true, false);
	
		
	}

}
