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

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.datastore.db.IDataStore;
import io.antmedia.datastore.db.types.Broadcast;


/**
 * Organizes and checks stream fetcher and restarts them if it is required
 * @author davut
 *
 */
public class StreamFetcherManager {

	protected static Logger logger = LoggerFactory.getLogger(StreamFetcherManager.class);

	private int streamCheckerCount = 0;

	private List<StreamFetcher> streamFetcherList = new ArrayList<>();

	private int streamCheckerInterval = 10000;

	private ISchedulingService schedulingService;

	private IDataStore datastore;
	
	public StreamFetcherManager(ISchedulingService schedulingService, IDataStore datastore) {
		this.schedulingService = schedulingService;
		this.datastore = datastore;
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
		streamFetcherList.add(streamScheduler);

	}

	public List<StreamFetcher> getCamSchedulerList() {
		return streamFetcherList;
	}

	public void stopStreaming(Broadcast stream) {
		logger.warn("inside of stopStreaming");

		for (StreamFetcher streamScheduler : streamFetcherList) {
			if (streamScheduler.getStream().getStreamId().equals(stream.getStreamId())) {
				streamScheduler.stopStream();
				streamFetcherList.remove(streamScheduler);
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

				if (streamFetcherList.size() > 0) {

					streamCheckerCount++;

					logger.warn("checkerCount is  :" + streamCheckerCount);

					if (streamCheckerCount % 180 == 0) {

						for (StreamFetcher streamScheduler : streamFetcherList) {
							if (streamScheduler.isStreamAlive()) {
								streamScheduler.stopStream();
							}
							streamScheduler.startStream();
						}

					} else {
						for (StreamFetcher streamScheduler : streamFetcherList) {
							if (!streamScheduler.isStreamAlive()) {
								if (datastore != null) {
									logger.info("Updating stream status to finished, updating status of stream {}", streamScheduler.getStream().getStreamId());
									
									datastore.updateStatus(streamScheduler.getStream().getStreamId(), 
											AntMediaApplicationAdapter.BROADCAST_STATUS_FINISHED);
								}
								streamScheduler.startStream();
							}
						}
					}
				}
			}
		}, 5000);

	}

	public IDataStore getDatastore() {
		return datastore;
	}

	public void setDatastore(IDataStore datastore) {
		this.datastore = datastore;
	}

}
