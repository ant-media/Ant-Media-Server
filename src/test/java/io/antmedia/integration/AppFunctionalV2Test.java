package io.antmedia.integration;

import static org.bytedeco.ffmpeg.global.avformat.avformat_network_init;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ProcessHandle.Info;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.tika.utils.ExceptionUtils;
import org.awaitility.Awaitility;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.AppSettings;
import io.antmedia.EncoderSettings;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.datastore.db.types.Broadcast.PlayListItem;
import io.antmedia.datastore.db.types.Endpoint;
import io.antmedia.datastore.db.types.VoD;
import io.antmedia.muxer.IAntMediaStreamHandler;
import io.antmedia.rest.BroadcastRestService;
import io.antmedia.rest.RestServiceBase.BroadcastStatistics;
import io.antmedia.rest.model.Result;
import io.antmedia.rest.model.Version;
import io.antmedia.settings.ServerSettings;
import io.antmedia.test.StreamSchedularUnitTest;

public class AppFunctionalV2Test {


	private BroadcastRestService restService = null;
	public static final String SERVER_ADDR = ServerSettings.getLocalHostAddress(); 
	protected static Logger logger = LoggerFactory.getLogger(AppFunctionalV2Test.class);

	public static Process process;
	private static Process tmpExec;
	private static String ROOT_SERVICE_URL;

	public static final int MAC_OS_X = 0;
	public static final int LINUX = 1;
	public static final int WINDOWS = 2;
	static {
		ROOT_SERVICE_URL = "http://" + SERVER_ADDR + ":5080/rest";
		logger.info("ROOT SERVICE URL: " + ROOT_SERVICE_URL);

	}

	@Rule
	public TestRule watcher = new TestWatcher() {
		protected void starting(Description description) {
			System.out.println("Starting test: " + description.getMethodName());
		}

		protected void failed(Throwable e, Description description) {
			System.out.println("Failed test: " + description.getMethodName());
		};
		protected void finished(Description description) {
			System.out.println("Finishing test: " + description.getMethodName());
		};
	};
	private RestServiceV2Test restServiceTest;
	private int numberOfClientsInHLSPlay;

	private static int OS_TYPE;
	public static String ffmpegPath = "ffmpeg";
	public static String ffprobePath = "ffprobe";
	static {
		String osName = System.getProperty("os.name", "").toLowerCase();
		if (osName.startsWith("mac os x") || osName.startsWith("darwin")) {
			OS_TYPE = MAC_OS_X;
			ffprobePath = "/usr/local/bin/ffprobe";
		} else if (osName.startsWith("windows")) {
			OS_TYPE = WINDOWS;
		} else if (osName.startsWith("linux")) {
			OS_TYPE = LINUX;
		}
	}

	public static int getOS() {
		return OS_TYPE;
	}

	@BeforeClass
	public static void beforeClass() {
		if (OS_TYPE == MAC_OS_X) {
			ffmpegPath = "/usr/local/bin/ffmpeg";
		}
	//	av_register_all();
		avformat_network_init();
	}

	@Before
	public void before() {
		restService = new BroadcastRestService();

		File webApps = new File("webapps");
		if (!webApps.exists()) {
			webApps.mkdirs();
		}
		File junit = new File(webApps, "junit");
		if (!junit.exists()) {
			junit.mkdirs();
		}
		restServiceTest = new RestServiceV2Test();

		try {
			//we use this delete operation because sometimes there are too many vod files and
			//vod service returns 50 for max and this make some tests fail

			int currentVodNumber = restServiceTest.callTotalVoDNumber();
			logger.info("current vod number before test {}", String.valueOf(currentVodNumber));
			if (currentVodNumber > 10) {


				//delete vods
				List<VoD> voDList = restServiceTest.callGetVoDList();
				if (voDList != null) {
					for (VoD voD : voDList) {
						RestServiceV2Test.deleteVoD(voD.getVodId());
					}
				}

				currentVodNumber = restServiceTest.callTotalVoDNumber();
				logger.info("vod number after deletion {}", String.valueOf(currentVodNumber));
			}


		}
		catch (Exception e) {
			e.printStackTrace();
			fail(ExceptionUtils.getStackTrace(e));
		}
	}


