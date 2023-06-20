package io.antmedia;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.catalina.util.NetMask;
import org.apache.commons.lang3.RandomStringUtils;
import org.bson.types.ObjectId;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Field;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Index;
import dev.morphia.annotations.Indexes;
import io.antmedia.rest.VoDRestService;

/**
 * Application Settings for each application running in Ant Media Server.
 * Each setting should have a default value with @Value annotation. Otherwise it breaks compatibility 
 *
 * For naming please use the following convention
 * start with "settings" put dot(.) and related parameter.
 * like settings.hlsTime
 * 
 * If default values are not as expected, this is the signal that server is not started correctly for any 
 * reason. Don't patch it with null-check or similar things. Take a look at why server is not started correctly
 *
 *
 * These settings are set for each applications and stored in the file <AMS_DIR>/webapps/<AppName>/WEB_INF/red5-web.properties.
 * Click on any field to see its default value.
 *
 * Example: click on 	aacEncodingEnabled --> The line @Value("${settings.aacEncodingEnabled:true}") means that
 * its default value is true and it can be changed from the file  <AMS_DIR>/webapps/<AppName>/WEB_INF/red5-web.properties
 * Be careful about the type of the field. For this case its boolean.
 *
 * @author mekya
 *
 */
@Entity("AppSettings")
@Indexes({ @Index(fields = @Field("appName"))})
@PropertySource("/WEB-INF/red5-web.properties")
@JsonIgnoreProperties(ignoreUnknown = true)
public class AppSettings implements Serializable{

	private static final long serialVersionUID = 1L;

	@JsonIgnore
	@Id
	private ObjectId dbId;

	public static final String PROPERTIES_FILE_PATH = "/WEB-INF/red5-web.properties";

	private static final String SETTINGS_ENCODING_SPECIFIC = "settings.encoding.specific";
	public static final String SETTINGS_ADD_DATE_TIME_TO_MP4_FILE_NAME = "settings.addDateTimeToMp4FileName";
	public static final String SETTINGS_HLS_MUXING_ENABLED = "settings.hlsMuxingEnabled";
	public static final String SETTINGS_DASH_MUXING_ENABLED = "settings.dashMuxingEnabled";
	public static final String SETTINGS_DASH_WINDOW_SIZE = "settings.dashWindowSize";
	public static final String SETTINGS_DASH_EXTRA_WINDOW_SIZE = "settings.dashExtraWindowSize";
	public static final String SETTINGS_ENCODER_SETTINGS_STRING = "settings.encoderSettingsString";
	public static final String SETTINGS_HLS_LIST_SIZE = "settings.hlsListSize";
	public static final String SETTINGS_HLS_TIME = "settings.hlsTime";
	public static final String SETTINGS_HLS_HTTP_ENDPOINT = "settings.hlsHttpEndpoint";
	public static final String SETTINGS_DASH_SEG_DURATION = "settings.dashSegDuration";
	public static final String SETTINGS_DASH_FRAGMENT_DURATION = "settings.dashFragmentDuration";
	public static final String SETTINGS_DASH_TARGET_LATENCY = "settings.dashTargetLatency";	
	public static final String SETTINGS_WEBRTC_ENABLED = "settings.webRTCEnabled";
	public static final String SETTINGS_USE_ORIGINAL_WEBRTC_ENABLED = "settings.useOriginalWebRTCEnabled";
	public static final String SETTINGS_DELETE_HLS_FILES_ON_ENDED = "settings.deleteHLSFilesOnEnded";
	public static final String SETTINGS_DELETE_DASH_FILES_ON_ENDED = "settings.deleteDASHFilesOnEnded";
	public static final String SETTINGS_LISTENER_HOOK_URL = "settings.listenerHookURL";
	public static final String SETTINGS_ACCEPT_ONLY_STREAMS_IN_DATA_STORE = "settings.acceptOnlyStreamsInDataStore";
	public static final String SETTINGS_TOKEN_CONTROL_ENABLED = "settings.tokenControlEnabled";
	public static final String SETTINGS_PUBLISH_TOKEN_CONTROL_ENABLED = "settings.publishTokenControlEnabled";
	public static final String SETTINGS_PLAY_TOKEN_CONTROL_ENABLED = "settings.playTokenControlEnabled";
	public static final String SETTINGS_TIME_TOKEN_SUBSCRIBER_ONLY = "settings.timeTokenSubscriberOnly";
	public static final String SETTINGS_TIME_TOKEN_PERIOD = "settings.timeTokenPeriod";
	public static final String SETTINGS_HLS_PLAY_LIST_TYPE = "settings.hlsPlayListType";
	public static final String SETTINGS_VOD_FOLDER = "settings.vodFolder";
	public static final String SETTINGS_PREVIEW_OVERWRITE = "settings.previewOverwrite";
	private static final String SETTINGS_STALKER_DB_SERVER = "settings.stalkerDBServer";
	private static final String SETTINGS_STALKER_DB_USER_NAME = "settings.stalkerDBUsername";
	private static final String SETTINGS_STALKER_DB_PASSWORD = "settings.stalkerDBPassword";
	public static final String SETTINGS_OBJECT_DETECTION_ENABLED = "settings.objectDetectionEnabled";
	private static final String SETTINGS_CREATE_PREVIEW_PERIOD = "settings.createPreviewPeriod";
	public static final String SETTINGS_MP4_MUXING_ENABLED = "settings.mp4MuxingEnabled";
	public static final String SETTINGS_WEBM_MUXING_ENABLED = "settings.webMMuxingEnabled";
	private static final String SETTINGS_STREAM_FETCHER_BUFFER_TIME = "settings.streamFetcherBufferTime";
	public static final String SETTINGS_STREAM_FETCHER_RESTART_PERIOD = "settings.streamFetcherRestartPeriod";
	private static final String SETTINGS_STREAM_FETCHER_AUTO_START = "settings.streamFetcherAutoStart";
	private static final String SETTINGS_MUXER_FINISH_SCRIPT = "settings.muxerFinishScript";
	public static final String SETTINGS_WEBRTC_FRAME_RATE = "settings.webRTCFrameRate";
	public static final String SETTINGS_HASH_CONTROL_PUBLISH_ENABLED = "settings.hashControlPublishEnabled";
	public static final String SETTINGS_HASH_CONTROL_PLAY_ENABLED = "settings.hashControlPlayEnabled";
	public static final String TOKEN_HASH_SECRET = "tokenHashSecret";
	public static final String SETTINGS_WEBRTC_PORT_RANGE_MIN = "settings.webrtc.portRangeMin";
	public static final String SETTINGS_WEBRTC_PORT_RANGE_MAX = "settings.webrtc.portRangeMax";
	public static final String SETTINGS_WEBRTC_STUN_SERVER_URI = "settings.webrtc.stunServerURI";
	public static final String SETTINGS_WEBRTC_TURN_SERVER_USERNAME = "settings.webrtc.turnServerUsername";
	public static final String SETTINGS_WEBRTC_TURN_SERVER_CREDENTIAL = "settings.webrtc.turnServerCredential";
	
	public static final String SETTINGS_WEBRTC_TCP_CANDIDATE_ENABLED = "settings.webrtc.tcpCandidateEnabled"; 
	public static final String SETTINGS_WEBRTC_SDP_SEMANTICS = "settings.webrtc.sdpSemantics";

	private static final String SETTINGS_SIGNALING_ENABLED = "signaling.enabled";
	private static final String SETTINGS_SIGNALING_ADDRESS = "signaling.address";

	private static final String SETTINGS_ENCODING_ENCODER_NAME = "settings.encoding.encoderName";
	private static final String SETTINGS_ENCODING_PRESET = "settings.encoding.preset";
	private static final String SETTINGS_ENCODING_PROFILE = "settings.encoding.profile";
	private static final String SETTINGS_ENCODING_LEVEL = "settings.encoding.level";
	private static final String SETTINGS_ENCODING_RC = "settings.encoding.rc";
	private static final String SETTINGS_ENCODING_THREAD_COUNT = "settings.encoding.threadCount";
	private static final String SETTINGS_ENCODING_THREAD_TYPE= "settings.encoding.threadType";
	private static final String SETTINGS_PREVIEW_HEIGHT = "settings.previewHeight";

	private static final String SETTINGS_ENCODING_VP8_THREAD_COUNT = "settings.encoding.vp8.threadCount";
	private static final String SETTINGS_ENCODING_VP8_SPEED = "settings.encoding.vp8.speed";
	private static final String SETTINGS_ENCODING_VP8_DEADLINE = "settings.encoding.vp8.deadline";

	/**
	 * Generate preview if there is any adaptive settings.
	 * Preview generation depends on adaptive settings and it's generated by default.
	 * Default value is true.
	 */
	public static final String SETTINGS_GENERATE_PREVIEW = "settings.previewGenerate";

	public static final String SETTINGS_REMOTE_ALLOWED_CIDR = "settings.remoteAllowedCIDR";

	public static final String SETTINGS_WRITE_STATS_TO_DATASTORE = "settings.writeStatsToDatastore";


	public static final String SETTINGS_ENCODER_SELECTION_PREFERENCE = "settings.encoderSelectionPreference";

	public static final String SETTINGS_ALLOWED_PUBLISHER_IPS = "settings.allowedPublisherCIDR";

	public static final String BEAN_NAME = "app.settings";

	private static final String SETTINGS_EXCESSIVE_BANDWIDTH_THRESHOLD = "settings.excessiveBandwidth.threshold";

	private static final String SETTINGS_EXCESSIVE_BANDWIDTH_CALL_THRESHOLD = "settings.excessiveBandwidth.call.threshold";

	private static final String SETTINGS_PORT_ALLOCATOR_FLAGS = "settings.portAllocator.flags";

	private static final String SETTINGS_EXCESSIVE_BANDWIDTH_TRY_COUNT_BEFORE_SWITCH_BACK = "settings.excessiveBandwith.tryCount.beforeSwitchback";

	private static final String SETTINGS_EXCESSIVE_BANDWIDTH_ENABLED = "settings.excessiveBandwidth_enabled";

	private static final String SETTINGS_EXCESSIVE_BANDWIDTH_PACKET_LOSS_DIFF_THRESHOLD_FOR_SWITCH_BACK = "settings.excessiveBandwidth.packetLossDiffThreshold.forSwitchback";

	private static final String SETTINGS_EXCESSIVE_BANDWIDTH_RTT_MEASUREMENT_THRESHOLD_FOR_SWITCH_BACK = "settings.excessiveBandwidth.rttMeasurementDiffThreshold.forSwitchback";

	private static final String SETTINGS_REPLACE_CANDIDATE_ADDR_WITH_SERVER_ADDR = "settings.replaceCandidateAddrWithServerAddr";

	public static final String SETTINGS_DB_APP_NAME = "db.app.name";

	public static final String SETTINGS_ENCODING_TIMEOUT = "settings.encoding.timeout";

	public static final String SETTINGS_WEBRTC_CLIENT_START_TIMEOUT = "settings.webrtc.client.start.timeoutMs";

	public static final String SETTINGS_DEFAULT_DECODERS_ENABLED = "settings.defaultDecodersEnabled";

	public static final String SETTINGS_COLLECT_SOCIAL_MEDIA_ACTIVITY_ENABLED = "settings.collectSocialMediaActivityEnabled";

	private static final String SETTINGS_HTTP_FORWARDING_EXTENSION = "settings.httpforwarding.extension";

	private static final String SETTINGS_HTTP_FORWARDING_BASE_URL = "settings.httpforwarding.baseURL";

	private static final String SETTINGS_RTMP_MAX_ANALYZE_DURATION_MS = "settings.rtmp.maxAnalyzeDurationMS";

	private static final String SETTINGS_DISABLE_IPV6_CANDIDATES = "settings.disableIPv6Candidates";

	private static final String SETTINGS_RTSP_PULL_TRANSPORT_TYPE = "settings.rtspPullTransportType";

	public static final String SETTINGS_H264_ENABLED = "settings.h264Enabled";

	public static final String SETTINGS_VP8_ENABLED = "settings.vp8Enabled";

	public static final String SETTINGS_H265_ENABLED = "settings.h265Enabled";

	public static final String SETTINGS_MAX_FPS_ACCEPT = "settings.maxFpsAccept";

	public static final String SETTINGS_DATA_CHANNEL_ENABLED = "settings.dataChannelEnabled";

	public static final String SETTINGS_DATA_CHANNEL_PLAYER_DISTRIBUTION = "settings.dataChannelPlayerDistrubution";

	public static final String SETTINGS_MAX_RESOLUTION_ACCEPT = "settings.maxResolutionAccept";

	public static final String SETTINGS_MAX_BITRATE_ACCEPT = "settings.maxBitrateAccept";

	public static final String SETTINGS_AUDIO_BITRATE_SFU = "settings.audioBitrateSFU";

	public static final String SETTINGS_ENDPOINT_REPUBLISH_LIMIT = "settings.endpoint.republishLimit";

	public static final String SETTINGS_ENDPOINT_HEALTH_CHECK_PERIOD_MS = "settings.endpoint.healthCheckPeriodMs";


	/**
	 * In data channel, player messages are delivered to nobody,
	 * In order words, player cannot send messages
	 */
	public static final String DATA_CHANNEL_PLAYER_TO_NONE = "none";

	/**
	 * In data channel, player messages are delivered to only publisher
	 */
	public static final String DATA_CHANNEL_PLAYER_TO_PUBLISHER = "publisher";

	/**
	 * In data channel, player messages are delivered to everyone both publisher and all players
	 */
	public static final String DATA_CHANNEL_PLAYER_TO_ALL = "all";

	public static final String SETTINGS_HLS_FLAGS = "settings.hlsflags";

	public static final String SETTINGS_UPLOAD_EXTENSIONS_TO_S3 = "settings.uploadExtensionsToS3";

	public static final String SETTINGS_S3_STORAGE_CLASS= "settings.s3StorageClass";

	public static final String SETTINGS_RTSP_TIMEOUT_DURATION_MS = "settings.rtspTimeoutDurationMs";

	public static final String SETTINGS_RTMP_INGEST_BUFFER_TIME_MS = "settings.rtmpIngestBufferTimeMs";

	public static final String SETTINGS_ACCEPT_ONLY_ROOMS_IN_DATA_STORE = "settings.acceptOnlyRoomsInDataStore";

