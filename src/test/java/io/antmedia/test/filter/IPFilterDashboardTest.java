package io.antmedia.test.filter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

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

import io.antmedia.filter.IPFilterDashboard;
import io.antmedia.settings.ServerSettings;

public class IPFilterDashboardTest {

	protected static Logger logger = LoggerFactory.getLogger(IPFilterDashboardTest.class);

	
	@Test
	public void testBugNullContext() {
		 IPFilterDashboard ipFilter = Mockito.spy(new IPFilterDashboard());
		 
		 Mockito.doReturn(null).when(ipFilter).getAppContext();
		 assertFalse(ipFilter.isAllowedDashboard("127.0.0.1"));
		 

		 Mockito.doReturn(null).when(ipFilter).getServerSettings();
		 assertFalse(ipFilter.isAllowedDashboard("127.0.0.1"));
		 
		 
		
	}
	
	
    @Test
    public void testDoFilterPass() throws IOException, ServletException {
        IPFilterDashboard ipFilter = Mockito.spy(new IPFilterDashboard());

        MockHttpServletRequest httpServletRequest = new MockHttpServletRequest();
        httpServletRequest.setRemoteAddr("127.0.0.1");
        
        MockHttpServletResponse httpServletResponse = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();
        
        ServerSettings serverSettings = new ServerSettings();
        serverSettings.setAllowedDashboardCIDR("127.0.0.1/8");
        
        Mockito.doReturn(serverSettings).when(ipFilter).getServerSettings();
        
        ipFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);

        assertEquals(HttpStatus.OK.value(),httpServletResponse.getStatus());
    }

    @Test
    public void testDoFilterFail() throws IOException, ServletException {
    	IPFilterDashboard ipFilter = Mockito.spy(new IPFilterDashboard());

        MockHttpServletRequest httpServletRequest = new MockHttpServletRequest();
        httpServletRequest.setRemoteAddr("192.168.0.1");
        MockHttpServletResponse httpServletResponse = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();
        
        ServerSettings serverSettings = new ServerSettings();
        
        serverSettings.setAllowedDashboardCIDR("127.0.0.1/8");
        Mockito.doReturn(serverSettings).when(ipFilter).getServerSettings();
        
        httpServletRequest.setPathInfo("");
        
        ipFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);

        assertEquals(HttpStatus.FORBIDDEN.value(),httpServletResponse.getStatus());
    }
	
}
