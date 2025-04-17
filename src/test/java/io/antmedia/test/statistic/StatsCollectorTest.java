package io.antmedia.test.statistic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.awaitility.Awaitility;
import org.bytedeco.ffmpeg.avutil.AVRational;
import org.bytedeco.javacpp.Pointer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.red5.server.Launcher;
import org.red5.server.api.IContext;
import org.red5.server.api.IServer;
import org.red5.server.api.scope.IScope;
import org.springframework.context.ApplicationContext;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.SystemUtils;
import io.antmedia.console.AdminApplication;
import io.antmedia.console.datastore.AbstractConsoleDataStore;
import io.antmedia.console.datastore.ConsoleDataStoreFactory;
import io.antmedia.console.rest.CommonRestService;
import io.antmedia.datastore.db.types.User;
import io.antmedia.muxer.IAntMediaStreamHandler;
import io.antmedia.rest.WebRTCClientStats;
import io.antmedia.datastore.db.types.UserType;
import io.antmedia.settings.ServerSettings;
import io.antmedia.statistic.GPUUtils;
import io.antmedia.statistic.GPUUtils.MemoryStatus;
import io.antmedia.statistic.StatsCollector;
import io.antmedia.webrtc.api.IWebRTCAdaptor;
import io.antmedia.websocket.WebSocketCommunityHandler;
import io.vertx.core.Vertx;

public class StatsCollectorTest {
	
	static {
		//just init javacpp
		AVRational rat = new AVRational();
		
	}
	
	static Vertx vertx;
	static Vertx webRTCVertx;
	
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
	public void testCpuAverage() {
		StatsCollector monitor = new StatsCollector();
		monitor.setWindowSize(3);
		
		monitor.addCpuMeasurement(5, 3);
		assertEquals(5, monitor.getCpuLoad());
		assertEquals(3, StatsCollector.getProcessCpuLoad());
		
		monitor.addCpuMeasurement(7, 5);
		assertEquals(6, monitor.getCpuLoad());
		assertEquals(4, StatsCollector.getProcessCpuLoad());

		
		monitor.addCpuMeasurement(9, 7);
		assertEquals(7, monitor.getCpuLoad());
		assertEquals(5, StatsCollector.getProcessCpuLoad());

		
		monitor.addCpuMeasurement(11, 9);
		assertEquals(9, monitor.getCpuLoad());
		assertEquals(7, StatsCollector.getProcessCpuLoad());

	}
	
	@Test
	public void testClassCastException() 
	{		
		ApplicationContext appContext = Mockito.mock(ApplicationContext.class);
		
		Mockito.when(appContext.containsBean(AntMediaApplicationAdapter.BEAN_NAME)).thenReturn(true);
		
		Mockito.when(appContext.getBean(AntMediaApplicationAdapter.BEAN_NAME)).thenReturn(new AdminApplication());
		
		assertNull(StatsCollector.getAppAdaptor(appContext));
		
		Mockito.when(appContext.getBean(AntMediaApplicationAdapter.BEAN_NAME)).thenReturn(new AntMediaApplicationAdapter());
		
		assertNotNull(StatsCollector.getAppAdaptor(appContext));
		
		Mockito.when(appContext.containsBean(AntMediaApplicationAdapter.BEAN_NAME)).thenReturn(false);
		assertNull(StatsCollector.getAppAdaptor(appContext));
	}
	
	
	@Test
	public void testThreadDump() {
		ThreadInfo[] threadDump = StatsCollector.getThreadDump();
		assertNotNull(threadDump);
		
		JsonArray threadDumpJSON = StatsCollector.getThreadDumpJSON();
		assertNotNull(threadDumpJSON);
		
	}
	
