package io.antmedia;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.bytedeco.javacpp.avcodec.AVPacket;
import org.bytedeco.javacpp.avformat.AVFormatContext;
import org.red5.server.api.scope.IBroadcastScope;
import org.red5.server.api.scope.IScope;
import org.red5.server.api.stream.IBroadcastStream;
import org.red5.server.api.stream.IClientBroadcastStream;
import org.red5.server.api.stream.IPlayItem;
import org.red5.server.api.stream.IStreamCapableConnection;
import org.red5.server.api.stream.IStreamPublishSecurity;
import org.red5.server.api.stream.IStreamService;
import org.red5.server.api.stream.ISubscriberStream;
import org.red5.server.stream.ClientBroadcastStream;
import org.red5.server.stream.StreamService;
import org.red5.server.util.ScopeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.antmedia.cluster.IClusterNotifier;
import io.antmedia.datastore.db.DataStore;
import io.antmedia.datastore.db.DataStoreFactory;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.datastore.db.types.Endpoint;
import io.antmedia.datastore.db.types.SocialEndpointCredentials;
import io.antmedia.datastore.db.types.VoD;
import io.antmedia.datastore.preference.PreferenceStore;
import io.antmedia.filter.StreamAcceptFilter;
import io.antmedia.ipcamera.OnvifCamera;
import io.antmedia.muxer.IAntMediaStreamHandler;
import io.antmedia.muxer.IStreamAcceptFilter;
import io.antmedia.muxer.MuxAdaptor;
import io.antmedia.rest.RestServiceBase;
import io.antmedia.rest.model.Result;
import io.antmedia.security.AcceptOnlyStreamsInDataStore;
import io.antmedia.settings.ServerSettings;
import io.antmedia.shutdown.AMSShutdownManager;
import io.antmedia.shutdown.IShutdownListener;
import io.antmedia.social.endpoint.PeriscopeEndpoint;
import io.antmedia.social.endpoint.VideoServiceEndpoint;
import io.antmedia.social.endpoint.VideoServiceEndpoint.DeviceAuthParameters;
import io.antmedia.statistic.type.RTMPToWebRTCStats;
import io.antmedia.statistic.type.WebRTCAudioReceiveStats;
import io.antmedia.statistic.type.WebRTCAudioSendStats;
import io.antmedia.statistic.type.WebRTCVideoReceiveStats;
import io.antmedia.statistic.type.WebRTCVideoSendStats;
import io.antmedia.streamsource.StreamFetcher;
import io.antmedia.streamsource.StreamFetcherManager;
import io.antmedia.webrtc.api.IWebRTCAdaptor;
import io.vertx.core.Vertx;

public class AntMediaApplicationAdapter implements IAntMediaStreamHandler, IShutdownListener {

	public static final String BEAN_NAME = "web.handler";
	public static final String BROADCAST_STATUS_CREATED = "created";
	public static final String BROADCAST_STATUS_BROADCASTING = "broadcasting";
	public static final String BROADCAST_STATUS_FINISHED = "finished";
	public static final String BROADCAST_STATUS_PREPARING = "preparing";
	public static final int BROADCAST_STATS_RESET = 0;
	public static final String HOOK_ACTION_END_LIVE_STREAM = "liveStreamEnded";
	public static final String HOOK_ACTION_START_LIVE_STREAM = "liveStreamStarted";
	public static final String HOOK_ACTION_VOD_READY = "vodReady";
	
	public static final String DEFAULT_LOCALHOST = "127.0.0.1";

	protected static Logger logger = LoggerFactory.getLogger(AntMediaApplicationAdapter.class);
	private ServerSettings serverSettings;
	public static final String VOD = "VoD";
	public static final String LIVE_STREAM = "liveStream";
	public static final String IP_CAMERA = "ipCamera";
	public static final String STREAM_SOURCE = "streamSource";
	protected static final int END_POINT_LIMIT = 20;
	public static final String FACEBOOK = "facebook";
	public static final String PERISCOPE = "periscope";
	public static final String YOUTUBE = "youtube";
	public static final String FACEBOOK_ENDPOINT_CLASS = "io.antmedia.enterprise.social.endpoint.FacebookEndpoint";
	public static final String YOUTUBE_ENDPOINT_CLASS = "io.antmedia.enterprise.social.endpoint.YoutubeEndpoint";

	private Map<String, VideoServiceEndpoint> videoServiceEndpoints = new HashMap<>();
	private List<VideoServiceEndpoint> videoServiceEndpointsHavingError = new ArrayList<>();
	private List<IStreamPublishSecurity> streamPublishSecurityList;
	private HashMap<String, OnvifCamera> onvifCameraList = new HashMap<>();
	protected StreamFetcherManager streamFetcherManager;
	protected List<MuxAdaptor> muxAdaptors;
	private DataStore dataStore;
	private DataStoreFactory dataStoreFactory;

	private StreamAcceptFilter streamAcceptFilter;
	private AppSettings appSettings;
	private Vertx vertx;
	private IScope scope;

	protected List<String> encoderBlockedStreams = new ArrayList<>();
	private int numberOfEncoderNotOpenedErrors = 0;
	protected int publishTimeoutStreams = 0;
	private List<String> publishTimeoutStreamsList = new ArrayList<>();
	
	protected WebRTCVideoReceiveStats webRTCVideoReceiveStats = new WebRTCVideoReceiveStats();

	protected WebRTCAudioReceiveStats webRTCAudioReceiveStats = new WebRTCAudioReceiveStats();
	
	
	protected WebRTCVideoSendStats webRTCVideoSendStats = new WebRTCVideoSendStats();

	protected WebRTCAudioSendStats webRTCAudioSendStats = new WebRTCAudioSendStats();
	
	private IClusterNotifier clusterNotifier;

