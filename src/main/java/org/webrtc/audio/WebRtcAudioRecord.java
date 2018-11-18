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


import java.lang.System;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webrtc.CalledByNative;
import org.webrtc.audio.JavaAudioDeviceModule.AudioRecordErrorCallback;
import org.webrtc.audio.JavaAudioDeviceModule.AudioRecordStartErrorCode;
import org.webrtc.audio.JavaAudioDeviceModule.SamplesReadyCallback;

import io.antmedia.webrtc.api.IAudioRecordListener;

public class WebRtcAudioRecord {
	private static final String TAG = "WebRtcAudioRecordExternal";

	// Default audio data format is PCM 16 bit per sample.
	// Guaranteed to be supported by all devices.
	private static final int BITS_PER_SAMPLE = 16;

	// Requested size of each recorded buffer provided to the client.
	private static final int CALLBACK_BUFFER_SIZE_MS = 10;

	// Average number of callbacks per second.
	private static final int BUFFERS_PER_SECOND = 1000 / CALLBACK_BUFFER_SIZE_MS;

	// We ask for a native buffer size of BUFFER_SIZE_FACTOR * (minimum required
	// buffer size). The extra space is allocated to guard against glitches under
	// high load.
	private static final int BUFFER_SIZE_FACTOR = 2;

	// The AudioRecordJavaThread is allowed to wait for successful call to join()
	// but the wait times out afther this amount of time.
	private static final long AUDIO_RECORD_THREAD_JOIN_TIMEOUT_MS = 2000;

	public static final int DEFAULT_AUDIO_SOURCE = 0; //AudioSource.VOICE_COMMUNICATION

	//  private final Context context
	//  private final AudioManager audioManager
	private final int audioSource;

	private long nativeAudioRecord;

	private @Nullable ByteBuffer byteBuffer;

	//private @Nullable AudioRecord audioRecord = null
	//private @Nullable AudioRecordThread audioThread = null
	
	
	private static Logger logger = LoggerFactory.getLogger(WebRtcAudioRecord.class);

	private volatile boolean microphoneMute = false;
	private byte[] emptyBytes;

	private final @Nullable AudioRecordErrorCallback errorCallback;
	private final @Nullable SamplesReadyCallback audioSamplesReadyCallback;
	private final boolean isAcousticEchoCancelerSupported;
	private final boolean isNoiseSuppressorSupported;

	private ByteBuffer encodedByteBuffer;

	private IAudioRecordListener audioRecordListener;


	public void notifyDataIsReady(byte[] audioData) {
		byteBuffer.clear();
		byteBuffer.put(audioData);

		nativeDataIsRecorded(nativeAudioRecord, audioData.length);
	}

	/**
	 * 10ms of audio data
	 */
	public void notifyDataWithEmptyBuffer() {
		byteBuffer.clear();
		byteBuffer.put(emptyBytes);
		
		nativeDataIsRecorded(nativeAudioRecord, emptyBytes.length);
	}

	/**
	 * @param audio => 20ms of encoded audio data
	 */
	public void notifyEncodedData(byte[] audio) {
		encodedByteBuffer.clear();
		encodedByteBuffer.put(audio);
		nativeEncodedDataIsReady(nativeAudioRecord, audio.length);
	}


	@CalledByNative
	WebRtcAudioRecord(Object context, Object audioManager) {
		this(context, audioManager, DEFAULT_AUDIO_SOURCE, null /* errorCallback */,
				null /* audioSamplesReadyCallback */, false,
				false, null);
	}

	public WebRtcAudioRecord(Object context, Object audioManager, int audioSource,
			@Nullable AudioRecordErrorCallback errorCallback,
			@Nullable SamplesReadyCallback audioSamplesReadyCallback,
			boolean isAcousticEchoCancelerSupported, boolean isNoiseSuppressorSupported, IAudioRecordListener audioRecordListener) {
		if (isAcousticEchoCancelerSupported) {
			throw new IllegalArgumentException("HW AEC not supported");
		}
		if (isNoiseSuppressorSupported) {
			throw new IllegalArgumentException("HW NS not supported");
		}
		this.audioSource = audioSource;
		this.errorCallback = errorCallback;
		this.audioSamplesReadyCallback = audioSamplesReadyCallback;
		this.isAcousticEchoCancelerSupported = isAcousticEchoCancelerSupported;
		this.isNoiseSuppressorSupported = isNoiseSuppressorSupported;
		this.audioRecordListener = audioRecordListener;
	}

