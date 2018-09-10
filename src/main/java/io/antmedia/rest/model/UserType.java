package io.antmedia.rest.model;

public enum UserType {
	
	/**
	 * Facebook user, below user type is used for social media 
	 */
	FACEBOOK_USER("facebook_user"),
	
	/**
	 * Periscope user, below user type is used for social media 
	 */
	PERISCOPE_USER("periscope_user"),
	
	/**
	 * Youtube user, below user type is used for social media 
	 */
	YOUTUBE_USER("youtube_user"),
	
	/**
	 * ADMIN user, below user type is for control panel management
	 */
	ADMIN("admin");
	
	
	private String typeName;
	
	UserType(String typeName) {
		this.typeName = typeName;
	}
	
	@Override
	public String toString() {
		return typeName;
	}

}
