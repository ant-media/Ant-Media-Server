package io.antmedia.muxer;


import static org.bytedeco.ffmpeg.global.avcodec.AV_PKT_FLAG_KEY;
import static org.bytedeco.ffmpeg.global.avcodec.av_bsf_free;
import static org.bytedeco.ffmpeg.global.avcodec.av_bsf_receive_packet;
import static org.bytedeco.ffmpeg.global.avcodec.av_bsf_send_packet;
import static org.bytedeco.ffmpeg.global.avcodec.av_init_packet;
import static org.bytedeco.ffmpeg.global.avcodec.av_packet_free;
import static org.bytedeco.ffmpeg.global.avcodec.av_packet_ref;
import static org.bytedeco.ffmpeg.global.avcodec.av_packet_unref;
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_parameters_copy;
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_parameters_from_context;
import static org.bytedeco.ffmpeg.global.avformat.AVFMT_NOFILE;
import static org.bytedeco.ffmpeg.global.avformat.AVIO_FLAG_WRITE;
import static org.bytedeco.ffmpeg.global.avformat.av_write_frame;
import static org.bytedeco.ffmpeg.global.avformat.av_write_trailer;
import static org.bytedeco.ffmpeg.global.avformat.avformat_alloc_output_context2;
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
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.bytedeco.ffmpeg.avcodec.AVBSFContext;
import org.bytedeco.ffmpeg.avcodec.AVCodec;
import org.bytedeco.ffmpeg.avcodec.AVCodecContext;
import org.bytedeco.ffmpeg.avcodec.AVCodecParameters;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.avformat.AVIOContext;
import org.bytedeco.ffmpeg.avformat.AVStream;
import org.bytedeco.ffmpeg.avutil.AVDictionary;
import org.bytedeco.ffmpeg.avutil.AVRational;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avformat;
import org.bytedeco.javacpp.BytePointer;
import org.red5.server.api.IContext;
import org.red5.server.api.scope.IScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import io.antmedia.AppSettings;
import io.antmedia.storage.StorageClient;
import io.vertx.core.Vertx;

public abstract class RecordMuxer extends Muxer {

	protected static Logger logger = LoggerFactory.getLogger(RecordMuxer.class);
	protected File fileTmp;
	protected StorageClient storageClient = null;
	protected String streamId;
	protected int videoIndex;
	protected int audioIndex;
	protected int resolution;
	protected AVBSFContext bsfExtractdataContext = null;

	protected AVPacket tmpPacket;
	protected Map<Integer, AVRational> codecTimeBaseMap = new HashMap<>();

	protected AVPacket videoPkt;
	protected int rotation;

	private String subFolder = null;

	/**
	 * By default first video key frame should be checked
	 * and below flag should be set to true
	 * If first video key frame should not be checked,
	 * then below should be flag in advance
	 */
	protected boolean firstKeyFrameReceivedChecked = false;

	/**
	 * Dynamic means that this mp4 muxer is added on the fly.
	 * It means it's started after broadcasting is started and it can be stopped before brodcasting has finished
	 */
	protected boolean dynamic = false;

	private String s3FolderPath = "streams";



	public RecordMuxer(StorageClient storageClient, Vertx vertx, String s3FolderPath) {
		super(vertx);
		this.storageClient = storageClient;
		this.s3FolderPath = s3FolderPath;
	}

	protected int[] SUPPORTED_CODECS;
	private long firstAudioDts = -1;
	private long firstVideoDts = -1;

