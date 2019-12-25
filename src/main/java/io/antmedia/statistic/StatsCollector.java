package io.antmedia.statistic;

import java.lang.management.ManagementFactory;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;

import org.apache.commons.lang3.exception.ExceptionUtils;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContextAware;

import com.brsanthu.googleanalytics.GoogleAnalytics;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.IApplicationAdaptorFactory;
import io.antmedia.SystemUtils;
import io.antmedia.rest.WebRTCClientStats;
import io.antmedia.settings.ServerSettings;
import io.antmedia.shutdown.AMSShutdownManager;
import io.antmedia.statistic.GPUUtils.MemoryStatus;
import io.antmedia.webrtc.api.IWebRTCAdaptor;
import io.vertx.core.Vertx;

public class StatsCollector implements IStatsCollector, ApplicationContextAware {	

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

	public static final String SYSTEM_CPU_LOAD = "systemCPULoad";

	public static final String PROCESS_CPU_TIME = "processCPUTime";

	public static final String CPU_USAGE = "cpuUsage";

	public static final String INSTANCE_ID = "instanceId";

	public static final String JVM_MEMORY_USAGE = "jvmMemoryUsage";

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

	public static final String TOTAL_LIVE_STREAMS = "totalLiveStreamSize";

	public static final String LOCAL_WEBRTC_LIVE_STREAMS = "localWebRTCLiveStreams";

	public static final String LOCAL_WEBRTC_VIEWERS = "localWebRTCViewers";

	public static final String LOCAL_HLS_VIEWERS = "localHLSViewers";

	private static final String TIME = "time";

	protected static final Logger logger = LoggerFactory.getLogger(StatsCollector.class);

	private static final String MEASURED_BITRATE = "measured_bitrate";

	private static final String SEND_BITRATE = "send_bitrate";

	private static final String AUDIO_FRAME_SEND_PERIOD = "audio_frame_send_period";

	private static final String VIDEO_FRAME_SEND_PERIOD = "video_frame_send_period";

	private static final String STREAM_ID = "streamId";

	private static final String WEBRTC_CLIENT_ID = "webrtcClientId";

	private Queue<IScope> scopes = new ConcurrentLinkedQueue<>();

	public static final String GA_TRACKING_ID = "UA-93263926-3";
	
	@Autowired
	private Vertx vertx;
	private Queue<Integer> cpuMeasurements = new ConcurrentLinkedQueue<>();

	Gson gson = new Gson();

	private int windowSize = 5;
	private int measurementPeriod = 5000;
	private int staticSendPeriod = 15000;
	
	private int cpuLoad;
	private int cpuLimit = 70;
	
	/**
	 * Min Free Ram Size that free memory should be always more than min
	 */
	private int minFreeRamSize = 20;

	private String kafkaBrokers = null;

	public static final String INSTANCE_STATS_TOPIC_NAME = "ams-instance-stats";

	public static final String WEBRTC_STATS_TOPIC_NAME = "ams-webrtc-stats";

	public static final String UP_TIME = "up-time";

	public static final String START_TIME = "start-time";

	public static final String SERVER_TIMING = "server-timing";

	private static final String ENCODERS_BLOCKED = "encoders-blocked";

	private static final String ENCODERS_NOT_OPENED = "encoders-not-opened";

	private static final String PUBLISH_TIMEOUT_ERRORS = "publish-timeout-errors";

	private Producer<Long,String> kafkaProducer = null;

	private long cpuMeasurementTimerId = -1;

	private long kafkaTimerId = -1;
	
	private boolean heartBeatEnabled = true;

	private long hearbeatPeriodicTask;
	
	private int heartbeatPeriodMs = 300000;
	
