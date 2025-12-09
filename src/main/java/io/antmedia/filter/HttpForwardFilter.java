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
import io.antmedia.rest.RestServiceBase;

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
				String httpForwardingBaseURL = appSettings.getHttpForwardingBaseURL();
			    String httpForwardingExtension = appSettings.getHttpForwardingExtension();
	
		        String redirectUri = getRedirectUrl(requestURI, httpForwardingBaseURL, httpForwardingExtension);
	
		        if (redirectUri != null && !HlsManifestModifierFilter.isHLSIntervalQuery((HttpServletRequest) request)) {
		            HttpServletResponse httpResponse = (HttpServletResponse) response;
		            httpResponse.sendRedirect(redirectUri);
		            return; // return immediately after redirect
		        }
	
		        logger.trace("No matching extension or forwarding settings for request URI: {}", requestURI);
			}
	    }

	    chain.doFilter(request, response);
	}

	public static String getRedirectUrl(String requestURI, String httpForwardingBaseURL, String httpForwardingExtension) throws IOException {
	    if (httpForwardingExtension == null || httpForwardingExtension.isEmpty() ||
	        httpForwardingBaseURL == null || httpForwardingBaseURL.isEmpty()) {
	        return null; // Incomplete settings
	    }

	    String[] extensions = httpForwardingExtension.split(COMMA);
	    for (String extension : extensions) {
	        if (requestURI.endsWith(extension)) {
	            String subURI = requestURI.substring(requestURI.indexOf(SLASH, 1));

	            String normalizedBaseURL = httpForwardingBaseURL.endsWith(SLASH) ? httpForwardingBaseURL : httpForwardingBaseURL + SLASH;
	            String normalizedSubURI = subURI.startsWith(SLASH) ? subURI.substring(1) : subURI;

	            String redirectUri = normalizedBaseURL + normalizedSubURI;

	            if (redirectUri.contains(DOUBLE_DOT)) {
	                throw new IOException("URI is not well formatted " + redirectUri.replaceAll(RestServiceBase.REPLACE_CHARS, "_"));
	            }
	            return redirectUri;
	        }
	    }
	    return null; // No matching extension
	}

	
	

}
