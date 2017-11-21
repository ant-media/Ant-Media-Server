package io.antmedia.webrtc.api;

import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;

public interface IWebRTCClient {
	
	
	public void setWebRTCAdaptor(IWebRTCAdaptor webRTCAdaptor);
	
	
	/**
	 * Send video packet to connected client
	 * @param videoPacket
	 * @param isKeyFrame
	 */
	public void sendVideoPacket(byte[] videoPacket, boolean isKeyFrame, long timestamp);
	
	
	/**
	 * Send audio packet to connected client
	 * @param audioPacket
	 */
	public void sendAudioPacket(byte[] audioPacket, long timestamp);
	
	
	
	public int getTargetBitrate();
	
	
	public void start();
	
	public void setRemoteDescription(SessionDescription sdp);
	
	public void addIceCandidate(IceCandidate iceCandidate);


	void sendVideoConfPacket(byte[] videoConfData, byte[] videoPacket, long timestamp);
	
	void setVideoResolution(int width, int height);


	public void setWebRTCMuxer(IWebRTCMuxer webRTCMuxer);
	
	public IWebRTCMuxer getWebRTCMuxer();
	
	public void stop();

	
}
