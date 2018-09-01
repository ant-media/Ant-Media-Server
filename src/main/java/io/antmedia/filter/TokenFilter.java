package io.antmedia.filter;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import io.antmedia.AppSettings;
import io.antmedia.datastore.db.IDataStore;
import io.antmedia.datastore.db.types.Token;
import io.antmedia.statistic.IStreamStats;

public class TokenFilter implements javax.servlet.Filter , ApplicationContextAware {

	protected static Logger logger = LoggerFactory.getLogger(TokenFilter.class);
	private FilterConfig filterConfig;
	private IDataStore dataStore;
	private AppSettings settings;
	public static final String BEAN_NAME = "token.filter";

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		dataStore = (IDataStore) applicationContext.getBean(IDataStore.BEAN_NAME);
		if (applicationContext.containsBean(AppSettings.BEAN_NAME)) {
			settings = (AppSettings)applicationContext.getBean(AppSettings.BEAN_NAME);

		}

	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		this.filterConfig = filterConfig;

	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {

		HttpServletRequest httpRequest =(HttpServletRequest)request;
        HttpServletResponse httpResponse = (HttpServletResponse)response;

		String method = httpRequest.getMethod();
		if (method.equals("GET") && settings.isTokenControlEnabled()) {
			
			Token token = new Token();
			token.setTokenId(getTokenId(httpRequest.getRequestURI()));
			
			if(dataStore.validateToken(token) == null) {
			httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN,"Invalid Token");
			
			return;
			}
		}
	
		chain.doFilter(request, response);

	}
	
	public String getTokenId(String requestURI) {
		
		return requestURI.substring(requestURI.lastIndexOf('='), requestURI.length());
		
	}

	@Override
	public void destroy() {
		//no need
	}

}
