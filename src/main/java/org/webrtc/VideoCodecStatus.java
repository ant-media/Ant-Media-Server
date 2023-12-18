/*
 *  Copyright 2017 The WebRTC project authors. All Rights Reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

package org.webrtc;

/**
 * Status codes reported by video encoding/decoding components. This should be kept in sync with
 * video_error_codes.h.
 */
/*
 * #define WEBRTC_VIDEO_CODEC_TARGET_BITRATE_OVERSHOOT 5
#define WEBRTC_VIDEO_CODEC_OK_REQUEST_KEYFRAME 4
#define WEBRTC_VIDEO_CODEC_NO_OUTPUT 1
#define WEBRTC_VIDEO_CODEC_OK 0
#define WEBRTC_VIDEO_CODEC_ERROR -1
#define WEBRTC_VIDEO_CODEC_MEMORY -3
#define WEBRTC_VIDEO_CODEC_ERR_PARAMETER -4
#define WEBRTC_VIDEO_CODEC_TIMEOUT -6
#define WEBRTC_VIDEO_CODEC_UNINITIALIZED -7
#define WEBRTC_VIDEO_CODEC_FALLBACK_SOFTWARE -13
#define WEBRTC_VIDEO_CODEC_ERR_SIMULCAST_PARAMETERS_NOT_SUPPORTED -15
#define WEBRTC_VIDEO_CODEC_ENCODER_FAILURE -16
 */
public enum VideoCodecStatus {
  REQUEST_KEY_FRAME(4),
  TARGET_BITRATE_OVERSHOOT(5),
  REQUEST_SLI(2),
  NO_OUTPUT(1),
  OK(0),
  ERROR(-1),
  LEVEL_EXCEEDED(-2),
  MEMORY(-3),
  ERR_PARAMETER(-4),
  ERR_SIZE(-5),
  TIMEOUT(-6),
  UNINITIALIZED(-7),
  ERR_REQUEST_SLI(-12),
  FALLBACK_SOFTWARE(-13);

  private final int number;

  private VideoCodecStatus(int number) {
    this.number = number;
  }

  @CalledByNative
  public int getNumber() {
    return number;
  }
}
