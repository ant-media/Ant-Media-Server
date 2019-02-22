package io.antmedia.datastore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.AppSettings;
import io.antmedia.AppSettingsModel;
import io.antmedia.cluster.IClusterNotifier;
import io.antmedia.datastore.preference.PreferenceStore;
import io.antmedia.security.AcceptOnlyStreamsInDataStore;

public class AppSettingsManager {
	private static final String DEFAULT_LOCALHOST = "127.0.0.1";
	private static final Logger log = LoggerFactory.getLogger(AppSettingsManager.class);

	private AppSettingsManager() {

	}

	public static boolean updateAppSettings(ApplicationContext applicationContext, AppSettingsModel settingsModel, boolean sendToCluster) {
		boolean result = false;
		if (updateAppSettingsFile(applicationContext.getApplicationName(), settingsModel))
		{
			if (applicationContext.containsBean(AcceptOnlyStreamsInDataStore.BEAN_NAME)) {
				AcceptOnlyStreamsInDataStore securityHandler = (AcceptOnlyStreamsInDataStore) applicationContext.getBean(AcceptOnlyStreamsInDataStore.BEAN_NAME);
				securityHandler.setEnabled(settingsModel.isAcceptOnlyStreamsInDataStore());
			}
			
			if(sendToCluster && applicationContext.getParent().containsBean(IClusterNotifier.BEAN_NAME)) {
				IClusterNotifier cluster = (IClusterNotifier) applicationContext.getParent().getBean(IClusterNotifier.BEAN_NAME);
				cluster.sendAppSettings(applicationContext.getApplicationName(), settingsModel);
			}
			
			if (applicationContext.containsBean(AppSettings.BEAN_NAME)) {
				AppSettings appSettings = (AppSettings) applicationContext.getBean(AppSettings.BEAN_NAME);

				appSettings.setMp4MuxingEnabled(settingsModel.isMp4MuxingEnabled());
				appSettings.setAddDateTimeToMp4FileName(settingsModel.isAddDateTimeToMp4FileName());
				appSettings.setHlsMuxingEnabled(settingsModel.isHlsMuxingEnabled());
				appSettings.setObjectDetectionEnabled(settingsModel.isObjectDetectionEnabled());
				appSettings.setHlsListSize(String.valueOf(settingsModel.getHlsListSize()));
				appSettings.setHlsTime(String.valueOf(settingsModel.getHlsTime()));
				appSettings.setHlsPlayListType(settingsModel.getHlsPlayListType());
				appSettings.setAcceptOnlyStreamsInDataStore(settingsModel.isAcceptOnlyStreamsInDataStore());
				appSettings.setTokenControlEnabled(settingsModel.isTokenControlEnabled());
				appSettings.setWebRTCEnabled(settingsModel.isWebRTCEnabled());
				appSettings.setWebRTCFrameRate(settingsModel.getWebRTCFrameRate());
				appSettings.setHashControlPublishEnabled(settingsModel.isHashControlPublishEnabled());
				appSettings.setHashControlPlayEnabled(settingsModel.isHashControlPlayEnabled());
				appSettings.setTokenHashSecret(settingsModel.getTokenHashSecret());

				appSettings.setRemoteAllowedCIDR(settingsModel.getRemoteAllowedCIDR());
				
				appSettings.setAdaptiveResolutionList(settingsModel.getEncoderSettings());

				String oldVodFolder = appSettings.getVodFolder();

				appSettings.setVodFolder(settingsModel.getVodFolder());
				appSettings.setPreviewOverwrite(settingsModel.isPreviewOverwrite());

				AntMediaApplicationAdapter bean = (AntMediaApplicationAdapter) applicationContext.getBean("web.handler");

				bean.synchUserVoDFolder(oldVodFolder, settingsModel.getVodFolder());

				log.warn("app settings updated for {}", applicationContext.getApplicationName());	
				result = true;
			}
			else {
				log.warn("App{} has no app.settings bean to update ", applicationContext.getApplicationName());
				result = false;
			}
		}

		return result;
	}

