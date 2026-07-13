package io.antmedia.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import org.apache.catalina.util.NetMask;
import org.apache.commons.lang3.RandomUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.red5.server.scope.WebScope;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.AppSettings;
import io.antmedia.EncoderSettings;
import io.antmedia.rest.RestServiceBase;

@ContextConfiguration(locations = { "test.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
@ExtendWith(SpringExtension.class)
public class AppSettingsUnitTest {

	@Autowired
	private ApplicationContext applicationContext;

	protected WebScope appScope;
	static {
		System.setProperty("red5.deployment.type", "junit");
		System.setProperty("red5.root", ".");

	}

	@Test
	public void testDefaultSettings() throws ParseException 
	{
		if (appScope == null) 
		{
			appScope = (WebScope) applicationContext.getBean("web.scope");
            assertEquals(1, appScope.getDepth());
		}

		AppSettings appSettings = (AppSettings) applicationContext.getBean("app.settings");

		assertEquals("0.5", appSettings.getDashFragmentDuration());
		assertEquals("6", appSettings.getDashSegDuration());

		assertEquals("stun:stun1.l.google.com:19302", appSettings.getStunServerURI());
        assertFalse(appSettings.isWebRTCTcpCandidatesEnabled());
		assertEquals("", appSettings.getEncoderName());
		assertEquals(480, appSettings.getPreviewHeight());
		assertFalse(appSettings.isUseOriginalWebRTCEnabled());
		assertEquals(5000, appSettings.getCreatePreviewPeriod());

		//check default value
        assertFalse(appSettings.isForceAspectRatioInTranscoding());
		appSettings.setForceAspectRatioInTranscoding(true);
        assertTrue(appSettings.isForceAspectRatioInTranscoding());
		appSettings.setForceAspectRatioInTranscoding(false);
        assertFalse(appSettings.isForceAspectRatioInTranscoding());

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


        assertFalse(appSettings.isRtmpPlaybackEnabled());
		appSettings.setRtmpPlaybackEnabled(true);
        assertTrue(appSettings.isRtmpPlaybackEnabled());
		appSettings.setRtmpPlaybackEnabled(false);


		assertEquals("mpegts", appSettings.getHlsSegmentType());
		appSettings.setHlsSegmentType("fmp4");
		assertEquals("fmp4", appSettings.getHlsSegmentType());

		JSONObject visibilityMatrix = (JSONObject) new JSONParser().parse("{\"default\":[\"default\"]}");

		appSettings.setParticipantVisibilityMatrix(visibilityMatrix);
		Map<String, List<String>> participantVisibilityMatrix = appSettings.getParticipantVisibilityMatrix();
		assertEquals(1, participantVisibilityMatrix.size());
		JSONArray jsonArray = (JSONArray) participantVisibilityMatrix.get("default");
		assertEquals("default", jsonArray.get(0));
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
		String serializedEncoderSettingString ="[{\"videoBitrate\":"+videoBitrate1+",\"forceSameResolutionEncode\":false,\"forceEncode\":"+forceEncode1+",\"audioBitrate\":"+audioBitrate1+",\"height\":"+height1+"},{\"videoBitrate\":"+videoBitrate2+",\"forceSameResolutionEncode\":false,\"forceEncode\":"+forceEncode2+",\"audioBitrate\":"+audioBitrate2+",\"height\":"+height2+"},{\"videoBitrate\":"+videoBitrate3+",\"forceSameResolutionEncode\":false,\"forceEncode\":"+forceEncode3+",\"audioBitrate\":"+audioBitrate3+",\"height\":"+height3+"}]";

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

		assertEquals(serializedEncoderSettingString, appSettings.encodersList2Str(list));

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
		assertEquals(serializedEncoderSettingString, appSettings.encodersList2Str(list));

		String newFormatEncoderSettingStringWithoutOptionalFlags =
				"[{\"videoBitrate\":"+videoBitrate1+",\"audioBitrate\":"+audioBitrate1+",\"height\":"+height1+"}]";
		list = AppSettings.encodersStr2List(newFormatEncoderSettingStringWithoutOptionalFlags);
		assertEquals(1, list.size());
		assertEquals(480, list.get(0).getHeight());
		assertEquals(500000, list.get(0).getVideoBitrate());
		assertEquals(128000, list.get(0).getAudioBitrate());
		assertTrue(list.get(0).isForceEncode());
		assertFalse(list.get(0).isForceSameResolutionEncode());
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
		assertTrue(appSettings.isWebRTCEnabled());
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

		int idleTimeOut = RandomUtils.insecure().randomInt();
		appSettings.setOriginEdgeIdleTimeout(idleTimeOut);
		assertEquals(idleTimeOut, appSettings.getOriginEdgeIdleTimeout());

		appSettings.setAddDateTimeToHlsFileName(true);
        assertTrue(appSettings.isAddDateTimeToHlsFileName());

		appSettings.setPlayWebRTCStreamOnceForEachSession(false);
		assertFalse(appSettings.isPlayWebRTCStreamOnceForEachSession());

		appSettings.setStatsBasedABREnabled(false);
        assertFalse(appSettings.isStatsBasedABREnabled());
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


        assertFalse(appSettings.isHwScalingEnabled());
		appSettings.setHwScalingEnabled(false);
        assertFalse(appSettings.isHwScalingEnabled());

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

		String webhookPlayAuthUrl = "playAuthUrl";
		appSettings.setWebhookPlayAuthUrl(webhookPlayAuthUrl);
		assertEquals(webhookPlayAuthUrl, appSettings.getWebhookPlayAuthUrl());

		String recordinfSubFolder = "subfolder";
		appSettings.setRecordingSubfolder(recordinfSubFolder);
		assertEquals(recordinfSubFolder, appSettings.getRecordingSubfolder());


		assertNull(appSettings.getCustomSetting("test"));
		appSettings.setCustomSetting("test", "hello");
		assertEquals("hello", appSettings.getCustomSetting("test"));


		JSONObject customFields = new JSONObject();
		customFields.put("test2", "hello2");
		appSettings.setCustomSettings(customFields);
		assertEquals("hello2", appSettings.getCustomSetting("test2"));
		assertNull(appSettings.getCustomSetting("test"));



		appSettings.setRelayRTMPMetaDataToMuxers(true);
		assertTrue(appSettings.isRelayRTMPMetaDataToMuxers());

		appSettings.setRelayRTMPMetaDataToMuxers(false);
		assertFalse(appSettings.isRelayRTMPMetaDataToMuxers());

		appSettings.setDropWebRTCIngestIfNoPacketReceived(true);
		assertTrue(appSettings.isDropWebRTCIngestIfNoPacketReceived());

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
        assertTrue(appSettings.isWriteStatsToDatastore());
        assertFalse(appSettings.isDashMuxingEnabled());
		assertEquals("", appSettings.getListenerHookURL());
        assertFalse(appSettings.isPreviewOverwrite());
		assertEquals(0, appSettings.getStreamFetcherBufferTime());
		assertEquals("delete_segments+program_date_time", appSettings.getHlsflags());
		assertEquals("/usr/local/antmedia/mysql", appSettings.getMySqlClientPath());
        assertFalse(appSettings.isPlayTokenControlEnabled());
        assertFalse(appSettings.isEnableTimeTokenForPlay());
		assertEquals("", appSettings.getMuxerFinishScript());
		assertEquals(30, appSettings.getWebRTCFrameRate());
		assertEquals("", appSettings.getTokenHashSecret());
        assertFalse(appSettings.isHashControlPlayEnabled());
		assertEquals(60000, appSettings.getWebRTCPortRangeMax());
		assertEquals(50000, appSettings.getWebRTCPortRangeMin());
		assertEquals("", appSettings.getAllowedPublisherCIDR());
		assertEquals(300000, appSettings.getExcessiveBandwidthValue());
		assertEquals(0, appSettings.getPortAllocatorFlags());
		assertEquals(0, appSettings.getUpdateTime());
		assertEquals("", appSettings.getHttpForwardingExtension());
		assertEquals("", appSettings.getHttpForwardingBaseURL());
		assertEquals(1500, appSettings.getMaxAnalyzeDurationMS());
        assertFalse(appSettings.isGeneratePreview());
        assertTrue(appSettings.isDisableIPv6Candidates());
		assertEquals("3", appSettings.getRtspPullTransportType());
		assertEquals(5000, appSettings.getRtspTimeoutDurationMs());
		assertEquals(0, appSettings.getMaxResolutionAccept());
        assertTrue(appSettings.isH264Enabled());
        assertFalse(appSettings.isVp8Enabled());
        assertFalse(appSettings.isH265Enabled());
        assertTrue(appSettings.isDataChannelEnabled());
		assertEquals(0, appSettings.getRtmpIngestBufferTimeMs());
		assertEquals("", appSettings.getDataChannelWebHookURL());
		assertEquals(0, appSettings.getEncoderThreadCount());
		assertEquals(0, appSettings.getEncoderThreadType());
		assertEquals(1, appSettings.getVp8EncoderThreadCount());
		assertEquals("unifiedPlan", appSettings.getWebRTCSdpSemantics());
        assertTrue(appSettings.isDeleteDASHFilesOnEnded());
		assertEquals(360, appSettings.getHeightRtmpForwarding());
		assertEquals(96000, appSettings.getAudioBitrateSFU());
        assertTrue(appSettings.isAacEncodingEnabled());
		assertEquals(-1, appSettings.getWebRTCViewerLimit());
		assertEquals("", appSettings.getJwtSecretKey());
        assertFalse(appSettings.isJwtControlEnabled());
        assertTrue(appSettings.isIpFilterEnabled());
		assertEquals(-1, appSettings.getIngestingStreamLimit());
		assertEquals(60, appSettings.getTimeTokenPeriod());
        assertFalse(appSettings.isPullWarFile());
		assertEquals("", appSettings.getJwtStreamSecretKey());
        assertFalse(appSettings.isPublishJwtControlEnabled());
        assertFalse(appSettings.isPlayJwtControlEnabled());
		assertEquals("", appSettings.getDashHttpEndpoint());
        assertFalse(appSettings.isS3RecordingEnabled());
		assertEquals("", appSettings.getS3SecretKey());
		assertEquals("", appSettings.getS3AccessKey());
		assertEquals("", appSettings.getS3RegionName());
		assertEquals("", appSettings.getS3BucketName());
		assertEquals("no-store, no-cache, must-revalidate, max-age=0", appSettings.getS3CacheControl());
		assertEquals("", appSettings.getS3Endpoint());
        assertFalse(appSettings.isForceDecoding());
        assertTrue(appSettings.isAddOriginalMuxerIntoHLSPlaylist());
		assertEquals("", appSettings.getWebhookAuthenticateURL());
		assertEquals("", appSettings.getVodUploadFinishScript());
		assertEquals("%r%b", appSettings.getFileNameFormat());
        assertFalse(appSettings.isSignalingEnabled());
		assertEquals("", appSettings.getSignalingAddress());
        assertFalse(appSettings.isMp4MuxingEnabled());
        assertFalse(appSettings.isAddDateTimeToMp4FileName());
        assertTrue(appSettings.isHlsMuxingEnabled());
        assertTrue(appSettings.isWebRTCEnabled());
        assertTrue(appSettings.isDeleteHLSFilesOnEnded());
		assertEquals("15", appSettings.getHlsListSize());
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
        assertTrue(appSettings.isLLDashEnabled());
        assertFalse(appSettings.isLLHLSEnabled());
        assertFalse(appSettings.isHlsEnabledViaDash());
        assertFalse(appSettings.isUseTimelineDashMuxing());
		assertEquals(2000, appSettings.getWebRTCKeyframeTime());
        assertTrue(appSettings.isDashHttpStreaming());
		assertEquals("streams", appSettings.getS3StreamsFolderPath());
		assertEquals("previews", appSettings.getS3PreviewsFolderPath());
		assertEquals("public-read", appSettings.getS3Permission());
		assertEquals("127.0.0.1", appSettings.getRemoteAllowedCIDR());
        assertFalse(appSettings.isWebMMuxingEnabled());
		assertEquals("", appSettings.getEncoderSettingsString());
		assertEquals("127.0.0.1", appSettings.getAllowedCIDRList().poll().toString());
        assertFalse(appSettings.isUseOriginalWebRTCEnabled());
		assertEquals(5000, appSettings.getCreatePreviewPeriod());
		assertEquals("stun:stun1.l.google.com:19302", appSettings.getStunServerURI());
		assertEquals("", appSettings.getEncoderName());
		assertEquals(480, appSettings.getPreviewHeight());
		assertEquals("2", appSettings.getHlsTime());
		assertEquals("", appSettings.getAppName());
		assertEquals(0, appSettings.getGopSize());
		assertEquals("", appSettings.getJwksURL());
        assertFalse(appSettings.isWebRTCTcpCandidatesEnabled());
        assertFalse(appSettings.isForceAspectRatioInTranscoding());
		assertEquals(2000, appSettings.getEndpointHealthCheckPeriodMs());
        assertFalse(appSettings.isAcceptOnlyStreamsInDataStore());
        assertFalse(appSettings.isAcceptOnlyRoomsInDataStore());
		assertEquals(0, appSettings.getRestartStreamFetcherPeriod());
        assertFalse(appSettings.isPublishTokenControlEnabled());
        assertFalse(appSettings.isEnableTimeTokenForPublish());
        assertFalse(appSettings.isHashControlPublishEnabled());
		assertEquals(0, appSettings.getAllowedPublisherCIDRList().size());
		assertEquals("gpu_and_cpu", appSettings.getEncoderSelectionPreference());
		assertEquals(3, appSettings.getExcessiveBandwidthCallThreshold());
        assertFalse(appSettings.isExcessiveBandwidthAlgorithmEnabled());
		assertEquals(10, appSettings.getPacketLossDiffThresholdForSwitchback());
        assertFalse(appSettings.isReplaceCandidateAddrWithServerAddr());
		assertEquals("all", appSettings.getDataChannelPlayerDistribution());
		assertEquals(10000, appSettings.getWebRTCClientStartTimeoutMs());
        assertFalse(appSettings.isStartStreamFetcherAutomatically());
		assertEquals("", appSettings.getHlsEncryptionKeyInfoFile());
		assertEquals("", appSettings.getWarFileOriginServerAddress());
		assertEquals("", appSettings.getContentSecurityPolicyHeaderValue());
		assertEquals("", appSettings.getTurnServerCredential());
		assertEquals("", appSettings.getTurnServerUsername());
		assertEquals("", appSettings.getHlsHttpEndpoint());
        assertFalse(appSettings.isRtmpPlaybackEnabled());
		assertEquals(-1, appSettings.getMaxAudioTrackCount());
		assertEquals(-1, appSettings.getMaxVideoTrackCount());
		assertEquals(2, appSettings.getOriginEdgeIdleTimeout());
        assertFalse(appSettings.isAddDateTimeToHlsFileName());
        assertTrue(appSettings.isPlayWebRTCStreamOnceForEachSession());
        assertTrue(appSettings.isStatsBasedABREnabled());
		assertEquals(1, appSettings.getAbrDownScalePacketLostRatio(), 0.0001);
		assertEquals(0.1, appSettings.getAbrUpScalePacketLostRatio(), 0.0001);
		assertEquals(30, appSettings.getAbrUpScaleJitterMs(), 0.0001);
		assertEquals(150, appSettings.getAbrUpScaleRTTMs(), 0.0001);
		assertNotNull(appSettings.getClusterCommunicationKey());
        assertFalse(appSettings.isId3TagEnabled());
        assertFalse(appSettings.isSendAudioLevelToViewers());
		assertNull(appSettings.getTimeTokenSecretForPublish());
		assertNull(appSettings.getTimeTokenSecretForPlay());

		assertFalse(appSettings.isHwScalingEnabled());

		assertNotNull(appSettings.getSubscriberAuthenticationKey());
		assertNull(appSettings.getFirebaseAccountKeyJSON());
		assertNull(appSettings.getApnKeyId());
		assertNull(appSettings.getApnTeamId());
		assertNull(appSettings.getApnPrivateKey());
		assertEquals("api.sandbox.push.apple.com", appSettings.getApnsServer());

		assertEquals(0, appSettings.getWebhookRetryCount());
		assertEquals(1000, appSettings.getWebhookRetryDelay());


		assertFalse(appSettings.isSecureAnalyticEndpoint());
		assertEquals("mpegts", appSettings.getHlsSegmentType());

		assertFalse(appSettings.isWebhookPlayAuthEnabled());
		assertEquals("", appSettings.getWebhookPlayAuthUrl());

		assertNull(appSettings.getRecordingSubfolder());
		assertEquals("application/json", appSettings.getWebhookContentType());
                        
		assertFalse(appSettings.isS3PathStyleAccessEnabled());

		appSettings.setS3PathStyleAccessEnabled(true);

		assertTrue(appSettings.isS3PathStyleAccessEnabled());

		assertEquals(2000, appSettings.getIceGatheringTimeoutMs());

		assertNotNull(appSettings.getParticipantVisibilityMatrix());

		Map<String, List<String>> trackSelectionMode = appSettings.getParticipantVisibilityMatrix();
		
		assertEquals(4, trackSelectionMode.size());
		assertEquals(1, trackSelectionMode.get("default").size());
		assertEquals(List.of("default"), trackSelectionMode.get("default"));
		
		assertEquals(2, trackSelectionMode.get("speaker").size());
		assertEquals(Arrays.asList("speaker", "active_attendee"), trackSelectionMode.get("speaker"));

		assertEquals(2, trackSelectionMode.get("attendee").size());
		assertEquals(Arrays.asList("speaker","active_attendee"), trackSelectionMode.get("attendee"));

		assertEquals(2, trackSelectionMode.get("speaker").size());
		assertEquals(Arrays.asList("active_attendee", "speaker"), trackSelectionMode.get("active_attendee"));


		Map<String, Object> map = appSettings.getCustomSettings();
		assertNotNull(map);
		assertEquals(0, map.size());

		assertTrue(appSettings.isRelayRTMPMetaDataToMuxers());

		assertFalse(appSettings.isDropWebRTCIngestIfNoPacketReceived());

		assertEquals(150, appSettings.getSrtReceiveLatencyInMs());
		appSettings.setSrtReceiveLatencyInMs(200);
		assertEquals(200, appSettings.getSrtReceiveLatencyInMs());

		assertEquals(-1, appSettings.getWebhookStreamStatusUpdatePeriodMs());

		assertEquals(150, appSettings.getEncodingQueueSize());
		appSettings.setEncodingQueueSize(200);
		assertEquals(200, appSettings.getEncodingQueueSize());
		assertEquals("png", appSettings.getPreviewFormat());
		assertEquals(75, appSettings.getPreviewQuality());

		assertEquals("", appSettings.getSubFolder());
		appSettings.setSubFolder("test/folder");
		assertEquals("test/folder", appSettings.getSubFolder());
		
		assertFalse(appSettings.isWriteSubscriberEventsToDatastore());
		
		assertEquals("%09d", appSettings.getHlsSegmentFileSuffixFormat());
		appSettings.setHlsSegmentFileSuffixFormat("%s");
		assertEquals("%s", appSettings.getHlsSegmentFileSuffixFormat());

		
		appSettings.setAppStatus(AppSettings.APPLICATION_STATUS_INSTALLED);
		assertEquals(AppSettings.APPLICATION_STATUS_INSTALLED, appSettings.getAppStatus());
		
		appSettings.setAppInstallationTime(100);
		assertEquals(100, appSettings.getAppInstallationTime());
		
		
		assertEquals(10000000, appSettings.getS3TransferBufferSizeInBytes());
		appSettings.setS3TransferBufferSizeInBytes(50000);
		assertEquals(50000, appSettings.getS3TransferBufferSizeInBytes());
		
		Map<String, Map<String, String>> mapEncoderParameters = appSettings.getEncoderParameters();
		assertNotNull(mapEncoderParameters);
		assertEquals(0, mapEncoderParameters.size());
		
		Map<String, String> libopenH264 = new HashMap<>();
		libopenH264.put("rc_mode", "quality");
		mapEncoderParameters.put("libopenh264", libopenH264);
		
		
		appSettings.setEncoderParameters(mapEncoderParameters);
		
		mapEncoderParameters = appSettings.getEncoderParameters();
		assertNotNull(mapEncoderParameters);
		assertEquals(1, mapEncoderParameters.size());
		assertEquals("quality", mapEncoderParameters.get("libopenh264").get("rc_mode"));
		
		assertEquals(120, appSettings.getAudioLevelThreshold());
		appSettings.setAudioLevelThreshold(100);
		assertEquals(100, appSettings.getAudioLevelThreshold());
		
		assertEquals("", appSettings.getStreamStartedScript());
		assertEquals("", appSettings.getStreamEndedScript());
		assertEquals("", appSettings.getStreamIdleTimeoutScript());
		assertTrue(appSettings.isHwDecoderEnabled());
		appSettings.setHwDecoderEnabled(false);
		assertFalse(appSettings.isHwDecoderEnabled());

		
		assertFalse(appSettings.isAv1Enabled());
		appSettings.setAv1Enabled(true);
		assertTrue(appSettings.isAv1Enabled());

		assertFalse(appSettings.isDisableAudio());
		appSettings.setDisableAudio(true);
		assertTrue(appSettings.isDisableAudio());

		//if we add a new field, we just need to check its default value in this test
		//When a new field is added or removed please update the number of fields and make this test pass
		//by also checking its default value. 

		assertEquals(186,
				numberOfFields, "New field is added to settings. PAY ATTENTION: Please CHECK ITS DEFAULT VALUE and fix the number of fields.");
	}



}
