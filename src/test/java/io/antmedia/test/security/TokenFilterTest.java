package io.antmedia.test.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.WebApplicationContext;

import com.amazonaws.util.Base32;

import io.antmedia.AppSettings;
import io.antmedia.datastore.db.types.Subscriber;
import io.antmedia.datastore.db.types.Token;
import io.antmedia.filter.TokenFilterManager;
import io.antmedia.security.ITokenService;
import io.antmedia.security.TOTPGenerator;


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
	public void testDoFilterWithPOST() {

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
			when(mockRequest.getMethod()).thenReturn("POST");
			when(mockRequest.getRemoteAddr()).thenReturn(clientIP);

			when(mockRequest.getParameter("token")).thenReturn(tokenId);

			when(mockRequest.getRequestURI()).thenReturn("/LiveApp/streams/"+streamId+".m3u8");

			logger.info("session id {}, stream id {}", sessionId, streamId);
			tokenFilter.doFilter(mockRequest, mockResponse, mockChain);


			verify(tokenService, never()).checkToken(tokenId, streamId, sessionId, Token.PLAY_TOKEN);
			verify(mockResponse, times(1)).sendError(HttpServletResponse.SC_FORBIDDEN,"Invalid Request Type");	

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
	public void testDoFilterJwtToken() {

		FilterConfig filterconfig = mock(FilterConfig.class);
		ServletContext servletContext = mock(ServletContext.class);
		ConfigurableWebApplicationContext context = mock(ConfigurableWebApplicationContext.class);
		when(context.isRunning()).thenReturn(true);

		ITokenService tokenService = mock(ITokenService.class);
		AppSettings settings = new AppSettings();
		settings.resetDefaults();
		settings.setPlayJwtControlEnabled(true);


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

			verify(tokenService, times(1)).checkJwtToken(tokenId, streamId, sessionId, Token.PLAY_TOKEN);

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
			verify(tokenService, times(1)).checkTimeBasedSubscriber(subscriberId, streamId, sessionId, subscriberCode, Subscriber.PLAY_TYPE);


		} catch (ServletException|IOException e) {
			e.printStackTrace();
			fail(ExceptionUtils.getStackTrace(e));
		} 
		catch (Exception e) {
			e.printStackTrace();
			fail(ExceptionUtils.getStackTrace(e));
		}
	}	

	private AppSettings mockAppSettings(boolean timeTokenSubscriberOnly, boolean playJwtControlEnabled, boolean enableTimeTokenForPlay, boolean playTokenControlEnabled, boolean hashControlPlayEnabled) {
		AppSettings appSettings = mock(AppSettings.class);
		when(appSettings.isTimeTokenSubscriberOnly()).thenReturn(timeTokenSubscriberOnly);
		when(appSettings.isPlayJwtControlEnabled()).thenReturn(playJwtControlEnabled);
		when(appSettings.isEnableTimeTokenForPlay()).thenReturn(enableTimeTokenForPlay);
		when(appSettings.isPlayTokenControlEnabled()).thenReturn(playTokenControlEnabled);
		when(appSettings.isHashControlPlayEnabled()).thenReturn(hashControlPlayEnabled);
		return appSettings;
	}

	@Test
	public void testIsAnySecurityEnabled() {

		AppSettings appSettings = mockAppSettings(false, false, false, false, false);

		boolean result = TokenFilterManager.isAnySecurityEnabled(appSettings);
		assertFalse(result);
		
		appSettings = mockAppSettings(true, false, false, false, false);
        result = TokenFilterManager.isAnySecurityEnabled(appSettings);
        assertTrue(result);
        
        appSettings = mockAppSettings(false, true, false, false, false);
        result = TokenFilterManager.isAnySecurityEnabled(appSettings);
        assertTrue(result);
        
        appSettings = mockAppSettings(false, false, true, false, false);
        result = TokenFilterManager.isAnySecurityEnabled(appSettings);
        assertTrue(result);
        
        appSettings = mockAppSettings(false, false, false, true, false);
        result = TokenFilterManager.isAnySecurityEnabled(appSettings);
        assertTrue(result);
        
        appSettings = mockAppSettings(false, false, false, false, true);
        result = TokenFilterManager.isAnySecurityEnabled(appSettings);
        assertTrue(result);
        
        appSettings = mockAppSettings(true, true, true, true, true);
        result = TokenFilterManager.isAnySecurityEnabled(appSettings);
        assertTrue(result);

	}


	@Test
	public void testPassThroughIfNoStreamIdAndSecurity() {
		FilterConfig filterconfig = mock(FilterConfig.class);
		ServletContext servletContext = mock(ServletContext.class);
		ConfigurableWebApplicationContext context = mock(ConfigurableWebApplicationContext.class);
		when(context.isRunning()).thenReturn(true);

		ITokenService tokenService = mock(ITokenService.class);
		AppSettings settings = new AppSettings();
		settings.resetDefaults();



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


			HttpSession session = mock(HttpSession.class);
			String sessionId = RandomStringUtils.randomAlphanumeric(16);
			String clientIP = "10.0.0.1";
			when(session.getId()).thenReturn(sessionId);
			when(mockRequest.getSession()).thenReturn(session);
			when(mockRequest.getMethod()).thenReturn("GET");
			when(mockRequest.getRemoteAddr()).thenReturn(clientIP);

			String requestURI = "/LiveApp/streams/"+ streamId +"_anything.ts";
			assertNull(TokenFilterManager.getStreamId(requestURI));

			when(mockRequest.getRequestURI()).thenReturn(requestURI);

			when(mockResponse.getStatus()).thenReturn(HttpServletResponse.SC_OK);

			logger.info("session id {}, stream id {}", sessionId, streamId);
			tokenFilter.doFilter(mockRequest, mockResponse, mockChain);

			verify(mockResponse, never()).sendError(HttpServletResponse.SC_FORBIDDEN, "Cluster communication token is not valid for streamId:" + streamId);


			settings.setPlayJwtControlEnabled(true);
			tokenFilter.doFilter(mockRequest, mockResponse, mockChain);
			verify(mockResponse, times(1)).sendError(HttpServletResponse.SC_FORBIDDEN, "Cannot specified the stream id from the url");



			settings.setPlayJwtControlEnabled(true);
			requestURI = "/LiveApp/streams/"+ streamId +".m3u8";
			assertNotNull(TokenFilterManager.getStreamId(requestURI));
			when(mockRequest.getRequestURI()).thenReturn(requestURI);

			tokenFilter.doFilter(mockRequest, mockResponse, mockChain);

			verify(tokenService, times(1)).checkJwtToken(any(), anyString(), anyString(), anyString());


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
	public void testClusterCommunication() {

		FilterConfig filterconfig = mock(FilterConfig.class);
		ServletContext servletContext = mock(ServletContext.class);
		ConfigurableWebApplicationContext context = mock(ConfigurableWebApplicationContext.class);
		when(context.isRunning()).thenReturn(true);

		ITokenService tokenService = mock(ITokenService.class);
		AppSettings settings = mock(AppSettings.class);
		settings.resetDefaults();
		settings.setPlayTokenControlEnabled(true);
		when(settings.isPlayTokenControlEnabled()).thenReturn(true);
		when(settings.getClusterCommunicationKey()).thenReturn(RandomStringUtils.randomAlphabetic(10));
		when(settings.getHlsSegmentFileSuffixFormat()).thenReturn("%9d");



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
			verify(mockResponse).sendError(HttpServletResponse.SC_FORBIDDEN, TokenFilterManager.NOT_INITIALIZED);
			verify(settings, times(0)).isPlayTokenControlEnabled();


			when(context.getBean("token.service")).thenReturn(tokenService);

			tokenFilter.doFilter(mockRequest, mockResponse, mockChain);

			//isPlayTokenControlEnabled should be called because there is no header TOKEN_HEADER_FOR_NODE_COMMUNICATION for internal communication
			verify(settings, times(2)).isPlayTokenControlEnabled();
			verify(mockResponse).sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid Token for streamId:" + streamId);
			verify(mockChain, never()).doFilter(mockRequest, mockResponse);


			when(mockRequest.getHeader(TokenFilterManager.TOKEN_HEADER_FOR_NODE_COMMUNICATION)).thenReturn(RandomStringUtils.randomAlphanumeric(32));
			when(tokenService.isJwtTokenValid(anyString(), anyString(), anyString(), anyString())).thenReturn(true);

			tokenFilter.doFilter(mockRequest, mockResponse, mockChain);

			verify(tokenService, times(1)).isJwtTokenValid(anyString(), anyString(), anyString(), anyString());

			//play token should not be called again because there is header(TOKEN_HEADER_FOR_NODE_COMMUNICATION) and token service returns true so it just bypass
			verify(settings, times(2)).isPlayTokenControlEnabled();
			verify(mockChain, times(1)).doFilter(mockRequest, mockResponse);



			when(tokenService.isJwtTokenValid(anyString(), anyString(), anyString(), anyString())).thenReturn(false);
			tokenFilter.doFilter(mockRequest, mockResponse, mockChain);

			verify(tokenService, times(2)).isJwtTokenValid(anyString(), anyString(), anyString(), anyString());
			//it should not be called again because there is TOKEN_HEADER_FOR_NODE_COMMUNICATION header and it is not valid
			verify(settings, times(2)).isPlayTokenControlEnabled();
			//it should not be called again because there is TOKEN_HEADER_FOR_NODE_COMMUNICATION header and it is not valid
			verify(mockChain, times(1)).doFilter(mockRequest, mockResponse);
			verify(mockResponse).sendError(HttpServletResponse.SC_FORBIDDEN, "Cluster communication token is not valid for streamId:" + streamId);


			//make the streamId null

			when(mockRequest.getRequestURI()).thenReturn("/LiveApp/streams/"+streamId+"_anything.ts");
			tokenFilter.doFilter(mockRequest, mockResponse, mockChain);

			//isJwtTokenValid should not be called because streamId is not valid - it's same number of call with the previous one
			verify(tokenService, times(2)).isJwtTokenValid(anyString(), anyString(), anyString(), anyString());


			// Test DRM case - streamId with "drm/" prefix should be stripped before JWT validation
			when(mockRequest.getRequestURI()).thenReturn("/LiveApp/streams/drm/" + streamId + "/master.mpd");
			when(tokenService.isJwtTokenValid(anyString(), anyString(), eq(streamId), anyString())).thenReturn(true);

			tokenFilter.doFilter(mockRequest, mockResponse, mockChain);
			verify(tokenService, atLeast(1)).isJwtTokenValid(anyString(), anyString(), eq(streamId), anyString());
			verify(mockChain, times(2)).doFilter(mockRequest, mockResponse);


			// Test the other DRM case
			when(mockRequest.getRequestURI()).thenReturn("/LiveApp/streams/drm/" + streamId + "/master.m3u8");
			when(tokenService.isJwtTokenValid(anyString(), anyString(), eq(streamId), anyString())).thenReturn(true);

			tokenFilter.doFilter(mockRequest, mockResponse, mockChain);
			verify(tokenService, atLeast(1)).isJwtTokenValid(anyString(), anyString(), eq(streamId), anyString());
			verify(mockChain, atLeast(1)).doFilter(mockRequest, mockResponse);

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
			assertTrue("First 4 characters are zero, this is why this test failed. It may happen with low possibility."
					+ "With this luck, you may meet the ice bear in the desert :)"
					+ "Have a break and relax, then try again ;)", intCode > 100);
			//first 4 characters are zero, meet the ice bear in the desert :)
		}
		else {
			assertTrue(intCode > 100000);
		}

		assertTrue(intCode < 1000000);
	}

	@Test
	public void testGeneratedSecret() {
		String subscriberId = "sub1";
		String streamId = "stream1";
		String type = "publish";
		String secret = "secret";
		String generatedSecretCode = TOTPGenerator.getSecretCodeForNotRecordedSubscriberId(subscriberId, streamId, type, secret);

		assertEquals(Base32.encodeAsString((secret+subscriberId+streamId+type).getBytes()), generatedSecretCode);

		secret = "secret123";
		generatedSecretCode = TOTPGenerator.getSecretCodeForNotRecordedSubscriberId(subscriberId, streamId, type, secret);
		assertEquals(Base32.encodeAsString((secret+subscriberId+streamId+type+"XXXXX").getBytes()), generatedSecretCode);

		generatedSecretCode = TOTPGenerator.getSecretCodeForNotRecordedSubscriberId(subscriberId, streamId, type, null);
		assertNull(generatedSecretCode);


	}

}
