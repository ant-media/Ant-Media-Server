package io.antmedia.cluster;

import io.antmedia.IAppSettingsUpdateListener;
import io.antmedia.ICreateAppListener;
import io.antmedia.IDeleteAppListener;

public interface IClusterNotifier {
	
	public static final String BEAN_NAME = "tomcat.cluster";
	
	public IClusterStore getClusterStore();
	
	public void registerSettingUpdateListener(String appName, IAppSettingsUpdateListener listener);
	
	public void registerCreateAppListener(ICreateAppListener createApplistener);
	
	public void registerDeleteAppListener(IDeleteAppListener deleteApplistener);

}
