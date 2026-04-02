package io.antmedia.cluster;

import io.antmedia.IAppSettingsUpdateListener;
import io.antmedia.ICreateAppListener;
import io.antmedia.IDeleteAppListener;
import io.antmedia.IDeployPluginListener;
import io.antmedia.IUndeployPluginListener;

public interface IClusterNotifier {

	public static final String BEAN_NAME = "tomcat.cluster";

	public IClusterStore getClusterStore();

	public void registerSettingUpdateListener(String appName, IAppSettingsUpdateListener listener);

	public void registerCreateAppListener(ICreateAppListener createApplistener);

	public void registerDeleteAppListener(IDeleteAppListener deleteApplistener);

	public void registerDeployPluginListener(IDeployPluginListener listener);

	public void registerUndeployPluginListener(IUndeployPluginListener listener);

	public void notifyDeployPlugin(String pluginName, String jarFileURI, String secretKey);

	public void notifyUndeployPlugin(String pluginName);

}
