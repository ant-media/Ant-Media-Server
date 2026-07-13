package io.antmedia.test.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;

import io.antmedia.datastore.db.types.User;
import io.antmedia.datastore.db.types.UserType;
import io.antmedia.rest.ConsoleUserResolver;
import io.antmedia.rest.JWTFilterV3;
import io.antmedia.settings.ServerSettings;
import jakarta.servlet.ServletContext;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

/**
 * Tests {@link JWTFilterV3}: the pure scope-authorization helpers and the full decision tree
 * of {@code filter()} (with mocked request/context/settings). Tokens are minted with the same
 * java-jwt library a real client would use.
 */
public class JWTFilterV3Test {

	private static final String SECRET = "test-secret-key";
	private static final String APP = "LiveApp";

	// ---- scope authorization helpers (pure logic) ----

	@Test
	public void testSystemScopeGrantsWriteToAnyApp() {
		assertTrue(JWTFilterV3.hasWriteAccess("admin:system", "live"));
		assertTrue(JWTFilterV3.hasWriteAccess("user:system", "anyApp"));
	}

	@Test
	public void testApplicationScopeMatchesOnlyItsApp() {
		assertTrue(JWTFilterV3.hasWriteAccess("user:application:live", "live"));
		assertTrue(JWTFilterV3.hasWriteAccess("admin:application:live", "live"));
		assertFalse(JWTFilterV3.hasWriteAccess("user:application:live", "other"));
	}

	@Test
	public void testReadOnlyNeverGrantsWrite() {
		assertFalse(JWTFilterV3.hasWriteAccess("read_only:system", "live"));
		assertFalse(JWTFilterV3.hasWriteAccess("read_only:application:live", "live"));
	}

	@Test
	public void testMultipleScopesAreUnionOfGrants() {
		assertTrue(JWTFilterV3.hasWriteAccess("read_only:application:app2 user:system", "live"));
		assertTrue(JWTFilterV3.hasWriteAccess("admin:application:test_app user:application:live", "live"));
		assertFalse(JWTFilterV3.hasWriteAccess("read_only:system read_only:application:live", "live"));
	}

	@Test
	public void testInvalidOrEmptyScopeDenied() {
		assertFalse(JWTFilterV3.hasWriteAccess(null, "live"));
		assertFalse(JWTFilterV3.hasWriteAccess("", "live"));
		assertFalse(JWTFilterV3.hasWriteAccess("garbage", "live"));
		assertFalse(JWTFilterV3.hasWriteAccess("admin", "live"));
		assertFalse(JWTFilterV3.hasWriteAccess("superuser:system", "live"));
	}

	@Test
	public void testAdminAccessOnlyForAdminScopes() {
		assertTrue(JWTFilterV3.hasAdminAccess("admin:system", "live"));
		assertTrue(JWTFilterV3.hasAdminAccess("admin:application:live", "live"));
		assertTrue(JWTFilterV3.hasAdminAccess("user:system admin:application:live", "live"));
	}

	@Test
	public void testUserAndReadOnlyAreNotAdmin() {
		assertFalse(JWTFilterV3.hasAdminAccess("user:system", "live"));
		assertFalse(JWTFilterV3.hasAdminAccess("user:application:live", "live"));
		assertFalse(JWTFilterV3.hasAdminAccess("read_only:system", "live"));
		assertFalse(JWTFilterV3.hasAdminAccess("admin:application:other", "live"));
		assertFalse(JWTFilterV3.hasAdminAccess(null, "live"));
	}

	@Test
	public void testReadAccessGrantedForAllRoles() {
		assertTrue(JWTFilterV3.hasReadAccess("read_only:system", "live"));
		assertTrue(JWTFilterV3.hasReadAccess("read_only:application:live", "live"));
		assertTrue(JWTFilterV3.hasReadAccess("user:application:live", "live"));
		assertTrue(JWTFilterV3.hasReadAccess("admin:system", "live"));
	}

