package io.antmedia.console.rest;

import java.util.List;

import jakarta.servlet.ServletContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import io.antmedia.cluster.ClusterNode;
import io.antmedia.rest.BroadcastRestService.SimpleStat;
import io.antmedia.rest.model.Result;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;

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