	public static final String SETTINGS_DATA_CHANNEL_WEBHOOK_URL = "settings.dataChannelWebHook";

	/**
	 * WebRTC SDP Semantics:PLAN B
	 */
	public static final String SDP_SEMANTICS_PLAN_B = "planB";

	/**
	 * WebRTC SDP Semantics:UNIFIED PLAN
	 */
	public static final String SDP_SEMANTICS_UNIFIED_PLAN = "unifiedPlan";

	/**
	 * Height Property key for WebRTC to RTMP  forwarding
	 */
	private static final String SETTINGS_HEIGHT_RTMP_FORWARDING = "settings.heightRtmpForwarding";

	/**
	 *
	 */
	private static final String SETTINGS_AAC_ENCODING_ENABLED="settings.aacEncodingEnabled";

	private static final String SETTINGS_GOP_SIZE = "settings.gopSize";

	private static final String SETTINGS_CONSTANT_RATE_FACTOR = "settings.constantRateFactor";

	private static final String SETTINGS_WEBRTC_VIEWER_LIMIT = "settings.webRTCViewerLimit";

	public static final String SETTINGS_JWT_SECRET_KEY = "settings.jwtSecretKey";

	public static final String SETTINGS_JWT_CONTROL_ENABLED = "settings.jwtControlEnabled";
	public static final String SETTINGS_JWT_BLACKLIST_ENABLED = "settings.jwtBlacklistEnabled";

	public static final String SETTINGS_IP_FILTER_ENABLED = "settings.ipFilterEnabled";

	private static final String SETTINGS_INGESTING_STREAM_LIMIT = "settings.ingestingStreamLimit";

	private static final String SETTINGS_WEBRTC_KEYFRAME_TIME = "settings.webRTCKeyframeTime";

	public static final String SETTINGS_JWT_STREAM_SECRET_KEY = "settings.jwtStreamSecretKey";

	public static final String SETTINGS_PLAY_JWT_CONTROL_ENABLED = "settings.playJwtControlEnabled";

	public static final String SETTINGS_PUBLISH_JWT_CONTROL_ENABLED = "settings.publishJwtControlEnabled";

	public static final String SETTINGS_DASH_ENABLE_LOW_LATENCY = "settings.dash.llEnabled";

	public static final String SETTINGS_HLS_ENABLE_LOW_LATENCY = "settings.dash.llHlsEnabled";

	public static final String SETTINGS_HLS_ENABLED_VIA_DASH_LOW_LATENCY = "settings.dash.hlsEnabled";

	private static final String SETTINGS_USE_TIMELINE_DASH_MUXING = "settings.dash.useTimeline";

	private static final String SETTINGS_DASH_HTTP_STREAMING = "settings.dash.httpStreaming";

	private static final String SETTINGS_S3_STREAMS_FOLDER_PATH = "settings.s3.streams.folder.path";

	private static final String SETTINGS_S3_PREVIEWS_FOLDER_PATH = "settings.s3.previews.folder.path";

	private static final String SETTINGS_DASH_HTTP_ENDPOINT = "settings.dash.httpEndpoint";

	private static final String SETTINGS_FORCE_DECODING = "settings.forceDecoding";

	private static final String SETTINGS_ADD_ORIGINAL_MUXER_INTO_HLS_PLAYLIST = "settings.addOriginalMuxerIntoHlsPlaylist";

	public static final String SETTINGS_S3_RECORDING_ENABLED = "settings.s3RecordingEnabled";

	public static final String SETTINGS_S3_ACCESS_KEY = "settings.s3AccessKey";
	public static final String SETTINGS_S3_SECRET_KEY = "settings.s3SecretKey";
	public static final String SETTINGS_S3_REGION_NAME = "settings.s3RegionName";
	public static final String SETTINGS_S3_BUCKET_NAME = "settings.s3BucketName";
	public static final String SETTINGS_S3_ENDPOINT = "settings.s3Endpoint";
	public static final String SETTINGS_S3_PERMISSION = "settings.s3Permission";
	public static final String SETTINGS_S3_CACHE_CONTROL = "settings.s3CacheControl";
	public static final String SETTINGS_ENABLE_TIME_TOKEN_PLAY = "settings.enableTimeTokenForPlay";
	public static final String SETTINGS_ENABLE_TIME_TOKEN_PUBLISH = "settings.enableTimeTokenForPublish";

	public static final String SETTINGS_HLS_ENCRYPTION_KEY_INFO_FILE = "settings.hlsEncryptionKeyInfoFile";


	public static final String SETTINGS_JWKS_URL = "settings.jwksURL";
	public static final String SETTINGS_WEBHOOK_AUTHENTICATE_URL = "settings.webhookAuthenticateURL";

	public static final String SETTINGS_FORCE_ASPECT_RATIO_IN_TRANSCODING = "settings.forceAspectRationInTranscoding";

	public static final String SETTINGS_VOD_UPLOAD_FINISH_SCRIPT = "settings.vodUploadFinishScript";
	public static final String SETTINGS_FILE_NAME_FORMAT = "settings.fileNameFormat";

	private static final String SETTINGS_CONTENT_SECURITY_POLICY_HEADER_VALUE = "settings.contentSecurityPolicyHeaderValue";
	
	public static final String SETTINGS_MAX_AUDIO_TRACK_COUNT = "settings.maxAudioTrackCount";

	public static final String SETTINGS_MAX_VIDEO_TRACK_COUNT = "settings.maxVideoTrackCount";
	
	private static final String SETTINGS_RTMP_PLAYBACK_ENABLED = "settings.rtmpPlaybackEnabled";

	private static final String SETTINGS_ORIGIN_EDGE_CONNECTION_IDLE_TIMEOUT = "settings.originEdgeIdleTimeout";
	
	public static final String SETTINGS_ADD_DATE_TIME_TO_HLS_FILE_NAME = "settings.addDateTimeToHlsFileName";

	public static final String SETTINGS_PLAY_WEBRTC_STREAM_ONCE_FOR_EACH_SESSION = "settings.playWebRTCStreamOnceForEachSession";

	private static final String SETTINGS_STATS_BASED_ABR_ALGORITHM_ENABLED = "settings.statsBasedABREnabled";
	private static final String SETTINGS_ABR_DOWN_SCALE_PACKET_LOST_RATIO = "settings.abrDownScalePacketLostRatio";
	private static final String SETTINGS_ABR_UP_SCALE_PACKET_LOST_RATIO = "settings.abrUpScalePacketLostRatio";
	private static final String SETTINGS_ABR_UP_SCALE_RTT_MS = "settings.abrUpScaleRTTMs";
	private static final String SETTINGS_ABR_UP_SCALE_JITTER_MS = "settings.abrUpScaleJitterMs";

	public static final String SETTINGS_CLUSTER_COMMUNICATION_KEY = "settings.clusterCommunicationKey";


	/**
	 * Comma separated CIDR that rest services are allowed to response
	 * Allowed IP addresses to reach REST API, It must be in CIDR format as a.b.c.d/x
	 */
	@Value("${"+SETTINGS_REMOTE_ALLOWED_CIDR+":127.0.0.1}")
	private String remoteAllowedCIDR = "127.0.0.1";

	/**
	 * It's mandatory, If it is set true then a mp4 file is created into <APP_DIR>/streams directory
	 * Default value is false
	 */
	@Value( "${"+SETTINGS_MP4_MUXING_ENABLED+":false}" )
	private boolean mp4MuxingEnabled;

	/**
	 * Enable/Disable WebM recording
	 */
	@Value( "${"+SETTINGS_WEBM_MUXING_ENABLED+":false}" )
	private boolean webMMuxingEnabled;

	/**
	 * It's mandatory, Date and time are added to created .mp4 file name, Default value is false
	 */
	@Value( "${"+SETTINGS_ADD_DATE_TIME_TO_MP4_FILE_NAME+":false}" )
	private boolean addDateTimeToMp4FileName;

	/**
	 * The format of output mp4 and ts files.
	 * To add resolution like stream1_240p.mp4, add %r to the string
	 * To add bitrate like stream1_500kbps, add %b to the string
	 * Add both for stream1_240p500kbps
	 */
	@Value( "${"+SETTINGS_FILE_NAME_FORMAT+":%r%b}" )
	private String fileNameFormat = "%r%b";
	
	/**
	 * Enable/disable hls recording
	 *  If it is set true then HLS files are created into <APP_DIR>/streams and HLS playing is enabled,
	 *  Default value is true
	 */
	@Value( "${"+SETTINGS_HLS_MUXING_ENABLED+":true}" )
	private boolean hlsMuxingEnabled = true;

	/**
	 * Encoder settings in comma separated format
	 * This must be set for adaptive streaming,
	 * If it is empty SFU mode will be active in WebRTCAppEE,
	 * video height, video bitrate, and audio bitrate are set as an example,
	 * Ex. 480,300000,96000,360,200000,64000.
	 */
	@Value( "${"+SETTINGS_ENCODER_SETTINGS_STRING+":}" )
	private String encoderSettingsString = "";

	/**
	 * This is for making this instance run also as a signaling server.
	 * Signaling Server lets Ant Media Server instances behind NAT stream its content to the peer in the Internet
	 */
	@Value( "${"+SETTINGS_SIGNALING_ENABLED+":false}" )
	private boolean signalingEnabled;

	/**
	 * This is for using another Ant Media instance as signaling server.
	 * If your server is behind a NAT it will allow possible connection.
	 * It should be full qualified URI like this 
	 * ws://107.23.25.77:5080/WebRTCAppEE/websocket/signaling
	 */
	@Value( "${"+SETTINGS_SIGNALING_ADDRESS+":#{null}}" )
	private String signalingAddress;

	/**
	 * Number of segments(chunks) in m3u8 files
	 * Set the maximum number of playlist entries, If 0 the list file will contain all the segments,
	 */
	@Value( "${"+SETTINGS_HLS_LIST_SIZE+":5}" )
	private String hlsListSize = "5";

	/**
	 * Duration of segments in m3u8 files
	 * Target segment length in seconds,
	 * Segment will be cut on the next key frame after this time has passed.
	 */
	@Value( "${"+SETTINGS_HLS_TIME+":#{null}}" )
	private String hlsTime;

	/**
	 * Binary entity for uploading the extensions
	 * 0 means does not upload, 1 means upload
	 * Least significant digit switches mp4 files upload to s3
	 * Second digit switches HLS files upload to s3
	 * Most significant digit switches PNG files upload to s3
	 * Example: 5 ( 101 in binary ) means upload mp4 and PNG but not HLS
	 * HLS files still will be saved on the server if deleteHLSFilesOnEnded flag is false
	 */
	@Value( "${"+SETTINGS_UPLOAD_EXTENSIONS_TO_S3+":7}" )
	private int uploadExtensionsToS3=7;

	/*
	 * S3 Storage classes. Possible values are 
	 * 		STANDARD, REDUCED_REDUNDANCY, GLACIER, STANDARD_IA, ONEZONE_IA, INTELLIGENT_TIERING, DEEP_ARCHIVE
	 * 
	 * Case sensitivity is important. 
	 * 
	 * More information is available at AWS S3 -> https://www.amazonaws.cn/en/s3/storage-classes/
	 */
	@Value( "${"+SETTINGS_S3_STORAGE_CLASS+":STANDARD}" )
	private String s3StorageClass="STANDARD";
	/**
	 * Endpoint will try to republish if error occurs,
	 * however the error might get fixed internally in case of small issues without republishing
	 * This value is the check time for endpoint in 3 trials
	 * For example for 2 seconds, there will be 2 checks in 2 second intervals,
	 * if each of them fails it will try to republish in 3rd check.
	 */
	@Value( "${"+SETTINGS_ENDPOINT_HEALTH_CHECK_PERIOD_MS+":2000}" )
	private int endpointHealthCheckPeriodMs=2000;

	/**
	 * This limit is for republishing to a certain endpoint for how many times
	 * For example in case we tried to republish 3 times and still got an error
	 * We conclude that the endpoint is dead and close it.
	 */
	@Value( "${"+SETTINGS_ENDPOINT_REPUBLISH_LIMIT+":3}" )
	private int endpointRepublishLimit=3;

	/**
	 * Duration of segments in mpd files,
	 * Segments are a property of DASH. A segment is the minimal download unit.
	 *  
	 */
	@Value( "${"+SETTINGS_DASH_SEG_DURATION+":6}" )
	private String dashSegDuration="6";

	/**
	 * Fragments are a property of fragmented MP4 files, Typically a fragment consists of moof + mdat.
	 *
	 */
	@Value( "${"+SETTINGS_DASH_FRAGMENT_DURATION+":0.5}" )
	private String dashFragmentDuration="0.5";


	/**
	 * Latency of the DASH streaming,
	 */
	@Value( "${"+SETTINGS_DASH_TARGET_LATENCY+":3.5}" )
	private String targetLatency="3.5";

	/**
	 * DASH window size, Number of files in manifest
	 */
	@Value( "${"+SETTINGS_DASH_WINDOW_SIZE+":5}" )
	private String dashWindowSize="5";

	/**
	 * DASH extra window size, Number of segments kept outside of the manifest before removing from disk
	 */
	@Value( "${"+SETTINGS_DASH_EXTRA_WINDOW_SIZE+":5}" )
	private String dashExtraWindowSize="5";

	/**
	 * Enable low latency dash, This settings is effective if dash is enabled
	 */
	@Value( "${"+SETTINGS_DASH_ENABLE_LOW_LATENCY+":true}" )
	private boolean lLDashEnabled=true;

	/**
	 * Enable low latency hls via dash muxer, LLHLS is effective if dash is enabled.
	 */
	@Value( "${"+SETTINGS_HLS_ENABLE_LOW_LATENCY+":false}" )
	private boolean lLHLSEnabled=false;

	/**
	 * Enable hls through DASH muxer, LLHLS is effective if dash is enabled.
	 */
	@Value( "${"+SETTINGS_HLS_ENABLED_VIA_DASH_LOW_LATENCY+":false}" )
	private boolean hlsEnabledViaDash=false;

	/**
	 * Use timeline in dash muxing.
	 */
	@Value( "${"+SETTINGS_USE_TIMELINE_DASH_MUXING+":false}" )
	private boolean useTimelineDashMuxing=false;

