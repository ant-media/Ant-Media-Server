package io.antmedia.streamsource;

import static io.antmedia.streamsource.StreamSourceFixture.NO_RETRY_IN_THIS_TEST_MS;
import static io.antmedia.streamsource.StreamSourceFixture.peek;
import static io.antmedia.streamsource.StreamSourceFixture.settle;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.red5.server.api.IContext;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.cluster.ClusterNode;
import io.antmedia.cluster.IClusterNotifier;
import io.antmedia.cluster.IClusterStore;
import io.antmedia.datastore.db.DataStore;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.muxer.IAntMediaStreamHandler;
import io.antmedia.streamsource.StreamSourceFixture.ScriptedManager;

/**
 * Cluster failover: the lowest live node assigns the sources nobody else will start to the live node with
 * the fewest streams, and every node starts the stale sources it owns. Only rows whose owner really cannot
 * start them qualify, and every tick looks again.
 */
class StreamSourceClusterCoordinatorTest {

	/** This node, see the fixture's server settings. */
	private static final String HOST = "127.0.0.1";
	private static final String PEER = "127.0.0.5";
	private static final String OTHER_PEER = "127.0.0.7";

	/** A node that left the cluster. */
	private static final String DEPARTED = "192.168.1.9";

	private StreamSourceFixture fixture;

	private final IClusterNotifier clusterNotifier = mock(IClusterNotifier.class);
	private final IClusterStore clusterStore = mock(IClusterStore.class);

	/** Origins that answer the http reachability check, any other origin is gone. */
	private final Set<String> answering = ConcurrentHashMap.newKeySet();

	/** Runs while the reachability check of that origin is in flight. */
	private final Map<String, Runnable> whileAsking = new ConcurrentHashMap<>();

	private ScriptedManager cluster;
	private StreamSourceClusterCoordinator coordinator;

	@BeforeEach
	void before() {
		fixture = new StreamSourceFixture();
		fixture.appSettings.setStreamFetcherRetryDelayMs(NO_RETRY_IN_THIS_TEST_MS);
	}

	@AfterEach
	void after() {
		fixture.close();
	}

	@Test
	void theOrphansOfADeadNodeAreSpreadOverTheLeastLoadedNodes() {
		fixture.appSettings.setStartStreamFetcherAutomatically(true);
		startClusterMode();
		clusterNodes(alive(HOST), alive(PEER), alive(OTHER_PEER), dead(DEPARTED));

		when(fixture.dataStore.getLocalLiveBroadcastCount(HOST)).thenReturn(2L);
		//given to the peer on an earlier tick and not started yet, so it is load the peer is about to take
		assigned("assigned-earlier", PEER);

		for (int i = 0; i < 4; i++) {
			orphan("orphan-" + i, AntMediaApplicationAdapter.STREAM_SOURCE, DEPARTED);
		}

		try (MockedStatic<AntMediaApplicationAdapter> statics = originsAnswerFromTheTest()) {
			tick();

			//2 1 0 at the start: the other peer, the peer on the tie, the other peer, then all three are at 2 and the lowest wins
			assertEquals(Map.of(PEER, 2L, OTHER_PEER, 2L, HOST, 1L),
					fixture.rows.values().stream().collect(Collectors.groupingBy(Broadcast::getOriginAdress, Collectors.counting())));

			//what this node got starts in the same tick, the peers' rows stay stale until their own ticks take them
			Set<String> mine = fixture.rows.values().stream().filter(row -> HOST.equals(row.getOriginAdress())).map(Broadcast::getStreamId).collect(Collectors.toSet());
			assertEquals(mine, cluster.getStreamFetcherList().keySet());
			assertTrue(fixture.rows.values().stream().filter(row -> !HOST.equals(row.getOriginAdress())).allMatch(DataStore::isStaleStreamSource));

			//rows a live node got a moment ago are its own to start, the next tick leaves them alone
			Map<String, String> owners = fixture.rows.values().stream().collect(Collectors.toMap(Broadcast::getStreamId, Broadcast::getOriginAdress));
			tick();
			assertEquals(owners, fixture.rows.values().stream().collect(Collectors.toMap(Broadcast::getStreamId, Broadcast::getOriginAdress)));
		}

		cluster.shutdown();
	}

