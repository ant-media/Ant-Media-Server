package io.antmedia.streamsource;

import static io.antmedia.streamsource.StreamSourceFixture.HOST;
import static io.antmedia.streamsource.StreamSourceFixture.awaitState;
import static io.antmedia.streamsource.StreamSourceFixture.peek;
import static io.antmedia.streamsource.StreamSourceFixture.tick;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.datastore.db.types.Broadcast.PlayListItem;
import io.antmedia.datastore.db.types.BroadcastUpdate;
import io.antmedia.muxer.IAntMediaStreamHandler;
import io.antmedia.rest.model.Result;
import io.antmedia.streamsource.StreamFetcher.Reason;
import io.antmedia.streamsource.StreamFetcher.State;
import io.antmedia.streamsource.StreamSourceFixture.ScriptedFetcher;
import io.antmedia.streamsource.StreamSourceFixture.ScriptedManager;
import io.vertx.core.Context;

/**
 * The manager's own job: who is allowed to start a source, what the registry says about it, and the
 * broadcast row behind it. The state machine underneath is scripted, nothing here opens a source.
 */
class StreamFetcherManagerTest {

	private static final long LONG_AGO = 60000;

	/** Another node, which a standalone server never sees alive. */
	private static final String OTHER_NODE = "10.0.0.9";

	private StreamSourceFixture fixture;
	private ScriptedManager manager;
	private Context context;

	@BeforeEach
	void before() {
		fixture = new StreamSourceFixture();
		manager = fixture.newManager();
		context = manager.context();
	}

	@AfterEach
	void after() {
		manager.getStreamFetcherList().values().forEach(StreamFetcher::stopStream);
		manager.fetchers.forEach(ScriptedFetcher::releaseAll);
		fixture.close();
	}

	@Test
	void anEntryInTheRegistryAlwaysWins() {
		Broadcast source = fixture.row("source", "fake://source");
		assertTrue(manager.startStreaming(source).isSuccess());

		ScriptedFetcher running = manager.fetchers.get(0);
		awaitState(running, State.CONNECTING);
		assertSame(running, manager.getStreamFetcher("source"));

		Result again = manager.startStreaming(source);
		assertFalse(again.isSuccess());
		assertTrue(again.getMessage().contains("already active"));
		assertEquals(1, manager.fetchers.size(), "a refused start must not even build a fetcher");

		//still refused mid teardown, the entry only goes once the database says finished
		running.stopStream();
		awaitState(running, State.STOPPING);
		assertFalse(manager.startStreaming(source).isSuccess());

		running.releaseAll();
		awaitState(running, State.STOPPED);
		assertTrue(manager.getStreamFetcherList().isEmpty());
		assertTrue(manager.startStreaming(source).isSuccess(), "the id is free again once it is unregistered");
	}

	@Test
	void aUserStartClaimsTheSourceForThisNode() {
		//stopped by its user after failing a few times on a node that is gone
		Broadcast source = fixture.row("claimed", "fake://claimed");
		source.setFetcherNode(OTHER_NODE);
		source.setFetcherDesiredAction(Broadcast.FETCHER_DESIRED_ACTION_STOP);
		source.setFetcherFailures(4);

		assertTrue(manager.startStreaming(source).isSuccess());
		assertEquals(HOST, source.getFetcherNode());
		assertEquals(Broadcast.FETCHER_DESIRED_ACTION_START, source.getFetcherDesiredAction());
		assertEquals(0, source.getFetcherFailures(), "a user start gives the source a fresh retry budget");
		//the node that died never cleared what it told the edges
		verify(fixture.dataStore).clearStreamInfoList("claimed");

		//held here from before a restart, nothing runs it yet
		assertTrue(manager.startStreaming(heldRow("held")).isSuccess());
		assertNotNull(manager.getStreamFetcher("held"));

		//another node took it after it was read, so the claim on what was read loses
		Broadcast asRead = fixture.row("moved", "fake://moved");
		fixture.row("moved", "fake://moved").setFetcherNode(OTHER_NODE);

		assertFalse(manager.startStreaming(asRead).isSuccess());
		assertNull(manager.getStreamFetcher("moved"), "a lost claim starts nothing");
		assertEquals(OTHER_NODE, fixture.rows.get("moved").getFetcherNode());

		//updated on another node after the caller read it, the claim starts what the row says now
		Broadcast beforeUpdate = fixture.row("updated", "fake://old");
		fixture.row("updated", "fake://new");
		assertTrue(manager.startStreaming(beforeUpdate).isSuccess());
		assertEquals("fake://new", peek(manager.getStreamFetcher("updated"), "streamUrl"));
	}

