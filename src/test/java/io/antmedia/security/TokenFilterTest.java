package io.antmedia.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

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
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.WebApplicationContext;

import com.amazonaws.util.Base32;

import io.antmedia.AppSettings;
import io.antmedia.datastore.db.types.Token;
import io.antmedia.filter.TokenFilterManager;
import io.antmedia.filter.TokenGenerator;
import io.antmedia.muxer.MuxAdaptor;


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
	public void testDoFilter() {
		
		FilterConfig filterconfig = mock(FilterConfig.class);
		ServletContext servletContext = mock(ServletContext.class);
		ConfigurableWebApplicationContext context = mock(ConfigurableWebApplicationContext.class);
		when(context.isRunning()).thenReturn(true);
		
		ITokenService tokenService = mock(ITokenService.class);
		AppSettings settings = new AppSettings();
		settings.resetDefaults();
		settings.setPlayTokenControlEnabled(true);

		
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
			e.printStackTrace();
			fail(ExceptionUtils.getStackTrace(e));
		} 
		catch (Exception e) {
			e.printStackTrace();
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
		
		FilterConfig filterconfig = mock(FilterConfig.class);
		ServletContext servletContext = mock(ServletContext.class);
		ConfigurableWebApplicationContext context = mock(ConfigurableWebApplicationContext.class);
		when(context.isRunning()).thenReturn(true);
		
		ITokenService tokenService = mock(ITokenService.class);
		AppSettings settings = mock(AppSettings.class);
		settings.resetDefaults();
		settings.setPlayTokenControlEnabled(true);

		TokenGenerator tokenGenerator = new TokenGenerator();
		
		when(context.getBean("token.service")).thenReturn(tokenService);
		when(context.getBean(AppSettings.BEAN_NAME)).thenReturn(settings);
		when(context.containsBean(TokenGenerator.BEAN_NAME)).thenReturn(true);
		when(context.getBean(TokenGenerator.BEAN_NAME)).thenReturn(tokenGenerator);

		
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
			when(mockRequest.getAttribute("ClusterToken")).thenReturn(tokenGenerator.getGenetaredToken());
			
			
			when(mockRequest.getRequestURI()).thenReturn("/LiveApp/streams/"+streamId+".m3u8");
			
			when(mockResponse.getStatus()).thenReturn(HttpServletResponse.SC_OK);

			logger.info("session id {}, stream id {}", sessionId, streamId);
			tokenFilter.doFilter(mockRequest, mockResponse, mockChain);
			
			
			verify(settings, never()).isPlayTokenControlEnabled();
			
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
	public void testTOTPGenerator() 
	{
		byte[] secretBytes = Base32.decode("mysecret");
		String code = TOTPGenerator.generateTOTP(secretBytes, 60, 6, "HmacSHA1");
		int intCode = Integer.parseInt(code);	
		logger.info("generated code: {} int value:{}", code, intCode);
		assertEquals(6, code.length());
		
		if (code.charAt(0) == '0') {
			//first character can be zero.
			assertTrue(intCode > 10000);
			//if both first two characters are zero, meet the ice bear in the desert :)
		}
		else {
			assertTrue(intCode > 100000);
		}
		
		assertTrue(intCode < 1000000);
	}
	
}
