package io.antmedia.rest;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.annotation.Nullable;
import javax.servlet.ServletContext;
import javax.ws.rs.FormParam;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.red5.server.api.scope.IScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.AppSettings;
import io.antmedia.datastore.db.DataStore;
import io.antmedia.datastore.db.DataStoreFactory;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.datastore.db.types.Endpoint;
import io.antmedia.datastore.db.types.VoD;
import io.antmedia.ipcamera.OnvifCamera;
import io.antmedia.rest.BroadcastRestService.ProcessBuilderFactory;
import io.antmedia.rest.model.Result;
import io.antmedia.social.endpoint.VideoServiceEndpoint;
import io.antmedia.streamsource.StreamFetcher;
import io.antmedia.webrtc.api.IWebRTCAdaptor;
import io.swagger.annotations.ApiParam;

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
	
	public static final String HTTP = "http://";
	public static final String RTSP = "rtsp://";
	
	protected static Logger logger = LoggerFactory.getLogger(RestServiceBase.class);

	private static String hostaddress;
	
	private ProcessBuilderFactory processBuilderFactory = null;

	@Context
	protected ServletContext servletContext;
	protected DataStoreFactory dataStoreFactory;
	private DataStore dbStore;
	protected ApplicationContext appCtx;
	protected IScope scope;
	protected AntMediaApplicationAdapter appInstance;
	
	private AppSettings appSettings;

	protected boolean addSocialEndpoints(Broadcast broadcast, String socialEndpointIds) {	
		boolean success = true;
		Map<String, VideoServiceEndpoint> endPointServiceList = getApplication().getVideoServiceEndpoints();

		String[] endpointIds = socialEndpointIds.split(",");

		if (endPointServiceList != null) {
			for (String endpointId : endpointIds) {
				VideoServiceEndpoint videoServiceEndpoint = endPointServiceList.get(endpointId);
				if (videoServiceEndpoint != null) {
					success = success && addSocialEndpoint(broadcast, videoServiceEndpoint);
				}
				else {
					success = false;
					String warning = endpointId + " endpoint does not exist in this app.";
					logger.warn(warning);
				}
			}
		}
		else {
			success = false;
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

	protected static String getHostAddress() {

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

	protected AppSettings getAppSettings() {
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
	
	public Result deleteBroadcast(String id) {
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
	
	public Result updateBroadcast(String streamId, String name, String description, String socialNetworksToPublish) {

		boolean result = getDataStore().updateName(streamId, name, description);
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
	
	public Result addSocialEndpoint(String id, String endpointServiceId) 
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
	
	public Result revokeSocialNetwork(String endpointId) {
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
			Endpoint endpoint = new Endpoint();
			endpoint.setRtmpUrl(rtmpUrl);
			endpoint.type = "generic";

			success = getDataStore().addEndpoint(id, endpoint);
		} catch (Exception e) {
			logger.error(ExceptionUtils.getStackTrace(e));
		}

		return new Result(success, message);
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
	
	public ProcessBuilderFactory getProcessBuilderFactory() {
		return processBuilderFactory;
	}


	public void setProcessBuilderFactory(ProcessBuilderFactory processBuilderFactory) {
		this.processBuilderFactory = processBuilderFactory;
	}
	
	
	public IWebRTCAdaptor getWebRTCAdaptor() {
		IWebRTCAdaptor adaptor = null;
		ApplicationContext appContext = getAppContext();
		if (appContext != null && appContext.containsBean(IWebRTCAdaptor.BEAN_NAME)) {
			adaptor = (IWebRTCAdaptor) appContext.getBean(IWebRTCAdaptor.BEAN_NAME);
		}
		return adaptor;
	}
	
	public Result addIPCamera(Broadcast stream, String socialEndpointIds) {

		Result connResult = new Result(false);

		if(checkIPCamAddr(stream.getIpAddr())) {
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
				stream.setStatus(AntMediaApplicationAdapter.BROADCAST_STATUS_CREATED);

				String id = getDataStore().save(stream);


				if (id.length() > 0) {
					Broadcast newCam = getDataStore().get(stream.getStreamId());
					if (socialEndpointIds != null && socialEndpointIds.length()>0) {
						addSocialEndpoints(newCam, socialEndpointIds);
					}

					StreamFetcher streamFetcher = getApplication().startStreaming(newCam);
					//if IP Camera is not being started while adding, do not record it to datastore
					if (streamFetcher == null) {
						getDataStore().delete(stream.getStreamId());
						connResult.setSuccess(false);
						connResult.setErrorId(FETCHER_NOT_STARTED_ERROR);
					}
				}
			}
		}

		return connResult;
	}
	
	public static Result connectToCamera(Broadcast stream) {

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
	
	
	public static boolean checkIPCamAddr (String url) {

		boolean ipAddrControl = false;
		String[] ipAddrParts = null;
		String ipAddr = url;

		if(url != null && (url.startsWith(HTTP) ||
				url.startsWith("https://") ||
				url.startsWith("rtmp://") ||
				url.startsWith("rtmps://") ||
				url.startsWith(RTSP))) {

			ipAddrParts = url.split("//");
			ipAddr = ipAddrParts[1];
			ipAddrControl=true;

		}
		if (ipAddr != null) {
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
			logger.info("IP: {}", ipAddr);

			if(ipAddr.split("\\.").length == 4 && validateIPaddress(ipAddr)){
				ipAddrControl = true;
			}
		}
		return ipAddrControl;
	}
	
	public static boolean validateIPaddress(String ipaddress)  {
		logger.info("inside check validateIPaddress{}", ipaddress);

		final String IPV4_REGEX = "(([0-1]?[0-9]{1,2}\\.)|(2[0-4][0-9]\\.)|(25[0-5]\\.)){3}(([0-1]?[0-9]{1,2})|(2[0-4][0-9])|(25[0-5]))";
		final String loopback_REGEX = "^localhost$|^127(?:\\.[0-9]+){0,2}\\.[0-9]+$|^(?:0*\\:)*?:?0*1$";

		Pattern patternIP4 = Pattern.compile(IPV4_REGEX);
		Pattern patternLoopBack = Pattern.compile(loopback_REGEX);

		return patternIP4.matcher(ipaddress).matches() || patternLoopBack.matcher(ipaddress).matches() ;

	}
	
	public static boolean checkStreamUrl (String url) {

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
	
	public Result addSource(Broadcast stream, String socialEndpointIds) {
		Result result=new Result(false);

		if(checkStreamUrl(stream.getStreamUrl())) {
			Date currentDate = new Date();
			long unixTime = currentDate.getTime();

			stream.setDate(unixTime);
			stream.setStatus(AntMediaApplicationAdapter.BROADCAST_STATUS_CREATED);

			String id = getDataStore().save(stream);
			if (id.length() > 0) {

				Broadcast newSource = getDataStore().get(stream.getStreamId());
				if (socialEndpointIds != null && socialEndpointIds.length()>0) {
					addSocialEndpoints(newSource, socialEndpointIds);
				}

				StreamFetcher streamFetcher = getApplication().startStreaming(newSource);

				
				result.setMessage(id);

				//if it's not started while adding, do not record it to datastore
				if (streamFetcher != null) {
					result.setSuccess(true);
				}
				else {
					getDataStore().delete(stream.getStreamId());
					result.setErrorId(FETCHER_NOT_STARTED_ERROR);
					result.setSuccess(false);
				}
			}

		}
		return result;
	}


}
