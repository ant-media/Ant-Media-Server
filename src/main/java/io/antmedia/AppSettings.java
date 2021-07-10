package io.antmedia;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.catalina.util.NetMask;
import org.bson.types.ObjectId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Field;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Index;
import dev.morphia.annotations.Indexes;
import dev.morphia.annotations.NotSaved;

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
public class AppSettings {
	
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
	public static final String FACEBOOK_CLIENT_ID = "facebook.clientId";
	public static final String FACEBOOK_CLIENT_SECRET = "facebook.clientSecret";
	public static final String PERISCOPE_CLIENT_ID = "periscope.clientId";
	public static final String PERISCOPE_CLIENT_SECRET = "periscope.clientSecret";
	public static final String YOUTUBE_CLIENT_ID = "youtube.clientId";
	public static final String YOUTUBE_CLIENT_SECRET = "youtube.clientSecret";
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
	public static final String SETTINGS_WEBRTC_TCP_CANDIDATE_ENABLED = "settings.webrtc.tcpCandidateEnabled"; 
	public static final String SETTINGS_WEBRTC_SDP_SEMANTICS = "settings.webrtc.sdpSemantics"; 

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

	private static final String SETTINGS_HLS_FLAGS = "settings.hlsflags";
	
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
	
	public static final String SETTINGS_IP_FILTER_ENABLED = "settings.ipFilterEnabled";

	private static final String SETTINGS_INGESTING_STREAM_LIMIT = "settings.ingestingStreamLimit";
	
	private static final String SETTINGS_WEBRTC_KEYFRAME_TIME = "settings.webRTCKeyframeTime";
	
	public static final String SETTINGS_JWT_STREAM_SECRET_KEY = "settings.jwtStreamSecretKey";
	
	public static final String SETTINGS_PLAY_JWT_CONTROL_ENABLED = "settings.playJwtControlEnabled";
	
	public static final String SETTINGS_PUBLISH_JWT_CONTROL_ENABLED = "settings.publishJwtControlEnabled";
	
	private static final String SETTINGS_DASH_ENABLE_LOW_LATENCY = "settings.dash.llEnabled";

	private static final String SETTINGS_HLS_ENABLE_LOW_LATENCY = "settings.dash.llHlsEnabled";

	private static final String SETTINGS_HLS_ENABLED_VIA_DASH_LOW_LATENCY = "settings.dash.hlsEnabled";

	private static final String SETTINGS_USE_TIMELINE_DASH_MUXING = "settings.dash.useTimeline";

	private static final String SETTINGS_DASH_HTTP_STREAMING = "settings.dash.httpStreaming";
	
	private static final String SETTINGS_S3_STREAMS_FOLDER_PATH = "settings.s3.streams.folder.path";
	
	private static final String SETTINGS_S3_PREVIEWS_FOLDER_PATH = "settings.s3.previews.folder.path";
	
	private static final String SETTINGS_DASH_HTTP_ENDPOINT = "settings.dash.httpEndpoint";
	
	private static final String SETTINGS_FORCE_DECODING = "settings.forceDecoding";

	public static final String SETTINGS_S3_RECORDING_ENABLED = "settings.s3RecordingEnabled";

	public static final String SETTINGS_S3_ACCESS_KEY = "settings.s3AccessKey";
	public static final String SETTINGS_S3_SECRET_KEY = "settings.s3SecretKey";
	public static final String SETTINGS_S3_REGION_NAME = "settings.s3RegionName";
	public static final String SETTINGS_S3_BUCKET_NAME = "settings.s3BucketName";
	public static final String SETTINGS_S3_ENDPOINT = "settings.s3Endpoint";
	public static final String SETTINGS_ENABLE_TIME_TOKEN_PLAY = "settings.enableTimeTokenForPlay";
	public static final String SETTINGS_ENABLE_TIME_TOKEN_PUBLISH = "settings.enableTimeTokenForPublish";
	
