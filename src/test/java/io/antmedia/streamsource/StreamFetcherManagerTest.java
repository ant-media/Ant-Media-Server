package io.antmedia.streamsource;

import static io.antmedia.streamsource.StreamSourceFixture.NO_RETRY_IN_THIS_TEST_MS;
import static io.antmedia.streamsource.StreamSourceFixture.awaitState;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

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
import io.antmedia.streamsource.StreamFetcher.State;
import io.antmedia.streamsource.StreamSourceFixture.ScriptedFetcher;
import io.antmedia.streamsource.StreamSourceFixture.ScriptedManager;
import io.vertx.core.Context;

/**
 * The manager's own job: who is allowed to start a source, what the registry says about it, and the
 * broadcast status behind it. The state machine underneath is scripted, nothing here opens a source.
 */
class StreamFetcherManagerTest {

	private static final long LONG_AGO = 60000;

	private StreamSourceFixture fixture;
	private ScriptedManager manager;
	private Context context;

	@BeforeEach
	void before() {
		fixture = new StreamSourceFixture();
		fixture.appSettings.setStreamFetcherRetryDelayMs(NO_RETRY_IN_THIS_TEST_MS);

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
	void forceStartSkipsTheDatabaseHalfOfTheCheck() {
		//a row left saying broadcasting by a restart, owned by this very host
		Broadcast stale = fixture.row("stale", "fake://stale");
		stale.setStatus(IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING);
		stale.setOriginAdress("127.0.0.1");

		assertFalse(manager.startStreaming(stale).isSuccess(), "the database says this host already owns it");
		assertTrue(manager.startStreaming(stale, true).isSuccess());
		assertEquals(1, manager.getStreamFetcherList().size());
	}

	@Test
	void everyRefusalLeavesTheRegistryClean() {
		when(fixture.licenceService.isLicenceSuspended()).thenReturn(true);
		Result suspended = manager.startStreaming(fixture.row("licensed", "fake://licensed"));
		assertFalse(suspended.isSuccess());
		assertEquals("License is suspended", suspended.getMessage());
		assertTrue(manager.getStreamFetcherList().isEmpty());
		when(fixture.licenceService.isLicenceSuspended()).thenReturn(false);

		//somebody else registered between the check and the put
		ScriptedManager raced = fixture.newManager();
		StreamFetcher squatter = mock(StreamFetcher.class);
		raced.getStreamFetcherList().put("contended", squatter);

		Result lost = raced.startStreaming(fixture.row("contended", "fake://contended"), true);
		assertFalse(lost.isSuccess());
		assertSame(squatter, raced.getStreamFetcher("contended"));
		assertEquals(State.IDLE, raced.fetchers.get(0).getState(), "the fetcher that lost the race must never start");

		//a throw after registering is the stuck source this rewrite is about: nothing would ever stop it
		ScriptedManager broken = fixture.newManager();
		broken.prepare = fetcher -> fetcher.failOnStart = true;
		Result thrown = broken.startStreaming(fixture.row("broken", "fake://broken"));
		assertFalse(thrown.isSuccess());
		assertTrue(broken.getStreamFetcherList().isEmpty(), "a registration that never started must be rolled back");

		//a server on its way down must not accept work its pool will not outlive
		ScriptedManager closing = fixture.newManager();
		closing.shuttingDown();
		Result late = closing.startStreaming(fixture.row("late", "fake://late"));
		assertFalse(late.isSuccess());
		assertEquals("Server is shutting down", late.getMessage());
		assertTrue(closing.getStreamFetcherList().isEmpty());
	}

	@Test
	void theBroadcastStatusFollowsTheTransitions() throws Exception {
		fixture.appSettings.setStreamFetcherRetryDelayMs(20);
		manager.prepare = fetcher -> fetcher.script(FakeWorker.publishesThenEnds(), FakeWorker.holding());

		List<Boolean> registeredWhenClosed = new CopyOnWriteArrayList<>();
		doAnswer(call -> {
			registeredWhenClosed.add(manager.getStreamFetcherList().containsKey("status"));
			return null;
		}).when(fixture.app).closeBroadcast(eq("status"), any(), any());

		manager.startStreaming(fixture.row("status", "fake://status"));
		ScriptedFetcher fetcher = manager.fetchers.get(0);

		Awaitility.await("the second attempt is connecting")
				.atMost(15, TimeUnit.SECONDS)
				.until(() -> fetcher.workers.size() == 2 && fetcher.getState() == State.CONNECTING);

		//CONNECTING, RECONNECT_WAIT and CONNECTING again all claim the source as preparing
		verify(fixture.app, times(3)).getFreshBroadcastUpdateForStatus(
				IAntMediaStreamHandler.PUBLISH_TYPE_PULL, IAntMediaStreamHandler.BROADCAST_STATUS_PREPARING);
		verify(fixture.dataStore, times(3)).updateBroadcastFields(eq("status"), any());
		verify(fixture.app, times(1)).startPublish("status", 0, IAntMediaStreamHandler.PUBLISH_TYPE_PULL, null, null);

		//the attempt that was on air ended, so the broadcast ends with it. Once, not once per transition
		assertEquals(List.of(true), registeredWhenClosed);

		CompletableFuture<Void> stopped = fetcher.stopStream();
		fetcher.releaseAll();
		stopped.get(15, TimeUnit.SECONDS);

		//whoever polls for finished has to be able to start again, so the entry goes first
		assertEquals(List.of(true, false), registeredWhenClosed);
	}

	@Test
	void onTickIsThePerSourceMaintenance() {
		fixture.appSettings.setRestartStreamFetcherPeriod(1);
		manager.testSetStreamCheckerInterval(1000);

		//the broadcast is gone, so nothing owns this source any more
		StreamFetcher orphan = tickFetcher("deleted", State.STREAMING, LONG_AGO);
		manager.onTick(orphan);
		verify(orphan).stopStream();

		//nobody is watching and the restart period is also up: stopping wins over reconnecting
		Broadcast unwatched = fixture.row("unwatched", "fake://unwatched");
		unwatched.setAutoStartStopEnabled(true);
		unwatched.setStartTime(System.currentTimeMillis() - LONG_AGO);

		StreamFetcher idle = tickFetcher("unwatched", State.STREAMING, LONG_AGO);
		manager.onTick(idle);
		verify(idle).stopStream();
		verify(idle, never()).restart();

		//up for longer than the forced restart period, so it reconnects in place
		fixture.row("restarts", "fake://restarts");
		StreamFetcher streaming = tickFetcher("restarts", State.STREAMING, LONG_AGO);
		manager.onTick(streaming);
		verify(streaming).restart();
		verify(streaming, never()).stopStream();

		//a source that is not up yet has nothing to reconnect
		StreamFetcher connecting = tickFetcher("restarts", State.CONNECTING, LONG_AGO);
		manager.onTick(connecting);
		verify(connecting, never()).restart();

		//a forced reconnect would restart the current item, not the playlist, so playlists sit it out
		Broadcast playlist = fixture.row("playlist", "fake://playlist");
		playlist.setType(AntMediaApplicationAdapter.PLAY_LIST);
		StreamFetcher item = tickFetcher("playlist", State.STREAMING, LONG_AGO);
		manager.onTick(item);
		verify(item, never()).restart();

		//stopping just the playing item would make the controller start the next one
		Broadcast unwatchedPlaylist = fixture.row("unwatched-playlist", "fake://unwatched-playlist");
		unwatchedPlaylist.setType(AntMediaApplicationAdapter.PLAY_LIST);
		unwatchedPlaylist.setAutoStartStopEnabled(true);
		unwatchedPlaylist.setStartTime(System.currentTimeMillis() - LONG_AGO);

		StreamFetcher playlistItem = tickFetcher("unwatched-playlist", State.STREAMING, LONG_AGO);
		manager.onTick(playlistItem);
		verify(playlistItem, never()).stopStream();
	}

	@Test
	void aSourceStillBeingReachedIsKeptFromDecaying() {
		fixture.row("waiting", "fake://waiting");

		StreamFetcher reconnecting = tickFetcher("waiting", State.RECONNECT_WAIT, LONG_AGO);
		manager.onTick(reconnecting);

		ArgumentCaptor<BroadcastUpdate> written = ArgumentCaptor.forClass(BroadcastUpdate.class);
		verify(fixture.dataStore).updateBroadcastFields(eq("waiting"), written.capture());

		BroadcastUpdate refresh = written.getValue();
		assertEquals(IAntMediaStreamHandler.BROADCAST_STATUS_PREPARING, refresh.getStatus());
		assertNotNull(refresh.getUpdateTime());
		assertNull(refresh.getStartTime(), "the auto stop timeout measures from the start time, it must not move");

		//a source that is up needs no refresh, its packets already keep it fresh
		fixture.row("live", "fake://live");
		manager.onTick(tickFetcher("live", State.STREAMING, LONG_AGO));
		verify(fixture.dataStore, never()).updateBroadcastFields(eq("live"), any());
	}

	@Test
	void isStreamRunningLooksPastThisNode() throws Exception {
		Broadcast free = fixture.row("free", "fake://free");
		assertFalse(manager.isStreamRunning(free));

		manager.startStreaming(free);
		assertTrue(manager.isStreamRunning(free), "a registry entry is this node owning it");

		//a playlist between two items has no fetcher, but it is still running
		Broadcast playlist = playlistRow("between-items");
		manager.prepare = fetcher -> fetcher.script(FakeWorker.holding());
		assertTrue(manager.getPlaylistController().startPlaylist(playlist).isSuccess());
		assertTrue(manager.isStreamRunning(playlist));

		//the database says it is live, on a host that answers
		Broadcast owned = fixture.row("owned", "fake://owned");
		owned.setStatus(IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING);
		owned.setOriginAdress("127.0.0.1");
		assertTrue(manager.isStreamRunning(owned));

		//live on a node that cannot be reached, so this node is free to take it over
		owned.setOriginAdress("10.255.255.1");
		assertFalse(manager.isStreamRunning(owned));

		//a row that still says broadcasting but stopped being updated has decayed, nobody holds it
		owned.setOriginAdress("127.0.0.1");
		owned.setUpdateTime(System.currentTimeMillis() - AntMediaApplicationAdapter.STREAM_TIMEOUT_MS - 1000);
		assertFalse(manager.isStreamRunning(owned), "a stale broadcasting row is the stuck source, it must not block a restart");

		//a finished row is free whoever owned it
		owned.setStatus(IAntMediaStreamHandler.BROADCAST_STATUS_FINISHED);
		owned.setUpdateTime(System.currentTimeMillis());
		assertFalse(manager.isStreamRunning(owned));
	}

	@Test
	void bootResumeOnlyTakesTheSourcesNobodyElseWill() {
		Broadcast unattended = fixture.row("unattended", "fake://unattended");
		//a row the last run left behind, which is exactly why the resume has to force past the check
		unattended.setStatus(IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING);

		Broadcast onDemand = fixture.row("on-demand", "fake://on-demand");
		onDemand.setAutoStartStopEnabled(true);

		fixture.appSettings.setStartStreamFetcherAutomatically(false);
		manager.resumeUnattendedSources();
		assertTrue(manager.getStreamFetcherList().isEmpty(), "the setting is off, nothing may be resumed");

		fixture.appSettings.setStartStreamFetcherAutomatically(true);
		manager.resumeUnattendedSources();

		assertTrue(manager.getStreamFetcherList().containsKey("unattended"));
		assertFalse(manager.getStreamFetcherList().containsKey("on-demand"), "a source a viewer starts is not resumed at boot");
	}

	@Test
	void shutdownEndsThePlaylistsFirstAndThenEverythingElse() throws Exception {
		manager.startStreaming(fixture.row("plain-source", "fake://plain-source"));
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
		assertFalse(manager.startStreaming(fixture.row("too-late", "fake://too-late")).isSuccess(),
				"the pool is gone, a fetcher started now would retry forever");
	}

	@Test
	void stopStreamingHandlesEveryKindOfId() throws Exception {
		assertEquals("Stream id is not defined", manager.stopStreaming("   ", false).getMessage());

		Result unknown = manager.stopStreaming("nobody", false);
		assertFalse(unknown.isSuccess());
		assertTrue(unknown.getMessage().contains("No matching stream source"));

		manager.startStreaming(fixture.row("stop-async", "fake://stop-async"));
		ScriptedFetcher async = manager.fetchers.get(0);
		awaitState(async, State.CONNECTING);

		assertTrue(manager.stopStreaming("stop-async", false).isSuccess());
		async.releaseAll();
		awaitState(async, State.STOPPED);
		assertFalse(manager.getStreamFetcherList().containsKey("stop-async"));

		//a blocking stop only returns once the database says finished
		ScriptedManager quick = fixture.newManager();
		quick.prepare = fetcher -> fetcher.workerSupplier = FakeWorker::new;
		quick.startStreaming(fixture.row("stop-blocking", "fake://stop-blocking"));
		awaitState(quick.fetchers.get(0), State.RECONNECT_WAIT);

		assertTrue(quick.stopStreaming("stop-blocking", true).isSuccess());
		assertEquals(State.STOPPED, quick.fetchers.get(0).getState());
		assertTrue(quick.getStreamFetcherList().isEmpty());

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

	/** onTick only reads these three off a fetcher, so a mock says exactly what the test means. */
	private StreamFetcher tickFetcher(String streamId, State state, long inStateForMs) {
		StreamFetcher fetcher = mock(StreamFetcher.class);
		when(fetcher.getStreamId()).thenReturn(streamId);
		when(fetcher.getState()).thenReturn(state);
		when(fetcher.getStateSinceMs()).thenReturn(System.currentTimeMillis() - inStateForMs);
		return fetcher;
	}

	private Broadcast playlistRow(String streamId) {
		Broadcast playlist = fixture.row(streamId, "fake://" + streamId);
		playlist.setType(AntMediaApplicationAdapter.PLAY_LIST);
		playlist.setPlayListItemList(List.of(new PlayListItem("fake://item", AntMediaApplicationAdapter.VOD)));
		return playlist;
	}
}
