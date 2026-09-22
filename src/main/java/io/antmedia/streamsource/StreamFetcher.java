package io.antmedia.streamsource;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.red5.server.api.scope.IScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.AppSettings;
import io.antmedia.datastore.db.DataStore;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.muxer.MuxAdaptor;
import io.antmedia.rest.model.Result;
import io.vertx.core.Context;
import io.vertx.core.Vertx;

/**
 * Pulls one source into the server. This class is the state machine only: it owns the state, the
 * attempt counter and the timers, and it never touches the database. Opening the source and pulling
 * packets is a {@link StreamFetcherWorker} running on a pool thread, every status write belongs to the
 * {@link StreamFetcherManager} that listens to the transitions.
 *
 * Every transition runs on one vert.x context, so no field below needs a lock. Public entry points
 * post their work to that context and return.
 */
public class StreamFetcher {

	private static final Logger logger = LoggerFactory.getLogger(StreamFetcher.class);

	/** No open and no packet within this window aborts the running attempt. */
	private static final long ACTIVITY_TIMEOUT_MS = AntMediaApplicationAdapter.STREAM_TIMEOUT_MS;

	/** An attempt that does not return within this window after an abort is given up on. */
	static final long STOPPING_TIMEOUT_MS = 10000;

	/** How often the two timeouts above are checked.   */
	private static final long TICK_PERIOD_MS = MuxAdaptor.STAT_UPDATE_PERIOD_MS;

	public enum State {
		IDLE, CONNECTING, STREAMING, RECONNECT_WAIT, STOPPING, STOPPED
	}

	/**
	 * A reason why state was changed or requested. 
	 * Usually why an attempt ended, or why this StreamFetcher stopped.
	 *  Never a state, always data. */
	public enum Reason {
		OPEN_FAILED, READ_ERROR, EOF, TIMEOUT, DELETED, STOP_REQUESTED, RECONNECT, RETRIES_EXHAUSTED, NO_RETRY
	}

	/**
	 * Callback when state has changed, and once per tick while this StreamFetcher is alive.
	 * The manager is the only implementation. This is how the registry and the broadcast status follow this StreamFetcher.
	 * Everything that needs the database or a policy decision lives behind these two calls, never in here.
	 */
	public interface StateListener {

		void onTransition(StreamFetcher fetcher, State from, State to);

		/** Periodic maintenance of one source: auto stop, forced restart, keeping its status fresh. */
		void onTick(StreamFetcher fetcher);
	}

	/**
	 * The old start/stop callback, kept because the adapter and RTMPClusterStreamFetcher implement it.
	 * Started fires on the first packet, finished fires once this StreamFetcher is done for good.
	 */
	public interface IStreamFetcherListener {

		void streamFinished(IStreamFetcherListener listener);

		void streamStarted(IStreamFetcherListener listener);
	}

	@Getter
	private final String streamId;
	@Getter
	private final String streamUrl;
	private final String streamType;
	@Getter
	private final IScope scope;
	private final Vertx vertx;
	private final AppSettings appSettings;

	private final CompletableFuture<Void> stopFuture = new CompletableFuture<>();

	private Context context;
	private ExecutorService pool;
	private StateListener listener;
	@Setter
	@Getter
	private IStreamFetcherListener streamFetcherListener;

	//lazily resolved from both the context and a worker thread
	private final AtomicReference<DataStore> dataStore = new AtomicReference<>();
	private final AtomicReference<AntMediaApplicationAdapter> appInstance = new AtomicReference<>();

	//only written on the shared context, volatile because getState() is public and read from anywhere
	@Getter
	private volatile State state = State.IDLE;
	private long attemptId;
	private int failures;
	private int abandonedWorkers;

	/** False keeps this StreamFetcher from reconnecting after a failure. Playlist items are started this way. */
	@Setter
	private boolean restartStream = true;

	/** True while the attempt that just ended had reached */
	@Getter
	private boolean published;

	private long seekTimeMs;

	/** Time when the current state was entered. So we can calculate how long has fetcher been in current state */
	@Getter
	private volatile long stateSinceMs;

	@Getter
	private Reason lastReason;
	private Reason pendingReason;
	private Result lastError = new Result(false, "");
	private long tickTimerId = -1;
	private long retryTimerId = -1;

