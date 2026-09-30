package io.antmedia.cluster;

import io.antmedia.IAppSettingsUpdateListener;
import io.antmedia.ICreateAppListener;
import io.antmedia.IDeleteAppListener;

public interface IClusterNotifier {
	
	String BEAN_NAME = "tomcat.cluster";
	
	IClusterStore getClusterStore();
	
	void registerSettingUpdateListener(String appName, IAppSettingsUpdateListener listener);
	
	void registerCreateAppListener(ICreateAppListener createApplistener);
	
	void registerDeleteAppListener(IDeleteAppListener deleteApplistener);

}
