package io.antmedia.rest;


import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.media.multipart.FormDataParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.AppSettings;
import io.antmedia.StreamIdValidator;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.datastore.db.types.ConferenceRoom;
import io.antmedia.datastore.db.types.SocialEndpointChannel;
import io.antmedia.datastore.db.types.SocialEndpointCredentials;
import io.antmedia.datastore.db.types.TensorFlowObject;
import io.antmedia.datastore.db.types.Token;
import io.antmedia.datastore.db.types.VoD;
import io.antmedia.rest.model.Interaction;
import io.antmedia.rest.model.Result;
import io.antmedia.rest.model.Version;
import io.antmedia.social.LiveComment;
import io.antmedia.webrtc.api.IWebRTCAdaptor;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.Contact;
import io.swagger.annotations.ExternalDocs;
import io.swagger.annotations.Info;
import io.swagger.annotations.License;
import io.swagger.annotations.SwaggerDefinition;

/**
 * Use BroadcastRestServiceV2
 * @author mekya
 *
 */
@Component
@Path("/")
@Deprecated
public class BroadcastRestService extends RestServiceBase{


	@ApiModel(value="BroadcastStatistics", description="The statistics class of the broadcasts")
	public static class BroadcastStatistics {

		@ApiModelProperty(value = "the total RTMP viewers of the stream")
		public final int totalRTMPWatchersCount;

		@ApiModelProperty(value = "the total HLS viewers of the stream")
		public final int totalHLSWatchersCount;

		@ApiModelProperty(value = "the total WebRTC viewers of the stream")
		public final int totalWebRTCWatchersCount;

		public BroadcastStatistics(int totalRTMPWatchersCount, int totalHLSWatchersCount,
				int totalWebRTCWatchersCount) {
			this.totalRTMPWatchersCount = totalRTMPWatchersCount;
			this.totalHLSWatchersCount = totalHLSWatchersCount;
			this.totalWebRTCWatchersCount = totalWebRTCWatchersCount;
		}
	}

	@ApiModel(value="LiveStatistics", description="The statistics class of the broadcasts live stream count")
	public static class LiveStatistics  {

		@ApiModelProperty(value = "the total live stream count of the stream")
		public final long totalLiveStreamCount;

		public LiveStatistics(long totalLiveStreamCount) {
			this.totalLiveStreamCount = totalLiveStreamCount;
		}

	}

	public static final int MAX_ITEM_IN_ONE_LIST = 50;
	public static final int ERROR_SOCIAL_ENDPOINT_UNDEFINED_CLIENT_ID = -1;
	public static final int ERROR_SOCIAL_ENDPOINT_UNDEFINED_ENDPOINT = -2;
	public static final int ERROR_SOCIAL_ENDPOINT_EXCEPTION_IN_ASKING_AUTHPARAMS = -3;



	public interface ProcessBuilderFactory {
		Process make(String...args);
	}

	protected static Logger logger = LoggerFactory.getLogger(BroadcastRestService.class);


	/**
	 * Creates a broadcast and returns the full broadcast object with rtmp
	 * address and other information.
	 * 
	 * @param broadcast
	 *            Broadcast object only related information should be set, it
	 *            may be null as well.
	 * 
	 * @return {@link io.antmedia.datastore.db.types.Broadcast}
	 * 
	 */
	@POST
	@Consumes({ MediaType.APPLICATION_JSON })
	@Path("/broadcast/create")
	@Produces(MediaType.APPLICATION_JSON)
	public Broadcast createBroadcast(@ApiParam(value = "Broadcast object only related information should be set, it may be null as well.", required = true) Broadcast broadcast) {
		if (broadcast != null) {
			// make sure stream id is not set on rest service
			broadcast.resetStreamId();
		}

		return createBroadcastWithStreamID(broadcast);
	}

	/**
	 * Ant Media Server does not use this rest service by default. 
	 * 
	 * Creates a broadcast without reset StreamID and returns the full broadcast object with rtmp
	 * address and other information.
	 * 
	 * @param broadcast
	 *            Broadcast object only related information should be set, it
	 *            may be null as well.
	 * 
	 * @return {@link io.antmedia.datastore.db.types.Broadcast}
	 * 
	 */
	@POST
	@Consumes({ MediaType.APPLICATION_JSON })
	@Path("/broadcast/createWithStreamID")
	@Produces(MediaType.APPLICATION_JSON)
	public Broadcast createBroadcastWithStreamID(@ApiParam(value = "Broadcast object only related information should be set, it may be null as well.", required = true) Broadcast broadcast) {
		if (broadcast != null) {
			//check stream id if exists
			boolean nameValid = StreamIdValidator.isStreamIdValid(broadcast.getStreamId()); 
			if(!nameValid) {
				logger.error("Stream name ({}) is invalid.", broadcast.getStreamId());
				return null;
			}
		}
		
		return super.createBroadcastWithStreamID(broadcast);
	}


