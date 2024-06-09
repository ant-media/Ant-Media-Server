package io.antmedia.test.filter;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.WebApplicationContext;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.datastore.db.DataStore;
import io.antmedia.datastore.db.DataStoreFactory;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.filter.DashStatisticsFilter;
import io.antmedia.statistic.DashViewerStats;
import io.antmedia.statistic.IStreamStats;

public class DashStatisticsFilterTest {
	
	private DashStatisticsFilter dashStatisticsFilter;

	protected static Logger logger = LoggerFactory.getLogger(DashStatisticsFilterTest.class);


	@Before
	public void before() {
		dashStatisticsFilter = new DashStatisticsFilter();
	}
	
	@After
	public void after() {
		dashStatisticsFilter = null;
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
		
		dashStatisticsFilter.setConfig(filterconfig);
		
		
		assertNull(dashStatisticsFilter.getStreamStats(DashViewerStats.BEAN_NAME));
		
		
		
		HttpServletRequest mockRequest = mock(HttpServletRequest.class);
		HttpServletResponse mockResponse = mock(HttpServletResponse.class);
		FilterChain mockChain = mock(FilterChain.class);
		
		HttpSession session = mock(HttpSession.class);
		String sessionId = RandomStringUtils.randomAlphanumeric(16);
		when(session.getId()).thenReturn(sessionId);
		when(mockRequest.getSession()).thenReturn(session);
		when(mockRequest.getMethod()).thenReturn("GET");
		
		String streamId = RandomStringUtils.randomAlphanumeric(8);
		when(mockRequest.getRequestURI()).thenReturn("/LiveApp/streams/"+streamId+"/"+streamId+".m4s");
		
		when(mockResponse.getStatus()).thenReturn(HttpServletResponse.SC_OK);
		
		try {
			dashStatisticsFilter.doFilter(mockRequest, mockResponse, mockChain);
		} catch (IOException e) {
			logger.error(ExceptionUtils.getStackTrace(e));
			fail(ExceptionUtils.getStackTrace(e));
		} catch (ServletException e) {
			logger.error(ExceptionUtils.getStackTrace(e));
			fail(ExceptionUtils.getStackTrace(e));
		}
	}
	
	@Test
	public void testGetApplicationAdaptor() {
		FilterConfig filterconfig = mock(FilterConfig.class);
		ServletContext servletContext = mock(ServletContext.class);
		ConfigurableWebApplicationContext context = mock(ConfigurableWebApplicationContext.class);
		
		when(context.isRunning()).thenReturn(true);
		IStreamStats streamStats = mock(IStreamStats.class);
		
		when(context.getBean(DashViewerStats.BEAN_NAME)).thenReturn(streamStats);
		
		when(servletContext.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE))
				.thenReturn(context);
		
		when(filterconfig.getServletContext()).thenReturn(servletContext);
		
