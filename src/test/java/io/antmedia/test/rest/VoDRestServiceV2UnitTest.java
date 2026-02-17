package io.antmedia.test.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.red5.server.scope.Scope;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;

import dev.morphia.Datastore;
import dev.morphia.DeleteOptions;
import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.AppSettings;
import io.antmedia.datastore.db.DataStore;
import io.antmedia.datastore.db.InMemoryDataStore;
import io.antmedia.datastore.db.MapDBStore;
import io.antmedia.datastore.db.MongoStore;
import io.antmedia.datastore.db.types.VoD;
import io.antmedia.integration.AppFunctionalV2Test;
import io.antmedia.integration.MuxingTest;
import io.antmedia.rest.RestServiceBase.ProcessBuilderFactory;
import io.antmedia.rest.VoDRestService;
import io.antmedia.rest.model.Result;
import io.antmedia.settings.ServerSettings;
import io.antmedia.statistic.IStatsCollector;
import io.antmedia.statistic.StatsCollector;
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
		File webapps = new File("webapps");
		try {
			AppFunctionalV2Test.delete(webapps);
		} catch (IOException e) {
			e.printStackTrace();
		}
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
		VoD newVod = new VoD("vodFile", "vodFile", file.getPath(), file.getName(), System.currentTimeMillis(), 0, 0, 6000,
				VoD.USER_VOD,vodId,null);
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

		VoD streamVod = new VoD("streamName", "streamId", "filePath", "vodName", 111, 111, 111, 111, VoD.STREAM_VOD, vodId, null);
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

		assertEquals(1, restServiceReal.getVodList(0, 50, null, null, null, null).size());

		restServiceReal.deleteVoD(vodId);

		assertEquals(0, restServiceReal.getVodList(0, 50, null, null, null, null).size());

		assertNull(datastore.getVoD(vodId));

		vodId = RandomStringUtils.randomNumeric(24);
		//Preview does not exist path
		streamVod = new VoD("streamName", "streamId", "filePath", "vodName", 111, 111, 111, 111, VoD.STREAM_VOD, vodId, "/fake/path/doesnotexist");
		datastore.addVod(streamVod);

		assertNotNull(datastore.getVoD(vodId));

		voD = restServiceReal.getVoD(vodId);
		assertEquals(streamVod.getPreviewFilePath(), voD.getPreviewFilePath());

		assertEquals(1, restServiceReal.getVodList(0, 50, null, null, null, null).size());

		restServiceReal.deleteVoD(vodId);

		assertEquals(0, restServiceReal.getVodList(0, 50, null, null, null, null).size());

	}


	@Test
	public void testDeleteVoDs() {
		InMemoryDataStore datastore = new InMemoryDataStore("datastore");
		restServiceReal.setDataStore(datastore);

		String vodId = RandomStringUtils.randomNumeric(24);

		VoD streamVod = new VoD("streamName", "streamId", "filePath", "vodName", 111, 111, 111, 111, VoD.STREAM_VOD, vodId, null);
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

		assertEquals(1, restServiceReal.getVodList(0, 50, null, null, null, null).size());

		restServiceReal.deleteVoDsBulk(vodId);

		assertEquals(0, restServiceReal.getVodList(0, 50, null, null, null, null).size());

		assertNull(datastore.getVoD(vodId));


		Result result = restServiceReal.deleteVoDs(new String[] {});
		assertFalse(result.isSuccess());

		result = restServiceReal.deleteVoDsBulk(null);
		assertFalse(result.isSuccess());

		result = restServiceReal.deleteVoDsBulk("123" + (int)(Math.random()*10000));
		assertFalse(result.isSuccess());


	}
	

	@Test
	public void testUploadVodFile() throws FileNotFoundException, IOException {
		AppSettings appSettings = new AppSettings();

		String fileName = RandomStringUtils.randomAlphabetic(11) + ".mp4"; 
		try (FileInputStream inputStream = new FileInputStream("src/test/resources/sample_MP4_480.mp4")) {

			Scope scope = mock(Scope.class);
			String scopeName = "scope";
			when(scope.getName()).thenReturn(scopeName);


			File f = new File("webapps/scope/streams");
			MuxingTest.delete(f);

			restServiceReal.setScope(scope);
			restServiceReal.setAppSettings(appSettings);

			DataStore store = new InMemoryDataStore("testdb");
			restServiceReal.setDataStore(store);

			ApplicationContext context = mock(ApplicationContext.class);
			when(context.getBean(AntMediaApplicationAdapter.BEAN_NAME)).thenReturn(app);

			restServiceReal.setAppCtx(context);

			assertNull(f.list());

			assertEquals(0, store.getTotalVodNumber());

			assertEquals(0, restServiceReal.getTotalVodNumber().getNumber());

			Result result = restServiceReal.uploadVoDFile(fileName, inputStream);

			assertTrue(result.isSuccess());
			String vodId = result.getDataId();

			assertTrue(f.isDirectory());

			assertEquals(1, f.list().length);

			assertEquals(1, store.getTotalVodNumber());

			assertEquals(1, restServiceReal.getTotalVodNumber().getNumber());

			assertEquals(30526, restServiceReal.getVoD(vodId).getDuration());

			try (FileInputStream inputStream2 = new FileInputStream("src/test/resources/big-buck-bunny_trailer.webm")) {

				result = restServiceReal.uploadVoDFile(fileName, inputStream2);
				assertTrue(result.isSuccess());

			}


			assertTrue(f.isDirectory());

			assertEquals(2, f.list().length);

			assertEquals(2, store.getTotalVodNumber());

			assertEquals(2, restServiceReal.getTotalVodNumber().getNumber());

			try (FileInputStream inputStream2 = new FileInputStream("src/test/resources/test.mp3")) {

				result = restServiceReal.uploadVoDFile(fileName, inputStream2);
				assertTrue(result.isSuccess());
			}


			assertTrue(f.isDirectory());

			assertEquals(3, f.list().length);

			assertEquals(3, store.getTotalVodNumber());

			assertEquals(3, restServiceReal.getTotalVodNumber().getNumber());

			// Test upload with metadata
			try (FileInputStream inputStream4 = new FileInputStream("src/test/resources/sample_MP4_480.mp4")) {
				String testMetadata = "{\"customField\":\"value\"}";
				Result result4 = restServiceReal.uploadVoDFile(fileName, inputStream4, testMetadata);
				assertTrue(result4.isSuccess());
				
				VoD vodWithMetadata = restServiceReal.getVoD(result4.getDataId());
				assertEquals(testMetadata, vodWithMetadata.getMetadata());
			}

			// Test upload without metadata (null) - should work, metadata should be null
			try (FileInputStream inputStream5 = new FileInputStream("src/test/resources/sample_MP4_480.mp4")) {
				Result result5 = restServiceReal.uploadVoDFile(fileName, inputStream5, null);
				assertTrue(result5.isSuccess());
				
				VoD vodWithoutMetadata = restServiceReal.getVoD(result5.getDataId());
				assertNull(vodWithoutMetadata.getMetadata());
			}
		}


	}

	@Test
	public void testUploadVoDfileNameAndResource() throws IOException {
		VoDRestService streamSourceRest2 = Mockito.spy(restServiceReal);

		MapDBStore datastore = spy(new MapDBStore("testUploadVoDfileNameAndResource", vertx));

		Scope scope = mock(Scope.class);
		String scopeName = "junit";
		when(scope.getName()).thenReturn(scopeName);

		AntMediaApplicationAdapter app = mock(AntMediaApplicationAdapter.class);
		when(app.getScope()).thenReturn(scope);

		ApplicationContext context = mock(ApplicationContext.class);
		when(context.getBean(AntMediaApplicationAdapter.BEAN_NAME)).thenReturn(app);

		StatsCollector statsCollector = mock(StatsCollector.class);
		when(statsCollector.enoughResource()).thenReturn(true);
		when(context.getBean(IStatsCollector.BEAN_NAME)).thenReturn(statsCollector);

		streamSourceRest2.setAppCtx(context);

		String fileName = RandomStringUtils.randomAlphabetic(11) + ".anythingelse";
		try(FileInputStream inputStream = new FileInputStream("src/test/resources/sample_MP4_480.mp4")) {

			Result result = streamSourceRest2.uploadVoDFile(fileName, inputStream);			

			assertFalse(result.isSuccess());
			assertEquals("notSupportedFileType", result.getMessage());

			AppSettings appSettings = new AppSettings();
			Mockito.doReturn(appSettings).when(streamSourceRest2).getAppSettings();
			Mockito.doReturn(datastore).when(streamSourceRest2).getDataStore();

			fileName = RandomStringUtils.randomAlphabetic(11) + ".wmv";
			result = streamSourceRest2.uploadVoDFile(fileName, inputStream);
			assertTrue(result.isSuccess());


			appSettings.setVodUploadFinishScript("src/test/resources/echo.sh");
			result = streamSourceRest2.uploadVoDFile(fileName, inputStream);
			assertTrue(result.isSuccess());


			when(statsCollector.enoughResource()).thenReturn(false);
			result = streamSourceRest2.uploadVoDFile(fileName, inputStream);
			assertFalse(result.isSuccess());
		}
		
		datastore.close(true);

	}

	@Test
	public void testVoDUploadFinishedScript() throws IOException  {

		VoDRestService streamSourceRest2 = Mockito.spy(restServiceReal);

		MapDBStore datastore = spy(new MapDBStore("testVoDUploadFinishedScript", vertx));

		Scope scope = mock(Scope.class);
		String scopeName = "junit";
		when(scope.getName()).thenReturn(scopeName);

		AntMediaApplicationAdapter app = mock(AntMediaApplicationAdapter.class);
		when(app.getScope()).thenReturn(scope);

		ApplicationContext context = mock(ApplicationContext.class);
		when(context.getBean(AntMediaApplicationAdapter.BEAN_NAME)).thenReturn(app);

		StatsCollector statsCollector = mock(StatsCollector.class);
		when(statsCollector.enoughResource()).thenReturn(true);
		when(context.getBean(IStatsCollector.BEAN_NAME)).thenReturn(statsCollector);

		streamSourceRest2.setAppCtx(context);


		AppSettings appSettings = new AppSettings();
		appSettings.setVodUploadFinishScript("src/test/resources/echo.sh");

		Mockito.doReturn(app).when(streamSourceRest2).getApplication();
		Mockito.doReturn(datastore).when(streamSourceRest2).getDataStore();
		Mockito.doReturn(appSettings).when(streamSourceRest2).getAppSettings();



		String fileName = RandomStringUtils.randomAlphabetic(11) + ".mp4";
		try(FileInputStream inputStream = new FileInputStream("src/test/resources/sample_MP4_480.mp4")) {

			Result result = streamSourceRest2.uploadVoDFile(fileName, inputStream);				
			assertTrue(result.isSuccess());

			ArgumentCaptor<VoD> vodCapture = ArgumentCaptor.forClass(VoD.class);
			Mockito.verify(datastore, Mockito.timeout(5000).times(1)).addVod(vodCapture.capture());

			assertEquals(VoD.PROCESS_STATUS_INQUEUE, vodCapture.getValue().getProcessStatus());

			Mockito.verify(streamSourceRest2,Mockito.times(1)).startVoDScriptProcess(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.anyString());

			verify(datastore, Mockito.timeout(5000).times(1)).updateVoDProcessStatus(result.getDataId(), VoD.PROCESS_STATUS_PROCESSING);

			verify(datastore, Mockito.timeout(5000).times(1)).updateVoDProcessStatus(result.getDataId(), VoD.PROCESS_STATUS_FINISHED);


			VoD voD = datastore.getVoD(result.getDataId());

			assertEquals(VoD.PROCESS_STATUS_FINISHED, voD.getProcessStatus());
		}
		
		datastore.close(true);


	}

	@Test
	public void testStartVoDScriptProcess() throws IOException, InterruptedException {
		VoDRestService streamSourceRest2 = Mockito.spy(restServiceReal);

		InMemoryDataStore datastore = spy(new InMemoryDataStore("datastore"));

		streamSourceRest2.setDataStore(datastore);

		String scriptPath = "src/test/resources/echo.sh";
		File file = new File("vodFile");
		VoD vod = mock(VoD.class);
		when(vod.getFilePath()).thenReturn("vodFile");
		when(vod.getVodId()).thenReturn("vodId");

		Process process = mock(Process.class);
		doReturn(process).when(streamSourceRest2).getProcess(Mockito.anyString());


		when(process.waitFor()).thenReturn(0);
		streamSourceRest2.startVoDScriptProcess(scriptPath, file, vod, vod.getVodId());

		verify(datastore, Mockito.timeout(5000).times(1)).updateVoDProcessStatus(vod.getVodId(), VoD.PROCESS_STATUS_PROCESSING);
		verify(datastore, Mockito.timeout(5000).times(1)).updateVoDProcessStatus(vod.getVodId(), VoD.PROCESS_STATUS_FINISHED);


		when(process.waitFor()).thenReturn(-1);

		streamSourceRest2.startVoDScriptProcess(scriptPath, file, vod, vod.getVodId());
		verify(datastore, Mockito.timeout(5000).times(2)).updateVoDProcessStatus(vod.getVodId(), VoD.PROCESS_STATUS_PROCESSING);
		verify(datastore, Mockito.timeout(5000).times(1)).updateVoDProcessStatus(vod.getVodId(), VoD.PROCESS_STATUS_FINISHED);


	}





	@Test
	public void testVoDSorting() {
		InMemoryDataStore imDatastore = new InMemoryDataStore("datastore");
		vodSorting(imDatastore);

		MapDBStore mapDataStore = new MapDBStore(RandomStringUtils.randomAlphanumeric(6) + ".db", vertx);
		vodSorting(mapDataStore);

		DataStore mongoDataStore = new MongoStore("127.0.0.1", "", "", "testdb");
		Datastore store = ((MongoStore) mongoDataStore).getVodDatastore();

		store.find(VoD.class).delete(new DeleteOptions().multi(true));
		vodSorting(mongoDataStore);
	}

	public void vodSorting(DataStore datastore) {
		restServiceReal.setDataStore(datastore);

		VoD vod1 = new VoD("streamName", "streamId", "filePath", "vodName2", 333, 333, 111, 111, VoD.STREAM_VOD, "vod_1", null);
		VoD vod2 = new VoD("streamName", "streamId", "filePath", "vodName1", 222, 222, 111, 111, VoD.STREAM_VOD, "vod_2",null);
		VoD vod3 = new VoD("streamName", "streamId", "filePath", "vodName3", 111, 111, 111, 111, VoD.STREAM_VOD, "vod_3", null);

		datastore.addVod(vod1);
		datastore.addVod(vod2);
		datastore.addVod(vod3);

		Scope scope = mock(Scope.class);
		String scopeName = "junit";
		when(scope.getName()).thenReturn(scopeName);

		AntMediaApplicationAdapter app = mock(AntMediaApplicationAdapter.class);
		when(app.getScope()).thenReturn(scope);

		ApplicationContext context = mock(ApplicationContext.class);
		when(context.getBean(AntMediaApplicationAdapter.BEAN_NAME)).thenReturn(app);

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
		VoD vod1 = new VoD("streamName", streamId, "filePath", "vodName2", 333, 333, 111, 111, VoD.STREAM_VOD, vodId1,null);
		VoD vod2 = new VoD("streamName", streamId, "filePath", "vodName1", 222, 222, 111, 111, VoD.STREAM_VOD, vodId2,null);
		VoD vod3 = new VoD("streamName", "streamId123", "filePath", "vodName3", 111, 111, 111, 111, VoD.STREAM_VOD, vodId3,null);

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

	@Test
	public void testImportVoDs() {
		VoDRestService restService=new VoDRestService();
		InMemoryDataStore dataStore = new InMemoryDataStore("test");
		restService.setDataStore(dataStore);

		Scope scope = mock(Scope.class);
		String scopeName = "junit";
		when(scope.getName()).thenReturn(scopeName);

		AntMediaApplicationAdapter app = new AntMediaApplicationAdapter();
		app.setDataStore(dataStore);
		app.setScope(scope);

		ApplicationContext context = mock(ApplicationContext.class);
		when(context.getBean(AntMediaApplicationAdapter.BEAN_NAME)).thenReturn(app);

		restService.setAppCtx(context);
		List<VoD> vodList = dataStore.getVodList(0, 50, null, null, null, null);
		assertEquals(0, vodList.size());
		Result result = restService.importVoDs("src/test");
		assertTrue(result.isSuccess());
		vodList = dataStore.getVodList(0, 50, null, null, null, null);
		//there are 9 files under src/test directory

		//find the number of files under src/test

		File sourceDir = new File("src/test/resources");
		File[] listFiles = sourceDir.listFiles();
		int numberOfFiles = 0;
		for (File file : listFiles) 
		{
			String fileExtension = FilenameUtils.getExtension(file.getName());

			if (file.isFile() && ("mp4".equals(fileExtension) || "flv".equals(fileExtension)
					|| "mkv".equals(fileExtension) || "m3u8".equals(fileExtension))) 
			{
				numberOfFiles++;
			}
		}

		sourceDir = new File("src/test/resources/fixtures");
		listFiles = sourceDir.listFiles();
		for (File file : listFiles) 
		{
			String fileExtension = FilenameUtils.getExtension(file.getName());

			if (file.isFile() && ("mp4".equals(fileExtension) || "flv".equals(fileExtension)
					|| "mkv".equals(fileExtension) || "m3u8".equals(fileExtension))) 
			{
				numberOfFiles++;
			}
		}


		assertEquals(numberOfFiles, vodList.size());
		//file path should include the relative path
		for (VoD voD : vodList) {
			//file path include the file name
			assertTrue(voD.getVodName().contains("."));
			assertTrue(voD.getFilePath().contains(voD.getVodName()));
		}

		File f = new File("webapps/junit/streams/test");
		assertTrue(Files.isSymbolicLink(f.toPath()));

		//it should find the flv src/test/resources/fixtures/test.flv
		boolean foundFixturesTest = false;
		for (VoD voD : vodList) {
			System.out.println("vod file path: " + voD.getFilePath());
			if (voD.getFilePath().equals("streams/test/resources/fixtures/test.flv")) {
				foundFixturesTest = true;
			}
		}

		assertTrue(foundFixturesTest);


		result = restService.importVoDs("src/test");
		assertFalse(result.isSuccess());
		vodList = dataStore.getVodList(0, 50, null, null, null, null);
		//there are numberOfFiles files under src/test directory it should not increae
		assertEquals(numberOfFiles, vodList.size());

		result = restService.importVoDs(null);
		assertFalse(result.isSuccess());
		vodList = dataStore.getVodList(0, 50, null, null, null, null);
		//there are numberOfFiles files under src/test directory it should not increae
		assertEquals(numberOfFiles, vodList.size());

		result = restService.importVoDs("");
		assertFalse(result.isSuccess());
		vodList = dataStore.getVodList(0, 50, null, null, null, null);
		//there are numberOfFiles files under src/test directory it should not increae
		assertEquals(numberOfFiles, vodList.size());


		result = restService.unlinksVoD(null);
		assertFalse(result.isSuccess());

		result = restService.unlinksVoD("src/test");
		assertTrue(result.isSuccess());
		vodList = dataStore.getVodList(0, 50, null, null, null, null);
		assertEquals(0, vodList.size());


	}
}
