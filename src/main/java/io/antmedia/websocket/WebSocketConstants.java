package io.antmedia.websocket;

public class WebSocketConstants {
	

	private WebSocketConstants() {
	}
	
	public static final String ATTR_STREAM_NAME = "ATTR_STREAM_NAME";

	public static final String ATTR_ROOM_NAME = "ATTR_ROOM_NAME";
	
	public static final String NOTIFICATION_COMMAND = "notification";
	
	public static final String PING_COMMAND = "ping";
	
	public static final String PONG_COMMAND = "pong";
	
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
	
	public static final String PUBLISH_COMMAND = "publish";

	public static final String PUBLISH_STARTED = "publish_started";

	public static final String PUBLISH_FINISHED = "publish_finished";

	public static final String ERROR_CODE = "error_code";

	public static final String NO_STREAM_EXIST = "no_stream_exist";

	public static final String JOIN_ROOM_COMMAND = "joinRoom";

	public static final String ROOM = "room";

	public static final String JOIN_COMMAND = "join";
	
	public static final String SERVER_WILL_STOP = "server_will_stop";
	
	public static final String TRACK_ID = "trackId";

	public static final String ENABLED = "enabled";
	
	/**
	 * this is for leaving from room in 1-N and N-N connection
	 */
	
	public static final String LEAVE_THE_ROOM = "leaveFromRoom";

	public static final String JOINED_THE_ROOM = "joinedTheRoom";
	
	public static final String LEAVED_THE_ROOM = "leavedFromRoom";
	
	/**
	 * this token is used to access resources or start broadcast when token security is enabled
	 */
	
	public static final String TOKEN = "token";
	
	
	/**
	 * this subscriber id is used to access resources or start broadcast when time based subscriber security is enabled
	 */
	public static final String SUBSCRIBER_ID = "subscriberId";
	
	/**
	 * this subscriber code is used to access resources or start broadcast when time based subscriber security is enabled
	 */
	public static final String SUBSCRIBER_CODE = "subscriberCode";
	
	
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
	 * This is sent back to the user if publisher wants to send a stream with an unregistered id
	 * and server is configured not to allow this kind of streams
	 */
	public static final String NOT_ALLOWED_UNREGISTERED_STREAM = "not_allowed_unregistered_streams";
	
	/**
	 * This is sent back to the user when there is no room specified in 
	 * joining the video conference
	 */
	public static final String NO_ROOM_SPECIFIED = "no_room_specified";
	
	/**
	 * This is sent back to the user when context is not initialized yet
	 */
	public static final String NOT_INITIALIZED_YET = "not_initialized_yet";
	
	/**
	 * This is sent back to the user when there is no room specified in 
	 * joining the video conference
	 */
	public static final String ROOM_TIME_INVALID = "room_not_active_or_expired";
	
	
	/**
	 * This is sent back to the user when stream plannedStartDate and plannedEndDate 
	 * values are in interval or not.
	 */
	public static final String STREAM_TIME_INVALID = "stream_not_active_or_expired";
	
	
	public static final String UNAUTHORIZED = "unauthorized_access";
	
	/**
	 * This is sent back to the user when a new play message received while
	 * it is playing or it is just to play 
	 */
	public static final String ALREADY_PLAYING = "already_playing";

	/**
	 * This is sent back to the user when a new publish message received while
	 * there it's publishing or it's just to publish
	 */
	public static final String ALREADY_PUBLISHING = "already_publishing";

	/**
	 * This is sent back to the user when there is no codec enabled in the server 
	 * and someone try to make a publish
	 */
	public static final String NO_CODEC_ENABLED_IN_THE_SERVER = "no_codec_enabled_in_the_server";
	
	/**
	 * Command that let server returns information about a specific stream.
	 * This info includes height, bitrates, etc.
	 */
	public static final String GET_STREAM_INFO_COMMAND = "getStreamInfo";
	
	/**
	 * Command that stream set resolution request.
	 */
	public static final String FORCE_STREAM_QUALITY = "forceStreamQuality";
	
