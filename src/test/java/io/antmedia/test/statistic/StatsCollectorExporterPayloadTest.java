package io.antmedia.test.statistic;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

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

		IStatsExporter exporter = Mockito.mock(IStatsExporter.class);
		statsCollector.setStatsExporter(exporter);

		statsCollector.sendInstanceStats(new ConcurrentLinkedQueue<>());

		ArgumentCaptor<JsonObject> payloadCaptor = ArgumentCaptor.forClass(JsonObject.class);
		verify(exporter).sendStats(payloadCaptor.capture(), eq(IStatsExporter.INSTANCE_STATS));

		JsonObject payload = payloadCaptor.getValue();
		assertTrue(payload.has(StatsCollector.USER_EMAIL));
		assertTrue(payload.has(StatsCollector.USER_EMAIL_HASH));
		assertTrue(payload.has(StatsCollector.LICENSE_KEY_HASH));
		assertTrue(payload.get(StatsCollector.USER_EMAIL_HASH).getAsLong() > 0);
		assertTrue(payload.get(StatsCollector.LICENSE_KEY_HASH).getAsLong() > 0);
	}
}
