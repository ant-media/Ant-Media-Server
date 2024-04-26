package io.antmedia.muxer;

import static org.bytedeco.ffmpeg.global.avcodec.*;
import static org.bytedeco.ffmpeg.global.avformat.*;
import static org.bytedeco.ffmpeg.global.avformat.AVIO_FLAG_WRITE;
import static org.bytedeco.ffmpeg.global.avformat.av_write_frame;
import static org.bytedeco.ffmpeg.global.avformat.av_write_trailer;
import static org.bytedeco.ffmpeg.global.avformat.avformat_close_input;
import static org.bytedeco.ffmpeg.global.avformat.avformat_find_stream_info;
import static org.bytedeco.ffmpeg.global.avformat.avformat_free_context;
import static org.bytedeco.ffmpeg.global.avformat.avformat_new_stream;
import static org.bytedeco.ffmpeg.global.avformat.avformat_open_input;
import static org.bytedeco.ffmpeg.global.avformat.avformat_write_header;
import static org.bytedeco.ffmpeg.global.avformat.avio_closep;
import static org.bytedeco.ffmpeg.global.avutil.AVMEDIA_TYPE_AUDIO;
import static org.bytedeco.ffmpeg.global.avutil.AVMEDIA_TYPE_DATA;
import static org.bytedeco.ffmpeg.global.avutil.AVMEDIA_TYPE_VIDEO;
import static org.bytedeco.ffmpeg.global.avutil.AV_NOPTS_VALUE;
import static org.bytedeco.ffmpeg.global.avutil.AV_PIX_FMT_YUV420P;
import static org.bytedeco.ffmpeg.global.avutil.AV_ROUND_NEAR_INF;
import static org.bytedeco.ffmpeg.global.avutil.AV_ROUND_PASS_MINMAX;
import static org.bytedeco.ffmpeg.global.avutil.av_dict_free;
import static org.bytedeco.ffmpeg.global.avutil.av_dict_set;
import static org.bytedeco.ffmpeg.global.avutil.av_rescale_q;
import static org.bytedeco.ffmpeg.global.avutil.av_rescale_q_rnd;
import static org.bytedeco.ffmpeg.global.avutil.av_strerror;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.antmedia.FFmpegUtilities;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.bytedeco.ffmpeg.avcodec.AVBSFContext;
import org.bytedeco.ffmpeg.avcodec.AVBitStreamFilter;
import org.bytedeco.ffmpeg.avcodec.AVCodec;
import org.bytedeco.ffmpeg.avcodec.AVCodecContext;
import org.bytedeco.ffmpeg.avcodec.AVCodecParameters;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.avformat.AVIOContext;
import org.bytedeco.ffmpeg.avformat.AVStream;
import org.bytedeco.ffmpeg.avutil.AVChannelLayout;
import org.bytedeco.ffmpeg.avutil.AVDictionary;
import org.bytedeco.ffmpeg.avutil.AVRational;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avformat;
import org.bytedeco.javacpp.BytePointer;
import org.red5.server.api.IContext;
import org.red5.server.api.scope.IScope;
import org.red5.server.api.stream.IStreamFilenameGenerator;
import org.red5.server.api.stream.IStreamFilenameGenerator.GenerationType;
import org.red5.server.stream.DefaultStreamFilenameGenerator;
import org.red5.server.util.ScopeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;

import io.antmedia.AppSettings;
import io.vertx.core.Vertx;

/**
 * PLEASE READ HERE BEFORE YOU IMPLEMENT A MUXER THAT INHERITS THIS CLASS
 *
 *
 * One muxer can be used by multiple encoder so some functions(init,
 * writeTrailer) may be called multiple times, save functions with guards and
 * sync blocks
 *
 * Muxer MUST NOT changed packet content somehow, data, stream index, pts, dts,
 * duration, etc. because packets are shared with other muxers. If packet
 * content changes, other muxer cannot do their job correctly.
 *
 * Muxers generally run in multi-thread environment so that writePacket
 * functions can be called by different thread at the same time. Protect
 * writePacket with synchronized keyword
 *
 *
 * @author mekya
 *
 */
public abstract class Muxer {

	private long currentVoDTimeStamp = 0;

	protected String extension;
	protected String format;
	protected boolean isInitialized = false;

	protected Map<String, String> options = new HashMap<>();
	protected Logger logger;

	protected static Logger loggerStatic = LoggerFactory.getLogger(Muxer.class);

	protected AVFormatContext outputFormatContext;

	public static final String DATE_TIME_PATTERN = "yyyy-MM-dd_HH-mm-ss.SSS";

	protected File file;

	protected Vertx vertx;

	protected IScope scope;

	private boolean addDateTimeToResourceName = false;

	protected AtomicBoolean isRunning = new AtomicBoolean(false);

	protected byte[] videoExtradata = null;

	public static final String TEMP_EXTENSION = ".tmp_extension";

	protected int time2log = 0;

	protected AVPacket audioPkt;

	protected List<Integer> registeredStreamIndexList = new ArrayList<>();
	/**
	 * Bitstream filter name that will be applied to packets
	 */
	protected String bsfVideoName = null;

	protected String streamId = null;

	protected Map<Integer, AVRational> inputTimeBaseMap = new ConcurrentHashMap<>();


	protected AVBSFContext videoBsfFilterContext = null;

	protected int videoWidth;
	protected int videoHeight;

	protected volatile boolean headerWritten = false;

	/**
	 * This is the initial original resource name without any suffix such _1, _2, or .mp4, .webm
	 */
	protected String initialResourceNameWithoutExtension;

	protected AVPacket tmpPacket;

	protected long firstAudioDts = 0;
	protected long firstVideoDts = 0;

	protected AVPacket videoPkt;
	protected int rotation;

	/**
	 * ts and m4s files index length
	 */
	public static final int SEGMENT_INDEX_LENGTH = 9;

	protected Map<Integer, Integer> inputOutputStreamIndexMap = new ConcurrentHashMap<>();

	private int resolution;

