package io.antmedia.streamsource;

import static io.antmedia.streamsource.StreamSourceFixture.NO_RETRY_IN_THIS_TEST_MS;
import static io.antmedia.streamsource.StreamSourceFixture.awaitState;
import static io.antmedia.streamsource.StreamSourceFixture.peek;
import static io.antmedia.streamsource.StreamSourceFixture.poke;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.antmedia.rest.model.Result;
import io.antmedia.streamsource.StreamFetcher.IStreamFetcherListener;
import io.antmedia.streamsource.StreamFetcher.Reason;
import io.antmedia.streamsource.StreamFetcher.State;
import io.antmedia.streamsource.StreamSourceFixture.Recorder;
import io.antmedia.streamsource.StreamSourceFixture.ScriptedFetcher;
import io.antmedia.streamsource.StreamSourceFixture.Transition;

/**
 * The state machine on its own. No ffmpeg, no database, no Spring: the workers are written by the
 * test and the manager's seat is taken by a {@link Recorder}.
 */
class StreamFetcherTest {

	/** The public ways into the engine, the rows of the state x event matrix. */
	private enum Entry {
		START, STOP, RESTART, SEEK, FIRST_PACKET, TICK
	}

	private StreamSourceFixture fixture;

	@BeforeEach
	void before() {
		fixture = new StreamSourceFixture();
		//a retry that fires behind an assertion would make every test below a coin flip
		fixture.appSettings.setStreamFetcherRetryDelayMs(NO_RETRY_IN_THIS_TEST_MS);
	}

	@AfterEach
	void after() {
		fixture.close();
	}

	@Test
	void startsOnceAndOnlyWithAPoolToRunOn() {
		Recorder recorder = new Recorder();
		ScriptedFetcher fetcher = inState(State.CONNECTING, "start", recorder);

		fetcher.startStream();
		fetcher.startStream();
		fixture.settle();

		assertEquals(State.CONNECTING, fetcher.getState());
		assertEquals(1, fetcher.workers.size(), "a second start must not open a second attempt");
		assertEquals(List.of(State.CONNECTING), recorder.states());

		//without a pool nothing could ever end an attempt, so it would abort and retry forever
		Recorder noPoolRecorder = new Recorder();
		ScriptedFetcher noPool = fixture.newFetcher("no-pool", noPoolRecorder);
		noPool.initialize(fixture.context, null, noPoolRecorder);
		noPool.startStream();

		awaitState(noPool, State.STOPPED);
		assertEquals(Reason.NO_RETRY, noPool.getLastReason());
		assertTrue(noPool.workers.isEmpty());
		assertEquals(-1L, peek(noPool, "tickTimerId"), "a fetcher that never started must not leave a timer");
	}

	@Test
	void stopCompletesFromEveryState() throws Exception {
		for (State state : State.values()) {
			Recorder recorder = new Recorder();
			ScriptedFetcher fetcher = inState(state, "stop-" + state, recorder);

			CompletableFuture<Void> stopped = fetcher.stopStream();
			fetcher.releaseAll();
			stopped.get(15, TimeUnit.SECONDS);

			assertEquals(State.STOPPED, fetcher.getState(), "stopping from " + state);
			assertSame(stopped, fetcher.stopStream(), "every caller waits on the same future");

			boolean hadAnAttemptToTearDown = state != State.IDLE && state != State.RECONNECT_WAIT;
			assertEquals(hadAnAttemptToTearDown, recorder.states().contains(State.STOPPING), "teardown path from " + state);
			assertEquals(-1L, peek(fetcher, "tickTimerId"), "tick timer left armed after stopping from " + state);
			assertEquals(-1L, peek(fetcher, "retryTimerId"), "retry timer left armed after stopping from " + state);
		}
	}

	@Test
	void restartOnlyActsWhereThereIsSomethingToReconnect() {
		for (State state : List.of(State.IDLE, State.CONNECTING, State.STOPPING, State.STOPPED)) {
			ScriptedFetcher fetcher = inState(state, "restart-ignored-" + state, new Recorder());
			int attemptsBefore = fetcher.workers.size();

			fetcher.restart();
			fixture.settle();

			assertEquals(state, fetcher.getState(), "restart must be ignored in " + state);
			assertEquals(attemptsBefore, fetcher.workers.size());
			fetcher.releaseAll();
		}

		//STREAMING drops the connection and comes back under the same fetcher and the same registration
		ScriptedFetcher streaming = fixture.newFetcher("restart-streaming", new Recorder());
		streaming.script(FakeWorker.publishing(), FakeWorker.holding());
		streaming.startStream();
		awaitState(streaming, State.STREAMING);

		streaming.restart();
		awaitState(streaming, State.STOPPING);
		streaming.workers.get(0).release();

		awaitState(streaming, State.CONNECTING);
		assertEquals(2, streaming.workers.size());

		//RECONNECT_WAIT connects now instead of sitting out a retry timer that is ten minutes away
		ScriptedFetcher waiting = inState(State.RECONNECT_WAIT, "restart-waiting", new Recorder());
		waiting.restart();
		awaitState(waiting, State.CONNECTING);
		assertEquals(2, waiting.workers.size());
	}

