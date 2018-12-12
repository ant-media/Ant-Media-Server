package io.antmedia.test.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.AppSettings;
import io.antmedia.AppSettingsModel;
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
		
		AppSettingsModel savedSettings = AppSettingsManager.getAppSettings(appName);
		assertTrue(savedSettings.isMp4MuxingEnabled());
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
