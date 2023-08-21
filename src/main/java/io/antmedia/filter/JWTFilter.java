package io.antmedia.filter;


import java.io.IOException;
import java.security.interfaces.RSAPublicKey;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkException;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.UrlJwkProvider;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;

import io.antmedia.AppSettings;

public class JWTFilter extends AbstractFilter {

	protected static Logger log = LoggerFactory.getLogger(JWTFilter.class);

	public static final String JWT_TOKEN_HEADER = "Authorization";

	private AppSettings appSettings;

	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

		appSettings = getAppSettings();

		HttpServletRequest httpRequest = (HttpServletRequest) request;

		if(appSettings != null && !appSettings.isJwtControlEnabled() || (httpRequest.getHeader(JWT_TOKEN_HEADER) != null && checkJWT(httpRequest.getHeader(JWT_TOKEN_HEADER)))) {
			chain.doFilter(request, response);
			return;
		}

		((HttpServletResponse) response).sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid App JWT Token");
	}

	private boolean checkJWT(String jwtString) {
		boolean result = false;

		String jwksURL = appSettings.getJwksURL();

		if (jwksURL != null && !jwksURL.isEmpty()) {
			result = isJWKSTokenValid(appSettings.getJwksURL(), jwtString);
		}
		else {
			result = isJWTTokenValid(appSettings.getJwtSecretKey(), jwtString);
		}

		return result;
	}

	private static boolean isJWKSTokenValid(String jwksURL, String jwtString)  {

		boolean result = false;
		try {
			DecodedJWT jwt = JWT.decode(jwtString);
			JwkProvider provider = new UrlJwkProvider(jwksURL);
			Jwk jwk = provider.get(jwt.getKeyId());
			Algorithm algorithm = Algorithm.RSA256((RSAPublicKey) jwk.getPublicKey(), null);
			algorithm.verify(jwt);
			result = true;
		}
		catch (JWTVerificationException ex) {
			logger.error(ex.toString());
		} catch (JwkException e) {
			logger.error(e.toString());
		}
		return result;
	}


	public static boolean isJWTTokenValid(String jwtSecretKey, String jwtToken) {
		boolean result = false;

		try {
			Algorithm algorithm = Algorithm.HMAC256(jwtSecretKey);
			JWTVerifier verifier = JWT.require(algorithm)
					.build();
			verifier.verify(jwtToken);
			result = true;
		}
		catch (JWTVerificationException ex) {
			logger.error(ExceptionUtils.getStackTrace(ex));
		} 

		return result;
	}


}