	public static final String SETTINGS_HLS_ENCRYPTION_KEY_INFO_FILE = "settings.hlsEncryptionKeyInfoFile";
	
	public static final String SETTINGS_JWKS_URL = "settings.jwksURL";
	
	public static final String SETTINGS_ENCODING_RESOLUTION_CHECK = "settings.encoding.resolutionCheck";
	

	@JsonIgnore
	@NotSaved
	private List<NetMask> allowedCIDRList = new ArrayList<>();
	
	/**
	 * This object is used for synchronizaiton of CIDR operations
	 */
	private Object cidrLock = new Object();


	/**
	 * Comma separated CIDR that rest services are allowed to response
	 * Allowed IP addresses to reach REST API, It must be in CIDR format as a.b.c.d/x
	 */
	@Value("${"+SETTINGS_REMOTE_ALLOWED_CIDR+":127.0.0.1}")
    private String remoteAllowedCIDR;

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
	 * Enable/disable hls recording
	 *  If it is set true then HLS files are created into <APP_DIR>/streams and HLS playing is enabled,
	 *  Default value is true
	 */
	@Value( "${"+SETTINGS_HLS_MUXING_ENABLED+":true}" )
	private boolean hlsMuxingEnabled;
	
	/**
	 * Encoder settings in comma separated format
	 * This must be set for adaptive streaming,
	 * If it is empty SFU mode will be active in WebRTCAppEE,
	 * video height, video bitrate, and audio bitrate are set as an example,
	 * Ex. 480,300000,96000,360,200000,64000.
	 */
	@Value( "${"+SETTINGS_ENCODER_SETTINGS_STRING+"}" )
	private String encoderSettingsString;
	
	/**
	 * Number of segments(chunks) in m3u8 files
	 * Set the maximum number of playlist entries, If 0 the list file will contain all the segments,
	 */
	@Value( "${"+SETTINGS_HLS_LIST_SIZE+":#{null}}" )
	private String hlsListSize;
	
	/**
	 * Duration of segments in m3u8 files
	 * Target segment length in seconds,
	 * Segment will be cut on the next key frame after this time has passed.
	 */
	@Value( "${"+SETTINGS_HLS_TIME+":#{null}}" )
	private String hlsTime;
	
	/**
	 * Duration of segments in mpd files,
	 * Segments are a property of DASH. A segment is the minimal download unit.
	 *  
	 */
	@Value( "${"+SETTINGS_DASH_SEG_DURATION+":6}" )
	private String dashSegDuration;
	
	/**
	 * Fragments are a property of fragmented MP4 files, Typically a fragment consists of moof + mdat.
	 *
	 */
	@Value( "${"+SETTINGS_DASH_FRAGMENT_DURATION+":0.5}" )
	private String dashFragmentDuration;
	
	
	/**
	 * Latency of the DASH streaming,
	 */
	@Value( "${"+SETTINGS_DASH_TARGET_LATENCY+":3.5}" )
	private String targetLatency;
	
	/**
	 * DASH window size, Number of files in manifest
	 */
	@Value( "${"+SETTINGS_DASH_WINDOW_SIZE+":5}" )
	private String dashWindowSize;

	/**
	 * DASH extra window size, Number of segments kept outside of the manifest before removing from disk
	 */
	@Value( "${"+SETTINGS_DASH_EXTRA_WINDOW_SIZE+":5}" )
	private String dashExtraWindowSize;
	
	/**
	 * Enable low latency dash, This settings is effective if dash is enabled
	 */
	@Value( "${"+SETTINGS_DASH_ENABLE_LOW_LATENCY+":true}" )
	private boolean lLDashEnabled;
	
	/**
	 * Enable low latency hls via dash muxer, LLHLS is effective if dash is enabled.
	 */
	@Value( "${"+SETTINGS_HLS_ENABLE_LOW_LATENCY+":false}" )
	private boolean lLHLSEnabled;
	
