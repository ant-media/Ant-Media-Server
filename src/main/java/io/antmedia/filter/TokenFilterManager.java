package io.antmedia.filter;

import java.io.IOException;
import java.util.regex.Pattern;

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
	private static final String NOT_INITIALIZED= "Not initialized";
	protected static Logger logger = LoggerFactory.getLogger(TokenFilterManager.class);
	private ITokenService tokenService;


	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		
		HttpServletRequest httpRequest =(HttpServletRequest)request;
		HttpServletResponse httpResponse = (HttpServletResponse)response;

		String method = httpRequest.getMethod();
		String tokenId = ((HttpServletRequest) request).getParameter("token");
		String subscriberId = ((HttpServletRequest) request).getParameter("subscriberId");
		String subscriberCodeText = ((HttpServletRequest) request).getParameter("subscriberCode");
		
		if (tokenId != null) {
			tokenId = tokenId.replaceAll(REPLACE_CHARS_REGEX, "_");
		}
		if (subscriberId != null) {
			subscriberId = subscriberId.replaceAll(REPLACE_CHARS_REGEX, "_");
		}
		if (subscriberCodeText != null) {
			subscriberCodeText = subscriberCodeText.replaceAll(REPLACE_CHARS_REGEX, "_");
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
			
			if(appSettings.isTimeTokenSubscriberOnly() || appSettings.isEnableTimeTokenForPlay() || appSettings.isEnableTimeTokenForPublish()) {
				ITokenService tokenServiceTmp = getTokenService();
				
				if(!tokenServiceTmp.checkTimeBasedSubscriber(subscriberId, streamId, sessionId, subscriberCodeText, false)) {
					httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "Time Based subscriber id or code is invalid");
					logger.warn("subscriber request for subscriberIDor subscriberCode is not valid");
					return; 					
				}
			}
			
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
					httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN, NOT_INITIALIZED);
					logger.warn("Token service is not initialized. Server is getting started for stream id:{} from request: {}", streamId, clientIP);
					return;
				}
			}

			if (appSettings.isHashControlPlayEnabled()) 
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
					httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN, NOT_INITIALIZED);
					logger.warn("Token service is not initialized. Server is getting started for stream id:{} from request: {}", streamId, clientIP);
					return;
				}
			}
			
			if (appSettings.isPlayJwtControlEnabled()) 
			{
				ITokenService tokenServiceTmp = getTokenService();
				if (tokenServiceTmp != null) 
				{
					if (!tokenServiceTmp.checkJwtToken(tokenId, streamId, Token.PLAY_TOKEN)) {
						httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN,"Invalid JWT Token");
						logger.warn("JWT token is not valid");
						return; 
					}
				}
				else {
					httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN, NOT_INITIALIZED);
					logger.warn("JWT Token service is not initialized. Server is getting started for stream id:{} from request: {}", streamId, clientIP);
					return;
				}
			}
			chain.doFilter(request, response);	
		}
		else {
			httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN,"Invalid Request Type");
		}
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
		int startIndex = requestURI.indexOf('/');
		
		requestURI = requestURI.split("streams")[1];
		
		//if request is adaptive file (ending with _adaptive.m3u8)
		endIndex = requestURI.lastIndexOf(MuxAdaptor.ADAPTIVE_SUFFIX + ".m3u8");
		if (endIndex != -1) {
			return requestURI.substring(startIndex+1, endIndex);
		}
		
		//if specific bitrate is requested
		String hlsRegex = "(.*)_[0-9]+p.m3u8$";  // matches ending with _[resolution]p.m3u8
		if (requestURI.matches(hlsRegex)) {
			endIndex = requestURI.lastIndexOf('_'); //because file format is [NAME]_[RESOLUTION]p.m3u8
			return requestURI.substring(startIndex+1, endIndex);
		}
		
		//if just the m3u8 file
		endIndex = requestURI.lastIndexOf(".m3u8");
		if (endIndex != -1) {
			return requestURI.substring(startIndex+1, endIndex);
		}
		
		//if specific ts file requested
		String tsRegex = "(.*)_[0-9]+p+[0-9][0-9][0-9][0-9].ts$";  // matches ending with _[_240p0000].ts or default ts file extension  _[_0p0000].ts
		if (requestURI.matches(tsRegex)) {
			endIndex = requestURI.lastIndexOf('_'); //because file format is [NAME]_[RESOLUTION]p[0000].ts
			return requestURI.substring(startIndex+1, endIndex);
		}

		//if multiple files with same id requested such as : 541211332342978513714151_480p_1.mp4 or 541211332342978513714151_480p.mp4 
		String mp4Regex2 = "(.*)+(_[0-9]+p+_[0-9]|_|_[0-9])+.mp4$"; 
		if (requestURI.matches(mp4Regex2)) {
			endIndex = requestURI.lastIndexOf('_'); //if multiple files with same id requested such as : 541211332342978513714151_480p_1.mp4 
			//_480p regex
 			String mp4resolutionRegex = "(.*)+_[0-9]+p$"; 
			if(requestURI.substring(startIndex+1, endIndex).matches(mp4resolutionRegex)) {
				endIndex = requestURI.substring(startIndex, endIndex).lastIndexOf('_');
			}
			return requestURI.substring(startIndex+1, endIndex);
		}
	
		//if default mp4 file requested such as: 541211332342978513714151.mp4
		endIndex = requestURI.lastIndexOf(".mp4");
		if (endIndex != -1) {
			return requestURI.substring(startIndex+1, endIndex);
		}
		
		return null;
	}
	
	
}
