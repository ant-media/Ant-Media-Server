package io.antmedia.streamsource;

import static io.antmedia.streamsource.StreamSourceFixture.NO_RETRY_IN_THIS_TEST_MS;
import static io.antmedia.streamsource.StreamSourceFixture.awaitState;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.datastore.db.types.Broadcast.PlayListItem;
import io.antmedia.muxer.IAntMediaStreamHandler;
import io.antmedia.rest.model.Result;
import io.antmedia.streamsource.StreamFetcher.State;
import io.antmedia.streamsource.StreamSourceFixture.ScriptedFetcher;
import io.antmedia.streamsource.StreamSourceFixture.ScriptedManager;
import io.vertx.core.Context;

/**
 * The controller's own job: which item comes next and when the playlist is over. Items are real
 * {@link StreamFetcher}s on scripted workers, so an item ends exactly when the test says it does.
 *
 * The url check runs against a local http server rather than a stub, because which urls are asked at
 * all is half of what this class decides.
 */
class PlaylistControllerTest {

	private StreamSourceFixture fixture;
	private ScriptedManager manager;
	private PlaylistController controller;
	private Context context;

	/** The worker each started item gets, in order. Past the end an item holds, so the playlist parks there. */
	private final Queue<Supplier<FakeWorker>> itemWorkers = new ConcurrentLinkedQueue<>();

	private volatile Consumer<ScriptedFetcher> onItemMade;

	private HttpServer probe;
	private final List<String> probed = new CopyOnWriteArrayList<>();
	private final CountDownLatch releaseSlowProbe = new CountDownLatch(1);

	@BeforeEach
	void before() throws IOException {
		probe = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		probe.createContext("/", this::answerProbe);
		probe.start();

		fixture = new StreamSourceFixture();
		fixture.appSettings.setStreamFetcherRetryDelayMs(NO_RETRY_IN_THIS_TEST_MS);

		manager = fixture.newManager();
		controller = manager.getPlaylistController();
		context = manager.context();

		manager.prepare = fetcher -> {
			Supplier<FakeWorker> next = itemWorkers.poll();
			if (next != null) {
				fetcher.workerSupplier = next;
			}
			if (onItemMade != null) {
				onItemMade.accept(fetcher);
			}
		};
	}

	@AfterEach
	void after() {
		releaseSlowProbe.countDown();
		probe.stop(0);

		manager.getStreamFetcherList().values().forEach(StreamFetcher::stopStream);
		manager.fetchers.forEach(ScriptedFetcher::releaseAll);
		fixture.close();
	}

	@Test
	void everyWayInRefusesWhatItCannotPlay() {
		Broadcast plain = fixture.row("plain", "fake://plain");
		assertFalse(controller.startPlaylist(plain).isSuccess(), "an ordinary source is not a playlist");

		Broadcast empty = playlist("empty");
		assertFalse(controller.startPlaylist(empty).isSuccess());
		empty.setPlayListItemList(null);
		assertFalse(controller.startPlaylist(empty).isSuccess());

		assertFalse(controller.playItem("nobody", 0).isSuccess(), "there is no such broadcast");
		assertFalse(controller.playItem("plain", 0).isSuccess());
		assertFalse(controller.playItem("empty", 0).isSuccess());

		Broadcast list = playlist("list", "rtsp://a", "rtsp://b");
		assertFalse(controller.playItem("list", 0).isSuccess(), "nothing to skip in a playlist that is not playing");

		assertTrue(controller.startPlaylist(list).isSuccess());
		assertFalse(controller.startPlaylist(list).isSuccess(), "the session is what owns the playlist");

		awaitItems("list", 1);
		assertFalse(controller.playItem("list", 2).isSuccess(), "index 2 of a two item list");
		assertEquals(0, list.getCurrentPlayIndex(), "a refused skip leaves the playlist where it was");

		//an index left over from a list that was edited since starts from the top instead of refusing
		for (int stored : List.of(-3, 9)) {
			Broadcast wild = playlist("wild" + stored, "rtsp://a", "rtsp://b");
			wild.setCurrentPlayIndex(stored);

			assertTrue(controller.startPlaylist(wild).isSuccess());
			awaitItems("wild" + stored, 1);
			assertEquals(0, wild.getCurrentPlayIndex());
		}
	}