	@Test
	void anAttemptTheEngineGaveUpOnCannotComeBack() {
		Recorder recorder = new Recorder();
		ScriptedFetcher fetcher = fixture.newFetcher("abandoned", recorder);
		fetcher.script(FakeWorker.publishing(), FakeWorker.holding());

		fetcher.startStream();
		awaitState(fetcher, State.STREAMING);

		FakeWorker stuck = fetcher.workers.get(0);
		fetcher.restart();
		awaitState(fetcher, State.STOPPING);

		//the attempt is still blocked inside its native call, so the tick past the deadline moves on without it
		poke(fetcher, "stateSinceMs", System.currentTimeMillis() - StreamFetcher.STOPPING_TIMEOUT_MS - 1);
		fixture.tick(fetcher);

		awaitState(fetcher, State.CONNECTING);
		assertNotEquals(0L, stuck.abandonedAtMs, "an abandoned worker must be stamped, it may still touch shared state");
		assertEquals(2, fetcher.workers.size());

		//now the abandoned attempt finally returns, into a fetcher that has moved on
		Runnable latePacket = stuck.onFirstPacket;
		stuck.release();
		latePacket.run();

		Awaitility.await("the late attempt is ignored")
				.during(500, TimeUnit.MILLISECONDS)
				.atMost(5, TimeUnit.SECONDS)
				.until(() -> fetcher.getState() == State.CONNECTING);
		assertEquals(2, fetcher.workers.size());
	}

	@Test
	void retriesUntilTheAttemptBudgetRunsOut() {
		fixture.appSettings.setStreamFetcherRetryDelayMs(10);

		//zero means give up after the first failure
		fixture.appSettings.setStreamFetcherMaxRetryAttempts(0);
		ScriptedFetcher noRetries = fixture.newFetcher("retry-none", new Recorder());
		noRetries.startStream();
		awaitState(noRetries, State.STOPPED);
		assertEquals(Reason.RETRIES_EXHAUSTED, noRetries.getLastReason());
		assertEquals(1, noRetries.workers.size());

		//two means the first attempt plus two more
		fixture.appSettings.setStreamFetcherMaxRetryAttempts(2);
		ScriptedFetcher twoRetries = fixture.newFetcher("retry-two", new Recorder());
		twoRetries.startStream();
		awaitState(twoRetries, State.STOPPED);
		assertEquals(Reason.RETRIES_EXHAUSTED, twoRetries.getLastReason());
		assertEquals(3, twoRetries.workers.size());

		//an attempt that published clears the budget, so a flaky source is never given up on
		ScriptedFetcher recovering = fixture.newFetcher("retry-reset", new Recorder());
		recovering.script(FakeWorker.ending(Reason.EOF), FakeWorker.ending(Reason.EOF), FakeWorker.publishesThenEnds());
		recovering.startStream();
		awaitState(recovering, State.STOPPED);
		assertEquals(Reason.RETRIES_EXHAUSTED, recovering.getLastReason());
		assertEquals(5, recovering.workers.size(), "the two attempts before the one that published must not count");
	}

	@Test
	void theRetryDelayIsReadWhenItIsUsedAndNeverTrustedToBeSane() {
		//the fetcher was built while the delay was ten minutes, lowering it now has to take effect
		ScriptedFetcher live = fixture.newFetcher("retry-live", new Recorder());
		fixture.appSettings.setStreamFetcherRetryDelayMs(20);
		live.startStream();

		Awaitility.await("the lowered delay is used")
				.atMost(10, TimeUnit.SECONDS)
				.until(() -> live.workers.size() >= 3);
		live.stopStream();

		//vert.x refuses a timer shorter than 1ms, and that throw used to strand the fetcher in RECONNECT_WAIT
		ScriptedFetcher zeroDelay = fixture.newFetcher("retry-zero", new Recorder());
		fixture.appSettings.setStreamFetcherRetryDelayMs(0);
		zeroDelay.startStream();

		Awaitility.await("a zero delay still retries")
				.atMost(10, TimeUnit.SECONDS)
				.until(() -> zeroDelay.workers.size() >= 3);
		zeroDelay.stopStream();
	}

