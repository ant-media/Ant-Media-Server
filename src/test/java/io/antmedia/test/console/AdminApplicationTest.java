package io.antmedia.test.console;

import java.io.IOException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.red5.server.api.scope.IScope;
import org.red5.server.scope.WebScope;
import org.red5.server.tomcat.WarDeployer;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.console.AdminApplication;
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
	public void testCreateDeleteApplication() 
	{
		//create application
		AdminApplication app = Mockito.spy(new AdminApplication());
		app.setVertx(vertx);
		WarDeployer warDeployer = Mockito.mock(WarDeployer.class);
		app.setWarDeployer(warDeployer);
		app.createApplication("test", null);

		Mockito.verify(app).runCreateAppScript("test", null);
		Mockito.verify(warDeployer, Mockito.timeout(4000)).deploy(true);


		//delete application
		WebScope rootScope = Mockito.mock(WebScope.class);
		Mockito.doReturn(rootScope).when(app).getRootScope();

		WebScope appScope = Mockito.mock(WebScope.class);
		Mockito.doReturn(appScope).when(rootScope).getScope(Mockito.anyString());

		AntMediaApplicationAdapter adapter = Mockito.mock(AntMediaApplicationAdapter.class);
		Mockito.doReturn(adapter).when(app).getApplicationAdaptor(Mockito.any());

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
		
		int warDeployDuration = 2000;
		String appName = "test";
		
		WarDeployer warDeployer = new WarDeployer() {
			public void deploy(boolean startApplication) {
				long t0 = System.currentTimeMillis();
				while(System.currentTimeMillis() < t0+warDeployDuration);
			};
		};
		app.setWarDeployer(warDeployer);
		Mockito.doReturn(true).when(app).runCreateAppScript(Mockito.any(), Mockito.any());
		
		app.createApplication(appName, null);

		Mockito.verify(app).runCreateAppScript(appName, null);

		assertFalse(app.createApplicationWithURL(appName, "some_url"));
	}

}
