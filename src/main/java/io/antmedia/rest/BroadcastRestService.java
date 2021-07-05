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
import io.antmedia.RecordType;
import io.antmedia.StreamIdValidator;
import io.antmedia.cluster.IClusterNotifier;
import io.antmedia.cluster.IStreamInfo;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.datastore.db.types.ConferenceRoom;
import io.antmedia.datastore.db.types.Endpoint;
import io.antmedia.datastore.db.types.SocialEndpointChannel;
import io.antmedia.datastore.db.types.SocialEndpointCredentials;
import io.antmedia.datastore.db.types.Subscriber;
import io.antmedia.datastore.db.types.SubscriberStats;
import io.antmedia.datastore.db.types.TensorFlowObject;
import io.antmedia.datastore.db.types.Token;
import io.antmedia.ipcamera.OnvifCamera;
import io.antmedia.muxer.IAntMediaStreamHandler;
import io.antmedia.rest.model.BasicStreamInfo;
import io.antmedia.rest.model.Interaction;
import io.antmedia.rest.model.Result;
import io.antmedia.social.LiveComment;
import io.antmedia.statistic.type.RTMPToWebRTCStats;
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
public class BroadcastRestService extends RestServiceBase{

	
	private static final String REPLACE_CHARS = "[\n|\r|\t]";
	private static final String WEBM = "webm";
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
	public Response createBroadcast(@ApiParam(value = "Broadcast object. Set the required fields, it may be null as well.", required = false) Broadcast broadcast,
			@ApiParam(value = "Comma separated social network IDs, they must in comma separated and IDs must match with the defined IDs.", required = false) @QueryParam("socialNetworks") String socialEndpointIds,
			@ApiParam(value = "Only effective if stream is IP Camera or Stream Source. If it's true, it starts automatically pulling stream. Its value is false by default", required = false, defaultValue="false") @QueryParam("autoStart") boolean autoStart) {


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

	@ApiOperation(value = "Gets the broadcast list from database. It returns max 50 items at a time", notes = "",responseContainer = "List", response = Broadcast.class)
	@GET
	@Path("/list/{offset}/{size}")
	@Produces(MediaType.APPLICATION_JSON)
	public List<Broadcast> getBroadcastList(@ApiParam(value = "This is the offset of the list, it is useful for pagination. If you want to use sort mechanism, we recommend using Mongo DB.", required = true) @PathParam("offset") int offset,
			@ApiParam(value = "Number of items that will be fetched. If there is not enough item in the datastore, returned list size may less then this value", required = true) @PathParam("size") int size,
			@ApiParam(value = "Type of the stream. Possible values are \"liveStream\", \"ipCamera\", \"streamSource\", \"VoD\"", required = false) @QueryParam("type_by") String typeBy,
			@ApiParam(value = "Field to sort. Possible values are \"name\", \"date\", \"status\"", required = false) @QueryParam("sort_by") String sortBy,
			@ApiParam(value = "\"asc\" for Ascending, \"desc\" Descending order", required = false) @QueryParam("order_by") String orderBy,
			@ApiParam(value = "Search parameter, returns specific items that contains search string", required = false) @QueryParam("search") String search
			) {
		return getDataStore().getBroadcastList(offset, size, typeBy, sortBy, orderBy, search);
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

	@Deprecated
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
			if (status.equals(IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING)) 
			{
				boolean started = getMuxAdaptor(id).startRtmpStreaming(rtmpUrl, 0);
				result.setSuccess(started);
			}
		}
		else {
			if (logger.isErrorEnabled()) {
				logger.error("Rtmp endpoint({}) was not added to the stream: {}", rtmpUrl != null ? rtmpUrl.replaceAll(REPLACE_CHARS, "_") : null , id.replaceAll(REPLACE_CHARS, "_"));
			}
		}
		
		return result;
	}
	

