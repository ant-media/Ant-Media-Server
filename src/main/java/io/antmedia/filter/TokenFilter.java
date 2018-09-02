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
import org.springframework.web.context.WebApplicationContext;

import io.antmedia.AppSettings;
import io.antmedia.datastore.db.IDataStore;
import io.antmedia.datastore.db.types.Token;
import io.antmedia.statistic.HlsViewerStats;
import io.antmedia.statistic.IStreamStats;
import io.antmedia.token.TokenService;

public class TokenFilter implements javax.servlet.Filter   {

	protected static Logger logger = LoggerFactory.getLogger(TokenFilter.class);
	private FilterConfig filterConfig;
	private IDataStore dataStore;
	ApplicationContext context;

	private AppSettings settings;
	private TokenService tokenService;


	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		this.filterConfig = filterConfig;

	}




	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {

		context = (ApplicationContext) filterConfig.getServletContext().getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);


		HttpServletRequest httpRequest =(HttpServletRequest)request;
		HttpServletResponse httpResponse = (HttpServletResponse)response;

		String method = httpRequest.getMethod();
		String tokenId = ((HttpServletRequest) request).getParameter("token");

		logger.info("request url:  {} ", httpRequest.getRequestURI());
		logger.info("token:  {}", tokenId);

		if (method.equals("GET") && getAppSettings().isTokenControlEnabled()) {

			boolean result = getTokenService().checkToken(tokenId);
			if(!result) {
				httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN,"Invalid Token");
				logger.info("token {} is not valid", tokenId);
				return; 
			}
			chain.doFilter(request, response);
		}
		chain.doFilter(request, response);

	}


	public TokenService getTokenService() {
		if (tokenService == null) {
			ApplicationContext context = (ApplicationContext) filterConfig.getServletContext().getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
			tokenService = (TokenService)context.getBean(TokenService.BEAN_NAME);

		}
		return tokenService;
	}

	public AppSettings getAppSettings() {
		if (settings == null) {
			ApplicationContext context = (ApplicationContext) filterConfig.getServletContext().getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
			settings = (AppSettings)context.getBean(AppSettings.BEAN_NAME);

		}
		return settings;
	}


	@Override
	public void destroy() {
		//no need
	}



}
