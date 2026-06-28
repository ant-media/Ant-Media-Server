package io.antmedia.test;

import static org.bytedeco.ffmpeg.global.avcodec.av_packet_unref;
import static org.bytedeco.ffmpeg.global.avformat.av_read_frame;
import static org.bytedeco.ffmpeg.global.avformat.avformat_alloc_context;
import static org.bytedeco.ffmpeg.global.avformat.avformat_find_stream_info;
import static org.bytedeco.ffmpeg.global.avformat.avformat_open_input;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.RandomUtils;
import org.awaitility.Awaitility;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.avutil.AVDictionary;
import org.bytedeco.ffmpeg.global.avformat;
import org.bytedeco.ffmpeg.global.avutil;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.red5.server.scope.WebScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.AppSettings;
import io.antmedia.FFmpegUtilities;
import io.antmedia.datastore.db.DataStore;
import io.antmedia.datastore.db.IDataStoreFactory;
import io.antmedia.datastore.db.InMemoryDataStore;
import io.antmedia.datastore.db.MapDBStore;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.datastore.db.types.Broadcast.PlayListItem;
import io.antmedia.integration.AppFunctionalV2Test;
import io.antmedia.muxer.IAntMediaStreamHandler;
import io.antmedia.muxer.Muxer;
import io.antmedia.rest.BroadcastRestService;
import io.antmedia.rest.model.Result;
import io.antmedia.statistic.IStatsCollector;
import io.antmedia.streamsource.StreamFetcher;
import io.antmedia.streamsource.StreamFetcherManager;
import io.antmedia.streamsource.StreamFetcher.IStreamFetcherListener;
import io.vertx.core.Vertx;

