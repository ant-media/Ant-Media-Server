package io.antmedia.analytic.model;

public class PlayerStatsEvent extends AnalyticEvent {
	
	public static final String EVENT_PLAYER_STATS = "playerStats";
	
	private long totalBytesTransferred;
	
	private String protocol;
	
	private long byteTransferred;
	
	private String uri;
	
	private String clientIP;
	
	public PlayerStatsEvent() {
		setEvent(EVENT_PLAYER_STATS);
	}

	public long getTotalBytesTransferred() {
		return totalBytesTransferred;
	}

	public void setTotalBytesTransferred(long totalBytesTransferred) {
		this.totalBytesTransferred = totalBytesTransferred;
	}

	public String getProtocol() {
		return protocol;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	public long getByteTransferred() {
		return byteTransferred;
	}

	public void setByteTransferred(long byteTransferred) {
		this.byteTransferred = byteTransferred;
	}

	public String getClientIP() {
		return clientIP;
	}

	public void setClientIP(String clientIP) {
		this.clientIP = clientIP;
	}



}
