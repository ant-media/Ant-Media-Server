package io.antmedia.test.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.AppSettings;
import io.antmedia.AppSettingsModel;
import io.antmedia.EncoderSettings;
import io.antmedia.datastore.AppSettingsManager;

public class AppSettingsManagerTest {
	String appName = "TestApp";
	String path = "webapps/"+appName+"/WEB-INF/red5-web.properties";
	File settingsFile = new File(path);
	
	@Before
	public void before() {
		assertFalse(settingsFile.exists());
		
		settingsFile.getParentFile().mkdirs();
		try {
			settingsFile.createNewFile();
		} catch (IOException e) {
		
			e.printStackTrace();
		}
		
		assertEquals(0, settingsFile.length());
	}

	@After
	public void after() {
		deleteDir(new File("webapps"));
	}

	
	@Test
	public void testChangeAndGetSettings() {
		
		AppSettingsModel savedSettings = AppSettingsManager.getAppSettings(appName);
		assertEquals(0, savedSettings.getWebRTCFrameRate());
		
		
		AppSettingsModel settings = new AppSettingsModel();
		settings.setMp4MuxingEnabled(true);
		
		ApplicationContext mockContext = mock(ApplicationContext.class);
		AppSettings mockSettings = mock(AppSettings.class);
		AntMediaApplicationAdapter mockApplicationAdapter = mock(AntMediaApplicationAdapter.class);	
		
		when(mockContext.containsBean(AppSettings.BEAN_NAME)).thenReturn(true);
		when(mockContext.getBean(AppSettings.BEAN_NAME)).thenReturn(mockSettings);
		when(mockContext.getApplicationName()).thenReturn(appName);
		when(mockContext.getBean("web.handler")).thenReturn(mockApplicationAdapter);
						
		AppSettingsManager.updateAppSettings(mockContext, settings, false);
		verify(mockSettings, times(1)).setMp4MuxingEnabled(settings.isMp4MuxingEnabled());
		verify(mockApplicationAdapter, times(1)).synchUserVoDFolder(any(), any());
		assertNotEquals(0, settingsFile.length());
		
		savedSettings = AppSettingsManager.getAppSettings(appName);
		assertTrue(savedSettings.isMp4MuxingEnabled());
		assertEquals(5, savedSettings.getHlsListSize());
		assertEquals("", savedSettings.getVodFolder());
		assertEquals(2, savedSettings.getHlsTime());
		assertEquals("", savedSettings.getHlsPlayListType());
		assertEquals(0, savedSettings.getEncoderSettings().size());
		
		
		settings.setHlsListSize(12);
		settings.setVodFolder("/mnt/storage");
		settings.setHlsTime(17);
		settings.setHlsPlayListType("event");
		List<EncoderSettings> encoderSettings = new ArrayList<>();
		encoderSettings.add(new EncoderSettings(720, 2500000, 128000));
		settings.setEncoderSettings(encoderSettings);
		settings.setPreviewOverwrite(false);
		
		AppSettingsManager.updateAppSettings(mockContext, settings, false);
		
		savedSettings = AppSettingsManager.getAppSettings(appName);
		assertEquals(12, savedSettings.getHlsListSize());
		assertEquals("/mnt/storage", savedSettings.getVodFolder());
		assertEquals(17, savedSettings.getHlsTime());
		assertEquals("event", savedSettings.getHlsPlayListType());
		assertEquals(1, savedSettings.getEncoderSettings().size());
		assertEquals(720, savedSettings.getEncoderSettings().get(0).getHeight());
		assertEquals(2500000, savedSettings.getEncoderSettings().get(0).getVideoBitrate());
		assertEquals(128000, savedSettings.getEncoderSettings().get(0).getAudioBitrate());
		
		
			
	}
	
	
	
	
	void deleteDir(File file) {
	    File[] contents = file.listFiles();
	    if (contents != null) {
	        for (File f : contents) {
	            deleteDir(f);
	        }
	    }
	    file.delete();
	}
}
