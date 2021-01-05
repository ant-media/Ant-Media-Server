package io.antmedia.webrtc;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.antmedia.cluster.IStreamInfo;
import io.antmedia.rest.WebRTCClientStats;
import io.antmedia.webrtc.api.IWebRTCAdaptor;
import io.antmedia.webrtc.api.IWebRTCClient;
import io.antmedia.webrtc.api.IWebRTCMuxer;

/**
 * This class is used to mock the webrtc adaptor in community edition
 * @author mekya
 *
 */
public class MockWebRTCAdaptor implements IWebRTCAdaptor{

	@Override
	public void registerMuxer(String streamId, IWebRTCMuxer webRTCMuxer) {
		//No implementation since this is mock
	}

	@Override
	public void unRegisterMuxer(String streamId, IWebRTCMuxer webRTCMuxer) {
		//No implementation since this is mock
	}

	@Override
	public boolean registerWebRTCClient(String streamId, IWebRTCClient webRTCClient, VideoCodec codec) {
		return false;
	}

	@Override
	public boolean streamExists(String streamId) {
		return false;
	}

	@Override
	public List<IStreamInfo> getStreamInfo(String streamId) {
		return new ArrayList<>();
	}

	@Override
	public void adaptStreamingQuality(String streamId, IWebRTCClient webRTCClient, VideoCodec codec) {
		//No implementation since this is mock
	}

	@Override
	public boolean registerWebRTCClient(String streamId, IWebRTCClient webRTCClusterClient, int resolutionHeight, VideoCodec codec) {
		return false;
	}

	@Override
	public int getNumberOfLiveStreams() {
		return -1;
	}

	@Override
	public int getNumberOfTotalViewers() {
		return -1;
	}

	@Override
	public int getNumberOfViewers(String streamId) {
		return -1;
	}

	@Override
	public List<WebRTCClientStats> getWebRTCClientStats(String streamId) {
		return new ArrayList<>();
	}

	@Override
	public Set<String> getStreams() {
		return new HashSet<>();
	}

	@Override
	public void setExcessiveBandwidthValue(int excessiveBandwidthValue) {
		//No implementation since this is mock
	}

	@Override
	public void setExcessiveBandwidthCallThreshold(int excessiveBandwidthCallThreshold) {
		//No implementation since this is mock
	}

	@Override
	public void setExcessiveBandwidthAlgorithmEnabled(boolean excessiveBandwidthAlgorithmEnabled) {
		//No implementation since this is mock
	}

	@Override
	public void setPacketLossDiffThresholdForSwitchback(int packetLossDiffThresholdForSwitchback) {
		//No implementation since this is mock
	}

	@Override
	public void setRttMeasurementDiffThresholdForSwitchback(int rttMeasurementDiffThresholdForSwitchback) {
		//No implementation since this is mock
	}

	@Override
	public void setTryCountBeforeSwitchback(int tryCountBeforeSwitchback) {
		//No implementation since this is mock
	}

	@Override
	public void forceStreamingQuality(String streamId, IWebRTCClient webRTCClient, int streamHeight) {
		//No implementation since this is mock
	}
}