	public boolean appStart(IScope app) {
		setScope(app);
		vertx = (Vertx) app.getContext().getBean(VERTX_BEAN_NAME);

		//initalize to access the data store directly in the code
		getDataStore();

		if (app.getContext().hasBean(IClusterNotifier.BEAN_NAME)) {
			//which means it's in cluster mode
			clusterNotifier = (IClusterNotifier) app.getContext().getBean(IClusterNotifier.BEAN_NAME);
			
			clusterNotifier.registerSettingUpdateListener(getAppSettings().getAppName(), settings -> updateSettings(settings, false));
		}

		vertx.setTimer(1, l -> {
				streamFetcherManager = new StreamFetcherManager(vertx, getDataStore(),app);
				streamFetcherManager.setRestartStreamFetcherPeriod(appSettings.getRestartStreamFetcherPeriod());
				List<Broadcast> streams = getDataStore().getExternalStreamsList();
				logger.info("Stream source size: {}", streams.size());
				streamFetcherManager.startStreams(streams);

				List<SocialEndpointCredentials> socialEndpoints = getDataStore().getSocialEndpoints(0, END_POINT_LIMIT);

				logger.info("socialEndpoints size: {}", socialEndpoints.size());

				for (SocialEndpointCredentials socialEndpointCredentials : socialEndpoints) 
				{
					VideoServiceEndpoint endPointService = null;
					if (socialEndpointCredentials.getServiceName().equals(FACEBOOK)) 
					{
						endPointService = getEndpointService(FACEBOOK_ENDPOINT_CLASS, socialEndpointCredentials, appSettings.getFacebookClientId(), appSettings.getFacebookClientSecret());
					}
					else if (socialEndpointCredentials.getServiceName().equals(PERISCOPE)) 
					{
						endPointService = getEndpointService(PeriscopeEndpoint.class.getName(), socialEndpointCredentials, appSettings.getPeriscopeClientId(), appSettings.getPeriscopeClientSecret());
					}
					else if (socialEndpointCredentials.getServiceName().equals(YOUTUBE)) 
					{
						endPointService = getEndpointService(YOUTUBE_ENDPOINT_CLASS, socialEndpointCredentials, appSettings.getYoutubeClientId(), appSettings.getYoutubeClientSecret());
					}

					if (endPointService != null) {
						endPointService.setCollectInteractivity(appSettings.isCollectSocialMediaActivity());
						videoServiceEndpoints.put(endPointService.getCredentials().getId(), endPointService);
					}
				}

				synchUserVoDFolder(null, appSettings.getVodFolder());
		});

	
		AMSShutdownManager.getInstance().subscribe(this);

		//With the common app structure, we won't need to null check for WebRTCAdaptor
		if (app.getContext().hasBean(IWebRTCAdaptor.BEAN_NAME)) 
		{
			IWebRTCAdaptor webRTCAdaptor = (IWebRTCAdaptor) app.getContext().getBean(IWebRTCAdaptor.BEAN_NAME);

			webRTCAdaptor.setExcessiveBandwidthValue(appSettings.getExcessiveBandwidthValue());
			webRTCAdaptor.setExcessiveBandwidthCallThreshold(appSettings.getExcessiveBandwidthCallThreshold());
			webRTCAdaptor.setTryCountBeforeSwitchback(appSettings.getExcessiveBandwithTryCountBeforeSwitchback());
			webRTCAdaptor.setExcessiveBandwidthAlgorithmEnabled(appSettings.isExcessiveBandwidthAlgorithmEnabled());
			webRTCAdaptor.setPacketLossDiffThresholdForSwitchback(appSettings.getPacketLossDiffThresholdForSwitchback());
			webRTCAdaptor.setRttMeasurementDiffThresholdForSwitchback(appSettings.getRttMeasurementDiffThresholdForSwitchback());
		}

		return true;
	}

	public void appStop(IScope app) 
	{
		//not used
	}

	public boolean synchUserVoDFolder(String oldFolderPath, String vodFolderPath) 
	{
		boolean result = false;
		File streamsFolder = new File("webapps/" + getScope().getName() + "/streams");

		try {
			deleteOldFolderPath(oldFolderPath, streamsFolder);
			//even if an exception occurs, catch it in here and do not prevent the below operations
		} catch (IOException e) {
			logger.error(e.getMessage());
		}

		File f = new File(vodFolderPath == null ? "" : vodFolderPath);
		try {
			if (!streamsFolder.exists()) {
				streamsFolder.mkdir();
			}
			if (f.exists() && f.isDirectory()) {
				String newLinkPath = streamsFolder.getAbsolutePath() + "/" + f.getName();
				File newLinkFile = new File(newLinkPath);
				if (!newLinkFile.exists()) {
					Path target = f.toPath();
					Files.createSymbolicLink(newLinkFile.toPath(), target);
				}
			}
			//if file does not exists, it means reset the vod
			getDataStore().fetchUserVodList(f);
			result = true;
		} catch (IOException e) {
			logger.error(e.getMessage());
		}



		return result;
	}

	public boolean deleteOldFolderPath(String oldFolderPath, File streamsFolder) throws IOException {
		boolean result = false;
		if (oldFolderPath != null && !oldFolderPath.isEmpty() && streamsFolder != null) 
		{
			File f = new File(oldFolderPath);
			File linkFile = new File(streamsFolder.getAbsolutePath(), f.getName());
			if (linkFile.exists() && linkFile.isDirectory()) {
				Files.delete(linkFile.toPath());
				result = true;
			}
		}
		return result;
	}

	public void streamBroadcastClose(IBroadcastStream stream) {
		String streamName = stream.getPublishedName();
		vertx.executeBlocking(future -> closeBroadcast(streamName), null);
	}

