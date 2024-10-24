package io.antmedia.filter;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.antmedia.AppSettings;


public class IPFilter extends AbstractFilter {

	protected static Logger log = LoggerFactory.getLogger(IPFilter.class);

	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException 
	
	{
		
		/**
		 * This filter is being used for accessing applications REST API so that return valid is if {@link #isAllowed) or it's coming from isNodeCommunicationTokenValid
		 * Check the {@code RestProxyFilter} for getting more information about isNodeCommunicationTokenValid
		 * 
		 */
		
		HttpServletRequest httpRequest = (HttpServletRequest) request;
		if (isAllowed(request.getRemoteAddr()) || RestProxyFilter.isNodeCommunicationTokenValid(httpRequest.getHeader(TokenFilterManager.TOKEN_HEADER_FOR_NODE_COMMUNICATION),  getAppSettings().getClusterCommunicationKey(), httpRequest.getRequestURI())) {
			chain.doFilter(request, response);
			return;
		}
		
		if(((HttpServletRequest)request).getRequestURL().toString().contains("rest/v2/acm")) {
			chain.doFilter(request, response);
			return;
		}
		((HttpServletResponse) response).sendError(HttpServletResponse.SC_FORBIDDEN, "Not allowed IP");
	}

	/**
	 * Test if a remote's IP address is allowed to proceed.
	 *
	 * @param remoteIPAdrress The remote's IP address, as a string
	 * @return true if allowed
	 */
	public boolean isAllowed(final String remoteIPAdrress) {
		AppSettings appSettings = getAppSettings();
		boolean result = false;
		if(appSettings != null) 
		{
			if (appSettings.isIpFilterEnabled()) {
				result = checkCIDRList(appSettings.getAllowedCIDRList(),remoteIPAdrress);
			}
			else {
				result = true;
			}
		}
		
		return result;
	}
}
