package io.antmedia.analytic.model;

public class PublishStartedEvent extends AnalyticEvent {

	public static final String EVENT_PUBLISH_STARTED = "publishStarted";

	private int height;
	private int width;
	private String videoCodec;
	private String audioCodec;
	private String protocol;

	
	public PublishStartedEvent() {
		setEvent(EVENT_PUBLISH_STARTED);
	}

	public int getHeight() {
		return height;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	public int getWidth() {
		return width;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public String getVideoCodec() {
		return videoCodec;
	}

	public void setVideoCodec(String videoCodec) {
		this.videoCodec = videoCodec;
	}

	public String getAudioCodec() {
		return audioCodec;
	}

	public void setAudioCodec(String audioCodec) {
		this.audioCodec = audioCodec;
	}

	public String getProtocol() {
		return protocol;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}
	
}
