package io.antmedia.test.console;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.catalina.Container;
import org.apache.catalina.Host;
import org.apache.catalina.connector.Connector;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.coyote.UpgradeProtocol;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.red5.server.LoaderBase;
import org.red5.server.api.IApplicationContext;
import org.red5.server.api.scope.IScope;
import org.red5.server.scope.Scope;
import org.red5.server.scope.WebScope;
import org.red5.server.tomcat.TomcatConnector;
import org.red5.server.tomcat.TomcatLoader;
import org.red5.server.tomcat.WarDeployer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.console.AdminApplication;
import io.antmedia.console.datastore.ConsoleDataStoreFactory;
import io.antmedia.datastore.db.InMemoryDataStore;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.datastore.db.types.BroadcastUpdate;
import io.antmedia.datastore.db.types.VoD;
import io.vertx.core.Vertx;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class AdminApplicationTest {
	
	private Logger logger = LoggerFactory.getLogger(AdminApplicationTest.class);

	static Vertx vertx;

	@BeforeAll
	public static void beforeClass() {
		vertx = Vertx.vertx();
	}

	@AfterAll
	public static void afterClass() {
		vertx.close();
	}


	@Test
	public void testUndeployedDirectoryWhileDeletingApp() throws Exception {

		String appName = RandomStringUtils.randomAlphabetic(19);
		AdminApplication app = Mockito.spy(new AdminApplication());
		app.setVertx(vertx);

		AntMediaApplicationAdapter adapter = Mockito.mock(AntMediaApplicationAdapter.class);
		Mockito.doReturn(adapter).when(app).getApplicationAdaptor(Mockito.any());

		WebScope rootScope = Mockito.mock(WebScope.class);
		Mockito.doReturn(rootScope).when(app).getRootScope();

		WarDeployer warDeployer = Mockito.mock(WarDeployer.class);
		app.setWarDeployer(warDeployer);

		app.deleteApplication(appName, false);

		Mockito.verify(app, Mockito.times(0)).runDeleteAppScript(Mockito.anyString());

		WebScope appScope = Mockito.mock(WebScope.class);
		Mockito.doReturn(appScope).when(rootScope).getScope(Mockito.anyString());
		Mockito.when(appScope.isRunning()).thenReturn(false);
		app.getCurrentApplicationCreationProcesses().remove(appName);
		app.deleteApplication(appName, false);

		Mockito.verify(app, Mockito.times(0)).runDeleteAppScript(Mockito.anyString());

		Mockito.verify(appScope, Mockito.never()).destroy();

		Mockito.when(appScope.isRunning()).thenReturn(true);
		app.getCurrentApplicationCreationProcesses().remove(appName);
		assertFalse(app.deleteApplication(appName, false));

		Mockito.verify(app, Mockito.times(1)).runDeleteAppScript(Mockito.anyString());
		Mockito.verify(appScope, Mockito.times(1)).destroy();

		File f = new File("webapps/" + appName);
		if (!f.exists()) {
			f.mkdirs();
		}

		assertTrue(f.exists());
		app.getCurrentApplicationCreationProcesses().remove(appName);		
		Mockito.when(appScope.isRunning()).thenReturn(false);
		app.getCurrentApplicationCreationProcesses().remove(appName);
		app.deleteApplication(appName, false);

		Mockito.verify(app, Mockito.times(2)).runDeleteAppScript(Mockito.anyString());


	}
	
	
	@Test
	public void testCreateAppParameters() {
		AdminApplication app = Mockito.spy(new AdminApplication());
		app.setVertx(vertx);
		
		Mockito.doReturn(false).when(app).runConfiguredCommand(Mockito.anyString(), Mockito.any(String[].class));
		ConsoleDataStoreFactory consoleDataStoreFactory = Mockito.mock(ConsoleDataStoreFactory.class);
		app.setDataStoreFactory(consoleDataStoreFactory);
		
		app.runCreateAppScript("app", false, null, null);
		
		ArgumentCaptor<String> commandCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String[]> argsCaptor = ArgumentCaptor.forClass(String[].class);
		//"/bin/bash create_app.sh -n app -w true -p /Users/mekya/git/Ant-Media-Server -c false -m null -u null -s null"
		
		Mockito.verify(app).runConfiguredCommand(commandCaptor.capture(), argsCaptor.capture());
		String command = commandCaptor.getValue() + " " + String.join(" ", argsCaptor.getValue());
		
		assertTrue(command.contains("-c false"));
		assertTrue(command.contains("-n app"));
		
		assertFalse(command.contains("-m "));
		assertFalse(command.contains("-u "));
		assertFalse(command.contains("-s "));
		assertFalse(command.contains("-f "));
		
		Mockito.when(consoleDataStoreFactory.getDbType()).thenReturn("mapdb");
		app.runCreateAppScript("app", false, "dbUrl" , null);
		
		Mockito.verify(app, Mockito.times(2)).runConfiguredCommand(commandCaptor.capture(), argsCaptor.capture());
		command = commandCaptor.getValue() + " " + String.join(" ", argsCaptor.getValue());
		assertTrue(command.contains("-c false"));
		assertTrue(command.contains("-n app"));
		
		assertFalse(command.contains("-m dbUrl"));
		assertFalse(command.contains("-u username"));
		assertFalse(command.contains("-s pass"));
		assertFalse(command.contains("-f "));
		
		
		Mockito.when(consoleDataStoreFactory.getDbType()).thenReturn("mongob");
		app.runCreateAppScript("app", false, "dbUrl" , null);
		
		Mockito.verify(app, Mockito.times(3)).runConfiguredCommand(commandCaptor.capture(), argsCaptor.capture());
		command = commandCaptor.getValue() + " " + String.join(" ", argsCaptor.getValue());
		assertTrue(command.contains("-c false"));
		assertTrue(command.contains("-n app"));
		
		assertTrue(command.contains("-m dbUrl"));
		assertFalse(command.contains("-u username")); //false because we do not use username and pass anymore
		assertFalse(command.contains("-s pass")); //false because we do not use username and pass anymore
		assertFalse(command.contains("-f"));
		
		
		app.runCreateAppScript("app", false, "dbUrl" , "warfile");
		
		Mockito.verify(app, Mockito.times(4)).runConfiguredCommand(commandCaptor.capture(), argsCaptor.capture());
		command = commandCaptor.getValue() + " " + String.join(" ", argsCaptor.getValue());
		assertTrue(command.contains("-c false"));
		assertTrue(command.contains("-n app"));
		
		assertTrue(command.contains("-m dbUrl"));
		assertFalse(command.contains("-u username")); //false because we do not use username and pass anymore
		assertFalse(command.contains("-s pass")); //false because we do not use username and pass anymore
		assertTrue(command.contains("-f warfile"));

	}
	@Test
	public void testApplictionWarFileName() {
		 	assertEquals("myapp", WarDeployer.getApplicationName("myapp-1.0.0.war"));
	        assertEquals("customer-portal", WarDeployer.getApplicationName("customer-portal-2.5.war"));
	        assertEquals("testapp", WarDeployer.getApplicationName("testapp-2024.04.26.war"));

	        assertEquals("e-commerce", WarDeployer.getApplicationName("e-commerce.war"));
	        assertEquals("simpleapp", WarDeployer.getApplicationName("simpleapp.war"));
	        assertEquals("something-else-v1", WarDeployer.getApplicationName("something-else-v1.war"));

	        assertEquals("fancy-app", WarDeployer.getApplicationName("fancy-app-1.0.0.war"));
	        assertEquals("my-super-cool-app", WarDeployer.getApplicationName("my-super-cool-app.war"));

	        assertEquals("app", WarDeployer.getApplicationName("app-1.war"));
	        assertEquals("app", WarDeployer.getApplicationName("app-123.war"));
	        assertEquals("weirdapp-", WarDeployer.getApplicationName("weirdapp-.war"));
	   
	}
	@Test
	public void testGetDirectorySize() {
		AdminApplication app = Mockito.spy(new AdminApplication());

		File testDir = new File(".");
		long directorySize = app.getDirectorySize(testDir.toPath());
		logger.info("Directory size: {}", directorySize);
		assertTrue(directorySize >= 10000000); // 10 MB
		
		
		File testFile = new File("pom.xml");
		directorySize = app.getDirectorySize(testFile.toPath());
		logger.info("Size: {}", directorySize);
		assertTrue(directorySize >= 1000); // 
		
		
		testFile = new File("not exist");
		directorySize = app.getDirectorySize(testFile.toPath());
		logger.info("Directory size: {}", directorySize);
		assertEquals(-1, directorySize); // 10 MB
		
	}

	@Test
	public void testCreateDeleteApplication() 
	{
		//create application
		AdminApplication app = Mockito.spy(new AdminApplication());
		app.setVertx(vertx);
		
		app.setDataStoreFactory(Mockito.mock(ConsoleDataStoreFactory.class));

		WebScope rootScope = Mockito.mock(WebScope.class);
		Mockito.doReturn(rootScope).when(app).getRootScope();

		WebScope appScope = Mockito.mock(WebScope.class);
		Mockito.doReturn(appScope).when(rootScope).getScope(Mockito.anyString());

		Mockito.when(appScope.isRunning()).thenReturn(true);

		WarDeployer warDeployer = Mockito.mock(WarDeployer.class);
		app.setWarDeployer(warDeployer);
		app.createApplication("test", null);

		Mockito.verify(app, Mockito.never()).runCreateAppScript("test", false, null, null);


		Mockito.when(appScope.isRunning()).thenReturn(false);
		app.createApplication("test", null);

		Mockito.verify(app).runCreateAppScript("test", false, null, null);
		Mockito.verify(warDeployer, Mockito.timeout(4000)).deploy(true);


		AntMediaApplicationAdapter adapter = Mockito.mock(AntMediaApplicationAdapter.class);
		Mockito.doReturn(adapter).when(app).getApplicationAdaptor(Mockito.any());
		Mockito.when(appScope.isRunning()).thenReturn(true);

		boolean result = app.deleteApplication("test", true);
		assertFalse(result);

		try {
			Mockito.verify(appScope).destroy();
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		Mockito.verify(adapter).stopApplication(true);
		Mockito.verify(warDeployer).undeploy("test");



		try {
			Mockito.doThrow(new Exception()).when(appScope).destroy();
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		Mockito.doReturn(true).when(app).runDeleteAppScript(Mockito.any());

		result = app.deleteApplication("test", false);
		assertFalse(result);

	}


	@Test
	public void testSpecialChars() {
		AdminApplication app = new AdminApplication();

		boolean result = app.runConfiguredCommand("test &");
		assertFalse(result);
		result = app.runConfiguredCommand("echo x");
		assertFalse(result);
		result = app.runConfiguredCommand(AdminApplication.CREATE_APP_COMMAND, "echo x");
		assertFalse(result);
		result = app.runConfiguredCommand(AdminApplication.CREATE_APP_COMMAND, "&echo");
		assertFalse(result);
	}

	@Test
	public void testCreateAppCommandArguments() throws Exception {
		AdminApplication app = new AdminApplication();

		assertTrue(areCommandArgumentsValid(app, AdminApplication.CREATE_APP_COMMAND,
				"-n", "testapp", "-m", "mongodb://user:password@127.0.0.1:27018/admin?readPreference=secondaryPreferred&authSource=admin",
				"-p", "/usr/local/antmedia", "-w", "true", "-c", "false"));
		assertTrue(areCommandArgumentsValid(app, AdminApplication.CREATE_APP_COMMAND,
				"testapp", "/usr/local/antmedia"));
		assertFalse(areCommandArgumentsValid(app, AdminApplication.CREATE_APP_COMMAND,
				"-n", "test app"));
		assertFalse(areCommandArgumentsValid(app, AdminApplication.CREATE_APP_COMMAND,
				"-m", "mongodb://127.0.0.1:27017/admin;touch/tmp/test"));
		assertFalse(areCommandArgumentsValid(app, AdminApplication.CREATE_APP_COMMAND,
				"-x", "testapp"));
		assertFalse(areCommandArgumentsValid(app, AdminApplication.CREATE_APP_COMMAND,
				"testapp", "/usr/local/antmedia", "mongodb://127.0.0.1:27017/admin"));
	}

	private boolean areCommandArgumentsValid(AdminApplication app, String configuredCommand, String... args) throws Exception {
		Method method = AdminApplication.class.getDeclaredMethod("areCommandArgumentsValid", String.class, String[].class);
		method.setAccessible(true);
		return (boolean) method.invoke(app, configuredCommand, args);
	}

	@Test
	public void testRunConfiguredCommand() {
		AdminApplication app = new AdminApplication();

		boolean runCommand = app.runConfiguredCommand("");
		assertFalse(runCommand);

		runCommand = app.runConfiguredCommand("echo x");
		assertFalse(runCommand);

	}
	
	@Test
	public void testGetApplication() {
		AntMediaApplicationAdapter adaptor = Mockito.mock(AntMediaApplicationAdapter.class);
		AdminApplication adminApplication = Mockito.spy(new AdminApplication());
		
		IScope rootScope = Mockito.mock(IScope.class);
		Mockito.doReturn(rootScope).when(adminApplication).getRootScope();
		
		List<String> applications = adminApplication.getApplications();
		assertTrue(applications.isEmpty());
		
		
		Set<String> scopeNames = new HashSet<>();
		scopeNames.add("live");
		scopeNames.add("vod");
		scopeNames.add("root");
		
		Mockito.when(rootScope.getScopeNames()).thenReturn(scopeNames);
		
		applications = adminApplication.getApplications();
		assertTrue(applications.isEmpty());
		
		Scope liveScope = Mockito.mock(Scope.class);
		Mockito.when(rootScope.getScope("live")).thenReturn(liveScope);
		applications = adminApplication.getApplications();
		assertTrue(applications.isEmpty());
		
		Mockito.when(liveScope.isRunning()).thenReturn(true);
		applications = adminApplication.getApplications();
		assertFalse(applications.isEmpty());
		assertEquals(1, applications.size());
		

	}
	
	@Test
	public void testLiveStreamCount() {
		AntMediaApplicationAdapter adaptor = Mockito.mock(AntMediaApplicationAdapter.class);
		AdminApplication adminApplication = Mockito.spy(new AdminApplication());

		Mockito.doReturn(adaptor).when(adminApplication).getApplicationAdaptor(Mockito.any());
		InMemoryDataStore dataStore = new InMemoryDataStore("junit");
		Mockito.when(adaptor.getDataStore()).thenReturn(dataStore);
		assertEquals(0, adminApplication.getAppLiveStreamCount(Mockito.mock(IScope.class)));


		Broadcast broadcast = new Broadcast();
		broadcast.setUpdateTime(System.currentTimeMillis());
		broadcast.setStatus(AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING);
		String id = dataStore.save(broadcast);
		assertEquals(1, dataStore.getActiveBroadcastCount());

		assertEquals(1, adminApplication.getAppLiveStreamCount(Mockito.mock(IScope.class)));

		dataStore.save(new Broadcast());
		assertEquals(1, adminApplication.getAppLiveStreamCount(Mockito.mock(IScope.class)));

		BroadcastUpdate broadcastUpdate = new BroadcastUpdate();
		broadcastUpdate.setStatus(AntMediaApplicationAdapter.BROADCAST_STATUS_FINISHED);
		dataStore.updateBroadcastFields(id, broadcastUpdate);

		assertEquals(0, adminApplication.getAppLiveStreamCount(Mockito.mock(IScope.class)));


	}


	@Test
	public void testCreateApplicationWitURL() {
		try {
			AdminApplication adminApplication = Mockito.spy(new AdminApplication());
			Mockito.doReturn(false).when(adminApplication).createApplication(Mockito.anyString(), Mockito.any());

			adminApplication.createApplicationWithURL("app", "https://antmedia.io/rest", "secret");		
			Mockito.verify(adminApplication).downloadWarFile("app", "https://antmedia.io/rest", "secret");

			adminApplication.createApplicationWithURL("app2", null, null);
			//it should be never for app2 because url is null
			Mockito.verify(adminApplication, Mockito.never()).downloadWarFile(Mockito.eq("app2"), nullable(String.class), nullable(String.class));


			adminApplication.createApplicationWithURL("app3", "", null);
			//it should be never for app3 because url is ""
			Mockito.verify(adminApplication, Mockito.never()).downloadWarFile(Mockito.eq("app3"), Mockito.anyString(),eq(null));

			adminApplication.createApplicationWithURL("app4", "htdfdf", null);
			//it should be 2 time because url is not starting with http. It also with different app name.
			Mockito.verify(adminApplication, Mockito.times(1)).downloadWarFile(Mockito.anyString(),Mockito.anyString(), nullable(String.class));
			
			adminApplication.createApplicationWithURL("app5", "https://dfaf", null);
			//it should be 2 time because url is  starting with http. It also with different app name.
			Mockito.verify(adminApplication, Mockito.times(2)).downloadWarFile(Mockito.anyString(),Mockito.anyString(), nullable(String.class));
			
			
			
			
			adminApplication = Mockito.spy(new AdminApplication());
			Mockito.doReturn(false).when(adminApplication).createApplication(Mockito.anyString(), Mockito.any());
			
			Mockito.doReturn(null).when(adminApplication).downloadWarFile(Mockito.anyString(), anyString(), anyString());
			adminApplication.createApplicationWithURL("app6", "https://antmedia.io/rest", "secret");
			verify(adminApplication, never()).createApplication(Mockito.anyString(), Mockito.any());


			Mockito.doReturn(new File("test")).when(adminApplication).downloadWarFile(Mockito.anyString(), anyString(), anyString());
			adminApplication.createApplicationWithURL("app6", "https://antmedia.io/rest", "secret");
			verify(adminApplication, times(1)).createApplication(Mockito.anyString(), Mockito.any());


		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testPullWarFile(){
		AntMediaApplicationAdapter adaptor = Mockito.mock(AntMediaApplicationAdapter.class);
		AdminApplication adminApplication = Mockito.spy(new AdminApplication());
		try{
			//Just download something to check if it is downloading, the method only downloads with an http request.
			assertNotNull(adminApplication.downloadWarFile("LiveApp", "https://antmedia.io/rest", "secret"));
		}
		catch(Exception e){
			e.printStackTrace();
			fail();
		}
	}
	
	@Test
	public void testGetWarFileInTmpDirectory() throws IOException {
		
		File warFileInTmpDirectory = AdminApplication.getWarFileInTmpDirectory("anywardoesnotexist");
		assertNull(warFileInTmpDirectory);
		
		//create a file in tmp directory
		String appName = "test";
		String filename = "test.war";
		
		File f = new File(AdminApplication.getJavaTmpDirectory(), filename);
		f.deleteOnExit();
		f.createNewFile();
		
		warFileInTmpDirectory = AdminApplication.getWarFileInTmpDirectory(AdminApplication.getWarName(appName));
		assertNotNull(warFileInTmpDirectory);

		
	}

	@Test
	public void testVodCount() {
		AntMediaApplicationAdapter adaptor = Mockito.mock(AntMediaApplicationAdapter.class);
		AdminApplication adminApplication = Mockito.spy(new AdminApplication());

		Mockito.doReturn(adaptor).when(adminApplication).getApplicationAdaptor(Mockito.any());
		InMemoryDataStore dataStore = new InMemoryDataStore("junit");
		Mockito.when(adaptor.getDataStore()).thenReturn(dataStore);

		assertEquals(0, adminApplication.getVoDCount(Mockito.mock(IScope.class)));

		VoD streamVod = new VoD();
		String id = dataStore.addVod(streamVod);

		assertEquals(1, adminApplication.getVoDCount(Mockito.mock(IScope.class)));

		dataStore.deleteVod(id);

		assertEquals(0, adminApplication.getVoDCount(Mockito.mock(IScope.class)));
	}

	@Test
	public void testPreventConcurrentInstallationSameWar() 
	{
		//create application
		AdminApplication app = Mockito.spy(new AdminApplication());
		app.setVertx(vertx);
		
		app.setDataStoreFactory(Mockito.mock(ConsoleDataStoreFactory.class));

		WebScope rootScope = Mockito.mock(WebScope.class);
		Mockito.doReturn(rootScope).when(app).getRootScope();

		WebScope appScope = Mockito.mock(WebScope.class);
		Mockito.doReturn(appScope).when(rootScope).getScope(Mockito.anyString());
		Mockito.when(appScope.isRunning()).thenReturn(true);

		int warDeployDuration = 2000;
		String appName = "test";

		WarDeployer warDeployer = new WarDeployer() {
			public void deploy(boolean startApplication) {
				long t0 = System.currentTimeMillis();
				while(System.currentTimeMillis() < t0+warDeployDuration);
			};
		};
		app.setWarDeployer(warDeployer);
		Mockito.doReturn(true).when(app).runCreateAppScript(Mockito.any(), Mockito.anyBoolean(), Mockito.any(), Mockito.any());

		app.createApplication(appName, null);

		Mockito.verify(app, Mockito.never()).runCreateAppScript(appName, false, null, null);

		Mockito.when(appScope.isRunning()).thenReturn(false);
		app.createApplication(appName, null);
		Mockito.verify(app).runCreateAppScript(appName, false, null, null);

		assertFalse(app.createApplicationWithURL(appName, "some_url", null));

		assertFalse(app.createApplication(appName, "some_url"));
	}

	@Test
	public void testTomcatLoaderRemoveContext() {
		TomcatLoader tomcatLoader = new TomcatLoader();
		Host host = Mockito.mock(Host.class);
		tomcatLoader.setBaseHost(host);
		Container[] container = new Container[0];
		Mockito.when(host.findChildren()).thenReturn(container);
		Mockito.when(host.getName()).thenReturn("host");

		String path = "path";

		IApplicationContext applicationContext = Mockito.mock(IApplicationContext.class);
		LoaderBase.setRed5ApplicationContext(path, applicationContext);
		tomcatLoader.removeContext(path);

		Mockito.verify(applicationContext).stop();
		assertNull(LoaderBase.getRed5ApplicationContext(path));


		LoaderBase.setRed5ApplicationContext(tomcatLoader.getHostId() + path, applicationContext);
		tomcatLoader.removeContext(path);
		Mockito.verify(applicationContext, Mockito.times(2)).stop();
		assertNull(LoaderBase.getRed5ApplicationContext(tomcatLoader.getHostId() + path));


		tomcatLoader.removeContext(path);
	}

	@Test
	public void testTomcatConnector() {
		{
			TomcatConnector tomcatConnector = new TomcatConnector();
			tomcatConnector.setAddress("127.0.0.1");
			tomcatConnector.init();

			Connector connector = tomcatConnector.getConnector();

			UpgradeProtocol[] protocols = connector.findUpgradeProtocols();
			assertEquals(0, protocols.length);
			
			assertTrue(tomcatConnector.isInitialized());
		}

		{
			TomcatConnector tomcatConnector = new TomcatConnector();
			tomcatConnector.setAddress("127.0.0.1");
			tomcatConnector.setSecure(true);
			tomcatConnector.init();

			Connector connector = tomcatConnector.getConnector();

			UpgradeProtocol[] protocols = connector.findUpgradeProtocols();
			assertEquals(0, protocols.length);
			assertTrue(tomcatConnector.isInitialized());

		}
		
		{
			TomcatConnector tomcatConnector = new TomcatConnector();
			tomcatConnector.setAddress("127.0.0.1");
			tomcatConnector.setSecure(true);
			HashMap<String, String> map = new HashMap<>();
			map.put("sslEnabledProtocols", "TLSv1.2");
			map.put("ciphers", "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256");
			map.put("useServerCipherSuitesOrder", "true");
			map.put("SSLCertificateFile", "conf/fullchain.pem");
			map.put("SSLCertificateChainFile", "conf/chain.pem");
			map.put("SSLCertificateKeyFile", "conf/key.pem");
			map.put("clientAuth", "false");

			tomcatConnector.setConnectionProperties(map);
			tomcatConnector.setUpgradeHttp2Protocol(true);

			tomcatConnector.init();

			Connector connector = tomcatConnector.getConnector();

			UpgradeProtocol[] protocols = connector.findUpgradeProtocols();
			assertEquals(1, protocols.length);
			assertTrue(tomcatConnector.isInitialized());

		}
	}

}
