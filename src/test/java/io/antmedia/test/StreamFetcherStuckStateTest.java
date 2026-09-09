package io.antmedia.test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.awaitility.Awaitility;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.red5.server.scope.WebScope;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.streamsource.StreamFetcher;
import io.antmedia.streamsource.StreamFetcher.WorkerThread;
import io.antmedia.streamsource.StreamFetcherManager;
import io.vertx.core.Vertx;

/**
 * Fetcher lifecycle fixes for issue #7944. The stuck state itself is covered by StreamFetcherUnitTest
 * (testHealDoesNotFireWithoutWorkerThread, testSkippedPullDoesNotPoisonStatusAndDeregisters,
 * testDeadRegistrationIsEvictedAndRestarted). This file only pins the three behaviours those do not touch.
 */
@ContextConfiguration(locations = { "test.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
@ExtendWith(SpringExtension.class)
public class StreamFetcherStuckStateTest {

	//VOD paces the local file at 1x, so a worker that wrongly goes live keeps running and is easy to catch
	private static final String LOCAL_SOURCE = "src/test/resources/test_video_360p.flv";

	@Autowired
	private ApplicationContext applicationContext;

	private WebScope appScope;
	private AntMediaApplicationAdapter app;
	private Vertx vertx;
	private String streamId;

	static {
		System.setProperty("red5.deployment.type", "junit");
		System.setProperty("red5.root", ".");
	}

	@BeforeEach
	public void before() {
		appScope = (WebScope) applicationContext.getBean("web.scope");
		app = (AntMediaApplicationAdapter) applicationContext.getBean("web.handler");
		vertx = (Vertx) applicationContext.getBean(AntMediaApplicationAdapter.VERTX_BEAN_NAME);
		app.getStreamFetcherManager().stopCheckerJob();
	}

	@AfterEach
	public void after() {
		StreamFetcherManager manager = app.getStreamFetcherManager();
		manager.stopCheckerJob();
		if (streamId != null) {
			manager.stopStreaming(streamId, false);
			manager.getStreamFetcherList().remove(streamId);
			app.getDataStore().delete(streamId);
			streamId = null;
		}
	}

	private StreamFetcher newFetcher(String url, String status) {
		Broadcast broadcast = new Broadcast("stuck-repro", "127.0.0.1", "admin", "admin",
				url, AntMediaApplicationAdapter.VOD);
		streamId = app.getDataStore().save(broadcast);
		assertNotNull(streamId);
		app.getDataStore().updateStatus(streamId, status);

		StreamFetcher fetcher = new StreamFetcher(url, streamId, AntMediaApplicationAdapter.VOD, appScope, vertx, 0);
		fetcher.setDataStore(app.getDataStore());
		fetcher.setRestartStream(false);
		return fetcher;
	}

	/**
	 * close() arms a 3s retry timer. stopStream() used to null the id without cancelling the timer, so a
	 * fetcher that had already been stopped and deregistered still woke up and started pulling again.
	 */
	@Test
	public void testStopCancelsThePendingRetry() {
		StreamFetcher fetcher = newFetcher(LOCAL_SOURCE, AntMediaApplicationAdapter.BROADCAST_STATUS_FINISHED);
		fetcher.setRestartStream(true);

		//nothing published and no stop requested, so close() takes the reconnect branch
		fetcher.new WorkerThread().close(null);
		assertTrue(fetcher.isRetryPending(), "close() should have armed a retry timer");

		fetcher.stopStream();

		//the retry assigns StreamFetcher.thread when it fires, so a null thread means it really was cancelled
		Awaitility.await().during(5, TimeUnit.SECONDS).atMost(10, TimeUnit.SECONDS)
				.until(() -> fetcher.getThread() == null);
	}

	/**
	 * startStream() spawns a helper that creates the worker a moment later, so a stop can land before the
	 * worker enters run(). run() used to clear stopRequestReceived on entry, which swallowed that stop and
	 * let a fetcher that was already deregistered go live anyway.
	 */
	@Test
	public void testStopBeforeWorkerStartsIsNotSwallowed() throws Exception {
		//terminated_unexpectedly so run() does not take the "already streaming" early return
		StreamFetcher fetcher = newFetcher(LOCAL_SOURCE,
				AntMediaApplicationAdapter.BROADCAST_STATUS_TERMINATED_UNEXPECTEDLY);

		fetcher.stopStream();
		WorkerThread worker = fetcher.new WorkerThread();
		worker.start();

		//it must bail out of prepare instead of pulling the whole file
		worker.join(TimeUnit.SECONDS.toMillis(20));
		assertFalse(worker.isAlive(), "a stopped fetcher must not keep pulling");
		assertFalse(AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING
						.equals(app.getDataStore().get(streamId).getStatus()),
				"a stopped fetcher must not go live");
	}

	/**
	 * Teardown can block for tens of seconds writing trailers and uploading segments.
	 * setThreadActive(false) used to run before close(), so the checker saw a live fetcher as a registration
	 * with no thread and evicted it mid teardown.
	 */
	@Test
	public void testFetcherReportsActiveWhileClosing() throws Exception {
		//unreadable source, so prepare fails and we reach close() without depending on any stop handling
		StreamFetcher fetcher = newFetcher("/nonexistent/no-such-source.flv",
				AntMediaApplicationAdapter.BROADCAST_STATUS_TERMINATED_UNEXPECTEDLY);

		AtomicBoolean activeWhenClosing = new AtomicBoolean();
		CountDownLatch closed = new CountDownLatch(1);

		//subclass rather than spy, a Mockito spy on a Thread breaks once it is started
		WorkerThread worker = fetcher.new WorkerThread() {
			@Override
			public void close(AVPacket pkt) {
				activeWhenClosing.set(fetcher.isThreadActive());
				super.close(pkt);
				closed.countDown();
			}
		};

		worker.start();

		assertTrue(closed.await(30, TimeUnit.SECONDS), "worker never reached close()");
		assertTrue(activeWhenClosing.get(),
				"fetcher must not report a dead thread while it is still tearing down");
	}
}
