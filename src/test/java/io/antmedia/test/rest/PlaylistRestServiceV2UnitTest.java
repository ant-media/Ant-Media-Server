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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.datastore.db.InMemoryDataStore;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.datastore.db.types.Playlist;
import io.antmedia.rest.PlaylistRestServiceV2;
import io.antmedia.rest.model.Result;
import io.vertx.core.Vertx;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.meta.When;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.red5.server.api.stream.IClientBroadcastStream;
import org.red5.server.api.stream.IStreamCapableConnection;
import org.red5.server.scope.Scope;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.AppSettings;
import io.antmedia.IApplicationAdaptorFactory;
import io.antmedia.datastore.db.DataStore;
import io.antmedia.datastore.db.InMemoryDataStore;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.datastore.db.types.ConferenceRoom;
import io.antmedia.datastore.db.types.Endpoint;
import io.antmedia.datastore.db.types.SocialEndpointCredentials;
import io.antmedia.datastore.db.types.TensorFlowObject;
import io.antmedia.datastore.db.types.Token;
import io.antmedia.datastore.db.types.VoD;
import io.antmedia.integration.MuxingTest;
import io.antmedia.muxer.HLSMuxer;
import io.antmedia.muxer.Mp4Muxer;
import io.antmedia.muxer.MuxAdaptor;
import io.antmedia.muxer.Muxer;
import io.antmedia.rest.BroadcastRestService;
import io.antmedia.rest.BroadcastRestService.BroadcastStatistics;
import io.antmedia.rest.BroadcastRestService.ProcessBuilderFactory;
import io.antmedia.rest.RestServiceBase;
import io.antmedia.rest.StreamsSourceRestService;
import io.antmedia.rest.WebRTCClientStats;
import io.antmedia.rest.model.Interaction;
import io.antmedia.rest.model.Result;
import io.antmedia.rest.model.User;
import io.antmedia.rest.model.Version;
import io.antmedia.security.ITokenService;
import io.antmedia.settings.ServerSettings;
import io.antmedia.social.LiveComment;
import io.antmedia.social.ResourceOrigin;
import io.antmedia.social.endpoint.PeriscopeEndpoint;
import io.antmedia.social.endpoint.VideoServiceEndpoint;
import io.antmedia.social.endpoint.VideoServiceEndpoint.DeviceAuthParameters;
import io.antmedia.statistic.StatsCollector;
import io.antmedia.webrtc.api.IWebRTCAdaptor;
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

		Playlist oldPlaylist = new Playlist("playlistTestId" ,0 ,"playlistName", 111, 111, broadcastList);
		/*
		Result oldPlaylistCreated = restServiceReal.createPlaylist(oldPlaylist,false);

		assertEquals(true, oldPlaylistCreated.isSuccess());

		Playlist newPlaylist = restServiceReal.getPlaylist(oldPlaylist.getPlaylistId());

		assertEquals("testPlaylistId", newPlaylist.getPlaylistId());

		assertEquals(newPlaylist.getPlaylistId(), oldPlaylist.getPlaylistId());
*/

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
		
		//when(restServiceReal.saveBroadcast(broadcastItem1, "", scopeName, dataStore, "", "", "")).thenReturn(value)
		
		//result = restServiceReal.createPlaylist(playlist, false);

		//assertNotNull(playlist.getPlaylistId());
		
		
		
		

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
		
		playlist.setPlaylistId(null);
		
		result = restServiceReal.editPlaylist(playlist.getPlaylistId(), playlist);
		
		assertEquals(false, result.isSuccess());
		
		playlist.setPlaylistId("afterTestPlaylistId");
		
		playlist.setPlaylistName("afterTestPlaylistName");
		
		result = restServiceReal.editPlaylist(playlist.getPlaylistId(), playlist);
		
		assertEquals(true, result.isSuccess());
		
		assertEquals("afterTestPlaylistId", playlist.getPlaylistId());
		
		assertEquals("afterTestPlaylistName", playlist.getPlaylistName());	

	}

	@Test
	public void testStopPlaylist() {
		
		

	}

	@Test
	public void testStartPlaylist() {

	}
	

}
