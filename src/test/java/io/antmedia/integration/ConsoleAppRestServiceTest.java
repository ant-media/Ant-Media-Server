package io.antmedia.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
import io.antmedia.rest.model.AppSettingsModel;
import io.antmedia.rest.model.Result;
import io.antmedia.rest.model.User;
import io.antmedia.test.Application;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ConsoleAppRestServiceTest {

	private static String ROOT_SERVICE_URL;

	private static String ffmpegPath = "ffmpeg";

	private static String TEST_USER_EMAIL = "ci@antmedia.io";
	private static String TEST_USER_PASS = "ci@ant";

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

			user.email = "any_email";
			authenticatedUserResult = callAuthenticateUser(user);
			assertFalse(authenticatedUserResult.isSuccess());

			user.email = TEST_USER_EMAIL;
			user.password = TEST_USER_PASS;
			Result createInitialUser = callCreateInitialUser(user);
			assertTrue(createInitialUser.isSuccess());

			// authenticate the user
			authenticatedUserResult = callAuthenticateUser(user);
			assertTrue(authenticatedUserResult.isSuccess());

			user.email = TEST_USER_EMAIL;
			user.password = "any_pass";
			authenticatedUserResult = callAuthenticateUser(user);
			assertFalse(authenticatedUserResult.isSuccess());

			// try to create user that is being used in first step
			// but this time it should fail
			firstLogin = callisFirstLogin();
			assertFalse(firstLogin.isSuccess());

			user.email = "any_email";
			user.password = "any_pass";
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
			user.email = TEST_USER_EMAIL;
			user.password = TEST_USER_PASS;
			Result authenticatedUserResult = callAuthenticateUser(user);
			assertTrue(authenticatedUserResult.isSuccess());

			// get LiveApp default settings and check the default values
			// get settings from the app

			// change app settings

			// get app settings and assert settings has changed

			// check the related file to make sure settings changed for restart

			// return back to default values
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
			user.email = TEST_USER_EMAIL;
			user.password = TEST_USER_PASS;
			Result authenticatedUserResult = callAuthenticateUser(user);
			assertTrue(authenticatedUserResult.isSuccess());

			AppSettingsModel appSettingsModel = callGetAppSettings("LiveApp");

			// Change app settings and make mp4 and hls muxing false
			appSettingsModel.mp4MuxingEnabled = false;
			appSettingsModel.hlsMuxingEnabled = false;
			List<EncoderSettings> encoderSettings = new ArrayList<EncoderSettings>();
			for (int i = 0; i < appSettingsModel.encoderSettings.size(); i++) {
				encoderSettings.add(appSettingsModel.encoderSettings.get(i));
			}
			appSettingsModel.encoderSettings.clear();

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
			appSettingsModel.mp4MuxingEnabled = true;
			appSettingsModel.hlsMuxingEnabled = true;
			appSettingsModel.encoderSettings = encoderSettings;

			result = callSetAppSettings("LiveApp", appSettingsModel);
			assertTrue(result.isSuccess());

			AppSettingsModel callGetAppSettings = callGetAppSettings("LiveApp");
			assertTrue(callGetAppSettings.encoderSettings.size() > 0);

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
			user.email = TEST_USER_EMAIL;
			user.password = TEST_USER_PASS;
			Result authenticatedUserResult = callAuthenticateUser(user);
			assertTrue(authenticatedUserResult.isSuccess());

			// get settings from the app
			AppSettingsModel appSettingsModel = callGetAppSettings("LiveApp");

			// assertFalse(appSettingsModel.acceptOnlyStreamsInDataStore);

			// change settings test testAllowOnlyStreamsInDataStore is true
			appSettingsModel.acceptOnlyStreamsInDataStore = true;

			Result result = callSetAppSettings("LiveApp", appSettingsModel);
			assertTrue(result.isSuccess());

			// check app settings
			appSettingsModel = callGetAppSettings("LiveApp");
			assertTrue(appSettingsModel.acceptOnlyStreamsInDataStore);

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
			appSettingsModel.acceptOnlyStreamsInDataStore = false;
			result = callSetAppSettings("LiveApp", appSettingsModel);
			assertTrue(result.isSuccess());

			AppSettingsModel callGetAppSettings = callGetAppSettings("LiveApp");
			assertFalse(appSettingsModel.acceptOnlyStreamsInDataStore);

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
				appSettingsModel.acceptOnlyStreamsInDataStore = true;
				result = callSetAppSettings("LiveApp", appSettingsModel);
				assertTrue(result.isSuccess());

				callGetAppSettings = callGetAppSettings("LiveApp");
				assertTrue(appSettingsModel.acceptOnlyStreamsInDataStore);
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
				appSettingsModel.acceptOnlyStreamsInDataStore = false;
				result = callSetAppSettings("LiveApp", appSettingsModel);
				assertTrue(result.isSuccess());

				callGetAppSettings = callGetAppSettings("LiveApp");
				assertFalse(appSettingsModel.acceptOnlyStreamsInDataStore);
			}

		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

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
