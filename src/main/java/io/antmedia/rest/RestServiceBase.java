package io.antmedia.rest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.nio.file.Files;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.jar.Manifest;
import java.util.regex.Pattern;

import javax.annotation.Nullable;
import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.red5.server.api.scope.IBroadcastScope;
import org.red5.server.api.scope.IScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.AppSettings;
import io.antmedia.RecordType;
import io.antmedia.cluster.IClusterNotifier;
import io.antmedia.datastore.db.DataStore;
import io.antmedia.datastore.db.DataStoreFactory;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.datastore.db.types.Broadcast.PlayListItem;
import io.antmedia.datastore.db.types.ConferenceRoom;
import io.antmedia.datastore.db.types.Endpoint;
import io.antmedia.datastore.db.types.TensorFlowObject;
import io.antmedia.datastore.db.types.Token;
import io.antmedia.datastore.db.types.VoD;
import io.antmedia.ipcamera.OnvifCamera;
import io.antmedia.ipcamera.onvifdiscovery.OnvifDiscovery;
import io.antmedia.muxer.IAntMediaStreamHandler;
import io.antmedia.muxer.Mp4Muxer;
import io.antmedia.muxer.MuxAdaptor;
import io.antmedia.muxer.Muxer;
import io.antmedia.muxer.RecordMuxer;
import io.antmedia.rest.model.Result;
import io.antmedia.rest.model.Version;
import io.antmedia.security.ITokenService;
import io.antmedia.settings.ServerSettings;
import io.antmedia.statistic.DashViewerStats;
import io.antmedia.statistic.HlsViewerStats;
import io.antmedia.statistic.IStatsCollector;
import io.antmedia.storage.StorageClient;
import io.antmedia.streamsource.StreamFetcher;
import io.antmedia.webrtc.api.IWebRTCAdaptor;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

public abstract class RestServiceBase {

	@ApiModel(value="BroadcastStatistics", description="The statistics class of the broadcasts")
	public static class BroadcastStatistics {

		@ApiModelProperty(value = "the total RTMP viewers of the stream")
		public final int totalRTMPWatchersCount;

		@ApiModelProperty(value = "the total HLS viewers of the stream")
		public final int totalHLSWatchersCount;

		@ApiModelProperty(value = "the total WebRTC viewers of the stream")
		public final int totalWebRTCWatchersCount;

		@ApiModelProperty(value = "the total DASH viewers of the stream")
		public final int totalDASHWatchersCount;


		public BroadcastStatistics(int totalRTMPWatchersCount, int totalHLSWatchersCount,
								   int totalWebRTCWatchersCount, int totalDASHWatchersCount) {
			this.totalRTMPWatchersCount = totalRTMPWatchersCount;
			this.totalHLSWatchersCount = totalHLSWatchersCount;
			this.totalWebRTCWatchersCount = totalWebRTCWatchersCount;
			this.totalDASHWatchersCount = totalDASHWatchersCount;
		}
	}

	@ApiModel(value="AppBroadcastStatistics", description="The statistics class of the app. It provides total number of viewers and active live streams")
	public static class AppBroadcastStatistics extends BroadcastStatistics {

		@ApiModelProperty(value = "the total active live stream count")
		public final int activeLiveStreamCount;

		public AppBroadcastStatistics(int totalRTMPWatchersCount, int totalHLSWatchersCount,
									  int totalWebRTCWatchersCount, int totalDASHWatchersCount, int activeLiveStreamCount )
		{
			super(totalRTMPWatchersCount, totalHLSWatchersCount, totalWebRTCWatchersCount, totalDASHWatchersCount);
			this.activeLiveStreamCount = activeLiveStreamCount;
		}

	}

	public interface ProcessBuilderFactory {
		Process make(String...args);
	}

	/**
	 * Key for Manifest entry of Build number. It should match with the value in pom.xml
	 */
	public static final String BUILD_NUMBER = "Build-Number";

	public static final String ENTERPRISE_EDITION = "Enterprise Edition";

	public static final String COMMUNITY_EDITION = "Community Edition";

	public static final int MAX_ITEM_IN_ONE_LIST = 50;
	public static final int ERROR_SOCIAL_ENDPOINT_UNDEFINED_CLIENT_ID = -1;
	public static final int ERROR_SOCIAL_ENDPOINT_UNDEFINED_ENDPOINT = -2;
	public static final int ERROR_SOCIAL_ENDPOINT_EXCEPTION_IN_ASKING_AUTHPARAMS = -3;

	public static final int RECORD_ENABLE = 1;
	public static final int RECORD_DISABLE = -1;
	public static final int RECORD_NO_SET = 0;

	public static final int HIGH_CPU_ERROR = -3;
	public static final int FETCHER_NOT_STARTED_ERROR = -4;
	public static final int INVALID_STREAM_NAME_ERROR = -5;

	public static final String HTTP = "http://";
	public static final String RTSP = "rtsp://";
	public static final String ENDPOINT_GENERIC = "generic";

	protected static Logger logger = LoggerFactory.getLogger(RestServiceBase.class);

	private ProcessBuilderFactory processBuilderFactory = null;

	//TODO: This REGEX does not fully match 10.10.157.200. It ignores the last 0 it matches 10.10.157.20 and it cause problem in replacements
	public static final String IPV4_REGEX = "(([0-1]?[0-9]{1,2}\\.)|(2[0-4][0-9]\\.)|(25[0-5]\\.)){3}(([0-1]?[0-9]{1,2})|(2[0-4][0-9])|(25[0-5]))";

	public static final String LOOPBACK_REGEX = "^localhost$|^127(?:\\.[0-9]+){0,2}\\.[0-9]+$|^(?:0*\\:)*?:?0*1$";
	private static final String REPLACE_CHARS = "[\n|\r|\t]";
	@Context
	protected ServletContext servletContext;
	protected DataStoreFactory dataStoreFactory;
	private DataStore dbStore;
	protected ApplicationContext appCtx;
	protected IScope scope;
	protected AntMediaApplicationAdapter appInstance;

	private AppSettings appSettings;

	private ServerSettings serverSettings;


	public void setAppCtx(ApplicationContext appCtx) {
		this.appCtx = appCtx;
	}

	@Nullable
	public ApplicationContext getAppContext() {
		if (servletContext != null) {
			appCtx = (ApplicationContext) servletContext
					.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
		}
		return appCtx;
	}

	/**
	 * this is for testing
	 * @param app
	 */
	public void setApplication(AntMediaApplicationAdapter app) {
		this.appInstance = app;
	}