	@Test
	void rowsNobodyElseWillStartAreGivenToAnotherNode() {
		fixture.appSettings.setStartStreamFetcherAutomatically(true);
		startClusterMode();
		clusterNodes(alive(HOST), alive(PEER), dead(DEPARTED));

		//loaded, so everything below lands on this node, which starts it in the same tick
		when(fixture.dataStore.getLocalLiveBroadcastCount(PEER)).thenReturn(5L);

		//the peer is live but has not taken it in time: still booting, its starts are refused, or the app does not run there
		Broadcast stuck = assigned("stuck", PEER);
		stuck.setUpdateTime(System.currentTimeMillis() - StreamSourceClusterCoordinator.OWNER_START_TIMEOUT_MS - 1000);

		orphan("orphan", AntMediaApplicationAdapter.STREAM_SOURCE, DEPARTED);
		orphan("camera", AntMediaApplicationAdapter.IP_CAMERA, DEPARTED);
		orphan("on-demand", AntMediaApplicationAdapter.STREAM_SOURCE, DEPARTED).setAutoStartStopEnabled(true);
		orphan("stopped", AntMediaApplicationAdapter.STREAM_SOURCE, DEPARTED).setStatus(IAntMediaStreamHandler.BROADCAST_STATUS_FINISHED);

		//no owner to ask, so a row without one waits as long as a live owner gets
		orphan("no-owner-yet", AntMediaApplicationAdapter.STREAM_SOURCE, "");
		orphan("no-owner", AntMediaApplicationAdapter.STREAM_SOURCE, null)
				.setUpdateTime(System.currentTimeMillis() - StreamSourceClusterCoordinator.OWNER_START_TIMEOUT_MS - 1000);

		//gone from the node list, but it still answers http, so it is still fetching
		orphan("still-answers", AntMediaApplicationAdapter.STREAM_SOURCE, "192.168.1.8");
		answering.add("192.168.1.8");

		//the http check takes up to a second, and the owner can come back or the row can go meanwhile
		Broadcast reclaimed = orphan("reclaimed", AntMediaApplicationAdapter.STREAM_SOURCE, "192.168.1.7");
		whileAsking.put("192.168.1.7", () -> reclaimed.setUpdateTime(System.currentTimeMillis()));
		orphan("deleted", AntMediaApplicationAdapter.STREAM_SOURCE, "192.168.1.6");
		whileAsking.put("192.168.1.6", () -> fixture.rows.remove("deleted"));

		try (MockedStatic<AntMediaApplicationAdapter> statics = originsAnswerFromTheTest()) {
			tick();
			assertEquals(Set.of("stuck", "orphan", "camera", "no-owner"), cluster.getStreamFetcherList().keySet());
			assertEquals("192.168.1.8", fixture.rows.get("still-answers").getOriginAdress());
			assertEquals("192.168.1.7", fixture.rows.get("reclaimed").getOriginAdress());
			assertEquals("", fixture.rows.get("no-owner-yet").getOriginAdress());

			//a row that went stale after the last tick is picked up by the next one, nobody has to leave again
			orphan("late", AntMediaApplicationAdapter.STREAM_SOURCE, DEPARTED);
			tick();
			assertEquals(Set.of("stuck", "orphan", "camera", "no-owner", "late"), cluster.getStreamFetcherList().keySet());

			//the peer dies before it takes what it was given, so that is an orphan again
			assigned("given-to-the-peer", PEER);
			clusterNodes(alive(HOST), dead(PEER));
			tick();
			assertEquals(Set.of("stuck", "orphan", "camera", "no-owner", "late", "given-to-the-peer"), cluster.getStreamFetcherList().keySet());
		}

		cluster.shutdown();
	}

