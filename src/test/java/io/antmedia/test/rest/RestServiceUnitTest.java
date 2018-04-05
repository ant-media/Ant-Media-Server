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

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.red5.server.scope.Scope;

import com.google.gson.Gson;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.AppSettings;
import io.antmedia.datastore.db.IDataStore;
import io.antmedia.datastore.db.InMemoryDataStore;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.rest.BroadcastRestService;
import io.antmedia.rest.model.Result;

public class RestServiceUnitTest  {


	private BroadcastRestService restService = null;

	@Before
	public void before() {
		restService = new BroadcastRestService();
		
	}

	@After
	public void after() {
		restService = null;
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

}
