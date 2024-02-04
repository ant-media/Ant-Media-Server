package io.antmedia.test.rest;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationContext;

import io.antmedia.AppSettings;
import io.antmedia.filter.JWTFilter;
import io.antmedia.pushnotification.IPushNotificationService;
import io.antmedia.rest.PushNotificationRestService;
import io.antmedia.rest.model.Result;
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
		
		
		pushNotificationRestService.sendPushNotification("topic", "jsonMessage","fcm");
		verify(pushNotificationService).sendNotification("topic", "jsonMessage","fcm");
		
		pushNotificationRestService.sendPushNotification("topic", "jsonMessage","apns");
		verify(pushNotificationService).sendNotification("topic", "jsonMessage","apns");
		
		pushNotificationRestService.sendPushNotification("topic", "jsonMessage","");
		verify(pushNotificationService).sendNotification("topic", "jsonMessage");
		
		pushNotificationRestService.sendPushNotification("topic", "jsonMessage", null);
		verify(pushNotificationService, times(2)).sendNotification("topic", "jsonMessage");
		
		
		
		pushNotificationRestService.sendPushNotification(Arrays.asList("subscriber1", "subscriber2"), "jsonMessage", "fcm");
		verify(pushNotificationService).sendNotification(Arrays.asList("subscriber1", "subscriber2"), "jsonMessage","fcm");
		
		
		pushNotificationRestService.sendPushNotification(Arrays.asList("subscriber1", "subscriber2"), "jsonMessage", "apns");
		verify(pushNotificationService).sendNotification(Arrays.asList("subscriber1", "subscriber2"), "jsonMessage","apns");
		
		pushNotificationRestService.sendPushNotification(Arrays.asList("subscriber1", "subscriber2"), "jsonMessage", "");
		verify(pushNotificationService).sendNotification(Arrays.asList("subscriber1", "subscriber2"), "jsonMessage");
		
		pushNotificationRestService.sendPushNotification(Arrays.asList("subscriber1", "subscriber2"), "jsonMessage", null);
		verify(pushNotificationService, times(2)).sendNotification(Arrays.asList("subscriber1", "subscriber2"), "jsonMessage");
		
		
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
        assertTrue(JWTFilter.isJWTTokenValid(appSettings.getSubscriberAuthenticationKey(), result.getDataId(), "subscriber1"));
        
        assertFalse(JWTFilter.isJWTTokenValid(appSettings.getSubscriberAuthenticationKey(), result.getDataId(), "subscriber12"));
        
        result = pushNotificationRestService.getSubscriberAuthenticationToken("subscriber1", 3);
        
        assertTrue(JWTFilter.isJWTTokenValid(appSettings.getSubscriberAuthenticationKey(), result.getDataId(), "subscriber1"));
        String token = result.getDataId();
		Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> {
			return !JWTFilter.isJWTTokenValid(appSettings.getSubscriberAuthenticationKey(), token,
					"subscriber1");
		});


	}
	
}