	@Test
	void anArmedRetryTimerCannotOutliveWhatArmedIt() {
		fixture.appSettings.setStreamFetcherRetryDelayMs(1000);

		//a stop landed while the retry was armed, the timer must not bring the source back
		ScriptedFetcher stopped = fixture.newFetcher("retry-after-stop", new Recorder());
		stopped.startStream();
		awaitState(stopped, State.RECONNECT_WAIT);
		assertNotEquals(-1L, peek(stopped, "retryTimerId"));

		stopped.stopStream();
		awaitState(stopped, State.STOPPED);
		assertEquals(-1L, peek(stopped, "retryTimerId"));

		Awaitility.await("the retry timer stays dead")
				.during(1500, TimeUnit.MILLISECONDS)
				.atMost(6, TimeUnit.SECONDS)
				.until(() -> stopped.getState() == State.STOPPED);
		assertEquals(1, stopped.workers.size());

		//a restart during the wait connects now, and the timer it replaced must not add a second attempt
		ScriptedFetcher restarted = fixture.newFetcher("retry-after-restart", new Recorder());
		restarted.script(FakeWorker.ending(Reason.EOF), FakeWorker.holding());
		restarted.startStream();
		awaitState(restarted, State.RECONNECT_WAIT);
		restarted.restart();
		awaitState(restarted, State.CONNECTING);

		Awaitility.await("the replaced timer never fires")
				.during(1500, TimeUnit.MILLISECONDS)
				.atMost(6, TimeUnit.SECONDS)
				.until(() -> restarted.getState() == State.CONNECTING);
		assertEquals(2, restarted.workers.size());
		restarted.releaseAll();
	}

	@Test
	void givesUpForGoodWhenRetryingWouldBePointless() {
		//restartStream=false is how a playlist item is started: one attempt, then it is the playlist's call
		ScriptedFetcher once = fixture.newFetcher("no-restart", new Recorder());
		once.setRestartStream(false);
		once.startStream();
		awaitState(once, State.STOPPED);
		assertEquals(Reason.NO_RETRY, once.getLastReason());
		assertEquals(1, once.workers.size());

		//the row was already gone when the attempt was about to open, so nothing is even tried
		ScriptedFetcher deletedBefore = fixture.newFetcher("deleted-before", new Recorder());
		fixture.rows.remove("deleted-before");
		deletedBefore.startStream();
		awaitState(deletedBefore, State.STOPPED);
		assertEquals(Reason.DELETED, deletedBefore.getLastReason());
		assertEquals(0, deletedBefore.workers.get(0).runs.get());

		//the row goes while the attempt runs, so it is the retry decision that has to catch it
		ScriptedFetcher deletedDuring = fixture.newFetcher("deleted-during", new Recorder());
		deletedDuring.script(FakeWorker.holding());
		deletedDuring.startStream();
		awaitState(deletedDuring, State.CONNECTING);
		fixture.rows.remove("deleted-during");
		deletedDuring.releaseAll();
		awaitState(deletedDuring, State.STOPPED);
		assertEquals(Reason.DELETED, deletedDuring.getLastReason());
		assertEquals(1, deletedDuring.workers.size());

		//deleted halfway through a teardown that was about to reconnect
		ScriptedFetcher deletedInStopping = fixture.newFetcher("deleted-stopping", new Recorder());
		deletedInStopping.script(FakeWorker.publishing());
		deletedInStopping.startStream();
		awaitState(deletedInStopping, State.STREAMING);
		deletedInStopping.restart();
		awaitState(deletedInStopping, State.STOPPING);
		fixture.rows.remove("deleted-stopping");
		deletedInStopping.releaseAll();
		awaitState(deletedInStopping, State.STOPPED);
		assertEquals(Reason.DELETED, deletedInStopping.getLastReason());
	}

