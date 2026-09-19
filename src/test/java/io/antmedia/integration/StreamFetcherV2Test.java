package io.antmedia.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.RandomStringUtils;
import org.awaitility.Awaitility;
import org.bytedeco.ffmpeg.global.avformat;
import org.bytedeco.ffmpeg.global.avutil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.red5.server.scheduling.QuartzSchedulingService;
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
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.datastore.db.types.Endpoint;
import io.antmedia.rest.model.Result;

@ContextConfiguration(locations = { "../test/test.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
@ExtendWith(SpringExtension.class)
public class StreamFetcherV2Test {

	@Autowired
	private ApplicationContext applicationContext;

	public static final int MAC_OS_X = 0;
	public static final int LINUX = 1;
	public static final int WINDOWS = 2;

	public static final String BIG_BUNNY_MP4_URL = "https://avtshare01.rz.tu-ilmenau.de/avt-vqdb-uhd-1/test_1/segments/bigbuck_bunny_8bit_750kbps_720p_60.0fps_h264.mp4";

	private static final Random RANDOM = new Random();

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

	@BeforeAll
	public static void beforeClass() {
		if (OS_TYPE == MAC_OS_X) {
			ffmpegPath = "/usr/local/bin/ffmpeg";
		}
		//	avformat.av_register_all();
		avformat.avformat_network_init();
		avutil.av_log_set_level(avutil.AV_LOG_INFO);

	}

	@BeforeEach
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
			result = RestServiceV2Test.addEndpoint(streamSource.getStreamId(), endpoint);
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
			result = RestServiceV2Test.removeEndpoint(streamSource.getStreamId(), endpointId);

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


	/**
	 * The pull path and endpoint republish end to end, over REST only. An rtmp publish into this app is
	 * pulled back out of its own hls output as a stream source, which republishes to a second broadcast
	 * in the same app. That second broadcast can only go live if the fetcher opened the source, built a
	 * MuxAdaptor and set its endpoints up, so it covers the whole chain in one signal.
	 */
	@Test
	public void testSetupEndpointStreamFetcher() {
		RestServiceV2Test restService = new RestServiceV2Test();
		String publishedStreamId = RandomStringUtils.randomAlphanumeric(8);
		String hlsUrl = "http://127.0.0.1:5080/LiveApp/streams/" + publishedStreamId + ".m3u8";
		Process rtmpPublisher = AppFunctionalV2Test.execute(ffmpegPath
				+ " -re -i src/test/resources/test.flv -codec copy -f flv rtmp://127.0.0.1/LiveApp/" + publishedStreamId);
		String sourceId = null;
		String targetId = null;

		try {
			Awaitility.await().atMost(40, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
				Broadcast published = restService.getBroadcast(publishedStreamId);
				return published != null && AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING.equals(published.getStatus());
			});

			//the source cannot be opened before the playlist has segments to hand out
			Awaitility.await().atMost(40, TimeUnit.SECONDS).pollInterval(2, TimeUnit.SECONDS).until(() ->
					MuxingTest.testFile(hlsUrl));

			//what the source republishes into, an ordinary broadcast of this same app
			Broadcast target = restService.createBroadcast("endpoint target");
			targetId = target.getStreamId();
			final String activeTargetId = targetId;

			Broadcast source = restService.createBroadcast("endpoint source", AntMediaApplicationAdapter.STREAM_SOURCE, hlsUrl, null);
			sourceId = source.getStreamId();
			final String activeSourceId = sourceId;

			Endpoint endpoint = new Endpoint();
			endpoint.setEndpointUrl(target.getRtmpURL());
			assertTrue(RestServiceV2Test.addEndpoint(activeSourceId, endpoint).isSuccess());

			//fail here, rather than as a timeout below, if the endpoint never landed on the broadcast
			assertEquals(1, restService.getBroadcast(activeSourceId).getEndPointList().size());

			assertTrue(restService.startStreaming(activeSourceId).isSuccess());

			Awaitility.await().atMost(30, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() ->
					AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING.equals(restService.getBroadcast(activeSourceId).getStatus()));

			//the target goes live only once packets actually reach it through the endpoint muxer
			Awaitility.await().atMost(60, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
				Broadcast republished = restService.getBroadcast(activeTargetId);
				return republished != null && AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING.equals(republished.getStatus());
			});

			//a start time from this publish, not one left over from when the target was created
			Broadcast republished = restService.getBroadcast(activeTargetId);
			assertTrue(System.currentTimeMillis() - republished.getStartTime() < 10000,
					"the republished broadcast should have just started, its start time is " + republished.getStartTime());

			//stopping the source has to take the republish down with it, not leave the target live
			assertTrue(restService.stopStreaming(activeSourceId).isSuccess());

			Awaitility.await().atMost(20, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() ->
					!AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING.equals(restService.getBroadcast(activeSourceId).getStatus()));

			Awaitility.await().atMost(20, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
				Broadcast stopped = restService.getBroadcast(activeTargetId);
				return stopped == null || !AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING.equals(stopped.getStatus());
			});
		}
		catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		finally {
			//run even if an assertion above failed - otherwise a failure here leaves a live fetcher
			//running for the rest of the class's shared Spring context (@DirtiesContext is AFTER_CLASS)
			if (sourceId != null) {
				restService.stopStreaming(sourceId);
				RestServiceV2Test.callDeleteBroadcast(sourceId);
			}
			if (targetId != null) {
				RestServiceV2Test.callDeleteBroadcast(targetId);
			}
			rtmpPublisher.destroy();
		}
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
		boolean originalRtmpPlaybackEnabled = appSettingsModel.isRtmpPlaybackEnabled();
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

		//restore only what this test changed - pushing resetDefaults() here used to blanket-overwrite
		//every LiveApp setting mid-class
		appSettingsModel = ConsoleAppRestServiceTest.callGetAppSettings("LiveApp");
		appSettingsModel.setRtmpPlaybackEnabled(originalRtmpPlaybackEnabled);
		result = ConsoleAppRestServiceTest.callSetAppSettings("LiveApp", appSettingsModel);
		assertTrue(result.isSuccess());

	}

	@Test
	public void testSeekTimeRefusedWhenSourceNotRunning() {
		RestServiceV2Test restService = new RestServiceV2Test();
		String streamId = null;
		try {
			//never started, so no StreamFetcher is registered for it - seek-time must refuse, not NPE
			Broadcast source = restService.createBroadcast("seek refusal test", AntMediaApplicationAdapter.STREAM_SOURCE, "srt://127.0.0.1:" + freeSrtPort(), null);
			streamId = source.getStreamId();

			Result result = RestServiceV2Test.callSeekTime(streamId, 5000);
			assertFalse(result.isSuccess());
		}
		catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		finally {
			if (streamId != null) {
				RestServiceV2Test.callDeleteBroadcast(streamId);
			}
		}
	}

	@Test
	public void testSeekTimeOnRunningVodSource() {
		RestServiceV2Test restService = new RestServiceV2Test();
		Broadcast vodSource = null;
		try {
			//playlists register their active item's StreamFetcher under the playlist's own streamId
			//(see StreamFetcherManager#startPlaylist/createAndStartNextPlaylistItem), so seek-time on a
			//running playlist item goes through this exact same getStreamFetcher(id)+seekTime() path -
			//no separate playlist-specific test needed here.
			vodSource = restService.createBroadcast("seek vod test", AntMediaApplicationAdapter.VOD, BIG_BUNNY_MP4_URL, null);
			final String streamId = vodSource.getStreamId();

			Result result = restService.startStreaming(streamId);
			assertTrue(result.isSuccess());

			Awaitility.await().atMost(10, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() ->
					MuxingTest.testFile("http://" + AppFunctionalV2Test.SERVER_ADDR + ":5080/LiveApp/streams/" + streamId + ".m3u8"));

			result = RestServiceV2Test.callSeekTime(streamId, 5000);
			assertTrue(result.isSuccess());
		}
		catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		finally {
			//run even if an assertion above failed - otherwise a failure here leaves a live VoD fetcher
			//running for the rest of the class's shared Spring context (@DirtiesContext is AFTER_CLASS)
			if (vodSource != null) {
				restService.stopStreaming(vodSource.getStreamId());
				RestServiceV2Test.callDeleteBroadcast(vodSource.getStreamId());
			}
		}
	}

	@Test
	public void testIpCameraErrorReflectsLastConnectionAttempt() {
		Process publisher = null;
		RestServiceV2Test restService = new RestServiceV2Test();
		String streamId = null;
		try {
			int port = freeSrtPort();
			//nothing listens on this port yet - the first connection attempt must fail
			Broadcast source = restService.createBroadcast("camera error test", AntMediaApplicationAdapter.STREAM_SOURCE, "srt://127.0.0.1:" + port, null);
			streamId = source.getStreamId();

			Result result = restService.startStreaming(streamId);
			assertTrue(result.isSuccess());

			String finalStreamId = streamId;
			Awaitility.await().atMost(10, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
				Result cameraError = RestServiceV2Test.callGetCameraError(finalStreamId);
				return !cameraError.isSuccess() && cameraError.getMessage() != null && !cameraError.getMessage().isEmpty();
			});

			//bring the source up - StreamFetcher retries every 3s (STREAM_FETCH_RE_TRY_PERIOD_MS), so the
			//next attempt should succeed on its own without restarting the fetcher
			publisher = startSrtPublisher(port);

			Awaitility.await().atMost(15, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() ->
					RestServiceV2Test.callGetCameraError(finalStreamId).isSuccess());
		}
		catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		finally {
			//run even if an assertion above failed - otherwise a failure here leaves a live fetcher
			//running for the rest of the class's shared Spring context (@DirtiesContext is AFTER_CLASS)
			if (streamId != null) {
				restService.stopStreaming(streamId);
				RestServiceV2Test.callDeleteBroadcast(streamId);
			}
			stopPublisher(publisher);
		}
	}

	@Test
	public void testDeleteWhileStreamSourceActivelyBroadcasting() {
		int port = freeSrtPort();
		Process publisher = startSrtPublisher(port);
		RestServiceV2Test restService = new RestServiceV2Test();
		//the recreate step below deliberately reuses this id, so one cleanup covers both broadcasts
		final String streamId = "delete-active-" + RandomStringUtils.randomAlphanumeric(6);
		try {
			Broadcast source = new Broadcast();
			source.setStreamId(streamId);
			source.setType(AntMediaApplicationAdapter.STREAM_SOURCE);
			source.setStreamUrl("srt://127.0.0.1:" + port);
			RestServiceV2Test.createBroadcast(source);

			Result result = restService.startStreaming(streamId);
			assertTrue(result.isSuccess());

			Awaitility.await().atMost(15, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
				Broadcast broadcast = restService.getBroadcast(streamId);
				return broadcast != null && AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING.equals(broadcast.getStatus());
			});

			//delete while still actively pulling - must tear down cleanly, not error or leave a zombie fetcher
			result = RestServiceV2Test.callDeleteBroadcast(streamId);
			assertTrue(result.isSuccess());
			assertNull(RestServiceV2Test.callGetBroadcast(streamId));

			//the streamId must be immediately reusable, not left blocked by leftover state
			Broadcast recreated = new Broadcast();
			recreated.setStreamId(streamId);
			recreated.setType(AntMediaApplicationAdapter.STREAM_SOURCE);
			recreated.setStreamUrl("srt://127.0.0.1:" + port);
			Broadcast created = RestServiceV2Test.createBroadcast(recreated);
			assertEquals(streamId, created.getStreamId());
		}
		catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		finally {
			//covers the original too if the delete under test failed, and it may still be pulling
			restService.stopStreaming(streamId);
			RestServiceV2Test.callDeleteBroadcast(streamId);
			stopPublisher(publisher);
		}
	}

	@Test
	public void testCreateListBulkForStreamSourceAndPlaylist() {
		String prefix = "bulk_" + RandomStringUtils.randomAlphanumeric(6) + "_";
		try {
			Broadcast streamSource1 = new Broadcast();
			streamSource1.setStreamId(prefix + "source1");
			streamSource1.setType(AntMediaApplicationAdapter.STREAM_SOURCE);
			streamSource1.setStreamUrl("srt://127.0.0.1:" + freeSrtPort());

			Broadcast streamSource2 = new Broadcast();
			streamSource2.setStreamId(prefix + "source2");
			streamSource2.setType(AntMediaApplicationAdapter.STREAM_SOURCE);
			streamSource2.setStreamUrl("srt://127.0.0.1:" + freeSrtPort());

			Broadcast playlist = new Broadcast();
			playlist.setStreamId(prefix + "playlist");
			playlist.setType(AntMediaApplicationAdapter.PLAY_LIST);

			List<Result> results = RestServiceV2Test.callCreateBroadcastList(Arrays.asList(streamSource1, streamSource2, playlist), null);
			assertEquals(3, results.size());
			assertEquals(prefix + "source1", results.get(0).getDataId());
			assertEquals(prefix + "source2", results.get(1).getDataId());
			assertEquals(prefix + "playlist", results.get(2).getDataId());
			for (Result result : results) {
				assertTrue(result.isSuccess());
				assertEquals("created", result.getMessage());
			}

			assertEquals(AntMediaApplicationAdapter.STREAM_SOURCE, RestServiceV2Test.callGetBroadcast(prefix + "source1").getType());
			assertEquals(AntMediaApplicationAdapter.STREAM_SOURCE, RestServiceV2Test.callGetBroadcast(prefix + "source2").getType());
			assertEquals(AntMediaApplicationAdapter.PLAY_LIST, RestServiceV2Test.callGetBroadcast(prefix + "playlist").getType());
		}
		catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		finally {
			RestServiceV2Test.callDeleteBroadcast(prefix + "source1");
			RestServiceV2Test.callDeleteBroadcast(prefix + "source2");
			RestServiceV2Test.callDeleteBroadcast(prefix + "playlist");
		}
	}

	@Test
	public void testStreamSourceSelfHealsAfterTransientDrop() {
		int port = freeSrtPort();
		Process publisher = startSrtPublisher(port);
		RestServiceV2Test restService = new RestServiceV2Test();
		String streamId = null;
		try {
			//only meaningful if nothing forces a restart behind its back. @BeforeEach resets the local
			//AppSettings bean, not the server's, so check the server's
			ConsoleAppRestServiceTest.resetCookieStore();
			assertTrue(new ConsoleAppRestServiceTest().createFirstUserAndLogin());
			assertEquals(0, ConsoleAppRestServiceTest.callGetAppSettings("LiveApp").getRestartStreamFetcherPeriod(),
					"restartStreamFetcherPeriod must be 0 here, otherwise controlStreamFetchers(true) forces a "
					+ "stop+restart and this stops testing StreamFetcher's own retry loop");

			Broadcast source = restService.createBroadcast("self heal test", AntMediaApplicationAdapter.STREAM_SOURCE, "srt://127.0.0.1:" + port, null);
			streamId = source.getStreamId();
			final String activeStreamId = streamId;

			Result result = restService.startStreaming(activeStreamId);
			assertTrue(result.isSuccess());

			Awaitility.await().atMost(15, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() ->
					AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING.equals(restService.getBroadcast(activeStreamId).getStatus()));

			//plain network blip: nothing forces a restart, so only StreamFetcher's own 3s retry loop
			//(STREAM_FETCH_RE_TRY_PERIOD_MS) can heal this
			stopPublisher(publisher);
			publisher = null;

			//confirm the drop was actually detected (status leaves broadcasting) before bringing the
			//source back, so this proves reconnection, not tolerance of an outage nobody noticed
			Awaitility.await().atMost(10, TimeUnit.SECONDS).pollInterval(200, TimeUnit.MILLISECONDS).until(() ->
					!AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING.equals(restService.getBroadcast(activeStreamId).getStatus()));

			publisher = startSrtPublisher(port);

			Awaitility.await().atMost(20, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() ->
					AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING.equals(restService.getBroadcast(activeStreamId).getStatus()));
		}
		catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		finally {
			//run even if an assertion above failed - otherwise a failure here leaves a live fetcher
			//running for the rest of the class's shared Spring context (@DirtiesContext is AFTER_CLASS)
			if (streamId != null) {
				restService.stopStreaming(streamId);
				RestServiceV2Test.callDeleteBroadcast(streamId);
			}
			stopPublisher(publisher);
		}
	}

	//TODO: expected to fail on current master - real bug found 2026-09-17, see TODO-Progress.md checkpoint
	//6. Root cause: InMemoryDataStore#getExternalStreamsList() stores live Broadcast object references,
	//not copies. It adds a broadcast to the returned list, then mutates that SAME object's status to
	//"preparing" in place before persisting it. appStart()'s auto-resume loop then calls
	//startStreaming(broadcast, true) on that already-mutated object; StreamFetcherManager#isStreamRunning()
	//reads status=preparing, treats it as already streaming, and (since originAdress is blank so
	//isInstanceAlive() trivially returns true) refuses to start it as "already active". Net effect: with
	//InMemoryDataStore, boot-time auto-resume self-sabotages and never actually starts anything. Checked
	//MapBasedDataStore and MongoStore's getExternalStreamsList() - both return a snapshot taken before the
	//status mutation, so this looks InMemoryDataStore-specific, not necessarily hitting MapDB/Mongo-backed
	//production - not verified end-to-end against those though. Don't hack this test to pass - revisit
	//after the state machine rewrite (or as its own fix, discuss with user first).
	@Test
	public void testBootTimeAutoResumeStartsUnattendedStreamSource() throws Exception {
		//drives the embedded adapter directly, so the local bean is the right one here - the REST-driven
		//tests in this class hit the server's instead
		getAppSettings().setStartStreamFetcherAutomatically(true);

		int port = freeSrtPort();
		Process publisher = startSrtPublisher(port);
		String cleanupStreamId = null;
		try {
			final String streamId = "boot-resume-" + RandomStringUtils.randomAlphanumeric(6);
			cleanupStreamId = streamId;
			Broadcast source = new Broadcast("boot resume test", null, null, null, "srt://127.0.0.1:" + port, AntMediaApplicationAdapter.STREAM_SOURCE);
			source.setStreamId(streamId);
			//this is exactly the set AntMediaApplicationAdapter#appStart's vertx.setTimer(1000, ...) block
			//targets: getExternalStreamsList() + !isAutoStartStopEnabled(), see MapBasedDataStore
			source.setAutoStartStopEnabled(false);
			app.getDataStore().save(source);

			//appStart() gates almost everything on createInitializationProcess()'s .initialized/.closed
			//marker files under webapps/{appName}/ - without both present it reads as an unclean-stop
			//restart and calls resetBroadcasts() first (wiping viewer counts and forcing every broadcast's
			//status to "finished"). Pre-creating both here simulates a clean prior shutdown, so this test
			//isolates just the auto-resume block instead of also exercising the crash-recovery path.
			File appWebappDir = new File("webapps/" + appScope.getName());
			appWebappDir.mkdirs();
			new File(appWebappDir, ".initialized").createNewFile();
			new File(appWebappDir, ".closed").createNewFile();

			//appStart is the real boot hook (it already ran once when this embedded context first came
			//up). Call it again directly to exercise the exact same auto-resume code path a real server
			//restart takes, instead of actually killing and restarting the JVM/container.
			app.appStart(appScope);

			Awaitility.await().atMost(15, TimeUnit.SECONDS).pollInterval(500, TimeUnit.MILLISECONDS).until(() ->
					AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING.equals(app.getDataStore().get(streamId).getStatus()));

			assertNotNull(app.getStreamFetcherManager().getStreamFetcher(streamId));
		}
		finally {
			//expected to fail on master, so cleanup has to be here or the source stays registered in the
			//embedded context for the whole class
			if (cleanupStreamId != null) {
				app.getStreamFetcherManager().stopStreaming(cleanupStreamId, true);
				app.getDataStore().delete(cleanupStreamId);
			}
			stopPublisher(publisher);
		}
	}

	/**
	 * Two external REST callers racing start against stop on the same source must never wedge it: not
	 * running, yet every later start refused as "already active" because a registry entry outlived its
	 * worker. The source is this app's own hls output, since a single shot ffmpeg listener exits as soon
	 * as the first stop disconnects it, which would make the last step untestable. An http server serves
	 * reader after reader, so the racing calls stay the only variable.
	 */
	@Test
	public void testConcurrentStartStopRequestsLeaveSystemInConsistentState() {
		ExecutorService executor = Executors.newFixedThreadPool(2);
		RestServiceV2Test restService = new RestServiceV2Test();
		String publishedStreamId = RandomStringUtils.randomAlphanumeric(8);
		String hlsUrl = "http://127.0.0.1:5080/LiveApp/streams/" + publishedStreamId + ".m3u8";
		Process rtmpPublisher = AppFunctionalV2Test.execute(ffmpegPath
				+ " -re -i src/test/resources/test.flv -codec copy -f flv rtmp://127.0.0.1/LiveApp/" + publishedStreamId);
		String cleanupStreamId = null;
		try {
			Awaitility.await().atMost(40, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
				Broadcast published = restService.getBroadcast(publishedStreamId);
				return published != null && AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING.equals(published.getStatus());
			});

			//nothing can be pulled before the playlist has segments to hand out
			Awaitility.await().atMost(40, TimeUnit.SECONDS).pollInterval(2, TimeUnit.SECONDS).until(() ->
					MuxingTest.testFile(hlsUrl));

			Broadcast source = restService.createBroadcast("concurrent race test", AntMediaApplicationAdapter.STREAM_SOURCE, hlsUrl, null);
			String streamId = source.getStreamId();
			cleanupStreamId = streamId;

			//the racing calls only mean something against a source that can actually connect
			Result initialStart = restService.startStreaming(streamId);
			assertTrue(initialStart.isSuccess());
			Awaitility.await().atMost(30, TimeUnit.SECONDS).pollInterval(500, TimeUnit.MILLISECONDS).until(() ->
					AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING.equals(restService.getBroadcast(streamId).getStatus()));

			//fire near-simultaneous external start+stop a handful of times - REST callers racing each other,
			//distinct from the internal checker race testStuckSourceUnderRestartRaceViaRestOnly covers
			for (int i = 0; i < 5; i++) {
				CountDownLatch latch = new CountDownLatch(1);
				Future<Result> startFuture = executor.submit(() -> {
					latch.await();
					return restService.startStreaming(streamId);
				});
				Future<Result> stopFuture = executor.submit(() -> {
					latch.await();
					return restService.stopStreaming(streamId);
				});
				latch.countDown();
				startFuture.get(15, TimeUnit.SECONDS);
				stopFuture.get(15, TimeUnit.SECONDS);
			}

			//whatever state the race left things in, the system must be genuinely restartable afterward -
			//not the checkpoint-3 symptom (status stuck non-broadcasting while /start refuses as
			//"already active" with no worker thread actually running)
			restService.stopStreaming(streamId);
			Awaitility.await().atMost(15, TimeUnit.SECONDS).pollInterval(500, TimeUnit.MILLISECONDS).until(() ->
					!AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING.equals(restService.getBroadcast(streamId).getStatus()));

			Result restarted = restService.startStreaming(streamId);
			assertTrue(restarted.isSuccess(), "a fresh /start must be accepted once the racing calls settle, not refused as a ghost 'already active'");

			//an hls source takes a playlist fetch and a segment or two to come back, unlike a live transport
			Awaitility.await().atMost(30, TimeUnit.SECONDS).pollInterval(500, TimeUnit.MILLISECONDS).until(() ->
					AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING.equals(restService.getBroadcast(streamId).getStatus()));
		}
		catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		finally {
			//run even if an assertion above failed - otherwise a failure here leaves a live broadcast (and
			//possibly a fetcher still racing) for the rest of the class's shared Spring context
			if (cleanupStreamId != null) {
				restService.stopStreaming(cleanupStreamId);
				RestServiceV2Test.callDeleteBroadcast(cleanupStreamId);
			}
			executor.shutdownNow();
			rtmpPublisher.destroy();
		}
	}

	//TODO: expected to fail on master by design - this is the repro for the bug the rewrite exists to fix,
	//not a regression. Reproduces 6/6 streams stuck. Goes green when the rewrite lands, don't weaken it.
	//A source's real feed comes back but REST refuses to restart it forever as "already active"
	//(PR https://github.com/ant-media/Ant-Media-Server/pull/8072). Needs an internal checker tick to land
	//inside the ~3s retry gap after a drop - a single stream only hits that window ~30% of the time, so
	//this staggers several real sources across one checker period (10s) to cover it in one sweep.
	@Test
	public void testStuckSourceUnderRestartRaceViaRestOnly() {
		int streamCount = 6;
		int basePort = freeSrtPort();
		List<String> streamIds = new ArrayList<>();
		List<Process> publishers = new ArrayList<>();
		RestServiceV2Test restService = new RestServiceV2Test();
		Integer originalRestartStreamFetcherPeriod = null;
		try {
			//settings endpoint is admin-gated; reset needed since its @BeforeEach never runs from here
			ConsoleAppRestServiceTest.resetCookieStore();
			assertTrue(new ConsoleAppRestServiceTest().createFirstUserAndLogin());

			//force a restart on every checker tick to reliably land inside the retry-gap race window
			AppSettings appSettings = ConsoleAppRestServiceTest.callGetAppSettings("LiveApp");
			originalRestartStreamFetcherPeriod = appSettings.getRestartStreamFetcherPeriod();
			appSettings.setRestartStreamFetcherPeriod(4);
			assertTrue(ConsoleAppRestServiceTest.callSetAppSettings("LiveApp", appSettings).isSuccess());

			for (int i = 0; i < streamCount; i++) {
				int port = basePort + i;
				String streamId = "rest-proof-" + i + "-" + RandomStringUtils.randomAlphanumeric(4);
				streamIds.add(streamId);
				publishers.add(startSrtPublisher(port));

				Broadcast source = new Broadcast();
				source.setStreamId(streamId);
				source.setType(AntMediaApplicationAdapter.STREAM_SOURCE);
				source.setStreamUrl("srt://127.0.0.1:" + port);
				RestServiceV2Test.createBroadcast(source);
				restService.startStreaming(streamId);
			}

			//30s not 10s: forced restarts can catch a source mid-restart before its first broadcasting
			Awaitility.await().atMost(30, TimeUnit.SECONDS).pollInterval(500, TimeUnit.MILLISECONDS).until(() -> {
				for (String streamId : streamIds) {
					Broadcast broadcast = restService.getBroadcast(streamId);
					if (broadcast == null || !AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING.equals(broadcast.getStatus())) {
						return false;
					}
				}
				return true;
			});

			//stagger the drops evenly across one checker period so every phase of it is covered in one sweep
			long spacingMs = 10000L / streamCount;
			List<Integer> order = new ArrayList<>();
			for (int i = 0; i < streamCount; i++) {
				order.add(i);
			}
			Collections.shuffle(order);
			for (int i : order) {
				//plain destroy() here, not stopPublisher() - waiting for exit would distort the stagger
				publishers.get(i).destroy();
				Thread.sleep(spacingMs);
			}

			Thread.sleep(3000); //give the last drop its full retry-gap window before recovering

			//every port is rebound below, so make sure the old listeners are really gone first
			for (Process publisher : publishers) {
				stopPublisher(publisher);
			}

			for (int i = 0; i < streamCount; i++) {
				publishers.set(i, startSrtPublisher(basePort + i));
			}

			//bounded wait for natural recovery - a straggler here isn't necessarily stuck, only a
			//refused restart below proves that
			long recoveryDeadline = System.currentTimeMillis() + 18000;
			List<String> lagging;
			do {
				Thread.sleep(1000);
				lagging = new ArrayList<>();
				for (String streamId : streamIds) {
					Broadcast broadcast = restService.getBroadcast(streamId);
					if (broadcast == null || !AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING.equals(broadcast.getStatus())) {
						lagging.add(streamId);
					}
				}
			} while (!lagging.isEmpty() && System.currentTimeMillis() < recoveryDeadline);

			List<String> stuck = new ArrayList<>();
			for (String streamId : lagging) {
				Result restart = restService.startStreaming(streamId);
				if (!restart.isSuccess() && restart.getMessage() != null && restart.getMessage().contains("already active")) {
					stuck.add(streamId);
				}
			}

			assertTrue(stuck.isEmpty(), "client-visible stuck sources (feed is back, REST refuses to restart): " + stuck);
		}
		catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		finally {
			//restore first: this lives on the server and is written through to red5-web.properties, so
			//leaving it set would leak into later tests and later runs
			if (originalRestartStreamFetcherPeriod != null) {
				try {
					AppSettings appSettings = ConsoleAppRestServiceTest.callGetAppSettings("LiveApp");
					appSettings.setRestartStreamFetcherPeriod(originalRestartStreamFetcherPeriod);
					ConsoleAppRestServiceTest.callSetAppSettings("LiveApp", appSettings);
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
			for (Process publisher : publishers) {
				stopPublisher(publisher);
			}
			for (String streamId : streamIds) {
				RestServiceV2Test.callDeleteBroadcast(streamId);
			}
		}
	}

	private static Process startSrtPublisher(int port) {
		return AppFunctionalV2Test.execute("ffmpeg -hide_banner -loglevel error -re -f lavfi "
				+ "-i testsrc=size=640x360:rate=15 -c:v libx264 -preset ultrafast -tune zerolatency "
				+ "-g 15 -pix_fmt yuv420p -f mpegts srt://0.0.0.0:" + port + "?mode=listener");
	}

	//fixed ports used to collide between methods here ("Address already in use")
	private static int freeSrtPort() {
		return 20000 + RANDOM.nextInt(20000);
	}

	private static void stopPublisher(Process publisher) {
		if (publisher == null) {
			return;
		}
		publisher.destroy();
		try {
			if (!publisher.waitFor(5, TimeUnit.SECONDS)) {
				publisher.destroyForcibly();
				publisher.waitFor(5, TimeUnit.SECONDS);
			}
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

}
