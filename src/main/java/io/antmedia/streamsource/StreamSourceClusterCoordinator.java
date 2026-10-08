package io.antmedia.streamsource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.red5.server.api.IContext;
import org.red5.server.api.scope.IScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.AppSettings;
import io.antmedia.cluster.ClusterNode;
import io.antmedia.cluster.IClusterNotifier;
import io.antmedia.cluster.IClusterStore;
import io.antmedia.datastore.db.DataStore;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.datastore.db.types.BroadcastUpdate;
import io.antmedia.licence.ILicenceService;
import io.antmedia.muxer.IAntMediaStreamHandler;
import io.antmedia.rest.model.Result;
import io.antmedia.settings.ServerSettings;
import io.vertx.core.Context;
import io.vertx.core.Vertx;

/**
 * The cluster side of one application's stream sources, one on every node. Each tick every node starts
 * the stale sources it owns, and the lowest live node also assigns the stale sources nobody else will
 * start to the live node with the fewest streams. Ownership only moves through the database, by claims.
 * Created by {@link StreamFetcherManager} in cluster mode only.
 */
public class StreamSourceClusterCoordinator {

	private static final Logger logger = LoggerFactory.getLogger(StreamSourceClusterCoordinator.class);

	private static final long TICK_PERIOD_MS = 5000;

	/** A tick this slow eats most of its period, so the next ones start skipping. */
	private static final long SLOW_TICK_MS = 4000;

	/**
	 * A live owner takes its stale row within a tick, one that has not for this long cannot, so another node gets
	 * it. Well past the longest a source its owner still holds can go without a refresh: a stall, its activity
	 * abort and a stop that has to be abandoned add up to about 60s from the last refresh.
	 */
	static final long OWNER_START_TIMEOUT_MS = 90000;

	/** A cluster is a handful of nodes. This is only here so the query can never grow unbounded. */
	private static final int MAX_CLUSTER_NODES = 1000;

	private final StreamFetcherManager manager;
	private final Vertx vertx;
	/** The manager's shared context, the one every StreamFetcher transition runs on. */
	private final Context context;
	private final IScope scope;
	private final AppSettings appSettings;
	private final ServerSettings serverSettings;
	private final IClusterNotifier clusterNotifier;
	private final ILicenceService licenseService;

	private long timerId;
	private final AtomicBoolean tickRunning = new AtomicBoolean();

	public StreamSourceClusterCoordinator(StreamFetcherManager manager, Vertx vertx, Context context, IScope scope) {
		this.manager = manager;
		this.vertx = vertx;
		this.context = context;
		this.scope = scope;

		IContext red5Context = scope.getContext();
		this.appSettings = (AppSettings) red5Context.getBean(AppSettings.BEAN_NAME);
		this.serverSettings = (ServerSettings) red5Context.getBean(ServerSettings.BEAN_NAME);
		this.clusterNotifier = (IClusterNotifier) red5Context.getBean(IClusterNotifier.BEAN_NAME);
		this.licenseService = (ILicenceService) red5Context.getBean(ILicenceService.BEAN_NAME);

		timerId = vertx.setPeriodic(TICK_PERIOD_MS, l -> vertx.executeBlocking(() -> { tick(); return null; }, false));
	}

