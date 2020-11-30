package io.antmedia.test.token;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
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
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.WebApplicationContext;

import io.antmedia.AppSettings;
import io.antmedia.datastore.db.types.Token;
import io.antmedia.filter.TokenFilterManager;
import io.antmedia.filter.TokenGenerator;
import io.antmedia.muxer.MuxAdaptor;
import io.antmedia.security.ITokenService;
import io.antmedia.security.MockTokenService;


public class TokenFilterTest {
	protected static Logger logger = LoggerFactory.getLogger(TokenFilterTest.class);

	private TokenFilterManager tokenFilter;

	@Before
	public void before() {
		tokenFilter = new TokenFilterManager();
	}

	@After
	public void after() {
		tokenFilter = null;
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
	public void testUninitializedBean() {
		FilterConfig filterconfig = mock(FilterConfig.class);
	
		ServletContext servletContext = mock(ServletContext.class);
		ConfigurableWebApplicationContext context = mock(ConfigurableWebApplicationContext.class);
		when(context.isRunning()).thenReturn(true);
		when(servletContext.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE))
		.thenReturn(context);
		
		when(filterconfig.getServletContext()).thenReturn(servletContext);
		
		tokenFilter.setConfig(filterconfig);
		
		assertNull(tokenFilter.getAppSettings());
		
		HttpServletRequest mockRequest = mock(HttpServletRequest.class);
		
		HttpSession session = mock(HttpSession.class);
		
		when(mockRequest.getSession()).thenReturn(session);
		when(mockRequest.getMethod()).thenReturn("GET");
		
		String clientIP = "10.0.0.1";
		when(mockRequest.getRemoteAddr()).thenReturn(clientIP);

		String tokenId = RandomStringUtils.randomAlphanumeric(8);
		when(mockRequest.getParameter("token")).thenReturn(tokenId);

		String streamId = RandomStringUtils.randomAlphanumeric(8);
		when(mockRequest.getRequestURI()).thenReturn("/LiveApp/streams/"+streamId+".m3u8");
		
		FilterChain mockChain = mock(FilterChain.class);
		HttpServletResponse mockResponse = mock(HttpServletResponse.class);
		
		when(mockResponse.getStatus()).thenReturn(HttpServletResponse.SC_OK);
		
		
		try {
			tokenFilter.doFilter(mockRequest, mockResponse, mockChain);
		
		
			TokenFilterManager spyFilter = Mockito.spy(tokenFilter);
			
			AppSettings settings = new AppSettings();
			settings.setPlayTokenControlEnabled(true);
			Mockito.doReturn(settings).when(spyFilter).getAppSettings();
		
			spyFilter.doFilter(mockRequest, mockResponse, mockChain);
		
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

		MockTokenService tokenService = mock(MockTokenService.class);
		AppSettings settings = new AppSettings();
		settings.setPlayTokenControlEnabled(true);

		when(context.getBean(ITokenService.BeanName.TOKEN_SERVICE.toString())).thenReturn(tokenService);
		when(context.getBean(AppSettings.BEAN_NAME)).thenReturn(settings);

		when(servletContext.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE))
		.thenReturn(context);

		when(filterconfig.getServletContext()).thenReturn(servletContext);

		try {
			tokenFilter.init(filterconfig);

			HttpServletRequest mockRequest = mock(HttpServletRequest.class);
			HttpServletResponse mockResponse = mock(HttpServletResponse.class);
			FilterChain mockChain = mock(FilterChain.class);

			String streamId = RandomStringUtils.randomAlphanumeric(8);
			String tokenId = RandomStringUtils.randomAlphanumeric(8);
			HttpSession session = mock(HttpSession.class);
			String sessionId = RandomStringUtils.randomAlphanumeric(16);
			String clientIP = "10.0.0.1";
			when(session.getId()).thenReturn(sessionId);
			when(mockRequest.getSession()).thenReturn(session);
			when(mockRequest.getMethod()).thenReturn("GET");
			when(mockRequest.getRemoteAddr()).thenReturn(clientIP);

			when(mockRequest.getParameter("token")).thenReturn(tokenId);

			when(mockRequest.getRequestURI()).thenReturn("/LiveApp/streams/"+streamId+".m3u8");

			when(mockResponse.getStatus()).thenReturn(HttpServletResponse.SC_OK);

			logger.info("session id {}, stream id {}", sessionId, streamId);
			tokenFilter.doFilter(mockRequest, mockResponse, mockChain);

			verify(tokenService, times(1)).checkToken(tokenId, streamId, sessionId, Token.PLAY_TOKEN);


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
	public void testDoFilterSubscriber() {
		
		FilterConfig filterconfig = mock(FilterConfig.class);
		ServletContext servletContext = mock(ServletContext.class);
		ConfigurableWebApplicationContext context = mock(ConfigurableWebApplicationContext.class);
		when(context.isRunning()).thenReturn(true);
		
		ITokenService tokenService = mock(ITokenService.class);
		AppSettings settings = new AppSettings();
		settings.resetDefaults();
		settings.setTimeTokenSubscriberOnly(true);

		
		when(context.getBean("token.service")).thenReturn(tokenService);
		when(context.getBean(AppSettings.BEAN_NAME)).thenReturn(settings);
		
		when(servletContext.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE))
				.thenReturn(context);
		
		when(filterconfig.getServletContext()).thenReturn(servletContext);
		
		try {
			tokenFilter.init(filterconfig);
			
			HttpServletRequest mockRequest = mock(HttpServletRequest.class);
			HttpServletResponse mockResponse = mock(HttpServletResponse.class);
			FilterChain mockChain = mock(FilterChain.class);
			
			String streamId = RandomStringUtils.randomAlphanumeric(8);
			String subscriberId = "subscriberId1";
			String subscriberCode = "546546";
			
			HttpSession session = mock(HttpSession.class);
			String sessionId = RandomStringUtils.randomAlphanumeric(16);
			String clientIP = "10.0.0.1";
			when(session.getId()).thenReturn(sessionId);
			when(mockRequest.getSession()).thenReturn(session);
			when(mockRequest.getMethod()).thenReturn("GET");
			when(mockRequest.getRemoteAddr()).thenReturn(clientIP);
			
			when(mockRequest.getParameter("subscriberId")).thenReturn(subscriberId);
			when(mockRequest.getParameter("subscriberCode")).thenReturn(subscriberCode);
			
			when(mockRequest.getRequestURI()).thenReturn("/LiveApp/streams/"+streamId+".m3u8");
			
			when(mockResponse.getStatus()).thenReturn(HttpServletResponse.SC_OK);

			logger.info("session id {}, stream id {}", sessionId, streamId);
			tokenFilter.doFilter(mockRequest, mockResponse, mockChain);
			
			// checkTimeBasedSubscriber is called once
			verify(tokenService, times(1)).checkTimeBasedSubscriber(subscriberId, streamId, sessionId, subscriberCode, false);
			
			
			
		} catch (ServletException|IOException e) {
			e.printStackTrace();
			fail(ExceptionUtils.getStackTrace(e));
		} 
		catch (Exception e) {
			e.printStackTrace();
			fail(ExceptionUtils.getStackTrace(e));
		}
	}	

	@Test
	public void testGetStreamId() {
		String streamId = "streamId";
		assertEquals(streamId, TokenFilterManager.getStreamId("/liveapp/streams/"+streamId+"_davut_diyen_kedi_adaptive.m3u8"));

		assertEquals(streamId, TokenFilterManager.getStreamId("/liveapp/streams/"+streamId+".m3u8"));

		assertEquals(streamId, TokenFilterManager.getStreamId("/liveapp/streams/"+streamId+".mp4"));

		assertEquals(streamId, TokenFilterManager.getStreamId("/liveapp/streams/"+streamId+ MuxAdaptor.ADAPTIVE_SUFFIX + ".m3u8"));

		assertEquals(streamId, TokenFilterManager.getStreamId("/liveapp/streams/"+streamId+"_240p.m3u8"));

		assertNull(TokenFilterManager.getStreamId("/liveapp/streams/"+streamId+".u8"));


		//below test case
		streamId = "AgTWuHxp";
		String requestURI = "/LiveApp/streams/"+ streamId + ".m3u8"; 
		assertEquals(streamId, TokenFilterManager.getStreamId(requestURI));
	}
	
	@Test
	public void testTokenGenerator() {
		TokenGenerator tg = new TokenGenerator();
		
		String t1 = tg.getGenetaredToken();
		String t2 = tg.getGenetaredToken();

		assertEquals(t1,  t2);
	}


}
