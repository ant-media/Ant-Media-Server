package io.antmedia.analytic.model;

public class PublishEndedEvent extends AnalyticEvent {
	
	public static final String EVENT_PUBLISH_ENDED = "publishEnded";
	
	private long durationMs;

	public PublishEndedEvent() {
		setEvent(EVENT_PUBLISH_ENDED);
	}

	public long getDurationMs() {
		return durationMs;
	}

	public void setDurationMs(long durationMs) {
		this.durationMs = durationMs;
	}

}
