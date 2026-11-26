package io.antmedia.console.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;

import io.antmedia.console.datastore.AbstractConsoleDataStore;
import io.antmedia.datastore.db.types.User;
import io.antmedia.datastore.db.types.UserType;
import io.antmedia.settings.ServerSettings;
import io.antmedia.test.UnitTestBase;

@Tag("fast")
class AuthenticationFilterAuthorizationTest extends UnitTestBase<AuthenticationFilter> {

	@ParameterizedTest
	@MethodSource("managementWriteAttacks")
	void shouldRejectManagementWritesRegardlessOfDispatchParameter(UserCase actor, Route route, String target) throws Exception {
		assertAccess(actor.user(), managementRequest(route.method(), route.path(), target), false);
	}

	static Stream<Arguments> managementWriteAttacks() {
		List<Route> routes = List.of(new Route("POST", "/rest/v2/users"), new Route("PUT", "/rest/v2/users"),
				new Route("DELETE", "/rest/v2/users/admin"), new Route("POST", "/rest/v2/server-settings"),
				new Route("POST", "/rest/v2/applications/settings/OtherApp"),
				new Route("PUT", "/rest/v2/applications/LiveApp"), new Route("PUT", "/rest/v2/applications/evil"),
				new Route("PUT", "/rest/v2/plugins/evil"), new Route("POST", "/rest/v2/plugins/install-from-url"));
		List<UserCase> actors = List.of(modern("LiveApp", UserType.USER), modern("LiveApp", UserType.ADMIN),
				modern("LiveApp", UserType.READ_ONLY), modern("system", UserType.USER),
				legacy("LiveApp", UserType.USER), legacy("LiveApp", UserType.ADMIN), legacy("system", UserType.USER),
				new UserCase("modern role overrides legacy admin",
						new User("user@antmedia.test", "password", UserType.ADMIN, "system", Map.of("LiveApp", "USER"))));
		return actors.stream().flatMap(actor -> routes.stream().flatMap(route ->
				Stream.of(null, "LiveApp", "LiveApp/rest/v2/broadcasts")
						.map(target -> Arguments.of(actor, route, target))));
	}

	@ParameterizedTest
	@MethodSource("sensitiveReadAttacks")
	void shouldRejectOutOfScopeReadsRegardlessOfDispatchParameter(UserCase actor, String path) throws Exception {
		assertAccess(actor.user(), managementRequest("GET", path, "LiveApp"), false);
	}

	static Stream<Arguments> sensitiveReadAttacks() {
		return Stream.of(modern("LiveApp", UserType.ADMIN), modern("LiveApp", UserType.USER),
				modern("LiveApp", UserType.READ_ONLY), legacy("LiveApp", UserType.READ_ONLY))
				.flatMap(actor -> Stream.of("/rest/v2/server-settings", "/rest/v2/heap-dump",
						"/rest/v2/applications/settings/OtherApp")
						.map(path -> Arguments.of(actor, path)));
	}

	@ParameterizedTest
	@MethodSource("proxyPermissions")
	void shouldPreserveProxyRolesAndRestrictUserWritesToContent(UserCase actor, String method, String resource, boolean allowed)
			throws Exception {
		check(actor.user(), method, "/rest/v2/request", null, query("LiveApp" + resource), allowed);
	}

	static Stream<Arguments> proxyPermissions() {
		List<String> userContent = List.of("/rest/v2/broadcasts/create", "/rest/v2/vods/id");
		List<String> adminWrites = List.of("/rest/v2/settings", "/rest/v2/broadcasts-extra",
				"/rest/v2/settings?hint=/rest/v2/broadcasts");
		return Stream.of(UserType.values()).flatMap(role ->
				Stream.of(modern("LiveApp", role), modern("system", role), legacy("LiveApp", role))
						.flatMap(actor -> Stream.of("GET", "POST", "PUT", "DELETE").flatMap(method ->
								Stream.concat(userContent.stream(), adminWrites.stream())
										.map(resource -> Arguments.of(actor, method, resource,
												"GET".equals(method) || role == UserType.ADMIN
														|| role == UserType.USER && userContent.contains(resource))))));
	}

	@ParameterizedTest
	@MethodSource("settingsPermissions")
	void shouldPreserveOwnApplicationSettingsPermissions(UserCase actor, String method, boolean allowed) throws Exception {
		assertAccess(actor.user(), managementRequest(method, "/rest/v2/applications/settings/LiveApp", "OtherApp"), allowed);
		check(actor.user(), method, "/rest", "/v2/applications/settings/LiveApp/", null, allowed);
	}

