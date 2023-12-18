package io.antmedia.cluster;

import java.util.List;

import io.antmedia.AppSettings;

public interface IClusterStore 
{
	public List<ClusterNode> getClusterNodes(int offset, int size);

	public ClusterNode getClusterNode(String nodeId);
	
	public ClusterNode getClusterNodeFromIP(String ipAddress);
	
	public long getNodeCount();
		
	public boolean deleteNode(String nodeId);
	
	public boolean addOrUpdate(ClusterNode node);
	
	public boolean saveSettings(AppSettings settings);

	public AppSettings getSettings(String appName);
		
	public List<AppSettings> getAllSettings();
	
	/**
	 * Delete the app settings in the database
	 * @param appName: Name of the app to be deleted
	 * @return number of deleted records. It should be 1. 
	 */
	public long deleteAppSettings(String appName);
}
