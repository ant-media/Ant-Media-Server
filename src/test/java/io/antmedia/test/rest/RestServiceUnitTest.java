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
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

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
import io.antmedia.datastore.db.types.Token;
import io.antmedia.datastore.db.types.VoD;
import io.antmedia.rest.BroadcastRestService;
import io.antmedia.rest.BroadcastRestService.BroadcastStatistics;
import io.antmedia.rest.BroadcastRestService.ProcessBuilderFactory;
import io.antmedia.rest.model.Result;
import io.antmedia.rest.model.Version;
import io.antmedia.social.endpoint.VideoServiceEndpoint;


@ContextConfiguration(locations = { "test.xml" })
public class RestServiceUnitTest {


	private BroadcastRestService restService = null;
	public AntMediaApplicationAdapter app = null;

	static {
		System.setProperty("red5.deployment.type", "junit");
		System.setProperty("red5.root", ".");
	}


	@Before
	public void before() {
		restService = new BroadcastRestService();
	}

	@After
	public void after() {
		restService = null;
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

		restService.setScope(scope);

		restService.setAppSettings(settings);

		Broadcast broadcast = new Broadcast(null, "name");
		IDataStore store = new InMemoryDataStore("testdb");
		restService.setDataStore(store);

		Process process = mock(Process.class);
		try {
			when(process.waitFor()).thenReturn(0);


			ProcessBuilderFactory factory = new ProcessBuilderFactory() {
				@Override
				public Process make(String... args) {
					return process;
				}
			};
			restService.setProcessBuilderFactory(factory);

			Broadcast createBroadcast = restService.createBroadcast(broadcast);
			assertNotNull(createBroadcast.getStreamId());

			Result result = restService.importLiveStreams2Stalker();
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

		restService.setScope(scope);

		restService.setAppSettings(settings);

		//Vod vod = new Vod();
		File file = new File(vodFolder, "test_file");
		String vodId = RandomStringUtils.randomNumeric(24);
		VoD newVod = new VoD("vodFile", "vodFile", file.getPath(), file.getName(), System.currentTimeMillis(), 0, 6000,
				VoD.USER_VOD,vodId);
		IDataStore store = new InMemoryDataStore("testdb");
		restService.setDataStore(store);

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
			restService.setProcessBuilderFactory(factory);

			Result result = restService.importVoDsToStalker();

			assertFalse(result.isSuccess());

			when(settings.getVodFolder()).thenReturn(vodFolderPath);

			result = restService.importVoDsToStalker();

			assertTrue(result.isSuccess());

		}  catch (InterruptedException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}
	
	@Test
	public void testDeleteVoD() {
		InMemoryDataStore datastore = new InMemoryDataStore("datastore");
		restService.setDataStore(datastore);
		
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
		
		restService.setAppCtx(context);
		
		VoD voD = restService.getVoD(vodId);
		assertEquals(vodId, voD.getVodId());
		assertEquals(streamVod.getStreamId(), voD.getStreamId());
		assertEquals(streamVod.getVodName(), voD.getVodName());
		assertEquals(streamVod.getFilePath(), voD.getFilePath());
		
		assertEquals(1, restService.getVodList(0, 50).size());
		
		restService.deleteVoD(vodId);
		
		assertEquals(0, restService.getVodList(0, 50).size());
		
		assertNull(datastore.getVoD(vodId));
		
	}


	@Test
	public void testBugBroadcastStatisticNull() {
		Scope scope = mock(Scope.class);
		String scopeName = "scope";
		when(scope.getName()).thenReturn(scopeName);
		
		AntMediaApplicationAdapter app = new AntMediaApplicationAdapter();
	
		ApplicationContext context = mock(ApplicationContext.class);
		
		restService.setAppCtx(context);
		restService.setApplication(app);
		restService.setScope(scope);
		restService.setDataStore(new InMemoryDataStore("testdb"));
		
		
		BroadcastStatistics broadcastStatistics = restService.getBroadcastStatistics(null);
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
		restService.setAppCtx(context);
		
		InMemoryDataStore dataStore = new InMemoryDataStore("testdb");
		restService.setDataStore(dataStore);
		Broadcast broadcast = new Broadcast();
		String streamId = dataStore.save(broadcast);
		
		dataStore.updateHLSViewerCount(streamId, 30);
		BroadcastStatistics broadcastStatistics = restService.getBroadcastStatistics(streamId);
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


		restService.setApplication(app);
		restService.setScope(scope);
		restService.setDataStore(new InMemoryDataStore("testdb"));

		restService.setAppSettings(settings);

		//get device auth parameters for facebook
		Result object = (Result)restService.getDeviceAuthParameters("facebook");

		// it should be facebook is not defined in this scope
		assertFalse(object.isSuccess());
		assertEquals(BroadcastRestService.ERROR_SOCIAL_ENDPOINT_UNDEFINED_ENDPOINT, object.getErrorId());

		//make client id and client secret has value for facebook
		when(settings.getFacebookClientId()).thenReturn("12313");
		when(settings.getFacebookClientSecret()).thenReturn("sdfsfsf");

		// get device auth parameter for facebook
		object = (Result)restService.getDeviceAuthParameters("facebook");

		//it should be again facebook is not defined in this scope
		assertFalse(object.isSuccess());
		assertEquals(BroadcastRestService.ERROR_SOCIAL_ENDPOINT_UNDEFINED_ENDPOINT, object.getErrorId());

		//make the same test for youtube and expect same results
		when(settings.getYoutubeClientId()).thenReturn(null);
		when(settings.getYoutubeClientSecret()).thenReturn(null);
		object = (Result)restService.getDeviceAuthParameters("youtube");

		assertFalse(object.isSuccess());
		assertEquals(BroadcastRestService.ERROR_SOCIAL_ENDPOINT_UNDEFINED_ENDPOINT, object.getErrorId());

		when(settings.getYoutubeClientId()).thenReturn("121212");
		when(settings.getYoutubeClientSecret()).thenReturn("1212121");

		object = (Result)restService.getDeviceAuthParameters("youtube");

		assertFalse(object.isSuccess());
		assertEquals(BroadcastRestService.ERROR_SOCIAL_ENDPOINT_UNDEFINED_ENDPOINT, object.getErrorId());

		//make client id and clien secret null for periscope
		when(settings.getPeriscopeClientId()).thenReturn(null);
		when(settings.getPeriscopeClientSecret()).thenReturn(null);

		//get device auth parameter for periscope
		object = (Result)restService.getDeviceAuthParameters("periscope");

		//it should be client id and client secret missing
		assertFalse(object.isSuccess());
		assertEquals(BroadcastRestService.ERROR_SOCIAL_ENDPOINT_UNDEFINED_CLIENT_ID, object.getErrorId());

		//make client id and client secret have value for periscope
		when(settings.getPeriscopeClientId()).thenReturn("121212");
		when(settings.getPeriscopeClientSecret()).thenReturn("121212");

		//it should be different error because client id and cleint secret is not correct
		object  = (Result) restService.getDeviceAuthParameters("periscope");
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

		restService.setScope(scope);

		restService.setAppSettings(settings);

		Broadcast broadcast = new Broadcast(null, "name");
		IDataStore store = new InMemoryDataStore("testdb");
		restService.setDataStore(store);
		Broadcast createBroadcast = restService.createBroadcast(broadcast);

		assertEquals(hookURL, createBroadcast.getListenerHookURL());

		Broadcast broadcastTmp = restService.getBroadcast(createBroadcast.getStreamId());

		assertEquals(hookURL, broadcastTmp.getListenerHookURL());

	}


	@Test
	public void testAddEndpoint() {
		AppSettings settings = mock(AppSettings.class);
		String serverName = "fully.qualified.domain.name";
		when(settings.getServerName()).thenReturn(serverName);
		restService.setAppSettings(settings);

		Broadcast broadcast = new Broadcast(null, "name");
		IDataStore store = new InMemoryDataStore("testdb");
		restService.setDataStore(store);

		Scope scope = mock(Scope.class);
		String scopeName = "scope";
		when(scope.getName()).thenReturn(scopeName);
		restService.setScope(scope);

		Broadcast createBroadcast = restService.createBroadcast(broadcast);
		String streamId = createBroadcast.getStreamId();
		assertNotNull(streamId);

		String endpointURL = "rtmp://test.endpoint.url/test";
		Result result = restService.addEndpoint(streamId, endpointURL);
		assertTrue(result.isSuccess());

		Broadcast broadcast2 = restService.getBroadcast(streamId);
		assertEquals(broadcast.getStreamId(), broadcast2.getStreamId());

		assertEquals(1, broadcast2.getEndPointList().size());
		Endpoint endpoint = broadcast2.getEndPointList().get(0);
		assertEquals(endpointURL, endpoint.rtmpUrl);
		assertEquals("generic", endpoint.type);
	}

	

	@Test
	public void testAddSocialEndpoint() {
		AppSettings settings = mock(AppSettings.class);
		String serverName = "fully.qualified.domain.name";
		when(settings.getServerName()).thenReturn(serverName);
		restService.setAppSettings(settings);

		Broadcast broadcast = new Broadcast(null, "name");
		IDataStore store = new InMemoryDataStore("testdb");
		restService.setDataStore(store);

		Scope scope = mock(Scope.class);
		String scopeName = "scope";
		when(scope.getName()).thenReturn(scopeName);
		restService.setScope(scope);

		AntMediaApplicationAdapter appAdaptor = mock(AntMediaApplicationAdapter.class);

		restService.setApplication(appAdaptor);

		Broadcast broadcastCreated = restService.createBroadcast(broadcast);

		Result result = restService.addSocialEndpoint(broadcastCreated.getStreamId(), "not_exist");
		assertFalse(result.isSuccess());

		when(appAdaptor.getVideoServiceEndpoints()).thenReturn(null);
		result = restService.addSocialEndpoint(broadcastCreated.getStreamId(), "not_exist");
		assertFalse(result.isSuccess());

		result = restService.addSocialEndpoint("not_exist", "not exist");
		assertFalse(result.isSuccess());


		ArrayList<VideoServiceEndpoint> endpointList = new ArrayList<>();
		VideoServiceEndpoint videoServiceEndpoint = mock(VideoServiceEndpoint.class);
		String endpointServiceId = "mock_endpoint";
		SocialEndpointCredentials credentials = new SocialEndpointCredentials();
		credentials.setId(endpointServiceId);
		when(videoServiceEndpoint.getCredentials()).thenReturn(credentials);
		endpointList.add(videoServiceEndpoint);

		when(appAdaptor.getVideoServiceEndpoints()).thenReturn(endpointList);

		try {

			String broadcastId = "broadcastId"  + (int)(Math.random() * 10000);
			String streamId = "streamId"  + (int)(Math.random() * 10000);
			String name = "broadcastId"  + (int)(Math.random() * 10000);
			String rtmpUrl = "rtmpUrl"  + (int)(Math.random() * 10000);
			String type = "type"  + (int)(Math.random() * 10000);

			when(videoServiceEndpoint.createBroadcast(broadcastCreated.getName(), broadcastCreated.getDescription(),
					broadcastCreated.isIs360(), broadcastCreated.isPublicStream(), 720, true))
			.thenReturn(new Endpoint(broadcastId, streamId, name, rtmpUrl, type, endpointServiceId));

			result = restService.addSocialEndpoint(broadcastCreated.getStreamId(), endpointServiceId);
			assertTrue(result.isSuccess());

			Mockito.verify(videoServiceEndpoint).createBroadcast(broadcastCreated.getName(), broadcastCreated.getDescription(),
					broadcastCreated.isIs360(), broadcastCreated.isPublicStream(), 720, true);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		result = restService.revokeSocialNetwork(endpointServiceId);
		assertTrue(result.isSuccess());

		Mockito.verify(videoServiceEndpoint).resetCredentials();
		assertTrue(endpointList.isEmpty());

		result = restService.revokeSocialNetwork("not_exist");
		assertFalse(result.isSuccess());

		when(appAdaptor.getVideoServiceEndpoints()).thenReturn(null);
		result = restService.revokeSocialNetwork("not_exist");
		assertFalse(result.isSuccess());

	}

	@Test
	public void testDeleteBroadcast() {
		AppSettings settings = mock(AppSettings.class);
		String serverName = "fully.qualified.domain.name";
		when(settings.getServerName()).thenReturn(serverName);
		restService.setAppSettings(settings);


		IDataStore store = new InMemoryDataStore("testdb");
		restService.setDataStore(store);

		Scope scope = mock(Scope.class);
		String scopeName = "scope";
		when(scope.getName()).thenReturn(scopeName);
		restService.setScope(scope);

		AntMediaApplicationAdapter appAdaptor = mock(AntMediaApplicationAdapter.class);
		IClientBroadcastStream broadcastStream = mock(IClientBroadcastStream.class);
		IStreamCapableConnection streamCapableConnection = mock(IStreamCapableConnection.class);

		when(broadcastStream.getConnection()).thenReturn(streamCapableConnection);
		when(appAdaptor.getBroadcastStream(Mockito.any(Scope.class), Mockito.any(String.class))).thenReturn(broadcastStream);

		restService.setApplication(appAdaptor);

		int streamCount = 15; 
		for (int i = 0; i < streamCount; i++) {
			Broadcast broadcast = new Broadcast(null, "name");
			Broadcast broadcastCreated = restService.createBroadcast(broadcast);
			assertNotNull(broadcastCreated.getStreamId());

			Broadcast broadcast2 = restService.getBroadcast(broadcastCreated.getStreamId());
			assertNotNull(broadcast2.getStreamId());
		}

		List<Broadcast> broadcastList = restService.getBroadcastList(0, 20);
		assertEquals(streamCount, broadcastList.size());

		for (Broadcast item: broadcastList) {
			Result result = restService.deleteBroadcast(item.getStreamId());
			assertTrue(result.isSuccess());
		}

		Mockito.verify(streamCapableConnection, Mockito.times(streamCount)).close();


	}



	@Test
	public void testGetVersion() {
		Version version = restService.getVersion();
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

		restService.setScope(scope);
		restService.setAppSettings(settings);

		Broadcast broadcast = new Broadcast(null, "name");
		IDataStore store = new InMemoryDataStore("testdb");
		restService.setDataStore(store);
		Broadcast createBroadcast = restService.createBroadcast(broadcast);

		assertEquals("rtmp://" + serverName + "/" + scopeName + "/" + broadcast.getStreamId() , createBroadcast.getRtmpURL());

		when(settings.getServerName()).thenReturn(null);

		Broadcast createBroadcast2 = restService.createBroadcast(broadcast);

		try {
			assertEquals("rtmp://" + InetAddress.getLocalHost().getHostAddress() + "/" + scopeName + "/" + broadcast.getStreamId() , createBroadcast2.getRtmpURL());
		} catch (UnknownHostException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		when(settings.getServerName()).thenReturn("");

		Broadcast createBroadcast3 = restService.createBroadcast(broadcast);

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
		restService.setAppSettings(settings);

		Scope scope = mock(Scope.class);
		String scopeName = "scope";
		when(scope.getName()).thenReturn(scopeName);
		restService.setScope(scope);

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
		restService.setDataStore(store);

		Broadcast createdBroadcast = restService.createBroadcast(broadcast);
		assertNotNull(createdBroadcast.getStreamId());
		assertNotEquals(createdBroadcast.getStreamId(), streamId);

		assertFalse(createdBroadcast.isZombi());

	}

	@Test
	public void testAllInOne() {
		AppSettings settings = mock(AppSettings.class);
		when(settings.getListenerHookURL()).thenReturn(null);
		restService.setAppSettings(settings);

		Scope scope = mock(Scope.class);
		String scopeName = "scope";
		when(scope.getName()).thenReturn(scopeName);

		restService.setScope(scope);


		Broadcast broadcast = new Broadcast(null, "name");
		IDataStore store = new InMemoryDataStore("testdb");
		restService.setDataStore(store);
		Broadcast createBroadcast = restService.createBroadcast(broadcast);

		assertNotNull(createBroadcast);
		assertNotNull(createBroadcast.getStreamId());
		assertNotNull(createBroadcast.getName());
		assertNotNull(createBroadcast.getStatus());
		assertNull(createBroadcast.getListenerHookURL());

		Broadcast createBroadcast2 = restService.createBroadcast(null);

		assertNotNull(createBroadcast2);
		assertNotNull(createBroadcast2.getStreamId());
		assertNull(createBroadcast2.getName());
		assertNotNull(createBroadcast2.getStatus());
		assertNull(createBroadcast2.getListenerHookURL());



		Gson gson = new Gson();

		Broadcast broadcastTmp = restService.getBroadcast(createBroadcast.getStreamId());
		assertNotNull(broadcastTmp);
		assertEquals(createBroadcast.getStatus(), broadcastTmp.getStatus());
		assertNull(broadcastTmp.getListenerHookURL());

		//update status
		boolean updateStatus = store.updateStatus(createBroadcast.getStreamId(), AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING);
		assertTrue(updateStatus);

		//check status
		broadcastTmp = restService.getBroadcast(createBroadcast.getStreamId());
		assertNotNull(broadcastTmp);
		assertEquals(AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING, broadcastTmp.getStatus());
		assertNull(broadcastTmp.getListenerHookURL());

		//update status again
		updateStatus = store.updateStatus(createBroadcast.getStreamId(), AntMediaApplicationAdapter.BROADCAST_STATUS_FINISHED);
		assertTrue(updateStatus);

		//check status
		broadcastTmp = restService.getBroadcast(createBroadcast.getStreamId());
		assertNotNull(broadcastTmp);
		assertEquals( AntMediaApplicationAdapter.BROADCAST_STATUS_FINISHED, broadcastTmp.getStatus());
		assertNull(broadcastTmp.getListenerHookURL());


		broadcastTmp = restService.getBroadcast("jdkdkdkdk");
		assertNotNull(broadcastTmp);
		assertNull(broadcastTmp.getStatus());


		broadcastTmp = restService.getBroadcast(createBroadcast.getStreamId());
		assertNotNull(broadcastTmp);
		assertEquals(AntMediaApplicationAdapter.BROADCAST_STATUS_FINISHED, broadcastTmp.getStatus());
		assertEquals(broadcastTmp.getStreamId(), createBroadcast.getStreamId());
		assertEquals(broadcastTmp.getName(), createBroadcast.getName());

	}
	
	@Test
	public void testTokenOperations() {
		
		IDataStore store = new InMemoryDataStore("testdb");
		restService.setDataStore(store);
		
		//create token
		Token testToken = restService.getToken("1234", 15764264);
		
		assertNotNull(testToken.getTokenId());
		
		//get tokens of stream
		List <Token> tokens = restService.listTokens(testToken.getStreamId(), 0, 10);
		
		assertEquals(1, tokens.size());
		
		//revoke tokens
		restService.revokeTokens(testToken.getStreamId());
		
		//get tokens of stream
		tokens = restService.listTokens(testToken.getStreamId(), 0, 10);
		
		//it should be zero because all tokens are revoked
		assertEquals(0, tokens.size());
		
		//create token again
		testToken = restService.getToken("1234", 15764264);
		
		//validate token
		Token validatedToken = restService.validateToken(testToken);
		
		//token should be validated and returned
		assertNotNull(validatedToken);
		
		//this should be false, because validated token is deleted after consumed
		Token expiredToken = restService.validateToken(testToken);
		
		assertNull(expiredToken);
		
		
		
	}


}
