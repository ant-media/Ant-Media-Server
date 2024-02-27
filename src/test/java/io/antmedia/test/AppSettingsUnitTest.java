package io.antmedia.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Queue;

import org.apache.catalina.util.NetMask;
import org.apache.commons.lang3.RandomUtils;
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
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
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
		
		assertEquals("0.5", appSettings.getDashFragmentDuration());
		assertEquals("6", appSettings.getDashSegDuration());
		
		assertEquals("stun:stun1.l.google.com:19302", appSettings.getStunServerURI());
		assertEquals(false, appSettings.isWebRTCTcpCandidatesEnabled());
		assertEquals("", appSettings.getEncoderName());
		assertEquals(480, appSettings.getPreviewHeight());
		assertFalse(appSettings.isUseOriginalWebRTCEnabled());
		assertEquals(5000, appSettings.getCreatePreviewPeriod());
		
		//check default value
		assertEquals(false, appSettings.isForceAspectRatioInTranscoding());
		appSettings.setForceAspectRatioInTranscoding(true);
		assertEquals(true, appSettings.isForceAspectRatioInTranscoding());
		appSettings.setForceAspectRatioInTranscoding(false);
		assertEquals(false, appSettings.isForceAspectRatioInTranscoding());
		
		Queue<NetMask> allowedCIDRList = appSettings.getAllowedCIDRList();
		System.out.println("allowedCIDRList ->" + allowedCIDRList.size());
		
		assertEquals("%r%b",appSettings.getFileNameFormat());
		
		appSettings.setFileNameFormat(null);
		assertNull(appSettings.getFileNameFormat());
		
		
		assertEquals("", appSettings.getTurnServerUsername());
		appSettings.setTurnServerUsername("turnserverusername");
		assertEquals("turnserverusername", appSettings.getTurnServerUsername());
		
		assertEquals("", appSettings.getTurnServerCredential());
		appSettings.setTurnServerCredential("turnservercredential");
		assertEquals("turnservercredential", appSettings.getTurnServerCredential());
		
		
		assertEquals(false, appSettings.isRtmpPlaybackEnabled());
		appSettings.setRtmpPlaybackEnabled(true);
		assertEquals(true, appSettings.isRtmpPlaybackEnabled());
		appSettings.setRtmpPlaybackEnabled(false);
		
	}

	@Test
	public void testUseAsSignalingSetting(){
		AppSettings settings = new AppSettings();

		settings.setSignalingEnabled(true);
		assertTrue(settings.isSignalingEnabled());

		settings.setSignalingAddress("192.168.0.1");
		assertEquals("192.168.0.1",settings.getSignalingAddress());
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
		assertTrue(appSettings.isPlayWebRTCStreamOnceForEachSession());
	}
	
	@Test
	public void testEncoderSettingsAtStartUp() {
		AppSettings appSettings = new AppSettings();
		String encSettings = "480,500000,96000,240,300000,64000";
		assertEquals(0, appSettings.getEncoderSettings().size());
		appSettings.setEncoderSettingsString(encSettings);
		assertNotNull(appSettings.getEncoderSettings());
		assertEquals(2, appSettings.getEncoderSettings().size());
	}
	
	@Test
	public void testSettings() {
		AppSettings appSettings = new AppSettings();
		appSettings.setMaxAudioTrackCount(5);
		assertEquals(5, appSettings.getMaxAudioTrackCount());
		
		appSettings.setMaxVideoTrackCount(10);
		assertEquals(10, appSettings.getMaxVideoTrackCount());
		
		int idleTimeOut = RandomUtils.nextInt();
		appSettings.setOriginEdgeIdleTimeout(idleTimeOut);
		assertEquals(idleTimeOut, appSettings.getOriginEdgeIdleTimeout());
		
		appSettings.setAddDateTimeToHlsFileName(true);
		assertEquals(true, appSettings.isAddDateTimeToHlsFileName());

		appSettings.setPlayWebRTCStreamOnceForEachSession(false);
		assertFalse(appSettings.isPlayWebRTCStreamOnceForEachSession());

		appSettings.setStatsBasedABREnabled(false);
		assertEquals(false, appSettings.isStatsBasedABREnabled());
		appSettings.setAbrDownScalePacketLostRatio(2);
		assertEquals(2, appSettings.getAbrDownScalePacketLostRatio(), 0.0001);
		appSettings.setAbrUpScalePacketLostRatio(0.2f);
		assertEquals(0.2, appSettings.getAbrUpScalePacketLostRatio(), 0.0001);
		appSettings.setAbrUpScaleJitterMs(50);
		assertEquals(50, appSettings.getAbrUpScaleJitterMs(), 0.0001);
		appSettings.setAbrUpScaleRTTMs(100);
		assertEquals(100, appSettings.getAbrUpScaleRTTMs(), 0.0001);
		
		appSettings.setSendAudioLevelToViewers(true);
		assertTrue(appSettings.isSendAudioLevelToViewers());
		
		appSettings.setSendAudioLevelToViewers(false);
		assertFalse(appSettings.isSendAudioLevelToViewers());
		
		appSettings.setTimeTokenSecretForPlay("secretplay");
		assertEquals("secretplay", appSettings.getTimeTokenSecretForPlay());
		
		
		appSettings.setTimeTokenSecretForPublish("secretpublish");
		assertEquals("secretpublish", appSettings.getTimeTokenSecretForPublish());

		
		assertEquals(true, appSettings.isHwScalingEnabled());
		appSettings.setHwScalingEnabled(false);
		assertEquals(false, appSettings.isHwScalingEnabled());

		String apnKeyId = "apnkeyid";
		appSettings.setApnKeyId(apnKeyId);
		assertEquals(apnKeyId, appSettings.getApnKeyId());
		
		String teamId = "apnTeamId";
		appSettings.setApnTeamId(teamId);
		assertEquals(teamId, appSettings.getApnTeamId());
		
		String apnServer = "apnServer";
		appSettings.setApnsServer(apnServer);
		assertEquals(apnServer, appSettings.getApnsServer());
		
		String privateKey = "privateKey";
		appSettings.setApnPrivateKey(privateKey);
		assertEquals(privateKey, appSettings.getApnPrivateKey());

		int webHookRetryCount = 2;
		appSettings.setWebhookRetryCount(webHookRetryCount);
		assertEquals(webHookRetryCount, appSettings.getWebhookRetryCount());

		long webHookRetryDelay = 2000;
		appSettings.setWebhookRetryDelay(webHookRetryDelay);
		assertEquals(webHookRetryDelay, appSettings.getWebhookRetryDelay());

	}
	
	
	@Test
	public void testDefaultAppSettings() {
		testUnsetAppSettings(new AppSettings());
	}
	
	@Test
	public void testBeanAppSettings() {
		testUnsetAppSettings((AppSettings) applicationContext.getBean("app.settings"));
	}
	
	
	
	
	public void testUnsetAppSettings(AppSettings appSettings) {
		
		Field[] declaredFields = appSettings.getClass().getDeclaredFields();
		
		int numberOfFields = 0;
		for (Field field : declaredFields) 
		{     
           
           if (!Modifier.isFinal(field.getModifiers()) && !Modifier.isStatic(field.getModifiers())) 
           {
        	   numberOfFields++;
			}
		}
		
		
		Method[] methods = appSettings.getClass().getMethods();
		
		for (Method method: methods) {
			if (method.getName().startsWith("get") || method.getName().startsWith("is")) {
				System.out.println("assertEquals(null, appSettings."+method.getName()+"());");
			}
		}
		
		
		//check the default values of all settings in order to not encounter any problem again
		// dbId
		//no need to check dbID
		
		assertEquals(4, appSettings.getExcessiveBandwithTryCountBeforeSwitchback());
		assertEquals(20, appSettings.getRttMeasurementDiffThresholdForSwitchback());
		assertEquals(true, appSettings.isWriteStatsToDatastore());
		assertEquals(false, appSettings.isDashMuxingEnabled());
		assertEquals("", appSettings.getListenerHookURL());
		assertEquals(false, appSettings.isObjectDetectionEnabled());
		assertEquals("", appSettings.getVodFolder());
		assertEquals(false, appSettings.isPreviewOverwrite());
		assertEquals("", appSettings.getStalkerDBServer());
		assertEquals("", appSettings.getStalkerDBUsername());
		assertEquals("", appSettings.getStalkerDBPassword());
		assertEquals(0, appSettings.getStreamFetcherBufferTime());
		assertEquals("delete_segments", appSettings.getHlsflags());
		assertEquals("/usr/local/antmedia/mysql", appSettings.getMySqlClientPath());
		assertEquals(false, appSettings.isPlayTokenControlEnabled());
		assertEquals(false, appSettings.isTimeTokenSubscriberOnly());
		assertEquals(false, appSettings.isEnableTimeTokenForPlay());
		assertEquals("", appSettings.getMuxerFinishScript());
		assertEquals(30, appSettings.getWebRTCFrameRate());
		assertEquals("", appSettings.getTokenHashSecret());
		assertEquals(false, appSettings.isHashControlPlayEnabled());
		assertEquals(60000, appSettings.getWebRTCPortRangeMax());
		assertEquals(50000, appSettings.getWebRTCPortRangeMin());
		assertEquals("", appSettings.getEncoderPreset());
		assertEquals("", appSettings.getEncoderProfile());
		assertEquals("", appSettings.getEncoderLevel());
		assertEquals("", appSettings.getEncoderRc());
		assertEquals("", appSettings.getEncoderSpecific());
		assertEquals("", appSettings.getAllowedPublisherCIDR());
		assertEquals(300000, appSettings.getExcessiveBandwidthValue());
		assertEquals(0, appSettings.getPortAllocatorFlags());
		assertEquals(0, appSettings.getUpdateTime());
		assertEquals(5000, appSettings.getEncodingTimeout());
		assertEquals(false, appSettings.isDefaultDecodersEnabled());
		assertEquals("", appSettings.getHttpForwardingExtension());
		assertEquals("", appSettings.getHttpForwardingBaseURL());
		assertEquals(1500, appSettings.getMaxAnalyzeDurationMS());
		assertEquals(false, appSettings.isGeneratePreview());
		assertEquals(true, appSettings.isDisableIPv6Candidates());
		assertEquals("3", appSettings.getRtspPullTransportType());
		assertEquals(5000, appSettings.getRtspTimeoutDurationMs());
		assertEquals(0, appSettings.getMaxResolutionAccept());
		assertEquals(true, appSettings.isH264Enabled());
		assertEquals(false, appSettings.isVp8Enabled());
		assertEquals(false, appSettings.isH265Enabled());
		assertEquals(true, appSettings.isDataChannelEnabled());
		assertEquals(0, appSettings.getRtmpIngestBufferTimeMs());
		assertEquals("", appSettings.getDataChannelWebHookURL());
		assertEquals(0, appSettings.getEncoderThreadCount());
		assertEquals(0, appSettings.getEncoderThreadType());
		assertEquals(null, appSettings.getH265EncoderProfile());
		assertEquals(null, appSettings.getH265EncoderPreset());
		assertEquals(null, appSettings.getH265EncoderLevel());
		assertEquals(null, appSettings.getH265EncoderSpecific());
		assertEquals(null, appSettings.getH265EncoderRc());
		assertEquals(4, appSettings.getVp8EncoderSpeed());
		assertEquals("realtime", appSettings.getVp8EncoderDeadline());
		assertEquals(1, appSettings.getVp8EncoderThreadCount());
		assertEquals("unifiedPlan", appSettings.getWebRTCSdpSemantics());
		assertEquals(true, appSettings.isDeleteDASHFilesOnEnded());
		assertEquals(360, appSettings.getHeightRtmpForwarding());
		assertEquals(96000, appSettings.getAudioBitrateSFU());
		assertEquals(true, appSettings.isAacEncodingEnabled());
		assertEquals("23", appSettings.getConstantRateFactor());
		assertEquals(-1, appSettings.getWebRTCViewerLimit());
		assertEquals("", appSettings.getJwtSecretKey());
		assertEquals(false, appSettings.isJwtControlEnabled());
		assertEquals(true, appSettings.isIpFilterEnabled());
		assertEquals(-1, appSettings.getIngestingStreamLimit());
		assertEquals(60, appSettings.getTimeTokenPeriod());
		assertEquals(false, appSettings.isToBeDeleted());
		assertEquals(false, appSettings.isPullWarFile());
		assertEquals("", appSettings.getJwtStreamSecretKey());
		assertEquals(false, appSettings.isPublishJwtControlEnabled());
		assertEquals(false, appSettings.isPlayJwtControlEnabled());
		assertEquals("", appSettings.getDashHttpEndpoint());
		assertEquals(false, appSettings.isS3RecordingEnabled());
		assertEquals("", appSettings.getS3SecretKey());
		assertEquals("", appSettings.getS3AccessKey());
		assertEquals("", appSettings.getS3RegionName());
		assertEquals("", appSettings.getS3BucketName());
		assertEquals("no-store, no-cache, must-revalidate, max-age=0", appSettings.getS3CacheControl());
		assertEquals("", appSettings.getS3Endpoint());
		assertEquals(false, appSettings.isForceDecoding());
		assertEquals(true, appSettings.isAddOriginalMuxerIntoHLSPlaylist());
		assertEquals("", appSettings.getWebhookAuthenticateURL());
		assertEquals("", appSettings.getVodUploadFinishScript());
		assertEquals("%r%b", appSettings.getFileNameFormat());
		assertEquals(false, appSettings.isSignalingEnabled());
		assertEquals("", appSettings.getSignalingAddress());
		assertEquals(false, appSettings.isMp4MuxingEnabled());
		assertEquals(false, appSettings.isAddDateTimeToMp4FileName());
		assertEquals(true, appSettings.isHlsMuxingEnabled());
		assertEquals(true, appSettings.isWebRTCEnabled());
		assertEquals(true, appSettings.isDeleteHLSFilesOnEnded());
		assertEquals("5", appSettings.getHlsListSize());
		assertEquals("", appSettings.getHlsPlayListType());
		assertEquals(0, appSettings.getEncoderSettings().size());
		assertEquals(7, appSettings.getUploadExtensionsToS3());
		assertEquals("STANDARD", appSettings.getS3StorageClass());
		assertEquals(3, appSettings.getEndpointRepublishLimit());
		assertEquals("6", appSettings.getDashSegDuration());
		assertEquals("0.5", appSettings.getDashFragmentDuration());
		assertEquals("3.5", appSettings.getTargetLatency());
		assertEquals("5", appSettings.getDashWindowSize());
		assertEquals("5", appSettings.getDashExtraWindowSize());
		assertEquals(true, appSettings.islLDashEnabled());
		assertEquals(false, appSettings.islLHLSEnabled());
		assertEquals(false, appSettings.isHlsEnabledViaDash());
		assertEquals(false, appSettings.isUseTimelineDashMuxing());
		assertEquals(2000, appSettings.getWebRTCKeyframeTime());
		assertEquals(true, appSettings.isDashHttpStreaming());
		assertEquals("streams", appSettings.getS3StreamsFolderPath());
		assertEquals("previews", appSettings.getS3PreviewsFolderPath());
		assertEquals("public-read", appSettings.getS3Permission());
		assertEquals("127.0.0.1", appSettings.getRemoteAllowedCIDR());
		assertEquals(false, appSettings.isWebMMuxingEnabled());
		assertEquals("", appSettings.getEncoderSettingsString());
		assertEquals("127.0.0.1", appSettings.getAllowedCIDRList().poll().toString());
		assertEquals(false, appSettings.isUseOriginalWebRTCEnabled());
		assertEquals(5000, appSettings.getCreatePreviewPeriod());
		assertEquals("stun:stun1.l.google.com:19302", appSettings.getStunServerURI());
		assertEquals("", appSettings.getEncoderName());
		assertEquals(480, appSettings.getPreviewHeight());
		assertEquals("2", appSettings.getHlsTime());
		assertEquals("", appSettings.getAppName());
		assertEquals(0, appSettings.getGopSize());
		assertEquals("", appSettings.getJwksURL());
		assertEquals(false, appSettings.isWebRTCTcpCandidatesEnabled());
		assertEquals(false, appSettings.isForceAspectRatioInTranscoding());
		assertEquals(2000, appSettings.getEndpointHealthCheckPeriodMs());
		assertEquals(false, appSettings.isAcceptOnlyStreamsInDataStore());
		assertEquals(false, appSettings.isAcceptOnlyRoomsInDataStore());
		assertEquals(0, appSettings.getRestartStreamFetcherPeriod());
		assertEquals(false, appSettings.isPublishTokenControlEnabled());
		assertEquals(false, appSettings.isEnableTimeTokenForPublish());
		assertEquals(false, appSettings.isHashControlPublishEnabled());
		assertEquals(0, appSettings.getAllowedPublisherCIDRList().size());
		assertEquals("gpu_and_cpu", appSettings.getEncoderSelectionPreference());
		assertEquals(3, appSettings.getExcessiveBandwidthCallThreshold());
		assertEquals(false, appSettings.isExcessiveBandwidthAlgorithmEnabled());
		assertEquals(10, appSettings.getPacketLossDiffThresholdForSwitchback());
		assertEquals(false, appSettings.isReplaceCandidateAddrWithServerAddr());
		assertEquals("all", appSettings.getDataChannelPlayerDistribution());
		assertEquals(10000, appSettings.getWebRTCClientStartTimeoutMs());
		assertEquals(false, appSettings.isStartStreamFetcherAutomatically());
		assertEquals("", appSettings.getHlsEncryptionKeyInfoFile());
		assertEquals("", appSettings.getWarFileOriginServerAddress());
		assertEquals("", appSettings.getContentSecurityPolicyHeaderValue());
		assertEquals("", appSettings.getTurnServerCredential());
		assertEquals("", appSettings.getTurnServerUsername());
		assertEquals("", appSettings.getHlsHttpEndpoint());
		assertEquals(false, appSettings.isRtmpPlaybackEnabled());
		assertEquals(-1, appSettings.getMaxAudioTrackCount());
		assertEquals(-1, appSettings.getMaxVideoTrackCount());
		assertEquals(2, appSettings.getOriginEdgeIdleTimeout());
		assertEquals(false, appSettings.isAddDateTimeToHlsFileName());
		assertEquals(true, appSettings.isPlayWebRTCStreamOnceForEachSession());
		assertEquals(true, appSettings.isStatsBasedABREnabled());
		assertEquals(1, appSettings.getAbrDownScalePacketLostRatio(), 0.0001);
		assertEquals(0.1, appSettings.getAbrUpScalePacketLostRatio(), 0.0001);
		assertEquals(30, appSettings.getAbrUpScaleJitterMs(), 0.0001);
		assertEquals(150, appSettings.getAbrUpScaleRTTMs(), 0.0001);
		assertNotNull(appSettings.getClusterCommunicationKey());
		assertEquals(false, appSettings.isId3TagEnabled());
		assertEquals(true, appSettings.isSendAudioLevelToViewers());
		assertNull(appSettings.getTimeTokenSecretForPublish());
		assertNull(appSettings.getTimeTokenSecretForPlay());

		assertEquals(true, appSettings.isHwScalingEnabled());

		assertNotNull(appSettings.getSubscriberAuthenticationKey());
		assertNull(appSettings.getFirebaseAccountKeyJSON());
		assertNull(appSettings.getApnKeyId());
		assertNull(appSettings.getApnTeamId());
		assertNull(appSettings.getApnPrivateKey());
		assertEquals("api.sandbox.push.apple.com", appSettings.getApnsServer());

		assertEquals(0, appSettings.getWebhookRetryCount());
		assertEquals(1000, appSettings.getWebhookRetryDelay());

		//if we add a new field, we just need to check its default value in this test
		//When a new field is added or removed please update the number of fields and make this test pass
		//by also checking its default value. 

		assertEquals("New field is added to settings. PAY ATTENTION: Please CHECK ITS DEFAULT VALUE and fix the number of fields.", 
					177, numberOfFields);

		
	}
	
	

}