		try {
			DashStatisticsFilter dashStatisticsFilterSpy = Mockito.spy(dashStatisticsFilter);
			dashStatisticsFilterSpy.init(filterconfig);
			
			assertNull(dashStatisticsFilterSpy.getAntMediaApplicationAdapter());
			
			when(context.getBean(AntMediaApplicationAdapter.BEAN_NAME)).thenReturn(Mockito.mock(AntMediaApplicationAdapter.class));
			assertNotNull(dashStatisticsFilterSpy.getAntMediaApplicationAdapter());

			
		}
		catch (Exception e) {
			logger.error(ExceptionUtils.getStackTrace(e));
			fail(ExceptionUtils.getStackTrace(e));
		}
	}
	
	@Test
	public void testDoFilterForNotFound() {
		FilterConfig filterconfig = mock(FilterConfig.class);
		ServletContext servletContext = mock(ServletContext.class);
		ConfigurableWebApplicationContext context = mock(ConfigurableWebApplicationContext.class);
		
		when(context.isRunning()).thenReturn(true);
		IStreamStats streamStats = mock(IStreamStats.class);
		
		when(context.getBean(DashViewerStats.BEAN_NAME)).thenReturn(streamStats);
		
		when(servletContext.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE))
				.thenReturn(context);
		
		when(filterconfig.getServletContext()).thenReturn(servletContext);
		
		try {
			DashStatisticsFilter dashStatisticsFilterSpy = Mockito.spy(dashStatisticsFilter);
			dashStatisticsFilterSpy.init(filterconfig);
			
			HttpServletRequest mockRequest = mock(HttpServletRequest.class);
			HttpServletResponse mockResponse = mock(HttpServletResponse.class);
			FilterChain mockChain = mock(FilterChain.class);
			
			HttpSession session = mock(HttpSession.class);
			String sessionId = RandomStringUtils.randomAlphanumeric(16);
			when(session.getId()).thenReturn(sessionId);
			when(mockRequest.getSession()).thenReturn(session);
			when(mockRequest.getMethod()).thenReturn("GET");
			
			String streamId = RandomStringUtils.randomAlphanumeric(8);
			when(mockRequest.getRequestURI()).thenReturn("/LiveApp/streams/"+streamId+"/"+streamId+"m4s");
			
			when(mockResponse.getStatus()).thenReturn(HttpServletResponse.SC_NOT_FOUND);
			
			DataStoreFactory dsf = mock(DataStoreFactory.class);		
			when(context.getBean(DataStoreFactory.BEAN_NAME)).thenReturn(dsf);
			
			DataStore dataStore = mock(DataStore.class);
			when(dataStore.isAvailable()).thenReturn(true);
			when(dsf.getDataStore()).thenReturn(dataStore);
			
			AntMediaApplicationAdapter appAdaptor = Mockito.mock(AntMediaApplicationAdapter.class);
			doReturn(appAdaptor).when(dashStatisticsFilterSpy).getAntMediaApplicationAdapter();
			

			logger.info("session id {}, stream id {}", sessionId, streamId);
			dashStatisticsFilterSpy.doFilter(mockRequest, mockResponse, mockChain);
			Mockito.verify(dashStatisticsFilterSpy, times(1)).startStreamingIfAutoStartStopEnabled(mockRequest, streamId);
			Mockito.verify(appAdaptor, Mockito.never()).startStreaming(Mockito.any());

			
			when(mockRequest.getMethod()).thenReturn("HEAD");
			dashStatisticsFilterSpy.doFilter(mockRequest, mockResponse, mockChain);
			Mockito.verify(dashStatisticsFilterSpy, times(2)).startStreamingIfAutoStartStopEnabled(mockRequest, streamId);
			Mockito.verify(appAdaptor, Mockito.never()).startStreaming(Mockito.any());


			Broadcast broadcast = mock(Broadcast.class);
			Mockito.doReturn(broadcast).when(dashStatisticsFilterSpy).getBroadcast(Mockito.any(), Mockito.any());
			dashStatisticsFilterSpy.doFilter(mockRequest, mockResponse, mockChain);
			Mockito.verify(dashStatisticsFilterSpy, times(3)).startStreamingIfAutoStartStopEnabled(mockRequest, streamId);
			Mockito.verify(appAdaptor, Mockito.times(0)).startStreaming(Mockito.any());

			Mockito.when(broadcast.isAutoStartStopEnabled()).thenReturn(true);
			dashStatisticsFilterSpy.doFilter(mockRequest, mockResponse, mockChain);

			Mockito.verify(dashStatisticsFilterSpy, times(4)).startStreamingIfAutoStartStopEnabled(mockRequest, streamId);
			Mockito.verify(appAdaptor, Mockito.times(1)).startStreaming(Mockito.any());
			
			{
				when(mockRequest.getRequestURI()).thenReturn("/LiveApp/streams");
				dashStatisticsFilterSpy.doFilter(mockRequest, mockResponse, mockChain);
				//it will not change the times because there is no stream id
				Mockito.verify(dashStatisticsFilterSpy, times(4)).startStreamingIfAutoStartStopEnabled(mockRequest, streamId);
				Mockito.verify(appAdaptor, Mockito.times(1)).startStreaming(Mockito.any());
				
				when(mockRequest.getMethod()).thenReturn("GET");
				dashStatisticsFilterSpy.doFilter(mockRequest, mockResponse, mockChain);
				//it will not change the times because there is no stream id
				Mockito.verify(dashStatisticsFilterSpy, times(4)).startStreamingIfAutoStartStopEnabled(mockRequest, streamId);
				Mockito.verify(appAdaptor, Mockito.times(1)).startStreaming(Mockito.any());
				

			}
			
			{
				when(mockResponse.getStatus()).thenReturn(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				dashStatisticsFilterSpy.doFilter(mockRequest, mockResponse, mockChain);
				//it will not change the times because internal server error
				Mockito.verify(dashStatisticsFilterSpy, times(4)).startStreamingIfAutoStartStopEnabled(mockRequest, streamId);
				Mockito.verify(appAdaptor, Mockito.times(1)).startStreaming(Mockito.any());
				
				when(mockRequest.getRequestURI()).thenReturn("/LiveApp/streams/"+streamId+"/"+streamId+"m4s");
				dashStatisticsFilterSpy.doFilter(mockRequest, mockResponse, mockChain);
				//it will not change the times becauser internal server error
				Mockito.verify(dashStatisticsFilterSpy, times(4)).startStreamingIfAutoStartStopEnabled(mockRequest, streamId);
				Mockito.verify(appAdaptor, Mockito.times(1)).startStreaming(Mockito.any());

			}
			{
				when(mockRequest.getMethod()).thenReturn("POST");
				dashStatisticsFilterSpy.doFilter(mockRequest, mockResponse, mockChain);
				//it will not change the times because post request
				Mockito.verify(dashStatisticsFilterSpy, times(4)).startStreamingIfAutoStartStopEnabled(mockRequest, streamId);
				Mockito.verify(appAdaptor, Mockito.times(1)).startStreaming(Mockito.any());
			}
		
			
		} catch (Exception e) {
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
		
		when(context.getBean(DashViewerStats.BEAN_NAME)).thenReturn(streamStats);
		
		when(servletContext.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE))
				.thenReturn(context);
		
		when(filterconfig.getServletContext()).thenReturn(servletContext);
		
		
		try {
			dashStatisticsFilter.init(filterconfig);
			//when(dashStatisticsFilter.getStreamStats()).thenReturn(streamStats);
			
			HttpServletRequest mockRequest = mock(HttpServletRequest.class);
			HttpServletResponse mockResponse = mock(HttpServletResponse.class);
			FilterChain mockChain = mock(FilterChain.class);
			
			HttpSession session = mock(HttpSession.class);
			String sessionId = RandomStringUtils.randomAlphanumeric(16);
			when(session.getId()).thenReturn(sessionId);
			when(mockRequest.getSession()).thenReturn(session);
			when(mockRequest.getMethod()).thenReturn("GET");
			
			String streamId = RandomStringUtils.randomAlphanumeric(8);
			when(mockRequest.getRequestURI()).thenReturn("/LiveApp/streams/"+streamId+"/"+streamId+"m4s");
			
			when(mockResponse.getStatus()).thenReturn(HttpServletResponse.SC_OK);
			
			DataStoreFactory dsf = mock(DataStoreFactory.class);		
			when(context.getBean(DataStoreFactory.BEAN_NAME)).thenReturn(dsf);
			
			DataStore dataStore = mock(DataStore.class);
			when(dataStore.isAvailable()).thenReturn(true);
			when(dsf.getDataStore()).thenReturn(dataStore);

			logger.info("session id {}, stream id {}", sessionId, streamId);
			dashStatisticsFilter.doFilter(mockRequest, mockResponse, mockChain);
			
			
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
	public void testDASHViewerLimit() {
		String streamId = RandomStringUtils.randomAlphanumeric(8);

		FilterConfig filterconfig = mock(FilterConfig.class);
		ServletContext servletContext = mock(ServletContext.class);
		ConfigurableWebApplicationContext context = mock(ConfigurableWebApplicationContext.class);
		
		IStreamStats streamStats = mock(IStreamStats.class);
		
		when(context.getBean(DashViewerStats.BEAN_NAME)).thenReturn(streamStats);
		
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
		broadcast.setDashViewerLimit(2);
		when(dataStore.get(streamId)).thenReturn(broadcast);

		
		try {
			dashStatisticsFilter.init(filterconfig);
			//when(dashStatisticsFilter.getStreamStats()).thenReturn(streamStats);
			
			String sessionId = requestDash(streamId);		
			verify(streamStats, times(1)).registerNewViewer(streamId, sessionId, null);
			broadcast.setDashViewerCount(1);
			
			String sessionId2 = requestDash(streamId);		
			verify(streamStats, times(1)).registerNewViewer(streamId, sessionId2, null);
			broadcast.setDashViewerCount(2);

			String sessionId3 = requestDash(streamId);		
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

	private String requestDash(String streamId) throws IOException, ServletException {
		HttpServletRequest mockRequest = mock(HttpServletRequest.class);
		HttpServletResponse mockResponse = mock(HttpServletResponse.class);
		FilterChain mockChain = mock(FilterChain.class);
		
		HttpSession session = mock(HttpSession.class);
		String sessionId = RandomStringUtils.randomAlphanumeric(16);
		when(session.getId()).thenReturn(sessionId);
		when(mockRequest.getSession()).thenReturn(session);
		when(mockRequest.getMethod()).thenReturn("GET");
		
		when(mockRequest.getRequestURI()).thenReturn("LliveApp/streams/"+streamId+ "/" + streamId + "_0segment00139.m4s");
		
		when(mockResponse.getStatus()).thenReturn(HttpServletResponse.SC_OK);

		logger.info("session id {}, stream id {}", sessionId, streamId);
		dashStatisticsFilter.doFilter(mockRequest, mockResponse, mockChain);
		return sessionId;
	}

}
