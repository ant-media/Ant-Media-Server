package io.antmedia.console.rest;

import java.io.IOException;
import java.security.interfaces.RSAPublicKey;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import io.antmedia.settings.ServerSettings;


import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkException;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.UrlJwkProvider;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import io.antmedia.filter.AbstractFilter;


public class JWTServerFilter extends AbstractFilter {

	private ServerSettings serverSettings;
	public static final String JWT_TOKEN = "Authorization";


	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		HttpServletRequest httpjwtRequest =(HttpServletRequest)request;
		serverSettings = getServerSetting();

		if((serverSettings != null && !serverSettings.isJwtServerControlEnabled()) || (httpjwtRequest.getHeader(JWT_TOKEN) == null) || (httpjwtRequest.getHeader(JWT_TOKEN) != null && checkJWT(httpjwtRequest.getHeader(JWT_TOKEN)))) {
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
}
