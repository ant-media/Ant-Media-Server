package io.antmedia.rest;

public class WebRTCClientStats {

	private int measuredBitrate;
	
	private int sendBitrate;
	
	private double videoFrameSendPeriod;
	
	private double audioFrameSendPeriod;
	
	private double videoThreadCheckInterval;
	
	private double audioThreadCheckInterval;

	public WebRTCClientStats(int measuredBitrate, int sendBitrate, double videoFrameSendPeriod, double audioFrameSendPeriod,
			double videoThreadCheckInterval, double audioThreadCheckInterval) {
		this.setMeasuredBitrate(measuredBitrate);
		this.setSendBitrate(sendBitrate);
		this.setVideoFrameSendPeriod(videoFrameSendPeriod);
		this.setAudioFrameSendPeriod(audioFrameSendPeriod);
		this.setVideoThreadCheckInterval(videoThreadCheckInterval);
		this.setAudioThreadCheckInterval(audioThreadCheckInterval);
	}

	public double getAudioThreadCheckInterval() {
		return audioThreadCheckInterval;
	}

	public void setAudioThreadCheckInterval(double audioThreadCheckInterval) {
		this.audioThreadCheckInterval = audioThreadCheckInterval;
	}

	public double getVideoThreadCheckInterval() {
		return videoThreadCheckInterval;
	}

	public void setVideoThreadCheckInterval(double videoThreadCheckInterval) {
		this.videoThreadCheckInterval = videoThreadCheckInterval;
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
