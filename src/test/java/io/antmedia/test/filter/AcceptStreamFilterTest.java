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
import io.antmedia.security.AcceptOnlyStreamsInDataStore;

import org.bytedeco.ffmpeg.global.*;
import org.bytedeco.ffmpeg.avcodec.*;
import org.bytedeco.ffmpeg.avformat.*;
import org.bytedeco.ffmpeg.avutil.*;
import org.bytedeco.ffmpeg.swresample.*;
import org.bytedeco.ffmpeg.swscale.*;

import static org.bytedeco.ffmpeg.global.avutil.*;
import static org.bytedeco.ffmpeg.global.avformat.*;
import static org.bytedeco.ffmpeg.global.avcodec.*;
import static org.bytedeco.ffmpeg.global.avdevice.*;
import static org.bytedeco.ffmpeg.global.swresample.*;
import static org.bytedeco.ffmpeg.global.swscale.*;

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
		
		assertEquals(0,acceptStreamFilterSpy.getMaxFps());
		assertEquals(0,acceptStreamFilterSpy.getMaxResolution());
		assertEquals(0,acceptStreamFilterSpy.getMaxBitrate());
		
		Mockito.doReturn(30).when(acceptStreamFilterSpy).getStreamFps(Mockito.any(),Mockito.any());
		Mockito.doReturn(1080).when(acceptStreamFilterSpy).getStreamResolution(Mockito.any(),Mockito.any());
		Mockito.doReturn(2000000l).when(acceptStreamFilterSpy).getStreamBitrate(Mockito.any(),Mockito.any());
		
		assertEquals(true,acceptStreamFilterSpy.isValidStreamParameters(inputFormatContext,pkt));
		
		// Default Scenario
		
		Mockito.doReturn(100).when(acceptStreamFilterSpy).getMaxFps();
		Mockito.doReturn(1080).when(acceptStreamFilterSpy).getMaxResolution();
		Mockito.doReturn(2000000).when(acceptStreamFilterSpy).getMaxBitrate();
		
		Mockito.doReturn(60).when(acceptStreamFilterSpy).getStreamFps(Mockito.any(),Mockito.any());
		Mockito.doReturn(720).when(acceptStreamFilterSpy).getStreamResolution(Mockito.any(),Mockito.any());
		Mockito.doReturn(1000000l).when(acceptStreamFilterSpy).getStreamBitrate(Mockito.any(),Mockito.any());
		
		assertEquals(true,acceptStreamFilterSpy.isValidStreamParameters(inputFormatContext,pkt));		
		
		// Stream FPS > Max FPS Scenario
		
		Mockito.doReturn(30).when(acceptStreamFilterSpy).getMaxFps();
		Mockito.doReturn(1080).when(acceptStreamFilterSpy).getMaxResolution();
		Mockito.doReturn(2000000).when(acceptStreamFilterSpy).getMaxBitrate();
		
		
		Mockito.doReturn(60).when(acceptStreamFilterSpy).getStreamFps(Mockito.any(),Mockito.any());
		Mockito.doReturn(720).when(acceptStreamFilterSpy).getStreamResolution(Mockito.any(),Mockito.any());
		Mockito.doReturn(1000000l).when(acceptStreamFilterSpy).getStreamBitrate(Mockito.any(),Mockito.any());
		
		assertEquals(false,acceptStreamFilterSpy.isValidStreamParameters(inputFormatContext,pkt));		
		
		// Stream Resolution > Max Resolution Scenario
		Mockito.doReturn(100).when(acceptStreamFilterSpy).getMaxFps();
		Mockito.doReturn(480).when(acceptStreamFilterSpy).getMaxResolution();
		Mockito.doReturn(2000000).when(acceptStreamFilterSpy).getMaxBitrate();
		
		// Stream parameters 
		Mockito.doReturn(60).when(acceptStreamFilterSpy).getStreamFps(Mockito.any(),Mockito.any());
		Mockito.doReturn(720).when(acceptStreamFilterSpy).getStreamResolution(Mockito.any(),Mockito.any());
		Mockito.doReturn(1000000l).when(acceptStreamFilterSpy).getStreamBitrate(Mockito.any(),Mockito.any());
		
		assertEquals(false,acceptStreamFilterSpy.isValidStreamParameters(inputFormatContext,pkt));	
		
		// Stream Bitrate > Max Bitrate Scenario
		Mockito.doReturn(100).when(acceptStreamFilterSpy).getMaxFps();
		Mockito.doReturn(1080).when(acceptStreamFilterSpy).getMaxResolution();
		Mockito.doReturn(2000000).when(acceptStreamFilterSpy).getMaxBitrate();
		
		// Stream parameters 
		Mockito.doReturn(60).when(acceptStreamFilterSpy).getStreamFps(Mockito.any(),Mockito.any());
		Mockito.doReturn(720).when(acceptStreamFilterSpy).getStreamResolution(Mockito.any(),Mockito.any());
		Mockito.doReturn(5000000l).when(acceptStreamFilterSpy).getStreamBitrate(Mockito.any(),Mockito.any());
		
		assertEquals(false,acceptStreamFilterSpy.isValidStreamParameters(inputFormatContext,pkt));	
		
		// Stream Bitrate > Max Bitrate Scenario && getMaxResolutionAccept = null
		Mockito.doReturn(100).when(acceptStreamFilterSpy).getMaxFps();
		Mockito.doReturn(0).when(acceptStreamFilterSpy).getMaxResolution();
		Mockito.doReturn(2000000).when(acceptStreamFilterSpy).getMaxBitrate();
		
		// Stream parameters 
		Mockito.doReturn(60).when(acceptStreamFilterSpy).getStreamFps(Mockito.any(),Mockito.any());
		Mockito.doReturn(720).when(acceptStreamFilterSpy).getStreamResolution(Mockito.any(),Mockito.any());
		Mockito.doReturn(5000000l).when(acceptStreamFilterSpy).getStreamBitrate(Mockito.any(),Mockito.any());
		
		assertEquals(false,acceptStreamFilterSpy.isValidStreamParameters(inputFormatContext,pkt));	
		
		// Normal Scenario & getMaxBitrateAccept = null
		Mockito.doReturn(100).when(acceptStreamFilterSpy).getMaxFps();
		Mockito.doReturn(1080).when(acceptStreamFilterSpy).getMaxResolution();
		Mockito.doReturn(0).when(acceptStreamFilterSpy).getMaxBitrate();
		
		// Stream parameters 
		Mockito.doReturn(60).when(acceptStreamFilterSpy).getStreamFps(Mockito.any(),Mockito.any());
		Mockito.doReturn(720).when(acceptStreamFilterSpy).getStreamResolution(Mockito.any(),Mockito.any());
		Mockito.doReturn(5000000l).when(acceptStreamFilterSpy).getStreamBitrate(Mockito.any(),Mockito.any());
		
		assertEquals(true,acceptStreamFilterSpy.isValidStreamParameters(inputFormatContext,pkt));	
		
		// Normal Scenario & getMaxFpsAccept = null & getMaxBitrateAccept = null
		Mockito.doReturn(0).when(acceptStreamFilterSpy).getMaxFps();
		Mockito.doReturn(1080).when(acceptStreamFilterSpy).getMaxResolution();
		Mockito.doReturn(0).when(acceptStreamFilterSpy).getMaxBitrate();
		
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
