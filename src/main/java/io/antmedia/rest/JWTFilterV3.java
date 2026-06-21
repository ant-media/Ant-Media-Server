package io.antmedia.rest;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;

import io.antmedia.rest.model.Result;
import io.antmedia.settings.ServerSettings;
import jakarta.annotation.Priority;
import jakarta.servlet.ServletContext;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.ext.Provider;

/**
 * v3 REST JWT authorization filter. Applied only to endpoints annotated with
 * {@link JwtV3Secured}. Implements the slice-1 subset of the v3 auth algorithm:
 * server-level gate, signature + claim validation and scope-based authorization.
 * Live user verification and owner_id checks are intentionally out of scope here.
 *
 * <p>Configured with the existing global Management settings
 * {@code server.jwtServerControlEnabled} and {@code server.jwtServerSecretKey}.
 */
@Provider
@JwtV3Secured
@Priority(Priorities.AUTHENTICATION)
public class JWTFilterV3 implements ContainerRequestFilter {

	private static final Logger logger = LoggerFactory.getLogger(JWTFilterV3.class);

	public static final String AUDIENCE_REST = "rest";
	public static final String SCOPE_CLAIM = "scope";
	public static final String BEARER_PREFIX = "Bearer";

	/** Request property holding the authenticated user id (JWT sub) for downstream resources. */
	public static final String AUTHENTICATED_USER_ID = "ams.v3.userId";

	private static final String PERMISSION_ADMIN = "admin";
	private static final String PERMISSION_USER = "user";
	private static final String RESOURCE_SYSTEM = "system";
	private static final String RESOURCE_APPLICATION = "application";

	@Context
	private ServletContext servletContext;

	@Override
	public void filter(ContainerRequestContext requestContext) {

		ServerSettings serverSettings = getServerSettings();

		// 1. Gate: v3 is default-closed and requires the global JWT control + secret.
		if (serverSettings == null || !serverSettings.isJwtServerControlEnabled()
				|| StringUtils.isBlank(serverSettings.getJwtServerSecretKey())) {
			abort(requestContext, Status.UNAUTHORIZED, "v3 REST JWT authorization is not enabled");
			return;
		}

		// 2. Extract bearer token.
		String token = extractToken(requestContext.getHeaderString(HttpHeaders.AUTHORIZATION));
		if (token == null) {
			abort(requestContext, Status.UNAUTHORIZED, "Missing JWT token");
			return;
		}

		// 3. & 4. Validate signature, audience, expiration and required claims.
		DecodedJWT jwt = verify(token, serverSettings.getJwtServerSecretKey());
		if (jwt == null) {
			abort(requestContext, Status.UNAUTHORIZED, "Invalid JWT token");
			return;
		}

		if (StringUtils.isBlank(jwt.getSubject())) {
			abort(requestContext, Status.UNAUTHORIZED, "JWT is missing 'sub'");
			return;
		}

		Claim scopeClaim = jwt.getClaim(SCOPE_CLAIM);
		if (scopeClaim.isMissing() || scopeClaim.isNull() || StringUtils.isBlank(scopeClaim.asString())) {
			abort(requestContext, Status.UNAUTHORIZED, "JWT is missing 'scope'");
			return;
		}

		// 5. Authorize: create broadcast is a write operation.
		String appName = getApplicationName();
		if (!hasWriteAccess(scopeClaim.asString(), appName)) {
			abort(requestContext, Status.FORBIDDEN, "JWT scope does not grant write access to this application");
			return;
		}

		// Hand the already-parsed user id to downstream resources so they don't re-parse the token.
		requestContext.setProperty(AUTHENTICATED_USER_ID, jwt.getSubject());
	}

	private DecodedJWT verify(String token, String secret) {
		try {
			Algorithm algorithm = Algorithm.HMAC256(secret);
			return JWT.require(algorithm)
					.withAudience(AUDIENCE_REST)
					.build()
					.verify(token);
		}
		catch (JWTVerificationException e) {
			logger.warn("v3 JWT verification failed: {}", e.getMessage());
			return null;
		}
	}

	/**
	 * Returns true if any scope in the space-separated claim grants write access
	 * (admin or user) to the system or to the given application. read_only never
	 * grants write.
	 */
	public static boolean hasWriteAccess(String scopeClaim, String appName) {
		if (StringUtils.isBlank(scopeClaim)) {
			return false;
		}
		for (String scope : scopeClaim.trim().split("\\s+")) {
			String[] parts = scope.split(":");
			if (parts.length < 2) {
				continue;
			}
			String permission = parts[0];
			if (!PERMISSION_ADMIN.equals(permission) && !PERMISSION_USER.equals(permission)) {
				continue;
			}
			if (RESOURCE_SYSTEM.equals(parts[1])) {
				return true;
			}
			if (RESOURCE_APPLICATION.equals(parts[1]) && parts.length >= 3 && parts[2].equals(appName)) {
				return true;
			}
		}
		return false;
	}

	private static String extractToken(String header) {
		if (StringUtils.isBlank(header)) {
			return null;
		}
		String token = header;
		if (token.toLowerCase().startsWith(BEARER_PREFIX.toLowerCase())) {
			token = token.substring(BEARER_PREFIX.length()).trim();
		}
		return StringUtils.isBlank(token) ? null : token;
	}

	private String getApplicationName() {
		// Context path is "/<appName>" for an AMS application webapp.
		return StringUtils.strip(servletContext.getContextPath(), "/");
	}

	private ServerSettings getServerSettings() {
		WebApplicationContext context = WebApplicationContextUtils.getWebApplicationContext(servletContext);
		return context != null ? (ServerSettings) context.getBean(ServerSettings.BEAN_NAME) : null;
	}

	private static void abort(ContainerRequestContext requestContext, Status status, String message) {
		requestContext.abortWith(Response.status(status)
				.entity(new Result(false, message))
				.type(MediaType.APPLICATION_JSON)
				.build());
	}
}
