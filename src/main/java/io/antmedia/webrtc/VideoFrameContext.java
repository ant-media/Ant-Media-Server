package io.antmedia.webrtc;

import org.webrtc.VideoFrame;

public class VideoFrameContext {
	public final VideoFrame videoFrame;
	public final long timestampMS;

	public VideoFrameContext(VideoFrame videoFrame, long timestampMS) {
		this.videoFrame = videoFrame;
		this.timestampMS = timestampMS;
	}
}