package io.antmedia.test.statistic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.apache.kafka.clients.producer.MockProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.common.serialization.LongSerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.Test;

import com.google.gson.JsonObject;

import io.antmedia.statistic.IStatsExporter;
import io.antmedia.statistic.KafkaStatsExporter;

public class KafkaStatsExporterTest {

	@Test
	public void testStartAndStop() {
		KafkaStatsExporter exporter = new KafkaStatsExporter("localhost:9092");
		exporter.start();
		assertNotNull(exporter.getKafkaProducer());

		exporter.stop();

		JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty("foo", 1);
		try {
			exporter.sendStats(jsonObject, IStatsExporter.INSTANCE_STATS);
			fail("Expected IllegalStateException after producer is closed");
		} catch (IllegalStateException e) {
			// expected: producer is closed
		}
	}

	@Test
	public void testSendStatsSendsToGivenTopic() {
		KafkaStatsExporter exporter = new KafkaStatsExporter("localhost:9092");

		Producer<Long, String> kafkaProducer = new MockProducer<>(true, new LongSerializer(), new StringSerializer());
		exporter.setKafkaProducer(kafkaProducer);

		JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty("foo", 1);

		exporter.sendStats(jsonObject, IStatsExporter.INSTANCE_STATS);

		MockProducer<Long, String> mockProducer = (MockProducer<Long, String>) kafkaProducer;
		assertEquals(1, mockProducer.history().size());
		assertEquals(IStatsExporter.INSTANCE_STATS, mockProducer.history().get(0).topic());
	}
}