	public void closeBroadcast(String streamName) {

		try {

				getDataStore().updateStatus(streamName, BROADCAST_STATUS_FINISHED);
				Broadcast broadcast = getDataStore().get(streamName);
								
				if (broadcast != null) {
					final String listenerHookURL = broadcast.getListenerHookURL();
					final String streamId = broadcast.getStreamId();
					if (listenerHookURL != null && listenerHookURL.length() > 0) {
						final String name = broadcast.getName();
						final String category = broadcast.getCategory();
						
						vertx.setTimer(1, e -> notifyHook(listenerHookURL, streamId, HOOK_ACTION_END_LIVE_STREAM, name, category,
								null, null));
					}

					stopPublishingSocialEndpoints(broadcast);

					if (broadcast.isZombi()) {
						getDataStore().delete(streamName);
					}

				}
		} catch (Exception e) {
			logger.error(ExceptionUtils.getStackTrace(e));
		}
	}
	
	public void stopPublishingSocialEndpoints(Broadcast broadcast) 
	{
		List<Endpoint> endPointList = broadcast.getEndPointList();
		if (endPointList != null) {
			for (Endpoint endpoint : endPointList) {
				VideoServiceEndpoint videoServiceEndPoint = getVideoServiceEndPoint(endpoint.getEndpointServiceId());
				if (videoServiceEndPoint != null) {
					try {
						videoServiceEndPoint.stopBroadcast(endpoint);
					} catch (Exception e) {
						logger.error(ExceptionUtils.getStackTrace(e));
					}
				}
			}
			// recreate endpoints for social media
			recreateEndpointsForSocialMedia(broadcast, endPointList);
		}
	}

	public void recreateEndpointsForSocialMedia(Broadcast broadcast, List<Endpoint> endPointList) {
		//below removeList and addList is due to avoid concurrent exception
		List<Endpoint> removeList = new ArrayList<>();
		List<Endpoint> addList = new ArrayList<>();
		for (Endpoint endpoint : endPointList) {

			if (!"".equals(endpoint.type)) 
			{
				VideoServiceEndpoint videoServiceEndPoint = getVideoServiceEndPoint(endpoint.getEndpointServiceId());
				if (videoServiceEndPoint != null) 
				{
					Endpoint newEndpoint;
					try {
						newEndpoint = videoServiceEndPoint.createBroadcast(broadcast.getName(),
								broadcast.getDescription(), broadcast.getStreamId(), broadcast.isIs360(), broadcast.isPublicStream(), 720, true);
						removeList.add(endpoint);
						addList.add(newEndpoint);
						
					} catch (Exception e) {
						logger.error(ExceptionUtils.getStackTrace(e));
					}

				}
			}
		}
		for (Endpoint endpoint : removeList) {
			getDataStore().removeEndpoint(broadcast.getStreamId(), endpoint);
		}
		
		for (Endpoint endpoint : addList) {
			getDataStore().addEndpoint(broadcast.getStreamId(), endpoint);
		}
		
	}

	public VideoServiceEndpoint getEndpointService(String className, 
			SocialEndpointCredentials socialEndpointCredentials, String clientId, String clientSecret)
	{
		try {
			VideoServiceEndpoint endPointService;
			Class endpointClass = Class.forName(className);

			endPointService = (VideoServiceEndpoint) endpointClass.getConstructor(String.class, String.class, DataStore.class, SocialEndpointCredentials.class, Vertx.class)
					.newInstance(clientId, clientSecret, getDataStore(), socialEndpointCredentials, vertx);
			endPointService.setCollectInteractivity(appSettings.isCollectSocialMediaActivity());
			return endPointService;
		}
		catch (Exception e) {
			logger.error(ExceptionUtils.getStackTrace(e));
		}
		return null;
	}


	public void streamPlayItemPlay(IPlayItem item, boolean isLive) {
		vertx.setTimer(1, l -> getDataStore().updateRtmpViewerCount(item.getName(), true));
	}

	public void streamPlayItemStop(IPlayItem item) {
		vertx.setTimer(1, l -> getDataStore().updateRtmpViewerCount(item.getName(), false));
	}

	public void streamSubscriberClose(ISubscriberStream stream) {
		vertx.setTimer(1, l -> getDataStore().updateRtmpViewerCount(stream.getBroadcastStreamPublishName(), false));
	}

	public void streamPublishStart(final IBroadcastStream stream) {
		String streamName = stream.getPublishedName();
		logger.info("stream name in streamPublishStart: {}", streamName );

		startPublish(streamName);
	}

	public void startPublish(String streamName) {
		vertx.executeBlocking( handler -> {
			try {

				DataStore dataStoreLocal = getDataStore();
				
					Broadcast broadcast = dataStoreLocal.get(streamName);

					if (broadcast == null) {

						broadcast = saveUndefinedBroadcast(streamName, getScope().getName(), dataStoreLocal, appSettings,  AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING, getServerSettings().getServerName(), getServerSettings().getHostAddress());

					} else {

						boolean result = dataStoreLocal.updateStatus(streamName, BROADCAST_STATUS_BROADCASTING);
						logger.info(" Status of stream {} is set to Broadcasting with result: {}", broadcast.getStreamId(), result);
					}

					final String listenerHookURL = broadcast.getListenerHookURL();
					final String streamId = broadcast.getStreamId();
					if (listenerHookURL != null && listenerHookURL.length() > 0) {
						final String name = broadcast.getName();
						final String category = broadcast.getCategory();
						vertx.setTimer(1, e -> notifyHook(listenerHookURL, streamId, HOOK_ACTION_START_LIVE_STREAM, name, category,
								null, null));
					}

					publishSocialEndpoints(broadcast.getEndPointList());
				
				handler.complete();
			} catch (Exception e) {
				logger.error(ExceptionUtils.getStackTrace(e));
				handler.fail(ExceptionUtils.getStackTrace(e));
			}
			
		}, null);
		
		logger.info("start publish leaved");
	}

	private ServerSettings getServerSettings() 
	{
		if (serverSettings == null) {
			serverSettings = (ServerSettings)scope.getContext().getApplicationContext().getBean(ServerSettings.BEAN_NAME);
		}
		return serverSettings;
	}


