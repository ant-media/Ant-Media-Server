package io.antmedia.filter;

import javax.servlet.Filter;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;

import org.springframework.context.ApplicationContext;
import org.springframework.web.context.WebApplicationContext;

import io.antmedia.AppSettings;

public abstract class AbstractFilter implements Filter{

	protected FilterConfig config;
	
	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		this.config = filterConfig;
	}
	
	public AppSettings getAppSettings() 
	{
		AppSettings appSettings = null;
		ApplicationContext context = getAppContext();
		if (context != null) {
			appSettings = (AppSettings)context.getBean(AppSettings.BEAN_NAME);
		}
		return appSettings;

	}
	
	
	public ApplicationContext getAppContext() {
		return (ApplicationContext) getConfig().getServletContext().getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
	}
	
	public FilterConfig getConfig() {
		return config;
	}
	
	public void setConfig(FilterConfig config) {
		this.config = config;
	}
	
	

	@Override
	public void destroy() {
		//nothing to destroy
	}
}
