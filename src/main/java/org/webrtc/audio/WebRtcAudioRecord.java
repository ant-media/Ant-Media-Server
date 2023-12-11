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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webrtc.CalledByNative;
import org.webrtc.Logging;
import org.webrtc.audio.JavaAudioDeviceModule.AudioRecordErrorCallback;
import org.webrtc.audio.JavaAudioDeviceModule.AudioRecordStartErrorCode;
import org.webrtc.audio.JavaAudioDeviceModule.AudioRecordStateCallback;
import org.webrtc.audio.JavaAudioDeviceModule.SamplesReadyCallback;

import io.antmedia.webrtc.api.IAudioRecordListener;

public class WebRtcAudioRecord {

	private static Logger logger = LoggerFactory.getLogger(WebRtcAudioRecord.class);

	private static final String TAG = "WebRtcAudioRecordExternal";

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

	public static final int DEFAULT_AUDIO_SOURCE = 0; //AudioSource.VOICE_COMMUNICATION;

	// Default audio data format is PCM 16 bit per sample.
	// Guaranteed to be supported by all devices.
	public static final int DEFAULT_AUDIO_FORMAT = 0; //AudioFormat.ENCODING_PCM_16BIT;

	// Indicates AudioRecord has started recording audio.
	private static final int AUDIO_RECORD_START = 0;

	// Indicates AudioRecord has stopped recording audio.
	private static final int AUDIO_RECORD_STOP = 1;

	// Time to wait before checking recording status after start has been called. Tests have
	// shown that the result can sometimes be invalid (our own status might be missing) if we check
	// directly after start.
	private static final int CHECK_REC_STATUS_DELAY_MS = 100;

	//private final Context context;
	//private final AudioManager audioManager;
	private final int audioSource;
	private final int audioFormat;

	private long nativeAudioRecord;

	//private final WebRtcAudioEffects effects = new WebRtcAudioEffects();

	private @Nullable ByteBuffer byteBuffer;

	//private @Nullable AudioRecord audioRecord;
	//private @Nullable AudioRecordThread audioThread;
	//private @Nullable AudioDeviceInfo preferredDevice;

	private final ScheduledExecutorService executor;
	private @Nullable ScheduledFuture<String> future;

	private volatile boolean microphoneMute;
	//private final AtomicReference<Boolean> audioSourceMatchesRecordingSessionRef =
	//  new AtomicReference<>();
	private byte[] emptyBytes;

	private final @Nullable AudioRecordErrorCallback errorCallback;
	private final @Nullable AudioRecordStateCallback stateCallback;
	private final @Nullable SamplesReadyCallback audioSamplesReadyCallback;
	private final boolean isAcousticEchoCancelerSupported;
	private final boolean isNoiseSuppressorSupported;

	private IAudioRecordListener audioRecordListener;

	private Map<String, ByteBuffer> encodedByteBuffers = new LinkedHashMap<String, ByteBuffer>();

