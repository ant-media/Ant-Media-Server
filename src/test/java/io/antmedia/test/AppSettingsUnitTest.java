package io.antmedia.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

import org.apache.catalina.util.NetMask;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Test;
import org.red5.server.scope.WebScope;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.AppSettings;
import io.antmedia.EncoderSettings;
import io.antmedia.rest.RestServiceBase;

@ContextConfiguration(locations = { "test.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class AppSettingsUnitTest extends AbstractJUnit4SpringContextTests {

	
	protected WebScope appScope;
	static {
		System.setProperty("red5.deployment.type", "junit");
		System.setProperty("red5.root", ".");
		
	}
	
	@Test
	public void testDefaultSettings() 
	{
		if (appScope == null) 
		{
			appScope = (WebScope) applicationContext.getBean("web.scope");
			assertTrue(appScope.getDepth() == 1);
		}
		
		AppSettings appSettings = (AppSettings) applicationContext.getBean("app.settings");
		
		assertEquals("stun:stun1.l.google.com:19302", appSettings.getStunServerURI());
		assertEquals(false, appSettings.isWebRTCTcpCandidatesEnabled());
		assertNull(appSettings.getEncoderName());
		assertEquals(480, appSettings.getPreviewHeight());
		assertFalse(appSettings.isUseOriginalWebRTCEnabled());
		assertEquals(5000, appSettings.getCreatePreviewPeriod());
		
		//check default value
		assertEquals(false, appSettings.isForceAspectRatioInTranscoding());
		appSettings.setForceAspectRatioInTranscoding(true);
		assertEquals(true, appSettings.isForceAspectRatioInTranscoding());
		appSettings.setForceAspectRatioInTranscoding(false);
		assertEquals(false, appSettings.isForceAspectRatioInTranscoding());
		
		List<NetMask> allowedCIDRList = appSettings.getAllowedCIDRList();
		System.out.println("allowedCIDRList ->" + allowedCIDRList.size());
	}
	
	@Test
	public void testAppSettingsFileWebhookAuthenticateURL() {
		AppSettings appSettings = new AppSettings();
		
		File f = new File("webapps/junit/WEB-INF/");
		f.mkdirs();
		File propertiesFile = new File(f.getAbsolutePath(), "red5-web.properties");
		propertiesFile.delete();
		
		
		try {
			f.createNewFile();
			AntMediaApplicationAdapter.updateAppSettingsFile("junit", appSettings);
			BufferedReader br = new BufferedReader(new FileReader(propertiesFile.getAbsolutePath()));
			
			String readLine=null;
			while ((readLine = br.readLine()) != null) {
				assertNotEquals("settings.webhookAuthenticateURL=null", readLine);
			}
			
			br.close();
			
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		
	}
	
	/*
	@Test
	public void testXMLApplication() {
		
		XmlWebApplicationContext applicationContext = new XmlWebApplicationContext();
		    applicationContext.setConfigLocations(
		            "red5-web.xml");
		    applicationContext.setServletContext(new MockServletContext(new ResourceLoader() {
				
				@Override
				public Resource getResource(String location) {
					return new FileSystemResource("src/test/resources/WEB-INF/xml/" + location);
				}
				
				@Override
				public ClassLoader getClassLoader() {
					return getClassLoader();
				}
			}));
		    applicationContext.refresh();
		    
		    
		    assertNotNull(applicationContext);
		    
		   
		
		 
	}
	*/
	
	
	@Test
	public void testEncodeSettings() {
		AppSettings appSettings = new AppSettings();
		int height1 = 480;
		int videoBitrate1= 500000;
		int audioBitrate1 = 128000;
		boolean forceEncode1 = false;
		
		int height2 = 360;
		int videoBitrate2 = 400000;
		int audioBitrate2 = 64000;
		boolean forceEncode2 = true;
		
		int height3 = 240;
		int videoBitrate3 = 300000;
		int audioBitrate3 = 32000;
		boolean forceEncode3 = false;
		
			
		//Try with new format settings
		String newFormatEncoderSettingString ="[{\"videoBitrate\":"+videoBitrate1+",\"forceEncode\":"+forceEncode1+",\"audioBitrate\":"+audioBitrate1+",\"height\":"+height1+"},{\"videoBitrate\":"+videoBitrate2+",\"forceEncode\":"+forceEncode2+",\"audioBitrate\":"+audioBitrate2+",\"height\":"+height2+"},{\"videoBitrate\":"+videoBitrate3+",\"forceEncode\":"+forceEncode3+",\"audioBitrate\":"+audioBitrate3+",\"height\":"+height3+"}]";
		
		List<EncoderSettings> list = AppSettings.encodersStr2List(newFormatEncoderSettingString);
		
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
		
		assertEquals(newFormatEncoderSettingString, appSettings.encodersList2Str(list));
		
		//Try with old format settings
		String oldFormatEncoderSettingString = height1+"," + videoBitrate1 + "," + audioBitrate1
				+ "," + height2 +"," + videoBitrate2 + "," + audioBitrate2
				+ "," + height3 +"," + videoBitrate3 + "," + audioBitrate3;
		list = AppSettings.encodersStr2List(oldFormatEncoderSettingString);
		
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
		
		//It will convert new json format
		list.get(0).setForceEncode(false);
		list.get(1).setForceEncode(true);
		list.get(2).setForceEncode(false);
		assertEquals(newFormatEncoderSettingString, appSettings.encodersList2Str(list));
	}
	
	

	@Test
	public void isCommunity() {
		assertFalse(RestServiceBase.isEnterprise());
	}
	
	@Test
	public void testDefaultValues() {		
		AppSettings appSettings = new AppSettings();
		appSettings.resetDefaults();
		assertFalse(appSettings.isMp4MuxingEnabled());
		assertFalse(appSettings.isAddDateTimeToMp4FileName());
		assertTrue(appSettings.isHlsMuxingEnabled());
		assertFalse(appSettings.isWebRTCEnabled());
		assertTrue(appSettings.isDeleteHLSFilesOnEnded());
		assertFalse(appSettings.isMp4MuxingEnabled());
		assertNull(appSettings.getHlsListSize());
		assertNull(appSettings.getHlsTime());
		assertNull(appSettings.getHlsPlayListType());
		assertTrue(appSettings.getEncoderSettings().isEmpty());
	}
	
	@Test
	public void testEncoderSettingsAtStartUp() {
		AppSettings appSettings = new AppSettings();
		String encSettings = "480,500000,96000,240,300000,64000";
		assertNull(appSettings.getEncoderSettings());
		appSettings.setEncoderSettingsString(encSettings);
		assertNotNull(appSettings.getEncoderSettings());
		assertEquals(2, appSettings.getEncoderSettings().size());
	}

}
