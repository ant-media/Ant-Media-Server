package io.antmedia.test.pushnotification;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Arrays;

import org.junit.Test;

import io.antmedia.pushnotification.PushNotificationServiceCommunity;
import io.antmedia.rest.model.Result;

public class PushNotificationCommunityTest {
	
	
	@Test
	public void testPushNotificaitonServiceCommunity() {
		PushNotificationServiceCommunity pushNotificationServiceCommunity = new PushNotificationServiceCommunity();
		
		Result sendNotification = pushNotificationServiceCommunity.sendNotification("title", "message", "token");
		assertFalse(sendNotification.isSuccess());
		assertEquals("Push Notification Service is not available community edition. Please use enterprise edition", sendNotification.getMessage());
		
		sendNotification = pushNotificationServiceCommunity.sendNotification("topic", "message");
		assertFalse(sendNotification.isSuccess());
		assertEquals("Push Notification Service is not available community edition. Please use enterprise edition", sendNotification.getMessage());
		
		sendNotification = pushNotificationServiceCommunity.sendNotification(Arrays.asList(""), "message");
		assertFalse(sendNotification.isSuccess());
		assertEquals("Push Notification Service is not available community edition. Please use enterprise edition", sendNotification.getMessage());
		
		sendNotification = pushNotificationServiceCommunity.sendNotification(Arrays.asList(""), "message", "serviceName");
		assertFalse(sendNotification.isSuccess());
		assertEquals("Push Notification Service is not available community edition. Please use enterprise edition", sendNotification.getMessage());
		
	}

}
