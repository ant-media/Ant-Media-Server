package org.webrtc;

public abstract class AudioSink {
	long nativeAudioSink;

	public AudioSink() {
		nativeAudioSink = nativeWrapAudioSink(this);
	}

	public void dispose() {
		if (nativeAudioSink == 0) {
			// Already disposed.
			return;
		}

		freeWrappedAudioSink(nativeAudioSink);
		nativeAudioSink = 0;
	}

	private static native long nativeWrapAudioSink(AudioSink callbacks);
	private static native void freeWrappedAudioSink(long nativeAudioSink);


	public abstract void onData(byte[] audio_data, int bits_per_sample, int sample_rate, int number_of_channels,
			int number_of_frames);
}