	@Test
	public void testReadAccessDeniedForOtherAppOrEmpty() {
		assertFalse(JWTFilterV3.hasReadAccess("read_only:application:other", "live"));
		assertFalse(JWTFilterV3.hasReadAccess(null, "live"));
		assertFalse(JWTFilterV3.hasReadAccess("garbage", "live"));
	}

	private static User user(Map<String, String> appNameUserType) {
		User u = new User();
		u.setAppNameUserType(appNameUserType);
		return u;
	}

	private static User legacyUser(String scope, UserType type) {
		User u = new User();
		u.setScope(scope);
		u.setUserType(type);
		return u;
	}

	@Test
	public void testScopeFromUserSystemAdmin() {
		assertEquals("admin:system", JWTFilterV3.scopeFromUser(user(Map.of("system", "ADMIN"))));
	}

	@Test
	public void testScopeFromUserAppScoped() {
		assertEquals("user:application:live", JWTFilterV3.scopeFromUser(user(Map.of("live", "USER"))));
		assertEquals("read_only:application:live", JWTFilterV3.scopeFromUser(user(Map.of("live", "READ_ONLY"))));
	}

	@Test
	public void testScopeFromUserMultipleApps() {
		// map order isn't guaranteed — assert on derived access, not the exact string
		String scope = JWTFilterV3.scopeFromUser(user(Map.of("app1", "USER", "app2", "READ_ONLY")));
		assertTrue(JWTFilterV3.hasWriteAccess(scope, "app1"));
		assertFalse(JWTFilterV3.hasWriteAccess(scope, "app2"));
		assertTrue(JWTFilterV3.hasReadAccess(scope, "app2"));
		assertFalse(JWTFilterV3.hasReadAccess(scope, "app3"));
	}

	@Test
	public void testScopeFromUserLegacyForm() {
		assertEquals("admin:system", JWTFilterV3.scopeFromUser(legacyUser("system", UserType.ADMIN)));
		assertEquals("user:application:live", JWTFilterV3.scopeFromUser(legacyUser("live", UserType.USER)));
	}

	@Test
	public void testScopeFromUserLegacyScopeTakesPrecedence() {
		// legacy scope wins over the map
		User u = legacyUser("system", UserType.ADMIN);
		u.setAppNameUserType(Map.of("live", "READ_ONLY"));
		assertEquals("admin:system", JWTFilterV3.scopeFromUser(u));
	}

	@Test
	public void testScopeFromUserEmptyOrUnknownDenied() {
		assertEquals("", JWTFilterV3.scopeFromUser(null));
		assertEquals("", JWTFilterV3.scopeFromUser(new User()));
		assertEquals("", JWTFilterV3.scopeFromUser(user(Map.of("live", "SUPERUSER"))));
		// derived empty scope grants nothing
		assertFalse(JWTFilterV3.hasReadAccess(JWTFilterV3.scopeFromUser(new User()), "live"));
	}

	// ---- filter() decision tree ----

	private static String token(String aud, String sub, String scope, Date exp, String secret) {
		JWTCreator.Builder builder = JWT.create();
		if (aud != null) builder.withAudience(aud);
		if (sub != null) builder.withSubject(sub);
		if (scope != null) builder.withClaim("scope", scope);
		if (exp != null) builder.withExpiresAt(exp);
		return builder.sign(Algorithm.HMAC256(secret));
	}

	private static String validToken(String scope) {
		return token("rest", "testuser", scope, new Date(System.currentTimeMillis() + 3600_000), SECRET);
	}

	/**
	 * Runs the filter and returns the abort Response, or null if allowed through. The live user
	 * (step 3) mirrors the token's scope, so a valid token maps to a user with the same role.
	 */
	private Response runFilter(boolean controlEnabled, String settingsSecret, String authHeader, String httpMethod) {
		return runFilter(controlEnabled, settingsSecret, authHeader, httpMethod, userFromToken(authHeader));
	}