	public void start() {
		cpuMeasurementTimerId  = getVertx().setPeriodic(measurementPeriod, l -> addCpuMeasurement(SystemUtils.getSystemCpuLoad()));
		startKafkaProducer();
		
		if (heartBeatEnabled) {
			logger.info("Starting heartbeats for the version:{} and type:{}", Launcher.getVersion(), Launcher.getVersionType());
			startAnalytic(Launcher.getVersion(), Launcher.getVersionType());

			startHeartBeats(Launcher.getVersion(), Launcher.getVersionType(), heartbeatPeriodMs);
			
			notifyShutDown(Launcher.getVersion(), Launcher.getVersionType());
		}
		else {
			logger.info("Heartbeats are disabled for this instance");
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

	
	public GoogleAnalytics getGoogleAnalytic(String implementationVersion, String type) {
		return GoogleAnalytics.builder()
				.withAppVersion(implementationVersion)
				.withAppName(type)
				.withTrackingId(GA_TRACKING_ID).build();

	}
	
	private void sendWebRTCClientStats() {
		getVertx().executeBlocking(
				b -> {
					collectAndSendWebRTCClientsStats();
					b.complete();
				}, 
				r -> {

				});
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
		jsonObject.addProperty(TOTAL_SWAP_SPACE, SystemUtils.osTotalSwapSpace());
		jsonObject.addProperty(FREE_SWAP_SPACE, SystemUtils.osFreeSwapSpace());
		jsonObject.addProperty(IN_USE_SWAP_SPACE, SystemUtils.osInUseSwapSpace());
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
	
	
	public static JsonObject getSystemResourcesInfo(Queue<IScope> scopes) 
	{
		JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty(INSTANCE_ID, Launcher.getInstanceId());
		jsonObject.add(CPU_USAGE, getCPUInfoJSObject());
		jsonObject.add(JVM_MEMORY_USAGE, getJVMMemoryInfoJSObject());
		jsonObject.add(SYSTEM_INFO, getSystemInfoJSObject());
		jsonObject.add(SYSTEM_MEMORY_INFO, getSysteMemoryInfoJSObject());
		jsonObject.add(FILE_SYSTEM_INFO, getFileSystemInfoJSObject());

		//add gpu info 
		jsonObject.add(StatsCollector.GPU_USAGE_INFO, StatsCollector.getGPUInfoJSObject());

		int localHlsViewers = 0;
		int localWebRTCViewers = 0;
		int localWebRTCStreams = 0;
		int encodersBlocked = 0;
		int encodersNotOpened = 0;
		int publishTimeoutError = 0;
		if (scopes != null) {
			for (Iterator<IScope> iterator = scopes.iterator(); iterator.hasNext();) { 
				IScope scope = iterator.next();
				localHlsViewers += getHLSViewers(scope);

				if( scope.getContext().getApplicationContext().containsBean(IWebRTCAdaptor.BEAN_NAME)) {
					IWebRTCAdaptor webrtcAdaptor = (IWebRTCAdaptor)scope.getContext().getApplicationContext().getBean(IWebRTCAdaptor.BEAN_NAME);
					localWebRTCViewers += webrtcAdaptor.getNumberOfTotalViewers();
					localWebRTCStreams += webrtcAdaptor.getNumberOfLiveStreams();
				}
				AntMediaApplicationAdapter adaptor = ((IApplicationAdaptorFactory) scope.getContext().getApplicationContext().getBean(AntMediaApplicationAdapter.BEAN_NAME)).getAppAdaptor();
				encodersBlocked += adaptor.getNumberOfEncodersBlocked();
				encodersNotOpened += adaptor.getNumberOfEncoderNotOpenedErrors();
				publishTimeoutError += adaptor.getNumberOfPublishTimeoutError();
			}
		}

		//add local webrtc viewer size
		jsonObject.addProperty(StatsCollector.LOCAL_WEBRTC_LIVE_STREAMS, localWebRTCStreams);
		jsonObject.addProperty(StatsCollector.LOCAL_WEBRTC_VIEWERS, localWebRTCViewers);
		jsonObject.addProperty(StatsCollector.LOCAL_HLS_VIEWERS, localHlsViewers);	
		jsonObject.addProperty(StatsCollector.ENCODERS_BLOCKED, encodersBlocked);
		jsonObject.addProperty(StatsCollector.ENCODERS_NOT_OPENED, encodersNotOpened);
		jsonObject.addProperty(StatsCollector.PUBLISH_TIMEOUT_ERRORS, publishTimeoutError);
		
		//add timing info
		jsonObject.add(StatsCollector.SERVER_TIMING, getServerTime());

		return jsonObject;
	}

	private static int getHLSViewers(IScope scope) {
		if (scope.getContext().getApplicationContext().containsBean(HlsViewerStats.BEAN_NAME)) {
			HlsViewerStats hlsViewerStats = (HlsViewerStats) scope.getContext().getApplicationContext().getBean(HlsViewerStats.BEAN_NAME);
			if (hlsViewerStats != null) {
				return hlsViewerStats.getTotalViewerCount();
			}
		}
		return 0;
	}

	public void sendInstanceStats(Queue<IScope> scopes) {

		JsonObject jsonObject = getSystemResourcesInfo(scopes);

		jsonObject.addProperty(TIME, DateTimeFormatter.ISO_INSTANT.format(Instant.now()));

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


	@Override
	public boolean enoughResource(){

		boolean enoughResource = false;

		if(cpuLoad < cpuLimit) 
		{
			long freeJvmRamValue = getFreeRam();
			
			if (freeJvmRamValue > minFreeRamSize) 
			{
				long maxPhysicalBytes = Pointer.maxPhysicalBytes();
				long physicalBytes = Pointer.physicalBytes();
				if (maxPhysicalBytes > 0) 
				{
					long freeNativeMemory = SystemUtils.convertByteSize(maxPhysicalBytes - physicalBytes, "MB"); 
					if (freeNativeMemory > minFreeRamSize )
					{
						enoughResource = true;
					}
					else {
						logger.error("Not enough resource. Due to no enough native memory. Current free memory:{} min free memory:{}", freeNativeMemory, minFreeRamSize);
					}
					
				}
				else {
					//if maxPhysicalBytes is not reported, just proceed
					enoughResource = true;
				}
				
			}
			else {
				logger.error("Not enough resource. Due to not free RAM. Free RAM should be more than  {} but it is: {}", minFreeRamSize, freeJvmRamValue);
			}
		}
		else {
			logger.error("Not enough resource. Due to high cpu load: {} cpu limit: {}", cpuLoad, cpuLimit);
		}

		return enoughResource; 
	}

	@Override
	public int getFreeRam() {
		//return the allocatable free ram which means max memory - inuse memory
		//inuse memory means total memory - free memory
		long inuseMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
		return (int)SystemUtils.convertByteSize(Runtime.getRuntime().maxMemory() - inuseMemory, "MB");
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
	
	public boolean startHeartBeats(String implementationVersion, String type, int periodMS) {
		boolean result = false;

		hearbeatPeriodicTask = vertx.setPeriodic(periodMS, 
			l -> {
				if(logger != null) {
					logger.info("-Heartbeat-");
				}
				else {
					System.out.println("-Heartbeat-");
				}
				getGoogleAnalytic(implementationVersion, type).event()
				.eventCategory("server_status")
				.eventAction("heartbeat")
				.eventLabel("")
				.clientId(Launcher.getInstanceId())
				.send();
			}
		);
		
		return result;
	}

	public void startAnalytic(String implementationVersion, String type) {
		vertx.runOnContext(l -> 
			getGoogleAnalytic(implementationVersion, type).screenView()
			.sessionControl("start")
			.clientId(Launcher.getInstanceId())
			.send()
		);
	}
	
	public boolean notifyShutDown(String implementationVersion, String type) {
		boolean result = false;

		Runtime.getRuntime().addShutdownHook(new Thread() {

			@Override
			public void run() {
				
				if(logger != null) {
					logger.info("Shutting down just a sec");
				}
				AMSShutdownManager.getInstance().notifyShutdown();
				getGoogleAnalytic(implementationVersion, type).screenView()
				.clientId(Launcher.getInstanceId())
				.sessionControl("end")
				.send();
			}
		});
		result = true;
		return result;
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
	
}
