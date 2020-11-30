package io.antmedia.filter;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.filters.CorsFilter;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.tomcat.util.http.ResponseUtil;
import org.apache.tomcat.util.res.StringManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This filter is implemented in order to make easy to develop angular app
 * @author mekya
 *
 */
public class CorsHeaderFilter extends CorsFilter {

	protected static Logger logger = LoggerFactory.getLogger(CorsHeaderFilter.class);
	
	
	private static final StringManager sm = StringManager.getManager(CorsFilter.class);

	@Override
	public void handleSimpleCORS(final HttpServletRequest request,
			final HttpServletResponse response, final FilterChain filterChain)
					throws IOException, ServletException {

		CorsFilter.CORSRequestType requestType = checkRequestType(request);
		if (!(requestType == CorsFilter.CORSRequestType.SIMPLE ||
				requestType == CorsFilter.CORSRequestType.ACTUAL)) {
			throw new IllegalArgumentException(
					sm.getString("corsFilter.wrongType2",
							CorsFilter.CORSRequestType.SIMPLE,
							CorsFilter.CORSRequestType.ACTUAL));
		}

		final String origin = request.getHeader(CorsFilter.REQUEST_HEADER_ORIGIN);
		final String method = request.getMethod();

		// Section 6.1.2
		if (!isOriginAllowedInternal(origin)) {
			handleInvalidCORSInternal(request, response, filterChain);
			return;
		}

		if (!getAllowedHttpMethods().contains(method)) {
			handleInvalidCORSInternal(request, response, filterChain);
			return;
		}

		addStandardHeadersInternal(request, response, origin);

		// Forward the request down the filter chain.
		filterChain.doFilter(request, response);
	}

