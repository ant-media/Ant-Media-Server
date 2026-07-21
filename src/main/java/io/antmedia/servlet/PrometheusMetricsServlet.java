package io.antmedia.servlet;

import java.io.IOException;
import java.io.Serial;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import io.antmedia.settings.ServerSettings;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Serves the Actuator-configured Prometheus registry from the existing container. */
public class PrometheusMetricsServlet extends HttpServlet {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final Logger logger = LoggerFactory.getLogger(PrometheusMetricsServlet.class);
    private static final String CONTENT_TYPE = "text/plain; version=0.0.4; charset=utf-8";

    private transient PrometheusMeterRegistry meterRegistry;
    private transient ServerSettings serverSettings;

    @Override
    public void init() throws ServletException {
        WebApplicationContext context = WebApplicationContextUtils
                .getRequiredWebApplicationContext(getServletContext());
        meterRegistry = context.getBean(PrometheusMeterRegistry.class);
        serverSettings = context.getBean(ServerSettings.BEAN_NAME, ServerSettings.class);
        if (serverSettings.isPrometheusEnabled()) {
            logger.info("Prometheus metrics are available at http://localhost:{}/metrics",
                    serverSettings.getPrometheusPort());
        } else {
            logger.info("Prometheus metrics are disabled.");
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!serverSettings.isPrometheusEnabled()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        response.setContentType(CONTENT_TYPE);
        try {
            var writer = response.getWriter();
            response.setStatus(HttpServletResponse.SC_OK);
            writer.write(meterRegistry.scrape());
        } catch (IOException e) {
            logger.error("Could not write Prometheus metrics response", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
