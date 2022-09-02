package io.antmedia.webrtc.api;

import java.nio.ByteBuffer;
import java.util.Queue;

import io.antmedia.cluster.IStreamInfo;
import io.antmedia.webrtc.VideoCodec;

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
	 * Send video packet to WebRTCClients
	 * @param videoPacket
	 * @param isKeyFrame
	 * @param trackIndex
	 */
	public void sendTrackVideoPacket(ByteBuffer videoPacket, boolean isKeyFrame, long timestamp, int frameRotation,
			String trackId);
	
	/**
	 * Send audio packet to WebRTCClients
	 * @param audioPacket
	 */
	public void sendAudioPacket(ByteBuffer audioPacket, long timestamp);

	/**
	 * Send track's audio packet to WebRTCClients
	 * @param audioPacket
	 * @param trackId
	 */
	public void sendTrackAudioPacket(ByteBuffer audioPacket,long timestamp, String trackId);
	
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
	
	/**
	 * Return the video codec of the IWebRTCMuxer
	 */
	public VideoCodec getVideoCodec();


	/**
	 * Set the frame id in webrtc stack and relative capture time ms
	 * This let us calculate the absolute latency
	 * @param frameId
	 * @param captureTimeMs
	 */
	public void setFrameIdAndCaptureTimeMs(long frameId, long captureTimeMs);

}