	void tick() {
		if (manager.isServerShuttingDown() || !appSettings.isStartStreamFetcherAutomatically() || !tickRunning.compareAndSet(false, true)) {
			return;
		}

		long startTime = System.currentTimeMillis();
		try {
			String host = serverSettings.getHostAddress();
			try {
				assignIfLowest(host);
			}
			catch (Exception e) {
				//what this node owns must not wait on a node list or an assign pass that keeps failing
				logger.error(ExceptionUtils.getStackTrace(e));
			}

			//needs no node list, the database says what this node owns and the claims keep a row on one node.
			//After the assign pass, so what it gave this node starts in the same tick
			startOwnStaleSources(host);
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

	private void assignIfLowest(String host) {
		IClusterStore clusterStore = clusterNotifier.getClusterStore();
		List<ClusterNode> nodes = clusterStore != null ? clusterStore.getClusterNodes(0, MAX_CLUSTER_NODES) : null;
		Set<String> aliveHosts = nodes == null ? Set.of() : nodes.stream()
				.filter(node -> ClusterNode.ALIVE.equals(node.getStatus()) && StringUtils.isNotBlank(node.getIp()))
				.map(ClusterNode::getIp)
				.collect(Collectors.toSet());

		if (!aliveHosts.contains(host)) {
			logger.warn("This node:{} is not in the cluster node list, so it does not assign stream sources", host);
		}
		//every node computes the same lowest address, so exactly one of them assigns
		else if (host.equals(Collections.min(aliveHosts))) {
			assignStaleSources(host, aliveHosts);
		}
	}

	/**
	 * Gives every stale row nobody else will start to the live node with the fewest streams: rows of a node
	 * that left the cluster and does not answer anymore, and rows a live node, or no node at all, did not
	 * start in time. Only the owner changes, the row stays stale and the new owner's own pass takes it.
	 */
	private void assignStaleSources(String host, Set<String> aliveHosts) {
		DataStore datastore = manager.getDatastore();
		long now = System.currentTimeMillis();

		List<Broadcast> toAssign = new ArrayList<>();
		//stale rows a live node owns and has not started yet are load it is about to take
		Map<String, Long> load = new HashMap<>();
		//one check per origin, all in flight at once, so a scan waits for the slowest origin and not for their sum
		Map<String, CompletableFuture<Boolean>> originAnswers = new HashMap<>();

		for (Broadcast row : datastore.getStaleStreamSources(null)) {
			String owner = row.getOriginAdress();

			if (StringUtils.isNotBlank(owner) && !aliveHosts.contains(owner)) {
				originAnswers.computeIfAbsent(owner,
						key -> AntMediaApplicationAdapter.isInstanceAliveAsync(key, host, serverSettings.getDefaultHttpPort(), scope.getName()));
				toAssign.add(row);
			}
			//a live owner that has not taken it by now cannot: still booting, its starts are refused, or the app does
			//not run there. A row with no owner has nobody to ask, so it waits just as long
			else if (now - row.getUpdateTime() >= OWNER_START_TIMEOUT_MS) {
				toAssign.add(row);
			}
			else if (StringUtils.isNotBlank(owner)) {
				load.merge(owner, 1L, Long::sum);
			}
		}

		//the node list can lag and a clock can be off, but a node that still answers http is still fetching
		toAssign.removeIf(row -> originAnswers.getOrDefault(row.getOriginAdress(), CompletableFuture.completedFuture(false)).join());
		if (toAssign.isEmpty()) {
			return;
		}

		for (String node : aliveHosts) {
			load.merge(node, datastore.getLocalLiveBroadcastCount(node), Long::sum);
		}

		for (Broadcast row : toAssign) {
			//never back to the owner that did not start it, and ties go to the lower address like the election
			String target = aliveHosts.stream()
					.filter(node -> !node.equals(row.getOriginAdress()))
					.min(Comparator.<String>comparingLong(load::get).thenComparing(Comparator.naturalOrder()))
					.orElse(null);
			if (target == null) {
				continue;
			}

			BroadcastUpdate assign = new BroadcastUpdate();
			assign.setOriginAdress(target);
			assign.setStatus(IAntMediaStreamHandler.BROADCAST_STATUS_TERMINATED_UNEXPECTEDLY);
			//the target's time to start it counts from here
			assign.setUpdateTime(System.currentTimeMillis());

			if (datastore.claimStaleStreamSource(row.getStreamId(), row.getOriginAdress(), assign)) {
				load.merge(target, 1L, Long::sum);
				logger.info("Assigned streamId:{} of node:{} to node:{}", row.getStreamId(), row.getOriginAdress(), target);
			}
		}
	}

	/**
	 * Takes the stale sources the database says this node owns and starts them. Each on the shared context,
	 * because a STOPPED transition unregisters before it writes finished, and a source the user just
	 * stopped must not start again in that gap.
	 */
	private void startOwnStaleSources(String host) {
		DataStore datastore = manager.getDatastore();

		for (Broadcast row : datastore.getStaleStreamSources(host)) {
			context.runOnContext(v -> {
				//a start refused after the take would keep the row fresh, so it would never move to a node that can start it
				if (manager.isServerShuttingDown() || licenseService.isLicenceSuspended() || manager.getStreamFetcher(row.getStreamId()) != null) {
					return;
				}

				BroadcastUpdate take = manager.getApplication().getFreshBroadcastUpdateForStatus(IAntMediaStreamHandler.PUBLISH_TYPE_PULL, IAntMediaStreamHandler.BROADCAST_STATUS_PREPARING);
				if (datastore.claimStaleStreamSource(row.getStreamId(), host, take)) {
					//forced, the take already wrote preparing
					Result result = manager.startStreaming(row, true);
					logger.info("Took the stale stream source streamId:{} this node owns, start success:{} message:{}",
							row.getStreamId(), result.isSuccess(), result.getMessage());
				}
			});
		}
	}

	public void shutdown() {
		vertx.cancelTimer(timerId);
		timerId = -1;
	}
}
