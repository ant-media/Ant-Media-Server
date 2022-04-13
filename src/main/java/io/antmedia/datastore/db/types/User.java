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
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(value="User", description="The basic user class")
@Entity(value = "user")
@Indexes({ @Index(fields = @Field("email")), @Index(fields = @Field("fullName")) })
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
	 * ADMIN can do anything in its scope.
	 *   If it's scope is system, it can CRUD anything
	 *   If it's scope is an application, it can CRUD anything in the application. 
	 *      it cannot access the web panel services
	 * 	  
	 * READ_ONLY can read anything in its scope.
	 *   If it's scope is system, it can READ anything 
	 *   If it's scope is an application, it can only READ anything in the application
	 *      it cannot access the web panel services
	 * 
	 * USER can do anything but cannot change the settings in its scope.
	 *   If it's scope is system, it can CRUD content but cannot change system settings/application settings
	 *   If it's scope is an application, it can CRUD content but cannot change application settings
	 *      it cannot access the web panel services
	 */
	@ApiModelProperty(value = "the type of the user", allowableValues = "ADMIN, READ-ONLY, USER")
	private UserType userType;

	/**
	 * Scope of the user. If it's scope is system, it can access the stuff in system-level
	 * It's scope is an application, it can access the stuff in application-level
	 * 
	 * It makes more sense with UserType
	 */
	@ApiModelProperty(value = "Scope can be 'system' or name of the application. Scope of the user. If it's scope is system, it can "
			+ "access the stuff in system-level. If it's scope is an application, it can access the stuff in application-level"
			+ "It makes more sense with UserType")
	private String scope;

	
	/**
	 * New password of the user, below field is not set  for all user types
	 */
	@ApiModelProperty(value = "the new password of the user")
	@JsonProperty(access = Access.WRITE_ONLY)
	private String newPassword;
	
	/**
	 * Name of the user. Use firstname and lastname
	 */
	@Deprecated
	@ApiModelProperty(value = "the name of the user")
	private String fullName;
	
	@ApiModelProperty(value = "Fist name of the user")
	private String firstName;
	
	@ApiModelProperty(value = "last name of the user")
	private String lastName;
	
	/**
	 * URL of the picture if exists
	 */
	@ApiModelProperty(value = "the URL of the user picture")
	private String picture;
	
	/**
	 * ID of the user
	 */
	@ApiModelProperty(value = "the id of the user")
	@JsonIgnore
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