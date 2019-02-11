package io.antmedia.webrtc.api;

import java.nio.ByteBuffer;
import java.util.Queue;

import io.antmedia.cluster.IStreamInfo;

public interface IWebRTCMuxer extends IStreamInfo {
	

	
	public void setWebRTCAdaptor(IWebRTCAdaptor webRTCAdaptor);
	
	/**
	 * Register to WebRTC Adaptor
	 */
	public void registerToAdaptor();
	
	
	public String getStreamId();
	
	
	/**
	 * Register new WebRTCClient to send video data
	 * First packet to send should be video conf data
	 * @param webRTCClient
	 */
	public void registerWebRTCClient(IWebRTCClient webRTCClient);
	
	
	/**
	 * Deregisters WebRTCClient from its list and does not send any
	 * video or audio packet to this WebRTCClient
	 * @param webRTCClient
	 */
	public boolean unRegisterWebRTCClient(IWebRTCClient webRTCClient);
	
	
	/**
	 * Send video packet to WebRTCClients
	 * @param videoPacket
	 * @param isKeyFrame
	 */
	public void sendVideoPacket(ByteBuffer videoPacket, boolean isKeyFrame, long timestamp, int frameRotation);
	
	
	/**
	 * Send audio packet to WebRTCClients
	 * @param audioPacket
	 */
	public void sendAudioPacket(ByteBuffer audioPacket, long timestamp);
	
	/**
	 * Returns number of WebRTCClients registered to the muxer
	 */
	public int getClientCount();
	
	
	public boolean contains(IWebRTCClient webRTCClient);
	
	
	/**
	 * Return the list of webrtc clients receiving data from webrtc muxer 
	 * @return
	 */
	public Queue<IWebRTCClient> getClientList();
	
}
