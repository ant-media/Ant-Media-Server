package io.antmedia.rest.model;

import io.antmedia.social.ResourceOrigin;

public class Interaction {
	
	/**
	 * Origin of the resource
	 */
	private ResourceOrigin origin;
	
	/**
	 * Total number of like interaction
	 */
	private int likeCount;
	
	/**
	 * Total number of wow interaction
	 */
	private int wowCount;
	
	/**
	 * Total number of sad interaction
	 */
	private int sadCount;
	
	/**
	 * Total number of angry interaction
	 */
	private int angryCount;
	
	/**
	 * Total number of haha count
	 */
	private int hahaCount;
	
	/**
	 * Total number of love count;
	 */
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
