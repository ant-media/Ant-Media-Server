package io.antmedia.test.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang3.RandomStringUtils;
import org.awaitility.Awaitility;
import org.bytedeco.ffmpeg.global.avformat;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.mockito.Mockito;
import org.red5.server.api.IContext;
import org.red5.server.api.scope.IScope;
import org.red5.server.api.stream.IClientBroadcastStream;
import org.red5.server.api.stream.IStreamCapableConnection;
import org.red5.server.scope.Scope;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.AppSettings;
import io.antmedia.RecordType;
import io.antmedia.datastore.db.DataStore;
import io.antmedia.datastore.db.InMemoryDataStore;
import io.antmedia.datastore.db.MongoStore;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.datastore.db.types.ConferenceRoom;
import io.antmedia.datastore.db.types.Endpoint;
import io.antmedia.datastore.db.types.StreamInfo;
import io.antmedia.datastore.db.types.Subscriber;
import io.antmedia.datastore.db.types.SubscriberStats;
import io.antmedia.datastore.db.types.TensorFlowObject;
import io.antmedia.datastore.db.types.Token;
import io.antmedia.datastore.db.types.VoD;
import io.antmedia.datastore.db.types.WebRTCViewerInfo;
import io.antmedia.ipcamera.OnvifCamera;
import io.antmedia.ipcamera.onvifdiscovery.DeviceDiscovery;
import io.antmedia.muxer.HLSMuxer;
import io.antmedia.muxer.Mp4Muxer;
import io.antmedia.muxer.MuxAdaptor;
import io.antmedia.muxer.Muxer;
import io.antmedia.muxer.RecordMuxer;
import io.antmedia.rest.BroadcastRestService;
import io.antmedia.rest.RestServiceBase;
import io.antmedia.rest.RestServiceBase.BroadcastStatistics;
import io.antmedia.rest.RestServiceBase.ProcessBuilderFactory;
import io.antmedia.rest.RootRestService;
import io.antmedia.rest.WebRTCClientStats;
import io.antmedia.rest.model.BasicStreamInfo;
import io.antmedia.rest.model.Result;
import io.antmedia.rest.model.Version;
import io.antmedia.security.ITokenService;
import io.antmedia.settings.ServerSettings;
import io.antmedia.statistic.DashViewerStats;
import io.antmedia.statistic.HlsViewerStats;
import io.antmedia.statistic.IStatsCollector;
import io.antmedia.statistic.StatsCollector;
import io.antmedia.streamsource.StreamFetcher;
import io.antmedia.streamsource.StreamFetcherManager;
import io.antmedia.test.StreamFetcherUnitTest;
import io.antmedia.webrtc.VideoCodec;
import io.antmedia.webrtc.api.IWebRTCAdaptor;
import io.antmedia.websocket.WebSocketConstants;
import io.vertx.core.Vertx;