	static Stream<Arguments> settingsPermissions() {
		return Stream.of(UserType.values()).flatMap(role ->
				Stream.of(modern("LiveApp", role), legacy("LiveApp", role)).flatMap(actor ->
						Stream.of("GET", "POST").map(method ->
								Arguments.of(actor, method, "GET".equals(method) || role == UserType.ADMIN))));
	}

	@ParameterizedTest
	@ValueSource(strings = {"LiveApp2/rest/v2/broadcasts", "LiveApp-other/rest/v2/vods", "OtherApp/rest/v2/broadcasts",
			"LiveApp%3bOtherApp/rest/v2/broadcasts"})
	void shouldMatchApplicationNamesExactly(String target) throws Exception {
		check(modern("LiveApp", UserType.ADMIN).user(), "GET", "/rest/v2/request", null, query(target), false);
		check(legacy("LiveApp", UserType.ADMIN).user(), "POST", "/rest/request", null, query(target), false);
	}

	@ParameterizedTest
	@ValueSource(strings = {"users", "server-settings", "rest", "LiveApp"})
	void shouldNotTreatManagementResourceNamesAsApplicationPermissions(String app) throws Exception {
		check(modern(app, UserType.ADMIN).user(), "POST", "/rest", "/v2/users", null, false);
		check(modern(app, UserType.ADMIN).user(), "POST", "/rest", "/v2/server-settings", null, false);
		check(modern(app, UserType.ADMIN).user(), "PUT", "/rest", "/v2/applications/" + app, null, false);
	}

	@ParameterizedTest
	@ValueSource(strings = {"LiveApp/../OtherApp/rest/v2/broadcasts", "LiveApp/..;/OtherApp/rest/v2/broadcasts",
			"LiveApp/%2e%2e/OtherApp/rest/v2/broadcasts", "LiveApp/%2e%2e%3b/OtherApp/rest/v2/broadcasts",
			"LiveApp/rest/v2/broadcasts/../../settings", "LiveApp%2f..%2fOtherApp/rest/v2/broadcasts",
			"LiveApp/rest/v2/broadcasts/%00bad", "LiveApp\\..\\OtherApp/rest/v2/broadcasts"})
	void shouldRejectAmbiguousOrTraversingProxyTargets(String target) throws Exception {
		check(modern("LiveApp", UserType.ADMIN).user(), "POST", "/rest/v2/request", null, query(target), false);
	}

	@ParameterizedTest
	@ValueSource(strings = {"_path=LiveApp/rest/v2/broadcasts&_path=OtherApp/rest/v2/broadcasts",
			"_path=OtherApp/rest/v2/broadcasts&_path=LiveApp/rest/v2/broadcasts",
			"_path=LiveApp/rest/v2/broadcasts;%5Fpath=OtherApp/rest/v2/broadcasts",
			"_path=LiveApp/rest/v2/broadcasts&%5Fpath=OtherApp/rest/v2/broadcasts",
			"_path", "_path=", "other=value", "_path=%", "_path=%ZZ", "_path=%2F", "_path=%2F%2F"})
	void shouldRejectDuplicateOrMissingProxyTargets(String rawQuery) throws Exception {
		check(modern("LiveApp", UserType.ADMIN).user(), "GET", "/rest/v2/request", null, rawQuery, false);
	}

	@Test
	void shouldNotUseFormParametersForProxyAuthorization() throws Exception {
		MockHttpServletRequest request = request("POST", "/rest/v2/request", null, null);
		request.addParameter(AuthenticationFilter.DISPATCH_PATH_URL, "LiveApp/rest/v2/broadcasts");
		assertAccess(modern("LiveApp", UserType.ADMIN).user(), request, false);
	}

	@Test
	void shouldPreserveBothProxyMappingsLeadingSlashAndPathInfo() throws Exception {
		for (String servlet : List.of("/rest/v2/request", "/rest/request")) {
			for (String leading : List.of("", "/")) {
				check(modern("LiveApp", UserType.USER).user(), "POST", servlet, null,
						query(leading + "LiveApp/rest/v2/broadcasts/create"), true);
			}
		}
		check(modern("LiveApp", UserType.USER).user(), "POST", "/rest/v2/request", "/rest/v2/vods/id", query("LiveApp"), true);
		check(modern("LiveApp", UserType.USER).user(), "POST", "/rest/v2/request", "/../../settings",
				query("LiveApp/rest/v2/broadcasts"), false);
		check(modern("LiveApp", UserType.USER).user(), "POST", "/rest/v2/request", "/rest/v2/broadcasts",
				query("LiveApp/rest/v2/settings?hint="), false);
	}

