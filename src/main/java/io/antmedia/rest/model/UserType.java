package io.antmedia.rest.model;

public enum UserType {
	
	/**
	 * ADMIN user, below user type is for control panel management
	 */
	ADMIN("admin"),

	/**
	 * READ_ONLY user, below user type is for control panel management
	 */
	READ_ONLY("read_only");
	
	
	private String typeName;
	
	UserType(String typeName) {
		this.typeName = typeName;
	}
	
	@Override
	public String toString() {
		return typeName;
	}

}
