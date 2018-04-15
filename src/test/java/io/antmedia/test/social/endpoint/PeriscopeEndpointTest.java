package io.antmedia.test.social.endpoint;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.google.api.services.youtube.model.LiveStreamStatus;

import io.antmedia.api.periscope.type.Broadcast;
import io.antmedia.datastore.db.IDataStore;
import io.antmedia.datastore.db.InMemoryDataStore;
import io.antmedia.datastore.db.MapDBStore;
import io.antmedia.datastore.db.types.BroadcastStatus;
import io.antmedia.datastore.db.types.Endpoint;
import io.antmedia.datastore.db.types.SocialEndpointCredentials;
import io.antmedia.datastore.preference.PreferenceStore;
import io.antmedia.integration.MuxingTest;
import io.antmedia.social.endpoint.PeriscopeEndpoint;
import io.antmedia.social.endpoint.VideoServiceEndpoint.DeviceAuthParameters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class PeriscopeEndpointTest {

	private static final String TARGET_TEST_PROPERTIES = "src/test/resources/preset-red5-web.db";

	// This is test app
	public String CLIENT_ID = "5PXoLeNEcFEKBYOh2W-lJHTF_D584hF4XI-ENDIHCOCzArNaMx";
	public String CLIENT_SECRET = "tYHjmoe42iD1FX0wSLgF7-4kdnM9mabgznuSdaSkVDFFflYomK";

	public static final int MAC_OS_X = 0;
	public static final int LINUX = 1;
	public static final int WINDOWS = 2;
	private static int OS_TYPE;
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

	private static String ffmpegPath = "ffmpeg";

	@BeforeClass
	public static void beforeClass() {
		if (OS_TYPE == MAC_OS_X) {
			ffmpegPath = "/usr/local/bin/ffmpeg";
		}
	}

	/**
	 * This function should not be enabled in CI because it requires manual interaction with PSCP service
	 */
	//@Test
	public void testAccessToken() {

		IDataStore dataStore = null;
		try {
			
			File f = new File(TARGET_TEST_PROPERTIES);
			if (f.exists()) {
				f.delete();
			}
			
			dataStore = new MapDBStore(TARGET_TEST_PROPERTIES);
			
			PeriscopeEndpoint endPoint = new PeriscopeEndpoint(CLIENT_ID, CLIENT_SECRET, dataStore, null);
			DeviceAuthParameters device = null;

			device = endPoint.askDeviceAuthParameters();
			assertNotNull(device);

			assertNotNull(device.user_code);
			assertNotNull(device.device_code);
			assertNotNull(device.expires_in);
			assertNotNull(device.interval);
			assertNotNull(device.verification_url);

			System.out.println("user code:" + device.user_code);
			System.out.println("device code:" + device.device_code);
			System.out.println("expires in:" + device.expires_in);
			System.out.println("interval:" + device.interval);
			System.out.println("verification_url:" + device.verification_url);

			do {
				boolean authentiated;
				try {
					authentiated = endPoint.askIfDeviceAuthenticated();
					if (authentiated) {
						break;
					}
					System.out.println("waiting for " + device.interval + " seconds");
					Thread.sleep(device.interval * 1000);

				} catch (Exception e) {
					e.printStackTrace();
				}

			} while (true);
			
			assertNotNull(endPoint.getCredentials());
			assertNotNull(endPoint.getCredentials().getAccountName());
			assertTrue(endPoint.getCredentials().getAccountName().length() > 3 );
			assertNotNull(endPoint.getCredentials().getAccountId());
			
			assertEquals("periscope", endPoint.getCredentials().getServiceName());
			
			
			assertNotNull(endPoint.getAccountName());
			
			assertTrue(endPoint.getAccountName().length() > 0);
			
			List<SocialEndpointCredentials> socialEndpoints = dataStore.getSocialEndpoints(0, 100);
			assertEquals(1, socialEndpoints.size());
			
			
		} catch (Exception e) {

			e.printStackTrace();
			fail(e.getMessage());
		}
		finally 
		{
			dataStore.close();
		}
		
		testUpdateToken();
		
	}
	
	//@Test This function should not be called as a test funciton. It is called in testAccessToken
	public void testUpdateToken() {
		IDataStore dataStore = new MapDBStore(TARGET_TEST_PROPERTIES);
		
		List<SocialEndpointCredentials> socialEndpoints = dataStore.getSocialEndpoints(0, 10);
		assertEquals(1, socialEndpoints.size());
		
		PeriscopeEndpoint endPoint = new PeriscopeEndpoint(CLIENT_ID, CLIENT_SECRET, dataStore, socialEndpoints.get(0));

		try {
			endPoint.updateToken();
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		
		dataStore.close();
		
		dataStore = new MapDBStore(TARGET_TEST_PROPERTIES);
			
		socialEndpoints = dataStore.getSocialEndpoints(0, 10);
		assertEquals(1, socialEndpoints.size());
		
		endPoint = new PeriscopeEndpoint(CLIENT_ID, CLIENT_SECRET, dataStore, socialEndpoints.get(0));

		try {
			endPoint.updateToken();
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		
		dataStore.close();
	}
	

	@Test
	public void testCreateBroadcastNoName() {
		IDataStore dataStore = new MapDBStore(TARGET_TEST_PROPERTIES);
		List<SocialEndpointCredentials> socialEndpoints = dataStore.getSocialEndpoints(0, 10);
		assertEquals(1, socialEndpoints.size());
		
		PeriscopeEndpoint endPoint = new PeriscopeEndpoint(CLIENT_ID, CLIENT_SECRET, dataStore, socialEndpoints.get(0));

		try {
			Endpoint endpoint = endPoint.createBroadcast("", "", false, false, 720, true);
			assertNotNull(endpoint);
			assertNotNull(endpoint.rtmpUrl);
			assertTrue(endpoint.rtmpUrl.length() > 0);

		} catch (Exception e) {

			e.printStackTrace();
			fail(e.getMessage());
		}
		
		dataStore.close();
	}

	@Test
	public void testCreateBroadcast() {
		IDataStore dataStore = new MapDBStore(TARGET_TEST_PROPERTIES);
		List<SocialEndpointCredentials> socialEndpoints = dataStore.getSocialEndpoints(0, 10);
		assertEquals(1, socialEndpoints.size());
		
		PeriscopeEndpoint endPoint = new PeriscopeEndpoint(CLIENT_ID, CLIENT_SECRET, dataStore, socialEndpoints.get(0));
		try {
			String name = "Event name";
			Endpoint endpoint = endPoint.createBroadcast(name, null, false, false, 720, true);

			System.out.println("rtmp url is:" + endpoint.rtmpUrl);

			
			/// usr/local/bin/
			Process execute = MuxingTest.execute(
					ffmpegPath + " -re -i src/test/resources/test_video_360p.flv -acodec copy -vcodec copy -f flv "
							+ endpoint.rtmpUrl);

			LiveStreamStatus streamStatus = null;

			boolean started = false;
			
			Thread.sleep(3000);
			
			endPoint.publishBroadcast(endpoint);

			Thread.sleep(7000);
			
			String status = endPoint.getBroadcast(endpoint);
			assertEquals(BroadcastStatus.LIVE_NOW, status);
			

			endPoint.stopBroadcast(endpoint);

			execute.destroy();
			
			Thread.sleep(15000);
			
			status = endPoint.getBroadcast(endpoint);
			assertEquals(BroadcastStatus.UNPUBLISHED, status);

		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		finally {
			dataStore.close();
		}
		
		

	}

}
