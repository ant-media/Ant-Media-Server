package io.antmedia.integration;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.message.BasicNameValuePair;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.quartz.spi.InstanceIdGenerator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.datastore.db.types.Endpoint;
import io.antmedia.rest.BroadcastRestService;
import io.antmedia.rest.BroadcastRestService.BroadcastStatistics;
import io.antmedia.rest.BroadcastRestService.LiveStatistics;
import io.antmedia.rest.BroadcastRestService.Result;
import io.antmedia.social.endpoint.VideoServiceEndpoint.DeviceAuthParameters;


public class RestServiceTest {


	private static final String ROOT_APP_URL = "http://localhost:5080/LiveApp";

	private static final String ROOT_SERVICE_URL = "http://localhost:5080/LiveApp/rest";
	private static Process tmpExec;
	private BroadcastRestService restService = null;

	public static final int MAC_OS_X = 0;
	public static final int LINUX = 1;
	public static final int WINDOWS = 2;
	private static int OS_TYPE;
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

	private static String ffmpegPath = "ffmpeg";
	Gson gson = new Gson();


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
	public void testBroadcastCreateFunctional() {
		createBroadcast("name");
	}

	public Broadcast createBroadcast(String name) {
		String url = ROOT_SERVICE_URL + "/broadcast/create";

		HttpClient client = HttpClients.custom()
				.setRedirectStrategy(new LaxRedirectStrategy())
				.build();
		Gson gson = new Gson();
		Broadcast broadcast = new Broadcast();
		if (name != null) {
			broadcast.setName(name);
		}

		try {

			HttpUriRequest post = RequestBuilder.post()
					.setUri(url)
					.setHeader(HttpHeaders.CONTENT_TYPE, "application/json")
					.setEntity(new StringEntity(gson.toJson(broadcast)))
					.build();

			HttpResponse response = client.execute(post);

			StringBuffer result = readResponse(response);

			if (response.getStatusLine().getStatusCode() != 200) {
				throw new Exception(result.toString());
			}
			System.out.println("result string: " + result.toString());
			Broadcast tmp = gson.fromJson(result.toString(), Broadcast.class);
			assertNotNull(tmp);
			assertEquals(tmp.getName(), broadcast.getName());
			return tmp;
		}
		catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		return null;
	}





	@Test
	public void testBroadcastCreateFunctionalWithoutName() {

		createBroadcast(null);

	}

	@Test
	public void testBroadcastCreateFunctionalWithoutObject() {

		String url = ROOT_SERVICE_URL + "/broadcast/create";

		HttpClient client = HttpClients.custom()
				.setRedirectStrategy(new LaxRedirectStrategy())
				.build();
		Gson gson = new Gson();
		Broadcast broadcast = null; //new Broadcast();
		//broadcast.name = "name";

		try {

			HttpUriRequest post = RequestBuilder.post()
					.setUri(url)
					.setHeader(HttpHeaders.CONTENT_TYPE, "application/json")
					.setEntity(new StringEntity(gson.toJson(broadcast)))
					.build();

			HttpResponse response = client.execute(post);

			StringBuffer result = readResponse(response);

			if (response.getStatusLine().getStatusCode() != 200) {
				throw new Exception(result.toString());
			}
			System.out.println("result string: " + result.toString());
			Broadcast tmp = gson.fromJson(result.toString(), Broadcast.class);
			assertNotNull(tmp);

		}
		catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}


