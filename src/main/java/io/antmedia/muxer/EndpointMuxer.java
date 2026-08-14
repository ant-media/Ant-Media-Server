package io.antmedia.muxer;


import static org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_AAC;
import static org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_H264;
import static org.bytedeco.ffmpeg.global.avcodec.AV_INPUT_BUFFER_PADDING_SIZE;
import static org.bytedeco.ffmpeg.global.avcodec.AV_PKT_DATA_NEW_EXTRADATA;
import static org.bytedeco.ffmpeg.global.avcodec.AV_PKT_FLAG_KEY;
import static org.bytedeco.ffmpeg.global.avcodec.av_bsf_receive_packet;
import static org.bytedeco.ffmpeg.global.avcodec.av_bsf_send_packet;
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
import static org.bytedeco.ffmpeg.global.avutil.AV_DICT_IGNORE_SUFFIX;
import static org.bytedeco.ffmpeg.global.avutil.av_dict_free;
import static org.bytedeco.ffmpeg.global.avutil.av_dict_get;
import static org.bytedeco.ffmpeg.global.avutil.av_dict_set;
import static org.bytedeco.ffmpeg.global.avutil.av_rescale_q;
import static org.bytedeco.ffmpeg.global.avutil.av_rescale_q_rnd;
import org.bytedeco.ffmpeg.global.avformat;
import org.bytedeco.ffmpeg.global.avutil;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
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
import org.bytedeco.ffmpeg.avutil.AVDictionaryEntry;
import org.bytedeco.ffmpeg.avutil.AVRational;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.SizeTPointer;

import io.vertx.core.Vertx;

public class EndpointMuxer extends Muxer {

	/** Consecutive unplaceable packets before we call the endpoint broken. */
	private static final int UNWRITABLE_LIMIT = 200;

	private final String url;
	private volatile boolean trailerWritten = false;
	private IEndpointStatusListener statusListener;

	private volatile String status = IAntMediaStreamHandler.BROADCAST_STATUS_CREATED;

	/** Separate from {@code this} so the drain never blocks on the synchronized methods. */
	private final Object statusLock = new Object();

	private volatile boolean keyFrameReceived = false;

	private final AtomicBoolean preparedIO = new AtomicBoolean(false);
	private final AtomicBoolean cancelOpenIO = new AtomicBoolean(false);

	public String muxerType = null;

	/** Owns the queue and the pacing policy. Null until the header is written. */
	private volatile EndpointMuxerPacingEngine engine;
	private volatile EndpointMuxerAnalytics analytics;

	/** Last dts written per output stream. Sized with the engine, then drain-thread only. */
	private long[] lastWrittenDts;
	/** Consecutive drops by {@link #isWritable}. Drain-thread only, guarded by {@link #writeLock}. */
	private int unwritableDtsCount = 0;
	private final AtomicBoolean drainScheduled = new AtomicBoolean(false);
	private volatile boolean running = false;
	private long drainTimerId = -1;
	private final Object writeLock = new Object();
	private long endpointFirstPacketDtsMs = -1;
	private long endpointFirstAudioDts = -1;
	private long endpointFirstVideoDts = -1;

	public EndpointMuxer(String url, Vertx vertx) {
		super(vertx);
		this.format = "flv";
		this.url = url;

		parseEndpointURL(this.url);
	}

	public String getMuxerType() {
		return muxerType;
	}

	void parseEndpointURL(String url){
		if(url == null) {
			return;
		}

		if(url.startsWith("rtmp")) {
			format = "flv";
			muxerType = "rtmp";
			// Only counts while the socket is fully unwritable, and it restarts on every partial
			// send. So it bounds a dead remote, not a slow one.
			options.put("rw_timeout", "5000000");

			// Kernel autotunes to tcp_wmem max otherwise, 4MB on most boxes, which parks seconds
			// of media where no policy of ours can see it. 1MB seams like fine balance for all policies.
			options.put("send_buffer_size", "1048576");

			// Publisher-side tunings. NOTE: rtmp_live/rtmp_buffer are subscriber-only
			options.put("tcp_nodelay", "1");
			options.put("flvflags", "no_duration_filesize");

			// check if app name is present in the URL rtmp://Domain.com/AppName/StreamId
			String regex = "rtmp(s)?://[a-zA-Z0-9\\.-]+(:[0-9]+)?/([^/]+)/.*";

			Pattern rtmpAppName = Pattern.compile(regex);
			Matcher checkAppName = rtmpAppName.matcher(url);

			if (!checkAppName.matches()) {
				//this is the fix to send stream for urls without app
				options.put("rtmp_app", "");
			}
		} else if(url.startsWith("srt")){
			muxerType = "srt";
			format = "mpegts";
		}
	}
	
