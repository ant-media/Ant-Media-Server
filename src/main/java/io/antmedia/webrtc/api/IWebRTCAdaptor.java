package io.antmedia.webrtc.api;

import java.util.List;

import org.red5.server.api.scope.IScopeService;

public interface IWebRTCAdaptor extends IScopeService {
	
	
	public static String BEAN_NAME = "webrtc.adaptor";

	void registerMuxer(String streamId, IWebRTCMuxer webRTCMuxer);

	void unRegisterMuxer(String streamId, IWebRTCMuxer webRTCMuxer);

	boolean registerWebRTCClient(String streamId, IWebRTCClient webRTCClient);

	//boolean deregisterWebRTCClient(String streamId, IWebRTCClient webRTCClient);

	boolean streamExists(String streamId);
	
	List<IStreamInfo> getStreamOptions(String streamId);

	IWebRTCMuxer getAdaptedWebRTCMuxer(String streamId, IWebRTCClient webRTCClient);

}