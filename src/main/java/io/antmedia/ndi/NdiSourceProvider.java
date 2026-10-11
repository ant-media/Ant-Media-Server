package io.antmedia.ndi;

import java.util.List;

import org.red5.server.api.scope.IScope;

public interface NdiSourceProvider {

	/**
	 * Returns the NDI source names discovered on the network.
	 */
	List<String> getNdiSources();

	/**
	 * Starts an explicitly selected NDI source in the target application.
	 */
	boolean startNdiSource(String sourceName, String streamId, IScope scope);
}
