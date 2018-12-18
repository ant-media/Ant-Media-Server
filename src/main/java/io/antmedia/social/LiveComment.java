package io.antmedia.social;

import io.antmedia.rest.model.User;

public class LiveComment {
	
	/**
	 * id of the comment
	 */
	private String id;
	
	/**
	 * Content of the comment
	 */
	private String message;
	
	/**
	 * User who write this comment
	 */
	private User from;

	/**
	 * Origin of the comment
	 */
	private ResourceOrigin origin;
	
	/**
	 * Timestamp of the comment
	 */
	private long timestamp;

	public LiveComment(String id, String message, User from, ResourceOrigin origin, long timestamp) {
		this.id = id;
		this.message = message;
		this.from = from;
		this.origin = origin;
		this.timestamp = timestamp;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public User getFrom() {
		return from;
	}

	public void setFrom(User from) {
		this.from = from;
	}

	public ResourceOrigin getOrigin() {
		return origin;
	}

	public void setOrigin(ResourceOrigin origin) {
		this.origin = origin;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}
	
	

}
