package io.antmedia.social;

public enum ResourceOrigin {
	/**
	 * Resource from Facebook
	 */
	FACEBOOK("facebook"), 
	/**
	 * Resource from Periscope
	 */
	PERISCOPE("periscope"),
	/**
	 * Resource from Youtube
	 */
	YOUTUBE("youtube"), 
	/**
	 * Resource from Server directly
	 */
	SERVER("server");
	
	private String originName;
	
	ResourceOrigin(String name) {
	    this.originName =  name;
	 }
	
	@Override
	public String toString() {
		return this.originName;
	}

}
