package io.antmedia.cluster;

import io.antmedia.IAppSettingsUpdateListener;
import io.antmedia.ICreateAppListener;
import io.antmedia.IDeleteAppListener;
import io.antmedia.IDeployPluginListener;
import io.antmedia.IUndeployPluginListener;

public interface IClusterNotifier {

	String BEAN_NAME = "tomcat.cluster";

	IClusterStore getClusterStore();

	void registerSettingUpdateListener(String appName, IAppSettingsUpdateListener listener);

	void registerCreateAppListener(ICreateAppListener createApplistener);

	void registerDeleteAppListener(IDeleteAppListener deleteApplistener);

	void registerDeployPluginListener(IDeployPluginListener listener);

	void registerUndeployPluginListener(IUndeployPluginListener listener);

}
