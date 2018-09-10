package io.antmedia.rest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.red5.server.api.scope.IBroadcastScope;
import org.red5.server.api.scope.IScope;
import org.red5.server.api.stream.IBroadcastStream;
import org.red5.server.api.stream.IClientBroadcastStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

import com.drew.lang.annotations.Nullable;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.AppSettings;
import io.antmedia.datastore.db.IDataStore;
import io.antmedia.datastore.db.types.Broadcast;
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
import io.antmedia.social.LiveComment;
import io.antmedia.social.endpoint.PeriscopeEndpoint;
import io.antmedia.social.endpoint.VideoServiceEndpoint;
import io.antmedia.social.endpoint.VideoServiceEndpoint.DeviceAuthParameters;
import io.antmedia.storage.StorageClient;
import io.antmedia.storage.StorageClient.FileType;
import io.antmedia.webrtc.api.IWebRTCAdaptor;

@Component
@Path("/")
public class BroadcastRestService {

	public static class BroadcastStatistics {

		public final int totalRTMPWatchersCount;
		public final int totalHLSWatchersCount;
		public final int totalWebRTCWatchersCount;

		public BroadcastStatistics(int totalRTMPWatchersCount, int totalHLSWatchersCount,
				int totalWebRTCWatchersCount) {
			this.totalRTMPWatchersCount = totalRTMPWatchersCount;
			this.totalHLSWatchersCount = totalHLSWatchersCount;
			this.totalWebRTCWatchersCount = totalWebRTCWatchersCount;
		}
	}

	public static class LiveStatistics  {

		public final long totalLiveStreamCount;

		public LiveStatistics(long totalLiveStreamCount) {
			this.totalLiveStreamCount = totalLiveStreamCount;
		}

	}

	public static final int ERROR_SOCIAL_ENDPOINT_UNDEFINED_CLIENT_ID = -1;
	public static final int ERROR_SOCIAL_ENDPOINT_UNDEFINED_ENDPOINT = -2;
	public static final int ERROR_SOCIAL_ENDPOINT_EXCEPTION_IN_ASKING_AUTHPARAMS = -3;

	public static final String ENTERPRISE_EDITION = "Enterprise Edition";
	public static final String COMMUNITY_EDITION = "Community Edition";

	@Context
	private ServletContext servletContext;

	private IScope scope;


	private ApplicationContext appCtx;

	private AntMediaApplicationAdapter app;

	private IDataStore dataStore;

	private AppSettings appSettings;
	public interface ProcessBuilderFactory {
		Process make(String...args);
	}

