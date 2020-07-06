package io.antmedia.webrtc.api;

import java.util.List;
import java.util.Set;

import org.red5.server.api.scope.IScopeService;

import io.antmedia.cluster.IStreamInfo;
import io.antmedia.rest.WebRTCClientStats;
import io.antmedia.webrtc.VideoCodec;


public interface IWebRTCAdaptor extends IScopeService {
	
	
	public static String BEAN_NAME = "webrtc.adaptor";

	void registerMuxer(String streamId, IWebRTCMuxer webRTCMuxer);

	void unRegisterMuxer(String streamId, IWebRTCMuxer webRTCMuxer);

	boolean registerWebRTCClient(String streamId, IWebRTCClient webRTCClient, VideoCodec codec);

	boolean streamExists(String streamId);
	
	List<IStreamInfo> getStreamInfo(String streamId);

	/**
	 * Try to find the best bitrate for the client
	 * @param streamId
	 * @param webRTCClient
	 */
	void adaptStreamingQuality(String streamId, IWebRTCClient webRTCClient, VideoCodec codec);

	/**
	 * Try to force defined stream quality for the client
	 * @param streamId
	 * @param webRTCClient
	 * @param streamHeight
	 */
	void forceStreamingQuality(String streamId, IWebRTCClient webRTCClient, int streamHeight);

	/**
	 * Register to specific resolution
	 * 
	 * It is used in clustering
	 * 
	 * @param streamId
	 * @param webRTCClusterClient
	 * @param resolutionHeight
	 */
	boolean registerWebRTCClient(String streamId, IWebRTCClient webRTCClusterClient, int resolutionHeight, VideoCodec codec);
	
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
	
	/**
	 * Returns the stream id in the WebRTCAdaptor
	 */
	Set<String> getStreams();

	/**
	 * Sets the excessive bandwidth threshold value
	 * @param excessiveBandwidthValue
	 */
	void setExcessiveBandwidthValue(int excessiveBandwidthValue);

	/**
	 * Sets the excessive bandwidth call threshold value
	 * @param excessiveBandwidthCallThreshold
	 */
	void setExcessiveBandwidthCallThreshold(int excessiveBandwidthCallThreshold);

	/**
	 * Enable or disable excessive bandwidth algorithm
	 * @param excessiveBandwidthAlgorithmEnabled
	 */
	void setExcessiveBandwidthAlgorithmEnabled(boolean excessiveBandwidthAlgorithmEnabled);

	/**
	 * Set packet loss threshold if packetLoss is bigger than this value in ExcessiveBandwidth
	 * algorithm, it switches back to lower quality without try every attempts {@link #setTryCountBeforeSwitchback(int)}
	 * @param packetLossDiffThresholdForSwitchback
	 */
	void setPacketLossDiffThresholdForSwitchback(int packetLossDiffThresholdForSwitchback);

	/**
	 * Set rtt measurement threshold if rttMeasurement is bigger than this value in ExcessiveBandwidth
	 * algorithm, it switches back to lower quality without try every attempts {@link #setTryCountBeforeSwitchback(int)}
	 * @param rttMeasurementDiffThresholdForSwitchback
	 */
	void setRttMeasurementDiffThresholdForSwitchback(int rttMeasurementDiffThresholdForSwitchback);

	/**
	 * Number of tries to switch back to lower quality in ExcessiveBandwidth
	 * @param tryCountBeforeSwitchback
	 */
	void setTryCountBeforeSwitchback(int tryCountBeforeSwitchback);

}