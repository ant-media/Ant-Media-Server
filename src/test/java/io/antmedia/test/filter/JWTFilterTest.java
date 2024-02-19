package io.antmedia.test.filter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import jakarta.servlet.ServletException;

import jakarta.servlet.http.HttpServletResponse;
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
import io.antmedia.filter.JWTFilter;

public class JWTFilterTest {
	
	protected static Logger logger = LoggerFactory.getLogger(JWTFilterTest.class);
	
    @Test
    public void testDoFilterPass() throws IOException, ServletException {
    	   	
    	JWTFilter jwtFilter = Mockito.spy(new JWTFilter());
        
        MockHttpServletResponse httpServletResponse;
        MockHttpServletRequest httpServletRequest;
        MockFilterChain filterChain;
        
        AppSettings appSettings = new AppSettings();
        appSettings.setJwtSecretKey("testtesttesttesttesttesttesttest");       
        appSettings.setJwtControlEnabled(true);
        
        Mockito.doReturn(appSettings).when(jwtFilter).getAppSettings();
        
		String token = JWT.create().sign(Algorithm.HMAC256(appSettings.getJwtSecretKey()));
		String invalidToken = JWT.create().sign(Algorithm.HMAC256("invalid-key-invalid-key-invalid-key"));
		
		System.out.println("Valid Token: " + token);

        // App Settings Null (App getting initialized)
        {
            //reset filterchain
            filterChain = new MockFilterChain();

            //reset httpServletResponse
            httpServletResponse = Mockito.spy(new MockHttpServletResponse());

            //reset httpServletRequest
            httpServletRequest = new MockHttpServletRequest();

            appSettings.setJwtControlEnabled(true);

            Mockito.doReturn(null).when(jwtFilter).getAppSettings();

            httpServletRequest.addHeader("Authorization", token);

            jwtFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);
            assertEquals(HttpStatus.FORBIDDEN.value(),httpServletResponse.getStatus());
            Mockito.verify(httpServletResponse).sendError(HttpServletResponse.SC_FORBIDDEN, "Application is getting initialized");

        }
        // JWT Token enable and invalid token scenario
        {   
        	//reset filterchain
        	filterChain = new MockFilterChain();
        	
        	//reset httpServletResponse
        	httpServletResponse = new MockHttpServletResponse();
        	
        	//reset httpServletRequest
        	httpServletRequest = new MockHttpServletRequest();
        	
            appSettings.setJwtControlEnabled(true);
            
            Mockito.doReturn(appSettings).when(jwtFilter).getAppSettings();
            
            httpServletRequest.addHeader("Authorization", invalidToken);

            jwtFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);
            assertEquals(HttpStatus.FORBIDDEN.value(),httpServletResponse.getStatus());
        }
        
        // JWT Token disable and passed token scenario
        {
        	//reset filterchains
        	filterChain = new MockFilterChain();
        	
        	//reset httpServletResponses
        	httpServletResponse = new MockHttpServletResponse();
        	
        	//reset httpServletRequest
        	httpServletRequest = new MockHttpServletRequest();
        	
            appSettings.setJwtControlEnabled(false);
            
            Mockito.doReturn(appSettings).when(jwtFilter).getAppSettings();
            
            httpServletRequest.addHeader("Authorization", token);
            
            jwtFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);
            assertEquals(HttpStatus.OK.value(),httpServletResponse.getStatus()); 
        }
        
        // JWT Token enable and valid token scenario
        {
        	//reset filterchains
        	filterChain = new MockFilterChain();
        	
        	//reset httpServletResponses
        	httpServletResponse = new MockHttpServletResponse();
        	
        	//reset httpServletRequest
        	httpServletRequest = new MockHttpServletRequest();
        	
            appSettings.setJwtControlEnabled(true);
            
            Mockito.doReturn(appSettings).when(jwtFilter).getAppSettings();
            
            httpServletRequest.addHeader("Authorization", token);
            
            jwtFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);
            assertEquals(HttpStatus.OK.value(),httpServletResponse.getStatus());
        }
        
        // JWT Token enable and null header token scenario
        {
        	//reset filterchains
        	filterChain = new MockFilterChain();
        	
        	//reset httpServletResponses
        	httpServletResponse = new MockHttpServletResponse();
        	
        	//reset httpServletRequest
        	httpServletRequest = new MockHttpServletRequest();
        	
            appSettings.setJwtControlEnabled(true);
            
            Mockito.doReturn(appSettings).when(jwtFilter).getAppSettings();
            
            jwtFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);
            assertEquals(HttpStatus.FORBIDDEN.value(),httpServletResponse.getStatus());
        }        
    }
    
    
    @Test
	public void testGenerateAndVerifyTokenWithIssuer() {

		String token = JWTFilter.generateJwtToken("testtesttesttesttesttesttesttest", System.currentTimeMillis() + 10000, "test");
		assertTrue(JWTFilter.isJWTTokenValid("testtesttesttesttesttesttesttest", token, "test"));
		
		assertFalse(JWTFilter.isJWTTokenValid("testtesttesttesttesttesttesttest", token, "test2"));
		
	}
    
    @Test
	public void testJWTTokenValidWithSubscribers() {

		
		String token = JWTFilter.generateJwtToken("testtesttesttesttesttesttesttest", System.currentTimeMillis() + 10000, "subscriberId", "test");
		
		
		assertFalse(JWTFilter.isJWTTokenValid("testtesttesttesttesttesttesttest", token, "subscriberId", "test2"));

		assertFalse(JWTFilter.isJWTTokenValid("testtesttesttesttesttesttesttest", token, "subscriberId_df", "test2"));


		assertTrue(JWTFilter.isJWTTokenValid("testtesttesttesttesttesttesttest", token, "subscriberId", "test"));


	}	

}
