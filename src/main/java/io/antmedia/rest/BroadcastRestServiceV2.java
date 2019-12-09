package io.antmedia.rest;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.springframework.stereotype.Component;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.StreamIdValidator;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.datastore.db.types.ConferenceRoom;
import io.antmedia.datastore.db.types.SocialEndpointChannel;
import io.antmedia.datastore.db.types.SocialEndpointCredentials;
import io.antmedia.datastore.db.types.TensorFlowObject;
import io.antmedia.datastore.db.types.Token;
import io.antmedia.ipcamera.OnvifCamera;
import io.antmedia.rest.BroadcastRestService.BroadcastStatistics;
import io.antmedia.rest.model.Interaction;
import io.antmedia.rest.model.Result;
import io.antmedia.social.LiveComment;
import io.antmedia.statistic.type.WebRTCAudioReceiveStats;
import io.antmedia.statistic.type.WebRTCAudioSendStats;
import io.antmedia.statistic.type.WebRTCVideoReceiveStats;
import io.antmedia.statistic.type.WebRTCVideoSendStats;
import io.antmedia.webrtc.api.IWebRTCAdaptor;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Contact;
import io.swagger.annotations.ExternalDocs;
import io.swagger.annotations.Info;
import io.swagger.annotations.License;
import io.swagger.annotations.SwaggerDefinition;

@Api(value = "BroadcastRestService")
@SwaggerDefinition(
		info = @Info(
				description = "Ant Media Server REST API Reference",
				version = "v2.0",
				title = "Ant Media Server REST API Reference",
				contact = @Contact(name = "Ant Media Info", email = "contact@antmedia.io", url = "https://antmedia.io"),
				license = @License(name = "Apache 2.0", url = "http://www.apache.org")),
		consumes = {"application/json"},
		produces = {"application/json"},
		schemes = {SwaggerDefinition.Scheme.HTTP, SwaggerDefinition.Scheme.HTTPS},
		externalDocs = @ExternalDocs(value = "External Docs", url = "https://antmedia.io"),
		basePath = "/v2"
		)
@Component
@Path("/v2/broadcasts")
public class BroadcastRestServiceV2 extends RestServiceBase{

	
	private static final String VALUE_IS_LESS_THAN_ZERO = "Value is less than zero";
	private static final String STREAM_ID_NOT_VALID = "Stream id not valid";
	private static final String RELATIVE_MOVE = "relative";
	private static final String ABSOLUTE_MOVE = "absolute";
	private static final String CONTINUOUS_MOVE = "continuous";

	@ApiModel(value="SimpleStat", description="Simple generic statistics class to return single values")
	public static class SimpleStat {
		@ApiModelProperty(value = "the stat value")
		public long number;

		public SimpleStat(long number) {
			this.number = number;
		}

		public long getNumber() {
			return number;
		}
	}
	
	@ApiModel(value="WebRTCSendStats", description="Aggregation of WebRTC Low Level Send Stats")
	public static class WebRTCSendStats
	{
		@ApiModelProperty(value = "Audio send stats")
		private final WebRTCAudioSendStats audioSendStats;
		
		@ApiModelProperty(value = "Video send stats")
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
	
	@ApiModel(value="WebRTCReceiveStats", description="Aggregation of WebRTC Low Level Receive Stats")
	public static class WebRTCReceiveStats
	{
		@ApiModelProperty(value = "Audio receive stats")
		private final WebRTCAudioReceiveStats audioReceiveStats;
		
		@ApiModelProperty(value = "Video receive stats")
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
	

