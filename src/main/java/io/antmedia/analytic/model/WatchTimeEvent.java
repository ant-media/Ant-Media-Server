package io.antmedia.analytic.model;

/**
 * Event received from player.
 * It's received periodically from the player side
 * It's good to calculate the total amount of watch time of the stream(live, vod) and
 * which parts of the video watch most
 * 
 * Assume that we this event with startTimeMs: 10000 and watchTimeMs: 5000
 * it means that User has watched the video for 5 seconds and it's between 10 secs - 15 secs of the video
 * 
 * @author mekya
 *
 */
public class WatchTimeEvent extends PlayEvent {

	public static final String EVENT_WATCH_TIME = "watchTime";
	
	/**
	 * The amount of duration where user watched this video. 
	 */
	private long watchTimeMs;

	
	/**
	 * The starting time of the video where user starts watching. It's the part of video time. 
	 */
	private long startTimeMs;
	
	
	public WatchTimeEvent() {
		setEvent(EVENT_WATCH_TIME);
	}

	public long getStartTimeMs() {
		return startTimeMs;
	}

	public void setStartTimeMs(long startTimeMs) {
		this.startTimeMs = startTimeMs;
	}

	public long getWatchTimeMs() {
		return watchTimeMs;
	}

	public void setWatchTimeMs(long watchTimeMs) {
		this.watchTimeMs = watchTimeMs;
	}
	
	

}