	public  static final AVRational avRationalTimeBase;
	static {
		avRationalTimeBase = new AVRational();
		avRationalTimeBase.num(1);
		avRationalTimeBase.den(1);
	}

	/**
	 * This class is used generally to send direct video buffer to muxer
	 * @author mekya
	 *
	 */
	public static class VideoBuffer {
		
		
		private ByteBuffer encodedVideoFrame;
		/**
		 * DTS and PTS may be normalized values according to the audio
		 * This is why there is {@link #originalFrameTimeMs} exists
		 */
		private long dts;
		private long pts; 
		
		private long firstFrameTimeStamp;
		
		private long originalFrameTimeMs;
		private int frameRotation;
		private int streamIndex;
		private boolean keyFrame;
		
	
		public void setEncodedVideoFrame(ByteBuffer encodedVideoFrame) {
			this.encodedVideoFrame = encodedVideoFrame;
		}
		
		public void setTimeStamps(long dts, long pts, long firstFrameTimeStamp, long originalFrameTimeMs) {
			this.dts = dts;
			this.pts = pts;
			this.firstFrameTimeStamp = firstFrameTimeStamp;
			this.originalFrameTimeMs = originalFrameTimeMs;
		}
		
		public void setFrameRotation(int frameRotation) {
			this.frameRotation = frameRotation;
		}
		
		public void setStreamIndex(int streamIndex) {
			this.streamIndex = streamIndex;
		}
		
		public void setKeyFrame(boolean isKeyFrame) {
			this.keyFrame = isKeyFrame;
		}
		
		public ByteBuffer getEncodedVideoFrame() {
			return encodedVideoFrame;
		}
		
		public long getDts() {
			return dts;
		}
		public long getPts() {
			return pts;
		}
		public long getFirstFrameTimeStamp() {
			return firstFrameTimeStamp;
		}
		
		public int getFrameRotation() {
			return frameRotation;
		}
		public int getStreamIndex() {
			return streamIndex;
		}
		
		public boolean isKeyFrame() {
			return keyFrame;
		}
		
		public long getOriginalFrameTimeMs() {
			return originalFrameTimeMs;
		}
		
	}
	/**
	 * By default first video key frame are not checked, so it's true.
	 *
	 * If the first video key frame should be checked, make this setting to false. It's being used in RecordMuxer and HLSMuxer
	 */
	protected boolean firstKeyFrameReceived = true;
	private long lastPts;

	protected AVDictionary optionDictionary = new AVDictionary(null);

	private long firstPacketDtsMs = -1;

	protected Muxer(Vertx vertx) {
		this.vertx = vertx;
		logger = LoggerFactory.getLogger(this.getClass());
	}

	public static File getPreviewFile(IScope scope, String name, String extension) {
		String appScopeName = ScopeUtils.findApplication(scope).getName();
		return new File(String.format("%s/webapps/%s/%s", System.getProperty("red5.root"), appScopeName,
				"previews/" + name + extension));
	}
	public static File getRecordFile(IScope scope, String name, String extension, String subFolder) 
	{
		// get stream filename generator
		IStreamFilenameGenerator generator = (IStreamFilenameGenerator) ScopeUtils.getScopeService(scope,
				IStreamFilenameGenerator.class, DefaultStreamFilenameGenerator.class);
		// generate filename
		String fileName = generator.generateFilename(scope, name, extension, GenerationType.RECORD, subFolder);
		File file = null;
		if (generator.resolvesToAbsolutePath()) {
			file = new File(fileName);
		} else {
			Resource resource = scope.getContext().getResource(fileName);
			if (resource.exists()) {
				try {
					file = resource.getFile();
					loggerStatic.debug("File exists: {} writable: {}", file.exists(), file.canWrite());
				} catch (IOException ioe) {
					loggerStatic.error("File error: {}", ExceptionUtils.getStackTrace(ioe));
				}
			} else {
				String appScopeName = ScopeUtils.findApplication(scope).getName();
				file = new File(
						String.format("%s/webapps/%s/%s", System.getProperty("red5.root"), appScopeName, fileName));
			}
		}
		return file;
	}

	public static File getUserRecordFile(IScope scope, String userVoDFolder, String name) {
		String appScopeName = ScopeUtils.findApplication(scope).getName();
		return new File(String.format("%s/webapps/%s/%s", System.getProperty("red5.root"), appScopeName,
				"streams/" + userVoDFolder + "/" + name ));
	}

	/**
	 * Add a new stream with this codec, codecContext and stream Index
	 * parameters. After adding streams, need to call prepareIO()
	 *
	 * This method is called by encoder. After encoder is opened, it adds codec context to the muxer
	 *
	 * @param codec
	 * @param codecContext
	 * @param streamIndex
	 * @return
	 */	
	public synchronized boolean addStream(AVCodec codec, AVCodecContext codecContext, int streamIndex) {

		AVCodecParameters codecParameter = new AVCodecParameters();
		int ret = avcodec_parameters_from_context(codecParameter, codecContext);
		if (ret < 0) {
			logger.error("Cannot get codec parameters for {}", streamId);
			return false;
		}
		return addStream(codecParameter, codecContext.time_base(), streamIndex);
	}

	public String getOutputURL() {
		return file.getAbsolutePath();
	}


	public boolean openIO() {
	

		if ((getOutputFormatContext().oformat().flags() & AVFMT_NOFILE) == 0)
		{
			//if it's different from zero, it means no file is need to be open.
			//If it's zero, Not "no file" and it means that file is need to be open .
			String url =  getOutputURL();
			AVIOContext pb = new AVIOContext(null);

			int ret = avformat.avio_open2(pb, url , AVIO_FLAG_WRITE, null, getOptionDictionary());
			if (ret < 0) {
				logger.warn("Could not open output url: {} ",  url);
				return false;
			}
			getOutputFormatContext().pb(pb);
		}
		return true;
	}

