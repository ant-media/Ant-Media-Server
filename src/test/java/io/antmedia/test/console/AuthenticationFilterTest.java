package io.antmedia.test.console;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.ws.rs.HttpMethod;

import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.ConfigurableWebApplicationContext;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;

import io.antmedia.console.datastore.AbstractConsoleDataStore;
import io.antmedia.console.rest.AuthenticationFilter;
import io.antmedia.console.rest.CommonRestService;
import io.antmedia.datastore.db.types.User;
import io.antmedia.datastore.db.types.UserType;
import io.antmedia.settings.ServerSettings;

public class AuthenticationFilterTest {
	
	@Test
	public void testCheckPublicMethods() {

	    AuthenticationFilter filter = Mockito.spy(new AuthenticationFilter());
		
		ServerSettings serverSettings = new ServerSettings();
		Mockito.doReturn(serverSettings).when(filter).getServerSettings();
		
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		HttpServletResponse response =  Mockito.mock(HttpServletResponse.class);
		FilterChain chain = Mockito.mock(FilterChain.class);
		
		Mockito.when(request.getSession()).thenReturn(Mockito.mock(HttpSession.class));

		
		try {
			Mockito.when(request.getRequestURI()).thenReturn("/rest/v2/authentication-status");
			filter.doFilter(request, response, chain);
			Mockito.verify(chain).doFilter(request, response);

			Mockito.when(request.getRequestURI()).thenReturn("/rest/v2/users/initial");
			filter.doFilter(request, response, chain);
			Mockito.verify(chain, Mockito.times(2)).doFilter(request, response);

			Mockito.when(request.getRequestURI()).thenReturn("/rest/v2/first-login-status");
			filter.doFilter(request, response, chain);
			Mockito.verify(chain, Mockito.times(3)).doFilter(request, response);

			Mockito.when(request.getRequestURI()).thenReturn("/rest/v2/users/authenticate");
			filter.doFilter(request, response, chain);
			Mockito.verify(chain, Mockito.times(4)).doFilter(request, response);

			Mockito.when(request.getRequestURI()).thenReturn("/rest/v2/users");
			filter.doFilter(request, response, chain);
			Mockito.verify(chain, Mockito.times(4)).doFilter(request, response);
			Mockito.verify(response).sendError(HttpServletResponse.SC_FORBIDDEN, "Not authenticated user");
			
			
			Mockito.when(request.getRequestURI()).thenReturn("/rest/v2/blocked");
			filter.doFilter(request, response, chain);
			Mockito.verify(chain, Mockito.times(4)).doFilter(request, response);
			Mockito.verify(response, Mockito.times(2)).sendError(HttpServletResponse.SC_FORBIDDEN, "Not authenticated user");
			
			
			Mockito.when(request.getRequestURI()).thenReturn("/rest/v2/users/*/blocked");
			filter.doFilter(request, response, chain);
			Mockito.verify(chain, Mockito.times(5)).doFilter(request, response);

		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (ServletException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}
	
	@Test
	public void testGetMethod() {
		AuthenticationFilter filter = Mockito.spy(new AuthenticationFilter());

		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		HttpServletResponse response =  Mockito.mock(HttpServletResponse.class);
		FilterChain chain = Mockito.mock(FilterChain.class);
		
		HttpSession session = Mockito.mock(HttpSession.class);
		Mockito.when(request.getSession()).thenReturn(session);
		
		Mockito.when(session.getAttribute(CommonRestService.IS_AUTHENTICATED)).thenReturn(true);
		Mockito.when(session.getAttribute(CommonRestService.USER_EMAIL)).thenReturn("test@antmedia.ip");
		Mockito.when(session.getAttribute(CommonRestService.USER_PASSWORD)).thenReturn("test");
		

		Mockito.when(request.getMethod()).thenReturn(HttpMethod.GET);
		Mockito.doReturn(null).when(filter).getWebApplicationContext();
		try {
			
			Mockito.when(request.getRequestURI()).thenReturn("/rest/v2/system-status");
			//datastore is not set
			filter.doFilter(request, response, chain);
			Mockito.verify(response, Mockito.times(1)).sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database is not available. Please try again");
			
			AbstractConsoleDataStore store = Mockito.mock(AbstractConsoleDataStore.class);
			Mockito.doReturn(store).when(filter).getAbstractConsoleDataStore();
			filter.doFilter(request, response, chain);
			Mockito.verify(response, Mockito.times(1)).sendError(HttpServletResponse.SC_FORBIDDEN, "No user in this session");
			
			
			User user = Mockito.mock(User.class);
			Mockito.when(store.getUser(Mockito.any())).thenReturn(user);
			filter.doFilter(request, response, chain);
			//it should continue, because user scope is not defined and it's accepted as system
			Mockito.verify(chain, Mockito.times(1)).doFilter(request, response);
			
			Mockito.when(user.getScope()).thenReturn("system");
			filter.doFilter(request, response, chain);
			//it should continue, because user scope is system
			Mockito.verify(chain, Mockito.times(2)).doFilter(request, response);
			
			Mockito.when(user.getScope()).thenReturn("LiveApp");
			filter.doFilter(request, response, chain);
			Mockito.verify(response, Mockito.times(1)).sendError(HttpServletResponse.SC_FORBIDDEN, "Not allowed to access this resource. Contact system admin");
			
			Mockito.when(request.getRequestURI()).thenReturn("/rest/v2/version");
			filter.doFilter(request, response, chain);
			//it should continue, because path is allowed
			Mockito.verify(chain, Mockito.times(3)).doFilter(request, response);
			
			Mockito.when(request.getRequestURI()).thenReturn("/rest/v2/enterprise-edition");
			filter.doFilter(request, response, chain);
			//it should continue, because path is allowed
			Mockito.verify(chain, Mockito.times(4)).doFilter(request, response);
			
			Mockito.when(request.getRequestURI()).thenReturn("/rest/v2/applications/settings/LiveApp");
			filter.doFilter(request, response, chain);
			//it should continue, because path is allowed
			Mockito.verify(chain, Mockito.times(5)).doFilter(request, response);
			
			Mockito.when(request.getRequestURI()).thenReturn("/rest/v2/applications/settings/");
			filter.doFilter(request, response, chain);
			//it should not continue, because user scope is not matching
			Mockito.verify(response, Mockito.times(2)).sendError(HttpServletResponse.SC_FORBIDDEN, "Not allowed to access this resource. Contact system admin");
			
			// Type: User, Scope: WebRTCAppEE and requested application: WebRTCAppEE
			Mockito.when(request.getRequestURI()).thenReturn("/rest/v2/applications/settings/LiveApp");
			Mockito.when(user.getScope()).thenReturn("LiveApp");
			filter.doFilter(request, response, chain);
			Mockito.verify(chain, Mockito.times(6)).doFilter(request, response);
			
			// Type: User, Scope: WebRTCAppEE and requested application: LiveApp
			// It shouldn't access
			Mockito.when(request.getRequestURI()).thenReturn("/rest/v2/applications/settings/LiveApp");
			Mockito.when(user.getScope()).thenReturn("WebRTCAppEE");
			filter.doFilter(request, response, chain);
			Mockito.verify(response, Mockito.times(3)).sendError(HttpServletResponse.SC_FORBIDDEN, "Not allowed to access this resource. Contact system admin");
			
			// Type: User, Scope: system and requested application: LiveApp
			// It should access
			Mockito.when(request.getRequestURI()).thenReturn("/rest/v2/applications/settings/LiveApp");
			Mockito.when(user.getScope()).thenReturn("system");
			filter.doFilter(request, response, chain);
			Mockito.verify(chain, Mockito.times(7)).doFilter(request, response);
			
			
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (ServletException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		
	}
	
	@Test
	public void testNotGetMethods() {
		
		AuthenticationFilter filter = Mockito.spy(new AuthenticationFilter());

		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		HttpServletResponse response =  Mockito.mock(HttpServletResponse.class);
		FilterChain chain = Mockito.mock(FilterChain.class);
		
		HttpSession session = Mockito.mock(HttpSession.class);
		Mockito.when(request.getSession()).thenReturn(session);
		
		Mockito.when(session.getAttribute(CommonRestService.IS_AUTHENTICATED)).thenReturn(true);
		Mockito.when(session.getAttribute(CommonRestService.USER_EMAIL)).thenReturn("test@antmedia.io");
		Mockito.when(session.getAttribute(CommonRestService.USER_PASSWORD)).thenReturn("test");
		

		Mockito.when(request.getMethod()).thenReturn(HttpMethod.POST);
		Mockito.doReturn(null).when(filter).getWebApplicationContext();
		
		AbstractConsoleDataStore store = Mockito.mock(AbstractConsoleDataStore.class);
		Mockito.doReturn(store).when(filter).getAbstractConsoleDataStore();
		User user = Mockito.mock(User.class);
		Mockito.when(store.getUser(Mockito.any())).thenReturn(user);
		
		try {
			Mockito.when(request.getRequestURI()).thenReturn("/rest/v2/users/password");
			filter.doFilter(request, response, chain);
			Mockito.verify(chain, Mockito.times(1)).doFilter(request, response);
			
			Mockito.when(request.getRequestURI()).thenReturn("/rest/v2/support/request");
			filter.doFilter(request, response, chain);
			Mockito.verify(chain, Mockito.times(2)).doFilter(request, response);
			
			Mockito.when(request.getRequestURI()).thenReturn("/rest/v2/server-settings");
			filter.doFilter(request, response, chain);
			Mockito.verify(chain, Mockito.times(3)).doFilter(request, response);
			
			// Change server settings requests 
			// Type: Admin, Scope: null
			// It should access
			Mockito.when(user.getUserType()).thenReturn(UserType.ADMIN);
			Mockito.when(request.getRequestURI()).thenReturn("/rest/v2/server-settings");
			filter.doFilter(request, response, chain);
			Mockito.verify(chain, Mockito.times(4)).doFilter(request, response);
			
			// Type: Admin, Scope: LiveApp and requested: change server-settings
			// It shouldn't access
			Mockito.when(user.getUserType()).thenReturn(UserType.ADMIN);
			Mockito.when(user.getScope()).thenReturn("LiveApp");
			Mockito.when(request.getRequestURI()).thenReturn("/rest/v2/server-settings");
			filter.doFilter(request, response, chain);
			Mockito.verify(response, Mockito.times(1)).sendError(HttpServletResponse.SC_FORBIDDEN, "Not allowed to access this resource. Contact system admin");
			
			// Type: User, Scope: LiveApp and requested: change server-settings
			// It shouldn't access
			Mockito.when(user.getUserType()).thenReturn(UserType.USER);
			Mockito.when(user.getScope()).thenReturn("LiveApp");
			Mockito.when(request.getRequestURI()).thenReturn("/rest/v2/server-settings");
			filter.doFilter(request, response, chain);
			Mockito.verify(response, Mockito.times(2)).sendError(HttpServletResponse.SC_FORBIDDEN, "Not allowed to access this resource. Contact system admin");
	
			// Type: User, Scope: system and requested: change server-settings
			// It shouldn't access
			Mockito.when(user.getUserType()).thenReturn(UserType.USER);
			Mockito.when(user.getScope()).thenReturn("system");
			Mockito.when(request.getParameter(AuthenticationFilter.DISPATCH_PATH_URL)).thenReturn("");
			Mockito.when(request.getRequestURI()).thenReturn("/rest/v2/server-settings");
			filter.doFilter(request, response, chain);
			Mockito.verify(response, Mockito.times(3)).sendError(HttpServletResponse.SC_FORBIDDEN, "Not allowed to access this resource. Contact system admin");			
			
			
			// Admin internal request tests
			
			// Type: User, Scope: system and requested application: WebRTCAppEE
			// It should access
			Mockito.when(user.getUserType()).thenReturn(UserType.ADMIN);
			Mockito.when(user.getScope()).thenReturn("system");
			Mockito.when(request.getParameter(AuthenticationFilter.DISPATCH_PATH_URL)).thenReturn("WebRTCAppEE/rest/v2/");
			Mockito.when(request.getRequestURI()).thenReturn("/rest/v2/request");
			filter.doFilter(request, response, chain);
			Mockito.verify(chain, Mockito.times(5)).doFilter(request, response);
			
			// Type: User, Scope: WebRTCAppEE and requested application: LiveApp
			// It shouldn't access
			Mockito.when(user.getUserType()).thenReturn(UserType.ADMIN);
			Mockito.when(user.getScope()).thenReturn("WebRTCAppEE");
			Mockito.when(request.getParameter(AuthenticationFilter.DISPATCH_PATH_URL)).thenReturn("/LiveApp/rest/v2");
			filter.doFilter(request, response, chain);
			Mockito.verify(response, Mockito.times(4)).sendError(HttpServletResponse.SC_FORBIDDEN, "Not allowed to access this resource. Contact system admin");
			
			// Type: User, Scope: WebRTCAppEE and requested application: WebRTCAppEE
			// It should access
			Mockito.when(user.getUserType()).thenReturn(UserType.ADMIN);
			Mockito.when(user.getScope()).thenReturn("WebRTCAppEE");
			Mockito.when(request.getParameter(AuthenticationFilter.DISPATCH_PATH_URL)).thenReturn("/WebRTCAppEE/rest/v2");
			filter.doFilter(request, response, chain);
			Mockito.verify(chain, Mockito.times(6)).doFilter(request, response);
			
			
			// User internal request tests
			
			// Type: User, Scope: system and requested application: WebRTCAppEE
			// It should access
			Mockito.when(user.getUserType()).thenReturn(UserType.USER);
			Mockito.when(user.getScope()).thenReturn("system");
			Mockito.when(request.getParameter(AuthenticationFilter.DISPATCH_PATH_URL)).thenReturn("WebRTCAppEE/rest/v2/broadcasts");
			Mockito.when(request.getRequestURI()).thenReturn("rest/v2/request");
			filter.doFilter(request, response, chain);
			Mockito.verify(chain, Mockito.times(7)).doFilter(request, response);
			
			// Type: User, Scope: WebRTCAppEE and requested application: LiveApp
			// It shouldn't access
			Mockito.when(user.getUserType()).thenReturn(UserType.USER);
			Mockito.when(user.getScope()).thenReturn("WebRTCAppEE");
			Mockito.when(request.getParameter(AuthenticationFilter.DISPATCH_PATH_URL)).thenReturn("/LiveApp/rest/v2/broadcasts");
			filter.doFilter(request, response, chain);
			Mockito.verify(response, Mockito.times(5)).sendError(HttpServletResponse.SC_FORBIDDEN, "Not allowed to access this resource. Contact system admin");
			
			// Type: User, Scope: WebRTCAppEE and requested application: WebRTCAppEE
			// It should access
			Mockito.when(user.getUserType()).thenReturn(UserType.USER);
			Mockito.when(user.getScope()).thenReturn("WebRTCAppEE");
			Mockito.when(request.getParameter(AuthenticationFilter.DISPATCH_PATH_URL)).thenReturn("/WebRTCAppEE/rest/v2/vods");
			filter.doFilter(request, response, chain);
			Mockito.verify(chain, Mockito.times(8)).doFilter(request, response);
			
			// Read-Only internal request tests
			
			// Type: Read-Only, Scope: system and requested application: WebRTCAppEE
			// It shouldn't access
			Mockito.when(user.getUserType()).thenReturn(UserType.READ_ONLY);
			Mockito.when(user.getScope()).thenReturn("system");
			Mockito.when(request.getParameter(AuthenticationFilter.DISPATCH_PATH_URL)).thenReturn("WebRTCAppEE/rest/v2/broadcasts");
			Mockito.when(request.getRequestURI()).thenReturn("rest/v2/request");
			filter.doFilter(request, response, chain);
			Mockito.verify(response, Mockito.times(6)).sendError(HttpServletResponse.SC_FORBIDDEN, "Not allowed to access this resource. Contact system admin");
			
			// Type: Read-Only, Scope: WebRTCAppEE and requested application: LiveApp
			// It shouldn't access
			Mockito.when(user.getUserType()).thenReturn(UserType.READ_ONLY);
			Mockito.when(user.getScope()).thenReturn("WebRTCAppEE");
			Mockito.when(request.getParameter(AuthenticationFilter.DISPATCH_PATH_URL)).thenReturn("/LiveApp/rest/v2/broadcasts");
			filter.doFilter(request, response, chain);
			Mockito.verify(response, Mockito.times(7)).sendError(HttpServletResponse.SC_FORBIDDEN, "Not allowed to access this resource. Contact system admin");
			
			// Type: Read-Only, Scope: WebRTCAppEE and requested application: WebRTCAppEE
			// It shouldn't access
			Mockito.when(user.getUserType()).thenReturn(UserType.READ_ONLY);
			Mockito.when(user.getScope()).thenReturn("WebRTCAppEE");
			Mockito.when(request.getParameter(AuthenticationFilter.DISPATCH_PATH_URL)).thenReturn("/WebRTCAppEE/rest/v2/vods");
			filter.doFilter(request, response, chain);
			Mockito.verify(response, Mockito.times(8)).sendError(HttpServletResponse.SC_FORBIDDEN, "Not allowed to access this resource. Contact system admin");
			
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (ServletException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		
	}

	
	  @Test
	    public void testJWTFilterPass() throws IOException, ServletException {
			ConfigurableWebApplicationContext webAppContext = Mockito.mock(ConfigurableWebApplicationContext.class);	
			when(webAppContext.isRunning()).thenReturn(true);
	    	
	    	AuthenticationFilter authenticationFilter = Mockito.spy(new AuthenticationFilter());
	        
	    	HttpServletResponse httpServletResponse;
	        MockHttpServletRequest httpServletRequest;
	        MockFilterChain filterChain;
	        
	        ServerSettings serverSettings = new ServerSettings();
	        serverSettings.setJwtServerControlEnabled(true);
	        serverSettings.setJwtServerSecretKey("testtesttesttesttesttesttesttest");
	        Mockito.doReturn(serverSettings).when(authenticationFilter).getServerSettings();
	        
	        String validToken = JWT.create().sign(Algorithm.HMAC256(serverSettings.getJwtServerSecretKey()));
	        String invalidToken = JWT.create().sign(Algorithm.HMAC256("invalid-key-invalid-key-invalid-key"));
	        String appJwtToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.e30._dEfIoSDLBJo49hmyivqYziMIGeWOD19Ex0kGGK_6ww";
	        String jwkstoken = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6InFWMWhqT0o2RFlFOWIzRDFSU0lHSSJ9.eyJpc3MiOiJodHRwczovL2FudG1lZGlhLnVzLmF1dGgwLmNvbS8iLCJzdWIiOiI3UVEzWTlLSzJPY1dOSzRwMXpydGU1UzQxSWR4amxLc0BjbGllbnRzIiwiYXVkIjoiaHR0cHM6Ly9hbnRtZWRpYS51cy5hdXRoMC5jb20vYXBpL3YyLyIsImlhdCI6MTYyODE1MTQ4NSwiZXhwIjoxNjI4MjM3ODg1LCJhenAiOiI3UVEzWTlLSzJPY1dOSzRwMXpydGU1UzQxSWR4amxLcyIsImd0eSI6ImNsaWVudC1jcmVkZW50aWFscyJ9.dgtT0dqL_JiA1AJRIWYyJMU7KG_EpJzucqlmIdEt36rL35G9QLWcxJVWCM-OFDAje9UaNDqFVHMNfXzDhvXrs5LvPlEFSZVAZUtMgjP0X94hlCjKIrvhnfN2lcDZFsSMIqXJeoPMjlRvGItrphRQaMr5ow3eCcvRYVK1MXvptGisdh1rTVBWPRRvaFH8x5yISw98DwNauVWW949o8JDIkortDQEXHmpipC7NoACCzZmGlmBv6ubaZxyTwh7QAp68kx6_tIcmj7nm6cLdoheFjH-io-ee6oTkLRr2krRqSjJrY9A_hJYH4Gixpe-F7mMeE8dMuRGhWue3pfMJmy8ulQ";

   			AbstractConsoleDataStore store = Mockito.mock(AbstractConsoleDataStore.class);
   			Mockito.doReturn(store).when(authenticationFilter).getAbstractConsoleDataStore();

	        // JWT Token null && JWT Server filter enable scenario
   			// It will try to login with user/pass details
	        {   
	        	//reset filterchain
	        	filterChain = new MockFilterChain();
	        	
	        	//reset httpServletResponse
	        	httpServletResponse = Mockito.spy(new MockHttpServletResponse());
	        	
	        	//reset httpServletRequest
	        	httpServletRequest = new MockHttpServletRequest();
	        	
	            authenticationFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);
	            assertEquals(HttpStatus.FORBIDDEN.value(),httpServletResponse.getStatus());
				Mockito.verify(httpServletResponse).sendError(HttpServletResponse.SC_FORBIDDEN, "Not authenticated user");
	        }
	        
	        // JWT Token disable and valid token scenario
	        {
	            //reset filterchains
	            filterChain = new MockFilterChain();

	            //reset httpServletResponses
	        	httpServletResponse = Mockito.spy(new MockHttpServletResponse());

	            //reset httpServletRequest
	            httpServletRequest = new MockHttpServletRequest();

	            serverSettings.setJwtServerControlEnabled(false);

	            Mockito.doReturn(serverSettings).when(authenticationFilter).getServerSettings();

	            httpServletRequest.addHeader("ProxyAuthorization", validToken);

	            authenticationFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);
	            assertEquals(HttpStatus.FORBIDDEN.value(),httpServletResponse.getStatus());
	            Mockito.verify(httpServletResponse).sendError(HttpServletResponse.SC_FORBIDDEN, "Not authenticated user");
	        }
	        
	        //Restore JWT Server Control to true
        	serverSettings.setJwtServerControlEnabled(true);
	        
	        // JWT Token filled && JWT Server filter enable && invalid token scenario
	        {   
	        	//reset filterchain
	        	filterChain = new MockFilterChain();
	        	
	        	//reset httpServletResponse
	        	httpServletResponse = new MockHttpServletResponse();
	        	
	        	//reset httpServletRequest
	        	httpServletRequest = new MockHttpServletRequest();
	        	
	            httpServletRequest.addHeader("ProxyAuthorization", invalidToken);

	            authenticationFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);
	            assertEquals(HttpStatus.FORBIDDEN.value(),httpServletResponse.getStatus());
	        }
	        
	        // JWT Token filled && JWT Server filter enable && valid token scenario
	        {   
	        	//reset filterchain
	        	filterChain = new MockFilterChain();
	        	
	        	//reset httpServletResponse
	        	httpServletResponse = new MockHttpServletResponse();
	        	
	        	//reset httpServletRequest
	        	httpServletRequest = new MockHttpServletRequest();
	        	
	            httpServletRequest.addHeader("ProxyAuthorization", validToken);

	            authenticationFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);
	            assertEquals(HttpStatus.OK.value(),httpServletResponse.getStatus());
	        }
	        
	        // JWT Token filled && JWT Server filter enable && valid token scenario
	        {   
	        	//reset filterchain
	        	filterChain = new MockFilterChain();
	        	
	        	//reset httpServletResponse
	        	httpServletResponse = new MockHttpServletResponse();
	        	
	        	//reset httpServletRequest
	        	httpServletRequest = new MockHttpServletRequest();
	        	
	        	//Authorization is not correct header to access the resources
	            httpServletRequest.addHeader("Authorization", validToken);

	            authenticationFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);
	            assertEquals(HttpStatus.FORBIDDEN.value(),httpServletResponse.getStatus());
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
	            
	            authenticationFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);
	            assertEquals(HttpStatus.OK.value(),httpServletResponse.getStatus());
	        }  
	        
	        // Valid JWKS Token Scenario
	        {
	            //reset filterchains
	            filterChain = new MockFilterChain();

	            //reset httpServletResponses
	            httpServletResponse = new MockHttpServletResponse();

	            //reset httpServletRequest
	            httpServletRequest = new MockHttpServletRequest();

	            httpServletRequest.setRemoteAddr("11.11.11.11");
	            serverSettings.setJwksURL("https://antmedia.us.auth0.com");
	            serverSettings.setJwtServerSecretKey("4Hr7PWwrTf6YFynkO5QeNQrlxe5r7HtfUdLhis2i_vbXdtF1VI0SwnP0ZSlhf0Yh");

	            Mockito.doReturn(serverSettings).when(authenticationFilter).getServerSettings();

	            httpServletRequest.addHeader("ProxyAuthorization", jwkstoken);

	            authenticationFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);
	            assertEquals(HttpStatus.OK.value(),httpServletResponse.getStatus());
	        }
	        
