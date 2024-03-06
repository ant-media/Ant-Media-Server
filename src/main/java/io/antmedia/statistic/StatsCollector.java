package io.antmedia.statistic;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.LongSerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.bytedeco.javacpp.Pointer;
import org.red5.server.Launcher;
import org.red5.server.api.IServer;
import org.red5.server.api.listeners.IScopeListener;
import org.red5.server.api.scope.IScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.FFmpegUtilities;
import io.antmedia.SystemUtils;
import io.antmedia.console.AdminApplication;
import io.antmedia.console.datastore.AbstractConsoleDataStore;
import io.antmedia.console.rest.CommonRestService;
import io.antmedia.datastore.db.types.Licence;
import io.antmedia.datastore.db.types.User;
import io.antmedia.licence.ILicenceService;
import io.antmedia.muxer.IAntMediaStreamHandler;
import io.antmedia.rest.RestServiceBase;
import io.antmedia.rest.WebRTCClientStats;
import io.antmedia.rest.model.UserType;
import io.antmedia.settings.ServerSettings;
import io.antmedia.statistic.GPUUtils.MemoryStatus;
import io.antmedia.webrtc.api.IWebRTCAdaptor;
import io.antmedia.websocket.WebSocketCommunityHandler;
import io.vertx.core.Vertx;
import io.vertx.ext.dropwizard.MetricsService;



public class StatsCollector implements IStatsCollector, ApplicationContextAware, DisposableBean {	

	public static final String FREE_NATIVE_MEMORY = "freeNativeMemory";

	public static final String TOTAL_NATIVE_MEMORY = "totalNativeMemory";

	public static final String IN_USE_NATIVE_MEMORY = "inUseNativeMemory";

	public static final String AVAILABLE_MEMORY = "availableMemory";

	public static final String IN_USE_SWAP_SPACE = "inUseSwapSpace";

	public static final String FREE_SWAP_SPACE = "freeSwapSpace";

	public static final String TOTAL_SWAP_SPACE = "totalSwapSpace";

	public static final String VIRTUAL_MEMORY = "virtualMemory";

	public static final String PROCESSOR_COUNT = "processorCount";

	public static final String JAVA_VERSION = "javaVersion";

	public static final String OS_ARCH = "osArch";

	public static final String OS_NAME = "osName";

	public static final String IN_USE_SPACE = "inUseSpace";

	public static final String FREE_SPACE = "freeSpace";

	public static final String TOTAL_SPACE = "totalSpace";

	public static final String USABLE_SPACE = "usableSpace";

	public static final String IN_USE_MEMORY = "inUseMemory";

	public static final String FREE_MEMORY = "freeMemory";

	public static final String TOTAL_MEMORY = "totalMemory";

	public static final String MAX_MEMORY = "maxMemory";

	public static final String PROCESS_CPU_LOAD = "processCPULoad";
	
	public static final String SYSTEM_LOAD_AVERAGE_IN_LAST_MINUTE = "systemLoadAverageLastMinute";

	public static final String SYSTEM_CPU_LOAD = "systemCPULoad";

	public static final String PROCESS_CPU_TIME = "processCPUTime";

	public static final String CPU_USAGE = "cpuUsage";

	public static final String INSTANCE_ID = "instanceId";

	public static final String MARKETPLACE_NAME = "marketplace";

	public static final String USER_EMAIL = "userEmail";

	public static final String LICENSE_VALID = "licenseValid";

	public static final String INSTANCE_TYPE = "instanceType";

	public static final String INSTANCE_VERSION = "instanceVersion";

	public static final String JVM_MEMORY_USAGE = "jvmMemoryUsage";

	public static final String NATIVE_MEMORY_USAGE = "nativeMemoryUsage";

	public static final String SYSTEM_INFO = "systemInfo";

	public static final String SYSTEM_MEMORY_INFO = "systemMemoryInfo";

	public static final String FILE_SYSTEM_INFO = "fileSystemInfo";

	public static final String GPU_UTILIZATION = "gpuUtilization";

	public static final String GPU_DEVICE_INDEX = "index";

	public static final String GPU_MEMORY_UTILIZATION = "memoryUtilization";

	public static final String GPU_MEMORY_TOTAL = "memoryTotal";

	public static final String GPU_MEMORY_FREE = "memoryFree";

	public static final String GPU_MEMORY_USED = "memoryUsed";

	public static final String GPU_DEVICE_NAME = "deviceName";

	public static final String GPU_USAGE_INFO = "gpuUsageInfo";

	public static final String FFMPEG_BUILD_INFO = "ffmpegBuildInfo";

	public static final String TOTAL_LIVE_STREAMS = "totalLiveStreamSize";

	public static final String LOCAL_WEBRTC_LIVE_STREAMS = "localWebRTCLiveStreams";

	public static final String LOCAL_LIVE_STREAMS = "localLiveStreams";

	public static final String LOCAL_WEBRTC_VIEWERS = "localWebRTCViewers";

	public static final String LOCAL_HLS_VIEWERS = "localHLSViewers";

	public static final String LOCAL_DASH_VIEWERS = "localDASHViewers";

	private static final String TIME = "time";

	protected static final Logger logger = LoggerFactory.getLogger(StatsCollector.class);

	private static final String MEASURED_BITRATE = "measured_bitrate";

	private static final String SEND_BITRATE = "send_bitrate";

	private static final String AUDIO_FRAME_SEND_PERIOD = "audio_frame_send_period";

	private static final String VIDEO_FRAME_SEND_PERIOD = "video_frame_send_period";