	/**
	 * Enable hls through DASH muxer, LLHLS is effective if dash is enabled.
	 */
	@Value( "${"+SETTINGS_HLS_ENABLED_VIA_DASH_LOW_LATENCY+":false}" )
	private boolean hlsEnabledViaDash;
	
	/**
	 * Use timeline in dash muxing.
	 */
	@Value( "${"+SETTINGS_USE_TIMELINE_DASH_MUXING+":false}" )
	private boolean useTimelineDashMuxing;
	
	/**
	 * Enable/disable webrtc,
	 * It's mandatory, If it is set true then WebRTC playing is enabled, Default value is false
	 */
	@Value( "${"+SETTINGS_WEBRTC_ENABLED+":true}" )
	private boolean webRTCEnabled;
	
	/**
	 * The flag that sets using the original webrtc stream in streaming,
	 * This setting is effective if there is any adaptive bitrate setting,
	 * For instance assume that there is adaptive bitrate with 480p and incoming stream is 720p
	 * Then if this setting is true, there are two bitrates for playing 720p and 480p,
	 * In this case if this setting is false, there is one bitrate for playing that is 480p
	 */
	@Value( "${"+SETTINGS_USE_ORIGINAL_WEBRTC_ENABLED+":false}" )
	private boolean useOriginalWebRTCEnabled;

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
	@Value( "${"+TOKEN_HASH_SECRET+":''}" )
	private String tokenHashSecret;

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
	private String listenerHookURL;

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
	 * It's mandatory,
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
	private int timeTokenPeriod;	
	
	/**
	 * It can be event: or vod, Check HLS documentation for EXT-X-PLAYLIST-TYPE.
	 *
	 */
	@Value( "${"+SETTINGS_HLS_PLAY_LIST_TYPE+":#{null}}" )
	private String hlsPlayListType;

	/**
	 * Facebook client id
	 * This is client id provided by Facebook to broadcast streams to Facebook.
	 */
	@Value( "${"+FACEBOOK_CLIENT_ID+"}" )
	private String facebookClientId;

	/**
	 * Facebook client secret
	 * Secret key for the Facebook client id.
	 */
	@Value( "${"+FACEBOOK_CLIENT_SECRET+"}" )
	private String facebookClientSecret;

	/**
	 * Periscope app client id
	 * This is client id provided by Periscope to broadcast streams to Periscope.
	 */
	@Value( "${"+PERISCOPE_CLIENT_ID+"}" )
	private String  periscopeClientId;

	/**
	 * Periscope app client secret
	 * Secret key for the Periscope client id.
	 */
	@Value( "${"+PERISCOPE_CLIENT_SECRET+"}" )
	private String  periscopeClientSecret;

	/**
	 * Youtube client id
	 * This is client id provided by YouTube to broadcast streams to YouTube.
	 */
	@Value( "${"+YOUTUBE_CLIENT_ID+"}" )
	private String youtubeClientId;

	/**
	 * Youtube client secret for youtube client id
	 */
	@Value( "${"+YOUTUBE_CLIENT_SECRET+"}" )
	private String youtubeClientSecret;

	/**
	 * The path for manually saved used VoDs
	 * Determines the directory to store VOD files.
	 */
	@Value( "${"+SETTINGS_VOD_FOLDER+"}" )
	private String vodFolder;

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

	@Value( "${"+SETTINGS_STALKER_DB_SERVER+"}" )
	private String stalkerDBServer;

	/**
	 * Username of stalker portal DB
	 * Database user name of IP TV Ministra platform.
	 */
	@Value( "${"+SETTINGS_STALKER_DB_USER_NAME+"}" )
	private String stalkerDBUsername;

	/**
	 * Password of the stalker portal DB User
	 * Database password of IP TV Ministra platform.
	 */
	@Value( "${"+SETTINGS_STALKER_DB_PASSWORD+"}" )
	private String stalkerDBPassword;

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
	private int createPreviewPeriod;

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
	@Value( "${"+SETTINGS_STREAM_FETCHER_AUTO_START+":true}" )
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
	private String hlsflags;

