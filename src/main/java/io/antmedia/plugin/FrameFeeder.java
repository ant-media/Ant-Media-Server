package io.antmedia.plugin;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.bytedeco.ffmpeg.avutil.AVFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.antmedia.plugin.api.IFrameListener;

public class FrameFeeder {

	private String streamId;
	
	private Queue<IFrameListener> listeners = new ConcurrentLinkedQueue<>();
	private static final Logger logger = LoggerFactory.getLogger(FrameFeeder.class);


	public FrameFeeder(String streamId) {
		this.streamId = streamId;
	}

	public AVFrame onVideoFrame(AVFrame frame) {
		AVFrame processedFrame = frame;
		for (IFrameListener iFrameListener : listeners) {
			processedFrame = iFrameListener.onVideoFrame(streamId, processedFrame);
			if(processedFrame == null) {
				break;
			}
		}

		return processedFrame;
	}
	
	public AVFrame onAudioFrame(AVFrame frame) {
		AVFrame processedFrame = frame;
		for (IFrameListener iFrameListener : listeners) {
			processedFrame = iFrameListener.onAudioFrame(streamId, processedFrame);
			if(processedFrame == null) {
				break;
			}
		}

		return processedFrame;
	}

	public void addListener(IFrameListener listener) {
		listeners.add(listener);
	}
	
	public void writeTrailer() {
		for (IFrameListener iFrameListener : listeners) {
			iFrameListener.writeTrailer(streamId);
		}
	}

	public void removeFrameListener(IFrameListener listener) {
		listeners.remove(listener);		
	}


}
