package io.antmedia.filter;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.util.NetMask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.WebApplicationContext;

import io.antmedia.AppSettings;


public class IPFilter extends AbstractFilter {

	protected static Logger log = LoggerFactory.getLogger(IPFilter.class);

	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		if (isAllowed(request.getRemoteAddr())) {
			chain.doFilter(request, response);
			return;
		}
		((HttpServletResponse) response).sendError(HttpServletResponse.SC_FORBIDDEN, "Not allowed IP");
	}



	/**
	 * Test if a remote's IP address is allowed to proceed.
	 *
	 * @param property The remote's IP address, as a string
	 * @return true if allowed
	 */
	public boolean isAllowed(final String property) 
	{
		
		AppSettings appSettings = getAppSettings();
		if (appSettings != null) 
		{
			try {
				InetAddress addr = InetAddress.getByName(property);
				List<NetMask> allowedCIDRList = appSettings.getAllowedCIDRList();

				for (final NetMask nm : allowedCIDRList) {
					if (nm.matches(addr)) {
						return true;
					}
				}
				
			} catch (UnknownHostException e) {
				// This should be in the 'could never happen' category but handle it
				// to be safe.
				log.error("error", e);
			}
		}
		// Deny this request
		return false;
	}

}
