package io.antmedia.filter;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Date;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.AppSettings;
import io.antmedia.cluster.IClusterNotifier;
import io.antmedia.datastore.db.DataStore;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.datastore.db.types.Subscriber;
import io.antmedia.rest.servlet.EndpointProxy;
import io.antmedia.settings.ServerSettings;
import jakarta.ws.rs.HttpMethod;

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

		String method = httpRequest.getMethod();

		if (HttpMethod.POST.equals(method) || HttpMethod.PUT.equals(method) || HttpMethod.DELETE.equals(method)) 
		{
			String streamId = getStreamId(httpRequest.getRequestURI());

			if (streamId != null && !streamId.isEmpty())
			{
				Broadcast broadcast = getBroadcast(httpRequest, streamId);
				boolean subscriberBlockReq = isSubscriberBlockReq(httpRequest.getRequestURI());

				log.debug("STREAM ID = {} BROADCAST = {} ", streamId, broadcast);
				//If it is not related with the broadcast, we can skip this filter
				if (broadcast != null && subscriberBlockReq) {
					try {
						//We must wrap request otherwise we cannot read it multiple times.(here and on BroadcastRestService)
						//Need to extract subscriberId from request body so that we can get its registeredNodeIp and redirect request accordingly.

						String subscriberId = getSubscriberId(httpRequest.getRequestURI());
						if(subscriberId != null)
						{
							DataStore dataStore = getDataStore();
							Subscriber subscriber = dataStore.getSubscriber(streamId, subscriberId);
							
							if (subscriber != null && !StringUtils.isBlank(subscriber.getRegisteredNodeIp()) 
									&& !isRequestDestinedForThisNode(request.getRemoteAddr(), subscriber.getRegisteredNodeIp())
									&& isHostRunning(subscriber.getRegisteredNodeIp(), getServerSettings().getDefaultHttpPort())) 
							{
								forwardRequestToNode(request, response, subscriber.getRegisteredNodeIp());
							} 
							else 
							{
								chain.doFilter(request, response);
							}
						}
						else
						{
							chain.doFilter(request, response);
						}

					} catch (IOException e) {
						logger.error(e.getMessage());
					}
				}

				/** 
				 * if broadcast is not null and it's streaming and this node is not destined for this node,
				 * forward the request to the origin address. This also handles the scenario if the origin server is dead or broadcast stuck
				 * because AntMediaApplicationAdapter.isStreaming checks the last update time
				 */
				else if (broadcast != null && AntMediaApplicationAdapter.isStreaming(broadcast)
						&& !isRequestDestinedForThisNode(request.getRemoteAddr(), broadcast.getOriginAdress())
						&& isHostRunning(broadcast.getOriginAdress(), getServerSettings().getDefaultHttpPort())) 
				{


					forwardRequestToNode(request, response, broadcast.getOriginAdress());
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


	public boolean isHostRunning(String address, int port) {

		try(Socket socket = new Socket()) {
			
			SocketAddress sockaddr = new InetSocketAddress(address, port);
			socket.connect(sockaddr, 5000);
		}
		catch (NumberFormatException | IOException e) {
			return false;
		}
		return true;
	}


	public void forwardRequestToNode(ServletRequest request, ServletResponse response, String registeredNodeIp) throws IOException, ServletException 
	{
		//token validity is 5 seconds -> 5000
		String jwtToken = JWTFilter.generateJwtToken(getAppSettings().getClusterCommunicationKey(), System.currentTimeMillis() + 5000);
		AppSettings appSettings = getAppSettings();
		ServerSettings serverSettings = getServerSettings();
		String restRouteOfSubscriberNode = "http://" + registeredNodeIp + ":" + serverSettings.getDefaultHttpPort()  + File.separator + appSettings.getAppName() + File.separator+ "rest";
		log.info("Redirecting the request({}) to node {}", ((HttpServletRequest)request).getRequestURI(), registeredNodeIp);
		EndpointProxy endpointProxy = new EndpointProxy(jwtToken);
		endpointProxy.initTarget(restRouteOfSubscriberNode);
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
	 * REST method is in this format "/{id}/subscribers/{sid}/block" -> {@code BroadcastRestService#blockSubscriber(String, String, Subscriber)}
	 * We're going to get the {sid} from the url
	 * @param reqURI
	 * @return
	 */
	private String getSubscriberId(String reqURI) {
		try{

			reqURI = reqURI.split("subscribers/")[1];
			//reqURI is now {sid}/block
			return reqURI.substring(0, reqURI.indexOf("/"));
		}
		catch (ArrayIndexOutOfBoundsException | StringIndexOutOfBoundsException e){
			logger.warn("There is no subscriber id in the URI");
		}
		
		
		return null;
	}

	public boolean isSubscriberBlockReq(String requestUri){
		//Using raw string here as identifier is not a good practice. find a better way
		return requestUri.contains("subscribers") && requestUri.contains("block");

	}

	/**
	 * Check if the request should be handled in this node
	 * 
	 * @param requestAddress
	 * @param nodeAddress
	 * @return true if this node should handle this request or return false
	 */
	public  boolean isRequestDestinedForThisNode(String requestAddress, String nodeAddress) {
		ApplicationContext context = getAppContext();
		boolean isCluster = context.containsBean(IClusterNotifier.BEAN_NAME);
		return !isCluster || requestAddress.equals(getServerSettings().getHostAddress())
				|| getServerSettings().getHostAddress().equals(nodeAddress);
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