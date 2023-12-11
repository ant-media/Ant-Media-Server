package io.antmedia.test.filter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;

import jakarta.servlet.ServletException;

import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;

import io.antmedia.AppSettings;
import io.antmedia.filter.ContentSecurityPolicyHeaderFilter;
import io.antmedia.filter.JWTFilter;

public class ContentSecurityPolicyHeaderFilterTest {
	
	protected static Logger logger = LoggerFactory.getLogger(ContentSecurityPolicyHeaderFilterTest.class);
	
    @Test
    public void testDoFilterPass() throws IOException, ServletException {
    	   	
    	ContentSecurityPolicyHeaderFilter filter = Mockito.spy(new ContentSecurityPolicyHeaderFilter());
        
        MockHttpServletResponse httpServletResponse;
        MockHttpServletRequest httpServletRequest;
        MockFilterChain filterChain;
        
      
        //set appsettings null
        Mockito.doReturn(null).when(filter).getAppSettings();
		
        {   
        	//reset filterchain
        	filterChain = new MockFilterChain();
        	
        	//reset httpServletResponse
        	httpServletResponse = new MockHttpServletResponse();
        	
        	//reset httpServletRequest
        	httpServletRequest = new MockHttpServletRequest();
        	

            filter.doFilter(httpServletRequest, httpServletResponse, filterChain);
            assertNull(httpServletResponse.getHeader("Content-Security-Policy"));
        }
        
        
        AppSettings appSettings = new AppSettings();
        //set appsettings but don't set content security policy
        Mockito.doReturn(appSettings).when(filter).getAppSettings();
    	
        {   
        	//reset filterchain
        	filterChain = new MockFilterChain();
        	
        	//reset httpServletResponse
        	httpServletResponse = new MockHttpServletResponse();
        	
        	//reset httpServletRequest
        	httpServletRequest = new MockHttpServletRequest();
        	
            assertEquals("", appSettings.getContentSecurityPolicyHeaderValue());

            filter.doFilter(httpServletRequest, httpServletResponse, filterChain);
            assertNull(httpServletResponse.getHeader("Content-Security-Policy"));
        }
        
        
        appSettings.setContentSecurityPolicyHeaderValue("frame-ancestor self;");
        {   
        	//reset filterchain
        	filterChain = new MockFilterChain();
        	
        	//reset httpServletResponse
        	httpServletResponse = new MockHttpServletResponse();
        	
        	//reset httpServletRequest
        	httpServletRequest = new MockHttpServletRequest();
        	
            assertNotNull(appSettings.getContentSecurityPolicyHeaderValue());

            filter.doFilter(httpServletRequest, httpServletResponse, filterChain);
            assertNotNull(httpServletResponse.getHeader("Content-Security-Policy"));
            assertEquals("frame-ancestor self;", httpServletResponse.getHeader("Content-Security-Policy"));
        }
        
        appSettings.setContentSecurityPolicyHeaderValue("");
        {   
        	//reset filterchain
        	filterChain = new MockFilterChain();
        	
        	//reset httpServletResponse
        	httpServletResponse = new MockHttpServletResponse();
        	
        	//reset httpServletRequest
        	httpServletRequest = new MockHttpServletRequest();
        	
            assertNotNull(appSettings.getContentSecurityPolicyHeaderValue());

            filter.doFilter(httpServletRequest, httpServletResponse, filterChain);
            assertNull(httpServletResponse.getHeader("Content-Security-Policy"));
        }
        
    }

}
