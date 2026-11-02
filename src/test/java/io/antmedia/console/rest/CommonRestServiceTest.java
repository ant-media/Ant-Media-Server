package io.antmedia.console.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.red5.server.api.scope.IScope;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.StaticWebApplicationContext;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.console.AdminApplication;
import io.antmedia.console.AdminApplication.ApplicationInfo;
import io.antmedia.console.datastore.AbstractConsoleDataStore;
import io.antmedia.console.datastore.ConsoleDataStoreFactory;
import io.antmedia.datastore.db.InMemoryDataStore;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.datastore.db.types.User;
import io.antmedia.rest.model.StreamSearchResult;
import io.antmedia.settings.ServerSettings;
import io.antmedia.statistic.IStatsCollector;
import io.antmedia.statistic.StatsCollector;
import io.antmedia.test.UnitTestBase;

@Tag("fast")
class CommonRestServiceTest extends UnitTestBase<CommonRestService> {

	private static final String USER_EMAIL = "scoped@test.com";

	private AdminApplication application;

	private static User user(String scope, Map<String, String> appRoles) {
		return new User(USER_EMAIL, "pass", null, scope, appRoles);
	}

	private static User customer1User() {
		return user(null, Map.of("customer1", "READ_ONLY"));
	}

	private static User systemUser() {
		return user("system", null);
	}

	private static Broadcast stream(String streamId, String name) throws Exception {
		Broadcast broadcast = new Broadcast(name);
		broadcast.setStreamId(streamId);
		return broadcast;
	}

