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

public class StreamSources {


	protected static Logger logger = LoggerFactory.getLogger(StreamSources.class);
	public static final String BROADCAST_STATUS_FINISHED = "finished";

	private int streamCheckerCount = 0;

	private List<StreamFetcher> schedulerList = new ArrayList<>();

	private int streamCheckerInterval = 10000;

	private ISchedulingService schedulingService;
	
	public StreamSources(ISchedulingService schedulingService) {
		this.schedulingService = schedulingService;
	}

	public int getStreamCheckerInterval() {
		return streamCheckerInterval;
	}


	public void setStreamCheckerInterval(int streamCheckerInterval) {
		this.streamCheckerInterval = streamCheckerInterval;
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

		schedulingService.addScheduledJobAfterDelay(streamCheckerInterval, new IScheduledJob() {

			@Override
			public void execute(ISchedulingService service) throws CloneNotSupportedException {

				if (schedulerList.size() > 0) {

					streamCheckerCount++;

					logger.warn("checkerCount is  :" + streamCheckerCount);

					if (streamCheckerCount % 180 == 0) {

						for (StreamFetcher streamScheduler : schedulerList) {
							if (streamScheduler.isRunning()) {
								streamScheduler.stopStream();
							}
							streamScheduler.startStream();
						}

					} else {
						for (StreamFetcher streamScheduler : schedulerList) {
							if (!streamScheduler.isRunning()) {
								streamScheduler.getStream().setStatus(BROADCAST_STATUS_FINISHED);
								streamScheduler.startStream();
							}
						}
					}
				}
			}
		}, 5000);

	}

}
