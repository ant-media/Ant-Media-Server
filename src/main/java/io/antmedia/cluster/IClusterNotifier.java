package io.antmedia.cluster;

import java.util.List;

import io.antmedia.IAppSettingsUpdateListener;
import io.antmedia.ICreateAppListener;
import io.antmedia.IDeleteAppListener;

public interface IClusterNotifier {
	
	String BEAN_NAME = "tomcat.cluster";
	
	IClusterStore getClusterStore();

	/** The nodes whose heartbeat is fresh, see {@link ClusterNode#getStatus()}. */
	List<ClusterNode> getAliveNodes();

	/**
	 * The alive node with the lowest address, string order. Every node works out the same one from the heartbeats,
	 * so nothing is elected or stored. Null when no node is alive.
	 */
	ClusterNode getLeader();

	void registerSettingUpdateListener(String appName, IAppSettingsUpdateListener listener);
	
	void registerCreateAppListener(ICreateAppListener createApplistener);
	
	void registerDeleteAppListener(IDeleteAppListener deleteApplistener);

}
