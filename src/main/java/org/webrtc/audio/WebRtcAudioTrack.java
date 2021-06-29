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

import java.nio.ByteBuffer;

import javax.annotation.Nullable;

import org.webrtc.AudioTrack;
import org.webrtc.CalledByNative;
import org.webrtc.Logging;
import org.webrtc.ThreadUtils;
import org.webrtc.audio.JavaAudioDeviceModule.AudioTrackErrorCallback;
import org.webrtc.audio.JavaAudioDeviceModule.AudioTrackStartErrorCode;
import org.webrtc.audio.JavaAudioDeviceModule.AudioTrackStateCallback;

import io.antmedia.webrtc.api.IAudioTrackListener;

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
  private static final int DEFAULT_USAGE = 0; //getDefaultUsageAttribute();


  // Indicates the AudioTrack has started playing audio.
  private static final int AUDIO_TRACK_START = 0;

  // Indicates the AudioTrack has stopped playing audio.
  private static final int AUDIO_TRACK_STOP = 1;

  private long nativeAudioTrack;

  private final ThreadUtils.ThreadChecker threadChecker = new ThreadUtils.ThreadChecker();

  private ByteBuffer byteBuffer;

  private @Nullable AudioTrack audioTrack;

  // Samples to be played are replaced by zeros if |speakerMute| is set to true.
  // Can be used to ensure that the speaker is fully muted.
  private volatile boolean speakerMute;
  private byte[] emptyBytes;

  private final @Nullable AudioTrackErrorCallback errorCallback;
  private final @Nullable AudioTrackStateCallback stateCallback;

  private int readSizeInBytes;
  
  private int sampleRate;

  private int bytesPerSample;
  
  private int channels;
	
  private IAudioTrackListener audioTrackListener;
  
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
    this(context, audioManager, null /* errorCallback */, null /* stateCallback */, null);
  }

  WebRtcAudioTrack(Object context, Object audioManager,
      @Nullable AudioTrackErrorCallback errorCallback,
      @Nullable AudioTrackStateCallback stateCallback, IAudioTrackListener audioTrackListener) {
    threadChecker.detachThread();
    this.errorCallback = errorCallback;
    this.stateCallback = stateCallback;
    this.audioTrackListener = audioTrackListener;
  }

  @CalledByNative
  public void setNativeAudioTrack(long nativeAudioTrack) {
    this.nativeAudioTrack = nativeAudioTrack;
  }

  @CalledByNative
  private boolean initPlayout(int sampleRate, int channels, double bufferSizeFactor) {
    threadChecker.checkIsOnValidThread();
    this.sampleRate = sampleRate;
    Logging.d(TAG,
            "initPlayout(sampleRate=" + sampleRate + ", channels=" + channels
                + ", bufferSizeFactor=" + bufferSizeFactor + ")");
    final int bytesPerFrame = channels * (BITS_PER_SAMPLE / 8);
    
    this.bytesPerSample = bytesPerFrame;
	this.channels = channels;
	byteBuffer = ByteBuffer.allocateDirect(bytesPerFrame * (sampleRate / BUFFERS_PER_SECOND));
	Logging.d(TAG, "byteBuffer.capacity: " + byteBuffer.capacity());
	emptyBytes = new byte[byteBuffer.capacity()];
	
    // Rather than passing the ByteBuffer with every callback (requiring
    // the potentially expensive GetDirectBufferAddress) we simply have the
    // the native class cache the address to the memory once.
    nativeCacheDirectBufferAddress(nativeAudioTrack, byteBuffer);
	
    return true;
  }

  @CalledByNative
  private boolean startPlayout() {
    threadChecker.checkIsOnValidThread();
    Logging.d(TAG,
            "startPlayout(sampleRate=" + sampleRate + ", channels=" + channels
                + ")");
    if (this.audioTrackListener != null) {
		this.audioTrackListener.playoutStarted();
	}
    return true;
  }
  
  public int getBytesPerSample() {
		return bytesPerSample;
  }

  @CalledByNative
  private boolean stopPlayout() {
	  threadChecker.checkIsOnValidThread();
	  if (this.audioTrackListener != null) {
		this.audioTrackListener.playoutStopped();
		this.audioTrackListener = null;
	  }
	  return true;
  }

  // Get max possible volume index for a phone call audio stream.
  @CalledByNative
  private int getStreamMaxVolume() {
    threadChecker.checkIsOnValidThread();
    Logging.d(TAG, "getStreamMaxVolume");
    return 0;
  }

  // Set current volume level for a phone call audio stream.
  @CalledByNative
  private boolean setStreamVolume(int volume) {
    threadChecker.checkIsOnValidThread();
    Logging.d(TAG, "setStreamVolume(" + volume + ")");
    Logging.e(TAG, "The device implements a fixed volume policy.");
    return false;
  }



  /** Get current volume level for a phone call audio stream. */
  @CalledByNative
  private int getStreamVolume() {
    threadChecker.checkIsOnValidThread();
    Logging.d(TAG, "getStreamVolume");
    return 0;
  }

  @CalledByNative
  private int GetPlayoutUnderrunCount() {
	  
	Logging.d(TAG, "GetPlayoutUnderrunCount");
	return -2;
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
    Logging.w(TAG, "setSpeakerMute(" + mute + ")");
    speakerMute = mute;
  }

  private void reportWebRtcAudioTrackInitError(String errorMessage) {
    Logging.e(TAG, "Init playout error: " + errorMessage);
    if (errorCallback != null) {
      errorCallback.onWebRtcAudioTrackInitError(errorMessage);
    }
  }

  private void reportWebRtcAudioTrackStartError(
      AudioTrackStartErrorCode errorCode, String errorMessage) {
    Logging.e(TAG, "Start playout error: " + errorCode + ". " + errorMessage);
    if (errorCallback != null) {
      errorCallback.onWebRtcAudioTrackStartError(errorCode, errorMessage);
    }
  }

  private void reportWebRtcAudioTrackError(String errorMessage) {
    Logging.e(TAG, "Run-time playback error: " + errorMessage);
    if (errorCallback != null) {
      errorCallback.onWebRtcAudioTrackError(errorMessage);
    }
  }

  private void doAudioTrackStateCallback(int audioState) {
    Logging.d(TAG, "doAudioTrackStateCallback: " + audioState);
    if (stateCallback != null) {
      if (audioState == WebRtcAudioTrack.AUDIO_TRACK_START) {
        stateCallback.onWebRtcAudioTrackStart();
      } else if (audioState == WebRtcAudioTrack.AUDIO_TRACK_STOP) {
        stateCallback.onWebRtcAudioTrackStop();
      } else {
        Logging.e(TAG, "Invalid audio state");
      }
    }
  }
  
  public int getSampleRate() {
	return sampleRate;
  }
	
  public int getChannels() {
	return channels;
  }
}
