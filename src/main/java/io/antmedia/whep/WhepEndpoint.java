package io.antmedia.whep;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.UUID;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.UriInfo;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import io.antmedia.rest.RestServiceBase;
import io.antmedia.rest.model.Result;
import io.antmedia.webrtc.PlayParameters;
import io.antmedia.websocket.WebSocketConstants;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.ExternalDocumentation;

@OpenAPIDefinition(
        info = @Info(
                description = "Ant Media Server WHEP endpoint",
                version = "v2.0",
                title = "Ant Media Server WHEP Endpoint",
                contact = @Contact(name = "Ant Media Info", email = "contact@antmedia.io", url = "https://antmedia.io"),
                license = @License(name = "Apache 2.0", url = "https://www.apache.org/licenses/LICENSE-2.0")),
        externalDocs = @ExternalDocumentation(description = "Rest Guide", url="https://antmedia.io/docs")
)
@Component
//we bind it to /whep path in web.xml
@Path("/")
public class WhepEndpoint extends RestServiceBase {

    private static final Logger logger = LoggerFactory.getLogger(WhepEndpoint.class);

    /**
     * Get the answer from the client and start the playback
     * @param uriInfo
     * @param streamId
     * @param subscriberId
     * @param viewerInfo
     * @param token
     * @param sdp Answer from the client
     * @return SDP Answer
     */
    @Operation(summary = "Play a webrtc stream through WebRTC-HTTP egress protocol(WHEP). HTTP for signaling.")
    @POST
    @Consumes({ "application/sdp" })
    @Path("/{streamId}")
    @Produces("application/sdp")
    public CompletableFuture<Response> startWhepPlay(@Context UriInfo uriInfo, 
                                                   @PathParam(WebSocketConstants.STREAM_ID) String streamId,
                                                   @QueryParam(WebSocketConstants.SUBSCRIBER_ID) String subscriberId,
                                                   @QueryParam("viewerInfo") String viewerInfo,
                                                   @HeaderParam("Authorization") String token, 
                                                   @Parameter String sdp) {
        
        // Generate a unique session ID
        String sessionId = UUID.randomUUID().toString();

        logger.info("Whep playback initiated for stream: {} with sessionId: {}, viewerInfo: {}", streamId, sessionId, viewerInfo);
        List<String> enabledTracks = new ArrayList<>();
        // Create play parameters
        PlayParameters playParameters = new PlayParameters(streamId, null, null, false, null, enabledTracks, false, subscriberId, null, null, viewerInfo, sessionId, "default", false, null, false, true);

        // Start HTTP signaling for playback
        CompletableFuture<Result> startHttpSignaling = getApplication().startWhepHttpSignaling(playParameters, sdp, sessionId);
        
        return startHttpSignaling.thenApply(result -> {
            logger.info("WHEP playback started successfully for stream: {} waiting for answer SDP", streamId);
            return prepareResponse(result, uriInfo);
        }).exceptionally(e -> {
            // Complete future with error hides the exception so we need to explicitly log it and return it
            logger.error("Error during WHEP playback for stream: {}", streamId, e);
            return Response.serverError().build();
        });
    }
    
    /**
     * Prepares the HTTP response for a WHEP session
     * @param result
     * @param uriInfo
     * @return HTTP Response
     */
    public Response prepareResponse(Result result, UriInfo uriInfo) {
        try {
            if (!result.isSuccess()) {
                return Response.status(Status.FORBIDDEN).entity(result.getMessage()).build();
            }
            
            List<String> extensions = new ArrayList<>();
            
            // Create resource URI with eTag
            String resource = uriInfo.getRequestUri().toString();
            URI uri = URI.create(resource);
            
            return Response.created(uri)
                    .status(Status.ACCEPTED)
                    .entity(result.getMessage())
                    .header("Link", String.join(",", extensions))
                    .type("application/sdp")
                    .build();
        }
        catch (Exception e) {
            // Complete future with error hides the exception so we need to explicitly log it and return it
            logger.error("Error preparing the response, error: {}", ExceptionUtils.getStackTrace(e));
            return Response.serverError().build();
        }
    }

    /**
     * Stop a WebRTC playback through WebRTC-HTTP egress protocol (WHEP)
     * @param streamId
     * @param eTag
     * @return Result
     */
    @Operation(summary = "Stop a webrtc playback through WebRTC-HTTP egress protocol(WHEP). HTTP for signaling.")
    @DELETE
    @Consumes({ "application/sdp" })
    @Path("/{streamId}/{eTag}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response stopWhepPlay(@PathParam("streamId") String streamId, @PathParam("eTag") String eTag){
        
        Result result = getApplication().stopWhepPlay(streamId, eTag);
        if(result.isSuccess()){
            return Response.ok().entity(result).build();
        }
        return Response.status(Status.NOT_FOUND).entity(result).build();
    }
} 