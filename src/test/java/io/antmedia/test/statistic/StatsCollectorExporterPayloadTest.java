package io.antmedia.test.statistic;

import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.gson.JsonObject;

import io.antmedia.statistic.IStatsExporter;
import io.antmedia.statistic.StatsCollector;
import io.vertx.core.Vertx;

public class StatsCollectorExporterPayloadTest {

	private static Vertx vertx;
	private static Vertx webRTCVertx;

	@BeforeClass
	public static void beforeClass() {
		vertx = Vertx.vertx();
		webRTCVertx = Vertx.vertx();
	}

	@AfterClass
	public static void afterClass() {
		vertx.close();
		webRTCVertx.close();
	}

	@Test
	public void testSendInstanceStatsIncludesEmailAndHashes() throws Exception {
		StatsCollector statsCollector = new StatsCollector();
		statsCollector.setVertx(vertx);
		statsCollector.setWebRTCVertx(webRTCVertx);
		statsCollector.setUserEmail("user@test.com");

		Field licenceKeyField = StatsCollector.class.getDeclaredField("licenceKey");
		licenceKeyField.setAccessible(true);
		licenceKeyField.set(statsCollector, "my-license-key");

		CapturingStatsExporter exporter = new CapturingStatsExporter();
		statsCollector.setStatsExporter(exporter);

		statsCollector.sendInstanceStats(new ConcurrentLinkedQueue<>());

		JsonObject payload = exporter.payload;
		assertTrue(payload.has(StatsCollector.USER_EMAIL));
		assertTrue(payload.has(StatsCollector.USER_EMAIL_HASH));
		assertTrue(payload.has(StatsCollector.LICENSE_KEY_HASH));
		assertTrue(payload.get(StatsCollector.USER_EMAIL_HASH).getAsLong() > 0);
		assertTrue(payload.get(StatsCollector.LICENSE_KEY_HASH).getAsLong() > 0);
		assertTrue(IStatsExporter.INSTANCE_STATS.equals(exporter.type));
	}

	private static class CapturingStatsExporter implements IStatsExporter {
		private JsonObject payload;
		private String type;

		@Override
		public void start() {
			// no-op
		}

		@Override
		public void sendStats(JsonObject jsonObject, String type) {
			this.payload = jsonObject;
			this.type = type;
		}

		@Override
		public void stop() {
			// no-op
		}
	}
}
