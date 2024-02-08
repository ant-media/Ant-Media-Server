package io.antmedia.filter;


import java.io.IOException;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

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
		if(appSettings == null){
			((HttpServletResponse) response).sendError(HttpServletResponse.SC_FORBIDDEN, "Application is getting initialized");
			return;
		}
		if(!appSettings.isJwtControlEnabled() || (httpRequest.getHeader(JWT_TOKEN_HEADER) != null && checkJWT(httpRequest.getHeader(JWT_TOKEN_HEADER)))) {
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
			logger.error("JWT token is not valid for a jwtToken");

		} 

		return result;
	}
	
	
	
	public static boolean isJWTTokenValid(String jwtSecretKey, String jwtToken, String issuer) {
		boolean result = false;

		try {
			Algorithm algorithm = Algorithm.HMAC256(jwtSecretKey);
			JWTVerifier verifier = JWT.require(algorithm)
					.withIssuer(issuer)
					.build();
			verifier.verify(jwtToken);
			result = true;
		}
		catch (JWTVerificationException ex) {
			logger.error("JWT token is not valid for issuer name: {}", issuer);

		} 

		return result;
	}
	
	/**
	 * This method checks the claim value in the JWT token. For instance, we just need to give claimName as `subscriberId` and claimValue as the subscribers' id such as email
	 * 
	 *Typical usage is like that 
	 *isJWTTokenValid(jwtSecretKey, jwtToken, "subscriberId", "myemail@example.com");
	 *
	 * @param jwtSecretKey
	 * @param jwtToken
	 * @param claimName
	 * @param claimValue
	 * @return
	 */
	public static boolean isJWTTokenValid(String jwtSecretKey, String jwtToken, String claimName, String claimValue) {
		boolean result = false;

		try {
			Algorithm algorithm = Algorithm.HMAC256(jwtSecretKey);
			JWTVerifier verifier = JWT.require(algorithm)
					.withClaim(claimName, claimValue)
					.build();
			verifier.verify(jwtToken);
			result = true;
		}
		catch (JWTVerificationException ex) {
			logger.error("JWT token is not valid for claim name: {}", claimName);
		} 

		return result;
	}
	
	public static String generateJwtToken(String jwtSecretKey, long expireDateUnixTimeStampMs, String claimName, String claimValue) {
		Date expireDateType = new Date(expireDateUnixTimeStampMs);
		String jwtTokenId = null;
		try {
			Algorithm algorithm = Algorithm.HMAC256(jwtSecretKey);

			jwtTokenId = JWT.create().
					withExpiresAt(expireDateType).
					withClaim(claimName, claimValue).
					sign(algorithm);

		} catch (Exception e) {
			logger.error(ExceptionUtils.getStackTrace(e));
		}

		return jwtTokenId;
	}
	
	
	public static String generateJwtToken(String jwtSecretKey, long expireDateUnixTimeStampMs) {
		return generateJwtToken(jwtSecretKey, expireDateUnixTimeStampMs, "");
	}
	
	public static String generateJwtToken(String jwtSecretKey, long expireDateUnixTimeStampMs, String issuer) {
		Date expireDateType = new Date(expireDateUnixTimeStampMs);
		String jwtTokenId = null;
		try {
			Algorithm algorithm = Algorithm.HMAC256(jwtSecretKey);

			jwtTokenId = JWT.create().
					withExpiresAt(expireDateType).
					withIssuer(issuer).
					sign(algorithm);

		} catch (Exception e) {
			logger.error(ExceptionUtils.getStackTrace(e));
		}

		return jwtTokenId;
	}


}