	/**
	 * Audio thread which keeps calling ByteBuffer.read() waiting for audio
	 * to be recorded. Feeds recorded data to the native counterpart as a
	 * periodic sequence of callbacks using DataIsRecorded().
	 * This thread uses a Process.THREAD_PRIORITY_URGENT_AUDIO priority.
	 */
	/*
  private class AudioRecordThread extends Thread {
    private volatile boolean keepAlive = true;

    public AudioRecordThread(String name) {
      super(name);
    }

    @Override
    public void run() {
      Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO);
      Logging.d(TAG, "AudioRecordThread" + WebRtcAudioUtils.getThreadInfo());
      assertTrue(audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING);

      // Audio recording has started and the client is informed about it.
      doAudioRecordStateCallback(AUDIO_RECORD_START);

      long lastTime = System.nanoTime();
      AudioTimestamp audioTimestamp = null;
      if (Build.VERSION.SDK_INT >= 24) {
        audioTimestamp = new AudioTimestamp();
      }
      while (keepAlive) {
        int bytesRead = audioRecord.read(byteBuffer, byteBuffer.capacity());
        if (bytesRead == byteBuffer.capacity()) {
          if (microphoneMute) {
            byteBuffer.clear();
            byteBuffer.put(emptyBytes);
          }
          // It's possible we've been shut down during the read, and stopRecording() tried and
          // failed to join this thread. To be a bit safer, try to avoid calling any native methods
          // in case they've been unregistered after stopRecording() returned.
          if (keepAlive) {
            long captureTimeNs = 0;
            if (Build.VERSION.SDK_INT >= 24) {
              if (audioRecord.getTimestamp(audioTimestamp, AudioTimestamp.TIMEBASE_MONOTONIC)
                  == AudioRecord.SUCCESS) {
                captureTimeNs = audioTimestamp.nanoTime;
              }
            }
            nativeDataIsRecorded(nativeAudioRecord, bytesRead, captureTimeNs);
          }
          if (audioSamplesReadyCallback != null) {
            // Copy the entire byte buffer array. The start of the byteBuffer is not necessarily
            // at index 0.
            byte[] data = Arrays.copyOfRange(byteBuffer.array(), byteBuffer.arrayOffset(),
                byteBuffer.capacity() + byteBuffer.arrayOffset());
            audioSamplesReadyCallback.onWebRtcAudioRecordSamplesReady(
                new JavaAudioDeviceModule.AudioSamples(audioRecord.getAudioFormat(),
                    audioRecord.getChannelCount(), audioRecord.getSampleRate(), data));
          }
        } else {
          String errorMessage = "AudioRecord.read failed: " + bytesRead;
          Logging.e(TAG, errorMessage);
          if (bytesRead == AudioRecord.ERROR_INVALID_OPERATION) {
            keepAlive = false;
            reportWebRtcAudioRecordError(errorMessage);
          }
        }
      }

      try {
        if (audioRecord != null) {
          audioRecord.stop();
          doAudioRecordStateCallback(AUDIO_RECORD_STOP);
        }
      } catch (IllegalStateException e) {
        Logging.e(TAG, "AudioRecord.stop failed: " + e.getMessage());
      }
    }

    // Stops the inner thread loop and also calls AudioRecord.stop().
    // Does not block the calling thread.
    public void stopThread() {
      Logging.d(TAG, "stopThread");
      keepAlive = false;
    }
  }
	 */
	@CalledByNative
	WebRtcAudioRecord(Object context, Object audioManager) {
		this(context, null /* scheduler */, audioManager, DEFAULT_AUDIO_SOURCE,
				DEFAULT_AUDIO_FORMAT, null /* errorCallback */, null /* stateCallback */,
				null /* audioSamplesReadyCallback */, false,
				false, null);
	}

