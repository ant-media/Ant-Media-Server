package io.antmedia.streamsource;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.cluster.ClusterNode;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.muxer.IAntMediaStreamHandler;
import io.antmedia.rest.model.Result;
import io.antmedia.settings.ServerSettings;

/**
 * Moves running stream sources from the nodes that hold the most to the nodes that hold the fewest, counted over
 * every application as placement counts them, until the fewest is streamSourceBalanceFactorTarget percent of the
 * most. Only sources of applications with streamFetcherRebalanceEnabled move, at most streamSourceRebalanceBatchSize
 * to each node per round, because a move is a claim over the running holder: the source is down until the new holder
 * is on air.
 *
 * A source only moves to a node holding at least two fewer, so every move evens the counts out and no source moves
 * back and forth. Ticked by the leader in its first application only, so one place decides for the cluster. Only
 * that coordinator's ticks touch the fields, and its tickRunning flag orders them.
 */
class StreamSourceRebalancer {

	private static final Logger logger = LoggerFactory.getLogger(StreamSourceRebalancer.class);

	/** After the nodes that may fetch change: the leader places a failover at once, a node that joined starts its applications. */
	static final long SETTLE_MS = 20000;

	private final StreamSourceCoordinator coordinator;
	private final ServerSettings serverSettings;

	/** The nodes that may fetch, as the last tick saw them. */
	private Set<String> nodes = Set.of();
	/** System.nanoTime() when the current wait for the next round began. */
	private long waitingSince;
	private long waitMs;

	private record Movable(AntMediaApplicationAdapter application, Broadcast row) { }

	StreamSourceRebalancer(StreamSourceCoordinator coordinator, ServerSettings serverSettings) {
		this.coordinator = coordinator;
		this.serverSettings = serverSettings;
	}

	/**
	 * Runs a round when one is due: SETTLE_MS after the nodes that may fetch change, on the next tick after a round
	 * that moved sources, else a period after the last round.
	 */
	void tick() {
		Map<String, ClusterNode> mayFetch = StreamSourceCoordinator.nodesThatMayFetch(coordinator.aliveNodes());
		long now = System.nanoTime();
		boolean nodesChanged = !mayFetch.keySet().equals(nodes);
		if (!nodesChanged && TimeUnit.NANOSECONDS.toMillis(now - waitingSince) < waitMs) {
			return;
		}

		//before the round, so a round that throws waits a full period too
		nodes = Set.copyOf(mayFetch.keySet());
		waitingSince = now;
		waitMs = nodesChanged ? SETTLE_MS : serverSettings.getStreamSourceRebalancePeriodMs();
		if (!nodesChanged && round(mayFetch)) {
			//its moves are in the counts the next tick reads
			waitMs = 0;
		}
	}

