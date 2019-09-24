package io.antmedia.webrtc;

import java.util.List;
import java.util.Set;

import io.antmedia.cluster.IStreamInfo;
import io.antmedia.rest.WebRTCClientStats;
import io.antmedia.webrtc.api.IWebRTCAdaptor;
import io.antmedia.webrtc.api.IWebRTCClient;
import io.antmedia.webrtc.api.IWebRTCMuxer;

public class MockWebRTCAdaptor implements IWebRTCAdaptor{

	@Override
	public void registerMuxer(String streamId, IWebRTCMuxer webRTCMuxer) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void unRegisterMuxer(String streamId, IWebRTCMuxer webRTCMuxer) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean registerWebRTCClient(String streamId, IWebRTCClient webRTCClient) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean streamExists(String streamId) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public List<IStreamInfo> getStreamOptions(String streamId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void adaptStreamingQuality(String streamId, IWebRTCClient webRTCClient) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean registerWebRTCClient(String streamId, IWebRTCClient webRTCClusterClient, int resolutionHeight) {
		// TODO Auto-generated method stub
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
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<String> getStreams() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setExcessiveBandwidthValue(int excessiveBandwidthValue) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setExcessiveBandwidthCallThreshold(int excessiveBandwidthCallThreshold) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setExcessiveBandwidthAlgorithmEnabled(boolean excessiveBandwidthAlgorithmEnabled) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setPacketLossDiffThresholdForSwitchback(int packetLossDiffThresholdForSwitchback) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setRttMeasurementDiffThresholdForSwitchback(int rttMeasurementDiffThresholdForSwitchback) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setTryCountBeforeSwitchback(int tryCountBeforeSwitchback) {
		// TODO Auto-generated method stub
		
	}
}
