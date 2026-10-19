package io.antmedia.streamsource;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.red5.server.api.IContext;
import org.red5.server.api.scope.IScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.antmedia.AppSettings;
import io.antmedia.cluster.ClusterNode;
import io.antmedia.cluster.IClusterNotifier;
import io.antmedia.datastore.db.DataStore;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.datastore.db.types.BroadcastUpdate;
import io.vertx.core.Context;
import io.vertx.core.Vertx;

/**
 * Keeps the wanted stream sources of one application running. 
 * Runs on every node. Standalone mode is treated as cluster of one.
 * 
 * Each tick this node starts or lets go of the sources it holds, and the leader (lowest alive
 * address) takes every source nobody holds. Every take is a claim on the row, so only one node gets it.
 */
public class StreamSourceCoordinator {

	private static final Logger logger = LoggerFactory.getLogger(StreamSourceCoordinator.class);

	private static final long TICK_PERIOD_MS = 5000;

	/** A tick that takes this long logs a warning. 
	 *  A tick still running when the next one is due makes that one skip. */
	private static final long SLOW_TICK_MS = 4000;

	private final StreamFetcherManager manager;
	private final Vertx vertx;
	/** The manager's shared context, the one every StreamFetcher transition runs on. */
	private final Context context;
	private final IScope scope;
	private final AppSettings appSettings;
	/** Null on a standalone server. */
	private final IClusterNotifier clusterNotifier;

	private long timerId;
	private final AtomicBoolean tickRunning = new AtomicBoolean();

	public StreamSourceCoordinator(StreamFetcherManager manager, Vertx vertx, Context context, IScope scope) {
		this.manager = manager;
		this.vertx = vertx;
		this.context = context;
		this.scope = scope;

		IContext red5Context = scope.getContext();
		this.appSettings = (AppSettings) red5Context.getBean(AppSettings.BEAN_NAME);
		this.clusterNotifier = red5Context.hasBean(IClusterNotifier.BEAN_NAME) ? (IClusterNotifier) red5Context.getBean(IClusterNotifier.BEAN_NAME) : null;

		timerId = vertx.setPeriodic(TICK_PERIOD_MS, l -> vertx.executeBlocking(() -> { tick(); return null; }, false));
	}

	/** Every TICK_PERIOD_MS, and once at application start, which resumes the sources this node held before. */
	public void tick() {
		if (manager.isServerShuttingDown() || !tickRunning.compareAndSet(false, true)) {
			return;
		}

		long startTime = System.currentTimeMillis();
		try {
			reconcileHeldSources();
			if (isLeader()) {
				takeOrphans();
			}
		}
		catch (Exception e) {
			logger.error(ExceptionUtils.getStackTrace(e));
		}
		finally {
			long elapsedMs = System.currentTimeMillis() - startTime;
			if (elapsedMs >= SLOW_TICK_MS) {
				logger.warn("Stream source check took {}ms for app:{}, it runs every {}ms", elapsedMs, scope.getName(), TICK_PERIOD_MS);
			}
			tickRunning.set(false);
		}
	}

	/**
	 * Starts or lets go of every source held here that nothing here runs, mostly after a restart. On the
	 * shared context, because STOPPED unregisters before it lets go of the row: from any other thread a
	 * source that just stopped looks held and not running.
	 */
	private void reconcileHeldSources() {
		DataStore datastore = manager.getDatastore();
		String host = manager.getHost();

		for (Broadcast row : datastore.getHeldStreamFetcherSources(host)) {
			context.runOnContext(v -> {
				String streamId = row.getStreamId();
				if (manager.getStreamFetcher(streamId) != null) {
					return;
				}

				//held where nothing runs it would stay down for good, let it go for the leader
				if (!isToBeStartedAutomatically(row) || !manager.claimAndStart(row, host, false).isSuccess()) {
					logger.info("Letting go of streamId:{}, this node holds it but does not run it", streamId);
					datastore.compareAndSetFetcherNode(streamId, host, null, false, new BroadcastUpdate());
				}
			});
		}
	}

	/**
	 * Takes every wanted source nobody holds, or whose node is dead. 
	 * One that was let go after a failed attempt waits out streamFetcherRetryDelayMs first, 
	 * counted from the start of that attempt.
	 */
	private void takeOrphans() {
		long startedBefore = System.currentTimeMillis() - appSettings.getStreamFetcherRetryDelayMs();

		for (Broadcast row : manager.getDatastore().getOrphanStreamFetcherSources(aliveNodes())) {
			if (isToBeStartedAutomatically(row) && row.getStartTime() <= startedBefore) {
				context.runOnContext(v -> manager.claimAndStart(row, row.getFetcherNode(), false));
			}
		}
	}

	/**
	 * A wanted source is started without anybody asking after a failed attempt, \
	 * until the retry limit stops it.
	 * 
	 * After a restart or a failover only when startStreamFetcherAutomatically is on.
	 */
	private boolean isToBeStartedAutomatically(Broadcast row) {
		return Broadcast.FETCHER_DESIRED_ACTION_START.equals(row.getFetcherDesiredAction())
				&& (appSettings.isStartStreamFetcherAutomatically() || row.getFetcherFailures() > 0);
	}

	/** The nodes whose heartbeat is fresh, and this one whatever its own heartbeat says. */
	Set<String> aliveNodes() {
		Set<String> alive = new HashSet<>();
		alive.add(manager.getHost());
		if (clusterNotifier != null) {
			clusterNotifier.getAliveNodes().forEach(node -> alive.add(node.getIp()));
		}
		return alive;
	}

	private boolean isLeader() {
		if (clusterNotifier == null) {
			return true;
		}
		ClusterNode leader = clusterNotifier.getLeader();
		return leader != null && manager.getHost().equals(leader.getIp());
	}

	public void shutdown() {
		vertx.cancelTimer(timerId);
		timerId = -1;
	}
}
