package io.antmedia.filter;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

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
