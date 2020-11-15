package io.antmedia.integration;

import static org.bytedeco.ffmpeg.global.avformat.av_register_all;
import static org.bytedeco.ffmpeg.global.avformat.avformat_network_init;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
import io.antmedia.datastore.db.types.Endpoint;
import io.antmedia.datastore.db.types.VoD;
import io.antmedia.rest.BroadcastRestService;
import io.antmedia.rest.RestServiceBase.BroadcastStatistics;
import io.antmedia.rest.model.Result;
import io.antmedia.rest.model.Version;
import io.antmedia.settings.ServerSettings;

public class AppFunctionalV2Test {
	

	private BroadcastRestService restService = null;
	private static final String SERVER_ADDR = ServerSettings.getLocalHostAddress(); 
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
	private static String ffmpegPath = "ffmpeg";
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

	public static int getOS() {
		return OS_TYPE;
	}

	@BeforeClass
	public static void beforeClass() {
		if (OS_TYPE == MAC_OS_X) {
			ffmpegPath = "/usr/local/bin/ffmpeg";
		}
		av_register_all();
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
	public void testCreateBroadcast() {
		RestServiceV2Test restService = new RestServiceV2Test();

		Broadcast broadcast = restService.createBroadcast("TOBB Demo");

		logger.info("broadcast id:{}", broadcast.getStreamId());

	}
	
	@Test
	public void testSetUpEndPoints() {

		try {
			RestServiceV2Test restService = new RestServiceV2Test();

			Broadcast source=restService.createBroadcast("source_stream");
			Broadcast endpoint=restService.createBroadcast("endpoint_stream");
			
			
			Awaitility.await().atMost(2, TimeUnit.SECONDS).until(() -> {
				return (restService.getBroadcast(source.getStreamId()) != null) && (restService.getBroadcast(endpoint.getStreamId()) != null);
			});

			restService.addEndpoint(source.getStreamId(), endpoint.getRtmpURL());

			assertNotNull(restService.getBroadcast(source.getStreamId()).getEndPointList());

			Process rtmpSendingProcess = execute(ffmpegPath
					+ " -re -i src/test/resources/test.flv  -codec copy -f flv rtmp://127.0.0.1/LiveApp/"
					+ source.getStreamId());
			
			//Check Stream list size and Streams status		
			Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> {
				return restService.callGetLiveStatistics() == 2 
						&& restService.callGetBroadcast(source.getStreamId()).getStatus().equals(AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING)
						&& restService.callGetBroadcast(endpoint.getStreamId()).getStatus().equals(AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING);
			});
			
			rtmpSendingProcess.destroy();

			//wait for creating mp4 files

			String sourceURL = "http://" + SERVER_ADDR + ":5080/LiveApp/streams/" + source.getStreamId() + ".mp4";

			Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> {
				return MuxingTest.getByteArray(sourceURL) != null;
			});

			String endpointURL = "http://" + SERVER_ADDR + ":5080/LiveApp/streams/" + endpoint.getStreamId() + ".mp4";
			Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> {
				return MuxingTest.getByteArray(endpointURL) != null;
			});

			//test mp4 files
			assertTrue(MuxingTest.testFile(sourceURL));
			assertTrue(MuxingTest.testFile(endpointURL));

