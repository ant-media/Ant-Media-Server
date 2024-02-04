package io.antmedia.pushnotification;

import java.util.List;

import io.antmedia.rest.model.Result;

public interface IPushNotificationService {
	
	static final String BEAN_NAME = "push.notification.service";
	
	public enum PushNotificationServiceTypes {
		FIREBASE_CLOUD_MESSAGING("fcm"),
		APPLE_PUSH_NOTIFICATION("apn");
		
		
		private String name;
		
		PushNotificationServiceTypes(String name) {
			this.name = name;
		}
		
		@Override
		public String toString() {
			return this.name;
		}
	}
		
	/**
	 * Send push notifcaiton according to service name
	 * 
	 * @param topic
	 * @param jsonMessage
	 * @param serviceName: fcm or apn
	 * @return
	 */
	Result sendNotification(String topic, String jsonMessage, String serviceName);
	
	/**
	 * Send notification to both services if they are enabled
	 * 
	 * @param topic
	 * @param jsonMessage
	 * @return
	 */
	Result sendNotification(String topic, String jsonMessage) ;
	
	
	/**
	 * Send notification according to the subscriberIds
	 * @param subscriberIds
	 * @param jsonMessage
	 * @return
	 */
	Result sendNotification(List<String> subscriberIds, String jsonMessage);
	
	
	/**
	 * Send notification according to the subscriberIds and service
	 * @param subscriberIds
	 * @param jsonMessage
	 * @return
	 */
	Result sendNotification(List<String> subscriberIds, String jsonMessage, String serviceName);
}
