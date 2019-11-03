package io.antmedia.filter;

import javax.servlet.Filter;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.WebApplicationContext;

import io.antmedia.AppSettings;

public abstract class AbstractFilter implements Filter{

	protected static Logger logger = LoggerFactory.getLogger(AbstractFilter.class);
	protected FilterConfig config;
	
	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		this.config = filterConfig;
	}
	
	public AppSettings getAppSettings() 
	{
		AppSettings appSettings = null;
		ConfigurableWebApplicationContext context = getAppContext();
		if (context != null) {
			appSettings = (AppSettings)context.getBean(AppSettings.BEAN_NAME);
		}
		return appSettings;
	}
	
	
	public ConfigurableWebApplicationContext getAppContext() {
		ConfigurableWebApplicationContext appContext = (ConfigurableWebApplicationContext) getConfig().getServletContext().getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
		if (appContext != null && appContext.isRunning()) {
			return appContext;
		}
		else {
			if (appContext == null) {
				logger.warn("App context not initialized ");
			}
			else {
				logger.warn("App context not running yet." );
			}
		}
		
		return null;
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
