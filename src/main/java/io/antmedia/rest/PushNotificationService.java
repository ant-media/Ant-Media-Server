package io.antmedia.rest;

import java.util.List;

import org.springframework.stereotype.Component;

import io.antmedia.rest.model.Result;
import io.swagger.annotations.Api;
import jakarta.ws.rs.Path;

@Api(value = "")
@Component
@Path("/v2/push-notification")
public class PushNotificationService {
	
	
	
	public Result getSubscriberAuthenticationToken() {
		return null;
	}
	
	public Result sendPushNotification(List<String> subscriberIdList, String jsonMessage, String serviceName) {
		return null;
	}
	
	
	public Result sendPushNotification(String topic, String jsonMessage, String serviceName) {
		return null;
	}
	
	public Result sendDataChannelMessage(List<String> subscriberIdList, String jsonMessage, String serviceName) {
		return null;
	}
	
	public Result sendDataChannelMessage(String subscriberId, String jsonMessage, String serviceName) {
		return null;
	}
	
	

	
	

}