	public AntMediaApplicationAdapter getApplication() {
		if (appInstance == null) {
			ApplicationContext appContext = getAppContext();
			if (appContext != null) {
				appInstance = (AntMediaApplicationAdapter) appContext.getBean(AntMediaApplicationAdapter.BEAN_NAME);
			}
		}
		return appInstance;
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

	public DataStore getDataStore() {
		if (dbStore == null) {
			dbStore = getDataStoreFactory().getDataStore();
		}
		return dbStore;
	}

	public void setDataStore(DataStore dataStore) {
		this.dbStore = dataStore;
	}

	public DataStoreFactory getDataStoreFactory() {
		if(dataStoreFactory == null) {
			WebApplicationContext ctxt = WebApplicationContextUtils.getWebApplicationContext(servletContext);
			if (ctxt != null) {
				dataStoreFactory = (DataStoreFactory) ctxt.getBean("dataStoreFactory");
			}
		}
		return dataStoreFactory;
	}


	public void setDataStoreFactory(DataStoreFactory dataStoreFactory) {
		this.dataStoreFactory = dataStoreFactory;
	}

	public Broadcast createBroadcastWithStreamID(Broadcast broadcast) {
		return saveBroadcast(broadcast, IAntMediaStreamHandler.BROADCAST_STATUS_CREATED, getScope().getName(),
				getDataStore(), getAppSettings().getListenerHookURL(), getServerSettings(), 0);
	}

	public static Broadcast saveBroadcast(Broadcast broadcast, String status, String scopeName, DataStore dataStore,
										  String settingsListenerHookURL, ServerSettings serverSettings, long absoluteStartTimeMs) {

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
		String fqdn = serverSettings.getServerName();

		if (fqdn == null || fqdn.length() == 0) {
			fqdn = serverSettings.getHostAddress();
		}
		broadcast.setOriginAdress(serverSettings.getHostAddress());
		broadcast.setAbsoluteStartTimeMs(absoluteStartTimeMs);

		removeEmptyPlayListItems(broadcast);

		if (fqdn != null && fqdn.length() >= 0) {
			broadcast.setRtmpURL("rtmp://" + fqdn + "/" + scopeName + "/");
		}

		dataStore.save(broadcast);
		return broadcast;
	}



	public AppSettings getAppSettings() {
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


	public ServerSettings getServerSettings() {
		if (serverSettings == null) {
			ApplicationContext appContext = getAppContext();
			if (appContext != null) {
				serverSettings = (ServerSettings) appContext.getBean(ServerSettings.BEAN_NAME);
			}
		}
		return serverSettings;
	}

	public void setServerSettings(ServerSettings serverSettings) {
		this.serverSettings = serverSettings;
	}


	protected Result deleteBroadcast(String id) {
		Result result = new Result (false);
		boolean stopResult = false;
		Broadcast broadcast = null;

		if (id != null && (broadcast = getDataStore().get(id)) != null)
		{
			boolean isCluster = getAppContext().containsBean(IClusterNotifier.BEAN_NAME);

			if (isCluster && !broadcast.getOriginAdress().equals(getServerSettings().getHostAddress()) && broadcast.getStatus().equals(IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING))
			{
				logger.error("Please send a Delete Broadcast request to the {} node or Delete Broadcast in a stopped broadcast.", broadcast.getOriginAdress());
				result.setSuccess(false);
			}
			else {
				stopResult = stopBroadcastInternal(broadcast);

				result.setSuccess(getDataStore().delete(id));

				if(result.isSuccess())
				{
					if (stopResult) {
						logger.info("broadcast {} is deleted and stopped successfully", broadcast.getStreamId());
						result.setMessage("broadcast is deleted and stopped successfully");
					}
					else {
						logger.info("broadcast {} is deleted but could not stopped", broadcast);
						result.setMessage("broadcast is deleted but could not stopped ");
					}
				}
			}
		}
		else
		{
			logger.warn("Broadcast delete operation not successfull because broadcast is not found in db for stream id:{}", id != null ? id.replaceAll(REPLACE_CHARS, "_") : null);
		}
		return result;
	}

	protected Result deleteBroadcasts(String[] streamIds) {

		Result result = new Result(false);

		if(streamIds != null)
		{
			for (String id : streamIds)
			{
				result = deleteBroadcast(id);
				if (!result.isSuccess())
				{
					logger.warn("It cannot delete {} and breaking the loop", id);
					break;
				}
			}
		}
		else
		{
			logger.warn("Requested deletion for Stream Ids is empty");
		}

		return result;
	}

	protected boolean stopBroadcastInternal(Broadcast broadcast) {
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

	protected Broadcast lookupBroadcast(String id) {
		Broadcast broadcast = null;
		try {
			broadcast = getDataStore().get(id);
		} catch (Exception e) {
			logger.error(ExceptionUtils.getStackTrace(e));
		}
		return broadcast;
	}

	protected ConferenceRoom lookupConference(String id) {
		ConferenceRoom room = null;
		try {
			room = getDataStore().getConferenceRoom(id);
		} catch (Exception e) {
			logger.error(ExceptionUtils.getStackTrace(e));
		}
		return room;
	}

	protected Result updateBroadcast(String streamId, Broadcast broadcast) {

		removeEmptyPlayListItems(broadcast);

		boolean result = getDataStore().updateBroadcastFields(streamId, broadcast);

		return new Result(result);
	}

	private static void removeEmptyPlayListItems(Broadcast broadcast)
	{
		List<PlayListItem> playListItemList = broadcast.getPlayListItemList();
		if (playListItemList != null)
		{
			Iterator<PlayListItem> iterator = playListItemList.iterator();
			while (iterator.hasNext())
			{
				PlayListItem listItem = iterator.next();
				if (listItem.getStreamUrl() == null || listItem.getStreamUrl().isEmpty())
				{
					iterator.remove();
				}
			}
		}
	}

	/**
	 * Update Stream Source or IP Camera info
	 * @param broadcast
	 * @param socialNetworksToPublish
	 * @return
	 */
	protected Result updateStreamSource(String streamId, Broadcast broadcast) {
		logger.debug("Updating camera info for stream {}", broadcast.getStreamId());

		if (!checkStreamUrl(broadcast.getStreamUrl())) {
			return new Result(false, "Stream URL is not valid");
		}

		Broadcast broadcastInDB = getDataStore().get(streamId);
		if (broadcastInDB == null) {
			streamId = streamId.replaceAll("[\n|\r|\t]", "_");
			logger.info("Broadcast with stream id: {} is null", streamId);
			return new Result(false, "Broadcast with streamId: " + streamId + " does not exist");
		}

		boolean resultStopStreaming = checkStopStreaming(broadcastInDB);
		waitStopStreaming(broadcastInDB, resultStopStreaming);

		if (AntMediaApplicationAdapter.IP_CAMERA.equals(broadcast.getType())) {
			Result connectionRes = connectToCamera(broadcast);
			if (!connectionRes.isSuccess()) {
				return connectionRes;
			}

			String rtspURL = connectionRes.getMessage();
			String authparam = broadcast.getUsername() + ":" + broadcast.getPassword() + "@";
			String rtspURLWithAuth = RTSP + authparam + rtspURL.substring(RTSP.length());
			logger.info("New Stream Source URL: {}", rtspURLWithAuth);
			broadcast.setStreamUrl(rtspURLWithAuth);
		}

		boolean result = getDataStore().updateBroadcastFields(streamId, broadcast);
		if (result) {
			Broadcast fetchedBroadcast = getDataStore().get(streamId);
			getApplication().startStreaming(fetchedBroadcast);
		}

		return new Result(result);
	}


	public boolean checkStopStreaming(Broadcast broadcast)
	{
		// If broadcast status is broadcasting, this will force stop the streaming.

		if(IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING.equals(broadcast.getStatus()))
		{
			return getApplication().stopStreaming(broadcast).isSuccess();
		}
		else if(getApplication().getStreamFetcherManager().isStreamRunning(broadcast)) {
			return getApplication().stopStreaming(broadcast).isSuccess();
		}
		else
		{
			// If broadcast status is stopped, this will return true.
			return true;
		}
	}

	public boolean waitStopStreaming(Broadcast broadcast, Boolean resultStopStreaming) {

		int i = 0;
		int waitPeriod = 250;
		// Broadcast status finished is not enough to be sure about broadcast's status.
		while (!IAntMediaStreamHandler.BROADCAST_STATUS_FINISHED.equals(getDataStore().get(broadcast.getStreamId()).getStatus()) && !resultStopStreaming.equals(true)) {
			try {
				i++;
				logger.info("Waiting for stop broadcast: {} Total wait time: {}ms", broadcast.getStreamId() , i*waitPeriod);

				Thread.sleep(waitPeriod);

				if(i > 20) {
					logger.warn("{} Stream ID broadcast could not be stopped. Total wait time: {}ms", broadcast.getStreamId() , i*waitPeriod);
					break;
				}
			} catch (InterruptedException e) {
				logger.error(e.getMessage());
				Thread.currentThread().interrupt();
			}

		}
		return true;
	}



	@Deprecated
	public Result addEndpoint(String id, String rtmpUrl) {
		boolean success = false;
		String message = null;
		try {
			if (validateStreamURL(rtmpUrl))
			{
				Endpoint endpoint = new Endpoint();
				endpoint.setRtmpUrl(rtmpUrl);
				endpoint.setType(ENDPOINT_GENERIC);

				success = getDataStore().addEndpoint(id, endpoint);
			}
		} catch (Exception e) {
			logger.error(ExceptionUtils.getStackTrace(e));
		}

		return new Result(success, message);
	}

	public Result addEndpoint(String id, Endpoint endpoint) {
		boolean success = false;
		String message = null;

		endpoint.setType(ENDPOINT_GENERIC);

		String endpointServiceId = endpoint.getEndpointServiceId();
		if (endpointServiceId == null || endpointServiceId.isEmpty()) {
			//generate custom endpoint invidual ID
			endpointServiceId = "custom"+RandomStringUtils.randomAlphabetic(6);
		}
		endpoint.setEndpointServiceId(endpointServiceId);


		try {
			if (validateStreamURL(endpoint.getRtmpUrl()))
			{
				success = getDataStore().addEndpoint(id, endpoint);
			}
		} catch (Exception e) {
			logger.error(ExceptionUtils.getStackTrace(e));
		}

		return new Result(success, endpointServiceId, message);
	}

	@Deprecated
	public Result removeEndpoint(String id, String rtmpUrl)
	{
		Endpoint endpoint = new Endpoint();
		endpoint.setRtmpUrl(rtmpUrl);
		endpoint.setType(ENDPOINT_GENERIC);

		boolean removed = getDataStore().removeEndpoint(id, endpoint, true);
		return new Result(removed);
	}

	public Result removeRTMPEndpoint(String id, Endpoint endpoint)
	{
		boolean removed = getDataStore().removeEndpoint(id, endpoint, false);

		return new Result(removed);

	}

	public  boolean isInSameNodeInCluster(String originAddress) {
		boolean isCluster = getAppContext().containsBean(IClusterNotifier.BEAN_NAME);
		return !isCluster || originAddress.equals(getServerSettings().getHostAddress());
	}

	public Result processRTMPEndpoint(String streamId, String originAddress, String rtmpUrl, boolean addEndpoint, int resolution) {
		Result result = new Result(false);
		if(isInSameNodeInCluster(originAddress))
		{
			if(addEndpoint) {
				result = getMuxAdaptor(streamId).startRtmpStreaming(rtmpUrl, resolution);
			}
			else {
				result = getMuxAdaptor(streamId).stopRtmpStreaming(rtmpUrl, resolution);
			}
		}
		else {
			logger.error("Please send a RTMP Endpoint request to the {} node or {} RTMP Endpoint in a stopped broadcast.", originAddress, addEndpoint ? "add" : "remove");
			result.setSuccess(false);
		}
		return result;
	}


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
				broadcastList.addAll(getDataStore().getBroadcastList(i*DataStore.MAX_ITEM_IN_ONE_LIST, DataStore.MAX_ITEM_IN_ONE_LIST,null,null,null,null));
			}

			StringBuilder insertQueryString = new StringBuilder();

			insertQueryString.append("DELETE FROM stalker_db.ch_links;");
			insertQueryString.append("DELETE FROM stalker_db.itv;");

			String fqdn = getServerSettings().getServerName();
			if (fqdn == null || fqdn.length() == 0) {
				fqdn = getServerSettings().getHostAddress();
			}

			int number = 1;
			for (Broadcast broadcast : broadcastList) {
				String cmd = "ffmpeg http://"+ fqdn + ":"+serverSettings.getDefaultHttpPort()+"/"
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
					vodList.addAll(getDataStore().getVodList(i*DataStore.MAX_ITEM_IN_ONE_LIST, DataStore.MAX_ITEM_IN_ONE_LIST, null, null, null, null));
				}

				String fqdn = getServerSettings().getServerName();
				if (fqdn == null || fqdn.length() == 0) {
					fqdn = getServerSettings().getHostAddress();
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
						String cmd = "ffmpeg http://"+ fqdn + ":"+serverSettings.getDefaultHttpPort()+"/"
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

	protected ProcessBuilderFactory getProcessBuilderFactory() {
		return processBuilderFactory;
	}


	public void setProcessBuilderFactory(ProcessBuilderFactory processBuilderFactory) {
		this.processBuilderFactory = processBuilderFactory;
	}


	public IWebRTCAdaptor getWebRTCAdaptor() {
		IWebRTCAdaptor adaptor = null;
		ApplicationContext appContext = getAppContext();
		if (appContext != null && appContext.containsBean(IWebRTCAdaptor.BEAN_NAME)) {
			Object webRTCAdaptorBean = appContext.getBean(IWebRTCAdaptor.BEAN_NAME);

			if(webRTCAdaptorBean != null) {
				adaptor = (IWebRTCAdaptor) webRTCAdaptorBean;
			}
		}
		return adaptor;
	}

	public Result addIPCamera(Broadcast stream) {

		Result connResult = new Result(false);

		if(validateStreamURL(stream.getIpAddr())) {
			logger.info("type {}", stream.getType());

			connResult = connectToCamera(stream);

			if (connResult.isSuccess()) {

				String authparam = stream.getUsername() + ":" + stream.getPassword() + "@";
				String rtspURLWithAuth = RTSP + authparam + connResult.getMessage().substring(RTSP.length());
				logger.info("rtsp url with auth: {}", rtspURLWithAuth);
				stream.setStreamUrl(rtspURLWithAuth);
				Date currentDate = new Date();
				long unixTime = currentDate.getTime();

				stream.setDate(unixTime);

				Broadcast savedBroadcast = saveBroadcast(stream, IAntMediaStreamHandler.BROADCAST_STATUS_CREATED, getScope().getName(), getDataStore(), getAppSettings().getListenerHookURL(), getServerSettings(), 0);

				connResult = getApplication().startStreaming(savedBroadcast);
				//if IP Camera is not being started while adding, do not record it to datastore
				if (!connResult.isSuccess())
				{
					getDataStore().delete(savedBroadcast.getStreamId());
				}
			}
		}
		else {
			connResult.setMessage("IP camera addr is not valid: " + stream.getIpAddr());
		}

		return connResult;
	}

	public Result addStreamSource(Broadcast stream) {

		Result result = new Result(false);

		IStatsCollector monitor = (IStatsCollector) getAppContext().getBean(IStatsCollector.BEAN_NAME);

		if(monitor.enoughResource())
		{
			if (stream.getType().equals(AntMediaApplicationAdapter.IP_CAMERA)) {
				result = addIPCamera(stream);
			}
			else if (stream.getType().equals(AntMediaApplicationAdapter.STREAM_SOURCE) ) {
				result = addSource(stream);
			}
			else{
				result.setMessage("Auto start query needs an IP camera or stream source.");
			}
		}
		else {

			logger.error("Stream Fetcher can not be created due to high cpu load/limit: {}/{} ram free/minfree:{}/{}",
					monitor.getCpuLoad(), monitor.getCpuLimit(), monitor.getFreeRam(), monitor.getMinFreeRamSize());
			result.setMessage("Resource usage is high");
			result.setErrorId(HIGH_CPU_ERROR);
		}

		return result;
	}

	public Result connectToCamera(Broadcast stream) {

		Result result = new Result(false);

		OnvifCamera onvif = new OnvifCamera();
		int connResult = onvif.connect(stream.getIpAddr(), stream.getUsername(), stream.getPassword());
		if (connResult == 0) {
			result.setSuccess(true);
			//set RTSP URL. This message is directly used in saving stream url to the datastore
			result.setMessage(onvif.getRTSPStreamURI());
		}else {
			//there is an error
			//set error code and send it
			result.setMessage("Could not connect to " + stream.getIpAddr() + " result:" + connResult);
			result.setErrorId(connResult);
			logger.info("Cannot connect to ip camera:{}", stream.getIpAddr());
		}

		return result;

	}


	/**
	 * Parse the string to check it's a valid url
	 * It can parse protocol://username:passwrd@server.fqdn/stream format as well
	 * @param url
	 * @return
	 */
	protected static boolean validateStreamURL(String url) {

		boolean ipAddrControl = false;
		String[] ipAddrParts = null;
		String serverAddr = url;

		if(url != null && (url.startsWith(HTTP) ||
				url.startsWith("https://") ||
				url.startsWith("rtmp://") ||
				url.startsWith("rtmps://") ||
				url.startsWith(RTSP))) {

			ipAddrParts = url.split("//");
			serverAddr = ipAddrParts[1];
			ipAddrControl=true;

		}
		if (serverAddr != null) {
			if (serverAddr.contains("@")){

				ipAddrParts = serverAddr.split("@");
				serverAddr = ipAddrParts[1];

			}
			if (serverAddr.contains(":")){

				ipAddrParts = serverAddr.split(":");
				serverAddr = ipAddrParts[0];

			}
			if (serverAddr.contains("/")){
				ipAddrParts = serverAddr.split("/");
				serverAddr = ipAddrParts[0];
			}

			if (logger.isInfoEnabled())  {
				logger.info("IP: {}", serverAddr.replaceAll("[\n|\r|\t]", "_"));
			}

			if(serverAddr.split("\\.").length == 4 && validateIPaddress(serverAddr)){
				ipAddrControl = true;
			}
		}
		return ipAddrControl;
	}

	protected static boolean validateIPaddress(String ipaddress)  {

		Pattern patternIP4 = Pattern.compile(IPV4_REGEX);
		Pattern patternLoopBack = Pattern.compile(LOOPBACK_REGEX);

		return patternIP4.matcher(ipaddress).matches() || patternLoopBack.matcher(ipaddress).matches() ;

	}

	public boolean checkStreamUrl (String url) {

		boolean streamUrlControl = false;
		String[] ipAddrParts = null;
		String ipAddr = null;

		if(url != null && (url.startsWith(HTTP) ||
				url.startsWith("https://") ||
				url.startsWith("rtmp://") ||
				url.startsWith("rtmps://") ||
				url.startsWith(RTSP) ||
				url.startsWith("udp://") ||
				url.startsWith("srt://")
		)) {
			streamUrlControl=true;
			ipAddrParts = url.split("//");
			ipAddr = ipAddrParts[1];

			if (ipAddr.contains("@")){

				ipAddrParts = ipAddr.split("@");
				ipAddr = ipAddrParts[1];

			}
			if (ipAddr.contains(":")){

				ipAddrParts = ipAddr.split(":");
				ipAddr = ipAddrParts[0];

			}
			if (ipAddr.contains("/")){

				ipAddrParts = ipAddr.split("/");
				ipAddr = ipAddrParts[0];

			}
		}
		return streamUrlControl;
	}

	protected Result addSource(Broadcast stream) {
		Result result=new Result(false);

		if(checkStreamUrl(stream.getStreamUrl())) {
			Date currentDate = new Date();
			long unixTime = currentDate.getTime();

			stream.setDate(unixTime);


			Broadcast savedBroadcast = saveBroadcast(stream, IAntMediaStreamHandler.BROADCAST_STATUS_CREATED, getScope().getName(), getDataStore(), getAppSettings().getListenerHookURL(), getServerSettings(), 0);

			result = getApplication().startStreaming(savedBroadcast);

			//if it's not started while adding, do not record it to datastore
			if(!result.isSuccess()) {
				getDataStore().delete(savedBroadcast.getStreamId());
				result.setErrorId(FETCHER_NOT_STARTED_ERROR);
			}
		}
		return result;
	}

	protected List<WebRTCClientStats> getWebRTCClientStatsList(int offset, int size, String streamId) {

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


	protected Result deleteVoD(String id) {
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
					String previewFilePath = voD.getPreviewFilePath();
					if(previewFilePath != null){
						File tmp = new File(previewFilePath);
						boolean resultThumbnail = Files.deleteIfExists(tmp.toPath());
						if (!resultThumbnail) {
							logger.warn("Preview is not deleted because it does not exist {}", tmp.getAbsolutePath());
						}
					}
					success = getDataStore().deleteVod(id);
					if (success) {
						message = "vod deleted";
					}


					String fileName = videoFile.getName();
					int indexOfFileExtension = fileName.lastIndexOf(".");
					String finalFileName = fileName.substring(0,indexOfFileExtension);

					//delete preview file if exists
					File previewFile = Muxer.getPreviewFile(getScope(), finalFileName, ".png");
					Files.deleteIfExists(previewFile.toPath());

					StorageClient storageClient = (StorageClient) appContext.getBean(StorageClient.BEAN_NAME);

					storageClient.delete(getAppSettings().getS3StreamsFolderPath() + File.separator + fileName);
					storageClient.delete(getAppSettings().getS3PreviewsFolderPath() + File.separator + finalFileName + ".png");

				}
				catch (Exception e) {
					logger.error(ExceptionUtils.getStackTrace(e));
				}
			}

		}
		return new Result(success, message);
	}

	protected Result deleteVoDs(String[] vodIds)
	{
		Result result = new Result(false);
		if(vodIds != null)
		{
			for (String id : vodIds)
			{
				result = deleteVoD(id);

				if (!result.isSuccess())
				{
					logger.warn("VoD:{} cannot be deleted and breaking the loop", id);
					break;
				}
			}
		}
		else
		{
			logger.warn("Requested deletion for VoD Ids is empty");
		}
		return result;
	}

	protected String getStreamsDirectory(String appScopeName) {
		return String.format("%s/webapps/%s/%s", System.getProperty("red5.root"), appScopeName, "streams");
	}

	protected Result uploadVoDFile(String fileName, InputStream inputStream) {
		boolean success = false;
		String message = "";
		String id= null;
		String appScopeName = getScope().getName();
		String fileExtension = FilenameUtils.getExtension(fileName);
		try {

			if ("mp4".equalsIgnoreCase(fileExtension) || "webm".equalsIgnoreCase(fileExtension) ||  "mov".equalsIgnoreCase(fileExtension) ||  "avi".equalsIgnoreCase(fileExtension)) {


				File streamsDirectory = new File(
						getStreamsDirectory(appScopeName));

				// if the directory does not exist, create it
				if (!streamsDirectory.exists()) {
					streamsDirectory.mkdirs();
				}
				String vodId = RandomStringUtils.randomNumeric(24);

				File savedFile = new File(String.format("%s/webapps/%s/%s", System.getProperty("red5.root"), appScopeName,
						"streams/" + vodId + "." + fileExtension));

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


					String relativePath = AntMediaApplicationAdapter.getRelativePath(path);

					VoD newVod = new VoD(fileName, "file", relativePath, fileName, unixTime, 0, Muxer.getDurationInMs(savedFile,fileName), fileSize,
							VoD.UPLOADED_VOD, vodId, null);

					id = getDataStore().addVod(newVod);

					if(id != null) {
						success = true;
						message = id;

						String vodFinishScript = getAppSettings().getVodUploadFinishScript();
						if (vodFinishScript != null && !vodFinishScript.isEmpty()) {
							getApplication().runScript(vodFinishScript + "  " + savedFile.getAbsolutePath());
						}

					}
				}
			}
			else {
				//this message has a wrong meaning on the other hand it has been used in the frontend(webpanel). Both sides should be updated 
				message = "notMp4File";
			}

		}
		catch (IOException iox) {
			logger.error(iox.getMessage());
		}


		return new Result(success, id, message);
	}


