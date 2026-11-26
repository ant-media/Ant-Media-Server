package io.antmedia.console.servlet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockServletConfig;

import io.antmedia.console.datastore.AbstractConsoleDataStore;
import io.antmedia.console.rest.AuthenticationFilter;
import io.antmedia.console.rest.CommonRestService;
import io.antmedia.datastore.db.types.User;
import io.antmedia.settings.ServerSettings;
import io.antmedia.test.UnitTestBase;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Tag("fast")
class ProxyServletAuthorizationTest extends UnitTestBase<ProxyServlet> {

	private RecordingProxy proxy;
	private AuthenticationFilter filter;

	@BeforeEach
	void setUpProxyAndUser() throws Exception {
		proxy = new RecordingProxy();
		MockServletConfig config = new MockServletConfig();
		config.addInitParameter("targetUri", "http://localhost:5080/{_path}");
		config.addInitParameter("forwardip", "false");
		proxy.init(config);
		filter = spy(new AuthenticationFilter());
		AbstractConsoleDataStore store = mock(AbstractConsoleDataStore.class);
		when(store.getUser("user@antmedia.test")).thenReturn(
				new User("user@antmedia.test", "password", null, null, Map.of("LiveApp", "USER")));
		doReturn(store).when(filter).getAbstractConsoleDataStore();
		doReturn(new ServerSettings()).when(filter).getServerSettings();
	}

	@AfterEach
	void closeProxy() {
		proxy.destroy();
	}

	@Test
	void shouldRequireTheFilterMarkerEvenWhenTheSessionIsLoggedIn() throws Exception {
		MockHttpServletRequest request = request("GET", null, query("LiveApp/rest/v2/vods"));
		MockHttpServletResponse response = new MockHttpServletResponse();

		proxy.service(request, response);

		assertThat(response.getStatus()).isEqualTo(403);
		assertThat(proxy.forwardedUri).isNull();
	}

	@Test
	void shouldForwardTheAuthorizedPathAndRemainingQueryParameters() throws Exception {
		MockHttpServletRequest request = request("POST", null,
				query("LiveApp/rest/v2/broadcasts/create") + "&autoStart=true");

		MockHttpServletResponse response = dispatch(request);

		assertThat(response.getStatus()).isEqualTo(200);
		assertThat(response.getContentAsString()).isEqualTo("ok");
		assertThat(proxy.forwardedUri).isEqualTo(
				"http://localhost:5080/LiveApp/rest/v2/broadcasts/create?autoStart=true");
	}

	@Test
	void shouldIncludeServletPathInfoInAuthorizationAndForwarding() throws Exception {
		MockHttpServletRequest request = request("POST", "/rest/v2/broadcasts/create", query("LiveApp"));

		assertThat(dispatch(request).getStatus()).isEqualTo(200);
		assertThat(proxy.forwardedUri).isEqualTo("http://localhost:5080/LiveApp/rest/v2/broadcasts/create");
	}

	@Test
	void shouldKeepTheFirstPathInfoSegmentAfterRepeatedSeparators() throws Exception {
		MockHttpServletRequest request = request("POST", "//catalog/rest/v2/broadcasts", query("LiveApp"));

		assertThat(dispatch(request).getStatus()).isEqualTo(403);
		assertThat(proxy.forwardedUri).isNull();
	}

	@Test
	void shouldRecognizeMatrixParametersFromHttpClientPathRewriting() throws Exception {
		String target = "/LiveApp%3Bformat=short/rest/v2/vods";

		assertThat(dispatch(request("POST", null, query(target))).getStatus()).isEqualTo(200);
		HttpRoute loopback = new HttpRoute(new HttpHost("localhost", 5080));
		assertThat(URIUtils.rewriteURIForRoute(URI.create(proxy.forwardedUri), loopback).getRawPath())
				.isEqualTo("/LiveApp;format=short/rest/v2/vods");
	}

