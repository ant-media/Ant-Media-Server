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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.antmedia.cluster.IClusterNotifier;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.rest.model.Result;
import io.antmedia.streamsource.StreamSourceFixture.ScriptedManager;

/**
 * Keeping the wanted sources running: every node starts or lets go of the sources it holds, and the leader
 * takes every source nobody holds. Every tick is run by hand.
 */
class StreamSourceCoordinatorTest {

	private static final String PEER = "127.0.0.5";

	/** A node that is not alive. */
	private static final String DEAD = "192.168.1.9";

	private StreamSourceFixture fixture;

	@BeforeEach
	void before() {
		fixture = new StreamSourceFixture();
	}

	@AfterEach
	void after() {
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
		when(fixture.licenceService.isLicenceSuspended()).thenReturn(true);
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
	void aStopThatLandsBeforeTheTakeWins() {
		ScriptedManager node = fixture.newManager();
		fixture.appSettings.setStartStreamFetcherAutomatically(true);
		Broadcast orphan = row("orphan", DEAD);

		//the take queues up behind this, so the stop lands between the read and the take
		CountDownLatch hold = new CountDownLatch(1);
		node.context().runOnContext(v -> {
			try {
				hold.await(10, TimeUnit.SECONDS);
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		});
		node.getCoordinator().tick();
		orphan.setFetcherDesiredAction(Broadcast.FETCHER_DESIRED_ACTION_STOP);
		hold.countDown();
		settle(node.context());

		assertTrue(node.getStreamFetcherList().isEmpty());
		assertEquals(DEAD, orphan.getFetcherNode());
	}

	@Test
	void inAClusterOnlyTheLeaderTakesWhatNobodyHolds() {
		fixture.appSettings.setStartStreamFetcherAutomatically(true);
		//a lower address is alive, so taking orphans is that node's job, starting what it holds is this one's
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
	}

	@Test
	void aSourceAnAliveNodeHoldsIsLeftToThatNode() {
		fixture.clusterMode(HOST, PEER);
		ScriptedManager node = fixture.newManager();

		Broadcast peers = row("peers", PEER);
		assertTrue(node.isStreamRunning(peers));
		Result start = node.startStreaming(peers);
		assertFalse(start.isSuccess());
		assertTrue(start.getMessage().contains("already active"));
		assertEquals(PEER, peers.getFetcherNode());

		//the stop is written for the peer, its next tick stops it
		assertTrue(node.stopStreaming("peers", false).isSuccess());
		assertEquals(Broadcast.FETCHER_DESIRED_ACTION_STOP, peers.getFetcherDesiredAction());
		assertEquals(PEER, peers.getFetcherNode());

		//a node that is gone runs nothing, so its source is taken
		Broadcast deadNodes = row("dead-nodes", DEAD);
		assertFalse(node.isStreamRunning(deadNodes));
		assertTrue(node.startStreaming(deadNodes).isSuccess());
		assertEquals(HOST, deadNodes.getFetcherNode());
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
