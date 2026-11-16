package io.antmedia.streamsource;

import static io.antmedia.streamsource.StreamSourceFixture.HOST;
import static io.antmedia.streamsource.StreamSourceFixture.aliveNodes;
import static io.antmedia.streamsource.StreamSourceFixture.awaitState;
import static io.antmedia.streamsource.StreamSourceFixture.peek;
import static io.antmedia.streamsource.StreamSourceFixture.poke;
import static io.antmedia.streamsource.StreamSourceFixture.settle;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.UrlQueryUtils;
import io.antmedia.cluster.ClusterNode;
import io.antmedia.cluster.IClusterNotifier;
import io.antmedia.datastore.db.DataStore;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.filter.JWTFilter;
import io.antmedia.filter.TokenFilterManager;
import io.antmedia.muxer.IAntMediaStreamHandler;
import io.antmedia.rest.model.Result;
import io.antmedia.streamsource.StreamFetcher.State;
import io.antmedia.streamsource.StreamSourceFixture.ScriptedFetcher;
import io.antmedia.streamsource.StreamSourceFixture.ScriptedManager;

/**
 * Where the stream sources run: a start goes to the alive holder or the best node, every node starts or lets
 * go of what it holds, and the leader places what nobody holds. Every tick is run by hand.
 */
class StreamSourceCoordinatorTest {

	private static final String PEER = "127.0.0.5";

	private static final String OTHER_PEER = "127.0.0.6";

	private static final String THIRD_PEER = "127.0.0.7";

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
		Broadcast heldByPeer = row("peers", PEER);
		tick(node);
		assertEquals(Set.of("mine"), node.getStreamFetcherList().keySet());
		assertEquals(DEAD, orphan.getFetcherNode());

		//the lower node is gone, so this one leads. What an alive peer holds is not an orphan
		aliveNodes(cluster, HOST, PEER);
		tick(node);
		assertEquals(Set.of("mine", "orphan"), node.getStreamFetcherList().keySet());
		assertEquals(PEER, heldByPeer.getFetcherNode());

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
		Broadcast heldByPeer = row("peers", PEER);
		assertTrue(node.isStreamRunning(heldByPeer));
		answers.put(PEER, new Result(false, "peers", "Stream is already active. It's already streaming or trying to connect"));
		Result start = node.startStreaming(heldByPeer);
		assertFalse(start.isSuccess());
		assertTrue(start.getMessage().contains("already active"));
		assertEquals(List.of(new Claim(PEER, "peers", PEER, true, true)), claims);
		assertTrue(node.getStreamFetcherList().isEmpty());

		//the stop is written for the peer, its next tick stops it
		assertTrue(node.stopStreaming("peers", false).isSuccess());
		assertEquals(Broadcast.FETCHER_DESIRED_ACTION_STOP, heldByPeer.getFetcherDesiredAction());
		assertEquals(PEER, heldByPeer.getFetcherNode());

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
	void aNodeThatDoesNotFetchIsNeverPlacedOnWhileAnotherCan() {
		fixture.appSettings.setStartStreamFetcherAutomatically(true);
		IClusterNotifier cluster = fixture.clusterMode(HOST, PEER, OTHER_PEER);
		//this node is switched off but its heartbeat is from before that restart, the other peer's from before the setting existed
		when(fixture.serverSettings.isStreamSourceFetchEnabled()).thenReturn(false);
		when(cluster.getAliveNodes()).thenReturn(List.of(heartbeat(HOST, true), heartbeat(PEER, false), heartbeat(OTHER_PEER, null)));
		ScriptedManager node = fixture.newManager();

		//nobody holds anything, so by the usual order this node would go first and PEER next
		assertTrue(node.startStreaming(fixture.row("one", "fake://one")).isSuccess());
		assertEquals(OTHER_PEER, fixture.rows.get("one").getFetcherNode());

		//this node leads and places them all on the one node that fetches, however many it already holds
		for (int i = 0; i < 3; i++) {
			row("orphan-" + i, DEAD);
		}
		tick(node);
		assertEquals(4, fixture.rows.values().stream().filter(row -> OTHER_PEER.equals(row.getFetcherNode())).count());
		assertTrue(node.getStreamFetcherList().isEmpty());
		assertTrue(claims.stream().allMatch(claim -> OTHER_PEER.equals(claim.node())), "a node that does not fetch is never asked");
	}

