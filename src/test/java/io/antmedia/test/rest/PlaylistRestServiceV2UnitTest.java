package io.antmedia.test.rest;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.red5.server.scope.Scope;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.AppSettings;
import io.antmedia.datastore.db.InMemoryDataStore;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.datastore.db.types.Playlist;
import io.antmedia.rest.PlaylistRestServiceV2;
import io.antmedia.rest.RestServiceBase;
import io.antmedia.rest.model.Result;
import io.antmedia.statistic.StatsCollector;
import io.antmedia.streamsource.StreamFetcherManager;
import io.vertx.core.Vertx;



@ContextConfiguration(locations = { "test.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class PlaylistRestServiceV2UnitTest {

	private PlaylistRestServiceV2 restServiceReal = null;
	public AntMediaApplicationAdapter app = null;

	static {
		System.setProperty("red5.deployment.type", "junit");
		System.setProperty("red5.root", ".");
	}

	Vertx vertx = io.vertx.core.Vertx.vertx();


	@Before
	public void before() {
		restServiceReal = new PlaylistRestServiceV2();
	}

	@After
	public void after() {
		restServiceReal = null;
	}


	@Test
	public void testGetPlaylist() {

		//create a broadcast
		Broadcast broadcastItem1=new Broadcast();

		List<Broadcast> broadcastList = new ArrayList<>();

		broadcastList.add(broadcastItem1);

		Playlist getPlaylistIdNull = null;

		Scope scope = mock(Scope.class);
		String scopeName = "scope";
		when(scope.getName()).thenReturn(scopeName);

		restServiceReal.setScope(scope);

		InMemoryDataStore dataStore = new InMemoryDataStore("testdb");
		restServiceReal.setDataStore(dataStore);

		String checkPlaylistIdNull = null;

		getPlaylistIdNull = restServiceReal.getPlaylist(checkPlaylistIdNull);

		// Check Get Playlist is null
		assertEquals(null, getPlaylistIdNull.getPlaylistId());

		//Create playlist

		Playlist oldPlaylist = new Playlist("playlistTestId" ,0 ,"playlistName", 111, 111, broadcastList);

		boolean oldPlaylistCreated = dataStore.createPlaylist(oldPlaylist);

		assertEquals(true, oldPlaylistCreated);

		Playlist newPlaylist = restServiceReal.getPlaylist(oldPlaylist.getPlaylistId());

		assertEquals("playlistTestId", newPlaylist.getPlaylistId());

		assertEquals(newPlaylist.getPlaylistId(), oldPlaylist.getPlaylistId());


	}



	@Test
	public void testCreatePlaylist() {

		Result result = new Result(false);

		RestServiceBase restService = mock(RestServiceBase.class);

		//create a broadcast
		Broadcast broadcastItem1=new Broadcast();

		//create a broadcast
		Broadcast broadcastItem2=new Broadcast();

		List<Broadcast> broadcastList = new ArrayList<>();

		broadcastList.add(broadcastItem1);
		broadcastList.add(broadcastItem2);

		Playlist playlist = new Playlist("playlistTestId" ,0 ,"playlistName", 111, 111, broadcastList);

		Scope scope = mock(Scope.class);
		String scopeName = "scope";
		when(scope.getName()).thenReturn(scopeName);

		restServiceReal.setScope(scope);

		ApplicationContext context = mock(ApplicationContext.class);
		AppSettings settings = mock(AppSettings.class);

		restServiceReal.setAppSettings(settings);

		InMemoryDataStore dataStore = new InMemoryDataStore("testdb");
		restServiceReal.setDataStore(dataStore);
		restService.setDataStore(dataStore);

		restServiceReal.setAppCtx(context);

		// Add playlist in DB

		dataStore.createPlaylist(playlist);

		// Test already created playlist Id  

		result = restServiceReal.createPlaylist(playlist, false);

		assertEquals("Playlist id is already being used" , result.getMessage());

		// Test already invalid created playlist Id  

		playlist.setPlaylistId(" asd asd _ ?/assa s");

		result = restServiceReal.createPlaylist(playlist, false);

		assertEquals("Playlist id is not valid" , result.getMessage());

		// Test null playlist Id


		playlist.setPlaylistId(null);

		//doReturn(restService.saveBroadcast(broadcastItem1, AntMediaApplicationAdapter.BROADCAST_STATUS_CREATED, scope.getName(), dataStore, "", "", "")).when(restServiceReal).saveBroadcast(any(), any(), any(), any(), any(), any(), any());

		/*
		when(restServiceReal.saveBroadcast(any(), any(), any(), any(), any(), any(), any())).thenReturn(restService.saveBroadcast(broadcastItem1, "", scopeName, dataStore, "", "", ""));

		when(restService.getDataStore().save(any())).thenReturn(any());

		result = restServiceReal.createPlaylist(playlist, false);

		assertNotNull(playlist.getPlaylistId());
		 */

	}

	@Test
	public void testDeletePlaylist() {

		Playlist playlist = new Playlist();
		Result result = new Result(false);
		AppSettings settings = mock(AppSettings.class);

		Scope scope = mock(Scope.class);
		String scopeName = "scope";
		when(scope.getName()).thenReturn(scopeName);

		restServiceReal.setScope(scope);

		InMemoryDataStore dataStore = new InMemoryDataStore("testdb");

		AntMediaApplicationAdapter app = Mockito.spy(new AntMediaApplicationAdapter());

		app.setAppSettings(settings);

		Mockito.doReturn(dataStore).when(app).getDataStore();

		restServiceReal.setDataStore(dataStore);

		playlist.setPlaylistId("testPlaylistId");

		//create a broadcast
		Broadcast broadcastItem1=new Broadcast();

		List<Broadcast> broadcastList = new ArrayList<>();

		broadcastList.add(broadcastItem1);

		playlist.setBroadcastItemList(broadcastList);

		dataStore.createPlaylist(playlist);

		result = restServiceReal.deletePlaylist(playlist.getPlaylistId());

		assertEquals(true, result.isSuccess());

		// If there is no Playlist in DB.

		result = restServiceReal.deletePlaylist("testDbNullPlaylistId");

		assertEquals(false, result.isSuccess());

		// If Playlist ID is null.

		result = restServiceReal.deletePlaylist(null);

		assertEquals(false, result.isSuccess());


	}

	@Test
	public void testEditPlaylist() {

		Result result = new Result(false);

		//create a broadcast
		Broadcast broadcastItem1=new Broadcast();

		//create a broadcast
		Broadcast broadcastItem2=new Broadcast();

		List<Broadcast> broadcastList = new ArrayList<>();

		broadcastList.add(broadcastItem1);
		broadcastList.add(broadcastItem2);

		Playlist playlist = new Playlist("playlistTestId" ,0 ,"playlistName", 111, 111, broadcastList);

		Scope scope = mock(Scope.class);
		String scopeName = "scope";
		when(scope.getName()).thenReturn(scopeName);

		restServiceReal.setScope(scope);

		ApplicationContext context = mock(ApplicationContext.class);

		InMemoryDataStore dataStore = new InMemoryDataStore("testdb");
		restServiceReal.setDataStore(dataStore);

		restServiceReal.setAppCtx(context);

		playlist.setPlaylistId("testPlaylistId");

		dataStore.createPlaylist(playlist);

		// getPlaylistId = null & playlistId = null

		playlist.setPlaylistId(null);

		result = restServiceReal.editPlaylist(null, playlist);

		assertEquals(false, result.isSuccess());

		// getPlaylistId = null & playlistId != null		

		result = restServiceReal.editPlaylist("test123", playlist);

		assertEquals(false, result.isSuccess());

		// getPlaylistId != null & playlistId = null

		playlist.setPlaylistId("notNullId");

		result = restServiceReal.editPlaylist(null, playlist);

		// getPlaylistId != null & playlistId != null

		playlist.setPlaylistId("afterTestPlaylistId");

		playlist.setPlaylistName("afterTestPlaylistName");

		result = restServiceReal.editPlaylist(playlist.getPlaylistId(), playlist);

		assertEquals(true, result.isSuccess());

		assertEquals("afterTestPlaylistId", playlist.getPlaylistId());

		assertEquals("afterTestPlaylistName", playlist.getPlaylistName());	

		// getPlaylistId != null & playlistId != null

		playlist.setPlaylistId("afterTestPlaylistId");

		playlist.setPlaylistName("afterTestPlaylistName");

		playlist.setBroadcastItemList(null);

		result = restServiceReal.editPlaylist(playlist.getPlaylistId(), playlist);

		assertEquals(true, result.isSuccess());

		assertEquals("afterTestPlaylistId", playlist.getPlaylistId());

		assertEquals("afterTestPlaylistName", playlist.getPlaylistName());	

	}

	/*
	@Test
	public void testStopPlaylist() {

		RestServiceBase restService = mock(RestServiceBase.class);

		AntMediaApplicationAdapter adptr = mock(AntMediaApplicationAdapter.class);

		Result result = new Result(true);

		//create a broadcast
		Broadcast broadcastItem1=new Broadcast();

		try {
			broadcastItem1.setStreamId("asdas");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		//create a broadcast
		Broadcast broadcastItem2=new Broadcast();

		List<Broadcast> broadcastList = new ArrayList<>();

		broadcastList.add(broadcastItem1);
		broadcastList.add(broadcastItem2);

		Playlist playlist = new Playlist("playlistTestId" ,0 ,"playlistName", 111, 111, broadcastList);

		Scope scope = mock(Scope.class);
		String scopeName = "scope";
		when(scope.getName()).thenReturn(scopeName);

		restServiceReal.setScope(scope);

		ApplicationContext context = mock(ApplicationContext.class);

		InMemoryDataStore dataStore = new InMemoryDataStore("testdb");
		restServiceReal.setDataStore(dataStore);

		restServiceReal.setAppCtx(context);

		playlist.setPlaylistId("testPlaylistId");

		playlist.setPlaylistStatus("");

		dataStore.createPlaylist(playlist);

		when(restService.getApplication()).thenReturn(adptr);

		when(restService.getApplication().stopStreaming(any())).thenReturn(result);

		//result = restServiceReal.stopPlaylist(playlist.getPlaylistId());		

		//assertEquals(AntMediaApplicationAdapter.BROADCAST_STATUS_FINISHED, playlist.getPlaylistStatus());


	}
	 */
	@Test
	public void testStartPlaylist() {


		RestServiceBase restService = mock(RestServiceBase.class);

		AntMediaApplicationAdapter adptr = mock(AntMediaApplicationAdapter.class);

		Result result = new Result(false);

		//create a broadcast
		Broadcast broadcastItem1=new Broadcast();

		try {
			broadcastItem1.setStreamId("asdas");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		//create a broadcast
		Broadcast broadcastItem2=new Broadcast();

		List<Broadcast> broadcastList = new ArrayList<>();

		broadcastList.add(broadcastItem1);
		broadcastList.add(broadcastItem2);

		Playlist playlist = new Playlist("playlistTestId" ,0 ,"playlistName", 111, 111, broadcastList);

		Scope scope = mock(Scope.class);
		String scopeName = "scope";
		when(scope.getName()).thenReturn(scopeName);

		restServiceReal.setScope(scope);

		ApplicationContext context = mock(ApplicationContext.class);

		AntMediaApplicationAdapter application = mock(AntMediaApplicationAdapter.class);

		InMemoryDataStore dataStore = new InMemoryDataStore("testdb");
		restServiceReal.setDataStore(dataStore);

		restServiceReal.setAppCtx(context);

		playlist.setPlaylistId("testPlaylistId");

		playlist.setPlaylistStatus("");

		dataStore.createPlaylist(playlist);

		//when(restService.startPlaylistService(playlist)).thenReturn(result);

		//	Mockito.doReturn(restService.startPlaylistService(playlist)).when(restServiceReal).startPlaylistService(playlist);

		//when(restServiceReal.startPlaylistService(playlist)).thenReturn(result);

		result = restServiceReal.startPlaylist(null);		

		assertEquals(false,result.isSuccess());


		StatsCollector monitor = mock(StatsCollector.class);
		when(monitor.enoughResource()).thenReturn(false);
		when(context.getBean(StatsCollector.BEAN_NAME)).thenReturn(monitor);

		result = restServiceReal.startPlaylist(playlist.getPlaylistId());

		assertEquals(false,result.isSuccess());

		// Start Playlist with Enough resources

		StreamFetcherManager asd = mock(StreamFetcherManager.class);

		//when(restService.getApplication()).thenReturn(application);

		Mockito.doReturn(application).when(restService).getApplication();

		when(application.getStreamFetcherManager()).thenReturn(asd);

		when(monitor.enoughResource()).thenReturn(true);
		when(context.getBean(StatsCollector.BEAN_NAME)).thenReturn(monitor);

		//result = restServiceReal.startPlaylist(playlist.getPlaylistId());		

		//assertEquals(AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING, playlist.getPlaylistStatus());

		//assertEquals(true,result.isSuccess());



	}


}
