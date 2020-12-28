package io.antmedia.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;

import io.antmedia.AppSettings;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.message.BasicNameValuePair;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.awaitility.Awaitility;
import org.bytedeco.ffmpeg.global.avformat;
import org.bytedeco.ffmpeg.global.avutil;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.datastore.db.types.Endpoint;
import io.antmedia.datastore.db.types.SocialEndpointCredentials;
import io.antmedia.datastore.db.types.VoD;
import io.antmedia.rest.BroadcastRestService.SimpleStat;
import io.antmedia.rest.RestServiceBase.BroadcastStatistics;
import io.antmedia.rest.model.Result;
import io.antmedia.rest.model.Version;
import io.antmedia.social.endpoint.VideoServiceEndpoint.DeviceAuthParameters;

public class RestServiceV2Test {

	private static final String ROOT_APP_URL = "http://localhost:5080/LiveApp";

	private static final String ROOT_SERVICE_URL = "http://localhost:5080/LiveApp/rest";
	private static final String SERVER_ADDR = "127.0.0.1";
	private static Process tmpExec;
	protected static Logger logger = LoggerFactory.getLogger(RestServiceV2Test.class);
	public AntMediaApplicationAdapter app = null;

	@Context
	private ServletContext servletContext;

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
	private static Gson gson = new Gson();

	@Rule
	public TestRule watcher = new TestWatcher() {
		protected void starting(Description description) {
			System.out.println("Starting test: " + description.getMethodName());
		}

		protected void failed(Throwable e, Description description) {
			System.out.println("Failed test: " + description.getMethodName());
			e.printStackTrace();
		};
		protected void finished(Description description) {
			System.out.println("Finishing test: " + description.getMethodName());
		};
	};

	@BeforeClass
	public static void beforeClass() {
		if (OS_TYPE == MAC_OS_X) {
			ffmpegPath = "/usr/local/bin/ffmpeg";
		}
		avformat.av_register_all();
		avformat.avformat_network_init();
		avutil.av_log_set_level(avutil.AV_LOG_INFO);
		
		
		//delete broadcast in the db before starting
		List<Broadcast> broadcastList = callGetBroadcastList();
		for (Broadcast broadcast : broadcastList) {
			deleteBroadcast(broadcast.getStreamId());
		}
	}

	@Test
	public void testBroadcastCreateFunctional() {
		createBroadcast("name");
	}

	public Broadcast createBroadcast(String name) {
		return createBroadcast(name, null, null);
	}

