package io.antmedia.console.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Pattern;

import org.apache.commons.codec.binary.Hex;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;

public class PasswordService {

	private static final Pattern MD5_PATTERN = Pattern.compile("^[a-fA-F0-9]{32}$");
	private static final String ARGON2_PREFIX = "$argon2";

	private final Argon2PasswordEncoder argon2PasswordEncoder;

	public PasswordService() {
		this(new Argon2PasswordEncoder(16, 32, 2, 65536, 3));
	}

	public PasswordService(Argon2PasswordEncoder argon2PasswordEncoder) {
		this.argon2PasswordEncoder = argon2PasswordEncoder;
	}

	public String hash(String rawPassword) {
		return argon2PasswordEncoder.encode(rawPassword);
	}

	public PasswordVerificationResult verify(String rawPassword, String storedPassword) {
		if (rawPassword == null || storedPassword == null) {
			return PasswordVerificationResult.failed();
		}

		if (isArgon2Hash(storedPassword)) {
			try {
				return new PasswordVerificationResult(argon2PasswordEncoder.matches(rawPassword, storedPassword), false);
			}
			catch (RuntimeException e) {
				return PasswordVerificationResult.failed();
			}
		}

		String md5Password = getMD5Hash(rawPassword);
		boolean legacyMatch = storedPassword.equals(md5Password)
				|| storedPassword.equals(getMD5Hash(md5Password))
				|| (!isMD5Hash(storedPassword) && storedPassword.equals(rawPassword));

		return new PasswordVerificationResult(legacyMatch, legacyMatch);
	}

	public boolean isArgon2Hash(String storedPassword) {
		return storedPassword != null && storedPassword.startsWith(ARGON2_PREFIX);
	}

	private boolean isMD5Hash(String storedPassword) {
		return storedPassword != null && MD5_PATTERN.matcher(storedPassword).matches();
	}

	public static String getMD5Hash(String pass) {
		String passResult;
		try {
			MessageDigest m = MessageDigest.getInstance("MD5");
			m.reset();
			m.update(pass.getBytes(StandardCharsets.UTF_8));
			byte[] digestResult = m.digest();
			passResult = Hex.encodeHexString(digestResult);
		} catch (NoSuchAlgorithmException e) {
            // MD5 is guaranteed to be present in the JDK
            throw new IllegalStateException(e);
		}
		return passResult;
	}

	public static class PasswordVerificationResult {
		private final boolean verified;
		private final boolean needsRehash;

		public PasswordVerificationResult(boolean verified, boolean needsRehash) {
			this.verified = verified;
			this.needsRehash = needsRehash;
		}

		public static PasswordVerificationResult failed() {
			return new PasswordVerificationResult(false, false);
		}

		public boolean isVerified() {
			return verified;
		}

		public boolean isNeedsRehash() {
			return needsRehash;
		}
	}
}
