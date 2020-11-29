package io.antmedia.test.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
import java.util.List;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.red5.server.scope.Scope;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;

import dev.morphia.Datastore;
import dev.morphia.query.Query;
import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.AppSettings;
import io.antmedia.IApplicationAdaptorFactory;
import io.antmedia.datastore.db.DataStore;
import io.antmedia.datastore.db.InMemoryDataStore;
import io.antmedia.datastore.db.MapDBStore;
import io.antmedia.datastore.db.MongoStore;
import io.antmedia.datastore.db.types.VoD;
import io.antmedia.integration.MuxingTest;
import io.antmedia.rest.RestServiceBase.ProcessBuilderFactory;
import io.antmedia.rest.VoDRestService;
import io.antmedia.rest.model.Result;
import io.antmedia.settings.ServerSettings;
import io.vertx.core.Vertx;


@ContextConfiguration(locations = { "test.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class VoDRestServiceV2UnitTest {


	private VoDRestService restServiceReal = null;
	public AntMediaApplicationAdapter app = null;

	static {
		System.setProperty("red5.deployment.type", "junit");
		System.setProperty("red5.root", ".");
	}

	Vertx vertx = io.vertx.core.Vertx.vertx();


	@Before
	public void before() {
		restServiceReal = new VoDRestService();
	}

	@After
	public void after() {
		restServiceReal = null;
	}

	@Test
	public void synchUserVodList()  {

		Result result = new Result(false);

		String vodFolder = "vodFolder";
		VoDRestService streamSourceRest = Mockito.spy(restServiceReal);
		AntMediaApplicationAdapter adaptor = mock (AntMediaApplicationAdapter.class);
		InMemoryDataStore store = new InMemoryDataStore("test");
		AppSettings settings = mock(AppSettings.class);

		Mockito.doReturn(adaptor).when(streamSourceRest).getApplication();
		Mockito.doReturn(store).when(streamSourceRest).getDataStore();
		Mockito.doReturn(settings).when(streamSourceRest).getAppSettings();
		
		
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
		
		String vodFolderPath = "webapps/junit/streams/vod_folder";

		File vodFolder = new File(vodFolderPath);
		vodFolder.mkdirs();
		assertTrue(vodFolder.exists());

		Scope scope = mock(Scope.class);
		String scopeName = "scope";
		when(scope.getName()).thenReturn(scopeName);

		restServiceReal.setScope(scope);

		restServiceReal.setAppSettings(settings);
		
		ServerSettings serverSettings = mock(ServerSettings.class);
		when(serverSettings.getServerName()).thenReturn("localhost");
		restServiceReal.setServerSettings(serverSettings);

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
		
		IApplicationAdaptorFactory application = new IApplicationAdaptorFactory() {
			@Override
			public AntMediaApplicationAdapter getAppAdaptor() {
				return app;
			}
		};

		ApplicationContext context = mock(ApplicationContext.class);
		when(context.getBean(AntMediaApplicationAdapter.BEAN_NAME)).thenReturn(application);

		restServiceReal.setAppCtx(context);

		VoD voD = restServiceReal.getVoD(vodId);
		assertEquals(vodId, voD.getVodId());
		assertEquals(streamVod.getStreamId(), voD.getStreamId());
		assertEquals(streamVod.getVodName(), voD.getVodName());
		assertEquals(streamVod.getFilePath(), voD.getFilePath());

		assertEquals(1, restServiceReal.getVodList(0, 50, null, null, null, null).size());

		restServiceReal.deleteVoD(vodId);

		assertEquals(0, restServiceReal.getVodList(0, 50, null, null, null, null).size());

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
			
			assertEquals(0, restServiceReal.getTotalVodNumber().getNumber());

			restServiceReal.uploadVoDFile(fileName, inputStream);


			assertTrue(f.isDirectory());

			assertEquals(1, f.list().length);

			assertEquals(1, store.getTotalVodNumber());
			
			assertEquals(1, restServiceReal.getTotalVodNumber().getNumber());

		} catch (FileNotFoundException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}
	
	@Test
	public void testVoDSorting() {
		InMemoryDataStore imDatastore = new InMemoryDataStore("datastore");
		vodSorting(imDatastore);
		
		MapDBStore mapDataStore = new MapDBStore("testdb");
		vodSorting(mapDataStore);
		
		DataStore mongoDataStore = new MongoStore("localhost", "", "", "testdb");
		Datastore store = ((MongoStore) mongoDataStore).getVodDatastore();
		Query<VoD> deleteQuery = store.find(VoD.class);
		store.delete(deleteQuery);
		vodSorting(mongoDataStore);
	}
	
	public void vodSorting(DataStore datastore) {
		restServiceReal.setDataStore(datastore);

		VoD vod1 = new VoD("streamName", "streamId", "filePath", "vodName2", 333, 111, 111, VoD.STREAM_VOD, "vod_1");
		VoD vod2 = new VoD("streamName", "streamId", "filePath", "vodName1", 222, 111, 111, VoD.STREAM_VOD, "vod_2");
		VoD vod3 = new VoD("streamName", "streamId", "filePath", "vodName3", 111, 111, 111, VoD.STREAM_VOD, "vod_3");
		
		datastore.addVod(vod1);
		datastore.addVod(vod2);
		datastore.addVod(vod3);

		Scope scope = mock(Scope.class);
		String scopeName = "junit";
		when(scope.getName()).thenReturn(scopeName);

		AntMediaApplicationAdapter app = mock(AntMediaApplicationAdapter.class);
		when(app.getScope()).thenReturn(scope);
		
		IApplicationAdaptorFactory application = new IApplicationAdaptorFactory() {
			@Override
			public AntMediaApplicationAdapter getAppAdaptor() {
				return app;
			}
		};

		ApplicationContext context = mock(ApplicationContext.class);
		when(context.getBean(AntMediaApplicationAdapter.BEAN_NAME)).thenReturn(application);

		restServiceReal.setAppCtx(context);
		
		List<VoD> vodList = restServiceReal.getVodList(0, 50, null, null, null, null);
		assertEquals(3, vodList.size());
		assertEquals(vodList.get(0).getVodId(), vod1.getVodId());
		assertEquals(vodList.get(1).getVodId(), vod2.getVodId());
		assertEquals(vodList.get(2).getVodId(), vod3.getVodId());
		
		vodList = restServiceReal.getVodList(0, 50, "", "", "", "");
		assertEquals(3, vodList.size());
		assertEquals(vodList.get(0).getVodId(), vod1.getVodId());
		assertEquals(vodList.get(1).getVodId(), vod2.getVodId());
		assertEquals(vodList.get(2).getVodId(), vod3.getVodId());
		
		
		vodList = restServiceReal.getVodList(0, 50, "name", "asc", "", "");
		assertEquals(3, vodList.size());
		assertEquals(vodList.get(0).getVodId(), vod2.getVodId());
		assertEquals(vodList.get(1).getVodId(), vod1.getVodId());
		assertEquals(vodList.get(2).getVodId(), vod3.getVodId());

		vodList = restServiceReal.getVodList(0, 50, "name", "asc", "", "vod");
		assertEquals(3, vodList.size());
		assertEquals(vodList.get(0).getVodId(), vod2.getVodId());
		assertEquals(vodList.get(1).getVodId(), vod1.getVodId());
		assertEquals(vodList.get(2).getVodId(), vod3.getVodId());
		
		vodList = restServiceReal.getVodList(0, 50, "name", "desc", null, null);
		assertEquals(3, vodList.size());
		assertEquals(vodList.get(0).getVodId(), vod3.getVodId());
		assertEquals(vodList.get(1).getVodId(), vod1.getVodId());
		assertEquals(vodList.get(2).getVodId(), vod2.getVodId());

		vodList = restServiceReal.getVodList(0, 50, "name", "desc", null,"vo");
		assertEquals(3, vodList.size());
		assertEquals(vodList.get(0).getVodId(), vod3.getVodId());
		assertEquals(vodList.get(1).getVodId(), vod1.getVodId());
		assertEquals(vodList.get(2).getVodId(), vod2.getVodId());
		
		vodList = restServiceReal.getVodList(0, 50, "date", "asc", null, null);
		assertEquals(3, vodList.size());
		assertEquals(vodList.get(0).getVodId(), vod3.getVodId());
		assertEquals(vodList.get(1).getVodId(), vod2.getVodId());
		assertEquals(vodList.get(2).getVodId(), vod1.getVodId());
		
		vodList = restServiceReal.getVodList(0, 50, "date", "desc", null, null);
		assertEquals(3, vodList.size());
		assertEquals(vodList.get(0).getVodId(), vod1.getVodId());
		assertEquals(vodList.get(1).getVodId(), vod2.getVodId());
		assertEquals(vodList.get(2).getVodId(), vod3.getVodId());
		
		vodList = restServiceReal.getVodList(0, 2, "name", "desc", null, null);
		assertEquals(2, vodList.size());
		assertEquals(vodList.get(0).getVodId(), vod3.getVodId());
		assertEquals(vodList.get(1).getVodId(), vod1.getVodId());

		vodList = restServiceReal.getVodList(0, 2, "name", "desc", null, "vod");
		assertEquals(2, vodList.size());
		assertEquals(vodList.get(0).getVodId(), vod3.getVodId());
		assertEquals(vodList.get(1).getVodId(), vod1.getVodId());
		
		datastore.deleteVod(vod1.getVodId());
		datastore.deleteVod(vod2.getVodId());
		datastore.deleteVod(vod3.getVodId());
		
		vodList = restServiceReal.getVodList(0, 50, null, null, null, null);
		assertEquals(0, vodList.size());
	}

	@Test
	public void testGetVoDIdByStreamId()  {
		VoDRestService restService=new VoDRestService();
		InMemoryDataStore dataStore = new InMemoryDataStore("test");
		restService.setDataStore(dataStore);
		String streamId=RandomStringUtils.randomNumeric(24);
		String vodId1="vod_1";
		String vodId2="vod_2";
		String vodId3="vod_3";
		VoD vod1 = new VoD("streamName", streamId, "filePath", "vodName2", 333, 111, 111, VoD.STREAM_VOD, vodId1);
		VoD vod2 = new VoD("streamName", streamId, "filePath", "vodName1", 222, 111, 111, VoD.STREAM_VOD, vodId2);
		VoD vod3 = new VoD("streamName", "streamId123", "filePath", "vodName3", 111, 111, 111, VoD.STREAM_VOD, vodId3);

		dataStore.addVod(vod1);
		dataStore.addVod(vod2);
		dataStore.addVod(vod3);

		List<VoD> vodResult = restService.getVodList(0, 50, null, null, streamId, null);

		boolean vod1Match = false, vod2Match = false;
		for (VoD vod : vodResult) {
			if (vod.getVodId().equals(vod1.getVodId())) {
				vod1Match = true;
			}
			else if (vod.getVodId().equals(vod2.getVodId())) {
				vod2Match = true;
			}
			else if (vod.getVodId().equals(vod3.getVodId())) {
				fail("vod3 should not be matched");
			}
		}
		assertTrue(vod1Match);
		assertTrue(vod2Match);
	}
}