	/**
	 * This function may be called by multiple encoders. Make sure that it is
	 * called once.
	 *
	 * See the sample implementations how it is being protected
	 *
	 * Implement this function with synchronized keyword as the subclass
	 *
	 * @return
	 */
	public synchronized boolean prepareIO() {


		/**
		 * We need to extract addedStream information in some cases because we treat audio and video separate
		 * In addStream for example, if we don't check this we end up removing the muxer completely if one of the operations fail.
		 */
		if (isRunning.get()) {
			logger.warn("Muxer is already running for stream: {} so it's not preparing io again and returning", streamId);
			return false;
		}

		boolean result = false;

		if (openIO()) 
		{
			result = writeHeader();
		}
		return result;
	}

	public boolean writeHeader() 
	{
		AVDictionary optionsDictionary = null;

		if (!options.isEmpty()) {
			optionsDictionary = new AVDictionary();
			Set<String> keySet = options.keySet();
			for (String key : keySet) {
				av_dict_set(optionsDictionary, key, options.get(key), 0);
			}

		}	
			

		int ret = avformat_write_header(getOutputFormatContext(), optionsDictionary);		
		if (ret < 0) {
			if (logger.isWarnEnabled()) 	{
				logger.warn("Could not write header. File: {} Error: {}", file.getAbsolutePath(), getErrorDefinition(ret));
			}
			clearResource();
			return false;
		}
		else {
			logger.info("Header is written for stream:{} and url:{}", streamId, getOutputURL());
		}
		

		if (optionsDictionary != null) {
			av_dict_free(optionsDictionary);
		}
		isRunning.set(true);
		headerWritten = true;
		return true;
	}

	/**
	 * This function may be called by multiple encoders. Make sure that it is
	 * called once.
	 *
	 * See the sample implementations how it is being protected
	 *
	 * Implement this function with synchronized keyword as the subclass
	 *
	 * @return
	 */
	public synchronized void writeTrailer() {
		if (!isRunning.get() || outputFormatContext == null) {
			//return if it is already null
			logger.warn("OutputFormatContext is not initialized or it is freed for stream: {}", streamId);
			return;
		}

		logger.info("writing trailer for stream: {}", streamId);
		isRunning.set(false);

		av_write_trailer(outputFormatContext);

		clearResource();
	
	}

	protected synchronized void clearResource() {
		if (tmpPacket != null) {
			av_packet_free(tmpPacket);
			tmpPacket = null;
		}

		if (videoPkt != null) {
			av_packet_free(videoPkt);
			videoPkt = null;
		}

		if (audioPkt != null) {
			av_packet_free(audioPkt);
			audioPkt = null;
		}

		if (videoBsfFilterContext != null) {
			av_bsf_free(videoBsfFilterContext);
			videoBsfFilterContext = null;
		}

		/* close output */
		if (outputFormatContext != null &&
				(outputFormatContext.oformat().flags() & AVFMT_NOFILE) == 0 
						&& outputFormatContext.pb() != null
						&& (outputFormatContext.flags() & AVFormatContext.AVFMT_FLAG_CUSTOM_IO) == 0)
		{
			avio_closep(outputFormatContext.pb());
		}

		if (outputFormatContext != null) {
			avformat_free_context(outputFormatContext);
			outputFormatContext = null;
		}
		av_dict_free(optionDictionary);
	}

	/**
	 * Write packets to the output. This function is used in by MuxerAdaptor
	 * which is in community edition
	 *
	 * Check if outputContext.pb is not null for the ffmpeg base Muxers
	 *
	 * Implement this function with synchronized keyword as the subclass
	 *
	 * @param pkt
	 *            The content of the data as a AVPacket object
	 */
	public synchronized void writePacket(AVPacket pkt, AVStream stream) {

		if (checkToDropPacket(pkt, stream.codecpar().codec_type())) {
			//drop packet 
			return;
		}

		if (!isRunning.get() || !registeredStreamIndexList.contains(pkt.stream_index())) 
		{
			logPacketIssue("Not writing packet1 for {} - Is running:{} or stream index({}) is registered: {}", streamId, isRunning.get(), pkt.stream_index(), registeredStreamIndexList.contains(pkt.stream_index()));
			return;
		}

		int inputStreamIndex = pkt.stream_index();
		int outputStreamIndex = inputOutputStreamIndexMap.get(inputStreamIndex);
		AVStream outStream = outputFormatContext.streams(outputStreamIndex);

		pkt.stream_index(outputStreamIndex);

		writePacket(pkt, inputTimeBaseMap.get(inputStreamIndex),  outStream.time_base(), outStream.codecpar().codec_type());

		pkt.stream_index(inputStreamIndex);
	}
	
	public void logPacketIssue(String format, Object... arguments) {
		if (time2log % 200 == 0) {
			logger.warn(format, arguments);
			time2log = 0;
		}
		time2log++;
	}


	/**
	 * Write packets to the output. This function is used in transcoding.
	 * Previously, It's the replacement of {link {@link #writePacket(AVPacket)}
	 * @param avpacket
	 * @param codecContext
	 */
	public synchronized void writePacket(AVPacket pkt, AVCodecContext codecContext) {
		if (!isRunning.get() || !registeredStreamIndexList.contains(pkt.stream_index())) {
			logPacketIssue("Not writing packet for {} - Is running:{} or stream index({}) is registered: {}", streamId, isRunning.get(), pkt.stream_index(), registeredStreamIndexList.contains(pkt.stream_index()));
			return;
		}
		int inputStreamIndex = pkt.stream_index();
		int outputStreamIndex = inputOutputStreamIndexMap.get(inputStreamIndex);
		AVStream outStream = outputFormatContext.streams(outputStreamIndex);

		AVRational codecTimebase = inputTimeBaseMap.get(inputStreamIndex);
		int codecType = outStream.codecpar().codec_type();

		if (!checkToDropPacket(pkt, codecType)) {
			//added for audio video sync
			writePacket(pkt, codecTimebase,  outStream.time_base(), codecType);
		}

	}