	/**
	 * Enable/disable webrtc,
	 * It's mandatory, If it is set true then WebRTC playing is enabled, Default value is false
	 */
	@Value( "${"+SETTINGS_WEBRTC_ENABLED+":true}" )
	private boolean webRTCEnabled=true;

	/**
	 * The flag that sets using the original webrtc stream in streaming,
	 * This setting is effective if there is any adaptive bitrate setting,
	 * For instance assume that there is adaptive bitrate with 480p and incoming stream is 720p
	 * Then if this setting is true, there are two bitrates for playing 720p and 480p,
	 * In this case if this setting is false, there is one bitrate for playing that is 480p
	 */
	@Value( "${"+SETTINGS_USE_ORIGINAL_WEBRTC_ENABLED+":false}" )
	private boolean useOriginalWebRTCEnabled=false;

	/**
	 * It's mandatory,
	 * If this value is true, hls files(m3u8 and ts files) are deleted after the broadcasting
	 * has finished,
	 * Default value is true.
	 */
	@Value( "${"+SETTINGS_DELETE_HLS_FILES_ON_ENDED+":true}" )
	private boolean deleteHLSFilesOnEnded = true;

	/**
	 * If this value is true, dash files(mpd and m4s files) are deleted after the broadcasting
	 * has finished.
	 */
	@Value( "${"+SETTINGS_DELETE_DASH_FILES_ON_ENDED+":true}" )
	private boolean deleteDASHFilesOnEnded = true;

	/**
	 * The secret string used for creating hash based tokens
	 * The key that used in hash generation for hash-based access control.
	 */
	@Value( "${"+TOKEN_HASH_SECRET+":}" )
	private String tokenHashSecret = "";

	/**
	 * It's mandatory,
	 * If it is set true then hash based access control enabled for publishing,
	 * enable hash control as token for publishing operations using shared secret
	 * Default value is false.
	 */
	@Value( "${"+SETTINGS_HASH_CONTROL_PUBLISH_ENABLED+":false}" )
	private boolean hashControlPublishEnabled;

	/**
	 * It's mandatory,
	 * If it is set true then hash based access control enabled for playing,
	 * enable hash control as token for playing operations using shared secret
	 * Default value is false.
	 */
	@Value( "${"+SETTINGS_HASH_CONTROL_PLAY_ENABLED+":false}" )
	private boolean hashControlPlayEnabled;

	/**
	 * The URL for action callback
	 *  You must set this to subscribe some event notifications,
	 *  For details check: https://antmedia.io/webhook-integration/
	 */
	@Value( "${"+SETTINGS_LISTENER_HOOK_URL+":}" )
	private String listenerHookURL = "";

	/**
	 * The control for publishers
	 * It's mandatory,
	 * If it is set true you cannot start publishing unless you add the stream id to the database,
	 * You can add stream id by REST API. Default value is false.
	 */
	@Value( "${"+SETTINGS_ACCEPT_ONLY_STREAMS_IN_DATA_STORE+":false}" )
	private boolean acceptOnlyStreamsInDataStore;

	/**
	 * The control for rooms
	 */
	@Value( "${"+SETTINGS_ACCEPT_ONLY_ROOMS_IN_DATA_STORE+":false}" )
	private boolean acceptOnlyRoomsInDataStore;

	/**
	 * The settings for enabling one-time token control mechanism for accessing resources and publishing
	 * Check for details: https://antmedia.io/secure-video-streaming/. Default value is false.
	 */

	@Value("#{'${"+ SETTINGS_PUBLISH_TOKEN_CONTROL_ENABLED +":${" + SETTINGS_TOKEN_CONTROL_ENABLED +":false}}'}") 
	private boolean publishTokenControlEnabled ;
	// check old SETTINGS_TOKEN_CONTROL_ENABLED for backward compatibility
	// https://stackoverflow.com/questions/49653241/can-multiple-property-names-be-specified-in-springs-value-annotation
	/**
	 * The settings for enabling one-time token control mechanism for accessing resources and publishing
	 * It's mandatory, This enables token control,
	 * Check for details: https://antmedia.io/secure-video-streaming/. Default value is false.
	 */
	@Value("#{'${"+ SETTINGS_PLAY_TOKEN_CONTROL_ENABLED +":${" + SETTINGS_TOKEN_CONTROL_ENABLED +":false}}'}")
	private boolean playTokenControlEnabled ;

	/**
	 * The settings for accepting only time based token subscribers as connections to the streams 
	 * @Deprecated. Please use {@link #enableTimeTokenForPlay} or {@link #enableTimeTokenForPublish}
	 */
	@Value( "${"+SETTINGS_TIME_TOKEN_SUBSCRIBER_ONLY+":false}" )
	@Deprecated
	private boolean timeTokenSubscriberOnly;
	/**
	 * the settings for accepting only time based token subscribers as connections to the streams
	 */
	@Value( "${"+SETTINGS_ENABLE_TIME_TOKEN_PLAY+":false}" )
	private boolean enableTimeTokenForPlay;
	/**
	 * the settings for accepting only time based token subscribers as connections to the streams
	 */
	@Value( "${"+SETTINGS_ENABLE_TIME_TOKEN_PUBLISH+":false}" )
	private boolean enableTimeTokenForPublish;

	/**
	 * period for the generated time token 
	 */
	@Value( "${"+SETTINGS_TIME_TOKEN_PERIOD+":60}" )
	private int timeTokenPeriod = 60;	

	/**
	 * It can be event: or vod, Check HLS documentation for EXT-X-PLAYLIST-TYPE.
	 *
	 */
	@Value( "${"+SETTINGS_HLS_PLAY_LIST_TYPE+":#{null}}" )
	private String hlsPlayListType;

	/**
	 * The path for manually saved used VoDs
	 * Determines the directory to store VOD files.
	 * @Deprecated use {@link VoDRestService#importVoDs(String)}
	 */
	@Value( "${"+SETTINGS_VOD_FOLDER+":}" )
	private String vodFolder = "";

	/**
	 * Overwrite preview files if exist, default value is false
	 * If it is set true and new stream starts with the same id,
	 * preview of the new one overrides the previous file,
	 * If it is false previous file saved with a suffix.
	 */
	@Value( "${"+SETTINGS_PREVIEW_OVERWRITE+":false}" )
	private boolean previewOverwrite;

	/**
	 * Address of the Stalker Portal DB server
	 * Database host address of IP TV Ministra platform.
	 */

	@Value( "${"+SETTINGS_STALKER_DB_SERVER+":}" )
	private String stalkerDBServer = "";

	/**
	 * Username of stalker portal DB
	 * Database user name of IP TV Ministra platform.
	 */
	@Value( "${"+SETTINGS_STALKER_DB_USER_NAME+":}" )
	private String stalkerDBUsername = "";

	/**
	 * Password of the stalker portal DB User
	 * Database password of IP TV Ministra platform.
	 */
	@Value( "${"+SETTINGS_STALKER_DB_PASSWORD+":}" )
	private String stalkerDBPassword = "";

	/**
	 * It's mandatory,
	 * The directory contains the tensorflow object detection model
	 *  If it is set true then object detection algorithm is run for streaming video,
	 * Default value is false.
	 */
	@Value( "${"+SETTINGS_OBJECT_DETECTION_ENABLED+":false}" )
	private boolean objectDetectionEnabled;
	/**
	 * It's mandatory,
	 * This determines the period (milliseconds) of preview (png) file creation,
	 * This file is created into <APP_DIR>/preview directory. Default value is 5000.
	 */

	@Value( "${"+SETTINGS_CREATE_PREVIEW_PERIOD+":5000}" )
	private int createPreviewPeriod = 5000;

	/**
	 * It's mandatory,
	 * Restart stream fetcher period in seconds
	 * Restart time for fetched streams from external sources,
	 * Default value is 0
	 */
	@Value( "${"+SETTINGS_STREAM_FETCHER_RESTART_PERIOD+":0}" )
	private int restartStreamFetcherPeriod;

	/**
	 * Stream fetchers are started automatically if it is set true
	 */
	@Value( "${"+SETTINGS_STREAM_FETCHER_AUTO_START+":false}" )
	private boolean startStreamFetcherAutomatically;

	/**
	 * It's mandatory,
	 * Stream fetcher buffer time in milliseconds,
	 * Stream is buffered for this duration and after that it will be started,
	 * Buffering time for fetched streams from external sources. 0 means no buffer,
	 * Default value is 0
	 */

	//@Value( "${"+SETTINGS_STREAM_FETCHER_BUFFER_TIME+"}" )
	private int streamFetcherBufferTime = 0;


	/**
	 * HLS Flags for FFmpeg HLS Muxer,
	 * Please add value by plus prefix in the properties file like this
	 * settings.hlsflags=+program_date_time
	 * 
	 * you can add + separated more options like below
	 * settings.hlsflags=+program_date_time+round_durations+append_list
	 *
	 * Separate with + or -.
	 * Check for details: https://ffmpeg.org/ffmpeg-formats.html#Options-6
	 * 
	 */
	@Value( "${" + SETTINGS_HLS_FLAGS + ":delete_segments}")
	private String hlsflags="delete_segments";

	private String mySqlClientPath = "/usr/local/antmedia/mysql";

	/**
	 * This is a script file path that is called by Runtime when muxing is finished,
	 * Bash script file path will be called after stream finishes.
	 */
	@Value( "${"+SETTINGS_MUXER_FINISH_SCRIPT+":}" )
	private String muxerFinishScript = "";

	/**
	 * It's mandatory,
	 * Determines the frame rate of video publishing to the WebRTC players,
	 * Default value is 30 because users are complaining about the 20fps(previous value) and may not know to change it
	 */
	@Value( "${"+SETTINGS_WEBRTC_FRAME_RATE+":30}" )
	private int webRTCFrameRate = 30;

	/**
	 * Min port number of the port range of WebRTC, It's effective when user publishes stream,
	 * This value should be less than the {@link #webRTCPortRangeMax}
	 * Determines the minimum port number for WebRTC connections, Default value is 0.
	 */
	@Value( "${" + SETTINGS_WEBRTC_PORT_RANGE_MIN +":0}")
	private int webRTCPortRangeMin;

	/**
	 * Max port number of the port range of WebRTC, It's effective when user publishes stream
	 * In order to port range port this value should be higher than {@link #webRTCPortRangeMin} 
	 */
	@Value( "${" + SETTINGS_WEBRTC_PORT_RANGE_MAX +":0}")
	private int webRTCPortRangeMax;

	/**
	 * STUN or TURN Server URI
	 * STUN server URI used for WebRTC ICE candidates
	 * You can check: https://antmedia.io/learn-webrtc-basics-components/,
	 * Default value is stun:stun.l.google.com:19302
	 * 
	 * STUN or TURN URL can be set for this properoy
	 */
	@Value( "${" + SETTINGS_WEBRTC_STUN_SERVER_URI +":stun:stun1.l.google.com:19302}")
	private String stunServerURI = "stun:stun1.l.google.com:19302";

	/**
	 * TURN server username for WebRTC ICE candidates.
	 * In order to be effective, {@code #stunServerURI} and {@code #turnServerCredential} should be set
	 */
	@Value( "${" + SETTINGS_WEBRTC_TURN_SERVER_USERNAME +":#{null}}")
	private String turnServerUsername;

	/**
	 * TURN server credentai for WebRTC ICE candidates.
	 * In order to be effective, {@code #stunServerURI} and {@code #turnServerUsername} should be set
	 */
	@Value( "${" + SETTINGS_WEBRTC_TURN_SERVER_CREDENTIAL +":#{null}}")
	private String turnServerCredential;
	
	/**
	 * It's mandatory,
	 * TCP candidates are enabled/disabled.It's effective when user publishes stream
	 * It's disabled by default
	 * If it is set true then TCP candidates can be used for WebRTC connection,
	 * If it is false only UDP port will be used,
	 * Default value is true.
	 */
	@Value( "${" + SETTINGS_WEBRTC_TCP_CANDIDATE_ENABLED +":false}")
	private boolean webRTCTcpCandidatesEnabled;

	/**
	 * WebRTC SDP Semantics
	 * It can "planB" or "unifiedPlan"
	 */
	@Value( "${" + SETTINGS_WEBRTC_SDP_SEMANTICS +":" + SDP_SEMANTICS_UNIFIED_PLAN + "}")
	private String webRTCSdpSemantics = SDP_SEMANTICS_UNIFIED_PLAN;


	/**
	 * Port Allocator Flags for WebRTC
	 * PORTALLOCATOR_DISABLE_UDP = 0x01,
	 * PORTALLOCATOR_DISABLE_STUN = 0x02,
	 * PORTALLOCATOR_DISABLE_RELAY = 0x04,
	 */
	@Value( "${" + SETTINGS_PORT_ALLOCATOR_FLAGS +":0}")
	private int portAllocatorFlags;
	
	
	@Deprecated
	/**
	 * If it's enabled, interactivity(like, comment, etc.) is collected from social media channel,
	 * Default value is false.
	 * 
	 * This feature is removed
	 * 
	 */
	@Value( "${" + SETTINGS_COLLECT_SOCIAL_MEDIA_ACTIVITY_ENABLED +":false}")
	private boolean collectSocialMediaActivity;

	/**
	 * Name of the encoder to be used in adaptive bitrate,
	 * If there is a GPU, server tries to open h264_nvenc,
	 * If there is no GPU, server tries to open libx264 by default
	 * Can be h264_nvenc or libx264. If you set h264_nvenc but it cannot be opened then libx264 will be used,
	 * Name of the encoder to be used in adaptive bitrate,
	 * If there is a GPU, server tries to open h264_nvenc,
	 * If there is no GPU, server tries to open libx264 by default
	 */
	@Value( "${" + SETTINGS_ENCODING_ENCODER_NAME +":#{null}}")
	private String encoderName;

	/**
	 * Encoder's preset value in adaptive bitrate
	 * Libx264 presets are there
	 * https://trac.ffmpeg.org/wiki/Encode/H.264
	 * Ant Media Server uses "veryfast" by default
	 *
	 */
	@Value( "${" + SETTINGS_ENCODING_PRESET +":#{null}}")
	private String encoderPreset;

	/**
	 * Encoder profile in adaptive bitrate,
	 * It's baseline by default.
	 */
	@Value( "${" + SETTINGS_ENCODING_PROFILE +":#{null}}")
	private String encoderProfile;

