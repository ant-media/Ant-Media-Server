package io.antmedia.cluster;

import java.util.List;

import io.antmedia.AppSettings;
import io.antmedia.statistic.StatsCollector;

public interface IClusterStore 
{
	public List<ClusterNode> getClusterNodes(int offset, int size);

	public ClusterNode getClusterNode(String nodeId);
	
	public ClusterNode getClusterNodeFromIP(String ipAddress);
	
	public long getNodeCount();
		
	public boolean deleteNode(String nodeId);
	
	public boolean addOrUpdate(ClusterNode node);

	/**
	 * Update only the admin-defined note of a cluster node, leaving the heartbeat-reported fields
	 * (memory/cpu/lastUpdateTime) untouched. Does nothing if the node does not exist.
	 * @param nodeId id of the node
	 * @param note note text; an empty string clears the note
	 * @return true if the node exists and the note was persisted, false otherwise
	 */
	public boolean updateClusterNodeNote(String nodeId, String note);
	
	public boolean saveSettings(AppSettings settings);

	public AppSettings getSettings(String appName);
		
	public List<AppSettings> getAllSettings();
	
	/**
	 * Delete the app settings in the database
	 * @param appName: Name of the app to be deleted
	 * @return number of deleted records. It should be 1. 
	 */
	public long deleteAppSettings(String appName);
	
	/**
	 * Set the stats collector to the cluster store
	 * @param statsCollector
	 */
	public void setStatsCollector(StatsCollector statsCollector);
}