	public void publishSocialEndpoints(List<Endpoint> endPointList) 
	{
		if (endPointList != null) 
		{
			for (Endpoint endpoint : endPointList) {
				VideoServiceEndpoint videoServiceEndPoint = getVideoServiceEndPoint(endpoint.getEndpointServiceId());
				if (videoServiceEndPoint != null) {
					try {
						videoServiceEndPoint.publishBroadcast(endpoint);
						logger.info("publish broadcast called for {}" , videoServiceEndPoint.getName());
					} catch (Exception e) {
						logger.error(ExceptionUtils.getStackTrace(e));
					}
				}
			}
		}
	}
	
	
	
	
	

	public static Broadcast saveUndefinedBroadcast(String streamId, String scopeName, DataStore dataStore, AppSettings appSettings, String streamStatus, String fqdn, String hostAddress) {		
		Broadcast newBroadcast = new Broadcast();
		long now = System.currentTimeMillis();
		newBroadcast.setDate(now);
		newBroadcast.setStartTime(now);
		newBroadcast.setZombi(true);
		try {
			newBroadcast.setStreamId(streamId);

			String settingsListenerHookURL = null; 
			if (appSettings != null) {
				settingsListenerHookURL = appSettings.getListenerHookURL();
			}

			return RestServiceBase.saveBroadcast(newBroadcast,
					streamStatus, scopeName, dataStore,
					settingsListenerHookURL, fqdn, hostAddress);
		} catch (Exception e) {
			logger.error(ExceptionUtils.getStackTrace(e));
		}

		return null;
	}

	public VideoServiceEndpoint getVideoServiceEndPoint(String id) {
		if (videoServiceEndpoints != null) {
			return videoServiceEndpoints.get(id);
		}
		return null;
	}

	@Override
	public void muxingFinished(final String streamId, File file, long duration, int resolution) {
		String vodName = file.getName();
		String filePath = file.getPath();
		long fileSize = file.length();
		long systemTime = System.currentTimeMillis();
		String[] subDirs = filePath.split(Pattern.quote(File.separator));
		Integer pathLength=Integer.valueOf(subDirs.length);
		String relativePath= subDirs[pathLength-2]+'/'+subDirs[pathLength-1];
		String listenerHookURL = null;
		String streamName = file.getName();

		Broadcast broadcast = getDataStore().get(streamId);
		if (broadcast != null && broadcast.getName() != null) {
			streamName = broadcast.getName();
			listenerHookURL = broadcast.getListenerHookURL();
			if (resolution != 0) {
				streamName = streamName + " (" + resolution + "p)";

			}
		}
		if (listenerHookURL == null || listenerHookURL.isEmpty()) {
			// if hook URL is not defined for stream specific, then try to get common one from app
			listenerHookURL = appSettings.getListenerHookURL();
		}

		String vodId = RandomStringUtils.randomNumeric(24);
		VoD newVod = new VoD(streamName, streamId, relativePath, vodName, systemTime, duration, fileSize, VoD.STREAM_VOD, vodId);

		if (getDataStore().addVod(newVod) == null) {
			logger.warn("Stream vod with stream id {} cannot be added to data store", streamId);
		}

		int index;
		
		//HOOK_ACTION_VOD_READY is called only the listenerHookURL is defined either for stream or in AppSettings
		if (listenerHookURL != null && !listenerHookURL.isEmpty() && 
				(index = vodName.lastIndexOf(".mp4")) != -1) 
		{
			final String baseName = vodName.substring(0, index);
			String finalListenerHookURL = listenerHookURL;
			
			vertx.setTimer(1, e ->	notifyHook(finalListenerHookURL, streamId, HOOK_ACTION_VOD_READY, null, null, baseName, vodId));
		}

		String muxerFinishScript = appSettings.getMuxerFinishScript();
		if (muxerFinishScript != null && !muxerFinishScript.isEmpty()) {	
			runScript(muxerFinishScript + "  " + file.getAbsolutePath());
		}


	}

	public void runScript(String scriptFile) {
		vertx.executeBlocking(future -> {
			try {
				logger.info("running muxer finish script: {}", scriptFile);
				Process exec = Runtime.getRuntime().exec(scriptFile);
				int result = exec.waitFor();
				future.complete();
				logger.info("completing script: {} with return value {}", scriptFile, result);
			} catch (IOException e) {
				logger.error(ExceptionUtils.getStackTrace(e));
			} catch (InterruptedException e) {
				logger.error(ExceptionUtils.getStackTrace(e));
				Thread.currentThread().interrupt();
			} 

		}, res -> {

		});
	}

	private static class AuthCheckJob {

		private int count;
		private VideoServiceEndpoint videoServiceEndpoint;
		private int interval;
		private AntMediaApplicationAdapter appAdapter;

		public AuthCheckJob(int count, int interval, VideoServiceEndpoint videoServiceEndpoint, AntMediaApplicationAdapter adapter) {
			this.count = count;
			this.videoServiceEndpoint = videoServiceEndpoint;
			this.interval = interval;
			this.appAdapter = adapter;
		}

		public void execute() {
			try {
				if (!videoServiceEndpoint.askIfDeviceAuthenticated()) {
					count++;
					if (count < 10) {
						if (videoServiceEndpoint.getError() == null) {
							appAdapter.getVertx().setTimer(interval, l->{
								new AuthCheckJob(count, interval, videoServiceEndpoint, appAdapter).execute();
							});
							logger.info("Asking authetnication for {}", videoServiceEndpoint.getName());
						}
						else {
							//there is an error so do not ask again
							this.appAdapter.getVideoServiceEndpointsHavingError().add(videoServiceEndpoint);
						}
					}
					else {
						videoServiceEndpoint.setError(VideoServiceEndpoint.AUTHENTICATION_TIMEOUT);
						this.appAdapter.getVideoServiceEndpointsHavingError().add(videoServiceEndpoint);
						logger.info("Not authenticated for {} and will not try again", videoServiceEndpoint.getName());
					}
				}
				else {
					logger.info("Authenticated, adding video service endpoint type: {} with id: {} to the app", videoServiceEndpoint.getName(), videoServiceEndpoint.getCredentials().getId());
					this.appAdapter.getVideoServiceEndpoints().put(videoServiceEndpoint.getCredentials().getId(), videoServiceEndpoint);

				}
			} catch (Exception e) {
				logger.error(ExceptionUtils.getStackTrace(e));
			}
		}
	}