	private static boolean updateAppSettingsFile(String appName, AppSettingsModel appsettings) {
		PreferenceStore store = new PreferenceStore("red5-web.properties");
		store.setFullPath("webapps/"+appName+"/WEB-INF/red5-web.properties");

		store.put(AppSettings.SETTINGS_MP4_MUXING_ENABLED, String.valueOf(appsettings.isMp4MuxingEnabled()));
		store.put(AppSettings.SETTINGS_ADD_DATE_TIME_TO_MP4_FILE_NAME, String.valueOf(appsettings.isAddDateTimeToMp4FileName()));
		store.put(AppSettings.SETTINGS_HLS_MUXING_ENABLED, String.valueOf(appsettings.isHlsMuxingEnabled()));
		store.put(AppSettings.SETTINGS_ACCEPT_ONLY_STREAMS_IN_DATA_STORE, String.valueOf(appsettings.isAcceptOnlyStreamsInDataStore()));
		store.put(AppSettings.SETTINGS_OBJECT_DETECTION_ENABLED, String.valueOf(appsettings.isObjectDetectionEnabled()));
		store.put(AppSettings.SETTINGS_TOKEN_CONTROL_ENABLED, String.valueOf(appsettings.isTokenControlEnabled()));
		store.put(AppSettings.SETTINGS_WEBRTC_ENABLED, String.valueOf(appsettings.isWebRTCEnabled()));
		store.put(AppSettings.SETTINGS_WEBRTC_FRAME_RATE, String.valueOf(appsettings.getWebRTCFrameRate()));
		store.put(AppSettings.SETTINGS_HASH_CONTROL_PUBLISH_ENABLED, String.valueOf(appsettings.isHashControlPublishEnabled()));
		store.put(AppSettings.SETTINGS_HASH_CONTROL_PLAY_ENABLED, String.valueOf(appsettings.isHashControlPlayEnabled()));
		
		store.put(AppSettings.SETTINGS_REMOTE_ALLOWED_CIDR, appsettings.getRemoteAllowedCIDR() != null 
																? appsettings.getRemoteAllowedCIDR() 
																: DEFAULT_LOCALHOST);
		
		if (appsettings.getVodFolder() == null) {
			store.put(AppSettings.SETTINGS_VOD_FOLDER, "");
		}else {
			store.put(AppSettings.SETTINGS_VOD_FOLDER, appsettings.getVodFolder());
		}

		if (appsettings.getHlsListSize() < 5) {
			store.put(AppSettings.SETTINGS_HLS_LIST_SIZE, "5");
		}
		else {
			store.put(AppSettings.SETTINGS_HLS_LIST_SIZE, String.valueOf(appsettings.getHlsListSize()));
		}

		if (appsettings.getHlsTime() < 2) {
			store.put(AppSettings.SETTINGS_HLS_TIME, "2");
		}
		else {
			store.put(AppSettings.SETTINGS_HLS_TIME, String.valueOf(appsettings.getHlsTime()));
		}

		if (appsettings.getHlsPlayListType() == null) {
			store.put(AppSettings.SETTINGS_HLS_PLAY_LIST_TYPE, "");
		}
		else {
			store.put(AppSettings.SETTINGS_HLS_PLAY_LIST_TYPE, appsettings.getHlsPlayListType());
		}

		if (appsettings.getFacebookClientId() == null){
			store.put(AppSettings.FACEBOOK_CLIENT_ID, "");
		}
		else {
			store.put(AppSettings.FACEBOOK_CLIENT_ID, appsettings.getFacebookClientId());
		}

		if (appsettings.getEncoderSettings() == null) {
			store.put(AppSettings.SETTINGS_ENCODER_SETTINGS_STRING, "");
		}
		else {
			store.put(AppSettings.SETTINGS_ENCODER_SETTINGS_STRING, AppSettings.encodersList2Str(appsettings.getEncoderSettings()));
		}
		
		if (appsettings.getTokenHashSecret() == null) {
			store.put(AppSettings.TOKEN_HASH_SECRET, "");
		}
		else {
			store.put(AppSettings.TOKEN_HASH_SECRET, appsettings.getTokenHashSecret());
		}

		store.put(AppSettings.SETTINGS_PREVIEW_OVERWRITE, String.valueOf(appsettings.isPreviewOverwrite()));

		return store.save();

	}