	protected Result synchUserVodList() {
		boolean result = false;
		int errorId = -1;
		String message = "";

		String vodFolder = getAppSettings().getVodFolder();

		logger.info("synch user vod list vod folder is {}", vodFolder);

		if (vodFolder != null && vodFolder.length() > 0) {

			result = getApplication().synchUserVoDFolder(null, vodFolder);
		}
		else {
			errorId = 404;
			message = "no VodD folder defined";
		}

		return new Result(result, message, errorId);
	}


	public MuxAdaptor getMuxAdaptor(String streamId)
	{
		AntMediaApplicationAdapter application = getApplication();
		MuxAdaptor selectedMuxAdaptor = null;

		if(application != null)
		{
			List<MuxAdaptor> muxAdaptors = application.getMuxAdaptors();
			for (MuxAdaptor muxAdaptor : muxAdaptors)
			{
				if (streamId.equals(muxAdaptor.getStreamId()))
				{
					selectedMuxAdaptor = muxAdaptor;
					break;
				}
			}
		}

		return selectedMuxAdaptor;
	}

	public boolean addRtmpMuxerToMuxAdaptor(String streamId, String rtmpURL) {
		MuxAdaptor muxAdaptor = getMuxAdaptor(streamId);
		boolean result = false;
		if (muxAdaptor != null) {
			//result = muxAdaptor.addRTMPEndpoint(rtmpURL);
		}

		return result;
	}

