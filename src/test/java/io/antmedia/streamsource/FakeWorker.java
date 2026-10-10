package io.antmedia.streamsource;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.muxer.MuxAdaptor;
import io.antmedia.streamsource.StreamFetcher.Reason;

/**
 * The worker a test writes instead of ffmpeg. It either ends at once with a reason, or blocks the way
 * a real open does until the test releases it. Releasing, not the abort flag, is what ends a blocked
 * attempt, so a test can always decide when an attempt returns.
 */
class FakeWorker extends StreamFetcherWorker {

	final AtomicInteger runs = new AtomicInteger();

	/** Counts what the fetcher forwards here. MuxAdaptor itself cannot be mocked without natives. */
	final AtomicInteger muxAdaptorReads = new AtomicInteger();

	/** Calls onFirstPacket, which is what moves the engine to STREAMING. */
	volatile boolean publishes;

	volatile boolean holds;
	volatile Reason endReason = Reason.EOF;
	volatile long seekedToMs = -1;

	private final CountDownLatch released = new CountDownLatch(1);

	static FakeWorker holding() {
		FakeWorker worker = new FakeWorker();
		worker.holds = true;
		return worker;
	}

	static FakeWorker publishing() {
		FakeWorker worker = holding();
		worker.publishes = true;
		return worker;
	}

	static FakeWorker ending(Reason reason) {
		FakeWorker worker = new FakeWorker();
		worker.endReason = reason;
		return worker;
	}

	/** Delivers one packet and then ends, which is what clears the retry budget. */
	static FakeWorker publishesThenEnds() {
		FakeWorker worker = new FakeWorker();
		worker.publishes = true;
		return worker;
	}

	@Override
	public Reason run(Broadcast broadcast) {
		runs.incrementAndGet();

		if (publishes) {
			lastActivityMs = System.currentTimeMillis();
			onFirstPacket.run();
		}

		try {
			if (holds) {
				released.await();
			}
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}

		return endReason;
	}

	void release() {
		released.countDown();
	}

	@Override
	public MuxAdaptor getMuxAdaptor() {
		muxAdaptorReads.incrementAndGet();
		return null;
	}

	@Override
	public void seek(long seekTimeMs) {
		seekedToMs = seekTimeMs;
	}
}
