package io.antmedia.test.social.endpoint;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.RandomStringUtils;
import org.awaitility.Awaitility;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.google.api.services.youtube.model.LiveStreamStatus;

import io.antmedia.api.periscope.type.Broadcast;
import io.antmedia.api.periscope.type.IChatListener;
import io.antmedia.api.periscope.type.User;
import io.antmedia.api.periscope.type.User.ProfileImageUrls;
import io.antmedia.api.periscope.type.chatEndpointTypes.ChatMessage;
import io.antmedia.api.periscope.type.chatEndpointTypes.HeartMessage;
import io.antmedia.api.periscope.type.chatEndpointTypes.ViewerCountMessage;
import io.antmedia.datastore.db.IDataStore;
import io.antmedia.datastore.db.InMemoryDataStore;
import io.antmedia.datastore.db.MapDBStore;
import io.antmedia.datastore.db.types.BroadcastStatus;
import io.antmedia.datastore.db.types.Endpoint;
import io.antmedia.datastore.db.types.SocialEndpointCredentials;
import io.antmedia.datastore.preference.PreferenceStore;
import io.antmedia.integration.MuxingTest;
import io.antmedia.rest.model.UserType;
import io.antmedia.social.LiveComment;
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
			
			PeriscopeEndpoint endPoint = new PeriscopeEndpoint(CLIENT_ID, CLIENT_SECRET, dataStore, null, null);
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
		
		PeriscopeEndpoint endPoint = new PeriscopeEndpoint(CLIENT_ID, CLIENT_SECRET, dataStore, socialEndpoints.get(0), null);

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
		
		endPoint = new PeriscopeEndpoint(CLIENT_ID, CLIENT_SECRET, dataStore, socialEndpoints.get(0), null);

		try {
			endPoint.updateToken();
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		
		dataStore.close();
	}
	
	@Test
	public void testAskDeviceParameters() {

		IDataStore dataStore = null;
		try {
			
			dataStore = new InMemoryDataStore(TARGET_TEST_PROPERTIES);
			
			PeriscopeEndpoint endPoint = new PeriscopeEndpoint(CLIENT_ID, CLIENT_SECRET, dataStore, null, null);
			DeviceAuthParameters device = null;
			
			assertEquals(CLIENT_ID, endPoint.getClientId());
			assertEquals(CLIENT_SECRET, endPoint.getClientSecret());
			
			assertTrue(endPoint.isInitialized());

			device = endPoint.askDeviceAuthParameters();
			assertNotNull(device);

			assertNotNull(device.user_code);
			assertNotNull(device.device_code);
			assertNotNull(device.expires_in);
			assertNotNull(device.interval);
			assertNotNull(device.verification_url);
			
			assertFalse(endPoint.isAuthenticated());
			
			assertFalse(endPoint.askIfDeviceAuthenticated());
			
			
			String accountName = RandomStringUtils.randomAlphanumeric(23);
			String accessToken = RandomStringUtils.randomAlphanumeric(23);
			String refreshToken = RandomStringUtils.randomAlphanumeric(23);
			String expireTimeInSeconds = RandomStringUtils.randomAlphanumeric(23);
			String token_type = RandomStringUtils.randomAlphanumeric(23);
			String accountId = RandomStringUtils.randomAlphanumeric(23);
			endPoint.saveCredentials(accountName, accessToken, refreshToken, expireTimeInSeconds, token_type, accountId);
			
			SocialEndpointCredentials credentials = endPoint.getCredentials();
			assertEquals(accountName, credentials.getAccountName());
			assertEquals(accessToken, credentials.getAccessToken());
			assertEquals(refreshToken, credentials.getRefreshToken());
			assertEquals(expireTimeInSeconds, credentials.getExpireTimeInSeconds());
			assertEquals(token_type, credentials.getTokenType());
			assertEquals(accountId, credentials.getAccountId());
			
			credentials = dataStore.getSocialEndpointCredentials(credentials.getId());
			assertEquals(accountName, credentials.getAccountName());
			assertEquals(accessToken, credentials.getAccessToken());
			assertEquals(refreshToken, credentials.getRefreshToken());
			assertEquals(expireTimeInSeconds, credentials.getExpireTimeInSeconds());
			assertEquals(token_type, credentials.getTokenType());
			assertEquals(accountId, credentials.getAccountId());
			
			endPoint.resetCredentials();
			
			assertNull(endPoint.getCredentials());
			assertNull(dataStore.getSocialEndpointCredentials(credentials.getId()));
		
		} catch (Exception e) {

			e.printStackTrace();
			fail(e.getMessage());
		}
		finally 
		{
			dataStore.close();
		}		
	}
	

	@Test
	public void testCreateBroadcastNoName() {
		IDataStore dataStore = new MapDBStore(TARGET_TEST_PROPERTIES);
		List<SocialEndpointCredentials> socialEndpoints = dataStore.getSocialEndpoints(0, 10);
		assertEquals(1, socialEndpoints.size());
		
		PeriscopeEndpoint endPoint = new PeriscopeEndpoint(CLIENT_ID, CLIENT_SECRET, dataStore, socialEndpoints.get(0), null);

		try {
			Endpoint endpoint = endPoint.createBroadcast("", "", null, false, false, 720, true);
			assertNotNull(endpoint);
			assertNotNull(endpoint.getRtmpUrl());
			assertTrue(endpoint.getRtmpUrl().length() > 0);

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
		
		PeriscopeEndpoint endPoint = new PeriscopeEndpoint(CLIENT_ID, CLIENT_SECRET, dataStore, socialEndpoints.get(0), null);
		try {
			
			assertEquals("faraklit06", endPoint.getAccountName());
			
			String name = "Event name";
			Endpoint endpoint = endPoint.createBroadcast(name, null, null, false, false, 720, true);

			System.out.println("rtmp url is:" + endpoint.getRtmpUrl());

			
			/// usr/local/bin/
			Process execute = MuxingTest.execute(
					ffmpegPath + " -re -i src/test/resources/test_video_360p.flv -acodec copy -vcodec copy -f flv "
							+ endpoint.getRtmpUrl());

			LiveStreamStatus streamStatus = null;

			boolean started = false;
			
			endPoint.publishBroadcast(endpoint);
			

			Awaitility.await().atMost(60, TimeUnit.SECONDS)
				.pollInterval(2, TimeUnit.SECONDS)
				.until(() -> {
					return endPoint.getBroadcast(endpoint).equals(BroadcastStatus.LIVE_NOW);
				});

			endPoint.stopBroadcast(endpoint);

			execute.destroy();
			
			
			Awaitility.await().atMost(20, TimeUnit.SECONDS)
				.pollInterval(2, TimeUnit.SECONDS)
				.until(() -> {
					return endPoint.getBroadcast(endpoint).equals(BroadcastStatus.UNPUBLISHED);
				});
			

		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		finally {
			dataStore.close();
		}
	}
	
	@Test
	public void testChatEndpoint() {
		IDataStore dataStore = new MapDBStore(TARGET_TEST_PROPERTIES);
		List<SocialEndpointCredentials> socialEndpoints = dataStore.getSocialEndpoints(0, 10);
		assertEquals(1, socialEndpoints.size());
		
		PeriscopeEndpoint endPoint = new PeriscopeEndpoint(CLIENT_ID, CLIENT_SECRET, dataStore, socialEndpoints.get(0), null);
		
		String name = "Event name";
		String serverStreamId = RandomStringUtils.randomAlphabetic(8);
		Endpoint endpoint;
		try {
			endpoint = endPoint.createBroadcast(name, null, serverStreamId, false, false, 720, true);
			IChatListener chatListener = endPoint.getNewChatListener(endpoint);
			
			int randomMessageCount = (int) (Math.random() * 100);
			for (int i = 0; i < randomMessageCount; i++) {
				HeartMessage heartMessage = new HeartMessage();
				heartMessage.id =  RandomStringUtils.randomAlphabetic(8);
				heartMessage.type = "heart";
				heartMessage.user = null;
				chatListener.heartMessageReceived(heartMessage);
				
				assertEquals(i+1, endPoint.getInteraction(serverStreamId).getLoveCount());
			}
			
			assertEquals(randomMessageCount, endPoint.getInteraction(serverStreamId).getLoveCount());
			assertEquals(0, endPoint.getInteraction(serverStreamId).getLikeCount());
			assertEquals(0, endPoint.getInteraction(serverStreamId).getHahaCount());
			
			//test viewer message
			randomMessageCount = (int) (Math.random() * 100);
			int totalViewCount = 0;
			for (int i = 0; i < randomMessageCount ; i++) {
				ViewerCountMessage viewerMessage = new ViewerCountMessage();
				viewerMessage.id = RandomStringUtils.randomAlphabetic(8);
				viewerMessage.live = (int) (Math.random() * 100);
				totalViewCount = (int) (Math.random() * 100);
				
				viewerMessage.total = totalViewCount;
				chatListener.viewerCountMessageReceived(viewerMessage);
				
				assertEquals(totalViewCount, endPoint.getLiveViews(endpoint.getServerStreamId()));
			}
			
			assertEquals(totalViewCount, endPoint.getLiveViews(endpoint.getServerStreamId()));
			
			
			//test chat message
			randomMessageCount = (int) (Math.random() * 100) + 10;
			for (int i = 0; i < randomMessageCount ; i++) {
				ChatMessage chatMessage = new ChatMessage();
				chatMessage.id = RandomStringUtils.randomAlphabetic(8);
				chatMessage.text = RandomStringUtils.randomAlphanumeric(48);
				
				chatMessage.user = new User();
				chatMessage.user.id = RandomStringUtils.randomAlphabetic(8);
				chatMessage.user.display_name = RandomStringUtils.randomAlphabetic(8);		
				chatMessage.user.profile_image_urls = new ArrayList<>();
				chatMessage.user.profile_image_urls.add(chatMessage.user.new ProfileImageUrls());
				chatListener.chatMessageReceived(chatMessage);
				assertEquals(i+1, endPoint.getTotalCommentsCount(serverStreamId));
			}
			
			assertEquals(randomMessageCount, endPoint.getTotalCommentsCount(serverStreamId));
			
			List<LiveComment> comments = endPoint.getComments(serverStreamId, 0, 5);
			assertEquals(5, comments.size());
			
			comments = endPoint.getComments(serverStreamId, 200, 500);
			assertNull(comments);
			
			
			endPoint.stopBroadcast(endpoint);
			assertNull(endPoint.getComments(serverStreamId, 0, 500));
			assertEquals(0, endPoint.getTotalCommentsCount(serverStreamId));
			
			assertEquals(0, endPoint.getLiveViews(endpoint.getServerStreamId()));
			assertNull(endPoint.getInteraction(serverStreamId));
			
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		finally {
			dataStore.close();
		}
	}

}
