package io.antmedia.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.rest.BroadcastRestService;
import io.antmedia.rest.BroadcastRestService.BroadcastStatistics;
import io.antmedia.rest.BroadcastRestService.LiveStatistics;
import io.antmedia.test.Application;

public class AppFunctionalTest {

	private BroadcastRestService restService = null;
	public static Process process;

	public static final int MAC_OS_X = 0;
	public static final int LINUX = 1;
	public static final int WINDOWS = 2;
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
	}

	@Before
	public void before() {
		restService = new BroadcastRestService();
	}

	@After
	public void after() {
		restService = null;
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

		System.out.println("broadcast id:" + broadcast.getStreamId());

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
	public void testZombiStream() {

		try {
			// just create RestServiceTest, do not create broadcast through rest
			// service
			RestServiceTest restService = new RestServiceTest();

			List<Broadcast> broadcastList = restService.callGetBroadcastList();
			int size = broadcastList.size();
			// publish live stream to the server
			String streamId = "zombiStreamId";
			executeProcess(ffmpegPath
					+ " -re -i src/test/resources/test.flv -acodec copy -vcodec copy -f flv rtmp://localhost/LiveApp/"
					+ streamId);

			Thread.sleep(5000);

			// getLiveStreams from server and check that zombi stream exists and
			// status is broadcasting
			broadcastList = restService.callGetBroadcastList();
			assertNotNull(broadcastList);

			// maximum return list size is 50
			// assertEquals(broadcastList.size(), size+1);
			Broadcast broadcast = restService.callGetBroadcast(streamId);

			assertEquals(broadcast.getStatus(), AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING);

			// stop publishing live stream
			destroyProcess();

			Thread.sleep(3000);

			// getLiveStream from server and check that zombi stream not exists
			broadcastList = restService.callGetBroadcastList();
			assertNotNull(broadcastList);
			assertEquals(broadcastList.size(), size);

		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/**
	 * TODO: This test case should be improved
	 */
	@Test
	public void testStatistics() {

		try {
			RestServiceTest restService = new RestServiceTest();

			LiveStatistics liveStatistics = restService.callGetLiveStatistics();
			assertEquals(liveStatistics.totalHLSWatchersCount, 0);
			assertEquals(liveStatistics.totalRTMPWatchersCount, 0);
			assertEquals(liveStatistics.totalWebRTCWatchersCount, 0);
			assertEquals(liveStatistics.totalLiveStreamCount, 0);

			// publish live stream to the server
			String streamId = "zombiStreamId1";
			executeProcess(ffmpegPath
					+ " -re -i src/test/resources/test.flv -acodec copy -vcodec copy -f flv rtmp://localhost/LiveApp/"
					+ streamId);

			Thread.sleep(3000);

			liveStatistics = restService.callGetLiveStatistics();
			assertEquals(liveStatistics.totalHLSWatchersCount, 0);
			assertEquals(liveStatistics.totalRTMPWatchersCount, 0);
			assertEquals(liveStatistics.totalWebRTCWatchersCount, 0);
			assertEquals(liveStatistics.totalLiveStreamCount, 1);

			BroadcastStatistics broadcastStatistics = restService.callGetBroadcastStatistics(streamId);
			assertEquals(broadcastStatistics.totalHLSWatchersCount, 0);
			assertEquals(broadcastStatistics.totalRTMPWatchersCount, 0);
			assertEquals(broadcastStatistics.totalWebRTCWatchersCount, 0);

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

			liveStatistics = restService.callGetLiveStatistics();
			assertEquals(liveStatistics.totalHLSWatchersCount, 0);
			assertEquals(liveStatistics.totalRTMPWatchersCount, 0);
			assertEquals(liveStatistics.totalWebRTCWatchersCount, 0);
			assertEquals(liveStatistics.totalLiveStreamCount, 0);

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

}
