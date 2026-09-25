package io.antmedia.test;

import static org.bytedeco.ffmpeg.global.avcodec.av_packet_unref;
import static org.bytedeco.ffmpeg.global.avformat.av_read_frame;
import static org.bytedeco.ffmpeg.global.avformat.avformat_alloc_context;
import static org.bytedeco.ffmpeg.global.avformat.avformat_find_stream_info;
import static org.bytedeco.ffmpeg.global.avformat.avformat_open_input;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.RandomUtils;
import org.awaitility.Awaitility;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.avutil.AVDictionary;
import org.bytedeco.ffmpeg.global.avformat;
import org.bytedeco.ffmpeg.global.avutil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.red5.server.scope.WebScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.AppSettings;
import io.antmedia.FFmpegUtilities;
import io.antmedia.datastore.db.DataStore;
import io.antmedia.datastore.db.InMemoryDataStore;
import io.antmedia.datastore.db.MapDBStore;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.integration.AppFunctionalV2Test;
import io.antmedia.integration.CameraEmulator;
import io.antmedia.rest.BroadcastRestService;
import io.antmedia.rest.model.Result;
import io.antmedia.streamsource.StreamFetcher;
import io.antmedia.streamsource.StreamFetcherManager;
import io.vertx.core.Vertx;

@ContextConfiguration(locations = { "test.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
@ExtendWith(SpringExtension.class)
public class StreamSchedularUnitTest {

	@Autowired
	private ApplicationContext applicationContext;

	public Application app = null;
	public static String VALID_MP4_URL = "https://avtshare01.rz.tu-ilmenau.de/avt-vqdb-uhd-1/test_1/segments/bigbuck_bunny_8bit_750kbps_720p_60.0fps_h264.mp4";
	public static String INVALID_MP4_URL = "invalid_link";
	private WebScope appScope;
	protected static Logger logger = LoggerFactory.getLogger(StreamSchedularUnitTest.class);

	static {
		System.setProperty("red5.deployment.type", "junit");
		System.setProperty("red5.root", ".");


	}

	private AntMediaApplicationAdapter appInstance;
	private AppSettings appSettings;
	private Vertx vertx;


	@BeforeAll
	public static void beforeClass() {
		//avformat.av_register_all();
		avformat.avformat_network_init();
	}

	@BeforeEach
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

	@AfterEach
	public void after() {

		try {
			AppFunctionalV2Test.delete(new File("webapps"));
		} catch (IOException e) {
			e.printStackTrace();
		}

		//reset to default
		Application.enableSourceHealthUpdate = false;

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

		CameraEmulator.start();

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
		CameraEmulator.stop();


		Awaitility.await().atMost(10, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() ->  {
			return !streamFetcher.isThreadActive();
		});

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
		CameraEmulator.start();

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
		CameraEmulator.stop();


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
		CameraEmulator.start();

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
		CameraEmulator.stop();


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

	public AppSettings getAppSettings() {
		if (appSettings == null) {
			appSettings = (AppSettings) applicationContext.getBean(AppSettings.BEAN_NAME);
		}
		return appSettings;
	}

	@Test
	public void testBroadcastStatusForStreamSource()
	{
		CameraEmulator.start();
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
				return !streamFetcher.isThreadActive();
			});


			Result stopStreaming2 = fetcherManager.stopStreaming(nonExistingStreamSource, false);
			assertTrue(stopStreaming2.isSuccess());

			//the registry entry goes on the STOPPED transition, not on the stop call, so the id is
			//still taken here and a second stop is still accepted
			assertTrue(fetcherManager.stopStreaming(nonExistingStreamSource, false).isSuccess());

			Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> {
				return fetcherManager.getStreamFetcherList().size() == 0;
			});

			assertFalse(fetcherManager.stopStreaming(nonExistingStreamSource, false).isSuccess());

		}
		catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		CameraEmulator.stop();


	}

}