	public ByteBuffer getPacketBufferWithExtradata(byte[] extradata, AVPacket pkt){

		ByteBuffer	byteBuffer = ByteBuffer.allocateDirect(extradata.length + pkt.size());
		byteBuffer.put(extradata);

		if (pkt.size() > 0) {
			logger.debug("Adding extradata to record muxer packet pkt size:{}", pkt.size());
			byteBuffer.put(pkt.data().position(0).limit(pkt.size()).asByteBuffer());
		}

		return byteBuffer;
	}


	public void setBitstreamFilter(String bsfName) {
		this.bsfVideoName = bsfName;
	}
	
	public String getBitStreamFilter() {
		return bsfVideoName;
	}

	public File getFile() {
		return file;
	}

	public String getFileName() {
		if (file != null) {
			return file.getName();
		}
		return null;
	}

	public String getFormat() {
		return format;
	}

	/**
	 * Inits the file to write. Multiple encoders can init the muxer. It is
	 * redundant to init multiple times.
	 */
	public void init(IScope scope, String name, int resolution, String subFolder, int videoBitrate) {
		this.streamId = name;
		init(scope, name, resolution, true, subFolder, videoBitrate);
	}

	/**
	 * Init file name
	 *
	 * file format is NAME[-{DATETIME}][_{RESOLUTION_HEIGHT}p_{BITRATE}kbps].{EXTENSION}
	 *
	 * Datetime format is yyyy-MM-dd_HH-mm
	 *
	 * We are using "-" instead of ":" in HH:mm -> Stream filename must not contain ":" character.
	 *
	 * sample naming -> stream1-yyyy-MM-dd_HH-mm_480p_500kbps.mp4 if datetime is added
	 * stream1_480p.mp4 if no datetime
	 *
	 * @param scope
	 * @param name,
	 *            name of the stream
	 * @param resolution
	 *            height of the stream, if it is zero, then no resolution will
	 *            be added to resource name
	 * @param overrideIfExist
	 *            whether override if a file exists with the same name
	 * @param bitrate
	 * 			  bitrate of the stream, if it is zero, no bitrate will
	 * 			  be added to resource name
	 */
	public void init(IScope scope, final String name, int resolution, boolean overrideIfExist, String subFolder, int bitrate) {
		if (!isInitialized) {
			isInitialized = true;
			this.scope = scope;
			this.resolution = resolution;

			//Refactor: Getting AppSettings smells here
			AppSettings appSettings = getAppSettings();

			initialResourceNameWithoutExtension = getExtendedName(name, resolution, bitrate, appSettings.getFileNameFormat());


			file = getResourceFile(scope, initialResourceNameWithoutExtension, extension, subFolder);

			File parentFile = file.getParentFile();

			if (!parentFile.exists()) {
				// check if parent file does not exist
				parentFile.mkdirs();
			} else {
				// if parent file exists,
				// check overrideIfExist and file.exists
				File tempFile = getResourceFile(scope, initialResourceNameWithoutExtension, extension+TEMP_EXTENSION, subFolder);

				if (!overrideIfExist && (file.exists() || tempFile.exists())) {
					String tmpName = initialResourceNameWithoutExtension;
					int i = 1;
					do {
						tempFile = getResourceFile(scope, tmpName, extension+TEMP_EXTENSION, subFolder);
						file = getResourceFile(scope, tmpName, extension, subFolder);
						tmpName = initialResourceNameWithoutExtension + "_" + i;
						i++;
					} while (file.exists() || tempFile.exists());
				}
			}

			audioPkt = avcodec.av_packet_alloc();
			av_init_packet(audioPkt);

			videoPkt = avcodec.av_packet_alloc();
			av_init_packet(videoPkt);

			tmpPacket = avcodec.av_packet_alloc();
			av_init_packet(tmpPacket);

		}
	}

	public AppSettings getAppSettings() {
		IContext context = this.scope.getContext();
		ApplicationContext appCtx = context.getApplicationContext();
		return (AppSettings) appCtx.getBean(AppSettings.BEAN_NAME);
	}
	
	public String getExtendedName(String name, int resolution, int bitrate, String fileNameFormat){
		// set default name
		String resourceName = name;
		int bitrateKbps = bitrate / 1000;
		
		// added before the if statement because of if addDateTimeToResourceName parameter return false, currentVoDTimeStamp returns 0
		LocalDateTime ldt =  LocalDateTime.now();
		currentVoDTimeStamp = ldt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

		// add date time parameter to resource name if it is set
		if (addDateTimeToResourceName) 
		{
			resourceName = name + "-" + ldt.format(DateTimeFormatter.ofPattern(DATE_TIME_PATTERN));
			if (logger.isInfoEnabled()) {
				logger.info("Date time resource name: {} local date time: {}", resourceName, ldt.format(DateTimeFormatter.ofPattern(DATE_TIME_PATTERN)));
			}
		}
		String lowerCaseFormat = fileNameFormat.toLowerCase();
		// add resolution height parameter if it is different than 0
		if (resolution != 0 && lowerCaseFormat.contains("%r") && bitrateKbps != 0 && lowerCaseFormat.contains("%b"))
		{
			resourceName += (lowerCaseFormat.indexOf("r") > lowerCaseFormat.indexOf("b")) ?  "_" + bitrateKbps + "kbps" + resolution + "p" : "_" + resolution + "p" + bitrateKbps + "kbps";
		}
		else if(resolution != 0 && lowerCaseFormat.contains("%r") && (bitrateKbps == 0 || !lowerCaseFormat.contains("%b"))){
			resourceName += "_" + resolution + "p" ;
		}
		else if((resolution == 0 || !lowerCaseFormat.contains("%r")) && bitrateKbps != 0 && lowerCaseFormat.contains("%b")){
			resourceName += "_" + bitrateKbps + "kbps" ;
		}
		else if( (!lowerCaseFormat.contains("%r") && !lowerCaseFormat.contains("%b")) && resolution != 0){
			logger.info("No identifier found for file name, adding resolution");
			resourceName += "_" + resolution + "p" ;
			
		}
		return resourceName;
	}

	public File getResourceFile(IScope scope, String name, String extension, String subFolder) {
		return getRecordFile(scope, name, extension, subFolder);
	}

