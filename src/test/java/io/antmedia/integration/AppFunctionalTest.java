package io.antmedia.integration;

import static org.bytedeco.javacpp.avformat.av_register_all;
import static org.bytedeco.javacpp.avformat.avformat_network_init;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.awaitility.Awaitility;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.red5.server.stream.ClientBroadcastStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

import com.google.gson.Gson;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.AppSettings;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.rest.BroadcastRestService;
import io.antmedia.rest.BroadcastRestService.BroadcastStatistics;
import io.antmedia.rest.BroadcastRestService.LiveStatistics;
import io.antmedia.rest.model.Result;
import io.antmedia.test.Application;

public class AppFunctionalTest {

	private BroadcastRestService restService = null;
	private AppSettings appSettings;
	private static final Logger log = LoggerFactory.getLogger(AppFunctionalTest.class);
	private static final String SERVER_ADDR = "127.0.0.1"; 

	public static Process process;
	private static Process tmpExec;
	private static String ROOT_SERVICE_URL;

	public static final int MAC_OS_X = 0;
	public static final int LINUX = 1;
	public static final int WINDOWS = 2;
	private static BasicCookieStore httpCookieStore;
	static {

		try {
			ROOT_SERVICE_URL = "http://" + InetAddress.getLocalHost().getHostAddress() + ":5080/ConsoleApp/rest";
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}

		log.info("ROOT SERVICE URL: " + ROOT_SERVICE_URL);

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
		RestServiceTest restService = new RestServiceTest();

		Broadcast broadcast = restService.createBroadcast("TOBB Demo");

		log.info("broadcast id:{}", broadcast.getStreamId());

	}

	@Test
	public void testSendEndLiveStreamToThirdparty() {
		/*
		 * String url =
		 * "http://10.2.42.238/ant-media-space/admin//listenerHookURL.php";
		 * StringBuffer notifyHook = AntMediaApplicationAdapter.notifyHook(url,
		 * "809630328345580383813514",
		 * AntMediaApplicationAdapter.HOOK_ACTION_END_LIVE_STREAM);
		 * System.out.println("Result: " + notifyHook.toString());
		 */
	}

