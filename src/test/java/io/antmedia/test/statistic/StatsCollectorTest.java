package io.antmedia.test.statistic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.awaitility.Awaitility;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.red5.server.Launcher;
import org.red5.server.api.IContext;
import org.red5.server.api.scope.IScope;
import org.springframework.context.ApplicationContext;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.antmedia.rest.WebRTCClientStats;
import io.antmedia.statistic.GPUUtils;
import io.antmedia.statistic.StatsCollector;
import io.antmedia.statistic.GPUUtils.MemoryStatus;
import io.antmedia.webrtc.api.IWebRTCAdaptor;
import io.vertx.core.Vertx;

public class StatsCollectorTest {

	@Test
	public void testCpuAverage() {
		StatsCollector monitor = new StatsCollector();
		monitor.setWindowSize(3);
		
		monitor.addCpuMeasurement(5);
		assertEquals(5, monitor.getCpuLoad());
		
		monitor.addCpuMeasurement(7);
		assertEquals(6, monitor.getCpuLoad());
		
		monitor.addCpuMeasurement(9);
		assertEquals(7, monitor.getCpuLoad());
		
		monitor.addCpuMeasurement(11);
		assertEquals(9, monitor.getCpuLoad());
	}
	
	
	@Test
	public void testThreadDump() {
		ThreadInfo[] threadDump = StatsCollector.getThreadDump();
		assertNotNull(threadDump);
		
		JsonArray threadDumpJSON = StatsCollector.getThreadDumpJSON();
		assertNotNull(threadDumpJSON);
		
	}
	
	@Test
	public void testJSObjects() {
		JsonObject jsObject = StatsCollector.getCPUInfoJSObject();
		assertTrue(jsObject.has(StatsCollector.PROCESS_CPU_TIME));
		assertTrue(jsObject.has(StatsCollector.SYSTEM_CPU_LOAD));
		assertTrue(jsObject.has(StatsCollector.PROCESS_CPU_LOAD));
		
		assertTrue(jsObject.get(StatsCollector.SYSTEM_CPU_LOAD).getAsInt() <= 100);
		
		
		jsObject = StatsCollector.getJVMMemoryInfoJSObject();
		assertTrue(jsObject.has(StatsCollector.IN_USE_MEMORY));
		assertTrue(jsObject.has(StatsCollector.FREE_MEMORY));
		assertTrue(jsObject.has(StatsCollector.TOTAL_MEMORY));
		assertTrue(jsObject.has(StatsCollector.MAX_MEMORY));
		

		jsObject = StatsCollector.getFileSystemInfoJSObject();
		assertTrue(jsObject.has(StatsCollector.IN_USE_SPACE));
		assertTrue(jsObject.has(StatsCollector.FREE_SPACE));
		assertTrue(jsObject.has(StatsCollector.TOTAL_SPACE));
		assertTrue(jsObject.has(StatsCollector.USABLE_SPACE));
		
		
		jsObject = StatsCollector.getSystemInfoJSObject();
		assertTrue(jsObject.has(StatsCollector.PROCESSOR_COUNT));
		assertTrue(jsObject.has(StatsCollector.JAVA_VERSION));
		assertTrue(jsObject.has(StatsCollector.OS_ARCH));
		assertTrue(jsObject.has(StatsCollector.OS_NAME));
				
		jsObject = StatsCollector.getSysteMemoryInfoJSObject();
		assertTrue(jsObject.has(StatsCollector.VIRTUAL_MEMORY));
		assertTrue(jsObject.has(StatsCollector.TOTAL_MEMORY));
		assertTrue(jsObject.has(StatsCollector.FREE_MEMORY));
		assertTrue(jsObject.has(StatsCollector.IN_USE_MEMORY));
		assertTrue(jsObject.has(StatsCollector.TOTAL_SWAP_SPACE));
		assertTrue(jsObject.has(StatsCollector.FREE_SWAP_SPACE));
		assertTrue(jsObject.has(StatsCollector.IN_USE_SWAP_SPACE));
		
		Launcher.setInstanceIdFilePath("target/instanceId");
		jsObject = StatsCollector.getSystemResourcesInfo(null);
		assertTrue(jsObject.has(StatsCollector.CPU_USAGE));
		assertTrue(jsObject.has(StatsCollector.JVM_MEMORY_USAGE));
		assertTrue(jsObject.has(StatsCollector.SYSTEM_INFO));
		assertTrue(jsObject.has(StatsCollector.SYSTEM_MEMORY_INFO));
		assertTrue(jsObject.has(StatsCollector.FILE_SYSTEM_INFO));
		assertTrue(jsObject.has(StatsCollector.GPU_USAGE_INFO));
		assertTrue(jsObject.has(StatsCollector.LOCAL_WEBRTC_LIVE_STREAMS));
		assertTrue(jsObject.has(StatsCollector.LOCAL_WEBRTC_VIEWERS));
		assertTrue(jsObject.has(StatsCollector.LOCAL_HLS_VIEWERS));
		
		GPUUtils gpuUtils = Mockito.mock(GPUUtils.class);
		MemoryStatus memoryStatus = Mockito.mock(MemoryStatus.class);
		Mockito.when(gpuUtils.getMemoryStatus(0)).thenReturn(memoryStatus);
		jsObject = StatsCollector.getGPUInfoJSObject(0, gpuUtils);
		assertTrue(jsObject.has(StatsCollector.GPU_DEVICE_INDEX));
		assertTrue(jsObject.has(StatsCollector.GPU_UTILIZATION));
		assertTrue(jsObject.has(StatsCollector.GPU_MEMORY_UTILIZATION));
		assertTrue(jsObject.has(StatsCollector.GPU_MEMORY_TOTAL));
		assertTrue(jsObject.has(StatsCollector.GPU_MEMORY_FREE));
		assertTrue(jsObject.has(StatsCollector.GPU_MEMORY_USED));
		assertTrue(jsObject.has(StatsCollector.GPU_DEVICE_NAME));
		
		
		jsObject = StatsCollector.getThreadInfoJSONObject();
		assertTrue(jsObject.has(StatsCollector.DEAD_LOCKED_THREAD));
		assertTrue(jsObject.has(StatsCollector.THREAD_COUNT));
		assertTrue(jsObject.has(StatsCollector.THREAD_PEEK_COUNT));

	}
	