			restService.deleteBroadcast(source.getStreamId());
			restService.deleteBroadcast(endpoint.getStreamId());

		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	
	@Test
	public void testSetUpEndPointsV2() {

		try {
			RestServiceV2Test restService = new RestServiceV2Test();

			Broadcast source=restService.createBroadcast("source_stream");
			Broadcast endpointStream=restService.createBroadcast("endpoint_stream");
			
			Awaitility.await().atMost(2, TimeUnit.SECONDS).until(() -> {
				return (restService.getBroadcast(source.getStreamId()) != null) && (restService.getBroadcast(endpointStream.getStreamId()) != null);
			});

			Endpoint endpoint = new Endpoint();
			endpoint.setRtmpUrl(endpointStream.getRtmpURL());
			
			restService.addEndpointV2(source.getStreamId(), endpoint);
			
			Awaitility.await().atMost(1, TimeUnit.SECONDS).until(() -> {
				return restService.getBroadcast(source.getStreamId()) != null;
			});

			assertNotNull(restService.getBroadcast(source.getStreamId()).getEndPointList());

			Process rtmpSendingProcess = execute(ffmpegPath
					+ " -re -i src/test/resources/test.flv  -codec copy -f flv rtmp://127.0.0.1/LiveApp/"
					+ source.getStreamId());

			//Check Stream list size and Streams status
			Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> {
				return restService.callGetLiveStatistics() == 2 
						&& restService.callGetBroadcast(source.getStreamId()).getStatus().equals(AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING)
						&& restService.callGetBroadcast(endpointStream.getStreamId()).getStatus().equals(AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING);
			});

			rtmpSendingProcess.destroy();

			//wait for creating mp4 files

			String sourceURL = "http://" + SERVER_ADDR + ":5080/LiveApp/streams/" + source.getStreamId() + ".mp4";

			Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> {
				return MuxingTest.getByteArray(sourceURL) != null;
			});

			String endpointURL = "http://" + SERVER_ADDR + ":5080/LiveApp/streams/" + endpointStream.getStreamId() + ".mp4";
			Awaitility.await().atMost(10, TimeUnit.SECONDS).pollInterval(2, TimeUnit.SECONDS).until(() -> {
				return MuxingTest.testFile(endpointURL);
			});

			//test mp4 files
			assertTrue(MuxingTest.testFile(sourceURL));

