package io.antmedia.test;

import java.io.File;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.muxer.IAntMediaStreamHandler;

public class Application extends AntMediaApplicationAdapter implements IAntMediaStreamHandler {

	public static String id = null;
	public static File file = null;
	public static long duration = 0;

	public static String notifyHookAction = null;
	public static String notitfyURL = null;
	public static String notifyId = null;
	public static String notifyStreamName = null;
	public static String notifyCategory = null;
	public static String notifyVodName = null;

	public static boolean enableSourceHealthUpdate = false;

	@Override
	public void muxingFinished(String id, File file, long duration, int resolution) {
		super.muxingFinished(id, file, duration, resolution);
		Application.id = id;
		Application.file = file;
		Application.duration = duration;
	}

	public static void resetFields() {
		Application.id = null;
		Application.file = null;
		Application.duration = 0;
		notifyHookAction = null;
		notitfyURL = null;
		notifyId = null;
		notifyStreamName = null;
		notifyCategory = null;
		notifyVodName = null;

	}

	public StringBuilder notifyHook(String url, String id, String action, String streamName, String category,
			String vodName) {
		notifyHookAction = action;
		notitfyURL = url;
		notifyId = id;
		notifyStreamName = streamName;
		notifyCategory = category;
		notifyVodName = vodName;

		return null;
	}

	@Override
	public void setQualityParameters(String id, String quality, double speed, int pendingPacketSize) {
		if (enableSourceHealthUpdate) {
			super.setQualityParameters(id, quality, speed, pendingPacketSize);
		}
	}

}
