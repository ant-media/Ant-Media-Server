package org.red5.server.adapter;

import java.io.File;
import java.util.List;

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
					List<Endpoint> endPointList = broadcast.getEndPointList();
					for (Endpoint endpoint : endPointList) {
						VideoServiceEndpoint videoServiceEndPoint = getVideoServiceEndPoint(endpoint.type);
						if (videoServiceEndPoint != null) {
							videoServiceEndPoint.stopBroadcast(endpoint);
						}
					}
				}

			}
		}
		catch (Exception e){
			e.printStackTrace();
		}
		super.streamBroadcastClose(stream);
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
					for (Endpoint endpoint : endPointList) {
						VideoServiceEndpoint videoServiceEndPoint = getVideoServiceEndPoint(endpoint.type);
						if (videoServiceEndPoint != null) {
							videoServiceEndPoint.publishBroadcast(endpoint);
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
	private VideoServiceEndpoint getVideoServiceEndPoint(String type) {
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

}
