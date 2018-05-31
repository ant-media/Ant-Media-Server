package io.antmedia.rest;

public class WebRTCClientStats {

	private int measuredBitrate;
	
	private int sendBitrate;
	
	private int videoFrameSendPeriod;
	
	private int audioFrameSendPeriod;
	
	private int videoThreadCheckInterval;
	
	private int audioThreadCheckInterval;

	public WebRTCClientStats(int measuredBitrate, int sendBitrate, int videoFrameSendPeriod, int audioFrameSendPeriod,
			int videoThreadCheckInterval, int audioThreadCheckInterval) {
		this.setMeasuredBitrate(measuredBitrate);
		this.setSendBitrate(sendBitrate);
		this.setVideoFrameSendPeriod(videoFrameSendPeriod);
		this.setAudioFrameSendPeriod(audioFrameSendPeriod);
		this.setVideoThreadCheckInterval(videoThreadCheckInterval);
		this.setAudioThreadCheckInterval(audioThreadCheckInterval);
	}

	public int getAudioThreadCheckInterval() {
		return audioThreadCheckInterval;
	}

	public void setAudioThreadCheckInterval(int audioThreadCheckInterval) {
		this.audioThreadCheckInterval = audioThreadCheckInterval;
	}

	public int getVideoThreadCheckInterval() {
		return videoThreadCheckInterval;
	}

	public void setVideoThreadCheckInterval(int videoThreadCheckInterval) {
		this.videoThreadCheckInterval = videoThreadCheckInterval;
	}

	public int getAudioFrameSendPeriod() {
		return audioFrameSendPeriod;
	}

	public void setAudioFrameSendPeriod(int audioFrameSendPeriod) {
		this.audioFrameSendPeriod = audioFrameSendPeriod;
	}

	public int getVideoFrameSendPeriod() {
		return videoFrameSendPeriod;
	}

	public void setVideoFrameSendPeriod(int videoFrameSendPeriod) {
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
