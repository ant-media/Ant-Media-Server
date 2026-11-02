package io.antmedia.streamsource;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.muxer.MuxAdaptor;
import io.antmedia.rest.model.Result;
import io.antmedia.streamsource.StreamFetcher.Reason;

/**
 * One connect and pull cycle for a {@link StreamFetcher}, run on a pool thread. A worker knows how to
 * open one kind of source and push its packets, and nothing about states, retries or the database.
 * {@link FfmpegWorker} is the real one, tests hand the StreamFetcher their own.
 *
 * The fields below are the contract with the StreamFetcher. It owns them, the worker only reacts to them.
 */
public abstract class StreamFetcherWorker {

	/** Set by the StreamFetcher to cut a blocking open or read short. The worker must return promptly. */
	final AtomicBoolean abortRequested = new AtomicBoolean();

	/** Stamped by the worker on every packet, read by the StreamFetcher to spot a dead source. */
	volatile long lastActivityMs;

	/** Outcome of the last open, success or failure. Feeds {@link StreamFetcher#getCameraError()}. */
	final AtomicReference<Result> error = new AtomicReference<>();

	/** Run by the worker once the source starts delivering, which is what starts the broadcast. */
	Runnable onFirstPacket;

	/**
	 * When the StreamFetcher stopped waiting for this worker, or 0. Past that point it has moved on
	 * and a newer attempt may own the stream, so the worker must not write to anything shared.
	 */
	volatile long abandonedAtMs;

	/** Opens the source, pulls until abort, failure or end of file, then closes what it opened. */
	public abstract Reason run(Broadcast broadcast);

	public MuxAdaptor getMuxAdaptor() {
		return null;
	}

	public void seek(long seekTimeMs) {
		//only meaningful while a source is open
	}
}
