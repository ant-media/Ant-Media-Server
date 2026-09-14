package io.antmedia.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.RandomStringUtils;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;
import org.bytedeco.ffmpeg.global.avformat;
import org.bytedeco.ffmpeg.global.avutil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
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
import io.antmedia.datastore.db.DataStore;
import io.antmedia.datastore.db.DataStoreFactory;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.datastore.db.types.Endpoint;
import io.antmedia.rest.model.Result;
import io.antmedia.streamsource.StreamFetcher;
import io.vertx.core.Vertx;

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

	/**
	 * Asserts that a dying, superseded fetcher worker must not overwrite a live broadcast's status.
	 *
	 * Choreography:
	 * 1. The fetcher worker parks inside avformat_open_input against a silent TCP tarpit
	 *    (see {@link TarpitRtmpProxy}) while the broadcast stays in PREPARING.
	 * 2. After STREAM_TIMEOUT_MS (20 secs) the periodic checker derives terminated_unexpectedly and
	 *    stop-flags the parked worker; the flag cannot interrupt the blocked handshake read, so the
	 *    old worker stays parked. What happens next depends on the build: older builds restart in
	 *    place (a second connection parks on the tarpit by itself), builds with the deferred-restart
	 *    listener only evict. To get a second parked worker on both, the test simulates the
	 *    customer's watchdog: it keeps POSTing start until the tarpit holds two connections. On
	 *    deferral builds that start succeeds (fetcher evicted, derived status terminated); on older
	 *    builds the extra start harmlessly vetoes against the already-restarted worker.
	 * 3. The new worker's connection is released to a local "ffmpeg -rtmp_listen 1" server and the
	 *    broadcast reaches BROADCASTING: a healthy fetcher is streaming.
	 * 4. The old worker's held connection is closed. Its prepare fails and, because it carries the
	 *    stop flag, it does not retry: its close path runs while the replacement owns the stream.
	 *
	 * Required behavior asserted below: the status stays broadcasting the whole time (never
	 * finished) with updateTime advancing, and a start attempt is honestly vetoed because the
	 * stream really is active. When this test fails with finished observed about a second after
	 * the socket close, that is the status split-brain bug: the dying worker stamped FINISHED over
	 * the live broadcast, leaving a dead-looking stream that cannot be restarted over REST.
	 *
	 * Note: the tarpit is required because the server's own RTMP endpoint rejects pulls immediately
	 * when rtmpPlaybackEnabled=false (the default), which makes the fetcher fast-cycle its 3 sec
	 * retry loop instead of parking in prepare.
	 */
	@Test
	public void testFetcherStopDuringPrepareDoesNotCorruptLiveStatus() throws Exception {
		RestServiceV2Test restService = new RestServiceV2Test();

		String tarpitStreamId = "tarpit_" + RandomStringUtils.randomAlphanumeric(8);

		try (TarpitRtmpProxy tarpit = new TarpitRtmpProxy()) {
			String streamUrl = "rtmp://127.0.0.1:" + tarpit.getPort() + "/LiveApp/" + tarpitStreamId;
			Broadcast streamSource = restService.createBroadcast("tarpitSource", AntMediaApplicationAdapter.STREAM_SOURCE, streamUrl, null);
			assertNotNull(streamSource);
			String streamId = streamSource.getStreamId();

			Process rtmpServer = null;
			try {
				Result result = restService.startStreaming(streamId);
				assertTrue(result.isSuccess());

				//worker parks in avformat_open_input: tarpit accepted the connection but stays silent
				Awaitility.await().atMost(15, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
					Broadcast b = RestServiceV2Test.callGetBroadcast(streamId);
					return tarpit.getHeldConnectionCount() == 1 && b != null
							&& AntMediaApplicationAdapter.BROADCAST_STATUS_PREPARING.equals(b.getStatus());
				});

				//spawn the rtmp server up front so release() below is instant; otherwise the next
				//20 sec staleness cycle can stop-flag the second worker before it reaches BROADCASTING
				int rtmpServerPort = findFreePort();
				rtmpServer = AppFunctionalV2Test.execute(ffmpegPath
						+ " -re -stream_loop -1 -i src/test/resources/test.flv -codec copy -f flv -rtmp_listen 1 rtmp://127.0.0.1:"
						+ rtmpServerPort + "/LiveApp/" + tarpitStreamId);

				//let the checker stop-flag the parked worker (20 secs staleness + tick), then make the
				//second worker explicit: older builds restart in place so the second connection is
				//already held and the extra start harmlessly vetoes; deferral builds only evict, so
				//this watchdog-style start succeeds and parks the second worker
				//pure delay: the stop-flag isn't observable over REST, so there is no condition to poll on
				Awaitility.await().pollDelay(25, TimeUnit.SECONDS).atMost(26, TimeUnit.SECONDS).until(() -> true);
				Awaitility.await().atMost(35, TimeUnit.SECONDS).pollInterval(3, TimeUnit.SECONDS).until(() -> {
					if (tarpit.getHeldConnectionCount() < 2) {
						restService.startStreaming(streamId);
					}
					return tarpit.getHeldConnectionCount() == 2;
				});
				assertEquals(2, tarpit.getHeldConnectionCount());

				//release only the new worker: it completes prepare and streams
				tarpit.release(1, rtmpServerPort);

				Awaitility.await().atMost(40, TimeUnit.SECONDS).pollInterval(2, TimeUnit.SECONDS).until(() -> {
					Broadcast b = RestServiceV2Test.callGetBroadcast(streamId);
					return b != null && AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING.equals(b.getStatus());
				});

				//kill the old worker's parked connection: its prepare fails and its close path runs
				//while the replacement fetcher owns the stream
				tarpit.closeHeldConnection(0);

				//the dying worker must not overwrite the live status: never finished, broadcasting throughout
				//(on the split-brain bug this fails with finished about a second after the close)
				//during() asserts the status holds broadcasting continuously for the whole window
				Awaitility.await().during(25, TimeUnit.SECONDS).atMost(30, TimeUnit.SECONDS).until(() ->
						AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING.equals(RestServiceV2Test.callGetBroadcast(streamId).getStatus()));
				assertTrue(rtmpServer.isAlive());

				//healthy live stream: updateTime advances while status stays broadcasting
				long updateTimeSample = RestServiceV2Test.callGetBroadcast(streamId).getUpdateTime();
				Awaitility.await().atMost(40, TimeUnit.SECONDS).pollInterval(5, TimeUnit.SECONDS).until(() -> {
					Broadcast b = RestServiceV2Test.callGetBroadcast(streamId);
					return b != null && b.getUpdateTime() > updateTimeSample
							&& AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING.equals(b.getStatus());
				});

				//with a truthful status the "already active" veto is correct behavior
				result = restService.startStreaming(streamId);
				assertFalse(result.isSuccess());
				assertEquals(AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING,
						RestServiceV2Test.callGetBroadcast(streamId).getStatus());
			}
			finally {
				if (rtmpServer != null) {
					rtmpServer.destroy();
				}
				restService.stopStreaming(streamId);
				RestServiceV2Test.callDeleteBroadcast(streamId);
			}
		}
	}

	private static int findFreePort() throws IOException {
		try (ServerSocket socket = new ServerSocket(0)) {
			return socket.getLocalPort();
		}
	}

	/**
	 * TCP tarpit for the stream fetcher: accepts connections and holds them silently so the
	 * fetcher worker parks inside avformat_open_input (no timeout options are set for rtmp urls
	 * and there is no interrupt callback). On release a held connection is piped to a local
	 * "ffmpeg -rtmp_listen 1" server so the buffered RTMP handshake completes and data flows.
	 */
	private static class TarpitRtmpProxy implements Closeable {

		private final ServerSocket serverSocket;
		private final List<Socket> heldConnections = Collections.synchronizedList(new ArrayList<>());
		private volatile boolean running = true;

		TarpitRtmpProxy() throws IOException {
			serverSocket = new ServerSocket(0, 5, InetAddress.getLoopbackAddress());
			Thread acceptor = new Thread(() -> {
				while (running) {
					try {
						heldConnections.add(serverSocket.accept());
					} catch (IOException e) {
						break;
					}
				}
			});
			acceptor.setDaemon(true);
			acceptor.start();
		}

		int getPort() {
			return serverSocket.getLocalPort();
		}

		int getHeldConnectionCount() {
			return heldConnections.size();
		}

		void closeHeldConnection(int connectionIndex) throws IOException {
			heldConnections.get(connectionIndex).close();
		}

		void release(int connectionIndex, int upstreamPort) throws IOException {
			Socket downstream = heldConnections.get(connectionIndex);
			Socket upstream;
			try {
				//retry the upstream connect until the rtmp server is listening (was a 40x500ms sleep loop)
				upstream = Awaitility.await().atMost(20, TimeUnit.SECONDS).pollInterval(500, TimeUnit.MILLISECONDS)
						.ignoreExceptions()
						.until(() -> new Socket(InetAddress.getLoopbackAddress(), upstreamPort), Objects::nonNull);
			} catch (ConditionTimeoutException e) {
				throw new IOException("upstream rtmp server is not listening on port " + upstreamPort);
			}
			pump(downstream, upstream);
			pump(upstream, downstream);
		}

		private static void pump(Socket from, Socket to) {
			Thread pumpThread = new Thread(() -> {
				try {
					InputStream in = from.getInputStream();
					OutputStream out = to.getOutputStream();
					byte[] buffer = new byte[8192];
					int read;
					while ((read = in.read(buffer)) != -1) {
						out.write(buffer, 0, read);
						out.flush();
					}
				} catch (IOException e) {
					//connection teardown ends the pump
				}
			});
			pumpThread.setDaemon(true);
			pumpThread.start();
		}

		@Override
		public void close() throws IOException {
			running = false;
			serverSocket.close();
			synchronized (heldConnections) {
				for (Socket socket : heldConnections) {
					socket.close();
				}
			}
		}
	}

}
