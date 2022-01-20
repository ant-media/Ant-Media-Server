package io.antmedia.test.filter;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpSession;
import javax.ws.rs.HttpMethod;

import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.ConfigurableWebApplicationContext;


import io.antmedia.console.rest.AuthenticationFilter;
import io.antmedia.datastore.db.DataStore;
import io.antmedia.datastore.db.DataStoreFactory;
import io.antmedia.datastore.db.InMemoryDataStore;
import io.antmedia.settings.ServerSettings;

public class AuthenticationFilterTest {
	
	 protected static Logger logger = LoggerFactory.getLogger(AuthenticationFilterTest.class);
	    private ServerSettings serverSettings;
	    private static final String USER_PASSWORD = "user.password";
	    public static final String USER_EMAIL = "user.email";
	    public static final String IS_AUTHENTICATED = "isAuthenticated";

	    @Test
	    public void testDoFilterPass() throws IOException, ServletException {
			 ConfigurableWebApplicationContext webAppContext = Mockito.mock(ConfigurableWebApplicationContext.class);	
			 when(webAppContext.isRunning()).thenReturn(true);
	    	
	    	AuthenticationFilter authenticationFilter = Mockito.spy(new AuthenticationFilter());
	        
	        MockHttpServletResponse httpServletResponse;
	        MockHttpServletRequest httpServletRequest;
	        MockFilterChain filterChain;
	        
	        serverSettings = new ServerSettings();
	        serverSettings.setJwtServerControlEnabled(true);
	        
	        Mockito.doReturn(serverSettings).when(authenticationFilter).getServerSetting();

			
	        // JWT Token null && JWT Server filter enable scenario
	        {   
	        	//reset filterchain
	        	filterChain = new MockFilterChain();
	        	
	        	//reset httpServletResponse
	        	httpServletResponse = new MockHttpServletResponse();
	        	
	        	//reset httpServletRequest
	        	httpServletRequest = new MockHttpServletRequest();
	        	
	            serverSettings.setJwtServerControlEnabled(true);
	            
	            Mockito.doReturn(serverSettings).when(authenticationFilter).getServerSetting();

	            authenticationFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);
	            assertEquals(HttpStatus.FORBIDDEN.value(),httpServletResponse.getStatus());
	        }
	        
	        // JWT Token filled && JWT Server filter enable scenario
	        {   
	        	//reset filterchain
	        	filterChain = new MockFilterChain();
	        	
	        	//reset httpServletResponse
	        	httpServletResponse = new MockHttpServletResponse();
	        	
	        	//reset httpServletRequest
	        	httpServletRequest = new MockHttpServletRequest();
	        	
	            serverSettings.setJwtServerControlEnabled(true);
	            
	            Mockito.doReturn(serverSettings).when(authenticationFilter).getServerSetting();
	            
	            httpServletRequest.addHeader("Authorization", "testtest");

	            authenticationFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);
	            assertEquals(HttpStatus.OK.value(),httpServletResponse.getStatus());
	        }
	        
	        // JWT Token null && JWT Server filter enable && requestURI is "rest/v2/authentication-status" parameters scenario
	        {   
	        	//reset filterchain
	        	filterChain = new MockFilterChain();
	        	
	        	//reset httpServletResponse
	        	httpServletResponse = new MockHttpServletResponse();
	        	
	        	//reset httpServletRequest
	        	httpServletRequest = new MockHttpServletRequest();
	        	
		         httpServletRequest.setRequestURI("/rest/v2/authentication-status");
	        	
	            serverSettings.setJwtServerControlEnabled(true);
	            
	            Mockito.doReturn(serverSettings).when(authenticationFilter).getServerSetting();
	            
	            authenticationFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);
	            assertEquals(HttpStatus.OK.value(),httpServletResponse.getStatus());
	        }
			
	        // JWT Token null && JWT Server filter disable && GET Method
	        {   
	        	//reset filterchain
	        	filterChain = new MockFilterChain();
	        	
	        	//reset httpServletResponse
	        	httpServletResponse = new MockHttpServletResponse();
	        	
	        	//reset httpServletRequest
	        	httpServletRequest = new MockHttpServletRequest();
	        	
	            Mockito.doReturn(serverSettings).when(authenticationFilter).getServerSetting();
	            
	            String password = "password";
	            String userName = "username" + (int) (Math.random() * 100000);

	            HttpSession session = Mockito.mock(HttpSession.class);
	            Mockito.when(session.getAttribute(IS_AUTHENTICATED)).thenReturn(true);
	            Mockito.when(session.getAttribute(USER_EMAIL)).thenReturn(userName);
	            Mockito.when(session.getAttribute(USER_PASSWORD)).thenReturn(password);

	            httpServletRequest.setSession(session);
	            httpServletRequest.setMethod(HttpMethod.GET);
	            
	            authenticationFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);
	            assertEquals(HttpStatus.OK.value(),httpServletResponse.getStatus());
	        }
	        
	        // JWT Token null && JWT Server filter disable && POST Method
	        {   
	        	//reset filterchain
	        	filterChain = new MockFilterChain();
	        	
	        	//reset httpServletResponse
	        	httpServletResponse = new MockHttpServletResponse();
	        	
	        	//reset httpServletRequest
	        	httpServletRequest = new MockHttpServletRequest();

	            Mockito.doReturn(serverSettings).when(authenticationFilter).getServerSetting();
	            
	            String password = "password";
	            String userName = "username" + (int) (Math.random() * 100000);

	            HttpSession session = Mockito.mock(HttpSession.class);
	            Mockito.when(session.getAttribute(IS_AUTHENTICATED)).thenReturn(true);
	            Mockito.when(session.getAttribute(USER_EMAIL)).thenReturn(userName);
	            Mockito.when(session.getAttribute(USER_PASSWORD)).thenReturn(password);

	            httpServletRequest.setSession(session);
	            
	            httpServletRequest.setMethod(HttpMethod.POST);
	            
	   		 	DataStoreFactory dtFactory = Mockito.mock(DataStoreFactory.class);
	   			DataStore dataStore = new InMemoryDataStore("dbname");
	   			
	   		 	Mockito.when(dtFactory.getDataStore()).thenReturn(dataStore);
	   		 	
	   		 	Mockito.doReturn(webAppContext).when(authenticationFilter).getWebApplicationContext();
	   		 	Mockito.doReturn(dtFactory).when(webAppContext).getBean(DataStoreFactory.BEAN_NAME);		 	

	            authenticationFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);
	            assertEquals(HttpStatus.FORBIDDEN.value(),httpServletResponse.getStatus());
	        }
	        
	        
			
	    
	    }

}
