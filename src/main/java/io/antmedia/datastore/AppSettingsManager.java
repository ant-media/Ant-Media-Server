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

	private static final String SETTINGS_PREVIEW_OVERWRITE = "settings.previewOverwrite";
	private static final String SETTINGS_ENCODER_SETTINGS_STRING = "settings.encoderSettingsString";
	private static final String FACEBOOK_CLIENT_ID = "facebook.clientId";
	private static final String SETTINGS_HLS_PLAY_LIST_TYPE = "settings.hlsPlayListType";
	private static final String SETTINGS_HLS_TIME = "settings.hlsTime";
	private static final String SETTINGS_HLS_LIST_SIZE = "settings.hlsListSize";
	private static final String SETTINGS_VOD_FOLDER = "settings.vodFolder";
	private static final String SETTINGS_OBJECT_DETECTION_ENABLED = "settings.objectDetectionEnabled";
	private static final String SETTINGS_HLS_MUXING_ENABLED = "settings.hlsMuxingEnabled";
	private static final String SETTINGS_ADD_DATE_TIME_TO_MP4_FILE_NAME = "settings.addDateTimeToMp4FileName";
	private static final String SETTINGS_MP4_MUXING_ENABLED = "settings.mp4MuxingEnabled";
	private static final String SETTINGS_ACCEPT_ONLY_STREAMS_IN_DATA_STORE = "settings.acceptOnlyStreamsInDataStore";
	private static final String SETTINGS_WEBRTC_ENABLED = "settings.webRTCEnabled";
	private static final String SETTINGS_WEBRTC_FRAME_RATE = "settings.webRTCFrameRate";

	
	
	private static final Logger log = LoggerFactory.getLogger(AppSettingsManager.class);

	private AppSettingsManager() {

	}


	public static boolean updateAppSettings(ApplicationContext applicationContext, AppSettingsModel settingsModel, boolean sendToCluster) {
		if (!updateAppSettingsFile(applicationContext.getApplicationName(), settingsModel))
		{
			return false;
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

			appSettings.setAdaptiveResolutionList(settingsModel.getEncoderSettings());

			String oldVodFolder = appSettings.getVodFolder();

			appSettings.setVodFolder(settingsModel.getVodFolder());
			appSettings.setPreviewOverwrite(settingsModel.isPreviewOverwrite());

			AntMediaApplicationAdapter bean = (AntMediaApplicationAdapter) applicationContext.getBean("web.handler");

			bean.synchUserVoDFolder(oldVodFolder, settingsModel.getVodFolder());

			log.warn("app settings updated");	
		}
		else {
			log.warn("App has no app.settings bean");
			return false;
		}
		if (applicationContext.containsBean(AcceptOnlyStreamsInDataStore.BEAN_NAME)) {
			AcceptOnlyStreamsInDataStore securityHandler = (AcceptOnlyStreamsInDataStore) applicationContext.getBean(AcceptOnlyStreamsInDataStore.BEAN_NAME);
			securityHandler.setEnabled(settingsModel.isAcceptOnlyStreamsInDataStore());
		}

		if(sendToCluster && applicationContext.getParent().containsBean(IClusterNotifier.BEAN_NAME)) {
			IClusterNotifier cluster = (IClusterNotifier) applicationContext.getParent().getBean(IClusterNotifier.BEAN_NAME);
			cluster.sendAppSettings(applicationContext.getApplicationName(), settingsModel);
		}

		return true;
	}

	private static boolean updateAppSettingsFile(String appName, AppSettingsModel appsettings) {
		PreferenceStore store = new PreferenceStore("red5-web.properties");
		store.setFullPath("webapps/"+appName+"/WEB-INF/red5-web.properties");

		store.put(SETTINGS_MP4_MUXING_ENABLED, String.valueOf(appsettings.isMp4MuxingEnabled()));
		store.put(SETTINGS_ADD_DATE_TIME_TO_MP4_FILE_NAME, String.valueOf(appsettings.isAddDateTimeToMp4FileName()));
		store.put(SETTINGS_HLS_MUXING_ENABLED, String.valueOf(appsettings.isHlsMuxingEnabled()));
		store.put(SETTINGS_ACCEPT_ONLY_STREAMS_IN_DATA_STORE, String.valueOf(appsettings.isAcceptOnlyStreamsInDataStore()));
		store.put(SETTINGS_OBJECT_DETECTION_ENABLED, String.valueOf(appsettings.isObjectDetectionEnabled()));
		store.put("settings.tokenControlEnabled", String.valueOf(appsettings.isTokenControlEnabled()));
		store.put(SETTINGS_WEBRTC_ENABLED, String.valueOf(appsettings.isWebRTCEnabled()));
		store.put(SETTINGS_WEBRTC_FRAME_RATE, String.valueOf(appsettings.getWebRTCFrameRate()));

		
		if (appsettings.getVodFolder() == null) {
			store.put(SETTINGS_VOD_FOLDER, "");
		}else {
			store.put(SETTINGS_VOD_FOLDER, appsettings.getVodFolder());
		}


		if (appsettings.getHlsListSize() < 5) {
			store.put(SETTINGS_HLS_LIST_SIZE, "5");
		}
		else {
			store.put(SETTINGS_HLS_LIST_SIZE, String.valueOf(appsettings.getHlsListSize()));
		}

		if (appsettings.getHlsTime() < 2) {
			store.put(SETTINGS_HLS_TIME, "2");
		}
		else {
			store.put(SETTINGS_HLS_TIME, String.valueOf(appsettings.getHlsTime()));
		}

		if (appsettings.getHlsPlayListType() == null) {
			store.put(SETTINGS_HLS_PLAY_LIST_TYPE, "");
		}
		else {
			store.put(SETTINGS_HLS_PLAY_LIST_TYPE, appsettings.getHlsPlayListType());
		}

		if (appsettings.getFacebookClientId() == null){
			store.put(FACEBOOK_CLIENT_ID, "");
		}
		else {
			store.put(FACEBOOK_CLIENT_ID, appsettings.getFacebookClientId());
		}

		if (appsettings.getEncoderSettings() == null) {
			store.put(SETTINGS_ENCODER_SETTINGS_STRING, "");
		}
		else {
			store.put(SETTINGS_ENCODER_SETTINGS_STRING, io.antmedia.AppSettings.getEncoderSettingsString(appsettings.getEncoderSettings()));
		}

		store.put(SETTINGS_PREVIEW_OVERWRITE, String.valueOf(appsettings.isPreviewOverwrite()));

		return store.save();

	}

	public static AppSettingsModel getAppSettings(String appname) {
		PreferenceStore store = new PreferenceStore("red5-web.properties");
		store.setFullPath("webapps/"+appname+"/WEB-INF/red5-web.properties");
		AppSettingsModel appSettings = new AppSettingsModel();

		if (store.get(SETTINGS_MP4_MUXING_ENABLED) != null) {
			appSettings.setMp4MuxingEnabled(Boolean.parseBoolean(store.get(SETTINGS_MP4_MUXING_ENABLED)));
		}
		
		if (store.get(SETTINGS_WEBRTC_ENABLED) != null) {
			appSettings.setWebRTCEnabled(Boolean.parseBoolean(store.get(SETTINGS_WEBRTC_ENABLED)));
		}
		
		if (store.get(SETTINGS_ADD_DATE_TIME_TO_MP4_FILE_NAME) != null) {
			appSettings.setAddDateTimeToMp4FileName(Boolean.parseBoolean(store.get(SETTINGS_ADD_DATE_TIME_TO_MP4_FILE_NAME)));
		}
		if (store.get(SETTINGS_HLS_MUXING_ENABLED) != null) {
			appSettings.setHlsMuxingEnabled(Boolean.parseBoolean(store.get(SETTINGS_HLS_MUXING_ENABLED)));
		}
		if (store.get(SETTINGS_OBJECT_DETECTION_ENABLED) != null) {
			appSettings.setObjectDetectionEnabled(Boolean.parseBoolean(store.get(SETTINGS_OBJECT_DETECTION_ENABLED)));
		}

		if (store.get(SETTINGS_HLS_LIST_SIZE) != null) {
			appSettings.setHlsListSize(Integer.valueOf(store.get(SETTINGS_HLS_LIST_SIZE)));
		}

		if (store.get(SETTINGS_HLS_TIME) != null) {
			appSettings.setHlsTime(Integer.valueOf(store.get(SETTINGS_HLS_TIME)));
		}
		
		if (store.get(SETTINGS_WEBRTC_FRAME_RATE) != null) {
			appSettings.setWebRTCFrameRate(Integer.valueOf(store.get(SETTINGS_WEBRTC_FRAME_RATE)));
		}
		
		appSettings.setHlsPlayListType(store.get(SETTINGS_HLS_PLAY_LIST_TYPE));
		appSettings.setFacebookClientId(store.get(FACEBOOK_CLIENT_ID));
		appSettings.setFacebookClientSecret(store.get("facebook.clientSecret"));
		appSettings.setYoutubeClientId(store.get("youtube.clientId"));
		appSettings.setYoutubeClientSecret(store.get("youtube.clientSecret"));
		appSettings.setPeriscopeClientId(store.get("periscope.clientId"));
		appSettings.setPeriscopeClientSecret(store.get("periscope.clientSecret"));
		appSettings.setAcceptOnlyStreamsInDataStore(Boolean.valueOf(store.get(SETTINGS_ACCEPT_ONLY_STREAMS_IN_DATA_STORE)));
		appSettings.setVodFolder(store.get(SETTINGS_VOD_FOLDER));
		appSettings.setTokenControlEnabled(Boolean.parseBoolean(store.get("settings.tokenControlEnabled")));

		appSettings.setEncoderSettings(io.antmedia.AppSettings.getEncoderSettingsList(store.get(SETTINGS_ENCODER_SETTINGS_STRING)));

		if (store.get(SETTINGS_PREVIEW_OVERWRITE) != null) {
			appSettings.setPreviewOverwrite(Boolean.parseBoolean(store.get(SETTINGS_PREVIEW_OVERWRITE)));
		}

		return appSettings;
	}

}
