package io.antmedia.console.rest;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.HttpMethod;

import org.springframework.web.context.ConfigurableWebApplicationContext;

import io.antmedia.console.datastore.ConsoleDataStoreFactory;
import io.antmedia.console.datastore.IConsoleDataStore;
import io.antmedia.datastore.db.IDataStoreFactory;
import io.antmedia.filter.AbstractFilter;
import io.antmedia.rest.model.User;
import io.antmedia.rest.model.UserType;

public class AuthenticationFilter extends AbstractFilter {


	private IConsoleDataStore getDataStore() 
	{
		IConsoleDataStore dataStore = null;

		ConfigurableWebApplicationContext appContext = getWebApplicationContext();
		if (appContext != null && appContext.isRunning()) 
		{
			Object dataStoreFactory = appContext.getBean(IDataStoreFactory.BEAN_NAME);

			if (dataStoreFactory instanceof ConsoleDataStoreFactory) 
			{
				IConsoleDataStore dataStoreTemp = ((ConsoleDataStoreFactory)dataStoreFactory).getDataStore();
				if (dataStoreTemp.isAvailable()) 
				{
					dataStore = dataStoreTemp;
				}
				else {
					logger.warn("DataStore is not available. It may be closed or not initialized");
				}
			}	
		}
		return dataStore;
	}


	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {

		String path = ((HttpServletRequest) request).getRequestURI();
		
		if (path.equals("/rest/isAuthenticated") ||
				path.equals("/rest/authenticateUser") || 
				path.equals("/rest/addInitialUser") ||
				path.equals("/rest/isFirstLogin") ||

				path.equals("/rest/v2/authentication-status") ||
				path.equals("/rest/v2/users/initial") ||
				path.equals("/rest/v2/first-login-status") ||
				path.equals("/rest/v2/users/authenticate") ) 
		{
			chain.doFilter(request, response);
		}
		else if (CommonRestService.isAuthenticated(((HttpServletRequest)request).getSession()))
		{
			HttpServletRequest httpRequest =(HttpServletRequest)request;
			String method = httpRequest.getMethod();
			
			if (HttpMethod.GET.equals(method))  
			{
				chain.doFilter(request, response);
			}
			else
			{
				//if it's not GET method, it should be PUT, DELETE or POST, check if user is admin
				IConsoleDataStore store = getDataStore();
				if (store != null) 
				{
					String userEmail = (String)httpRequest.getSession().getAttribute(CommonRestService.USER_EMAIL);
					User currentUser = store.getUser(userEmail);
					if(currentUser != null && UserType.ADMIN.equals(currentUser.getUserType())) 
					{
						chain.doFilter(request, response);
					}
					else 
					{
						logger.warn("User is  null or not admin. User e-mail:{} and user type:{}", userEmail, currentUser != null ? currentUser.getUserType() : null );
						HttpServletResponse resp = (HttpServletResponse) response;
						resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
					}
				}
				else 
				{
					HttpServletResponse resp = (HttpServletResponse) response;
					resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
				}
			}
			
		}
		else {
			HttpServletResponse resp = (HttpServletResponse) response;
			resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
		}
		

	}

	@Override
	public void destroy() {


	}

}
