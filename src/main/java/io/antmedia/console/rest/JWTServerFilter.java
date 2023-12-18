package io.antmedia.console.rest;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import io.antmedia.filter.AbstractFilter;

/**
 * Just keep this class for compatibility. It'll be deleted
 * @author mekya
 *
 */
public class JWTServerFilter extends AbstractFilter {

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		chain.doFilter(request, response);
	}

}
