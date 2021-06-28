package io.antmedia.datastore.db.types;


import com.fasterxml.jackson.annotation.JsonIgnore;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;


@ApiModel(value="ConnectionEvent", description="Connection Event for the subscriber")
public class ConnectionEvent {
	@JsonIgnore
	public static final String CONNECTED_EVENT = "connected";
	@JsonIgnore
	public static final String DISCONNECTED_EVENT = "disconnected";
	
	/**
	 * timestamp of this event
	 */
	@ApiModelProperty(value = "the timestamp of the event")
	private long timestamp;
	
	/**
	 * type of the event Connection/Disconnection
	 */
	@ApiModelProperty(value = "the type of the event")
	private String eventType;

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public String getEventType() {
		return eventType;
	}

	public void setEventType(String eventType) {
		this.eventType = eventType;
	}
	
}