	/**
	 * Encoder level in adaptive bitrate
	 */
	@Value( "${" + SETTINGS_ENCODING_LEVEL +":#{null}}")
	private String encoderLevel;

	/**
	 * Encoding rate control in adaptive bitrate
	 */
	@Value( "${" + SETTINGS_ENCODING_RC +":#{null}}")
	private String encoderRc;

	/**
	 * Encoder specific configuration for libx264 in adaptive bitrate,
	 * This is the x264-params in ffmpeg
	 * Specific settings for selected encoder,
	 * For libx264 please check https://trac.ffmpeg.org/wiki/Encode/H.264
	 */
	@Value( "${" + SETTINGS_ENCODING_SPECIFIC +":#{null}}")
	private String encoderSpecific;

	/**
	 * Encoder thread count.
	 */
	@Value( "${" + SETTINGS_ENCODING_THREAD_COUNT +":0}")
	private int encoderThreadCount;

	/**
	 * Encoder thread type
	 * 0: auto
	 * 1: frame
	 * 2: slice
	 */
	@Value( "${" + SETTINGS_ENCODING_THREAD_TYPE +":0}")
	private int encoderThreadType;

	/**
	 * Set quality/speed ratio modifier, Higher values speed up the encode at the cost of quality.
	 */
	@Value( "${" + SETTINGS_ENCODING_VP8_SPEED +":4}")
	private int vp8EncoderSpeed = 4;

	/**
	 * VP8 Encoder deadline:
	 *  best
	 * 	good 
	 *  realtime
	 */ 
	@Value( "${" + SETTINGS_ENCODING_VP8_DEADLINE +":realtime}")
	private String vp8EncoderDeadline = "realtime";

	/**
	 * VP8 Encoder thread count.
	 */
	@Value( "${" + SETTINGS_ENCODING_VP8_THREAD_COUNT +":1}")
	private int vp8EncoderThreadCount = 1;

	/**
	 * It's mandatory,
	 * Determines the height of preview file,
	 * Default value is 480
	 */

	@Value( "${" + SETTINGS_PREVIEW_HEIGHT +":480}")
	private int previewHeight = 480;

	/**
	 * Generate preview if there is any adaptive settings,
	 * 
	 * Preview generation depends on adaptive settings and it's generated by default
	 */
	@Value( "${" + SETTINGS_GENERATE_PREVIEW+":false}")
	private boolean generatePreview;

	@Value( "${" + SETTINGS_WRITE_STATS_TO_DATASTORE +":true}")
	private boolean writeStatsToDatastore = true;

	/**
	 * Can be "gpu_and_cpu" or "only_gpu"
	 * 
	 * "only_gpu" only tries to open the GPU for encoding,
	 * If it cannot open the gpu codec it returns false
	 * 
	 * "gpu_and_cpu" first tries to open the GPU for encoding
	 * if it does not open, it tries to open the CPU for encoding
	 * 
	 */
	@Value( "${" + SETTINGS_ENCODER_SELECTION_PREFERENCE+":gpu_and_cpu}")
	private String encoderSelectionPreference = "gpu_and_cpu";

	/**
	 * Comma separated CIDR that server accepts/ingests RTMP streams from,
	 * Default value is null which means that it accepts/ingests stream from everywhere
	 */
	@Value( "${" + SETTINGS_ALLOWED_PUBLISHER_IPS+":#{null}}")
	private String allowedPublisherCIDR;

	/**
	 * *******************************************************
	 * What is Excessive Bandwidth Algorithm?
	 * Excessive Bandwidth Algorithm tries to switch to higher bitrate even if bandwidth seems not enough
	 * 
	 * Why is it implemented?
	 * WebRTC stack sometimes does not calculate the bandwidth correctly. For instance,
	 * when network quality drop for a few seconds, it does not calculates the bitrate correctly
	 * 
	 * How it works?
	 * If measured bandwidth - the current video bitrate is more than {@link #excessiveBandwidthValue}
	 * for consecutive {@link #excessiveBandwidthCallThreshold} times it switches to higher bitrate
	 * 
	 * If bandwidth measured is still than the required bandwidth it tries {@link #excessiveBandwithTryCountBeforeSwitchback}
	 * times to stay in the high bitrate. It also switches back to lower quality 
	 * if packetLoss different is bigger than {@link #packetLossDiffThresholdForSwitchback} or 
	 * rtt time difference is bigger than {@link #rttMeasurementDiffThresholdForSwitchback} before 
	 * {@link #tryCountBeforeSwitchback} reaches to zero
	 * 
	 * 
	 * Side effect
	 * If network fluctuates too much or not consistent, quality of the video changes also fluctuates too much for the viewers
	 * *********************************************************
	 */

	/**
	 *  The excessive bandwidth threshold value
	 */
	@Value("${" + SETTINGS_EXCESSIVE_BANDWIDTH_THRESHOLD + ":300000}")
	private int excessiveBandwidthValue = 300000;



	/**
	 * The excessive bandwidth call threshold value
	 */
	@Value("${" + SETTINGS_EXCESSIVE_BANDWIDTH_CALL_THRESHOLD + ":3}")
	private int excessiveBandwidthCallThreshold = 3;


	@Value("${" + SETTINGS_EXCESSIVE_BANDWIDTH_TRY_COUNT_BEFORE_SWITCH_BACK + ":4}")
	private int excessiveBandwithTryCountBeforeSwitchback = 4;

	/**
	 * Enable or disable excessive bandwidth algorithm
	 */
	@Value("${" + SETTINGS_EXCESSIVE_BANDWIDTH_ENABLED+ ":false}")
	private boolean excessiveBandwidthAlgorithmEnabled;

	/**
	 * packet loss threshold if packetLoss is bigger than this value in ExcessiveBandwidth
	 * algorithm, it switches back to lower quality without try every attempts {@link #excessiveBandwithTryCountBeforeSwitchback}
	 */
	@Value("${" + SETTINGS_EXCESSIVE_BANDWIDTH_PACKET_LOSS_DIFF_THRESHOLD_FOR_SWITCH_BACK+ ":10}")
	private int packetLossDiffThresholdForSwitchback = 10;

	/**
	 * rtt measurement threshold diff if rttMeasurement is bigger than this value in ExcessiveBandwidth
	 * algorithm, it switches back to lower quality without try every attempts {@link #setTryCountBeforeSwitchback(int)}
	 * @param rttMeasurementDiffThresholdForSwitchback
	 */
	@Value("${" + SETTINGS_EXCESSIVE_BANDWIDTH_RTT_MEASUREMENT_THRESHOLD_FOR_SWITCH_BACK+ ":20}")
	private int rttMeasurementDiffThresholdForSwitchback=20;

	/**
	 * Replace candidate addr with server addr,
	 * In order to use it you should set serverName in conf/red5.properties
	 */
	@Value("${" + SETTINGS_REPLACE_CANDIDATE_ADDR_WITH_SERVER_ADDR+ ":false}")
	private boolean replaceCandidateAddrWithServerAddr;


	/**
	 * Applicaiton name for the data store which should exist so that no default value
	 * such as LiveApp, WebRTCApp etc.
	 */
	@Value("${" + SETTINGS_DB_APP_NAME +":}")
	private String appName = "";

	/**
	 * Timeout for encoding
	 * If encoder cannot encode a frame in this timeout, streaming is finished by server. 
	 */
	@Value("${" + SETTINGS_ENCODING_TIMEOUT +":5000}")
	private int encodingTimeout = 5000;

	/**
	 * If webrtc client is not started in this time, it'll close automatically
	 */
	@Value("${" + SETTINGS_WEBRTC_CLIENT_START_TIMEOUT +":10000}")
	private int webRTCClientStartTimeoutMs = 10000;

	/**
	 * Set true to enable WebRTC default decoders(such as VP8, VP9) 
	 * Set false to only enable h264 decoder
	 * If it is set true, WebRTC using default decoders(such as VP8, VP9).
	 * If it is set false, WebRTC using only default h264 decoder.
	 * Default value is false.
	 * 
	 * Deprecated: Use {@code vp8Enabled} and {@code h264enabled}
	 */
	@Deprecated
	@Value("${" + SETTINGS_DEFAULT_DECODERS_ENABLED+ ":false}")
	private boolean defaultDecodersEnabled;

	/**
	 * Update time of the setting in the cluster
	 */
	private long updateTime = 0;

	/**
	 * Forwards the http requests with this extension to {@link #httpForwardingBaseURL}
	 * It supports comma separated extensions Like mp4,m3u8
	 * Don't add any leading, trailing white spaces
	 */
	@Value("${" + SETTINGS_HTTP_FORWARDING_EXTENSION+ ":}")
	private String httpForwardingExtension = "";

	/**
	 * Forward the incoming http request to this base url
	 */
	@Value("${" + SETTINGS_HTTP_FORWARDING_BASE_URL+ ":}")
	private String httpForwardingBaseURL = "";

	/**
	 * Max analyze duration in for determining video and audio existence in RTMP streams
	 */
	@Value("${" + SETTINGS_RTMP_MAX_ANALYZE_DURATION_MS+ ":1500}")
	private int maxAnalyzeDurationMS = 1500;

	/**
	 * Enable/Disable IPv6 Candidates for WebRTC It's disabled by default
	 */
	@Value("${" + SETTINGS_DISABLE_IPV6_CANDIDATES+ ":true}")
	private boolean disableIPv6Candidates = true;

	/**
	 * Specify the rtsp transport type in pulling IP Camera or RTSP sources
	 * It can be tcp or udp
	 */
	@Value("${" + SETTINGS_RTSP_PULL_TRANSPORT_TYPE+ ":tcp}")
	private String rtspPullTransportType = "tcp";

	/**
	 * Specify the rtsp transport type in pulling IP Camera or RTSP sources
	 * It can be tcp or udp
	 */
	@Value("${" + SETTINGS_RTSP_TIMEOUT_DURATION_MS+ ":5000}")
	private int rtspTimeoutDurationMs = 5000;

	/**
	 * Max FPS value in RTMP streams
	 */
	@Value("${" + SETTINGS_MAX_FPS_ACCEPT+":0}")
	private int maxFpsAccept;

	/**
	 * Max Resolution value in RTMP streams
	 */
	@Value("${" + SETTINGS_MAX_RESOLUTION_ACCEPT+":0}")
	private int maxResolutionAccept;

	/**
	 * Max Bitrate value in RTMP streams
	 */
	@Value("${" + SETTINGS_MAX_BITRATE_ACCEPT+":0}")
	private int maxBitrateAccept;

	/**
	 * Enable/Disable h264 encoding It's enabled by default
	 */
	@Value("${" + SETTINGS_H264_ENABLED+ ":true}")
	private boolean h264Enabled = true;

	/**
	 * Enable/Disable vp8 encoding It's disabled by default
	 */
	@Value("${" + SETTINGS_VP8_ENABLED+ ":false}")
	private boolean vp8Enabled;

	/**
	 * Enable/disable H265 Encoding Disabled by default
	 */
	@Value("${" + SETTINGS_H265_ENABLED+ ":false}")
	private boolean h265Enabled;


	/**
	 * Enable/Disable data channel It's disabled by default
	 * When data channel is enabled, publisher can send messages to the players
	 */
	@Value("${" + SETTINGS_DATA_CHANNEL_ENABLED+ ":true}")
	private boolean dataChannelEnabled = true;


	/**
	 * Defines the distribution list for player messages
	 * it can be  none/publisher/all
	 * none: player messages are delivered to nobody
	 * publisher: player messages are delivered to only publisher
	 * all:  player messages are delivered to everyone both publisher and all players
	 */
	@Value("${" + SETTINGS_DATA_CHANNEL_PLAYER_DISTRIBUTION+ ":"+DATA_CHANNEL_PLAYER_TO_ALL+"}")
	private String dataChannelPlayerDistribution = DATA_CHANNEL_PLAYER_TO_ALL;

	/**
	 * RTMP ingesting buffer time in Milliseconds Server buffer this amount of video packet in order to compensate
	 * when stream is not received for some time
	 */
	@Value("${" + SETTINGS_RTMP_INGEST_BUFFER_TIME_MS+ ":0}")
	private long rtmpIngestBufferTimeMs;

	/**
	 * All data channel messages are delivered to these hook as well
	 * So that it'll be integrated to any third party application
	 */
	@Value( "${" + SETTINGS_DATA_CHANNEL_WEBHOOK_URL+":#{null}}")
	private String dataChannelWebHookURL;

	private String h265EncoderPreset;

	private String h265EncoderProfile;

	private String h265EncoderRc;

	private String h265EncoderSpecific;

	private String h265EncoderLevel;

	/**
	 * The height of the stream that is transcoded from incoming WebRTC stream to the RTMP
	 * This settings is effective in community edition by default
	 * It's also effective WebRTC to RTMP direct forwarding by giving rtmpForward=true in WebSocket communication
	 * in Enterprise Edition
	 */
	@Value( "${" + SETTINGS_HEIGHT_RTMP_FORWARDING+":360}")
	private int heightRtmpForwarding = 360;

	/**
	 * In SFU mode we still transcode the audio to opus and aac
	 * This settings determines the audio bitrate for opus and aac
	 * It's the bitrate that is used transcoding the audio in AAC and Opus
	 * After version(2.3), we directly forward incoming audio to the viewers without transcoding.
	 */
	@Value("${" + SETTINGS_AUDIO_BITRATE_SFU+":96000}")
	private int audioBitrateSFU = 96000;

	/**
	 * Enable/disable dash recording
	 */
	@Value( "${"+SETTINGS_DASH_MUXING_ENABLED+":false}" )
	private boolean dashMuxingEnabled;

	/** 
	 * If aacEncodingEnabled is true, aac encoding will be active even if mp4 or hls muxing is not enabled,
	 * If aacEncodingEnabled is false, aac encoding is only activated if mp4 or hls muxing is enabled in the settings,
	 *
	 * This value should be true if you're sending stream to RTMP endpoints or enable/disable mp4 recording on the fly
	 */
	@Value( "${"+SETTINGS_AAC_ENCODING_ENABLED+":true}" )
	private boolean aacEncodingEnabled = true;