	@Test
	public void testGetterSetter() {
		StatsCollector resMonitor = new StatsCollector();
		
		assertEquals(70, resMonitor.getCpuLimit());
		resMonitor.setCpuLimit(45);
		assertEquals(45, resMonitor.getCpuLimit());
		
		resMonitor.setCpuLimit(150);
		assertEquals(100, resMonitor.getCpuLimit());
		
		resMonitor.setCpuLimit(9);
		assertEquals(10, resMonitor.getCpuLimit());
		
		Vertx vertx = Vertx.vertx();
		resMonitor.setVertx(vertx);
		
		assertEquals(vertx, resMonitor.getVertx());
		
		assertEquals(15000, resMonitor.getStaticSendPeriod());
		resMonitor.setStaticSendPeriod(9000);
		assertEquals(9000, resMonitor.getStaticSendPeriod());
		
		String kafkaBroker = "This is kafka broker";
		assertNull(resMonitor.getKafkaBrokers());
		resMonitor.setKafkaBrokers(kafkaBroker);
		assertEquals(kafkaBroker, resMonitor.getKafkaBrokers());
		
	}
	
	@Test
	public void testHeartbeat() {

		StatsCollector resMonitor = Mockito.spy(new StatsCollector());
		//check default value
		assertEquals(300000, resMonitor.getHeartbeatPeriodMs());
		
		resMonitor.setHeartbeatPeriodMs(3000);
		resMonitor.setVertx(Vertx.vertx());
		Launcher.setInstanceIdFilePath("target/instanceId");
		resMonitor.start();
		
		assertTrue(resMonitor.isHeartBeatEnabled());
		
		
		Awaitility.await().pollDelay(5,TimeUnit.SECONDS).atMost(20, TimeUnit.SECONDS)
		.pollInterval(1, TimeUnit.SECONDS)
		.until(()->{
			return true;
		});
		
		Mockito.verify(resMonitor, Mockito.times(1)).startAnalytic(Launcher.getVersion(), Launcher.getVersionType());
		
		Mockito.verify(resMonitor, Mockito.times(1)).notifyShutDown(Launcher.getVersion(), Launcher.getVersionType());
		
		Mockito.verify(resMonitor, Mockito.times(1)).startHeartBeats(Launcher.getVersion(), Launcher.getVersionType(), 3000);
		
		resMonitor.cancelHeartBeat();
		
		
		
		resMonitor.setHeartBeatEnabled(false);
		resMonitor.start();
		assertFalse(resMonitor.isHeartBeatEnabled());
		Mockito.verify(resMonitor, Mockito.times(1)).startAnalytic(Launcher.getVersion(), Launcher.getVersionType());
		
		Mockito.verify(resMonitor, Mockito.times(1)).notifyShutDown(Launcher.getVersion(), Launcher.getVersionType());
		
		Mockito.verify(resMonitor, Mockito.times(1)).startHeartBeats(Launcher.getVersion(), Launcher.getVersionType(), 3000);
		
		resMonitor.cancelHeartBeat();
		
	}
	
