/*
 *  Copyright 2013 The WebRTC project authors. All Rights Reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

package org.webrtc;

import java.util.LinkedList;


/** Java wrapper for a C++ AudioTrackInterface */
public class AudioTrack extends MediaStreamTrack {


	private final LinkedList<AudioSink> audioSinks = new LinkedList<AudioSink>();

	public AudioTrack(long nativeTrack) {
		super(nativeTrack);
	}

	public void addSink(AudioSink renderer) {
		audioSinks.add(renderer);
		nativeAddSink(nativeTrack, renderer.nativeAudioSink);
	}

	public void removeSink(AudioSink renderer) {
		if (!audioSinks.remove(renderer)) {
			return;
		}
		nativeRemoveSink(nativeTrack, renderer.nativeAudioSink);
		renderer.dispose();
	}

	public void dispose() {
		while (!audioSinks.isEmpty()) {
			removeSink(audioSinks.getFirst());
		}
		super.dispose();
	}

	private static native void free(long nativeTrack);

	private static native void nativeAddSink(long nativeTrack, long nativeRenderer);

	private static native void nativeRemoveSink(long nativeTrack, long nativeRenderer);

}
