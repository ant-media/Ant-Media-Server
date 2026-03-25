package io.antmedia.rest;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.stereotype.Component;

import com.amazonaws.event.ProgressEvent;
import com.amazonaws.event.ProgressEventType;
import com.amazonaws.event.ProgressListener;
import com.amazonaws.util.Base32;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.AppSettings;
import io.antmedia.RecordType;
import io.antmedia.StreamIdValidator;
import io.antmedia.cluster.IClusterNotifier;
import io.antmedia.cluster.IStreamInfo;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.datastore.db.types.BroadcastUpdate;
import io.antmedia.datastore.db.types.ConnectionEvent;
import io.antmedia.datastore.db.types.Endpoint;
import io.antmedia.datastore.db.types.Subscriber;
import io.antmedia.datastore.db.types.Token;
import io.antmedia.filter.TokenFilterManager;
import io.antmedia.ipcamera.OnvifCamera;
import io.antmedia.muxer.HLSMuxer;
import io.antmedia.muxer.IAntMediaStreamHandler;
import io.antmedia.muxer.MuxAdaptor;
import io.antmedia.muxer.Muxer;
import io.antmedia.muxer.RecordMuxer;
import io.antmedia.rest.model.BasicStreamInfo;
import io.antmedia.rest.model.Result;
import io.antmedia.security.ITokenService;
import io.antmedia.security.TOTPGenerator;
import io.antmedia.statistic.type.WebRTCAudioReceiveStats;
import io.antmedia.statistic.type.WebRTCAudioSendStats;
import io.antmedia.statistic.type.WebRTCVideoReceiveStats;
import io.antmedia.statistic.type.WebRTCVideoSendStats;
import io.antmedia.storage.StorageClient;
import io.antmedia.streamsource.StreamFetcher;
import io.antmedia.webrtc.api.IWebRTCAdaptor;
import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.servers.Server;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

@OpenAPIDefinition(
		info = @Info(
				description = "Ant Media Server REST API for Broadcasts",
				version = "v2.0",
				title = "Ant Media Server REST API Reference",
				contact = @Contact(name = "Ant Media Info", email = "contact@antmedia.io", url = "https://antmedia.io"),
				license = @License(name = "Apache 2.0", url = "https://www.apache.org/licenses/LICENSE-2.0")),
		externalDocs = @ExternalDocumentation(description = "Rest Guide", url="https://antmedia.io/docs"),
		servers = {
				@Server(
						description = "test server",
						url = "https://test.antmedia.io:5443/Sandbox/rest/"

						)}

		)
@Component
@Path("/v2/broadcasts")
public class BroadcastRestService extends RestServiceBase{


	private static final String STREAM_ID_NOT_VALID = "Stream id not valid";
	private static final String RELATIVE_MOVE = "relative";
	private static final String ABSOLUTE_MOVE = "absolute";
	private static final String CONTINUOUS_MOVE = "continuous";
	
	private static final int MIN_TOTP_EXPIRATION_TIME = 10;
    private static final int MAX_TOTP_EXPIRATION_TIME = Integer.MAX_VALUE;
    

	@Schema(description="Simple generic statistics class to return single values")
	public static class SimpleStat {
		@Schema(description = "the stat value")
		public long number;

		public SimpleStat(long number) {
			this.number = number;
		}

		public long getNumber() {
			return number;
		}
	}

	@Schema(description="Aggregation of WebRTC Low Level Send Stats")
	public static class WebRTCSendStats
	{
		@Schema(description = "Audio send stats")
		private final WebRTCAudioSendStats audioSendStats;

		@Schema(description = "Video send stats")
		private final WebRTCVideoSendStats videoSendStats;

		public WebRTCSendStats(WebRTCAudioSendStats audioSendStats, WebRTCVideoSendStats videoSendStats) {
			this.audioSendStats = audioSendStats;
			this.videoSendStats = videoSendStats;
		}

		public WebRTCVideoSendStats getVideoSendStats() {
			return videoSendStats;
		}

		public WebRTCAudioSendStats getAudioSendStats() {
			return audioSendStats;
		}
	}

	@Schema(description="Aggregation of WebRTC Low Level Receive Stats")
	public static class WebRTCReceiveStats
	{
		@Schema(description = "Audio receive stats")
		private final WebRTCAudioReceiveStats audioReceiveStats;

		@Schema(description = "Video receive stats")
		private final WebRTCVideoReceiveStats videoReceiveStats;

		public WebRTCReceiveStats(WebRTCAudioReceiveStats audioReceiveStats, WebRTCVideoReceiveStats videoReceiveStats) {
			this.audioReceiveStats = audioReceiveStats;
			this.videoReceiveStats = videoReceiveStats;
		}

		public WebRTCVideoReceiveStats getVideoReceiveStats() {
			return videoReceiveStats;
		}

		public WebRTCAudioReceiveStats getAudioReceiveStats() {
			return audioReceiveStats;
		}
	}


	@Operation(description = "Creates a Broadcast, IP Camera or Stream Source and returns the full broadcast object with rtmp address and "
			+ "other information. The different between Broadcast and IP Camera or Stream Source is that Broadcast is ingested by Ant Media Server"
			+ "IP Camera or Stream Source is pulled by Ant Media Server")
	@ApiResponse(responseCode = "400", description = "If stream id is already used in the data store, it returns error", 
	content = @Content(
			mediaType = "application/json",
			schema = @Schema(implementation = Result.class)
			)
			)
	@ApiResponse(responseCode = "200", description = "Returns the created stream", 
	content = @Content(
			mediaType = "application/json",
			schema = @Schema(implementation = Broadcast.class)
			)
			)
	@POST
	@Consumes({ MediaType.APPLICATION_JSON })
	@Path("/create")
	@Produces(MediaType.APPLICATION_JSON)
	public Response createBroadcast(@Parameter(description = "Broadcast object. Set the required fields, it may be null as well.", required = false) Broadcast broadcast,
			@Parameter(description = "Only effective if stream is IP Camera or Stream Source. If it's true, it starts automatically pulling stream. Its value is false by default", required = false) @QueryParam("autoStart") boolean autoStart) {

		if (broadcast != null && broadcast.getStreamId() != null) {

			try {
				broadcast.setStreamId(broadcast.getStreamId().trim());

				if (!broadcast.getStreamId().isEmpty()) 
				{
					// make sure stream id is not set on rest service
					Broadcast broadcastTmp = getDataStore().get(broadcast.getStreamId());
					if (broadcastTmp != null) 
					{
						return Response.status(Status.BAD_REQUEST).entity(new Result(false, "Stream id is already being used. Please change stream id or keep it empty")).build();
					}
					else if (!StreamIdValidator.isStreamIdValid(broadcast.getStreamId())) 
					{
						return Response.status(Status.BAD_REQUEST).entity(new Result(false, "Stream id is not valid.")).build();
					}
				}
			}
			catch (Exception e) 
			{
				logger.error(ExceptionUtils.getStackTrace(e));
				return Response.status(Status.BAD_REQUEST).entity(new Result(false, "Stream id set generated exception")).build(); 
			}


		}

		Object returnObject = new Result(false, "unexpected parameters received");

		if (autoStart)  
		{
			//auto is only effective for IP Camera or Stream Source 
			//so if it's true, it should be IP Camera or Stream Soruce
			//otherwise wrong parameter
			if (broadcast != null) {
				returnObject = addStreamSource(broadcast);
			}
		}
		else {
			//TODO we need to refactor this method. Refactor validateStreamURL and checkStreamURL
			if (broadcast != null && 
					((AntMediaApplicationAdapter.IP_CAMERA.equals(broadcast.getType()) && !validateStreamURL(broadcast.getIpAddr()))
							|| 
							(AntMediaApplicationAdapter.STREAM_SOURCE.equals(broadcast.getType()) && !checkStreamUrl(broadcast.getStreamUrl()))
							)
					) {
				return Response.status(Status.BAD_REQUEST).entity(new Result(false, "Stream url is not valid. ")).build();
			}
			if(broadcast != null && broadcast.getSubFolder() != null) {
				if(broadcast.getSubFolder().contains(".."))
					return Response.status(Status.BAD_REQUEST).entity(new Result(false, "Subfolder is not valid. ")).build();
			}
			returnObject = createBroadcastWithStreamID(broadcast);

		}

		return Response.status(Status.OK).entity(returnObject).build();
	}

	@Operation(summary = "Delete broadcast from data store and stop if it's broadcasting")
	@ApiResponse(responseCode = "200", description = "If it's deleted, success is true. If it's not deleted, success if false.",
	content = @Content(
			mediaType = "application/json",
			schema = @Schema(implementation = Broadcast.class)
			)
			)
	@DELETE
	@Consumes({ MediaType.APPLICATION_JSON })
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public Result deleteBroadcast(@Parameter(description = " Id of the broadcast", required = true) @PathParam("id") String id,
			@Parameter(description = "Deletion request for subtracks also", required = false) @QueryParam("deleteSubtracks") Boolean deleteSubtracks) {
		return super.deleteBroadcast(id, deleteSubtracks);		
	}

