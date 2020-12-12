package io.antmedia.test.filter;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.HashSet;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.filters.CorsFilter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import io.antmedia.filter.CorsHeaderFilter;

public class CorsHeaderFilterTest {
	
	private CorsHeaderFilter corsFilter;

	@Before
	public void before() {
		corsFilter = new CorsHeaderFilter();
	}
	
	@After
	public void after() {
		corsFilter = null;
	}
	
	@Test
	public void testhandleSimpleCORS() {
		CorsHeaderFilter corsFilterSpy = Mockito.spy(corsFilter);
		
		Mockito.doReturn(true).when(corsFilterSpy).isAnyOriginAllowed();
		
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		HttpServletResponse response =  Mockito.mock(HttpServletResponse.class);
		FilterChain filterChain =  Mockito.mock(FilterChain.class);
		
		{	
			// Allow this request for angular app development
			Mockito.when(request.getHeader(CorsFilter.REQUEST_HEADER_ORIGIN)).thenReturn("http://localhost:4200");
			corsFilterSpy.addStandardHeadersInternal(request, response, "http://localhost:4200");
			Mockito.verify(response).addHeader(CorsFilter.RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:4200");
		}
		{
			request = Mockito.mock(HttpServletRequest.class);
			response =  Mockito.mock(HttpServletResponse.class);
			filterChain =  Mockito.mock(FilterChain.class);
			
			// request.getHeader("Authorization") have right header, so allow this request
			Mockito.when(request.getHeader("Authorization")).thenReturn("JwtToken");
			Mockito.when(request.getHeader(CorsFilter.REQUEST_HEADER_ORIGIN)).thenReturn("http://11.22.33.44:5080");
			corsFilterSpy.addStandardHeadersInternal(request, response, "http://11.22.33.44:5080");
			Mockito.verify(response).addHeader(CorsFilter.RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_ORIGIN, "http://11.22.33.44:5080");
		}
		{
			request = Mockito.mock(HttpServletRequest.class);
			response =  Mockito.mock(HttpServletResponse.class);
			filterChain =  Mockito.mock(FilterChain.class);
			
			//	REQUEST_HEADER_ACCESS_CONTROL_REQUEST_HEADERS have wrong header, so deny this request
			Mockito.when(request.getHeader(CorsFilter.REQUEST_HEADER_ACCESS_CONTROL_REQUEST_HEADERS)).thenReturn("otherHeader");
			Mockito.when(request.getHeader(CorsFilter.REQUEST_HEADER_ORIGIN)).thenReturn("http://55.66.77.88:5080");
			corsFilterSpy.addStandardHeadersInternal(request, response, "http://55.66.77.88:5080");
			Mockito.verify(response).addHeader(CorsFilter.RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_ORIGIN, "*");
		}
		{
			request = Mockito.mock(HttpServletRequest.class);
			response =  Mockito.mock(HttpServletResponse.class);
			filterChain =  Mockito.mock(FilterChain.class);
			
			//	REQUEST_HEADER_ACCESS_CONTROL_REQUEST_HEADERS have right header, so allow this request
			Mockito.when(request.getHeader(CorsFilter.REQUEST_HEADER_ACCESS_CONTROL_REQUEST_HEADERS)).thenReturn("authorization");
			Mockito.when(request.getHeader(CorsFilter.REQUEST_HEADER_ORIGIN)).thenReturn("http://55.66.77.88:5080");
			corsFilterSpy.addStandardHeadersInternal(request, response, "http://55.66.77.88:5080");
			Mockito.verify(response).addHeader(CorsFilter.RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_ORIGIN, "http://55.66.77.88:5080");
		}
		{
			request = Mockito.mock(HttpServletRequest.class);
			response =  Mockito.mock(HttpServletResponse.class);
			filterChain =  Mockito.mock(FilterChain.class);
			
			// REQUEST_HEADER_ACCESS_CONTROL_REQUEST_HEADERS have null header, so deny this request
			Mockito.when(request.getHeader(CorsFilter.REQUEST_HEADER_ACCESS_CONTROL_REQUEST_HEADERS)).thenReturn(null);
			Mockito.when(request.getHeader(CorsFilter.REQUEST_HEADER_ORIGIN)).thenReturn("http://55.66.77.88:5080");
			corsFilterSpy.addStandardHeadersInternal(request, response, "http://55.66.77.88:5080");
			Mockito.verify(response).addHeader(CorsFilter.RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_ORIGIN, "*");
		}
		
		//below codes for increasing coverage
		Mockito.when(request.getMethod()).thenReturn("OPTIONS");
		HashSet httpMethods = new HashSet<>();
		httpMethods.add("POST");
		Mockito.doReturn(httpMethods).when(corsFilterSpy).getAllowedHttpMethods();
		
		Mockito.doReturn(httpMethods).when(corsFilterSpy).getAllowedHttpHeaders();
		Mockito.doReturn(httpMethods).when(corsFilterSpy).getExposedHeaders();
		Mockito.doReturn(false).when(corsFilterSpy).isAnyOriginAllowed();
		
		Mockito.doReturn(100L).when(corsFilterSpy).getPreflightMaxAge();
		
		corsFilterSpy.addStandardHeadersInternal(request, response, "http://localhost:4200");
		
		try {
			Mockito.doReturn(true).when(corsFilterSpy).isAnyOriginAllowed();
			corsFilterSpy.handleSimpleCORS(request, response, filterChain);
					
			
			Mockito.doReturn(false).when(corsFilterSpy).isAnyOriginAllowed();
			corsFilterSpy.handleSimpleCORS(request, response, filterChain);
			
			
			Mockito.when(request.getHeader(CorsFilter.REQUEST_HEADER_ACCESS_CONTROL_REQUEST_METHOD)).thenReturn("any");
			corsFilterSpy.handlePreflightCORS(request, response, filterChain);
			
			Mockito.doReturn(true).when(corsFilterSpy).isAnyOriginAllowed();
			corsFilterSpy.handlePreflightCORS(request, response, filterChain);
			
			httpMethods.add("any");
			Mockito.doReturn(httpMethods).when(corsFilterSpy).getAllowedHttpHeaders();
			corsFilterSpy.handlePreflightCORS(request, response, filterChain);
			
			
		} catch (IOException | ServletException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		
		
		
		
	}

}