	@Test
	void whenNoNodeFetchesTheOthersTakeItAsALastResort() {
		IClusterNotifier cluster = fixture.clusterMode(HOST, PEER, OTHER_PEER);
		when(fixture.serverSettings.isStreamSourceFetchEnabled()).thenReturn(false);
		ClusterNode otherPeer = heartbeat(OTHER_PEER, false);
		when(cluster.getAliveNodes()).thenReturn(List.of(heartbeat(HOST, false), heartbeat(PEER, false), otherPeer));
		ScriptedManager node = fixture.newManager();
		row("a", PEER);
		row("b", OTHER_PEER);

		//the usual order among them all, so this node, holding the fewest, takes it
		assertTrue(node.startStreaming(fixture.row("one", "fake://one")).isSuccess());
		assertEquals(HOST, fixture.rows.get("one").getFetcherNode());
		assertTrue(claims.isEmpty());

		//a node that fetches is back: it gets the next one, though all hold one and this node would go first
		otherPeer.setStreamSourceFetchEnabled(true);
		assertTrue(node.startStreaming(fixture.row("two", "fake://two")).isSuccess());
		assertEquals(OTHER_PEER, fixture.rows.get("two").getFetcherNode());
		assertEquals(List.of(new Claim(OTHER_PEER, "two", null, true, true)), claims);
	}

	@Test
	void aFullOriginKeepsTheEdgesOut() {
		fixture.appSettings.setStartStreamFetcherAutomatically(true);
		IClusterNotifier cluster = fixture.clusterMode(HOST, PEER, OTHER_PEER);
		when(fixture.serverSettings.isStreamSourceFetchEnabled()).thenReturn(false);
		when(cluster.getAliveNodes()).thenReturn(List.of(heartbeat(HOST, false), heartbeat(PEER, false), load(OTHER_PEER, "90", "1000/8000")));
		ScriptedManager node = fixture.newManager();

		Result start = node.startStreaming(fixture.row("one", "fake://one"));
		assertEquals(StreamFetcherManager.START_REFUSED, start.getErrorId());
		assertEquals("No node can take the stream now", start.getMessage());

		Broadcast orphan = row("orphan", DEAD);
		tick(node);
		assertNull(fixture.rows.get("one").getFetcherNode());
		assertEquals(DEAD, orphan.getFetcherNode());
		assertTrue(node.getStreamFetcherList().isEmpty());
		assertTrue(claims.isEmpty());

		//only once the origin is dead do the edges take over, one each
		when(cluster.getAliveNodes()).thenReturn(List.of(heartbeat(HOST, false), heartbeat(PEER, false)));
		tick(node);
		assertEquals(1, node.getStreamFetcherList().size());
		assertEquals(1, fixture.rows.values().stream().filter(row -> PEER.equals(row.getFetcherNode())).count());
	}

	@Test
	void aSourceAnOptOutNodeHoldsMovesToAnOrigin() {
		IClusterNotifier cluster = fixture.clusterMode(HOST, PEER);
		when(cluster.getAliveNodes()).thenReturn(List.of(heartbeat(HOST, true), heartbeat(PEER, false)));
		ScriptedManager node = fixture.newManager();

		//an edge took it as the last resort while the origins were down. This node claims it over the alive edge,
		//whose own tick then stops it
		Broadcast heldByEdge = row("edges", PEER);
		assertTrue(node.startStreaming(heldByEdge).isSuccess());
		assertEquals(HOST, heldByEdge.getFetcherNode());
		assertNotNull(node.getStreamFetcher("edges"));
		assertTrue(claims.isEmpty(), "the edge is not asked");
	}

