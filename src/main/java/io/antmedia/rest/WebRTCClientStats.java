package io.antmedia.rest;

import io.antmedia.statistic.type.WebRTCAudioSendStats;
import io.antmedia.statistic.type.WebRTCVideoSendStats;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(value="WebRTCClientStats", description="The WebRTC Client Statistics class")
public class WebRTCClientStats {

	@ApiModelProperty(value = "the measured bitrate of the WebRTC Client")
	private int measuredBitrate;
	
	@ApiModelProperty(value = "the sent bitrate of the WebRTC Client")
	private int sendBitrate;
	
	@ApiModelProperty(value = "the video frame sent period of the WebRTC Client")
	private double videoFrameSendPeriod;
	
	@ApiModelProperty(value = "the audio frame send period of the WebRTC Client")
	private double audioFrameSendPeriod;

	@ApiModelProperty(value = "WebRTC Client Id which is basically hash of the object")
	private int clientId;

	@ApiModelProperty(value = "Number of video packets sent")
	private long videoPacketCount;

	@ApiModelProperty(value = "Number of audio packets sent")
	private long audioPacketCount;
	
	@ApiModelProperty(value = "Video sent low level stats")
	private WebRTCVideoSendStats videoSentStats;
	
	@ApiModelProperty(value = "Audio sent low level stats")
	private WebRTCAudioSendStats audioSentStats;
	
	@ApiModelProperty(value = "Free text information for the client")
	private String clientInfo;

	@ApiModelProperty(value = "WebRTC Client's ip address")
	private String clientIp;

	public WebRTCClientStats(int measuredBitrate, int sendBitrate, double videoFrameSendPeriod, double audioFrameSendPeriod, 
			long videoPacketCount, long audioPacketCount, int clientId, String clientInfo, String clientIp) {
		this.measuredBitrate = measuredBitrate;
		this.sendBitrate = sendBitrate;
		this.videoFrameSendPeriod = videoFrameSendPeriod;
		this.audioFrameSendPeriod = audioFrameSendPeriod;
		this.videoPacketCount = videoPacketCount;
		this.audioPacketCount = audioPacketCount;
		this.clientId = clientId;
		this.clientInfo = clientInfo;
		this.clientIp = clientIp;
	}

	public double getAudioFrameSendPeriod() {
		return audioFrameSendPeriod;
	}

	public void setAudioFrameSendPeriod(double audioFrameSendPeriod) {
		this.audioFrameSendPeriod = audioFrameSendPeriod;
	}

	public double getVideoFrameSendPeriod() {
		return videoFrameSendPeriod;
	}

	public void setVideoFrameSendPeriod(double videoFrameSendPeriod) {
		this.videoFrameSendPeriod = videoFrameSendPeriod;
	}

	public int getSendBitrate() {
		return sendBitrate;
	}

	public void setSendBitrate(int sendBitrate) {
		this.sendBitrate = sendBitrate;
	}

	public int getMeasuredBitrate() {
		return measuredBitrate;
	}

	public void setMeasuredBitrate(int measuredBitrate) {
		this.measuredBitrate = measuredBitrate;
	}

	public int getClientId() {
		return clientId;
	}

	public void setClientId(int clientId) {
		this.clientId = clientId;
	}

	public long getAudioPacketCount() {
		return audioPacketCount;
	}

	public void setAudioPacketCount(long audioPacketCount) {
		this.audioPacketCount = audioPacketCount;
	}

	public long getVideoPacketCount() {
		return videoPacketCount;
	}

	public void setVideoPacketCount(long videoPacketCount) {
		this.videoPacketCount = videoPacketCount;
	}

	public WebRTCAudioSendStats getAudioSentStats() {
		return audioSentStats;
	}

	public void setAudioSentStats(WebRTCAudioSendStats audioSentStats) {
		this.audioSentStats = audioSentStats;
	}

	public WebRTCVideoSendStats getVideoSentStats() {
		return videoSentStats;
	}

	public void setVideoSentStats(WebRTCVideoSendStats videoSentStats) {
		this.videoSentStats = videoSentStats;
	}

	public String getClientInfo() {
		return clientInfo;
	}

	public void setClientInfo(String clientInfo) {
		this.clientInfo = clientInfo;
	}

	public String getClientIp() { return clientIp; }

	public void setClientIp(String clientIp) { this.clientIp = clientIp; }
	
}
