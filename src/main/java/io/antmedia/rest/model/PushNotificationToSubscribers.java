package io.antmedia.rest.model;

import java.util.List;

public class PushNotificationToSubscribers {
	
	private List<String> subscribers;
	
	private String jsonMessage;

	public String getJsonMessage() {
		return jsonMessage;
	}

	public void setJsonMessage(String jsonMessage) {
		this.jsonMessage = jsonMessage;
	}

	public List<String> getSubscribers() {
		return subscribers;
	}

	public void setSubscribers(List<String> subscribers) {
		this.subscribers = subscribers;
	}

	
	
	

}
