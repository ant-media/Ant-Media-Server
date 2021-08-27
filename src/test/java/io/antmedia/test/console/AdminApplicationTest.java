package io.antmedia.test.console;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.io.IOException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.red5.server.scope.WebScope;
import org.red5.server.tomcat.WarDeployer;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.console.AdminApplication;
import io.vertx.core.Vertx;

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
		app.createApplication("test");
		
		Mockito.verify(app).runCreateAppScript("test");
		Mockito.verify(warDeployer, Mockito.timeout(4000)).deploy(true);
		
		
		//delete application
		WebScope rootScope = Mockito.mock(WebScope.class);
		Mockito.doReturn(rootScope).when(app).getRootScope();
		
		WebScope appScope = Mockito.mock(WebScope.class);
		Mockito.doReturn(appScope).when(rootScope).getScope(Mockito.anyString());
		
		AntMediaApplicationAdapter adapter = Mockito.mock(AntMediaApplicationAdapter.class);
		Mockito.doReturn(adapter).when(app).getApplicationAdaptor(Mockito.any());
		
		boolean result = app.deleteApplication("test");
		assertFalse(result);
		
		Mockito.verify(adapter).serverShuttingdown();
		try {
			Mockito.verify(appScope).destroy();
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		
		Mockito.verify(warDeployer).undeploy("test");

		
		
		try {
			Mockito.doThrow(new Exception()).when(appScope).destroy();
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		Mockito.doReturn(true).when(app).runDeleteAppScript(Mockito.any());
		
		result = app.deleteApplication("test");
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

}