	@Test
	void itemsPlayOneAfterAnotherAndNeverOverlap() {
		List<Boolean> previousStillRegistered = new CopyOnWriteArrayList<>();
		onItemMade = fetcher -> previousStillRegistered.add(manager.getStreamFetcherList().containsKey("queue"));

		//one item that plays and ends, one that ends without ever publishing
		itemWorkers.add(FakeWorker::publishesThenEnds);
		itemWorkers.add(FakeWorker::new);

		Broadcast queue = playlist("queue", "rtsp://a", "rtsp://b", "rtsp://c");
		assertTrue(controller.startPlaylist(queue).isSuccess());
		awaitItems("queue", 3);

		assertEquals(List.of(false, false, false), previousStillRegistered,
				"an item may only start once the one before it is completely gone");
		assertEquals(2, queue.getCurrentPlayIndex());
		assertEquals(IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING, queue.getPlayListStatus());
		assertEquals(1, itemsOf("queue").get(1).workers.size(), "an item never retries itself, the controller moves on");

		List<PlayListItem> edited = new ArrayList<>(queue.getPlayListItemList());
		edited.add(new PlayListItem("rtsp://added", AntMediaApplicationAdapter.VOD));
		queue.setPlayListItemList(edited);

		itemsOf("queue").forEach(ScriptedFetcher::releaseAll);
		awaitItems("queue", 4);
		assertEquals("rtsp://added", itemsOf("queue").get(3).getStreamUrl(),
				"the list is read again for every item, so an edit made while it plays lands");
	}

	@Test
	void aSecondItemIsRefusedWhileOneIsStartingOrPlaying() {
		Broadcast playing = playlist("playing", "rtsp://a", "rtsp://b");
		itemWorkers.add(FakeWorker::publishing);

		assertTrue(controller.startPlaylist(playing).isSuccess());
		awaitItems("playing", 1);
		awaitState(itemsOf("playing").get(0), State.STREAMING);

		//a late or duplicate stopped callback while the item is still up must not put a second one under the id
		stopped("playing", true);
		assertStaysAt("playing", 1);
		assertEquals(0, playing.getCurrentPlayIndex());

		Broadcast checking = playlist("checking", url("/slow"), "rtsp://b");
		assertTrue(controller.startPlaylist(checking).isSuccess());
		awaitProbed("/slow");

		//the same, for an item that is not registered yet because its url is still being checked
		stopped("checking", false);
		releaseSlowProbe.countDown();

		awaitItems("checking", 1);
		assertStaysAt("checking", 1);
		assertEquals(0, checking.getCurrentPlayIndex());
	}

	@Test
	void aSkipIsHonouredWhereverTheItemIs() {
		Broadcast skipping = playlist("skipping", "rtsp://a", "rtsp://b", "rtsp://c");
		itemWorkers.add(FakeWorker::publishing);

		assertTrue(controller.startPlaylist(skipping).isSuccess());
		awaitItems("skipping", 1);
		awaitState(itemsOf("skipping").get(0), State.STREAMING);

		//a negative index means the one after the item that is playing
		Result skipped = controller.playItem("skipping", -1);
		assertTrue(skipped.isSuccess());
		assertEquals("Playing item:1", skipped.getMessage());

		//the item that was playing has to reach its terminal state before the skip target comes up
		fixture.settle(context);
		assertEquals(1, itemsOf("skipping").size());

		itemsOf("skipping").forEach(ScriptedFetcher::releaseAll);
		awaitItems("skipping", 2);
		assertEquals("rtsp://b", itemsOf("skipping").get(1).getStreamUrl());
		assertEquals(1, skipping.getCurrentPlayIndex());

		//a skip that lands while an item is having its url checked is picked up, not dropped
		Broadcast checked = playlist("checked", url("/slow"), "rtsp://b", "rtsp://c");
		assertTrue(controller.startPlaylist(checked).isSuccess());
		awaitProbed("/slow");

		assertTrue(controller.playItem("checked", 2).isSuccess());
		releaseSlowProbe.countDown();

		awaitItems("checked", 1);
		assertEquals("rtsp://c", itemsOf("checked").get(0).getStreamUrl(), "the checked item must not start as well");
		assertEquals(2, checked.getCurrentPlayIndex());
	}

