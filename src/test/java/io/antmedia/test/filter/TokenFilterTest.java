package io.antmedia.test.filter;

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
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.WebApplicationContext;

import io.antmedia.AppSettings;
import io.antmedia.datastore.db.types.Token;
import io.antmedia.filter.TokenFilter;
import io.antmedia.muxer.MuxAdaptor;
import io.antmedia.security.TokenService;

public class TokenFilterTest {
	protected static Logger logger = LoggerFactory.getLogger(TokenFilterTest.class);
	
	private TokenFilter tokenFilter;
	
	@Before
	public void before() {
		tokenFilter = new TokenFilter();
	}
	
	@After
	public void after() {
		tokenFilter = null;
	}
	

	@Test
	public void testDoFilter() {
		
		FilterConfig filterconfig = mock(FilterConfig.class);
		ServletContext servletContext = mock(ServletContext.class);
		ApplicationContext context = mock(ApplicationContext.class);
		
		
		TokenService tokenService = mock(TokenService.class);
		AppSettings settings = new AppSettings();
		settings.setTokenControlEnabled(true);

		
		when(context.getBean(TokenService.BEAN_NAME)).thenReturn(tokenService);
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
	public void testGetStreamId() {
		String streamId = "streamId";
		assertEquals(streamId, TokenFilter.getStreamId("/liveapp/streams/"+streamId+"_davut_diyen_kedi_adaptive.m3u8"));
		
		assertEquals(streamId, TokenFilter.getStreamId("/liveapp/streams/"+streamId+".m3u8"));
		
		assertEquals(streamId, TokenFilter.getStreamId("/liveapp/streams/"+streamId+".mp4"));
		
		assertEquals(streamId, TokenFilter.getStreamId("/liveapp/streams/"+streamId+ MuxAdaptor.ADAPTIVE_SUFFIX + ".m3u8"));
		
		assertEquals(streamId, TokenFilter.getStreamId("/liveapp/streams/"+streamId+"_240p.m3u8"));
		
		assertNull(TokenFilter.getStreamId("/liveapp/streams/"+streamId+".u8"));
	}
	
	
}