	@ApiOperation(value = "Adds a third party rtmp end point to the stream. It supports adding after broadcast is started. Resolution can be specified to send a specific adaptive resolution. If an url is already added to a stream, trying to add the same rtmp url will return false.", notes = "", response = Result.class)

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/{id}/rtmp-endpoint")
	@Produces(MediaType.APPLICATION_JSON)
	public Result addEndpointV3(@ApiParam(value = "Broadcast id", required = true) @PathParam("id") String id,
			@ApiParam(value = "RTMP url of the endpoint that stream will be republished. If required, please encode the URL", required = true) Endpoint endpoint,
								@ApiParam(value = "Resolution of the broadcast that is wanted to send to the RTMP endpoint. Only applicable if there is at least an adaptive resolution is available.", required = false) @QueryParam("resolution") int resolution) {
		
		String rtmpUrl = null;
		Result result = new Result(false);
		
		if(endpoint != null && endpoint.getRtmpUrl() != null) {
			
			Broadcast broadcast = getDataStore().get(id);
			if (broadcast != null) {
			
				List<Endpoint> endpoints = broadcast.getEndPointList();
				if (endpoints == null || endpoints.stream().noneMatch(o -> o.getRtmpUrl().equals(endpoint.getRtmpUrl()))) {
					rtmpUrl = endpoint.getRtmpUrl();
					result = super.addEndpoint(id, endpoint);
				}
			}
		}
		
		if (result.isSuccess()) 
		{
			result = processRTMPEndpoint(result,  getDataStore().get(id), rtmpUrl, true, resolution);
		}
		else {
			result.setMessage("Rtmp endpoint is not added to datastore");
			if (logger.isErrorEnabled()) {
				logger.error("Rtmp endpoint({}) was not added to the stream: {}", rtmpUrl != null ? rtmpUrl.replaceAll(REPLACE_CHARS, "_") : null , id.replaceAll(REPLACE_CHARS, "_"));
			}
		}
		
		return result;
	}
	
	@Deprecated
	@DELETE
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/{id}/endpoint")
	@Produces(MediaType.APPLICATION_JSON)
	public Result removeEndpoint(@ApiParam(value = "Broadcast id", required = true) @PathParam("id") String id, 
			@ApiParam(value = "RTMP url of the endpoint that will be stopped.", required = true) @QueryParam("rtmpUrl") String rtmpUrl ) {
		Result result = super.removeEndpoint(id, rtmpUrl);
		if (result.isSuccess()) 
		{
			String status = getDataStore().get(id).getStatus();
			if (status.equals(IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING)) 
			{
				boolean started = getMuxAdaptor(id).stopRtmpStreaming(rtmpUrl, 0);
				result.setSuccess(started);
			}
		}
		else {	
		
			if (logger.isErrorEnabled()) {
				logger.error("Rtmp endpoint({}) was not removed from the stream: {}", rtmpUrl != null ? rtmpUrl.replaceAll(REPLACE_CHARS, "_") : null , id.replaceAll(REPLACE_CHARS, "_"));
			}
		}
		
		return result;
	}