	@ApiOperation(value = "Creates a Broadcast, IP Camera or Stream Source and returns the full broadcast object with rtmp address and "
			+ "other information. The different between Broadcast and IP Camera or Stream Source is that Broadcast is ingested by Ant Media Server"
			+ "IP Camera or Stream Source is pulled by Ant Media Server")
	@ApiResponses(value = { @ApiResponse(code = 400, message = "If stream id is already used in the data store, it returns error", response=Result.class),
			@ApiResponse(code = 200, message = "Returns the created stream", response = Broadcast.class)})
	@POST
	@Consumes({ MediaType.APPLICATION_JSON })
	@Path("/create")
	@Produces(MediaType.APPLICATION_JSON)
	public Response createBroadcast(@ApiParam(value = "Broadcast object only related information should be set, it may be null as well.", required = false) Broadcast broadcast,
			@ApiParam(value = "Comma separated social network IDs, they must in comma separated and IDs must match with the defined IDs.", required = false) @QueryParam("socialNetworks") String socialEndpointIds,
			@ApiParam(value = "Only effective if stream is IP Camera or Stream Source. If it's true, it starts automatically pulling stream. Default value is false by default", required = false, defaultValue="false") @QueryParam("autoStart") boolean autoStart) {


		if (broadcast != null && broadcast.getStreamId() != null && !broadcast.getStreamId().isEmpty()) {
			// make sure stream id is not set on rest service
			Broadcast broadcastTmp = getDataStore().get(broadcast.getStreamId());
			if (broadcastTmp != null) 
			{
				return Response.status(Status.BAD_REQUEST).entity(new Result(false, "Stream id is already being used. ")).build();
			}
			else if (!StreamIdValidator.isStreamIdValid(broadcast.getStreamId())) 
			{
				return Response.status(Status.BAD_REQUEST).entity(new Result(false, "Stream id is not valid. ")).build();
			}

		}

		Object returnObject = new Result(false, "unexpected parameters received");

		if (autoStart)  
		{
			//auto is only effective for IP Camera or Stream Source 
			//so if it's true, it should be IP Camera or Stream Soruce
			//otherwise wrong parameter
			if (broadcast != null) {
				returnObject = addStreamSource(broadcast, socialEndpointIds);
			}
		}
		else {
			Broadcast createdBroadcast = createBroadcastWithStreamID(broadcast);

			if (createdBroadcast.getStreamId() != null && socialEndpointIds != null) {
				String[] endpointIds = socialEndpointIds.split(",");
				for (String endpointId : endpointIds) {
					addSocialEndpoint(createdBroadcast.getStreamId(), endpointId);
				}
			}
			returnObject = createdBroadcast;

		}

		return Response.status(Status.OK).entity(returnObject).build();
	}

	@ApiOperation(value = "Delete broadcast from data store and stop if it's broadcasting", response = Result.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "If it's deleted, success is true. If it's not deleted, success if false.") })
	@DELETE
	@Consumes({ MediaType.APPLICATION_JSON })
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public Result deleteBroadcast(@ApiParam(value = " Id of the braodcast", required = true) @PathParam("id") String id) {
		return super.deleteBroadcast(id);		
	}


