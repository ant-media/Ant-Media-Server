package io.antmedia.test.filter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.List;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;

import io.antmedia.filter.JWTFilterV3;
import io.antmedia.settings.ServerSettings;
import jakarta.servlet.ServletContext;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

/**
 * Exercises the full decision tree of {@link JWTFilterV3#filter} with mocked
 * request/context/settings. Tokens are minted with the same java-jwt library a
 * real client would use.
 */
public class JWTFilterV3IntegrationTest {

	private static final String SECRET = "test-secret-key";
	private static final String APP = "LiveApp";

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
	 * Runs the filter and returns the abort Response, or null if the request was allowed through.
	 */
	private Response runFilter(boolean controlEnabled, String settingsSecret, String authHeader) {
		ServletContext servletContext = mock(ServletContext.class);
		when(servletContext.getContextPath()).thenReturn("/" + APP);

		WebApplicationContext webContext = mock(WebApplicationContext.class);
		ServerSettings settings = mock(ServerSettings.class);
		when(settings.isJwtServerControlEnabled()).thenReturn(controlEnabled);
		when(settings.getJwtServerSecretKey()).thenReturn(settingsSecret);
		when(webContext.getBean(ServerSettings.BEAN_NAME)).thenReturn(settings);

		JWTFilterV3 filter = new JWTFilterV3();
		ReflectionTestUtils.setField(filter, "servletContext", servletContext);

		ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
		when(requestContext.getHeaderString(HttpHeaders.AUTHORIZATION)).thenReturn(authHeader);

		try (MockedStatic<WebApplicationContextUtils> mocked = mockStatic(WebApplicationContextUtils.class)) {
			mocked.when(() -> WebApplicationContextUtils.getWebApplicationContext(servletContext)).thenReturn(webContext);
			filter.filter(requestContext);
		}

		ArgumentCaptor<Response> captor = ArgumentCaptor.forClass(Response.class);
		verify(requestContext, atLeast(0)).abortWith(captor.capture());
		List<Response> aborts = captor.getAllValues();
		return aborts.isEmpty() ? null : aborts.get(0);
	}

	private Response runWithToken(String authHeader) {
		return runFilter(true, SECRET, authHeader);
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
		assertStatus(Status.UNAUTHORIZED, runFilter(false, SECRET, "Bearer " + validToken("admin:system")));
	}

	@Test
	public void testBlankSecretUnauthorized() {
		assertStatus(Status.UNAUTHORIZED, runFilter(true, "", "Bearer " + validToken("admin:system")));
	}
}
