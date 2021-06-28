package io.antmedia.statistic.type;

import java.math.BigInteger;

public class WebRTCVideoReceiveStats 
{
	long videoFirCount;
	long videoPliCount;
	long videoNackCount;
	long videoPacketsReceived;
	int videoPacketsLost;
	double videoFractionLost;
	long videoFrameReceived;
	BigInteger videoBytesReceived = BigInteger.ZERO;
	private long videoPacketsReceivedPerSecond;
	private BigInteger videoBytesReceivedPerSecond = BigInteger.ZERO;
	private long videoFrameReceivedPerSecond;
	
	public long getVideoFirCount() {
		return videoFirCount;
	}
	
	public void setVideoFirCount(long videoFirCountDelta) {
		this.videoFirCount = videoFirCountDelta;
	}
	
	public long getVideoPliCount() {
		return videoPliCount;
	}
	
	public void setVideoPliCount(long videoPliCountDelta) {
		this.videoPliCount = videoPliCountDelta;
	}
	
	public long getVideoNackCount() {
		return videoNackCount;
	}
	
	public void setVideoNackCount(long videoNackCountDelta) {
		this.videoNackCount = videoNackCountDelta;
	}
	
	public long getVideoPacketsReceived() {
		return videoPacketsReceived;
	}
	
	public void setVideoPacketsReceived(long videoPacketsReceivedDelta) {
		this.videoPacketsReceived = videoPacketsReceivedDelta;
	}
	
	public int getVideoPacketsLost() {
		return videoPacketsLost;
	}
	
	public void setVideoPacketsLost(int videoPacketsLostDelta) {
		this.videoPacketsLost = videoPacketsLostDelta;
	}
	
	public double getVideoFractionLost() {
		return videoFractionLost;
	}
	
	public void setVideoFractionLost(double videoFractionLostDelta) {
		this.videoFractionLost = videoFractionLostDelta;
	}
	
	public long getVideoFrameReceived() {
		return videoFrameReceived;
	}
	
	public void setVideoFrameReceived(long videFrameReceivedDelta) {
		this.videoFrameReceived = videFrameReceivedDelta;
	}
	
	public BigInteger getVideoBytesReceived() {
		return videoBytesReceived;
	}
	
	public void setVideoBytesReceived(BigInteger videoBytesReceivedDelta) {
		this.videoBytesReceived = videoBytesReceivedDelta;
	}
	
	public void setVideoPacketsReceivedPerSecond(long l) {
		this.videoPacketsReceivedPerSecond = l;
	}
	
	public long getVideoPacketsReceivedPerSecond() {
		return videoPacketsReceivedPerSecond;
	}
	
	public void setVideoBytesReceivedPerSecond(BigInteger bytesReceivedPerSecond) {
		this.videoBytesReceivedPerSecond = bytesReceivedPerSecond;
	}
	
	public BigInteger getVideoBytesReceivedPerSecond() {
		return videoBytesReceivedPerSecond;
	}
	
	public void setVideoFrameReceivedPerSecond(long l) {
		this.videoFrameReceivedPerSecond = l;
	}
	
	public long getVideoFrameReceivedPerSecond() {
		return videoFrameReceivedPerSecond;
	}
	
	public void addVideoStats(WebRTCVideoReceiveStats videoReceiveStats) {
		if (videoReceiveStats != null) 
		{
			this.videoFirCount += videoReceiveStats.getVideoFirCount();
			this.videoPliCount += videoReceiveStats.getVideoPliCount();
			this.videoNackCount += videoReceiveStats.getVideoNackCount();
			this.videoPacketsReceived  += videoReceiveStats.getVideoPacketsReceived();
			this.videoPacketsLost += videoReceiveStats.getVideoPacketsLost();
			this.videoFractionLost += videoReceiveStats.getVideoFractionLost();
			this.videoFrameReceived += videoReceiveStats.getVideoFrameReceived();
			this.videoBytesReceived = this.videoBytesReceived.add(videoReceiveStats.getVideoBytesReceived());
			this.videoPacketsReceivedPerSecond += videoReceiveStats.getVideoPacketsReceivedPerSecond();
			this.videoBytesReceivedPerSecond = this.videoBytesReceivedPerSecond.add(videoReceiveStats.getVideoBytesReceivedPerSecond());
			this.videoFrameReceivedPerSecond += videoReceiveStats.getVideoFrameReceivedPerSecond();
		}
	}
}
