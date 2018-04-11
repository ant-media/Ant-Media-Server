package io.antmedia;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

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
import org.red5.server.api.stream.IStreamPublishSecurity;
import org.red5.server.stream.ClientBroadcastStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.antmedia.datastore.db.IDataStore;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.datastore.db.types.Endpoint;
import io.antmedia.datastore.db.types.SocialEndpointCredentials;
import io.antmedia.datastore.db.types.Vod;
import io.antmedia.ipcamera.OnvifCamera;
import io.antmedia.muxer.IMuxerListener;
import io.antmedia.muxer.MuxAdaptor;
import io.antmedia.rest.BroadcastRestService;
import io.antmedia.social.endpoint.PeriscopeEndpoint;
import io.antmedia.social.endpoint.VideoServiceEndpoint;
import io.antmedia.social.endpoint.VideoServiceEndpoint.DeviceAuthParameters;
import io.antmedia.streamsource.StreamFetcherManager;

public class AntMediaApplicationAdapter extends MultiThreadedApplicationAdapter implements IMuxerListener {

	public static final String BROADCAST_STATUS_CREATED = "created";
	public static final String BROADCAST_STATUS_BROADCASTING = "broadcasting";
	public static final String BROADCAST_STATUS_FINISHED = "finished";
	public static final String HOOK_ACTION_END_LIVE_STREAM = "liveStreamEnded";
	public static final String HOOK_ACTION_START_LIVE_STREAM = "liveStreamStarted";
	public static final String HOOK_ACTION_VOD_READY = "vodReady";
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



	private List<VideoServiceEndpoint> videoServiceEndpoints = new ArrayList<>();
	private List<VideoServiceEndpoint> videoServiceEndpointsHavingError = new ArrayList<>();
	
	private List<IStreamPublishSecurity> streamPublishSecurityList;

	private HashMap<String, OnvifCamera> onvifCameraList = new HashMap<String, OnvifCamera>();

	private StreamFetcherManager streamFetcherManager;

	private IDataStore dataStore;

	public IDataStore getDataStore() {
		return dataStore;
	}

	public void setDataStore(IDataStore dataStore) {
		this.dataStore = dataStore;
	}

	private AppSettings appSettings;
	

	@Override
	public boolean appStart(IScope app) {

		if (getStreamPublishSecurityList() != null) {
			for (IStreamPublishSecurity streamPublishSecurity : getStreamPublishSecurityList()) {
				registerStreamPublishSecurity(streamPublishSecurity);
			}
		}
		addScheduledOnceJob(0, new IScheduledJob() {

			@Override
			public void execute(ISchedulingService service) throws CloneNotSupportedException {
				streamFetcherManager = new StreamFetcherManager(AntMediaApplicationAdapter.this, dataStore);
				List<Broadcast> streams = getDataStore().getExternalStreamsList();
				streamFetcherManager.startStreams(streams);

				List<SocialEndpointCredentials> socialEndpoints = dataStore.getSocialEndpoints(0, END_POINT_LIMIT);

				for (SocialEndpointCredentials socialEndpointCredentials : socialEndpoints) 
				{
					VideoServiceEndpoint endPointService = null;
					if (socialEndpointCredentials.getServiceName().equals(FACEBOOK)) 
					{
						endPointService = getEndpointService(FACEBOOK_ENDPOINT_CLASS, socialEndpointCredentials, appSettings.getFacebookClientId(), appSettings.getFacebookClientSecret());
					}
					else if (socialEndpointCredentials.getServiceName().equals(PERISCOPE)) 
					{
						endPointService = new PeriscopeEndpoint(appSettings.getPeriscopeClientId(), 
								appSettings.getPeriscopeClientSecret(), dataStore, socialEndpointCredentials);
					}
					else if (socialEndpointCredentials.getServiceName().equals(YOUTUBE)) 
					{
						endPointService = getEndpointService(YOUTUBE_ENDPOINT_CLASS, socialEndpointCredentials, appSettings.getYoutubeClientId(), appSettings.getYoutubeClientSecret());
					}

					if (endPointService != null) {
						videoServiceEndpoints.add(endPointService);
					}
				}

			}


		});


		return super.appStart(app);
	}

