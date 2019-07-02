package io.antmedia.test.rest;

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

import javax.ws.rs.core.Response;

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
import io.antmedia.rest.BroadcastRestServiceV2;
import io.antmedia.rest.StreamsSourceRestService;
import io.antmedia.rest.VoDRestServiceV2;
import io.antmedia.rest.WebRTCClientStats;
import io.antmedia.rest.model.Interaction;
import io.antmedia.rest.model.Result;
import io.antmedia.rest.model.User;
import io.antmedia.rest.model.Version;
import io.antmedia.security.ITokenService;
import io.antmedia.social.LiveComment;
import io.antmedia.social.ResourceOrigin;
import io.antmedia.social.endpoint.PeriscopeEndpoint;
import io.antmedia.social.endpoint.VideoServiceEndpoint;
import io.antmedia.social.endpoint.VideoServiceEndpoint.DeviceAuthParameters;
import io.antmedia.webrtc.api.IWebRTCAdaptor;
import io.vertx.core.Vertx;


@ContextConfiguration(locations = { "test.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class VoDRestServiceV2UnitTest {


	private VoDRestServiceV2 restServiceReal = null;
	public AntMediaApplicationAdapter app = null;

	static {
		System.setProperty("red5.deployment.type", "junit");
		System.setProperty("red5.root", ".");
	}

	Vertx vertx = io.vertx.core.Vertx.vertx();


	@Before
	public void before() {
		restServiceReal = new VoDRestServiceV2();
	}

	@After
	public void after() {
		restServiceReal = null;
	}

	@Test
	public void synchUserVodList()  {

		Result result = new Result(false);

		String vodFolder = "vodFolder";
		VoDRestServiceV2 streamSourceRest = Mockito.spy(restServiceReal);
		AntMediaApplicationAdapter adaptor = mock (AntMediaApplicationAdapter.class);
		InMemoryDataStore store = new InMemoryDataStore("test");
		AppSettings settings = mock(AppSettings.class);

		Mockito.doReturn(adaptor).when(streamSourceRest).getApplication();
		Mockito.doReturn(store).when(streamSourceRest).getDataStore();
		Mockito.doReturn(settings).when(adaptor).getAppSettings();
		when(settings.getVodFolder()).thenReturn(vodFolder);
		Mockito.doReturn(true).when(adaptor).synchUserVoDFolder(null, vodFolder);


		result = streamSourceRest.synchUserVodList();

		assertTrue(result.isSuccess());
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

			DataStore store = new InMemoryDataStore("testdb");
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


}
