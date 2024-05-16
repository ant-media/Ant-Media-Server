package io.antmedia.datastore.db.types;

import org.bson.types.ObjectId;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Field;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Index;
import dev.morphia.annotations.Indexes;
import dev.morphia.utils.IndexType;
import io.antmedia.rest.model.UserType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "The user information")
@Entity(value = "user")
@Indexes({ @Index(fields = @Field("email")), @Index(fields = @Field("fullName")) })
public class User {
	
	/**
     * The email of the user.
     */
    @Schema(description = "The email of the user")
    private String email;

    /**
     * The password of the user. This field is only set for certain user types.
     */
    @Schema(description = "The password of the user")
    private String password;

    /**
     * The type of the user.
     */
    @Schema(description = "The type of the user", allowableValues = {"ADMIN", "READ-ONLY", "USER"})
    private UserType userType;

    /**
     * The scope of the user. It can be 'system' or the name of the application.
     */
    @Schema(description = "The scope of the user. If it's 'system', it can access system-level stuff. If it's an application name, it can access application-level stuff.")
    private String scope;

    /**
     * The new password of the user. This field is only set for certain user types.
     */
    @Schema(description = "The new password of the user")
    private String newPassword;

    /**
     * The first name of the user.
     */
    @Schema(description = "The first name of the user")
    private String firstName;

    /**
     * The last name of the user.
     */
    @Schema(description = "The last name of the user")
    private String lastName;
    
    @Deprecated
    @Schema(description = "The full name of the user")
    private String fullName;


    /**
     * The URL of the user's picture.
     */
    @Schema(description = "The URL of the user's picture")
    private String picture;

    /**
     * The id of the user.
     */
    @Schema(description = "The id of the user")
    @Id
    private ObjectId id;
	
	public User(String email, String password, UserType userType, String scope) {
		this.email = email;
		this.password = password;
		this.userType = userType;
		this.scope = scope;
	}

	public User() {
		
	}
	
	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getScope() {
		return scope;
	}

	public void setScope(String scope) {
		this.scope = scope;
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

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}	
}