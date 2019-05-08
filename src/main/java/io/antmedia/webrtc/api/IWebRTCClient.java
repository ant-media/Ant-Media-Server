package io.antmedia.webrtc.api;

import java.nio.ByteBuffer;

import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;

public interface IWebRTCClient {
	
	
	public void setWebRTCAdaptor(IWebRTCAdaptor webRTCAdaptor);
	
	
	/**
	 * Send video packet to connected client
	 * @param videoPacket
	 * @param isKeyFrame
	 */
	public void sendVideoPacket(ByteBuffer videoPacket, boolean isKeyFrame, long timestamp, int frameRotation);
	
	
	/**
	 * Send audio packet to connected client
	 * @param audioPacket
	 */
	public void sendAudioPacket(ByteBuffer audioPacket, long timestamp);
	
	
	
	public int getTargetBitrate();
	
	
	public void start();
	
	public void setRemoteDescription(SessionDescription sdp);
	
	public void addIceCandidate(IceCandidate iceCandidate);
	
	public void setVideoResolution(int width, int height);


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
	float getVideoFrameSentPeriod();
	
	/**
	 * Return the period of send audio period in milliseconds
	 * @return
	 */
	float getAudioFrameSentPeriod();
	
	/** 
	 * @return the number of times video packet send called
	 */
	long getSendVideoPacketCallCount();
	
	/**
	 * @return number of times audio packet send called
	 */
	long getSendAudioPacketCallCount();
}
