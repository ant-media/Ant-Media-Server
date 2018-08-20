package io.antmedia.rest.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;

public class User {
	
	/**
	 * Email of the user
	 */
	private String email;
	
	/**
	 * Password of the user, ignore password and this field is not set for all user types
	 */
	@JsonProperty(access = Access.WRITE_ONLY)
	private String password;
	
	/**
	 * Type of the user
	 */
	private UserType userType;
	
	/**
	 * New password of the user, below field is not set  for all user types
	 */
	@JsonProperty(access = Access.WRITE_ONLY)
	private String newPassword;
	
	/**
	 * Name of the user
	 */
	private String fullName;
	
	/**
	 * URL of the picture if exists
	 */
	private String picture;
	
	/**
	 * ID of the user
	 */
	private String id;
	
	public User(String email, String password, UserType userType) {
		this.email = email;
		this.password = password;
		this.userType = userType;
	}
	
	public User(String id, String name, String picture, UserType userType) {
		this.id = id;
		this.fullName = name;
		this.picture = picture;
		this.userType = userType;
	}
	
	
	public User() {
		
	}
	
	
	public String getEmail() {
		return email;
	}


	public void setEmail(String email) {
		this.email = email;
	}


	public String getPassword() {
		return password;
	}


	public void setPassword(String password) {
		this.password = password;
	}


	public UserType getUserType() {
		return userType;
	}


	public void setUserType(UserType userType) {
		this.userType = userType;
	}


	public String getNewPassword() {
		return newPassword;
	}


	public void setNewPassword(String newPassword) {
		this.newPassword = newPassword;
	}

	public String getFullName() {
		return fullName;
	}

	public void setFullName(String fullName) {
		this.fullName = fullName;
	}

	public String getPicture() {
		return picture;
	}

	public void setPicture(String picture) {
		this.picture = picture;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}




	
}