	private static final String STREAM_ID = "streamId";

	private static final String WEBRTC_CLIENT_ID = "webrtcClientId";

	private static final String WEBRTC_VIEWER_INFO = "webrtcViewerInfo";

	private Queue<IScope> scopes = new ConcurrentLinkedQueue<>();

	public static final String GA_TRACKING_ID = "UA-93263926-3";

	private Vertx vertx;
	private Queue<Integer> cpuMeasurements = new ConcurrentLinkedQueue<>();

	private static Gson gson = new Gson();

	private int windowSize = 5;
	private int measurementPeriod = 1000;
	private int staticSendPeriod = 15000;

	private int cpuLoad;
	private int cpuLimit = 75;
	
	
	/**
	 * Memory limit in percentage
	 */
	private int memoryLimit = 75;

	/**
	 * Min Free Ram Size that free memory should be always more than min
	 * Use {@link #memoryLimit}
	 */
	private int minFreeRamSize = 50;

	private String kafkaBrokers = null;

	public static final String INSTANCE_STATS_TOPIC_NAME = "ams-instance-stats";

	public static final String WEBRTC_STATS_TOPIC_NAME = "ams-webrtc-stats";

	public static final String UP_TIME = "up-time";

	public static final String START_TIME = "start-time";

	public static final String SERVER_TIMING = "server-timing";

	private static final String ENCODERS_BLOCKED = "encoders-blocked";

	private static final String ENCODERS_NOT_OPENED = "encoders-not-opened";

	private static final String PUBLISH_TIMEOUT_ERRORS = "publish-timeout-errors";

	private static final String THREAD_DUMP = "thread-dump";

	public static final String DEAD_LOCKED_THREAD = "dead-locked-thread";

	public static final String THREAD_COUNT = "thread-count";

	public static final String THREAD_PEEK_COUNT = "thread-peek-count";

	private static final String THREAD_NAME = "thread-name";

	private static final String THREAD_ID = "thread-id";

	private static final String THREAD_BLOCKED_TIME = "blocked-time";

	private static final String THREAD_BLOCKED_COUNT = "blocked-count";

	private static final String THREAD_WAITED_TIME = "waited-time";

	private static final String THREAD_WAITED_COUNT = "waited-count";

	private static final String THREAD_LOCK_NAME = "lock-name";

	private static final String THREAD_LOCK_OWNER_ID = "lock-owner-id";

	private static final String THREAD_LOCK_OWNER_NAME = "lock-owner-name";

	private static final String THREAD_IN_NATIVE = "in-native";

	private static final String THREAD_SUSPENDED = "suspended";

	private static final String THREAD_STATE = "state";

	private static final String THREAD_CPU_TIME = "cpu-time";

	private static final String THREAD_USER_TIME = "user-time";

	public static final String IN_USE_JVM_NATIVE_MEMORY = "inUseMemory";

	public static final String MAX_JVM_NATIVE_MEMORY = "maxMemory";

	public static final String JVM_NATIVE_MEMORY_USAGE = "jvmNativeMemoryUsage";

	private static final String HOST_ADDRESS = "host-address";

	private static final String IP_ADDRESS = "ip-address";

	private static final String VERTX_WORKER_QUEUE_SIZE = "vertx.pools.worker.vert.x-worker-thread.queue-size";

	private static final String VERTX_WORKER_THREAD_QUEUE_SIZE = "vertx-worker-thread-queue-size";

	private static final String WEBRTC_VERTX_WORKER_THREAD_QUEUE_SIZE = "webrtc-vertx-worker-thread-queue-size";


	public static final String HOOK_HIGH_RESOURCE_USAGE = "highResourceUsage";
	public static final String HOOK_UNEXPECTED_SERVER_SHUTDOWN = "unexpectedServerShutdown";

	private static final String SOFTWARE_VERSION = "softwareVersion";


	private Producer<Long,String> kafkaProducer = null;

	private long cpuMeasurementTimerId = -1;

	private long kafkaTimerId = -1;

	private boolean heartBeatEnabled = true;

	private long hearbeatPeriodicTask;

	private int heartbeatPeriodMs = 300000;

	private String hostAddress;

	private Vertx webRTCVertx;

	private int time2Log = 0;

	private String marketplace;

	private static MetricsService vertXMetrics;

	private static MetricsService webRTCVertxMetrics;

	private ILicenceService licenseService;

	private String userEmail;

	/**
	 * Webhook url to notify high resource usage, unexpected shutdown. More callbacks can be added
	 */
	private String webhookURL;

	private long unexpectedShutDownDelayMs = 30000;


