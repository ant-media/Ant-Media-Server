package io.antmedia.console.rest;

import java.util.List;

import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

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

@Component
@Path("/cluster")
public class ClusterRestService extends ClusterRestServiceV2 {
	protected static Logger logger = LoggerFactory.getLogger(ClusterRestService.class);
	
	@Context
	private ServletContext servletContext;
	
	
	@GET
	@Path("/node-count")
	@Override
	public SimpleStat getNodeCount() {
		return super.getNodeCount();
	}
	
	@GET
	@Path("/nodes/{offset}/{size}")
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public List<ClusterNode> getNodeList(@PathParam("offset") int offset, @PathParam("size") int size) {
		return super.getNodeList(offset, size);
	}	
	
	@GET
	@Path("/deleteNode/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public Result deleteNode(@PathParam("id") String nodeId) {
		return super.deleteNode(nodeId);
	}
}
