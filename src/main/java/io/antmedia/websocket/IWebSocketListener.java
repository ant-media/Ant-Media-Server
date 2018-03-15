package io.antmedia.websocket;

public interface IWebSocketListener {
	
	
	public static final String ATTR_STREAM_NAME = "ATTR_STREAM_NAME";

	public static final String ATTR_ROOM_NAME = "ATTR_ROOM_NAME";
	
	public static final String NOTIFICATION_COMMAND = "notification";
	
	public static final String COMMAND = "command";

	public static final String ATTR_SIGNALLING_CONNECTION = "ATTR_SIGNALLING_CONNECTION";

	public static final String STREAM_ID = "streamId";

	public static final String DEFINITION = "definition";

	public static final String CANDIDATE_LABEL = "label";

	public static final String SDP = "sdp";

	public static final String TYPE = "type";

	public static final String PLAY_FINISHED = "play_finished";

	public static final String PLAY_STARTED = "play_started";

	public static final String CANDIDATE_ID = "id";

	public static final String CANDIDATE_SDP = "candidate";

	public static final String TAKE_CONFIGURATION_COMMAND = "takeConfiguration";

	public static final String TAKE_CANDIDATE_COMMAND = "takeCandidate";

	public static final String ERROR_COMMAND = "error";

	public static final String PLAY_COMMAND = "play";

	public static final String STOP_COMMAND = "stop";
	
	public static final String START_COMMAND = "start";

	public static final String STREAM_LEAVED = "streamLeaved";
	
	public static final String STREAM_JOINED = "streamJoined";

}