	@Test
	void aLastResortSourceIsHandedBackWhenAnOriginReturns() {
		IClusterNotifier cluster = fixture.clusterMode(HOST, PEER);
		when(fixture.serverSettings.isStreamSourceFetchEnabled()).thenReturn(false);
		when(cluster.getAliveNodes()).thenReturn(List.of(heartbeat(HOST, false)));
		ScriptedManager edge = fixture.newManager();

		//the origin is dead, so this edge runs both as the last resort
		assertTrue(edge.startStreaming(fixture.row("cam-1", "fake://cam-1")).isSuccess());
		assertTrue(edge.startStreaming(fixture.row("cam-2", "fake://cam-2")).isSuccess());

		//back but full, nothing moves
		ClusterNode origin = load(PEER, "90", "1000/8000");
		when(cluster.getAliveNodes()).thenReturn(List.of(heartbeat(HOST, false), origin));
		tick(edge);
		assertTrue(claims.isEmpty());

		//room by its heartbeat but its application is still starting: asked once, the other one waits for the next tick
		origin.setCpu("10");
		broken.add(PEER);
		tick(edge);
		assertEquals(1, claims.size());
		assertEquals(Set.of("cam-1", "cam-2"), edge.getStreamFetcherList().keySet());

		//ready: it claims both over the edge, which runs them until its own fetcher ticks
		broken.clear();
		claims.clear();
		tick(edge);
		assertEquals(Set.of(new Claim(PEER, "cam-1", HOST, false, true), new Claim(PEER, "cam-2", HOST, false, true)), Set.copyOf(claims));
		assertEquals(2, edge.getStreamFetcherList().size());

		//the edge stops them without writing over the origin's rows, and does not offer them again
		for (ScriptedFetcher fetcher : edge.fetchers) {
			StreamSourceFixture.tick(fetcher);
			fetcher.releaseAll();
			awaitState(fetcher, State.STOPPED);
			Broadcast row = fixture.rows.get(fetcher.getStreamId());
			assertEquals(PEER, row.getFetcherNode());
			assertEquals(IAntMediaStreamHandler.BROADCAST_STATUS_PREPARING, row.getStatus(), "no end write over the origin's row");
		}
		assertTrue(edge.getStreamFetcherList().isEmpty());
		claims.clear();
		tick(edge);
		assertTrue(claims.isEmpty());
	}

	@Test
	void aSourceOfANodeThatStopsAnsweringMovesAndTheOldHolderStepsAside() {
		fixture.appSettings.setStartStreamFetcherAutomatically(true);
		IClusterNotifier cluster = fixture.clusterMode(HOST, PEER);
		//two real nodes on one database, a manager takes its host when it is made
		when(fixture.serverSettings.getHostAddress()).thenReturn(PEER);
		ScriptedManager frozen = fixture.newManager();
		when(fixture.serverSettings.getHostAddress()).thenReturn(HOST);
		ScriptedManager leader = fixture.newManager();

		frozen.prepare = fetcher -> fetcher.script(FakeWorker.publishing());
		Broadcast row = fixture.row("cam", "fake://cam");
		assertTrue(frozen.startStreaming(row).isSuccess());
		ScriptedFetcher stale = frozen.fetchers.get(0);
		awaitState(stale, State.STREAMING);
		assertEquals(PEER, row.getFetcherNode());
		//on air for a while, so the retry delay does not hold the failover back
		row.setStartTime(System.currentTimeMillis() - 60000);

		//its heartbeat stops and it is not ticked meanwhile, the leader takes the source over it
		aliveNodes(cluster, HOST);
		tick(leader);
		assertEquals(HOST, row.getFetcherNode());
		assertNotNull(leader.getStreamFetcher("cam"));
		assertSame(stale, frozen.getStreamFetcher("cam"), "a frozen node does not know yet");

		//back: its first tick stops it without a trace, the leader's row and run are untouched
		aliveNodes(cluster, HOST, PEER);
		StreamSourceFixture.tick(stale);
		stale.releaseAll();
		awaitState(stale, State.STOPPED);
		tick(frozen);
		assertTrue(frozen.getStreamFetcherList().isEmpty());
		assertEquals(HOST, row.getFetcherNode());
		assertEquals(IAntMediaStreamHandler.BROADCAST_STATUS_PREPARING, row.getStatus(), "no end write over the leader's row");
		assertNotNull(leader.getStreamFetcher("cam"));
		assertTrue(claims.isEmpty());
	}

