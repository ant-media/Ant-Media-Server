package io.antmedia.test.statistic;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.mockito.Mockito;

import com.google.gson.JsonObject;

import io.antmedia.statistic.IStatsExporter;
import io.antmedia.statistic.PrometheusStatsExporter;
import io.prometheus.metrics.core.metrics.Gauge;

public class PrometheusStatsExporterTest {

	@Test
	public void testSendStatsUpdatesOnlyInstanceStatsGauges() {
		PrometheusStatsExporter exporter = new PrometheusStatsExporter(9090);

		Gauge cpuGauge = Mockito.mock(Gauge.class);
		Gauge nestedGauge = Mockito.mock(Gauge.class);

		exporter.getGauges().put("cpu", cpuGauge);
		exporter.getGauges().put("memory_used", nestedGauge);

		JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty("cpu", 10);
		JsonObject nested = new JsonObject();
		nested.addProperty("used", 20);
		jsonObject.add("memory", nested);

		exporter.sendStats(jsonObject, IStatsExporter.WEBRTC_CLIENT_STATS);
		verify(cpuGauge, never()).set(Mockito.anyDouble());
		verify(nestedGauge, never()).set(Mockito.anyDouble());

		exporter.sendStats(jsonObject, IStatsExporter.INSTANCE_STATS);
		verify(cpuGauge).set(10.0);
		verify(nestedGauge).set(20.0);
	}
}
