package io.antmedia.test.console;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import org.apache.catalina.Container;
import org.apache.catalina.Host;
import org.apache.catalina.connector.Connector;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.coyote.UpgradeProtocol;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.red5.server.LoaderBase;
import org.red5.server.api.IApplicationContext;
import org.red5.server.api.scope.IScope;
import org.red5.server.scope.WebScope;
import org.red5.server.tomcat.TomcatConnector;
import org.red5.server.tomcat.TomcatLoader;
import org.red5.server.tomcat.WarDeployer;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.console.AdminApplication;
import io.antmedia.console.datastore.ConsoleDataStoreFactory;
import io.antmedia.datastore.db.InMemoryDataStore;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.datastore.db.types.VoD;
import io.vertx.core.Vertx;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;

public class AdminApplicationTest {

	static Vertx vertx;

	@BeforeClass
	public static void beforeClass() {
		vertx = Vertx.vertx();
	}

	@AfterClass
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
		
		Mockito.doReturn(false).when(app).runCommand(Mockito.anyString());
		ConsoleDataStoreFactory consoleDataStoreFactory = Mockito.mock(ConsoleDataStoreFactory.class);
		app.setDataStoreFactory(consoleDataStoreFactory);
		
		app.runCreateAppScript("app", false, null , null, null, null);
		
		ArgumentCaptor<String> commandCaptor = ArgumentCaptor.forClass(String.class);
		//"/bin/bash create_app.sh -n app -w true -p /Users/mekya/git/Ant-Media-Server -c false -m null -u null -s null"
		
		Mockito.verify(app).runCommand(commandCaptor.capture());
		
		assertTrue(commandCaptor.getValue().contains("-c false"));
		assertTrue(commandCaptor.getValue().contains("-n app"));
		
		assertFalse(commandCaptor.getValue().contains("-m "));
		assertFalse(commandCaptor.getValue().contains("-u "));
		assertFalse(commandCaptor.getValue().contains("-s "));
		assertFalse(commandCaptor.getValue().contains("-f "));
		
		Mockito.when(consoleDataStoreFactory.getDbType()).thenReturn("mapdb");
		app.runCreateAppScript("app", false, "dbUrl" , "username", "pass", null);
		
		Mockito.verify(app, Mockito.times(2)).runCommand(commandCaptor.capture());
		assertTrue(commandCaptor.getValue().contains("-c false"));
		assertTrue(commandCaptor.getValue().contains("-n app"));
		
		assertFalse(commandCaptor.getValue().contains("-m dbUrl"));
		assertFalse(commandCaptor.getValue().contains("-u username"));
		assertFalse(commandCaptor.getValue().contains("-s pass"));
		assertFalse(commandCaptor.getValue().contains("-f "));
		
		
		Mockito.when(consoleDataStoreFactory.getDbType()).thenReturn("mongob");
		app.runCreateAppScript("app", false, "dbUrl" , "username", "pass", null);
		
		Mockito.verify(app, Mockito.times(3)).runCommand(commandCaptor.capture());
		assertTrue(commandCaptor.getValue().contains("-c false"));
		assertTrue(commandCaptor.getValue().contains("-n app"));
		
		assertTrue(commandCaptor.getValue().contains("-m dbUrl"));
		assertTrue(commandCaptor.getValue().contains("-u username"));
		assertTrue(commandCaptor.getValue().contains("-s pass"));
		assertFalse(commandCaptor.getValue().contains("-f"));
		
		
		app.runCreateAppScript("app", false, "dbUrl" , "username", "pass", "warfile");
		
		Mockito.verify(app, Mockito.times(4)).runCommand(commandCaptor.capture());
		assertTrue(commandCaptor.getValue().contains("-c false"));
		assertTrue(commandCaptor.getValue().contains("-n app"));
		
		assertTrue(commandCaptor.getValue().contains("-m dbUrl"));
		assertTrue(commandCaptor.getValue().contains("-u username"));
		assertTrue(commandCaptor.getValue().contains("-s pass"));
		assertTrue(commandCaptor.getValue().contains("-f warfile"));

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

		Mockito.verify(app, Mockito.never()).runCreateAppScript("test", false, null, null, null, null);


		Mockito.when(appScope.isRunning()).thenReturn(false);
		app.createApplication("test", null);

		Mockito.verify(app).runCreateAppScript("test", false, null, null, null, null);
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
		AdminApplication app = Mockito.spy(new AdminApplication());

