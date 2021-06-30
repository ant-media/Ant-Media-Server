package io.antmedia;

import java.io.Serializable;

public class EncoderSettings implements Serializable{
	
	private  int height;
	
	private  int videoBitrate;
	
	private  int audioBitrate;
	
	public EncoderSettings() {
		
	}

	public EncoderSettings(int height, int videoBitrate, int audioBitrate) {
		this.height = height;
		this.videoBitrate = videoBitrate;
		this.setAudioBitrate(audioBitrate);
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

}