	/**
	 * Creates a conference room with the parameters. 
	 * The room name is key so if this is called with the same room name 
	 * then new room is overwritten to old one.
	 * 
	 * @param room Conference Room object with start and end date
	 * 
	 * @return {@link io.antmedia.datastore.db.types.ConferenceRoom}
	 * 
	 */
	@POST
	@Consumes({ MediaType.APPLICATION_JSON })
	@Path("/broadcast/createConferenceRoom")
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public ConferenceRoom createConferenceRoom(@ApiParam(value = "Conference Room object with start and end date", required = true) ConferenceRoom room) {
		return super.createConferenceRoom(room);
	}

	/**
	 * Edits previously saved conference room
	 * @param room Conference Room object with start and end date
	 * 
	 * @return {@link io.antmedia.datastore.db.types.ConferenceRoom}
	 * 
	 */
	@POST
	@Consumes({ MediaType.APPLICATION_JSON })
	@Path("/broadcast/editConferenceRoom")
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public ConferenceRoom editConferenceRoom(@ApiParam(value = "Conference Room object with start and end date", required = true) ConferenceRoom room) {
		return super.editConferenceRoom(room);
	}

	/**
	 * Deletes previously saved conference room 
	 * @param roomName the name of the conference room
	 * 
	 * @return true if successfully deleted, false if not 
	 * 
	 */
	@POST
	@Consumes({ MediaType.APPLICATION_JSON })
	@Path("/broadcast/deleteConferenceRoom")
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public boolean deleteConferenceRoom(@ApiParam(value = "the name of the conference room", required = true) @QueryParam("roomName") String roomName) {
		return super.deleteConferenceRoom(roomName);
	}


	/**
	 * Use createBroadcast with listenerHookURL
	 * @param name
	 * @param listenerHookURL
	 * @return
	 * 
	 * deprecated use createBroadcast with listenerHookURL , it will be deleted.
	 */
	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Path("/broadcast/createPortalBroadcast")
	@Produces(MediaType.APPLICATION_JSON)
	public Broadcast createPortalBroadcast(@ApiParam(value = "name of the broadcast", required = true) @FormParam("name") String name, @ApiParam(value = "listenerHookURL", required = true) @FormParam("listenerHookURL") String listenerHookURL) {

		Broadcast broadcast=new Broadcast();

		broadcast.setName(name);
		broadcast.setListenerHookURL(listenerHookURL);
		String settingsListenerHookURL = null; 
		String fqdn = null;
		AppSettings appSettingsLocal = getAppSettings();
		if (appSettingsLocal != null) {
			settingsListenerHookURL = appSettingsLocal.getListenerHookURL();
		}
		fqdn = getServerSettings().getServerName();

		return saveBroadcast(broadcast, AntMediaApplicationAdapter.BROADCAST_STATUS_CREATED, getScope().getName(),
				getDataStore(), settingsListenerHookURL, fqdn, getServerSettings().getHostAddress());
	}




	/**
	 * Create broadcast and bind social networks at the same time. Server should
	 * be authorized in advance to make this service return success
	 * 
	 * @param broadcast
	 *            Broadcast {@link io.antmedia.datastore.db.types.Broadcast}
	 * 
	 * @param socialNetworksToPublish
	 *            Comma separated social network names Social network names must
	 *            in comma separated and names must match with the defined names
	 *            like facebook,periscope,youtube etc.
	 * 
	 * @return {@link io.antmedia.rest.BroadcastRestService.Result}
	 * 
	 */
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/broadcast/createWithSocial")
	@Produces(MediaType.APPLICATION_JSON)
	public Broadcast createWithSocial(@ApiParam(value = "Broadcast", required = true) Broadcast broadcast,
			@ApiParam(value = "Comma separated social network IDs, they must in comma separated and IDs must match with the defined IDs.", required = true) @QueryParam("socialNetworks") String socialEndpointIds) {
		broadcast = createBroadcast(broadcast);
		if (broadcast.getStreamId() != null && socialEndpointIds != null) {
			String[] endpointIds = socialEndpointIds.split(",");
			for (String endpointId : endpointIds) {
				addSocialEndpoint(broadcast.getStreamId(), endpointId);
			}
		}

		return getBroadcast(broadcast.getStreamId());
	}


	/**
	 * Stops broadcasting of requested stream
	 * 
	 * @param streamId the id of the requested stream
	 * 
	 * @return {@link io.antmedia.rest.BroadcastRestService.Result}
	 */

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/broadcast/stop/{streamId}")
	@Produces(MediaType.APPLICATION_JSON)
	public Result stopBroadcast(@ApiParam(value = "id of the stream", required = true) @PathParam("streamId") String streamId) {
		boolean result = false;

		if (streamId != null) {
			result = stopBroadcastInternal(getDataStore().get(streamId));
		}

		return new Result(result);
	}