	private String mySqlClientPath = "/usr/local/antmedia/mysql";

	/**
	 * This is a script file path that is called by Runtime when muxing is finished,
	 * Bash script file path will be called after stream finishes.
	 */
	@Value( "${"+SETTINGS_MUXER_FINISH_SCRIPT+":}" )
	private String muxerFinishScript;

	/**
	 * It's mandatory,
	 * Determines the frame rate of video publishing to the WebRTC players,
	 * Default value is 20
	 */
	@Value( "${"+SETTINGS_WEBRTC_FRAME_RATE+":20}" )
	private int webRTCFrameRate;
	
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
	 * Stun Server URI
	 * Stun server URI used for WebRTC signaling,
	 * You can check: https://antmedia.io/learn-webrtc-basics-components/,
	 * Default value is stun:stun.l.google.com:19302.
	 */
	@Value( "${" + SETTINGS_WEBRTC_STUN_SERVER_URI +":stun:stun1.l.google.com:19302}")
	private String stunServerURI;

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
	@Value( "${" + SETTINGS_WEBRTC_SDP_SEMANTICS +":" + SDP_SEMANTICS_PLAN_B + "}")
	private String webRTCSdpSemantics;
	
	
	/**
	 * Port Allocator Flags for WebRTC
	 * PORTALLOCATOR_DISABLE_UDP = 0x01,
  	 * PORTALLOCATOR_DISABLE_STUN = 0x02,
  	 * PORTALLOCATOR_DISABLE_RELAY = 0x04,
	 */
	@Value( "${" + SETTINGS_PORT_ALLOCATOR_FLAGS +":0}")
	private int portAllocatorFlags;
	/**
	 * If it's enabled, interactivity(like, comment, etc.) is collected from social media channel,
	 * Default value is false.
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
	private int vp8EncoderSpeed;
	
	/**
	 * VP8 Encoder deadline:
	 *  best
	 * 	good 
	 *  realtime
	 */ 
	@Value( "${" + SETTINGS_ENCODING_VP8_DEADLINE +":realtime}")
	private String vp8EncoderDeadline;
	
	/**
	 * VP8 Encoder thread count.
	 */
	@Value( "${" + SETTINGS_ENCODING_VP8_THREAD_COUNT +":1}")
	private int vp8EncoderThreadCount;
  
	/**
	 * It's mandatory,
	 * Determines the height of preview file,
	 * Default value is 480
	 */

	@Value( "${" + SETTINGS_PREVIEW_HEIGHT +":480}")
	private int previewHeight;
	
	/**
	 * Generate preview if there is any adaptive settings,
	 * 
	 * Preview generation depends on adaptive settings and it's generated by default
	 */
	@Value( "${" + SETTINGS_GENERATE_PREVIEW+":false}")
	private boolean generatePreview;
	
	@Value( "${" + SETTINGS_WRITE_STATS_TO_DATASTORE +":true}")
	private boolean writeStatsToDatastore;
	
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
	@Value( "${" + SETTINGS_ENCODER_SELECTION_PREFERENCE+":'gpu_and_cpu'}")
	private String encoderSelectionPreference;
	
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
	private int excessiveBandwidthValue;

	/**
	 * The excessive bandwidth call threshold value
	 */
	@Value("${" + SETTINGS_EXCESSIVE_BANDWIDTH_CALL_THRESHOLD + ":3}")
	private int excessiveBandwidthCallThreshold;
	
	
	@Value("${" + SETTINGS_EXCESSIVE_BANDWIDTH_TRY_COUNT_BEFORE_SWITCH_BACK + ":4}")
	private int excessiveBandwithTryCountBeforeSwitchback;
	
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
	private int packetLossDiffThresholdForSwitchback;

