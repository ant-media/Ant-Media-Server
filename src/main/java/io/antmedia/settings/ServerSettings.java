package io.antmedia.settings;

import org.springframework.beans.factory.annotation.Value;

public class ServerSettings {
	
	public static final String BEAN_NAME = "ant.media.server.settings";

	
	private static final String SETTINGS_HEART_BEAT_ENABLED = "server.heartbeatEnabled";
	
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

}
