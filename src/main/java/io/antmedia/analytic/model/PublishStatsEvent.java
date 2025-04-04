package io.antmedia.analytic.model;

public class PublishStatsEvent extends AnalyticEvent{
	public static final String EVENT_PUBLISH_STATS = "publishStats";
	
	private long totalByteReceived;
	/**
	 * Amount of byte transferred between loggings 
	 */
	private long byteTransferred;
	
	/*
	 * Duration of the stream in milliseconds
	 */
	private long durationMs;
	
	/**
	 * Width of the stream
	 */
	private int width;
	/**
	 * Height of the stream
	 */
	private int height;
	
	/**
	 * Speed of the stream. It should be around 1.0x for RTMP, RTSP, SRT streams. This value does not make sense if it's WebRTC
	 */
	private double speed;
	
	/**
	 * The current size of the input queue.
	 */
	private int inputQueueSize;
	
	/**
	 * The current size of the queue of frames awaiting encoding.
	 */
	private int encodingQueueSize;
	
	/**
	 * Indicates that frames were dropped during the encoding process. 
	 * 
	 * Frames are dropped if the size of the encoding queue exceeds the 
	 * threshold defined in {@link AppSettings#encodingQueueSize}.
	 */
	private int droppedFrameCountInEncoding;

	/**
	 * Indicates that packets were dropped during the ingestion. 
	 * 
	 * Packets are dropped if the size of the inputQueueSize exceeds the 
	 * threshold defined in {@link AppSettings#encodingQueueSize}.
	 */
	private int droppedPacketCountInIngestion;
	
	/**
	 * The round trip time of the stream. It's meaningful for WebRTC streams
	 */
	private int roundTripTimeMs;
	
	/**
	 * The jitter of the stream. It's meaningful for WebRTC streams
	 */
	private int jitterMs;
	
	/**
	 * The packet lost ratio of the stream. It's meaningful for WebRTC streams
	 */
	private double packetLostRatio;
	
	/**
	 * The total number of packets lost during WebRTC streaming
	 */
	private int packetsLost;
	
	/**
	 * The remote IP of the broadaster
	 */
	private String remoteIp;
	
	/**
	 * The user agent of the broadcaster
	 */
	private String userAgent;
	
	public PublishStatsEvent() {
		setEvent(EVENT_PUBLISH_STATS);
	}

	public long getTotalByteReceived() {
		return totalByteReceived;
	}

	public void setTotalByteReceived(long totalByteReceived) {
		this.totalByteReceived = totalByteReceived;
	}

	public long getDurationMs() {
		return durationMs;
	}

	public void setDurationMs(long durationMs) {
		this.durationMs = durationMs;
	}

	public int getWidth() {
		return width;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public int getHeight() {
		return height;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	public long getByteTransferred() {
		return byteTransferred;
	}

	public void setByteTransferred(long byteTransferred) {
		this.byteTransferred = byteTransferred;
	}

	/**
	 * @return the speed
	 */
	public double getSpeed() {
		return speed;
	}

	/**
	 * @param speed the speed to set
	 */
	public void setSpeed(double speed) {
		this.speed = speed;
	}

	/**
	 * @return the inputQueueSize
	 */
	public int getInputQueueSize() {
		return inputQueueSize;
	}

	/**
	 * @param inputQueueSize the inputQueueSize to set
	 */
	public void setInputQueueSize(int inputQueueSize) {
		this.inputQueueSize = inputQueueSize;
	}

	/**
	 * @return the encodingQueueSize
	 */
	public int getEncodingQueueSize() {
		return encodingQueueSize;
	}

	/**
	 * @param encodingQueueSize the encodingQueueSize to set
	 */
	public void setEncodingQueueSize(int encodingQueueSize) {
		this.encodingQueueSize = encodingQueueSize;
	}

	/**
	 * @return the droppedFrameCountInEncoding
	 */
	public int getDroppedFrameCountInEncoding() {
		return droppedFrameCountInEncoding;
	}

	/**
	 * @param droppedFrameCountInEncoding the droppedFrameCountInEncoding to set
	 */
	public void setDroppedFrameCountInEncoding(int droppedFrameCountInEncoding) {
		this.droppedFrameCountInEncoding = droppedFrameCountInEncoding;
	}

	/**
	 * @return the droppedPacketCountInIngestion
	 */
	public int getDroppedPacketCountInIngestion() {
		return droppedPacketCountInIngestion;
	}

	/**
	 * @param droppedPacketCountInIngestion the droppedPacketCountInIngestion to set
	 */
	public void setDroppedPacketCountInIngestion(int droppedPacketCountInIngestion) {
		this.droppedPacketCountInIngestion = droppedPacketCountInIngestion;
	}

	/**
	 * @return the roundTripTimeMs
	 */
	public int getRoundTripTimeMs() {
		return roundTripTimeMs;
	}

	/**
	 * @param roundTripTimeMs the roundTripTimeMs to set
	 */
	public void setRoundTripTimeMs(int roundTripTimeMs) {
		this.roundTripTimeMs = roundTripTimeMs;
	}

	/**
	 * @return the jitterMs
	 */
	public int getJitterMs() {
		return jitterMs;
	}

	/**
	 * @param jitterMs the jitterMs to set
	 */
	public void setJitterMs(int jitterMs) {
		this.jitterMs = jitterMs;
	}

	/**
	 * @return the packetLostRatio
	 */
	public double getPacketLostRatio() {
		return packetLostRatio;
	}

	/**
	 * @param packetLostRatio the packetLostRatio to set
	 */
	public void setPacketLostRatio(double packetLostRatio) {
		this.packetLostRatio = packetLostRatio;
	}

	/**
	 * @return the packetsLost
	 */
	public int getPacketsLost() {
		return packetsLost;
	}

	/**
	 * @param packetsLost the packetsLost to set
	 */
	public void setPacketsLost(int packetsLost) {
		this.packetsLost = packetsLost;
	}

	/**
	 * @return the remoteIp
	 */
	public String getRemoteIp() {
		return remoteIp;
	}

	/**
	 * @param remoteIp the remoteIp to set
	 */
	public void setRemoteIp(String remoteIp) {
		this.remoteIp = remoteIp;
	}

	/**
	 * @return the userAgent
	 */
	public String getUserAgent() {
		return userAgent;
	}

	/**
	 * @param userAgent the userAgent to set
	 */
	public void setUserAgent(String userAgent) {
		this.userAgent = userAgent;
	}

}
