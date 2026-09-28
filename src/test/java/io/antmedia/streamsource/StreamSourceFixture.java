package io.antmedia.streamsource;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.awaitility.Awaitility;
import org.red5.server.api.IContext;
import org.red5.server.api.scope.IScope;
import org.springframework.context.ApplicationContext;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.AppSettings;
import io.antmedia.datastore.db.DataStore;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.datastore.db.types.BroadcastUpdate;
import io.antmedia.licence.ILicenceService;
import io.antmedia.muxer.IAntMediaStreamHandler;
import io.antmedia.settings.ServerSettings;
import io.antmedia.streamsource.StreamFetcher.State;
import io.antmedia.streamsource.StreamFetcher.StateListener;
import io.vertx.core.Context;
import io.vertx.core.Vertx;

/**
 * Everything a {@link StreamFetcher} or a {@link StreamFetcherManager} reads at construction, without
 * Spring and without ffmpeg: a mocked scope answering the beans they look up, a data store backed by a
 * map, and one real vert.x.
 *
 * In this package because the seam is: {@link StreamFetcherWorker}'s fields and half of the manager
 * and controller API are package private.
 */
class StreamSourceFixture implements AutoCloseable {

	static final String STREAM_TYPE = "streamSource";

	/** Long enough that a retry never fires behind an assertion by accident. */
	static final long NO_RETRY_IN_THIS_TEST_MS = 600000;

	final Vertx vertx = Vertx.vertx();
	final Context context = vertx.getOrCreateContext();
	final ExecutorService pool;

	final AppSettings appSettings = new AppSettings();
	final ServerSettings serverSettings = mock(ServerSettings.class);
	final ILicenceService licenceService = mock(ILicenceService.class);
	final AntMediaApplicationAdapter app = mock(AntMediaApplicationAdapter.class);
	final DataStore dataStore = mock(DataStore.class);
	final IScope scope = mock(IScope.class);

	/** The rows {@link #dataStore} serves. Remove one to delete a broadcast mid test. */
	final Map<String, Broadcast> rows = new ConcurrentHashMap<>();

	StreamSourceFixture() {
		pool = Executors.newCachedThreadPool(runnable -> {
			Thread thread = new Thread(runnable, "fixture-worker");
			thread.setDaemon(true);
			return thread;
		});

		IContext red5Context = mock(IContext.class);
		ApplicationContext springContext = mock(ApplicationContext.class);

		when(scope.getName()).thenReturn("junit");
		when(scope.getContext()).thenReturn(red5Context);
		when(red5Context.getApplicationContext()).thenReturn(springContext);

		//the manager reads its beans off the red5 context, the fetcher off the spring one behind it
		when(red5Context.getBean(AppSettings.BEAN_NAME)).thenReturn(appSettings);
		when(red5Context.getBean(ServerSettings.BEAN_NAME)).thenReturn(serverSettings);
		when(red5Context.getBean(ILicenceService.BEAN_NAME)).thenReturn(licenceService);
		when(red5Context.getBean(AntMediaApplicationAdapter.BEAN_NAME)).thenReturn(app);
		when(springContext.getBean(AppSettings.BEAN_NAME)).thenReturn(appSettings);
		when(springContext.getBean(AntMediaApplicationAdapter.BEAN_NAME)).thenReturn(app);

		when(licenceService.isLicenceSuspended()).thenReturn(false);
		when(serverSettings.getHostAddress()).thenReturn("127.0.0.1");
		when(serverSettings.getDefaultHttpPort()).thenReturn(5080);

		when(app.getDataStore()).thenReturn(dataStore);
		when(app.getFreshBroadcastUpdateForStatus(anyString(), anyString())).thenAnswer(call -> {
			BroadcastUpdate update = new BroadcastUpdate();
			update.setStatus(call.getArgument(1));
			update.setUpdateTime(System.currentTimeMillis());
			return update;
		});

		//the real adapter writes finished from here, and the manager leans on that to free the stream id
		doAnswer(call -> {
			Broadcast row = rows.get((String) call.getArgument(0));
			if (row != null) {
				row.setStatus(IAntMediaStreamHandler.BROADCAST_STATUS_FINISHED);
			}
			return null;
		}).when(app).closeBroadcast(anyString(), any(), any());

		when(dataStore.get(anyString())).thenAnswer(call -> rows.get((String) call.getArgument(0)));
		when(dataStore.getExternalStreamsList()).thenAnswer(call -> new ArrayList<>(rows.values()));
		when(dataStore.updateBroadcastFields(anyString(), any())).thenAnswer(call -> apply(call.getArgument(0), call.getArgument(1)));
	}

