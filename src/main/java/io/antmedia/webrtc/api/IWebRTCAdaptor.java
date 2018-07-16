package io.antmedia.webrtc.api;

import java.util.List;

import org.red5.server.api.scope.IScopeService;

import io.antmedia.rest.WebRTCClientStats;


public interface IWebRTCAdaptor extends IScopeService {
	
	
	public static String BEAN_NAME = "webrtc.adaptor";

	void registerMuxer(String streamId, IWebRTCMuxer webRTCMuxer);

	void unRegisterMuxer(String streamId, IWebRTCMuxer webRTCMuxer);

	boolean registerWebRTCClient(String streamId, IWebRTCClient webRTCClient);

	boolean streamExists(String streamId);
	
	List<IStreamInfo> getStreamOptions(String streamId);

	/**
	 * Try to find the best bitrate for the client
	 * @param streamId
	 * @param webRTCClient
	 */
	void adaptStreamingQuality(String streamId, IWebRTCClient webRTCClient);

	/**
	 * Register to specific resolution
	 * 
	 * It is used in clustering
	 * 
	 * @param streamId
	 * @param webRTCClusterClient
	 * @param resolutionHeight
	 */
	boolean registerWebRTCClient(String streamId, IWebRTCClient webRTCClusterClient, int resolutionHeight);
	
	/**
	 * Returns number of active live streams 
	 * @return
	 */
	int getNumberOfLiveStreams();
	
	/**
	 * Returns total number of viewers
	 * @return
	 */
	int getNumberOfTotalViewers();
	
	/**
	 * Returns total number of viewer of a specific stream
	 * @param streamId
	 * @return
	 */
	int getNumberOfViewers(String streamId);
	
	/**
	 * Return webrtc client stats
	 * @param streamId
	 * @return
	 */
	List<WebRTCClientStats> getWebRTCClientStats(String streamId);

}