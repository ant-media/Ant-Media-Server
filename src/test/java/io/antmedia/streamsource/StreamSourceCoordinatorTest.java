package io.antmedia.streamsource;

import static io.antmedia.streamsource.StreamSourceFixture.HOST;
import static io.antmedia.streamsource.StreamSourceFixture.aliveNodes;
import static io.antmedia.streamsource.StreamSourceFixture.peek;
import static io.antmedia.streamsource.StreamSourceFixture.settle;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import io.antmedia.UrlQueryUtils;
import io.antmedia.cluster.ClusterNode;
import io.antmedia.cluster.IClusterNotifier;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.filter.JWTFilter;
import io.antmedia.filter.TokenFilterManager;
import io.antmedia.rest.model.Result;
import io.antmedia.streamsource.StreamSourceFixture.ScriptedManager;

/**
 * Where the stream sources run: a start goes to the alive holder or the best node, every node starts or lets
 * go of what it holds, and the leader places what nobody holds. Every tick is run by hand.
 */
class StreamSourceCoordinatorTest {

	private static final String PEER = "127.0.0.5";

	private static final String OTHER_PEER = "127.0.0.6";

	/** A node that is not alive. */
	private static final String DEAD = "192.168.1.9";

	/** What another node was asked. */
	record Claim(String node, String streamId, String expectedFetcherNode, boolean userStart, boolean signed) { }

	private StreamSourceFixture fixture;

	/** Every other node. 127.0.0.x all reach this one server, it tells them apart by the address called. */
	private HttpServer peers;
	private final List<Claim> claims = new CopyOnWriteArrayList<>();
	/** How a peer answers a claim. One that is not in here takes the source. */
	private final Map<String, Result> answers = new ConcurrentHashMap<>();
	/** Peers that answer with an HTTP error, the way one that is going down does. */
	private final Set<String> broken = ConcurrentHashMap.newKeySet();

	@BeforeEach
	void before() throws IOException {
		fixture = new StreamSourceFixture();

		peers = HttpServer.create(new InetSocketAddress(0), 0);
		peers.createContext("/", this::answerClaim);
		peers.start();
		when(fixture.serverSettings.getDefaultHttpPort()).thenReturn(peers.getAddress().getPort());
	}

	@AfterEach
	void after() {
		peers.stop(0);
		fixture.close();
	}

	@Test
	void aNodeStartsWhatItHoldsAndLetsGoOfTheRest() {
		ScriptedManager node = fixture.newManager();
		fixture.appSettings.setStartStreamFetcherAutomatically(true);

		Broadcast wanted = row("wanted", HOST);
		Broadcast stopped = row("stopped", HOST);
		stopped.setFetcherDesiredAction(Broadcast.FETCHER_DESIRED_ACTION_STOP);
		//nobody watched it when this node went down, a viewer starts it again
		Broadcast onDemand = row("on-demand", HOST);
		onDemand.setAutoStartStopEnabled(true);

		StreamFetcher running = mock(StreamFetcher.class);
		node.getStreamFetcherList().put("running", running);
		row("running", HOST);

		tick(node);
		assertEquals(Set.of("wanted", "running"), node.getStreamFetcherList().keySet());
		assertSame(running, node.getStreamFetcher("running"), "a source that runs here is not started again");
		assertEquals(HOST, wanted.getFetcherNode());
		assertNull(stopped.getFetcherNode(), "nothing runs it here, so it is let go");
		assertNull(onDemand.getFetcherNode());

		//with the setting off a restart brings nothing back, but a failed attempt is still tried again
		fixture.appSettings.setStartStreamFetcherAutomatically(false);
		Broadcast notResumed = row("not-resumed", HOST);
		row("retried", HOST).setFetcherFailures(1);
		tick(node);
		assertNull(notResumed.getFetcherNode());
		assertTrue(node.getStreamFetcherList().containsKey("retried"));

		//a start that is refused must not leave the source held where nothing runs it
		when(fixture.statsCollector.enoughResource()).thenReturn(false);
		Broadcast refused = row("refused", HOST);
		refused.setFetcherFailures(1);
		tick(node);
		assertNull(refused.getFetcherNode());
		assertFalse(node.getStreamFetcherList().containsKey("refused"));
	}

