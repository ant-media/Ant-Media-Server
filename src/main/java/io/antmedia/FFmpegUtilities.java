package io.antmedia;

import static org.bytedeco.ffmpeg.global.avutil.avutil_configuration;

import org.bytedeco.javacpp.BytePointer;


public class FFmpegUtilities {
	
	private static String buildConfiguration;

	public static String getBuildConfiguration() {
		if(buildConfiguration == null) {
			BytePointer conf = avutil_configuration();
			buildConfiguration = conf.getString();
		}
		return buildConfiguration;
	}
}