			restService.deleteBroadcast(source.getStreamId());
			restService.deleteBroadcast(endpointStream.getStreamId());

		} catch (Exception e) {
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
				Awaitility.await().pollDelay(9, TimeUnit.SECONDS).atMost(10, TimeUnit.SECONDS).until(()-> {
					return rtmpSendingProcess2.isAlive();
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
			settingsList.add(new EncoderSettings(240, 300000, 64000));
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
	public void testSendRTMPStream() {

		try {

			boolean found240p = false;
			List<EncoderSettings> encoderSettingsActive = null;
			AppSettings appSettingsModel = null;
			boolean mp4MuxingEnabled = false;
			Broadcast broadcast = restServiceTest.createBroadcast("RTMP_stream");
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
				settingsList.add(new EncoderSettings(240, 300000, 64000));
				appSettingsModel.setEncoderSettings(settingsList);
				result = ConsoleAppRestServiceTest.callSetAppSettings("LiveApp", appSettingsModel);
				assertTrue(result.isSuccess());

			}


			Process rtmpSendingProcess = execute(ffmpegPath
					+ " -re -i src/test/resources/test.flv  -codec copy -f flv rtmp://127.0.0.1/LiveApp/"
					+ broadcast.getStreamId());

			//wait for fetching stream
			Thread.sleep(5000);

			rtmpSendingProcess.destroy();

			//wait for creating  files
			Awaitility.await().atMost(10, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
				return MuxingTest.testFile("http://" + SERVER_ADDR + ":5080/LiveApp/streams/" + broadcast.getStreamId() + ".mp4");
			});

			assertTrue(MuxingTest.testFile("http://" + SERVER_ADDR + ":5080/LiveApp/streams/" + broadcast.getStreamId() + ".m3u8"));

			boolean isEnterprise = callIsEnterpriseEdition().getMessage().contains("Enterprise");
			if(isEnterprise) {

				assertTrue(MuxingTest.testFile("http://" + SERVER_ADDR + ":5080/LiveApp/streams/" + broadcast.getStreamId() + "_240p.m3u8"));

				Awaitility.await().atMost(10, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
					return MuxingTest.testFile("http://" + SERVER_ADDR + ":5080/LiveApp/streams/" + broadcast.getStreamId() + "_240p.mp4");
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
							if (vod.getStreamId().equals(broadcast.getStreamId())) 
							{
								foundTime++;
							}
							if (foundTime == 2) {
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

				
					//2 more VoDs should be added to DB, one is original other one ise 240p mp4 files
					//480p is not created because original stream is 360p
					
					int foundTime = 0;
					for (int i = 0; i*50 < lastVodNumber; i++) {
						List<VoD> vodList = restServiceTest.callGetVoDList(i*50, 50);
						for (VoD vod : vodList) {
							if (vod.getStreamId().equals(broadcast.getStreamId())) 
							{
								return true;
							}
						}						
					}
					
					return false;

				});
			}

			List<VoD> callGetVoDList = RestServiceV2Test.callGetVoDList();
			boolean found = false;
			VoD vod1 = null;
			VoD vod2 = null;
			for (VoD voD : callGetVoDList) {
				if (voD.getStreamId().equals(broadcast.getStreamId())) 
				{
					if (voD.getFilePath().equals("streams/"+broadcast.getStreamId() + ".mp4")) {
						vod1 = voD;
					}
					else if (voD.getFilePath().equals("streams/"+broadcast.getStreamId() + "_240p.mp4")) {
						vod2 = voD;
					}

					//file path does not contain vod id
					assertFalse(voD.getFilePath().contains(voD.getVodId()));
					found = true;
				}
			}
			assertTrue(found);
			assertNotNull(vod1);
			assertTrue(MuxingTest.isURLAvailable("http://" + SERVER_ADDR + ":5080/LiveApp/"+ vod1.getFilePath()));
			assertTrue(RestServiceV2Test.deleteVoD(vod1.getVodId()).isSuccess());
			assertFalse(MuxingTest.isURLAvailable("http://" + SERVER_ADDR + ":5080/LiveApp/"+ vod1.getFilePath()));

			if (isEnterprise) {
				assertNotNull(vod2);
				assertTrue(MuxingTest.isURLAvailable("http://" + SERVER_ADDR + ":5080/LiveApp/"+ vod2.getFilePath()));
				assertTrue(RestServiceV2Test.deleteVoD(vod2.getVodId()).isSuccess());
				assertFalse(MuxingTest.isURLAvailable("http://" + SERVER_ADDR + ":5080/LiveApp/"+ vod2.getFilePath()));
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
	public void testZombiStream() {

		try {
			ConsoleAppRestServiceTest.resetCookieStore();
			Result result = ConsoleAppRestServiceTest.callisFirstLogin();
			if (result.isSuccess()) {
				Result createInitialUser = ConsoleAppRestServiceTest.createDefaultInitialUser();
				assertTrue(createInitialUser.isSuccess());
			}

			result = ConsoleAppRestServiceTest.authenticateDefaultUser();
			assertTrue(result.isSuccess());
			
			AppSettings appSettings = ConsoleAppRestServiceTest.callGetAppSettings("LiveApp");
			//make webrtc enabled false because it's enabled by true
			appSettings.setWebRTCEnabled(false);
			appSettings.setH264Enabled(true);
			appSettings.setEncoderSettings(Arrays.asList(new EncoderSettings(240, 300000, 64000)));
			result = ConsoleAppRestServiceTest.callSetAppSettings("LiveApp", appSettings);
			assertTrue(result.isSuccess());
			
			// just create RestServiceTest, do not create broadcast through rest
			// service
			RestServiceV2Test restService = new RestServiceV2Test();
			
			List<Broadcast> broadcastList = restService.callGetBroadcastList();
			int size = broadcastList.size();
			
			int currentVodNumber = restService.callTotalVoDNumber();
			logger.info("current vod number: {}", currentVodNumber);
			// publish live stream to the server
			String streamId = "zombiStreamId"  + (int)(Math.random()*999999);
			executeProcess(ffmpegPath
					+ " -re -i src/test/resources/test.flv -acodec copy -vcodec copy -f flv rtmp://localhost/LiveApp/"
					+ streamId);


			Awaitility.await().atMost(20, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> { 
				return MuxingTest.isURLAvailable("http://" + SERVER_ADDR + ":5080/LiveApp/streams/" +streamId+ "_0p0001.ts" );
			});

			// getLiveStreams from server and check that zombi stream exists and
			// status is broadcasting
			broadcastList = restService.callGetBroadcastList();
			assertNotNull(broadcastList);

			// maximum return list size is 50
			// assertEquals(broadcastList.size(), size+1);
			Broadcast broadcast = restService.callGetBroadcast(streamId);

			assertEquals(AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING, broadcast.getStatus());


			Awaitility.await().atMost(10, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
				return MuxingTest.isURLAvailable("http://" + SERVER_ADDR + ":5080/LiveApp/streams/" +streamId+ ".m3u8" );
			});


			assertTrue(MuxingTest.testFile("http://" + SERVER_ADDR + ":5080/LiveApp/streams/" +streamId+ ".m3u8" ));

			Awaitility.await().atMost(20, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> { 
				return restService.callGetBroadcast(streamId).getHlsViewerCount() == 1;
			});

			BroadcastStatistics broadcastStatistics = restService.callGetBroadcastStatistics(streamId);
			assertEquals(1, broadcastStatistics.totalHLSWatchersCount);


			// stop publishing live stream
			destroyProcess();

			Awaitility.await().atMost(20, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> { 
				List<VoD> callGetVoDList = restService.callGetVoDList();
				for (VoD vod : callGetVoDList) {
					if (vod.getStreamId().equals(streamId)) {
						return true;
					}
				}
				return false;
			});

			// getLiveStream from server and check that zombi stream not exists
			broadcastList = restService.callGetBroadcastList();
			assertNotNull(broadcastList);
			assertEquals(broadcastList.size(), size);
			
			
			boolean isEnterprise = callIsEnterpriseEdition().getMessage().contains("Enterprise");
			if (isEnterprise) {
				Awaitility.await().atMost(20, TimeUnit.SECONDS).pollInterval(3, TimeUnit.SECONDS).until(() -> { 
					int vodNumber = restService.callTotalVoDNumber();
					int foundTime = 0;
					for (int i = 0; i*50 < vodNumber; i++) {
						List<VoD> vodList = restService.callGetVoDList(i*50, 50);
						for (VoD vod : vodList) {
							if (vod.getStreamId().equals(streamId)) 
							{
								foundTime++;
							}
							if (foundTime == 2) {
								return true;
							}
						}
						
					}
					//one for it self and one for 240p
					return false;
				});
			}

		} catch (InterruptedException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}


		Awaitility.await().atMost(10, TimeUnit.SECONDS).pollInterval(2, TimeUnit.SECONDS).until(() -> { 
			RestServiceV2Test restService = new RestServiceV2Test();
			return 0 == restService.callGetLiveStatistics();
		});

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
		
		numberOfClientsInHLSPlay--; 
		
		//Check Stream list size and Streams status.		
		Awaitility.await().atMost(45, TimeUnit.SECONDS).pollInterval(2, TimeUnit.SECONDS).until(() -> {
			//it decreases 2 because there is no MuxingTest.testFile request and numberOfClientsInHLSPlay decrease by one
			return restService.callGetBroadcast(stream.getStreamId()).getHlsViewerCount() == 8 ; 
		});
		
		numberOfClientsInHLSPlay-=2; 
		
		
		//Check Stream list size and Streams status		
		Awaitility.await().atMost(45, TimeUnit.SECONDS).pollInterval(2, TimeUnit.SECONDS).until(() -> {
			return restService.callGetBroadcast(stream.getStreamId()).getHlsViewerCount() == 6 ; 
		});
		
		numberOfClientsInHLSPlay-=4; 
		
		//Check Stream list size and Streams status		
		Awaitility.await().atMost(45, TimeUnit.SECONDS).pollInterval(2, TimeUnit.SECONDS).until(() -> {
			return restService.callGetBroadcast(stream.getStreamId()).getHlsViewerCount() == 2 ; 
		});

		numberOfClientsInHLSPlay--; 
		rtmpSendingProcess.destroy();
		
		//Check Stream list size and Streams status		
		Awaitility.await().atMost(45, TimeUnit.SECONDS).pollInterval(2, TimeUnit.SECONDS).until(() -> { 
			return restService.callGetBroadcast(stream.getStreamId()).getHlsViewerCount() == 0 ;
		});
		
		numberOfClientsInHLSPlay-=3; 
		
		assertTrue(scheduleWithFixedDelay.cancel(false));
		executor.shutdown(); 
	}
	
	
	/**
	 * TODO: This test case should be improved
	 */
	@Test
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
			assertEquals(-1, broadcastStatistics.totalWebRTCWatchersCount); 


			broadcastStatistics = restService.callGetBroadcastStatistics("unknown_stream_id");
			assertNotNull(broadcastStatistics);
			assertEquals(-1, broadcastStatistics.totalHLSWatchersCount);
			assertEquals(-1, broadcastStatistics.totalRTMPWatchersCount);
			assertEquals(-1, broadcastStatistics.totalWebRTCWatchersCount);

			destroyProcess();

			Thread.sleep(1000);

			broadcastStatistics = restService.callGetBroadcastStatistics(streamId);
			assertNotNull(broadcastStatistics);
			assertEquals(-1, broadcastStatistics.totalHLSWatchersCount);
			assertEquals(-1, broadcastStatistics.totalRTMPWatchersCount);
			assertEquals(-1, broadcastStatistics.totalWebRTCWatchersCount);


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
	
	// Before running test all endpoints should be authenticated
	@Test
	public void testBroadcastStream() {
		try {
			// call web service to create stream

			RestServiceV2Test restService = new RestServiceV2Test();

			final Broadcast broadcast = restService.createBroadcast("name");

			Broadcast receivedBroadcast = restService.getBroadcast(broadcast.getStreamId());
			assertEquals("name", receivedBroadcast.getName());

			// TODO: add this to enterprise
			/*
			 * Result result =
			 * restService.addSocialEndpoint(broadcast.getStreamId(),
			 * "facebook"); assertTrue(result.success);
			 * 
			 * result = restService.addSocialEndpoint(broadcast.getStreamId(),
			 * "youtube"); assertTrue(result.success);
			 * 
			 */

			// Result result =
			// restService.addSocialEndpoint(broadcast.getStreamId(),
			// "periscope");
			// assertTrue(result.isSuccess());

			executeProcess(ffmpegPath
					+ " -re -i src/test/resources/test.flv -acodec copy -vcodec copy -f flv rtmp://localhost/LiveApp/"
					+ broadcast.getStreamId());

			
			Awaitility.await().atMost(20, TimeUnit.SECONDS).pollInterval(2, TimeUnit.SECONDS).until(() -> {
				// call web service to get stream info and check status
				Broadcast broadcastTemp = RestServiceV2Test.getBroadcast(broadcast.getStreamId().toString());
				return broadcastTemp != null && AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING.equals(broadcastTemp.getStatus());
			});


			process.destroy();

			// call web service to get stream info and check status
			Awaitility.await().atMost(20, TimeUnit.SECONDS).pollInterval(2, TimeUnit.SECONDS).until(() -> {
				// call web service to get stream info and check status
				Broadcast broadcastTemp = RestServiceV2Test.getBroadcast(broadcast.getStreamId().toString());
				return broadcastTemp != null && AntMediaApplicationAdapter.BROADCAST_STATUS_FINISHED.equals(broadcastTemp.getStatus());
			});

		}  catch (Exception e) { 
			e.printStackTrace();
			fail(e.getMessage());
		}

		//let the server update live stream count


		Awaitility.await().atMost(10, TimeUnit.SECONDS).pollInterval(2, TimeUnit.SECONDS).until(() -> { 
			RestServiceV2Test restService = new RestServiceV2Test();

			return 0 == restService.callGetLiveStatistics();
		});

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
				Thread.sleep(1000);
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
