package io.antmedia.filter;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.HttpMethod;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.AppSettings;
import io.antmedia.cluster.IClusterNotifier;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.muxer.IAntMediaStreamHandler;
import io.antmedia.rest.servlet.EndpointProxy;

/**
 * This filter forwards incoming requests to the origin node that is responsible for that stream.
 * It adds JWT token for security check
 * 
 * @author mekya
 *
 */
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
				
				/** 
				 * if broadcast is not null and it's streaming and this node is not destined for this node,
				 * forward the request to the origin address. This also handles the scenario if the origin server is dead or broadcast stuck
				 * because AntMediaApplicationAdapter.isStreaming checks the last update time
				 */
				if (broadcast != null && AntMediaApplicationAdapter.isStreaming(broadcast) 
						&& !isRequestDestinedForThisNode(request.getRemoteAddr(), broadcast.getOriginAdress())) 
				{
					
					
					forwardRequestToOrigin(request, response, broadcast);
				}
				else 
				{
					/**
					 * This means that this request should be handled in this node so let chain.doFilter
					 *
					 * Security check of this key is done on the {@link IPFilter} because IPFilter may block proceeding this kind of requests
					 *
					 * The IPFilter security filters will proceed in this flow
					 * If its security check is successful, it will proceed
					 * If its security check is failed, 
					 *  it will check if there is a valid header(TokenFilterManager.TOKEN_HEADER_FOR_NODE_COMMUNICATION)({@link AbstractFilter#isNodeCommunicationTokenValid(HttpServletRequest httpRequest)} ), 
					 *   If yes, it will proceed
					 *   If not, it returns 403 error
					 */
					   
					
					chain.doFilter(request, response);	
					
					
				}
				
			}
		}
		else 
		{
			chain.doFilter(request, response);
		}
	}


	public void forwardRequestToOrigin(ServletRequest request, ServletResponse response, Broadcast broadcast) throws ServletException, IOException {
		
		//token validity is 5 seconds -> 5000
		String jwtToken = generateJwtToken(getAppSettings().getClusterCommunicationKey(), System.currentTimeMillis() + 5000);
		AppSettings settings = getAppSettings();
		String originAdress = "http://" + broadcast.getOriginAdress() + ":" + getServerSetting().getDefaultHttpPort()  + File.separator + settings.getAppName() + "/rest";
		log.info("Redirecting the request({}) to origin {}", ((HttpServletRequest)request).getRequestURI(), originAdress);
		EndpointProxy endpointProxy = new EndpointProxy(jwtToken);
		endpointProxy.initTarget(originAdress);
		endpointProxy.service(request, response);
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

	/**
	 * Check if the request should be handled in this node
	 * 
	 * @param requestAddress
	 * @param streamOriginAddress
	 * @return true if this node should handle this request or return false
	 */
	public  boolean isRequestDestinedForThisNode(String requestAddress, String streamOriginAddress) {
		ApplicationContext context = getAppContext();
		boolean isCluster = context.containsBean(IClusterNotifier.BEAN_NAME);
		return !isCluster || requestAddress.equals(getServerSetting().getHostAddress())
				|| getServerSetting().getHostAddress().equals(streamOriginAddress);
	}
	
	
	public static String generateJwtToken(String jwtSecretKey, long expireDateUnixTimeStampMs) {
		Date expireDateType = new Date(expireDateUnixTimeStampMs);
		String jwtTokenId = null;
		try {
			Algorithm algorithm = Algorithm.HMAC256(jwtSecretKey);

			jwtTokenId = JWT.create().
					withExpiresAt(expireDateType).
					sign(algorithm);

		} catch (Exception e) {
			logger.error(ExceptionUtils.getStackTrace(e));
		}

		return jwtTokenId;
	}
	
	/**
	 * This method checks if there is a token in the header for internal node communication and if it exists, checks its validity
	 * 
	 * @param httpRequest
	 * @return true if there is a token and it's valid. Otherwise it returns false.
	 * 
	 */
	public static  boolean isNodeCommunicationTokenValid(String jwtInternalCommunicationToken, String jwtSecretKey, String requestURI) 
	{
		boolean result = false;
		if (jwtInternalCommunicationToken != null)
		{
			result = JWTFilter.isJWTTokenValid(jwtSecretKey, jwtInternalCommunicationToken);
			if(result) {
				logger.info("Request forwarded:{} by another node is validated successfully", requestURI);
			}
			else {
				logger.warn("Requested forwarded:{} by another node is failed because cluster jwt token is valid", requestURI);
			}
		}
		else {
			logger.debug("Node communicaiton header:{} is not found ", TokenFilterManager.TOKEN_HEADER_FOR_NODE_COMMUNICATION);
		}
		return  result;
	}
}