	@Test
	void everyRefusalLeavesTheRegistryAndTheRowClean() {
		when(fixture.licenceService.isLicenceSuspended()).thenReturn(true);
		Broadcast licensed = fixture.row("licensed", "fake://licensed");
		Result suspended = manager.startStreaming(licensed);
		assertFalse(suspended.isSuccess());
		assertEquals("License is suspended", suspended.getMessage());
		assertEquals(StreamFetcherManager.START_REFUSED, suspended.getErrorId(), "nothing was touched, so another node may take it");
		assertTrue(manager.getStreamFetcherList().isEmpty());
		assertNull(licensed.getFetcherNode(), "a refused start claims nothing");
		when(fixture.licenceService.isLicenceSuspended()).thenReturn(false);

		//this node is at its cpu or memory limit
		when(fixture.statsCollector.enoughResource()).thenReturn(false);
		Broadcast busy = fixture.row("busy", "fake://busy");
		Result full = manager.startStreaming(busy);
		assertEquals(StreamFetcherManager.START_REFUSED, full.getErrorId());
		assertTrue(manager.fetchers.isEmpty());
		assertNull(busy.getFetcherNode());
		when(fixture.statsCollector.enoughResource()).thenReturn(true);

		//somebody else registered first
		StreamFetcher squatter = mock(StreamFetcher.class);
		manager.getStreamFetcherList().put("contended", squatter);
		Broadcast contended = fixture.row("contended", "fake://contended");
		assertFalse(manager.startStreaming(contended).isSuccess());
		assertSame(squatter, manager.getStreamFetcher("contended"));
		assertTrue(manager.fetchers.isEmpty(), "the start is refused before a fetcher is made");
		assertNull(contended.getFetcherNode());

		//a throw after registering is the stuck source this rewrite is about: nothing would ever stop it.
		//It is a failed attempt like any other, so the source is let go for the next start
		ScriptedManager broken = fixture.newManager();
		broken.prepare = fetcher -> fetcher.failOnStart = true;
		Broadcast brokenRow = fixture.row("broken", "fake://broken");
		assertFalse(broken.startStreaming(brokenRow).isSuccess());
		assertTrue(broken.getStreamFetcherList().isEmpty(), "a registration that never started must be rolled back");
		assertNull(brokenRow.getFetcherNode());
		assertEquals(1, brokenRow.getFetcherFailures());

		//a server on its way down must not accept work its pool will not outlive
		ScriptedManager closing = fixture.newManager();
		closing.shuttingDown();
		Broadcast late = fixture.row("late", "fake://late");
		Result lateResult = closing.startStreaming(late);
		assertFalse(lateResult.isSuccess());
		assertEquals("Server is shutting down", lateResult.getMessage());
		assertTrue(closing.getStreamFetcherList().isEmpty());
		assertNull(late.getFetcherNode());
	}

