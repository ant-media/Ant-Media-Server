/*
 *  Copyright (c) 2015 The WebRTC project authors. All Rights Reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

package org.webrtc.audio;

import java.lang.Thread;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import javax.annotation.Nullable;
import org.webrtc.audio.JavaAudioDeviceModule.AudioTrackErrorCallback;
import org.webrtc.audio.JavaAudioDeviceModule.AudioTrackStartErrorCode;

import org.webrtc.CalledByNative;

public class WebRtcAudioTrack {
	private static final String TAG = "WebRtcAudioTrackExternal";

	// Default audio data format is PCM 16 bit per sample.
	// Guaranteed to be supported by all devices.
	private static final int BITS_PER_SAMPLE = 16;

	// Requested size of each recorded buffer provided to the client.
	private static final int CALLBACK_BUFFER_SIZE_MS = 10;

	// Average number of callbacks per second.
	private static final int BUFFERS_PER_SECOND = 1000 / CALLBACK_BUFFER_SIZE_MS;

	// The AudioTrackThread is allowed to wait for successful call to join()
	// but the wait times out afther this amount of time.
	private static final long AUDIO_TRACK_THREAD_JOIN_TIMEOUT_MS = 2000;

	// By default, WebRTC creates audio tracks with a usage attribute
	// corresponding to voice communications, such as telephony or VoIP.
	//private static final int DEFAULT_USAGE = getDefaultUsageAttribute();


	private long nativeAudioTrack;
	// private final Context context;
	// private final AudioManager audioManager;
	// private final ThreadUtils.ThreadChecker threadChecker = new ThreadUtils.ThreadChecker();

	private ByteBuffer byteBuffer;

	//private @Nullable AudioTrack audioTrack = null;
	//private @Nullable AudioTrackThread audioThread = null;

	// Samples to be played are replaced by zeros if |speakerMute| is set to true.
	// Can be used to ensure that the speaker is fully muted.
	private volatile boolean speakerMute = false;
	private byte[] emptyBytes;

	private final @Nullable AudioTrackErrorCallback errorCallback;

	private int sampleRate;

	private int bytesPerSample;

	private int channels;

	private int readSizeInBytes;

	/**
	 * Audio thread which keeps calling AudioTrack.write() to stream audio.
	 * Data is periodically acquired from the native WebRTC layer using the
	 * nativeGetPlayoutData callback function.
	 * This thread uses a Process.THREAD_PRIORITY_URGENT_AUDIO priority.
	 */
	/*
	private class AudioTrackThread extends Thread {
		private volatile boolean keepAlive = true;

		public AudioTrackThread(String name) {
			super(name);
		}

		@Override
		public void run() {
			System.out.println("AudioTrackThread" + this);

			// Fixed size in bytes of each 10ms block of audio data that we ask for
			// using callbacks to the native WebRTC client.
			final int sizeInBytes = byteBuffer.capacity();

			while (keepAlive) {
				// Get 10ms of PCM data from the native WebRTC client. Audio data is
				// written into the common ByteBuffer using the address that was
				// cached at construction.
				nativeGetPlayoutData(nativeAudioTrack, sizeInBytes);
				// Write data until all data has been written to the audio sink.
				// Upon return, the buffer position will have been advanced to reflect
				// the amount of data that was successfully written to the AudioTrack.
				assertTrue(sizeInBytes <= byteBuffer.remaining());

				//TODO: Start ---------------------------
				//data is ready and callback a function
				//data is bytebuffer and lenght is sizeInBytes
				//TODO: End -----------------------------
				//byte[] data = new byte[sizeInBytes];
				//byteBuffer.rewind();
				//byteBuffer.get(data);

				audioTrackListener.onData(byteBuffer, bytesPerSample, sampleRate, channels);

				// The byte buffer must be rewinded since byteBuffer.position() is
				// increased at each call to AudioTrack.write(). If we don't do this,
				// next call to AudioTrack.write() will fail.
				byteBuffer.rewind();


			}


		}

		// Stops the inner thread loop which results in calling AudioTrack.stop().
		// Does not block the calling thread.
		public void stopThread() {
			System.out.println("stopThread");
			keepAlive = false;
		}
	}
	*/


	public ByteBuffer getPlayoutData() {
		byteBuffer.rewind();
		// Fixed size in bytes of each 10ms block of audio data that we ask for
		// using callbacks to the native WebRTC client.
		final int sizeInBytes = byteBuffer.capacity();
		// Get 10ms of PCM data from the native WebRTC client. Audio data is
		// written into the common ByteBuffer using the address that was
		// cached at construction.
		nativeGetPlayoutData(nativeAudioTrack, sizeInBytes);
		
		this.readSizeInBytes = sizeInBytes;
		assertTrue(sizeInBytes <= byteBuffer.remaining());
		
		return byteBuffer;
	}
	
	public int getReadSizeInBytes() {
		return readSizeInBytes;
	}
	

	@CalledByNative
	WebRtcAudioTrack(Object context, Object audioManager) {
		this(context, audioManager, null /* errorCallback */);
	}

	WebRtcAudioTrack(
			Object context, Object audioManager, @Nullable AudioTrackErrorCallback errorCallback) {
		//threadChecker.detachThread();
		//this.context = context;
		//this.audioManager = audioManager;
		this.errorCallback = errorCallback;
		//this.volumeLogger = new VolumeLogger(audioManager);
	}

	@CalledByNative
	public void setNativeAudioTrack(long nativeAudioTrack) {
		this.nativeAudioTrack = nativeAudioTrack;
	}

	@CalledByNative
	private boolean initPlayout(int sampleRate, int channels) {
		//threadChecker.checkIsOnValidThread();
		System.out.println("initPlayout(sampleRate=" + sampleRate + ", channels=" + channels + ")");
		this.sampleRate = sampleRate;

		final int bytesPerFrame = channels * (BITS_PER_SAMPLE / 8);
		this.bytesPerSample = bytesPerFrame;
		this.channels = channels;
		byteBuffer = ByteBuffer.allocateDirect(bytesPerFrame * (sampleRate / BUFFERS_PER_SECOND));
		System.out.println("byteBuffer.capacity: " + byteBuffer.capacity());
		emptyBytes = new byte[byteBuffer.capacity()];
		// Rather than passing the ByteBuffer with every callback (requiring
		// the potentially expensive GetDirectBufferAddress) we simply have the
		// the native class cache the address to the memory once.
		nativeCacheDirectBufferAddress(nativeAudioTrack, byteBuffer);

		return true;
	}

	@CalledByNative
	private boolean startPlayout() {
		System.out.println("startPlayout");
		//audioThread = new AudioTrackThread("AudioTrackJavaThread");
		//audioThread.setPriority(Thread.MAX_PRIORITY);
		//audioThread.start();
		return true;
	}
	
	public int getBytesPerSample() {
		return bytesPerSample;
	}

	@CalledByNative
	private boolean stopPlayout() {
		System.out.println("stopPlayout");

		//audioThread.stopThread();

		System.out.println("Stopping the AudioTrackThread...");
		//audioThread.interrupt();

		System.out.println("AudioTrackThread has now been stopped.");
		//audioThread = null;


		releaseAudioResources();
		return true;
	}


	// Get max possible volume index for a phone call audio stream.
	@CalledByNative
	private int getStreamMaxVolume() {
		System.out.println("getStreamMaxVolume");
		//return audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL);
		return 0;
	}

	// Set current volume level for a phone call audio stream.
	@CalledByNative
	private boolean setStreamVolume(int volume) {
		//threadChecker.checkIsOnValidThread();
		System.out.println("setStreamVolume(" + volume + ")");
		//if (isVolumeFixed()) 
		{
			System.out.println("The device implements a fixed volume policy.");
			return false;
		}
		//audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, volume, 0);
		//return true;
	}


	/** Get current volume level for a phone call audio stream. */
	@CalledByNative
	private int getStreamVolume() {
		System.out.println("getStreamVolume");
		//return audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL);
		return 0;
	}

	// Helper method which throws an exception  when an assertion has failed.
	private static void assertTrue(boolean condition) {
		if (!condition) {
			throw new AssertionError("Expected condition to be true");
		}
	}

	private static native void nativeCacheDirectBufferAddress(
			long nativeAudioTrackJni, ByteBuffer byteBuffer);
	private static native void nativeGetPlayoutData(long nativeAudioTrackJni, int bytes);

	// Sets all samples to be played out to zero if |mute| is true, i.e.,
	// ensures that the speaker is muted.
	public void setSpeakerMute(boolean mute) {
		System.out.println( "setSpeakerMute(" + mute + ")");
		speakerMute = mute;
	}

	// Releases the native AudioTrack resources.
	private void releaseAudioResources() {
		System.out.println("releaseAudioResources");
	}

	private void reportWebRtcAudioTrackInitError(String errorMessage) {
		System.out.println("Init playout error: " + errorMessage);
		//WebRtcAudioUtils.logAudioState(TAG, context, audioManager);
		if (errorCallback != null) {
			errorCallback.onWebRtcAudioTrackInitError(errorMessage);
		}
	}

	private void reportWebRtcAudioTrackStartError(
			AudioTrackStartErrorCode errorCode, String errorMessage) {
		System.out.println("Start playout error: " + errorCode + ". " + errorMessage);
		//WebRtcAudioUtils.logAudioState(TAG, context, audioManager);
		if (errorCallback != null) {
			errorCallback.onWebRtcAudioTrackStartError(errorCode, errorMessage);
		}
	}

	private void reportWebRtcAudioTrackError(String errorMessage) {
		System.out.println( "Run-time playback error: " + errorMessage);
		//WebRtcAudioUtils.logAudioState(TAG, context, audioManager);
		if (errorCallback != null) {
			errorCallback.onWebRtcAudioTrackError(errorMessage);
		}
	}

	public int getSampleRate() {
		return sampleRate;
	}

	public int getChannels() {
		return channels;
	}
}
