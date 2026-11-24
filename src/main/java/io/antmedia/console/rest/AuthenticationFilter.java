package io.antmedia.console.rest;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.interfaces.RSAPublicKey;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIUtils;
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

import io.antmedia.UrlQueryUtils;
import io.antmedia.console.datastore.AbstractConsoleDataStore;
import io.antmedia.console.datastore.ConsoleDataStoreFactory;
import io.antmedia.datastore.db.IDataStoreFactory;
import io.antmedia.datastore.db.types.User;
import io.antmedia.filter.AbstractFilter;
import io.antmedia.datastore.db.types.UserType;
import io.antmedia.settings.ServerSettings;
import jakarta.ws.rs.HttpMethod;

public class AuthenticationFilter extends AbstractFilter {

	public static final String DISPATCH_PATH_URL = "_path";
	public static final String PROXY_AUTHORIZATION_HEADER_JWT_TOKEN = "ProxyAuthorization";
	public static final String FORBIDDEN_ERROR = "Not allowed to access this resource. Contact system admin";
	public static final String PANEL_AUTHENTICATED_ATTRIBUTE = "io.antmedia.console.panelAuthenticated";
	private static final Pattern BLOCKED_STATUS_PATH = Pattern.compile("^/rest/v2/users/[^/]+/blocked$");
	private static final Pattern APP_SETTINGS_PATH = Pattern.compile("/rest/v2/applications/settings/([^/]+)/?");
	private static final Pattern MATRIX_PARAMETERS = Pattern.compile(";[^/]*");

	public AbstractConsoleDataStore getAbstractConsoleDataStore()
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
		String pathInfo = httpRequest.getPathInfo();
		String path = httpRequest.getServletPath() + (pathInfo != null ? pathInfo : "");
		String method = httpRequest.getMethod();

		ServerSettings serverSettings = getServerSettings();


		
		/*
		 * JWT Filter checking below parameters:
		 * JWT Server Filter enable or not 
		 * Is Token filled or not
		 * Is JWT Server Token valid or invalid 
		 */
		
		//Pay attention
		//Header: PROXY_AUTHORIZATION_HEADER_JWT_TOKEN(ProxyAuthorization) is for accessing web panel
		//Header: JWTFilter.JWT_TOKEN_HEADER(Authorization) is for accessing the app settings
		
		//It should be different otherwise there is a problem in accessing the app rest methods through web panel 
		String jwtToken = httpRequest.getHeader(PROXY_AUTHORIZATION_HEADER_JWT_TOKEN); 
		if (serverSettings != null 
				&& serverSettings.isJwtServerControlEnabled()
				&& !StringUtils.isBlank(jwtToken)) 
		
