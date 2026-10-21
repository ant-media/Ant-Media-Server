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
import javax.annotation.Nullable;

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
import io.antmedia.streamsource.StreamFetcher.Reason;
import io.antmedia.streamsource.StreamFetcher.State;
import io.vertx.core.Context;
import io.vertx.core.Vertx;

/**
 * Runs the {@link StreamFetcher}s of one application on this node. It keeps the registry of sources running
 * here and the broadcast row of each. A StreamFetcher runs the state machine, this class listens to its state
 * changes and turns them into database writes and registry changes.
 *
 * A source runs here only while its row has this node as {@code fetcherNode}. Every write checks that in the
 * same database call, so if another node took the source, the write is refused and the fetcher stops here
 * without writing anything. A fetcher does not retry by itself: a failed attempt is counted on the row and the
 * row is released, then the {@link StreamSourceCoordinator} starts it again, here or on another node, until
 * the retry limit stops it.
 *
 * All work on StreamFetchers runs on one shared vert.x context, the same one they run on, so it runs one task
 * at a time: a status write can't land in the middle of a state change, and a claim can't land in the middle
 * of a reconcile of the same source.
 */
public class StreamFetcherManager implements StreamFetcher.StateListener {

	private static final Logger logger = LoggerFactory.getLogger(StreamFetcherManager.class);

	private static final String ALREADY_ACTIVE_MESSAGE = "Stream is already active. It's already streaming or trying to connect";

	private static final String STOPPED_MESSAGE = "Stream stopped";

	/** errorId of a start this node turned down without touching the row, so another node may take it. */
	public static final int START_REFUSED = -7;

	/** A claim waits this long for the shared context. */
	private static final long CLAIM_TIMEOUT_MS = 10000;

	/** Non terminal StreamFetchers by stream id. An entry here means this node fetches that source. */
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

	/** This node's address, the value written to {@code fetcherNode} of a broadcast this node fetches. */
	@Getter
	private final String host;

	@Getter
	private final PlaylistController playlistController;

	@Getter
	private final StreamSourceCoordinator coordinator;

	@Setter
	@Getter
	private DataStore datastore;
	private AntMediaApplicationAdapter appInstance;

	private volatile int streamCheckerIntervalMs = MuxAdaptor.STAT_UPDATE_PERIOD_MS;

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
		this.host = serverSettings.getHostAddress();

		this.pool = Executors.newCachedThreadPool(runnable -> {
			Thread thread = new Thread(runnable, "stream-fetcher-" + scope.getName() + "-" + poolThreadCount.incrementAndGet());
			thread.setDaemon(true);
			return thread;
		});

		this.playlistController = new PlaylistController(this, vertx, context, appSettings);

		AMSShutdownManager.getInstance().subscribe(this::shuttingDown);

