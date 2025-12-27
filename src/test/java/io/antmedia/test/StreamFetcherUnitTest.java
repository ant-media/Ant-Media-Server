package io.antmedia.test;

import static org.bytedeco.ffmpeg.global.avformat.avformat_find_stream_info;
import static org.bytedeco.ffmpeg.global.avformat.avformat_open_input;
import static org.bytedeco.ffmpeg.global.avutil.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.awaitility.Awaitility;
import org.bytedeco.ffmpeg.avcodec.AVCodecParameters;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.avformat.AVInputFormat;
import org.bytedeco.ffmpeg.avformat.AVStream;
import org.bytedeco.ffmpeg.avutil.AVDictionary;
import org.bytedeco.ffmpeg.avutil.AVDictionaryEntry;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avformat;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacpp.BytePointer;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.mockito.Mockito;
import org.red5.server.api.scope.IScope;
import org.red5.server.scope.WebScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.AppSettings;
import io.antmedia.RecordType;
import io.antmedia.datastore.db.DataStore;
import io.antmedia.datastore.db.DataStoreFactory;
import io.antmedia.datastore.db.InMemoryDataStore;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.integration.AppFunctionalV2Test;
import io.antmedia.integration.MuxingTest;
import io.antmedia.ipcamera.OnvifCamera;
import io.antmedia.muxer.Mp4Muxer;
import io.antmedia.muxer.MuxAdaptor;
import io.antmedia.rest.model.Result;
import io.antmedia.statistic.StatsCollector;
import io.antmedia.streamsource.StreamFetcher;
import io.antmedia.streamsource.StreamFetcher.WorkerThread;
import io.antmedia.streamsource.StreamFetcherManager;
import io.vertx.core.Vertx;

