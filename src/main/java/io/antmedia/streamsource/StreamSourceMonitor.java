package io.antmedia.streamsource;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.cluster.ClusterNode;
import io.antmedia.cluster.IClusterNotifier;
import io.antmedia.cluster.IClusterStore;
import io.antmedia.datastore.db.DataStore;
import io.antmedia.datastore.db.types.Broadcast;
import io.vertx.core.Vertx;

public class StreamSourceMonitor {

	public static final int MONITORING_PERIOD = 10000;
	public static final String BEAN_NAME = "web.handler";
	protected static Logger logger = LoggerFactory.getLogger(StreamSourceMonitor.class);
	
	private Vertx vertx;
	AntMediaApplicationAdapter app;
	private DataStore dataStore;
	private long monitorTask = -1;
	private IClusterStore clusterDataStore;
	private int MAX_NODE_COUNT = 1000;
	private int nextLiveNode = 0;
	
	public StreamSourceMonitor(AntMediaApplicationAdapter application, IClusterNotifier clusterNotifier) {
		this.app = application;
		if(clusterNotifier != null) {
			this.clusterDataStore = clusterNotifier.getClusterStore();
		}
		vertx = application.getVertx();
		dataStore = application.getDataStore();
		monitorTask = vertx.setPeriodic(MONITORING_PERIOD, id -> monitorStreamSources());
	}
		
	public void monitorStreamSources() {
		if(clusterDataStore == null || checkIfMaster()) { //standalone mode or master in cluster mode
			handleUnexpectedlyTerminatedStreamSources();
		}
	}

	private boolean checkIfMaster() {
		String thisNodeIp = app.getServerSettings().getHostAddress();
		ClusterNode masterNode = clusterDataStore.getMasterNode();

		
		if(masterNode != null && masterNode.getIp().equals(thisNodeIp)) {
			logger.info("Master node ip:{} this node ip:{}", masterNode.getIp(), thisNodeIp);
			//this node is master
			return true;
		}
		else if (masterNode == null || masterNode.getStatus().equals(ClusterNode.DEAD)){
			logger.info("Becoming Master this node ip:{}", thisNodeIp);

			//there is no master or current master is dead. Then promote yourself to master
			ClusterNode thisNode = clusterDataStore.getClusterNodeFromIP(thisNodeIp);
			clusterDataStore.promoteToMaster(thisNode, masterNode);
			return true;
		}
		
		return false;
	}
	
	private void handleUnexpectedlyTerminatedStreamSources() {
		List<String> liveNodes = getLiveNodes();
		
		List<Broadcast> externalStreams = dataStore.getExternalStreamsList();
		for (Broadcast broadcast : externalStreams) {
			if(!broadcast.isAutoStartStopEnabled()) 
			{
				broadcast.setOriginAdress(liveNodes.get(nextLiveNode++ % liveNodes.size()));
				if(app.startStreaming(broadcast).isSuccess()) 
				{
					logger.info("Unexpectedly Terminated stream {} transferred on to origin {}", broadcast.getStreamId(), broadcast.getOriginAdress());
				}
			}
		}
	}

	private List<String> getLiveNodes() {
		List<String> liveNodes = new ArrayList<String>();

		if(clusterDataStore == null) { //standalone mode
			liveNodes.add(app.getServerSettings().getHostAddress());
		}
		else { //cluster mode
			List<ClusterNode> nodes = clusterDataStore.getClusterNodes(0, MAX_NODE_COUNT);
			
			for (ClusterNode clusterNode : nodes) {
				if(clusterNode.getStatus().equals(ClusterNode.ALIVE)) {
					liveNodes.add(clusterNode.getIp());
				}
			}
		}
		return liveNodes;
	}

	public void stop() {
		if(monitorTask != -1) {
			vertx.cancelTimer(monitorTask);
		}
	}
}