	public WebRtcAudioRecord(Object context, ScheduledExecutorService scheduler,
			Object audioManager, int audioSource, int audioFormat,
			@Nullable AudioRecordErrorCallback errorCallback,
			@Nullable AudioRecordStateCallback stateCallback,
			@Nullable SamplesReadyCallback audioSamplesReadyCallback,
			boolean isAcousticEchoCancelerSupported, boolean isNoiseSuppressorSupported, IAudioRecordListener audioRecordListener) {
		// if (isAcousticEchoCancelerSupported && !WebRtcAudioEffects.isAcousticEchoCancelerSupported()) {
		//   throw new IllegalArgumentException("HW AEC not supported");
		// }
		// if (isNoiseSuppressorSupported && !WebRtcAudioEffects.isNoiseSuppressorSupported()) {
		//   throw new IllegalArgumentException("HW NS not supported");
		// }
		// this.context = context;
		this.executor = scheduler;
		// this.audioManager = audioManager;
		this.audioSource = audioSource;
		this.audioFormat = audioFormat;
		this.errorCallback = errorCallback;
		this.stateCallback = stateCallback;
		this.audioSamplesReadyCallback = audioSamplesReadyCallback;
		this.isAcousticEchoCancelerSupported = isAcousticEchoCancelerSupported;
		this.isNoiseSuppressorSupported = isNoiseSuppressorSupported;
		this.audioRecordListener = audioRecordListener;
		//   Logging.d(TAG, "ctor" + WebRtcAudioUtils.getThreadInfo());
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

	// Returns true if a valid call to verifyAudioConfig() has been done. Should always be
	// checked before using the returned value of isAudioSourceMatchingRecordingSession().
	@CalledByNative
	boolean isAudioConfigVerified() {
		//return audioSourceMatchesRecordingSessionRef.get() != null;
		return true;
	}

	// Returns true if verifyAudioConfig() succeeds. This value is set after a specific delay when
	// startRecording() has been called. Hence, should preferably be called in combination with
	// stopRecording() to ensure that it has been set properly. `isAudioConfigVerified` is
	// enabled in WebRtcAudioRecord to ensure that the returned value is valid.
	@CalledByNative
	boolean isAudioSourceMatchingRecordingSession() {
		return true;
		/*
    Boolean audioSourceMatchesRecordingSession = audioSourceMatchesRecordingSessionRef.get();
    if (audioSourceMatchesRecordingSession == null) {
      Logging.w(TAG, "Audio configuration has not yet been verified");
      return false;
    }
    return audioSourceMatchesRecordingSession;
		 */
	}

	@CalledByNative
	private boolean enableBuiltInAEC(boolean enable) {
		Logging.d(TAG, "enableBuiltInAEC(" + enable + ")");
		return false;
	}

	@CalledByNative
	private boolean enableBuiltInNS(boolean enable) {
		Logging.d(TAG, "enableBuiltInNS(" + enable + ")");
		return false;
	}

	@CalledByNative
	private int initRecording(int sampleRate, int channels) {
		System.out.println("initRecording(sampleRate=" + sampleRate + ", channels=" + channels + ")");

		//TODO: Different Audio format other than PCM_16 may be supported 
		//below 2 is BITS_PER_SAMPLE(16)/8 = 2 
		final int bytesPerFrame = channels * 2;
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

		return framesPerBuffer;
	}

	/**
	 * Prefer a specific {@link AudioDeviceInfo} device for recording. Calling after recording starts
	 * is valid but may cause a temporary interruption if the audio routing changes.
	 */
	//@RequiresApi(Build.VERSION_CODES.M)
	//@TargetApi(Build.VERSION_CODES.M)
	//void setPreferredDevice(@Nullable AudioDeviceInfo preferredDevice) {
	//Logging.d(
	//  TAG, "setPreferredDevice " + (preferredDevice != null ? preferredDevice.getId() : null));
	//this.preferredDevice = preferredDevice;
	//if (audioRecord != null) {
	//  if (!audioRecord.setPreferredDevice(preferredDevice)) {
	//    Logging.e(TAG, "setPreferredDevice failed");
	//  }
	// }
	//}


	public void triggerAudioRecordStarted() {
		if (audioRecordListener != null) {
			audioRecordListener.audioRecordStarted();
		}
	}

	public void triggerAudioRecordStopped() {
		if (audioRecordListener != null) {
			audioRecordListener.audioRecordStoppped();
			//PAY ATTENTION: We commented out of the line below because in some cases, 
			//WebRTC audio record start and stopped in multitrack(conferencing)
			//If it's stopped, it's not started again because audioRecordListener becomes null
			//We've implemented a basic test code for null-check. 
			//This is the fix of this issue https://github.com/ant-media/Ant-Media-Server/issues/5808
			//@mekya -> Dec 2, 2023
			//audioRecordListener = null
		}
	}
	
	public IAudioRecordListener getAudioRecordListener() {
		return audioRecordListener;
	}

	@CalledByNative
	private boolean startRecording() {
		Logging.d(TAG, "startRecording");

		triggerAudioRecordStarted();
		return true;
	}

	@CalledByNative
	private boolean stopRecording() {
		Logging.d(TAG, "stopRecording");

		triggerAudioRecordStopped();
		return true;
	}

	



	// Helper method which throws an exception  when an assertion has failed.
	private static void assertTrue(boolean condition) {
		if (!condition) {
			throw new AssertionError("Expected condition to be true");
		}
	}

	//  private int channelCountToConfiguration(int channels) {
	//    return (channels == 1 ? AudioFormat.CHANNEL_IN_MONO : AudioFormat.CHANNEL_IN_STEREO);
	//  }

	private native void nativeCacheDirectBufferAddress(
			long nativeAudioRecordJni, ByteBuffer byteBuffer);
	private native void nativeDataIsRecorded(
			long nativeAudioRecordJni, int bytes, long captureTimestampNs);

	private native void nativeCacheDirectBufferAddressForEncodedAudio(
			long nativeAudioRecordJni, String trackId, ByteBuffer byteBuffer);


	public native void nativeEncodedDataIsReady(long nativeAudioRecordJni, String trackId, int bytes);

	public void encodedDataIsReady(long nativeAudioRecordJni, String trackId, int bytes) {
		nativeEncodedDataIsReady(nativeAudioRecordJni, trackId, bytes);
	}


	// Sets all recorded samples to zero if `mute` is true, i.e., ensures that
	// the microphone is muted.
	public void setMicrophoneMute(boolean mute) {
		Logging.w(TAG, "setMicrophoneMute(" + mute + ")");
		microphoneMute = mute;
	}

	// Releases the native AudioRecord resources.
	private void releaseAudioResources() {
		Logging.d(TAG, "releaseAudioResources");
		//if (audioRecord != null) {
		//audioRecord.release();
		//audioRecord = null;
		//}
		//audioSourceMatchesRecordingSessionRef.set(null);
	}

	private void reportWebRtcAudioRecordInitError(String errorMessage) {
		Logging.e(TAG, "Init recording error: " + errorMessage);
		//WebRtcAudioUtils.logAudioState(TAG, context, audioManager);
		//logRecordingConfigurations(audioRecord, false /* verifyAudioConfig */);
		if (errorCallback != null) {
			errorCallback.onWebRtcAudioRecordInitError(errorMessage);
		}
	}

	private void reportWebRtcAudioRecordStartError(
			AudioRecordStartErrorCode errorCode, String errorMessage) {
		Logging.e(TAG, "Start recording error: " + errorCode + ". " + errorMessage);
		// WebRtcAudioUtils.logAudioState(TAG, context, audioManager);
		// logRecordingConfigurations(audioRecord, false /* verifyAudioConfig */);
		if (errorCallback != null) {
			errorCallback.onWebRtcAudioRecordStartError(errorCode, errorMessage);
		}
	}

	private void reportWebRtcAudioRecordError(String errorMessage) {
		Logging.e(TAG, "Run-time recording error: " + errorMessage);
		//WebRtcAudioUtils.logAudioState(TAG, context, audioManager);
		if (errorCallback != null) {
			errorCallback.onWebRtcAudioRecordError(errorMessage);
		}
	}

	private void doAudioRecordStateCallback(int audioState) {
		Logging.d(TAG, "doAudioRecordStateCallback: " + audioStateToString(audioState));
		if (stateCallback != null) {
			if (audioState == WebRtcAudioRecord.AUDIO_RECORD_START) {
				stateCallback.onWebRtcAudioRecordStart();
			} else if (audioState == WebRtcAudioRecord.AUDIO_RECORD_STOP) {
				stateCallback.onWebRtcAudioRecordStop();
			} else {
				Logging.e(TAG, "Invalid audio state");
			}
		}
	}

	// Reference from Android code, AudioFormat.getBytesPerSample. BitPerSample / 8
	// Default audio data format is PCM 16 bits per sample.
	// Guaranteed to be supported by all devices
	/*
  private static int getBytesPerSample(int audioFormat) {
    switch (audioFormat) {
      case AudioFormat.ENCODING_PCM_8BIT:
        return 1;
      case AudioFormat.ENCODING_PCM_16BIT:
      case AudioFormat.ENCODING_IEC61937:
      case AudioFormat.ENCODING_DEFAULT:
        return 2;
      case AudioFormat.ENCODING_PCM_FLOAT:
        return 4;
      case AudioFormat.ENCODING_INVALID:
      default:
        throw new IllegalArgumentException("Bad audio format " + audioFormat);
    }
  }
	 */


	// Use an ExecutorService to schedule a task after a given delay where the task consists of
	// checking (by logging) the current status of active recording sessions.
	//  private void scheduleLogRecordingConfigurationsTask(AudioRecord audioRecord) {
	//    Logging.d(TAG, "scheduleLogRecordingConfigurationsTask");
	//    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
	//      return;
	//    }

	//    Callable<String> callable = () -> {
	//      if (this.audioRecord == audioRecord) {
	//        logRecordingConfigurations(audioRecord, true /* verifyAudioConfig */);
	//      } else {
	//        Logging.d(TAG, "audio record has changed");
	//      }
	//      return "Scheduled task is done";
	//    };

	//    if (future != null && !future.isDone()) {
	//      future.cancel(true /* mayInterruptIfRunning */);
	//    }
	// Schedule call to logRecordingConfigurations() from executor thread after fixed delay.
	//    future = executor.schedule(callable, CHECK_REC_STATUS_DELAY_MS, TimeUnit.MILLISECONDS);
	//  };

	//  @TargetApi(Build.VERSION_CODES.N)
	//  private static boolean logActiveRecordingConfigs(
	//      int session, List<AudioRecordingConfiguration> configs) {
	//    assertTrue(!configs.isEmpty());
	//    final Iterator<AudioRecordingConfiguration> it = configs.iterator();
	//    Logging.d(TAG, "AudioRecordingConfigurations: ");
	//    while (it.hasNext()) {
	//      final AudioRecordingConfiguration config = it.next();
	//     StringBuilder conf = new StringBuilder();
	// The audio source selected by the client.
	//      final int audioSource = config.getClientAudioSource();
	//      conf.append("  client audio source=")
	//         .append(WebRtcAudioUtils.audioSourceToString(audioSource))
	//          .append(", client session id=")
	//          .append(config.getClientAudioSessionId())
	// Compare with our own id (based on AudioRecord#getAudioSessionId()).
	//          .append(" (")
	//          .append(session)
	//          .append(")")
	//          .append("\n");
	// Audio format at which audio is recorded on this Android device. Note that it may differ
	// from the client application recording format (see getClientFormat()).
	//      AudioFormat format = config.getFormat();
	//      conf.append("  Device AudioFormat: ")
	//         .append("channel count=")
	//          .append(format.getChannelCount())
	//          .append(", channel index mask=")
	//          .append(format.getChannelIndexMask())
	// Only AudioFormat#CHANNEL_IN_MONO is guaranteed to work on all devices.
	//          .append(", channel mask=")
	//          .append(WebRtcAudioUtils.channelMaskToString(format.getChannelMask()))
	//          .append(", encoding=")
	//          .append(WebRtcAudioUtils.audioEncodingToString(format.getEncoding()))
	//          .append(", sample rate=")
	//          .append(format.getSampleRate())
	//          .append("\n");
	// Audio format at which the client application is recording audio.
	//      format = config.getClientFormat();
	//      conf.append("  Client AudioFormat: ")
	//          .append("channel count=")
	//          .append(format.getChannelCount())
	//          .append(", channel index mask=")
	//          .append(format.getChannelIndexMask())
	// Only AudioFormat#CHANNEL_IN_MONO is guaranteed to work on all devices.
	//          .append(", channel mask=")
	//          .append(WebRtcAudioUtils.channelMaskToString(format.getChannelMask()))
	//          .append(", encoding=")
	//          .append(WebRtcAudioUtils.audioEncodingToString(format.getEncoding()))
	//          .append(", sample rate=")
	//          .append(format.getSampleRate())
	//          .append("\n");
	// Audio input device used for this recording session.
	//      final AudioDeviceInfo device = config.getAudioDevice();
	//      if (device != null) {
	//        assertTrue(device.isSource());
	//        conf.append("  AudioDevice: ")
	//           .append("type=")
	//            .append(WebRtcAudioUtils.deviceTypeToString(device.getType()))
	//            .append(", id=")
	//            .append(device.getId());
	//      }
	//      Logging.d(TAG, conf.toString());
	//    }
	//    return true;
	//  }

	// Verify that the client audio configuration (device and format) matches the requested
	// configuration (same as AudioRecord's).
	//  @TargetApi(Build.VERSION_CODES.N)
	//  private static boolean verifyAudioConfig(int source, int session, AudioFormat format,
	//      AudioDeviceInfo device, List<AudioRecordingConfiguration> configs) {
	//    assertTrue(!configs.isEmpty());
	//    final Iterator<AudioRecordingConfiguration> it = configs.iterator();
	//    while (it.hasNext()) {
	//      final AudioRecordingConfiguration config = it.next();
	//      final AudioDeviceInfo configDevice = config.getAudioDevice();
	//      if (configDevice == null) {
	//        continue;
	//      }
	//      if ((config.getClientAudioSource() == source)
	//          && (config.getClientAudioSessionId() == session)
	// Check the client format (should match the format of the AudioRecord instance).
	//          && (config.getClientFormat().getEncoding() == format.getEncoding())
	//          && (config.getClientFormat().getSampleRate() == format.getSampleRate())
	//          && (config.getClientFormat().getChannelMask() == format.getChannelMask())
	//         && (config.getClientFormat().getChannelIndexMask() == format.getChannelIndexMask())
	// Ensure that the device format is properly configured.
	//          && (config.getFormat().getEncoding() != AudioFormat.ENCODING_INVALID)
	//          && (config.getFormat().getSampleRate() > 0)
	//  For the channel mask, either the position or index-based value must be valid.
	//          && ((config.getFormat().getChannelMask() != AudioFormat.CHANNEL_INVALID)
	//              || (config.getFormat().getChannelIndexMask() != AudioFormat.CHANNEL_INVALID))
	//          && checkDeviceMatch(configDevice, device)) {
	//        Logging.d(TAG, "verifyAudioConfig: PASS");
	//        return true;
	//      }
	//    }
	//    Logging.e(TAG, "verifyAudioConfig: FAILED");
	//    return false;
	//  }

	//  @TargetApi(Build.VERSION_CODES.N)
	// Returns true if device A parameters matches those of device B.
	// TODO(henrika): can be improved by adding AudioDeviceInfo#getAddress() but it requires API 29.
	//  private static boolean checkDeviceMatch(AudioDeviceInfo devA, AudioDeviceInfo devB) {
	//    return ((devA.getId() == devB.getId() && (devA.getType() == devB.getType())));
	//  }

	private static String audioStateToString(int state) {
		switch (state) {
		case WebRtcAudioRecord.AUDIO_RECORD_START:
			return "START";
		case WebRtcAudioRecord.AUDIO_RECORD_STOP:
			return "STOP";
		default:
			return "INVALID";
		}
	}

	/**
	 * @param audio => 20ms of encoded audio data
	 */
	public void notifyEncodedData(String trackId, ByteBuffer audio) {
		ByteBuffer encodedByteBuffer = getEncodedByteBuffers().get(trackId);
		if(encodedByteBuffer == null) {
			encodedByteBuffer = ByteBuffer.allocateDirect(byteBuffer.capacity()*10);
			nativeCacheDirectBufferAddressForEncodedAudio(nativeAudioRecord, trackId, encodedByteBuffer);
			getEncodedByteBuffers().put(trackId, encodedByteBuffer);
		}

		if (audio.limit() <= encodedByteBuffer.capacity()) {
			encodedByteBuffer.clear();
			audio.rewind();
			encodedByteBuffer.put(audio);
			encodedDataIsReady(nativeAudioRecord, trackId, audio.limit());
		}
		else {
			logger.warn("Discarding audio packet because audio packet size({}) is bigger than buffer capacity{} and limit {}", audio.limit(), encodedByteBuffer.capacity(), encodedByteBuffer.limit());
		}
	}



	public Map<String, ByteBuffer> getEncodedByteBuffers() {
		return encodedByteBuffers;
	}
}