	public boolean isAddDateTimeToSourceName() {
		return addDateTimeToResourceName;
	}

	public void setAddDateTimeToSourceName(boolean addDateTimeToSourceName) {
		this.addDateTimeToResourceName = addDateTimeToSourceName;
	}

	/**
	 * Add video stream to the muxer with direct parameters. 
	 * 
	 * This method is called when there is a WebRTC ingest and there is no adaptive streaming
	 *
	 * @param width, video width
	 * @param height, video height
	 * @param codecId, codec id of the stream
	 * @param streamIndex, stream index
	 * @param isAVC, true if packets are in AVC format, false if in annexb format
	 * @return true if successful,
	 * false if failed
	 */
	public synchronized boolean addVideoStream(int width, int height, AVRational timebase, int codecId, int streamIndex,
			boolean isAVC, AVCodecParameters codecpar) {
		boolean result = false;
		AVFormatContext outputContext = getOutputFormatContext();
		if (outputContext != null && isCodecSupported(codecId) && !isRunning.get())
		{
			registeredStreamIndexList.add(streamIndex);
			AVStream outStream = avformat_new_stream(outputContext, null);
			outStream.codecpar().width(width);
			outStream.codecpar().height(height);
			outStream.codecpar().codec_id(codecId);
			outStream.codecpar().codec_type(AVMEDIA_TYPE_VIDEO);
			outStream.codecpar().format(AV_PIX_FMT_YUV420P);
			outStream.codecpar().codec_tag(0);

			AVRational timeBase = new AVRational();
			timeBase.num(1).den(1000);
			inputTimeBaseMap.put(streamIndex, timeBase);
			inputOutputStreamIndexMap.put(streamIndex, outStream.index());
			videoWidth = width;
			videoHeight = height;
			result = true;
		}
		return result;
	}

	/**
	 * Add audio stream to the muxer. 
	 * @param sampleRate
	 * @param channelLayout
	 * @param codecId
	 * @param streamIndex, is the stream index of source
	 * @return
	 */
	public synchronized boolean addAudioStream(int sampleRate, AVChannelLayout channelLayout, int codecId, int streamIndex) {
		boolean result = false;
		AVFormatContext outputContext = getOutputFormatContext();
		if (outputContext != null && isCodecSupported(codecId))
		{
			registeredStreamIndexList.add(streamIndex);
			AVStream outStream = avformat_new_stream(outputContext, null);
			outStream.codecpar().sample_rate(sampleRate);
			outStream.codecpar().ch_layout(channelLayout);
			outStream.codecpar().codec_id(codecId);
			outStream.codecpar().codec_type(AVMEDIA_TYPE_AUDIO);
			outStream.codecpar().codec_tag(0);

			AVRational timeBase = new AVRational();
			////////////////////////
			//TODO: This is a workaround solution. Adding sampleRate as timebase may not be correct. This method is only called by OpusForwarder
			/////////////////////////

			//update about the workaround solution: We need to set the samplerate as timebase because 
			// audio timestamp is coming with the sample rate scale from webrtc side
			timeBase.num(1).den(sampleRate);
			inputTimeBaseMap.put(streamIndex, timeBase);
			inputOutputStreamIndexMap.put(streamIndex, outStream.index());
			result = true;
		}

		return result;
	}

	public AVStream avNewStream(AVFormatContext context) {
		return avformat_new_stream(context, null);
	}

	/**
	 * Add stream to the muxer. This method is called by direct muxing. 
	 * For instance from RTMP, SRT ingest & Stream Pull 
	 * 	to HLS, MP4, HLS, DASH WebRTC Muxing
	 * 
	 * @param codecParameters
	 * @param timebase
	 * @param streamIndex, is the stream index of the source. Sometimes source and target stream index do not match
	 * @return
	 */
	public synchronized boolean addStream(AVCodecParameters codecParameters, AVRational timebase, int streamIndex) 
	{
		if (isRunning.get()) {
			logger.warn("It is already running and cannot add new stream while it's running for stream:{} and output:{}", streamId, getOutputURL());
			return false;
		}
		boolean result = false;
		AVFormatContext outputContext = getOutputFormatContext();
		if (outputContext != null 
				&& isCodecSupported(codecParameters.codec_id()) &&
				(codecParameters.codec_type() == AVMEDIA_TYPE_AUDIO || codecParameters.codec_type() == AVMEDIA_TYPE_VIDEO)
				)
		{

			
			AVStream outStream = avNewStream(outputContext);
			//if it's not running add to the list
			registeredStreamIndexList.add(streamIndex);

			if (bsfVideoName != null && codecParameters.codec_type() == AVMEDIA_TYPE_VIDEO) 
			{
				AVBSFContext videoBitstreamFilter = initVideoBitstreamFilter(codecParameters, timebase);
				if (videoBitstreamFilter != null) 
				{
					codecParameters = videoBitstreamFilter.par_out();
					timebase = videoBitstreamFilter.time_base_out();
				}

			}
			String codecType = "audio";
			if (codecParameters.codec_type() == AVMEDIA_TYPE_VIDEO) 
			{
				codecType = "video";
				videoWidth = codecParameters.width();
				videoHeight = codecParameters.height();
			}

			avcodec_parameters_copy(outStream.codecpar(), codecParameters);
			logger.info("Adding timebase to the input time base map index:{} value: {}/{} for stream:{} type:{}", 
					outStream.index(), timebase.num(), timebase.den(), streamId, codecType);
			inputTimeBaseMap.put(streamIndex, timebase);
			inputOutputStreamIndexMap.put(streamIndex, outStream.index());

			outStream.codecpar().codec_tag(0);
			result = true;

		}
		else if (codecParameters.codec_type() == AVMEDIA_TYPE_DATA) 
		{
			if(codecParameters.codec_id() == AV_CODEC_ID_TIMED_ID3) 
			{
				AVStream outStream = avNewStream(outputContext);
				registeredStreamIndexList.add(streamIndex);

				avcodec_parameters_copy(outStream.codecpar(), codecParameters);
				logger.info("Adding ID3 stream timebase to the input time base map index:{} value: {}/{} for stream:{}",
						outStream.index(), timebase.num(), timebase.den(), streamId);
				inputTimeBaseMap.put(streamIndex, timebase);
				inputOutputStreamIndexMap.put(streamIndex, outStream.index());
			}
			//if it's data, do not add and return true
			result = true;
		}
		else {
			logger.warn("Stream is not added for muxing to {} for stream:{}", getFileName(), streamId);
		}
		return result;
	}