	@After
	public void after() {
		restService = null;
		try {
			delete(new File("webapps"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testRegularExp() {
		String regularExp = "^.*_{1}[0-9]{3}p{1}\\.mp4{1}$";

		String name = "ksdfjs39483948348ksdfksf_240p.mp4";
		assertTrue(name.matches(regularExp));

	}

	@Test
	public void testSetUpEndPointsV2() {
		assertTrue("This test is moved to RestServiceV2Test#testAddEndpointCrossCheckV2", true);
	}


	@Test
	public void testPlayList() {

		try {
			//create playlist 

			Broadcast broadcast = RestServiceV2Test.createBroadcast("test stream", AntMediaApplicationAdapter.PLAY_LIST, null, null);
			assertNotNull(broadcast);

			Broadcast broadcast2 = RestServiceV2Test.getBroadcast(broadcast.getStreamId());
			assertEquals(AntMediaApplicationAdapter.PLAY_LIST, broadcast2.getType());

			assertEquals(broadcast.getStreamId(), broadcast2.getStreamId());
			assertNull(broadcast.getPlayListItemList());

			boolean startBroadast = RestServiceV2Test.callStartBroadast(broadcast.getStreamId());
			//it should be false because there is no item in the play list
			assertFalse(startBroadast);

			//add new items to play list
			List<PlayListItem> playList = new ArrayList<>();
			playList.add(new PlayListItem(StreamSchedularUnitTest.VALID_MP4_URL, AntMediaApplicationAdapter.VOD));
			playList.add(new PlayListItem(StreamSchedularUnitTest.VALID_MP4_URL, AntMediaApplicationAdapter.VOD));

			Result result = RestServiceV2Test.callUpdateBroadcast(broadcast.getStreamId(), null, null, "", null, null, playList);
			assertTrue(result.isSuccess());

			broadcast2 = RestServiceV2Test.getBroadcast(broadcast.getStreamId());
			assertNotNull(broadcast2.getPlayListItemList());
			assertEquals(2, broadcast2.getPlayListItemList().size());

			//start play list
			startBroadast = RestServiceV2Test.callStartBroadast(broadcast.getStreamId());
			assertTrue(startBroadast);

			//play the play list
			Awaitility.await().atMost(10, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
				return MuxingTest.testFile("http://" + SERVER_ADDR + ":5080/LiveApp/streams/" + broadcast.getStreamId() + ".m3u8");
			});
			broadcast2 = RestServiceV2Test.getBroadcast(broadcast.getStreamId());
			assertEquals(AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING, broadcast2.getStatus());

			logger.info("Waiting playlist to switch to next item for stream: {}", broadcast.getStreamId());
			//wait play list switch to next item
			Awaitility.await().atMost(25, TimeUnit.SECONDS).pollInterval(2, TimeUnit.SECONDS).until(() -> {
				Broadcast tmp = RestServiceV2Test.getBroadcast(broadcast.getStreamId());
				return tmp.getCurrentPlayIndex() == 1;
			});


			//play the play list with delay to make sure it's playing the next item
			Awaitility.await().pollDelay(10, TimeUnit.SECONDS).atMost(20, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
				return MuxingTest.testFile("http://" + SERVER_ADDR + ":5080/LiveApp/streams/" + broadcast.getStreamId() + ".m3u8");
			});

			logger.info("Stop Broadcast service is calling for stream: {}", broadcast.getStreamId());
			//stop the play list
			boolean stopBroadcast = RestServiceV2Test.callStopBroadcastService(broadcast.getStreamId());
			assertTrue(stopBroadcast);


			Awaitility.await().atMost(25, TimeUnit.SECONDS).pollInterval(2, TimeUnit.SECONDS).until(() -> {
				Broadcast tmp = RestServiceV2Test.getBroadcast(broadcast.getStreamId());
				return AntMediaApplicationAdapter.BROADCAST_STATUS_FINISHED.equals(tmp.getStatus())
						&& AntMediaApplicationAdapter.BROADCAST_STATUS_FINISHED.equals(tmp.getPlayListStatus());
			});			

			//delete playlist
			result = RestServiceV2Test.callDeleteBroadcast(broadcast.getStreamId());
			assertTrue(result.isSuccess());

			assertNull(RestServiceV2Test.callGetBroadcast(broadcast.getStreamId()));

		}
		catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}


	@Test
	public void testStreamAcceptFilter() {

		ConsoleAppRestServiceTest.resetCookieStore();
		Result result;
		try {
			result = ConsoleAppRestServiceTest.callisFirstLogin();

			if (result.isSuccess()) {
				Result createInitialUser = ConsoleAppRestServiceTest.createDefaultInitialUser();
				assertTrue(createInitialUser.isSuccess());
			}

			result = ConsoleAppRestServiceTest.authenticateDefaultUser();
			assertTrue(result.isSuccess());
			Random r = new Random();
			String streamId = "streamId" + r.nextInt();

			AppSettings appSettingsModel = ConsoleAppRestServiceTest.callGetAppSettings("LiveApp");
			{
				appSettingsModel.setMaxResolutionAccept(144);

				result = ConsoleAppRestServiceTest.callSetAppSettings("LiveApp", appSettingsModel);
				assertTrue(result.isSuccess());

				//this process should be terminated autotimacally because test.flv has 25fps and 360p

				Process rtmpSendingProcess = execute(ffmpegPath
						+ " -re -i src/test/resources/test.flv  -codec copy -f flv rtmp://127.0.0.1/LiveApp/"
						+ streamId);

				Awaitility.await().atMost(10, TimeUnit.SECONDS).until(()-> { 
					return !rtmpSendingProcess.isAlive();
				});
			}


			{
				appSettingsModel.setMaxResolutionAccept(0);
				//appSettingsModel.setMaxFpsAccept(0);

				result = ConsoleAppRestServiceTest.callSetAppSettings("LiveApp", appSettingsModel);
				assertTrue(result.isSuccess());
				Process rtmpSendingProcess2 = execute(ffmpegPath
						+ " -re -i src/test/resources/test.flv  -codec copy -f flv rtmp://127.0.0.1/LiveApp/"
						+ streamId);

				//this process should NOT be terminated autotimacally because test.flv has 25fps 

				Awaitility.await().atMost(5, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
					return MuxingTest.testFile("http://" + SERVER_ADDR + ":5080/LiveApp/streams/" + streamId + ".m3u8");
				});

				rtmpSendingProcess2.destroy();
			}


			Awaitility.await().atMost(10, TimeUnit.SECONDS).pollInterval(2, TimeUnit.SECONDS).until(() -> { 
				RestServiceV2Test restService = new RestServiceV2Test();
				return 0 == restService.callGetLiveStatistics();
			});




		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	@Test
	public void testAdaptiveMasterFileBug() 
	{

		try {
			//check if enterprise edition
			ConsoleAppRestServiceTest.resetCookieStore();
			Result result = ConsoleAppRestServiceTest.callisFirstLogin();
			if (result.isSuccess()) {
				Result createInitialUser = ConsoleAppRestServiceTest.createDefaultInitialUser();
				assertTrue(createInitialUser.isSuccess());
			}

			result = ConsoleAppRestServiceTest.authenticateDefaultUser();
			assertTrue(result.isSuccess());

			Result isEnterpriseEdition = ConsoleAppRestServiceTest.callIsEnterpriseEdition();
			if (!isEnterpriseEdition.isSuccess()) {
				//if it's not enterprise return
				return;
			}


			//add adaptive settings
			AppSettings appSettingsModel = ConsoleAppRestServiceTest.callGetAppSettings("LiveApp");
			List<EncoderSettings> encoderSettingsActive = appSettingsModel.getEncoderSettings();


			List<EncoderSettings> settingsList = new ArrayList<>();
			settingsList.add(new EncoderSettings(240, 300000, 64000,true));
			appSettingsModel.setEncoderSettings(settingsList);
			result = ConsoleAppRestServiceTest.callSetAppSettings("LiveApp", appSettingsModel);
			assertTrue(result.isSuccess());

			//send stream with ffmpeg 
			String streamId = "streamId_"  + (int)(Math.random()*100000);
			Process rtmpSendingProcess = execute(ffmpegPath
					+ " -re -i src/test/resources/test.flv  -codec copy -f flv rtmp://127.0.0.1/LiveApp/"
					+ streamId);

			//check adaptive.m3u8 file exists
			//wait for creating  files
			Awaitility.await().atMost(10, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
				return MuxingTest.testFile("http://" + SERVER_ADDR + ":5080/LiveApp/streams/" + streamId + "_adaptive.m3u8");
			});

			//stop streaming
			rtmpSendingProcess.destroy();
			rtmpSendingProcess.waitFor();
			
			RestServiceV2Test restService = new RestServiceV2Test();
			
			
			Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> {
				return restService.getBroadcast(streamId) == null;
			});
			
			//start streaming again immediately
			rtmpSendingProcess = execute(ffmpegPath
					+ " -re -i src/test/resources/test.flv  -codec copy -f flv rtmp://127.0.0.1/LiveApp/"
					+ streamId);

			//check that adaptive.m3u8 file is created

			//file should exist because previous streaming is just finished
			assertTrue(MuxingTest.testFile("http://" + SERVER_ADDR + ":5080/LiveApp/streams/" + streamId + "_adaptive.m3u8"));

			//It should still exists after 15 seconds. The bug is that this file is not re-created again
			Awaitility.await().pollDelay(15, TimeUnit.SECONDS).atMost(20, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
				return MuxingTest.testFile("http://" + SERVER_ADDR + ":5080/LiveApp/streams/" + streamId + "_adaptive.m3u8");
			});

			rtmpSendingProcess.destroy();
			rtmpSendingProcess.waitFor();
		}
		catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testSendRTMPWithoutAACHeader() {

		try {
			//prepare settings
			ConsoleAppRestServiceTest.resetCookieStore();
			Result result = ConsoleAppRestServiceTest.callisFirstLogin();
			if (result.isSuccess()) {
				Result createInitialUser = ConsoleAppRestServiceTest.createDefaultInitialUser();
				assertTrue(createInitialUser.isSuccess());
			}

			result = ConsoleAppRestServiceTest.authenticateDefaultUser();
			assertTrue(result.isSuccess());

			AppSettings appSettingsModel = ConsoleAppRestServiceTest.callGetAppSettings("LiveApp");

			appSettingsModel.setMp4MuxingEnabled(true);
			appSettingsModel.setAcceptOnlyStreamsInDataStore(false);
			appSettingsModel.setHlsMuxingEnabled(true);
			result = ConsoleAppRestServiceTest.callSetAppSettings("LiveApp", appSettingsModel);
			assertTrue(result.isSuccess());
			
			String streamId = "streamId" + (int)(Math.random()*90000);
			
			Process rtmpSendingProcess = execute(ffmpegPath
					+ " -re -i src/test/resources/test.flv  -codec copy -f flv rtmp://127.0.0.1/LiveApp/"
					+ streamId);
			
			Awaitility.await().atMost(10, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS)
			.until(() -> MuxingTest.testFile("http://" + SERVER_ADDR + ":5080/LiveApp/streams/" +  streamId + ".m3u8"));

			
			assertTrue(MuxingTest.videoExists);
			assertTrue(MuxingTest.audioExists);
			
			rtmpSendingProcess.destroy();
			
			Awaitility.await().atMost(10, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
				return MuxingTest.testFile("http://" + SERVER_ADDR + ":5080/LiveApp/streams/" + streamId + ".mp4");
			});


		}
		catch(Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testSendRTMPStream() {

		try {

			boolean found240p = false;
			List<EncoderSettings> encoderSettingsActive = null;
			AppSettings appSettingsModel = null;
			boolean mp4MuxingEnabled = false;
			Broadcast broadcast = restServiceTest.createBroadcast("RTMP_stream", null, null, null);
			Broadcast broadcastWithSubFolder = restServiceTest.createBroadcast("RTMP_stream_with_subfolder",null,null, "testFolder");
			{
				//prepare settings
				ConsoleAppRestServiceTest.resetCookieStore();
				Result result = ConsoleAppRestServiceTest.callisFirstLogin();
				if (result.isSuccess()) {
					Result createInitialUser = ConsoleAppRestServiceTest.createDefaultInitialUser();
					assertTrue(createInitialUser.isSuccess());
				}

				result = ConsoleAppRestServiceTest.authenticateDefaultUser();
				assertTrue(result.isSuccess());

				appSettingsModel = ConsoleAppRestServiceTest.callGetAppSettings("LiveApp");
				encoderSettingsActive = appSettingsModel.getEncoderSettings();


				mp4MuxingEnabled = appSettingsModel.isMp4MuxingEnabled();

				appSettingsModel.setMp4MuxingEnabled(true);

				List<EncoderSettings> settingsList = new ArrayList<>();
				settingsList.add(new EncoderSettings(240, 300000, 64000,true));
				appSettingsModel.setEncoderSettings(settingsList);
				result = ConsoleAppRestServiceTest.callSetAppSettings("LiveApp", appSettingsModel);
				assertTrue(result.isSuccess());
				assertEquals("testFolder",broadcastWithSubFolder.getSubFolder());
			}


			Process rtmpSendingProcess = execute(ffmpegPath
					+ " -re -i src/test/resources/test.flv  -codec copy -f flv rtmp://127.0.0.1/LiveApp/"
					+ broadcast.getStreamId());

			//wait for fetching stream
			Awaitility.await().atMost(10, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS)
			.until(() -> MuxingTest.testFile("http://" + SERVER_ADDR + ":5080/LiveApp/streams/" +  broadcast.getStreamId() + ".m3u8"));

			Info processInfo = rtmpSendingProcess.info();

			// stop rtmp streaming
			rtmpSendingProcess.destroy();
			int duration = (int)(System.currentTimeMillis() - processInfo.startInstant().get().toEpochMilli());

			//wait for creating  files
			Awaitility.await().atMost(10, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
				return MuxingTest.testFile("http://" + SERVER_ADDR + ":5080/LiveApp/streams/" + broadcast.getStreamId() + ".mp4");
			});

			assertTrue(MuxingTest.testFile("http://" + SERVER_ADDR + ":5080/LiveApp/streams/" + broadcast.getStreamId() + ".m3u8"));

			//For Subfolder Vod recording
			rtmpSendingProcess = execute(ffmpegPath
					+ " -re -i src/test/resources/test.flv  -codec copy -f flv rtmp://127.0.0.1/LiveApp/"
					+ broadcastWithSubFolder.getStreamId());
			//wait for fetching stream with subfolder record
			Awaitility.await().atMost(10, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS)
			.until(() -> MuxingTest.testFile("http://" + SERVER_ADDR + ":5080/LiveApp/streams/" + broadcastWithSubFolder.getSubFolder()+ "/" +broadcastWithSubFolder.getStreamId() + ".m3u8"));

			processInfo = rtmpSendingProcess.info();

			// stop rtmp streaming
			rtmpSendingProcess.destroy();
			duration = (int)(System.currentTimeMillis() - processInfo.startInstant().get().toEpochMilli());

			//wait for creating  files
			Awaitility.await().atMost(10, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
				return MuxingTest.testFile("http://" + SERVER_ADDR + ":5080/LiveApp/streams/" + broadcastWithSubFolder.getSubFolder()+ "/" + broadcastWithSubFolder.getStreamId() + ".mp4");
			});

			assertTrue(MuxingTest.testFile("http://" + SERVER_ADDR + ":5080/LiveApp/streams/" + broadcastWithSubFolder.getSubFolder()+ "/" + broadcastWithSubFolder.getStreamId() + ".m3u8"));

			boolean isEnterprise = callIsEnterpriseEdition().getMessage().contains("Enterprise");
			if(isEnterprise) {

				assertTrue(MuxingTest.testFile("http://" + SERVER_ADDR + ":5080/LiveApp/streams/" + broadcast.getStreamId() + "_240p300kbps.m3u8"));
				assertTrue(MuxingTest.testFile("http://" + SERVER_ADDR + ":5080/LiveApp/streams/" + broadcastWithSubFolder.getSubFolder()+ "/" +  broadcastWithSubFolder.getStreamId() + "_240p300kbps.m3u8"));

				Awaitility.await().atMost(10, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
					return MuxingTest.testFile("http://" + SERVER_ADDR + ":5080/LiveApp/streams/" + broadcast.getStreamId() + "_240p300kbps.mp4");
				});
				Awaitility.await().atMost(10, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
					return MuxingTest.testFile("http://" + SERVER_ADDR + ":5080/LiveApp/streams/" + broadcastWithSubFolder.getSubFolder()+ "/" +   broadcastWithSubFolder.getStreamId() + "_240p300kbps.mp4");
				});
				Awaitility.await().atMost(30, TimeUnit.SECONDS).pollInterval(2, TimeUnit.SECONDS).until(() -> {


					int vodNumber = restServiceTest.callTotalVoDNumber();
					logger.info("vod number after test {}", vodNumber);

					//2 more VoDs should be added to DB, one is original other one ise 240p mp4 files
					//480p is not created because original stream is 360p

					int foundTime = 0;
					for (int i = 0; i*50 < vodNumber; i++) {
						List<VoD> vodList = restServiceTest.callGetVoDList(i*50, 50);
						for (VoD vod : vodList) {
							if (vod.getStreamId().equals(broadcast.getStreamId()) || vod.getStreamId().equals(broadcastWithSubFolder.getStreamId())) 
							{
								foundTime++;
							}
							if (foundTime == 4) {
								return true;
							}
						}						
					}

					return false;

				});

			}
			else {
				Awaitility.await().atMost(30, TimeUnit.SECONDS).pollInterval(2, TimeUnit.SECONDS).until(() -> {
					int lastVodNumber = restServiceTest.callTotalVoDNumber();
					logger.info("vod number after test {}", lastVodNumber);
					//2 more VoDs should be added to DB, one is original other one ise 240p mp4 files
					//480p is not created because original stream is 360p


					int foundTime = 0;
					for (int i = 0; i*50 < lastVodNumber; i++) {
						List<VoD> vodList = restServiceTest.callGetVoDList(i*50, 50);
						for (VoD vod : vodList) {
							if (vod.getStreamId().equals(broadcast.getStreamId()) || vod.getStreamId().equals(broadcastWithSubFolder.getStreamId())) 
							{
								foundTime++;

							}
							if(foundTime == 2) {
								return true;
							}
						}						
					}

					return false;

				});
			}

