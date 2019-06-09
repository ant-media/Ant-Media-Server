package io.antmedia.filter;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.WebApplicationContext;

import io.antmedia.AppSettings;
import io.antmedia.datastore.db.types.Token;
import io.antmedia.muxer.MuxAdaptor;
import io.antmedia.security.ITokenService;

public class TokenFilterManager implements javax.servlet.Filter   {

	protected static Logger logger = LoggerFactory.getLogger(TokenFilterManager.class);
	private FilterConfig filterConfig;
	private AppSettings settings;
	private ITokenService tokenService;


	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		this.filterConfig = filterConfig;

	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {

		boolean result = false;

		HttpServletRequest httpRequest =(HttpServletRequest)request;
		HttpServletResponse httpResponse = (HttpServletResponse)response;

		String method = httpRequest.getMethod();
		String tokenId = ((HttpServletRequest) request).getParameter("token");
		String sessionId = httpRequest.getSession().getId();
		String streamId = getStreamId(httpRequest.getRequestURI());
		String clientIP = httpRequest.getRemoteAddr();


		logger.debug("Client IP: {}, request url:  {}, token:  {}, sessionId: {},streamId:  {} ",clientIP 
				,httpRequest.getRequestURI(), tokenId, sessionId, streamId);


		if ("GET".equals(method)) {
			if(getAppSettings().isTokenControlEnabled()) {

				result = getTokenService().checkToken(tokenId, streamId, sessionId, Token.PLAY_TOKEN);
				if(!result) {
					httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN,"Invalid Token");
					logger.warn("token {} is not valid", tokenId);
					return; 
				}
				chain.doFilter(request, response);
			}

			else if (getAppSettings().isHashControlPlayEnabled()) {
				result = getTokenService().checkHash(tokenId, streamId, sessionId, Token.PLAY_TOKEN);

				if(!result) {
					httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN,"Invalid Hash");
					logger.warn("hash {} is not valid", tokenId);
					return; 
				}

				chain.doFilter(request, response);
			}else {
				
				chain.doFilter(request, response);
			}
		}
		else {
			chain.doFilter(httpRequest, response);
		}

	}

	public ITokenService getTokenService() {
		if (tokenService == null) {
			ApplicationContext context = (ApplicationContext) filterConfig.getServletContext().getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
			tokenService = (ITokenService)context.getBean(ITokenService.BeanName.TOKEN_SERVICE.toString());

		}
		return tokenService;
	}


	public void setTokenService(ITokenService tokenService) {
		this.tokenService = tokenService;
	}

	public AppSettings getAppSettings() {
		if (settings == null) {
			ApplicationContext context = (ApplicationContext) filterConfig.getServletContext().getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
			settings = (AppSettings)context.getBean(AppSettings.BEAN_NAME);

		}
		return settings;
	}

	public static String getStreamId(String requestURI) {
		int endIndex;
		int startIndex = requestURI.lastIndexOf('/');

		if(requestURI.contains("_")) {
			//if multiple files with same id requested such as : 541211332342978513714151_480p_1.mp4 
			return requestURI.split("_")[0].substring(startIndex+1);
		}

		//if mp4 file requested
		endIndex = requestURI.lastIndexOf(".mp4");
		if (endIndex != -1) {
			return requestURI.substring(startIndex+1, endIndex);
		}

		//if request is adaptive file ( ending with _adaptive.m3u8)
		endIndex = requestURI.lastIndexOf(MuxAdaptor.ADAPTIVE_SUFFIX + ".m3u8");
		if (endIndex != -1) {
			return requestURI.substring(startIndex+1, endIndex);
		}

		//if specific bitrate is requested
		String regex = "_[0-9]+p\\.m3u8$";  // matches ending with _[resolution]p.m3u8
		if (requestURI.matches(regex)) {
			endIndex = requestURI.lastIndexOf('_'); //because file format is [NAME]_[RESOLUTION]p.m3u8
			return requestURI.substring(startIndex+1, endIndex);
		}

		//if just the m3u8 file
		endIndex = requestURI.lastIndexOf(".m3u8");
		if (endIndex != -1) {
			return requestURI.substring(startIndex+1, endIndex);
		}


		return null;
	}

	@Override
	public void destroy() {

		//no need to deploy
	}










}
