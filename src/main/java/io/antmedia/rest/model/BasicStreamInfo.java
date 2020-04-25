package io.antmedia.rest.model;

import io.antmedia.cluster.IStreamInfo;
import io.antmedia.webrtc.VideoCodec;

public class BasicStreamInfo implements IStreamInfo {

	private int videoHeight;
	private int videoWidth;
	private int videoBitrate;
	private int audioBitrate;
	private VideoCodec videoCodec;
	
	
	public BasicStreamInfo(int videoHeight, int videoWidth, int videoBitrate, int audioBitrate, VideoCodec videoCodec) {
		this.videoHeight = videoHeight;
		this.videoWidth = videoWidth;
		this.videoBitrate = videoBitrate;
		this.audioBitrate = audioBitrate;
		this.videoCodec = videoCodec;
	}

	@Override
	public int getVideoHeight() {
		return videoHeight;
	}

	@Override
	public int getVideoWidth() {
		return videoWidth;
	}

	@Override
	public int getVideoBitrate() {
		return videoBitrate;
	}

	@Override
	public int getAudioBitrate() {
		return audioBitrate;
	}

	@Override
	public VideoCodec getVideoCodec() {
		return videoCodec;
	}

}