	public void addStandardHeadersInternal(final HttpServletRequest request,
			final HttpServletResponse response, String origin) {

		final String method = request.getMethod();

		if (!isAnyOriginAllowed()) {
			// If only specific origins are allowed, the response will vary by
			// origin
			ResponseUtil.addVaryFieldName(response, CorsFilter.REQUEST_HEADER_ORIGIN);
		}

		// CORS requests (SIMPLE, ACTUAL, PRE_FLIGHT) set the following headers
		// although non-CORS requests do not need to. The headers are always set
		// as a) they do no harm in the non-CORS case and b) it allows the same
		// response to be cached for CORS and non-CORS requests.

		// Add a single Access-Control-Allow-Origin header.
		if (isAnyOriginAllowed() && !internalOriginCheck(origin,request))  //this simplifies angular app development and JWT Filter requests
		{
			// If any origin is allowed, return header with '*'.
			response.addHeader(CorsFilter.RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_ORIGIN, "*");
		} else {
			// Add a single Access-Control-Allow-Origin header, with the value
			// of the Origin header as value.
			
				try {
					if (!(origin.matches("^http.*") || origin.matches("^ws.*"))) {
						throw new IOException("origin does not start http or ws. It is " + origin);
					}
					response.addHeader(CorsFilter.RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_ORIGIN, origin);
				} catch (IOException e) {
					logger.error(ExceptionUtils.getStackTrace(e));
				}
			

		}

		// If the resource supports credentials, add a single
		// Access-Control-Allow-Credentials header with the case-sensitive
		// string "true" as value.
		if (isSupportsCredentials() || internalOriginCheck(origin,request)) {
			response.addHeader(CorsFilter.RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
		}

		// If the list of exposed headers is not empty add one or more
		// Access-Control-Expose-Headers headers, with as values the header
		// field names given in the list of exposed headers.

		if ((getExposedHeaders() != null) && (!getExposedHeaders().isEmpty())) {
			String exposedHeadersString = join(getExposedHeaders(), ",");
			response.addHeader(CorsFilter.RESPONSE_HEADER_ACCESS_CONTROL_EXPOSE_HEADERS,
					exposedHeadersString);
		}

		if ("OPTIONS".equals(method)) {
			// For an OPTIONS request, the response will vary based on the
			// value or absence of the following headers. Hence they need be be
			// included in the Vary header.
			ResponseUtil.addVaryFieldName(
					response, CorsFilter.REQUEST_HEADER_ACCESS_CONTROL_REQUEST_METHOD);
			ResponseUtil.addVaryFieldName(
					response, CorsFilter.REQUEST_HEADER_ACCESS_CONTROL_REQUEST_HEADERS);

			// CORS PRE_FLIGHT (OPTIONS) requests set the following headers although
			// non-CORS OPTIONS requests do not need to. The headers are always set
			// as a) they do no harm in the non-CORS case and b) it allows the same
			// response to be cached for CORS and non-CORS requests.

			if (getPreflightMaxAge() > 0) {
				response.addHeader(
						CorsFilter.RESPONSE_HEADER_ACCESS_CONTROL_MAX_AGE,
						String.valueOf(getPreflightMaxAge()));
			}

			if  ((getAllowedHttpMethods() != null) && (!getAllowedHttpMethods().isEmpty())) {
				response.addHeader(
						CorsFilter.RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_METHODS,
						join(getAllowedHttpMethods(), ","));
			}

			if ((getAllowedHttpHeaders() != null) && (!getAllowedHttpHeaders().isEmpty())) {
				response.addHeader(
						CorsFilter.RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_HEADERS,
						join(getAllowedHttpHeaders(), ","));
			}
		}
	}
	
	private boolean internalOriginCheck(String origin, HttpServletRequest request) {
		// localhost:4200 -> This simplifies angular app development
		// request.getHeader("Authorization") != null -> GET, POST and etc requests with JWT Control
		// (request.getHeader(REQUEST_HEADER_ACCESS_CONTROL_REQUEST_HEADERS) != null && request.getHeader(REQUEST_HEADER_ACCESS_CONTROL_REQUEST_HEADERS).contains("authorization") -> For the option request
		
		return origin.equals("http://localhost:4200") || request.getHeader("Authorization") != null || (request.getHeader(REQUEST_HEADER_ACCESS_CONTROL_REQUEST_HEADERS) != null && request.getHeader(REQUEST_HEADER_ACCESS_CONTROL_REQUEST_HEADERS).contains("authorization"));
	}

	private boolean isOriginAllowedInternal(final String origin) {
		if (isAnyOriginAllowed()) {
			return true;
		}

		// If 'Origin' header is a case-sensitive match of any of allowed
		// origins, then return true, else return false.
		return getAllowedOrigins().contains(origin);
	}

	/**
	 * Handles a CORS request that violates specification.
	 *
	 * @param request The {@link HttpServletRequest} object.
	 * @param response The {@link HttpServletResponse} object.
	 * @param filterChain The {@link FilterChain} object.
	 */
	private void handleInvalidCORSInternal(final HttpServletRequest request,
			final HttpServletResponse response, final FilterChain filterChain) {

		response.setContentType("text/plain");
		response.setStatus(HttpServletResponse.SC_FORBIDDEN);
		response.resetBuffer();
	}


	@Override
	public void handlePreflightCORS(final HttpServletRequest request,
			final HttpServletResponse response, final FilterChain filterChain)
					throws IOException, ServletException {

		CORSRequestType requestType = checkRequestType(request);
		if (requestType != CORSRequestType.PRE_FLIGHT) {
			throw new IllegalArgumentException(sm.getString("corsFilter.wrongType1",
					CORSRequestType.PRE_FLIGHT.name().toLowerCase(Locale.ENGLISH)));
		}

		final String origin = request.getHeader(CorsFilter.REQUEST_HEADER_ORIGIN);

		// Section 6.2.2
		if (!isOriginAllowedInternal(origin)) {
			handleInvalidCORSInternal(request, response, filterChain);
			return;
		}

		// Section 6.2.3
		String accessControlRequestMethod = request.getHeader(
				CorsFilter.REQUEST_HEADER_ACCESS_CONTROL_REQUEST_METHOD);
		if (accessControlRequestMethod == null) {
			handleInvalidCORSInternal(request, response, filterChain);
			return;
		} else {
			accessControlRequestMethod = accessControlRequestMethod.trim();
		}

		// Section 6.2.4
		String accessControlRequestHeadersHeader = request.getHeader(
				CorsFilter.REQUEST_HEADER_ACCESS_CONTROL_REQUEST_HEADERS);
		List<String> accessControlRequestHeaders = new LinkedList<>();
		if (accessControlRequestHeadersHeader != null &&
				!accessControlRequestHeadersHeader.trim().isEmpty()) {
			String[] headers = accessControlRequestHeadersHeader.trim().split(",");
			for (String header : headers) {
				accessControlRequestHeaders.add(header.trim().toLowerCase(Locale.ENGLISH));
			}
		}

		// Section 6.2.5
		if (!getAllowedHttpMethods().contains(accessControlRequestMethod)) {
			handleInvalidCORSInternal(request, response, filterChain);
			return;
		}

		// Section 6.2.6
		if (!accessControlRequestHeaders.isEmpty()) {
			for (String header : accessControlRequestHeaders) {
				if (!getAllowedHttpHeaders().contains(header)) {
					handleInvalidCORSInternal(request, response, filterChain);
					return;
				}
			}
		}

		addStandardHeadersInternal(request, response, origin);

		// Do not forward the request down the filter chain.
	}


}
