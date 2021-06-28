/*
 *  Copyright 2018 The WebRTC project authors. All Rights Reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

package org.webrtc;

import java.nio.ByteBuffer;

/**
 * Creates a native {@code webrtc::AudioDecoderFactory} with the builtin audio decoders.
 */
public class BuiltinAudioDecoderFactoryFactory implements AudioDecoderFactoryFactory {

	public interface AudioPacketListener {
		public void onAudioPacketData(ByteBuffer data, long timestamp);
	}

	private AudioPacketListener audioPacketListener;
	private boolean customDecoder = false;
	private long audioDecoderFactory = -1;
	private static final int BUFFER_LIMIT = 4096;
	ByteBuffer buffer = ByteBuffer.allocateDirect(BUFFER_LIMIT);

	@Override
	public long createNativeAudioDecoderFactory() {
		audioDecoderFactory = nativeCreateBuiltinAudioDecoderFactory(this, customDecoder, buffer);
		return audioDecoderFactory;
	}


	@CalledByNative 
	public synchronized void onAudioPacket(int size, long timestamp) {
		buffer.rewind();
		buffer.limit(size);
		
		
		ByteBuffer byteBuffer = ByteBuffer.allocateDirect(size);
		byteBuffer.put(buffer);
		byteBuffer.limit(size);
		byteBuffer.rewind();
		
		
		buffer.rewind();
		buffer.limit(BUFFER_LIMIT);
		audioPacketListener.onAudioPacketData(byteBuffer, timestamp);
	}

	public void setAudioPacketListener(AudioPacketListener audioPacketListener) {
		this.audioPacketListener = audioPacketListener;
	}

	private static native long nativeCreateBuiltinAudioDecoderFactory(BuiltinAudioDecoderFactoryFactory thisObj, boolean customDecoder, ByteBuffer byteBuffer);


	public void setCustomDecoder(boolean customDecoder) {
		this.customDecoder = customDecoder;
	}
}
