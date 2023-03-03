package io.antmedia.statistic;

import io.antmedia.AntMediaApplicationAdapter;

public interface IStreamStats {

	/**
	 * Register a new viewer to a stream
	 *
	 * @param streamId
	 * @param sessionId
	 * @param jwt
	 */
	void registerNewViewer(String streamId, String sessionId, String subscriberId, String viewerType, String jwt, AntMediaApplicationAdapter antMediaApplicationAdapter);
	
	
	/**
	 * Return the number of viewers of the stream
	 * @param streamId
	 * @return
	 */
	int getViewerCount(String streamId);
	


}
