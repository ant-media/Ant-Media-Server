package io.antmedia.datastore.db.types;

import dev.morphia.annotations.Entity;

import java.io.Serializable;

public enum UserType implements Serializable {
	
	/**
	 * ADMIN user, It can do anything in its scope
	 */
	ADMIN("ADMIN"),

	/**
	 * READ_ONLY user can just read the value in its scope
	 */
	READ_ONLY("READ_ONLY"),
	
	/**
	 * USER, it can CRUD content but it cannot change settings
	 */
	USER("USER");
	
	public String typeName;
	
	UserType(String typeName) {
		this.typeName = typeName;
	}
	
	@Override
	public String toString() {
		return typeName;
	}

}