	@Test
	public void testSetUpEndPoints() {

		try {
			RestServiceTest restService = new RestServiceTest();

			Broadcast source=restService.createBroadcast("source_stream");
			Broadcast endpoint=restService.createBroadcast("endpoint_stream");

			restService.addEndpoint(source.getStreamId(), endpoint.getRtmpURL());

			Thread.sleep(1000);

			assertNotNull(restService.getBroadcast(source.getStreamId()).getEndPointList());

			Process rtmpSendingProcess = execute(ffmpegPath
					+ " -re -i src/test/resources/test.flv  -codec copy -f flv rtmp://127.0.0.1/LiveApp/"
					+ source.getStreamId());

			//wait for fetching stream
			Thread.sleep(5000);

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
	public void testSendRTMPStream() {

		try {
			
			RestServiceTest rest = new RestServiceTest();
			
			int currentVodNumber = Integer.valueOf(rest.callTotalVoDNumber().getMessage());
			
			log.info("current vod number before test {}", String.valueOf(currentVodNumber));
			

			Broadcast broadcast=rest.createBroadcast("RTMP_stream");

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

			if(!BroadcastRestService.isEnterprise()) {
				return;
			}

			assertTrue(MuxingTest.testFile("http://" + SERVER_ADDR + ":5080/LiveApp/streams/" + broadcast.getStreamId() + "_240p.m3u8"));
			
			int lastVodNumber = Integer.valueOf(rest.callTotalVoDNumber().getMessage());
			log.info("vod number after test {}", String.valueOf(lastVodNumber));
			
			//2 more VoDs should be added to DB, one is original other one ise 240p mp4 files
			assertEquals(currentVodNumber + 2, lastVodNumber);

		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	
	@Test
	public void testZombiStream() {

		try {
			// just create RestServiceTest, do not create broadcast through rest
			// service
			RestServiceTest restService = new RestServiceTest();

			List<Broadcast> broadcastList = restService.callGetBroadcastList();
			int size = broadcastList.size();
			// publish live stream to the server
			String streamId = "zombiStreamId"  + (int)(Math.random()*9999);
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

			Awaitility.await().atMost(5, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
				return restService.callGetBroadcast(streamId).getHlsViewerCount() == 1;
			});
			

			// stop publishing live stream
			destroyProcess();

			Thread.sleep(3000);

			// getLiveStream from server and check that zombi stream not exists
			broadcastList = restService.callGetBroadcastList();
			assertNotNull(broadcastList);
			assertEquals(broadcastList.size(), size);

		} catch (InterruptedException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		
		
		Awaitility.await().atMost(10, TimeUnit.SECONDS).pollInterval(2, TimeUnit.SECONDS).until(() -> {
			RestServiceTest restService = new RestServiceTest();
			return 0 == restService.callGetLiveStatistics().totalLiveStreamCount;
		});

	}

	/**
	 * TODO: This test case should be improved
	 */
	@Test
	public void testStatistics() {

		try {
			RestServiceTest restService = new RestServiceTest();

			List<Broadcast> broadcastList = restService.callGetBroadcastList();
			for (Broadcast broadcast : broadcastList) {
				System.out.println("brodcast url: " + broadcast.getStreamId() + " status: " + broadcast.getStatus());
			}
			LiveStatistics liveStatistics = restService.callGetLiveStatistics();
			assertEquals(0, liveStatistics.totalLiveStreamCount);

			// publish live stream to the server
			String streamId = "zombiStreamId1";
			executeProcess(ffmpegPath
					+ " -re -i src/test/resources/test.flv -acodec copy -vcodec copy -f flv rtmp://localhost/LiveApp/"
					+ streamId);

			Thread.sleep(3000);

			liveStatistics = restService.callGetLiveStatistics();
			assertEquals(liveStatistics.totalLiveStreamCount, 1);

			BroadcastStatistics broadcastStatistics = restService.callGetBroadcastStatistics(streamId);
			assertEquals(broadcastStatistics.totalHLSWatchersCount, -1);  //-1 mean it is not availeble
			assertEquals(broadcastStatistics.totalRTMPWatchersCount, 0);
			assertEquals(broadcastStatistics.totalWebRTCWatchersCount, -1); // -1 mean it is not available 

			broadcastStatistics = restService.callGetBroadcastStatistics("unknown_stream_id");
			assertNotNull(broadcastStatistics);
			assertEquals(broadcastStatistics.totalHLSWatchersCount, -1);
			assertEquals(broadcastStatistics.totalRTMPWatchersCount, -1);
			assertEquals(broadcastStatistics.totalWebRTCWatchersCount, -1);

			destroyProcess();

			Thread.sleep(1000);

			broadcastStatistics = restService.callGetBroadcastStatistics(streamId);
			assertNotNull(broadcastStatistics);
			assertEquals(broadcastStatistics.totalHLSWatchersCount, -1);
			assertEquals(broadcastStatistics.totalRTMPWatchersCount, -1);
			assertEquals(broadcastStatistics.totalWebRTCWatchersCount, -1);

			
			Awaitility.await().atMost(10, TimeUnit.SECONDS).pollInterval(2, TimeUnit.SECONDS).until(() -> {

				return 0 == restService.callGetLiveStatistics().totalLiveStreamCount;
			});
			

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

			RestServiceTest restService = new RestServiceTest();

			Broadcast broadcast = restService.createBroadcast("name");

			broadcast = restService.getBroadcast(broadcast.getStreamId());
			assertEquals(broadcast.getName(), "name");

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

			Thread.sleep(10000);

			// call web service to get stream info and check status
			broadcast = restService.getBroadcast(broadcast.getStreamId().toString());
			assertNotNull(broadcast);
			assertEquals(broadcast.getStatus(), Application.BROADCAST_STATUS_BROADCASTING);

			process.destroy();

			Thread.sleep(10000);

			// call web service to get stream info and check status
			broadcast = restService.getBroadcast(broadcast.getStreamId().toString());
			assertNotNull(broadcast);
			assertEquals(broadcast.getStatus(), Application.BROADCAST_STATUS_FINISHED);

		} catch (InterruptedException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		
		//let the server update live stream count
		
		
		Awaitility.await().atMost(10, TimeUnit.SECONDS).pollInterval(2, TimeUnit.SECONDS).until(() -> {
			RestServiceTest restService = new RestServiceTest();

			LiveStatistics liveStatistics = restService.callGetLiveStatistics();
			return 0 == liveStatistics.totalLiveStreamCount;
		});
		
	}

	public static void executeProcess(final String command) {
		new Thread() {
			public void run() {
				try {

					AppFunctionalTest.process = Runtime.getRuntime().exec(command);
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

			//directory is empty, then delete it
			if(file.list().length==0){

				file.delete();
				//System.out.println("Directory is deleted : " 
				//	+ file.getAbsolutePath());

			}else{

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
					//System.out.println("Directory is deleted : " 
					//		+ file.getAbsolutePath());
				}
			}

		}else{
			//if file, then delete it
			file.delete();
			//System.out.println("File is deleted : " + file.getAbsolutePath());
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
		String url = "localhost:5080/ConsoleApp/rest/isEnterpriseEdition";

		HttpClient client = HttpClients.custom().setRedirectStrategy(new LaxRedirectStrategy())
				.setDefaultCookieStore(httpCookieStore).build();
		Gson gson = new Gson();

		HttpUriRequest post = RequestBuilder.get().setUri(url).build();

		HttpResponse response = client.execute(post);

		StringBuffer result = RestServiceTest.readResponse(response);
		if (response.getStatusLine().getStatusCode() != 200) {
			throw new Exception(result.toString());
		}
		System.out.println("result string: " + result.toString());
		Result tmp = gson.fromJson(result.toString(), Result.class);
		assertNotNull(tmp);
		return tmp;

	}


}
