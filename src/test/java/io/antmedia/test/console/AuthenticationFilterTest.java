package io.antmedia.test.console;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

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
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.web.context.ConfigurableWebApplicationContext;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.sun.net.httpserver.HttpServer;

import io.antmedia.console.datastore.AbstractConsoleDataStore;
import io.antmedia.console.rest.AuthenticationFilter;
import io.antmedia.console.rest.CommonRestService;
import io.antmedia.console.servlet.ProxyServlet;
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
		Mockito.when(request.getMethod()).thenReturn(HttpMethod.GET);


		try {
			Mockito.when(request.getServletPath()).thenReturn("/rest/v2/authentication-status");
			filter.doFilter(request, response, chain);
			Mockito.verify(chain).doFilter(request, response);

			Mockito.when(request.getServletPath()).thenReturn("/rest/v2/users/initial");
			filter.doFilter(request, response, chain);
			Mockito.verify(chain, Mockito.times(2)).doFilter(request, response);

			Mockito.when(request.getServletPath()).thenReturn("/rest/v2/first-login-status");
			filter.doFilter(request, response, chain);
			Mockito.verify(chain, Mockito.times(3)).doFilter(request, response);

			Mockito.when(request.getServletPath()).thenReturn("/rest/v2/users/authenticate");
			filter.doFilter(request, response, chain);
			Mockito.verify(chain, Mockito.times(4)).doFilter(request, response);

			Mockito.when(request.getServletPath()).thenReturn("/rest/v2/users");
			filter.doFilter(request, response, chain);
			Mockito.verify(chain, Mockito.times(4)).doFilter(request, response);
			Mockito.verify(response).sendError(HttpServletResponse.SC_FORBIDDEN, "Not authenticated user");
			
			
			Mockito.when(request.getServletPath()).thenReturn("/rest/v2/blocked");
			filter.doFilter(request, response, chain);
			Mockito.verify(chain, Mockito.times(4)).doFilter(request, response);
			Mockito.verify(response, Mockito.times(2)).sendError(HttpServletResponse.SC_FORBIDDEN, "Not authenticated user");
			
			
			Mockito.when(request.getServletPath()).thenReturn("/rest/v2/users/*/blocked");
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
			
			Mockito.when(request.getServletPath()).thenReturn("/rest/v2/system-status");
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
			
			Mockito.when(request.getServletPath()).thenReturn("/rest/v2/version");
			filter.doFilter(request, response, chain);
			//it should continue, because path is allowed
			Mockito.verify(chain, Mockito.times(3)).doFilter(request, response);
			
			Mockito.when(request.getServletPath()).thenReturn("/rest/v2/enterprise-edition");
			filter.doFilter(request, response, chain);
			//it should continue, because path is allowed
			Mockito.verify(chain, Mockito.times(4)).doFilter(request, response);
			
			Mockito.when(request.getServletPath()).thenReturn("/rest/v2/applications/settings/LiveApp");
			filter.doFilter(request, response, chain);
			//it should continue, because path is allowed
			Mockito.verify(chain, Mockito.times(5)).doFilter(request, response);
			
			Mockito.when(request.getServletPath()).thenReturn("/rest/v2/applications/settings/");
			filter.doFilter(request, response, chain);
			//it should not continue, because user scope is not matching
			Mockito.verify(response, Mockito.times(2)).sendError(HttpServletResponse.SC_FORBIDDEN, "Not allowed to access this resource. Contact system admin");
			
			// Type: User, Scope: WebRTCAppEE and requested application: WebRTCAppEE
			Mockito.when(request.getServletPath()).thenReturn("/rest/v2/applications/settings/LiveApp");
			Mockito.when(user.getScope()).thenReturn("LiveApp");
			filter.doFilter(request, response, chain);
			Mockito.verify(chain, Mockito.times(6)).doFilter(request, response);
			
			// Type: User, Scope: WebRTCAppEE and requested application: LiveApp
			// It shouldn't access
			Mockito.when(request.getServletPath()).thenReturn("/rest/v2/applications/settings/LiveApp");
			Mockito.when(user.getScope()).thenReturn("WebRTCAppEE");
			filter.doFilter(request, response, chain);
			Mockito.verify(response, Mockito.times(3)).sendError(HttpServletResponse.SC_FORBIDDEN, "Not allowed to access this resource. Contact system admin");
			
			// Type: User, Scope: system and requested application: LiveApp
			// It should access
			Mockito.when(request.getServletPath()).thenReturn("/rest/v2/applications/settings/LiveApp");
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
			Mockito.when(request.getServletPath()).thenReturn("/rest/v2/users/password");
			filter.doFilter(request, response, chain);
			Mockito.verify(chain, Mockito.times(1)).doFilter(request, response);
			
			Mockito.when(request.getServletPath()).thenReturn("/rest/v2/support/request");
			filter.doFilter(request, response, chain);
			Mockito.verify(chain, Mockito.times(2)).doFilter(request, response);
			
			Mockito.when(request.getServletPath()).thenReturn("/rest/v2/server-settings");
			filter.doFilter(request, response, chain);
			Mockito.verify(chain, Mockito.times(3)).doFilter(request, response);
			
			// Change server settings requests 
			// Type: Admin, Scope: null
			// It should access
			Mockito.when(user.getUserType()).thenReturn(UserType.ADMIN);
			Mockito.when(request.getServletPath()).thenReturn("/rest/v2/server-settings");
			filter.doFilter(request, response, chain);
			Mockito.verify(chain, Mockito.times(4)).doFilter(request, response);
			
			// Type: Admin, Scope: LiveApp and requested: change server-settings
			// It shouldn't access
			Mockito.when(user.getUserType()).thenReturn(UserType.ADMIN);
			Mockito.when(user.getScope()).thenReturn("LiveApp");
			Mockito.when(request.getServletPath()).thenReturn("/rest/v2/server-settings");
			filter.doFilter(request, response, chain);
			Mockito.verify(response, Mockito.times(1)).sendError(HttpServletResponse.SC_FORBIDDEN, "Not allowed to access this resource. Contact system admin");
			
			// Type: User, Scope: LiveApp and requested: change server-settings
			// It shouldn't access
			Mockito.when(user.getUserType()).thenReturn(UserType.USER);
			Mockito.when(user.getScope()).thenReturn("LiveApp");
			Mockito.when(request.getServletPath()).thenReturn("/rest/v2/server-settings");
			filter.doFilter(request, response, chain);
			Mockito.verify(response, Mockito.times(2)).sendError(HttpServletResponse.SC_FORBIDDEN, "Not allowed to access this resource. Contact system admin");
	
			// Type: User, Scope: system and requested: change server-settings
			// It shouldn't access
			Mockito.when(user.getUserType()).thenReturn(UserType.USER);
			Mockito.when(user.getScope()).thenReturn("system");
			Mockito.when(request.getParameter(AuthenticationFilter.DISPATCH_PATH_URL)).thenReturn("");
			Mockito.when(request.getServletPath()).thenReturn("/rest/v2/server-settings");
			filter.doFilter(request, response, chain);
			Mockito.verify(response, Mockito.times(3)).sendError(HttpServletResponse.SC_FORBIDDEN, "Not allowed to access this resource. Contact system admin");			
			
			
			// Admin internal request tests
			
			// Type: User, Scope: system and requested application: WebRTCAppEE
			// It should access
			Mockito.when(user.getUserType()).thenReturn(UserType.ADMIN);
			Mockito.when(user.getScope()).thenReturn("system");
			Mockito.when(request.getParameter(AuthenticationFilter.DISPATCH_PATH_URL)).thenReturn("WebRTCAppEE/rest/v2/");
			Mockito.when(request.getServletPath()).thenReturn("/rest/v2/request");
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
			Mockito.when(request.getServletPath()).thenReturn("rest/v2/request");
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
			Mockito.when(request.getServletPath()).thenReturn("rest/v2/request");
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
	        	
		        httpServletRequest.setServletPath("/rest/v2/authentication-status");
	            
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

			Mockito.when(request.getServletPath()).thenReturn("/rest/v2/system-status");
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

			Mockito.when(request.getServletPath()).thenReturn("/rest/v2/version");
			filter.doFilter(request, response, chain);
			//it should continue, because path is allowed
			Mockito.verify(chain, Mockito.times(1)).doFilter(request, response);

			Mockito.when(request.getServletPath()).thenReturn("/rest/v2/enterprise-edition");
			filter.doFilter(request, response, chain);
			//it should continue, because path is allowed
			Mockito.verify(chain, Mockito.times(2)).doFilter(request, response);

			Mockito.when(request.getServletPath()).thenReturn("/rest/v2/applications/settings/LiveApp");
			filter.doFilter(request, response, chain);
			//it should continue, because path is allowed
			Mockito.verify(chain, Mockito.times(3)).doFilter(request, response);

			Mockito.when(request.getServletPath()).thenReturn("/rest/v2/applications/settings/");
			filter.doFilter(request, response, chain);
			Mockito.verify(response, Mockito.times(2)).sendError(HttpServletResponse.SC_FORBIDDEN, "Not allowed to access this resource. Contact system admin");

			// It shouldn't access
			Mockito.when(request.getServletPath()).thenReturn("/rest/v2/applications/settings/WebRTCAppEE");
			filter.doFilter(request, response, chain);
			Mockito.verify(response, Mockito.times(3)).sendError(HttpServletResponse.SC_FORBIDDEN, "Not allowed to access this resource. Contact system admin");

			// It should access
			appNameUserTypeMap.put("WebRTCAppEE", UserType.ADMIN.toString());
			Mockito.when(request.getServletPath()).thenReturn("/rest/v2/applications/settings/WebRTCAppEE");
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
			Mockito.when(request.getServletPath()).thenReturn("/rest/v2/users/password");
			filter.doFilter(request, response, chain);
			Mockito.verify(chain, Mockito.times(1)).doFilter(request, response);

			Mockito.when(request.getServletPath()).thenReturn("/rest/v2/support/request");
			filter.doFilter(request, response, chain);
			Mockito.verify(chain, Mockito.times(2)).doFilter(request, response);

			appNameUserTypeMap = new HashMap<>();
			appNameUserTypeMap.put("system", UserType.ADMIN.toString());
			Mockito.when(user.getAppNameUserType()).thenReturn(appNameUserTypeMap);

			Mockito.when(request.getServletPath()).thenReturn("/rest/v2/server-settings");
			filter.doFilter(request, response, chain);
			Mockito.verify(chain, Mockito.times(3)).doFilter(request, response);

			// Type: Admin, Scope: LiveApp and requested: change server-settings
			// It shouldn't access
			 appNameUserTypeMap = new HashMap<>();
			appNameUserTypeMap.put("WebRTCAppEE", UserType.ADMIN.toString());
			Mockito.when(user.getAppNameUserType()).thenReturn(appNameUserTypeMap);

			Mockito.when(request.getServletPath()).thenReturn("/rest/v2/server-settings");
			filter.doFilter(request, response, chain);
			Mockito.verify(response, Mockito.times(1)).sendError(HttpServletResponse.SC_FORBIDDEN, "Not allowed to access this resource. Contact system admin");

			// Type: User, Scope: LiveApp and requested: change server-settings
			// It shouldn't access
			appNameUserTypeMap = new HashMap<>();
			appNameUserTypeMap.put("LiveApp", UserType.USER.toString());
			Mockito.when(user.getAppNameUserType()).thenReturn(appNameUserTypeMap);
			Mockito.when(request.getServletPath()).thenReturn("/rest/v2/server-settings");
			filter.doFilter(request, response, chain);
			Mockito.verify(response, Mockito.times(2)).sendError(HttpServletResponse.SC_FORBIDDEN, "Not allowed to access this resource. Contact system admin");

			// Type: User, Scope: system and requested: change server-settings
			// It shouldn't access
			appNameUserTypeMap = new HashMap<>();
			appNameUserTypeMap.put("system", UserType.USER.toString());
			Mockito.when(user.getAppNameUserType()).thenReturn(appNameUserTypeMap);
			Mockito.when(request.getParameter(AuthenticationFilter.DISPATCH_PATH_URL)).thenReturn("");
			Mockito.when(request.getServletPath()).thenReturn("/rest/v2/server-settings");
			filter.doFilter(request, response, chain);
			Mockito.verify(response, Mockito.times(3)).sendError(HttpServletResponse.SC_FORBIDDEN, "Not allowed to access this resource. Contact system admin");


			// Admin internal request tests

			// Type: User, Scope: system and requested application: WebRTCAppEE
			// It should access

			appNameUserTypeMap = new HashMap<>();
			appNameUserTypeMap.put("system", UserType.ADMIN.toString());
			Mockito.when(user.getAppNameUserType()).thenReturn(appNameUserTypeMap);

			Mockito.when(request.getParameter(AuthenticationFilter.DISPATCH_PATH_URL)).thenReturn("WebRTCAppEE/rest/v2/");
			Mockito.when(request.getServletPath()).thenReturn("/rest/v2/request");
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
			Mockito.when(request.getServletPath()).thenReturn("rest/v2/request");
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
			Mockito.when(request.getServletPath()).thenReturn("rest/v2/request");
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

	private static MockHttpServletRequest dispatchedRequest(String method, String rawUri, String servletPath, String pathInfo) {
		MockHttpServletRequest request = new MockHttpServletRequest(method, rawUri);
		request.setServletPath(servletPath);
		request.setPathInfo(pathInfo);
		return request;
	}

	private static AuthenticationFilter securityTestFilter(ServerSettings serverSettings, AbstractConsoleDataStore store) {
		AuthenticationFilter filter = Mockito.spy(new AuthenticationFilter());
		Mockito.doReturn(serverSettings).when(filter).getServerSettings();
		Mockito.doReturn(store).when(filter).getAbstractConsoleDataStore();
		return filter;
	}

	private static AbstractConsoleDataStore loginAsReadOnlySystemUser(MockHttpServletRequest request) {
		String email = "readonly@antmedia.io";
		MockHttpSession session = new MockHttpSession();
		session.setAttribute(CommonRestService.IS_AUTHENTICATED, true);
		session.setAttribute(CommonRestService.USER_EMAIL, email);
		session.setAttribute(CommonRestService.USER_PASSWORD, "password");
		request.setSession(session);

		AbstractConsoleDataStore store = Mockito.mock(AbstractConsoleDataStore.class);
		Mockito.when(store.getUser(email)).thenReturn(new User(email, "password", UserType.READ_ONLY, null,
				Map.of("system", UserType.READ_ONLY.toString())));
		return store;
	}

	@Test
	public void testDotDotUnderUsersPathDoesNotReachProxyWithoutLogin() throws Exception {
		AuthenticationFilter filter = securityTestFilter(new ServerSettings(), null);

		for (String method : new String[] {"GET", "POST"}) {
			MockHttpServletRequest request = dispatchedRequest(method, "/rest/v2/users/x/../../request/blocked", "/rest/v2/request", "/blocked");
			MockHttpServletResponse response = new MockHttpServletResponse();
			MockFilterChain chain = new MockFilterChain();

			filter.doFilter(request, response, chain);

			assertThat(chain.getRequest()).as(method).isNull();
			assertThat(response.getStatus()).as(method).isEqualTo(HttpServletResponse.SC_FORBIDDEN);
			assertThat(request.getAttribute(AuthenticationFilter.PANEL_AUTHENTICATED_ATTRIBUTE)).as(method).isNull();
		}
	}

	@Test
	public void testBlockedStatusIsPublicOnlyForSingleSegmentGet() throws Exception {
		AuthenticationFilter filter = securityTestFilter(new ServerSettings(), null);

		MockHttpServletRequest get = dispatchedRequest("GET", "/rest/v2/users/admin@antmedia.io/blocked", "/rest", "/v2/users/admin@antmedia.io/blocked");
		MockFilterChain getChain = new MockFilterChain();
		filter.doFilter(get, new MockHttpServletResponse(), getChain);
		assertThat(getChain.getRequest()).isSameAs(get);
		assertThat(get.getAttribute(AuthenticationFilter.PANEL_AUTHENTICATED_ATTRIBUTE)).isNull();

		MockHttpServletRequest post = dispatchedRequest("POST", "/rest/v2/users/admin@antmedia.io/blocked", "/rest", "/v2/users/admin@antmedia.io/blocked");
		MockHttpServletResponse postResponse = new MockHttpServletResponse();
		MockFilterChain postChain = new MockFilterChain();
		filter.doFilter(post, postResponse, postChain);
		assertThat(postChain.getRequest()).isNull();
		assertThat(postResponse.getStatus()).isEqualTo(HttpServletResponse.SC_FORBIDDEN);

		MockHttpServletRequest nested = dispatchedRequest("GET", "/rest/v2/users/a/b/blocked", "/rest", "/v2/users/a/b/blocked");
		MockHttpServletResponse nestedResponse = new MockHttpServletResponse();
		MockFilterChain nestedChain = new MockFilterChain();
		filter.doFilter(nested, nestedResponse, nestedChain);
		assertThat(nestedChain.getRequest()).isNull();
		assertThat(nestedResponse.getStatus()).isEqualTo(HttpServletResponse.SC_FORBIDDEN);
	}

	@Test
	public void testDotDotUnderSupportPathDoesNotLetReadOnlyUserWriteThroughProxy() throws Exception {
		MockHttpServletRequest request = dispatchedRequest("POST", "/rest/v2/support/request/../../request", "/rest/v2/request", null);
		request.addParameter(AuthenticationFilter.DISPATCH_PATH_URL, "LiveApp/rest/v2/broadcasts/create");
		AuthenticationFilter filter = securityTestFilter(new ServerSettings(), loginAsReadOnlySystemUser(request));
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain chain = new MockFilterChain();

		filter.doFilter(request, response, chain);

		assertThat(chain.getRequest()).isNull();
		assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_FORBIDDEN);
		assertThat(response.getErrorMessage()).isEqualTo(AuthenticationFilter.FORBIDDEN_ERROR);
	}

	@Test
	public void testLoggedInRequestIsMarkedForProxy() throws Exception {
		MockHttpServletRequest request = dispatchedRequest("GET", "/rest/v2/request", "/rest/v2/request", null);
		request.addParameter(AuthenticationFilter.DISPATCH_PATH_URL, "LiveApp/rest/v2/broadcasts/list/0/10");
		AuthenticationFilter filter = securityTestFilter(new ServerSettings(), loginAsReadOnlySystemUser(request));
		MockFilterChain chain = new MockFilterChain();

		filter.doFilter(request, new MockHttpServletResponse(), chain);

		assertThat(chain.getRequest()).isSameAs(request);
		assertThat(request.getAttribute(AuthenticationFilter.PANEL_AUTHENTICATED_ATTRIBUTE)).isEqualTo(Boolean.TRUE);
	}

	@Test
	public void testValidServerJwtIsMarkedForProxy() throws Exception {
		ServerSettings serverSettings = new ServerSettings();
		serverSettings.setJwtServerControlEnabled(true);
		serverSettings.setJwtServerSecretKey("testtesttesttesttesttesttesttest");
		AuthenticationFilter filter = securityTestFilter(serverSettings, null);
		MockHttpServletRequest request = dispatchedRequest("GET", "/rest/v2/request", "/rest/v2/request", null);
		request.addHeader(AuthenticationFilter.PROXY_AUTHORIZATION_HEADER_JWT_TOKEN,
				JWT.create().sign(Algorithm.HMAC256(serverSettings.getJwtServerSecretKey())));
		MockFilterChain chain = new MockFilterChain();

		filter.doFilter(request, new MockHttpServletResponse(), chain);

		assertThat(chain.getRequest()).isSameAs(request);
		assertThat(request.getAttribute(AuthenticationFilter.PANEL_AUTHENTICATED_ATTRIBUTE)).isEqualTo(Boolean.TRUE);
	}

	@Test
	public void testProxyForwardsOnlyRequestsAuthenticatedByFilter() throws Exception {
		AtomicReference<String> forwardedUri = new AtomicReference<>();
		HttpServer app = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		app.createContext("/", exchange -> {
			forwardedUri.set(exchange.getRequestURI().toString());
			byte[] body = "ok".getBytes(StandardCharsets.UTF_8);
			exchange.sendResponseHeaders(200, body.length);
			exchange.getResponseBody().write(body);
			exchange.close();
		});
		app.start();

		ProxyServlet proxy = new ProxyServlet();
		try {
			MockServletConfig config = new MockServletConfig();
			config.addInitParameter("targetUri", "http://127.0.0.1:" + app.getAddress().getPort() + "/{_path}");
			config.addInitParameter("forwardip", "false");
			proxy.init(config);

			MockHttpServletRequest unmarked = dispatchedRequest("GET", "/rest/v2/request", "/rest/v2/request", null);
			unmarked.setQueryString("_path=LiveApp/rest/v2/broadcasts/list/0/10");
			MockHttpServletResponse unmarkedResponse = new MockHttpServletResponse();
			proxy.service(unmarked, unmarkedResponse);
			assertThat(unmarkedResponse.getStatus()).isEqualTo(HttpServletResponse.SC_FORBIDDEN);
			assertThat(forwardedUri.get()).isNull();

			MockHttpServletRequest marked = dispatchedRequest("GET", "/rest/v2/request", "/rest/v2/request", null);
			marked.setQueryString("_path=LiveApp/rest/v2/broadcasts/list/0/10");
			marked.setAttribute(AuthenticationFilter.PANEL_AUTHENTICATED_ATTRIBUTE, Boolean.TRUE);
			MockHttpServletResponse markedResponse = new MockHttpServletResponse();
			proxy.service(marked, markedResponse);
			assertThat(markedResponse.getStatus()).isEqualTo(200);
			assertThat(markedResponse.getContentAsString()).isEqualTo("ok");
			assertThat(forwardedUri.get()).isEqualTo("/LiveApp/rest/v2/broadcasts/list/0/10");
		}
		finally {
			proxy.destroy();
			app.stop(0);
		}
	}

}
