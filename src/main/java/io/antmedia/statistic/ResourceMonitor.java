package io.antmedia.statistic;

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
import org.red5.server.Launcher;
import org.red5.server.api.IServer;
import org.red5.server.api.listeners.IScopeListener;
import org.red5.server.api.scope.IScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContextAware;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.antmedia.IResourceMonitor;
import io.antmedia.SystemUtils;
import io.antmedia.rest.WebRTCClientStats;
import io.antmedia.statistic.GPUUtils.MemoryStatus;
import io.antmedia.webrtc.api.IWebRTCAdaptor;
import io.vertx.core.Vertx;

public class ResourceMonitor implements IResourceMonitor, ApplicationContextAware {	

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

	protected static final Logger logger = LoggerFactory.getLogger(ResourceMonitor.class);

	private static final String MEASURED_BITRATE = "measured_bitrate";

	private static final String SEND_BITRATE = "send_bitrate";

	private static final String AUDIO_FRAME_SEND_PERIOD = "audio_frame_send_period";

	private static final String VIDEO_FRAME_SEND_PERIOD = "video_frame_send_period";

	private static final String STREAM_ID = "streamId";

	private static final String WEBRTC_CLIENT_ID = "webrtcClientId";

	private ConcurrentLinkedQueue<IScope> scopes = new ConcurrentLinkedQueue<>();

	@Autowired
	private Vertx vertx;
	private Queue<Integer> cpuMeasurements = new ConcurrentLinkedQueue<>();

	Gson gson = new Gson();

	private int windowSize = 5;
	private int measurementPeriod = 5000;
	private int staticSendPeriod = 15000;
	private int avgCpuUsage;
	private int cpuLimit = 70;

	private String kafkaBrokers = null;

	private String instanceStatsTopicName = "ams-instance-stats";
	
	private String webRTCStatsTopicName = "ams-webrtc-stats";

	private Producer<Long,String> kafkaProducer = null;

	public void start() {
		startKafkaProducer();
		getVertx().setPeriodic(measurementPeriod, l -> addCpuMeasurement(SystemUtils.getSystemCpuLoad()));
	}

	private void startKafkaProducer() {
		if (kafkaBrokers != null && !kafkaBrokers.isEmpty()) {
			kafkaProducer = createKafkaProducer();		

			getVertx().setPeriodic(staticSendPeriod, l -> {
				sendInstanceStats(scopes);
				sendWebRTCClientStats();
			});
		}	
	}

	private void sendWebRTCClientStats() {
		getVertx().executeBlocking(
				b -> {
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
					b.complete();
				}, 
				r -> {

				});
		
	}

	private void sendWebRTCClientStats2Kafka(List<WebRTCClientStats> webRTCClientStatList, String streamId) {
		JsonArray jsonArray = new JsonArray();
		JsonObject jsonObject;
		String dateTime = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
		for (WebRTCClientStats webRTCClientStat : webRTCClientStatList) 
		{
			jsonObject = new JsonObject();
			jsonObject.addProperty(STREAM_ID, streamId);
			jsonObject.addProperty(WEBRTC_CLIENT_ID, webRTCClientStat.getClientId());
			jsonObject.addProperty(AUDIO_FRAME_SEND_PERIOD, webRTCClientStat.getAudioFrameSendPeriod());
			jsonObject.addProperty(VIDEO_FRAME_SEND_PERIOD, webRTCClientStat.getVideoFrameSendPeriod());
			jsonObject.addProperty(MEASURED_BITRATE, webRTCClientStat.getMeasuredBitrate());
			jsonObject.addProperty(SEND_BITRATE, webRTCClientStat.getSendBitrate());
			jsonObject.addProperty(TIME, dateTime);
			jsonArray.add(jsonObject);
		}
		
		send2Kafka(jsonArray, webRTCStatsTopicName);
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
		jsonObject.addProperty("usableSpace", SystemUtils.osHDUsableSpace(null,"B", false));
		jsonObject.addProperty("totalSpace", SystemUtils.osHDTotalSpace(null, "B", false));
		jsonObject.addProperty("freeSpace", SystemUtils.osHDFreeSpace(null,  "B", false));
		jsonObject.addProperty("inUseSpace", SystemUtils.osHDInUseSpace(null, "B", false));
		return jsonObject;
	}


