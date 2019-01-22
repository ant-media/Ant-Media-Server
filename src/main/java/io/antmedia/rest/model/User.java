package io.antmedia.rest.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(value="User", description="The basic user class")
public class User {
	
	/**
	 * Email of the user
	 */
	@ApiModelProperty(value = "the email of the user")
	private String email;
	
	/**
	 * Password of the user, ignore password and this field is not set for all user types
	 */
	@ApiModelProperty(value = "the password of the user")
	@JsonProperty(access = Access.WRITE_ONLY)
	private String password;
	
	/**
	 * Type of the user
	 */
	@ApiModelProperty(value = "the type of the user", allowableValues = "facebook_user, periscope_user, youtube_user, admin")
	private UserType userType;
	
	/**
	 * New password of the user, below field is not set  for all user types
	 */
	@ApiModelProperty(value = "the new password of the user")
	@JsonProperty(access = Access.WRITE_ONLY)
	private String newPassword;
	
	/**
	 * Name of the user
	 */
	@ApiModelProperty(value = "the name of the user")
	private String fullName;
	
	/**
	 * URL of the picture if exists
	 */
	@ApiModelProperty(value = "the URL of the user picture")
	private String picture;
	
	/**
	 * ID of the user
	 */
	@ApiModelProperty(value = "the id of the user")
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