	@Test
	void aSourceStartsOnlyOnItsPreferredNodeWhileThatNodeIsAlive() {
		fixture.appSettings.setStartStreamFetcherAutomatically(true);
		IClusterNotifier cluster = fixture.clusterMode(HOST, PEER, OTHER_PEER);
		ScriptedManager node = fixture.newManager();
		//by the usual order this node would go first and PEER, holding the most, last
		row("a", PEER);
		row("b", PEER);

		Broadcast pinned = fixture.row("pinned", "fake://pinned");
		pinned.setPreferredFetcherNode(PEER);
		assertTrue(node.startStreaming(pinned).isSuccess());
		assertEquals(PEER, pinned.getFetcherNode());

		//full by its heartbeat and refusing: it is still the only node asked, and the start answers its refusal
		when(cluster.getAliveNodes()).thenReturn(List.of(heartbeat(HOST, null), load(PEER, "90", "1000/8000"), heartbeat(OTHER_PEER, null)));
		Result full = new Result(false, "second", "Not enough resource on server to start streaming.");
		full.setErrorId(StreamFetcherManager.START_REFUSED);
		answers.put(PEER, full);
		Broadcast second = fixture.row("second", "fake://second");
		second.setPreferredFetcherNode(PEER);
		claims.clear();
		Result refused = node.startStreaming(second);
		assertEquals(StreamFetcherManager.START_REFUSED, refused.getErrorId());
		assertEquals("Not enough resource on server to start streaming.", refused.getMessage());
		assertEquals(List.of(PEER), claims.stream().map(Claim::node).toList());
		assertNull(second.getFetcherNode());

		//the leader does not give its sources to another node either, they wait for it
		Broadcast orphan = row("orphan", DEAD);
		orphan.setPreferredFetcherNode(PEER);
		claims.clear();
		tick(node);
		assertEquals(Set.of("second", "orphan"), claims.stream().map(Claim::streamId).collect(Collectors.toSet()));
		assertTrue(claims.stream().allMatch(claim -> PEER.equals(claim.node())), "asked once each this tick, nobody else");
		assertEquals(DEAD, orphan.getFetcherNode());
		assertNull(second.getFetcherNode());
		assertTrue(node.getStreamFetcherList().isEmpty());
	}

	@Test
	void aSourceFailsOverFromItsPreferredNodeOnlyWhileItIsDead() {
		fixture.appSettings.setStartStreamFetcherAutomatically(true);
		IClusterNotifier cluster = fixture.clusterMode(HOST, PEER);
		ScriptedManager node = fixture.newManager();

		//its preferred node went down while running it: the leader starts it on another node
		Broadcast failedOver = row("failed-over", PEER);
		failedOver.setPreferredFetcherNode(PEER);
		aliveNodes(cluster, HOST);
		tick(node);
		assertEquals(HOST, failedOver.getFetcherNode());

		//back again, it does not take over what runs elsewhere now, a start goes to where it runs
		aliveNodes(cluster, HOST, PEER);
		tick(node);
		assertFalse(node.startStreaming(failedOver).isSuccess(), "already active here");
		assertEquals(HOST, failedOver.getFetcherNode());
		assertTrue(claims.isEmpty());

		//an address no alive node has, or an empty one, is no preference: the usual order
		Broadcast unknown = fixture.row("unknown", "fake://unknown");
		unknown.setPreferredFetcherNode(DEAD);
		assertTrue(node.startStreaming(unknown).isSuccess());
		assertEquals(PEER, unknown.getFetcherNode());
		Broadcast empty = fixture.row("empty", "fake://empty");
		empty.setPreferredFetcherNode("");
		assertTrue(node.startStreaming(empty).isSuccess());
		assertEquals(HOST, empty.getFetcherNode());

		//a preferred node that does not fetch is passed over while one that does is alive
		when(cluster.getAliveNodes()).thenReturn(List.of(heartbeat(HOST, null), heartbeat(PEER, false)));
		Broadcast onAnEdge = fixture.row("on-an-edge", "fake://on-an-edge");
		onAnEdge.setPreferredFetcherNode(PEER);
		claims.clear();
		assertTrue(node.startStreaming(onAnEdge).isSuccess());
		assertEquals(HOST, onAnEdge.getFetcherNode());
		assertTrue(claims.isEmpty());
	}

