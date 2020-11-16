package io.antmedia.filter;


import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.crypto.SecretKey;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.antmedia.AppSettings;

public class JWTFilter extends AbstractFilter {
	
	protected static Logger log = LoggerFactory.getLogger(JWTFilter.class);
	
	public static final String JWT_TOKEN = "jwtToken";
	
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		
		AppSettings appSettings = getAppSettings();

		if(!appSettings.isJwtControlEnabled()) {
			return;
		}
		
		HttpServletRequest httpRequest = (HttpServletRequest) request;
		
		
		if(httpRequest.getHeader(JWT_TOKEN) == null) {
			// Check IP Filter
			chain.doFilter(request, response);
			return;
		}
		
		if(httpRequest.getHeader(JWT_TOKEN) != null && checkJWT(httpRequest.getHeader(JWT_TOKEN))) {
			// No need to check IP Filter
			return;
		}

		((HttpServletResponse) response).sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid JWT Token");
	}
	
	private boolean checkJWT(String jwtString) {
		
		AppSettings appSettings = getAppSettings();
		SecretKey key = Keys.hmacShaKeyFor(appSettings.getJwtSecretKey().getBytes(StandardCharsets.UTF_8));
		boolean result = false;

		try {
			Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(jwtString); 
		    result = true;
		}
		catch (JwtException ex) {
			result = false;
		}
		return result;
	}
}
