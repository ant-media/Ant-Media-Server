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
import io.antmedia.datastore.db.types.Playlist;
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


	public StreamFetcherManager(Vertx vertx, DataStore datastore,IScope scope) {
		this.vertx = vertx;
		this.datastore = datastore;
		this.scope=scope;
		this.appSettings = (AppSettings) scope.getContext().getBean(AppSettings.BEAN_NAME);
	}

	public StreamFetcher make(Broadcast stream, IScope scope, Vertx vertx) {
		return new StreamFetcher(stream, scope, vertx);
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

	public boolean checkAlreadyFetch(Broadcast broadcast) {
		
		boolean alreadyFetching = false;
		
		for (StreamFetcher streamFetcher : streamFetcherList) {
			if (streamFetcher.getStream().getStreamId().equals(broadcast.getStreamId())) {
				alreadyFetching = true;
				break;
			}
		}
		
		return alreadyFetching;
		
	}
	
	public void alreadyFetchProcess(StreamFetcher streamScheduler) {
		
		streamScheduler.startStream();

		if(!streamFetcherList.contains(streamScheduler)) {
			streamFetcherList.add(streamScheduler);
		}

		if (streamFetcherScheduleJobName == -1) {
			scheduleStreamFetcherJob();
		}
		
	}


	public StreamFetcher startStreaming(@Nonnull Broadcast broadcast) {	

		//check if broadcast is already being fetching
		boolean alreadyFetching;
		
		alreadyFetching = checkAlreadyFetch(broadcast);

		StreamFetcher streamScheduler = null;
		
		if (!alreadyFetching) {

			try {
				streamScheduler =  make(broadcast, scope, vertx);
				streamScheduler.setRestartStream(restartStreamAutomatically);
				
				alreadyFetchProcess(streamScheduler);
			}
			catch (Exception e) {
				streamScheduler = null;
				logger.error(e.getMessage());
			}
		}

		return streamScheduler;
	}


	public StreamFetcher playlistStartStreaming(@Nonnull Broadcast broadcast,StreamFetcher streamScheduler) {	

		//check if broadcast is already being fetching
		boolean alreadyFetching = false;
		
		alreadyFetching = checkAlreadyFetch(broadcast);

		if (!alreadyFetching) 
		{
			try {
				streamScheduler.setRestartStream(false);
				alreadyFetchProcess(streamScheduler);
			}
			catch (Exception e) {
				streamScheduler = null;
				logger.error(ExceptionUtils.getStackTrace(e));
			}
		}

		return streamScheduler;
	}

	public Result stopStreaming(Broadcast stream) {
		logger.warn("inside of stopStreaming for {}", stream.getStreamId());
		Result result = new Result(false);

		for (StreamFetcher scheduler : streamFetcherList) {
			if (scheduler.getStream().getStreamId().equals(stream.getStreamId())) {
				scheduler.stopStream();
				streamFetcherList.remove(scheduler);
				result.setSuccess(true);
				break;
			}
		}
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
				return result;
			}

		} catch (IOException e) {
			result.setSuccess(false);
		}
		return result;		
	}


	public void startPlaylistThread(Playlist playlist){

		// Get current stream in Playlist
		Broadcast playlistBroadcastItem = playlist.getBroadcastItemList().get(playlist.getCurrentPlayIndex());

		// Check Stream URL is valid.
		// If stream URL is not valid, it's trying next broadcast and trying.
		if(checkStreamUrlWithHTTP(playlistBroadcastItem.getStreamUrl()).isSuccess()) 
		{

			// Create Stream Fetcher with Playlist Broadcast Item
			StreamFetcher streamScheduler = new StreamFetcher(playlistBroadcastItem, scope, vertx);
			// Update Playlist current playing status
			playlist.setPlaylistStatus(AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING);
			// Update broadcast current playing status
			playlist.getBroadcastItemList().get(playlist.getCurrentPlayIndex()).setStatus(AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING); 
			// Update Datastore current play broadcast
			datastore.editPlaylist(playlist.getPlaylistId(), playlist);

			streamScheduler.setStreamFetcherListener(listener -> {

				// It's necessary for skip new Stream Fetcher
				stopStreaming(playlistBroadcastItem);
				// Get current playlist in database
				Playlist newPlaylist = datastore.getPlaylist(playlistBroadcastItem.getStreamId());
				//Check playlist is not deleted and not stopped
				if(newPlaylist.getPlaylistStatus().equals(AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING) && newPlaylist.getPlaylistId() != null)
				{
					newPlaylist = skipNextPlaylistQueue(newPlaylist);
					// Get Current Playlist Stream Index
					int currentStreamIndex = newPlaylist.getCurrentPlayIndex();
					// Check Stream URL is valid.
					// If stream URL is not valid, it's trying next broadcast and trying.
					if(checkStreamUrlWithHTTP(newPlaylist.getBroadcastItemList().get(currentStreamIndex).getStreamUrl()).isSuccess()) 
					{
						//update broadcast informations
						Broadcast fetchedBroadcast = newPlaylist.getBroadcastItemList().get(currentStreamIndex);
						Result result = new Result(false);
						result.setSuccess(datastore.updateBroadcastFields(fetchedBroadcast.getStreamId(), fetchedBroadcast));
						StreamFetcher newStreamScheduler = new StreamFetcher(fetchedBroadcast,scope,vertx);
						newStreamScheduler.setStreamFetcherListener(listener);
						playlistStartStreaming(playlistBroadcastItem,newStreamScheduler);
					}
					else 
					{
						logger.info("Current Playlist Stream URL -> {} is invalid", newPlaylist.getBroadcastItemList().get(currentStreamIndex).getStreamUrl());
						newPlaylist = skipNextPlaylistQueue(newPlaylist);
						startPlaylistThread(newPlaylist);
					}
				}
			});

			playlistStartStreaming(playlistBroadcastItem,streamScheduler);

		}
		else {

			logger.warn("Current Playlist Stream URL -> {} is invalid", playlistBroadcastItem.getStreamUrl());
			
			// This method skip next playlist item
			playlist = skipNextPlaylistQueue(playlist);

			if(checkStreamUrlWithHTTP(playlist.getBroadcastItemList().get(playlist.getCurrentPlayIndex()).getStreamUrl()).isSuccess()) {
				startPlaylistThread(playlist);
			}
			else {
				playlist.setPlaylistStatus(AntMediaApplicationAdapter.BROADCAST_STATUS_FINISHED);
				// Update Datastore current play broadcast
				datastore.editPlaylist(playlist.getPlaylistId(), playlist);
			}

		}
	}

	public Playlist skipNextPlaylistQueue(Playlist playlist) {

		// Get Current Playlist Stream Index
		int currentStreamIndex = playlist.getCurrentPlayIndex()+1;

		if(playlist.getBroadcastItemList().size() <= currentStreamIndex) 
		{
			//update playlist first broadcast
			playlist.setCurrentPlayIndex(0);
			datastore.editPlaylist(playlist.getPlaylistId(), playlist);
		}

		else {
			// update playlist currentPlayIndex value.
			playlist.setCurrentPlayIndex(currentStreamIndex);
			datastore.editPlaylist(playlist.getPlaylistId(), playlist);
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
			Broadcast stream = streamScheduler.getStream();

			if (!streamScheduler.isStreamAlive() && datastore != null && stream.getStreamId() != null) 
			{
				logger.info("Stream is not alive and setting quality to poor of stream: {} url: {}", stream.getStreamId(), stream.getStreamUrl());
				datastore.updateSourceQualityParameters(stream.getStreamId(), null, 0, 0);
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

	public DataStore getDatastore() {
		return datastore;
	}

	public void setDatastore(DataStore datastore) {
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