	@Test
	public void testSendInstanceKafkaStats() {
		StatsCollector resMonitor = Mockito.spy(new StatsCollector());
		
		Producer<Long, String> kafkaProducer = Mockito.mock(Producer.class);
		
		Future<RecordMetadata> futureMetdata = Mockito.mock(Future.class);
		
		
		//ProducerRecord<Long, String> record = new ProducerRecord<>(topicName,
		//		gson.toJson(jsonElement));
		
		ArgumentCaptor<ProducerRecord<Long, String>> producerRecord = ArgumentCaptor.forClass(ProducerRecord.class);
		
		Mockito.when(kafkaProducer.send(any())).thenReturn(futureMetdata);
		
		Launcher.setInstanceIdFilePath("target/instanceId");
		resMonitor.setKafkaProducer(kafkaProducer);
		resMonitor.sendInstanceStats(null);
		
		
		verify(kafkaProducer).send(producerRecord.capture());
		
		assertEquals(StatsCollector.INSTANCE_STATS_TOPIC_NAME, producerRecord.getValue().topic());
	}
	
	@Test
	public void testSendWebRTCKafkaStats() {
		StatsCollector resMonitor = Mockito.spy(new StatsCollector());
		
		Producer<Long, String> kafkaProducer = Mockito.mock(Producer.class);
		
		Future<RecordMetadata> futureMetdata = Mockito.mock(Future.class);
		
		
		//ProducerRecord<Long, String> record = new ProducerRecord<>(topicName,
		//		gson.toJson(jsonElement));
		
		ArgumentCaptor<ProducerRecord<Long, String>> producerRecord = ArgumentCaptor.forClass(ProducerRecord.class);
		
		Mockito.when(kafkaProducer.send(any())).thenReturn(futureMetdata);
		
		Launcher.setInstanceIdFilePath("target/instanceId");
		resMonitor.setKafkaProducer(kafkaProducer);
		
		List<WebRTCClientStats> webRTCClientStatList = new ArrayList<>();
		WebRTCClientStats stats = new WebRTCClientStats(100, 50, 40, 20, 60, 444, 9393838);
		webRTCClientStatList.add(stats);
		resMonitor.sendWebRTCClientStats2Kafka(webRTCClientStatList, "stream1");
		
		
		verify(kafkaProducer).send(producerRecord.capture());
		
		assertEquals(StatsCollector.WEBRTC_STATS_TOPIC_NAME, producerRecord.getValue().topic());
	}
	
	@Test
	public void testCreateKafka() {
		Launcher.setInstanceIdFilePath("target/instanceId");
		StatsCollector resMonitor = new StatsCollector();
		try {
			Producer<Long, String> kafkaProducer = resMonitor.createKafkaProducer();
			//it should throw exception
			fail("it shold throw exception");
		}
		catch (NullPointerException e) {
			
		}
		
		resMonitor.setKafkaBrokers("localhost:9092");
		Producer<Long, String> kafkaProducer = resMonitor.createKafkaProducer();
		assertNotNull(kafkaProducer);
		kafkaProducer.close();
	}
	
