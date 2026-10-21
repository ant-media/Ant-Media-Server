package io.antmedia.streamsource;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.red5.server.api.IContext;
import org.red5.server.api.scope.IScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import io.antmedia.AppSettings;
import io.antmedia.cluster.ClusterNode;
import io.antmedia.cluster.IClusterNotifier;
import io.antmedia.datastore.db.DataStore;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.datastore.db.types.BroadcastUpdate;
import io.antmedia.filter.JWTFilter;
import io.antmedia.filter.TokenFilterManager;
import io.antmedia.rest.model.Result;
import io.antmedia.settings.ServerSettings;
import io.antmedia.statistic.IStatsCollector;
import io.vertx.core.Context;
import io.vertx.core.Vertx;

/**
 * Decides where the stream sources of one application run. 
 * Runs on every node. Standalone mode is treated as cluster of one.
 * 
 * A start goes to the alive node that holds the source, or else to the alive node with room and the fewest
 * sources. Each tick this node starts or lets go of the sources it holds, and the leader (lowest alive
 * address) places every source nobody alive holds. Every take is a claim on the row, so only one node gets it.
 *
 * Nodes with streamSourceFetchEnabled off (edges) take a source only while no node with it on is alive, and hand
 * it over once one with room is.
 *
 * A source with a preferredFetcherNode that is alive and able to fetch starts there and nowhere else, it only fails
 * over while that node is dead. A source that already runs on another node is not moved to it.
 */
public class StreamSourceCoordinator {

	private static final Logger logger = LoggerFactory.getLogger(StreamSourceCoordinator.class);

	private static final long TICK_PERIOD_MS = 5000;

	/** A tick that takes this long logs a warning. 
	 *  A tick still running when the next one is due makes that one skip. */
	private static final long SLOW_TICK_MS = 4000;

	/** Placements the leader runs at once, so a big failover does not flood the nodes. */
	private static final int MAX_PLACEMENTS_IN_FLIGHT = 10;

	/** A node that does not answer a claim in this time is skipped. */
	private static final long NODE_CALL_TIMEOUT_MS = 1500;

	/**
	 * How long the token on a claim-and-start call to another node stays valid. The other node checks the expiry
	 * with its own clock, so this leaves room for node clocks a few seconds apart.
	 */
	private static final long NODE_TOKEN_VALIDITY_MS = 10000;

	private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
			.version(HttpClient.Version.HTTP_1_1)
			.connectTimeout(Duration.ofMillis(NODE_CALL_TIMEOUT_MS))
			.build();

	private static final Gson GSON = new Gson();

	private final StreamFetcherManager manager;
	private final Vertx vertx;
	/** The manager's shared context, the one every StreamFetcher transition runs on. */
	private final Context context;
	/** The manager's pool, it runs the leader's placements. */
	private final ExecutorService pool;
	private final IScope scope;
	private final AppSettings appSettings;
	private final ServerSettings serverSettings;
	/** Null on a standalone server. */
	private final IClusterNotifier clusterNotifier;

	private long timerId;
	private final AtomicBoolean tickRunning = new AtomicBoolean();

	public StreamSourceCoordinator(StreamFetcherManager manager, Vertx vertx, Context context, ExecutorService pool, IScope scope) {
		this.manager = manager;
		this.vertx = vertx;
		this.context = context;
		this.pool = pool;
		this.scope = scope;

		IContext red5Context = scope.getContext();
		this.appSettings = (AppSettings) red5Context.getBean(AppSettings.BEAN_NAME);
		this.serverSettings = (ServerSettings) red5Context.getBean(ServerSettings.BEAN_NAME);
		this.clusterNotifier = red5Context.hasBean(IClusterNotifier.BEAN_NAME) ? (IClusterNotifier) red5Context.getBean(IClusterNotifier.BEAN_NAME) : null;

		if (!serverSettings.isStreamSourceFetchEnabled()) {
			if (clusterNotifier == null) {
				logger.info("streamSourceFetchEnabled=false has no effect without a cluster, app:{}", scope.getName());
			}
			else {
				logger.info("This node does not fetch stream sources of app:{} while another node can (streamSourceFetchEnabled=false)", scope.getName());
			}
		}

		timerId = vertx.setPeriodic(TICK_PERIOD_MS, l -> vertx.executeBlocking(() -> { tick(); return null; }, false));
	}