	@Override
	public void streamBroadcastClose(IBroadcastStream stream) {
		try {
			String streamName = stream.getPublishedName();
			if (dataStore != null) {
				dataStore.updateStatus(streamName, BROADCAST_STATUS_FINISHED);
				Broadcast broadcast = dataStore.get(streamName);

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
										null);
							}
						});
					}

					List<Endpoint> endPointList = broadcast.getEndPointList();
					if (endPointList != null) {
						for (Endpoint endpoint : endPointList) {
							VideoServiceEndpoint videoServiceEndPoint = getVideoServiceEndPoint(endpoint.endpointServiceId);
							if (videoServiceEndPoint != null) {
								try {
									videoServiceEndPoint.stopBroadcast(endpoint);
								} catch (Exception e) {
									e.printStackTrace();
								}
							}
						}
					}

					// recreate endpoints for social media

					if (endPointList != null) {
						recreateEndpointsForSocialMedia(broadcast, endPointList);
					}

					if (broadcast.isZombi()) {
						dataStore.delete(streamName);
					}

				}

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		super.streamBroadcastClose(stream);
	}

	public void recreateEndpointsForSocialMedia(Broadcast broadcast, List<Endpoint> endPointList) {
		for (Endpoint endpoint : endPointList) {

			if (endpoint.type != null && !endpoint.type.equals("")) {
				VideoServiceEndpoint videoServiceEndPoint = getVideoServiceEndPoint(endpoint.endpointServiceId);
				if (videoServiceEndPoint != null) {
					Endpoint newEndpoint;
					try {
						newEndpoint = videoServiceEndPoint.createBroadcast(broadcast.getName(),
								broadcast.getDescription(), broadcast.isIs360(), broadcast.isPublicStream(), 720, true);
						getDataStore().removeEndpoint(broadcast.getStreamId(), endpoint);
						getDataStore().addEndpoint(broadcast.getStreamId(), newEndpoint);
					} catch (Exception e) {
						e.printStackTrace();
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

			endPointService = (VideoServiceEndpoint) endpointClass.getConstructor(String.class, String.class, IDataStore.class, SocialEndpointCredentials.class)
					.newInstance(clientId, clientSecret, dataStore, socialEndpointCredentials);
			return endPointService;
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public void streamPublishStart(final IBroadcastStream stream) {

		addScheduledOnceJob(0, new IScheduledJob() {

			@Override
			public void execute(ISchedulingService service) throws CloneNotSupportedException {
				String streamName = stream.getPublishedName();
				try {

					if (dataStore != null) {

						Broadcast broadcast = dataStore.get(streamName);

						if (broadcast == null) {
							broadcast = saveZombiBroadcast(streamName);

						} else {
							dataStore.updateStatus(streamName, BROADCAST_STATUS_BROADCASTING);
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
											null);
								}
							});
						}

						List<Endpoint> endPointList = broadcast.getEndPointList();
						if (endPointList != null) {
							for (Endpoint endpoint : endPointList) {
								VideoServiceEndpoint videoServiceEndPoint = getVideoServiceEndPoint(endpoint.endpointServiceId);
								if (videoServiceEndPoint != null) {
									try {
										videoServiceEndPoint.publishBroadcast(endpoint);
										log.info("publish broadcast called for " + videoServiceEndPoint.getName());
									} catch (Exception e) {
										e.printStackTrace();
									}
								}

							}
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

		});

		super.streamPublishStart(stream);
	}

	private Broadcast saveZombiBroadcast(String streamName) {
		Broadcast newBroadcast = new Broadcast();
		newBroadcast.setDate(System.currentTimeMillis());
		newBroadcast.setZombi(true);
		try {
			newBroadcast.setStreamId(streamName);

			return BroadcastRestService.saveBroadcast(newBroadcast,
					AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING, getScope().getName(), dataStore,
					appSettings);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	public VideoServiceEndpoint getVideoServiceEndPoint(String id) {
		if (videoServiceEndpoints != null) {
			for (VideoServiceEndpoint serviceEndpoint : videoServiceEndpoints) {
				if (serviceEndpoint.getCredentials().getId().equals(id)) {
					return serviceEndpoint;
				}
			}
		}
		return null;
	}

	@Override
	public void muxingFinished(final String streamId, File file, long duration) {
		String name = file.getName();
		String filePath = file.getPath();
		long fileSize = file.length();
		long unixTime = System.currentTimeMillis();

		String streamName = "";

		if (dataStore != null) {
			int index;
			// reg expression of a translated file, kdjf03030_240p.mp4
			String regularExp = "^.*_{1}[0-9]{3}p{1}\\.mp4{1}$";

			if (!name.matches(regularExp) && (index = name.lastIndexOf(".mp4")) != -1) {
				final String baseName = name.substring(0, index);
				dataStore.updateDuration(streamId, duration);

				Broadcast broadcast = dataStore.get(streamId);

				if (broadcast != null) {
					streamName = broadcast.getName();
				} 
				else {
					streamName = "deleted stream";
				}

				Vod newVod = new Vod(streamName, streamId, filePath, name, unixTime, duration, fileSize, "streamVod");

				getDataStore().addVod(streamId, newVod);

				if (broadcast != null) {
					final String listenerHookURL = broadcast.getListenerHookURL();

					if (listenerHookURL != null && listenerHookURL.length() > 0) {

						addScheduledOnceJob(100, new IScheduledJob() {

							@Override
							public void execute(ISchedulingService service) throws CloneNotSupportedException {
								notifyHook(listenerHookURL, streamId, HOOK_ACTION_VOD_READY, null, null, baseName);
							}
						});
					}
				}

			}
		}
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
					logger.info("Authenticated, adding video service endpoint {} to the app", videoServiceEndpoint.getName());
					this.appAdapter.getVideoServiceEndpoints().add(videoServiceEndpoint);
					
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public void startDeviceAuthStatusPolling(VideoServiceEndpoint videoServiceEndpoint,
			DeviceAuthParameters askDeviceAuthParameters) {
		int timeDelta = askDeviceAuthParameters.interval * 1000;
		addScheduledOnceJob(timeDelta, new AuthCheckJob(0, timeDelta, videoServiceEndpoint, this));
	}

	public List<VideoServiceEndpoint> getVideoServiceEndpoints() {
		return videoServiceEndpoints;
	}

	public List<VideoServiceEndpoint> getVideoServiceEndpointsHavingError(){
		return videoServiceEndpointsHavingError ;
	}
	
	public void setVideoServiceEndpoints(List<VideoServiceEndpoint> videoServiceEndpoints) {
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
	 * 
	 * @return
	 */
	public StringBuffer notifyHook(String url, String id, String action, String streamName, String category,
			String vodName) {
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

		StringBuffer response = null;
		try {
			response = sendPOST(url, variables);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return response;
	}

	public static StringBuffer sendPOST(String url, Map<String, String> variables) throws IOException {

		StringBuffer response = null;
		try (CloseableHttpClient httpClient = HttpClients.createDefault()) 
		{
			HttpPost httpPost = new HttpPost(url);
			httpPost.addHeader("User-Agent", "Daaavuuuuuttttt https://www.youtube.com/watch?v=cbyTDRgW4Jg");

			List<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
			Set<Entry<String, String>> entrySet = variables.entrySet();
			for (Entry<String, String> entry : entrySet) {
				urlParameters.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
			}

			HttpEntity postParams = new UrlEncodedFormEntity(urlParameters);
			httpPost.setEntity(postParams);

			CloseableHttpResponse httpResponse = httpClient.execute(httpPost);

			System.out.println("POST Response Status:: " + httpResponse.getStatusLine().getStatusCode());

			BufferedReader reader = new BufferedReader(new InputStreamReader(httpResponse.getEntity().getContent()));

			String inputLine;
			response = new StringBuffer();

			while ((inputLine = reader.readLine()) != null) {
				response.append(inputLine);
			}
			reader.close();

		}


		return response;

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
	@Override
	public void sourceQualityChanged(String id,String quality) {
		log.info("source stream quality changed, new quality is: "+quality);

		getDataStore().updateSourceQuality(id, quality);
	}

	
	@Override
	public void sourceSpeedChanged(String id,double speed) {
		// log.info("source stream quality changed, new quality is: "+speed);

		getDataStore().updateSourceSpeed(id, speed);
	}
	

	public StreamFetcherManager getSources() {
		return streamFetcherManager;
	}

	public void setSources(StreamFetcherManager sources) {
		this.streamFetcherManager = sources;
	}

	public void startStreaming(Broadcast broadcast) {
		streamFetcherManager.startStreaming(broadcast);
	}

	public void stopStreaming(Broadcast cam) {
		streamFetcherManager.stopStreaming(cam);
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

}