	private static List<Broadcast> streams(String idPrefix, int count) throws Exception {
		List<Broadcast> broadcasts = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			broadcasts.add(stream(idPrefix + i, "cam " + i));
		}
		return broadcasts;
	}

	private void givenApps(String... appNames) {
		application = mock(AdminApplication.class);
		when(application.getApplications()).thenReturn(List.of(appNames));
		classUnderTest = spy(new CommonRestService());
		classUnderTest.setApplication(application);
	}

	private void givenStreams(String appName, List<Broadcast> broadcasts) {
		InMemoryDataStore dataStore = new InMemoryDataStore(appName);
		broadcasts.forEach(dataStore::save);
		AntMediaApplicationAdapter adaptor = mock(AntMediaApplicationAdapter.class);
		when(adaptor.getDataStore()).thenReturn(dataStore);
		doReturn(adaptor).when(classUnderTest).getAppAdaptor(appName);
	}

	private void givenSession(User user) {
		AbstractConsoleDataStore consoleStore = mock(AbstractConsoleDataStore.class);
		when(consoleStore.getUser(USER_EMAIL)).thenReturn(user);
		ConsoleDataStoreFactory factory = mock(ConsoleDataStoreFactory.class);
		when(factory.getDataStore()).thenReturn(consoleStore);
		classUnderTest.setDataStoreFactory(factory);

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.getSession().setAttribute(CommonRestService.USER_EMAIL, USER_EMAIL);
		classUnderTest.setRequestForTest(request);
	}

	// Server JWT requests pass AuthenticationFilter with no session user.
	private void givenNoSessionUser() {
		ConsoleDataStoreFactory factory = mock(ConsoleDataStoreFactory.class);
		when(factory.getDataStore()).thenReturn(mock(AbstractConsoleDataStore.class));
		classUnderTest.setDataStoreFactory(factory);
		classUnderTest.setRequestForTest(new MockHttpServletRequest());
	}

	private static ApplicationInfo appInfo(String name) {
		ApplicationInfo info = new ApplicationInfo();
		info.name = name;
		return info;
	}

	private static List<String> appNames(String applicationInfoJson) {
		JsonArray array = JsonParser.parseString(applicationInfoJson).getAsJsonArray();
		List<String> names = new ArrayList<>();
		for (JsonElement element : array) {
			names.add(element.getAsJsonObject().get("name").getAsString());
		}
		return names;
	}

	@Test
	void shouldGrantEveryAppToSystemAndBlankScope() {
		assertThat(CommonRestService.canAccessApp(user("system", null), "customer1")).isTrue();
		assertThat(CommonRestService.canAccessApp(user("", null), "customer1")).isTrue();
		assertThat(CommonRestService.canAccessApp(user(null, Map.of("system", "READ_ONLY")), "customer1")).isTrue();
	}

	@Test
	void shouldGrantEveryAppWhenAppRolesAreMissing() {
		assertThat(CommonRestService.canAccessApp(user(null, null), "customer1")).isTrue();
		assertThat(CommonRestService.canAccessApp(user(null, new HashMap<>()), "customer1")).isTrue();
	}

	@Test
	void shouldMatchScopedAppsExactly() {
		User mapUser = customer1User();
		assertThat(CommonRestService.canAccessApp(mapUser, "customer1")).isTrue();
		assertThat(CommonRestService.canAccessApp(mapUser, "customer10")).isFalse();
		assertThat(CommonRestService.canAccessApp(mapUser, "Customer1")).isFalse();
		assertThat(CommonRestService.canAccessApp(mapUser, "customer")).isFalse();

		User scopeUser = user("customer1", null);
		assertThat(CommonRestService.canAccessApp(scopeUser, "customer1")).isTrue();
		assertThat(CommonRestService.canAccessApp(scopeUser, "customer10")).isFalse();
	}

	// Same precedence as AuthenticationFilter: a set scope ignores the map.
	@Test
	void shouldPreferScopeOverAppRoles() {
		User user = user("customer1", Map.of("customer2", "ADMIN"));

		assertThat(CommonRestService.canAccessApp(user, "customer1")).isTrue();
		assertThat(CommonRestService.canAccessApp(user, "customer2")).isFalse();
	}

	@Test
	void shouldSearchEveryAppForSystemUser() throws Exception {
		givenApps("customer1", "customer2");
		givenStreams("customer1", List.of(stream("cam1", "Front door"), stream("lobby1", "Lobby")));
		givenStreams("customer2", List.of(stream("cam2", "Back door")));
		givenSession(systemUser());

		StreamSearchResult result = classUnderTest.searchStreams("door");

		assertThat(result.results()).containsExactly(
				new StreamSearchResult.Item("cam1", "Front door", "customer1"),
				new StreamSearchResult.Item("cam2", "Back door", "customer2"));
		assertThat(result.truncated()).isFalse();
	}

	@Test
	void shouldMatchStreamIdAndNameIgnoringCase() throws Exception {
		givenApps("customer1");
		givenStreams("customer1", List.of(stream("cam1", "Front door"), stream("lobby1", "Lobby")));
		givenSession(systemUser());

		assertThat(classUnderTest.searchStreams("CAM1").results()).extracting(StreamSearchResult.Item::streamId).containsExactly("cam1");
		assertThat(classUnderTest.searchStreams("FRONT").results()).extracting(StreamSearchResult.Item::streamId).containsExactly("cam1");
	}

	@Test
	void shouldReturnStreamsWithoutName() throws Exception {
		givenApps("customer1");
		givenStreams("customer1", List.of(stream("cam1", null)));
		givenSession(systemUser());

		assertThat(classUnderTest.searchStreams("cam1").results())
				.containsExactly(new StreamSearchResult.Item("cam1", null, "customer1"));
	}

	@Test
	void shouldNotTouchAppsTheUserCannotAccess() throws Exception {
		givenApps("customer1", "customer10");
		givenStreams("customer1", List.of(stream("cam1", "Front door")));
		givenStreams("customer10", List.of(stream("cam10", "Other tenant door")));
		givenSession(customer1User());

		StreamSearchResult result = classUnderTest.searchStreams("door");

		assertThat(result.results()).extracting(StreamSearchResult.Item::appName).containsExactly("customer1");
		verify(classUnderTest, never()).getAppAdaptor("customer10");
	}

	@Test
	void shouldNotSearchConsoleApp() throws Exception {
		givenApps(AdminApplication.APP_NAME, "customer1");
		givenStreams("customer1", List.of(stream("cam1", "Front door")));
		givenSession(systemUser());

		classUnderTest.searchStreams("door");

		verify(classUnderTest, never()).getAppAdaptor(AdminApplication.APP_NAME);
	}

	@Test
	void shouldSkipAppWithoutAdaptor() throws Exception {
		givenApps("stopping", "customer1");
		doReturn(null).when(classUnderTest).getAppAdaptor("stopping");
		givenStreams("customer1", List.of(stream("cam1", "Front door")));
		givenSession(systemUser());

		assertThat(classUnderTest.searchStreams("door").results()).extracting(StreamSearchResult.Item::appName).containsExactly("customer1");
	}

	@Test
	void shouldSearchEveryAppForRequestWithoutSessionUser() throws Exception {
		givenApps("customer1", "customer2");
		givenStreams("customer1", List.of(stream("cam1", "Front door")));
		givenStreams("customer2", List.of(stream("cam2", "Back door")));
		givenNoSessionUser();

		assertThat(classUnderTest.searchStreams("door").results()).hasSize(2);
	}

	@Test
	void shouldReturnNothingWhenSessionUserIsGone() throws Exception {
		givenApps("customer1");
		givenSession(null);

		assertThat(classUnderTest.searchStreams("door").results()).isEmpty();
		verify(classUnderTest, never()).getAppAdaptor(anyString());
	}

	@Test
	void shouldStopAtFiftyResultsAndSkipRemainingApps() throws Exception {
		givenApps("a", "b", "c");
		givenStreams("a", streams("a", 30));
		givenStreams("b", streams("b", 30));
		givenStreams("c", streams("c", 30));
		givenSession(systemUser());

		StreamSearchResult result = classUnderTest.searchStreams("cam");

		assertThat(result.results()).hasSize(50);
		assertThat(result.truncated()).isTrue();
		verify(classUnderTest, never()).getAppAdaptor("c");
	}

	@Test
	void shouldNotFlagExactlyFiftyResultsAsTruncated() throws Exception {
		givenApps("a", "b", "c");
		givenStreams("a", streams("a", 30));
		givenStreams("b", streams("b", 20));
		givenStreams("c", List.of());
		givenSession(systemUser());

		StreamSearchResult result = classUnderTest.searchStreams("cam");

		assertThat(result.results()).hasSize(50);
		assertThat(result.truncated()).isFalse();
	}

	// The 51st match lives in a later app, so reaching 50 must not stop the check.
	@Test
	void shouldFlagTruncatedWhenExtraMatchIsInLaterApp() throws Exception {
		givenApps("a", "b");
		givenStreams("a", streams("a", 50));
		givenStreams("b", streams("b", 1));
		givenSession(systemUser());

		StreamSearchResult result = classUnderTest.searchStreams("cam");

		assertThat(result.results()).hasSize(50);
		assertThat(result.truncated()).isTrue();
	}

	@Test
	void shouldIgnoreInvalidQueriesWithoutTouchingApps() {
		givenApps("customer1");
		givenSession(systemUser());

		for (String query : new String[] { null, "", "   ", "a", " a ", "x".repeat(101) }) {
			StreamSearchResult result = classUnderTest.searchStreams(query);

			assertThat(result.results()).as("query [%s]", query).isEmpty();
			assertThat(result.truncated()).isFalse();
		}
		verify(classUnderTest, never()).getAppAdaptor(anyString());
	}

	@Test
	void shouldTrimQuery() throws Exception {
		givenApps("customer1");
		givenStreams("customer1", List.of(stream("cam1", "Front door")));
		givenSession(systemUser());

		assertThat(classUnderTest.searchStreams("  cam1  ").results()).hasSize(1);
	}

	@Test
	void shouldLoadApplicationInfoOnlyForAllowedApps() {
		givenApps("customer1", "customer10");
		when(application.getApplicationInfo(List.of("customer1"))).thenReturn(List.of(appInfo("customer1")));
		givenSession(customer1User());

		// Storage is computed per listed app. The stub only answers the filtered list, so any other argument returns nothing.
		assertThat(appNames(classUnderTest.getApplicationInfo())).containsExactly("customer1");
	}

	@Test
	void shouldLoadApplicationInfoForAllAppsForSystemUser() {
		givenApps("customer1", "customer10");
		when(application.getApplicationInfo(List.of("customer1", "customer10")))
				.thenReturn(List.of(appInfo("customer1"), appInfo("customer10")));
		givenSession(systemUser());

		assertThat(appNames(classUnderTest.getApplicationInfo())).containsExactly("customer1", "customer10");
	}

	@Test
	void shouldLoadApplicationInfoForAllAppsWithoutSessionUser() {
		givenApps("customer1", "customer10");
		when(application.getApplicationInfo(List.of("customer1", "customer10")))
				.thenReturn(List.of(appInfo("customer1"), appInfo("customer10")));
		givenNoSessionUser();

		assertThat(appNames(classUnderTest.getApplicationInfo())).containsExactly("customer1", "customer10");
	}

	@Test
	void shouldReturnNullAppAdaptorWithoutApplication() {
		classUnderTest = new CommonRestService();

		assertThat(classUnderTest.getAppAdaptor("app")).isNull();
	}

	@Test
	void shouldIncludeApplicationLiveStreamCountInSystemResources() {
		AdminApplication application = mock(AdminApplication.class);
		IScope rootScope = mock(IScope.class);
		IScope appScope = mock(IScope.class);
		when(application.getRootScope()).thenReturn(rootScope);
		when(application.getApplications()).thenReturn(List.of("app"));
		when(rootScope.getScope("app")).thenReturn(appScope);
		when(application.getAppLiveStreamCount(appScope)).thenReturn(3);

		classUnderTest = new CommonRestService();
		classUnderTest.setApplication(application);

		try (MockedStatic<StatsCollector> statsCollector = mockStatic(StatsCollector.class)) {
			statsCollector.when(() -> StatsCollector.getSystemResourcesInfo(org.mockito.ArgumentMatchers.any()))
					.thenReturn(new JsonObject());

			JsonObject result = JsonParser.parseString(classUnderTest.getSystemResourcesInfo()).getAsJsonObject();

			assertThat(result.get(StatsCollector.TOTAL_LIVE_STREAMS).getAsInt()).isEqualTo(3);
		}
	}

	@Test
	void shouldSkipSpringInjectionWhenDependenciesAreAlreadyInjected() {
		AbstractConsoleDataStore dataStore = mock(AbstractConsoleDataStore.class);
		ConsoleDataStoreFactory dataStoreFactory = mock(ConsoleDataStoreFactory.class);
		when(dataStoreFactory.getDataStore()).thenReturn(dataStore);
		classUnderTest = new CommonRestService();
		classUnderTest.setDataStoreFactory(dataStoreFactory);

		assertThatCode(classUnderTest::initializeSpringDependencies).doesNotThrowAnyException();
	}

	@Test
	void shouldSkipSpringInjectionWithoutServletContext() {
		classUnderTest = new CommonRestService();

		assertThatCode(classUnderTest::initializeSpringDependencies).doesNotThrowAnyException();
	}

	@Test
	void shouldInjectSpringDependenciesIntoJerseyManagedInstance() {
		MockServletContext servletContext = new MockServletContext();
		StaticWebApplicationContext springContext = new StaticWebApplicationContext();
		springContext.setServletContext(servletContext);

		AbstractConsoleDataStore dataStore = mock(AbstractConsoleDataStore.class);
		ConsoleDataStoreFactory dataStoreFactory = mock(ConsoleDataStoreFactory.class);
		when(dataStoreFactory.getDataStore()).thenReturn(dataStore);
		ServerSettings serverSettings = mock(ServerSettings.class);
		IStatsCollector statsCollector = mock(IStatsCollector.class);
		AdminApplication application = mock(AdminApplication.class);

		springContext.getBeanFactory().registerSingleton("dataStoreFactory", dataStoreFactory);
		springContext.getBeanFactory().registerSingleton(ServerSettings.BEAN_NAME, serverSettings);
		springContext.getBeanFactory().registerSingleton(IStatsCollector.BEAN_NAME, statsCollector);
		springContext.getBeanFactory().registerSingleton("web.handler", application);
		springContext.refresh();
		servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, springContext);

		classUnderTest = new CommonRestService();
		ReflectionTestUtils.setField(classUnderTest, "servletContext", servletContext);
		classUnderTest.initializeSpringDependencies();

		assertThat(classUnderTest.getDataStoreFactory()).isSameAs(dataStoreFactory);
		assertThat(classUnderTest.getDataStore()).isSameAs(dataStore);
		assertThat(classUnderTest.getServerSettings()).isSameAs(serverSettings);
		assertThat(classUnderTest.getStatsCollector()).isSameAs(statsCollector);
		assertThat(classUnderTest.getApplication()).isSameAs(application);
		assertThat(classUnderTest.getLicenceServiceInstance()).isNull();

		springContext.close();
	}
}