	public void startDeviceAuthStatusPolling(VideoServiceEndpoint videoServiceEndpoint,
			DeviceAuthParameters askDeviceAuthParameters) {
		int timeDelta = askDeviceAuthParameters.interval * 1000;
		getVertx().setTimer(timeDelta, l->{
			new AuthCheckJob(0, timeDelta, videoServiceEndpoint, this).execute();
		});
	}

	public Map<String, VideoServiceEndpoint> getVideoServiceEndpoints() {
		return videoServiceEndpoints;
	}

	public List<VideoServiceEndpoint> getVideoServiceEndpointsHavingError(){
		return videoServiceEndpointsHavingError ;
	}

	public void setVideoServiceEndpoints(Map<String, VideoServiceEndpoint> videoServiceEndpoints) {
		this.videoServiceEndpoints = videoServiceEndpoints;
	}

	/**
	 * Notify hook with parameters below
	 * 
	 * @param url
	 *            is the url of the service to be called
	 * 
	 * @param id
	 *            is the stream id that is unique for each stream
	 * 
	 * @param action
	 *            is the name of the action to be notified, it has values such
	 *            as {@link #HOOK_ACTION_END_LIVE_STREAM}
	 *            {@link #HOOK_ACTION_START_LIVE_STREAM}
	 * 
	 * @param streamName,
	 *            name of the stream. It is not the name of the file. It is just
	 *            a user friendly name
	 * 
	 * @param category,
	 *            category of the stream
	 * 
	 * @param vodName name of the vod 
	 * 
	 * @param vodId id of the vod in the datastore
	 * 
	 * @return
	 */
	public StringBuilder notifyHook(String url, String id, String action, String streamName, String category,
			String vodName, String vodId) {
		StringBuilder response = null;

		if (url != null && url.length() > 0) {
			Map<String, String> variables = new HashMap<>();

			variables.put("id", id);
			variables.put("action", action);
			if (streamName != null) {
				variables.put("streamName", streamName);
			}
			if (category != null) {
				variables.put("category", category);
			}

			if (vodName != null) {
				variables.put("vodName", vodName);
			}

			if (vodId != null) {
				variables.put("vodId", vodId);
			}

			try {
				response = sendPOST(url, variables);
			} catch (Exception e) {
				//Make Exception generi
				logger.error(ExceptionUtils.getStackTrace(e));
			}
		}
		return response;
	}

	public StringBuilder sendPOST(String url, Map<String, String> variables) throws IOException {

		StringBuilder response = null;
		try (CloseableHttpClient httpClient = getHttpClient()) 
		{
			HttpPost httpPost = new HttpPost(url);
			RequestConfig requestConfig =RequestConfig.custom()
					.setConnectTimeout(2000)
					.setConnectionRequestTimeout(2000)
					.setSocketTimeout(2000).build();
			httpPost.setConfig(requestConfig);
			List<NameValuePair> urlParameters = new ArrayList<>();
			Set<Entry<String, String>> entrySet = variables.entrySet();
			for (Entry<String, String> entry : entrySet) {
				urlParameters.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
			}

			HttpEntity postParams = new UrlEncodedFormEntity(urlParameters);
			httpPost.setEntity(postParams);

			CloseableHttpResponse httpResponse = httpClient.execute(httpPost);
			logger.info("POST Response Status:: {}" , httpResponse.getStatusLine().getStatusCode());

			HttpEntity entity = httpResponse.getEntity();
			if (entity != null) 
			{ 
				//read entity if it's available
				BufferedReader reader = new BufferedReader(new InputStreamReader(entity.getContent()));

				String inputLine;
				response = new StringBuilder();

				while ((inputLine = reader.readLine()) != null) {
					response.append(inputLine);
				}
				reader.close();
			}

		}
		return response;
	}
	
	public CloseableHttpClient getHttpClient() {
		return HttpClients.createDefault();
	}

	public List<IStreamPublishSecurity> getStreamPublishSecurityList() {
		return streamPublishSecurityList;
	}

	public void setStreamPublishSecurityList(List<IStreamPublishSecurity> streamPublishSecurityList) {
		this.streamPublishSecurityList = streamPublishSecurityList;
	}

	
	public AppSettings getAppSettings() {
		return appSettings;
	}
	

	public void setAppSettings(AppSettings appSettings) {
		this.appSettings = appSettings;
	}
	
	public StreamAcceptFilter getStreamAcceptFilter() {
		return streamAcceptFilter;
	}
	

	public void setStreamAcceptFilter(StreamAcceptFilter streamAcceptFilter) {
		this.streamAcceptFilter = streamAcceptFilter;
	}
	
	public boolean isValidStreamParameters(AVFormatContext inputFormatContext,AVPacket pkt) {
		return streamAcceptFilter.isValidStreamParameters(inputFormatContext, pkt);
	}


	public StreamFetcher startStreaming(Broadcast broadcast) {
		if(broadcast.getType().equals(AntMediaApplicationAdapter.IP_CAMERA) ||
				broadcast.getType().equals(AntMediaApplicationAdapter.STREAM_SOURCE))  {
			return streamFetcherManager.startStreaming(broadcast);
		}
		return null;
	}