	/**
	 * rtt measurement threshold diff if rttMeasurement is bigger than this value in ExcessiveBandwidth
	 * algorithm, it switches back to lower quality without try every attempts {@link #setTryCountBeforeSwitchback(int)}
	 * @param rttMeasurementDiffThresholdForSwitchback
	 */
	@Value("${" + SETTINGS_EXCESSIVE_BANDWIDTH_RTT_MEASUREMENT_THRESHOLD_FOR_SWITCH_BACK+ ":20}")
	private int rttMeasurementDiffThresholdForSwitchback;
	
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
	@Value("${" + SETTINGS_DB_APP_NAME +"}")
	private String appName;
	
	/**
	 * Timeout for encoding
	 * If encoder cannot encode a frame in this timeout, streaming is finished by server. 
	 */
	@Value("${" + SETTINGS_ENCODING_TIMEOUT +":5000}")
	private int encodingTimeout;
	
	/**
	 * If webrtc client is not started in this time, it'll close automatically
	 */
	@Value("${" + SETTINGS_WEBRTC_CLIENT_START_TIMEOUT +":5000}")
	private int webRTCClientStartTimeoutMs;
	
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

	private long updateTime;

	private List<EncoderSettings> encoderSettings;

	/**
	 * Forwards the http requests with this extension to {@link #httpForwardingBaseURL}
	 * It supports comma separated extensions Like mp4,m3u8
	 * Don't add any leading, trailing white spaces
	 */
	@Value("${" + SETTINGS_HTTP_FORWARDING_EXTENSION+ ":''}")
	private String httpForwardingExtension;
	
	/**
	 * Forward the incoming http request to this base url
	 */
	@Value("${" + SETTINGS_HTTP_FORWARDING_BASE_URL+ ":''}")
	private String httpForwardingBaseURL;

	/**
	 * Max analyze duration in for determining video and audio existence in RTMP streams
	 */
	@Value("${" + SETTINGS_RTMP_MAX_ANALYZE_DURATION_MS+ ":1500}")
	private int maxAnalyzeDurationMS;
	
	/**
	 * Enable/Disable IPv6 Candidates for WebRTC It's disabled by default
	 */
	@Value("${" + SETTINGS_DISABLE_IPV6_CANDIDATES+ ":true}")
	private boolean disableIPv6Candidates;
	
	/**
	 * Specify the rtsp transport type in pulling IP Camera or RTSP sources
	 * It can be tcp or udp
	 */
	@Value("${" + SETTINGS_RTSP_PULL_TRANSPORT_TYPE+ ":tcp}")
	private String rtspPullTransportType;
	
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

	@JsonIgnore
	@NotSaved
	private List<NetMask> allowedPublisherCIDRList = new ArrayList<>();
	
	
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
	@Value("${" + SETTINGS_DATA_CHANNEL_ENABLED+ ":false}")
	private boolean dataChannelEnabled;
	
	
	/**
	 * Defines the distribution list for player messages
	 * it can be  none/publisher/all
	 * none: player messages are delivered to nobody
	 * publisher: player messages are delivered to only publisher
	 * all:  player messages are delivered to everyone both publisher and all players
	 */
	@Value("${" + SETTINGS_DATA_CHANNEL_PLAYER_DISTRIBUTION+ ":"+DATA_CHANNEL_PLAYER_TO_ALL+"}")
	private String dataChannelPlayerDistribution;

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
	private int heightRtmpForwarding;
  
	/**
	 * In SFU mode we still transcode the audio to opus and aac
	 * This settings determines the audio bitrate for opus and aac
	 * It's the bitrate that is used transcoding the audio in AAC and Opus
	 * After version(2.3), we directly forward incoming audio to the viewers without transcoding.
	 */
	@Value("${" + SETTINGS_AUDIO_BITRATE_SFU+":96000}")
	private int audioBitrateSFU;

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
	private boolean aacEncodingEnabled;
	
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
	private String constantRateFactor;
	
	/**
	 * Application level WebRTC viewer limit
	 */
	@Value( "${"+SETTINGS_WEBRTC_VIEWER_LIMIT+":-1}" )
	private int webRTCViewerLimit = -1;
	
	/*
	 * Set to true when you want to delete an application 
	 */
	private boolean toBeDeleted = false;


