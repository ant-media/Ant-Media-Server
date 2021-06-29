package io.antmedia.test.utils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;

import io.antmedia.integration.AppFunctionalV2Test;


public class VideoProber  {
	public static  final String WIDTH = "width";
	public static  final String HEIGHT = "height";
	public static  final String BITRATE = "bitrate";
	public static  final String CODEC = "codec_name";
	public static  final String DURATION = "duration";
	public static  final String FPS = "r_frame_rate";

	public static  final char VIDEO = 'v';
	public static  final char AUDIO = 'a';


	public static VideoInfo getFileInfo(String path) {
		VideoInfo info = new VideoInfo();
		info.isExist = new File(path).exists();
		info.path = path;
		info.videoCodec = getProperty(path, VIDEO, CODEC);
		info.videoWidth = getProperty(path, VIDEO, WIDTH);
		info.videoHeight = getProperty(path, VIDEO, HEIGHT);
		info.videoFps = getProperty(path, VIDEO, FPS);
		info.videoBitrate = getProperty(path, VIDEO, BITRATE);
		info.videoDuration = run(AppFunctionalV2Test.ffprobePath + " "+path+" -show_streams -select_streams v 2>&1 | sed -n 's/TAG:DURATION=//p'").trim();
		info.audioDuration = run(AppFunctionalV2Test.ffprobePath + " "+path+" -show_streams -select_streams a 2>&1 | sed -n 's/TAG:DURATION=//p'").trim();
		String videoPacketCountCommand = AppFunctionalV2Test.ffprobePath + " "+path+" -show_packets 2>&1 | grep codec_type=video | wc -l";
		String videoPacketCount = run(videoPacketCountCommand).trim();
		System.out.println("video packet count: " + videoPacketCountCommand);
		info.videoPacketsCount = Integer.parseInt(videoPacketCount);
		info.audioPacketsCount = Integer.parseInt(run(AppFunctionalV2Test.ffprobePath + " "+path+" -show_packets 2>&1 | grep codec_type=audio | wc -l").trim());
		try {
			info.audioStartTimeMs = (long)(Float.parseFloat(run(AppFunctionalV2Test.ffprobePath + " "+path+" -show_streams -select_streams a 2>&1 | sed -n 's/start_time=//p'").trim()) * 1000);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		info.audioCodec = getProperty(path, AUDIO, CODEC);
		info.audioBitrate = getProperty(path, AUDIO, BITRATE);
		info.videoDurationMS = toMsDuration(info.videoDuration);
		info.audioDurationMS = toMsDuration(info.audioDuration.replace(",", "."));
		return info;
	}

	private static long toMsDuration(String strDuration) {
		if(strDuration.isEmpty()) {
			return 0;
		}
		String[] tokens = strDuration.split(":");
		long hours = Long.parseLong(tokens[0])*3600*1000;
		long minutes = Long.parseLong(tokens[1])*60*1000;
		long seconds = (long) (Double.parseDouble(tokens[2])*1000);

		return hours+minutes+seconds;
	}

	public static String getProperty(String path, char type, String property) {
		String command = AppFunctionalV2Test.ffprobePath + " -v error -select_streams "+type+":0 -show_entries stream="+property+" -of csv=s=x:p=0 "+path;
		return run(command).trim();
	}

	public static String run(String command) {
		ProcessBuilder pb = new ProcessBuilder("/bin/sh", "-c", command);
		try {
			return IOUtils.toString(pb.start().getInputStream(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			e.printStackTrace();
			return e.getMessage();
		}
	}
	
	
	
}