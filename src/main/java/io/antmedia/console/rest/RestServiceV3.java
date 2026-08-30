package io.antmedia.console.rest;

import java.time.Instant;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;

import io.antmedia.rest.JWTFilterV3;
import io.antmedia.rest.model.JWTGenerationRequest;
import io.antmedia.rest.model.JWTGenerationResponse;
import io.antmedia.rest.model.Result;
import io.antmedia.settings.ServerSettings;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

/**
 * System-level v3 management endpoints. Reachable under {@code /rest/v3} in the console
 * webapp and protected by the console AuthenticationFilter (admin only for writes).
 */
@Path("/v3")
public class RestServiceV3 extends CommonRestService {

	private static final String ISSUER = "ams";

	@Operation(description = "Generates a v3 REST JWT for the given user and scopes. Requires system admin. "
			+ "The token is signed with the server JWT secret and is not stored.")
	@POST
	@Path("/jwts")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response createJwt(JWTGenerationRequest request) {

		if (request == null) {
			return error("Request body is required");
		}

		ServerSettings serverSettings = getServerSettings();
		String secret = serverSettings != null ? serverSettings.getJwtServerSecretKey() : null;
		if (StringUtils.isBlank(secret)) {
			return error("JWT server secret is not set");
		}
		if (!JWTFilterV3.AUDIENCE_REST.equals(request.getType())) {
			return error("type must be '" + JWTFilterV3.AUDIENCE_REST + "'");
		}
		if (StringUtils.isBlank(request.getUserId())) {
			return error("user_id is required");
		}
		if (!getDataStore().doesUsernameExist(request.getUserId())) {
			return error("user_id does not exist");
		}
		if (request.getScopes() == null || request.getScopes().isEmpty()) {
			return error("at least one scope is required");
		}

		Long expiration = request.getExpiration();
		if (expiration != null && expiration <= Instant.now().getEpochSecond()) {
			return error("expiration must be a unix timestamp in the future");
		}

		JWTCreator.Builder builder = JWT.create()
				.withIssuer(ISSUER)
				.withSubject(request.getUserId())
				.withAudience(JWTFilterV3.AUDIENCE_REST)
				.withIssuedAt(new Date())
				.withClaim(JWTFilterV3.SCOPE_CLAIM, String.join(" ", request.getScopes()));
		if (expiration != null) {
			builder.withExpiresAt(new Date(expiration * 1000));
		}
		String jwt = builder.sign(Algorithm.HMAC256(secret));

		JWTGenerationResponse response = new JWTGenerationResponse();
		response.setSuccess(true);
		response.setJwt(jwt);
		return Response.status(Status.OK).entity(response).build();
	}

	private static Response error(String message) {
		return Response.status(Status.BAD_REQUEST).entity(new Result(false, message)).build();
	}
}