	@CalledByNative
	public void setNativeAudioRecord(long nativeAudioRecord) {
		this.nativeAudioRecord = nativeAudioRecord;
	}

	@CalledByNative
	boolean isAcousticEchoCancelerSupported() {
		return isAcousticEchoCancelerSupported;
	}

	@CalledByNative
	boolean isNoiseSuppressorSupported() {
		return isNoiseSuppressorSupported;
	}

	@CalledByNative
	private boolean enableBuiltInAEC(boolean enable) {
		System.out.println("enableBuiltInAEC(" + enable + ")");
		return false;
	}

	@CalledByNative
	private boolean enableBuiltInNS(boolean enable) {
		System.out.println("enableBuiltInNS(" + enable + ")");
		return false;
	}

	@CalledByNative
	private int initRecording(int sampleRate, int channels) {
		System.out.println("initRecording(sampleRate=" + sampleRate + ", channels=" + channels + ")");

		final int bytesPerFrame = channels * (BITS_PER_SAMPLE / 8);
		final int framesPerBuffer = sampleRate / BUFFERS_PER_SECOND;
		byteBuffer = ByteBuffer.allocateDirect(bytesPerFrame * framesPerBuffer);
		//if (!(byteBuffer.hasArray())) {
		//	reportWebRtcAudioRecordInitError("ByteBuffer does not have backing array.");
		//	return -1;
		//}
		System.out.println("byteBuffer.capacity: " + byteBuffer.capacity());
		emptyBytes = new byte[byteBuffer.capacity()];
		// Rather than passing the ByteBuffer with every callback (requiring
		// the potentially expensive GetDirectBufferAddress) we simply have the
		// the native class cache the address to the memory once.
		nativeCacheDirectBufferAddress(nativeAudioRecord, byteBuffer);

		encodedByteBuffer = ByteBuffer.allocateDirect(byteBuffer.capacity()*3);
		nativeCacheDirectBufferAddressForEncodedAudio(nativeAudioRecord, encodedByteBuffer);

		return framesPerBuffer;
	}

	@CalledByNative
	private boolean startRecording() {
		System.out.println("startRecording");
		if (audioRecordListener != null) {
			audioRecordListener.audioRecordStarted();
		}
		return true;
	}

	@CalledByNative
	private boolean stopRecording() {
		System.out.println("stopRecording");
		if (audioRecordListener != null) {
			audioRecordListener.audioRecordStoppped();
		}
		releaseAudioResources();
		return true;
	}

	// Helper method which throws an exception  when an assertion has failed.
	private static void assertTrue(boolean condition) {
		if (!condition) {
			throw new AssertionError("Expected condition to be true");
		}
	}

	private native void nativeCacheDirectBufferAddress(
			long nativeAudioRecordJni, ByteBuffer byteBuffer);
	private native void nativeDataIsRecorded(long nativeAudioRecordJni, int bytes);

	private native void nativeCacheDirectBufferAddressForEncodedAudio(
			long nativeAudioRecordJni, ByteBuffer byteBuffer);

	private native void nativeEncodedDataIsReady(long nativeAudioRecordJni, int bytes);

	// Sets all recorded samples to zero if |mute| is true, i.e., ensures that
	// the microphone is muted.
	public void setMicrophoneMute(boolean mute) {
		System.out.println("setMicrophoneMute(" + mute + ")");
		microphoneMute = mute;
	}

	// Releases the native AudioRecord resources.
	private void releaseAudioResources() {
		System.out.println("releaseAudioResources");
	}

	private void reportWebRtcAudioRecordInitError(String errorMessage) {
		System.out.println("Init recording error: " + errorMessage);
		if (errorCallback != null) {
			errorCallback.onWebRtcAudioRecordInitError(errorMessage);
		}
	}

	private void reportWebRtcAudioRecordStartError(
			AudioRecordStartErrorCode errorCode, String errorMessage) {
		System.out.println("Start recording error: " + errorCode + ". " + errorMessage);
		if (errorCallback != null) {
			errorCallback.onWebRtcAudioRecordStartError(errorCode, errorMessage);
		}
	}

	private void reportWebRtcAudioRecordError(String errorMessage) {
		System.out.println("Run-time recording error: " + errorMessage);
		if (errorCallback != null) {
			errorCallback.onWebRtcAudioRecordError(errorMessage);
		}
	}


}