	/** Only the fields the state machine and the playlist controller write, enough to assert on. */
	private boolean apply(String streamId, BroadcastUpdate update) {
		Broadcast row = rows.get(streamId);
		if (row == null || update == null) {
			return false;
		}

		if (update.getStatus() != null) {
			row.setStatus(update.getStatus());
		}
		if (update.getPlayListStatus() != null) {
			row.setPlayListStatus(update.getPlayListStatus());
		}
		if (update.getCurrentPlayIndex() != null) {
			row.setCurrentPlayIndex(update.getCurrentPlayIndex());
		}
		if (update.getUpdateTime() != null) {
			row.setUpdateTime(update.getUpdateTime());
		}
		return true;
	}

	/** Without a row the worker is never run at all, the fetcher calls the source deleted. */
	Broadcast row(String streamId, String streamUrl) {
		Broadcast broadcast = new Broadcast();
		try {
			broadcast.setStreamId(streamId);
		}
		catch (Exception e) {
			throw new IllegalStateException(e);
		}
		broadcast.setStreamUrl(streamUrl);
		broadcast.setType(STREAM_TYPE);
		//a broadcasting or preparing row decays to terminated_unexpectedly once this goes stale
		broadcast.setUpdateTime(System.currentTimeMillis());
		rows.put(streamId, broadcast);
		return broadcast;
	}

	/** A fetcher with a stored row, wired to this fixture's context, pool and listener. */
	ScriptedFetcher newFetcher(String streamId, StateListener listener) {
		row(streamId, "fake://" + streamId);

		ScriptedFetcher fetcher = new ScriptedFetcher(streamId, "fake://" + streamId, scope, vertx);
		fetcher.setDataStore(dataStore);
		fetcher.initialize(context, pool, listener);
		return fetcher;
	}

	ScriptedManager newManager() {
		return new ScriptedManager(vertx, dataStore, scope);
	}

	/**
	 * Waits until everything already posted to the given context has run. Asserting that something
	 * did *not* happen needs this, the entry points all return before their work does. It is also
	 * what gives the calling thread a happens before edge on the plain fields those tasks wrote.
	 */
	static void settle(Context target) {
		CompletableFuture<Void> done = new CompletableFuture<>();
		target.runOnContext(v -> done.complete(null));
		try {
			done.get(10, TimeUnit.SECONDS);
		}
		catch (Exception e) {
			throw new IllegalStateException("The shared context did not drain", e);
		}
	}

	/**
	 * A state is published at the top of the transition that reaches it, and everything that follows
	 * from it, the registry entry, the status write, the timers, runs further down the same task.
	 * Waiting on the state alone therefore hands the test a fetcher that is still half way through
	 * its transition, so this settles the fetcher's own context before it returns.
	 */
	static void awaitState(StreamFetcher fetcher, State state) {
		Awaitility.await("streamId:" + fetcher.getStreamId() + " reaches " + state)
				.atMost(15, TimeUnit.SECONDS)
				.until(() -> fetcher.getState() == state);

		settle((Context) peek(fetcher, "context"));
	}

	@Override
	public void close() {
		pool.shutdownNow();
		try {
			vertx.close().toCompletionStage().toCompletableFuture().get(10, TimeUnit.SECONDS);
		}
		catch (Exception e) {
			Thread.currentThread().interrupt();
		}
	}

	/**
	 * tick() is private and its timer only fires every 10s. This and {@link #peek}/{@link #poke} reach
	 * in so the tick rules can be asserted in milliseconds instead of half a minute.
	 */
	static void tick(StreamFetcher fetcher) {
		Context target = (Context) peek(fetcher, "context");

		target.runOnContext(v -> {
			try {
				Method tick = StreamFetcher.class.getDeclaredMethod("tick");
				tick.setAccessible(true);
				tick.invoke(fetcher);
			}
			catch (Exception e) {
				throw new IllegalStateException(e);
			}
		});
		settle(target);
	}

