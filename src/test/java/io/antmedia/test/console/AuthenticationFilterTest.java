package io.antmedia.test.console;

import static org.junit.Assert.fail;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.HttpMethod;

import org.junit.Test;
import org.mockito.Mockito;

import io.antmedia.console.datastore.AbstractConsoleDataStore;
import io.antmedia.console.rest.AuthenticationFilter;
import io.antmedia.console.rest.CommonRestService;
import io.antmedia.datastore.db.types.User;
import io.antmedia.rest.model.UserType;

public class AuthenticationFilterTest {


	@Test
	public void testCheckPublicMethods() {

		AuthenticationFilter filter = new AuthenticationFilter();

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
			Mockito.doReturn(store).when(filter).getDataStore();
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
		Mockito.doReturn(store).when(filter).getDataStore();
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

}