	/**
	 * Updates the properties of the broadcast
	 * 
	 * @param broadcast {@link io.antmedia.datastore.db.types.Broadcast}
	 * @param socialNetworksToPublish    Comma separated social network names Social network names must
	 *           						 in comma separated and names must match with the defined names
	 *          						 like Facebook, Periscope,Youtube etc.
	 * @return {@link io.antmedia.rest.BroadcastRestService.Result}
	 */
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/broadcast/update")
	@Produces(MediaType.APPLICATION_JSON)
	public Result updateBroadcast(@ApiParam(value = "Broadcast object", required = true) Broadcast broadcast,
			@ApiParam(value = "Comma separated social network IDs, they must in comma separated and IDs must match with the defined IDs", required = true) @QueryParam("socialNetworks") String socialNetworksToPublish) {

		return super.updateBroadcast(broadcast.getStreamId(), broadcast, socialNetworksToPublish);
	}

	/**
	 * Revokes authorization from a social network account that is authorized
	 * before
	 * 
	 * @param endpointId the social network endpoint id of the social network account
	 * @return {@link io.antmedia.rest.BroadcastRestService.Result}
	 */
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/broadcast/revokeSocialNetwork/{endpointId}")
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public Result revokeSocialNetwork(@ApiParam(value = "Endpoint id", required = true) @PathParam("endpointId") String endpointId) {
		return super.revokeSocialNetwork(endpointId);
	}

	/**
	 * Add social endpoint to a stream. 
	 * 
	 * @param id - the id of the stream
	 * 
	 * @param endpointServiceId
	 *            the id  of the service in order
	 *            to have successful operation. Social network must be
	 *            authorized in advance
	 * 
	 * @return {@link io.antmedia.rest.BroadcastRestService.Result}
	 * 
	 */
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/broadcast/addSocialEndpointJS/{id}/{endpointServiceId}")
	@Produces(MediaType.APPLICATION_JSON)
	public Result addSocialEndpointJSON(@ApiParam(value = "Stream id", required = true) @PathParam("id") String id,
			@ApiParam(value = "the id of the service in order to have successfull operation. Social network must be authorized in advance", required = true) @PathParam("endpointServiceId") String endpointServiceId) {
		return addSocialEndpoint(id, endpointServiceId);
	}

	/**
	 * Add social endpoint to a stream. Use the JSON version of this method
	 * 
	 * @param id
	 *            of the stream
	 * 
	 * @param endpointServiceId
	 *            the id of the service in order
	 *            to have successfull operation. Social network must be
	 *            authorized in advance
	 * @return {@link io.antmedia.rest.BroadcastRestService.Result}
	 * 
	 */
	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Path("/broadcast/addSocialEndpoint")
	@Produces(MediaType.APPLICATION_JSON)
	public Result addSocialEndpoint(@ApiParam(value = "Stream id", required = true) @FormParam("id") String id,

			@ApiParam(value = "the id of the service in order to have successfull operation. Social network must be authorized in advance", required = true)
	@FormParam("serviceName") String endpointServiceId) {
		
		return super.addSocialEndpoint(id, endpointServiceId);
	}

	/**
	 * Add a third pary RTMP end point to the stream. When broadcast is started,
	 * it will send RTMP stream to this RTMP URL as well.
	 * 
	 * @param id
	 *            This is the id of broadcast
	 * 
	 * @param rtmpUrl
	 *            RTMP  URL of the endpoint that stream will be republished
	 * 
	 * @return {@link io.antmedia.rest.BroadcastRestService.Result}
	 * 
	 */
	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Path("/broadcast/addEndpoint")
	@Produces(MediaType.APPLICATION_JSON)
	public Result addEndpoint(@ApiParam(value = "Broadcast id", required = true) @FormParam("id") String id,
			@ApiParam(value = "RTMP url of the endpoint that stream will be republished", required = true) @FormParam("rtmpUrl") String rtmpUrl) {
		return super.addEndpoint(id, rtmpUrl);
	}

	/**
	 * Returns live comments from a specific endpoint like Facebook, Youtube, Pscp, etc.
	 * It works If interactivity is collected which can be enabled/disabled by properties file.
	 * 
	 * @param endpointServiceId This is the id of the endpoint service 
	 * @param streamId This is the id of the stream
	 * @param offset this is the start offset where to start getting comment
	 * @param batch number of items to be returned
	 * @return {@link io.antmedia.social.LiveComment }
	 */
	@GET
	@Path("/broadcast/getLiveComments/{endpointServiceId}/{streamId}/{offset}/{batch}")
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public List<LiveComment> getLiveCommentsFromEndpoint(@ApiParam(value = "This is the id of the endpoint service", required = true)
				@PathParam("endpointServiceId") String endpointServiceId,
				@ApiParam(value = "Stream id", required = true)
				@PathParam("streamId") String streamId,
				@ApiParam(value = "this is the start offset where to start getting comment", required = true)
				@PathParam("offset") int offset,
				@ApiParam(value = "number of items to be returned", required = true)
				@PathParam("batch") int batch) 
	{

		return super.getLiveCommentsFromEndpoint(endpointServiceId, streamId, offset, batch);
	}

