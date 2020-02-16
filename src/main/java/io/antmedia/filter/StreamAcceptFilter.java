package io.antmedia.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;

import io.antmedia.datastore.db.DataStore;
import io.antmedia.datastore.db.DataStoreFactory;
import io.antmedia.muxer.IStreamAcceptFilter;

public class StreamAcceptFilter implements IStreamAcceptFilter{

	private ApplicationContext appContext;

	protected static Logger logger = LoggerFactory.getLogger(StreamAcceptFilter.class);

	private int result;

	@Autowired
	private DataStoreFactory dataStoreFactory;

	private DataStore dataStore;

	@Value("${settings.maxFpsAccept:#{null}}")
	private String maxFpsAccept;

	@Value("${settings.maxResolutionAccept:#{null}}")
	private String maxResolutionAccept;

	@Value("${settings.maxBitrateAccept:#{null}}")
	private String maxBitrateAccept;

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
		if(getMaxFpsAccept() != null) {
			result = checkMaxFPSAccept(streamFps);
		}

		// Check Resolution value
		if(result == -1 && getMaxResolutionAccept() != null) {
			result = checkResolutionAccept(streamResolution);
		}

		// Check bitrate value
		if(result == -1 && getMaxBitrateAccept() != null) {
			result = checkMaxBitrateAccept(streamBitrate);
		}

		return result;
	}

	public int checkMaxFPSAccept(int streamFPSValue) {
		if(Integer.parseInt(getMaxFpsAccept()) <= streamFPSValue) {
			result = 0;
		}
		else {
			result = -1;
		}
		return result;
	} 

	public int checkResolutionAccept(int streamResolutionValue) {
		if(Integer.parseInt(getMaxResolutionAccept()) <= streamResolutionValue) {
			result = 1;
		}
		else {
			result = -1;
		}
		return result;
	} 

	public int checkMaxBitrateAccept(int streamBitrateValue) {
		if(Integer.parseInt(getMaxBitrateAccept()) <= streamBitrateValue) {
			result = 2;
		}
		else {
			result = -1;
		}
		return result;

	} 

	public ApplicationContext getAppContext() {
		return appContext;
	}

	public void setAppContext(ApplicationContext appContext) {
		this.appContext = appContext;
	}

	public DataStore getDatastore() {
		if (dataStore == null) {
			dataStore = dataStoreFactory.getDataStore();
		}
		return dataStore;
	}


	public void setDataStore(DataStore dataStore) {
		this.dataStore = dataStore;
	}

	public DataStoreFactory getDataStoreFactory() {
		return dataStoreFactory;
	}


	public void setDataStoreFactory(DataStoreFactory dataStoreFactory) {
		this.dataStoreFactory = dataStoreFactory;
	}

	public String getMaxFpsAccept() {
		return maxFpsAccept;
	}

	public void setMaxFpsAccept(String maxFpsAccept) {
		this.maxFpsAccept = maxFpsAccept;
	}

	public String getMaxResolutionAccept() {
		return maxResolutionAccept;
	}

	public void setMaxResolutionAccept(String maxResolutionAccept) {
		this.maxResolutionAccept = maxResolutionAccept;
	}

	public String getMaxBitrateAccept() {
		return maxBitrateAccept;
	}

	public void setMaxBitrateAccept(String maxBitrateAccept) {
		this.maxBitrateAccept = maxBitrateAccept;
	}



}
