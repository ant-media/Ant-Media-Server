package io.antmedia.analytic.model;

public class KeyFrameStatsEvent extends AnalyticEvent {
	
	public static final String EVENT_KEY_FRAME_STATS = "keyFrameStats";

	private int keyFramesInLastMinute;
	private int keyFrameIntervalMs;
	
	public KeyFrameStatsEvent() {
		setEvent(EVENT_KEY_FRAME_STATS);
	}

	public int getKeyFramesInLastMinute() {
		return keyFramesInLastMinute;
	}

	public void setKeyFramesInLastMinute(int keyFramesInLastMinute) {
		this.keyFramesInLastMinute = keyFramesInLastMinute;
	}

	public int getKeyFrameIntervalMs() {
		return keyFrameIntervalMs;
	}

	public void setKeyFrameIntervalMs(int keyFrameIntervalMs) {
		this.keyFrameIntervalMs = keyFrameIntervalMs;
	}

}