	@Test
	public void testGetUserEmail() 
	{
		ConcurrentLinkedQueue<IScope> scopes = new ConcurrentLinkedQueue<>();
		IScope scope = Mockito.mock(IScope.class);
		IContext context = Mockito.mock(IContext.class);
		Mockito.when(scope.getContext()).thenReturn(context);
		AdminApplication adminApp = Mockito.mock(AdminApplication.class);
		scopes.add(scope);
		
		ApplicationContext appContext = Mockito.mock(ApplicationContext.class);
		Mockito.when(context.getApplicationContext()).thenReturn(appContext);
		
		Mockito.when(appContext.containsBean(Mockito.anyString())).thenReturn(true);
		Mockito.when(appContext.getBean(Mockito.anyString())).thenReturn(adminApp);
		
		ConsoleDataStoreFactory dtFactory = Mockito.mock(ConsoleDataStoreFactory.class);
		AbstractConsoleDataStore dataStore = Mockito.mock(AbstractConsoleDataStore.class);
		
		Mockito.when(dtFactory.getDataStore()).thenReturn(dataStore);
		Mockito.when(adminApp.getDataStoreFactory()).thenReturn(dtFactory);
		
		List<User> userList = new ArrayList<>();
		String userEmail = "test@antmedia.io";
		User user = new User(userEmail, null, UserType.ADMIN, CommonRestService.SCOPE_SYSTEM, null);
		userList.add(user);
		Mockito.when(dataStore.getUserList()).thenReturn(userList);
		
		StatsCollector statsCollector = new StatsCollector();
		statsCollector.setScopes(scopes);
		
		assertEquals(userEmail, statsCollector.getUserEmail());
		
		
		userList.get(0).setUserType(UserType.READ_ONLY);
		//it is not null because userEmail is set once
		assertEquals(userEmail, statsCollector.getUserEmail());
		
		statsCollector.setUserEmail(null);
		assertNull(statsCollector.getUserEmail());
		
		
		statsCollector.setUserEmail(null);
		userList.get(0).setUserType(UserType.ADMIN);
		userList.get(0).setScope("app1");
		assertNull(statsCollector.getUserEmail());
		
		statsCollector.setUserEmail(null);
		userList.remove(0);
		assertNull(statsCollector.getUserEmail());
		
		scopes.remove();
		user = new User(userEmail, null, UserType.ADMIN, CommonRestService.SCOPE_SYSTEM, null);
		userList.add(user);
		assertNull(statsCollector.getUserEmail());
		
		
		
		scopes.add(scope);
		Mockito.when(appContext.getBean(Mockito.anyString())).thenReturn(null);
		assertNull(statsCollector.getUserEmail());
		
		Mockito.when(appContext.getBean(Mockito.anyString())).thenReturn(adminApp);
		Mockito.when(appContext.containsBean(Mockito.anyString())).thenReturn(false);
		assertNull(statsCollector.getUserEmail());
		
		
		Mockito.when(appContext.containsBean(Mockito.anyString())).thenReturn(true);
		assertEquals(userEmail, statsCollector.getUserEmail());
		
	}
	
	private static byte[] getMacAddress(NetworkInterface networkInterface) {
		byte[] macAddressBytes = null;
		try {
			if (!networkInterface.isVirtual() && !networkInterface.isLoopback()) {
				macAddressBytes = networkInterface.getHardwareAddress();
			}

		} catch (SocketException e) {
			//log.error(ExceptionUtils.getStackTrace(e));
			e.printStackTrace();
		}
		return macAddressBytes;
	}

	
	private static String getHashInstanceId() {
		StringBuilder instanceId = new StringBuilder();
		try {

			Enumeration<NetworkInterface> networks = NetworkInterface.getNetworkInterfaces();
			while (networks.hasMoreElements()) {
				NetworkInterface network = networks.nextElement();
				byte[] mac = getMacAddress(network);
				instanceId = new StringBuilder();
				if (mac != null) {

					for (byte b : mac) {
						instanceId.append(String.format("%02X:", b));
					}
					if (instanceId.length() > 0) {
						instanceId.deleteCharAt(instanceId.length() - 1); // Remove trailing colon
					}
					System.out.println("ethernet:" + instanceId.toString());
					System.out.println("instanceId:" + CommonRestService.getMD5Hash(instanceId.toString()));
					//break;
				}
			}
		} catch (Exception e) {
			//logger.error(e.toString());
			e.printStackTrace();
		}

		if (instanceId.length() == 0) {
			instanceId.append(UUID.randomUUID().toString());
		}
		
		return CommonRestService.getMD5Hash(instanceId.toString());
	}
	
	@Test
	public void testInstanceId() {
		
		//System.out.println("InstanceId :" + Launcher.getInstanceId());
		
		getHashInstanceId();
		
	}
	