	/**
	 * Application JWT secret key
	 */
	@Value( "${"+SETTINGS_JWT_SECRET_KEY+":#{null}}" )
	private String jwtSecretKey;
	
	/**
	 * Application JWT Control Enabled
	 */
	@Value( "${"+SETTINGS_JWT_CONTROL_ENABLED+":false}" )
	private boolean jwtControlEnabled;
	
	/**
	 * Application IP Filter Enabled
	 */
	@Value( "${"+SETTINGS_IP_FILTER_ENABLED+":true}" )
	private boolean ipFilterEnabled;
	
	/**
	 * Application level total incoming stream limit
	 */
	@Value( "${"+SETTINGS_INGESTING_STREAM_LIMIT+":-1}" )
	private int ingestingStreamLimit;
	
	/**
	 * WebRTC Keyframe Time, Ant Media Server asks key frame for every webRTCKeyframeTime in SFU mode,
	 * It's in milliseconds
	 */
	@Value( "${"+SETTINGS_WEBRTC_KEYFRAME_TIME+":2000}" )
	private int webRTCKeyframeTime;
	
	/**
	 * Application JWT stream secret key
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
	private boolean dashHttpStreaming;
	
	
	/**
	 * It's S3 streams MP4, WEBM  and HLS files storage name . 
	 * It's streams by default.
	 * 
	 */
	@Value( "${"+SETTINGS_S3_STREAMS_FOLDER_PATH+":streams}" )
	private String  s3StreamsFolderPath;

	/**
	 * It's S3 stream PNG files storage name . 
	 * It's previews by default.
	 * 
	 */
	@Value( "${"+SETTINGS_S3_PREVIEWS_FOLDER_PATH+":previews}" )
	private String  s3PreviewsFolderPath;
	
	/*
	 * Use http endpoint  in CMAF/HLS. 
	 * It's configurable to send any stream in HTTP Endpoint with this option
	 */
	@Value( "${"+SETTINGS_DASH_HTTP_ENDPOINT+":#{null}}" )
	private String dashHttpEndpoint;
	
	/**
	 * Force stream decoding even if there is no adaptive setting
	 */
	@Value("${" + SETTINGS_FORCE_DECODING+ ":false}")
	private boolean forceDecoding;
	

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
	 * Enable/Disable stream resolution check flag
	 * If it's enabled, Ant Media Server will ignore if the adaptive requested resolution is higher than the incoming stream
	 *  It's false by default
	 */
	@Value( "${"+SETTINGS_ENCODING_RESOLUTION_CHECK+":false}" )
	private boolean encodingResolutionCheck;

	public boolean isWriteStatsToDatastore() {
		return writeStatsToDatastore;
	}

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

	public boolean isHlsMuxingEnabled() {
		return hlsMuxingEnabled;
	}

	public void setHlsMuxingEnabled(boolean hlsMuxingEnabled) {
		this.hlsMuxingEnabled = hlsMuxingEnabled;
	}
	
	public boolean isDashMuxingEnabled() {
		return dashMuxingEnabled;
	}

	public void setDashMuxingEnabled(boolean dashMuxingEnabled) {
		this.dashMuxingEnabled = dashMuxingEnabled;
	}

	public String getHlsPlayListType() {
		return hlsPlayListType;
	}

	public void setHlsPlayListType(String hlsPlayListType) {
		this.hlsPlayListType = hlsPlayListType;
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
		String encoderSettingsString = "";

		for (EncoderSettings encoderSettings : encoderSettingsList) {
			if (encoderSettingsString.length() != 0) {
				encoderSettingsString += ",";
			}
			encoderSettingsString += encoderSettings.getHeight() + "," + encoderSettings.getVideoBitrate() + "," + encoderSettings.getAudioBitrate();
		}
		return encoderSettingsString;
	}

