package io.antmedia.test.rest;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.json.simple.JSONObject;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationContext;

import io.antmedia.AppSettings;
import io.antmedia.filter.JWTFilter;
import io.antmedia.pushnotification.IPushNotificationService;
import io.antmedia.rest.PushNotificationRestService;
import io.antmedia.rest.model.PushNotificationToSubscribers;
import io.antmedia.rest.model.Result;
import io.antmedia.websocket.WebSocketConstants;
import jakarta.servlet.ServletContext;

public class PushNotificationRestServiceTest {
	
	
	@Test
	public void testSendNotification() {
		PushNotificationRestService pushNotificationRestService = new PushNotificationRestService();
		IPushNotificationService pushNotificationService = Mockito.mock(IPushNotificationService.class);
		ServletContext servletContext = Mockito.mock(ServletContext.class);
		
		pushNotificationRestService.setServletContext(servletContext);
		
		ApplicationContext appContext = Mockito.mock(ApplicationContext.class);
		Mockito.when(servletContext.getAttribute(Mockito.anyString())).thenReturn(appContext);
		
		Mockito.when(appContext.getBean(IPushNotificationService.BEAN_NAME)).thenReturn(pushNotificationService);
		
		JSONObject jsObject = new JSONObject();
		jsObject.put("title", "hello world");
		jsObject.put("apn-topic", "io.antmedia.ios.webrtc.sample");
		
		pushNotificationRestService.sendPushNotification("topic", jsObject.toJSONString(),"fcm");
		verify(pushNotificationService).sendNotification("topic", jsObject,"fcm");
		
		pushNotificationRestService.sendPushNotification("topic", jsObject.toJSONString(),"apns");
		verify(pushNotificationService).sendNotification("topic", jsObject,"apns");
		
		pushNotificationRestService.sendPushNotification("topic", jsObject.toJSONString(),"");
		verify(pushNotificationService).sendNotification("topic", jsObject);
		
		pushNotificationRestService.sendPushNotification("topic", jsObject.toJSONString(), null);
		verify(pushNotificationService, times(2)).sendNotification("topic", jsObject);
		
		Result result = pushNotificationRestService.sendPushNotification("topic", "Not json string", null);
		assertFalse(result.isSuccess());
		assertTrue(result.getMessage().contains("cannot be parsed"));
		
		
		PushNotificationToSubscribers pushNotificationToSubscribers = new PushNotificationToSubscribers();
		
		pushNotificationToSubscribers.setJsonMessage(jsObject.toJSONString());
		pushNotificationToSubscribers.setSubscribers(Arrays.asList("subscriber1", "subscriber2"));
		pushNotificationRestService.sendPushNotification(pushNotificationToSubscribers, "fcm");
		verify(pushNotificationService).sendNotification(Arrays.asList("subscriber1", "subscriber2"), jsObject,"fcm");
		
		
		pushNotificationRestService.sendPushNotification(pushNotificationToSubscribers, "apns");
		verify(pushNotificationService).sendNotification(Arrays.asList("subscriber1", "subscriber2"), jsObject,"apns");
		
		pushNotificationRestService.sendPushNotification(pushNotificationToSubscribers,"");
		verify(pushNotificationService).sendNotification(Arrays.asList("subscriber1", "subscriber2"), jsObject);
		
		pushNotificationRestService.sendPushNotification(pushNotificationToSubscribers, null);
		verify(pushNotificationService, times(2)).sendNotification(Arrays.asList("subscriber1", "subscriber2"), jsObject);
		
		
		pushNotificationToSubscribers.setJsonMessage("Not json string");
		result = pushNotificationRestService.sendPushNotification(pushNotificationToSubscribers, null);
		assertFalse(result.isSuccess());
		assertTrue(result.getMessage().contains("cannot be parsed"));
		
		
	}

	


	@Test
    public void testGetToken() {
		PushNotificationRestService pushNotificationRestService = new PushNotificationRestService();
		IPushNotificationService pushNotificationService = Mockito.mock(IPushNotificationService.class);
		ServletContext servletContext = Mockito.mock(ServletContext.class);
		
		pushNotificationRestService.setServletContext(servletContext);
		
		ApplicationContext appContext = Mockito.mock(ApplicationContext.class);
		Mockito.when(servletContext.getAttribute(Mockito.anyString())).thenReturn(appContext);
		
		Mockito.when(appContext.getBean(IPushNotificationService.BEAN_NAME)).thenReturn(pushNotificationService);
		AppSettings appSettings = new AppSettings();
		when(appContext.getBean(AppSettings.BEAN_NAME)).thenReturn(appSettings);
        
        Result result = pushNotificationRestService.getSubscriberAuthenticationToken("subscriber1", 0);
        assertTrue(JWTFilter.isJWTTokenValid(appSettings.getSubscriberAuthenticationKey(), result.getDataId(), WebSocketConstants.SUBSCRIBER_ID, "subscriber1"));
        
        assertFalse(JWTFilter.isJWTTokenValid(appSettings.getSubscriberAuthenticationKey(), result.getDataId(), WebSocketConstants.SUBSCRIBER_ID, "subscriber12"));
        
        result = pushNotificationRestService.getSubscriberAuthenticationToken("subscriber1", 3);
        
        assertTrue(JWTFilter.isJWTTokenValid(appSettings.getSubscriberAuthenticationKey(), result.getDataId(), WebSocketConstants.SUBSCRIBER_ID, "subscriber1"));
        String token = result.getDataId();
		Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> {
			return !JWTFilter.isJWTTokenValid(appSettings.getSubscriberAuthenticationKey(), token,
					"subscriber1");
		});

	}
	

	
}
