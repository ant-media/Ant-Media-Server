package io.antmedia.test.statistic;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.Future;

import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.google.gson.JsonObject;

import io.antmedia.statistic.IStatsExporter;
import io.antmedia.statistic.KafkaStatsExporter;

public class KafkaStatsExporterTest {

	@Test
	public void testSendStatsSendsToGivenTopic() {
		KafkaStatsExporter exporter = new KafkaStatsExporter("localhost:9092");

		Producer<Long, String> kafkaProducer = Mockito.mock(Producer.class);
		Future<RecordMetadata> futureMetadata = Mockito.mock(Future.class);
		when(kafkaProducer.send(any())).thenReturn(futureMetadata);
		exporter.setKafkaProducer(kafkaProducer);

		JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty("foo", 1);

		exporter.sendStats(jsonObject, IStatsExporter.INSTANCE_STATS);

		ArgumentCaptor<ProducerRecord<Long, String>> producerRecordCaptor = ArgumentCaptor.forClass(ProducerRecord.class);
		verify(kafkaProducer).send(producerRecordCaptor.capture());
		assertEquals(IStatsExporter.INSTANCE_STATS, producerRecordCaptor.getValue().topic());
	}
}