	public boolean isCodecSupported(int codecId) {
		for (int i=0; i< SUPPORTED_CODECS.length; i++) {
			if (codecId == SUPPORTED_CODECS[i]) {
				return true;
			}
		}
		return false;

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void init(IScope scope, final String name, int resolutionHeight, String subFolder) {
		super.init(scope, name, resolutionHeight, false, subFolder);

		this.streamId = name;
		this.resolution = resolutionHeight;

		tmpPacket = avcodec.av_packet_alloc();
		av_init_packet(tmpPacket);

		videoPkt = avcodec.av_packet_alloc();
		av_init_packet(videoPkt);

	}
	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized boolean addVideoStream(int width, int height, AVRational timebase, int codecId, int streamIndex,
			boolean isAVC, AVCodecParameters codecpar) {
		boolean result = false;
		AVFormatContext outputContext = getOutputFormatContext();
		if (outputContext != null && isCodecSupported(codecId))
		{
			registeredStreamIndexList.add(streamIndex);
			AVStream outStream = avformat_new_stream(outputContext, null);
			outStream.codecpar().width(width);
			outStream.codecpar().height(height);
			outStream.codecpar().codec_id(codecId);
			outStream.codecpar().codec_type(AVMEDIA_TYPE_VIDEO);
			outStream.codecpar().format(AV_PIX_FMT_YUV420P);
			outStream.codecpar().codec_tag(0);
			//outStream.time_base(timebase);

			AVRational timeBase = new AVRational();
			timeBase.num(1).den(1000);
			codecTimeBaseMap.put(streamIndex, timeBase);
			result = true;
		}

		return result;
	}

	public boolean addAudioStream(int sampleRate, int channelLayout, int codecId, int streamIndex) {
		boolean result = false;
		AVFormatContext outputContext = getOutputFormatContext();
		if (outputContext != null && isCodecSupported(codecId))
		{
			registeredStreamIndexList.add(streamIndex);
			AVStream outStream = avformat_new_stream(outputContext, null);
			outStream.codecpar().sample_rate(sampleRate);
			outStream.codecpar().channel_layout(channelLayout);
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
			codecTimeBaseMap.put(streamIndex, timeBase);
			result = true;
		}

		return result;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized boolean addStream(AVCodec codec, AVCodecContext codecContext, int streamIndex) {
		
		AVCodecParameters codecParameter = new AVCodecParameters();
		int ret = avcodec_parameters_from_context(codecParameter, codecContext);
		if (ret < 0) {
			logger.error("Cannot get codec parameters for {}", streamId);
		}
		return addStream(codecParameter, codecContext.time_base(), streamIndex);
	}
	
	

	@Override
	public synchronized boolean addStream(AVCodecParameters codecParameters, AVRational timebase, int streamIndex) 
	{
		boolean result = false;
		AVFormatContext outputContext = getOutputFormatContext();
		if (outputContext != null && isCodecSupported(codecParameters.codec_id()) &&
				(codecParameters.codec_type() == AVMEDIA_TYPE_AUDIO || codecParameters.codec_type() == AVMEDIA_TYPE_VIDEO)
				)
		{
			AVStream outStream = avNewStream(outputContext);

			avcodec_parameters_copy(outStream.codecpar(), codecParameters);
			outStream.time_base(timebase);
			codecTimeBaseMap.put(outStream.index(), timebase);
			registeredStreamIndexList.add(streamIndex);
			outStream.codecpar().codec_tag(0);

			if (codecParameters.codec_type() == AVMEDIA_TYPE_AUDIO)
			{
				audioIndex = outStream.index();
			}
			else {
				videoIndex = outStream.index();
			}
			result = true;
		}
		else if (codecParameters.codec_type() == AVMEDIA_TYPE_DATA) {
			//if it's data, do not add and return true
			result = true;
		}
		else {
			logger.warn("Stream is not added for muxing to {} for stream:{}", getFileName(), streamId);
		}

		return result;
	}



	public AVFormatContext getOutputFormatContext() {
		if (outputFormatContext == null) {
			outputFormatContext= new AVFormatContext(null);
			fileTmp = new File(file.getAbsolutePath() + TEMP_EXTENSION);
			int ret = avformat_alloc_output_context2(outputFormatContext, null, format, fileTmp.getAbsolutePath());
			if (ret < 0) {
				logger.info("Could not create output context for {}", streamId);
				return null;
			}
		}
		return outputFormatContext;
	}

	public AVStream avNewStream(AVFormatContext context) {
		return avformat_new_stream(context, null);
	}

	protected boolean prepareAudioOutStream(AVStream inStream, AVStream outStream) {
		int ret = avcodec_parameters_copy(outStream.codecpar(), inStream.codecpar());
		if (ret < 0) {
			logger.info("Cannot get codec parameters for {}", streamId);
			return false;
		}
		return true;
	}
	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized boolean prepareIO() {


		AVFormatContext context = getOutputFormatContext();
		if (context == null || context.pb() != null) {
			//return false if it is already prepared
			return false;
		}

		AVIOContext pb = new AVIOContext(null);

		int ret = avformat.avio_open(pb, fileTmp.getAbsolutePath(), AVIO_FLAG_WRITE);
		if (ret < 0) {
			logger.warn("Could not open output file: {}" +
					" parent file exists:{}" , fileTmp.getAbsolutePath() , fileTmp.getParentFile().exists());
			return false;
		}

		context.pb(pb);

		AVDictionary optionsDictionary = null;

		if (!options.isEmpty()) {
			optionsDictionary = new AVDictionary();
			Set<String> keySet = options.keySet();
			for (String key : keySet) {
				av_dict_set(optionsDictionary, key, options.get(key), 0);
			}
		}

		ret = avformat_write_header(context, optionsDictionary);
		if (ret < 0) {
			logger.warn("could not write header for {}", fileTmp.getName());

			clearResource();
			return false;
		}
		if (optionsDictionary != null) {
			av_dict_free(optionsDictionary);
		}

		isRunning.set(true);
		return true;
	}


	@Override
	public synchronized void writeVideoBuffer(ByteBuffer encodedVideoFrame, long dts, int frameRotation, int streamIndex,boolean isKeyFrame,long firstFrameTimeStamp, long pts) {
		/*
		 * this control is necessary to prevent server from a native crash
		 * in case of initiation and preparation takes long.
		 * because native objects like videoPkt can not be initiated yet
		 */
		if (!isRunning.get()) {
			if (time2log  % 100 == 0) {
				logger.warn("Not writing VideoBuffer for {} because Is running:{}", streamId, isRunning.get());
				time2log = 0;
			}
			time2log++;
			return;
		}

		/*
		 * Rotation field is used add metadata to the mp4.
		 * this method is called in directly creating mp4 from coming encoded WebRTC H264 stream
		 */
		this.rotation = frameRotation;
		videoPkt.stream_index(streamIndex);
		videoPkt.pts(pts);
		videoPkt.dts(dts);
		if(isKeyFrame) {
			videoPkt.flags(videoPkt.flags() | AV_PKT_FLAG_KEY);
		}

		encodedVideoFrame.rewind();
		videoPkt.data(new BytePointer(encodedVideoFrame));
		videoPkt.size(encodedVideoFrame.limit());
		videoPkt.position(0);
		writePacket(videoPkt, (AVCodecContext)null);

		av_packet_unref(videoPkt);
	}

	@Override
	public synchronized void writeAudioBuffer(ByteBuffer audioFrame, int streamIndex, long timestamp) {
		if (!isRunning.get()) {
			if (time2log  % 100 == 0) {
				logger.warn("Not writing AudioBuffer for {} because Is running:{}", streamId, isRunning.get());
				time2log = 0;
			}
			time2log++;
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

	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized void writeTrailer() {

		if (!isRunning.get() || outputFormatContext == null || outputFormatContext.pb() == null) {
			//return if it is already null
			logger.warn("OutputFormatContext is not initialized or it is freed for file {}", fileTmp != null ? fileTmp.getName() : null);
			return;
		}

		logger.info("Record Muxer writing trailer for stream: {}", streamId);
		isRunning.set(false);

		av_write_trailer(outputFormatContext);

		logger.info("Clearing resources for stream: {}", streamId);
		clearResource();

		logger.info("Resources are cleaned for stream: {}", streamId);

		isRecording = false;

		vertx.executeBlocking(l->{
			try {


				String absolutePath = fileTmp.getAbsolutePath();

				String origFileName = absolutePath.replace(TEMP_EXTENSION, "");

				final File f = new File(origFileName);

				logger.info("File: {} exist: {}", fileTmp.getAbsolutePath(), fileTmp.exists());
				finalizeRecordFile(f);

				IContext context = RecordMuxer.this.scope.getContext();
				ApplicationContext appCtx = context.getApplicationContext();
				Object bean = appCtx.getBean("web.handler");
				if (bean instanceof IAntMediaStreamHandler) {
					((IAntMediaStreamHandler)bean).muxingFinished(streamId, f, getDurationInMs(f,streamId), resolution);
				}

				AppSettings appSettings = (AppSettings) appCtx.getBean(AppSettings.BEAN_NAME);


				if (appSettings.isS3RecordingEnabled()) {
					logger.info("Storage client is available saving {} to storage", f.getName());
					saveToStorage(s3FolderPath + File.pathSeparator + subFolder + File.pathSeparator, f, getFile().getName(), storageClient);
				}
			} catch (Exception e) {
				logger.error(e.getMessage());
			}
			l.complete();
		}, null);

	}

	public static void saveToStorage(String prefix, File fileToUpload, String fileName, StorageClient storageClient) {

		// Check file exist in S3 and change file names. In this way, new file is created after the file name changed.


		if (storageClient.fileExist(prefix + fileName)) {

			String tmpName =  fileName;

			int i = 0;
			do {
				i++;
				fileName = tmpName.replace(".", "_"+ i +".");

			} while (storageClient.fileExist(prefix + fileName));
		}

		storageClient.save(prefix + fileName, fileToUpload);
	}


	protected void finalizeRecordFile(final File file) throws IOException {
		Files.move(fileTmp.toPath(),file.toPath());
		logger.info("{} is ready", file.getName());
	}

	public static long getDurationInMs(File f, String streamId) {
		AVFormatContext inputFormatContext = avformat.avformat_alloc_context();
		int ret;
		if (avformat_open_input(inputFormatContext, f.getAbsolutePath(), null, (AVDictionary)null) < 0) {
			logger.info("cannot open input context for duration for stream: {}", streamId);
			avformat_close_input(inputFormatContext);
			return -1L;
		}

		ret = avformat_find_stream_info(inputFormatContext, (AVDictionary)null);
		if (ret < 0) {
			logger.info("Could not find stream information for stream: {}", streamId);
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

	protected void clearResource() {
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

		if (bsfExtractdataContext != null) {
			av_bsf_free(bsfExtractdataContext);
			bsfExtractdataContext = null;
		}

		/* close output */
		if ((outputFormatContext.flags() & AVFMT_NOFILE) == 0)
			avio_closep(outputFormatContext.pb());

		avformat_free_context(outputFormatContext);
		outputFormatContext = null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized void writePacket(AVPacket pkt, AVStream stream) {

		if (!firstKeyFrameReceivedChecked && stream.codecpar().codec_type() == AVMEDIA_TYPE_VIDEO) {
			//we set start time here because we start recording with key frame and drop the other
			//setting here improves synch between audio and video
			//setVideoStartTime(pkt.pts());
			int keyFrame = pkt.flags() & AV_PKT_FLAG_KEY;
			if (keyFrame == 1) {
				firstKeyFrameReceivedChecked = true;
				logger.warn("First key frame received for stream: {}", streamId);
			} else {
				logger.warn("First video packet is not key frame. It will drop for direct muxing. Stream {}", streamId);
				// return if firstKeyFrameReceived is not received
				// below return is important otherwise it does not work with like some encoders(vidiu)
				return;
			}
		}

		if (!isRunning.get() || !registeredStreamIndexList.contains(pkt.stream_index())) {
			if (time2log  % 100 == 0) {
				logger.warn("Not writing packet1 for {} - Is running:{} or stream index({}) is registered: {}", streamId, isRunning.get(), pkt.stream_index(), registeredStreamIndexList.contains(pkt.stream_index()));
				time2log = 0;
			}
			time2log++;
			return;
		}
		int streamIndex;

		if (stream.codecpar().codec_type() == AVMEDIA_TYPE_VIDEO) {
			streamIndex = videoIndex;
		}
		else if (stream.codecpar().codec_type() == AVMEDIA_TYPE_AUDIO) {
			streamIndex = audioIndex;
		}
		else {
			logger.error("Undefined codec type for stream: {} ", streamId);
			return;
		}

		AVStream outStream = outputFormatContext.streams(streamIndex);
		int index = pkt.stream_index();
		pkt.stream_index(streamIndex);

		writePacket(pkt, stream.time_base(),  outStream.time_base(), outStream.codecpar().codec_type());

		pkt.stream_index(index);
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized void writePacket(AVPacket pkt, AVCodecContext codecContext) {
		if (!isRunning.get() || !registeredStreamIndexList.contains(pkt.stream_index())) {
			if (time2log  % 100 == 0)
			{
				logger.warn("Not writing packet for {} - Is running:{} or stream index({}) is registered: {}", streamId, isRunning.get(), pkt.stream_index(), registeredStreamIndexList.contains(pkt.stream_index()));
				time2log = 0;
			}
			time2log++;
			return;
		}

		AVStream outStream = outputFormatContext.streams(pkt.stream_index());
		AVRational codecTimebase = codecTimeBaseMap.get(pkt.stream_index());
		int codecType = outStream.codecpar().codec_type();

		if (!firstKeyFrameReceivedChecked && codecType == AVMEDIA_TYPE_VIDEO) {
			int keyFrame = pkt.flags() & AV_PKT_FLAG_KEY;
			//we set start time here because we start recording with key frame and drop the other
			//setting here improves synch between audio and video
			if (keyFrame == 1) {
				firstKeyFrameReceivedChecked = true;
				logger.warn("First key frame received for stream: {}", streamId);
			} else {
				logger.info("First video packet is not key frame. It will drop for direct muxing. Stream {}", streamId);
				// return if firstKeyFrameReceived is not received
				// below return is important otherwise it does not work with like some encoders(vidiu)
				return;
			}
		}
		writePacket(pkt, codecTimebase,  outStream.time_base(), codecType);

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
	private void writePacket(AVPacket pkt, AVRational inputTimebase, AVRational outputTimebase, int codecType)
	{

		AVFormatContext context = getOutputFormatContext();
		if (context == null || context.pb() == null) {
			logger.warn("output context.pb field is null for stream: {}", streamId);
			return;
		}

		long pts = pkt.pts();
		long dts = pkt.dts();
		long duration = pkt.duration();
		long pos = pkt.pos();

		pkt.duration(av_rescale_q(pkt.duration(), inputTimebase, outputTimebase));
		pkt.pos(-1);

		if (codecType == AVMEDIA_TYPE_AUDIO)
		{
			if(firstAudioDts == -1) {
				firstAudioDts = pkt.dts();
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
			if(firstVideoDts == -1) {
				firstVideoDts = pkt.dts();
			}
			// we don't set startTimeInVideoTimebase here because we only start with key frame and we drop all frames
			// until the first key frame
			pkt.pts(av_rescale_q_rnd(pkt.pts() - firstVideoDts , inputTimebase, outputTimebase, AV_ROUND_NEAR_INF|AV_ROUND_PASS_MINMAX));
			pkt.dts(av_rescale_q_rnd(pkt.dts() - firstVideoDts, inputTimebase, outputTimebase, AV_ROUND_NEAR_INF|AV_ROUND_PASS_MINMAX));

			int ret = av_packet_ref(tmpPacket , pkt);
			if (ret < 0) {
				logger.error("Cannot copy video packet for {}", streamId);
				return;
			}

			writeVideoFrame(tmpPacket, context);

			av_packet_unref(tmpPacket);

		}
		else {
			//for any other stream like subtitle, etc.
			int ret = av_write_frame(context, pkt);

			if (ret < 0 && logger.isWarnEnabled()) {
				if (time2log  % 100 == 0)  {
					byte[] data = new byte[64];
					av_strerror(ret, data, data.length);
					logger.warn("cannot frame to muxer({}) not audio and not video. Error is {} ", file.getName(), new String(data, 0, data.length));
					time2log = 0;
				}
				time2log++;
			}
		}


		pkt.pts(pts);
		pkt.dts(dts);
		pkt.duration(duration);
		pkt.pos(pos);

	}


	/**
	 * {@inheritDoc}
	 */
	protected void writeVideoFrame(AVPacket pkt, AVFormatContext context) {
		int ret;
		if (bsfExtractdataContext != null) {
			ret = av_bsf_send_packet(bsfExtractdataContext, tmpPacket);
			if (ret < 0)
				return;

			while (av_bsf_receive_packet(bsfExtractdataContext, tmpPacket) == 0)
			{
				ret = av_write_frame(context, tmpPacket);
				if (ret < 0 && logger.isWarnEnabled()) {
					byte[] data = new byte[64];
					av_strerror(ret, data, data.length);
					logger.warn("cannot write video frame to muxer({}) av_bsf_receive_packet. Error is {} ", file.getName(), new String(data, 0, data.length));
				}

			}
		}
		else {

			ret = av_write_frame(context, pkt);
			if (ret < 0 && logger.isWarnEnabled()) {
				byte[] data = new byte[64];
				av_strerror(ret, data, data.length);
				logger.warn("cannot write video frame to muxer({}). Pts: {} dts:{}  Error is {} ", file.getName(), pkt.pts(), pkt.dts(), new String(data, 0, data.length));
			}
		}
	}

	protected void writeAudioFrame(AVPacket pkt, AVRational inputTimebase, AVRational outputTimebase,
			AVFormatContext context, long dts) {
		int ret;
		ret = av_write_frame(context, tmpPacket);
		if (ret < 0 && logger.isInfoEnabled()) {

			byte[] data = new byte[64];
			av_strerror(ret, data, data.length);
			logger.info("cannot write audio frame to muxer({}). Error is {} ", file.getName(), new String(data, 0, data.length));
		}
	}

	public void setDynamic(boolean dynamic) {
		this.dynamic = dynamic;
	}

	public boolean isDynamic() {
		return dynamic;
	}
}
