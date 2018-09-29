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

import org.webrtc.CalledByNative;

/**
 * This class contains static functions to query sample rate and input/output audio buffer sizes.
 */
class WebRtcAudioManager {
  private static final String TAG = "WebRtcAudioManagerExternal";

  private static final int DEFAULT_SAMPLE_RATE_HZ = 16000;

  // Default audio data format is PCM 16 bit per sample.
  // Guaranteed to be supported by all devices.
  private static final int BITS_PER_SAMPLE = 16;

  private static final int DEFAULT_FRAME_PER_BUFFER = 256;

  @CalledByNative
  static Object getAudioManager(Object context) {
    //return (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
	  return null;
  }

  @CalledByNative
  static int getOutputBufferSize(
      Object context, Object audioManager, int sampleRate, int numberOfOutputChannels) {
        return DEFAULT_FRAME_PER_BUFFER;
  }

  @CalledByNative
  static int getInputBufferSize(
      Object context, Object audioManager, int sampleRate, int numberOfInputChannels) {
    return DEFAULT_FRAME_PER_BUFFER;
  }

  /**
   * Returns the native input/output sample rate for this device's output stream.
   */
  @CalledByNative
  static int getSampleRate(Object audioManager) {
    System.out.println("Sample rate is set to " + DEFAULT_SAMPLE_RATE_HZ + " Hz");
    return DEFAULT_SAMPLE_RATE_HZ;
  }

}
