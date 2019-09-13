package io.antmedia.settings;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.Charset;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class ServerSettings implements ApplicationContextAware {
	
	public static final String BEAN_NAME = "ant.media.server.settings";

	
	private static final String SETTINGS_HEART_BEAT_ENABLED = "server.heartbeatEnabled";
	
	private static final String SETTINGS_USE_GLOBAL_IP = "useGlobalIp";

	
	private static Logger logger = LoggerFactory.getLogger(ServerSettings.class);

	private static String localHostAddress;
	
	private static String globalHostAddress;
	
	private static String hostAddress;
	
	/**
	 * Fully Qualified Domain Name
	 */
	private String serverName;
	/**
	 * Customer License Key
	 */
	private String licenceKey;
	
	/**
	 * The setting for customized marketplace build
	 */
	private boolean buildForMarket = false;
	
	
	
	private String logLevel = null;
	
	@Value( "${"+SETTINGS_HEART_BEAT_ENABLED+":true}" )
	private boolean heartbeatEnabled; 
	
	@Value( "${"+SETTINGS_USE_GLOBAL_IP+":false}" )
	private boolean useGlobalIp;

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
	
	//TODO: "static"  should be removed
	public static String getHostAddress() {
		return hostAddress;
	}
	
	public static String getGlobalHostAddress(){
		
		if (globalHostAddress == null) {
			InputStream in = null;
			try {
				in = new URL("http://checkip.amazonaws.com").openStream();
				globalHostAddress = IOUtils.toString(in, Charset.defaultCharset()).trim();
			} catch (IOException e) {
				logger.error(ExceptionUtils.getStackTrace(e));
			}
			finally {
				IOUtils.closeQuietly(in);
			}
		}

		return globalHostAddress;
	}
	
	public static String getLocalHostAddress() {

		if (localHostAddress == null) {
			long startTime = System.currentTimeMillis();
			try {
				/*
				 * InetAddress.getLocalHost().getHostAddress() takes long time(5sec in macos) to return.
				 * Let it is run once
				 */
				localHostAddress = InetAddress.getLocalHost().getHostAddress();
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
	
		if (useGlobalIp) {
			hostAddress = getGlobalHostAddress();
		}
		else {
			//************************************
			//this method may sometimes takes long to return
			//delaying initialization may cause some after issues
			hostAddress = getLocalHostAddress();
		}
		
	}

	public boolean isUseGlobalIp() {
		return useGlobalIp;
	}

	public void setUseGlobalIp(boolean useGlobalIp) {
		this.useGlobalIp = useGlobalIp;
	}

}
