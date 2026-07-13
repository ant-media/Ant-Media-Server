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

public class EndpointMuxer extends Muxer {

	/**
	 * ~3-5s of frames at typical FPS
	 **/
	private static final int PACKET_QUEUE_CAPACITY = 200;

	/** Sentinel posted to {@link #packetQueue} to wake the worker for shutdown. */
	private static final Object POISON_PILL = new Object();

	/**
	 * Must exceed rw_timeout so an in-flight blocking write isn't cut short —
	 * otherwise teardown frees the format context out from under the worker.
	 */
	private static final long WORKER_JOIN_TIMEOUT_MS = 20_000L;

	private String url;
	private volatile boolean trailerWritten = false;
	private IEndpointStatusListener statusListener;

	private BytePointer allocatedExtraDataPointer = null;

	private volatile String status = IAntMediaStreamHandler.BROADCAST_STATUS_CREATED;

	/**
	 * Dedicated lock for status mutation so the worker doesn't contend on
	 * other synchronized methods
	 */
	private final Object statusLock = new Object();

	private volatile boolean keyFrameReceived = false;

	private AtomicBoolean preparedIO = new AtomicBoolean(false);
	private AtomicBoolean cancelOpenIO = new AtomicBoolean(false);

	public String muxerType = null;

	/**
	 * Async write queue... producer clones, worker drains.
	 * */
	private final LinkedBlockingQueue<Object> packetQueue = new LinkedBlockingQueue<>(PACKET_QUEUE_CAPACITY);
	private volatile boolean isWorkerRunning = false;
	private Thread workerThread;

	/**
	 * Held around each native write and around teardown's free path. Safety net
	 * if {@link #shutdownWorkerAndJoin} times out with the worker still mid-write.
	 */
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
			startWorkerThread();
			setStatus(IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING);

