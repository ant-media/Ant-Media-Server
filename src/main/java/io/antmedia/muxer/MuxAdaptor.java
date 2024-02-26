package io.antmedia.muxer;

import static io.antmedia.muxer.IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING;
import static org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_AAC;
import static org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_H264;
import static org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_PNG;
import static org.bytedeco.ffmpeg.global.avcodec.AV_PKT_FLAG_KEY;
import static org.bytedeco.ffmpeg.global.avutil.AVMEDIA_TYPE_ATTACHMENT;
import static org.bytedeco.ffmpeg.global.avutil.AVMEDIA_TYPE_AUDIO;
import static org.bytedeco.ffmpeg.global.avutil.AVMEDIA_TYPE_DATA;
import static org.bytedeco.ffmpeg.global.avutil.AVMEDIA_TYPE_SUBTITLE;
import static org.bytedeco.ffmpeg.global.avutil.AVMEDIA_TYPE_VIDEO;
import static org.bytedeco.ffmpeg.global.avutil.AV_PIX_FMT_YUV420P;
import static org.bytedeco.ffmpeg.global.avutil.AV_SAMPLE_FMT_FLTP;
import static org.bytedeco.ffmpeg.global.avutil.av_channel_layout_default;
import static org.bytedeco.ffmpeg.global.avutil.av_free;
import static org.bytedeco.ffmpeg.global.avutil.av_malloc;
import static org.bytedeco.ffmpeg.global.avutil.av_rescale_q;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.mina.core.buffer.IoBuffer;
import org.bytedeco.ffmpeg.avcodec.AVCodecContext;
import org.bytedeco.ffmpeg.avcodec.AVCodecParameters;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.avformat.AVStream;
import org.bytedeco.ffmpeg.avutil.AVChannelLayout;
import org.bytedeco.ffmpeg.avutil.AVRational;
import org.bytedeco.javacpp.BytePointer;
import org.red5.codec.AVCVideo;
import org.red5.codec.IAudioStreamCodec;
import org.red5.codec.IStreamCodecInfo;
import org.red5.codec.IVideoStreamCodec;
import org.red5.server.api.IConnection;
import org.red5.server.api.IContext;
import org.red5.server.api.scope.IScope;
import org.red5.server.api.stream.IBroadcastStream;
import org.red5.server.api.stream.IStreamCapableConnection;
import org.red5.server.api.stream.IStreamPacket;
import org.red5.server.net.rtmp.event.CachedEvent;
import org.red5.server.net.rtmp.message.Constants;
import org.red5.server.stream.ClientBroadcastStream;
import org.red5.server.stream.IRecordingListener;
import org.red5.server.stream.consumer.FileConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.AppSettings;
import io.antmedia.EncoderSettings;
import io.antmedia.RecordType;
import io.antmedia.datastore.db.DataStore;
import io.antmedia.datastore.db.IDataStoreFactory;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.datastore.db.types.Endpoint;
import io.antmedia.muxer.parser.AACConfigParser;
import io.antmedia.muxer.parser.AACConfigParser.AudioObjectTypes;
import io.antmedia.muxer.parser.SpsParser;
import io.antmedia.muxer.parser.codec.AACAudio;
import io.antmedia.plugin.PacketFeeder;
import io.antmedia.plugin.api.IPacketListener;
import io.antmedia.plugin.api.StreamParametersInfo;
import io.antmedia.rest.model.Result;
import io.antmedia.settings.ServerSettings;
import io.antmedia.storage.StorageClient;
import io.vertx.core.Vertx;


public class MuxAdaptor implements IRecordingListener, IEndpointStatusListener {


	public static final int STAT_UPDATE_PERIOD_MS = 10000;


	public static final String ADAPTIVE_SUFFIX = "_adaptive";
	private static Logger logger = LoggerFactory.getLogger(MuxAdaptor.class);

	protected ConcurrentLinkedQueue<IStreamPacket> streamPacketQueue = new ConcurrentLinkedQueue<>();
	protected AtomicBoolean isPipeReaderJobRunning = new AtomicBoolean(false);
	private   AtomicBoolean isBufferedWriterRunning = new AtomicBoolean(false);

	protected List<Muxer> muxerList =  Collections.synchronizedList(new ArrayList<Muxer>());
	protected boolean deleteHLSFilesOnExit = true;
	protected boolean deleteDASHFilesOnExit = true;


	private int videoStreamIndex;
	protected int audioStreamIndex;

	protected boolean previewOverwrite = false;

	protected volatile boolean enableVideo = false;
	protected volatile boolean enableAudio = false;

	boolean firstAudioPacketSkipped = false;
	boolean firstVideoPacketSkipped = false;

	private long packetPollerId = -1;

	private ConcurrentSkipListSet<IStreamPacket> bufferQueue = new ConcurrentSkipListSet<>((a, b) -> Long.compare(a.getTimestamp(), b.getTimestamp()));


	private volatile boolean stopRequestExist = false;

	public static final int RECORDING_ENABLED_FOR_STREAM = 1;
	public static final int RECORDING_DISABLED_FOR_STREAM = -1;
	public static final int RECORDING_NO_SET_FOR_STREAM = 0;
	protected static final long WAIT_TIME_MILLISECONDS = 5;
	protected AtomicBoolean isRecording = new AtomicBoolean(false);
	protected ClientBroadcastStream broadcastStream;
	protected boolean mp4MuxingEnabled;
	protected boolean webMMuxingEnabled;
	protected boolean addDateTimeToMp4FileName;
	protected boolean hlsMuxingEnabled;
	protected boolean dashMuxingEnabled;
	protected boolean objectDetectionEnabled;

	protected ConcurrentHashMap<String, Boolean> isHealthCheckStartedMap = new ConcurrentHashMap<>();
	protected ConcurrentHashMap<String, Integer> errorCountMap = new ConcurrentHashMap<>();
	protected ConcurrentHashMap<String, Integer> retryCounter = new ConcurrentHashMap<>();
	protected ConcurrentHashMap<String, String> statusMap = new ConcurrentHashMap<>();
	protected int rtmpEndpointRetryLimit;
	protected int healthCheckPeriodMS;

	protected boolean webRTCEnabled = false;
	protected StorageClient storageClient;
	protected String hlsTime;
	protected String hlsListSize;
	protected String hlsPlayListType;
	protected String dashSegDuration;
	protected String dashFragmentDuration;
	protected String targetLatency;
	List<EncoderSettings> adaptiveResolutionList = null;

	protected DataStore dataStore;

	/**
	 * By default first video key frame should be checked
	 * and below flag should be set to true
	 * If first video key frame should not be checked,
	 * then below should be flag in advance
	 */
	private boolean firstKeyFrameReceivedChecked = false;
	protected String streamId;
	protected long startTime;

	protected IScope scope;

	private String oldQuality;
	public static final  AVRational TIME_BASE_FOR_MS;
	private IAntMediaStreamHandler appAdapter;

	protected List<EncoderSettings> encoderSettingsList;
	protected static boolean isStreamSource = false;

	private int previewCreatePeriod;
	private double oldspeed;
	private long lastQualityUpdateTime = 0;
	private Broadcast broadcast;
	protected AppSettings appSettings;
	private int previewHeight;
	private int lastFrameTimestamp;
	private int maxAnalyzeDurationMS = 1000;
	protected boolean generatePreview = true;
	private int firstReceivedFrameTimestamp = -1;
	protected int totalIngestedVideoPacketCount = 0;
	private long bufferTimeMs = 0;

	protected ServerSettings serverSettings;

	/**
	 * Packet times in ordered way to calculate streaming health
	 * Key is the packet ime
	 * Value is the system time at that moment
	 *
	 */
	private LinkedList<PacketTime> packetTimeList = new LinkedList<PacketTime>();

	public boolean addID3Data(String data) {
		for (Muxer muxer : muxerList) {
			if(muxer instanceof HLSMuxer) {
				((HLSMuxer)muxer).addID3Data(data);
				return true;
			}
		}
		return false;
	}

	public static class PacketTime {
		public final long packetTimeMs;
		public final long systemTimeMs;
		public PacketTime(long packetTimeMs, long systemTimeMs) {
			this.packetTimeMs = packetTimeMs;
			this.systemTimeMs = systemTimeMs;
		}
	}

	protected Vertx vertx;

	/**
	 * Accessed from multiple threads so make it volatile
	 */
	private AtomicBoolean buffering = new AtomicBoolean(false);
	private int bufferLogCounter;

	/**
	 * The time when buffering has been finished. It's volatile because it's accessed from multiple threads
	 */
	private volatile long bufferingFinishTimeMs = 0;

	/**
	 * Mux adaptor is generally used in RTMP.
	 * However it can be also used to stream RTSP Pull so that isAVC can be false
	 */
	private boolean avc = true;

	private long bufferedPacketWriterId = -1;
	private volatile long lastPacketTimeMsInQueue = 0;
	private volatile long firstPacketReadyToSentTimeMs = 0;
	protected String dataChannelWebHookURL = null;
	protected long absoluteTotalIngestTime = 0;
	/**
	 * It's defined here because EncoderAdaptor should access it directly to add new streams.
	 * Don't prefer to access to dashMuxer directly. Access it with getter
	 */
	protected Muxer dashMuxer = null;

	private long checkStreamsStartTime = -1;
	private byte[] videoDataConf;
	private byte[] audioDataConf;
	private AtomicInteger queueSize = new AtomicInteger(0);
	private long startTimeMs;
	protected long totalIngestTime;
	private int fps = 0;
	protected int width;
	protected int height;
	protected AVFormatContext streamSourceInputFormatContext;
	private AVCodecParameters videoCodecParameters;
	protected AVCodecParameters audioCodecParameters;
	private BytePointer audioExtraDataPointer;
	private BytePointer videoExtraDataPointer;
	private AtomicLong endpointStatusUpdaterTimer = new AtomicLong(-1l);
	private ConcurrentHashMap<String, String> endpointStatusUpdateMap = new ConcurrentHashMap<>();	

