package io.antmedia.datastore.db.types;

import dev.morphia.annotations.Entity;
import io.antmedia.pushnotification.IPushNotificationService.PushNotificationServiceTypes;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Push Notification Token")
@Entity
public class PushNotificationToken {

    @Schema(description = "The token value")
	private String token;

	/**
	 * {@link PushNotificationServiceTypes}
	 * fcm or apn
	 * fcm: Firebase Cloud Messagnig
	 * apn: Apple Push Notification
	 */
    @Schema(description = "The service name for push notifications. Possible values are 'fcm' for Firebase Cloud Messaging or 'apn' for Apple Push Notification.")
	private String serviceName;
	
	private String extraData;
	
	//Keep empty constructor for construction from mongodb and similar ways. 
	public PushNotificationToken() {
	}

	public PushNotificationToken(String token, String serviceName) {
		super();
		this.setToken(token);
		this.setServiceName(serviceName);
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public String getServiceName() {
		return serviceName;
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	public String getExtraData() {
		return extraData;
	}

	public void setExtraData(String extraData) {
		this.extraData = extraData;
	}
	
	
	
	
	

}
