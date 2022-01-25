package io.antmedia.console.rest;

import java.io.IOException;
import java.security.interfaces.RSAPublicKey;

import javax.annotation.Nullable;
import javax.servlet.FilterChain;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Context;


import io.antmedia.settings.ServerSettings;

import org.springframework.context.ApplicationContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkException;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.UrlJwkProvider;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.AppSettings;
import io.antmedia.console.AdminApplication;
import io.antmedia.datastore.db.DataStore;
import io.antmedia.datastore.db.DataStoreFactory;
import io.antmedia.filter.AbstractFilter;


public class JWTServerFilter extends AbstractFilter {

	private ServerSettings serverSettings;
	public static final String JWT_TOKEN = "Authorization";
	
	@Context
	protected ServletContext servletContext;
	protected DataStoreFactory dataStoreFactory;
	private DataStore dbStore;
	protected ApplicationContext appCtx;
	//protected IScope scope;
	protected AntMediaApplicationAdapter appInstance;


	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		HttpServletRequest httpjwtRequest =(HttpServletRequest)request;
		serverSettings = getServerSetting();
		
		//TODO Find a solution for App JWT Enable case
		
		/*
		 * There are 3 cases for allowing JWT Server Filter
		 * 1- Disable JWT Server Control Status. It's disabled by default
		 * 2- If there is no JWT Token in HTTP Request. It means that request control in Authentication Filter
		 * 3- If there is a valid JWT Token header in HTTP Request
		 */
		if((serverSettings != null && !serverSettings.isJwtServerControlEnabled()) 
				|| (httpjwtRequest.getHeader(JWT_TOKEN) == null) 
				|| (httpjwtRequest.getHeader(JWT_TOKEN) != null && checkJWT(httpjwtRequest.getHeader(JWT_TOKEN)))
				){
			chain.doFilter(request, response);
		}
		else {
			HttpServletResponse resp = (HttpServletResponse) response;
			resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
		}
	}	

	private boolean checkJWT( String jwtString) {
		boolean result = true;
		try {
			String jwksURL = serverSettings.getJwksURL();

			if (jwksURL != null && !jwksURL.isEmpty()) {
				DecodedJWT jwt = JWT.decode(jwtString);
				JwkProvider provider = new UrlJwkProvider(jwksURL);
				Jwk jwk = provider.get(jwt.getKeyId());
				Algorithm algorithm = Algorithm.RSA256((RSAPublicKey) jwk.getPublicKey(), null);
				algorithm.verify(jwt);
			}
			else {
				Algorithm algorithm = Algorithm.HMAC256(serverSettings.getJwtServerSecretKey());
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
	/*
	public AntMediaApplicationAdapter getApplication() {
		if (appInstance == null) {
			ApplicationContext appContext = getAppContext();
			if (appContext != null) {
				appInstance = (AntMediaApplicationAdapter) appContext.getBean(AntMediaApplicationAdapter.BEAN_NAME);
			}
		}
		return appInstance;
	}
	
	
	@Nullable
	public ApplicationContext getAppContext() {
		if (servletContext != null) {
			appCtx = (ApplicationContext) servletContext
					.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
		}
		return appCtx;
	}
	*/
}