	private Response runFilter(boolean controlEnabled, String settingsSecret, String authHeader, String httpMethod, User liveUser) {
		ServletContext servletContext = mock(ServletContext.class);
		when(servletContext.getContextPath()).thenReturn("/" + APP);

		WebApplicationContext webContext = mock(WebApplicationContext.class);
		ServerSettings settings = mock(ServerSettings.class);
		when(settings.isJwtServerControlEnabled()).thenReturn(controlEnabled);
		when(settings.getJwtServerSecretKey()).thenReturn(settingsSecret);
		when(webContext.getBean(ServerSettings.BEAN_NAME)).thenReturn(settings);

		ConsoleUserResolver resolver = mock(ConsoleUserResolver.class);
		when(resolver.getUser(any())).thenReturn(liveUser);

		JWTFilterV3 filter = new JWTFilterV3();
		ReflectionTestUtils.setField(filter, "servletContext", servletContext);
		ReflectionTestUtils.setField(filter, "userResolver", resolver);

		ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
		when(requestContext.getHeaderString(HttpHeaders.AUTHORIZATION)).thenReturn(authHeader);
		when(requestContext.getMethod()).thenReturn(httpMethod);

		try (MockedStatic<WebApplicationContextUtils> mocked = mockStatic(WebApplicationContextUtils.class)) {
			mocked.when(() -> WebApplicationContextUtils.getWebApplicationContext(servletContext)).thenReturn(webContext);
			filter.filter(requestContext);
		}

		ArgumentCaptor<Response> captor = ArgumentCaptor.forClass(Response.class);
		verify(requestContext, atLeast(0)).abortWith(captor.capture());
		List<Response> aborts = captor.getAllValues();
		return aborts.isEmpty() ? null : aborts.get(0);
	}

	/** Builds a user whose live role reproduces the given scope string (inverse of scopeFromUser). */
	private static User userForScope(String scope) {
		Map<String, String> map = new HashMap<>();
		if (scope != null) {
			for (String tok : scope.trim().split("\\s+")) {
				String[] p = tok.split(":");
				String type = p.length >= 1 ? typeName(p[0]) : null;
				if (p.length < 2 || type == null) {
					continue;
				}
				if ("system".equals(p[1])) {
					map.put("system", type);
				}
				else if ("application".equals(p[1]) && p.length >= 3) {
					map.put(p[2], type);
				}
			}
		}
		return user(map);
	}

	private static String typeName(String permission) {
		switch (permission) {
			case "admin": return "ADMIN";
			case "user": return "USER";
			case "read_only": return "READ_ONLY";
			default: return null;
		}
	}

	/** Decodes the token in the header and mirrors its scope to a user, or null if undecodable. */
	private static User userFromToken(String authHeader) {
		if (authHeader == null) {
			return null;
		}
		String t = authHeader.toLowerCase().startsWith("bearer") ? authHeader.substring("Bearer".length()).trim() : authHeader;
		try {
			return userForScope(JWT.decode(t).getClaim("scope").asString());
		}
		catch (Exception e) {
			return null;
		}
	}

	// write request (POST) unless stated otherwise
	private Response runWithToken(String authHeader) {
		return runFilter(true, SECRET, authHeader, "POST");
	}

	private Response runGet(String authHeader) {
		return runFilter(true, SECRET, authHeader, "GET");
	}

	private static void assertAllowed(Response response) {
		assertNull("request should have been allowed through", response);
	}

	private static void assertStatus(Status expected, Response response) {
		assertNotNull("request should have been aborted", response);
		assertEquals(expected.getStatusCode(), response.getStatus());
	}

	@Test
	public void testAdminSystemAllowed() {
		assertAllowed(runWithToken("Bearer " + validToken("admin:system")));
	}

	@Test
	public void testUserApplicationAllowed() {
		assertAllowed(runWithToken("Bearer " + validToken("user:application:" + APP)));
	}

