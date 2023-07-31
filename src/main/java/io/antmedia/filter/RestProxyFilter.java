package io.antmedia.filter;

import java.io.File;
import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.HttpMethod;

import io.antmedia.datastore.db.types.Token;
import io.antmedia.security.ITokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import io.antmedia.AppSettings;
import io.antmedia.cluster.IClusterNotifier;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.muxer.IAntMediaStreamHandler;
import io.antmedia.rest.servlet.EndpointProxy;

public class RestProxyFilter extends AbstractFilter {

	protected static Logger log = LoggerFactory.getLogger(RestProxyFilter.class);

	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException
	{
		HttpServletRequest httpRequest =(HttpServletRequest)request;
		HttpServletResponse httpResponse = (HttpServletResponse)response;

		String method = httpRequest.getMethod();

		if (HttpMethod.POST.equals(method) || HttpMethod.PUT.equals(method) || HttpMethod.DELETE.equals(method)) 
		{
			String streamId = getStreamId(httpRequest.getRequestURI());

			if (streamId != null && !streamId.isEmpty())
			{
				Broadcast broadcast = getBroadcast(httpRequest, streamId);
				log.debug("STREAM ID = {} BROADCAST = {} ", streamId, broadcast);

				//If it is not related with the broadcast, we can skip this filter
				if (broadcast == null
						|| !IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING.equals(broadcast.getStatus())
						|| isInSameNodeInCluster(request.getRemoteAddr(), broadcast.getOriginAdress())
						|| isForwardedByAnotherNode(streamId, httpRequest.getHeader(TokenFilterManager.TOKEN_HEADER_FOR_NODE_COMMUNICATION)))
				{
					chain.doFilter(request, response);
				}
				else
				{
					String jwtToken = getJwtInternalToken(streamId);
					AppSettings settings = getAppSettings();
					String originAdress = "http://" + broadcast.getOriginAdress() + ":" + getServerSetting().getDefaultHttpPort()  + File.separator + settings.getAppName() + "/rest";
					log.info("Redirecting the request to origin {}", originAdress);
					EndpointProxy endpointProxy = new EndpointProxy(jwtToken);
					endpointProxy.initTarget(originAdress);
					endpointProxy.service(request, response);
				}
			}
		}
		else 
		{
			chain.doFilter(request, response);
		}
	}

	public boolean isForwardedByAnotherNode(String streamId, String jwtInternalCommunicationToken) {
		boolean result = false;
		if (jwtInternalCommunicationToken != null)
		{
			result = getTokenService().isJwtTokenValid(jwtInternalCommunicationToken, getAppSettings() .getClusterCommunicationKey(), streamId, Token.PLAY_TOKEN);
			if(result) {
				log.info("Request forwarded by another node is received for stream id: {}", streamId);
			}
		}
		return  result;
	}

	public String getJwtInternalToken(String streamId) {
		return ITokenService.generateJwtToken(getAppSettings().getClusterCommunicationKey(), streamId , System.currentTimeMillis() + 30000, Token.PLAY_TOKEN);
	}

	public String getStreamId(String reqURI){
		try{
			reqURI = reqURI.split("broadcasts/")[1];
		}
		catch (ArrayIndexOutOfBoundsException e){
			return null;
		}
		if(reqURI.contains("/"))
			reqURI = reqURI.substring(0, reqURI.indexOf("/"));
		return reqURI;
	}

	public  boolean isInSameNodeInCluster(String requestAddress, String streamOriginAddress) {
		ApplicationContext context = getAppContext();
		boolean isCluster = context.containsBean(IClusterNotifier.BEAN_NAME);
		return !isCluster || requestAddress.equals(getServerSetting().getHostAddress())
				|| getServerSetting().getHostAddress().equals(streamOriginAddress);
	}
}