package io.antmedia.settings;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.bytedeco.ffmpeg.global.avutil;
import org.red5.server.tomcat.TomcatConnector;
import org.red5.server.tomcat.TomcatLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.PropertySource;
import org.apache.catalina.util.NetMask;
import org.webrtc.Logging;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.antmedia.licence.ILicenceService;

@PropertySource("/conf/red5.properties")
@JsonIgnoreProperties(ignoreUnknown=true)
public class ServerSettings implements ApplicationContextAware, Serializable {

	private static final long serialVersionUID = 1L;

	public static final String BEAN_NAME = "ant.media.server.settings";

	private static final String SETTINGS_HEART_BEAT_ENABLED = "server.heartbeatEnabled";

	private static final String SETTINGS_USE_GLOBAL_IP = "useGlobalIp";

	private static final String SETTINGS_PROXY_ADDRESS = "proxy.address";

	private static final String SETTINGS_NODE_GROUP = "nodeGroup";

	public static final String LOG_LEVEL_ALL = "ALL";
	public static final String LOG_LEVEL_TRACE = "TRACE";
	public static final String LOG_LEVEL_DEBUG = "DEBUG";
	public static final String LOG_LEVEL_INFO = "INFO";
	public static final String LOG_LEVEL_WARN = "WARN";
	public static final String LOG_LEVEL_ERROR = "ERROR";
	public static final String LOG_LEVEL_OFF = "OFF";

	public static final String DEFAULT_NODE_GROUP = "default";


	private static final String SETTINGS_CPU_MEASUREMENT_PERIOD_MS = "server.cpu_measurement_period_ms";

	private static final String SETTINGS_CPU_MEASUREMENT_WINDOW_SIZE = "server.cpu_measurement_window_size";

	private static final String SETTINGS_SERVER_DEFAULT_HTTP_PORT = "http.port";

	private static final String SETTINGS_ORIGIN_PORT = "server.origin_port";

	private static final String SETTINGS_SRT_PORT = "server.srt_port";

	private static final String SETTINGS_RTMP_PORT = "rtmp.port";

	private static final String ALLOWED_DASH_BOARD_CIDR = "server.allowed_dashboard_CIDR";

	private static final String SETTINGS_NATIVE_LOG_LEVEL = "nativeLogLevel";

	private static final String SETTINGS_LOG_LEVEL = "logLevel";

	private static final String SETTINGS_LICENSE_KEY = "server.licence_key";

	private static final String SETTINGS_SERVER_NAME = "server.name";

	private static final String SETTINGS_MARKET_PLACE_NAME = "server.marketplace";

	public static final String SETTINGS_JWT_SERVER_SECRET_KEY = "server.jwtServerSecretKey";

	/** jwt server filter control*/
	public static final String SETTINGS_JWT_SERVER_CONTROL_ENABLED = "server.jwtServerControlEnabled";

	public static final String SETTINGS_JWKS_URL = "server.jwksURL";

	private static final String SETTINGS_SERVER_STATUS_WEBHOOK_URL = "server.statusWebHookURL";
	
	private static final String RTMPS_ENABLED = "rtmps.enabled";

	private static final String LOCAL_LICENCE_SERVER_IP = "server.localLicenceServerIp";

	/**
	 * The IP filter that is allowed to access the web panel of Ant Media Server
	 */
	@Value( "${"+ALLOWED_DASH_BOARD_CIDR+":'0.0.0.0/0'}" )
	private String allowedDashboardCIDR;

	@JsonIgnore
	private transient Queue<NetMask> allowedCIDRList = new ConcurrentLinkedQueue<>();


	private static Logger logger = LoggerFactory.getLogger(ServerSettings.class);

	private static String localHostAddress;

	private static String globalHostAddress;

	private String hostAddress;

	/**
	 * Fully Qualified Domain Name of the Server. 
	 * It's used in SSL as well.
	 */
	@Value( "${"+SETTINGS_SERVER_NAME+":#{null}}" )
	private String serverName;

	/**
	 * Customer License Key
	 */
	@Value( "${"+SETTINGS_LICENSE_KEY+":#{null}}" )
	private String licenceKey;

	/**
	 * The setting for customized marketplace build.
	 * It's initialized by getting the value from the LicenceBean
	 */
	private boolean buildForMarket = false;

	/**
	 * Name of the marketplace
	 */
	@Value( "${"+SETTINGS_MARKET_PLACE_NAME+":#{null}}" )
	private String marketplace;


