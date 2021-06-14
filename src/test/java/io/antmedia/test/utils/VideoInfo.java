package io.antmedia.test.utils;

import java.io.File;

public class VideoInfo{
	public String path;
	public boolean isExist;
	public String videoWidth;
	public String videoHeight;
	public String videoCodec;
	public String videoFps;
	public String videoDuration;
	public String videoBitrate;
	public String audioCodec;
	public String audioBitrate;
	public String audioDuration;
	public long videoDurationMS;
	public long audioDurationMS;
	public int videoPacketsCount;
	public int audioPacketsCount;
	public long audioStartTimeMs;
	
	@Override
	public String toString() {
		return "file path: " + path + " video width: "  + videoWidth + " video height: " + videoHeight
				+ " video codec: " + videoCodec + " video fps: " + videoFps + " video duration: " + videoDuration
				+ " video bitrate: " + videoBitrate + " audio codec: " + audioCodec + " audio bitrate: " + audioBitrate
				+ " audio start time ms: " + audioStartTimeMs;
	}
	

}