	/**
	 * GOP size, AKA key frame interval,
	 * GOP size is group of pictures that encoder sends key frame for each group,
	 * The unit is not the seconds, Please don't confuse the seconds that are used in key frame intervals
	 *  
	 * If GOP size is 50 and your frame rate is 25, it means that encoder will send key frame 
	 * for every 2 seconds,
	 * 
	 * Default value is 0 so it uses incoming gop size by default.
	 * 
	 */
	@Value( "${"+SETTINGS_GOP_SIZE+":0}" )
	private int gopSize;

	/**
	 * Constant Rate Factor used by x264, x265, VP8,
	 * Use values between 4-51
	 * 
	 */
	@Value( "${"+SETTINGS_CONSTANT_RATE_FACTOR+":23}" )
	private String constantRateFactor = "23";

	/**
	 * Application level WebRTC viewer limit
	 */
	@Value( "${"+SETTINGS_WEBRTC_VIEWER_LIMIT+":-1}" )
	private int webRTCViewerLimit = -1;

	/**
	 * Set to true when you want to delete an application 
	 */
	private boolean toBeDeleted = false;

	/**
	 * Set to true when the app settings are only created for pulling the war file.
	 */
	private boolean pullWarFile = false;

	/**
	 * Address of the original place of the war file.
	 */
	private String warFileOriginServerAddress;


	/**
	 * Application JWT secret key for accessing the REST API
	 */
	@Value( "${"+SETTINGS_JWT_SECRET_KEY+":#{null}}" )
	private String jwtSecretKey;

	/**
	 * Application JWT Control Enabled for accessing the REST API
	 * TODO: Remove this field. Just check if jwtSecretKey is not empty then it means jwt filter is enabled
	 */
	@Value( "${"+SETTINGS_JWT_CONTROL_ENABLED+":false}" )
	private boolean jwtControlEnabled;

	@Value( "${"+SETTINGS_JWT_BLACKLIST_ENABLED+":false}" )
	private boolean jwtBlacklistEnabled;
	/**
	 * Application IP Filter Enabled
	 */
	@Value( "${"+SETTINGS_IP_FILTER_ENABLED+":true}" )
	private boolean ipFilterEnabled = true;

	/**
	 * Application level total incoming stream limit
	 */
	@Value( "${"+SETTINGS_INGESTING_STREAM_LIMIT+":-1}" )
	private int ingestingStreamLimit = -1;

	/**
	 * WebRTC Keyframe Time, Ant Media Server asks key frame for every webRTCKeyframeTime in SFU mode,
	 * It's in milliseconds
	 */
	@Value( "${"+SETTINGS_WEBRTC_KEYFRAME_TIME+":2000}" )
	private int webRTCKeyframeTime=2000;

	/**
	 * Application JWT stream secret key. Provide 32 character or more in length
	 */
	@Value( "${"+SETTINGS_JWT_STREAM_SECRET_KEY+":#{null}}" )
	private String jwtStreamSecretKey;

	/**
	 * The settings for enabling jwt token filter mechanism for accessing resources and publishing
	 */
	@Value( "${"+SETTINGS_PUBLISH_JWT_CONTROL_ENABLED+":false}" )
	private boolean publishJwtControlEnabled;

	/**
	 * The settings for enabling jwt token filter mechanism for accessing resources and playing
	 */
	@Value( "${"+SETTINGS_PLAY_JWT_CONTROL_ENABLED+":false}" )
	private boolean playJwtControlEnabled;

	/**
	 * Use http streaming in Low Latency Dash,
	 * If it's true, it sends files through http
	 * If it's false, it writes files to disk directly
	 * 
	 * In order to have Low Latency http streaming should be used
	 */
	@Value( "${"+SETTINGS_DASH_HTTP_STREAMING+":true}" )
	private boolean dashHttpStreaming=true;


	/**
	 * It's S3 streams MP4, WEBM  and HLS files storage name . 
	 * It's streams by default.
	 * 
	 */
	@Value( "${"+SETTINGS_S3_STREAMS_FOLDER_PATH+":streams}" )
	private String  s3StreamsFolderPath="streams";

	/**
	 * It's S3 stream PNG files storage name . 
	 * It's previews by default.
	 * 
	 */
	@Value( "${"+SETTINGS_S3_PREVIEWS_FOLDER_PATH+":previews}" )
	private String  s3PreviewsFolderPath="previews";

	/*
	 * Use http endpoint  in CMAF/HLS. 
	 * It's configurable to send any stream in HTTP Endpoint with this option
	 */
	@Value( "${"+SETTINGS_DASH_HTTP_ENDPOINT+":#{null}}" )
	private String dashHttpEndpoint;
	
	/**
	 * Http endpoint to push the HLS stream
	 */
	@Value( "${"+SETTINGS_HLS_HTTP_ENDPOINT+":#{null}}" )
	private String hlsHttpEndpoint;

	/**
	 * Force stream decoding even if there is no adaptive setting
	 */
	@Value("${" + SETTINGS_FORCE_DECODING+ ":false}")
	private boolean forceDecoding;

	/**
	 * Add the original hls stream to the playlist if adaptive bitrate setting is enabled
	 */
	@Value("${" + SETTINGS_ADD_ORIGINAL_MUXER_INTO_HLS_PLAYLIST+ ":true}")
	private boolean addOriginalMuxerIntoHLSPlaylist = true;

	/**
	 * Application JWT Control Enabled
	 */
	@Value( "${"+SETTINGS_S3_RECORDING_ENABLED+":false}" )
	private boolean s3RecordingEnabled;

	/**
	 * S3 Access key
	 */
	@Value( "${"+SETTINGS_S3_ACCESS_KEY+":#{null}}" )
	private String s3AccessKey;

	/**
	 * S3 Secret Key
	 */
	@Value( "${"+SETTINGS_S3_SECRET_KEY+":#{null}}" )
	private String s3SecretKey;

	/**
	 * S3 Bucket Name
	 */
	@Value( "${"+SETTINGS_S3_BUCKET_NAME+":#{null}}" )
	private String s3BucketName;

	/**
	 * S3 Region Name
	 */
	@Value( "${"+SETTINGS_S3_REGION_NAME+":#{null}}" )
	private String s3RegionName;

	/**
	 * S3 Endpoint
	 */
	@Value( "${"+SETTINGS_S3_ENDPOINT+":#{null}}" )
	private String s3Endpoint;

	/**
	 * S3 Cache Control Metadata
	 */
	@Value( "${"+SETTINGS_S3_CACHE_CONTROL+":no-store, no-cache, must-revalidate, max-age=0}" )
	private String s3CacheControl = "no-store, no-cache, must-revalidate, max-age=0";

	/*
	 * The permission to use in uploading the files to the S3. 
	 * Following values are accepted. Default value is public-read
	 * public-read
	 * private
	 * public-read-write
	 * authenticated-read
	 * log-delivery-write
	 * bucket-owner-read
	 * bucket-owner-full-control
	 * aws-exec-read
	 * 
	 */
	@Value( "${"+SETTINGS_S3_PERMISSION+":public-read}" )
	private String s3Permission = "public-read";

	/**
	 *  HLS Encryption key info file full path.
	 *  Format of the file
	 *  ```
	 *  key URI
	 *  key file path
	 *  IV (optional)
	 *  ``
	 *  
	 *  The first line of key_info_file specifies the key URI written to the playlist. 
	 *  The key URL is used to access the encryption key during playback. 
	 *  The second line specifies the path to the key file used to obtain the key during the encryption process. 
	 *  The key file is read as a single packed array of 16 octets in binary format. 
	 *  The optional third line specifies the initialization vector (IV) as a hexadecimal string to be used 
	 *  instead of the segment sequence number (default) for encryption. 
	 *  
	 *  Changes to key_info_file will result in segment encryption with the new key/IV and an entry in the playlist for the new key URI/IV if hls_flags periodic_rekey is enabled.
	 *
	 *  Key info file example:
	 *  ```
	 *  http://server/file.key
	 *  /path/to/file.key
	 *  0123456789ABCDEF0123456789ABCDEF
	 *  ```
	 */
	@Value( "${" + SETTINGS_HLS_ENCRYPTION_KEY_INFO_FILE +":#{null}}")
	private String hlsEncryptionKeyInfoFile;

	/*
	 * JWKS URL - it's effective if {@link#jwtControlEnabled} is true
	 * 
	 * It's null by default. If it's not null, JWKS is used to filter. 
	 * Otherwise it uses JWT
	 */

	@Value( "${" + SETTINGS_JWKS_URL +":#{null}}")
	private String jwksURL;

	/**
	 * This settings forces the aspect ratio to match the incoming aspect ratio perfectly.
	 * For instance, if the incoming source is 1280x720 and there is an adaptive bitrate with 480p
	 * There is no integer value that makes this equation true 1280/720 = x/480 -> x = 853.333
	 * 
	 *
	 * So Ant Media Server can change the video height to match the aspect ratio perfectly. 
	 * This is critical when there are multi-bitrates in the dash streaming. 
	 * Because dash requires perfect match of aspect ratios of all streams
	 * 
	 * The disadvantage of this approach is that there may be have some uncommon resolutions at the result of the transcoding.
	 * So that default value is false
	 * 
	 */
	@Value( "${" + SETTINGS_FORCE_ASPECT_RATIO_IN_TRANSCODING +":false}")
	private boolean forceAspectRatioInTranscoding;

	/**
	 * Enable Webhook Authentication when publishing streams
	 */
	@Value( "${"+SETTINGS_WEBHOOK_AUTHENTICATE_URL+":}" )
	private String webhookAuthenticateURL = "";
	
	/**
	 * The maximum audio track in a multitrack playing connection
	 * If it is -1 then a new audio track connection is established for each track
	 * otherwise, audio connections are established as many as this value and
	 * the limited connections are shared between tracks.
	 */
	@Value( "${"+SETTINGS_MAX_AUDIO_TRACK_COUNT+":-1}" )
	private int maxAudioTrackCount = -1;
	
	
	/**
	 * The maximum video track in a multitrack playing connection
	 * If it is -1 then a new video track connection is established for each track
	 * otherwise, video connections are established as many as this value and
	 * the limited connections are shared between tracks.
	 */
	@Value( "${"+SETTINGS_MAX_VIDEO_TRACK_COUNT+":-1}" )
	private int maxVideoTrackCount = -1;
	
	
	/**
	 * This is a script file path that is called by Runtime when VoD upload is finished,
	 * Bash script file path will be called after upload process finishes.
	 */
	@Value( "${"+SETTINGS_VOD_UPLOAD_FINISH_SCRIPT+":}" )
	private String vodUploadFinishScript = "";
	
	/**
	 * Value of the content security policy header(csp) 
	 * The new Content-Security-Policy HTTP response header helps you reduce XSS risks 
	 * on modern browsers by declaring which dynamic resources are allowed to load.
	 * 
	 * https://content-security-policy.com/
	 */
	@Value( "${"+SETTINGS_CONTENT_SECURITY_POLICY_HEADER_VALUE+":#{null}}" )
	private String contentSecurityPolicyHeaderValue;
	
	/**
	 * RTMP playback is not maintained and its support will be removed completely.
	 * It also causes some stability issues on the server side. 
	 * We highly recommend users to use CMAF(DASH) instead of RTMP playback 
	 */
	@Value( "${"+SETTINGS_RTMP_PLAYBACK_ENABLED +":false}" )
	private boolean rtmpPlaybackEnabled = false;
	
	
	/**
	 * The maximum idle time between origin and edge connection.
	 * After this timeout connection will be re-established if
	 * the stream is still active on origin.
	 */
	@Value( "${"+SETTINGS_ORIGIN_EDGE_CONNECTION_IDLE_TIMEOUT+":2}" )
	private int originEdgeIdleTimeout = 2;
	
	/**
	 * It's mandatory, Date and time are added to created .m3u8 and .ts file name, Default value is false
	 */
	@Value( "${"+SETTINGS_ADD_DATE_TIME_TO_HLS_FILE_NAME+":false}" )
	private boolean addDateTimeToHlsFileName;

	/**
	 * This setting prevents playing stream id more than once in the same websocket/webrtc session. 
	 * If it is true, trying to play stream id more than once in the same websocket session will produce 'already playing' error
	 * Default value is true.
	 * It uses session id to match subscriber
	 */
	@Value( "${"+SETTINGS_PLAY_WEBRTC_STREAM_ONCE_FOR_EACH_SESSION+":true}" )
	private boolean playWebRTCStreamOnceForEachSession = true;

	public boolean isWriteStatsToDatastore() {
		return writeStatsToDatastore;
	}


	/**
	 * Enables the WebRTC statistics based Adaptive Bitrate switch algorithm
	 */
	@Value( "${"+SETTINGS_STATS_BASED_ABR_ALGORITHM_ENABLED+":true}" )
	private boolean statsBasedABREnabled = true;

	/**
	 * Packet lost percentage to decide serving video with lower resolution
	 */
	@Value( "${"+SETTINGS_ABR_DOWN_SCALE_PACKET_LOST_RATIO+":1}" )
	private float abrDownScalePacketLostRatio = 1;

	/**
	 * Packet lost percentage to decide serving video with higher resolution
	 */
	@Value( "${"+SETTINGS_ABR_UP_SCALE_PACKET_LOST_RATIO+":0.1}" )
	private float abrUpScalePacketLostRatio = 0.1f;

	/**
	 * Round trip time in ms to decide serving video with higher resolution
	 */
	@Value( "${"+SETTINGS_ABR_UP_SCALE_RTT_MS+":150}" )
	private int abrUpScaleRTTMs = 150;

	/**
	 * Jitter in ms to decide serving video with higher resolution
	 */
	@Value( "${"+SETTINGS_ABR_UP_SCALE_JITTER_MS+":30}" )
	private int abrUpScaleJitterMs = 30;
	
	/**
	 * Key that is being used to validate the requests between communication in the cluster nodes
	 * 
	 * In initialization no matter if spring or field definition is effective, the important thing is that having some random value
	 */
	@Value( "${"+SETTINGS_CLUSTER_COMMUNICATION_KEY+ ":#{ T(org.apache.commons.lang3.RandomStringUtils).randomAlphanumeric(32)}" )
	private String clusterCommunicationKey = RandomStringUtils.randomAlphanumeric(32);

	public void setWriteStatsToDatastore(boolean writeStatsToDatastore) {
		this.writeStatsToDatastore = writeStatsToDatastore;
	}

	public boolean isAddDateTimeToMp4FileName() {
		return addDateTimeToMp4FileName;
	}

