package io.antmedia.streamsource;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.red5.server.api.scope.IScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.AppSettings;
import io.antmedia.datastore.db.DataStore;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.datastore.db.types.BroadcastUpdate;
import io.antmedia.datastore.db.types.Broadcast.PlayListItem;
import io.antmedia.licence.ILicenceService;
import io.antmedia.muxer.IAntMediaStreamHandler;
import io.antmedia.muxer.MuxAdaptor;
import io.antmedia.rest.model.Result;
import io.antmedia.settings.ServerSettings;
import io.antmedia.shutdown.AMSShutdownManager;
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

	private Map<String, StreamFetcher> streamFetcherList = new ConcurrentHashMap<>();

	/**
	 * Time period in milli seconds for checking stream fetchers status, restart issues etc. 
	 * It's the same value with MuxAdaptor.STAT_UPDATE_PERIOD_MS because it updates the database record and let us understand if stream is alive 
	 * with {@link AntMediaApplicationAdapter#isStreaming(Broadcast)}
	 *
	 */
	private int streamCheckerIntervalMs = MuxAdaptor.STAT_UPDATE_PERIOD_MS;

	private DataStore datastore;

	private IScope scope;

	private long streamFetcherScheduleJobName = -1L;

	protected AtomicBoolean isJobRunning = new AtomicBoolean(false);

	private boolean restartStreamAutomatically = true;

	private Vertx vertx;

	private int lastRestartCount;

	private AppSettings appSettings;

	private ILicenceService licenseService;
	
	boolean serverShuttingDown = false;

	private ServerSettings serverSettings;


	public StreamFetcherManager(Vertx vertx, DataStore datastore,IScope scope) {
		this.vertx = vertx;
		this.datastore = datastore;
		this.scope=scope;
		this.appSettings = (AppSettings) scope.getContext().getBean(AppSettings.BEAN_NAME);
		this.serverSettings = (ServerSettings) scope.getContext().getBean(ServerSettings.BEAN_NAME);
		this.licenseService = (ILicenceService)scope.getContext().getBean(ILicenceService.BeanName.LICENCE_SERVICE.toString());
		AMSShutdownManager.getInstance().subscribe(()-> shuttingDown());
	}

    public void shuttingDown() {
		serverShuttingDown = true;
	}

	public StreamFetcher make(Broadcast stream, IScope scope, Vertx vertx) {
		return new StreamFetcher(stream.getStreamUrl(), stream.getStreamId(), stream.getType(), scope, vertx, stream.getSeekTimeInMs());
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
	public void testSetStreamCheckerInterval(int streamCheckerInterval) {
		this.streamCheckerIntervalMs = streamCheckerInterval;
	}

	public boolean isStreamRunning(Broadcast broadcast) {

		boolean isStreamLive = false;
		
		if (streamFetcherList.containsKey(broadcast.getStreamId())) {
			logger.info("Stream is still on FetcherManagerList so it's active for streamId:{}", broadcast.getStreamId());
			isStreamLive = true;
		}

		if (!isStreamLive) 
		{
			//this stream may be fetching in somewhere in the cluster
			
			String status = broadcast.getStatus();
			boolean isStatusStreaming = AntMediaApplicationAdapter.isStreaming(status);
			boolean isInstanceRunning = false;
			if (isStatusStreaming) {
				isInstanceRunning = AntMediaApplicationAdapter.isInstanceAlive(broadcast.getOriginAdress(), serverSettings.getHostAddress(), serverSettings.getDefaultHttpPort(), scope.getName());
			}
			isStreamLive = isStatusStreaming && isInstanceRunning;
					
			logger.info("Stream is live:{}, originInstanceRunning:{} and streamId:{}", isStreamLive, isInstanceRunning, broadcast.getStreamId());
		}

		return isStreamLive;
	}

	public Result startStreamScheduler(StreamFetcher streamScheduler) {

		Result result = new Result(false);
		result.setDataId(streamScheduler.getStreamId());
		if (!licenseService.isLicenceSuspended()) {
			logger.info("Starting stream fetcher for streamId:{}", streamScheduler.getStreamId());
			streamScheduler.startStream();

			if (streamFetcherList.containsKey(streamScheduler.getStreamId())) {
				//this log has been put while we refactor streamFetcherList
				logger.warn("There is already a stream schedule exists for streamId:{} ", streamScheduler.getStreamId());
			}

			streamFetcherList.put(streamScheduler.getStreamId(), streamScheduler);

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
		return startStreaming(broadcast, false);
	}

	public Result startStreaming(@Nonnull Broadcast broadcast, boolean forceStart) {	

		//check if broadcast is already being fetching
		boolean alreadyFetching = isStreamRunning(broadcast);
		//FYI: Even if the stream is trying to prepare in any node in the cluster, alreadyFetching returns false to not have any duplication
		
		
		StreamFetcher streamScheduler = null;

		Result result = new Result(false);
		if (!alreadyFetching) {

			try {
				
				streamScheduler = make(broadcast, scope, vertx);
				streamScheduler.setRestartStream(restartStreamAutomatically);
				streamScheduler.setDataStore(getDatastore());
				streamScheduler.setStartStreamForce(forceStart);

				result = startStreamScheduler(streamScheduler);
			}
			catch (Exception e) {
				logger.error(ExceptionUtils.getStackTrace(e));
				result.setMessage("Problem occured while fetching the stream");
			}
		}
		else {
			logger.info("Stream is already active for streamId:{}", broadcast.getStreamId());
			result.setMessage("Stream is already active. It's already streaming or trying to connect");
		}

		return result;
	}

	public Result stopStreaming(String streamId, boolean stopBlocking)
	{
		logger.warn("inside of stopStreaming for {}", streamId);
		Result result = new Result(false);
		
		if (StringUtils.isNotBlank(streamId)) 
		{
			StreamFetcher scheduler = streamFetcherList.remove(streamId);
			if (scheduler != null) {
                if(stopBlocking){
                    result.setSuccess(scheduler.stopStreamBlocking());
                }
                else{
                    scheduler.stopStream();
                    result.setSuccess(true);
                }
                result.setMessage(result.isSuccess() ? "Stream stopped" : "Failed to stop the stream with Blocking :"+streamId);
                return result;
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

	public void playNextItemInList(String streamId, IStreamFetcherListener listener) {
		// Get current playlist in database, it may be updated
		Broadcast playlist = datastore.get(streamId);
		if (playlist != null) {
			playItemInList(playlist, listener, -1);
		}
	}

	public Result createAndStartNextPlaylistItem(Broadcast playlistBroadcast, IStreamFetcherListener listener,int currentStreamIndex){
		PlayListItem fetchedBroadcast = playlistBroadcast.getPlayListItemList().get(currentStreamIndex);

		BroadcastUpdate broadcastUpdate = new BroadcastUpdate();
		broadcastUpdate.setPlayListStatus(playlistBroadcast.getPlayListStatus());
		broadcastUpdate.setCurrentPlayIndex(playlistBroadcast.getCurrentPlayIndex());

		datastore.updateBroadcastFields(playlistBroadcast.getStreamId(), broadcastUpdate);

		StreamFetcher newStreamScheduler = new StreamFetcher(fetchedBroadcast.getStreamUrl(), playlistBroadcast.getStreamId(), fetchedBroadcast.getType(), scope,vertx, fetchedBroadcast.getSeekTimeInMs());
		newStreamScheduler.setRestartStream(false);
		newStreamScheduler.setStreamFetcherListener(listener);

		return startStreamScheduler(newStreamScheduler);
	}


	/**
	 * 
	 * @param playlist
	 * @param listener
	 * @param index if it's -1, it plays the next item, if it's zero or bigger, it skips that item to play
	 */
	public Result playItemInList(Broadcast playlist, IStreamFetcherListener listener, int index) 
	{
		Result result = new Result(false);

		stopStreaming(playlist.getStreamId(), true);

        if (serverShuttingDown) {
			logger.info("Playlist will not try to play the next item because server is shutting down");
			result.setMessage("Playlist will not try to play the next item because server is shutting down");
			return result;
		}

		//Check playlist is not stopped and there is an item to play

		if(!IAntMediaStreamHandler.BROADCAST_STATUS_FINISHED.equals(playlist.getPlayListStatus())
				&& skipNextPlaylistQueue(playlist, index) != null)
		{

			// Get Current Playlist Stream Index
			int currentStreamIndex = playlist.getCurrentPlayIndex();
			// Check Stream URL is valid.
			// If stream URL is not valid, it's trying next broadcast and trying.
			if(checkStreamUrlWithHTTP(playlist.getPlayListItemList().get(currentStreamIndex).getStreamUrl()).isSuccess())
			{
                return createAndStartNextPlaylistItem(playlist, listener, currentStreamIndex);
			}
			else
			{
				logger.info("Current Playlist Stream URL -> {} is invalid", playlist.getPlayListItemList().get(currentStreamIndex).getStreamUrl());
				playlist = skipNextPlaylistQueue(playlist, -1);
				result = startPlaylist(playlist);
				return result;
			}
		}
		else {
			result.setMessage("Playlist is either stopped or there is no item to play");
		}


		return result;
	}


	public Result startPlaylist(Broadcast playlist){


		Result result = new Result(false);
		List<PlayListItem> playListItemList = playlist.getPlayListItemList();

		if (isStreamRunning(playlist)) 
		{
			logger.warn("Playlist is already running for stream:{}", playlist.getStreamId());
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

				logger.info("Starting playlist item:{} for streamId:{}", playlistBroadcastItem.getStreamUrl(), playlist.getStreamId());
				// Check Stream URL is valid.
				// If stream URL is not valid, it's trying next broadcast and trying.
				// Create Stream Fetcher with Playlist Broadcast Item
				StreamFetcher streamScheduler = new StreamFetcher(playlistBroadcastItem.getStreamUrl(), playlist.getStreamId(), playlistBroadcastItem.getType(), scope, vertx, playlistBroadcastItem.getSeekTimeInMs());
				// Update Playlist current playing status
				playlist.setPlayListStatus(IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING);
				
				
				BroadcastUpdate broadcastUpdate = new BroadcastUpdate();
				broadcastUpdate.setPlayListStatus(playlist.getPlayListStatus());
				broadcastUpdate.setCurrentPlayIndex(playlist.getCurrentPlayIndex());
				
				// Update Datastore current play broadcast
				datastore.updateBroadcastFields(playlist.getStreamId(), broadcastUpdate);

				String streamId = playlist.getStreamId();

				streamScheduler.setStreamFetcherListener(new IStreamFetcherListener() {
					@Override
					public void streamFinished(IStreamFetcherListener listener) {
						playNextItemInList(streamId, listener);
					}

					@Override
					public void streamStarted(IStreamFetcherListener listener) {
						// not needed
					}
				});

				streamScheduler.setRestartStream(false);
				startStreamScheduler(streamScheduler);
				result.setSuccess(true);

			}
			else 
			{

				logger.warn("Current Playlist Stream URL -> {} is invalid", playlistBroadcastItem.getStreamUrl());

				// This method skip next playlist item
				playlist = skipNextPlaylistQueue(playlist, -1);

				if(checkStreamUrlWithHTTP(playlist.getPlayListItemList().get(playlist.getCurrentPlayIndex()).getStreamUrl()).isSuccess()) {
					result = startPlaylist(playlist);
				}
				else {
					playlist.setStatus(IAntMediaStreamHandler.BROADCAST_STATUS_FINISHED);
					// Update Datastore current play broadcast
					
					BroadcastUpdate broadcastUpdate = new BroadcastUpdate();
					broadcastUpdate.setStatus(playlist.getStatus());
					broadcastUpdate.setCurrentPlayIndex(playlist.getCurrentPlayIndex());
					
					datastore.updateBroadcastFields(playlist.getStreamId(), broadcastUpdate);
					result.setSuccess(false);
				}

			}
		}
		else {
			String msg = "There is no playlist  for stream id:" + playlist.getStreamId();
			logger.warn(msg);
			result.setMessage(msg);
		}
		return result;
	}

	/**
	 * Skips the next item or set to first item in the list. If the looping is disabled, it will not set to first item and return nul
	 * @param playlist
	 * @param index: if it's -1, plays the next item, otherwise it plays the item that is in the index
	 * @return Broadcast object for the next item. If it's not looping, it will return null
	 */
	public Broadcast skipNextPlaylistQueue(Broadcast playlist, int index) {

		// Get Current Playlist Stream Index
		int currentStreamIndex = index;
		if (index < 0) {
			currentStreamIndex = playlist.getCurrentPlayIndex()+1;
		}

		if(playlist.getPlayListItemList().size() <= currentStreamIndex) 
		{
			//update playlist first broadcast
			
			playlist.setCurrentPlayIndex(0);
			if (!playlist.isPlaylistLoopEnabled()) 
			{
				logger.info("Play list looping is not enabled. It will be stopped for stream: {}", playlist.getStreamId());
				//streaming is already stopped so that just update the database
				playlist.setPlayListStatus(IAntMediaStreamHandler.BROADCAST_STATUS_FINISHED);
				
				BroadcastUpdate broadcastUpdate = new BroadcastUpdate();
				broadcastUpdate.setPlayListStatus(playlist.getPlayListStatus());
				broadcastUpdate.setCurrentPlayIndex(playlist.getCurrentPlayIndex());
				
				datastore.updateBroadcastFields(playlist.getStreamId(), broadcastUpdate);

				//return null if it's not looping
				return null;
			}
			else {
				logger.info("Playlist has finished and playlist loop is enabled so setting index to 0 for playlist:{}", playlist.getStreamId());
			}

		}
		else {
			// update playlist currentPlayIndex value.
			playlist.setCurrentPlayIndex(currentStreamIndex);
		}

		logger.info("Next index to play in play list is {} for stream: {}", playlist.getCurrentPlayIndex(), playlist.getStreamId());

		return playlist;
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


				boolean restart = countToRestart > lastRestartCount;
				if (restart) {
					lastRestartCount = countToRestart;
					logger.info("This is {} times that restarting streams", lastRestartCount);
				}
				
				controlStreamFetchers(restart);
			}

		});

		logger.info("StreamFetcherSchedule job name {}", streamFetcherScheduleJobName);
	}
	
	public boolean isToBeStoppedAutomatically(Broadcast broadcast) 
	{
		
		boolean timeout = broadcast.getStartTime() != 0 && (System.currentTimeMillis() > (broadcast.getStartTime() + streamCheckerIntervalMs));
		
		// broadcast autoStartEnabled and there is nobody watching and it's started more than streamCheckerIntervalMs ago
		boolean  isTobeStopped = broadcast.isAutoStartStopEnabled() && !broadcast.isAnyoneWatching() && timeout;
		
		logger.info("Stream:{} isToBeStoppedAutomatically decision is {}  - details autoStartStopEnabled:{} isAnyonewatching:{} timeout:{} streamCheckerIntervalMs:{}",
					broadcast.getStreamId(), isTobeStopped, broadcast.isAutoStartStopEnabled(), broadcast.isAnyoneWatching(), timeout,  streamCheckerIntervalMs);
				
		return isTobeStopped;
				
	}

	public void controlStreamFetchers(boolean restart) {
		for (StreamFetcher streamScheduler : streamFetcherList.values()) {

			//get the updated broadcast object
			Broadcast broadcast = datastore.get(streamScheduler.getStreamId());

			if (broadcast != null && AntMediaApplicationAdapter.PLAY_LIST.equals(broadcast.getType())) {
				// For playlists, only check auto-stop if autoStartStopEnabled is true
				if (broadcast.isAutoStartStopEnabled() && isToBeStoppedAutomatically(broadcast)) {
					logger.info("Auto-stopping playlist {} because no viewers are watching", streamScheduler.getStreamId());
					stopPlayList(streamScheduler.getStreamId());
				}
				continue;
			}

			boolean autoStop = false;
			if (restart || broadcast == null ||
					(autoStop = isToBeStoppedAutomatically(broadcast)))
			{
				
				logger.info("Calling stop stream {} due to restart -> {}, broadcast is null -> {}, auto stop because no viewer -> {}", 
						streamScheduler.getStreamId(), restart, broadcast == null, autoStop);
				
				stopStreaming(streamScheduler.getStreamId(), false);
				
			}
			else {
				
				logger.info("Stream:{} is alive -> {}, is it blocked -> {}", streamScheduler.getStreamId(), streamScheduler.isStreamAlive(), streamScheduler.isStreamBlocked());
				//stream blocked means there is a connection to stream source and it's waiting to read a new packet
				//Most of the time the problem is related to the stream source side.
				
				if (!streamScheduler.isStreamBlocked() && !streamScheduler.isStreamAlive() && 
						broadcast != null && AntMediaApplicationAdapter.BROADCAST_STATUS_TERMINATED_UNEXPECTEDLY.equals(broadcast.getStatus())) {
					// if it's not blocked and it's not alive, stop the stream 
					logger.info("Stopping the stream because it is not getting updated(aka terminated_unexpectedly) and it will start for the streamId:{}", streamScheduler.getStreamId());
					stopStreaming(streamScheduler.getStreamId(), false);
					//turn restart to true because we restart the stream to reconnect
					restart = true;
				}
			}
			
			
			
			//start streaming if broadcast object is in db(it means not deleted)
			if (restart && broadcast != null) 
			{	
				//it may be still running because stop operation is async
				//So start streaming after it's finished
				if (isStreamRunning(broadcast)) 
				{
					logger.info("Setting stream fetcher listener to restart when it's finished for streamId:{}", broadcast.getStreamId());
					streamScheduler.setStreamFetcherListener(
						new IStreamFetcherListener() {
							@Override
							//Get the updated version because we don't know when it's called and we need up to date info
							public void streamFinished(IStreamFetcherListener listener) {
								Broadcast freshBroadcast = datastore.get(streamScheduler.getStreamId());
								if (freshBroadcast != null) {
									startStreaming(freshBroadcast);
								}
							}
							public void streamStarted(IStreamFetcherListener listener) {
								//not needed
							}
						}
					);
				}
				else
				{
					startStreaming(broadcast);
				}
			}

			
		}
	}

	public DataStore getDatastore() {
		return datastore;
	}

	public void setDatastore(DataStore datastore) {
		this.datastore = datastore;
	}

	public Map<String, StreamFetcher> getStreamFetcherList() {
		return streamFetcherList;
	}

	public StreamFetcher getStreamFetcher(String streamId) 
	{
		return streamFetcherList.get(streamId);
	}

	public void setStreamFetcherList(Map<String, StreamFetcher> streamFetcherList) {
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

		Result result = stopStreaming(streamId, false);
		if (result.isSuccess()) 
		{
			result = new Result(false);
			Broadcast broadcast = datastore.get(streamId);
			if (broadcast != null && AntMediaApplicationAdapter.PLAY_LIST.equals(broadcast.getType())) 
			{
				broadcast.setPlayListStatus(IAntMediaStreamHandler.BROADCAST_STATUS_FINISHED);
				BroadcastUpdate broadcastUpdate = new BroadcastUpdate();
				broadcastUpdate.setPlayListStatus(broadcast.getPlayListStatus());
				result.setSuccess(datastore.updateBroadcastFields(streamId, broadcastUpdate));
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