	@Test
	void theBroadcastFollowsTheTransitions() {
		manager.prepare = fetcher -> fetcher.script(FakeWorker.publishesThenEnds());

		List<Boolean> registeredWhenEnded = new CopyOnWriteArrayList<>();
		doAnswer(call -> {
			registeredWhenEnded.add(manager.getStreamFetcherList().containsKey("status"));
			Predicate<BroadcastUpdate> end = call.getArgument(3);
			end.test(new BroadcastUpdate());
			return null;
		}).when(fixture.app).closeBroadcast(eq("status"), any(), any(), any());

		Broadcast row = fixture.row("status", "fake://status");
		manager.startStreaming(row);
		ScriptedFetcher fetcher = manager.fetchers.get(0);
		awaitState(fetcher, State.STOPPED);

		verify(fixture.app).startPublish("status", 0, IAntMediaStreamHandler.PUBLISH_TYPE_PULL, null, null);
		//the attempt was on air, so its broadcast is ended, once, and only after the entry is gone
		assertEquals(List.of(false), registeredWhenEnded);

		//it dropped, so it is let go to be tried again
		assertNull(row.getFetcherNode());
		assertEquals(Broadcast.FETCHER_DESIRED_ACTION_START, row.getFetcherDesiredAction());
		assertEquals(1, row.getFetcherFailures());
		assertEquals(IAntMediaStreamHandler.BROADCAST_STATUS_PREPARING, row.getStatus());
		//written by startPublish once the source is on air, never on connect
		assertNull(row.getOriginAdress());
		assertTrue(row.getStartTime() > 0, "every attempt writes its start, the retry delay counts from it");
	}

	@Test
	void aFailedAttemptIsCountedAndStopsTheSourceAtTheLimit() {
		fixture.appSettings.setStreamFetcherMaxRetryAttempts(3);
		manager.prepare = fetcher -> fetcher.script(FakeWorker.ending(Reason.OPEN_FAILED));

		//one failure counted by another node already, the count goes on from there
		Broadcast row = fixture.row("failing", "fake://failing");
		row.setFetcherFailures(1);

		assertTrue(manager.claimAndStart("failing", null, false).isSuccess());
		awaitState(manager.fetchers.get(0), State.STOPPED);
		assertNull(row.getFetcherNode());
		assertEquals(2, row.getFetcherFailures());
		assertEquals(Broadcast.FETCHER_DESIRED_ACTION_START, row.getFetcherDesiredAction());
		assertEquals(IAntMediaStreamHandler.BROADCAST_STATUS_PREPARING, row.getStatus());
		//it never got on air, so there is no broadcast to end
		verify(fixture.app, never()).closeBroadcast(eq("failing"), any(), any(), any());

		//the third failure in a row reaches the limit: not wanted anymore, so nothing starts it again
		assertTrue(manager.claimAndStart("failing", null, false).isSuccess());
		awaitState(manager.fetchers.get(1), State.STOPPED);
		assertNull(row.getFetcherNode());
		assertEquals(3, row.getFetcherFailures());
		assertEquals(Broadcast.FETCHER_DESIRED_ACTION_STOP, row.getFetcherDesiredAction());
		assertEquals(IAntMediaStreamHandler.BROADCAST_STATUS_FINISHED, row.getStatus());

		//a source that gets on air counts from zero again
		Broadcast flaky = fixture.row("flaky", "fake://flaky");
		flaky.setFetcherFailures(2);
		manager.prepare = fetcher -> fetcher.script(FakeWorker.publishesThenEnds());
		assertTrue(manager.claimAndStart("flaky", null, false).isSuccess());
		awaitState(manager.fetchers.get(2), State.STOPPED);
		assertEquals(1, flaky.getFetcherFailures());
		assertEquals(Broadcast.FETCHER_DESIRED_ACTION_START, flaky.getFetcherDesiredAction());
	}