	protected PacketFeeder packetFeeder;

	private static final int COUNT_TO_LOG_BUFFER = 500;

	static {
		TIME_BASE_FOR_MS = new AVRational();
		TIME_BASE_FOR_MS.num(1);
		TIME_BASE_FOR_MS.den(1000);
	}

	private AVRational videoTimeBase = TIME_BASE_FOR_MS;
	private AVRational audioTimeBase = TIME_BASE_FOR_MS;

	//NOSONAR because we need to keep the reference of the field
	protected AVChannelLayout channelLayout;

	public static MuxAdaptor initializeMuxAdaptor(ClientBroadcastStream clientBroadcastStream, boolean isSource, IScope scope) {
		MuxAdaptor muxAdaptor = null;
		ApplicationContext applicationContext = scope.getContext().getApplicationContext();
		boolean tryEncoderAdaptor = false;
		if (applicationContext.containsBean(AppSettings.BEAN_NAME)) {
			AppSettings appSettings = (AppSettings) applicationContext.getBean(AppSettings.BEAN_NAME);
			List<EncoderSettings> list = appSettings.getEncoderSettings();
			if ((list != null && !list.isEmpty()) || appSettings.isWebRTCEnabled() || appSettings.isForceDecoding()) {
				/*
				 * enable encoder adaptor if webrtc enabled because we're supporting forwarding video to end user
				 * without transcoding. We need encoder adaptor because we need to transcode audio
				 */
				tryEncoderAdaptor = true;
			}
		}

		if (tryEncoderAdaptor) {
			//if adaptive bitrate enabled, take a look at encoder adaptor exists
			//if it is not enabled, then initialize only mux adaptor
			try {
				Class transraterClass = Class.forName("io.antmedia.enterprise.adaptive.EncoderAdaptor");

				muxAdaptor = (MuxAdaptor) transraterClass.getConstructor(ClientBroadcastStream.class)
						.newInstance(clientBroadcastStream);

			} catch (Exception e) {
				logger.error(e.getMessage());
			}
		}
		if (muxAdaptor == null) {
			muxAdaptor = new MuxAdaptor(clientBroadcastStream);
		}
		muxAdaptor.setStreamSource(isSource);

		return muxAdaptor;
	}


	protected MuxAdaptor(ClientBroadcastStream clientBroadcastStream) {

		this.broadcastStream = clientBroadcastStream;
	}

	public boolean addMuxer(Muxer muxer) {
		return addMuxer(muxer, 0);
	}

	public boolean addMuxer(Muxer muxer, int resolutionHeight)
	{
		boolean result = false;
		if (directMuxingSupported() && (resolutionHeight == 0 || resolutionHeight == height)) 
		{	
			if (isRecording.get()) 
			{
				result = prepareMuxer(muxer, resolutionHeight);
			}
			else 
			{
				result = addMuxerInternal(muxer);
			}
		}
		return result;
	}

	public boolean removeMuxer(Muxer muxer) 
	{
		boolean result = false;
		if (muxerList.remove(muxer)) 
		{
			muxer.writeTrailer();
			result = true;
		}
		return result;
	}

	protected boolean addMuxerInternal(Muxer muxer) 
	{
		boolean result = false;
		if (!muxerList.contains(muxer)) 
		{
			result = muxerList.add(muxer);
		}
		return result;
	}


	@Override
	public boolean init(IConnection conn, String name, boolean isAppend) {

		return init(conn.getScope(), name, isAppend);
	}

	protected void enableSettings() {
		AppSettings appSettingsLocal = getAppSettings();
		hlsMuxingEnabled = appSettingsLocal.isHlsMuxingEnabled();
		dashMuxingEnabled = appSettingsLocal.isDashMuxingEnabled();
		mp4MuxingEnabled = appSettingsLocal.isMp4MuxingEnabled();
		webMMuxingEnabled = appSettingsLocal.isWebMMuxingEnabled();
		objectDetectionEnabled = appSettingsLocal.isObjectDetectionEnabled();

		addDateTimeToMp4FileName = appSettingsLocal.isAddDateTimeToMp4FileName();
		webRTCEnabled = appSettingsLocal.isWebRTCEnabled();
		deleteHLSFilesOnExit = appSettingsLocal.isDeleteHLSFilesOnEnded();
		deleteDASHFilesOnExit = appSettingsLocal.isDeleteDASHFilesOnEnded();
		hlsListSize = appSettingsLocal.getHlsListSize();
		hlsTime = appSettingsLocal.getHlsTime();
		hlsPlayListType = appSettingsLocal.getHlsPlayListType();
		dashSegDuration = appSettingsLocal.getDashSegDuration();
		dashFragmentDuration = appSettingsLocal.getDashFragmentDuration();
		targetLatency = appSettingsLocal.getTargetLatency();

		previewOverwrite = appSettingsLocal.isPreviewOverwrite();
		encoderSettingsList = appSettingsLocal.getEncoderSettings();
		previewCreatePeriod = appSettingsLocal.getCreatePreviewPeriod();
		maxAnalyzeDurationMS = appSettingsLocal.getMaxAnalyzeDurationMS();
		generatePreview = appSettingsLocal.isGeneratePreview();
		previewHeight = appSettingsLocal.getPreviewHeight();
		bufferTimeMs = appSettingsLocal.getRtmpIngestBufferTimeMs();
		dataChannelWebHookURL = appSettingsLocal.getDataChannelWebHookURL();

		rtmpEndpointRetryLimit = appSettingsLocal.getEndpointRepublishLimit();
		healthCheckPeriodMS = appSettingsLocal.getEndpointHealthCheckPeriodMs();
	}

	public void initStorageClient() {
		if (scope.getContext().getApplicationContext().containsBean(StorageClient.BEAN_NAME)) {
			storageClient = (StorageClient) scope.getContext().getApplicationContext().getBean(StorageClient.BEAN_NAME);
		}
	}

	@Override
	public boolean init(IScope scope, String streamId, boolean isAppend) {

		this.streamId = streamId;
		this.scope = scope;
		packetFeeder = new PacketFeeder(streamId);

		getDataStore();

		//TODO: Refactor -> saving broadcast is called two times in RTMP ingesting. It should be one time
		getStreamHandler().updateBroadcastStatus(streamId, startTimeMs, IAntMediaStreamHandler.PUBLISH_TYPE_RTMP, getDataStore().get(streamId));

		enableSettings();
		initServerSettings();
		initStorageClient();
		enableMp4Setting();
		enableWebMSetting();
		initVertx();

		if (mp4MuxingEnabled) {
			addMp4Muxer();
			logger.info("adding MP4 Muxer, add datetime to file name {}", addDateTimeToMp4FileName);
		}

		if (hlsMuxingEnabled) {

			HLSMuxer hlsMuxer = new HLSMuxer(vertx, storageClient, getAppSettings().getS3StreamsFolderPath(), getAppSettings().getUploadExtensionsToS3(), getAppSettings().getHlsHttpEndpoint(), getAppSettings().isAddDateTimeToHlsFileName());
			hlsMuxer.setHlsParameters( hlsListSize, hlsTime, hlsPlayListType, getAppSettings().getHlsflags(), getAppSettings().getHlsEncryptionKeyInfoFile());
			hlsMuxer.setDeleteFileOnExit(deleteHLSFilesOnExit);
			hlsMuxer.setId3Enabled(appSettings.isId3TagEnabled());
			addMuxer(hlsMuxer);
			logger.info("adding HLS Muxer for {}", streamId);
		}

		getDashMuxer();
		if (dashMuxer != null) {
			addMuxer(dashMuxer);
		}

		for (Muxer muxer : muxerList) {
			muxer.init(scope, streamId, 0, broadcast.getSubFolder(), 0);
		}
		getStreamHandler().muxAdaptorAdded(this);
		return true;
	}

	public Muxer getDashMuxer()
	{
		if (dashMuxingEnabled && dashMuxer == null) {
			try {
				Class dashMuxerClass = Class.forName("io.antmedia.enterprise.muxer.DASHMuxer");

				logger.info("adding DASH Muxer for {}", streamId);

				dashMuxer = (Muxer) dashMuxerClass.getConstructors()[0].newInstance(vertx, dashFragmentDuration, dashSegDuration, targetLatency, deleteDASHFilesOnExit, !appSettings.getEncoderSettings().isEmpty(),
						appSettings.getDashWindowSize(), appSettings.getDashExtraWindowSize(), appSettings.islLDashEnabled(), appSettings.islLHLSEnabled(),
						appSettings.isHlsEnabledViaDash(), appSettings.isUseTimelineDashMuxing(), appSettings.isDashHttpStreaming(),appSettings.getDashHttpEndpoint(), serverSettings.getDefaultHttpPort());



			}
			catch (ClassNotFoundException e) {
				logger.info("DashMuxer class not found for stream:{}", streamId);
			}
			catch (Exception e) {
				logger.error(ExceptionUtils.getStackTrace(e));
			}

		}
		return dashMuxer;
	}

	private void initVertx() {
		if (scope.getContext().getApplicationContext().containsBean(IAntMediaStreamHandler.VERTX_BEAN_NAME))
		{
			vertx = (Vertx)scope.getContext().getApplicationContext().getBean(IAntMediaStreamHandler.VERTX_BEAN_NAME);
			logger.info("vertx exist {}", vertx);
		}
		else {
			logger.info("No vertx bean for stream {}", streamId);
		}
	}