			List<VoD> callGetVoDList = RestServiceV2Test.callGetVoDList();
			boolean found = false;
			boolean found2 = false;
			VoD vod1 = null;
			VoD vod2 = null;
			VoD vod3 = null;
			VoD vod4 = null;
			for (VoD voD : callGetVoDList) {
				if (voD.getStreamId().equals(broadcast.getStreamId())) 
				{
					if (voD.getFilePath().equals("streams/"+broadcast.getStreamId() + ".mp4")) {
						vod1 = voD;
					}
					else if (voD.getFilePath().equals("streams/"+broadcast.getStreamId() + "_240p300kbps.mp4")) {
						vod2 = voD;
					}

					//file path does not contain vod id
					assertFalse(voD.getFilePath().contains(voD.getVodId()));
					found = true;
				}
				else if(voD.getStreamId().equals(broadcastWithSubFolder.getStreamId())){
					if (voD.getFilePath().equals("streams/" + broadcastWithSubFolder.getSubFolder() + "/" + broadcastWithSubFolder.getStreamId() + ".mp4")) {
						vod3 = voD;
					}
					else if (voD.getFilePath().equals("streams/" + broadcastWithSubFolder.getSubFolder() + "/" + broadcastWithSubFolder.getStreamId() + "_240p300kbps.mp4")) {
						vod4 = voD;
					}

					//file path does not contain vod id
					assertFalse(voD.getFilePath().contains(voD.getVodId()));
					found2 = true;
				}
			}
			assertTrue(found);
			assertTrue(found2);
			assertNotNull(vod1);
			assertNotNull(vod3);
			assertTrue(MuxingTest.isURLAvailable("http://" + SERVER_ADDR + ":5080/LiveApp/"+ vod1.getFilePath()));
			assertTrue(MuxingTest.isURLAvailable("http://" + SERVER_ADDR + ":5080/LiveApp/"+ vod3.getFilePath()));
			assertTrue(RestServiceV2Test.deleteVoD(vod1.getVodId()).isSuccess());
			assertTrue(RestServiceV2Test.deleteVoD(vod3.getVodId()).isSuccess());
			assertFalse(MuxingTest.isURLAvailable("http://" + SERVER_ADDR + ":5080/LiveApp/"+ vod1.getFilePath()));
			assertFalse(MuxingTest.isURLAvailable("http://" + SERVER_ADDR + ":5080/LiveApp/"+ vod3.getFilePath()));
			if (isEnterprise) {
				assertNotNull(vod2);
				assertNotNull(vod4);
				assertTrue(MuxingTest.isURLAvailable("http://" + SERVER_ADDR + ":5080/LiveApp/"+ vod2.getFilePath()));
				assertTrue(MuxingTest.isURLAvailable("http://" + SERVER_ADDR + ":5080/LiveApp/"+ vod4.getFilePath()));
				assertTrue(RestServiceV2Test.deleteVoD(vod2.getVodId()).isSuccess());
				assertTrue(RestServiceV2Test.deleteVoD(vod4.getVodId()).isSuccess());
				assertFalse(MuxingTest.isURLAvailable("http://" + SERVER_ADDR + ":5080/LiveApp/"+ vod2.getFilePath()));
				assertFalse(MuxingTest.isURLAvailable("http://" + SERVER_ADDR + ":5080/LiveApp/"+ vod4.getFilePath()));
			}


