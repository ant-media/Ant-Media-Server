package io.antmedia.rest.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Result of a v3 JWT generation request")
public class JWTGenerationResponse extends Result {

	@Schema(description = "The generated JWT, present when success is true.")
	private String jwt;

	public JWTGenerationResponse() {
		super(false);
	}

	public String getJwt() {
		return jwt;
	}

	public void setJwt(String jwt) {
		this.jwt = jwt;
	}
}