			return true;
		}
		else{
			logger.warn("Trying to write header after writing trailer");
			return false;
		}
	}

	private void startWorkerThread() {
		if (isWorkerRunning) {
			return;
		}
		isWorkerRunning = true;
		String threadName = "EndpointMuxerWorker-" + (muxerType != null ? muxerType : "?") + "-" + streamId;
		workerThread = new Thread(this::processQueue, threadName);
		// Daemon so a leaked muxer can't block JVM shutdown. Graceful teardown
		// still flows through shutdownWorkerAndJoin → POISON_PILL.
		workerThread.setDaemon(true);
		workerThread.start();
		logger.info("Worker thread started: {}", threadName);
	}

	/**
	 * {@inheritDoc}
	 * Look at the comments {@code writeHeader}
	 */
	@Override
	public synchronized void writeTrailer() {
		cancelOpenIO.set(true);

		// Drain the worker (let it write any queued packets) and wait for it
		// to exit before we touch the native format context.
		shutdownWorkerAndJoin();

		// The writeLock is the safety net for the case the join times out.
		synchronized (writeLock) {
			if(headerWritten) {
				if (!trailerWritten) {
					super.writeTrailer();
					trailerWritten = true;
				}
			} else{
				logger.info("Not writing trailer because header is not written yet");
				super.clearResource();
				preparedIO.set(false);
			}
		}
		setStatus(IAntMediaStreamHandler.BROADCAST_STATUS_FINISHED);
	}

	@Override
	public synchronized void clearResource() {
		// Worker may still be holding a reference to outputFormatContext; signal
		// + join before super.clearResource frees it. No-op if the worker never
		// started or has already exited.
		shutdownWorkerAndJoin();

		// The writeLock for safety if worker is in the middle of write
		synchronized (writeLock) {
			super.clearResource();
			if (!headerWritten) {
				preparedIO.set(false);
			}
		}

		/**
		 *  Don't free the allocatedExtraDataPointer because it's internally deallocated
		 *
		 * if (allocatedExtraDataPointer != null) {
		 *	avutil.av_free(allocatedExtraDataPointer);
		 *	allocatedExtraDataPointer = null;
		 * }
		 */

		//allocatedExtraDataPointer is freed when the context is closing
	}

	/**
	 * Idempotent (safe to call however may times)
	 * Join may take up to {@link #WORKER_JOIN_TIMEOUT_MS} on a slow remote.
	 **/
	private void shutdownWorkerAndJoin() {
		Thread t;
		synchronized (this) {
			if (isWorkerRunning) {
				isWorkerRunning = false;
				if (!packetQueue.offer(POISON_PILL)) {
					// Queue full: free one slot so the wake-up sentinel lands.
					Object stale = packetQueue.poll();
					if (stale instanceof AVPacket) {
						av_packet_free((AVPacket) stale);
					}
					if (!packetQueue.offer(POISON_PILL)) {
						logger.debug("Could not enqueue poison pill for {}, relying on poll timeout", url);
					}
				}
			}
			t = workerThread;
		}

		if (t == null || t == Thread.currentThread() || !t.isAlive()) {
			return;
		}

		try {
			t.join(WORKER_JOIN_TIMEOUT_MS);
			if (t.isAlive()) {
				logger.warn("Worker thread did not exit within {}ms for {}", WORKER_JOIN_TIMEOUT_MS, url);
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
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

		pkt.pts(av_rescale_q_rnd(pkt.pts(), inputTimebase, outputTimebase, AV_ROUND_NEAR_INF|AV_ROUND_PASS_MINMAX));
		pkt.dts(av_rescale_q_rnd(pkt.dts(), inputTimebase, outputTimebase, AV_ROUND_NEAR_INF|AV_ROUND_PASS_MINMAX));
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
	 * Clones {@code src} (which shares data with {@link #getTmpPacket()}
	 * {@link #addExtradataIfRequired} mutates tmpPacket directly) and hands it
	 * to the worker queue.
	 */
	private void enqueuePacket(AVPacket src, AVFormatContext context) {
		if (!isWorkerRunning || cancelOpenIO.get()) {
			return;
		}

		boolean isKeyFrame = (src.flags() & AV_PKT_FLAG_KEY) != 0;
		if (context.streams(src.stream_index()).codecpar().codec_type() == AVMEDIA_TYPE_VIDEO) {
			addExtradataIfRequired(src, isKeyFrame);
		}

		AVPacket clone = av_packet_clone(src);
		if (clone == null) {
			return;
		}

		// Head-drop on full: oldest out, new in. Keeps the receiver close to live.
		// TODO: GOP-aware drop (preserve keyframes) — see fix/RtmpMuxer-ThreadedQueue.
		if (!packetQueue.offer(clone)) {
			Object stale = packetQueue.poll();
			if (stale instanceof AVPacket) {
				av_packet_free((AVPacket) stale);
			}
			if (!packetQueue.offer(clone)) {
				av_packet_free(clone);
			}
			analytics.recordDrop(packetQueue.size());
		}
	}

	/**
	 * Worker loop.
	 * Holds {@link #writeLock} per native write; never acquires
	 * {@code this}, since teardown holds it while joining us.
	 */
	private void processQueue() {
		try {
			while (isWorkerRunning) {
				Object item;
				try {
					item = packetQueue.poll(10, TimeUnit.MILLISECONDS);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					break;
				}
				if (item == null) {
					continue;
				}
				if (item == POISON_PILL) {
					break;
				}

				AVPacket pkt = (AVPacket) item;
				long writeStartNanos = System.nanoTime();
				long pktDts = pkt.dts();
				boolean wrote = false;
				try {
					synchronized (writeLock) {
						// Teardown may have signalled shutdown while we waited on the lock.
						if (!isWorkerRunning) {
							break;
						}
						if (outputFormatContext != null && outputFormatContext.pb() != null) {
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
					logger.error("Worker write error for {}: {}", url, e.toString());
				} finally {
					if (wrote) {
						analytics.recordWrite(System.nanoTime() - writeStartNanos, pktDts, packetQueue.size());
					}
					av_packet_free(pkt);
				}
			}
		} finally {
			drainQueue();
		}
	}

	private void drainQueue() {
		Object item;
		while ((item = packetQueue.poll()) != null) {
			if (item instanceof AVPacket) {
				av_packet_free((AVPacket) item);
			}
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
	 * {@link #recordWrite} is worker-thread only (plain fields).
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
		/** EWMA of per-window averages — multi-window baseline for spike detection. */
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
		 * Worker-thread only — fields are unsynchronized by design.
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