	@Test
	void aSourcePinnedToARefusingOriginDoesNotHoldBackTheHandBackOfTheOthers() {
		IClusterNotifier cluster = fixture.clusterMode(HOST, PEER, OTHER_PEER);
		when(fixture.serverSettings.isStreamSourceFetchEnabled()).thenReturn(false);
		when(cluster.getAliveNodes()).thenReturn(List.of(heartbeat(HOST, false)));
		ScriptedManager edge = fixture.newManager();

		//both origins were dead, so this edge runs both as the last resort
		Broadcast pinned = fixture.row("pinned", "fake://pinned");
		pinned.setPreferredFetcherNode(PEER);
		assertTrue(edge.startStreaming(pinned).isSuccess());
		Broadcast free = fixture.row("free", "fake://free");
		assertTrue(edge.startStreaming(free).isSuccess());

		//back, but the preferred origin of the first one refuses: whichever is offered first, the other one still
		//moves, to the origin that takes it, and the refusing one is asked once
		Result full = new Result(false, "pinned", "Not enough resource on server to start streaming.");
		full.setErrorId(StreamFetcherManager.START_REFUSED);
		answers.put(PEER, full);
		when(cluster.getAliveNodes()).thenReturn(List.of(heartbeat(HOST, false), heartbeat(PEER, null), heartbeat(OTHER_PEER, null)));
		tick(edge);
		assertEquals(OTHER_PEER, free.getFetcherNode());
		assertEquals(HOST, pinned.getFetcherNode(), "it keeps running here until its origin takes it");
		assertEquals(1, claims.stream().filter(claim -> PEER.equals(claim.node())).count());
	}

	@Test
	void aStartCountsTheSourcesOfEveryApplication() {
		fixture.appSettings.setStartStreamFetcherAutomatically(true);
		fixture.clusterMode(HOST, PEER);
		ScriptedManager node = fixture.newManager();
		//this node holds three sources of another application, the peer one of this one
		DataStore otherApplicationStore = mock(DataStore.class);
		when(otherApplicationStore.getHeldStreamFetcherSourceCounts()).thenReturn(Map.of(HOST, 3L));
		fixture.application("other", otherApplicationStore);
		row("peers", PEER);

		assertTrue(node.startStreaming(fixture.row("one", "fake://one")).isSuccess());
		assertEquals(PEER, fixture.rows.get("one").getFetcherNode());

		//the leader counts the same way: two on the peer, three here
		Broadcast orphan = row("orphan", DEAD);
		tick(node);
		assertEquals(PEER, orphan.getFetcherNode());

		//an application whose database is down is left out of the count, it does not stop the starts of the others
		when(otherApplicationStore.getHeldStreamFetcherSourceCounts()).thenThrow(new IllegalStateException("redis is down"));
		assertTrue(node.startStreaming(fixture.row("two", "fake://two")).isSuccess());
		assertEquals(HOST, fixture.rows.get("two").getFetcherNode());
	}

	@Test
	void startsThatRunAtOnceSpreadOverTheNodes() {
		fixture.clusterMode(HOST, PEER, OTHER_PEER);
		ScriptedManager node = fixture.newManager();

		//each start reads the counts before the others are claimed, the claims in flight are what spreads them
		List<CompletableFuture<Result>> starts = new ArrayList<>();
		for (int i = 0; i < 6; i++) {
			Broadcast row = fixture.row("burst-" + i, "fake://burst-" + i);
			starts.add(CompletableFuture.supplyAsync(() -> node.startStreaming(row), fixture.pool));
		}
		starts.forEach(start -> assertTrue(start.join().isSuccess()));
		assertEquals(List.of(2L, 2L, 2L), List.of(held(HOST), held(PEER), held(OTHER_PEER)));
	}

