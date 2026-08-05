package io.antmedia.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import io.antmedia.test.UnitTestBase;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import io.antmedia.statistic.StatsCollector;
import org.red5.server.Launcher;

@Tag("fast")
class PrometheusMetricsConfigurationTest extends UnitTestBase<PrometheusMetricsConfiguration> {

    @Test
    void shouldConfigurePrometheusAndJvmMetrics() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(StatsCollector.class, () -> mock(StatsCollector.class));
            context.register(PrometheusMetricsConfiguration.class);
            context.refresh();
            PrometheusMeterRegistry registry = context.getBean(PrometheusMeterRegistry.class);

            assertThat(registry.find("jvm.memory.used").meters()).isNotEmpty();
            assertThat(registry.find("system.cpu.count").meters()).isNotEmpty();
            assertThat(registry.find("jvm.memory.used")
                    .tag("antmedia_instance_id", Launcher.getInstanceId()).meters()).isNotEmpty();
            assertThat(registry.find("jvm.memory.used").meters())
                    .allSatisfy(meter -> {
                        assertThat(meter.getId().getTag("antmedia_instance_ip")).isNotBlank();
                        assertThat(meter.getId().getTag("antmedia_private_ip")).isNotBlank();
                    });
        }
    }
}
