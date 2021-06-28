package io.antmedia.statistic.type;

import java.math.BigInteger;

public class WebRTCAudioReceiveStats {
	long audioPacketsReceived;
	BigInteger audioBytesReceived = BigInteger.ZERO;
	int audioPacketsLost; 
	double audioJitter;
	double audioFractionLost;
	private long audioPacketsReceivedPerSecond;
	private BigInteger audioBytesReceivedPerSecond = BigInteger.ZERO;
	
	public long getAudioPacketsReceived() {
		return audioPacketsReceived;
	}
	public void setAudioPacketsReceived(long audioPacketsReceivedDelta) {
		this.audioPacketsReceived = audioPacketsReceivedDelta;
	}
	public BigInteger getAudioBytesReceived() {
		return audioBytesReceived;
	}
	public void setAudioBytesReceived(BigInteger audioBytesReceivedDelta) {
		this.audioBytesReceived = audioBytesReceivedDelta;
	}
	public int getAudioPacketsLost() {
		return audioPacketsLost;
	}
	public void setAudioPacketsLost(int audioPacketsLostDelta) {
		this.audioPacketsLost = audioPacketsLostDelta;
	}
	public double getAudioJitter() {
		return audioJitter;
	}
	public void setAudioJitter(double audioJitterDelta) {
		this.audioJitter = audioJitterDelta;
	}
	public double getAudioFractionLost() {
		return audioFractionLost;
	}
	public void setAudioFractionLost(double audioFractionLostDelta) {
		this.audioFractionLost = audioFractionLostDelta;
	}
	public void addAudioStats(WebRTCAudioReceiveStats audioStats) 
	{
		if (audioStats != null) 
		{
			this.audioPacketsReceived += audioStats.getAudioPacketsReceived();
			this.audioBytesReceived = this.audioBytesReceived.add(audioStats.getAudioBytesReceived());
			this.audioPacketsLost += audioStats.getAudioPacketsLost();
			this.audioJitter += audioStats.getAudioJitter();
			this.audioFractionLost += audioStats.getAudioFractionLost();
			this.audioPacketsReceivedPerSecond += audioStats.getAudioPacketsReceivedPerSecond();
			this.audioBytesReceivedPerSecond = this.audioBytesReceivedPerSecond.add(audioStats.getAudioBytesReceivedPerSecond());
		}
	}
	
	public void setAudioPacketsReceivedPerSecond(long l) {
		this.audioPacketsReceivedPerSecond = l;
	}
	
	public long getAudioPacketsReceivedPerSecond() {
		return audioPacketsReceivedPerSecond;
	}
	public void setAudioBytesReceivedPerSecond(BigInteger bytesPerSecond) {
		this.audioBytesReceivedPerSecond = bytesPerSecond;
	}
	
	public BigInteger getAudioBytesReceivedPerSecond() {
		return audioBytesReceivedPerSecond;
	}
	
}