	@Test
	void theLeaderMovesRunningSourcesInBatchesUntilTheNodesAreBalanced() {
		rebalance(3);
		fixture.clusterMode(HOST, PEER, OTHER_PEER);
		ScriptedManager node = fixture.newManager();
		//this node is back after a failover, the peers run all of its sources
		for (int i = 0; i < 15; i++) {
			onAir("peers-" + i, PEER);
			onAir("other-peers-" + i, OTHER_PEER);
		}

		//the first tick sees the nodes, a round waits for them to settle after they change
		tick(node);
		assertEquals(0, held(HOST));
		assertNextRoundIn(node, StreamSourceRebalancer.SETTLE_MS);

		//this node is the only one that needs sources, it takes three a round from the node that holds the most, and
		//the next round comes on the next tick
		rebalanceTick(node);
		assertEquals(List.of(3L, 13L, 14L), List.of(held(HOST), held(PEER), held(OTHER_PEER)));
		assertNextRoundIn(node, 0);
		rebalanceTick(node);
		assertEquals(List.of(6L, 12L, 12L), List.of(held(HOST), held(PEER), held(OTHER_PEER)));
		rebalanceTick(node);
		assertEquals(List.of(9L, 10L, 11L), List.of(held(HOST), held(PEER), held(OTHER_PEER)));

		//9 of 11 is 82%, over the target: nothing moves and the next look is a period away
		rebalanceTick(node);
		assertEquals(List.of(9L, 10L, 11L), List.of(held(HOST), held(PEER), held(OTHER_PEER)));
		assertNextRoundIn(node, 60000);
		assertEquals(9, node.getStreamFetcherList().size());
		assertTrue(claims.isEmpty(), "every move came to this node, no peer was asked");
	}

	@Test
	void everyNodeThatNeedsSourcesTakesABatchInTheSameRound() {
		rebalance(2);
		fixture.clusterMode(HOST, PEER, OTHER_PEER, THIRD_PEER);
		ScriptedManager node = fixture.newManager();
		//the peer runs everything, this node and the two other peers are empty
		for (int i = 0; i < 10; i++) {
			onAir("peers-" + i, PEER);
		}
		tick(node);

		//two to each empty node at once
		rebalanceTick(node);
		assertEquals(List.of(2L, 4L, 2L, 2L), List.of(held(HOST), held(PEER), held(OTHER_PEER), held(THIRD_PEER)));
		assertEquals(4, claims.size());
		assertNextRoundIn(node, 0);

		//one more from the peer, then 3, 3, 2, 2 is as even as ten sources get on four nodes
		rebalanceTick(node);
		assertEquals(List.of(3L, 3L, 2L, 2L), List.of(held(HOST), held(PEER), held(OTHER_PEER), held(THIRD_PEER)));
		rebalanceTick(node);
		assertEquals(List.of(3L, 3L, 2L, 2L), List.of(held(HOST), held(PEER), held(OTHER_PEER), held(THIRD_PEER)));
		assertNextRoundIn(node, 60000);
	}

