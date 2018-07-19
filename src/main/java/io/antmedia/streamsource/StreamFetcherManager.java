package io.antmedia.streamsource;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.red5.server.api.scheduling.IScheduledJob;
import org.red5.server.api.scheduling.ISchedulingService;
import org.red5.server.api.scope.IScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.antmedia.datastore.db.IDataStore;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.muxer.MuxAdaptor;


/**
 * Organizes and checks stream fetcher and restarts them if it is required
 * @author davut
 *
 */
public class StreamFetcherManager {

	protected static Logger logger = LoggerFactory.getLogger(StreamFetcherManager.class);

	private int streamCheckerCount = 0;

	private Queue<StreamFetcher> streamFetcherList = new ConcurrentLinkedQueue<>();

	/**
	 * Time period in milli seconds for checking stream fetchers status, restart issues etc. 
	 */
	private int streamCheckerIntervalMs = 10000;

	private ISchedulingService schedulingService;

	private IDataStore datastore;

	private IScope scope;

	private String streamFetcherScheduleJobName;

	protected AtomicBoolean isJobRunning = new AtomicBoolean(false);

	private boolean restartStreamAutomatically = true;

	/**
	 * Time period in seconds for restarting stream fetchers
	 */
	private int restartStreamFetcherPeriodSeconds;

	public StreamFetcherManager(ISchedulingService schedulingService, IDataStore datastore,IScope scope) {
		this.schedulingService = schedulingService;
		this.datastore = datastore;
		this.scope=scope;
	}

	public StreamFetcher make(Broadcast stream, IScope scope, ISchedulingService schedulingService) {
		return new StreamFetcher(stream, scope, schedulingService);
	}

	public int getStreamCheckerInterval() {
		return streamCheckerIntervalMs;
	}


	/**
	 * Set stream checker interval, this value is used in periodically checking 
	 * the status of the stream fetchers
	 * 
	 * @param streamCheckerInterval, time period of the stream fetcher check interval in milliseconds
	 */
	public void setStreamCheckerInterval(int streamCheckerInterval) {
		this.streamCheckerIntervalMs = streamCheckerInterval;
	}

	/**
	 * Set stream fetcher restart period, this value is used in periodically stopping and starting
	 * stream fetchers. If this value is zero it will not restart stream fetchers
	 * 
	 * @param restartStreamFetcherPeriod, time period of the stream fetcher restart period in seconds
	 */
	public void setRestartStreamFetcherPeriod(int restartStreamFetcherPeriod) {
		this.restartStreamFetcherPeriodSeconds = restartStreamFetcherPeriod;	
	}


	public StreamFetcher startStreaming(Broadcast broadcast) {	

		StreamFetcher streamScheduler = null;
		try {
			streamScheduler =  make(broadcast, scope, schedulingService);
			streamScheduler.setRestartStream(restartStreamAutomatically);
			streamScheduler.startStream();

			streamFetcherList.add(streamScheduler);
			if (streamFetcherScheduleJobName == null) {
				scheduleStreamFetcherJob();
			}
		}
		catch (Exception e) {
			streamScheduler = null;
			logger.error(e.getMessage());
		}

		return streamScheduler;
	}

	public StreamFetcher stopStreaming(Broadcast stream) {
		logger.warn("inside of stopStreaming for {}", stream.getStreamId());

		StreamFetcher streamScheduler = null;
		for (StreamFetcher scheduler : streamFetcherList) {
			if (scheduler.getStream().getStreamId().equals(stream.getStreamId())) {
				scheduler.stopStream();
				streamFetcherList.remove(scheduler);
				streamScheduler = scheduler;
				break;
			}
		}
		
		return streamScheduler;
	}

	public void stopCheckerJob() {
		if (streamFetcherScheduleJobName != null) {
			schedulingService.removeScheduledJob(streamFetcherScheduleJobName);
			streamFetcherScheduleJobName = null;
		}
	}

	public void startStreams(List<Broadcast> streams) {

		for (int i = 0; i < streams.size(); i++) {
			startStreaming(streams.get(i));
		}

		scheduleStreamFetcherJob();
	}

	private void scheduleStreamFetcherJob() {
		if (streamFetcherScheduleJobName != null) {
			schedulingService.removeScheduledJob(streamFetcherScheduleJobName);
		}

		streamFetcherScheduleJobName = schedulingService.addScheduledJobAfterDelay(streamCheckerIntervalMs, new IScheduledJob() {

			private int lastRestartCount = 0;

			@Override
			public void execute(ISchedulingService service) throws CloneNotSupportedException {

				if (!streamFetcherList.isEmpty()) {

					streamCheckerCount++;

					logger.warn("StreamFetcher Check Count:{}" , streamCheckerCount);

					int countToRestart = 0;
					if (restartStreamFetcherPeriodSeconds > 0) 
					{
						int streamCheckIntervalSec = streamCheckerIntervalMs / 1000;
						countToRestart = (streamCheckerCount * streamCheckIntervalSec) / restartStreamFetcherPeriodSeconds;
					}


					if (countToRestart > lastRestartCount) {
						lastRestartCount = countToRestart;
						logger.info("This is {} times that restarting streams", lastRestartCount);
						restartStreamFetchers();
					} else {
						checkStreamFetchersStatus();
					}
				}
			}
			
		}, streamCheckerIntervalMs);

		logger.info("StreamFetcherSchedule job name {}", streamFetcherScheduleJobName);
	}
	
	public void checkStreamFetchersStatus() {
		for (StreamFetcher streamScheduler : streamFetcherList) {
			Broadcast stream = streamScheduler.getStream();
			if (!streamScheduler.isStreamAlive() && datastore != null && stream.getStreamId() != null) 
			{
				logger.info("Updating stream quality to poor of stream {}", stream.getStreamId() );
				datastore.updateSourceQualityParameters(stream.getStreamId(), MuxAdaptor.QUALITY_POOR, 0, 0);
			}
		}
	}
	
	public void restartStreamFetchers() {
		for (StreamFetcher streamScheduler : streamFetcherList) {

			if (streamScheduler.isStreamAlive()) 
			{
				logger.info("Calling stop stream {}", streamScheduler.getStream().getStreamId());
				streamScheduler.stopStream();
			}
			else {
				logger.info("Stream is not alive {}", streamScheduler.getStream().getStreamId());
			}

			streamScheduler.startStream();
		}
	}

	public IDataStore getDatastore() {
		return datastore;
	}

	public void setDatastore(IDataStore datastore) {
		this.datastore = datastore;
	}

	public Queue<StreamFetcher> getStreamFetcherList() {
		return streamFetcherList;
	}

	public void setStreamFetcherList(Queue<StreamFetcher> streamFetcherList) {
		this.streamFetcherList = streamFetcherList;
	}



	public boolean isRestartStreamAutomatically() {
		return restartStreamAutomatically;
	}



	public void setRestartStreamAutomatically(boolean restartStreamAutomatically) {
		this.restartStreamAutomatically = restartStreamAutomatically;
	}



	public int getStreamCheckerCount() {
		return streamCheckerCount;
	}



	public void setStreamCheckerCount(int streamCheckerCount) {
		this.streamCheckerCount = streamCheckerCount;
	}

}