	@Test
	void theLeaderTakesEveryWantedSourceNobodyHolds() {
		ScriptedManager node = fixture.newManager();
		fixture.appSettings.setStartStreamFetcherAutomatically(true);

		Broadcast deadNodes = row("dead-nodes", DEAD);
		row("nobodys", null);
		Broadcast stopped = row("stopped", null);
		stopped.setFetcherDesiredAction(Broadcast.FETCHER_DESIRED_ACTION_STOP);
		row("on-demand", null).setAutoStartStopEnabled(true);

		//let go after an attempt that started a moment ago, so it waits out the retry delay first
		Broadcast justFailed = row("just-failed", null);
		justFailed.setFetcherFailures(1);
		justFailed.setStartTime(System.currentTimeMillis());

		tick(node);
		assertEquals(Set.of("dead-nodes", "nobodys"), node.getStreamFetcherList().keySet());
		assertEquals(HOST, deadNodes.getFetcherNode());
		assertNull(stopped.getFetcherNode());

		justFailed.setStartTime(System.currentTimeMillis() - fixture.appSettings.getStreamFetcherRetryDelayMs() - 1);
		tick(node);
		assertTrue(node.getStreamFetcherList().containsKey("just-failed"));

		//with the setting off a failover brings nothing back, but a failed attempt is still tried again
		fixture.appSettings.setStartStreamFetcherAutomatically(false);
		Broadcast notFailedOver = row("not-failed-over", DEAD);
		row("retried", DEAD).setFetcherFailures(2);
		tick(node);
		assertEquals(DEAD, notFailedOver.getFetcherNode());
		assertTrue(node.getStreamFetcherList().containsKey("retried"));
	}

	@Test
	void aStopThatLandsBeforeTheTakeWins() throws Exception {
		ScriptedManager node = fixture.newManager();
		fixture.appSettings.setStartStreamFetcherAutomatically(true);
		Broadcast orphan = row("orphan", DEAD);

		//the leader reads it as wanted, and its user stops it right after
		Broadcast asRead = new Broadcast();
		asRead.setStreamId("orphan");
		asRead.setFetcherNode(DEAD);
		asRead.setFetcherDesiredAction(Broadcast.FETCHER_DESIRED_ACTION_START);
		//doAnswer, a when(...) here would run the fixture's own answer with a null node list
		doAnswer(call -> {
			orphan.setFetcherDesiredAction(Broadcast.FETCHER_DESIRED_ACTION_STOP);
			return List.of(asRead);
		}).when(fixture.dataStore).getOrphanStreamFetcherSources(any());

		tick(node);
		assertTrue(node.getStreamFetcherList().isEmpty());
		assertEquals(DEAD, orphan.getFetcherNode());
	}

	@Test
	void inAClusterOnlyTheLeaderTakesWhatNobodyHolds() {
		fixture.appSettings.setStartStreamFetcherAutomatically(true);
		//a lower address is alive, so placing orphans is that node's job, starting what it holds is this one's
		IClusterNotifier cluster = fixture.clusterMode("10.0.0.1", HOST, PEER);
		ScriptedManager node = fixture.newManager();

		row("mine", HOST);
		Broadcast orphan = row("orphan", DEAD);
		Broadcast peers = row("peers", PEER);
		tick(node);
		assertEquals(Set.of("mine"), node.getStreamFetcherList().keySet());
		assertEquals(DEAD, orphan.getFetcherNode());

		//the lower node is gone, so this one leads. What an alive peer holds is not an orphan
		aliveNodes(cluster, HOST, PEER);
		tick(node);
		assertEquals(Set.of("mine", "orphan"), node.getStreamFetcherList().keySet());
		assertEquals(PEER, peers.getFetcherNode());

		//no node in the list is alive, this one included: nobody leads, but what this node holds still runs
		aliveNodes(cluster);
		Broadcast later = row("later", DEAD);
		row("held-later", HOST);
		tick(node);
		assertEquals(DEAD, later.getFetcherNode());
		assertTrue(node.getStreamFetcherList().containsKey("held-later"));

		//a node list that cannot be read never holds up what this node holds
		when(cluster.getLeader()).thenThrow(new IllegalStateException("cluster store is down"));
		row("held-while-down", HOST);
		tick(node);
		assertTrue(node.getStreamFetcherList().containsKey("held-while-down"));
		assertTrue(claims.isEmpty());
	}