		try {
			String shellCommand = "/bin/bash create_app.sh -n oVs9G24e5BQqbaTNVtjh -w true -p /usr/local/antmedia -c false";
			Process process = app.getProcess("/bin/bash create_app.sh -n oVs9G24e5BQqbaTNVtjh -w true -p /usr/local/antmedia -c false");
			assertNotNull(process);

			String[] originalParameters = shellCommand.split(" ");

			ArgumentCaptor<String[]> parameters = ArgumentCaptor.forClass(String[].class);
			Mockito.verify(app).getProcessBuilder(parameters.capture());
			String[] params = parameters.getValue();

			for (int i = 0; i < params.length; i++) {
				assertEquals(params[i], originalParameters[i]);
			}


			shellCommand = "/bin/bash create_app.sh -n ProdApp -w true -p /usr/local/antmedia -c true -m mongodb://amsdfasfsadfdfdfpshot:6xNRRsdfsdfafd9NodO8vAFFBEHidfdfdfa87QDKXdCMubACDbhfQH1g==@amssdfafdafdadbsnapshot.mongo.cosmos.azure.com:10255/?ssl=true&replicaSet=globaldb&retrywrites=false&maxIdleTimeMS=120000&appName=@amssadfasdfdbsnsdfadfapshot@ -u  -s";
			process = app.getProcess(shellCommand);
			assertNotNull(process);

			originalParameters = shellCommand.split(" ");
			parameters = ArgumentCaptor.forClass(String[].class);
			Mockito.verify(app, Mockito.times(2)).getProcessBuilder(parameters.capture());
			params = parameters.getValue();
			for (int i = 0; i < params.length; i++) {
				if (originalParameters[i].contains("mongodb")) {
					assertEquals("'" + originalParameters[i] + "'",  params[i]);
				}
				else {
					assertEquals(params[i], originalParameters[i]);
				}
			}

			shellCommand = "test & ";
			process = app.getProcess(shellCommand);
			assertNotNull(process);

			originalParameters = shellCommand.split(" ");
			parameters = ArgumentCaptor.forClass(String[].class);
			Mockito.verify(app, Mockito.times(3)).getProcessBuilder(parameters.capture());
			params = parameters.getValue();
			for (int i = 0; i < params.length; i++) {
				if (originalParameters[i].contains("&")) {
					assertEquals("'" + originalParameters[i] + "'",  params[i]);
				}
				else {
					assertEquals(params[i], originalParameters[i]);
				}
			}


			boolean result = app.runCommand("test &");
			assertTrue(result);

			originalParameters = shellCommand.split(" ");
			parameters = ArgumentCaptor.forClass(String[].class);
			Mockito.verify(app, Mockito.times(4)).getProcessBuilder(parameters.capture());
			params = parameters.getValue();
			for (int i = 0; i < params.length; i++) {
				if (originalParameters[i].contains("&")) {
					assertEquals("'" + originalParameters[i] + "'",  params[i]);
				}
				else {
					assertEquals(params[i], originalParameters[i]);
				}
			}



		} 
		catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testRunCommand() {
		AdminApplication app = Mockito.spy(new AdminApplication());

		Process process = Mockito.mock(Process.class);


		try {
			Mockito.doThrow(new IOException()).when(app).getProcess(Mockito.anyString());
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		boolean runCommand = app.runCommand("");
		assertFalse(runCommand);


		try {
			Mockito.doReturn(process).when(app).getProcess(Mockito.anyString());
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		try {
			Mockito.doThrow(new InterruptedException()).when(process).waitFor();
		} catch (InterruptedException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		runCommand = app.runCommand("");
		assertFalse(runCommand);



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
		broadcast.setStatus(AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING);
		String id = dataStore.save(broadcast);
		assertEquals(1, dataStore.getActiveBroadcastCount());

		assertEquals(1, adminApplication.getAppLiveStreamCount(Mockito.mock(IScope.class)));

		dataStore.save(new Broadcast());
		assertEquals(1, adminApplication.getAppLiveStreamCount(Mockito.mock(IScope.class)));

		broadcast.setStatus(AntMediaApplicationAdapter.BROADCAST_STATUS_FINISHED);
		dataStore.updateBroadcastFields(id, broadcast);

		assertEquals(0, adminApplication.getAppLiveStreamCount(Mockito.mock(IScope.class)));


	}


	@Test
	public void testCreateApplicationWitURL() {
		try {
			AdminApplication adminApplication = Mockito.spy(new AdminApplication());
			Mockito.doReturn(false).when(adminApplication).createApplication(Mockito.anyString(), Mockito.any());

			adminApplication.createApplicationWithURL("app", "https://antmedia.io/rest");		
			Mockito.verify(adminApplication).downloadWarFile("app", "https://antmedia.io/rest");

			adminApplication.createApplicationWithURL("app2", null);
			//it should be never for app2 because url is null
			Mockito.verify(adminApplication, Mockito.never()).downloadWarFile(Mockito.eq("app2"), Mockito.anyString());


			adminApplication.createApplicationWithURL("app3", "");
			//it should be never for app3 because url is ""
			Mockito.verify(adminApplication, Mockito.never()).downloadWarFile(Mockito.eq("app3"), Mockito.anyString());

			adminApplication.createApplicationWithURL("app4", "htdfdf");
			//it should be 2 time because there is an url. It also with different app name.
			Mockito.verify(adminApplication, Mockito.times(2)).downloadWarFile(Mockito.anyString(),Mockito.anyString());


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
			assertNotNull(adminApplication.downloadWarFile("LiveApp", "https://antmedia.io/rest"));
		}
		catch(Exception e){
			e.printStackTrace();
			fail();
		}
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
		Mockito.doReturn(true).when(app).runCreateAppScript(Mockito.any(), Mockito.anyBoolean(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());

		app.createApplication(appName, null);

		Mockito.verify(app, Mockito.never()).runCreateAppScript(appName, false, null, null, null, null);

		Mockito.when(appScope.isRunning()).thenReturn(false);
		app.createApplication(appName, null);
		Mockito.verify(app).runCreateAppScript(appName, false, null, null, null, null);

		assertFalse(app.createApplicationWithURL(appName, "some_url"));

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