	/** Every TICK_PERIOD_MS, and once at application start, which resumes the sources this node held before. */
	public void tick() {
		if (manager.isServerShuttingDown() || !tickRunning.compareAndSet(false, true)) {
			return;
		}

		long startTime = System.currentTimeMillis();
		try {
			List<Broadcast> held = manager.getDatastore().getHeldStreamFetcherSources(manager.getHost());
			reconcileHeldSources(held);
			handBack(held);
			if (isLeader()) {
				placeOrphans();
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
	private void reconcileHeldSources(List<Broadcast> held) {
		DataStore datastore = manager.getDatastore();
		String host = manager.getHost();

		for (Broadcast row : held) {
			context.runOnContext(v -> {
				String streamId = row.getStreamId();
				if (manager.getStreamFetcher(streamId) != null) {
					return;
				}

				//held where nothing runs it would stay down for good, let it go for the leader
				if (!isToBeStartedAutomatically(row) || !manager.claimAndStart(streamId, host, false).isSuccess()) {
					logger.info("Letting go of streamId:{}, this node holds it but does not run it", streamId);
					datastore.compareAndSetFetcherNode(streamId, host, null, false, new BroadcastUpdate());
				}
			});
		}
	}

	/**
	 * On a node with streamSourceFetchEnabled off, moves what it runs as the last resort to a node that fetches,
	 * once one with room is alive. That node claims it over this one, whose fetcher tick then stops it here without
	 * a trace, so it never stops before another node runs it. A refusal leaves it running here.
	 */
	private void handBack(List<Broadcast> held) {
		if (serverSettings.isStreamSourceFetchEnabled()) {
			return;
		}

		List<Broadcast> running = held.stream().filter(row -> manager.getStreamFetcher(row.getStreamId()) != null).toList();
		if (running.isEmpty()) {
			return;
		}

		Map<String, ClusterNode> mayFetch = nodesThatMayFetch(aliveNodes());
		if (mayFetch.containsKey(manager.getHost()) || mayFetch.values().stream().allMatch(this::isFull)) {
			return;
		}

		//a node that refused is not asked again this tick, so a starting origin costs one call
		Set<String> refused = new HashSet<>();
		for (Broadcast row : running) {
			if (refused.containsAll(mayFetch.keySet())) {
				return;
			}
			logger.info("Handing streamId:{} over to a node that fetches stream sources", row.getStreamId());
			placeAndStart(row, false, refused);
		}
	}

	/**
	 * Places every wanted source nobody holds, or whose node is dead. 
	 * One that was let go after a failed attempt waits out streamFetcherRetryDelayMs first, 
	 * counted from the start of that attempt.
	 *
	 * Each source is offered to one node at a time, MAX_PLACEMENTS_IN_FLIGHT at once. A refusal is about the node
	 * (full, still starting, going down, not answering), so that node gets nothing more this tick and what it
	 * refused is placed again with fresh counts. Falling through to a second choice worked out up front would send
	 * everything one node refused to the same next node.
	 */
	private void placeOrphans() {
		Map<String, ClusterNode> alive = aliveNodes();
		long startedBefore = System.currentTimeMillis() - appSettings.getStreamFetcherRetryDelayMs();

		Deque<Broadcast> pending = manager.getDatastore().getOrphanStreamFetcherSources(alive.keySet()).stream()
				.filter(row -> isToBeStartedAutomatically(row) && row.getStartTime() <= startedBefore)
				.collect(Collectors.toCollection(ArrayDeque::new));
		if (pending.isEmpty()) {
			return;
		}

		//picked once from every alive node: an origin that refuses below must not let the edges in, nor a preferred
		//node that refuses let another node take its sources
		Map<String, ClusterNode> mayFetch = nodesThatMayFetch(alive);
		Set<String> refused = new HashSet<>();
		Map<String, Long> held = new HashMap<>(manager.getDatastore().getHeldStreamFetcherSourceCounts());
		while (!pending.isEmpty() && !manager.isServerShuttingDown()) {
			List<Placement> placements = new ArrayList<>();
			while (placements.size() < MAX_PLACEMENTS_IN_FLIGHT && !pending.isEmpty()) {
				Broadcast row = pending.poll();
				List<String> candidates = candidates(row, mayFetch, held, refused);
				if (candidates.isEmpty()) {
					logger.warn("No node can take streamId:{} now", row.getStreamId());
					continue;
				}
				//counted when it is chosen, not when it answers, so the placements running at once spread too
				String node = candidates.get(0);
				held.merge(node, 1L, Long::sum);
				placements.add(new Placement(row, node,
						CompletableFuture.supplyAsync(() -> claimAndStartOn(node, row.getStreamId(), row.getFetcherNode(), false), pool)));
			}

			for (Placement placement : placements) {
				Result result = placement.result().join();
				if (result.getErrorId() == StreamFetcherManager.START_REFUSED) {
					logger.info("Node:{} did not take streamId:{}, it is skipped until the next tick: {}", placement.node(), placement.row().getStreamId(), result.getMessage());
					held.merge(placement.node(), -1L, Long::sum);
					refused.add(placement.node());
					pending.add(placement.row());
				}
			}
		}
	}

	/** A source the leader offered to one node, and that node's answer to come. */
	private record Placement(Broadcast row, String node, CompletableFuture<Result> result) { }

	/**
	 * Starts the source on the first of its candidates that takes it, moving on while a node refuses or does not
	 * answer. Stops at any other answer: a node took it, a node claimed it but could not start it, or the claim was
	 * lost because the row changed since it was read.
	 */
	Result placeAndStart(Broadcast row, boolean userStart) {
		return placeAndStart(row, userStart, new HashSet<>());
	}

	/** @param refused the nodes that refused this tick, they are not asked, and every node that refuses here is added */
	private Result placeAndStart(Broadcast row, boolean userStart, Set<String> refused) {
		String streamId = row.getStreamId();
		Result result = new Result(false, streamId, "No node can take the stream now");
		result.setErrorId(StreamFetcherManager.START_REFUSED);

		for (String node : candidates(row, nodesThatMayFetch(aliveNodes()), manager.getDatastore().getHeldStreamFetcherSourceCounts(), refused)) {
			result = claimAndStartOn(node, streamId, row.getFetcherNode(), userStart);
			if (result.getErrorId() != StreamFetcherManager.START_REFUSED) {
				return result;
			}
			logger.info("Node:{} did not take streamId:{} {}", node, streamId, result.getMessage());
			refused.add(node);
		}

		logger.warn("No node could take streamId:{} {}", streamId, result.getMessage());
		return result;
	}

	/**
	 * The holder while it may fetch, else the preferred node while it may fetch, alone even when full, and none once
	 * it refused. Otherwise the nodes that may fetch, have room and did not refuse, by: fewest held sources, then this
	 * node, then the lower address. An alive holder that may not fetch (an edge while an origin is alive) loses the
	 * source to the one that claims it, its own tick then stops it.
	 **/
	private List<String> candidates(Broadcast row, Map<String, ClusterNode> mayFetch, Map<String, Long> held, Set<String> refused) {
		String pinned = mayFetch.containsKey(row.getFetcherNode()) ? row.getFetcherNode() : row.getPreferredFetcherNode();
		if (mayFetch.containsKey(pinned)) {
			return refused.contains(pinned) ? List.of() : List.of(pinned);
		}

		String host = manager.getHost();
		return mayFetch.values().stream()
				.filter(node -> !isFull(node) && !refused.contains(node.getIp()))
				.map(ClusterNode::getIp)
				.sorted(Comparator.<String>comparingLong(ip -> held.getOrDefault(ip, 0L))
						.thenComparing(ip -> !host.equals(ip))
						.thenComparing(Comparator.naturalOrder()))
				.toList();
	}

	/** At or over this node's cpu or memory limit by its last heartbeat. A node without numbers is not full. */
	private boolean isFull(ClusterNode node) {
		IStatsCollector stats = manager.getApplication().getStatsCollector();
		try {
			//a cpu limit of 100 switches the cpu check off, as in enoughResource
			if (stats.getCpuLimit() < 100 && node.getCpu() != null && Double.parseDouble(node.getCpu()) >= stats.getCpuLimit()) {
				return true;
			}
			if (node.getMemory() != null) {
				String[] usedAndTotalMb = node.getMemory().split("/");
				double totalMb = Double.parseDouble(usedAndTotalMb[1]);
				return totalMb > 0 && Double.parseDouble(usedAndTotalMb[0]) * 100 / totalMb >= stats.getMemoryLimit();
			}
		}
		catch (RuntimeException e) {
			logger.warn("Cannot read the load of node:{} cpu:{} memory:{}", node.getIp(), node.getCpu(), node.getMemory());
		}
		return false;
	}

	/** {@link StreamFetcherManager#claimAndStart} on the given node, over the claim call for another one. No answer counts as a refusal. */
	private Result claimAndStartOn(String node, String streamId, @Nullable String expectedFetcherNode, boolean userStart) {
		if (manager.getHost().equals(node)) {
			return manager.claimAndStart(streamId, expectedFetcherNode, userStart);
		}

		String url = "http://" + node + ":" + serverSettings.getDefaultHttpPort() + "/" + scope.getName()
				+ "/rest/cluster-communication/stream-source/" + streamId + "/claim-and-start?userStart=" + userStart
				+ (expectedFetcherNode != null ? "&expectedFetcherNode=" + expectedFetcherNode : "");
		try {
			HttpRequest request = HttpRequest.newBuilder(URI.create(url))
					.timeout(Duration.ofMillis(NODE_CALL_TIMEOUT_MS))
					.header(TokenFilterManager.TOKEN_HEADER_FOR_NODE_COMMUNICATION,
							JWTFilter.generateJwtToken(appSettings.getClusterCommunicationKey(), System.currentTimeMillis() + NODE_TOKEN_VALIDITY_MS))
					.POST(HttpRequest.BodyPublishers.noBody())
					.build();

			HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
			Result result = response.statusCode() == HttpURLConnection.HTTP_OK ? GSON.fromJson(response.body(), Result.class) : null;
			if (result != null) {
				return result;
			}
			logger.warn("Node:{} answered the claim of streamId:{} with HTTP {}", node, streamId, response.statusCode());
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		catch (Exception e) {
			logger.warn("Node:{} did not answer the claim of streamId:{} {}", node, streamId, e.toString());
		}

		Result noAnswer = new Result(false, streamId, "Node " + node + " did not answer");
		noAnswer.setErrorId(StreamFetcherManager.START_REFUSED);
		return noAnswer;
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

	/** The alive nodes by address with their last heartbeat, and this one whatever its own heartbeat says. */
	Map<String, ClusterNode> aliveNodes() {
		Map<String, ClusterNode> alive = new HashMap<>();
		if (clusterNotifier != null) {
			clusterNotifier.getAliveNodes().forEach(node -> alive.put(node.getIp(), node));
		}
		//this node's own setting is the truth, its heartbeat may still carry the one it had before a restart
		alive.computeIfAbsent(manager.getHost(), host -> new ClusterNode(host, host))
				.setStreamSourceFetchEnabled(serverSettings.isStreamSourceFetchEnabled());
		return alive;
	}

	/**
	 * The alive nodes with streamSourceFetchEnabled, or all of them as a last resort when none of those is alive.
	 * One that is full still counts, so it keeps the others out.
	 */
	private static Map<String, ClusterNode> nodesThatMayFetch(Map<String, ClusterNode> alive) {
		Map<String, ClusterNode> mayFetch = new HashMap<>(alive);
		mayFetch.values().removeIf(node -> !node.isStreamSourceFetchEnabled());
		return mayFetch.isEmpty() ? new HashMap<>(alive) : mayFetch;
	}

	/** False while this node has streamSourceFetchEnabled off and a node with it on is alive. */
	boolean mayFetchHere() {
		return serverSettings.isStreamSourceFetchEnabled() || nodesThatMayFetch(aliveNodes()).containsKey(manager.getHost());
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
