package io.antmedia.test.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.red5.server.scope.Scope;

import io.antmedia.AppSettings;
import io.antmedia.datastore.db.InMemoryDataStore;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.rest.BroadcastRestServiceV3;
import io.antmedia.rest.JWTFilterV3;
import io.antmedia.settings.ServerSettings;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Response;

public class BroadcastRestServiceV3UnitTest {

	private BroadcastRestServiceV3 service;

	@Before
	public void before() {
		service = new BroadcastRestServiceV3();
		Scope scope = mock(Scope.class);
		when(scope.getName()).thenReturn("scope");
		service.setScope(scope);
		service.setAppSettings(new AppSettings());
		service.setServerSettings(mock(ServerSettings.class));
		service.setDataStore(new InMemoryDataStore("v3test"));
	}

	private ContainerRequestContext requestWithUser(String userId) {
		ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
		when(requestContext.getProperty(JWTFilterV3.AUTHENTICATED_USER_ID)).thenReturn(userId);
		return requestContext;
	}

	@Test
	public void testOwnerStampedFromAuthenticatedUser() {
		Broadcast created = (Broadcast) service
				.createBroadcast(new Broadcast(null, "owner-default"), false, requestWithUser("testuser"))
				.getEntity();
		assertEquals("testuser", created.getOwnerId());
	}

	@Test
	public void testExplicitOwnerIsPreserved() {
		Broadcast broadcast = new Broadcast(null, "owner-explicit");
		broadcast.setOwnerId("alice");
		Broadcast created = (Broadcast) service
				.createBroadcast(broadcast, false, requestWithUser("testuser"))
				.getEntity();
		assertEquals("alice", created.getOwnerId());
	}

	@Test
	public void testNoOwnerWhenPrincipalMissing() {
		Broadcast created = (Broadcast) service
				.createBroadcast(new Broadcast(null, "no-owner"), false, requestWithUser(null))
				.getEntity();
		assertNull(created.getOwnerId());
	}

	@Test
	public void testCreatePersistsBroadcast() {
		Broadcast created = (Broadcast) service
				.createBroadcast(new Broadcast(null, "persist"), false, requestWithUser("testuser"))
				.getEntity();

		Broadcast stored = service.getDataStore().get(created.getStreamId());
		assertEquals("testuser", stored.getOwnerId());
	}

	@Test
	public void testDuplicateStreamIdRejected() throws Exception {
		Broadcast first = (Broadcast) service
				.createBroadcast(new Broadcast(null, "dup"), false, requestWithUser("testuser"))
				.getEntity();

		Broadcast second = new Broadcast(null, "dup");
		second.setStreamId(first.getStreamId());
		Response response = service.createBroadcast(second, false, requestWithUser("testuser"));
		assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
	}
}