		this.coordinator = new StreamSourceCoordinator(this, vertx, context, pool, scope);
	}

	public void shuttingDown() {
		serverShuttingDown = true;
	}

	/** Overridden by tests to hand out StreamFetchers that run without ffmpeg. */
	public StreamFetcher make(String streamId, String streamUrl, String streamType, long seekTimeMs) {
		return new StreamFetcher(streamUrl, streamId, streamType, scope, vertx, seekTimeMs);
	}

	/**
	 * A user start: the REST start and create, and a viewer of an on-demand source. Runs on the alive node that
	 * holds the source while that node may fetch, else on its alive preferred node, otherwise on the best node, and
	 * resets its failure count.
	 */
	public Result startStreaming(@Nonnull Broadcast broadcast) {
		return coordinator.placeAndStart(broadcast, true);
	}

	/**
	 * Claims the source for this node and starts it here, on the shared context, where nothing registers or
	 * lets go of the same source in between. A refusal comes before the claim and carries
	 * {@link #START_REFUSED}, so nothing has to be undone and another node may try.
	 *
	 * @param expectedFetcherNode the fetcher node the caller read, the claim only wins while the row still says so
	 * @param userStart claims whatever the desired action is and resets the failure count. Any other start only
	 * claims while the desired action is still start, so a stop that landed after the read is never undone
	 */
	public Result claimAndStart(String streamId, @Nullable String expectedFetcherNode, boolean userStart) {
		if (Vertx.currentContext() == context) {
			return claimAndStartOnContext(streamId, expectedFetcherNode, userStart);
		}

		try {
			return CompletableFuture.supplyAsync(() -> claimAndStartOnContext(streamId, expectedFetcherNode, userStart), task -> context.runOnContext(v -> task.run()))
					.get(CLAIM_TIMEOUT_MS, TimeUnit.MILLISECONDS);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		catch (Exception e) {
			logger.error("Claim did not complete for streamId:{} {}", streamId, ExceptionUtils.getStackTrace(e));
		}
		return new Result(false, streamId, "Problem occured while fetching the stream");
	}

	private Result claimAndStartOnContext(String streamId, @Nullable String expectedFetcherNode, boolean userStart) {
		//before the gates: a full node that already runs the source says so, instead of refusing it
		if (streamFetcherList.containsKey(streamId)) {
			logger.info("Stream is already active for streamId:{}", streamId);
			return new Result(false, streamId, ALREADY_ACTIVE_MESSAGE);
		}

		String refusal = refusal(streamId);
		if (refusal == null && !coordinator.mayFetchHere()) {
			refusal = "Stream source fetching is disabled on this node";
		}
		if (refusal == null && !getApplication().getStatsCollector().enoughResource()) {
			refusal = "Not enough resource on server to start streaming.";
		}
		if (refusal != null) {
			Result refused = new Result(false, streamId, refusal);
			refused.setErrorId(START_REFUSED);
			return refused;
		}

		BroadcastUpdate claim = new BroadcastUpdate();
		claim.setFetcherDesiredAction(Broadcast.FETCHER_DESIRED_ACTION_START);
		if (userStart) {
			claim.setFetcherFailures(0);
		}

		if (!datastore.compareAndSetFetcherNode(streamId, expectedFetcherNode, host, !userStart, claim)) {
			logger.info("Not starting streamId:{}, its row changed since it was read with fetcher node:{}", streamId, expectedFetcherNode);
			return new Result(false, streamId, ALREADY_ACTIVE_MESSAGE);
		}
		logger.info("Claimed streamId:{} from fetcher node:{} user start:{}", streamId, expectedFetcherNode, userStart);

		//a holder that died never cleared its stream info, and edges would keep connecting to it
		datastore.clearStreamInfoList(streamId);

		//read after the claim, which dropped any cached copy, so an update made on another node a moment ago is in it
		Broadcast row = datastore.get(streamId);
		if (row == null) {
			logger.info("Not starting streamId:{}, it was deleted while it was claimed", streamId);
			return new Result(false, streamId, "No matching stream source:" + streamId);
		}

		Result result = startFetcher(streamId, row.getStreamUrl(), row.getType(), row.getSeekTimeInMs(), false);
		if (!result.isSuccess()) {
			releaseFailedAttempt(streamId, new BroadcastUpdate());
		}
		return result;
	}

	/**
	 * Starts one playlist item under the playlist's own stream id, so everything outside this package
	 * keeps seeing a single stream. What happens when it ends is the {@link PlaylistController}'s call.
	 * A playlist has no fetcher node, so its item's writes are not checked against one.
	 */
	Result startPlaylistItem(String playlistId, PlayListItem item) {
		String refusal = refusal(playlistId);
		return refusal != null ? new Result(false, playlistId, refusal) : startFetcher(playlistId, item.getStreamUrl(), item.getType(), item.getSeekTimeInMs(), true);
	}

	/** Why this node cannot start a fetcher right now, null when it can. */
	private String refusal(String streamId) {
		if (licenseService.isLicenceSuspended()) {
			logger.error("License is suspended, not fetching streamId:{}", streamId);
			return "License is suspended";
		}

		if (serverShuttingDown) {
			logger.info("Not fetching the stream source because it is shutting down, streamId:{}", streamId);
			return "Server is shutting down";
		}

		return null;
	}

	private Result startFetcher(String streamId, String streamUrl, String streamType, long seekTimeMs, boolean playlistItem) {
		Result result = new Result(false);
		result.setDataId(streamId);

		StreamFetcher fetcher = null;
		try {
			fetcher = make(streamId, streamUrl, streamType, seekTimeMs);
			fetcher.initialize(context, pool, this);
			fetcher.setDataStore(datastore);
			fetcher.setPlaylistItem(playlistItem);

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
	 * A user stop. The intent is written first, so nothing but a user starts the source again. Then the
	 * StreamFetcher here stops it, or the alive holder on its next tick, or it is let go here when its holder
	 * is gone. The registry entry goes on the STOPPED transition, once the database says finished.
	 */
	public Result stopStreaming(String streamId, boolean stopBlocking) {
		logger.info("Stop streaming is called for streamId:{} blocking:{}", streamId, stopBlocking);

		Result result = new Result(false);
		result.setDataId(streamId);

		if (StringUtils.isBlank(streamId)) {
			result.setMessage("Undefined stream id");
			return result;
		}

		if (playlistController.isRunning(streamId)) {
			//stopping only the item that happens to be playing would make the controller start the next one
			CompletableFuture<Void> stopped = playlistController.stopPlaylistAsync(streamId);
			result.setSuccess(!stopBlocking || awaitStopped(stopped, streamId));
			result.setMessage(result.isSuccess() ? "Playlist stopped" : "Failed to stop the playlist with Blocking :" + streamId);
			return result;
		}

		Broadcast row = datastore.get(streamId);
		if (row != null) {
			BroadcastUpdate stop = new BroadcastUpdate();
			stop.setFetcherDesiredAction(Broadcast.FETCHER_DESIRED_ACTION_STOP);
			datastore.updateBroadcastFields(streamId, stop);
		}

		//after the intent: a claim that won just before and registers after this read is stopped by its first tick
		StreamFetcher fetcher = streamFetcherList.get(streamId);
		if (fetcher == null) {
			if (row == null) {
				result.setMessage("No matching stream source:" + streamId);
				return result;
			}
			releaseUnfetched(row);
			result.setSuccess(true);
			result.setMessage(STOPPED_MESSAGE);
			return result;
		}

		if (stopBlocking) {
			result.setSuccess(fetcher.stopStreamBlocking());
			result.setMessage(result.isSuccess() ? STOPPED_MESSAGE : "Failed to stop the stream with Blocking :" + streamId);
		}
		else {
			fetcher.stopStream();
			result.setSuccess(true);
			result.setMessage(STOPPED_MESSAGE);
		}

		return result;
	}

	/** Lets go of a stopped source nothing here runs. An alive holder other than this node does it itself on its next tick. */
	private void releaseUnfetched(Broadcast row) {
		String fetcherNode = row.getFetcherNode();
		if (!host.equals(fetcherNode) && coordinator.aliveNodes().containsKey(fetcherNode)) {
			return;
		}

		BroadcastUpdate finished = new BroadcastUpdate();
		finished.setStatus(IAntMediaStreamHandler.BROADCAST_STATUS_FINISHED);
		finished.setUpdateTime(System.currentTimeMillis());
		//only if the holder is still the one read, a node that claimed it since stops it on its next tick
		datastore.compareAndSetFetcherNode(row.getStreamId(), fetcherNode, null, false, finished);
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
	 * Every state change of every StreamFetcher this manager owns, on the shared context. This is where
	 * the broadcast status of a running source is written.
	 */
	@Override
	public void onTransition(StreamFetcher fetcher, State from, State to) {
		String streamId = fetcher.getStreamId();

		if (to == State.CONNECTING) {
			if (fetcher.isPublished()) {
				//a forced reconnect ended the attempt that was on air, the source stays on this node
				getApplication().closeBroadcast(streamId, null, null, update -> write(fetcher, update));
			}

			BroadcastUpdate preparing = getApplication().getFreshBroadcastUpdateForStatus(IAntMediaStreamHandler.PUBLISH_TYPE_PULL, IAntMediaStreamHandler.BROADCAST_STATUS_PREPARING);
			if (!fetcher.isPlaylistItem()) {
				//null leaves it untouched, startPublish writes it once on air
				preparing.setOriginAdress(null);
			}
			write(fetcher, preparing);
		}
		else if (to == State.STREAMING) {
			BroadcastUpdate onAir = new BroadcastUpdate();
			onAir.setFetcherFailures(0);
			//startPublish writes this host as origin unfenced, so only after a fenced write proved the row is ours
			if (write(fetcher, onAir)) {
				getApplication().startPublish(streamId, 0, IAntMediaStreamHandler.PUBLISH_TYPE_PULL, null, null);
			}
		}
		else if (to == State.STOPPED) {
			//unregister before the row is let go, whoever polls for finished must be able to start again
			streamFetcherList.remove(streamId, fetcher);
			writeStopped(fetcher);
			//last, so a playlist starts its next item only once this one is completely gone
			playlistController.onItemStopped(fetcher);
		}
	}

	/**
	 * Lets go of the row and ends the broadcast, unless another node took the source over, then nothing is
	 * written or announced. A stop leaves the desired action alone: a user stop wrote stop already, and a
	 * shutdown leaves it start, so another node takes it.
	 */
	private void writeStopped(StreamFetcher fetcher) {
		String streamId = fetcher.getStreamId();
		Reason reason = fetcher.getLastReason();

		if (fetcher.isPlaylistItem() || reason == Reason.STOP_REQUESTED || reason == Reason.DELETED) {
			getApplication().closeBroadcast(streamId, null, null, update -> fencedWrite(fetcher, null, update));
		}
		else if (fetcher.isPublished()) {
			getApplication().closeBroadcast(streamId, null, null, update -> releaseFailedAttempt(streamId, update));
		}
		else {
			//it never got on air, so there is no broadcast to end
			releaseFailedAttempt(streamId, new BroadcastUpdate());
		}
	}

	/**
	 * Counts a failed attempt on the row and lets the source go, so the leader tries it again, maybe on
	 * another node. The count spans every node, at streamFetcherMaxRetryAttempts the source is stopped.
	 *
	 * @param update the rest of the write, the end of the broadcast when the attempt was on air
	 * @return false when the row is gone or this node does not hold it anymore
	 */
	private boolean releaseFailedAttempt(String streamId, BroadcastUpdate update) {
		Broadcast row = datastore.get(streamId);
		if (row == null) {
			return false;
		}

		int failures = row.getFetcherFailures() + 1;
		int maxAttempts = appSettings.getStreamFetcherMaxRetryAttempts();
		if (maxAttempts >= 0 && failures >= maxAttempts) {
			logger.error("Giving up on streamId:{} after {} failed attempts in a row", streamId, failures);
			update.setFetcherDesiredAction(Broadcast.FETCHER_DESIRED_ACTION_STOP);
			update.setStatus(IAntMediaStreamHandler.BROADCAST_STATUS_FINISHED);
		}
		else {
			logger.warn("Fetching streamId:{} failed {} times in a row, letting it go so it is tried again", streamId, failures);
			//still being tried. If nobody takes it, it decays to terminated_unexpectedly like any row
			update.setStatus(IAntMediaStreamHandler.BROADCAST_STATUS_PREPARING);
		}
		update.setFetcherFailures(failures);
		update.setUpdateTime(System.currentTimeMillis());

		return datastore.compareAndSetFetcherNode(streamId, host, null, false, update);
	}

	/** Every write a running source causes. Refused means another node holds it now, so it stops here. */
	private boolean write(StreamFetcher fetcher, BroadcastUpdate update) {
		if (fencedWrite(fetcher, host, update)) {
			return true;
		}

		logger.info("Stopping streamId:{} on this node:{}, its row does not name this node anymore", fetcher.getStreamId(), host);
		fetcher.stopStream();
		return false;
	}

	/** Writes only while the row names this node, and sets its fetcher node to newFetcherNode. A playlist item is not fenced. */
	private boolean fencedWrite(StreamFetcher fetcher, @Nullable String newFetcherNode, BroadcastUpdate update) {
		if (fetcher.isPlaylistItem()) {
			datastore.updateBroadcastFields(fetcher.getStreamId(), update);
			return true;
		}
		return datastore.compareAndSetFetcherNode(fetcher.getStreamId(), host, newFetcherNode, false, update);
	}

	/**
	 * Periodic maintenance of one source, driven by that StreamFetcher's own tick: stop it when its broadcast
	 * is gone, another node holds it, it is not wanted anymore or nobody is watching it, reconnect it when it
	 * has been up longer than the forced restart period, and keep the status of one that is still connecting
	 * from decaying.
	 */
	@Override
	public void onTick(StreamFetcher fetcher) {
		String streamId = fetcher.getStreamId();
		State state = fetcher.getState();
		Broadcast broadcast = datastore.get(streamId);
		int restartPeriodSeconds = appSettings.getRestartStreamFetcherPeriod();
		boolean isPlaylist = fetcher.isPlaylistItem();

		if (broadcast == null) {
			logger.info("Stopping the stream fetcher because its broadcast is deleted, streamId:{}", streamId);
			fetcher.stopStream();
		}
		//a streaming source writes nothing on a tick, so this read is how it learns another node took it over
		else if (!isPlaylist && !host.equals(broadcast.getFetcherNode())) {
			logger.info("Stopping streamId:{} on this node:{}, its row names fetcher node:{}", streamId, host, broadcast.getFetcherNode());
			fetcher.stopStream();
		}
		else if (!isPlaylist && Broadcast.FETCHER_DESIRED_ACTION_STOP.equals(broadcast.getFetcherDesiredAction())) {
			logger.info("Stopping streamId:{} because it is not wanted anymore", streamId);
			fetcher.stopStream();
		}
		else if (isToBeStoppedAutomatically(broadcast)) {
			logger.info("Auto stopping the {} because nobody is watching it, streamId:{}", isPlaylist ? "playlist" : "stream source", streamId);
			if (isPlaylist) {
				playlistController.stopPlaylist(streamId);
			}
			else {
				//not wanted until a viewer starts it again, so nothing else starts it meanwhile
				BroadcastUpdate stop = new BroadcastUpdate();
				stop.setFetcherDesiredAction(Broadcast.FETCHER_DESIRED_ACTION_STOP);
				if (write(fetcher, stop)) {
					fetcher.stopStream();
				}
			}
		}
		//a forced reconnect would restart the current item, not the playlist, so playlists sit it out
		else if (!isPlaylist && restartPeriodSeconds > 0 && state == State.STREAMING
				&& System.currentTimeMillis() - fetcher.getStateSinceMs() >= restartPeriodSeconds * 1000L) {
			logger.info("Reconnecting streamId:{} because it has been streaming for longer than the forced restart period of {}s", streamId, restartPeriodSeconds);
			fetcher.restart();
		}
		else if (state == State.CONNECTING) {
			//keep a source we are still trying to reach from decaying to terminated_unexpectedly. Only these two
			//fields, so the start time the auto stop timeout and the retry delay measure from is not pushed forward
			BroadcastUpdate update = new BroadcastUpdate();
			update.setStatus(IAntMediaStreamHandler.BROADCAST_STATUS_PREPARING);
			update.setUpdateTime(System.currentTimeMillis());
			write(fetcher, update);
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
	 * True when something holds this stream id: a StreamFetcher or a playlist session of this node, or the
	 * alive node the row names as its fetcher node. A playlist has no fetcher node, it runs where its row is
	 * live, if that origin answers. Playlist items are started through startPlaylistItem, which does not come
	 * through here, so a session never blocks a switch.
	 */
	public boolean isStreamRunning(Broadcast broadcast) {
		String streamId = broadcast.getStreamId();
		if (streamFetcherList.containsKey(streamId) || playlistController.isRunning(streamId)) {
			return true;
		}

		if (!AntMediaApplicationAdapter.PLAY_LIST.equals(broadcast.getType())) {
			return coordinator.aliveNodes().containsKey(broadcast.getFetcherNode());
		}

		if (!AntMediaApplicationAdapter.isStreaming(broadcast.getStatus())) {
			return false;
		}

		boolean originAlive = AntMediaApplicationAdapter.isInstanceAlive(broadcast.getOriginAdress(),
				host, serverSettings.getDefaultHttpPort(), scope.getName());

		logger.info("Playlist is live on origin:{}, that instance is running:{} and streamId:{}",
				broadcast.getOriginAdress(), originAlive, streamId);

		return originAlive;
	}

	/**
	 * Stops every source and waits for the database to say finished before the application goes down. Every
	 * source is let go and stays wanted, so the other nodes take it over.
	 */
	public void shutdown() {
		logger.info("Stopping all stream fetchers for app:{}", scope.getName());

		//refuse late starts, the pool is gone at the end of this method
		serverShuttingDown = true;
		coordinator.shutdown();

		//a playlist has to be marked finished as a whole, not just have its current item stopped. Its task
		//is queued on the shared context ahead of the stops below, so no item gets to advance the playlist
		List<CompletableFuture<Void>> stopping = new ArrayList<>();
		stopping.add(playlistController.shutdown());
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

	AntMediaApplicationAdapter getApplication() {
		if (appInstance == null) {
			appInstance = (AntMediaApplicationAdapter) scope.getContext().getBean(AntMediaApplicationAdapter.BEAN_NAME);
		}
		return appInstance;
	}
}
