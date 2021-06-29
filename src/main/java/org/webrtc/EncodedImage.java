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

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

/**
 * An encoded frame from a video stream. Used as an input for decoders and as an output for
 * encoders.
 */
public class EncodedImage implements RefCounted {
  // Must be kept in sync with common_types.h FrameType.
  public enum FrameType {
    EmptyFrame(0),
    VideoFrameKey(3),
    VideoFrameDelta(4);

    private final int nativeIndex;

    private FrameType(int nativeIndex) {
      this.nativeIndex = nativeIndex;
    }

    public int getNative() {
      return nativeIndex;
    }

    @CalledByNative("FrameType")
    static FrameType fromNativeIndex(int nativeIndex) {
      for (FrameType type : FrameType.values()) {
        if (type.getNative() == nativeIndex) {
          return type;
        }
      }
      throw new IllegalArgumentException("Unknown native frame type: " + nativeIndex);
    }
  }

  private final RefCountDelegate refCountDelegate;
  private final boolean supportsRetain;
  public final ByteBuffer buffer;
  public final int encodedWidth;
  public final int encodedHeight;
  public long captureTimeMs; // Deprecated
  public long captureTimeNs;
  public final FrameType frameType;
  public final int rotation;
  public final boolean completeFrame;
  public final @Nullable Integer qp;
  public final List<NaluIndex> naluIndices;

  // TODO(bugs.webrtc.org/9378): Use retain and release from jni code.
  @Override
  public void retain() {
    refCountDelegate.retain();
  }

  @Override
  public void release() {
    refCountDelegate.release();
  }

  // A false return value means that the encoder expects that the buffer is no longer used after
  // VideoEncoder.Callback.onEncodedFrame returns.
  @CalledByNative
  boolean maybeRetain() {
    if (supportsRetain) {
      retain();
      return true;
    } else {
      return false;
    }
  }

  @CalledByNative
  private EncodedImage(ByteBuffer buffer, boolean supportsRetain,
	      @Nullable Runnable releaseCallback, int encodedWidth, int encodedHeight, long captureTimeNs,
	      FrameType frameType, int rotation, boolean completeFrame, @Nullable Integer qp) {
	  this(buffer, supportsRetain, releaseCallback, encodedWidth, encodedHeight, captureTimeNs, frameType, rotation, completeFrame, qp, null);
  }
  
  private EncodedImage(ByteBuffer buffer, boolean supportsRetain,
      @Nullable Runnable releaseCallback, int encodedWidth, int encodedHeight, long captureTimeNs,
      FrameType frameType, int rotation, boolean completeFrame, @Nullable Integer qp, List<NaluIndex> naluIndices) {
    this.buffer = buffer;
    this.encodedWidth = encodedWidth;
    this.encodedHeight = encodedHeight;
    this.captureTimeMs = TimeUnit.NANOSECONDS.toMillis(captureTimeNs);
    this.captureTimeNs = captureTimeNs;
    this.frameType = frameType;
    this.rotation = rotation;
    this.completeFrame = completeFrame;
    this.qp = qp;
    this.supportsRetain = supportsRetain;
    this.refCountDelegate = new RefCountDelegate(releaseCallback);
    this.naluIndices = naluIndices;
  }

  @CalledByNative
  private ByteBuffer getBuffer() {
    return buffer;
  }

  @CalledByNative
  private int getEncodedWidth() {
    return encodedWidth;
  }

  @CalledByNative
  private int getEncodedHeight() {
    return encodedHeight;
  }

  @CalledByNative
  private long getCaptureTimeNs() {
    return captureTimeNs;
  }

  @CalledByNative
  private int getFrameType() {
    return frameType.getNative();
  }

  @CalledByNative
  private int getRotation() {
    return rotation;
  }

  @CalledByNative
  private boolean getCompleteFrame() {
    return completeFrame;
  }

  @CalledByNative
  private @Nullable Integer getQp() {
    return qp;
  }
  
  public void setCaptureTimeNs(long captureTimeNs) {
	  this.captureTimeNs = captureTimeNs;
	  this.captureTimeMs = TimeUnit.NANOSECONDS.toMillis(captureTimeNs);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private ByteBuffer buffer;
    private boolean supportsRetain;
    private @Nullable Runnable releaseCallback;
    private int encodedWidth;
    private int encodedHeight;
    private long captureTimeNs;
    private EncodedImage.FrameType frameType;
    private int rotation;
    private boolean completeFrame;
    private @Nullable Integer qp;
	private List<NaluIndex> naluIndices;

    private Builder() {}

    @Deprecated
    public Builder setBuffer(ByteBuffer buffer) {
      this.buffer = buffer;
      this.releaseCallback = null;
      this.supportsRetain = false;
      return this;
    }

    public Builder setBuffer(ByteBuffer buffer, @Nullable Runnable releaseCallback) {
      this.buffer = buffer;
      this.releaseCallback = releaseCallback;
      this.supportsRetain = true;
      return this;
    }

    public Builder setEncodedWidth(int encodedWidth) {
      this.encodedWidth = encodedWidth;
      return this;
    }

    public Builder setEncodedHeight(int encodedHeight) {
      this.encodedHeight = encodedHeight;
      return this;
    }

    @Deprecated
    public Builder setCaptureTimeMs(long captureTimeMs) {
      this.captureTimeNs = TimeUnit.MILLISECONDS.toNanos(captureTimeMs);
      return this;
    }

    public Builder setCaptureTimeNs(long captureTimeNs) {
      this.captureTimeNs = captureTimeNs;
      return this;
    }

    public Builder setFrameType(EncodedImage.FrameType frameType) {
      this.frameType = frameType;
      return this;
    }

    public Builder setRotation(int rotation) {
      this.rotation = rotation;
      return this;
    }

    public Builder setCompleteFrame(boolean completeFrame) {
      this.completeFrame = completeFrame;
      return this;
    }

    public Builder setQp(@Nullable Integer qp) {
      this.qp = qp;
      return this;
    }

    public EncodedImage createEncodedImage() {
      return new EncodedImage(buffer, supportsRetain, releaseCallback, encodedWidth, encodedHeight,
          captureTimeNs, frameType, rotation, completeFrame, qp, naluIndices);
    }

	public Builder setNaluIndices(List<NaluIndex> naluIndices) {
		this.naluIndices = naluIndices;
		return this;
	}
	
	
  }
}