	public AVBSFContext initVideoBitstreamFilter(AVCodecParameters codecParameters, AVRational timebase) {
		AVBitStreamFilter bsfilter = av_bsf_get_by_name(bsfVideoName);
		videoBsfFilterContext = new AVBSFContext(null);
		int ret = av_bsf_alloc(bsfilter, videoBsfFilterContext);

		if (ret < 0) {
			logger.info("cannot allocate bsf context for {}", getOutputURL());
			return null;
		}

		ret = avcodec_parameters_copy(videoBsfFilterContext.par_in(), codecParameters);
		if (ret < 0) {
			logger.info("cannot copy input codec parameters for {}", getOutputURL());
			return null;
		}

		videoBsfFilterContext.time_base_in(timebase);
		ret = av_bsf_init(videoBsfFilterContext);
		if (ret < 0) {
			logger.info("cannot init bit stream filter context for {}", getOutputURL());
			return null;
		}

		return videoBsfFilterContext;
	}

	public synchronized void writeVideoBuffer(ByteBuffer encodedVideoFrame, long dts, int frameRotation, int streamIndex,boolean isKeyFrame,long firstFrameTimeStamp, long pts) {
		VideoBuffer videoBuffer = new VideoBuffer();
		videoBuffer.setEncodedVideoFrame(encodedVideoFrame);
		videoBuffer.setTimeStamps(dts, pts, firstFrameTimeStamp, pts);
		videoBuffer.setFrameRotation(frameRotation);
		videoBuffer.setStreamIndex(streamIndex);
		videoBuffer.setKeyFrame(isKeyFrame);
		writeVideoBuffer(videoBuffer);
	}
	

	public synchronized void writeVideoBuffer(VideoBuffer buffer) {
		/*
		 * this control is necessary to prevent server from a native crash
		 * in case of initiation and preparation takes long.
		 * because native objects like videoPkt can not be initiated yet
		 */
		if (!isRunning.get()) {
			logPacketIssue("Not writing VideoBuffer for {} because Is running:{}", streamId, isRunning.get());
			return;
		}

		
		/*
		 * Rotation field is used add metadata to the mp4.
		 * this method is called in directly creating mp4 from coming encoded WebRTC H264 stream
		 */
		this.rotation = buffer.getFrameRotation();
		videoPkt.stream_index(buffer.getStreamIndex());
		videoPkt.pts(buffer.getPts());
		videoPkt.dts(buffer.getDts());
		if(buffer.isKeyFrame()) {
			videoPkt.flags(videoPkt.flags() | AV_PKT_FLAG_KEY);
		}

		buffer.getEncodedVideoFrame().rewind();
		videoPkt.data(new BytePointer(buffer.getEncodedVideoFrame()));
		videoPkt.size(buffer.getEncodedVideoFrame().limit());
		videoPkt.position(0);
		writePacket(videoPkt, (AVCodecContext)null);

		av_packet_unref(videoPkt);
	}

	public synchronized void writeAudioBuffer(ByteBuffer audioFrame, int streamIndex, long timestamp) {
		if (!isRunning.get()) {
			logPacketIssue("Not writing AudioBuffer for {} because Is running:{}", streamId, isRunning.get());
			return;
		}

		audioPkt.stream_index(streamIndex);
		audioPkt.pts(timestamp);
		audioPkt.dts(timestamp);
		audioFrame.rewind();
		audioPkt.flags(audioPkt.flags() | AV_PKT_FLAG_KEY);
		audioPkt.data(new BytePointer(audioFrame));
		audioPkt.size(audioFrame.limit());
		audioPkt.position(0);

		writePacket(audioPkt, (AVCodecContext)null);

		av_packet_unref(audioPkt);

	}

	public List<Integer> getRegisteredStreamIndexList() {
		return registeredStreamIndexList;
	}

	public void setIsRunning(AtomicBoolean isRunning) {
		this.isRunning = isRunning;
	}

	public void setOption(String optionName,String value){
		av_dict_set(optionDictionary, optionName, value, 0);
	}
	public AVDictionary getOptionDictionary(){
		return optionDictionary;
	}
	public abstract boolean isCodecSupported(int codecId);

	public abstract AVFormatContext getOutputFormatContext();

	/**
	 * Return decision about dropping packet or not
	 * 
	 * @param pkt
	 * @param codecType
	 * @return true to drop the packet, false to not drop packet
	 */
	public boolean checkToDropPacket(AVPacket pkt, int codecType) {
		if (!firstKeyFrameReceived && codecType == AVMEDIA_TYPE_VIDEO) 
		{
			if(firstPacketDtsMs == -1) {
				firstVideoDts = pkt.dts();
				firstPacketDtsMs  = av_rescale_q(pkt.dts(), inputTimeBaseMap.get(pkt.stream_index()), MuxAdaptor.TIME_BASE_FOR_MS);
			}
			else 
			if (firstVideoDts == -1) {
				firstVideoDts = av_rescale_q(firstPacketDtsMs, MuxAdaptor.TIME_BASE_FOR_MS, inputTimeBaseMap.get(pkt.stream_index()));
				if ((pkt.dts() - firstVideoDts) < 0) {
					firstVideoDts = pkt.dts();
				}
			}

			int keyFrame = pkt.flags() & AV_PKT_FLAG_KEY;
			//we set start time here because we start recording with key frame and drop the other
			//setting here improves synch between audio and video
			if (keyFrame == 1) {
				firstKeyFrameReceived = true;
				logger.warn("First key frame received for stream: {}", streamId);
			} else {
				logger.info("First video packet is not key frame. It will drop for direct muxing. Stream {}", streamId);
				// return if firstKeyFrameReceived is not received
				// below return is important otherwise it does not work with like some encoders(vidiu)
				return true;

			}
		}
		//don't drop packet because it's either audio packet or key frame is received
		return false;
	}



