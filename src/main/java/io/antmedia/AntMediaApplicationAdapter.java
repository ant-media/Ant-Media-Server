package io.antmedia;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

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

import io.antmedia.datastore.db.DataStoreFactory;
import io.antmedia.datastore.db.DataStore;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.datastore.db.types.Endpoint;
import io.antmedia.datastore.db.types.SocialEndpointCredentials;
import io.antmedia.datastore.db.types.VoD;
import io.antmedia.ipcamera.OnvifCamera;
import io.antmedia.muxer.IAntMediaStreamHandler;
import io.antmedia.rest.BroadcastRestService;
import io.antmedia.rest.model.Result;
import io.antmedia.shutdown.AMSShutdownManager;
import io.antmedia.shutdown.IShutdownListener;
import io.antmedia.social.endpoint.PeriscopeEndpoint;
import io.antmedia.social.endpoint.VideoServiceEndpoint;
import io.antmedia.social.endpoint.VideoServiceEndpoint.DeviceAuthParameters;
import io.antmedia.streamsource.StreamFetcher;
import io.antmedia.streamsource.StreamFetcherManager;
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

	protected static Logger logger = LoggerFactory.getLogger(AntMediaApplicationAdapter.class);
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
	private DataStore dataStore;
	DataStoreFactory dataStoreFactory;

	private AppSettings appSettings;
	private Vertx vertx;


	@Override
	public boolean appStart(IScope app) {
		vertx = (Vertx) getContext().getBean(VERTX_BEAN_NAME);

		//initalize to access the data store directly in the code
		getDataStore();


		if (getStreamPublishSecurityList() != null) {
			for (IStreamPublishSecurity streamPublishSecurity : getStreamPublishSecurityList()) {
				registerStreamPublishSecurity(streamPublishSecurity);
			}
		}
		String scheduledJobName = addScheduledOnceJob(0, new IScheduledJob() {

			@Override
			public void execute(ISchedulingService service) throws CloneNotSupportedException {
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

				if (appSettings != null) {
					synchUserVoDFolder(null, appSettings.getVodFolder());
				}
			}
		});

		logger.info("AppStart scheduled job name: {}", scheduledJobName);
		
		AMSShutdownManager.getInstance().subscribe(this);


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
		vertx.executeBlocking(future -> {
			try {
				closeBroadcast(streamName);
				future.complete(true);
			}
			catch (Exception e) {
				logger.error(ExceptionUtils.getStackTrace(e));
				future.complete(false);
			}
		},
				result -> 
		logger.info("close broadcast operation for {} is finished with {}", streamName, result.result())
				);


		super.streamBroadcastClose(stream);
	}

	public void closeBroadcast(String streamName) {

		try {

			if (dataStore != null) {
				getDataStore().updateStatus(streamName, BROADCAST_STATUS_FINISHED);
				Broadcast broadcast = getDataStore().get(streamName);
								
				if (broadcast != null) {
					final String listenerHookURL = broadcast.getListenerHookURL();
					final String streamId = broadcast.getStreamId();
					if (listenerHookURL != null && listenerHookURL.length() > 0) {
						final String name = broadcast.getName();
						final String category = broadcast.getCategory();
						addScheduledOnceJob(100, new IScheduledJob() {

							@Override
							public void execute(ISchedulingService service) throws CloneNotSupportedException {
								notifyHook(listenerHookURL, streamId, HOOK_ACTION_END_LIVE_STREAM, name, category,
										null, null);
							}
						});
					}

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
					}
					// recreate endpoints for social media

					if (endPointList != null) {
						recreateEndpointsForSocialMedia(broadcast, endPointList);
					}

					if (broadcast.isZombi()) {
						getDataStore().delete(streamName);
					}

				}

			}
		} catch (Exception e) {
			logger.error(ExceptionUtils.getStackTrace(e));
		}
	}

	public void recreateEndpointsForSocialMedia(Broadcast broadcast, List<Endpoint> endPointList) {
		for (Endpoint endpoint : endPointList) {

			if (!"".equals(endpoint.type)) {
				VideoServiceEndpoint videoServiceEndPoint = getVideoServiceEndPoint(endpoint.getEndpointServiceId());
				if (videoServiceEndPoint != null) {
					Endpoint newEndpoint;
					try {
						newEndpoint = videoServiceEndPoint.createBroadcast(broadcast.getName(),
								broadcast.getDescription(), broadcast.getStreamId(), broadcast.isIs360(), broadcast.isPublicStream(), 720, true);
						getDataStore().removeEndpoint(broadcast.getStreamId(), endpoint);
						getDataStore().addEndpoint(broadcast.getStreamId(), newEndpoint);
					} catch (Exception e) {
						logger.error(ExceptionUtils.getStackTrace(e));
					}

				}
			}
		}
	}

	public VideoServiceEndpoint getEndpointService(String className, 
			SocialEndpointCredentials socialEndpointCredentials, String clientId, String clientSecret)
	{
		try {
			VideoServiceEndpoint endPointService;
			Class endpointClass = Class.forName(className);

			endPointService = (VideoServiceEndpoint) endpointClass.getConstructor(String.class, String.class, DataStore.class, SocialEndpointCredentials.class, Vertx.class)
					.newInstance(clientId, clientSecret, dataStore, socialEndpointCredentials, vertx);
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
		addScheduledOnceJob(0, service -> {
			if (dataStore != null) {
				dataStore.updateRtmpViewerCount(item.getName(), true);
			}

		});
	}

	@Override
	public void streamPlayItemStop(ISubscriberStream stream, IPlayItem item) {
		super.streamPlayItemStop(stream, item);
		addScheduledOnceJob(0, service -> {
			if (dataStore != null) {
				dataStore.updateRtmpViewerCount(item.getName(), false);
			}
		});
	}

	@Override
	public void streamSubscriberClose(ISubscriberStream stream) {
		super.streamSubscriberClose(stream);
		addScheduledOnceJob(0, service -> {
			if (dataStore != null) {
				dataStore.updateRtmpViewerCount(stream.getBroadcastStreamPublishName(), false);
			}
		});
	}

	@Override
	public void streamPublishStart(final IBroadcastStream stream) {
		String streamName = stream.getPublishedName();
		logger.info("stream name in streamPublishStart: {}", streamName );

		startPublish(streamName);

		super.streamPublishStart(stream);
	}

	public void startPublish(String streamName) {
		addScheduledOnceJob(0, new IScheduledJob() {

			@Override
			public void execute(ISchedulingService service) throws CloneNotSupportedException {

				try {

					DataStore dataStoreLocal = getDataStore();
					if (dataStoreLocal != null) {

						Broadcast broadcast = dataStoreLocal.get(streamName);

						if (broadcast == null) {

							broadcast = saveUndefinedBroadcast(streamName, getScope().getName(), dataStoreLocal, appSettings);

						} else {

							boolean result = dataStoreLocal.updateStatus(streamName, BROADCAST_STATUS_BROADCASTING);
							logger.info(" Status of stream {} is set to Broadcasting with result: {}", broadcast.getStreamId(), result);
						}

						final String listenerHookURL = broadcast.getListenerHookURL();
						final String streamId = broadcast.getStreamId();
						if (listenerHookURL != null && listenerHookURL.length() > 0) {
							final String name = broadcast.getName();
							final String category = broadcast.getCategory();
							addScheduledOnceJob(100, new IScheduledJob() {

								@Override
								public void execute(ISchedulingService service) throws CloneNotSupportedException {
									notifyHook(listenerHookURL, streamId, HOOK_ACTION_START_LIVE_STREAM, name, category,
											null, null);
								}
							});
						}

						List<Endpoint> endPointList = broadcast.getEndPointList();
						if (endPointList != null) {
							for (Endpoint endpoint : endPointList) {
								VideoServiceEndpoint videoServiceEndPoint = getVideoServiceEndPoint(endpoint.getEndpointServiceId());
								if (videoServiceEndPoint != null) {
									try {
										videoServiceEndPoint.publishBroadcast(endpoint);
										log.info("publish broadcast called for {}" , videoServiceEndPoint.getName());
									} catch (Exception e) {
										logger.error(ExceptionUtils.getStackTrace(e));
									}
								}

							}
						}
					}
				} catch (Exception e) {
					logger.error(ExceptionUtils.getStackTrace(e));
				}
			}

		});
	}

	public static Broadcast saveUndefinedBroadcast(String streamId, String scopeName, DataStore dataStore, AppSettings appSettings) {
		return saveUndefinedBroadcast(streamId, scopeName, dataStore, appSettings, AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING);
	}
	
	public static Broadcast saveUndefinedBroadcast(String streamId, String scopeName, DataStore dataStore, AppSettings appSettings, String streamStatus) {
		Broadcast newBroadcast = new Broadcast();
		newBroadcast.setDate(System.currentTimeMillis());
		newBroadcast.setZombi(true);
		try {
			newBroadcast.setStreamId(streamId);

			String settingsListenerHookURL = null; 
			String fqdn = null;
			if (appSettings != null) {
				settingsListenerHookURL = appSettings.getListenerHookURL();
				fqdn = appSettings.getServerName();
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
		if (broadcast != null) {
			streamName = broadcast.getName();
			listenerHookURL = broadcast.getListenerHookURL();
			if (resolution != 0) {
				streamName = streamName + " (" + resolution + "p)";

			}
		}

		String vodId = RandomStringUtils.randomNumeric(24);
		VoD newVod = new VoD(streamName, streamId, relativePath, vodName, systemTime, duration, fileSize, VoD.STREAM_VOD, vodId);

		if (getDataStore().addVod(newVod) == null) {
			logger.warn("Stream vod with stream id {} cannot be added to data store", streamId);
		}

		int index;
		//HOOK_ACTION_VOD_READY is called only the stream in the datastore
		//it is not called for zombi streams
		if (listenerHookURL != null && !listenerHookURL.isEmpty() && 
				(index = vodName.lastIndexOf(".mp4")) != -1) 
		{
			final String baseName = vodName.substring(0, index);
			String finalListenerHookURL = listenerHookURL;
			addScheduledOnceJob(100, new IScheduledJob() {

				@Override
				public void execute(ISchedulingService service) throws CloneNotSupportedException {
					notifyHook(finalListenerHookURL, streamId, HOOK_ACTION_VOD_READY, null, null, baseName, vodId);
				}
			});
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

		if(appSettings == null) {

			AppSettings appSettingsTmp = new AppSettings();

			appSettingsTmp.setMp4MuxingEnabled(true);
			appSettingsTmp.setAddDateTimeToMp4FileName(true);
			appSettingsTmp.setWebRTCEnabled(false);
			appSettingsTmp.setHlsMuxingEnabled(true);
			appSettingsTmp.setObjectDetectionEnabled(false);
			appSettingsTmp.setAdaptiveResolutionList(null);
			appSettingsTmp.setHlsListSize(null);
			appSettingsTmp.setHlsTime(null);
			appSettingsTmp.setHlsPlayListType(null);
			appSettingsTmp.setDeleteHLSFilesOnEnded(true);
			appSettingsTmp.setPreviewOverwrite(false);
			appSettingsTmp.setTokenControlEnabled(false);
			this.appSettings=appSettingsTmp;
		}

		return appSettings;
	}

	public void setAppSettings(AppSettings appSettings) {
		this.appSettings = appSettings;
	}


	public StreamFetcher startStreaming(Broadcast broadcast) {
		return streamFetcherManager.startStreaming(broadcast);
	}

	public Result stopStreaming(Broadcast broadcast) {
		Result result = new Result(false);
		if(broadcast.getType().equals(AntMediaApplicationAdapter.IP_CAMERA)|| broadcast.getType().equals(AntMediaApplicationAdapter.STREAM_SOURCE)) {
			result = streamFetcherManager.stopStreaming(broadcast);
		} 
		else if (broadcast.getType().equals(AntMediaApplicationAdapter.LIVE_STREAM)) {
			IBroadcastStream broadcastStream = getBroadcastStream(getScope(), broadcast.getStreamId());
			if (broadcastStream != null) {
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
	}
}