	public void setAddDateTimeToMp4FileName(boolean addDateTimeToMp4FileName) {
		this.addDateTimeToMp4FileName = addDateTimeToMp4FileName;
	}

	public boolean isMp4MuxingEnabled() {
		return mp4MuxingEnabled;
	}

	public void setMp4MuxingEnabled(boolean mp4MuxingEnabled) {
		this.mp4MuxingEnabled = mp4MuxingEnabled;
	}
	
	public void setFileNameFormat(String fileNameFormat) {
		this.fileNameFormat = fileNameFormat;
	}
	public String getFileNameFormat() {
		return fileNameFormat;
	}

	public boolean isHlsMuxingEnabled() {
		return hlsMuxingEnabled;
	}

	public void setHlsMuxingEnabled(boolean hlsMuxingEnabled) {
		this.hlsMuxingEnabled = hlsMuxingEnabled;
	}

	public boolean isDashMuxingEnabled() {
		return dashMuxingEnabled;
	}

	public void setSignalingEnabled(boolean signalingEnabled){
		this.signalingEnabled = signalingEnabled;
	}

	public boolean isSignalingEnabled(){
		return signalingEnabled;
	}

	public void setSignalingAddress(String signalingAddress){
		this.signalingAddress = signalingAddress;
	}
	public String getSignalingAddress(){
		return signalingAddress;
	}

	public void setDashMuxingEnabled(boolean dashMuxingEnabled) {
		this.dashMuxingEnabled = dashMuxingEnabled;
	}

	public int getEndpointRepublishLimit(){
		return endpointRepublishLimit;
	}
	public void setEndpointRepublishLimit(int endpointRepublishLimit){
		this.endpointRepublishLimit = endpointRepublishLimit;
	}
	public int getEndpointHealthCheckPeriodMs(){
		return endpointHealthCheckPeriodMs;
	}
	public void setEndpointHealthCheckPeriodMs(int endpointHealthCheckPeriodMs){
		this.endpointHealthCheckPeriodMs = endpointHealthCheckPeriodMs;
	}

	public String getHlsPlayListType() {
		return hlsPlayListType;
	}

	public void setHlsPlayListType(String hlsPlayListType) {
		this.hlsPlayListType = hlsPlayListType;
	}

	public void setUploadExtensionsToS3(int uploadExtensionsToS3){
		this.uploadExtensionsToS3 = uploadExtensionsToS3;
	}

	public int getUploadExtensionsToS3(){
		return this.uploadExtensionsToS3;
	}

	public void setS3StorageClass(String s3StorageClass){
		this.s3StorageClass = s3StorageClass;
	}
	public String getS3StorageClass(){
		return this.s3StorageClass;
	}

	public String getHlsTime() {
		return hlsTime;
	}

	public void setHlsTime(String hlsTime) {
		this.hlsTime = hlsTime;
	}

	public String getHlsListSize() {
		return hlsListSize;
	}

	public void setHlsListSize(String hlsListSize) {
		this.hlsListSize = hlsListSize;
	}

	public boolean isWebRTCEnabled() {
		return webRTCEnabled;
	}

	public void setWebRTCEnabled(boolean webRTCEnabled) {
		this.webRTCEnabled = webRTCEnabled;
	}

	public static String encodersList2Str(List<EncoderSettings> encoderSettingsList) 
	{
		if(encoderSettingsList == null) {
			return "";
		}

		JSONArray jsonArray = new JSONArray();

		for (EncoderSettings encoderSettings : encoderSettingsList) {
			JSONObject encoderJSON = new JSONObject();
			encoderJSON.put(EncoderSettings.RESOLUTION_HEIGHT, encoderSettings.getHeight());
			encoderJSON.put(EncoderSettings.VIDEO_BITRATE, encoderSettings.getVideoBitrate());
			encoderJSON.put(EncoderSettings.AUDIO_BITRATE, encoderSettings.getAudioBitrate());
			encoderJSON.put(EncoderSettings.FORCE_ENCODE, encoderSettings.isForceEncode());
			jsonArray.add(encoderJSON);
		}
		return jsonArray.toJSONString();
	}

	public static List<EncoderSettings> encodersStr2List(String encoderSettingsString)  {
		if(encoderSettingsString == null) {
			return null;
		}

		int height;
		int videoBitrate;
		int audioBitrate;
		boolean forceEncode;
		List<EncoderSettings> encoderSettingsList = new ArrayList<>();

		try {
			JSONParser jsonParser = new JSONParser();
			JSONArray jsonArray = (JSONArray) jsonParser.parse(encoderSettingsString);
			JSONObject jsObject;

			for (int i = 0; i < jsonArray.size(); i++) {
				jsObject =  (JSONObject)jsonArray.get(i);
				height = Integer.parseInt(jsObject.get(EncoderSettings.RESOLUTION_HEIGHT).toString());
				videoBitrate = Integer.parseInt(jsObject.get(EncoderSettings.VIDEO_BITRATE).toString());
				audioBitrate = Integer.parseInt(jsObject.get(EncoderSettings.AUDIO_BITRATE).toString());
				forceEncode = (boolean)jsObject.get(EncoderSettings.FORCE_ENCODE);
				encoderSettingsList.add(new EncoderSettings(height,videoBitrate,audioBitrate,forceEncode));
			}
		}
		catch (ParseException e) {
			// If there is old format, then try to add encoderSettingsList
			String[] values = encoderSettingsString.split(",");

			if (values.length >= 3){
				for (int i = 0; i < values.length; i++) {
					height = Integer.parseInt(values[i]);
					i++;
					videoBitrate = Integer.parseInt(values[i]);
					i++;
					audioBitrate = Integer.parseInt(values[i]);
					encoderSettingsList.add(new EncoderSettings(height, videoBitrate, audioBitrate,true));
				}
			}
		}
		return encoderSettingsList;
	}

	public String getEncoderSettingsString() {
		return encoderSettingsString;
	}

	public List<EncoderSettings> getEncoderSettings() {
		return encodersStr2List(encoderSettingsString);
	}

	public void setEncoderSettings(List<EncoderSettings> settings) {
		encoderSettingsString = encodersList2Str(settings);
	}

	public void setEncoderSettingsString(String encoderSettingsString) {
		this.encoderSettingsString = encoderSettingsString;
	}

	public boolean isDeleteHLSFilesOnEnded() {
		return deleteHLSFilesOnEnded;
	}

	public void setDeleteHLSFilesOnEnded(boolean deleteHLSFilesOnEnded) {
		this.deleteHLSFilesOnEnded = deleteHLSFilesOnEnded;
	}

	public String getListenerHookURL() {
		return listenerHookURL;
	}

	public void setListenerHookURL(String listenerHookURL) {
		this.listenerHookURL = listenerHookURL;
	}

	public boolean isAcceptOnlyStreamsInDataStore() {
		return acceptOnlyStreamsInDataStore;
	}

	public void setAcceptOnlyStreamsInDataStore(boolean acceptOnlyStreamsInDataStore) {
		this.acceptOnlyStreamsInDataStore = acceptOnlyStreamsInDataStore;
	}

	public boolean isAcceptOnlyRoomsInDataStore() {
		return acceptOnlyRoomsInDataStore;
	}

	public void setAcceptOnlyRoomsInDataStore(boolean acceptOnlyRoomsInDataStore) {
		this.acceptOnlyRoomsInDataStore = acceptOnlyRoomsInDataStore;
	}

	public boolean isObjectDetectionEnabled() {
		return objectDetectionEnabled;
	}

	public void setObjectDetectionEnabled(Boolean objectDetectionEnabled) {
		this.objectDetectionEnabled = objectDetectionEnabled;
	}

	public String getVodFolder() {
		return vodFolder;
	}

	public void setVodFolder(String vodFolder) {
		this.vodFolder = vodFolder;
	}

	public int getCreatePreviewPeriod() {
		return createPreviewPeriod;
	}

	public void setCreatePreviewPeriod(int period) {
		this.createPreviewPeriod = period;
	}

	public boolean isPreviewOverwrite() {
		return previewOverwrite;
	}

	public void setPreviewOverwrite(boolean previewOverwrite) {
		this.previewOverwrite = previewOverwrite;
	}

	public String getStalkerDBServer() {
		return stalkerDBServer;
	}

	public void setStalkerDBServer(String stalkerDBServer) {
		this.stalkerDBServer = stalkerDBServer;
	}

	public String getStalkerDBUsername() {
		return stalkerDBUsername;
	}

	public void setStalkerDBUsername(String stalkerDBUsername) {
		this.stalkerDBUsername = stalkerDBUsername;
	}

	public String getStalkerDBPassword() {
		return stalkerDBPassword;
	}

	public void setStalkerDBPassword(String stalkerDBPassword) {
		this.stalkerDBPassword = stalkerDBPassword;
	}

	public int getRestartStreamFetcherPeriod() {
		return this.restartStreamFetcherPeriod ;
	}

	public void setRestartStreamFetcherPeriod(int restartStreamFetcherPeriod) {
		this.restartStreamFetcherPeriod = restartStreamFetcherPeriod;
	}

	public int getStreamFetcherBufferTime() {
		return streamFetcherBufferTime;
	}

	public void setStreamFetcherBufferTime(int streamFetcherBufferTime) {
		this.streamFetcherBufferTime = streamFetcherBufferTime;
	}

	public String getHlsflags() {
		return hlsflags;
	}

	public void setHlsflags(String hlsflags) {
		this.hlsflags = hlsflags;
	}

	public String getMySqlClientPath() {
		return this.mySqlClientPath;

	}

	public void setMySqlClientPath(String mySqlClientPath) {
		this.mySqlClientPath = mySqlClientPath;
	}


	public boolean isPublishTokenControlEnabled() {
		return publishTokenControlEnabled;
	}

	public void setPublishTokenControlEnabled(boolean publishTokenControlEnabled) {
		this.publishTokenControlEnabled = publishTokenControlEnabled;
	}

	public boolean isPlayTokenControlEnabled() {
		return playTokenControlEnabled;
	}

	public void setPlayTokenControlEnabled(boolean playTokenControlEnabled) {
		this.playTokenControlEnabled = playTokenControlEnabled;
	}

	/**
	 * @Deprecated Please use {@link #isEnableTimeTokenForPlay()} or {@link #isEnableTimeTokenForPublish()}
	 * @return
	 */
	@Deprecated
	public boolean isTimeTokenSubscriberOnly() {
		return timeTokenSubscriberOnly;
	}

	@Deprecated
	public void setTimeTokenSubscriberOnly(boolean timeTokenSubscriberOnly) {
		this.timeTokenSubscriberOnly = timeTokenSubscriberOnly;
	}

	public boolean isEnableTimeTokenForPlay() {
		return enableTimeTokenForPlay;
	}

	public void setEnableTimeTokenForPlay(boolean enableTimeTokenForPlay) {
		this.enableTimeTokenForPlay = enableTimeTokenForPlay;
	}
	public boolean isEnableTimeTokenForPublish() {
		return enableTimeTokenForPublish;
	}

	public void setEnableTimeTokenForPublish(boolean enableTimeTokenForPublish) {
		this.enableTimeTokenForPublish = enableTimeTokenForPublish;
	}

	public String getMuxerFinishScript() {
		return muxerFinishScript;
	}

	public void setMuxerFinishScript(String muxerFinishScript) {
		this.muxerFinishScript = muxerFinishScript;
	}

	public int getWebRTCFrameRate() {
		return webRTCFrameRate;
	}

	public void setWebRTCFrameRate(int webRTCFrameRate) {
		this.webRTCFrameRate = webRTCFrameRate;
	}

	public boolean isCollectSocialMediaActivity() {
		return collectSocialMediaActivity;
	}

	public void setCollectSocialMediaActivity(boolean collectSocialMediaActivity) {
		this.collectSocialMediaActivity = collectSocialMediaActivity;
	}


	public String getTokenHashSecret() {
		return tokenHashSecret;
	}

	public void setTokenHashSecret(String tokenHashSecret) {
		this.tokenHashSecret = tokenHashSecret;
	}


	public boolean isHashControlPlayEnabled() {
		return hashControlPlayEnabled;
	}

	public void setHashControlPlayEnabled(boolean hashControlPlayEnabled) {
		this.hashControlPlayEnabled = hashControlPlayEnabled;
	}

	public boolean isHashControlPublishEnabled() {
		return hashControlPublishEnabled;
	}

	public void setHashControlPublishEnabled(boolean hashControlPublishEnabled) {
		this.hashControlPublishEnabled = hashControlPublishEnabled;
	}

	public void resetDefaults() {
		mp4MuxingEnabled = false;
		addDateTimeToMp4FileName = false;
		hlsMuxingEnabled = true;
		hlsListSize = null;
		hlsTime = null;
		webRTCEnabled = false;
		deleteHLSFilesOnEnded = true;
		deleteDASHFilesOnEnded = true;
		acceptOnlyStreamsInDataStore = false;
		publishTokenControlEnabled = false;
		playTokenControlEnabled = false;
		timeTokenSubscriberOnly = false;
		enableTimeTokenForPlay = false;
		enableTimeTokenForPublish = false;
		hlsPlayListType = null;
		previewOverwrite = false;
		objectDetectionEnabled = false;
		createPreviewPeriod = 5000;
		restartStreamFetcherPeriod = 0;
		webRTCFrameRate = 20;
		hashControlPlayEnabled = false;
		hashControlPublishEnabled = false;
		tokenHashSecret = "";
		encoderSettingsString = "";
		remoteAllowedCIDR = "127.0.0.1";
		aacEncodingEnabled=true;
		ipFilterEnabled=true;
		ingestingStreamLimit = -1;
	}

	public int getWebRTCPortRangeMax() {
		return webRTCPortRangeMax;
	}

	public void setWebRTCPortRangeMax(int webRTCPortRangeMax) {
		this.webRTCPortRangeMax = webRTCPortRangeMax;
	}

	public int getWebRTCPortRangeMin() {
		return webRTCPortRangeMin;
	}

	public void setWebRTCPortRangeMin(int webRTCPortRangeMin) {
		this.webRTCPortRangeMin = webRTCPortRangeMin;
	}

	public String getStunServerURI() {
		return stunServerURI;
	}

	public void setStunServerURI(String stunServerURI) {
		this.stunServerURI = stunServerURI;
	}

