package io.antmedia.test.filter;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.WebApplicationContext;

import io.antmedia.datastore.db.DataStore;
import io.antmedia.datastore.db.DataStoreFactory;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.filter.HlsStatisticsFilter;
import io.antmedia.statistic.HlsViewerStats;
import io.antmedia.statistic.IStreamStats;

public class HlsStatisticsFilterTest {
	
	private HlsStatisticsFilter hlsStatisticsFilter;

	protected static Logger logger = LoggerFactory.getLogger(HlsStatisticsFilterTest.class);


	@Before
	public void before() {
		hlsStatisticsFilter = new HlsStatisticsFilter();
	}
	
	@After
	public void after() {
		hlsStatisticsFilter = null;
	}
	
	@Rule
	public TestRule watcher = new TestWatcher() {
	   protected void starting(Description description) {
	      System.out.println("Starting test: " + description.getMethodName());
	   }
	   
	   protected void failed(Throwable e, Description description) {
		   System.out.println("Failed test: " + description.getMethodName());
	   };
	   protected void finished(Description description) {
		   System.out.println("Finishing test: " + description.getMethodName());
	   };
	};
	
	@Test
	public void testUninitialized() {
		FilterConfig filterconfig = mock(FilterConfig.class);
		
		ServletContext servletContext = mock(ServletContext.class);
		ApplicationContext context = mock(ConfigurableWebApplicationContext.class);
		when(servletContext.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE))
		.thenReturn(context);
		
		when(filterconfig.getServletContext()).thenReturn(servletContext);
		
		hlsStatisticsFilter.setConfig(filterconfig);
		
		
		assertNull(hlsStatisticsFilter.getStreamStats());
		
		
		
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
		
		try {
			hlsStatisticsFilter.doFilter(mockRequest, mockResponse, mockChain);
		} catch (IOException e) {
			logger.error(ExceptionUtils.getStackTrace(e));
			fail(ExceptionUtils.getStackTrace(e));
		} catch (ServletException e) {
			logger.error(ExceptionUtils.getStackTrace(e));
			fail(ExceptionUtils.getStackTrace(e));
		}
	}
	
	@Test
	public void testDoFilter() {
		FilterConfig filterconfig = mock(FilterConfig.class);
		ServletContext servletContext = mock(ServletContext.class);
		ConfigurableWebApplicationContext context = mock(ConfigurableWebApplicationContext.class);
		
		when(context.isRunning()).thenReturn(true);
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
			
			DataStoreFactory dsf = mock(DataStoreFactory.class);		
			when(context.getBean(DataStoreFactory.BEAN_NAME)).thenReturn(dsf);
			
			DataStore dataStore = mock(DataStore.class);
			when(dataStore.isAvailable()).thenReturn(true);
			when(dsf.getDataStore()).thenReturn(dataStore);

			logger.info("session id {}, stream id {}", sessionId, streamId);
			hlsStatisticsFilter.doFilter(mockRequest, mockResponse, mockChain);
			
			
			verify(streamStats, times(1)).registerNewViewer(streamId, sessionId, null);
			
			
			
		} catch (ServletException|IOException e) {
			logger.error(ExceptionUtils.getStackTrace(e));
			fail(ExceptionUtils.getStackTrace(e));
		} 
		catch (Exception e) {
			logger.error(ExceptionUtils.getStackTrace(e));
			fail(ExceptionUtils.getStackTrace(e));
		}
		
		
	}
	
	@Test
	public void testHLSViewerLimit() {
		String streamId = RandomStringUtils.randomAlphanumeric(8);

		FilterConfig filterconfig = mock(FilterConfig.class);
		ServletContext servletContext = mock(ServletContext.class);
		ConfigurableWebApplicationContext context = mock(ConfigurableWebApplicationContext.class);
		
		IStreamStats streamStats = mock(IStreamStats.class);
		when(context.getBean(HlsViewerStats.BEAN_NAME)).thenReturn(streamStats);
		
		when(context.isRunning()).thenReturn(true);
		DataStoreFactory dsf = mock(DataStoreFactory.class);		
		when(context.getBean(DataStoreFactory.BEAN_NAME)).thenReturn(dsf);
		
		when(servletContext.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE))
				.thenReturn(context);
		
		when(filterconfig.getServletContext()).thenReturn(servletContext);
		
		DataStore dataStore = mock(DataStore.class);
		when(dataStore.isAvailable()).thenReturn(true);
		when(dsf.getDataStore()).thenReturn(dataStore);

		Broadcast broadcast = new Broadcast();
		broadcast.setHlsViewerLimit(2);
		when(dataStore.get(streamId)).thenReturn(broadcast);

		
		try {
			hlsStatisticsFilter.init(filterconfig);
			
			String sessionId = requestHls(streamId);		
			verify(streamStats, times(1)).registerNewViewer(streamId, sessionId, null);
			broadcast.setHlsViewerCount(1);
			
			String sessionId2 = requestHls(streamId);		
			verify(streamStats, times(1)).registerNewViewer(streamId, sessionId2, null);
			broadcast.setHlsViewerCount(2);

			String sessionId3 = requestHls(streamId);		
			verify(streamStats, never()).registerNewViewer(streamId, sessionId3, null);
		} catch (ServletException|IOException e) {
			logger.error(ExceptionUtils.getStackTrace(e));
			fail(ExceptionUtils.getStackTrace(e));
		} 
		catch (Exception e) {
			logger.error(ExceptionUtils.getStackTrace(e));
			fail(ExceptionUtils.getStackTrace(e));
		}
		
		
	}

	private String requestHls(String streamId) throws IOException, ServletException {
		HttpServletRequest mockRequest = mock(HttpServletRequest.class);
		HttpServletResponse mockResponse = mock(HttpServletResponse.class);
		FilterChain mockChain = mock(FilterChain.class);
		
		HttpSession session = mock(HttpSession.class);
		String sessionId = RandomStringUtils.randomAlphanumeric(16);
		when(session.getId()).thenReturn(sessionId);
		when(mockRequest.getSession()).thenReturn(session);
		when(mockRequest.getMethod()).thenReturn("GET");
		
		when(mockRequest.getRequestURI()).thenReturn("/LiveApp/streams/"+streamId+".m3u8");
		
		when(mockResponse.getStatus()).thenReturn(HttpServletResponse.SC_OK);

		logger.info("session id {}, stream id {}", sessionId, streamId);
		hlsStatisticsFilter.doFilter(mockRequest, mockResponse, mockChain);
		return sessionId;
	}

}
