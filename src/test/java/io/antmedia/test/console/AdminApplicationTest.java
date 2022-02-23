package io.antmedia.test.console;

import java.io.IOException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
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
	public void testPullWarFile(){
		AntMediaApplicationAdapter adaptor = Mockito.mock(AntMediaApplicationAdapter.class);
		AdminApplication adminApplication = Mockito.spy(new AdminApplication());
		try{
			//Just download something to check if it is downloading, the method only downloads with an http request.
			assertTrue(adminApplication.pullWarFile("LiveApp", "https://antmedia.io/rest"));
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
		
		String id = dataStore.addVod(Mockito.mock(VoD.class));
		
		assertEquals(1, adminApplication.getVoDCount(Mockito.mock(IScope.class)));
		
		dataStore.deleteVod(id);
		
		assertEquals(0, adminApplication.getVoDCount(Mockito.mock(IScope.class)));
	}

}