  /**
	 * Command that let server returns information about a specific room.
	 * This info includes stream ID's list in room.
	 */
	public static final String GET_ROOM_INFO_COMMAND = "getRoomInfo";

	/**
	 * Notification field used when returning stream information
	 */
	public static final String STREAM_INFORMATION_NOTIFICATION = "streamInformation";
	
	/**
	 * Notification field used when returning room information
	 */
	public static final String ROOM_INFORMATION_NOTIFICATION = "roomInformation";

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
	 * Field to specify the measured bitrate for a WebRTCClient
	 */
	public static final String TARGET_BITRATE = "targetBitrate";
	
	/**
	 * Field to specify the stream video codec
	 */
	public static final String VIDEO_CODEC = "videoCodec";

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

	/**
	 * Error definition it is send when publisher tries to publish with in use stream id
	 */
	public static final String STREAM_ID_IN_USE = "streamIdInUse";
	/**
	 * Error definition it is send when cpu usage exceeds the limit
	 */
	public static final String HIGH_RESOURCE_USAGE = "highResourceUsage";
	/**
	 * Error definition it is send when viewer limit reached
	 */
	public static final String VIEWER_LIMIT_REACHED = "viewerLimitReached";
	/**
	 * Error definition it is send when stream name contains special characters
	 */
	public static final String INVALID_STREAM_NAME = "invalidStreamName";
	
	/**
	 * Error definition, it's send when video encoder is not opened 
	 */
	public static final String ENCODER_NOT_OPENED = "encoderNotOpened";

	/**
	 * Error definition, it's send when video encoder is blocked
	 */
	public static final String ENCODER_BLOCKED = "encoderBlocked";

	/**
	 * Error definition, it's send when publishing has not started and timeout 
	 */
	public static final String PUBLISH_TIMEOUT_ERROR = "publishTimeoutError";

	/**
	 * Error definition, it's send when remote description is not set, it's generally due to 
	 * encoder incompatibilities
	 */
	public static final Object NOT_SET_REMOTE_DESCRIPTION = "notSetRemoteDescription";
	
	/**
	 * P2P Mode used in session user parameters
	 */
	public static final String ATTR_P2P_MULTIPEER = "multiPeer";
	
	/**
	 * P2P Mode used in session user parameters
	 */
	public static final String ATTR_P2P_MODE = "mode";
	
	/**
	 * P2P Mode play
	 */
	public static final String P2P_MODE_PLAY = "play";
	
	/**
	 * P2P Mode both
	 */
	public static final String P2P_MODE_BOTH = "both";

	
	/**
	 * This command used for P2P connections with multipeers 
	 * to connect new peers to generated new connections with desired id
	 */
	public static final String CONNECT_WITH_NEW_ID_COMMAND = "connectWithNewId";
	
	/**
	 * This command used for P2P connections with multipeers 
	 * to start new connection with desired id
	 */
	public static final String START_NEW_P2P_CONNECTION_COMMAND = "startNewP2PConnection";
	
	/**
	 * This command used for message and data transfer between peers 
	 */
	public static final String PEER_MESSAGE_COMMAND = "peerMessageCommand";
	/**
	 * This command used for multitrack stream 
	 * to play or pause a tranck
	 */
	public static final String ENABLE_TRACK = "enableTrack";
	
	/**
	 * This command used to get subtracks for a stream id
	 */
	public static final String  GET_TRACK_LIST = "getTrackList";

	/**
	 * This command used to send subtracks for a stream id
	 */
	public static final String  TRACK_LIST = "trackList";

	/**
	 * Notification to send measured bitrate
	 */
	public static final String BITRATE_MEASUREMENT = "bitrateMeasurement";

	/**
	 * Error definition. It's sent when data store is not available. 
	 * It's not available if it's closed or not available;
	 */
	public static final String DATA_STORE_NOT_AVAILABLE = "data_store_not_available";

	/**
	 * It's send when community handler does not start streaming
	 */
	public static final String SERVER_ERROR_CHECK_LOGS = "server_error_check_logs";

}
