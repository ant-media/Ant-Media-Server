package io.antmedia.test.token;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
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
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.WebApplicationContext;

import io.antmedia.AppSettings;
import io.antmedia.datastore.db.types.Subscriber;
import io.antmedia.datastore.db.types.Token;
import io.antmedia.filter.TokenFilterManager;
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
	public void testDoFilterOneTimeToken() {

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
	public void testDoFilterHashBasedToken() {

		FilterConfig filterconfig = mock(FilterConfig.class);
		ServletContext servletContext = mock(ServletContext.class);
		ConfigurableWebApplicationContext context = mock(ConfigurableWebApplicationContext.class);
		when(context.isRunning()).thenReturn(true);

		MockTokenService tokenService = mock(MockTokenService.class);
		AppSettings settings = new AppSettings();
		settings.setHashControlPlayEnabled(true);

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

			verify(tokenService, times(1)).checkHash(tokenId, streamId, sessionId, Token.PLAY_TOKEN);


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
	public void testDoFilterJwtToken() {

		FilterConfig filterconfig = mock(FilterConfig.class);
		ServletContext servletContext = mock(ServletContext.class);
		ConfigurableWebApplicationContext context = mock(ConfigurableWebApplicationContext.class);
		when(context.isRunning()).thenReturn(true);

		MockTokenService tokenService = mock(MockTokenService.class);
		AppSettings settings = new AppSettings();
		settings.setPlayJwtControlEnabled(true);

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

			verify(tokenService, times(1)).checkJwtToken(tokenId, streamId, sessionId, Token.PLAY_TOKEN);


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

	@Test
	public void testGetStreamId() {
		String streamId = "stream_Id";
		
		assertEquals("test_stream_Id_davut_diyen_kedi", TokenFilterManager.getStreamId("/liveapp/streams/"+"test_"+streamId+"_davut_diyen_kedi_adaptive.m3u8"));
		assertEquals("test_stream_Id_davut_diyen_kedi", TokenFilterManager.getStreamId("/liveapp/streams/subfolder/"+"test_"+streamId+"_davut_diyen_kedi_adaptive.m3u8"));

		
		assertEquals(streamId, TokenFilterManager.getStreamId("/liveapp/streams/"+streamId+ MuxAdaptor.ADAPTIVE_SUFFIX +".m3u8"));
		assertEquals(streamId, TokenFilterManager.getStreamId("/liveapp/streams/subfolder/"+streamId+ MuxAdaptor.ADAPTIVE_SUFFIX +".m3u8"));
		
		assertEquals("stream_Id_underline_test", TokenFilterManager.getStreamId("/liveapp/streams/"+streamId+ "_underline_test" +".m3u8"));
		assertEquals("stream_Id_underline_test", TokenFilterManager.getStreamId("/liveapp/streams/subfolder/"+streamId+ "_underline_test" +".m3u8"));
		
		assertEquals("stream_Id_underline_test", TokenFilterManager.getStreamId("/liveapp/streams/"+streamId+ "_underline_test_240p" +".m3u8"));
		assertEquals("stream_Id_underline_test", TokenFilterManager.getStreamId("/liveapp/streams/subfolder/"+streamId+ "_underline_test_240p" +".m3u8"));
		
		assertEquals("stream_Id_underline_test", TokenFilterManager.getStreamId("/liveapp/streams/"+streamId+ "_underline_test_500kbps" +".m3u8"));
		assertEquals("stream_Id_underline_test", TokenFilterManager.getStreamId("/liveapp/streams/subfolder/"+streamId+ "_underline_test_500kbps" +".m3u8"));

		assertEquals("stream_Id_underline_test", TokenFilterManager.getStreamId("/liveapp/streams/"+streamId+ "_underline_test_480p300kbps" +".m3u8"));
		assertEquals("stream_Id_underline_test", TokenFilterManager.getStreamId("/liveapp/streams/subfolder/"+streamId+ "_underline_test_480p300kbps" +".m3u8"));
		
		
		// Tests for CMAF
		assertEquals(streamId, TokenFilterManager.getStreamId("/liveapp/chunked/"+streamId+"/media_1.m3u8"));
		
		assertEquals(streamId, TokenFilterManager.getStreamId("/liveapp/chunked/"+streamId+"/master.m3u8"));
		
		assertEquals(streamId, TokenFilterManager.getStreamId("/liveapp/chunked/"+streamId+"/media_0.m3u8"));		
		
		assertEquals(streamId, TokenFilterManager.getStreamId("/liveapp/streams/"+streamId+".m3u8"));
		assertEquals(streamId, TokenFilterManager.getStreamId("/liveapp/streams/subfolder/"+streamId+".m3u8"));
		
		assertEquals(streamId, TokenFilterManager.getStreamId("/liveapp/streams/"+streamId+ "_240p.m3u8"));
		assertEquals(streamId, TokenFilterManager.getStreamId("/liveapp/streams/subfolder/"+streamId+ "_240p.m3u8"));
		
		assertEquals(streamId, TokenFilterManager.getStreamId("/liveapp/streams/"+streamId+ "_500kbps.m3u8"));
		assertEquals(streamId, TokenFilterManager.getStreamId("/liveapp/streams/subfolder/"+streamId+ "_500kbps.m3u8"));
		
		assertEquals(streamId, TokenFilterManager.getStreamId("/liveapp/streams/"+streamId+"_480p300kbps_1"+".mp4")); 
		assertEquals(streamId, TokenFilterManager.getStreamId("/liveapp/streams/subfolder/"+streamId+"_480p300kbps_1"+".mp4")); 
		
		assertEquals(streamId, TokenFilterManager.getStreamId("/liveapp/streams/"+streamId+"_1"+".mp4")); 
		assertEquals(streamId, TokenFilterManager.getStreamId("/liveapp/streams/subfolder/"+streamId+"_1"+".mp4")); 

		assertEquals(streamId, TokenFilterManager.getStreamId("/liveapp/streams/"+streamId+".mp4")); 
		assertEquals(streamId, TokenFilterManager.getStreamId("/liveapp/streams/subfolder/"+streamId+".mp4")); 
		
		
		assertEquals(streamId+ "_underline_test", TokenFilterManager.getStreamId("/liveapp/streams/"+streamId+ "_underline_test-2021-05-18_11-26-26.842"+".mp4")); 
		assertEquals(streamId+ "_underline_test", TokenFilterManager.getStreamId("/liveapp/streams/subfolder/"+streamId+ "_underline_test-2021-05-18_11-26-26.842"+".mp4")); 
		
		
		assertEquals(streamId+ "_underline_test", TokenFilterManager.getStreamId("/liveapp/streams/"+streamId+ "_underline_test-2021-05-18_11-26-26.842_240p500kbps"+".mp4")); 
		assertEquals(streamId+ "_underline_test", TokenFilterManager.getStreamId("/liveapp/streams/subfolder/"+streamId+ "_underline_test-2021-05-18_11-26-26.842_240p500kbps"+".mp4")); 

		
		assertEquals(streamId, TokenFilterManager.getStreamId("/liveapp/streams/"+streamId+"_240p300kbps.m3u8"));
		assertEquals(streamId, TokenFilterManager.getStreamId("/liveapp/streams/subfolder/"+streamId+"_240p300kbps.m3u8"));
		
		assertEquals(streamId, TokenFilterManager.getStreamId("/liveapp/streams/"+streamId+"0000.ts")); 
		assertEquals(streamId, TokenFilterManager.getStreamId("/liveapp/streams/subfolder/"+streamId+"0000.ts"));
		
		
		assertEquals("monstercat", TokenFilterManager.getStreamId("/liveapp/streams/monstercat060218000.ts")); 
		assertEquals("monstercat", TokenFilterManager.getStreamId("/liveapp/streams/subfolder/monstercat060218000.ts")); 
		
		assertEquals(streamId, TokenFilterManager.getStreamId("/liveapp/streams/"+streamId+"_240p300kbps0000.ts")); 
		assertEquals(streamId, TokenFilterManager.getStreamId("/liveapp/streams/subfolder/"+streamId+"_240p300kbps0000.ts")); 

		assertNull(TokenFilterManager.getStreamId("/liveapp/streams/"+streamId+".u8"));
		assertNull(TokenFilterManager.getStreamId("/liveapp/streams/subfolder/"+streamId+".u8"));
		
		// Add "_" in appname and stream Id
		
		assertEquals("test_stream_Id_davut_diyen_kedi", TokenFilterManager.getStreamId("/live_app/streams/"+"test_" + streamId+"_davut_diyen_kedi_adaptive.m3u8"));
		assertEquals("test_stream_Id_davut_diyen_kedi", TokenFilterManager.getStreamId("/live_app/streams/subfolder/"+"test_" + streamId+"_davut_diyen_kedi_adaptive.m3u8"));
		
		assertEquals("test_test_"+streamId, TokenFilterManager.getStreamId("/live_app/streams/"+"test_test_"+streamId+ MuxAdaptor.ADAPTIVE_SUFFIX + ".m3u8"));
		assertEquals("test_test_"+streamId, TokenFilterManager.getStreamId("/live_app/streams/subfolder/"+"test_test_"+streamId+ MuxAdaptor.ADAPTIVE_SUFFIX + ".m3u8"));
		
		assertEquals("test_test_"+streamId, TokenFilterManager.getStreamId("/live_app/streams/"+"test_test_"+streamId+".m3u8"));
		assertEquals("test_test_"+streamId, TokenFilterManager.getStreamId("/live_app/streams/subfolder/"+"test_test_"+streamId+".m3u8"));
		
		assertEquals("test_test_"+streamId, TokenFilterManager.getStreamId("/live_app/streams/"+"test_test_"+streamId+"/"+"test_test_"+streamId+"_1segment00139.m4s"));
		
		
		// Tests for CMAF
		assertEquals("test_test_" + streamId, TokenFilterManager.getStreamId("/live_app/chunked/test_test_" + streamId+"/media_1.m3u8"));
		
		assertEquals("test_test_" + streamId, TokenFilterManager.getStreamId("/live_app/chunked/test_test_"+ streamId+"/master.m3u8"));
		
		assertEquals("test_test_" + streamId, TokenFilterManager.getStreamId("/live_app/chunked/test_test_" + streamId+"/media_0.m3u8"));		

		assertEquals("test_test_"+streamId, TokenFilterManager.getStreamId("/live_app/streams/"+"test_test_"+streamId+".mp4"));
		assertEquals("test_test_"+streamId, TokenFilterManager.getStreamId("/live_app/streams/subfolder/"+"test_test_"+streamId+".mp4"));
		
		assertEquals("test_test_"+streamId, TokenFilterManager.getStreamId("/liveapp/streams/"+"test_test_"+streamId+"_1"+".mp4")); 
		assertEquals("test_test_"+streamId, TokenFilterManager.getStreamId("/liveapp/streams/subfolder/"+"test_test_"+streamId+"_1"+".mp4")); 

		
		assertEquals("test_test_"+streamId, TokenFilterManager.getStreamId("/liveapp/streams/"+"test_test_"+streamId+"_480p400kbps_1"+".mp4")); 
		assertEquals("test_test_"+streamId, TokenFilterManager.getStreamId("/liveapp/streams/subfolder/"+"test_test_"+streamId+"_480p400kbps_1"+".mp4")); 
		
		
		assertEquals("test_test_"+streamId, TokenFilterManager.getStreamId("/liveapp/streams/"+"test_test_"+streamId+"_480p300kbps_1"+".mp4")); 
		assertEquals("test_test_"+streamId, TokenFilterManager.getStreamId("/liveapp/streams/subfolder/"+"test_test_"+streamId+"_480p300kbps_1"+".mp4")); 
		
		assertEquals("test_test_"+streamId , TokenFilterManager.getStreamId("/live_app/streams/"+"test_test_"+streamId+ "-2021-05-18_11-26-26.842"+".mp4")); 
		assertEquals("test_test_"+streamId , TokenFilterManager.getStreamId("/live_app/streams/subfolder/"+"test_test_"+streamId+ "-2021-05-18_11-26-26.842"+".mp4")); 
		
		assertEquals("test_test_"+streamId, TokenFilterManager.getStreamId("/live_app/streams/"+"test_test_"+streamId+ "-2021-05-18_11-26-26.842_240p250kbps"+".mp4")); 
		assertEquals("test_test_"+streamId, TokenFilterManager.getStreamId("/live_app/streams/subfolder/"+"test_test_"+streamId+ "-2021-05-18_11-26-26.842_240p250kbps"+".mp4")); 

		assertEquals("test_test_"+streamId, TokenFilterManager.getStreamId("/live_app/streams/"+"test_test_"+streamId+"_240p500kbps.m3u8"));
		assertEquals("test_test_"+streamId, TokenFilterManager.getStreamId("/live_app/streams/subfolder/"+"test_test_"+streamId+"_240p500kbps.m3u8"));
		
		assertEquals("test_test_"+streamId, TokenFilterManager.getStreamId("/live_app/streams/"+"test_test_"+streamId+"_240p300kbps.m3u8"));
		assertEquals("test_test_"+streamId, TokenFilterManager.getStreamId("/live_app/streams/subfolder/"+"test_test_"+streamId+"_240p300kbps.m3u8"));
				
		assertEquals("test_test_"+streamId, TokenFilterManager.getStreamId("/liveapp/streams/"+"test_test_"+streamId+"_0p500kbps0000.ts")); 
		assertEquals("test_test_"+streamId, TokenFilterManager.getStreamId("/liveapp/streams/subfolder/"+"test_test_"+streamId+"_0p500kbps0000.ts")); 
		
		assertEquals("test_test_"+streamId, TokenFilterManager.getStreamId("/liveapp/streams/"+"test_test_"+streamId+"_240p120kbps0000.ts"));
		assertEquals("test_test_"+streamId, TokenFilterManager.getStreamId("/liveapp/streams/subfolder/"+"test_test_"+streamId+"_240p120kbps0000.ts"));
		
		assertEquals("test_test_"+streamId, TokenFilterManager.getStreamId("/liveapp/streams/"+"test_test_"+streamId+"/"+"test_test_"+streamId+"_0segment00139.m4s"));

		assertEquals("test_test_"+streamId, TokenFilterManager.getStreamId("/liveapp/streams/"+"test_test_"+streamId+"/"+"test_test_"+streamId+"_0segment00139000001.m4s"));

		
		assertNull(TokenFilterManager.getStreamId("/live_app/streams/"+streamId+".u8"));
		assertNull(TokenFilterManager.getStreamId("/live_app/streams/subfolder/"+streamId+".u8"));
		
		
						
		assertNull(TokenFilterManager.getStreamId("/liveapp/streams/"+streamId+".u8"));
		assertNull(TokenFilterManager.getStreamId("/liveapp/streams/subfolder/"+streamId+".u8"));

		assertEquals(streamId, TokenFilterManager.getStreamId("/liveapp/streams/"+streamId+".webm"));
		assertEquals(streamId, TokenFilterManager.getStreamId("/liveapp/streams/subfolder/"+streamId+".webm"));
		
		assertEquals(streamId, TokenFilterManager.getStreamId("/liveapp/streams/"+streamId+"_1.webm"));
		assertEquals(streamId, TokenFilterManager.getStreamId("/liveapp/streams/subfolder/"+streamId+"_1.webm"));
		
		assertEquals(streamId, TokenFilterManager.getStreamId("/liveapp/streams/"+streamId+"-2021-12-26_19-13-12.371.webm"));
		assertEquals(streamId, TokenFilterManager.getStreamId("/liveapp/streams/subfolder/"+streamId+"-2021-12-26_19-13-12.371.webm"));
		
		assertEquals(streamId, TokenFilterManager.getStreamId("/liveapp/streams/"+streamId+"-2021-12-26_19-13-39.524_240p500kbps.webm"));
		assertEquals(streamId, TokenFilterManager.getStreamId("/liveapp/streams/subfolder/"+streamId+"-2021-12-26_19-13-39.524_240p500kbps.webm"));
		
		assertEquals(streamId, TokenFilterManager.getStreamId("/liveapp/streams/"+streamId+"_240p500kbps.mp4"));
		assertEquals(streamId, TokenFilterManager.getStreamId("/liveapp/streams/subfolder/"+streamId+"_240p500kbps.mp4"));
		
		assertEquals(streamId, TokenFilterManager.getStreamId("/liveapp/streams/"+streamId+"_240p500kbps.webm"));
		assertEquals(streamId, TokenFilterManager.getStreamId("/liveapp/streams/subfolder/"+streamId+"_240p500kbps.webm"));
		
		assertEquals(streamId, TokenFilterManager.getStreamId("/liveapp/streams/"+streamId+"_240p500kbps_1.webm"));
		assertEquals(streamId, TokenFilterManager.getStreamId("/liveapp/streams/subfolder/"+streamId+"_240p500kbps_1.webm"));
		
		assertEquals(streamId, TokenFilterManager.getStreamId("/liveapp/streams/"+streamId+"_240p500kbps_2.webm"));
		assertEquals(streamId, TokenFilterManager.getStreamId("/liveapp/streams/subfolder/"+streamId+"_240p500kbps_2.webm"));
		

		assertEquals(streamId+"_tahir_diyen_kedi_adaptive_123", TokenFilterManager.getStreamId("/liveapp/streams/"+streamId+"_tahir_diyen_kedi_adaptive_123_480p600kbps_1.mp4"));
		assertEquals(streamId+"_tahir_diyen_kedi_adaptive_123", TokenFilterManager.getStreamId("/liveapp/streams/subfolder/"+streamId+"_tahir_diyen_kedi_adaptive_123_480p600kbps_1.mp4"));

		assertEquals(streamId+"_tahir_diyen_kedi_adaptive", TokenFilterManager.getStreamId("/liveapp/streams/"+streamId+"_tahir_diyen_kedi_adaptive_12.webm"));
		assertEquals(streamId+"_tahir_diyen_kedi_adaptive", TokenFilterManager.getStreamId("/liveapp/streams/subfolder/"+streamId+"_tahir_diyen_kedi_adaptive_12.webm"));


		//below test case
		streamId = "AgTWuHxp";
		String requestURI = "/LiveApp/streams/"+ streamId + ".m3u8"; 
		assertEquals(streamId, TokenFilterManager.getStreamId(requestURI));
		
		requestURI = "/LiveApp/streams/subfolder/"+ streamId + ".m3u8"; 
		assertEquals(streamId, TokenFilterManager.getStreamId(requestURI));
				
	}
	

}