	protected void initServerSettings() {
		if(scope.getContext().getApplicationContext().containsBean(ServerSettings.BEAN_NAME)) {
			serverSettings = (ServerSettings)scope.getContext().getApplicationContext().getBean(ServerSettings.BEAN_NAME);
			logger.info("serverSettings exist {}", serverSettings);
		}
		else {
			logger.info("No serverSettings bean for stream {}", streamId);
		}
	}

	protected void enableMp4Setting() {
		broadcast = getBroadcast();

		if (broadcast.getMp4Enabled() == RECORDING_DISABLED_FOR_STREAM)
		{
			// if stream specific mp4 setting is disabled
			mp4MuxingEnabled = false;
		}
		else if (broadcast.getMp4Enabled() == RECORDING_ENABLED_FOR_STREAM)
		{
			// if stream specific mp4 setting is enabled
			mp4MuxingEnabled = true;
		}

	}

	protected void enableWebMSetting() {
		broadcast = getBroadcast();

		if (broadcast.getWebMEnabled() == RECORDING_DISABLED_FOR_STREAM)
		{
			// if stream specific WebM setting is disabled
			webMMuxingEnabled = false;
		}
		else if (broadcast.getWebMEnabled() == RECORDING_ENABLED_FOR_STREAM)
		{
			// if stream specific WebM setting is enabled
			webMMuxingEnabled = true;
		}

	}

	public static void setUpEndPoints(MuxAdaptor muxAdaptor, Broadcast broadcast, Vertx vertx) 
	{
		if (broadcast != null) {
			List<Endpoint> endPointList = broadcast.getEndPointList();

			if (endPointList != null && !endPointList.isEmpty()) 
			{
				for (Endpoint endpoint : endPointList) {
					RtmpMuxer rtmpMuxer = new RtmpMuxer(endpoint.getRtmpUrl(), vertx);
					rtmpMuxer.setStatusListener(muxAdaptor);
					muxAdaptor.addMuxer(rtmpMuxer);
				}
			}
		}

	}


	public AVCodecParameters getAudioCodecParameters() {

		if (audioDataConf != null && audioCodecParameters == null) 
		{
			AACConfigParser aacParser = new AACConfigParser(audioDataConf, 0);

			if (!aacParser.isErrorOccured()) 
			{
				audioCodecParameters = new AVCodecParameters();
				audioCodecParameters.sample_rate(aacParser.getSampleRate());

				channelLayout = new AVChannelLayout();
				av_channel_layout_default(channelLayout, aacParser.getChannelCount());

				audioCodecParameters.ch_layout(channelLayout);

				audioCodecParameters.codec_id(AV_CODEC_ID_AAC);
				audioCodecParameters.codec_type(AVMEDIA_TYPE_AUDIO);

				if (aacParser.getObjectType() == AudioObjectTypes.AAC_LC) {

					audioCodecParameters.profile(AVCodecContext.FF_PROFILE_AAC_LOW);
				}
				else if (aacParser.getObjectType() == AudioObjectTypes.AAC_LTP) {

					audioCodecParameters.profile(AVCodecContext.FF_PROFILE_AAC_LTP);
				}
				else if (aacParser.getObjectType() == AudioObjectTypes.AAC_MAIN) {

					audioCodecParameters.profile(AVCodecContext.FF_PROFILE_AAC_MAIN);
				}
				else if (aacParser.getObjectType() == AudioObjectTypes.AAC_SSR) {

					audioCodecParameters.profile(AVCodecContext.FF_PROFILE_AAC_SSR);
				}

				audioCodecParameters.frame_size(aacParser.getFrameSize());
				audioCodecParameters.format(AV_SAMPLE_FMT_FLTP);
				audioExtraDataPointer = new BytePointer(av_malloc(audioDataConf.length)).capacity(audioDataConf.length);
				audioExtraDataPointer.position(0).put(audioDataConf);
				audioCodecParameters.extradata(audioExtraDataPointer);
				audioCodecParameters.extradata_size(audioDataConf.length);
				audioCodecParameters.codec_tag(0);
			}
			else {
				logger.warn("Cannot parse AAC header succesfully for stream:{}", streamId);
			}
		}
		return audioCodecParameters;
	}


	public AVCodecParameters getVideoCodecParameters() 
	{
		if (videoDataConf != null && videoCodecParameters == null) {
			SpsParser spsParser = new SpsParser(getAnnexbExtradata(videoDataConf), 5);

			videoCodecParameters = new AVCodecParameters();
			width = spsParser.getWidth();
			height = spsParser.getHeight();
			videoCodecParameters.width(spsParser.getWidth());
			videoCodecParameters.height(spsParser.getHeight());
			videoCodecParameters.codec_id(AV_CODEC_ID_H264);
			videoCodecParameters.codec_type(AVMEDIA_TYPE_VIDEO);


			videoExtraDataPointer = new BytePointer(av_malloc(videoDataConf.length)).capacity(videoDataConf.length); 
			videoExtraDataPointer.position(0).put(videoDataConf);
			videoCodecParameters.extradata_size(videoDataConf.length);
			videoCodecParameters.extradata(videoExtraDataPointer);

			videoCodecParameters.format(AV_PIX_FMT_YUV420P);
			videoCodecParameters.codec_tag(0);
		}
		return videoCodecParameters;
	}


	/**
	 * Prepares the parameters. This method is called in RTMP ingesting
	 * @return
	 * @throws Exception
	 */
	public boolean prepare() throws Exception {

		int streamIndex = 0;
		AVCodecParameters codecParameters = getVideoCodecParameters();
		if (codecParameters != null) {
			logger.info("Incoming video width: {} height:{} stream:{}", codecParameters.width(), codecParameters.height(), streamId);
			addStream2Muxers(codecParameters, TIME_BASE_FOR_MS, streamIndex);
			videoStreamIndex = streamIndex;
			streamIndex++;
		}


		AVCodecParameters parameters = getAudioCodecParameters();
		if (parameters != null) {
			addStream2Muxers(parameters, TIME_BASE_FOR_MS, streamIndex);
			audioStreamIndex = streamIndex;
		}
		else {
			logger.info("There is no audio in the stream or not received AAC Sequence header for stream:{} muting the audio", streamId);
			enableAudio = false;
		}

		prepareMuxerIO();

		registerToMainTrackIfExists();
		return true;
	}


	public void registerToMainTrackIfExists() {
		if(broadcastStream.getParameters() != null) {
			String mainTrack = broadcastStream.getParameters().get("mainTrack");
			if(mainTrack != null) {
				Broadcast broadcastLocal = getBroadcast();
				broadcastLocal.setMainTrackStreamId(mainTrack);
				getDataStore().updateBroadcastFields(streamId, broadcastLocal);

				Broadcast mainBroadcast = getDataStore().get(mainTrack);
				if(mainBroadcast == null) {
					mainBroadcast = new Broadcast();
					try {
						mainBroadcast.setStreamId(mainTrack);
					} catch (Exception e) {
						logger.error(ExceptionUtils.getStackTrace(e));
					}
					mainBroadcast.setZombi(true);
					mainBroadcast.setStatus(BROADCAST_STATUS_BROADCASTING);
					mainBroadcast.getSubTrackStreamIds().add(streamId);
					getDataStore().save(mainBroadcast);
				}
				else {
					mainBroadcast.getSubTrackStreamIds().add(streamId);
					getDataStore().updateBroadcastFields(mainTrack, mainBroadcast);
				}
			}
		}
	}


	/**
	 * Prepares parameters and muxers. This method is called when pulling stream source
	 * @param inputFormatContext
	 * @return
	 * @throws Exception
	 */
	public boolean prepareFromInputFormatContext(AVFormatContext inputFormatContext) throws Exception {

		this.streamSourceInputFormatContext = inputFormatContext;
		// Dump information about file onto standard error

		int streamIndex = 0;
		int streamCount = inputFormatContext.nb_streams();
		for (int i=0; i < streamCount; i++)
		{
			AVStream stream = inputFormatContext.streams(i);
			AVCodecParameters codecpar = stream.codecpar();
			if (codecpar.codec_type() == AVMEDIA_TYPE_VIDEO && !isBlacklistCodec(codecpar.codec_id())) {

				videoTimeBase = inputFormatContext.streams(i).time_base();
				logger.info("Video format codec Id: {} width:{} height:{} for stream: {} source index:{} target index:{}", codecpar.codec_id(), codecpar.width(), codecpar.height(), streamId, i, streamIndex);
				width = codecpar.width();
				height = codecpar.height();

				addStream2Muxers(codecpar, stream.time_base(), i);
				videoStreamIndex = streamIndex;
				videoCodecParameters = codecpar;
				streamIndex++;

			}
			else if (codecpar.codec_type() == AVMEDIA_TYPE_AUDIO) 
			{
				logger.info("Audio format sample rate:{} bitrate:{} for stream: {} source index:{} target index:{}",codecpar.sample_rate(), codecpar.bit_rate(), streamId, i, streamIndex);
				audioTimeBase = inputFormatContext.streams(i).time_base();
				addStream2Muxers(codecpar, stream.time_base(), i);
				audioStreamIndex = streamIndex;
				audioCodecParameters = codecpar;
				streamIndex++;
			}
		}

		if (enableVideo && (width == 0 || height == 0)) {
			logger.info("Width or height is zero so returning for stream: {}", streamId);
			return false;
		}

		isRecording.set(true); 

		prepareMuxerIO();
		return true;
	}


