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
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
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
import io.antmedia.datastore.db.DataStore;
import io.antmedia.datastore.db.IDataStoreFactory;
import io.antmedia.datastore.db.InMemoryDataStore;
import io.antmedia.datastore.db.MapDBStore;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.datastore.db.types.Broadcast.PlayListItem;
import io.antmedia.integration.AppFunctionalV2Test;
import io.antmedia.muxer.MuxAdaptor;
import io.antmedia.rest.BroadcastRestService;
import io.antmedia.rest.model.Result;
import io.antmedia.statistic.IStatsCollector;
import io.antmedia.streamsource.StreamFetcher;
import io.antmedia.streamsource.StreamFetcherManager;
import io.vertx.core.Vertx;

@ContextConfiguration(locations = { "test.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class StreamSchedularUnitTest extends AbstractJUnit4SpringContextTests {

	public Application app = null;
	public static String VALID_MP4_URL = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerMeltdowns.mp4";
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

			StreamFetcher camScheduler = new StreamFetcher(newCam.getStreamUrl(), newCam.getStreamId(), newCam.getType(), appScope, null);

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

			StreamFetcher streamScheduler = new StreamFetcher(newCam.getStreamUrl(), newCam.getStreamId(), newCam.getType(), appScope, vertx);

			assertFalse(streamScheduler.isExceptionInThread());

			streamScheduler.startStream();

			streamScheduler.setConnectionTimeout(2000);


			Awaitility.await().pollDelay(3, TimeUnit.SECONDS).until(() -> 
			!streamScheduler.isStreamAlive()
					);
			//this should be false because stream is not alive 
			assertFalse(streamScheduler.isStreamAlive());

			streamScheduler.stopStream();


			Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> 
			!streamScheduler.isThreadActive());

			assertFalse(streamScheduler.isStreamAlive());

			assertFalse(streamScheduler.isExceptionInThread());

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

			new StreamFetcher(newCam.getStreamUrl(), newCam.getStreamId(), newCam.getType(), appScope, null);

			fail("it should throw exception above");
		}
		catch (Exception e) {
		}

		try {
			AVFormatContext inputFormatContext = new AVFormatContext();

			Broadcast newCam2 = new Broadcast("test", "10.2.40.63:8080", "admin", "admin", null, AntMediaApplicationAdapter.IP_CAMERA);
			newCam2.setStreamId("newcam2_" + (int)(Math.random()*10000));

			new StreamFetcher(newCam2.getStreamUrl(), newCam2.getStreamId(), newCam2.getType(), appScope, null);

			fail("it should throw exception above");
		}
		catch (Exception e) {
		}
	}

	@Test
	public void testcheckStreamFetchersStatus() {
		DataStore dataStore = new InMemoryDataStore("target/testAddCamera.db"); //applicationContext.getBean(IDataStore.BEAN_NAME);

		assertNotNull(dataStore);
		StreamFetcherManager streamFetcherManager = new StreamFetcherManager(vertx, dataStore, appScope);

		StreamFetcher streamFetcher = Mockito.mock(StreamFetcher.class);
		Mockito.when(streamFetcher.getStreamId()).thenReturn("streamId");
		Mockito.when(streamFetcher.isStreamAlive()).thenReturn(true);


		streamFetcherManager.getStreamFetcherList().add(streamFetcher);


		streamFetcherManager.checkStreamFetchersStatus();


		Mockito.when(streamFetcher.isStreamAlive()).thenReturn(false);
		MuxAdaptor muxAdaptor = Mockito.mock(MuxAdaptor.class);

		Mockito.when(streamFetcher.getMuxAdaptor()).thenReturn(muxAdaptor);
		streamFetcherManager.checkStreamFetchersStatus();
		Mockito.verify(muxAdaptor).updateStreamQualityParameters("streamId", null, 0.01d, 0);

		Mockito.when(streamFetcher.getMuxAdaptor()).thenReturn(null);
		streamFetcherManager.checkStreamFetchersStatus();
		Mockito.verify(muxAdaptor, Mockito.times(1)).updateStreamQualityParameters("streamId", null, 0.01d, 0);


		Mockito.when(streamFetcher.getStreamId()).thenReturn(null);
		Mockito.when(streamFetcher.getMuxAdaptor()).thenReturn(muxAdaptor);
		streamFetcherManager.checkStreamFetchersStatus();
		Mockito.verify(muxAdaptor, Mockito.times(1)).updateStreamQualityParameters("streamId", null, 0.01d, 0);


		streamFetcherManager.setDatastore(null);
		streamFetcherManager.checkStreamFetchersStatus();
		Mockito.verify(muxAdaptor, Mockito.times(1)).updateStreamQualityParameters("streamId", null, 0.01d, 0);




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
		boolean result = streamFetcherManager.stopStreaming(newCam.getStreamId()).isSuccess();
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

			System.out.println("data store: " + dataStore + " testId data ->" + dataStore.get("testId"));

			Awaitility.await().atMost(40, TimeUnit.SECONDS).pollDelay(2, TimeUnit.SECONDS)
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

			Awaitility.await().atMost(10, TimeUnit.SECONDS)
			.until(() ->dataStore.get("testId").getCurrentPlayIndex() == 1);

			Awaitility.await().atMost(10, TimeUnit.SECONDS)
			.until(() -> AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING.equals(dataStore.get("testId").getStatus()));

			
			Awaitility.await().atMost(20, TimeUnit.SECONDS).until(() -> {
				return AntMediaApplicationAdapter.BROADCAST_STATUS_FINISHED.equals(dataStore.get("testId").getStatus());
			});
			
			Awaitility.await().atMost(20, TimeUnit.SECONDS)
			.until(() ->dataStore.get("testId").getCurrentPlayIndex() == 3);
			
			Awaitility.await().atMost(20, TimeUnit.SECONDS)
			.until(() -> AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING.equals(dataStore.get("testId").getStatus()));
			
			
			// Playlist will return Finished status and current play index = 0
			
			Awaitility.await().atMost(30, TimeUnit.SECONDS).until(() -> {
				return AntMediaApplicationAdapter.BROADCAST_STATUS_FINISHED.equals(dataStore.get("testId").getStatus());
			});
			
			Awaitility.await().atMost(30, TimeUnit.SECONDS).until(() -> {
				return AntMediaApplicationAdapter.BROADCAST_STATUS_FINISHED.equals(dataStore.get("testId").getPlayListStatus());
			});
			
			Awaitility.await().atMost(20, TimeUnit.SECONDS)
			.until(() ->dataStore.get("testId").getCurrentPlayIndex() == 0);

			// Update playlist with DB

	//		playlist = dataStore.get(playlist.getStreamId());
	//		assertEquals(AntMediaApplicationAdapter.BROADCAST_STATUS_FINISHED, playlist.getPlayListStatus());

			//Check Stream URL function

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
		Result result = service.deleteBroadcast(newCam.getStreamId());
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
		Result result = service.stopStreaming(newCam.getStreamId());

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
			logger.error("cannot open input context with error: " + new String(data, 0, data.length) + "ret value = "+ String.valueOf(ret));
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

				logger.error("cannot read frame from input context: " + new String(data, 0, data.length));	
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
	@Test
	public void testBandwidth() {

		boolean deleteHLSFilesOnExit = getAppSettings().isDeleteHLSFilesOnEnded();
		getAppSettings().setDeleteHLSFilesOnEnded(false);

		File f = new File("target/test.db");
		if (f.exists()) {
			try {
				Files.delete(f.toPath());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		DataStore dataStore = app.getDataStore(); //new MapDBStore("target/test.db"); //applicationContext.getBean(IDataStore.BEAN_NAME);

		//assertNotNull(dataStore);

		//DataStoreFactory dsf = Mockito.mock(DataStoreFactory.class);
		//Mockito.when(dsf.getDataStore()).thenReturn(dataStore);
		//app.setDataStoreFactory(dsf);

		//set mapdb datastore to stream fetcher because in memory datastore just have references and updating broadcst
		// object updates the reference in inmemorydatastore
		app.getStreamFetcherManager().setDatastore(dataStore);


		logger.info("running testBandwidth");
		Application.enableSourceHealthUpdate = true;
		assertNotNull(dataStore);

		startCameraEmulator();

		Broadcast newSource = new Broadcast("testBandwidth", "10.2.40.63:8080", "admin", "admin", 
				"rtsp://127.0.0.1:6554/test.flv",
				AntMediaApplicationAdapter.STREAM_SOURCE);

		//add stream to data store
		dataStore.save(newSource);

		Broadcast newZombiSource = new Broadcast("testBandwidth", "10.2.40.63:8080", "admin", "admin", 
				"rtsp://127.0.0.1:6554/test.flv",
				AntMediaApplicationAdapter.STREAM_SOURCE);

		newZombiSource.setZombi(true);
		//add second stream to datastore
		dataStore.save(newZombiSource);


		List<Broadcast> streams = new ArrayList<>();

		streams.add(newSource);
		streams.add(newZombiSource);

		//let stream fetching start
		app.getStreamFetcherManager().testSetStreamCheckerInterval(5000);
		//do not restart if it fails
		app.getStreamFetcherManager().setRestartStreamAutomatically(false);
		app.getStreamFetcherManager().startStreams(streams);



		Awaitility.await().atMost(12, TimeUnit.SECONDS).until(() -> {
			return dataStore.get(newZombiSource.getStreamId()).getSpeed() != 0;
		});

		logger.info("before first control");

		List<Broadcast> broadcastList =  dataStore.getBroadcastList(0,  20, null, null, null, null);

		Broadcast fetchedBroadcast = null;

		for (Broadcast broadcast : broadcastList) {

			logger.info("broadcast name: " + broadcast.getName() + " broadcast status :" + broadcast.getStatus() + " broadcast is zombi: " + broadcast.isZombi());
			if(broadcast.isZombi()) {

				fetchedBroadcast=broadcast;	
				break;
			}
		}

		assertNotNull(fetchedBroadcast);
		assertEquals(fetchedBroadcast.getStreamId(), newZombiSource.getStreamId());
		assertNotNull(fetchedBroadcast.getSpeed());


		Awaitility.await().atMost(20, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
			Broadcast stream = dataStore.get(newSource.getStreamId());
			logger.info("speed {} stream id: {}" , stream.getSpeed(), stream.getStreamId()) ;
			return stream != null && Math.abs(stream.getSpeed()-1) < 0.2;
		});



		limitNetworkInterfaceBandwidth(findActiveInterface());

		logger.info("Checking quality is again");

		Awaitility.await().atMost(30, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
			Broadcast streamTmp = dataStore.get(newSource.getStreamId());
			logger.info("speed {}" , streamTmp.getSpeed()) ;
			logger.info("quality {}" , streamTmp.getQuality()) ;

			return streamTmp != null && streamTmp.getSpeed() < 0.7;
			// the critical thing is the speed which less that 0.7
		});

		resetNetworkInterface(findActiveInterface());

		for (Broadcast broadcast: broadcastList) {
			app.getStreamFetcherManager().stopStreaming(broadcast.getStreamId());
		}

		Awaitility.await().atMost(5, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
			return app.getStreamFetcherManager().getStreamFetcherList().size() == 0;
		});

		//list size should be zero
		//assertEquals(0, app.getStreamFetcherManager().getStreamFetcherList().size());
		logger.info("leaving testBandwidth");

		Application.enableSourceHealthUpdate = false;

		getAppSettings().setDeleteHLSFilesOnEnded(deleteHLSFilesOnExit);

		stopCameraEmulator()	;	

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

	private void resetNetworkInterface(String activeInterface) {
		logger.info("Running resetNetworkInterface");

		runCommand("sudo wondershaper clear "+activeInterface);

	}

	private void limitNetworkInterfaceBandwidth(String activeInterface) {

		logger.info("Running limitNetworkInterfaceBandwidth");
		logger.info("active interface {}", activeInterface);

		String command = "sudo wondershaper "+activeInterface+" 20 20";
		logger.info("command : {}",command);
		runCommand(command);

		logger.info("Exiting limitNetworkInterfaceBandwidth");

	}

	public void runCommand(String command) {
		String[] argsStop = new String[] { "/bin/bash", "-c", command };

		try {
			logger.info("Running runCommand");

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

				procStop.waitFor();
			}

		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

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


			Result stopStreaming2 = fetcherManager.stopStreaming(nonExistingStreamSource);
			assertTrue(stopStreaming2.isSuccess());
			stopStreaming2 = fetcherManager.stopStreaming(nonExistingStreamSource);
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


