package io.antmedia.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;

import io.antmedia.datastore.db.DataStore;
import io.antmedia.datastore.db.DataStoreFactory;
import io.antmedia.rest.model.Result;

public class StreamAcceptFilter {

	private ApplicationContext appContext;

	protected static Logger logger = LoggerFactory.getLogger(StreamAcceptFilter.class);

	@Autowired
	private DataStoreFactory dataStoreFactory;

	private DataStore dataStore;

	@Value("${settings.maxFpsAccept}")
	private String maxFpsAccept = null;

	@Value("${settings.maxResolutionAccept}")
	private String maxResolutionAccept = null;

	@Value("${settings.maxBitrateAccept}")
	private String maxBitrateAccept = null;

	public Result checkStreamParameters(String parameters) {

		Result result;

		// Check bitrate value
		result = checkMaxBitrateAccept(Integer.parseInt(parameters));

		// Check FPS value
		if(result.isSuccess()) {
			result = checkMaxFPSAccept(Integer.parseInt(parameters));
		}

		// Check Resolution value
		if(result.isSuccess()) {
			result = checkResolutionAccept(Integer.parseInt(parameters));
		}

		return result;
	}

	public Result checkMaxBitrateAccept(int streamBitrateValue) {
		Result result = new Result(false);

		if(getMaxBitrateAccept() != null) {
			result.setSuccess(true);
		}
		else if(Integer.parseInt(getMaxBitrateAccept()) <= streamBitrateValue) {
			result.setSuccess(false);
			result.setMessage("Max Bitrate error");
		}
		else {
			result.setSuccess(true);
		}

		return result;

	} 

	public Result checkMaxFPSAccept(int streamFPSValue) {
		Result result = new Result(false);

		if(getMaxFpsAccept() != null) {
			result.setSuccess(true);
		}
		else if(Integer.parseInt(getMaxFpsAccept()) <= streamFPSValue) {
			result.setSuccess(false);
			result.setMessage("Max FPS error");
		}
		else {
			result.setSuccess(true);
		}

		return result;

	} 

	public Result checkResolutionAccept(int streamResolutionValue) {
		Result result = new Result(false);

		if(getMaxResolutionAccept() != null) {
			result.setSuccess(true);
		}
		else if(Integer.parseInt(getMaxResolutionAccept()) <= streamResolutionValue) {
			result.setSuccess(false);
			result.setMessage("Max Resolution error");
		}
		else {
			result.setSuccess(true);
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
