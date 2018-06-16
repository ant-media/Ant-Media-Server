package io.antmedia.rest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.red5.server.api.scope.IBroadcastScope;
import org.red5.server.api.scope.IScope;
import org.red5.server.api.scope.ScopeType;
import org.red5.server.api.stream.IBroadcastStream;
import org.red5.server.api.stream.IClientBroadcastStream;
import org.red5.server.util.ScopeUtils;
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
import io.antmedia.datastore.db.types.Vod;
import io.antmedia.muxer.Muxer;
import io.antmedia.rest.model.Result;
import io.antmedia.rest.model.Version;
import io.antmedia.social.endpoint.PeriscopeEndpoint;
import io.antmedia.social.endpoint.VideoServiceEndpoint;
import io.antmedia.social.endpoint.VideoServiceEndpoint.DeviceAuthParameters;
import io.antmedia.storage.StorageClient;
import io.antmedia.storage.StorageClient.FileType;

@Component
@Path("/")
public class BroadcastRestService {

	//TODO: Where this class is being used? I do not see any reference in code.  @mekya
	public static class SearchParam {
		public String keyword = null;

		public String getKeyword() {
			return keyword;
		}

		public void setKeyword(String keyword) {
			this.keyword = keyword;
		}

		public long getStartDate() {
			return startDate;
		}

		public void setStartDate(long startDate) {
			this.startDate = startDate;
		}

		public long getEndDate() {
			return endDate;
		}

		public void setEndDate(long endDate) {
			this.endDate = endDate;
		}

		public long startDate = 0;
		public long endDate = 0;

		public SearchParam(String keyword, long startDate, long endDate) {
			this.keyword = keyword;
			this.startDate = startDate;
			this.endDate = endDate;
		}

		public SearchParam() {
			super();
		}

	}


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

	public static class LiveStatistics extends BroadcastStatistics {

		public final int totalLiveStreamCount;

		public LiveStatistics(int totalLiveStreamCount, int totalRTMPWatchersCount, int totalHLSWatchersCount,
				int totalWebRTCWatchersCount) {
			super(totalRTMPWatchersCount, totalHLSWatchersCount, totalWebRTCWatchersCount);
			this.totalLiveStreamCount = totalLiveStreamCount;

		}

	}

	public static final int ERROR_SOCIAL_ENDPOINT_UNDEFINED_CLIENT_ID = -1;
	public static final int ERROR_SOCIAL_ENDPOINT_UNDEFINED_ENDPOINT = -2;
	public static final int ERROR_SOCIAL_ENDPOINT_EXCEPTION_IN_ASKING_AUTHPARAMS = -3;

