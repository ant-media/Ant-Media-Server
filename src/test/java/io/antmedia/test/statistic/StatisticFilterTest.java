package io.antmedia.test.statistic;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
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

@Tag("fast")
public class StatisticFilterTest {

	private static final String COOKIE_HEADER = "Set-Cookie";

	@Test
	public void shouldMigrateFingerprintEntryWhenViewerIdCookieComesBack() throws Exception {
		IStreamStats streamStats = mock(IStreamStats.class);
		HlsStatisticsFilter filter = newFilter(streamStats);

		String streamId = "stream1";
		String uuid = UUID.randomUUID().toString();
		String fingerprint = "old-fingerprint";

		HttpServletRequest request = mock(HttpServletRequest.class);
		HttpServletResponse response = mock(HttpServletResponse.class);
		FilterChain chain = mock(FilterChain.class);

		when(request.getMethod()).thenReturn("GET");
		when(request.getRequestURI()).thenReturn("/LiveApp/streams/" + streamId + ".m3u8");
		when(request.getCookies()).thenReturn(new Cookie[] { new Cookie("viewerId", uuid + "|" + fingerprint) });
		when(request.getParameter("subscriberId")).thenReturn(null);
		when(response.getStatus()).thenReturn(HttpServletResponse.SC_OK);

		filter.doFilter(request, response, chain);

		verify(streamStats, times(1)).migrateViewerEntry(streamId, fingerprint, uuid);
		verify(streamStats, times(1)).registerNewViewer(streamId, uuid, null);
		//the fingerprint is dropped from the cookie so the migration is requested only once
		verify(response, times(1)).addHeader(COOKIE_HEADER, "viewerId=" + uuid + "; Path=/; HttpOnly; SameSite=Lax");
	}

	@Test
	public void shouldNotMigrateAgainOnceTheCookieOnlyCarriesTheUuid() throws Exception {
		IStreamStats streamStats = mock(IStreamStats.class);
		HlsStatisticsFilter filter = newFilter(streamStats);

		String streamId = "stream3";
		String uuid = UUID.randomUUID().toString();

		HttpServletRequest request = mock(HttpServletRequest.class);
		HttpServletResponse response = mock(HttpServletResponse.class);
		FilterChain chain = mock(FilterChain.class);

		when(request.getMethod()).thenReturn("GET");
		when(request.getRequestURI()).thenReturn("/LiveApp/streams/" + streamId + ".m3u8");
		when(request.getCookies()).thenReturn(new Cookie[] { new Cookie("viewerId", uuid) });
		when(request.getParameter("subscriberId")).thenReturn(null);
		when(response.getStatus()).thenReturn(HttpServletResponse.SC_OK);

		filter.doFilter(request, response, chain);
		filter.doFilter(request, response, chain);

		verify(streamStats, times(2)).registerNewViewer(streamId, uuid, null);
		verify(streamStats, never()).migrateViewerEntry(anyString(), anyString(), anyString());
		//the viewer already carries its final id, no cookie is handed out again
		verify(response, never()).addHeader(eq(COOKIE_HEADER), anyString());
	}

	@Test
	public void shouldCountUnderTheFingerprintAndStillReportTheSubscriberId() throws Exception {
		IStreamStats streamStats = mock(IStreamStats.class);
		HlsStatisticsFilter filter = newFilter(streamStats);

		String streamId = "stream2";
		HttpServletResponse response = mock(HttpServletResponse.class);
		FilterChain chain = mock(FilterChain.class);
		when(response.getStatus()).thenReturn(HttpServletResponse.SC_OK);

		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getMethod()).thenReturn("GET");
		when(request.getRequestURI()).thenReturn("/LiveApp/streams/" + streamId + ".m3u8");
		when(request.getCookies()).thenReturn(null);
		when(request.getRemoteAddr()).thenReturn("10.1.2.3");
		when(request.getHeader("User-Agent")).thenReturn("UA-Test");
		when(request.getHeader("Accept-Language")).thenReturn("en-US");
		String expectedHash = sha256("10.1.2.3|UA-Test|en-US");

		//the subscriber is counted under its own viewer id, the subscriber id is only reported
		when(request.getParameter("subscriberId")).thenReturn("subscriberA");
		filter.doFilter(request, response, chain);
		verify(streamStats, times(1)).registerNewViewer(streamId, expectedHash, "subscriberA");

		when(request.getParameter("subscriberId")).thenReturn(null);
		filter.doFilter(request, response, chain);
		verify(streamStats, times(1)).registerNewViewer(streamId, expectedHash, null);
	}

	@Test
	public void shouldOnlyHandOutTheCookieOnPlaylistRequests() throws Exception {
		IStreamStats streamStats = mock(IStreamStats.class);
		HlsStatisticsFilter filter = newFilter(streamStats);

		HttpServletRequest segment = mock(HttpServletRequest.class);
		HttpServletResponse response = mock(HttpServletResponse.class);
		FilterChain chain = mock(FilterChain.class);

		when(segment.getMethod()).thenReturn("GET");
		when(segment.getRequestURI()).thenReturn("/LiveApp/streams/stream4000000001.ts");
		when(segment.getCookies()).thenReturn(null);
		when(segment.getParameter("subscriberId")).thenReturn(null);
		when(response.getStatus()).thenReturn(HttpServletResponse.SC_OK);

		filter.doFilter(segment, response, chain);

		//the viewer is still counted, it just has to ask for the playlist to get a cookie
		verify(streamStats, times(1)).registerNewViewer(eq("stream4"), anyString(), isNull());
		verify(response, never()).addHeader(eq(COOKIE_HEADER), anyString());
	}

	private HlsStatisticsFilter newFilter(IStreamStats streamStats) throws Exception {
		FilterConfig filterConfig = mock(FilterConfig.class);
		ServletContext servletContext = mock(ServletContext.class);
		ConfigurableWebApplicationContext context = mock(ConfigurableWebApplicationContext.class);
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

		HlsStatisticsFilter filter = new HlsStatisticsFilter();
		filter.init(filterConfig);
		return filter;
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
