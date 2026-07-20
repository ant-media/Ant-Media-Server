package io.antmedia.servlet;

import java.io.IOException;
import java.io.Serial;

import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Serves the Actuator-configured Prometheus registry from the existing container. */
public class PrometheusMetricsServlet extends HttpServlet {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final String CONTENT_TYPE = "text/plain; version=0.0.4; charset=utf-8";

    private transient PrometheusMeterRegistry meterRegistry;

    @Override
    public void init() throws ServletException {
        WebApplicationContext context = WebApplicationContextUtils
                .getRequiredWebApplicationContext(getServletContext());
        meterRegistry = context.getBean(PrometheusMeterRegistry.class);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(CONTENT_TYPE);
        response.getWriter().write(meterRegistry.scrape());
    }
}
