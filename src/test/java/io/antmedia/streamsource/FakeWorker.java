package io.antmedia.streamsource;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.muxer.MuxAdaptor;
import io.antmedia.streamsource.StreamFetcher.Reason;

/**
 * The worker a test writes instead of ffmpeg. It covers the four shapes the engine has to survive: a
 * source that starts delivering, one that ends on its own, one that blocks until it is aborted, and
 * one that stays blocked after the abort and has to be abandoned.
 */
class FakeWorker extends StreamFetcherWorker {

	/** Counts down once run() is on a pool thread, so a test can wait for the attempt to be live. */
	final CountDownLatch started = new CountDownLatch(1);

	final AtomicInteger runs = new AtomicInteger();

	/** Calls onFirstPacket, which is what moves the engine to STREAMING. */
	volatile boolean publishes;

	/** Blocks in run() the way a real open or read does, instead of returning at once. */
	volatile boolean holds;

	/** Stays blocked even after the abort, which is the attempt the engine has to give up on. */
	volatile boolean ignoresAbort;

	volatile Reason endReason = Reason.EOF;

	volatile Broadcast broadcast;
	volatile long seekedToMs = -1;

	/** Counts the reads the fetcher forwards here, MuxAdaptor itself cannot be mocked without natives. */
	final AtomicInteger muxAdaptorReads = new AtomicInteger();

	private final CountDownLatch released = new CountDownLatch(1);

	static FakeWorker holding() {
		FakeWorker worker = new FakeWorker();
		worker.holds = true;
		worker.ignoresAbort = true;
		return worker;
	}

	static FakeWorker publishing() {
		FakeWorker worker = holding();
		worker.publishes = true;
		return worker;
	}

	/** Delivers one packet and then ends, which is what clears the retry budget. */
	static FakeWorker publishesThenEnds() {
		FakeWorker worker = new FakeWorker();
		worker.publishes = true;
		return worker;
	}

	static FakeWorker ending(Reason reason) {
		FakeWorker worker = new FakeWorker();
		worker.endReason = reason;
		return worker;
	}

	@Override
	public Reason run(Broadcast broadcast) {
		this.broadcast = broadcast;
		runs.incrementAndGet();
		started.countDown();

		if (publishes) {
			lastActivityMs = System.currentTimeMillis();
			onFirstPacket.run();
		}

		try {
			while (holds && !released.await(10, TimeUnit.MILLISECONDS)) {
				if (abortRequested.get() && !ignoresAbort) {
					break;
				}
			}
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}

		return endReason;
	}

	/** Lets a holding worker return, whether it was aborted, abandoned or neither. */
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
