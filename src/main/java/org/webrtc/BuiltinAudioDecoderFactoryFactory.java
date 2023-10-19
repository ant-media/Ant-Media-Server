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

/*
 *  Copyright 2018 The WebRTC project authors. All Rights Reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

/**
 * Creates a native {@code webrtc::AudioDecoderFactory} with the builtin audio decoders.
 */
public class BuiltinAudioDecoderFactoryFactory implements AudioDecoderFactoryFactory {
	//  @Override
	//  public long createNativeAudioDecoderFactory() {
	//    return nativeCreateBuiltinAudioDecoderFactory();
	//  }

	//private static native long nativeCreateBuiltinAudioDecoderFactory();

	public interface AudioPacketListener {
		//audio level is between 0 and 127. 0 is the max and 127 is the lowest
		public void onAudioPacketData(ByteBuffer data, long timestamp, int audioLevel, boolean voiceActivity, boolean hasAudioLevel);
	}

	private AudioPacketListener audioPacketListener;
	private boolean customDecoder = false;
	private long audioDecoderFactory = -1;
	ByteBuffer buffer = ByteBuffer.allocateDirect(4096);

	@Override
	public long createNativeAudioDecoderFactory() {
		audioDecoderFactory = nativeCreateBuiltinAudioDecoderFactory(this, customDecoder, buffer);
		return audioDecoderFactory;
	}


	@CalledByNative 
	void onAudioPacket(int size, long timestamp, int audioLevel, boolean voiceActivity, boolean hasAudioLevel) {
		byte data[] = new byte[size];
		buffer.rewind();
		buffer.get(data, 0, size);
		audioPacketListener.onAudioPacketData(ByteBuffer.wrap(data), timestamp, audioLevel, voiceActivity, hasAudioLevel);
	}

	public void setAudioPacketListener(AudioPacketListener audioPacketListener) {
		this.audioPacketListener = audioPacketListener;
	}
	
	public AudioPacketListener getAudioPacketListener() {
		return audioPacketListener;
	}

	private static native long nativeCreateBuiltinAudioDecoderFactory(BuiltinAudioDecoderFactoryFactory thisObj, boolean customDecoder, ByteBuffer byteBuffer);


	public void setCustomDecoder(boolean customDecoder) {
		this.customDecoder = customDecoder;
	}

}