	public static byte[] getAnnexbExtradata(byte[] avcExtradata){
		IoBuffer buffer = IoBuffer.wrap(avcExtradata);

		buffer.skip(6); //skip first 6 bytes for avc
		short spsSize = buffer.getShort();
		byte[] sps = new byte[spsSize];

		buffer.get(sps);

		buffer.skip(1); //skip one byte for pps number

		short ppsSize = buffer.getShort();


		byte[] pps = new byte[ppsSize];
		buffer.get(pps);

		byte[] extradataAnnexb = new byte[8 + spsSize + ppsSize];
		extradataAnnexb[0] = 0x00;
		extradataAnnexb[1] = 0x00;
		extradataAnnexb[2] = 0x00;
		extradataAnnexb[3] = 0x01;

		System.arraycopy(sps, 0, extradataAnnexb, 4, spsSize);

		extradataAnnexb[4 + spsSize] = 0x00;
		extradataAnnexb[5 + spsSize] = 0x00;
		extradataAnnexb[6 + spsSize] = 0x00;
		extradataAnnexb[7 + spsSize] = 0x01;

		System.arraycopy(pps, 0, extradataAnnexb, 8 + spsSize, ppsSize);
		return extradataAnnexb;
	}


	public static String getStreamType(int codecType) 
	{
		String streamType = "not_known";

		if (codecType == AVMEDIA_TYPE_VIDEO) 
		{
			streamType = "video";
		}
		else if (codecType == AVMEDIA_TYPE_AUDIO) 
		{
			streamType = "audio";
		}
		else if (codecType == AVMEDIA_TYPE_DATA) 
		{
			streamType = "data";
		}
		else if (codecType == AVMEDIA_TYPE_SUBTITLE) 
		{
			streamType = "subtitle";
		}
		else if (codecType == AVMEDIA_TYPE_ATTACHMENT) 
		{
			streamType = "attachment";
		}

		return streamType;
	}

	public void addStream2Muxers(AVCodecParameters codecParameters, AVRational rat, int streamIndex) 
	{
		synchronized (muxerList) {

			Iterator<Muxer> iterator = muxerList.iterator();
			while (iterator.hasNext())
			{
				Muxer muxer = iterator.next();

				if (!muxer.addStream(codecParameters, rat, streamIndex)) 
				{

					logger.warn("addStream returns false {} for stream: {} for {} stream", muxer.getFormat(), streamId, getStreamType(codecParameters.codec_type()));
				}
			}
		}

		startTime = System.currentTimeMillis();
	}

	public void prepareMuxerIO() 
	{
		synchronized (muxerList) {

			Iterator<Muxer> iterator = muxerList.iterator();
			while (iterator.hasNext())
			{
				Muxer muxer = iterator.next();
				if (!muxer.prepareIO())
				{
					iterator.remove();
					logger.error("prepareIO returns false {} for stream: {}", muxer.getFormat(), streamId);
				}
			}
		}
		startTime = System.currentTimeMillis();

	}

	/**
	 * @param streamId        id of the stream
	 * @param quality,        quality string
	 * @param packetTime,     time of the packet in milliseconds
	 * @param duration,       the total elapsed time in milliseconds
	 * @param inputQueueSize, input queue size of the packets that is waiting to be processed
	 */
	public void updateStreamQualityParameters(String streamId, String quality, double speed, int inputQueueSize) {
		long now = System.currentTimeMillis();

		//increase updating time to STAT_UPDATE_PERIOD_MS seconds because it may cause some issues in mongodb updates 
		//or 
		//update before STAT_UPDATE_PERIOD_MS if speed something meaningful
		if ((now - lastQualityUpdateTime) > STAT_UPDATE_PERIOD_MS || (lastQualityUpdateTime == 0 && speed > 0.8)) 
		{

			logger.info("Stream queue size:{} speed:{} for streamId:{} ", inputQueueSize, speed, streamId);
			lastQualityUpdateTime = now;

			getStreamHandler().setQualityParameters(streamId, quality, speed, inputQueueSize, System.currentTimeMillis());
			oldQuality = quality;
			oldspeed = speed;
		}
	}

	public IAntMediaStreamHandler getStreamHandler() {
		if (appAdapter == null) {
			IContext context = MuxAdaptor.this.scope.getContext();
			ApplicationContext appCtx = context.getApplicationContext();
			//this returns the StreamApplication instance
			appAdapter = (IAntMediaStreamHandler) appCtx.getBean(AntMediaApplicationAdapter.BEAN_NAME);
		}
		return appAdapter;
	}



	public AppSettings getAppSettings() {

		if (appSettings == null && scope.getContext().getApplicationContext().containsBean(AppSettings.BEAN_NAME)) {
			appSettings = (AppSettings) scope.getContext().getApplicationContext().getBean(AppSettings.BEAN_NAME);
		}
		return appSettings;
	}


	public DataStore getDataStore() {
		if (dataStore == null) {

			IDataStoreFactory dsf = (IDataStoreFactory) scope.getContext().getBean(IDataStoreFactory.BEAN_NAME);
			dataStore = dsf.getDataStore();
		}
		return dataStore;
	}

	public void writeStreamPacket(IStreamPacket packet) 
	{
		long dts = packet.getTimestamp() & 0xffffffffL;
		if (packet.getDataType() == Constants.TYPE_VIDEO_DATA)
		{

			if(!enableVideo) {
				logger.warn("Video data was disabled beginning of the stream, so discarding video packets.");
				return;
			}

			logger.trace("writeVideoBuffer video data packet timestamp:{} and packet timestamp:{} streamId:{}", dts, packet.getTimestamp(), streamId);
			measureIngestTime(dts, ((CachedEvent)packet).getReceivedTime());
			if (!firstVideoPacketSkipped) {
				firstVideoPacketSkipped = true;
				return;
			}
			int bodySize = packet.getData().limit();
			byte frameType = packet.getData().position(0).get();

			//position 1 nalu type
			//position 2,3,4 composition time offset
			int compositionTimeOffset = (packet.getData().position(2).get() << 16)  | packet.getData().position(3).getShort();
			long pts = dts + compositionTimeOffset;

			//we get 5 less bytes because first 5 bytes is related to the video tag. It's not part of the generic packet
			ByteBuffer byteBuffer = ByteBuffer.allocateDirect(bodySize-5);
			byteBuffer.put(packet.getData().buf().position(5));


			synchronized (muxerList) 
			{
				packetFeeder.writeVideoBuffer(byteBuffer, dts, 0, videoStreamIndex, (frameType & 0xF0) == IVideoStreamCodec.FLV_FRAME_KEY, 0, pts);

				for (Muxer muxer : muxerList) 
				{
					muxer.writeVideoBuffer(byteBuffer, dts, 0, videoStreamIndex, (frameType & 0xF0) == IVideoStreamCodec.FLV_FRAME_KEY, 0, pts);
				}
			}


		}
		else if (packet.getDataType() == Constants.TYPE_AUDIO_DATA) {

			if(!enableAudio) {
				logger.debug("Audio data was disabled beginning of the stream, so discarding audio packets.");
				return;
			}

			if (!firstAudioPacketSkipped) {
				firstAudioPacketSkipped = true;
				return;
			}
			int bodySize = packet.getData().limit();
			//we get 2 less bytes because first 2 bytes is related to the audio tag. It's not part of the generic packet
			ByteBuffer byteBuffer = ByteBuffer.allocateDirect(bodySize-2);
			byteBuffer.put(packet.getData().buf().position(2));

			logger.trace("writeAudioBuffer video data packet timestamp:{} and packet timestamp:{} streamId:{}", dts, packet.getTimestamp(), streamId);


			synchronized (muxerList) 
			{
				packetFeeder.writeAudioBuffer(byteBuffer, audioStreamIndex, dts);

				for (Muxer muxer : muxerList) 
				{
					muxer.writeAudioBuffer(byteBuffer, audioStreamIndex, dts);
				}
			}

		}
	}


	/**
	 * Check if max analyze time has been passed. 
	 * If it initializes the prepare then isRecording is set to true in prepareParameters
	 * 
	 * @return
	 */
	public void checkMaxAnalyzeTotalTime() {
		long totalTime = System.currentTimeMillis() - checkStreamsStartTime;
		int elapsedFrameTimeStamp = lastFrameTimestamp - firstReceivedFrameTimestamp;

		if (totalTime >= (2* maxAnalyzeDurationMS)) 
		{
			logger.error("Total max time({}) is spent to determine video and audio existence for stream:{}. It's skipped waiting", (2*maxAnalyzeDurationMS), streamId);
			logger.info("Streams for {} enableVideo:{} enableAudio:{} total spend time: {} elapsed frame timestamp:{} stop request exists: {}", streamId, enableVideo, enableAudio, totalTime, elapsedFrameTimeStamp, stopRequestExist);

			if (enableAudio || enableVideo) {
				prepareParameters();
			}
			else {
				logger.error("There is no video and audio in the incoming stream: {} closing rtmp connection", streamId);
				closeRtmpConnection();
			}

		}
	}