	@Test
	void everyNodeStartsTheStaleSourcesItOwns() {
		fixture.appSettings.setStartStreamFetcherAutomatically(true);
		startClusterMode();
		//a lower address is alive, so assigning is that node's job, starting its own rows is this one's
		clusterNodes(alive("10.0.0.1"), alive(HOST), dead(DEPARTED));

		assigned("given", HOST);
		//left saying broadcasting by a restart that skipped the reset at boot
		orphan("left-behind", AntMediaApplicationAdapter.STREAM_SOURCE, HOST);
		assigned("lowest-nodes", "10.0.0.1");
		orphan("orphan", AntMediaApplicationAdapter.STREAM_SOURCE, DEPARTED);

		//its fetcher is still here, stopping, and the row went stale under it
		Broadcast held = assigned("held", HOST);
		StreamFetcher holder = mock(StreamFetcher.class);
		when(holder.stopStream()).thenReturn(CompletableFuture.completedFuture(null));
		cluster.getStreamFetcherList().put("held", holder);

		//another node took it between the query and the take
		assigned("lost", HOST);
		doReturn(false).when(fixture.dataStore).claimStaleStreamSource(eq("lost"), any(), any());

		//a start refused after the take would keep the row fresh here, and the lowest node could never move it
		when(fixture.licenceService.isLicenceSuspended()).thenReturn(true);
		tick();
		assertEquals(Set.of("held"), cluster.getStreamFetcherList().keySet());
		assertTrue(DataStore.isStaleStreamSource(fixture.rows.get("given")));

		when(fixture.licenceService.isLicenceSuspended()).thenReturn(false);
		tick();
		assertEquals(Set.of("given", "left-behind", "held"), cluster.getStreamFetcherList().keySet());
		assertEquals(holder, cluster.getStreamFetcher("held"));
		assertTrue(DataStore.isStaleStreamSource(held), "a source this node still holds is not taken again");
		assertEquals("10.0.0.1", fixture.rows.get("lowest-nodes").getOriginAdress());
		assertEquals(DEPARTED, fixture.rows.get("orphan").getOriginAdress());

		//the user stops one of them here, which writes finished, and the next tick leaves it stopped
		assertTrue(cluster.stopStreaming("given", true).isSuccess());
		tick();
		assertEquals(Set.of("left-behind", "held"), cluster.getStreamFetcherList().keySet());

		cluster.shutdown();
	}

	@Test
	void aRowIsNeverGivenBackToTheLiveOwnerThatDidNotStartIt() {
		fixture.appSettings.setStartStreamFetcherAutomatically(true);
		startClusterMode();
		clusterNodes(alive(HOST), alive(PEER));
		long staleSince = System.currentTimeMillis() - StreamSourceClusterCoordinator.OWNER_START_TIMEOUT_MS - 1000;

		//the peer has the fewest streams, but it already had this row for longer than a live owner needs
		when(fixture.dataStore.getLocalLiveBroadcastCount(HOST)).thenReturn(10L);
		assigned("stuck", PEER).setUpdateTime(staleSince);
		tick();
		assertEquals(Set.of("stuck"), cluster.getStreamFetcherList().keySet());

		//alone in the cluster, an owner that cannot start its row has nobody to give it to, so the row is left as it was
		when(fixture.licenceService.isLicenceSuspended()).thenReturn(true);
		Broadcast alone = assigned("alone", HOST);
		alone.setUpdateTime(staleSince);
		clusterNodes(alive(HOST));
		tick();
		assertEquals(HOST, alone.getOriginAdress());
		assertEquals(staleSince, alone.getUpdateTime());

		cluster.shutdown();
	}