@ContextConfiguration(locations = { "test.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
public class StreamSchedularUnitTest extends AbstractJUnit4SpringContextTests {

	public Application app = null;
	public static String VALID_MP4_URL = "https://avtshare01.rz.tu-ilmenau.de/avt-vqdb-uhd-1/test_1/segments/bigbuck_bunny_8bit_750kbps_720p_60.0fps_h264.mp4";
	public static String VALID_LONG_DURATION_MP4_URL = "https://avtshare01.rz.tu-ilmenau.de/avt-vqdb-uhd-1/test_1/segments/cutting_orange_tuil_2000kbps_720p_59.94fps_h264.mp4";
	public static String VALID_LONG_DURATION_MP4_URL_2 = "https://avtshare01.rz.tu-ilmenau.de/avt-vqdb-uhd-1/test_1/segments/vegetables_tuil_2000kbps_720p_59.94fps_h264.mp4";
	public static String VALID_LONG_DURATION_MP4_URL_3 = "https://avtshare01.rz.tu-ilmenau.de/avt-vqdb-uhd-1/test_2/segments/Dancers_8s_2470kbps_720p_60.0fps_h264.mp4";
	public static String INVALID_MP4_URL = "invalid_link";
	public static String INVALID_403_MP4_URL = "https://httpstat.us/403";
	private WebScope appScope;
	protected static Logger logger = LoggerFactory.getLogger(StreamSchedularUnitTest.class);

	static {
		System.setProperty("red5.deployment.type", "junit");
		System.setProperty("red5.root", ".");


	}

	@Rule
	public TestRule watcher = new TestWatcher() {
		protected void starting(Description description) {
			System.out.println("Starting test: " + description.getMethodName());
		}

		protected void failed(Throwable e, Description description) {
			e.printStackTrace();
			System.out.println("Failed test: " + description.getMethodName());
		};
		protected void finished(Description description) {
			System.out.println("Finishing test: " + description.getMethodName());
		};
	};
	private AntMediaApplicationAdapter appInstance;
	private AppSettings appSettings;
	private Vertx vertx;


	@BeforeClass
	public static void beforeClass() {
		//avformat.av_register_all();
		avformat.avformat_network_init();
	}

	@Before
	public void before() {
		File webApps = new File("webapps");
		if (!webApps.exists()) {
			webApps.mkdirs();
		}
		File junit = new File(webApps, "junit");
		if (!junit.exists()) {
			junit.mkdirs();
		}

		if (appScope == null) {
			appScope = (WebScope) applicationContext.getBean("web.scope");
			logger.debug("Application / web scope: {}", appScope);
			assertTrue(appScope.getDepth() == 1);
		}

		if (app == null) {
			app = (Application) applicationContext.getBean("web.handler");
			logger.debug("Application / web scope: {}", appScope);
			assertTrue(appScope.getDepth() == 1);
		}

		vertx = (Vertx) applicationContext.getBean(AntMediaApplicationAdapter.VERTX_BEAN_NAME);

		//reset to default
		Application.enableSourceHealthUpdate = false;

	}

	@After
	public void after() {

		try {
			AppFunctionalV2Test.delete(new File("webapps"));
		} catch (IOException e) {
			e.printStackTrace();
		}

		//reset to default
		Application.enableSourceHealthUpdate = false;

	}

	/*
	 *@Test This test is commented out, because isStreamAlive is controlled instead of just controlling thread aliveness in {@link testThreadStopStart}
	 */
	public void testStreamSchedular() throws InterruptedException {

		try {
			AVFormatContext inputFormatContext = new AVFormatContext();

			Broadcast newCam = new Broadcast("testSchedular", "10.2.40.63:8080", "admin", "admin",
					"rtsp://184.72.239.149/vod/mp4:BigBuckBunny_115k.mov", "streamSource");

			StreamFetcher camScheduler = new StreamFetcher(newCam.getStreamUrl(), newCam.getStreamId(), newCam.getType(), appScope, null, 0);

			camScheduler.setConnectionTimeout(10000);

			camScheduler.startStream();
			Thread.sleep(7000);

			//this should be false because this rtsp url cannot be used

			assertTrue(camScheduler.isStreamAlive());

			camScheduler.stopStream();

			Thread.sleep(5000);

			assertFalse(camScheduler.isStreamAlive());
			assertFalse(camScheduler.isThreadActive());
		}
		catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}


	@Test
	public void testStreamSchedularConnectionTimeout() throws InterruptedException {
		logger.info("running testStreamSchedularConnectionTimeout");
		try (AVFormatContext inputFormatContext = new AVFormatContext()) {


			Broadcast newCam = new Broadcast("testSchedular2", "10.2.40.64:8080", "admin", "admin",
					"rtsp://11.2.40.63:8554/live1.sdp", AntMediaApplicationAdapter.IP_CAMERA);

			newCam.setStreamId("new_cam" + (int)(Math.random()*10000));

			StreamFetcher streamScheduler = new StreamFetcher(newCam.getStreamUrl(), newCam.getStreamId(), newCam.getType(), appScope, vertx, 0);

			assertFalse(streamScheduler.isExceptionInThread());

			assertNotNull(streamScheduler.getDataStore().save(newCam));



			streamScheduler.startStream();

			streamScheduler.setConnectionTimeout(2000);


			Awaitility.await().pollDelay(3, TimeUnit.SECONDS).until(() -> 
			!streamScheduler.isStreamAlive()
					);
			//this should be false because stream is not alive 
			assertFalse(streamScheduler.isStreamAlive());


			assertEquals(IAntMediaStreamHandler.BROADCAST_STATUS_PREPARING, streamScheduler.getDataStore().get(newCam.getStreamId()).getStatus());


			streamScheduler.stopStream();


			Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> 
			!streamScheduler.isThreadActive());

			assertFalse(streamScheduler.isStreamAlive());

			assertFalse(streamScheduler.isExceptionInThread());

			assertEquals(IAntMediaStreamHandler.BROADCAST_STATUS_FINISHED, streamScheduler.getDataStore().get(newCam.getStreamId()).getStatus());


			logger.info("leaving testStreamSchedularConnectionTimeout");

		}
		catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}


	}

	@Test
	public void testPrepareInput() throws InterruptedException {
		try {

			Broadcast newCam = null;

			new StreamFetcher(newCam.getStreamUrl(), newCam.getStreamId(), newCam.getType(), appScope, null, 0);

			fail("it should throw exception above");
		}
		catch (Exception e) {
		}

		try {
			AVFormatContext inputFormatContext = new AVFormatContext();

			Broadcast newCam2 = new Broadcast("test", "10.2.40.63:8080", "admin", "admin", null, AntMediaApplicationAdapter.IP_CAMERA);
			newCam2.setStreamId("newcam2_" + (int)(Math.random()*10000));

			new StreamFetcher(newCam2.getStreamUrl(), newCam2.getStreamId(), newCam2.getType(), appScope, null, 0);

			fail("it should throw exception above");
		}
		catch (Exception e) {
		}
	}

	@Test
	public void testAddCameraBug() {

		boolean deleteHLSFilesOnExit = getAppSettings().isDeleteHLSFilesOnEnded();

		getAppSettings().setDeleteHLSFilesOnEnded(false);

		DataStore dataStore = new MapDBStore("target/testAddCamera.db", vertx); //applicationContext.getBean(IDataStore.BEAN_NAME);

		assertNotNull(dataStore);
		StreamFetcherManager streamFetcherManager = new StreamFetcherManager(vertx, dataStore, appScope);
		//app.setDataStore(dataStore);

		//set mapdb datastore to stream fetcher because in memory datastore just have references and updating broadcst
		// object updates the reference in inmemorydatastore
		//app.getStreamFetcherManager().setDatastore(dataStore);


		logger.info("running testAddCameraBug");
		Application.enableSourceHealthUpdate = true;
		assertNotNull(dataStore);

		startCameraEmulator();

		Broadcast newCam = new Broadcast("testAddCamera", "127.0.0.1:8080", "admin", "admin", "rtsp://127.0.0.1:6554/test.flv",
				AntMediaApplicationAdapter.IP_CAMERA);

		//add stream to data store
		dataStore.save(newCam);

		//result=getInstance().startStreaming(newCam);
		boolean streamingStarted = streamFetcherManager.startStreaming(newCam).isSuccess();

		//check whether answer from StreamFetcherManager is true or not after new IPCamera is added
		assertTrue(streamingStarted);

		StreamFetcher streamFetcher  = streamFetcherManager.getStreamFetcher(newCam.getStreamId());

		Awaitility.await().atMost(10, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() ->  {
			return streamFetcher.isThreadActive();
		});

		//getInstance().stopStreaming(newCam);
		boolean result = streamFetcherManager.stopStreaming(newCam.getStreamId(), false).isSuccess();
		assertTrue(result);
		stopCameraEmulator();

		streamFetcherManager.stopCheckerJob();

		Awaitility.await().atMost(10, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() ->  {
			return !streamFetcher.isThreadActive();
		});

		getAppSettings().setDeleteHLSFilesOnEnded(deleteHLSFilesOnExit);
		Application.enableSourceHealthUpdate = false;

	}

	@Test
	public void testStartPlaylistThread() {

		BroadcastRestService service = new BroadcastRestService();

		service.setApplication(app);


		boolean deleteHLSFilesOnExit = getAppSettings().isDeleteHLSFilesOnEnded();
		getAppSettings().setDeleteHLSFilesOnEnded(false);

		ApplicationContext context = mock(ApplicationContext.class);
		when(context.getBean(AntMediaApplicationAdapter.BEAN_NAME)).thenReturn(app);

		IStatsCollector statCollector = Mockito.mock(IStatsCollector.class);
		when(statCollector.enoughResource()).thenReturn(true);
		when(context.getBean(IStatsCollector.BEAN_NAME)).thenReturn(statCollector);


		//create a test db
		IDataStoreFactory dsf = (IDataStoreFactory) appScope.getContext().getBean(IDataStoreFactory.BEAN_NAME);

		DataStore dataStore = dsf.getDataStore(); //new InMemoryDataStore("dts");
		assertNotNull(dataStore);
		service.setDataStore(dataStore);
		service.setAppCtx(context);

		app.setDataStore(dataStore);


		//create a stream Manager
		StreamFetcherManager streamFetcherManager = Mockito.spy(new StreamFetcherManager(vertx, dataStore, appScope)); // aaaa
		//app.getAppAdaptor().getStreamFetcherManager();

		app.setStreamFetcherManager(streamFetcherManager);

		//create a broadcast
		PlayListItem broadcastItem1 = new PlayListItem(VALID_MP4_URL, AntMediaApplicationAdapter.VOD);
		broadcastItem1.setDurationInMs(Muxer.getDurationInMs(broadcastItem1.getStreamUrl(), ""));
		logger.info("Duration of the stream: {}", broadcastItem1.getDurationInMs());
		assertTrue(10000 == broadcastItem1.getDurationInMs());

		try {


			//create a broadcast
			PlayListItem broadcastItem2 = new PlayListItem(VALID_MP4_URL, AntMediaApplicationAdapter.VOD);

			//create a broadcast
			PlayListItem broadcastItem3 = new PlayListItem(VALID_MP4_URL, AntMediaApplicationAdapter.VOD);

			//create a broadcast
			PlayListItem broadcastItem4 = new PlayListItem(VALID_MP4_URL, AntMediaApplicationAdapter.VOD);

			List<PlayListItem> broadcastList = new ArrayList<>();

			broadcastList.add(broadcastItem1);
			broadcastList.add(broadcastItem2);
			broadcastList.add(broadcastItem3);

			Broadcast playlist = new Broadcast();
			playlist.setStreamId("testId");
			playlist.setType(AntMediaApplicationAdapter.PLAY_LIST);
			playlist.setPlayListItemList(broadcastList);
			playlist.setStatus(AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING);

			dataStore.save(playlist);

			Result startPlaylist = streamFetcherManager.startPlaylist(playlist);
			assertTrue(startPlaylist.isSuccess());

			{
				//it should return false because it's already streaming
				startPlaylist = streamFetcherManager.startPlaylist(playlist);
				assertFalse(startPlaylist.isSuccess());
			}

			{
				Broadcast playlist2Free = new Broadcast();
				dataStore.save(playlist2Free);
				//it should return false because it's no playlist item
				startPlaylist = streamFetcherManager.startPlaylist(playlist2Free);
				assertFalse(startPlaylist.isSuccess());
			}


			assertNotNull(streamFetcherManager);		

			//check that there is no job related left related with stream fetching

			logger.info("data store: {} testId data {} ", dataStore, dataStore.get("testId"));

			Awaitility.await().atMost(41, TimeUnit.SECONDS).pollDelay(2, TimeUnit.SECONDS)
			.until(() -> dataStore.get("testId").getCurrentPlayIndex() == 2 && dataStore.get("testId").getStatus().equals(AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING));


			boolean result = streamFetcherManager.stopPlayList("testId").isSuccess();
			assertTrue(result);


			String streamId = playlist.getStreamId();
			Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> {
				// Get playlist with DB
				Broadcast tmp = dataStore.get(streamId);
				return AntMediaApplicationAdapter.BROADCAST_STATUS_FINISHED.equals(tmp.getStatus());
			});

			//Get latest status of playlist
			playlist = dataStore.get(streamId);
			assertEquals(AntMediaApplicationAdapter.BROADCAST_STATUS_FINISHED, playlist.getStatus());
			assertEquals(2, playlist.getCurrentPlayIndex());


			// Restore play index
			playlist.setCurrentPlayIndex(0);

			broadcastList.get(0).setStreamUrl(INVALID_MP4_URL);
			broadcastList.get(1).setStreamUrl(INVALID_MP4_URL);
			broadcastList.get(2).setStreamUrl(INVALID_MP4_URL);

			playlist.setPlayListItemList(broadcastList);

			streamFetcherManager.startPlaylist(playlist);	

			assertEquals(AntMediaApplicationAdapter.BROADCAST_STATUS_FINISHED, playlist.getStatus());	
			assertEquals(1, playlist.getCurrentPlayIndex());


			// Restore play index
			playlist.setCurrentPlayIndex(0);

			broadcastList.get(0).setStreamUrl(INVALID_MP4_URL);
			broadcastList.get(1).setStreamUrl(VALID_MP4_URL);
			broadcastList.get(2).setStreamUrl(INVALID_MP4_URL);
			// Valid new broadcast
			broadcastList.add(broadcastItem4);

			playlist.setPlayListItemList(broadcastList);

			Awaitility.await().atMost(10, TimeUnit.SECONDS)
			.until(() -> AntMediaApplicationAdapter.BROADCAST_STATUS_FINISHED.equals(dataStore.get("testId").getStatus()));


			playlist.setPlaylistLoopEnabled(false);
			assertTrue(streamFetcherManager.startPlaylist(playlist).isSuccess());

			Awaitility.await().atMost(10, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS)
			.until(() ->dataStore.get("testId").getCurrentPlayIndex() == 1);

			Awaitility.await().atMost(10, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS)
			.until(() -> AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING.equals(dataStore.get("testId").getStatus()));

			//it should switch to third index - VALID_MP4_URL lenght is 15 seconds
			Awaitility.await().atMost(20, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS)
			.until(() ->dataStore.get("testId").getCurrentPlayIndex() == 3);

			Awaitility.await().atMost(20, TimeUnit.SECONDS)
			.until(() -> AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING.equals(dataStore.get("testId").getStatus()));

			// Playlist will return Finished status and current play index = 0
			Awaitility.await().atMost(20, TimeUnit.SECONDS).pollDelay(15, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
				String status = dataStore.get("testId").getStatus();
				logger.info("Status for testId: {}", status);
				return AntMediaApplicationAdapter.BROADCAST_STATUS_FINISHED.equals(status);
			});

			Awaitility.await().atMost(20, TimeUnit.SECONDS)
			.until(() -> { 
				int index = dataStore.get("testId").getCurrentPlayIndex();
				logger.info("Checking index:{} if zero", index);

				return index == 0;
			});

			Result checked = StreamFetcherManager.checkStreamUrlWithHTTP(INVALID_MP4_URL);

			assertEquals(false, checked.isSuccess());

			checked = StreamFetcherManager.checkStreamUrlWithHTTP(VALID_MP4_URL);

			assertEquals(true, checked.isSuccess());		

			checked = StreamFetcherManager.checkStreamUrlWithHTTP(INVALID_403_MP4_URL);

			assertEquals(false, checked.isSuccess());		


			{
				Result stopPlayList = streamFetcherManager.stopPlayList(null);
				assertFalse(stopPlayList.isSuccess());
			}


			//convert to original settings
			getAppSettings().setDeleteHLSFilesOnEnded(deleteHLSFilesOnExit);
			Application.enableSourceHealthUpdate = false;


		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	@Test
	public void testServerShuttingDownForPlaylist() throws Exception {
		BroadcastRestService service = new BroadcastRestService();

		service.setApplication(app);


		boolean deleteHLSFilesOnExit = getAppSettings().isDeleteHLSFilesOnEnded();
		getAppSettings().setDeleteHLSFilesOnEnded(false);

		ApplicationContext context = mock(ApplicationContext.class);
		when(context.getBean(AntMediaApplicationAdapter.BEAN_NAME)).thenReturn(app);

		IStatsCollector statCollector = Mockito.mock(IStatsCollector.class);
		when(statCollector.enoughResource()).thenReturn(true);
		when(context.getBean(IStatsCollector.BEAN_NAME)).thenReturn(statCollector);


		//create a test db
		IDataStoreFactory dsf = (IDataStoreFactory) appScope.getContext().getBean(IDataStoreFactory.BEAN_NAME);

		DataStore dataStore = dsf.getDataStore(); //new InMemoryDataStore("dts");
		assertNotNull(dataStore);
		service.setDataStore(dataStore);
		service.setAppCtx(context);

		app.setDataStore(dataStore);


		//create a stream Manager
		StreamFetcherManager streamFetcherManager = Mockito.spy(new StreamFetcherManager(vertx, dataStore, appScope)); // aaaa
		//app.getAppAdaptor().getStreamFetcherManager();

		app.setStreamFetcherManager(streamFetcherManager);

		String streamId = "testPlaylistServerShuttingDownForPlaylist" + System.currentTimeMillis(); 



		//create a broadcast
		PlayListItem broadcastItem1 = new PlayListItem(VALID_LONG_DURATION_MP4_URL, AntMediaApplicationAdapter.VOD);

		//create a broadcast
		PlayListItem broadcastItem2 = new PlayListItem(VALID_LONG_DURATION_MP4_URL_2, AntMediaApplicationAdapter.VOD);

		//create a broadcast
		PlayListItem broadcastItem3 = new PlayListItem(VALID_LONG_DURATION_MP4_URL_3, AntMediaApplicationAdapter.VOD);

		List<PlayListItem> broadcastList = new ArrayList<>();

		broadcastList.add(broadcastItem1);
		broadcastList.add(broadcastItem2);
		broadcastList.add(broadcastItem3);

		Broadcast playlist = new Broadcast();
		playlist.setStreamId(streamId);
		playlist.setType(AntMediaApplicationAdapter.PLAY_LIST);
		playlist.setPlayListItemList(broadcastList);
		playlist.setStatus(AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING);

		dataStore.save(playlist);

		{	
			logger.info("Proceeding the last section ");
			Awaitility.await().atMost(5, TimeUnit.SECONDS).until(()-> {
				return !streamFetcherManager.isStreamRunning(playlist);
			});
			Result startPlaylist = streamFetcherManager.startPlaylist(playlist);
			assertTrue(startPlaylist.isSuccess());
			
			Awaitility.await().atMost(5, TimeUnit.SECONDS).until(()-> {
				return streamFetcherManager.isStreamRunning(playlist);
			});
			
			logger.info("--isStreamRunning:{}", streamFetcherManager.isStreamRunning(playlist));
			assertTrue(streamFetcherManager.isStreamRunning(playlist));
			assertNotNull(streamFetcherManager.getStreamFetcher(streamId));

			Awaitility.await().atMost(20, TimeUnit.SECONDS)
			.until(() -> AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING.equals(dataStore.get(streamId).getStatus()));

			streamFetcherManager.shuttingDown();

			logger.info("StreamFetcherManager:{} and sthis streamfetchermanager :{}", app.getStreamFetcherManager(), streamFetcherManager);
			assertNotNull(streamFetcherManager.getStreamFetcher(streamId));
			assertNotNull(app.getStreamFetcherManager().getStreamFetcher(streamId));

			Result result = service.playNextItem(streamId, -1);
			assertFalse(result.isSuccess());
			logger.info("result message:{}", result.getMessage());
			assertTrue(result.getMessage().contains("server is shutting down"));

			Awaitility.await().atMost(20, TimeUnit.SECONDS).until(()-> {
				return !streamFetcherManager.isStreamRunning(playlist);
			});

		}

		
	}

	@Test
	public void testSkipPlaylistItem() throws Exception {

		BroadcastRestService service = new BroadcastRestService();

		service.setApplication(app);


		boolean deleteHLSFilesOnExit = getAppSettings().isDeleteHLSFilesOnEnded();
		getAppSettings().setDeleteHLSFilesOnEnded(false);

		ApplicationContext context = mock(ApplicationContext.class);
		when(context.getBean(AntMediaApplicationAdapter.BEAN_NAME)).thenReturn(app);

		IStatsCollector statCollector = Mockito.mock(IStatsCollector.class);
		when(statCollector.enoughResource()).thenReturn(true);
		when(context.getBean(IStatsCollector.BEAN_NAME)).thenReturn(statCollector);


		//create a test db
		IDataStoreFactory dsf = (IDataStoreFactory) appScope.getContext().getBean(IDataStoreFactory.BEAN_NAME);

		DataStore dataStore = dsf.getDataStore(); //new InMemoryDataStore("dts");
		assertNotNull(dataStore);
		service.setDataStore(dataStore);
		service.setAppCtx(context);

		app.setDataStore(dataStore);


		//create a stream Manager
		StreamFetcherManager streamFetcherManager = Mockito.spy(new StreamFetcherManager(vertx, dataStore, appScope)); // aaaa
		//app.getAppAdaptor().getStreamFetcherManager();

		app.setStreamFetcherManager(streamFetcherManager);

		String streamId = "testPlaylistStreamId";



		//create a broadcast
		PlayListItem broadcastItem1 = new PlayListItem(VALID_LONG_DURATION_MP4_URL, AntMediaApplicationAdapter.VOD);

		//create a broadcast
		PlayListItem broadcastItem2 = new PlayListItem(VALID_LONG_DURATION_MP4_URL_2, AntMediaApplicationAdapter.VOD);

		//create a broadcast
		PlayListItem broadcastItem3 = new PlayListItem(VALID_LONG_DURATION_MP4_URL_3, AntMediaApplicationAdapter.VOD);

		List<PlayListItem> broadcastList = new ArrayList<>();

		broadcastList.add(broadcastItem1);
		broadcastList.add(broadcastItem2);
		broadcastList.add(broadcastItem3);

		Broadcast playlist = new Broadcast();
		playlist.setStreamId(streamId);
		playlist.setType(AntMediaApplicationAdapter.PLAY_LIST);
		playlist.setPlayListItemList(broadcastList);
		playlist.setStatus(AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING);

		dataStore.save(playlist);

		Result startPlaylist = streamFetcherManager.startPlaylist(playlist);
		assertTrue(startPlaylist.isSuccess());

		// Check it currentPlayIndex is 0
		Awaitility.await().atMost(20, TimeUnit.SECONDS)
		.until(() -> { 
			int index = dataStore.get(streamId).getCurrentPlayIndex();
			logger.info("Checking index:{} if zero", index);
			return index == 0;
		});

		Awaitility.await().atMost(20, TimeUnit.SECONDS)
		.until(() -> {
			File f = new File("webapps/junit/streams/testPlaylistStreamId.m3u8");
			return f.exists();
		});


		{
			// It means that it will skip next playlist item
			Result result = service.playNextItem(streamId, null);
			assertTrue(result.isSuccess());

			// Check it currentPlayIndex is 1
			Awaitility.await().atMost(20, TimeUnit.SECONDS)
			.until(() ->dataStore.get(streamId).getCurrentPlayIndex() == 1);

			Awaitility.await().atMost(20, TimeUnit.SECONDS)
			.until(() -> AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING.equals(dataStore.get(streamId).getStatus()));
		}

		{
			// It means that it will skip 100. playlist item. If there is no playlist item, It will result false
			Result result = service.playNextItem(streamId, 100);

			assertFalse(result.isSuccess());

			assertEquals(1, dataStore.get(streamId).getCurrentPlayIndex());

			Awaitility.await().atMost(20, TimeUnit.SECONDS)
			.until(() -> AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING.equals(dataStore.get(streamId).getStatus()));
		}

		{
			// It means that it will play the item in the index 2. playlist item.
			service.playNextItem(streamId, 2);

			// Check it currentPlayIndex is 1
			Awaitility.await().atMost(20, TimeUnit.SECONDS)
			.until(() ->dataStore.get(streamId).getCurrentPlayIndex() == 2);

			Awaitility.await().atMost(20, TimeUnit.SECONDS)
			.until(() -> AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING.equals(dataStore.get(streamId).getStatus()));
		}


		{
			StreamFetcher streamFetcher = streamFetcherManager.getStreamFetcher(streamId);
			assertNotNull(streamFetcher);
			Result stopPlayList = streamFetcherManager.stopPlayList(streamId);
			assertTrue(stopPlayList.isSuccess());

			Awaitility.await().atMost(5, TimeUnit.SECONDS).until(()-> {
				return !streamFetcher.isThreadActive();
			});

		}

		

		//convert to original settings
		getAppSettings().setDeleteHLSFilesOnEnded(deleteHLSFilesOnExit);
		Application.enableSourceHealthUpdate = false;



	}

	@Test
	public void testIsStreamRunning() 
	{
		DataStore dataStore = new InMemoryDataStore("test");
		StreamFetcherManager streamFetcherManager = Mockito.spy(new StreamFetcherManager(vertx, dataStore, appScope));

		Broadcast broadcast = new Broadcast();

		dataStore.save(broadcast);

		boolean isStreamRunning = streamFetcherManager.isStreamRunning(broadcast);
		assertFalse(isStreamRunning);

		broadcast.setStatus(AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING);
		broadcast.setUpdateTime(System.currentTimeMillis());

		isStreamRunning = streamFetcherManager.isStreamRunning(broadcast);
		assertTrue(isStreamRunning);

		broadcast.setOriginAdress("not.accessible.antmedia.io");
		isStreamRunning = streamFetcherManager.isStreamRunning(broadcast);
		assertFalse(isStreamRunning);

		broadcast.setUpdateTime(0);

		isStreamRunning = streamFetcherManager.isStreamRunning(broadcast);
		assertFalse(isStreamRunning);

	}

	@Test
	public void testControlStreamFetchersPlayListAndRestart() {
		DataStore dataStore = Mockito.mock(DataStore.class);
		StreamFetcherManager streamFetcherManager = Mockito.spy(new StreamFetcherManager(vertx, dataStore, appScope));
		Map<String, StreamFetcher> streamFetcherList = new ConcurrentHashMap<>();

		StreamFetcher fetcher = Mockito.mock(StreamFetcher.class);
		String streamId = "stream123456";
		String streamUrl = "streamurl";
		streamFetcherList.put(streamId, fetcher);
		Mockito.when(fetcher.getStreamId()).thenReturn(streamId);
		Mockito.when(fetcher.getStreamUrl()).thenReturn(streamUrl);

		when(fetcher.isStreamAlive()).thenReturn(true);
		when(fetcher.isStreamBlocked()).thenReturn(false);

		Broadcast broadcast = mock(Broadcast.class);
		when(dataStore.get(Mockito.any())).thenReturn(broadcast);
		when(broadcast.getStreamId()).thenReturn(streamId);
		when(broadcast.getStreamUrl()).thenReturn("streamurl");
		when(broadcast.getType()).thenReturn(AntMediaApplicationAdapter.PLAY_LIST);
		when(broadcast.isAutoStartStopEnabled()).thenReturn(false);

		streamFetcherManager.setStreamFetcherList(streamFetcherList);

		streamFetcherManager.controlStreamFetchers(false);
		//it should not call isToBeStoppedAutomatically because type is playlist and autoStartStopEnabled is false
		Mockito.verify(streamFetcherManager, Mockito.never()).isToBeStoppedAutomatically(Mockito.any());



		assertFalse(streamFetcherManager.getStreamFetcherList().isEmpty());
		when(broadcast.getType()).thenReturn(AntMediaApplicationAdapter.STREAM_SOURCE);
		streamFetcherManager.setStreamFetcherList(streamFetcherList);
		Mockito.doReturn(true).when(streamFetcherManager).isStreamRunning(Mockito.any());
		streamFetcherManager.controlStreamFetchers(true);


		ArgumentCaptor<IStreamFetcherListener> listenerCaptor = ArgumentCaptor.forClass(IStreamFetcherListener.class);
		Mockito.verify(fetcher).setStreamFetcherListener(listenerCaptor.capture());
		listenerCaptor.getValue().streamFinished(null);;
		Mockito.verify(streamFetcherManager).startStreaming(broadcast);

	}

	@Test
	public void testControlStreamFetchersPlayListAutoStop() {
		DataStore dataStore = Mockito.mock(DataStore.class);
		StreamFetcherManager streamFetcherManager = Mockito.spy(new StreamFetcherManager(vertx, dataStore, appScope));
		Map<String, StreamFetcher> streamFetcherList = new ConcurrentHashMap<>();

		StreamFetcher fetcher = Mockito.mock(StreamFetcher.class);
		String streamId = "playlistStream123";
		String streamUrl = "streamurl";
		streamFetcherList.put(streamId, fetcher);
		Mockito.when(fetcher.getStreamId()).thenReturn(streamId);
		Mockito.when(fetcher.getStreamUrl()).thenReturn(streamUrl);

		when(fetcher.isStreamAlive()).thenReturn(true);
		when(fetcher.isStreamBlocked()).thenReturn(false);

		Broadcast broadcast = mock(Broadcast.class);
		when(dataStore.get(Mockito.any())).thenReturn(broadcast);
		when(broadcast.getStreamId()).thenReturn(streamId);
		when(broadcast.getStreamUrl()).thenReturn("streamurl");
		when(broadcast.getType()).thenReturn(AntMediaApplicationAdapter.PLAY_LIST);
		when(broadcast.isAutoStartStopEnabled()).thenReturn(true);

		streamFetcherManager.setStreamFetcherList(streamFetcherList);

		// When autoStartStopEnabled is true, isToBeStoppedAutomatically should be called for playlists
		Mockito.doReturn(false).when(streamFetcherManager).isToBeStoppedAutomatically(Mockito.any());
		streamFetcherManager.controlStreamFetchers(false);
		Mockito.verify(streamFetcherManager, Mockito.times(1)).isToBeStoppedAutomatically(broadcast);

		// Reset and test when isToBeStoppedAutomatically returns true - should call stopPlayList
		Mockito.reset(streamFetcherManager);
		streamFetcherManager.setStreamFetcherList(streamFetcherList);
		Mockito.doReturn(true).when(streamFetcherManager).isToBeStoppedAutomatically(Mockito.any());
		Mockito.doReturn(new Result(true)).when(streamFetcherManager).stopPlayList(Mockito.any());

		streamFetcherManager.controlStreamFetchers(false);
		Mockito.verify(streamFetcherManager, Mockito.times(1)).isToBeStoppedAutomatically(broadcast);
		Mockito.verify(streamFetcherManager, Mockito.times(1)).stopPlayList(streamId);
	}
	
	
	@Test
	public void testRestartIsAliveAndNotBlocked() {
		
		DataStore dataStore = Mockito.mock(DataStore.class); 
		StreamFetcherManager streamFetcherManager = Mockito.spy(new StreamFetcherManager(vertx, dataStore, appScope));

		streamFetcherManager.controlStreamFetchers(false);

		Map<String, StreamFetcher> streamFetcherList = new ConcurrentHashMap<>();

		StreamFetcher fetcher = Mockito.mock(StreamFetcher.class);
		String streamId = "stream123456";
		String streamUrl = "streamurl";
		streamFetcherList.put(streamId, fetcher);
		Mockito.when(fetcher.getStreamId()).thenReturn(streamId);
		Mockito.when(fetcher.getStreamUrl()).thenReturn(streamUrl);
		
		Broadcast broadcast = mock(Broadcast.class);
		when(dataStore.get(Mockito.any())).thenReturn(broadcast);
		when(broadcast.getStreamId()).thenReturn(streamId);
		when(broadcast.getStreamUrl()).thenReturn("streamurl");
		
		when(broadcast.getStatus()).thenReturn(AntMediaApplicationAdapter.BROADCAST_STATUS_TERMINATED_UNEXPECTEDLY);


		streamFetcherManager.setStreamFetcherList(streamFetcherList);
		
		when(fetcher.isStreamAlive()).thenReturn(false);
		when(fetcher.isStreamBlocked()).thenReturn(false);
		
		streamFetcherManager.controlStreamFetchers(false);

		verify(fetcher, times(1)).stopStream();
		verify(streamFetcherManager, times(1)).startStreaming(Mockito.any());

	}

	/**
	 * Regression test: a terminated fetcher being evicted must NOT cause healthy fetchers visited
	 * later in the same {@link StreamFetcherManager#controlStreamFetchers(boolean)} pass to be
	 * stopped/restarted. Previously the per-stream eviction mutated the shared {@code restart}
	 * parameter, which leaked onto every subsequent stream in the loop (the "cascade" bug).
	 */
	@Test
	public void testDoNotRestartHealthyStreams() {

		DataStore dataStore = Mockito.mock(DataStore.class);
		StreamFetcherManager streamFetcherManager = Mockito.spy(new StreamFetcherManager(vertx, dataStore, appScope));

		// LinkedHashMap so the terminated stream is iterated FIRST (to reproduce the leak); values() returns a
		// snapshot copy so the in-loop stopStreaming() removal does not throw ConcurrentModification.
		Map<String, StreamFetcher> orderedList = new LinkedHashMap<String, StreamFetcher>() {
			@Override
			public Collection<StreamFetcher> values() {
				return new ArrayList<>(super.values());
			}
		};

		// Terminated: not alive, not blocked, status TERMINATED_UNEXPECTEDLY -> should be evicted+restarted
		String terminatedId = "terminatedStream";
		StreamFetcher terminatedFetcher = Mockito.mock(StreamFetcher.class);
		when(terminatedFetcher.getStreamId()).thenReturn(terminatedId);
		when(terminatedFetcher.isStreamAlive()).thenReturn(false);
		when(terminatedFetcher.isStreamBlocked()).thenReturn(false);
		Broadcast terminatedBroadcast = mock(Broadcast.class);
		when(terminatedBroadcast.getStreamId()).thenReturn(terminatedId);
		when(terminatedBroadcast.getStatus()).thenReturn(AntMediaApplicationAdapter.BROADCAST_STATUS_TERMINATED_UNEXPECTEDLY);

		// Healthy: alive, not blocked -> must be left completely alone
		String healthyId = "healthyStream";
		StreamFetcher healthyFetcher = Mockito.mock(StreamFetcher.class);
		when(healthyFetcher.getStreamId()).thenReturn(healthyId);
		when(healthyFetcher.isStreamAlive()).thenReturn(true);
		when(healthyFetcher.isStreamBlocked()).thenReturn(false);
		Broadcast healthyBroadcast = mock(Broadcast.class);
		when(healthyBroadcast.getStreamId()).thenReturn(healthyId);

		orderedList.put(terminatedId, terminatedFetcher);
		orderedList.put(healthyId, healthyFetcher);

		when(dataStore.get(terminatedId)).thenReturn(terminatedBroadcast);
		when(dataStore.get(healthyId)).thenReturn(healthyBroadcast);

		// isolate the restart-leak behavior from auto-stop and async-teardown paths
		Mockito.doReturn(false).when(streamFetcherManager).isToBeStoppedAutomatically(Mockito.any());
		Mockito.doReturn(false).when(streamFetcherManager).isStreamRunning(Mockito.any());
		Mockito.doReturn(new Result(true)).when(streamFetcherManager).startStreaming(Mockito.any(Broadcast.class));

		streamFetcherManager.setStreamFetcherList(orderedList);

		// not a periodic restart - the only restart should come from the terminated eviction
		streamFetcherManager.controlStreamFetchers(false);

		// terminated is stopped and restarted
		verify(terminatedFetcher, times(1)).stopStream();
		verify(streamFetcherManager, times(1)).startStreaming(terminatedBroadcast);

		// healthy stream is untouched - this is the assertion that fails without the fix
		verify(healthyFetcher, Mockito.never()).stopStream();
		verify(streamFetcherManager, Mockito.never()).startStreaming(healthyBroadcast);
	}


	/**
	 * Integration-style regression test for the duplicate-connection storm.
	 *
	 * Real {@link StreamFetcherManager} driving real {@link StreamFetcher} worker threads. A fetcher
	 * opens its source but then stays in teardown (in production: blocked 60-107s in the S3-backed HLS
	 * header write) - here that single stall is emulated with a latch so the test is deterministic;
	 * everything else (the periodic checker logic, the {@code threadActive} lifecycle, the finished
	 * listener, the concurrent threads) is the real code path.
	 *
	 * Invariant under test: while a worker is still active, the periodic checker must NEVER start a
	 * second fetcher for the same streamId (a second camera connection). Pre-fix every tick spawns a
	 * new fetcher (the storm); with the isThreadActive() guard exactly one connection exists.
	 */
	@Test
	public void testNoDuplicateConnectionWhileWorkerStillActive() {

		DataStore dataStore = Mockito.mock(DataStore.class);
		StreamFetcherManager manager = Mockito.spy(new StreamFetcherManager(vertx, dataStore, appScope));

		String streamId = "stuckCam";
		// stale broadcast -> reads as TERMINATED_UNEXPECTEDLY, which is what triggers the eviction+restart path
		Broadcast broadcast = mock(Broadcast.class);
		when(broadcast.getStreamId()).thenReturn(streamId);
		when(broadcast.getStreamUrl()).thenReturn("rtsp://127.0.0.1:6554/test.flv");
		when(broadcast.getType()).thenReturn(AntMediaApplicationAdapter.IP_CAMERA);
		when(broadcast.getStatus()).thenReturn(AntMediaApplicationAdapter.BROADCAST_STATUS_TERMINATED_UNEXPECTEDLY);
		when(dataStore.get(streamId)).thenReturn(broadcast);

		final CountDownLatch headerWriteBlock = new CountDownLatch(1);
		final List<BlockingFetcher> created = new ArrayList<>();

		// every (re)start flows through make(): hand back a fetcher whose worker opens then stalls
		Mockito.doAnswer(inv -> {
			BlockingFetcher f = new BlockingFetcher(broadcast, appScope, vertx, headerWriteBlock);
			created.add(f);
			return f;
		}).when(manager).make(Mockito.any(), Mockito.any(), Mockito.any());

		try {
			// initial start = one connection
			manager.startStreaming(broadcast);
			assertEquals(1, created.size());

			// several periodic checker ticks while the first worker is still blocked
			for (int i = 0; i < 3; i++) {
				manager.controlStreamFetchers(false);
			}

			// the invariant: no new connection was opened while the previous worker was still active.
			// Pre-fix this is 4 (one per tick + initial); with the fix it stays 1.
			assertEquals("a second connection was opened while the previous worker was still active",
					1, created.size());
			long active = created.stream().filter(StreamFetcher::isThreadActive).count();
			assertEquals(1, active);
		}
		finally {
			// release the stall so the worker thread exits cleanly
			headerWriteBlock.countDown();
		}
	}

	/**
	 * A real StreamFetcher whose worker opens the source and then blocks (emulating the S3-backed
	 * header write) until the latch is released, keeping {@code threadActive} true but never alive.
	 */
	private static class BlockingFetcher extends StreamFetcher {
		private final CountDownLatch block;
		BlockingFetcher(Broadcast b, WebScope scope, Vertx vertx, CountDownLatch block) {
			super(b.getStreamUrl(), b.getStreamId(), b.getType(), scope, vertx, 0);
			this.block = block;
		}
		@Override
		public void startStream() {
			setThreadActive(true); // the real run() sets this before opening the source
			Thread worker = new Thread(() -> {
				try {
					block.await(); // stand-in for the long header write
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
				setThreadActive(false); // worker exits once the (emulated) header write completes
				IStreamFetcherListener listener = getStreamFetcherListener();
				if (listener != null) {
					listener.streamFinished(listener); // real worker fires this on exit -> single restart
				}
			}, "blocking-fetcher-" + getStreamId());
			worker.setDaemon(true);
			worker.start();
		}
		// async stop request only; a native open can't be interrupted, so the thread stays active until the latch
		@Override
		public void stopStream() { /* no-op for the test */ }
		@Override
		public boolean isStreamAlive() { return false; }   // no packets are read while blocked
		@Override
		public boolean isStreamBlocked() { return false; }
	}

	@Test
	public void testControlStreamFetchers() {
		//create a test db
		DataStore dataStore = Mockito.mock(DataStore.class); 
		StreamFetcherManager streamFetcherManager = Mockito.spy(new StreamFetcherManager(vertx, dataStore, appScope));

		streamFetcherManager.controlStreamFetchers(false);

		Map<String, StreamFetcher> streamFetcherList = new ConcurrentHashMap<>();

		StreamFetcher fetcher = Mockito.mock(StreamFetcher.class);
		String streamId = "stream123456";
		String streamUrl = "streamurl";
		streamFetcherList.put(streamId, fetcher);
		Mockito.when(fetcher.getStreamId()).thenReturn(streamId);
		Mockito.when(fetcher.getStreamUrl()).thenReturn(streamUrl);


		streamFetcherManager.setStreamFetcherList(streamFetcherList);

		streamFetcherManager.controlStreamFetchers(false);
		//because broadcast is null
		verify(fetcher, times(1)).stopStream();


		assertEquals(0, streamFetcherManager.getStreamFetcherList().size());
		streamFetcherList.put(streamId, fetcher);


		streamFetcherManager.controlStreamFetchers(true);
		//broadcast is null so stop stream will be called
		verify(fetcher, times(2)).stopStream();
		//it will not called because broadcast is null
		verify(fetcher, times(0)).startStream();
		verify(streamFetcherManager, times(0)).startStreaming(Mockito.any());

		assertEquals(0, streamFetcherManager.getStreamFetcherList().size());
		streamFetcherList.put(streamId, fetcher);


		Broadcast broadcast = mock(Broadcast.class);
		when(dataStore.get(Mockito.any())).thenReturn(broadcast);
		when(broadcast.getStreamId()).thenReturn(streamId);
		when(broadcast.getStreamUrl()).thenReturn("streamurl");
		
		when(fetcher.isStreamAlive()).thenReturn(true);
		when(fetcher.isStreamBlocked()).thenReturn(false);

		streamFetcherManager.controlStreamFetchers(false);
		//it will not change above stream is alive and broadcast is not null
		verify(fetcher, times(2)).stopStream();
		verify(fetcher, times(0)).startStream();
		verify(streamFetcherManager, times(0)).startStreaming(Mockito.any());


		when(broadcast.isAutoStartStopEnabled()).thenReturn(true);
		when(broadcast.isAnyoneWatching()).thenReturn(true);
		streamFetcherManager.controlStreamFetchers(false);
		//it will not change above stream is alive and broadcast is not null and someone is watching
		verify(fetcher, times(2)).stopStream();
		verify(fetcher, times(0)).startStream();
		verify(streamFetcherManager, times(0)).startStreaming(Mockito.any());



		when(broadcast.isAutoStartStopEnabled()).thenReturn(true);
		when(broadcast.isAnyoneWatching()).thenReturn(false);
		streamFetcherManager.controlStreamFetchers(false);
		//it will not change above because it does not passed enough time
		verify(fetcher, times(2)).stopStream();
		verify(fetcher, times(0)).startStream();
		verify(streamFetcherManager, times(0)).startStreaming(Mockito.any());


		when(broadcast.isAutoStartStopEnabled()).thenReturn(true);
		when(broadcast.isAnyoneWatching()).thenReturn(false);
		when(broadcast.getStartTime()).thenReturn(1l);
		streamFetcherManager.controlStreamFetchers(false);
		//it will not change above because it has passed enough time
		verify(fetcher, times(3)).stopStream();
		verify(fetcher, times(0)).startStream();
		verify(streamFetcherManager, times(0)).startStreaming(Mockito.any());

		assertEquals(0, streamFetcherManager.getStreamFetcherList().size());
		streamFetcherList.put(streamId, fetcher);


		when(broadcast.isAutoStartStopEnabled()).thenReturn(false);
		when(broadcast.isAnyoneWatching()).thenReturn(false);
		streamFetcherManager.controlStreamFetchers(false);
		//it will not change above stream is alive and broadcast is not null and isAutoStartStopEnabled false
		verify(fetcher, times(3)).stopStream();
		verify(fetcher, times(0)).startStream();
		verify(streamFetcherManager, times(0)).startStreaming(Mockito.any());


		when(broadcast.isAutoStartStopEnabled()).thenReturn(false);
		when(broadcast.isAnyoneWatching()).thenReturn(true);
		streamFetcherManager.controlStreamFetchers(false);
		//it will not change above stream is alive and broadcast is not null and  isAutoStartStopEnabled false
		verify(fetcher, times(3)).stopStream();
		verify(fetcher, times(0)).startStream();
		verify(streamFetcherManager, times(0)).startStreaming(Mockito.any());


		streamFetcherManager.controlStreamFetchers(true);
		//it willl not change because restart is true
		verify(fetcher, times(4)).stopStream();
		verify(streamFetcherManager, times(1)).startStreaming(Mockito.any());	
		
		streamFetcherManager.stopStreaming(streamId, false);



	}



	@Test
	public void testStopFetchingWhenDeleted() {

		BroadcastRestService service = new BroadcastRestService();

		ApplicationContext context = mock(ApplicationContext.class);
		service.setAppCtx(context);
		when(context.containsBean(Mockito.any())).thenReturn(false);

		service.setApplication(app);

		boolean deleteHLSFilesOnExit = getAppSettings().isDeleteHLSFilesOnEnded();

		getAppSettings().setDeleteHLSFilesOnEnded(false);

		//create a test db
		DataStore dataStore = new MapDBStore("target/testDelete.db", vertx); 
		service.setDataStore(dataStore);

		//create a stream fetcher
		StreamFetcherManager streamFetcherManager = new StreamFetcherManager(vertx, dataStore, appScope);

		app.setStreamFetcherManager(streamFetcherManager);


		Application.enableSourceHealthUpdate = true;

		assertNotNull(dataStore);

		//start emulator
		startCameraEmulator();

		Broadcast newCam = new Broadcast("testStopCamera", "127.0.0.1:8080", "admin", "admin", "rtsp://127.0.0.1:6554/test.flv",
				AntMediaApplicationAdapter.IP_CAMERA);

		//add stream to data store
		dataStore.save(newCam);

		boolean streamingStarted = streamFetcherManager.startStreaming(newCam).isSuccess();
		assertTrue(streamingStarted);

		//check whether answer from StreamFetcherManager is true or not after new IPCamera is added
		StreamFetcher streamFetcher  = streamFetcherManager.getStreamFetcher(newCam.getStreamId());
		assertNotNull(streamFetcher);

		Awaitility.await().atMost(10, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() ->  {
			return streamFetcher.isThreadActive();
		});



		//just delete broadcast instead of calling stop
		Result result = service.deleteBroadcast(newCam.getStreamId(), false);
		assertTrue(result.isSuccess());

		//stop emulator
		stopCameraEmulator();

		streamFetcherManager.stopCheckerJob();

		//check that fetcher is nor running
		Awaitility.await().atMost(10, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() ->  {
			return !streamFetcher.isThreadActive();
		});


		//convert to original settings
		getAppSettings().setDeleteHLSFilesOnEnded(deleteHLSFilesOnExit);
		Application.enableSourceHealthUpdate = false;

	}


	@Test
	public void testStopFetchingWhenStopCalled() {


		BroadcastRestService service = new BroadcastRestService();

		service.setApplication(app);

		boolean deleteHLSFilesOnExit = getAppSettings().isDeleteHLSFilesOnEnded();

		getAppSettings().setDeleteHLSFilesOnEnded(false);

		//create a test db
		DataStore dataStore = new MapDBStore("target/testStop.db", vertx); 
		service.setDataStore(dataStore);

		//create a stream fetcher
		StreamFetcherManager streamFetcherManager = new StreamFetcherManager(vertx, dataStore, appScope);

		app.setStreamFetcherManager(streamFetcherManager);


		Application.enableSourceHealthUpdate = true;

		assertNotNull(dataStore);

		//start emulator
		startCameraEmulator();

		Broadcast newCam = new Broadcast("testStopCamera", "127.0.0.1:8080", "admin", "admin", "rtsp://127.0.0.1:6554/test.flv",
				AntMediaApplicationAdapter.IP_CAMERA);

		//add stream to data store
		dataStore.save(newCam);


		//result=getInstance().startStreaming(newCam);
		boolean streamingStarted = streamFetcherManager.startStreaming(newCam).isSuccess();

		//check whether answer from StreamFetcherManager is true or not after new IPCamera is added
		assertTrue(streamingStarted);

		StreamFetcher streamFetcher  = streamFetcherManager.getStreamFetcher(newCam.getStreamId());

		//check whether answer from StreamFetcherManager is true or not after new IPCamera is added
		assertNotNull(streamFetcher);

		Awaitility.await().atMost(10, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() ->  {
			return streamFetcher.isThreadActive();
		});

		//just delete broadcast instead of calling stop
		Result result = service.stopStreaming(newCam.getStreamId(), false, null);

		assertTrue(result.isSuccess());
		//stop emulator
		stopCameraEmulator();

		streamFetcherManager.stopCheckerJob();

		//check that fetcher is nor running
		Awaitility.await().atMost(10, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() ->  {
			return !streamFetcher.isThreadActive();
		});

		//convert to original settings
		getAppSettings().setDeleteHLSFilesOnEnded(deleteHLSFilesOnExit);
		Application.enableSourceHealthUpdate = false;

	}


	public void testIPTVStream() {

		AVFormatContext inputFormatContext = avformat_alloc_context();
		int ret;
		String url = "http://kaptaniptv.com:8000/live/oguzmermer2/jNwNLK1VLk/10476.ts";

		AVDictionary optionsDictionary = new AVDictionary();


		if ((ret = avformat_open_input(inputFormatContext, url, null, optionsDictionary)) < 0) {

			byte[] data = new byte[1024];
			avutil.av_strerror(ret, data, data.length);
			logger.error("cannot open input context with error: {} ret value = {}",
					FFmpegUtilities.byteArrayToString(data), ret);
			return;
		}

		ret = avformat_find_stream_info(inputFormatContext, (AVDictionary) null);
		if (ret < 0) {
			logger.error("Could not find stream information\n");
			return;
		}

		AVPacket pkt = new AVPacket();

		long startTime = System.currentTimeMillis();

		int i = 0;
		while (true) {
			ret = av_read_frame(inputFormatContext, pkt);
			if (ret < 0) {
				byte[] data = new byte[1024];
				avutil.av_strerror(ret, data, data.length);

				logger.error("cannot read frame from input context: {}",  FFmpegUtilities.byteArrayToString(data));
			}

			av_packet_unref(pkt);
			i++;
			if (i % 150 == 0) {
				long duration = System.currentTimeMillis() - startTime;

				logger.info("running duration: " + (duration/1000));
			}
		}
		/*
		long duration = System.currentTimeMillis() - startTime;

		logger.info("total duration: " + (duration/1000));
		avformat_close_input(inputFormatContext);
		inputFormatContext = null;

		 */

	}

	/*
	 * This test code may not run on local instance. Because, it includes commands having "sudo" pieces and waits reply 
	 * for them. Therefore it may not proceed. It is configured for travis CI/CD tool which can run sudo commands 
	 * automatically.
	 * 
	 */

	//@Test
	public void testBandwidth() {

		//This test is moved to {@link @MuxerUnitTest#testStreamSpeed} because it uses wondershaper and there is some kind of incompatibility with wondershaper and
		//new versions

	}

	private void runShellCommand(String[] params) {
		try {
			logger.info("Running runShellCommand");

			Process procStop = new ProcessBuilder(params).start();

			InputStream inputStream = procStop.getInputStream();
			byte[] data = new byte[1024];
			int length;
			while ((length = inputStream.read(data, 0, data.length)) > 0) {
				System.out.println(new String(data, 0, length));
			}

			inputStream = procStop.getErrorStream();
			while ((length = inputStream.read(data, 0, data.length)) > 0) {
				System.out.println(new String(data, 0, length));
			}

			procStop.waitFor();

		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private int resetNetworkInterface(String activeInterface) {
		logger.info("Running resetNetworkInterface");

		String command = "sudo wondershaper " + activeInterface + " clear";
		return runCommand(command);
	}


	private int limitNetworkInterfaceBandwidth(String activeInterface) {

		logger.info("Running limitNetworkInterfaceBandwidth");
		logger.info("active interface {}", activeInterface);


		//Delete root qdisc - ignore the result
		String command = "sudo wondershaper " + activeInterface + " 40 40";
		// ignore the result
		return runCommand(command);

	}



	public int runCommand(String command) {
		String[] argsStop = new String[] { "/bin/bash", "-c", command };

		try {
			logger.info("Running runCommand: {}", command);

			Process procStop = new ProcessBuilder(argsStop).start();

			InputStream inputStream = procStop.getInputStream();
			byte[] data = new byte[1024];
			int length;
			if (inputStream != null) {
				while ((length = inputStream.read(data, 0, data.length)) > 0) {
					System.out.println(new String(data, 0, length));
				}

				inputStream = procStop.getErrorStream();
				while ((length = inputStream.read(data, 0, data.length)) > 0) {
					System.out.println(new String(data, 0, length));
				}

				return procStop.waitFor();
			}

		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		return -1;

	}

	public String findActiveInterface() {

		String activeInterface = null;


		String[] argsStop = new String[] { "/bin/bash", "-c",
		"ip addr | awk '/LOOPBACK/ {print $2}' | sed 's/.$//'" };

		try {
			logger.info("Running findActiveInterface");

			Process procStop = new ProcessBuilder(argsStop).start();

			InputStream inputStream = procStop.getInputStream();
			byte[] data = new byte[1024];
			int length;
			while ((length = inputStream.read(data, 0, data.length)) > 0) {
				System.out.println(new String(data, 0, length));
				activeInterface = new String(data, 0, length);
			}

			inputStream = procStop.getErrorStream();
			while ((length = inputStream.read(data, 0, data.length)) > 0) {
				System.out.println(new String(data, 0, length));
			}

			procStop.waitFor();

		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}


		return activeInterface.substring(0, activeInterface.length()-1);
	}




	public AntMediaApplicationAdapter getInstance() {
		if (appInstance == null) {
			appInstance = (AntMediaApplicationAdapter) applicationContext.getBean("web.handler");
		}
		return appInstance;
	}

	private void startCameraEmulator() {
		stopCameraEmulator();

		ProcessBuilder pb = new ProcessBuilder("/usr/local/onvif/runme.sh");
		Process p = null;
		try {
			p = pb.start();
			while (!p.isAlive()) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			//wait here to let the emulator get ready
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		}

	}


	private void stopCameraEmulator() {
		// close emulator in order to simulate cut-off
		String[] argsStop = new String[] { "/bin/bash", "-c",
		"kill -9 $(ps aux | grep 'onvifser' | awk '{print $2}')" };
		String[] argsStop2 = new String[] { "/bin/bash", "-c",
		"kill -9 $(ps aux | grep 'rtspserve' | awk '{print $2}')" };
		try {
			Process procStop = new ProcessBuilder(argsStop).start();
			Process procStop2 = new ProcessBuilder(argsStop2).start();


		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public AppSettings getAppSettings() {
		if (appSettings == null) {
			appSettings = (AppSettings) applicationContext.getBean(AppSettings.BEAN_NAME);
		}
		return appSettings;
	}

	@Test
	public void testBroadcastStatusForStreamSource() 
	{
		startCameraEmulator();
		try (AVFormatContext inputFormatContext = new AVFormatContext()) {

			String existingStreamSource = "existingStreamSource"+RandomUtils.nextInt();
			Broadcast existingBroadcast = new Broadcast(existingStreamSource, "10.2.40.63:8080", "admin", "admin", 
					"rtsp://127.0.0.1:6554/test.flv",
					AntMediaApplicationAdapter.STREAM_SOURCE);


			existingBroadcast.setStreamId(existingStreamSource);

			DataStore dataStore = app.getDataStore();
			dataStore.save(existingBroadcast);

			StreamFetcherManager fetcherManager = new StreamFetcherManager(vertx, dataStore, appScope);

			/*
			Result startStreaming = fetcherManager.startStreaming(existingBroadcast);
			assertTrue(startStreaming.isSuccess());

			Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> 
			{
				return dataStore.get(existingStreamSource).getStatus() == AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING;
			});

			startStreaming = fetcherManager.startStreaming(existingBroadcast);
			//it should be false because it's already fetching
			assertFalse(startStreaming.isSuccess());

			Result stopStreaming = fetcherManager.stopStreaming(existingBroadcast.getStreamId());
			assertTrue(stopStreaming.isSuccess());
			stopStreaming = fetcherManager.stopStreaming(existingBroadcast.getStreamId());
			assertFalse(stopStreaming.isSuccess());

			Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> {
				return fetcherManager.getStreamFetcherList().size() == 0;
			});

			Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> 
			{
				return dataStore.get(existingStreamSource).getStatus() == AntMediaApplicationAdapter.BROADCAST_STATUS_FINISHED;
			});
			 */


			//non existing url

			String nonExistingStreamSource = "nonExistingStreamSource"+RandomUtils.nextInt();
			Broadcast nonExistingBroadcast = new Broadcast(nonExistingStreamSource, "10.2.40.63:8080", "admin", "admin", 
					"rtsp://127.0.0.1:6554/fakeurl.flv",
					AntMediaApplicationAdapter.STREAM_SOURCE);

			nonExistingBroadcast.setStreamId(nonExistingStreamSource);
			dataStore.save(nonExistingBroadcast);

			Result startStreaming2 = fetcherManager.startStreaming(nonExistingBroadcast);
			assertTrue(startStreaming2.isSuccess());

			startStreaming2 = fetcherManager.startStreaming(nonExistingBroadcast);
			assertFalse(startStreaming2.isSuccess());

			Awaitility.await().pollDelay(5, TimeUnit.SECONDS).until(() -> {
				return !AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING.equals(dataStore.get(nonExistingStreamSource).getStatus());
			});

			StreamFetcher streamFetcher = fetcherManager.getStreamFetcher(nonExistingStreamSource);

			Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> {
				return !streamFetcher.isStreamAlive();
			});


			Result stopStreaming2 = fetcherManager.stopStreaming(nonExistingStreamSource, false);
			assertTrue(stopStreaming2.isSuccess());
			stopStreaming2 = fetcherManager.stopStreaming(nonExistingStreamSource, false);
			assertFalse(stopStreaming2.isSuccess());

			Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> {
				return fetcherManager.getStreamFetcherList().size() == 0;
			});

		}
		catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		stopCameraEmulator();


	}

}


