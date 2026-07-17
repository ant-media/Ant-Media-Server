package io.antmedia.muxer;


import static org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_AAC;
import static org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_H264;
import static org.bytedeco.ffmpeg.global.avcodec.AV_INPUT_BUFFER_PADDING_SIZE;
import static org.bytedeco.ffmpeg.global.avcodec.AV_PKT_DATA_NEW_EXTRADATA;
import static org.bytedeco.ffmpeg.global.avcodec.AV_PKT_FLAG_KEY;
import static org.bytedeco.ffmpeg.global.avcodec.av_bsf_receive_packet;
import static org.bytedeco.ffmpeg.global.avcodec.av_bsf_send_packet;
import static org.bytedeco.ffmpeg.global.avcodec.av_packet_clone;
import static org.bytedeco.ffmpeg.global.avcodec.av_packet_free;
import static org.bytedeco.ffmpeg.global.avcodec.av_packet_ref;
import static org.bytedeco.ffmpeg.global.avcodec.av_packet_unref;
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_parameters_copy;
import static org.bytedeco.ffmpeg.global.avformat.AVFMT_NOFILE;
import static org.bytedeco.ffmpeg.global.avformat.AVIO_FLAG_WRITE;
import static org.bytedeco.ffmpeg.global.avformat.av_interleaved_write_frame;
import static org.bytedeco.ffmpeg.global.avformat.avformat_alloc_output_context2;
import static org.bytedeco.ffmpeg.global.avutil.AVMEDIA_TYPE_AUDIO;
import static org.bytedeco.ffmpeg.global.avutil.AVMEDIA_TYPE_VIDEO;
import static org.bytedeco.ffmpeg.global.avutil.AV_ROUND_NEAR_INF;
import static org.bytedeco.ffmpeg.global.avutil.AV_ROUND_PASS_MINMAX;
import static org.bytedeco.ffmpeg.global.avutil.av_dict_free;
import static org.bytedeco.ffmpeg.global.avutil.av_dict_set;
import static org.bytedeco.ffmpeg.global.avutil.av_rescale_q;
import static org.bytedeco.ffmpeg.global.avutil.av_rescale_q_rnd;
import org.bytedeco.ffmpeg.global.avformat;
import org.bytedeco.ffmpeg.global.avutil;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.SizeTPointer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Vertx;
import io.vertx.core.WorkerExecutor;

public class EndpointMuxer extends Muxer {

	/** ~1-2s of frames at typical FPS */
	private static final int PACKET_QUEUE_CAPACITY = 100;

	/** {@link #videoStreamIndex} before the output streams are known, and once it turns out there are none. */
	private static final int STREAM_UNRESOLVED = -2;
	private static final int NO_VIDEO_STREAM = -1;

	/**
	 * Fallback so a drop cycle cannot wait forever. A drop normally ends on the next
	 * video keyframe, but a source whose video died while audio keeps flowing will never
	 * send one, and waiting on it would keep the endpoint silent for good. After this
	 * much media time the cycle resumes on whatever arrives instead: brief artifacts,
	 * but alive.
	 */
	private static final long RESUME_WAIT_LIMIT_MS = 10_000L;

	/**
	 * Absorb publisher-side gaps: an incoming dts jump above this many seconds is
	 * charged to {@link #dropOffsetMs} like a drop, so the endpoint sees an unbroken
	 * timeline instead of the gap. -1 disables. Must stay well above the source frame
	 * interval: a source slower than one frame per this period reads as a permanent gap
	 * and gets its timeline compressed, growing latency without bound.
	 */
	private static final int ABSORB_SOURCE_GAP_SECONDS = 1;
	private static final long SOURCE_GAP_MS = ABSORB_SOURCE_GAP_SECONDS * 1000L;

	/**
	 * Enforce a live edge. An upstream stall whose packets got buffered (a blocked
	 * publisher that kept encoding) arrives late as a burst with continuous timestamps:
	 * no queue fills and no dts jumps, yet the endpoint replays the backlog seconds
	 * behind live for good. When arrivals lag wall clock by more than this many seconds,
	 * skip forward until they are near live again. -1 disables. A source that is
	 * persistently slower than realtime accumulates lag legitimately and gets a jump
	 * cut each time it reaches this budget; that is what enforcing a live edge means.
	 */
	private static final int ABSORB_SOURCE_LAG_SECONDS = 3;
	private static final long SOURCE_LAG_MS = ABSORB_SOURCE_LAG_SECONDS * 1000L;
	/** Where a lag skip aims to land, well under the trigger so it does not re-fire. */
	private static final long LAG_RESUME_TARGET_MS = 1000L;

	/** Shared write pool; sized by the {@code endpointMuxerExecutor} bean, or the fallback below if it's absent (old config). */
	static final String WORKER_POOL_NAME = "endpoint-muxer-pool";

	private String url;
	private volatile boolean trailerWritten = false;
	private IEndpointStatusListener statusListener;

	private BytePointer allocatedExtraDataPointer = null;

	private volatile String status = IAntMediaStreamHandler.BROADCAST_STATUS_CREATED;

	/** Status mutation lock kept off {@code this} so the drain job doesn't contend on the synchronized methods. */
	private final Object statusLock = new Object();

	private volatile boolean keyFrameReceived = false;

	private AtomicBoolean preparedIO = new AtomicBoolean(false);
	private AtomicBoolean cancelOpenIO = new AtomicBoolean(false);

	public String muxerType = null;

