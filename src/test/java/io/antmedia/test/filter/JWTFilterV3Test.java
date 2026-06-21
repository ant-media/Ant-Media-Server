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
}