	@Test
	void onlyANodeThatSeesItselfAsTheLowestAssigns() {
		assertNull(peek(fixture.newManager(), "clusterCoordinator"), "a single server has nobody to share with");

		startClusterMode();
		orphan("orphan", AntMediaApplicationAdapter.STREAM_SOURCE, DEPARTED);

		try (MockedStatic<AntMediaApplicationAdapter> statics = originsAnswerFromTheTest()) {
			//the setting that resumes sources at boot is also what turns failover on
			clusterNodes(alive(HOST));
			tick();
			assertTrue(cluster.getStreamFetcherList().isEmpty());
			fixture.appSettings.setStartStreamFetcherAutomatically(true);

			//this node drops out of its own list, so it cannot tell what the others see. What it owns needs no list
			assigned("given", HOST);
			clusterNodes(alive("192.168.1.2"));
			tick();
			assertEquals(Set.of("given"), cluster.getStreamFetcherList().keySet());

			//no cluster store, an empty answer, a store that is down. None of it may leave the guard taken
			when(clusterNotifier.getClusterStore()).thenReturn(null);
			tick();
			when(clusterNotifier.getClusterStore()).thenReturn(clusterStore);
			doReturn(null).when(clusterStore).getClusterNodes(anyInt(), anyInt());
			tick();
			doThrow(new IllegalStateException("cluster store is down")).when(clusterStore).getClusterNodes(anyInt(), anyInt());
			assigned("given-while-down", HOST);
			tick();
			assertEquals(DEPARTED, fixture.rows.get("orphan").getOriginAdress());
			assertEquals(Set.of("given", "given-while-down"), cluster.getStreamFetcherList().keySet(), "a failing node list never holds up what this node owns");

			clusterNodes(alive(HOST));
			tick();
			assertEquals(Set.of("given", "given-while-down", "orphan"), cluster.getStreamFetcherList().keySet());
		}

		cluster.shutdown();
		assertEquals(-1L, peek(coordinator, "timerId"), "the check dies with the application");
	}

	@Test
	void everyDeadOriginIsAskedOnceAndAllOfThemAtOnce() throws Exception {
		fixture.appSettings.setStartStreamFetcherAutomatically(true);
		startClusterMode();

		orphan("first", AntMediaApplicationAdapter.STREAM_SOURCE, DEPARTED);
		orphan("other", AntMediaApplicationAdapter.STREAM_SOURCE, "192.168.1.8");
		orphan("second", AntMediaApplicationAdapter.STREAM_SOURCE, DEPARTED);
		clusterNodes(alive(HOST));

		List<String> asked = new CopyOnWriteArrayList<>();
		CompletableFuture<Boolean> answer = new CompletableFuture<>();

		//a static mock only works on the thread that opened it, so the tick opens its own
		CompletableFuture<Void> scan = CompletableFuture.runAsync(() -> {
			try (MockedStatic<AntMediaApplicationAdapter> statics = mockStatic(AntMediaApplicationAdapter.class, CALLS_REAL_METHODS)) {
				statics.when(() -> AntMediaApplicationAdapter.isInstanceAliveAsync(anyString(), anyString(), anyInt(), anyString())).thenAnswer(call -> {
					asked.add(call.getArgument(0));
					return answer;
				});
				coordinator.tick();
			}
		});

		//both origins are asked before either answers, a scan that waited on each in turn would stall at the first
		Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> asked.size() == 2);
		answer.complete(false);
		scan.get(5, TimeUnit.SECONDS);
		settle(cluster.context());

		assertEquals(Set.of(DEPARTED, "192.168.1.8"), Set.copyOf(asked));
		assertEquals(2, asked.size(), "two orphans share an origin, it is asked once");
		assertEquals(Set.of("first", "other", "second"), cluster.getStreamFetcherList().keySet());

