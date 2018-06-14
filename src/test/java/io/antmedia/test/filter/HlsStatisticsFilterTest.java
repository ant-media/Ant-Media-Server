package io.antmedia.test.filter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.WebApplicationContext;

import static org.mockito.Mockito.*;

import java.io.IOException;

import io.antmedia.filter.HlsStatisticsFilter;
import io.antmedia.statistic.HlsViewerStats;
import io.antmedia.statistic.IStreamStats;

public class HlsStatisticsFilterTest {
	
	private HlsStatisticsFilter hlsStatisticsFilter;


	@Before
	public void before() {
		hlsStatisticsFilter = new HlsStatisticsFilter();
	}
	
	public void after() {
		hlsStatisticsFilter = null;
	}
	
	
	@Test
	public void testGetStreamId() {
		String streamId = "stream_id_knhbgv";
		assertEquals(streamId, HlsStatisticsFilter.getStreamId("/liveapp/streams/"+streamId+"_adaptive.m3u8"));
		
		assertEquals(streamId, HlsStatisticsFilter.getStreamId("/liveapp/streams/"+streamId+".m3u8"));
		
		assertEquals(streamId, HlsStatisticsFilter.getStreamId("/liveapp/streams/"+streamId+"_240p.m3u8"));
		
		assertNull(HlsStatisticsFilter.getStreamId("/liveapp/streams/"+streamId+".u8"));
	}
	
	@Test
	public void testDoFilter() {
		FilterConfig filterconfig = mock(FilterConfig.class);
		ServletContext servletContext = mock(ServletContext.class);
		ApplicationContext context = mock(ApplicationContext.class);
		
		IStreamStats streamStats = mock(IStreamStats.class);
		
		when(context.getBean(HlsViewerStats.BEAN_NAME)).thenReturn(streamStats);
		
		when(servletContext.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE))
				.thenReturn(context);
		
		when(filterconfig.getServletContext()).thenReturn(servletContext);
		
		
		try {
			hlsStatisticsFilter.init(filterconfig);
			
			HttpServletRequest mockRequest = mock(HttpServletRequest.class);
			HttpServletResponse mockResponse = mock(HttpServletResponse.class);
			FilterChain mockChain = mock(FilterChain.class);
			
			HttpSession session = mock(HttpSession.class);
			String sessionId = RandomStringUtils.randomAlphanumeric(16);
			when(session.getId()).thenReturn(sessionId);
			when(mockRequest.getSession()).thenReturn(session);
			when(mockRequest.getMethod()).thenReturn("GET");
			
			String streamId = RandomStringUtils.randomAlphanumeric(8);
			when(mockRequest.getRequestURI()).thenReturn("/LiveApp/streams/"+streamId+".m3u8");
			
			when(mockResponse.getStatus()).thenReturn(HttpServletResponse.SC_OK);
			
			hlsStatisticsFilter.doFilter(mockRequest, mockResponse, mockChain);
			
			
			verify(streamStats, times(1)).registerNewViewer(streamId, sessionId);
			
			
			
		} catch (ServletException|IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} 
		
		
	}

}