	@Test
	public void testCollectAndSendWebRTCStats() {
		Launcher.setInstanceIdFilePath("target/instanceId");
		StatsCollector resMonitor = new StatsCollector();
		Producer<Long, String> kafkaProducer = Mockito.mock(Producer.class);
		resMonitor.setKafkaProducer(kafkaProducer);
		
		Future<RecordMetadata> futureMetdata = Mockito.mock(Future.class);
		
		Mockito.when(kafkaProducer.send(any())).thenReturn(futureMetdata);
		
		ConcurrentLinkedQueue<IScope> scopes = new ConcurrentLinkedQueue<>();
		IScope scope = Mockito.mock(IScope.class);
		IContext context = Mockito.mock(IContext.class);
		Mockito.when(scope.getContext()).thenReturn(context);
		
		ApplicationContext appContext = Mockito.mock(ApplicationContext.class);
		Mockito.when(context.getApplicationContext()).thenReturn(appContext);
		Mockito.when(appContext.containsBean(any())).thenReturn(true);
	
		IWebRTCAdaptor webRTCAdaptor = Mockito.mock(IWebRTCAdaptor.class);
		Mockito.when(appContext.getBean(Mockito.anyString())).thenReturn(webRTCAdaptor);
		
		Set<String> streams = new HashSet<String>(); 
		streams.add("stream1");
		Mockito.when(webRTCAdaptor.getStreams()).thenReturn(streams);
		List<WebRTCClientStats> webRTCClientStatList = new ArrayList<>();
		WebRTCClientStats stats = new WebRTCClientStats(100, 50, 40, 20, 60, 444, 9393838);
		webRTCClientStatList.add(stats);
		 
		Mockito.when(webRTCAdaptor.getWebRTCClientStats(any())).thenReturn(webRTCClientStatList);
		
		scopes.add(scope);
		
		resMonitor.setScopes(scopes);
		resMonitor.setVertx(Vertx.vertx());
		resMonitor.setStaticSendPeriod(5000);
	
		resMonitor.collectAndSendWebRTCClientsStats();
		
		verify(kafkaProducer, Mockito.times(1)).send(Mockito.any());		
		
	}
	
	@Test
	public void testServertime() {
		JsonObject serverTime = StatsCollector.getServerTime();
		assertTrue(serverTime.has(StatsCollector.START_TIME));
		assertTrue(serverTime.has(StatsCollector.UP_TIME));
		
		long startTime = serverTime.get(StatsCollector.START_TIME).getAsLong();
		long upTime =  serverTime.get(StatsCollector.UP_TIME).getAsLong();
		
		assertEquals(ManagementFactory.getRuntimeMXBean().getUptime(), upTime, 100);
		assertEquals(ManagementFactory.getRuntimeMXBean().getStartTime(), startTime, 100);
		
		assertTrue(startTime > 0);
		assertTrue(upTime > 0);	
	}
	
	@Test
	public void testCheckSystemResources() {
		
		StatsCollector monitor = Mockito.spy(new StatsCollector());
		
		//Cpu Limit = 70 & RAM Limit = 200 MB
		
		//check default values
		
		Mockito.when(monitor.getCpuLoad()).thenReturn(10);
		
		Mockito.when(monitor.getFreeRam()).thenReturn(500);
		
		assertEquals(true,monitor.enoughResource());
		
		//CPU value over 70
		
		monitor.setCpuLoad(80);
		
		Mockito.when(monitor.getFreeRam()).thenReturn(500);
		
		assertEquals(false, monitor.enoughResource());
		
		//RAM free value under 200
		
		Mockito.when(monitor.getCpuLoad()).thenReturn(10);
		
		Mockito.when(monitor.getFreeRam()).thenReturn(100);
		
		assertEquals(false,monitor.enoughResource());
		
		
		
		
	}
	
}
