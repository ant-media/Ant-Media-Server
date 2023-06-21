package io.antmedia.filter;

import java.io.File;
import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.HttpMethod;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import io.antmedia.AppSettings;
import io.antmedia.cluster.IClusterNotifier;
import io.antmedia.datastore.db.DataStore;
import io.antmedia.datastore.db.DataStoreFactory;
import io.antmedia.datastore.db.IDataStoreFactory;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.muxer.IAntMediaStreamHandler;
import io.antmedia.rest.servlet.EndpointProxy;

public class RestProxyFilter extends AbstractFilter {

	protected static Logger log = LoggerFactory.getLogger(RestProxyFilter.class);

	private DataStore dataStore;

	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException 
	{
		HttpServletRequest httpReq = (HttpServletRequest) request;
		String reqURI = httpReq.getRequestURI();

		String method = httpReq.getMethod();

		if (HttpMethod.POST.equals(method) || HttpMethod.PUT.equals(method) || HttpMethod.DELETE.equals(method)) 
		{
			String streamId = getStreamId(reqURI);
			if (streamId != null && !streamId.isEmpty()) 
			{
				Broadcast broadcast = getBroadcast(httpReq, streamId);
				log.debug("STREAM ID = {} BROADCAST = {} ", streamId, broadcast);
				
				//If it is not related with the broadcast, we can skip this filter
				if (broadcast == null 
						|| !IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING.equals(broadcast.getStatus())
						|| isInSameNodeInCluster(request.getRemoteAddr(), broadcast.getOriginAdress()) ) 
				{
					chain.doFilter(request, response);
				}
				else
				{
					AppSettings settings = getAppSettings();
					String originAdress = "http://" + broadcast.getOriginAdress() + ":" + getServerSetting().getDefaultHttpPort()  + File.separator + settings.getAppName() + "/rest";
					log.info("Redirecting request to origin {}", originAdress);
					EndpointProxy endpointProxy = new EndpointProxy();
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

	public DataStore getDataStore() {
		if(dataStore == null) {
			ApplicationContext context = getAppContext();
			if(context != null){
				dataStore = ((DataStoreFactory) context.getBean(IDataStoreFactory.BEAN_NAME)).getDataStore();
			}
			else{
				log.error("RestProxyFilter is not initialized because context returns null");
			}
		}
		return dataStore;
	}

}