	@Test
	void theEndOfTheListWrapsOrFinishes() {
		Broadcast looping = playlist("looping", "rtsp://a", "rtsp://b");
		itemWorkers.add(FakeWorker::publishesThenEnds);
		itemWorkers.add(FakeWorker::publishesThenEnds);

		assertTrue(controller.startPlaylist(looping).isSuccess());
		awaitItems("looping", 3);
		assertEquals(0, looping.getCurrentPlayIndex(), "the third item is the first one again");
		assertTrue(controller.isRunning("looping"));

		Broadcast once = playlist("once", "rtsp://a", "rtsp://b");
		once.setPlaylistLoopEnabled(false);
		itemWorkers.add(FakeWorker::publishesThenEnds);
		itemWorkers.add(FakeWorker::publishesThenEnds);

		assertTrue(controller.startPlaylist(once).isSuccess());
		awaitFinished("once");
		assertEquals(2, itemsOf("once").size());
		assertEquals(0, once.getCurrentPlayIndex(), "a playlist that ran out rewinds, so the next start begins at the top");

		//asking for the next item while the last one of a non looping playlist plays ends it
		Broadcast last = playlist("last", "rtsp://a", "rtsp://b");
		last.setPlaylistLoopEnabled(false);
		last.setCurrentPlayIndex(1);

		assertTrue(controller.startPlaylist(last).isSuccess());
		awaitItems("last", 1);

		Result next = controller.playItem("last", -1);
		assertTrue(next.isSuccess());
		assertEquals("Playlist is finished, there was no next item to play", next.getMessage());
		awaitFinished("last");
	}

	@Test
	void onlyHttpItemsAreProbed() {
		Broadcast mixed = playlist("mixed", "http://127.0.0.1:1/refused", url("/gone"), url("/ok"),
				"rtsp://127.0.0.1:1/live", "srt://127.0.0.1:1");
		mixed.setPlaylistLoopEnabled(false);

		itemWorkers.add(FakeWorker::publishesThenEnds);
		itemWorkers.add(FakeWorker::new);

		assertTrue(controller.startPlaylist(mixed).isSuccess());
		awaitItems("mixed", 3);

		assertEquals(List.of("/gone", "/ok"), probed, "only http can be asked, everything else is worth trying");
		assertEquals("srt://127.0.0.1:1", itemsOf("mixed").get(2).getStreamUrl());
		assertEquals(4, mixed.getCurrentPlayIndex());
		assertTrue(controller.isRunning("mixed"));
	}

	@Test
	void aPassThatPlayedNothingBacksOffAndThenGivesUp() {
		Broadcast waiting = playlist("waiting", url("/gone-a"), url("/gone-b"));
		assertTrue(controller.startPlaylist(waiting).isSuccess());

		Awaitility.await("the playlist says it is trying again rather than playing")
				.atMost(15, TimeUnit.SECONDS)
				.until(() -> IAntMediaStreamHandler.BROADCAST_STATUS_PREPARING.equals(waiting.getPlayListStatus()));

		assertEquals(List.of("/gone-a", "/gone-b"), probed, "a list of dead urls must not spin the event loop");
		assertTrue(controller.isRunning("waiting"));

		probed.clear();
		fixture.appSettings.setStreamFetcherRetryDelayMs(1);
		fixture.appSettings.setStreamFetcherMaxRetryAttempts(1);

		Broadcast givingUp = playlist("giving-up", url("/gone-c"), url("/gone-d"));
		assertTrue(controller.startPlaylist(givingUp).isSuccess());
		awaitFinished("giving-up");

		//two passes, and each one walked the whole list: the stored index moves even when the check fails
		assertEquals(List.of("/gone-c", "/gone-d", "/gone-c", "/gone-d"), probed);
		assertEquals(0, givingUp.getCurrentPlayIndex());
	}

