package io.antmedia.console.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;

import io.antmedia.console.security.PasswordService.PasswordVerificationResult;
import io.antmedia.test.UnitTestBase;

@Tag("fast")
class PasswordServiceTest extends UnitTestBase<PasswordService> {

	private final PasswordService passwordService = new PasswordService(new Argon2PasswordEncoder(8, 16, 1, 16, 1));
	private final String md5HashedPassword = PasswordService.getMD5Hash("password");

	@Test
	void shouldHashClientMd5WithSaltedArgon2id() {
		String hashedPassword = passwordService.hash(md5HashedPassword);

		assertThat(hashedPassword).startsWith("$argon2id$");
		assertThat(passwordService.hash(md5HashedPassword)).isNotEqualTo(hashedPassword);
		assertVerification(passwordService.verify(md5HashedPassword, hashedPassword), true, false);
		assertVerification(passwordService.verify("password", hashedPassword), false, false);
		assertVerification(passwordService.verify(PasswordService.getMD5Hash(md5HashedPassword), hashedPassword), false, false);
		assertVerification(passwordService.verify(PasswordService.getMD5Hash("wrong-password"), hashedPassword), false, false);
	}

	@Test
	void shouldUseProductionArgon2idParameters() {
		assertThat(new PasswordService().hash(md5HashedPassword)).startsWith("$argon2id$v=19$m=65536,t=3,p=2$");
	}

	@Test
	void shouldVerifyLegacySingleAndDoubleMd5WithClientMd5() {
		assertVerification(passwordService.verify(md5HashedPassword, md5HashedPassword), true, true);
		assertVerification(passwordService.verify(md5HashedPassword, PasswordService.getMD5Hash(md5HashedPassword)), true, true);
	}

	@Test
	void shouldRetainLegacyApiPasswordCompatibility() {
		assertVerification(passwordService.verify("password", md5HashedPassword), true, true);
		assertVerification(passwordService.verify("password", PasswordService.getMD5Hash(md5HashedPassword)), true, true);
		assertVerification(passwordService.verify("password", "password"), true, true);
	}

	@Test
	void shouldRejectNullAndIncorrectCredentials() {
		assertVerification(passwordService.verify(null, md5HashedPassword), false, false);
		assertVerification(passwordService.verify(md5HashedPassword, null), false, false);
		String wrongPassword = PasswordService.getMD5Hash("wrong-password");
		assertVerification(passwordService.verify(wrongPassword, md5HashedPassword), false, false);
		assertVerification(passwordService.verify(wrongPassword, PasswordService.getMD5Hash(md5HashedPassword)), false, false);
	}

	@Test
	void shouldRejectMalformedArgon2Hashes() {
		Argon2PasswordEncoder encoder = mock(Argon2PasswordEncoder.class);
		when(encoder.matches(md5HashedPassword, "$argon2hash")).thenThrow(new IllegalArgumentException("invalid hash"));
		PasswordService service = new PasswordService(encoder);

		assertVerification(service.verify(md5HashedPassword, "$argon2hash"), false, false);
	}

	@Test
	void shouldCalculateLegacyMd5Hash() {
		assertThat(md5HashedPassword).isEqualTo("5f4dcc3b5aa765d61d8327deb882cf99");
	}

	@Test
	void shouldIdentifyArgon2Hashes() {
		assertThat(passwordService.isArgon2Hash("$argon2id$v=19$m=16,t=1,p=1$salt$hash")).isTrue();
		assertThat(passwordService.isArgon2Hash(null)).isFalse();
		assertThat(passwordService.isArgon2Hash(md5HashedPassword)).isFalse();
		assertThat(passwordService.isArgon2Hash("password")).isFalse();
	}

	private void assertVerification(PasswordVerificationResult result, boolean verified, boolean needsRehash) {
		assertThat(result.isVerified()).isEqualTo(verified);
		assertThat(result.isNeedsRehash()).isEqualTo(needsRehash);
	}
}
