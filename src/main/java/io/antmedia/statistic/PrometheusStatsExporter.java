package io.antmedia.statistic;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.prometheus.metrics.core.metrics.Gauge;
import io.prometheus.metrics.exporter.pushgateway.PushGateway;
import io.prometheus.metrics.instrumentation.jvm.JvmMetrics;
import io.prometheus.metrics.model.registry.PrometheusRegistry;

public class PrometheusStatsExporter implements IStatsExporter {

	private static final Logger logger = LoggerFactory.getLogger(PrometheusStatsExporter.class);

	private final String pushGatewayAddress;
	private static final String job = "AntMedia";
	private final String instance;
	private final String userEmail;

	private PrometheusRegistry registry;
	private PushGateway pushGateway;
	private final Map<String, Gauge> gauges = new HashMap<>();

	public PrometheusStatsExporter(String pushGatewayAddress, String instance, String userEmail) {

		this.pushGatewayAddress = (pushGatewayAddress == null || pushGatewayAddress.isEmpty()) ? "localhost:9091"
				: pushGatewayAddress;
		this.instance = instance;
		this.userEmail = userEmail;
	}

	@Override
	public void start() throws IOException {
		registry = new PrometheusRegistry();
		JvmMetrics.builder().register(registry);
		registerGauges();

		PushGateway.Builder builder = PushGateway.builder()
				.address(pushGatewayAddress)
				.job(job)
				.registry(registry);

		if (instance != null && !instance.isEmpty()) {
			builder = builder.groupingKey("instance", instance);
		}
		if (userEmail != null && !userEmail.isEmpty()) {
			builder = builder.groupingKey("user", userEmail);
		}

		pushGateway = builder.build();
		logger.info("Prometheus PushGateway exporter initialized for job={} address={} instance={} user={}",
				job, pushGatewayAddress, instance, userEmail);
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

		if (pushGateway != null) {
			try {
				pushGateway.pushAdd();
			} catch (IOException e) {
				logger.warn("Failed to push metrics to PushGateway at {}: {}", pushGatewayAddress, e.getMessage());
			}
		}
	}

	@Override
	public void stop() {
		if (pushGateway != null) {
			try {
				pushGateway.delete();
				logger.info("Deleted metrics for job={} instance={} user={} from PushGateway at {}",
						job, instance, userEmail, pushGatewayAddress);
			} catch (IOException e) {
				logger.warn("Failed to delete metrics from PushGateway at {}: {}", pushGatewayAddress, e.getMessage());
			}
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
		createGauge(StatsCollector.USER_EMAIL_HASH, "Hashed user email value");
		createGauge(StatsCollector.LICENSE_KEY_HASH, "Hashed license key value");
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
