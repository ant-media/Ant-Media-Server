package io.antmedia.test.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.red5.server.api.IContext;
import org.red5.server.scope.Scope;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.AppSettings;
import io.antmedia.datastore.db.DataStore;
import io.antmedia.datastore.db.InMemoryDataStore;
import io.antmedia.datastore.db.MapDBStore;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.datastore.db.types.Broadcast.PlayListItem;
import io.antmedia.rest.BroadcastRestService;
import io.antmedia.rest.model.Result;
import io.antmedia.settings.ServerSettings;
import io.antmedia.statistic.IStatsCollector;
import io.antmedia.statistic.StatsCollector;
import io.vertx.core.Vertx;
import jakarta.ws.rs.core.Response.Status;

@ContextConfiguration(locations = { "test.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class PlaylistRestServiceV2UnitTest {

	BroadcastRestService restServiceReal = null;
	public AntMediaApplicationAdapter app = null;

	static {
		System.setProperty("red5.deployment.type", "junit");
		System.setProperty("red5.root", ".");
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


	@Test
	public void testGetPlaylist() {

		//create a broadcast
		PlayListItem broadcastItem1 = new PlayListItem();

		List<PlayListItem> playItemList = new ArrayList<>();

		playItemList.add(broadcastItem1);

		Broadcast playlistIdNull = null;

		Scope scope = mock(Scope.class);
		String scopeName = "scope";
		when(scope.getName()).thenReturn(scopeName);

		restServiceReal.setScope(scope);

		InMemoryDataStore dataStore = new InMemoryDataStore("testdb");
		restServiceReal.setDataStore(dataStore);

		String checkPlaylistIdNull = null;

		jakarta.ws.rs.core.Response response = restServiceReal.getBroadcast(checkPlaylistIdNull);

		// Check Get Playlist is null
		assertEquals(404, response.getStatus());

		//Create playlist

		Broadcast oldPlaylist = new Broadcast();
		oldPlaylist.setType(AntMediaApplicationAdapter.PLAY_LIST);

		String streamId = dataStore.save(oldPlaylist);

		assertNotNull(streamId);

		response = restServiceReal.getBroadcast(oldPlaylist.getStreamId());
		Broadcast returnedBroadcast = (Broadcast) response.getEntity();

		assertEquals(streamId, returnedBroadcast.getStreamId());
		
		assertEquals(AntMediaApplicationAdapter.PLAY_LIST, returnedBroadcast.getType());

	}



	@Test
	public void testCreatePlaylist() {

		Result result = new Result(false);

		//create a broadcast
		PlayListItem broadcastItem1 = new PlayListItem();

		//create a broadcast
		PlayListItem broadcastItem2 = new PlayListItem();

		List<PlayListItem> broadcastList = new ArrayList<>();

		broadcastList.add(broadcastItem1);
		broadcastList.add(broadcastItem2);

		Broadcast playlist = new Broadcast();
		playlist.setType(AntMediaApplicationAdapter.PLAY_LIST);
		playlist.setPlayListItemList(broadcastList);
		

		StatsCollector monitor = mock(StatsCollector.class);

		Scope scope = mock(Scope.class);
		String scopeName = "scope";
		when(scope.getName()).thenReturn(scopeName);

		restServiceReal.setScope(scope);

		ApplicationContext context = mock(ApplicationContext.class);

		InMemoryDataStore dataStore = new InMemoryDataStore("datastore");
		restServiceReal.setDataStore(dataStore);

		AppSettings settings = mock(AppSettings.class);
		when(settings.getListenerHookURL()).thenReturn(null);
		restServiceReal.setAppSettings(settings);

		restServiceReal.setAppCtx(context);

		AntMediaApplicationAdapter app = mock(AntMediaApplicationAdapter.class);
		when(app.getScope()).thenReturn(scope);


		ServerSettings serverSettings = Mockito.mock(ServerSettings.class);
		restServiceReal.setServerSettings(serverSettings);

		when(context.getBean(AntMediaApplicationAdapter.BEAN_NAME)).thenReturn(app);

		//when(restServiceSpy.getApplication()).thenReturn(adptr);

		Mockito.doReturn(dataStore).when(app).getDataStore();

		// Add playlist in DB

		dataStore.save(playlist);

		// Test already created playlist Id  

		jakarta.ws.rs.core.Response response = restServiceReal.createBroadcast(playlist, false);
		
		Mockito.verify(app, Mockito.never()).schedulePlayList(Mockito.anyLong(), Mockito.any());

		assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
		// Test already invalid created playlist Id  

		try {
			playlist.setStreamId(" asd asd _ ?/assa s");
	
			response = restServiceReal.createBroadcast(playlist, false);
	
			assertEquals(Status.BAD_REQUEST.getStatusCode() , response.getStatus());
	

			Broadcast playlist2 = new Broadcast();
			playlist2.setType(AntMediaApplicationAdapter.PLAY_LIST);
			playlist2.setPlayListItemList(broadcastList);
	
			when(monitor.enoughResource()).thenReturn(false);
			when(context.getBean(StatsCollector.BEAN_NAME)).thenReturn(monitor);
	
			response = restServiceReal.createBroadcast(playlist2, false);
			Mockito.verify(app).schedulePlayList(Mockito.anyLong(), Mockito.any());

			
			Broadcast broadcast = (Broadcast) response.getEntity();
	
			assertNotNull(playlist.getStreamId());
	
			assertEquals(false, result.isSuccess());
	
			// Test with null broadcast Item list and false
			playlist2.setStreamId("testWithNullBroadcastList");
			broadcastList.clear();
			playlist.setPlayListItemList(broadcastList);
	
			response = restServiceReal.createBroadcast(playlist2, false);
			broadcast = (Broadcast) response.getEntity();
	
			assertEquals(Status.OK.getStatusCode(), response.getStatus());
		
		}
		catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}



	}

	@Test
	public void testDeletePlaylist() {

		BroadcastRestService restServiceSpy = Mockito.spy(restServiceReal);

		AntMediaApplicationAdapter adptr = mock(AntMediaApplicationAdapter.class);

		Broadcast playlist = new Broadcast();
		playlist.setType(AntMediaApplicationAdapter.PLAY_LIST);
		Result result = new Result(false);		

		Scope scope = mock(Scope.class);
		String scopeName = "scope";
		when(scope.getName()).thenReturn(scopeName);
		IContext icontext = mock(IContext.class);
		when(icontext.getBean(AppSettings.BEAN_NAME)).thenReturn(new AppSettings());
		
		when(scope.getContext()).thenReturn(icontext);

		restServiceReal.setScope(scope);

		ApplicationContext context = mock(ApplicationContext.class);

		InMemoryDataStore dataStore = new InMemoryDataStore("testdb");
		restServiceReal.setDataStore(dataStore);

		restServiceReal.setAppCtx(context);

		AntMediaApplicationAdapter app = Mockito.spy(new AntMediaApplicationAdapter());
		when(app.getScope()).thenReturn(scope);
		app.setDataStore(dataStore);

		when(context.getBean(AntMediaApplicationAdapter.BEAN_NAME)).thenReturn(app);

		when(restServiceSpy.getApplication()).thenReturn(adptr);

		when(restServiceSpy.getApplication().stopStreaming(Mockito.any())).thenReturn(result);

		try {
			playlist.setStreamId("testPlaylistId");
		}
		catch (Exception e) 
		{
			e.printStackTrace();
			fail(e.getMessage());
		}

		//create a broadcast
		PlayListItem broadcastItem1 = new PlayListItem();

		List<PlayListItem> broadcastList = new ArrayList<>();

		broadcastList.add(broadcastItem1);

		playlist.setPlayListItemList(broadcastList);

		String streamId = dataStore.save(playlist);
		assertEquals("testPlaylistId", streamId);
		

		result = restServiceReal.deleteBroadcast(playlist.getStreamId());
		Mockito.verify(app).cancelPlaylistSchedule(playlist.getStreamId());

		assertEquals(true, result.isSuccess());

		//If there is no Playlist in DB.

		result = restServiceReal.deleteBroadcast("testDbNullPlaylistId");

		assertEquals(false, result.isSuccess());

		// If Playlist ID is null.

		result = restServiceReal.deleteBroadcast(null);

		assertEquals(false, result.isSuccess());


	}

	@Test
	public void testEditPlaylist() {

		Result result = new Result(false);

		//create a broadcast
		PlayListItem broadcastItem1 = new PlayListItem();

		//create a broadcast
		PlayListItem broadcastItem2 = new PlayListItem();

		List<PlayListItem> broadcastList = new ArrayList<>();

		broadcastList.add(broadcastItem1);
		broadcastList.add(broadcastItem2);

		Broadcast playlist = new Broadcast();
		playlist.setType(AntMediaApplicationAdapter.PLAY_LIST);
		playlist.setPlayListItemList(broadcastList);
		

		Scope scope = mock(Scope.class);
		String scopeName = "scope";
		when(scope.getName()).thenReturn(scopeName);

		restServiceReal.setScope(scope);

		ApplicationContext context = mock(ApplicationContext.class);

		DataStore dataStore = new MapDBStore("testdb", vertx);
		restServiceReal.setDataStore(dataStore);

		restServiceReal.setAppCtx(context);
		AntMediaApplicationAdapter app = Mockito.mock(AntMediaApplicationAdapter.class);
		restServiceReal.setApplication(app);
		
		try {
			playlist.setStreamId("testPlaylistId");
		}
		catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		dataStore.save(playlist);

		// getPlaylistId = null & playlistId = null
		
		playlist.setPlannedStartDate(100);
		playlist.setUpdateTime(System.currentTimeMillis());
		playlist.setStatus(AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING);
		
		assertTrue(restServiceReal.isStreaming(playlist));
		

		result = restServiceReal.updateBroadcast(playlist.getStreamId(), playlist);
		Mockito.verify(app).cancelPlaylistSchedule(playlist.getStreamId());
		Mockito.verify(app).schedulePlayList(Mockito.anyLong(), Mockito.any());
		//because we don't restart for playlist
		Mockito.verify(app, Mockito.never()).stopStreaming(Mockito.any());
		Mockito.verify(app, Mockito.never()).startStreaming(Mockito.any());
		
		

		assertEquals(true, result.isSuccess());

		// getPlaylistId = null & playlistId != null		

		result = restServiceReal.updateBroadcast("test123", playlist);

		assertEquals(false, result.isSuccess());

		// getPlaylistId != null & playlistId = null
		try {
			playlist.setStreamId("notNullId");
		}
		catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		result = restServiceReal.updateBroadcast(null, playlist);

		// getPlaylistId != null & playlistId != null
		try {
			playlist.setStreamId("afterTestPlaylistId");
		}
		catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		playlist.setName("afterTestPlaylistName");
		dataStore.save(playlist);
		
		result = restServiceReal.updateBroadcast(playlist.getStreamId(), playlist);

		assertEquals(true, result.isSuccess());

		assertEquals("afterTestPlaylistId", playlist.getStreamId());

		assertEquals("afterTestPlaylistName", playlist.getName());	

		// getPlaylistId != null & playlistId != null


		broadcastList.clear();

		playlist.setPlayListItemList(broadcastList);

		result = restServiceReal.updateBroadcast(playlist.getStreamId(), playlist);

		assertEquals(true, result.isSuccess());

		assertEquals("afterTestPlaylistId", playlist.getStreamId());

		assertEquals("afterTestPlaylistName", playlist.getName());	

	}


	
	@Test
	public void testStopPlaylist() {

		BroadcastRestService restServiceSpy = Mockito.spy(restServiceReal);

		Result result = new Result(true);

		//create a broadcast
		PlayListItem broadcastItem1 = new PlayListItem();

		//create a broadcast
		PlayListItem broadcastItem2 = new PlayListItem();

		List<PlayListItem> broadcastList = new ArrayList<>();

		broadcastList.add(broadcastItem1);
		broadcastList.add(broadcastItem2);

		Broadcast playlist = new Broadcast();
		playlist.setType(AntMediaApplicationAdapter.PLAY_LIST);
		playlist.setPlayListItemList(broadcastList);


		Scope scope = mock(Scope.class);
		String scopeName = "scope";
		when(scope.getName()).thenReturn(scopeName);
		IContext icontext = mock(IContext.class);
		when(icontext.getBean(AppSettings.BEAN_NAME)).thenReturn(new AppSettings());
		
		
		when(scope.getContext()).thenReturn(icontext);

		restServiceReal.setScope(scope);

		ApplicationContext context = mock(ApplicationContext.class);

		InMemoryDataStore dataStore = new InMemoryDataStore("testdb");
		restServiceReal.setDataStore(dataStore);

		restServiceReal.setAppCtx(context);

		AntMediaApplicationAdapter app = Mockito.spy(new AntMediaApplicationAdapter());
		app.setDataStore(dataStore);
		when(app.getScope()).thenReturn(scope);

		when(context.getBean(AntMediaApplicationAdapter.BEAN_NAME)).thenReturn(app);
		try {
			playlist.setStreamId("testPlaylistId");
		}
		catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}


		String streamId = dataStore.save(playlist);
		assertEquals("testPlaylistId", streamId);

		// Playlist current broadcast is empty scenario
		result = restServiceReal.stopStreamingV2(playlist.getStreamId());	

		assertEquals(false, result.isSuccess());

		// Playlist ID is null scenario

		result = restServiceReal.stopStreamingV2(null);		

		assertEquals(false, result.isSuccess());

		// Playlist is null scenario
		result = restServiceReal.stopStreamingV2("nullPlaylist");		
		assertEquals(false, result.isSuccess());

		// Playlist current Broadcast null scenario

		playlist.setCurrentPlayIndex(99);

		result = restServiceReal.stopStreamingV2(playlist.getStreamId());		

		assertEquals(false, result.isSuccess());

		// Playlist is stop normal scenario
		// Pllaylist current broadcast ID change back

		// Restore playlist default value
		playlist = dataStore.get(playlist.getStreamId());
		playlist.setCurrentPlayIndex(0);



		when(restServiceSpy.getApplication()).thenReturn(mock(AntMediaApplicationAdapter.class));
		when(restServiceSpy.getApplication().stopStreaming(Mockito.any())).thenReturn(result);
		result = restServiceReal.stopStreamingV2(playlist.getStreamId());	
		//it's created because it's not started
		assertEquals(AntMediaApplicationAdapter.BROADCAST_STATUS_CREATED, playlist.getStatus());

	}

	@Test
	public void testStartPlaylist() {

		Result result = new Result(false);

		//create a broadcast
		PlayListItem broadcastItem1 = new PlayListItem();

		//create a broadcast
		PlayListItem broadcastItem2 = new PlayListItem();

		List<PlayListItem> broadcastList = new ArrayList<>();

		broadcastList.add(broadcastItem1);
		broadcastList.add(broadcastItem2);

		Broadcast playlist = new Broadcast();
		
		playlist.setType(AntMediaApplicationAdapter.PLAY_LIST);
		try {
			playlist.setStreamId("playlistTestId");
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		playlist.setPlayListItemList(broadcastList);


		Scope scope = mock(Scope.class);
		String scopeName = "scope";
		when(scope.getName()).thenReturn(scopeName);
		
		IContext icontext = mock(IContext.class);
		when(icontext.getBean(AppSettings.BEAN_NAME)).thenReturn(new AppSettings());
		when(scope.getContext()).thenReturn(icontext);

		restServiceReal.setScope(scope);

		ApplicationContext context = mock(ApplicationContext.class);

		InMemoryDataStore dataStore = new InMemoryDataStore("testdb");
		restServiceReal.setDataStore(dataStore);

		restServiceReal.setAppCtx(context);

		AntMediaApplicationAdapter app = Mockito.spy(new AntMediaApplicationAdapter());
		
		when(app.getScope()).thenReturn(scope);
		app.setDataStore(dataStore);
		//init stream fetcher
		app.getStreamFetcherManager();

		when(context.getBean(AntMediaApplicationAdapter.BEAN_NAME)).thenReturn(app);
		IStatsCollector collector = mock(IStatsCollector.class);
		when(collector.enoughResource()).thenReturn(true);
		when(context.getBean(IStatsCollector.BEAN_NAME)).thenReturn(collector);


		dataStore.save(playlist);

		// Playlist current broadcast is empty scenario

		result = restServiceReal.startStreamSourceV2(playlist.getStreamId());
		assertEquals(false, result.isSuccess());


		// Playlist ID is null scenario

		result = restServiceReal.startStreamSourceV2(null);		
		assertEquals(false, result.isSuccess());

		// Playlist is null scenario

		Broadcast broadcast = new Broadcast();
		dataStore.save(broadcast);
		result = restServiceReal.startStreamSourceV2(broadcast.getStreamId());		

		assertEquals(false, result.isSuccess());

		// Playlist current Broadcast null scenario

		playlist.setCurrentPlayIndex(99);
		result = restServiceReal.startStreamSourceV2(playlist.getStreamId());
		assertEquals(false, result.isSuccess());
		
		Broadcast broadcast2 = dataStore.get(playlist.getStreamId());
		
		assertEquals(AntMediaApplicationAdapter.BROADCAST_STATUS_FINISHED, broadcast2.getStatus());

		
		
	}
	



}
