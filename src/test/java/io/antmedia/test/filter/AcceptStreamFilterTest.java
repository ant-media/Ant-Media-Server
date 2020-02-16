package io.antmedia.test.filter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.filters.CorsFilter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.red5.server.scope.WebScope;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.AppSettings;
import io.antmedia.datastore.db.DataStore;
import io.antmedia.datastore.db.DataStoreFactory;
import io.antmedia.datastore.db.InMemoryDataStore;
import io.antmedia.filter.CorsHeaderFilter;
import io.antmedia.filter.StreamAcceptFilter;
import io.antmedia.integration.AppFunctionalTest;
import io.antmedia.security.AcceptOnlyStreamsInDataStore;

@ContextConfiguration(locations = {"../test.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class AcceptStreamFilterTest extends AbstractJUnit4SpringContextTests {
	
	private StreamAcceptFilter acceptStreamFilter;
	private AppSettings appSettings;
	
	static {
		System.setProperty("red5.deployment.type", "junit");
		System.setProperty("red5.root", ".");
	}

	@Before
	public void before() {
		acceptStreamFilter = new StreamAcceptFilter();
		
		File webApps = new File("webapps");
		if (!webApps.exists()) {
			webApps.mkdirs();
		}
		File junit = new File(webApps, "junit");
		if (!junit.exists()) {
			junit.mkdirs();
		}


		//reset values in the bean
		getAppSettings().resetDefaults();
		getAppSettings().setAddDateTimeToMp4FileName(false);
	}
	
	@After
	public void after() {
		acceptStreamFilter = null;
		
		try {
			AppFunctionalTest.delete(new File("webapps"));
		} catch (IOException e) {
			e.printStackTrace();
		}

		//reset values in the bean
		getAppSettings().resetDefaults();
		getAppSettings().setAddDateTimeToMp4FileName(false);
	}
	
	
	@Test
	public void testAcceptFilter() {
		
		StreamAcceptFilter acceptStreamFilterSpy = Mockito.spy(acceptStreamFilter);
		
		AntMediaApplicationAdapter spyAdapter = Mockito.spy(AntMediaApplicationAdapter.class);
		
		DataStore dataStore = new InMemoryDataStore("dbname");
		DataStoreFactory dsf = Mockito.mock(DataStoreFactory.class);
		Mockito.when(dsf.getDataStore()).thenReturn(dataStore);
		spyAdapter.setDataStoreFactory(dsf);
		
		//[0]-> streamFps
		//[1]-> StreamResolution
		//[2]-> StreamBitrate
		String[] parameters = {"20", "231", "223"};
		
		assertEquals(null,acceptStreamFilterSpy.getMaxFpsAccept());
		assertEquals(null,acceptStreamFilterSpy.getMaxResolutionAccept());
		assertEquals(null,acceptStreamFilterSpy.getMaxBitrateAccept());
		
		assertEquals(-1,acceptStreamFilterSpy.checkStreamParameters(parameters));
		
		// Default Scenario
		
		Mockito.when(acceptStreamFilterSpy.getMaxFpsAccept()).thenReturn("100");
		Mockito.when(acceptStreamFilterSpy.getMaxResolutionAccept()).thenReturn("1080");
		Mockito.when(acceptStreamFilterSpy.getMaxBitrateAccept()).thenReturn("2000000");
		
		// Stream parameters 
		parameters[0] = "60";
		parameters[1] = "720";
		parameters[2] = "1000000";
		
		assertEquals(-1,acceptStreamFilterSpy.checkStreamParameters(parameters));		

		// Stream FPS > Max FPS Scenario
		
		Mockito.when(acceptStreamFilterSpy.getMaxFpsAccept()).thenReturn("30");
		Mockito.when(acceptStreamFilterSpy.getMaxResolutionAccept()).thenReturn("1080");
		Mockito.when(acceptStreamFilterSpy.getMaxBitrateAccept()).thenReturn("2000000");
		
		// Stream parameters 
		parameters[0] = "60";
		parameters[1] = "720";
		parameters[2] = "1000000";
		
		assertEquals(0,acceptStreamFilterSpy.checkStreamParameters(parameters));		
		
		// Stream Resolution > Max Resolution Scenario
		
		Mockito.when(acceptStreamFilterSpy.getMaxFpsAccept()).thenReturn("100");
		Mockito.when(acceptStreamFilterSpy.getMaxResolutionAccept()).thenReturn("480");
		Mockito.when(acceptStreamFilterSpy.getMaxBitrateAccept()).thenReturn("2000000");
		
		// Stream parameters 
		parameters[0] = "60";
		parameters[1] = "720";
		parameters[2] = "1000000";
		
		assertEquals(1,acceptStreamFilterSpy.checkStreamParameters(parameters));	
		
		// Stream Bitrate > Max Bitrate Scenario
		
		Mockito.when(acceptStreamFilterSpy.getMaxFpsAccept()).thenReturn("100");
		Mockito.when(acceptStreamFilterSpy.getMaxResolutionAccept()).thenReturn("1080");
		Mockito.when(acceptStreamFilterSpy.getMaxBitrateAccept()).thenReturn("2000000");
		
		// Stream parameters 
		parameters[0] = "60";
		parameters[1] = "720";
		parameters[2] = "5000000";
		
		assertEquals(2,acceptStreamFilterSpy.checkStreamParameters(parameters));	
		
		// Stream Bitrate > Max Bitrate Scenario && getMaxResolutionAccept = null
		
		Mockito.when(acceptStreamFilterSpy.getMaxFpsAccept()).thenReturn("100");
		Mockito.when(acceptStreamFilterSpy.getMaxResolutionAccept()).thenReturn(null);
		Mockito.when(acceptStreamFilterSpy.getMaxBitrateAccept()).thenReturn("2000000");
		
		// Stream parameters 
		parameters[0] = "60";
		parameters[1] = "720";
		parameters[2] = "5000000";
		
		assertEquals(2,acceptStreamFilterSpy.checkStreamParameters(parameters));	
		
		// Normal Scenario & getMaxBitrateAccept = null
		
		Mockito.when(acceptStreamFilterSpy.getMaxFpsAccept()).thenReturn("100");
		Mockito.when(acceptStreamFilterSpy.getMaxResolutionAccept()).thenReturn("1080");
		Mockito.when(acceptStreamFilterSpy.getMaxBitrateAccept()).thenReturn(null);
		
		// Stream parameters 
		parameters[0] = "60";
		parameters[1] = "720";
		parameters[2] = "5000000";
		
		assertEquals(-1,acceptStreamFilterSpy.checkStreamParameters(parameters));	
		
		// Normal Scenario & getMaxFpsAccept = null & getMaxBitrateAccept = null
		
		Mockito.when(acceptStreamFilterSpy.getMaxFpsAccept()).thenReturn(null);
		Mockito.when(acceptStreamFilterSpy.getMaxResolutionAccept()).thenReturn("1080");
		Mockito.when(acceptStreamFilterSpy.getMaxBitrateAccept()).thenReturn(null);
		
		// Stream parameters 
		parameters[0] = "60";
		parameters[1] = "720";
		parameters[2] = "5000000";
		
		assertEquals(-1,acceptStreamFilterSpy.checkStreamParameters(parameters));	
		
	}
	
	public AppSettings getAppSettings() {
		if (appSettings == null) {
			appSettings = (AppSettings) applicationContext.getBean(AppSettings.BEAN_NAME);
		}
		return appSettings;
	}

}
