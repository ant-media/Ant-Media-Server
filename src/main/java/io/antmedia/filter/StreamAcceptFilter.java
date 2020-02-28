package io.antmedia.filter;

import org.bytedeco.javacpp.avcodec.AVPacket;
import org.bytedeco.javacpp.avformat.AVFormatContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import io.antmedia.AppSettings;
import io.antmedia.muxer.IStreamAcceptFilter;

public class StreamAcceptFilter implements IStreamAcceptFilter,ApplicationContextAware{

	private AppSettings appSettings;

	protected static Logger logger = LoggerFactory.getLogger(StreamAcceptFilter.class);

	public boolean isValidStreamParameters(AVFormatContext inputFormatContext,AVPacket pkt) {
		// Check FPS value
		return  checkFPSAccept(getStreamFps(inputFormatContext, pkt)) && 
				checkResolutionAccept(getStreamResolution(inputFormatContext, pkt)) &&
				checkBitrateAccept(getStreamBitrate(inputFormatContext, pkt));
	}

	public boolean checkFPSAccept(int streamFPSValue) {
		if(getMaxFpsAccept() == null) {
			return true;
		}
		else if(Integer.parseInt(getMaxFpsAccept()) < streamFPSValue) {
			logger.error("Current Stream Max FPS reached. Stream FPS less than {} but Stream FPS is: {}", Integer.parseInt(getMaxFpsAccept()), streamFPSValue);
			return false;
		}
		return true;
	} 

	public boolean checkResolutionAccept(int streamResolutionValue) {
		if(getMaxResolutionAccept() == null) {
			return true;
		}
		else if(Integer.parseInt(getMaxResolutionAccept()) < streamResolutionValue) {
			logger.error("Current Stream Max Resolution reached. Stream Resolution less than {} but Stream Resolution is: {}", Integer.parseInt(getMaxResolutionAccept()), streamResolutionValue);
			return false;
		}
		return true;
	} 

	public boolean checkBitrateAccept(long streamBitrateValue) {
		if(getMaxBitrateAccept() == null) {
			return true;
		}		
		else if(Integer.parseInt(getMaxBitrateAccept()) < streamBitrateValue) {
			logger.error("Current Stream Max Bitrate reached. Stream Bitrate less than {} but Stream Bitrate is: {}", Integer.parseInt(getMaxBitrateAccept()), streamBitrateValue);
			return false;
		}
		return true;
	} 

	public int getStreamFps(AVFormatContext inputFormatContext,AVPacket pkt) {
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


	public AppSettings getAppSettings() {
		return appSettings;
	}


	public String getMaxResolutionAccept() {
		return getAppSettings().getMaxResolutionAccept();
	}

	public String getMaxBitrateAccept() {
		return getAppSettings().getMaxBitrateAccept();
	}

	public String getMaxFpsAccept() {
		return getAppSettings().getMaxFpsAccept();
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		if (applicationContext.containsBean(AppSettings.BEAN_NAME)) {
			appSettings = (AppSettings)applicationContext.getBean(AppSettings.BEAN_NAME);
		}
	}

}