	@Test
	void everyWayAPlaylistEndsEarly() {
		assertEquals("Stream id is not defined", controller.stopPlaylist("  ").getMessage());
		assertEquals("Stream id is not defined", controller.stopPlaylist(null).getMessage());

		Result unknown = controller.stopPlaylist("nobody");
		assertFalse(unknown.isSuccess());
		assertTrue(unknown.getMessage().contains("not running"));

		Broadcast stopped = playlist("stopped", "rtsp://a", "rtsp://b", "rtsp://c");
		stopped.setCurrentPlayIndex(1);
		assertTrue(controller.startPlaylist(stopped).isSuccess());
		awaitItems("stopped", 1);

		//a stop that lands on a skip already waiting for the current item beats it
		assertTrue(controller.playItem("stopped", 2).isSuccess());

		Result byHand = controller.stopPlaylist("stopped");
		assertTrue(byHand.isSuccess());
		assertEquals("Playlist is stopped", byHand.getMessage());

		//the session goes first, so the item reaching STOPPED can no longer start the next one
		awaitFinished("stopped");
		itemsOf("stopped").forEach(ScriptedFetcher::releaseAll);
		assertStaysAt("stopped", 1);
		assertEquals(1, stopped.getCurrentPlayIndex(), "a stop keeps its place, only running out rewinds");

		//caught between two items: nothing to stop, but the session still has to go
		Broadcast between = playlist("between", url("/slow"), "rtsp://b");
		assertTrue(controller.startPlaylist(between).isSuccess());
		awaitProbed("/slow");

		controller.shutdown().orTimeout(15, TimeUnit.SECONDS).join();
		assertFalse(controller.isRunning("between"));
		assertEquals(IAntMediaStreamHandler.BROADCAST_STATUS_FINISHED, between.getPlayListStatus());

		releaseSlowProbe.countDown();
		assertStaysAt("between", 0);

		//a server on its way down finishes the playlist instead of picking up where it left off
		Broadcast closing = playlist("closing", "rtsp://a", "rtsp://b", "rtsp://c");
		closing.setCurrentPlayIndex(1);
		assertTrue(controller.startPlaylist(closing).isSuccess());
		awaitItems("closing", 1);

		manager.shuttingDown();
		assertTrue(controller.playItem("closing", 2).isSuccess(), "the skip is accepted, shutting down is not its call");
		itemsOf("closing").forEach(ScriptedFetcher::releaseAll);

		awaitFinished("closing");
		assertStaysAt("closing", 1);
		assertEquals(1, closing.getCurrentPlayIndex(), "shutting down beats the skip that was waiting");
	}