	@Test
	public void testBearerPrefixOptional() {
		assertAllowed(runWithToken(validToken("admin:system")));
	}

	@Test
	public void testReadOnlyForbidden() {
		assertStatus(Status.FORBIDDEN, runWithToken("Bearer " + validToken("read_only:system")));
	}

	@Test
	public void testUserForOtherApplicationForbidden() {
		assertStatus(Status.FORBIDDEN, runWithToken("Bearer " + validToken("user:application:otherApp")));
	}

	@Test
	public void testMissingHeaderUnauthorized() {
		assertStatus(Status.UNAUTHORIZED, runWithToken(null));
	}

	@Test
	public void testExpiredTokenUnauthorized() {
		String expired = token("rest", "testuser", "admin:system", new Date(System.currentTimeMillis() - 10_000), SECRET);
		assertStatus(Status.UNAUTHORIZED, runWithToken("Bearer " + expired));
	}

	@Test
	public void testWrongAudienceUnauthorized() {
		String wrongAud = token("management", "testuser", "admin:system", new Date(System.currentTimeMillis() + 3600_000), SECRET);
		assertStatus(Status.UNAUTHORIZED, runWithToken("Bearer " + wrongAud));
	}

	@Test
	public void testMissingSubUnauthorized() {
		String noSub = token("rest", null, "admin:system", new Date(System.currentTimeMillis() + 3600_000), SECRET);
		assertStatus(Status.UNAUTHORIZED, runWithToken("Bearer " + noSub));
	}

	@Test
	public void testMissingScopeUnauthorized() {
		String noScope = token("rest", "testuser", null, new Date(System.currentTimeMillis() + 3600_000), SECRET);
		assertStatus(Status.UNAUTHORIZED, runWithToken("Bearer " + noScope));
	}

	@Test
	public void testBadSignatureUnauthorized() {
		String wrongSecret = token("rest", "testuser", "admin:system", new Date(System.currentTimeMillis() + 3600_000), "another-secret");
		assertStatus(Status.UNAUTHORIZED, runWithToken("Bearer " + wrongSecret));
	}

	@Test
	public void testControlDisabledUnauthorized() {
		assertStatus(Status.UNAUTHORIZED, runFilter(false, SECRET, "Bearer " + validToken("admin:system"), "POST"));
	}

	@Test
	public void testBlankSecretUnauthorized() {
		assertStatus(Status.UNAUTHORIZED, runFilter(true, "", "Bearer " + validToken("admin:system"), "POST"));
	}

	@Test
	public void testReadOnlyTokenAllowedForGet() {
		assertAllowed(runGet("Bearer " + validToken("read_only:system")));
		assertAllowed(runGet("Bearer " + validToken("read_only:application:" + APP)));
	}

	@Test
	public void testReadOnlyTokenStillForbiddenForWrite() {
		assertStatus(Status.FORBIDDEN, runWithToken("Bearer " + validToken("read_only:system")));
	}

	@Test
	public void testGetForbiddenForOtherApp() {
		assertStatus(Status.FORBIDDEN, runGet("Bearer " + validToken("read_only:application:otherApp")));
	}