	public Result stopStreaming(Broadcast broadcast) {
		Result result = new Result(false);
		if(broadcast.getType().equals(AntMediaApplicationAdapter.IP_CAMERA) ||
				broadcast.getType().equals(AntMediaApplicationAdapter.STREAM_SOURCE) ||
						broadcast.getType().equals(AntMediaApplicationAdapter.VOD)) 
		{
			result = streamFetcherManager.stopStreaming(broadcast);
		} 
		else if (broadcast.getType().equals(AntMediaApplicationAdapter.LIVE_STREAM)) 
		{
			IBroadcastStream broadcastStream = getBroadcastStream(getScope(), broadcast.getStreamId());
			if (broadcastStream != null) 
			{
				
				IStreamCapableConnection connection = ((IClientBroadcastStream) broadcastStream).getConnection();
				if (connection != null) {
					connection.close();
				}
				else {
					logger.warn("Connection is null. It should not happen for stream: {}. Analyze the logs", broadcast.getStreamId());
				}
				result.setSuccess(true);
			}
		}
		return result;
	}

	public IBroadcastStream getBroadcastStream(IScope scope, String name) {
		IStreamService service = (IStreamService) ScopeUtils.getScopeService(scope, IStreamService.class,
				StreamService.class);
		if (service instanceof StreamService) {
			IBroadcastScope bs = ((StreamService) service).getBroadcastScope(scope, name);
			if (bs != null) {
				return bs.getClientBroadcastStream();
			}
		}
		return null;
	}
	
	public OnvifCamera getOnvifCamera(String id) {
		OnvifCamera onvifCamera = onvifCameraList.get(id);
		if (onvifCamera == null) {

			Broadcast camera = getDataStore().get(id);
			if (camera != null) {
				onvifCamera = new OnvifCamera();
				onvifCamera.connect(camera.getIpAddr(), camera.getUsername(), camera.getPassword());

				onvifCameraList.put(id, onvifCamera);
			}
		}
		return onvifCamera;
	}

	public StreamFetcherManager getStreamFetcherManager() {
		return streamFetcherManager;
	}
	
	public void setStreamFetcherManager(StreamFetcherManager streamFetcherManager) {
		this.streamFetcherManager = streamFetcherManager;
	}

	@Override
	public void setQualityParameters(String id, String quality, double speed, int pendingPacketSize) {
		
		vertx.setTimer(5, h -> {
			logger.info("update source quality for stream: {} quality:{} speed:{}", id, quality, speed);
			getDataStore().updateSourceQualityParameters(id, quality, speed, pendingPacketSize);
		});
	}

	public DataStore getDataStore() {
		if(dataStore == null)
		{
			dataStore = dataStoreFactory.getDataStore();
		}
		return dataStore;
	}

	public DataStoreFactory getDataStoreFactory() {
		return dataStoreFactory;
	}


	public void setDataStoreFactory(DataStoreFactory dataStoreFactory) {
		this.dataStoreFactory = dataStoreFactory;
	}

	/**
	 * This setter for test cases
	 * @param vertx
	 */
	public void setVertx(Vertx vertx) {
		this.vertx = vertx;
	}
	
	public void closeRTMPStreams() 
	{
		List<MuxAdaptor> adaptors = getMuxAdaptors();
		synchronized (adaptors) 
		{
			for (MuxAdaptor adaptor : adaptors) {
				if(adaptor.getBroadcast().getType().equals(AntMediaApplicationAdapter.LIVE_STREAM)) {
					
					ClientBroadcastStream broadcastStream = adaptor.getBroadcastStream();
					if (broadcastStream != null) {
						broadcastStream.stop();
					}
					adaptor.stop();
				}
			}
		}
	}
	
	public void closeStreamFetchers() {
		if (streamFetcherManager != null) {
			Queue<StreamFetcher> fetchers = streamFetcherManager.getStreamFetcherList();
			for (StreamFetcher streamFetcher : fetchers) {
				streamFetcher.stopStream();
			}
		}
	}
	
	public void waitUntilLiveStreamsStopped() {
		int i = 0;
		int waitPeriod = 1000;
		while(getDataStore().getLocalLiveBroadcastCount(getServerSettings().getHostAddress()) > 0) {
			try {
				if (i > 3) {
					logger.warn("Waiting for active broadcasts number decrease to zero for app: {}"
							+ "total wait time: {}ms", getScope().getName(), i*waitPeriod);
				}
				if (i>10) {
					logger.error("*********************************************************************************");
					logger.error("Not all live streams're stopped. It's even breaking the loop to finish the server");
					logger.error("*********************************************************************************");
					break;
				}
				i++;
				Thread.sleep(waitPeriod);
				
			} catch (InterruptedException e) {
				logger.error(ExceptionUtils.getStackTrace(e));
				Thread.currentThread().interrupt();
			}
		}
	}


	@Override
	public void serverShuttingdown() {
		logger.info("{} is closing streams", getScope().getName());
		closeStreamFetchers();
		closeRTMPStreams();
		waitUntilLiveStreamsStopped();
		
		getDataStore().close();
	}

	@Override
	public void muxAdaptorAdded(MuxAdaptor muxAdaptor){
		getMuxAdaptors().add(muxAdaptor);
	}

	@Override
	public void muxAdaptorRemoved(MuxAdaptor muxAdaptor) {
		getMuxAdaptors().remove(muxAdaptor);
	}

	public List<MuxAdaptor> getMuxAdaptors() {
		if(muxAdaptors == null){
			muxAdaptors = Collections.synchronizedList(new ArrayList());
		}
		return muxAdaptors;
	}

	public IScope getScope() {
		return scope;
	}


	public void setScope(IScope scope) {
		this.scope = scope;
	}
	


	/**
	 * Number of encoders blocked. 
	 * @return
	 */
	public int getNumberOfEncodersBlocked() {
		return encoderBlockedStreams.size();
	}
	
