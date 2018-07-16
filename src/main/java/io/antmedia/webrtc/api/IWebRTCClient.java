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


	/**
	 * Returns the time in milliseconds between the time when start function is called 
	 * and the time when streaming is started
	 * @return the time in milliseconds
	 * or -1 if timing is not available yet
	 */
	long getTimeToStartStreaming();
	
	/**
	 * Returns the time in milliseconds between the time when stop function is called 
	 * and the time when streaming is fully stopped
	 * @return the time in milliseconds
	 * or -1 if timing is not available yet
	 */
	long getTimeToStop();
	
	/**
	 * Return the period of send video period in milliseconds
	 * @return
	 */
	double getVideoFrameSentPeriod();
	
	/**
	 * Return the period of send audio period in milliseconds
	 * @return
	 */
	double getAudioFrameSentPeriod();
	
	/**
	 * Return the period of entering audio thread interval in milliseconds
	 * @return
	 */
	double getAudioThreadCheckInterval();
	
	/**
	 * Return the priod of entering video thread interval in milliseconds
	 * @return
	 */
	double getVideoThreadCheckInterval();

	
}
