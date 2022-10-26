package io.antmedia.plugin.api;

import org.bytedeco.ffmpeg.avcodec.AVCodecParameters;
import org.bytedeco.ffmpeg.avutil.AVRational;

public class StreamParametersInfo {
	public AVCodecParameters codecParameters;
	public AVRational timeBase;
	public boolean enabled;
	public boolean hostedInOtherNode = false; //set true if the stream is hosted by another node in cluster mode
}
