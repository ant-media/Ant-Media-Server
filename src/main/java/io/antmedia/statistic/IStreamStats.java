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
	 * Remove a specific viewer entry from the stream's viewer map.
	 * Used to clean up a fingerprint-based entry when a cookie-based identity takes over.
	 *
	 * @param streamId the stream ID
	 * @param viewerKey the viewer key to remove (e.g. the fingerprint hash)
	 */
	void removeViewerEntry(String streamId, String viewerKey);

}
