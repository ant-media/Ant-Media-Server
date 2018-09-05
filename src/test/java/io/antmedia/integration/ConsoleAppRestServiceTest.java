package io.antmedia.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.google.gson.Gson;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.EncoderSettings;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.rest.BroadcastRestService;
import io.antmedia.rest.model.AppSettingsModel;
import io.antmedia.rest.model.Result;
import io.antmedia.rest.model.User;
import io.antmedia.test.Application;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ConsoleAppRestServiceTest {

	private static String ROOT_SERVICE_URL;

	private static String ffmpegPath = "ffmpeg";

	private static String TEST_USER_EMAIL = "test@antmedia.io";
	private static String TEST_USER_PASS = "testtest";

	private static BasicCookieStore httpCookieStore;

	static {

		try {
			ROOT_SERVICE_URL = "http://" + InetAddress.getLocalHost().getHostAddress() + ":5080/ConsoleApp/rest";
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}

		System.out.println("ROOT SERVICE URL: " + ROOT_SERVICE_URL);

	}

	@BeforeClass
	public static void beforeClass() {
		if (AppFunctionalTest.getOS() == AppFunctionalTest.MAC_OS_X) {
			ffmpegPath = "/usr/local/bin/ffmpeg";
		}
	}

	@Before
	public void before() {
		httpCookieStore = new BasicCookieStore();
	}

	@After
	public void teardown() {
		httpCookieStore = null;

	}

	/**
	 * This test should run first
	 */
	@Test
	public void testACreateInitialUser() {
		try {
			// create user for the first login
			Result firstLogin = callisFirstLogin();
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

	@Test
	public void testGetAppSettings() {
		try {

			User user = new User();
			user.setEmail(TEST_USER_EMAIL);
			user.setPassword(TEST_USER_PASS);
			Result authenticatedUserResult = callAuthenticateUser(user);
			assertTrue(authenticatedUserResult.isSuccess());

			// get LiveApp default settings and check the default values
			// get settings from the app
			Result result = callIsEnterpriseEdition();
			String appName = "WebRTCApp";
			if (result.isSuccess()) {
				appName = "WebRTCAppEE";
			}
			
			AppSettingsModel appSettingsModel = callGetAppSettings(appName);
			assertEquals(null, appSettingsModel.getVodFolder());
			 
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

	/**
	 * This may be a bug, just check it out, it is working as expectes
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
			assertEquals(broadcastCreated.getStatus(), Application.BROADCAST_STATUS_CREATED);

			AppFunctionalTest.executeProcess(ffmpegPath
					+ " -re -i src/test/resources/test.flv -acodec copy -vcodec copy -f flv rtmp://localhost/LiveApp/"
					+ broadcastCreated.getStreamId());

			Thread.sleep(5000);

			// check stream status is broadcasting
			Broadcast broadcast = RestServiceTest.callGetBroadcast(broadcastCreated.getStreamId());

			assertEquals(broadcast.getStatus(), AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING);

			// stop stream
			AppFunctionalTest.destroyProcess();

			Thread.sleep(3000);

			// check stream status is finished
			broadcast = RestServiceTest.callGetBroadcast(broadcastCreated.getStreamId());

			assertEquals(broadcast.getStatus(), AntMediaApplicationAdapter.BROADCAST_STATUS_FINISHED);

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
	
	private boolean checkURLExist(String url) throws Exception {
		
		HttpClient client = HttpClients.custom().setRedirectStrategy(new LaxRedirectStrategy())
				.setDefaultCookieStore(httpCookieStore).build();
		Gson gson = new Gson();

		HttpUriRequest post = RequestBuilder.get().setUri(url).build();

		HttpResponse response = client.execute(post);

		StringBuffer result = RestServiceTest.readResponse(response);
		if (response.getStatusLine().getStatusCode() == 200) {
			return true;
		}
		return false;
	}

	private Result callisFirstLogin() throws Exception {
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
		System.out.println("result string: " + result.toString());
		Result tmp = gson.fromJson(result.toString(), Result.class);
		assertNotNull(tmp);
		return tmp;

	}

	private Result callCreateInitialUser(User user) throws Exception {

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
		System.out.println("result string: " + result.toString());
		Result tmp = gson.fromJson(result.toString(), Result.class);
		assertNotNull(tmp);
		return tmp;
	}

	private Result callAuthenticateUser(User user) throws Exception {
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
		System.out.println("result string: " + result.toString());
		Result tmp = gson.fromJson(result.toString(), Result.class);
		assertNotNull(tmp);
		return tmp;
	}

	private Result callSetAppSettings(String appName, AppSettingsModel appSettingsModel) throws Exception {
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
		System.out.println("result string: " + result.toString());
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
		System.out.println("result string: " + result.toString());
		Result tmp = gson.fromJson(result.toString(), Result.class);
		assertNotNull(tmp);
		return tmp;

	}

	public AppSettingsModel callGetAppSettings(String appName) throws Exception {

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
		System.out.println("result string: " + result.toString());
		AppSettingsModel tmp = gson.fromJson(result.toString(), AppSettingsModel.class);
		assertNotNull(tmp);
		return tmp;
	}
}