	/**
	 * Moves one source at a time to the node holding the fewest that has room, did not refuse this round and has not
	 * taken a batch yet, from the node holding the most of those holding at least two more that has one to move. A
	 * refusal only drops that node. Any other answer uses one move of that node's batch, and the counts follow only
	 * a move that succeeded.
	 *
	 * @return true when it used a move
	 */
	private boolean round(Map<String, ClusterNode> mayFetch) {
		List<AntMediaApplicationAdapter> rebalanced = coordinator.applications().stream()
				.filter(application -> application.getAppSettings().isStreamFetcherRebalanceEnabled())
				.toList();
		if (rebalanced.isEmpty()) {
			return false;
		}

		Map<String, Long> heldEverywhere = coordinator.heldCountsOfEveryApplication();
		Map<String, Long> held = new HashMap<>();
		mayFetch.keySet().forEach(node -> held.put(node, heldEverywhere.getOrDefault(node, 0L)));

		Set<String> receivers = mayFetch.values().stream()
				.filter(node -> !coordinator.isFull(node))
				.map(ClusterNode::getIp)
				.collect(Collectors.toCollection(HashSet::new));
		Comparator<String> fewestFirst = Comparator.<String>comparingLong(held::get).thenComparing(Comparator.naturalOrder());
		Comparator<String> mostFirst = Comparator.<String>comparingLong(held::get).reversed().thenComparing(Comparator.naturalOrder());
		Map<String, Deque<Movable>> movable = new HashMap<>();
		int batchSize = serverSettings.getStreamSourceRebalanceBatchSize();
		int target = serverSettings.getStreamSourceBalanceFactorTarget();
		Map<String, Integer> taken = new HashMap<>();

		while (balanceFactor(held.values()) < target) {
			String to = receivers.stream()
					.filter(node -> taken.getOrDefault(node, 0) < batchSize)
					.min(fewestFirst)
					.orElse(null);
			if (to == null) {
				break;
			}
			long fewest = held.get(to);
			String from = held.keySet().stream()
					.filter(node -> held.get(node) - fewest >= 2)
					.sorted(mostFirst)
					.filter(node -> !movable.computeIfAbsent(node, holder -> movableSources(holder, rebalanced, mayFetch)).isEmpty())
					.findFirst()
					.orElse(null);
			if (from == null) {
				logger.debug("No stream source can move to node:{} to balance the nodes, held:{}", to, held);
				break;
			}

			Movable source = movable.get(from).poll();
			String streamId = source.row().getStreamId();
			logger.info("Moving streamId:{} of app:{} from node:{} to node:{} to balance the nodes, held:{}", streamId, source.application().getName(), from, to, held);
			Result result = source.application().getStreamFetcherManager().getCoordinator().claimAndStartOn(to, streamId, from, false);
			if (result.getErrorId() == StreamFetcherManager.START_REFUSED) {
				logger.info("Node:{} did not take streamId:{}, it gets no source this round: {}", to, streamId, result.getMessage());
				receivers.remove(to);
				movable.get(from).push(source);
				continue;
			}

			taken.merge(to, 1, Integer::sum);
			if (result.isSuccess()) {
				held.merge(from, -1L, Long::sum);
				held.merge(to, 1L, Long::sum);
			}
			else {
				logger.info("streamId:{} did not move to node:{}: {}", streamId, to, result.getMessage());
			}
		}
		return !taken.isEmpty();
	}

	/**
	 * The sources of the rebalanced applications that the node runs on air, least watched first. Stays: a source
	 * pinned to a node that may fetch, as in placement, one that runs on demand, it is placed again at its next start,
	 * and a VOD, which would play from its beginning again.
	 */
	private static Deque<Movable> movableSources(String node, List<AntMediaApplicationAdapter> rebalanced, Map<String, ClusterNode> mayFetch) {
		return rebalanced.stream()
				.flatMap(application -> application.getDataStore().getHeldStreamFetcherSources(node).stream()
						.filter(row -> IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING.equals(row.getStatus())
								&& Broadcast.FETCHER_DESIRED_ACTION_START.equals(row.getFetcherDesiredAction())
								&& !row.isAutoStartStopEnabled()
								&& !AntMediaApplicationAdapter.VOD.equals(row.getType())
								&& !mayFetch.containsKey(row.getPreferredFetcherNode()))
						.map(row -> new Movable(application, row)))
				//a count can be below zero after a move, the new holder starts from zero while the old viewers leave
				.sorted(Comparator.comparingInt((Movable source) -> Math.max(0, source.row().getHlsViewerCount())
						+ Math.max(0, source.row().getWebRTCViewerCount()) + Math.max(0, source.row().getRtmpViewerCount())
						+ Math.max(0, source.row().getDashViewerCount())))
				.collect(Collectors.toCollection(ArrayDeque::new));
	}

	/** The fewest held as a percent of the most, rounded up. 100 when nobody holds any. */
	private static int balanceFactor(Collection<Long> held) {
		long most = Collections.max(held);
		return most == 0 ? 100 : (int) Math.ceil(Collections.min(held) * 100.0 / most);
	}
}
