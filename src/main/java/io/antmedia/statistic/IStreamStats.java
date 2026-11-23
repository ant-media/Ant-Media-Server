package io.antmedia.statistic;

public interface IStreamStats {

	/**
	 * Register a new viewer to a stream
	 * @param streamId
	 * @param sessionId
	 */
	void registerNewViewer(String streamId, String sessionId, String subscriberId);
	
	
	/**
	 * Return the number of viewers of the stream
	 * @param streamId
	 * @return
	 */
	int getViewerCount(String streamId);

	/**
	 * Move a viewer entry from one key to another, leaving the viewer count unchanged.
	 * Used when a viewer that was first counted under its browser fingerprint starts sending the
	 * viewerId cookie back, so the same client is not counted as a second viewer.
	 *
	 * Implemented as a no-op by default so implementations living outside this repository keep
	 * compiling, they simply keep counting the fingerprint entry until it times out.
	 *
	 * @param streamId the stream ID
	 * @param oldViewerKey the key the viewer is currently registered under (the fingerprint hash)
	 * @param newViewerKey the key the viewer should be registered under (the cookie uuid)
	 */
	default void migrateViewerEntry(String streamId, String oldViewerKey, String newViewerKey) {
	}

}