	@Test
	void aForcedReconnectEndsTheBroadcastThatWasOnAirAndKeepsTheSource() {
		manager.prepare = fetcher -> fetcher.script(FakeWorker.publishing(), FakeWorker.holding());
		Broadcast row = fixture.row("reconnect", "fake://reconnect");
		manager.startStreaming(row);
		ScriptedFetcher fetcher = manager.fetchers.get(0);
		awaitState(fetcher, State.STREAMING);

		fetcher.restart();
		awaitState(fetcher, State.STOPPING);
		fetcher.workers.get(0).release();
		awaitState(fetcher, State.CONNECTING);

		verify(fixture.app, times(1)).closeBroadcast(eq("reconnect"), any(), any(), any());
		assertEquals(IAntMediaStreamHandler.BROADCAST_STATUS_PREPARING, row.getStatus());
		assertEquals(HOST, row.getFetcherNode(), "a reconnect is not a failure, the source stays here");
		assertEquals(0, row.getFetcherFailures());
		assertSame(fetcher, manager.getStreamFetcher("reconnect"), "reconnected in place, same fetcher and registration");
	}

	@Test
	void onTickIsThePerSourceMaintenance() {
		fixture.appSettings.setRestartStreamFetcherPeriod(1);
		manager.testSetStreamCheckerInterval(1000);

		//nobody is watching and the restart period is also up: stopping wins over reconnecting
		Broadcast unwatched = heldRow("unwatched");
		unwatched.setAutoStartStopEnabled(true);
		unwatched.setFetcherDesiredAction(Broadcast.FETCHER_DESIRED_ACTION_START);
		unwatched.setStartTime(System.currentTimeMillis() - LONG_AGO);

		StreamFetcher idle = tickFetcher("unwatched", State.STREAMING, LONG_AGO);
		manager.onTick(idle);
		verify(idle).stopStream();
		verify(idle, never()).restart();
		//not wanted until a viewer starts it again, so nothing else may start it meanwhile
		assertEquals(Broadcast.FETCHER_DESIRED_ACTION_STOP, unwatched.getFetcherDesiredAction());

		//one viewer of any kind is enough to keep the same source up
		unwatched.setFetcherDesiredAction(Broadcast.FETCHER_DESIRED_ACTION_START);
		unwatched.setHlsViewerCount(1);
		StreamFetcher watched = tickFetcher("unwatched", State.STREAMING, LONG_AGO);
		manager.onTick(watched);
		verify(watched, never()).stopStream();

		//and a source nobody has found yet is left alone until its grace window is up
		unwatched.setHlsViewerCount(0);
		unwatched.setStartTime(System.currentTimeMillis());
		StreamFetcher justStarted = tickFetcher("unwatched", State.STREAMING, LONG_AGO);
		manager.onTick(justStarted);
		verify(justStarted, never()).stopStream();

		//up for longer than the forced restart period, so it reconnects in place
		heldRow("restarts");
		StreamFetcher streaming = tickFetcher("restarts", State.STREAMING, LONG_AGO);
		manager.onTick(streaming);
		verify(streaming).restart();
		verify(streaming, never()).stopStream();

		//the period is measured per source, one that only just came up is left to run
		StreamFetcher justUp = tickFetcher("restarts", State.STREAMING, 0);
		manager.onTick(justUp);
		verify(justUp, never()).restart();

		//a source that is not up yet has nothing to reconnect
		StreamFetcher connecting = tickFetcher("restarts", State.CONNECTING, LONG_AGO);
		manager.onTick(connecting);
		verify(connecting, never()).restart();

		//a forced reconnect would restart the current item, not the playlist, so playlists sit it out
		playlistRow("playlist");
		StreamFetcher item = tickFetcher("playlist", State.STREAMING, LONG_AGO);
		when(item.isPlaylistItem()).thenReturn(true);
		manager.onTick(item);
		verify(item, never()).restart();

		//stopping just the playing item would make the controller start the next one, so the playlist goes whole
		Broadcast unwatchedPlaylist = playlistRow("unwatched-playlist");
		unwatchedPlaylist.setAutoStartStopEnabled(true);
		unwatchedPlaylist.setStartTime(System.currentTimeMillis() - LONG_AGO);

		manager.prepare = fetcher -> fetcher.script(FakeWorker.holding());
		assertTrue(manager.getPlaylistController().startPlaylist(unwatchedPlaylist).isSuccess());
		Awaitility.await("the playlist item is registered")
				.atMost(15, TimeUnit.SECONDS)
				.until(() -> manager.getStreamFetcher("unwatched-playlist") != null);

		StreamFetcher playlistItem = tickFetcher("unwatched-playlist", State.STREAMING, LONG_AGO);
		when(playlistItem.isPlaylistItem()).thenReturn(true);
		manager.onTick(playlistItem);
		verify(playlistItem, never()).stopStream();

		Awaitility.await("the playlist is finished, not just the item that was playing")
				.atMost(15, TimeUnit.SECONDS)
				.until(() -> !manager.getPlaylistController().isRunning("unwatched-playlist"));
	}

