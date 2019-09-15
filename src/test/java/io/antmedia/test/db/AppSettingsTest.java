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
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.red5.server.api.IContext;
import org.red5.server.api.scope.IScope;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.AppSettings;
import io.antmedia.AppSettingsModel;
import io.antmedia.EncoderSettings;
import io.antmedia.datastore.db.InMemoryDataStore;
import io.antmedia.security.AcceptOnlyStreamsInDataStore;

public class AppSettingsTest {
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
	public void testZeroEncoderSettings() {
		
		AppSettingsModel settings = new AppSettingsModel();
		
		AppSettings mockSettings = Mockito.spy(new AppSettings());
		
		AntMediaApplicationAdapter mockApplicationAdapter = Mockito.spy(new AntMediaApplicationAdapter());			
		mockApplicationAdapter.setAppSettings(mockSettings);
		
		Mockito.doReturn(new InMemoryDataStore("")).when(mockApplicationAdapter).getDataStore();
		
		IContext context = mock(IContext.class);
		Mockito.doReturn(context).when(mockApplicationAdapter).getContext();
		when(context.getBean(AcceptOnlyStreamsInDataStore.BEAN_NAME)).thenReturn(mock(AcceptOnlyStreamsInDataStore.class));
		
		
		IScope scope = mock(IScope.class);
		when(scope.getName()).thenReturn(appName);
		Mockito.doReturn(scope).when(mockApplicationAdapter).getScope();		
		
		//null case
		assertTrue(mockApplicationAdapter.updateSettings(settings));
		verify(mockSettings, times(1)).setAdaptiveResolutionList(null);
		
		
		List<EncoderSettings> encoderSettings = new ArrayList<>();
		encoderSettings.add(new EncoderSettings(720, 2500000, 128000)); //correct 
		encoderSettings.add(new EncoderSettings(0, 2500000, 128000)); //wrong setting
		encoderSettings.add(new EncoderSettings(720, 0, 128000)); //wrong setting
		encoderSettings.add(new EncoderSettings(720, 2500000, 0)); //wrong setting
		settings.setEncoderSettings(encoderSettings);
		
		
		assertTrue(mockApplicationAdapter.updateSettings(settings));
		
		AppSettings savedSettings = mockApplicationAdapter.getAppSettings();
		
		assertEquals(1, savedSettings.getEncoderSettings().size()); //wrong settings not applied, it is 1
		assertEquals(720, savedSettings.getEncoderSettings().get(0).getHeight());
		assertEquals(2500000, savedSettings.getEncoderSettings().get(0).getVideoBitrate());
		assertEquals(128000, savedSettings.getEncoderSettings().get(0).getAudioBitrate());
		
		
		
		ArgumentCaptor<List<EncoderSettings>> encoderSettingsCapture = ArgumentCaptor.forClass(List.class);
		verify(mockSettings, times(2)).setAdaptiveResolutionList(encoderSettingsCapture.capture());
		
		List<EncoderSettings> encoderSettings2 = encoderSettingsCapture.getValue();
		
		assertEquals(720, encoderSettings2.get(0).getHeight());
		assertEquals(2500000, encoderSettings2.get(0).getVideoBitrate());
		assertEquals(128000, encoderSettings2.get(0).getAudioBitrate());
		
	}
	
	@Test
	public void testChangeAndGetSettings() {
		
		
		AppSettingsModel settings = new AppSettingsModel();
		settings.setMp4MuxingEnabled(true);
		
		AppSettings mockSettings = Mockito.spy(new AppSettings());
		AntMediaApplicationAdapter mockApplicationAdapter = Mockito.spy(new AntMediaApplicationAdapter());	
		
		mockApplicationAdapter.setAppSettings(mockSettings);
		
		Mockito.doReturn(new InMemoryDataStore("")).when(mockApplicationAdapter).getDataStore();
		
		IContext context = mock(IContext.class);
		Mockito.doReturn(context).when(mockApplicationAdapter).getContext();
		when(context.getBean(AcceptOnlyStreamsInDataStore.BEAN_NAME)).thenReturn(mock(AcceptOnlyStreamsInDataStore.class));
		
		
		IScope scope = mock(IScope.class);
		when(scope.getName()).thenReturn(appName);
		Mockito.doReturn(scope).when(mockApplicationAdapter).getScope();		
								
		mockApplicationAdapter.updateSettings(settings);
		
		verify(mockSettings, times(1)).setMp4MuxingEnabled(settings.isMp4MuxingEnabled());
		verify(mockApplicationAdapter, times(1)).synchUserVoDFolder(any(), any());
		assertNotEquals(0, settingsFile.length());
		
		AppSettings savedSettings = mockApplicationAdapter.getAppSettings();
		assertTrue(savedSettings.isMp4MuxingEnabled());
		assertEquals("5", savedSettings.getHlsListSize());
		assertEquals("", savedSettings.getVodFolder());
		assertEquals("1", savedSettings.getHlsTime());
		assertEquals("", savedSettings.getHlsPlayListType());
		assertEquals(0, savedSettings.getEncoderSettings().size());
		
		
		settings.setHlsListSize(12);
		settings.setVodFolder("/mnt/storage");
		settings.setHlsTime(17);
		settings.setHlsPlayListType("event");
		List<EncoderSettings> encoderSettings = new ArrayList<>();
		encoderSettings.add(new EncoderSettings(720, 2500000, 128000)); //correct 
		encoderSettings.add(new EncoderSettings(0, 2500000, 128000)); //wrong setting
		encoderSettings.add(new EncoderSettings(720, 0, 128000)); //wrong setting
		encoderSettings.add(new EncoderSettings(720, 2500000, 0)); //wrong setting
		settings.setEncoderSettings(encoderSettings);
		settings.setPreviewOverwrite(false);
		
		mockApplicationAdapter.updateSettings(settings);
		
		savedSettings = mockApplicationAdapter.getAppSettings();
		assertEquals("12", savedSettings.getHlsListSize());
		assertEquals("/mnt/storage", savedSettings.getVodFolder());
		assertEquals("17", savedSettings.getHlsTime());
		assertEquals("event", savedSettings.getHlsPlayListType());
		assertEquals(1, savedSettings.getEncoderSettings().size()); //wrong settings not applied, it is 1
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