	/**
	 * Return the number of live views in specified video service endpoint
	 * It works If interactivity is collected which can be enabled/disabled by properties file.
	 * 
	 * @param endpointServiceId- the id of the endpoint
	 * @param streamId- the id of the stream
	 * @return {@link io.antmedia.rest.BroadcastRestService.Result}
	 */
	@GET
	@Path("/broadcast/getLiveViewsCount/{endpointServiceId}/{streamId}")
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public Result getViewerCountFromEndpoint(@ApiParam(value = "the id of the endpoint", required = true)
	@PathParam("endpointServiceId") String endpointServiceId,
	@ApiParam(value = "the id of the stream", required = true)
	@PathParam("streamId") String streamId) 
	{
		return super.getViewerCountFromEndpoint(endpointServiceId, streamId);
	}


	/**
	 * Returns the number of live comment count in a specific video service endpoint
	 * It works If interactivity is collected which can be enabled/disabled by properties file.
	 * 
	 * @param endpointServiceId- the id of the endpoint
	 * @param streamId- the id of the stream
	 * @return @return {@link io.antmedia.rest.BroadcastRestService.Result}
	 */
	@GET
	@Path("/broadcast/getLiveCommentsCount/{endpointServiceId}/{streamId}")
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public Result getLiveCommentsCount(@ApiParam(value = " the id of the endpoint", required = true) @PathParam("endpointServiceId") String endpointServiceId,
			@ApiParam(value = "the id of the stream", required = true)  @PathParam("streamId") String streamId) {
		return super.getLiveCommentsCount(endpointServiceId, streamId);
	}

	/**
	 * Return the interaction from a specific endpoint like facebook, youtube, pscp, etc. 
	 * It works If interactivity is collected which can be enabled/disabled by properties file.
	 * 
	 * @param endpointServiceId- the id of the endpoint
	 * @param streamId- the id of the stream
	 * @return {@link io.antmedia.rest.model.Interaction }
	 */
	@GET
	@Path("/broadcast/getInteraction/{endpointServiceId}/{streamId}")
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public Interaction getInteractionFromEndpoint(@ApiParam(value = "the id of the endpoint", required = true) @PathParam("endpointServiceId") String endpointServiceId,
			@ApiParam(value = "the id of the stream", required = true) @PathParam("streamId") String streamId) {
		return super.getInteractionFromEndpoint(endpointServiceId, streamId);
	}

	/**
	 * Get broadcast object
	 * 
	 * @param id - id of the broadcast
	 * 
	 * @return broadcast object nothing if broadcast is not found
	 * 
	 */
	@GET
	@Path("/broadcast/get")
	@Produces(MediaType.APPLICATION_JSON)
	public Broadcast getBroadcast(@ApiParam(value = "id of the broadcast", required = true) @QueryParam("id") String id) {
		Broadcast broadcast = null;
		if (id != null) {
			broadcast = lookupBroadcast(id);
		}
		if (broadcast == null) {
			broadcast = new Broadcast(null, null);
		}
		return broadcast;
	}

	/**
	 * Get VoD file from database
	 * @param id- id of the VoD
	 * @return {@link io.antmedia.datastore.db.types.VoD}
	 */
	@GET
	@Path("/broadcast/getVoD")
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public VoD getVoD(@ApiParam(value = "id of the VoD", required = true) @QueryParam("id") String id) {
		return super.getVoD(id);
	}
	
	/**
	 * Get detected objects from the stream
	 * 
	 * @param id - the id of the stream 
	 * 
	 * @return  the list of TensorFlowObject objects {@link io.antmedia.datastore.db.types.TensorFlowObject}
	 */
	@GET
	@Path("/detection/get")
	@Produces(MediaType.APPLICATION_JSON)
	public List<TensorFlowObject> getDetectedObjects(@ApiParam(value = "id of the stream", required = true) @QueryParam("id") String id) {
		List<TensorFlowObject> list = null;

		if (id != null) {
			list = getDataStore().getDetection(id);
		}

		if (list == null) {
			//do not return null in rest service
			list = new ArrayList<>();
		}

		return list;
	}


	/**
	 * Get detected objects from the stream based on offset and size
	 * 
	 * @param id- the id of the stream
	 * @param offset - starting point of the list
	 * @param size - total size of the return list
	 * @return the list of TensorFlowObject objects {@link io.antmedia.datastore.db.types.TensorFlowObject}
	 */