	@Test
	void shouldPreserveEncodedQueriesInsideTheTarget() throws Exception {
		String target = "LiveApp/rest/v2/broadcasts/duration?url=http%3A%2F%2Fexample.com%2Fvideo.mp4";

		assertThat(dispatch(request("GET", null, query(target))).getStatus()).isEqualTo(200);
		assertThat(proxy.forwardedUri).isEqualTo("http://localhost:5080/" + target);
	}

	@Test
	void shouldPreserveFormBodiesAndIgnoreTheirDispatchParameters() throws Exception {
		String body = "name=camera&_path=OtherApp/rest/v2/settings";
		MockHttpServletRequest request = spy(request("POST", null, query("LiveApp/rest/v2/broadcasts/create")));
		request.setContentType("application/x-www-form-urlencoded");
		request.setContent(body.getBytes(StandardCharsets.UTF_8));
		request.addHeader("Content-Length", Integer.toString(body.length()));
		request.addParameter(AuthenticationFilter.DISPATCH_PATH_URL, "OtherApp/rest/v2/settings");

		assertThat(dispatch(request).getStatus()).isEqualTo(200);
		assertThat(proxy.forwardedUri).isEqualTo("http://localhost:5080/LiveApp/rest/v2/broadcasts/create");
		assertThat(proxy.forwardedBody).isEqualTo(body);
		verify(request, never()).getParameter(anyString());
		verify(request, never()).getParameterMap();
	}

	@ParameterizedTest
	@ValueSource(strings = {"_path=LiveApp/rest/v2/broadcasts&_path=OtherApp/rest/v2/broadcasts",
			"_path=LiveApp/rest/v2/broadcasts;_path=OtherApp/rest/v2/broadcasts",
			"_path=LiveApp/rest/v2/settings?hint=/rest/v2/broadcasts",
			"_path=LiveApp2/rest/v2/broadcasts"})
	void shouldStopUnauthorizedRequestsBeforeTheUpstreamCall(String rawQuery) throws Exception {
		MockHttpServletResponse response = dispatch(request("POST", null, rawQuery));

		assertThat(response.getStatus()).isEqualTo(403);
		assertThat(response.getErrorMessage()).isEqualTo(AuthenticationFilter.FORBIDDEN_ERROR);
		assertThat(proxy.forwardedUri).isNull();
	}

	private MockHttpServletResponse dispatch(MockHttpServletRequest request) throws Exception {
		MockHttpServletResponse response = new MockHttpServletResponse();
		filter.doFilter(request, response, (incoming, outgoing) ->
				proxy.service((HttpServletRequest) incoming, (HttpServletResponse) outgoing));
		return response;
	}

	private MockHttpServletRequest request(String method, String pathInfo, String query) {
		MockHttpServletRequest request = new MockHttpServletRequest(method, "/rest/v2/request");
		request.setServletPath("/rest/v2/request");
		request.setPathInfo(pathInfo);
		request.setQueryString(query);
		MockHttpSession session = new MockHttpSession();
		session.setAttribute(CommonRestService.IS_AUTHENTICATED, true);
		session.setAttribute(CommonRestService.USER_EMAIL, "user@antmedia.test");
		session.setAttribute(CommonRestService.USER_PASSWORD, "password");
		request.setSession(session);
		return request;
	}

	private String query(String target) {
		return "_path=" + URLEncoder.encode(target, StandardCharsets.UTF_8);
	}

	private static class RecordingProxy extends ProxyServlet {
		private String forwardedUri;
		private String forwardedBody;

		@Override
		protected HttpResponse doExecute(HttpServletRequest request, HttpServletResponse response, HttpRequest upstream)
				throws IOException {
			forwardedUri = upstream.getRequestLine().getUri();
			if (upstream instanceof HttpEntityEnclosingRequest enclosing) {
				forwardedBody = EntityUtils.toString(enclosing.getEntity(), StandardCharsets.UTF_8);
			}
			BasicHttpResponse result = new BasicHttpResponse(HttpVersion.HTTP_1_1, 200, "OK");
			result.setEntity(new StringEntity("ok", StandardCharsets.UTF_8));
			return result;
		}
	}
}