	@Nullable
	protected Mp4Muxer getMp4Muxer(MuxAdaptor muxAdaptor) {
		Mp4Muxer mp4Muxer = null;
		for (Muxer muxer : muxAdaptor.getMuxerList()) {
			if (muxer instanceof Mp4Muxer) {
				mp4Muxer = (Mp4Muxer) muxer;
			}
		}
		return mp4Muxer;
	}

	protected RecordMuxer startRecord(String streamId, RecordType recordType, int resolutionHeight) {
		MuxAdaptor muxAdaptor = getMuxAdaptor(streamId);
		if (muxAdaptor != null)
		{
			return muxAdaptor.startRecording(recordType, resolutionHeight);
		}

		return null;
	}

	/**
	 *
	 * @param streamId
	 * @param recordType
	 * @param resolutionHeight
	 * @return
	 */
	protected @Nullable RecordMuxer stopRecord(String streamId, RecordType recordType, int resolutionHeight)
	{
		MuxAdaptor muxAdaptor = getMuxAdaptor(streamId);

		if (muxAdaptor != null)
		{
			return muxAdaptor.stopRecording(recordType, resolutionHeight);
		}

		return null;
	}

	protected BroadcastStatistics getBroadcastStatistics(String id) {

		int totalRTMPViewer = -1;
		int totalWebRTCViewer = -1;
		int totalHLSViewer = -1;
		int totalDASHViewer = -1;
		if (id != null)
		{
			IBroadcastScope broadcastScope = getScope().getBroadcastScope(id);

			if (broadcastScope != null)	{
				totalRTMPViewer = broadcastScope.getConsumers().size();
			}

			Broadcast broadcast = getDataStore().get(id);
			if (broadcast != null) {
				totalHLSViewer = broadcast.getHlsViewerCount();
				totalDASHViewer = broadcast.getDashViewerCount();
				totalWebRTCViewer = broadcast.getWebRTCViewerCount();
			}
		}

		return new BroadcastStatistics(totalRTMPViewer, totalHLSViewer, totalWebRTCViewer,totalDASHViewer);
	}

