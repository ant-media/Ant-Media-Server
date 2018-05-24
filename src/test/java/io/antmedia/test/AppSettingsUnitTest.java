package io.antmedia.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import org.junit.Test;
import org.red5.server.Launcher;

import io.antmedia.AppSettings;
import io.antmedia.EncoderSettings;
import io.antmedia.rest.BroadcastRestService;

public class AppSettingsUnitTest {
	
	@Test
	public void testEncodeSettings() {
		AppSettings appSettings = new AppSettings();
		int height1 = 480;
		int videoBitrate1= 500000;
		int audioBitrate1 = 128000;
		
		int height2 = 360;
		int videoBitrate2 = 400000;
		int audioBitrate2 = 64000;
		
		int height3 = 240;
		int videoBitrate3 = 300000;
		int audioBitrate3 = 32000;
		String encoderSettingString = height1+"," + videoBitrate1 + "," + audioBitrate1
				+ "," + height2 +"," + videoBitrate2 + "," + audioBitrate2
				+ "," + height3 +"," + videoBitrate3 + "," + audioBitrate3;
		List<EncoderSettings> list = AppSettings.getEncoderSettingsList(encoderSettingString);
		
	
		
		assertEquals(3, list.size());
		assertEquals(480, list.get(0).getHeight());
		assertEquals(500000, list.get(0).getVideoBitrate());
		assertEquals(128000, list.get(0).getAudioBitrate());
		
		assertEquals(360, list.get(1).getHeight());
		assertEquals(400000, list.get(1).getVideoBitrate());
		assertEquals(64000, list.get(1).getAudioBitrate());
		
		assertEquals(240, list.get(2).getHeight());
		assertEquals(300000, list.get(2).getVideoBitrate());
		assertEquals(32000, list.get(2).getAudioBitrate());
		
		assertEquals(encoderSettingString, appSettings.getEncoderSettingsString(list));
	}
	
	
	@Test
	public void testReadWriteSimple() {
			Launcher launcher = new Launcher();
			File f = new File("testFile");
			String content = "contentntntnt";
			launcher.writeToFile(f.getAbsolutePath(), content);
			
			String fileContent = launcher.getFileContent(f.getAbsolutePath());
			
			assertEquals(fileContent, content);
			
			try {
				Files.delete(f.toPath());
			} catch (IOException e) {
				e.printStackTrace();
			}
	}
	
	@Test
	public void isCommunity() {
		assertFalse(BroadcastRestService.isEnterprise());
	}
	
	@Test
	public void testDefaultValues() {
		AppSettings appSettings = new AppSettings();
		
		assertFalse(appSettings.isMp4MuxingEnabled());
		assertFalse(appSettings.isAddDateTimeToMp4FileName());
		assertTrue(appSettings.isHlsMuxingEnabled());
		assertFalse(appSettings.isWebRTCEnabled());
		assertTrue(appSettings.isDeleteHLSFilesOnExit());
		assertFalse(appSettings.isMp4MuxingEnabled());
		assertNull(appSettings.getHlsListSize());
		assertNull(appSettings.getHlsTime());
		assertNull(appSettings.getHlsPlayListType());
		assertNull(appSettings.getAdaptiveResolutionList());
		

				
	}

}
