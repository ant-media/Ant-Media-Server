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
import org.bytedeco.javacpp.avcodec.AVPacket;
import org.bytedeco.javacpp.avformat.AVFormatContext;
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

		//reset values in the bean
		getAppSettings().resetDefaults();
		getAppSettings().setAddDateTimeToMp4FileName(false);
	}
	
	
	@Test
	public void testAcceptFilter() {
		
		StreamAcceptFilter acceptStreamFilterSpy = Mockito.spy(acceptStreamFilter);
		
		AppSettings appSettings = new AppSettings();
		
		AVFormatContext inputFormatContext = null;
		AVPacket pkt = null;

		
		AntMediaApplicationAdapter spyAdapter = Mockito.spy(AntMediaApplicationAdapter.class);
		
		DataStore dataStore = new InMemoryDataStore("dbname");
		DataStoreFactory dsf = Mockito.mock(DataStoreFactory.class);
		Mockito.when(dsf.getDataStore()).thenReturn(dataStore);
		spyAdapter.setDataStoreFactory(dsf);
		
		//Mockito.when(acceptStreamFilterSpy.getAppSetting()).thenReturn(getAppSettings());
		
		Mockito.doReturn(appSettings).when(acceptStreamFilterSpy).getAppSettings();
		
		assertEquals(null,acceptStreamFilterSpy.getMaxFpsAccept());
		assertEquals(null,acceptStreamFilterSpy.getMaxResolutionAccept());
		assertEquals(null,acceptStreamFilterSpy.getMaxBitrateAccept());
		
		Mockito.doReturn(30).when(acceptStreamFilterSpy).getStreamFps(Mockito.any(),Mockito.any());
		Mockito.doReturn(1080).when(acceptStreamFilterSpy).getStreamResolution(Mockito.any(),Mockito.any());
		Mockito.doReturn(2000000l).when(acceptStreamFilterSpy).getStreamBitrate(Mockito.any(),Mockito.any());
		
		assertEquals(true,acceptStreamFilterSpy.isValidStreamParameters(inputFormatContext,pkt));
		
		// Default Scenario
		 
		Mockito.when(acceptStreamFilterSpy.getMaxFpsAccept()).thenReturn("100");
		Mockito.when(acceptStreamFilterSpy.getMaxResolutionAccept()).thenReturn("1080");
		Mockito.when(acceptStreamFilterSpy.getMaxBitrateAccept()).thenReturn("2000000");
		
		Mockito.doReturn(60).when(acceptStreamFilterSpy).getStreamFps(Mockito.any(),Mockito.any());
		Mockito.doReturn(720).when(acceptStreamFilterSpy).getStreamResolution(Mockito.any(),Mockito.any());
		Mockito.doReturn(1000000l).when(acceptStreamFilterSpy).getStreamBitrate(Mockito.any(),Mockito.any());
		
		assertEquals(true,acceptStreamFilterSpy.isValidStreamParameters(inputFormatContext,pkt));		
		
		// Stream FPS > Max FPS Scenario
		
		Mockito.when(acceptStreamFilterSpy.getMaxFpsAccept()).thenReturn("30");
		Mockito.when(acceptStreamFilterSpy.getMaxResolutionAccept()).thenReturn("1080");
		Mockito.when(acceptStreamFilterSpy.getMaxBitrateAccept()).thenReturn("2000000");
		
		
		Mockito.doReturn(60).when(acceptStreamFilterSpy).getStreamFps(Mockito.any(),Mockito.any());
		Mockito.doReturn(720).when(acceptStreamFilterSpy).getStreamResolution(Mockito.any(),Mockito.any());
		Mockito.doReturn(1000000l).when(acceptStreamFilterSpy).getStreamBitrate(Mockito.any(),Mockito.any());
		
		assertEquals(false,acceptStreamFilterSpy.isValidStreamParameters(inputFormatContext,pkt));		
		
		// Stream Resolution > Max Resolution Scenario
		
		Mockito.when(acceptStreamFilterSpy.getMaxFpsAccept()).thenReturn("100");
		Mockito.when(acceptStreamFilterSpy.getMaxResolutionAccept()).thenReturn("480");
		Mockito.when(acceptStreamFilterSpy.getMaxBitrateAccept()).thenReturn("2000000");
		
		// Stream parameters 
		Mockito.doReturn(60).when(acceptStreamFilterSpy).getStreamFps(Mockito.any(),Mockito.any());
		Mockito.doReturn(720).when(acceptStreamFilterSpy).getStreamResolution(Mockito.any(),Mockito.any());
		Mockito.doReturn(1000000l).when(acceptStreamFilterSpy).getStreamBitrate(Mockito.any(),Mockito.any());
		
		assertEquals(false,acceptStreamFilterSpy.isValidStreamParameters(inputFormatContext,pkt));	
		
		// Stream Bitrate > Max Bitrate Scenario
		
		Mockito.when(acceptStreamFilterSpy.getMaxFpsAccept()).thenReturn("100");
		Mockito.when(acceptStreamFilterSpy.getMaxResolutionAccept()).thenReturn("1080");
		Mockito.when(acceptStreamFilterSpy.getMaxBitrateAccept()).thenReturn("2000000");
		
		// Stream parameters 
		Mockito.doReturn(60).when(acceptStreamFilterSpy).getStreamFps(Mockito.any(),Mockito.any());
		Mockito.doReturn(720).when(acceptStreamFilterSpy).getStreamResolution(Mockito.any(),Mockito.any());
		Mockito.doReturn(5000000l).when(acceptStreamFilterSpy).getStreamBitrate(Mockito.any(),Mockito.any());
		
		assertEquals(false,acceptStreamFilterSpy.isValidStreamParameters(inputFormatContext,pkt));	
		
		// Stream Bitrate > Max Bitrate Scenario && getMaxResolutionAccept = null
		
		Mockito.when(acceptStreamFilterSpy.getMaxFpsAccept()).thenReturn("100");
		Mockito.when(acceptStreamFilterSpy.getMaxResolutionAccept()).thenReturn(null);
		Mockito.when(acceptStreamFilterSpy.getMaxBitrateAccept()).thenReturn("2000000");
		
		// Stream parameters 
		Mockito.doReturn(60).when(acceptStreamFilterSpy).getStreamFps(Mockito.any(),Mockito.any());
		Mockito.doReturn(720).when(acceptStreamFilterSpy).getStreamResolution(Mockito.any(),Mockito.any());
		Mockito.doReturn(5000000l).when(acceptStreamFilterSpy).getStreamBitrate(Mockito.any(),Mockito.any());
		
		assertEquals(false,acceptStreamFilterSpy.isValidStreamParameters(inputFormatContext,pkt));	
		
		// Normal Scenario & getMaxBitrateAccept = null
		
		Mockito.when(acceptStreamFilterSpy.getMaxFpsAccept()).thenReturn("100");
		Mockito.when(acceptStreamFilterSpy.getMaxResolutionAccept()).thenReturn("1080");
		Mockito.when(acceptStreamFilterSpy.getMaxBitrateAccept()).thenReturn(null);
		
		// Stream parameters 
		Mockito.doReturn(60).when(acceptStreamFilterSpy).getStreamFps(Mockito.any(),Mockito.any());
		Mockito.doReturn(720).when(acceptStreamFilterSpy).getStreamResolution(Mockito.any(),Mockito.any());
		Mockito.doReturn(5000000l).when(acceptStreamFilterSpy).getStreamBitrate(Mockito.any(),Mockito.any());
		
		assertEquals(true,acceptStreamFilterSpy.isValidStreamParameters(inputFormatContext,pkt));	
		
		// Normal Scenario & getMaxFpsAccept = null & getMaxBitrateAccept = null
		
		Mockito.when(acceptStreamFilterSpy.getMaxFpsAccept()).thenReturn(null);
		Mockito.when(acceptStreamFilterSpy.getMaxResolutionAccept()).thenReturn("1080");
		Mockito.when(acceptStreamFilterSpy.getMaxBitrateAccept()).thenReturn(null);
		
		// Stream parameters 
		Mockito.doReturn(60).when(acceptStreamFilterSpy).getStreamFps(Mockito.any(),Mockito.any());
		Mockito.doReturn(720).when(acceptStreamFilterSpy).getStreamResolution(Mockito.any(),Mockito.any());
		Mockito.doReturn(5000000l).when(acceptStreamFilterSpy).getStreamBitrate(Mockito.any(),Mockito.any());

		assertEquals(true,acceptStreamFilterSpy.isValidStreamParameters(inputFormatContext,pkt));	
	}
	
	public AppSettings getAppSettings() {
		if (appSettings == null) {
			appSettings = (AppSettings) applicationContext.getBean(AppSettings.BEAN_NAME);
		}
		return appSettings;
	}

}