	@Test
	void theTickStopsASourceThatIsNotThisNodesToFetch() {
		//the broadcast is gone
		StreamFetcher deleted = tickFetcher("deleted", State.STREAMING, LONG_AGO);
		manager.onTick(deleted);
		verify(deleted).stopStream();

		//another node took it over, or a stop let go of it while this node looked dead
		for (String fetcherNode : Arrays.asList(OTHER_NODE, null)) {
			heldRow("taken").setFetcherNode(fetcherNode);
			StreamFetcher taken = tickFetcher("taken", State.STREAMING, LONG_AGO);
			manager.onTick(taken);
			verify(taken).stopStream();
		}
		//the row is not this node's anymore, so nothing is written to it
		verify(fixture.dataStore, never()).compareAndSetFetcherNode(eq("taken"), any(), any(), anyBoolean(), any());
		verify(fixture.dataStore, never()).updateBroadcastFields(eq("taken"), any());

		//its user stopped it on a node that does not run it
		heldRow("unwanted").setFetcherDesiredAction(Broadcast.FETCHER_DESIRED_ACTION_STOP);
		StreamFetcher unwanted = tickFetcher("unwanted", State.STREAMING, LONG_AGO);
		manager.onTick(unwanted);
		verify(unwanted).stopStream();

		//a playlist has no fetcher node, its item is left alone
		playlistRow("playlist");
		StreamFetcher item = tickFetcher("playlist", State.STREAMING, LONG_AGO);
		when(item.isPlaylistItem()).thenReturn(true);
		manager.onTick(item);
		verify(item, never()).stopStream();
	}

	@Test
	void aSourceAnotherNodeTookStopsWithoutATrace() {
		manager.prepare = fetcher -> fetcher.script(FakeWorker.publishing());
		Broadcast row = fixture.row("taken-over", "fake://taken-over");
		manager.startStreaming(row);
		ScriptedFetcher fetcher = manager.fetchers.get(0);
		awaitState(fetcher, State.STREAMING);

		//another node decided this one was dead and claimed the source
		row.setFetcherNode(OTHER_NODE);
		tick(fetcher);
		fetcher.releaseAll();
		awaitState(fetcher, State.STOPPED);

		assertTrue(manager.getStreamFetcherList().isEmpty());
		assertEquals(OTHER_NODE, row.getFetcherNode());
		assertEquals(IAntMediaStreamHandler.BROADCAST_STATUS_PREPARING, row.getStatus(), "nothing is written over the other node's row");

		//taken while it was still connecting: its first packet must not publish it here, startPublish is not fenced
		Broadcast early = fixture.row("taken-early", "fake://taken-early");
		manager.prepare = fetcher -> fetcher.script(new FakeWorker() {
			{
				publishes = true;
				holds = true;
			}

			@Override
			public Reason run(Broadcast broadcast) {
				early.setFetcherNode(OTHER_NODE);
				return super.run(broadcast);
			}
		});
		manager.startStreaming(early);
		ScriptedFetcher earlyFetcher = manager.fetchers.get(1);
		awaitState(earlyFetcher, State.STOPPING);
		earlyFetcher.releaseAll();
		awaitState(earlyFetcher, State.STOPPED);

		verify(fixture.app, never()).startPublish(eq("taken-early"), anyLong(), any(), any(), any());
		assertEquals(OTHER_NODE, early.getFetcherNode());
	}

