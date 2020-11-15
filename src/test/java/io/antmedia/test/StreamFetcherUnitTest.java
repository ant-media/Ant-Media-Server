package io.antmedia.test;

import static org.bytedeco.ffmpeg.global.avutil.AVMEDIA_TYPE_DATA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.bytedeco.ffmpeg.global.avcodec.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.bytedeco.ffmpeg.avcodec.AVCodecParameters;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.avformat.AVStream;
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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.AppSettings;
import io.antmedia.IApplicationAdaptorFactory;
import io.antmedia.RecordType;
import io.antmedia.datastore.db.DataStore;
import io.antmedia.datastore.db.DataStoreFactory;
import io.antmedia.datastore.db.InMemoryDataStore;
import io.antmedia.datastore.db.MapDBStore;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.integration.AppFunctionalV2Test;
import io.antmedia.integration.MuxingTest;
import io.antmedia.ipcamera.OnvifCamera;
import io.antmedia.muxer.Mp4Muxer;
import io.antmedia.muxer.MuxAdaptor;
import io.antmedia.rest.model.Result;
import io.antmedia.streamsource.StreamFetcher;
import io.antmedia.streamsource.StreamFetcherManager;
import io.vertx.core.Vertx;

@ContextConfiguration(locations = { "test.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
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
		avformat.av_register_all();
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
			app = ((IApplicationAdaptorFactory) applicationContext.getBean("web.handler")).getAppAdaptor();
			logger.debug("Application / web scope: {}", appScope);
			assertTrue(appScope.getDepth() == 1);
		}

		vertx = (Vertx) applicationContext.getBean(AntMediaApplicationAdapter.VERTX_BEAN_NAME);


		stopCameraEmulator();

		//reset values in the bean
		getAppSettings().resetDefaults();
		getAppSettings().setMp4MuxingEnabled(true);
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
	public void testBugUpdateStreamFetcherStatus() {

		logger.info("starting testBugUpdateStreamFetcherStatus");

		//create ip camera broadcast
		DataStore dataStore = new MapDBStore("target/testbug.db"); //applicationContext.getBean(IDataStore.BEAN_NAME);

		assertNotNull(dataStore);
		DataStoreFactory dsf = Mockito.mock(DataStoreFactory.class);
		Mockito.when(dsf.getDataStore()).thenReturn(dataStore);

		app.setDataStoreFactory(dsf);

		//set mapdb datastore to stream fetcher because in memory datastore just have references and updating broadcst
		// object updates the reference in inmemorydatastore
		app.getStreamFetcherManager().setDatastore(dataStore);

		app.getStreamFetcherManager().setRestartStreamAutomatically(false);
		app.getStreamFetcherManager().setStreamCheckerInterval(5000);

		app.getStreamFetcherManager().getStreamFetcherList().clear();

		assertEquals(0, app.getStreamFetcherManager().getStreamFetcherList().size());

		//save it data store
		Broadcast newCam = new Broadcast("testOnvif", "127.0.0.1:8080", "admin", "admin", "rtsp://127.0.0.1:6554/test.flv",
				AntMediaApplicationAdapter.IP_CAMERA);
		String id = dataStore.save(newCam);


		//set status to broadcasting
		dataStore.updateStatus(id, AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING);
		Broadcast broadcast = dataStore.get(id);
		logger.info("broadcast stream id {}" , id);
		assertEquals(AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING, broadcast.getStatus());

		//start StreamFetcher
		app.getStreamFetcherManager().startStreams(Arrays.asList(broadcast));

		assertEquals(1, app.getStreamFetcherManager().getStreamFetcherList().size());

		//wait 5seconds because connectivity time out is 4sec by default
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}


		//check that it is not started
		boolean flag3 = false;
		for (StreamFetcher camScheduler : app.getStreamFetcherManager().getStreamFetcherList()) {
			if (camScheduler.getStream().getIpAddr().equals(newCam.getIpAddr())) {
				// it should be false because emulator has not been started yet
				assertFalse(camScheduler.isStreamAlive());
				flag3 = true;

			}
		}

		assertTrue(flag3);

		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		//check that broadcast status in datastore in finished or not broadcasting
		broadcast = dataStore.get(id);
		assertEquals(AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING, broadcast.getStatus());
		assertEquals(0, broadcast.getSpeed(), 2L);


		app.getStreamFetcherManager().stopStreaming(newCam);
		assertEquals(0, app.getStreamFetcherManager().getStreamFetcherList().size());


		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		app.stopStreaming(newCam);

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
			Broadcast stream =  Mockito.mock(Broadcast.class);

			stream.setStreamId(String.valueOf((Math.random() * 100000)));

			stream.setStreamUrl("anyurl");
			streamFetcher.setStream(stream);
			when(streamFetcher.getStream()).thenReturn(stream);
			when(streamFetcher.isStreamAlive()).thenReturn(true);
			when(streamFetcher.getCameraError()).thenReturn(new Result(true));

			StreamFetcherManager fetcherManager_ = new StreamFetcherManager(vertx, memoryDataStore, appScope);
			StreamFetcherManager fetcherManager = Mockito.spy(fetcherManager_);

			Mockito.doReturn(streamFetcher).when(fetcherManager).make(stream, appScope, vertx);

			//set checker interval to 3 seconds
			fetcherManager.setStreamCheckerInterval(4000);

			//set restart period to 5 seconds
			appSettings.setRestartStreamFetcherPeriod(5);

			//Start stream fetcher
			StreamFetcher result = fetcherManager.startStreaming(stream);
			assertNotNull(result);


			//wait 10-12 seconds
			//check that stream fetcher stop and start stream is called 2 times
			verify(streamFetcher, timeout(13000).times(2)).stopStream();
			//it is +1 because it is called at first start
			verify(streamFetcher, times(3)).startStream(); 

			//set restart period to 0 seconds
			appSettings.setRestartStreamFetcherPeriod(0);

			//wait 10-12 seconds

			//check that stream fetcher stop and start stream is not called
			verify(streamFetcher, timeout(13000).times(2)).stopStream();
			verify(streamFetcher, times(3)).startStream(); 

			//set restart period to 5 seconds
			appSettings.setRestartStreamFetcherPeriod(5);

			//wait 10-12 seconds

			//check that stream fetcher stop and start stream is not called
			verify(streamFetcher, timeout(14000).atLeast(4)).stopStream();
			verify(streamFetcher, atLeast(5)).startStream(); 

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

		try {

			// start stream fetcher

			Broadcast newCam = new Broadcast("onvifCam1", "127.0.0.1:8080", "admin", "admin", "rtsp://127.0.0.1:6554/test.flv",
					AntMediaApplicationAdapter.IP_CAMERA);
			assertNotNull(newCam.getStreamUrl());

			try {
				newCam.setStreamId((int)Math.random()*100000 + "");
			} catch (Exception e) {
				e.printStackTrace();
				fail(e.getMessage());
			}

			assertNotNull(newCam.getStreamId());

			StreamFetcher fetcher = new StreamFetcher(newCam, appScope, vertx);


			startCameraEmulator();

			// thread start 
			fetcher.startStream();

			Thread.sleep(6000);

			//check that thread is running
			assertTrue(fetcher.isThreadActive());
			assertTrue(fetcher.isStreamAlive());


			//stop thread
			fetcher.stopStream();

			Thread.sleep(5000);

			assertFalse(fetcher.isStreamAlive());
			assertFalse(fetcher.isThreadActive());

			//change the flag that shows thread is still running
			fetcher.setThreadActive(true);

			fetcher.debugSetStopRequestReceived(false);
			//start thread
			fetcher.startStream();

			Thread.sleep(6000);
			//check that thread is not started because thread active is true
			assertFalse(fetcher.isStreamAlive());
			assertTrue(fetcher.isThreadActive());


			logger.info("Change the flag that previous thread is stopped");
			//change the flag that previous thread is stopped
			fetcher.setThreadActive(false);

			//wait a little
			Thread.sleep(5000);

			//check that thread is started
			assertTrue(fetcher.isStreamAlive());
			assertTrue(fetcher.isThreadActive());

			fetcher.stopStream();

			Thread.sleep(6000);
			assertFalse(fetcher.isStreamAlive());
			assertFalse(fetcher.isThreadActive());

			stopCameraEmulator();

			Thread.sleep(3000);

		} catch (Exception e) {
			e.printStackTrace();
		}

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

		stopCameraEmulator();

	}



	@Test
	public void testCameraErrorCodes() {

		logger.info("starting testCameraErrorCodes");

		try {
			// start stream fetcher

			Broadcast newCam = new Broadcast("onvifCam2", "127.0.0.1:8080", "admin", "admin", "rtsp://10.122.59.79:6554/test.flv",
					AntMediaApplicationAdapter.IP_CAMERA);
			assertNotNull(newCam.getStreamUrl());

			try {
				newCam.setStreamId((int)Math.random()*100000 + "");
			} catch (Exception e) {
				e.printStackTrace();
				fail(e.getMessage());
			}

			assertNotNull(newCam.getStreamId());

			StreamFetcher fetcher = new StreamFetcher(newCam, appScope, vertx);
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

			assertNotNull(newCam2.getStreamId());

			StreamFetcher fetcher2 = new StreamFetcher(newCam2, appScope, vertx);
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

		} catch (Exception e) {
			e.printStackTrace();
		}
	}



	@Test
	public void testStreamFetcherBuffer() {

		try {	
			getAppSettings().setDeleteHLSFilesOnEnded(false);

			Broadcast newCam = new Broadcast("streamSource", "127.0.0.1:8080", "admin", "admin", 
					"src/test/resources/test_video_360p.flv",
					AntMediaApplicationAdapter.STREAM_SOURCE);

			assertNotNull(newCam.getStreamUrl());

			String id = getInstance().getDataStore().save(newCam);

			assertNotNull(newCam.getStreamId());

			StreamFetcher fetcher = new StreamFetcher(newCam, appScope, vertx);

			fetcher.setBufferTime(20000);

			fetcher.setRestartStream(false);

			assertFalse(fetcher.isThreadActive());
			assertFalse(fetcher.isStreamAlive());

			// start 
			fetcher.startStream();

			//wait for fetching stream
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

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

			Thread.sleep(2000);

			// start stream fetcher

			Broadcast newCam3 = new Broadcast("onvifCam4", "127.0.0.1:8080", "admin", "admin", "rtsp://127.0.0.1:6554/test.flv",
					AntMediaApplicationAdapter.IP_CAMERA);
			assertNotNull(newCam3.getStreamUrl());


			newCam3.setStreamId("stream_id_" + (int)(Math.random() * 100000));


			Thread.sleep(3000);


			StreamFetcher fetcher3 = new StreamFetcher(newCam3, appScope, vertx);
			fetcher3.setRestartStream(false);

			// thread start 
			fetcher3.startStream();

			Thread.sleep(6000);
			
			assertEquals(1, getInstance().getMuxAdaptors().size());

			String str3=fetcher3.getCameraError().getMessage();
			logger.info("error:   "+str3);

			assertNull(fetcher3.getCameraError().getMessage());
			assertTrue(fetcher3.isStreamAlive());

			fetcher3.stopStream();

			Thread.sleep(2000);
			
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
	public void testBugUnexpectedStream() 
	{
		
		AVFormatContext inputFormatContext = Mockito.mock(AVFormatContext.class);
		when(inputFormatContext.nb_streams()).thenReturn(1);
		
		AVStream stream = Mockito.mock(AVStream.class);
		when(inputFormatContext.streams(0)).thenReturn(stream);
		AVCodecParameters pars = Mockito.mock(AVCodecParameters.class);
		when(stream.codecpar()).thenReturn(pars);
		
		when(pars.codec_type()).thenReturn(AVMEDIA_TYPE_DATA);
		stream.codecpar(pars);
		
		Mp4Muxer mp4Muxer = Mockito.spy(new Mp4Muxer(null, null));
		
		mp4Muxer.init(appScope, "test", 480);
		
		Mockito.doReturn(true).when(mp4Muxer).isCodecSupported(Mockito.anyInt());
		
		mp4Muxer.addStream(pars, MuxAdaptor.TIME_BASE_FOR_MS);
		
		Mockito.verify(mp4Muxer, Mockito.never()).avNewStream(Mockito.any());
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
	public void testTSSourceAndBugStreamSpeed() {
		logger.info("running testTSSource");
		//test TS Source
		testFetchStreamSources("src/test/resources/nba.ts", false, false);
		logger.info("leaving testTSSource");
	}

	@Test
	public void testShoutcastSource() {
		logger.info("running testShoutcastSource");
		//test Southcast Source
		testFetchStreamSources("http://powerfm.listenpowerapp.com/powerfm/mpeg/icecast.audio", false, false);
		logger.info("leaving testShoutcastSource");
	}

	@Test
	public void testAudioOnlySource() {
		logger.info("running testAudioOnlySource");
		//test AudioOnly Source
		testFetchStreamSources("https://moondigitaledge.radyotvonline.net/karadenizfm/playlist.m3u8", false, false);
		logger.info("leaving testAudioOnlySource");
	}

	public void testFetchStreamSources(String source, boolean restartStream, boolean checkContext) {

		Application.enableSourceHealthUpdate = true;
		boolean deleteHLSFilesOnExit = getAppSettings().isDeleteHLSFilesOnEnded();
		try {
			getAppSettings().setDeleteHLSFilesOnEnded(false);

			Broadcast newCam = new Broadcast("streamSource", "127.0.0.1:8080", "admin", "admin", source,
					AntMediaApplicationAdapter.STREAM_SOURCE);

			assertNotNull(newCam.getStreamUrl());
			DataStore dataStore = getInstance().getDataStore();

			String id = dataStore.save(newCam);

			assertNotNull(newCam.getStreamId());

			StreamFetcher fetcher = new StreamFetcher(newCam, appScope, vertx);

			fetcher.setRestartStream(restartStream);

			assertFalse(fetcher.isThreadActive());
			assertFalse(fetcher.isStreamAlive());

			// start 
			fetcher.startStream();
			
			//wait for fetching stream
			if (checkContext) {
				Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> {
					// This issue is the check of #1600
					return fetcher.getMuxAdaptor() != null && fetcher.getMuxAdaptor().isEnableAudio();
				});
			}
	
			Awaitility.await().pollDelay(5, TimeUnit.SECONDS).atMost(10, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(()-> {
				double speed = dataStore.get(newCam.getStreamId()).getSpeed();
				//this value was so high over 9000. After using first packet time it's value is about 100-200
				//it is still high and it is normal because it reads vod from disk it does not read live stream.
				//Btw, nba.ts , in testTSSourceAndBugStreamSpeed, is generated specifically by copying timestamps directy
				//from live stream by using copyts parameter in ffmpeg 
				logger.info("Speed of the stream: {}", speed);
				return speed < 1000;
			});
			

			Thread.sleep(3000);

			//wait for packaging files
			fetcher.stopStream();
			

			String mp4File = "webapps/junit/streams/"+newCam.getStreamId() +".mp4";

			Awaitility.waitAtMost(10, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
				return new File(mp4File).exists();
			});
			

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
	public void testStopRequestReceived() {
		Broadcast stream = new Broadcast("streamSource", "127.0.0.1:8080", "admin", "admin", "rtsp://localhost:44332/this_does_not_exist",
				AntMediaApplicationAdapter.STREAM_SOURCE);
		DataStore dataStore = getInstance().getDataStore();
		String id = dataStore.save(stream);
		
		StreamFetcher fetcher = new StreamFetcher(stream, appScope, vertx);
		
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

			StreamFetcher fetcher = new StreamFetcher(newCam, appScope, vertx);

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
			appInstance = ((IApplicationAdaptorFactory) applicationContext.getBean("web.handler")).getAppAdaptor();
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
			
			StreamFetcher camScheduler = new StreamFetcher(newCam, appScope, vertx);
			
			camScheduler.setConnectionTimeout(10000);

			camScheduler.startStream();
			
			Awaitility.await().atMost(15, TimeUnit.SECONDS).until(() -> camScheduler.getMuxAdaptor() != null);			
			Thread.sleep(2000);
			assertTrue(camScheduler.getMuxAdaptor().startRecording(RecordType.MP4));
			Thread.sleep(5000);
			assertTrue(camScheduler.getMuxAdaptor().stopRecording(RecordType.MP4));
			Thread.sleep(2000);
			camScheduler.stopStream();
			assertTrue(MuxingTest.testFile("webapps/junit/streams/"+newCam.getStreamId() +".mp4"));
			apps.setMp4MuxingEnabled(mp4Recording);
		}
		catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}


}
