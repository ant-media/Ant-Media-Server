package io.antmedia.test.filter;

import static org.junit.Assert.fail;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;

import io.antmedia.AppSettings;
import io.antmedia.filter.HttpForwardFilter;

public class HttpForwardFilterTest {

	 	@Test
	    public void testDoFilterPass() throws IOException, ServletException
	    {
	 		 HttpForwardFilter httpForwardFilter = Mockito.spy(new HttpForwardFilter());

	         MockHttpServletRequest httpServletRequest = new MockHttpServletRequest();
	         httpServletRequest.setRemoteAddr("192.168.0.1");
	         httpServletRequest.setRequestURI("/LiveApp/streams/test.m3u8");
	         HttpServletResponse httpServletResponse = Mockito.mock(HttpServletResponse.class);
	         FilterChain filterChain = Mockito.mock(FilterChain.class);
	         AppSettings appSettings = new AppSettings();
	         
	         Mockito.doReturn(null).when(httpForwardFilter).getAppSettings();
	         httpForwardFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);
	         Mockito.verify(filterChain, Mockito.times(1)).doFilter(httpServletRequest, httpServletResponse);
	         
	         
	         Mockito.doReturn(appSettings).when(httpForwardFilter).getAppSettings();
	         httpForwardFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);
	         Mockito.verify(filterChain, Mockito.times(2)).doFilter(httpServletRequest, httpServletResponse);
	         
	         //set extension
	         appSettings.setHttpForwardingExtension("m3u8");
	         httpForwardFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);
	         //chain filter should be called because no base url
	         Mockito.verify(filterChain, Mockito.times(3)).doFilter(httpServletRequest, httpServletResponse);
	         
	         
	         //set extension empty
	         appSettings.setHttpForwardingExtension("");
	         httpForwardFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);
	         //chain filter should be called because no base url
	         Mockito.verify(filterChain, Mockito.times(4)).doFilter(httpServletRequest, httpServletResponse);
	        
	         appSettings.setHttpForwardingExtension("m3u8");
	         appSettings.setHttpForwardingBaseURL("");
	         httpForwardFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);
	         //chain filter should  be called because   url is empty
	         Mockito.verify(filterChain, Mockito.times(5)).doFilter(httpServletRequest, httpServletResponse);
	         
	         appSettings.setHttpForwardingExtension("m3u8");
	         appSettings.setHttpForwardingBaseURL("http://url/");
	         httpForwardFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);
	         //chain filter should not be called because  base url is et
	         Mockito.verify(filterChain, Mockito.times(5)).doFilter(httpServletRequest, httpServletResponse);
	         Mockito.verify(httpServletResponse).sendRedirect("http://url/streams/test.m3u8");
	         
	         
	         httpServletRequest.setRequestURI(null);
	         httpForwardFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);
	         Mockito.verify(filterChain, Mockito.times(6)).doFilter(httpServletRequest, httpServletResponse);
	         
	         
	         httpServletRequest.setRequestURI("");
	         httpForwardFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);
	         Mockito.verify(filterChain, Mockito.times(7)).doFilter(httpServletRequest, httpServletResponse);
	         
	         
	         httpServletRequest.setRequestURI("/LiveApp/rest/broadcast");
	         httpForwardFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);
	         Mockito.verify(filterChain, Mockito.times(8)).doFilter(httpServletRequest, httpServletResponse);
	         
	         appSettings.setHttpForwardingExtension("m3u8,mp4");
	         httpServletRequest.setRequestURI("/LiveApp/rest/broadcast.mp4");
	         httpForwardFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);
	         //it shoud not increase because comma separated mp4
	         Mockito.verify(filterChain, Mockito.times(8)).doFilter(httpServletRequest, httpServletResponse);
	         
	         
	         appSettings.setHttpForwardingExtension("mkv");
	         httpServletRequest.setRequestURI("/LiveApp/rest/broadcast.mp4");
	         httpForwardFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);
	         //it shoud  increase because comma separated mp4
	         Mockito.verify(filterChain, Mockito.times(9)).doFilter(httpServletRequest, httpServletResponse);
	         
	         appSettings.setHttpForwardingExtension("mp4");
	         try {
	        	 	httpServletRequest.setRequestURI("/LiveApp/rest/../broadcast.mp4");
	        	 	httpForwardFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);
	         	fail("It should throw exception");
	         }
	         catch (IOException e) {
	        	 
	         }
	         //it should not  increase because it throws exception
	         Mockito.verify(filterChain, Mockito.times(9)).doFilter(httpServletRequest, httpServletResponse);
	         try {
		         appSettings.setHttpForwardingExtension("mp4");
		         httpServletRequest.setRequestURI("/LiveApp/rest/../broadcast.mp4");
		         httpForwardFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);
	         }
	         catch (IOException e) {
	        	 
	         }
	         //it should not increase because it throws exception
	         Mockito.verify(filterChain, Mockito.times(9)).doFilter(httpServletRequest, httpServletResponse);
	         
	         appSettings.setHttpForwardingExtension("mp4");
	         httpServletRequest.setRequestURI("/LiveApp/rest/broadcast.mp4");
	         httpForwardFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);
	         //it should not increase because it's  ok to forward
	         Mockito.verify(filterChain, Mockito.times(9)).doFilter(httpServletRequest, httpServletResponse);
	         
	         {
	        	 appSettings.setHttpForwardingExtension("m3u8");
	        	 appSettings.setHttpForwardingBaseURL("http://url");
		         httpServletRequest.setRequestURI("/LiveApp/streams/test.m3u8");

		         httpForwardFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);
		         //chain filter should not be called because  base url is et
		       //  Mockito.verify(filterChain, Mockito.times(5)).doFilter(httpServletRequest, httpServletResponse);
		         Mockito.verify(httpServletResponse, Mockito.times(2)).sendRedirect("http://url/streams/test.m3u8");
	         }
	         
	         {
	        	 appSettings.setHttpForwardingExtension("m3u8");
	        	 appSettings.setHttpForwardingBaseURL("http://url");
		         httpServletRequest.setRequestURI("LiveApp/streams/test.m3u8");

		         httpForwardFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);
		         //chain filter should not be called because  base url is et
		       //  Mockito.verify(filterChain, Mockito.times(5)).doFilter(httpServletRequest, httpServletResponse);
		         Mockito.verify(httpServletResponse, Mockito.times(3)).sendRedirect("http://url/streams/test.m3u8");
	        	 
	         }
	          
	         
	    }
}
