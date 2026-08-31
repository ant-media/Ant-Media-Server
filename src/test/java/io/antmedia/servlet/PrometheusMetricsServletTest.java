package io.antmedia.servlet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.StaticWebApplicationContext;

import io.antmedia.settings.ServerSettings;
import io.antmedia.test.UnitTestBase;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Tag("fast")
class PrometheusMetricsServletTest extends UnitTestBase<PrometheusMetricsServlet> {

    @Test
    void shouldWritePrometheusMetrics() throws Exception {
        PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        registry.counter("antmedia_test_requests").increment();

        PrometheusMetricsServlet servlet = initializeServlet(registry);

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        StringWriter body = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(body));

        servlet.doGet(request, response);

        verify(response).setStatus(HttpServletResponse.SC_OK);
        verify(response).setContentType("text/plain; version=0.0.4; charset=utf-8");
        assertThat(body.toString()).contains("antmedia_test_requests_total 1.0");
    }

    @Test
    void shouldHandleExceptionWhileGettingResponseWriter() throws Exception {
        PrometheusMetricsServlet servlet = initializeServlet(
                new PrometheusMeterRegistry(PrometheusConfig.DEFAULT));

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getWriter()).thenThrow(new IOException("Writer is unavailable"));

        assertThatCode(() -> servlet.doGet(request, response)).doesNotThrowAnyException();

        verify(response, never()).setStatus(HttpServletResponse.SC_OK);
        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        verify(response).setContentType("text/plain; version=0.0.4; charset=utf-8");
    }

    @Test
    void shouldReturnNotFoundWhenPrometheusIsDisabled() throws Exception {
        ServerSettings serverSettings = new ServerSettings();
        serverSettings.setPrometheusEnabled(false);
        PrometheusMetricsServlet servlet = initializeServlet(
                new PrometheusMeterRegistry(PrometheusConfig.DEFAULT), serverSettings);

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        servlet.doGet(request, response);

        verify(response).setStatus(HttpServletResponse.SC_NOT_FOUND);
        verify(response, never()).getWriter();
    }

    private PrometheusMetricsServlet initializeServlet(PrometheusMeterRegistry registry) throws Exception {
        ServerSettings serverSettings = new ServerSettings();
        serverSettings.setPrometheusEnabled(true);
        return initializeServlet(registry, serverSettings);
    }

    private PrometheusMetricsServlet initializeServlet(PrometheusMeterRegistry registry,
            ServerSettings serverSettings) throws Exception {
        StaticWebApplicationContext applicationContext = new StaticWebApplicationContext();
        applicationContext.getBeanFactory().registerSingleton("prometheusMeterRegistry", registry);
        applicationContext.getBeanFactory().registerSingleton(ServerSettings.BEAN_NAME, serverSettings);

        MockServletContext servletContext = new MockServletContext();
        servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, applicationContext);

        PrometheusMetricsServlet servlet = new PrometheusMetricsServlet();
        servlet.init(new MockServletConfig(servletContext));
        return servlet;
    }
}
