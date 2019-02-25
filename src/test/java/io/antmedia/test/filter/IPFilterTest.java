package io.antmedia.test.filter;

import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import io.antmedia.AppSettings;
import io.antmedia.filter.IPFilter;

import javax.servlet.ServletException;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

public class IPFilterTest {

    @Test
    public void testDoFilterPass() throws IOException, ServletException {
        IPFilter ipFilter = Mockito.spy(new IPFilter());

        MockHttpServletRequest httpServletRequest = new MockHttpServletRequest();
        httpServletRequest.setRemoteAddr("127.0.0.1");
        
        MockHttpServletResponse httpServletResponse = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();
        
        AppSettings appSettings = new AppSettings();
        appSettings.setRemoteAllowedCIDR("127.0.0.1/8");
        
        Mockito.doReturn(appSettings).when(ipFilter).getAppSettings();
        
        ipFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);

        assertEquals(HttpStatus.OK.value(),httpServletResponse.getStatus());
    }

    @Test
    public void testDoFilterFail() throws IOException, ServletException {
        IPFilter ipFilter = Mockito.spy(new IPFilter());

        MockHttpServletRequest httpServletRequest = new MockHttpServletRequest();
        httpServletRequest.setRemoteAddr("192.168.0.1");
        MockHttpServletResponse httpServletResponse = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();
        AppSettings appSettings = new AppSettings();
        appSettings.setRemoteAllowedCIDR("127.0.0.1/8");
        Mockito.doReturn(appSettings).when(ipFilter).getAppSettings();
        
        ipFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);

        assertEquals(HttpStatus.FORBIDDEN.value(),httpServletResponse.getStatus());
    }

}