	@ApiOperation(value = "Remove third pary rtmp end point from the stream. For the stream that is broadcasting, it will stop immediately", notes = "", response = Result.class)
	@DELETE
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/{id}/rtmp-endpoint")
	@Produces(MediaType.APPLICATION_JSON)
	public Result removeEndpointV2(@ApiParam(value = "Broadcast id", required = true) @PathParam("id") String id, 
			@ApiParam(value = "RTMP url of the endpoint that will be stopped.", required = true) @QueryParam("endpointServiceId") String endpointServiceId, 
								   @ApiParam(value = "Resolution specifier if endpoint has been added with resolution. Only applicable if user added RTMP endpoint with a resolution speficier. Otherwise won't work and won't remove the endpoint.", required = true) 
									   @QueryParam("resolution") int resolution){
		
		//Get rtmpURL with broadcast
		String rtmpUrl = null;
		Broadcast broadcast = getDataStore().get(id);
		Result result;
		
		if(endpointServiceId != null && broadcast != null && broadcast.getEndPointList() != null && !broadcast.getEndPointList().isEmpty() ) {
			for(Endpoint endpoint: broadcast.getEndPointList()) {
				if(endpoint.getEndpointServiceId().equals(endpointServiceId)) {
					rtmpUrl = endpoint.getRtmpUrl();
				}
			}
		}

		result = super.removeRTMPEndpoint(id, endpointServiceId);
		
		if (result.isSuccess()) 
		{
			result = processRTMPEndpoint(result, broadcast, rtmpUrl, false, resolution);
		}
		else if (logger.isErrorEnabled()) {	
			logger.error("Rtmp endpoint({}) was not removed from the stream: {}", rtmpUrl != null ? rtmpUrl.replaceAll(REPLACE_CHARS, "_") : null , id.replaceAll(REPLACE_CHARS, "_"));
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

	@ApiOperation(value = "Get the number of broadcasts depending on the searched items ", notes = "", response = SimpleStat.class)
	@GET
	@Path("/count/{search}")
	@Produces(MediaType.APPLICATION_JSON)
	public SimpleStat getTotalBroadcastNumberV2(
			@ApiParam(value = "Search parameter to get the number of items including it ", required = true) @PathParam("search") String search)
	{
		return new SimpleStat(getDataStore().getPartialBroadcastNumber(search));
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
	
	@ApiOperation(value = "Generates JWT token for specified stream. It's not required to let the server generate JWT. Generally JWT tokens should be generated on the client side.")
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Returns token", response=Token.class), 
			@ApiResponse(code = 400, message = "When there is an error in creating token", response=Result.class)})
	@GET
	@Path("/{id}/jwt-token")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getJwtTokenV2 (@ApiParam(value = "The id of the stream", required = true) @PathParam("id")String streamId,
			@ApiParam(value = "The expire time of the token. It's in unix timestamp seconds.", required = true) @QueryParam("expireDate") long expireDate,
			@ApiParam(value = "Type of the JWT token. It may be play or publish ", required = true) @QueryParam("type") String type,
			@ApiParam(value = "Room Id that token belongs to. It's not mandatory ", required = false) @QueryParam("roomId") String roomId) 
	{
		Object result = super.getJwtToken(streamId, expireDate, type, roomId);
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
	
	@ApiOperation(value = "Get the all subscribers of the requested stream", notes = "",responseContainer = "List", response = Subscriber.class)
	@GET
	@Path("/{id}/subscribers/list/{offset}/{size}")
	@Produces(MediaType.APPLICATION_JSON)
	public List<Subscriber> listSubscriberV2(@ApiParam(value = "the id of the stream", required = true) @PathParam("id") String streamId,
			@ApiParam(value = "the starting point of the list", required = true) @PathParam("offset") int offset,
			@ApiParam(value = "size of the return list (max:50 )", required = true) @PathParam("size") int size) {
		List<Subscriber> subscribers = null;
		if(streamId != null) {
			subscribers = getDataStore().listAllSubscribers(streamId, offset, size);
		}
		return subscribers;
	}	
	
	@ApiOperation(value = "Get the all subscriber statistics of the requested stream", notes = "",responseContainer = "List", response = SubscriberStats.class)
	@GET
	@Path("/{id}/subscriber-stats/list/{offset}/{size}")
	@Produces(MediaType.APPLICATION_JSON)
	public List<SubscriberStats> listSubscriberStatsV2(@ApiParam(value = "the id of the stream", required = true) @PathParam("id") String streamId,
			@ApiParam(value = "the starting point of the list", required = true) @PathParam("offset") int offset,
			@ApiParam(value = "size of the return list (max:50 )", required = true) @PathParam("size") int size) {
		List<SubscriberStats> subscriberStats = null;
		if(streamId != null) {
			subscriberStats = getDataStore().listAllSubscriberStats(streamId, offset, size);
		}
		return subscriberStats;
	}
	
	@ApiOperation(value = "Add Subscriber to the requested stream ", response = Result.class)
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/{id}/subscribers")
	@Produces(MediaType.APPLICATION_JSON)
	public Result addSubscriber(
			@ApiParam(value = "the id of the stream", required = true) @PathParam("id") String streamId,
			@ApiParam(value = "Subscriber to be added to this stream", required = true) Subscriber subscriber) {
		boolean result = false;
		if (subscriber != null) {
			// add stream id inside the Subscriber
			subscriber.setStreamId(streamId);
			// create a new stats object before adding to datastore
			subscriber.setStats(new SubscriberStats());
			// subscriber is not connected yet
			subscriber.setConnected(false);
			
			if (streamId != null) {
				result = getDataStore().addSubscriber(streamId, subscriber);
			}
		}
		return new Result(result);
	}
	
	@ApiOperation(value = "Delete specific subscriber from data store for selected stream", response = Result.class)
	@DELETE
	@Consumes({ MediaType.APPLICATION_JSON })
	@Path("/{id}/subscribers/{sid}")
	@Produces(MediaType.APPLICATION_JSON)
	public Result deleteSubscriber(@ApiParam(value = "the id of the stream", required = true) @PathParam("id") String streamId,
			@ApiParam(value = "the id of the subscriber", required = true) @PathParam("sid") String subscriberId) {
		boolean result =  false;
				
		if(streamId != null) {
			result = getDataStore().deleteSubscriber(streamId, subscriberId);
		}

		return new Result(result);	
	}

	@ApiOperation(value = " Removes all subscriber related with the requested stream", notes = "", response = Result.class)
	@DELETE
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/{id}/subscribers")
	@Produces(MediaType.APPLICATION_JSON)
	public Result revokeSubscribers(@ApiParam(value = "the id of the stream", required = true) @PathParam("id") String streamId) {
		boolean result =  false;
		
		if(streamId != null) {
			result = getDataStore().revokeSubscribers(streamId);
		}

		return new Result(result);
	}	
	
	@ApiOperation(value = "Get the broadcast live statistics total RTMP watcher count, total HLS watcher count, total WebRTC watcher count", notes = "", response = BroadcastStatistics.class)
	@GET
	@Path("/{id}/broadcast-statistics")
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public BroadcastStatistics getBroadcastStatistics(@ApiParam(value = "the id of the stream", required = true) @PathParam("id") String id) {
		return super.getBroadcastStatistics(id);
	}
	
	@ApiOperation(value = "Get the total broadcast live statistics total HLS watcher count, total WebRTC watcher count", notes = "", response = BroadcastStatistics.class)
	@GET
	@Path("/total-broadcast-statistics")
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public AppBroadcastStatistics getBroadcastTotalStatistics() {
		return super.getBroadcastTotalStatistics();
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
	
	@ApiOperation(value = "Get RTMP to WebRTC path stats in general", notes = "",response = RTMPToWebRTCStats.class)
	@GET
	@Path("/{id}/rtmp-to-webrtc-stats")
	@Produces(MediaType.APPLICATION_JSON)
	public RTMPToWebRTCStats getRTMPToWebRTCStats(@ApiParam(value = "the id of the stream", required = true) @PathParam("id") String id) 
	{
		return getApplication().getRTMPToWebRTCStats(id);
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

	@Deprecated
	@ApiOperation(value = "Returns filtered broadcast list according to type. It's useful for getting IP Camera and Stream Sources from the whole list. If you want to use sort mechanism, we recommend using Mongo DB.", notes = "",responseContainer = "List",response = Broadcast.class)
	@GET
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/filter-list/{offset}/{size}/{type}")
	@Produces(MediaType.APPLICATION_JSON)
	public List<Broadcast> filterBroadcastListV2(@ApiParam(value = "starting point of the list", required = true) @PathParam("offset") int offset,
			@ApiParam(value = "size of the return list (max:50 )", required = true) @PathParam("size") int size,
			@ApiParam(value = "type of the stream. Possible values are \"liveStream\", \"ipCamera\", \"streamSource\", \"VoD\"", required = true) @PathParam("type") String type,
			@ApiParam(value = "field to sort", required = false) @QueryParam("sort_by") String sortBy,
			@ApiParam(value = "asc for Ascending, desc Descending order", required = false) @QueryParam("order_by") String orderBy
			) {
		return getDataStore().getBroadcastList(offset, size, type, sortBy, orderBy, null);
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
	
	
	@ApiOperation(value = "Set stream specific recording setting, this setting overrides general Mp4 and WebM Muxing Setting", notes = "", response = Result.class)
	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/{id}/recording/{recording-status}")
	@Produces(MediaType.APPLICATION_JSON)
	public Result enableRecording(@ApiParam(value = "the id of the stream", required = true) @PathParam("id") String streamId,
			@ApiParam(value = "Change recording status. If true, starts recording. If false stop recording", required = true) @PathParam("recording-status") boolean enableRecording,
			@ApiParam(value = "Record type: 'mp4' or 'webm'. It's optional parameter.", required = false) @QueryParam("recordType") String recordType) {
		if (logger.isInfoEnabled()) {
			logger.info("Recording method is called for {} to make it {} and record Type: {}", streamId.replaceAll(REPLACE_CHARS, "_"), enableRecording, recordType != null ? recordType.replaceAll(REPLACE_CHARS, "_") : null);
		}
		recordType = (recordType==null)?RecordType.MP4.toString():recordType;  // It means, if recordType is null, function using Mp4 Record by default
		return enableRecordMuxing(streamId, enableRecording, recordType);
	}


	@ApiOperation(value = "Get IP Camera Error after connection failure. If returns true, it means there is an error. If returns false, there is no error", notes = "Notes here", response = Result.class)
	@GET
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/{streamId}/ip-camera-error")
	@Produces(MediaType.APPLICATION_JSON)
	public Result getCameraErrorV2(@ApiParam(value = "StreamId of the IP Camera Streaming.", required = true) @PathParam("streamId") String streamId) {
		return super.getCameraErrorById(streamId);
	}

	@ApiOperation(value = "Start streaming sources(IP Cameras, Stream Sources, PlayLists) ", response = Result.class)
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
	
	@ApiOperation(value = "Add a subtrack to a main track (broadcast).", notes = "", response = Result.class)
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/{id}/subtrack")
	@Produces(MediaType.APPLICATION_JSON)
	public Result addSubTrack(@ApiParam(value = "Broadcast id", required = true) @PathParam("id") String id,
			@ApiParam(value = "Subtrack Stream Id", required = true) @QueryParam("id") String subTrackId) {
		
		Broadcast subTrack = getDataStore().get(subTrackId);
		subTrack.setMainTrackStreamId(id);
		boolean success = getDataStore().updateBroadcastFields(subTrackId, subTrack);
		success = success && getDataStore().addSubTrack(id, subTrackId);
		return new Result(success);
	}
	
	@ApiOperation(value = "Returns the stream info(width, height, bitrates and video codec) of the stream", response= BasicStreamInfo[].class)
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

	@ApiOperation(value = "Send stream participants a message through Data Channel in a WebRTC stream", notes = "", response = Result.class)
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/{id}/data")
	@Produces(MediaType.APPLICATION_JSON)
	public Result sendMessage(@ApiParam(value = "Message through Data Channel which will be sent to all WebRTC stream participants", required = true) String message, 
			@ApiParam(value = "Broadcast id", required = true) @PathParam("id") String id) {

		AntMediaApplicationAdapter application = getApplication();
		// check if WebRTC data channels are supported in this edition
		if(application != null && application.isDataChannelMessagingSupported()) {
			// check if data channel is enabled in the settings
			if(application.isDataChannelEnabled()) {
				// check if stream with given stream id exists
				if(application.doesWebRTCStreamExist(id)) {
					 // send the message through the application
					 boolean status = application.sendDataChannelMessage(id,message);
					 if(status) {
						 return new Result(true);
					 } else {
						 return new Result(false, "Operation not completed");
					 }
					
				} else {
					return new Result(false, "Requested WebRTC stream does not exist");
				}
				
			} else {
				return new Result(false, "Data channels are not enabled");
			}
			
		} else {
			return new Result(false, "Operation not supported in the Community Edition. Check the Enterprise version for more features.");
		}
	}
	@ApiOperation(value = "Gets the conference room list from database", notes = "",responseContainer = "List", response = ConferenceRoom.class)
	@GET
	@Path("/conference-rooms/list/{offset}/{size}")
	@Produces(MediaType.APPLICATION_JSON)
	public List<ConferenceRoom> getConferenceRoomList(@ApiParam(value = "This is the offset of the list, it is useful for pagination. If you want to use sort mechanism, we recommend using Mongo DB.", required = true) @PathParam("offset") int offset,
											@ApiParam(value = "Number of items that will be fetched. If there is not enough item in the datastore, returned list size may less then this value", required = true) @PathParam("size") int size,
											@ApiParam(value = "field to sort", required = false) @QueryParam("sort_by") String sortBy,
											@ApiParam(value = "asc for Ascending, desc Descending order", required = false) @QueryParam("order_by") String orderBy,
											@ApiParam(value = "Search parameter, returns specific items that contains search string", required = false) @QueryParam("search") String search
	) {
		return getDataStore().getConferenceRoomList(offset, size ,sortBy, orderBy, search);
	}

	@ApiOperation(value = "Get conference room object")
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Return the ConferenceRoom object"),
			@ApiResponse(code = 404, message = "ConferenceRoom object not found")})
	@GET
	@Path("/conference-rooms/{roomId}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getConferenceRoom(@ApiParam(value = "id of the room", required = true) @PathParam("roomId") String id) {
		ConferenceRoom room = null;
		if (id != null) {
			room = lookupConference(id);
		}
		if (room != null) {
			return Response.status(Status.OK).entity(room).build();
		}
		else {
			return Response.status(Status.NOT_FOUND).build();
		}
	}

	@ApiOperation(value="Returns the streams Ids in the room.",responseContainer ="List",response = String.class)
	@GET
	@Consumes({ MediaType.APPLICATION_JSON })
	@Path("/conference-rooms/{room_id}/room-info")
	@Produces(MediaType.APPLICATION_JSON)
	public RootRestService.RoomInfo getRoomInfo(@ApiParam(value="Room id", required=true) @PathParam("room_id") String roomId,
												@ApiParam(value="If Stream Id is entered, that stream id will be isolated from the result",required = false) @QueryParam("streamId") String streamId){
		ConferenceRoom room = getDataStore().getConferenceRoom(roomId);
		return new RootRestService.RoomInfo(roomId,RestServiceBase.getRoomInfoFromConference(roomId,streamId,getDataStore()), room);
	}

	@ApiOperation(value="Adds the specified stream with streamId to the room. ",response = Result.class)
	@PUT
	@Consumes({ MediaType.APPLICATION_JSON })
	@Path("/conference-rooms/{room_id}/add")
	@Produces(MediaType.APPLICATION_JSON)
	public Result addStreamToTheRoom(@ApiParam(value="Room id", required=true) @PathParam("room_id") String roomId,
												@ApiParam(value="Stream id to add to the conference room",required = true) @QueryParam("streamId") String streamId){
		return new Result(RestServiceBase.addStreamToConferenceRoom(roomId,streamId,getDataStore()));
	}

	@ApiOperation(value="Deletes the specified stream correlated with streamId in the room. ",response = Result.class)
	@PUT
	@Consumes({ MediaType.APPLICATION_JSON })
	@Path("/conference-rooms/{room_id}/delete")
	@Produces(MediaType.APPLICATION_JSON)
	public Result deleteStreamFromTheRoom(@ApiParam(value="Room id", required=true) @PathParam("room_id") String roomId,
									  @ApiParam(value="Stream id to delete from the conference room",required = true) @QueryParam("streamId") String streamId){
		return new Result(RestServiceBase.removeStreamFromRoom(roomId,streamId,getDataStore()));
	}
	
	@ApiOperation(value="Seek seconds in Stream Fetcher.",responseContainer ="List",response = String.class)
	@GET
	@Consumes({ MediaType.APPLICATION_JSON })
	@Path("/seek-stream/{streamId}/{streamTime}")
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public Result changeStreamTime(@ApiParam(value="streamId", required=true) @PathParam("streamId") String streamId,
			@ApiParam(value="streamTime", required=true) @PathParam("streamTime") int streamTime
			){
		return super.changeStreamTime(streamId, streamTime);
	}

}
