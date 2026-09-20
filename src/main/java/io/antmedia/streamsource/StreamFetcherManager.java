package io.antmedia.streamsource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nonnull;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.red5.server.api.scope.IScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.AppSettings;
import io.antmedia.datastore.db.DataStore;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.datastore.db.types.Broadcast.PlayListItem;
import io.antmedia.datastore.db.types.BroadcastUpdate;
import io.antmedia.licence.ILicenceService;
import io.antmedia.muxer.IAntMediaStreamHandler;
import io.antmedia.muxer.MuxAdaptor;
import io.antmedia.rest.model.Result;
import io.antmedia.settings.ServerSettings;
import io.antmedia.shutdown.AMSShutdownManager;
import io.antmedia.streamsource.StreamFetcher.State;
import io.vertx.core.Context;
import io.vertx.core.Vertx;

/**
 * Owns every {@link StreamFetcher} of one application: which sources are running, who is allowed to
 * start one, and the broadcast status behind each of them. The StreamFetchers run the state machine,
 * this class turns their transitions into database writes and registry changes.
 *
 * Everything here that touches a StreamFetcher runs on one shared vert.x context, the same one the
 * StreamFetchers use, so a status write can never cross with a transition.
 */
public class StreamFetcherManager implements StreamFetcher.StateListener {

	private static final Logger logger = LoggerFactory.getLogger(StreamFetcherManager.class);

	private static final String ALREADY_ACTIVE_MESSAGE = "Stream is already active. It's already streaming or trying to connect";

	/** Non terminal StreamFetchers by stream id. An entry here means this node owns that source. */
	@Setter
	@Getter
	private Map<String, StreamFetcher> streamFetcherList = new ConcurrentHashMap<>();

	private final Vertx vertx;
	private final IScope scope;
	private final Context context;
	private final ExecutorService pool;
	private final AtomicInteger poolThreadCount = new AtomicInteger();

	private final AppSettings appSettings;
	private final ServerSettings serverSettings;
	private final ILicenceService licenseService;

	@Getter
	private final PlaylistController playlistController;

	@Setter
	@Getter
	private DataStore datastore;
	private AntMediaApplicationAdapter appInstance;

	private volatile int streamCheckerIntervalMs = MuxAdaptor.STAT_UPDATE_PERIOD_MS;

	/** False makes new sources give up after their first failed attempt instead of reconnecting. */
	@Setter
	@Getter
	private boolean restartStreamAutomatically = true;

	@Getter
	private volatile boolean serverShuttingDown;

	public StreamFetcherManager(Vertx vertx, DataStore datastore, IScope scope) {
		this.vertx = vertx;
		this.datastore = datastore;
		this.scope = scope;
		this.context = vertx.getOrCreateContext();
		this.appSettings = (AppSettings) scope.getContext().getBean(AppSettings.BEAN_NAME);
		this.serverSettings = (ServerSettings) scope.getContext().getBean(ServerSettings.BEAN_NAME);
		this.licenseService = (ILicenceService) scope.getContext().getBean(ILicenceService.BEAN_NAME);

		this.pool = Executors.newCachedThreadPool(runnable -> {
			Thread thread = new Thread(runnable, "stream-fetcher-" + scope.getName() + "-" + poolThreadCount.incrementAndGet());
			thread.setDaemon(true);
			return thread;
		});

		this.playlistController = new PlaylistController(this, vertx, context, appSettings);

		AMSShutdownManager.getInstance().subscribe(this::shuttingDown);
	}

	public void shuttingDown() {
		serverShuttingDown = true;
	}

	/** Overridden by tests to hand out StreamFetchers that run without ffmpeg. */
	public StreamFetcher make(String streamId, String streamUrl, String streamType, long seekTimeMs) {
		return new StreamFetcher(streamUrl, streamId, streamType, scope, vertx, seekTimeMs);
	}

	public Result startStreaming(@Nonnull Broadcast broadcast) {
		return startStreaming(broadcast, false);
	}

