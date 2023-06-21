package io.antmedia.test.console;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.AppSettings;
import io.antmedia.cluster.IClusterNotifier;
import io.antmedia.cluster.IClusterStore;
import io.antmedia.console.AdminApplication;
import io.antmedia.console.datastore.ConsoleDataStoreFactory;
import io.antmedia.console.datastore.MapDBStore;
import io.antmedia.console.rest.CommonRestService;
import io.antmedia.console.rest.RestServiceV2;
import io.antmedia.datastore.db.DataStoreFactory;
import io.antmedia.datastore.db.types.User;
import io.antmedia.licence.ILicenceService;
import io.antmedia.rest.BroadcastRestService;
import io.antmedia.rest.model.Result;
import io.antmedia.rest.model.UserType;
import io.antmedia.settings.ServerSettings;
import io.antmedia.statistic.IStatsCollector;
import io.antmedia.webrtc.api.IWebRTCAdaptor;
import io.vertx.core.Vertx;
import org.bytedeco.ffmpeg.global.avformat;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.junit.*;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.mapdb.elsa.ElsaSerializerBase;
import org.mockito.Mockito;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.WebApplicationContext;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;
import static org.junit.Assert.assertNotNull;


public class ConsoleRestV2UnitTest {

	private RestServiceV2 restService;
	private MapDBStore dbStore;
	private Vertx vertx;

	private static final String USER_PASSWORD = "user.password";

	public static final String USER_EMAIL = "user.email";

	public static final String IS_AUTHENTICATED = "isAuthenticated";

	private static final int OFFSET_NOT_USED = -1;
	private static final int MAX_OFFSET_SIZE = 10000000;
	private static final int MAX_CHAR_SIZE = 512000;
	private static final int MIN_CHAR_SIZE = 10;
	private static final int MIN_OFFSET_SIZE = 10;
	private static final String LOG_CONTENT = "logContent";
	private static final String LOG_SIZE = "logSize";
	private static final String LOG_CONTENT_RANGE = "logContentRange";
	private static final float MEGABYTE = 1024f * 1024f;
	private static final String MB_STRING = "%.2f MB";
	private static final String LOG_TYPE_TEST = "test";
	private static final String LOG_TYPE_RANDOM = "random";
	private static final String TEST_LOG_LOCATION = "target/test-classes/ant-media-server.log";
	private static final String FILE_NOT_EXIST = "There are no registered logs yet";
	private static final String CREATED_FILE_TEXT = "2019-04-24 19:01:24,291 [main] INFO  org.red5.server.Launcher - Ant Media Server Enterprise 1.7.0-SNAPSHOT\n" +
			"2019-04-24 19:01:24,334 [main] INFO  o.s.c.s.FileSystemXmlApplicationContext - Refreshing org.springframework.context.support.FileSystemXmlApplicationContext@f0f2775: startup date [Wed Apr 24 19:01:24 EET 2019]; root of context hierarchy";


