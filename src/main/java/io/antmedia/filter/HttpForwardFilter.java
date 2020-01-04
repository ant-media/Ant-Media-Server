package io.antmedia.filter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.antmedia.AppSettings;

public class HttpForwardFilter extends AbstractFilter {

	private static final String SLASH = "/";
	protected static Logger logger = LoggerFactory.getLogger(HttpForwardFilter.class);
	private static final String COMMA = ",";
	
	private static final String DOUBLE_DOT =  "..";
	

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {

		String requestURI = ((HttpServletRequest)request).getRequestURI();
		if (requestURI != null && !requestURI.isEmpty()) {

			AppSettings appSettings = getAppSettings();

			if (appSettings != null) 
			{
				String httpForwardingExtension = appSettings.getHttpForwardingExtension(); 
				String httpForwardingBaseURL = appSettings.getHttpForwardingBaseURL();

				if (httpForwardingExtension != null && !httpForwardingExtension.isEmpty() &&
						httpForwardingBaseURL != null && !httpForwardingBaseURL.isEmpty()) 
				{
					String[] extension = httpForwardingExtension.split(COMMA);
					for (int i = 0; i < extension.length; i++) 
					{
						if (requestURI.endsWith(extension[i])) {
							
							String redirectUri = httpForwardingBaseURL + requestURI.substring(requestURI.indexOf(SLASH, 1));
							HttpServletResponse httpResponse = (HttpServletResponse) response;
							if (redirectUri.contains(DOUBLE_DOT)) {
								throw new IOException("URI is not well formatted");
							}
							httpResponse.sendRedirect(redirectUri);
							//return immediately
							return;
						}
					}
					logger.trace("Extensions({}) does match the request uri: {}", extension, requestURI);
				}
				else {
					logger.trace("No forwarding because extension or url not set for request: {}", requestURI);
				}
			}
		}

		chain.doFilter(request, response);
	}
	
	

}
