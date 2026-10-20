package io.antmedia.statistic;

public interface IStreamStats {

	/**
	 * Register a new viewer to a stream
	 * @param streamId
	 * @param sessionId
	 */
	void registerNewViewer(String streamId, String sessionId, String subscriberId);

	/**
	 * Mark a viewer session as an edge node pulling the stream. It still counts as a viewer;
	 * only the node's own player count leaves it out
	 * @param sessionId
	 */
	void registerEdgeSession(String sessionId);
	
	
	/**
	 * Return the number of viewers of the stream
	 * @param streamId
	 * @return
	 */
	int getViewerCount(String streamId);

}
