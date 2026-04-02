package io.antmedia.test.statistic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.ServerSocket;

import org.junit.Test;

import com.google.gson.JsonObject;

import io.antmedia.statistic.IStatsExporter;
import io.antmedia.statistic.PrometheusStatsExporter;
import io.prometheus.metrics.core.metrics.Gauge;
import io.prometheus.metrics.model.registry.PrometheusRegistry;

public class PrometheusStatsExporterTest {

	private int getAvailablePort() throws IOException {
		try (ServerSocket socket = new ServerSocket(0)) {
			return socket.getLocalPort();
		}
	}

	@Test
	public void testStartAndStop() throws Exception {
		int port = getAvailablePort();
		PrometheusStatsExporter exporter = new PrometheusStatsExporter("localhost:" + port, "testjob", "node-1", "user@example.com");

		exporter.start();
		assertNotNull(exporter.getRegistry());

		exporter.stop();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testSendStatsUpdatesOnlyInstanceStatsGauges() throws Exception {
		PrometheusStatsExporter exporter = new PrometheusStatsExporter("localhost:9091", "testjob", "node-1", "user@example.com");

		PrometheusRegistry registry = new PrometheusRegistry();
		Field registryField = PrometheusStatsExporter.class.getDeclaredField("registry");
		registryField.setAccessible(true);
		registryField.set(exporter, registry);

		Gauge cpuGauge = Gauge.builder().name("test_cpu").help("cpu").register(registry);
		Gauge nestedGauge = Gauge.builder().name("test_memory_used").help("memory used").register(registry);

		exporter.getGauges().put("cpu", cpuGauge);
		exporter.getGauges().put("memory_used", nestedGauge);

		JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty("cpu", 10);
		JsonObject nested = new JsonObject();
		nested.addProperty("used", 20);
		jsonObject.add("memory", nested);

		exporter.sendStats(jsonObject, IStatsExporter.WEBRTC_CLIENT_STATS);
		assertEquals(0.0, cpuGauge.get(), 0.001);
		assertEquals(0.0, nestedGauge.get(), 0.001);

		exporter.sendStats(jsonObject, IStatsExporter.INSTANCE_STATS);
		assertEquals(10.0, cpuGauge.get(), 0.001);
		assertEquals(20.0, nestedGauge.get(), 0.001);
	}
}