	public synchronized void encoderBlocked(String streamId, boolean blocked) {
		if (blocked) {
			encoderBlockedStreams.add(streamId);
		}
		else {
			encoderBlockedStreams.remove(streamId);
		}
	}

	
	public synchronized void incrementEncoderNotOpenedError() {
		numberOfEncoderNotOpenedErrors ++;
	}
	
	public int getNumberOfEncoderNotOpenedErrors() {
		return numberOfEncoderNotOpenedErrors;
	}
	
	public int getNumberOfPublishTimeoutError() {
		return publishTimeoutStreams;
	}
	
	public synchronized void publishTimeoutError(String streamId) {
		publishTimeoutStreams++;
		publishTimeoutStreamsList.add(streamId);
	}

	public WebRTCAudioReceiveStats getWebRTCAudioReceiveStats() {
		return webRTCAudioReceiveStats;
	}
	
	public WebRTCVideoReceiveStats getWebRTCVideoReceiveStats() {
		return webRTCVideoReceiveStats;
	}
	
	public WebRTCAudioSendStats getWebRTCAudioSendStats() {
		return webRTCAudioSendStats;
	}
	
	public WebRTCVideoSendStats getWebRTCVideoSendStats() {
		return webRTCVideoSendStats;
	} 
	
	public Vertx getVertx() {
		return vertx;
	}

	public boolean updateSettings(AppSettings newSettings, boolean notifyCluster) {

		boolean result = false;
		
		//if there is any wrong encoder settings, remove it at first
		List<EncoderSettings> encoderSettingsList = newSettings.getEncoderSettings();
		if (encoderSettingsList != null) {
			for (Iterator<EncoderSettings> iterator = encoderSettingsList.iterator(); iterator.hasNext();) {
				EncoderSettings encoderSettings = iterator.next();
				if (encoderSettings.getHeight() == 0 || encoderSettings.getVideoBitrate() == 0 || encoderSettings.getAudioBitrate() == 0)
				{
					iterator.remove();
				}
			}
		}
		//synch again because of string to list mapping- TODO: There is a better way for string to list mapping
		//in properties files
		newSettings.setEncoderSettings(encoderSettingsList);
		
		if (newSettings.getHlsListSize() == null || Integer.valueOf(newSettings.getHlsListSize()) < 5) {
			newSettings.setHlsListSize("5");
		}
		
		if (newSettings.getHlsTime() == null || Integer.valueOf(newSettings.getHlsTime()) < 1) {
			newSettings.setHlsTime("1");
		}

		/**
		 * ATTENTION: When a new settings added both 
		 *   {@link #updateAppSettingsFile} && {@link #updateAppSettingsBean} should be updated
		 */
		if (updateAppSettingsFile(getScope().getName(), newSettings))
		{
			AcceptOnlyStreamsInDataStore securityHandler = (AcceptOnlyStreamsInDataStore)  getScope().getContext().getBean(AcceptOnlyStreamsInDataStore.BEAN_NAME);
			securityHandler.setEnabled(newSettings.isAcceptOnlyStreamsInDataStore());

			updateAppSettingsBean(appSettings, newSettings);
			
			if (notifyCluster && clusterNotifier != null) {
				clusterNotifier.getClusterStore().saveSettings(appSettings);
			}
			
			result = true;
		}
		else {
			logger.warn("Settings cannot be saved for {}", getScope().getName());
		}

		return result;
	}
	
	public void setClusterNotifier(IClusterNotifier clusterNotifier) {
		this.clusterNotifier = clusterNotifier;
	}
	
	
	private boolean updateAppSettingsFile(String appName, AppSettings newAppsettings) 
	{
		/*
		 * Remember remember the 23th of November
		 * 
		 * String.valueof(null) returns "null" string. 
		 * 
		 * If we know the case above, we will write better codes. 
		 * 
		 */
		PreferenceStore store = new PreferenceStore("webapps/"+appName+"/WEB-INF/red5-web.properties");

		store.put(AppSettings.SETTINGS_MP4_MUXING_ENABLED, String.valueOf(newAppsettings.isMp4MuxingEnabled()));
		store.put(AppSettings.SETTINGS_ADD_DATE_TIME_TO_MP4_FILE_NAME, String.valueOf(newAppsettings.isAddDateTimeToMp4FileName()));
		store.put(AppSettings.SETTINGS_HLS_MUXING_ENABLED, String.valueOf(newAppsettings.isHlsMuxingEnabled()));
		store.put(AppSettings.SETTINGS_ACCEPT_ONLY_STREAMS_IN_DATA_STORE, String.valueOf(newAppsettings.isAcceptOnlyStreamsInDataStore()));
		store.put(AppSettings.SETTINGS_OBJECT_DETECTION_ENABLED, String.valueOf(newAppsettings.isObjectDetectionEnabled()));
		store.put(AppSettings.SETTINGS_TOKEN_CONTROL_ENABLED, String.valueOf(newAppsettings.isTokenControlEnabled()));
		store.put(AppSettings.SETTINGS_WEBRTC_ENABLED, String.valueOf(newAppsettings.isWebRTCEnabled()));
		store.put(AppSettings.SETTINGS_WEBRTC_FRAME_RATE, String.valueOf(newAppsettings.getWebRTCFrameRate()));
		store.put(AppSettings.SETTINGS_HASH_CONTROL_PUBLISH_ENABLED, String.valueOf(newAppsettings.isHashControlPublishEnabled()));
		store.put(AppSettings.SETTINGS_HASH_CONTROL_PLAY_ENABLED, String.valueOf(newAppsettings.isHashControlPlayEnabled()));
		
		store.put(AppSettings.SETTINGS_REMOTE_ALLOWED_CIDR, newAppsettings.getRemoteAllowedCIDR() != null 
																? newAppsettings.getRemoteAllowedCIDR() 
																: DEFAULT_LOCALHOST);
		
		store.put(AppSettings.SETTINGS_VOD_FOLDER, newAppsettings.getVodFolder() != null ? newAppsettings.getVodFolder() : "");
		store.put(AppSettings.SETTINGS_HLS_LIST_SIZE, String.valueOf(newAppsettings.getHlsListSize()));
		store.put(AppSettings.SETTINGS_HLS_TIME, String.valueOf(newAppsettings.getHlsTime()));
		store.put(AppSettings.SETTINGS_HLS_PLAY_LIST_TYPE, newAppsettings.getHlsPlayListType() != null ?  newAppsettings.getHlsPlayListType() : "");
		store.put(AppSettings.SETTINGS_ENCODER_SETTINGS_STRING, AppSettings.encodersList2Str(newAppsettings.getEncoderSettings()));
		store.put(AppSettings.TOKEN_HASH_SECRET, newAppsettings.getTokenHashSecret() != null ? newAppsettings.getTokenHashSecret() : "");
		store.put(AppSettings.SETTINGS_PREVIEW_OVERWRITE, String.valueOf(newAppsettings.isPreviewOverwrite()));
		store.put(AppSettings.SETTINGS_ALLOWED_PUBLISHER_IPS, newAppsettings.getAllowedPublisherCIDR() != null ? 
																	String.valueOf(newAppsettings.getAllowedPublisherCIDR())
																	: "");
		store.put(AppSettings.SETTINGS_H264_ENABLED, String.valueOf(newAppsettings.isH264Enabled()));
		store.put(AppSettings.SETTINGS_VP8_ENABLED, String.valueOf(newAppsettings.isVp8Enabled()));
		store.put(AppSettings.SETTINGS_DATA_CHANNEL_ENABLED, String.valueOf(newAppsettings.isDataChannelEnabled()));
		store.put(AppSettings.SETTINGS_DATA_CHANNEL_PLAYER_DISTRIBUTION, String.valueOf(newAppsettings.getDataChannelPlayerDistribution()));

		store.put(AppSettings.SETTINGS_MAX_RESOLUTION_ACCEPT, String.valueOf(newAppsettings.getMaxResolutionAccept()));
		store.put(AppSettings.SETTINGS_MAX_BITRATE_ACCEPT, String.valueOf(newAppsettings.getMaxBitrateAccept()));
		store.put(AppSettings.SETTINGS_MAX_FPS_ACCEPT, String.valueOf(newAppsettings.getMaxFpsAccept()));
		
		return store.save();
	}