	private final AtomicReference<StreamFetcherWorker> currentWorker = new AtomicReference<>();

	public StreamFetcher(String streamUrl, String streamId, String streamType, IScope scope, Vertx vertx, long seekTimeInMs) {
		if (streamUrl == null || streamId == null) {
			throw new NullPointerException("Stream is not initialized properly. Check "
					+ " stream id (" + streamId + ") and stream url (" + streamUrl + ") values");
		}

		this.streamUrl = streamUrl;
		this.streamId = streamId;
		this.streamType = streamType;
		this.scope = scope;
		this.vertx = vertx;
		this.seekTimeMs = seekTimeInMs;

		this.appSettings = (AppSettings) scope.getContext().getApplicationContext().getBean(AppSettings.BEAN_NAME);
	}

	/** Puts this StreamFetcher on the manager's shared context, thread pool and transition listener. */
	public void initialize(Context context, ExecutorService pool, StateListener listener) {
		this.context = context;
		this.pool = pool;
		this.listener = listener;
	}

	/** Overridden by tests to drive the state machine without ffmpeg. */
	protected StreamFetcherWorker createWorker() {
		return new FfmpegWorker(streamUrl, streamId, streamType, seekTimeMs, scope, vertx, appSettings);
	}

	public void startStream() {
		context.runOnContext(v -> {
			if (state != State.IDLE) {
				logger.warn("Start is ignored in state:{} for streamId:{}", state, streamId);
				return;
			}
			if (pool == null) {
				//fail loudly, without a pool no worker can ever end and this would abort and retry forever
				logger.error("Stream fetcher is not initialized with a thread pool, cannot fetch streamId:{}", streamId);
				enterStopped(Reason.NO_RETRY);
				return;
			}
			tickTimerId = vertx.setPeriodic(TICK_PERIOD_MS, t -> tick());
			enterConnecting();
		});
	}

	/**
	 * Asks this StreamFetcher to stop and returns the future that completes once it reaches
	 * {@link State#STOPPED}, which is also when the database says finished.
	 */
	public CompletableFuture<Void> stopStream() {
		logger.info("Stop is requested for {} and streamId:{}", streamUrl, streamId);
		context.runOnContext(v -> {
			switch (state) {
				case IDLE, RECONNECT_WAIT -> enterStopped(Reason.STOP_REQUESTED);
				case CONNECTING, STREAMING -> abort(Reason.STOP_REQUESTED);
				case STOPPING -> pendingReason = Reason.STOP_REQUESTED;
				default -> logger.info("Stop is ignored in state:{} for streamId:{}", state, streamId);
			}
		});
		return stopFuture;
	}

