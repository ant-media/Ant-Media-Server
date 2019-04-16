package io.antmedia.integration;

import static org.bytedeco.javacpp.avformat.av_register_all;
import static org.bytedeco.javacpp.avformat.avformat_network_init;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.awaitility.Awaitility;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.hash.Hashing;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.AppSettingsModel;
import io.antmedia.EncoderSettings;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.datastore.db.types.Licence;
import io.antmedia.datastore.db.types.Token;
import io.antmedia.muxer.MuxAdaptor;
import io.antmedia.rest.model.Result;
import io.antmedia.rest.model.User;
import io.antmedia.rest.model.Version;
import io.antmedia.settings.ServerSettings;
import io.antmedia.test.Application;


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ConsoleAppRestServiceTest{

	private static String ROOT_SERVICE_URL;

	private static String ffmpegPath = "ffmpeg";

	private static final String LOG_LEVEL = "logLevel";

	private static final String LOG_LEVEL_INFO = "INFO";
	private static final String LOG_LEVEL_WARN = "WARN";
	private static final String LOG_LEVEL_TEST = "TEST";

	private static String TEST_USER_EMAIL = "test@antmedia.io";
	private static String TEST_USER_PASS = "testtest";
	private static Process tmpExec;
	private static final String SERVER_ADDR = "127.0.0.1"; 
	private static final String SERVICE_URL = "http://localhost:5080/LiveApp/rest";
	private static Gson gson = new Gson();


	private static BasicCookieStore httpCookieStore;
	private static final Logger log = LoggerFactory.getLogger(ConsoleAppRestServiceTest.class);


	static {

		try {
			ROOT_SERVICE_URL = "http://" + InetAddress.getLocalHost().getHostAddress() + ":5080/rest";
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}

		System.out.println("ROOT SERVICE URL: " + ROOT_SERVICE_URL);

	}

	public static void resetCookieStore() {
		httpCookieStore = new BasicCookieStore();
	}

	@BeforeClass
	public static void beforeClass() {
		if (AppFunctionalTest.getOS() == AppFunctionalTest.MAC_OS_X) {
			ffmpegPath = "/usr/local/bin/ffmpeg";
		}
	}

	@Before
	public void before() {
		avformat_network_init();
		av_register_all();
		//initialize again
		httpCookieStore = new BasicCookieStore();
	}

	@After
	public void teardown() {
		httpCookieStore = null;

	}

	public static Result createDefaultInitialUser() throws Exception {
		User user = new User();
		user.setEmail(TEST_USER_EMAIL);
		user.setPassword(TEST_USER_PASS);
		Result createInitialUser = callCreateInitialUser(user);
		assertTrue(createInitialUser.isSuccess());
		return createInitialUser;
	}

	/**
	 * This test should run first
	 */
	@Test
	public void testACreateInitialUser() {
		try {
			// create user for the first login
			Result firstLogin = callisFirstLogin();
			if (!firstLogin.isSuccess()) {
				//if it's not first login check that TEST_USER_EMAIL and TEST_USER_PASS is authenticated
				User user = new User();
				user.setEmail(TEST_USER_EMAIL);
				user.setPassword(TEST_USER_PASS);
				assertTrue(callAuthenticateUser(user).isSuccess());
				return;
			}
			assertTrue("Server is not started from scratch. Please delete server.db file and restart server",
					firstLogin.isSuccess());

			User user = new User();

			Result authenticatedUserResult = callAuthenticateUser(user);
			assertFalse(authenticatedUserResult.isSuccess());

			user.setEmail("any_email");
			authenticatedUserResult = callAuthenticateUser(user);
			assertFalse(authenticatedUserResult.isSuccess());

			user.setEmail(TEST_USER_EMAIL);
			user.setPassword(TEST_USER_PASS);
			Result createInitialUser = callCreateInitialUser(user);
			assertTrue(createInitialUser.isSuccess());

			// authenticate the user
			authenticatedUserResult = callAuthenticateUser(user);
			assertTrue(authenticatedUserResult.isSuccess());

			user.setEmail(TEST_USER_EMAIL);
			user.setPassword( "any_pass");
			authenticatedUserResult = callAuthenticateUser(user);
			assertFalse(authenticatedUserResult.isSuccess());

			// try to create user that is being used in first step
			// but this time it should fail
			firstLogin = callisFirstLogin();
			assertFalse(firstLogin.isSuccess());

			user.setEmail("any_email");
			user.setPassword("any_pass");
			createInitialUser = callCreateInitialUser(user);
			assertFalse(createInitialUser.isSuccess());

		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	/**
	 * Bug test
	 */
	@Test
	public void testIsClusterMode() {
		try {
			User user = new User();
			user.setEmail(TEST_USER_EMAIL);
			user.setPassword(TEST_USER_PASS);
			Result authenticatedUserResult = callAuthenticateUser(user);
			assertTrue(authenticatedUserResult.isSuccess());

			Result result = callIsClusterMode();
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	public static Result authenticateDefaultUser() throws Exception{
		User user = new User();
		user.setEmail(TEST_USER_EMAIL);
		user.setPassword(TEST_USER_PASS);
		return callAuthenticateUser(user);
	}

	@Test
	public void testGetAppSettings() {
		try {
			Result authenticatedUserResult = authenticateDefaultUser();
			assertTrue(authenticatedUserResult.isSuccess());

			// get LiveApp default settings and check the default values
			// get settings from the app
			Result result = callIsEnterpriseEdition();
			String appName = "WebRTCApp";
			if (result.isSuccess()) {
				appName = "WebRTCAppEE";
			}

			AppSettingsModel appSettingsModel = callGetAppSettings(appName);
			assertEquals("", appSettingsModel.getVodFolder());

			appSettingsModel = callGetAppSettings("LiveApp");

			// change app settings - change vod folder
			String new_vod_folder = "vod_folder";
			assertNotEquals(new_vod_folder, appSettingsModel.getVodFolder());
			String defaultValue = appSettingsModel.getVodFolder();


			appSettingsModel.setVodFolder(new_vod_folder);
			result = callSetAppSettings("LiveApp", appSettingsModel);
			assertTrue(result.isSuccess());

			// get app settings and assert settings has changed - check vod folder has changed
			appSettingsModel = callGetAppSettings("LiveApp");
			assertEquals(new_vod_folder, appSettingsModel.getVodFolder());

			// check the related file to make sure settings changed for restart
			// return back to default values
			appSettingsModel.setVodFolder(defaultValue);
			result = callSetAppSettings("LiveApp", appSettingsModel);
			assertTrue(result.isSuccess());


		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}



	@Test
	public void testGetServerSettings() {
		try {
			Result authenticatedUserResult = authenticateDefaultUser();
			assertTrue(authenticatedUserResult.isSuccess());

			//get Server Settings
			ServerSettings serverSettings = callGetServerSettings();
			String serverName = serverSettings.getServerName();
			String licenseKey = serverSettings.getLicenceKey();
			boolean isMarketRelease = serverSettings.isBuildForMarket();


			// change Server settings 
			serverSettings.setServerName("newServerName");
			serverSettings.setLicenceKey("newLicenseKey");
			serverSettings.setBuildForMarket(!isMarketRelease);

			//check that settings saved
			Result result = callSetServerSettings(serverSettings);
			assertTrue(result.isSuccess());

			// get serverSettings again

			serverSettings = callGetServerSettings();

			assertEquals("newServerName", serverSettings.getServerName());
			assertEquals("newLicenseKey", serverSettings.getLicenceKey());
			assertEquals(!isMarketRelease, serverSettings.isBuildForMarket());

			// return back to original values

			serverSettings.setServerName(serverName);
			serverSettings.setLicenceKey(licenseKey);
			serverSettings.setBuildForMarket(isMarketRelease);

			//save original settings
			result = callSetServerSettings(serverSettings);
			assertTrue(result.isSuccess());


		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}



	/**
	 * This may be a bug, just check it out, it is working as expected
	 */
	@Test
	public void testMuxingDisableCheckStreamStatus() {

		try {
			// Get App Settings
			User user = new User();
			user.setEmail(TEST_USER_EMAIL);
			user.setPassword(TEST_USER_PASS);
			Result authenticatedUserResult = callAuthenticateUser(user);
			assertTrue(authenticatedUserResult.isSuccess());

			AppSettingsModel appSettingsModel = callGetAppSettings("LiveApp");

			// Change app settings and make mp4 and hls muxing false
			appSettingsModel.setMp4MuxingEnabled(false);
			appSettingsModel.setHlsMuxingEnabled(false);
			List<EncoderSettings> encoderSettings = new ArrayList<EncoderSettings>();
			for (int i = 0; i < appSettingsModel.getEncoderSettings().size(); i++) {
				encoderSettings.add(appSettingsModel.getEncoderSettings().get(i));
			}
			appSettingsModel.getEncoderSettings().clear();

			Result result = callSetAppSettings("LiveApp", appSettingsModel);
			assertTrue(result.isSuccess());

			// send stream
			Broadcast broadcastCreated = RestServiceTest.callCreateBroadcast(10000);
			assertNotNull(broadcastCreated.getStreamId());
			assertEquals(Application.BROADCAST_STATUS_CREATED, broadcastCreated.getStatus());

			AppFunctionalTest.executeProcess(ffmpegPath
					+ " -re -i src/test/resources/test.flv -acodec copy -vcodec copy -f flv rtmp://localhost/LiveApp/"
					+ broadcastCreated.getStreamId());

			// check stream status is broadcasting
			Awaitility.await().atMost(5, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() ->
			{
				Broadcast broadcast = RestServiceTest.callGetBroadcast(broadcastCreated.getStreamId());
				return broadcast.getStatus().equals(AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING);
			});

			Broadcast broadcast = RestServiceTest.callGetBroadcast(broadcastCreated.getStreamId());
			assertEquals(AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING, broadcast.getStatus());

			// stop stream
			AppFunctionalTest.destroyProcess();

			Awaitility.await().atMost(5, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() ->
			{
				Broadcast broadcastTmp = RestServiceTest.callGetBroadcast(broadcastCreated.getStreamId());
				return broadcastTmp.getStatus().equals(AntMediaApplicationAdapter.BROADCAST_STATUS_FINISHED);
			});

			// check stream status is finished
			broadcast = RestServiceTest.callGetBroadcast(broadcastCreated.getStreamId());

			assertEquals(AntMediaApplicationAdapter.BROADCAST_STATUS_FINISHED, broadcast.getStatus());

			// restore settings
			appSettingsModel.setMp4MuxingEnabled(true);
			appSettingsModel.setHlsMuxingEnabled(true);
			appSettingsModel.setEncoderSettings(encoderSettings);

			result = callSetAppSettings("LiveApp", appSettingsModel);
			assertTrue(result.isSuccess());

			AppSettingsModel callGetAppSettings = callGetAppSettings("LiveApp");
			assertTrue(callGetAppSettings.getEncoderSettings().size() > 0);

		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testLogLevel() throws Exception {

		try {	

			Result authenticatedUserResult = authenticateDefaultUser();
			assertTrue(authenticatedUserResult.isSuccess());

			//get Log Level Check (Default Log Level INFO)

			String logLevel = callGetLogLevel();
			JSONObject logJSON = (JSONObject) new JSONParser().parse(logLevel);
			String tmpObject = (String) logJSON.get(LOG_LEVEL); 
			assertEquals(LOG_LEVEL_INFO, tmpObject);

			// change Log Level Check (INFO -> WARN)
			Result callSetLogLevelWarn = callSetLogLevel(LOG_LEVEL_WARN);
			assertTrue(callSetLogLevelWarn.isSuccess());

			logLevel = callGetLogLevel();
			logJSON = (JSONObject) new JSONParser().parse(logLevel);
			tmpObject = (String) logJSON.get(LOG_LEVEL); 
			assertEquals(LOG_LEVEL_WARN, tmpObject);

			// change Log Level Check (currently Log Level doesn't change)
			Result callSetLogLevelTest = callSetLogLevel(LOG_LEVEL_TEST);
			assertFalse(callSetLogLevelTest.isSuccess());

			// check log status
			logLevel = callGetLogLevel();
			logJSON = (JSONObject) new JSONParser().parse(logLevel);
			tmpObject = (String) logJSON.get(LOG_LEVEL); 

			assertEquals(LOG_LEVEL_WARN, tmpObject);
			
			
			//restore the log 
			callSetLogLevelTest = callSetLogLevel(LOG_LEVEL_INFO);
			assertTrue(callSetLogLevelTest.isSuccess());

		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}


	@Test
	public void testIPFilter() {
		try {

			User user = new User();
			user.setEmail(TEST_USER_EMAIL);
			user.setPassword(TEST_USER_PASS);
			Result authenticatedUserResult = callAuthenticateUser(user);
			assertTrue(authenticatedUserResult.isSuccess());

			//get the applications from server
			String applications = callGetApplications();

			JSONObject appsJSON = (JSONObject) new JSONParser().parse(applications);
			JSONArray jsonArray = (JSONArray) appsJSON.get("applications");
			//choose the one of them

			int index = (int)(Math.random()*jsonArray.size());
			String appName = (String) jsonArray.get(index);

			log.info("appName: {}", appName);


			//call a rest service 
			List<Broadcast> broadcastList = callGetBroadcastList(appName);
			//assert that it's successfull
			assertNotNull(broadcastList);

			AppSettingsModel appSettings = callGetAppSettings(appName);

			String remoteAllowedCIDR = appSettings.getRemoteAllowedCIDR();
			assertEquals("127.0.0.1", remoteAllowedCIDR);

			//change the settings and ip filter does not accept rest services
			appSettings.setRemoteAllowedCIDR("");

			Result result = callSetAppSettings(appName, appSettings);
			assertTrue(result.isSuccess());

			//call a rest service
			broadcastList = callGetBroadcastList(appName);

			//assert that it's failed
			assertNull(broadcastList);

			assertEquals(403, lastStatusCode);

			//restore settings
			appSettings.setRemoteAllowedCIDR(remoteAllowedCIDR);
			callSetAppSettings(appName, appSettings);

		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	static int lastStatusCode;


	public static List<Broadcast> callGetBroadcastList(String appName) {
		try {

			String url = "http://127.0.0.1:5080/" + appName + "/rest/broadcast/getList/0/50";

			CloseableHttpClient client = HttpClients.custom().setRedirectStrategy(new LaxRedirectStrategy()).build();
			// Gson gson = new Gson();
			// Broadcast broadcast = null; //new Broadcast();
			// broadcast.name = "name";

			HttpUriRequest get = RequestBuilder.get().setUri(url)
					.setHeader(HttpHeaders.CONTENT_TYPE, "application/json")
					// .setEntity(new StringEntity(gson.toJson(broadcast)))
					.build();

			CloseableHttpResponse response = client.execute(get);

			StringBuffer result = readResponse(response);

			if (response.getStatusLine().getStatusCode() != 200) {
				lastStatusCode = response.getStatusLine().getStatusCode();
				throw new Exception(result.toString());
			}
			System.out.println("result string: " + result.toString());
			Type listType = new TypeToken<List<Broadcast>>() {
			}.getType();

			return gson.fromJson(result.toString(), listType);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Test
	public void testChangePreviewOverwriteSettings() {
		//check if enterprise edition, if it is enterprise run the test
		//if not skip this test

		Result result;
		try {


			// first authenticate user
			User user = new User();
			user.setEmail(TEST_USER_EMAIL);
			user.setPassword(TEST_USER_PASS);
			Result authenticatedUserResult = callAuthenticateUser(user);
			assertTrue(authenticatedUserResult.isSuccess());


			result = callIsEnterpriseEdition();
			if (!result.isSuccess()) {
				//if it is not enterprise return
				return ;
			}

			//get app settings
			AppSettingsModel appSettingsModel = callGetAppSettings("LiveApp");


			//check that preview overwrite is false by default
			assertFalse(appSettingsModel.isPreviewOverwrite());

			//send a short stream
			String streamId = "test_stream_" + (int)(Math.random() * 1000);
			AppFunctionalTest.executeProcess(ffmpegPath
					+ " -re -i src/test/resources/test.flv -acodec copy -vcodec copy -f flv rtmp://localhost/LiveApp/"
					+ streamId);

			Thread.sleep(5000);

			//stop it
			AppFunctionalTest.destroyProcess();

			//check that preview is created
			assertTrue(checkURLExist("http://localhost:5080/LiveApp/previews/"+streamId+".png"));


			//send a short stream with same name again
			AppFunctionalTest.executeProcess(ffmpegPath
					+ " -re -i src/test/resources/test.flv -acodec copy -vcodec copy -f flv rtmp://localhost/LiveApp/"
					+ streamId);

			Thread.sleep(5000);

			//stop it
			AppFunctionalTest.destroyProcess();

			//let the muxing finish
			Thread.sleep(3000);
			//check that second preview is created
			assertTrue(checkURLExist("http://localhost:5080/LiveApp/previews/"+streamId+"_1.png"));

			//change settings and make preview overwrite true
			appSettingsModel.setPreviewOverwrite(true);

			result = callSetAppSettings("LiveApp", appSettingsModel);
			assertTrue(result.isSuccess());

			appSettingsModel = callGetAppSettings("LiveApp");
			assertTrue(appSettingsModel.isPreviewOverwrite());

			streamId = "test_stream_" + (int)(Math.random() * 1000);
			AppFunctionalTest.executeProcess(ffmpegPath
					+ " -re -i src/test/resources/test.flv -acodec copy -vcodec copy -f flv rtmp://localhost/LiveApp/"
					+ streamId);

			Thread.sleep(5000);

			//stop it
			AppFunctionalTest.destroyProcess();

			//check that preview is created
			assertTrue(checkURLExist("http://localhost:5080/LiveApp/previews/"+streamId+".png"));

			//send a short stream with same name again
			AppFunctionalTest.executeProcess(ffmpegPath
					+ " -re -i src/test/resources/test.flv -acodec copy -vcodec copy -f flv rtmp://localhost/LiveApp/"
					+ streamId);

			//let the muxing finish
			Thread.sleep(5000);

			//stop it
			AppFunctionalTest.destroyProcess();

			Thread.sleep(3000);

			//check that second preview with the same created.
			assertTrue(checkURLExist("http://localhost:5080/LiveApp/previews/"+streamId+".png"));


			appSettingsModel.setPreviewOverwrite(false);
			result = callSetAppSettings("LiveApp", appSettingsModel);
			assertTrue(result.isSuccess());


			appSettingsModel = callGetAppSettings("LiveApp");
			assertFalse(appSettingsModel.isPreviewOverwrite());

		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	@Test
	public void testAllowOnlyStreamsInDataStore() {
		try {
			// first authenticate user

			User user = new User();
			user.setEmail(TEST_USER_EMAIL);
			user.setPassword(TEST_USER_PASS);
			Result authenticatedUserResult = callAuthenticateUser(user);
			assertTrue(authenticatedUserResult.isSuccess());

			// get settings from the app
			AppSettingsModel appSettingsModel = callGetAppSettings("LiveApp");

			// assertFalse(appSettingsModel.acceptOnlyStreamsInDataStore);

			// change settings test testAllowOnlyStreamsInDataStore is true
			appSettingsModel.setAcceptOnlyStreamsInDataStore(true);

			Result result = callSetAppSettings("LiveApp", appSettingsModel);
			assertTrue(result.isSuccess());

			// check app settings
			appSettingsModel = callGetAppSettings("LiveApp");
			assertTrue(appSettingsModel.isAcceptOnlyStreamsInDataStore());

			// send anonymous stream
			String streamId = "zombiStreamId";
			AppFunctionalTest.executeProcess(ffmpegPath
					+ " -re -i src/test/resources/test.flv -acodec copy -vcodec copy -f flv rtmp://localhost/LiveApp/"
					+ streamId);

			Thread.sleep(5000);

			// check that it is not accepted
			Broadcast broadcast = RestServiceTest.callGetBroadcast(streamId);
			assertNull(broadcast.getStreamId());

			AppFunctionalTest.destroyProcess();

			// create a stream through rest service
			// check that it is accepted
			{
				Broadcast broadcastCreated = RestServiceTest.callCreateBroadcast(10000);
				assertNotNull(broadcastCreated.getStreamId());
				assertEquals(broadcastCreated.getStatus(), Application.BROADCAST_STATUS_CREATED);

				AppFunctionalTest.executeProcess(ffmpegPath
						+ " -re -i src/test/resources/test.flv -acodec copy -vcodec copy -f flv rtmp://localhost/LiveApp/"
						+ broadcastCreated.getStreamId());

				Thread.sleep(5000);
				assertTrue(AppFunctionalTest.isProcessAlive());

				broadcast = RestServiceTest.callGetBroadcast(broadcastCreated.getStreamId());
				assertNotNull(broadcast);
				assertEquals(broadcast.getStatus(), Application.BROADCAST_STATUS_BROADCASTING);

				AppFunctionalTest.destroyProcess();
			}

			// change settings testAllowOnlyStreamsInDataStore to false
			appSettingsModel.setAcceptOnlyStreamsInDataStore(false);
			result = callSetAppSettings("LiveApp", appSettingsModel);
			assertTrue(result.isSuccess());

			AppSettingsModel callGetAppSettings = callGetAppSettings("LiveApp");
			assertFalse(appSettingsModel.isAcceptOnlyStreamsInDataStore());

			// send anonymous stream
			{
				streamId = "zombiStreamId";
				AppFunctionalTest.executeProcess(ffmpegPath
						+ " -re -i src/test/resources/test.flv -acodec copy -vcodec copy -f flv rtmp://localhost/LiveApp/"
						+ streamId);

				Thread.sleep(5000);

				// check that it is accepted
				broadcast = RestServiceTest.callGetBroadcast(streamId);
				assertNotNull(broadcast.getStreamId());
				assertEquals(broadcast.getStatus(), AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING);

				AppFunctionalTest.destroyProcess();
			}

			// create a stream through rest service
			// check that it is accepted
			{
				Broadcast broadcastCreated = RestServiceTest.callCreateBroadcast(10000);
				assertNotNull(broadcastCreated.getStreamId());
				assertEquals(broadcastCreated.getStatus(), Application.BROADCAST_STATUS_CREATED);

				AppFunctionalTest.executeProcess(ffmpegPath
						+ " -re -i src/test/resources/test.flv -acodec copy -vcodec copy -f flv rtmp://localhost/LiveApp/"
						+ broadcastCreated.getStreamId());

				Thread.sleep(5000);
				assertTrue(AppFunctionalTest.isProcessAlive());

				broadcast = RestServiceTest.callGetBroadcast(broadcastCreated.getStreamId());
				assertNotNull(broadcast);
				assertEquals(broadcast.getStatus(), Application.BROADCAST_STATUS_BROADCASTING);

				AppFunctionalTest.destroyProcess();
			}

			{
				// change settings and accept only streams in data store
				appSettingsModel.setAcceptOnlyStreamsInDataStore(true);
				result = callSetAppSettings("LiveApp", appSettingsModel);
				assertTrue(result.isSuccess());

				callGetAppSettings = callGetAppSettings("LiveApp");
				assertTrue(appSettingsModel.isAcceptOnlyStreamsInDataStore());
			}

			// send anonymous stream
			// check that it is not accepted
			{
				streamId = "zombiStreamId";
				AppFunctionalTest.executeProcess(ffmpegPath
						+ " -re -i src/test/resources/test.flv -acodec copy -vcodec copy -f flv rtmp://localhost/LiveApp/"
						+ streamId);

				Thread.sleep(5000);

				// check that it is not accepted
				broadcast = RestServiceTest.callGetBroadcast(streamId);
				assertNull(broadcast.getStreamId());
				AppFunctionalTest.destroyProcess();
			}

			// create a stream through rest service
			// check that it is accepted
			{
				Broadcast broadcastCreated = RestServiceTest.callCreateBroadcast(10000);
				assertNotNull(broadcastCreated.getStreamId());
				assertEquals(broadcastCreated.getStatus(), Application.BROADCAST_STATUS_CREATED);

				AppFunctionalTest.executeProcess(ffmpegPath
						+ " -re -i src/test/resources/test.flv -acodec copy -vcodec copy -f flv rtmp://localhost/LiveApp/"
						+ broadcastCreated.getStreamId());

				Thread.sleep(5000);
				assertTrue(AppFunctionalTest.isProcessAlive());

				broadcast = RestServiceTest.callGetBroadcast(broadcastCreated.getStreamId());
				assertNotNull(broadcast);
				assertEquals(broadcast.getStatus(), Application.BROADCAST_STATUS_BROADCASTING);

				AppFunctionalTest.destroyProcess();
			}

			{
				// change settings and accept only streams to false, because it
				// effects other tests in data store
				appSettingsModel.setAcceptOnlyStreamsInDataStore(false);
				result = callSetAppSettings("LiveApp", appSettingsModel);
				assertTrue(result.isSuccess());

				callGetAppSettings = callGetAppSettings("LiveApp");
				assertFalse(appSettingsModel.isAcceptOnlyStreamsInDataStore());
			}

		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	@Test
	public void testTokenControl() {
		Result enterpriseResult;
		try {

			// authenticate user
			User user = new User();
			user.setEmail(TEST_USER_EMAIL);
			user.setPassword(TEST_USER_PASS);
			Result authenticatedUserResult = callAuthenticateUser(user);
			assertTrue(authenticatedUserResult.isSuccess());

			enterpriseResult = callIsEnterpriseEdition();
			if (!enterpriseResult.isSuccess()) {
				//if it is not enterprise return
				return ;
			}

			// get settings from the app
			AppSettingsModel appSettings = callGetAppSettings("LiveApp");

			appSettings.setTokenControlEnabled(true);
			appSettings.setMp4MuxingEnabled(true);


			Result result = callSetAppSettings("LiveApp", appSettings);
			assertTrue(result.isSuccess());

			appSettings = callGetAppSettings("LiveApp");
			assertTrue(appSettings.isTokenControlEnabled());

			Broadcast broadcast = RestServiceTest.callCreateRegularBroadcast();
			Token accessToken = callGetToken(broadcast.getStreamId(), Token.PLAY_TOKEN, 15444343);
			assertNotNull(accessToken);


			Process rtmpSendingProcess = execute(ffmpegPath
					+ " -re -i src/test/resources/test.flv  -codec copy -f flv rtmp://127.0.0.1/LiveApp/"
					+ broadcast.getStreamId());


			//it should be false, because publishing is not allowed and hls files are not created
			Awaitility.await().pollDelay(5, TimeUnit.SECONDS).atMost(10, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
				return ConsoleAppRestServiceTest.getStatusCode("http://" + SERVER_ADDR + ":5080/LiveApp/streams/" + broadcast.getStreamId() + ".m3u8?token=" + accessToken.getTokenId())==404;
			});

			rtmpSendingProcess.destroy();


			//create token for publishing
			Token publishToken = callGetToken(broadcast.getStreamId(), Token.PUBLISH_TOKEN, 15444343);
			assertNotNull(publishToken);

			//create token for playing/accessing file
			Token accessToken2 = callGetToken(broadcast.getStreamId(), Token.PLAY_TOKEN, 15444343);
			assertNotNull(accessToken2);

			Process rtmpSendingProcessToken = execute(ffmpegPath
					+ " -re -i src/test/resources/test.flv  -codec copy -f flv rtmp://127.0.0.1/LiveApp/"
					+ broadcast.getStreamId()+ "?token=" + publishToken.getTokenId());


			//it should be false because token control is enabled but no token provided

			Awaitility.await()
			.pollDelay(5, TimeUnit.SECONDS)
			.atMost(10, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(()-> {
				return  !MuxingTest.testFile("http://" + SERVER_ADDR + ":5080/LiveApp/streams/" 
						+ broadcast.getStreamId() + ".mp4");
			});

			rtmpSendingProcessToken.destroy();

			//this time, it should be true since valid token is provided
			Awaitility.await().atMost(10, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
				return MuxingTest.testFile("http://" + SERVER_ADDR + ":5080/LiveApp/streams/" 
						+ broadcast.getStreamId() + ".mp4?token=" + accessToken2.getTokenId());
			});

			appSettings.setTokenControlEnabled(false);


			Result flag = callSetAppSettings("LiveApp", appSettings);
			assertTrue(flag.isSuccess());



		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}


	@Test
	public void testLicenseControl() {

		Result enterpriseResult;
		try {

			// authenticate user
			User user = new User();
			user.setEmail(TEST_USER_EMAIL);
			user.setPassword(TEST_USER_PASS);
			Result authenticatedUserResult = callAuthenticateUser(user);
			assertTrue(authenticatedUserResult.isSuccess());

			enterpriseResult = callIsEnterpriseEdition();
			if (!enterpriseResult.isSuccess()) {
				//if it is not enterprise return
				return ;
			}

			// get Server Settings
			ServerSettings serverSettings = callGetServerSettings();

			//set test license key
			serverSettings.setLicenceKey("test-test");
			serverSettings.setBuildForMarket(false);

			Result flag = callSetServerSettings(serverSettings);

			
			//request license check via rest service
			Licence activeLicence = callGetLicenceStatus(serverSettings.getLicenceKey());


			//it should not be null because test license key is active
			assertNotNull(activeLicence);

			//set build for market as true
			serverSettings.setBuildForMarket(true);

			//save this setting
			flag = callSetServerSettings(serverSettings);

			//check that setting is saved
			assertTrue (flag.isSuccess());

			
			//check license status

			activeLicence = callGetLicenceStatus(serverSettings.getLicenceKey());

			//it should be null because it is market build
			assertNull(activeLicence);



			//set build for market setting to default
			serverSettings.setBuildForMarket(false);

			//save default setting
			flag = callSetServerSettings(serverSettings);

			//check that setting is saved
			assertTrue (flag.isSuccess());


		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	@Test
	public void testHashControl() {
		Result enterpiseResult;
		try {


			// authenticate user
			User user = new User();
			user.setEmail(TEST_USER_EMAIL);
			user.setPassword(TEST_USER_PASS);
			Result authenticatedUserResult = callAuthenticateUser(user);
			assertTrue(authenticatedUserResult.isSuccess());

			enterpiseResult = callIsEnterpriseEdition();
			if (!enterpiseResult.isSuccess()) {
				//if it is not enterprise return
				return ;
			}

			// get settings from the app
			AppSettingsModel appSettings = callGetAppSettings("LiveApp");

			//set hash publish control enabled
			appSettings.setHashControlPublishEnabled(true);
			appSettings.setMp4MuxingEnabled(true);

			//define hash secret 
			String secret = "secret";
			appSettings.setTokenHashSecret(secret);


			Result result = callSetAppSettings("LiveApp", appSettings);
			assertTrue(result.isSuccess());

			appSettings = callGetAppSettings("LiveApp");

			//check that settings is set successfully
			assertTrue(appSettings.isHashControlPublishEnabled());

			//create a broadcast
			Broadcast broadcast = RestServiceTest.callCreateRegularBroadcast();

			//publish stream
			Process rtmpSendingProcess = execute(ffmpegPath
					+ " -re -i src/test/resources/test.flv  -codec copy -f flv rtmp://127.0.0.1/LiveApp/"
					+ broadcast.getStreamId());


			//publishing is not allowed therefore hls files are not created
			Awaitility.await().pollDelay(5, TimeUnit.SECONDS).atMost(10, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
				return ConsoleAppRestServiceTest.getStatusCode("http://" + SERVER_ADDR + ":5080/LiveApp/streams/" + broadcast.getStreamId() + ".m3u8?token=hash" )==404;
			});

			rtmpSendingProcess.destroy();

			//generate correct hash value

			String hashCombine = broadcast.getStreamId() + Token.PUBLISH_TOKEN + secret ;

			String sha256hex = Hashing.sha256()
					.hashString(hashCombine, StandardCharsets.UTF_8)
					.toString();


			//start publish with generated hash value
			Process rtmpSendingProcessHash = execute(ffmpegPath
					+ " -re -i src/test/resources/test.flv  -codec copy -f flv rtmp://127.0.0.1/LiveApp/"
					+ broadcast.getStreamId()+ "?token=" + sha256hex);



			//this time, HLS files should be created
			Awaitility.await().atMost(10, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
				return MuxingTest.testFile("http://" + SERVER_ADDR + ":5080/LiveApp/streams/" 
						+ broadcast.getStreamId() + ".m3u8" );
			});

			//destroy process
			rtmpSendingProcessHash.destroy();

			//return back settings for other tests
			appSettings.setHashControlPublishEnabled(false);

			Result flag = callSetAppSettings("LiveApp", appSettings);

			//check that settings saved successfully
			assertTrue(flag.isSuccess());

		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	@Test
	public void testGetSystemResourcesInfo() {
		try {
			// authenticate user
			User user = new User();
			user.setEmail(TEST_USER_EMAIL);
			user.setPassword(TEST_USER_PASS);
			Result authenticatedUserResult;
			authenticatedUserResult = callAuthenticateUser(user);
			assertTrue(authenticatedUserResult.isSuccess());

			String systemResourcesInfo = callGetSystemResourcesInfo();

			JSONParser parser = new JSONParser();		
			JSONObject jsObject = (JSONObject) parser.parse(systemResourcesInfo);
			JSONObject tmpObject = (JSONObject) jsObject.get("cpuUsage");
			assertTrue(tmpObject.containsKey("processCPUTime"));
			assertTrue(tmpObject.containsKey("systemCPULoad"));
			assertTrue(tmpObject.containsKey("processCPULoad"));

			tmpObject = (JSONObject) jsObject.get("jvmMemoryUsage"); 
			assertTrue(tmpObject.containsKey("maxMemory"));
			assertTrue(tmpObject.containsKey("totalMemory"));
			assertTrue(tmpObject.containsKey("freeMemory"));
			assertTrue(tmpObject.containsKey("inUseMemory"));


			tmpObject = (JSONObject) jsObject.get("systemInfo"); 
			assertTrue(tmpObject.containsKey("osName"));
			assertTrue(tmpObject.containsKey("osArch"));
			assertTrue(tmpObject.containsKey("javaVersion"));
			assertTrue(tmpObject.containsKey("processorCount"));

			tmpObject = (JSONObject) jsObject.get("systemMemoryInfo"); 
			assertTrue(tmpObject.containsKey("virtualMemory"));
			assertTrue(tmpObject.containsKey("totalMemory"));
			assertTrue(tmpObject.containsKey("freeMemory"));
			assertTrue(tmpObject.containsKey("inUseMemory"));
			assertTrue(tmpObject.containsKey("totalSwapSpace"));
			assertTrue(tmpObject.containsKey("freeSwapSpace"));
			assertTrue(tmpObject.containsKey("inUseSwapSpace"));

			tmpObject = (JSONObject) jsObject.get("fileSystemInfo"); 
			assertTrue(tmpObject.containsKey("usableSpace"));
			assertTrue(tmpObject.containsKey("totalSpace"));
			assertTrue(tmpObject.containsKey("freeSpace"));
			assertTrue(tmpObject.containsKey("inUseSpace"));

			assertTrue(jsObject.containsKey("gpuUsageInfo"));

			assertTrue(jsObject.containsKey("totalLiveStreamSize"));

			System.out.println("system resource info: " + systemResourcesInfo);

		}
		catch (Exception e) {
			fail(e.getMessage());
		}

	}

	@Test
	public void testGetVersion() {
		try {
			// authenticate user
			User user = new User();
			user.setEmail(TEST_USER_EMAIL);
			user.setPassword(TEST_USER_PASS);
			Result authenticatedUserResult;
			authenticatedUserResult = callAuthenticateUser(user);
			assertTrue(authenticatedUserResult.isSuccess());

			String version = callGetSoftwareVersion();

			Version versionObj = gson.fromJson(version, Version.class);

			assertEquals(13 , versionObj.getBuildNumber().length());
			assertNotNull(versionObj.getVersionType());
			assertNotEquals("null", versionObj.getVersionType());

			assertNotNull(versionObj.getVersionName());
			assertNotEquals("null", versionObj.getVersionName());

		}
		catch (Exception e) {
			fail(e.getMessage());
		}

	}

	@Test
	public void testMp4Setting() {
		/**
		 * This is testing stream-specific mp4 setting via rest service and results
		 */
		try {

			// authenticate user
			User user = new User();
			user.setEmail(TEST_USER_EMAIL);
			user.setPassword(TEST_USER_PASS);
			Result authenticatedUserResult;
			authenticatedUserResult = callAuthenticateUser(user);
			assertTrue(authenticatedUserResult.isSuccess());

			// get settings from the app
			AppSettingsModel appSettings = callGetAppSettings("LiveApp");

			//disable mp4 muxing
			appSettings.setMp4MuxingEnabled(false);
			appSettings.setHlsMuxingEnabled(true);
			Result result = callSetAppSettings("LiveApp", appSettings);
			assertTrue(result.isSuccess());

			// create broadcast
			Broadcast broadcast = RestServiceTest.callCreateRegularBroadcast();

			/**
			 * CASE 1: General setting is disabled (default) and stream setting is 0 (default)
			 */

			Process rtmpSendingProcess = execute(ffmpegPath
					+ " -re -i src/test/resources/test.flv  -codec copy -f flv rtmp://127.0.0.1/LiveApp/"
					+ broadcast.getStreamId());

			//wait until stream is broadcasted
			Awaitility.await().atMost(15, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
				return MuxingTest.testFile("http://" + SERVER_ADDR + ":5080/LiveApp/streams/" + broadcast.getStreamId() + ".m3u8");
			});

			rtmpSendingProcess.destroy();

			//it should be false, because mp4 settings is disabled and stream mp4 setting is 0, so mp4 file not created
			Awaitility.await().atMost(15, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
				return !MuxingTest.testFile("http://" + SERVER_ADDR + ":5080/LiveApp/streams/" + broadcast.getStreamId() + ".mp4");
			});

			/**
			 * CASE 2: General setting is disabled (default) and stream setting is 1 
			 */

			//create new stream to avoid same stream name
			Broadcast broadcast2 = RestServiceTest.callCreateRegularBroadcast();

			//set stream specific mp4 setting to 1, general setting is still disabled
			result = RestServiceTest.callEnableMp4Muxing(broadcast2.getStreamId(), MuxAdaptor.MP4_ENABLED_FOR_STREAM);

			assertTrue(result.isSuccess());

			//send stream
			rtmpSendingProcess = execute(ffmpegPath
					+ " -re -i src/test/resources/test.flv  -codec copy -f flv rtmp://127.0.0.1/LiveApp/"
					+ broadcast2.getStreamId());

			//wait until stream is broadcasted
			Awaitility.await().atMost(15, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
				return MuxingTest.testFile("http://" + SERVER_ADDR + ":5080/LiveApp/streams/" + broadcast2.getStreamId() + ".m3u8");
			});


			rtmpSendingProcess.destroy();

			//it should be true this time, because stream mp4 setting is 1 although general setting is disabled
			Awaitility.await().atMost(15, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
				return MuxingTest.testFile("http://" + SERVER_ADDR + ":5080/LiveApp/streams/" + broadcast2.getStreamId() + ".mp4");
			});

			/**
			 * CASE 3: General setting is enabled and stream setting is 0 
			 */

			//create new stream to avoid same stream name
			Broadcast broadcast3 = RestServiceTest.callCreateRegularBroadcast();

			//enable mp4 muxing
			appSettings.setMp4MuxingEnabled(true);
			result = callSetAppSettings("LiveApp", appSettings);
			assertTrue(result.isSuccess());

			//set stream spesific mp4 settings to 0
			result = RestServiceTest.callEnableMp4Muxing(broadcast3.getStreamId(), MuxAdaptor.MP4_NO_SET_FOR_STREAM);
			assertTrue(result.isSuccess());


			//send stream
			rtmpSendingProcess = execute(ffmpegPath
					+ " -re -i src/test/resources/test.flv  -codec copy -f flv rtmp://127.0.0.1/LiveApp/"
					+ broadcast3.getStreamId());

			//wait until stream is broadcasted
			Awaitility.await().atMost(15, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
				return MuxingTest.testFile("http://" + SERVER_ADDR + ":5080/LiveApp/streams/" + broadcast3.getStreamId() + ".m3u8");
			});

			rtmpSendingProcess.destroy();

			//it should be true this time also, because stream mp4 setting is 0 but general setting is enabled
			Awaitility.await().atMost(15, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
				return MuxingTest.testFile("http://" + SERVER_ADDR + ":5080/LiveApp/streams/" + broadcast3.getStreamId() + ".mp4");
			});

			/**
			 * CASE 4: General setting is enabled (default) and stream setting is -1 
			 */

			// create new broadcast because mp4 files exist with same streamId
			Broadcast broadcast4 = RestServiceTest.callCreateRegularBroadcast();

			// general setting is still enabled and set stream spesific mp4 settings to -1
			result = RestServiceTest.callEnableMp4Muxing(broadcast4.getStreamId(), MuxAdaptor.MP4_DISABLED_FOR_STREAM);
			assertTrue(result.isSuccess());

			//send stream
			rtmpSendingProcess = execute(ffmpegPath
					+ " -re -i src/test/resources/test.flv  -codec copy -f flv rtmp://127.0.0.1/LiveApp/"
					+ broadcast4.getStreamId());

			//wait until stream is broadcasted
			Awaitility.await().atMost(15, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
				return MuxingTest.testFile("http://" + SERVER_ADDR + ":5080/LiveApp/streams/" + broadcast4.getStreamId() + ".m3u8");
			});

			rtmpSendingProcess.destroy();

			//it should be false this time, because stream mp4 setting is -1 althouh general setting is enabled
			Awaitility.await().atMost(15, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
				return !MuxingTest.testFile("http://" + SERVER_ADDR + ":5080/LiveApp/streams/" + broadcast4.getStreamId() + ".mp4");
			});


			//disable mp4 muxing
			appSettings.setMp4MuxingEnabled(false);
			result = callSetAppSettings("LiveApp", appSettings);
			assertTrue(result.isSuccess());


		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	public static Token callGetToken(String streamId, String type, long expireDate) throws Exception {
		String url = SERVICE_URL + "/broadcast/getToken";

		CloseableHttpClient client = HttpClients.custom().setRedirectStrategy(new LaxRedirectStrategy()).build();

		HttpUriRequest get = RequestBuilder.get().setUri(url + "?id=" + streamId + "&expireDate=" + expireDate + "&type=" + type)
				.setHeader(HttpHeaders.CONTENT_TYPE, "application/json")
				.build();

		CloseableHttpResponse response = client.execute(get);

		StringBuffer result = readResponse(response);

		if (response.getStatusLine().getStatusCode() != 200) {
			throw new Exception(result.toString());
		}
		System.out.println("result string: " + result.toString());

		return gson.fromJson(result.toString(), Token.class);
	}

	public static int getStatusCode(String url) throws Exception {

		log.info("url: {}",url);

		HttpClient client = HttpClients.custom().setRedirectStrategy(new LaxRedirectStrategy())
				.setDefaultCookieStore(httpCookieStore).build();

		HttpUriRequest post = RequestBuilder.get().setUri(url).build();

		HttpResponse response = client.execute(post);

		StringBuffer result = RestServiceTest.readResponse(response);

		log.info("response status code: {}",response.getStatusLine().getStatusCode());

		return response.getStatusLine().getStatusCode();
	}

	public static boolean checkURLExist(String url) throws Exception {
		int statusCode = getStatusCode(url);
		if (statusCode == 200) {
			return true;
		}
		return false;
	}



	public static Result callisFirstLogin() throws Exception {
		String url = ROOT_SERVICE_URL + "/isFirstLogin";
		HttpClient client = HttpClients.custom().setRedirectStrategy(new LaxRedirectStrategy()).build();
		Gson gson = new Gson();
		HttpUriRequest post = RequestBuilder.get().setUri(url).setHeader(HttpHeaders.CONTENT_TYPE, "application/json")
				.build();

		HttpResponse response = client.execute(post);

		StringBuffer result = RestServiceTest.readResponse(response);

		if (response.getStatusLine().getStatusCode() != 200) {
			throw new Exception(result.toString());
		}
		log.info("result string: " + result.toString());
		Result tmp = gson.fromJson(result.toString(), Result.class);
		assertNotNull(tmp);
		return tmp;

	}

	public static Result callCreateInitialUser(User user) throws Exception {

		String url = ROOT_SERVICE_URL + "/addInitialUser";
		HttpClient client = HttpClients.custom().setRedirectStrategy(new LaxRedirectStrategy()).build();
		Gson gson = new Gson();
		HttpUriRequest post = RequestBuilder.post().setUri(url).setHeader(HttpHeaders.CONTENT_TYPE, "application/json")
				.setEntity(new StringEntity(gson.toJson(user))).build();

		HttpResponse response = client.execute(post);

		StringBuffer result = RestServiceTest.readResponse(response);

		if (response.getStatusLine().getStatusCode() != 200) {
			throw new Exception(result.toString());
		}
		log.info("result string: " + result.toString());
		Result tmp = gson.fromJson(result.toString(), Result.class);
		assertNotNull(tmp);
		return tmp;
	}

	private static Result callAuthenticateUser(User user) throws Exception {
		String url = ROOT_SERVICE_URL + "/authenticateUser";
		HttpClient client = HttpClients.custom().setRedirectStrategy(new LaxRedirectStrategy())
				.setDefaultCookieStore(httpCookieStore).build();
		Gson gson = new Gson();
		HttpUriRequest post = RequestBuilder.post().setUri(url).setHeader(HttpHeaders.CONTENT_TYPE, "application/json")
				.setEntity(new StringEntity(gson.toJson(user))).build();

		HttpResponse response = client.execute(post);

		StringBuffer result = RestServiceTest.readResponse(response);

		if (response.getStatusLine().getStatusCode() != 200) {
			throw new Exception(result.toString());
		}
		log.info("result string: " + result.toString());
		Result tmp = gson.fromJson(result.toString(), Result.class);
		assertNotNull(tmp);
		return tmp;
	}

	public static Result callSetAppSettings(String appName, AppSettingsModel appSettingsModel) throws Exception {
		String url = ROOT_SERVICE_URL + "/changeSettings/" + appName;
		HttpClient client = HttpClients.custom().setRedirectStrategy(new LaxRedirectStrategy())
				.setDefaultCookieStore(httpCookieStore).build();
		Gson gson = new Gson();

		HttpUriRequest post = RequestBuilder.post().setUri(url).setHeader(HttpHeaders.CONTENT_TYPE, "application/json")
				.setEntity(new StringEntity(gson.toJson(appSettingsModel))).build();

		HttpResponse response = client.execute(post);

		StringBuffer result = RestServiceTest.readResponse(response);

		if (response.getStatusLine().getStatusCode() != 200) {
			throw new Exception(result.toString());
		}
		log.info("result string: " + result.toString());
		Result tmp = gson.fromJson(result.toString(), Result.class);
		assertNotNull(tmp);
		return tmp;

	}



	public static Result callSetServerSettings(ServerSettings serverSettings) throws Exception {
		String url = ROOT_SERVICE_URL + "/changeServerSettings";
		HttpClient client = HttpClients.custom().setRedirectStrategy(new LaxRedirectStrategy())
				.setDefaultCookieStore(httpCookieStore).build();
		Gson gson = new Gson();

		HttpUriRequest post = RequestBuilder.post().setUri(url).setHeader(HttpHeaders.CONTENT_TYPE, "application/json")
				.setEntity(new StringEntity(gson.toJson(serverSettings))).build();

		HttpResponse response = client.execute(post);

		StringBuffer result = RestServiceTest.readResponse(response);

		if (response.getStatusLine().getStatusCode() != 200) {
			throw new Exception(result.toString());
		}
		log.info("result string: " + result.toString());
		Result tmp = gson.fromJson(result.toString(), Result.class);
		assertNotNull(tmp);
		return tmp;

	}


	public static String callGetApplications() throws Exception {
		String url = ROOT_SERVICE_URL + "/getApplications";

		HttpClient client = HttpClients.custom().setRedirectStrategy(new LaxRedirectStrategy())
				.setDefaultCookieStore(httpCookieStore).build();
		Gson gson = new Gson();

		HttpUriRequest post = RequestBuilder.get().setUri(url).build();

		HttpResponse response = client.execute(post);

		StringBuffer result = RestServiceTest.readResponse(response);
		if (response.getStatusLine().getStatusCode() != 200) {
			throw new Exception(result.toString());
		}
		log.info("result string: " + result.toString());
		return result.toString();
	}



	public static String callGetSoftwareVersion() throws Exception {
		String url = ROOT_SERVICE_URL + "/getVersion";
		HttpClient client = HttpClients.custom().setRedirectStrategy(new LaxRedirectStrategy())
				.setDefaultCookieStore(httpCookieStore).build();
		Gson gson = new Gson();

		HttpUriRequest post = RequestBuilder.get().setUri(url).build();

		HttpResponse response = client.execute(post);

		StringBuffer result = RestServiceTest.readResponse(response);
		if (response.getStatusLine().getStatusCode() != 200) {
			throw new Exception(result.toString());
		}
		log.info("result string: " + result.toString());
		return result.toString();
	}


	public static String callGetSystemResourcesInfo() throws Exception {
		String url = ROOT_SERVICE_URL + "/getSystemResourcesInfo";
		HttpClient client = HttpClients.custom().setRedirectStrategy(new LaxRedirectStrategy())
				.setDefaultCookieStore(httpCookieStore).build();
		Gson gson = new Gson();

		HttpUriRequest post = RequestBuilder.get().setUri(url).build();

		HttpResponse response = client.execute(post);

		StringBuffer result = RestServiceTest.readResponse(response);
		if (response.getStatusLine().getStatusCode() != 200) {
			throw new Exception(result.toString());
		}
		log.info("result string: " + result.toString());
		return result.toString();
	}

	public static Result callIsClusterMode() throws Exception {
		String url = ROOT_SERVICE_URL + "/isInClusterMode";

		HttpClient client = HttpClients.custom().setRedirectStrategy(new LaxRedirectStrategy())
				.setDefaultCookieStore(httpCookieStore).build();
		Gson gson = new Gson();

		HttpUriRequest post = RequestBuilder.get().setUri(url).build();

		HttpResponse response = client.execute(post);

		StringBuffer result = RestServiceTest.readResponse(response);
		if (response.getStatusLine().getStatusCode() != 200) {
			throw new Exception(result.toString());
		}
		log.info("result string: " + result.toString());
		Result tmp = gson.fromJson(result.toString(), Result.class);
		assertNotNull(tmp);
		return tmp;
	}

	public static Result callIsEnterpriseEdition() throws Exception {
		String url = ROOT_SERVICE_URL + "/isEnterpriseEdition";

		HttpClient client = HttpClients.custom().setRedirectStrategy(new LaxRedirectStrategy())
				.setDefaultCookieStore(httpCookieStore).build();
		Gson gson = new Gson();

		HttpUriRequest post = RequestBuilder.get().setUri(url).build();

		HttpResponse response = client.execute(post);

		StringBuffer result = RestServiceTest.readResponse(response);
		if (response.getStatusLine().getStatusCode() != 200) {
			throw new Exception(result.toString());
		}
		log.info("result string: " + result.toString());
		Result tmp = gson.fromJson(result.toString(), Result.class);
		assertNotNull(tmp);
		return tmp;

	}

	public static AppSettingsModel callGetAppSettings(String appName) throws Exception {

		String url = ROOT_SERVICE_URL + "/getSettings/" + appName;

		HttpClient client = HttpClients.custom().setRedirectStrategy(new LaxRedirectStrategy())
				.setDefaultCookieStore(httpCookieStore).build();
		Gson gson = new Gson();

		HttpUriRequest post = RequestBuilder.get().setUri(url).build();

		HttpResponse response = client.execute(post);

		StringBuffer result = RestServiceTest.readResponse(response);

		if (response.getStatusLine().getStatusCode() != 200) {
			System.out.println("status code: " + response.getStatusLine().getStatusCode());
			throw new Exception(result.toString());
		}
		log.info("result string: " + result.toString());
		AppSettingsModel tmp = gson.fromJson(result.toString(), AppSettingsModel.class);
		assertNotNull(tmp);
		return tmp;
	}

	public static ServerSettings callGetServerSettings() throws Exception {

		String url = ROOT_SERVICE_URL + "/getServerSettings";

		HttpClient client = HttpClients.custom().setRedirectStrategy(new LaxRedirectStrategy())
				.setDefaultCookieStore(httpCookieStore).build();
		Gson gson = new Gson();

		HttpUriRequest post = RequestBuilder.get().setUri(url).build();

		HttpResponse response = client.execute(post);

		StringBuffer result = RestServiceTest.readResponse(response);

		if (response.getStatusLine().getStatusCode() != 200) {
			System.out.println("status code: " + response.getStatusLine().getStatusCode());
			throw new Exception(result.toString());
		}
		log.info("result string: " + result.toString());
		ServerSettings tmp = gson.fromJson(result.toString(), ServerSettings.class);
		assertNotNull(tmp);
		return tmp;
	}

	public static Licence callGetLicenceStatus(String key) throws Exception {

		Licence tmp = null;

		String url = ROOT_SERVICE_URL + "/getLicenceStatus/" + key;

		HttpClient client = HttpClients.custom().setRedirectStrategy(new LaxRedirectStrategy())
				.setDefaultCookieStore(httpCookieStore).build();
		Gson gson = new Gson();

		HttpUriRequest post = RequestBuilder.get().setUri(url).build();

		HttpResponse response = client.execute(post);

		StringBuffer result = RestServiceTest.readResponse(response);

		log.info("result string: " + result.toString());
		tmp = gson.fromJson(result.toString(), Licence.class);

		return tmp;
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
						log.info(new String(data, 0, length));
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			};
		}.start();

		while (tmpExec == null) {
			log.info("Waiting for exec get initialized...");

			Awaitility.await().pollDelay(1, TimeUnit.SECONDS).atMost(10, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
				return tmpExec !=null;
			});
		}

		return tmpExec;
	}

	public static String callGetLogLevel() throws Exception {

		String url = ROOT_SERVICE_URL + "/getLogLevel";

		HttpClient client = HttpClients.custom().setRedirectStrategy(new LaxRedirectStrategy())
				.setDefaultCookieStore(httpCookieStore).build();

		Gson gson = new Gson();

		HttpUriRequest post = RequestBuilder.get().setUri(url).build();

		HttpResponse response = client.execute(post);

		StringBuffer result = RestServiceTest.readResponse(response);

		if (response.getStatusLine().getStatusCode() != 200) {
			System.out.println("status code: " + response.getStatusLine().getStatusCode());
			throw new Exception(result.toString());
		}

		log.info("result string: " + result.toString());
		return result.toString();
	}

	public static Result callSetLogLevel(String level) throws Exception {

		String url = ROOT_SERVICE_URL + "/changeLogLevel/"+level;

		HttpClient client = HttpClients.custom().setRedirectStrategy(new LaxRedirectStrategy())
				.setDefaultCookieStore(httpCookieStore).build();

		Gson gson = new Gson();

		HttpUriRequest post = RequestBuilder.get().setUri(url).build();

		HttpResponse response = client.execute(post);

		StringBuffer result = RestServiceTest.readResponse(response);

		if (response.getStatusLine().getStatusCode() != 200) {
			System.out.println("status code: " + response.getStatusLine().getStatusCode());
			throw new Exception(result.toString());
		}

		log.info("result string: " + result.toString());
		Result tmp = gson.fromJson(result.toString(), Result.class);

		return tmp;
	}



}