	@Test
	void theTickRescuesAnAttemptThatWentSilent() {
		//an active state with no worker behind it: nothing is pulling and nothing would ever end it
		Recorder recorder = new Recorder();
		ScriptedFetcher lost = fixture.newFetcher("tick-no-worker", recorder);
		lost.script(FakeWorker.holding());
		lost.startStream();
		awaitState(lost, State.CONNECTING);

		poke(lost, "currentWorker", null);
		fixture.tick(lost);

		assertEquals(State.RECONNECT_WAIT, lost.getState());
		assertEquals(Reason.READ_ERROR, lost.getLastReason());
		assertTrue(recorder.ticks.get() >= 1, "the owner still gets its maintenance call");

		//the worker it lost track of returns late, into a fetcher that is already waiting to retry
		lost.releaseAll();
		Awaitility.await("the lost worker cannot change anything")
				.during(400, TimeUnit.MILLISECONDS)
				.atMost(5, TimeUnit.SECONDS)
				.until(() -> lost.getState() == State.RECONNECT_WAIT);

		//an open that delivers nothing is aborted and counts as a failure
		ScriptedFetcher silent = fixture.newFetcher("tick-silent", new Recorder());
		silent.script(FakeWorker.publishing());
		silent.startStream();
		awaitState(silent, State.STREAMING);

		silent.workers.get(0).lastActivityMs = 0;
		fixture.tick(silent);
		awaitState(silent, State.STOPPING);
		assertTrue(silent.workers.get(0).abortRequested.get());

		silent.releaseAll();
		awaitState(silent, State.RECONNECT_WAIT);
		assertEquals(Reason.TIMEOUT, silent.getLastReason());

		//nothing left to maintain once it is terminal
		Recorder deadRecorder = new Recorder();
		ScriptedFetcher dead = inState(State.STOPPED, "tick-dead", deadRecorder);
		int ticksBefore = deadRecorder.ticks.get();
		fixture.tick(dead);
		assertEquals(ticksBefore, deadRecorder.ticks.get());
	}

	@Test
	void theListenerSeesEveryTransitionAndCannotStrandTheEngine() throws Exception {
		Recorder recorder = new Recorder();
		ScriptedFetcher fetcher = fixture.newFetcher("listener", recorder);
		fetcher.script(FakeWorker.publishesThenEnds(), FakeWorker.holding());
		fixture.appSettings.setStreamFetcherRetryDelayMs(20);

		IStreamFetcherListener legacy = mock(IStreamFetcherListener.class);
		fetcher.setStreamFetcherListener(legacy);

		fetcher.startStream();
		Awaitility.await("the second attempt is connecting")
				.atMost(15, TimeUnit.SECONDS)
				.until(() -> fetcher.workers.size() == 2 && fetcher.getState() == State.CONNECTING);

		//published is what tells the manager an attempt that was on air just ended
		assertEquals(List.of(
				new Transition(State.IDLE, State.CONNECTING, false),
				new Transition(State.CONNECTING, State.STREAMING, true),
				new Transition(State.STREAMING, State.RECONNECT_WAIT, true),
				new Transition(State.RECONNECT_WAIT, State.CONNECTING, false)), recorder.transitions);

		verify(legacy, timeout(5000)).streamStarted(legacy);
		verify(legacy, never()).streamFinished(any());

		//a listener that throws must not keep the engine from finishing its teardown
		recorder.failWith = new IllegalStateException("this listener is broken");
		CompletableFuture<Void> stopped = fetcher.stopStream();
		fetcher.releaseAll();
		stopped.get(15, TimeUnit.SECONDS);

		assertEquals(State.STOPPED, fetcher.getState());
		verify(legacy, timeout(5000)).streamFinished(legacy);
	}

	@Test
	void aPendingStopBeatsAPendingReconnect() throws Exception {
		//reconnect asked for first, then a stop: the stop wins and the source does not come back
		ScriptedFetcher stopWins = inState(State.STREAMING, "stop-wins", new Recorder());
		stopWins.restart();
		awaitState(stopWins, State.STOPPING);
		stopWins.stopStream();
		fixture.settle();
		stopWins.releaseAll();

		awaitState(stopWins, State.STOPPED);
		assertEquals(Reason.STOP_REQUESTED, stopWins.getLastReason());
		assertEquals(1, stopWins.workers.size(), "the pending reconnect must not have opened an attempt");

		//stop first, then a reconnect: a reconnect is not allowed to undo a stop that already landed
		ScriptedFetcher stopStays = inState(State.STREAMING, "stop-stays", new Recorder());
		stopStays.stopStream();
		awaitState(stopStays, State.STOPPING);
		stopStays.restart();
		fixture.settle();
		stopStays.releaseAll();

		awaitState(stopStays, State.STOPPED);
		assertEquals(Reason.STOP_REQUESTED, stopStays.getLastReason());
		assertEquals(1, stopStays.workers.size());

		//two stops in a row are one stop
		Recorder twiceRecorder = new Recorder();
		ScriptedFetcher twice = inState(State.STREAMING, "stop-twice", twiceRecorder);
		CompletableFuture<Void> first = twice.stopStream();
		CompletableFuture<Void> second = twice.stopStream();
		twice.releaseAll();
		first.get(15, TimeUnit.SECONDS);
		second.get(15, TimeUnit.SECONDS);

		assertEquals(1, twiceRecorder.states().stream().filter(state -> state == State.STOPPED).count());

		//a listener that stops the fetcher from inside the callback it is being told about
		Recorder reentrant = new Recorder();
		reentrant.hook = (fetcher, to) -> {
			if (to == State.STREAMING) {
				fetcher.stopStream();
			}
		};
		ScriptedFetcher fromCallback = fixture.newFetcher("stop-from-callback", reentrant);
		fromCallback.script(FakeWorker.publishing());
		fromCallback.startStream();
		awaitState(fromCallback, State.STOPPING);
		fromCallback.releaseAll();
		awaitState(fromCallback, State.STOPPED);
	}

