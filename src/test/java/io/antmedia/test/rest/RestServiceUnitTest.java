package io.antmedia.test.rest;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.red5.server.api.stream.IClientBroadcastStream;
import org.red5.server.api.stream.IStreamCapableConnection;
import org.red5.server.scope.Scope;
import org.red5.server.scope.WebScope;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;

import com.google.gson.Gson;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.AppSettings;
import io.antmedia.datastore.db.IDataStore;
import io.antmedia.datastore.db.InMemoryDataStore;
import io.antmedia.datastore.db.MapDBStore;
import io.antmedia.datastore.db.MongoStore;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.datastore.db.types.Endpoint;
import io.antmedia.datastore.db.types.SocialEndpointCredentials;
import io.antmedia.datastore.db.types.TensorFlowObject;
import io.antmedia.datastore.db.types.Token;
import io.antmedia.datastore.db.types.VoD;
import io.antmedia.integration.MuxingTest;
import io.antmedia.rest.BroadcastRestService;
import io.antmedia.rest.BroadcastRestService.BroadcastStatistics;
import io.antmedia.rest.BroadcastRestService.ProcessBuilderFactory;
import io.antmedia.rest.model.Interaction;
import io.antmedia.rest.model.Result;
import io.antmedia.rest.model.User;
import io.antmedia.rest.model.Version;
import io.antmedia.social.LiveComment;
import io.antmedia.social.ResourceOrigin;
import io.antmedia.social.endpoint.PeriscopeEndpoint;
import io.antmedia.social.endpoint.VideoServiceEndpoint;
import io.antmedia.social.endpoint.VideoServiceEndpoint.DeviceAuthParameters;


@ContextConfiguration(locations = { "test.xml" })
public class RestServiceUnitTest {


	private BroadcastRestService restServiceReal = null;
	public AntMediaApplicationAdapter app = null;

	static {
		System.setProperty("red5.deployment.type", "junit");
		System.setProperty("red5.root", ".");
	}


	@Before
	public void before() {
		restServiceReal = new BroadcastRestService();
	}

	@After
	public void after() {
		restServiceReal = null;
	}



