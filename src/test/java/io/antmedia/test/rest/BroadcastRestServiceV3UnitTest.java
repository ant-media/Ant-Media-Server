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
import io.antmedia.datastore.db.types.BroadcastUpdate;
import io.antmedia.rest.BroadcastRestServiceV3;
import io.antmedia.rest.JWTFilterV3;
import io.antmedia.settings.ServerSettings;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

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

	private ContainerRequestContext request(String userId, boolean admin) {
		ContainerRequestContext requestContext = requestWithUser(userId);
		when(requestContext.getProperty(JWTFilterV3.ADMIN_ACCESS)).thenReturn(admin);
		return requestContext;
	}

	/** Creates a broadcast owned by the given user and returns its stream id. */
	private String createOwnedBroadcast(String owner) {
		Broadcast created = (Broadcast) service
				.createBroadcast(new Broadcast(null, "asset"), false, requestWithUser(owner))
				.getEntity();
		return created.getStreamId();
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
		assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
	}

	// ---- update ownership ----

	@Test
	public void testOwnerCanUpdate() {
		String id = createOwnedBroadcast("alice");
		BroadcastUpdate update = new BroadcastUpdate();
		update.setName("renamed");
		Response response = service.updateBroadcast(id, update, request("alice", false));
		assertEquals(Status.OK.getStatusCode(), response.getStatus());
	}

	@Test
	public void testAdminCanUpdateOthersBroadcast() {
		String id = createOwnedBroadcast("alice");
		BroadcastUpdate update = new BroadcastUpdate();
		update.setName("renamed");
		Response response = service.updateBroadcast(id, update, request("bob", true));
		assertEquals(Status.OK.getStatusCode(), response.getStatus());
	}

	@Test
	public void testNonOwnerUserCannotUpdate() {
		String id = createOwnedBroadcast("alice");
		Response response = service.updateBroadcast(id, new BroadcastUpdate(), request("bob", false));
		assertEquals(Status.FORBIDDEN.getStatusCode(), response.getStatus());
	}

	@Test
	public void testUpdateMissingBroadcastReturnsNotFound() {
		Response response = service.updateBroadcast("does-not-exist", new BroadcastUpdate(), request("alice", false));
		assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
	}

	@Test
	public void testUpdateNullBodyRejected() {
		String id = createOwnedBroadcast("alice");
		Response response = service.updateBroadcast(id, null, request("alice", false));
		assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
	}

	@Test
	public void testUpdateAllowedWhenNoOwnerSet() {
		// broadcast created without a principal -> no ownerId -> any write-authorized user may update
		Broadcast created = (Broadcast) service
				.createBroadcast(new Broadcast(null, "no-owner"), false, requestWithUser(null)).getEntity();
		Response response = service.updateBroadcast(created.getStreamId(), new BroadcastUpdate(), request("bob", false));
		assertEquals(Status.OK.getStatusCode(), response.getStatus());
	}

	// ---- delete ownership (denial paths) ----

	@Test
	public void testNonOwnerUserCannotDelete() {
		String id = createOwnedBroadcast("alice");
		Response response = service.deleteBroadcast(id, false, request("bob", false));
		assertEquals(Status.FORBIDDEN.getStatusCode(), response.getStatus());
	}

	@Test
	public void testDeleteMissingBroadcastReturnsNotFound() {
		Response response = service.deleteBroadcast("does-not-exist", false, request("alice", false));
		assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
	}

	// ---- read ----

	@Test
	public void testGetExistingBroadcast() {
		String id = createOwnedBroadcast("alice");
		Response response = service.getBroadcast(id);
		assertEquals(Status.OK.getStatusCode(), response.getStatus());
		assertEquals(id, ((Broadcast) response.getEntity()).getStreamId());
	}

	@Test
	public void testGetMissingBroadcastReturnsNotFound() {
		Response response = service.getBroadcast("does-not-exist");
		assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
	}

	@Test
	public void testListReturnsCreatedBroadcasts() {
		createOwnedBroadcast("alice");
		createOwnedBroadcast("bob");
		assertEquals(2, service.getBroadcastList(0, 50, null, null, null, null).size());
	}
}
