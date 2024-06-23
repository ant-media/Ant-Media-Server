package io.antmedia.whip;

import java.net.URI;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONObject;
import org.springframework.stereotype.Component;

import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.rest.RestServiceBase;
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
	
	
	
	@GET
	@Path("/hello")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getBroadcast(@Parameter(description = "id of the broadcast", required = true) @QueryParam("id") String id) {
	
			return Response.status(Status.OK).entity(id).build();
		
	}
	
	
	@Operation(summary = "Publish a webrtc stream through WebRTC-HTTP ingestion protocol(WHIP). HTTP for signaling.")
	@POST
	@Consumes({ "application/sdp" })
	@Path("/{streamId}")
	@Produces(MediaType.APPLICATION_JSON)
	public CompletableFuture<Response> startWhipPublish(@Context UriInfo uriInfo, @PathParam(WebSocketConstants.STREAM_ID) String streamId,
														@QueryParam(WebSocketConstants.TOKEN) String tokenId,
														@QueryParam(WebSocketConstants.VIDEO) boolean enableVideo,
														@QueryParam(WebSocketConstants.AUDIO) boolean enableAudio,
														@QueryParam(WebSocketConstants.SUBSCRIBER_ID) String subscriberId,
														@QueryParam(WebSocketConstants.SUBSCRIBER_CODE) String subscriberCode,
														@QueryParam(WebSocketConstants.STREAM_NAME) String streamName,
														@QueryParam(WebSocketConstants.MAIN_TRACK) String mainTrack,
														@QueryParam(WebSocketConstants.META_DATA) String metaData,
														@QueryParam(WebSocketConstants.LINK_SESSION) String linkedSession,
														@Parameter String sdp) {
		CompletableFuture<String> completionSignal = new CompletableFuture<>();

		PublishParameters publishParameters = new PublishParameters(streamId);
		publishParameters.setTokenId(tokenId);
		publishParameters.setEnableVideo(enableVideo);
		publishParameters.setEnableAudio(enableAudio);
		publishParameters.setSubscriberId(subscriberId);
		publishParameters.setSubscriberCode(subscriberCode);
		publishParameters.setStreamName(streamName);
		publishParameters.setMainTrack(mainTrack);
		publishParameters.setMetaData(metaData);
		

		String sessionId = RandomStringUtils.randomAlphanumeric(6);

		getApplication().startHttpSignaling(publishParameters, sdp, sessionId, completionSignal);

		 return completionSignal.exceptionally(e -> {
			logger.error("Could not complete webrtc http signaling.");
			e.printStackTrace();
			return null;
		}).thenApply(serverSdp -> {
			if(StringUtils.isBlank(serverSdp)){
				//if serversdp is null, it means that it's not allowed. 
				//TODO: refactor the logic
                return Response.status(Status.FORBIDDEN)
						.build();
			}

			 String turnAddr = getApplication().getAppSettings().getStunServerURI();
			 String turnServerUsername = getApplication().getAppSettings().getTurnServerUsername();
			 String turnServerPassword = getApplication().getAppSettings().getTurnServerCredential();
			 String turnServerInfo = "";

			 if(StringUtils.isNotBlank(turnServerUsername) && StringUtils.isNotBlank(turnServerPassword)){
				 turnServerInfo = turnAddr + "?transport=udp; rel=\"ice-server\" username="+turnServerUsername+";"+" credential="+turnServerPassword;
			 }

			 String defaultStunStr = "stun:stun1.l.google.com:19302; rel=ice-server";

			 String eTag = sessionId; // Replace with parsed ETag
			 String resource = uriInfo.getRequestUri().toString()+"/"+eTag;
			 ArrayList<String> extensions = new ArrayList<>();
			 extensions.add(defaultStunStr);
			 if(StringUtils.isNotBlank(turnServerInfo)){
				 extensions.add(turnServerInfo);
			 }

			 Response response = Response.created(URI.create(resource))
					 .entity(serverSdp)
					 .header("ETag", eTag)
					 .header("Link", String.join(",", extensions))
					 .type("application/sdp")
					 .build();
			return response;

		});

	}

	@Operation(summary = "Stop a webrtc stream through WebRTC-HTTP ingestion protocol(WHIP). HTTP for signaling.")
	@DELETE
	@Consumes({ "application/sdp" })
	@Path("/{streamId}/{eTag}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response stopWhipPublish(@PathParam("streamId") String streamId,@PathParam("eTag") String eTag){

		boolean stopWhipBroadcastRes = getApplication().stopWhipBroadcast(streamId, eTag);
		if(stopWhipBroadcastRes){
			return Response.ok().build();
		}
		return Response.status(Status.NOT_FOUND).build();
	}


}
