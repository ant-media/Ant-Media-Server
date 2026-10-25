package io.antmedia.console.servlet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.http.HttpRequest;
import org.apache.http.message.BasicHttpRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.WebApplicationContext;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.AppSettings;
import io.antmedia.console.AdminApplication;
import io.antmedia.console.rest.AuthenticationFilter;
import io.antmedia.filter.JWTFilter;
import io.antmedia.test.UnitTestBase;

@Tag("fast")
class ProxyServletTest extends UnitTestBase<ProxyServlet> {

	private static final String SECRET = "testtesttesttesttesttesttesttest";
	private static final String BROWSER_TOKEN = "browser-token";

	private AppSettings appSettings;
	private AdminApplication adminApplication;
	private MockHttpServletRequest request;
	private HttpRequest proxyRequest;

	@BeforeEach
	void setUp() {
		classUnderTest = new ProxyServlet();

		appSettings = new AppSettings();
		appSettings.setJwtControlEnabled(true);
		appSettings.setJwtSecretKey(SECRET);

		AntMediaApplicationAdapter adaptor = mock(AntMediaApplicationAdapter.class);
		when(adaptor.getAppSettings()).thenReturn(appSettings);
		ApplicationContext appContext = mock(ApplicationContext.class);
		when(appContext.getBean(AntMediaApplicationAdapter.BEAN_NAME)).thenReturn(adaptor);
		adminApplication = mock(AdminApplication.class);
		when(adminApplication.getApplicationContext("live")).thenReturn(appContext);
		WebApplicationContext rootContext = mock(WebApplicationContext.class);
		when(rootContext.getBean("web.handler")).thenReturn(adminApplication);

		request = new MockHttpServletRequest();
		request.getServletContext().setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, rootContext);
		request.setParameter(AuthenticationFilter.DISPATCH_PATH_URL, "live/rest/v2/broadcasts/list/0/10");
		request.addHeader(JWTFilter.JWT_TOKEN_AUTHORIZATION_HEADER, BROWSER_TOKEN);

		proxyRequest = new BasicHttpRequest("GET", "/live/rest/v2/broadcasts/list/0/10");
	}

	private String forwardedAuthorization() {
		classUnderTest.copyRequestHeaders(request, proxyRequest);
		return proxyRequest.getFirstHeader(JWTFilter.JWT_TOKEN_AUTHORIZATION_HEADER).getValue();
	}

	@Test
	void shouldReplaceBrowserTokenWithServerSignedToken() {
		String token = forwardedAuthorization();

		assertThat(token).isNotEqualTo(BROWSER_TOKEN);
		assertThat(JWTFilter.isJWTTokenValid(SECRET, token)).isTrue();
		assertThat(proxyRequest.getHeaders(JWTFilter.JWT_TOKEN_AUTHORIZATION_HEADER)).hasSize(1);
	}

	@Test
	void shouldResolveAppNameWithLeadingSlash() {
		request.setParameter(AuthenticationFilter.DISPATCH_PATH_URL, "/live/rest/v2/broadcasts/count");

		assertThat(JWTFilter.isJWTTokenValid(SECRET, forwardedAuthorization())).isTrue();
	}

	@Test
	void shouldNotTouchHeaderWhenJwtDisabled() {
		appSettings.setJwtControlEnabled(false);

		assertThat(forwardedAuthorization()).isEqualTo(BROWSER_TOKEN);
	}

	@Test
	void shouldNotTouchHeaderForJwksApp() {
		appSettings.setJwksURL("https://example.com/.well-known/jwks.json");

		assertThat(forwardedAuthorization()).isEqualTo(BROWSER_TOKEN);
	}

	@Test
	void shouldNotTouchHeaderWhenAppNotFound() {
		request.setParameter(AuthenticationFilter.DISPATCH_PATH_URL, "unknown/rest/v2/broadcasts/count");

		assertThat(forwardedAuthorization()).isEqualTo(BROWSER_TOKEN);
	}

	@Test
	void shouldNotTouchHeaderWithoutPathOrRootContext() {
		request.removeParameter(AuthenticationFilter.DISPATCH_PATH_URL);
		assertThat(forwardedAuthorization()).isEqualTo(BROWSER_TOKEN);

		request.setParameter(AuthenticationFilter.DISPATCH_PATH_URL, "live/rest/v2/broadcasts/count");
		request.getServletContext().removeAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
		proxyRequest = new BasicHttpRequest("GET", "/live/rest/v2/broadcasts/count");
		assertThat(forwardedAuthorization()).isEqualTo(BROWSER_TOKEN);
	}
}
