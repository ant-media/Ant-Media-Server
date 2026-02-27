package io.antmedia.muxer;

import io.vertx.core.Vertx;

/**
 * Factory interface for creating {@link RtmpMuxer} instances.
 * <p>
 * Set programmatically via {@link MuxAdaptor#setRtmpMuxerFactory(IRtmpMuxerFactory)}.
 * </p>
 */
@FunctionalInterface
public interface IRtmpMuxerFactory {

	/**
	 * Create a new {@link RtmpMuxer} (or a subclass) for the given RTMP URL.
	 *
	 * @param url   the RTMP endpoint URL
	 * @param vertx the Vert.x instance used for async scheduling
	 * @return a fully constructed {@link RtmpMuxer}
	 */
	RtmpMuxer create(String url, Vertx vertx);
}
