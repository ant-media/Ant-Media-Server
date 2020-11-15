package io.antmedia.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import io.antmedia.AppSettings;

public class StreamAcceptFilter implements ApplicationContextAware{

	private AppSettings appSettings;

	private String streamId;

	protected static Logger logger = LoggerFactory.getLogger(StreamAcceptFilter.class);
	
	public boolean isValidStreamParameters(int width, int height, int fps, int bitrate, String streamId) 
	{
		this.streamId = streamId;
		return  checkFPSAccept(fps) && 
				checkResolutionAccept(width, height) &&
				checkBitrateAccept(bitrate);
	}

	public boolean checkFPSAccept(int fps) 
	{
		if(fps != 0 && getMaxFps() > 0 && getMaxFps()  < fps) {
			logger.error("Exceeding Max FPS({}) limit. FPS is: {} streamId: {}", getMaxFps(), fps, streamId);
			return false;
		}		
		return true;
	} 

	public boolean checkResolutionAccept(int width, int height) 
	{
		if (height != 0 && getMaxResolution() > 0 && getMaxResolution() < height) {
			logger.error("Exceeding Max Resolution({}) acceptable limit. Resolution is: {} streamId:{}", getMaxResolution(), height, streamId);
			return false;
		}
		return true;

	} 

	public boolean checkBitrateAccept(long streamBitrateValue) 
	{
		if (streamBitrateValue != 0 && getMaxBitrate() > 0 && getMaxBitrate() < streamBitrateValue) {
			logger.error("Exceeding Max Bitrate({}) acceptable limit. Stream Bitrate is: {} streamId:{}", getMaxBitrate(), streamBitrateValue, streamId);
			return false;
		}
		return true;
	} 

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		if (applicationContext.containsBean(AppSettings.BEAN_NAME)) {
			appSettings = (AppSettings)applicationContext.getBean(AppSettings.BEAN_NAME);
		}
	}
	

	public AppSettings getAppSettings() {
		return appSettings;
	}
	
	public int getMaxFps() {
		//It's disabled for now because we don't directly get fps from stream in new rtmp ingesting method
		return 0;
	}
	
	public int getMaxResolution() {
		if (appSettings != null) {
			return appSettings.getMaxResolutionAccept();
		}
		return 0;
	}

	public int getMaxBitrate() {
		//It's disabled for now because we don't directly get bitrate from stream in new rtmp ingesting method
		return 0;
	}
	
}
