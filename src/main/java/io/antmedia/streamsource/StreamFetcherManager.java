package io.antmedia.streamsource;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nonnull;

import org.apache.tika.utils.ExceptionUtils;
import org.red5.server.api.scope.IScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.antmedia.AppSettings;
import io.antmedia.datastore.db.DataStore;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.licence.ILicenceService;
import io.antmedia.muxer.MuxAdaptor;
import io.antmedia.rest.model.Result;
import io.vertx.core.Vertx;


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

	private DataStore datastore;

	private IScope scope;

	private long streamFetcherScheduleJobName = -1L;

	protected AtomicBoolean isJobRunning = new AtomicBoolean(false);

	private boolean restartStreamAutomatically = true;

	private Vertx vertx;

	private int lastRestartCount;

	private AppSettings appSettings;

	private ILicenceService licenseService;


	public StreamFetcherManager(Vertx vertx, DataStore datastore,IScope scope) {
		this.vertx = vertx;
		this.datastore = datastore;
		this.scope=scope;
		this.appSettings = (AppSettings) scope.getContext().getBean(AppSettings.BEAN_NAME);
		this.licenseService = (ILicenceService)scope.getContext().getBean(ILicenceService.BeanName.LICENCE_SERVICE.toString());
	}

	public StreamFetcher make(Broadcast stream, IScope scope, Vertx vertx) {
		return new StreamFetcher(stream.getStreamUrl(), stream.getStreamId(), stream.getType(), stream.getPlayListItemList(), scope, vertx);
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

	public boolean isStreamRunning(String streamId) {

		boolean alreadyFetching = false;

		for (StreamFetcher streamFetcher : streamFetcherList) {
			if (streamFetcher.getStreamId().equals(streamId)) {
				alreadyFetching = true;
				break;
			}
		}

		return alreadyFetching;

	}

	public Result startStreamScheduler(StreamFetcher streamScheduler) {

		Result result = new Result(false);
		result.setDataId(streamScheduler.getStreamId());
		if (!licenseService.isLicenceSuspended()) {
			streamScheduler.startStream();

			if(!streamFetcherList.contains(streamScheduler)) {
				streamFetcherList.add(streamScheduler);
			}

			if (streamFetcherScheduleJobName == -1) {
				scheduleStreamFetcherJob();
			}
			result.setSuccess(true);
		}
		else {
			logger.error("License is suspend and new stream scheduler is not started {}", streamScheduler.getStreamUrl());
			result.setMessage("License is suspended");
		}
		return result;

	}


	public Result startStreaming(@Nonnull Broadcast broadcast) {	

		//check if broadcast is already being fetching
		boolean alreadyFetching;

		alreadyFetching = isStreamRunning(broadcast.getStreamId());

		StreamFetcher streamScheduler = null;

		Result result = new Result(false);
		if (!alreadyFetching) {

			try {
				streamScheduler = make(broadcast, scope, vertx);
				streamScheduler.setRestartStream(restartStreamAutomatically);
				streamScheduler.setDataStore(getDatastore());

				result = startStreamScheduler(streamScheduler);
			}
			catch (Exception e) {
				logger.error(ExceptionUtils.getStackTrace(e));
				result.setMessage("Problem occured while fetching the stream");
			}
		}
		else {
			result.setMessage("Stream is already active. It's already streaming or trying to connect");
		}

		return result;
	}

	public Result stopStreaming(String streamId) 
	{
		logger.warn("inside of stopStreaming for {}", streamId);
		Result result = new Result(false);

		for (StreamFetcher scheduler : streamFetcherList) 
		{
			if (scheduler.getStreamId().equals(streamId)) 
			{
				scheduler.stopStream();
				streamFetcherList.remove(scheduler);
				result.setSuccess(true);
				break;
			}
		}
		result.setMessage(result.isSuccess() ? "Stream stopped" : "No matching stream source in this server:"+streamId);
		result.setDataId(streamId);
		return result;
	}

	public void stopCheckerJob() {
		if (streamFetcherScheduleJobName != -1) {
			vertx.cancelTimer(streamFetcherScheduleJobName);
			streamFetcherScheduleJobName = -1;
		}
	}

	public void startStreams(List<Broadcast> streams) {

		for (int i = 0; i < streams.size(); i++) {
			startStreaming(streams.get(i));
		}

		scheduleStreamFetcherJob();
	}

	private void scheduleStreamFetcherJob() {
		if (streamFetcherScheduleJobName != -1) {
			vertx.cancelTimer(streamFetcherScheduleJobName);
		}

		streamFetcherScheduleJobName = vertx.setPeriodic(streamCheckerIntervalMs, l-> {

			if (!streamFetcherList.isEmpty()) {

				streamCheckerCount++;

				logger.debug("StreamFetcher Check Count:{}" , streamCheckerCount);

				int countToRestart = 0;
				int restartStreamFetcherPeriodSeconds = appSettings.getRestartStreamFetcherPeriod();
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

		});

		logger.info("StreamFetcherSchedule job name {}", streamFetcherScheduleJobName);
	}

	public void checkStreamFetchersStatus() {
		for (StreamFetcher streamScheduler : streamFetcherList) {
			String streamId = streamScheduler.getStreamId();


			if (!streamScheduler.isStreamAlive() && datastore != null && streamId != null) 
			{
				MuxAdaptor muxAdaptor = streamScheduler.getMuxAdaptor();
				if (muxAdaptor != null) {
					//make speed bigger than zero in order to visible in the web panel
					muxAdaptor.changeStreamQualityParameters(streamId, null, 0.01d, 0);
				}
				else {
					logger.warn("Mux adaptor is not initialized for stream fetcher with stream id: {} It's likely that stream fetching is not started yet", streamId);
				}
			}
		}
	}

	public void restartStreamFetchers() {
		for (StreamFetcher streamScheduler : streamFetcherList) {

			if (streamScheduler.isStreamAlive()) 
			{
				logger.info("Calling stop stream {}", streamScheduler.getStreamId());
				streamScheduler.stopStream();
			}
			else {
				logger.info("Stream is not alive {}", streamScheduler.getStreamId());
			}

			streamScheduler.startStream();
		}
	}

	public DataStore getDatastore() {
		return datastore;
	}

	public void setDatastore(DataStore datastore) {
		this.datastore = datastore;
	}

	public Queue<StreamFetcher> getStreamFetcherList() {
		return streamFetcherList;
	}

	public StreamFetcher getStreamFetcher(String streamId) 
	{
		for (StreamFetcher streamFetcher : streamFetcherList) {
			if (streamFetcher.getStreamId().equals(streamId)) {
				return streamFetcher;
			}
		}
		return null;
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
