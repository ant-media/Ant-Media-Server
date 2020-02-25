package io.antmedia.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import io.antmedia.AppSettings;
import io.antmedia.muxer.IStreamAcceptFilter;

public class StreamAcceptFilter implements IStreamAcceptFilter{

	private ApplicationContext appContext;
	
	AppSettings appSettings;

	protected static Logger logger = LoggerFactory.getLogger(StreamAcceptFilter.class);

	private int result;

	/*
	 * If return -1, this mean everything is fine
	 * If return 0, this mean Stream FPS > Max FPS
	 * If return 1, this mean Resolution FPS > Max Resolution
	 * If return 2, this mean Bitrate FPS > Max Bitrate
	 */

	public int checkStreamParameters(String[] parameters) {

		// It's defined true
		result = -1;

		int streamFps = Integer.parseInt(parameters[0]);
		int streamResolution = Integer.parseInt(parameters[1]);
		int streamBitrate = Integer.parseInt(parameters[2]);

		// Check FPS value
		if(getAppSetting().getMaxFpsAccept() != null) {
			result = checkMaxFPSAccept(streamFps);
		}

		// Check Resolution value
		if(result == -1 && getAppSetting().getMaxResolutionAccept() != null) {
			result = checkResolutionAccept(streamResolution);
		}

		// Check bitrate value
		if(result == -1 && getAppSetting().getMaxBitrateAccept() != null) {
			result = checkMaxBitrateAccept(streamBitrate);
		}

		return result;
	}

	public int checkMaxFPSAccept(int streamFPSValue) {
		if(Integer.parseInt(getAppSetting().getMaxFpsAccept()) <= streamFPSValue) {
			result = 0;
		}
		else {
			result = -1;
		}
		return result;
	} 

	public int checkResolutionAccept(int streamResolutionValue) {
		if(Integer.parseInt(getAppSetting().getMaxResolutionAccept()) <= streamResolutionValue) {
			result = 1;
		}
		else {
			result = -1;
		}
		return result;
	} 

	public int checkMaxBitrateAccept(int streamBitrateValue) {
		if(Integer.parseInt(getAppSetting().getMaxBitrateAccept()) <= streamBitrateValue) {
			result = 2;
		}
		else {
			result = -1;
		}
		return result;

	} 
	
	
	public AppSettings getAppSetting() {
		
		if (appContext.containsBean(AppSettings.BEAN_NAME)) {
			appSettings = (AppSettings)appContext.getBean(AppSettings.BEAN_NAME);
		}
		
		return appSettings;

	}

	public ApplicationContext getAppContext() {
		return appContext;
	}

	public void setAppContext(ApplicationContext appContext) {
		this.appContext = appContext;
	}

}