	/** Producer clones packets in; the drain job writes them out. */
	private final LinkedBlockingQueue<AVPacket> packetQueue = new LinkedBlockingQueue<>(PACKET_QUEUE_CAPACITY);
	/** Lock for frame dropping */
	private final Object queueLock = new Object();
	/** Media time removed by drops, subtracted on the way out. Guarded by {@link #queueLock}. */
	private long dropOffsetMs = 0;
	/** Set while a drop is in progress, cleared on the packet the endpoint restarts from. Producer-thread only. */
	private boolean waitingForResumePoint = false;
	/** Media time the in-progress drop cut from, AV_NOPTS_VALUE when idle. Producer-thread only. */
	private long dropStartMs = avutil.AV_NOPTS_VALUE;
	/** Last incoming dts on any stream, for publisher gap detection. Producer-thread only. */
	private long lastInputDtsMs = avutil.AV_NOPTS_VALUE;
	/** Furthest incoming dts on any stream. Lag is measured from this, so interleave skew cannot read as lateness. Producer-thread only. */
	private long maxInputDtsMs = avutil.AV_NOPTS_VALUE;
	/** Learned inter-packet step, cushions where a gap resume re-anchors. Producer-thread only. */
	private long inputStepMs = 33;
	/** Set for gap cycles: nothing was discarded, so any video packet resumes, no keyframe wait. Producer-thread only. */
	private boolean resumeOnAnyVideo = false;
	/** Wall/media reference lag is measured against, re-anchored at every resume and whenever arrivals run ahead. Producer-thread only. */
	private long wallStartMs = 0;
	private long dtsStartMs = avutil.AV_NOPTS_VALUE;
	/** Lag cycles only: the resume keyframe must reach this dts, so one cycle skips the whole stale backlog. Producer-thread only. */
	private long skipToDtsMs = avutil.AV_NOPTS_VALUE;
	/** Output index of the video stream. Producer-thread only. */
	private int videoStreamIndex = STREAM_UNRESOLVED;
	/** Last dts written per output stream. Drain-thread only, guarded by {@link #writeLock}. */
	private long[] lastWrittenDts;
	private final AtomicBoolean drainScheduled = new AtomicBoolean(false);
	private volatile boolean running = false;
	private long drainTimerId = -1;

	/** Guards each native write against the teardown free path. */
	private final Object writeLock = new Object();

	private final EndpointAnalytics analytics;

	/**
	 * Drop packets for this long after first arrival. Lets the source pipeline
	 * settle before streaming to endpoint, for stability.
	 */
	private static final long STARTUP_GRACE_PERIOD_MS = 1500L;
	private long graceStartMs = 0L;

	public EndpointMuxer(String url, Vertx vertx) {
		super(vertx);
		this.format = "flv";
		this.url = url;
		this.analytics = new EndpointAnalytics(url, PACKET_QUEUE_CAPACITY);

		// Base inits these to 0 while its own rebase branches guard on -1, so that
		// logic is dead. -1 marks "origin not captured yet" for captureFirstDts.
		this.firstVideoDts = -1;
		this.firstAudioDts = -1;

		parseEndpointURL(this.url);
	}

	public String getMuxerType() {
		return muxerType;
	}

	void parseEndpointURL(String url){
		if(url == null)
			return;
		if(url.startsWith("rtmp")) {
			format = "flv";
			muxerType = "rtmp";
			// Cap AVIO blocking so a dead/slow remote can't wedge us for the
			// kernel TCP retransmit window (~75s) on open or indefinitely on writes.
			options.put("rw_timeout", "10000000");

			// Publisher-side tunings. NODE: rtmp_live/rtmp_buffer are subscriber-only
			options.put("tcp_nodelay", "1");
			options.put("rtmp_maxchunk", "32768");
			options.put("flvflags", "no_duration_filesize");

			// check if app name is present in the URL rtmp://Domain.com/AppName/StreamId
			String regex = "rtmp(s)?://[a-zA-Z0-9\\.-]+(:[0-9]+)?/([^/]+)/.*";

			Pattern rtmpAppName = Pattern.compile(regex);
			Matcher checkAppName = rtmpAppName.matcher(url);

			if (!checkAppName.matches()) {
				//this is the fix to send stream for urls without app
				options.put("rtmp_app", "");
			}
		}
		else if(url.startsWith("srt")){
			muxerType = "srt";
			format = "mpegts";
		}
	}
	