	/**
	 * Migrated from StreamFetcherUnitTest#testStreamFinishedCalledAfterBroadcastClosed. The manager
	 * closes the broadcast from onTransition, and a reconnect listener restarts the stream from
	 * streamFinished. Fire them the other way round and the close lands on the stream the listener
	 * has just restarted.
	 */
	@Test
	void theOwnerIsToldBeforeTheReconnectListenerIs() throws Exception {
		List<String> order = new CopyOnWriteArrayList<>();

		Recorder recorder = new Recorder();
		recorder.hook = (fetcher, to) -> order.add("owner:" + to);

		ScriptedFetcher fetcher = fixture.newFetcher("callback-order", recorder);
		fetcher.script(FakeWorker.publishing());
		fetcher.setStreamFetcherListener(new IStreamFetcherListener() {

			@Override
			public void streamStarted(IStreamFetcherListener listener) {
				order.add("streamStarted");
			}

			@Override
			public void streamFinished(IStreamFetcherListener listener) {
				order.add("streamFinished");
			}
		});

		fetcher.startStream();
		awaitState(fetcher, State.STREAMING);

		CompletableFuture<Void> stopped = fetcher.stopStream();
		fetcher.releaseAll();
		stopped.get(15, TimeUnit.SECONDS);

		assertEquals(List.of(
				"owner:CONNECTING",
				"owner:STREAMING", "streamStarted",
				"owner:STOPPING",
				"owner:STOPPED", "streamFinished"), order);
	}

	/**
	 * Migrated from StreamFetcherUnitTest#testCameraErrorCodes and #testCameraStartedProperly, which
	 * needed a real camera and a real ffmpeg to assert what is an engine rule: the running attempt owns
	 * the error, and once it is gone the last one it produced is what an operator gets to see.
	 */
	@Test
	void theCameraErrorOutlivesTheAttemptThatProducedIt() {
		ScriptedFetcher healthy = fixture.newFetcher("camera-ok", new Recorder());
		FakeWorker opened = FakeWorker.publishing();
		opened.error = new Result(true);
		healthy.script(opened);

		healthy.startStream();
		awaitState(healthy, State.STREAMING);

		assertTrue(healthy.getCameraError().isSuccess());

		healthy.getMuxAdaptor();
		assertEquals(1, opened.muxAdaptorReads.get(), "the mux adaptor is asked of the running attempt");

		ScriptedFetcher failing = fixture.newFetcher("camera-error", new Recorder());
		FakeWorker refused = FakeWorker.holding();
		refused.error = new Result(false, "Connection refused");
		failing.script(refused, FakeWorker.holding());

		failing.startStream();
		awaitState(failing, State.CONNECTING);
		assertEquals("Connection refused", failing.getCameraError().getMessage());

		refused.release();
		awaitState(failing, State.RECONNECT_WAIT);

		//the worker that knew why is gone, the reason must not go with it
		assertEquals("Connection refused", failing.getCameraError().getMessage());
		assertNull(failing.getMuxAdaptor(), "no attempt is running, so there is no mux adaptor");
		assertEquals(0, refused.muxAdaptorReads.get(), "a worker the fetcher let go of is never asked again");
	}