	static Object peek(Object target, String fieldName) {
		try {
			return declaredField(target, fieldName).get(target);
		}
		catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	static void poke(Object target, String fieldName, Object value) {
		try {
			declaredField(target, fieldName).set(target, value);
		}
		catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	private static Field declaredField(Object target, String fieldName) throws NoSuchFieldException {
		for (Class<?> type = target.getClass(); type != null; type = type.getSuperclass()) {
			try {
				Field field = type.getDeclaredField(fieldName);
				field.setAccessible(true);
				return field;
			}
			catch (NoSuchFieldException ignored) {
				//declared further up
			}
		}
		throw new NoSuchFieldException(fieldName + " on " + target.getClass());
	}

	/** A {@link StreamFetcher} that hands out the workers the test wrote instead of an ffmpeg one. */
	static class ScriptedFetcher extends StreamFetcher {

		/** One per attempt, in order. */
		final List<FakeWorker> workers = new CopyOnWriteArrayList<>();

		/** Used once the script runs out, so a fetcher can retry forever without being written out. */
		volatile Supplier<FakeWorker> workerSupplier = FakeWorker::new;

		/** Makes startStream() throw, which is how the manager's registration rollback is tested. */
		volatile boolean failOnStart;

		private final Queue<FakeWorker> scripted = new ConcurrentLinkedQueue<>();

		ScriptedFetcher(String streamId, String streamUrl, IScope scope, Vertx vertx) {
			super(streamUrl, streamId, STREAM_TYPE, scope, vertx, 0);
		}

		@Override
		public void startStream() {
			if (failOnStart) {
				throw new IllegalStateException("this fetcher cannot start");
			}
			super.startStream();
		}

		/** Handed to the next attempts in order. */
		void script(FakeWorker... next) {
			scripted.addAll(List.of(next));
		}

		void releaseAll() {
			workers.forEach(FakeWorker::release);
		}

		@Override
		protected StreamFetcherWorker createWorker() {
			FakeWorker worker = scripted.poll();
			if (worker == null) {
				worker = workerSupplier.get();
			}
			workers.add(worker);
			return worker;
		}
	}

	/** A manager that hands out scripted fetchers, so its registry and status writes run without ffmpeg. */
	static class ScriptedManager extends StreamFetcherManager {

		/** One per make(), in order. */
		final List<ScriptedFetcher> fetchers = new CopyOnWriteArrayList<>();

		/** Runs on each fetcher the moment it is made, before the manager starts it. */
		volatile Consumer<ScriptedFetcher> prepare;

		private final IScope managerScope;
		private final Vertx managerVertx;

		ScriptedManager(Vertx vertx, DataStore datastore, IScope scope) {
			super(vertx, datastore, scope);
			this.managerVertx = vertx;
			this.managerScope = scope;
		}

		@Override
		public StreamFetcher make(String streamId, String streamUrl, String streamType, long seekTimeMs) {
			ScriptedFetcher fetcher = new ScriptedFetcher(streamId, streamUrl, managerScope, managerVertx);
			//holding by default, so a state stays put until the test lets the attempt go
			fetcher.workerSupplier = FakeWorker::holding;

			if (prepare != null) {
				prepare.accept(fetcher);
			}

			fetchers.add(fetcher);
			return fetcher;
		}

		Context context() {
			return (Context) peek(this, "context");
		}
	}

	record Transition(State from, State to, boolean published) { }

	/** The manager's seat, so a test sees exactly what the manager would have been told. */
	static class Recorder implements StateListener {

		final List<Transition> transitions = new CopyOnWriteArrayList<>();
		final AtomicInteger ticks = new AtomicInteger();

		/** Thrown out of both callbacks, to prove a broken listener cannot strand the engine. */
		volatile RuntimeException failWith;

		/** Runs inside onTransition, which is how a reentrant call from a listener is tested. */
		volatile BiConsumer<StreamFetcher, State> hook;

		@Override
		public void onTransition(StreamFetcher fetcher, State from, State to) {
			transitions.add(new Transition(from, to, fetcher.isPublished()));

			if (hook != null) {
				hook.accept(fetcher, to);
			}
			if (failWith != null) {
				throw failWith;
			}
		}

		@Override
		public void onTick(StreamFetcher fetcher) {
			ticks.incrementAndGet();

			if (failWith != null) {
				throw failWith;
			}
		}

		List<State> states() {
			return transitions.stream().map(Transition::to).toList();
		}
	}
}