	public void start() {
		cpuMeasurementTimerId  = getVertx().setPeriodic(measurementPeriod, l -> 
		{
			addCpuMeasurement(SystemUtils.getSystemCpuLoad());


			//log every 5 minute
			if (300000/measurementPeriod == time2Log) {
				if(logger != null) 
				{
					logger.info("System CPU:%{} Process CPU:%{} System Load Average:{} Memory:%{}", cpuLoad, SystemUtils.getProcessCpuLoad(), SystemUtils.getSystemLoadAverageLastMinute(), getMemoryLoad());

					int vertxWorkerQueueSize = getVertWorkerQueueSizeStatic();

					int webRTCVertxWorkerQueueSize = getWebRTCVertxWorkerQueueSizeStatic();

					logger.info("Vertx worker queue size:{} WebRTCVertx worker queue size:{}", vertxWorkerQueueSize, webRTCVertxWorkerQueueSize);

				}

				time2Log = 0;
			}
			time2Log++;
		});
		startKafkaProducer();

		if (heartBeatEnabled) {

			logger.warn("Starting heartbeats for the version:{} and type:{}", Launcher.getVersion(), Launcher.getVersionType());

			getVertx().setPeriodic(heartbeatPeriodMs, l -> startAnalytic());
		}
		else {
			logger.info("Heartbeats are disabled for this instance");
		}


		//Notify for unexpected shutdowns
		if (webhookURL != null && !webhookURL.isEmpty())  {
			//let is pass some time to make all scopes are ready
			getVertx().setTimer(unexpectedShutDownDelayMs , h -> {

				ArrayList<String> appNames = new ArrayList<>();
				for (Iterator<IScope> iterator = scopes.iterator(); iterator.hasNext();) { 
					IScope scope = iterator.next();

					AntMediaApplicationAdapter adaptor = null;

					if ((adaptor = getAppAdaptor(scope.getContext().getApplicationContext())) != null 
							&& !adaptor.isShutdownProperly())
					{
						appNames.add(scope.getName());
					}
				}

				if (!appNames.isEmpty()) 
				{
					sendUnexpectedShutdownHook(appNames);
				}

			});
		}
	}

	private void startKafkaProducer() {
		if (kafkaBrokers != null && !kafkaBrokers.isEmpty()) {
			kafkaProducer = createKafkaProducer();		

			kafkaTimerId  = getVertx().setPeriodic(staticSendPeriod, l -> {
				sendInstanceStats(scopes);
				sendWebRTCClientStats();
			});
		}	
	}

	private static int getVertWorkerQueueSizeStatic() {
		io.vertx.core.json.JsonObject queueSizeMetrics = vertXMetrics.getMetricsSnapshot(VERTX_WORKER_QUEUE_SIZE);
		io.vertx.core.json.JsonObject jsonObject = null;
		if (queueSizeMetrics != null) {
			jsonObject = queueSizeMetrics.getJsonObject(VERTX_WORKER_QUEUE_SIZE);
		}
		return jsonObject != null ? jsonObject.getInteger("count") : -1;
	}

	public int getVertWorkerQueueSize() {
		return getVertWorkerQueueSizeStatic();
	}

	private static int getWebRTCVertxWorkerQueueSizeStatic() {
		io.vertx.core.json.JsonObject queueSizeMetrics = webRTCVertxMetrics.getMetricsSnapshot(VERTX_WORKER_QUEUE_SIZE);
		io.vertx.core.json.JsonObject jsonObject = null;
		if (queueSizeMetrics != null) {
			jsonObject = queueSizeMetrics.getJsonObject(VERTX_WORKER_QUEUE_SIZE);
		}
		return jsonObject != null ? jsonObject.getInteger("count") : -1;
	}

	public int getWebRTCVertxWorkerQueueSize() {
		return getWebRTCVertxWorkerQueueSizeStatic();
	}

	private void sendWebRTCClientStats() {
		getVertx().executeBlocking(
				b -> {
					collectAndSendWebRTCClientsStats();
					b.complete();
				}, 
				null);
	}


	public void collectAndSendWebRTCClientsStats() {

		for (Iterator<IScope> iterator = scopes.iterator(); iterator.hasNext();) { 
			IScope scope = iterator.next();

			if( scope.getContext().getApplicationContext().containsBean(IWebRTCAdaptor.BEAN_NAME))
			{
				IWebRTCAdaptor webrtcAdaptor = (IWebRTCAdaptor)scope.getContext().getApplicationContext().getBean(IWebRTCAdaptor.BEAN_NAME);
				Set<String> streams = webrtcAdaptor.getStreams();
				List<WebRTCClientStats> webRTCClientStats;
				for (String streamId : streams) {
					webRTCClientStats = webrtcAdaptor.getWebRTCClientStats(streamId);
					sendWebRTCClientStats2Kafka(webRTCClientStats, streamId);			
				}							
			}
		}


	}

	public void sendWebRTCClientStats2Kafka(List<WebRTCClientStats> webRTCClientStatList, String streamId) {
		JsonObject jsonObject;
		String dateTime = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
		for (WebRTCClientStats webRTCClientStat : webRTCClientStatList) 
		{
			jsonObject = new JsonObject();
			jsonObject.addProperty(STREAM_ID, streamId);
			jsonObject.addProperty(WEBRTC_CLIENT_ID, webRTCClientStat.getClientId());
			jsonObject.addProperty(AUDIO_FRAME_SEND_PERIOD, (int)webRTCClientStat.getAudioFrameSendPeriod());
			jsonObject.addProperty(VIDEO_FRAME_SEND_PERIOD, (int)webRTCClientStat.getVideoFrameSendPeriod());
			jsonObject.addProperty(MEASURED_BITRATE, webRTCClientStat.getMeasuredBitrate());
			jsonObject.addProperty(SEND_BITRATE, webRTCClientStat.getSendBitrate());
			jsonObject.addProperty(TIME, dateTime);
			jsonObject.addProperty(HOST_ADDRESS, hostAddress);
			jsonObject.addProperty(WEBRTC_VIEWER_INFO, webRTCClientStat.getClientInfo());
			jsonObject.addProperty(IP_ADDRESS, webRTCClientStat.getClientIp());

			//logstash cannot parse json array so that we send each info separately
			send2Kafka(jsonObject, WEBRTC_STATS_TOPIC_NAME);
		}
	}