	@Operation(description = "Delete multiple broadcasts from data store and stop if they are broadcasting")
	@ApiResponse(responseCode = "200", description = "If it's deleted, success is true. If it's not deleted, success if false.",
	content = @Content(
			mediaType = "application/json",
			schema = @Schema(implementation = Broadcast.class)
			)
			)
	@DELETE
	@Consumes({ MediaType.APPLICATION_JSON })
	@Path("/")
	@Produces(MediaType.APPLICATION_JSON)
	public Result deleteBroadcastsBulk(@Parameter(description = "Comma-separated stream Ids", required = true) @QueryParam("ids") String streamIds) 
	{
		if (StringUtils.isNotBlank(streamIds)) {
			return super.deleteBroadcasts(streamIds.split(","));
		}
		else {
			return new Result(false, "ids parameter is blank");
		}
	}


	@Operation(description = "Get broadcast object")
	@ApiResponse(responseCode = "200", description = "Return the broadcast object",
	content = @Content(
			mediaType = "application/json",
			schema = @Schema(implementation = Broadcast.class)
			)
			)
	@ApiResponse(responseCode = "404", description = "Broadcast object not found")
	@GET
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getBroadcast(@Parameter(description = "id of the broadcast", required = true) @PathParam("id") String id) {
		Broadcast broadcast = null;
		if (id != null) {
			broadcast = lookupBroadcast(id);
		}
		if (broadcast != null) {
			return Response.status(Status.OK).entity(broadcast).build();
		}
		else {
			return Response.status(Status.NOT_FOUND).build();
		}
	}

	@Operation(description = "Gets the broadcast list from database. It returns max 50 items at a time")
	@GET
	@Path("/list/{offset}/{size}")
	@Produces(MediaType.APPLICATION_JSON)
	public List<Broadcast> getBroadcastList(@Parameter(description = "This is the offset of the list, it is useful for pagination. If you want to use sort mechanism, we recommend using Mongo DB.", required = true) @PathParam("offset") int offset,
			@Parameter(description = "Number of items that will be fetched. If there is not enough item in the datastore, returned list size may less then this value", required = true) @PathParam("size") int size,
			@Parameter(description = "Type of the stream. Possible values are \"liveStream\", \"ipCamera\", \"streamSource\", \"VoD\"", required = false) @QueryParam("type_by") String typeBy,
			@Parameter(description = "Field to sort. Possible values are \"name\", \"date\", \"status\"", required = false) @QueryParam("sort_by") String sortBy,
			@Parameter(description = "\"asc\" for Ascending, \"desc\" Descending order", required = false) @QueryParam("order_by") String orderBy,
			@Parameter(description = "Search parameter, returns specific items that contains search string", required = false) @QueryParam("search") String search
			) {
		return getDataStore().getBroadcastList(offset, size, typeBy, sortBy, orderBy, search);
	}