	public int getVideoWidth() {
		return videoWidth;
	}

	public int getVideoHeight() {
		return videoHeight;
	}

	/**
	 * All other writePacket functions call this function to make the job
	 *
	 * @param pkt
	 * Content of the data in AVPacket class
	 *
	 * @param inputTimebase
	 * input time base is required to calculate the correct dts and pts values for the container
	 *
	 * @param outputTimebase
	 * output time base is required to calculate the correct dts and pts values for the container
	 */
	protected synchronized void writePacket(AVPacket pkt, AVRational inputTimebase, AVRational outputTimebase, int codecType)
	{
		AVFormatContext context = getOutputFormatContext();

		long pts = pkt.pts();
		long dts = pkt.dts();
		long duration = pkt.duration();
		long pos = pkt.pos();

		pkt.duration(av_rescale_q(pkt.duration(), inputTimebase, outputTimebase));
		pkt.pos(-1);

		if (codecType == AVMEDIA_TYPE_AUDIO)
		{
			//removing firstAudioDTS is required when recording/muxing has started on the fly
			if(firstPacketDtsMs == -1) {
				firstAudioDts = pkt.dts();
				firstPacketDtsMs  = av_rescale_q(pkt.dts(), inputTimeBaseMap.get(pkt.stream_index()), MuxAdaptor.TIME_BASE_FOR_MS);
				logger.debug("The first incoming packet is audio and its packet dts:{}ms streamId:{} ", firstPacketDtsMs, streamId);
			}
			else 
			if (firstAudioDts == -1) {
				firstAudioDts = av_rescale_q(firstPacketDtsMs, MuxAdaptor.TIME_BASE_FOR_MS, inputTimeBaseMap.get(pkt.stream_index()));
				logger.debug("First packetDtsMs:{}ms is already received calculated the firstAudioDts:{} and incoming packet dts:{} streamId:{}", 
								firstPacketDtsMs, firstAudioDts, pkt.dts(), streamId);
				
				if ((pkt.dts() - firstAudioDts) < 0) {
					firstAudioDts = pkt.dts();
				}
			}
			
			pkt.pts(av_rescale_q_rnd(pkt.pts() - firstAudioDts, inputTimebase, outputTimebase, AV_ROUND_NEAR_INF|AV_ROUND_PASS_MINMAX));
			pkt.dts(av_rescale_q_rnd(pkt.dts() - firstAudioDts , inputTimebase, outputTimebase, AV_ROUND_NEAR_INF|AV_ROUND_PASS_MINMAX));


			int ret = av_packet_ref(tmpPacket , pkt);
			if (ret < 0) {
				logger.error("Cannot copy audio packet for {}", streamId);
				return;
			}
			writeAudioFrame(tmpPacket, inputTimebase, outputTimebase, context, dts);

			av_packet_unref(tmpPacket);
		}
		else if (codecType == AVMEDIA_TYPE_VIDEO)
		{
			//removing firstVideoDts is required when recording/muxing has started on the fly
			pkt.pts(av_rescale_q_rnd(pkt.pts() - firstVideoDts , inputTimebase, outputTimebase, AV_ROUND_NEAR_INF|AV_ROUND_PASS_MINMAX));
			pkt.dts(av_rescale_q_rnd(pkt.dts() - firstVideoDts, inputTimebase, outputTimebase, AV_ROUND_NEAR_INF|AV_ROUND_PASS_MINMAX));

			//we set the firstVideoDts in checkToDropPacket Method to not have audio/video synch issue

			// we don't set startTimeInVideoTimebase here because we only start with key frame and we drop all frames
			// until the first key frame
			boolean isKeyFrame = false;
			if ((pkt.flags() & AV_PKT_FLAG_KEY) == 1) {
				isKeyFrame = true;
			}

			int ret = av_packet_ref(tmpPacket , pkt);
			if (ret < 0) {
				logger.error("Cannot copy video packet for {}", streamId);
				return;
			}			
			/*
			 * We add this check because when encoder calls this method the packet needs extra data inside
			 * However, SFUForwarder calls writeVideoBuffer and the method packets itself there
			 * To prevent memory issues and crashes we don't repacket if the packet is ready to use from SFU forwarder
			 */
			addExtradataIfRequired(pkt, isKeyFrame);

			lastPts = tmpPacket.pts();

			writeVideoFrame(tmpPacket, context);
			av_packet_unref(tmpPacket);
		}
		else {
			//for any other stream like subtitle, etc.
			pkt.pts(av_rescale_q_rnd(pkt.pts(), inputTimebase, outputTimebase, AV_ROUND_NEAR_INF|AV_ROUND_PASS_MINMAX));
			pkt.dts(av_rescale_q_rnd(pkt.dts(), inputTimebase, outputTimebase, AV_ROUND_NEAR_INF|AV_ROUND_PASS_MINMAX));

			writeDataFrame(pkt, context);
		}

		pkt.pts(pts);
		pkt.dts(dts);
		pkt.duration(duration);
		pkt.pos(pos);

	}

	public void writeDataFrame(AVPacket pkt, AVFormatContext context) {
		int ret = av_packet_ref(tmpPacket , pkt);
		if (ret < 0) {
			logger.error("Cannot copy data packet for {}", streamId);
			return;
		}

		ret = av_write_frame(context, tmpPacket);

		if (ret < 0 && logger.isWarnEnabled()) {
			logPacketIssue("cannot frame to muxer({}) not audio and not video. Error is {} ", file.getName(), getErrorDefinition(ret));
		}
		av_packet_unref(tmpPacket);
	}

