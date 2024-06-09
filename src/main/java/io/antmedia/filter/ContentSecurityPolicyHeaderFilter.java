package io.antmedia.filter;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;

import io.antmedia.AppSettings;

public class ContentSecurityPolicyHeaderFilter extends AbstractFilter 
{

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		
		AppSettings appSettings = getAppSettings();
		
		if (appSettings != null) 
		{
			String headerValue = appSettings.getContentSecurityPolicyHeaderValue();
			if (headerValue != null && !headerValue.isEmpty()) 
			{
				HttpServletResponse httpResponse = (HttpServletResponse) response;
		        httpResponse.setHeader("Content-Security-Policy", headerValue);
			}
		}
		
		chain.doFilter(request, response);

	}

}
