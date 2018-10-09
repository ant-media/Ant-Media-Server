/*
 *  Copyright 2018 The WebRTC project authors. All Rights Reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

package org.webrtc.audio;

import org.webrtc.JniCommon;

import io.antmedia.webrtc.api.IAudioRecordListener;

/**
 * AudioDeviceModule implemented using android.media.AudioRecord as input and
 * android.media.AudioTrack as output.
 */
/**
 * It's being changed in-place because jni functions are implemented with package name
 * @author mekya
 */
public class JavaAudioDeviceModule implements AudioDeviceModule {
  private static final String TAG = "JavaAudioDeviceModule";

  public static Builder builder(Object context) {
    return new Builder(context);
  }

  
  private static final int DEFAULT_SAMPLE_RATE_HZ = 16000;
  
  
  public static class Builder {
    private int sampleRate;
    private int audioSource = WebRtcAudioRecord.DEFAULT_AUDIO_SOURCE;
    private AudioTrackErrorCallback audioTrackErrorCallback;
    private AudioRecordErrorCallback audioRecordErrorCallback;
    private SamplesReadyCallback samplesReadyCallback;
    private boolean useHardwareAcousticEchoCanceler = isBuiltInAcousticEchoCancelerSupported();
    private boolean useHardwareNoiseSuppressor = isBuiltInNoiseSuppressorSupported();
    private boolean useStereoInput;
    private boolean useStereoOutput;
	private IAudioRecordListener audioRecordListener;

    private Builder(Object context) {
    		this.sampleRate = DEFAULT_SAMPLE_RATE_HZ;
    }

    /**
     * Call this method if the default handling of querying the native sample rate shall be
     * overridden. Can be useful on some devices where the available Android APIs are known to
     * return invalid results.
     */
    public Builder setSampleRate(int sampleRate) {
    	  System.out.println("Sample rate overridden to: " + sampleRate);
      this.sampleRate = sampleRate;
      return this;
    }
    
    public Builder setAudioRecordListener(IAudioRecordListener audioRecordListener) {
		this.audioRecordListener = audioRecordListener;
		return this;
	}

    /**
     * Call this to change the audio source. The argument should be one of the values from
     * android.media.MediaRecorder.AudioSource. The default is AudioSource.VOICE_COMMUNICATION.
     */
    public Builder setAudioSource(int audioSource) {
      this.audioSource = audioSource;
      return this;
    }

    /**
     * Set a callback to retrieve errors from the AudioTrack.
     */
    public Builder setAudioTrackErrorCallback(AudioTrackErrorCallback audioTrackErrorCallback) {
      this.audioTrackErrorCallback = audioTrackErrorCallback;
      return this;
    }

    /**
     * Set a callback to retrieve errors from the AudioRecord.
     */
    public Builder setAudioRecordErrorCallback(AudioRecordErrorCallback audioRecordErrorCallback) {
      this.audioRecordErrorCallback = audioRecordErrorCallback;
      return this;
    }

    /**
     * Set a callback to listen to the raw audio input from the AudioRecord.
     */
    public Builder setSamplesReadyCallback(SamplesReadyCallback samplesReadyCallback) {
      this.samplesReadyCallback = samplesReadyCallback;
      return this;
    }

    /**
     * Control if the built-in HW noise suppressor should be used or not. The default is on if it is
     * supported. It is possible to query support by calling isBuiltInNoiseSuppressorSupported().
     */
    public Builder setUseHardwareNoiseSuppressor(boolean useHardwareNoiseSuppressor) {
      if (useHardwareNoiseSuppressor && !isBuiltInNoiseSuppressorSupported()) {
    	  	System.out.println("HW NS not supported");
        useHardwareNoiseSuppressor = false;
      }
      this.useHardwareNoiseSuppressor = useHardwareNoiseSuppressor;
      return this;
    }

    /**
     * Control if the built-in HW acoustic echo canceler should be used or not. The default is on if
     * it is supported. It is possible to query support by calling
     * isBuiltInAcousticEchoCancelerSupported().
     */
    public Builder setUseHardwareAcousticEchoCanceler(boolean useHardwareAcousticEchoCanceler) {
      if (useHardwareAcousticEchoCanceler && !isBuiltInAcousticEchoCancelerSupported()) {
    	  	System.out.println("HW AEC not supported");
        useHardwareAcousticEchoCanceler = false;
      }
      this.useHardwareAcousticEchoCanceler = useHardwareAcousticEchoCanceler;
      return this;
    }

    /**
     * Control if stereo input should be used or not. The default is mono.
     */
    public Builder setUseStereoInput(boolean useStereoInput) {
      this.useStereoInput = useStereoInput;
      return this;
    }

    /**
     * Control if stereo output should be used or not. The default is mono.
     */
    public Builder setUseStereoOutput(boolean useStereoOutput) {
      this.useStereoOutput = useStereoOutput;
      return this;
    }

    /**
     * Construct an AudioDeviceModule based on the supplied arguments. The caller takes ownership
     * and is responsible for calling release().
     */
    public AudioDeviceModule createAudioDeviceModule() {
    		System.out.println("createAudioDeviceModule");
      if (useHardwareNoiseSuppressor) {
    	  	System.out.println("HW NS will be used.");
      } else {
        if (isBuiltInNoiseSuppressorSupported()) {
        		System.out.println("Overriding default behavior; now using WebRTC NS!");
        }
        System.out.println("HW NS will not be used.");
      }
      if (useHardwareAcousticEchoCanceler) {
    	  	System.out.println("HW AEC will be used.");
      } else {
        if (isBuiltInAcousticEchoCancelerSupported()) {
        	System.out.println("Overriding default behavior; now using WebRTC AEC!");
        }
        System.out.println("HW AEC will not be used.");
      }
      final WebRtcAudioRecord audioInput =
          new WebRtcAudioRecord(null, null, audioSource, audioRecordErrorCallback,
              samplesReadyCallback, useHardwareAcousticEchoCanceler, useHardwareNoiseSuppressor, audioRecordListener);
      final WebRtcAudioTrack audioOutput =
          new WebRtcAudioTrack(null, null, audioTrackErrorCallback);
      return new JavaAudioDeviceModule(null, null, audioInput, audioOutput, sampleRate,
          useStereoInput, useStereoOutput);
    }
  }

