package io.antmedia.test.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
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

import com.google.common.base.Verify;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.AppSettings;
import io.antmedia.IApplicationAdaptorFactory;
import io.antmedia.datastore.db.DataStore;
import io.antmedia.datastore.db.InMemoryDataStore;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.datastore.db.types.Playlist;
import io.antmedia.rest.PlaylistRestService;
import io.antmedia.rest.model.Result;
import io.antmedia.settings.ServerSettings;
import io.antmedia.statistic.StatsCollector;
import io.vertx.core.Vertx;
import static org.mockito.Mockito.*;



@ContextConfiguration(locations = { "test.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class PlaylistRestServiceV2UnitTest {

	private PlaylistRestService restServiceReal = null;
	public AntMediaApplicationAdapter app = null;

	static {
		System.setProperty("red5.deployment.type", "junit");
		System.setProperty("red5.root", ".");
	}

	Vertx vertx = io.vertx.core.Vertx.vertx();


	@Before
	public void before() {
		restServiceReal = new PlaylistRestService();
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

		Playlist oldPlaylist = new Playlist("playlistTestId" ,0 ,"playlistName",AntMediaApplicationAdapter.BROADCAST_STATUS_CREATED, 111, 111, broadcastList);

		boolean oldPlaylistCreated = dataStore.createPlaylist(oldPlaylist);

		assertEquals(true, oldPlaylistCreated);

		Playlist newPlaylist = restServiceReal.getPlaylist(oldPlaylist.getPlaylistId());

		assertEquals("playlistTestId", newPlaylist.getPlaylistId());

		assertEquals(newPlaylist.getPlaylistId(), oldPlaylist.getPlaylistId());


	}



	@Test
	public void testCreatePlaylist() {

		Result result = new Result(false);

		//create a broadcast
		Broadcast broadcastItem1=new Broadcast();

		//create a broadcast
		Broadcast broadcastItem2=new Broadcast();

		List<Broadcast> broadcastList = new ArrayList<>();

		broadcastList.add(broadcastItem1);
		broadcastList.add(broadcastItem2);

		Playlist playlist = new Playlist("playlistTestId" ,0 ,"playlistName",AntMediaApplicationAdapter.BROADCAST_STATUS_CREATED, 111, 111, broadcastList);


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

		IApplicationAdaptorFactory application = new IApplicationAdaptorFactory() {
			@Override
			public AntMediaApplicationAdapter getAppAdaptor() {
				return app;
			}
		};



		ServerSettings serverSettings = Mockito.mock(ServerSettings.class);
		restServiceReal.setServerSettings(serverSettings);

		when(context.getBean(AntMediaApplicationAdapter.BEAN_NAME)).thenReturn(application);

		//when(restServiceSpy.getApplication()).thenReturn(adptr);

		Mockito.doReturn(dataStore).when(app).getDataStore();

		// Add playlist in DB

		dataStore.createPlaylist(playlist);

		// Test already created playlist Id  

		result = restServiceReal.createPlaylist(playlist, false);

		assertEquals("Playlist id is already being used" , result.getMessage());

		// Test already invalid created playlist Id  

		playlist.setPlaylistId(" asd asd _ ?/assa s");

		result = restServiceReal.createPlaylist(playlist, false);

		assertEquals("Playlist id is not valid" , result.getMessage());

		// Test null playlist Id and not empty broadcast list

		playlist.setPlaylistId(null);		

		when(monitor.enoughResource()).thenReturn(false);
		when(context.getBean(StatsCollector.BEAN_NAME)).thenReturn(monitor);

		result = restServiceReal.createPlaylist(playlist, true);

		assertNotNull(playlist.getPlaylistId());

		assertEquals(false, result.isSuccess());

		// Test with null broadcast Item list and false

		playlist.setPlaylistId("testWithNullBroadcastList");

		broadcastList.clear();

		playlist.setBroadcastItemList(broadcastList);

		result = restServiceReal.createPlaylist(playlist, false);

		assertNotNull(playlist.getPlaylistId());

		assertEquals(true, result.isSuccess());


	}

	@Test
	public void testDeletePlaylist() {

		PlaylistRestService restServiceSpy = Mockito.spy(restServiceReal);

		AntMediaApplicationAdapter adptr = mock(AntMediaApplicationAdapter.class);

		Playlist playlist = new Playlist();
		Result result = new Result(false);		

		Scope scope = mock(Scope.class);
		String scopeName = "scope";
		when(scope.getName()).thenReturn(scopeName);

		restServiceReal.setScope(scope);

		ApplicationContext context = mock(ApplicationContext.class);

		InMemoryDataStore dataStore = new InMemoryDataStore("testdb");
		restServiceReal.setDataStore(dataStore);

		restServiceReal.setAppCtx(context);

		AntMediaApplicationAdapter app = mock(AntMediaApplicationAdapter.class);
		when(app.getScope()).thenReturn(scope);

		IApplicationAdaptorFactory application = new IApplicationAdaptorFactory() {
			@Override
			public AntMediaApplicationAdapter getAppAdaptor() {
				return app;
			}
		};

		when(context.getBean(AntMediaApplicationAdapter.BEAN_NAME)).thenReturn(application);

		when(restServiceSpy.getApplication()).thenReturn(adptr);

		when(restServiceSpy.getApplication().stopStreaming(Mockito.any())).thenReturn(result);


		playlist.setPlaylistId("testPlaylistId");

		//create a broadcast
		Broadcast broadcastItem1=new Broadcast();

		List<Broadcast> broadcastList = new ArrayList<>();

		broadcastList.add(broadcastItem1);

		playlist.setBroadcastItemList(broadcastList);

		dataStore.createPlaylist(playlist);


		result = restServiceReal.deletePlaylist(playlist.getPlaylistId());

		assertEquals(true, result.isSuccess());

		//If there is no Playlist in DB.

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

		Playlist playlist = new Playlist("playlistTestId" ,0 ,"playlistName",AntMediaApplicationAdapter.BROADCAST_STATUS_CREATED, 111, 111, broadcastList);

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

		broadcastList.clear();

		playlist.setBroadcastItemList(broadcastList);

		result = restServiceReal.editPlaylist(playlist.getPlaylistId(), playlist);

		assertEquals(true, result.isSuccess());

		assertEquals("afterTestPlaylistId", playlist.getPlaylistId());

		assertEquals("afterTestPlaylistName", playlist.getPlaylistName());	

	}


	@Test
	public void testStopPlaylist() {

		PlaylistRestService restServiceSpy = Mockito.spy(restServiceReal);

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

		Playlist playlist = new Playlist("testPlaylistId" ,0 ,"playlistName",AntMediaApplicationAdapter.BROADCAST_STATUS_CREATED, 111, 111, broadcastList);



		Scope scope = mock(Scope.class);
		String scopeName = "scope";
		when(scope.getName()).thenReturn(scopeName);

		restServiceReal.setScope(scope);

		ApplicationContext context = mock(ApplicationContext.class);

		InMemoryDataStore dataStore = new InMemoryDataStore("testdb");
		restServiceReal.setDataStore(dataStore);

		restServiceReal.setAppCtx(context);

		AntMediaApplicationAdapter app = mock(AntMediaApplicationAdapter.class);
		when(app.getScope()).thenReturn(scope);

		IApplicationAdaptorFactory application = new IApplicationAdaptorFactory() {
			@Override
			public AntMediaApplicationAdapter getAppAdaptor() {
				return app;
			}
		};

		when(context.getBean(AntMediaApplicationAdapter.BEAN_NAME)).thenReturn(application);

		playlist.setPlaylistId("testPlaylistId");

		playlist.setPlaylistStatus("");

		dataStore.createPlaylist(playlist);


		// Playlist current broadcast is empty scenario

		try {
			playlist.getBroadcastItemList().get(playlist.getCurrentPlayIndex()).setStreamId("");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	

		result = restServiceReal.stopPlaylist(playlist.getPlaylistId());	

		assertEquals(false, result.isSuccess());

		// Playlist ID is null scenario

		result = restServiceReal.stopPlaylist(null);		

		assertEquals(false, result.isSuccess());

		// Playlist is null scenario

		result = restServiceReal.stopPlaylist("nullPlaylist");		

		assertEquals(false, result.isSuccess());

		assertEquals("Playlist not found", result.getMessage());

		// Playlist current Broadcast null scenario

		playlist.setCurrentPlayIndex(99);

		result = restServiceReal.stopPlaylist(playlist.getPlaylistId());		

		assertEquals(false, result.isSuccess());

		assertEquals("Playlist Current Broadcast not found. Playlist Broadcast Size: " + playlist.getBroadcastItemList().size() + " Playlist Current Broadcast Index: "+playlist.getCurrentPlayIndex(), result.getMessage());

		// Playlist is stop normal scenario
		// Pllaylist current broadcast ID change back

		// Restore playlist default value
		playlist = dataStore.getPlaylist(playlist.getPlaylistId());
		playlist.setCurrentPlayIndex(0);

		try {
			playlist.getBroadcastItemList().get(playlist.getCurrentPlayIndex()).setStreamId("testPlaylistId");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	

		when(restServiceSpy.getApplication()).thenReturn(adptr);

		when(restServiceSpy.getApplication().stopStreaming(Mockito.any())).thenReturn(result);

		result = restServiceReal.stopPlaylist(playlist.getPlaylistId());	

		assertEquals(AntMediaApplicationAdapter.BROADCAST_STATUS_FINISHED,playlist.getPlaylistStatus());

	}

	@Test
	public void testStartPlaylist() {

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

		Playlist playlist = new Playlist("playlistTestId" ,0 ,"playlistName",AntMediaApplicationAdapter.BROADCAST_STATUS_FINISHED, 111, 111, broadcastList);



		Scope scope = mock(Scope.class);
		String scopeName = "scope";
		when(scope.getName()).thenReturn(scopeName);

		restServiceReal.setScope(scope);

		ApplicationContext context = mock(ApplicationContext.class);

		InMemoryDataStore dataStore = new InMemoryDataStore("testdb");
		restServiceReal.setDataStore(dataStore);

		restServiceReal.setAppCtx(context);

		AntMediaApplicationAdapter app = mock(AntMediaApplicationAdapter.class);
		when(app.getScope()).thenReturn(scope);

		IApplicationAdaptorFactory application = new IApplicationAdaptorFactory() {
			@Override
			public AntMediaApplicationAdapter getAppAdaptor() {
				return app;
			}
		};

		when(context.getBean(AntMediaApplicationAdapter.BEAN_NAME)).thenReturn(application);

		playlist.setPlaylistStatus("");

		dataStore.createPlaylist(playlist);

		// Playlist current broadcast is empty scenario

		try {
			playlist.getBroadcastItemList().get(playlist.getCurrentPlayIndex()).setStreamId("");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	

		result = restServiceReal.startPlaylist(playlist.getPlaylistId());	

		assertEquals(false, result.isSuccess());


		// Playlist ID is null scenario

		result = restServiceReal.startPlaylist(null);		

		assertEquals(false, result.isSuccess());

		// Playlist is null scenario

		result = restServiceReal.startPlaylist("nullPlaylist");		

		assertEquals(false, result.isSuccess());

		assertEquals("Playlist not found", result.getMessage());

		// Playlist current Broadcast null scenario

		playlist.setCurrentPlayIndex(99);

		result = restServiceReal.startPlaylist(playlist.getPlaylistId());		

		assertEquals(false, result.isSuccess());

		assertEquals("Playlist Current Broadcast not found. Playlist Broadcast Size: " + playlist.getBroadcastItemList().size() + " Playlist Current Broadcast Index: "+playlist.getCurrentPlayIndex(), result.getMessage());

	}
	
	@Test
	public void testGetOrCreatePlaylist() {
		DataStore dataStore = mock(DataStore.class);
		restServiceReal.setDataStore(dataStore );
		assertNotNull(restServiceReal.getOrCreatePlaylist(null));
		verify(dataStore, never()).getPlaylist(anyString());
		restServiceReal.getOrCreatePlaylist("test");
		verify(dataStore, times(1)).getPlaylist("test");
	}


}
