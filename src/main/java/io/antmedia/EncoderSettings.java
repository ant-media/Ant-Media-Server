package io.antmedia;

import java.io.Serializable;

public class EncoderSettings implements Serializable{
	
	private  int height;
	private  int videoBitrate;
	private  int audioBitrate;
	private String profile;
	private String tune;
	private String preset;

	/*
 	* Enable/Disable stream resolution check flag
	 * If it's enabled, Ant Media Server will ignore if the adaptive requested resolution is higher than the incoming stream
	 *  It's true by default
 */
	private boolean  forceEncode = true;
	
	public static final String HEIGHT = "height";
	public static final String VIDEO_BITRATE = "videoBitrate";
	public static final String AUDIO_BITRATE = "audioBitrate";
	public static final String FORCE_ENCODE = "forceEncode";
	public static final String ENC_PROFILE = "profile";
	public static final String ENC_TUNE = "tune";
	public static final String ENC_PRESET = "preset";

	public EncoderSettings() {
		
	}

	public EncoderSettings(int height, int videoBitrate, int audioBitrate, boolean forceEncode, String profile, String tune, String preset) {
		this.height = height;
		this.videoBitrate = videoBitrate;
		this.audioBitrate = audioBitrate;
		this.forceEncode = forceEncode;
		this.preset = preset;
		this.tune = tune;
		this.profile = profile;
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

	public void setTune(String tune){ this.tune = tune;}

	public String getTune(){return tune;}

	public void setProfile(String profile){ this.profile = profile;}

	public String getProfile(){return profile;}

	public void setPreset(String preset){this.preset = preset;}

	public String getPreset(){return preset;}

}