	protected AppBroadcastStatistics getBroadcastTotalStatistics() {

		int totalWebRTCViewer = -1;
		int totalHLSViewer = -1;
		int totalDASHViewer = -1;

		if (getAppContext().containsBean(HlsViewerStats.BEAN_NAME)) {
			HlsViewerStats hlsViewerStats = (HlsViewerStats) getAppContext().getBean(HlsViewerStats.BEAN_NAME);
			totalHLSViewer = hlsViewerStats.getTotalViewerCount();
		}

		if (getAppContext().containsBean(DashViewerStats.BEAN_NAME)) {
			DashViewerStats dashViewerStats = (DashViewerStats) getAppContext().getBean(DashViewerStats.BEAN_NAME);
			totalDASHViewer = dashViewerStats.getTotalViewerCount();
		}


		IWebRTCAdaptor webRTCAdaptor = getWebRTCAdaptor();

		if(webRTCAdaptor != null) {
			totalWebRTCViewer = webRTCAdaptor.getNumberOfTotalViewers();
		}

		int activeBroadcastCount = (int)getDataStore().getActiveBroadcastCount();

		return new AppBroadcastStatistics(-1, totalHLSViewer, totalWebRTCViewer, totalDASHViewer, activeBroadcastCount);
	}


	protected Result getCameraErrorById(String streamId) {
		Result result = new Result(false);

		for (StreamFetcher camScheduler : getApplication().getStreamFetcherManager().getStreamFetcherList())
		{
			if (camScheduler.getStreamId().equals(streamId)) {
				result = camScheduler.getCameraError();
				break;
			}
		}

		return result;
	}