	@Test
	void aSourceStillBeingReachedIsKeptFromDecaying() {
		heldRow("waiting");
		manager.onTick(tickFetcher("waiting", State.CONNECTING, LONG_AGO));

		ArgumentCaptor<BroadcastUpdate> written = ArgumentCaptor.forClass(BroadcastUpdate.class);
		verify(fixture.dataStore).compareAndSetFetcherNode(eq("waiting"), eq(HOST), eq(HOST), eq(false), written.capture());

		BroadcastUpdate refresh = written.getValue();
		assertEquals(IAntMediaStreamHandler.BROADCAST_STATUS_PREPARING, refresh.getStatus());
		assertNotNull(refresh.getUpdateTime());
		assertNull(refresh.getStartTime(), "the auto stop timeout and the retry delay measure from the start time, it must not move");

		//a source that is up needs no refresh, its packets already keep it fresh
		heldRow("live");
		manager.onTick(tickFetcher("live", State.STREAMING, LONG_AGO));
		verify(fixture.dataStore, never()).compareAndSetFetcherNode(eq("live"), any(), any(), anyBoolean(), any());

		//the row named this node when it was read, the write finds another node there
		heldRow("lost");
		doReturn(false).when(fixture.dataStore).compareAndSetFetcherNode(eq("lost"), any(), any(), anyBoolean(), any());
		StreamFetcher lost = tickFetcher("lost", State.CONNECTING, LONG_AGO);
		manager.onTick(lost);
		verify(lost).stopStream();
	}

	@Test
	void isStreamRunningLooksPastThisNode() {
		Broadcast free = fixture.row("free", "fake://free");
		assertFalse(manager.isStreamRunning(free));

		manager.startStreaming(free);
		assertTrue(manager.isStreamRunning(free), "a registry entry is this node fetching it");

		//a playlist between two items has no fetcher, but it is still running
		Broadcast playlist = playlistRow("between-items");
		manager.prepare = fetcher -> fetcher.script(FakeWorker.holding());
		assertTrue(manager.getPlaylistController().startPlaylist(playlist).isSuccess());
		assertTrue(manager.isStreamRunning(playlist));

		//held by an alive node, this one, though nothing runs it here yet
		Broadcast held = heldRow("held");
		assertTrue(manager.isStreamRunning(held));

		//held by a node that is not alive, or by nobody, so it is free to take
		held.setFetcherNode(OTHER_NODE);
		assertFalse(manager.isStreamRunning(held));
		held.setFetcherNode(null);
		held.setStatus(IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING);
		assertFalse(manager.isStreamRunning(held), "the fetcher node says who runs a source, not its status");

		//a playlist has no fetcher node, it runs where its row is live, on a host that answers
		Broadcast live = playlistRow("live-playlist");
		live.setStatus(IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING);
		live.setOriginAdress(HOST);
		assertTrue(manager.isStreamRunning(live));

		live.setOriginAdress("10.255.255.1");
		assertFalse(manager.isStreamRunning(live));

		live.setOriginAdress(HOST);
		live.setStatus(IAntMediaStreamHandler.BROADCAST_STATUS_FINISHED);
		assertFalse(manager.isStreamRunning(live));
	}

