package org.red5.server.adapter;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
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
import org.red5.server.api.scheduling.IScheduledJob;
import org.red5.server.api.scheduling.ISchedulingService;
import org.red5.server.api.stream.IBroadcastStream;

import io.antmedia.datastore.db.IDataStore;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.datastore.db.types.Endpoint;
import io.antmedia.muxer.IMuxerListener;
import io.antmedia.social.endpoint.VideoServiceEndpoint;
import io.antmedia.social.endpoint.VideoServiceEndpoint.DeviceAuthParameters;

public class AntMediaApplicationAdapter extends MultiThreadedApplicationAdapter implements IMuxerListener{

	public static final String BROADCAST_STATUS_CREATED = "created";
	public static final String BROADCAST_STATUS_BROADCASTING = "broadcasting";
	public static final String BROADCAST_STATUS_FINISHED = "finished";
	public static final String HOOK_ACTION_END_LIVE_STREAM = "endLiveStream";
	

	private List<VideoServiceEndpoint> videoServiceEndpoints;
	private IDataStore dataStore;
	public IDataStore getDataStore() {
		return dataStore;
	}
	public void setDataStore(IDataStore dataStore) {
		this.dataStore = dataStore;
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
					if (listenerHookURL != null && listenerHookURL.length() > 0) 
					{
						addScheduledOnceJob(100, new IScheduledJob() {
							
							@Override
							public void execute(ISchedulingService service) throws CloneNotSupportedException {
								notifyHook(listenerHookURL, streamId, HOOK_ACTION_END_LIVE_STREAM);
							}
						});
					}
								
					List<Endpoint> endPointList = broadcast.getEndPointList();
					if (endPointList != null) {
						for (Endpoint endpoint : endPointList) {
							VideoServiceEndpoint videoServiceEndPoint = getVideoServiceEndPoint(stream, endpoint.type);
							if (videoServiceEndPoint != null) {
								try {
									videoServiceEndPoint.stopBroadcast(endpoint);
								}
								catch (Exception e) {
									e.printStackTrace();
								}
							}
						}
					}
					
					//recreate endpoints for social media
					if (endPointList != null) {
						recreateEndpointsForSocialMedia(broadcast, endPointList);
					}
					
					
				}

			}
		}
		catch (Exception e){
			e.printStackTrace();
		}
		super.streamBroadcastClose(stream);
	}
	
	public void recreateEndpointsForSocialMedia(Broadcast broadcast, List<Endpoint> endPointList) {
		for (Endpoint endpoint : endPointList) {
			
			if (endpoint.type != null && !endpoint.type.equals("")) {
				VideoServiceEndpoint videoServiceEndPoint = getVideoServiceEndPoint(null, endpoint.type);
				if (videoServiceEndPoint != null) {
					Endpoint newEndpoint;
					try {
						newEndpoint = videoServiceEndPoint.createBroadcast(broadcast.getName(), broadcast.getDescription(), broadcast.isIs360(), broadcast.isPublicStream(), 720);
						getDataStore().removeEndpoint(broadcast.getStreamId(), endpoint);
						getDataStore().addEndpoint(broadcast.getStreamId(), newEndpoint);
					} catch (Exception e) {
						e.printStackTrace();
					}
					
				}
			}
		}
	}

	@Override
	public void streamPublishStart(IBroadcastStream stream) {
		try {
			String streamName = stream.getPublishedName();
			if (dataStore != null) {
				dataStore.updateStatus(streamName, BROADCAST_STATUS_BROADCASTING);

				Broadcast broadcast = dataStore.get(streamName);
				if (broadcast != null) {
					List<Endpoint> endPointList = broadcast.getEndPointList();
					if (endPointList != null) {
						for (Endpoint endpoint : endPointList) {
							VideoServiceEndpoint videoServiceEndPoint = getVideoServiceEndPoint(stream, endpoint.type);
							if (videoServiceEndPoint != null) {
								videoServiceEndPoint.publishBroadcast(endpoint);
							}
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		super.streamPublishStart(stream);
	}

	//TODO: make video serviceEndpoinst HashMap
	protected VideoServiceEndpoint getVideoServiceEndPoint(IBroadcastStream stream, String type) {
		if (videoServiceEndpoints != null) {
			for (VideoServiceEndpoint serviceEndpoint : videoServiceEndpoints) {
				if (serviceEndpoint.getName().equals(type)) {
					return serviceEndpoint;
				}
			}
		}
		return null;
	}

	@Override
	public void muxingFinished(File file, long duration) {
		String name = file.getName();
		if (dataStore != null) {
			int index;
			// reg expression of a translated file, kdjf03030_240p.mp4
			String regularExp = "^.*_{1}[0-9]{3}p{1}\\.mp4{1}$"; 
			if (!name.matches(regularExp) && (index = name.lastIndexOf(".mp4")) != -1) {
				name = name.substring(0, index);
				dataStore.updateDuration(name, duration);
			}
		}
	}

	private static class AuthCheckJob implements IScheduledJob {

		private int count;
		private VideoServiceEndpoint videoServiceEndpoint;
		private int interval;

		public AuthCheckJob(int count, int interval, VideoServiceEndpoint videoServiceEndpoint) {
			this.count = count;
			this.videoServiceEndpoint = videoServiceEndpoint;
			this.interval = interval;
		}

		@Override
		public void execute(ISchedulingService service) throws CloneNotSupportedException {
			
			
			try {
				if (!videoServiceEndpoint.askIfDeviceAuthenticated()) {
					count++;
					if (count < 10) {
						service.addScheduledOnceJob(interval, new AuthCheckJob(count, interval, videoServiceEndpoint));
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public void startDeviceAuthStatusPolling(VideoServiceEndpoint videoServiceEndpoint, DeviceAuthParameters askDeviceAuthParameters) 
	{
		int timeDelta = askDeviceAuthParameters.interval * 1000;
		addScheduledOnceJob(timeDelta, new AuthCheckJob(0, timeDelta, videoServiceEndpoint));
	}

	public List<VideoServiceEndpoint> getVideoServiceEndpoints() {
		return videoServiceEndpoints;
	}

	public void setVideoServiceEndpoints(List<VideoServiceEndpoint> videoServiceEndpoints) {
		this.videoServiceEndpoints = videoServiceEndpoints;
	}
	
	
	public static StringBuffer notifyHook(String url, String id, String action) {
		Map<String, String> variables = new HashMap<>();
		variables.put("id", id);
		variables.put("action", action);
		StringBuffer response = null;
		try {
			response = sendPOST(url, variables);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return response;
	}
	
	public static StringBuffer sendPOST(String url, Map<String, String> variables) throws IOException {

		CloseableHttpClient httpClient = HttpClients.createDefault();
		HttpPost httpPost = new HttpPost(url);
		httpPost.addHeader("User-Agent", "Daaavuuuuuttttt https://www.youtube.com/watch?v=cbyTDRgW4Jg");

		List<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
		Set<Entry<String,String>> entrySet = variables.entrySet();
		for (Entry<String, String> entry : entrySet) {
			urlParameters.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
		}
		

		HttpEntity postParams = new UrlEncodedFormEntity(urlParameters);
		httpPost.setEntity(postParams);

		CloseableHttpResponse httpResponse = httpClient.execute(httpPost);

		System.out.println("POST Response Status:: "
				+ httpResponse.getStatusLine().getStatusCode());

		BufferedReader reader = new BufferedReader(new InputStreamReader(
				httpResponse.getEntity().getContent()));

		String inputLine;
		StringBuffer response = new StringBuffer();

		while ((inputLine = reader.readLine()) != null) {
			response.append(inputLine);
		}
		reader.close();

		// print result
		httpClient.close();
		
		return response;

	}

}
