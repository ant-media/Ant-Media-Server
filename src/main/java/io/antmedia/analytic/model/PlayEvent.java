package io.antmedia.analytic.model;

public class PlayEvent extends AnalyticEvent {

	
	/**
	 * playStarted is generated both on the server side and client side
	 * 
	 * For the client side, when user paused and plays the video, it's triggered. It's both WebRTC, HLS and VoD
	 */
	public static final String EVENT_PLAY_STARTED = "playStarted";
	
	/**
	 * playEnded is generated both ton the server side and client side
	 * For the client side, it's received when play is ended. It's both WebRTC, HLS and VoD
	 */
	public static final String EVENT_PLAY_ENDED = "playEnded";
	
	/**
	 * playPaused event is received from the player
	 */
	public static final String EVENT_PLAY_PAUSED = "playPaused";
	
	/**
	 * playStartedFirstTime is received from the player. It's the first event when the user starts playing. 
	 * It's usefull to count total number of unique views.
	 */
	public static final String EVENT_PLAY_STARTED_FIRST_TIME = "playStartedFirstTime";

	private String protocol;
		
	private String clientIP;
	
	private String subscriberId;

	public String getProtocol() {
		return protocol;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	public String getSubscriberId() {
		return subscriberId;
	}

	public void setSubscriberId(String subscriberId) {
		this.subscriberId = subscriberId;
	}

	public String getClientIP() {
		return clientIP;
	}

	public void setClientIP(String clientIP) {
		this.clientIP = clientIP;
	}
}
