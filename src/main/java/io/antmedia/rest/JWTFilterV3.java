package io.antmedia.rest;

import org.apache.commons.lang3.ArrayUtils;
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
import jakarta.ws.rs.HttpMethod;
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
 * JWT authorization filter for the v3 REST API. Runs only on endpoints annotated with
 * {@link JwtV3Secured}. It validates the bearer token (signature, audience, expiry and the
 * required claims) and checks that the token scopes grant the requested access to the
 * current application.
 *
 * <p>Uses the global settings {@code server.jwtServerControlEnabled} and
 * {@code server.jwtServerSecretKey}. Access is denied while JWT control is disabled.
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

	/** Request property (Boolean) telling resources whether the token has admin access to this app. */
	public static final String ADMIN_ACCESS = "ams.v3.adminAccess";

	private static final String PERMISSION_ADMIN = "admin";
	private static final String PERMISSION_USER = "user";
	private static final String PERMISSION_READ_ONLY = "read_only";
	private static final String RESOURCE_SYSTEM = "system";
	private static final String RESOURCE_APPLICATION = "application";

	@Context
	private ServletContext servletContext;

	@Override
	public void filter(ContainerRequestContext requestContext) {

		ServerSettings serverSettings = getServerSettings();

		// Deny unless JWT control is on and a secret is configured.
		if (serverSettings == null || !serverSettings.isJwtServerControlEnabled()
				|| StringUtils.isBlank(serverSettings.getJwtServerSecretKey())) {
			abort(requestContext, Status.UNAUTHORIZED, "v3 REST JWT authorization is not enabled");
			return;
		}

		String token = extractToken(requestContext.getHeaderString(HttpHeaders.AUTHORIZATION));
		if (token == null) {
			abort(requestContext, Status.UNAUTHORIZED, "Missing JWT token");
			return;
		}

		// Verify signature, audience and expiry, then require the sub and scope claims.
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

		// GET is a read, anything else is a write.
		String appName = getApplicationName();
		boolean granted = HttpMethod.GET.equals(requestContext.getMethod())
				? hasReadAccess(scopeClaim.asString(), appName)
				: hasWriteAccess(scopeClaim.asString(), appName);
		if (!granted) {
			abort(requestContext, Status.FORBIDDEN, "JWT scope does not grant access to this application");
			return;
		}

		// Hand the already-parsed identity/role to downstream resources so they don't re-parse the token.
		requestContext.setProperty(AUTHENTICATED_USER_ID, jwt.getSubject());
		requestContext.setProperty(ADMIN_ACCESS, hasAdminAccess(scopeClaim.asString(), appName));
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
			logger.debug("v3 JWT verification failed: {}", e.getMessage());
			return null;
		}
	}

	/**
	 * Returns true if any scope in the space-separated claim grants write access
	 * (admin or user) to the system or to the given application. read_only never
	 * grants write.
	 */
	public static boolean hasWriteAccess(String scopeClaim, String appName) {
		return hasAccess(scopeClaim, appName, PERMISSION_ADMIN, PERMISSION_USER);
	}

	/**
	 * Returns true if any scope grants admin access to the system or the given application.
	 */
	public static boolean hasAdminAccess(String scopeClaim, String appName) {
		return hasAccess(scopeClaim, appName, PERMISSION_ADMIN);
	}

	/**
	 * Returns true if any scope grants read access (any role) to the system or the given application.
	 */
	public static boolean hasReadAccess(String scopeClaim, String appName) {
		return hasAccess(scopeClaim, appName, PERMISSION_ADMIN, PERMISSION_USER, PERMISSION_READ_ONLY);
	}

	private static boolean hasAccess(String scopeClaim, String appName, String... allowedPermissions) {
		if (StringUtils.isBlank(scopeClaim)) {
			return false;
		}
		for (String scope : scopeClaim.trim().split("\\s+")) {
			String[] parts = scope.split(":");
			if (parts.length < 2 || !ArrayUtils.contains(allowedPermissions, parts[0])) {
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
