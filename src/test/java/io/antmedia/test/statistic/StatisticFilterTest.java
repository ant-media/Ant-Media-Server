package io.antmedia.test.statistic;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

import org.junit.Test;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.WebApplicationContext;

import io.antmedia.AppSettings;
import io.antmedia.datastore.db.DataStore;
import io.antmedia.datastore.db.DataStoreFactory;
import io.antmedia.filter.HlsStatisticsFilter;
import io.antmedia.statistic.HlsViewerStats;
import io.antmedia.statistic.IStreamStats;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class StatisticFilterTest {

	@Test
	public void shouldCleanupFingerprintWhenViewerIdCookieExists() throws Exception {
		HlsStatisticsFilter filter = new HlsStatisticsFilter();

		FilterConfig filterConfig = mock(FilterConfig.class);
		ServletContext servletContext = mock(ServletContext.class);
		ConfigurableWebApplicationContext context = mock(ConfigurableWebApplicationContext.class);
		IStreamStats streamStats = mock(IStreamStats.class);
		DataStoreFactory dataStoreFactory = mock(DataStoreFactory.class);
		DataStore dataStore = mock(DataStore.class);

		when(filterConfig.getServletContext()).thenReturn(servletContext);
		when(servletContext.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE)).thenReturn(context);
		when(context.isRunning()).thenReturn(true);
		when(context.getBean(HlsViewerStats.BEAN_NAME)).thenReturn(streamStats);
		when(context.getBean(DataStoreFactory.BEAN_NAME)).thenReturn(dataStoreFactory);
		when(context.getBean(AppSettings.BEAN_NAME)).thenReturn(new AppSettings());
		when(dataStoreFactory.getDataStore()).thenReturn(dataStore);
		when(dataStore.isAvailable()).thenReturn(true);

		filter.init(filterConfig);

		String streamId = "stream1";
		String uuid = UUID.randomUUID().toString();
		String fingerprint = "old-fingerprint";
		Cookie viewerCookie = new Cookie("viewerId", uuid + "|" + fingerprint);

		HttpServletRequest request = mock(HttpServletRequest.class);
		HttpServletResponse response = mock(HttpServletResponse.class);
		FilterChain chain = mock(FilterChain.class);

		when(request.getMethod()).thenReturn("GET");
		when(request.getRequestURI()).thenReturn("/LiveApp/streams/" + streamId + ".m3u8");
		when(request.getCookies()).thenReturn(new Cookie[] { viewerCookie });
		when(request.getParameter("subscriberId")).thenReturn(null);
		when(response.getStatus()).thenReturn(HttpServletResponse.SC_OK);

		filter.doFilter(request, response, chain);

		verify(streamStats, times(1)).removeViewerEntry(streamId, fingerprint);
		verify(streamStats, times(1)).registerNewViewer(streamId, uuid, null);
	}

	@Test
			public void shouldUseSubscriberIdIfProvidedOtherwiseUseFingerprintHash() throws Exception {
		HlsStatisticsFilter filter = new HlsStatisticsFilter();

		FilterConfig filterConfig = mock(FilterConfig.class);
		ServletContext servletContext = mock(ServletContext.class);
		ConfigurableWebApplicationContext context = mock(ConfigurableWebApplicationContext.class);
		IStreamStats streamStats = mock(IStreamStats.class);
		DataStoreFactory dataStoreFactory = mock(DataStoreFactory.class);
		DataStore dataStore = mock(DataStore.class);

		when(filterConfig.getServletContext()).thenReturn(servletContext);
		when(servletContext.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE)).thenReturn(context);
		when(context.isRunning()).thenReturn(true);
		when(context.getBean(HlsViewerStats.BEAN_NAME)).thenReturn(streamStats);
		when(context.getBean(DataStoreFactory.BEAN_NAME)).thenReturn(dataStoreFactory);
		when(context.getBean(AppSettings.BEAN_NAME)).thenReturn(new AppSettings());
		when(dataStoreFactory.getDataStore()).thenReturn(dataStore);
		when(dataStore.isAvailable()).thenReturn(true);

		filter.init(filterConfig);

		String streamId = "stream2";
		HttpServletResponse response = mock(HttpServletResponse.class);
		FilterChain chain = mock(FilterChain.class);
		when(response.getStatus()).thenReturn(HttpServletResponse.SC_OK);

		HttpServletRequest withSubscriber = mock(HttpServletRequest.class);
		when(withSubscriber.getMethod()).thenReturn("GET");
		when(withSubscriber.getRequestURI()).thenReturn("/LiveApp/streams/" + streamId + ".m3u8");
		when(withSubscriber.getParameter("subscriberId")).thenReturn("subscriberA");

		filter.doFilter(withSubscriber, response, chain);
		verify(streamStats, times(1)).registerNewViewer(streamId, "subscriberA", "subscriberA");
		verify(streamStats, never()).removeViewerEntry(streamId, "subscriberA");

		HttpServletRequest withoutSubscriber = mock(HttpServletRequest.class);
		when(withoutSubscriber.getMethod()).thenReturn("GET");
		when(withoutSubscriber.getRequestURI()).thenReturn("/LiveApp/streams/" + streamId + ".m3u8");
		when(withoutSubscriber.getParameter("subscriberId")).thenReturn(null);
		when(withoutSubscriber.getCookies()).thenReturn(null);
		when(withoutSubscriber.getHeader("X-Forwarded-For")).thenReturn(null);
		when(withoutSubscriber.getRemoteAddr()).thenReturn("10.1.2.3");
		when(withoutSubscriber.getHeader("User-Agent")).thenReturn("UA-Test");
		when(withoutSubscriber.getHeader("Accept-Language")).thenReturn("en-US");

		filter.doFilter(withoutSubscriber, response, chain);

		String expectedHash = sha256("10.1.2.3|UA-Test|en-US");
		verify(streamStats, times(1)).registerNewViewer(streamId, expectedHash, null);
	}

	private String sha256(String value) throws Exception {
		MessageDigest md = MessageDigest.getInstance("SHA-256");
		byte[] hash = md.digest(value.getBytes(StandardCharsets.UTF_8));
		StringBuilder sb = new StringBuilder();
		for (byte b : hash) {
			sb.append(String.format("%02x", b));
		}
		return sb.toString();
	}
}