	@Test
	public void testNoAppContextUnauthorized() {
		// Web application context not available (e.g. app still initializing) -> deny.
		ServletContext servletContext = mock(ServletContext.class);
		when(servletContext.getContextPath()).thenReturn("/" + APP);

		JWTFilterV3 filter = new JWTFilterV3();
		ReflectionTestUtils.setField(filter, "servletContext", servletContext);

		ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
		when(requestContext.getHeaderString(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer " + validToken("admin:system"));

		try (MockedStatic<WebApplicationContextUtils> mocked = mockStatic(WebApplicationContextUtils.class)) {
			mocked.when(() -> WebApplicationContextUtils.getWebApplicationContext(servletContext)).thenReturn(null);
			filter.filter(requestContext);
		}

		ArgumentCaptor<Response> captor = ArgumentCaptor.forClass(Response.class);
		verify(requestContext).abortWith(captor.capture());
		assertEquals(Status.UNAUTHORIZED.getStatusCode(), captor.getValue().getStatus());
	}

	@Test
	public void testAdminTokenSetsUserIdAndAdminAccess() {
		ContainerRequestContext requestContext = runSuccess("owner-123", "admin:system");
		verify(requestContext, never()).abortWith(any());
		verify(requestContext).setProperty(JWTFilterV3.AUTHENTICATED_USER_ID, "owner-123");
		verify(requestContext).setProperty(JWTFilterV3.ADMIN_ACCESS, true);
	}

	@Test
	public void testUserTokenSetsUserIdButNotAdminAccess() {
		ContainerRequestContext requestContext = runSuccess("owner-123", "user:application:" + APP);
		verify(requestContext, never()).abortWith(any());
		verify(requestContext).setProperty(JWTFilterV3.AUTHENTICATED_USER_ID, "owner-123");
		verify(requestContext).setProperty(JWTFilterV3.ADMIN_ACCESS, false);
	}

	private ContainerRequestContext runSuccess(String sub, String scope) {
		return runSuccess(sub, scope, userForScope(scope));
	}

	private ContainerRequestContext runSuccess(String sub, String scope, User liveUser) {
		String token = token("rest", sub, scope, new Date(System.currentTimeMillis() + 3600_000), SECRET);

		ServletContext servletContext = mock(ServletContext.class);
		when(servletContext.getContextPath()).thenReturn("/" + APP);
		WebApplicationContext webContext = mock(WebApplicationContext.class);
		ServerSettings settings = mock(ServerSettings.class);
		when(settings.isJwtServerControlEnabled()).thenReturn(true);
		when(settings.getJwtServerSecretKey()).thenReturn(SECRET);
		when(webContext.getBean(ServerSettings.BEAN_NAME)).thenReturn(settings);

		ConsoleUserResolver resolver = mock(ConsoleUserResolver.class);
		when(resolver.getUser(any())).thenReturn(liveUser);

		JWTFilterV3 filter = new JWTFilterV3();
		ReflectionTestUtils.setField(filter, "servletContext", servletContext);
		ReflectionTestUtils.setField(filter, "userResolver", resolver);

		ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
		when(requestContext.getHeaderString(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer " + token);

		try (MockedStatic<WebApplicationContextUtils> mocked = mockStatic(WebApplicationContextUtils.class)) {
			mocked.when(() -> WebApplicationContextUtils.getWebApplicationContext(servletContext)).thenReturn(webContext);
			filter.filter(requestContext);
		}
		return requestContext;
	}

	// ---- step 3: live user verification ----

	@Test
	public void testDeletedUserForbidden() {
		// valid token, but the user no longer exists in the console store
		assertStatus(Status.FORBIDDEN, runFilter(true, SECRET, "Bearer " + validToken("admin:system"), "POST", null));
	}

	@Test
	public void testDowngradedUserForbiddenForWrite() {
		// token still says admin:system, but the live user is only read_only now
		User downgraded = user(Map.of("system", "READ_ONLY"));
		assertStatus(Status.FORBIDDEN, runFilter(true, SECRET, "Bearer " + validToken("admin:system"), "POST", downgraded));
	}

	@Test
	public void testDowngradedAdminLosesAdminAccess() {
		// token says admin:system, but the live user is only a USER: write is allowed, admin is not
		User demoted = user(Map.of("system", "USER"));
		ContainerRequestContext requestContext = runSuccess("owner-123", "admin:system", demoted);
		verify(requestContext, never()).abortWith(any());
		verify(requestContext).setProperty(JWTFilterV3.AUTHENTICATED_USER_ID, "owner-123");
		verify(requestContext).setProperty(JWTFilterV3.ADMIN_ACCESS, false);
	}
}
