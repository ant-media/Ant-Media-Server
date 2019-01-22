package io.antmedia.social;

import io.antmedia.rest.model.User;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(value="LiveComment", description="The live comment class written on social media about related stream")
public class LiveComment {
	
	/**
	 * id of the comment
	 */
	@ApiModelProperty(value = "the id of the comment")
	private String id;
	
	/**
	 * Content of the comment
	 */
	@ApiModelProperty(value = "the content of the comment")
	private String message;
	
	/**
	 * User who write this comment
	 */
	@ApiModelProperty(value = "the owner of the comment")
	private User from;

	/**
	 * Origin of the comment
	 */
	@ApiModelProperty(value = "the origin of the comment", allowableValues = "facebook, periscope, youtube, server")
	private ResourceOrigin origin;
	
	/**
	 * Timestamp of the comment
	 */
	@ApiModelProperty(value = "the timestamp of the stream")
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