	public Result startStreamSource(String id)
	{
		Result result = new Result(false);
		Broadcast broadcast = getDataStore().get(id);

		if (broadcast != null)
		{
			if(broadcast.getStreamUrl() != null || Objects.equals(broadcast.getType(), AntMediaApplicationAdapter.PLAY_LIST))
			{
				result = getApplication().startStreaming(broadcast);
			}
			else if (Objects.equals(broadcast.getType(), AntMediaApplicationAdapter.IP_CAMERA))
			{
				//if streamURL is not defined before for IP Camera, connect to it again and define streamURL
				result = connectToCamera(broadcast);

				if (result.isSuccess())
				{
					String authparam = broadcast.getUsername() + ":" + broadcast.getPassword() + "@";
					String rtspURLWithAuth = RTSP + authparam + result.getMessage().substring(RTSP.length());
					logger.info("rtsp url with auth: {}", rtspURLWithAuth);
					broadcast.setStreamUrl(rtspURLWithAuth);

					result = getApplication().startStreaming(broadcast);
				}
			}
			else {
				result.setMessage("Stream url is null and it's not an IP camera to get stream url for id:" + id);
			}
		}
		else {
			result.setMessage("No Stream Exists with id:"+id);
		}
		return result;
	}



	public Result stopStreaming(String id)
	{
		Result result = new Result(false);
		Broadcast broadcast = getDataStore().get(id);
		if(broadcast != null) {
			result = getApplication().stopStreaming(broadcast);
		}
		return result;
	}


	protected String[] searchOnvifDevices() {

		String localIP = null;
		String[] list = null;
		Enumeration<NetworkInterface> interfaces = null;
		try {
			interfaces = NetworkInterface.getNetworkInterfaces();
		} catch (SocketException e) {
			// handle error
		}

		if (interfaces != null) {
			while (interfaces.hasMoreElements()) {
				NetworkInterface i = interfaces.nextElement();
				Enumeration<InetAddress> addresses = i.getInetAddresses();
				while (addresses.hasMoreElements() && (localIP == null || localIP.isEmpty())) {
					InetAddress address = addresses.nextElement();
					if (!address.isLoopbackAddress() && address.isSiteLocalAddress()) {
						localIP = address.getHostAddress();
					}
				}
			}
			logger.info("IP Address: {} " , localIP);
		}

		if (localIP != null) {

			String[] ipAddrParts = localIP.split("\\.");

			String ipAd = ipAddrParts[0] + "." + ipAddrParts[1] + "." + ipAddrParts[2] + ".";
			ArrayList<String> addressList = new ArrayList<>();

			for (int i = 2; i < 255; i++) {
				addressList.add(ipAd + i);

			}

			List<URL> onvifDevices = OnvifDiscovery.discoverOnvifDevices(true, addressList);


			list = getIPArray(onvifDevices);

		}

		return list;
	}

	protected String[] getOnvifDeviceProfiles(String id) {
		OnvifCamera camera = getApplication().getOnvifCamera(id);
		return camera.getProfiles();
	}


