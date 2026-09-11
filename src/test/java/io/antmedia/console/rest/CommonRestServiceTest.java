package io.antmedia.console.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.List;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.red5.server.api.scope.IScope;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.StaticWebApplicationContext;

import io.antmedia.console.AdminApplication;
import io.antmedia.console.datastore.AbstractConsoleDataStore;
import io.antmedia.console.datastore.ConsoleDataStoreFactory;
import io.antmedia.settings.ServerSettings;
import io.antmedia.statistic.IStatsCollector;
import io.antmedia.statistic.StatsCollector;
import io.antmedia.test.UnitTestBase;

@Tag("fast")
class CommonRestServiceTest extends UnitTestBase<CommonRestService> {

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