			{
				//restore settings
				appSettingsModel.setEncoderSettings(encoderSettingsActive);
				appSettingsModel.setMp4MuxingEnabled(mp4MuxingEnabled);
				Result result = ConsoleAppRestServiceTest.callSetAppSettings("LiveApp", appSettingsModel);
				assertTrue(result.isSuccess());

			}

		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	@Test
	public void testHLSStatistics() {

		RestServiceV2Test restService = new RestServiceV2Test();

		Random r = new Random();
		String streamId = "streamId" + r.nextInt();

		Broadcast stream=restService.createBroadcast(streamId);

		//src/test/resources/test.flv

		Process rtmpSendingProcess = execute(ffmpegPath
				+ " -re -i src/test/resources/test.flv  -codec copy -f flv rtmp://127.0.0.1/LiveApp/"
				+ stream.getStreamId());

		//Wait for the m3u8 file is available
		Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> {
			return MuxingTest.testFile("http://" + SERVER_ADDR + ":5080/LiveApp/streams/" +stream.getStreamId()+ ".m3u8" );
		});	


		//INFO: the request above also increase the viewer counter so that initial number of viewer is +1 from the below value
		numberOfClientsInHLSPlay = 9;
		ArrayList<CookieStore> cookieStoreList = new ArrayList<>();
		for (int i=0 ; i < numberOfClientsInHLSPlay; i++ ) {
			cookieStoreList.add(new BasicCookieStore());
		} 

		ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