	public String[] getIPArray(List<URL> onvifDevices) {

		String[] list = null;
		if (onvifDevices != null)
		{
			list = new String[onvifDevices.size()];
			for (int i = 0; i < onvifDevices.size(); i++) {
				list[i] = StringUtils.substringBetween(onvifDevices.get(i).toString(), HTTP, "/");
				logger.info("IP Camera found: {}", onvifDevices.get(i));
			}
		}
		return list;
	}

	protected boolean moveRelative(String id, float valueX, float valueY, float valueZoom) {
		boolean result = false;
		OnvifCamera camera = getApplication().getOnvifCamera(id);
		if (camera != null) {
			result = camera.moveRelative(valueX, valueY, valueZoom);
		}
		return result;
	}

	protected boolean moveAbsolute(String id, float valueX, float valueY, float valueZoom) {
		boolean result = false;
		OnvifCamera camera = getApplication().getOnvifCamera(id);
		if (camera != null) {
			result = camera.moveAbsolute(valueX, valueY, valueZoom);
		}
		return result;
	}

	protected boolean moveContinous(String id, float valueX, float valueY, float valueZoom) {
		boolean result = false;
		OnvifCamera camera = getApplication().getOnvifCamera(id);
		if (camera != null) {
			result = camera.moveContinous(valueX, valueY, valueZoom);
		}
		return result;
	}

