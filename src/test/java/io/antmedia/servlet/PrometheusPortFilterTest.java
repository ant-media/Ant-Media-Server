package io.antmedia.servlet;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import io.antmedia.settings.ServerSettings;
import io.antmedia.test.UnitTestBase;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Tag("fast")
class PrometheusPortFilterTest extends UnitTestBase<PrometheusPortFilter> {

    private static final int PROMETHEUS_PORT = 9090;

    private PrometheusPortFilter filter;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain chain;

    @BeforeEach
    void setUp() throws Exception {
        ServerSettings serverSettings = new ServerSettings();
        serverSettings.setPrometheusPort(PROMETHEUS_PORT);

        filter = spy(new PrometheusPortFilter());
        doReturn(serverSettings).when(filter).getServerSettings();
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        chain = mock(FilterChain.class);
    }

    @Test
    void shouldAllowMetricsOnlyOnPrometheusPort() throws Exception {
        when(request.getLocalPort()).thenReturn(PROMETHEUS_PORT);
        when(request.getServletPath()).thenReturn(PrometheusPortFilter.METRICS_PATH);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(response, never()).sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    @Test
    void shouldRejectMetricsOnMainPort() throws Exception {
        when(request.getLocalPort()).thenReturn(5080);
        when(request.getServletPath()).thenReturn(PrometheusPortFilter.METRICS_PATH);

        filter.doFilter(request, response, chain);

        verify(response).sendError(HttpServletResponse.SC_NOT_FOUND);
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void shouldRejectOtherEndpointsOnPrometheusPort() throws Exception {
        when(request.getLocalPort()).thenReturn(PROMETHEUS_PORT);
        when(request.getServletPath()).thenReturn("/rest/v2/version");

        filter.doFilter(request, response, chain);

        verify(response).sendError(HttpServletResponse.SC_NOT_FOUND);
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void shouldAllowOtherEndpointsOnMainPort() throws Exception {
        when(request.getLocalPort()).thenReturn(5080);
        when(request.getServletPath()).thenReturn("/rest/v2/version");

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(response, never()).sendError(HttpServletResponse.SC_NOT_FOUND);
    }
}