		ScheduledFuture<?> scheduleWithFixedDelay = executor.scheduleWithFixedDelay(new Runnable() {

			@Override
			public void run() 
			{

				logger.info("Running m3u8 fetch for {} clients", numberOfClientsInHLSPlay);
				for (int i=0 ; i < numberOfClientsInHLSPlay; i++ ) 
				{
					try {
						getURL("http://"+SERVER_ADDR+":5080/LiveApp/streams/"+stream.getStreamId()+".m3u8", cookieStoreList.get(i));
					}
					catch (Exception e) {
						e.printStackTrace();
					}
				}


			}
		}, 0, 2, TimeUnit.SECONDS); 



		//Check Stream list size and Streams status		
		Awaitility.await().atMost(45, TimeUnit.SECONDS).pollInterval(2, TimeUnit.SECONDS).until(() -> {
			//it is +1 of the numberOfClientsInHLSPlay because previous MuxingTest.testFile creates a viewer as well 
			return restService.callGetBroadcast(stream.getStreamId()).getHlsViewerCount() == 10 ;
		});

		numberOfClientsInHLSPlay-=4; 

		//Check Stream list size and Streams status.		
		Awaitility.await().atMost(45, TimeUnit.SECONDS).pollInterval(2, TimeUnit.SECONDS).until(() -> {
			//it decreases 5 because there is no MuxingTest.testFile request and numberOfClientsInHLSPlay decrease by one
			return restService.callGetBroadcast(stream.getStreamId()).getHlsViewerCount() == 5 ; 
		});

