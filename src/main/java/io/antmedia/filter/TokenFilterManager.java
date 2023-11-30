package io.antmedia.filter;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.antmedia.AppSettings;
import io.antmedia.datastore.db.types.Subscriber;
import io.antmedia.datastore.db.types.Token;
import io.antmedia.muxer.MuxAdaptor;
import io.antmedia.muxer.Muxer;
import io.antmedia.security.ITokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.HttpMethod;

public class TokenFilterManager extends AbstractFilter   {

	private static final String REPLACE_CHARS_REGEX = "[\n|\r|\t]";
	public static final String NOT_INITIALIZED= "Not initialized";
	protected static Logger logger = LoggerFactory.getLogger(TokenFilterManager.class);
	public static final String TOKEN_HEADER_FOR_NODE_COMMUNICATION = "ClusterAuthorization";


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

		if (appSettings == null) {
			httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "Server is getting initialized.");
			logger.warn("AppSettings not initialized. Server is getting started for stream id:{} from request: {}", streamId, clientIP);
			return;
		}


		logger.debug("Client IP: {}, request url:  {}, token:  {}, sessionId: {},streamId:  {} ",clientIP 
				,httpRequest.getRequestURI(), tokenId, sessionId, streamId);



		/**
		 * In cluster mode, edges make HLS request to Origin. Token isn't passed with these requests.
		 * So if token is enabled, origin returns 403. In order to resolve this issue
		 * we generate an jwt token based on application secret key {@code AppSettings#clusterCommunicationKey} and add to the header {@code JWTFilter#JWT_TOKEN_HEADER}
		 * If there is a header like, it checks and bypass token control.
		 */


		if (HttpMethod.GET.equals(method) || HttpMethod.HEAD.equals(method)) 
		{
			if (streamId == null) {
				logger.warn("No streamId found in the request: {}", httpRequest.getRequestURI());
				return ;
			}
			
			ITokenService tokenServiceTmp = getTokenService();

			if (tokenServiceTmp == null) {
				httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN, TokenFilterManager.NOT_INITIALIZED);
				logger.warn("Token service is not initialized. Server is getting started for stream id:{} from request: {}", streamId, clientIP);
				return;
			}

			String jwtInternalCommunicationToken = httpRequest.getHeader(TOKEN_HEADER_FOR_NODE_COMMUNICATION);

			
			if (jwtInternalCommunicationToken != null) 
			{
				//if jwtInternalCommunicationToken is not null, 
				//it means that this is the origin instance and receiving request from the edge node directly
				
				boolean checkJwtToken = tokenServiceTmp.isJwtTokenValid(jwtInternalCommunicationToken, appSettings.getClusterCommunicationKey(), streamId, Token.PLAY_TOKEN);
				if (!checkJwtToken) 
				{
					httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "Cluster communication token is not valid for streamId:" + streamId);
					logger.warn("Cluster communication token is not valid for streamId:{}" , streamId);
					return; 	
				}
			}
			else
			{
				// if it enters this block, it means 
				// 1. server may be is in cluster mode and this is edge node
				// 2. server in standalone mode

				if ((appSettings.isTimeTokenSubscriberOnly() || appSettings.isEnableTimeTokenForPlay()) && 
						!tokenServiceTmp.checkTimeBasedSubscriber(subscriberId, streamId, sessionId, subscriberCodeText, Subscriber.PLAY_TYPE)) {
					httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "Time Based subscriber id or code is invalid");
					logger.warn("subscriber request for subscriberID or subscriberCode is not valid for streamId: {}", streamId);
					return; 					
				}

				if (appSettings.isPlayTokenControlEnabled() && !tokenServiceTmp.checkToken(tokenId, streamId, sessionId, Token.PLAY_TOKEN)) {
					httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid Token for streamId:" + streamId);
					logger.warn("token {} is not valid for stream id:{}", tokenId, streamId);
					return; 
				}

				if (appSettings.isHashControlPlayEnabled() && !tokenServiceTmp.checkHash(tokenId, streamId, sessionId, Token.PLAY_TOKEN)) {
					httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN,"Invalid Hash");
					logger.warn("hash {} is not valid", tokenId);
					return; 
				}

				if (appSettings.isPlayJwtControlEnabled() && !tokenServiceTmp.checkJwtToken(tokenId, streamId, sessionId, Token.PLAY_TOKEN)) {
					httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN,"Invalid JWT Token");
					logger.warn("JWT token is not valid");
					return; 
				}
			}
			chain.doFilter(request, response);	
		}
		else {
			httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid Request Type");
			logger.warn("Invalid method type({}) for stream: {} and request uri: {}", method, streamId, httpRequest.getRequestURI());
		}
	}


	public static String getStreamId(String requestURI) {
		requestURI = requestURI.replaceAll(REPLACE_CHARS_REGEX, "_");

		int endIndex;
		int startIndex = requestURI.indexOf('/');

		if(requestURI.contains("streams")) {
			requestURI = requestURI.split("streams")[1];
		}

		if(requestURI.contains("m4s") || requestURI.contains("mpd")) {
			startIndex = requestURI.indexOf("/");
			endIndex = requestURI.lastIndexOf("/");
			if(endIndex == 0){
				return requestURI;
			}
			return requestURI.substring(startIndex+1, endIndex);
		}


		else if(requestURI.contains("chunked")) {
			requestURI = requestURI.split("chunked")[1];
			startIndex = requestURI.indexOf("/");
			endIndex = requestURI.lastIndexOf("/");
			return requestURI.substring(startIndex+1, endIndex);
		}


		//if request is adaptive file (ending with _adaptive.m3u8)
		endIndex = requestURI.lastIndexOf(MuxAdaptor.ADAPTIVE_SUFFIX + ".m3u8");
		if (endIndex != -1) {
			return requestURI.substring(requestURI.lastIndexOf("/")+1, endIndex);
		}

		//if specific bitrate is requested
		String hlsRegex = "(.*)_([0-9]+p|[0-9]+kbps|[0-9]+p[0-9]+kbps).m3u8$"; // matches ending with _[resolution]p[bitrate]kbps.m3u8 or _[resolution]p.m3u8 or _[bitrate]kbps.m3u8
		if (requestURI.matches(hlsRegex)) {
			endIndex = requestURI.lastIndexOf('_'); //because file format is [NAME]_[RESOLUTION]p[bitrate]kbps.m3u8 or [NAME]_[RESOLUTION]p.m3u8 or _[bitrate]kbps.m3u8
			return requestURI.substring(requestURI.lastIndexOf("/")+1, endIndex);
		}

		//if just the m3u8 file
		endIndex = requestURI.lastIndexOf(".m3u8");
		if (endIndex != -1) {
			return requestURI.substring(requestURI.lastIndexOf("/")+1, endIndex);
		}

		//if specific ts file requested
		String tsRegex = "(.*)_([0-9]+p|[0-9]+kbps|[0-9]+p[0-9]+kbps)+[0-9]{" + Muxer.SEGMENT_INDEX_LENGTH + "}.ts$";  // matches ending with _[_240p300kbps0000].ts or _[_300kbps0000].ts or _[_240p0000].ts default ts file extension _[0000].ts
		if (requestURI.matches(tsRegex)) {
			endIndex = requestURI.lastIndexOf('_'); //because file format is [NAME]_[RESOLUTION]p[0000].ts
			return requestURI.substring(requestURI.lastIndexOf("/")+1, endIndex);
		}

		//for backward compatibility
		tsRegex = "(.*)_([0-9]+p|[0-9]+kbps|[0-9]+p[0-9]+kbps)+[0-9]{4}.ts$";  // matches ending with _[_240p300kbps0000].ts or _[_300kbps0000].ts or _[_240p0000].ts default ts file extension _[0000].ts
		if (requestURI.matches(tsRegex)) {
			endIndex = requestURI.lastIndexOf('_'); //because file format is [NAME]_[RESOLUTION]p[0000].ts
			return requestURI.substring(requestURI.lastIndexOf("/")+1, endIndex);
		}

		tsRegex = "(.*)[0-9]{"+ Muxer.SEGMENT_INDEX_LENGTH +"}.ts$";  // matches default ts file extension  [0000].ts
		if (requestURI.matches(tsRegex)) {
			endIndex = requestURI.lastIndexOf('.'); //because file format is [NAME][0000].ts
			return requestURI.substring(requestURI.lastIndexOf("/")+1, endIndex-Muxer.SEGMENT_INDEX_LENGTH);
		}

		//for backward compatibility
		tsRegex = "(.*)[0-9]{4}.ts$";  // matches default ts file extension  [0000].ts
		if (requestURI.matches(tsRegex)) {
			endIndex = requestURI.lastIndexOf('.'); //because file format is [NAME][0000].ts
			return requestURI.substring(requestURI.lastIndexOf("/")+1, endIndex-4);
		}

		//streamId_underline_test-2021-05-18_11-26-26.842.mp4 and streamId_underline_test-2021-05-18_11-26-26.842_360p500kbps.mp4 
		String vodDatetimeRegex = "(.*)+(-20)[0-9][0-9]+(-)+([0-9][0-9])+(.*)";
		String vodResolutionBitrateRegex = "(.*)+_[0-9]+p+[0-9]+kbps+(.*)";
		if (requestURI.matches(vodDatetimeRegex)) 
		{
			endIndex = requestURI.lastIndexOf('_'); //if multiple files with same id requested such as : 541211332342978513714151_480p_1.mp4 
			startIndex = requestURI.lastIndexOf("/");
			//_480p regex
			if(requestURI.matches(vodResolutionBitrateRegex)) {
				requestURI = requestURI.substring(startIndex, endIndex);
				endIndex = requestURI.lastIndexOf('.');
				//Remove -2021-05-18_11-26-26 character size
				endIndex -= Muxer.DATE_TIME_PATTERN.length()-3; 
				startIndex = 0;
			}
			else {
				//Remove -2021-05-18 character size
				endIndex -= Muxer.DATE_TIME_PATTERN.length()-12;
			}
			return requestURI.substring(startIndex+1, endIndex);
		}

		//if multiple files with same id requested such as : 541211332342978513714151_480p5000kbps_1.mp4 or 541211332342978513714151_480p500kbps.mp4 
		if (requestURI.matches(vodResolutionBitrateRegex)) 
		{
			endIndex = requestURI.lastIndexOf('_'); //if multiple files with same id requested such as : 541211332342978513714151_480p500kbps_1.mp4 
			startIndex = requestURI.lastIndexOf("/");
			if(requestURI.substring(startIndex+1, endIndex).matches(vodResolutionBitrateRegex)) 
			{
				requestURI = requestURI.substring(startIndex, endIndex);
				endIndex = requestURI.lastIndexOf('_');
				startIndex = 0;
			}
			return requestURI.substring(startIndex+1, endIndex);
		}

		//if default mp4 file requested such as: 541211332342978513714151.mp4, 541211332342978513714151_23.mp4
		String underScoreRegex = "(.*)_[0-9]+(.*)";
		endIndex = requestURI.lastIndexOf(".mp4");
		if (endIndex == -1) 
		{
			//if default webm file requested such as: 541211332342978513714151.webm
			endIndex = requestURI.lastIndexOf(".webm");
		}

		if (endIndex != -1) 
		{
			if (requestURI.matches(underScoreRegex)) 
			{
				endIndex = requestURI.lastIndexOf("_");
			}
			return requestURI.substring(requestURI.lastIndexOf("/")+1, endIndex);
		}


		return null;
	}


}