	@Test
	void shouldPreserveMultipleApplicationsWithDifferentRoles() throws Exception {
		User user = new User("user@antmedia.test", "password", null, null,
				Map.of("LiveApp", "ADMIN", "OtherApp", "READ_ONLY"));
		check(user, "POST", "/rest/v2/request", null, query("LiveApp/rest/v2/settings"), true);
		check(user, "GET", "/rest/v2/request", null, query("OtherApp/rest/v2/broadcasts"), true);
		check(user, "POST", "/rest/v2/request", null, query("OtherApp/rest/v2/broadcasts"), false);
		check(user, "POST", "/rest", "/v2/server-settings", query("LiveApp"), false);
	}

	@Test
	void shouldPreserveSystemAdminAndLegacyAccounts() throws Exception {
		for (User user : List.of(modern("system", UserType.ADMIN).user(), legacy("system", UserType.ADMIN).user(),
				legacy(null, UserType.ADMIN).user())) {
			check(user, "POST", "/rest", "/v2/users", query("LiveApp"), true);
			check(user, "GET", "/rest", "/v2/server-settings", null, true);
			check(user, "POST", "/rest/v2/request", null, query("OtherApp/rest/v2/settings"), true);
		}
		check(modern("system", UserType.USER).user(), "GET", "/rest", "/v2/server-settings", null, true);
		check(modern("system", UserType.READ_ONLY).user(), "GET", "/rest/v2/request", null,
				query("OtherApp/rest/v2/broadcasts"), true);
	}

	@Test
	void shouldPreserveReadsWithoutInferringAdministratorFromMissingRoles() throws Exception {
		User unscoped = legacy(null, null).user();
		check(unscoped, "GET", "/rest", "/v2/system-status", null, true);
		check(unscoped, "POST", "/rest", "/v2/server-settings", null, false);
		User scoped = legacy("LiveApp", null).user();
		check(scoped, "GET", "/rest", "/v2/applications/settings/LiveApp", null, true);
		check(scoped, "GET", "/rest", "/v2/applications/settings/OtherApp", null, false);
		check(scoped, "POST", "/rest/v2/request", null, query("LiveApp/rest/v2/broadcasts"), false);
	}

	@Test
	void shouldAcceptTheDocumentedReadOnlySpellingInApplicationAssignments() throws Exception {
		User user = new User("user@antmedia.test", "password", null, null, Map.of("LiveApp", "READ-ONLY"));
		check(user, "GET", "/rest", "/v2/applications/settings/LiveApp", null, true);
		check(user, "POST", "/rest/v2/request", null, query("LiveApp/rest/v2/broadcasts"), false);
		check(user, "GET", "/rest", "/v2/applications/settings/OtherApp", null, false);
	}

	@Test
	void shouldPreserveSelfServiceAndVersionAccessForScopedUsers() throws Exception {
		User user = modern("LiveApp", UserType.READ_ONLY).user();
		check(user, "POST", "/rest", "/v2/users/password", query("OtherApp"), true);
		check(user, "POST", "/rest", "/v2/support/request", null, true);
		check(user, "GET", "/rest", "/v2/version", null, true);
		check(user, "GET", "/rest", "/v2/enterprise-edition", null, true);
		check(user, "GET", "/rest", "/v2/admin-status", null, true);
	}

	@Test
	void shouldAuthorizeOnlyTheDeclaredSelfServiceMethods() throws Exception {
		User reader = modern("LiveApp", UserType.READ_ONLY).user();
		for (String path : List.of("/v2/users/initial", "/v2/users/authenticate", "/v2/users/password")) {
			check(reader, "DELETE", "/rest", path, null, false);
		}
		check(reader, "POST", "/rest", "/v2/support/request/", null, true);
		check(reader, "POST", "/rest", "/v2/support/request/child", null, false);
	}

	@ParameterizedTest
	@ValueSource(strings = {"/v2/users/initial", "/v2/users/authenticate"})
	void shouldRequireLoginForGetAndKeepPostAndOptionsPublic(String path) throws Exception {
		AuthenticationFilter filter = spy(new AuthenticationFilter());
		doReturn(new ServerSettings()).when(filter).getServerSettings();
		for (String method : List.of("GET", "POST", "OPTIONS")) {
			MockHttpServletRequest request = request(method, "/rest", path, null);
			MockHttpServletResponse response = new MockHttpServletResponse();
			MockFilterChain chain = new MockFilterChain();
			filter.doFilter(request, response, chain);
			if ("GET".equals(method)) {
				assertThat(chain.getRequest()).isNull();
				assertThat(response.getStatus()).isEqualTo(403);
				assertThat(response.getErrorMessage()).isEqualTo("Not authenticated user");
			} else {
				assertThat(chain.getRequest()).isSameAs(request);
				assertThat(response.getStatus()).isEqualTo(200);
			}
			assertThat(request.getAttribute(AuthenticationFilter.PANEL_AUTHENTICATED_ATTRIBUTE)).isNull();
		}
	}

