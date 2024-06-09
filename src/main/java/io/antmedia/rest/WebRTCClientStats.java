package io.antmedia.rest;

import io.antmedia.statistic.type.WebRTCAudioSendStats;
import io.antmedia.statistic.type.WebRTCVideoSendStats;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "WebRTC Client statistics.")
public class WebRTCClientStats {

	@Schema(description = "The measured bitrate of the WebRTC Client")
    private int measuredBitrate;
    
    @Schema(description = "The sent bitrate of the WebRTC Client")
    private int sendBitrate;
    
    @Schema(description = "The video frame sent period of the WebRTC Client")
    private double videoFrameSendPeriod;
    
    @Schema(description = "The audio frame send period of the WebRTC Client")
    private double audioFrameSendPeriod;

    @Schema(description = "WebRTC Client Id which is basically hash of the object")
    private int clientId;

    @Schema(description = "Number of video packets sent")
    private long videoPacketCount;

    @Schema(description = "Number of audio packets sent")
    private long audioPacketCount;
    
    @Schema(description = "Video sent low level stats")
    private WebRTCVideoSendStats videoSentStats;
    
    @Schema(description = "Audio sent low level stats")
    private WebRTCAudioSendStats audioSentStats;
    
    @Schema(description = "Free text information for the client")
    private String clientInfo;

    @Schema(description = "WebRTC Client's ip address")
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
