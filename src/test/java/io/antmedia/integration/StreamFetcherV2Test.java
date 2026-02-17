package io.antmedia.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.RandomStringUtils;
import org.awaitility.Awaitility;
import org.bytedeco.ffmpeg.global.avformat;
import org.bytedeco.ffmpeg.global.avutil;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.mockito.Mockito;
import org.red5.server.scheduling.QuartzSchedulingService;
import org.red5.server.scope.WebScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.AppSettings;
import io.antmedia.datastore.db.DataStore;
import io.antmedia.datastore.db.DataStoreFactory;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.datastore.db.types.Endpoint;
import io.antmedia.rest.model.Result;
import io.antmedia.streamsource.StreamFetcher;
import io.vertx.core.Vertx;

@ContextConfiguration(locations = { "../test/test.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class StreamFetcherV2Test extends AbstractJUnit4SpringContextTests{

	public static final int MAC_OS_X = 0;
	public static final int LINUX = 1;
	public static final int WINDOWS = 2;

	public static final String BIG_BUNNY_MP4_URL = "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4";

	private static int OS_TYPE;

	static {
		System.setProperty("red5.deployment.type", "junit");
		System.setProperty("red5.root", ".");
	}

	static {
		String osName = System.getProperty("os.name", "").toLowerCase();
		if (osName.startsWith("mac os x") || osName.startsWith("darwin")) {
			OS_TYPE = MAC_OS_X;
		} else if (osName.startsWith("windows")) {
			OS_TYPE = WINDOWS;
		} else if (osName.startsWith("linux")) {
			OS_TYPE = LINUX;
		}
	}


	private WebScope appScope;
	protected static Logger logger = LoggerFactory.getLogger(StreamFetcherV2Test.class);
	public AntMediaApplicationAdapter app = null;
	private AntMediaApplicationAdapter appInstance;
	private AppSettings appSettings;
	private QuartzSchedulingService scheduler;

	private static String ffmpegPath = "ffmpeg";

	@Rule
	public TestRule watcher = new TestWatcher() {
		protected void starting(Description description) {
			System.out.println("Starting test: " + description.getMethodName());
		}

		protected void failed(Throwable e, Description description) {
			System.out.println("Failed test: " + description.getMethodName() );
			e.printStackTrace();
		};
		protected void finished(Description description) {
			System.out.println("Finishing test: " + description.getMethodName());
		};
	};

	@BeforeClass
	public static void beforeClass() {
		if (OS_TYPE == MAC_OS_X) {
			ffmpegPath = "/usr/local/bin/ffmpeg";
		}
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

			app = ((AntMediaApplicationAdapter) applicationContext.getBean("web.handler"));
			logger.debug("Application / web scope: {}", appScope);
			assertTrue(appScope.getDepth() == 1);
		}

		scheduler = (QuartzSchedulingService) applicationContext.getBean(QuartzSchedulingService.BEAN_NAME);

		getAppSettings().resetDefaults();
		getAppSettings().setMp4MuxingEnabled(true);
	}



	public AppSettings getAppSettings() {
		if (appSettings == null) {
			appSettings = (AppSettings) applicationContext.getBean(AppSettings.BEAN_NAME);
		}
		return appSettings;
	}


	@Test
	public void testUpdateStreamSource() {
		RestServiceV2Test restService = new RestServiceV2Test();
		String name = "test";
		String streamUrl = "rtmp://127.0.0.1/LiveApp/streamtest";
		Broadcast streamSource = restService.createBroadcast("test", "streamSource", "rtmp://127.0.0.1/LiveApp/streamtest", null);

		assertNotNull(streamSource);
		assertEquals(name, streamSource.getName());
		assertEquals(streamUrl, streamSource.getStreamUrl());

		name = "test2";
		String streamUrl2 = "rtmp://127.0.0.1/LiveApp/test1234";
		Result result = restService.callUpdateBroadcast(streamSource.getStreamId(), name, null, "", streamUrl2, "streamSource", null);
		assertTrue(result.isSuccess());

		Broadcast returnedBroadcast;
		try {
			returnedBroadcast = restService.callGetBroadcast(streamSource.getStreamId());
			assertEquals(name, returnedBroadcast.getName());
			assertEquals(streamUrl2, returnedBroadcast.getStreamUrl());

			result = restService.callDeleteBroadcast(streamSource.getStreamId());
			assertTrue(result.isSuccess());


		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}


	}

	@Test
	public void testVoDFetchAndRTMPPush() {
		//create a stream fetcher broadcast with VoD type by pointing to the following url 
		//BIG_BUNNY_MP4_URL
		RestServiceV2Test restService = new RestServiceV2Test();
		String name = "test";
		String streamUrl = BIG_BUNNY_MP4_URL;
		//"rtsp://wowzaec2demo.streamlock.net/vod/mp4:BigBuckBunny_115k.mov"; 
		String type = AntMediaApplicationAdapter.VOD; //AntMediaApplicationAdapter.STREAM_SOURCE;
		Broadcast streamSource = restService.createBroadcast("test", type, streamUrl, null);

		assertNotNull(streamSource);

		//start streaming
		Result result = restService.startStreaming(streamSource.getStreamId());
		assertTrue(result.isSuccess());

		//check that m3u8 file is created and working
		Awaitility.await().atMost(10, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
			return MuxingTest.testFile("http://" + AppFunctionalV2Test.SERVER_ADDR + ":5080/LiveApp/streams/" + streamSource.getStreamId() + ".m3u8");
		});

		//add rtmp endpoint 
		Endpoint endpoint = new Endpoint();
		String endpointStreamId = "endpoint_" + (int)(Math.random()*10000);
		endpoint.setEndpointUrl("rtmp://127.0.0.1/LiveApp/" + endpointStreamId);
		try 
		{
			result = RestServiceV2Test.addEndpointV3(streamSource.getStreamId(), endpoint);
			assertTrue(result.isSuccess());
			String endpointId = result.getDataId();
			//check that rtmp endpoint is streaming

			Awaitility.await().atMost(10, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
				return RestServiceV2Test.callGetBroadcast(endpointStreamId) != null;
			});

			Awaitility.await().atMost(10, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
				return MuxingTest.testFile("http://" + AppFunctionalV2Test.SERVER_ADDR + ":5080/LiveApp/streams/" + endpointStreamId + ".m3u8");
			});

			//remove rtmp endpoint
			result = RestServiceV2Test.removeEndpointV2(streamSource.getStreamId(), endpointId);

			//check that rtmp endpoint is not streaming
			assertTrue(result.isSuccess());

			//stop pulling stream source streaming
			result = restService.stopStreaming(streamSource.getStreamId());
			assertTrue(result.isSuccess());

			result = RestServiceV2Test.callDeleteBroadcast(streamSource.getStreamId());
			assertTrue(result.isSuccess());

			//end point should be null because it is deleted
			Awaitility.await().atMost(10, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
				return RestServiceV2Test.callGetBroadcast(endpointStreamId) == null;
			});

			Awaitility.await().atMost(10, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
				return RestServiceV2Test.callGetBroadcast(streamSource.getStreamId()) == null;
			});

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail(e.getMessage());
		}


	}


	@Test
	public void testSetupEndpointStreamFetcher() {
		RestServiceV2Test restService = new RestServiceV2Test();

		List<Broadcast> broadcastList = restService.callGetBroadcastList();

		Broadcast endpointStream = restService.createBroadcast("endpoint_stream");

		DataStore dataStore = app.getDataStore();

		String streamId = RandomStringUtils.randomAlphanumeric(8);
		Process rtmpSendingProcess = AppFunctionalV2Test.execute(ffmpegPath
				+ " -re -i src/test/resources/test.flv  -codec copy -f flv rtmp://127.0.0.1/LiveApp/"
				+ streamId);

		Awaitility.await().atMost(40, TimeUnit.SECONDS).pollInterval(2, TimeUnit.SECONDS)
		.until(() -> {
			Broadcast broadcast = restService.getBroadcast(streamId);
			return broadcast != null && broadcast.getStatus() != null && 
					broadcast.getStatus().equals(AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING);
		});

		//create a local stream
		//add librtmp style in the url
		Broadcast localStream = new Broadcast("name", null, null, null, "http://127.0.0.1:5080/LiveApp/streams/"+ streamId + ".m3u8", AntMediaApplicationAdapter.STREAM_SOURCE);
		dataStore.save(localStream);

		Endpoint endpoint = new Endpoint();
		endpoint.setEndpointUrl(endpointStream.getRtmpURL());
		//add endpoint to the server
		dataStore.addEndpoint(localStream.getStreamId(), endpoint);

		DataStoreFactory dsf = Mockito.mock(DataStoreFactory.class);
		Mockito.when(dsf.getDataStore()).thenReturn(dataStore);

		app.setDataStoreFactory(dsf);

		//create stream fetcher
		StreamFetcher streamFetcher = new StreamFetcher(localStream.getStreamUrl(), localStream.getStreamId(), localStream.getType(), appScope, Vertx.vertx(), 0);

		//start stream fetcher
		streamFetcher.startStream();

		//check that server has the stream

		Awaitility.await().atMost(250, TimeUnit.SECONDS).pollInterval(2, TimeUnit.SECONDS)
		.until(() -> {
			return restService.getBroadcast(endpointStream.getStreamId()).getStatus().equals(AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING);
		});

		//Check start time
		Broadcast broadcast = restService.getBroadcast(endpointStream.getStreamId());
		assertNotNull(broadcast);
		long now = System.currentTimeMillis();
		//broadcast start time should be at most 5 sec before now
		assertTrue((now-broadcast.getStartTime()) < 5000);
		
		assertTrue(streamFetcher.isThreadActive());
		
		//stop stream fetcher
		streamFetcher.stopStream();

		rtmpSendingProcess.destroy();
		//delete stream on the server
		Result result = restService.callDeleteBroadcast(endpointStream.getStreamId());
		assertTrue(result.isSuccess());

		Awaitility.await().atMost(20, TimeUnit.SECONDS)
		.until(() -> {
			return restService.getBroadcast(streamId) == null;
		});	

		Awaitility.await().atMost(10, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS)
		.until(() -> {
			return broadcastList.size() == restService.callGetBroadcastList().size();
		});	
		
		//Make sure thread is stopped
		Awaitility.await().atMost(20, TimeUnit.SECONDS)
		.until(() -> {
		   return !streamFetcher.isThreadActive();
		});
		
		
	}

	@Test
	public void testRtmpPull() throws Exception {

		ConsoleAppRestServiceTest.resetCookieStore();
		Result result;

		result = ConsoleAppRestServiceTest.callisFirstLogin();

		if (result.isSuccess()) {
			Result createInitialUser = ConsoleAppRestServiceTest.createDefaultInitialUser();
			assertTrue(createInitialUser.isSuccess());
		}

		result = ConsoleAppRestServiceTest.authenticateDefaultUser();
		assertTrue(result.isSuccess());

		RestServiceV2Test restService = new RestServiceV2Test();

		AppSettings appSettingsModel = ConsoleAppRestServiceTest.callGetAppSettings("LiveApp");
		appSettingsModel.setRtmpPlaybackEnabled(true);

		result = ConsoleAppRestServiceTest.callSetAppSettings("LiveApp", appSettingsModel);
		assertTrue(result.isSuccess());

		String rtmpPullStreamName = "rtmpPullStream" + (int)(Math.random()*10000);
		String rtmpStreamName = "rtmpStream" + (int)(Math.random()*10000);

		Broadcast rtmpNormalStream = restService.createBroadcast(rtmpStreamName, AntMediaApplicationAdapter.LIVE_STREAM, null, null);
		String rtmpNormalStreamId = rtmpNormalStream.getStreamId();

		Process rtmpSendingProcess = AppFunctionalV2Test.execute(ffmpegPath
				+ " -re -i src/test/resources/test.flv  -codec copy -f flv rtmp://127.0.0.1/LiveApp/"
				+ rtmpNormalStreamId);

		Awaitility.await().atMost(40, TimeUnit.SECONDS).pollInterval(2, TimeUnit.SECONDS)
				.until(() -> {
					Broadcast broadcast = restService.getBroadcast(rtmpNormalStreamId);
					return broadcast != null && broadcast.getStatus() != null &&
							broadcast.getStatus().equals(AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING);
				});

		Broadcast rtmpPullStream = restService.createBroadcast(rtmpPullStreamName, AntMediaApplicationAdapter.STREAM_SOURCE, "rtmp://127.0.0.1/LiveApp/"+ rtmpNormalStreamId , null);
		String rtmpPullStreamId = rtmpPullStream.getStreamId();
		result = restService.startStreaming(rtmpPullStreamId);
		assertTrue(result.isSuccess());
		Awaitility.await().atMost(40, TimeUnit.SECONDS).pollInterval(2, TimeUnit.SECONDS)
				.until(() -> {
					Broadcast broadcast = restService.getBroadcast(rtmpPullStreamId);
					return broadcast != null && broadcast.getStatus() != null &&
							broadcast.getStatus().equals(AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING);
				});

		rtmpNormalStream = restService.getBroadcast(rtmpNormalStreamId);
		assertTrue(rtmpNormalStream.getRtmpViewerCount() == 1);
		Thread.sleep(5000);

		rtmpSendingProcess.destroy();


		result = restService.callDeleteBroadcast(rtmpNormalStreamId);
		assertTrue(result.isSuccess());

		result = restService.callDeleteBroadcast(rtmpPullStreamId);
		assertTrue(result.isSuccess());

		appSettings.resetDefaults();
		result = ConsoleAppRestServiceTest.callSetAppSettings("LiveApp", appSettings);
		assertTrue(result.isSuccess());

	}



}
