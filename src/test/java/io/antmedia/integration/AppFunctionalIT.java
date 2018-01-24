package io.antmedia.integration;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.rest.BroadcastRestService;
import io.antmedia.rest.BroadcastRestService.Result;
import io.antmedia.test.Application;


public class AppFunctionalIT {


	private BroadcastRestService restService = null;
	public static Process process;
	
	public static final int MAC_OS_X = 0;
	public static final int LINUX = 1;
	public static final int WINDOWS = 2;
	private static int OS_TYPE;
	private static String ffmpegPath="ffmpeg";
	static {
		String osName = System.getProperty("os.name", "").toLowerCase();
		if (osName.startsWith("mac os x") || osName.startsWith("darwin"))  {
			OS_TYPE = MAC_OS_X;
		}
		else if (osName.startsWith("windows"))  {
			OS_TYPE = WINDOWS;
		}
		else if (osName.startsWith("linux"))  {
			OS_TYPE = LINUX;
		}
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
		RestServiceIT restService = new RestServiceIT();


		Broadcast broadcast = restService.createBroadcast("TOBB Demo");
		
		
		System.out.println("broadcast id:" + broadcast.getStreamId());
		
		
	}
	
	@Test
	public void testSendEndLiveStreamToThirdparty() {
		/*
		String url = "http://10.2.42.238/ant-media-space/admin//listenerHookURL.php";
		StringBuffer notifyHook = AntMediaApplicationAdapter.notifyHook(url, "809630328345580383813514",
				AntMediaApplicationAdapter.HOOK_ACTION_END_LIVE_STREAM);
		System.out.println("Result: " + notifyHook.toString());
		*/
		
	}

	
	
	//Before running test all endpoints should be authenticated
	@Test
	public void testBroadcastStream() {
		try {
			//call web service to create stream

			RestServiceIT restService = new RestServiceIT();


			Broadcast broadcast = restService.createBroadcast("name");
			
			broadcast = restService.getBroadcast(broadcast.getStreamId());
			assertEquals(broadcast.getName(), "name");
			
			
			//TODO: add this to enterprise
			/*
			Result result = restService.addSocialEndpoint(broadcast.getStreamId(), "facebook");
			assertTrue(result.success);
			
			result = restService.addSocialEndpoint(broadcast.getStreamId(), "youtube");
			assertTrue(result.success);
			
			*/
			
			Result result = restService.addSocialEndpoint(broadcast.getStreamId(), "periscope");
			assertTrue(result.success);
			

			executeProcess(ffmpegPath + " -re -i src/test/resources/test.flv -acodec copy -vcodec copy -f flv rtmp://localhost/LiveApp/" + broadcast.getStreamId());

			Thread.sleep(40000);

			//call web service to get stream info and check status
			broadcast = restService.getBroadcast(broadcast.getStreamId().toString());
			assertNotNull(broadcast);
			assertEquals(broadcast.getStatus(), Application.BROADCAST_STATUS_BROADCASTING);

			process.destroy();


			Thread.sleep(10000);

			//call web service to get stream info and check status
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

					AppFunctionalIT.process = Runtime.getRuntime().exec(command);
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
	
	public static void destroyProcess() {
		process.destroy();
	}

	public static boolean exists(String URLName, boolean followRedirects){
		try {
			HttpURLConnection.setFollowRedirects(followRedirects);
			// note : you may also need
			//        HttpURLConnection.setInstanceFollowRedirects(false)
			HttpURLConnection con =
					(HttpURLConnection) new URL(URLName).openConnection();
			con.setRequestMethod("HEAD");
			return (con.getResponseCode() == HttpURLConnection.HTTP_OK);
		}
		catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

}
