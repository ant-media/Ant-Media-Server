package io.antmedia.test.console;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;

import io.antmedia.console.datastore.AbstractConsoleDataStore;
import io.antmedia.console.rest.RestServiceV3;
import io.antmedia.rest.model.JWTGenerationRequest;
import io.antmedia.rest.model.JWTGenerationResponse;
import io.antmedia.settings.ServerSettings;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

public class RestServiceV3UnitTest {

	private static final String SECRET = "test-secret-key";

	private RestServiceV3 service;
	private AbstractConsoleDataStore store;

	@Before
	public void before() {
		service = Mockito.spy(new RestServiceV3());
		store = mock(AbstractConsoleDataStore.class);
		service.setDataStore(store);
		when(store.doesUsernameExist("alice")).thenReturn(true);

		ServerSettings settings = new ServerSettings();
		settings.setJwtServerSecretKey(SECRET);
		Mockito.doReturn(settings).when(service).getServerSettings();
	}

	private static JWTGenerationRequest request(String type, String userId, List<String> scopes, Long expiration) {
		JWTGenerationRequest request = new JWTGenerationRequest();
		request.setType(type);
		request.setUserId(userId);
		request.setScopes(scopes);
		request.setExpiration(expiration);
		return request;
	}

	@Test
	public void testGeneratesValidToken() {
		Response response = service.createJwt(request("rest", "alice", Arrays.asList("admin:system"), null));
		assertEquals(Status.OK.getStatusCode(), response.getStatus());

		JWTGenerationResponse body = (JWTGenerationResponse) response.getEntity();
		assertTrue(body.isSuccess());

		DecodedJWT jwt = JWT.require(Algorithm.HMAC256(SECRET)).withAudience("rest").build().verify(body.getJwt());
		assertEquals("alice", jwt.getSubject());
		assertEquals("ams", jwt.getIssuer());
		assertEquals("admin:system", jwt.getClaim("scope").asString());
	}

	@Test
	public void testScopesJoinedWithSpace() {
		Response response = service.createJwt(request("rest", "alice", Arrays.asList("user:system", "read_only:application:live"), null));
		JWTGenerationResponse body = (JWTGenerationResponse) response.getEntity();
		DecodedJWT jwt = JWT.require(Algorithm.HMAC256(SECRET)).build().verify(body.getJwt());
		assertEquals("user:system read_only:application:live", jwt.getClaim("scope").asString());
	}

	@Test
	public void testExpirationApplied() {
		long exp = Instant.now().getEpochSecond() + 3600;
		Response response = service.createJwt(request("rest", "alice", Arrays.asList("admin:system"), exp));
		JWTGenerationResponse body = (JWTGenerationResponse) response.getEntity();
		DecodedJWT jwt = JWT.require(Algorithm.HMAC256(SECRET)).build().verify(body.getJwt());
		assertEquals(exp, jwt.getExpiresAt().toInstant().getEpochSecond());
	}

	@Test
	public void testNullBodyRejected() {
		assertEquals(Status.BAD_REQUEST.getStatusCode(), service.createJwt(null).getStatus());
	}

	@Test
	public void testWrongTypeRejected() {
		Response response = service.createJwt(request("management", "alice", Arrays.asList("admin:system"), null));
		assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
	}

	@Test
	public void testMissingUserIdRejected() {
		Response response = service.createJwt(request("rest", null, Arrays.asList("admin:system"), null));
		assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
	}

	@Test
	public void testUnknownUserRejected() {
		Response response = service.createJwt(request("rest", "ghost", Arrays.asList("admin:system"), null));
		assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
	}

	@Test
	public void testEmptyScopesRejected() {
		Response response = service.createJwt(request("rest", "alice", Arrays.asList(), null));
		assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
	}

	@Test
	public void testPastExpirationRejected() {
		long past = Instant.now().getEpochSecond() - 10;
		Response response = service.createJwt(request("rest", "alice", Arrays.asList("admin:system"), past));
		assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
	}

	@Test
	public void testSecretNotSetRejected() {
		Mockito.doReturn(new ServerSettings()).when(service).getServerSettings();
		Response response = service.createJwt(request("rest", "alice", Arrays.asList("admin:system"), null));
		assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
	}
}
