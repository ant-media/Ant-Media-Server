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
	
	public static final String STREAM_NAME = "streamName";

	public static final String DEFINITION = "definition";

	public static final String CANDIDATE_LABEL = "label";

	public static final String SDP = "sdp";

	public static final String TYPE = "type";

	public static final String PLAY_FINISHED = "play_finished";

	public static final String PLAY_STARTED = "play_started";

	public static final String CANDIDATE_ID = "id";

	public static final String TOGGLE_AUDIO_COMMAND = "toggleAudio";

	public static final String TOGGLE_VIDEO_COMMAND = "toggleVideo";

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

	public static final String LINK_SESSION = "linkSession";

	public static final String REGISTER_ORIGIN_SERVER = "registerOriginServer";

	public static final String REGISTER_EDGE_SERVER = "registerEdgeServer";

	public static final String REGISTER_BROADCAST = "registerBroadcast";

	public static final String NO_STREAM_EXIST = "no_stream_exist";

	public static final String JOIN_ROOM_COMMAND = "joinRoom";
	
	/**
	 * This is the command that is sent from the server when a stream is started so that player can send a play command
	 * or take any action
	 */
	public static final String STREAMING_STARTED = "streaming_started";

	/**
	 * Command to get ICE server configuration to frontend from server
	 */
	public static final String GET_ICE_SERVER_CONFIG = "getIceServerConfig";

	public static final String ICE_SERVER_CONFIG_NOTIFICATION = "iceServerConfig";

	public static final String STUN_SERVER_URI = "stunServerUri";

	public static final String TURN_SERVER_USERNAME = "turnServerUsername";

	public static final String TURN_SERVER_CREDENTIAL = "turnServerCredential";

	/**
	 * Please use {@link #MAIN_TRACK} instead
	 */
	@Deprecated(forRemoval = true, since = "2.11.3")
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
	 * This is error definition and it's sent when one requests to get room information
	 * and there is no active stream or no room
	 */
	public static final String ROOM_NOT_ACTIVE = "no_active_streams_in_room";
	
	
	/**
	 * this token is used to access resources or start broadcast when token security is enabled
	 */
	
	public static final String TOKEN = "token";
	
	
	/**
	 * this subscriber id is used to access resources or start broadcast when time based subscriber security is enabled
	 */
	public static final String SUBSCRIBER_ID = "subscriberId";
	
	/**
	 * this subscriber name is the human readable name for a subscriber
	 */
	public static final String SUBSCRIBER_NAME = "subscriberName";
	
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
	 * This is sent back to the user if mainTrack 
	 */
	public static final String MAX_SUBTRACK_COUNT_REACHED = "main_track_has_max_subtrack_count__not_allowed_to_add_more_subtracks";

	
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
	
	
	/**
	 * This is sent back to the user if token is not valid
	 */
	public static final String UNAUTHORIZED = "unauthorized_access";
	
	/**
	 * This is sent back to the user when subscriber is blocked to play or publish
	 */
	public static final String BLOCKED = "user_blocked";
	
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
	 * Command that client stream change resolution notication.
	 */
	public static final String RESOLUTION_CHANGE_INFO_COMMAND = "resolutionChangeInfo";
	
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
	 * Stream Id list in the room
	 */
	public static final String STREAMS_IN_ROOM = "streams";
	
	/**
	 * Stream Name list in the room
	 */
	public static final String STREAM_LIST_IN_ROOM = "streamList";

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
	public static final String NOT_SET_REMOTE_DESCRIPTION = "notSetRemoteDescription";
	
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
	 * It's sent when community handler does not start streaming
	 */
	public static final String SERVER_ERROR_CHECK_LOGS = "server_error_check_logs";

	/**
	 * Free text info for the viewer
	 */
	public static final String VIEWER_INFO = "viewerInfo";
	/**
	 * It's sent when license is suspended
	 */
	public static final String LICENCE_SUSPENDED = "license_suspended_please_renew_license";
	
	/**
	 * It's sent to determine mainTrackId if exists
	 */
	public static final String MAIN_TRACK = "mainTrack";


	/**
	 * It's sent as parameter conference mode
	 */
	public static final String MODE = "mode";
	
	/**
	 * It's sent for conference in MCU mode
	 */
	public static final String MCU = "mcu";
	
	/**
	 * It's sent for conference in Audio Only MCU mode
	 */
	public static final String AMCU = "amcu";
	
	/**
	 * It's sent for conference in MCU mode
	 */
	public static final String MULTI_TRACK = "multitrack";
	
	/**
	 * It's sent for conference in legacy mode
	 */
	public static final String LEGACY = "legacy";

	/**
	 * It's sent for the restored webrtc publish sessions
	 */
	public static final String SESSION_RESTORED_DESCRIPTION = "session_restored";
	
	/**
	 * It's the field that maps sdp mid to stream id
	 */
	public static final String ID_MAPPING = "idMapping";

	/**
	 * It can be used to add some meta data to a broadcast
	 */
	public static final String META_DATA = "metaData";
	
	/**
	 * Command to update the meta data for a broadcast
	 */
	public static final String UPDATE_STREAM_META_DATA_COMMAND = "updateStreamMetaData";

	/**
	 * Command to inform AMS if a stream is pinned in conference mode
	 */
	public static final String ASSIGN_VIDEO_TRACK_COMMAND = "assignVideoTrackCommand";
	
	/**
	 * Command to change visible streams in conference mode, used for pagination
	 */
	public static final String UPDATE_VIDEO_TRACK_ASSIGNMENTS_COMMAND = "updateVideoTrackAssignmentsCommand";
	
	/**
	 * Command to set max video track count in conference
	 */
	public static final String SET_MAX_VIDEO_TRACK_COUNT_COMMAND = "setMaxVideoTrackCountCommand";
	
	/**
	 * Command to get debug info in conference
	 */
	public static final String GET_DEBUG_INFO_COMMAND = "getDebugInfo";
	
	/**
	 * Generated debug info in conference
	 */
	public static final String DEBUG_INFO = "debugInfo";

	/**
	 * Track id that is pinned for a stream
	 */
	public static final String VIDEO_TRACK_ID = "videoTrackId";
	
	/**
	 * Start index of a list for pagination
	 */
	public static final String OFFSET = "offset";
	
	/**
	 * Length of a page for pagination
	 */
	public static final String SIZE = "size";
	
	/**
	 * maximum number of tracks 
	 */
	public static final String MAX_TRACK_COUNT = "maxTrackCount";

	/**
	 * Command to get broadcast object
	 */
	public static final String GET_BROADCAST_OBJECT_COMMAND = "getBroadcastObject";

	/**
	 * Command to get video track assignments
	 */
	public static final String GET_VIDEO_TRACK_ASSIGNMENTS_COMMAND = "getVideoTrackAssignmentsCommand";

	/**
	 * broadcast object notification
	 */
	public static final String BROADCAST_OBJECT_NOTIFICATION = "broadcastObject";

	/**
	 * broadcast object constant
	 */
	public static final String BROADCAST = "broadcast";


	public static final String SEND_PUSH_NOTIFICATION_COMMAND = "sendPushNotification";

	public static final String REGISTER_PUSH_NOTIFICATION_TOKEN_COMMAND = "registerPushNotificationToken";

	public static final String AUTH_TOKEN_NOT_VALID_ERROR_DEFINITION = "authenticationTokenNotValid";

	/**
	 * Push Notificaiton Service Registration Token
	 */
	public static final String PNS_REGISTRATION_TOKEN = "pnsRegistrationToken";

	/**
	 * Push Notificaiton Service type, it can fcm or apn
	 * FCM: Firebase Cloud Messaging
	 * APN: Apple Notification Service
	 */
	public static final String PNS_TYPE = "pnsType";

	public static final String MISSING_PARAMETER_DEFINITION = "missingParameter";

	/**
	 * Information field in websocket communication
	 */
	public static final String INFORMATION = "information";

	/**
	 * Success field in websocket communication. If it's value true, the operation is successful.
	 * If it's value is false, the operation is failed
	 */
	public static final String SUCCESS = "success";

	/**
	 * Topic field to send push notification
	 */
	public static final String PUSH_NOTIFICATION_TOPIC = "pushNotificationTopic";

	/**
	 * Subscriber id list to send push notification
	 */
	public static final String SUBSCRIBER_ID_LIST_TO_NOTIFY = "subscriberIdsToNotify";

	/**
	 * Push Notification Content
	 */
	public static final String PUSH_NOTIFICATION_CONTENT = "pushNotificationContent";

	/**
	 * Participant role in the room
	 */
	public static final String ROLE = "role";
	
	/**
	 * Makes tracks disabled by default
	 */
	public static final String DISABLE_TRACKS_BY_DEFAULT = "disableTracksByDefault";

	/**
	 * Command to get subtrack infos for a main track
	 */
	public static final String GET_SUBTRACKS_COMMAND = "getSubtracks";

	/**
	 * Command to get subtrack count for a main track
	 */
	public static final String GET_SUBTRACKS_COUNT_COMMAND = "getSubtracksCount";

	/**
	 * subtrack (broadcast) object list notification
	 */
	public static final String SUBTRACK_LIST_NOTIFICATION = "subtrackList";
	
	
	/**
	 * Command to get subscriber list size
	 */
	public static final String GET_SUBSCRIBER_LIST_SIZE = "getSubscriberCount";
	
	/**
	 * subscriber count notification
	 */
	public static final String SUBSCRIBER_COUNT = "subscriberCount";
	
	/**
	 * Command to get subscribers for a stream
	 */
	public static final String GET_SUBSCRIBER_LIST = "getSubscribers";

	/**
	 * subscribers list notification
	 */
	public static final String SUBSCRIBER_LIST_NOTIFICATION = "subscriberList";

	/**
	 * status field in websocket communication
	 */
	public static final String STATUS = "status";
	
	/**
	 * sort field used for sorting subtracks
	 */
	public static final String SORT_BY = "sortBy";

	/**
	 * order (asc, desc) field used for ordering subtracks
	 */
	public static final String ORDER_BY = "orderBy";
	
	/**
	 * search field used for searching subtracks
	 */
	public static final String SEARCH = "search";


	/*
	 * count field in websocket communication
	 */
	public static final String COUNT = "count";

	/**
	 * subtrack (broadcast) object count notification
	 */
	public static final String SUBTRACK_COUNT_NOTIFICATION = "subtrackCount";

	/**
	 * subtrack (broadcast) object list
	 */
	public static final String SUBTRACK_LIST = "subtrackList";
	
	/**
	 * subscribers list
	 */
	public static final String SUBCRIBER_LIST = "subscriberList";

	/**
	 * This is the error definition that is sent when the stream does not get video or audio packet for the timeout duration.
	 * Currently it's implemented for WebRTC ingest
	 */
	public static final String NO_PACKET_RECEIVED_FOR_TIMEOUT_DURATION = "noPacketReceivedForTimeoutDuration";
	
	/**
	 * This is the error definition that is sent when mainTrack cannot be created or updated in publishing process.
	 */
	public static final String MAINTRACK_DB_OPERATION_FAILED = "mainTrackDBOperationFailed";


	/**
	 * This is passed in play websocket method to define the publisher stream id (if available) which uses same websocket channel with player
	 * For example in conference case a participant use same websocket to publish its stream and to play the others
	 */
	public static final String USER_PUBLISH_ID = "userPublishId";
	
	/**
	 * Notification to notify a new subtrack addition to a main track
	 */
	public static final String SUBTRACK_ADDED = "subtrackAdded";
	
	/**
	 * Notification to notify a new subtrack removal to a main track
	 */
	public static final String SUBTRACK_REMOVED = "subtrackRemoved";
	
	/**
	 * This is the error definition that is sent when the stream does not exist or not streaming
	 */
	public static final String STREAM_NOT_EXIST_OR_NOT_STREAMING_DEFINITION = "stream_not_exist_or_not_streaming";
	
	/**
	 * This is the error definition that is sent when the stream exits but not available as WebRTC because webrtc is not enabled
	 */
	public static final String WEBRTC_NOT_ENABLED_TO_PLAYBACK_DEFINITION = "webrtc_not_enabled";

	/**
	 * This is the definition that is sent when the is about to start for auto/start stop streams
	 */
	public static final String STREAMING_STARTS_SOON_DEFINITION = "streaming_starts_soon";

}