	private ProcessBuilderFactory processBuilderFactory = null;

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
	public Broadcast createBroadcast(Broadcast broadcast) {
		if (broadcast != null) {
			// make sure stream id is not set on rest service
			broadcast.resetStreamId();
		}

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
	public Broadcast createPortalBroadcast(@FormParam("name") String name, @FormParam("listenerHookURL") String listenerHookURL) {

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



	public static Broadcast saveBroadcast(Broadcast broadcast, String status, String scopeName, IDataStore dataStore,
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
			try {
				fqdn = InetAddress.getLocalHost().getHostAddress();
			} catch (UnknownHostException e) {
				logger.error(ExceptionUtils.getStackTrace(e));
			}
		}

		if (fqdn != null && fqdn.length() >= 0) {
			broadcast.setRtmpURL("rtmp://" + fqdn + "/" + scopeName + "/");
		}



		dataStore.save(broadcast);
		return broadcast;
	}

	/**
	 * Create broadcast and bind social networks at the same time Server should
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
	public Broadcast createWithSocial(Broadcast broadcast,
			@QueryParam("socialNetworks") String socialEndpointIds) {
		broadcast = createBroadcast(broadcast);
		if (broadcast.getStreamId() != null && socialEndpointIds != null) {
			String[] endpointIds = socialEndpointIds.split(",");
			for (String endpointId : endpointIds) {
				addSocialEndpoint(broadcast.getStreamId(), endpointId);
			}
		}

		return getBroadcast(broadcast.getStreamId());
	}

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/broadcast/stop/{streamId}")
	@Produces(MediaType.APPLICATION_JSON)
	public Result stopBroadcast(@PathParam("streamId") String streamId) {

		boolean result = false;
		String message = "";
		IBroadcastStream broadcastStream = getApplication().getBroadcastStream(getScope(), streamId);
		if (broadcastStream != null) {
			((IClientBroadcastStream) broadcastStream).getConnection().close();
			result = true;
		} else {
			message = "No active broadcast found with id " + streamId;

			logger.warn("No active broadcast found with id {}", streamId);
		}

		return new Result(result, message);
	}

	/**
	 * Updates broadcast name or status
	 * 
	 * @param broadcast
	 * 
	 * 
	 * @param id
	 *            id of the broadcast that is given when creating broadcast
	 * 
	 * @param name
	 *            New name of the broadcast
	 * 
	 * @param description
	 *            New description of the broadcast
	 * 
	 * @return {@link io.antmedia.rest.BroadcastRestService.Result}
	 * 
	 */
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/broadcast/update")
	@Produces(MediaType.APPLICATION_JSON)
	public Result updateBroadcast(Broadcast broadcast, @QueryParam("socialNetworks") String socialNetworksToPublish) {

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
	 * Revoke authorization from a social network account that is authorized
	 * before
	 * 
	 * @param serviceName
	 *            Name of the service
	 * 
	 */
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/broadcast/revokeSocialNetwork/{endpointId}")
	@Produces(MediaType.APPLICATION_JSON)
	public Result revokeSocialNetwork(@PathParam("endpointId") String endpointId) {
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
	 * @param id
	 *            of the stream
	 * 
	 * @param endpointServiceId
	 *            name of the service like facebook, youtube, periscope in order
	 *            to have successfull operation. Social network must be
	 *            authorized in advance
	 * 
	 * @return {@link io.antmedia.rest.BroadcastRestService.Result}
	 * 
	 */
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/broadcast/addSocialEndpointJS/{id}/{endpointServiceId}")
	@Produces(MediaType.APPLICATION_JSON)
	public Result addSocialEndpointJSON(@PathParam("id") String id, @PathParam("endpointServiceId") String endpointServiceId) {
		return addSocialEndpoint(id, endpointServiceId);
	}

	/**
	 * Add social endpoint to a stream. Use the JSON version of this method
	 * 
	 * @param id
	 *            of the stream
	 * 
	 * @param endpointServiceId
	 *            name of the service like facebook, youtube, periscope in order
	 *            to have successfull operation. Social network must be
	 *            authorized in advance
	 * @return {@link io.antmedia.rest.BroadcastRestService.Result}
	 * 
	 */
	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Path("/broadcast/addSocialEndpoint")
	@Produces(MediaType.APPLICATION_JSON)
	public Result addSocialEndpoint(@FormParam("id") String id, @FormParam("serviceName") String endpointServiceId) {
		boolean success = false;
		String message = null;
		Broadcast broadcast = lookupBroadcast(id);
		if (broadcast != null) {
			Map<String, VideoServiceEndpoint> endPointServiceList = getEndpointList();

			if (endPointServiceList != null) {

				VideoServiceEndpoint videoServiceEndpoint = endPointServiceList.get(endpointServiceId);

				if (videoServiceEndpoint != null) {
					Endpoint endpoint;
					try {
						endpoint = videoServiceEndpoint.createBroadcast(broadcast.getName(),
								broadcast.getDescription(), id, broadcast.isIs360(), broadcast.isPublicStream(),
								720, true);
						success = getDataStore().addEndpoint(id, endpoint);

					} catch (Exception e) {
						logger.error(ExceptionUtils.getStackTrace(e));
						message = e.getMessage();
					}
				}
				else {
					message = endpointServiceId + " endpoint does not exist in this app.";
					logger.warn(message);
				}
			} else {
				message = "No social endpoint is defined for this app. Consult your app developer";
				logger.warn(message);
			}
		} else {
			message = "No broadcast exist with the id specified";
			logger.warn(message);
		}

		return new Result(success, message);
	}

	/**
	 * Add a third pary rtmp end point to the stream. When broadcast is started,
	 * it will send rtmp stream to this rtmp url as well.
	 * 
	 * @param id
	 *            This is the id of broadcast
	 * 
	 * @param rtmpUrl
	 *            rtmp url of the endpoint that stream will be republished
	 * 
	 * @return {@link io.antmedia.rest.BroadcastRestService.Result}
	 * 
	 */
	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Path("/broadcast/addEndpoint")
	@Produces(MediaType.APPLICATION_JSON)
	public Result addEndpoint(@FormParam("id") String id, @FormParam("rtmpUrl") String rtmpUrl) {
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
	 * Returns live comments from a specific endpoint like facebook, youtube, pscp, etc.
	 * 
	 * 
	 * @param endpointServiceId 
	 * This is the id of the endpoint service 
	 * @param streamId This 
	 * is the id of the stream
	 * @param offset
	 * @param batch
	 * @return
	 */
	@GET
	@Path("/broadcast/getLiveComments/{endpointServiceId}/{streamId}/{offset}/{batch}")
	@Produces(MediaType.APPLICATION_JSON)
	public List<LiveComment> getLiveCommentsFromEndpoint(@PathParam("endpointServiceId") String endpointServiceId, @PathParam("streamId") String streamId, @PathParam("offset") int offset,  @PathParam("batch") int batch) {

		VideoServiceEndpoint videoServiceEndPoint = getApplication().getVideoServiceEndPoint(endpointServiceId);
		List<LiveComment> liveComment = null;
		if (videoServiceEndPoint != null) {
			liveComment = videoServiceEndPoint.getComments(streamId, offset, batch);
		}
		return liveComment;
	}
	
	/**
	 * Return the number of live views in specified video service endpoint
	 * 
	 * 
	 * @param endpointServiceId
	 * @param streamId
	 * @return
	 */
	@GET
	@Path("/broadcast/getLiveViewsCount/{endpointServiceId}/{streamId}")
	@Produces(MediaType.APPLICATION_JSON)
	public Result getViewerCountFromEndpoint(@PathParam("endpointServiceId") String endpointServiceId, @PathParam("streamId") String streamId) {
		VideoServiceEndpoint videoServiceEndPoint = getApplication().getVideoServiceEndPoint(endpointServiceId);
		long liveViews = 0;
		if (videoServiceEndPoint != null) {
			liveViews = videoServiceEndPoint.getLiveViews(streamId);
		}
		return new Result(true, String.valueOf(liveViews));
	}
	
	
	/**
	 * Returns the number of live comment count in a specific video service endpoint
	 * 
	 * @param endpointServiceId
	 * @param streamId
	 * @return
	 */
	@GET
	@Path("/broadcast/getLiveCommentsCount/{endpointServiceId}/{streamId}")
	@Produces(MediaType.APPLICATION_JSON)
	public Result getLiveCommentsCount( @PathParam("endpointServiceId") String endpointServiceId,  @PathParam("streamId") String streamId) {
		VideoServiceEndpoint videoServiceEndPoint = getApplication().getVideoServiceEndPoint(endpointServiceId);
		int commentCount = 0;
		if (videoServiceEndPoint != null) {
			commentCount = videoServiceEndPoint.getTotalCommentsCount(streamId);
		}
		return new Result(true, String.valueOf(commentCount));
	}

	/**
	 * Return the interaction from a specific endpoint like facebook, youtube, pscp, etc. 
	 * 
	 * @param endpointServiceId
	 * @param streamId
	 * @return
	 */
	@GET
	@Path("/broadcast/getInteraction/{endpointServiceId}/{streamId}")
	@Produces(MediaType.APPLICATION_JSON)
	public Interaction getInteractionFromEndpoint(@PathParam("endpointServiceId") String endpointServiceId, @PathParam("streamId") String streamId) {
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
	 * @param id
	 *            id of the broadcast
	 * 
	 * @return broadcast object nothing if broadcast is not found
	 * 
	 */
	@GET
	@Path("/broadcast/get")
	@Produces(MediaType.APPLICATION_JSON)
	public Broadcast getBroadcast(@QueryParam("id") String id) {
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
	 * Get vod file in db
	 * @param id
	 * @return
	 */
	@GET
	@Path("/broadcast/getVoD")
	@Produces(MediaType.APPLICATION_JSON)
	public VoD getVoD(@QueryParam("id") String id) {
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
	 * Get Detected objects
	 * 
	 * @param id
	 *            id of the stream
	 * 
	 * @return List of detected objects
	 * 
	 */
	@GET
	@Path("/detection/get")
	@Produces(MediaType.APPLICATION_JSON)
	public List<TensorFlowObject> getDetectedObjects(@QueryParam("id") String id) {
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


	@GET
	@Path("/detection/getList/{offset}/{size}")
	@Produces(MediaType.APPLICATION_JSON)
	public List<TensorFlowObject> getDetectionList(@QueryParam("id") String id, @PathParam("offset") int offset, @PathParam("size") int size) {
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
	 * Get Detected objects size
	 * 
	 * @param id
	 *            id of the stream
	 * 
	 * @return Size of detected objects
	 * 
	 */

	@GET
	@Path("/detection/getObjectDetectedTotal")
	@Produces(MediaType.APPLICATION_JSON)
	public long getObjectDetectedTotal(@QueryParam("id") String id){
		return getDataStore().getObjectDetectedTotal(id);
	}


	/**
	 * Gets the broadcast list from database
	 * 
	 * @param offset
	 *            This is the offset of the list, it is useful for pagination,
	 * 
	 * @param size
	 *            Number of items that will be fetched. If there is not enough
	 *            item in the datastore, returned list size may less then this
	 *            value
	 * 
	 * @return JSON broadcast list
	 * 
	 */
	@GET
	@Path("/broadcast/getList/{offset}/{size}")
	@Produces(MediaType.APPLICATION_JSON)
	public List<Broadcast> getBroadcastList(@PathParam("offset") int offset, @PathParam("size") int size) {
		return getDataStore().getBroadcastList(offset, size);
	}



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
			int pageCount = (int) broadcastCount/IDataStore.MAX_ITEM_IN_ONE_LIST
					+ ((broadcastCount % IDataStore.MAX_ITEM_IN_ONE_LIST != 0) ? 1 : 0);

			List<Broadcast> broadcastList = new ArrayList<>();
			for (int i = 0; i < pageCount; i++) {
				broadcastList.addAll(getDataStore().getBroadcastList(i*IDataStore.MAX_ITEM_IN_ONE_LIST, IDataStore.MAX_ITEM_IN_ONE_LIST));
			}

			StringBuilder insertQueryString = new StringBuilder();

			insertQueryString.append("DELETE FROM stalker_db.ch_links;");
			insertQueryString.append("DELETE FROM stalker_db.itv;");

			String fqdn = getAppSettings().getServerName();
			if (fqdn == null || fqdn.length() == 0) {
				try {
					fqdn = InetAddress.getLocalHost().getHostAddress();
				} catch (UnknownHostException e) {
					logger.error(ExceptionUtils.getStackTrace(e));
				}
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
				int pageCount = (int) totalVodNumber/IDataStore.MAX_ITEM_IN_ONE_LIST 
						+ ((totalVodNumber % IDataStore.MAX_ITEM_IN_ONE_LIST != 0) ? 1 : 0);

				List<VoD> vodList = new ArrayList<>();
				for (int i = 0; i < pageCount; i++) {
					vodList.addAll(getDataStore().getVodList(i*IDataStore.MAX_ITEM_IN_ONE_LIST, IDataStore.MAX_ITEM_IN_ONE_LIST));
				}


				String fqdn = getAppSettings().getServerName();
				if (fqdn == null || fqdn.length() == 0) {
					try {
						fqdn = InetAddress.getLocalHost().getHostAddress();
					} catch (UnknownHostException e) {
						logger.error(ExceptionUtils.getStackTrace(e));
					}
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



	@GET
	@Path("/broadcast/getVodList/{offset}/{size}")
	@Produces(MediaType.APPLICATION_JSON)
	public List<VoD> getVodList(@PathParam("offset") int offset, @PathParam("size") int size) {
		return getDataStore().getVodList(offset, size);
	}

	@GET
	@Path("/broadcast/getTotalVodNumber")
	@Produces(MediaType.APPLICATION_JSON)
	public long getTotalVodNumber() {
		return getDataStore().getTotalVodNumber();
	}

	/**
	 * Returns the version
	 * 
	 * TO DO: Change endpoint from /broadcast/getVersion to /getVersion 
	 * @return
	 */
	@GET
	@Path("/broadcast/getVersion")
	@Produces(MediaType.APPLICATION_JSON)
	public Version getVersion() {
		Version versionList = new Version();
		versionList.setVersionName(AntMediaApplicationAdapter.class.getPackage().getImplementationVersion());
		versionList.setVersionType(BroadcastRestService.isEnterprise() ? ENTERPRISE_EDITION : COMMUNITY_EDITION);

		logger.debug("Version Name {} Version Type {}", versionList.getVersionName(), versionList.getVersionType());
		return versionList;
	}


	@GET
	@Path("/broadcast/getTotalBroadcastNumber")
	@Produces(MediaType.APPLICATION_JSON)
	public long getTotalBroadcastNumber() {
		return getDataStore().getTotalBroadcastNumber();
	}

	/**
	 * Returns total live streams, total rtmp watchers, total hls and total
	 * webrtc watchers
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
	 * @param streamId
	 * @param expireDate
	 * @return token
	 */
	@GET
	@Path("/broadcast/getToken")
	@Produces(MediaType.APPLICATION_JSON)
	public Token getToken (@QueryParam("id")String streamId, @QueryParam("expireDate") long expireDate, @QueryParam("type") String type) {
		Token token = null;

		if(streamId != null) {

			token = getDataStore().createToken(streamId, expireDate, type);
		}

		return token;
	}


	/**
	 * Perform validation of token for required stream
	 * @param token
	 * @return validated token, either null or token. Null means not validated
	 */

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/broadcast/validateToken")
	@Produces(MediaType.APPLICATION_JSON)
	public Token validateToken (Token token) {
		Token validatedToken = null;

		if(token.getTokenId() != null) {

			validatedToken = getDataStore().validateToken(token);
		}

		return validatedToken;
	}


	/**
	 * Removes all tokens related with requested stream
	 * @param streamId
	 * @return result object including success or not
	 */

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/broadcast/revokeTokens")
	@Produces(MediaType.APPLICATION_JSON)
	public Result revokeTokens (@QueryParam("id")String streamId) {
		Result result = new Result(false);

		if(streamId != null) {

			result.setSuccess(getDataStore().revokeTokens(streamId));
		}

		return result;
	}


	/**
	 * Get the all tokens of requested stream
	 * @param streamId
	 * @param offset
	 * @param size
	 * @return token list of stream,  if no active tokens returns null
	 */
	@GET
	@Path("/broadcast/listTokens/{streamId}/{offset}/{size}")
	@Produces(MediaType.APPLICATION_JSON)
	public List<Token> listTokens (@PathParam("streamId") String streamId, @PathParam("offset") int offset, @PathParam("size") int size) {
		List<Token> tokens = null;

		if(streamId != null) {

			tokens = getDataStore().listAllTokens(streamId, offset, size);
		}

		return tokens;
	}


	/**
	 * Get the broadcast live statistics total rtmp watcher count, total hls
	 * watcher count, total webrtc watcher count
	 * 
	 * Return -1 for the values that is n/a
	 * 
	 * @param streamId
	 * @return {@link BroadcastStatistics} if broadcast exists null or 204(no
	 *         content) if no broadcast exists with that id
	 */
	@GET
	@Path("/broadcast/getBroadcastLiveStatistics")
	@Produces(MediaType.APPLICATION_JSON)
	public BroadcastStatistics getBroadcastStatistics(@QueryParam("id") String id) {

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


	@GET
	@Path("/broadcast/getWebRTCClientStats/{stream_id}")
	@Produces(MediaType.APPLICATION_JSON)
	public List<WebRTCClientStats> getWebRTCClientStats(@PathParam("stream_id") String streamId) {
		IWebRTCAdaptor webRTCAdaptor = getWebRTCAdaptor();
		if (webRTCAdaptor != null) {
			return webRTCAdaptor.getWebRTCClientStats(streamId);
		}
		return new ArrayList<>();
	}

	private IWebRTCAdaptor getWebRTCAdaptor() {
		IWebRTCAdaptor adaptor = null;
		ApplicationContext appContext = getAppContext();
		if (appContext != null && appContext.containsBean(IWebRTCAdaptor.BEAN_NAME)) {
			adaptor = (IWebRTCAdaptor) appContext.getBean(IWebRTCAdaptor.BEAN_NAME);
		}
		return adaptor;
	}


	/**
	 * Filter broadcast according to type
	 * 
	 * @param fileName
	 *            name of the file
	 * 
	 * @return {@link io.antmedia.rest.BroadcastRestService.Result}
	 * 
	 */

	@GET
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/broadcast/filterList/{offset}/{size}/{type}")
	@Produces(MediaType.APPLICATION_JSON)
	public List<Broadcast> filterBroadcastList(@PathParam("offset") int offset, @PathParam("size") int size,
			@PathParam("type") String type) {
		return getDataStore().filterBroadcastList(offset, size, type);
	}


	@POST
	@Consumes({ MediaType.APPLICATION_JSON })
	@Path("/broadcast/deleteVoDFile/{name}/{id}/{type}")
	@Produces(MediaType.APPLICATION_JSON)
	public Result deleteVoDFile(@PathParam("name") String fleName, @PathParam("id") String id,@PathParam("type") String type) {
		return deleteVoD(id);
	}


	@POST
	@Consumes({ MediaType.APPLICATION_JSON })
	@Path("/broadcast/deleteVoD/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Result deleteVoD(@PathParam("id") String id) {
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

	@POST
	@Consumes({MediaType.MULTIPART_FORM_DATA})
	@Path("/broadcast/uploadVoDFile/{name}")
	@Produces(MediaType.APPLICATION_JSON)
	public Result uploadVoDFile(@PathParam("name") String fileName, @FormDataParam("file") InputStream inputStream) {
		boolean success = false;
		String message = "";
		String id= null;
		String appScopeName = getScope().getName();
		String fileExtension = FilenameUtils.getExtension(fileName);
		try {

			if (fileExtension.equals("mp4")) {


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
	 *            Id of the braodcast
	 * 
	 * @return Result object with success field true or false
	 * 
	 */
	@POST
	@Consumes({ MediaType.APPLICATION_JSON })
	@Path("/broadcast/delete/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Result deleteBroadcast(@PathParam("id") String id) {
		Result result = new Result (false);

		if (id != null) {
			Broadcast broacast = getDataStore().get(id);
			if (broacast != null) {
				if (broacast.getType().equals(AntMediaApplicationAdapter.IP_CAMERA)||broacast.getType().equals(AntMediaApplicationAdapter.STREAM_SOURCE)) {
					getApplication().stopStreaming(broacast);

				}
				result.setSuccess(getDataStore().delete(id));
				boolean stopResult = stopBroadcast(id).isSuccess();

				if(result.isSuccess() && stopResult) {
					result.setMessage("brodcast is deleted and stopped successfully");
					logger.info("brodcast {} is deleted and stopped successfully", id);
				}
				else if(result.isSuccess() && !stopResult) {
					result.setMessage("brodcast is deleted but could not stopped ");
					logger.info("brodcast {} is deleted but could not stopped", id);
				}

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
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/broadcast/getDeviceAuthParameters/{serviceName}")
	@Produces(MediaType.APPLICATION_JSON)
	public Object getDeviceAuthParameters(@PathParam("serviceName") String serviceName) {
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
	 * @return Result object with success field. If success field is true, it is
	 *         authenticated if false, not authenticated
	 * 
	 */
	@POST
	@Consumes({ MediaType.APPLICATION_JSON })
	@Path("/broadcast/checkDeviceAuthStatus/{userCode}")
	@Produces(MediaType.APPLICATION_JSON)
	public Result checkDeviceAuthStatus(@PathParam("userCode") String userCode) {
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


	@GET
	@Consumes({ MediaType.APPLICATION_JSON })
	@Path("/broadcast/getSocialEndpoints/{offset}/{size}")
	@Produces(MediaType.APPLICATION_JSON)
	public List<SocialEndpointCredentials> getSocialEndpoints(@PathParam("offset") int offset, @PathParam("size") int size) {
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
	 * @param serviceName
	 *            Name of the social network (facebook,youtube,periscope)
	 * 
	 * @return {@link io.antmedia.datastore.db.types.SocialEndpointChannel}
	 * 
	 */
	@GET
	@Consumes({ MediaType.APPLICATION_JSON })
	@Path("/broadcast/getSocialNetworkChannel/{endpointId}")
	@Produces(MediaType.APPLICATION_JSON)
	public SocialEndpointChannel getSocialNetworkChannel(@PathParam("endpointId") String endpointId) {
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
	 * @param serviceName
	 *            Name of the social network
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
	public List<SocialEndpointChannel> getSocialNetworkChannelList(@PathParam("endpointId") String endpointId,
			@PathParam("type") String type) {

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
	 * @param serviceName
	 *            Name of the social network service
	 * 
	 * @param type
	 *            Type of the channel
	 * 
	 * @param channelId
	 *            id of the channel
	 * 
	 * @return {@link io.antmedia.rest.BroadcastRestService.Result}
	 * 
	 */
	@POST
	@Consumes({ MediaType.APPLICATION_JSON })
	@Path("/broadcast/setSocialNetworkChannel/{endpointId}/{type}/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Result setSocialNetworkChannelList(@PathParam("endpointId") String endpointId,
			@PathParam("type") String type, @PathParam("id") String channelId) {
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

	protected Map<String, VideoServiceEndpoint> getEndpointList() {
		return getApplication().getVideoServiceEndpoints();
	}

	protected List<VideoServiceEndpoint> getEndpointsHavingErrorList(){
		return getApplication().getVideoServiceEndpointsHavingError();
	}

	@Nullable
	private ApplicationContext getAppContext() 
	{
		if (appCtx == null && servletContext != null) {
			appCtx = (ApplicationContext) servletContext
					.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);

		}
		return appCtx;
	}

	public IDataStore getDataStore() {
		if (dataStore == null) {
			ApplicationContext appContext = getAppContext();
			if (appContext != null) {
				dataStore = (IDataStore) appContext.getBean("db.datastore");
			}
		}
		return dataStore;
	}

	public void setDataStore(IDataStore dataStore) {
		this.dataStore = dataStore;
	}

	public AntMediaApplicationAdapter getApplication() {
		if (app == null) {
			ApplicationContext appContext = getAppContext();
			if (appContext != null) {
				app = (AntMediaApplicationAdapter) appContext.getBean(AntMediaApplicationAdapter.BEAN_NAME);
			}
		}
		return app;
	}

	/**
	 * this is for testing
	 * @param app
	 */
	public void setApplication(AntMediaApplicationAdapter app) {
		this.app = app;
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

	public IScope getScope() {
		if (scope == null) {
			scope = getApplication().getScope();
		}
		return scope;
	}

	public void setScope(IScope scope) {
		this.scope = scope;
	}

	public void setAppCtx(ApplicationContext appCtx) {
		this.appCtx = appCtx;
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
