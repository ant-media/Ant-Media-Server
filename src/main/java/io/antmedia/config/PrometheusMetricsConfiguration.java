package io.antmedia.config;

import org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.JvmMetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.LogbackMetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.SystemMetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.prometheus.PrometheusMetricsExportAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.micrometer.core.instrument.MeterRegistry;
import io.antmedia.settings.ServerSettings;
import org.red5.server.Launcher;

import io.antmedia.statistic.AntMediaServerMetrics;
import io.antmedia.statistic.StatsCollector;

/**
 * Adds Actuator's Prometheus registry and standard JVM/system metric binders to
 * the server's existing Spring context.
 */
@Configuration
@ImportAutoConfiguration({
        MetricsAutoConfiguration.class,
        CompositeMeterRegistryAutoConfiguration.class,
        PrometheusMetricsExportAutoConfiguration.class,
        JvmMetricsAutoConfiguration.class,
        SystemMetricsAutoConfiguration.class,
        LogbackMetricsAutoConfiguration.class
})
public class PrometheusMetricsConfiguration {

    @Bean
    AntMediaServerMetrics antMediaServerMetrics(StatsCollector statsCollector) {
        return new AntMediaServerMetrics(statsCollector);
    }

    @Bean
    MeterRegistryCustomizer<MeterRegistry> antMediaInstanceIdCustomizer() {
        String instanceIp = valueOrUnknown(ServerSettings.getGlobalHostAddress());
        String privateIp = valueOrUnknown(ServerSettings.getLocalHostAddress());
        return registry -> registry.config()
                .commonTags("antmedia_instance_id", Launcher.getInstanceId(),
                        "antmedia_instance_ip", instanceIp,
                        "antmedia_private_ip", privateIp);
    }

    private String valueOrUnknown(String value) {
        return value == null || value.isBlank() ? "unknown" : value;
    }
}