@ContextConfiguration(locations = { "test.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class BroadcastRestServiceV2UnitTest {


	private BroadcastRestService restServiceReal = null;
	public AntMediaApplicationAdapter app = null;

	static {
		System.setProperty("red5.deployment.type", "junit");
		System.setProperty("red5.root", ".");
		avformat.avformat_network_init();
	}

	Vertx vertx = io.vertx.core.Vertx.vertx();


	@Before
	public void before() {
		restServiceReal = new BroadcastRestService();
	}

	@After
	public void after() {
		restServiceReal = null;
	}


	@Rule
    public TestRule watcher = new TestWatcher() {
        protected void starting(Description description) {
            System.out.println("Starting test: " + description.getMethodName());
        }

        protected void failed(Throwable e, Description description) {
            System.out.println("Failed test: " + description.getMethodName());
        }

        ;

        protected void finished(Description description) {
            System.out.println("Finishing test: " + description.getMethodName());
        }

        ;
    };

	/**
	 * These tests should be run with stalker db
	 */
	@Test
	public void testImportLiveStreams2Stalker()  {
		AppSettings settings = mock(AppSettings.class);


		when(settings.getStalkerDBServer()).thenReturn("192.168.1.29");
		when(settings.getStalkerDBUsername()).thenReturn("stalker");
		when(settings.getStalkerDBPassword()).thenReturn("1");
		//when(settings.getServerName()).thenReturn(null);
		
		ServerSettings serverSettings = mock(ServerSettings.class);
		when(serverSettings.getServerName()).thenReturn(null);
		restServiceReal.setServerSettings(serverSettings);

		Scope scope = mock(Scope.class);
		String scopeName = "scope";
		when(scope.getName()).thenReturn(scopeName);

		restServiceReal.setScope(scope);

		restServiceReal.setAppSettings(settings);

		Broadcast broadcast = new Broadcast(null, "name");
		DataStore store = new InMemoryDataStore("testdb");
		restServiceReal.setDataStore(store);

		Process process = mock(Process.class);
		try {
			when(process.waitFor()).thenReturn(0);


			ProcessBuilderFactory factory = new ProcessBuilderFactory() {
				@Override
				public Process make(String... args) {
					return process;
				}
			};
			restServiceReal.setProcessBuilderFactory(factory);

			Response response = restServiceReal.createBroadcast(broadcast, false);
			Broadcast createBroadcast = (Broadcast) response.getEntity();
			assertNotNull(createBroadcast.getStreamId());

			Result result = restServiceReal.importLiveStreams2Stalker();
			assertTrue(result.isSuccess());
		} 
		catch (InterruptedException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	/**
	 * These tests should be run with stalker db
	 */
	@Test
	public void testImportVoD2Stalker() {
		AppSettings settings = mock(AppSettings.class);

		when(settings.getStalkerDBServer()).thenReturn("192.168.1.29");
		when(settings.getStalkerDBUsername()).thenReturn("stalker");
		when(settings.getStalkerDBPassword()).thenReturn("1");

		ServerSettings serverSettings = mock(ServerSettings.class);
		when(serverSettings.getServerName()).thenReturn("localhost");
		restServiceReal.setServerSettings(serverSettings);

		String vodFolderPath = "webapps/junit/streams/vod_folder";

		File vodFolder = new File(vodFolderPath);
		vodFolder.mkdirs();
		assertTrue(vodFolder.exists());

		Scope scope = mock(Scope.class);
		String scopeName = "scope";
		when(scope.getName()).thenReturn(scopeName);

		restServiceReal.setScope(scope);

		restServiceReal.setAppSettings(settings);

		//Vod vod = new Vod();
		File file = new File(vodFolder, "test_file");
		String vodId = RandomStringUtils.randomNumeric(24);
		VoD newVod = new VoD("vodFile", "vodFile", file.getPath(), file.getName(), System.currentTimeMillis(), 0, 0, 6000,
				VoD.USER_VOD,vodId, null);
		DataStore store = new InMemoryDataStore("testdb");
		restServiceReal.setDataStore(store);

		assertNotNull(store.addVod(newVod));

		Process process = mock(Process.class);

		try {
			when(process.waitFor()).thenReturn(0);

			ProcessBuilderFactory factory = new ProcessBuilderFactory() {
				@Override
				public Process make(String... args) {
					return process;
				}
			};
			restServiceReal.setProcessBuilderFactory(factory);

			Result result = restServiceReal.importVoDsToStalker();

			assertFalse(result.isSuccess());

			when(settings.getVodFolder()).thenReturn(vodFolderPath);

			result = restServiceReal.importVoDsToStalker();

			assertTrue(result.isSuccess());

		}  catch (InterruptedException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	@Test
	public void testBugBroadcastStatisticNull() {
		Scope scope = mock(Scope.class);
		String scopeName = "scope";
		when(scope.getName()).thenReturn(scopeName);

		AntMediaApplicationAdapter app = new AntMediaApplicationAdapter();

		ApplicationContext context = mock(ApplicationContext.class);

		restServiceReal.setAppCtx(context);
		restServiceReal.setApplication(app);
		restServiceReal.setScope(scope);
		restServiceReal.setDataStore(new InMemoryDataStore("testdb"));


		BroadcastStatistics broadcastStatistics = restServiceReal.getBroadcastStatistics(null);
		assertNotNull(broadcastStatistics);
		assertEquals(-1, broadcastStatistics.totalHLSWatchersCount);
		assertEquals(-1, broadcastStatistics.totalDASHWatchersCount);
		assertEquals(-1, broadcastStatistics.totalRTMPWatchersCount);
		assertEquals(-1, broadcastStatistics.totalWebRTCWatchersCount);
	}
	
	@Test
	public void testTotalBroadcastStatistic() {
		Scope scope = mock(Scope.class);
		String scopeName = "scope";
		when(scope.getName()).thenReturn(scopeName);

		AntMediaApplicationAdapter app = new AntMediaApplicationAdapter();

		ApplicationContext context = mock(ApplicationContext.class);
		
		restServiceReal.setAppCtx(context);
		restServiceReal.setApplication(app);
		restServiceReal.setScope(scope);
		restServiceReal.setDataStore(new InMemoryDataStore("testdb"));
		
		IWebRTCAdaptor webrtcAdaptor = Mockito.mock(IWebRTCAdaptor.class);
		when(context.getBean(IWebRTCAdaptor.BEAN_NAME)).thenReturn(webrtcAdaptor);
		when(context.containsBean(IWebRTCAdaptor.BEAN_NAME)).thenReturn(true);
		
		HlsViewerStats hlsViewerStats = mock(HlsViewerStats.class);
		when(context.getBean(HlsViewerStats.BEAN_NAME)).thenReturn(hlsViewerStats);
		when(context.containsBean(HlsViewerStats.BEAN_NAME)).thenReturn(true);
		
		DashViewerStats dashViewerStats = mock(DashViewerStats.class);
		when(context.getBean(DashViewerStats.BEAN_NAME)).thenReturn(dashViewerStats);
		when(context.containsBean(DashViewerStats.BEAN_NAME)).thenReturn(true);
		
		BroadcastStatistics broadcastStatistics = restServiceReal.getBroadcastTotalStatistics();
		assertNotNull(broadcastStatistics);
		assertEquals(0, broadcastStatistics.totalHLSWatchersCount);
		assertEquals(0, broadcastStatistics.totalWebRTCWatchersCount);
		assertEquals(0, broadcastStatistics.totalDASHWatchersCount);
		
		when(context.containsBean(IWebRTCAdaptor.BEAN_NAME)).thenReturn(true);
		
	}

	@Test
	public void testEnableRecording() {
		BroadcastRestService restServiceSpy = Mockito.spy(restServiceReal);
		
		Mockito.doReturn(null).when(restServiceSpy).enableRecordMuxing(Mockito.anyString(), Mockito.anyBoolean(),Mockito.anyString(), anyInt());
		Mockito.doReturn(null).when(restServiceSpy).enableRecordMuxing(Mockito.anyString(), Mockito.anyBoolean(),Mockito.anyString(), anyInt());
		
		restServiceSpy.enableRecording("streamId", true, null, 0);
		verify(restServiceSpy).enableRecordMuxing("streamId", true,"mp4", 0);
		
		restServiceSpy.enableRecording("streamId", false, null, 0);
		verify(restServiceSpy).enableRecordMuxing("streamId", false,"mp4", 0);
		
		
		restServiceSpy.enableRecording("streamId", true, "webm", 0);
		verify(restServiceSpy).enableRecordMuxing("streamId", true,"webm", 0);
		
		restServiceSpy.enableRecording("streamId", false, "webm", 0);
		verify(restServiceSpy).enableRecordMuxing("streamId", false,"webm", 0);
		
		restServiceSpy.enableRecording("streamId", true, "mp4", 0);
		verify(restServiceSpy, times(2)).enableRecordMuxing("streamId", true,"mp4", 0);
		
		restServiceSpy.enableRecording("streamId", true, "mp4", 480);
		verify(restServiceSpy, times(1)).enableRecordMuxing("streamId", true,"mp4", 480);
		
	}

	@Test
	public void testWebRTCClientStats() {
		//create stream
		String streamId = RandomStringUtils.randomAlphanumeric(8);
		BroadcastRestService restServiceSpy = Mockito.spy(restServiceReal);
		//mock webrtc adaptor
		IWebRTCAdaptor webrtcAdaptor = Mockito.mock(IWebRTCAdaptor.class);

		Mockito.doReturn(webrtcAdaptor).when(restServiceSpy).getWebRTCAdaptor();

		//create random number of webrtc client stats 
		List<WebRTCClientStats> statsList = new ArrayList<>();
		int clientCount = (int)(Math.random()*999) + 70;

		for (int i = 0; i < clientCount; i++) {
			statsList.add(new WebRTCClientStats(500, 400, 40, 20, 0, 0, 0, "info", "192.168.1.1"));
		}

		Mockito.when(webrtcAdaptor.getWebRTCClientStats(Mockito.anyString())).thenReturn(statsList);

		//fetch 20 stats
		List<WebRTCClientStats> webRTCClientStatsList = restServiceSpy.getWebRTCClientStatsListV2(0, 20, streamId);

		//check 20 stats
		for(int i = 0; i< 20; i++) {
			assertEquals(statsList.get(i), webRTCClientStatsList.get(i));
		}

		//fetch 60 stats
		webRTCClientStatsList = restServiceSpy.getWebRTCClientStatsListV2(0, 60, streamId);
		//check return list 50
		assertEquals(50, webRTCClientStatsList.size());

		//check values
		for(int i = 0; i< 50; i++) {
			assertEquals(statsList.get(i), webRTCClientStatsList.get(i));
		}

		//request offset for minus value, it should return between 0 to size
		webRTCClientStatsList = restServiceSpy.getWebRTCClientStatsListV2(-10, 10, streamId);
		assertEquals(10, webRTCClientStatsList.size());
		//check values
		for(int i = 0; i< 10; i++) {
			assertEquals(statsList.get(i), webRTCClientStatsList.get(i));
		}


		webRTCClientStatsList = restServiceSpy.getWebRTCClientStatsListV2(20, 40, streamId);
		assertEquals(40, webRTCClientStatsList.size());
		//check values
		for(int i = 20; i < 60; i++) {
			assertEquals(statsList.get(i), webRTCClientStatsList.get(i-20));
		}


		webRTCClientStatsList = restServiceSpy.getWebRTCClientStatsListV2(clientCount, 40, streamId);
		assertEquals(0, webRTCClientStatsList.size());


		Mockito.doReturn(null).when(restServiceSpy).getWebRTCAdaptor();
		webRTCClientStatsList = restServiceSpy.getWebRTCClientStatsListV2(clientCount, 40, streamId);
		assertEquals(0, webRTCClientStatsList.size());

	}




	@Test
	public void testBugGetBroadcastStatistics() {
		Scope scope = mock(Scope.class);
		String scopeName = "junit";
		when(scope.getName()).thenReturn(scopeName);

		AntMediaApplicationAdapter app = mock(AntMediaApplicationAdapter.class);
		when(app.getScope()).thenReturn(scope);

		ApplicationContext context = mock(ApplicationContext.class);
		when(context.getBean(AntMediaApplicationAdapter.BEAN_NAME)).thenReturn(app);
		restServiceReal.setAppCtx(context);

		InMemoryDataStore dataStore = new InMemoryDataStore("testdb");
		restServiceReal.setDataStore(dataStore);
		Broadcast broadcast = new Broadcast();
		broadcast.setStatus(AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING);
		String streamId = dataStore.save(broadcast);

		dataStore.updateHLSViewerCount(streamId, 30);
		BroadcastStatistics broadcastStatistics = restServiceReal.getBroadcastStatistics(streamId);
		assertNotNull(broadcastStatistics);
		assertEquals(30, broadcastStatistics.totalHLSWatchersCount);

	}


	@Test
	public void testGetToken() {

		InMemoryDataStore datastore = mock(InMemoryDataStore.class);
		restServiceReal.setDataStore(datastore);
		String streamId = "stream " + (int)(Math.random() * 1000);


		ApplicationContext appContext = mock(ApplicationContext.class);

		when(appContext.containsBean(ITokenService.BeanName.TOKEN_SERVICE.toString())).thenReturn(false);
		Object tokenReturn = restServiceReal.getTokenV2(streamId, 123432, Token.PLAY_TOKEN, "testRoom").getEntity();
		assertTrue(tokenReturn instanceof Result);
		Result result = (Result) tokenReturn;
		//it should false, because appContext is null
		assertFalse(result.isSuccess());	 


		restServiceReal.setAppCtx(appContext);
		tokenReturn = restServiceReal.getTokenV2(streamId, 123432, Token.PLAY_TOKEN, "testRoom").getEntity();
		assertTrue(tokenReturn instanceof Result);
		result = (Result) tokenReturn;
		//it should be false, because there is no token service in the context
		assertFalse(result.isSuccess());	

		ITokenService tokenService = mock(ITokenService.class);
		{
			when(appContext.containsBean(ITokenService.BeanName.TOKEN_SERVICE.toString())).thenReturn(true);
			when(tokenService.createToken(streamId, 123432, Token.PLAY_TOKEN, "testRoom"))
			.thenReturn(null);
			when(appContext.getBean(ITokenService.BeanName.TOKEN_SERVICE.toString())).thenReturn(tokenService);

			tokenReturn = restServiceReal.getTokenV2(streamId, 123432, Token.PLAY_TOKEN, "testRoom").getEntity();
			assertTrue(tokenReturn instanceof Result);
			result = (Result) tokenReturn;
			//it should be false, becuase getTokenV2 service returns null
			assertFalse(result.isSuccess());	
		}

		Token token = new Token();
		token.setStreamId(streamId);
		token.setExpireDate(123432);
		token.setTokenId(RandomStringUtils.randomAlphabetic(8));
		token.setType(Token.PLAY_TOKEN);

		{
			when(tokenService.createToken(streamId, 123432, Token.PLAY_TOKEN, "testRoom" ))
			.thenReturn(token);
			restServiceReal.setAppCtx(appContext);


			tokenReturn = (Object) restServiceReal.getTokenV2(streamId, 123432, Token.PLAY_TOKEN, "testRoom").getEntity();
			assertTrue(tokenReturn instanceof Result);
			result = (Result) tokenReturn;
			assertFalse(result.isSuccess());
		}

		//check create token is called
		Mockito.verify(tokenService, Mockito.times(2)).createToken(streamId, 123432, Token.PLAY_TOKEN, "testRoom");
		//check saveToken is called
		Mockito.verify(datastore).saveToken(token);

		{	
			//set stream id null and it should return false
			tokenReturn = restServiceReal.getTokenV2(null, 0, Token.PLAY_TOKEN, "testRoom").getEntity();
			assertTrue(tokenReturn instanceof Result);
			result = (Result) tokenReturn;
			assertFalse(result.isSuccess());	
		}
		
		{	
			//set token type null and it should return false
			tokenReturn = restServiceReal.getTokenV2(streamId, 123432, null, "testRoom").getEntity();
			assertTrue(tokenReturn instanceof Result);
			result = (Result) tokenReturn;
			assertFalse(result.isSuccess());
		}

		Mockito.when(datastore.saveToken(Mockito.any())).thenReturn(true);
		tokenReturn = (Object) restServiceReal.getTokenV2(streamId, 123432, Token.PLAY_TOKEN, "testRoom").getEntity();
		assertTrue(tokenReturn instanceof Token);
		assertEquals(((Token)tokenReturn).getTokenId(), token.getTokenId());

	}
	
	@Test
	public void testGetJwtToken() {

		InMemoryDataStore datastore = mock(InMemoryDataStore.class);
		restServiceReal.setDataStore(datastore);
		String streamId = "stream " + (int)(Math.random() * 1000);

		AppSettings settings = mock(AppSettings.class);
		settings.setJwtStreamSecretKey("testtesttesttesttesttesttesttest");
		restServiceReal.setAppSettings(settings);

		ApplicationContext appContext = mock(ApplicationContext.class);

		
		when(appContext.containsBean(ITokenService.BeanName.TOKEN_SERVICE.toString())).thenReturn(false);
		Object tokenReturn = restServiceReal.getJwtTokenV2(streamId, 123432, Token.PLAY_TOKEN, "testRoom").getEntity();
		assertTrue(tokenReturn instanceof Result);
		Result result = (Result) tokenReturn;
		//it should false, because appContext is null
		assertFalse(result.isSuccess());	 


		restServiceReal.setAppCtx(appContext);
		tokenReturn = restServiceReal.getJwtTokenV2(streamId, 123432, Token.PLAY_TOKEN, "testRoom").getEntity();
		assertTrue(tokenReturn instanceof Result);
		result = (Result) tokenReturn;
		//it should be false, because there is no token service in the context
		assertFalse(result.isSuccess());	

		ITokenService tokenService = mock(ITokenService.class);
		{
			when(appContext.containsBean(ITokenService.BeanName.TOKEN_SERVICE.toString())).thenReturn(true);
			when(tokenService.createJwtToken(streamId, 123432, Token.PLAY_TOKEN, "testRoom"))
			.thenReturn(null);
			when(appContext.getBean(ITokenService.BeanName.TOKEN_SERVICE.toString())).thenReturn(tokenService);

			tokenReturn = restServiceReal.getJwtTokenV2(streamId, 123432, Token.PLAY_TOKEN, "testRoom").getEntity();
			assertTrue(tokenReturn instanceof Result);
			result = (Result) tokenReturn;
			//it should be false, because token service returns null
			assertFalse(result.isSuccess());	
		}

		Token token = new Token();
		token.setStreamId(streamId);
		token.setExpireDate(123432);
		token.setTokenId(RandomStringUtils.randomAlphabetic(8));
		token.setType(Token.PLAY_TOKEN);

		{
			when(tokenService.createJwtToken(streamId, 123432, Token.PLAY_TOKEN, "testRoom" ))
			.thenReturn(token);
			restServiceReal.setAppCtx(appContext);
			
			tokenReturn = (Object) restServiceReal.getJwtTokenV2(streamId, 123432, Token.PLAY_TOKEN, "testRoom").getEntity();
			assertTrue(tokenReturn instanceof Token);
			token = (Token) tokenReturn;
			assertEquals(((Token)tokenReturn).getTokenId(), token.getTokenId());
		}

		//check create token is called
		Mockito.verify(tokenService, Mockito.times(2)).createJwtToken(streamId, 123432, Token.PLAY_TOKEN, "testRoom");

		{	
			//set stream id null and it should return false
			tokenReturn = restServiceReal.getJwtTokenV2(null, 0, Token.PLAY_TOKEN, "testRoom").getEntity();
			assertTrue(tokenReturn instanceof Result);
			result = (Result) tokenReturn;
			assertFalse(result.isSuccess());	
		}
		
		{	
			//set token type null and it should return false
			tokenReturn = restServiceReal.getJwtTokenV2(streamId, 123432, null, "testRoom").getEntity();
			assertTrue(tokenReturn instanceof Result);
			result = (Result) tokenReturn;
			assertFalse(result.isSuccess());
		}

		Mockito.when(datastore.saveToken(Mockito.any())).thenReturn(true);
		tokenReturn = (Object) restServiceReal.getJwtTokenV2(streamId, 123432, Token.PLAY_TOKEN, "testRoom").getEntity();
		assertTrue(tokenReturn instanceof Token);
		assertEquals(((Token)tokenReturn).getTokenId(), token.getTokenId());

	}

	@Test
	public void testSettingsListenerHookURL() {
		AppSettings settings = mock(AppSettings.class);
		String hookURL = "http://url_hook";
		when(settings.getListenerHookURL()).thenReturn(hookURL);

		String serverName = "fually.qualified.domain.name";
		
		ServerSettings serverSettings = mock(ServerSettings.class);
		when(serverSettings.getServerName()).thenReturn(serverName);
		restServiceReal.setServerSettings(serverSettings);

		Scope scope = mock(Scope.class);
		String scopeName = "scope";
		when(scope.getName()).thenReturn(scopeName);

		restServiceReal.setScope(scope);

		restServiceReal.setAppSettings(settings);

		Broadcast broadcast = new Broadcast(null, "name");
		DataStore store = new InMemoryDataStore("testdb");
		restServiceReal.setDataStore(store);
		Broadcast createBroadcast = (Broadcast) restServiceReal.createBroadcast(broadcast, false).getEntity();

		assertEquals(hookURL, createBroadcast.getListenerHookURL());

		Broadcast broadcastTmp = (Broadcast) restServiceReal.getBroadcast(createBroadcast.getStreamId()).getEntity();

		assertEquals(hookURL, broadcastTmp.getListenerHookURL());


		//this makes the test code enter getHostAddress method
		when(serverSettings.getServerName()).thenReturn(null);

		

		Response response = (Response) restServiceReal.createBroadcast(broadcast, false);
		//return bad request because there is already a broadcast with the same id
		assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());

		//this case makes the test code get address from static field
		response = restServiceReal.createBroadcast(broadcast, false);
		//return bad request because there is already a broadcast with the same id
		assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
	}

	
	@Test
	public void testRemoveEndpoint() 
	{
		AppSettings settings = mock(AppSettings.class);
		String serverName = "fully.qualified.domain.name";
		restServiceReal.setAppSettings(settings);
		
		ServerSettings serverSettings = mock(ServerSettings.class);
		when(serverSettings.getServerName()).thenReturn(serverName);
		restServiceReal.setServerSettings(serverSettings);


		Broadcast broadcast = new Broadcast(null, "name");
		DataStore store = new InMemoryDataStore("testdb");
		restServiceReal.setDataStore(store);

		Scope scope = mock(Scope.class);
		String scopeName = "scope";
		when(scope.getName()).thenReturn(scopeName);
		restServiceReal.setScope(scope);
		
		assertFalse(restServiceReal.removeEndpoint("any_stream_not_registered", "rtmp://test.endpoint.url/server_test").isSuccess());
		String streamId = null;
		{
			Broadcast createBroadcast = (Broadcast) restServiceReal.createBroadcast(broadcast, false).getEntity();
			streamId = createBroadcast.getStreamId();
			assertNotNull(streamId);
			
			String endpointURL = "rtmp://test.endpoint.url/test";
			Result result = restServiceReal.addEndpointV2(streamId, endpointURL);
			assertTrue(result.isSuccess());
			
			assertEquals(1, store.get(streamId).getEndPointList().size());
			
			assertTrue(restServiceReal.removeEndpoint(streamId, endpointURL).isSuccess());
		}
		
		{
			BroadcastRestService restServiceSpy = Mockito.spy(restServiceReal);
			MuxAdaptor muxAdaptor = Mockito.mock(MuxAdaptor.class);
			
			Mockito.doReturn(muxAdaptor).when(restServiceSpy).getMuxAdaptor(streamId);
			
			Mockito.when(muxAdaptor.stopRtmpStreaming(Mockito.anyString(), Mockito.eq(0))).thenReturn(new Result(true));
			
			String endpointURL = "rtmp://test.endpoint.url/test";
			Result result = restServiceSpy.addEndpointV2(streamId, endpointURL);
			assertTrue(result.isSuccess());
			
			assertEquals(1, store.get(streamId).getEndPointList().size());
			
			store.updateStatus(streamId, AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING);
			
			assertTrue(restServiceSpy.removeEndpoint(streamId, endpointURL).isSuccess());
		}

	}
	
	@Test
	public void testRemoveEndpointV2() 
	{
		ApplicationContext context = mock(ApplicationContext.class);
		restServiceReal.setAppCtx(context);
		when(context.containsBean(any())).thenReturn(false);
		
		AppSettings settings = mock(AppSettings.class);
		String serverName = "fully.qualified.domain.name";
		String serverHostAddress = "127.0.1.1";
		restServiceReal.setAppSettings(settings);
		
		ServerSettings serverSettings = mock(ServerSettings.class);
		when(serverSettings.getServerName()).thenReturn(serverName);
		when(serverSettings.getHostAddress()).thenReturn(serverHostAddress);
		restServiceReal.setServerSettings(serverSettings);


		Broadcast broadcast1 = new Broadcast(null, "name1");
		Broadcast broadcast2 = new Broadcast(null, "name2");
		Broadcast broadcast3 = new Broadcast(null, "name3");
		Broadcast broadcast4 = new Broadcast(null, "name4");
		MongoStore store = new MongoStore("localhost", "", "", "testdb");
		restServiceReal.setDataStore(store);

		Scope scope = mock(Scope.class);
		String scopeName = "scope";
		when(scope.getName()).thenReturn(scopeName);
		restServiceReal.setScope(scope);
		
		assertFalse(restServiceReal.removeEndpointV2("any_stream_not_registered", "rtmp://test.endpoint.url/server_test", 0).isSuccess());
		String streamId = null;
		// Standallone Remove RTMP Endpoint with same origin and broadcast
		{			
			Broadcast createBroadcast = (Broadcast) restServiceReal.createBroadcast(broadcast1, false).getEntity();
			streamId = createBroadcast.getStreamId();
			assertNotNull(streamId);
			
			String endpointURL = "rtmp://test.endpoint.url/test";
			
			Endpoint endpoint = new Endpoint();
			endpoint.setRtmpUrl(endpointURL);
			
			Result result = restServiceReal.addEndpointV3(streamId, endpoint, 0);
			assertTrue(result.isSuccess());
			
			assertEquals(1, store.get(streamId).getEndPointList().size());
			
			serverHostAddress = "127.0.1.1";
			when(serverSettings.getHostAddress()).thenReturn(serverHostAddress);
			
			assertTrue(restServiceReal.removeEndpointV2(streamId, store.get(streamId).getEndPointList().get(0).getEndpointServiceId(), 0).isSuccess());
			
			assertEquals(0, store.get(streamId).getEndPointList().size());
		}
		
		// Standallone Remove RTMP Endpoint with different origin and broadcast
		{	
			Broadcast createBroadcast = (Broadcast) restServiceReal.createBroadcast(broadcast2, false).getEntity();
			streamId = createBroadcast.getStreamId();
			assertNotNull(streamId);
			
			String endpointURL = "rtmp://test.endpoint.url/test";
			
			Endpoint endpoint = new Endpoint();
			endpoint.setRtmpUrl(endpointURL);
			
			Result result = restServiceReal.addEndpointV3(streamId, endpoint, 0);
			assertTrue(result.isSuccess());
			
			assertEquals(1, store.get(streamId).getEndPointList().size());
			
			serverHostAddress = "55.55.55.55";
			when(serverSettings.getHostAddress()).thenReturn(serverHostAddress);
			
			assertTrue(restServiceReal.removeEndpointV2(streamId, store.get(streamId).getEndPointList().get(0).getEndpointServiceId(), 0).isSuccess());
			
			assertEquals(0, store.get(streamId).getEndPointList().size());
		}
		
		// enable Cluster mode with same origin and broadcast
		{
			// Set Default Host Address
			serverHostAddress = "127.0.1.1";
			when(serverSettings.getHostAddress()).thenReturn(serverHostAddress);
			
			Broadcast createBroadcast = (Broadcast) restServiceReal.createBroadcast(broadcast3, false).getEntity();
			streamId = createBroadcast.getStreamId();
			assertNotNull(streamId);
			
			BroadcastRestService restServiceSpy = Mockito.spy(restServiceReal);
			MuxAdaptor muxAdaptor = Mockito.mock(MuxAdaptor.class);
			
			Mockito.doReturn(muxAdaptor).when(restServiceSpy).getMuxAdaptor(streamId);
			
			Mockito.when(muxAdaptor.stopRtmpStreaming(Mockito.anyString(), Mockito.eq(0))).thenReturn(new Result(true));
			
			
			String endpointURL = "rtmp://test.endpoint.url/test";
			
			Endpoint endpoint = new Endpoint();
			endpoint.setRtmpUrl(endpointURL);

			Result result = restServiceSpy.addEndpointV3(streamId, endpoint, 0);
			assertTrue(result.isSuccess());
			
			assertEquals(1, store.get(streamId).getEndPointList().size());
			
			store.updateStatus(streamId, AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING);
			
			when(context.containsBean(any())).thenReturn(true);
			serverHostAddress = "127.0.1.1";
			when(serverSettings.getHostAddress()).thenReturn(serverHostAddress);
			
			assertTrue(restServiceSpy.removeEndpointV2(streamId, store.get(streamId).getEndPointList().get(0).getEndpointServiceId(), 0).isSuccess());
			assertEquals(0, store.get(streamId).getEndPointList().size());
		}
		
		// enable Cluster mode with different origin and broadcast
		{
			Broadcast createBroadcast = (Broadcast) restServiceReal.createBroadcast(broadcast4, false).getEntity();
			streamId = createBroadcast.getStreamId();
			assertNotNull(streamId);
			
			BroadcastRestService restServiceSpy = Mockito.spy(restServiceReal);
			MuxAdaptor muxAdaptor = Mockito.mock(MuxAdaptor.class);
			
			Mockito.doReturn(muxAdaptor).when(restServiceSpy).getMuxAdaptor(streamId);
			
			Mockito.when(muxAdaptor.stopRtmpStreaming(Mockito.anyString(), Mockito.eq(0))).thenReturn(new Result(true));
			String endpointURL = "rtmp://test.endpoint.url/test";
					
			Endpoint endpoint = new Endpoint();
			endpoint.setRtmpUrl(endpointURL);

			Result result = restServiceSpy.addEndpointV3(streamId, endpoint, 0);
			assertTrue(result.isSuccess());
					
			assertEquals(1, store.get(streamId).getEndPointList().size());
			
			store.updateStatus(streamId, AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING);
			
			when(context.containsBean(any())).thenReturn(true);
			serverHostAddress = "55.55.55.55";
			when(serverSettings.getHostAddress()).thenReturn(serverHostAddress);
					
			assertFalse(restServiceSpy.removeEndpointV2(streamId, store.get(streamId).getEndPointList().get(0).getEndpointServiceId(), 0).isSuccess());
			assertEquals(1, store.get(streamId).getEndPointList().size());
		}
				
		{
			// Set Default Host Address
			serverHostAddress = "127.0.1.1";
			when(serverSettings.getHostAddress()).thenReturn(serverHostAddress);
			
			Endpoint endpoint6 = new Endpoint();
			endpoint6.setRtmpUrl("rtmp://test.endpoint.url/any_stream_test");
			
			assertFalse(restServiceReal.addEndpointV3("Not_regsitered_stream_id", endpoint6, 0).isSuccess());
			
			assertEquals(1, store.get(streamId).getEndPointList().size());
		}

	}
	
	@Test
	public void testAddEndpoint() {
		AppSettings settings = mock(AppSettings.class);
		String serverName = "fully.qualified.domain.name";
		restServiceReal.setAppSettings(settings);
		
		ServerSettings serverSettings = mock(ServerSettings.class);
		when(serverSettings.getServerName()).thenReturn(serverName);
		restServiceReal.setServerSettings(serverSettings);


		Broadcast broadcast = new Broadcast(null, "name");
		DataStore store = new InMemoryDataStore("testdb");
		restServiceReal.setDataStore(store);

		Scope scope = mock(Scope.class);
		String scopeName = "scope";
		when(scope.getName()).thenReturn(scopeName);
		restServiceReal.setScope(scope);

		Broadcast createBroadcast = (Broadcast) restServiceReal.createBroadcast(broadcast, false).getEntity();
		String streamId = createBroadcast.getStreamId();
		assertNotNull(streamId);

		String endpointURL = "rtmp://test.endpoint.url/test";
		Result result = restServiceReal.addEndpointV2(streamId, endpointURL);
		assertTrue(result.isSuccess());
		
		assertFalse(restServiceReal.addEndpointV2(streamId, null).isSuccess());

		Broadcast broadcast2 = (Broadcast) restServiceReal.getBroadcast(streamId).getEntity();
		assertEquals(broadcast.getStreamId(), broadcast2.getStreamId());

		assertEquals(1, broadcast2.getEndPointList().size());
		Endpoint endpoint = broadcast2.getEndPointList().get(0);
		assertEquals(endpointURL, endpoint.getRtmpUrl());
		assertEquals("generic", endpoint.getType());
		
		{
			BroadcastRestService restServiceSpy = Mockito.spy(restServiceReal);
			MuxAdaptor muxAdaptor = Mockito.mock(MuxAdaptor.class);
			
			Mockito.doReturn(muxAdaptor).when(restServiceSpy).getMuxAdaptor(broadcast.getStreamId());
			
			Mockito.when(muxAdaptor.startRtmpStreaming(Mockito.anyString(), Mockito.eq(0))).thenReturn(new Result(true));
			
			store.updateStatus(broadcast.getStreamId(), AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING);
			assertTrue(restServiceSpy.addEndpointV2(streamId, "rtmp://test.endpoint.url/any_stream_test").isSuccess());
		}
		
		{
			assertFalse(restServiceReal.addEndpointV2("Not_regsitered_stream_id",  "rtmp://test.endpoint.url/any_stream_test").isSuccess());
		}
	}

	@Test
	public void testAddEndpointV2() {
		
		ApplicationContext context = mock(ApplicationContext.class);
		restServiceReal.setAppCtx(context);
		when(context.containsBean(any())).thenReturn(false);
		
		AppSettings settings = mock(AppSettings.class);
		String serverName = "fully.qualified.domain.name";
		String serverHostAddress = "127.0.1.1";
		restServiceReal.setAppSettings(settings);
		
		ServerSettings serverSettings = mock(ServerSettings.class);
		when(serverSettings.getServerName()).thenReturn(serverName);
		when(serverSettings.getHostAddress()).thenReturn(serverHostAddress);
		restServiceReal.setServerSettings(serverSettings);


		Broadcast broadcast = new Broadcast(null, "name");
		DataStore store = new InMemoryDataStore("testdb");
		restServiceReal.setDataStore(store);

		Scope scope = mock(Scope.class);
		String scopeName = "scope";
		when(scope.getName()).thenReturn(scopeName);
		restServiceReal.setScope(scope);

		Broadcast createBroadcast = (Broadcast) restServiceReal.createBroadcast(broadcast, false).getEntity();
		String streamId = createBroadcast.getStreamId();
		assertNotNull(streamId);
		
		String endpointURL = "rtmp://test.endpoint.url/test";
		
		Endpoint endpoint = new Endpoint();
		endpoint.setRtmpUrl(endpointURL);

		Result result = restServiceReal.addEndpointV3(streamId, endpoint, 0);
		assertTrue(result.isSuccess());
		assertNotNull(result.getDataId());
		String endpointServiceId = result.getDataId();
		
		endpoint = null;
		
		assertFalse(restServiceReal.addEndpointV3(streamId, endpoint, 0).isSuccess());

		Broadcast broadcast2 = (Broadcast) restServiceReal.getBroadcast(streamId).getEntity();
		assertEquals(broadcast.getStreamId(), broadcast2.getStreamId());

		assertEquals(1, broadcast2.getEndPointList().size());
		Endpoint endpoint2 = broadcast2.getEndPointList().get(0);
		assertEquals(endpointURL, endpoint2.getRtmpUrl());
		assertEquals("generic", endpoint2.getType());
		assertEquals(endpointServiceId, endpoint2.getEndpointServiceId());
		
		// Standallone Add RTMP Endpoint with same origin and broadcast
		{
			BroadcastRestService restServiceSpy = Mockito.spy(restServiceReal);
			MuxAdaptor muxAdaptor = Mockito.mock(MuxAdaptor.class);
			
			Mockito.doReturn(muxAdaptor).when(restServiceSpy).getMuxAdaptor(broadcast.getStreamId());
			
			Mockito.when(muxAdaptor.startRtmpStreaming(Mockito.anyString(), Mockito.eq(0))).thenReturn(new Result(true));
			
			Endpoint endpoint3 = new Endpoint();
			endpoint3.setRtmpUrl("rtmp://test.endpoint.url/any_stream_test");
			
			store.updateStatus(broadcast.getStreamId(), AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING);
			assertTrue(restServiceSpy.addEndpointV3(streamId, endpoint3, 0).isSuccess());
		}
		
		// Standallone Add RTMP Endpoint with different origin and broadcast
		{
			serverHostAddress = "55.55.55.55";
			when(serverSettings.getHostAddress()).thenReturn(serverHostAddress);
			
			BroadcastRestService restServiceSpy = Mockito.spy(restServiceReal);
			MuxAdaptor muxAdaptor = Mockito.mock(MuxAdaptor.class);
			
			Mockito.doReturn(muxAdaptor).when(restServiceSpy).getMuxAdaptor(broadcast.getStreamId());
			
			Mockito.when(muxAdaptor.startRtmpStreaming(Mockito.anyString(), Mockito.eq(0))).thenReturn(new Result(true));
			
			Endpoint endpoint3 = new Endpoint();
			//This is already in the endpoints list, so it won't be added.
			endpoint3.setRtmpUrl("rtmp://test.endpoint.url/any_stream_test");
			
			store.updateStatus(broadcast.getStreamId(), AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING);

			assertFalse(restServiceSpy.addEndpointV3(streamId, endpoint3, 0).isSuccess());

			Endpoint endpoint3true = new Endpoint();
			//This is not included in the endpoints list, so it should be true.
			endpoint3true.setRtmpUrl("rtmp://test.endpoint.url/any_other_stream_test");
			
			assertTrue(restServiceSpy.addEndpointV3(streamId, endpoint3true, 0).isSuccess());
		}
		
		// enable Cluster mode with same origin and broadcast
		{
			when(context.containsBean(any())).thenReturn(true);
			serverHostAddress = "127.0.1.1";
			when(serverSettings.getHostAddress()).thenReturn(serverHostAddress);

			
			BroadcastRestService restServiceSpy = Mockito.spy(restServiceReal);
			MuxAdaptor muxAdaptor = Mockito.mock(MuxAdaptor.class);
			
			Mockito.doReturn(muxAdaptor).when(restServiceSpy).getMuxAdaptor(broadcast.getStreamId());
			
			Mockito.when(muxAdaptor.startRtmpStreaming(Mockito.anyString(), Mockito.eq(0))).thenReturn(new Result(true));
			
			Endpoint endpoint4 = new Endpoint();
			//This is already in the endpoints list, so it won't be added.
			endpoint4.setRtmpUrl("rtmp://test.endpoint.url/any_stream_test");
			
			store.updateStatus(broadcast.getStreamId(), AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING);

			assertFalse(restServiceSpy.addEndpointV3(streamId, endpoint4, 0).isSuccess());

			Endpoint endpoint4true = new Endpoint();
			//This is not included in the endpoints list, so it should be true.
			endpoint4true.setRtmpUrl("rtmp://test.endpoint.url/any_stream_test2");
			
			assertTrue(restServiceSpy.addEndpointV3(streamId, endpoint4true, 0).isSuccess());
		}
		
		// enable Cluster mode with different origin and broadcast
		{
			when(context.containsBean(any())).thenReturn(true);
			serverHostAddress = "55.55.55.55";
			when(serverSettings.getHostAddress()).thenReturn(serverHostAddress);
			
			BroadcastRestService restServiceSpy = Mockito.spy(restServiceReal);
			MuxAdaptor muxAdaptor = Mockito.mock(MuxAdaptor.class);
			
			Mockito.doReturn(muxAdaptor).when(restServiceSpy).getMuxAdaptor(broadcast.getStreamId());
			
			Mockito.when(muxAdaptor.startRtmpStreaming(Mockito.anyString(), Mockito.eq(0))).thenReturn(new Result(true));
			
			Endpoint endpoint5 = new Endpoint();
			endpoint5.setRtmpUrl("rtmp://test.endpoint.url/any_stream_test");
			
			store.updateStatus(broadcast.getStreamId(), AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING);
			assertFalse(restServiceSpy.addEndpointV3(streamId, endpoint5, 0).isSuccess());
			
		}
		
		{
			Endpoint endpoint6 = new Endpoint();
			endpoint6.setRtmpUrl("rtmp://test.endpoint.url/any_stream_test");
			
			assertFalse(restServiceReal.addEndpointV3("Not_regsitered_stream_id", endpoint6, 0).isSuccess());
		}

	}

	@Test
	public void testDeleteBroadcast() {
		AppSettings settings = mock(AppSettings.class);
		String serverName = "fully.qualified.domain.name";
		restServiceReal.setAppSettings(settings);
		ServerSettings serverSettings = mock(ServerSettings.class);
		when(serverSettings.getServerName()).thenReturn(serverName);
		restServiceReal.setServerSettings(serverSettings);

		ApplicationContext context = mock(ApplicationContext.class);
		restServiceReal.setAppCtx(context);
		when(context.containsBean(any())).thenReturn(false);


		DataStore store = new InMemoryDataStore("testdb");
		restServiceReal.setDataStore(store);

		Scope scope = mock(Scope.class);
		String scopeName = "scope";
		when(scope.getName()).thenReturn(scopeName);
		restServiceReal.setScope(scope);

		AntMediaApplicationAdapter appAdaptor = Mockito.spy(new AntMediaApplicationAdapter());
		IClientBroadcastStream broadcastStream = mock(IClientBroadcastStream.class);
		IStreamCapableConnection streamCapableConnection = mock(IStreamCapableConnection.class);

		when(broadcastStream.getConnection()).thenReturn(streamCapableConnection);
		Mockito.doReturn(broadcastStream).when(appAdaptor).getBroadcastStream(Mockito.any(), Mockito.anyString());

		restServiceReal.setApplication(appAdaptor);

		int streamCount = 15; 
		for (int i = 0; i < streamCount; i++) {
			Broadcast broadcast = new Broadcast(null, "name");
			Broadcast broadcastCreated = (Broadcast) restServiceReal.createBroadcast(broadcast, false).getEntity();
			assertNotNull(broadcastCreated.getStreamId());

			Broadcast broadcast2 = (Broadcast) restServiceReal.getBroadcast(broadcastCreated.getStreamId()).getEntity();
			assertNotNull(broadcast2.getStreamId());
		}

		List<Broadcast> broadcastList = restServiceReal.getBroadcastList(0, 20, null, null, null, null);
		assertEquals(streamCount, broadcastList.size());

		for (Broadcast item: broadcastList) {
			Result result = restServiceReal.deleteBroadcast(item.getStreamId());
			assertTrue(result.isSuccess());
		}

		Mockito.verify(streamCapableConnection, Mockito.times(streamCount)).close();

		// Add test for Cluster
		restServiceReal.setAppCtx(context);
		when(context.containsBean(any())).thenReturn(true);
				
		// isCluster true / broadcast origin address != server host address / status = broadcasting
		{
			Broadcast broadcast = new Broadcast();
			broadcast.setOriginAdress("55.55.55.55");
			broadcast.setStatus(AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING);
			store.save(broadcast);
			
			when(restServiceReal.getServerSettings().getHostAddress()).thenReturn("127.0.0.1");
			
			Result result = restServiceReal.deleteBroadcast(broadcast.getStreamId());
			assertFalse(result.isSuccess());
		}
		
		// isCluster true / broadcast origin address == server host address / status = broadcasting
		{
			Broadcast broadcast = new Broadcast();
			broadcast.setOriginAdress("55.55.55.55");
			broadcast.setStatus(AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING);
			store.save(broadcast);
			
			when(restServiceReal.getServerSettings().getHostAddress()).thenReturn("55.55.55.55");
			
			Result result = restServiceReal.deleteBroadcast(broadcast.getStreamId());
			assertTrue(result.isSuccess());
		}
		
		// isCluster true / broadcast origin address != server host address / status = finished
		{
			when(restServiceReal.getServerSettings().getHostAddress()).thenReturn("127.0.0.1");
			
			Broadcast broadcast = new Broadcast();
			broadcast.setOriginAdress("55.55.55.55");
			broadcast.setStatus(AntMediaApplicationAdapter.BROADCAST_STATUS_FINISHED);
			store.save(broadcast);
			
			when(restServiceReal.getServerSettings().getHostAddress()).thenReturn("127.0.0.1");
			
			Result result = restServiceReal.deleteBroadcast(broadcast.getStreamId());
			assertTrue(result.isSuccess());
		}
		
		// isCluster true / broadcast origin address == server host address / status = finished
		{
			when(restServiceReal.getServerSettings().getHostAddress()).thenReturn("127.0.0.1");
			
			Broadcast broadcast = new Broadcast();
			broadcast.setOriginAdress("55.55.55.55");
			broadcast.setStatus(AntMediaApplicationAdapter.BROADCAST_STATUS_FINISHED);
			store.save(broadcast);
			
			when(restServiceReal.getServerSettings().getHostAddress()).thenReturn("55.55.55.55");
			
			Result result = restServiceReal.deleteBroadcast(broadcast.getStreamId());
			assertTrue(result.isSuccess());
		}

	}

	@Test
	public void testDeleteBroadcasts() {
		AppSettings settings = mock(AppSettings.class);
		String serverName = "fully.qualified.domain.name";
		restServiceReal.setAppSettings(settings);
		ServerSettings serverSettings = mock(ServerSettings.class);
		when(serverSettings.getServerName()).thenReturn(serverName);
		restServiceReal.setServerSettings(serverSettings);

		ApplicationContext context = mock(ApplicationContext.class);
		restServiceReal.setAppCtx(context);
		when(context.containsBean(any())).thenReturn(false);


		DataStore store = new InMemoryDataStore("testdb");
		restServiceReal.setDataStore(store);

		Scope scope = mock(Scope.class);
		String scopeName = "scope";
		when(scope.getName()).thenReturn(scopeName);
		restServiceReal.setScope(scope);

		AntMediaApplicationAdapter appAdaptor = Mockito.spy(new AntMediaApplicationAdapter());
		IClientBroadcastStream broadcastStream = mock(IClientBroadcastStream.class);
		IStreamCapableConnection streamCapableConnection = mock(IStreamCapableConnection.class);

		when(broadcastStream.getConnection()).thenReturn(streamCapableConnection);
		Mockito.doReturn(broadcastStream).when(appAdaptor).getBroadcastStream(Mockito.any(), Mockito.anyString());

		restServiceReal.setApplication(appAdaptor);

		int streamCount = 15;
		for (int i = 0; i < streamCount; i++) {
			Broadcast broadcast = new Broadcast(null, "name");
			Broadcast broadcastCreated = (Broadcast) restServiceReal.createBroadcast(broadcast, false).getEntity();
			assertNotNull(broadcastCreated.getStreamId());

			Broadcast broadcast2 = (Broadcast) restServiceReal.getBroadcast(broadcastCreated.getStreamId()).getEntity();
			assertNotNull(broadcast2.getStreamId());
		}

		List<Broadcast> broadcastList = restServiceReal.getBroadcastList(0, 20, null, null, null, null);
		assertEquals(streamCount, broadcastList.size());

		for (Broadcast item: broadcastList) {
			Result result = restServiceReal.deleteBroadcasts(new String[] {item.getStreamId()});
			assertTrue(result.isSuccess());
		}

		Mockito.verify(streamCapableConnection, Mockito.times(streamCount)).close();

		// Add test for Cluster
		restServiceReal.setAppCtx(context);
		when(context.containsBean(any())).thenReturn(true);

		// isCluster true / broadcast origin address != server host address / status = broadcasting
		{
			Broadcast broadcast = new Broadcast();
			broadcast.setOriginAdress("55.55.55.55");
			broadcast.setStatus(AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING);
			store.save(broadcast);

			when(restServiceReal.getServerSettings().getHostAddress()).thenReturn("127.0.0.1");

			Result result = restServiceReal.deleteBroadcasts(new String[] {broadcast.getStreamId()});
			assertFalse(result.isSuccess());
		}

		// isCluster true / broadcast origin address == server host address / status = broadcasting
		{
			Broadcast broadcast = new Broadcast();
			broadcast.setOriginAdress("55.55.55.55");
			broadcast.setStatus(AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING);
			store.save(broadcast);

			when(restServiceReal.getServerSettings().getHostAddress()).thenReturn("55.55.55.55");

			Result result = restServiceReal.deleteBroadcasts(new String[] {broadcast.getStreamId()});
			assertTrue(result.isSuccess());
		}

		// isCluster true / broadcast origin address != server host address / status = finished
		{
			when(restServiceReal.getServerSettings().getHostAddress()).thenReturn("127.0.0.1");

			Broadcast broadcast = new Broadcast();
			broadcast.setOriginAdress("55.55.55.55");
			broadcast.setStatus(AntMediaApplicationAdapter.BROADCAST_STATUS_FINISHED);
			store.save(broadcast);

			when(restServiceReal.getServerSettings().getHostAddress()).thenReturn("127.0.0.1");

			Result result = restServiceReal.deleteBroadcasts(new String[] {broadcast.getStreamId()});
			assertTrue(result.isSuccess());
		}

		// isCluster true / broadcast origin address == server host address / status = finished
		{
			when(restServiceReal.getServerSettings().getHostAddress()).thenReturn("127.0.0.1");

			Broadcast broadcast = new Broadcast();
			broadcast.setOriginAdress("55.55.55.55");
			broadcast.setStatus(AntMediaApplicationAdapter.BROADCAST_STATUS_FINISHED);
			store.save(broadcast);

			when(restServiceReal.getServerSettings().getHostAddress()).thenReturn("55.55.55.55");

			Result result = restServiceReal.deleteBroadcasts(new String[] {broadcast.getStreamId()});
			assertTrue(result.isSuccess());
		}
		
		{
			Result result = restServiceReal.deleteBroadcasts(new String[] {});
			assertFalse(result.isSuccess());
			
			result = restServiceReal.deleteBroadcasts(null);
			assertFalse(result.isSuccess());
		}

	}

	@Test
	public void testGetVersion() {
		RootRestService rootRestService = new RootRestService();
		Version version = rootRestService.getVersion();
		System.out.println("VersionName " + version.getVersionName());
		System.out.println("Expected VersionName " + AntMediaApplicationAdapter.class.getPackage().getImplementationVersion());
		assertEquals(version.getVersionName(), AntMediaApplicationAdapter.class.getPackage().getImplementationVersion());
		System.out.println("VersionType " + version.getVersionType());
		assertEquals(RestServiceBase.COMMUNITY_EDITION, version.getVersionType());
	}

	@Test
	public void testServerNameAndRtmpURL() {
		AppSettings settings = mock(AppSettings.class);
		String serverName = "fully.qualified.domain.name";
		
		ServerSettings serverSettings = Mockito.spy(new ServerSettings());
		
		when(serverSettings.getServerName()).thenReturn(serverName);
		restServiceReal.setServerSettings(serverSettings);


		Scope scope = mock(Scope.class);
		String scopeName = "scope";
		when(scope.getName()).thenReturn(scopeName);

		restServiceReal.setScope(scope);
		restServiceReal.setAppSettings(settings);

		Broadcast broadcast = new Broadcast(null, "name");
		DataStore store = new InMemoryDataStore("testdb");
		restServiceReal.setDataStore(store);
		Broadcast createBroadcast = (Broadcast) restServiceReal.createBroadcast(broadcast, false).getEntity();

		assertEquals("rtmp://" + serverName + "/" + scopeName + "/" + broadcast.getStreamId() , createBroadcast.getRtmpURL());

		when(serverSettings.getServerName()).thenReturn(null);


		broadcast = new Broadcast(null, "name");
		Response response = restServiceReal.createBroadcast(broadcast, false);
		assertEquals(Status.OK.getStatusCode(), response.getStatus());
		try {
			assertEquals("rtmp://" + InetAddress.getLocalHost().getHostAddress() + "/" + scopeName + "/" + broadcast.getStreamId() , broadcast.getRtmpURL());
		} catch (UnknownHostException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		when(serverSettings.getServerName()).thenReturn("");


		broadcast = new Broadcast(null, "name");
		Broadcast createBroadcast3 = (Broadcast) restServiceReal.createBroadcast(broadcast, false).getEntity();

		try {
			assertEquals("rtmp://" + InetAddress.getLocalHost().getHostAddress() + "/" + scopeName + "/" + broadcast.getStreamId() , createBroadcast3.getRtmpURL());
		} catch (UnknownHostException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testWithStreamId() {
		AppSettings settings = mock(AppSettings.class);
		when(settings.getListenerHookURL()).thenReturn(null);
		restServiceReal.setAppSettings(settings);
		
		ServerSettings serverSettings = Mockito.mock(ServerSettings.class);
		restServiceReal.setServerSettings(serverSettings);
		

		Scope scope = mock(Scope.class);
		String scopeName = "scope";
		when(scope.getName()).thenReturn(scopeName);
		restServiceReal.setScope(scope);

		Broadcast broadcast = new Broadcast(null, "name");
		String streamId = "streamId";
		try {
			broadcast.setStreamId(streamId);
		} 
		catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		DataStore store = new InMemoryDataStore("testdb");
		restServiceReal.setDataStore(store);

		Broadcast createdBroadcast = (Broadcast) restServiceReal.createBroadcast(broadcast, false).getEntity();
		assertNotNull(createdBroadcast.getStreamId());
		//create broadcast method does not reset id, if stream id is set and can be usable, it uses
		assertEquals(createdBroadcast.getStreamId(), streamId);

		assertFalse(createdBroadcast.isZombi());

		//testing Create Broadcast without reset Stream ID

		Broadcast broadcastWithStreamID = new Broadcast(null, "name");
		try {
			broadcastWithStreamID.setStreamId(streamId);
		} 
		catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		Response response = restServiceReal.createBroadcast(broadcastWithStreamID, false);
		//becase same stream id already exist in the db
		assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());

	}
	
	
	@Test
	public void testRecordFails() {
		
		ApplicationContext context = mock(ApplicationContext.class);
		restServiceReal.setAppCtx(context);		
		
		AntMediaApplicationAdapter app = new AntMediaApplicationAdapter();
		restServiceReal.setApplication(app);
		
		AppSettings settings = mock(AppSettings.class);
		when(settings.getListenerHookURL()).thenReturn(null);
		restServiceReal.setAppSettings(settings);
		
		Scope scope = mock(Scope.class);
		String scopeName = "scope";
		when(scope.getName()).thenReturn(scopeName);

		restServiceReal.setScope(scope);
		
		Broadcast broadcast = new Broadcast(null, "name");
		DataStore store = Mockito.spy(new InMemoryDataStore("testdb"));
		restServiceReal.setDataStore(store);
		
		ServerSettings serverSettings = Mockito.mock(ServerSettings.class);
		restServiceReal.setServerSettings(serverSettings);
		
		broadcast.setStatus(AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING);
		store.save(broadcast);
		
		when(context.containsBean(any())).thenReturn(false);
		
		//Check if stream is on a standalone server
		
		// Start MP4 Recording && Broadcast Status: Broadcasting, mp4Enabled: 0, it should return false
		Result result = restServiceReal.enableRecordMuxing(broadcast.getStreamId(), true, "mp4", 0);
		assertFalse(result.isSuccess());
		assertEquals("mp4 recording couldn't be started",result.getMessage());
		
		// Stop MP4 Recording && Broadcast Status: Broadcasting, mp4Enabled: 0, it should return false
		result = restServiceReal.enableRecordMuxing(broadcast.getStreamId(), false,"mp4", 0);
		assertFalse(result.isSuccess());
		assertEquals("mp4 recording couldn't be stopped",result.getMessage());
		
		Broadcast broadcast2 = new Broadcast(null, "name");
		store.save(broadcast2);
		
		// Stop WebM Recording && Broadcast Status: created, webmEnabled: 0, it should return true
		result = restServiceReal.enableRecordMuxing(broadcast2.getStreamId(), false,"webm", 0);
		assertTrue(result.isSuccess());
		
		// Start WebM Recording && Broadcast Status: created, webmEnabled: 0, it should return true
		result = restServiceReal.enableRecording(broadcast2.getStreamId(), true,"webm", 0);
		assertTrue(result.isSuccess());
		
		// Stop WebM Recording && Broadcast Status: created, webmEnabled: 0, it should return true
		result = restServiceReal.enableRecordMuxing(broadcast2.getStreamId(), false,"webm", 0);
		assertTrue(result.isSuccess());
		
		
		Broadcast broadcast3 = new Broadcast(null, "name");
		broadcast3.setStatus(AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING);
		store.save(broadcast3);
		
		doReturn(false).when(store).setWebMMuxing(Mockito.any(), Mockito.anyInt());
		result = restServiceReal.enableRecordMuxing(broadcast3.getStreamId(), false,"webm", 0);
		assertFalse(result.isSuccess());
		assertEquals("webm recording couldn't be stopped",result.getMessage());
		
		
		//Check if stream is on another cluster node
		broadcast3.setOriginAdress("127.0.0.1");		
		when(restServiceReal.getServerSettings().getHostAddress()).thenReturn("55.55.55.55");
		when(context.containsBean(any())).thenReturn(true);
		
		String type = "mp4";
		// Start MP4 Recording && Broadcast Status: Broadcasting, mp4Enabled: 0, it should return false
		result = restServiceReal.enableRecordMuxing(broadcast3.getStreamId(), true,type, 0);
		assertFalse(result.isSuccess());
		assertEquals("Please send " + type + " recording request to " + broadcast3.getOriginAdress() + " node or send request in a stopped status.",result.getMessage());
		
		// Stop MP4 Recording && Broadcast Status: Broadcasting, mp4Enabled: 0, it should return false
		BroadcastRestService restServiceSpy = spy(restServiceReal);
		doReturn(true).when(restServiceSpy).isAlreadyRecording(broadcast3.getStreamId(), RecordType.MP4, 0);
		result = restServiceSpy.enableRecordMuxing(broadcast3.getStreamId(), false,type, 0);		
		assertFalse(result.isSuccess());
		assertEquals("Please send " + type + " recording request to " + broadcast3.getOriginAdress() + " node or send request in a stopped status.",result.getMessage());
		
	}

	@Test
	public void testAllInOne() {
		AppSettings settings = mock(AppSettings.class);
		when(settings.getListenerHookURL()).thenReturn(null);
		restServiceReal.setAppSettings(settings);

		Scope scope = mock(Scope.class);
		String scopeName = "scope";
		when(scope.getName()).thenReturn(scopeName);

		restServiceReal.setScope(scope);

		Broadcast broadcast = new Broadcast(null, "name");
		DataStore store = new InMemoryDataStore("testdb");
		restServiceReal.setDataStore(store);
		
		ServerSettings serverSettings = Mockito.mock(ServerSettings.class);
		restServiceReal.setServerSettings(serverSettings);
		
		Broadcast createBroadcast = (Broadcast) restServiceReal.createBroadcast(broadcast, false).getEntity();

		assertNotNull(createBroadcast);
		assertNotNull(createBroadcast.getStreamId());
		assertNotNull(createBroadcast.getName());
		assertNotNull(createBroadcast.getStatus());
		assertNull(createBroadcast.getListenerHookURL());
		
		
		broadcast = new Broadcast();
		try {
			broadcast.setStreamId("  12345 ");
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		
		createBroadcast = (Broadcast) restServiceReal.createBroadcast(broadcast, false).getEntity();
		assertEquals("12345", createBroadcast.getStreamId());
		
		
		try {
			broadcast = Mockito.spy(new Broadcast());
			broadcast.setStreamId("111");
			Mockito.doThrow(NullPointerException.class).when(broadcast).setStreamId(Mockito.anyString());
			Result result = (Result) restServiceReal.createBroadcast(broadcast, false).getEntity();
			assertFalse(result.isSuccess());
			
		}
		catch (Exception e) {
			fail(e.getMessage());
		}

		Broadcast createBroadcast2 = (Broadcast) restServiceReal.createBroadcast(null, false).getEntity();

		assertNotNull(createBroadcast2);
		assertNotNull(createBroadcast2.getStreamId());
		assertNull(createBroadcast2.getName());
		assertNotNull(createBroadcast2.getStatus());
		assertNull(createBroadcast2.getListenerHookURL());

		Broadcast broadcastTmp = (Broadcast) restServiceReal.getBroadcast(createBroadcast.getStreamId()).getEntity();
		assertNotNull(broadcastTmp);
		assertEquals(createBroadcast.getStatus(), broadcastTmp.getStatus());
		assertNull(broadcastTmp.getListenerHookURL());

		//update status
		boolean updateStatus = store.updateStatus(createBroadcast.getStreamId(), AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING);
		assertTrue(updateStatus);

		//check status
		broadcastTmp = (Broadcast) restServiceReal.getBroadcast(createBroadcast.getStreamId()).getEntity();
		assertNotNull(broadcastTmp);
		assertEquals(AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING, broadcastTmp.getStatus());
		assertNull(broadcastTmp.getListenerHookURL());

		//update status again
		updateStatus = store.updateStatus(createBroadcast.getStreamId(), AntMediaApplicationAdapter.BROADCAST_STATUS_FINISHED);
		assertTrue(updateStatus);

		//check status
		broadcastTmp = (Broadcast) restServiceReal.getBroadcast(createBroadcast.getStreamId()).getEntity();
		assertNotNull(broadcastTmp);
		assertEquals( AntMediaApplicationAdapter.BROADCAST_STATUS_FINISHED, broadcastTmp.getStatus());
		assertNull(broadcastTmp.getListenerHookURL());


		Response response = restServiceReal.getBroadcast("jdkdkdkdk");
		assertNotNull(broadcastTmp);
		assertEquals(Status.NOT_FOUND.getStatusCode() ,response.getStatus());


		broadcastTmp = (Broadcast) restServiceReal.getBroadcast(createBroadcast.getStreamId()).getEntity();
		assertNotNull(broadcastTmp);
		assertEquals(AntMediaApplicationAdapter.BROADCAST_STATUS_FINISHED, broadcastTmp.getStatus());
		assertEquals(broadcastTmp.getStreamId(), createBroadcast.getStreamId());
		assertEquals(broadcastTmp.getName(), createBroadcast.getName());

		//create test broadcast for setting mp4 muxing setting
		Broadcast testBroadcast = (Broadcast) restServiceReal.createBroadcast(new Broadcast("testBroadcast"), false).getEntity();
		assertNotNull(testBroadcast.getStreamId());

		//check null case
		assertFalse(restServiceReal.enableRecordMuxing(null, true,"mp4", 0).isSuccess());

		//check that setting is saved
		assertTrue(restServiceReal.enableRecordMuxing(testBroadcast.getStreamId(), true,"mp4", 0).isSuccess());

		//check that setting is saved correctly
		assertEquals(MuxAdaptor.RECORDING_ENABLED_FOR_STREAM, ((Broadcast)restServiceReal.getBroadcast(testBroadcast.getStreamId()).getEntity()).getMp4Enabled());

	}
	
	

	@Test
    public void testEnableMp4Muxing() throws Exception 
	{
		final String scopeValue = "scope";
		final String dbName = "testdb";
		final String broadcastName = "testBroadcast";

        BroadcastRestService restServiceSpy = Mockito.spy(new BroadcastRestService());
		AppSettings settings = mock(AppSettings.class);
		when(settings.getListenerHookURL()).thenReturn(null);
		restServiceSpy.setAppSettings(settings);

		ServerSettings serverSettings = Mockito.mock(ServerSettings.class);
		restServiceSpy.setServerSettings(serverSettings);
		
		Scope scope = mock(Scope.class);
		when(scope.getName()).thenReturn(scopeValue);

		restServiceSpy.setScope(scope);

		DataStore store = new InMemoryDataStore(dbName);
		restServiceSpy.setDataStore(store);

        AntMediaApplicationAdapter application = mock(AntMediaApplicationAdapter.class);
        Mp4Muxer mockMp4Muxer = Mockito.mock(Mp4Muxer.class);
		HLSMuxer mockHLSMuxer = Mockito.mock(HLSMuxer.class);
        ArrayList<Muxer> mockMuxers = new ArrayList<>();
        mockMuxers.add(mockMp4Muxer);
        
        doReturn(true).when(restServiceSpy).isInSameNodeInCluster(Mockito.any());

        MuxAdaptor mockMuxAdaptor = Mockito.mock(MuxAdaptor.class);
        when(mockMuxAdaptor.getMuxerList()).thenReturn(mockMuxers);
        
        when(mockMuxAdaptor.startRecording(RecordType.MP4, 0)).thenReturn(Mockito.mock(Mp4Muxer.class));
        
        when(mockMuxAdaptor.stopRecording(RecordType.MP4, 0)).thenReturn(Mockito.mock(Mp4Muxer.class));

        ArrayList<MuxAdaptor> mockMuxAdaptors = new ArrayList<>();
        mockMuxAdaptors.add(mockMuxAdaptor);

        when(application.getMuxAdaptors()).thenReturn(mockMuxAdaptors);
        when(restServiceSpy.getApplication()).thenReturn(application);

        Response response = restServiceSpy.createBroadcast(new Broadcast(broadcastName), false);
        Broadcast testBroadcast = (Broadcast) response.getEntity();
		when(mockMuxAdaptor.getStreamId()).thenReturn(testBroadcast.getStreamId());

        assertTrue(restServiceSpy.enableRecordMuxing(testBroadcast.getStreamId(), true,"mp4", 0).isSuccess());

        verify(mockMuxAdaptor,never()).startRecording(RecordType.MP4, 0);

		mockMuxers.clear();
		mockMuxers.add(mockHLSMuxer);
		
		
		
		//disable
		assertTrue(restServiceSpy.enableRecordMuxing(testBroadcast.getStreamId(), false, "mp4", 0).isSuccess());
		
		store.updateStatus(testBroadcast.getStreamId(), AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING);
		
		assertTrue(restServiceSpy.enableRecordMuxing(testBroadcast.getStreamId(), true, "mp4", 0).isSuccess());
		verify(mockMuxAdaptor).startRecording(RecordType.MP4, 0);

		when(mockMuxAdaptor.isAlreadyRecording(RecordType.MP4, 0)).thenReturn(true);
		mockMuxers.add(mockMp4Muxer);

        assertEquals(MuxAdaptor.RECORDING_ENABLED_FOR_STREAM, ((Broadcast)restServiceSpy.getBroadcast(testBroadcast.getStreamId()).getEntity()).getMp4Enabled());
        assertTrue(restServiceSpy.enableRecordMuxing(testBroadcast.getStreamId(), false, "mp4", 0).isSuccess());
        verify(mockMuxAdaptor).stopRecording(RecordType.MP4, 0);
    }
	
	@Test
    public void testEnableWebMMuxing() throws Exception 
	{
		final String scopeValue = "scope";
        
        BroadcastRestService restServiceSpy = Mockito.spy(new BroadcastRestService());
		AppSettings settings = mock(AppSettings.class);
		when(settings.getListenerHookURL()).thenReturn(null);
		restServiceSpy.setAppSettings(settings);

		ServerSettings serverSettings = Mockito.mock(ServerSettings.class);
		restServiceSpy.setServerSettings(serverSettings);
		
		Scope scope = mock(Scope.class);
		when(scope.getName()).thenReturn(scopeValue);

		restServiceSpy.setScope(scope);

		DataStore store = new InMemoryDataStore("test");
		restServiceSpy.setDataStore(store);

        Response response = restServiceSpy.createBroadcast(new Broadcast("test"), false);
        Broadcast testBroadcast = (Broadcast) response.getEntity();
        testBroadcast.setStatus(AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING);
        String streamId = testBroadcast.getStreamId();
        
        MuxAdaptor mockMuxAdaptor = Mockito.mock(MuxAdaptor.class);
        doReturn(mockMuxAdaptor).when(restServiceSpy).getMuxAdaptor(streamId);
        doReturn(null).when(mockMuxAdaptor).startRecording(RecordType.WEBM, 0);
        when(mockMuxAdaptor.getStreamId()).thenReturn(streamId);

        doReturn(true).when(restServiceSpy).isInSameNodeInCluster(Mockito.any());

        //try to stop recording
        Result result = restServiceSpy.enableRecordMuxing(streamId, false, "webm", 0);
        //it should return false because there is no recording
        assertFalse(result.isSuccess());
        
        result = restServiceSpy.enableRecordMuxing(streamId, true, "webm", 0);
        assertFalse(result.isSuccess());
        doReturn(Mockito.mock(RecordMuxer.class)).when(mockMuxAdaptor).startRecording(RecordType.WEBM, 0);
  
        result = restServiceSpy.enableRecordMuxing(streamId, true, "webm", 0);
        assertTrue(result.isSuccess());
        assertNotNull(result.getMessage());
        verify(mockMuxAdaptor, times(2)).startRecording(RecordType.WEBM, 0);
        assertEquals(MuxAdaptor.RECORDING_ENABLED_FOR_STREAM, store.get(streamId).getWebMEnabled());

        //disable
        doReturn(Mockito.mock(RecordMuxer.class)).when(mockMuxAdaptor).stopRecording(RecordType.WEBM, 0);
        doReturn(true).when(restServiceSpy).isAlreadyRecording(streamId, RecordType.WEBM, 0);
		result = restServiceSpy.enableRecordMuxing(streamId, false, "webm", 0);
		assertTrue(result.isSuccess());
        verify(mockMuxAdaptor, times(1)).stopRecording(RecordType.WEBM, 0);
        assertEquals(MuxAdaptor.RECORDING_DISABLED_FOR_STREAM, store.get(streamId).getWebMEnabled());
        
      
        store.get(streamId).setWebMEnabled(MuxAdaptor.RECORDING_ENABLED_FOR_STREAM);
        doReturn(null).when(mockMuxAdaptor).stopRecording(RecordType.WEBM, 0);
		result = restServiceSpy.enableRecordMuxing(streamId, false, "webm", 0);
		assertFalse(result.isSuccess());
        
	}

	@Test
	public void testTokenOperations() {

		DataStore store = new InMemoryDataStore("testdb");
		restServiceReal.setDataStore(store);

		
		//create token
		Token token = new Token();
		token.setStreamId("1234");
		token.setTokenId("tokenId");
		token.setType(Token.PLAY_TOKEN);

		assertTrue(restServiceReal.getDataStore().saveToken(token));

		//get tokens of stream
		List <Token> tokens = restServiceReal.listTokensV2(token.getStreamId(), 0, 10);

		assertEquals(1, tokens.size());

		//revoke tokens
		restServiceReal.revokeTokensV2(token.getStreamId());

		//get tokens of stream
		tokens = restServiceReal.listTokensV2(token.getStreamId(), 0, 10);

		//it should be zero because all tokens are revoked
		assertEquals(0, tokens.size());


		//define a valid expire date
		long expireDate = Instant.now().getEpochSecond() + 1000;
		
		//create token again
		token = new Token();
		token.setStreamId("1234");
		token.setTokenId("tokenId");
		token.setType(Token.PLAY_TOKEN);
		token.setExpireDate(expireDate);

		assertTrue(restServiceReal.getDataStore().saveToken(token));

		//validate token
		Result result = restServiceReal.validateTokenV2(token);

		//token should be validated and returned
		assertTrue(result.isSuccess());

		//this should be false, because validated token is deleted after consumed
		result = restServiceReal.validateTokenV2(token);

		assertFalse(result.isSuccess());

	}
	
	@Test
	public void testTimeBasedSubscriberOperations() {

		DataStore store = new InMemoryDataStore("testdb");
		restServiceReal.setDataStore(store);

		
		//create subscribers
		Subscriber subscriber = new Subscriber();
		subscriber.setSubscriberId("timeSubscriber");
		subscriber.setStreamId("stream1");
		subscriber.setType(Subscriber.PLAY_TYPE);

		Subscriber subscriber2 = new Subscriber();
		subscriber2.setSubscriberId("timeSubscriber2");
		subscriber2.setStreamId("stream1");
		subscriber2.setType(Subscriber.PLAY_TYPE);
		
		assertTrue(restServiceReal.addSubscriber(subscriber.getStreamId(), subscriber).isSuccess());
		assertTrue(restServiceReal.addSubscriber(subscriber2.getStreamId(), subscriber2).isSuccess());
		
		//get tokens of stream
		List <Subscriber> subscribers = restServiceReal.listSubscriberV2(subscriber.getStreamId(), 0, 10);

		List <SubscriberStats> subscriberStats = restServiceReal.listSubscriberStatsV2(subscriber.getStreamId(), 0, 10);
		
		assertEquals(2, subscribers.size());
		assertEquals(2, subscriberStats.size());
		
		// remove subscriber
		assertTrue(restServiceReal.deleteSubscriber(subscriber.getStreamId(), subscriber.getSubscriberId()).isSuccess());
		
		subscribers = restServiceReal.listSubscriberV2(subscriber.getStreamId(), 0, 10);
		
		assertEquals(1, subscribers.size());
		
		//revoke tokens
		restServiceReal.revokeSubscribers(subscriber.getStreamId());

		// get subscribers
		subscribers = restServiceReal.listSubscriberV2(subscriber.getStreamId(), 0, 10);

		//it should be zero because all tokens are revoked
		assertEquals(0, subscribers.size());

	}	

	@Test
	public void testObjectDetectionOperations() {

		DataStore store = new InMemoryDataStore("testdb");
		restServiceReal.setDataStore(store);

		String streamId = "object_streamId";

		List<TensorFlowObject> detectedObjects = new ArrayList<>();

		//create detection object

		TensorFlowObject object = new TensorFlowObject("objectName", 92, "imageId");

		//add to list

		detectedObjects.add(object);

		restServiceReal.getDataStore().saveDetection(streamId, 0, detectedObjects);

		//get objects

		List<TensorFlowObject> objects = restServiceReal.getDetectionListV2(streamId, 0, 10);

		assertEquals(1, objects.size());		

		//get list of requested id

		List<TensorFlowObject> objectList = restServiceReal.getDetectionListV2(streamId, 0, 50);

		assertEquals(1, objectList.size());

		//get total number of saved detection list

		Long total = restServiceReal.getObjectDetectedTotal(streamId).getNumber();

		assertEquals(1, (int)(long)total);
	}

	@Test
	public void testStopLiveStream() {
		BroadcastRestService restService = new BroadcastRestService();
		AntMediaApplicationAdapter app = Mockito.spy(new AntMediaApplicationAdapter());
		DataStore ds = Mockito.mock(DataStore.class);
		String streamId = "test-stream";

		Broadcast broadcast = new Broadcast();
		try {
			broadcast.setStreamId(streamId);
		} catch (Exception e) {
			e.printStackTrace();
		}
		broadcast.setType(AntMediaApplicationAdapter.LIVE_STREAM);

		Mockito.doReturn(broadcast).when(ds).get(streamId);
		restService.setDataStore(ds);
		restService.setApplication(app);

		restService.stopStreamingV2(streamId);

		Mockito.verify(app, Mockito.times(1)).getBroadcastStream(null, streamId);
	}

	@Test
	public void testConferenceRoom() {
		DataStore store = new InMemoryDataStore("testdb");
		restServiceReal.setDataStore(store);

		ConferenceRoom room = new ConferenceRoom();
		
		long now = Instant.now().getEpochSecond();

		//should be null because roomName not defined
		Response response = restServiceReal.createConferenceRoomV2(room);
		assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());

		//define roomName
		room.setRoomId("roomName");
		
		//let it be zombi
		room.setZombi(true);
		
		//let it be mcu
		room.setMode(WebSocketConstants.MCU);

		//should not be null because room is saved to database and created room is returned
		assertNotNull(restServiceReal.createConferenceRoomV2(room));

		room = restServiceReal.getDataStore().getConferenceRoom(room.getRoomId());
		
		assertTrue(room.isZombi());
		assertEquals(WebSocketConstants.MCU, room.getMode());
		
		//this should not be null, because although start date is not defined, service create it as now
		assertNotNull(room.getStartDate());
		
		//this should not be null, because although end date is not defined, service create it as 1 hour later of now
		assertNotNull(room.getEndDate());

		//define a start date
		room.setStartDate(now);

		String origin = "someAddress";
		//define a start date
		room.setOriginAdress(origin);
		
		assertEquals(origin, room.getOriginAdress());
		
		//Test GET conference room by id rest service
		assertNotNull(restServiceReal.getConferenceRoom(room.getRoomId()));
		assertEquals(restServiceReal.getConferenceRoom(room.getRoomId()).getEntity(), room);

		Response getRoomResponse = restServiceReal.getConferenceRoom(room.getRoomId());
		assertEquals(200,getRoomResponse.getStatus());

		getRoomResponse = restServiceReal.getConferenceRoom(null);
		assertEquals(404,getRoomResponse.getStatus());

		getRoomResponse = restServiceReal.getConferenceRoom("nullllllllllllllllll");
		assertEquals(404,getRoomResponse.getStatus());

		//edit room with the new startDate
		//should not be null because room is saved to database and edited room is returned
		assertNotNull(restServiceReal.editConferenceRoom(room.getRoomId(), room));
		
		room = restServiceReal.getDataStore().getConferenceRoom(room.getRoomId());
		
		//check start date
		assertEquals(now, room.getStartDate());

		//delete room
		assertTrue(restServiceReal.deleteConferenceRoomV2(room.getRoomId()).isSuccess());
		
		//check that room does not exist  in db 
		assertNull(restServiceReal.getDataStore().getConferenceRoom(room.getRoomId()));
	}
	
	@Test
	public void testAddIPCameraViaCreateBroadcast() 
	{
		
		BroadcastRestService restService = Mockito.spy(restServiceReal);
		
		AntMediaApplicationAdapter adaptor = mock (AntMediaApplicationAdapter.class);
		Mockito.doReturn(adaptor).when(restService).getApplication();
		IScope scope = mock(IScope.class);
		when(scope.getName()).thenReturn("junit");
		Mockito.doReturn(new InMemoryDataStore("testAddIPCamera")).when(restService).getDataStore();
		
		Mockito.doReturn(scope).when(restService).getScope();
		
		ApplicationContext appContext = mock(ApplicationContext.class);
		restService.setAppCtx(appContext);
		Mockito.doReturn(new ServerSettings()).when(restService).getServerSettings();
		Mockito.doReturn(new AppSettings()).when(restService).getAppSettings();
		
		Broadcast broadcast = new Broadcast("testAddIPCamera", "10.2.40.64:8080", "admin", "admin",
				"rtsp://11.2.40.63:8554/live1.sdp", AntMediaApplicationAdapter.STREAM_SOURCE);
		Response createBroadcastResponse = restService.createBroadcast(broadcast, false);
		assertEquals(200, createBroadcastResponse.getStatus());
		
		broadcast = new Broadcast("testAddIPCamera", null, "admin", "admin",
				null, AntMediaApplicationAdapter.STREAM_SOURCE);
		
		createBroadcastResponse = restService.createBroadcast(broadcast, false);
		assertEquals(400, createBroadcastResponse.getStatus());
		
		broadcast = new Broadcast("testAddIPCamera", "10.2.40.64:8080", "admin", "admin",
				"rtsdfdfdfd-invalid-url", AntMediaApplicationAdapter.STREAM_SOURCE);
		
		createBroadcastResponse = restService.createBroadcast(broadcast, false);
		assertEquals(400, createBroadcastResponse.getStatus());
		
		createBroadcastResponse = restService.createBroadcast(null, false);
		assertEquals(200, createBroadcastResponse.getStatus());
		
		broadcast = new Broadcast("testAddIPCamera", "10.2.40.64:8080", "admin", "admin",
				null, AntMediaApplicationAdapter.IP_CAMERA);
		createBroadcastResponse = restService.createBroadcast(null, false);
		assertEquals(200, createBroadcastResponse.getStatus());
		
		broadcast = new Broadcast("testAddIPCamera", "false_ip_addr", "admin", "admin",
				null, AntMediaApplicationAdapter.IP_CAMERA);
		createBroadcastResponse = restService.createBroadcast(broadcast, false);
		assertEquals(400, createBroadcastResponse.getStatus());
		
		broadcast = new Broadcast("testAddIPCamera", "10.2.40.64:8080", "admin", "admin",
				"rtsdfdfdfd-invalid-url", AntMediaApplicationAdapter.IP_CAMERA);
		createBroadcastResponse = restService.createBroadcast(broadcast, false);
		assertEquals(200, createBroadcastResponse.getStatus());
		
		
		
	}
	
	@Test
	public void testAddIPCamera()  {
		Result result = new Result(false);

		Broadcast newCam = new Broadcast("testAddIPCamera", "10.2.40.64:8080", "admin", "admin",
				"rtsp://11.2.40.63:8554/live1.sdp", AntMediaApplicationAdapter.IP_CAMERA);

		BroadcastRestService streamSourceRest = Mockito.spy(restServiceReal);
		AntMediaApplicationAdapter adaptor = Mockito.spy (new AntMediaApplicationAdapter());
		StreamFetcher fetcher = mock (StreamFetcher.class);
		Result connResult = new Result(true);
		connResult.setMessage("rtsp://11.2.40.63:8554/live1.sdp");
		DataStore dataStore = new InMemoryDataStore("db");
		adaptor.setDataStore(dataStore);

		Mockito.doReturn(connResult).when(streamSourceRest).connectToCamera(newCam);
		Mockito.doReturn(adaptor).when(streamSourceRest).getApplication();
		Mockito.doReturn(new Result(true)).when(adaptor).startStreaming(newCam);
		Mockito.doReturn(new InMemoryDataStore("testAddIPCamera")).when(streamSourceRest).getDataStore();

		Mockito.doReturn(new ServerSettings()).when(streamSourceRest).getServerSettings();
		Mockito.doReturn(new AppSettings()).when(streamSourceRest).getAppSettings();
		
		IScope scope = mock(IScope.class);
		when(scope.getName()).thenReturn("junit");
		
		Mockito.doReturn(scope).when(streamSourceRest).getScope();
		
		IContext icontext = mock(IContext.class);
		when(icontext.getBean(AppSettings.BEAN_NAME)).thenReturn(new AppSettings());
		
		Mockito.when(scope.getContext()).thenReturn(icontext);
		adaptor.setScope(scope);
		

		
		ApplicationContext appContext = mock(ApplicationContext.class);

		streamSourceRest.setAppCtx(appContext);

		StatsCollector monitorService = new StatsCollector(); 
		
		when(appContext.getBean(IStatsCollector.BEAN_NAME)).thenReturn(monitorService);

		//define CPU load above limit
		int cpuLoad = 90;
		int cpuLimit = 80;


		monitorService.setCpuLimit(cpuLimit);
		monitorService.setCpuLoad(cpuLoad);
		

		//try to add IP camera
		result = streamSourceRest.addStreamSource(newCam);
		
		//should be false because load is above limit
		assertFalse(result.isSuccess());
		

		//should be -3 because it is CPU Load Error Code
		assertEquals(-3, result.getErrorId());
		
		Result cameraErrorV2 = streamSourceRest.getCameraErrorV2(newCam.getStreamId());
		assertFalse(cameraErrorV2.isSuccess());

		//define CPU load below limit
		int cpuLoad2 = 70;
		int cpuLimit2 = 80;


		monitorService.setCpuLimit(cpuLimit2);
		monitorService.setCpuLoad(cpuLoad2);

		result = streamSourceRest.addStreamSource(newCam);

		//should be true because load is below limit
		assertTrue(result.isSuccess());
		
		Broadcast noSpecifiedType =  new Broadcast("testAddIPCamera");
		result=streamSourceRest.addStreamSource(noSpecifiedType);
		//should be true since it wouldn't return true because there is no ip camera or stream source defined in the declaration.
		assertFalse(result.isSuccess());
		assertEquals("Auto start query needs an IP camera or stream source.",result.getMessage() );

	}


	@Test
	public void testStartStopStreamSource()  {

		//start ONVIF Camera emulator
		StreamFetcherUnitTest.startCameraEmulator();

		//create an IP Camera for emulator
		Broadcast newCam = new Broadcast("startStopIPCamera", "127.0.0.1:8080", "admin", "admin",
				null, AntMediaApplicationAdapter.IP_CAMERA);

		//simulate required operations
		BroadcastRestService streamSourceRest = Mockito.spy(restServiceReal);
		AntMediaApplicationAdapter adaptor = mock (AntMediaApplicationAdapter.class);
		StreamFetcher fetcher = mock (StreamFetcher.class);
		Mockito.doReturn(adaptor).when(streamSourceRest).getApplication();
		Mockito.doReturn(new Result(true)).when(adaptor).startStreaming(newCam);
		Mockito.doReturn(new Result(true)).when(adaptor).stopStreaming(newCam);
		Mockito.doReturn(new InMemoryDataStore("startStopStreamSource")).when(streamSourceRest).getDataStore();

		Mockito.doReturn(new ServerSettings()).when(streamSourceRest).getServerSettings();
		Mockito.doReturn(new AppSettings()).when(streamSourceRest).getAppSettings();
		
		IScope scope = mock(IScope.class);
		when(scope.getName()).thenReturn("junit");
		
		Mockito.doReturn(scope).when(streamSourceRest).getScope();
		
		//add IP Camera first
		assertTrue(streamSourceRest.addIPCamera(newCam).isSuccess());

		//stream URL should be defined after ONVIF operations
		//this assignment also ensures that, connection is successful to IP Camera via rest service using ONVIF operations
		
		assertEquals("rtsp://admin:admin@127.0.0.1:6554/test.flv", newCam.getStreamUrl());
		
		//stop request should trigger application adaptor stopStreaming
		assertTrue(streamSourceRest.stopStreamingV2(newCam.getStreamId()).isSuccess());
		
		//reset stream URL and check whether start rest service is able to get stream URL by connecting to camera using ONVIF
		newCam.setStreamUrl(null);
		
		
		newCam = streamSourceRest.getDataStore().get(newCam.getStreamId());
		
		//start again via rest service
		assertTrue(streamSourceRest.startStreamSource(newCam.getStreamId()).isSuccess());
		assertTrue(streamSourceRest.stopStreamingV2(newCam.getStreamId()).isSuccess());
		
		
		{
			//camera validity check
			Broadcast cast = new Broadcast();
			cast.setIpAddr("ht://124323");
			assertFalse(streamSourceRest.addIPCamera(cast).isSuccess());
		}
		
		{
			Broadcast streamSource = new Broadcast("---start-stop", "", "", "",
					null, AntMediaApplicationAdapter.STREAM_SOURCE);
			streamSourceRest.getDataStore().save(streamSource);
			Result result = streamSourceRest.startStreamSource(streamSource.getStreamId());
			assertFalse(result.isSuccess());
		}
		
		

		//stop camera emulator
		StreamFetcherUnitTest.stopCameraEmulator();
	}
	
	@Test
	public void testOnvifPTZ() {
		
		BroadcastRestService spyService = Mockito.spy(restServiceReal);
		
		String id = "invalid_?stream_id";
		assertFalse(spyService.moveIPCamera(id, null, null, null, null).isSuccess());
		assertFalse(spyService.moveIPCamera(id, null, null, null, "absolute").isSuccess());
		assertFalse(spyService.moveIPCamera(id, null, null, null, "relative").isSuccess());
		assertFalse(spyService.moveIPCamera(id, null, null, null, "continous").isSuccess());
		assertFalse(spyService.stopMove(id).isSuccess());
		

		 
		id = "valid_stream_id";
		OnvifCamera onvifCamera = Mockito.mock(OnvifCamera.class);
		AntMediaApplicationAdapter application = Mockito.mock(AntMediaApplicationAdapter.class);
		Mockito.when(application.getOnvifCamera(anyString())).thenReturn(onvifCamera);
		
		
		Mockito.doReturn(application).when(spyService).getApplication();
		
		spyService.moveIPCamera(id, null, null, null, null).isSuccess();
		Mockito.verify(onvifCamera).moveRelative(0, 0, 0);
		
		spyService.moveIPCamera(id, -0.5f, 0.5f, 0.3f, null).isSuccess();
		Mockito.verify(onvifCamera).moveRelative(-0.5f, 0.5f, 0.3f);
		spyService.moveIPCamera(id, 0.3f, 0.4f, 0.2f, "absolute").isSuccess();
		Mockito.verify(onvifCamera).moveAbsolute(0.3f, 0.4f, 0.2f);
		spyService.moveIPCamera(id, 0.3f, 0.4f, 0.2f, "relative").isSuccess();
		Mockito.verify(onvifCamera).moveRelative(0.3f, 0.4f, 0.2f);
		spyService.moveIPCamera(id, 0.3f, 0.4f, 0.2f, "continuous").isSuccess();
		Mockito.verify(onvifCamera).moveContinous(0.3f, 0.4f, 0.2f);
		
		spyService.stopMove(id);
		Mockito.verify(onvifCamera).moveStop();
		
		
		assertFalse(spyService.moveIPCamera(id, 0.3f, 0.4f, 0.2f, "false_value").isSuccess());
		
		
		
	}

	@Test
	public void testConnectToCamera()  {
		//start ONVIF Camera emulator
		StreamFetcherUnitTest.startCameraEmulator();

		//create a cam broadcast
		Broadcast newCam = new Broadcast("testAddIPCamera", "127.0.0.1:8080", "admin", "admin",
				"rtsp://11.2.40.63:8554/live1.sdp", AntMediaApplicationAdapter.IP_CAMERA);

		//simulate required operations
		BroadcastRestService streamSourceRest = Mockito.spy(restServiceReal);
		AntMediaApplicationAdapter adaptor = mock (AntMediaApplicationAdapter.class);
		StreamFetcher fetcher = mock (StreamFetcher.class);

		Mockito.doReturn(adaptor).when(streamSourceRest).getApplication();
		Mockito.doReturn(new Result(true)).when(adaptor).startStreaming(newCam);
		Mockito.doReturn(new InMemoryDataStore("testConnectToCamera")).when(streamSourceRest).getDataStore();

		//try to connect to camera
		Result result =	streamSourceRest.connectToCamera(newCam);

		//message should be RTSP address because it is reachable
		assertEquals("rtsp://127.0.0.1:6554/test.flv", result.getMessage());

		//set wrong IP Address
		newCam.setIpAddr("127.0.0.11:8080");

		//try to connect to camera
		result = streamSourceRest.connectToCamera(newCam);

		//message should be connection error code (-1) because IP is set
		assertEquals(-1, result.getErrorId());


		//stop camera emulator
		StreamFetcherUnitTest.stopCameraEmulator();


	}

	@Test
	public void testSearchOnvifDevices()  {

		//start ONVIF Cam emulator
		StreamFetcherUnitTest.startCameraEmulator();

		BroadcastRestService streamSourceRest = Mockito.spy(restServiceReal);

		//start ONVIF discovery
		String result[] = streamSourceRest.searchOnvifDevicesV2();

		//it should not null because discovery is performed
		assertNotNull(result);
		
		//*****************************************************************************
		//*****************************************************************************
		//          PAY ATTENTION
		//TODO: We should enable below assertion to make sure onvif discovery works 
		//however there is a problem in CI. We need to check it on a linux box later. 
		//assertEquals(1, result.length);
		//*****************************************************************************
		//*****************************************************************************
		
		
		//stop camera emulator
		StreamFetcherUnitTest.stopCameraEmulator();

	}
	
	@Test
	public void testGetIPArray() {
		
		BroadcastRestService streamSourceRest = Mockito.spy(restServiceReal);
		String[] ipArray = streamSourceRest.getIPArray(null);
		assertNull(ipArray);
		ipArray = streamSourceRest.getIPArray(new ArrayList<URL>());
		assertNotNull(ipArray);

		try {
			ipArray = streamSourceRest.getIPArray(Arrays.asList(new URL("http://192.168.3.23:8080/onvif/devices")));
			assertEquals(1, ipArray.length);
		} catch (MalformedURLException e) {
		
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
	@Test
	public void testDeviceDiscovery() {
		ExecutorService executor = Executors.newSingleThreadExecutor();
		int randomPort = (int)(Math.random()*5000) + 1024;
		int result = DeviceDiscovery.tryAddress(null, null, null, null, executor, randomPort, null);
		assertEquals(randomPort, result);
		
		result = DeviceDiscovery.tryAddress(null, null, null, null, executor, randomPort, null);
		assertEquals(-1, result);
		
		executor.shutdown();
	}

	@Test
	public void testAddStreamSource()  {

		Result result = new Result(false);

		Broadcast newCam = new Broadcast("testAddStreamSource", "10.2.40.64:8080", "admin", "admin",
				"rtsp://11.2.40.63:8554/live1.sdp", AntMediaApplicationAdapter.STREAM_SOURCE);

		BroadcastRestService streamSourceRest = Mockito.spy(restServiceReal);
		AntMediaApplicationAdapter adaptor = mock (AntMediaApplicationAdapter.class);
		StreamFetcher streamFetcher = mock(StreamFetcher.class);

		Mockito.doReturn(adaptor).when(streamSourceRest).getApplication();
		Mockito.doReturn(new InMemoryDataStore("testAddStreamSource")).when(streamSourceRest).getDataStore();
		Mockito.doReturn(new Result(true)).when(adaptor).startStreaming(Mockito.any());
		
		Mockito.doReturn(new ServerSettings()).when(streamSourceRest).getServerSettings();
		Mockito.doReturn(new AppSettings()).when(streamSourceRest).getAppSettings();
		
		IScope scope = mock(IScope.class);
		when(scope.getName()).thenReturn("junit");
		
		Mockito.doReturn(scope).when(streamSourceRest).getScope();

		ApplicationContext appContext = mock(ApplicationContext.class);

		streamSourceRest.setAppCtx(appContext);

		StatsCollector monitorService = new StatsCollector(); 
		
		when(appContext.getBean(IStatsCollector.BEAN_NAME)).thenReturn(monitorService);

		//define CPU load below limit
		int cpuLoad2 = 70;
		int cpuLimit2 = 80;

		monitorService.setCpuLoad(cpuLoad2);
		monitorService.setCpuLimit(cpuLimit2);
		monitorService.setMinFreeRamSize(0);
		
		result = streamSourceRest.addStreamSource(newCam);

		//should be true, because CPU load is above limit and other parameters defined correctly
		assertTrue(result.isSuccess());
	}
	
	@Test
	public void testcheckStopStreaming() 
	{
		BroadcastRestService streamSourceRest = Mockito.spy(restServiceReal);
		AntMediaApplicationAdapter adaptor = mock (AntMediaApplicationAdapter.class);
		Mockito.doReturn(adaptor).when(streamSourceRest).getApplication();
		Mockito.when(adaptor.getStreamFetcherManager()).thenReturn(mock(StreamFetcherManager.class));
		Mockito.when(adaptor.stopStreaming(any())).thenReturn(new Result(false));
		
		Broadcast broadcast = new Broadcast();
		//It means there is no stream to stop
		assertTrue(streamSourceRest.checkStopStreaming(broadcast));
		
		broadcast.setStatus(AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING);
		//it should return false because adaptor return false
		assertFalse(streamSourceRest.checkStopStreaming(broadcast));
	}
	
	@Test
	public void testUpdateStreamSource() {

		Result result = new Result(false);

		BroadcastRestService streamSourceRest = Mockito.spy(restServiceReal);
		
		AppSettings settings = mock(AppSettings.class);
		when(settings.getListenerHookURL()).thenReturn(null);
		streamSourceRest.setAppSettings(settings);
		
		AntMediaApplicationAdapter adaptor = mock (AntMediaApplicationAdapter.class);
		
		ServerSettings serverSettings = Mockito.mock(ServerSettings.class);
		streamSourceRest.setServerSettings(serverSettings);

		Scope scope = mock(Scope.class);
		String scopeName = "scope";
		when(scope.getName()).thenReturn(scopeName);

		streamSourceRest.setScope(scope);
		
		Broadcast streamSource = new Broadcast("testAddStreamSource", null, null, null,
				"rtsp://11.2.40.63:8554/live1.sdp", AntMediaApplicationAdapter.STREAM_SOURCE);
		
		streamSource.setStatus(AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING);
		
		StreamFetcher fetcher = mock(StreamFetcher.class);
		
		try {
			streamSource.setStreamId("selimTest");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		InMemoryDataStore store = new InMemoryDataStore("test");

		Mockito.doReturn(adaptor).when(streamSourceRest).getApplication();
		Mockito.doReturn(new Result(true)).when(adaptor).startStreaming(streamSource);
		Mockito.doReturn(store).when(streamSourceRest).getDataStore();

		store.save(streamSource);
		
		// Check Stream source update working normal.
		
		Mockito.doReturn(true).when(streamSourceRest).checkStreamUrl(any());
		
		Mockito.doReturn(true).when(streamSourceRest).checkStopStreaming(any());
		
		result = streamSourceRest.updateBroadcast(streamSource.getStreamId(), streamSource);
		
		assertEquals(true, result.isSuccess());
		
		Awaitility.await().atMost(22*250, TimeUnit.MILLISECONDS)
		.until(() -> streamSourceRest.waitStopStreaming(streamSource,false));
		
		// Test line 392 if condition

		Mockito.doReturn(false).when(streamSourceRest).checkStreamUrl(any());
		
		result = streamSourceRest.updateBroadcast(streamSource.getStreamId(), streamSource);
		
		assertEquals(false, result.isSuccess());
		
		// Test line 392 if condition
		
		streamSource.setStatus(null);
		
		Mockito.doReturn(true).when(streamSourceRest).checkStreamUrl(any());
		
		result = streamSourceRest.updateBroadcast(streamSource.getStreamId(), streamSource);
		
		assertEquals(true, result.isSuccess());
		
		result = streamSourceRest.updateBroadcast("not_exists" + (int)(Math.random()*10000), streamSource);
		
		assertEquals(false, result.isSuccess());

		
	}
	

	@Test
	public void testUpdateCamInfo()  {

		Result result = new Result(false);

		Broadcast newCam = new Broadcast("testUpdateCamInfo", "10.2.40.64:8080", "admin", "admin",
				"rtsp://11.2.40.63:8554/live1.sdp", AntMediaApplicationAdapter.IP_CAMERA);


		newCam.setStatus(AntMediaApplicationAdapter.BROADCAST_STATUS_CREATED);
		try {
			newCam.setStreamId("streamId");
		} catch (Exception e) {
			e.printStackTrace();
		}

		BroadcastRestService streamSourceRest = Mockito.spy(restServiceReal);
		AntMediaApplicationAdapter adaptor = mock (AntMediaApplicationAdapter.class);
		StreamFetcher fetcher = mock (StreamFetcher.class);
		InMemoryDataStore store = new InMemoryDataStore("test");

		Result connResult = new Result(true);
		connResult.setMessage("rtsp://11.2.40.63:8554/live1.sdp");

		Mockito.doReturn(connResult).when(streamSourceRest).connectToCamera(newCam);
		Mockito.doReturn(adaptor).when(streamSourceRest).getApplication();
		Mockito.doReturn(new Result(true)).when(adaptor).startStreaming(newCam);
		Mockito.doReturn(store).when(streamSourceRest).getDataStore();
		StreamFetcherManager sfm = mock (StreamFetcherManager.class);
		Mockito.doReturn(sfm).when(adaptor).getStreamFetcherManager();
		Mockito.doReturn(false).when(sfm).isStreamRunning(any());
		newCam.setSubFolder("testFolder");

		store.save(newCam);

		result = streamSourceRest.updateBroadcast(newCam.getStreamId(), newCam);
		
		
		Broadcast broadcast = store.get(newCam.getStreamId());
		assertEquals("testFolder", broadcast.getSubFolder());
		

		assertTrue(result.isSuccess());
		
	}
	
	@Test
	public void testAddStreamSourceWithEndPoint()  {

		Result result = new Result(false);
		//When there is no endpoint defined
		Broadcast source = new Broadcast("test_1");
		source.setDescription("");
		source.setIs360(false);
		source.setPublicStream(false);
		source.setType(AntMediaApplicationAdapter.STREAM_SOURCE);

		BroadcastRestService streamSourceRest = Mockito.spy(restServiceReal);
		AntMediaApplicationAdapter adaptor = mock (AntMediaApplicationAdapter.class);

		Mockito.doReturn(adaptor).when(streamSourceRest).getApplication();
		Mockito.doReturn(new InMemoryDataStore("testAddStreamSourceWithEndPoint")).when(streamSourceRest).getDataStore();
		Mockito.doReturn(true).when(streamSourceRest).checkStreamUrl(any());
		StreamFetcher fetcher = mock (StreamFetcher.class);
		Mockito.when(adaptor.startStreaming(Mockito.any())).thenReturn(new Result(true));
		StreamFetcherManager sfm = mock (StreamFetcherManager.class);
		Mockito.doReturn(sfm).when(adaptor).getStreamFetcherManager();
		Mockito.doReturn(false).when(sfm).isStreamRunning(any());
		
		Mockito.doReturn(new ServerSettings()).when(streamSourceRest).getServerSettings();
		Mockito.doReturn(new AppSettings()).when(streamSourceRest).getAppSettings();
		
		IScope scope = mock(IScope.class);
		when(scope.getName()).thenReturn("junit");
		
		Mockito.doReturn(scope).when(streamSourceRest).getScope();


		ApplicationContext appContext = mock(ApplicationContext.class);

		streamSourceRest.setAppCtx(appContext);

		StatsCollector monitorService = new StatsCollector();

		when(appContext.getBean(IStatsCollector.BEAN_NAME)).thenReturn(monitorService);

		//define CPU load below limit
		int cpuLoad2 = 70;
		int cpuLimit2 = 80;

		monitorService.setCpuLoad(cpuLoad2);
		monitorService.setCpuLimit(cpuLimit2);
		monitorService.setMinFreeRamSize(0);

		result = streamSourceRest.addStreamSource(source);
		assertNull(source.getEndPointList());

		//When there is an endpoint defined
		Broadcast source2 = new Broadcast("test_2");
		source2.setDescription("");
		source2.setIs360(false);
		source2.setPublicStream(false);
		source2.setType(AntMediaApplicationAdapter.STREAM_SOURCE);
		Endpoint endpoint = new Endpoint();
		endpoint.setRtmpUrl("rtmp://127.0.0.1");
		
		source2.setEndPointList(Arrays.asList(endpoint));

		result = streamSourceRest.addStreamSource(source2);
		assertEquals(1, source2.getEndPointList().size());

		Broadcast source3 = new Broadcast("test_3");
		source3.setDescription("");
		source3.setIs360(false);
		source3.setPublicStream(false);
		source3.setType(AntMediaApplicationAdapter.STREAM_SOURCE);
		Endpoint endpoint2 = new Endpoint();
		endpoint2.setRtmpUrl("rtmp://127.0.0.1");
		
		source3.setEndPointList(Arrays.asList(endpoint, endpoint2));

		//When there is two endpoints defined
		result = streamSourceRest.addStreamSource(source3);
		assertEquals(2, source3.getEndPointList().size());

		
		source.setEndPointList(Arrays.asList(endpoint));
		assertEquals(1, source.getEndPointList().size());
		//update first source now. At the moment we have endpoint_1
		result = streamSourceRest.updateBroadcast(source.getStreamId(), source);
		assertEquals(1, source.getEndPointList().size());
	}

	@Test
	public void testRTMPWebRTCStats()  {
		Scope scope = mock(Scope.class);
		String scopeName = "scope";
		when(scope.getName()).thenReturn(scopeName);

		AntMediaApplicationAdapter app = new AntMediaApplicationAdapter();

		ApplicationContext context = mock(ApplicationContext.class);

		restServiceReal.setAppCtx(context);
		restServiceReal.setApplication(app);
		restServiceReal.setScope(scope);
		assertNotNull(restServiceReal.getRTMPToWebRTCStats("stream1"));
	}
	
	@Test
	public void testAddSubtrack()  {
		String mainTrackId = RandomStringUtils.randomAlphanumeric(8);
		String subTrackId = RandomStringUtils.randomAlphanumeric(8);
		
		Broadcast mainTrack= new Broadcast();
		try {
			mainTrack.setStreamId(mainTrackId);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		Broadcast subtrack= new Broadcast();
		try {
			subtrack.setStreamId(subTrackId);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		BroadcastRestService broadcastRestService = new BroadcastRestService();
		DataStore datastore = Mockito.spy(new InMemoryDataStore("dummy"));
		datastore.save(mainTrack);
		datastore.save(subtrack);
		broadcastRestService.setDataStore(datastore);

		assertTrue(mainTrack.getSubTrackStreamIds().isEmpty());
		assertNull(subtrack.getMainTrackStreamId());
		
		broadcastRestService.addSubTrack(mainTrackId, subTrackId);
		
		assertEquals(1, mainTrack.getSubTrackStreamIds().size());
		assertEquals(subTrackId, mainTrack.getSubTrackStreamIds().get(0));
		assertEquals(mainTrackId, subtrack.getMainTrackStreamId());
		
		Result result = broadcastRestService.addSubTrack("trackIdNotExist", "subtrackNotExist");
		assertFalse(result.isSuccess());
		
		result = broadcastRestService.addSubTrack("trackIdNotExist", subTrackId);
		assertFalse(result.isSuccess());
		
		ConferenceRoom conferenceRoom = new ConferenceRoom();
		conferenceRoom.setRoomId(mainTrackId);
		assertTrue(datastore.createConferenceRoom(conferenceRoom));
		
		Mockito.doReturn(false).when(datastore).updateBroadcastFields(Mockito.any(), Mockito.any());
		result = broadcastRestService.addSubTrack(mainTrackId, subTrackId);
		assertFalse(result.isSuccess());
		
		
		Mockito.doReturn(true).when(datastore).updateBroadcastFields(Mockito.any(), Mockito.any());
		result = broadcastRestService.addSubTrack(mainTrackId, subTrackId);
		assertTrue(result.isSuccess());
		
		conferenceRoom = datastore.getConferenceRoom(mainTrackId);
		assertEquals(1,conferenceRoom.getRoomStreamList().size());

		
	}

	@Test
	public void testRemoveSubtrack()  {
		String mainTrackId = RandomStringUtils.randomAlphanumeric(8);
		String subTrackId = RandomStringUtils.randomAlphanumeric(8);

		Broadcast mainTrack= new Broadcast();
		try {
			mainTrack.setStreamId(mainTrackId);
			mainTrack.setSubTrackStreamIds(new ArrayList<>(Arrays.asList(subTrackId)));
		} catch (Exception e) {
			e.printStackTrace();
		}

		Broadcast subtrack= new Broadcast();
		try {
			subtrack.setStreamId(subTrackId);
			subtrack.setMainTrackStreamId(mainTrackId);
		} catch (Exception e) {
			e.printStackTrace();
		}

		BroadcastRestService broadcastRestService = new BroadcastRestService();
		DataStore datastore = Mockito.spy(new InMemoryDataStore("dummy"));
		datastore.save(mainTrack);
		datastore.save(subtrack);
		broadcastRestService.setDataStore(datastore);

		assertTrue(mainTrack.getSubTrackStreamIds().size() == 1);
		assertEquals(subTrackId, mainTrack.getSubTrackStreamIds().get(0));
		assertEquals(mainTrackId, subtrack.getMainTrackStreamId());

		Result result = broadcastRestService.removeSubTrack(mainTrackId, subTrackId);
		assertTrue(result.isSuccess());

		result = broadcastRestService.removeSubTrack(mainTrackId, "notExistSubTrackId");
		assertFalse(result.isSuccess());

		result = broadcastRestService.removeSubTrack("notExistMainTrackId", subTrackId);
		assertFalse(result.isSuccess());

	}
	
	@Test
	public void testGetStreamInfo() {
		BroadcastRestService broadcastRestService = Mockito.spy(new BroadcastRestService());
		MongoStore datastore = new MongoStore("localhost", "", "", "testdb");
		broadcastRestService.setDataStore(datastore);
		StreamInfo streamInfo = new StreamInfo(true, 720, 1080, 300, true, 64, 1000, 1000, VideoCodec.H264);
		String streamId = "streamId" + (int)(Math.random()*10000);
		streamInfo.setStreamId(streamId);
		
		datastore.saveStreamInfo(streamInfo);
	
		ApplicationContext context = mock(ApplicationContext.class);
		Mockito.doReturn(context).when(broadcastRestService).getAppContext();
		when(context.containsBean(any())).thenReturn(true);
		
		BasicStreamInfo[] streamInfo2 = broadcastRestService.getStreamInfo(streamId);
		
		assertEquals(1, streamInfo2.length);
		assertEquals(64, streamInfo2[0].getAudioBitrate());
		assertEquals(300, streamInfo2[0].getVideoBitrate());
		assertEquals(1080, streamInfo2[0].getVideoWidth());
		assertEquals(720, streamInfo2[0].getVideoHeight());
		assertEquals(VideoCodec.H264, streamInfo2[0].getVideoCodec());
		
		
		when(context.containsBean(any())).thenReturn(false);
		IWebRTCAdaptor webrtcAdaptor = Mockito.mock(IWebRTCAdaptor.class);
		when(context.getBean(IWebRTCAdaptor.BEAN_NAME)).thenReturn(webrtcAdaptor);
		streamInfo2 = broadcastRestService.getStreamInfo(streamId);
		assertEquals(0, streamInfo2.length);
		
		Mockito.when(webrtcAdaptor.getStreamInfo(streamId)).thenReturn(Arrays.asList(streamInfo));
		streamInfo2 = broadcastRestService.getStreamInfo(streamId);
		assertEquals(1, streamInfo2.length);
		assertEquals(64, streamInfo2[0].getAudioBitrate());
		assertEquals(300, streamInfo2[0].getVideoBitrate());
		assertEquals(1080, streamInfo2[0].getVideoWidth());
		assertEquals(720, streamInfo2[0].getVideoHeight());
		assertEquals(VideoCodec.H264, streamInfo2[0].getVideoCodec());
		
		
		Mockito.when(webrtcAdaptor.getStreamInfo(streamId)).thenReturn(null);
		streamInfo2 = broadcastRestService.getStreamInfo(streamId);
		assertEquals(0, streamInfo2.length);
		
		
	}
	
	@Test
	public void testSendMessage()  {
		Scope scope = mock(Scope.class);
		String scopeName = "scope";
		when(scope.getName()).thenReturn(scopeName);
		
		String streamId = "stream1";
		String message = "hi";
		
		// test the case of data channels not enabled
		AntMediaApplicationAdapter app = new AntMediaApplicationAdapter();
		AntMediaApplicationAdapter appSpy = Mockito.spy(app);

		ApplicationContext context = mock(ApplicationContext.class);

		restServiceReal.setAppCtx(context);
		restServiceReal.setApplication(appSpy);
		restServiceReal.setScope(scope);
		
		Result res = restServiceReal.sendMessage(message,streamId);
		assertEquals(false, res.isSuccess());
		
		// test the case of data channels not enabled
		AntMediaApplicationAdapter app2 = new AntMediaApplicationAdapter();
		AntMediaApplicationAdapter appSpy2 = Mockito.spy(app2);
		Mockito.doReturn(true).when(appSpy2).isDataChannelMessagingSupported();
		restServiceReal.setApplication(appSpy2);
		
		res = restServiceReal.sendMessage(message,streamId);
		assertEquals(false, res.isSuccess());
		assertEquals("Data channels are not enabled", res.getMessage());
		
		AntMediaApplicationAdapter app3 = new AntMediaApplicationAdapter();
		AntMediaApplicationAdapter appSpy3 = Mockito.spy(app3);
		Mockito.doReturn(true).when(appSpy3).isDataChannelMessagingSupported();
		Mockito.doReturn(true).when(appSpy3).isDataChannelEnabled();
		
		restServiceReal.setApplication(appSpy3);
		
		res = restServiceReal.sendMessage(message,streamId);
		assertEquals(false, res.isSuccess());
		assertEquals("Requested WebRTC stream does not exist", res.getMessage());
		
		AntMediaApplicationAdapter app4 = new AntMediaApplicationAdapter();
		AntMediaApplicationAdapter appSpy4 = Mockito.spy(app4);
		Mockito.doReturn(true).when(appSpy4).isDataChannelMessagingSupported();
		Mockito.doReturn(true).when(appSpy4).isDataChannelEnabled();
		Mockito.doReturn(true).when(appSpy4).doesWebRTCStreamExist(streamId);
		
		restServiceReal.setApplication(appSpy4);
	    
		res = restServiceReal.sendMessage(message,streamId);
		
		// check if returned result is true
		assertEquals(false, res.isSuccess());
		assertEquals("Operation not completed", res.getMessage());
		
	}



	@Test
	public void testGetRoomInfo()  {
		ApplicationContext context = mock(ApplicationContext.class);
		restServiceReal.setAppCtx(context);
		DataStore store = new InMemoryDataStore("testdb");
		restServiceReal.setDataStore(store);
		BroadcastRestService restServiceSpy = Mockito.spy(restServiceReal);
		ConferenceRoom room=new ConferenceRoom();
		room.setRoomId("testroom");
		Broadcast broadcast1=new Broadcast();
		Broadcast broadcast2=new Broadcast();
		try {
			broadcast1.setStreamId("stream1");
			broadcast1.setStatus(AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING);
			broadcast2.setStreamId("stream2");
			broadcast2.setStatus(AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING);
		} catch (Exception e) {
			e.printStackTrace();
		}
		store.save(broadcast1);
		store.save(broadcast2);
		List<String> streamIdList=new ArrayList<>();
		streamIdList.add("stream1");
		streamIdList.add("stream2");
		room.setRoomStreamList(streamIdList);
		store.createConferenceRoom(room);
		//If the stream id is provided in the list, it won't return that stream id. This is query parameter in the rest.
		RootRestService.RoomInfo testroom=restServiceSpy.getRoomInfo("testroom","stream1");
		assertEquals("testroom",testroom.getRoomId());
		assertEquals(1,testroom.getStreamDetailsMap().size());
		testroom=restServiceSpy.getRoomInfo("testroom","stream3");
		assertEquals("testroom",testroom.getRoomId());
		assertEquals(2,testroom.getStreamDetailsMap().size());
		testroom=restServiceSpy.getRoomInfo("someunknownroom","stream1");
		//Even though room is not defined yet, it will not return null.
		assertNotNull(testroom);
		assertEquals("someunknownroom",testroom.getRoomId());
		testroom=restServiceSpy.getRoomInfo(null,"stream1");
		assertNull(null,testroom.getRoomId());
	}

	@Test
	public void testaddStreamToTheRoom(){
		ApplicationContext currentcontext = mock(ApplicationContext.class);
		restServiceReal.setAppCtx(currentcontext);
		DataStore store = new InMemoryDataStore("testdb");
		restServiceReal.setDataStore(store);
		BroadcastRestService restServiceSpy = Mockito.spy(restServiceReal);
		
		AntMediaApplicationAdapter app = mock(AntMediaApplicationAdapter.class);
		when(currentcontext.getBean(AntMediaApplicationAdapter.BEAN_NAME)).thenReturn(app);
		
		ConferenceRoom room=new ConferenceRoom();
		room.setRoomId("testroom");
		store.createConferenceRoom(room);
		Broadcast broadcast1=new Broadcast();
		Broadcast broadcast2=new Broadcast();
		Broadcast broadcast3=new Broadcast();
		Broadcast broadcast4=new Broadcast();
		Broadcast broadcast5=new Broadcast();
		try {
			broadcast1.setStreamId("stream1");
			broadcast1.setStatus(AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING);
			broadcast2.setStreamId("stream2");
			broadcast2.setStatus(AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING);
			broadcast3.setStreamId("stream3");
			broadcast3.setStatus(AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING);
			broadcast4.setStreamId("stream4");
			broadcast5.setStreamId("stream5");
		} catch (Exception e) {
			e.printStackTrace();
		}
		store.save(broadcast1);
		store.save(broadcast2);
		store.save(broadcast3);
		store.save(broadcast4);
		store.save(broadcast5);
		restServiceSpy.addStreamToTheRoom("testroom","stream1");
		assertEquals(1,store.getConferenceRoom("testroom").getRoomStreamList().size());
		verify(app, times(1)).joinedTheRoom("testroom", "stream1");
		restServiceSpy.addStreamToTheRoom("testroom","stream2");
		assertEquals(2,store.getConferenceRoom("testroom").getRoomStreamList().size());
		restServiceSpy.addStreamToTheRoom(null,"stream3");
		assertEquals(2,store.getConferenceRoom("testroom").getRoomStreamList().size());
		restServiceSpy.addStreamToTheRoom("someunknownroom","stream3");
		assertEquals(2,store.getConferenceRoom("testroom").getRoomStreamList().size());
		restServiceSpy.addStreamToTheRoom("testroom","stream4");
		assertEquals(3,store.getConferenceRoom("testroom").getRoomStreamList().size());
		restServiceSpy.addStreamToTheRoom("testroom", "stream5");
		assertEquals(4,store.getConferenceRoom("testroom").getRoomStreamList().size());
	}

	@Test
	public void testremoveStreamFromRoom(){
		ApplicationContext currentcontext = mock(ApplicationContext.class);
		restServiceReal.setAppCtx(currentcontext);
		DataStore store = new InMemoryDataStore("testdb");
		restServiceReal.setDataStore(store);
		
		AntMediaApplicationAdapter app = mock(AntMediaApplicationAdapter.class);
		when(currentcontext.getBean(AntMediaApplicationAdapter.BEAN_NAME)).thenReturn(app);
		
		BroadcastRestService restServiceSpy = Mockito.spy(restServiceReal);
		ConferenceRoom room=new ConferenceRoom();
		room.setRoomId("testroom");
		Broadcast broadcast1=new Broadcast();
		Broadcast broadcast2=new Broadcast();
		try {
			broadcast1.setStreamId("stream1");
			broadcast1.setStatus(AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING);
			broadcast2.setStreamId("stream2");
			broadcast2.setStatus(AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING);
		} catch (Exception e) {
			e.printStackTrace();
		}
		store.save(broadcast1);
		store.save(broadcast2);
		List<String> streamIdList=new ArrayList<>();
		streamIdList.add("stream1");
		streamIdList.add("stream2");
		room.setRoomStreamList(streamIdList);
		store.createConferenceRoom(room);
		assertEquals(2,store.getConferenceRoom("testroom").getRoomStreamList().size());
		restServiceSpy.deleteStreamFromTheRoom("testroom","stream2");
		verify(app, times(1)).leftTheRoom("testroom", "stream2");
		assertEquals(1,store.getConferenceRoom("testroom").getRoomStreamList().size());
		restServiceSpy.deleteStreamFromTheRoom(null,"stream2");
		assertEquals(1,store.getConferenceRoom("testroom").getRoomStreamList().size());
		restServiceSpy.deleteStreamFromTheRoom("testroom","someunknownstream");
		assertEquals(1,store.getConferenceRoom("testroom").getRoomStreamList().size());
		restServiceSpy.deleteStreamFromTheRoom("someunknownroom","stream1");
		assertEquals(1,store.getConferenceRoom("testroom").getRoomStreamList().size());
		restServiceSpy.deleteStreamFromTheRoom("testroom","stream1");
		assertEquals(0,store.getConferenceRoom("testroom").getRoomStreamList().size());
	}
	
	
	@Test
	public void testWebRTCViewerRestOperations(){
		DataStore store = new InMemoryDataStore("testdb");
		restServiceReal.setDataStore(store);
		BroadcastRestService restServiceSpy = Mockito.spy(restServiceReal);
		assertEquals(0, restServiceSpy.getWebRTCViewerList(0, 5, "", "", "").size());
		
		WebRTCViewerInfo wwi = new WebRTCViewerInfo();
		String streamId = "stream"+RandomStringUtils.randomAlphanumeric(5);
		String viewerId = "viewer"+RandomStringUtils.randomAlphanumeric(5);
		String edgeAddress = RandomStringUtils.randomAlphanumeric(10);
		wwi.setStreamId(streamId);
		wwi.setViewerId(viewerId);
		wwi.setEdgeAddress(edgeAddress);
		
		store.saveViewerInfo(wwi);
		List<WebRTCViewerInfo> wwiList = restServiceSpy.getWebRTCViewerList(0, 5, "", "", "");
		assertEquals(1, wwiList.size());
		
		assertEquals(streamId, wwiList.get(0).getStreamId());
		assertEquals(viewerId, wwiList.get(0).getViewerId());
		assertEquals(edgeAddress, wwiList.get(0).getEdgeAddress());
		
		AntMediaApplicationAdapter testApp = Mockito.spy(new AntMediaApplicationAdapter());
		restServiceSpy.setApplication(testApp);
		restServiceSpy.stopPlaying(viewerId);
		verify(testApp, times(1)).stopPlaying(viewerId);
	}
	
	@Test
	public void testGetCameraProfiles() {
		//start ONVIF Camera emulator
		StreamFetcherUnitTest.startCameraEmulator();

		//create a cam broadcast
		Broadcast newCam = new Broadcast("testAddIPCamera", "127.0.0.1:8080", "admin", "admin",
				"", AntMediaApplicationAdapter.IP_CAMERA);
		try {
			newCam.setStreamId("test");
		} catch (Exception e) {
			e.printStackTrace();
		}

		//simulate required operations
		BroadcastRestService streamSourceRest = Mockito.spy(restServiceReal);
		AntMediaApplicationAdapter adaptor = Mockito.spy (new AntMediaApplicationAdapter());

		InMemoryDataStore datastore = new InMemoryDataStore("testConnectToCamera");
		
		Mockito.doReturn(adaptor).when(streamSourceRest).getApplication();
		Mockito.doReturn(new Result(true)).when(adaptor).startStreaming(newCam);
		Mockito.doReturn(datastore).when(streamSourceRest).getDataStore();
		Mockito.doReturn(datastore).when(adaptor).getDataStore();

		
		IScope scope = mock(IScope.class);
		Mockito.doReturn(scope).when(streamSourceRest).getScope();
		
		Mockito.doReturn(new ServerSettings()).when(streamSourceRest).getServerSettings();
		Mockito.doReturn(new AppSettings()).when(streamSourceRest).getAppSettings();
		
		//add IP Camera first
		assertTrue(streamSourceRest.addIPCamera(newCam).isSuccess());


		String[] profiles = streamSourceRest.getOnvifDeviceProfiles(newCam.getStreamId());
		
		assertEquals(2, profiles.length);
		
		assertNull(streamSourceRest.getOnvifDeviceProfiles("invalid id"));

	}
	
}
