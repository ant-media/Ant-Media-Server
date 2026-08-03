package io.antmedia.test.console;

import com.google.gson.JsonObject;
import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.AppSettings;
import io.antmedia.cluster.IClusterNotifier;
import io.antmedia.cluster.IClusterStore;
import io.antmedia.console.AdminApplication;
import io.antmedia.console.datastore.ConsoleDataStoreFactory;
import io.antmedia.console.datastore.MapDBStore;
import io.antmedia.console.rest.CommonRestService;
import io.antmedia.console.rest.RestServiceV2;
import io.antmedia.console.rest.SupportRequest;
import io.antmedia.console.rest.SupportRestService;
import io.antmedia.datastore.db.types.User;
import io.antmedia.datastore.db.types.UserType;
import io.antmedia.licence.ILicenceService;
import io.antmedia.rest.model.Result;
import io.antmedia.settings.ServerSettings;
import io.antmedia.statistic.IStatsCollector;
import io.antmedia.statistic.StatsCollector;
import io.vertx.core.Vertx;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import org.awaitility.Awaitility;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.junit.Rule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.mockito.ArgumentCaptor;
import org.red5.server.api.scope.IScope;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.WebApplicationContext;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;


public class ConsoleRestV2UnitTest {

	private RestServiceV2 restService;
	private MapDBStore dbStore;
	private Vertx vertx;

	private static final String USER_PASSWORD = "user.password";

	public static final String USER_EMAIL = "user.email";

	public static final String IS_AUTHENTICATED = "isAuthenticated";

	@Rule
	public TestRule watcher = new TestWatcher() {
		@Override
		protected void starting(Description description) {
			System.out.println("Starting test: " + description.getMethodName());
		}
		@Override
		protected void failed(Throwable e, Description description) {
			System.out.println("Failed test: " + description.getMethodName());
		}
		@Override
		protected void finished(Description description) {
			System.out.println("Finishing test: " + description.getMethodName());
		}
	};
	