	@Test
	public void testJSObjects() {
		
		StatsCollector statsCollector = new StatsCollector();
		
		statsCollector.setVertx(vertx);
		statsCollector.setWebRTCVertx(webRTCVertx);
		
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

		jsObject = StatsCollector.getJVMNativeMemoryInfoJSObject();
		assertTrue(jsObject.has(StatsCollector.IN_USE_JVM_NATIVE_MEMORY));
		assertTrue(jsObject.has(StatsCollector.MAX_JVM_NATIVE_MEMORY));
		
		jsObject = StatsCollector.getSystemResourcesInfo(null);
		assertTrue(jsObject.has(StatsCollector.CPU_USAGE));
		assertTrue(jsObject.has(StatsCollector.JVM_MEMORY_USAGE));
		assertTrue(jsObject.has(StatsCollector.SYSTEM_INFO));
		assertTrue(jsObject.has(StatsCollector.SYSTEM_MEMORY_INFO));
		assertTrue(jsObject.has(StatsCollector.FILE_SYSTEM_INFO));
		assertTrue(jsObject.has(StatsCollector.JVM_NATIVE_MEMORY_USAGE));
		assertTrue(jsObject.has(StatsCollector.GPU_USAGE_INFO));
		assertTrue(jsObject.has(StatsCollector.LOCAL_WEBRTC_LIVE_STREAMS));
		assertTrue(jsObject.has(StatsCollector.LOCAL_WEBRTC_VIEWERS));
		assertTrue(jsObject.has(StatsCollector.LOCAL_HLS_VIEWERS));
		assertTrue(jsObject.has(StatsCollector.LOCAL_LIVE_STREAMS));
		assertTrue(jsObject.has(StatsCollector.FFMPEG_BUILD_INFO));

		
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
		
		assertEquals(75, resMonitor.getCpuLimit());
		resMonitor.setCpuLimit(45);
		assertEquals(45, resMonitor.getCpuLimit());
		
		resMonitor.setCpuLimit(150);
		assertEquals(100, resMonitor.getCpuLimit());
		
		resMonitor.setCpuLimit(9);
		assertEquals(10, resMonitor.getCpuLimit());
		
		assertEquals(75, resMonitor.getMemoryLimit());
		resMonitor.setMemoryLimit(45);
		assertEquals(45, resMonitor.getMemoryLimit());
		resMonitor.setMemoryLimit(150);
		assertEquals(100, resMonitor.getMemoryLimit());
		
		resMonitor.setMemoryLimit(0);
		assertEquals(10, resMonitor.getMemoryLimit());
		
		
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

		AVRational rat = new AVRational();
		StatsCollector resMonitor = Mockito.spy(new StatsCollector());
		//check default value
		assertEquals(300000, resMonitor.getHeartbeatPeriodMs());
		
		resMonitor.setHeartbeatPeriodMs(3000);
		resMonitor.setVertx(Vertx.vertx());
		resMonitor.start();
		
		assertTrue(resMonitor.isHeartBeatEnabled());
		
		
		Awaitility.await().pollDelay(5,TimeUnit.SECONDS).atMost(20, TimeUnit.SECONDS)
		.pollInterval(1, TimeUnit.SECONDS)
		.until(()->{
			return true;
		});
		
		Mockito.verify(resMonitor, times(1)).startAnalytic();
		
		resMonitor.cancelHeartBeat();
		
		
		
		resMonitor.setHeartBeatEnabled(false);
		resMonitor.start();
		assertFalse(resMonitor.isHeartBeatEnabled());
		Mockito.verify(resMonitor, times(1)).startAnalytic();
		
		resMonitor.cancelHeartBeat();
		
	}
	
	@Test
	public void testSendInstanceKafkaStats() {
		StatsCollector resMonitor = Mockito.spy(new StatsCollector());
		
		resMonitor.setVertx(vertx);
		resMonitor.setWebRTCVertx(webRTCVertx);
		
		Producer<Long, String> kafkaProducer = Mockito.mock(Producer.class);
		
		Future<RecordMetadata> futureMetdata = Mockito.mock(Future.class);
		
		
		//ProducerRecord<Long, String> record = new ProducerRecord<>(topicName,
		//		gson.toJson(jsonElement));
		
		ArgumentCaptor<ProducerRecord<Long, String>> producerRecord = ArgumentCaptor.forClass(ProducerRecord.class);
		
		Mockito.when(kafkaProducer.send(any())).thenReturn(futureMetdata);
		
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
		
		resMonitor.setKafkaProducer(kafkaProducer);
		
		List<WebRTCClientStats> webRTCClientStatList = new ArrayList<>();
		WebRTCClientStats stats = new WebRTCClientStats(100, 50, 40, 20, 60, 444, 9393838, "info", "192.168.1.1");
		webRTCClientStatList.add(stats);
		resMonitor.sendWebRTCClientStats2Kafka(webRTCClientStatList, "stream1");
		
		
		verify(kafkaProducer).send(producerRecord.capture());
		
		assertEquals(StatsCollector.WEBRTC_STATS_TOPIC_NAME, producerRecord.getValue().topic());
	}
	
