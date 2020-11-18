package io.antmedia.test.filter;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.AppSettings;
import io.antmedia.datastore.db.DataStore;
import io.antmedia.datastore.db.DataStoreFactory;
import io.antmedia.datastore.db.InMemoryDataStore;
import io.antmedia.filter.StreamAcceptFilter;

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
		String streamId = "test-gg";
		
		AntMediaApplicationAdapter spyAdapter = Mockito.spy(AntMediaApplicationAdapter.class);
		
		DataStore dataStore = new InMemoryDataStore("dbname");
		DataStoreFactory dsf = Mockito.mock(DataStoreFactory.class);
		Mockito.when(dsf.getDataStore()).thenReturn(dataStore);
		spyAdapter.setDataStoreFactory(dsf);
		
		Mockito.doReturn(appSettings).when(acceptStreamFilterSpy).getAppSettings();
		
		assertEquals(0,acceptStreamFilterSpy.getMaxFps());
		assertEquals(0,acceptStreamFilterSpy.getMaxResolution());
		assertEquals(0,acceptStreamFilterSpy.getMaxBitrate());
		
		assertEquals(true,acceptStreamFilterSpy.isValidStreamParameters(1920, 1080, 30, 2000000,streamId));
		
		// Default Scenario
		
		Mockito.doReturn(100).when(acceptStreamFilterSpy).getMaxFps();
		Mockito.doReturn(1080).when(acceptStreamFilterSpy).getMaxResolution();
		Mockito.doReturn(2000000).when(acceptStreamFilterSpy).getMaxBitrate();
		
		assertEquals(true,acceptStreamFilterSpy.isValidStreamParameters(1280, 720, 60, 1000000,streamId));		
		
		// Stream FPS > Max FPS Scenario
		
		Mockito.doReturn(30).when(acceptStreamFilterSpy).getMaxFps();
		Mockito.doReturn(1080).when(acceptStreamFilterSpy).getMaxResolution();
		Mockito.doReturn(2000000).when(acceptStreamFilterSpy).getMaxBitrate();
		
		
		assertEquals(false,acceptStreamFilterSpy.isValidStreamParameters(1280, 720, 60, 1000000,streamId));		
		
		// Stream Resolution > Max Resolution Scenario
		Mockito.doReturn(100).when(acceptStreamFilterSpy).getMaxFps();
		Mockito.doReturn(480).when(acceptStreamFilterSpy).getMaxResolution();
		Mockito.doReturn(2000000).when(acceptStreamFilterSpy).getMaxBitrate();
			
		assertEquals(false,acceptStreamFilterSpy.isValidStreamParameters(1280, 720, 60, 1000000,streamId));	
		
		// Stream Bitrate > Max Bitrate Scenario
		Mockito.doReturn(100).when(acceptStreamFilterSpy).getMaxFps();
		Mockito.doReturn(1080).when(acceptStreamFilterSpy).getMaxResolution();
		Mockito.doReturn(2000000).when(acceptStreamFilterSpy).getMaxBitrate();
		
		assertEquals(false,acceptStreamFilterSpy.isValidStreamParameters(1280, 720, 60, 5000000,streamId));	
		
		// Stream Bitrate > Max Bitrate Scenario && getMaxResolutionAccept = null
		Mockito.doReturn(100).when(acceptStreamFilterSpy).getMaxFps();
		Mockito.doReturn(0).when(acceptStreamFilterSpy).getMaxResolution();
		Mockito.doReturn(2000000).when(acceptStreamFilterSpy).getMaxBitrate();
			
		assertEquals(false,acceptStreamFilterSpy.isValidStreamParameters(1280, 720, 60, 5000000,streamId));	
		
		// Normal Scenario & getMaxBitrateAccept = null
		Mockito.doReturn(100).when(acceptStreamFilterSpy).getMaxFps();
		Mockito.doReturn(1080).when(acceptStreamFilterSpy).getMaxResolution();
		Mockito.doReturn(0).when(acceptStreamFilterSpy).getMaxBitrate();
		
		assertEquals(true,acceptStreamFilterSpy.isValidStreamParameters(1280, 720, 60, 1000000, streamId));	
		
		// Normal Scenario & getMaxFpsAccept = null & getMaxBitrateAccept = null
		Mockito.doReturn(0).when(acceptStreamFilterSpy).getMaxFps();
		Mockito.doReturn(1080).when(acceptStreamFilterSpy).getMaxResolution();
		Mockito.doReturn(0).when(acceptStreamFilterSpy).getMaxBitrate();
		
		assertEquals(true,acceptStreamFilterSpy.isValidStreamParameters(1280, 720, 60, 5000000, streamId));	
		
		// For the Stream Planned Start / End Data Parameters Scenarios
		// Normal Scenario Stream Parameters which are getMaxFpsAccept = null & getMaxResolution = null & getMaxBitrateAccept = null 
		Mockito.doReturn(0).when(acceptStreamFilterSpy).getMaxFps();
		Mockito.doReturn(0).when(acceptStreamFilterSpy).getMaxResolution();
		Mockito.doReturn(0).when(acceptStreamFilterSpy).getMaxBitrate();
		
	}
	
	public AppSettings getAppSettings() {
		if (appSettings == null) {
			appSettings = (AppSettings) applicationContext.getBean(AppSettings.BEAN_NAME);
		}
		return appSettings;
	}

}