	@Override
	public String getOutputURL() {
		return url;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized boolean addStream(AVCodec codec, AVCodecContext codecContext, int streamIndex) {

		boolean result = super.addStream(codec, codecContext, streamIndex);
		
		setStatus(result ? IAntMediaStreamHandler.BROADCAST_STATUS_PREPARING : IAntMediaStreamHandler.BROADCAST_STATUS_FAILED);
		
		return result;

	}
	public void setStatusListener(IEndpointStatusListener listener){
		this.statusListener = listener;
	}

	@Override
	public AVFormatContext getOutputFormatContext() {
		if (outputFormatContext == null) {
			logger.info("Creating outputFormatContext");
			outputFormatContext= new AVFormatContext(null);
			int ret = avformat_alloc_output_context2(outputFormatContext, null, format, null);
			if (ret < 0) {
				setStatus(IAntMediaStreamHandler.BROADCAST_STATUS_FAILED);
				logger.info("Could not create output context for url {}", url);
				return null;
			}
		}
		return outputFormatContext;
	}

	@Override
	public boolean openIO() {
		if ((getOutputFormatContext().oformat().flags() & AVFMT_NOFILE) != 0) {
			return true;
		}

		AVDictionary localOpts = new AVDictionary();
		try {
			for (Map.Entry<String, String> e : options.entrySet()) {
				av_dict_set(localOpts, e.getKey(), e.getValue(), 0);
			}

			AVIOContext pb = new AVIOContext(null);
			int ret = avformat.avio_open2(pb, getOutputURL(), AVIO_FLAG_WRITE, null, localOpts);
			if (ret < 0) {
				logger.warn("Could not open output url: {}", getOutputURL());
				return false;
			}
			getOutputFormatContext().pb(pb);
			return true;
		} finally {
			av_dict_free(localOpts);
		}
	}

	/**
	 * Test-only shim: production opens its own local dict in {@link #openIO()}.
	 * Caller owns the returned dict.
	 */
	@Override
	public AVDictionary getOptionDictionary() {
		AVDictionary d = new AVDictionary();
		for (Map.Entry<String, String> e : options.entrySet()) {
			av_dict_set(d, e.getKey(), e.getValue(), 0);
		}
		return d;
	}

	public void setStatus(String status)
	{
		synchronized (statusLock) {
			if (!this.status.equals(status) && this.statusListener != null)
			{
				this.statusListener.endpointStatusUpdated(this.url, status);
			}
			this.status = status;
		}
	}
	
	public String getStatus(){
		return this.status;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized boolean prepareIO()
	{
		/*
		 * extradata context is created if addVideoStream is called from WebRTC Forwarder
		 */


		if (preparedIO.get()) {
			//it means it's already called
			return false;
		}
		preparedIO.set(true);
		cancelOpenIO.set(false);
		boolean result = false;
		//if there is a stream in the output format context, try to push
		if (getOutputFormatContext().nb_streams() > 0) 
		{
			this.vertx.executeBlocking(() -> {
				if (openIO())
				{
					if (bsfFilterContextList.isEmpty())
					{
						writeHeader();
						return null;
					}
					if (!exitIfCancelled())
					{
						isRunning.set(true);
						setStatus(IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING);
					}

				}
				else
				{
					clearResource();
					setStatus(IAntMediaStreamHandler.BROADCAST_STATUS_FAILED);
					logger.error("Cannot initializeOutputFormatContextIO for {} endpoint:{}", muxerType ,url);
				}

				return null;
			}, false);
			
			result = true;
		}
		else {
			setStatus(IAntMediaStreamHandler.BROADCAST_STATUS_FAILED);
		}
		
		return result;
	}

	/**
	 * If the broadcast is stopped while the muxer is writing the header
	 * it cannot complete writing the header
	 * Then writeTrailer causes crash because of memory problem.
	 * We need to control if header is written before trying to write Trailer and synchronize them.
	 */
	@Override
	public synchronized boolean writeHeader() {
		if(!trailerWritten)
		{
			long startTime = System.currentTimeMillis();
			super.writeHeader();
			long diff = System.currentTimeMillis() - startTime;
			logger.info("write header takes {} for {}:{} the bitstream filter name is {}", diff, muxerType, getOutputURL(), getBitStreamFilter());

			headerWritten = true;
			startDraining();
			setStatus(IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING);

			return true;
		}
		else{
			logger.warn("Trying to write header after writing trailer");
			return false;
		}
	}

	private synchronized void startDraining() {
		if (running) {
			return;
		}

		running = true;
		// No bean config: fall back to 16 threads, 15s max-execute (> rtmp rw_timeout 10s).
		final WorkerExecutor writeExecutor = vertx.createSharedWorkerExecutor(WORKER_POOL_NAME, 16, 15, TimeUnit.SECONDS);

		drainTimerId = vertx.setPeriodic(10, t -> {
			if (running && drainScheduled.compareAndSet(false, true)) {
				writeExecutor.executeBlocking(() -> { drain(); return null; }, false)
						.onComplete(ar -> drainScheduled.set(false));
			}
		});
		
		logger.info("Endpoint drain started for {}:{}", muxerType, url);
	}

	private synchronized void stopDraining() {
		running = false;
		if (drainTimerId != -1) {
			vertx.cancelTimer(drainTimerId);
			drainTimerId = -1;
		}
	}

	/**
	 * {@inheritDoc}
	 * Look at the comments {@code writeHeader}
	 */
	@Override
	public synchronized void writeTrailer() {
		cancelOpenIO.set(true);
		stopDraining();

		synchronized (writeLock) {
			if (headerWritten) {
				if (!trailerWritten) {
					super.writeTrailer();
					trailerWritten = true;
				}
			} else {
				logger.info("Not writing trailer because header is not written yet");
				super.clearResource();
				preparedIO.set(false);
			}
		}
		freeQueuedPackets();
		setStatus(IAntMediaStreamHandler.BROADCAST_STATUS_FINISHED);
	}

	@Override
	public synchronized void clearResource() {
		stopDraining();

		synchronized (writeLock) {
			super.clearResource();
			if (!headerWritten) {
				preparedIO.set(false);
			}
		}
		freeQueuedPackets();

		// allocatedExtraDataPointer is freed when the native context closes
	}

	private boolean exitIfCancelled() {
		if (!cancelOpenIO.get()) {
			return false;
		}
		logger.info("RTMP muxer openIO cancelled for {}", url);
		clearResource();
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized boolean addVideoStream(int width, int height, AVRational timebase, int codecId, int streamIndex, boolean isAVC, AVCodecParameters codecpar) {
		
		boolean result = super.addVideoStream(width, height, timebase, codecId, streamIndex, isAVC, codecpar);
		if (result && this.format.equals("flv"))
		{
			AVStream outStream = getOutputFormatContext().streams(inputOutputStreamIndexMap.get(streamIndex));
			
			setBitstreamFilter("extract_extradata");
			
			AVBSFContext avbsfContext = initVideoBitstreamFilter(getBitStreamFilter(), outStream.codecpar(), inputTimeBaseMap.get(streamIndex));
			
			if (avbsfContext != null) {
				int ret = avcodec_parameters_copy(outStream.codecpar(), avbsfContext.par_out());
				result = ret == 0;
			}
			logger.info("Adding video stream index:{} for stream:{}", streamIndex, url);
		}
		
		return result;
	}
	

	@Override
	public synchronized void writePacket(AVPacket pkt, final AVRational inputTimebase, final AVRational outputTimebase, int codecType)
	{
		if (inStartupGracePeriod()) {
			return;
		}

		AVFormatContext context = getOutputFormatContext();
		if (context.streams(pkt.stream_index()).codecpar().codec_type() ==  AVMEDIA_TYPE_AUDIO && !headerWritten) {
			//Opening the RTMP muxer may take some time and don't make audio queue increase
			logger.info("Not writing audio packet to muxer because header is not written yet for {}", url);
			return;
		}
		writeFrameInternal(pkt, inputTimebase, outputTimebase, context, codecType);
	}

	/**
	 * Endpoints are added on the fly, so by the time a push starts the source dts
	 * is already far from zero. Passing it through hands the receiver a stream
	 * claiming to start N seconds in, which some ingests turn into N seconds of
	 * buffer.
	 *
	 * Both streams rebase against a single origin, otherwise the A/V skew shifts.
	 * The base muxer does the same, but neither half of its version runs here: the
	 * audio half sits in the writePacket override this class replaces, and the
	 * video half sits behind firstKeyFrameReceived, which only RecordMuxer and
	 * HLSMuxer clear.
	 */
	private long captureFirstDts(AVPacket pkt, AVRational inputTimebase, int codecType) {
		if (codecType != AVMEDIA_TYPE_AUDIO && codecType != AVMEDIA_TYPE_VIDEO) {
			return 0;
		}
		boolean isAudio = codecType == AVMEDIA_TYPE_AUDIO;

		if (firstPacketDtsMs == -1) {
			firstPacketDtsMs = av_rescale_q(pkt.dts(), inputTimebase, MuxAdaptor.TIME_BASE_FOR_MS);
			if (isAudio) {
				firstAudioDts = pkt.dts();
			}
			else {
				firstVideoDts = pkt.dts();
			}
			logger.info("Rebasing {} push from first {} packet dts:{}ms for {}", muxerType,
					isAudio ? "audio" : "video", firstPacketDtsMs, url);
		}

		long firstDts = isAudio ? firstAudioDts : firstVideoDts;
		if (firstDts == -1) {
			firstDts = av_rescale_q(firstPacketDtsMs, MuxAdaptor.TIME_BASE_FOR_MS, inputTimebase);
			// The other stream can start behind the origin. Clamp so it can't go negative.
			if ((pkt.dts() - firstDts) < 0) {
				firstDts = pkt.dts();
			}
			if (isAudio) {
				firstAudioDts = firstDts;
			}
			else {
				firstVideoDts = firstDts;
			}
		}
		return firstDts;
	}

	/**
	 * Extracted so unit tests can stub it or spy...
	 */
	public boolean inStartupGracePeriod() {
		if (graceStartMs == 0L) {
			graceStartMs = System.currentTimeMillis();
			logger.info("Startup grace period ({} ms) started for {}", STARTUP_GRACE_PERIOD_MS, url);
		}
		return System.currentTimeMillis() - graceStartMs < STARTUP_GRACE_PERIOD_MS;
	}

	public synchronized void writeFrameInternal(AVPacket pkt, AVRational inputTimebase, AVRational outputTimebase,
			AVFormatContext context, int codecType)
	{
		long pts = pkt.pts();
		long dts = pkt.dts();
		long duration = pkt.duration();
		long pos = pkt.pos();

		long firstDts = captureFirstDts(pkt, inputTimebase, codecType);

		pkt.pts(av_rescale_q_rnd(pkt.pts() - firstDts, inputTimebase, outputTimebase, AV_ROUND_NEAR_INF|AV_ROUND_PASS_MINMAX));
		pkt.dts(av_rescale_q_rnd(pkt.dts() - firstDts, inputTimebase, outputTimebase, AV_ROUND_NEAR_INF|AV_ROUND_PASS_MINMAX));
		pkt.duration(av_rescale_q(pkt.duration(), inputTimebase, outputTimebase));
		pkt.pos(-1);
		int ret = 0;

		if (codecType == AVMEDIA_TYPE_VIDEO)
		{
			ret = av_packet_ref(getTmpPacket() , pkt);
			if (ret < 0) {
				setStatus(IAntMediaStreamHandler.BROADCAST_STATUS_ERROR);
				logger.error("Cannot copy packet for {}", file.getName());
				return;
			}
			if (!bsfFilterContextList.isEmpty() && bsfFilterContextList.get(0) != null)
			{
				ret = av_bsf_send_packet(bsfFilterContextList.get(0), getTmpPacket());
				if (ret < 0) {
					setStatus(IAntMediaStreamHandler.BROADCAST_STATUS_ERROR);
					logger.warn("cannot send packet to the filter");
					return;
				}

				while ((ret = av_bsf_receive_packet(bsfFilterContextList.get(0), getTmpPacket())) == 0)
				{
					if (!headerWritten)
					{
						SizeTPointer size = new SizeTPointer(1);
						BytePointer extradataBytePointer = avcodec.av_packet_get_side_data(getTmpPacket(), AV_PKT_DATA_NEW_EXTRADATA,  size);
						if (size.get() != 0)
						{
							allocatedExtraDataPointer = new BytePointer(avutil.av_malloc(size.get() + AV_INPUT_BUFFER_PADDING_SIZE)).capacity(size.get() + AV_INPUT_BUFFER_PADDING_SIZE);
							byte[] extraDataArray = new byte[(int)size.get()];
							extradataBytePointer.get(extraDataArray, 0, extraDataArray.length);
							allocatedExtraDataPointer.put(extraDataArray, 0, extraDataArray.length);
							logger.info("extradata size:{} extradata: {} allocated pointer: {}", size.get(), extradataBytePointer, allocatedExtraDataPointer);
							context.streams(pkt.stream_index()).codecpar().extradata(allocatedExtraDataPointer);
							context.streams(pkt.stream_index()).codecpar().extradata_size((int)size.get());
							writeHeader();
							setStatus(IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING);
						}
					}

					if (headerWritten)
					{
						enqueuePacket(getTmpPacket(), context);
					}
					else {
						setStatus(IAntMediaStreamHandler.BROADCAST_STATUS_ERROR);
						logger.warn("Header is not written yet for writing video packet for stream: {}", file.getName());
					}
				}
			}
			else
			{
				enqueuePacket(getTmpPacket(), context);
			}
			av_packet_unref(getTmpPacket());
		}
		else if (codecType == AVMEDIA_TYPE_AUDIO && headerWritten)
		{
			av_packet_ref(getTmpPacket() , pkt);
			enqueuePacket(getTmpPacket(), context);
			av_packet_unref(getTmpPacket());
		}

		pkt.pts(pts);
		pkt.dts(dts);
		pkt.duration(duration);
		pkt.pos(pos);
	}

	/**
	 * Clones {@code src} (it shares data with {@link #getTmpPacket()}, which
	 * {@link #addExtradataIfRequired} mutates in place) onto the drain queue.
	 *
	 * A full queue means the endpoint is behind for good: the backlog can only leave at
	 * timestamp pace, so keeping it keeps its whole duration as latency. Drop it instead
	 * and restart at the next resume point, relabelled so the endpoint never sees the hole.
	 */
	private void enqueuePacket(AVPacket src, AVFormatContext context) {
		if (!running || cancelOpenIO.get()) {
			return;
		}

		detectSourceStall(src, context);

		// Mid-drop: discard without cloning until the endpoint can pick the stream back up.
		if (waitingForResumePoint && !tryResume(src, context)) {
			return;
		}

		if (src.stream_index() == getVideoStreamIndex(context)) {
			addExtradataIfRequired(src, (src.flags() & AV_PKT_FLAG_KEY) != 0);
		}

		AVPacket clone = av_packet_clone(src);
		if (clone == null) {
			return;
		}
		if (packetQueue.offer(clone)) {
			return;
		}

		dropBacklog(context);
		// src may be a resume point itself, so restart on it rather than spend another GOP
		// waiting for the next one.
		if (waitingForResumePoint && !tryResume(src, context)) {
			av_packet_free(clone);
			return;
		}
		if (!packetQueue.offer(clone)) {
			av_packet_free(clone);
		}
	}

	/**
	 * Drops the whole backlog and holds the queue empty until the stream can restart
	 * cleanly. Records the dts the drain would have written next: it sits one frame past
	 * the last dts actually written, so resuming from it lands the new timeline flush
	 * against the old one, with no gap and no overlap.
	 *
	 * The queue is FIFO over what the drain has not written yet, so that marker is simply
	 * the oldest queued video packet, or the oldest of any stream when no video is queued
	 * (video stalled at the source, or an audio-only endpoint).
	 */
	private void dropBacklog(AVFormatContext context) {
		int videoIndex = getVideoStreamIndex(context);
		long videoMs = avutil.AV_NOPTS_VALUE;
		long anyMs = avutil.AV_NOPTS_VALUE;
		// queueLock keeps the drain from polling a packet this scan already accounted for.
		synchronized (queueLock) {
			AVPacket pkt;
			while ((pkt = packetQueue.poll()) != null) {
				try {
					long ms = dtsMs(pkt, context);
					if (ms == avutil.AV_NOPTS_VALUE) {
						continue;
					}
					if (anyMs == avutil.AV_NOPTS_VALUE) {
						anyMs = ms;
					}
					if (videoMs == avutil.AV_NOPTS_VALUE && pkt.stream_index() == videoIndex) {
						videoMs = ms;
					}
				}
				finally {
					av_packet_free(pkt);
				}
			}
		}
		dropStartMs = videoMs != avutil.AV_NOPTS_VALUE ? videoMs : anyMs;
		// Undelivered frames were just discarded, so this cycle must wait for a keyframe.
		resumeOnAnyVideo = false;
		// No marker means the drain emptied the queue between the failed offer and this
		// scan: nothing was lost, so there is nothing to charge and nothing to wait for.
		// Entering the drop cycle here would punch a real hole for no reason.
		waitingForResumePoint = dropStartMs != avutil.AV_NOPTS_VALUE;
		if (waitingForResumePoint) {
			analytics.recordDrop(packetQueue.size());
		}
	}

	/**
	 * A publisher-side stall reaches this muxer in one of two shapes, and the endpoint
	 * reacts to both exactly as badly as to a drop-made gap (see {@link #dropBacklog}).
	 *
	 * GAP: the publisher dropped frames, so dts jumps. Open a resume cycle re-anchored
	 * one step past the last delivered packet; nothing was discarded by us, so any video
	 * packet resumes, and the charge lands the stream flush against what was written.
	 *
	 * LAG: the publisher buffered and is delivering late, timestamps continuous. No
	 * queue fills and no dts jumps, so only wall clock exposes it: arrivals fall behind
	 * the wall/media reference. Skip forward by discarding until a keyframe near live
	 * ({@code skipToDtsMs}), charging the skipped span. The reference re-anchors whenever
	 * arrivals run ahead of it, so a fast source cannot bank margin that would mask a
	 * later stall, and at every resume, which is what grants a persistently slow source
	 * a fresh budget instead of discarding it forever.
	 *
	 * Detection watches every stream and the charge always waits for the resume point:
	 * after a stall either stream can arrive first, and a packet written before the
	 * charge exists would run ahead of the re-anchored timeline, after which the
	 * monotonic guard would mute its whole stream for the length of the stall. With a
	 * backlog queued both shapes fold into a normal drop cycle, one combined charge,
	 * keyframe aligned. While a drop is already waiting there is nothing to do, the
	 * resume charge spans whatever happened.
	 */
	private void detectSourceStall(AVPacket src, AVFormatContext context) {
		if (waitingForResumePoint) {
			return;
		}
		long dts = dtsMs(src, context);
		if (dts == avutil.AV_NOPTS_VALUE) {
			return;
		}
		long now = System.currentTimeMillis();
		long previous = lastInputDtsMs;
		lastInputDtsMs = dts;
		if (previous == avutil.AV_NOPTS_VALUE) {
			wallStartMs = now;
			dtsStartMs = dts;
			maxInputDtsMs = dts;
			return;
		}

		long delta = dts - previous;
		// The gap branch runs on the pre-gap maxInputDtsMs: this packet's own dts sits
		// PAST the jump, and anchoring there would charge nothing and ship the gap.
		if (SOURCE_GAP_MS > 0 && delta > SOURCE_GAP_MS) {
			logger.info("Detected {}ms publisher gap for {}", delta, url);
			openStallCycle(context, true);
			return;
		}
		if (dts > maxInputDtsMs) {
			maxInputDtsMs = dts;
		}
		// The step cushion only ever spans one packet interval; a gap must not teach it.
		if (delta > 0 && delta <= 1000) {
			inputStepMs = delta;
		}

		if (SOURCE_LAG_MS <= 0) {
			return;
		}
		long lag = (now - wallStartMs) - (maxInputDtsMs - dtsStartMs);
		if (lag < 0) {
			wallStartMs = now;
			dtsStartMs = maxInputDtsMs;
			return;
		}
		if (lag > SOURCE_LAG_MS) {
			logger.info("Arrivals lag live by {}ms for {}, skipping forward", lag, url);
			skipToDtsMs = maxInputDtsMs + (lag - LAG_RESUME_TARGET_MS);
			openStallCycle(context, false);
		}
	}

	/**
	 * Opens a resume cycle for a detected stall, anchored one step past the furthest
	 * delivered packet so the resume charge lands the stream flush against what was
	 * written. A gap discarded nothing of ours, so any video packet may resume; a lag
	 * skip discards delivered frames, so it must wait for a keyframe.
	 */
	private void openStallCycle(AVFormatContext context, boolean nothingDiscarded) {
		if (packetQueue.isEmpty()) {
			dropStartMs = maxInputDtsMs + inputStepMs;
			resumeOnAnyVideo = nothingDiscarded;
			waitingForResumePoint = true;
		}
		else {
			dropBacklog(context);
		}
	}

	/**
	 * Restarts the queue on the first packet the endpoint can decode from, charging the
	 * dropped span to {@link #dropOffsetMs} so the output timeline stays unbroken. A real
	 * gap costs more latency than the drop that caused it, because the receiver waits it
	 * out rather than skipping to live.
	 *
	 * The charge comes off video, so video resumes exactly where it left off. Audio takes
	 * the same offset, which is what keeps A/V sync exact: one shared offset maps both
	 * streams' media time the same way. Audio's own arrival skew against video survives as
	 * a sub-frame step that {@link #isMonotonic} absorbs.
	 */
	private boolean tryResume(AVPacket src, AVFormatContext context) {
		long dts = dtsMs(src, context);
		// A packet with no timestamp cannot anchor a resume: the drop would go uncharged,
		// handing the endpoint the very gap this cycle exists to prevent.
		if (dts == avutil.AV_NOPTS_VALUE) {
			return false;
		}
		boolean resumable = isResumePoint(src, context)
				&& (skipToDtsMs == avutil.AV_NOPTS_VALUE || dts >= skipToDtsMs);
		if (!resumable && !waitedOut(dts)) {
			return false;
		}
		if (dropStartMs != avutil.AV_NOPTS_VALUE) {
			long dropped = Math.max(0, dts - dropStartMs);
			long total;
			synchronized (queueLock) {
				dropOffsetMs += dropped;
				total = dropOffsetMs;
			}
			logger.info("Endpoint queue resumed for {}: dropped {}ms, offset now {}ms", url, dropped, total);
		}
		// Restart stall tracking from here: the charge above covered everything up to
		// this packet, so a stale pre-drop dts must not read as a second gap, and the
		// lag reference gets a fresh budget.
		lastInputDtsMs = dts;
		maxInputDtsMs = dts;
		wallStartMs = System.currentTimeMillis();
		dtsStartMs = dts;
		skipToDtsMs = avutil.AV_NOPTS_VALUE;
		resumeOnAnyVideo = false;
		waitingForResumePoint = false;
		dropStartMs = avutil.AV_NOPTS_VALUE;
		return true;
	}

	/**
	 * Where the endpoint can restart without decoding into a hole. After a backlog drop
	 * video must resume on a keyframe, because frames the decoder needs were thrown away.
	 * After a publisher gap nothing was, so any video packet restarts the stream. An
	 * audio-only endpoint has no GOP to respect, and waiting on a keyframe that can never
	 * arrive would wedge it for good, so there any packet will do.
	 */
	private boolean isResumePoint(AVPacket pkt, AVFormatContext context) {
		int videoIndex = getVideoStreamIndex(context);
		if (videoIndex == NO_VIDEO_STREAM) {
			return true;
		}
		return pkt.stream_index() == videoIndex
				&& (resumeOnAnyVideo || (pkt.flags() & AV_PKT_FLAG_KEY) != 0);
	}

	/**
	 * Give up waiting for a resume point the source is evidently not going to send: a
	 * video stall while audio keeps arriving, or a lag skip target past what the source
	 * delivers, would otherwise hold the endpoint silent for good. Restarting mid-GOP
	 * costs artifacts until the next keyframe, which is what the old drop did on every
	 * overflow.
	 *
	 * Absolute distance, because a source dts reset (encoder restart) lands far BELOW
	 * the anchor. Resuming hands it to the writer, whose error path drives the normal
	 * republish; discarding would hold the endpoint silent with no error ever raised.
	 */
	private boolean waitedOut(long dts) {
		return dts != avutil.AV_NOPTS_VALUE
				&& dropStartMs != avutil.AV_NOPTS_VALUE
				&& Math.abs(dts - dropStartMs) > RESUME_WAIT_LIMIT_MS;
	}

	/**
	 * Output index of the video stream, or {@link #NO_VIDEO_STREAM} when the endpoint
	 * carries none. Streams are fixed by the time anything is queued, so this resolves
	 * once. Matching on the index also keeps the hot path off {@code codecpar}, which a
	 * stream index out of range would dereference past the end of the stream array.
	 */
	private int getVideoStreamIndex(AVFormatContext context) {
		if (videoStreamIndex != STREAM_UNRESOLVED) {
			return videoStreamIndex;
		}
		int found = NO_VIDEO_STREAM;
		int count = context.nb_streams();
		for (int i = 0; i < count; i++) {
			if (context.streams(i).codecpar().codec_type() == AVMEDIA_TYPE_VIDEO) {
				found = i;
				break;
			}
		}
		// Only latch an answer once the context actually holds streams, so a torn-down
		// context can't cache "no video" over an endpoint that has it.
		if (count > 0) {
			videoStreamIndex = found;
			logger.info("Endpoint {} resolved video stream index:{}", url, found);
		}
		return found;
	}

	/** @return the packet's dts in ms, or AV_NOPTS_VALUE when it carries none. */
	private static long dtsMs(AVPacket pkt, AVFormatContext context) {
		if (pkt.dts() == avutil.AV_NOPTS_VALUE) {
			return avutil.AV_NOPTS_VALUE;
		}
		return av_rescale_q(pkt.dts(), context.streams(pkt.stream_index()).time_base(), MuxAdaptor.TIME_BASE_FOR_MS);
	}

	/**
	 * Slides a packet back onto the timeline the drops removed time from.
	 * AV_NOPTS_VALUE is Long.MIN_VALUE, so subtracting from it would overflow into a
	 * large positive timestamp rather than leave it absent.
	 */
	private static void shiftBack(AVPacket pkt, long offsetMs, AVFormatContext context) {
		if (offsetMs == 0) {
			return;
		}
		long offset = av_rescale_q(offsetMs, MuxAdaptor.TIME_BASE_FOR_MS, context.streams(pkt.stream_index()).time_base());
		if (pkt.pts() != avutil.AV_NOPTS_VALUE) {
			pkt.pts(pkt.pts() - offset);
		}
		if (pkt.dts() != avutil.AV_NOPTS_VALUE) {
			pkt.dts(pkt.dts() - offset);
		}
	}

	/**
	 * FFmpeg rejects a backward dts, and the endpoint republishes on that error, so the
	 * few ms of arrival skew a resume can leave on audio would cost a reconnect. Dropping
	 * the packet costs a frame of audio instead. Video never lands here: a resume starts
	 * on a keyframe, which is always past what was written.
	 *
	 * Caller holds {@link #writeLock}.
	 */
	private boolean isMonotonic(AVPacket pkt, AVFormatContext context) {
		if (lastWrittenDts == null) {
			lastWrittenDts = new long[context.nb_streams()];
			Arrays.fill(lastWrittenDts, avutil.AV_NOPTS_VALUE);
		}
		long dts = pkt.dts();
		int index = pkt.stream_index();
		if (dts == avutil.AV_NOPTS_VALUE || index < 0 || index >= lastWrittenDts.length) {
			return true;
		}
		if (lastWrittenDts[index] != avutil.AV_NOPTS_VALUE && dts < lastWrittenDts[index]) {
			return false;
		}
		lastWrittenDts[index] = dts;
		return true;
	}

	/** Writes all queued packets to the endpoint. Runs on the write-executor pool, never holds {@code this}. */
	private void drain() {
		while (running) {
			AVPacket pkt;
			long offsetMs;
			// Poll under queueLock so a concurrent drop sees a stable head, and sample
			// the offset with it: a drop landing mid-write must not re-stamp a packet
			// that predates it.
			synchronized (queueLock) {
				pkt = packetQueue.poll();
				offsetMs = dropOffsetMs;
			}
			if (pkt == null) {
				break;
			}
			writeToEndpoint(pkt, offsetMs);
		}
	}

	private void writeToEndpoint(AVPacket pkt, long offsetMs) {
		long startNanos = System.nanoTime();
		long dts = pkt.dts();
		boolean wrote = false;
		try {
			synchronized (writeLock) {
				// running re-checked under the lock: teardown may have freed the context while we waited.
				if (running && outputFormatContext != null && outputFormatContext.pb() != null) {
					// Shifted here rather than at poll: the context is only safe to touch under this lock.
					shiftBack(pkt, offsetMs, outputFormatContext);
					dts = pkt.dts();
					if (!isMonotonic(pkt, outputFormatContext)) {
						logPacketIssue("Dropping backward dts:{} on stream:{} for {}", dts, pkt.stream_index(), url);
						return;
					}
					int ret = av_interleaved_write_frame(outputFormatContext, pkt);
					wrote = true;
					if (ret < 0) {
						logPacketIssue("Cannot write packet for stream:{} and url:{}. Packet pts:{} dts:{} Error is {}",
								streamId, getOutputURL(), pkt.pts(), pkt.dts(), getErrorDefinition(ret));
						setStatus(IAntMediaStreamHandler.BROADCAST_STATUS_ERROR);
					} else if (!IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING.equals(status)) {
						setStatus(IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING);
					}
				}
			}
		} catch (Exception e) {
			logger.error("Endpoint write error for {}: {}", url, e.toString());
		} finally {
			if (wrote) {
				analytics.recordWrite(System.nanoTime() - startNanos, dts, packetQueue.size());
			}
			av_packet_free(pkt);
		}
	}

	/** Discards remaining queued packets on teardown. */
	private void freeQueuedPackets() {
		AVPacket pkt;
		while ((pkt = packetQueue.poll()) != null) {
			av_packet_free(pkt);
		}
	}


	@Override
	public synchronized void writeVideoBuffer(ByteBuffer encodedVideoFrame, long dts, int frameRotation, int streamIndex,
			boolean isKeyFrame,long firstFrameTimeStamp, long pts)
	{

		if (!isRunning.get() || !registeredStreamIndexList.contains(streamIndex)) {
			logPacketIssue("Not writing to {} muxer because it's not started for {}", muxerType,url);
			return;
		}

		if (!keyFrameReceived && isKeyFrame) {
			keyFrameReceived = true;
			logger.info("Key frame is received to start for {}:{}", muxerType,url);
		}

		if (keyFrameReceived) {
			super.writeVideoBuffer(encodedVideoFrame, dts, frameRotation, streamIndex, isKeyFrame, firstFrameTimeStamp, pts);
		}
	}

	@Override
	public boolean isCodecSupported(int codecId) {
		return (codecId == AV_CODEC_ID_H264 || codecId == AV_CODEC_ID_AAC);
	}

	/** Test hook: lets unit tests drive recordDrop/recordWrite directly. */
	public EndpointAnalytics getAnalytics() {
		return analytics;
	}

	/**
	 * Per-endpoint analytics stuff
	 * {@link #recordDrop} is producer-thread (atomic+volatile);
	 * {@link #recordWrite} is called only from the drain job, which runs one-at-a-time
	 * (guarded by {@code drainScheduled}), so its plain fields need no synchronization.
	 */
	public static class EndpointAnalytics {
		private static final Logger logger = LoggerFactory.getLogger(EndpointMuxer.class);

		// --- Drop counter (producer thread). ---
		private static final long DROP_LOG_INTERVAL_MS = 5_000L;
		private final AtomicLong dropCount = new AtomicLong();
		private volatile long lastDropLogMs = 0L;

		// --- Write-latency analytics (worker thread only). ---
		private static final long WRITE_STATS_LOG_INTERVAL_MS = 10_000L;
		/** A write is flagged only if it exceeds both this floor AND {@link #WRITE_SPIKE_RATIO}× baseline. */
		private static final long WRITE_SPIKE_FLOOR_NANOS = 100_000_000L;
		private static final long WRITE_SPIKE_RATIO = 5L;
		private long writeAccumNanos = 0L;
		private long writeMaxNanos = 0L;
		private int writeCount = 0;
		/** EWMA of per-window averages, a multi-window baseline for spike detection. */
		private long writeEwmaNanos = 0L;
		private long lastWriteStatsLogMs = 0L;

		// --- Burst-flush detection (worker thread only). ---
		/** Two consecutive writes are "back-to-back" if their inter-gap is below this. */
		private static final long BURST_GAP_NANOS = 2_000_000L;
		private static final int BURST_THRESHOLD = 3;
		/**
		 * DTS span (ms in FLV time base) the burst must cover. Set well above
		 * natural AV-interleave (~42ms) and routine source batching (~150ms),
		 * so anything firing here is real pathology.
		 */
		private static final long BURST_DTS_SPAN_THRESHOLD_MS = 500L;
		private long lastWriteEndNanos = 0L;
		private int burstCount = 0;
		private long burstStartMaxDts = Long.MIN_VALUE;
		private long currentMaxDts = Long.MIN_VALUE;
		private boolean burstWarned = false;

		private final String url;
		private final int queueCapacity;

		EndpointAnalytics(String url, int queueCapacity) {
			this.url = url;
			this.queueCapacity = queueCapacity;
		}

		/** One warn per {@link #DROP_LOG_INTERVAL_MS} regardless of drop rate. */
		public void recordDrop(int queueDepth) {
			long count = dropCount.incrementAndGet();
			long now = System.currentTimeMillis();
			if (now - lastDropLogMs >= DROP_LOG_INTERVAL_MS) {
				lastDropLogMs = now;
				logger.warn("Endpoint queue drops: total={} for {} (depth={}/{})",
						count, url, queueDepth, queueCapacity);
			}
		}

		/**
		 * Records per-write timing, detects burst-flushes, and emits periodic stats.
		 * Worker-thread only, so fields are unsynchronized by design.
		 */
		public void recordWrite(long durNanos, long pktDts, int queueDepth) {
			writeAccumNanos += durNanos;
			if (durNanos > writeMaxNanos) {
				writeMaxNanos = durNanos;
			}
			writeCount++;

			// Cross-stream max DTS, skipping AV_NOPTS_VALUE and non-monotonic regressions.
			if (pktDts != avutil.AV_NOPTS_VALUE
					&& (currentMaxDts == Long.MIN_VALUE || pktDts > currentMaxDts)) {
				currentMaxDts = pktDts;
			}

			// Flag a real backlog drain: many sub-gap writes covering a meaningful
			// chunk of DTS. One warn per burst event.
			long writeEndNanos = System.nanoTime();
			if (lastWriteEndNanos != 0L
					&& (writeEndNanos - lastWriteEndNanos) - durNanos < BURST_GAP_NANOS) {
				burstCount++;
				// Lazy init: capture the burst's DTS baseline at the first valid
				// sample to avoid Long.MIN_VALUE overflow in the span subtraction.
				if (burstStartMaxDts == Long.MIN_VALUE) {
					burstStartMaxDts = currentMaxDts;
				}
				if (burstStartMaxDts != Long.MIN_VALUE) {
					long dtsSpan = currentMaxDts - burstStartMaxDts;
					if (!burstWarned
							&& burstCount > BURST_THRESHOLD
							&& dtsSpan > BURST_DTS_SPAN_THRESHOLD_MS) {
						logger.warn("Worker burst-flush: {} packets back-to-back covering {}ms DTS for {} (qDepth={}/{})",
								burstCount, dtsSpan, url, queueDepth, queueCapacity);
						burstWarned = true;
					}
				}
			} else {
				burstCount = 1;
				burstStartMaxDts = currentMaxDts;
				burstWarned = false;
			}
			lastWriteEndNanos = writeEndNanos;

			if (durNanos > WRITE_SPIKE_FLOOR_NANOS
					&& writeEwmaNanos > 0L
					&& durNanos > writeEwmaNanos * WRITE_SPIKE_RATIO) {
				logger.warn("Write latency spike for {}: {}ms (baseline {}ms)",
						url, durNanos / 1_000_000L, writeEwmaNanos / 1_000_000L);
			}

			long now = System.currentTimeMillis();
			if (now - lastWriteStatsLogMs >= WRITE_STATS_LOG_INTERVAL_MS && writeCount > 0) {
				long avgNanos = writeAccumNanos / writeCount;
				// EWMA alpha = 0.2 (~5-window memory).
				writeEwmaNanos = (writeEwmaNanos == 0L)
						? avgNanos
						: (avgNanos / 5L) + (writeEwmaNanos * 4L / 5L);

				logger.info("Write timing for {}: n={} avg={}us max={}ms qDepth={}/{}",
						url, writeCount, avgNanos / 1000L, writeMaxNanos / 1000000L,
						queueDepth, queueCapacity);

				writeAccumNanos = 0L;
				writeMaxNanos = 0L;
				writeCount = 0;
				lastWriteStatsLogMs = now;
			}
		}
	}
}
