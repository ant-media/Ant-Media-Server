package io.antmedia.test.filter;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import io.antmedia.rest.JWTFilterV3;

public class JWTFilterV3Test {

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
}