	public void addExtradataIfRequired(AVPacket pkt, boolean isKeyFrame) 
	{
		if(videoExtradata != null && videoExtradata.length > 0 && isKeyFrame) 
		{
			ByteBuffer byteBuffer = getPacketBufferWithExtradata(videoExtradata, pkt);

			byteBuffer.position(0);

			//Started to manually packet the frames because we want to add the extra data.
			tmpPacket.data(new BytePointer(byteBuffer));
			tmpPacket.size(byteBuffer.limit());
		}
	}

	protected void writeVideoFrame(AVPacket pkt, AVFormatContext context) {
		int ret;
		
		
		if (videoBsfFilterContext != null) 
		{
			ret = av_bsf_send_packet(videoBsfFilterContext, pkt);
			if (ret < 0) {
				logger.warn("Cannot send packet to bit stream filter for stream:{}", streamId);
				return;
			}
			while (av_bsf_receive_packet(videoBsfFilterContext, pkt) == 0)
			{
				logger.trace("write video packet pts:{} dts:{}", pkt.pts(), pkt.dts());
				ret = av_write_frame(context, tmpPacket);
				if (ret < 0 && logger.isWarnEnabled()) {
					logger.warn("cannot write video frame to muxer({}) av_bsf_receive_packet. Error is {} ", file.getName(), getErrorDefinition(ret));
				}
			}
		}
		else 
		{
			logger.trace("write video packet pts:{} dts:{}", pkt.pts(), pkt.dts());
			ret = av_write_frame(context, pkt);
			if (ret < 0 && logger.isWarnEnabled()) {
				//TODO: this is written for some muxers like HLS because normalized video time is coming from WebRTC
				//WebRTCVideoForwarder#getVideoTime. Fix this problem when upgrading the webrtc stack
				logger.warn("cannot write video frame to muxer({}). Pts: {} dts:{}  Error is {} ", file.getName(), pkt.pts(), pkt.dts(), getErrorDefinition(ret));
			}
		}
	}

	protected void writeAudioFrame(AVPacket pkt, AVRational inputTimebase, AVRational outputTimebase,
			AVFormatContext context, long dts) {
		int ret;
		logger.trace("write audio packet pts:{} dts:{}", pkt.pts(), pkt.dts());
		ret = av_write_frame(context, tmpPacket);
		if (ret < 0 && logger.isInfoEnabled()) {
			logger.info("cannot write audio frame to muxer({}). Error is {} ", file.getName(), getErrorDefinition(ret));
		}
	}
	
	public static long getDurationInMs(File f, String streamId) {
		return getDurationInMs(f.getAbsolutePath(), streamId);
	}
	

	public static long getDurationInMs(String url, String streamId) {
		AVFormatContext inputFormatContext = avformat.avformat_alloc_context();
		int ret;
		if (streamId != null) {
			streamId = streamId.replaceAll("[\n\r\t]", "_");
		}
		if (avformat_open_input(inputFormatContext, url, null, (AVDictionary)null) < 0) 
		{
			loggerStatic.info("cannot open input context for duration for stream: {} for file:{}", streamId, url);
			avformat_close_input(inputFormatContext);
			return -1L;
		}

		ret = avformat_find_stream_info(inputFormatContext, (AVDictionary)null);
		if (ret < 0) {
			loggerStatic.info("Could not find stream information for stream: {} for file:{}", streamId, url);
			avformat_close_input(inputFormatContext);
			return -1L;
		}
		long durationInMS = -1;
		if (inputFormatContext.duration() != AV_NOPTS_VALUE)
		{
			durationInMS = inputFormatContext.duration() / 1000;
		}
		avformat_close_input(inputFormatContext);
		return durationInMS;
	}

	public static String getErrorDefinition(int errorCode) {
		byte[] data = new byte[128];
		av_strerror(errorCode, data, data.length);
		return FFmpegUtilities.byteArrayToString(data);
	}

	/**
	 * This is called when the current context will change/deleted soon. 
	 * It's called by encoder and likely due to aspect ratio change
	 * 
	 * After this method has been called, this method {@link Muxer#contextChanged(AVCodecContext, int)}
	 * should be called
	 * @param codecContext the current context that will be changed/deleted soon 
	 * 
	 * @param streamIndex
	 */
	public synchronized void contextWillChange(AVCodecContext codecContext, int streamIndex) {
		
	}
	
	/**
	 * It's called when the codecContext for the stream index has changed.
	 * 
	 * {@link Muxer#contextWillChange(AVCodecContext, int)} is called before this method is called.
	 * 
	 * @param codecContext
	 * @param streamIndex
	 */
	public synchronized void contextChanged(AVCodecContext codecContext, int streamIndex) {
		
		if (codecContext.codec_type() == AVMEDIA_TYPE_VIDEO) 
		{
			videoWidth = codecContext.width();
			videoHeight = codecContext.height();
			
			videoExtradata = new byte[codecContext.extradata_size()];

			if(videoExtradata.length > 0) 
			{
				BytePointer extraDataPointer = codecContext.extradata();
				extraDataPointer.get(videoExtradata).close();
				extraDataPointer.close();
				logger.info("extra data 0: {}  1: {}, 2:{}, 3:{}, 4:{}", videoExtradata[0], videoExtradata[1], videoExtradata[2], videoExtradata[3], videoExtradata[4]);
			}
		}
		
		
		inputTimeBaseMap.put(streamIndex, codecContext.time_base());
		
	}
	
	public Map<Integer, AVRational> getInputTimeBaseMap() {
		return inputTimeBaseMap;
	}

	public AVPacket getTmpPacket() {
		return tmpPacket;
	}
	
	public AtomicBoolean getIsRunning() {
		return isRunning;
	}
	
	public long getCurrentVoDTimeStamp() {
		return currentVoDTimeStamp;
	}

	public void setCurrentVoDTimeStamp(long currentVoDTimeStamp) {
		this.currentVoDTimeStamp = currentVoDTimeStamp;
	}

	public int getResolution() {
		return resolution;
	}

	public long getLastPts() {
		return lastPts;
	}
	
	public static String replaceDoubleSlashesWithSingleSlash(String url) {
		return url.replaceAll("(?<!:)//", "/");
	}
}