	public Broadcast createBroadcast(String name, String type, String streamUrl) {
		String url = ROOT_SERVICE_URL + "/v2/broadcasts/create";

		HttpClient client = HttpClients.custom().setRedirectStrategy(new LaxRedirectStrategy()).build();
		Gson gson = new Gson();
		Broadcast broadcast = new Broadcast();
		if (name != null) {
			broadcast.setName(name);
		}

		if (type != null) {
			broadcast.setType(type);
		}

		if (streamUrl != null) {
			broadcast.setStreamUrl(streamUrl);
		}

		try {

			HttpUriRequest post = RequestBuilder.post().setUri(url)
					.setHeader(HttpHeaders.CONTENT_TYPE, "application/json")
					.setEntity(new StringEntity(gson.toJson(broadcast))).build();

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
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		return null;
	}
	

	public Result updateBroadcast(String id, String name, String description, String socialNetworks, String streamUrl) {

		return updateBroadcast(id, name, description, socialNetworks, streamUrl, null);
	}

	public Result updateBroadcast(String id, String name, String description, String socialNetworks, String streamUrl, String type) {
		String url = ROOT_SERVICE_URL + "/v2/broadcasts/" + id;

		HttpClient client = HttpClients.custom().setRedirectStrategy(new LaxRedirectStrategy()).build();
		Broadcast broadcast = new Broadcast();
		try {
			broadcast.setStreamId(id);
		} catch (Exception e1) {
			e1.printStackTrace();
			fail(e1.getMessage());
		}
		broadcast.setName(name);
		broadcast.setDescription(description);
		if (streamUrl != null) {
			broadcast.setStreamUrl(streamUrl);
		}
		if (type != null) {
			broadcast.setType(type);
		}

		try {

			HttpUriRequest post = RequestBuilder.put().setUri(url + "?socialNetworks=" + socialNetworks)
					.setHeader(HttpHeaders.CONTENT_TYPE, "application/json")
					.setEntity(new StringEntity(gson.toJson(broadcast))).build();

			HttpResponse response = client.execute(post);

			StringBuffer result = readResponse(response);

			if (response.getStatusLine().getStatusCode() != 200) {
				throw new Exception(result.toString());
			}
			System.out.println("result string: " + result.toString());
			Result tmp = gson.fromJson(result.toString(), Result.class);

			return tmp;
		} catch (Exception e) {
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

		String url = ROOT_SERVICE_URL + "/v2/broadcasts/create";

		HttpClient client = HttpClients.custom().setRedirectStrategy(new LaxRedirectStrategy()).build();
		Broadcast broadcast = null; 

		try {

			HttpUriRequest post = RequestBuilder.post().setUri(url)
					.setHeader(HttpHeaders.CONTENT_TYPE, "application/json")
					.setEntity(new StringEntity(gson.toJson(broadcast))).build();

			HttpResponse response = client.execute(post);

			StringBuffer result = readResponse(response);

			if (response.getStatusLine().getStatusCode() != 200) {
				throw new Exception(result.toString());
			}
			System.out.println("result string: " + result.toString());
			Broadcast tmp = gson.fromJson(result.toString(), Broadcast.class);
			assertNotNull(tmp);

		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testBroadcasGetUndefined() {
		try {
			/// get broadcast
			String url = ROOT_SERVICE_URL + "/v2/broadcasts/"+"any_id_not_exits" + (int)(Math.random()*9999);

			CloseableHttpClient client = HttpClients.custom().setRedirectStrategy(new LaxRedirectStrategy()).build();

			HttpUriRequest get = RequestBuilder.get().setUri(url)
					.setHeader(HttpHeaders.CONTENT_TYPE, "application/json")
					// .setEntity(new StringEntity(gson.toJson(broadcast)))
					.build();

			CloseableHttpResponse response = client.execute(get);

			StringBuffer result = readResponse(response);

			assertEquals(404, response.getStatusLine().getStatusCode() );

		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testBroadcasGetUnknown() {
		Broadcast tmp2 = getBroadcast("dsfsfsfs");
		assertNull(tmp2);

	}

	public static Broadcast getBroadcast(String streamId) {
		try {
			/// get broadcast
			String url = ROOT_SERVICE_URL + "/v2/broadcasts/"+streamId;

			CloseableHttpClient client = HttpClients.custom().setRedirectStrategy(new LaxRedirectStrategy()).build();
			Gson gson = new Gson();


			HttpUriRequest get = RequestBuilder.get().setUri(url)
					.setHeader(HttpHeaders.CONTENT_TYPE, "application/json")
					// .setEntity(new StringEntity(gson.toJson(broadcast)))
					.build();

			CloseableHttpResponse response = client.execute(get);

			StringBuffer result = readResponse(response);

			if (response.getStatusLine().getStatusCode() == 404) {
				//stream is not found
				return null;
			}
			else if (response.getStatusLine().getStatusCode() != 200){
				throw new Exception("Status code not 200 ");
			}
			System.out.println("result string: " + result.toString());
			assertFalse(result.toString().contains("dbId"));
			Broadcast tmp2 = gson.fromJson(result.toString(), Broadcast.class);
			return tmp2;
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		return null;
	}

	public List<SocialEndpointCredentials> getSocialEndpointServices() {
		try {
			/// get broadcast
			String url = ROOT_SERVICE_URL + "/v2/broadcasts/social-endpoints/0/10";

			CloseableHttpClient client = HttpClients.custom().setRedirectStrategy(new LaxRedirectStrategy()).build();
			Gson gson = new Gson();

			HttpUriRequest get = RequestBuilder.get().setUri(url)
					.setHeader(HttpHeaders.CONTENT_TYPE, "application/json")
					// .setEntity(new StringEntity(gson.toJson(broadcast)))
					.build();

			CloseableHttpResponse response = client.execute(get);

			StringBuffer result = readResponse(response);

			if (response.getStatusLine().getStatusCode() != 200) {
				throw new Exception(result.toString());
			}
			System.out.println("result string: " + result.toString());
			assertFalse(result.toString().contains("dbId"));
			Type listType = new TypeToken<List<SocialEndpointCredentials>>() {
			}.getType();
			List<SocialEndpointCredentials> tmp2 = gson.fromJson(result.toString(), listType);
			return tmp2;
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		return null;
	}

	@Test
	public void testBroadcasGetFree() {
		try {
			/// get broadcast
			String url = ROOT_SERVICE_URL + "/v2/broadcasts/";

			CloseableHttpClient client = HttpClients.custom().setRedirectStrategy(new LaxRedirectStrategy()).build();
			Gson gson = new Gson();

			HttpUriRequest get = RequestBuilder.get().setUri(url)
					.setHeader(HttpHeaders.CONTENT_TYPE, "application/json")
					// .setEntity(new StringEntity(gson.toJson(broadcast)))
					.build();

			CloseableHttpResponse response = client.execute(get);

			StringBuffer result = readResponse(response);

			assertEquals(404, response.getStatusLine().getStatusCode() );

		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	public static Broadcast callCreateBroadcast(int expireTimeMS) throws Exception {

		String url = ROOT_SERVICE_URL + "/v2/broadcasts/create";

		HttpClient client = HttpClients.custom().setRedirectStrategy(new LaxRedirectStrategy()).build();
		Gson gson = new Gson();
		Broadcast broadcast = new Broadcast();
		broadcast.setExpireDurationMS(expireTimeMS);
		broadcast.setName("testBroadcast");

		HttpUriRequest post = RequestBuilder.post().setUri(url).setHeader(HttpHeaders.CONTENT_TYPE, "application/json")
				.setEntity(new StringEntity(gson.toJson(broadcast))).build();

		HttpResponse response = client.execute(post);

		StringBuffer result = readResponse(response);

		if (response.getStatusLine().getStatusCode() != 200) {
			throw new Exception(result.toString());
		}
		System.out.println("result string: " + result.toString());
		Broadcast tmp = gson.fromJson(result.toString(), Broadcast.class);
		assertNotNull(tmp);
		assertNotSame(0L, tmp.getDate());

		return tmp;

	}

	public static Result callEnableMp4Muxing(String streamId, int mode) throws Exception {
		return callEnableRecording(streamId, mode, null);
	}
	
	
	public static Result callEnableRecording(String streamId, int mode, String recordType)  throws Exception  {
		String url = ROOT_SERVICE_URL + "/v2/broadcasts/"+ streamId +"/recording/" + (mode == 1  ? "true" : "false");
		
		url += recordType != null ? "?recordType="+recordType : "";

		HttpClient client = HttpClients.custom().setRedirectStrategy(new LaxRedirectStrategy()).build();


		HttpUriRequest post = RequestBuilder.put().setUri(url).setHeader(HttpHeaders.CONTENT_TYPE, "application/json").build();

		HttpResponse response = client.execute(post);

		StringBuffer result = readResponse(response);

		if (response.getStatusLine().getStatusCode() != 200) {
			throw new Exception(result.toString());
		}
		System.out.println("result string: " + result.toString());
		Result tmp = gson.fromJson(result.toString(), Result.class);
		assertNotNull(tmp);

		return tmp;
	}

	public static Broadcast callCreateRegularBroadcast() throws Exception {

		String url = ROOT_SERVICE_URL + "/v2/broadcasts/create";

		HttpClient client = HttpClients.custom().setRedirectStrategy(new LaxRedirectStrategy()).build();
		Gson gson = new Gson();
		Broadcast broadcast = new Broadcast();
		broadcast.setName("testBroadcast");
		HttpUriRequest post = RequestBuilder.post().setUri(url).setHeader(HttpHeaders.CONTENT_TYPE, "application/json")
				.setEntity(new StringEntity(gson.toJson(broadcast))).build();

		HttpResponse response = client.execute(post);

		StringBuffer result = readResponse(response);

		if (response.getStatusLine().getStatusCode() != 200) {
			throw new Exception(result.toString());
		}
		System.out.println("result string: " + result.toString());
		Broadcast tmp = gson.fromJson(result.toString(), Broadcast.class);
		assertNotNull(tmp);
		assertNotSame(0L, tmp.getDate());

		return tmp;

	}
	
	public static String callAddStreamSource(Broadcast broadcast) throws Exception {
		return callAddStreamSource(broadcast, false);
	}

	public static String callAddStreamSource(Broadcast broadcast, boolean autoStart) throws Exception {

		String url = ROOT_SERVICE_URL + "/v2/broadcasts/create?autoStart="+autoStart;

		HttpClient client = HttpClients.custom().setRedirectStrategy(new LaxRedirectStrategy()).build();
		Gson gson = new Gson();

		HttpUriRequest post = RequestBuilder.post().setUri(url).setHeader(HttpHeaders.CONTENT_TYPE, "application/json")
				.setEntity(new StringEntity(gson.toJson(broadcast))).build();

		HttpResponse response = client.execute(post);

		StringBuffer result = readResponse(response);

		if (response.getStatusLine().getStatusCode() != 200) {
			throw new Exception(result.toString());
		}

		return result.toString();

	}


	public static Result callUploadVod(File file) throws Exception {

		String url = ROOT_SERVICE_URL + "/v2/vods/create?name=" + file.getName();
		HttpClient client = HttpClients.custom().setRedirectStrategy(new LaxRedirectStrategy()).build();

		HttpPost post = new HttpPost(url);
		MultipartEntityBuilder builder = MultipartEntityBuilder.create();         
		builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);

		FileBody fileBody = new FileBody(file) ;

		builder.addPart("file", fileBody);

		HttpEntity entity = builder.build();
		post.setEntity(entity);
		HttpResponse response = client.execute(post);

		StringBuffer result = readResponse(response);

		if (response.getStatusLine().getStatusCode() != 200) {
			throw new Exception(result.toString());
		}
		logger.info("result string: {} ",result.toString());
		Result tmp = gson.fromJson(result.toString(), Result.class);
		assertNotNull(tmp);

		return tmp;

	}

	public  int callTotalVoDNumber() throws Exception {

		String url = ROOT_SERVICE_URL + "/v2/vods/count";
		CloseableHttpClient client = HttpClients.custom().setRedirectStrategy(new LaxRedirectStrategy()).build();
		Gson gson = new Gson();

		HttpUriRequest get = RequestBuilder.get().setUri(url)
				.setHeader(HttpHeaders.CONTENT_TYPE, "application/json")
				// .setEntity(new StringEntity(gson.toJson(broadcast)))
				.build();
		CloseableHttpResponse response = client.execute(get);

		StringBuffer result = readResponse(response);

		if (response.getStatusLine().getStatusCode() != 200) {
			throw new Exception(result.toString());
		}
		System.out.println("result string: " + result.toString());

		logger.info("result string: {} ",result.toString());
		
		return (int)(gson.fromJson(result.toString(), SimpleStat.class).number);

	}

	public static Result callUpdateStreamSource(Broadcast broadcast) throws Exception {

		String url = ROOT_SERVICE_URL + "/v2/broadcasts/"+broadcast.getStreamId();

		HttpClient client = HttpClients.custom().setRedirectStrategy(new LaxRedirectStrategy()).build();
		Gson gson = new Gson();

		HttpUriRequest post = RequestBuilder.put().setUri(url).setHeader(HttpHeaders.CONTENT_TYPE, "application/json")
				.setEntity(new StringEntity(gson.toJson(broadcast))).build();

		HttpResponse response = client.execute(post);

		StringBuffer result = readResponse(response);

		if (response.getStatusLine().getStatusCode() != 200) {
			throw new Exception(result.toString());
		}
		System.out.println("callUpdateStreamSource result string: " + result.toString());
		Result tmp = gson.fromJson(result.toString(), Result.class);
		assertNotNull(tmp);

		return tmp;

	}

	@Test
	public void testGetVersion() {

		MavenXpp3Reader reader = new MavenXpp3Reader();
		try {
			System.out.println("Getting Version");
			//first, read version from pom.xml 
			Model model = reader.read(new FileReader("pom.xml"));
			logger.info(model.getParent().getVersion());

			//then get version from rest service
			String url = ROOT_SERVICE_URL + "/v2/version";

			CloseableHttpClient client = HttpClients.custom().setRedirectStrategy(new LaxRedirectStrategy()).build();

			HttpUriRequest get = RequestBuilder.get().setUri(url)
					.setHeader(HttpHeaders.CONTENT_TYPE, "application/json")
					.build();

			CloseableHttpResponse response = client.execute(get);

			StringBuffer result = readResponse(response);

			Version versionList = null;

			versionList = gson.fromJson(result.toString(), Version.class);
			//check that they are same
			assertEquals(model.getParent().getVersion()
					, versionList.getVersionName());

			assertNotNull(versionList.getBuildNumber());
			assertTrue(versionList.getBuildNumber().length() == 13); //format is yyyyMMdd_HHmm

		}catch(Exception e){
			e.printStackTrace();
		}


	}


	public int callGetLiveStatistics() {
		try {

			String url = ROOT_SERVICE_URL + "/v2/broadcasts/active-live-stream-count";

			CloseableHttpClient client = HttpClients.custom().setRedirectStrategy(new LaxRedirectStrategy()).build();

			HttpUriRequest get = RequestBuilder.get().setUri(url)
					.setHeader(HttpHeaders.CONTENT_TYPE, "application/json")
					// .setEntity(new StringEntity(gson.toJson(broadcast)))
					.build();

			CloseableHttpResponse response = client.execute(get);

			StringBuffer result = readResponse(response);

			if (response.getStatusLine().getStatusCode() != 200) {
				throw new Exception(result.toString());
			}
			System.out.println("result string: " + result.toString());

			return (int)gson.fromJson(result.toString(), SimpleStat.class).number;

		} catch (Exception e) {
			e.printStackTrace();
		}
		return -1;
	}

	public static BroadcastStatistics callGetBroadcastStatistics(String streamId) {
		try {

			String url = ROOT_SERVICE_URL + "/v2/broadcasts/"+ streamId +"/broadcast-statistics";

			CloseableHttpClient client = HttpClients.custom().setRedirectStrategy(new LaxRedirectStrategy()).build();

			HttpUriRequest get = RequestBuilder.get().setUri(url + "?id=" + streamId)
					.setHeader(HttpHeaders.CONTENT_TYPE, "application/json")
					// .setEntity(new StringEntity(gson.toJson(broadcast)))
					.build();

			CloseableHttpResponse response = client.execute(get);

			StringBuffer result = readResponse(response);

			if (response.getStatusLine().getStatusCode() != 200) {
				throw new Exception(result.toString());
			}
			System.out.println("result string: " + result.toString());

			return gson.fromJson(result.toString(), BroadcastStatistics.class);

		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		return null;
	}

	public static List<Broadcast> callGetBroadcastList() {
		try {

			String url = ROOT_SERVICE_URL + "/v2/broadcasts/list/0/50";

			CloseableHttpClient client = HttpClients.custom().setRedirectStrategy(new LaxRedirectStrategy()).build();

			HttpUriRequest get = RequestBuilder.get().setUri(url)
					.setHeader(HttpHeaders.CONTENT_TYPE, "application/json")
					// .setEntity(new StringEntity(gson.toJson(broadcast)))
					.build();

			CloseableHttpResponse response = client.execute(get);

			StringBuffer result = readResponse(response);

			if (response.getStatusLine().getStatusCode() != 200) {
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

	public static List<VoD> callGetVoDList() {
		return callGetVoDList(0, 50);
	}

	public static List<VoD> callGetVoDList(int offset, int size) {
		try {

			String url = ROOT_SERVICE_URL + "/v2/vods/list/"+offset+"/" + size;

			CloseableHttpClient client = HttpClients.custom().setRedirectStrategy(new LaxRedirectStrategy()).build();

			HttpUriRequest get = RequestBuilder.get().setUri(url)
					.setHeader(HttpHeaders.CONTENT_TYPE, "application/json")
					// .setEntity(new StringEntity(gson.toJson(broadcast)))
					.build();

			CloseableHttpResponse response = client.execute(get);

			StringBuffer result = readResponse(response);

			if (response.getStatusLine().getStatusCode() != 200) {
				throw new Exception(result.toString());
			}
			System.out.println("Get vod list string: " + result.toString());
			Type listType = new TypeToken<List<VoD>>() {
			}.getType();

			return gson.fromJson(result.toString(), listType);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public VoD callGetVoD(String id) {
		try {

			String url = ROOT_SERVICE_URL + "/v2/vods/"+id;

			CloseableHttpClient client = HttpClients.custom().setRedirectStrategy(new LaxRedirectStrategy()).build();

			HttpUriRequest get = RequestBuilder.get().setUri(url)
					.setHeader(HttpHeaders.CONTENT_TYPE, "application/json")
					// .setEntity(new StringEntity(gson.toJson(broadcast)))
					.build();

			CloseableHttpResponse response = client.execute(get);

			StringBuffer result = readResponse(response);

			if (response.getStatusLine().getStatusCode() != 200) {
				throw new Exception(result.toString());
			}
			System.out.println("result string: " + result.toString());
			Type listType = new TypeToken<VoD>() {
			}.getType();

			return gson.fromJson(result.toString(), listType);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static Broadcast callGetBroadcast(String streamId) throws Exception {
		String url = ROOT_SERVICE_URL + "/v2/broadcasts/" + streamId;

		CloseableHttpClient client = HttpClients.custom().setRedirectStrategy(new LaxRedirectStrategy()).build();

		HttpUriRequest get = RequestBuilder.get().setUri(url + "?id=" + streamId)
				.setHeader(HttpHeaders.CONTENT_TYPE, "application/json")
				// .setEntity(new StringEntity(gson.toJson(broadcast)))
				.build();

		CloseableHttpResponse response = client.execute(get);

		StringBuffer result = readResponse(response);

		if (response.getStatusLine().getStatusCode() == 404) {
			return null;
		}
		if (response.getStatusLine().getStatusCode() != 200) {
			throw new Exception(result.toString());
		}
		System.out.println("result string: " + result.toString());

		return gson.fromJson(result.toString(), Broadcast.class);
	}

	@Test
	public void testBroadcastGet() {
		try {

			Broadcast tmp = callCreateBroadcast(0);

			/// get broadcast

			Broadcast tmp2 = callGetBroadcast(tmp.getStreamId());
			assertNotNull(tmp);
			assertEquals(tmp.getStreamId(), tmp2.getStreamId());
			assertEquals(tmp.getName(), tmp2.getName());

		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testBroadcastDeleteNULL() {

		Result result2 = deleteBroadcast(null);

		assertNotNull(result2);
		assertFalse(result2.isSuccess());
	}

	public boolean callStopBroadcastService(String streamId) throws Exception {
		String url = ROOT_SERVICE_URL + "/v2/broadcasts/"+ streamId +"/stop";

		HttpClient client = HttpClients.custom().setRedirectStrategy(new LaxRedirectStrategy()).build();
		Gson gson = new Gson();

		HttpUriRequest post = RequestBuilder.post().setUri(url)
				.setHeader(HttpHeaders.CONTENT_TYPE, "application/json").build();

		HttpResponse response = client.execute(post);

		StringBuffer result = readResponse(response);

		if (response.getStatusLine().getStatusCode() != 200) {
			throw new Exception(result.toString());
		}
		System.out.println("result string: " + result.toString());
		Result responseResult = gson.fromJson(result.toString(), Result.class);
		assertNotNull(responseResult);
		return responseResult.isSuccess();
	}

	@Test
	public void testExpireBroadcast() {

		try {
			Broadcast broadcast = callCreateBroadcast(1000);
			System.out.println("broadcast stream id: " + broadcast.getStreamId());

			Thread.sleep(5000);
			Process execute = execute(ffmpegPath + " -re -i src/test/resources/test.flv -acodec copy "
					+ "	-vcodec copy -f flv rtmp://localhost/LiveApp/" + broadcast.getStreamId());

			Thread.sleep(3000);

			broadcast = callGetBroadcast(broadcast.getStreamId());

			assertEquals(AntMediaApplicationAdapter.BROADCAST_STATUS_CREATED, broadcast.getStatus());

			execute.destroy();

			Broadcast broadcastTemp = callCreateBroadcast(10000);
			System.out.println("broadcast stream id: " + broadcast.getStreamId());

			execute = execute(ffmpegPath + " -re -i src/test/resources/test.flv -acodec copy "
					+ "	-vcodec copy -f flv rtmp://localhost/LiveApp/" + broadcastTemp.getStreamId());


			Awaitility.await().atMost(60, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
				Broadcast broadcast2 = callGetBroadcast(broadcastTemp.getStreamId());

				return AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING.equals(broadcast2.getStatus());
			});
			
			execute.destroy();

		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testStopBroadcast() {
		try {
			logger.info("Running testStopBroadcast");
			// call stop broadcast and check result is false

			assertFalse(callStopBroadcastService(String.valueOf((int) (Math.random() * 1000))));

			// create stream
			Broadcast broadcast = callCreateBroadcast(0);

			assertFalse(callStopBroadcastService(broadcast.getStreamId()));

			Broadcast broadcastReturned = callGetBroadcast(broadcast.getStreamId());

			assertEquals(AntMediaApplicationAdapter.BROADCAST_STATUS_CREATED , broadcastReturned.getStatus());

			// publish stream
			Process execute = execute(ffmpegPath + " -re -i src/test/resources/test.flv -acodec copy "
					+ "	-vcodec copy -f flv rtmp://localhost/LiveApp/" + broadcast.getStreamId());

			Awaitility.await().atMost(90, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
				Broadcast broadcastReturnedTemp = callGetBroadcast(broadcast.getStreamId());
				return (AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING).equals(broadcastReturnedTemp.getStatus());
			});

			// It should return true this time
			assertTrue(callStopBroadcastService(broadcast.getStreamId()));

			// It should return false again because it is already closed
			assertFalse(callStopBroadcastService(broadcast.getStreamId()));


			Awaitility.await().atMost(90, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
				Broadcast broadcastReturnedTemp = callGetBroadcast(broadcast.getStreamId());
				return (AntMediaApplicationAdapter.BROADCAST_STATUS_FINISHED).equals(broadcastReturnedTemp.getStatus());
			});

		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		logger.info("leaving testStopBroadcast");
	}

	@Test
	public void testUploadVoDFile() {

		Result result = new Result(false);
		File file = new File("src/test/resources/sample_MP4_480.mp4");
		List<VoD> voDList = callGetVoDList();

		for (VoD vod : voDList) {
			deleteVoD(vod.getVodId());
		}
		voDList = callGetVoDList();
		int vodCount = voDList.size();
		logger.info("initial vod count: {}" , vodCount);


		try {
			result = callUploadVod(file);
		} catch (Exception e) {
			e.printStackTrace();
		}

		assertTrue(result.isSuccess());

		String vodId =  result.getMessage(); 

		Awaitility.await().atMost(10, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
			List<VoD> tmpVoDList = callGetVoDList();
			logger.info("received vod list size: {} expected vod list size: {}", tmpVoDList.size(), vodCount+1);
			return (vodCount+1 == tmpVoDList.size()) && 
					MuxingTest.testFile("http://" + SERVER_ADDR + ":5080/LiveApp/streams/" + vodId + ".mp4");
		});

		voDList = callGetVoDList();
		boolean found = false;
		for (VoD vod : voDList) {
			if (vod.getVodId().equals(vodId)) {
				//System.out.println("vod get name: " + vod.getVodName() + " file name: " + vodId);
				assertTrue(vod.getFilePath().contains(vodId));
				assertEquals(VoD.UPLOADED_VOD, vod.getType());
				found = true;
			}
		}

		assertTrue(found);

		VoD vod = callGetVoD(vodId);
		assertNotNull(vod);
		System.out.println("vod file name: " + vod.getFilePath());
		assertTrue(vod.getFilePath().startsWith("streams/" + vodId + ".mp4"));


		result = deleteVoD(vodId);
		assertTrue(result.isSuccess());

		//file should be deleted
		assertFalse(MuxingTest.isURLAvailable("http://" + SERVER_ADDR + ":5080/LiveApp/streams/" + vodId + ".mp4"));



	}

	public String makePOSTRequest(String url, String entity) {
		try {
			CloseableHttpClient client = HttpClients.custom().setRedirectStrategy(new LaxRedirectStrategy()).build();

			RequestBuilder builder = RequestBuilder.post().setUri(url).setHeader(HttpHeaders.CONTENT_TYPE,
					"application/json");

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
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		return null;
	}

	public static Result deleteBroadcast(String id) {
		try {
			// delete broadcast
			String url = ROOT_SERVICE_URL + "/v2/broadcasts/" + id;

			CloseableHttpClient client = HttpClients.custom().setRedirectStrategy(new LaxRedirectStrategy()).build();

			HttpUriRequest post = RequestBuilder.delete().setUri(url)
					.setHeader(HttpHeaders.CONTENT_TYPE, "application/json").build();

			CloseableHttpResponse response = client.execute(post);

			StringBuffer result = readResponse(response);

			if (response.getStatusLine().getStatusCode() != 200) {
				throw new Exception(result.toString());
			}
			System.out.println("result string: " + result.toString());
			Result result2 = gson.fromJson(result.toString(), Result.class);
			return result2;
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		return null;
	}

	public static Result deleteVoD(String id) {
		try {
			// delete broadcast
			String url = ROOT_SERVICE_URL + "/v2/vods/" + id;

			CloseableHttpClient client = HttpClients.custom().setRedirectStrategy(new LaxRedirectStrategy()).build();

			HttpUriRequest post = RequestBuilder.delete().setUri(url)
					.setHeader(HttpHeaders.CONTENT_TYPE, "application/json").build();

			CloseableHttpResponse response = client.execute(post);

			StringBuffer result = readResponse(response);

			if (response.getStatusLine().getStatusCode() != 200) {
				throw new Exception(result.toString());
			}
			System.out.println("result string: " + result.toString());
			Result result2 = gson.fromJson(result.toString(), Result.class);
			return result2;
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		return null;
	}


	@Test
	public void testUpdate() {

		System.out.println("Running testUpdate");
		// create broadcast
		Broadcast broadcast = createBroadcast(null);

		String name = "string name";
		String description = "String descriptio";
		// update name and description
		try {
			// update broadcast just name no social network
			Result result = updateBroadcast(broadcast.getStreamId(), name, description, "", null);
			assertTrue(result.isSuccess());

			// check that name is updated
			// get broadcast
			broadcast = getBroadcast(broadcast.getStreamId().toString());

			// check name and description
			assertEquals(broadcast.getName(), name);
			assertEquals(broadcast.getDescription(), description);
			assertTrue(broadcast.isPublish());


			// get broacdast
			broadcast = getBroadcast(broadcast.getStreamId().toString());

			// check publish info
			assertEquals(broadcast.getName(), name);
			assertEquals(broadcast.getDescription(), description);

			name = "name 2";
			description = " description 2";
			// update broadcast name and add social network
			result = updateBroadcast(broadcast.getStreamId(), name, description, "", null);
			assertTrue(result.isSuccess());

			broadcast = getBroadcast(broadcast.getStreamId().toString());
			assertEquals(broadcast.getName(), name);
			assertEquals(broadcast.getDescription(), description);

			// update broadcast name
			name = "name 3";
			description = " description 3";
			result = updateBroadcast(broadcast.getStreamId(), name, description, "", null);
			assertTrue(result.isSuccess());

			// check that name is updated on stream name and social end point
			// stream name
			broadcast = getBroadcast(broadcast.getStreamId().toString());
			assertEquals(broadcast.getName(), name);
			assertEquals(broadcast.getDescription(), description);

			assertNull(broadcast.getEndPointList());

			// update broadcast name and remove social endpoint
			result = updateBroadcast(broadcast.getStreamId(), name, description, "", null);

			// check that social endpoint is removed
			broadcast = getBroadcast(broadcast.getStreamId().toString());
			assertNull(broadcast.getEndPointList());

		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		System.out.println("Leaving testUpdate");

	}
	
	public static Result removeEndpoint(String broadcastId, String rtmpUrl) throws Exception 
	{
		String url = ROOT_SERVICE_URL + "/v2/broadcasts/"+ broadcastId +"/endpoint?rtmpUrl=" + rtmpUrl;
		
		CloseableHttpClient client = HttpClients.custom().setRedirectStrategy(new LaxRedirectStrategy()).build();
		
		HttpUriRequest request = RequestBuilder.delete().setUri(url).setHeader(HttpHeaders.CONTENT_TYPE, "application/json").build();
	
		CloseableHttpResponse response = client.execute(request);
		
		StringBuffer result = readResponse(response);
		
		if (response.getStatusLine().getStatusCode() != 200) {
			throw new Exception(result.toString());
		}
		
		Gson gson = new Gson();
		System.out.println("result string: " + result.toString());
		Result tmp = gson.fromJson(result.toString(), Result.class);

		return tmp;
	}
	
	public static Result removeEndpointV2(String broadcastId, String endpointServiceId) throws Exception 
	{
		String url = ROOT_SERVICE_URL + "/v2/broadcasts/"+ broadcastId +"/rtmp-endpoint?endpointServiceId=" + endpointServiceId;
		
		CloseableHttpClient client = HttpClients.custom().setRedirectStrategy(new LaxRedirectStrategy()).build();
		
		HttpUriRequest request = RequestBuilder.delete().setUri(url).setHeader(HttpHeaders.CONTENT_TYPE, "application/json").build();
	
		CloseableHttpResponse response = client.execute(request);
		
		StringBuffer result = readResponse(response);
		
		if (response.getStatusLine().getStatusCode() != 200) {
			throw new Exception(result.toString());
		}
		
		Gson gson = new Gson();
		System.out.println("result string: " + result.toString());
		Result tmp = gson.fromJson(result.toString(), Result.class);

		return tmp;
	}
	
	public static Result addEndpoint(String broadcastId, String rtmpUrl) throws Exception 
	{
		String url = ROOT_SERVICE_URL + "/v2/broadcasts/"+ broadcastId +"/endpoint?rtmpUrl=" + rtmpUrl;
		
		CloseableHttpClient client = HttpClients.custom().setRedirectStrategy(new LaxRedirectStrategy()).build();

		HttpUriRequest post = RequestBuilder.post().setUri(url)
				.setHeader(HttpHeaders.CONTENT_TYPE, "application/json").build();

		CloseableHttpResponse response = client.execute(post);

		StringBuffer result = readResponse(response);

		if (response.getStatusLine().getStatusCode() != 200) {
			throw new Exception(result.toString());
		}
		Gson gson = new Gson();
		System.out.println("result string: " + result.toString());
		Result tmp = gson.fromJson(result.toString(), Result.class);

		return tmp;
	}

	public static Result addEndpointV2(String broadcastId, Endpoint endpoint) throws Exception 
	{		
		String url = ROOT_SERVICE_URL + "/v2/broadcasts/"+ broadcastId +"/rtmp-endpoint";
		
		CloseableHttpClient client = HttpClients.custom().setRedirectStrategy(new LaxRedirectStrategy()).build();

		HttpUriRequest post = RequestBuilder.post().setUri(url)
				.setHeader(HttpHeaders.CONTENT_TYPE, "application/json")
				.setEntity(new StringEntity(gson.toJson(endpoint))).build();

		CloseableHttpResponse response = client.execute(post);

		StringBuffer result = readResponse(response);

		if (response.getStatusLine().getStatusCode() != 200) {
			throw new Exception(result.toString());
		}
		Gson gson = new Gson();
		System.out.println("result string: " + result.toString());
		Result tmp = gson.fromJson(result.toString(), Result.class);

		return tmp;
	}

	public Result addSocialEndpoint(String broadcastId, String serviceId) throws Exception 
	{
		String url = ROOT_SERVICE_URL + "/v2/broadcasts/"+ broadcastId +"/social-endpoints/" + serviceId;
		
		CloseableHttpClient client = HttpClients.custom().setRedirectStrategy(new LaxRedirectStrategy()).build();

		HttpUriRequest post = RequestBuilder.post().setUri(url)
				.setHeader(HttpHeaders.CONTENT_TYPE, "application/json").build();

		CloseableHttpResponse response = client.execute(post);

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
		
		String url = ROOT_SERVICE_URL + "/v2/broadcasts/social-networks/" + serviceName;
		
		CloseableHttpClient client = HttpClients.custom().setRedirectStrategy(new LaxRedirectStrategy()).build();

		HttpUriRequest post = RequestBuilder.post().setUri(url)
				.setHeader(HttpHeaders.CONTENT_TYPE, "application/json").build();

		CloseableHttpResponse response = client.execute(post);

		StringBuffer result = readResponse(response);

		if (response.getStatusLine().getStatusCode() != 200) {
			throw new Exception(result.toString());
		}

		Gson gson = new Gson();
		System.out.println("result string: " + result.toString());
		DeviceAuthParameters tmp = gson.fromJson(result.toString(), DeviceAuthParameters.class);

		return tmp;

	}

	private Result checkDeviceAuthStatus(String code) throws Exception 
	{
		String url = ROOT_SERVICE_URL + "/v2/broadcasts/social-network-status/" + code;
		
		CloseableHttpClient client = HttpClients.custom().setRedirectStrategy(new LaxRedirectStrategy()).build();

		HttpUriRequest post = RequestBuilder.get().setUri(url)
				.setHeader(HttpHeaders.CONTENT_TYPE, "application/json").build();

		CloseableHttpResponse response = client.execute(post);

		StringBuffer result = readResponse(response);

		if (response.getStatusLine().getStatusCode() != 200) {
			throw new Exception(result.toString());
		}
		Gson gson = new Gson();
		System.out.println("result string: " + result.toString());
		Result tmp = gson.fromJson(result.toString(), Result.class);

		return tmp;

	}

	// check that social endpoints are added correctly

	@Test
	public void testJSONIgnore() {
		Broadcast broadcast = new Broadcast();
		broadcast.setName("name");

		try {
			String serializedStr = new ObjectMapper().writeValueAsString(broadcast);
			// check that this field exist
			assertNull(broadcast.getDbId());
			assertFalse(serializedStr.toString().contains("dbId"));
		} catch (JsonProcessingException e) {
			e.printStackTrace();
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
			// authenticate facebook
			// get parameters
			DeviceAuthParameters deviceAuthParameters = getDeviceAuthParameters("facebook");
			System.out.println(" url: " + deviceAuthParameters.verification_url);
			System.out.println(" user code: " + deviceAuthParameters.user_code);
			assertNotNull(deviceAuthParameters.verification_url);
			assertNotNull(deviceAuthParameters.user_code);

			// ask if authenticated
			do {
				System.out.println("You should enter this code: " + deviceAuthParameters.user_code + " to this url: "
						+ deviceAuthParameters.verification_url);
				System.out.println("Waiting before asking auth status");

				Thread.sleep(deviceAuthParameters.interval * 1000);
				result = checkDeviceAuthStatus("facebook");
				System.out.println("auth status is " + result.isSuccess());

			} while (!result.isSuccess());

			assertTrue(result.isSuccess());

			// authenticate twitter
			deviceAuthParameters = getDeviceAuthParameters("periscope");
			System.out.println(" url: " + deviceAuthParameters.verification_url);
			System.out.println(" user code: " + deviceAuthParameters.user_code);
			assertNotNull(deviceAuthParameters.verification_url);
			assertNotNull(deviceAuthParameters.user_code);

			do {
				System.out.println("You should enter this code: " + deviceAuthParameters.user_code + " to this url: "
						+ deviceAuthParameters.verification_url);
				System.out.println("Waiting " + deviceAuthParameters.interval + " seconds before asking auth status");

				Thread.sleep(deviceAuthParameters.interval * 1000);
				result = checkDeviceAuthStatus("periscope");
				System.out.println("auth status is " + result.isSuccess());

			} while (!result.isSuccess());

			assertTrue(result.isSuccess());

			// authenticate youtube
			deviceAuthParameters = getDeviceAuthParameters("youtube");
			System.out.println(" url: " + deviceAuthParameters.verification_url);
			System.out.println(" user code: " + deviceAuthParameters.user_code);
			assertNotNull(deviceAuthParameters.verification_url);
			assertNotNull(deviceAuthParameters.user_code);

			do {
				System.out.println("You should enter this code: " + deviceAuthParameters.user_code + " to this url: "
						+ deviceAuthParameters.verification_url);
				System.out.println("Waiting " + deviceAuthParameters.interval + " seconds before asking auth status");

				Thread.sleep(deviceAuthParameters.interval * 1000);
				result = checkDeviceAuthStatus("youtube");
				System.out.println("auth status is " + result.isSuccess());

			} while (!result.isSuccess());

		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testAddEndpoint() {
		try {

			Broadcast broadcast = createBroadcast(null);

			// add generic endpoint
			Result result = addEndpoint(broadcast.getStreamId().toString(), "rtmp://dfjdksafjlaskfjalkfj");

			// check that it is successfull
			assertTrue(result.isSuccess());

			// get endpoint list
			broadcast = getBroadcast(broadcast.getStreamId().toString());

			// check that 2 element exist
			assertNotNull(broadcast.getEndPointList());
			assertEquals(1, broadcast.getEndPointList().size());

		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testAddEndpointV2() {
		try {

			Broadcast broadcast = createBroadcast(null);

			
			String rtmpUrl = "rtmp://dfjdksafjlaskfjalkfj";
			
			Endpoint endpoint = new Endpoint();
			endpoint.setRtmpUrl(rtmpUrl);

			// add generic endpoint
			Result result = addEndpointV2(broadcast.getStreamId().toString(), endpoint);

			// check that it is successfull
			assertTrue(result.isSuccess());

			// get endpoint list
			broadcast = getBroadcast(broadcast.getStreamId().toString());

			// check that 1 element exist
			assertNotNull(broadcast.getEndPointList());
			assertEquals(1, broadcast.getEndPointList().size());

		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
	@Test
	public void testAddEndpointCrossCheck() {
		try {
			
			List<Broadcast> broadcastList = callGetBroadcastList();
			int size = broadcastList.size();
			Broadcast broadcast = createBroadcast(null);

			String streamId = RandomStringUtils.randomAlphabetic(6);
			// add generic endpoint
			Result result = addEndpoint(broadcast.getStreamId().toString(), "rtmp://localhost/LiveApp/" + streamId);

			// check that it is successfull
			assertTrue(result.isSuccess());

			// get endpoint list
			broadcast = getBroadcast(broadcast.getStreamId().toString());

			// check that 4 element exist
			assertNotNull(broadcast.getEndPointList());
			assertEquals(1, broadcast.getEndPointList().size());

			broadcastList = callGetBroadcastList();
			assertEquals(size+1, broadcastList.size());

			Process execute = execute(
					ffmpegPath + " -re -i src/test/resources/test.flv -codec copy -f flv rtmp://localhost/LiveApp/"
							+ broadcast.getStreamId());


			Awaitility.await().atMost(60, TimeUnit.SECONDS).pollInterval(2, TimeUnit.SECONDS).until(() -> {
				//size should +2 because we restream again into the server
				return size+2 == callGetBroadcastList().size();
			});

			execute.destroy();

			result = deleteBroadcast(broadcast.getStreamId());
			assertTrue(result.isSuccess());

			Awaitility.await().atMost(60, TimeUnit.SECONDS)
			.pollInterval(2, TimeUnit.SECONDS).until(() -> 
			{
				int broadcastListSize = callGetBroadcastList().size();
				logger.info("broadcast list size: {} and it should be:{}", broadcastListSize, size);
				return size == callGetBroadcastList().size();
			});



		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testAddEndpointCrossCheckV2() {
		try {
			
			List<Broadcast> broadcastList = callGetBroadcastList();
			int size = broadcastList.size();
			Broadcast broadcast = createBroadcast(null);

			String streamId = RandomStringUtils.randomAlphabetic(6);
			
			String rtmpUrl = "rtmp://localhost/LiveApp/" + streamId;
			
			Endpoint endpoint = new Endpoint();
			endpoint.setRtmpUrl(rtmpUrl);

			// add generic endpoint
			Result result = addEndpointV2(broadcast.getStreamId().toString(), endpoint);

			// check that it is successfull
			assertTrue(result.isSuccess());

			// get endpoint list
			broadcast = getBroadcast(broadcast.getStreamId().toString());

			// check that 4 element exist
			assertNotNull(broadcast.getEndPointList());
			assertEquals(1, broadcast.getEndPointList().size());

			broadcastList = callGetBroadcastList();
			assertEquals(size+1, broadcastList.size());

			Process execute = execute(
					ffmpegPath + " -re -i src/test/resources/test.flv -codec copy -f flv rtmp://localhost/LiveApp/"
							+ broadcast.getStreamId());


			Awaitility.await().atMost(45, TimeUnit.SECONDS).pollInterval(2, TimeUnit.SECONDS).until(() -> {
				//size should +2 because we restream again into the server
				return size+2 == callGetBroadcastList().size();
			});

			execute.destroy();

			result = deleteBroadcast(broadcast.getStreamId());
			assertTrue(result.isSuccess());

			Awaitility.await().atMost(45, TimeUnit.SECONDS)
			.pollInterval(2, TimeUnit.SECONDS).until(() -> 
			{
				int broadcastListSize = callGetBroadcastList().size();
				logger.info("broadcast list size: {} and it should be:{}", broadcastListSize, size);
				return size == callGetBroadcastList().size();
			});



		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testAddStreamSourceUrlCheck() {

		//create broadcast
		Broadcast broadcast = new Broadcast();
		Result result = new Result (false);

		try {

			assertNotNull(broadcast);
			broadcast.setName("name");
			broadcast.setType(AntMediaApplicationAdapter.STREAM_SOURCE);
			broadcast.setStatus(AntMediaApplicationAdapter.BROADCAST_STATUS_CREATED);
			broadcast.setUsername("admin");
			broadcast.setPassword("admin");

			//define invalid stream url
			broadcast.setStreamUrl("rrtsp://admin:Admin12345@71.234.93.90:5011/12");
			
			try {
				gson.fromJson(callAddStreamSource(broadcast), Result.class);
				//it should throw exceptionbecause url is invalid
				fail("it should throw exceptionbecause url is invalid");
			}
			catch (Exception e) {
				//it should throw exceptionbecause url is invalid
			}
			//define valid stream url
			broadcast.setStreamUrl("rtsp://admin:Admin12345@71.234.93.90:5011/12");

			Broadcast createdBroadcast = gson.fromJson(callAddStreamSource(broadcast), Broadcast.class);

			//it should be true because url is valid
			assertNotNull(createdBroadcast.getStreamId());

			Broadcast fetchedBroadcast = callGetBroadcast(createdBroadcast.getStreamId());

			//change url

			fetchedBroadcast.setStreamUrl("rtsp://admin:Admin12345@71.234.93.90:5014/11");

			//update broadcast
			result = callUpdateStreamSource(fetchedBroadcast);

			assertTrue(result.isSuccess());

			fetchedBroadcast = callGetBroadcast(fetchedBroadcast.getStreamId());

			assertEquals("rtsp://admin:Admin12345@71.234.93.90:5014/11", fetchedBroadcast.getStreamUrl());

			deleteBroadcast(fetchedBroadcast.getStreamId());


		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testBroadcastDelete() {
		try {

			//create broadcast
			Broadcast broadcast = callCreateRegularBroadcast();
			assertNotNull(broadcast);

			/// get broadcast	
			Broadcast broadcastFetched = callGetBroadcast(broadcast.getStreamId());

			assertNotNull(broadcastFetched);
			assertEquals(broadcast.getStreamId(), broadcastFetched.getStreamId());
			assertEquals(broadcast.getName(), broadcastFetched.getName());

			// publish stream
			Process execute = execute(ffmpegPath + " -re -i src/test/resources/test.flv -acodec copy "
					+ "	-vcodec copy -f flv rtmp://localhost/LiveApp/" + broadcastFetched.getStreamId());

			/// get broadcast	
			Awaitility.await().atMost(40, TimeUnit.SECONDS).pollInterval(2, TimeUnit.SECONDS).until(()-> {
				Broadcast broadcast2 = callGetBroadcast(broadcast.getStreamId());
				return broadcast2 != null && AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING.equals(broadcast2.getStatus());
			});


			// delete broadcast
			Result result = deleteBroadcast(broadcastFetched.getStreamId());
			assertNotNull(result);
			assertTrue(result.isSuccess());

			//delete again
			Result result2 = deleteBroadcast(broadcastFetched.getStreamId());
			assertNotNull(result2);
			assertFalse(result2.isSuccess());

			Broadcast broadcast3 = callGetBroadcast(broadcast.getStreamId());
			assertNull(broadcast3);

		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	public static StringBuffer readResponse(HttpResponse response) throws IOException {
		StringBuffer result = new StringBuffer();

		if(response.getEntity() != null) {
			BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

			String line = "";
			while ((line = rd.readLine()) != null) {
				result.append(line);
			}
		}
		return result;
	}

	@Test
	public void testVoDIdListByStreamId() {
		
		ConsoleAppRestServiceTest.resetCookieStore();
		Result result;
		try {
			result = ConsoleAppRestServiceTest.callisFirstLogin();

			if (result.isSuccess()) {
				Result createInitialUser = ConsoleAppRestServiceTest.createDefaultInitialUser();
				assertTrue(createInitialUser.isSuccess());
			}

			result = ConsoleAppRestServiceTest.authenticateDefaultUser();
			assertTrue(result.isSuccess());
			Random r = new Random();
			String streamId = "streamId" + r.nextInt();

			AppSettings appSettingsModel = ConsoleAppRestServiceTest.callGetAppSettings("LiveApp");

			appSettingsModel.setMp4MuxingEnabled(true);

			result = ConsoleAppRestServiceTest.callSetAppSettings("LiveApp", appSettingsModel);
			assertTrue(result.isSuccess());

			//this process should be terminated automatically because test.flv has 25fps and 360p

			startStopRTMPBroadcast(streamId);

			Awaitility.await().atMost(50, TimeUnit.SECONDS).until(()-> {
				return isUrlExist("http://localhost:5080/LiveApp/streams/"+streamId+".mp4");
			});

			startStopRTMPBroadcast(streamId);

			Awaitility.await().atMost(50, TimeUnit.SECONDS).until(()-> {
				return isUrlExist("http://localhost:5080/LiveApp/streams/"+streamId+"_1.mp4");
			});

			startStopRTMPBroadcast("dummyStreamId");

			Awaitility.await().atMost(50, TimeUnit.SECONDS).until(()-> {
				return isUrlExist("http://localhost:5080/LiveApp/streams/"+"dummyStreamId.mp4");
			});
			String url = ROOT_SERVICE_URL + "/v2/vods/list/0/50?streamId="+streamId;
			HttpClient client = HttpClients.custom().setRedirectStrategy(new LaxRedirectStrategy())
					.setDefaultCookieStore(ConsoleAppRestServiceTest.getHttpCookieStore()).build();
			Gson gson = new Gson();

			HttpUriRequest get = RequestBuilder.get().setUri(url).build();

			HttpResponse response = client.execute(get);

			StringBuffer restResult = RestServiceV2Test.readResponse(response);

			if (response.getStatusLine().getStatusCode() != 200) {
				System.out.println("status code: " + response.getStatusLine().getStatusCode());
				throw new Exception(restResult.toString());
			}
			logger.info("result string: " + restResult.toString());
			Type listType = new TypeToken<List<VoD>>() {}.getType();

			List<VoD> vodIdList = gson.fromJson(restResult.toString(), listType);
			assertNotNull(vodIdList);

			boolean isEnterprise = callIsEnterpriseEdition().getMessage().contains("Enterprise");
			// It's added due to Adaptive Settings added as default in Enterprise Edition. 
			if(isEnterprise) {
				assertEquals(4, vodIdList.size());
			}
			else {
				assertEquals(2, vodIdList.size());
			}

		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	private void startStopRTMPBroadcast(String streamId) throws InterruptedException {
		Process rtmpSendingProcess = AppFunctionalV2Test.execute(ffmpegPath
				+ " -re -i src/test/resources/test.flv  -codec copy -f flv rtmp://127.0.0.1/LiveApp/"
				+ streamId);

		Awaitility.await().atMost(10, TimeUnit.SECONDS).until(()-> {
			return rtmpSendingProcess.isAlive();
		});

		Thread.sleep(5000);

		rtmpSendingProcess.destroy();
		Awaitility.await().atMost(10, TimeUnit.SECONDS).until(()-> {
			return !rtmpSendingProcess.isAlive();
		});
	}

	private boolean isUrlExist(String url) {
		try {
			return ((HttpURLConnection) new URL(url).openConnection()).getResponseCode() == 200;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}
	
	public Result callIsEnterpriseEdition() throws Exception {

		String url = "http://localhost:5080/LiveApp/rest/v2/version";
		CloseableHttpClient client = HttpClients.custom().setRedirectStrategy(new LaxRedirectStrategy()).build();
		Gson gson = new Gson();

		HttpUriRequest get = RequestBuilder.get().setUri(url).build();
		CloseableHttpResponse response = client.execute(get);

		StringBuffer result = readResponse(response);


		if (response.getStatusLine().getStatusCode() != 200) {
			throw new Exception(result.toString());
		}
		logger.info("result string: {} ",result.toString());

		Version version = gson.fromJson(result.toString(),Version.class);



		Result resultResponse = new Result(true, version.getVersionType());

		assertNotNull(resultResponse);

		return resultResponse;


	}

}
