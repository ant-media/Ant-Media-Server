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


public class IPFilter implements Filter {

	protected Logger log = LoggerFactory.getLogger(IPFilter.class);
	private FilterConfig config;

	public void init(FilterConfig filterConfig) throws ServletException {
		this.config = filterConfig;
	}

	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		if (isAllowed(request.getRemoteAddr())) {
			chain.doFilter(request, response);
			return;
		}
		((HttpServletResponse) response).sendError(HttpServletResponse.SC_FORBIDDEN, "Not allowed IP");
	}


	public AppSettings getAppSettings() {
		ApplicationContext context = (ApplicationContext) config.getServletContext().getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
		return (AppSettings) context.getBean(AppSettings.BEAN_NAME);
	}
	
	public void destroy() {
		//nothing to clean up
	}


	/**
	 * Test if a remote's IP address is allowed to proceed.
	 *
	 * @param property The remote's IP address, as a string
	 * @return true if allowed
	 */
	private boolean isAllowed(final String property) {
		final InetAddress addr;

		try {
			addr = InetAddress.getByName(property);
		} catch (UnknownHostException e) {
			// This should be in the 'could never happen' category but handle it
			// to be safe.
			log.error("error", e);
			return false;
		}
		List<NetMask> allowedCIDRList = getAppSettings().getAllowedCIDRList();
		
		for (final NetMask nm : allowedCIDRList) {
			if (nm.matches(addr)) {
				return true;
			}
		}

		// Deny this request
		return false;
	}

}