	private void updateAppSettingsBean(AppSettings appSettings, AppSettings newSettings) 
	{	
		appSettings.setMp4MuxingEnabled(newSettings.isMp4MuxingEnabled());
		appSettings.setAddDateTimeToMp4FileName(newSettings.isAddDateTimeToMp4FileName());
		appSettings.setHlsMuxingEnabled(newSettings.isHlsMuxingEnabled());
		appSettings.setObjectDetectionEnabled(newSettings.isObjectDetectionEnabled());
		appSettings.setHlsListSize(String.valueOf(newSettings.getHlsListSize()));
		appSettings.setHlsTime(String.valueOf(newSettings.getHlsTime()));
		appSettings.setHlsPlayListType(newSettings.getHlsPlayListType());
		appSettings.setAcceptOnlyStreamsInDataStore(newSettings.isAcceptOnlyStreamsInDataStore());
		appSettings.setTokenControlEnabled(newSettings.isTokenControlEnabled());
		appSettings.setWebRTCEnabled(newSettings.isWebRTCEnabled());
		appSettings.setWebRTCFrameRate(newSettings.getWebRTCFrameRate());
		appSettings.setHashControlPublishEnabled(newSettings.isHashControlPublishEnabled());
		appSettings.setHashControlPlayEnabled(newSettings.isHashControlPlayEnabled());
		appSettings.setTokenHashSecret(newSettings.getTokenHashSecret());

		appSettings.setRemoteAllowedCIDR(newSettings.getRemoteAllowedCIDR());
		
		appSettings.setEncoderSettings(newSettings.getEncoderSettings());
		
		String oldVodFolder = appSettings.getVodFolder();

		appSettings.setAllowedPublisherCIDR(newSettings.getAllowedPublisherCIDR());
		appSettings.setVodFolder(newSettings.getVodFolder());
		appSettings.setPreviewOverwrite(newSettings.isPreviewOverwrite());

		synchUserVoDFolder(oldVodFolder, newSettings.getVodFolder());
		
		appSettings.setH264Enabled(newSettings.isH264Enabled());
		appSettings.setVp8Enabled(newSettings.isVp8Enabled());

		appSettings.setDataChannelEnabled(newSettings.isDataChannelEnabled());
		appSettings.setDataChannelPlayerDistribution(newSettings.getDataChannelPlayerDistribution());
		
		appSettings.setMaxBitrateAccept(newSettings.getMaxBitrateAccept());
		appSettings.setMaxFpsAccept(newSettings.getMaxFpsAccept());
		appSettings.setMaxResolutionAccept(newSettings.getMaxResolutionAccept());
		
		logger.warn("app settings updated for {}", getScope().getName());	
	}
	
	public void setServerSettings(ServerSettings serverSettings) {
		this.serverSettings = serverSettings;
	}

	/*
	 * This method is overridden in enterprise edition since RTMP to WebRTC streaming is an enterprise feature.
	 */
	public List<RTMPToWebRTCStats> getRTMPToWebRTCStats() {
		return new ArrayList<RTMPToWebRTCStats>();
	}
	
	public boolean isDataChannelEnabled() {
		return false;
	}
	
	public boolean isDataChannelMessagingSupported() {
		
		return false;
	}
	
	public boolean sendDataChannelMessage(String streamId, String message) {
		
		return false;
	}
	
	public boolean doesWebRTCStreamExist(String streamId) {
		return false;
	}

}
