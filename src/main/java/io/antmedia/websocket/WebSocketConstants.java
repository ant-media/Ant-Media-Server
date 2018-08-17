package io.antmedia.websocket;

public class WebSocketConstants {
	
	private WebSocketConstants() {
	}
	
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

	public static final String PUBLISH_COMMAND = "publish";

	public static final String PUBLISH_STARTED = "publish_started";

	public static final String PUBLISH_FINISHED = "publish_finished";

	public static final String ERROR_CODE = "error_code";

	public static final String NO_STREAM_EXIST = "no_stream_exist";

	public static final String JOIN_ROOM_COMMAND = "joinRoom";

	public static final String ROOM = "room";

	public static final String JOIN_COMMAND = "join";

	public static final String JOINED_THE_ROOM = "joinedTheRoom";
	
	/**
	 * this token is used to access resources or start broadcast when token security is enabled
	 */
	
	public static final String TOKEN = "token";

	/**
	 * This is peer to peer connection error definition.
	 * It is sent back to the user when there is no peer associated with the stream
	 */
	public static final String NO_PEER_ASSOCIATED = "no_peer_associated_before";

	/**
	 * This is peer to peer connection notification.
	 * It is sent back to the user when it is joined to a stream
	 */
	public static final String JOINED_THE_STREAM = "joined";

	/**
	 * This is p2p connection command
	 * Peer send it to leave the room
	 */
	public static final String LEAVE_COMMAND = "leave";

	/**
	 * This is peer to peer connection notification
	 * ıt is sent back to the suser when it is leaved from the stream
	 */
	public static final String LEAVED_STREAM = "leaved";

	/**
	 * This is sent back to the user when there is no encoder settings available
	 * in publishing the stream
	 */
	public static final String NO_ENCODER_SETTINGS = "no_encoder_settings";

	/**
	 * This is sent back to the user if publisher wants to send a stream with an unregistered id
	 * and server is configured not to allow this kind of streams
	 */
	public static final String NOT_ALLOWED_UNREGISTERED_STREAM = "not_allowed_unregistered_streams";
	
	/**
	 * This is sent back to the user when there is no room specified in 
	 * joining the video conference
	 */
	public static final String NO_ROOM_SPECIFIED = "no_room_specified";
	
	
	public static final String UNAUTHORIZED = "unauthorized_access";

	/**
	 * Command that let server returns information about a specific stream.
	 * This info includes height, bitrates, etc.
	 */
	public static final String GET_STREAM_INFO_COMMAND = "getStreamInfo";

	/**
	 * Notification field used when returning stream information
	 */
	public static final String STREAM_INFORMATION_NOTIFICATION = "streamInformation";

	/**
	 * Field in messaging to specify the stream info 
	 */
	public static final String STREAM_INFO = "streamInfo";

	/**
	 * Field to specify the stream width
	 */
	public static final String STREAM_WIDTH = "streamWidth";

	/**
	 * Field to specify the stream height
	 */
	public static final String STREAM_HEIGHT = "streamHeight";

	/**
	 * Field to specify the stream video bitrate
	 */
	public static final String VIDEO_BITRATE = "videoBitrate";

	/**
	 * Field to specify the stream audio bitrate
	 */
	public static final String AUDIO_BITRATE = "audioBitrate";

	/**
	 * video field that defines if there is video
	 */
	public static final String VIDEO = "video";
	
	/**
	 * audio field that defines if there is audio
	 */
	public static final String AUDIO = "audio";

	/**
	 * Stream list in the room
	 */
	public static final String STREAMS_IN_ROOM = "streams";

	/**
	 * Error definition it is send when stream id is not specified in the message
	 */
	public static final String NO_STREAM_ID_SPECIFIED = "noStreamNameSpecified";

	/**
	 * Error definition it is send when local description is not set successfully
	 */
	public static final String NOT_SET_LOCAL_DESCRIPTION = "notSetLocalDescription";

}
