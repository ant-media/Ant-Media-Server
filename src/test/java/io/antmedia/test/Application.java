package io.antmedia.test;

import java.io.File;

import org.red5.server.adapter.MultiThreadedApplicationAdapter;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.muxer.IAntMediaStreamHandler;
import io.antmedia.muxer.MuxAdaptor;

public class Application extends MultiThreadedApplicationAdapter implements IAntMediaStreamHandler {

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
	public static String notifyVodId = null;
	
	AntMediaApplicationAdapter adaptor = new AntMediaApplicationAdapter();

	@Override
	public void muxingFinished(String id, File file, long duration, int resolution) {
		adaptor.muxingFinished(id, file, duration, resolution);
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
			String vodName, String vodId) {
		notifyHookAction = action;
		notitfyURL = url;
		notifyId = id;
		notifyStreamName = streamName;
		notifyCategory = category;
		notifyVodName = vodName;
		notifyVodId  = vodId;

		return null;
	}

	@Override
	public void setQualityParameters(String id, String quality, double speed, int pendingPacketSize) {
		if (enableSourceHealthUpdate) {
			adaptor.setQualityParameters(id, quality, speed, pendingPacketSize);
		}
	}

	@Override
	public void muxAdaptorAdded(MuxAdaptor muxAdaptor) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void muxAdaptorRemoved(MuxAdaptor muxAdaptor) {
		// TODO Auto-generated method stub
		
	}

}