	private static JsonObject getGPUInfoJSObject(int deviceIndex) {
		JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty(GPU_DEVICE_INDEX, deviceIndex);
		jsonObject.addProperty(GPU_UTILIZATION, GPUUtils.getInstance().getGPUUtilization(deviceIndex));
		jsonObject.addProperty(GPU_MEMORY_UTILIZATION, GPUUtils.getInstance().getMemoryUtilization(deviceIndex));
		MemoryStatus memoryStatus = GPUUtils.getInstance().getMemoryStatus(deviceIndex);
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
				jsonArray.add(getGPUInfoJSObject(i));
			}
		}
		return jsonArray;
	}

	public static JsonObject getCPUInfoJSObject() {
		JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty("processCPUTime", SystemUtils.getProcessCpuTime());
		jsonObject.addProperty("systemCPULoad", SystemUtils.getSystemCpuLoad());
		jsonObject.addProperty("processCPULoad", SystemUtils.getProcessCpuLoad());
		return jsonObject;
	}

	public static JsonObject getJVMMemoryInfoJSObject() {
		JsonObject jsonObject = new JsonObject();

		jsonObject.addProperty("maxMemory", SystemUtils.jvmMaxMemory("B", false));
		jsonObject.addProperty("totalMemory", SystemUtils.jvmTotalMemory("B", false));
		jsonObject.addProperty("freeMemory", SystemUtils.jvmFreeMemory("B", false));
		jsonObject.addProperty("inUseMemory", SystemUtils.jvmInUseMemory("B", false));
		return jsonObject;
	}

	public static JsonObject getSystemInfoJSObject() {
		JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty("osName", SystemUtils.osName);
		jsonObject.addProperty("osArch", SystemUtils.osArch);
		jsonObject.addProperty("javaVersion", SystemUtils.jvmVersion);
		jsonObject.addProperty("processorCount", SystemUtils.osProcessorX);
		return jsonObject;
	}

	public static JsonObject getSysteMemoryInfoJSObject() {
		JsonObject jsonObject = new JsonObject();

		jsonObject.addProperty("virtualMemory", SystemUtils.osCommittedVirtualMemory("B", false));
		jsonObject.addProperty("totalMemory", SystemUtils.osTotalPhysicalMemory("B", false));
		jsonObject.addProperty("freeMemory", SystemUtils.osFreePhysicalMemory("B", false));
		jsonObject.addProperty("inUseMemory", SystemUtils.osInUsePhysicalMemory("B", false));
		jsonObject.addProperty("totalSwapSpace", SystemUtils.osTotalSwapSpace("B", false));
		jsonObject.addProperty("freeSwapSpace", SystemUtils.osFreeSwapSpace("B", false));
		jsonObject.addProperty("inUseSwapSpace", SystemUtils.osInUseSwapSpace("B", false));
		return jsonObject;
	}


	public static JsonObject getSystemResourcesInfo(Queue<IScope> scopes) {
		JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty(INSTANCE_ID, Launcher.getInstanceId());
		jsonObject.add(CPU_USAGE, getCPUInfoJSObject());
		jsonObject.add(JVM_MEMORY_USAGE, getJVMMemoryInfoJSObject());
		jsonObject.add(SYSTEM_INFO, getSystemInfoJSObject());
		jsonObject.add(SYSTEM_MEMORY_INFO, getSysteMemoryInfoJSObject());
		jsonObject.add(FILE_SYSTEM_INFO, getFileSystemInfoJSObject());

		//add gpu info 
		jsonObject.add(ResourceMonitor.GPU_USAGE_INFO, ResourceMonitor.getGPUInfoJSObject());

		int localHlsViewers = 0;
		int localWebRTCViewers = 0;
		int localWebRTCStreams = 0;
		for (Iterator<IScope> iterator = scopes.iterator(); iterator.hasNext();) { 
			IScope scope = iterator.next();
			localHlsViewers += getHLSViewers(scope);

			if( scope.getContext().getApplicationContext().containsBean(IWebRTCAdaptor.BEAN_NAME)) {
				IWebRTCAdaptor webrtcAdaptor = (IWebRTCAdaptor)scope.getContext().getApplicationContext().getBean(IWebRTCAdaptor.BEAN_NAME);
				localWebRTCViewers += webrtcAdaptor.getNumberOfTotalViewers();
				localWebRTCStreams += webrtcAdaptor.getNumberOfLiveStreams();
			}
		}

		//add local webrtc viewer size
		jsonObject.addProperty(ResourceMonitor.LOCAL_WEBRTC_LIVE_STREAMS, localWebRTCStreams);
		jsonObject.addProperty(ResourceMonitor.LOCAL_WEBRTC_VIEWERS, localWebRTCViewers);
		jsonObject.addProperty(ResourceMonitor.LOCAL_HLS_VIEWERS, localHlsViewers);

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

		send2Kafka(jsonObject, instanceStatsTopicName); 

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
		avgCpuUsage = total/cpuMeasurements.size();
	}


	@Override
	public int getAvgCpuUsage() {
		return avgCpuUsage;
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
		this.cpuLimit = cpuLimit;
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
	}

	public int getStaticSendPeriod() {
		return staticSendPeriod;
	}

	public void setStaticSendPeriod(int staticSendPeriod) {
		this.staticSendPeriod = staticSendPeriod;
	}

	public String getKafkaBrokers() {
		return kafkaBrokers;
	}

	public void setKafkaBrokers(String kafkaBrokers) {
		this.kafkaBrokers = kafkaBrokers;
	}
}