	        // JWKS null scenario
	        {
	            //reset filterchains
	            filterChain = new MockFilterChain();

	            //reset httpServletResponses
	            httpServletResponse = new MockHttpServletResponse();

	            //reset httpServletRequest
	            httpServletRequest = new MockHttpServletRequest();

	            httpServletRequest.setRemoteAddr("11.11.11.11");
	            serverSettings.setJwksURL("");
	            serverSettings.setJwtServerSecretKey("random");

	            Mockito.doReturn(serverSettings).when(authenticationFilter).getServerSettings();

	            httpServletRequest.addHeader("ProxyAuthorization", jwkstoken);

	            authenticationFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);
	            assertEquals(HttpStatus.FORBIDDEN.value(),httpServletResponse.getStatus());
	        }
	        
	  }

	@Test
	public void testGetMethodMultiApp() {
		AuthenticationFilter filter = Mockito.spy(new AuthenticationFilter());

		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		HttpServletResponse response =  Mockito.mock(HttpServletResponse.class);
		FilterChain chain = Mockito.mock(FilterChain.class);

		HttpSession session = Mockito.mock(HttpSession.class);
		Mockito.when(request.getSession()).thenReturn(session);

		Mockito.when(session.getAttribute(CommonRestService.IS_AUTHENTICATED)).thenReturn(true);
		Mockito.when(session.getAttribute(CommonRestService.USER_EMAIL)).thenReturn("test@antmedia.ip");
		Mockito.when(session.getAttribute(CommonRestService.USER_PASSWORD)).thenReturn("test");


		Mockito.when(request.getMethod()).thenReturn(HttpMethod.GET);
		Mockito.doReturn(null).when(filter).getWebApplicationContext();
		try {

			Mockito.when(request.getRequestURI()).thenReturn("/rest/v2/system-status");
			//datastore is not set
			filter.doFilter(request, response, chain);
			Mockito.verify(response, Mockito.times(1)).sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database is not available. Please try again");

			AbstractConsoleDataStore store = Mockito.mock(AbstractConsoleDataStore.class);
			Mockito.doReturn(store).when(filter).getAbstractConsoleDataStore();
			filter.doFilter(request, response, chain);
			Mockito.verify(response, Mockito.times(1)).sendError(HttpServletResponse.SC_FORBIDDEN, "No user in this session");


			User user = Mockito.mock(User.class);
			Map<String,String> appNameUserTypeMap = new HashMap<>();
			appNameUserTypeMap.put("LiveApp", UserType.ADMIN.toString());

			Mockito.when(user.getAppNameUserType()).thenReturn(appNameUserTypeMap);


			user.setAppNameUserType(appNameUserTypeMap);
			Mockito.when(store.getUser(Mockito.any())).thenReturn(user);
			filter.doFilter(request, response, chain);
			//it should continue, because user scope is not defined and it's accepted as system
			Mockito.verify(response, Mockito.times(1)).sendError(HttpServletResponse.SC_FORBIDDEN, "Not allowed to access this resource. Contact system admin");

			Mockito.when(request.getRequestURI()).thenReturn("/rest/v2/version");
			filter.doFilter(request, response, chain);
			//it should continue, because path is allowed
			Mockito.verify(chain, Mockito.times(1)).doFilter(request, response);

			Mockito.when(request.getRequestURI()).thenReturn("/rest/v2/enterprise-edition");
			filter.doFilter(request, response, chain);
			//it should continue, because path is allowed
			Mockito.verify(chain, Mockito.times(2)).doFilter(request, response);

			Mockito.when(request.getRequestURI()).thenReturn("/rest/v2/applications/settings/LiveApp");
			filter.doFilter(request, response, chain);
			//it should continue, because path is allowed
			Mockito.verify(chain, Mockito.times(3)).doFilter(request, response);

			Mockito.when(request.getRequestURI()).thenReturn("/rest/v2/applications/settings/");
			filter.doFilter(request, response, chain);
			Mockito.verify(response, Mockito.times(2)).sendError(HttpServletResponse.SC_FORBIDDEN, "Not allowed to access this resource. Contact system admin");

			// It shouldn't access
			Mockito.when(request.getRequestURI()).thenReturn("/rest/v2/applications/settings/WebRTCAppEE");
			filter.doFilter(request, response, chain);
			Mockito.verify(response, Mockito.times(3)).sendError(HttpServletResponse.SC_FORBIDDEN, "Not allowed to access this resource. Contact system admin");

			// It should access
			appNameUserTypeMap.put("WebRTCAppEE", UserType.ADMIN.toString());
			Mockito.when(request.getRequestURI()).thenReturn("/rest/v2/applications/settings/WebRTCAppEE");
			filter.doFilter(request, response, chain);
			Mockito.verify(chain, Mockito.times(4)).doFilter(request, response);

		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (ServletException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	@Test
	public void testNotGetMethodsMultiApp() {

		AuthenticationFilter filter = Mockito.spy(new AuthenticationFilter());

		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		HttpServletResponse response =  Mockito.mock(HttpServletResponse.class);
		FilterChain chain = Mockito.mock(FilterChain.class);

		HttpSession session = Mockito.mock(HttpSession.class);
		Mockito.when(request.getSession()).thenReturn(session);

		Mockito.when(session.getAttribute(CommonRestService.IS_AUTHENTICATED)).thenReturn(true);
		Mockito.when(session.getAttribute(CommonRestService.USER_EMAIL)).thenReturn("test@antmedia.io");
		Mockito.when(session.getAttribute(CommonRestService.USER_PASSWORD)).thenReturn("test");


		Mockito.when(request.getMethod()).thenReturn(HttpMethod.POST);
		Mockito.doReturn(null).when(filter).getWebApplicationContext();

		AbstractConsoleDataStore store = Mockito.mock(AbstractConsoleDataStore.class);
		Mockito.doReturn(store).when(filter).getAbstractConsoleDataStore();
		User user = Mockito.mock(User.class);
		Map<String,String> appNameUserTypeMap = new HashMap<>();
		appNameUserTypeMap.put("LiveApp", UserType.ADMIN.toString());

		Mockito.when(user.getAppNameUserType()).thenReturn(appNameUserTypeMap);

		Mockito.when(store.getUser(Mockito.any())).thenReturn(user);

		try {
			Mockito.when(request.getRequestURI()).thenReturn("/rest/v2/users/password");
			filter.doFilter(request, response, chain);
			Mockito.verify(chain, Mockito.times(1)).doFilter(request, response);

			Mockito.when(request.getRequestURI()).thenReturn("/rest/v2/support/request");
			filter.doFilter(request, response, chain);
			Mockito.verify(chain, Mockito.times(2)).doFilter(request, response);

			appNameUserTypeMap = new HashMap<>();
			appNameUserTypeMap.put("system", UserType.ADMIN.toString());
			Mockito.when(user.getAppNameUserType()).thenReturn(appNameUserTypeMap);

			Mockito.when(request.getRequestURI()).thenReturn("/rest/v2/server-settings");
			filter.doFilter(request, response, chain);
			Mockito.verify(chain, Mockito.times(3)).doFilter(request, response);

			// Type: Admin, Scope: LiveApp and requested: change server-settings
			// It shouldn't access
			 appNameUserTypeMap = new HashMap<>();
			appNameUserTypeMap.put("WebRTCAppEE", UserType.ADMIN.toString());
			Mockito.when(user.getAppNameUserType()).thenReturn(appNameUserTypeMap);

			Mockito.when(request.getRequestURI()).thenReturn("/rest/v2/server-settings");
			filter.doFilter(request, response, chain);
			Mockito.verify(response, Mockito.times(1)).sendError(HttpServletResponse.SC_FORBIDDEN, "Not allowed to access this resource. Contact system admin");

			// Type: User, Scope: LiveApp and requested: change server-settings
			// It shouldn't access
			appNameUserTypeMap = new HashMap<>();
			appNameUserTypeMap.put("LiveApp", UserType.USER.toString());
			Mockito.when(user.getAppNameUserType()).thenReturn(appNameUserTypeMap);
			Mockito.when(request.getRequestURI()).thenReturn("/rest/v2/server-settings");
			filter.doFilter(request, response, chain);
			Mockito.verify(response, Mockito.times(2)).sendError(HttpServletResponse.SC_FORBIDDEN, "Not allowed to access this resource. Contact system admin");

			// Type: User, Scope: system and requested: change server-settings
			// It shouldn't access
			appNameUserTypeMap = new HashMap<>();
			appNameUserTypeMap.put("system", UserType.USER.toString());
			Mockito.when(user.getAppNameUserType()).thenReturn(appNameUserTypeMap);
			Mockito.when(request.getParameter(AuthenticationFilter.DISPATCH_PATH_URL)).thenReturn("");
			Mockito.when(request.getRequestURI()).thenReturn("/rest/v2/server-settings");
			filter.doFilter(request, response, chain);
			Mockito.verify(response, Mockito.times(3)).sendError(HttpServletResponse.SC_FORBIDDEN, "Not allowed to access this resource. Contact system admin");


			// Admin internal request tests

			// Type: User, Scope: system and requested application: WebRTCAppEE
			// It should access

			appNameUserTypeMap = new HashMap<>();
			appNameUserTypeMap.put("system", UserType.ADMIN.toString());
			Mockito.when(user.getAppNameUserType()).thenReturn(appNameUserTypeMap);

			Mockito.when(request.getParameter(AuthenticationFilter.DISPATCH_PATH_URL)).thenReturn("WebRTCAppEE/rest/v2/");
			Mockito.when(request.getRequestURI()).thenReturn("/rest/v2/request");
			filter.doFilter(request, response, chain);
			Mockito.verify(chain, Mockito.times(4)).doFilter(request, response);


			appNameUserTypeMap = new HashMap<>();
			appNameUserTypeMap.put("WebRTCAppEE", UserType.ADMIN.toString());
			Mockito.when(user.getAppNameUserType()).thenReturn(appNameUserTypeMap);
			Mockito.when(request.getParameter(AuthenticationFilter.DISPATCH_PATH_URL)).thenReturn("/LiveApp/rest/v2");
			filter.doFilter(request, response, chain);
			Mockito.verify(response, Mockito.times(4)).sendError(HttpServletResponse.SC_FORBIDDEN, "Not allowed to access this resource. Contact system admin");


			Mockito.when(request.getParameter(AuthenticationFilter.DISPATCH_PATH_URL)).thenReturn("/WebRTCAppEE/rest/v2");
			filter.doFilter(request, response, chain);
			Mockito.verify(chain, Mockito.times(5)).doFilter(request, response);



			appNameUserTypeMap = new HashMap<>();
			appNameUserTypeMap.put("WebRTCAppEE", UserType.USER.toString());
			Mockito.when(user.getAppNameUserType()).thenReturn(appNameUserTypeMap);
			Mockito.when(request.getParameter(AuthenticationFilter.DISPATCH_PATH_URL)).thenReturn("WebRTCAppEE/rest/v2/broadcasts");
			Mockito.when(request.getRequestURI()).thenReturn("rest/v2/request");
			filter.doFilter(request, response, chain);
			Mockito.verify(chain, Mockito.times(6)).doFilter(request, response);

			Mockito.when(request.getParameter(AuthenticationFilter.DISPATCH_PATH_URL)).thenReturn("/LiveApp/rest/v2/broadcasts");
			filter.doFilter(request, response, chain);
			Mockito.verify(response, Mockito.times(5)).sendError(HttpServletResponse.SC_FORBIDDEN, "Not allowed to access this resource. Contact system admin");

			// Type: User, Scope: WebRTCAppEE and requested application: WebRTCAppEE
			// It should access

			Mockito.when(request.getParameter(AuthenticationFilter.DISPATCH_PATH_URL)).thenReturn("/WebRTCAppEE/rest/v2/vods");
			filter.doFilter(request, response, chain);
			Mockito.verify(chain, Mockito.times(7)).doFilter(request, response);

			// Read-Only internal request tests

			// Type: Read-Only, Scope: system and requested application: WebRTCAppEE
			// It shouldn't access
			appNameUserTypeMap = new HashMap<>();
			appNameUserTypeMap.put("system", UserType.READ_ONLY.toString());
			Mockito.when(user.getAppNameUserType()).thenReturn(appNameUserTypeMap);
			Mockito.when(request.getParameter(AuthenticationFilter.DISPATCH_PATH_URL)).thenReturn("WebRTCAppEE/rest/v2/broadcasts");
			Mockito.when(request.getRequestURI()).thenReturn("rest/v2/request");
			filter.doFilter(request, response, chain);
			Mockito.verify(response, Mockito.times(6)).sendError(HttpServletResponse.SC_FORBIDDEN, "Not allowed to access this resource. Contact system admin");

			appNameUserTypeMap = new HashMap<>();
			appNameUserTypeMap.put("WebRTCAppEE", UserType.READ_ONLY.toString());
			Mockito.when(user.getAppNameUserType()).thenReturn(appNameUserTypeMap);
			Mockito.when(request.getParameter(AuthenticationFilter.DISPATCH_PATH_URL)).thenReturn("/LiveApp/rest/v2/broadcasts");
			filter.doFilter(request, response, chain);
			Mockito.verify(response, Mockito.times(7)).sendError(HttpServletResponse.SC_FORBIDDEN, "Not allowed to access this resource. Contact system admin");

			// Type: Read-Only, Scope: WebRTCAppEE and requested application: WebRTCAppEE
			// It shouldn't access
			Mockito.when(request.getParameter(AuthenticationFilter.DISPATCH_PATH_URL)).thenReturn("/WebRTCAppEE/rest/v2/vods");
			filter.doFilter(request, response, chain);
			Mockito.verify(response, Mockito.times(8)).sendError(HttpServletResponse.SC_FORBIDDEN, "Not allowed to access this resource. Contact system admin");

		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (ServletException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	private static final String PROXY_URI = "/rest/v2/request";
	private static final String APP_INFO_URI = "/rest/v2/applications-info";
	private static final String SEARCH_URI = "/rest/v2/streams/search";
	private static final String LIST_PATH = "/rest/v2/broadcasts/list/0/10";

	private static User mapUser(UserType userType, Map<String, String> appRoles) {
		return new User("scoped@test.com", "pass", userType, null, appRoles);
	}

	private static User customer1MapUser() {
		return mapUser(null, Map.of("customer1", UserType.ADMIN.toString()));
	}

	private static User customer1ScopeUser() {
		return new User("scoped@test.com", "pass", UserType.ADMIN, "customer1", new HashMap<>());
	}

	// Forbidden requests must end in a 403, so a filter exception fails the test rather than passing as "denied".
	private boolean isForwarded(User user, String method, String uri, String dispatchPath) throws Exception {
		AuthenticationFilter filter = Mockito.spy(new AuthenticationFilter());
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
		FilterChain chain = Mockito.mock(FilterChain.class);
		HttpSession session = Mockito.mock(HttpSession.class);

		when(request.getSession()).thenReturn(session);
		when(session.getAttribute(CommonRestService.IS_AUTHENTICATED)).thenReturn(true);
		when(session.getAttribute(CommonRestService.USER_EMAIL)).thenReturn(user.getEmail());
		when(session.getAttribute(CommonRestService.USER_PASSWORD)).thenReturn("pass");
		when(request.getMethod()).thenReturn(method);
		when(request.getRequestURI()).thenReturn(uri);
		when(request.getParameter(AuthenticationFilter.DISPATCH_PATH_URL)).thenReturn(dispatchPath);

		Mockito.doReturn(null).when(filter).getWebApplicationContext();
		AbstractConsoleDataStore store = Mockito.mock(AbstractConsoleDataStore.class);
		Mockito.doReturn(store).when(filter).getAbstractConsoleDataStore();
		when(store.getUser(Mockito.any())).thenReturn(user);

		filter.doFilter(request, response, chain);

		boolean forwarded = !Mockito.mockingDetails(chain).getInvocations().isEmpty();
		if (!forwarded) {
			Mockito.verify(response).sendError(HttpServletResponse.SC_FORBIDDEN, AuthenticationFilter.FORBIDDEN_ERROR);
		}
		return forwarded;
	}

	@Tag("fast")
	@Test
	public void testProxyPathMustMatchAllowedAppExactly() throws Exception {
		for (User user : List.of(customer1MapUser(), customer1ScopeUser())) {
			for (String allowed : List.of("customer1" + LIST_PATH, "/customer1" + LIST_PATH, "customer1", "customer1/rest/v2/broadcasts/cam.1")) {
				assertThat(isForwarded(user, HttpMethod.GET, PROXY_URI, allowed)).as(allowed).isTrue();
			}
			for (String denied : List.of("customer10" + LIST_PATH, "/customer10" + LIST_PATH, "customer1x" + LIST_PATH, "customer1-test" + LIST_PATH, "Customer1" + LIST_PATH)) {
				assertThat(isForwarded(user, HttpMethod.GET, PROXY_URI, denied)).as(denied).isFalse();
			}
		}
	}

	@Tag("fast")
	@Test
	public void testProxyWriteToPrefixMatchedAppIsForbidden() throws Exception {
		String own = "customer1/rest/v2/broadcasts/cam1";
		String other = "customer10/rest/v2/broadcasts/cam1";

		User mapRole = customer1MapUser();
		assertThat(isForwarded(mapRole, HttpMethod.DELETE, PROXY_URI, own)).isTrue();
		assertThat(isForwarded(mapRole, HttpMethod.DELETE, PROXY_URI, other)).isFalse();

		// User-level ADMIN with no scope: must be a clean 403 for other apps.
		User userLevelAdmin = mapUser(UserType.ADMIN, Map.of("customer1", UserType.ADMIN.toString()));
		assertThat(isForwarded(userLevelAdmin, HttpMethod.DELETE, PROXY_URI, own)).isTrue();
		assertThat(isForwarded(userLevelAdmin, HttpMethod.DELETE, PROXY_URI, other)).isFalse();

		User scoped = customer1ScopeUser();
		assertThat(isForwarded(scoped, HttpMethod.DELETE, PROXY_URI, own)).isTrue();
		assertThat(isForwarded(scoped, HttpMethod.DELETE, PROXY_URI, other)).isFalse();
	}

	// The proxy forwards _path to the server, which resolves these to customer10.
	@Tag("fast")
	@Test
	public void testProxyPathWithDotSegmentsIsForbidden() throws Exception {
		List<String> traversals = List.of(
				"customer1/../customer10" + LIST_PATH,
				"/customer1/../customer10" + LIST_PATH,
				"customer1/./../customer10" + LIST_PATH,
				"customer1/%2e%2e/customer10" + LIST_PATH,
				"customer1/%2E%2E/customer10" + LIST_PATH,
				"customer1/.%2e/customer10" + LIST_PATH,
				"customer1/%2e./customer10" + LIST_PATH,
				"customer1/..;/customer10" + LIST_PATH,
				"customer1/..%2fcustomer10" + LIST_PATH,
				"customer1/..\\customer10" + LIST_PATH);

		for (User user : List.of(customer1MapUser(), customer1ScopeUser())) {
			for (String traversal : traversals) {
				assertThat(isForwarded(user, HttpMethod.GET, PROXY_URI, traversal)).as(traversal).isFalse();
				assertThat(isForwarded(user, HttpMethod.POST, PROXY_URI, traversal)).as(traversal).isFalse();
			}
		}
	}

	// _path grants access only on the proxy. Elsewhere it must not unlock a management endpoint.
	@Tag("fast")
	@Test
	public void testProxyPathGrantsNothingOutsideTheProxy() throws Exception {
		String ownPath = "customer1" + LIST_PATH;

		for (User user : List.of(customer1MapUser(), customer1ScopeUser())) {
			for (String uri : List.of("/rest/v2/user-list", "/rest/v2/server-settings", "/rest/v2/thread-dump", "/rest/v2/system-status")) {
				assertThat(isForwarded(user, HttpMethod.GET, uri, ownPath)).as(uri).isFalse();
			}
			for (String uri : List.of("/rest/v2/users", "/rest/v2/server-settings")) {
				assertThat(isForwarded(user, HttpMethod.POST, uri, ownPath)).as(uri).isFalse();
			}

			assertThat(isForwarded(user, HttpMethod.GET, "/rest/request", ownPath)).isTrue();
			assertThat(isForwarded(user, HttpMethod.GET, "/rest/v2/request/sub", ownPath)).isTrue();

			// The container resolves these to /rest/v2/user-list after the filter has run.
			for (String disguised : List.of("/rest/v2/request/../user-list", "/rest/v2/request/%2e%2e/user-list", "/rest/v2/request/%2E%2E/user-list")) {
				assertThat(isForwarded(user, HttpMethod.GET, disguised, ownPath)).as(disguised).isFalse();
			}
		}

		User systemAdmin = mapUser(null, Map.of("system", UserType.ADMIN.toString()));
		assertThat(isForwarded(systemAdmin, HttpMethod.GET, "/rest/v2/user-list", "anything")).isTrue();
	}

	@Tag("fast")
	@Test
	public void testScopedUserCanReadOwnAppListAndSearchOnly() throws Exception {
		User readOnly = mapUser(null, Map.of("customer1", UserType.READ_ONLY.toString()));

		for (User user : List.of(customer1MapUser(), customer1ScopeUser(), readOnly)) {
			for (String uri : List.of(APP_INFO_URI, SEARCH_URI)) {
				assertThat(isForwarded(user, HttpMethod.GET, uri, null)).as(uri).isTrue();
				assertThat(isForwarded(user, HttpMethod.POST, uri, null)).as(uri).isFalse();
				assertThat(isForwarded(user, HttpMethod.GET, uri + "/extra", null)).as(uri).isFalse();
			}
		}
	}

	// CommonRestService treats "no session user" as a server JWT request, which is only safe if this holds.
	@Tag("fast")
	@Test
	public void testAnonymousCannotReadAppListOrSearch() throws Exception {
		for (String uri : List.of(APP_INFO_URI, SEARCH_URI)) {
			AuthenticationFilter filter = Mockito.spy(new AuthenticationFilter());
			HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
			HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
			FilterChain chain = Mockito.mock(FilterChain.class);

			when(request.getSession()).thenReturn(Mockito.mock(HttpSession.class));
			when(request.getMethod()).thenReturn(HttpMethod.GET);
			when(request.getRequestURI()).thenReturn(uri);
			Mockito.doReturn(null).when(filter).getWebApplicationContext();

			filter.doFilter(request, response, chain);

			Mockito.verify(chain, Mockito.never()).doFilter(request, response);
			Mockito.verify(response).sendError(HttpServletResponse.SC_FORBIDDEN, "Not authenticated user");
		}
	}

	@Tag("fast")
	@Test
	public void testScopedUserStaysForbiddenFromSystemEndpoints() throws Exception {
		List<String> systemUris = List.of("/rest/v2/system-status", "/rest/v2/server-settings", "/rest/v2/user-list", "/rest/v2/applications",
				"/rest/v2/cluster-mode-status", "/rest/v2/last-licence-status", "/rest/v2/thread-dump");

		for (User user : List.of(customer1MapUser(), customer1ScopeUser())) {
			for (String uri : systemUris) {
				assertThat(isForwarded(user, HttpMethod.GET, uri, null)).as(uri).isFalse();
			}
		}
	}

	@Tag("fast")
	@Test
	public void testScopedUserCanLogout() throws Exception {
		User readOnly = mapUser(null, Map.of("customer1", UserType.READ_ONLY.toString()));

		for (User user : List.of(customer1MapUser(), customer1ScopeUser(), readOnly)) {
			assertThat(isForwarded(user, HttpMethod.DELETE, "/rest/v2/users/logout", null)).isTrue();
		}
	}

	// Older users: blank scope and a null or empty map keep today's all-access.
	@Tag("fast")
	@Test
	public void testUsersWithoutScopesKeepAllAccess() throws Exception {
		User blankScope = new User("scoped@test.com", "pass", UserType.ADMIN, "", new HashMap<>());
		assertThat(isForwarded(blankScope, HttpMethod.GET, PROXY_URI, "anyapp" + LIST_PATH)).isTrue();

		assertThat(isForwarded(mapUser(null, null), HttpMethod.GET, "/rest/v2/system-status", null)).isTrue();
		assertThat(isForwarded(mapUser(null, new HashMap<>()), HttpMethod.GET, "/rest/v2/system-status", null)).isTrue();
		assertThat(isForwarded(mapUser(null, null), HttpMethod.GET, PROXY_URI, "anyapp" + LIST_PATH)).isTrue();
	}



}