	@Test
	void onlySourcesOnAirOfApplicationsThatAllowItMoveTheLeastWatchedFirst() {
		rebalance(1);
		fixture.appSettings.setStreamFetcherRebalanceEnabled(false);
		fixture.clusterMode(HOST, PEER);
		ScriptedManager node = fixture.newManager();
		//another application with rebalance off: its sources on the peer count, but never move
		DataStore otherApplicationStore = mock(DataStore.class);
		when(otherApplicationStore.getHeldStreamFetcherSourceCounts()).thenReturn(Map.of(PEER, 2L));
		AntMediaApplicationAdapter otherApplication = fixture.application("other", otherApplicationStore);

		//two WebRTC viewers, and an HLS count below zero from an earlier move, which must not make it look unwatched
		Broadcast watched = onAir("watched", PEER);
		watched.setWebRTCViewerCount(2);
		watched.setHlsViewerCount(-3);
		Broadcast unwatched = onAir("unwatched", PEER);
		onAir("pinned", PEER).setPreferredFetcherNode(PEER);
		row("connecting", PEER).setStatus(IAntMediaStreamHandler.BROADCAST_STATUS_PREPARING);
		Broadcast onDemand = onAir("on-demand", PEER);
		onDemand.setAutoStartStopEnabled(true);
		onDemand.setFetcherDesiredAction(Broadcast.FETCHER_DESIRED_ACTION_START);
		Broadcast vod = onAir("vod", PEER);
		vod.setType(AntMediaApplicationAdapter.VOD);
		vod.setFetcherDesiredAction(Broadcast.FETCHER_DESIRED_ACTION_START);
		tick(node);

		//off for this application too
		rebalanceTick(node);
		assertEquals(0, held(HOST));

		fixture.appSettings.setStreamFetcherRebalanceEnabled(true);
		rebalanceTick(node);
		assertEquals(HOST, unwatched.getFetcherNode());
		assertEquals(PEER, watched.getFetcherNode());
		rebalanceTick(node);
		assertEquals(HOST, watched.getFetcherNode());

		//two here against six on the peer, but nothing left there may move
		rebalanceTick(node);
		assertEquals(2, held(HOST));
		assertEquals(Set.of("pinned", "connecting", "on-demand", "vod"), fixture.rows.values().stream()
				.filter(row -> PEER.equals(row.getFetcherNode())).map(Broadcast::getStreamId).collect(Collectors.toSet()));
		assertNextRoundIn(node, 60000);
		verify(otherApplication, never()).getStreamFetcherManager();
		verify(otherApplicationStore, never()).getHeldStreamFetcherSources(any());
	}

	@Test
	void sourcesMoveOnlyToNodesWithRoomAndNeverBackAndForth() {
		rebalance(3);
		IClusterNotifier cluster = fixture.clusterMode(HOST, PEER, OTHER_PEER);
		//this node is over its cpu limit: it holds the fewest but takes nothing
		when(cluster.getAliveNodes()).thenReturn(List.of(load(HOST, "90", "1000/8000"), load(PEER, "10", "1000/8000"), load(OTHER_PEER, "10", "1000/8000")));
		ScriptedManager node = fixture.newManager();
		for (int i = 0; i < 7; i++) {
			onAir("peers-" + i, PEER);
		}
		tick(node);

		//the other peer is still starting its applications: asked once, the source stays and the round ends
		Result starting = new Result(false, "peers-0", "Not enough resource on server to start streaming.");
		starting.setErrorId(StreamFetcherManager.START_REFUSED);
		answers.put(OTHER_PEER, starting);
		rebalanceTick(node);
		assertEquals(7, held(PEER));
		assertEquals(1, claims.size());
		assertNextRoundIn(node, 60000);

		answers.remove(OTHER_PEER);
		rebalanceTick(node);
		assertEquals(4, held(PEER));
		assertEquals(3, held(OTHER_PEER));
		assertTrue(claims.stream().allMatch(claim -> OTHER_PEER.equals(claim.node()) && PEER.equals(claim.expectedFetcherNode()) && !claim.userStart()));

		//four against three is as close as they get. The empty full node keeps the factor at 0, still nothing moves
		rebalanceTick(node);
		rebalanceTick(node);
		assertEquals(4, held(PEER));
		assertEquals(3, held(OTHER_PEER));
		assertEquals(0, held(HOST));
		assertEquals(4, claims.size());
	}