	@Test
	void shutdownEndsThePlaylistsFirstAndThenEverythingElse() throws Exception {
		Broadcast plain = fixture.row("plain-source", "fake://plain-source");
		manager.startStreaming(plain);
		awaitState(manager.fetchers.get(0), State.CONNECTING);

		Broadcast playlist = playlistRow("shutting-playlist");
		assertTrue(manager.getPlaylistController().startPlaylist(playlist).isSuccess());
		Awaitility.await("the playlist item is registered")
				.atMost(15, TimeUnit.SECONDS)
				.until(() -> manager.getStreamFetcher("shutting-playlist") != null);

		//shutdown blocks on the context, so it cannot be called from one
		CompletableFuture<Void> done = CompletableFuture.runAsync(manager::shutdown);

		Awaitility.await("the playlist is finished as a whole before its item is torn down")
				.atMost(15, TimeUnit.SECONDS)
				.until(() -> !manager.getPlaylistController().isRunning("shutting-playlist"));

		manager.fetchers.forEach(ScriptedFetcher::releaseAll);
		done.get(20, TimeUnit.SECONDS);

		assertTrue(manager.getStreamFetcherList().isEmpty());
		//let go and still wanted, so another node takes it over
		assertNull(plain.getFetcherNode());
		assertEquals(Broadcast.FETCHER_DESIRED_ACTION_START, plain.getFetcherDesiredAction());
		assertEquals(IAntMediaStreamHandler.BROADCAST_STATUS_FINISHED, plain.getStatus());

		assertFalse(manager.startStreaming(fixture.row("too-late", "fake://too-late")).isSuccess(), "the pool is gone");
	}

	@Test
	void stopStreamingHandlesEveryKindOfId() throws Exception {
		assertEquals("Undefined stream id", manager.stopStreaming("   ", false).getMessage());

		Result unknown = manager.stopStreaming("nobody", false);
		assertFalse(unknown.isSuccess());
		assertTrue(unknown.getMessage().contains("No matching stream source"));

		Broadcast async = fixture.row("stop-async", "fake://stop-async");
		manager.startStreaming(async);
		ScriptedFetcher asyncFetcher = manager.fetchers.get(0);
		awaitState(asyncFetcher, State.CONNECTING);

		assertTrue(manager.stopStreaming("stop-async", false).isSuccess());
		assertEquals(Broadcast.FETCHER_DESIRED_ACTION_STOP, async.getFetcherDesiredAction(), "written first, so nothing starts it again");

		//the entry goes on the STOPPED transition, not on the stop call, so a second one is still accepted
		awaitState(asyncFetcher, State.STOPPING);
		assertTrue(manager.stopStreaming("stop-async", false).isSuccess());

		asyncFetcher.releaseAll();
		awaitState(asyncFetcher, State.STOPPED);
		assertFalse(manager.getStreamFetcherList().containsKey("stop-async"));
		assertNull(async.getFetcherNode());
		assertEquals(IAntMediaStreamHandler.BROADCAST_STATUS_FINISHED, async.getStatus());

		//a blocking stop only returns once the database says finished
		Broadcast blocking = fixture.row("stop-blocking", "fake://stop-blocking");
		manager.startStreaming(blocking);
		ScriptedFetcher blockingFetcher = manager.fetchers.get(1);
		awaitState(blockingFetcher, State.CONNECTING);

		CompletableFuture<Result> stopped = CompletableFuture.supplyAsync(() -> manager.stopStreaming("stop-blocking", true));
		awaitState(blockingFetcher, State.STOPPING);
		blockingFetcher.releaseAll();
		assertTrue(stopped.get(15, TimeUnit.SECONDS).isSuccess());
		assertEquals(IAntMediaStreamHandler.BROADCAST_STATUS_FINISHED, blocking.getStatus());

		//nothing runs it, it was let go after a failed attempt: the stop still sticks
		Broadcast released = fixture.row("released", "fake://released");
		released.setFetcherFailures(2);
		released.setStatus(IAntMediaStreamHandler.BROADCAST_STATUS_PREPARING);
		assertTrue(manager.stopStreaming("released", false).isSuccess());
		assertEquals(Broadcast.FETCHER_DESIRED_ACTION_STOP, released.getFetcherDesiredAction());
		assertEquals(IAntMediaStreamHandler.BROADCAST_STATUS_FINISHED, released.getStatus());

		//held by a node that is gone, nobody else will let go of it
		Broadcast orphaned = fixture.row("orphaned", "fake://orphaned");
		orphaned.setFetcherNode(OTHER_NODE);
		assertTrue(manager.stopStreaming("orphaned", false).isSuccess());
		assertNull(orphaned.getFetcherNode());
		assertEquals(IAntMediaStreamHandler.BROADCAST_STATUS_FINISHED, orphaned.getStatus());

		//a playlist id goes to the controller, stopping only its item would start the next one
		Broadcast playlist = playlistRow("stop-playlist");
		manager.prepare = fetcher -> fetcher.script(FakeWorker.holding());
		manager.getPlaylistController().startPlaylist(playlist);

		Result stoppedPlaylist = manager.stopStreaming("stop-playlist", false);
		assertTrue(stoppedPlaylist.isSuccess());
		assertEquals("Playlist stopped", stoppedPlaylist.getMessage());
	}