	public void execute() 
	{

		if (isPipeReaderJobRunning.compareAndSet(false, true)) 
		{
			try 
			{
				if (!isRecording.get()) {				

					if (checkStreamsStartTime == -1) {
						checkStreamsStartTime  = System.currentTimeMillis();
					}


					if (stopRequestExist) {
						logger.info("Stop request exists for stream:{}", streamId);
						broadcastStream.removeStreamListener(MuxAdaptor.this);
						logger.warn("closing adaptor for {} ", streamId);
						closeResources();
						logger.warn("closed adaptor for {}", streamId);
						getStreamHandler().stopPublish(streamId);
						//finally code execute and reset the isPipeReaderJobRunning
						return;

					}

					IStreamCodecInfo codecInfo = broadcastStream.getCodecInfo();
					enableVideo = codecInfo.hasVideo();
					enableAudio = codecInfo.hasAudio();

					getVideoDataConf(codecInfo);
					getAudioDataConf(codecInfo);

					// Sometimes AAC Sequenece Header is received later 
					// so that we check if we get the audio codec parameters correctly

					if (enableVideo && enableAudio && getAudioCodecParameters() != null)
					{
						logger.info("Video and audio is enabled in stream:{} queue size: {}", streamId, queueSize.get());
						prepareParameters();
					}
					else {
						checkMaxAnalyzeTotalTime();
					}
				}

				if (!isRecording.get())
				{

					//if it's not recording, return
					//finally code execute and reset the isPipeReaderJobRunning
					return;
				}


				IStreamPacket packet;
				while ((packet = streamPacketQueue.poll()) != null) {

					queueSize.decrementAndGet();

					if (!firstKeyFrameReceivedChecked && packet.getDataType() == Constants.TYPE_VIDEO_DATA) 
					{
						byte frameType = packet.getData().position(0).get();

						if ((frameType & 0xF0) == IVideoStreamCodec.FLV_FRAME_KEY) 
						{
							firstKeyFrameReceivedChecked = true;
							if(!appAdapter.isValidStreamParameters(width, height, fps, 0, streamId)) {
								logger.info("Stream({}) has not passed specified validity checks so it's stopping", streamId);
								closeRtmpConnection();
								break;
							}
						} else {
							logger.warn("First video packet is not key frame. It will drop for direct muxing. Stream {}", streamId);
							// return if firstKeyFrameReceived is not received
							// below return is important otherwise it does not work with like some encoders(vidiu)
							return;
						}
					}

					//TODO: if server does not accept packets, it does not update the quality
					long dts = packet.getTimestamp() & 0xffffffffL;
					updateQualityParameters(dts, TIME_BASE_FOR_MS);


					if (bufferTimeMs == 0) 
					{
						writeStreamPacket(packet);
					}
					else if (bufferTimeMs > 0)
					{
						addBufferQueue(packet);
					}

				}

				if (stopRequestExist) {
					broadcastStream.removeStreamListener(MuxAdaptor.this);
					logger.warn("closing adaptor for {} ", streamId);
					closeResources();
					logger.warn("closed adaptor for {}", streamId);
					getStreamHandler().stopPublish(streamId);
				}	
			}
			finally {
				//make sure pipeReader is set again no matter if there is an exception above. 
				//Because isPipeReaderJobRunning is not set, it may fill the memory and causes Out Of Memory
				isPipeReaderJobRunning.compareAndSet(true, false);
			}
		}
	}


	public void addBufferQueue(IStreamPacket packet) {
		//it's a ordered queue according to timestamp

		bufferQueue.add(packet);

		try {
			IStreamPacket pktHead = bufferQueue.first();
			IStreamPacket pktTrailer = bufferQueue.last();

			int bufferedDuration = pktTrailer.getTimestamp() - pktHead.getTimestamp();

			if (bufferedDuration > bufferTimeMs*5) {
				//if buffered duration is more than 5 times of the buffer time, remove packets from the head until it reach bufferTimeMs * 2

				//set buffering true to not let writeBufferedPacket method work
				buffering.set(true);

				Iterator<IStreamPacket> iterator = bufferQueue.iterator();

				while (iterator.hasNext()) 
				{
					pktHead = iterator.next();

					bufferedDuration = pktTrailer.getTimestamp() - pktHead.getTimestamp();
					if (bufferedDuration < bufferTimeMs * 2) {
						break;
					}
					iterator.remove();
				} 
			}

			bufferedDuration = pktTrailer.getTimestamp() - pktHead.getTimestamp();

			logger.trace("bufferedDuration:{} trailer timestamp:{} head timestamp:{}", bufferedDuration, pktTrailer.getTimestamp(), pktHead.getTimestamp());
			if (bufferedDuration > bufferTimeMs) 
			{ 
				if (buffering.get()) 
				{
					//have the buffering finish time ms
					bufferingFinishTimeMs = System.currentTimeMillis();
					//have the first packet sent time
					firstPacketReadyToSentTimeMs  = pktTrailer.getTimestamp();
					logger.info("Switching buffering from true to false for stream: {}", streamId);
				}
				//make buffering false whenever bufferDuration is bigger than bufferTimeMS
				//buffering is set to true when there is no packet left in the queue
				buffering.set(false);
			}

			bufferLogCounter++;
			if (bufferLogCounter % COUNT_TO_LOG_BUFFER == 0) {
				logger.info("ReadPacket -> Buffering status {}, buffer duration {}ms buffer time {}ms stream: {}", buffering, bufferedDuration, bufferTimeMs, streamId);
				bufferLogCounter = 0;
			}
		}
		catch (NoSuchElementException e) {
			//You may or may not ignore this exception @mekya
			logger.warn("You may or may not ignore this exception. I mean It can happen time to time in multithread environment -> {}", e.getMessage());
		}


	}


	private void getVideoDataConf(IStreamCodecInfo codecInfo) {
		if (enableVideo) {
			IVideoStreamCodec videoCodec = codecInfo.getVideoCodec();
			if (videoCodec instanceof AVCVideo)
			{
				IoBuffer videoBuffer = videoCodec.getDecoderConfiguration();
				if (videoBuffer != null) {
					videoDataConf = new byte[videoBuffer.limit()-5];
					videoBuffer.position(5).get(videoDataConf);
				}
			}
			else {
				logger.warn("Video codec is not AVC(H264) for stream: {}", streamId);
			}
		}
	}


	private void getAudioDataConf(IStreamCodecInfo codecInfo) {
		if (enableAudio) 
		{
			IAudioStreamCodec audioCodec = codecInfo.getAudioCodec();
			if (audioCodec instanceof AACAudio) 
			{
				IoBuffer audioBuffer = ((AACAudio)audioCodec).getDecoderConfiguration();
				if (audioBuffer != null) {
					audioDataConf = new byte[audioBuffer.limit()-2];
					audioBuffer.position(2).get(audioDataConf);
				}
			}
			else {
				logger.warn("Audio codec is not AAC for stream: {}", streamId);
			}
		}
	}

	private void prepareParameters() {
		try {
			prepare();
			isRecording.set(true);

			//Calling startPublish to here is critical. It's called after encoders are ready and isRecording is true
			//the above prepare method is overriden in EncoderAdaptor so that we resolve calling startPublish just here
			getStreamHandler().startPublish(streamId, broadcastStream.getAbsoluteStartTimeMs(), IAntMediaStreamHandler.PUBLISH_TYPE_RTMP);

		}
		catch(Exception e) {
			logger.error(ExceptionUtils.getStackTrace(e));
			closeRtmpConnection();
		}
	}


	private void measureIngestTime(long pktTimeStamp, long receivedTime) {

		totalIngestedVideoPacketCount++;

		long currentTime = System.currentTimeMillis();
		long packetIngestTime =  (currentTime - receivedTime);
		totalIngestTime += packetIngestTime;

		long absolutePacketIngestTime = currentTime - broadcastStream.getAbsoluteStartTimeMs() - pktTimeStamp;

		absoluteTotalIngestTime += absolutePacketIngestTime;		
	}

	public long getAbsoluteTimeMs() {
		if (broadcastStream != null) {
			return broadcastStream.getAbsoluteStartTimeMs();
		}
		return 0;
	}

	public void updateQualityParameters(long pts, AVRational timebase) {


		long packetTime = av_rescale_q(pts, timebase, TIME_BASE_FOR_MS);
		packetTimeList.add(new PacketTime(packetTime, System.currentTimeMillis()));


		if (packetTimeList.size() > 300) {
			//limit the size.
			packetTimeList.remove(0);
		}

		PacketTime firstPacket = packetTimeList.getFirst();
		PacketTime lastPacket = packetTimeList.getLast();

		long elapsedTime = lastPacket.systemTimeMs - firstPacket.systemTimeMs;
		long packetTimeDiff = lastPacket.packetTimeMs - firstPacket.packetTimeMs;


		double speed = 0L;
		if (elapsedTime > 0)
		{
			speed = (double) packetTimeDiff / elapsedTime;
			if (logger.isWarnEnabled() && Double.isNaN(speed)) {
				logger.warn("speed is NaN, packetTime: {}, first item packetTime: {}, elapsedTime:{}", packetTime, firstPacket.packetTimeMs, elapsedTime);
			}
		}
		updateStreamQualityParameters(this.streamId, null, speed, getInputQueueSize());


	}

	public void closeRtmpConnection() {

		ClientBroadcastStream clientBroadcastStream = getBroadcastStream();
		if (clientBroadcastStream != null) {
			clientBroadcastStream.stop();
			IStreamCapableConnection connection = clientBroadcastStream.getConnection();
			if (connection != null) {
				connection.close();
			}
		}
	}

	public void writePacket(AVStream stream, AVPacket pkt) {


		updateQualityParameters(pkt.pts(), stream.time_base());

		if (!firstKeyFrameReceivedChecked && stream.codecpar().codec_type() == AVMEDIA_TYPE_VIDEO) 
		{
			int keyFrame = pkt.flags() & AV_PKT_FLAG_KEY;
			if (keyFrame == 1) 
			{
				firstKeyFrameReceivedChecked = true;
				if(!appAdapter.isValidStreamParameters(width, height, fps, 0, streamId)) 
				{
					logger.info("Stream({}) has not passed specified validity checks so it's stopping", streamId);
					closeRtmpConnection();
					return;
				}
			} 
			else {
				logger.warn("First video packet is not key frame. It will drop for direct muxing. Stream {}", streamId);
				// return if firstKeyFrameReceived is not received
				// below return is important otherwise it does not work with like some encoders(vidiu)
				return;
			}
		}

		synchronized (muxerList)
		{
			packetFeeder.writePacket(pkt, stream.codecpar().codec_type());
			for (Muxer muxer : muxerList) 
			{
				if (!(muxer instanceof WebMMuxer)) 
				{
					muxer.writePacket(pkt, stream);
				}
			}
		}
	}

