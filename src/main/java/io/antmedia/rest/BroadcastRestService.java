package io.antmedia.rest;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.jar.Manifest;
import java.util.regex.Pattern;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.red5.server.api.scope.IBroadcastScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.AppSettings;
import io.antmedia.datastore.db.DataStore;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.datastore.db.types.ConferenceRoom;
import io.antmedia.datastore.db.types.Endpoint;
import io.antmedia.datastore.db.types.SocialEndpointChannel;
import io.antmedia.datastore.db.types.SocialEndpointCredentials;
import io.antmedia.datastore.db.types.TensorFlowObject;
import io.antmedia.datastore.db.types.Token;
import io.antmedia.datastore.db.types.VoD;
import io.antmedia.muxer.Muxer;
import io.antmedia.rest.model.Interaction;
import io.antmedia.rest.model.Result;
import io.antmedia.rest.model.Version;
import io.antmedia.security.ITokenService;
import io.antmedia.social.LiveComment;
import io.antmedia.social.endpoint.PeriscopeEndpoint;
import io.antmedia.social.endpoint.VideoServiceEndpoint;
import io.antmedia.social.endpoint.VideoServiceEndpoint.DeviceAuthParameters;
import io.antmedia.storage.StorageClient;
import io.antmedia.storage.StorageClient.FileType;
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

@Api(value = "BroadcastRestService")
@SwaggerDefinition(
		info = @Info(
				description = "Ant Media Server REST API Reference",
				version = "V1.0",
				title = "Ant Media Server REST API Reference",
				contact = @Contact(name = "Ant Media Info", email = "contact@antmedia.io", url = "https://antmedia.io"),
				license = @License(name = "Apache 2.0", url = "http://www.apache.org")),
		consumes = {"application/json" },
		produces = {"application/json" },
		schemes = {SwaggerDefinition.Scheme.HTTP, SwaggerDefinition.Scheme.HTTPS},
		externalDocs = @ExternalDocs(value = "External Docs", url = "https://antmedia.io"),
		basePath = "/"
		)
@Component
@Path("/")
public class BroadcastRestService extends RestServiceBase{

	/**
	 * Key for Manifest entry of Build number. It should match with the value in pom.xml
	 */
	private static final String BUILD_NUMBER = "Build-Number";

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

	public static final String ENTERPRISE_EDITION = "Enterprise Edition";
	public static final String COMMUNITY_EDITION = "Community Edition";
	public static final int MP4_ENABLE = 1;
	public static final int MP4_DISABLE = -1;
	public static final int MP4_NO_SET = 0;

	private AppSettings appSettings;

	public interface ProcessBuilderFactory {
		Process make(String...args);
	}

	private ProcessBuilderFactory processBuilderFactory = null;

	protected static Logger logger = LoggerFactory.getLogger(BroadcastRestService.class);
	private static String hostaddress;


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
	@ApiOperation(value = "Creates a broadcast and returns the full broadcast object with rtmp address and other information.", notes = "Notes here", response = Broadcast.class)
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
	@ApiOperation(value = "Creates a broadcast without reset StreamID and returns the full broadcast object with rtmp address and other information.", notes = "Ant Media Server does not use this rest service by default", response = Broadcast.class)
	@POST
	@Consumes({ MediaType.APPLICATION_JSON })
	@Path("/broadcast/createWithStreamID")
	@Produces(MediaType.APPLICATION_JSON)
	public Broadcast createBroadcastWithStreamID(@ApiParam(value = "Broadcast object only related information should be set, it may be null as well.", required = true) Broadcast broadcast) {

		String settingsListenerHookURL = null; 
		String fqdn = null;
		AppSettings appSettingsLocal = getAppSettings();
		if (appSettingsLocal != null) {
			settingsListenerHookURL = appSettingsLocal.getListenerHookURL();
			fqdn = appSettingsLocal.getServerName();
		}

		return saveBroadcast(broadcast, AntMediaApplicationAdapter.BROADCAST_STATUS_CREATED, getScope().getName(),
				getDataStore(), settingsListenerHookURL, fqdn);
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
	@ApiOperation(value = "Creates a conference room with the parameters. The room name is key so if this is called with the same room name then new room is overwritten to old one", response = ConferenceRoom.class)
	@POST
	@Consumes({ MediaType.APPLICATION_JSON })
	@Path("/broadcast/createConferenceRoom")
	@Produces(MediaType.APPLICATION_JSON)
	public ConferenceRoom createConferenceRoom(@ApiParam(value = "Conference Room object with start and end date", required = true) ConferenceRoom room) {

		if(room != null) {

			Calendar calendar = Calendar.getInstance();
			
	        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd hh:mm:ss");  			

			if(room.getStartDate() == null) {
				room.setStartDate(dateFormat.format(calendar.getTime()));
			}

			if(room.getEndDate() == null) {
				calendar.add(Calendar.HOUR, 1);
				room.setEndDate(dateFormat.format(calendar.getTime()));
			}

			if (getDataStore().createConferenceRoom(room)) {
				return room;
			}
		}
		return null;
	}

	/**
	 * Edits previously saved conference room
	 * @param room Conference Room object with start and end date
	 * 
	 * @return {@link io.antmedia.datastore.db.types.ConferenceRoom}
	 * 
	 */
	@ApiOperation(value = "Edits previously saved conference room", response = ConferenceRoom.class)
	@POST
	@Consumes({ MediaType.APPLICATION_JSON })
	@Path("/broadcast/editConferenceRoom")
	@Produces(MediaType.APPLICATION_JSON)
	public ConferenceRoom editConferenceRoom(@ApiParam(value = "Conference Room object with start and end date", required = true) ConferenceRoom room) {

		if(room != null) {

			if (getDataStore().editConferenceRoom(room)) {
				return room;
			}
		}
		return null;
	}

	/**
	 * Deletes previously saved conference room 
	 * @param roomName the name of the conference room
	 * 
	 * @return true if successfully deleted, false if not 
	 * 
	 */
	@ApiOperation(value = "Creates a conference room with the parameters. The room name is key so if this is called with the same room name then new room is overwritten to old one", response = ConferenceRoom.class)
	@POST
	@Consumes({ MediaType.APPLICATION_JSON })
	@Path("/broadcast/deleteConferenceRoom")
	@Produces(MediaType.APPLICATION_JSON)
	public boolean deleteConferenceRoom(@ApiParam(value = "the name of the conference room", required = true) @QueryParam("roomName") String roomName) {

		if(roomName != null) {
			return getDataStore().deleteConferenceRoom(roomName);
		}
		return false;
	}


	/**
	 * Use createBroadcast with listenerHookURL
	 * @param name
	 * @param listenerHookURL
	 * @return
	 * 
	 * deprecated use createBroadcast with listenerHookURL , it will be deleted.
	 */
	@ApiOperation(value = "Use createBroadcast with listenerHookURL", notes = "Notes here", response = Broadcast.class)
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
			fqdn = appSettingsLocal.getServerName();
		}

