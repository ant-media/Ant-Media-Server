package io.antmedia.rest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
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
import io.antmedia.IApplicationAdaptorFactory;
import io.antmedia.datastore.db.DataStore;
import io.antmedia.datastore.db.DataStoreFactory;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.datastore.db.types.ConferenceRoom;
import io.antmedia.datastore.db.types.Endpoint;
import io.antmedia.datastore.db.types.SocialEndpointChannel;
import io.antmedia.datastore.db.types.SocialEndpointCredentials;
import io.antmedia.datastore.db.types.TensorFlowObject;
import io.antmedia.datastore.db.types.Token;
import io.antmedia.datastore.db.types.VoD;
import io.antmedia.ipcamera.OnvifCamera;
import io.antmedia.ipcamera.onvifdiscovery.OnvifDiscovery;
import io.antmedia.muxer.Mp4Muxer;
import io.antmedia.muxer.MuxAdaptor;
import io.antmedia.muxer.Muxer;
import io.antmedia.rest.BroadcastRestService.BroadcastStatistics;
import io.antmedia.rest.BroadcastRestService.ProcessBuilderFactory;
import io.antmedia.rest.model.Interaction;
import io.antmedia.rest.model.Result;
import io.antmedia.rest.model.Version;
import io.antmedia.security.ITokenService;
import io.antmedia.settings.ServerSettings;
import io.antmedia.social.LiveComment;
import io.antmedia.social.endpoint.PeriscopeEndpoint;
import io.antmedia.social.endpoint.VideoServiceEndpoint;
import io.antmedia.social.endpoint.VideoServiceEndpoint.DeviceAuthParameters;
import io.antmedia.statistic.IStatsCollector;
import io.antmedia.storage.StorageClient;
import io.antmedia.storage.StorageClient.FileType;
import io.antmedia.streamsource.StreamFetcher;
import io.antmedia.webrtc.api.IWebRTCAdaptor;

public abstract class RestServiceBase {

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

	public static final int MP4_ENABLE = 1;
	public static final int MP4_DISABLE = -1;
	public static final int MP4_NO_SET = 0;

	public static final int HIGH_CPU_ERROR = -3;
	public static final int FETCHER_NOT_STARTED_ERROR = -4;
	public static final int INVALID_STREAM_NAME_ERROR = -5;

	public static final String HTTP = "http://";
	public static final String RTSP = "rtsp://";

	protected static Logger logger = LoggerFactory.getLogger(RestServiceBase.class);

	private ProcessBuilderFactory processBuilderFactory = null;

	//TODO: This REGEX does not fully match 10.10.157.200. It ignores the last 0 it matches 10.10.157.20 and it cause problem in replacements
	public static final String IPV4_REGEX = "(([0-1]?[0-9]{1,2}\\.)|(2[0-4][0-9]\\.)|(25[0-5]\\.)){3}(([0-1]?[0-9]{1,2})|(2[0-4][0-9])|(25[0-5]))";

	public static final String LOOPBACK_REGEX = "^localhost$|^127(?:\\.[0-9]+){0,2}\\.[0-9]+$|^(?:0*\\:)*?:?0*1$";


	@Context
	protected ServletContext servletContext;
	protected DataStoreFactory dataStoreFactory;
	private DataStore dbStore;
	protected ApplicationContext appCtx;
	protected IScope scope;
	protected AntMediaApplicationAdapter appInstance;

	private AppSettings appSettings;

	private ServerSettings serverSettings;

	protected boolean addSocialEndpoints(Broadcast broadcast, String socialEndpointIds) {	
		boolean success = false;
		Map<String, VideoServiceEndpoint> endPointServiceList = getApplication().getVideoServiceEndpoints();

		String[] endpointIds = socialEndpointIds.split(",");

		if (endPointServiceList != null) {
			for (String endpointId : endpointIds) {
				VideoServiceEndpoint videoServiceEndpoint = endPointServiceList.get(endpointId);
				if (videoServiceEndpoint != null) {
					success = addSocialEndpoint(broadcast, videoServiceEndpoint);
				}
				else {
					logger.warn("{} endpoint does not exist in this app.", endpointId);
				}
			}
		}
		else {
			logger.warn("endPointServiceList is null");
		}
		return success;
	}