	public boolean isWebRTCTcpCandidatesEnabled() {
		return webRTCTcpCandidatesEnabled;
	}

	public void setWebRTCTcpCandidatesEnabled(boolean webRTCTcpCandidatesEnabled) {
		this.webRTCTcpCandidatesEnabled = webRTCTcpCandidatesEnabled;
	}

	public String getEncoderName() {
		return encoderName;
	}

	public void setEncoderName(String encoderName) {
		this.encoderName = encoderName;
	}

	public String getEncoderPreset() {
		return encoderPreset;
	}

	public void setEncoderPreset(String encoderPreset) {
		this.encoderPreset = encoderPreset;
	}

	public String getEncoderProfile() {
		return encoderProfile;
	}

	public void setEncoderProfile(String encoderProfile) {
		this.encoderProfile = encoderProfile;
	}

	public String getEncoderLevel() {
		return encoderLevel;
	}

	public void setEncoderLevel(String encoderLevel) {
		this.encoderLevel = encoderLevel;
	}

	public String getEncoderRc() {
		return encoderRc;
	}

	public void setEncoderRc(String encoderRc) {
		this.encoderRc = encoderRc;
	}

	public String getEncoderSpecific() {
		return encoderSpecific;
	}

	public void setEncoderSpecific(String encoderSpecific) {
		this.encoderSpecific = encoderSpecific;
	}

	public int getPreviewHeight() {
		return previewHeight;
	}

	public void setPreviewHeight(int previewHeight) {
		this.previewHeight = previewHeight;
	}

	public boolean isUseOriginalWebRTCEnabled() {
		return useOriginalWebRTCEnabled;
	}

	public void setUseOriginalWebRTCEnabled(boolean useOriginalWebRTCEnabled) {
		this.useOriginalWebRTCEnabled = useOriginalWebRTCEnabled;
	}

	public synchronized String getRemoteAllowedCIDR() {
		return remoteAllowedCIDR;
	}

	/**
	 * the getAllowedCIDRList and setAllowedCIDRList are synchronized
	 * because ArrayList may throw concurrent modification
	 * @param remoteAllowedCIDR
	 */
	public synchronized void setRemoteAllowedCIDR(String remoteAllowedCIDR) {
		this.remoteAllowedCIDR = remoteAllowedCIDR;	
	}

	@JsonIgnore
	public synchronized Queue<NetMask> getAllowedCIDRList() 
	{
		Queue<NetMask> allowedCIDRList = new ConcurrentLinkedQueue<>();
		fillFromInput(remoteAllowedCIDR, allowedCIDRList);
		return allowedCIDRList;
	}

	public String getAllowedPublisherCIDR() {
		return allowedPublisherCIDR;
	}

	public void setAllowedPublisherCIDR(String allowedPublisherCIDR) 
	{
		this.allowedPublisherCIDR = allowedPublisherCIDR;	
	}

	@JsonIgnore
	public synchronized Queue<NetMask> getAllowedPublisherCIDRList() 
	{
		Queue<NetMask> allowedPublisherCIDRList = new ConcurrentLinkedQueue<>();
		fillFromInput(allowedPublisherCIDR, allowedPublisherCIDRList);
		return allowedPublisherCIDRList;
	}


	/**
	 * Fill a {@link NetMask} list from a string input containing a
	 * comma-separated list of (hopefully valid) {@link NetMask}s.
	 *
	 * @param input The input string
	 * @param target The list to fill
	 * @return a string list of processing errors (empty when no errors)
	 */
	private List<String> fillFromInput(final String input, final Queue<NetMask> target) {
		target.clear();
		if (input == null || input.isEmpty()) {
			return Collections.emptyList();
		}

		final List<String> messages = new LinkedList<>();
		NetMask nm;

		for (final String s : input.split("\\s*,\\s*")) {
			try {
				nm = new NetMask(s);
				target.add(nm);
			} catch (IllegalArgumentException e) {
				messages.add(s + ": " + e.getMessage());
			}
		}

		return Collections.unmodifiableList(messages);
	}

	public String getEncoderSelectionPreference() {
		return encoderSelectionPreference;
	}

	public void setEncoderSelectionPreference(String encoderSelectionPreference) {
		this.encoderSelectionPreference = encoderSelectionPreference;
	}

	public int getExcessiveBandwidthCallThreshold() {
		return excessiveBandwidthCallThreshold;
	}

	public void setExcessiveBandwidthCallThreshold(int excessiveBandwidthCallThreshold) {
		this.excessiveBandwidthCallThreshold = excessiveBandwidthCallThreshold;
	}

	public int getExcessiveBandwidthValue() {
		return excessiveBandwidthValue;
	}

	public void setExcessiveBandwidthValue(int excessiveBandwidthValue) {
		this.excessiveBandwidthValue = excessiveBandwidthValue;
	}

	public int getPortAllocatorFlags() {
		return portAllocatorFlags;
	}

	public void setPortAllocatorFlags(int flags) {
		this.portAllocatorFlags = flags;
	}

	public int getExcessiveBandwithTryCountBeforeSwitchback() {
		return excessiveBandwithTryCountBeforeSwitchback;
	}

	public boolean isExcessiveBandwidthAlgorithmEnabled() {
		return excessiveBandwidthAlgorithmEnabled;
	}

	public int getPacketLossDiffThresholdForSwitchback() {
		return packetLossDiffThresholdForSwitchback;
	}

	public int getRttMeasurementDiffThresholdForSwitchback() {
		return rttMeasurementDiffThresholdForSwitchback;
	}

	public void setExcessiveBandwithTryCountBeforeSwitchback(int excessiveBandwithTryCountBeforeSwitchback) {
		this.excessiveBandwithTryCountBeforeSwitchback = excessiveBandwithTryCountBeforeSwitchback;
	}

	public void setExcessiveBandwidthAlgorithmEnabled(boolean excessiveBandwidthAlgorithmEnabled) {
		this.excessiveBandwidthAlgorithmEnabled = excessiveBandwidthAlgorithmEnabled;
	}

	public void setPacketLossDiffThresholdForSwitchback(int packetLossDiffThresholdForSwitchback) {
		this.packetLossDiffThresholdForSwitchback = packetLossDiffThresholdForSwitchback;
	}

	public void setRttMeasurementDiffThresholdForSwitchback(int rttMeasurementDiffThresholdForSwitchback) {
		this.rttMeasurementDiffThresholdForSwitchback = rttMeasurementDiffThresholdForSwitchback;
	}

	public boolean isReplaceCandidateAddrWithServerAddr() {
		return this.replaceCandidateAddrWithServerAddr;
	}

	public void setReplaceCandidateAddrWithServerAddr(boolean replaceCandidateAddrWithServerAddr) {
		this.replaceCandidateAddrWithServerAddr = replaceCandidateAddrWithServerAddr;
	}

	public long getUpdateTime() {
		return updateTime;
	}

	public void setUpdateTime(long updateTime) {
		this.updateTime = updateTime;
	}

	public void setAppName(String appName) {
		this.appName = appName;
	}

	public String getAppName() {
		return appName;
	}

	public int getEncodingTimeout() {
		return encodingTimeout;
	}

	public void setEncodingTimeout(int encodingTimeout) {
		this.encodingTimeout = encodingTimeout;
	}

	public boolean isDefaultDecodersEnabled() {
		return defaultDecodersEnabled;
	}

	public void setDefaultDecodersEnabled(boolean defaultDecodersEnabled) {
		this.defaultDecodersEnabled = defaultDecodersEnabled;
	}

	public String getHttpForwardingExtension() {
		return httpForwardingExtension;
	}

	public void setHttpForwardingExtension(String httpForwardingExtension) {
		this.httpForwardingExtension = httpForwardingExtension;
	}

	public String getHttpForwardingBaseURL() {
		return httpForwardingBaseURL;
	}

	public void setHttpForwardingBaseURL(String httpForwardingBaseURL) {
		this.httpForwardingBaseURL = httpForwardingBaseURL;
	}

	public int getMaxAnalyzeDurationMS() {
		return maxAnalyzeDurationMS;
	}

	public void setMaxAnalyzeDurationMS(int maxAnalyzeDurationMS) {
		this.maxAnalyzeDurationMS = maxAnalyzeDurationMS;
	}

	public boolean isGeneratePreview() {
		return generatePreview;
	}

	public void setGeneratePreview(boolean generatePreview) {
		this.generatePreview = generatePreview;
	}

	public boolean isDisableIPv6Candidates() {
		return disableIPv6Candidates;
	}

	public void setDisableIPv6Candidates(boolean disableIPv6Candidates) {
		this.disableIPv6Candidates = disableIPv6Candidates;
	}

	public String getRtspPullTransportType() {
		return rtspPullTransportType;
	}

	public void setRtspPullTransportType(String rtspPullTransportType) {
		this.rtspPullTransportType = rtspPullTransportType;
	}
	public int getRtspTimeoutDurationMs() {
		return rtspTimeoutDurationMs;
	}

	public void setRtspTimeoutDurationMs(int rtspTimeoutDurationMs) {
		this.rtspTimeoutDurationMs = rtspTimeoutDurationMs;
	}

	public int getMaxResolutionAccept() {
		return maxResolutionAccept;
	}

	public void setMaxResolutionAccept(int maxResolutionAccept) {
		this.maxResolutionAccept = maxResolutionAccept;
	}

	public boolean isH264Enabled() {
		return h264Enabled;
	}

	public void setH264Enabled(boolean h264Enabled) {
		this.h264Enabled = h264Enabled;
	}

	public boolean isVp8Enabled() {
		return vp8Enabled;
	}

	public void setVp8Enabled(boolean vp8Enabled) {
		this.vp8Enabled = vp8Enabled;
	}

	public boolean isH265Enabled() {
		return h265Enabled;
	}

	public void setH265Enabled(boolean h265Enabled) {
		this.h265Enabled = h265Enabled;
	}

	public boolean isDataChannelEnabled() {
		return dataChannelEnabled;
	}

	public void setDataChannelEnabled(boolean dataChannelEnabled) {
		this.dataChannelEnabled = dataChannelEnabled;
	}

	public String getDataChannelPlayerDistribution() {
		return dataChannelPlayerDistribution;
	}

	public void setDataChannelPlayerDistribution(String dataChannelPlayerDistribution) {
		this.dataChannelPlayerDistribution = dataChannelPlayerDistribution;
	}

	public long getRtmpIngestBufferTimeMs() {
		return rtmpIngestBufferTimeMs;
	}

	public void setRtmpIngestBufferTimeMs(long rtmpIngestBufferTimeMs) {
		this.rtmpIngestBufferTimeMs = rtmpIngestBufferTimeMs;
	}

	public String getDataChannelWebHook() {
		return dataChannelWebHookURL;
	}

	public void setDataChannelWebHookURL(String dataChannelWebHookURL) {
		this.dataChannelWebHookURL = dataChannelWebHookURL;
	}

	public int getEncoderThreadCount() {
		return encoderThreadCount;
	}

	public void setEncoderThreadCount(int encoderThreadCount) {
		this.encoderThreadCount = encoderThreadCount;
	}

	public int getEncoderThreadType() {
		return encoderThreadType;
	}

	public void setEncoderThreadType(int encoderThreadType) {
		this.encoderThreadType = encoderThreadType;
	}

	public int getWebRTCClientStartTimeoutMs() {
		return webRTCClientStartTimeoutMs;
	}

	public void setWebRTCClientStartTimeoutMs(int webRTCClientStartTimeout) {
		this.webRTCClientStartTimeoutMs = webRTCClientStartTimeout;
	}

	public String getH265EncoderProfile() {
		return this.h265EncoderProfile;
	}

	public String getH265EncoderPreset() {
		return this.h265EncoderPreset;
	}

	public String getH265EncoderLevel() {
		return this.h265EncoderLevel;
	}

	public String getH265EncoderSpecific() {
		return this.h265EncoderSpecific;
	}

	public String getH265EncoderRc() {
		return this.h265EncoderRc ;
	}

	public void setH265EncoderLevel(String encoderLevel) {
		this.h265EncoderLevel = encoderLevel;
	}

	public void setH265EncoderPreset(String preset) {
		this.h265EncoderPreset = preset;
	}

	public void setH265EncoderProfile(String profile) {
		this.h265EncoderProfile = profile;
	}

	public void setH265EncoderRc(String encoderRc) {
		this.h265EncoderRc = encoderRc;
	}

	public void setH265EncoderSpecific(String encoderSpecific) {
		this.h265EncoderSpecific = encoderSpecific;
	}

	public boolean isWebMMuxingEnabled() {
		return webMMuxingEnabled;
	}

	public void setWebMMuxingEnabled(boolean webMMuxingEnabled) {
		this.webMMuxingEnabled = webMMuxingEnabled;
	}

	public int getVp8EncoderSpeed() {
		return vp8EncoderSpeed;
	}

	public void setVp8EncoderSpeed(int vp8EncoderSpeed) {
		this.vp8EncoderSpeed = vp8EncoderSpeed;
	}

	public String getVp8EncoderDeadline() {
		return vp8EncoderDeadline;
	}

	public void setVp8EncoderDeadline(String vp8EncoderDeadline) {
		this.vp8EncoderDeadline = vp8EncoderDeadline;
	}

	public int getVp8EncoderThreadCount() {
		return vp8EncoderThreadCount;
	}

	public void setVp8EncoderThreadCount(int vp8EncoderThreadCount) {
		this.vp8EncoderThreadCount = vp8EncoderThreadCount;
	}

	public String getWebRTCSdpSemantics() {
		return webRTCSdpSemantics;
	}

	public void setWebRTCSdpSemantics(String webRTCSdpSemantics) {
		this.webRTCSdpSemantics = webRTCSdpSemantics;
	}

	public boolean isStartStreamFetcherAutomatically() {
		return startStreamFetcherAutomatically;
	}

	public void setStartStreamFetcherAutomatically(boolean startStreamFetcherAutomatically) {
		this.startStreamFetcherAutomatically = startStreamFetcherAutomatically;
	}

	public boolean isDeleteDASHFilesOnEnded() {
		return deleteDASHFilesOnEnded;
	}

	public void setDeleteDASHFilesOnEnded(boolean deleteDASHFilesOnEnded) {
		this.deleteDASHFilesOnEnded = deleteDASHFilesOnEnded;
	}

	public String getTargetLatency() {
		return targetLatency;
	}

