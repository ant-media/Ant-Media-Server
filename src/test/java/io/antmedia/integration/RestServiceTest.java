package io.antmedia.integration;

import static org.bytedeco.javacpp.avformat.av_register_all;
import static org.bytedeco.javacpp.avformat.avformat_network_init;
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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;

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
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.red5.server.api.scope.IScope;
import org.red5.server.scope.WebScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.datastore.db.MapDBStore;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.datastore.db.types.Endpoint;
import io.antmedia.datastore.db.types.SocialEndpointCredentials;
import io.antmedia.datastore.db.types.VoD;
import io.antmedia.rest.BroadcastRestService;
import io.antmedia.rest.BroadcastRestService.BroadcastStatistics;
import io.antmedia.rest.BroadcastRestService.LiveStatistics;
import io.antmedia.rest.StreamsSourceRestService;
import io.antmedia.rest.model.Result;
import io.antmedia.rest.model.Version;
import io.antmedia.social.endpoint.VideoServiceEndpoint.DeviceAuthParameters;
import io.antmedia.test.MuxerUnitTest;
import io.antmedia.test.StreamSchedularUnitTest;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.awaitility.Awaitility;
import org.bytedeco.javacpp.avformat;
import org.bytedeco.javacpp.avutil;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;


import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

public class RestServiceTest {

	private static final String ROOT_APP_URL = "http://localhost:5080/LiveApp";

	private static final String ROOT_SERVICE_URL = "http://localhost:5080/LiveApp/rest";
	private static final String SERVER_ADDR = "127.0.0.1";
	private static Process tmpExec;
	private BroadcastRestService restService = null;
	protected static Logger logger = LoggerFactory.getLogger(RestServiceTest.class);
	public AntMediaApplicationAdapter app = null;
	private WebScope appScope;
	private ApplicationContext appCtx;
	private IScope scope;

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