	@ApiOperation(value = "Get broadcast object")
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Return the broadcast object"),
			@ApiResponse(code = 404, message = "Broadcast object not found")})
	@GET
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getBroadcast(@ApiParam(value = "id of the broadcast", required = true) @PathParam("id") String id) {
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

	@ApiOperation(value = "Gets the broadcast list from database", notes = "",responseContainer = "List", response = Broadcast.class)
	@GET
	@Path("/list/{offset}/{size}")
	@Produces(MediaType.APPLICATION_JSON)
	public List<Broadcast> getBroadcastList(@ApiParam(value = "This is the offset of the list, it is useful for pagination", required = true) @PathParam("offset") int offset,
			@ApiParam(value = "Number of items that will be fetched. If there is not enough item in the datastore, returned list size may less then this value", required = true) @PathParam("size") int size) {
		return getDataStore().getBroadcastList(offset, size);
	}


	@ApiOperation(value = "Updates the Broadcast objects fields if it's not null." + 
			" The updated fields are as follows: name, description, userName, password, IP address, streamUrl of the broadcast. " + 
			"It also updates the social endpoints", notes = "", response = Result.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "If it's updated, success field is true. If it's not updated, success  field if false.")})
	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public Result updateBroadcast(@ApiParam(value="Broadcast id", required = true) @PathParam("id") String id, 
			@ApiParam(value="Broadcast object with the updates") Broadcast broadcast,
			@ApiParam(value = "Comma separated social network IDs, they must in comma separated and IDs must match with the defined IDs", required = true) @QueryParam("socialNetworks") String socialNetworksToPublish) {
		Result result = new Result(false);
		if (id != null && broadcast != null) 
		{
			if (broadcast.getType() != null && 
					(broadcast.getType().equals(AntMediaApplicationAdapter.IP_CAMERA) || 
							broadcast.getType().equals(AntMediaApplicationAdapter.STREAM_SOURCE))) 
			{
				result = super.updateStreamSource(id, broadcast, socialNetworksToPublish);
			}
			else 
			{
				result = super.updateBroadcast(id, broadcast, socialNetworksToPublish);
			}

		}
		return result;
	}

	@ApiOperation(value = "Revoke authorization from a social network account that is authorized before", notes = "", response = Result.class)
	@DELETE
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/social-networks/{endpointId}")
	@Produces(MediaType.APPLICATION_JSON)
	public Result revokeSocialNetworkV2(@ApiParam(value = "Endpoint id", required = true) @PathParam("endpointId") String endpointId) {
		return super.revokeSocialNetwork(endpointId);
	}

	@ApiOperation(value = "Add social endpoint to a stream for the specified service id. ", notes = "", response = Result.class)
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/{id}/social-endpoints/{endpointServiceId}")
	@Produces(MediaType.APPLICATION_JSON)
	public Result addSocialEndpointJSONV2(@ApiParam(value = "Stream id", required = true) @PathParam("id") String id,
			@ApiParam(value = "the id of the service in order to have successfull operation. Social network must be authorized in advance", required = true) @PathParam("endpointServiceId") String endpointServiceId) {
		return addSocialEndpoint(id, endpointServiceId);
	}

	@ApiOperation(value = "Add a third pary rtmp end point to the stream. It supports adding after broadcast is started ", notes = "", response = Result.class)
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/{id}/endpoint")
	@Produces(MediaType.APPLICATION_JSON)
	public Result addEndpointV2(@ApiParam(value = "Broadcast id", required = true) @PathParam("id") String id,
			@ApiParam(value = "RTMP url of the endpoint that stream will be republished. If required, please encode the URL", required = true) @QueryParam("rtmpUrl") String rtmpUrl) {
		
		Result result = super.addEndpoint(id, rtmpUrl);
		if (result.isSuccess()) 
		{
			String status = getDataStore().get(id).getStatus();
			if (status.equals(AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING)) 
			{
				boolean started = getMuxAdaptor(id).startRtmpStreaming(rtmpUrl);
				result.setSuccess(started);
			}
		}
		else {
			if (logger.isErrorEnabled()) {
				logger.error("Rtmp endpoint({}) was not added to the stream: {}", rtmpUrl != null ? rtmpUrl.replaceAll("[\n|\r|\t]", "_") : null , id.replaceAll("[\n|\r|\t]", "_"));
			}
		}
		
		return result;
	}
	
	@ApiOperation(value = "Remove third pary rtmp end point from the stream. For the stream that is broadcasting, it will stop immediately", notes = "", response = Result.class)
	@DELETE
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/{id}/endpoint")
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public Result removeEndpoint(@ApiParam(value = "Broadcast id", required = true) @PathParam("id") String id, 
			@ApiParam(value = "RTMP url of the endpoint that will be stopped.", required = true) @QueryParam("rtmpUrl") String rtmpUrl ) {
		Result result = super.removeEndpoint(id, rtmpUrl);
		if (result.isSuccess()) 
		{
			String status = getDataStore().get(id).getStatus();
			if (status.equals(AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING)) 
			{
				boolean started = getMuxAdaptor(id).stopRtmpStreaming(rtmpUrl);
				result.setSuccess(started);
			}
		}
		else {	
		
			if (logger.isErrorEnabled()) {
				logger.error("Rtmp endpoint({}) was not removed from the stream: {}", rtmpUrl != null ? rtmpUrl.replaceAll("[\n|\r|\t]", "_") : null , id.replaceAll("[\n|\r|\t]", "_"));
			}
		}
		
		return result;
	}

	@ApiOperation(value = "Returns live comments from a specific endpoint like Facebook, Youtube, PSCP, etc. It works If interactivity is collected which can be enabled/disabled by properties file.", notes = "Notes here", responseContainer = "List", response = LiveComment.class)
	@GET
	@Path("/{id}/social-endpoints/{endpointServiceId}/live-comments/{offset}/{batch}")
	@Produces(MediaType.APPLICATION_JSON)
	public List<LiveComment> getLiveCommentsFromEndpointV2(@ApiParam(value = "This is the id of the endpoint service", required = true)
	@PathParam("endpointServiceId") String endpointServiceId,
	@ApiParam(value = "Broadcast id", required = true) @PathParam("id") String streamId,
	@ApiParam(value = "this is the start offset where to start getting comment", required = true) @PathParam("offset") int offset,
	@ApiParam(value = "number of items to be returned", required = true) @PathParam("batch") int batch) 
	{

		return super.getLiveCommentsFromEndpoint(endpointServiceId, streamId, offset, batch);
	}


	@ApiOperation(value = "Return the number of live views in specified video service endpoint. It works If interactivity is collected which can be enabled/disabled by properties file.", notes = "", response = Result.class)
	@GET
	@Path("/{id}/social-endpoints/{endpointServiceId}/live-views-count")
	@Produces(MediaType.APPLICATION_JSON)
	public Result getViewerCountFromEndpointV2(@ApiParam(value = "the id of the endpoint", required = true) @PathParam("endpointServiceId") String endpointServiceId,
			@ApiParam(value = "the id of the stream", required = true) @PathParam("id") String streamId) 
	{
		return super.getViewerCountFromEndpoint(endpointServiceId, streamId);
	}


	@ApiOperation(value = "Returns the number of live comment count from a specific video service endpoint. It works If interactivity is collected which can be enabled/disabled by properties file.", notes = "", response = Result.class)
	@GET
	@Path("/{id}/social-endpoints/{endpointServiceId}/live-comments-count")
	@Produces(MediaType.APPLICATION_JSON)
	public Result getLiveCommentsCountV2(@ApiParam(value = " the id of the endpoint", required = true) @PathParam("endpointServiceId") String endpointServiceId,
			@ApiParam(value = "the id of the stream", required = true)  @PathParam("id") String streamId) {
		return super.getLiveCommentsCount(endpointServiceId, streamId);
	}


	@ApiOperation(value = "Return the interaction from a specific endpoint like Facebook, Youtube, PSCP, etc. It works If interactivity is collected which can be enabled/disabled by properties file.", notes = "", response = Interaction.class)
	@GET
	@Path("/{id}/social-endpoints/{endpointServiceId}/interaction")
	@Produces(MediaType.APPLICATION_JSON)
	public Interaction getInteractionFromEndpointV2(@ApiParam(value = "the id of the endpoint", required = true) @PathParam("endpointServiceId") String endpointServiceId,
			@ApiParam(value = "the id of the stream", required = true) @PathParam("id") String streamId) {
		return super.getInteractionFromEndpoint(endpointServiceId, streamId);
	}



	@ApiOperation(value = "Get detected objects from the stream based on offset and size", notes = "",responseContainer = "List", response = TensorFlowObject.class)
	@GET
	@Path("/{id}/detections/{offset}/{size}")
	@Produces(MediaType.APPLICATION_JSON)
	public List<TensorFlowObject> getDetectionListV2(@ApiParam(value = "the id of the stream", required = true) @PathParam("id") String id,
			@ApiParam(value = "starting point of the list", required = true) @PathParam("offset") int offset,
			@ApiParam(value = "total size of the return list", required = true) @PathParam("size") int size) {
		return super.getDetectionList(id, offset, size);
	}

	@ApiOperation(value = "Get total number of detected objects", notes = "", response = Long.class)
	@GET
	@Path("/{id}/detections/count")
	@Produces(MediaType.APPLICATION_JSON)
	public SimpleStat getObjectDetectedTotal(@ApiParam(value = "id of the stream", required = true) @PathParam("id") String id){
		return new SimpleStat(getDataStore().getObjectDetectedTotal(id));
	}

	@ApiOperation(value = "Import Live Streams to Stalker Portal", notes = "", response = Result.class)
	@POST
	@Path("/import-to-stalker")
	@Produces(MediaType.APPLICATION_JSON)
	public Result importLiveStreams2StalkerV2() 
	{
		return super.importLiveStreams2Stalker();
	}


	@ApiOperation(value = "Get the total number of broadcasts", notes = "", response = SimpleStat.class)
	@GET
	@Path("/count")
	@Produces(MediaType.APPLICATION_JSON)
	public SimpleStat getTotalBroadcastNumberV2() {
		return new SimpleStat(getDataStore().getTotalBroadcastNumber());
	}

	@ApiOperation(value = "Return the active live streams", notes = "", response = SimpleStat.class)
	@GET
	@Path("/active-live-stream-count")
	@Produces(MediaType.APPLICATION_JSON)
	public SimpleStat getAppLiveStatistics() {
		return new SimpleStat(getDataStore().getActiveBroadcastCount());
	}


	@ApiOperation(value = "Generates random one-time token for specified stream")
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Returns token", response=Token.class), 
			@ApiResponse(code = 400, message = "When there is an error in creating token", response=Result.class)})
	@GET
	@Path("/{id}/token")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getTokenV2 (@ApiParam(value = "The id of the stream", required = true) @PathParam("id")String streamId,
			@ApiParam(value = "The expire time of the token. It's in unix timestamp seconds", required = true) @QueryParam("expireDate") long expireDate,
			@ApiParam(value = "Type of the token. It may be play or publish ", required = true) @QueryParam("type") String type,
			@ApiParam(value = "Room Id that token belongs to. It's not mandatory ", required = false) @QueryParam("roomId") String roomId) 
	{
		Object result = super.getToken(streamId, expireDate, type, roomId);
		if (result instanceof Token) {
			return Response.status(Status.OK).entity(result).build();
		}
		else {
			return Response.status(Status.BAD_REQUEST).entity(result).build();
		}
	}

	@ApiOperation(value = "Perform validation of token for requested stream. If validated, success field is true, "
			+ "not validated success field false", response = Result.class)
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/validate-token")
	@Produces(MediaType.APPLICATION_JSON)
	public Result validateTokenV2(@ApiParam(value = "Token to be validated", required = true) Token token) 
	{
		boolean result =  false;
		Token validateToken = super.validateToken(token);
		if (validateToken != null) {
			result = true;
		}

		return new Result(result);
	}


	@ApiOperation(value = " Removes all tokens related with requested stream", notes = "", response = Result.class)
	@DELETE
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/{id}/tokens")
	@Produces(MediaType.APPLICATION_JSON)
	public Result revokeTokensV2(@ApiParam(value = "the id of the stream", required = true) @PathParam("id") String streamId) {
		return super.revokeTokens(streamId);
	}


	@ApiOperation(value = "Get the all tokens of requested stream", notes = "",responseContainer = "List", response = Token.class)
	@GET
	@Path("/{id}/tokens/list/{offset}/{size}")
	@Produces(MediaType.APPLICATION_JSON)
	public List<Token> listTokensV2(@ApiParam(value = "the id of the stream", required = true) @PathParam("id") String streamId,
			@ApiParam(value = "the starting point of the list", required = true) @PathParam("offset") int offset,
			@ApiParam(value = "size of the return list (max:50 )", required = true) @PathParam("size") int size) {
		List<Token> tokens = null;
		if(streamId != null) {
			tokens = getDataStore().listAllTokens(streamId, offset, size);
		}
		return tokens;
	}

	@ApiOperation(value = "Get the broadcast live statistics total RTMP watcher count, total HLS watcher count, total WebRTC watcher count", notes = "", response = BroadcastStatistics.class)
	@GET
	@Path("/{id}/broadcast-statistics")
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public BroadcastStatistics getBroadcastStatistics(@ApiParam(value = "the id of the stream", required = true) @PathParam("id") String id) {
		return super.getBroadcastStatistics(id);
	}

	@ApiOperation(value = "Get WebRTC Low Level Send stats in general", notes = "",response = WebRTCSendStats.class)
	@GET
	@Path("/webrtc-send-low-level-stats")
	@Produces(MediaType.APPLICATION_JSON)
	public WebRTCSendStats getWebRTCLowLevelSendStats() 
	{
		return new WebRTCSendStats(getApplication().getWebRTCAudioSendStats(), getApplication().getWebRTCVideoSendStats());
	}
	
	@ApiOperation(value = "Get WebRTC Low Level receive stats in general", notes = "",response = WebRTCSendStats.class)
	@GET
	@Path("/webrtc-receive-low-level-stats")
	@Produces(MediaType.APPLICATION_JSON)
	public WebRTCReceiveStats getWebRTCLowLevelReceiveStats() 
	{
		return new WebRTCReceiveStats(getApplication().getWebRTCAudioReceiveStats(), getApplication().getWebRTCVideoReceiveStats());
	}
	
	
	@ApiOperation(value = "Get WebRTC Client Statistics such as : Audio bitrate, Video bitrate, Target bitrate, Video Sent Period etc.", notes = "", responseContainer = "List",response = WebRTCClientStats.class)
	@GET
	@Path("/{stream_id}/webrtc-client-stats/{offset}/{size}")
	@Produces(MediaType.APPLICATION_JSON)
	public List<WebRTCClientStats> getWebRTCClientStatsListV2(@ApiParam(value = "offset of the list", required = true) @PathParam("offset") int offset,
			@ApiParam(value = "Number of items that will be fetched", required = true) @PathParam("size") int size,
			@ApiParam(value = "the id of the stream", required = true) @PathParam("stream_id") String streamId) {

		return super.getWebRTCClientStatsList(offset, size, streamId);
	}

	@ApiOperation(value = "Returns filtered broadcast list according to type. It's useful for getting IP Camera and Stream Sources from the whole list", notes = "",responseContainer = "List",response = Broadcast.class)
	@GET
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/filter-list/{offset}/{size}/{type}")
	@Produces(MediaType.APPLICATION_JSON)
	public List<Broadcast> filterBroadcastListV2(@ApiParam(value = "starting point of the list", required = true) @PathParam("offset") int offset,
			@ApiParam(value = "size of the return list (max:50 )", required = true) @PathParam("size") int size,
			@ApiParam(value = "type of the stream. Possible values are \"liveStream\", \"ipCamera\", \"streamSource\", \"VoD\"", required = true) @PathParam("type") String type) {
		return getDataStore().filterBroadcastList(offset, size, type);
	}


	@ApiOperation(value = "Get device parameters for social network authorization.", notes = "", response = Object.class)
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/social-networks/{serviceName}")
	@Produces(MediaType.APPLICATION_JSON)
	public Object getDeviceAuthParametersV2(@ApiParam(value = "Name of the service, like Facebook, Youtube, Periscope", required = true) @PathParam("serviceName") String serviceName) {
		return super.getDeviceAuthParameters(serviceName);
	}

	@ApiOperation(value = "Check if device is authenticated in the social network. In authorization phase, " +
			"this function may be polled periodically until it returns success." +
			"Server checks social network service for about 1 minute so that if user" +
			"does not enter DeviceAuthParameters in a 1 minute, this function will" +
			"never return true", notes = "", response = Result.class)
	@GET
	@Consumes({ MediaType.APPLICATION_JSON })
	@Path("/social-network-status/{userCode}")
	@Produces(MediaType.APPLICATION_JSON)
	public Result checkDeviceAuthStatusV2(@ApiParam(value = "Code of social media account", required = true) @PathParam("userCode") String userCode) {
		return super.checkDeviceAuthStatus(userCode);
	}

	@ApiOperation(value = "Get Credentials of Social Endpoints", notes = "", responseContainer = "List",response = SocialEndpointCredentials.class)
	@GET
	@Consumes({ MediaType.APPLICATION_JSON })
	@Path("/social-endpoints/{offset}/{size}")
	@Produces(MediaType.APPLICATION_JSON)
	public List<SocialEndpointCredentials> getSocialEndpointsV2(@ApiParam(value = "the starting point of the list", required = true) @PathParam("offset") int offset,
			@ApiParam(value = "size of the return list (max:50 )", required = true) @PathParam("size") int size) {
		return super.getSocialEndpoints(offset, size);
	}

	@ApiOperation(value = "Some social networks have different channels especially for facebook," +
			"Live stream can be published on Facebook Page or Personal account, this" +
			"service returns the related information about that.", notes = "", response = SocialEndpointChannel.class)
	@GET
	@Consumes({ MediaType.APPLICATION_JSON })
	@Path("/social-networks-channel/{endpointId}")
	@Produces(MediaType.APPLICATION_JSON)
	public SocialEndpointChannel getSocialNetworkChannelV2(@ApiParam(value = "endpointId", required = true) @PathParam("endpointId") String endpointId) {
		return super.getSocialNetworkChannel(endpointId);
	}


	@ApiOperation(value = "Returns available social network channels for the specific service", notes = "",responseContainer = "List",response = SocialEndpointChannel.class)
	@GET
	@Consumes({ MediaType.APPLICATION_JSON })
	@Path("/social-networks-channel-lists/{endpointId}/{type}")
	@Produces(MediaType.APPLICATION_JSON)
	public List<SocialEndpointChannel> getSocialNetworkChannelListV2(@ApiParam(value = "endpointId", required = true) @PathParam("endpointId") String endpointId,
			@ApiParam(value = "This is very service specific, it may be page for Facebook", required = true) @PathParam("type") String type) {
		return super.getSocialNetworkChannelList(endpointId, type);
	}


	@ApiOperation(value = "If there are multiple channels in a social network," +
			"this method sets specific channel for that endpoint" +
			"If a user has pages in Facebook, this method sets the specific page to publish live stream to", notes = "", response = Result.class)
	@PUT
	@Consumes({ MediaType.APPLICATION_JSON })
	@Path("/social-networks-channels/{endpointId}/{type}/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Result setSocialNetworkChannelListV2(@ApiParam(value = "endpointId", required = true) @PathParam("endpointId") String endpointId,
			@ApiParam(value = "type", required = true) @PathParam("type") String type,
			@ApiParam(value = "id", required = true) @PathParam("id") String channelId) {
		return super.setSocialNetworkChannelList(endpointId, type, channelId);
	}
	
	
	@ApiOperation(value = "Set stream specific recording setting, this setting overrides general Mp4 Muxing Setting", notes = "", response = Result.class)
	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/{id}/recording/{recording-status}")
	@Produces(MediaType.APPLICATION_JSON)
	public Result enableMp4Muxing(@ApiParam(value = "the id of the stream", required = true) @PathParam("id") String streamId,
			@ApiParam(value = "Change recording status. If true, starts recording. If false stop recording", required = true) @PathParam("recording-status") boolean enableRecording) {
		boolean result = false;
		String message = null;
		if (streamId != null) 
		{
			Broadcast broadcast = getDataStore().get(streamId);
			if (broadcast != null) 
			{
				if (enableRecording) 
				{
					
					if (broadcast.getMp4Enabled() != MP4_ENABLE) 
					{
						result = getDataStore().setMp4Muxing(streamId, MP4_ENABLE);
						//if it's not enabled, start it
						if (broadcast.getStatus().equals(AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING))
						{
							result = startMp4Muxing(streamId);
							if (!result) 
							{
								streamId = streamId.replaceAll("[\n|\r|\t]", "_");
								logger.warn("Mp4 recording could not be started for stream: {}", streamId);
							}
						}	
					}
					else 
					{
						if (broadcast.getStatus().equals(AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING)) 
						{
							message = "Recording is already active. Please stop it first";
						}
					}
				}
				else 
				{
					boolean stopAttempted = false;
					if (broadcast.getMp4Enabled() == MP4_ENABLE && broadcast.getStatus().equals(AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING)) 
					{
						stopAttempted = true;
						//we can stop recording
						result = stopMp4Muxing(streamId);
						if (!result) 
						{
							streamId = streamId.replaceAll("[\n|\r|\t]", "_");
							logger.warn("Mp4 recording could not be stopped for stream: {}", streamId);
						}
						
					}
					boolean dataStoreResult = getDataStore().setMp4Muxing(streamId, MP4_DISABLE);
					
					result = stopAttempted ? (result && dataStoreResult) : dataStoreResult;
				}
			}
			else 
			{
				message = "no stream for this id: " + streamId + " or wrong setting parameter";
			}
		}

		return new Result(result, message);
	}


	@ApiOperation(value = "Get IP Camera Error after connection failure. If returns true, it means there is an error. If returns false, there is no error", notes = "Notes here", response = Result.class)
	@GET
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/{ipAddr}/ip-camera-error")
	@Produces(MediaType.APPLICATION_JSON)
	public Result getCameraErrorV2(@ApiParam(value = "IP Addr of the Camera. This IP may contain port number", required = true) @PathParam("ipAddr") String ipAddr) {
		return super.getCameraError(ipAddr);
	}

	@ApiOperation(value = "Start external sources (IP Cameras and Stream Sources) again if it is added and stopped before", response = Result.class)
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/{id}/start")
	@Produces(MediaType.APPLICATION_JSON)
	public Result startStreamSourceV2(@ApiParam(value = "the id of the stream. The broadcast type should be IP Camera or Stream Source otherwise it does not work", required = true) @PathParam("id") String id) 
	{
		return super.startStreamSource(id);
	}

	@ApiOperation(value = "Stop streaming for the active stream. It both stops ingested(RTMP, WebRTC) or pulled stream sources (IP Cameras and Stream Sources)", response = Result.class)
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/{id}/stop")
	@Produces(MediaType.APPLICATION_JSON)
	public Result stopStreamingV2(@ApiParam(value = "the id of the broadcast.", required = true) @PathParam("id") String id) 
	{
		return super.stopStreaming(id);
	}


	@ApiOperation(value = "Get Discovered ONVIF IP Cameras, this service perform a discovery inside of internal network and get automatically  ONVIF enabled camera information", notes = "Notes here", response = Result.class)
	@GET
	@Path("/onvif-devices")
	@Produces(MediaType.APPLICATION_JSON)
	public String[] searchOnvifDevicesV2() {
		return super.searchOnvifDevices();
	}


	@ApiOperation(value = "Move IP Camera. It support continuous, relative and absolute move. By default it's relative move."
			+ "Movement parameters should be given according to movement type. "
			+ "Generally here are the values "
			+ "For Absolute move, value X and value Y is between -1.0f and 1.0f. Zooom value is between 0.0f and 1.0f"
			+ "For Relative move, value X, value Y and Zoom Value is between -1.0f and 1.0f"
			+ "For Continous move,value X, value Y and Zoom Value is between -1.0f and 1.0f ", response = Result.class)
	@POST
	@Path("/{id}/ip-camera/move")
	@Produces(MediaType.APPLICATION_JSON)
	public Result moveIPCamera(@ApiParam(value = "The id of the IP Camera", required = true) @PathParam("id") String id,
			@ApiParam(value = "Movement in X direction. If not specified, it's assumed to be zero. Valid ranges between -1.0f and 1.0f for all movements ", required = false) @QueryParam("valueX") Float valueX,
			@ApiParam(value = "Movement in Y direction. If not specified, it's assumed to be zero. Valid ranges between -1.0f and 1.0f for all movements ", required = false) @QueryParam("valueY") Float valueY,
			@ApiParam(value = "Movement in Zoom. If not specified, it's assumed to be zero. Valid ranges for relative and continous move is between -1.0f and 1.0f. For absolute move between 0.0f and 1.0f ", required = false) @QueryParam("valueZ") Float valueZ,
			@ApiParam(value = "Movement type. It can be absolute, relative or continuous. If not specified, it's relative", required = false) @QueryParam("movement") String movement
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
	
	@ApiOperation(value="Stop move for IP Camera.", response = Result.class)
	@POST
	@Path("/{id}/ip-camera/stop-move")
	@Produces(MediaType.APPLICATION_JSON)
	public Result stopMove(@ApiParam(value = "the id of the IP Camera", required = true) @PathParam("id") String id) {
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


	@ApiOperation(value = "Creates a conference room with the parameters. The room name is key so if this is called with the same room name then new room is overwritten to old one", response = ConferenceRoom.class)
	@ApiResponses(value = { @ApiResponse(code = 400, message = "If operation is no completed for any reason", response=Result.class),
			@ApiResponse(code = 200, message = "Returns the created conference room", response = ConferenceRoom.class)})
	@POST
	@Consumes({ MediaType.APPLICATION_JSON })
	@Path("/conference-rooms")
	@Produces(MediaType.APPLICATION_JSON)
	public Response createConferenceRoomV2(@ApiParam(value = "Conference Room object with start and end date", required = true) ConferenceRoom room) {

		ConferenceRoom confRoom = super.createConferenceRoom(room);
		if (confRoom != null) {
			return Response.status(Status.OK).entity(room).build();
		}
		return Response.status(Status.BAD_REQUEST).entity(new Result(false, "Operation not completed")).build();

	}

	@ApiOperation(value = "Edits previously saved conference room", response = Response.class)
	@ApiResponses(value = { @ApiResponse(code = 400, message = "If operation is no completed for any reason", response=Result.class),
			@ApiResponse(code = 200, message = "Returns the updated Conference room", response = ConferenceRoom.class)})
	@PUT
	@Consumes({ MediaType.APPLICATION_JSON })
	@Path("/conference-rooms/{room_id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response editConferenceRoom(@ApiParam(value="Room id") @PathParam("room_id") String roomId,  @ApiParam(value = "Conference Room object with start and end date", required = true) ConferenceRoom room) {

		if(room != null && getDataStore().editConferenceRoom(roomId, room)) {
			return Response.status(Status.OK).entity(room).build();
		}
		return Response.status(Status.BAD_REQUEST).entity(new Result(false, "Operation not completed")).build();
	}

	@ApiOperation(value = "Deletes a conference room. The room id is key so if this is called with the same room id then new room is overwritten to old one", response = Result.class)
	@DELETE
	@Consumes({ MediaType.APPLICATION_JSON })
	@Path("/conference-rooms/{room_id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Result deleteConferenceRoomV2(@ApiParam(value = "the id of the conference room", required = true) @PathParam("room_id") String roomId) {
		return new Result(super.deleteConferenceRoom(roomId));
	}



}