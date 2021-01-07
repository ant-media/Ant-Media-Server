package io.antmedia.webrtc;

import org.bytedeco.ffmpeg.avutil.AVFrame;

public class AudioFrameContext 
{
	long timestampMs;
	int numberOfFrames;
	int channels;
	int sampleRate;
	private AVFrame frame;

	public AudioFrameContext(AVFrame frame, long timestampMS, int numberOfFrames, int channels, int sampleRate) {
		this.frame = frame;
		this.timestampMs = timestampMS;
		this.numberOfFrames = numberOfFrames;
		this.channels = channels;
		this.sampleRate = sampleRate;
	}
}