		cluster.shutdown();
	}

	@Test
	void anOriginThatIsNotAValidAddressDoesNotStopTheScan() {
		fixture.appSettings.setStartStreamFetcherAutomatically(true);
		startClusterMode();
		clusterNodes(alive(HOST));

		//a value a REST client wrote, no request can be built for it
		orphan("bad-origin", AntMediaApplicationAdapter.STREAM_SOURCE, "bad origin");
		orphan("orphan", AntMediaApplicationAdapter.STREAM_SOURCE, DEPARTED);

		try (MockedStatic<AntMediaApplicationAdapter> statics = mockStatic(AntMediaApplicationAdapter.class, CALLS_REAL_METHODS)) {
			//only the good origin is answered here, the bad one goes through the real check
			statics.when(() -> AntMediaApplicationAdapter.isInstanceAliveAsync(eq(DEPARTED), anyString(), anyInt(), anyString()))
					.thenReturn(CompletableFuture.completedFuture(false));
			tick();
		}

		assertEquals(Set.of("bad-origin", "orphan"), cluster.getStreamFetcherList().keySet());
		cluster.shutdown();
	}

	/** A manager in cluster mode. The coordinator's own 5s timer is cancelled, the test runs every tick by hand. */
	private void startClusterMode() {
		when(clusterNotifier.getClusterStore()).thenReturn(clusterStore);
		IContext red5Context = fixture.scope.getContext();
		when(red5Context.hasBean(IClusterNotifier.BEAN_NAME)).thenReturn(true);
		when(red5Context.getBean(IClusterNotifier.BEAN_NAME)).thenReturn(clusterNotifier);

		cluster = fixture.newManager();
		//a started source fails its first attempt at once and then waits out a retry that never comes
		cluster.prepare = fetcher -> fetcher.workerSupplier = FakeWorker::new;

		coordinator = (StreamSourceClusterCoordinator) peek(cluster, "clusterCoordinator");
		assertTrue(fixture.vertx.cancelTimer((long) peek(coordinator, "timerId")), "cluster mode ticks on a timer");
	}

	/** One tick. The takes it posts run on the shared context, so this waits for them too. */
	private void tick() {
		coordinator.tick();
		settle(cluster.context());
	}

	/** Answers the http reachability check for an origin, which really costs up to a second. */
	private MockedStatic<AntMediaApplicationAdapter> originsAnswerFromTheTest() {
		MockedStatic<AntMediaApplicationAdapter> statics = mockStatic(AntMediaApplicationAdapter.class, CALLS_REAL_METHODS);
		statics.when(() -> AntMediaApplicationAdapter.isInstanceAliveAsync(anyString(), anyString(), anyInt(), anyString())).thenAnswer(call -> {
			String origin = call.getArgument(0);
			whileAsking.getOrDefault(origin, () -> { }).run();
			return CompletableFuture.completedFuture(answering.contains(origin));
		});
		return statics;
	}

	private void clusterNodes(ClusterNode... nodes) {
		doReturn(List.of(nodes)).when(clusterStore).getClusterNodes(anyInt(), anyInt());
	}

	private static ClusterNode alive(String ip) {
		return new ClusterNode(ip, "node-" + ip);
	}

	/** Still listed, but it stopped sending its heartbeat. */
	private static ClusterNode dead(String ip) {
		ClusterNode node = alive(ip);
		node.setLastUpdateTime(0);
		return node;
	}

	/** A source whose owner stopped refreshing it, so its status has decayed to terminated_unexpectedly. */
	private Broadcast orphan(String streamId, String type, String origin) {
		Broadcast row = fixture.row(streamId, "fake://" + streamId);
		row.setType(type);
		row.setOriginAdress(origin);
		row.setStatus(IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING);
		row.setUpdateTime(System.currentTimeMillis() - AntMediaApplicationAdapter.STREAM_TIMEOUT_MS - 1000);
		return row;
	}

	/** What the assign pass leaves behind: the new owner is written, the row stays stale until that node takes it. */
	private Broadcast assigned(String streamId, String owner) {
		Broadcast row = fixture.row(streamId, "fake://" + streamId);
		row.setOriginAdress(owner);
		row.setStatus(IAntMediaStreamHandler.BROADCAST_STATUS_TERMINATED_UNEXPECTEDLY);
		return row;
	}
}