	protected List<TensorFlowObject> getDetectionList(String id, int offset, int size) {
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

	protected Object getToken (String streamId, long expireDate, String type, String roomId)
	{
		Token token = null;
		String message = "Define Stream ID, Token Type and Expire Date (unix time)";
		if(streamId != null && type != null && expireDate > 0) {

			ApplicationContext appContext = getAppContext();

			if(appContext != null && appContext.containsBean(ITokenService.BeanName.TOKEN_SERVICE.toString()))
			{
				ITokenService tokenService = (ITokenService)appContext.getBean(ITokenService.BeanName.TOKEN_SERVICE.toString());
				token = tokenService.createToken(streamId, expireDate, type, roomId);
				if(token != null)
				{
					if (getDataStore().saveToken(token)) {
						//returns token only everything is OK
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

	protected Object getJwtToken (String streamId, long expireDate, String type, String roomId)
	{
		Token token = null;
		String message = "Define Stream ID, Token Type and Expire Date (unix time)";

		if(streamId != null && type != null && expireDate > 0) {

			ApplicationContext appContext = getAppContext();

			if(appContext != null && appContext.containsBean(ITokenService.BeanName.TOKEN_SERVICE.toString()))
			{
				ITokenService tokenService = (ITokenService)appContext.getBean(ITokenService.BeanName.TOKEN_SERVICE.toString());
				token = tokenService.createJwtToken(streamId, expireDate, type, roomId);
				if(token != null)
				{
					return token;
				}
				else {
					message = "Cannot create JWT token. The problem can be ->  this is community edition or JWT stream key is not set or it's length is less than 32";
				}
			}
			else {
				message = "No token service in this app";
			}
		}

		return new Result(false, message);
	}

	protected Token validateToken (Token token) {
		Token validatedToken = null;

		if(token.getTokenId() != null) {

			validatedToken = getDataStore().validateToken(token);
		}

		return validatedToken;
	}

	protected Result revokeTokens (String streamId) {
		Result result = new Result(false);

		if(streamId != null) {

			result.setSuccess(getDataStore().revokeTokens(streamId));
		}

		return result;
	}

	public static boolean deleteConferenceRoom(String roomId, DataStore store) {

		if(roomId != null) {
			if (logger.isInfoEnabled()) {
				logger.info("Deleting conference room:{} from database ", roomId.replaceAll(REPLACE_CHARS, "_"));
			}
			return store.deleteConferenceRoom(roomId);
		}
		return false;
	}

	protected ConferenceRoom editConferenceRoom(ConferenceRoom room)
	{
		if(room != null && getDataStore().editConferenceRoom(room.getRoomId(), room)) {
			return room;
		}
		return null;
	}

	protected ConferenceRoom createConferenceRoom(ConferenceRoom room) {

		if(room != null) {

			if(room.getStartDate() == 0) {
				room.setStartDate(Instant.now().getEpochSecond());
			}

			if(room.getEndDate() == 0) {
				room.setEndDate(Instant.now().getEpochSecond() + 3600 );
			}

			if (getDataStore().createConferenceRoom(room)) {
				return room;
			}
		}
		return null;
	}

	protected VoD getVoD(String id) {
		VoD vod = null;
		if (id != null) {
			vod = getDataStore().getVoD(id);
		}
		if (vod == null) {
			vod = new VoD();
		}
		return vod;
	}

	public static Version getSoftwareVersion() {
		Version version = new Version();
		version.setVersionName(AntMediaApplicationAdapter.class.getPackage().getImplementationVersion());

		URL url = null;

		Class<RestServiceBase> clazz = RestServiceBase.class;
		String className = clazz.getSimpleName() + ".class";
		String classPath = clazz.getResource(className).toString();
		String manifestPath = classPath.substring(0, classPath.lastIndexOf("!") + 1) +
				"/META-INF/MANIFEST.MF";

		try {
			url = new URL(manifestPath);
		} catch (MalformedURLException e) {
			logger.error(e.getMessage());
		}

		Manifest manifest;

		try {
			if (url != null)
			{
				manifest = new Manifest(url.openStream());
				version.setBuildNumber(manifest.getMainAttributes().getValue(RestServiceBase.BUILD_NUMBER));
			}
			else {
				logger.error("url(META-INF/MANIFEST.MF) is null when getting software version");
			}
		} catch (IOException e) {
			logger.error(e.getMessage());
		}

		version.setVersionType(isEnterprise() ? RestServiceBase.ENTERPRISE_EDITION : RestServiceBase.COMMUNITY_EDITION);

		logger.info("Version Name {} Version Type {}", version.getVersionName(), version.getVersionType());
		return version;
	}

	public static boolean isEnterprise() {
		try {
			Class.forName("io.antmedia.enterprise.adaptive.EncoderAdaptor");
			return true;
		} catch (ClassNotFoundException e) {
			return false;
		}
	}

	/**
	 * Get the active streams in the room
	 *
	 * @param roomId: It's the id of the room
	 * @param streamId: The id of the room to be extracted from the list. It's generally the publisher stream id in websocket communication
	 * @param store: Datastore object to run the query
	 *
	 * @return null if there is no room recorded in the database, returns map filled with the active streams. Key is the streamId, value is the name
	 */
	public static Map<String,String> getRoomInfoFromConference(String roomId, String streamId, DataStore store){
		HashMap<String,String> streamDetailsMap = null;

		if (roomId != null)
		{
			ConferenceRoom conferenceRoom = store.getConferenceRoom(roomId);
			if (conferenceRoom == null) {
				logger.warn("There is no room with id:{}", roomId.replaceAll("[\n|\r|\t]", "_"));
				return streamDetailsMap;
			}
			streamDetailsMap = new HashMap<>();

			List<String> tempList = conferenceRoom.getRoomStreamList();
			if(tempList != null) {
				for (String tmpStreamId : tempList)
				{
					Broadcast broadcast = store.get(tmpStreamId);
					if (broadcast != null && broadcast.getStatus().equals(IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING))
					{
						streamDetailsMap.put(tmpStreamId, broadcast.getName());
					}
				}
				//remove the itself from the streamDetailsMap
				streamDetailsMap.remove(streamId);
			}
		}
		return streamDetailsMap;
	}

	public static boolean addStreamToConferenceRoom(String roomId,String streamId,DataStore store)
	{
		if(roomId!=null){
			List<String> roomStreamList = null;
			ConferenceRoom conferenceRoom = store.getConferenceRoom(roomId);
			if(conferenceRoom!=null){
				roomStreamList = conferenceRoom.getRoomStreamList();
				if(!roomStreamList.contains(streamId)){
					Broadcast broadcast=store.get(streamId);
					if(broadcast != null) {
						roomStreamList.add(streamId);
						conferenceRoom.setRoomStreamList(roomStreamList);
						store.editConferenceRoom(roomId, conferenceRoom);
						return true;
					}
				}
			}
		}
		return false;
	}


	public static synchronized boolean removeStreamFromRoom(String roomId, String streamId,DataStore store)
	{
		if (roomId != null)
		{
			//remove from room-stream list
			List<String> roomStreamList = null;
			ConferenceRoom conferenceRoom = store.getConferenceRoom(roomId);

			if(conferenceRoom != null)
			{
				roomStreamList = conferenceRoom.getRoomStreamList();

				// This is for the Conference Room list
				if(roomStreamList.contains(streamId))
				{
					roomStreamList.remove(streamId);
					conferenceRoom.setRoomStreamList(roomStreamList);
					store.editConferenceRoom(roomId, conferenceRoom);
					if (logger.isInfoEnabled()) {
						logger.info("stream:{} is removed from room:{} ", streamId.replaceAll(REPLACE_CHARS, "_"), roomId.replaceAll(REPLACE_CHARS, "_"));
					}
					return true;
				}
			}

		}
		return false;
	}

	public static String logFailedOperation(boolean enableRecording,String streamId,RecordType type){
		String id = streamId.replaceAll(REPLACE_CHARS, "_");
		if (enableRecording)
		{
			logger.warn("{} recording could not be started for stream: {}", type,id);
		}
		else
		{
			logger.warn("{} recording could not be stopped for stream: {}",type, id);
		}
		return id;
	}

	public Result enableRecordMuxing(String streamId, boolean enableRecording, String type, int resolutionHeight)
	{
		boolean result = false;
		String message = null;
		String status = (enableRecording)?"started":"stopped";
		String vodId = null;

		RecordType recordType = null;
		//type cannot be null
		if (type.equals(RecordType.MP4.toString()))
		{
			recordType = RecordType.MP4;
		}
		else if (type.equals(RecordType.WEBM.toString()))
		{
			recordType = RecordType.WEBM;
		}


		if (streamId != null && recordType != null) 
		{
			Broadcast broadcast = getDataStore().get(streamId);
			if (broadcast != null)
			{
				if(!broadcast.getStatus().equals(IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING))
				{
					if(recordType == RecordType.MP4) {
						broadcast.setMp4Enabled(enableRecording ? RECORD_ENABLE : RECORD_DISABLE);
					}
					else {
						broadcast.setWebMEnabled(enableRecording ? RECORD_ENABLE : RECORD_DISABLE);
					}
					result = true;
				}
				else {
					boolean isAlreadyRecording = isAlreadyRecording(streamId, recordType, resolutionHeight);
					//start recording and there is no active recording or stop recording and there is active recording
					if (enableRecording != isAlreadyRecording)
					{
						result = true;
						RecordMuxer muxer = null;

						if (isInSameNodeInCluster(broadcast.getOriginAdress())) 
						{
							if (enableRecording)
							{
								muxer = startRecord(streamId, recordType, resolutionHeight);
								if (muxer != null) {
									vodId = RandomStringUtils.randomAlphanumeric(24);
									muxer.setVodId(vodId);
									message = Long.toString(muxer.getCurrentVoDTimeStamp());
									logger.warn("{} recording is {} for stream: {}", type,status,streamId);
								}

							}
							else
							{
								muxer = stopRecord(streamId, recordType, resolutionHeight);
								if (muxer != null) {
									vodId = muxer.getVodId();
									message = Long.toString(muxer.getCurrentVoDTimeStamp());
								}
							}

							//Check process status result
							if (muxer == null)
							{
								result = false;
								logFailedOperation(enableRecording, streamId, recordType);
								message= recordType +" recording couldn't be " + status;
							}
						}
						else
						{
							message="Please send " + type + " recording request to " + broadcast.getOriginAdress() + " node or send request in a stopped status.";
							result = false;
						}
					}
					else {
						if(enableRecording) {
							message = type+" recording couldn't be started";
						}
						else {
							message = type+" recording couldn't be stopped";
						}
						result = false;
					}

				}
				// If record process works well then change record status in DB
				if (result)
				{
					if (recordType == RecordType.WEBM)
					{
						result = getDataStore().setWebMMuxing(streamId, enableRecording ? RECORD_ENABLE : RECORD_DISABLE);
					}
					else if (recordType == RecordType.MP4)
					{
						result = getDataStore().setMp4Muxing(streamId, enableRecording ? RECORD_ENABLE : RECORD_DISABLE);
					}
				}
			}
		}
		else
		{
			message = "No stream for this id: " + streamId + " or unexpected record type. Record type is "+ recordType;
		}

		return new Result(result, vodId, message);
	}

	public boolean isAlreadyRecording(String streamId, RecordType recordType, int resolutionHeight) {
		MuxAdaptor muxAdaptor = getMuxAdaptor(streamId);
		return muxAdaptor != null && muxAdaptor.isAlreadyRecording(recordType, resolutionHeight);
	}

	public Result importVoDs(String directory) {
		return getApplication().importVoDFolder(directory);
	}

	public Result unlinksVoD(String directory) {
		return getApplication().unlinksVoD(directory);
	}

}