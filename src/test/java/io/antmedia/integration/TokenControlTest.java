package io.antmedia.integration;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

import static org.bytedeco.javacpp.avformat.av_register_all;
import static org.bytedeco.javacpp.avformat.avformat_network_init;

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

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.datastore.db.types.Token;
import io.antmedia.rest.model.AppSettingsModel;
import io.antmedia.rest.model.Result;
import io.antmedia.rest.model.User;

public class TokenControlTest {

	private static String ROOT_SERVICE_URL;
	private static final String SERVICE_URL = "http://localhost:5080/LiveApp/rest";

	private static String ffmpegPath = "ffmpeg";
	private static BasicCookieStore httpCookieStore;
	private static final String SERVER_ADDR = "127.0.0.1"; 
	private static Process tmpExec;
	private static final Logger log = LoggerFactory.getLogger(TokenControlTest.class);


	private static String TEST_USER_EMAIL = "test@antmedia.io";
	private static String TEST_USER_PASS = "testtest";

	private static Gson gson = new Gson();

	static {

		try {
			ROOT_SERVICE_URL = "http://" + InetAddress.getLocalHost().getHostAddress() + ":5080/ConsoleApp/rest";
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}

		log.info("ROOT SERVICE URL: " + ROOT_SERVICE_URL);

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

		httpCookieStore = new BasicCookieStore();
	}

	@After
	public void teardown() {
		httpCookieStore = null;

	}

	@Test
	public void testTokenControl() {

		try {
			// authenticate user
			User user = new User();
			user.setEmail(TEST_USER_EMAIL);
			user.setPassword(TEST_USER_PASS);
			Result authenticatedUserResult = callAuthenticateUser(user);
			assertTrue(authenticatedUserResult.isSuccess());

			// get settings from the app
			AppSettingsModel appSettings = callGetAppSettings("LiveApp");

			appSettings.setTokenControlEnabled(true);
			appSettings.setMp4MuxingEnabled(true);


			Result result = callSetAppSettings("LiveApp", appSettings);
			assertTrue(result.isSuccess());

			appSettings = callGetAppSettings("LiveApp");
			assertTrue(appSettings.isTokenControlEnabled());

			Broadcast broadcast = RestServiceTest.callCreateRegularBroadcast();
			Token accessToken = callGetToken(broadcast.getStreamId());
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
			Token publishToken = callGetToken(broadcast.getStreamId());
			assertNotNull(publishToken);

			//create token for playing/accessing file
			Token accessToken2 = callGetToken(broadcast.getStreamId());
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
			appSettings.setMp4MuxingEnabled(false);


			Result flag = callSetAppSettings("LiveApp", appSettings);
			assertTrue(flag.isSuccess());
			
			

		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

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


	public static Token callGetToken(String streamId) throws Exception {
		String url = SERVICE_URL + "/broadcast/getToken";

		CloseableHttpClient client = HttpClients.custom().setRedirectStrategy(new LaxRedirectStrategy()).build();

		HttpUriRequest get = RequestBuilder.get().setUri(url + "?id=" + streamId + "&expireDate=15784343")
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

}
