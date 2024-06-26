package io.antmedia.whip;

import java.net.URI;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.kafka.common.Uuid;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.rest.RestServiceBase;
import io.antmedia.rest.model.Result;
import io.antmedia.webrtc.PublishParameters;
import io.antmedia.websocket.WebSocketConstants;
import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.servers.Server;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.core.Response.Status;


@OpenAPIDefinition(
		info = @Info(
				description = "Ant Media Server WHIP endpoint",
				version = "v2.0",
				title = "Ant Media Server WHIP Endpoint",
				contact = @Contact(name = "Ant Media Info", email = "contact@antmedia.io", url = "https://antmedia.io"),
				license = @License(name = "Apache 2.0", url = "https://www.apache.org/licenses/LICENSE-2.0")),
		externalDocs = @ExternalDocumentation(description = "Rest Guide", url="https://antmedia.io/docs")

		)
@Component
//we bind it to /whip path in web.xml
@Path("/")
public class WhipEndpoint extends RestServiceBase {

	private static final Logger logger = LoggerFactory.getLogger(WhipEndpoint.class);



	@Operation(summary = "Publish a webrtc stream through WebRTC-HTTP ingestion protocol(WHIP). HTTP for signaling.")
	@POST
	@Consumes({ "application/sdp" })
	@Path("/{streamId}")
	@Produces("application/sdp")
	public CompletableFuture<Response> startWhipPublish(@Context UriInfo uriInfo, @PathParam(WebSocketConstants.STREAM_ID) String streamId,
			@QueryParam(WebSocketConstants.VIDEO) Boolean enableVideo,
			@QueryParam(WebSocketConstants.AUDIO) Boolean enableAudio,
			@QueryParam(WebSocketConstants.SUBSCRIBER_ID) String subscriberId,
			@QueryParam(WebSocketConstants.SUBSCRIBER_CODE) String subscriberCode,
			@QueryParam(WebSocketConstants.STREAM_NAME) String streamName,
			@QueryParam(WebSocketConstants.MAIN_TRACK) String mainTrack,
			@QueryParam(WebSocketConstants.META_DATA) String metaData,
			@QueryParam(WebSocketConstants.LINK_SESSION) String linkedSession,
			@HeaderParam("Authorization") String token, 
			@Parameter String sdp) {

		PublishParameters publishParameters = new PublishParameters(streamId);
		publishParameters.setToken(token);
		
		publishParameters.setEnableVideo(enableVideo == null || enableVideo);
		publishParameters.setEnableAudio(enableAudio == null || enableAudio);
		publishParameters.setSubscriberId(subscriberId);
		publishParameters.setSubscriberCode(subscriberCode);
		publishParameters.setStreamName(streamName);
		publishParameters.setMainTrack(mainTrack);
		publishParameters.setMetaData(metaData);

		
		String sessionId = UUID.randomUUID().toString();

		CompletableFuture<Result> startHttpSignaling = getApplication().startHttpSignaling(publishParameters, sdp, sessionId);

		return startHttpSignaling.exceptionally(e -> {
			logger.error("Could not complete webrtc http signaling: {}", ExceptionUtils.getStackTrace(e));
			
			return null;
		}).thenApply(result -> { return prepareResponse(result, sessionId, uriInfo); });

	}

	public Response prepareResponse(Result result, String sessionId, UriInfo uriInfo) {

		try {
			if(!result.isSuccess())
			{
				return Response.status(Status.FORBIDDEN)
						.entity(result.getMessage())
						.build();
			}


			//TODO: make it parametric
			String defaultStunStr = "stun:stun1.l.google.com:19302; rel=ice-server";
			ArrayList<String> extensions = new ArrayList<>();
			extensions.add(defaultStunStr);
			
			
			String turnAddr = getApplication().getAppSettings().getStunServerURI();
			String turnServerUsername = getApplication().getAppSettings().getTurnServerUsername();
			String turnServerPassword = getApplication().getAppSettings().getTurnServerCredential();
			String turnServerInfo = "";

			if(StringUtils.isNotBlank(turnServerUsername) && StringUtils.isNotBlank(turnServerPassword)){
				//TODO: Increase security here
				turnServerInfo = turnAddr + "?transport=udp; rel=\"ice-server\" username="+turnServerUsername+";"+" credential="+turnServerPassword;
				extensions.add(turnServerInfo);
			}

			

			String eTag = sessionId; // Replace with parsed ETag
			String resource = uriInfo.getRequestUri().toString()+"/"+eTag;
			URI uri = URI.create(resource);

			return Response.created(uri)
					.status(Status.CREATED)
					.entity(result.getMessage())
					.header("ETag", eTag)
					.header("Link", String.join(",", extensions))
					.type("application/sdp")
					.build();
		}
		catch (Exception e) {
			//Complete future with error hides the exception so we need to explicitly log it and return it
			logger.error("Exception in prepareResponse {}", ExceptionUtils.getStackTrace(e));
			return Response.serverError().build();
		}

	}

	@Operation(summary = "Stop a webrtc stream through WebRTC-HTTP ingestion protocol(WHIP). HTTP for signaling.")
	@DELETE
	@Consumes({ "application/sdp" })
	@Path("/{streamId}/{eTag}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response stopWhipPublish(@PathParam("streamId") String streamId, @PathParam("eTag") String eTag){

		Result result = getApplication().stopWhipBroadcast(streamId, eTag);
		if(result.isSuccess()){
			return Response.ok().entity(result).build();
		}
		return Response.status(Status.NOT_FOUND).entity(result).build();
	}


}