	public static AppSettingsModel getAppSettings(String appname) {
		PreferenceStore store = new PreferenceStore("red5-web.properties");
		store.setFullPath("webapps/"+appname+"/WEB-INF/red5-web.properties");
		AppSettingsModel appSettings = new AppSettingsModel();
		
		
		
		if (store.get(AppSettings.SETTINGS_HASH_CONTROL_PLAY_ENABLED) != null) {
			appSettings.setHashControlPlayEnabled(Boolean.parseBoolean(store.get(AppSettings.SETTINGS_HASH_CONTROL_PLAY_ENABLED)));
		}
		
		if (store.get(AppSettings.SETTINGS_HASH_CONTROL_PUBLISH_ENABLED) != null) {
			appSettings.setHashControlPublishEnabled(Boolean.parseBoolean(store.get(AppSettings.SETTINGS_HASH_CONTROL_PUBLISH_ENABLED)));
		}
		
		if (store.get(AppSettings.TOKEN_HASH_SECRET) != null) {
			appSettings.setTokenHashSecret(store.get(AppSettings.TOKEN_HASH_SECRET));
		}
		

		if (store.get(AppSettings.SETTINGS_MP4_MUXING_ENABLED) != null) {
			appSettings.setMp4MuxingEnabled(Boolean.parseBoolean(store.get(AppSettings.SETTINGS_MP4_MUXING_ENABLED)));
		}

		if (store.get(AppSettings.SETTINGS_WEBRTC_ENABLED) != null) {
			appSettings.setWebRTCEnabled(Boolean.parseBoolean(store.get(AppSettings.SETTINGS_WEBRTC_ENABLED)));
		}

		if (store.get(AppSettings.SETTINGS_ADD_DATE_TIME_TO_MP4_FILE_NAME) != null) {
			appSettings.setAddDateTimeToMp4FileName(Boolean.parseBoolean(store.get(AppSettings.SETTINGS_ADD_DATE_TIME_TO_MP4_FILE_NAME)));
		}
		if (store.get(AppSettings.SETTINGS_HLS_MUXING_ENABLED) != null) {
			appSettings.setHlsMuxingEnabled(Boolean.parseBoolean(store.get(AppSettings.SETTINGS_HLS_MUXING_ENABLED)));
		}
		if (store.get(AppSettings.SETTINGS_OBJECT_DETECTION_ENABLED) != null) {
			appSettings.setObjectDetectionEnabled(Boolean.parseBoolean(store.get(AppSettings.SETTINGS_OBJECT_DETECTION_ENABLED)));
		}

		if (store.get(AppSettings.SETTINGS_HLS_LIST_SIZE) != null) {
			appSettings.setHlsListSize(Integer.valueOf(store.get(AppSettings.SETTINGS_HLS_LIST_SIZE)));
		}

		if (store.get(AppSettings.SETTINGS_HLS_TIME) != null) {
			appSettings.setHlsTime(Integer.valueOf(store.get(AppSettings.SETTINGS_HLS_TIME)));
		}

		if (store.get(AppSettings.SETTINGS_WEBRTC_FRAME_RATE) != null) {
			appSettings.setWebRTCFrameRate(Integer.valueOf(store.get(AppSettings.SETTINGS_WEBRTC_FRAME_RATE)));
		}

		appSettings.setHlsPlayListType(store.get(AppSettings.SETTINGS_HLS_PLAY_LIST_TYPE));
		appSettings.setFacebookClientId(store.get(AppSettings.FACEBOOK_CLIENT_ID));
		appSettings.setFacebookClientSecret(store.get(AppSettings.FACEBOOK_CLIENT_SECRET));
		appSettings.setYoutubeClientId(store.get(AppSettings.YOUTUBE_CLIENT_ID));
		appSettings.setYoutubeClientSecret(store.get(AppSettings.YOUTUBE_CLIENT_SECRET));
		appSettings.setPeriscopeClientId(store.get(AppSettings.PERISCOPE_CLIENT_ID));
		appSettings.setPeriscopeClientSecret(store.get(AppSettings.PERISCOPE_CLIENT_SECRET));
		appSettings.setAcceptOnlyStreamsInDataStore(Boolean.valueOf(store.get(AppSettings.SETTINGS_ACCEPT_ONLY_STREAMS_IN_DATA_STORE)));
		appSettings.setVodFolder(store.get(AppSettings.SETTINGS_VOD_FOLDER));
		appSettings.setTokenControlEnabled(Boolean.parseBoolean(store.get(AppSettings.SETTINGS_TOKEN_CONTROL_ENABLED)));

		appSettings.setEncoderSettings(AppSettings.encodersStr2List(store.get(AppSettings.SETTINGS_ENCODER_SETTINGS_STRING)));

		if (store.get(AppSettings.SETTINGS_PREVIEW_OVERWRITE) != null) {
			appSettings.setPreviewOverwrite(Boolean.parseBoolean(store.get(AppSettings.SETTINGS_PREVIEW_OVERWRITE)));
		}
		
		String remoteAllowedCIDR = store.get(AppSettings.SETTINGS_REMOTE_ALLOWED_CIDR);
		if (remoteAllowedCIDR != null && !remoteAllowedCIDR.isEmpty())
		{
			appSettings.setRemoteAllowedCIDR(store.get(AppSettings.SETTINGS_REMOTE_ALLOWED_CIDR));
		}
		else {
			//default value
			appSettings.setRemoteAllowedCIDR(DEFAULT_LOCALHOST);
		}
		
		return appSettings;
	}

}
