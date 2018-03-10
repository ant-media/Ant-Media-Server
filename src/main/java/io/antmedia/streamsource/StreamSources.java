package io.antmedia.streamsource;

import java.util.ArrayList;
import java.util.List;

import org.red5.logging.Red5LoggerFactory;
import org.red5.server.adapter.MultiThreadedApplicationAdapter;
import org.red5.server.api.scheduling.IScheduledJob;
import org.red5.server.api.scheduling.ISchedulingService;
import org.red5.server.api.scope.IScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.ipcamera.IPCameraApplicationAdapter;

public class StreamSources {

	private MultiThreadedApplicationAdapter adaptor;

	protected static Logger logger = LoggerFactory.getLogger(StreamSources.class);

	private int streamCheckerCount = 0;

	private static Logger log = Red5LoggerFactory.getLogger(IPCameraApplicationAdapter.class);

	private List<StreamFetcher> schedulerList = new ArrayList<>();

	private int streamCheckerInterval = 60000;

	public StreamSources(IScope app) {

		adaptor = new MultiThreadedApplicationAdapter();

		adaptor.setScope(app);

	}

	public int getCameraCheckerInterval() {
		return streamCheckerInterval;
	}

	public void setCameraCheckerInterval(int cameraCheckerInterval) {
		this.streamCheckerInterval = cameraCheckerInterval;
	}

	public void startStreaming(Broadcast broadcast) {

		StreamFetcher streamScheduler = new StreamFetcher(broadcast);
		streamScheduler.startStream();
		schedulerList.add(streamScheduler);

	}

	public List<StreamFetcher> getCamSchedulerList() {
		return schedulerList;
	}

	public void stopStreaming(Broadcast stream) {
		logger.warn("inside of stopStreaming");

		for (StreamFetcher streamScheduler : schedulerList) {
			if (streamScheduler.getStream().getStreamId().equals(stream.getStreamId())) {
				streamScheduler.stopStream();
				schedulerList.remove(streamScheduler);
				break;
			}

		}

	}

	public void startStreams(List<Broadcast> streams) {

		for (int i = 0; i < streams.size(); i++) {
			startStreaming(streams.get(i));
		}

		adaptor.addScheduledJobAfterDelay(streamCheckerInterval, new IScheduledJob() {

			@Override
			public void execute(ISchedulingService service) throws CloneNotSupportedException {

				if (schedulerList.size() > 0) {

					streamCheckerCount++;

					logger.warn("checkerCount is  :" + streamCheckerCount);

					if (streamCheckerCount % 60 == 0) {

						for (StreamFetcher streamScheduler : schedulerList) {
							if (streamScheduler.isRunning()) {
								streamScheduler.stopStream();
							}
							streamScheduler.startStream();
						}

					} else {
						for (StreamFetcher streamScheduler : schedulerList) {
							if (!streamScheduler.isRunning()) {
								streamScheduler.getStream().setStatus("finished");
								streamScheduler.startStream();
							}
						}
					}

				}
			}
		}, 5000);

	}

}