		return saveBroadcast(broadcast, AntMediaApplicationAdapter.BROADCAST_STATUS_CREATED, getScope().getName(),
				getDataStore(), settingsListenerHookURL, fqdn);
	}



	public static Broadcast saveBroadcast(Broadcast broadcast, String status, String scopeName, DataStore dataStore,
			String settingsListenerHookURL, String fqdn) {

		if (broadcast == null) {
			broadcast = new Broadcast();
		}
		broadcast.setStatus(status);
		broadcast.setDate(System.currentTimeMillis());

		String listenerHookURL = broadcast.getListenerHookURL();

		if ((listenerHookURL == null || listenerHookURL.isEmpty()) 
				&& settingsListenerHookURL != null && !settingsListenerHookURL.isEmpty()) {

			broadcast.setListenerHookURL(settingsListenerHookURL);
		}

		if (fqdn == null || fqdn.length() == 0) {
			fqdn = getHostAddress(); 
		}

		if (fqdn != null && fqdn.length() >= 0) {
			broadcast.setRtmpURL("rtmp://" + fqdn + "/" + scopeName + "/");
		}

		dataStore.save(broadcast);
		return broadcast;
	}

	private static String getHostAddress() {
		
		if (hostaddress == null) {
			long startTime = System.currentTimeMillis();
			try {
				/*
				 * InetAddress.getLocalHost().getHostAddress() takes long time(5sec in macos) to return.
				 * Let it is run once
				 */
				hostaddress = InetAddress.getLocalHost().getHostAddress();
			} catch (UnknownHostException e) {
				logger.error(ExceptionUtils.getStackTrace(e));
			}
			long diff = System.currentTimeMillis() - startTime;
			if (diff > 1000) {
				logger.warn("Getting host adress took {}ms. it's cached now and will return immediately from now on. You can "
						+ " alternatively set serverName in conf/red5.properties file ", diff);
			}
		}
		
		
		return hostaddress;
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
	@ApiOperation(value = "Create broadcast and bind social networks at the same time Server should be authorized in advance to make this service return success", notes = "Notes here", response = Broadcast.class)
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

	@ApiOperation(value = "Stops broadcasting of requested stream", notes = "", response = Result.class)
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

	private boolean stopBroadcastInternal(Broadcast broadcast) {
		boolean result = false;
		if (broadcast != null) {
			result = getApplication().stopStreaming(broadcast).isSuccess(); 
			if (result) {
				logger.info("broadcast is stopped streamId: {}", broadcast.getStreamId());
			}
			else {
				logger.error("No active broadcast found with id {}, so could not stopped", broadcast.getStreamId());
			}
		}
		return result;
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
	@ApiOperation(value = "Updates the properties of the broadcast", notes = "", response = Result.class)
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/broadcast/update")
	@Produces(MediaType.APPLICATION_JSON)
	public Result updateBroadcast(@ApiParam(value = "Broadcast object", required = true) Broadcast broadcast,
			@ApiParam(value = "Comma separated social network IDs, they must in comma separated and IDs must match with the defined IDs", required = true) @QueryParam("socialNetworks") String socialNetworksToPublish) {

		boolean result = getDataStore().updateName(broadcast.getStreamId(), broadcast.getName(),
				broadcast.getDescription());
		StringBuilder message = new StringBuilder();
		int errorId = 0;
		if (result) {
			Broadcast fetchedBroadcast = getDataStore().get(broadcast.getStreamId());
			getDataStore().removeAllEndpoints(fetchedBroadcast.getStreamId());

			if (socialNetworksToPublish != null && socialNetworksToPublish.length() > 0) {
				String[] socialNetworks = socialNetworksToPublish.split(",");

				for (String networkName : socialNetworks) {
					Result addSocialEndpoint = addSocialEndpoint(broadcast.getStreamId(), networkName);
					if (!addSocialEndpoint.isSuccess()) {
						result = false;
						message.append(networkName).append(" ");
						errorId = -1;
						break;
					}
				}
			}
		}
		if (message.length() > 0) {
			message.append(" endpoint cannot be added");
		}
		return new Result(result, message.toString(), errorId);
	}

	/**
	 * Revokes authorization from a social network account that is authorized
	 * before
	 * 
	 * @param endpointId the social network endpoint id of the social network account
	 * @return {@link io.antmedia.rest.BroadcastRestService.Result}
	 */
	@ApiOperation(value = "Revoke authorization from a social network account that is authorized before", notes = "", response = Result.class)
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/broadcast/revokeSocialNetwork/{endpointId}")
	@Produces(MediaType.APPLICATION_JSON)
	public Result revokeSocialNetwork(@ApiParam(value = "Endpoint id", required = true) @PathParam("endpointId") String endpointId) {
		Map<String, VideoServiceEndpoint> endPointServiceMap = getEndpointList();
		String message = null;
		boolean result = false;
		if (endPointServiceMap != null) {

			VideoServiceEndpoint videoServiceEndpoint = endPointServiceMap.get(endpointId);
			if (videoServiceEndpoint != null) {
				videoServiceEndpoint.resetCredentials();
				endPointServiceMap.remove(endpointId);
				result = true;
			}
			else {
				message = "Service with the name specified is not found in this app";
			}
		} 
		else {
			message = "No endpoint is defined for this app";
		}
		return new Result(result, message);
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
	@ApiOperation(value = "Add social endpoint to a stream. ", notes = "", response = Result.class)
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
	@ApiOperation(value = "Add social endpoint to a stream. Use the JSON version of this method ", notes = "", response = Result.class)
	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Path("/broadcast/addSocialEndpoint")
	@Produces(MediaType.APPLICATION_JSON)
	public Result addSocialEndpoint(@ApiParam(value = "Stream id", required = true) @FormParam("id") String id,

			@ApiParam(value = "the id of the service in order to have successfull operation. Social network must be authorized in advance", required = true)
	@FormParam("serviceName") String endpointServiceId) {

		Broadcast broadcast = lookupBroadcast(id);

		boolean success = addSocialEndpoints(broadcast, endpointServiceId);
		String message = "";
		if(!success) {
			message  = endpointServiceId+" endpoint can not be added to "+id;
		}

		return new Result(success, message);
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
	@ApiOperation(value = "Add a third pary rtmp end point to the stream. When broadcast is started,it will send rtmp stream to this rtmp url as well. ", notes = "", response = Result.class)
	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Path("/broadcast/addEndpoint")
	@Produces(MediaType.APPLICATION_JSON)
	public Result addEndpoint(@ApiParam(value = "Broadcast id", required = true) @FormParam("id") String id,
			@ApiParam(value = "RTMP url of the endpoint that stream will be republished", required = true) @FormParam("rtmpUrl") String rtmpUrl) {
		boolean success = false;
		String message = null;
		try {
			Endpoint endpoint = new Endpoint();
			endpoint.setRtmpUrl(rtmpUrl);
			endpoint.type = "generic";

			success = getDataStore().addEndpoint(id, endpoint);
		} catch (Exception e) {
			logger.error(ExceptionUtils.getStackTrace(e));
		}

		return new Result(success, message);
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
	@ApiOperation(value = "Returns live comments from a specific endpoint like Facebook, Youtube, PSCP, etc. It works If interactivity is collected which can be enabled/disabled by properties file.", notes = "Notes here", responseContainer = "List", response = LiveComment.class)
	@GET
	@Path("/broadcast/getLiveComments/{endpointServiceId}/{streamId}/{offset}/{batch}")
	@Produces(MediaType.APPLICATION_JSON)
	public List<LiveComment> getLiveCommentsFromEndpoint(@ApiParam(value = "This is the id of the endpoint service", required = true)
	@PathParam("endpointServiceId") String endpointServiceId,
	@ApiParam(value = "Stream id", required = true)
	@PathParam("streamId") String streamId,
	@ApiParam(value = "this is the start offset where to start getting comment", required = true)
	@PathParam("offset") int offset,
	@ApiParam(value = "number of items to be returned", required = true)
	@PathParam("batch") int batch) {

		VideoServiceEndpoint videoServiceEndPoint = getApplication().getVideoServiceEndPoint(endpointServiceId);
		List<LiveComment> liveComment = null;
		if (videoServiceEndPoint != null) {
			liveComment = videoServiceEndPoint.getComments(streamId, offset, batch);
		}
		return liveComment;
	}

	/**
	 * Return the number of live views in specified video service endpoint
	 * It works If interactivity is collected which can be enabled/disabled by properties file.
	 * 
	 * @param endpointServiceId- the id of the endpoint
	 * @param streamId- the id of the stream
	 * @return {@link io.antmedia.rest.BroadcastRestService.Result}
	 */
	@ApiOperation(value = "Return the number of live views in specified video service endpoint. It works If interactivity is collected which can be enabled/disabled by properties file.", notes = "", response = Result.class)
	@GET
	@Path("/broadcast/getLiveViewsCount/{endpointServiceId}/{streamId}")
	@Produces(MediaType.APPLICATION_JSON)
	public Result getViewerCountFromEndpoint(@ApiParam(value = "the id of the endpoint", required = true)
	@PathParam("endpointServiceId") String endpointServiceId,
	@ApiParam(value = "the id of the stream", required = true)
	@PathParam("streamId") String streamId) {
		VideoServiceEndpoint videoServiceEndPoint = getApplication().getVideoServiceEndPoint(endpointServiceId);
		long liveViews = 0;
		if (videoServiceEndPoint != null) {
			liveViews = videoServiceEndPoint.getLiveViews(streamId);
		}
		return new Result(true, String.valueOf(liveViews));
	}


	/**
	 * Returns the number of live comment count in a specific video service endpoint
	 * It works If interactivity is collected which can be enabled/disabled by properties file.
	 * 
	 * @param endpointServiceId- the id of the endpoint
	 * @param streamId- the id of the stream
	 * @return @return {@link io.antmedia.rest.BroadcastRestService.Result}
	 */
	@ApiOperation(value = "Returns the number of live comment count from a specific video service endpoint. It works If interactivity is collected which can be enabled/disabled by properties file.", notes = "", response = Result.class)
	@GET
	@Path("/broadcast/getLiveCommentsCount/{endpointServiceId}/{streamId}")
	@Produces(MediaType.APPLICATION_JSON)
	public Result getLiveCommentsCount(@ApiParam(value = " the id of the endpoint", required = true) @PathParam("endpointServiceId") String endpointServiceId,
			@ApiParam(value = "the id of the stream", required = true)  @PathParam("streamId") String streamId) {
		VideoServiceEndpoint videoServiceEndPoint = getApplication().getVideoServiceEndPoint(endpointServiceId);
		int commentCount = 0;
		if (videoServiceEndPoint != null) {
			commentCount = videoServiceEndPoint.getTotalCommentsCount(streamId);
		}
		return new Result(true, String.valueOf(commentCount));
	}

	/**
	 * Return the interaction from a specific endpoint like facebook, youtube, pscp, etc. 
	 * It works If interactivity is collected which can be enabled/disabled by properties file.
	 * 
	 * @param endpointServiceId- the id of the endpoint
	 * @param streamId- the id of the stream
	 * @return {@link io.antmedia.rest.model.Interaction }
	 */
	@ApiOperation(value = "Return the interaction from a specific endpoint like Facebook, Youtube, PSCP, etc. It works If interactivity is collected which can be enabled/disabled by properties file.", notes = "", response = Interaction.class)
	@GET
	@Path("/broadcast/getInteraction/{endpointServiceId}/{streamId}")
	@Produces(MediaType.APPLICATION_JSON)
	public Interaction getInteractionFromEndpoint(@ApiParam(value = "the id of the endpoint", required = true) @PathParam("endpointServiceId") String endpointServiceId,
			@ApiParam(value = "the id of the stream", required = true) @PathParam("streamId") String streamId) {
		Interaction interaction = null;
		VideoServiceEndpoint videoServiceEndPoint = getApplication().getVideoServiceEndPoint(endpointServiceId);
		if (videoServiceEndPoint != null) {
			interaction = videoServiceEndPoint.getInteraction(streamId);
		}
		return interaction;
	}



	protected Broadcast lookupBroadcast(String id) {
		Broadcast broadcast = null;
		try {
			broadcast = getDataStore().get(id);
		} catch (Exception e) {
			logger.error(ExceptionUtils.getStackTrace(e));
		}
		return broadcast;
	}

	/**
	 * Get broadcast object
	 * 
	 * @param id - id of the broadcast
	 * 
	 * @return broadcast object nothing if broadcast is not found
	 * 
	 */
	@ApiOperation(value = "Get broadcast object", notes = "", response = Broadcast.class)
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
	@ApiOperation(value = "VoD file from database", notes = "", response = VoD.class)
	@GET
	@Path("/broadcast/getVoD")
	@Produces(MediaType.APPLICATION_JSON)
	public VoD getVoD(@ApiParam(value = "id of the VoD", required = true) @QueryParam("id") String id) {
		VoD vod = null;
		if (id != null) {
			vod = getDataStore().getVoD(id);
		}
		if (vod == null) {
			vod = new VoD();
		}
		return vod;
	}
	
	/**
	 * Get detected objects from the stream
	 * 
	 * @param id - the id of the stream 
	 * 
	 * @return  the list of TensorFlowObject objects {@link io.antmedia.datastore.db.types.TensorFlowObject}
	 */
	@ApiOperation(value = "Get Detected objects", notes = "",responseContainer = "List", response = TensorFlowObject.class)
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

	@ApiOperation(value = "Get detected objects from the stream based on offset and size", notes = "",responseContainer = "List", response = TensorFlowObject.class)
	@GET
	@Path("/detection/getList/{offset}/{size}")
	@Produces(MediaType.APPLICATION_JSON)
	public List<TensorFlowObject> getDetectionList(@ApiParam(value = "the id of the stream", required = true) @QueryParam("id") String id,
			@ApiParam(value = "starting point of the list", required = true) @PathParam("offset") int offset,
			@ApiParam(value = "total size of the return list", required = true) @PathParam("size") int size) {
		List<TensorFlowObject> list = null;

		if (id != null) {
			list = getDataStore().getDetectionList(id, offset, size);	
		}

		if (list == null) {
			//do not return null in rest service
			list = new ArrayList<>();
		}


		return list;
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
	@ApiOperation(value = "Get total number of detected objects", notes = "", response = Long.class)
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
	@ApiOperation(value = "Gets the broadcast list from database", notes = "",responseContainer = "List", response = Broadcast.class)
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

	@ApiOperation(value = "Import Live Streams to Stalker Portal", notes = "", response = Result.class)
	@POST
	@Path("/importLiveStreamsToStalker")
	@Produces(MediaType.APPLICATION_JSON)
	public Result importLiveStreams2Stalker() 
	{

		String stalkerDBServer = getAppSettings().getStalkerDBServer();
		String stalkerDBUsername = getAppSettings().getStalkerDBUsername();
		String stalkerDBPassword = getAppSettings().getStalkerDBPassword();

		boolean result = false;
		String message = "";
		int errorId = -1;
		if (stalkerDBServer != null && stalkerDBServer.length() > 0
				&& stalkerDBUsername != null && stalkerDBUsername.length() > 0
				&& stalkerDBPassword != null && stalkerDBPassword.length() > 0) 
		{


			long broadcastCount = getDataStore().getBroadcastCount();
			int pageCount = (int) broadcastCount/DataStore.MAX_ITEM_IN_ONE_LIST
					+ ((broadcastCount % DataStore.MAX_ITEM_IN_ONE_LIST != 0) ? 1 : 0);

			List<Broadcast> broadcastList = new ArrayList<>();
			for (int i = 0; i < pageCount; i++) {
				broadcastList.addAll(getDataStore().getBroadcastList(i*DataStore.MAX_ITEM_IN_ONE_LIST, DataStore.MAX_ITEM_IN_ONE_LIST));
			}

			StringBuilder insertQueryString = new StringBuilder();

			insertQueryString.append("DELETE FROM stalker_db.ch_links;");
			insertQueryString.append("DELETE FROM stalker_db.itv;");

			String fqdn = getAppSettings().getServerName();
			if (fqdn == null || fqdn.length() == 0) {
				fqdn = getHostAddress();
			}

			int number = 1;
			for (Broadcast broadcast : broadcastList) {
				String cmd = "ffmpeg http://"+ fqdn + ":5080/" 
						+ getScope().getName() + "/streams/"+broadcast.getStreamId()+".m3u8";

				insertQueryString.append("INSERT INTO stalker_db.itv(name, number, tv_genre_id, base_ch, cmd, languages)"
						+ " VALUES ('"+broadcast.getName()+"' , "+ number +", 2, 1, '"+ cmd +"', '');");

				insertQueryString.append("SET @last_id=LAST_INSERT_ID();"
						+ "INSERT INTO stalker_db.ch_links(ch_id, url)"
						+ " VALUES(@last_id, '"+ cmd +"');");
				number++;
			}
			result = runStalkerImportQuery(insertQueryString.toString(), stalkerDBServer, stalkerDBUsername, stalkerDBPassword);
		}
		else {
			message = "Portal DB info is missing";
			errorId = 404;
		}


		return new Result(result, message, errorId);
	}

	private boolean runStalkerImportQuery(String query, String stalkerDBServer, String stalkerDBUsername, String stalkerDBPassword) {

		boolean result = false;
		try {

			Process p = getProcess(query, stalkerDBServer, stalkerDBUsername, stalkerDBPassword);

			if (p != null) {
				InputStream is = p.getInputStream();
				if (is != null) {
					byte[] data = new byte[1024];
					int length;
					while ((length = is.read(data, 0, data.length)) != -1) {
						if (logger.isInfoEnabled()) {
							logger.info(new String(data, 0, length));
						}
					}
				}

				int exitWith = p.waitFor();

				if (exitWith == 0) {
					result = true;
				}	
			}

		} catch (IOException e) {
			logger.error(ExceptionUtils.getStackTrace(e));
		} catch (InterruptedException e) {
			logger.error(ExceptionUtils.getStackTrace(e));
			Thread.currentThread().interrupt();
		} 
		return result;
	}

	private Process getProcess(String query, String stalkerDBServer, String stalkerDBUsername, String stalkerDBPassword) {
		Process process = null;
		String mysqlClientPath = getAppSettings().getMySqlClientPath();
		if (processBuilderFactory != null) {

			process = processBuilderFactory.make(mysqlClientPath, 
					"-h", stalkerDBServer,
					"-u", stalkerDBUsername,
					"-p"+stalkerDBPassword,
					"-e",   query);
		}
		else {
			try {
				process = new ProcessBuilder(
						mysqlClientPath, 
						"-h", stalkerDBServer,
						"-u", stalkerDBUsername,
						"-p"+stalkerDBPassword,
						"-e",   query  
						).redirectErrorStream(true).start();
			} catch (IOException e) {
				logger.error(ExceptionUtils.getStackTrace(e));
			}
		}

		return process;

	}


	/**
	 * Import VoDs to Stalker Portal
	 * 
	 * @return {@link io.antmedia.rest.BroadcastRestService.Result}
	 */

	@ApiOperation(value = "Import VoDs to Stalker Portal", notes = "", response = Result.class)
	@POST
	@Path("/importVoDsToStalker")
	@Produces(MediaType.APPLICATION_JSON)
	public Result importVoDsToStalker() 
	{

		String stalkerDBServer = getAppSettings().getStalkerDBServer();
		String stalkerDBUsername = getAppSettings().getStalkerDBUsername();
		String stalkerDBPassword = getAppSettings().getStalkerDBPassword();

		boolean result = false;
		String message = "";
		int errorId = -1;
		if (stalkerDBServer != null && stalkerDBUsername != null && stalkerDBPassword != null) {

			String vodFolderPath = getAppSettings().getVodFolder();
			if (vodFolderPath != null && !vodFolderPath.isEmpty()) {

				long totalVodNumber = getDataStore().getTotalVodNumber();
				int pageCount = (int) totalVodNumber/DataStore.MAX_ITEM_IN_ONE_LIST 
						+ ((totalVodNumber % DataStore.MAX_ITEM_IN_ONE_LIST != 0) ? 1 : 0);

				List<VoD> vodList = new ArrayList<>();
				for (int i = 0; i < pageCount; i++) {
					vodList.addAll(getDataStore().getVodList(i*DataStore.MAX_ITEM_IN_ONE_LIST, DataStore.MAX_ITEM_IN_ONE_LIST));
				}


				String fqdn = getAppSettings().getServerName();
				if (fqdn == null || fqdn.length() == 0) {
					fqdn = getHostAddress();
				}

				StringBuilder insertQueryString = new StringBuilder();

				//delete all videos in stalker to import new ones
				insertQueryString.append("DELETE FROM stalker_db.video_series_files;");
				insertQueryString.append("DELETE FROM stalker_db.video;");

				for (VoD vod : vodList) {
					if (vod.getType().equals(VoD.USER_VOD)) {
						insertQueryString.append("INSERT INTO stalker_db.video(name, o_name, protocol, category_id, cat_genre_id_1, status, cost, path, accessed) "
								+ "values('"+ vod.getVodName() + "', '"+vod.getVodName()+"', '', 1, 1, 1, 0, '"+vod.getVodName()+"', 1);");

						File vodFolder = new File(vodFolderPath);
						int lastIndexOf = vod.getFilePath().lastIndexOf(vodFolder.getName());
						String filePath = vod.getFilePath().substring(lastIndexOf);
						String cmd = "ffmpeg http://"+ fqdn + ":5080/" 
								+ getScope().getName() + "/streams/" + filePath;

						insertQueryString.append("SET @last_id=LAST_INSERT_ID();");

						insertQueryString.append("INSERT INTO stalker_db.video_series_files"
								+ "(video_id, file_type, protocol, url, languages, quality, date_add, date_modify, status, accessed)"
								+ "VALUES(@last_id, 'video', 'custom', '"+cmd+"', 'a:1:{i:0;s:2:\"en\";}', 5, NOW(), NOW(), 1, 1);");

					}

				}

				result = runStalkerImportQuery(insertQueryString.toString(), stalkerDBServer, stalkerDBUsername, stalkerDBPassword );
			}
			else {
				message = "No VoD folder specified";
				errorId = 500;
			}
		}
		else {
			message = "Portal DB info is missing";
			errorId = 404;
		}

		return new Result(result, message, errorId);

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

	@ApiOperation(value = " Get the VoD list from database", notes = "", responseContainer = "List",response = VoD.class)
	@GET
	@Path("/broadcast/getVodList/{offset}/{size}")
	@Produces(MediaType.APPLICATION_JSON)
	public List<VoD> getVodList(@ApiParam(value = "offset of the list", required = true) @PathParam("offset") int offset,
			@ApiParam(value = "Number of items that will be fetched", required = true) @PathParam("size") int size) {
		return getDataStore().getVodList(offset, size);
	}

	/**
	 * Get the total number of VoDs
	 * 
	 * @return the number of total VoDs
	 */

	@ApiOperation(value = "Get the total number of VoDs", notes = "", response = Long.class)
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
	@ApiOperation(value = "", notes = "", response = Version.class)
	@GET
	@Path("/broadcast/getVersion")
	@Produces(MediaType.APPLICATION_JSON)
	public Version getVersion() {
		return getSoftwareVersion();
	}

	public static Version getSoftwareVersion() {
		Version version = new Version();
		version.setVersionName(AntMediaApplicationAdapter.class.getPackage().getImplementationVersion());

		URLClassLoader cl = (URLClassLoader) AntMediaApplicationAdapter.class.getClassLoader();
		URL url = cl.findResource("META-INF/MANIFEST.MF");
		Manifest manifest;
		try {
			manifest = new Manifest(url.openStream());
			version.setBuildNumber(manifest.getMainAttributes().getValue(BUILD_NUMBER));
		} catch (IOException e) {
			//No need to implement
		}

		version.setVersionType(BroadcastRestService.isEnterprise() ? ENTERPRISE_EDITION : COMMUNITY_EDITION);

		logger.debug("Version Name {} Version Type {}", version.getVersionName(), version.getVersionType());
		return version;
	}


	/**
	 * Get the total number of broadcasts
	 * 
	 * @return the number of total broadcasts
	 */

	@ApiOperation(value = "Get the total number of broadcasts", notes = "", response = Long.class)
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
	@ApiOperation(value = "Return the statistics of the  total live streams, total RTMP watchers, total HLS and total WebRTC watchers", notes = "", response = LiveStatistics.class)
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
	 * @return  {@link io.antmedia.datastore.db.types.Token}
	 */
	@ApiOperation(value = "Generates random one-time token for specified stream", notes = "", response = Token.class)
	@GET
	@Path("/broadcast/getToken")
	@Produces(MediaType.APPLICATION_JSON)
	public Object getToken (@ApiParam(value = "the id of the stream", required = true) @QueryParam("id")String streamId,
			@ApiParam(value = "the expire date of the token", required = true) @QueryParam("expireDate") long expireDate,
			@ApiParam(value = "type", required = true) @QueryParam("type") String type) 
	{
		Token token = null;
		String message = "No stream id";
		if(streamId != null) {

			ApplicationContext appContext = getAppContext();

			if(appContext != null && appContext.containsBean(ITokenService.BeanName.TOKEN_SERVICE.toString())) 
			{
				ITokenService tokenService = (ITokenService)appContext.getBean(ITokenService.BeanName.TOKEN_SERVICE.toString());
				token = tokenService.createToken(streamId, expireDate, type);
				if(token != null) 
				{
					if (getDataStore().saveToken(token)) {
						//return token only everthing is ok
						return token;
					}
					else {
						message = "Cannot save token to the datastore";
					}
				}
				else {
					message = "Cannot create token. It can be a mock token service";
				}
			}
			else {
				message = "No token service in this app";
			}
		}

		return new Result(false, message);
	}


	/**
	 * Perform validation of token for requested stream
	 * @param token - sent token for validation
	 * @return validated token {@link io.antmedia.datastore.db.types.Token}, either null or token. Null means not validated
	 */

	@ApiOperation(value = "Perform validation of token for requested stream", notes = "", response = Token.class)
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/broadcast/validateToken")
	@Produces(MediaType.APPLICATION_JSON)
	public Token validateToken (@ApiParam(value = "token to be validated", required = true) Token token) {
		Token validatedToken = null;

		if(token.getTokenId() != null) {

			validatedToken = getDataStore().validateToken(token);
		}

		return validatedToken;
	}


	/**
	 * Removes all tokens related with requested stream
	 * @param streamId - the id of the stream
	 * @return {@link io.antmedia.rest.BroadcastRestService.Result}
	 */
	@ApiOperation(value = " Removes all tokens related with requested stream", notes = "", response = Result.class)
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/broadcast/revokeTokens")
	@Produces(MediaType.APPLICATION_JSON)
	public Result revokeTokens (@ApiParam(value = "the id of the stream", required = true) @QueryParam("id") String streamId) {
		Result result = new Result(false);

		if(streamId != null) {

			result.setSuccess(getDataStore().revokeTokens(streamId));
		}

		return result;
	}


	/**
	 * Get the all tokens of requested stream
	 * @param streamId - the id of the stream
	 * @param offset - the starting point of the list
	 * @param size - size of the return list (max:50 )
	 * @return token list of stream,  if no active tokens then returns null
	 */
	@ApiOperation(value = "Get the all tokens of requested stream", notes = "",responseContainer = "List", response = Token.class)
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
	 * Set stream specific Mp4 Muxing setting, this setting overrides general Mp4 Muxing Setting
	 * 
	 * @param streamId - the id of the stream
	 * @param enableMp4 - the integer value for Mp4 Muxing, 1 = Enable Muxing, -1 = Disable Muxing, 0 = No Settings
	 * @return  {@link io.antmedia.rest.BroadcastRestService.Result}
	 */

	@ApiOperation(value = "Set stream specific Mp4 Muxing setting, this setting overrides general Mp4 Muxing Setting", notes = "", response = Result.class)
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/broadcast/enableMp4Muxing")
	@Produces(MediaType.APPLICATION_JSON)
	public Result enableMp4Muxing (@ApiParam(value = "the id of the stream", required = true) @QueryParam("id")String streamId,
			@ApiParam(value = "the integer value for Mp4 Muxing, 1 = Enable Muxing, -1 = Disable Muxing, 0 = No Settings", required = true) @QueryParam("enableMp4") int enableMp4) {
		Result result = new Result(false);
		if(streamId != null) {

			if(getDataStore().setMp4Muxing(streamId, enableMp4)) {		
				result.setSuccess(true);
				result.setMessage("streamId:"+ streamId);
			}else {
				result.setMessage("no stream for this id: " + streamId + " or wrong setting parameter");
			}
		}

		return result;
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
	@ApiOperation(value = "Get the broadcast live statistics total RTMP watcher count, total HLS watcher count, total WebRTC watcher count", notes = "", response = BroadcastStatistics.class)
	@GET
	@Path("/broadcast/getBroadcastLiveStatistics")
	@Produces(MediaType.APPLICATION_JSON)
	public BroadcastStatistics getBroadcastStatistics(@ApiParam(value = "the id of the stream", required = true) @QueryParam("id") String id) {

		int totalRTMPViewer = -1;
		int totalWebRTCViewer = -1;
		int totalHLSViewer = -1;
		if (id != null) 
		{
			IBroadcastScope broadcastScope = getScope().getBroadcastScope(id);

			if (broadcastScope != null)	{
				totalRTMPViewer = broadcastScope.getConsumers().size();
			}

			Broadcast broadcast = getDataStore().get(id);
			if (broadcast != null) {
				totalHLSViewer = broadcast.getHlsViewerCount();
			}

			IWebRTCAdaptor webRTCAdaptor = getWebRTCAdaptor();

			if (webRTCAdaptor != null) {
				totalWebRTCViewer = webRTCAdaptor.getNumberOfViewers(id);
			}
		}

		return new BroadcastStatistics(totalRTMPViewer, totalHLSViewer, totalWebRTCViewer);
	}


	/**
	 * Get WebRTC Client Statistics such as : Audio bitrate, Video bitrate, Target bitrate, Video Sent Period etc.
	 * 
	 * @param streamId - the id of the stream
	 * @return the list of {@link io.antmedia.rest.WebRTCClientStats }
	 */

	@ApiOperation(value = "Get WebRTC Client Statistics such as : Audio bitrate, Video bitrate, Target bitrate, Video Sent Period etc.", notes = "", responseContainer = "List",response = WebRTCClientStats.class)
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

	@ApiOperation(value = "Get WebRTC Client Statistics such as : Audio bitrate, Video bitrate, Target bitrate, Video Sent Period etc.", notes = "", responseContainer = "List",response = WebRTCClientStats.class)
	@GET
	@Path("/broadcast/getWebRTCClientStatsList/{offset}/{size}/{stream_id}")
	@Produces(MediaType.APPLICATION_JSON)
	public List<WebRTCClientStats> getWebRTCClientStatsList(@ApiParam(value = "offset of the list", required = true) @PathParam("offset") int offset,
			@ApiParam(value = "Number of items that will be fetched", required = true) @PathParam("size") int size,
			@ApiParam(value = "the id of the stream", required = true) @PathParam("stream_id") String streamId) {

		List<WebRTCClientStats> list = new ArrayList<>();

		IWebRTCAdaptor webRTCAdaptor = getWebRTCAdaptor();

		if (webRTCAdaptor != null) 
		{
			Collection<WebRTCClientStats> webRTCClientStats = webRTCAdaptor.getWebRTCClientStats(streamId);

			int t = 0;
			int itemCount = 0;
			if (size > MAX_ITEM_IN_ONE_LIST) {
				size = MAX_ITEM_IN_ONE_LIST;
			}
			if (offset < 0) {
				offset = 0;
			}

			for (WebRTCClientStats webrtcClientStat : webRTCClientStats) {
				if (t < offset) {
					t++;
					continue;
				}
				list.add(webrtcClientStat);
				itemCount++;

				if (itemCount >= size ) {
					return list;
				}
			}
		}
		return list;
	}

	public IWebRTCAdaptor getWebRTCAdaptor() {
		IWebRTCAdaptor adaptor = null;
		ApplicationContext appContext = getAppContext();
		if (appContext != null && appContext.containsBean(IWebRTCAdaptor.BEAN_NAME)) {
			adaptor = (IWebRTCAdaptor) appContext.getBean(IWebRTCAdaptor.BEAN_NAME);
		}
		return adaptor;
	}

	/**
	 * Get filtered broadcast list
	 * @param offset - starting point of the list
	 * @param size - size of the return list (max:50 )
	 * @param type - type of the stream
	 * @return list of the broadcast objects
	 */
	@ApiOperation(value = "Returns filtered broadcast list", notes = "",responseContainer = "List",response = Broadcast.class)
	@GET
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/broadcast/filterList/{offset}/{size}/{type}")
	@Produces(MediaType.APPLICATION_JSON)
	public List<Broadcast> filterBroadcastList(@ApiParam(value = "starting point of the list", required = true) @PathParam("offset") int offset,
			@ApiParam(value = "size of the return list (max:50 )", required = true) @PathParam("size") int size,
			@ApiParam(value = "type of the stream", required = true) @PathParam("type") String type) {
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

	@ApiOperation(value = "Delete specific VoD File. Deprecated -> Use deleteVoD method (/broadcast/deleteVoD/{id})", notes = "", response = Result.class)
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
	@ApiOperation(value = "Delete specific VoD File", notes = "", response = Result.class)
	@POST
	@Consumes({ MediaType.APPLICATION_JSON })
	@Path("/broadcast/deleteVoD/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Result deleteVoD(@ApiParam(value = "the id of the VoD file", required = true) @PathParam("id") String id) {
		boolean success = false;
		String message = "";
		ApplicationContext appContext = getAppContext();
		if (appContext != null) {

			File videoFile = null;
			VoD voD = getDataStore().getVoD(id);
			if (voD != null) {
				try {
					String filePath = String.format("webapps/%s/%s", getScope().getName(), voD.getFilePath());
					videoFile = new File(filePath);
					boolean result = Files.deleteIfExists(videoFile.toPath());
					if (!result) {
						logger.warn("File is not deleted because it does not exist {}", videoFile.getAbsolutePath());
					}
					success = getDataStore().deleteVod(id);
					if (success) {
						message = "vod deleted";
					}

					String fileName = videoFile.getName();
					String[] splitFileName = StringUtils.split(fileName,".");
					//delete preview file if exists
					File previewFile = Muxer.getPreviewFile(getScope(), splitFileName[0], ".png");
					Files.deleteIfExists(previewFile.toPath());

					if (appContext.containsBean("app.storageClient")) {
						StorageClient storageClient = (StorageClient) appContext.getBean("app.storageClient");

						storageClient.delete(splitFileName[0] + ".mp4", FileType.TYPE_STREAM);
						storageClient.delete(splitFileName[0] + ".png", FileType.TYPE_PREVIEW);
					}
				}
				catch (Exception e) {
					logger.error(ExceptionUtils.getStackTrace(e));
				}
			}

		}
		return new Result(success, message);
	}

	/**
	 * Upload external user VoD file to Ant Media Server
	 * 
	 * @param fileName - the name of the VoD File
	 * @param inputStream - the input stream of VoD file
	 * @return {@link io.antmedia.rest.BroadcastRestService.Result}
	 */

	@ApiOperation(value = "Upload external VoD file to Ant Media Server", notes = "", response = Result.class)
	@POST
	@Consumes({MediaType.MULTIPART_FORM_DATA})
	@Path("/broadcast/uploadVoDFile/{name}")
	@Produces(MediaType.APPLICATION_JSON)
	public Result uploadVoDFile(@ApiParam(value = "the name of the VoD File", required = true) @PathParam("name") String fileName,
			@ApiParam(value = "file", required = true) @FormDataParam("file") InputStream inputStream) {
		boolean success = false;
		String message = "";
		String id= null;
		String appScopeName = getScope().getName();
		String fileExtension = FilenameUtils.getExtension(fileName);
		try {

			if ("mp4".equals(fileExtension)) {


				File streamsDirectory = new File(
						getStreamsDirectory(appScopeName));

				// if the directory does not exist, create it
				if (!streamsDirectory.exists()) {
					streamsDirectory.mkdirs();
				}
				String vodId = RandomStringUtils.randomNumeric(24);
				File savedFile = new File(String.format("%s/webapps/%s/%s", System.getProperty("red5.root"), appScopeName,
						"streams/" + vodId + ".mp4"));

				int read = 0;
				byte[] bytes = new byte[2048];
				try (OutputStream outpuStream = new FileOutputStream(savedFile))
				{

					while ((read = inputStream.read(bytes)) != -1) {
						outpuStream.write(bytes, 0, read);
					}
					outpuStream.flush();

					long fileSize = savedFile.length();
					long unixTime = System.currentTimeMillis();

					String path = savedFile.getPath();

					String[] subDirs = path.split(Pattern.quote(File.separator));

					Integer pathLength = subDirs.length;

					String relativePath = subDirs[pathLength-2]+ File.separator +subDirs[pathLength-1];

					VoD newVod = new VoD(fileName, "file", relativePath, fileName, unixTime, 0, fileSize,
							VoD.UPLOADED_VOD, vodId);

					id = getDataStore().addVod(newVod);

					if(id != null) {
						success = true;
						message = id;
					} 
				}
			} 
			else {
				message = "notMp4File";
			}

		} 
		catch (IOException iox) {
			logger.error(iox.getMessage());
		} 


		return new Result(success, id, message);
	}


	public String getStreamsDirectory(String appScopeName) {
		return String.format("%s/webapps/%s/%s", System.getProperty("red5.root"), appScopeName, "streams");
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
	@ApiOperation(value = "Delete broadcast from data store", notes = "", response = Result.class)
	@POST
	@Consumes({ MediaType.APPLICATION_JSON })
	@Path("/broadcast/delete/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Result deleteBroadcast(@ApiParam(value = " Id of the braodcast", required = true) @PathParam("id") String id) {
		Result result = new Result (false);
		boolean stopResult = false;

		if (id != null) {
			Broadcast broacast = getDataStore().get(id);
			stopResult = stopBroadcastInternal(broacast);

			result.setSuccess(getDataStore().delete(id));

			if(result.isSuccess() && stopResult) {
				result.setMessage("brodcast is deleted and stopped successfully");
				logger.info("brodcast {} is deleted and stopped successfully", id);
			}
			else if(result.isSuccess() && !stopResult) {
				result.setMessage("brodcast is deleted but could not stopped ");
				logger.info("brodcast {} is deleted but could not stopped", id);
			}
		}
		return result;
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
	@ApiOperation(value = "Get device parameters for social network authorization.", notes = "", response = Object.class)
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/broadcast/getDeviceAuthParameters/{serviceName}")
	@Produces(MediaType.APPLICATION_JSON)
	public Object getDeviceAuthParameters(@ApiParam(value = "Name of the service, like Facebook, Youtube, Periscope", required = true) @PathParam("serviceName") String serviceName) {
		String message = null;
		boolean missingClientIdAndSecret = false;

		int errorId = -1;
		VideoServiceEndpoint videoServiceEndpoint = null;
		if (serviceName.equals(AntMediaApplicationAdapter.FACEBOOK)) 
		{
			String clientId = getAppSettings().getFacebookClientId();
			String clientSecret = getAppSettings().getFacebookClientSecret();

			videoServiceEndpoint = getApplication().getEndpointService(AntMediaApplicationAdapter.FACEBOOK_ENDPOINT_CLASS, null, clientId, clientSecret);

			if (isClientIdMissing(videoServiceEndpoint, clientId, clientSecret)) 
			{
				missingClientIdAndSecret = true;
			}

		}
		else if (serviceName.equals(AntMediaApplicationAdapter.YOUTUBE)) 
		{

			String clientId = getAppSettings().getYoutubeClientId();
			String clientSecret = getAppSettings().getYoutubeClientSecret();

			videoServiceEndpoint = getApplication().getEndpointService(AntMediaApplicationAdapter.YOUTUBE_ENDPOINT_CLASS, null, clientId, clientSecret);

			if (isClientIdMissing(videoServiceEndpoint, clientId, clientSecret)) 
			{
				missingClientIdAndSecret = true;
			}

		}
		else if (serviceName.equals(AntMediaApplicationAdapter.PERISCOPE)) 
		{
			String clientId = getAppSettings().getPeriscopeClientId();
			String clientSecret = getAppSettings().getPeriscopeClientSecret();

			videoServiceEndpoint = getApplication().getEndpointService(PeriscopeEndpoint.class.getName(), null, clientId, clientSecret);

			if (isClientIdMissing(videoServiceEndpoint, clientId, clientSecret))  {
				missingClientIdAndSecret = true;
			}
		}

		try {

			if (missingClientIdAndSecret) {
				errorId = ERROR_SOCIAL_ENDPOINT_UNDEFINED_CLIENT_ID;
				message = "Please enter service client id and client secret in app configuration";
			}
			else if (videoServiceEndpoint == null) {
				errorId = ERROR_SOCIAL_ENDPOINT_UNDEFINED_ENDPOINT;
				message = "Service with the name specified is not found in this app";
			}
			else {
				DeviceAuthParameters askDeviceAuthParameters = videoServiceEndpoint.askDeviceAuthParameters();

				getApplication().startDeviceAuthStatusPolling(videoServiceEndpoint,
						askDeviceAuthParameters);
				return askDeviceAuthParameters;
			}
		}
		catch (Exception e) {
			errorId = ERROR_SOCIAL_ENDPOINT_EXCEPTION_IN_ASKING_AUTHPARAMS;
			message = "Exception in asking parameters";
			logger.error(ExceptionUtils.getStackTrace(e));
		}

		return new Result(false, message, errorId);
	}

	private boolean isClientIdMissing(VideoServiceEndpoint videoServiceEndpoint, String clientId, String clientSecret) {
		boolean result = false;
		if ((videoServiceEndpoint != null) && 
				(clientId == null || clientSecret == null || 
				clientId.length() == 0 || clientSecret.length() == 0)) {
			result = true;
		}
		return result;
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
	@ApiOperation(value = "Check if device is authenticated in the social network. In authorization phase, " +
			"this function may be polled periodically until it returns success." +
			"Server checks social network service for about 1 minute so that if user" +
			"does not enter DeviceAuthParameters in a 1 minute, this function will" +
			"never return true", notes = "", response = Result.class)
	@POST
	@Consumes({ MediaType.APPLICATION_JSON })
	@Path("/broadcast/checkDeviceAuthStatus/{userCode}")
	@Produces(MediaType.APPLICATION_JSON)
	public Result checkDeviceAuthStatus(@ApiParam(value = "Code of social media account", required = true) @PathParam("userCode") String userCode) {
		Map<String, VideoServiceEndpoint> endPointMap = getEndpointList();
		String message = null;
		boolean authenticated = false;
		String endpointId = null;
		if (endPointMap != null) {
			for (VideoServiceEndpoint videoServiceEndpoint : endPointMap.values()) {
				//if there is an endpoint added to the list with same user code,
				//it means it is authenticated
				DeviceAuthParameters authParameters = videoServiceEndpoint.getAuthParameters();
				if (authParameters != null && authParameters.user_code.equals(userCode)) {
					authenticated = true;
					endpointId = videoServiceEndpoint.getCredentials().getId();
					break;
				}
			}
		}
		if (!authenticated) {
			List<VideoServiceEndpoint> endPointList = getEndpointsHavingErrorList();
			for (VideoServiceEndpoint videoServiceEndpoint : endPointList) {
				DeviceAuthParameters authParameters = videoServiceEndpoint.getAuthParameters();
				if (authParameters != null && authParameters.user_code.equals(userCode)) {
					message = videoServiceEndpoint.getError();
					endPointList.remove(videoServiceEndpoint);
					break;
				}
			}

		}
		return new Result(authenticated, endpointId, message);
	}

	/**
	 * Get Credentials of Social Endpoints
	 * 
	 * @param offset - the starting point of the list
	 * @param size - size of the return list (max:50 )
	 * @return - list of {@link io.antmedia.datastore.db.types.SocialEndpointCredentials}
	 */
	@ApiOperation(value = "Get Credentials of Social Endpoints", notes = "", responseContainer = "List",response = SocialEndpointCredentials.class)
	@GET
	@Consumes({ MediaType.APPLICATION_JSON })
	@Path("/broadcast/getSocialEndpoints/{offset}/{size}")
	@Produces(MediaType.APPLICATION_JSON)
	public List<SocialEndpointCredentials> getSocialEndpoints(@ApiParam(value = "the starting point of the list", required = true) @PathParam("offset") int offset,
			@ApiParam(value = "size of the return list (max:50 )", required = true) @PathParam("size") int size) {
		List<SocialEndpointCredentials> endPointCredentials = new ArrayList<>();
		Map<String, VideoServiceEndpoint> endPointMap = getEndpointList();
		if (endPointMap != null) {
			for (VideoServiceEndpoint videoServiceEndpoint : endPointMap.values()) {
				endPointCredentials.add(videoServiceEndpoint.getCredentials());
			}
		}
		return endPointCredentials;
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
	@ApiOperation(value = "Some social networks have different channels especially for facebook," +
			"Live stream can be published on Facebook Page or Personal account, this" +
			"service returns the related information about that.", notes = "", response = SocialEndpointChannel.class)
	@GET
	@Consumes({ MediaType.APPLICATION_JSON })
	@Path("/broadcast/getSocialNetworkChannel/{endpointId}")
	@Produces(MediaType.APPLICATION_JSON)
	public SocialEndpointChannel getSocialNetworkChannel(@ApiParam(value = "endpointId", required = true) @PathParam("endpointId") String endpointId) {
		Map<String, VideoServiceEndpoint> endPointMap = getEndpointList();
		VideoServiceEndpoint endPoint = endPointMap.get(endpointId);
		SocialEndpointChannel channel = null;
		if (endPoint != null) {
			channel = endPoint.getChannel();
		}
		return channel;
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
	@ApiOperation(value = "Returns available social network channels for the specific service", notes = "",responseContainer = "List",response = SocialEndpointChannel.class)
	@GET
	@Consumes({ MediaType.APPLICATION_JSON })
	@Path("/broadcast/getSocialNetworkChannelList/{endpointId}/{type}")
	@Produces(MediaType.APPLICATION_JSON)
	public List<SocialEndpointChannel> getSocialNetworkChannelList(@ApiParam(value = "endpointId", required = true) @PathParam("endpointId") String endpointId,
			@ApiParam(value = "This is very service specific, it may be page for Facebook", required = true) @PathParam("type") String type) {

		Map<String, VideoServiceEndpoint> endPointMap = getEndpointList();
		VideoServiceEndpoint endPoint = endPointMap.get(endpointId);
		List<SocialEndpointChannel>  channelList = null;
		if (endPoint != null) {
			channelList = endPoint.getChannelList();
		}
		return channelList;
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
	@ApiOperation(value = "If there are multiple channels in a social network," +
			"this method sets specific channel for that endpoint" +
			"If a user has pages in Facebook, this method sets the specific page to publish live stream to", notes = "", response = Result.class)
	@POST
	@Consumes({ MediaType.APPLICATION_JSON })
	@Path("/broadcast/setSocialNetworkChannel/{endpointId}/{type}/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Result setSocialNetworkChannelList(@ApiParam(value = "endpointId", required = true) @PathParam("endpointId") String endpointId,
			@ApiParam(value = "type", required = true) @PathParam("type") String type,
			@ApiParam(value = "id", required = true) @PathParam("id") String channelId) {
		boolean result = false;
		Map<String, VideoServiceEndpoint> endPointMap = getEndpointList();

		VideoServiceEndpoint endPoint = endPointMap.get(endpointId);

		if (endPoint != null) {
			result = endPoint.setActiveChannel(type, channelId);
		}
		return new Result(result, null);
	}


	public static boolean isEnterprise() {
		try {
			Class.forName("io.antmedia.enterprise.adaptive.EncoderAdaptor");
			return true;
		} catch (ClassNotFoundException e) {
			return false;
		}
	}

	public long getRecordCount() {
		return getDataStore().getBroadcastCount();
	}

	protected List<VideoServiceEndpoint> getEndpointsHavingErrorList(){
		return getApplication().getVideoServiceEndpointsHavingError();
	}

	private AppSettings getAppSettings() {
		if (appSettings == null) {
			ApplicationContext appContext = getAppContext();
			if (appContext != null) {
				appSettings = (AppSettings) appContext.getBean(AppSettings.BEAN_NAME);
			}
		}
		return appSettings;
	}

	public void setAppSettings(AppSettings appSettings) {
		this.appSettings = appSettings;
	}

	public ProcessBuilderFactory getProcessBuilderFactory() {
		return processBuilderFactory;
	}


	public void setProcessBuilderFactory(ProcessBuilderFactory processBuilderFactory) {
		this.processBuilderFactory = processBuilderFactory;
	}


}
