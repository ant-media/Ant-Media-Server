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

import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.red5.server.scheduling.QuartzSchedulingService;
import org.red5.server.scope.Scope;
import org.red5.server.scope.WebScope;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

import com.google.gson.Gson;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.AppSettings;
import io.antmedia.datastore.db.IDataStore;
import io.antmedia.datastore.db.InMemoryDataStore;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.datastore.db.types.Vod;
import io.antmedia.rest.BroadcastRestService;
import io.antmedia.rest.model.Result;


@ContextConfiguration(locations = { "test.xml" })
public class RestServiceUnitTest extends AbstractJUnit4SpringContextTests{


	private BroadcastRestService restService = null;
	private AntMediaApplicationAdapter appInstance;
	private WebScope appScope;
	public AntMediaApplicationAdapter app = null;
	
	static {
		System.setProperty("red5.deployment.type", "junit");
		System.setProperty("red5.root", ".");
	}


	@Before
	public void before() {
		restService = new BroadcastRestService();

		if (appScope == null) {
			appScope = (WebScope) applicationContext.getBean("web.scope");
			assertTrue(appScope.getDepth() == 1);
		}

		if (app == null) 
		{
			app = (AntMediaApplicationAdapter) applicationContext.getBean("web.handler");
			assertTrue(appScope.getDepth() == 1);
		}


	}

	@After
	public void after() {
		restService = null;
	}


	/**
	 * These tests should be run with stalker db
	 */
	//@Test
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
		Broadcast createBroadcast = restService.createBroadcast(broadcast);


		Result result = restService.importLiveStreams2Stalker();
		assertTrue(result.isSuccess());
	}

	/**
	 * These tests should be run with stalker db
	 */
	//@Test
	public void testImportVoD2Stalker() {
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

		//Vod vod = new Vod();
		File file = new File("test_file");
		Vod newVod = new Vod("vodFile", "vodFile", file.getPath(), file.getName(), System.currentTimeMillis(), 0, 6000,
				Vod.USER_VOD);
		IDataStore store = new InMemoryDataStore("testdb");
		restService.setDataStore(store);

		assertTrue(store.addUserVod(newVod));


		Result result = restService.importVoDsToStalker();

		assertTrue(result.isSuccess());

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
		assertEquals(broadcastTmp.getStatus(), AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING);
		assertNull(broadcastTmp.getListenerHookURL());

		//update status again
		updateStatus = store.updateStatus(createBroadcast.getStreamId(), AntMediaApplicationAdapter.BROADCAST_STATUS_FINISHED);
		assertTrue(updateStatus);

		//check status
		broadcastTmp = restService.getBroadcast(createBroadcast.getStreamId());
		assertNotNull(broadcastTmp);
		assertEquals(broadcastTmp.getStatus(), AntMediaApplicationAdapter.BROADCAST_STATUS_FINISHED);
		assertNull(broadcastTmp.getListenerHookURL());


		broadcastTmp = restService.getBroadcast("jdkdkdkdk");
		assertNotNull(broadcastTmp);
		assertNull(broadcastTmp.getStatus());


		broadcastTmp = restService.getBroadcast(createBroadcast.getStreamId());
		assertNotNull(broadcastTmp);
		assertEquals(broadcastTmp.getStatus(), AntMediaApplicationAdapter.BROADCAST_STATUS_FINISHED);
		assertEquals(broadcastTmp.getStreamId(), createBroadcast.getStreamId());
		assertEquals(broadcastTmp.getName(), createBroadcast.getName());

		Result deleteBroadcast = restService.deleteBroadcast(createBroadcast.getStreamId());
		assertTrue(deleteBroadcast.isSuccess());


		deleteBroadcast = restService.deleteBroadcast(createBroadcast.getStreamId());
		assertFalse(deleteBroadcast.isSuccess());

		deleteBroadcast = restService.deleteBroadcast(null);
		assertFalse(deleteBroadcast.isSuccess());

		try {
			createBroadcast.setStreamId(null);
			fail("it shoudl throw exception");
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		deleteBroadcast = restService.deleteBroadcast(null);
		assertFalse(deleteBroadcast.isSuccess());
	}


	public AntMediaApplicationAdapter getInstance() {
		if (appInstance == null) {
			appInstance = (AntMediaApplicationAdapter) applicationContext.getBean("web.handler");
		}
		return appInstance;
	}


}