	@Test
	void seekOnlyReachesAnAttemptThatIsRunning() {
		ScriptedFetcher streaming = inState(State.STREAMING, "seek-live", new Recorder());
		streaming.seekTime(4242);
		fixture.settle();
		assertEquals(4242L, streaming.workers.get(0).seekedToMs);

		//nothing to forward to, but the value has to be there for the attempt that comes next
		ScriptedFetcher idle = fixture.newFetcher("seek-idle", new Recorder());
		idle.seekTime(777);
		fixture.settle();
		assertEquals(777L, peek(idle, "seekTimeMs"));
	}

	/**
	 * Every entry point against every state. Nothing may throw, nothing may end up in a state it has
	 * no way out of, and a live fetcher always has the timer that will move it on.
	 */
	@Test
	void everyEntryPointIsSafeInEveryState() {
		for (State from : State.values()) {
			for (Entry entry : Entry.values()) {
				if (from == State.IDLE && entry == Entry.FIRST_PACKET) {
					//there is no attempt to deliver a packet for, the callback is created with one
					continue;
				}

				String label = from + " + " + entry;
				ScriptedFetcher fetcher = inState(from, "matrix-" + from + "-" + entry, new Recorder());

				fire(fetcher, entry);

				State after = expected(from, entry);
				assertEquals(after, fetcher.getState(), label);

				long tickTimer = (long) peek(fetcher, "tickTimerId");
				long retryTimer = (long) peek(fetcher, "retryTimerId");

				if (after == State.STOPPED) {
					assertEquals(-1L, tickTimer, "tick timer left armed after " + label);
					assertEquals(-1L, retryTimer, "retry timer left armed after " + label);
				}
				else if (!fetcher.workers.isEmpty()) {
					assertNotEquals(-1L, tickTimer, "nothing left to move this fetcher on after " + label);
				}
				if (after == State.RECONNECT_WAIT) {
					assertNotEquals(-1L, retryTimer, "nothing left to retry after " + label);
				}

				fetcher.releaseAll();
			}
		}
	}

	private void fire(ScriptedFetcher fetcher, Entry entry) {
		switch (entry) {
			case START -> fetcher.startStream();
			case STOP -> fetcher.stopStream();
			case RESTART -> fetcher.restart();
			case SEEK -> fetcher.seekTime(4242);
			case FIRST_PACKET -> fetcher.workers.get(fetcher.workers.size() - 1).onFirstPacket.run();
			case TICK -> fixture.tick(fetcher);
		}
		fixture.settle();
	}

	private static State expected(State from, Entry entry) {
		return switch (entry) {
			case START -> from == State.IDLE ? State.CONNECTING : from;
			case STOP -> switch (from) {
				case IDLE, RECONNECT_WAIT -> State.STOPPED;
				case CONNECTING, STREAMING -> State.STOPPING;
				default -> from;
			};
			case RESTART -> switch (from) {
				case STREAMING -> State.STOPPING;
				case RECONNECT_WAIT -> State.CONNECTING;
				default -> from;
			};
			case FIRST_PACKET -> from == State.CONNECTING ? State.STREAMING : from;
			case SEEK, TICK -> from;
		};
	}

	/**
	 * A fetcher parked in one state. The workers hold and ignore the abort, so a state stays put until
	 * the test releases them.
	 */
	private ScriptedFetcher inState(State target, String streamId, Recorder recorder) {
		ScriptedFetcher fetcher = fixture.newFetcher(streamId, recorder);

		switch (target) {
			case IDLE -> {
				//a fetcher that was built but never started
			}
			case CONNECTING -> {
				fetcher.script(FakeWorker.holding());
				fetcher.startStream();
				awaitState(fetcher, State.CONNECTING);
			}
			case STREAMING -> {
				fetcher.script(FakeWorker.publishing());
				fetcher.startStream();
				awaitState(fetcher, State.STREAMING);
			}
			case RECONNECT_WAIT -> {
				fetcher.script(FakeWorker.ending(Reason.EOF), FakeWorker.holding());
				fetcher.startStream();
				awaitState(fetcher, State.RECONNECT_WAIT);
			}
			case STOPPING -> {
				fetcher.script(FakeWorker.holding());
				fetcher.startStream();
				awaitState(fetcher, State.CONNECTING);
				fetcher.stopStream();
				awaitState(fetcher, State.STOPPING);
			}
			case STOPPED -> {
				fetcher.script(FakeWorker.holding());
				fetcher.startStream();
				awaitState(fetcher, State.CONNECTING);
				fetcher.stopStream();
				fetcher.releaseAll();
				awaitState(fetcher, State.STOPPED);
			}
		}

		return fetcher;
	}
}