	protected boolean addSocialEndpoint(Broadcast broadcast, VideoServiceEndpoint socialEndpoint) {
		Endpoint endpoint;
		try {
			endpoint = socialEndpoint.createBroadcast(broadcast.getName(),
					broadcast.getDescription(), broadcast.getStreamId(), broadcast.isIs360(), broadcast.isPublicStream(),
					720, true);
			return getDataStore().addEndpoint(broadcast.getStreamId(), endpoint);

		} catch (Exception e) {
			logger.error(ExceptionUtils.getStackTrace(e));
		}
		return false;
	}

	public void setAppCtx(ApplicationContext appCtx) {
		this.appCtx = appCtx;
	}

	@Nullable
	protected ApplicationContext getAppContext() {
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
				appInstance = ((IApplicationAdaptorFactory) appContext.getBean(AntMediaApplicationAdapter.BEAN_NAME)).getAppAdaptor();
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
			dataStoreFactory = (DataStoreFactory) ctxt.getBean("dataStoreFactory");
		}
		return dataStoreFactory;
	}


	public void setDataStoreFactory(DataStoreFactory dataStoreFactory) {
		this.dataStoreFactory = dataStoreFactory;
	}

	protected Map<String, VideoServiceEndpoint> getEndpointList() {
		return getApplication().getVideoServiceEndpoints();
	}

	public Broadcast createBroadcastWithStreamID(Broadcast broadcast) {

		return saveBroadcast(broadcast, AntMediaApplicationAdapter.BROADCAST_STATUS_CREATED, getScope().getName(),
				getDataStore(), getAppSettings().getListenerHookURL(), getServerSettings().getServerName(), getServerSettings().getHostAddress());
	}

	public static Broadcast saveBroadcast(Broadcast broadcast, String status, String scopeName, DataStore dataStore,
			String settingsListenerHookURL, String fqdn, String hostAddress) {

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
			fqdn = hostAddress; 
		}
		broadcast.setOriginAdress(hostAddress);

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

		if (id != null) {
			Broadcast broacast = getDataStore().get(id);
			stopResult = stopBroadcastInternal(broacast);

			result.setSuccess(getDataStore().delete(id));

			if(result.isSuccess() && stopResult) {
				logger.info("brodcast {} is deleted and stopped successfully", broacast.getStreamId());
				result.setMessage("brodcast is deleted and stopped successfully");

			}
			else if(result.isSuccess() && !stopResult) {
				logger.info("brodcast {} is deleted but could not stopped", broacast);
				result.setMessage("brodcast is deleted but could not stopped ");
			}

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

	protected Result updateBroadcast(String streamId, Broadcast broadcast, String socialNetworksToPublish) {

		boolean result = getDataStore().updateBroadcastFields(streamId, broadcast);
		StringBuilder message = new StringBuilder();
		int errorId = 0;
		if (result) {
			Broadcast fetchedBroadcast = getDataStore().get(streamId);
			getDataStore().removeAllEndpoints(fetchedBroadcast.getStreamId());

			if (socialNetworksToPublish != null && socialNetworksToPublish.length() > 0) {
				String[] socialNetworks = socialNetworksToPublish.split(",");

				for (String networkName : socialNetworks) {
					Result addSocialEndpoint = addSocialEndpoint(streamId, networkName);
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
	 * Update Stream Source or IP Camera info
	 * @param broadcast
	 * @param socialNetworksToPublish
	 * @return
	 */
	protected Result updateStreamSource(String streamId, Broadcast broadcast, String socialNetworksToPublish) {

		boolean result = false;
		logger.debug("update cam info for stream {}", broadcast.getStreamId());

		if( checkStreamUrl(broadcast.getStreamUrl()) && broadcast.getStatus()!=null){
			getApplication().stopStreaming(broadcast);
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				logger.error(e.getMessage());
				Thread.currentThread().interrupt();
			}
			if(broadcast.getType().equals(AntMediaApplicationAdapter.IP_CAMERA)) {
				String rtspURL = connectToCamera(broadcast).getMessage();

				if (rtspURL != null) {

					String authparam = broadcast.getUsername() + ":" + broadcast.getPassword() + "@";
					String rtspURLWithAuth = RTSP + authparam + rtspURL.substring(RTSP.length());
					logger.info("new RTSP URL: {}" , rtspURLWithAuth);
					broadcast.setStreamUrl(rtspURLWithAuth);
				}
			}

			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				logger.error(e.getMessage());
				Thread.currentThread().interrupt();
			}

			result = getDataStore().updateBroadcastFields(streamId, broadcast);
			Broadcast fetchedBroadcast = getDataStore().get(broadcast.getStreamId());
			getDataStore().removeAllEndpoints(fetchedBroadcast.getStreamId());

			if (socialNetworksToPublish != null && socialNetworksToPublish.length() > 0) {
				addSocialEndpoints(fetchedBroadcast, socialNetworksToPublish);
			}

			getApplication().startStreaming(broadcast);
		}
		return new Result(result);
	}

	protected Result addSocialEndpoint(String id, String endpointServiceId) 
	{
		Broadcast broadcast = lookupBroadcast(id);

		boolean success = false;
		String message = "";
		if (broadcast != null) 
		{
			success = addSocialEndpoints(broadcast, endpointServiceId);
			if(!success) {
				message  = endpointServiceId+" endpoint can not be added to "+id;
			}
		}
		return new Result(success, message);
	}

	protected Result revokeSocialNetwork(String endpointId) {
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

	public Result addEndpoint(String id, String rtmpUrl) {
		boolean success = false;
		String message = null;
		try {
			if (validateStreamURL(rtmpUrl)) 
			{
				Endpoint endpoint = new Endpoint();
				endpoint.setRtmpUrl(rtmpUrl);
				endpoint.type = "generic";

				success = getDataStore().addEndpoint(id, endpoint);
			}
		} catch (Exception e) {
			logger.error(ExceptionUtils.getStackTrace(e));
		}

		return new Result(success, message);
	}
	
	
	public Result removeEndpoint(String id, String rtmpUrl) 
	{
		Endpoint endpoint = new Endpoint();
		endpoint.setRtmpUrl(rtmpUrl);
		endpoint.type = "generic";
		
		boolean removed = getDataStore().removeEndpoint(id, endpoint);
		return new Result(removed);
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
				broadcastList.addAll(getDataStore().getBroadcastList(i*DataStore.MAX_ITEM_IN_ONE_LIST, DataStore.MAX_ITEM_IN_ONE_LIST));
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
					vodList.addAll(getDataStore().getVodList(i*DataStore.MAX_ITEM_IN_ONE_LIST, DataStore.MAX_ITEM_IN_ONE_LIST, null, null));
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

	public Result addIPCamera(Broadcast stream, String socialEndpointIds) {

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

				Broadcast savedBroadcast = saveBroadcast(stream, AntMediaApplicationAdapter.BROADCAST_STATUS_CREATED, getScope().getName(), getDataStore(), getAppSettings().getListenerHookURL(), getServerSettings().getServerName(), getServerSettings().getHostAddress());

				if (socialEndpointIds != null && socialEndpointIds.length()>0) {
					addSocialEndpoints(savedBroadcast, socialEndpointIds);
				}

				StreamFetcher streamFetcher = getApplication().startStreaming(savedBroadcast);
				//if IP Camera is not being started while adding, do not record it to datastore
				if (streamFetcher == null) {
					getDataStore().delete(savedBroadcast.getStreamId());
					connResult.setSuccess(false);
					connResult.setErrorId(FETCHER_NOT_STARTED_ERROR);
				}

			}
		}

		return connResult;
	}

	public Result addStreamSource(Broadcast stream, String socialEndpointIds) {

		Result result = new Result(false);


		IStatsCollector monitor = (IStatsCollector) getAppContext().getBean(IStatsCollector.BEAN_NAME);

		if(monitor.enoughResource()) 
		{
			if (stream.getType().equals(AntMediaApplicationAdapter.IP_CAMERA)) {
				result = addIPCamera(stream, socialEndpointIds);
			}
			else if (stream.getType().equals(AntMediaApplicationAdapter.STREAM_SOURCE) ) {
				result = addSource(stream, socialEndpointIds);
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
			//it means no connection or authentication error
			//set RTMP URL
			result.setMessage(onvif.getRTSPStreamURI());
		}else {
			//there is an error
			//set error code and send it
			result.setMessage(String.valueOf(connResult));
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
				url.startsWith(RTSP))) {
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
			if(ipAddr.split("\\.").length == 4 && !validateIPaddress(ipAddr)){
				streamUrlControl = false;
			}
		}
		return streamUrlControl;
	}

	protected Result addSource(Broadcast stream, String socialEndpointIds) {
		Result result=new Result(false);

		if(checkStreamUrl(stream.getStreamUrl())) {
			Date currentDate = new Date();
			long unixTime = currentDate.getTime();

			stream.setDate(unixTime);


			Broadcast savedBroadcast = saveBroadcast(stream, AntMediaApplicationAdapter.BROADCAST_STATUS_CREATED, getScope().getName(), getDataStore(), getAppSettings().getListenerHookURL(), getServerSettings().getServerName(), getServerSettings().getHostAddress());

			if (socialEndpointIds != null && socialEndpointIds.length()>0) {
				addSocialEndpoints(savedBroadcast, socialEndpointIds);
			}

			StreamFetcher streamFetcher = getApplication().startStreaming(savedBroadcast);


			result.setMessage(savedBroadcast.getStreamId());

			//if it's not started while adding, do not record it to datastore
			if (streamFetcher != null) {
				result.setSuccess(true);
			}
			else {
				getDataStore().delete(savedBroadcast.getStreamId());
				result.setErrorId(FETCHER_NOT_STARTED_ERROR);
				result.setSuccess(false);
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

	protected Object getDeviceAuthParameters(String serviceName) {
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

	protected boolean isClientIdMissing(VideoServiceEndpoint videoServiceEndpoint, String clientId, String clientSecret) {
		boolean result = false;
		if ((videoServiceEndpoint != null) && 
				(clientId == null || clientSecret == null || 
				clientId.length() == 0 || clientSecret.length() == 0)) {
			result = true;
		}
		return result;
	}

	protected Result checkDeviceAuthStatus(String userCode) {
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

	protected boolean startMp4Muxing(String streamId) {
		boolean result = false;
		MuxAdaptor muxAdaptor = getMuxAdaptor(streamId);
		if (muxAdaptor != null) 
		{
			result = muxAdaptor.startRecording();
		}

		return result;
	}

	protected boolean stopMp4Muxing(String streamId) 
	{
		boolean result = false;
		MuxAdaptor muxAdaptor = getMuxAdaptor(streamId);

		if (muxAdaptor != null) 
		{
			result = muxAdaptor.stopRecording();
		}

		return result;
	}

	protected List<VideoServiceEndpoint> getEndpointsHavingErrorList(){
		return getApplication().getVideoServiceEndpointsHavingError();
	}


	protected BroadcastStatistics getBroadcastStatistics(String id) {

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

	protected List<SocialEndpointCredentials> getSocialEndpoints(int offset, int size) {
		List<SocialEndpointCredentials> endPointCredentials = new ArrayList<>();
		Map<String, VideoServiceEndpoint> endPointMap = getEndpointList();
		if (endPointMap != null) {
			for (VideoServiceEndpoint videoServiceEndpoint : endPointMap.values()) {
				endPointCredentials.add(videoServiceEndpoint.getCredentials());
			}
		}
		return endPointCredentials;
	}

	protected SocialEndpointChannel getSocialNetworkChannel(String endpointId) {
		Map<String, VideoServiceEndpoint> endPointMap = getEndpointList();
		VideoServiceEndpoint endPoint = endPointMap.get(endpointId);
		SocialEndpointChannel channel = null;
		if (endPoint != null) {
			channel = endPoint.getChannel();
		}
		return channel;
	}

	protected List<SocialEndpointChannel> getSocialNetworkChannelList(String endpointId, String type) {

		Map<String, VideoServiceEndpoint> endPointMap = getEndpointList();
		VideoServiceEndpoint endPoint = endPointMap.get(endpointId);
		List<SocialEndpointChannel>  channelList = null;
		if (endPoint != null) {
			channelList = endPoint.getChannelList();
		}
		return channelList;
	}


	protected Result setSocialNetworkChannelList(String endpointId, String type, String channelId) {
		boolean result = false;
		Map<String, VideoServiceEndpoint> endPointMap = getEndpointList();

		VideoServiceEndpoint endPoint = endPointMap.get(endpointId);

		if (endPoint != null) {
			result = endPoint.setActiveChannel(type, channelId);
		}
		return new Result(result, null);
	}

	protected Result getCameraError(String id) {
		Result result = new Result(true);

		for (StreamFetcher camScheduler : getApplication().getStreamFetcherManager().getStreamFetcherList()) {
			if (camScheduler.getStream().getIpAddr().equals(id)) {
				result = camScheduler.getCameraError();
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
			if(broadcast.getStreamUrl() == null && broadcast.getType().equals(AntMediaApplicationAdapter.IP_CAMERA)) 
			{
				//if streamURL is not defined before for IP Camera, connect to it again and define streamURL
				Result connResult = connectToCamera(broadcast);

				if (connResult.isSuccess()) 
				{
					String authparam = broadcast.getUsername() + ":" + broadcast.getPassword() + "@";
					String rtspURLWithAuth = RTSP + authparam + connResult.getMessage().substring(RTSP.length());
					logger.info("rtsp url with auth: {}", rtspURLWithAuth);
					broadcast.setStreamUrl(rtspURLWithAuth);
				}
			}

			if(getApplication().startStreaming(broadcast) != null) {

				result.setSuccess(true);
			}
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

			list = new String[onvifDevices.size()];

			if (!onvifDevices.isEmpty()) {

				for (int i = 0; i < onvifDevices.size(); i++) {

					list[i] = StringUtils.substringBetween(onvifDevices.get(i).toString(), HTTP, "/");
				}
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

	protected Result getViewerCountFromEndpoint(String endpointServiceId, String streamId) 
	{
		VideoServiceEndpoint videoServiceEndPoint = getApplication().getVideoServiceEndPoint(endpointServiceId);
		long liveViews = 0;
		if (videoServiceEndPoint != null) {
			liveViews = videoServiceEndPoint.getLiveViews(streamId);
		}
		return new Result(true, String.valueOf(liveViews));
	}

	protected Result getLiveCommentsCount(String endpointServiceId, String streamId) {
		VideoServiceEndpoint videoServiceEndPoint = getApplication().getVideoServiceEndPoint(endpointServiceId);
		int commentCount = 0;
		if (videoServiceEndPoint != null) {
			commentCount = videoServiceEndPoint.getTotalCommentsCount(streamId);
		}
		return new Result(true, String.valueOf(commentCount));
	}

	protected Interaction getInteractionFromEndpoint(String endpointServiceId, String streamId) {
		Interaction interaction = null;
		VideoServiceEndpoint videoServiceEndPoint = getApplication().getVideoServiceEndPoint(endpointServiceId);
		if (videoServiceEndPoint != null) {
			interaction = videoServiceEndPoint.getInteraction(streamId);
		}
		return interaction;
	}

	protected List<LiveComment> getLiveCommentsFromEndpoint(String endpointServiceId, String streamId, int offset, int batch) 
	{

		VideoServiceEndpoint videoServiceEndPoint = getApplication().getVideoServiceEndPoint(endpointServiceId);
		List<LiveComment> liveComment = null;
		if (videoServiceEndPoint != null) {
			liveComment = videoServiceEndPoint.getComments(streamId, offset, batch);
		}
		return liveComment;
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
		String message = "Define stream Id and Expire Date (unix time)";
		if(streamId != null && expireDate > 0) {

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

	protected boolean deleteConferenceRoom(String roomName) {

		if(roomName != null) {
			return getDataStore().deleteConferenceRoom(roomName);
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

		URLClassLoader cl = (URLClassLoader) AntMediaApplicationAdapter.class.getClassLoader();
		URL url = cl.findResource("META-INF/MANIFEST.MF");
		Manifest manifest;
		try {
			manifest = new Manifest(url.openStream());
			version.setBuildNumber(manifest.getMainAttributes().getValue(RestServiceBase.BUILD_NUMBER));
		} catch (IOException e) {
			//No need to implement
		}

		version.setVersionType(isEnterprise() ? RestServiceBase.ENTERPRISE_EDITION : RestServiceBase.COMMUNITY_EDITION);

		logger.debug("Version Name {} Version Type {}", version.getVersionName(), version.getVersionType());
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


}
