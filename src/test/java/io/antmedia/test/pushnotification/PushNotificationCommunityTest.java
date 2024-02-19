package io.antmedia.test.pushnotification;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Arrays;

import org.json.simple.JSONObject;
import org.junit.Test;

import io.antmedia.pushnotification.PushNotificationServiceCommunity;
import io.antmedia.rest.model.Result;

public class PushNotificationCommunityTest {
	
	
	@Test
	public void testPushNotificaitonServiceCommunity() {
		PushNotificationServiceCommunity pushNotificationServiceCommunity = new PushNotificationServiceCommunity();
		
		JSONObject jsObject = new JSONObject();
		jsObject.put("title", "hello world");
		jsObject.put("apn-topic", "io.antmedia.ios.webrtc.sample");
		
		Result sendNotification = pushNotificationServiceCommunity.sendNotification("title", jsObject, "token");
		assertFalse(sendNotification.isSuccess());
		assertEquals("Push Notification Service is not available community edition. Please use enterprise edition", sendNotification.getMessage());
		
		sendNotification = pushNotificationServiceCommunity.sendNotification("topic", jsObject);
		assertFalse(sendNotification.isSuccess());
		assertEquals("Push Notification Service is not available community edition. Please use enterprise edition", sendNotification.getMessage());
		
		sendNotification = pushNotificationServiceCommunity.sendNotification(Arrays.asList(""), jsObject);
		assertFalse(sendNotification.isSuccess());
		assertEquals("Push Notification Service is not available community edition. Please use enterprise edition", sendNotification.getMessage());
		
		sendNotification = pushNotificationServiceCommunity.sendNotification(Arrays.asList(""), jsObject, "serviceName");
		assertFalse(sendNotification.isSuccess());
		assertEquals("Push Notification Service is not available community edition. Please use enterprise edition", sendNotification.getMessage());
		
	}

}
