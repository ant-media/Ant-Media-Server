package io.antmedia.streamsource;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

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
import io.antmedia.muxer.MuxAdaptor;
import io.antmedia.rest.model.Result;


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

	public  static class StreamFetcherFactory {
		public StreamFetcher make(Broadcast stream, IScope scope, ISchedulingService schedulingService) {
			return new StreamFetcher(stream, scope, schedulingService);
		}
	}

	private boolean restartStreamAutomatically = true;

	private StreamFetcherFactory streamFetcherFactory;

	/**
	 * Time period in seconds for restarting stream fetchers
	 */
	private int restartStreamFetcherPeriodSeconds;

	public StreamFetcherManager(ISchedulingService schedulingService, IDataStore datastore,IScope scope) {
		this(schedulingService, datastore, scope, null);
	}



	public StreamFetcherManager(ISchedulingService schedulingService, IDataStore datastore,IScope scope, StreamFetcherFactory streamFetcherFactory) {
		this.schedulingService = schedulingService;
		this.datastore = datastore;
		this.scope=scope;
		this.streamFetcherFactory = streamFetcherFactory;
		if(this.streamFetcherFactory == null) {
			this.streamFetcherFactory = new StreamFetcherFactory();
		}

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


	public Result startStreaming(Broadcast broadcast) {	

		Result result=new Result(false);

		try {
			StreamFetcher streamScheduler = streamFetcherFactory.make(broadcast, scope, schedulingService);
			streamScheduler.setRestartStream(restartStreamAutomatically);
			streamScheduler.startStream();

			String broadcastType = broadcast.getType();
			if(broadcastType != null && broadcastType.equals(AntMediaApplicationAdapter.IP_CAMERA)) {
				try {
					Thread.sleep(6000);
				} catch (InterruptedException e) {
					logger.error(e.getMessage());
				}
			}
			if(!streamScheduler.getCameraError().isSuccess()) {
				result=streamScheduler.getCameraError();
			}
			else {
				result.setSuccess(true);
			}
			streamFetcherList.add(streamScheduler);
			if (streamFetcherScheduleJobName == null) {
				scheduleStreamFetcherJob();
			}
		}
		catch (Exception e) {
			logger.error(e.getMessage());
		}

		return result;
	}

	public void stopStreaming(Broadcast stream) {
		logger.warn("inside of stopStreaming for {}", stream.getStreamId());

		for (StreamFetcher streamScheduler : streamFetcherList) {
			if (streamScheduler.getStream().getStreamId().equals(stream.getStreamId())) {
				streamScheduler.stopStream();
				streamFetcherList.remove(streamScheduler);
				break;
			}
		}
	}

	public void stopCheckerJob() {
		if (streamFetcherScheduleJobName != null) {
			schedulingService.removeScheduledJob(streamFetcherScheduleJobName);
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

					} else {
						for (StreamFetcher streamScheduler : streamFetcherList) {
							Broadcast stream = streamScheduler.getStream();
							if (!streamScheduler.isStreamAlive() && datastore != null && stream.getStreamId() != null) 
							{
								logger.info("Updating stream quality to poor of stream {}", stream.getStreamId() );
								datastore.updateSourceQualityParameters(stream.getStreamId(), MuxAdaptor.QUALITY_POOR, 0, 0);
							}
						}
					}
				}
			}
		}, streamCheckerIntervalMs);

		logger.info("StreamFetcherSchedule job name {}", streamFetcherScheduleJobName);
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
