package io.antmedia.filter;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.util.NetMask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.antmedia.settings.ServerSettings;

public class IPFilterDashboard extends AbstractFilter{

	protected static Logger logger = LoggerFactory.getLogger(IPFilterDashboard.class);

	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

		if (isAllowedDashboard(request.getRemoteAddr())) {
			chain.doFilter(request, response);
			return;
		}

		((HttpServletResponse) response).sendError(HttpServletResponse.SC_FORBIDDEN, "Not allowed IP");
	}


	public boolean isAllowedDashboard(final String property) {

		ServerSettings serverSettings = getServerSetting();

		if (serverSettings != null) {
			
			return checkCIDRList(serverSettings.getAllowedCIDRList(),property);
			
		}
		// Deny this request
		return false;
	}	

}