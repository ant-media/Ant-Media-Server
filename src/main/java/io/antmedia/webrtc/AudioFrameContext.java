package io.antmedia.webrtc;

import org.bytedeco.ffmpeg.avutil.AVFrame;

public class AudioFrameContext 
{
	public long timestampMs;
	public int numberOfFrames;
	public int channels;
	public int sampleRate;
	public byte[] data;

	public AudioFrameContext(byte[] data, long timestampMS, int numberOfFrames, int channels, int sampleRate) {
		this.data = data;
		this.timestampMs = timestampMS;
		this.numberOfFrames = numberOfFrames;
		this.channels = channels;
		this.sampleRate = sampleRate;
	}
}
