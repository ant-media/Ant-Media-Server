package io.antmedia.filter;

import org.bytedeco.javacpp.avcodec.AVPacket;
import org.bytedeco.javacpp.avformat.AVFormatContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import io.antmedia.AppSettings;

public class StreamAcceptFilter implements ApplicationContextAware{

	private AppSettings appSettings;

	protected static Logger logger = LoggerFactory.getLogger(StreamAcceptFilter.class);
	
	private int maxFps = 0;
	private int maxResolution = 0;
	private int maxBitrate = 0;

	public boolean isValidStreamParameters(AVFormatContext inputFormatContext,AVPacket pkt) 
	{
		// Check FPS value
		return  checkFPSAccept(getStreamFps(inputFormatContext, pkt)) && 
				checkResolutionAccept(getStreamResolution(inputFormatContext, pkt)) &&
				checkBitrateAccept(getStreamBitrate(inputFormatContext, pkt));
	}

	public boolean checkFPSAccept(int streamFPSValue) 
	{
		if(maxFps > 0 && maxFps < streamFPSValue) {
			logger.error("Exceeding Max FPS({}) limit. FPS is: {}", maxFps, streamFPSValue);
			return false;
		}		
		return true;
	} 

	public boolean checkResolutionAccept(int streamResolutionValue) 
	{
		if (maxResolution > 0 && maxResolution < streamResolutionValue) {
			logger.error("Exceeding Max Resolution({}) acceptable limit. Resolution is: {}", maxResolution, streamResolutionValue);
			return false;
		}
		return true;

	} 

	public boolean checkBitrateAccept(long streamBitrateValue) 
	{
		if (maxBitrate > 0 && maxBitrate < streamBitrateValue) {
			logger.error("Exceeding Max Bitrate({}) acceptable limit. Stream Bitrate is: {}", maxBitrate, streamBitrateValue);
			return false;
		}
		return true;
	} 

	public int getStreamFps(AVFormatContext inputFormatContext,AVPacket pkt) 
	{
		int streamFPSValue = (inputFormatContext.streams(pkt.stream_index()).r_frame_rate().num()) / (inputFormatContext.streams(pkt.stream_index()).r_frame_rate().den());
		logger.info("Stream FPS value: {}",streamFPSValue);

		return streamFPSValue;
	}

	public int getStreamResolution(AVFormatContext inputFormatContext,AVPacket pkt) {
		int streamResolutionValue = inputFormatContext.streams(pkt.stream_index()).codecpar().height();
		logger.error("Stream Resolution value: {}",streamResolutionValue);

		return streamResolutionValue;
	}

	public long getStreamBitrate(AVFormatContext inputFormatContext,AVPacket pkt) {
		long streamBitrateValue = inputFormatContext.streams(pkt.stream_index()).codecpar().bit_rate();
		logger.error("Stream Bitrate value: {}",streamBitrateValue);

		return streamBitrateValue;		
	}


	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		if (applicationContext.containsBean(AppSettings.BEAN_NAME)) {
			appSettings = (AppSettings)applicationContext.getBean(AppSettings.BEAN_NAME);
			maxFps = appSettings.getMaxFpsAccept();
			maxResolution = appSettings.getMaxResolutionAccept();
			maxBitrate = appSettings.getMaxBitrateAccept();
			
		}
	}
	
	public void setMaxFps(int maxFps) {
		this.maxFps = maxFps;
	}
	
	public void setMaxResolution(int maxResolution) {
		this.maxResolution = maxResolution;
	}
	
	public void setMaxBitrate(int maxBitrate) {
		this.maxBitrate = maxBitrate;
	}

	public AppSettings getAppSettings() {
		return appSettings;
	}
	
	public int getMaxFps() {
		return maxFps;
	}
	
	public int getMaxResolution() {
		return maxResolution;
	}

	public int getMaxBitrate() {
		return maxBitrate;
	}
}