	/**
	 * These tests should be run with stalker db
	 */
	@Test
	public void testImportLiveStreams2Stalker()  {
		AppSettings settings = mock(AppSettings.class);


		when(settings.getStalkerDBServer()).thenReturn("192.168.1.29");
		when(settings.getStalkerDBUsername()).thenReturn("stalker");
		when(settings.getStalkerDBPassword()).thenReturn("1");
		when(settings.getServerName()).thenReturn("localhost");

		Scope scope = mock(Scope.class);
		String scopeName = "scope";
		when(scope.getName()).thenReturn(scopeName);

		restServiceReal.setScope(scope);

		restServiceReal.setAppSettings(settings);

		Broadcast broadcast = new Broadcast(null, "name");
		IDataStore store = new InMemoryDataStore("testdb");
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

			Broadcast createBroadcast = restServiceReal.createBroadcast(broadcast);
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
		when(settings.getServerName()).thenReturn("localhost");

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
		VoD newVod = new VoD("vodFile", "vodFile", file.getPath(), file.getName(), System.currentTimeMillis(), 0, 6000,
				VoD.USER_VOD,vodId);
		IDataStore store = new InMemoryDataStore("testdb");
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
	public void testDeleteVoD() {
		InMemoryDataStore datastore = new InMemoryDataStore("datastore");
		restServiceReal.setDataStore(datastore);
		
		String vodId = RandomStringUtils.randomNumeric(24);
		
		VoD streamVod = new VoD("streamName", "streamId", "filePath", "vodName", 111, 111, 111, VoD.STREAM_VOD, vodId);
		datastore.addVod(streamVod);
		
		assertNotNull(datastore.getVoD(vodId));
		
		Scope scope = mock(Scope.class);
		String scopeName = "junit";
		when(scope.getName()).thenReturn(scopeName);
		
		AntMediaApplicationAdapter app = mock(AntMediaApplicationAdapter.class);
		when(app.getScope()).thenReturn(scope);
		
		ApplicationContext context = mock(ApplicationContext.class);
		when(context.getBean(AntMediaApplicationAdapter.BEAN_NAME)).thenReturn(app);
		
		restServiceReal.setAppCtx(context);
		
		VoD voD = restServiceReal.getVoD(vodId);
		assertEquals(vodId, voD.getVodId());
		assertEquals(streamVod.getStreamId(), voD.getStreamId());
		assertEquals(streamVod.getVodName(), voD.getVodName());
		assertEquals(streamVod.getFilePath(), voD.getFilePath());
		
		assertEquals(1, restServiceReal.getVodList(0, 50).size());
		
		restServiceReal.deleteVoD(vodId);
		
		assertEquals(0, restServiceReal.getVodList(0, 50).size());
		
		assertNull(datastore.getVoD(vodId));
		
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
		assertEquals(-1, broadcastStatistics.totalRTMPWatchersCount);
		assertEquals(-1, broadcastStatistics.totalWebRTCWatchersCount);
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
		String streamId = dataStore.save(broadcast);
		
		dataStore.updateHLSViewerCount(streamId, 30);
		BroadcastStatistics broadcastStatistics = restServiceReal.getBroadcastStatistics(streamId);
		assertNotNull(broadcastStatistics);
		assertEquals(30, broadcastStatistics.totalHLSWatchersCount);
		
	}
	
	
	@Test
	public void testGetDeviceAuthparameters() {
		//this is community edition

		//make client id and client secret null for facebook
		AppSettings settings = mock(AppSettings.class);
		when(settings.getFacebookClientId()).thenReturn(null);
		when(settings.getFacebookClientSecret()).thenReturn(null);

		Scope scope = mock(Scope.class);
		String scopeName = "scope";
		when(scope.getName()).thenReturn(scopeName);

		AntMediaApplicationAdapter app = new AntMediaApplicationAdapter();


		restServiceReal.setApplication(app);
		restServiceReal.setScope(scope);
		restServiceReal.setDataStore(new InMemoryDataStore("testdb"));

		restServiceReal.setAppSettings(settings);

		//get device auth parameters for facebook
		Result object = (Result)restServiceReal.getDeviceAuthParameters("facebook");

		// it should be facebook is not defined in this scope
		assertFalse(object.isSuccess());
		assertEquals(BroadcastRestService.ERROR_SOCIAL_ENDPOINT_UNDEFINED_ENDPOINT, object.getErrorId());

		//make client id and client secret has value for facebook
		when(settings.getFacebookClientId()).thenReturn("12313");
		when(settings.getFacebookClientSecret()).thenReturn("sdfsfsf");

		// get device auth parameter for facebook
		object = (Result)restServiceReal.getDeviceAuthParameters("facebook");

		//it should be again facebook is not defined in this scope
		assertFalse(object.isSuccess());
		assertEquals(BroadcastRestService.ERROR_SOCIAL_ENDPOINT_UNDEFINED_ENDPOINT, object.getErrorId());

		//make the same test for youtube and expect same results
		when(settings.getYoutubeClientId()).thenReturn(null);
		when(settings.getYoutubeClientSecret()).thenReturn(null);
		object = (Result)restServiceReal.getDeviceAuthParameters("youtube");

		assertFalse(object.isSuccess());
		assertEquals(BroadcastRestService.ERROR_SOCIAL_ENDPOINT_UNDEFINED_ENDPOINT, object.getErrorId());

		when(settings.getYoutubeClientId()).thenReturn("121212");
		when(settings.getYoutubeClientSecret()).thenReturn("1212121");

		object = (Result)restServiceReal.getDeviceAuthParameters("youtube");

		assertFalse(object.isSuccess());
		assertEquals(BroadcastRestService.ERROR_SOCIAL_ENDPOINT_UNDEFINED_ENDPOINT, object.getErrorId());

		//make client id and clien secret null for periscope
		when(settings.getPeriscopeClientId()).thenReturn(null);
		when(settings.getPeriscopeClientSecret()).thenReturn(null);

		//get device auth parameter for periscope
		object = (Result)restServiceReal.getDeviceAuthParameters("periscope");

		//it should be client id and client secret missing
		assertFalse(object.isSuccess());
		assertEquals(BroadcastRestService.ERROR_SOCIAL_ENDPOINT_UNDEFINED_CLIENT_ID, object.getErrorId());

		//make client id and client secret have value for periscope
		when(settings.getPeriscopeClientId()).thenReturn("121212");
		when(settings.getPeriscopeClientSecret()).thenReturn("121212");

		//it should be different error because client id and cleint secret is not correct
		object  = (Result) restServiceReal.getDeviceAuthParameters("periscope");
		assertFalse(object.isSuccess());
		assertEquals(BroadcastRestService.ERROR_SOCIAL_ENDPOINT_EXCEPTION_IN_ASKING_AUTHPARAMS, object.getErrorId());
	}

	@Test
	public void testSettingsListenerHookURL() {
		AppSettings settings = mock(AppSettings.class);
		String hookURL = "http://url_hook";
		when(settings.getListenerHookURL()).thenReturn(hookURL);

		String serverName = "fually.qualified.domain.name";
		when(settings.getServerName()).thenReturn(serverName);

		Scope scope = mock(Scope.class);
		String scopeName = "scope";
		when(scope.getName()).thenReturn(scopeName);

		restServiceReal.setScope(scope);

		restServiceReal.setAppSettings(settings);

		Broadcast broadcast = new Broadcast(null, "name");
		IDataStore store = new InMemoryDataStore("testdb");
		restServiceReal.setDataStore(store);
		Broadcast createBroadcast = restServiceReal.createBroadcast(broadcast);

		assertEquals(hookURL, createBroadcast.getListenerHookURL());

		Broadcast broadcastTmp = restServiceReal.getBroadcast(createBroadcast.getStreamId());

		assertEquals(hookURL, broadcastTmp.getListenerHookURL());

	}


	@Test
	public void testAddEndpoint() {
		AppSettings settings = mock(AppSettings.class);
		String serverName = "fully.qualified.domain.name";
		when(settings.getServerName()).thenReturn(serverName);
		restServiceReal.setAppSettings(settings);

		Broadcast broadcast = new Broadcast(null, "name");
		IDataStore store = new InMemoryDataStore("testdb");
		restServiceReal.setDataStore(store);

		Scope scope = mock(Scope.class);
		String scopeName = "scope";
		when(scope.getName()).thenReturn(scopeName);
		restServiceReal.setScope(scope);

		Broadcast createBroadcast = restServiceReal.createBroadcast(broadcast);
		String streamId = createBroadcast.getStreamId();
		assertNotNull(streamId);

		String endpointURL = "rtmp://test.endpoint.url/test";
		Result result = restServiceReal.addEndpoint(streamId, endpointURL);
		assertTrue(result.isSuccess());

		Broadcast broadcast2 = restServiceReal.getBroadcast(streamId);
		assertEquals(broadcast.getStreamId(), broadcast2.getStreamId());

		assertEquals(1, broadcast2.getEndPointList().size());
		Endpoint endpoint = broadcast2.getEndPointList().get(0);
		assertEquals(endpointURL, endpoint.getRtmpUrl());
		assertEquals("generic", endpoint.type);
	}

	

	@Test
	public void testAddSocialEndpoint() {
		AppSettings settings = mock(AppSettings.class);
		String serverName = "fully.qualified.domain.name";
		when(settings.getServerName()).thenReturn(serverName);
		restServiceReal.setAppSettings(settings);

		Broadcast broadcast = new Broadcast(null, "name");
		IDataStore store = new InMemoryDataStore("testdb");
		restServiceReal.setDataStore(store);

		Scope scope = mock(Scope.class);
		String scopeName = "scope";
		when(scope.getName()).thenReturn(scopeName);
		restServiceReal.setScope(scope);

		AntMediaApplicationAdapter appAdaptor = mock(AntMediaApplicationAdapter.class);

		restServiceReal.setApplication(appAdaptor);

		Broadcast broadcastCreated = restServiceReal.createBroadcast(broadcast);

		Result result = restServiceReal.addSocialEndpoint(broadcastCreated.getStreamId(), "not_exist");
		assertFalse(result.isSuccess());

		when(appAdaptor.getVideoServiceEndpoints()).thenReturn(null);
		result = restServiceReal.addSocialEndpoint(broadcastCreated.getStreamId(), "not_exist");
		assertFalse(result.isSuccess());

		result = restServiceReal.addSocialEndpoint("not_exist", "not exist");
		assertFalse(result.isSuccess());


		Map<String, VideoServiceEndpoint> endpointList = new HashMap<>();
		VideoServiceEndpoint videoServiceEndpoint = mock(VideoServiceEndpoint.class);
		String endpointServiceId = "mock_endpoint";
		SocialEndpointCredentials credentials = new SocialEndpointCredentials();
		credentials.setId(endpointServiceId);
		when(videoServiceEndpoint.getCredentials()).thenReturn(credentials);
		endpointList.put(endpointServiceId, videoServiceEndpoint);

		when(appAdaptor.getVideoServiceEndpoints()).thenReturn(endpointList);

		try {

			String broadcastId = "broadcastId"  + (int)(Math.random() * 10000);
			String streamId = "streamId"  + (int)(Math.random() * 10000);
			String name = "broadcastId"  + (int)(Math.random() * 10000);
			String rtmpUrl = "rtmpUrl"  + (int)(Math.random() * 10000);
			String type = "type"  + (int)(Math.random() * 10000);

			when(videoServiceEndpoint.createBroadcast(broadcastCreated.getName(), broadcastCreated.getDescription(), 
					broadcastCreated.getStreamId(), broadcastCreated.isIs360(), broadcastCreated.isPublicStream(), 720, true))
			.thenReturn(new Endpoint(broadcastId, streamId, name, rtmpUrl, type, endpointServiceId, broadcastCreated.getStreamId()));

			result = restServiceReal.addSocialEndpoint(broadcastCreated.getStreamId(), endpointServiceId);
			assertTrue(result.isSuccess());

			Mockito.verify(videoServiceEndpoint).createBroadcast(broadcastCreated.getName(), broadcastCreated.getDescription(),
					 broadcastCreated.getStreamId(), broadcastCreated.isIs360(), broadcastCreated.isPublicStream(), 720, true);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		result = restServiceReal.revokeSocialNetwork(endpointServiceId);
		assertTrue(result.isSuccess());

		Mockito.verify(videoServiceEndpoint).resetCredentials();
		assertTrue(endpointList.isEmpty());

		result = restServiceReal.revokeSocialNetwork("not_exist");
		assertFalse(result.isSuccess());

		when(appAdaptor.getVideoServiceEndpoints()).thenReturn(null);
		result = restServiceReal.revokeSocialNetwork("not_exist");
		assertFalse(result.isSuccess());

	}
	
	
	@Test
	public void testGetSocialEndpoints() {
		BroadcastRestService restServiceSpy = Mockito.spy(restServiceReal);
		AntMediaApplicationAdapter application = mock(AntMediaApplicationAdapter.class);
		Map<String, VideoServiceEndpoint> endpointMap = new HashMap<>();
		
		PeriscopeEndpoint endpoint = mock(PeriscopeEndpoint.class);
		String endpointId =  RandomStringUtils.randomAlphabetic(5);
		endpointMap.put(endpointId, endpoint);
		
		PeriscopeEndpoint endpoint2 = mock(PeriscopeEndpoint.class);
		String endpointId2 =  RandomStringUtils.randomAlphabetic(5);
		endpointMap.put(endpointId2, endpoint2);
		
		when(application.getVideoServiceEndpoints()).thenReturn(endpointMap);
		
		when(restServiceSpy.getApplication()).thenReturn(application);
		
		List<SocialEndpointCredentials> list = restServiceSpy.getSocialEndpoints(0, 100);
		assertEquals(2, list.size());
	}
	
	@Test
	public void testCheckDeviceAuthStatus() {
		BroadcastRestService restServiceSpy = Mockito.spy(restServiceReal);
		
		AntMediaApplicationAdapter application = mock(AntMediaApplicationAdapter.class);
		Map<String, VideoServiceEndpoint> endpointMap = new HashMap<>();
		
		PeriscopeEndpoint endpoint = mock(PeriscopeEndpoint.class);
		String endpointId =  RandomStringUtils.randomAlphabetic(5);
		endpointMap.put(endpointId, endpoint);
		DeviceAuthParameters auth = new DeviceAuthParameters();
		String userCode = RandomStringUtils.randomAlphabetic(14);
		auth.user_code = userCode;
		
		SocialEndpointCredentials credentials = getSocialEndpointCrendential();
		credentials.setId(endpointId);
		when(endpoint.getCredentials()).thenReturn(credentials);
		when(endpoint.getAuthParameters()).thenReturn(auth);
		when(application.getVideoServiceEndpoints()).thenReturn(endpointMap);
		
		when(restServiceSpy.getApplication()).thenReturn(application);
		
		Result checkDeviceAuthStatus = restServiceSpy.checkDeviceAuthStatus(userCode);
		assertTrue(checkDeviceAuthStatus.isSuccess());
		
		
		checkDeviceAuthStatus = restServiceSpy.checkDeviceAuthStatus(userCode + "spoiler");
		assertFalse(checkDeviceAuthStatus.isSuccess());
		
		List<VideoServiceEndpoint> endpointErrorList = new ArrayList<>();
		PeriscopeEndpoint endpointError = mock(PeriscopeEndpoint.class);
		
		DeviceAuthParameters auth2 = new DeviceAuthParameters();
		userCode = RandomStringUtils.randomAlphabetic(14);
		auth2.user_code = userCode;
		
		when(endpointError.getAuthParameters()).thenReturn(auth2);
		
		endpointErrorList.add(endpointError);
		when(application.getVideoServiceEndpointsHavingError()).thenReturn(endpointErrorList);
		checkDeviceAuthStatus = restServiceSpy.checkDeviceAuthStatus(userCode);
		assertFalse(checkDeviceAuthStatus.isSuccess());
		
		assertEquals(0, application.getVideoServiceEndpointsHavingError().size());
		
	}
	
	
	@Test
	public void testGetInteractionsFromEndpoint() {
		BroadcastRestService restServiceSpy = Mockito.spy(restServiceReal);
		
		AntMediaApplicationAdapter application = mock(AntMediaApplicationAdapter.class);
		
		SocialEndpointCredentials credentials = getSocialEndpointCrendential();
		credentials.setId(String.valueOf((int)(Math.random() * 1000)));

		PeriscopeEndpoint endpoint = mock(PeriscopeEndpoint.class);
		when(application.getVideoServiceEndPoint(credentials.getId())).thenReturn(endpoint);
		
		when(restServiceSpy.getApplication()).thenReturn(application);
		
		String streamId =  "name"   + (int)(Math.random() * 1000);
		ArrayList<LiveComment> liveCommentList = new ArrayList<>();
		liveCommentList.add(new LiveComment("id", "message", new User(), ResourceOrigin.PERISCOPE, System.currentTimeMillis()));
		when(endpoint.getComments(streamId, 0, 10)).thenReturn(liveCommentList);
		
		when(endpoint.getTotalCommentsCount(streamId)).thenReturn(124);
		
		Result liveCommentsCount = restServiceSpy.getLiveCommentsCount(credentials.getId(), streamId);
		assertEquals(124, Integer.valueOf(liveCommentsCount.getMessage()).intValue());
		
		liveCommentsCount = restServiceSpy.getLiveCommentsCount(credentials.getId(), streamId + "dd");
		assertEquals(0, Integer.valueOf(liveCommentsCount.getMessage()).intValue());
		
		liveCommentsCount = restServiceSpy.getLiveCommentsCount(credentials.getId() + "spoiler", streamId);
		assertEquals(0, Integer.valueOf(liveCommentsCount.getMessage()).intValue());
		
		List<LiveComment> liveComments = restServiceSpy.getLiveCommentsFromEndpoint(credentials.getId(), streamId, 0, 10);
		assertEquals(1, liveComments.size());
		
		Result viewerCountFromEndpoint = restServiceSpy.getViewerCountFromEndpoint(credentials.getId(), streamId + "spoiler");
		assertEquals(0, Integer.valueOf(viewerCountFromEndpoint.getMessage()).intValue());
		
		
		when(endpoint.getLiveViews(streamId)).thenReturn((long) 234);
		viewerCountFromEndpoint = restServiceSpy.getViewerCountFromEndpoint(credentials.getId(), streamId);
		assertEquals(234, Integer.valueOf(viewerCountFromEndpoint.getMessage()).intValue());
		
		Interaction interaction = new Interaction();
		interaction.setAngryCount(23);
		interaction.setLikeCount(33);
		when(endpoint.getInteraction(streamId)).thenReturn(interaction);
		Interaction interactionFromEndpoint = restServiceSpy.getInteractionFromEndpoint(credentials.getId(), streamId);
		assertEquals(23, interactionFromEndpoint.getAngryCount());
		assertEquals(33, interactionFromEndpoint.getLikeCount());
		
	}

	public SocialEndpointCredentials getSocialEndpointCrendential() {
		String name = "name"  + (int)(Math.random() * 1000);
		String serviceName = "serviceName" + (int)(Math.random() * 1000);
		String authTimeInMillisecoonds = "authTimeInMillisecoonds" + (int)(Math.random() * 1000);
		String expireTimeInSeconds = "expireTimeInSeconds" + (int)(Math.random() * 1000);
		String tokenType = "tokenType" + (int)(Math.random() * 1000);
		String accessToken = "accessToken" + (int)(Math.random() * 1000);
		String refreshToken = "refreshToken" + (int)(Math.random() * 1000);
		
		SocialEndpointCredentials credentials = new SocialEndpointCredentials(name, serviceName, authTimeInMillisecoonds, expireTimeInSeconds, tokenType, accessToken, refreshToken);
		return credentials;
	}

	@Test
	public void testDeleteBroadcast() {
		AppSettings settings = mock(AppSettings.class);
		String serverName = "fully.qualified.domain.name";
		when(settings.getServerName()).thenReturn(serverName);
		restServiceReal.setAppSettings(settings);


		IDataStore store = new InMemoryDataStore("testdb");
		restServiceReal.setDataStore(store);

		Scope scope = mock(Scope.class);
		String scopeName = "scope";
		when(scope.getName()).thenReturn(scopeName);
		restServiceReal.setScope(scope);

		AntMediaApplicationAdapter appAdaptor = mock(AntMediaApplicationAdapter.class);
		IClientBroadcastStream broadcastStream = mock(IClientBroadcastStream.class);
		IStreamCapableConnection streamCapableConnection = mock(IStreamCapableConnection.class);

		when(broadcastStream.getConnection()).thenReturn(streamCapableConnection);
		when(appAdaptor.getBroadcastStream(Mockito.any(Scope.class), Mockito.any(String.class))).thenReturn(broadcastStream);

		restServiceReal.setApplication(appAdaptor);

		int streamCount = 15; 
		for (int i = 0; i < streamCount; i++) {
			Broadcast broadcast = new Broadcast(null, "name");
			Broadcast broadcastCreated = restServiceReal.createBroadcast(broadcast);
			assertNotNull(broadcastCreated.getStreamId());

			Broadcast broadcast2 = restServiceReal.getBroadcast(broadcastCreated.getStreamId());
			assertNotNull(broadcast2.getStreamId());
		}

		List<Broadcast> broadcastList = restServiceReal.getBroadcastList(0, 20);
		assertEquals(streamCount, broadcastList.size());

		for (Broadcast item: broadcastList) {
			Result result = restServiceReal.deleteBroadcast(item.getStreamId());
			assertTrue(result.isSuccess());
		}

		Mockito.verify(streamCapableConnection, Mockito.times(streamCount)).close();


	}

	
	@Test
	public void testUploadVodFile() {
		
		String fileName = RandomStringUtils.randomAlphabetic(11) + ".mp4"; 
		FileInputStream inputStream;
		try {
			inputStream = new FileInputStream("src/test/resources/sample_MP4_480.mp4");
			
			Scope scope = mock(Scope.class);
			String scopeName = "scope";
			when(scope.getName()).thenReturn(scopeName);
			
			
			File f = new File("webapps/scope/streams");
			MuxingTest.delete(f);
			
			restServiceReal.setScope(scope);
			
			IDataStore store = new InMemoryDataStore("testdb");
			restServiceReal.setDataStore(store);
			
			assertNull(f.list());
			
			assertEquals(0, store.getTotalVodNumber());
			
			restServiceReal.uploadVoDFile(fileName, inputStream);
			
			
			assertTrue(f.isDirectory());
			
			assertEquals(1, f.list().length);
			
			assertEquals(1, store.getTotalVodNumber());
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		
	}


	@Test
	public void testGetVersion() {
		Version version = restServiceReal.getVersion();
		assertEquals(version.getVersionName(), AntMediaApplicationAdapter.class.getPackage().getImplementationVersion());
		assertEquals(BroadcastRestService.COMMUNITY_EDITION, version.getVersionType());
	}

	@Test
	public void testServerNameAndRtmpURL() {
		AppSettings settings = mock(AppSettings.class);
		String serverName = "fully.qualified.domain.name";
		when(settings.getServerName()).thenReturn(serverName);

		Scope scope = mock(Scope.class);
		String scopeName = "scope";
		when(scope.getName()).thenReturn(scopeName);

		restServiceReal.setScope(scope);
		restServiceReal.setAppSettings(settings);

		Broadcast broadcast = new Broadcast(null, "name");
		IDataStore store = new InMemoryDataStore("testdb");
		restServiceReal.setDataStore(store);
		Broadcast createBroadcast = restServiceReal.createBroadcast(broadcast);

		assertEquals("rtmp://" + serverName + "/" + scopeName + "/" + broadcast.getStreamId() , createBroadcast.getRtmpURL());

		when(settings.getServerName()).thenReturn(null);

		Broadcast createBroadcast2 = restServiceReal.createBroadcast(broadcast);

		try {
			assertEquals("rtmp://" + InetAddress.getLocalHost().getHostAddress() + "/" + scopeName + "/" + broadcast.getStreamId() , createBroadcast2.getRtmpURL());
		} catch (UnknownHostException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		when(settings.getServerName()).thenReturn("");

		Broadcast createBroadcast3 = restServiceReal.createBroadcast(broadcast);

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
		IDataStore store = new InMemoryDataStore("testdb");
		restServiceReal.setDataStore(store);

		Broadcast createdBroadcast = restServiceReal.createBroadcast(broadcast);
		assertNotNull(createdBroadcast.getStreamId());
		assertNotEquals(createdBroadcast.getStreamId(), streamId);

		assertFalse(createdBroadcast.isZombi());

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
		IDataStore store = new InMemoryDataStore("testdb");
		restServiceReal.setDataStore(store);
		Broadcast createBroadcast = restServiceReal.createBroadcast(broadcast);

		assertNotNull(createBroadcast);
		assertNotNull(createBroadcast.getStreamId());
		assertNotNull(createBroadcast.getName());
		assertNotNull(createBroadcast.getStatus());
		assertNull(createBroadcast.getListenerHookURL());

		Broadcast createBroadcast2 = restServiceReal.createBroadcast(null);

		assertNotNull(createBroadcast2);
		assertNotNull(createBroadcast2.getStreamId());
		assertNull(createBroadcast2.getName());
		assertNotNull(createBroadcast2.getStatus());
		assertNull(createBroadcast2.getListenerHookURL());



		Gson gson = new Gson();

		Broadcast broadcastTmp = restServiceReal.getBroadcast(createBroadcast.getStreamId());
		assertNotNull(broadcastTmp);
		assertEquals(createBroadcast.getStatus(), broadcastTmp.getStatus());
		assertNull(broadcastTmp.getListenerHookURL());

		//update status
		boolean updateStatus = store.updateStatus(createBroadcast.getStreamId(), AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING);
		assertTrue(updateStatus);

		//check status
		broadcastTmp = restServiceReal.getBroadcast(createBroadcast.getStreamId());
		assertNotNull(broadcastTmp);
		assertEquals(AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING, broadcastTmp.getStatus());
		assertNull(broadcastTmp.getListenerHookURL());

		//update status again
		updateStatus = store.updateStatus(createBroadcast.getStreamId(), AntMediaApplicationAdapter.BROADCAST_STATUS_FINISHED);
		assertTrue(updateStatus);

		//check status
		broadcastTmp = restServiceReal.getBroadcast(createBroadcast.getStreamId());
		assertNotNull(broadcastTmp);
		assertEquals( AntMediaApplicationAdapter.BROADCAST_STATUS_FINISHED, broadcastTmp.getStatus());
		assertNull(broadcastTmp.getListenerHookURL());


		broadcastTmp = restServiceReal.getBroadcast("jdkdkdkdk");
		assertNotNull(broadcastTmp);
		assertNull(broadcastTmp.getStatus());


		broadcastTmp = restServiceReal.getBroadcast(createBroadcast.getStreamId());
		assertNotNull(broadcastTmp);
		assertEquals(AntMediaApplicationAdapter.BROADCAST_STATUS_FINISHED, broadcastTmp.getStatus());
		assertEquals(broadcastTmp.getStreamId(), createBroadcast.getStreamId());
		assertEquals(broadcastTmp.getName(), createBroadcast.getName());

	}
	
	@Test
	public void testTokenOperations() {
		
		IDataStore store = new InMemoryDataStore("testdb");
		restServiceReal.setDataStore(store);
		
		//create token
		Token testToken = restServiceReal.getToken("1234", 15764264, Token.PLAY_TOKEN);
		
		assertNotNull(testToken.getTokenId());
		
		//get tokens of stream
		List <Token> tokens = restServiceReal.listTokens(testToken.getStreamId(), 0, 10);
		
		assertEquals(1, tokens.size());
		
		//revoke tokens
		restServiceReal.revokeTokens(testToken.getStreamId());
		
		//get tokens of stream
		tokens = restServiceReal.listTokens(testToken.getStreamId(), 0, 10);
		
		//it should be zero because all tokens are revoked
		assertEquals(0, tokens.size());
		
		//create token again
		testToken = restServiceReal.getToken("1234", 15764264, Token.PLAY_TOKEN);
		
		//validate token
		Token validatedToken = restServiceReal.validateToken(testToken);
		
		//token should be validated and returned
		assertNotNull(validatedToken);
		
		//this should be false, because validated token is deleted after consumed
		Token expiredToken = restServiceReal.validateToken(testToken);
		
		assertNull(expiredToken);
		
		
		
	}
	
	@Test
	public void testObjectDetectionOperations() {
		
		IDataStore store = new InMemoryDataStore("testdb");
		restServiceReal.setDataStore(store);
		
		String streamId = "object_streamId";
		
		List<TensorFlowObject> detectedObjects = new ArrayList<>();
		
		//create detection object
		
		TensorFlowObject object = new TensorFlowObject("objectName", 92, "imageId");
		
		//add to list
		
		detectedObjects.add(object);
		
		restServiceReal.getDataStore().saveDetection(streamId, 0, detectedObjects);
		
		//get objects
		
		List<TensorFlowObject> objects = restServiceReal.getDetectedObjects(streamId);
		
		assertEquals(1, objects.size());		
		
		//get list of requested id
		
		List<TensorFlowObject> objectList = restServiceReal.getDetectionList(streamId, 0, 50);
		
		assertEquals(1, objectList.size());
		
		//get total number of saved detection list
		
		Long total = restServiceReal.getObjectDetectedTotal(streamId);
		
		assertEquals(1, (int)(long)total);
		
		
		
	}


}
