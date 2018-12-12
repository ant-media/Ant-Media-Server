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
	
	private static final String SETTINGS_ACCEPT_ONLY_STREAMS_IN_DATA_STORE = "settings.acceptOnlyStreamsInDataStore";

	private static final Logger log = LoggerFactory.getLogger(AppSettingsManager.class);

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

		store.put("settings.mp4MuxingEnabled", String.valueOf(appsettings.isMp4MuxingEnabled()));
		store.put("settings.addDateTimeToMp4FileName", String.valueOf(appsettings.isAddDateTimeToMp4FileName()));
		store.put("settings.hlsMuxingEnabled", String.valueOf(appsettings.isHlsMuxingEnabled()));
		store.put(SETTINGS_ACCEPT_ONLY_STREAMS_IN_DATA_STORE, String.valueOf(appsettings.isAcceptOnlyStreamsInDataStore()));
		store.put("settings.objectDetectionEnabled", String.valueOf(appsettings.isObjectDetectionEnabled()));
		store.put("settings.tokenControlEnabled", String.valueOf(appsettings.isTokenControlEnabled()));
		
		if (appsettings.getVodFolder() == null) {
			store.put("settings.vodFolder", "");
		}else {
			store.put("settings.vodFolder", appsettings.getVodFolder());
		}


		if (appsettings.getHlsListSize() < 5) {
			store.put("settings.hlsListSize", "5");
		}
		else {
			store.put("settings.hlsListSize", String.valueOf(appsettings.getHlsListSize()));
		}

		if (appsettings.getHlsTime() < 2) {
			store.put("settings.hlsTime", "2");
		}
		else {
			store.put("settings.hlsTime", String.valueOf(appsettings.getHlsTime()));
		}

		if (appsettings.getHlsPlayListType() == null) {
			store.put("settings.hlsPlayListType", "");
		}
		else {
			store.put("settings.hlsPlayListType", appsettings.getHlsPlayListType());
		}

		if (appsettings.getFacebookClientId() == null){
			store.put("facebook.clientId", "");
		}
		else {
			store.put("facebook.clientId", appsettings.getFacebookClientId());
		}

		if (appsettings.getEncoderSettings() == null) {
			store.put("settings.encoderSettingsString", "");
		}
		else {
			store.put("settings.encoderSettingsString", io.antmedia.AppSettings.getEncoderSettingsString(appsettings.getEncoderSettings()));
		}

		store.put("settings.previewOverwrite", String.valueOf(appsettings.isPreviewOverwrite()));
				
		return store.save();
		
	}

	public static AppSettingsModel getAppSettings(String appname) {
		PreferenceStore store = new PreferenceStore("red5-web.properties");
		store.setFullPath("webapps/"+appname+"/WEB-INF/red5-web.properties");
		AppSettingsModel appSettings = new AppSettingsModel();

		if (store.get("settings.mp4MuxingEnabled") != null) {
			appSettings.setMp4MuxingEnabled(Boolean.parseBoolean(store.get("settings.mp4MuxingEnabled")));
		}
		if (store.get("settings.addDateTimeToMp4FileName") != null) {
			appSettings.setAddDateTimeToMp4FileName(Boolean.parseBoolean(store.get("settings.addDateTimeToMp4FileName")));
		}
		if (store.get("settings.hlsMuxingEnabled") != null) {
			appSettings.setHlsMuxingEnabled(Boolean.parseBoolean(store.get("settings.hlsMuxingEnabled")));
		}
		if (store.get("settings.objectDetectionEnabled") != null) {
			appSettings.setObjectDetectionEnabled(Boolean.parseBoolean(store.get("settings.objectDetectionEnabled")));
		}
		
		if (store.get("settings.hlsListSize") != null) {
			appSettings.setHlsListSize(Integer.valueOf(store.get("settings.hlsListSize")));
		}

		if (store.get("settings.hlsTime") != null) {
			appSettings.setHlsTime(Integer.valueOf(store.get("settings.hlsTime")));
		}
		appSettings.setHlsPlayListType(store.get("settings.hlsPlayListType"));
		appSettings.setFacebookClientId(store.get("facebook.clientId"));
		appSettings.setFacebookClientSecret(store.get("facebook.clientSecret"));
		appSettings.setYoutubeClientId(store.get("youtube.clientId"));
		appSettings.setYoutubeClientSecret(store.get("youtube.clientSecret"));
		appSettings.setPeriscopeClientId(store.get("periscope.clientId"));
		appSettings.setPeriscopeClientSecret(store.get("periscope.clientSecret"));
		appSettings.setAcceptOnlyStreamsInDataStore(Boolean.valueOf(store.get(SETTINGS_ACCEPT_ONLY_STREAMS_IN_DATA_STORE)));
		appSettings.setVodFolder(store.get("settings.vodFolder"));
		appSettings.setTokenControlEnabled(Boolean.parseBoolean(store.get("settings.tokenControlEnabled")));

		appSettings.setEncoderSettings(io.antmedia.AppSettings.getEncoderSettingsList(store.get("settings.encoderSettingsString")));

		if (store.get("settings.previewOverwrite") != null) {
			appSettings.setPreviewOverwrite(Boolean.parseBoolean(store.get("settings.previewOverwrite")));
		}

		return appSettings;
	}

}