	@Test
	public void testCreateKafka() {
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
		WebRTCClientStats stats = new WebRTCClientStats(100, 50, 40, 20, 60, 444, 9393838, "info", "192.168.1.1");
		webRTCClientStatList.add(stats);
		 
		Mockito.when(webRTCAdaptor.getWebRTCClientStats(any())).thenReturn(webRTCClientStatList);
		
		scopes.add(scope);
		
		resMonitor.setScopes(scopes);
		resMonitor.setVertx(Vertx.vertx());
		resMonitor.setStaticSendPeriod(5000);
	
		resMonitor.collectAndSendWebRTCClientsStats();
		
		verify(kafkaProducer, times(1)).send(Mockito.any());
		
	}
	
	@Test
	public void testStatusSendUnexpectedShutdownbHook() {
		ServerSettings serverSettings = new ServerSettings();
		
		serverSettings.setCpuMeasurementPeriodMs(10000);
		//Create StatsCollector
		StatsCollector statsCollector = Mockito.spy(new StatsCollector());
		
		ApplicationContext context = Mockito.mock(ApplicationContext.class);
		Mockito.when(context.getBean(IServer.ID)).thenReturn(Mockito.mock(IServer.class));
		Mockito.when(context.getBean(ServerSettings.BEAN_NAME)).thenReturn(serverSettings);
		
		Mockito.when(context.getBean(IAntMediaStreamHandler.VERTX_BEAN_NAME)).thenReturn(vertx);
		Mockito.when(context.getBean(WebSocketCommunityHandler.WEBRTC_VERTX_BEAN_NAME)).thenReturn(webRTCVertx);
		//Call setApplicationContext
		statsCollector.setApplicationContext(context);
		
		statsCollector.start();
		Mockito.verify(statsCollector, Mockito.never()).sendUnexpectedShutdownHook(Mockito.any());
		
		String httpUrl = "http://example.com";
		serverSettings.setServerStatusWebHookURL(httpUrl);
		
		assertEquals(30000, statsCollector.getUnexpectedShutDownDelayMs());
		statsCollector.setUnexpectedShutDownDelayMs(10);
		statsCollector.setApplicationContext(context);
		statsCollector.start();
		Mockito.verify(statsCollector, Mockito.after(100).never()).sendUnexpectedShutdownHook(Mockito.any());
		
		
		ConcurrentLinkedQueue<IScope> scopes = new ConcurrentLinkedQueue<>();
		IScope scope = Mockito.mock(IScope.class);
		IContext mockContext = Mockito.mock(IContext.class);
		Mockito.when(scope.getContext()).thenReturn(mockContext);
		AntMediaApplicationAdapter appAdaptor = new AntMediaApplicationAdapter();
		Mockito.when(context.getBean(AntMediaApplicationAdapter.BEAN_NAME)).thenReturn(appAdaptor);
		Mockito.when(context.containsBean(AntMediaApplicationAdapter.BEAN_NAME)).thenReturn(true);
		Mockito.when(mockContext.getApplicationContext()).thenReturn(context);
		
		scopes.add(scope);
		statsCollector.setScopes(scopes);
		
		
		statsCollector.setApplicationContext(context);
		statsCollector.start();
		Mockito.verify(statsCollector, Mockito.after(100).never()).sendUnexpectedShutdownHook(Mockito.any());
		
		appAdaptor.setShutdownProperly(false);
		statsCollector.setApplicationContext(context);
		statsCollector.start();
		Mockito.verify(statsCollector, Mockito.after(500)).sendUnexpectedShutdownHook(Mockito.any());
		
	}
	
	
	
	
	@Test
	public void testServerSettingsInteraction() {
		//Create server settings
		ServerSettings serverSettings = new ServerSettings();
		
		serverSettings.setCpuMeasurementPeriodMs(10000);
		serverSettings.setCpuMeasurementWindowSize(10);
		
		//Create StatsCollector
		StatsCollector statsCollector = new StatsCollector();
		
		//Create ApplicaitonContext mock and bind the server settings
		ApplicationContext context = Mockito.mock(ApplicationContext.class);
		Mockito.when(context.getBean(IServer.ID)).thenReturn(Mockito.mock(IServer.class));
		Mockito.when(context.getBean(ServerSettings.BEAN_NAME)).thenReturn(serverSettings);
		
		Mockito.when(context.getBean(IAntMediaStreamHandler.VERTX_BEAN_NAME)).thenReturn(vertx);
		Mockito.when(context.getBean(WebSocketCommunityHandler.WEBRTC_VERTX_BEAN_NAME)).thenReturn(webRTCVertx);
		//Call setApplicationContext
		statsCollector.setApplicationContext(context);
		
		//Check the fields
		assertEquals(10000, statsCollector.getMeasurementPeriod());
		assertEquals(10, statsCollector.getWindowSize());
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
		monitor.setVertx(vertx);
		monitor.setWebRTCVertx(webRTCVertx);
		
		long minValue = 100;
		long maxValue = 1000;
		
		//Cpu Limit = 70 & Min Free Ram Size = 50 MB
		
		//check default values
		Mockito.when(monitor.getOSType()).thenReturn(SystemUtils.LINUX);
		Mockito.when(monitor.getCpuLoad()).thenReturn(10);
		Mockito.when(monitor.getMemoryLoad()).thenReturn(10);
		assertEquals(true, monitor.enoughResource());
		try {
			Mockito.verify(monitor, Mockito.after(100).never()).sendPOST(Mockito.any(), Mockito.any());
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		
		Mockito.when(monitor.getOSType()).thenReturn(SystemUtils.MAC_OS_X);
		Mockito.when(monitor.getFreeRam()).thenReturn(100);
		assertEquals(true, monitor.enoughResource());
		try {
			Mockito.verify(monitor, Mockito.after(100).never()).sendPOST(Mockito.any(), Mockito.any());
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		
		
		//CPU value over 70
		Mockito.when(monitor.getOSType()).thenReturn(SystemUtils.LINUX);
		Mockito.when(monitor.getCpuLoad()).thenReturn(80);
		
		assertEquals(false, monitor.enoughResource());
		try {
			Mockito.verify(monitor, Mockito.after(100).never()).sendPOST(Mockito.any(), Mockito.any());
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		
		//RAM load is 80
		
		Mockito.when(monitor.getCpuLoad()).thenReturn(10);
		Mockito.when(monitor.getMemoryLoad()).thenReturn(80);
		monitor.setWebhookURL("http://example.com");
		
		assertEquals(false, monitor.enoughResource());
		
		try {
			Mockito.verify(monitor, Mockito.after(5000)).sendPOST(Mockito.any(), Mockito.any());
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		
		Mockito.when(monitor.getOSType()).thenReturn(SystemUtils.MAC_OS_X);
		Mockito.when(monitor.getFreeRam()).thenReturn(10);
		assertEquals(false, monitor.enoughResource());
		
		try {
			Mockito.verify(monitor, Mockito.after(5000).times(2)).sendPOST(Mockito.any(), Mockito.any());
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		
		Mockito.when(monitor.getFreeRam()).thenReturn(-1);
		assertEquals(true, monitor.enoughResource());
		
		try {
			Mockito.verify(monitor, Mockito.after(5000).times(2)).sendPOST(Mockito.any(), Mockito.any());
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		
		
		// Check false values in Max and Current physical memory
		
		Mockito.when(monitor.getOSType()).thenReturn(SystemUtils.LINUX);
		Mockito.when(monitor.getCpuLoad()).thenReturn(10);
		
		Mockito.when(monitor.getMemoryLoad()).thenReturn(10);
		
		
		assertEquals(true, monitor.enoughResource());
		
		try {
			
			Mockito.verify(monitor, Mockito.after(100).times(2)).sendPOST(Mockito.any(), Mockito.any());
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		
	}
	
	@Test
	public void testMemInfo() {
		
		AVRational rational = new AVRational();
		assertTrue( 0 != SystemUtils.osAvailableMemory());
	}
	
	@Test
	public void testGetAppAdaptor() 
	{
		ApplicationContext appContext = Mockito.mock(ApplicationContext.class);
		assertNull(StatsCollector.getAppAdaptor(appContext));
		
		Mockito.when(appContext.containsBean(AntMediaApplicationAdapter.BEAN_NAME)).thenReturn(true);
		Mockito.when(appContext.getBean(AntMediaApplicationAdapter.BEAN_NAME)).thenReturn(new AntMediaApplicationAdapter());
		assertNotNull(StatsCollector.getAppAdaptor(appContext));
		
		AntMediaApplicationAdapter adaptor = Mockito.mock(AntMediaApplicationAdapter.class);
		Mockito.when(appContext.getBean(AntMediaApplicationAdapter.BEAN_NAME)).thenReturn(adaptor);
		assertEquals(adaptor, StatsCollector.getAppAdaptor(appContext));
		
		
	}

	@Test
	public void testOsAvailableMemory() {
		long availableMemory = 1024;
		long containerizedMemory = 2048;
		try (MockedStatic<SystemUtils> mockedSystemUtils = mockStatic(SystemUtils.class)) {
			// Set up base mocking to allow real method call
			mockedSystemUtils.when(SystemUtils::osAvailableMemory)
					.thenCallRealMethod();

			// Test non-containerized scenario
			mockedSystemUtils.when(SystemUtils::isContainerized).thenReturn(false);
			mockedSystemUtils.when(SystemUtils::availablePhysicalBytes).thenReturn(availableMemory);

			// Reset the state for containerized scenario
			SystemUtils.containerized = null;
			
			long nonContainerizedResult = SystemUtils.osAvailableMemory();
			assertEquals(availableMemory, nonContainerizedResult);

			// Reset the state for containerized scenario
			SystemUtils.containerized = null;

			// Test containerized scenario
			mockedSystemUtils.when(SystemUtils::isContainerized).thenReturn(true);
			mockedSystemUtils.when(SystemUtils::getMemAvailableFromCgroup).thenReturn(containerizedMemory);

			long containerizedResult = SystemUtils.osAvailableMemory();
			assertEquals(containerizedMemory, containerizedResult);

			// Verify all calls
			mockedSystemUtils.verify(SystemUtils::isContainerized, times(2));
			mockedSystemUtils.verify(SystemUtils::availablePhysicalBytes, times(1));
			mockedSystemUtils.verify(SystemUtils::getMemAvailableFromCgroup, times(1));
		}
	}

	@Test
	public void testIsContainerized() {
		Path mockDockerEnvPath = Path.of("/tmp/test/.dockerenv");
		Path mockCgroupPath = Path.of("/tmp/test/cgroup");

		try (MockedStatic<Files> mockedFiles = mockStatic(Files.class);
			 MockedStatic<Paths> mockedPaths = mockStatic(Paths.class);
			 ) {

			// Setup path mocks
			mockedPaths.when(() -> Paths.get("/.dockerenv")).thenReturn(mockDockerEnvPath);
			mockedPaths.when(() -> Paths.get("/proc/self/cgroup")).thenReturn(mockCgroupPath);

			// Test 1: Docker environment file exists
			mockedFiles.when(() -> Files.exists(mockDockerEnvPath)).thenReturn(true);
			assertTrue(SystemUtils.isContainerized());


			// Test 2: Docker in cgroup
			mockedFiles.when(() -> Files.exists(mockDockerEnvPath)).thenReturn(false);
			mockedFiles.when(() -> Files.exists(mockCgroupPath)).thenReturn(true);
			mockedFiles.when(() -> Files.readAllLines(mockCgroupPath))
					.thenReturn(Collections.singletonList("12:memory:/docker/someId"));
			assertTrue(SystemUtils.isContainerized());

			// Test 3: LXC in cgroup
			mockedFiles.when(() -> Files.readAllLines(mockCgroupPath))
					.thenReturn(Collections.singletonList("12:memory:/lxc/someId"));
			assertTrue(SystemUtils.isContainerized());

			// Test 4: Kubernetes in cgroup
			mockedFiles.when(() -> Files.readAllLines(mockCgroupPath))
					.thenReturn(Collections.singletonList("12:memory:/kubepods/someId"));
			assertTrue(SystemUtils.isContainerized());

			// Test 5: Containerd in cgroup
			mockedFiles.when(() -> Files.readAllLines(mockCgroupPath))
					.thenReturn(Collections.singletonList("12:memory:/containerd/someId"));
			assertTrue(SystemUtils.isContainerized());

			// Test 6: No container
			mockedFiles.when(() -> Files.readAllLines(mockCgroupPath))
					.thenReturn(Collections.singletonList("12:memory:/user.slice"));
			assertFalse(SystemUtils.isContainerized());

			// Test 7: File access exception
			mockedFiles.when(() -> Files.exists(mockDockerEnvPath))
					.thenThrow(new SecurityException("Access denied"));
			assertFalse(SystemUtils.isContainerized());

			// Test 8: Read exception
			mockedFiles.when(() -> Files.exists(mockDockerEnvPath)).thenReturn(false);
			mockedFiles.when(() -> Files.exists(mockCgroupPath)).thenReturn(true);
			mockedFiles.when(() -> Files.readAllLines(mockCgroupPath))
					.thenThrow(new IOException("Read error"));
			assertFalse(SystemUtils.isContainerized());
		}

	}

	@Test
	public void testGetMemAvailableFromCgroup() throws IOException {
		String cgroupV1UsagePath = "/sys/fs/cgroup/memory/memory.usage_in_bytes";
		String cgroupV1LimitPath = "/sys/fs/cgroup/memory/memory.limit_in_bytes";
		String cgroupV2UsagePath = "/sys/fs/cgroup/memory.current";
		String cgroupV2LimitPath = "/sys/fs/cgroup/memory.max";
		String expectedUsageStr = "5000";
		String expectedLimitStr = "10000";

		long expectedOsFreeMemory = 8000L;

		// Test all three scenarios using nested try-with-resources
		try (MockedStatic<Files> mockedFiles = mockStatic(Files.class);
			 MockedStatic<Paths> mockedPaths = mockStatic(Paths.class)) {

			// Mock Path objects
			Path cgroupV1UsagePathObj = mock(Path.class);
			Path cgroupV1LimitPathObj = mock(Path.class);
			Path cgroupV2UsagePathObj = mock(Path.class);
			Path cgroupV2LimitPathObj = mock(Path.class);

			// Set up Paths.get() mocks
			when(Paths.get(cgroupV1UsagePath)).thenReturn(cgroupV1UsagePathObj);
			when(Paths.get(cgroupV1LimitPath)).thenReturn(cgroupV1LimitPathObj);
			when(Paths.get(cgroupV2UsagePath)).thenReturn(cgroupV2UsagePathObj);
			when(Paths.get(cgroupV2LimitPath)).thenReturn(cgroupV2LimitPathObj);

			// Test Scenario 1: cgroups v1 exists
			mockedFiles.when(() -> Files.exists(cgroupV1UsagePathObj)).thenReturn(true);
			mockedFiles.when(() -> Files.exists(cgroupV1LimitPathObj)).thenReturn(true);

			try (MockedStatic<SystemUtils> mockedMemoryUtils = mockStatic(SystemUtils.class,
					CALLS_REAL_METHODS)) {
				mockedMemoryUtils.when(() -> SystemUtils.readCgroupFile(cgroupV1UsagePath))
						.thenReturn(expectedUsageStr);
				mockedMemoryUtils.when(() -> SystemUtils.readCgroupFile(cgroupV1LimitPath))
						.thenReturn(expectedLimitStr);

				long result = SystemUtils.getMemAvailableFromCgroup();
				assertEquals(Long.parseLong(expectedLimitStr) - Long.parseLong(expectedUsageStr), result);
			}

			// Test Scenario 2: cgroups v2 exists (v1 doesn't exist)
			mockedFiles.when(() -> Files.exists(cgroupV1UsagePathObj)).thenReturn(false);
			mockedFiles.when(() -> Files.exists(cgroupV1LimitPathObj)).thenReturn(false);
			mockedFiles.when(() -> Files.exists(cgroupV2UsagePathObj)).thenReturn(true);
			mockedFiles.when(() -> Files.exists(cgroupV2LimitPathObj)).thenReturn(true);

			try (MockedStatic<SystemUtils> mockedMemoryUtils = mockStatic(SystemUtils.class,
					CALLS_REAL_METHODS)) {
				mockedMemoryUtils.when(() -> SystemUtils.readCgroupFile(cgroupV2UsagePath))
						.thenReturn(expectedUsageStr);
				mockedMemoryUtils.when(() -> SystemUtils.readCgroupFile(cgroupV2LimitPath))
						.thenReturn(expectedLimitStr);

				long result = SystemUtils.getMemAvailableFromCgroup();
				assertEquals(Long.parseLong(expectedLimitStr) - Long.parseLong(expectedUsageStr), result);
			}

			// Test Scenario 3: No cgroups exist, falls back to OS memory
			mockedFiles.when(() -> Files.exists(cgroupV2UsagePathObj)).thenReturn(false);
			mockedFiles.when(() -> Files.exists(cgroupV2LimitPathObj)).thenReturn(false);

			try (MockedStatic<SystemUtils> mockedMemoryUtils = mockStatic(SystemUtils.class,
					CALLS_REAL_METHODS)) {
				mockedMemoryUtils.when(SystemUtils::osFreePhysicalMemory)
						.thenReturn(expectedOsFreeMemory);

				long result = SystemUtils.getMemAvailableFromCgroup();
				assertEquals(expectedOsFreeMemory, result);
			}
		}
	}

	@Test
	public void testOsTotalPhysicalMemory() throws IOException {
		final long PHYSICAL_MEMORY_SIZE = 16_000_000_000L; // 16GB

		try (MockedStatic<Files> mockedFiles = mockStatic(Files.class);
			 MockedStatic<SystemUtils> mockedSystemUtils = mockStatic(SystemUtils.class,
					 CALLS_REAL_METHODS)) {

			// Test Case 1: Non-containerized environment
			mockedSystemUtils.when(SystemUtils::isContainerized)
					.thenReturn(false);
			mockedSystemUtils.when(SystemUtils::getTotalPhysicalMemorySize)
					.thenReturn(PHYSICAL_MEMORY_SIZE);

			// Call the real method for osTotalPhysicalMemory
			mockedSystemUtils.when(SystemUtils::osTotalPhysicalMemory)
					.thenCallRealMethod();

			assertEquals(PHYSICAL_MEMORY_SIZE, SystemUtils.osTotalPhysicalMemory());

			// Test Case 2: Containerized environment with cgroups v1
			mockedSystemUtils.when(SystemUtils::isContainerized)
					.thenReturn(true);

			// Mock cgroups v1 path exists
			mockedFiles.when(() -> Files.exists(Paths.get("/sys/fs/cgroup/memory/memory.limit_in_bytes")))
					.thenReturn(true);
			mockedFiles.when(() -> Files.exists(Paths.get("/sys/fs/cgroup/memory.max")))
					.thenReturn(false);

			// Test 2a: Normal memory limit
			long containerLimit = 8_000_000_000L; // 8GB
			mockedSystemUtils.when(() -> SystemUtils.readCgroupFile(anyString()))
					.thenReturn(String.valueOf(containerLimit));

			assertEquals(containerLimit, SystemUtils.getMemoryLimitFromCgroup().longValue());

			// Test 2b: Memory limit above MAX_CONTAINER_MEMORY_LIMIT_BYTES
			mockedSystemUtils.when(() -> SystemUtils.readCgroupFile(anyString()))
					.thenReturn(String.valueOf(SystemUtils.MAX_CONTAINER_MEMORY_LIMIT_BYTES + 1));

			assertEquals(PHYSICAL_MEMORY_SIZE, SystemUtils.getMemoryLimitFromCgroup().longValue());

			// Test Case 3: Containerized environment with cgroups v2
			mockedFiles.when(() -> Files.exists(Paths.get("/sys/fs/cgroup/memory/memory.limit_in_bytes")))
					.thenReturn(false);
			mockedFiles.when(() -> Files.exists(Paths.get("/sys/fs/cgroup/memory.max")))
					.thenReturn(true);

			// Test 3a: Normal memory limit
			mockedSystemUtils.when(() -> SystemUtils.readCgroupFile(anyString()))
					.thenReturn("4000000000"); // 4GB

			assertEquals(4_000_000_000L, SystemUtils.getMemoryLimitFromCgroup().longValue());

			// Test 3b: No limit set (max)
			mockedSystemUtils.when(() -> SystemUtils.readCgroupFile(anyString()))
					.thenReturn(SystemUtils.MAX_MEMORY_CGROUP_V2);

			assertEquals(PHYSICAL_MEMORY_SIZE, SystemUtils.getMemoryLimitFromCgroup().longValue());

			// Test Case 4: No cgroup files exist
			mockedFiles.when(() -> Files.exists(any(Path.class)))
					.thenReturn(false);

			assertEquals(PHYSICAL_MEMORY_SIZE, SystemUtils.getMemoryLimitFromCgroup().longValue());

			// Test Case 5: IOException handling
			mockedSystemUtils.when(SystemUtils::isContainerized)
					.thenReturn(true);
			mockedSystemUtils.when(SystemUtils::getMemoryLimitFromCgroup)
					.thenThrow(new IOException("Test exception"));

			assertEquals(PHYSICAL_MEMORY_SIZE, SystemUtils.osTotalPhysicalMemory());
		}
	}

}