		assertTrue(scheduleWithFixedDelay.cancel(false));
		executor.shutdown();

		//Check Stream list size and Streams status		
		Awaitility.await().atMost(45, TimeUnit.SECONDS).pollInterval(2, TimeUnit.SECONDS).until(() -> { 
			return restService.callGetBroadcast(stream.getStreamId()).getHlsViewerCount() == 0 ;
		});

		rtmpSendingProcess.destroy();


	}


	/**
	 * TODO: This test case should be improved
	 */
	//@Test
	public void testStatistics() {

		try {

			ConsoleAppRestServiceTest.resetCookieStore();
			Result result = ConsoleAppRestServiceTest.callisFirstLogin();
			if (result.isSuccess()) {
				Result createInitialUser = ConsoleAppRestServiceTest.createDefaultInitialUser();
				assertTrue(createInitialUser.isSuccess());
			}

			result = ConsoleAppRestServiceTest.authenticateDefaultUser();
			assertTrue(result.isSuccess());

			RestServiceV2Test restService = new RestServiceV2Test();


			AppSettings appSettings = ConsoleAppRestServiceTest.callGetAppSettings("LiveApp");
			//make webrtc enabled false because it's enabled by true
			appSettings.setWebRTCEnabled(false);
			
			//It's once crashed with following settings so that enabling these one to reproduce the problem
			
			//The bug is fixed by changing source code in the dashenc.c. See the build_ffmpeg.md in enterprise to
			//get more details
			appSettings.setDashMuxingEnabled(true);
			appSettings.setEncoderSettings(Arrays.asList(new EncoderSettings(240, 300000, 64000, true)));
			appSettings.setGeneratePreview(true);
			
			result = ConsoleAppRestServiceTest.callSetAppSettings("LiveApp", appSettings);
			assertTrue(result.isSuccess());


			List<Broadcast> broadcastList = restService.callGetBroadcastList();
			for (Broadcast broadcast : broadcastList) {
				System.out.println("brodcast url: " + broadcast.getStreamId() + " status: " + broadcast.getStatus());
			}

			Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> {
				return 0 == restService.callGetLiveStatistics();
			});

			// publish live stream to the server
			String streamId = "zombiStreamId1";
			executeProcess(ffmpegPath
					+ " -re -i src/test/resources/test.flv -acodec copy -vcodec copy -f flv rtmp://localhost/LiveApp/"
					+ streamId);

			Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> {
				return 1 == restService.callGetLiveStatistics();
			}); 

			BroadcastStatistics broadcastStatistics = restService.callGetBroadcastStatistics(streamId);
			assertEquals(0, broadcastStatistics.totalHLSWatchersCount); 
			assertEquals(0, broadcastStatistics.totalRTMPWatchersCount);
			//we get webrtc watcher count from database
			assertEquals(0, broadcastStatistics.totalWebRTCWatchersCount); 

			BroadcastStatistics totalBroadcastStatistics = restService.callGetTotalBroadcastStatistics();
			assertEquals(-1, totalBroadcastStatistics.totalRTMPWatchersCount); 
			assertEquals(0, totalBroadcastStatistics.totalHLSWatchersCount); 
			assertEquals(0, totalBroadcastStatistics.totalWebRTCWatchersCount); 


			broadcastStatistics = restService.callGetBroadcastStatistics("unknown_stream_id");
			assertNotNull(broadcastStatistics);
			assertEquals(-1, broadcastStatistics.totalHLSWatchersCount);
			assertEquals(-1, broadcastStatistics.totalRTMPWatchersCount);
			assertEquals(-1, broadcastStatistics.totalWebRTCWatchersCount);

			destroyProcess();

			Awaitility.await().pollInterval(2, TimeUnit.SECONDS).atMost(10, TimeUnit.SECONDS).until(()-> {
				BroadcastStatistics localStats = restService.callGetBroadcastStatistics(streamId);
				return localStats != null && -1 == localStats.totalHLSWatchersCount && -1 == localStats.totalRTMPWatchersCount
						&& -1 == localStats.totalWebRTCWatchersCount;
			});

			Awaitility.await().atMost(10, TimeUnit.SECONDS).pollInterval(2, TimeUnit.SECONDS).until(() -> {
				return 0 == restService.callGetLiveStatistics();
			});


			//make webrtc enabled false because it's enabled by true
			appSettings.setWebRTCEnabled(true);
			result = ConsoleAppRestServiceTest.callSetAppSettings("LiveApp", appSettings);
			assertTrue(result.isSuccess());

		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	public static void executeProcess(final String command) {
		new Thread() {
			public void run() {
				try {

					AppFunctionalV2Test.process = Runtime.getRuntime().exec(command);
					InputStream errorStream = process.getErrorStream();
					byte[] data = new byte[1024];
					int length = 0;

					while ((length = errorStream.read(data, 0, data.length)) > 0) {
						System.out.println(new String(data, 0, length));
					}
				} catch (IOException e) {

					e.printStackTrace();
				}
			};
		}.start();
	}

	public static boolean isProcessAlive() {
		return process.isAlive();
	}

	public static void destroyProcess() {
		process.destroy();
	}


	public static String getURL(String url, CookieStore cookieStore) throws Exception 
	{
		HttpClient client = HttpClients.custom().setDefaultCookieStore(cookieStore).build();
		HttpUriRequest get = RequestBuilder.get().setUri(url).build();

		HttpResponse response = client.execute(get);

		StringBuffer result = readResponse(response);

		if (response.getStatusLine().getStatusCode() != 200) {
			throw new Exception(result.toString());
		}

		return result.toString();
	}

	public static boolean exists(String URLName, boolean followRedirects) {
		try {
			HttpURLConnection.setFollowRedirects(followRedirects);
			// note : you may also need
			// HttpURLConnection.setInstanceFollowRedirects(false)
			HttpURLConnection con = (HttpURLConnection) new URL(URLName).openConnection();
			con.setRequestMethod("HEAD");
			return (con.getResponseCode() == HttpURLConnection.HTTP_OK);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	public static void delete(File file)
			throws IOException{

		if(file.isDirectory()){

			if (Files.isSymbolicLink(file.toPath())) {
				Files.deleteIfExists(file.toPath());
			}
			else if(file.list().length == 0){
				//directory is empty, then delete it

				file.delete();
			}
			else
			{
				//list all the directory contents
				String files[] = file.list();

				for (String temp : files) {
					//construct the file structure
					File fileDelete = new File(file, temp);

					//recursive delete
					delete(fileDelete);
				}

				//check the directory again, if empty then delete it
				if(file.list().length==0){
					file.delete();
				}
			}

		}else{
			//if file, then delete it
			file.delete();
		}
	}

	public static Process execute(final String command) {
		tmpExec = null;
		new Thread() {
			public void run() {
				try {

					tmpExec = Runtime.getRuntime().exec(command);
					InputStream errorStream = tmpExec.getErrorStream();
					byte[] data = new byte[1024];
					int length = 0;

					while ((length = errorStream.read(data, 0, data.length)) > 0) {
						System.out.println(new String(data, 0, length));
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			};
		}.start();

		while (tmpExec == null) {
			try {
				System.out.println("Waiting for exec get initialized...");
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		return tmpExec;
	}

	public Result callIsEnterpriseEdition() throws Exception {

		String url = "http://localhost:5080/LiveApp/rest/v2/version";
		CloseableHttpClient client = HttpClients.custom().setRedirectStrategy(new LaxRedirectStrategy()).build();
		Gson gson = new Gson();

		HttpUriRequest get = RequestBuilder.get().setUri(url).build();
		CloseableHttpResponse response = client.execute(get);

		StringBuffer result = readResponse(response);


		if (response.getStatusLine().getStatusCode() != 200) {
			throw new Exception(result.toString());
		}
		logger.info("result string: {} ",result.toString());

		Version version = gson.fromJson(result.toString(),Version.class);



		Result resultResponse = new Result(true, version.getVersionType());

		assertNotNull(resultResponse);

		return resultResponse;

	}
	public static StringBuffer readResponse(HttpResponse response) throws IOException {
		BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

		StringBuffer result = new StringBuffer();
		String line = "";
		while ((line = rd.readLine()) != null) {
			result.append(line);
		}
		return result;
	}


}
