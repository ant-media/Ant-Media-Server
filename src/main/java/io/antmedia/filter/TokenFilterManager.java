package io.antmedia.filter;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.ConfigurableWebApplicationContext;

import io.antmedia.AppSettings;
import io.antmedia.datastore.db.types.Token;
import io.antmedia.muxer.MuxAdaptor;
import io.antmedia.security.ITokenService;

public class TokenFilterManager extends AbstractFilter   {

	private static final String REPLACE_CHARS_REGEX = "[\n|\r|\t]";
	protected static Logger logger = LoggerFactory.getLogger(TokenFilterManager.class);
	private ITokenService tokenService;


	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		
		HttpServletRequest httpRequest =(HttpServletRequest)request;
		HttpServletResponse httpResponse = (HttpServletResponse)response;

		String method = httpRequest.getMethod();
		String tokenId = ((HttpServletRequest) request).getParameter("token");
		if (tokenId != null) {
			tokenId = tokenId.replaceAll(REPLACE_CHARS_REGEX, "_");
		}
		 
		String sessionId = httpRequest.getSession().getId();
		String streamId = getStreamId(httpRequest.getRequestURI());
		
		String clientIP = httpRequest.getRemoteAddr().replaceAll(REPLACE_CHARS_REGEX, "_");

		
		AppSettings appSettings = getAppSettings();
		TokenGenerator tokenGenerator = getTokenGenerator();

		if (appSettings == null) {
			httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN,"Server is getting initialized.");
			logger.warn("AppSettings not initialized. Server is getting started for stream id:{} from request: {}", streamId, clientIP);
			return;
		}


		logger.debug("Client IP: {}, request url:  {}, token:  {}, sessionId: {},streamId:  {} ",clientIP 
				,httpRequest.getRequestURI(), tokenId, sessionId, streamId);


		/*
		 * In cluster mode edges make HLS request to Origin. Token isn't passed with this requests.
		 * So if token enabled, origin returns 403. So we generate an cluster secret, store it in ClusterToken attribute
		 * then check it here to bypass token control.
		 */
		String clusterToken = (String) request.getAttribute("ClusterToken");
		if ("GET".equals(method) 
				&& (tokenGenerator == null || clusterToken == null || !clusterToken.equals(tokenGenerator.getGenetaredToken()))) 
		{
			
			if(appSettings.isPlayTokenControlEnabled()) 
			{
				
				ITokenService tokenServiceTmp = getTokenService();
				if (tokenServiceTmp != null) 
				{
					if (!tokenServiceTmp.checkToken(tokenId, streamId, sessionId, Token.PLAY_TOKEN)) {
						httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid Token");
						logger.warn("token {} is not valid", tokenId);
						return; 
					}
				}
				else {
					httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "Not initialized");
					logger.warn("Token service is not initialized. Server is getting started for stream id:{} from request: {}", streamId, clientIP);
					return;
				}
			}

			else if (appSettings.isHashControlPlayEnabled()) 
			{
				ITokenService tokenServiceTmp = getTokenService();
				if (tokenServiceTmp != null) 
				{
					if (!tokenServiceTmp.checkHash(tokenId, streamId, sessionId, Token.PLAY_TOKEN)) {
						httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN,"Invalid Hash");
						logger.warn("hash {} is not valid", tokenId);
						return; 
					}
				}
				else {
					httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "Not initialized");
					logger.warn("Token service is not initialized. Server is getting started for stream id:{} from request: {}", streamId, clientIP);
					return;
				}
			}
			
		}
	
		chain.doFilter(request, response);

	}

	private TokenGenerator getTokenGenerator() {
		TokenGenerator tokenGenerator = null;
		ConfigurableWebApplicationContext context = getAppContext();
		if (context != null && context.containsBean(TokenGenerator.BEAN_NAME)) {
			tokenGenerator = (TokenGenerator)context.getBean(TokenGenerator.BEAN_NAME);
		}
		return tokenGenerator;
	}

	public ITokenService getTokenService() {
		if (tokenService == null) {
			ApplicationContext context = getAppContext();
			if (context != null) {
				tokenService = (ITokenService)context.getBean(ITokenService.BeanName.TOKEN_SERVICE.toString());
			}
		}
		return tokenService;
	}


	public void setTokenService(ITokenService tokenService) {
		this.tokenService = tokenService;
	}

	public static String getStreamId(String requestURI) {
		
		requestURI = requestURI.replaceAll(REPLACE_CHARS_REGEX, "_");
		
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

}