	@GET
	@Path("/detection/getList/{offset}/{size}")
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public List<TensorFlowObject> getDetectionList(@ApiParam(value = "the id of the stream", required = true) @QueryParam("id") String id,
			@ApiParam(value = "starting point of the list", required = true) @PathParam("offset") int offset,
			@ApiParam(value = "total size of the return list", required = true) @PathParam("size") int size) {
		return super.getDetectionList(id, offset, size);
	}

	/**
	 * Get total number of detected objects
	 * 
	 * @param id
	 *            id of the stream
	 * 
	 * @return number of detected objects
	 * 
	 */
	@GET
	@Path("/detection/getObjectDetectedTotal")
	@Produces(MediaType.APPLICATION_JSON)
	public long getObjectDetectedTotal(@ApiParam(value = "id of the stream", required = true) @QueryParam("id") String id){
		return getDataStore().getObjectDetectedTotal(id);
	}


	/**
	 * Get the broadcast list from database
	 * 
	 * @param offset
	 *            This is the offset of the list, it is useful for pagination,
	 * 
	 * @param size
	 *            Number of items that will be fetched. If there is not enough
	 *            item in the datastore, returned list size may less then this
	 *            value
	 * 
	 * @return JSON list of broadcast objects
	 * 
	 */
	@GET
	@Path("/broadcast/getList/{offset}/{size}")
	@Produces(MediaType.APPLICATION_JSON)
	public List<Broadcast> getBroadcastList(@ApiParam(value = "This is the offset of the list, it is useful for pagination", required = true) @PathParam("offset") int offset,
			@ApiParam(value = "Number of items that will be fetched. If there is not enough item in the datastore, returned list size may less then this value", required = true) @PathParam("size") int size) {
		return getDataStore().getBroadcastList(offset, size);
	}


	/**
	 * Import Live Streams to Stalker Portal
	 * 
	 * @return {@link io.antmedia.rest.BroadcastRestService.Result}
	 */

	@POST
	@Path("/importLiveStreamsToStalker")
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public Result importLiveStreams2Stalker() 
	{
		return super.importLiveStreams2Stalker();
	}



	/**
	 * Import VoDs to Stalker Portal
	 * 
	 * @return {@link io.antmedia.rest.BroadcastRestService.Result}
	 */
	@POST
	@Path("/importVoDsToStalker")
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public Result importVoDsToStalker() 
	{
		return super.importVoDsToStalker();
	}

	/**
	 * Get the VoD list from database
	 * 
	 * @param offset
	 *            This is the offset of the list, it is useful for pagination,
	 * 
	 * @param size
	 *            Number of items that will be fetched. If there is not enough
	 *            item in the datastore, returned list size may less then this
	 *            value
	 * 
	 * @return JSON list of VoD objects
	 * 
	 */
	@GET
	@Path("/broadcast/getVodList/{offset}/{size}")
	@Produces(MediaType.APPLICATION_JSON)
	public List<VoD> getVodList(@ApiParam(value = "offset of the list", required = true) @PathParam("offset") int offset,
			@ApiParam(value = "Number of items that will be fetched", required = true) @PathParam("size") int size) {
		return getDataStore().getVodList(offset, size, null, null);
	}

	/**
	 * Get the total number of VoDs
	 * 
	 * @return the number of total VoDs
	 */

	@GET
	@Path("/broadcast/getTotalVodNumber")
	@Produces(MediaType.APPLICATION_JSON)
	public long getTotalVodNumber() {
		return getDataStore().getTotalVodNumber();
	}

	/**
	 * Returns the version of the Ant Media Server
	 * 
	 * TO DO: Change endpoint from /broadcast/getVersion to /getVersion 
	 * @return {@link io.antmedia.rest.model.Version}
	 */
	@GET
	@Path("/broadcast/getVersion")
	@Produces(MediaType.APPLICATION_JSON)
	public Version getVersion() {
		return getSoftwareVersion();
	}


	/**
	 * Get the total number of broadcasts
	 * 
	 * @return the number of total broadcasts
	 */

	@GET
	@Path("/broadcast/getTotalBroadcastNumber")
	@Produces(MediaType.APPLICATION_JSON)
	public long getTotalBroadcastNumber() {
		return getDataStore().getTotalBroadcastNumber();
	}

	/**
	 * Return the statistics of the  total live streams, total RTMP watchers, total HLS and total
	 * WebRTC watchers
	 * 
	 * @return {@link LiveStatistics}
	 */
	@GET
	@Path("/broadcast/getAppLiveStatistics")
	@Produces(MediaType.APPLICATION_JSON)
	public LiveStatistics getAppLiveStatistics() {
		long activeBroadcastCount = getDataStore().getActiveBroadcastCount();
		return new LiveStatistics(activeBroadcastCount);
	}