	@Test
	void aStartGoesToTheAliveNodeWithRoomThatHoldsTheFewest() {
		IClusterNotifier cluster = fixture.clusterMode(HOST, PEER, OTHER_PEER);
		ScriptedManager node = fixture.newManager();
		ClusterNode self = load(HOST, "10", "1000/8000");
		ClusterNode busy = load(OTHER_PEER, "90", "1000/8000");
		when(cluster.getAliveNodes()).thenReturn(List.of(self, load(PEER, "10", "1000/8000"), busy));

		//the busy peer holds the fewest but is over the cpu limit. This node and PEER hold the same, so this node goes first
		assertTrue(node.startStreaming(fixture.row("one", "fake://one")).isSuccess());
		assertEquals(HOST, fixture.rows.get("one").getFetcherNode());
		assertTrue(claims.isEmpty());

		assertTrue(node.startStreaming(fixture.row("two", "fake://two")).isSuccess());
		assertEquals(PEER, fixture.rows.get("two").getFetcherNode());
		assertEquals(List.of(new Claim(PEER, "two", null, true, true)), claims);

		//this node is over the memory limit and the other peer is back under its own: two peers hold one each, the lower address wins
		self.setMemory("7000/8000");
		busy.setCpu("10");
		row("held-by-other-peer", OTHER_PEER);
		assertTrue(node.startStreaming(fixture.row("three", "fake://three")).isSuccess());
		assertEquals(PEER, fixture.rows.get("three").getFetcherNode());
	}

	@Test
	void aNodeThatRefusesOrDoesNotAnswerIsSkippedAndALostClaimEndsTheStart() {
		fixture.clusterMode(HOST, PEER, OTHER_PEER);
		ScriptedManager node = fixture.newManager();
		//this node holds the most, so it is asked last
		row("a", HOST);
		row("b", HOST);
		row("c", OTHER_PEER);

		Result full = new Result(false, "fresh", "Not enough resource on server to start streaming.");
		full.setErrorId(StreamFetcherManager.START_REFUSED);
		answers.put(PEER, full);
		broken.add(OTHER_PEER);

		assertTrue(node.startStreaming(fixture.row("fresh", "fake://fresh")).isSuccess());
		assertEquals(List.of(PEER, OTHER_PEER), claims.stream().map(Claim::node).toList());
		assertEquals(HOST, fixture.rows.get("fresh").getFetcherNode());

		//another node claimed it first: nobody else is asked and the start says so
		claims.clear();
		answers.put(PEER, new Result(false, "taken", "Stream is already active. It's already streaming or trying to connect"));
		Result lost = node.startStreaming(fixture.row("taken", "fake://taken"));
		assertFalse(lost.isSuccess());
		assertTrue(lost.getMessage().contains("already active"));
		assertEquals(List.of(PEER), claims.stream().map(Claim::node).toList());
		assertNull(node.getStreamFetcher("taken"));
	}

	@Test
	void aSourceAnAliveNodeHoldsIsLeftToThatNode() {
		fixture.clusterMode(HOST, PEER);
		ScriptedManager node = fixture.newManager();

		//the start goes to its holder, which answers for itself
		Broadcast peers = row("peers", PEER);
		assertTrue(node.isStreamRunning(peers));
		answers.put(PEER, new Result(false, "peers", "Stream is already active. It's already streaming or trying to connect"));
		Result start = node.startStreaming(peers);
		assertFalse(start.isSuccess());
		assertTrue(start.getMessage().contains("already active"));
		assertEquals(List.of(new Claim(PEER, "peers", PEER, true, true)), claims);
		assertTrue(node.getStreamFetcherList().isEmpty());

		//the stop is written for the peer, its next tick stops it
		assertTrue(node.stopStreaming("peers", false).isSuccess());
		assertEquals(Broadcast.FETCHER_DESIRED_ACTION_STOP, peers.getFetcherDesiredAction());
		assertEquals(PEER, peers.getFetcherNode());

		//a node that is gone runs nothing, so its source is placed again
		Broadcast deadNodes = row("dead-nodes", DEAD);
		assertFalse(node.isStreamRunning(deadNodes));
		assertTrue(node.startStreaming(deadNodes).isSuccess());
		assertEquals(HOST, deadNodes.getFetcherNode());
	}

	@Test
	void theLeaderSpreadsWhatNobodyHolds() {
		fixture.appSettings.setStartStreamFetcherAutomatically(true);
		fixture.clusterMode(HOST, PEER);
		ScriptedManager node = fixture.newManager();
		for (int i = 0; i < 4; i++) {
			row("orphan-" + i, DEAD);
		}

		//placed at once, so the count has to go up as each one is chosen, not as it answers
		tick(node);
		assertEquals(2, node.getStreamFetcherList().size());
		assertEquals(2, fixture.rows.values().stream().filter(row -> PEER.equals(row.getFetcherNode())).count());
		assertTrue(claims.stream().allMatch(claim -> DEAD.equals(claim.expectedFetcherNode()) && !claim.userStart()));
	}