		HttpClient client = HttpClients.custom().setRedirectStrategy(new LaxRedirectStrategy()).build();
		Gson gson = new Gson();
		Broadcast broadcast = new Broadcast();
		if (name != null) {
			broadcast.setName(name);
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


	public Broadcast save(Broadcast broadcast) {
		String url = ROOT_SERVICE_URL + "/broadcast/create";

		HttpClient client = HttpClients.custom().setRedirectStrategy(new LaxRedirectStrategy()).build();
		Gson gson = new Gson();


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



	public Result updateBroadcast(String id, String name, String description, String socialNetworks) {
		String url = ROOT_SERVICE_URL + "/broadcast/update";

		HttpClient client = HttpClients.custom().setRedirectStrategy(new LaxRedirectStrategy()).build();
		Gson gson = new Gson();
		Broadcast broadcast = new Broadcast();
		try {
			broadcast.setStreamId(id);
		} catch (Exception e1) {
			e1.printStackTrace();
			fail(e1.getMessage());
		}
		broadcast.setName(name);
		broadcast.setDescription(description);

		try {

			HttpUriRequest post = RequestBuilder.post().setUri(url + "?socialNetworks=" + socialNetworks)
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

		String url = ROOT_SERVICE_URL + "/broadcast/create";

		HttpClient client = HttpClients.custom().setRedirectStrategy(new LaxRedirectStrategy()).build();
		Gson gson = new Gson();
		Broadcast broadcast = null; // new Broadcast();
		// broadcast.name = "name";

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
			String url = ROOT_SERVICE_URL + "/broadcast/get";

			CloseableHttpClient client = HttpClients.custom().setRedirectStrategy(new LaxRedirectStrategy()).build();
			Gson gson = new Gson();
			// Broadcast broadcast = null; //new Broadcast();
			// broadcast.name = "name";

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
			Broadcast tmp2 = gson.fromJson(result.toString(), Broadcast.class);
			assertNotNull(tmp2);
			assertEquals(null, tmp2.getStreamId());

		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testBroadcasGetUnknown() {
		Broadcast tmp2 = getBroadcast("dsfsfsfs");
		assertNotNull(tmp2);
		assertEquals(null, tmp2.getStreamId());

	}

	public Broadcast getBroadcast(String streamId) {
		try {
			/// get broadcast
			String url = ROOT_SERVICE_URL + "/broadcast/get";

			CloseableHttpClient client = HttpClients.custom().setRedirectStrategy(new LaxRedirectStrategy()).build();
			Gson gson = new Gson();
			// Broadcast broadcast = null; //new Broadcast();
			// broadcast.name = "name";

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
			String url = ROOT_SERVICE_URL + "/broadcast/getSocialEndpoints/0/10";

			CloseableHttpClient client = HttpClients.custom().setRedirectStrategy(new LaxRedirectStrategy()).build();
			Gson gson = new Gson();
			// Broadcast broadcast = null; //new Broadcast();
			// broadcast.name = "name";

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
			String url = ROOT_SERVICE_URL + "/broadcast/get";

			CloseableHttpClient client = HttpClients.custom().setRedirectStrategy(new LaxRedirectStrategy()).build();
			Gson gson = new Gson();
			// Broadcast broadcast = null; //new Broadcast();
			// broadcast.name = "name";

			HttpUriRequest get = RequestBuilder.get().setUri(url + "?id=")
					.setHeader(HttpHeaders.CONTENT_TYPE, "application/json")
					// .setEntity(new StringEntity(gson.toJson(broadcast)))
					.build();

			CloseableHttpResponse response = client.execute(get);

			StringBuffer result = readResponse(response);

			if (response.getStatusLine().getStatusCode() != 200) {
				throw new Exception(result.toString());
			}
			System.out.println("result string: " + result.toString());
			Broadcast tmp2 = gson.fromJson(result.toString(), Broadcast.class);
			assertNotNull(tmp2);
			assertEquals(null, tmp2.getStreamId());

		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	public static Broadcast callCreateBroadcast(int expireTimeMS) throws Exception {

		String url = ROOT_SERVICE_URL + "/broadcast/create";

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

	public static Broadcast callCreateRegularBroadcast() throws Exception {

		String url = ROOT_SERVICE_URL + "/broadcast/create";

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

	public static Result callAddStreamSource(Broadcast broadcast) throws Exception {

		String url = ROOT_SERVICE_URL + "/streamSource/addStreamSource";

		HttpClient client = HttpClients.custom().setRedirectStrategy(new LaxRedirectStrategy()).build();
		Gson gson = new Gson();

		HttpUriRequest post = RequestBuilder.post().setUri(url).setHeader(HttpHeaders.CONTENT_TYPE, "application/json")
				.setEntity(new StringEntity(gson.toJson(broadcast))).build();

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
	
	
	public static Result callUploadVod(File file) throws Exception {

		String url = ROOT_SERVICE_URL + "/broadcast/uploadVoDFile/" + file.getName();
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

		String url = ROOT_SERVICE_URL + "/broadcast/getTotalVodNumber";
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


		if (response.getStatusLine().getStatusCode() != 200) {
			throw new Exception(result.toString());
		}
		logger.info("result string: {} ",result.toString());
		
		return gson.fromJson(result.toString(),Integer.class);

	}
	
	public static Result callUpdateStreamSource(Broadcast broadcast) throws Exception {

		String url = ROOT_SERVICE_URL + "/streamSource/updateCamInfo";

		HttpClient client = HttpClients.custom().setRedirectStrategy(new LaxRedirectStrategy()).build();
		Gson gson = new Gson();

		HttpUriRequest post = RequestBuilder.post().setUri(url).setHeader(HttpHeaders.CONTENT_TYPE, "application/json")
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
			//first, read version from pom.xml 
			Model model = reader.read(new FileReader("pom.xml"));
			logger.info(model.getParent().getVersion());

			//then get version from rest service
			String url = ROOT_SERVICE_URL + "/broadcast/getVersion";

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

		}catch(Exception e){
			e.printStackTrace();
		}


	}


	public LiveStatistics callGetLiveStatistics() {
		try {

			String url = ROOT_SERVICE_URL + "/broadcast/getAppLiveStatistics";

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
				throw new Exception(result.toString());
			}
			System.out.println("result string: " + result.toString());

			return gson.fromJson(result.toString(), LiveStatistics.class);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public BroadcastStatistics callGetBroadcastStatistics(String streamId) {
		try {

			String url = ROOT_SERVICE_URL + "/broadcast/getBroadcastLiveStatistics";

			CloseableHttpClient client = HttpClients.custom().setRedirectStrategy(new LaxRedirectStrategy()).build();
			// Gson gson = new Gson();
			// Broadcast broadcast = null; //new Broadcast();
			// broadcast.name = "name";

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

	public List<Broadcast> callGetBroadcastList() {
		try {

			String url = ROOT_SERVICE_URL + "/broadcast/getList/0/50";

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
		try {

			String url = ROOT_SERVICE_URL + "/broadcast/getVodList/0/50";

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

			String url = ROOT_SERVICE_URL + "/broadcast/getVoD?id="+id;

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
		String url = ROOT_SERVICE_URL + "/broadcast/get";

		CloseableHttpClient client = HttpClients.custom().setRedirectStrategy(new LaxRedirectStrategy()).build();
		// Gson gson = new Gson();
		// Broadcast broadcast = null; //new Broadcast();
		// broadcast.name = "name";

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

		return gson.fromJson(result.toString(), Broadcast.class);
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
		String url = ROOT_SERVICE_URL + "/broadcast/stop";

		HttpClient client = HttpClients.custom().setRedirectStrategy(new LaxRedirectStrategy()).build();
		Gson gson = new Gson();

		HttpUriRequest post = RequestBuilder.post().setUri(url + "/" + streamId)
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

			Thread.sleep(2000);

			Process execute = execute(ffmpegPath + " -re -i src/test/resources/test.flv -acodec copy "
					+ "	-vcodec copy -f flv rtmp://localhost/LiveApp/" + broadcast.getStreamId());

			Thread.sleep(5000);

			broadcast = callGetBroadcast(broadcast.getStreamId());

			assertEquals(AntMediaApplicationAdapter.BROADCAST_STATUS_CREATED, broadcast.getStatus());

			execute.destroy();

			broadcast = callCreateBroadcast(5000);
			System.out.println("broadcast stream id: " + broadcast.getStreamId());

			execute = execute(ffmpegPath + " -re -i src/test/resources/test.flv -acodec copy "
					+ "	-vcodec copy -f flv rtmp://localhost/LiveApp/" + broadcast.getStreamId());

			Thread.sleep(5000);

			broadcast = callGetBroadcast(broadcast.getStreamId());

			assertEquals(AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING, broadcast.getStatus());

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

			Thread.sleep(5000);

			broadcastReturned = callGetBroadcast(broadcast.getStreamId());

			assertEquals(AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING , broadcastReturned.getStatus());

			// It should return true this time
			assertTrue(callStopBroadcastService(broadcast.getStreamId()));

			// It should return false again because it is already closed
			assertFalse(callStopBroadcastService(broadcast.getStreamId()));

			Thread.sleep(5000);

			broadcastReturned = callGetBroadcast(broadcast.getStreamId());

			assertEquals(AntMediaApplicationAdapter.BROADCAST_STATUS_FINISHED, broadcastReturned.getStatus());


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

	public Result deleteBroadcast(String id) {
		try {
			// delete broadcast
			String url = ROOT_SERVICE_URL + "/broadcast/delete/" + id;

			CloseableHttpClient client = HttpClients.custom().setRedirectStrategy(new LaxRedirectStrategy()).build();

			HttpUriRequest post = RequestBuilder.post().setUri(url)
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
			String url = ROOT_SERVICE_URL + "/broadcast/deleteVoD/" + id;

			CloseableHttpClient client = HttpClients.custom().setRedirectStrategy(new LaxRedirectStrategy()).build();

			HttpUriRequest post = RequestBuilder.post().setUri(url)
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
			Result result = updateBroadcast(broadcast.getStreamId(), name, description, "");
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


			List<SocialEndpointCredentials> socialEndpointServices = getSocialEndpointServices();
			assertTrue(socialEndpointServices.size() > 0);

			name = "name 2";
			description = " description 2";
			// update broadcast name and add social network
			result = updateBroadcast(broadcast.getStreamId(), name, description, socialEndpointServices.get(0).getId());
			assertTrue(result.isSuccess());

			broadcast = getBroadcast(broadcast.getStreamId().toString());
			assertEquals(broadcast.getName(), name);
			assertEquals(broadcast.getDescription(), description);

			// update broadcast name
			name = "name 3";
			description = " description 3";
			result = updateBroadcast(broadcast.getStreamId(), name, description, socialEndpointServices.get(0).getId());
			assertTrue(result.isSuccess());

			// check that name is updated on stream name and social end point
			// stream name
			broadcast = getBroadcast(broadcast.getStreamId().toString());
			assertEquals(broadcast.getName(), name);
			assertEquals(broadcast.getDescription(), description);

			assertEquals(broadcast.getEndPointList().get(0).getName(), name);

			// update broadcast name and remove social endpoint
			result = updateBroadcast(broadcast.getStreamId(), name, description, "");

			// check that social endpoint is removed
			broadcast = getBroadcast(broadcast.getStreamId().toString());
			assertNull(broadcast.getEndPointList());

		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		
		System.out.println("Leaving testUpdate");

	}

	public Result addEndpoint(String broadcastId, String rtmpUrl) throws Exception {
		CloseableHttpClient httpclient = HttpClients.createDefault();

		HttpPost httppost = new HttpPost(ROOT_SERVICE_URL + "/broadcast/addEndpoint");
		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
		nameValuePairs.add(new BasicNameValuePair("Content-Type", "application/x-www-form-urlencoded;"));
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
		nameValuePairs.add(new BasicNameValuePair("Content-Type", "application/x-www-form-urlencoded;"));
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
		// List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
		// nameValuePairs.add(new
		// BasicNameValuePair("Content-Type","application/x-www-form-urlencoded;"));
		// nameValuePairs.add(new BasicNameValuePair("serviceName",
		// serviceName));
		// httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs,
		// StandardCharsets.UTF_8));

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
		// List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
		// nameValuePairs.add(new
		// BasicNameValuePair("Content-Type","application/x-www-form-urlencoded;"));
		// nameValuePairs.add(new BasicNameValuePair("serviceName",
		// serviceName));
		// httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs,
		// StandardCharsets.UTF_8));

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

	@Test
	public void testCheckSocialEndpointRecreated() {
		System.out.println("Running testCheckSocialEndpointRecreated");
		Result result;
		try {
			// create broadcast
			Broadcast broadcast = createBroadcast("social_endpoint_check");
			// add facebook endpoint

			/*
			 * 
			 * in enterprise edition result =
			 * addSocialEndpoint(broadcast.getStreamId().toString(),
			 * "facebook");
			 * 
			 * //check that it is successfull assertTrue(result.success);
			 */

			/*
			 * in enterprise edition //add youtube endpoint result =
			 * addSocialEndpoint(broadcast.getStreamId().toString(), "youtube");
			 * 
			 * //check that it is succes full assertTrue(result.success);
			 */

			List<SocialEndpointCredentials> socialEndpointServices = getSocialEndpointServices();
			assertTrue(socialEndpointServices.size() > 0);

			// add twitter endpoint
			result = addSocialEndpoint(broadcast.getStreamId().toString(), socialEndpointServices.get(0).getId());

			// check that it is succes full
			assertTrue(result.isSuccess());

			// get endpoint list
			broadcast = getBroadcast(broadcast.getStreamId().toString());

			// check that 1 element exist
			assertNotNull(broadcast.getEndPointList());
			assertEquals(1, broadcast.getEndPointList().size());

			broadcast = getBroadcast(broadcast.getStreamId().toString());
			List<Endpoint> endpointList = broadcast.getEndPointList();

			for (Endpoint endpoint : endpointList) {
				System.out.println("endpoint url: " + endpoint.getRtmpUrl() + " broadcast.id=" + endpoint.getBroadcastId()
						+ " stream id: " + endpoint.getStreamId());
			}

			Process execute = execute(
					ffmpegPath + " -re -i src/test/resources/test.flv -codec copy -f flv rtmp://localhost/LiveApp/"
							+ broadcast.getStreamId());

			Thread.sleep(20000);

			execute.destroy();

			// this value is critical because server creates endpoints on social
			// networks
			Thread.sleep(15000);

			broadcast = getBroadcast(broadcast.getStreamId().toString());
			List<Endpoint> endpointList2 = broadcast.getEndPointList();
			assertEquals(1, endpointList2.size());

			for (Endpoint endpoint : endpointList2) {
				System.out.println("new endpoint url: " + endpoint.getRtmpUrl() + " broadcast.id=" + endpoint.getBroadcastId()
						+ " stream id: " + endpoint.getStreamId());

			}

			for (Endpoint endpoint : endpointList2) {
				for (Endpoint endpointFirst : endpointList) {
					System.out.println("new endpoint rtmp URL -> " + endpoint.getRtmpUrl() + " first endpoint URL -> " + endpointFirst.getRtmpUrl());
					System.out.println("new broadcast id -> " + endpoint.getBroadcastId() + " first broadcast Id -> " + endpointFirst.getBroadcastId());
					assertTrue(!endpoint.getRtmpUrl().equals(endpointFirst.getRtmpUrl())
							|| !endpoint.getBroadcastId().equals(endpointFirst.getBroadcastId()));
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		
		System.out.println("Leaving testCheckSocialEndpointRecreated");
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

		System.out.println("Running testAddEndpoint");
		try {

			Broadcast broadcast = createBroadcast(null);

			List<SocialEndpointCredentials> socialEndpointServices = getSocialEndpointServices();
			assertTrue(!socialEndpointServices.isEmpty());
			// add twitter endpoint
			Result result = addSocialEndpoint(broadcast.getStreamId().toString(), socialEndpointServices.get(0).getId());

			// check that it is succes full
			assertTrue(result.isSuccess());

			/*
			 * //add youtube endpoint result =
			 * addSocialEndpoint(broadcast.getStreamId().toString(), "youtube");
			 * //check that it is succes full assertTrue(result.success);
			 */

			// add generic endpoint
			result = addEndpoint(broadcast.getStreamId().toString(), "rtmp://dfjdksafjlaskfjalkfj");

			// check that it is successfull
			assertTrue(result.isSuccess());

			// get endpoint list
			broadcast = getBroadcast(broadcast.getStreamId().toString());

			// check that 4 element exist
			assertNotNull(broadcast.getEndPointList());
			assertEquals(2, broadcast.getEndPointList().size());

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

			result = callAddStreamSource(broadcast);

			//it should be false because url is invalid
			assertFalse(result.isSuccess());
			
			//define valid stream url
			broadcast.setStreamUrl("rtsp://admin:Admin12345@71.234.93.90:5011/12");

			result = callAddStreamSource(broadcast);

			//it should be true because url is valid
			assertTrue(result.isSuccess());

			Broadcast fetchedBroadcast = callGetBroadcast(result.getMessage());
			
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
			Awaitility.await().atMost(10, TimeUnit.SECONDS).pollInterval(2, TimeUnit.SECONDS).until(()-> {
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
			assertNotEquals(AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING , broadcast3.getStatus());


		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
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

}
