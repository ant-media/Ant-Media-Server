package io.antmedia.webrtc.datachannel;

public class DataChannelConstants {
	/*
	 * defines the event type sent to the client vie data channel
	 */
	public static final String EVENT_TYPE = "eventType";
	public static final String COMMAND = "command";
	public static final String COMMAND_TYPE_EVENT= "event";
	
	/*********************** VARIABLES ***********************/
	public static final String TRACK_ID = "trackId";
	public static final String STREAM_ID = "streamId";
	public static final String PAYLOAD = "payload";
	public static final String AUDIO_LEVEL = "audioLevel";
	public static final String VIDEO_TRACK_LABEL = "videoLabel";
	public static final String AUDIO_TRACK_LABEL = "audioLabel";
	public static final String NO = "no";
	public static final String RESERVED = "reserved";	
	public static final String ROLE = "role";
	public static final String SUBSCRIBER_ID = "subscriberId";
	public static final String SUBSCRIBER_NAME = "subscriberName";


	/*********************** EVENTS ***********************/
	public static final String AUDIO_TRACK_ASSIGNMENT = "AUDIO_TRACK_ASSIGNMENT";
	
	public static final String UPDATE_AUDIO_LEVEL = "UPDATE_AUDIO_LEVEL";

	public static final String UPDATE_PARTICIPANT_ROLE = "UPDATE_PARTICIPANT_ROLE";

	public static final String VIDEO_TRACK_ASSIGNMENT_LIST = "VIDEO_TRACK_ASSIGNMENT_LIST";
	public static final String TRACK_LIST_UPDATED = "TRACK_LIST_UPDATED";
	public static final String  PLAYER_ADDED = "PLAYER_ADDED";
	public static final String  PLAYER_REMOVED = "PLAYER_REMOVED";

}