		{
			if(checkJWT(jwtToken)) {
				httpRequest.setAttribute(PANEL_AUTHENTICATED_ATTRIBUTE, Boolean.TRUE);
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
				((HttpMethod.POST.equals(method) || HttpMethod.OPTIONS.equals(method))
						&& (path.equals("/rest/v2/users/initial") || path.equals("/rest/v2/users/authenticate"))) ||
				path.equals("/rest/v2/first-login-status") ||
				path.equals("/rest/v2/liveness") ||
				(HttpMethod.GET.equals(method) && BLOCKED_STATUS_PATH.matcher(path).matches())
				) 
		{
			chain.doFilter(request, response);
		}
		else if (CommonRestService.isAuthenticated(((HttpServletRequest)request).getSession()))
		{
			String userEmail = (String)httpRequest.getSession().getAttribute(CommonRestService.USER_EMAIL);
			AbstractConsoleDataStore store = getAbstractConsoleDataStore();
			if (store != null) 
			{
				User currentUser = store.getUser(userEmail);
				if (currentUser != null) 
				{
					if (isAuthorized(currentUser, httpRequest, path, method)) {
						httpRequest.setAttribute(PANEL_AUTHENTICATED_ATTRIBUTE, Boolean.TRUE);
						chain.doFilter(request, response);
					}
					else {
						((HttpServletResponse) response).sendError(HttpServletResponse.SC_FORBIDDEN, FORBIDDEN_ERROR);
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

	private boolean isAuthorized(User user, HttpServletRequest request, String path, String method) {
		boolean read = HttpMethod.GET.equals(method);
		if ("/rest/v2/request".equals(request.getServletPath()) || "/rest/request".equals(request.getServletPath())) {
			String targetPath = getProxyPath(request);
			if (targetPath == null) {
				return false;
			}
			int separator = targetPath.indexOf('/', 1);
			String appName = separator < 0 ? targetPath.substring(1) : targetPath.substring(1, separator);
			String appPath = separator < 0 ? "" : targetPath.substring(separator);
			UserType role = getUserRole(user, appName.isEmpty() ? CommonRestService.SCOPE_SYSTEM : appName);
			return role != null && (read || role == UserType.ADMIN
					|| role == UserType.USER && (isPathOrChild(appPath, "/rest/v2/broadcasts")
							|| isPathOrChild(appPath, "/rest/v2/vods")));
		}

		if (read && ("/rest/v2/version".equals(path) || "/rest/v2/enterprise-edition".equals(path)
				|| "/rest/v2/admin-status".equals(path))) {
			return true;
		}
		if (HttpMethod.POST.equals(method) && ("/rest/v2/users/password".equals(path)
				|| "/rest/v2/support/request".equals(path) || "/rest/v2/support/request/".equals(path))) {
			return true;
		}

		Matcher settings = APP_SETTINGS_PATH.matcher(path);
		UserType role = getUserRole(user, settings.matches() ? settings.group(1) : CommonRestService.SCOPE_SYSTEM);
		return role != null && (read || role == UserType.ADMIN);
	}

	private UserType getUserRole(User user, String appName) {
		Map<String, String> roles = user.getAppNameUserType();
		if (roles != null) {
			String role = roles.get(roles.containsKey(CommonRestService.SCOPE_SYSTEM) ? CommonRestService.SCOPE_SYSTEM : appName);
			return role == null ? null : switch (role) {
				case "ADMIN" -> UserType.ADMIN;
				case "USER" -> UserType.USER;
				case "READ_ONLY", "READ-ONLY" -> UserType.READ_ONLY;
				default -> null;
			};
		}
		// Missing maps use the legacy scope/userType fields.
		String scope = user.getScope();
		if (scope != null && !CommonRestService.SCOPE_SYSTEM.equals(scope) && !scope.equals(appName)) {
			return null;
		}
		// Old records without a role had read access in their scope. A write requires an explicit role.
		return user.getUserType() == null ? UserType.READ_ONLY : user.getUserType();
	}

	private String getProxyPath(HttpServletRequest request) {
		try {
			List<String> targets = UrlQueryUtils.getDecodedParamValues(request.getQueryString(), DISPATCH_PATH_URL);
			if (targets.size() != 1 || targets.get(0) == null) {
				return null;
			}
			String pathInfo = request.getPathInfo();
			String suffix = pathInfo == null ? "" : new URI(null, "localhost", pathInfo, null, null).getRawPath();
			URI target = new URI("http://localhost/" + targets.get(0) + suffix);
			// HttpClient rewrites repeated separators, which can also re-encode path segments.
			String rawPath = target.getRawPath();
			if (rawPath.contains("//")) {
				rawPath = URIUtils.rewriteURI(target, null, URIUtils.DROP_FRAGMENT_AND_NORMALIZE).getRawPath();
			}
			// Tomcat strips raw matrix parameters before percent decoding.
			String targetPath = new URI("http://localhost" + MATRIX_PARAMETERS.matcher(rawPath).replaceAll("")).getPath();
			StringBuilder path = new StringBuilder();
			for (String part : targetPath.split("/+")) {
				int parameter = part.indexOf(';');
				String segment = parameter < 0 ? part : part.substring(0, parameter);
				if (".".equals(segment) || "..".equals(segment) || part.indexOf('\\') >= 0
						|| part.chars().anyMatch(Character::isISOControl)) {
					return null;
				}
				if (!part.isEmpty()) {
					path.append('/').append(part);
				}
			}
			return path.isEmpty() ? "/" : path.toString();
		}
		catch (URISyntaxException | IllegalArgumentException e) {
			return null;
		}
	}

	private boolean isPathOrChild(String path, String prefix) {
		return path.equals(prefix) || path.startsWith(prefix + "/");
	}

	private boolean checkJWT( String jwtString) {
		boolean result = true;
		try {

			String jwksURL = getServerSettings().getJwksURL();

			if (jwksURL != null && !jwksURL.isEmpty()) {
				DecodedJWT jwt = JWT.decode(jwtString);
				JwkProvider provider = new UrlJwkProvider(getServerSettings().getJwksURL());
				Jwk jwk = provider.get(jwt.getKeyId());
				Algorithm algorithm = Algorithm.RSA256((RSAPublicKey) jwk.getPublicKey(), null);
				algorithm.verify(jwt);
			}
			else {
				Algorithm algorithm = Algorithm.HMAC256(getServerSettings().getJwtServerSecretKey());
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
