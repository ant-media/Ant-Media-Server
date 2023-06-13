package io.antmedia.filter;


import java.io.IOException;
import java.security.interfaces.RSAPublicKey;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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

	private boolean checkJWT( String jwtString) {
		boolean result = true;
		try {

			String jwksURL = appSettings.getJwksURL();

			if (jwksURL != null && !jwksURL.isEmpty()) {
				DecodedJWT jwt = JWT.decode(jwtString);
				JwkProvider provider = new UrlJwkProvider(appSettings.getJwksURL());
				Jwk jwk = provider.get(jwt.getKeyId());
				Algorithm algorithm = Algorithm.RSA256((RSAPublicKey) jwk.getPublicKey(), null);
				algorithm.verify(jwt);
			}
			else {
				Algorithm algorithm = Algorithm.HMAC256(appSettings.getJwtSecretKey());
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