	@Before
	public void before() {
		File f = new File("server.db");
		if (f.exists()) {
			try {
				Files.delete(f.toPath());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		restService = new RestServiceV2();
		vertx = Vertx.vertx();
		dbStore = new MapDBStore(vertx);
		restService.setDataStore(dbStore);
	}

	@After
	public void after() {
		// dbStore.clear();
		dbStore.close();
		vertx.close();

		File f = new File("server.db");
		if (f.exists()) {
			try {
				Files.delete(f.toPath());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	@Test
	public void getUserList(){
		String password = "password";
		String userName = "username" + (int) (Math.random() * 1000000000);
		User user = new User(userName, password, UserType.ADMIN, "all");
		Result result = restService.addUser(user);

		// System.out.println("error id: " + result.errorId);
		assertTrue(result.isSuccess());
		assertEquals(1, restService.getUserList().size());

		assertNotNull(restService.getUserList());

		userName = "username" + (int) (Math.random() * 1000000000);
		user = new User(userName, "second pass", UserType.ADMIN, "all");

		user.setPassword("second pass");
		user.setUserType(UserType.ADMIN);
		result = restService.addUser(user);
		assertEquals(2, restService.getUserList().size());
	}


	@Test
	public void testAddUser() {

		String password = "password";
		String userName = "username" + (int) (Math.random() * 1000000000);
		User user = new User(userName, password, UserType.ADMIN, "system");
		Result result = restService.addUser(user);

		// System.out.println("error id: " + result.errorId);
		assertTrue(result.isSuccess());

		String userName2 = "username" + (int) (Math.random() * 1000000000);

		user = new User(userName2, "second pass", UserType.ADMIN, "system");

		user.setPassword("second pass");
		user.setUserType(UserType.READ_ONLY);
		result = restService.addUser(user);

		assertTrue(result.isSuccess());

		user = new User(userName, "second pass", UserType.ADMIN, "system");

		user.setPassword("second pass");
		user.setUserType(UserType.ADMIN);
		result = restService.addUser(user);

		assertFalse(result.isSuccess());

		user = new User(userName, "second pass", UserType.ADMIN, "system");

		user.setPassword("second pass");
		user.setUserType(UserType.READ_ONLY);
		result = restService.addUser(user);

		assertFalse(result.isSuccess());

		user.setEmail("ksks" + (int) (Math.random() * 1000000000));
		user.setPassword("second pass");
		user.setUserType(UserType.ADMIN);
		result = restService.addUser(user);
		assertTrue(result.isSuccess());

		result = restService.addUser(null);

		assertFalse(result.isSuccess());
	}

	private volatile boolean err;

	@Test
	public void testMultipleThreads() {

		Thread thread = null;
		err = false;
		for (int i = 0; i < 10; i++) {
			thread = new Thread() {
				public void run() {

					for (int i = 0; i < 20; i++) {
						try {
							testAddUser();
						} catch (Exception e) {
							e.printStackTrace();
							System.err.println("error--------");
							// fail(e.getMessage());
							err = true;
						} catch (AssertionError error) {
							error.printStackTrace();
							System.err.println("assertion error: " + error);
							// fail(error.getMessage());
							err = true;

						}
					}

				};
			};
			thread.start();
		}

		try {
			/*
			 * while (thread.isAlive()) { Thread.sleep(1000); }
			 */
			thread.join();
			assertFalse(err);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}

	@Test
	public void testSendInfo() {
		RestServiceV2 restServiceSpy = Mockito.spy(restService);

		Mockito.doReturn(new ServerSettings()).when(restServiceSpy).getServerSettings();

		boolean sendUserInfo = restServiceSpy.sendUserInfo("test@antmedia.io", "firstname", "lastname", "scope", "admin");
		assertTrue(sendUserInfo);
	}

	@Test
	public void testGetStatsCollector() {
		RestServiceV2 restServiceSpy = Mockito.spy(restService);
		Mockito.doReturn(null).when(restServiceSpy).getContext();

		assertNull(restServiceSpy.getStatsCollector());
		WebApplicationContext context = Mockito.mock(WebApplicationContext.class);
		Mockito.doReturn(context).when(restServiceSpy).getContext();
		Mockito.when(context.getBean(Mockito.anyString())).thenReturn(Mockito.mock(IStatsCollector.class));

		assertNotNull(restServiceSpy.getStatsCollector());
		assertNotNull(restServiceSpy.getStatsCollector());

	}

	@Test
	public void testGetServerSettingsInternal() {
		RestServiceV2 restServiceSpy = Mockito.spy(restService);
		Mockito.doReturn(null).when(restServiceSpy).getContext();

		assertNull(restServiceSpy.getServerSettingsInternal());
		WebApplicationContext context = Mockito.mock(WebApplicationContext.class);
		Mockito.doReturn(context).when(restServiceSpy).getContext();
		Mockito.when(context.getBean(Mockito.anyString())).thenReturn(Mockito.mock(ServerSettings.class));

		assertNotNull(restServiceSpy.getServerSettingsInternal());
		assertNotNull(restServiceSpy.getServerSettingsInternal());

	}

	@Test
	public void testGetLicenseService() {
		RestServiceV2 restServiceSpy = Mockito.spy(restService);
		Mockito.doReturn(null).when(restServiceSpy).getContext();

		assertNull(restServiceSpy.getLicenceServiceInstance());
		WebApplicationContext context = Mockito.mock(WebApplicationContext.class);
		Mockito.doReturn(context).when(restServiceSpy).getContext();
		Mockito.when(context.getBean(Mockito.anyString())).thenReturn(Mockito.mock(ILicenceService.class));

		assertNotNull(restServiceSpy.getLicenceServiceInstance());
		assertNotNull(restServiceSpy.getLicenceServiceInstance());

	}

	@Test
	public void testGetApplication() {
		RestServiceV2 restServiceSpy = Mockito.spy(restService);
		Mockito.doReturn(null).when(restServiceSpy).getContext();

		assertNull(restServiceSpy.getApplication());
		WebApplicationContext context = Mockito.mock(WebApplicationContext.class);
		Mockito.doReturn(context).when(restServiceSpy).getContext();
		Mockito.when(context.getBean(Mockito.anyString())).thenReturn(Mockito.mock(AdminApplication.class));

		assertNotNull(restServiceSpy.getApplication());
		assertNotNull(restServiceSpy.getApplication());

	}

	@Test
	public void testDataStoreFactory() {
		RestServiceV2 restServiceSpy = Mockito.spy(restService);
		Mockito.doReturn(null).when(restServiceSpy).getContext();

		assertNull(restServiceSpy.getDataStoreFactory());
		WebApplicationContext context = Mockito.mock(WebApplicationContext.class);
		Mockito.doReturn(context).when(restServiceSpy).getContext();
		Mockito.when(context.getBean(Mockito.anyString())).thenReturn(Mockito.mock(ConsoleDataStoreFactory.class));

		assertNotNull(restServiceSpy.getDataStoreFactory());
		assertNotNull(restServiceSpy.getDataStoreFactory());

	}

	@Test
	public void testIsClusterMode() {
		RestServiceV2 restServiceSpy = Mockito.spy(restService);

		Mockito.doReturn(null).when(restServiceSpy).getContext();
		assertFalse(restServiceSpy.isClusterMode());

		WebApplicationContext context = Mockito.mock(WebApplicationContext.class);
		Mockito.doReturn(context).when(restServiceSpy).getContext();
		Mockito.when(context.containsBean(Mockito.anyString())).thenReturn(true);
		assertTrue(restServiceSpy.isClusterMode());

	}

	@Test
	public void testUploadApplication(){
		FileInputStream inputStream;
		try{
			inputStream = new FileInputStream("src/test/resources/sample_MP4_480.mp4");
			String tmpsDirectory = System.getProperty("java.io.tmpdir");
			if (!tmpsDirectory.endsWith("/")) {
				tmpsDirectory += "/";
			}

			{
				RestServiceV2 restServiceSpy = Mockito.spy(restService);

				List<String> apps = new ArrayList<>();
				apps.add("tahirrrr");
				AdminApplication adminApp = Mockito.mock(AdminApplication.class);
				Mockito.when(adminApp.getApplications()).thenReturn(apps);

				Mockito.doReturn(adminApp).when(restServiceSpy).getApplication();
				Mockito.doReturn(false).when(restServiceSpy).isClusterMode();

				restServiceSpy.createApplication("taso", inputStream);

				Mockito.verify(adminApp).createApplication("taso",tmpsDirectory + "taso.war");
			}

			{
				RestServiceV2 restServiceSpy = Mockito.spy(restService);

				List<String> apps = new ArrayList<>();
				apps.add("tahirrrr");
				AdminApplication adminApp = Mockito.mock(AdminApplication.class);
				Mockito.when(adminApp.getApplications()).thenReturn(apps);

				Mockito.doReturn(adminApp).when(restServiceSpy).getApplication();
				Mockito.doReturn(false).when(restServiceSpy).isClusterMode();

				restServiceSpy.createApplication("taso", null);

				Mockito.verify(adminApp).createApplication("taso", null);
			}


			{
				RestServiceV2 restServiceSpy = Mockito.spy(restService);

				List<String> apps = new ArrayList<>();
				apps.add("LiveApp");
				AdminApplication adminApp = Mockito.mock(AdminApplication.class);
				Mockito.when(adminApp.getApplications()).thenReturn(apps);

				Mockito.doReturn(adminApp).when(restServiceSpy).getApplication();
				Mockito.doReturn(false).when(restServiceSpy).isClusterMode();

				restServiceSpy.createApplication("LiveApp", inputStream);

				Mockito.verify(adminApp, Mockito.never()).createApplication("LiveApp", "LiveApp.war");
			}

			{
				RestServiceV2 restServiceSpy = Mockito.spy(restService);
				ServerSettings settings = new ServerSettings();

				List<String> apps = new ArrayList<>();
				apps.add("tahirrrr");
				AdminApplication adminApp = Mockito.mock(AdminApplication.class);
				Mockito.when(adminApp.getApplications()).thenReturn(apps);
				IClusterNotifier clusterNotifier = Mockito.mock(IClusterNotifier.class);
				IClusterStore clusterStore = Mockito.mock(IClusterStore.class);

				Mockito.when(adminApp.getClusterNotifier()).thenReturn(clusterNotifier);
				Mockito.when(clusterNotifier.getClusterStore()).thenReturn(clusterStore);
				Mockito.when(clusterStore.saveSettings(Mockito.any())).thenReturn(true);



				Mockito.doReturn(adminApp).when(restServiceSpy).getApplication();
				Mockito.doReturn(true).when(restServiceSpy).isClusterMode();
				Mockito.doReturn(settings).when(restServiceSpy).getServerSettings();

				restServiceSpy.createApplication("taso", inputStream);

				Mockito.verify(adminApp).createApplication("taso", tmpsDirectory + "taso.war");
			}

			{
				RestServiceV2 restServiceSpy = Mockito.spy(restService);

				List<String> apps = new ArrayList<>();
				apps.add("LiveApp");
				AdminApplication adminApp = Mockito.mock(AdminApplication.class);
				Mockito.when(adminApp.getApplications()).thenReturn(apps);

				Mockito.doReturn(adminApp).when(restServiceSpy).getApplication();
				Mockito.doReturn(false).when(restServiceSpy).isClusterMode();

				restServiceSpy.createApplication("*_?", inputStream).isSuccess();

				Mockito.verify(adminApp, Mockito.never()).createApplication("*_?", "*_?.war");
			}
		}
		catch(Exception e){

			e.printStackTrace();
			fail(e.getMessage());
		}


	}

	@Test
	public void testChangePassword() {

		String password = "password";
		String userName = "username" + (int) (Math.random() * 100000);
		User user = new User(userName, password, UserType.ADMIN, "all");

		HttpSession session = Mockito.mock(HttpSession.class);
		Mockito.when(session.getAttribute(IS_AUTHENTICATED)).thenReturn(true);
		Mockito.when(session.getAttribute(USER_EMAIL)).thenReturn(userName);
		Mockito.when(session.getAttribute(USER_PASSWORD)).thenReturn(password);

		HttpServletRequest mockRequest = Mockito.mock(HttpServletRequest.class);

		Mockito.when(mockRequest.getSession()).thenReturn(session);

		restService.setRequestForTest(mockRequest);

		Result result = restService.addInitialUser(user);
		assertTrue(result.isSuccess());
		assertEquals(restService.getMD5Hash(password), dbStore.getUser(userName).getPassword());
		assertEquals(UserType.ADMIN, dbStore.getUser(userName).getUserType());

		//Change password tests
		user.setNewPassword("password2");
		Result result2 = restService.changeUserPasswordInternal(userName, user);
		assertTrue(result2.isSuccess());

		assertEquals(restService.getMD5Hash("password2"), dbStore.getUser(userName).getPassword());

		user.setPassword("password2");
		user.setNewPassword("12345");
		result2 = restService.changeUserPasswordInternal(userName, user);
		assertTrue(result2.isSuccess());

		assertEquals(restService.getMD5Hash("12345"), dbStore.getUser(userName).getPassword());

		//Does not exist with pass
		result2 = restService.changeUserPasswordInternal(userName, user);
		System.out.println(result2.getMessage());
		assertFalse(result2.isSuccess());

		//Does not exist with username
		user.setPassword(user.getNewPassword());
		result2 = restService.changeUserPasswordInternal("test", user);
		System.out.println(result2.getMessage());
		assertFalse(result2.isSuccess());

		//No new password
		user = new User(userName, "12345", UserType.ADMIN, "all");
		result2 = restService.changeUserPasswordInternal(userName, user);
		System.out.println(result2.getMessage());
		assertFalse(result2.isSuccess());

	}
	@Test
	public void testEditUser(){

		String password = "password";
		String userName = "username" + (int) (Math.random() * 100000);
		User user = new User(userName, password, UserType.ADMIN, "system");

		HttpSession session = Mockito.mock(HttpSession.class);
		Mockito.when(session.getAttribute(IS_AUTHENTICATED)).thenReturn(true);
		Mockito.when(session.getAttribute(USER_EMAIL)).thenReturn(userName);
		Mockito.when(session.getAttribute(USER_PASSWORD)).thenReturn(password);

		HttpServletRequest mockRequest = Mockito.mock(HttpServletRequest.class);

		Mockito.when(mockRequest.getSession()).thenReturn(session);

		restService.setRequestForTest(mockRequest);

		Result result = restService.addInitialUser(user);
		assertTrue(result.isSuccess());
		assertEquals(restService.getMD5Hash(password), dbStore.getUser(userName).getPassword());
		assertEquals(UserType.ADMIN, dbStore.getUser(userName).getUserType());

		//Add second user
		String password2 = "password2";
		String userName2 = "username" + (int) (Math.random() * 100000);
		User user2 = new User(userName2, password2, UserType.READ_ONLY, "system");

		result = restService.addUser(user2);
		assertTrue(result.isSuccess());

		//Change User type as another user
		user2.setUserType(UserType.ADMIN);
		result = restService.editUser(user2);
		assertTrue(result.isSuccess());
		assertEquals(user2.getUserType(), dbStore.getUser(userName2).getUserType());

		//Change password as another user
		user2.setNewPassword("password2");
		result = restService.editUser(user2);
		assertTrue(result.isSuccess());

		assertEquals(restService.getMD5Hash("password2"), dbStore.getUser(userName2).getPassword());

		//Null check
		result = restService.editUser(null);
		assertFalse(result.isSuccess());
	}

	@Test
	public void testDeleteUser() {
		String password = "password";
		String userName = "username" + (int) (Math.random() * 100000);
		User user = new User(userName, password, UserType.ADMIN, "all");

		HttpSession session = Mockito.mock(HttpSession.class);
		Mockito.when(session.getAttribute(IS_AUTHENTICATED)).thenReturn(true);
		Mockito.when(session.getAttribute(USER_EMAIL)).thenReturn(userName);
		Mockito.when(session.getAttribute(USER_PASSWORD)).thenReturn(password);

		HttpServletRequest mockRequest = Mockito.mock(HttpServletRequest.class);

		Mockito.when(mockRequest.getSession()).thenReturn(session);

		restService.setRequestForTest(mockRequest);

		Result result = restService.addUser(user);
		assertTrue(result.isSuccess());
		assertNotNull(dbStore.getUser(userName));

		String userName2 = "username" + (int) (Math.random() * 100000);
		User user2 = new User(userName2, password, UserType.READ_ONLY, "all");

		//Trying to delete a non existant user
		result = restService.deleteUser(userName2);
		assertFalse(result.isSuccess());
		assertNull(dbStore.getUser(userName2));

		//Add user2 and delete
		result = restService.addUser(user2);
		assertTrue(result.isSuccess());
		assertNotNull(dbStore.getUser(userName));

		result = restService.deleteUser(userName2);
		System.out.println(result.getMessage());
		assertTrue(result.isSuccess());
		assertNull(dbStore.getUser(userName2));

	}

	@Test
	public void testDeleteApplication() {

		RestServiceV2 restServiceSpy = Mockito.spy(restService);

		AntMediaApplicationAdapter adapter = Mockito.mock(AntMediaApplicationAdapter.class);
		Mockito.doReturn(adapter).when(restServiceSpy).getAppAdaptor(Mockito.any());
		Mockito.when(adapter.getAppSettings()).thenReturn(Mockito.mock(AppSettings.class));

		AdminApplication adminApp = Mockito.mock(AdminApplication.class);

		Mockito.doReturn(adminApp).when(restServiceSpy).getApplication();
		Mockito.doReturn("").when(restServiceSpy).changeSettings(Mockito.any(), Mockito.any());
		Mockito.doReturn(false).when(restServiceSpy).isClusterMode();

		Result result = restServiceSpy.deleteApplication("test", true);
		assertFalse(result.isSuccess());


		Mockito.when(adminApp.deleteApplication(Mockito.anyString(),Mockito.eq(true))).thenReturn(true);
		result = restServiceSpy.deleteApplication("test", true);
		assertTrue(result.isSuccess());


		Mockito.doReturn(null).when(restServiceSpy).getAppAdaptor(Mockito.any());
		result = restServiceSpy.deleteApplication("test", true);
		assertFalse(result.isSuccess());
		
		
		result = restServiceSpy.deleteApplication("test??", true);
		assertFalse(result.isSuccess()); //because there is invalid character

	}

	@Test
	public void testLiveness() {
		RestServiceV2 restServiceSpy = Mockito.spy(restService);

		Response liveness = restServiceSpy.liveness();
		assertEquals(Status.OK.getStatusCode(), liveness.getStatus());

		Mockito.doReturn(null).when(restServiceSpy).getHostname();

		liveness = restServiceSpy.liveness();
		assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), liveness.getStatus());
	}

	@Test
	public void testResetBroadcast() {
		RestServiceV2 restServiceSpy = Mockito.spy(restService);

		AntMediaApplicationAdapter adapter = Mockito.mock(AntMediaApplicationAdapter.class);
		AdminApplication adminApp = Mockito.mock(AdminApplication.class);
		Mockito.doReturn(adminApp).when(restServiceSpy).getApplication();

		restServiceSpy.resetBroadcast("junit");
		Mockito.verify(adapter, Mockito.never()).resetBroadcasts();

		ApplicationContext appContext = Mockito.mock(ApplicationContext.class);
		Mockito.when(adminApp.getApplicationContext(Mockito.any())).thenReturn(appContext);
		restServiceSpy.resetBroadcast("junit");
		Mockito.verify(adapter, Mockito.never()).resetBroadcasts();


		Mockito.when(appContext.getBean(Mockito.anyString())).thenReturn(adapter);
		restServiceSpy.resetBroadcast("junit");
		Mockito.verify(adapter).resetBroadcasts();


	}

	@Test
	public void testShutDownStatus() {
		RestServiceV2 restServiceSpy = Mockito.spy(restService);
		AntMediaApplicationAdapter adaptor = Mockito.mock(AntMediaApplicationAdapter.class);

		Mockito.doReturn(adaptor).when(restServiceSpy).getAppAdaptor("app1");
		Mockito.doReturn(null).when(restServiceSpy).getAppAdaptor("app2");
		restServiceSpy.setShutdownStatus("app1,app2");

		Mockito.verify(adaptor).setShutdownProperly(true);
	}

	@Test
	public void testExtractFQDN() {
		String domain = CommonRestService.extractFQDN("http://example.com/path/to/page.html");
		assertEquals("example.com", domain);

		domain = CommonRestService.extractFQDN("https://www.subdomain.example.com");
		assertEquals("www.subdomain.example.com", domain);


		domain = CommonRestService.extractFQDN("ftp://ftp.example.com/files");
		assertEquals("ftp.example.com", domain);

		domain = CommonRestService.extractFQDN("wss://www.subdomain.example.com");
		assertEquals("www.subdomain.example.com", domain);

		domain = CommonRestService.extractFQDN("ws://www.subdomain.example.com");
		assertEquals("www.subdomain.example.com", domain);

		domain = CommonRestService.extractFQDN("");
		assertNull(domain);
		domain = CommonRestService.extractFQDN(null);
		assertNull(domain);
	}

	@Test
	public void testConfigureSSL() 
	{
		RestServiceV2 restServiceSpy = Mockito.spy(restService);
		AdminApplication adminApp = Mockito.mock(AdminApplication.class);
		Mockito.doReturn(adminApp).when(restServiceSpy).getApplication();
		Mockito.when(adminApp.runCommand(Mockito.anyString())).thenReturn(true);

		Result result = restServiceSpy.configureSsl(null, null, null, null, null, null, null, null);
		assertFalse(result.isSuccess());
		Mockito.verify(adminApp, Mockito.never()).runCommand(Mockito.anyString());

		result = restServiceSpy.configureSsl(null, "", null, null, null, null, null, null);
		assertFalse(result.isSuccess());
		Mockito.verify(adminApp, Mockito.never()).runCommand(Mockito.anyString());

		result = restServiceSpy.configureSsl("", "", null, null, null, null, null, null);
		assertFalse(result.isSuccess());
		Mockito.verify(adminApp, Mockito.never()).runCommand(Mockito.anyString());
		
		result = restServiceSpy.configureSsl("example.com", "", null, null, null, null, null, null);
		assertFalse(result.isSuccess());
		Mockito.verify(adminApp, Mockito.never()).runCommand(Mockito.anyString());


		result = restServiceSpy.configureSsl("", "ANTMEDIA_SUBDOMAIN", null, null, null, null, null, null);
		assertTrue(result.isSuccess());
		Mockito.verify(adminApp, Mockito.times(1)).runCommand(Mockito.anyString());


		result = restServiceSpy.configureSsl("", "CUSTOM_DOMAIN", null, null, null, null, null, null);
		assertFalse(result.isSuccess());
		Mockito.verify(adminApp, Mockito.times(1)).runCommand(Mockito.anyString());

		result = restServiceSpy.configureSsl("http://example.com", "CUSTOM_DOMAIN", null, null, null, null, null, null);
		assertTrue(result.isSuccess());
		Mockito.verify(adminApp, Mockito.times(2)).runCommand(Mockito.anyString());

		//ignores the given domain name
		result = restServiceSpy.configureSsl("http://example.com", "ANTMEDIA_SUBDOMAIN", null, null, null, null, null, null);
		assertTrue(result.isSuccess());
		Mockito.verify(adminApp, Mockito.times(3)).runCommand(Mockito.anyString());


		result = restServiceSpy.configureSsl("http://example.com", "CUSTOM_CERTIFICATE", null, null, null, null, null, null);
		assertFalse(result.isSuccess());
		Mockito.verify(adminApp, Mockito.times(3)).runCommand(Mockito.anyString());

		try {
				
			InputStream fullChainInputStream = new FileInputStream("src/test/resources/test.properties");

			result = restServiceSpy.configureSsl("http://example.com", "CUSTOM_CERTIFICATE", fullChainInputStream, null, null, null, null, null);
			assertFalse(result.isSuccess());
			Mockito.verify(adminApp, Mockito.times(3)).runCommand(Mockito.anyString());
			
			FormDataContentDisposition fullChainFileContent = Mockito.mock(FormDataContentDisposition.class);
			Mockito.when(fullChainFileContent.getFileName()).thenReturn(null);
			result = restServiceSpy.configureSsl("http://example.com", "CUSTOM_CERTIFICATE", fullChainInputStream, fullChainFileContent, null, null, null, null);
			assertFalse(result.isSuccess());
			Mockito.verify(adminApp, Mockito.times(3)).runCommand(Mockito.anyString());
			
			Mockito.when(fullChainFileContent.getFileName()).thenReturn("");
			result = restServiceSpy.configureSsl("http://example.com", "CUSTOM_CERTIFICATE", fullChainInputStream, fullChainFileContent, null, null, null, null);
			assertFalse(result.isSuccess());
			Mockito.verify(adminApp, Mockito.times(3)).runCommand(Mockito.anyString());
			
			Mockito.when(fullChainFileContent.getFileName()).thenReturn("fullchain.pem");
			result = restServiceSpy.configureSsl("http://example.com", "CUSTOM_CERTIFICATE", fullChainInputStream, fullChainFileContent, null, null, null, null);
			assertFalse(result.isSuccess());
			Mockito.verify(adminApp, Mockito.times(3)).runCommand(Mockito.anyString());
			
			
			//private key file
			InputStream privateKeyFileInputStream = new FileInputStream("src/test/resources/test.properties");

			result = restServiceSpy.configureSsl("http://example.com", "CUSTOM_CERTIFICATE", fullChainInputStream, fullChainFileContent, privateKeyFileInputStream, null, null, null);
			assertFalse(result.isSuccess());
			Mockito.verify(adminApp, Mockito.times(3)).runCommand(Mockito.anyString());
			
			FormDataContentDisposition privateFileContent = Mockito.mock(FormDataContentDisposition.class);
			Mockito.when(privateFileContent.getFileName()).thenReturn(null);
			result = restServiceSpy.configureSsl("http://example.com", "CUSTOM_CERTIFICATE", fullChainInputStream, fullChainFileContent, privateKeyFileInputStream, privateFileContent, null, null);
			assertFalse(result.isSuccess());
			Mockito.verify(adminApp, Mockito.times(3)).runCommand(Mockito.anyString());
			
			Mockito.when(privateFileContent.getFileName()).thenReturn("");
			result = restServiceSpy.configureSsl("http://example.com", "CUSTOM_CERTIFICATE", fullChainInputStream, fullChainFileContent, privateKeyFileInputStream, privateFileContent, null, null);
			assertFalse(result.isSuccess());
			Mockito.verify(adminApp, Mockito.times(3)).runCommand(Mockito.anyString());
			
			Mockito.when(privateFileContent.getFileName()).thenReturn("fullchain.pem");
			result = restServiceSpy.configureSsl("http://example.com", "CUSTOM_CERTIFICATE", fullChainInputStream, fullChainFileContent, privateKeyFileInputStream, privateFileContent, null, null);
			assertFalse(result.isSuccess());
			Mockito.verify(adminApp, Mockito.times(3)).runCommand(Mockito.anyString());
		
			
			//chain file
			
			InputStream chainFileInputStream = new FileInputStream("src/test/resources/test.properties");

			result = restServiceSpy.configureSsl("http://example.com", "CUSTOM_CERTIFICATE", fullChainInputStream, fullChainFileContent, privateKeyFileInputStream, privateFileContent, chainFileInputStream, null);
			assertFalse(result.isSuccess());
			Mockito.verify(adminApp, Mockito.times(3)).runCommand(Mockito.anyString());
			
			FormDataContentDisposition chainFileContent = Mockito.mock(FormDataContentDisposition.class);
			Mockito.when(chainFileContent.getFileName()).thenReturn(null);
			result = restServiceSpy.configureSsl("http://example.com", "CUSTOM_CERTIFICATE", fullChainInputStream, fullChainFileContent, privateKeyFileInputStream, privateFileContent, chainFileInputStream, chainFileContent);
			assertFalse(result.isSuccess());
			Mockito.verify(adminApp, Mockito.times(3)).runCommand(Mockito.anyString());
			
			Mockito.when(chainFileContent.getFileName()).thenReturn("");
			result = restServiceSpy.configureSsl("http://example.com", "CUSTOM_CERTIFICATE", fullChainInputStream, fullChainFileContent, privateKeyFileInputStream, privateFileContent, chainFileInputStream, chainFileContent);
			assertFalse(result.isSuccess());
			Mockito.verify(adminApp, Mockito.times(3)).runCommand(Mockito.anyString());
			
			Mockito.when(chainFileContent.getFileName()).thenReturn("fullchain.pem");
			result = restServiceSpy.configureSsl("http://example.com", "CUSTOM_CERTIFICATE", fullChainInputStream, fullChainFileContent, privateKeyFileInputStream, privateFileContent, chainFileInputStream, chainFileContent);
			assertTrue(result.isSuccess());
			Mockito.verify(adminApp, Mockito.times(4)).runCommand(Mockito.anyString());
			
			
			

		} catch (FileNotFoundException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}


	}

}
