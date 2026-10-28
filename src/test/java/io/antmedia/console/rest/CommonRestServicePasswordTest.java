package io.antmedia.console.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;

import io.antmedia.console.datastore.AbstractConsoleDataStore;
import io.antmedia.console.security.PasswordService;
import io.antmedia.datastore.db.types.User;
import io.antmedia.datastore.db.types.UserType;
import io.antmedia.test.UnitTestBase;

@Tag("fast")
class CommonRestServicePasswordTest extends UnitTestBase<CommonRestService> {

	private static final String EMAIL = "admin@example.com";
	private final String md5HashedPassword = PasswordService.getMD5Hash("password");
	private final PasswordService passwordService = new PasswordService(new Argon2PasswordEncoder(8, 16, 1, 16, 1));
	private AbstractConsoleDataStore dataStore;
	private MockHttpServletRequest request;

	@BeforeEach
	void setUp() {
		dataStore = mock(AbstractConsoleDataStore.class);
		request = new MockHttpServletRequest();
		classUnderTest = new CommonRestService() {
			@Override
			public boolean sendUserInfo(String email, String firstname, String lastname, String scope,
					String userType, Map<String, String> appNameUserTypeMap) {
				return true;
			}
		};
		classUnderTest.dataStore = dataStore;
		classUnderTest.setPasswordService(passwordService);
		classUnderTest.setRequestForTest(request);
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void shouldCreateAndAuthenticateUserWithClientMd5(boolean initialUser) {
		User storedUser = user(md5HashedPassword);
		when(dataStore.addUser(storedUser)).thenReturn(true);
		when(dataStore.getUser(EMAIL)).thenReturn(storedUser);

		assertThat((initialUser ? classUnderTest.addInitialUser(storedUser) : classUnderTest.addUser(storedUser))
				.isSuccess()).isTrue();
		assertStoredCredential(storedUser, md5HashedPassword);
		assertThat(classUnderTest.authenticateUser(user(md5HashedPassword)).isSuccess()).isTrue();
		assertThat(CommonRestService.isAuthenticated(request.getSession())).isTrue();
		assertThat(request.getSession().getAttribute(CommonRestService.USER_PASSWORD)).isEqualTo("authenticated");
		verify(dataStore, never()).editUser(any());
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void shouldMigrateLegacyPasswordOnLoginAndAcceptNextLogin(boolean doubleMd5) {
		User storedUser = user(doubleMd5 ? PasswordService.getMD5Hash(md5HashedPassword) : md5HashedPassword);
		when(dataStore.getUser(EMAIL)).thenReturn(storedUser);
		when(dataStore.editUser(storedUser)).thenReturn(true);

		assertThat(classUnderTest.authenticateUser(user(md5HashedPassword)).isSuccess()).isTrue();
		assertStoredCredential(storedUser, md5HashedPassword);
		String migratedHash = storedUser.getPassword();
		assertThat(classUnderTest.authenticateUser(user(md5HashedPassword)).isSuccess()).isTrue();
		assertThat(storedUser.getPassword()).isEqualTo(migratedHash);
		verify(dataStore, times(1)).editUser(storedUser);
	}

	@Test
	void shouldNotMigrateLegacyPasswordAfterFailedLogin() {
		User storedUser = user(PasswordService.getMD5Hash(md5HashedPassword));
		when(dataStore.getUser(EMAIL)).thenReturn(storedUser);

		assertThat(classUnderTest.authenticateUser(user(PasswordService.getMD5Hash("wrong-password"))).isSuccess()).isFalse();
		assertThat(storedUser.getPassword()).isEqualTo(PasswordService.getMD5Hash(md5HashedPassword));
		verify(dataStore, never()).editUser(any());
	}

	@ParameterizedTest
	@ValueSource(strings = {"md5", "doubleMd5", "argon2"})
	void shouldChangePasswordUsingClientMd5(String format) {
		String storedPassword = switch (format) {
			case "md5" -> md5HashedPassword;
			case "doubleMd5" -> PasswordService.getMD5Hash(md5HashedPassword);
			default -> passwordService.hash(md5HashedPassword);
		};
		User storedUser = user(storedPassword);
		when(dataStore.getUser(EMAIL)).thenReturn(storedUser);
		when(dataStore.editUser(storedUser)).thenReturn(true);
		String newMd5HashedPassword = PasswordService.getMD5Hash("new-password");
		User changeRequest = user(md5HashedPassword);
		changeRequest.setNewPassword(newMd5HashedPassword);

		assertThat(classUnderTest.changeUserPasswordInternal(EMAIL, changeRequest).isSuccess()).isTrue();
		assertStoredCredential(storedUser, newMd5HashedPassword);
		assertThat(passwordService.verify(md5HashedPassword, storedUser.getPassword()).isVerified()).isFalse();
		assertThat(classUnderTest.authenticateUser(user(newMd5HashedPassword)).isSuccess()).isTrue();
	}

	@Test
	void shouldHashClientMd5WhenAdminResetsPassword() {
		request.getSession().setAttribute(CommonRestService.USER_EMAIL, "other-admin@example.com");
		User editedUser = user(null);
		editedUser.setNewPassword(md5HashedPassword);
		when(dataStore.doesUsernameExist(EMAIL)).thenReturn(true);
		when(dataStore.editUser(editedUser)).thenReturn(true);

		assertThat(classUnderTest.editUser(editedUser).isSuccess()).isTrue();
		assertStoredCredential(editedUser, md5HashedPassword);
		assertThat(editedUser.getNewPassword()).isNull();
	}

	private User user(String password) {
		return new User(EMAIL, password, UserType.ADMIN, CommonRestService.SCOPE_SYSTEM,
				Map.of(CommonRestService.SCOPE_SYSTEM, UserType.ADMIN.toString()));
	}

	private void assertStoredCredential(User user, String md5HashedPassword) {
		assertThat(user.getPassword()).startsWith("$argon2id$");
		assertThat(passwordService.verify(md5HashedPassword, user.getPassword()).isVerified()).isTrue();
		assertThat(passwordService.verify("password", user.getPassword()).isVerified()).isFalse();
		assertThat(passwordService.verify(PasswordService.getMD5Hash(md5HashedPassword), user.getPassword()).isVerified()).isFalse();
	}
}
