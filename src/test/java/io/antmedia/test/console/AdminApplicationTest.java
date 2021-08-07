package io.antmedia.test.console;

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
		
		app.deleteApplication("test");
		
		Mockito.verify(adapter).serverShuttingdown();
		try {
			Mockito.verify(appScope).destroy();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		Mockito.verify(warDeployer).undeploy("test");
		
		
	}

}