	@Test
	void whatANodeRefusesIsSpreadOverTheOthers() {
		fixture.appSettings.setStartStreamFetcherAutomatically(true);
		fixture.clusterMode(HOST, PEER, OTHER_PEER);
		ScriptedManager node = fixture.newManager();
		//the peer holds nothing because it is still starting: its heartbeat is up, its application is not
		broken.add(PEER);
		row("a", HOST);
		row("b", HOST);
		row("c", OTHER_PEER);
		row("d", OTHER_PEER);
		for (int i = 0; i < 4; i++) {
			row("orphan-" + i, DEAD);
		}

		//three go to the empty peer first. Each refusal sends its source back with fresh counts, not to one fixed second choice
		tick(node);
		assertEquals(4, fixture.rows.values().stream().filter(row -> HOST.equals(row.getFetcherNode())).count());
		assertEquals(4, fixture.rows.values().stream().filter(row -> OTHER_PEER.equals(row.getFetcherNode())).count());
		assertEquals(3, claims.stream().filter(claim -> PEER.equals(claim.node())).count(), "not asked again in the same tick");
	}

	@Test
	void theCoordinatorTicksUntilTheApplicationGoesDown() {
		StreamFetcherManager manager = new StreamFetcherManager(fixture.vertx, fixture.dataStore, fixture.scope);
		assertNotEquals(-1L, peek(manager.getCoordinator(), "timerId"), "it ticks on a timer");
		manager.shutdown();
		assertEquals(-1L, peek(manager.getCoordinator(), "timerId"), "the timer dies with the application");

		//a server on its way down takes nothing new
		ScriptedManager closing = fixture.newManager();
		fixture.appSettings.setStartStreamFetcherAutomatically(true);
		row("orphan", DEAD);
		row("held", HOST);
		closing.shuttingDown();
		tick(closing);
		assertTrue(closing.getStreamFetcherList().isEmpty());
	}

	/** A peer's side of claim-and-start: it takes the source unless told otherwise. */
	private void answerClaim(HttpExchange exchange) throws IOException {
		String node = exchange.getLocalAddress().getAddress().getHostAddress();
		//  /junit/rest/cluster-communication/stream-source/{id}/claim-and-start
		String streamId = exchange.getRequestURI().getPath().split("/")[5];
		String query = exchange.getRequestURI().getRawQuery();
		List<String> expected = UrlQueryUtils.getParamValues(query, "expectedFetcherNode");
		String token = exchange.getRequestHeaders().getFirst(TokenFilterManager.TOKEN_HEADER_FOR_NODE_COMMUNICATION);

		claims.add(new Claim(node, streamId, expected.isEmpty() ? null : expected.get(0),
				Boolean.parseBoolean(UrlQueryUtils.getParamValues(query, "userStart").get(0)),
				JWTFilter.isJWTTokenValid(fixture.appSettings.getClusterCommunicationKey(), token)));

		if (broken.contains(node)) {
			exchange.sendResponseHeaders(503, -1);
			exchange.close();
			return;
		}

		Result answer = answers.get(node);
		if (answer == null) {
			fixture.rows.get(streamId).setFetcherNode(node);
			answer = new Result(true, streamId, "Stream fetcher is started");
		}
		byte[] body = new Gson().toJson(answer).getBytes(StandardCharsets.UTF_8);
		exchange.sendResponseHeaders(200, body.length);
		try (OutputStream out = exchange.getResponseBody()) {
			out.write(body);
		}
	}

	/** An alive node with its last heartbeat numbers: system cpu percent and "usedMB/totalMB". */
	private static ClusterNode load(String ip, String cpu, String memory) {
		ClusterNode node = new ClusterNode(ip, "node-" + ip);
		node.setCpu(cpu);
		node.setMemory(memory);
		return node;
	}

	/** One tick. What it takes runs on the shared context, so this waits for that too. */
	private void tick(ScriptedManager node) {
		node.getCoordinator().tick();
		settle(node.context());
	}

	/** A wanted source, held by fetcherNode, or by nobody when it is null. */
	private Broadcast row(String streamId, String fetcherNode) {
		Broadcast row = fixture.row(streamId, "fake://" + streamId);
		row.setFetcherNode(fetcherNode);
		return row;
	}
}