	/**
	 * Admits a source and starts fetching it. An entry in the registry always wins, so a source that
	 * is streaming, retrying or still stopping is refused. With forceStart the database half of the
	 * check is skipped, which is what boot time resume needs because the rows it reads already say
	 * preparing.
	 */
	public Result startStreaming(@Nonnull Broadcast broadcast, boolean forceStart) {
		if (!forceStart && isStreamRunning(broadcast)) {
			logger.info("Stream is already active for streamId:{}", broadcast.getStreamId());
			return new Result(false, broadcast.getStreamId(), ALREADY_ACTIVE_MESSAGE);
		}

		return startFetcher(broadcast.getStreamId(), broadcast.getStreamUrl(), broadcast.getType(),
				broadcast.getSeekTimeInMs(), restartStreamAutomatically);
	}

	/**
	 * Starts one playlist item under the playlist's own stream id, so everything outside this package
	 * keeps seeing a single stream. What happens when it ends is the {@link PlaylistController}'s call,
	 * which is why the item itself never retries.
	 */
	Result startPlaylistItem(String playlistId, PlayListItem item) {
		return startFetcher(playlistId, item.getStreamUrl(), item.getType(), item.getSeekTimeInMs(), false);
	}

	private Result startFetcher(String streamId, String streamUrl, String streamType, long seekTimeMs, boolean restartOnFailure) {
		Result result = new Result(false);
		result.setDataId(streamId);

		if (licenseService.isLicenceSuspended()) {
			logger.error("License is suspended, not fetching the stream source:{}", streamUrl);
			result.setMessage("License is suspended");
			return result;
		}

		if (serverShuttingDown) {
			logger.info("Not fetching the stream source because it is shutting down, streamId:{}", streamId);
			result.setMessage("Server is shutting down");
			return result;
		}

		StreamFetcher fetcher = null;
		try {
			fetcher = make(streamId, streamUrl, streamType, seekTimeMs);
			fetcher.initialize(context, pool, this);
			fetcher.setDataStore(datastore);
			fetcher.setRestartStream(restartOnFailure);

			if (streamFetcherList.putIfAbsent(streamId, fetcher) != null) {
				logger.info("Another stream fetcher was registered first for streamId:{}", streamId);
				result.setMessage(ALREADY_ACTIVE_MESSAGE);
				return result;
			}

			fetcher.startStream();

			result.setSuccess(true);
			result.setMessage("Stream fetcher is started");
		}
		catch (Exception e) {
			//a registration that never got started is exactly the stuck source this rewrite is about,
			//nothing would ever stop it and every later start would be refused. No-op when not registered
			streamFetcherList.remove(streamId, fetcher);
			logger.error(ExceptionUtils.getStackTrace(e));
			result.setMessage("Problem occured while fetching the stream");
		}

		return result;
	}

	/**
	 * Asks the StreamFetcher to stop. The registry entry is removed by its STOPPED transition, not
	 * here, so the entry disappears only once the database says finished.
	 */
	public Result stopStreaming(String streamId, boolean stopBlocking) {
		logger.info("Stop streaming is called for streamId:{} blocking:{}", streamId, stopBlocking);

		Result result = new Result(false);
		result.setDataId(streamId);

		if (StringUtils.isBlank(streamId)) {
			result.setMessage("Stream id is not defined");
			return result;
		}

		if (playlistController.isRunning(streamId)) {
			//stopping only the item that happens to be playing would make the controller start the next one
			CompletableFuture<Void> stopped = playlistController.stopPlaylistAsync(streamId);
			result.setSuccess(!stopBlocking || awaitStopped(stopped, streamId));
			result.setMessage(result.isSuccess() ? "Playlist stopped" : "Failed to stop the playlist with Blocking :" + streamId);
			return result;
		}

		StreamFetcher fetcher = streamFetcherList.get(streamId);
		if (fetcher == null) {
			result.setMessage("No matching stream source in this server:" + streamId);
			return result;
		}

		if (stopBlocking) {
			result.setSuccess(fetcher.stopStreamBlocking());
			result.setMessage(result.isSuccess() ? "Stream stopped" : "Failed to stop the stream with Blocking :" + streamId);
		}
		else {
			fetcher.stopStream();
			result.setSuccess(true);
			result.setMessage("Stream stopped");
		}

		return result;
	}

