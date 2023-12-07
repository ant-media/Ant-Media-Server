package io.antmedia.filter;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;

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


	public boolean isAllowedDashboard(final String remoteIPAdrress){
		ServerSettings serverSettings = getServerSettings();
		if (serverSettings != null){
			return checkCIDRList(serverSettings.getAllowedCIDRList(),remoteIPAdrress);
		}
		// Deny this request
		return false;
	}	

}