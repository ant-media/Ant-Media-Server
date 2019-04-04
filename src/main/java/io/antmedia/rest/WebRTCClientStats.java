package io.antmedia.rest;

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

	public WebRTCClientStats(int measuredBitrate, int sendBitrate, double videoFrameSendPeriod, double audioFrameSendPeriod) {
		this.setMeasuredBitrate(measuredBitrate);
		this.setSendBitrate(sendBitrate);
		this.setVideoFrameSendPeriod(videoFrameSendPeriod);
		this.setAudioFrameSendPeriod(audioFrameSendPeriod);
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
	
}
