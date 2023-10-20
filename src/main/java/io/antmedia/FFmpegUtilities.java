package io.antmedia;

import static org.bytedeco.ffmpeg.global.avutil.avutil_configuration;

import org.bytedeco.javacpp.BytePointer;

import java.nio.charset.StandardCharsets;


public class FFmpegUtilities {
	
	private static String buildConfiguration;

	public static String getBuildConfiguration() {
		if(buildConfiguration == null) {
			BytePointer conf = avutil_configuration();
			buildConfiguration = conf.getString();
		}
		return buildConfiguration;
	}

	/**
	 * Turns a C-style null terminated string in a byte array as
	 * a String object, assuming UTF8 encoding of the original data
	 *
	 * @param nullTerminatedChars byte buffer that contains a null terminated string
	 * @return the trimmed string
	 */
	public static String byteArrayToString(byte[] nullTerminatedChars) {
		if(nullTerminatedChars == null) {
			return "";
		}
		int termin = 0;
		while(termin < nullTerminatedChars.length && nullTerminatedChars[termin] != 0) {
			termin++;
		}
		return new String(nullTerminatedChars, 0, termin, StandardCharsets.UTF_8);
	}
}