	@Value( "${"+SETTINGS_LOG_LEVEL+":'INFO'}" )
	private String logLevel = null;

	/**
	 * if the license is offline. It checks license key against hardware
	 * So license key should be provided by Ant Media specifically.
	 * It's initialized by getting the value from the LicenceBean
	 */
	private boolean offlineLicense = false;

	/**
	 * Native Log Level is used for ffmpeg and WebRTC logs
	 */
	@Value( "${"+SETTINGS_NATIVE_LOG_LEVEL+":'ERROR'}" )
	private String nativeLogLevel = LOG_LEVEL_ERROR;

	/**
	 * Enable heart beat for Ant Media Server
	 */
	@Value( "${"+SETTINGS_HEART_BEAT_ENABLED+":true}" )
	private boolean heartbeatEnabled; 

	/**
	 * Use global IP address for especially in cluster communication
	 */
	@Value( "${"+SETTINGS_USE_GLOBAL_IP+":false}" )
	private boolean useGlobalIp;

	/**
	 * The proxy IP address and port for license checking. 
	 * If there is a proxy in front of Ant Media Server(reverse proxy) please enter its IP and port
	 * The format will be <proxy_ip>:<port_number> for example:
	 * 					 192.168.0.1:3012
	 */
	@Value( "${"+SETTINGS_PROXY_ADDRESS+":#{null}}" )
	private String proxyAddress;


	@Value( "${"+SETTINGS_NODE_GROUP+":"+DEFAULT_NODE_GROUP+"}" )
	private String nodeGroup = DEFAULT_NODE_GROUP;

	private Logging.Severity webrtcLogLevel = Logging.Severity.LS_WARNING;

	/**
	 * CPU load is measured for every period and this measurement is used to understand 
	 * if server has enough CPU to handle new requests
	 */
	@Value( "${"+SETTINGS_CPU_MEASUREMENT_PERIOD_MS+":1000}" )
	private int cpuMeasurementPeriodMs;


	/**
	 * Measured CPU load are added to a list with this size and average of the measure CPU loads
	 * are calculated. It's used to check CPU has enough CPU resource
	 */
	@Value( "${"+SETTINGS_CPU_MEASUREMENT_WINDOW_SIZE+":5}" )
	private int cpuMeasurementWindowSize;

	/**
	 * Server default HTTP port
	 * It's 5080 by default
	 */
	@Value( "${"+SETTINGS_SERVER_DEFAULT_HTTP_PORT+":5080}" )
	private int defaultHttpPort;

	/**
	 * Server JWT Control Enabled to access the REST API of the web panel
	 */
	@Value( "${"+SETTINGS_JWT_SERVER_CONTROL_ENABLED+":false}" )
	private boolean jwtServerControlEnabled;

	/**
	 * Server JWT secret key
	 * "afw7Zz9MqvLiheA5X3GFEKvLWb1JTKC2"
	 *
	 */
	@Value( "${"+SETTINGS_JWT_SERVER_SECRET_KEY+":#{null}}" )
	private String jwtServerSecretKey;

	/*
	 * JWKS URL - it's effective if {@link#jwtControlEnabled} is true
	 *
	 * It's null by default. If it's not null, JWKS is used to filter.
	 * Otherwise it uses JWT
	 */

	@Value( "${" + SETTINGS_JWKS_URL +":#{null}}")
	private String jwksURL;

	/**
	 * The port that is opened by origin in cluster mode.
	 * Edges are connected to the origin through this port.
	 */
	@Value( "${"+SETTINGS_ORIGIN_PORT+":5000}" )
	private int originServerPort;

	/**
	 * The SRT port that server opens to listen incoming SRT connections
	 */
	@Value("${"+SETTINGS_SRT_PORT + ":4200}")
	private int srtPort = 4200;

	/**
	 * Nme of the application which will ingestthe SRT Streams that don't have streamid.
	 */
	@Value( "${appIngestsSrtStreamsWithoutStreamId:LiveApp}" )
	private String appIngestsSrtStreamsWithoutStreamId="LiveApp";

	private boolean sslEnabled = false;
	
	/**
	 * RTMPS enabled or not.
	 * Pay attention that we inject value for setter method {@link #setRtmpsEnabled}  
	 */
	private static boolean rtmpsEnabled = false;
	
	/**
	 * The RTMP port that server opens to listen incoming RTMP connections
	 */
	@Value("${"+SETTINGS_RTMP_PORT + ":1935}")
	private int rtmpPort = 1935;