	@Test
	void aBlockingStopRefusesToRunOnTheEventLoop() throws Exception {
		Broadcast playlist = playlistRow("no-deadlock");
		manager.prepare = fetcher -> fetcher.script(FakeWorker.holding());
		assertTrue(manager.getPlaylistController().startPlaylist(playlist).isSuccess());

		CompletableFuture<Result> fromContext = new CompletableFuture<>();
		context.runOnContext(v -> fromContext.complete(manager.stopStreaming("no-deadlock", true)));

		Result result = fromContext.get(15, TimeUnit.SECONDS);
		assertFalse(result.isSuccess(), "the transition it would wait for needs this very thread");
		assertTrue(result.getMessage().contains("Failed to stop the playlist"));
	}

	@Test
	void aBlockingStopFromAnInterruptedCallerGivesUpAtOnce() {
		manager.startStreaming(fixture.row("interrupted-source", "fake://interrupted-source"));
		awaitState(manager.fetchers.get(0), State.CONNECTING);

		Thread.currentThread().interrupt();
		Result source = manager.stopStreaming("interrupted-source", true);
		assertTrue(Thread.interrupted(), "the interrupt belongs to the caller, it must still be set");
		assertFalse(source.isSuccess(), "the stop was asked for but nobody waited to see it land");
		assertTrue(source.getMessage().contains("Failed to stop the stream with Blocking"));

		//an item that holds, so the playlist cannot be over before anybody waits for it
		manager.prepare = fetcher -> fetcher.script(FakeWorker.holding());
		assertTrue(manager.getPlaylistController().startPlaylist(playlistRow("interrupted-playlist")).isSuccess());
		Awaitility.await("the playlist item is registered")
				.atMost(15, TimeUnit.SECONDS)
				.until(() -> manager.getStreamFetcher("interrupted-playlist") != null);

		Thread.currentThread().interrupt();
		Result playlist = manager.stopStreaming("interrupted-playlist", true);
		assertTrue(Thread.interrupted());
		assertFalse(playlist.isSuccess());
		assertTrue(playlist.getMessage().contains("Failed to stop the playlist with Blocking"));
	}

	/** onTick only reads these off a fetcher, so a mock says exactly what the test means. */
	private StreamFetcher tickFetcher(String streamId, State state, long inStateForMs) {
		StreamFetcher fetcher = mock(StreamFetcher.class);
		when(fetcher.getStreamId()).thenReturn(streamId);
		when(fetcher.getState()).thenReturn(state);
		when(fetcher.getStateSinceMs()).thenReturn(System.currentTimeMillis() - inStateForMs);
		return fetcher;
	}

	/** A source this node holds. */
	private Broadcast heldRow(String streamId) {
		Broadcast row = fixture.row(streamId, "fake://" + streamId);
		row.setFetcherNode(HOST);
		return row;
	}

	private Broadcast playlistRow(String streamId) {
		Broadcast playlist = fixture.row(streamId, "fake://" + streamId);
		playlist.setType(AntMediaApplicationAdapter.PLAY_LIST);
		playlist.setPlayListItemList(List.of(new PlayListItem("fake://item", AntMediaApplicationAdapter.VOD)));
		return playlist;
	}
}
