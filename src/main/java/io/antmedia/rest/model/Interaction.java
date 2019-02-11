package io.antmedia.rest.model;

import io.antmedia.social.ResourceOrigin;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(value="Interaction", description="The social media interaction for stream class")
public class Interaction {
	
	/**
	 * Origin of the resource
	 */
	@ApiModelProperty(value = "the origin of the resource", allowableValues = "facebook, periscope, youtube, server")
	private ResourceOrigin origin;
	
	/**
	 * Total number of like interaction
	 */
	@ApiModelProperty(value = "the total number of like interaction")
	private int likeCount;
	
	/**
	 * Total number of wow interaction
	 */
	@ApiModelProperty(value = "the total number of wow interaction")
	private int wowCount;
	
	/**
	 * Total number of sad interaction
	 */
	@ApiModelProperty(value = "the total number of sad interaction")
	private int sadCount;
	
	/**
	 * Total number of angry interaction
	 */
	@ApiModelProperty(value = "the total number of angry interaction")
	private int angryCount;
	
	/**
	 * Total number of haha count
	 */
	@ApiModelProperty(value = "the total number of haha interaction")
	private int hahaCount;
	
	/**
	 * Total number of love count;
	 */
	@ApiModelProperty(value = "the total number of like interaction")
	private int loveCount;


	public ResourceOrigin getOrigin() {
		return origin;
	}


	public void setOrigin(ResourceOrigin origin) {
		this.origin = origin;
	}


	public int getLikeCount() {
		return likeCount;
	}


	public void setLikeCount(int likeCount) {
		this.likeCount = likeCount;
	}


	public int getWowCount() {
		return wowCount;
	}


	public void setWowCount(int wowCount) {
		this.wowCount = wowCount;
	}


	public int getSadCount() {
		return sadCount;
	}


	public void setSadCount(int sadCount) {
		this.sadCount = sadCount;
	}


	public int getAngryCount() {
		return angryCount;
	}


	public void setAngryCount(int angryCount) {
		this.angryCount = angryCount;
	}


	public int getHahaCount() {
		return hahaCount;
	}


	public void setHahaCount(int hahaCount) {
		this.hahaCount = hahaCount;
	}


	public int getLoveCount() {
		return loveCount;
	}


	public void setLoveCount(int loveCount) {
		this.loveCount = loveCount;
	}
	
	
	

}
