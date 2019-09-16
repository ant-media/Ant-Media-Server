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
import java.util.concurrent.TimeUnit;
import java.util.Queue;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.configuration2.FileBasedConfiguration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.ReloadingFileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.reloading.PeriodicReloadingTrigger;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.red5.server.adapter.MultiThreadedApplicationAdapter;
import org.red5.server.api.scheduling.IScheduledJob;
import org.red5.server.api.scheduling.ISchedulingService;
import org.red5.server.api.scope.IScope;
import org.red5.server.api.stream.IBroadcastStream;
import org.red5.server.api.stream.IClientBroadcastStream;
import org.red5.server.api.stream.IPlayItem;
import org.red5.server.api.stream.IStreamPublishSecurity;
import org.red5.server.api.stream.ISubscriberStream;
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
import io.antmedia.ipcamera.OnvifCamera;
import io.antmedia.muxer.IAntMediaStreamHandler;
import io.antmedia.muxer.MuxAdaptor;
import io.antmedia.rest.BroadcastRestService;
import io.antmedia.rest.model.Result;
import io.antmedia.security.AcceptOnlyStreamsInDataStore;
import io.antmedia.settings.ServerSettings;
import io.antmedia.shutdown.AMSShutdownManager;
import io.antmedia.shutdown.IShutdownListener;
import io.antmedia.social.endpoint.PeriscopeEndpoint;
import io.antmedia.social.endpoint.VideoServiceEndpoint;
import io.antmedia.social.endpoint.VideoServiceEndpoint.DeviceAuthParameters;
import io.antmedia.statistic.type.WebRTCAudioReceiveStats;
import io.antmedia.statistic.type.WebRTCAudioSendStats;
import io.antmedia.statistic.type.WebRTCVideoReceiveStats;
import io.antmedia.statistic.type.WebRTCVideoSendStats;
import io.antmedia.streamsource.StreamFetcher;
import io.antmedia.streamsource.StreamFetcherManager;
import io.antmedia.webrtc.api.IWebRTCAdaptor;
import io.vertx.core.Vertx;

public class AntMediaApplicationAdapter extends MultiThreadedApplicationAdapter implements IAntMediaStreamHandler, IShutdownListener {

	public static final String BEAN_NAME = "web.handler";
	public static final String BROADCAST_STATUS_CREATED = "created";
	public static final String BROADCAST_STATUS_BROADCASTING = "broadcasting";
	public static final String BROADCAST_STATUS_FINISHED = "finished";
	public static final String BROADCAST_STATUS_PREPARING = "preparing";
	public static final int BROADCAST_STATS_RESET = 0;
	public static final String HOOK_ACTION_END_LIVE_STREAM = "liveStreamEnded";
	public static final String HOOK_ACTION_START_LIVE_STREAM = "liveStreamStarted";
	public static final String HOOK_ACTION_VOD_READY = "vodReady";

	public static final String VERTX_BEAN_NAME = "vertxCore";
	
	public static final String DEFAULT_LOCALHOST = "127.0.0.1";

	protected static Logger logger = LoggerFactory.getLogger(AntMediaApplicationAdapter.class);
	private ServerSettings serverSettings;
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

	private AppSettings appSettings;
	private Vertx vertx;

	protected List<String> encoderBlockedStreams = new ArrayList<>();
	private int numberOfEncoderNotOpenedErrors = 0;
	protected int publishTimeoutStreams = 0;
	private List<String> publishTimeoutStreamsList = new ArrayList<>();

	protected WebRTCVideoReceiveStats webRTCVideoReceiveStats = new WebRTCVideoReceiveStats();

	protected WebRTCAudioReceiveStats webRTCAudioReceiveStats = new WebRTCAudioReceiveStats();


	protected WebRTCVideoSendStats webRTCVideoSendStats = new WebRTCVideoSendStats();

	protected WebRTCAudioSendStats webRTCAudioSendStats = new WebRTCAudioSendStats();
	private IClusterNotifier clusterNotifier;