	/**
	 * Stops and waits for this StreamFetcher to reach {@link State#STOPPED}. Must not be called on
	 * the shared vert.x context, the transition it waits for needs that thread.
	 */
	public boolean stopStreamBlocking() {
		if (Context.isOnVertxThread()) {
			logger.error("stopStreamBlocking is called on a vert.x thread for streamId:{}. It cannot complete there", streamId);
		}

		//past the abandonment window, so this outlives the attempt it is waiting for
		long waitMs = STOPPING_TIMEOUT_MS + 2000;

		try {
			stopStream().get(waitMs, TimeUnit.MILLISECONDS);
			return true;
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		catch (Exception e) {
			logger.error(ExceptionUtils.getStackTrace(e));
		}

		logger.warn("Stream fetcher did not stop in {}ms for streamId:{}", waitMs, streamId);
		return false;
	}

	/** Drops the current connection and connects again, keeping this StreamFetcher and its registration. */
	public void restart() {
		context.runOnContext(v -> {
			switch (state) {
				case STREAMING -> abort(Reason.RECONNECT);
				case RECONNECT_WAIT -> enterConnecting();
				default -> logger.info("Restart is ignored in state:{} for streamId:{}", state, streamId);
			}
		});
	}

	public void seekTime(long seekTimeInMilliseconds) {
		context.runOnContext(v -> {
			seekTimeMs = seekTimeInMilliseconds;
			StreamFetcherWorker worker = currentWorker.get();
			if (worker != null) {
				worker.seek(seekTimeInMilliseconds);
			}
		});
	}

	private void enterConnecting() {
		attemptId++;
		long id = attemptId;

		StreamFetcherWorker worker = createWorker();
		worker.lastActivityMs = System.currentTimeMillis();
		worker.onFirstPacket = () -> context.runOnContext(v -> firstPacket(id));
		currentWorker.set(worker);

		transition(State.CONNECTING);

		try {
			pool.execute(() -> runWorker(id, worker));
		}
		catch (RejectedExecutionException e) {
			logger.error("Cannot start a fetch attempt for streamId:{}. {}", streamId, e.getMessage());
			context.runOnContext(v -> workerEnded(id, Reason.OPEN_FAILED));
		}
	}

	private void runWorker(long id, StreamFetcherWorker worker) {
		Reason reason;
		try {
			Broadcast broadcast = getDataStore().get(streamId);
			if (broadcast != null) {
				reason = worker.run(broadcast);
			}
			else {
				logger.info("Broadcast is deleted so it is not fetched for streamId:{}", streamId);
				reason = Reason.DELETED;
			}
		}
		catch (Exception e) {
			logger.error(ExceptionUtils.getStackTrace(e));
			reason = Reason.READ_ERROR;
		}

		Reason ended = reason;
		context.runOnContext(v -> workerEnded(id, ended));
	}

	private void firstPacket(long id) {
		if (id != attemptId || state != State.CONNECTING) {
			return;
		}
		failures = 0;
		published = true;
		lastReason = null;
		transition(State.STREAMING);
	}

	private void workerEnded(long id, Reason reason) {
		if (id != attemptId) {
			logger.info("Dropping {} from the old attempt:{} for streamId:{}", reason, id, streamId);
			return;
		}

		StreamFetcherWorker ended = currentWorker.get();
		Result endedError = ended != null ? ended.error.get() : null;
		if (endedError != null) {
			lastError = endedError;
		}
		currentWorker.set(null);

		switch (state) {
			case CONNECTING, STREAMING -> retryOrStop(reason);
			case STOPPING -> {
				if (pendingReason == Reason.STOP_REQUESTED) {
					enterStopped(Reason.STOP_REQUESTED);
				}
				else if (pendingReason == Reason.RECONNECT) {
					enterConnecting();
				}
				else {
					retryOrStop(Reason.TIMEOUT);
				}
			}
			default -> logger.warn("Worker ended with {} in state:{} for streamId:{}", reason, state, streamId);
		}

		//the transition above is the one that reported the ended attempt, the next one is a new attempt
		published = false;
	}

	private void retryOrStop(Reason reason) {
		int maxRetryAttempts = appSettings.getStreamFetcherMaxRetryAttempts();

		if (reason == Reason.DELETED) {
			enterStopped(Reason.DELETED);
		}
		else if (!restartStream) {
			enterStopped(Reason.NO_RETRY);
		}
		else if (maxRetryAttempts >= 0 && failures + 1 > maxRetryAttempts) {
			logger.error("Giving up on {} after {} failed attempts for streamId:{}", streamUrl, failures, streamId);
			enterStopped(Reason.RETRIES_EXHAUSTED);
		}
		else if (getDataStore().get(streamId) == null) {
			enterStopped(Reason.DELETED);
		}
		else {
			failures++;
			lastReason = reason;
			transition(State.RECONNECT_WAIT);
		}
	}

	private void abort(Reason reason) {
		pendingReason = reason;
		lastReason = reason;
		StreamFetcherWorker worker = currentWorker.get();
		if (worker != null) {
			worker.abortRequested.set(true);
		}
		transition(State.STOPPING);
	}

	private void enterStopped(Reason reason) {
		lastReason = reason;
		transition(State.STOPPED);
	}

	private void tick() {
		long now = System.currentTimeMillis();
		StreamFetcherWorker worker = currentWorker.get();
		boolean fetching = state == State.CONNECTING || state == State.STREAMING;

		if (fetching && worker == null) {
			//a transition died half way, so nothing is pulling this source and nothing will ever end it.
			//Recovering here is what keeps a registered stream fetcher from going silent forever
			logger.error("There is no worker in state:{} for streamId:{}, recovering the stream fetcher", state, streamId);
			workerEnded(attemptId, Reason.READ_ERROR);
		}
		else if (fetching && now - worker.lastActivityMs > ACTIVITY_TIMEOUT_MS) {
			logger.warn("No activity for {}ms in state:{}, aborting the attempt for streamId:{}", ACTIVITY_TIMEOUT_MS, state, streamId);
			abort(Reason.TIMEOUT);
		}
		else if (state == State.STOPPING && now - stateSinceMs > STOPPING_TIMEOUT_MS) {
			abandonedWorkers++;
			if (worker != null) {
				worker.abandonedAtMs = now;
			}
			logger.error("Abandoning stream fetcher worker that did not return in {}ms for url:{} attempt:{} streamId:{}."
					+ " Its ffmpeg thread stays alive until the native call returns. Abandoned worker count for this source is {}",
					STOPPING_TIMEOUT_MS, streamUrl, attemptId, streamId, abandonedWorkers);
			workerEnded(attemptId, Reason.TIMEOUT);
		}

		//the owner does the rest: it is the only one allowed to read the broadcast and decide anything
		if (listener != null && state != State.STOPPED) {
			try {
				listener.onTick(this);
			}
			catch (Exception e) {
				logger.error(ExceptionUtils.getStackTrace(e));
			}
		}
	}

	/**
	 * Commits the new state and its timers, then tells the listeners. A failing listener can never
	 * leave this StreamFetcher half way through a transition.
	 */
	private void transition(State to) {
		State from = state;
		state = to;
		stateSinceMs = System.currentTimeMillis();

		if (to == State.RECONNECT_WAIT) {
			long id = attemptId;
			//vert.x refuses a timer shorter than 1ms, and a throw here would leave this stuck in RECONNECT_WAIT
			long retryDelayMs = Math.max(1, appSettings.getStreamFetcherRetryDelayMs());
			logger.info("Stream fetcher will try to fetch {} again after {}ms, failure:{} streamId:{}", streamUrl, retryDelayMs, failures, streamId);

			retryTimerId = vertx.setTimer(retryDelayMs, t -> {
				if (id == attemptId && state == State.RECONNECT_WAIT) {
					enterConnecting();
				}
			});
		}
		else if (retryTimerId != -1) {
			vertx.cancelTimer(retryTimerId);
			retryTimerId = -1;
		}

		if (to == State.STOPPED && tickTimerId != -1) {
			vertx.cancelTimer(tickTimerId);
			tickTimerId = -1;
		}

		logger.info("Stream fetcher {} -> {} attempt:{} reason:{} streamId:{}", from, to, attemptId, lastReason, streamId);

		try {
			if (listener != null) {
				listener.onTransition(this, from, to);
			}
		}
		catch (Exception e) {
			logger.error(ExceptionUtils.getStackTrace(e));
		}

		try {
			if (streamFetcherListener != null && to == State.STREAMING) {
				streamFetcherListener.streamStarted(streamFetcherListener);
			}
			else if (streamFetcherListener != null && to == State.STOPPED) {
				streamFetcherListener.streamFinished(streamFetcherListener);
			}
		}
		catch (Exception e) {
			logger.error(ExceptionUtils.getStackTrace(e));
		}

		if (to == State.STOPPED) {
			stopFuture.complete(null);
		}
	}

	public boolean isThreadActive() {
		return state == State.CONNECTING || state == State.STREAMING || state == State.STOPPING;
	}

	public Result getCameraError() {
		StreamFetcherWorker worker = currentWorker.get();
		Result workerError = worker != null ? worker.error.get() : null;
		return workerError != null ? workerError : lastError;
	}

	public MuxAdaptor getMuxAdaptor() {
		StreamFetcherWorker worker = currentWorker.get();
		return worker != null ? worker.getMuxAdaptor() : null;
	}

	public void setDataStore(DataStore dataStore) {
		this.dataStore.set(dataStore);
	}

	public DataStore getDataStore() {
		DataStore store = dataStore.get();
		if (store == null) {
			store = getInstance().getDataStore();
			dataStore.set(store);
		}
		return store;
	}

	public AntMediaApplicationAdapter getInstance() {
		AntMediaApplicationAdapter instance = appInstance.get();
		if (instance == null) {
			instance = (AntMediaApplicationAdapter) scope.getContext().getApplicationContext().getBean(AntMediaApplicationAdapter.BEAN_NAME);
			appInstance.set(instance);
		}
		return instance;
	}

}