	@Test
	void aFailureWhileMovingOnEndsThePlaylist() {
		boolean[] databaseDown = { false };
		when(fixture.dataStore.get(anyString())).thenAnswer(call -> {
			if (databaseDown[0]) {
				throw new IllegalStateException("database is down");
			}
			return fixture.rows.get((String) call.getArgument(0));
		});

		Broadcast blip = playlist("blip", "rtsp://a", "rtsp://b");
		assertTrue(controller.startPlaylist(blip).isSuccess());
		awaitItems("blip", 1);

		//a throw while picking the next item would strand the playlist: nothing playing, nothing left
		//to restart it
		databaseDown[0] = true;
		itemsOf("blip").forEach(ScriptedFetcher::releaseAll);
		awaitFinished("blip");

		databaseDown[0] = false;
		assertStaysAt("blip", 1);
		assertEquals(0, blip.getCurrentPlayIndex());

		//an item that cannot even be started ends the playlist rather than leaving it with nothing on air
		onItemMade = fetcher -> fetcher.failOnStart = true;

		Broadcast unstartable = playlist("unstartable", "rtsp://a", "rtsp://b");
		unstartable.setCurrentPlayIndex(1);
		assertTrue(controller.startPlaylist(unstartable).isSuccess());

		awaitFinished("unstartable");
		assertTrue(manager.getStreamFetcherList().isEmpty(), "a registration that never started must be rolled back");
		assertEquals(0, unstartable.getCurrentPlayIndex());

		onItemMade = null;

		//the list is emptied while its last item plays, so there is nothing left to move on to
		Broadcast emptied = playlist("emptied", "rtsp://a");
		assertTrue(controller.startPlaylist(emptied).isSuccess());
		awaitItems("emptied", 1);

		emptied.setPlayListItemList(List.of());
		itemsOf("emptied").forEach(ScriptedFetcher::releaseAll);
		awaitFinished("emptied");

		//the playlist itself is deleted while it plays, so there is nothing left to write to either
		Broadcast gone = playlist("gone", "rtsp://a", "rtsp://b");
		assertTrue(controller.startPlaylist(gone).isSuccess());
		awaitItems("gone", 1);

		fixture.rows.remove("gone");
		itemsOf("gone").forEach(ScriptedFetcher::releaseAll);

		Awaitility.await("the deleted playlist lets go of its stream id")
				.atMost(15, TimeUnit.SECONDS)
				.until(() -> !controller.isRunning("gone"));
		assertStaysAt("gone", 1);
	}

	private Broadcast playlist(String streamId, String... urls) {
		Broadcast playlist = fixture.row(streamId, "fake://" + streamId);
		playlist.setType(AntMediaApplicationAdapter.PLAY_LIST);
		playlist.setPlayListItemList(List.of(urls).stream()
				.map(url -> new PlayListItem(url, AntMediaApplicationAdapter.VOD))
				.toList());
		return playlist;
	}

	private String url(String path) {
		return "http://127.0.0.1:" + probe.getAddress().getPort() + path;
	}

	private void answerProbe(HttpExchange exchange) throws IOException {
		String path = exchange.getRequestURI().getPath();
		probed.add(path);

		if (path.equals("/slow")) {
			try {
				releaseSlowProbe.await(15, TimeUnit.SECONDS);
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}

		exchange.sendResponseHeaders(path.startsWith("/gone") ? 404 : 200, -1);
		exchange.close();
	}

	private List<ScriptedFetcher> itemsOf(String streamId) {
		return manager.fetchers.stream().filter(fetcher -> streamId.equals(fetcher.getStreamId())).toList();
	}

	private void awaitItems(String streamId, int count) {
		Awaitility.await(streamId + " started " + count + " item(s)")
				.atMost(15, TimeUnit.SECONDS)
				.until(() -> itemsOf(streamId).size() >= count);
		fixture.settle(context);
	}

	private void awaitFinished(String streamId) {
		Awaitility.await(streamId + " is finished")
				.atMost(15, TimeUnit.SECONDS)
				.until(() -> !controller.isRunning(streamId)
						&& IAntMediaStreamHandler.BROADCAST_STATUS_FINISHED.equals(fixture.rows.get(streamId).getPlayListStatus()));
	}

	private void awaitProbed(String path) {
		Awaitility.await("the url check for " + path + " is in flight")
				.atMost(15, TimeUnit.SECONDS)
				.until(() -> probed.contains(path));
	}

	/** Proves nothing else came up afterwards, which a settle cannot: the url check is off context. */
	private void assertStaysAt(String streamId, int items) {
		Awaitility.await(streamId + " starts nothing else")
				.during(Duration.ofSeconds(1))
				.atMost(15, TimeUnit.SECONDS)
				.until(() -> itemsOf(streamId).size() == items);
	}

	/** The stopped callback the manager makes for every item, without an item behind it. */
	private void stopped(String streamId, boolean published) {
		StreamFetcher item = mock(StreamFetcher.class);
		when(item.getStreamId()).thenReturn(streamId);
		when(item.isPublished()).thenReturn(published);

		context.runOnContext(v -> controller.onItemStopped(item));
		fixture.settle(context);
	}
}
