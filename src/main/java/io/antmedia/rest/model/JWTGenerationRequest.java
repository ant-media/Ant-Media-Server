package io.antmedia.rest.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request body to generate a v3 REST JWT")
public class JWTGenerationRequest {

	@Schema(description = "Token type. Only \"rest\" is accepted.")
	private String type;

	@Schema(description = "Reserved for future use.")
	private String version;

	@Schema(description = "Id (email) of the user the token is issued for.")
	@JsonProperty("user_id")
	private String userId;

	@Schema(description = "Optional expiration as a unix timestamp in seconds.")
	private Long expiration;

	@Schema(description = "Scopes granted to the token, e.g. \"admin:system\" or \"user:application:live\".")
	private List<String> scopes;

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public Long getExpiration() {
		return expiration;
	}

	public void setExpiration(Long expiration) {
		this.expiration = expiration;
	}

	public List<String> getScopes() {
		return scopes;
	}

	public void setScopes(List<String> scopes) {
		this.scopes = scopes;
	}
}
