package io.antmedia.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.DockerImageName;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

class PrometheusScrapeIntegrationTest {

    private static final int ANT_MEDIA_PROMETHEUS_PORT = 9090;
    private static final int PROMETHEUS_HTTP_PORT = 9090;
    private static final String PROMETHEUS_IMAGE = "prom/prometheus:v2.55.1";
    private static final String METRIC_NAME = "antmedia_streams_live";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @Test
    @Timeout(120)
    void shouldScrapeAntMediaMetrics() throws Exception {
        assertAntMediaPrometheusEndpointIsAvailable();
        Testcontainers.exposeHostPorts(ANT_MEDIA_PROMETHEUS_PORT);

        GenericContainer<?> prometheus = createPrometheusContainer();
        try {
            prometheus.start();
            String prometheusUrl = "http://" + prometheus.getHost() + ":"
                    + prometheus.getMappedPort(PROMETHEUS_HTTP_PORT);

            Awaitility.await()
                    .atMost(Duration.ofSeconds(30))
                    .pollInterval(Duration.ofSeconds(1))
                    .untilAsserted(() -> assertTargetIsHealthy(prometheusUrl));

            Awaitility.await()
                    .atMost(Duration.ofSeconds(10))
                    .pollInterval(Duration.ofSeconds(1))
                    .untilAsserted(() -> assertMetricWasScraped(prometheusUrl));
        }
        catch (Exception e) {
            throw new AssertionError("Prometheus container logs:\n" + prometheus.getLogs(), e);
        }
        finally {
            prometheus.stop();
        }
    }

    private GenericContainer<?> createPrometheusContainer() {
        String configuration = """
                global:
                  scrape_interval: 1s
                  scrape_timeout: 1s
                scrape_configs:
                  - job_name: ant-media-server
                    metrics_path: /metrics
                    static_configs:
                      - targets: ['host.testcontainers.internal:9090']
                """;

        return new GenericContainer<>(DockerImageName.parse(PROMETHEUS_IMAGE))
                .withExposedPorts(PROMETHEUS_HTTP_PORT)
                .withCopyToContainer(Transferable.of(configuration), "/etc/prometheus/prometheus.yml")
                .waitingFor(Wait.forHttp("/-/ready").forPort(PROMETHEUS_HTTP_PORT));
    }

    private void assertAntMediaPrometheusEndpointIsAvailable() throws IOException, InterruptedException {
        HttpResponse<String> response = get("http://localhost:" + ANT_MEDIA_PROMETHEUS_PORT + "/metrics");

        assertThat(response.statusCode())
                .as("Ant Media Server must already be running with its Prometheus endpoint enabled")
                .isEqualTo(200);
        assertThat(response.body()).contains(METRIC_NAME);
    }

    private void assertTargetIsHealthy(String prometheusUrl) throws IOException, InterruptedException {
        HttpResponse<String> response = get(prometheusUrl + "/api/v1/targets");

        assertThat(response.statusCode()).isEqualTo(200);
        JsonObject body = JsonParser.parseString(response.body()).getAsJsonObject();
        assertThat(body.get("status").getAsString()).isEqualTo("success");

        JsonArray activeTargets = body.getAsJsonObject("data").getAsJsonArray("activeTargets");
        assertThat(activeTargets).as("Prometheus active targets").hasSize(1);

        JsonObject target = activeTargets.get(0).getAsJsonObject();
        assertThat(target.get("health").getAsString())
                .as("Prometheus target health for %s; last error: %s",
                        target.get("scrapeUrl").getAsString(), target.get("lastError").getAsString())
                .isEqualTo("up");
    }

    private void assertMetricWasScraped(String prometheusUrl) throws IOException, InterruptedException {
        String query = URLEncoder.encode(METRIC_NAME, StandardCharsets.UTF_8);
        HttpResponse<String> response = get(prometheusUrl + "/api/v1/query?query=" + query);

        assertThat(response.statusCode()).isEqualTo(200);
        JsonObject body = JsonParser.parseString(response.body()).getAsJsonObject();
        assertThat(body.get("status").getAsString()).isEqualTo("success");

        JsonArray result = body.getAsJsonObject("data").getAsJsonArray("result");
        assertThat(result).as("Prometheus query result for %s", METRIC_NAME).isNotEmpty();
        assertThat(result.get(0).getAsJsonObject().getAsJsonObject("metric").get("job").getAsString())
                .isEqualTo("ant-media-server");
    }

    private HttpResponse<String> get(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