	@Test
	public void testBroadcasGetUndefined() {
		try {
			/// get broadcast 
			String url = ROOT_SERVICE_URL + "/broadcast/get";

			CloseableHttpClient client = HttpClients.custom()
					.setRedirectStrategy(new LaxRedirectStrategy())
					.build();
			Gson gson = new Gson();
			//Broadcast broadcast = null; //new Broadcast();
			//broadcast.name = "name";



			HttpUriRequest get = RequestBuilder.get()
					.setUri(url)
					.setHeader(HttpHeaders.CONTENT_TYPE, "application/json")
					//	.setEntity(new StringEntity(gson.toJson(broadcast)))
					.build();

			CloseableHttpResponse response = client.execute(get);

			StringBuffer result = readResponse(response);

			if (response.getStatusLine().getStatusCode() != 200) {
				throw new Exception(result.toString());
			}
			System.out.println("result string: " + result.toString());
			Broadcast tmp2 = gson.fromJson(result.toString(), Broadcast.class);
			assertNotNull(tmp2);
			assertEquals(tmp2.getStreamId(), null);

		}
		catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testBroadcasGetUnknown() {
		Broadcast tmp2 = getBroadcast("dsfsfsfs");
		assertNotNull(tmp2);
		assertEquals(tmp2.getStreamId(), null);

	}

	public Broadcast getBroadcast(String streamId) {
		try {
			/// get broadcast 
			String url = ROOT_SERVICE_URL + "/broadcast/get";

			CloseableHttpClient client = HttpClients.custom()
					.setRedirectStrategy(new LaxRedirectStrategy())
					.build();
			Gson gson = new Gson();
			//Broadcast broadcast = null; //new Broadcast();
			//broadcast.name = "name";



			HttpUriRequest get = RequestBuilder.get()
					.setUri(url + "?id=" + streamId)
					.setHeader(HttpHeaders.CONTENT_TYPE, "application/json")
					//	.setEntity(new StringEntity(gson.toJson(broadcast)))
					.build();

			CloseableHttpResponse response = client.execute(get);

			StringBuffer result = readResponse(response);

			if (response.getStatusLine().getStatusCode() != 200) {
				throw new Exception(result.toString());
			}
			System.out.println("result string: " + result.toString());
			assertFalse(result.toString().contains("dbId"));
			Broadcast tmp2 = gson.fromJson(result.toString(), Broadcast.class);
			return tmp2;
		}
		catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		return null;
	}


	@Test
	public void testBroadcasGetFree() {
		try {
			/// get broadcast 
			String url = ROOT_SERVICE_URL + "/broadcast/get";

			CloseableHttpClient client = HttpClients.custom()
					.setRedirectStrategy(new LaxRedirectStrategy())
					.build();
			Gson gson = new Gson();
			//Broadcast broadcast = null; //new Broadcast();
			//broadcast.name = "name";



			HttpUriRequest get = RequestBuilder.get()
					.setUri(url + "?id=")
					.setHeader(HttpHeaders.CONTENT_TYPE, "application/json")
					//	.setEntity(new StringEntity(gson.toJson(broadcast)))
					.build();

			CloseableHttpResponse response = client.execute(get);

			StringBuffer result = readResponse(response);

			if (response.getStatusLine().getStatusCode() != 200) {
				throw new Exception(result.toString());
			}
			System.out.println("result string: " + result.toString());
			Broadcast tmp2 = gson.fromJson(result.toString(), Broadcast.class);
			assertNotNull(tmp2);
			assertEquals(tmp2.getStreamId(), null);

		}
		catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}


	public Broadcast callCreateBroadcast(int expireTimeMS) throws Exception {

		String url = ROOT_SERVICE_URL + "/broadcast/create";

		HttpClient client = HttpClients.custom()
				.setRedirectStrategy(new LaxRedirectStrategy())
				.build();
		Gson gson = new Gson();
		Broadcast broadcast = new Broadcast();
		broadcast.setExpireDurationMS(expireTimeMS);
		broadcast.setName("namesdfsf");



		HttpUriRequest post = RequestBuilder.post()
				.setUri(url)
				.setHeader(HttpHeaders.CONTENT_TYPE, "application/json")
				.setEntity(new StringEntity(gson.toJson(broadcast)))
				.build();

		HttpResponse response = client.execute(post);

		StringBuffer result = readResponse(response);

		if (response.getStatusLine().getStatusCode() != 200) {
			throw new Exception(result.toString());
		}
		System.out.println("result string: " + result.toString());
		Broadcast tmp = gson.fromJson(result.toString(), Broadcast.class);
		assertNotNull(tmp);
		assertNotSame(tmp.getDate(), 0L);

		return tmp;

	}
	
	
	public LiveStatistics callGetLiveStatistics() {
		try {

			String url = ROOT_SERVICE_URL + "/broadcast/getAppLiveStatistics";

			CloseableHttpClient client = HttpClients.custom()
					.setRedirectStrategy(new LaxRedirectStrategy())
					.build();
			//Gson gson = new Gson();
			//Broadcast broadcast = null; //new Broadcast();
			//broadcast.name = "name";



			HttpUriRequest get = RequestBuilder.get()
					.setUri(url)
					.setHeader(HttpHeaders.CONTENT_TYPE, "application/json")
					//	.setEntity(new StringEntity(gson.toJson(broadcast)))
					.build();

			CloseableHttpResponse response = client.execute(get);

			StringBuffer result = readResponse(response);

			if (response.getStatusLine().getStatusCode() != 200) {
				throw new Exception(result.toString());
			}
			System.out.println("result string: " + result.toString());
			

			return  gson.fromJson(result.toString(), LiveStatistics.class);

		}
		catch(Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	
	public BroadcastStatistics callGetBroadcastStatistics(String streamId) {
		try {

			String url = ROOT_SERVICE_URL + "/broadcast/getBroadcastLiveStatistics";

			CloseableHttpClient client = HttpClients.custom()
					.setRedirectStrategy(new LaxRedirectStrategy())
					.build();
			//Gson gson = new Gson();
			//Broadcast broadcast = null; //new Broadcast();
			//broadcast.name = "name";



			HttpUriRequest get = RequestBuilder.get()
					.setUri(url+"?id="+streamId)
					.setHeader(HttpHeaders.CONTENT_TYPE, "application/json")
					//	.setEntity(new StringEntity(gson.toJson(broadcast)))
					.build();

			CloseableHttpResponse response = client.execute(get);

			StringBuffer result = readResponse(response);

			if (response.getStatusLine().getStatusCode() != 200) {
				throw new Exception(result.toString());
			}
			System.out.println("result string: " + result.toString());
			

			return  gson.fromJson(result.toString(), BroadcastStatistics.class);

		}
		catch(Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		return null;
	}
	
	
	

	public List<Broadcast> callGetBroadcastList() {
		try {

			String url = ROOT_SERVICE_URL + "/broadcast/getList/0/50";

			CloseableHttpClient client = HttpClients.custom()
					.setRedirectStrategy(new LaxRedirectStrategy())
					.build();
			//Gson gson = new Gson();
			//Broadcast broadcast = null; //new Broadcast();
			//broadcast.name = "name";



			HttpUriRequest get = RequestBuilder.get()
					.setUri(url)
					.setHeader(HttpHeaders.CONTENT_TYPE, "application/json")
					//	.setEntity(new StringEntity(gson.toJson(broadcast)))
					.build();

			CloseableHttpResponse response = client.execute(get);

			StringBuffer result = readResponse(response);

			if (response.getStatusLine().getStatusCode() != 200) {
				throw new Exception(result.toString());
			}
			System.out.println("result string: " + result.toString());
			Type listType = new TypeToken<List<Broadcast>>(){}.getType();

			return  gson.fromJson(result.toString(), listType);

		}
		catch(Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public Broadcast callGetBroadcast(String streamId) throws Exception {
		String url = ROOT_SERVICE_URL + "/broadcast/get";

		CloseableHttpClient client = HttpClients.custom()
				.setRedirectStrategy(new LaxRedirectStrategy())
				.build();
		//Gson gson = new Gson();
		//Broadcast broadcast = null; //new Broadcast();
		//broadcast.name = "name";



		HttpUriRequest get = RequestBuilder.get()
				.setUri(url + "?id="+streamId)
				.setHeader(HttpHeaders.CONTENT_TYPE, "application/json")
				//	.setEntity(new StringEntity(gson.toJson(broadcast)))
				.build();

		CloseableHttpResponse response = client.execute(get);

		StringBuffer result = readResponse(response);

		if (response.getStatusLine().getStatusCode() != 200) {
			throw new Exception(result.toString());
		}
		System.out.println("result string: " + result.toString());

		return  gson.fromJson(result.toString(), Broadcast.class);
	}
	
	

	@Test
	public void testBroadcasGet() {
		try {

			Broadcast tmp = callCreateBroadcast(0);


			/// get broadcast 

			Broadcast tmp2 = callGetBroadcast(tmp.getStreamId());
			assertNotNull(tmp);
			assertEquals(tmp.getStreamId(), tmp2.getStreamId());
			assertEquals(tmp.getName(), tmp2.getName());

		}
		catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testBroadcastDeleteNULL() {

		Result result2 = deleteBroadcast(null);

		assertNotNull(result2);
		assertFalse(result2.success);
	}


	public boolean callStopBroadcastService(String streamId) throws Exception {
		String url = ROOT_SERVICE_URL + "/broadcast/stop";

		HttpClient client = HttpClients.custom()
				.setRedirectStrategy(new LaxRedirectStrategy())
				.build();
		Gson gson = new Gson();

		HttpUriRequest post = RequestBuilder.post()
				.setUri(url + "/" + streamId)
				.setHeader(HttpHeaders.CONTENT_TYPE, "application/json")
				.build();

		HttpResponse response = client.execute(post);

		StringBuffer result = readResponse(response);

		if (response.getStatusLine().getStatusCode() != 200) {
			throw new Exception(result.toString());
		}
		System.out.println("result string: " + result.toString());
		Result responseResult = gson.fromJson(result.toString(), Result.class);
		assertNotNull(responseResult);
		return responseResult.success;
	}

	@Test
	public void testExpireBroadcast() {

		try {
			Broadcast broadcast = callCreateBroadcast(1000);
			System.out.println("broadcast stream id: " + broadcast.getStreamId());

			Thread.sleep(2000);

			Process execute = execute(ffmpegPath + " -re -i src/test/resources/test.flv -acodec copy "
					+ "	-vcodec copy -f flv rtmp://localhost/LiveApp/" + broadcast.getStreamId());


			Thread.sleep(5000);

			broadcast = callGetBroadcast(broadcast.getStreamId());


			assertEquals(broadcast.getStatus(), "created");

			execute.destroy();


			broadcast = callCreateBroadcast(5000);
			System.out.println("broadcast stream id: " + broadcast.getStreamId());


			execute = execute(ffmpegPath + " -re -i src/test/resources/test.flv -acodec copy "
					+ "	-vcodec copy -f flv rtmp://localhost/LiveApp/" + broadcast.getStreamId());


			Thread.sleep(5000);

			broadcast = callGetBroadcast(broadcast.getStreamId());

			assertEquals(broadcast.getStatus(), "broadcasting");

			execute.destroy();

		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	@Test
	public void testStopBroadcast() {
		try {
			//call stop broadcast and check result is false

			assertFalse(callStopBroadcastService(String.valueOf((int)(Math.random()*1000))));


			//create stream
			Broadcast broadcast = callCreateBroadcast(0);

			assertFalse(callStopBroadcastService(broadcast.getStreamId()));

			Broadcast broadcastReturned = callGetBroadcast(broadcast.getStreamId());

			assertEquals(broadcastReturned.getStatus(), "created");

			//publish stream 
			Process execute = execute(ffmpegPath + " -re -i src/test/resources/test.flv -acodec copy "
					+ "	-vcodec copy -f flv rtmp://localhost/LiveApp/" + broadcast.getStreamId());

			Thread.sleep(5000);

			broadcastReturned = callGetBroadcast(broadcast.getStreamId());

			assertEquals(broadcastReturned.getStatus(), "broadcasting");

			// It should return true this time
			assertTrue(callStopBroadcastService(broadcast.getStreamId()));


			//It should return false again because it is already closed
			assertFalse(callStopBroadcastService(broadcast.getStreamId()));

			broadcastReturned = callGetBroadcast(broadcast.getStreamId());

			assertEquals(broadcastReturned.getStatus(), "finished");
		}
		catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}


	}


	@Test
	public void testDeleteVoDFile() {

		try {
			String streamName = "vod_delete_test";
			Broadcast broadcast = createBroadcast(streamName);
			assertNotNull(broadcast);
			assertNotNull(broadcast.getStreamId());

			Process execute = execute(ffmpegPath + " -re -i src/test/resources/test.flv -acodec copy "
					+ "	-vcodec copy -f flv rtmp://localhost/LiveApp/" + broadcast.getStreamId());


			Thread.sleep(20000);

			execute.destroy();

			//let mp4 file to be created
			Thread.sleep(2000);

			boolean vodExists = AppFunctionalTest.exists(ROOT_APP_URL + "/streams/" + broadcast.getStreamId() + ".mp4", false);

			assertTrue(vodExists);


			String url = ROOT_SERVICE_URL + "/broadcast/deleteVoDFile/"+ broadcast.getStreamId();

			String response = makePOSTRequest(url, null);

			Result deleteVoDResponse = gson.fromJson(response.toString(), Result.class);
			assertTrue(deleteVoDResponse.success);


			try {
				Process process = Runtime.getRuntime().exec("./src/test/resources/scripts/check_file_deleted.sh " + broadcast.getStreamId());
				InputStream errorStream = process.getInputStream();
				byte[] data = new byte[1024];
				int length = 0;
				String errorStr = null;

				try {
					while ((length = errorStream.read(data, 0, data.length)) > 0) {
						errorStr  = new String(data, 0, length);
						System.out.println("errorstr:" + errorStr);
					}
				}
				catch (Exception e) {
					e.printStackTrace();
				}

				assertTrue(errorStr == null || errorStr.length() == 0);


			} catch (IOException e) {
				e.printStackTrace();
				fail(e.getMessage());
			}

		} catch (InterruptedException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}


	public String makePOSTRequest(String url, String entity) {
		try {
			CloseableHttpClient client = HttpClients.custom()
					.setRedirectStrategy(new LaxRedirectStrategy())
					.build();


			RequestBuilder builder = RequestBuilder.post()
					.setUri(url)
					.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");

			if (entity != null) {
				builder.setEntity(new StringEntity(entity));
			}

			HttpUriRequest post = builder.build();
			CloseableHttpResponse response = client.execute(post);

			StringBuffer result = readResponse(response);

			if (response.getStatusLine().getStatusCode() != 200) {
				throw new Exception(result.toString());
			}
			return result.toString();
		}
		catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		return null;
	}


	public Result deleteBroadcast(String id) {
		try {
			//delete broadcast
			String url = ROOT_SERVICE_URL + "/broadcast/delete/"+ id;

			CloseableHttpClient client = HttpClients.custom()
					.setRedirectStrategy(new LaxRedirectStrategy())
					.build();


			HttpUriRequest post = RequestBuilder.post()
					.setUri(url)
					.setHeader(HttpHeaders.CONTENT_TYPE, "application/json")
					.build();

			CloseableHttpResponse response = client.execute(post);

			StringBuffer result = readResponse(response);

			if (response.getStatusLine().getStatusCode() != 200) {
				throw new Exception(result.toString());
			}
			System.out.println("result string: " + result.toString());
			Result result2 = gson.fromJson(result.toString(), Result.class);
			return result2;
		}
		catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		return null;
	}


	public Result updateNameAndDescription(String broadcastId, String name,	String description ) throws Exception {
		CloseableHttpClient httpclient = HttpClients.createDefault();


		HttpPost httppost = new HttpPost(ROOT_SERVICE_URL + "/broadcast/updateInfo");
		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
		nameValuePairs.add(new BasicNameValuePair("Content-Type","application/x-www-form-urlencoded;"));
		nameValuePairs.add(new BasicNameValuePair("id", broadcastId));
		nameValuePairs.add(new BasicNameValuePair("name", name));
		nameValuePairs.add(new BasicNameValuePair("description", description));
		httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs, StandardCharsets.UTF_8));


		CloseableHttpResponse response = httpclient.execute(httppost);

		StringBuffer result = readResponse(response);

		if (response.getStatusLine().getStatusCode() != 200) {
			throw new Exception(result.toString());
		}
		Gson gson = new Gson();
		System.out.println("result string: " + result.toString());
		Result tmp = gson.fromJson(result.toString(), Result.class);

		return tmp;
	}

	public Result updatePublish(String broadcastId, boolean publish) throws Exception {
		CloseableHttpClient httpclient = HttpClients.createDefault();


		HttpPost httppost = new HttpPost(ROOT_SERVICE_URL + "/broadcast/updatePublishStatus");
		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
		nameValuePairs.add(new BasicNameValuePair("Content-Type","application/x-www-form-urlencoded;"));
		nameValuePairs.add(new BasicNameValuePair("id", broadcastId));
		nameValuePairs.add(new BasicNameValuePair("publish", String.valueOf(publish)));
		httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs, StandardCharsets.UTF_8));


		CloseableHttpResponse response = httpclient.execute(httppost);

		StringBuffer result = readResponse(response);

		if (response.getStatusLine().getStatusCode() != 200) {
			throw new Exception(result.toString());
		}
		Gson gson = new Gson();
		System.out.println("result string: " + result.toString());
		Result tmp = gson.fromJson(result.toString(), Result.class);
		return tmp;
	}


	@Test
	public void testUpdate() {

		//create broadcast
		Broadcast broadcast = createBroadcast(null);

		String name = "string name";
		String description = "String descriptio";
		//update name and description
		try {
			Result result = updateNameAndDescription(broadcast.getStreamId().toString(), name, description);
			assertTrue(result.success);

			//get broadcast
			broadcast = getBroadcast(broadcast.getStreamId().toString());

			//check name and description
			assertEquals(broadcast.getName(), name);
			assertEquals(broadcast.getDescription(), description);
			assertTrue(broadcast.isPublish());

			//update publish info
			boolean publish = false;
			result = updatePublish(broadcast.getStreamId().toString(), publish);
			assertTrue(result.success);

			//get broacdast
			broadcast = getBroadcast(broadcast.getStreamId().toString());

			//check publish info
			assertEquals(broadcast.getName(), name);
			assertEquals(broadcast.getDescription(), description);
			assertEquals(broadcast.isPublish(), publish);

		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	public Result addEndpoint(String broadcastId, String rtmpUrl) throws Exception 
	{
		CloseableHttpClient httpclient = HttpClients.createDefault();


		HttpPost httppost = new HttpPost(ROOT_SERVICE_URL + "/broadcast/addEndpoint");
		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
		nameValuePairs.add(new BasicNameValuePair("Content-Type","application/x-www-form-urlencoded;"));
		nameValuePairs.add(new BasicNameValuePair("id", broadcastId));
		nameValuePairs.add(new BasicNameValuePair("rtmpUrl", rtmpUrl));
		httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs, StandardCharsets.UTF_8));


		CloseableHttpResponse response = httpclient.execute(httppost);

		StringBuffer result = readResponse(response);

		if (response.getStatusLine().getStatusCode() != 200) {
			throw new Exception(result.toString());
		}
		Gson gson = new Gson();
		System.out.println("result string: " + result.toString());
		Result tmp = gson.fromJson(result.toString(), Result.class);

		return tmp;
	}

	public Result addSocialEndpoint(String broadcastId, String name) throws Exception {
		CloseableHttpClient httpclient = HttpClients.createDefault();


		HttpPost httppost = new HttpPost(ROOT_SERVICE_URL + "/broadcast/addSocialEndpoint");
		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
		nameValuePairs.add(new BasicNameValuePair("Content-Type","application/x-www-form-urlencoded;"));
		nameValuePairs.add(new BasicNameValuePair("id", broadcastId));
		nameValuePairs.add(new BasicNameValuePair("serviceName", name));
		httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs, StandardCharsets.UTF_8));


		CloseableHttpResponse response = httpclient.execute(httppost);

		StringBuffer result = readResponse(response);

		if (response.getStatusLine().getStatusCode() != 200) {
			throw new Exception(result.toString());
		}
		Gson gson = new Gson();
		System.out.println("result string: " + result.toString());
		Result tmp = gson.fromJson(result.toString(), Result.class);

		return tmp;
	}

	private DeviceAuthParameters getDeviceAuthParameters(String serviceName) throws Exception {
		CloseableHttpClient httpclient = HttpClients.createDefault();


		HttpPost httppost = new HttpPost(ROOT_SERVICE_URL + "/broadcast/getDeviceAuthParameters/" + serviceName);
		//	List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
		//	nameValuePairs.add(new BasicNameValuePair("Content-Type","application/x-www-form-urlencoded;"));
		//	nameValuePairs.add(new BasicNameValuePair("serviceName", serviceName));
		//	httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs, StandardCharsets.UTF_8));


		CloseableHttpResponse response = httpclient.execute(httppost);

		StringBuffer result = readResponse(response);

		if (response.getStatusLine().getStatusCode() != 200) {
			throw new Exception(result.toString());
		}


		Gson gson = new Gson();
		System.out.println("result string: " + result.toString());
		DeviceAuthParameters tmp = gson.fromJson(result.toString(), DeviceAuthParameters.class);

		return tmp;

	}

	private Result checkDeviceAuthStatus(String serviceName) throws Exception {
		CloseableHttpClient httpclient = HttpClients.createDefault();


		HttpPost httppost = new HttpPost(ROOT_SERVICE_URL + "/broadcast/checkDeviceAuthStatus/" + serviceName);
		//	List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
		//	nameValuePairs.add(new BasicNameValuePair("Content-Type","application/x-www-form-urlencoded;"));
		//	nameValuePairs.add(new BasicNameValuePair("serviceName", serviceName));
		//	httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs, StandardCharsets.UTF_8));


		CloseableHttpResponse response = httpclient.execute(httppost);

		StringBuffer result = readResponse(response);

		if (response.getStatusLine().getStatusCode() != 200) {
			throw new Exception(result.toString());
		}
		Gson gson = new Gson();
		System.out.println("result string: " + result.toString());
		Result tmp = gson.fromJson(result.toString(), Result.class);

		return tmp;

	}

	//TODO: restart server after testAddEndpoint and 
	// check that social endpoints are added correctly 

	@Test
	public void testJSONIgnore() {
		Broadcast broadcast = new Broadcast();
		broadcast.setName("name");

		try {
			String serializedStr = new ObjectMapper().writeValueAsString(broadcast);
			//check that this field exist
			assertNull(broadcast.getDbId());
			System.out.println("json result: " + serializedStr);
			assertFalse(serializedStr.toString().contains("dbId"));
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
	}


	@Test
	public void testCheckSocialEndpointRecreated() {
		Result result;
		try {
			//create broadcast
			Broadcast broadcast = createBroadcast("social_endpoint_check");
			//add facebook endpoint

			/*

			in enterprise edition
			result = addSocialEndpoint(broadcast.getStreamId().toString(), "facebook");

			//check that it is successfull
			assertTrue(result.success);
			 */

			/*
			 * in enterprise edition
			//add youtube endpoint
			result = addSocialEndpoint(broadcast.getStreamId().toString(), "youtube");

			//check that it is succes full
			assertTrue(result.success);
			 */


			//add twitter endpoint
			result = addSocialEndpoint(broadcast.getStreamId().toString(), "periscope");

			//check that it is succes full
			assertTrue(result.success);

			//get endpoint list
			broadcast = getBroadcast(broadcast.getStreamId().toString());

			//check that 1 element exist
			assertNotNull(broadcast.getEndPointList());
			assertEquals(broadcast.getEndPointList().size(), 1);



			broadcast = getBroadcast(broadcast.getStreamId().toString());
			List<Endpoint> endpointList = broadcast.getEndPointList();

			for (Endpoint endpoint : endpointList) {
				System.out.println("endpoint url: " + endpoint.rtmpUrl + " broadcast.id=" + endpoint.broadcastId + " stream id: " + endpoint.streamId);
			}

			Process execute = execute(ffmpegPath + " -re -i src/test/resources/test.flv -codec copy -f flv rtmp://localhost/LiveApp/" + broadcast.getStreamId());

			Thread.sleep(20000);

			execute.destroy();

			//this value is critical because server creates endpoints on social networks
			Thread.sleep(15000);

			broadcast = getBroadcast(broadcast.getStreamId().toString());
			List<Endpoint> endpointList2 = broadcast.getEndPointList();
			assertEquals(endpointList2.size(), 1);

			for (Endpoint endpoint : endpointList2) {
				System.out.println("new endpoint url: " + endpoint.rtmpUrl + " broadcast.id=" + endpoint.broadcastId + " stream id: " + endpoint.streamId);

			}

			for (Endpoint endpoint : endpointList2) {
				for (Endpoint endpointFirst : endpointList) {
					assertTrue(!endpoint.rtmpUrl.equals(endpointFirst.rtmpUrl) || !endpoint.broadcastId.equals(endpointFirst.broadcastId));
				}
			}


		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
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

					InputStream inputStream = tmpExec.getInputStream();

					while ((length = inputStream.read(data, 0, data.length)) > 0) {
						System.out.println(new String(data, 0, length));
					}
					System.out.println("Leaving thread");


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


	//@Test
	public void authenticateSocialEndpoints() {
		Result result;
		try {
			//authenticate facebook
			//get parameters
			DeviceAuthParameters deviceAuthParameters = getDeviceAuthParameters("facebook");
			System.out.println(" url: " + deviceAuthParameters.verification_url );
			System.out.println(" user code: " + deviceAuthParameters.user_code );
			assertNotNull(deviceAuthParameters.verification_url);
			assertNotNull(deviceAuthParameters.user_code);

			//ask if authenticated
			do {
				System.out.println("You should enter this code: " + deviceAuthParameters.user_code +
						" to this url: " + deviceAuthParameters.verification_url);
				System.out.println("Waiting before asking auth status");

				Thread.sleep(deviceAuthParameters.interval * 1000);
				result = checkDeviceAuthStatus("facebook");
				System.out.println("auth status is " + result.success);

			} while (!result.success);


			assertTrue(result.success);



			//authenticate twitter
			deviceAuthParameters = getDeviceAuthParameters("periscope");
			System.out.println(" url: " + deviceAuthParameters.verification_url );
			System.out.println(" user code: " + deviceAuthParameters.user_code );
			assertNotNull(deviceAuthParameters.verification_url);
			assertNotNull(deviceAuthParameters.user_code);

			do {
				System.out.println("You should enter this code: " + deviceAuthParameters.user_code +
						" to this url: " + deviceAuthParameters.verification_url);
				System.out.println("Waiting "+ deviceAuthParameters.interval + " seconds before asking auth status");

				Thread.sleep(deviceAuthParameters.interval * 1000);
				result = checkDeviceAuthStatus("periscope");
				System.out.println("auth status is " + result.success);

			} while (!result.success);

			assertTrue(result.success);

			//authenticate youtube
			deviceAuthParameters = getDeviceAuthParameters("youtube");
			System.out.println(" url: " + deviceAuthParameters.verification_url );
			System.out.println(" user code: " + deviceAuthParameters.user_code );
			assertNotNull(deviceAuthParameters.verification_url);
			assertNotNull(deviceAuthParameters.user_code);

			do {
				System.out.println("You should enter this code: " + deviceAuthParameters.user_code +
						" to this url: " + deviceAuthParameters.verification_url);
				System.out.println("Waiting "+ deviceAuthParameters.interval + " seconds before asking auth status");

				Thread.sleep(deviceAuthParameters.interval * 1000);
				result = checkDeviceAuthStatus("youtube");
				System.out.println("auth status is " + result.success);

			} while (!result.success);


		}
		catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	@Test
	public void testAddEndpoint() {

		try {
			/*
			//create broadcast


			System.out.println("broadcast id string: " + broadcast.getStreamId().toString());

			//get broadcast
			broadcast = getBroadcast(broadcast.getStreamId().toString());

			//checek there no endpoint
			assertNull(broadcast.getEndPointList());

			//add facebook end point
			Result result = addSocialEndpoint(broadcast.getStreamId().toString(), "facebook");

			//check that error returns because it is not authenticated
			assertFalse(result.success);
			assertNotNull(result.message);

			//add twitter end point
			result = addSocialEndpoint(broadcast.getStreamId().toString(), "twitter");

			//check that error returns
			assertFalse(result.success);
			assertNotNull(result.message);

			//add youtube end point
			result = addSocialEndpoint(broadcast.getStreamId().toString(), "youtube");

			//check that error returns
			assertFalse(result.success);
			assertNotNull(result.message);
			 */

			/*


			updateNameAndDescription(broadcast.getStreamId().toString(), "name", "description");

			//add facebook endpoint
			Result result = addSocialEndpoint(broadcast.getStreamId().toString(), "facebook");

			//check that it is successfull
			assertTrue(result.success);

			 */

			Broadcast broadcast = createBroadcast(null);

			//add twitter endpoint
			Result result = addSocialEndpoint(broadcast.getStreamId().toString(), "periscope");

			//check that it is succes full
			assertTrue(result.success);

			/*
			//add youtube endpoint
			result = addSocialEndpoint(broadcast.getStreamId().toString(), "youtube");

			//check that it is succes full
			assertTrue(result.success);

			 */

			//add generic endpoint
			result = addEndpoint(broadcast.getStreamId().toString(), "rtmp://dfjdksafjlaskfjalkfj");

			//check that it is successfull
			assertTrue(result.success);

			//get endpoint list
			broadcast = getBroadcast(broadcast.getStreamId().toString());

			//check that 4 element exist
			assertNotNull(broadcast.getEndPointList());
			assertEquals(broadcast.getEndPointList().size(), 2);

		}
		catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}


	@Test
	public void testBroadcastDelete() {
		try {

			String url = ROOT_SERVICE_URL + "/broadcast/create";

			HttpClient client = HttpClients.custom()
					.setRedirectStrategy(new LaxRedirectStrategy())
					.build();
			Gson gson = new Gson();
			Broadcast broadcast = new Broadcast();
			broadcast.setName("namesdfsf");



			HttpUriRequest post = RequestBuilder.post()
					.setUri(url)
					.setHeader(HttpHeaders.CONTENT_TYPE, "application/json")
					.setEntity(new StringEntity(gson.toJson(broadcast)))
					.build();

			HttpResponse response = client.execute(post);

			StringBuffer result = readResponse(response);

			if (response.getStatusLine().getStatusCode() != 200) {
				throw new Exception(result.toString());
			}
			System.out.println("result string: " + result.toString());
			Broadcast tmp = gson.fromJson(result.toString(), Broadcast.class);
			assertNotNull(tmp);
			assertNotSame(tmp.getDate(), 0L);



			/// get broadcast 
			url = ROOT_SERVICE_URL + "/broadcast/get";

			client = HttpClients.custom()
					.setRedirectStrategy(new LaxRedirectStrategy())
					.build();
			//Gson gson = new Gson();
			//Broadcast broadcast = null; //new Broadcast();
			//broadcast.name = "name";



			HttpUriRequest get = RequestBuilder.get()
					.setUri(url + "?id="+tmp.getStreamId())
					.setHeader(HttpHeaders.CONTENT_TYPE, "application/json")
					//	.setEntity(new StringEntity(gson.toJson(broadcast)))
					.build();

			response = client.execute(get);

			result = readResponse(response);

			if (response.getStatusLine().getStatusCode() != 200) {
				throw new Exception(result.toString());
			}
			System.out.println("result string: " + result.toString());
			Broadcast tmp2 = gson.fromJson(result.toString(), Broadcast.class);
			assertNotNull(tmp);
			assertEquals(tmp.getStreamId(), tmp2.getStreamId());
			assertEquals(tmp.getName(), tmp2.getName());


			//delete broadcast
			url = ROOT_SERVICE_URL + "/broadcast/delete/"+ tmp2.getStreamId().toString();

			client = HttpClients.custom()
					.setRedirectStrategy(new LaxRedirectStrategy())
					.build();


			post = RequestBuilder.post()
					.setUri(url)
					.setHeader(HttpHeaders.CONTENT_TYPE, "application/json")
					//.setEntity(new StringEntity(gson.toJson(tmp2)))
					.build();

			response = client.execute(post);

			result = readResponse(response);

			if (response.getStatusLine().getStatusCode() != 200) {
				throw new Exception(result.toString());
			}
			System.out.println("result string: " + result.toString());
			Result result2 = gson.fromJson(result.toString(), Result.class);
			assertNotNull(result2);
			assertTrue(result2.success);


			//get the same object
			/// get broadcast 
			url = ROOT_SERVICE_URL + "/broadcast/get";

			client = HttpClients.custom()
					.setRedirectStrategy(new LaxRedirectStrategy())
					.build();
			//Gson gson = new Gson();
			//Broadcast broadcast = null; //new Broadcast();
			//broadcast.name = "name";

			get = RequestBuilder.get()
					.setUri(url + "?id="+tmp.getStreamId())
					.setHeader(HttpHeaders.CONTENT_TYPE, "application/json")
					//	.setEntity(new StringEntity(gson.toJson(broadcast)))
					.build();

			response = client.execute(get);

			result = readResponse(response);

			if (response.getStatusLine().getStatusCode() != 200) {
				throw new Exception(result.toString());
			}
			System.out.println("result string: " + result.toString());
			tmp2 = gson.fromJson(result.toString(), Broadcast.class);
			assertNotNull(tmp2);
			assertEquals(tmp2.getStreamId(), null);
			assertEquals(tmp2.getName(), null);

		}
		catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}




	protected StringBuffer readResponse(HttpResponse response) throws IOException {
		BufferedReader rd = new BufferedReader(
				new InputStreamReader(response.getEntity().getContent()));

		StringBuffer result = new StringBuffer();
		String line = "";
		while ((line = rd.readLine()) != null) {
			result.append(line);
		}
		return result;
	}

}
