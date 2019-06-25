package io.antmedia.test.statistic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

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

import com.google.gson.JsonObject;

import io.antmedia.rest.WebRTCClientStats;
import io.antmedia.statistic.GPUUtils;
import io.antmedia.statistic.ResourceMonitor;
import io.antmedia.statistic.GPUUtils.MemoryStatus;
import io.antmedia.webrtc.api.IWebRTCAdaptor;
import io.vertx.core.Vertx;

public class ResourceMonitorTest {

	@Test
	public void testCpuAverage() {
		ResourceMonitor monitor = new ResourceMonitor();
		monitor.setWindowSize(3);
		
		monitor.addCpuMeasurement(5);
		assertEquals(5, monitor.getCpuUsage());
		
		monitor.addCpuMeasurement(7);
		assertEquals(6, monitor.getCpuUsage());
		
		monitor.addCpuMeasurement(9);
		assertEquals(7, monitor.getCpuUsage());
		
		monitor.addCpuMeasurement(11);
		assertEquals(9, monitor.getCpuUsage());
	}
	
	
	@Test
	public void testJSObjects() {
		JsonObject jsObject = ResourceMonitor.getCPUInfoJSObject();
		assertTrue(jsObject.has(ResourceMonitor.PROCESS_CPU_TIME));
		assertTrue(jsObject.has(ResourceMonitor.SYSTEM_CPU_LOAD));
		assertTrue(jsObject.has(ResourceMonitor.PROCESS_CPU_LOAD));
		
		assertTrue(jsObject.get(ResourceMonitor.SYSTEM_CPU_LOAD).getAsInt() <= 100);
		
		
		jsObject = ResourceMonitor.getJVMMemoryInfoJSObject();
		assertTrue(jsObject.has(ResourceMonitor.IN_USE_MEMORY));
		assertTrue(jsObject.has(ResourceMonitor.FREE_MEMORY));
		assertTrue(jsObject.has(ResourceMonitor.TOTAL_MEMORY));
		assertTrue(jsObject.has(ResourceMonitor.MAX_MEMORY));
		

		jsObject = ResourceMonitor.getFileSystemInfoJSObject();
		assertTrue(jsObject.has(ResourceMonitor.IN_USE_SPACE));
		assertTrue(jsObject.has(ResourceMonitor.FREE_SPACE));
		assertTrue(jsObject.has(ResourceMonitor.TOTAL_SPACE));
		assertTrue(jsObject.has(ResourceMonitor.USABLE_SPACE));
		
		
		jsObject = ResourceMonitor.getSystemInfoJSObject();
		assertTrue(jsObject.has(ResourceMonitor.PROCESSOR_COUNT));
		assertTrue(jsObject.has(ResourceMonitor.JAVA_VERSION));
		assertTrue(jsObject.has(ResourceMonitor.OS_ARCH));
		assertTrue(jsObject.has(ResourceMonitor.OS_NAME));
				
		jsObject = ResourceMonitor.getSysteMemoryInfoJSObject();
		assertTrue(jsObject.has(ResourceMonitor.VIRTUAL_MEMORY));
		assertTrue(jsObject.has(ResourceMonitor.TOTAL_MEMORY));
		assertTrue(jsObject.has(ResourceMonitor.FREE_MEMORY));
		assertTrue(jsObject.has(ResourceMonitor.IN_USE_MEMORY));
		assertTrue(jsObject.has(ResourceMonitor.TOTAL_SWAP_SPACE));
		assertTrue(jsObject.has(ResourceMonitor.FREE_SWAP_SPACE));
		assertTrue(jsObject.has(ResourceMonitor.IN_USE_SWAP_SPACE));
		
		Launcher.setInstanceIdFilePath("target/instanceId");
		jsObject = ResourceMonitor.getSystemResourcesInfo(null);
		assertTrue(jsObject.has(ResourceMonitor.CPU_USAGE));
		assertTrue(jsObject.has(ResourceMonitor.JVM_MEMORY_USAGE));
		assertTrue(jsObject.has(ResourceMonitor.SYSTEM_INFO));
		assertTrue(jsObject.has(ResourceMonitor.SYSTEM_MEMORY_INFO));
		assertTrue(jsObject.has(ResourceMonitor.FILE_SYSTEM_INFO));
		assertTrue(jsObject.has(ResourceMonitor.GPU_USAGE_INFO));
		assertTrue(jsObject.has(ResourceMonitor.LOCAL_WEBRTC_LIVE_STREAMS));
		assertTrue(jsObject.has(ResourceMonitor.LOCAL_WEBRTC_VIEWERS));
		assertTrue(jsObject.has(ResourceMonitor.LOCAL_HLS_VIEWERS));
		
		GPUUtils gpuUtils = Mockito.mock(GPUUtils.class);
		MemoryStatus memoryStatus = Mockito.mock(MemoryStatus.class);
		Mockito.when(gpuUtils.getMemoryStatus(0)).thenReturn(memoryStatus);
		jsObject = ResourceMonitor.getGPUInfoJSObject(0, gpuUtils);
		assertTrue(jsObject.has(ResourceMonitor.GPU_DEVICE_INDEX));
		assertTrue(jsObject.has(ResourceMonitor.GPU_UTILIZATION));
		assertTrue(jsObject.has(ResourceMonitor.GPU_MEMORY_UTILIZATION));
		assertTrue(jsObject.has(ResourceMonitor.GPU_MEMORY_TOTAL));
		assertTrue(jsObject.has(ResourceMonitor.GPU_MEMORY_FREE));
		assertTrue(jsObject.has(ResourceMonitor.GPU_MEMORY_USED));
		assertTrue(jsObject.has(ResourceMonitor.GPU_DEVICE_NAME));
		
	}
	
	@Test
	public void testGetterSetter() {
		ResourceMonitor resMonitor = new ResourceMonitor();
		
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
	public void testSendInstanceKafkaStats() {
		ResourceMonitor resMonitor = Mockito.spy(new ResourceMonitor());
		
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
		
		assertEquals(ResourceMonitor.INSTANCE_STATS_TOPIC_NAME, producerRecord.getValue().topic());
	}
	
	@Test
	public void testSendWebRTCKafkaStats() {
		ResourceMonitor resMonitor = Mockito.spy(new ResourceMonitor());
		
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
		
		assertEquals(ResourceMonitor.WEBRTC_STATS_TOPIC_NAME, producerRecord.getValue().topic());
	}
	
	@Test
	public void testCreateKafka() {
		Launcher.setInstanceIdFilePath("target/instanceId");
		ResourceMonitor resMonitor = new ResourceMonitor();
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
		ResourceMonitor resMonitor = new ResourceMonitor();
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
	public void testCheckSystemResources() {
		
		ResourceMonitor monitor = Mockito.spy(new ResourceMonitor());
		
		//Cpu Limit = 70 & RAM Limit = 200 MB
		
		//check default values
		
		Mockito.when(monitor.getCpuUsage()).thenReturn(10);
		
		Mockito.when(monitor.getFreeRam()).thenReturn(500);
		
		assertEquals(true,monitor.enoughResource());
		
		//CPU value over 70
		
		Mockito.when(monitor.getCpuUsage()).thenReturn(80);
		
		Mockito.when(monitor.getFreeRam()).thenReturn(500);
		
		assertEquals(false,monitor.enoughResource());
		
		//RAM free value under 200
		
		Mockito.when(monitor.getCpuUsage()).thenReturn(10);
		
		Mockito.when(monitor.getFreeRam()).thenReturn(100);
		
		assertEquals(false,monitor.enoughResource());
		
		
		
		
	}
	
}