	@Operation(description = "Updates the Broadcast objects fields if it's not null." + 
			" The updated fields are as follows: name, description, userName, password, IP address, streamUrl of the broadcast. " + 
			"It also updates the social endpoints")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "If it's updated, success field is true. If it's not updated, success field is false.")
	})
	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Result updateBroadcast(@Parameter(description="Broadcast id", required = true) @PathParam("id") String id, 
			@Parameter(description="Broadcast object with the updates") BroadcastUpdate broadcast) {
		Result result = new Result(false);
		if (id != null && broadcast != null) 
		{
			Broadcast broadcastInDB = getDataStore().get(id);
			if (broadcastInDB == null) {
				String streamId = id.replaceAll(REPLACE_CHARS, "_");
				logger.info("Broadcast with stream id: {} is null", streamId);
				return new Result(false, "Broadcast with streamId: " + streamId + " does not exist");
			}

			if (broadcastInDB.getType() != null && 
					(broadcastInDB.getType().equals(AntMediaApplicationAdapter.IP_CAMERA) || 
							broadcastInDB.getType().equals(AntMediaApplicationAdapter.STREAM_SOURCE) || 
							broadcastInDB.getType().equals(AntMediaApplicationAdapter.VOD) || 
							broadcastInDB.getType().equals(AntMediaApplicationAdapter.PLAY_LIST))) 
			{
				result = super.updateStreamSource(id, broadcast, broadcastInDB);
			}
			else 
			{
				result = super.updateBroadcast(id, broadcast);
			}

		}
		return result;
	}

	@Operation(description = "Gets the durations of the stream url in milliseconds",
			responses = {
					@ApiResponse(responseCode = "200", description = "If operation is successful, duration will be in dataId field and success field is true. "
							+ "If it's failed, errorId has the error code(-1: duration is not available, -2: url is not opened, -3: cannot get stream info) and success field is false",
							content = @Content(
									mediaType = "application/json",
									schema = @Schema(implementation = Result.class)
									))
	}
			)

	@GET
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/duration")
	@Produces(MediaType.APPLICATION_JSON)
	public Result getDuration(@Parameter(description="Url of the stream that its duration will be returned", required=true) @QueryParam("url")  String url) {
		Result result = new Result(false);
		if (StringUtils.isNotBlank(url)) {
			long durationInMs = Muxer.getDurationInMs(url,null);
			if (durationInMs >= 0) {
				result.setSuccess(true);
				result.setDataId(Long.toString(durationInMs));
			}
			else {
				result.setErrorId((int)durationInMs);
			}
		}

		return result;
	}


	@Operation(description = "Seeks the playing stream source, vod or playlist on the fly. It accepts seekTimeMs parameter in milliseconds")
	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/{id}/seek-time/{seekTimeMs}")
	@Produces(MediaType.APPLICATION_JSON)
	public Result updateSeekTime(@Parameter(description="Broadcast id", required = true) @PathParam("id") String id, 
			@Parameter(description="Seek time in milliseconds", required = true) @PathParam("seekTimeMs") long seekTimeMs) {
		Result result = new Result(false);
		if (StringUtils.isNotBlank(id)) 
		{

			StreamFetcher streamFetcher = getApplication().getStreamFetcherManager().getStreamFetcher(id);
			if (streamFetcher != null) {
				streamFetcher.seekTime(seekTimeMs);
				result.setSuccess(true);
			}
			else {
				result.setMessage("Not active stream source found with this id: " + id + " make sure you give the id of a running stream source");
			}
		}
		else {
			result.setMessage("Id field is blank.");
		}
		return result;

	}


	@Operation(summary = "Adds a third party RTMP or SRT end point to the stream",
			description = "It supports adding RTMP or SRT restreaming endpoints after broadcast is started. Resolution can be specified to send a specific adaptive resolution. If an URL is already added to a stream, trying to add the same Endpoint URL will return false.",
			responses = {
					@ApiResponse(responseCode = "200", description = "Add Endpoint URL response",
							content = @Content(
									mediaType = "application/json",
									schema = @Schema(implementation = Result.class)
									))
	}
			)

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/{id}/endpoint")
	@Produces(MediaType.APPLICATION_JSON)
	public Result addEndpoint(@Parameter(description = "Broadcast id", required = true) @PathParam("id") String id,
								@Parameter(description = "SRT or RTMP URL of the destination endpoint where the stream will be republished. Encode the URL if required", required = true) Endpoint endpoint,
								@Parameter(description = "Resolution height of the broadcast that is wanted to send to the endpoint. ", required = false) @QueryParam("resolutionHeight") int resolutionHeight) {

		String endpointUrl = null;
		Result result = new Result(false);

		if(endpoint == null || endpoint.getEndpointUrl() == null) {
			result.setMessage("Missing Endpoint url");
			return result;
		}

		Broadcast broadcast = getDataStore().get(id);
		if (broadcast == null){
			result.setMessage("Stream does not exist with Id: " + id);
			return result;
		}

		List<Endpoint> endpoints = broadcast.getEndPointList();
		if (endpoints == null || endpoints.stream().noneMatch(o -> o.getEndpointUrl().equals(endpoint.getEndpointUrl())))
		{
			endpointUrl = endpoint.getEndpointUrl();

			if (broadcast.getStatus().equals(IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING))
			{
			  result = processEndpoint(broadcast.getStreamId(), broadcast.getOriginAdress(), endpointUrl, true, resolutionHeight);
			  if (result.isSuccess())
			  {
				result = super.addEndpoint(id, endpoint);
			  }
			}
			else
			{
			  result = super.addEndpoint(id, endpoint);
			}


			if (!result.isSuccess())
			{
			  result.setMessage("Endpoint is not added to stream: " + id);

			}
			logRtmpEndpointInfo(id, endpoint, result.isSuccess());
		}
		else
		{
			result.setMessage("Endpoint is not added to datastore for stream " + id + ". It is already added ->" + endpoint.getEndpointUrl());
		}

		return result;
	}

	private void logRtmpEndpointInfo(String id, Endpoint endpoint, boolean result) {
		if (logger.isInfoEnabled()) {
			logger.info("Rtmp endpoint({}) adding to the stream: {} is {}", endpoint.getEndpointUrl().replaceAll(REPLACE_CHARS, "_") , id.replaceAll(REPLACE_CHARS, "_"), result);
		}
	}

	@Operation(summary = "Removes a third party RTMP or SRT end point from the stream",
			description = "It supports removing RTMP or SRT restreaming endpoints after broadcast is started. Resolution can be specified if endpoint is added with resolution. If an URL is not added to a stream, trying to remove that Endpoint URL will return false.",
			responses = {
					@ApiResponse(responseCode = "200", description = "Remove Endpoint URL response",
							content = @Content(
									mediaType = "application/json",
									schema = @Schema(implementation = Result.class)
									))
	}
			)
	@DELETE
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/{id}/endpoint")
	@Produces(MediaType.APPLICATION_JSON)
	public Result removeEndpoint(@Parameter(description = "Broadcast id", required = true) @PathParam("id") String id,
			@Parameter(description = "RTMP or SRT URL of the target endpoint that should be stopped", required = true) @QueryParam("endpointServiceId") String endpointServiceId,
			@Parameter(description = "Resolution specifier if endpoint has been added with resolution. Only applicable if user added RTMP or SRT endpoint with a resolution speficier. Otherwise won't work and won't remove the endpoint.")
			@QueryParam("resolutionHeight") int resolutionHeight){

		//Get rtmpURL with broadcast
		String rtmpUrl = null;
		Broadcast broadcast = getDataStore().get(id);
		Result result = new Result(false);
		//check if resolutoonHeight is valid, if it's not valid, set it to 0 to not encounter any problem in remove process. If resolutionHeight is 0, it will remove endpoint without resolution control.
		resolutionHeight = resolutionHeight > 0 ? resolutionHeight : 0;
		if (broadcast != null && endpointServiceId != null && broadcast.getEndPointList() != null && !broadcast.getEndPointList().isEmpty())
		{

			Endpoint endpoint = getEndpointMuxerFromList(endpointServiceId, broadcast);
			if (endpoint != null && endpoint.getEndpointUrl() != null) {
				rtmpUrl = endpoint.getEndpointUrl();
				result = removeRTMPEndpointProcess(broadcast, endpoint, resolutionHeight, id);
			}
		}
		if (logger.isInfoEnabled())
		{
			logger.info("Rtmp endpoint({}) removal operation is {} from the stream: {}", rtmpUrl != null ? rtmpUrl.replaceAll(REPLACE_CHARS, "_") : null , result.isSuccess(), id.replaceAll(REPLACE_CHARS, "_"));
		}
		return result;
	}


	private Result removeRTMPEndpointProcess(Broadcast broadcast, Endpoint endpoint, int resolutionHeight, String id) {
		Result result;

		if (IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING.equals(broadcast.getStatus()))
		{
			result = processEndpoint(broadcast.getStreamId(), broadcast.getOriginAdress(), endpoint.getEndpointUrl(), false, resolutionHeight);
			if (result.isSuccess())
			{
				result = super.removeRTMPEndpoint(id, endpoint);
			}
		}
		else
		{
			result = super.removeRTMPEndpoint(id, endpoint);
		}


		return result;
	}

	private Endpoint getEndpointMuxerFromList(String endpointServiceId, Broadcast broadcast) {
		Endpoint endpoint = null;
		for(Endpoint selectedEndpoint: broadcast.getEndPointList())
		{
			if(selectedEndpoint.getEndpointServiceId().equals(endpointServiceId)) {
				endpoint = selectedEndpoint;
			}
		}
		return endpoint;
	}


	@Operation(summary = "Get the total number of broadcasts",
			description = "Retrieves the total number of broadcasts.",
			responses = {
					@ApiResponse(responseCode = "200", description = "Total number of broadcasts",
							content = @Content(
									mediaType = "application/json",
									schema = @Schema(implementation = SimpleStat.class)
									))
	}
			)
	@GET
	@Path("/count")
	@Produces(MediaType.APPLICATION_JSON)
	public SimpleStat getTotalBroadcastNumberV2() {
		return new SimpleStat(getDataStore().getTotalBroadcastNumber());
	}

	@Operation(summary = "Get the number of broadcasts based on search criteria",
			description = "Retrieves the number of broadcasts matching the specified search criteria.",
			responses = {
					@ApiResponse(responseCode = "200", description = "Number of broadcasts for searched items",
							content = @Content(
									mediaType = "application/json",
									schema = @Schema(implementation = SimpleStat.class)
									))
	}
			)
	@GET
	@Path("/count/{search}")
	@Produces(MediaType.APPLICATION_JSON)
	public SimpleStat getTotalBroadcastNumberV2(
			@Parameter(description = "Search parameter to get the number of items including it ", required = true) @PathParam("search") String search)
	{
		return new SimpleStat(getDataStore().getPartialBroadcastNumber(search));
	}

	@Operation(summary = "Return the active live streams",
			description = "Retrieves the currently active live streams.",
			responses = {
					@ApiResponse(responseCode = "200", description = "Active live streams",
							content = @Content(
									mediaType = "application/json",
									schema = @Schema(implementation = SimpleStat.class)
									))
	}
			)
	@GET
	@Path("/active-live-stream-count")
	@Produces(MediaType.APPLICATION_JSON)
	public SimpleStat getAppLiveStatistics() {
		return new SimpleStat(getDataStore().getActiveBroadcastCount());
	}




	@Operation(description = "Generates random one-time token for specified stream")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Returns token",
					content = @Content(mediaType = "application/json", 
					schema = @Schema(implementation = Token.class))),
			@ApiResponse(responseCode = "400", description = "When there is an error in creating token",
			content = @Content(mediaType = "application/json", 
			schema = @Schema(implementation = Result.class)))
	})
	@GET
	@Path("/{id}/token")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getTokenV2 (@Parameter(description = "The id of the stream", required = true) @PathParam("id")String streamId,
			@Parameter(description = "The expire time of the token. It's in unix timestamp seconds", required = true) @QueryParam("expireDate") long expireDate,
			@Parameter(description = "Type of the token. It may be play or publish ", required = true) @QueryParam("type") String type,
			@Parameter(description = "Room Id that token belongs to. It's not mandatory ", required = false) @QueryParam("roomId") String roomId) 
	{
		Object result = super.getToken(streamId, expireDate, type, roomId);
		if (result instanceof Token) {
			return Response.status(Status.OK).entity(result).build();
		}
		else {
			return Response.status(Status.BAD_REQUEST).entity(result).build();
		}
	}


	@Operation(description = "Generates JWT token for specified stream. It's not required to let the server generate JWT. Generally JWT tokens should be generated on the client side.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Returns token",
					content = @Content(
							mediaType = "application/json", 
							schema = @Schema(implementation = Token.class)
							)),
			@ApiResponse(responseCode = "400", description = "When there is an error in creating token",
			content = @Content(
					mediaType = "application/json", 
					schema = @Schema(implementation = Result.class)
					))
	})
	@GET
	@Path("/{id}/jwt-token")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getJwtTokenV2 (@Parameter(description = "The id of the stream", required = true) @PathParam("id")String streamId,
			@Parameter(description = "The expire time of the token. It's in unix timestamp seconds.", required = true) @QueryParam("expireDate") long expireDate,
			@Parameter(description = "Type of the JWT token. It may be play or publish ", required = true) @QueryParam("type") String type,
			@Parameter(description = "Room Id that token belongs to. It's not mandatory ", required = false) @QueryParam("roomId") String roomId) 
	{
		Object result = super.getJwtToken(streamId, expireDate, type, roomId);
		if (result instanceof Token) {
			return Response.status(Status.OK).entity(result).build();
		}
		else {
			return Response.status(Status.BAD_REQUEST).entity(result).build();
		}
	}

	@Operation(summary = "Perform validation of token for requested stream",
			description = "If validated, success field is true, not validated success field is false",
			responses = {
					@ApiResponse(responseCode = "200", description = "Token validation response",
							content = @Content(
									mediaType = "application/json",
									schema = @Schema(implementation = Result.class)
									))
	}
			)
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/validate-token")
	@Produces(MediaType.APPLICATION_JSON)
	public Result validateTokenV2(@Parameter(description = "Token to be validated", required = true) Token token) 
	{
		boolean result =  false;
		Token validateToken = super.validateToken(token);
		if (validateToken != null) {
			result = true;
		}

		return new Result(result);
	}


	@Operation(summary = "Removes all tokens related with requested stream",
			description = "",
			responses = {
					@ApiResponse(responseCode = "200", description = "Removal of tokens response",
							content = @Content(
									mediaType = "application/json",
									schema = @Schema(implementation = Result.class)
									))
	}
			)
	@DELETE
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/{id}/tokens")
	@Produces(MediaType.APPLICATION_JSON)
	public Result revokeTokensV2(@Parameter(description = "the id of the stream", required = true) @PathParam("id") String streamId) {
		return super.revokeTokens(streamId);
	}


	@Operation(summary = "Get all tokens of requested stream",
			description = "",
			responses = {
					@ApiResponse(responseCode = "200", description = "List of tokens",
							content = @Content(
									mediaType = "application/json",
									schema = @Schema(implementation = Token.class, type = "array")
									))
	}
			)
	@GET
	@Path("/{id}/tokens/list/{offset}/{size}")
	@Produces(MediaType.APPLICATION_JSON)
	public List<Token> listTokensV2(@Parameter(description = "the id of the stream", required = true) @PathParam("id") String streamId,
			@Parameter(description = "the starting point of the list", required = true) @PathParam("offset") int offset,
			@Parameter(description = "size of the return list (max:50 )", required = true) @PathParam("size") int size) {
		List<Token> tokens = null;
		if(streamId != null) {
			tokens = getDataStore().listAllTokens(streamId, offset, size);
		}
		return tokens;
	}

	@Operation(summary = "Get all subscribers of the requested stream",
			description = "It does not return subscriber-stats. Please use subscriber-stats method",
			responses = {
					@ApiResponse(responseCode = "200", description = "List of subscribers",
							content = @Content(
									mediaType = "application/json",
									schema = @Schema(implementation = Subscriber.class, type = "array")
									))
	}
			)
	@GET
	@Path("/{id}/subscribers/list/{offset}/{size}")
	@Produces(MediaType.APPLICATION_JSON)
	public List<Subscriber> listSubscriberV2(@Parameter(description = "the id of the stream", required = true) @PathParam("id") String streamId,
			@Parameter(description = "the starting point of the list", required = true) @PathParam("offset") int offset,
			@Parameter(description = "size of the return list (max:50 )", required = true) @PathParam("size") int size) {
		List<Subscriber> subscribers = null;
		if(streamId != null) {
			subscribers = getDataStore().listAllSubscribers(streamId, offset, size);
		}
		return subscribers;
	}	


	@Operation(summary = "Retrieve all subscriber statistics of the requested stream. ",
			description = "Fetches comprehensive statistics for all subscribers of the specified stream.",
			responses = {
					@ApiResponse(responseCode = "200", description = "List of subscriber statistics",
							content = @Content(
									mediaType = "application/json",
									schema = @Schema(implementation = ConnectionEvent.class, type = "array")
									))
	}
			)
	@GET
	@Path("/{id}/connection-events/{offset}/{size}")
	@Produces(MediaType.APPLICATION_JSON)
	public List<ConnectionEvent> getConnectionEvents(@Parameter(description = "the id of the stream", required = true) @PathParam("id") String streamId,
			@Parameter(description = "the starting point of the list", required = true) @PathParam("offset") int offset,
			@Parameter(description = "size of the return list (max:50 )", required = true) @PathParam("size") int size,
			@Parameter(description = "subscriberId to filter the connections events", required=false) @QueryParam("subscriberId") String subscriberId) {
		List<ConnectionEvent> connectionEvents = new ArrayList<>();
		if(StringUtils.isNotBlank(streamId)) {
			connectionEvents = getDataStore().getConnectionEvents(streamId, subscriberId, offset, size);
		}
		return connectionEvents;
	}


	@Operation(summary = "Add Subscriber to the requested stream",
			description = "Adds a subscriber to the requested stream. If the subscriber's type is 'publish', they can also play the stream, which is critical in conferencing. If the subscriber's type is 'play', they can only play the stream. If 'b32Secret' is not set, it will default to the AppSettings. The length of 'b32Secret' should be a multiple of 8 and use base32 characters A–Z, 2–7.",
			responses = {
					@ApiResponse(responseCode = "200", description = "Result of adding a subscriber",
							content = @Content(
									mediaType = "application/json",
									schema = @Schema(implementation = Result.class)
									))
	}
			)
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/{id}/subscribers")
	@Produces(MediaType.APPLICATION_JSON)
	public Result addSubscriber(
			@Parameter(description = "The id of the stream", required = true) @PathParam("id") String streamId,
			@Parameter(description = "Subscriber to be added to this stream", required = true) Subscriber subscriber) {
		boolean result = false;
		String message = "";
		if (subscriber != null && !StringUtils.isBlank(subscriber.getSubscriberId()) 
				&& subscriber.getSubscriberId().length() > 3 && StringUtils.isNotBlank(streamId)) 
		{
			// add stream id inside the Subscriber
			subscriber.setStreamId(streamId);
			// subscriber is not viewing anyone
			subscriber.setCurrentConcurrentConnections(0);

			boolean secretCodeLengthCorrect = true;
			if (StringUtils.isNotBlank(subscriber.getB32Secret())) {

				try {
					//Check secret code is correct format
					Base32.decode(subscriber.getB32Secret().getBytes());
				}
				catch (Exception e) {
					logger.warn("Secret code is not b32 compatible. It will not add subscriber ");
					secretCodeLengthCorrect = false;
				}
			}

			if (secretCodeLengthCorrect) {
				Integer totpExpiryPeriodSeconds = subscriber.getTotpExpiryPeriodSeconds();
				if (totpExpiryPeriodSeconds == null) {
					logger.info("Custom TOTP expiry period is set from AppSetings:{}", getAppSettings().getTimeTokenPeriod());
					totpExpiryPeriodSeconds = getAppSettings().getTimeTokenPeriod();
					subscriber.setTotpExpiryPeriodSeconds(totpExpiryPeriodSeconds);
				}
				
				if(totpExpiryPeriodSeconds >= MIN_TOTP_EXPIRATION_TIME 
						&& totpExpiryPeriodSeconds <= MAX_TOTP_EXPIRATION_TIME) {
					result = getDataStore().addSubscriber(streamId, subscriber);					
				}
				else {
					logger.info("Custom TOTP expiry period {} is out of range ({},{})",
							totpExpiryPeriodSeconds, MIN_TOTP_EXPIRATION_TIME, MAX_TOTP_EXPIRATION_TIME);
					message = "Custom TOTP expiry period must be between " + MIN_TOTP_EXPIRATION_TIME + " and " + MAX_TOTP_EXPIRATION_TIME;
				}
			}
			else {
				message = "Secret code is not multiple of 8 bytes length. Use b32Secret which is a string and its lenght is multiple of 8 bytes and allowed characters A-Z, 2-7";
			}

		}
		else {
			message = "Missing parameter: Make sure you set subscriber object correctly and make subscriberId's length at least 3";
		}
		return new Result(result, message);
	}
	
	@Operation(description="Return TOTP for the subscriberId, streamId, type. This is a helper method. You can generate TOTP on your end."
			+ "If subscriberId is not in the database, it generates TOTP from the secret in the AppSettings. Secret code is for the subscriberId not in the database"

			+ " secretCode = Base32.encodeAsString({secretFromSettings(publishsecret or playsecret according to the type)} + {subscriberId} + {streamId} + {type(publish or play)} + {Number of X to have the length multiple of 8}"
			+ "'+' means concatenating the strings. There is no explicit '+' in the secretCode ")
	@GET
	@Consumes({ MediaType.APPLICATION_JSON })
	@Path("/{id}/subscribers/{sid}/totp")
	@Produces(MediaType.APPLICATION_JSON)
	public Result getTOTP(@Parameter(description="The id of the stream that TOTP will be generated", required=true) @PathParam("id") String streamId, 
			@Parameter(description="The id of the subscriber that TOTP will be generated ", required=true) @PathParam("sid") String subscriberId, 
			@Parameter(description="The type of token. It's being used if subscriber is not in the database. It can be publish, play", 
			required=false) @QueryParam("type") String type) 
	{

		boolean result = false;
		String message = "";
		String totp = "";
		if (!StringUtils.isAnyBlank(streamId, subscriberId)) 
		{	
			Subscriber subscriber = getDataStore().getSubscriber(streamId, subscriberId);
			if (subscriber != null && StringUtils.isNotBlank(subscriber.getB32Secret())) 
			{
				byte[] decodedSubscriberSecret = Base32.decode(subscriber.getB32Secret().getBytes());

				// Use custom expiry period for this subscriber if it is set; otherwise fall back to global setting
				int period = subscriber.getTotpExpiryPeriodSeconds() != null ?
						subscriber.getTotpExpiryPeriodSeconds() : getAppSettings().getTimeTokenPeriod();

				totp = TOTPGenerator.generateTOTP(decodedSubscriberSecret, period,  6, ITokenService.HMAC_SHA1);
			}
			else 
			{	
				String secretFromSettings = getAppSettings().getTimeTokenSecretForPublish();
				if (Subscriber.PLAY_TYPE.equals(type)) 
				{
					secretFromSettings = getAppSettings().getTimeTokenSecretForPlay();
				}

				if (StringUtils.isNotBlank(secretFromSettings)) {
					//Secret code is generated by using this  secretFromSettings + subscriberId + streamId + type + "add number of X to have the length multiple of 8"
					totp = TOTPGenerator.generateTOTP(Base32.decode(TOTPGenerator.getSecretCodeForNotRecordedSubscriberId(subscriberId, streamId, type, secretFromSettings).getBytes()),
							getAppSettings().getTimeTokenPeriod(), 6, ITokenService.HMAC_SHA1);
				}
				else {
					message = "Secret is not set in AppSettings. Please set timetokensecret publish or play in Application settings";
				}

			}
			if (!StringUtils.isBlank(totp)) {
				result = true;
			}

		}
		else {
			message = "streamId or subscriberId is blank";
		}

		return new Result(result, totp, message);

	}

	@Operation(summary = "Delete specific subscriber from data store",
			description = "Deletes a specific subscriber from the data store for the selected stream.",
			responses = {
					@ApiResponse(responseCode = "200", description = "Result of deleting the subscriber",
							content = @Content(
									mediaType = "application/json",
									schema = @Schema(implementation = Result.class)
									))
	}
			)
	@DELETE
	@Consumes({ MediaType.APPLICATION_JSON })
	@Path("/{id}/subscribers/{sid}")
	@Produces(MediaType.APPLICATION_JSON)
	public Result deleteSubscriber(@Parameter(description = "the id of the stream", required = true) @PathParam("id") String streamId,
			@Parameter(description = "the id of the subscriber", required = true) @PathParam("sid") String subscriberId) {
		boolean result =  false;

		if(streamId != null) {
			result = getDataStore().deleteSubscriber(streamId, subscriberId);
		}

		return new Result(result);	
	}

	@Operation(summary = "Block specific subscriber",
			description = "Blocks a specific subscriber, enhancing security especially when used with TOTP streaming. The subscriber is blocked for a specified number of seconds from the moment this method is called.",
			responses = {
					@ApiResponse(responseCode = "200", description = "Result of blocking the subscriber",
							content = @Content(
									mediaType = "application/json",
									schema = @Schema(implementation = Result.class)
									))
	}
			)
	@PUT
	@Consumes({ MediaType.APPLICATION_JSON })
	@Path("/{id}/subscribers/{sid}/block/{seconds}/{type}")
	@Produces(MediaType.APPLICATION_JSON)
	public Result blockSubscriber(@Parameter(description = "the id of the stream", required = true) @PathParam("id") String streamId,
			@Parameter(description = "the id of the subscriber", required = true) @PathParam("sid") String subscriberId, 
			@Parameter(description = "seconds to block the user", required = true)  @PathParam("seconds") int seconds,
			@Parameter(description = "block type it can be 'publish', 'play' or 'publish_play'", required = true)  @PathParam("type") String blockType) {
		boolean result = false;
		String message = "";


        if (StringUtils.isAnyBlank(streamId, subscriberId)) {
			message = "streamId or subscriberId is blank";
			return new Result(result, message);
		}
    
        // Replace special characters in streamId and subscriberId
		streamId = streamId.replaceAll(REPLACE_CHARS, "_");
		subscriberId = subscriberId.replaceAll(REPLACE_CHARS, "_");

    
		//if the user is not in this node, it's in another node in the cluster.
		//The proxy filter will forward the request to the related node before {@link RestProxyFilter}
		result = getDataStore().blockSubscriber(streamId, subscriberId, blockType, seconds);

		message = "";
		AntMediaApplicationAdapter application = getApplication();
		if (Subscriber.PLAY_TYPE.equals(blockType) || Subscriber.PUBLISH_AND_PLAY_TYPE.equals(blockType)) {
			boolean playerStopped = application.stopPlayingBySubscriberId(subscriberId, streamId);
			if (!playerStopped) {
				logger.warn("Playback cannot be stopped for streamId:{} and subscriberId:{} likely there is no active subscriber", streamId, subscriberId);			
			}
				
		}

		if (Subscriber.PUBLISH_TYPE.equals(blockType) || Subscriber.PUBLISH_AND_PLAY_TYPE.equals(blockType)) {
			// Stops WebRTC streams
			if (!application.stopPublishingBySubscriberId(subscriberId, streamId)) {
				// WebRTC stream not stopped. Try to stop other streams
				this.stopStreaming(streamId, false, subscriberId);
			}
		}

		return new Result(result, message);
	}

	@Operation(summary = "Removes all subscribers related to the requested stream",
			description = "Deletes all subscriber data associated with the specified stream including ConnectionEvents.",
			responses = {
					@ApiResponse(responseCode = "200", description = "Result of removing all subscribers",
							content = @Content(
									mediaType = "application/json",
									schema = @Schema(implementation = Result.class)
									))
	}
			)
	@DELETE
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/{id}/subscribers")
	@Produces(MediaType.APPLICATION_JSON)
	public Result revokeSubscribers(@Parameter(description = "the id of the stream", required = true) @PathParam("id") String streamId) {
		boolean result =  false;

		if(streamId != null) {
			result = getDataStore().revokeSubscribers(streamId);
		}

		return new Result(result);
	}	

	@Operation(summary = "Get the broadcast live statistics",
			description = "Retrieves live statistics of the broadcast, including total RTMP watcher count, total HLS watcher count, and total WebRTC watcher count.",
			responses = {
					@ApiResponse(responseCode = "200", description = "Broadcast live statistics",
							content = @Content(
									mediaType = "application/json",
									schema = @Schema(implementation = BroadcastStatistics.class)
									))
	}
			)
	@GET
	@Path("/{id}/broadcast-statistics")
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public BroadcastStatistics getBroadcastStatistics(@Parameter(description = "the id of the stream", required = true) @PathParam("id") String id) {
		return super.getBroadcastStatistics(id);
	}

	@Operation(summary = "Get total broadcast live statistics",
			description = "Retrieves total live statistics of the broadcast, including total HLS watcher count and total WebRTC watcher count.",
			responses = {
					@ApiResponse(responseCode = "200", description = "Total broadcast live statistics",
							content = @Content(
									mediaType = "application/json",
									schema = @Schema(implementation = BroadcastStatistics.class)
									))
	}
			)
	@GET
	@Path("/total-broadcast-statistics")
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public AppBroadcastStatistics getBroadcastTotalStatistics() {
		return super.getBroadcastTotalStatistics();
	}

	@Operation(summary = "Get WebRTC Low Level Send Stats",
			description = "Retrieves general statistics for WebRTC low level send operations.",
			responses = {
					@ApiResponse(responseCode = "200", description = "WebRTC low level send statistics",
							content = @Content(
									mediaType = "application/json",
									schema = @Schema(implementation = WebRTCSendStats.class)
									))
	}
			)
	@GET
	@Path("/webrtc-send-low-level-stats")
	@Produces(MediaType.APPLICATION_JSON)
	public WebRTCSendStats getWebRTCLowLevelSendStats() 
	{
		return new WebRTCSendStats(getApplication().getWebRTCAudioSendStats(), getApplication().getWebRTCVideoSendStats());
	}

	@Operation(summary = "Get WebRTC Low Level Receive Stats",
			description = "Retrieves general statistics for WebRTC low level receive operations.",
			responses = {
					@ApiResponse(responseCode = "200", description = "WebRTC low level receive statistics",
							content = @Content(
									mediaType = "application/json",
									schema = @Schema(implementation = WebRTCReceiveStats.class)
									))
	}
			)
	@GET
	@Path("/webrtc-receive-low-level-stats")
	@Produces(MediaType.APPLICATION_JSON)
	public WebRTCReceiveStats getWebRTCLowLevelReceiveStats() 
	{
		return new WebRTCReceiveStats(getApplication().getWebRTCAudioReceiveStats(), getApplication().getWebRTCVideoReceiveStats());
	}


	@Operation(summary = "Get WebRTC Client Statistics",
			description = "Retrieves WebRTC client statistics, including audio bitrate, video bitrate, target bitrate, video sent period, etc.",
			responses = {
					@ApiResponse(responseCode = "200", description = "WebRTC client statistics",
							content = @Content(
									mediaType = "application/json",
									schema = @Schema(implementation = WebRTCClientStats.class, type = "array")
									))
	}
			)
	@GET
	@Path("/{stream_id}/webrtc-client-stats/{offset}/{size}")
	@Produces(MediaType.APPLICATION_JSON)
	public List<WebRTCClientStats> getWebRTCClientStatsListV2(@Parameter(description = "offset of the list", required = true) @PathParam("offset") int offset,
			@Parameter(description = "Number of items that will be fetched", required = true) @PathParam("size") int size,
			@Parameter(description = "the id of the stream", required = true) @PathParam("stream_id") String streamId) {

		return super.getWebRTCClientStatsList(offset, size, streamId);
	}

	@Operation(summary = "Set stream specific recording setting",
			description = "This setting overrides the general Mp4 and WebM Muxing Setting for a specific stream.",
			responses = {
					@ApiResponse(responseCode = "200", description = "Result of setting stream specific recording",
							content = @Content(
									mediaType = "application/json",
									schema = @Schema(implementation = Result.class)
									))
	}
			)
	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/{id}/recording/{recording-status}")
	@Produces(MediaType.APPLICATION_JSON)
	public Result enableRecording(@Parameter(description = "the id of the stream", required = true) @PathParam("id") String streamId,
			@Parameter(description = "Change recording status. If true, starts recording. If false stop recording", required = true) @PathParam("recording-status") boolean enableRecording,
			@Parameter(description = "Record type: 'mp4' or 'webm'. It's optional parameter.", required = false) @QueryParam("recordType") String recordType,
			@Parameter(description = "Resolution height of the broadcast that is wanted to record. ", required = false) @QueryParam("resolutionHeight") int resolutionHeight,
			@Parameter(description = "Optional base filename (without extension) for the output VOD.", required = false) @QueryParam("fileName") String fileName
			) {
		recordType = (recordType==null) ? RecordType.MP4.toString() : recordType; // It means, if recordType is null, function using Mp4 Record by default
		
		
		streamId = streamId.replaceAll(REPLACE_CHARS, "_");
		resolutionHeight = (resolutionHeight < 0) ? 0 : resolutionHeight; // If resolution height is not specified or it's less than or equal to 0, it will record the original resolution of the stream.
		return enableRecordMuxing(streamId, enableRecording, recordType, resolutionHeight, fileName);
	}

	// Backward-compatible overload for existing test callers
	public Result enableRecording(String streamId, boolean enableRecording, String recordType, int resolutionHeight) {
		recordType = (recordType==null) ? RecordType.MP4.toString() : recordType;
		return enableRecordMuxing(streamId, enableRecording, recordType, resolutionHeight);
	}

	@Operation(summary = "Get IP Camera Error after connection failure",
			description = "Checks for an error after a connection failure with an IP camera. Returning true indicates an error; false indicates no error.",
			responses = {
					@ApiResponse(responseCode = "200", description = "IP Camera error status",
							content = @Content(
									mediaType = "application/json",
									schema = @Schema(implementation = Result.class)
									))
	}
			)
	@GET
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/{streamId}/ip-camera-error")
	@Produces(MediaType.APPLICATION_JSON)
	public Result getCameraErrorV2(@Parameter(description = "StreamId of the IP Camera Streaming.", required = true) @PathParam("streamId") String streamId) {
		return super.getCameraErrorById(streamId);
	}

	@Operation(summary = "Start streaming sources",
			description = "Initiates streaming for sources such as IP Cameras, Stream Sources, and PlayLists.",
			responses = {
					@ApiResponse(responseCode = "200", description = "Result of starting streaming sources",
							content = @Content(
									mediaType = "application/json",
									schema = @Schema(implementation = Result.class)
									))
	}
			)
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/{id}/start")
	@Produces(MediaType.APPLICATION_JSON)
	public Result startStreamSourceV2(@Parameter(description = "the id of the stream. The broadcast type should be IP Camera or Stream Source otherwise it does not work", required = true) @PathParam("id") String id) 
	{
		return super.startStreamSource(id);
	}

	@Operation(summary = "Specify the next playlist item to play by index",
			description = "Sets the next playlist item to be played, based on its index. This method is applicable only to playlists.",
			responses = {
					@ApiResponse(responseCode = "200", description = "Result of specifying the next playlist item",
							content = @Content(
									mediaType = "application/json",
									schema = @Schema(implementation = Result.class)
									))
	}
			)
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/playlists/{id}/next")
	@Produces(MediaType.APPLICATION_JSON)
	public Result playNextItem(@Parameter(description = "The id of the playlist stream.", required = true) @PathParam("id") String id,			
			@Parameter(description = "The next item to play. If it's not specified or it's -1, it plays next item. If it's number, it skips that item in the playlist to play. The first item index is 0. ", required = false) @QueryParam("index") Integer index			
			) 
	{ 
		return super.playNextItem(id, index);
	}

	@Operation(summary = "Stop streaming for the active stream",
			description = "Terminates streaming for the active stream, including both ingested (RTMP, WebRTC) and pulled stream sources (IP Cameras and Stream Sources).",
			responses = {
					@ApiResponse(responseCode = "200", description = "Result of stopping the active stream",
							content = @Content(
									mediaType = "application/json",
									schema = @Schema(implementation = Result.class)
									))
	}
			)
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/{id}/stop")
	@Produces(MediaType.APPLICATION_JSON)
	public Result stopStreamingV2(@Parameter(description = "the id of the broadcast.", required = true) @PathParam("id") String id,
			@Parameter(description = "Stop also subtracks", required = false) @QueryParam("stopSubtracks") Boolean stopSubtracks) 
	{
		return super.stopStreaming(id, stopSubtracks, null);
	}


	@Operation(summary = "Get Discovered ONVIF IP Cameras",
			description = "Performs a discovery within the internal network to automatically retrieve information about ONVIF-enabled cameras.",
			responses = {
					@ApiResponse(responseCode = "200", description = "Result of discovering ONVIF IP cameras",
							content = @Content(
									mediaType = "application/json",
									schema = @Schema(implementation = Result.class)
									))
	}
			)
	@GET
	@Path("/onvif-devices")
	@Produces(MediaType.APPLICATION_JSON)
	public String[] searchOnvifDevicesV2() {
		return super.searchOnvifDevices();
	}

	@Operation(summary = "Get the Profile List for an ONVIF IP Camera",
			description = "Retrieves the profile list for an ONVIF IP camera.",
			responses = {
					@ApiResponse(responseCode = "200", description = "Profile list for the ONVIF IP camera",
							content = @Content(
									mediaType = "application/json",
									schema = @Schema(implementation = String[].class)
									))
	}
			)
	@GET
	@Path("/{id}/ip-camera/device-profiles")
	@Produces(MediaType.APPLICATION_JSON)
	public String[] getOnvifDeviceProfiles(@Parameter(description = "The id of the IP Camera", required = true) @PathParam("id") String id) {
		if (id != null && StreamIdValidator.isStreamIdValid(id)) {
			return super.getOnvifDeviceProfiles(id);
		}
		return null;
	}


	@Operation(summary = "Move IP Camera",
			description = "Supports continuous, relative, and absolute movement. By default, it's a relative move. Movement parameters should be provided according to the movement type. Generally, the following values are used: "
					+ "For Absolute move, value X and value Y are between -1.0f and 1.0f. Zoom value is between 0.0f and 1.0f. "
					+ "For Relative move, value X, value Y, and Zoom Value are between -1.0f and 1.0f. "
					+ "For Continuous move, value X, value Y, and Zoom Value are between -1.0f and 1.0f.",
					responses = {
							@ApiResponse(responseCode = "200", description = "Result of moving the IP camera",
									content = @Content(
											mediaType = "application/json",
											schema = @Schema(implementation = Result.class)
											))
	}
			)
	@POST
	@Path("/{id}/ip-camera/move")
	@Produces(MediaType.APPLICATION_JSON)
	public Result moveIPCamera(@Parameter(description = "The id of the IP Camera", required = true) @PathParam("id") String id,
			@Parameter(description = "Movement in X direction. If not specified, it's assumed to be zero. Valid ranges between -1.0f and 1.0f for all movements ", required = false) @QueryParam("valueX") Float valueX,
			@Parameter(description = "Movement in Y direction. If not specified, it's assumed to be zero. Valid ranges between -1.0f and 1.0f for all movements ", required = false) @QueryParam("valueY") Float valueY,
			@Parameter(description = "Movement in Zoom. If not specified, it's assumed to be zero. Valid ranges for relative and continous move is between -1.0f and 1.0f. For absolute move between 0.0f and 1.0f ", required = false) @QueryParam("valueZ") Float valueZ,
			@Parameter(description = "Movement type. It can be absolute, relative or continuous. If not specified, it's relative", required = false) @QueryParam("movement") String movement
			) {
		boolean result = false;
		String message = STREAM_ID_NOT_VALID;
		if (id != null && StreamIdValidator.isStreamIdValid(id)) {
			message = "";
			if (valueX == null) {
				valueX = 0f;
			}

			if (valueY == null) {
				valueY = 0f;
			}

			if (valueZ == null) {
				valueZ = 0f;
			}

			if (movement == null) {
				movement = RELATIVE_MOVE;
			}

			if (movement.equals(RELATIVE_MOVE)) {
				result = super.moveRelative(id, valueX, valueY, valueZ);
			}
			else if (movement.equals(CONTINUOUS_MOVE)) {
				result = super.moveContinous(id, valueX, valueY, valueZ);
			}
			else if (movement.equals(ABSOLUTE_MOVE)) {
				result = super.moveAbsolute(id, valueX, valueY, valueZ);
			}
			else  {
				message = "Movement type is not supported. Supported types are continous, relative and absolute but was " + movement;
			}		
		}
		return new Result(result, message);
	}

	@Operation(description = "Stop move for IP Camera")
	@POST
	@Path("/{id}/ip-camera/stop-move")
	@Produces(MediaType.APPLICATION_JSON)
	public Result stopMove(@Parameter(description = "the id of the IP Camera", required = true) @PathParam("id") String id) {
		boolean result = false;
		String message = STREAM_ID_NOT_VALID;
		if (id != null && StreamIdValidator.isStreamIdValid(id)) 
		{		
			OnvifCamera camera = getApplication().getOnvifCamera(id);
			if (camera != null) {
				result = camera.moveStop();
				message = "";
			}
			else {
				message = "Camera not found";
			}
		}
		return new Result(result, message);
	}






	@Operation(summary = "Add a subtrack to a main track (broadcast)",
			description = "Adds a subtrack to a main track (broadcast).",
			responses = {
					@ApiResponse(responseCode = "200", description = "Result of adding a subtrack",
							content = @Content(
									mediaType = "application/json",
									schema = @Schema(implementation = Result.class)
									))
	}
			)
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/{id}/subtrack")
	@Produces(MediaType.APPLICATION_JSON)
	public Result addSubTrack(@Parameter(description = "Broadcast id(main track)", required = true) @PathParam("id") String id,
			@Parameter(description = "Subtrack Stream Id", required = true) @QueryParam("id") String subTrackId) 
	{		
		Result result = RestServiceBase.addSubTrack(id, subTrackId, getDataStore());
		if(result.isSuccess()) {
			getApplication().joinedTheRoom(id, subTrackId);
		}
		return result;

	}

	@Operation(summary = "Delete a subtrack from a main track (broadcast)",
			description = "Deletes a subtrack from a main track (broadcast).",
			responses = {
					@ApiResponse(responseCode = "200", description = "Result of deleting a subtrack",
							content = @Content(
									mediaType = "application/json",
									schema = @Schema(implementation = Result.class)
									))
	}
			)
	@DELETE
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/{id}/subtrack")
	@Produces(MediaType.APPLICATION_JSON)
	public Result removeSubTrack(@Parameter(description = "Broadcast id(main track)", required = true) @PathParam("id") String id,
			@Parameter(description = "Subtrack Stream Id", required = true) @QueryParam("id") String subTrackId)
	{
		Result result = RestServiceBase.removeSubTrack(id, subTrackId, getDataStore());
		if(result.isSuccess()) {
			getApplication().leftTheRoom(id, subTrackId);
		}
		return result;
	}

	@Operation(summary = "Get subtracks of a broadcast",
			description = "Returns the list of subtracks associated with a main broadcast. Subtracks are alternative streams that are linked to a main broadcast.",
			responses = {
					@ApiResponse(responseCode = "200", description = "List of subtracks",
							content = @Content(
									mediaType = "application/json",
									schema = @Schema(implementation = Broadcast[].class)
									)),
					@ApiResponse(responseCode = "404", description = "Broadcast not found")
	}
			)
	@GET
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/{id}/subtracks/{offset}/{size}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getSubtracks(@Parameter(description = "Broadcast id (main track)", required = true) @PathParam("id") String id,
			@Parameter(description = "This is the offset of the list, it is useful for pagination.", required = true) @PathParam("offset") int offset,
			@Parameter(description = "Number of items that will be fetched. If there is not enough subtracks, returned list size may be less than this value", required = true) @PathParam("size") int size,
			@Parameter(description = "Role parameter for filtering subtracks", required = false) @QueryParam("role") String role) {

		// Verify that the main broadcast exists
		Broadcast mainBroadcast = getDataStore().get(id);
		if (mainBroadcast == null) {
			return Response.status(Status.NOT_FOUND).entity(new Result(false, "Broadcast not found with id: " + id)).build();
		}

		// Get the subtracks
		String roleParam = role != null ? role : "";
		List<Broadcast> subtracks = getDataStore().getSubtracks(id, offset, size, roleParam);

		return Response.status(Status.OK).entity(subtracks).build();
	}

	@Operation(summary = "Get active subtracks of a broadcast",
			description = "Returns the list of active subtracks associated with a main broadcast. Only returns subtracks that are currently broadcasting.",
			responses = {
					@ApiResponse(responseCode = "200", description = "List of active subtracks",
							content = @Content(
									mediaType = "application/json",
									schema = @Schema(implementation = Broadcast[].class)
									)),
					@ApiResponse(responseCode = "404", description = "Broadcast not found")
	}
			)
	@GET
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/{id}/active-subtracks/{offset}/{size}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getActiveSubtracks(@Parameter(description = "Broadcast id (main track)", required = true) @PathParam("id") String id,
			@Parameter(description = "This is the offset of the list, it is useful for pagination.", required = true) @PathParam("offset") int offset,
			@Parameter(description = "Number of items that will be fetched. If there is not enough active subtracks, returned list size may be less than this value", required = true) @PathParam("size") int size,
			@Parameter(description = "Role parameter for filtering active subtracks", required = false) @QueryParam("role") String role) {

		// Verify that the main broadcast exists
		Broadcast mainBroadcast = getDataStore().get(id);
		if (mainBroadcast == null) {
			return Response.status(Status.NOT_FOUND).entity(new Result(false, "Broadcast not found with id: " + id)).build();
		}

		// Get the active subtracks
		String roleParam = role != null ? role : "";
		List<Broadcast> activeSubtracks = getDataStore().getActiveSubtracks(id, offset, size, roleParam);

		return Response.status(Status.OK).entity(activeSubtracks).build();
	}

	@Operation(summary = "Get count of active subtracks of a broadcast",
			description = "Returns the count of active subtracks associated with a main broadcast. Only counts subtracks that are currently broadcasting.",
			responses = {
					@ApiResponse(responseCode = "200", description = "Count of active subtracks",
							content = @Content(
									mediaType = "application/json",
									schema = @Schema(implementation = Result.class)
									)),
					@ApiResponse(responseCode = "404", description = "Broadcast not found")
	}
			)
	@GET
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/{id}/active-subtracks-count")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getActiveSubtracksCount(@Parameter(description = "Broadcast id (main track)", required = true) @PathParam("id") String id,
			@Parameter(description = "Role parameter for filtering active subtracks", required = false) @QueryParam("role") String role) {

		// Verify that the main broadcast exists
		Broadcast mainBroadcast = getDataStore().get(id);
		if (mainBroadcast == null) {
			return Response.status(Status.NOT_FOUND).entity(new Result(false, "Broadcast not found with id: " + id)).build();
		}

		// Get the count of active subtracks
		String roleParam = role != null ? role : "";
		long count = getDataStore().getActiveSubtracksCount(id, roleParam);
		
		SimpleStat simpleStat = new SimpleStat(count);
		return Response.status(Status.OK).entity(simpleStat).build();
	}

	@Operation(summary = "Get stream information",
			description = "Returns the stream information including width, height, bitrates, and video codec.",
			responses = {
					@ApiResponse(responseCode = "200", description = "Stream information",
							content = @Content(
									mediaType = "application/json",
									schema = @Schema(implementation = BasicStreamInfo[].class)
									))
	}
			)
	@GET
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/{id}/stream-info")
	@Produces(MediaType.APPLICATION_JSON)
	public BasicStreamInfo[] getStreamInfo(@PathParam("id") String streamId) 
	{	
		boolean isCluster = getAppContext().containsBean(IClusterNotifier.BEAN_NAME);
		List<? extends IStreamInfo> streamInfoList;
		if (isCluster) {
			streamInfoList = getDataStore().getStreamInfoList(streamId);
		}
		else {
			IWebRTCAdaptor webRTCAdaptor = (IWebRTCAdaptor) getAppContext().getBean(IWebRTCAdaptor.BEAN_NAME);
			streamInfoList = webRTCAdaptor.getStreamInfo(streamId);
		}
		BasicStreamInfo[] basicStreamInfo = new BasicStreamInfo[0];
		if (streamInfoList != null) 
		{
			basicStreamInfo = new BasicStreamInfo[streamInfoList.size()];
			for (int i = 0; i < basicStreamInfo.length; i++) {
				IStreamInfo iStreamInfo = streamInfoList.get(i);
				basicStreamInfo[i] = new BasicStreamInfo(iStreamInfo.getVideoHeight(), iStreamInfo.getVideoWidth(), 
						iStreamInfo.getVideoBitrate(), iStreamInfo.getAudioBitrate(), iStreamInfo.getVideoCodec());
			}
		}

		return basicStreamInfo;
	}

	@Operation(summary = "Send message to stream participants via Data Channel",
			description = "Sends a message to stream participants through the Data Channel in a WebRTC stream.",
			responses = {
					@ApiResponse(responseCode = "200", description = "Result of sending the message",
							content = @Content(
									mediaType = "application/json",
									schema = @Schema(implementation = Result.class)
									))
	}
			)
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/{id}/data")
	@Produces(MediaType.APPLICATION_JSON)
	public Result sendMessage(@Parameter(description = "Message through Data Channel which will be sent to all WebRTC stream participants", required = true) String message, 
			@Parameter(description = "Broadcast id", required = true) @PathParam("id") String id) {


		return RestServiceBase.sendDataChannelMessage(id, message, getApplication(), getDataStore());
	}
	

	@Operation(description = "Add ID3 data to HLS stream at the moment")
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/{stream_id}/id3")
	@Produces(MediaType.APPLICATION_JSON)
	public Result addID3Data(@Parameter(description = "the id of the stream", required = true) @PathParam("stream_id") String streamId,
			@Parameter(description = "ID3 data.", required = false) String data) 
	{
		if(!getAppSettings().isId3TagEnabled()) {
			return new Result(false, null, "ID3 tag is not enabled");
		}
		logger.info("ID3 data is received for stream: {} data: {}", streamId.replaceAll(REPLACE_CHARS, "_"), data.replaceAll(REPLACE_CHARS, "_"));

		MuxAdaptor muxAdaptor = getMuxAdaptor(streamId);
		if(muxAdaptor != null) {
			return new Result(muxAdaptor.addID3Data(data));
		}
		else {
			return new Result(false, null, "Stream is not available");
		}
	}

	@Operation(description = "Add SEI data to HLS stream at the moment")
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/{stream_id}/sei")
	@Produces(MediaType.APPLICATION_JSON)
	public Result addSEIData(@Parameter(description = "the id of the stream", required = true) @PathParam("stream_id") String streamId,
			@Parameter(description = "SEI data.", required = false) String data) {

		MuxAdaptor muxAdaptor = getMuxAdaptor(streamId);
		if(muxAdaptor != null) {
			return new Result(muxAdaptor.addSEIData(data));
		}
		else {
			return new Result(false, null, "Stream is not available");
		}
	}

	//TODO: add start and end time support
	@Operation(description = "Converts the recorded HLS to MP4 file")
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/{hls_filename}/hls-to-mp4")
	@Produces(MediaType.APPLICATION_JSON)
	public Response convertHLStoMP4(@PathParam("hls_filename") String hlsFileName, 
			@QueryParam("download") boolean download,
			@QueryParam("deleteHLSFiles") boolean deleteHLSFiles) 
	{
		//check if the storageClient is enabled to record the stream to S3
		//if it is recording to s3 and hlsHttpEndpoint is active
		//Convert the hls to mp4 file by using hlsHttpEndpoint


		boolean result = false;
		if (StringUtils.isBlank(hlsFileName)) {
			return Response.status(Status.OK).entity(new Result(false, "HLS file name is empty")).build();
		}

		//check if m3u8 extension is given, if not add it
		String fileNameWithoutExtension = null;
		hlsFileName = hlsFileName.replaceAll(REPLACE_CHARS, "_");
		if (!hlsFileName.endsWith(".m3u8")) 
		{
			hlsFileName += ".m3u8";
		}
		


		String streamId = TokenFilterManager.getStreamId(hlsFileName);

		Broadcast broadcast = lookupBroadcast(streamId);
		int height = 0;
		long startTime = 0;
		String subFolder = null;
		if (broadcast != null) {
			subFolder = broadcast.getSubFolder();
			if (StringUtils.isNotBlank(subFolder)) {
				hlsFileName = subFolder + File.separator + hlsFileName;
			}
			startTime = broadcast.getStartTime();
			broadcast.getHeight();
		} 
		else {
			logger.warn("Broadcast not found for stream id: {}", streamId);
		}

		fileNameWithoutExtension = hlsFileName.substring(0, hlsFileName.length() - 5); // remove .m3u8

		File hlsFile = new File(IAntMediaStreamHandler.WEBAPPS_PATH + File.separator + getScope().getName() 
				+ File.separator + "streams" + File.separator + hlsFileName);
		File parentFolder = hlsFile.getParentFile();
		parentFolder.mkdirs();

		String outputPath = parentFolder.getAbsolutePath() + File.separator + fileNameWithoutExtension + ".mp4";

		File outputFile = new File(outputPath);
		AppSettings appSettings = getAppSettings();
		String hlsHttpEndpoint = appSettings.getHlsHttpEndpoint();

		String inputUrl = null;
		if (StringUtils.isNotBlank(hlsHttpEndpoint)) 
		{						
			// check if the stream exists
			inputUrl = hlsHttpEndpoint + File.separator + hlsFileName;
		}
		else {
			inputUrl = hlsFile.getAbsolutePath();
		}

		result = HLSMuxer.convertToMp4(inputUrl, outputPath);

		if (!result) {
			return Response.status(Status.OK).entity(new Result(false, "HLS to MP4 conversion has failed")).build(); 
		}

		long durationInMs = Muxer.getDurationInMs(outputFile, streamId);
		getApplication().muxingFinished(broadcast, streamId, outputFile, startTime, durationInMs, height, null, null);

		StorageClient storageClient = getApplication().getStorageClient();

		if (storageClient.isEnabled() && (RecordMuxer.S3_CONSTANT & getAppSettings().getUploadExtensionsToS3()) != 0)
		{

			uploadToS3(deleteHLSFiles, fileNameWithoutExtension, outputPath, appSettings, storageClient);

			result = true;
		}
		else if (deleteHLSFiles) {
			deleteLocalHLSFiles(hlsFile);
		}

		if (download) {
			//return the file as a download

			Response.ResponseBuilder response = Response.ok(outputFile);
			response.header("Content-Disposition", "attachment; filename=\"" + fileNameWithoutExtension + ".mp4\"");

			return response.build();
		}



		return Response.status(Status.OK).entity(new Result(result)).build();
	}

	public void deleteLocalHLSFiles(File hlsFile) {
		File[] hlsFiles = HLSMuxer.getHLSFilesInDirectory(hlsFile, HLSMuxer.HLS_FILES_REGEX_MATCHER);

		if (hlsFiles != null && hlsFiles.length > 0) {
			for (File hlsFileToDelete : hlsFiles) {
				if (!hlsFileToDelete.delete()) {
					logger.warn("Failed to delete HLS file: {}", hlsFileToDelete.getAbsolutePath());
				}
			}
		} else {
			logger.warn("No HLS files found in directory: {}", hlsFile.getAbsolutePath());
		}
	}

	public void uploadToS3(boolean deleteHLSFiles, String fileNameWithoutExtension, String outputPath,
			AppSettings appSettings, StorageClient storageClient) {
		String key = Muxer.replaceDoubleSlashesWithSingleSlash(appSettings.getS3StreamsFolderPath() + File.separator + fileNameWithoutExtension + ".mp4");

		logger.info("Saving converted MP4 file to S3 bucket with key: {}", key);
		storageClient.save(key, new File(outputPath), true, new ProgressListener() {

			@Override
			public void progressChanged(ProgressEvent progressEvent) 
			{
				if (progressEvent.getEventType() == ProgressEventType.TRANSFER_COMPLETED_EVENT) {
					// The transfer is completed
					logger.info("MP4 file upload completed to S3 bucket with key: {}", key);
					if (deleteHLSFiles) 
					{
						logger.info("Deleting HLS files from S3 bucket for stream: {}", fileNameWithoutExtension);
						String key = RecordMuxer.replaceDoubleSlashesWithSingleSlash(appSettings.getS3StreamsFolderPath() + File.separator + fileNameWithoutExtension);
						storageClient.deleteMultipleFiles(key, HLSMuxer.HLS_FILES_REGEX_MATCHER);
					}
				}
				else if (progressEvent.getEventType() == ProgressEventType.TRANSFER_FAILED_EVENT) {
					logger.error("MP4 file upload failed to S3 bucket with key: {}", key);
				}
			}
		});
	}
}