	/**
	 * Server status webhook url. It's called for several errors such 
	 * - high resource usage
	 * - Unexpected shutdown
	 */
	@Value("${"+SETTINGS_SERVER_STATUS_WEBHOOK_URL + ":#{null}}")
	private String serverStatusWebHookURL;


	/**
	 * Customer License Key
	 */
	@Value( "${"+LOCAL_LICENCE_SERVER_IP+":#{null}}" )
	private String localLicenceServerIps;


	public String getJwksURL() {
		return jwksURL;
	}

	public void setJwksURL(String jwksURL) {
		this.jwksURL = jwksURL;
	}

	public boolean isBuildForMarket() {
		return buildForMarket;
	}

	public void setBuildForMarket(boolean buildForMarket) {
		this.buildForMarket = buildForMarket;
	}

	public String getLicenceKey() {
		return licenceKey;
	}

	public void setLicenceKey(String licenceKey) {
		this.licenceKey = licenceKey;
	}

	public String getServerName() {
		return serverName;
	}

	public void setServerName(String serverName) {
		this.serverName = serverName;
	}

	public String getLogLevel() {
		return logLevel;
	}
	public void setLogLevel(String logLevel) {
		this.logLevel = logLevel;
	}

	public boolean isHeartbeatEnabled() {
		return heartbeatEnabled;
	}

	public void setHeartbeatEnabled(boolean heartbeatEnabled) {
		this.heartbeatEnabled = heartbeatEnabled;
	}

	public  String getHostAddress() {
		if (hostAddress == null ) {
			//which means that context is not initialized yet so that return localhost address
			logger.warn("ServerSettings is not initialized yet so that return local host address: {}", getLocalHostAddress());
			return getLocalHostAddress();
		}
		return hostAddress;
	}

	public static String getGlobalHostAddress()
	{
		if (globalHostAddress == null) 
		{
			try (InputStream in = new URL("http://checkip.amazonaws.com").openStream()){

				globalHostAddress = IOUtils.toString(in, Charset.defaultCharset()).trim();
			} catch (IOException e) {
				logger.error(ExceptionUtils.getStackTrace(e));
			}		
		}

		return globalHostAddress;
	}
	
	public static Enumeration<NetworkInterface> getNetworkInterfaces() throws SocketException {
		return NetworkInterface.getNetworkInterfaces();
	}

	public static InetAddress getNoneLoopbackHostAddress() 
	{

		InetAddress noneLoopbackAddress = null;
		try (final DatagramSocket socket = new DatagramSocket())
		{
			SocketAddress sockaddr = new InetSocketAddress("8.8.8.8", 10002); // no need to have 8.8.8.8 reachable and port is not important
			socket.connect(sockaddr);
			noneLoopbackAddress = socket.getLocalAddress();;

		} 
		catch (Exception e) {
		  logger.error(ExceptionUtils.getStackTrace(e));
		}

		return noneLoopbackAddress;


	}