	@Override
	public String getOutputURL() {
		return url;
	}
	
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
			AVFormatContext context = new AVFormatContext(null);
			int ret = avformat_alloc_output_context2(context, null, format, null);
			if (ret < 0) {
				setStatus(IAntMediaStreamHandler.BROADCAST_STATUS_FAILED);
				logger.info("Could not create output context for url {}", url);
				return null;
			}
			outputFormatContext = context;
		}
		return outputFormatContext;
	}

	@Override
	public boolean openIO() {
		AVFormatContext context = getOutputFormatContext();
		if (context == null) {
			return false;
		}

		if ((context.oformat().flags() & AVFMT_NOFILE) != 0) {
			return true;
		}

		AVDictionary localOpts = new AVDictionary();
		try {
			for (Map.Entry<String, String> e : options.entrySet()) {
				av_dict_set(localOpts, e.getKey(), e.getValue(), 0);
			}

			AVIOContext pb = new AVIOContext(null);
			int ret = avformat.avio_open2(pb, getOutputURL(), AVIO_FLAG_WRITE, null, localOpts);
			logIgnoredOptions(localOpts);
			if (ret < 0) {
				logger.warn("Could not open output url: {}", getOutputURL());
				return false;
			}
			context.pb(pb);
			return true;
		} finally {
			av_dict_free(localOpts);
		}
	}

	/**
	 * avio_open2 hands back what it did not consume. flvflags is expected, the same map also
	 * feeds avformat_write_header. Anything else listed never took effect.
	 */
	private void logIgnoredOptions(AVDictionary leftovers) {
		StringBuilder ignored = new StringBuilder();
		AVDictionaryEntry entry = null;
		while ((entry = av_dict_get(leftovers, "", entry, AV_DICT_IGNORE_SUFFIX)) != null) {
			if (ignored.length() > 0) {
				ignored.append(", ");
			}
			ignored.append(entry.key().getString()).append('=').append(entry.value().getString());
		}
		if (ignored.length() > 0) {
			logger.info("Endpoint {} options not consumed by avio_open2: {}", url, ignored);
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
				if (openIO()) {
					if (bsfFilterContextList.isEmpty()) {
						writeHeader();
						return null;
					}

					if (!exitIfCancelled()) {
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
		if (trailerWritten) {
			logger.warn("Trying to write header after writing trailer");
			return false;
		}

		long startTime = System.currentTimeMillis();
		boolean written = super.writeHeader();
		long diff = System.currentTimeMillis() - startTime;
		logger.info("write header takes {} for {}:{} the bitstream filter name is {}", diff, muxerType, getOutputURL(), getBitStreamFilter());

		if (!written) {
			setStatus(IAntMediaStreamHandler.BROADCAST_STATUS_ERROR);
			return false;
		}

		int streamCount = outputFormatContext.nb_streams();
		if (streamCount == 0) {
			logger.error("Endpoint {} has no output streams to push", url);
			setStatus(IAntMediaStreamHandler.BROADCAST_STATUS_ERROR);
			return false;
		}

		AVRational[] timeBases = new AVRational[streamCount];
		int videoIndex = -1;
		for (int i = 0; i < streamCount; i++) {
			AVStream stream = outputFormatContext.streams(i);
			timeBases[i] = new AVRational().num(stream.time_base().num()).den(stream.time_base().den());
			if (videoIndex == -1 && stream.codecpar().codec_type() == AVMEDIA_TYPE_VIDEO) {
				videoIndex = i;
			}
		}

		EndpointMuxerPacingPolicy policy = getAppSettings().isEndpointLiveEdgeEnabled() ? new EndpointMuxerLiveEdgePacing(url) : new EndpointMuxerBacklogPacing(url);
		analytics = new EndpointMuxerAnalytics(url, policy.queueCapacity());
		engine = new EndpointMuxerPacingEngine(policy, analytics, timeBases, videoIndex);
		lastWrittenDts = new long[streamCount];
		Arrays.fill(lastWrittenDts, avutil.AV_NOPTS_VALUE);

		logger.info("Endpoint {} initialized with policy {} (queue {}), {} streams, video stream index:{}",
				url, policy.getClass().getSimpleName(), policy.queueCapacity(), streamCount, videoIndex);

		// Publishes everything above to the drain thread, so keep it last.
		startDraining();
		setStatus(IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING);

		return true;
	}

	private synchronized void startDraining() {
		if (running) {
			return;
		}

		running = true;
		drainTimerId = vertx.setPeriodic(10, t -> {
			if (running && drainScheduled.compareAndSet(false, true)) {
				vertx.executeBlocking(() -> { drain(); return null; }, false)
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

		if (engine != null) {
			engine.close();
		}

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

		if (engine != null) {
			engine.close();
		}

		// the extradata buffer handed to codecpar is freed when the native context closes
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
		AVFormatContext context = getOutputFormatContext();
		if (context == null) {
			logPacketIssue("No output context for {}", url);
			return;
		}

		if (pkt.stream_index() < 0 || pkt.stream_index() >= context.nb_streams()) {
			logPacketIssue("Packet stream index:{} is out of range for {}", pkt.stream_index(), url);
			return;
		}

		if (context.streams(pkt.stream_index()).codecpar().codec_type() ==  AVMEDIA_TYPE_AUDIO && !headerWritten) {
			//Opening the RTMP muxer may take some time and don't make audio queue increase
			logger.info("Not writing audio packet to muxer because header is not written yet for {}", url);
			return;
		}
		writeFrameInternal(pkt, inputTimebase, outputTimebase, context, codecType);
	}

	/**
	 * Endpoints get added mid-stream, so source dts is far from zero and some ingests turn that
	 * into an equal amount of buffer. Rebase to zero, both streams off one origin or A/V skew
	 * shifts. Muxer has the same rebase, but its video half waits on firstKeyFrameReceived which
	 * only RecordMuxer and HLSMuxer clear, and its audio half is in the override we replace.
	 */
	private long captureFirstDts(AVPacket pkt, AVRational inputTimebase, int codecType) {
		if (codecType != AVMEDIA_TYPE_AUDIO && codecType != AVMEDIA_TYPE_VIDEO) {
			return 0;
		}
		boolean isAudio = codecType == AVMEDIA_TYPE_AUDIO;

		if (endpointFirstPacketDtsMs == -1) {
			endpointFirstPacketDtsMs = av_rescale_q(pkt.dts(), inputTimebase, MuxAdaptor.TIME_BASE_FOR_MS);
			if (isAudio) {
				endpointFirstAudioDts = pkt.dts();
			} else {
				endpointFirstVideoDts = pkt.dts();
			}
			logger.info("Rebasing {} push from first {} packet dts:{}ms for {}", muxerType,
					isAudio ? "audio" : "video", endpointFirstPacketDtsMs, url);
		}

		long firstDts = isAudio ? endpointFirstAudioDts : endpointFirstVideoDts;
		if (firstDts == -1) {
			firstDts = av_rescale_q(endpointFirstPacketDtsMs, MuxAdaptor.TIME_BASE_FOR_MS, inputTimebase);
			// The other stream can start behind the origin. Clamp so it can't go negative.
			if ((pkt.dts() - firstDts) < 0) {
				firstDts = pkt.dts();
			}

			if (isAudio) {
				endpointFirstAudioDts = firstDts;
			} else {
				endpointFirstVideoDts = firstDts;
			}
		}
		return firstDts;
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

		if (codecType == AVMEDIA_TYPE_VIDEO) {
			ret = av_packet_ref(getTmpPacket() , pkt);
			if (ret < 0) {
				setStatus(IAntMediaStreamHandler.BROADCAST_STATUS_ERROR);
				logger.error("Cannot copy packet for {}", file.getName());
				return;
			}
			if (!bsfFilterContextList.isEmpty() && bsfFilterContextList.get(0) != null) {
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
							// av_malloc'd, so the native context owns it from here and JavaCPP never frees it.
							BytePointer extradataCopy = new BytePointer(avutil.av_malloc(size.get() + AV_INPUT_BUFFER_PADDING_SIZE)).capacity(size.get() + AV_INPUT_BUFFER_PADDING_SIZE);
							byte[] extraDataArray = new byte[(int)size.get()];
							extradataBytePointer.get(extraDataArray, 0, extraDataArray.length);
							extradataCopy.put(extraDataArray, 0, extraDataArray.length);
							logger.info("extradata size:{} extradata: {} allocated pointer: {}", size.get(), extradataBytePointer, extradataCopy);
							context.streams(pkt.stream_index()).codecpar().extradata(extradataCopy);
							context.streams(pkt.stream_index()).codecpar().extradata_size((int)size.get());
							if (!writeHeader()) {
								// The failure freed the context, so `context` is dangling from here on.
								break;
							}
						}
					}

					if (headerWritten) {
						enqueueVideoPacket();
					} else {
						setStatus(IAntMediaStreamHandler.BROADCAST_STATUS_ERROR);
						logger.warn("Header is not written yet for writing video packet for stream: {}", file.getName());
					}
				}
			} else {
				enqueueVideoPacket();
			}
			av_packet_unref(getTmpPacket());
		} else if (codecType == AVMEDIA_TYPE_AUDIO && headerWritten) {
			av_packet_ref(getTmpPacket() , pkt);
			enqueuePacket(getTmpPacket());
			av_packet_unref(getTmpPacket());
		}

		pkt.pts(pts);
		pkt.dts(dts);
		pkt.duration(duration);
		pkt.pos(pos);
	}

	/**
	 * Runs on every video packet, even ones the policy drops: the decision lives in the engine,
	 * and the engine must not know about extradata.
	 *
	 * TODO: addExtradataIfRequired points tmpPacket.data() at a direct ByteBuffer nothing holds a
	 * reference to, while tmpPacket.buf() still points at the bsf buffer. av_packet_clone keeps
	 * the buf ref but copies the data pointer, so a queued clone can read freed memory. Encoder
	 * paths only. Fix in Muxer: give the payload an AVBufferRef the packet owns.
	 */
	private void enqueueVideoPacket() {
		addExtradataIfRequired(getTmpPacket(), (getTmpPacket().flags() & AV_PKT_FLAG_KEY) != 0);
		enqueuePacket(getTmpPacket());
	}

	/** Teardown can land between two packets, and submitting after it queues into a dead drain. */
	private void enqueuePacket(AVPacket pkt) {
		if (running && !cancelOpenIO.get()) {
			engine.submit(pkt);
		}
	}

	/**
	 * FFmpeg errors on both shapes and the republish path acts on that error, so dropping costs a
	 * frame of audio instead of a reconnect. Only ever audio: a resume leaves a few ms of arrival
	 * skew, and fixing it per stream would break A/V sync. Caller holds {@link #writeLock}.
	 */
	private boolean isWritable(AVPacket pkt) {
		long dts = pkt.dts();
		int index = pkt.stream_index();
		if (dts == avutil.AV_NOPTS_VALUE || index < 0 || index >= lastWrittenDts.length) {
			return true;
		}
		if (dts < 0 || (lastWrittenDts[index] != avutil.AV_NOPTS_VALUE && dts < lastWrittenDts[index])) {
			return false;
		}
		lastWrittenDts[index] = dts;
		return true;
	}

	/** Drains the queue to the endpoint. Runs on a vertx worker, never holds {@code this}. */
	private void drain() {
		AVPacket pkt;
		while (running && (pkt = engine.drainNext()) != null) {
			writeToEndpoint(pkt);
		}
	}

	private void writeToEndpoint(AVPacket pkt) {
		long startNanos = System.nanoTime();
		long pts = pkt.pts();
		long dts = pkt.dts();
		boolean wrote = false;
		try {
			synchronized (writeLock) {
				// running re-checked under the lock: teardown may have freed the context while we waited.
				if (running && outputFormatContext != null && outputFormatContext.pb() != null) {
					if (!isWritable(pkt)) {
						logPacketIssue("Dropping unwritable dts:{} on stream:{} for {}", dts, pkt.stream_index(), url);
						// A source dts reset makes every packet backward. Nothing reaches the
						// writer, so FFmpeg never errors and the endpoint goes silent for good.
						// Raise the error here now.
						if (++unwritableDtsCount > UNWRITABLE_LIMIT) {
							unwritableDtsCount = 0;
							logger.error("Endpoint {} cannot place any packet on its timeline", url);
							setStatus(IAntMediaStreamHandler.BROADCAST_STATUS_ERROR);
						}
						return;
					}
					unwritableDtsCount = 0;

					int ret = av_interleaved_write_frame(outputFormatContext, pkt);
					wrote = true;
					if (ret < 0) {
						logPacketIssue("Cannot write packet for stream:{} and url:{}. Packet pts:{} dts:{} Error is {}",
								streamId, getOutputURL(), pts, dts, getErrorDefinition(ret));
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
				analytics.recordWrite(System.nanoTime() - startNanos, dts, engine.size());
			}
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
	public EndpointMuxerAnalytics getAnalytics() {
		return analytics;
	}
}
