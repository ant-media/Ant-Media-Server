package io.antmedia.console.rest;

import java.util.ArrayList;
import java.util.List;

import jakarta.servlet.ServletContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import io.antmedia.cluster.ClusterNode;
import io.antmedia.cluster.IClusterNotifier;
import io.antmedia.cluster.IClusterStore;
import io.antmedia.rest.BroadcastRestService.SimpleStat;
import io.antmedia.rest.model.Result;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;

@Component
@Path("/v2/cluster")
public class ClusterRestServiceV2 {
	protected static Logger logger = LoggerFactory.getLogger(ClusterRestServiceV2.class);
	
	@Context
	private ServletContext servletContext;
	
	public IClusterStore getClusterStore() 
	{
		IClusterStore clusterStore = null;
		WebApplicationContext ctxt = WebApplicationContextUtils.getWebApplicationContext(servletContext);
		if (ctxt != null) {
			IClusterNotifier clusterNotifier = (IClusterNotifier) ctxt.getBean(IClusterNotifier.BEAN_NAME);
			clusterStore = clusterNotifier.getClusterStore();
		}
		
		return clusterStore;
	}
	
	@GET
	@Path("/node-count")
	public SimpleStat getNodeCount() {
		IClusterStore clusterStore = getClusterStore();
		long nodeCount = -1;
		if (clusterStore != null) {
			nodeCount = clusterStore.getNodeCount();
		}
		return new SimpleStat(nodeCount);
	}
	
	@GET
	@Path("/nodes/{offset}/{size}")
	@Produces(MediaType.APPLICATION_JSON)
	public List<ClusterNode> getNodeList(@PathParam("offset") int offset, @PathParam("size") int size) 
	{
		IClusterStore clusterStore = getClusterStore();
		List<ClusterNode> nodeList = null;
		if (clusterStore != null) 
		{
			nodeList = clusterStore.getClusterNodes(offset, size);
		}
		else {
			nodeList = new ArrayList<>();
		}
		return nodeList;
	}	
	
	@DELETE
	@Path("/node/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Result deleteNode(@PathParam("id") String nodeId) 
	{
		boolean result = false;
		IClusterStore clusterStore = getClusterStore();
		if (clusterStore != null) 
		{
			result = clusterStore.deleteNode(nodeId);
		}
		return new Result(result);
	}
}