	public synchronized void writeTrailer() {
		packetFeeder.writeTrailer();
		for (Muxer muxer : muxerList) {
			muxer.writeTrailer();
		}
	}


	public synchronized void closeResources() {
		logger.info("close resources for streamId -> {}", streamId);


		if (packetPollerId != -1) {
			vertx.cancelTimer(packetPollerId);
			logger.info("Cancelling packet poller task(id:{}) for streamId: {}", packetPollerId, streamId);
			packetPollerId = -1;

		}

		if (bufferedPacketWriterId != -1) {
			logger.info("Removing buffered packet writer id {} for stream: {}", bufferedPacketWriterId, streamId);
			vertx.cancelTimer(bufferedPacketWriterId);
			bufferedPacketWriterId = -1;
			writeAllBufferedPackets();
		}

		writeTrailer();

		if (videoExtraDataPointer != null) {
			av_free(videoExtraDataPointer.position(0));
			videoExtraDataPointer.close();
			videoExtraDataPointer = null;
		}

		if (audioExtraDataPointer != null) {
			av_free(audioExtraDataPointer.position(0));
			audioExtraDataPointer.close();
			audioExtraDataPointer = null;
		}

		updateStreamQualityParameters(this.streamId, null, 0, getInputQueueSize());
		getStreamHandler().muxAdaptorRemoved(this);

		isRecording.set(false);
	}


	/**
	 * This method means that if the MuxAdaptor writes 
	 * incoming packets to muxers({@link MuxAdaptor#muxerList}) directly without any StreamAdaptor/Encoders
	 * 
	 * It's true for RTMP, SRT ingest to MP4, HLS, RTMP Endpoint writing 
	 * but it's not true WebRTC ingest.
	 * 
	 * This method is being implemented in subclasses
	 * @return
	 */
	public boolean directMuxingSupported() {
		//REFACTOR: I think it may be good idea to proxy every packet through StreamAdaptor even for RTMP Ingest
		//It'll likely provide better compatibility for codecs and formats
		return true;
	}


	@Override
	public void start() {
		logger.info("Number of items in the queue while adaptor is being started to prepare is {}", getInputQueueSize());
		startTimeMs = System.currentTimeMillis();

		vertx.executeBlocking(() -> {
			logger.info("before prepare for {}", streamId);
			Boolean successful = false;
			try {

				packetPollerId = vertx.setPeriodic(10, t-> 
				vertx.executeBlocking(()-> {
					try {
						execute();
					}
					catch (Exception e) {
						logger.error(ExceptionUtils.getStackTrace(e));
					}
					return null;
				}, false));



				if (bufferTimeMs > 0)  
				{
					//this is just a simple hack to run in different context(different thread).
					logger.info("Scheduling the buffered packet writer for stream: {} buffer duration:{}ms", streamId, bufferTimeMs);
					bufferedPacketWriterId = vertx.setPeriodic(10, k -> 

					vertx.executeBlocking(()-> {
						try {
							writeBufferedPacket();
						}
						catch (Exception e) {
							logger.error(ExceptionUtils.getStackTrace(e));
						}
						return null;
					}, false)
							);

				}

				logger.info("Number of items in the queue while starting: {} for stream: {}", 
						getInputQueueSize(), streamId);

				successful = true;

			}
			catch (Exception e) {
				logger.error(ExceptionUtils.getStackTrace(e));
			}
			return successful;

		}, false  // run unordered
				);
	}

	@Override
	public void stop(boolean shutdownCompletely) {
		logger.info("Calling stop for {} input queue size:{}", streamId, getInputQueueSize());
		stopRequestExist = true;
	}

	public int getInputQueueSize() {
		return queueSize.get();
	}

	public boolean isStopRequestExist() {
		return stopRequestExist;
	}

	public void debugSetStopRequestExist(boolean stopRequest) {
		this.stopRequestExist = stopRequest;
	}


	/**
	 * This method is called when rtmpIngestBufferTime is bigger than zero
	 */
	public void writeBufferedPacket()
	{
		synchronized (this) {

			if (isBufferedWriterRunning.compareAndSet(false, true)) {
				try {
					if (!buffering.get())
					{
						while(!bufferQueue.isEmpty())
						{
							IStreamPacket tempPacket = bufferQueue.first();

							long now = System.currentTimeMillis();

							//elapsed time since the buffering  finished 
							long passedTime = now - bufferingFinishTimeMs;

							//time difference between this packet and the packet's timestamp when buffer is big enough to send
							long pktTimeDifferenceMs = tempPacket.getTimestamp() - firstPacketReadyToSentTimeMs;

							if (pktTimeDifferenceMs < passedTime)
							{
								writeStreamPacket(tempPacket);

								bufferQueue.remove(tempPacket); //remove the packet from the queue
							}
							else {
								break;
							}

						}

						//update buffering. If bufferQueue is empty, it should start buffering
						buffering.set(bufferQueue.isEmpty());

					}
					bufferLogCounter++; //we use this parameter in execute method as well
					if (bufferLogCounter % COUNT_TO_LOG_BUFFER  == 0) 
					{
						IStreamPacket streamPacket = !bufferQueue.isEmpty() ? bufferQueue.first() : null;
						int bufferedDuration = 0;
						if (streamPacket != null) {
							bufferedDuration = bufferQueue.last().getTimestamp() - streamPacket.getTimestamp();
						}
						logger.info("WriteBufferedPacket -> Buffering status {}, buffer duration {}ms buffer time {}ms stream: {}", buffering, bufferedDuration, bufferTimeMs, streamId);
						bufferLogCounter = 0;
					}
				}
				finally {
					isBufferedWriterRunning.compareAndSet(true, false);
				}
			}
		}
	}


	private void writeAllBufferedPackets()
	{
		synchronized (this) {
			logger.info("write all buffered packets for stream: {} ", streamId);
			while (!bufferQueue.isEmpty()) {

				IStreamPacket tempPacket = bufferQueue.first();
				writeStreamPacket(tempPacket);
				bufferQueue.remove(tempPacket);
			}
		}

	}

	@Override
	public void packetReceived(IBroadcastStream stream, IStreamPacket packet)
	{

		lastFrameTimestamp = packet.getTimestamp();
		if (firstReceivedFrameTimestamp  == -1) {
			logger.info("first received frame timestamp: {} for stream:{} ", lastFrameTimestamp, streamId);
			firstReceivedFrameTimestamp = lastFrameTimestamp;
		}
		if (stopRequestExist) 
		{
			//there may be a bug that red5 cannot stop the stream ClientBroadcastStream and it may fill the memory
			logger.warn("Stop request exist and dropping incoming packet for stream:{}", streamId);
			//try to close the connection again
			closeRtmpConnection();
			return;
		}



		queueSize.incrementAndGet();

		CachedEvent event = new CachedEvent();
		event.setData(packet.getData().duplicate());
		event.setDataType(packet.getDataType());
		event.setReceivedTime(System.currentTimeMillis());
		event.setTimestamp(packet.getTimestamp());

		streamPacketQueue.add(event);

	}

	@Override
	public boolean isRecording() {
		return isRecording.get();
	}

	@Override
	public boolean isAppending() {
		return false;
	}

	@Override
	public FileConsumer getFileConsumer() {
		return null;
	}

	@Override
	public void setFileConsumer(FileConsumer recordingConsumer) {
		//No need to implement
	}

	@Override
	public String getFileName() {
		return null;
	}

	@Override
	public void setFileName(String fileName) {
		//No need to implement
	}

	public List<Muxer> getMuxerList() {
		return muxerList;
	}


	public void setStorageClient(StorageClient storageClient) {
		this.storageClient = storageClient;
	}

	public boolean isWebRTCEnabled() {
		return webRTCEnabled;
	}

	public void setWebRTCEnabled(boolean webRTCEnabled) {
		this.webRTCEnabled = webRTCEnabled;
	}

	public void setHLSFilesDeleteOnExit(boolean deleteHLSFilesOnExit) {
		this.deleteHLSFilesOnExit = deleteHLSFilesOnExit;
	}

	public void setPreviewOverwrite(boolean overwrite) {
		this.previewOverwrite = overwrite;
	}


	public boolean isPreviewOverwrite() {
		return previewOverwrite;
	}

