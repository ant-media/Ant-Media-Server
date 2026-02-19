package io.antmedia.statistic;

import java.util.Properties;
import java.util.concurrent.ExecutionException;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.LongSerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.red5.server.Launcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class KafkaStatsExporter implements IStatsExporter {

	private static final Logger logger = LoggerFactory.getLogger(KafkaStatsExporter.class);

	private final String kafkaBrokers;
	private Producer<Long, String> kafkaProducer;
	private static final Gson gson = new Gson();

	public KafkaStatsExporter(String kafkaBrokers) {
		this.kafkaBrokers = kafkaBrokers;
	}

	@Override
	public void start() {
		Properties props = new Properties();
		props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBrokers);
		props.put(ProducerConfig.CLIENT_ID_CONFIG, Launcher.getInstanceId());
		props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, LongSerializer.class.getName());
		props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
		props.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 10000);
		kafkaProducer = new KafkaProducer<>(props);
		logger.info("Kafka stats exporter started (brokers: {})", kafkaBrokers);
	}

	@Override
	public void sendStats(JsonObject jsonObject, String type) {
		ProducerRecord<Long, String> record = new ProducerRecord<>(type, gson.toJson(jsonObject));
		try {
			kafkaProducer.send(record).get();
		} catch (ExecutionException e) {
			logger.error(ExceptionUtils.getStackTrace(e));
		} catch (InterruptedException e) {
			logger.error(ExceptionUtils.getStackTrace(e));
			Thread.currentThread().interrupt();
		}
	}

	@Override
	public void stop() {
		if (kafkaProducer != null) {
			kafkaProducer.close();
		}
	}

	public Producer<Long, String> getKafkaProducer() {
		return kafkaProducer;
	}


	public void setKafkaProducer(Producer<Long, String> kafkaProducer) {
		this.kafkaProducer = kafkaProducer;
	}
}
