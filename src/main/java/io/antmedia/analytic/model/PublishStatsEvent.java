package io.antmedia.analytic.model;

public class PublishStatsEvent extends AnalyticEvent{
	public static final String EVENT_PUBLISH_STATS = "publishStats";
	
	private long totalByteReceived;
	/**
	 * Amount of byte transferred between loggings 
	 */
	private long byteTransferred;
	private long durationMs;
	
	private int width;
	private int height;
	
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

}