	@BeforeEach
	void before() {
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

	@AfterEach
	void after() {
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
	void getUserList(){
		String password = "password";
		String userName = "username" + (int) (Math.random() * 1000000000);
		User user = new User(userName, password, UserType.ADMIN, "all", null);
		RestServiceV2 restServiceSpy = spy(restService);
		doReturn(new ServerSettings()).when(restServiceSpy).getServerSettings();

		Result result = restServiceSpy.addUser(user);

		assertTrue(result.isSuccess());
		assertEquals(1, restServiceSpy.getUserList().size());

		assertNotNull(restServiceSpy.getUserList());

		userName = "username" + (int) (Math.random() * 1000000000);
		user = new User(userName, "second pass", UserType.ADMIN, "all", null);

		user.setPassword("second pass");
		user.setUserType(UserType.ADMIN);
		result = restServiceSpy.addUser(user);
		assertEquals(2, restServiceSpy.getUserList().size());
	}


	@Test
	void testAddUser() {

		String password = "password";
		String userName = "username" + (int) (Math.random() * 1000000000);
		User user = new User(userName, password, UserType.ADMIN, "system", new HashMap<String, String>());
		RestServiceV2 restServiceSpy = spy(restService);
		doReturn(new ServerSettings()).when(restServiceSpy).getServerSettings();

		
		Result result = restServiceSpy.addUser(user);

		assertTrue(result.isSuccess());

		String userName2 = "username" + (int) (Math.random() * 1000000000);

		user = new User(userName2, "second pass", UserType.ADMIN, "system", new HashMap<String, String>());

		user.setPassword("second pass");
		user.setUserType(UserType.READ_ONLY);
		result = restServiceSpy.addUser(user);

		assertTrue(result.isSuccess());

		user = new User(userName, "second pass", UserType.ADMIN, "system", new HashMap<String, String>());

		user.setPassword("second pass");
		user.setUserType(UserType.ADMIN);
		result = restServiceSpy.addUser(user);

		assertFalse(result.isSuccess());

		user = new User(userName, "second pass", UserType.ADMIN, "system", new HashMap<String, String>());

		user.setPassword("second pass");
		user.setUserType(UserType.READ_ONLY);
		result = restServiceSpy.addUser(user);

		assertFalse(result.isSuccess());

		user.setEmail("ksks" + (int) (Math.random() * 1000000000));
		user.setPassword("second pass");
		user.setUserType(UserType.ADMIN);
		result = restServiceSpy.addUser(user);
		assertTrue(result.isSuccess());

		result = restServiceSpy.addUser(null);

		assertFalse(result.isSuccess());
	}

	private volatile boolean err;

	@Test
	void testMultipleThreads() {

		Thread thread = null;
		err = false;
		for (int i = 0; i < 10; i++) {
			thread = new Thread() {
				@Override
				public void run() {

					for (int i = 0; i < 20; i++) {
						try {
							testAddUser();
						} catch (Exception e) {
							e.printStackTrace();
							System.err.println("error--------");
							err = true;
						} catch (AssertionError error) {
							error.printStackTrace();
							System.err.println("assertion error: " + error);
							err = true;

						}
					}

				}
			};
			thread.start();
		}

		try {
			thread.join();
			assertFalse(err);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}
	
	@Test
	void testSupportRequest() {
		SupportRestService supportRestService = spy(new SupportRestService());
		
		SupportRequest supportRequest = new SupportRequest();
		supportRequest.setEmail("fromci@gmail.com");
		supportRequest.setDescription("This is coming from CI to test this endpoint. You can delete this message");
		supportRequest.setTitle("Test Message");
		supportRequest.setName("ci antmedia");
		
		ServerSettings serverSettings = new ServerSettings();
		serverSettings.setLicenceKey("license-key");
		doReturn(serverSettings).when(supportRestService).getServerSettings();
		
		StatsCollector collector = new StatsCollector();
		doReturn(collector).when(supportRestService).getStatsCollector();
		
		Result result = supportRestService.sendSupportRequest(supportRequest);
		assertTrue(result.isSuccess());
	}

	@Test
	void testSendInfo() {
		RestServiceV2 restServiceSpy = spy(restService);

		doReturn(new ServerSettings()).when(restServiceSpy).getServerSettings();

		Awaitility.await().atMost(10, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
			HashMap<String,String> appNameUserTypeMap = new HashMap<>();
			appNameUserTypeMap.put("system", UserType.ADMIN.toString());
			return restServiceSpy.sendUserInfo("test@antmedia.io", "firstname", "lastname","system","admin", appNameUserTypeMap);
		});
	}

	@Test
	void testGetStatsCollector() {
		IStatsCollector statsCollector = mock(IStatsCollector.class);

		restService.setStatsCollector(statsCollector);

		assertEquals(statsCollector, restService.getStatsCollector());

	}

	@Test
	void testGetServerSettingsInternal() {
		ServerSettings serverSettings = mock(ServerSettings.class);

		restService.setServerSettings(serverSettings);

		assertEquals(serverSettings, restService.getServerSettingsInternal());
		assertEquals(serverSettings, restService.getServerSettings());

	}

	@Test
	void testGetLicenseService() {
		ILicenceService licenceService = mock(ILicenceService.class);
		assertNull(restService.getLicenceStatus());
		assertNull(restService.getLicenceStatus("licence-key"));

		restService.setLicenceService(Optional.of(licenceService));

		assertEquals(licenceService, restService.getLicenceServiceInstance());

	}

	@Test
	void testGetApplication() {
		AdminApplication application = mock(AdminApplication.class);

		restService.setApplication(application);

		assertEquals(application, restService.getApplication());

	}

	@Test
	void testDataStoreFactory() {
		ConsoleDataStoreFactory dataStoreFactory = mock(ConsoleDataStoreFactory.class);
		when(dataStoreFactory.getDataStore()).thenReturn(dbStore);

		restService.setDataStoreFactory(dataStoreFactory);

		assertEquals(dataStoreFactory, restService.getDataStoreFactory());
		assertEquals(dbStore, restService.getDataStore());

	}

	@Test
	void testIsClusterMode() {
		RestServiceV2 restServiceSpy = spy(restService);

		doReturn(null).when(restServiceSpy).getContext();
		assertFalse(restServiceSpy.isClusterMode());

		WebApplicationContext context = mock(WebApplicationContext.class);
		doReturn(context).when(restServiceSpy).getContext();
		when(context.containsBean(anyString())).thenReturn(true);
		assertTrue(restServiceSpy.isClusterMode());

	}
	

	@Test
	void testUploadApplication(){
		FileInputStream inputStream;
		try{
			inputStream = new FileInputStream("src/test/resources/sample_MP4_480.mp4");
			String tmpsDirectory = System.getProperty("java.io.tmpdir");
			if (!tmpsDirectory.endsWith("/")) {
				tmpsDirectory += "/";
			}

			{
				RestServiceV2 restServiceSpy = spy(restService);

				List<String> apps = new ArrayList<>();
				apps.add("tahirrrr");
				AdminApplication adminApp = mock(AdminApplication.class);
				when(adminApp.getApplications()).thenReturn(apps);
				IScope rootScope = mock(IScope.class);
				String appName = "taso";
				
				when(rootScope.getScope(appName)).thenReturn(mock(IScope.class));
				when(adminApp.getRootScope()).thenReturn(rootScope);

				doReturn(adminApp).when(restServiceSpy).getApplication();
				doReturn(false).when(restServiceSpy).isClusterMode();
				doReturn(mock(AppSettings.class)).when(restServiceSpy).getSettings(appName);
				doReturn("").when(restServiceSpy).changeSettings(anyString(), any());

				doReturn(false).when(restServiceSpy).isApplicationExists(appName);

				restServiceSpy.createApplication(appName, inputStream);

				verify(adminApp).createApplication(appName, tmpsDirectory + "taso.war");
			}
			
			{
				
				RestServiceV2 restServiceSpy = spy(restService);

				AdminApplication adminApp = mock(AdminApplication.class);
				IScope rootScope = mock(IScope.class);
				String appName = "taso";
				
				when(rootScope.getScope(appName)).thenReturn(mock(IScope.class));
				when(adminApp.getRootScope()).thenReturn(rootScope);
				doReturn(true).when(adminApp).createApplication(anyString(), anyString());

				doReturn(adminApp).when(restServiceSpy).getApplication();
				doReturn(false).when(restServiceSpy).isClusterMode();
				
				doReturn(false).when(restServiceSpy).isApplicationExists(appName);
				
				doReturn(mock(AppSettings.class)).when(restServiceSpy).getSettings(appName);
				doReturn("").when(restServiceSpy).changeSettings(anyString(), any());


				Result result = restServiceSpy.createApplication(appName, inputStream);

				assertTrue(result.isSuccess());
				verify(adminApp).createApplication(appName, tmpsDirectory + "taso.war");
				
				
				doReturn(true).when(adminApp).createApplication(anyString(), anyString());
				when(rootScope.getScope(appName)).thenReturn(null);
				result = restServiceSpy.createApplication(appName, inputStream);
				assertFalse(result.isSuccess());
				verify(adminApp, times(2)).createApplication(appName, tmpsDirectory + "taso.war");
				
			}

			{
				RestServiceV2 restServiceSpy = spy(restService);

				List<String> apps = new ArrayList<>();
				apps.add("tahirrrr");
				AdminApplication adminApp = mock(AdminApplication.class);
				when(adminApp.getApplications()).thenReturn(apps);

				doReturn(adminApp).when(restServiceSpy).getApplication();
				doReturn(false).when(restServiceSpy).isClusterMode();
				IScope rootScope = mock(IScope.class);
				String appName = "taso";
				
				when(rootScope.getScope(appName)).thenReturn(mock(IScope.class));
				when(adminApp.getRootScope()).thenReturn(rootScope);
				doReturn(false).when(restServiceSpy).isApplicationExists(appName);
				doReturn(mock(AppSettings.class)).when(restServiceSpy).getSettings(appName);
				doReturn("").when(restServiceSpy).changeSettings(anyString(), any());


				restServiceSpy.createApplication(appName, null);

				verify(adminApp).createApplication(appName, null);
			}


			{
				RestServiceV2 restServiceSpy = spy(restService);

				List<String> apps = new ArrayList<>();
				apps.add("LiveApp");
				AdminApplication adminApp = mock(AdminApplication.class);
				when(adminApp.getApplications()).thenReturn(apps);

				doReturn(adminApp).when(restServiceSpy).getApplication();
				doReturn(false).when(restServiceSpy).isClusterMode();
				String appName = "LiveApp";
				IScope rootScope = mock(IScope.class);
				when(rootScope.getScope(appName)).thenReturn(mock(IScope.class));
				when(adminApp.getRootScope()).thenReturn(rootScope);
				doReturn(false).when(restServiceSpy).isApplicationExists(appName);
				
				doReturn(mock(AppSettings.class)).when(restServiceSpy).getSettings(appName);
				doReturn("").when(restServiceSpy).changeSettings(anyString(), any());


				restServiceSpy.createApplication(appName, inputStream);

				verify(adminApp, never()).createApplication(appName, appName + ".war");
			}

			{
				RestServiceV2 restServiceSpy = spy(restService);
				ServerSettings settings = new ServerSettings();

				List<String> apps = new ArrayList<>();
				apps.add("tahirrrr");
				AdminApplication adminApp = mock(AdminApplication.class);
				when(adminApp.getApplications()).thenReturn(apps);
				IClusterNotifier clusterNotifier = mock(IClusterNotifier.class);
				IClusterStore clusterStore = mock(IClusterStore.class);

				when(adminApp.getClusterNotifier()).thenReturn(clusterNotifier);
				when(clusterNotifier.getClusterStore()).thenReturn(clusterStore);
				when(clusterStore.saveSettings(any())).thenReturn(true);



				doReturn(adminApp).when(restServiceSpy).getApplication();
				doReturn(true).when(restServiceSpy).isClusterMode();
				doReturn(settings).when(restServiceSpy).getServerSettings();
				var appName = "taso";
				IScope rootScope = mock(IScope.class);
				when(rootScope.getScope(appName)).thenReturn(mock(IScope.class));
				when(adminApp.getRootScope()).thenReturn(rootScope);
				doReturn(false).when(restServiceSpy).isApplicationExists(appName);
				
				doReturn(mock(AppSettings.class)).when(restServiceSpy).getSettings(appName);
				doReturn("").when(restServiceSpy).changeSettings(anyString(), any());

				

				restServiceSpy.createApplication(appName, inputStream);

				verify(adminApp).createApplication(appName, tmpsDirectory + appName + ".war");
			}

			{
				RestServiceV2 restServiceSpy = spy(restService);

				List<String> apps = new ArrayList<>();
				apps.add("LiveApp");
				AdminApplication adminApp = mock(AdminApplication.class);
				when(adminApp.getApplications()).thenReturn(apps);

				doReturn(adminApp).when(restServiceSpy).getApplication();
				doReturn(false).when(restServiceSpy).isClusterMode();

				restServiceSpy.createApplication("*_?", inputStream).isSuccess();

				verify(adminApp, never()).createApplication("*_?", "*_?.war");
			}
		}
		catch(Exception e){

			e.printStackTrace();
			fail(e.getMessage());
		}


	}

	@Test
	void testChangePassword() {

		String password = "password";
		String userName = "username" + (int) (Math.random() * 100000);
		User user = new User(userName, password, UserType.ADMIN, "all", null);

		HttpSession session = mock(HttpSession.class);
		when(session.getAttribute(IS_AUTHENTICATED)).thenReturn(true);
		when(session.getAttribute(USER_EMAIL)).thenReturn(userName);
		when(session.getAttribute(USER_PASSWORD)).thenReturn(password);

		HttpServletRequest mockRequest = mock(HttpServletRequest.class);

		when(mockRequest.getSession()).thenReturn(session);

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
		user = new User(userName, "12345", UserType.ADMIN, "all", null);
		result2 = restService.changeUserPasswordInternal(userName, user);
		System.out.println(result2.getMessage());
		assertFalse(result2.isSuccess());

	}
	@Test
	void testEditUser(){

		String password = "password";
		String userName = "username" + (int) (Math.random() * 100000);
		User user = new User(userName, password, UserType.ADMIN, "system", null);

		HttpSession session = mock(HttpSession.class);
		when(session.getAttribute(IS_AUTHENTICATED)).thenReturn(true);
		when(session.getAttribute(USER_EMAIL)).thenReturn(userName);
		when(session.getAttribute(USER_PASSWORD)).thenReturn(password);

		HttpServletRequest mockRequest = mock(HttpServletRequest.class);

		when(mockRequest.getSession()).thenReturn(session);

		restService.setRequestForTest(mockRequest);

		Result result = restService.addInitialUser(user);
		assertTrue(result.isSuccess());
		assertEquals(restService.getMD5Hash(password), dbStore.getUser(userName).getPassword());
		assertEquals(UserType.ADMIN, dbStore.getUser(userName).getUserType());

		//Add second user
		String password2 = "password2";
		String userName2 = "username" + (int) (Math.random() * 100000);
		User user2 = new User(userName2, password2, UserType.READ_ONLY, "system", new HashMap<String, String>());

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
	void testInvalidateSession() {
		HttpSession session = mock(HttpSession.class);
		HttpServletRequest mockRequest = mock(HttpServletRequest.class);

		when(mockRequest.getSession()).thenReturn(session);
		restService.setRequestForTest(mockRequest);

		
		restService.deleteSession();
		
		verify(session).invalidate();
		
		
	}

	@Test
	void testDeleteUser() {
		String password = "password";
		String userName = "username" + (int) (Math.random() * 100000);
		User user = new User(userName, password, UserType.ADMIN, "all", null);

		HttpSession session = mock(HttpSession.class);
		when(session.getAttribute(IS_AUTHENTICATED)).thenReturn(true);
		when(session.getAttribute(USER_EMAIL)).thenReturn(userName);
		when(session.getAttribute(USER_PASSWORD)).thenReturn(password);

		HttpServletRequest mockRequest = mock(HttpServletRequest.class);

		when(mockRequest.getSession()).thenReturn(session);

		restService.setRequestForTest(mockRequest);

		Result result = restService.addUser(user);
		assertTrue(result.isSuccess());
		assertNotNull(dbStore.getUser(userName));

		String userName2 = "username" + (int) (Math.random() * 100000);
		User user2 = new User(userName2, password, UserType.READ_ONLY, "all", null);

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
	void testDeleteApplication() {

		RestServiceV2 restServiceSpy = spy(restService);

		AntMediaApplicationAdapter adapter = mock(AntMediaApplicationAdapter.class);
		doReturn(adapter).when(restServiceSpy).getAppAdaptor(any());
		when(adapter.getAppSettings()).thenReturn(mock(AppSettings.class));

		AdminApplication adminApp = mock(AdminApplication.class);

		doReturn(adminApp).when(restServiceSpy).getApplication();
		doReturn("").when(restServiceSpy).changeSettings(any(), any());
		doReturn(false).when(restServiceSpy).isClusterMode();

		Result result = restServiceSpy.deleteApplication("test", true);
		assertFalse(result.isSuccess());


		when(adminApp.deleteApplication(anyString(), eq(true))).thenReturn(true);
		result = restServiceSpy.deleteApplication("test", true);
		assertTrue(result.isSuccess());


		doReturn(null).when(restServiceSpy).getAppAdaptor(any());
		result = restServiceSpy.deleteApplication("test", true);
		assertFalse(result.isSuccess());
		
		
		result = restServiceSpy.deleteApplication("test??", true);
		assertFalse(result.isSuccess()); //because there is invalid character

	}

	@Test
	void testLiveness() {
		RestServiceV2 restServiceSpy = spy(restService);

		Response liveness = restServiceSpy.liveness();
		assertEquals(Status.OK.getStatusCode(), liveness.getStatus());

		doReturn(null).when(restServiceSpy).getHostname();

		liveness = restServiceSpy.liveness();
		assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), liveness.getStatus());
	}

	@Test
	void testResetBroadcast() {
		RestServiceV2 restServiceSpy = spy(restService);

		AntMediaApplicationAdapter adapter = mock(AntMediaApplicationAdapter.class);
		AdminApplication adminApp = mock(AdminApplication.class);
		doReturn(adminApp).when(restServiceSpy).getApplication();

		restServiceSpy.resetBroadcast("junit");
		verify(adapter, never()).resetBroadcasts();

		ApplicationContext appContext = mock(ApplicationContext.class);
		when(adminApp.getApplicationContext(any())).thenReturn(appContext);
		restServiceSpy.resetBroadcast("junit");
		verify(adapter, never()).resetBroadcasts();


		when(appContext.getBean(anyString())).thenReturn(adapter);
		restServiceSpy.resetBroadcast("junit");
		verify(adapter).resetBroadcasts();


	}

	@Test
	void testShutDownStatus() {
		RestServiceV2 restServiceSpy = spy(restService);
		AntMediaApplicationAdapter adaptor = mock(AntMediaApplicationAdapter.class);

		doReturn(adaptor).when(restServiceSpy).getAppAdaptor("app1");
		doReturn(null).when(restServiceSpy).getAppAdaptor("app2");
		restServiceSpy.setShutdownStatus("app1,app2");

		verify(adaptor).setShutdownProperly(true);
	}

	@Test
	void testExtractFQDN() {
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
	void testTriggerGC() {
		//just increase coverage and make sure that method is there.
		//It's better to check if it calls System.gc. We may add it later with Powermockito. It's good enough at this stage 
		RestServiceV2 restServiceSpy = spy(restService);
		Result result = restServiceSpy.triggerGc();
		assertTrue(result.isSuccess());
	}

	@Test
	void testConfigureSSL()
	{
		RestServiceV2 restServiceSpy = spy(restService);
		AdminApplication adminApp = mock(AdminApplication.class);
		doReturn(adminApp).when(restServiceSpy).getApplication();
		when(adminApp.runConfiguredCommand(eq(AdminApplication.ENABLE_SSL_COMMAND), any(String[].class))).thenReturn(true);

		Result result = restServiceSpy.configureSsl(null, null, null, null, null, null, null, null);
		assertFalse(result.isSuccess());
		verify(adminApp, never()).runConfiguredCommand(eq(AdminApplication.ENABLE_SSL_COMMAND), any(String[].class));

		result = restServiceSpy.configureSsl(null, "", null, null, null, null, null, null);
		assertFalse(result.isSuccess());
		verify(adminApp, never()).runConfiguredCommand(eq(AdminApplication.ENABLE_SSL_COMMAND), any(String[].class));

		result = restServiceSpy.configureSsl("", "", null, null, null, null, null, null);
		assertFalse(result.isSuccess());
		verify(adminApp, never()).runConfiguredCommand(eq(AdminApplication.ENABLE_SSL_COMMAND), any(String[].class));
		
		result = restServiceSpy.configureSsl("example.com", "", null, null, null, null, null, null);
		assertFalse(result.isSuccess());
		verify(adminApp, never()).runConfiguredCommand(eq(AdminApplication.ENABLE_SSL_COMMAND), any(String[].class));


		result = restServiceSpy.configureSsl("", "ANTMEDIA_SUBDOMAIN", null, null, null, null, null, null);
		assertTrue(result.isSuccess());
		verify(adminApp, times(1)).runConfiguredCommand(eq(AdminApplication.ENABLE_SSL_COMMAND), any(String[].class));


		result = restServiceSpy.configureSsl("", "CUSTOM_DOMAIN", null, null, null, null, null, null);
		assertFalse(result.isSuccess());
		verify(adminApp, times(1)).runConfiguredCommand(eq(AdminApplication.ENABLE_SSL_COMMAND), any(String[].class));

		result = restServiceSpy.configureSsl("http://example.com", "CUSTOM_DOMAIN", null, null, null, null, null, null);
		assertTrue(result.isSuccess());
		verify(adminApp, times(2)).runConfiguredCommand(eq(AdminApplication.ENABLE_SSL_COMMAND), any(String[].class));

		//ignores the given domain name
		result = restServiceSpy.configureSsl("http://example.com", "ANTMEDIA_SUBDOMAIN", null, null, null, null, null, null);
		assertTrue(result.isSuccess());
		verify(adminApp, times(3)).runConfiguredCommand(eq(AdminApplication.ENABLE_SSL_COMMAND), any(String[].class));


		result = restServiceSpy.configureSsl("http://example.com", "CUSTOM_CERTIFICATE", null, null, null, null, null, null);
		assertFalse(result.isSuccess());
		verify(adminApp, times(3)).runConfiguredCommand(eq(AdminApplication.ENABLE_SSL_COMMAND), any(String[].class));

		try {
				
			InputStream fullChainInputStream = new FileInputStream("src/test/resources/test.properties");

			result = restServiceSpy.configureSsl("http://example.com", "CUSTOM_CERTIFICATE", fullChainInputStream, null, null, null, null, null);
			assertFalse(result.isSuccess());
			verify(adminApp, times(3)).runConfiguredCommand(eq(AdminApplication.ENABLE_SSL_COMMAND), any(String[].class));
			
			FormDataContentDisposition fullChainFileContent = mock(FormDataContentDisposition.class);
			when(fullChainFileContent.getFileName()).thenReturn(null);
			result = restServiceSpy.configureSsl("http://example.com", "CUSTOM_CERTIFICATE", fullChainInputStream, fullChainFileContent, null, null, null, null);
			assertFalse(result.isSuccess());
			verify(adminApp, times(3)).runConfiguredCommand(eq(AdminApplication.ENABLE_SSL_COMMAND), any(String[].class));
			
			when(fullChainFileContent.getFileName()).thenReturn("");
			result = restServiceSpy.configureSsl("http://example.com", "CUSTOM_CERTIFICATE", fullChainInputStream, fullChainFileContent, null, null, null, null);
			assertFalse(result.isSuccess());
			verify(adminApp, times(3)).runConfiguredCommand(eq(AdminApplication.ENABLE_SSL_COMMAND), any(String[].class));
			
			when(fullChainFileContent.getFileName()).thenReturn("fullchain.pem");
			result = restServiceSpy.configureSsl("http://example.com", "CUSTOM_CERTIFICATE", fullChainInputStream, fullChainFileContent, null, null, null, null);
			assertFalse(result.isSuccess());
			verify(adminApp, times(3)).runConfiguredCommand(eq(AdminApplication.ENABLE_SSL_COMMAND), any(String[].class));
			
			
			//private key file
			InputStream privateKeyFileInputStream = new FileInputStream("src/test/resources/test.properties");

			result = restServiceSpy.configureSsl("http://example.com", "CUSTOM_CERTIFICATE", fullChainInputStream, fullChainFileContent, privateKeyFileInputStream, null, null, null);
			assertFalse(result.isSuccess());
			verify(adminApp, times(3)).runConfiguredCommand(eq(AdminApplication.ENABLE_SSL_COMMAND), any(String[].class));
			
			FormDataContentDisposition privateFileContent = mock(FormDataContentDisposition.class);
			when(privateFileContent.getFileName()).thenReturn(null);
			result = restServiceSpy.configureSsl("http://example.com", "CUSTOM_CERTIFICATE", fullChainInputStream, fullChainFileContent, privateKeyFileInputStream, privateFileContent, null, null);
			assertFalse(result.isSuccess());
			verify(adminApp, times(3)).runConfiguredCommand(eq(AdminApplication.ENABLE_SSL_COMMAND), any(String[].class));
			
			when(privateFileContent.getFileName()).thenReturn("");
			result = restServiceSpy.configureSsl("http://example.com", "CUSTOM_CERTIFICATE", fullChainInputStream, fullChainFileContent, privateKeyFileInputStream, privateFileContent, null, null);
			assertFalse(result.isSuccess());
			verify(adminApp, times(3)).runConfiguredCommand(eq(AdminApplication.ENABLE_SSL_COMMAND), any(String[].class));
			
			when(privateFileContent.getFileName()).thenReturn("fullchain.pem");
			result = restServiceSpy.configureSsl("http://example.com", "CUSTOM_CERTIFICATE", fullChainInputStream, fullChainFileContent, privateKeyFileInputStream, privateFileContent, null, null);
			assertFalse(result.isSuccess());
			verify(adminApp, times(3)).runConfiguredCommand(eq(AdminApplication.ENABLE_SSL_COMMAND), any(String[].class));
		
			
			//chain file
			
			InputStream chainFileInputStream = new FileInputStream("src/test/resources/test.properties");

			result = restServiceSpy.configureSsl("http://example.com", "CUSTOM_CERTIFICATE", fullChainInputStream, fullChainFileContent, privateKeyFileInputStream, privateFileContent, chainFileInputStream, null);
			assertFalse(result.isSuccess());
			verify(adminApp, times(3)).runConfiguredCommand(eq(AdminApplication.ENABLE_SSL_COMMAND), any(String[].class));
			
			FormDataContentDisposition chainFileContent = mock(FormDataContentDisposition.class);
			when(chainFileContent.getFileName()).thenReturn(null);
			result = restServiceSpy.configureSsl("http://example.com", "CUSTOM_CERTIFICATE", fullChainInputStream, fullChainFileContent, privateKeyFileInputStream, privateFileContent, chainFileInputStream, chainFileContent);
			assertFalse(result.isSuccess());
			verify(adminApp, times(3)).runConfiguredCommand(eq(AdminApplication.ENABLE_SSL_COMMAND), any(String[].class));
			
			when(chainFileContent.getFileName()).thenReturn("");
			result = restServiceSpy.configureSsl("http://example.com", "CUSTOM_CERTIFICATE", fullChainInputStream, fullChainFileContent, privateKeyFileInputStream, privateFileContent, chainFileInputStream, chainFileContent);
			assertFalse(result.isSuccess());
			verify(adminApp, times(3)).runConfiguredCommand(eq(AdminApplication.ENABLE_SSL_COMMAND), any(String[].class));
			
			when(chainFileContent.getFileName()).thenReturn("fullchain.pem");
			result = restServiceSpy.configureSsl("http://example.com", "CUSTOM_CERTIFICATE", fullChainInputStream, fullChainFileContent, privateKeyFileInputStream, privateFileContent, chainFileInputStream, chainFileContent);
			assertTrue(result.isSuccess());
			verify(adminApp, times(4)).runConfiguredCommand(eq(AdminApplication.ENABLE_SSL_COMMAND), any(String[].class));
			
			
			

		} catch (FileNotFoundException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}


	}

	@Test
	void testAuthenticateMultiAppUser(){
		
		String password = "password";
		
		String userName = "username" + (int) (Math.random() * 100000);

		Map<String,String> appNameUserTypeMap = new HashMap<>();
		appNameUserTypeMap.put("LiveApp", UserType.USER.toString());
		appNameUserTypeMap.put("live", UserType.READ_ONLY.toString());

		User user = new User(userName, CommonRestService.getMD5Hash(password), null, null, appNameUserTypeMap);
		dbStore.addUser(user);

		HttpSession session = mock(HttpSession.class);

		when(session.getAttribute(USER_EMAIL)).thenReturn(userName);
		when(session.getAttribute(USER_PASSWORD)).thenReturn(password);

		HttpServletRequest mockRequest = mock(HttpServletRequest.class);

		when(mockRequest.getSession()).thenReturn(session);

		restService.setRequestForTest(mockRequest);

		JsonObject appNameUserTypeJson = new JsonObject();
		for (Map.Entry<String, String> entry : user.getAppNameUserType().entrySet()) {
			String appName = entry.getKey();
			String userType = entry.getValue();

			appNameUserTypeJson.addProperty(appName, userType);
		}

		//Set the password because we get md5 hash 
		user.setPassword(password);
		Result result = restService.authenticateUser(user);
		assertTrue(result.isSuccess());
        assertEquals(result.getMessage(), appNameUserTypeJson.toString());
	}


	@Test
	void testAppStatus(){
		FileInputStream inputStream;
		try{
			inputStream = new FileInputStream("src/test/resources/sample_MP4_480.mp4");
			String tmpsDirectory = System.getProperty("java.io.tmpdir");

			AppSettings appSettings = new AppSettings();

			RestServiceV2 restServiceSpy = spy(restService);

			AdminApplication adminApp = mock(AdminApplication.class);
			IScope rootScope = mock(IScope.class);
			String appName = "testapp";

			IClusterNotifier clusterNotifier = mock(IClusterNotifier.class);
			IClusterStore clusterStore = mock(IClusterStore.class);
			when(clusterNotifier.getClusterStore()).thenReturn(clusterStore);
			when(adminApp.getClusterNotifier()).thenReturn(clusterNotifier);

			when(rootScope.getScope(appName)).thenReturn(mock(IScope.class));
			when(adminApp.getRootScope()).thenReturn(rootScope);
			doReturn(true).when(adminApp).createApplication(anyString(), anyString());
			doReturn(true).when(adminApp).deleteApplication(anyString(), anyBoolean());


			doReturn(adminApp).when(restServiceSpy).getApplication();
			doReturn(true).when(restServiceSpy).isClusterMode();
			doReturn(false).when(restServiceSpy).isApplicationExists(appName);
			doReturn(mock(ServerSettings.class)).when(restServiceSpy).getServerSettings();
			doReturn(appSettings).when(restServiceSpy).getSettings(appName);
			doReturn("").when(restServiceSpy).changeSettings(eq(appName), any());

			Result result = restServiceSpy.createApplication(appName, inputStream);

			ArgumentCaptor<AppSettings> appSettingsArgumentCaptor = ArgumentCaptor.forClass(AppSettings.class);
			verify(clusterStore, times(1)).saveSettings(appSettingsArgumentCaptor.capture());
			assertEquals(AppSettings.APPLICATION_STATUS_INSTALLING, appSettingsArgumentCaptor.getValue().getAppStatus());

			ArgumentCaptor<AppSettings> appSettingsArgumentCaptor2 = ArgumentCaptor.forClass(AppSettings.class);
			verify(restServiceSpy, times(1)).changeSettings(eq(appName), appSettingsArgumentCaptor2.capture());
			assertEquals(AppSettings.APPLICATION_STATUS_INSTALLED, appSettingsArgumentCaptor2.getValue().getAppStatus());

			assertTrue(result.isSuccess());

			result = restServiceSpy.deleteApplication(appName, true);
			assertTrue(result.isSuccess());

		}
		catch(Exception e){

			e.printStackTrace();
			fail(e.getMessage());
		}


	}
}