	@Test
	void theLeaderRebalancesOnceTheNodesHaveSettledAndNeverOntoAnEdge() {
		rebalance(3);
		IClusterNotifier cluster = fixture.clusterMode(HOST, PEER);
		ScriptedManager node = fixture.newManager();
		for (int i = 0; i < 6; i++) {
			onAir("peers-" + i, PEER);
		}
		tick(node);

		//another node joined: the nodes settle first, even with a round due
		aliveNodes(cluster, HOST, PEER, OTHER_PEER);
		rebalanceTick(node);
		assertEquals(6, held(PEER));
		assertNextRoundIn(node, StreamSourceRebalancer.SETTLE_MS);

		//a node with a lower address leads, it balances the cluster, not this one
		aliveNodes(cluster, "10.0.0.1", HOST, PEER, OTHER_PEER);
		rebalanceTick(node);
		rebalanceTick(node);
		assertEquals(6, held(PEER));

		//this node leads again, and the other peer is an edge: it gets nothing while an origin is alive
		aliveNodes(cluster, HOST, PEER, OTHER_PEER);
		when(cluster.getAliveNodes()).thenReturn(List.of(heartbeat(HOST, true), heartbeat(PEER, true), heartbeat(OTHER_PEER, false)));
		tick(node);
		rebalanceTick(node);
		assertEquals(List.of(3L, 3L, 0L), List.of(held(HOST), held(PEER), held(OTHER_PEER)));
		assertTrue(claims.isEmpty());

		//an application that sorts first decides on this node, this one stays out of it
		fixture.application("a-first", mock(DataStore.class));
		for (int i = 0; i < 4; i++) {
			onAir("more-" + i, PEER);
		}
		rebalanceTick(node);
		assertEquals(7, held(PEER));
	}

	@Test
	void aRoundIsNeverDueEarly() {
		rebalance(3);
		fixture.appSettings.setStreamFetcherRebalanceEnabled(false);
		when(fixture.serverSettings.getStreamSourceRebalancePeriodMs()).thenReturn(Long.MAX_VALUE);
		fixture.clusterMode(HOST, PEER);
		ScriptedManager node = fixture.newManager();
		for (int i = 0; i < 4; i++) {
			onAir("peers-" + i, PEER);
		}
		tick(node);

		//a round with nothing to do waits a period too long to add to the clock, which never comes round
		rebalanceTick(node);
		fixture.appSettings.setStreamFetcherRebalanceEnabled(true);
		tick(node);
		assertEquals(4, held(PEER));

		//a round that fails half way waits a full period, not one tick
		when(fixture.serverSettings.getStreamSourceRebalancePeriodMs()).thenReturn(60000L);
		doThrow(new IllegalStateException("the database is down")).when(fixture.dataStore).getHeldStreamFetcherSources(PEER);
		rebalanceTick(node);
		assertEquals(4, held(PEER));
		assertNextRoundIn(node, 60000);
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

	/** An alive node by its heartbeat. Null streamSourceFetchEnabled is a row written before the setting existed. */
	private static ClusterNode heartbeat(String ip, Boolean streamSourceFetchEnabled) {
		ClusterNode node = new ClusterNode(ip, "node-" + ip);
		node.setStreamSourceFetchEnabled(streamSourceFetchEnabled);
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

	/** A wanted source its node runs on air. */
	private Broadcast onAir(String streamId, String fetcherNode) {
		Broadcast row = row(streamId, fetcherNode);
		row.setStatus(IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING);
		return row;
	}

	/** The sources of this application the node holds. */
	private long held(String node) {
		return fixture.rows.values().stream().filter(row -> node.equals(row.getFetcherNode())).count();
	}

	/** The default server rebalance settings but the batch size, and rebalance on for this application. */
	private void rebalance(int batchSize) {
		when(fixture.serverSettings.getStreamSourceRebalancePeriodMs()).thenReturn(60000L);
		when(fixture.serverSettings.getStreamSourceRebalanceBatchSize()).thenReturn(batchSize);
		when(fixture.serverSettings.getStreamSourceBalanceFactorTarget()).thenReturn(80);
		fixture.appSettings.setStreamFetcherRebalanceEnabled(true);
	}

	/** One tick with the next rebalance round due, instead of waiting for it. */
	private void rebalanceTick(ScriptedManager node) {
		poke(peek(node.getCoordinator(), "rebalancer"), "waitMs", 0L);
		tick(node);
	}

	/** The last tick started a wait of this long for the next round. */
	private void assertNextRoundIn(ScriptedManager node, long ms) {
		assertEquals(ms, (long) peek(peek(node.getCoordinator(), "rebalancer"), "waitMs"));
	}
}