	public long getStartTime() {
		return startTime;
	}


	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}

	public List<EncoderSettings> getEncoderSettingsList() {
		return encoderSettingsList;
	}

	public void setEncoderSettingsList(List<EncoderSettings> encoderSettingsList) {
		this.encoderSettingsList = encoderSettingsList;
	}

	public boolean isStreamSource() {
		return isStreamSource;
	}

	public void setStreamSource(boolean isStreamSource) {
		this.isStreamSource = isStreamSource;
	}

	public boolean isObjectDetectionEnabled() {
		return objectDetectionEnabled;
	}

	public void setObjectDetectionEnabled(Boolean objectDetectionEnabled) {
		this.objectDetectionEnabled = objectDetectionEnabled;
	}

	public int getPreviewCreatePeriod() {
		return previewCreatePeriod;
	}

	public void setPreviewCreatePeriod(int previewCreatePeriod) {
		this.previewCreatePeriod = previewCreatePeriod;
	}

	public String getStreamId() {
		return streamId;
	}

	public void setStreamId(String streamId) {
		this.streamId = streamId;
	}

	public StorageClient getStorageClient() {
		return storageClient;
	}

	/**
	 * Setter for {@link #firstKeyFrameReceivedChecked}
	 *
	 * @param firstKeyFrameReceivedChecked
	 */
	public void setFirstKeyFrameReceivedChecked(boolean firstKeyFrameReceivedChecked) {
		this.firstKeyFrameReceivedChecked = firstKeyFrameReceivedChecked;
	}

	public Broadcast getBroadcast() {

		if (broadcast == null) {

			broadcast = dataStore.get(this.streamId);
		}
		return broadcast;
	}

	// this is for test cases
	public void setBroadcast(Broadcast broadcast) {
		this.broadcast = broadcast;
	}
	// this is for test cases
	public void setGeneratePreview(boolean generatePreview){
		this.generatePreview=generatePreview;
	}

	public int getPreviewHeight() {
		return previewHeight;
	}

	public void setPreviewHeight(int previewHeight) {
		this.previewHeight = previewHeight;
	}

	public Mp4Muxer createMp4Muxer() {
		Mp4Muxer mp4Muxer = new Mp4Muxer(storageClient, vertx, appSettings.getS3StreamsFolderPath());
		mp4Muxer.setAddDateTimeToSourceName(addDateTimeToMp4FileName);
		return mp4Muxer;
	}

	private Muxer addMp4Muxer() {
		Mp4Muxer mp4Muxer = createMp4Muxer();
		addMuxer(mp4Muxer);
		getDataStore().setMp4Muxing(streamId, RECORDING_ENABLED_FOR_STREAM);
		return mp4Muxer;
	}

	/**
	 * Start recording is used to start recording on the fly(stream is broadcasting).
	 * @param recordType MP4 or WEBM
	 * @param resolutionHeight	resolution height for the recording
	 * @return
	 */
	public RecordMuxer startRecording(RecordType recordType, int resolutionHeight) {

		if (!isRecording.get()) {
			logger.warn("Starting recording return false for stream:{} because stream is being prepared", streamId);
			return null;
		}

		if(isAlreadyRecording(recordType, resolutionHeight)) {
			logger.warn("Record is called while {} is already recording.", streamId);
			return null;
		}


		RecordMuxer muxer = null;
		if(recordType == RecordType.MP4) {
			Mp4Muxer mp4Muxer = createMp4Muxer();
			muxer = mp4Muxer;

			addMuxer(muxer, resolutionHeight);
		}
		else if(recordType == RecordType.WEBM) {
			//WebM record is not supported for incoming RTMP streams
		}
		else {
			logger.error("Unrecognized record type: {}", recordType);
		}

		return muxer;
	}

	public boolean prepareMuxer(Muxer muxer, int resolutionHeight) 
	{
		boolean streamAdded = false;

		muxer.init(scope, streamId, resolutionHeight, broadcast != null ? broadcast.getSubFolder(): null, 0);
		logger.info("prepareMuxer for stream:{} muxer:{}", streamId, muxer.getClass().getSimpleName());

		if (streamSourceInputFormatContext != null) 
		{


			for (int i = 0; i < streamSourceInputFormatContext.nb_streams(); i++) 
			{
				if (!muxer.addStream(streamSourceInputFormatContext.streams(i).codecpar(), streamSourceInputFormatContext.streams(i).time_base(), i)) {
					logger.warn("muxer add streams returns false {}", muxer.getFormat());
					break;
				}
				else {
					streamAdded = true;
				}
			}
		}
		else 
		{
			AVCodecParameters videoParameters = getVideoCodecParameters();
			if (videoParameters != null) {
				logger.info("Add video stream to muxer:{} for streamId:{}", muxer.getClass().getSimpleName(), streamId);
				if (muxer.addStream(videoParameters, TIME_BASE_FOR_MS, videoStreamIndex)) {
					streamAdded = true;
				}
			}

			AVCodecParameters audioParameters = getAudioCodecParameters();
			if (audioParameters != null) {
				logger.info("Add audio stream to muxer:{} for streamId:{}", muxer.getClass().getSimpleName(), streamId);
				if (muxer.addStream(audioParameters, TIME_BASE_FOR_MS, audioStreamIndex)) {
					streamAdded = true;
				}
			}
		}

		boolean prepared = false;
		if (streamAdded) 
		{
			prepared = muxer.prepareIO();

			if (prepared) 
			{
				prepared = addMuxerInternal(muxer);
				logger.info("Muxer:{} is prepared succesfully for streamId:{}", muxer.getClass().getSimpleName(), streamId);
			}
			else 
			{
				logger.warn("Muxer:{} cannot be prepared for streamId:{}", muxer.getClass().getSimpleName(), streamId);
			}

			//TODO: Check to release the resources if it's not already released
		}

		return prepared;
	}

	public boolean isAlreadyRecording(RecordType recordType, int resolutionHeight) {
		for (Muxer muxer : muxerList) {
			if(((muxer instanceof Mp4Muxer && recordType == RecordType.MP4)
					|| (muxer instanceof WebMMuxer && recordType == RecordType.WEBM))
					&& (resolutionHeight == 0 || resolutionHeight == height)) {
				return true;
			}
		}
		return false;
	}


	public Muxer findDynamicRecordMuxer(RecordType recordType) {
		synchronized (muxerList)
		{
			Iterator<Muxer> iterator = muxerList.iterator();
			while (iterator.hasNext())
			{
				Muxer muxer = iterator.next();
				if ((recordType == RecordType.MP4 && muxer instanceof Mp4Muxer)
						|| (recordType == RecordType.WEBM && muxer instanceof WebMMuxer)) {
					return muxer;
				}
			}
		}
		return null;
	}

	/**
	 * Stop recording is called to stop recording when the stream is broadcasting(on the fly)
	 * 
	 * @param recordType	MP4 or WEBM
	 * @param resolutionHeight	resolution height for the recording
	 * @return
	 */
	public RecordMuxer stopRecording(RecordType recordType, int resolutionHeight)
	{
		logger.info("stopRecording is called for streamId:{} and resolution:{}", streamId, resolutionHeight);
		Muxer muxer = findDynamicRecordMuxer(recordType);
		if (muxer != null && recordType == RecordType.MP4)
		{
			muxerList.remove(muxer);
			muxer.writeTrailer();
			return (RecordMuxer) muxer;
		}
		return null;
	}

	public ClientBroadcastStream getBroadcastStream() {
		return broadcastStream;
	}


	public Result startRtmpStreaming(String rtmpUrl, int resolutionHeight)
	{
		Result result = new Result(false);
		rtmpUrl = rtmpUrl.replaceAll("[\n\r\t]", "_");

		if (!isRecording.get()) 
		{
			logger.warn("Start rtmp streaming return false for stream:{} because stream is being prepared", streamId);
			result.setMessage("Start rtmp streaming return false for stream:"+ streamId +" because stream is being prepared. Try again");			
			return result;
		}
		logger.info("start rtmp streaming for stream id:{} to {} with requested resolution height{} stream resolution:{}", streamId, rtmpUrl, resolutionHeight, height);

		if (resolutionHeight == 0 || resolutionHeight == height) 
		{
			RtmpMuxer rtmpMuxer = new RtmpMuxer(rtmpUrl, vertx);
			rtmpMuxer.setStatusListener(this);
			if (prepareMuxer(rtmpMuxer, resolutionHeight)) 
			{
				result.setSuccess(true);
			}
			else 
			{
				logger.error("RTMP prepare returned false so that rtmp pushing to {} for {} didn't started ", rtmpUrl, streamId);
				result.setMessage("RTMP prepare returned false so that rtmp pushing to " + rtmpUrl + " for "+ streamId +" didn't started ");
			}
		}

		return result;
	}

	public void sendEndpointErrorNotifyHook(String url){
		IContext context = MuxAdaptor.this.scope.getContext();
		ApplicationContext appCtx = context.getApplicationContext();
		AntMediaApplicationAdapter adaptor = (AntMediaApplicationAdapter) appCtx.getBean(AntMediaApplicationAdapter.BEAN_NAME);
		adaptor.endpointFailedUpdate(this.streamId, url);
	}

	/**
	 * Periodically check the endpoint health status every 2 seconds
	 * If each check returned failed, try to republish to the endpoint
	 * @param url is the URL of the endpoint
	 */
	public void endpointStatusHealthCheck(String url)
	{
		rtmpEndpointRetryLimit = appSettings.getEndpointRepublishLimit();
		healthCheckPeriodMS = appSettings.getEndpointHealthCheckPeriodMs();
		vertx.setPeriodic(healthCheckPeriodMS, id ->
		{

			String status = statusMap.getOrDefault(url, null);
			logger.info("Checking the endpoint health for: {} and status: {} ", url, status);
			//Broadcast might get deleted in the process of checking
			if(broadcast == null || status == null || status.equals(IAntMediaStreamHandler.BROADCAST_STATUS_FINISHED))
			{
				logger.info("Endpoint trailer is written or broadcast deleted for: {} ", url);
				clearCounterMapsAndCancelTimer(url, id);
			}
			if(status.equals(IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING))
			{
				logger.info("Health check process finished since endpoint {} is broadcasting", url);
				clearCounterMapsAndCancelTimer(url, id);
			}
			else if(status.equals(IAntMediaStreamHandler.BROADCAST_STATUS_ERROR) || statusMap.get(url).equals(IAntMediaStreamHandler.BROADCAST_STATUS_FAILED) )
			{
				tryToRepublish(url, id);
			}
		});
	}


	public void clearCounterMapsAndCancelTimer(String url, Long id) 
	{
		isHealthCheckStartedMap.remove(url);
		errorCountMap.remove(url);
		retryCounter.remove(url);
		vertx.cancelTimer(id);
	}


	private void tryToRepublish(String url, Long id) 
	{
		int errorCount = errorCountMap.getOrDefault(url, 1);
		if(errorCount < 3)
		{
			errorCountMap.put(url, errorCount+1);
			logger.info("Endpoint check returned error for {} times for endpoint {}", errorCount , url);
		}
		else
		{
			int tmpRetryCount = retryCounter.getOrDefault(url, 1);
			if( tmpRetryCount <= rtmpEndpointRetryLimit){
				logger.info("Health check process failed, trying to republish to the endpoint: {}", url);

				//TODO: 0 as second parameter may cause a problem
				stopRtmpStreaming(url, 0);
				startRtmpStreaming(url, height);
				retryCounter.put(url, tmpRetryCount + 1);
			}
			else{
				logger.info("Exceeded republish retry limit, endpoint {} can't be reached and will be closed" , url);
				stopRtmpStreaming(url, 0);
				sendEndpointErrorNotifyHook(url);
				retryCounter.remove(url);
			}
			//Clear the data and cancel timer to free memory and CPU.
			isHealthCheckStartedMap.remove(url);
			errorCountMap.remove(url);
			vertx.cancelTimer(id);
		}
	}

	@Override
	public void endpointStatusUpdated(String url, String status)
	{
		logger.info("Endpoint status updated to {}  for streamId: {} for url: {}", status, streamId, url);

		/**
		 * Below code snippet updates the database at max 3 seconds interval
		 */
		endpointStatusUpdateMap.put(url, status);

		statusMap.put(url,status);

		if((status.equals(IAntMediaStreamHandler.BROADCAST_STATUS_ERROR) || status.equals(IAntMediaStreamHandler.BROADCAST_STATUS_FAILED)) && !isHealthCheckStartedMap.getOrDefault(url, false)){
			endpointStatusHealthCheck(url);
			isHealthCheckStartedMap.put(url, true);
		}

		if(status.equals(IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING) && retryCounter.getOrDefault(url, null) != null){
			retryCounter.remove(url);
		}

		if (endpointStatusUpdaterTimer.get() == -1) 
		{
			long timerId = vertx.setTimer(3000, h ->
			{
				endpointStatusUpdaterTimer.set(-1l);
				try {
					//update broadcast object
					logger.info("Updating endpoint status in datastore for streamId:{}", streamId);
					broadcast = getDataStore().get(broadcast.getStreamId());

					updateBroadcastRecord();
					endpointStatusUpdateMap.clear();


				} catch (Exception e) {
					logger.error(ExceptionUtils.getStackTrace(e));
				}
			});

			endpointStatusUpdaterTimer.set(timerId);
		}

	}


	private void updateBroadcastRecord() {
		if (broadcast != null) {
			for (Iterator iterator = broadcast.getEndPointList().iterator(); iterator.hasNext();) 
			{
				Endpoint endpoint = (Endpoint) iterator.next();
				String statusUpdate = endpointStatusUpdateMap.getOrDefault(endpoint.getRtmpUrl(), null);
				if (statusUpdate != null) {
					endpoint.setStatus(statusUpdate);
				}
				else {
					logger.warn("Endpoint is not found to update its status to {} for rtmp url:{}", statusUpdate, endpoint.getRtmpUrl());
				}
			}
			getDataStore().updateBroadcastFields(broadcast.getStreamId(), broadcast);

		}
		else {
			logger.error("Broadcast with streamId:{} is not found to update its endpoint status. It's likely a zombi stream", streamId);
		}
	}

	public RtmpMuxer getRtmpMuxer(String rtmpUrl)
	{
		RtmpMuxer rtmpMuxer = null;
		synchronized (muxerList)
		{
			Iterator<Muxer> iterator = muxerList.iterator();
			while (iterator.hasNext())
			{
				Muxer muxer = iterator.next();
				if (muxer instanceof RtmpMuxer &&
						((RtmpMuxer)muxer).getOutputURL().equals(rtmpUrl))
				{
					rtmpMuxer = (RtmpMuxer) muxer;
					break;
				}
			}
		}
		return rtmpMuxer;
	}

	public Result stopRtmpStreaming(String rtmpUrl, int resolutionHeight)
	{
		Result result = new Result(false);
		if (resolutionHeight == 0 || resolutionHeight == height) 
		{
			RtmpMuxer rtmpMuxer = getRtmpMuxer(rtmpUrl);
			String status = statusMap.getOrDefault(rtmpUrl, null);
			if (rtmpMuxer != null)
			{
				muxerList.remove(rtmpMuxer);
				statusMap.remove(rtmpUrl);
				rtmpMuxer.writeTrailer();
				result.setSuccess(true);
			}
			else if(status == null
					|| IAntMediaStreamHandler.BROADCAST_STATUS_ERROR.equals(status)
					|| IAntMediaStreamHandler.BROADCAST_STATUS_FAILED.equals(status)
					|| IAntMediaStreamHandler.BROADCAST_STATUS_FINISHED.equals(status))
			{
				/**
				 * When the stream is not found in the muxer list, stream url could be invalid or the stream is finished.
				 * In either case, we should return success.
				 */
				result.setSuccess(true);
			}
		}
		return result;
	}

	public boolean isEnableVideo() {
		return enableVideo;
	}


	public void setEnableVideo(boolean enableVideo) {
		this.enableVideo = enableVideo;
	}


	public boolean isEnableAudio() {
		return enableAudio;
	}


	public void setEnableAudio(boolean enableAudio) {
		this.enableAudio = enableAudio;
	}


	public int getLastFrameTimestamp() {
		return lastFrameTimestamp;
	}


	public void setLastFrameTimestamp(int lastFrameTimestamp) {
		this.lastFrameTimestamp = lastFrameTimestamp;
	}

	public void setAppSettings(AppSettings appSettings) {
		this.appSettings = appSettings;
	}

	public long getBufferTimeMs() {
		return bufferTimeMs;
	}

	public boolean isBuffering() {
		return buffering.get();
	}

	public void setBuffering(boolean buffering) {
		this.buffering.set(buffering);
	}

	public String getDataChannelWebHookURL() {
		return dataChannelWebHookURL;
	}

	public boolean isDeleteDASHFilesOnExit() {
		return deleteDASHFilesOnExit;
	}


	public void setDeleteDASHFilesOnExit(boolean deleteDASHFilesOnExit) {
		this.deleteDASHFilesOnExit = deleteDASHFilesOnExit;
	}

	public boolean isAvc() {
		return avc;
	}

	public void setAvc(boolean avc) {
		this.avc = avc;
	}

	public ConcurrentSkipListSet<IStreamPacket> getBufferQueue() {
		return bufferQueue;
	}

	public void setBufferingFinishTimeMs(long bufferingFinishTimeMs) {
		this.bufferingFinishTimeMs = bufferingFinishTimeMs;
	}

	public LinkedList<PacketTime> getPacketTimeList() {
		return packetTimeList;
	}

	public int getVideoStreamIndex() {
		return videoStreamIndex;
	}


	public void setVideoStreamIndex(int videoStreamIndex) {
		this.videoStreamIndex = videoStreamIndex;
	}


	public int getAudioStreamIndex() {
		return audioStreamIndex;
	}


	public void setAudioStreamIndex(int audioStreamIndex) {
		this.audioStreamIndex = audioStreamIndex;
	}

	public void addPacketListener(IPacketListener listener) {
		StreamParametersInfo videoInfo = new StreamParametersInfo();
		videoInfo.setCodecParameters(getVideoCodecParameters());
		videoInfo.setTimeBase(getVideoTimeBase());
		videoInfo.setEnabled(enableVideo);
		StreamParametersInfo audioInfo = new StreamParametersInfo();
		audioInfo.setCodecParameters(getAudioCodecParameters());
		audioInfo.setTimeBase(getAudioTimeBase());
		audioInfo.setEnabled(enableAudio);

		listener.setVideoStreamInfo(streamId, videoInfo);
		listener.setAudioStreamInfo(streamId, audioInfo);
		packetFeeder.addListener(listener);
	}

	public boolean removePacketListener(IPacketListener listener) {
		return packetFeeder.removeListener(listener);
	}

	public void setVideoCodecParameter(AVCodecParameters videoCodecParameters) {
		this.videoCodecParameters = videoCodecParameters;
	}

	public void setAudioCodecParameter(AVCodecParameters audioCodecParameters) {
		this.audioCodecParameters = audioCodecParameters;
	}

	public AVRational getVideoTimeBase() {
		return videoTimeBase;
	}

	public AVRational getAudioTimeBase() {
		return audioTimeBase;
	}

	public void setVideoTimeBase(AVRational videoTimeBase) {
		this.videoTimeBase = videoTimeBase;
	}


	public void setAudioTimeBase(AVRational audioTimeBase) {
		this.audioTimeBase = audioTimeBase;
	}

	public Vertx getVertx() {
		return vertx;
	}


	public Map<String, String> getEndpointStatusUpdateMap() {
		return endpointStatusUpdateMap;
	}

	public Map<String, Boolean> getIsHealthCheckStartedMap(){ return isHealthCheckStartedMap;}


	public void setHeight(int height) {
		this.height = height;
	}


	public void setIsRecording(boolean isRecording) {
		this.isRecording.set(isRecording);
	}


	public void setAudioDataConf(byte[] audioDataConf) {
		this.audioDataConf = audioDataConf;
	}

	public boolean isBlacklistCodec(int codecId) {
		return (codecId == AV_CODEC_ID_PNG);
	}


	public void setBufferTimeMs(long bufferTimeMs) {
		this.bufferTimeMs = bufferTimeMs;
	}


	public AtomicBoolean getIsPipeReaderJobRunning() {
		return isPipeReaderJobRunning;
	}

}


