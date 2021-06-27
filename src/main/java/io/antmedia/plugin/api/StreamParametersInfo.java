package io.antmedia.plugin.api;

import org.bytedeco.ffmpeg.avcodec.AVCodecParameters;
import org.bytedeco.ffmpeg.avutil.AVRational;

public class StreamParametersInfo {
	public AVCodecParameters codecParameters;
	public AVRational timeBase; 
}
