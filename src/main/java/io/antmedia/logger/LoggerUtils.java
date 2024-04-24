package io.antmedia.logger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import io.antmedia.analytic.model.AnalyticEvent;



public class LoggerUtils {

	static Gson gson = new Gson();

	private static final Logger analyticsLogger = LoggerFactory.getLogger("analytics");


	private LoggerUtils() {
		//Hide public constructor
	}

	public static void logAnalyticsFromClient(AnalyticEvent event) {
		event.setLogSource(AnalyticEvent.LOG_SOURCE_CLIENT);
		if (analyticsLogger.isInfoEnabled()) {
			analyticsLogger.info(gson.toJson(event));
		}

	}


	public static void logAnalyticsFromServer(AnalyticEvent event) {
		event.setLogSource(AnalyticEvent.LOG_SOURCE_SERVER);
		if (analyticsLogger.isInfoEnabled()) {
			analyticsLogger.info(gson.toJson(event));
		}
		
	}


}
