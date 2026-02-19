package io.antmedia.statistic;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.prometheus.metrics.core.metrics.Gauge;
import io.prometheus.metrics.exporter.httpserver.HTTPServer;
import io.prometheus.metrics.instrumentation.jvm.JvmMetrics;
import io.prometheus.metrics.model.registry.PrometheusRegistry;

public class PrometheusStatsExporter implements IStatsExporter {

	private static final Logger logger = LoggerFactory.getLogger(PrometheusStatsExporter.class);

	private final int port;
	private PrometheusRegistry registry;
	private HTTPServer httpServer;
	private final Map<String, Gauge> gauges = new HashMap<>();

	public PrometheusStatsExporter(int port) {
		this.port = port;
	}

	@Override
	public void start() throws IOException {
		registry = new PrometheusRegistry();
		JvmMetrics.builder().register(registry);
		registerGauges();

		httpServer = HTTPServer.builder()
				.port(port)
				.registry(registry)
				.buildAndStart();
		logger.info("Prometheus stats exporter started on port {}", port);
	}

	@Override
	public void sendStats(JsonObject jsonObject, String type) {
		if (!INSTANCE_STATS.equals(type)) {
			return;
		}
		for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
			JsonElement value = entry.getValue();
			if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()) {
				setGauge(entry.getKey(), value.getAsDouble());
			} else if (value.isJsonObject()) {
				updateFromNested(entry.getKey(), value.getAsJsonObject());
			}
		}
	}

	@Override
	public void stop() {
		if (httpServer != null) {
			httpServer.close();
			logger.info("Prometheus HTTP server stopped");
		}
	}

	private void updateFromNested(String prefix, JsonObject nested) {
		for (Map.Entry<String, JsonElement> entry : nested.entrySet()) {
			JsonElement value = entry.getValue();
			if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()) {
				setGauge(prefix + "_" + entry.getKey(), value.getAsDouble());
			}
		}
	}

	private void setGauge(String key, double value) {
		Gauge gauge = gauges.get(key);
		if (gauge != null) {
			gauge.set(value);
		}
	}

	private void registerGauges() {
		createGauge(StatsCollector.CPU_USAGE + "_" + StatsCollector.SYSTEM_CPU_LOAD, "System CPU load percent");
		createGauge(StatsCollector.CPU_USAGE + "_" + StatsCollector.PROCESS_CPU_LOAD, "Process CPU load percent");
		createGauge(StatsCollector.CPU_USAGE + "_" + StatsCollector.PROCESS_CPU_TIME, "Process CPU time");
		createGauge(StatsCollector.CPU_USAGE + "_" + StatsCollector.SYSTEM_LOAD_AVERAGE_IN_LAST_MINUTE, "System load average last minute");

		createGauge(StatsCollector.JVM_MEMORY_USAGE + "_" + StatsCollector.MAX_MEMORY, "JVM max memory");
		createGauge(StatsCollector.JVM_MEMORY_USAGE + "_" + StatsCollector.TOTAL_MEMORY, "JVM total memory");
		createGauge(StatsCollector.JVM_MEMORY_USAGE + "_" + StatsCollector.FREE_MEMORY, "JVM free memory");
		createGauge(StatsCollector.JVM_MEMORY_USAGE + "_" + StatsCollector.IN_USE_MEMORY, "JVM in-use memory");

		createGauge(StatsCollector.SYSTEM_MEMORY_INFO + "_" + StatsCollector.VIRTUAL_MEMORY, "System virtual memory");
		createGauge(StatsCollector.SYSTEM_MEMORY_INFO + "_" + StatsCollector.TOTAL_MEMORY, "System total physical memory");
		createGauge(StatsCollector.SYSTEM_MEMORY_INFO + "_" + StatsCollector.FREE_MEMORY, "System free physical memory");
		createGauge(StatsCollector.SYSTEM_MEMORY_INFO + "_" + StatsCollector.IN_USE_MEMORY, "System in-use physical memory");
		createGauge(StatsCollector.SYSTEM_MEMORY_INFO + "_" + StatsCollector.TOTAL_SWAP_SPACE, "Total swap space");
		createGauge(StatsCollector.SYSTEM_MEMORY_INFO + "_" + StatsCollector.FREE_SWAP_SPACE, "Free swap space");
		createGauge(StatsCollector.SYSTEM_MEMORY_INFO + "_" + StatsCollector.IN_USE_SWAP_SPACE, "In-use swap space");
		createGauge(StatsCollector.SYSTEM_MEMORY_INFO + "_" + StatsCollector.AVAILABLE_MEMORY, "Available memory");

		createGauge(StatsCollector.FILE_SYSTEM_INFO + "_" + StatsCollector.USABLE_SPACE, "Usable disk space");
		createGauge(StatsCollector.FILE_SYSTEM_INFO + "_" + StatsCollector.TOTAL_SPACE, "Total disk space");
		createGauge(StatsCollector.FILE_SYSTEM_INFO + "_" + StatsCollector.FREE_SPACE, "Free disk space");
		createGauge(StatsCollector.FILE_SYSTEM_INFO + "_" + StatsCollector.IN_USE_SPACE, "In-use disk space");

		createGauge(StatsCollector.JVM_NATIVE_MEMORY_USAGE + "_" + StatsCollector.IN_USE_JVM_NATIVE_MEMORY, "JVM native in-use memory");
		createGauge(StatsCollector.JVM_NATIVE_MEMORY_USAGE + "_" + StatsCollector.MAX_JVM_NATIVE_MEMORY, "JVM native max memory");

		createGauge(StatsCollector.LOCAL_WEBRTC_LIVE_STREAMS, "WebRTC live streams");
		createGauge(StatsCollector.LOCAL_LIVE_STREAMS, "live streams");
		createGauge(StatsCollector.LOCAL_WEBRTC_VIEWERS, "WebRTC viewers");
		createGauge(StatsCollector.LOCAL_HLS_VIEWERS, "HLS viewers");
		createGauge(StatsCollector.LOCAL_DASH_VIEWERS, "DASH viewers");
		createGauge(StatsCollector.DB_AVERAGE_QUERY_TIME_MS, "DB average query time in ms");
	}

	private void createGauge(String key, String help) {
		String prometheusName = "ams_" + key.replaceAll("[^a-zA-Z0-9_]", "_").toLowerCase();
		Gauge gauge = Gauge.builder()
				.name(prometheusName)
				.help(help)
				.register(registry);
		gauges.put(key, gauge);
	}

	public PrometheusRegistry getRegistry() {
		return registry;
	}

	public Map<String, Gauge> getGauges() {
		return gauges;
	}
}