	/**
	 * Generates random one-time token for specified stream
	 * @param streamId - the id of the stream
	 * @param expireDate - the expire date of the token
	 * @param type - type of the token (publish/play)
	 * @param roomName - room Name that token belongs to)
	 * @return  {@link io.antmedia.datastore.db.types.Token}
	 */
	@GET
	@Path("/broadcast/getToken")
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public Object getToken (@ApiParam(value = "the id of the stream", required = true) @QueryParam("id")String streamId,
			@ApiParam(value = "the expire date of the token", required = true) @QueryParam("expireDate") long expireDate,
			@ApiParam(value = "type of the token. It may be play or publish ", required = true) @QueryParam("type") String type,
			@ApiParam(value = "room Name that token belongs to ", required = true) @QueryParam("roomId") String roomId) 
 
	{
		return super.getToken(streamId, expireDate, type, roomId);
	}


	/**
	 * Perform validation of token for requested stream
	 * @param token - sent token for validation
	 * @return validated token {@link io.antmedia.datastore.db.types.Token}, either null or token. Null means not validated
	 */
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/broadcast/validateToken")
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public Token validateToken (@ApiParam(value = "token to be validated", required = true) Token token) {
		return super.validateToken(token);		
	}


	/**
	 * Removes all tokens related with requested stream
	 * @param streamId - the id of the stream
	 * @return {@link io.antmedia.rest.BroadcastRestService.Result}
	 */
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/broadcast/revokeTokens")
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public Result revokeTokens (@ApiParam(value = "the id of the stream", required = true) @QueryParam("id") String streamId) {
		return super.revokeTokens(streamId);
	}


	/**
	 * Get the all tokens of requested stream
	 * @param streamId - the id of the stream
	 * @param offset - the starting point of the list
	 * @param size - size of the return list (max:50 )
	 * @return token list of stream,  if no active tokens then returns null
	 */
	@GET
	@Path("/broadcast/listTokens/{streamId}/{offset}/{size}")
	@Produces(MediaType.APPLICATION_JSON)
	public List<Token> listTokens (@ApiParam(value = "the id of the stream", required = true) @PathParam("streamId") String streamId,
			@ApiParam(value = "the starting point of the list", required = true) @PathParam("offset") int offset,
			@ApiParam(value = "size of the return list (max:50 )", required = true) @PathParam("size") int size) {
		List<Token> tokens = null;

		if(streamId != null) {

			tokens = getDataStore().listAllTokens(streamId, offset, size);
		}

		return tokens;
	}

	/**
	 * Get the broadcast live statistics total RTMP watcher count, total HLS
	 * watcher count, total WebRTC watcher count
	 * 
	 * Return -1 for the values that is n/a
	 * 
	 * @param streamId - the id of the stream
	 * @return {@link BroadcastStatistics} if broadcast exists, null or 204(no
	 *         content) if no broadcast exists with that id
	 */
	@GET
	@Path("/broadcast/getBroadcastLiveStatistics")
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public BroadcastStatistics getBroadcastStatistics(@ApiParam(value = "the id of the stream", required = true) @QueryParam("id") String id) {
		return super.getBroadcastStatistics(id);
	}


	/**
	 * Get WebRTC Client Statistics such as : Audio bitrate, Video bitrate, Target bitrate, Video Sent Period etc.
	 * 
	 * @param streamId - the id of the stream
	 * @return the list of {@link io.antmedia.rest.WebRTCClientStats }
	 */

	@GET
	@Path("/broadcast/getWebRTCClientStats/{stream_id}")
	@Produces(MediaType.APPLICATION_JSON)
	public List<WebRTCClientStats> getWebRTCClientStats(@ApiParam(value = "the id of the stream", required = true) @PathParam("stream_id") String streamId) {
		IWebRTCAdaptor webRTCAdaptor = getWebRTCAdaptor();
		if (webRTCAdaptor != null) {
			return webRTCAdaptor.getWebRTCClientStats(streamId);
		}
		return new ArrayList<>();
	}


	/**
	 * Get WebRTC Client Statistics such as : Audio bitrate, Video bitrate, Target bitrate, Video Sent Period etc.
	 * 
	 * @param offset
	 *            This is the offset of the list, it is useful for pagination,
	 *            
	 * @param size
	 *            Number of items that will be fetched. If there is not enough
	 *            item in the WebRTCClientStats, returned list size may less then this
	 *            value
	 * 
	 * @param streamId - the id of the stream
	 * @return the list of {@link io.antmedia.rest.WebRTCClientStats }
	 */

	@GET
	@Path("/broadcast/getWebRTCClientStatsList/{offset}/{size}/{stream_id}")
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public List<WebRTCClientStats> getWebRTCClientStatsList(@ApiParam(value = "offset of the list", required = true) @PathParam("offset") int offset,
			@ApiParam(value = "Number of items that will be fetched", required = true) @PathParam("size") int size,
			@ApiParam(value = "the id of the stream", required = true) @PathParam("stream_id") String streamId) {

		return super.getWebRTCClientStatsList(offset, size, streamId);
	}


