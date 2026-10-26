package io.antmedia.console.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import io.antmedia.console.datastore.AbstractConsoleDataStore;
import io.antmedia.datastore.db.types.User;
import io.antmedia.datastore.db.types.UserType;
import io.antmedia.settings.ServerSettings;
import io.antmedia.test.UnitTestBase;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Filter must decide on the path Tomcat routes on, not the raw request URI
 */
@Tag("fast")
class AuthenticationFilterPathTest extends UnitTestBase<AuthenticationFilter> {

	private static final String EMAIL = "readonly@antmedia.io";

	private MockHttpServletResponse response;
	private MockFilterChain chain;

	@BeforeEach
	void setUp() {
		classUnderTest = spy(new AuthenticationFilter());
		doReturn(new ServerSettings()).when(classUnderTest).getServerSettings();

		Map<String, String> appNameUserType = new HashMap<>();
		appNameUserType.put("live", UserType.READ_ONLY.toString());
		User user = mock(User.class);
		when(user.getAppNameUserType()).thenReturn(appNameUserType);
		AbstractConsoleDataStore store = mock(AbstractConsoleDataStore.class);
		when(store.getUser(EMAIL)).thenReturn(user);
		doReturn(store).when(classUnderTest).getAbstractConsoleDataStore();

		response = new MockHttpServletResponse();
		chain = new MockFilterChain();
	}

	// rawUri is what the client sent, servletPath + pathInfo is what Tomcat normalized and routed on
	private MockHttpServletRequest request(String method, String rawUri, String servletPath, String pathInfo) {
		MockHttpServletRequest request = new MockHttpServletRequest(method, rawUri);
		request.setServletPath(servletPath);
		request.setPathInfo(pathInfo);
		return request;
	}

	private MockHttpServletRequest loggedInReadOnly(MockHttpServletRequest request) {
		request.getSession().setAttribute(CommonRestService.IS_AUTHENTICATED, true);
		request.getSession().setAttribute(CommonRestService.USER_EMAIL, EMAIL);
		request.getSession().setAttribute(CommonRestService.USER_PASSWORD, "pass");
		return request;
	}

	@Test
	void shouldRejectDotSegmentWhitelistBypassToProxy() throws Exception {
		MockHttpServletRequest request = request("GET", "/rest/v2/users/x/../../request/blocked", "/rest/v2/request", "/blocked");
		request.setParameter(AuthenticationFilter.DISPATCH_PATH_URL, "live/rest/v2/broadcasts/count?x=");

		classUnderTest.doFilter(request, response, chain);

		assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_FORBIDDEN);
		assertThat(response.getErrorMessage()).isEqualTo("Not authenticated user");
		assertThat(chain.getRequest()).isNull();
	}

	@Test
	void shouldStillAllowRealBlockedStatusWithoutSession() throws Exception {
		MockHttpServletRequest request = request("GET", "/rest/v2/users/a%40b.com/blocked", "/rest", "/v2/users/a@b.com/blocked");

		classUnderTest.doFilter(request, response, chain);

		assertThat(chain.getRequest()).isSameAs(request);
	}

	@Test
	void shouldRejectDotSegmentSupportRequestEscalation() throws Exception {
		MockHttpServletRequest request = loggedInReadOnly(
				request("DELETE", "/rest/v2/support/request/../../applications/live", "/rest", "/v2/applications/live"));

		classUnderTest.doFilter(request, response, chain);

		assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_FORBIDDEN);
		assertThat(chain.getRequest()).isNull();
	}

	@Test
	void shouldStillAllowSupportRequestForReadOnlyUser() throws Exception {
		MockHttpServletRequest request = loggedInReadOnly(request("POST", "/rest/v2/support/request", "/rest", "/v2/support/request"));

		classUnderTest.doFilter(request, response, chain);

		assertThat(chain.getRequest()).isSameAs(request);
	}
}