	@Override
	public boolean appStart(IScope app) {
		vertx = (Vertx) getContext().getBean(VERTX_BEAN_NAME);
		
	
		//initalize to access the data store directly in the code
		getDataStore();

		if (getContext().hasBean(IClusterNotifier.BEAN_NAME)) {
			//which means it's in cluster mode
			clusterNotifier = (IClusterNotifier) getContext().getBean(IClusterNotifier.BEAN_NAME);
			
			clusterNotifier.registerSettingUpdateListener(getAppSettings().getAppName(), 
					settings -> updateSettings(settings, false));
		}

		for (IStreamPublishSecurity streamPublishSecurity : getStreamPublishSecurityList()) {
			registerStreamPublishSecurity(streamPublishSecurity);
		}
		
		
		
		vertx.runOnContext(l -> {
			streamFetcherManager = new StreamFetcherManager(AntMediaApplicationAdapter.this, getDataStore(),app);
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
		if (getContext().hasBean(IWebRTCAdaptor.BEAN_NAME)) 
		{
			IWebRTCAdaptor webRTCAdaptor = (IWebRTCAdaptor) getContext().getBean(IWebRTCAdaptor.BEAN_NAME);

			webRTCAdaptor.setExcessiveBandwidthValue(appSettings.getExcessiveBandwidthValue());
			webRTCAdaptor.setExcessiveBandwidthCallThreshold(appSettings.getExcessiveBandwidthCallThreshold());
			webRTCAdaptor.setTryCountBeforeSwitchback(appSettings.getExcessiveBandwithTryCountBeforeSwitchback());
			webRTCAdaptor.setExcessiveBandwidthAlgorithmEnabled(appSettings.isExcessiveBandwidthAlgorithmEnabled());
			webRTCAdaptor.setPacketLossDiffThresholdForSwitchback(appSettings.getPacketLossDiffThresholdForSwitchback());
			webRTCAdaptor.setRttMeasurementDiffThresholdForSwitchback(appSettings.getRttMeasurementDiffThresholdForSwitchback());
		}

		return super.appStart(app);
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

	@Override
	public void streamBroadcastClose(IBroadcastStream stream) {
		String streamName = stream.getPublishedName();
		vertx.executeBlocking(future -> closeBroadcast(streamName), null);
		super.streamBroadcastClose(stream);
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

					vertx.runOnContext(e -> notifyHook(listenerHookURL, streamId, HOOK_ACTION_END_LIVE_STREAM, name, category,
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


	@Override
	public void streamPlayItemPlay(ISubscriberStream stream, IPlayItem item, boolean isLive) {
		super.streamPlayItemPlay(stream, item, isLive);

		vertx.runOnContext(l -> getDataStore().updateRtmpViewerCount(item.getName(), true));
	}

	@Override
	public void streamPlayItemStop(ISubscriberStream stream, IPlayItem item) {
		super.streamPlayItemStop(stream, item);
		vertx.runOnContext(l -> getDataStore().updateRtmpViewerCount(item.getName(), false));
	}

	@Override
	public void streamSubscriberClose(ISubscriberStream stream) {
		super.streamSubscriberClose(stream);

		vertx.runOnContext(l -> getDataStore().updateRtmpViewerCount(stream.getBroadcastStreamPublishName(), false));
	}

	@Override
	public void streamPublishStart(final IBroadcastStream stream) {
		String streamName = stream.getPublishedName();
		logger.info("stream name in streamPublishStart: {}", streamName );

		startPublish(streamName);

		super.streamPublishStart(stream);
	}

	public void startPublish(String streamName) {
		vertx.executeBlocking( handler -> {
			try {

				DataStore dataStoreLocal = getDataStore();

				Broadcast broadcast = dataStoreLocal.get(streamName);

				if (broadcast == null) {

					broadcast = saveUndefinedBroadcast(streamName, getScope().getName(), dataStoreLocal, appSettings, getServerSettings().getServerName());

				} else {

					boolean result = dataStoreLocal.updateStatus(streamName, BROADCAST_STATUS_BROADCASTING);
					logger.info(" Status of stream {} is set to Broadcasting with result: {}", broadcast.getStreamId(), result);
				}

				final String listenerHookURL = broadcast.getListenerHookURL();
				final String streamId = broadcast.getStreamId();
				if (listenerHookURL != null && listenerHookURL.length() > 0) {
					final String name = broadcast.getName();
					final String category = broadcast.getCategory();
					vertx.runOnContext(e -> notifyHook(listenerHookURL, streamId, HOOK_ACTION_START_LIVE_STREAM, name, category,
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






	public static Broadcast saveUndefinedBroadcast(String streamId, String scopeName, DataStore dataStore, AppSettings appSettings, String fqdn) {
		return saveUndefinedBroadcast(streamId, scopeName, dataStore, appSettings, AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING, fqdn);
	}

	public static Broadcast saveUndefinedBroadcast(String streamId, String scopeName, DataStore dataStore, AppSettings appSettings, String streamStatus, String fqdn) {
		Broadcast newBroadcast = new Broadcast();
		newBroadcast.setDate(System.currentTimeMillis());
		newBroadcast.setZombi(true);
		try {
			newBroadcast.setStreamId(streamId);

			String settingsListenerHookURL = null; 
			if (appSettings != null) {
				settingsListenerHookURL = appSettings.getListenerHookURL();
			}

			return BroadcastRestService.saveBroadcast(newBroadcast,
					streamStatus, scopeName, dataStore,
					settingsListenerHookURL, fqdn);
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

			vertx.runOnContext(e ->
			notifyHook(finalListenerHookURL, streamId, HOOK_ACTION_VOD_READY, null, null, baseName, vodId)	
					);

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

	private static class AuthCheckJob implements IScheduledJob {

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

		@Override
		public void execute(ISchedulingService service) throws CloneNotSupportedException {

			try {
				if (!videoServiceEndpoint.askIfDeviceAuthenticated()) {
					count++;
					if (count < 10) {
						if (videoServiceEndpoint.getError() == null) {
							service.addScheduledOnceJob(interval, new AuthCheckJob(count, interval, videoServiceEndpoint, appAdapter));
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
		addScheduledOnceJob(timeDelta, new AuthCheckJob(0, timeDelta, videoServiceEndpoint, this));
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
			httpPost.addHeader("User-Agent", "Daaavuuuuuttttt https://www.youtube.com/watch?v=cbyTDRgW4Jg");

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
				broadcast.getType().equals(AntMediaApplicationAdapter.STREAM_SOURCE)) 
		{
			result = streamFetcherManager.stopStreaming(broadcast);
		} 
		else if (broadcast.getType().equals(AntMediaApplicationAdapter.LIVE_STREAM)) 
		{
			IBroadcastStream broadcastStream = getBroadcastStream(getScope(), broadcast.getStreamId());
			if (broadcastStream != null) 
			{
				((IClientBroadcastStream) broadcastStream).getConnection().close();
				result.setSuccess(true);
			}
		}
		return result;
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
		logger.info("update source quality for stream: {} quality:{} speed:{}", id, quality, speed);
		getDataStore().updateSourceQualityParameters(id, quality, speed, pendingPacketSize);

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


	@Override
	public void serverShuttingdown() {
		logger.info("{} is shutting down.", getName());
		if (streamFetcherManager != null) {
			Queue<StreamFetcher> fetchers = streamFetcherManager.getStreamFetcherList();
			for (StreamFetcher streamFetcher : fetchers) {
				streamFetcher.stopStream();
			}
		}

		logger.info("RTMP Broadcasts are closing.");
		for (MuxAdaptor adaptor : getMuxAdaptors()) {
			if(adaptor.getBroadcast().getType().equals(AntMediaApplicationAdapter.LIVE_STREAM)) {
				adaptor.getBroadcastStream().stop();
				adaptor.stop();
			}
		}

		while(getDataStore().getLocalLiveBroadcastCount() > 0) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				logger.error(ExceptionUtils.getStackTrace(e));
				Thread.currentThread().interrupt();
			}
		}

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

		//************************************
		//ATTENTION: When a new settings added both updateAppSettingsFile 
		// && updateAppSettingsBean should be updated
		//*************************************
		if (updateAppSettingsFile(getScope().getName(), newSettings))
		{
			AcceptOnlyStreamsInDataStore securityHandler = (AcceptOnlyStreamsInDataStore)  getContext().getBean(AcceptOnlyStreamsInDataStore.BEAN_NAME);
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
	
	
	private boolean updateAppSettingsFile(String appName, AppSettings appsettings) 
	{
		PreferenceStore store = new PreferenceStore("webapps/"+appName+"/WEB-INF/red5-web.properties");

		store.put(AppSettings.SETTINGS_MP4_MUXING_ENABLED, String.valueOf(appsettings.isMp4MuxingEnabled()));
		store.put(AppSettings.SETTINGS_ADD_DATE_TIME_TO_MP4_FILE_NAME, String.valueOf(appsettings.isAddDateTimeToMp4FileName()));
		store.put(AppSettings.SETTINGS_HLS_MUXING_ENABLED, String.valueOf(appsettings.isHlsMuxingEnabled()));
		store.put(AppSettings.SETTINGS_ACCEPT_ONLY_STREAMS_IN_DATA_STORE, String.valueOf(appsettings.isAcceptOnlyStreamsInDataStore()));
		store.put(AppSettings.SETTINGS_OBJECT_DETECTION_ENABLED, String.valueOf(appsettings.isObjectDetectionEnabled()));
		store.put(AppSettings.SETTINGS_TOKEN_CONTROL_ENABLED, String.valueOf(appsettings.isTokenControlEnabled()));
		store.put(AppSettings.SETTINGS_WEBRTC_ENABLED, String.valueOf(appsettings.isWebRTCEnabled()));
		store.put(AppSettings.SETTINGS_WEBRTC_FRAME_RATE, String.valueOf(appsettings.getWebRTCFrameRate()));
		store.put(AppSettings.SETTINGS_HASH_CONTROL_PUBLISH_ENABLED, String.valueOf(appsettings.isHashControlPublishEnabled()));
		store.put(AppSettings.SETTINGS_HASH_CONTROL_PLAY_ENABLED, String.valueOf(appsettings.isHashControlPlayEnabled()));
		
		store.put(AppSettings.SETTINGS_REMOTE_ALLOWED_CIDR, appsettings.getRemoteAllowedCIDR() != null 
																? appsettings.getRemoteAllowedCIDR() 
																: DEFAULT_LOCALHOST);
		
		store.put(AppSettings.SETTINGS_VOD_FOLDER, appsettings.getVodFolder());
		store.put(AppSettings.SETTINGS_HLS_LIST_SIZE, String.valueOf(appsettings.getHlsListSize()));
		store.put(AppSettings.SETTINGS_HLS_TIME, String.valueOf(appsettings.getHlsTime()));
		store.put(AppSettings.SETTINGS_HLS_PLAY_LIST_TYPE, appsettings.getHlsPlayListType());
		store.put(AppSettings.SETTINGS_ENCODER_SETTINGS_STRING, AppSettings.encodersList2Str(appsettings.getEncoderSettings()));
		store.put(AppSettings.TOKEN_HASH_SECRET, appsettings.getTokenHashSecret());
		store.put(AppSettings.SETTINGS_PREVIEW_OVERWRITE, String.valueOf(appsettings.isPreviewOverwrite()));

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

		appSettings.setVodFolder(newSettings.getVodFolder());
		appSettings.setPreviewOverwrite(newSettings.isPreviewOverwrite());

		synchUserVoDFolder(oldVodFolder, newSettings.getVodFolder());

		logger.warn("app settings updated for {}", getScope().getName());	
	}

}