@ContextConfiguration(locations = { "test.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
public class StreamFetcherUnitTest extends AbstractJUnit4SpringContextTests {

	private WebScope appScope;
	protected static Logger logger = LoggerFactory.getLogger(StreamFetcherUnitTest.class);
	public AntMediaApplicationAdapter app = null;
	private AntMediaApplicationAdapter appInstance;
	private AppSettings appSettings;
	private Vertx vertx;

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
			System.out.println("Failed test: " + description.getMethodName() );
			e.printStackTrace();
		}
		protected void finished(Description description) {
			System.out.println("Finishing test: " + description.getMethodName());
		}
	};

	@BeforeClass
	public static void beforeClass() {
		//	avformat.av_register_all();
		avformat.avformat_network_init();
		avutil.av_log_set_level(avutil.AV_LOG_INFO);
	}

	@Before
	public void before() {

		try {
			AppFunctionalV2Test.delete(new File("webapps/junit/streams"));
		} catch (IOException e) {
			e.printStackTrace();
		}


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

		if (app == null)
		{
			app = (AntMediaApplicationAdapter) applicationContext.getBean("web.handler");
			logger.debug("Application / web scope: {}", appScope);
			assertTrue(appScope.getDepth() == 1);
		}

		vertx = (Vertx) applicationContext.getBean(AntMediaApplicationAdapter.VERTX_BEAN_NAME);


		stopCameraEmulator();

		//reset values in the bean
		getAppSettings().resetDefaults();
		getAppSettings().setMp4MuxingEnabled(true);

		avutil.av_log_set_level(avutil.AV_LOG_INFO);

	}

	@After
	public void after() {

		stopCameraEmulator();

		appScope = null;
		app = null;

		/*
		try {
			AppFunctionalV2Test.delete(new File("webapps"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		 */
	}
    @Test
    public void testWaitForStreamThreadToStop(){
        String streamid = "test";
        StreamFetcherManager manager = Mockito.spy(app.getStreamFetcherManager());
        StreamFetcher streamFetcher = Mockito.spy(new StreamFetcher("test", streamid, "test", appScope, vertx, 0));
        manager.getStreamFetcherList().put(streamid,streamFetcher);

        // thread already stoped
        assertTrue(manager.stopStreaming("test",true).isSuccess());
        verify(streamFetcher).stopStreamBlocking();

        reset(streamFetcher);

        // semaphore already release
        manager.getStreamFetcherList().put(streamid,streamFetcher);
        streamFetcher.setThreadActive(true);
        streamFetcher.getIsThreadStopedSemaphore().release();
        assertTrue(manager.stopStreaming("test",true).isSuccess());
        verify(streamFetcher).stopStreamBlocking();


        reset(streamFetcher);

        // could not stop stream failed
        manager.getStreamFetcherList().put(streamid,streamFetcher);
        streamFetcher.setThreadActive(true);
        streamFetcher.getIsThreadStopedSemaphore().drainPermits();
        assertFalse(manager.stopStreaming("test",true).isSuccess());
        verify(streamFetcher).stopStreamBlocking();

        // without waiting stop
        manager.getStreamFetcherList().put(streamid,streamFetcher);
        streamFetcher.setThreadActive(true);
        assertTrue(manager.stopStreaming("test",false).isSuccess());
    }

	@Test
	public void testForceStartLogic() throws Exception {
		Vertx mockedVertx = Mockito.mock(Vertx.class);
		DataStore dataStore = Mockito.mock(DataStore.class);
		AntMediaApplicationAdapter appAdapter = Mockito.mock(AntMediaApplicationAdapter.class);

		Broadcast broadcast = Mockito.mock(Broadcast.class);
		when(broadcast.getStreamId()).thenReturn("streamId");
		when(broadcast.getType()).thenReturn(AntMediaApplicationAdapter.STREAM_SOURCE);
		when(broadcast.getStatus()).thenReturn(AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING);

		StreamFetcher fetcher = Mockito.spy(new StreamFetcher("url", "streamId", AntMediaApplicationAdapter.STREAM_SOURCE, appScope, mockedVertx, 0));
		fetcher.setDataStore(dataStore);
		Mockito.doReturn(appAdapter).when(fetcher).getInstance();
		when(dataStore.get("streamId")).thenReturn(broadcast);
		Mockito.when(appAdapter.updateBroadcastStatus(Mockito.anyString(), Mockito.anyLong(),
				Mockito.anyString(), Mockito.any(Broadcast.class), Mockito.isNull(), Mockito.anyString())).thenReturn(broadcast);

		// Case 1: without force flag, should return before preparing input
		fetcher.setStartStreamForce(false);
		WorkerThread workerNoForce = Mockito.spy(fetcher.new WorkerThread());
		Mockito.doReturn(false).when(workerNoForce).prepareInputContext(Mockito.any());
		workerNoForce.run();
		Mockito.verify(workerNoForce, Mockito.never()).prepareInputContext(Mockito.any());

		// Case 2: with force flag, should bypass status check and reset flag
		fetcher.setStartStreamForce(true);
		WorkerThread workerForce = Mockito.spy(fetcher.new WorkerThread());
		Mockito.doReturn(false).when(workerForce).prepareInputContext(Mockito.any());
		workerForce.run();
		Mockito.verify(workerForce, Mockito.times(1)).prepareInputContext(Mockito.any());

		// Flag consumed; next run without setting force should abort again
		WorkerThread workerAfter = Mockito.spy(fetcher.new WorkerThread());
		Mockito.doReturn(false).when(workerAfter).prepareInputContext(Mockito.any());
		workerAfter.run();
		Mockito.verify(workerAfter, Mockito.never()).prepareInputContext(Mockito.any());
	}
	@Test
	public void testPlayItemInList() throws Exception {

		StreamFetcherManager manager = Mockito.spy(app.getStreamFetcherManager());
		String streamId = String.valueOf((Math.random() * 100000));

		Broadcast.PlayListItem broadcastItem1 = new Broadcast.PlayListItem("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerMeltdowns.mp4", AntMediaApplicationAdapter.VOD);

		//create a broadcast
		Broadcast.PlayListItem broadcastItem2 = new Broadcast.PlayListItem("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerMeltdowns.mp4", AntMediaApplicationAdapter.VOD);

		//create a broadcast
		Broadcast.PlayListItem broadcastItem3 = new Broadcast.PlayListItem("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerMeltdowns.mp4", AntMediaApplicationAdapter.VOD);

		List<Broadcast.PlayListItem> broadcastList = new ArrayList<>();

		broadcastList.add(broadcastItem1);
		broadcastList.add(broadcastItem2);
		broadcastList.add(broadcastItem3);

		Broadcast playlist = new Broadcast();
		playlist.setStreamId(streamId);
		playlist.setType(AntMediaApplicationAdapter.PLAY_LIST);
		playlist.setPlayListItemList(broadcastList);
		playlist.setStatus(AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING);

        StreamFetcher streamFetcher = mock(StreamFetcher.class);
        Semaphore semaphore = new Semaphore(0);
        semaphore.release();
        doReturn(semaphore).when(streamFetcher).getIsThreadStopedSemaphore();
        doReturn(streamFetcher).when(manager).getStreamFetcher(streamId);

		app.getDataStore().save(playlist);

        StatsCollector statsCollectorMock = Mockito.mock(StatsCollector.class);
        doReturn(true).when(statsCollectorMock).enoughResource();
        app.setStatsCollector(statsCollectorMock);
		boolean startStreaming = app.startStreaming(playlist).isSuccess();
		assertTrue(startStreaming);


		assertEquals(1, app.getStreamFetcherManager().getStreamFetcherList().size());

		StreamFetcher.IStreamFetcherListener listener = Mockito.mock(StreamFetcher.IStreamFetcherListener.class);
		manager.getStreamFetcher(streamId).setStreamFetcherListener(listener);
		manager.playItemInList(playlist,listener,1);

		// stream not stoped need to wait for the thread to stop to start next playlist
		verify(manager,timeout(10000).times(1)).createAndStartNextPlaylistItem(any(),any(),anyInt());

        Mockito.reset(manager);

		// thread already start next stream directly
        doReturn(streamFetcher).when(manager).getStreamFetcher(streamId);
		manager.playItemInList(playlist,streamFetcher.getStreamFetcherListener(),1);
		verify(manager,times(1)).createAndStartNextPlaylistItem(any(),any(),anyInt());

		// invalid url
		Mockito.reset(manager);
		broadcastItem1.setStreamUrl("test");
		playlist.getPlayListItemList().set(playlist.getCurrentPlayIndex(),broadcastItem1);
        doReturn(new Result(true)).when(manager).startPlaylist(playlist);
		manager.playItemInList(playlist,streamFetcher.getStreamFetcherListener(),1);
		verify(manager,times(1)).stopStreaming(streamId, true);
		verify(manager).skipNextPlaylistQueue(playlist,1);
		verify(manager).startPlaylist(playlist);
	}



	@Test
	public void testBugUpdateStreamFetcherStatus() {

		logger.info("starting testBugUpdateStreamFetcherStatus");

		//create ip camera broadcast
		DataStore dataStore = new InMemoryDataStore("target/testbug.db"); //applicationContext.getBean(IDataStore.BEAN_NAME);

		assertNotNull(dataStore);
		DataStoreFactory dsf = Mockito.mock(DataStoreFactory.class);
		Mockito.when(dsf.getDataStore()).thenReturn(dataStore);

		app.setDataStoreFactory(dsf);

		app.setDataStore(dataStore);
		//set mapdb datastore to stream fetcher because in memory datastore just have references and updating broadcst
		// object updates the reference in inmemorydatastore
		app.getStreamFetcherManager().setDatastore(dataStore);

		app.getStreamFetcherManager().setRestartStreamAutomatically(false);
		app.getStreamFetcherManager().testSetStreamCheckerInterval(5000);

		app.getStreamFetcherManager().getStreamFetcherList().clear();

		assertEquals(0, app.getStreamFetcherManager().getStreamFetcherList().size());

		//save it data store
		Broadcast newCam = new Broadcast("testOnvif", "127.0.0.1:8080", "admin", "admin", "rtsp://127.0.0.1:6554/test.flv",
				AntMediaApplicationAdapter.IP_CAMERA);
		String id = dataStore.save(newCam);


		//set status to broadcasting
		dataStore.updateStatus(id, AntMediaApplicationAdapter.BROADCAST_STATUS_CREATED);
		Broadcast broadcast = dataStore.get(id);
		logger.info("broadcast stream id {}" , id);
		assertEquals(AntMediaApplicationAdapter.BROADCAST_STATUS_CREATED, broadcast.getStatus());

		//start StreamFetcher
		app.getStreamFetcherManager().startStreaming(broadcast);


		assertEquals(1, app.getStreamFetcherManager().getStreamFetcherList().size());

		Awaitility.waitAtMost(10, TimeUnit.SECONDS).pollDelay(3, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
			//check that it is not started
			boolean flag3 = false;
			for (StreamFetcher camScheduler : app.getStreamFetcherManager().getStreamFetcherList().values())
			{
				Broadcast broadcastTmp = dataStore.get(camScheduler.getStreamId());
				if (broadcastTmp.getIpAddr().equals(newCam.getIpAddr()))
				{
					// it should be false because emulator has not been started yet
					assertFalse(camScheduler.isStreamAlive());
					flag3 = true;

				}
			}
			return flag3;
		});


		//check that broadcast status in datastore in finished or not broadcasting
		broadcast = dataStore.get(id);
		assertEquals(AntMediaApplicationAdapter.BROADCAST_STATUS_FINISHED, broadcast.getStatus());
		assertEquals(0, broadcast.getSpeed(), 2L);


		app.getStreamFetcherManager().stopStreaming(newCam.getStreamId(), false);
		assertEquals(0, app.getStreamFetcherManager().getStreamFetcherList().size());

		app.stopStreaming(newCam, false, null);


		logger.info("leaving testBugUpdateStreamFetcherStatus");

	}


	@Test
	public void testRestartPeriodStreamFetcher() {

		boolean deleteHLSFilesOnExit = getAppSettings().isDeleteHLSFilesOnEnded();
		getAppSettings().setDeleteHLSFilesOnEnded(false);
		try {
			//Create Stream Fetcher Manager

			InMemoryDataStore memoryDataStore = new InMemoryDataStore("testdb");

			//Create a mock StreamFetcher and add it to StreamFetcherManager
			StreamFetcher streamFetcher = Mockito.mock(StreamFetcher.class);
			Broadcast stream = new Broadcast();

			String streamId = String.valueOf((Math.random() * 100000));
			stream.setStreamId(streamId);

			String streamUrl = "anyurl";
			stream.setStreamUrl(streamUrl);
			memoryDataStore.save(stream);


			when(streamFetcher.getStreamId()).thenReturn(stream.getStreamId());
			when(streamFetcher.getStreamUrl()).thenReturn(streamUrl);

			when(streamFetcher.isStreamAlive()).thenReturn(true);
			when(streamFetcher.getCameraError()).thenReturn(new Result(true));

			StreamFetcherManager fetcherManager_ = new StreamFetcherManager(vertx, memoryDataStore, appScope);
			StreamFetcherManager fetcherManager = Mockito.spy(fetcherManager_);

			Mockito.doReturn(streamFetcher).when(fetcherManager).make(stream, appScope, vertx);

			//set checker interval to 2 seconds
			fetcherManager.testSetStreamCheckerInterval(1000);

			//set restart period to 5 seconds
			appSettings.setRestartStreamFetcherPeriod(2);

			//Start stream fetcher
			boolean streamingStarted = fetcherManager.startStreaming(stream).isSuccess();
			assertTrue(streamingStarted);


			//wait 10-12 seconds
			//check that stream fetcher stop and start stream is called 2 times
			verify(streamFetcher, timeout(5000).times(2)).stopStream();
			//it is +1 because it is called at first start
			verify(streamFetcher, timeout(500).times(3)).startStream();

			//set restart period to 0 seconds
			appSettings.setRestartStreamFetcherPeriod(0);

			//wait 10-12 seconds


			//check that stream fetcher stop and start stream is not called
			//wait 3 seconds
			verify(streamFetcher, timeout(2000).times(2)).stopStream();
			verify(streamFetcher, timeout(500).times(3)).startStream();

			//set restart period to 5 seconds
			appSettings.setRestartStreamFetcherPeriod(2);

			//wait 10-12 seconds

			//check that stream fetcher stop and start stream is not called
			verify(streamFetcher, timeout(3000).atLeast(3)).stopStream();
			verify(streamFetcher, timeout(500).atLeast(4)).startStream();

			appSettings.setRestartStreamFetcherPeriod(0);

			fetcherManager.stopCheckerJob();

		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		getAppSettings().setDeleteHLSFilesOnEnded(deleteHLSFilesOnExit);


	}

	@Test
	public void testThreadStopStart() {

		logger.info("starting testThreadStopStart");
		boolean deleteHLSFilesOnExit = getAppSettings().isDeleteHLSFilesOnEnded();
		getAppSettings().setDeleteHLSFilesOnEnded(false);


		// start stream fetcher

		Broadcast newCam = new Broadcast("onvifCam1", "127.0.0.1:8080", "admin", "admin", "rtsp://127.0.0.1:6554/test.flv",
				AntMediaApplicationAdapter.IP_CAMERA);
		assertNotNull(newCam.getStreamUrl());

		try {
			newCam.setStreamId((int)(Math.random()*100000) + "streamId");
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		assertNotNull(newCam.getStreamId());
		assertNotEquals("0", newCam.getStreamId());

		logger.info("Stream id is {}", newCam.getStreamId());

		getInstance().getDataStore().save(newCam);

		StreamFetcher fetcher = new StreamFetcher(newCam.getStreamUrl(), newCam.getStreamId(), newCam.getType(), appScope, vertx, 0);

		startCameraEmulator();

		// thread start
		fetcher.startStream();

		Awaitility.waitAtMost(10, TimeUnit.SECONDS).until(() -> fetcher.isThreadActive());
		Awaitility.waitAtMost(10, TimeUnit.SECONDS).until(() -> fetcher.isStreamAlive());

		//check that thread is running
		assertTrue(fetcher.isThreadActive());
		assertTrue(fetcher.isStreamAlive());


		//stop thread
		fetcher.stopStream();

		Awaitility.waitAtMost(10, TimeUnit.SECONDS).until(() -> !fetcher.isThreadActive());
		Awaitility.waitAtMost(10, TimeUnit.SECONDS).until(() -> !fetcher.isStreamAlive());


		//change the flag that shows thread is still running
		fetcher.setThreadActive(true);

		fetcher.debugSetStopRequestReceived(false);
		//start thread
		fetcher.startStream();

		//check that thread is not started because thread active is true
		Awaitility.waitAtMost(10, TimeUnit.SECONDS).pollDelay(2, TimeUnit.SECONDS).until(() -> !fetcher.isStreamAlive());
		Awaitility.waitAtMost(10, TimeUnit.SECONDS).until(() -> fetcher.isThreadActive());
		assertFalse(fetcher.isStreamAlive());
		assertTrue(fetcher.isThreadActive());


		logger.info("Change the flag that previous thread is stopped");
		//change the flag that previous thread is stopped
		fetcher.setThreadActive(false);

		//check that thread is started
		Awaitility.waitAtMost(10, TimeUnit.SECONDS).pollDelay(2, TimeUnit.SECONDS).until(() -> fetcher.isStreamAlive());
		Awaitility.waitAtMost(10, TimeUnit.SECONDS).until(() -> fetcher.isThreadActive());


		fetcher.stopStream();

		Awaitility.waitAtMost(10, TimeUnit.SECONDS).pollDelay(2, TimeUnit.SECONDS).until(() -> !fetcher.isStreamAlive());
		Awaitility.waitAtMost(10, TimeUnit.SECONDS).until(() -> !fetcher.isThreadActive());

		assertFalse(fetcher.isStreamAlive());
		assertFalse(fetcher.isThreadActive());

		stopCameraEmulator();



		logger.info("leaving testThreadStopStart");
		getAppSettings().setDeleteHLSFilesOnEnded(deleteHLSFilesOnExit);

	}

	@Test
	public void testOnvifError() {

		startCameraEmulator();

		Broadcast newCam = new Broadcast("onvifCam22", "127.0.0.1:8080", "admin", "admin", null,
				AntMediaApplicationAdapter.IP_CAMERA);


		OnvifCamera onvif = new OnvifCamera();

		int connResult = onvif.connect(newCam.getIpAddr(), newCam.getUsername(), newCam.getPassword());

		logger.info("connResult {}", connResult);

		//it should be 0 because URL and credentials are correct
		assertEquals(0, connResult);

		//define incorrect URL and test
		newCam.setIpAddr("127.0.0.11:8080");

		connResult = onvif.connect(newCam.getIpAddr(), newCam.getUsername(), newCam.getPassword());

		logger.info("connResult {}", connResult);

		//it should be -1 because there is a connection error
		assertEquals(-1, connResult);


		//Test with protocol
		newCam.setIpAddr("http://127.0.0.1:8080");
		connResult = onvif.connect(newCam.getIpAddr(), newCam.getUsername(), newCam.getPassword());
		logger.info("connResult {}", connResult);

		//it should be 0 because URL and credentials are correct
		assertEquals(0, connResult);

		stopCameraEmulator();

	}





	@Test
	public void testCameraErrorCodes() {

		logger.info("starting testCameraErrorCodes");


		// start stream fetcher

		Broadcast newCam = new Broadcast("onvifCam2", "127.0.0.1:8080", "admin", "admin", "rtsp://10.122.59.79:6554/test.flv",
				AntMediaApplicationAdapter.IP_CAMERA);
		assertNotNull(newCam.getStreamUrl());

		try {
			newCam.setStreamId((int)(Math.random()*100000) + "streamId");
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		assertNotNull(newCam.getStreamId());

		getInstance().getDataStore().save(newCam);

		StreamFetcher fetcher = new StreamFetcher(newCam.getStreamUrl(), newCam.getStreamId(), newCam.getType(), appScope, vertx, 0);
		fetcher.setRestartStream(false);
		// thread start
		fetcher.startStream();

		Awaitility.await().atMost(10, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() ->  {
			String message = fetcher.getCameraError().getMessage();
			return message != null && !message.isEmpty();
		});

		//Thread.sleep(8000);

		String str = fetcher.getCameraError().getMessage();
		logger.info("error:   "+str);

		assertNotNull(fetcher.getCameraError().getMessage());

		assertTrue(fetcher.getCameraError().getMessage().contains("timed out"));

		fetcher.stopStream();

		Awaitility.await().atMost(10, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() ->  {
			return !fetcher.isThreadActive();
		});

		// start stream fetcher

		Broadcast newCam2 = new Broadcast("onvifCam3", "127.0.0.1:8080", "admin", "admin", "rtsp://127.0.0.1:6554/test.flv",
				AntMediaApplicationAdapter.IP_CAMERA);
		assertNotNull(newCam2.getStreamUrl());

		try {
			newCam2.setStreamId("543534534534534");
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		getInstance().getDataStore().save(newCam2);

		assertNotNull(newCam2.getStreamId());

		StreamFetcher fetcher2 = new StreamFetcher(newCam2.getStreamUrl(), newCam2.getStreamId(), newCam2.getType(), appScope, vertx, 0);
		fetcher2.setRestartStream(false);
		// thread start
		fetcher2.startStream();

		Awaitility.await().atMost(10, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() ->  {
			String message = fetcher2.getCameraError().getMessage();
			return message != null && !message.isEmpty();
		});

		String str2 = fetcher2.getCameraError().getMessage();
		logger.info("error2:   "+str2);

		assertTrue(fetcher2.getCameraError().getMessage().contains("Connection refused"));

		fetcher2.stopStream();

		Awaitility.await().atMost(10, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() ->  {
			return !fetcher2.isThreadActive();
		});


	}

	@Test
	public void testPacketOrder() throws Exception {
		getAppSettings().setDeleteHLSFilesOnEnded(false);

		String file = "src/test/resources/test_video_360p.flv";
		Broadcast newCam = new Broadcast("streamSource", "127.0.0.1:8080", "admin", "admin",
				file,
				AntMediaApplicationAdapter.STREAM_SOURCE);

		newCam.setStreamId("streaskdjfksf");

		StreamFetcher fetcher = new StreamFetcher(newCam.getStreamUrl(), newCam.getStreamId(), newCam.getType(), appScope, vertx, 0);

		fetcher.setMuxAdaptor(Mockito.mock(MuxAdaptor.class));
		fetcher.setBufferTime(20000);

		fetcher.setRestartStream(false);

		AVFormatContext inputFormatContext = avformat.avformat_alloc_context();
		AVInputFormat findInputFormat = avformat.av_find_input_format("flv");
		if (avformat_open_input(inputFormatContext, (String) file, findInputFormat,
				(AVDictionary) null) < 0) {
			//	return false;
		}

		long startFindStreamInfoTime = System.currentTimeMillis();

		int ret = avformat_find_stream_info(inputFormatContext, (AVDictionary) null);
		if (ret < 0) {
			fail("Cannot find stream info");
		}


		WorkerThread worker = spy(fetcher.new WorkerThread());

		worker.setInputFormatContext(inputFormatContext);

		//give unordered pkts
		AVPacket pkt = new AVPacket();
		pkt.pts(100);
		pkt.dts(100);
		logger.info("sending first packet");
		worker.packetRead(pkt);

		pkt = new AVPacket();
		pkt.pts(0);
		pkt.dts(0);
		worker.packetRead(pkt);
		worker.calculateBufferStatus();

		assertEquals(100, worker.getBufferedDurationMs());


		pkt = new AVPacket();
		pkt.pts(50);
		pkt.dts(50);
		worker.packetRead(pkt);
		worker.calculateBufferStatus();
		
		assertEquals(100, worker.getBufferedDurationMs());

		pkt = new AVPacket();
		pkt.pts(500);
		pkt.dts(500);
		worker.packetRead(pkt);
		worker.calculateBufferStatus();

		assertEquals(500, worker.getBufferedDurationMs());


		//check them in the buffer with the correct order
		ConcurrentSkipListSet<AVPacket> bufferQueue = worker.getBufferQueue();
		pkt = bufferQueue.pollFirst();
		assertEquals(0, pkt.pts());

		pkt = bufferQueue.pollFirst();
		assertEquals(50, pkt.pts());

		pkt = bufferQueue.pollFirst();
		assertEquals(100, pkt.pts());



		pkt = bufferQueue.pollFirst();
		assertEquals(500, pkt.pts());




		getAppSettings().setDeleteHLSFilesOnEnded(true);
	}


	@Test
	public void testStreamFetcherBuffer() {

		try {
			getAppSettings().setDeleteHLSFilesOnEnded(false);

			Broadcast newCam = new Broadcast("streamSource", "127.0.0.1:8080", "admin", "admin",
					"src/test/resources/test_video_360p.flv",
					AntMediaApplicationAdapter.STREAM_SOURCE);
			
			newCam.setStreamId("stream_id_" + RandomStringUtils.randomAlphanumeric(12));

			assertNotNull(newCam.getStreamUrl());

			String id = getInstance().getDataStore().save(newCam);

			assertNotNull(newCam.getStreamId());

			StreamFetcher fetcher = new StreamFetcher(newCam.getStreamUrl(), newCam.getStreamId(), newCam.getType(), appScope, vertx, 0);

			fetcher.setBufferTime(20000);

			fetcher.setRestartStream(false);

			assertFalse(fetcher.isThreadActive());
			assertFalse(fetcher.isStreamAlive());

			// start
			fetcher.startStream();

			//wait for fetching stream
			Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() ->  {
				return fetcher.isThreadActive();
			});

			Awaitility.await().atMost(10, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() ->  {
				return MuxingTest.testFile("webapps/junit/streams/"+newCam.getStreamId() +".m3u8");
			});

			//wait for packaging files
			fetcher.stopStream();

			Awaitility.await().atMost(15,  TimeUnit.SECONDS).until(() -> !fetcher.isThreadActive());
			assertFalse(fetcher.isThreadActive());

			logger.info("before test m3u8 file");

			assertTrue(MuxingTest.testFile("webapps/junit/streams/"+newCam.getStreamId() +".m3u8"));

			logger.info("after test m3u8 file");
			//tmp file should be deleted
			File f = new File("webapps/junit/streams/"+newCam.getStreamId() +".mp4.tmp_extension");
			assertFalse(f.exists());


			f = new File("webapps/junit/streams/"+newCam.getStreamId() +".mp4");
			assertTrue(f.exists());


			logger.info("before test mp4 file");

			assertTrue(MuxingTest.testFile("webapps/junit/streams/"+newCam.getStreamId() +".mp4", 146000));

			logger.info("after test mp4 file");

			getInstance().getDataStore().delete(id);


		}
		catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		getAppSettings().setDeleteHLSFilesOnEnded(true);

	}

	@Test
	public void testCameraStartedProperly() {

		boolean deleteHLSFilesOnExit = getAppSettings().isDeleteHLSFilesOnEnded();
		try {

			getAppSettings().setDeleteHLSFilesOnEnded(false);


			startCameraEmulator();

			// start stream fetcher

			Broadcast newCam3 = new Broadcast("onvifCam4", "127.0.0.1:8080", "admin", "admin", "rtsp://127.0.0.1:6554/test.flv",
					AntMediaApplicationAdapter.IP_CAMERA);
			assertNotNull(newCam3.getStreamUrl());


			newCam3.setStreamId("stream_id_" + (int)(Math.random() * 100000));

			DataStore dataStore = new InMemoryDataStore("ntest");
			dataStore.save(newCam3);

			StreamFetcher fetcher3 = new StreamFetcher(newCam3.getStreamUrl(), newCam3.getStreamId(), newCam3.getType(), appScope, vertx,0);
			fetcher3.setRestartStream(false);

			fetcher3.setDataStore(dataStore);
			// thread start
			fetcher3.startStream();

			Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> {
				return 1 == getInstance().getMuxAdaptors().size();
			});
			assertEquals(1, getInstance().getMuxAdaptors().size());

			String str3=fetcher3.getCameraError().getMessage();
			assertTrue(fetcher3.getCameraError().isSuccess());
			logger.info("error:   "+str3);

			assertTrue(StringUtils.isBlank(fetcher3.getCameraError().getMessage()));

			Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> {
				return fetcher3.isStreamAlive();
			});

			fetcher3.stopStream();

			Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> {
				return 0 == getInstance().getMuxAdaptors().size();
			});

			assertEquals(0, getInstance().getMuxAdaptors().size());

			stopCameraEmulator();
		}
		catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		getAppSettings().setDeleteHLSFilesOnEnded(deleteHLSFilesOnExit);
	}


	@Test
	public void testFLVSource() {
		logger.info("running testFLVSource");
		//test FLV Source
		//this also tests bug about #1600
		testFetchStreamSources("src/test/resources/test_video_360p.flv", false, true);
		logger.info("leaving testFLVSource");
	}
	
	@Test
	public void testSeekTime() 
	{

		Application.enableSourceHealthUpdate = true;
		//duration of this file is 02:26 -> 146 seconds
		String source = "src/test/resources/test_video_360p.flv";
		boolean deleteHLSFilesOnExit = getAppSettings().isDeleteHLSFilesOnEnded();
		try {
			getAppSettings().setDeleteHLSFilesOnEnded(false);

			Broadcast newCam = new Broadcast("streamSource", "127.0.0.1:8080", "admin", "admin", source,
					AntMediaApplicationAdapter.VOD);

			assertNotNull(newCam.getStreamUrl());
			DataStore dataStore = new InMemoryDataStore("db"); //.getDataStore();

			String id = dataStore.save(newCam);


			assertNotNull(newCam.getStreamId());

			StreamFetcher fetcher = Mockito.spy(new StreamFetcher(newCam.getStreamUrl(), newCam.getStreamId(), newCam.getType(), appScope, vertx, 0));

			fetcher.setDataStore(dataStore);
			fetcher.setRestartStream(false);

			assertFalse(fetcher.isThreadActive());
			assertFalse(fetcher.isStreamAlive());

			// start
			fetcher.startStream();

			//wait for fetching stream
			Awaitility.await().atMost(50, TimeUnit.SECONDS).until(() -> {
				// This issue is the check of #1600
				return fetcher.getMuxAdaptor() != null;
			});


			Awaitility.await().atMost(20, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> fetcher.isStreamAlive());

			Awaitility.await().pollDelay(2, TimeUnit.SECONDS).atMost(10, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(()-> {
				double speed = dataStore.get(newCam.getStreamId()).getSpeed();
				//this value was so high over 9000. After using first packet time it's value is about 100-200
				//it is still high and it is normal because it reads vod from disk it does not read live stream.
				//Btw, nba.ts , in testTSSourceAndBugStreamSpeed, is generated specifically by copying timestamps directy
				//from live stream by using copyts parameter in ffmpeg
				logger.info("Speed of the stream: {}", speed);
				return speed < 1000;
			});
			
			assertFalse(fetcher.getSeekTimeRequestReceived().get());
			
			fetcher.seekTime(100000);
			
			assertTrue(fetcher.getSeekTimeRequestReceived().get());

			Awaitility.await().atMost(5000, TimeUnit.SECONDS).pollDelay(4, TimeUnit.SECONDS).until(()-> {
				//wait for packaging files
				fetcher.stopStream();
				return true;
			});
			

			assertFalse(fetcher.getSeekTimeRequestReceived().get());


			String mp4File = "webapps/junit/streams/"+newCam.getStreamId() +".mp4";


			Awaitility.waitAtMost(10, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
				return new File(mp4File).exists();
			});


			Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> !fetcher.isThreadActive());

			assertFalse(fetcher.isThreadActive());

			logger.info("before test m3u8 file");

			double speed = dataStore.get(newCam.getStreamId()).getSpeed();
			logger.info("Speed of the stream: {}", speed);

			assertTrue(MuxingTest.testFile("webapps/junit/streams/"+newCam.getStreamId() +".m3u8"));

			logger.info("after test m3u8 file");
			//tmp file should be deleted
			File f = new File("webapps/junit/streams/"+newCam.getStreamId() +".mp4.tmp_extension");
			assertFalse(f.exists());

			logger.info("before test mp4 file");

			assertTrue(MuxingTest.testFile(mp4File));

			logger.info("after test mp4 file");

			getInstance().getDataStore().delete(id);

		}
		catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		getAppSettings().setDeleteHLSFilesOnEnded(deleteHLSFilesOnExit);

		Application.enableSourceHealthUpdate = false;
	}


	@Test
	public void testBugUnexpectedStream() throws InterruptedException
	{

		AVCodecParameters pars = new AVCodecParameters(); 
		pars.codec_type(AVMEDIA_TYPE_DATA);

		Mp4Muxer mp4Muxer = Mockito.spy(new Mp4Muxer(null, null, "streams"));

		mp4Muxer.init(appScope, "test", 480, null, 750);


		Mockito.doReturn(true).when(mp4Muxer).isCodecSupported(Mockito.anyInt());

		mp4Muxer.addStream(pars, MuxAdaptor.TIME_BASE_FOR_MS, 0);

		Mockito.verify(mp4Muxer, Mockito.never()).avNewStream(Mockito.any());
		
		pars.close();
		pars = null;

		
	}

	@Test
	public void testRTSPSource() {
		startCameraEmulator();
		logger.info("running testRTSPSource");
		//test RTSP Source
		testFetchStreamSources("rtsp://127.0.0.1:6554/test.flv", false, true);
		logger.info("leaving testRTSPSource");
		stopCameraEmulator();
	}

	@Test
	public void testHLSSource() {
		logger.info("running testHLSSource");

		//test HLS Source
		testFetchStreamSources("src/test/resources/test.m3u8", false, false);
		logger.info("leaving testHLSSource");
	}
	
	@Test
	public void testHLSSourceFmp4() {
		logger.info("running testHLSSource");

		//test HLS Source
		String streamId = testFetchStreamSources("src/test/resources/test.m3u8", false, false, true, "fmp4");
		
		String[] filesInStreams = new File("webapps/junit/streams").list();
		boolean initFileFound = false;
		
		//matches 13 digits because System.currentTimeMillis() is used in the file
        String regex = streamId + "_\\d{13}_init.mp4";
		System.out.println("regex:"+regex);

		for (int i = 0; i < filesInStreams.length; i++) {
			System.out.println("files:"+filesInStreams[i]);
			initFileFound |= filesInStreams[i].matches(regex);
		}
		assertTrue(initFileFound);
		
		logger.info("leaving testHLSSource");
	}

	@Test
	public void testH264VideoPCMAudio() {
		logger.info("running testTSSource");
		//test h264 video and pcm audio
		testFetchStreamSources("src/test/resources/test_video_360p_pcm_audio.mkv", false, false);
		logger.info("leaving testTSSource");
	}


	@Test
	public void testTSSourceAndBugStreamSpeed() {
		logger.info("running testTSSource");
		//test TS Source
		testFetchStreamSources("src/test/resources/nba.ts", false, false);
		logger.info("leaving testTSSource");
	}

	@Test
	public void testShoutcastSource() {
		logger.info("running testShoutcastSource");
		//test Southcast Source - http://sc13.shoutcaststreaming.us/
		//http://107.181.227.250:8526/stream/1/
		//http://icecast.rte.ie/ieradio1
		testFetchStreamSources("http://stream.antenne.de:80/rockantenne", false, false);
		logger.info("leaving testShoutcastSource");
	}

	@Test
	public void testAudioOnlySource() {
		logger.info("running testAudioOnlySource");
		//test AudioOnly Source
		testFetchStreamSources("https://moondigitaledge.radyotvonline.net/karadenizfm/playlist.m3u8", false, false);
		logger.info("leaving testAudioOnlySource");
	}


	@Test
	public void testAudioOnlySourceClassFM() {
		logger.info("running testAudioOnlySourceClassFM");
		//test AudioOnly Source
		testFetchStreamSources("http://media-ice.musicradio.com/ClassicFM", false, false);
		logger.info("leaving testAudioOnlySource");
	}

	

	public void testFetchStreamSources(String source, boolean restartStream, boolean checkContext) {
		testFetchStreamSources(source, restartStream, checkContext, true);
	}
	
	public void testFetchStreamSources(String source, boolean restartStream, boolean checkContext, boolean audioExists)  {
		testFetchStreamSources(source, restartStream, checkContext, audioExists, null);
	}

	public String testFetchStreamSources(String source, boolean restartStream, boolean checkContext, boolean audioExists, String hlsFragmentType) {

		Application.enableSourceHealthUpdate = true;
		boolean deleteHLSFilesOnExit = getAppSettings().isDeleteHLSFilesOnEnded();
		String streamId = null;
		try {
			getAppSettings().setDeleteHLSFilesOnEnded(false);
			
			if (StringUtils.isBlank(hlsFragmentType)) {
				hlsFragmentType = "mpegts";
			}
			
			getAppSettings().setHlsSegmentType(hlsFragmentType);

			Broadcast newCam = new Broadcast("streamSource", "127.0.0.1:8080", "admin", "admin", source,
					AntMediaApplicationAdapter.STREAM_SOURCE);

			assertNotNull(newCam.getStreamUrl());
			DataStore dataStore = new InMemoryDataStore("db"); //.getDataStore();

			streamId = dataStore.save(newCam);


			assertNotNull(newCam.getStreamId());

			StreamFetcher fetcher = new StreamFetcher(newCam.getStreamUrl(), newCam.getStreamId(), newCam.getType(), appScope, vertx, 0);

			fetcher.setDataStore(dataStore);
			fetcher.setRestartStream(restartStream);

			assertFalse(fetcher.isThreadActive());
			assertFalse(fetcher.isStreamAlive());

			// start
			fetcher.startStream();

			//wait for fetching stream
			if (checkContext) {
				Awaitility.await().atMost(50, TimeUnit.SECONDS).until(() -> {
					// This issue is the check of #1600

					//xor ^ 
					// 0 ^ 0 -> 0
					// 0 ^ 1 -> 1
					// 1 ^ 0 -> 1
					// 1 ^ 1 -> 0
					return fetcher.getMuxAdaptor() != null && !(audioExists ^ fetcher.getMuxAdaptor().isEnableAudio());
				});
			}

			Awaitility.await().atMost(20, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> fetcher.isStreamAlive());

			Awaitility.await().pollDelay(2, TimeUnit.SECONDS).atMost(10, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(()-> {
				double speed = dataStore.get(newCam.getStreamId()).getSpeed();
				//this value was so high over 9000. After using first packet time it's value is about 100-200
				//it is still high and it is normal because it reads vod from disk it does not read live stream.
				//Btw, nba.ts , in testTSSourceAndBugStreamSpeed, is generated specifically by copying timestamps directy
				//from live stream by using copyts parameter in ffmpeg
				logger.info("Speed of the stream: {}", speed);
				return speed < 1000;
			});


			//wait for packaging files
			fetcher.stopStream();


			String mp4File = "webapps/junit/streams/"+newCam.getStreamId() +".mp4";


			Awaitility.waitAtMost(10, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
				return new File(mp4File).exists();
			});


			Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> !fetcher.isThreadActive());

			assertFalse(fetcher.isThreadActive());

			logger.info("before test m3u8 file");

			double speed = dataStore.get(newCam.getStreamId()).getSpeed();
			logger.info("Speed of the stream: {}", speed);

			assertTrue(MuxingTest.testFile("webapps/junit/streams/"+newCam.getStreamId() +".m3u8"));

			logger.info("after test m3u8 file");
			//tmp file should be deleted
			File f = new File("webapps/junit/streams/"+newCam.getStreamId() +".mp4.tmp_extension");
			assertFalse(f.exists());

			logger.info("before test mp4 file");

			assertTrue(MuxingTest.testFile(mp4File));

			logger.info("after test mp4 file");

			getInstance().getDataStore().delete(streamId);

		}
		catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		getAppSettings().setDeleteHLSFilesOnEnded(deleteHLSFilesOnExit);

		Application.enableSourceHealthUpdate = false;
		
		return streamId;


	}

	@Test
	public void testStopRequestReceived() {
		Broadcast stream = new Broadcast("streamSource", "127.0.0.1:8080", "admin", "admin", "rtsp://localhost:44332/this_does_not_exist",
				AntMediaApplicationAdapter.STREAM_SOURCE);
		DataStore dataStore = getInstance().getDataStore();
		String id = dataStore.save(stream);

		StreamFetcher fetcher = new StreamFetcher(stream.getStreamUrl(), stream.getStreamId(), stream.getType(), appScope, vertx, 0);

		fetcher.setRestartStream(true);

		assertFalse(fetcher.isThreadActive());
		assertFalse(fetcher.isStreamAlive());

		// start
		fetcher.startStream();

		Awaitility.await().pollDelay(3, TimeUnit.SECONDS).atMost(6, TimeUnit.SECONDS).until(() -> !fetcher.isStreamAlive());

		fetcher.stopStream();

		Awaitility.await().pollDelay(4, TimeUnit.SECONDS).atMost(7, TimeUnit.SECONDS).until(() -> !fetcher.isThreadActive());

	}

	@Test
	public void testHLSFlagResult() {

		boolean deleteHLSFilesOnExit = getAppSettings().isDeleteHLSFilesOnEnded();
		getAppSettings().setDeleteHLSFilesOnEnded(false);
		//getAppSettings().setHlsflags("+omit_endlist+append_list+split_by_time");
		getAppSettings().setHlsListSize("20");
		getAppSettings().setHlsTime("2");
		getAppSettings().setHlsflags("+omit_endlist+discont_start+split_by_time");

		try {
			String textInFile;

			Broadcast newCam = new Broadcast("streamSource", "127.0.0.1:8080", "admin", "admin", "src/test/resources/nba.ts",
					AntMediaApplicationAdapter.STREAM_SOURCE);

			assertNotNull(newCam.getStreamUrl());

			String id = getInstance().getDataStore().save(newCam);


			assertNotNull(newCam.getStreamId());
			assertEquals(id, newCam.getStreamId());

			StreamFetcher fetcher = new StreamFetcher(newCam.getStreamUrl(), newCam.getStreamId(), newCam.getType(), appScope, vertx, 0);

			fetcher.setRestartStream(false);

			assertFalse(fetcher.isThreadActive());
			assertFalse(fetcher.isStreamAlive());

			// start
			fetcher.startStream();


			//wait for fetching stream

			String hlsFile = "webapps/junit/streams/"+newCam.getStreamId() +".m3u8";
			Awaitility.await().pollDelay(5, TimeUnit.SECONDS).until(() -> {
				return new File(hlsFile).exists();
			});


			//wait for packaging files
			fetcher.stopStream();

			String mp4File = "webapps/junit/streams/"+newCam.getStreamId() +".mp4";

			Awaitility.await().until(() -> {
				return new File(mp4File).exists();
			});

			assertFalse(fetcher.isThreadActive());

			//assertTrue(MuxingTest.testFile(hlsFile));


			{
				// start again to check append_list working
				logger.info("Starting stream again for streamId:{} and streamId from fetcher:{} dataStore:{}", newCam.getStreamId(), fetcher.getStreamId(), getInstance().getDataStore().hashCode());
				assertNotNull(getInstance().getDataStore().get(newCam.getStreamId()));

				fetcher.startStream();

				//wait for fetching stream

				Awaitility.await().pollDelay(5, TimeUnit.SECONDS).until(() -> {
					return new File(hlsFile).exists();
				});

				//wait for packaging files
				fetcher.stopStream();

				String mp4File2 = "webapps/junit/streams/"+newCam.getStreamId() +"_1.mp4";

				Awaitility.await().until(() -> {
					return new File(mp4File2).exists();
				});

				assertFalse(fetcher.isThreadActive());
			}


			BufferedReader br = new BufferedReader(new FileReader("webapps/junit/streams/"+newCam.getStreamId() +".m3u8"));
			try {
				StringBuilder sb = new StringBuilder();
				String line = br.readLine();

				while (line != null) {
					sb.append(line);
					//  sb.append(System.lineSeparator());
					line = br.readLine();
				}
				textInFile = sb.toString();

				logger.info(textInFile);
			} finally {
				br.close();
			}

			//Check that m3u8 file does not include "EXT-X-ENDLIST" parameter because "omit_endlist" flag is used in HLS Muxer
			assertFalse(textInFile.contains("EXT-X-ENDLIST"));

			getInstance().getDataStore().delete(id);
		}
		catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		getAppSettings().setDeleteHLSFilesOnEnded(deleteHLSFilesOnExit);
		getAppSettings().setHlsflags(null);


	}


	public static void startCameraEmulator() {
		stopCameraEmulator();

		ProcessBuilder pb = new ProcessBuilder("/usr/local/onvif/runme.sh");
		Process p = null;
		try {
			p = pb.start();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}


	public static void stopCameraEmulator() {
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




	public AntMediaApplicationAdapter getInstance() {
		if (appInstance == null) {
			appInstance = (AntMediaApplicationAdapter) applicationContext.getBean("web.handler");
		}
		return appInstance;
	}

	public AppSettings getAppSettings() {
		if (appSettings == null) {
			appSettings = (AppSettings) applicationContext.getBean(AppSettings.BEAN_NAME);
		}
		return appSettings;
	}

	@Test
	public void testMP4RecordingOnTheFly() throws InterruptedException {

		try {
			startCameraEmulator();

			AppSettings apps = getAppSettings();
			boolean mp4Recording = apps.isMp4MuxingEnabled();
			apps.setMp4MuxingEnabled(false);

			String streamId = "Stream"+(int)(Math.random()*10000);
			Broadcast newCam = new Broadcast("testOnvif", "127.0.0.1:8080", "admin", "admin", "rtsp://127.0.0.1:6554/test.flv",
					AntMediaApplicationAdapter.IP_CAMERA);

			newCam.setStreamId(streamId);
			DataStore dtStore = new InMemoryDataStore("db");
			dtStore.save(newCam);

			StreamFetcher camScheduler = new StreamFetcher(newCam.getStreamUrl(), newCam.getStreamId(), newCam.getType(), appScope, vertx, 0);

			camScheduler.setDataStore(dtStore);
			camScheduler.setConnectionTimeout(10000);

			camScheduler.startStream();

			Awaitility.await().atMost(15, TimeUnit.SECONDS).until(() -> camScheduler.getMuxAdaptor() != null);
			Thread.sleep(2000);
			assertTrue(camScheduler.getMuxAdaptor().startRecording(RecordType.MP4, 0) != null);
			Thread.sleep(5000);
			assertTrue(camScheduler.getMuxAdaptor().stopRecording(RecordType.MP4, 0) != null);
			Thread.sleep(2000);
			camScheduler.stopStream();
			assertTrue(MuxingTest.testFile("webapps/junit/streams/"+newCam.getStreamId() +".mp4"));
			apps.setMp4MuxingEnabled(mp4Recording);

			stopCameraEmulator();
		}
		catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}


	@Test
	public void testVODStreamingInCaseOfReadProblem() throws Exception {
		StreamFetcher fetcher = new StreamFetcher("", "", AntMediaApplicationAdapter.VOD, appScope, vertx, 0);
		fetcher.setMuxAdaptor(mock(MuxAdaptor.class));
		WorkerThread worker = spy(fetcher.new WorkerThread());


		Broadcast broadcast = new Broadcast();
		doReturn(true).when(worker).prepareInputContext(broadcast);

		doNothing().when(worker).packetRead(any());
		doNothing().when(worker).close(any());
		doNothing().when(worker).unReferencePacket(any());


		doReturn(0).when(worker).readNextPacket(any());
		assertTrue(worker.readMore(mock(AVPacket.class)));
		verify(worker, times(1)).packetRead(any());


		assertTrue(worker.readMore(mock(AVPacket.class)));
		verify(worker, times(2)).packetRead(any());

		//return negative in VOD mode it won'tstop but not use packet
		doReturn(-1).when(worker).readNextPacket(any());
		assertTrue(worker.readMore(mock(AVPacket.class)));
		verify(worker, times(2)).packetRead(any());

		//return AVERROR_EOF
		doReturn(AVERROR_EOF).when(worker).readNextPacket(any());
		assertFalse(worker.readMore(mock(AVPacket.class)));
		verify(worker, times(2)).packetRead(any());
	}
	
	@Test
	public void testWritePacketOffset() {
		StreamFetcher fetcher = new StreamFetcher("", "", AntMediaApplicationAdapter.VOD, appScope, vertx, 0);

		MuxAdaptor muxAdaptor = mock(MuxAdaptor.class);
		fetcher.setMuxAdaptor(muxAdaptor);

		WorkerThread workerThread = fetcher.new WorkerThread();
		AVStream stream = new AVStream();


		fetcher.initDTSArrays(2);
		{
			AVPacket pkt = avcodec.av_packet_alloc();
			pkt.pts(10);
			pkt.dts(10);
			pkt.stream_index(1);
			pkt.data(new BytePointer(15)).size(15);

			workerThread.writePacket(stream, pkt);
			
			
			assertEquals(10, workerThread.getLastSentDTS()[1]);

			avcodec.av_packet_free(pkt);

		}

		{
			AVPacket pkt = avcodec.av_packet_alloc();
			pkt.pts(20);
			pkt.dts(20);
			pkt.stream_index(1);
			pkt.data(new BytePointer(15)).size(15);

			workerThread.writePacket(stream, pkt);

			
			assertEquals(20, workerThread.getLastSentDTS()[1]);
			avcodec.av_packet_free(pkt);


		}

		{
			AVPacket pkt = avcodec.av_packet_alloc();
			pkt.pts(15);
			pkt.dts(15);
			pkt.stream_index(1);
			pkt.data(new BytePointer(15)).size(15);

			workerThread.writePacket(stream, pkt);
			
			assertEquals(21, workerThread.getLastSentDTS()[1]);
			avcodec.av_packet_free(pkt);


		}

		{
			AVPacket pkt = avcodec.av_packet_alloc();
			pkt.pts(25);
			pkt.dts(25);
			pkt.stream_index(1);
			pkt.data(new BytePointer(15)).size(15);

			workerThread.writePacket(stream, pkt);

			
			assertEquals(25, workerThread.getLastSentDTS()[1]);
			avcodec.av_packet_free(pkt);

		}

		{
			AVPacket pkt = avcodec.av_packet_alloc();
			pkt.pts(30);
			pkt.dts(30);
			pkt.stream_index(1);
			pkt.data(new BytePointer(15)).size(15);

			workerThread.writePacket(stream, pkt);
			
			assertEquals(30, workerThread.getLastSentDTS()[1]);
			avcodec.av_packet_free(pkt);

			

		}


		{
			AVPacket pkt = avcodec.av_packet_alloc();
			pkt.pts(0);
			pkt.dts(0);
			pkt.stream_index(1);
			pkt.data(new BytePointer(15)).size(15);

			workerThread.writePacket(stream, pkt);
			
			assertEquals(31, workerThread.getLastSentDTS()[1]);
			avcodec.av_packet_free(pkt);

			

		}

		{
			AVPacket pkt = avcodec.av_packet_alloc();
			pkt.pts(10);
			pkt.dts(10);
			pkt.stream_index(1);
			pkt.data(new BytePointer(15)).size(15);

			workerThread.writePacket(stream, pkt);
			
			assertEquals(41, workerThread.getLastSentDTS()[1]);
			avcodec.av_packet_free(pkt);

			

		}

		{
			AVPacket pkt = avcodec.av_packet_alloc();
			pkt.pts(20);
			pkt.dts(20);
			pkt.stream_index(1);
			pkt.data(new BytePointer(15)).size(15);

			workerThread.writePacket(stream, pkt);

			assertEquals(51, workerThread.getLastSentDTS()[1]);
			avcodec.av_packet_free(pkt);

			
		}

	}
	
	@Test
	public void testCheckAndFixSynch() {
		StreamFetcher fetcher = new StreamFetcher("", "", AntMediaApplicationAdapter.VOD, appScope, vertx, 0);

		MuxAdaptor muxAdaptor = mock(MuxAdaptor.class);
		fetcher.setMuxAdaptor(muxAdaptor);

		WorkerThread workerThread = Mockito.spy(fetcher.new WorkerThread());
		AVStream stream = new AVStream();

		String source = "src/test/resources/test_video_360p.flv";
		
		fetcher.initDTSArrays(2);
		
		workerThread.checkAndFixSynch();
		Mockito.verify(workerThread, Mockito.never()).getCodecType(Mockito.anyInt());
		
		Mockito.doReturn(AVMEDIA_TYPE_VIDEO).when(workerThread).getCodecType(Mockito.anyInt());
		Mockito.doReturn(MuxAdaptor.TIME_BASE_FOR_MS).when(workerThread).getStreamTimebase(Mockito.anyInt());

		
		Awaitility.await().pollDelay(3, TimeUnit.SECONDS).atMost(5, TimeUnit.SECONDS).until(()-> {
			workerThread.checkAndFixSynch();
			return true;
		});
		
		Mockito.verify(workerThread, Mockito.times(2)).getCodecType(Mockito.anyInt());
		
		long[] lastSentDTS = fetcher.getLastSentDTS();
		assertEquals(-1, lastSentDTS[0]);
		assertEquals(-1, lastSentDTS[1]);

		
		lastSentDTS[0] = 0;
		lastSentDTS[1] = 200;
		
		Awaitility.await().pollDelay(3, TimeUnit.SECONDS).atMost(5, TimeUnit.SECONDS).until(()-> {
			workerThread.checkAndFixSynch();
			return true;
		});
		
		assertEquals(200, lastSentDTS[0]);
		assertEquals(200, lastSentDTS[1]);

	}

	@Test
	public void testRTSPAllowedMediaTypes(){
  		// allowed_media_types should be remove from url params ( because what if rtsp server does not support url param eg. happytime rtsp server )
		StreamFetcher streamFetcher = new StreamFetcher("rtsp://127.0.0.1:6554/test.flv?allowed_media_types=audio", "testRtspUrlParam", "rtsp_source", appScope, Vertx.vertx(), 0);

		AVDictionary testOptions = new AVDictionary();
		streamFetcher.parseRtspUrlParams(testOptions);
    		assertEquals(streamFetcher.getStreamUrl(),"rtsp://127.0.0.1:6554/test.flv");

		AVDictionaryEntry entry = avutil.av_dict_get(testOptions, "allowed_media_types", null, 0);
		if (entry != null) {
			String value = entry.value().getString();
			assertTrue("audio".equalsIgnoreCase(value));
		}

		assert(true);

    		// other url parameters should not be removed

		StreamFetcher streamFetcher1 = new StreamFetcher("rtsp://127.0.0.1:6554/test.flv?testParam=testParam", "testRtspUrlParam1", "rtsp_source", appScope, Vertx.vertx(), 0);

		AVDictionary testOptions1 = new AVDictionary();
		streamFetcher1.parseRtspUrlParams(testOptions1);
    		assertEquals("rtsp://127.0.0.1:6554/test.flv?testParam=testParam",streamFetcher1.getStreamUrl());

		//incorrect url format
		StreamFetcher streamFetcher2 = new StreamFetcher("rtsp://127.0.0.1:  space  6554/test.flv?allowed_media_types=video", "testRtspUrlParam2", "rtsp_source", appScope, Vertx.vertx(), 0);

		AVDictionary testOptions2 = new AVDictionary();
		streamFetcher2.parseRtspUrlParams(testOptions2);

		entry = avutil.av_dict_get(testOptions2, "allowed_media_types", null, 0);
    		assertTrue(entry == null);

		// param order should be preserverd

		streamFetcher1 = new StreamFetcher("rtsp://test:asdf%2499@127.0.0.1:554/cam/realmonitor?channel=2&subtype=1&allowed_media_types=video", "testRtspUrlParam1", "rtsp_source", appScope, Vertx.vertx(), 0);

		testOptions1 = new AVDictionary();
		streamFetcher1.parseRtspUrlParams(testOptions1);
		assertEquals("rtsp://test:asdf%2499@127.0.0.1:554/cam/realmonitor?channel=2&subtype=1",streamFetcher1.getStreamUrl());
	}
	@Test
	public void testInternalStreamFetcher(){

		//InternalStreamFetcher internalStreamFetcher = new InternalStreamFetcher("rtmp://test.com/test", "testRtspUrlParam1", "rtsp_source", appScope, Vertx.vertx(), 0);
		//AppSettings mockAppSettings = Mockito.mock(AppSettings.class);
		//doReturn("test").when(mockAppSettings).getClusterCommunicationKey();
		//assertTrue(internalStreamFetcher.getStreamUrl().startsWith("rtmp://test.com/test?token="));
        //assertEquals("rtmp://test.com/test", internalStreamFetcher.rtmpUrl);
	}
}
