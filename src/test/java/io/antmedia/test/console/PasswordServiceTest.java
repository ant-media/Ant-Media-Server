package io.antmedia.test.console;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;

import io.antmedia.console.security.PasswordService;
import io.antmedia.console.security.PasswordService.PasswordVerificationResult;

public class PasswordServiceTest {

	private final PasswordService passwordService = new PasswordService(new Argon2PasswordEncoder(8, 16, 1, 16, 1));

	@Test
	public void testHashAndVerifyArgon2Password() {
		String rawPassword = "password";

		String hashedPassword = passwordService.hash(rawPassword);

		assertNotEquals(rawPassword, hashedPassword);
		assertTrue(passwordService.isArgon2Hash(hashedPassword));

		PasswordVerificationResult result = passwordService.verify(rawPassword, hashedPassword);
		assertTrue(result.isVerified());
		assertFalse(result.isNeedsRehash());

		result = passwordService.verify("wrong-password", hashedPassword);
		assertFalse(result.isVerified());
		assertFalse(result.isNeedsRehash());
	}

	@Test
	public void testVerifyLegacyPasswordsNeedRehash() {
		assertLegacyPassword("password", PasswordService.getMD5Hash("password"));
		assertLegacyPassword("password", PasswordService.getMD5Hash(PasswordService.getMD5Hash("password")));
		assertLegacyPassword("password", "password");
	}

	@Test
	public void testVerifyFailsForNullAndInvalidPasswords() {
		PasswordVerificationResult result = passwordService.verify(null, "password");
		assertFalse(result.isVerified());
		assertFalse(result.isNeedsRehash());

		result = passwordService.verify("password", null);
		assertFalse(result.isVerified());
		assertFalse(result.isNeedsRehash());

		result = passwordService.verify("wrong-password", PasswordService.getMD5Hash("password"));
		assertFalse(result.isVerified());
		assertFalse(result.isNeedsRehash());
	}

	@Test
	public void testVerifyFailsWhenArgon2EncoderThrowsException() {
		Argon2PasswordEncoder encoder = Mockito.mock(Argon2PasswordEncoder.class);
		Mockito.when(encoder.matches("password", "$argon2hash")).thenThrow(new IllegalArgumentException("invalid hash"));
		PasswordService service = new PasswordService(encoder);

		PasswordVerificationResult result = service.verify("password", "$argon2hash");

		assertFalse(result.isVerified());
		assertFalse(result.isNeedsRehash());
	}

	@Test
	public void testGetMD5Hash() {
		assertEquals("5f4dcc3b5aa765d61d8327deb882cf99", PasswordService.getMD5Hash("password"));
	}

	@Test
	public void testIsArgon2Hash() {
		assertTrue(passwordService.isArgon2Hash("$argon2id$v=19$m=16,t=1,p=1$salt$hash"));
		assertFalse(passwordService.isArgon2Hash(null));
		assertFalse(passwordService.isArgon2Hash(PasswordService.getMD5Hash("password")));
		assertFalse(passwordService.isArgon2Hash("password"));
	}

	private void assertLegacyPassword(String rawPassword, String storedPassword) {
		PasswordVerificationResult result = passwordService.verify(rawPassword, storedPassword);
		assertTrue(result.isVerified());
		assertTrue(result.isNeedsRehash());
	}
}