	private static final String MYSQL_CLIENT_PATH = "/usr/local/antmedia/mysql";
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
		return saveBroadcast(broadcast, AntMediaApplicationAdapter.BROADCAST_STATUS_CREATED, getScope().getName(),
				getDataStore(), getAppSettings());
	}


	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Path("/broadcast/createPortalBroadcast")
	@Produces(MediaType.APPLICATION_JSON)
	public Broadcast createPortalBroadcast(@FormParam("name") String name, @FormParam("listenerHookURL") String listenerHookURL) {

		Broadcast broadcast=new Broadcast();

		broadcast.setName(name);
		broadcast.setListenerHookURL(listenerHookURL);


		return saveBroadcast(broadcast, AntMediaApplicationAdapter.BROADCAST_STATUS_CREATED, getScope().getName(),
				getDataStore(), getAppSettings());
	}



	public static Broadcast saveBroadcast(Broadcast broadcast, String status, String scopeName, IDataStore dataStore,
			AppSettings settings) {

		if (broadcast == null) {
			broadcast = new Broadcast();
		}
		broadcast.setStatus(status);
		broadcast.setDate(System.currentTimeMillis());

		String listenerHookURL = broadcast.getListenerHookURL();

		if (settings != null) {
			if (listenerHookURL == null || listenerHookURL.length() == 0) {
				String settingsListenerHookURL = settings.getListenerHookURL();
				if (settingsListenerHookURL != null && settingsListenerHookURL.length() > 0) {
					broadcast.setListenerHookURL(settingsListenerHookURL);
				}
			}

			String fqdn = settings.getServerName();
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
		String message = "";
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
						message += networkName + " ";
						errorId = -1;
						break;
					}
				}
			}
		}
		if (message.length() > 0) {
			message += " endpoint cannot be added";
		}
		return new Result(result, message, errorId);
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
		List<VideoServiceEndpoint> endPoint = getEndpointList();
		String message = null;
		boolean serviceFound = false;
		boolean result = false;
		if (endPoint != null) {
			for (VideoServiceEndpoint videoServiceEndpoint : endPoint) 
			{
				SocialEndpointCredentials credentials = videoServiceEndpoint.getCredentials();
				if (credentials.getId().equals(endpointId)) {
					serviceFound = true;
					videoServiceEndpoint.resetCredentials();
					endPoint.remove(videoServiceEndpoint);
					result = true;
					break;

				}
			}
			if (!serviceFound) {
				message = "Service with the name specified is not found in this app";
			}
		} else {
			message = "No endpoint is defined for this app";
		}
		return new Result(result, message);
	}

	/**
	 * Add social endpoint to a stream
	 * 
	 * @param id
	 *            of the broadcast
	 * 
	 * @param serviceName
	 *            name of the service like facebook, youtube, periscope in order
	 *            to have successfull operation. Social network must be
	 *            authorized in advance
	 * 
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
			List<VideoServiceEndpoint> endPointServiceList = getEndpointList();

			if (endPointServiceList != null) {
				boolean serviceFound = false;
				for (VideoServiceEndpoint videoServiceEndpoint : endPointServiceList) {

					SocialEndpointCredentials credentials = videoServiceEndpoint.getCredentials();
					if (credentials != null && credentials.getId().equals(endpointServiceId)) {
						serviceFound = true;

						Endpoint endpoint;
						try {
							endpoint = videoServiceEndpoint.createBroadcast(broadcast.getName(),
									broadcast.getDescription(), broadcast.isIs360(), broadcast.isPublicStream(),
									720, true);
							success = getDataStore().addEndpoint(id, endpoint);

						} catch (Exception e) {
							logger.error(ExceptionUtils.getStackTrace(e));
							message = e.getMessage();
						}

					}
				}
				if (!serviceFound) {
					message = endpointServiceId + " endpoint does not exist in this app.";
				}
			} else {
				message = "No social endpoint is defined for this app. Consult your app developer";
			}
		} else {
			message = "No broadcast exist with the id specified";
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
			Broadcast broadcast = lookupBroadcast(id);

			Endpoint endpoint = new Endpoint();
			endpoint.rtmpUrl = rtmpUrl;
			endpoint.type = "generic";

			success = getDataStore().addEndpoint(id, endpoint);
		} catch (Exception e) {
			logger.error(ExceptionUtils.getStackTrace(e));
		}

		return new Result(success, message);
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
		if (processBuilderFactory != null) {
			process = processBuilderFactory.make(MYSQL_CLIENT_PATH, 
					"-h", stalkerDBServer,
					"-u", stalkerDBUsername,
					"-p"+stalkerDBPassword,
					"-e",   query);
		}
		else {
			try {
				process = new ProcessBuilder(
						MYSQL_CLIENT_PATH, 
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

				List<Vod> vodList = new ArrayList<>();
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

				for (Vod vod : vodList) {
					if (vod.getType().equals(Vod.USER_VOD)) {
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
	public List<Vod> getVodList(@PathParam("offset") int offset, @PathParam("size") int size) {
		return getDataStore().getVodList(offset, size);
	}

	@GET
	@Path("/broadcast/getTotalVodNumber")
	@Produces(MediaType.APPLICATION_JSON)
	public long getTotalVodNumber() {
		return getDataStore().getTotalVodNumber();
	}

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
		Set<String> basicBroadcastScopes = getScope().getBasicScopeNames(ScopeType.BROADCAST);

		int totalLiveStreamCount = getScope().getBasicScopeNames(ScopeType.BROADCAST).size();
		int totalRTMPWatcherCount = getScope().getStatistics().getActiveClients() - totalLiveStreamCount;

		return new LiveStatistics(totalLiveStreamCount, totalRTMPWatcherCount, 0, 0);
	}

	/**
	 * Get the broadcast live statistics total rtmp watcher count, total hls
	 * watcher count, total webrtc watcher count
	 * 
	 * @param streamId
	 * @return {@link BroadcastStatistics} if broadcast exists null or 204(no
	 *         content) if no broacdast exists with that id
	 */
	@GET
	@Path("/broadcast/getBroadcastLiveStatistics")
	@Produces(MediaType.APPLICATION_JSON)
	public BroadcastStatistics getBroadcastStatistics(@QueryParam("id") String id) {
		IBroadcastScope broadcastScope = getScope().getBroadcastScope(id);
		BroadcastStatistics broadcastStatistics = null;

		if (broadcastScope != null) {
			broadcastStatistics = new BroadcastStatistics(broadcastScope.getConsumers().size(), 0, 0);
		} else {
			broadcastStatistics = new BroadcastStatistics(-1, -1, -1);
		}
		return broadcastStatistics;
	}

	/**
	 * Deletes vod file in the file system
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
	@Path("/broadcast/deleteVoDFile/{name}/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Result deleteVoDFile(@PathParam("name") String fileName, @PathParam("id") String id) {
		boolean success = false;
		String message = "";
		if (getAppContext() != null) {
			// TODO: write test code for this function

			File recordFile = Muxer.getRecordFile(getScope(), id, ".mp4");
			File uploadedFile = Muxer.getUploadRecordFile(getScope(), id, ".mp4");

			logger.info("recordfile {} : " , recordFile.getAbsolutePath());


			if (recordFile.exists()) {
				success = true;
				recordFile.delete();
				message = "streamvod found and deleted";
				getDataStore().deleteVod(id);

			} else if (uploadedFile.exists()) {
				success = true;
				uploadedFile.delete();
				message = "uploadedVod is found and deleted";
				getDataStore().deleteVod(id);

			}else {
				success = getDataStore().deleteVod(id);

			}
			File previewFile = Muxer.getPreviewFile(getScope(), fileName, ".png");
			if (previewFile.exists()) {
				previewFile.delete();
			}

			if (getAppContext().containsBean("app.storageClient")) {
				StorageClient storageClient = (StorageClient) getAppContext().getBean("app.storageClient");

				storageClient.delete(fileName + ".mp4", FileType.TYPE_STREAM);
				storageClient.delete(fileName + ".png", FileType.TYPE_PREVIEW);
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
		String appScopeName = ScopeUtils.findApplication(getScope()).getName();
		String fileExtension = FilenameUtils.getExtension(fileName);
		try {

			if (fileExtension.equals("mp4")) {

				File streamsDirectory = new File(
						String.format("%s/webapps/%s/%s", System.getProperty("red5.root"), appScopeName, "streams"));

				// if the directory does not exist, create it
				if (!streamsDirectory.exists()) {
					streamsDirectory.mkdir();
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

					String path=savedFile.getPath();

					String[] subDirs = path.split(Pattern.quote(File.separator));

					Integer pathLength=Integer.valueOf(subDirs.length);

					String relativePath=subDirs[pathLength-3]+'/'+subDirs[pathLength-2]+'/'+subDirs[pathLength-1];

					Vod newVod = new Vod(fileName, "file", relativePath, fileName, unixTime, 0, fileSize,
							Vod.UPLOADED_VOD, vodId);

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


		return new Result(success, message);
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

				}else if(result.isSuccess() && !stopResult) {
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

			if (videoServiceEndpoint != null) 
			{
				if (clientId == null || clientSecret == null || 
						clientId.length() == 0 || clientSecret.length() == 0) {
					missingClientIdAndSecret = true;
				}
			}

		}
		else if (serviceName.equals(AntMediaApplicationAdapter.YOUTUBE)) 
		{

			String clientId = getAppSettings().getYoutubeClientId();
			String clientSecret = getAppSettings().getYoutubeClientSecret();

			videoServiceEndpoint = getApplication().getEndpointService(AntMediaApplicationAdapter.YOUTUBE_ENDPOINT_CLASS, null, clientId, clientSecret);

			if (videoServiceEndpoint != null) 
			{
				if (clientId == null || clientSecret == null || 
						clientId.length() == 0 || clientSecret.length() == 0) {
					missingClientIdAndSecret = true;
				}
			}

		}
		else if (serviceName.equals(AntMediaApplicationAdapter.PERISCOPE)) 
		{
			String clientId = getAppSettings().getPeriscopeClientId();
			String clientSecret = getAppSettings().getPeriscopeClientSecret();
			if (clientId != null && clientSecret != null && 
					clientId.length() > 0 && clientSecret.length() > 0) {
				videoServiceEndpoint = new PeriscopeEndpoint(clientId,
						clientSecret, getDataStore(), null);
			}
			else {
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
		List<VideoServiceEndpoint> endPoint = getEndpointList();
		String message = null;
		boolean authenticated = false;
		String endpointId = null;
		if (endPoint != null) {
			for (VideoServiceEndpoint videoServiceEndpoint : endPoint) {
				//if there is an endpoint added to the list with same user code,
				//it means it is authenticated
				DeviceAuthParameters authParameters = videoServiceEndpoint.getAuthParameters();
				if (authParameters != null) {
					if (authParameters.user_code.equals(userCode)) {
						authenticated = true;
						endpointId = videoServiceEndpoint.getCredentials().getId();
						break;
					}
				}
			}
		}
		if (!authenticated) {
			endPoint = getEndpointsHavingErrorList();
			for (VideoServiceEndpoint videoServiceEndpoint : endPoint) {
				DeviceAuthParameters authParameters = videoServiceEndpoint.getAuthParameters();
				if (authParameters != null) {
					if (authParameters.user_code.equals(userCode)) {
						authenticated = false;
						message = videoServiceEndpoint.getError();
						endPoint.remove(videoServiceEndpoint);
						break;
					}
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
		List<VideoServiceEndpoint> endPoint = getEndpointList();
		List<SocialEndpointCredentials> endPointCredentials = new ArrayList();
		if (endPoint != null) {
			for (VideoServiceEndpoint videoServiceEndpoint : endPoint) {
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
		List<VideoServiceEndpoint> endPoint = getEndpointList();
		SocialEndpointChannel channel = null;
		if (endPoint != null) {
			for (VideoServiceEndpoint videoServiceEndpoint : endPoint) {

				SocialEndpointCredentials credentials = videoServiceEndpoint.getCredentials();
				if (credentials != null && credentials.getId().equals(endpointId)) {
					channel = videoServiceEndpoint.getChannel();
					break;
				}
			}
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
		List<VideoServiceEndpoint> endPoint = getEndpointList();
		List<SocialEndpointChannel> channelList = null;
		if (endPoint != null) {

			for (VideoServiceEndpoint videoServiceEndpoint : endPoint) {
				SocialEndpointCredentials credentials = videoServiceEndpoint.getCredentials();
				if (credentials != null && credentials.getId().equals(endpointId)) {
					channelList = videoServiceEndpoint.getChannelList();
					break;
				}
			}
		}
		return channelList;
	}

	/**
	 * Sets channel that live stream will be published on specific social
	 * network channel
	 * 
	 * @param serviceName
	 *            Name of the social network service
	 * 
	 * @param type
	 *            Type of the channel
	 * 
	 * @param id
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
			@PathParam("type") String type, @PathParam("id") String id) {
		boolean result = false;
		List<VideoServiceEndpoint> endPoint = getEndpointList();

		if (endPoint != null) {
			for (VideoServiceEndpoint videoServiceEndpoint : endPoint) {

				SocialEndpointCredentials credentials = videoServiceEndpoint.getCredentials();
				if (credentials != null && credentials.getId().equals(endpointId)) {
					result = videoServiceEndpoint.setActiveChannel(type, id);
					break;
				}

			}
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

	protected List<VideoServiceEndpoint> getEndpointList() {
		return (getApplication()).getVideoServiceEndpoints();
	}

	protected List<VideoServiceEndpoint> getEndpointsHavingErrorList(){
		return (getApplication()).getVideoServiceEndpointsHavingError();
	}

	@Nullable
	private ApplicationContext getAppContext() {
		if (servletContext != null) {
			appCtx = (ApplicationContext) servletContext
					.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);

		}

		return appCtx;
	}

	public IDataStore getDataStore() {
		if (dataStore == null) {
			dataStore = (IDataStore) getAppContext().getBean("db.datastore");
		}
		return dataStore;
	}

	public void setDataStore(IDataStore dataStore) {
		this.dataStore = dataStore;
	}

	protected AntMediaApplicationAdapter getApplication() {
		if (app == null) {
			app = (AntMediaApplicationAdapter) getAppContext().getBean("web.handler");
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
			appSettings = (AppSettings) getAppContext().getBean(AppSettings.BEAN_NAME);
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