	/**
	 * Waits for work that was posted to the shared context. Must not be called on that context, the
	 * transitions it waits for need that thread.
	 */
	private boolean awaitStopped(CompletableFuture<Void> stopped, String logId) {
		if (Context.isOnVertxThread()) {
			logger.error("Blocking stop is called on a vert.x thread for:{}. It cannot complete there", logId);
			return false;
		}

		//past the abandonment window, so this outlives the attempt it is waiting for
		long waitMs = StreamFetcher.STOPPING_TIMEOUT_MS + 2000;

		try {
			stopped.get(waitMs, TimeUnit.MILLISECONDS);
			return true;
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		catch (Exception e) {
			logger.warn("Did not stop in {}ms for:{}", waitMs, logId);
		}

		return false;
	}

	/**
	 * Every state change of every StreamFetcher this manager owns, on the shared context. This is the
	 * only place the broadcast status of a pulled source is written.
	 */
	@Override
	public void onTransition(StreamFetcher fetcher, State from, State to) {
		String streamId = fetcher.getStreamId();

		if (to == State.STOPPED) {
			//unregister before finished is written, whoever polls for finished must be able to start again
			streamFetcherList.remove(streamId, fetcher);
		}

		//end the broadcast once per attempt that published, and once on any stop whether it published or not
		boolean publishedAttemptEnded = fetcher.isPublished() && (to == State.CONNECTING || to == State.RECONNECT_WAIT);
		if (to == State.STOPPED || publishedAttemptEnded) {
			getApplication().closeBroadcast(streamId, null, null);
		}

		if (to == State.CONNECTING || to == State.RECONNECT_WAIT) {
			//this write is also the cluster claim: it stamps this host as the origin of the source
			datastore.updateBroadcastFields(streamId,
					getApplication().getFreshBroadcastUpdateForStatus(IAntMediaStreamHandler.PUBLISH_TYPE_PULL, IAntMediaStreamHandler.BROADCAST_STATUS_PREPARING));
		}
		else if (to == State.STREAMING) {
			getApplication().startPublish(streamId, 0, IAntMediaStreamHandler.PUBLISH_TYPE_PULL, null, null);
		}

		if (to == State.STOPPED) {
			//last, so a playlist starts its next item only once this one is completely gone
			playlistController.onItemStopped(fetcher);
		}
	}

	/**
	 * Periodic maintenance of one source, driven by that StreamFetcher's own tick: evict it when its
	 * broadcast is gone, stop it when nobody is watching, reconnect it when it has been up longer than
	 * the forced restart period, and keep the status of one that is still connecting from decaying.
	 */
	@Override
	public void onTick(StreamFetcher fetcher) {
		String streamId = fetcher.getStreamId();
		State state = fetcher.getState();
		Broadcast broadcast = datastore.get(streamId);
		int restartPeriodSeconds = appSettings.getRestartStreamFetcherPeriod();

		if (broadcast == null) {
			logger.info("Stopping the stream fetcher because its broadcast is deleted, streamId:{}", streamId);
			fetcher.stopStream();
			return;
		}

		//a playlist item is registered under the playlist's own id, so this broadcast is the playlist
		boolean isPlaylist = AntMediaApplicationAdapter.PLAY_LIST.equals(broadcast.getType());

		if (isToBeStoppedAutomatically(broadcast)) {
			logger.info("Auto stopping the {} because nobody is watching it, streamId:{}", isPlaylist ? "playlist" : "stream source", streamId);
			if (isPlaylist) {
				playlistController.stopPlaylist(streamId);
			}
			else {
				fetcher.stopStream();
			}
		}
		//a forced reconnect would restart the current item, not the playlist, so playlists sit it out
		else if (!isPlaylist && restartPeriodSeconds > 0 && state == State.STREAMING
				&& System.currentTimeMillis() - fetcher.getStateSinceMs() >= restartPeriodSeconds * 1000L) {
			logger.info("Reconnecting streamId:{} because it has been streaming for longer than the forced restart period of {}s", streamId, restartPeriodSeconds);
			fetcher.restart();
		}
		else if (state == State.CONNECTING || state == State.RECONNECT_WAIT) {
			//keep a source we are still trying to reach from decaying to terminated_unexpectedly.
			//only these two fields, so the start time the auto stop timeout measures from is not pushed forward
			BroadcastUpdate update = new BroadcastUpdate();
			update.setStatus(IAntMediaStreamHandler.BROADCAST_STATUS_PREPARING);
			update.setUpdateTime(System.currentTimeMillis());
			datastore.updateBroadcastFields(streamId, update);
		}
	}

	public boolean isToBeStoppedAutomatically(Broadcast broadcast) {
		boolean timeout = broadcast.getStartTime() != 0 && (System.currentTimeMillis() > (broadcast.getStartTime() + streamCheckerIntervalMs));
		boolean isToBeStopped = broadcast.isAutoStartStopEnabled() && !broadcast.isAnyoneWatching() && timeout;

		//this runs for every source on every checker tick, so only the decision to stop is worth an info line
		if (isToBeStopped) {
			logger.info("Stream:{} is to be stopped automatically, autoStartStopEnabled:{} isAnyoneWatching:{} timeout:{} streamCheckerIntervalMs:{}",
					broadcast.getStreamId(), broadcast.isAutoStartStopEnabled(), broadcast.isAnyoneWatching(), timeout, streamCheckerIntervalMs);
		}
		else {
			logger.debug("Stream:{} is not to be stopped automatically, autoStartStopEnabled:{} isAnyoneWatching:{} timeout:{} streamCheckerIntervalMs:{}",
					broadcast.getStreamId(), broadcast.isAutoStartStopEnabled(), broadcast.isAnyoneWatching(), timeout, streamCheckerIntervalMs);
		}

		return isToBeStopped;
	}

	/**
	 * True when something holds this stream id: a StreamFetcher here, a playlist of this node sitting
	 * between two items, or another node in the cluster that is still alive. Playlist items are started
	 * through startPlaylistItem, which does not come through here, so a session never blocks a switch.
	 */
	public boolean isStreamRunning(Broadcast broadcast) {
		if (streamFetcherList.containsKey(broadcast.getStreamId()) || playlistController.isRunning(broadcast.getStreamId())) {
			return true;
		}

		if (!AntMediaApplicationAdapter.isStreaming(broadcast.getStatus())) {
			return false;
		}

		boolean originAlive = AntMediaApplicationAdapter.isInstanceAlive(broadcast.getOriginAdress(),
				serverSettings.getHostAddress(), serverSettings.getDefaultHttpPort(), scope.getName());

		logger.info("Stream is owned by origin:{}, that instance is running:{} and streamId:{}",
				broadcast.getOriginAdress(), originAlive, broadcast.getStreamId());

		return originAlive;
	}

	/** Boot time resume of the sources that are not started on demand by a viewer. */
	public void resumeUnattendedSources() {
		if (!appSettings.isStartStreamFetcherAutomatically()) {
			return;
		}

		List<Broadcast> streams = datastore.getExternalStreamsList();
		logger.info("Resuming stream sources for app:{}, stream source size: {}", scope.getName(), streams.size());

		for (Broadcast broadcast : streams) {
			if (!broadcast.isAutoStartStopEnabled()) {
				startStreaming(broadcast, true);
			}
		}
	}

	/** Stops every source and waits for the database to say finished before the application goes down. */
	public void shutdown() {
		logger.info("Stopping all stream fetchers for app:{}", scope.getName());

		//refuse late starts, the pool is gone at the end of this method and a StreamFetcher would retry forever
		serverShuttingDown = true;

		//a playlist has to be marked finished as a whole, not just have its current item stopped
		awaitStopped(playlistController.shutdown(), scope.getName());

		List<CompletableFuture<Void>> stopping = new ArrayList<>();
		for (StreamFetcher fetcher : streamFetcherList.values()) {
			stopping.add(fetcher.stopStream());
		}

		awaitStopped(CompletableFuture.allOf(stopping.toArray(new CompletableFuture[0])), scope.getName());

		pool.shutdownNow();
	}

	public StreamFetcher getStreamFetcher(String streamId) {
		return streamFetcherList.get(streamId);
	}

	/**
	 * Grace window of the auto stop check: how long a source with nobody watching is left alone after
	 * it started.
	 * @param streamCheckerInterval in milliseconds
	 */
	public void testSetStreamCheckerInterval(int streamCheckerInterval) {
		this.streamCheckerIntervalMs = streamCheckerInterval;
	}

	private AntMediaApplicationAdapter getApplication() {
		if (appInstance == null) {
			appInstance = (AntMediaApplicationAdapter) scope.getContext().getBean(AntMediaApplicationAdapter.BEAN_NAME);
		}
		return appInstance;
	}
}
