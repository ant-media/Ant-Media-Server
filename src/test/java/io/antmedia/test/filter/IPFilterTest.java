package io.antmedia.test.filter;

import io.antmedia.filter.ipfilter.IPFilter;
import io.antmedia.filter.ipfilter.IPFilterSource;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.ServletException;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

public class IPFilterTest {

    @Test
    public void testDoFilterPass() throws IOException, ServletException {
        IPFilter ipFilter = new IPFilter(new LocalIPFilterSource());

        MockHttpServletRequest httpServletRequest = new MockHttpServletRequest();
        MockHttpServletResponse httpServletResponse = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        ipFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);

        assertEquals(httpServletResponse.getStatus(),HttpStatus.OK.value());
    }

    @Test
    public void testDoFilterFail() throws IOException, ServletException {
        IPFilter ipFilter = new IPFilter(new LocalIPFilterSource());

        MockHttpServletRequest httpServletRequest = new MockHttpServletRequest();
        httpServletRequest.setRemoteAddr("192.168.0.1");
        MockHttpServletResponse httpServletResponse = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        ipFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);

        assertEquals(httpServletResponse.getStatus(),HttpStatus.FORBIDDEN.value());
    }

    class LocalIPFilterSource implements IPFilterSource{

        @Override
        public String getIPFilterRegex() {
            return "127.0.0.1";
        }
    }
}