	@Test
	void shouldRequireAnAssignedScopeForManagementAndSettings() throws Exception {
		check(legacy("", UserType.ADMIN).user(), "POST", "/rest", "/v2/users", null, false);
		User admin = modern("LiveApp", UserType.ADMIN).user();
		check(admin, "POST", "/rest", "/v2/applications/settings/OtherApp/", null, false);
		check(admin, "POST", "/rest", "/v2/applications/settings/LiveApp/child", null, false);
	}

	@Test
	void shouldReserveTheProxyRootForSystemScope() throws Exception {
		for (String target : List.of("", "/")) {
			check(modern("system", UserType.READ_ONLY).user(), "GET", "/rest/request", null, query(target), true);
			check(modern("system", UserType.READ_ONLY).user(), "POST", "/rest/request", null, query(target), false);
			check(modern("system", UserType.ADMIN).user(), "POST", "/rest/request", null, query(target), true);
			check(modern("LiveApp", UserType.ADMIN).user(), "GET", "/rest/request", null, query(target), false);
		}
	}

	@Test
	void shouldFailClosedForUnknownRolesAndEmptyApplicationNames() throws Exception {
		for (Map<String, String> roles : List.of(Map.of("LiveApp", "unknown"), Map.of("", "ADMIN"), Map.<String, String>of())) {
			User user = new User("user@antmedia.test", "password", UserType.ADMIN, "system", roles);
			check(user, "POST", "/rest", "/v2/users", null, false);
			check(user, "GET", "/rest", "/v2/server-settings", query("LiveApp"), false);
			check(user, "POST", "/rest/v2/request", null, query("LiveApp/rest/v2/broadcasts"), false);
		}
	}

	private void check(User user, String method, String servletPath, String pathInfo, String query, boolean allowed)
			throws Exception {
		assertAccess(user, request(method, servletPath, pathInfo, query), allowed);
	}

	private void assertAccess(User user, MockHttpServletRequest request, boolean allowed) throws Exception {
		MockHttpSession session = new MockHttpSession();
		session.setAttribute(CommonRestService.IS_AUTHENTICATED, true);
		session.setAttribute(CommonRestService.USER_EMAIL, user.getEmail());
		session.setAttribute(CommonRestService.USER_PASSWORD, user.getPassword());
		request.setSession(session);
		AbstractConsoleDataStore store = mock(AbstractConsoleDataStore.class);
		when(store.getUser(user.getEmail())).thenReturn(user);
		AuthenticationFilter filter = spy(new AuthenticationFilter());
		doReturn(new ServerSettings()).when(filter).getServerSettings();
		doReturn(store).when(filter).getAbstractConsoleDataStore();
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain chain = new MockFilterChain();

		filter.doFilter(request, response, chain);

		assertThat(request.getAttribute(AuthenticationFilter.PANEL_AUTHENTICATED_ATTRIBUTE)).isEqualTo(allowed ? Boolean.TRUE : null);
		assertThat(response.getStatus()).as("%s %s?%s", request.getMethod(), request.getRequestURI(), request.getQueryString())
				.isEqualTo(allowed ? 200 : 403);
		assertThat(chain.getRequest()).isSameAs(allowed ? request : null);
		if (!allowed) {
			assertThat(response.getErrorMessage()).isEqualTo(AuthenticationFilter.FORBIDDEN_ERROR);
		}
	}

	private static MockHttpServletRequest request(String method, String servletPath, String pathInfo, String query) {
		MockHttpServletRequest request = new MockHttpServletRequest(method, servletPath + (pathInfo == null ? "" : pathInfo));
		request.setServletPath(servletPath);
		request.setPathInfo(pathInfo);
		request.setQueryString(query);
		return request;
	}

	private static MockHttpServletRequest managementRequest(String method, String path, String target) {
		MockHttpServletRequest request = request(method, "/rest", path.substring("/rest".length()),
				target == null ? null : query(target));
		// Spring's mock does not populate servlet parameters from setQueryString.
		if (target != null) {
			request.addParameter(AuthenticationFilter.DISPATCH_PATH_URL, target);
		}
		return request;
	}

	private static String query(String target) {
		return "_path=" + URLEncoder.encode(target, StandardCharsets.UTF_8);
	}

	private static UserCase modern(String scope, UserType role) {
		return new UserCase(scope + ":" + role, new User("user@antmedia.test", "password", null, null,
				Map.of(scope, role.toString())));
	}

	private static UserCase legacy(String scope, UserType role) {
		return new UserCase("legacy " + scope + ":" + role, new User("user@antmedia.test", "password", role, scope, null));
	}

	record UserCase(String name, User user) {
	}

	record Route(String method, String path) {
	}
}
