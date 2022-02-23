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

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.AppSettings;
import io.antmedia.datastore.db.DataStore;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.datastore.db.types.Broadcast.PlayListItem;
import io.antmedia.licence.ILicenceService;
import io.antmedia.muxer.IAntMediaStreamHandler;
import io.antmedia.muxer.MuxAdaptor;
import io.antmedia.rest.model.Result;
import io.antmedia.streamsource.StreamFetcher.IStreamFetcherListener;
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
		return new StreamFetcher(stream.getStreamUrl(), stream.getStreamId(), stream.getType(), scope, vertx);
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

	public static Result checkStreamUrlWithHTTP(String url){

		Result result = new Result(false);

		URL checkUrl;
		HttpURLConnection huc;
		int responseCode;

		try {
			checkUrl = new URL(url);
			huc = (HttpURLConnection) checkUrl.openConnection();
			responseCode = huc.getResponseCode();

			if(responseCode >= HttpURLConnection.HTTP_OK && responseCode < HttpURLConnection.HTTP_MOVED_PERM ) {
				result.setSuccess(true);
				return result;
			}
			else {
				result.setSuccess(false);
				result.setMessage("URL "+url+ "responded:"+responseCode);
				return result;
			}

		} catch (IOException e) {
			result.setSuccess(false);
		}
		return result;		
	}


	public void playNextItemInList(String streamId, IStreamFetcherListener listener) 
	{
		// It's necessary for skip new Stream Fetcher
		stopStreaming(streamId);

		// Get current playlist in database
		Broadcast playlist = datastore.get(streamId);


		//Check playlist is not deleted and not stopped
		//
		if(playlist != null && !IAntMediaStreamHandler.BROADCAST_STATUS_FINISHED.equals(playlist.getPlayListStatus())
				&& skipNextPlaylistQueue(playlist) != null)
		{

			// Get Current Playlist Stream Index
			int currentStreamIndex = playlist.getCurrentPlayIndex();
			// Check Stream URL is valid.
			// If stream URL is not valid, it's trying next broadcast and trying.
			if(checkStreamUrlWithHTTP(playlist.getPlayListItemList().get(currentStreamIndex).getStreamUrl()).isSuccess()) 
			{
				//update broadcast informations
				PlayListItem fetchedBroadcast = playlist.getPlayListItemList().get(currentStreamIndex);
				Result result = new Result(false);
				result.setSuccess(datastore.updateBroadcastFields(streamId, playlist));
				StreamFetcher newStreamScheduler = new StreamFetcher(fetchedBroadcast.getStreamUrl(), streamId, fetchedBroadcast.getType(), scope,vertx);
				newStreamScheduler.setStreamFetcherListener(listener);
				newStreamScheduler.setRestartStream(false);
				startStreamScheduler(newStreamScheduler);
			}
			else 
			{
				logger.info("Current Playlist Stream URL -> {} is invalid", playlist.getPlayListItemList().get(currentStreamIndex).getStreamUrl());
				playlist = skipNextPlaylistQueue(playlist);
				startPlaylist(playlist);
			}
		}

	}

	public Result startPlaylist(Broadcast playlist){


		Result result = new Result(false);
		List<PlayListItem> playListItemList = playlist.getPlayListItemList();

		if (isStreamRunning(playlist.getStreamId())) 
		{
			String msg = "Playlist is already running for stream:"+playlist.getStreamId();
			logger.warn(msg);
			result.setMessage(msg);
		}
		else if (playListItemList != null && !playListItemList.isEmpty()) 
		{

			// Get current stream in Playlist
			if (playlist.getCurrentPlayIndex() >= playlist.getPlayListItemList().size() || playlist.getCurrentPlayIndex() < 0) {
				logger.warn("Resetting current play index to 0 because it's not in correct range for id: {}", playlist.getStreamId());
				playlist.setCurrentPlayIndex(0);
			}

			PlayListItem playlistBroadcastItem = playlist.getPlayListItemList().get(playlist.getCurrentPlayIndex());

			if(checkStreamUrlWithHTTP(playlistBroadcastItem.getStreamUrl()).isSuccess()) 
			{

				// Check Stream URL is valid.
				// If stream URL is not valid, it's trying next broadcast and trying.
				// Create Stream Fetcher with Playlist Broadcast Item
				StreamFetcher streamScheduler = new StreamFetcher(playlistBroadcastItem.getStreamUrl(), playlist.getStreamId(), playlistBroadcastItem.getType(), scope, vertx);
				// Update Playlist current playing status
				playlist.setPlayListStatus(IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING);
				// Update Datastore current play broadcast
				datastore.updateBroadcastFields(playlist.getStreamId(), playlist);

				String streamId = playlist.getStreamId();

				streamScheduler.setStreamFetcherListener(listener -> {
					playNextItemInList(streamId, listener);
				});

				streamScheduler.setRestartStream(false);
				startStreamScheduler(streamScheduler);
				result.setSuccess(true);

			}
			else 
			{

				logger.warn("Current Playlist Stream URL -> {} is invalid", playlistBroadcastItem.getStreamUrl());

				// This method skip next playlist item
				playlist = skipNextPlaylistQueue(playlist);

				if(checkStreamUrlWithHTTP(playlist.getPlayListItemList().get(playlist.getCurrentPlayIndex()).getStreamUrl()).isSuccess()) {
					result = startPlaylist(playlist);
				}
				else {
					playlist.setStatus(AntMediaApplicationAdapter.BROADCAST_STATUS_FINISHED);
					// Update Datastore current play broadcast
					datastore.updateBroadcastFields(playlist.getStreamId(), playlist);
					result.setSuccess(false);
				}

			}
		}
		else {
			String msg = "There is no playlist for stream id:" + playlist.getStreamId();
			logger.warn(msg);
			result.setMessage(msg);
		}
		return result;
	}

	/**
	 * Skips the next item or set to first item in the list. If the looping is disabled, it will not set to first item and return nul
	 * @param playlist
	 * @return Broadcast object for the next item. If it's not looping, it will return null
	 */
	public Broadcast skipNextPlaylistQueue(Broadcast playlist) {

		// Get Current Playlist Stream Index
		int currentStreamIndex = playlist.getCurrentPlayIndex()+1;
		if(playlist.getPlayListItemList().size() <= currentStreamIndex) 
		{
			//update playlist first broadcast
			playlist.setCurrentPlayIndex(0);
			if (!playlist.isPlaylistLoopEnabled()) 
			{
				logger.info("Play list looping is not enabled. It will be stopped for stream: {}", playlist.getStreamId());
				//streaming is already stopped so that just update the database
				playlist.setPlayListStatus(IAntMediaStreamHandler.BROADCAST_STATUS_FINISHED);
				datastore.updateBroadcastFields(playlist.getStreamId(), playlist);

				//return null if it's not looping
				return null;
			}

		}
		else {
			// update playlist currentPlayIndex value.
			playlist.setCurrentPlayIndex(currentStreamIndex);
			logger.info("Next index to play in play list is {} for stream: {}", playlist.getCurrentPlayIndex(), playlist.getStreamId());
		}


		return playlist;
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

	public Result stopPlayList(String streamId) 
	{
		logger.info("Stopping playlist for stream: {}", streamId);

		Result result = stopStreaming(streamId);
		if (result.isSuccess()) 
		{
			result = new Result(false);
			Broadcast broadcast = datastore.get(streamId);
			if (broadcast != null && AntMediaApplicationAdapter.PLAY_LIST.equals(broadcast.getType())) 
			{
				broadcast.setPlayListStatus(IAntMediaStreamHandler.BROADCAST_STATUS_FINISHED);
				result.setSuccess(datastore.updateBroadcastFields(streamId, broadcast));
			}
			else {
				String msg = "Broadcast's type is not play list for stream:" + streamId;
				result.setMessage(msg);
				result.setDataId(streamId);
				logger.error(msg);
			}
		}
		else {
			logger.warn("Stop streamming returned false for stream:{} message:{}", streamId, result.getMessage());

		}

		return result;
	}

}