	public Producer<Long, String> createKafkaProducer() {
		Properties props = new Properties();
		props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBrokers);
		props.put(ProducerConfig.CLIENT_ID_CONFIG, Launcher.getInstanceId());
		props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, LongSerializer.class.getName());
		props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
		props.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 10000);
		return new KafkaProducer<>(props);
	}

	public static JsonObject getFileSystemInfoJSObject() {
		JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty(USABLE_SPACE, SystemUtils.osHDUsableSpace(null));
		jsonObject.addProperty(TOTAL_SPACE, SystemUtils.osHDTotalSpace(null));
		jsonObject.addProperty(FREE_SPACE, SystemUtils.osHDFreeSpace(null));
		jsonObject.addProperty(IN_USE_SPACE, SystemUtils.osHDInUseSpace(null));
		return jsonObject;
	}


	public static JsonObject getGPUInfoJSObject(int deviceIndex, GPUUtils gpuUtils) {
		JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty(GPU_DEVICE_INDEX, deviceIndex);
		jsonObject.addProperty(GPU_UTILIZATION, gpuUtils.getGPUUtilization(deviceIndex));
		jsonObject.addProperty(GPU_MEMORY_UTILIZATION, gpuUtils.getMemoryUtilization(deviceIndex));
		MemoryStatus memoryStatus = gpuUtils.getMemoryStatus(deviceIndex);
		jsonObject.addProperty(GPU_MEMORY_TOTAL, memoryStatus.getMemoryTotal());
		jsonObject.addProperty(GPU_MEMORY_FREE, memoryStatus.getMemoryFree());
		jsonObject.addProperty(GPU_MEMORY_USED, memoryStatus.getMemoryUsed());
		jsonObject.addProperty(GPU_DEVICE_NAME, GPUUtils.getInstance().getDeviceName(deviceIndex));

		return jsonObject;
	}


	public static JsonArray getGPUInfoJSObject() {
		int deviceCount = GPUUtils.getInstance().getDeviceCount();
		JsonArray jsonArray = new JsonArray();
		if (deviceCount > 0) {
			for (int i=0; i < deviceCount; i++) {
				jsonArray.add(getGPUInfoJSObject(i, GPUUtils.getInstance()));
			}
		}
		return jsonArray;
	}

	public static JsonObject getCPUInfoJSObject() {
		JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty(PROCESS_CPU_TIME, SystemUtils.getProcessCpuTime());
		jsonObject.addProperty(SYSTEM_CPU_LOAD, SystemUtils.getSystemCpuLoad());
		jsonObject.addProperty(PROCESS_CPU_LOAD, SystemUtils.getProcessCpuLoad());
		jsonObject.addProperty(SYSTEM_LOAD_AVERAGE_IN_LAST_MINUTE, SystemUtils.getSystemLoadAverageLastMinute());
		return jsonObject;
	}

	public static ThreadInfo[] getThreadDump() {
		ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
		return threadMXBean.dumpAllThreads(true, true);
	}

	public static JsonArray getThreadDumpJSON() {
		ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

		ThreadInfo[] threadDump = threadMXBean.dumpAllThreads(true, true);
		JsonArray jsonArray = new JsonArray();

		for (int i = 0; i < threadDump.length; i++) {
			JsonObject jsonObject = new JsonObject();

			jsonObject.addProperty(THREAD_NAME, threadDump[i].getThreadName());
			jsonObject.addProperty(THREAD_ID, threadDump[i].getThreadId());
			jsonObject.addProperty(THREAD_BLOCKED_TIME, threadDump[i].getBlockedTime());
			jsonObject.addProperty(THREAD_BLOCKED_COUNT, threadDump[i].getBlockedCount());
			jsonObject.addProperty(THREAD_WAITED_TIME, threadDump[i].getWaitedTime());
			jsonObject.addProperty(THREAD_WAITED_COUNT, threadDump[i].getWaitedCount());
			jsonObject.addProperty(THREAD_LOCK_NAME, threadDump[i].getLockName());
			jsonObject.addProperty(THREAD_LOCK_OWNER_ID, threadDump[i].getLockOwnerId());
			jsonObject.addProperty(THREAD_LOCK_OWNER_NAME, threadDump[i].getLockOwnerName());
			jsonObject.addProperty(THREAD_IN_NATIVE, threadDump[i].isInNative());
			jsonObject.addProperty(THREAD_SUSPENDED, threadDump[i].isSuspended());
			jsonObject.addProperty(THREAD_STATE, threadDump[i].getThreadState().toString());
			jsonObject.addProperty(THREAD_CPU_TIME, threadMXBean.getThreadCpuTime(threadDump[i].getThreadId()));
			jsonObject.addProperty(THREAD_USER_TIME, threadMXBean.getThreadUserTime(threadDump[i].getThreadId()));

			jsonArray.add(jsonObject);
		}

		return jsonArray;

	}

	private static JsonArray getDeadLockedThreads(long[] deadLockedThreads) {
		JsonArray jsonArray = new JsonArray();
		if (deadLockedThreads != null) {
			for (int i = 0; i < deadLockedThreads.length; i++) {
				jsonArray.add(deadLockedThreads[i]);
			}
		}
		return jsonArray;
	}

	public static JsonObject getThreadInfoJSONObject() {
		JsonObject jsonObject = new JsonObject();
		ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
		jsonObject.add(DEAD_LOCKED_THREAD, getDeadLockedThreads(threadMXBean.findDeadlockedThreads()));
		jsonObject.addProperty(THREAD_COUNT, threadMXBean.getThreadCount());
		jsonObject.addProperty(THREAD_PEEK_COUNT, threadMXBean.getPeakThreadCount());

		return jsonObject;
	}

	public static JsonObject getJVMMemoryInfoJSObject() {
		JsonObject jsonObject = new JsonObject();

		jsonObject.addProperty(MAX_MEMORY, SystemUtils.jvmMaxMemory());
		jsonObject.addProperty(TOTAL_MEMORY, SystemUtils.jvmTotalMemory());
		jsonObject.addProperty(FREE_MEMORY, SystemUtils.jvmFreeMemory());
		jsonObject.addProperty(IN_USE_MEMORY, SystemUtils.jvmInUseMemory());
		return jsonObject;
	}

	public static JsonObject getSystemInfoJSObject() {
		JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty(OS_NAME, SystemUtils.osName);
		jsonObject.addProperty(OS_ARCH, SystemUtils.osArch);
		jsonObject.addProperty(JAVA_VERSION, SystemUtils.jvmVersion);
		jsonObject.addProperty(PROCESSOR_COUNT, SystemUtils.osProcessorX);
		return jsonObject;
	}

	public static JsonObject getSysteMemoryInfoJSObject() {
		JsonObject jsonObject = new JsonObject();

		jsonObject.addProperty(VIRTUAL_MEMORY, SystemUtils.osCommittedVirtualMemory());
		jsonObject.addProperty(TOTAL_MEMORY, SystemUtils.osTotalPhysicalMemory());
		jsonObject.addProperty(FREE_MEMORY, SystemUtils.osFreePhysicalMemory());
		jsonObject.addProperty(IN_USE_MEMORY, SystemUtils.osInUsePhysicalMemory());

		//to handle the problem in raspberry pi4 + ubuntu 20.04
		try {
			jsonObject.addProperty(TOTAL_SWAP_SPACE, SystemUtils.osTotalSwapSpace());
			jsonObject.addProperty(FREE_SWAP_SPACE, SystemUtils.osFreeSwapSpace());
			jsonObject.addProperty(IN_USE_SWAP_SPACE, SystemUtils.osInUseSwapSpace());
		}catch (Exception e) {
			logger.error("swap memory statistic can not be read");
		}

		jsonObject.addProperty(AVAILABLE_MEMORY, SystemUtils.osAvailableMemory());

		return jsonObject;
	}

	public static JsonObject getJVMNativeMemoryInfoJSObject() {
		JsonObject jsonObject = new JsonObject();

		long maxPhysicalBytes = Pointer.maxPhysicalBytes();
		long inUsephysicalBytes = Pointer.physicalBytes();

		jsonObject.addProperty(IN_USE_JVM_NATIVE_MEMORY, inUsephysicalBytes);
		jsonObject.addProperty(MAX_JVM_NATIVE_MEMORY, maxPhysicalBytes);
		return jsonObject;
	}


	/**
	 * Returns server uptime and startime in milliseconds
	 * @return
	 */
	public static JsonObject getServerTime() 
	{
		JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty(StatsCollector.UP_TIME, ManagementFactory.getRuntimeMXBean().getUptime());
		jsonObject.addProperty(StatsCollector.START_TIME, ManagementFactory.getRuntimeMXBean().getStartTime());
		return jsonObject;
	}

	public static AdminApplication getAdminAppAdaptor(ApplicationContext appContext) {
		AdminApplication adaptor = null;

		if (appContext.containsBean(AntMediaApplicationAdapter.BEAN_NAME)) 
		{
			Object appHandler =appContext.getBean(AntMediaApplicationAdapter.BEAN_NAME);
			if (appHandler instanceof  AdminApplication) 
			{
				adaptor = (AdminApplication) appHandler;
			}
		}

		return adaptor;
	}

	public static AntMediaApplicationAdapter getAppAdaptor(ApplicationContext appContext) 
	{
		AntMediaApplicationAdapter adaptor = null;

		if (appContext.containsBean(AntMediaApplicationAdapter.BEAN_NAME)) 
		{
			Object appHandler =appContext.getBean(AntMediaApplicationAdapter.BEAN_NAME);
			if (appHandler instanceof  AntMediaApplicationAdapter) 
			{
				adaptor = (AntMediaApplicationAdapter) appHandler;
			}
		}

		return adaptor;
	}

	public static JsonObject getSystemResourcesInfo(Queue<IScope> scopes) 
	{
		JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty(INSTANCE_ID, Launcher.getInstanceId());
		jsonObject.add(CPU_USAGE, getCPUInfoJSObject());
		jsonObject.add(JVM_MEMORY_USAGE, getJVMMemoryInfoJSObject());
		jsonObject.add(SYSTEM_INFO, getSystemInfoJSObject());
		jsonObject.add(SYSTEM_MEMORY_INFO, getSysteMemoryInfoJSObject());
		jsonObject.add(FILE_SYSTEM_INFO, getFileSystemInfoJSObject());
		jsonObject.add(JVM_NATIVE_MEMORY_USAGE, getJVMNativeMemoryInfoJSObject());
		
		jsonObject.add(SOFTWARE_VERSION, gson.toJsonTree(RestServiceBase.getSoftwareVersion()));

		//add gpu info 
		jsonObject.add(StatsCollector.GPU_USAGE_INFO, StatsCollector.getGPUInfoJSObject());

		//add ffmpeg build info 
		jsonObject.addProperty(StatsCollector.FFMPEG_BUILD_INFO, FFmpegUtilities.getBuildConfiguration());

		int localHlsViewers = 0;
		int localDashViewers = 0;
		int localWebRTCViewers = 0;
		int localWebRTCStreams = 0;
		int localStreams = 0;
		int encodersBlocked = 0;
		int encodersNotOpened = 0;
		int publishTimeoutError = 0;
		if (scopes != null) {
			for (Iterator<IScope> iterator = scopes.iterator(); iterator.hasNext();) { 
				IScope scope = iterator.next();
				localHlsViewers += getHLSViewers(scope);
				localDashViewers += getDASHViewers(scope);

				if( scope.getContext().getApplicationContext().containsBean(IWebRTCAdaptor.BEAN_NAME)) {
					IWebRTCAdaptor webrtcAdaptor = (IWebRTCAdaptor)scope.getContext().getApplicationContext().getBean(IWebRTCAdaptor.BEAN_NAME);
					localWebRTCStreams += webrtcAdaptor.getNumberOfLiveStreams();
					localWebRTCViewers += webrtcAdaptor.getNumberOfTotalViewers();
				}

				AntMediaApplicationAdapter adaptor = null;

				if ((adaptor = getAppAdaptor(scope.getContext().getApplicationContext())) != null)
				{
					encodersBlocked += adaptor.getNumberOfEncodersBlocked();
					encodersNotOpened += adaptor.getNumberOfEncoderNotOpenedErrors();
					publishTimeoutError += adaptor.getNumberOfPublishTimeoutError();
					localStreams += adaptor.getMuxAdaptors().size();

				}
			}
		}

		//add local webrtc viewer size
		jsonObject.addProperty(StatsCollector.LOCAL_WEBRTC_LIVE_STREAMS, localWebRTCStreams);
		jsonObject.addProperty(StatsCollector.LOCAL_LIVE_STREAMS, localStreams);
		jsonObject.addProperty(StatsCollector.LOCAL_WEBRTC_VIEWERS, localWebRTCViewers);
		jsonObject.addProperty(StatsCollector.LOCAL_HLS_VIEWERS, localHlsViewers);	
		jsonObject.addProperty(StatsCollector.LOCAL_DASH_VIEWERS, localDashViewers);	
		jsonObject.addProperty(StatsCollector.ENCODERS_BLOCKED, encodersBlocked);
		jsonObject.addProperty(StatsCollector.ENCODERS_NOT_OPENED, encodersNotOpened);
		jsonObject.addProperty(StatsCollector.PUBLISH_TIMEOUT_ERRORS, publishTimeoutError);
		jsonObject.addProperty(StatsCollector.VERTX_WORKER_THREAD_QUEUE_SIZE, getVertWorkerQueueSizeStatic());
		jsonObject.addProperty(StatsCollector.WEBRTC_VERTX_WORKER_THREAD_QUEUE_SIZE, getWebRTCVertxWorkerQueueSizeStatic());

		//add timing info
		jsonObject.add(StatsCollector.SERVER_TIMING, getServerTime());

		return jsonObject;
	}

	private static int getHLSViewers(IScope scope) {
		if (scope.getContext().getApplicationContext().containsBean(HlsViewerStats.BEAN_NAME)) {
			HlsViewerStats hlsViewerStats = (HlsViewerStats) scope.getContext().getApplicationContext().getBean(HlsViewerStats.BEAN_NAME);
			return hlsViewerStats.getTotalViewerCount();
		}
		return 0;
	}

	private static int getDASHViewers(IScope scope) {
		if (scope.getContext().getApplicationContext().containsBean(DashViewerStats.BEAN_NAME)) {
			DashViewerStats dashViewerStats = (DashViewerStats) scope.getContext().getApplicationContext().getBean(DashViewerStats.BEAN_NAME);
			return dashViewerStats.getTotalViewerCount();
		}
		return 0;
	}

	public void sendInstanceStats(Queue<IScope> scopes) {

		JsonObject jsonObject = getSystemResourcesInfo(scopes);

		jsonObject.addProperty(TIME, DateTimeFormatter.ISO_INSTANT.format(Instant.now()));
		jsonObject.addProperty(HOST_ADDRESS, hostAddress);
		jsonObject.addProperty(IP_ADDRESS, ServerSettings.getGlobalHostAddress());

		send2Kafka(jsonObject, INSTANCE_STATS_TOPIC_NAME); 

	}

	public void send2Kafka(JsonElement jsonElement, String topicName) {
		ProducerRecord<Long, String> record = new ProducerRecord<>(topicName,
				gson.toJson(jsonElement));
		try {
			kafkaProducer.send(record).get();
		} 
		catch (ExecutionException e) {
			logger.error(ExceptionUtils.getStackTrace(e));
		} catch (InterruptedException e) {
			logger.error(ExceptionUtils.getStackTrace(e));
			Thread.currentThread().interrupt();
		}
	}

	public void addCpuMeasurement(int measurment) {
		cpuMeasurements.add(measurment);
		if(cpuMeasurements.size() > windowSize) {
			cpuMeasurements.poll();
		}

		int total = 0;
		for (int msrmnt : cpuMeasurements) {
			total += msrmnt;
		}		
		cpuLoad = total/cpuMeasurements.size();
	}
	


	
	public int getMemoryLoad() {
		long availableMemory = SystemUtils.osAvailableMemory();
		long totalMemory = SystemUtils.osTotalPhysicalMemory();
		return (int) (((double)(totalMemory - availableMemory) / totalMemory) * 100);
	}
	
	public int getOSType() {
		return SystemUtils.OS_TYPE;
	}

	@Override
	public boolean enoughResource(){

		boolean enoughResource = false;

		if(getCpuLoad() < getCpuLimit()) 
		{		
			if (getOSType() == SystemUtils.LINUX) {
				long memoryLoad = getMemoryLoad();
				enoughResource = memoryLoad < getMemoryLimit();
				if (!enoughResource)  
				{
					logger.error("Not enough resource. Due to memory limit. Memory usage should be less than %{} but it is %{}", getMemoryLimit(), memoryLoad);
				}
			}
			else {		
				int freeRam = getFreeRam();
				enoughResource = (freeRam > getMinFreeRamSize() || freeRam == -1) ;
				if (!enoughResource)  
				{
					logger.error("Not enough resource. Due to memory limit. Free memory should be more than {}MB but it is {}MB", getMinFreeRamSize(), freeRam);
				}
			}
		}
		else {
			logger.error("Not enough resource. Due to high cpu load: {} cpu limit: {}", cpuLoad, cpuLimit);
		}

		if (!enoughResource && StringUtils.isNotBlank(webhookURL)) {

			logger.info("Setting timer to call high resource usage hook.");
			vertx.setTimer(10, e -> 
			{ 
				try {
					JsonObject jsonObject = new JsonObject();
					jsonObject.addProperty("action", HOOK_HIGH_RESOURCE_USAGE);
					jsonObject.addProperty("host", hostAddress);
					jsonObject.addProperty("resourceInfo", getSystemResourcesInfo(scopes).toString());
					
					sendPOST(webhookURL, jsonObject);
				} catch (Exception ex) {
					//Make Exception generic
					logger.error(ExceptionUtils.getStackTrace(ex));
				}
			});
		}

		return enoughResource; 
	}


	public void sendUnexpectedShutdownHook(List<String> appNames) 
	{

		logger.info("Setting timer to call unexpected server shutdown hook.");
		vertx.setTimer(10, e -> {
			JsonObject jsonObject = new JsonObject();
			jsonObject.addProperty("action", HOOK_HIGH_RESOURCE_USAGE);
			jsonObject.addProperty("host", hostAddress);
			jsonObject.addProperty("appNames", String.join(",", appNames));
			try {
				sendPOST(webhookURL, jsonObject);
			} catch (Exception ex) {
				//Make Exception generic
				logger.error(ExceptionUtils.getStackTrace(ex));
			}
		});

	}


	public int sendPOST(String url, JsonObject json) throws IOException {

		int statusCode = -1;
		try (CloseableHttpClient httpClient = getHttpClient()) {
			HttpPost httpPost = new HttpPost(url);
			RequestConfig requestConfig = RequestConfig.custom()
					.setConnectTimeout(2000)
					.setConnectionRequestTimeout(2000)
					.setSocketTimeout(2000)
					.build();
			httpPost.setConfig(requestConfig);

			// Convert the variables map to a JSON object


			// Set the post request header and entity
			httpPost.setHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
			httpPost.setEntity(new StringEntity(json.toString()));

			try (CloseableHttpResponse httpResponse = httpClient.execute(httpPost)) {
				logger.info("POST Response Status:: {}", httpResponse.getStatusLine().getStatusCode());
				statusCode = httpResponse.getStatusLine().getStatusCode();				
			}
		}

		return statusCode;
	}



	@Override
	public int getFreeRam() {
		long availableMemory = SystemUtils.osAvailableMemory();
		if (availableMemory != 0) {
			return (int)SystemUtils.convertByteSize(availableMemory, "MB");
		}
		return -1;
	}

	@Override
	public int getMinFreeRamSize() {
		return minFreeRamSize;
	}

	public void setMinFreeRamSize(int ramLimit) {
		this.minFreeRamSize = ramLimit;
	}

	public void setCpuLoad(int cpuLoad) {
		this.cpuLoad = cpuLoad;
	}

	@Override
	public int getCpuLoad() {
		return cpuLoad;
	}


	public int getWindowSize() {
		return windowSize;
	}


	public void setWindowSize(int windowSize) {
		this.windowSize = windowSize;
	}


	public Vertx getVertx() {
		return vertx;
	}


	public void setVertx(Vertx vertx) {
		this.vertx = vertx;
		vertXMetrics= MetricsService.create(vertx);
	}

	public void setWebRTCVertx(Vertx webRTCVertx) {
		this.webRTCVertx = webRTCVertx;
		webRTCVertxMetrics =  MetricsService.create(webRTCVertx);
	}


	public void setCpuLimit(int cpuLimit) {
		if (cpuLimit > 100) {
			this.cpuLimit = 100;
		}
		else if (cpuLimit < 10) {
			this.cpuLimit = 10;
		}
		else {
			this.cpuLimit = cpuLimit;
		}
	}

	@Override
	public int getCpuLimit() {
		return cpuLimit;
	}

	@Override
	public void setApplicationContext(org.springframework.context.ApplicationContext applicationContext)
			throws BeansException {
		IServer server = (IServer) applicationContext.getBean(IServer.ID);
		server.addListener(new IScopeListener() {

			@Override
			public void notifyScopeRemoved(IScope scope) {
				scopes.remove(scope);
			}

			@Override
			public void notifyScopeCreated(IScope scope) {
				scopes.add(scope);
			}
		});

		ServerSettings serverSettings = (ServerSettings) applicationContext.getBean(ServerSettings.BEAN_NAME);
		heartBeatEnabled = serverSettings.isHeartbeatEnabled();
		hostAddress = serverSettings.getHostAddress();
		measurementPeriod = serverSettings.getCpuMeasurementPeriodMs();
		windowSize = serverSettings.getCpuMeasurementWindowSize();
		marketplace = serverSettings.getMarketplace();
		webhookURL = serverSettings.getServerStatusWebHookURL();

		licenseService = (ILicenceService) applicationContext.getBean(ILicenceService.BeanName.LICENCE_SERVICE.toString());

		setVertx((Vertx) applicationContext.getBean(IAntMediaStreamHandler.VERTX_BEAN_NAME));


		setWebRTCVertx((Vertx) applicationContext.getBean(WebSocketCommunityHandler.WEBRTC_VERTX_BEAN_NAME));

	}

	public int getStaticSendPeriod() {
		return staticSendPeriod;
	}

	public void setStaticSendPeriod(int staticSendPeriod) {
		this.staticSendPeriod = staticSendPeriod;
	}

	public void setKafkaProducer(Producer<Long, String> kafkaProducer) {
		this.kafkaProducer = kafkaProducer;
	}

	public String getKafkaBrokers() {
		return kafkaBrokers;
	}

	public void setKafkaBrokers(String kafkaBrokers) {
		this.kafkaBrokers = kafkaBrokers;
	}

	public void setScopes(Queue<IScope> scopes) {
		this.scopes = scopes;
	}

	public void cancelHeartBeat() {
		vertx.cancelTimer(hearbeatPeriodicTask);
	}

	public boolean isHeartBeatEnabled() {
		return heartBeatEnabled;
	}

	public void setHeartBeatEnabled(boolean heartBeatEnabled) {
		this.heartBeatEnabled = heartBeatEnabled;
	}

	public int getHeartbeatPeriodMs() {
		return heartbeatPeriodMs;
	}

	public void setHeartbeatPeriodMs(int heartbeatPeriodMs) {
		this.heartbeatPeriodMs = heartbeatPeriodMs;
	}

	@Override
	public void destroy() throws Exception {
		if(logger != null) {
			logger.info("Shutting down stats collector ");
		}

		if (heartBeatEnabled) 
		{  
			//send session end if heartBeatEnabled 
			if(logger != null) {
				logger.info("Ending analytic session");
			}
		}
		vertx.close();
		webRTCVertx.close();
		if(logger != null) {
			logger.info("Closing vertx ");
		}

	}

	public int getMeasurementPeriod() {
		return measurementPeriod;
	}

	public void startAnalytic() {

		String instanceId = Launcher.getInstanceId();
		String version = Launcher.getVersion();
		String type = Launcher.getVersionType();

		JsonObject instance = new JsonObject();
		instance.addProperty(INSTANCE_ID, instanceId);
		instance.addProperty(INSTANCE_TYPE, type);
		instance.addProperty(INSTANCE_VERSION, version);
		instance.addProperty(MARKETPLACE_NAME, marketplace);


		instance.addProperty(USER_EMAIL, getUserEmail());

		if (RestServiceBase.isEnterprise()) 
		{
			Licence lastLicenseStatus = licenseService.getLastLicenseStatus();
			String status = "invalid";
			if (lastLicenseStatus != null) 
			{
				status = lastLicenseStatus.getStatus();
			}
			instance.addProperty(LICENSE_VALID, status);
		}

		try (CloseableHttpClient client = getHttpClient())
		{
			HttpRequestBase post = (HttpRequestBase)RequestBuilder.post().setUri("https://us-central1-ant-media-server-analytics.cloudfunctions.net/sendHeartbeat").setHeader(HttpHeaders.CONTENT_TYPE, "application/json")
					.setEntity(new StringEntity(instance.toString())).build();

			RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(2 * 1000).setSocketTimeout(5*1000).build();

			post.setConfig(requestConfig);

			client.execute(post);

		}catch (IOException e) {
			logger.error("Couldn't connect Ant Media Server Analytics");
		} 
	}

	public void setUserEmail(String userEmail) {
		this.userEmail = userEmail;
	}

	public String getUserEmail() 
	{
		if (userEmail == null) 
		{
			for (Iterator<IScope> iterator = scopes.iterator(); iterator.hasNext();) 
			{ 
				IScope scope = iterator.next();


				AdminApplication adaptor = null;

				if ((adaptor = getAdminAppAdaptor(scope.getContext().getApplicationContext())) != null)
				{
					AbstractConsoleDataStore dataStore = adaptor.getDataStoreFactory().getDataStore();

					List<User> userList = dataStore.getUserList();

					userEmail = findAdminUser(userList);
					break;
				}
			}
		}
		return userEmail;
	}

	private String findAdminUser(List<User> userList) {

		String email = null;
		for (Iterator<User> iterator2 = userList.iterator(); iterator2.hasNext();) 
		{
			User user = iterator2.next();

			if (user.getUserType() == UserType.ADMIN && CommonRestService.SCOPE_SYSTEM.equals(user.getScope())) 
			{
				email = user.getEmail();
				break;
			}
		}
		return email;
	}

	public CloseableHttpClient getHttpClient() {
		return  HttpClients.custom().setRedirectStrategy(new LaxRedirectStrategy())
				.build();
	}

	public long getUnexpectedShutDownDelayMs() {
		return unexpectedShutDownDelayMs;
	}

	public void setUnexpectedShutDownDelayMs(long unexpectedShutDownDelayMs) {
		this.unexpectedShutDownDelayMs = unexpectedShutDownDelayMs;
	}

	public void setWebhookURL(String webhookURL) {
		this.webhookURL = webhookURL;
	}

	public int getMemoryLimit() {
		return memoryLimit;
	}

	public void setMemoryLimit(int memoryLimit) {
		if (memoryLimit > 100) {
			this.memoryLimit = 100;
		}
		else if (memoryLimit < 10) {
			this.memoryLimit = 10;
		}
		else {
			this.memoryLimit = memoryLimit;
		}
		
	}


}
