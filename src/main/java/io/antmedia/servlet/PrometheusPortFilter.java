package io.antmedia.servlet;

import java.io.IOException;

import io.antmedia.filter.AbstractFilter;
import io.antmedia.settings.ServerSettings;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Restricts the Prometheus connector to metrics and metrics to that connector. */
public class PrometheusPortFilter extends AbstractFilter {

    static final String METRICS_PATH = "/metrics";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        ServerSettings serverSettings = getServerSettings();

        if (serverSettings == null) {
            httpResponse.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            return;
        }

        boolean prometheusPort = request.getLocalPort() == serverSettings.getPrometheusPort();
        boolean metricsRequest = METRICS_PATH.equals(httpRequest.getServletPath());
        if (prometheusPort != metricsRequest) {
            httpResponse.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        chain.doFilter(request, response);
    }
}
