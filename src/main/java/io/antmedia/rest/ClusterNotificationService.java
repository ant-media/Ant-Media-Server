package io.antmedia.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import io.antmedia.rest.model.Result;
import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;


@OpenAPIDefinition(
		info = @Info(
				description = "Ant Media Server Internal Cluster REST API Reference",
				version = "V1.0",
				title = "Ant Media Server Internal Cluster REST API Reference",
				contact = @Contact(name = "Ant Media Info", email = "contact@antmedia.io", url = "https://antmedia.io"),
				license = @License(name = "Apache 2.0", url = "http://www.apache.org")),
		externalDocs = @ExternalDocumentation(url = "https://antmedia.io")
		)

@Component
@Path("/cluster-communication")
public class ClusterNotificationService extends RestServiceBase {
	
	protected static Logger logger = LoggerFactory.getLogger(ClusterNotificationService.class);

	
	@Operation(summary = "Notifies when streaming is started in another node. It is for internal communication between node. This is why it is hidden", description = "Notifies Stream Start/Stop operations.", responses = {
			@ApiResponse(responseCode = "200", description = "Received by the node", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Result.class)))
	})
	@POST
	@Path("/publish-started-notification/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	@Hidden
	public Result publishStarted(@Parameter(description = "id of the broadcast", required = true) @PathParam("id") String id,
			@Parameter(description = "role of the stream", required = false) @QueryParam("role") String role,
			@Parameter(description = "Main track of the stream", required = false) @QueryParam("mainTrackId") String mainTrackId) {
		return new Result(getApplication().publishStarted(id, role, mainTrackId));		
	}
	
	
	@Operation(summary = "Notifies when streaming is stopped in another node. It is for internal communication between nodes.This is why it is hidden", description = "Notifies Stream Start/Stop operations.", responses = {
			@ApiResponse(responseCode = "200", description = "Received by the node", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Result.class)))
	})
	@POST
	@Path("/publish-stopped-notification/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	@Hidden
	public Result publishStopped(@Parameter(description = "id of the broadcast", required = true) @PathParam("id") String id,
			@Parameter(description = "role of the stream", required = false) @QueryParam("role") String role,
			@Parameter(description = "Main track of the stream", required = false) @QueryParam("mainTrackId") String mainTrackId) {
		return new Result(getApplication().publishStopped(id, role, mainTrackId));
	}
	
	
}