	public static String getLocalHostAddress() {

		if (localHostAddress == null) {
			long startTime = System.currentTimeMillis();
			try {
				/*
				 * InetAddress.getLocalHost().getHostAddress() takes long time(5sec in macos) to return.
				 * Let it is run once
				 */
				InetAddress noneLoopbackHostAddress = getNoneLoopbackHostAddress();
				if (noneLoopbackHostAddress != null) 
				{
					logger.info("localhost address is set to none loopback address: {}", noneLoopbackHostAddress.getHostAddress());
					localHostAddress = noneLoopbackHostAddress.getHostAddress();
				}
				else 
				{

					localHostAddress = InetAddress.getLocalHost().getHostAddress(); 
					logger.info("localhost address is set to default localhost address: {}", localHostAddress);
				}
			} catch (UnknownHostException e) {
				logger.error(ExceptionUtils.getStackTrace(e));
			}
			long diff = System.currentTimeMillis() - startTime;
			if (diff > 1000) {
				logger.warn("Getting host adress took {}ms. it's cached now and will return immediately from now on. You can "
						+ " alternatively set serverName in conf/red5.properties file ", diff);
			}
		}


		return localHostAddress;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {

		String hostAddressEnv = getHostAddressFromEnvironment();
		if (hostAddressEnv != null && !hostAddressEnv.isEmpty()) {
			logger.info("Env host address is {}", hostAddressEnv);
			hostAddress = hostAddressEnv;
		}

		else if (useGlobalIp) {
			hostAddress = getGlobalHostAddress();
			logger.info("Using global host address is {}", hostAddress);
		}
		else {
			//************************************
			//this method may sometimes takes long to return
			//delaying initialization may cause some after issues
			hostAddress = getLocalHostAddress();
			logger.info("Using local host address is {}", hostAddress);
		}

		if (applicationContext.containsBean("tomcat.server")) {
			TomcatLoader tomcatLoader = (TomcatLoader) applicationContext.getBean("tomcat.server");

			List<TomcatConnector> connectors = tomcatLoader.getConnectors();
			for (TomcatConnector tomcatConnector : connectors) {
				if (tomcatConnector.isSecure()) {
					this.sslEnabled = true;
					break;
				}
			}
		}

		if (applicationContext.containsBean(ILicenceService.BeanName.LICENCE_SERVICE.toString())) 
		{

			ILicenceService licenseService = (ILicenceService) applicationContext.getBean(ILicenceService.BeanName.LICENCE_SERVICE.toString());

			if (ILicenceService.LICENCE_TYPE_MARKETPLACE.equals(licenseService.getLicenseType())) {
				buildForMarket = true;
			}
			else if (ILicenceService.LICENCE_TYPE_OFFLINE.equals(licenseService.getLicenseType())) {
				offlineLicense = true;
			}
		}
		setNativeLogLevel(nativeLogLevel);

	}

	public String getHostAddressFromEnvironment() {
		return System.getenv("AMS_HOST_ADDRESS");
	}

	public boolean isUseGlobalIp() {
		return useGlobalIp;
	}

	public void setUseGlobalIp(boolean useGlobalIp) {
		this.useGlobalIp = useGlobalIp;
	}

	public void setProxyAddress(String proxyAddress){
		this.proxyAddress = proxyAddress;
	}

	public String getProxyAddress(){
		return proxyAddress;
	}

	/**
	 * the getAllowedCIDRList and setAllowedCIDRList are synchronized because
	 * ArrayList may throw concurrent modification
	 * 
	 * @param allowedDashboardCIDR
	 */
	public void setAllowedDashboardCIDR(String allowedDashboardCIDR) {
		this.allowedDashboardCIDR = allowedDashboardCIDR;
		allowedCIDRList = new ConcurrentLinkedQueue<>();
		fillFromInput(allowedDashboardCIDR, allowedCIDRList);
	}

	public String getAllowedDashboardCIDR() {
		return allowedDashboardCIDR;
	}

	@JsonIgnore
	public Queue<NetMask> getAllowedCIDRList() {
		if (allowedCIDRList.isEmpty()) {
			fillFromInput(allowedDashboardCIDR, allowedCIDRList);
		}
		return allowedCIDRList;
	}

	/**
	 * Fill a {@link NetMask} list from a string input containing a comma-separated
	 * list of (hopefully valid) {@link NetMask}s.
	 *
	 * @param input  The input string
	 * @param target The list to fill
	 * @return a string list of processing errors (empty when no errors)
	 */
	private List<String> fillFromInput(final String input, final Queue<NetMask> target) {
		target.clear();
		if (input == null || input.isEmpty()) {
			return Collections.emptyList();
		}

		final List<String> messages = new LinkedList<>();
		NetMask nm;

		for (final String s : input.split("\\s*,\\s*")) {
			try {
				nm = new NetMask(s);
				target.add(nm);
			} catch (IllegalArgumentException e) {
				messages.add(s + ": " + e.getMessage());
			}
		}

		return Collections.unmodifiableList(messages);
	}

	public String getNativeLogLevel() {
		return nativeLogLevel;
	}

	public void setNativeLogLevel(String nativeLogLevel) {
		this.nativeLogLevel = nativeLogLevel;
		switch (this.nativeLogLevel) 
		{
		case LOG_LEVEL_ALL:
		case LOG_LEVEL_TRACE:
			webrtcLogLevel = Logging.Severity.LS_VERBOSE;
			avutil.av_log_set_level(avutil.AV_LOG_TRACE);			
			break;
		case LOG_LEVEL_DEBUG:
			webrtcLogLevel = Logging.Severity.LS_VERBOSE;
			avutil.av_log_set_level(avutil.AV_LOG_DEBUG);
			break;
		case LOG_LEVEL_INFO:
			webrtcLogLevel = Logging.Severity.LS_INFO;
			avutil.av_log_set_level(avutil.AV_LOG_INFO);
			break;
		case LOG_LEVEL_WARN:
			webrtcLogLevel = Logging.Severity.LS_WARNING;
			avutil.av_log_set_level(avutil.AV_LOG_WARNING);
			break;
		case LOG_LEVEL_ERROR:
			webrtcLogLevel = Logging.Severity.LS_ERROR;
			avutil.av_log_set_level(avutil.AV_LOG_ERROR);
			break;
		case LOG_LEVEL_OFF:
			webrtcLogLevel = Logging.Severity.LS_NONE;
			avutil.av_log_set_level(avutil.AV_LOG_QUIET);
			break;
		default:
			this.nativeLogLevel = LOG_LEVEL_WARN;
			webrtcLogLevel = Logging.Severity.LS_WARNING;
			avutil.av_log_set_level(avutil.AV_LOG_WARNING);
		}

	}

	public Logging.Severity getWebRTCLogLevel() {
		return webrtcLogLevel;
	}

	public String getNodeGroup() {
		return nodeGroup;
	}

	public void setNodeGroup(String nodeGroup) {
		this.nodeGroup = nodeGroup;
	}

	public int getCpuMeasurementPeriodMs() {
		return cpuMeasurementPeriodMs;
	}

	public void setCpuMeasurementPeriodMs(int cpuMeasurementPeriodMs) {
		this.cpuMeasurementPeriodMs = cpuMeasurementPeriodMs;
	}

	public int getCpuMeasurementWindowSize() {
		return cpuMeasurementWindowSize;
	}

	public void setCpuMeasurementWindowSize(int cpuMeasurementWindowSize) {
		this.cpuMeasurementWindowSize = cpuMeasurementWindowSize;
	}

	public int getDefaultHttpPort() {
		return defaultHttpPort;
	}

	public void setDefaultHttpPort(int defaultHttpPort) {
		this.defaultHttpPort = defaultHttpPort;
	}

	public int getOriginServerPort() {
		return originServerPort;
	}

	public void setOriginServerPort(int originServerPort) {
		this.originServerPort = originServerPort;
	}

	public int getSrtPort() {
		return srtPort;
	}

	public void setSrtPort(int srtPort) {
		this.srtPort = srtPort;
	}
	public int getRtmpPort() {
		return rtmpPort;
	}

	public String getMarketplace() {
		return marketplace;
	}

	public void setMarketplace(String marketplace) {
		this.marketplace = marketplace;
	}

	public String getJwtServerSecretKey() {
		return jwtServerSecretKey;
	}

	public void setJwtServerSecretKey(String jwtServerSecretKey){
		this.jwtServerSecretKey=jwtServerSecretKey;
	}

	public boolean isJwtServerControlEnabled() {
		return jwtServerControlEnabled;
	}

	public void setJwtServerControlEnabled(boolean jwtServerControlEnabled) {
		this.jwtServerControlEnabled = jwtServerControlEnabled;
	}

	public boolean isSslEnabled() {
		return sslEnabled;
	}

	public void setSslEnabled(boolean sslEnabled) {
		this.sslEnabled = sslEnabled;
	}

	public String getServerStatusWebHookURL() {
		return serverStatusWebHookURL;
	}


	public void setServerStatusWebHookURL(String serverStatusWebHookURL) {
		this.serverStatusWebHookURL = serverStatusWebHookURL;
	}

	public boolean isOfflineLicense() {
		return offlineLicense;
	}

	public void setOfflineLicense(boolean offlineLicense) {
		this.offlineLicense = offlineLicense;
	}

	public String getAppIngestsSrtStreamsWithoutStreamId() {
		return appIngestsSrtStreamsWithoutStreamId;
	}

	public void setAppIngestsSrtStreamsWithoutStreamId(String appIngestsSrtStreamsWithoutStreamId) {
		this.appIngestsSrtStreamsWithoutStreamId = appIngestsSrtStreamsWithoutStreamId;
	}

	public static boolean isRtmpsEnabled() {
		return ServerSettings.rtmpsEnabled;
	}

	/**
	 * Set the RTMPS enabled or not
	 * java:S2696 is suppressed because this method is used by Spring 
	 * @param rtmpsEnabled
	 */
	@Value("${"+RTMPS_ENABLED + ":false}")
	@SuppressWarnings("java:S2696")
	public void setRtmpsEnabled(boolean rtmpsEnabled) {
		ServerSettings.rtmpsEnabled = rtmpsEnabled;
	}

	public String getLocalLicenceServerIps() {
		return localLicenceServerIps;
	}

	public void setLocalLicenceServerIps(String localLicenceServerIps) {
		this.localLicenceServerIps = localLicenceServerIps;
	}
}