	public void setTargetLatency(String targetLatency) {
		this.targetLatency = targetLatency;
	}

	public int getHeightRtmpForwarding() {
		return heightRtmpForwarding;
	}

	public void setHeightRtmpForwarding(int heightRtmpForwarding) {
		this.heightRtmpForwarding = heightRtmpForwarding;
	}

	public int getAudioBitrateSFU() {
		return audioBitrateSFU;
	}

	public void setAudioBitrateSFU(int audioBitrateSFU) {
		this.audioBitrateSFU = audioBitrateSFU;
	}

	public void setAacEncodingEnabled(boolean aacEncodingEnabled){
		this.aacEncodingEnabled=aacEncodingEnabled;
	}

	public boolean isAacEncodingEnabled() {
		return aacEncodingEnabled;
	}

	public int getGopSize() {
		return gopSize;
	}

	public void setGopSize(int gopSize) {
		this.gopSize = gopSize;
	}

	public String getConstantRateFactor() {
		return constantRateFactor;
	}

	public void setConstantRateFactor(String constantRateFactor) {
		this.constantRateFactor = constantRateFactor;
	}

	public int getWebRTCViewerLimit() {
		return webRTCViewerLimit;
	}

	public void setWebRTCViewerLimit(int webRTCViewerLimit) {
		this.webRTCViewerLimit = webRTCViewerLimit;
	}

	public String getDashFragmentDuration() {
		return dashFragmentDuration;
	}

	public void setDashFragmentDuration(String dashFragmentDuration) {
		this.dashFragmentDuration = dashFragmentDuration;
	}

	public String getDashSegDuration() {
		return dashSegDuration;
	}

	public void setDashSegDuration(String dashSegDuration) {
		this.dashSegDuration = dashSegDuration;
	}

	public String getDashWindowSize() {
		return dashWindowSize;
	}

	public void setDashWindowSize(String dashWindowSize) {
		this.dashWindowSize = dashWindowSize;
	}

	public String getDashExtraWindowSize() {
		return dashExtraWindowSize;
	}

	public void setDashExtraWindowSize(String dashExtraWindowSize) {
		this.dashExtraWindowSize = dashExtraWindowSize;
	}

	public String getJwtSecretKey() {
		return jwtSecretKey;
	}

	public void setJwtSecretKey(String jwtSecretKey) {
		this.jwtSecretKey = jwtSecretKey;
	}

	public boolean isJwtControlEnabled() {
		return jwtControlEnabled;
	}

	public void setJwtBlacklistEnabled(boolean jwtBlacklistEnabled){
		this.jwtBlacklistEnabled = jwtBlacklistEnabled;
	}

	public boolean isJwtBlacklistEnabled() {
		return jwtBlacklistEnabled;
	}
	public void setJwtControlEnabled(boolean jwtControlEnabled) {
		this.jwtControlEnabled = jwtControlEnabled;
	}

	public boolean isIpFilterEnabled() {
		return ipFilterEnabled;
	}

	public void setIpFilterEnabled(boolean ipFilterEnabled) {
		this.ipFilterEnabled = ipFilterEnabled;
	}

	public int getIngestingStreamLimit() {
		return ingestingStreamLimit;
	}

	public void setIngestingStreamLimit(int ingestingStreamLimit) {
		this.ingestingStreamLimit = ingestingStreamLimit;
	}

	public int getTimeTokenPeriod() {
		return timeTokenPeriod;
	}

	public void setTimeTokenPeriod(int timeTokenPeriod) {
		this.timeTokenPeriod = timeTokenPeriod;
	}

	public boolean isToBeDeleted() {
		return toBeDeleted;
	}

	public void setToBeDeleted(boolean toBeDeleted) {
		this.toBeDeleted = toBeDeleted;
	}

	public boolean isPullWarFile() {
		return pullWarFile;
	}

	public void setPullWarFile(boolean pullWarFile) {
		this.pullWarFile = pullWarFile;
	}

	public int getWebRTCKeyframeTime() {
		return webRTCKeyframeTime;
	}

	public void setWebRTCKeyframeTime(int webRTCKeyframeTime) {
		this.webRTCKeyframeTime = webRTCKeyframeTime;
	}

	public String getJwtStreamSecretKey() {
		return jwtStreamSecretKey;
	}

	public void setJwtStreamSecretKey(String jwtStreamSecretKey) {
		this.jwtStreamSecretKey = jwtStreamSecretKey;
	}

	public boolean isPublishJwtControlEnabled() {
		return publishJwtControlEnabled;
	}

	public void setPublishJwtControlEnabled(boolean publishJwtControlEnabled) {
		this.publishJwtControlEnabled = publishJwtControlEnabled;
	}

	public boolean isPlayJwtControlEnabled() {
		return playJwtControlEnabled;
	}

	public void setPlayJwtControlEnabled(boolean playJwtControlEnabled) {
		this.playJwtControlEnabled = playJwtControlEnabled;
	}

	public boolean islLDashEnabled() {
		return lLDashEnabled;
	}

	public void setlLDashEnabled(boolean lLDashEnabled) {
		this.lLDashEnabled = lLDashEnabled;
	}

	public boolean islLHLSEnabled() {
		return lLHLSEnabled;
	}

	public void setlLHLSEnabled(boolean lLHLSEnabled) {
		this.lLHLSEnabled = lLHLSEnabled;
	}

	public boolean isHlsEnabledViaDash() {
		return hlsEnabledViaDash;
	}

	public void setHlsEnabledViaDash(boolean hlsEnabledViaDash) {
		this.hlsEnabledViaDash = hlsEnabledViaDash;
	}

	public boolean isUseTimelineDashMuxing() {
		return useTimelineDashMuxing;
	}

	public void setUseTimelineDashMuxing(boolean useTimelineDashMuxing) {
		this.useTimelineDashMuxing = useTimelineDashMuxing;
	}

	public boolean isDashHttpStreaming() {
		return dashHttpStreaming;
	}

	public void setDashHttpStreaming(boolean dashHttpStreaming) {
		this.dashHttpStreaming = dashHttpStreaming;
	}

	public String getS3StreamsFolderPath() {
		return s3StreamsFolderPath;
	}

	public String getDashHttpEndpoint() {
		return dashHttpEndpoint;
	}


	public boolean isS3RecordingEnabled() { return s3RecordingEnabled; }

	public void setS3RecordingEnabled(boolean s3RecordingEnabled) {
		this.s3RecordingEnabled = s3RecordingEnabled;
	}

	public String getS3SecretKey() {
		return s3SecretKey;
	}

	public void setS3SecretKey(String s3SecretKey) { this.s3SecretKey = s3SecretKey; }

	public String getS3AccessKey() {
		return s3AccessKey;
	}

	public void setS3AccessKey(String s3AccessKey) {
		this.s3AccessKey = s3AccessKey;
	}

	public String getS3RegionName() {
		return s3RegionName;
	}

	public void setS3RegionName(String s3RegionName) {
		this.s3RegionName = s3RegionName;
	}

	public String getS3BucketName() {
		return s3BucketName;
	}

	public void setS3BucketName(String s3BucketName) {
		this.s3BucketName = s3BucketName;
	}

	public String getS3Endpoint() {
		return s3Endpoint;
	}

	public void setS3Endpoint(String s3Endpoint) {
		this.s3Endpoint = s3Endpoint;
	}

	public String getS3CacheControl() {
		return s3CacheControl;
	}

	public void setS3CacheControl(String s3CacheControl) {
		this.s3CacheControl = s3CacheControl;
	}

	public void setDashHttpEndpoint(String dashHttpEndpoint) {
		this.dashHttpEndpoint = dashHttpEndpoint;
	}

	public String getHlsEncryptionKeyInfoFile() {
		return hlsEncryptionKeyInfoFile;
	}

	public void setHlsEncryptionKeyInfoFile(String hlsEncryptionKeyInfoFile) {
		this.hlsEncryptionKeyInfoFile = hlsEncryptionKeyInfoFile;
	}

	public void setS3StreamsFolderPath(String s3StreamsFolderPath) {
		this.s3StreamsFolderPath = s3StreamsFolderPath;
	}

	public String getS3PreviewsFolderPath() {
		return s3PreviewsFolderPath;
	}

	public void setS3PreviewsFolderPath(String s3PreviewsFolderPath) {
		this.s3PreviewsFolderPath = s3PreviewsFolderPath;
	}

	public boolean isForceDecoding() {
		return forceDecoding;
	}

	public void setForceDecoding(boolean forceDecoding) {
		this.forceDecoding = forceDecoding;
	}

	public boolean isAddOriginalMuxerIntoHLSPlaylist() {
		return addOriginalMuxerIntoHLSPlaylist;
	}

	public void setAddOriginalMuxerIntoHLSPlaylist(boolean addOriginalMuxerIntoHLSPlaylist) {
		this.addOriginalMuxerIntoHLSPlaylist = addOriginalMuxerIntoHLSPlaylist;
	}

	public String getJwksURL() {
		return jwksURL;
	}

	public void setJwksURL(String jwksURL) {
		this.jwksURL = jwksURL;
	}


	public String getWebhookAuthenticateURL(){
		return webhookAuthenticateURL;
	}

	public void setWebhookAuthenticateURL(String webhookAuthenticateURL) {
		this.webhookAuthenticateURL = webhookAuthenticateURL;
	}

	public boolean isForceAspectRatioInTranscoding() {
		return forceAspectRatioInTranscoding;
	}

	public void setForceAspectRatioInTranscoding(boolean forceAspectRatioInTranscoding) {
		this.forceAspectRatioInTranscoding = forceAspectRatioInTranscoding;

	}

	public String getS3Permission() {
		return s3Permission;
	}

	public void setS3Permission(String s3Permission) {
		this.s3Permission = s3Permission;
	}

	public int getMaxAudioTrackCount() {
		return maxAudioTrackCount;
	}

	public void setMaxAudioTrackCount(int maxAudioTrackCount) {
		this.maxAudioTrackCount = maxAudioTrackCount;
	}

	public String getWarFileOriginServerAddress() {
		return warFileOriginServerAddress;
	}

	public void setWarFileOriginServerAddress(String warFileOriginServerAddress) {
		this.warFileOriginServerAddress = warFileOriginServerAddress;
	}
	
	public String getVodFinishScript() {
		return vodUploadFinishScript;
	}

	public void setVodUploadFinishScript(String vodUploadFinishScript) {
		this.vodUploadFinishScript = vodUploadFinishScript;
	}

	public int getMaxVideoTrackCount() {
		return maxVideoTrackCount;
	}

	public void setMaxVideoTrackCount(int maxVideoTrackCount) {
		this.maxVideoTrackCount = maxVideoTrackCount;
	}
	
	public String getContentSecurityPolicyHeaderValue() {
		return contentSecurityPolicyHeaderValue;
	}

	public void setContentSecurityPolicyHeaderValue(String contentSecurityPolicyHeaderValue) {
		this.contentSecurityPolicyHeaderValue = contentSecurityPolicyHeaderValue;
	}

	public String getTurnServerUsername() {
		return turnServerUsername;
	}

	public void setTurnServerUsername(String turnServerUsername) {
		this.turnServerUsername = turnServerUsername;
	}

	public String getTurnServerCredential() {
		return turnServerCredential;
	}

	public void setTurnServerCredential(String turnServerCredential) {
		this.turnServerCredential = turnServerCredential;
	}

	public String getHlsHttpEndpoint() {
		return hlsHttpEndpoint;
	}

	public void setHlsHttpEndpoint(String hlsHttpEndpoint) {
		this.hlsHttpEndpoint = hlsHttpEndpoint;
	}

	public boolean isRtmpPlaybackEnabled() {
		return rtmpPlaybackEnabled;
	}

	public void setRtmpPlaybackEnabled(boolean rtmpPlaybackEnabled) {
		this.rtmpPlaybackEnabled = rtmpPlaybackEnabled;
	}

	public int getOriginEdgeIdleTimeout() {
		return originEdgeIdleTimeout;
	}

	public void setOriginEdgeIdleTimeout(int originEdgeIdleTimeout) {
		this.originEdgeIdleTimeout = originEdgeIdleTimeout;
	}
	
	public boolean isAddDateTimeToHlsFileName() {
		return addDateTimeToHlsFileName;
	}

	public void setAddDateTimeToHlsFileName(boolean addDateTimeToHlsFileName) {
		this.addDateTimeToHlsFileName = addDateTimeToHlsFileName;
	}

	public boolean isPlayWebRTCStreamOnceForEachSession() {
		return playWebRTCStreamOnceForEachSession;
	}

	public void setPlayWebRTCStreamOnceForEachSession(boolean playWebRTCStreamOnceForEachSession) {
		this.playWebRTCStreamOnceForEachSession = playWebRTCStreamOnceForEachSession;
	}

	public boolean isStatsBasedABREnabled() {
		return statsBasedABREnabled;
	}

	public void setStatsBasedABREnabled(boolean statsBasedABREnabled) {
		this.statsBasedABREnabled = statsBasedABREnabled;
	}

	public float getAbrDownScalePacketLostRatio() {
		return abrDownScalePacketLostRatio;
	}

	public void setAbrDownScalePacketLostRatio(float abrDownScalePacketLostRatio) {
		this.abrDownScalePacketLostRatio = abrDownScalePacketLostRatio;
	}

	public float getAbrUpScalePacketLostRatio() {
		return abrUpScalePacketLostRatio;
	}

	public void setAbrUpScalePacketLostRatio(float abrUpScalePacketLostRatio) {
		this.abrUpScalePacketLostRatio = abrUpScalePacketLostRatio;
	}

	public int getAbrUpScaleRTTMs() {
		return abrUpScaleRTTMs;
	}

	public void setAbrUpScaleRTTMs(int abrUpScaleRTTMs) {
		this.abrUpScaleRTTMs = abrUpScaleRTTMs;
	}

	public int getAbrUpScaleJitterMs() {
		return abrUpScaleJitterMs;
	}

	public void setAbrUpScaleJitterMs(int abrUpScaleJitterMs) {
		this.abrUpScaleJitterMs = abrUpScaleJitterMs;
	}

	public String getClusterCommunicationKey() {
		return clusterCommunicationKey;
	}

	public void setClusterCommunicationKey(String clusterCommunicationKey) {
		this.clusterCommunicationKey = clusterCommunicationKey;
	}

}