	/**
	 * Get filtered broadcast list
	 * @param offset - starting point of the list
	 * @param size - size of the return list (max:50 )
	 * @param type - type of the stream
	 * @return list of the broadcast objects
	 */
	@GET
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/broadcast/filterList/{offset}/{size}/{type}")
	@Produces(MediaType.APPLICATION_JSON)
	public List<Broadcast> filterBroadcastList(@ApiParam(value = "starting point of the list", required = true) @PathParam("offset") int offset,
			@ApiParam(value = "size of the return list (max:50 )", required = true) @PathParam("size") int size,
			@ApiParam(value = "type of the stream possible values are \"liveStream\", \"ipCamera\", \"streamSource\", \"VoD\"", required = true) @PathParam("type") String type) {
		return getDataStore().filterBroadcastList(offset, size, type);
	}


	/**
	 * Delete specific VoD File
	 * Deprecated -> Use deleteVoD method (/broadcast/deleteVoD/{id})
	 * 
	 * @param fileName- name of the VoD file
	 * @param id - id of the VoD file
	 * @param type -type of the VoD file
	 * @return {@link io.antmedia.rest.BroadcastRestService.Result}
	 */
	@POST
	@Consumes({ MediaType.APPLICATION_JSON })
	@Path("/broadcast/deleteVoDFile/{name}/{id}/{type}")
	@Produces(MediaType.APPLICATION_JSON)
	public Result deleteVoDFile(@ApiParam(value = "name", required = true) @PathParam("name") String fileName,
			@ApiParam(value = "id", required = true) @PathParam("id") String id,
			@ApiParam(value = "type", required = true) @PathParam("type") String type) {
		return deleteVoD(id);
	}


	/**
	 * Delete specific VoD File
	 * 
	 * @param id - the id of the VoD file
	 * @return {@link io.antmedia.rest.BroadcastRestService.Result}
	 */
	@POST
	@Consumes({ MediaType.APPLICATION_JSON })
	@Path("/broadcast/deleteVoD/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public Result deleteVoD(@ApiParam(value = "the id of the VoD file", required = true) @PathParam("id") String id) {
		return super.deleteVoD(id);
	}

	/**
	 * Upload external user VoD file to Ant Media Server
	 * 
	 * @param fileName - the name of the VoD File
	 * @param inputStream - the input stream of VoD file
	 * @return {@link io.antmedia.rest.BroadcastRestService.Result}
	 */

	@POST
	@Consumes({MediaType.MULTIPART_FORM_DATA})
	@Path("/broadcast/uploadVoDFile/{name}")
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public Result uploadVoDFile(@ApiParam(value = "the name of the VoD File", required = true) @PathParam("name") String fileName,
			@ApiParam(value = "file", required = true) @FormDataParam("file") InputStream inputStream) {
		return super.uploadVoDFile(fileName, inputStream);
	}


	/**
	 * Delete broadcast from data store
	 * 
	 * 
	 * @param id
	 *            Id of the broadcast
	 * 
	 * @return {@link io.antmedia.rest.BroadcastRestService.Result}
	 * 
	 */
	@POST
	@Consumes({ MediaType.APPLICATION_JSON })
	@Path("/broadcast/delete/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public Result deleteBroadcast(@ApiParam(value = " Id of the braodcast", required = true) @PathParam("id") String id) {
		return super.deleteBroadcast(id);
	}

	/**
	 * Get device parameters for social network authorization.
	 * 
	 * @param serviceName
	 *            Name of the service, like Facebook, Youtube, Periscope
	 * 
	 * @return If operation is successful, DeviceAuthParameters is returned
	 *         with related information.
	 * 
	 *         User should go to
	 *         {@link io.antmedia.social.endpoint.VideoServiceEndpoint.DeviceAuthParameters#verification_url}
	 *         and enter
	 *         {@link io.antmedia.social.endpoint.VideoServiceEndpoint.DeviceAuthParameters#user_code}
	 *         in a minute
	 * 
	 *         If not successful, it returns with Result object with message
	 * 
	 */
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/broadcast/getDeviceAuthParameters/{serviceName}")
	@Produces(MediaType.APPLICATION_JSON)
	public Object getDeviceAuthParameters(@ApiParam(value = "Name of the service, like Facebook, Youtube, Periscope", required = true) @PathParam("serviceName") String serviceName) {
		return super.getDeviceAuthParameters(serviceName);
	}

	/**
	 * Check if device is authenticated in the social network. In authorization
	 * phase, this function may be polled periodically until it returns success.
	 * Server checks social network service for about 1 minute so that if user
	 * does not enter DeviceAuthParameters in a 1 minute, this function will
	 * never return true
	 * 
	 * @param userCode Code of social media account
	 *          
	 * 
	 * @return {@link io.antmedia.rest.BroadcastRestService.Result} object with success field. If success field is true, it is
	 *         authenticated if false, not authenticated
	 * 
	 */
	@POST
	@Consumes({ MediaType.APPLICATION_JSON })
	@Path("/broadcast/checkDeviceAuthStatus/{userCode}")
	@Produces(MediaType.APPLICATION_JSON)
	public Result checkDeviceAuthStatus(@ApiParam(value = "Code of social media account", required = true) @PathParam("userCode") String userCode) {
		return super.checkDeviceAuthStatus(userCode);
	}

