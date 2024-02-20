package io.antmedia.pushnotification;

import java.util.List;

import org.json.simple.JSONObject;

import io.antmedia.rest.model.Result;

public class PushNotificationServiceCommunity implements IPushNotificationService {
	
	private static final String MESSAGE_TO_USE_ENTERPRISE_EDITION = "Push Notification Service is not available community edition. Please use enterprise edition";

	
	@Override
	public Result sendNotification(String topic, JSONObject jsonMessage, String serviceName) {
		return new Result(false, MESSAGE_TO_USE_ENTERPRISE_EDITION);
	}
	
	@Override
	public Result sendNotification(String topic, JSONObject jsonMessage) {
		return new Result(false, MESSAGE_TO_USE_ENTERPRISE_EDITION);
	}

	@Override
	public Result sendNotification(List<String> subscriberIds, JSONObject jsonMessage) {
		return new Result(false, MESSAGE_TO_USE_ENTERPRISE_EDITION);
	}

	@Override
	public Result sendNotification(List<String> subscriberIds, JSONObject jsonMessage, String serviceName) {
		return new Result(false, MESSAGE_TO_USE_ENTERPRISE_EDITION);
	}

}