  /* AudioRecord */
  // Audio recording error handler functions.
  public enum AudioRecordStartErrorCode {
    AUDIO_RECORD_START_EXCEPTION,
    AUDIO_RECORD_START_STATE_MISMATCH,
  }

  public static interface AudioRecordErrorCallback {
    void onWebRtcAudioRecordInitError(String errorMessage);
    void onWebRtcAudioRecordStartError(AudioRecordStartErrorCode errorCode, String errorMessage);
    void onWebRtcAudioRecordError(String errorMessage);
  }

  /**
   * Contains audio sample information.
   */
  public static class AudioSamples {
    /** See {@link AudioRecord#getAudioFormat()} */
    private final int audioFormat;
    /** See {@link AudioRecord#getChannelCount()} */
    private final int channelCount;
    /** See {@link AudioRecord#getSampleRate()} */
    private final int sampleRate;

    private final byte[] data;

    public AudioSamples(int audioFormat, int channelCount, int sampleRate, byte[] data) {
      this.audioFormat = audioFormat;
      this.channelCount = channelCount;
      this.sampleRate = sampleRate;
      this.data = data;
    }

    public int getAudioFormat() {
      return audioFormat;
    }

    public int getChannelCount() {
      return channelCount;
    }

    public int getSampleRate() {
      return sampleRate;
    }

    public byte[] getData() {
      return data;
    }
  }

  /** Called when new audio samples are ready. This should only be set for debug purposes */
  public static interface SamplesReadyCallback {
    void onWebRtcAudioRecordSamplesReady(AudioSamples samples);
  }

  /* AudioTrack */
  // Audio playout/track error handler functions.
  public enum AudioTrackStartErrorCode {
    AUDIO_TRACK_START_EXCEPTION,
    AUDIO_TRACK_START_STATE_MISMATCH,
  }

  public static interface AudioTrackErrorCallback {
    void onWebRtcAudioTrackInitError(String errorMessage);
    void onWebRtcAudioTrackStartError(AudioTrackStartErrorCode errorCode, String errorMessage);
    void onWebRtcAudioTrackError(String errorMessage);
  }

  /**
   * Returns true if the device supports built-in HW AEC, and the UUID is approved (some UUIDs can
   * be excluded).
   */
  public static boolean isBuiltInAcousticEchoCancelerSupported() {
    return false;
  }

  /**
   * Returns true if the device supports built-in HW NS, and the UUID is approved (some UUIDs can be
   * excluded).
   */
  public static boolean isBuiltInNoiseSuppressorSupported() {
    return false;
  }

//  private final Context context;
//  private final AudioManager audioManager;
  
  private final Object context;
  private final Object audioManager;
  private final WebRtcAudioRecord audioInput;
  private final WebRtcAudioTrack audioOutput;
  private final int sampleRate;
  private final boolean useStereoInput;
  private final boolean useStereoOutput;

  private final Object nativeLock = new Object();
  private long nativeAudioDeviceModule;

  private JavaAudioDeviceModule(Object context, Object audioManager,
      WebRtcAudioRecord audioInput, WebRtcAudioTrack audioOutput, int sampleRate,
      boolean useStereoInput, boolean useStereoOutput) {
    this.context = context;
    this.audioManager = audioManager;
    this.audioInput = audioInput;
    this.audioOutput = audioOutput;
    this.sampleRate = sampleRate;
    this.useStereoInput = useStereoInput;
    this.useStereoOutput = useStereoOutput;
  }
  
  public WebRtcAudioTrack getAudioTrack() {
	  return audioOutput;
  }
  
  public WebRtcAudioRecord getAudioRecord() {
	  return audioInput;
  }
 

  @Override
  public long getNativeAudioDeviceModulePointer() {
    synchronized (nativeLock) {
      if (nativeAudioDeviceModule == 0) {
        nativeAudioDeviceModule = nativeCreateAudioDeviceModule(context, audioManager, audioInput,
            audioOutput, sampleRate, useStereoInput, useStereoOutput);
      }
      return nativeAudioDeviceModule;
    }
  }

  @Override
  public void release() {
    synchronized (nativeLock) {
      if (nativeAudioDeviceModule != 0) {
        JniCommon.nativeReleaseRef(nativeAudioDeviceModule);
        nativeAudioDeviceModule = 0;
      }
    }
  }

  @Override
  public void setSpeakerMute(boolean mute) {
	System.out.println("setSpeakerMute: " + mute);
    audioOutput.setSpeakerMute(mute);
  }

  @Override
  public void setMicrophoneMute(boolean mute) {
	System.out.println("setMicrophoneMute: " + mute);
    audioInput.setMicrophoneMute(mute);
  }

  private static native long nativeCreateAudioDeviceModule(Object context,
      Object audioManager, WebRtcAudioRecord audioInput, WebRtcAudioTrack audioOutput,
      int sampleRate, boolean useStereoInput, boolean useStereoOutput);
}
