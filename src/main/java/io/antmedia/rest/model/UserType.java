package io.antmedia.rest.model;

public enum UserType {
	
	/**
	 * ADMIN user, It can do anything in its scope
	 */
	ADMIN("admin"),

	/**
	 * READ_ONLY user can just read the value in its scope
	 */
	READ_ONLY("read_only"),
	
	/**
	 * USER, it can CRUD content but it cannot change settings
	 */
	USER("user");
	
	private String typeName;
	
	UserType(String typeName) {
		this.typeName = typeName;
	}
	
	@Override
	public String toString() {
		return typeName;
	}

}
