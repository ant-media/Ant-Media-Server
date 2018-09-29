/*
 *  Copyright (c) 2013 The WebRTC project authors. All Rights Reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

package org.webrtc;

import javax.annotation.Nullable;

/**
 * Implements VideoCapturer.CapturerObserver and feeds frames to
 * webrtc::jni::AndroidVideoTrackSource.
 */
class NativeCapturerObserver implements CapturerObserver {
  // Pointer to webrtc::jni::AndroidVideoTrackSource.
  private final long nativeSource;
  // TODO(bugs.webrtc.org/9181): Remove.
  //@Nullable private final SurfaceTextureHelper surfaceTextureHelper;

  @CalledByNative
  public NativeCapturerObserver(long nativeSource) {
    this.nativeSource = nativeSource;
    //this.surfaceTextureHelper = null;
  }

  // TODO(bugs.webrtc.org/9181): Remove.
  public NativeCapturerObserver(long nativeSource, Object surfaceTextureHelper) {
    this.nativeSource = nativeSource;
  //  this.surfaceTextureHelper = surfaceTextureHelper;
  }

  @Override
  public void onCapturerStarted(boolean success) {
    nativeCapturerStarted(nativeSource, success);
  }

  @Override
  public void onCapturerStopped() {
    nativeCapturerStopped(nativeSource);
  }

  @Override
  public void onFrameCaptured(VideoFrame frame) {
    nativeOnFrameCaptured(nativeSource, frame.getBuffer().getWidth(), frame.getBuffer().getHeight(),
        frame.getRotation(), frame.getTimestampNs(), frame.getBuffer());
  }

  public void dispose() {
   // if (surfaceTextureHelper != null) {
   //   surfaceTextureHelper.dispose();
   // }
  }

  private static native void nativeCapturerStarted(long source, boolean success);
  private static native void nativeCapturerStopped(long source);
  private static native void nativeOnFrameCaptured(
      long source, int width, int height, int rotation, long timestampNs, VideoFrame.Buffer frame);
}
