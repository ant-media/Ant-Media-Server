package io.antmedia.console.rest;

import java.io.IOException;
import java.security.interfaces.RSAPublicKey;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.HttpMethod;

import org.springframework.web.context.ConfigurableWebApplicationContext;

import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkException;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.UrlJwkProvider;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;

import io.antmedia.console.datastore.AbstractConsoleDataStore;
import io.antmedia.console.datastore.ConsoleDataStoreFactory;
import io.antmedia.datastore.db.IDataStoreFactory;
import io.antmedia.datastore.db.types.User;
import io.antmedia.filter.AbstractFilter;
import io.antmedia.filter.JWTFilter;
import io.antmedia.rest.model.UserType;
import io.antmedia.settings.ServerSettings;

public class AuthenticationFilter extends AbstractFilter {

	public static final String DISPATCH_PATH_URL = "_path";
	/**
	 * Use the same token header with JWT Filter which is {@link JWTFilter#JWT_TOKEN_HEADER }
	 */
	@Deprecated
	public static final String JWT_TOKEN = "ProxyAuthorization";
	public static final String FORBIDDEN_ERROR = "Not allowed to access this resource. Contact system admin";

	public AbstractConsoleDataStore getDataStore()
	{
		AbstractConsoleDataStore dataStore = null;

		ConfigurableWebApplicationContext appContext = getWebApplicationContext();
		if (appContext != null && appContext.isRunning()) 
		{
			Object dataStoreFactory = appContext.getBean(IDataStoreFactory.BEAN_NAME);

			if (dataStoreFactory instanceof ConsoleDataStoreFactory) 
			{
				AbstractConsoleDataStore dataStoreTemp = ((ConsoleDataStoreFactory)dataStoreFactory).getDataStore();
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
	


	/**
	 * Check authentication and authorization
	 * 
	 * There are 3 types of user
	 * 
	 * ADMIN can do anything in its scope.
	 *   If it's scope is system, it can CRUD anything
	 *   If it's scope is an application, it can CRUD anything in the application. 
	 *      it cannot access the web panel services
	 * 	  
	 * READ_ONLY can read anything in its scope.
	 *   If it's scope is system, it can READ anything 
	 *   If it's scope is an application, it can only READ anything in the application
	 *      it cannot access the web panel services
	 * 
	 * USER can do anything but cannot change the settings in its scope.
	 *   If it's scope is system, it can CRUD content but cannot change system settings
	 *   If it's scope is an application, it can CRUD content but cannot change system settings/server settings, 
	 *   	cannot add/remove users or applications
	 *   
	 *   
	 * 
	 * Scope:
	 * - System
	 * - Specific Application
	 */
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {

		HttpServletRequest httpRequest = (HttpServletRequest) request;
		String path = ((HttpServletRequest) request).getRequestURI();

		ServerSettings serverSettings = getServerSetting();
		
		/*
		 * JWT Filter checking below parameters:
		 * JWT Server Filter enable or not 
		 * Is Token filled or not
		 * Is JWT Server Token valid or invalid 
		 */
		String jwtToken = httpRequest.getHeader(JWTFilter.JWT_TOKEN_HEADER);
		if (jwtToken == null) {
			//get it for compatibility
			jwtToken = httpRequest.getHeader(JWT_TOKEN);
		}
		if (serverSettings != null 
				&& serverSettings.isJwtServerControlEnabled()
				&& jwtToken != null) 
		
		{
			if(checkJWT(jwtToken)) {
				chain.doFilter(request, response);
			}
			else {
				((HttpServletResponse) response).sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid Server JWT Token");
			}
		}
		else if (path.equals("/rest/isAuthenticated") ||
				path.equals("/rest/authenticateUser") || 
				path.equals("/rest/addInitialUser") ||
				path.equals("/rest/isFirstLogin") ||

				path.equals("/rest/v2/authentication-status") ||
				path.equals("/rest/v2/users/initial") ||
				path.equals("/rest/v2/first-login-status") ||
				path.equals("/rest/v2/users/authenticate") ||
				path.equals("/rest/v2/liveness") ||
				(path.startsWith("/rest/v2/users/") && path.endsWith("/blocked"))
				) 
		{
			chain.doFilter(request, response);
		}
		else if (CommonRestService.isAuthenticated(((HttpServletRequest)request).getSession()))
		{
			String method = httpRequest.getMethod();

			String userEmail = (String)httpRequest.getSession().getAttribute(CommonRestService.USER_EMAIL);
			AbstractConsoleDataStore store = getDataStore();
			if (store != null) 
			{
				User currentUser = store.getUser(userEmail);
				if (currentUser != null) 
				{
					String userScope = currentUser.getScope();
					String dispatchURL = httpRequest.getParameter(DISPATCH_PATH_URL);
					boolean scopeAccess =  scopeAccessGranted(userScope, dispatchURL);
					
					if (HttpMethod.GET.equals(method))  
					{
						//This is the READ part. No need to check the user type because scope is critical
						if (scopeAccess || path.equals("/rest/v2/applications/settings/" + userScope)
								|| path.equals("/rest/v2/version") || path.equals("/rest/v2/enterprise-edition")
								|| path.equals("/rest/v2/admin-status")) 
						{
							chain.doFilter(request, response);
						}
						else 
						{
							((HttpServletResponse) response).sendError(HttpServletResponse.SC_FORBIDDEN, FORBIDDEN_ERROR);
						}

					}
					else
					{
						//if it's not GET method, it should be PUT, DELETE or POST, check the authorization
						if (path.equals("/rest/v2/users/password") || path.startsWith("/rest/v2/support/request")) 
						{
							//changing own password and sending support are allowed for all users
							chain.doFilter(request, response);
						}
						else if (scopeAccess) 
						{
							//if it's an admin, provide access - backward compatible
							if (UserType.ADMIN.equals(currentUser.getUserType()) || currentUser.getUserType() == null) 
							{
								chain.doFilter(request, response);
							}
							//user scope already checked on scopeAccessGranted. No need to check it again
							else if (UserType.USER.equals(currentUser.getUserType()) && 
									(dispatchURL != null && (dispatchURL.contains("/rest/v2/broadcasts") || dispatchURL.contains("/rest/v2/vods") )))
							{
								//if user scope is system and granted, it cannot change anythings in the system scope server-settings, add/delete apps and users
								//if user scope is application and granted, it can do anything in this scope
								chain.doFilter(request, response);
							}
							else {
								((HttpServletResponse)response).sendError(HttpServletResponse.SC_FORBIDDEN, FORBIDDEN_ERROR);
							}
						}
						else {
							if (UserType.ADMIN.equals(currentUser.getUserType()) && 
									(path.startsWith("/rest/v2/applications/settings/" + userScope) || (path.startsWith(userScope) || path.startsWith(userScope, 1)))) 
							{
								//only admin user can access to change the application settings out of its scope
								chain.doFilter(request, response);
							}
							else {
								((HttpServletResponse)response).sendError(HttpServletResponse.SC_FORBIDDEN, FORBIDDEN_ERROR);
							}
						}
					}
				}
				else {
					((HttpServletResponse) response).sendError(HttpServletResponse.SC_FORBIDDEN, "No user in this session");
				}
			}
			else {
				((HttpServletResponse) response).sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database is not available. Please try again");
			}

		}
		else {
			((HttpServletResponse) response).sendError(HttpServletResponse.SC_FORBIDDEN, "Not authenticated user");
		}

	}

	private boolean scopeAccessGranted(String userScope, String dispatchUrl){

		boolean granted = false;
		if (userScope == null || userScope.equals(CommonRestService.SCOPE_SYSTEM)) 
		{
			//Allow system level access 
			granted = true;
		}
		else 
		{
			//Allow application level access
			if ((dispatchUrl != null && (dispatchUrl.startsWith(userScope) || dispatchUrl.startsWith(userScope, 1)))) 
			{
				//second dispatch url is if the url starts with "/"
				granted = true;
			}

		}
		return granted;
	}
	

	private boolean checkJWT( String jwtString) {
		boolean result = true;
		try {

			String jwksURL = getServerSetting().getJwksURL();

			if (jwksURL != null && !jwksURL.isEmpty()) {
				DecodedJWT jwt = JWT.decode(jwtString);
				JwkProvider provider = new UrlJwkProvider(getServerSetting().getJwksURL());
				Jwk jwk = provider.get(jwt.getKeyId());
				Algorithm algorithm = Algorithm.RSA256((RSAPublicKey) jwk.getPublicKey(), null);
				algorithm.verify(jwt);
			}
			else {
				Algorithm algorithm = Algorithm.HMAC256(getServerSetting().getJwtServerSecretKey());
				JWTVerifier verifier = JWT.require(algorithm)
						.build();
				verifier.verify(jwtString);
			}

		}
		catch (JWTVerificationException ex) {
			logger.error(ex.toString());
			result = false;
		} catch (JwkException e) {
			logger.error(e.toString());
			result = false;
		}
		return result;
	}
	
}
