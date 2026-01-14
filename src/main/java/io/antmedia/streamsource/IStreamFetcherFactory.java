package io.antmedia.streamsource;

import org.red5.server.api.scope.IScope;

import io.vertx.core.Vertx;

/**
 * Factory interface to create {@link StreamFetcher} instances.
 *
 * It provides an injection/extension point so deployments/plugins can supply a custom
 * {@link StreamFetcher} implementation without modifying core code.
 */
@FunctionalInterface
public interface IStreamFetcherFactory {

	/**
	 * Default bean name to be discovered from the Red5/Spring context.
	 */
	String BEAN_NAME = "streamFetcherFactory";

	StreamFetcher create(String streamUrl, String streamId, String streamType, IScope scope, Vertx vertx, long seekTimeInMs);
}


