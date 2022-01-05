package io.antmedia;

import java.io.Serializable;

public class EncoderSettings implements Serializable{
	
	private  int height;
	private  int videoBitrate;
	private  int audioBitrate;

	/*
 	* Enable/Disable stream resolution check flag
	 * If it's enabled, Ant Media Server will ignore if the adaptive requested resolution is higher than the incoming stream
	 *  It's true by default
 */
	private boolean  forceEncode = true;
	
	public static final String RESOLUTION_HEIGHT = "height";
	public static final String VIDEO_BITRATE = "videoBitrate";
	public static final String AUDIO_BITRATE = "audioBitrate";
	public static final String FORCE_ENCODE = "forceEncode";

	public EncoderSettings() {
		
	}

	public EncoderSettings(int height, int videoBitrate, int audioBitrate, boolean forceEncode) {
		this.height = height;
		this.videoBitrate = videoBitrate;
		this.audioBitrate = audioBitrate;
		this.forceEncode = forceEncode;
	}

	public int getHeight() {
		return height;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	public int getVideoBitrate() {
		return videoBitrate;
	}

	public void setVideoBitrate(int videoBitrate) {
		this.videoBitrate = videoBitrate;
	}

	public int getAudioBitrate() {
		return audioBitrate;
	}

	public void setAudioBitrate(int audioBitrate) {
		this.audioBitrate = audioBitrate;
	}
	
	public boolean isForceEncode() {
		return forceEncode;
	}

	public void setForceEncode(boolean forceEncode) {
		this.forceEncode = forceEncode;
	}

}
