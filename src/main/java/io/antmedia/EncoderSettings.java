package io.antmedia;

import java.io.Serializable;

import dev.morphia.annotations.Entity;

@Entity
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
	
	/*
	 * Encode the stream even if source resolution is the same
	 * It is different from forceEncode. This doesn't let you upscale.
	 */
	private boolean  forceSameResolutionEncode = false;
	
	public static final String RESOLUTION_HEIGHT = "height";
	public static final String VIDEO_BITRATE = "videoBitrate";
	public static final String AUDIO_BITRATE = "audioBitrate";
	public static final String FORCE_ENCODE = "forceEncode";
	public static final String FORCE_SAME_RESOLUTION_ENCODE = "forceSameResolutionEncode";


	public EncoderSettings() {
		
	}

	public EncoderSettings(int height, int videoBitrate, int audioBitrate, boolean forceEncode) {
		this(height, videoBitrate, audioBitrate, forceEncode, false);
	}
	
	public EncoderSettings(int height, int videoBitrate, int audioBitrate, boolean forceEncode, boolean forceSameResolutionEncode) {
		this.height = height;
		this.videoBitrate = videoBitrate;
		this.audioBitrate = audioBitrate;
		this.forceEncode = forceEncode;
		this.forceSameResolutionEncode = forceSameResolutionEncode;
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

	public boolean isForceSameResolutionEncode() {
		return forceSameResolutionEncode;
	}

	public void setForceSameResolutionEncode(boolean forceSameResolutionEncode) {
		this.forceSameResolutionEncode = forceSameResolutionEncode;
	}

}