	/**
	 * Get Credentials of Social Endpoints
	 * 
	 * @param offset - the starting point of the list
	 * @param size - size of the return list (max:50 )
	 * @return - list of {@link io.antmedia.datastore.db.types.SocialEndpointCredentials}
	 */
	@GET
	@Consumes({ MediaType.APPLICATION_JSON })
	@Path("/broadcast/getSocialEndpoints/{offset}/{size}")
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public List<SocialEndpointCredentials> getSocialEndpoints(@ApiParam(value = "the starting point of the list", required = true) @PathParam("offset") int offset,
			@ApiParam(value = "size of the return list (max:50 )", required = true) @PathParam("size") int size) {
		return super.getSocialEndpoints(offset, size);
	}


	/**
	 * Some social networks have different channels especially for facebook,
	 * Live stream can be published on Facebook Page or Personal account, this
	 * service returns the related information about that.
	 * 
	 * @param endpointId
	 * 
	 * @return {@link io.antmedia.datastore.db.types.SocialEndpointChannel}
	 * 
	 */
	@GET
	@Consumes({ MediaType.APPLICATION_JSON })
	@Path("/broadcast/getSocialNetworkChannel/{endpointId}")
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public SocialEndpointChannel getSocialNetworkChannel(@ApiParam(value = "endpointId", required = true) @PathParam("endpointId") String endpointId) {
		return super.getSocialNetworkChannel(endpointId);
	}

	/**
	 * Returns available social network channels for the specific service
	 * 
	 * @param endpointId
	 * 
	 * @param type
	 *            This is very service specific, it may be page for Facebook
	 * 
	 * @return List of
	 *         {@link io.antmedia.datastore.db.types.SocialEndpointChannel}
	 * 
	 */
	@GET
	@Consumes({ MediaType.APPLICATION_JSON })
	@Path("/broadcast/getSocialNetworkChannelList/{endpointId}/{type}")
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public List<SocialEndpointChannel> getSocialNetworkChannelList(@ApiParam(value = "endpointId", required = true) @PathParam("endpointId") String endpointId,
			@ApiParam(value = "This is very service specific, it may be page for Facebook", required = true) @PathParam("type") String type) {
		return super.getSocialNetworkChannelList(endpointId, type);
	}

	/**
	 * If there are multiple channels in a social network,
	 * this method sets specific channel for that endpoint
	 * 
	 * If a user has pages in Facebook, this method sets the specific page to publish live stream to
	 * 
	 * @return {@link io.antmedia.rest.BroadcastRestService.Result}
	 * 
	 */
	@POST
	@Consumes({ MediaType.APPLICATION_JSON })
	@Path("/broadcast/setSocialNetworkChannel/{endpointId}/{type}/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public Result setSocialNetworkChannelList(@ApiParam(value = "endpointId", required = true) @PathParam("endpointId") String endpointId,
			@ApiParam(value = "type", required = true) @PathParam("type") String type,
			@ApiParam(value = "id", required = true) @PathParam("id") String channelId) {
		return super.setSocialNetworkChannelList(endpointId, type, channelId);
	}

	public long getRecordCount() {
		return getDataStore().getBroadcastCount();
	}

	
    /**
     * Set stream specific Mp4 Muxing setting, this setting overrides general Mp4 Muxing Setting
     *
     * @param streamId  - the id of the stream
     * @param enableMp4 - the integer value for Mp4 Muxing, 1 = Enable Muxing, -1 = Disable Muxing, 0 = No Settings
     * @return {@link io.antmedia.rest.BroadcastRestService.Result}
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/broadcast/enableMp4Muxing")
    @Produces(MediaType.APPLICATION_JSON)
    public Result enableMp4Muxing(@ApiParam(value = "the id of the stream", required = true) @QueryParam("id") String streamId,
                                  @ApiParam(value = "the integer value for Mp4 Muxing, 1 = Enable Muxing, -1 = Disable Muxing, 0 = No Settings", required = true) @QueryParam("enableMp4") int enableMp4) {
        Result result = new Result(false);
        if (streamId != null) {

            if (getDataStore().setMp4Muxing(streamId, enableMp4)) {
                if (MP4_ENABLE == enableMp4) {
                    startMp4Muxing(streamId);
                } else if (MP4_DISABLE == enableMp4) {
                    stopMp4Muxing(streamId);
                }
                result.setSuccess(true);
                result.setMessage("streamId:" + streamId);
            } else {
                result.setMessage("no stream for this id: " + streamId + " or wrong setting parameter");
            }
        }

        return result;
    }

    
}
