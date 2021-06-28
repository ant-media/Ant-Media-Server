package io.antmedia.datastore.db.types;

import dev.morphia.annotations.Embedded;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Field;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Index;
import dev.morphia.annotations.Indexes;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * POJO to store security credentials for social endpoints.
 * @author mekya
 *
 */
@Entity("SocialEndpointCredentials")

@Indexes({ @Index(fields = @Field("id"))})
@ApiModel(value="SocialEndpointCredentials", description="The SocialEndpoint Credentials  class")
public class SocialEndpointCredentials {
	
	/**
	 * Access token to make the service calls
	 */
	@JsonIgnore
	@ApiModelProperty(value = "the access token to make the service calls")
	private String accessToken;
	
	/**
	 * Refresh token to refresh the access token
	 */
	@JsonIgnore
	@ApiModelProperty(value = "the refresh token to refresh the access token")
	private String refreshToken;
	
	/**
	 * Token type
	 */
	@JsonIgnore
	@ApiModelProperty(value = "the type of the token")
	private String tokenType;
	
	/**
	 * Expire time in seconds
	 */
	@JsonIgnore
	@ApiModelProperty(value = "the expire time in seconds")
	private String expireTimeInSeconds;
	
	/**
	 * Authentication time in milliseconds
	 */
	@JsonIgnore
	@ApiModelProperty(value = "the authentication time in milliseconds")
	private String authTimeInMilliseconds;
	
	/**
	 * Id of the record that is stored in db
	 */
	@Id
	@ApiModelProperty(value = "the id of the record that is stored in db")
	private String id;
	
	/**
	 * Account or Page name
	 */
	@ApiModelProperty(value = "the account or Page name")
	private String accountName;
	
	/**
	 * Id of the account if exists
	 */
	@JsonIgnore
	@ApiModelProperty(value = "the id of the account if exists")
	private String accountId;
	
	/**
	 * Name of the service like facebook, youtube, periscope, twitch
	 */
	@ApiModelProperty(value = "the name of the service like Facebook, Youtube, Periscope, Twitch etc.")
	private String serviceName;
	
	/**
	 * User account, page account, etc.
	 */
	@ApiModelProperty(value = "the user account, page account, etc.")
	private String accountType;
	
	public SocialEndpointCredentials(String name, String serviceName, String authTimeInMillisecoonds, String expireTimeInSeconds, String tokenType, String accessToken, String refreshToken) {
		this.accountName = name;
		this.serviceName = serviceName;
		this.authTimeInMilliseconds = authTimeInMillisecoonds;
		this.expireTimeInSeconds = expireTimeInSeconds;
		this.tokenType = tokenType;
		this.accessToken = accessToken;
		this.refreshToken = refreshToken;
	}
	
	public SocialEndpointCredentials() {
	}

	public String getAccessToken() {
		return accessToken;
	}

	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}

	public String getRefreshToken() {
		return refreshToken;
	}

	public void setRefreshToken(String refreshToken) {
		this.refreshToken = refreshToken;
	}

	public String getTokenType() {
		return tokenType;
	}

	public void setTokenType(String tokenType) {
		this.tokenType = tokenType;
	}

	public String getExpireTimeInSeconds() {
		return expireTimeInSeconds;
	}

	public void setExpireTimeInSeconds(String expireTimeInSeconds) {
		this.expireTimeInSeconds = expireTimeInSeconds;
	}

	public String getAuthTimeInMilliseconds() {
		return authTimeInMilliseconds;
	}

	public void setAuthTimeInMilliseconds(String authTimeInMillisecoonds) {
		this.authTimeInMilliseconds = authTimeInMillisecoonds;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getAccountName() {
		return accountName;
	}

	public void setAccountName(String name) {
		this.accountName = name;
	}

	public String getServiceName() {
		return serviceName;
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	public String getAccountId() {
		return accountId;
	}

	public void setAccountId(String accountId) {
		this.accountId = accountId;
	}

	public String getAccountType() {
		return accountType;
	}

	public void setAccountType(String accountType) {
		this.accountType = accountType;
	}

}
