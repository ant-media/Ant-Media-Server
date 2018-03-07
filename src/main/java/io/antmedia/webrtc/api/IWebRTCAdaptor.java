package io.antmedia.webrtc.api;

import java.util.List;

import org.red5.server.api.scope.IScopeService;

public interface IWebRTCAdaptor extends IScopeService {
	
	
	public static String BEAN_NAME = "webrtc.adaptor";

	void registerMuxer(String streamId, IWebRTCMuxer webRTCMuxer);

	void unRegisterMuxer(String streamId, IWebRTCMuxer webRTCMuxer);

	boolean registerWebRTCClient(String streamId, IWebRTCClient webRTCClient);

	boolean streamExists(String streamId);
	
	List<IStreamInfo> getStreamOptions(String streamId);

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

}