	public static List<EncoderSettings> encodersStr2List(String encoderSettingsString) {
		if(encoderSettingsString == null) {
			return null;
		}

		String[] values = encoderSettingsString.split(",");

		List<EncoderSettings> encoderSettingsList = new ArrayList<>();
		if (values.length >= 3){
			for (int i = 0; i < values.length; i++) {
				int height = Integer.parseInt(values[i]);
				i++;
				int videoBitrate = Integer.parseInt(values[i]);
				i++;
				int audioBitrate = Integer.parseInt(values[i]);
				encoderSettingsList.add(new EncoderSettings(height, videoBitrate, audioBitrate));
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
		this.encoderSettings = settings;
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

	public String getYoutubeClientSecret() {
		return youtubeClientSecret;
	}

	public void setYoutubeClientSecret(String youtubeClientSecret) {
		this.youtubeClientSecret = youtubeClientSecret;
	}

	public String getYoutubeClientId() {
		return youtubeClientId;
	}

	public void setYoutubeClientId(String youtubeClientId) {
		this.youtubeClientId = youtubeClientId;
	}

	public String getPeriscopeClientSecret() {
		return periscopeClientSecret;
	}

	public void setPeriscopeClientSecret(String periscopeClientSecret) {
		this.periscopeClientSecret = periscopeClientSecret;
	}

	public String getPeriscopeClientId() {
		return periscopeClientId;
	}

	public void setPeriscopeClientId(String periscopeClientId) {
		this.periscopeClientId = periscopeClientId;
	}

	public String getFacebookClientSecret() {
		return facebookClientSecret;
	}

	public void setFacebookClientSecret(String facebookClientSecret) {
		this.facebookClientSecret = facebookClientSecret;
	}

	public String getFacebookClientId() {
		return facebookClientId;
	}

	public void setFacebookClientId(String facebookClientId) {
		this.facebookClientId = facebookClientId;
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

	public String getHlsFlags() {
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

	public String getRemoteAllowedCIDR() {
		synchronized (cidrLock) 
		{
			return remoteAllowedCIDR;
		}	
	}
	
	/**
	 * the getAllowedCIDRList and setAllowedCIDRList are synchronized
	 * because ArrayList may throw concurrent modification
	 * @param remoteAllowedCIDR
	 */
	public void setRemoteAllowedCIDR(String remoteAllowedCIDR) {
		synchronized(cidrLock) {
			this.remoteAllowedCIDR = remoteAllowedCIDR;
			allowedCIDRList = new ArrayList<>();
			fillFromInput(remoteAllowedCIDR, allowedCIDRList);
		}
	}

	public List<NetMask> getAllowedCIDRList() {
		synchronized(cidrLock) {
			if (allowedCIDRList.isEmpty()) {
				fillFromInput(remoteAllowedCIDR, allowedCIDRList);
			}
			return allowedCIDRList;
		}
	}
	
	public String getAllowedPublisherCIDR() {
		return allowedPublisherCIDR;
	}

	public void setAllowedPublisherCIDR(String allowedPublisherCIDR) 
	{
		synchronized (cidrLock) 
		{
			this.allowedPublisherCIDR = allowedPublisherCIDR;
			allowedPublisherCIDRList = new ArrayList<>();
			fillFromInput(allowedPublisherCIDR, allowedPublisherCIDRList);
		}
	}
	
	public List<NetMask> getAllowedPublisherCIDRList() 
	{
		synchronized (cidrLock) 
		{
			if (allowedPublisherCIDRList.isEmpty()) {
				fillFromInput(allowedPublisherCIDR, allowedPublisherCIDRList);
			}
		}
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
	private List<String> fillFromInput(final String input, final List<NetMask> target) {
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
	
	public String getJwksURL() {
		return jwksURL;
	}

	public void setJwksURL(String jwksURL) {
		this.jwksURL = jwksURL;
	}
	
	public boolean isEncodingResolutionCheck() {
		return encodingResolutionCheck;
	}

	public void setEncodingResolutionCheck(boolean encodingResolutionCheck) {
		this.encodingResolutionCheck = encodingResolutionCheck;
	}

}
