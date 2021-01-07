package io.antmedia.webrtc;

public class AudioFrameContext 
{
	public final byte[] data;
	public final long timestampMs;
	public final int numberOfFrames;
	public final int channels;
	public final int sampleRate;

	public AudioFrameContext(byte[] data, long timestampMS, int numberOfFrames, int channels, int sampleRate) {
		this.data = data;
		this.timestampMs = timestampMS;
		this.numberOfFrames = numberOfFrames;
		this.channels = channels;
		this.sampleRate = sampleRate;
	}
}
