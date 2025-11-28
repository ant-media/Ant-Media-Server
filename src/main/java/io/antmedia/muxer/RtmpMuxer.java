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
import static org.bytedeco.ffmpeg.global.avformat.av_write_frame;
import static org.bytedeco.ffmpeg.global.avformat.avformat_alloc_output_context2;
import static org.bytedeco.ffmpeg.global.avutil.AVMEDIA_TYPE_AUDIO;
import static org.bytedeco.ffmpeg.global.avutil.AVMEDIA_TYPE_VIDEO;
import static org.bytedeco.ffmpeg.global.avutil.AV_ROUND_NEAR_INF;
import static org.bytedeco.ffmpeg.global.avutil.AV_ROUND_PASS_MINMAX;
import static org.bytedeco.ffmpeg.global.avutil.av_rescale_q;
import static org.bytedeco.ffmpeg.global.avutil.av_rescale_q_rnd;

import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bytedeco.ffmpeg.avcodec.AVCodec;
import org.bytedeco.ffmpeg.avcodec.AVCodecContext;
import org.bytedeco.ffmpeg.avcodec.AVCodecParameters;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avcodec.AVBSFContext;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.avformat.AVStream;
import org.bytedeco.ffmpeg.avutil.AVRational;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.SizeTPointer;

import io.vertx.core.Vertx;

public class RtmpMuxer extends Muxer {
	protected static final int QUEUE_CAPACITY = 200;


	private String url;
	private volatile boolean trailerWritten = false;
	private IEndpointStatusListener statusListener;

	private BytePointer allocatedExtraDataPointer = null;

	private volatile String status = IAntMediaStreamHandler.BROADCAST_STATUS_CREATED;

	private volatile boolean keyFrameReceived = false;

	private AtomicBoolean preparedIO = new AtomicBoolean(false);

	// --- ASYNC & DROPPING FIELDS ---
	// Capacity 200 packets (approx 3-5 sec).
	protected LinkedBlockingQueue<Object> packetQueue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);
	private Thread workerThread;
	protected volatile boolean isWorkerRunning = false;
	protected boolean droppingPframes = false;
	private static final Object POISON_PILL = new Object();

	public RtmpMuxer(String url, Vertx vertx) {
		super(vertx);
		format = "flv";
		this.url = url;

		parseRtmpURL(this.url);

		// Prevents FFmpeg from writing duration and file size metadata in the FLV header.
		setOption("flvflags", "no_duration_filesize");

		// Tells ffmpeg we are working with live streams
		setOption("rtmp_live", "live");

		// Disable internal buffering so that we hit network immediately
		setOption("rtmp_buffer", "0");

		// Disable Nagle's algorithm - critical for low latency
		setOption("tcp_nodelay", "1");

		// Maximize chunk size (64KB) to reduce CPU overhead and header bloat
		setOption("rtmp_maxchunk", "65536");
	}

	void parseRtmpURL(String url){
		if(url == null)
			return;
		String regex = "rtmp(s)?://[a-zA-Z0-9\\.-]+(:[0-9]+)?/([^/]+)/.*";

		Pattern rtmpAppName = Pattern.compile(regex);
		Matcher checkAppName = rtmpAppName.matcher(url);

		if (!checkAppName.matches()) {
			setOption("rtmp_app","");
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
			outputFormatContext= new AVFormatContext(null);
			int ret = this.avFormatAllocOutputContext2Wrapper();
			if (ret < 0) {
				setStatus(IAntMediaStreamHandler.BROADCAST_STATUS_FAILED);
				logger.info("Could not create output context for url {}", url);
				return null;
			}
		}
		return outputFormatContext;
	}

	public synchronized void setStatus(String status)
	{
		if (!this.status.equals(status) && this.statusListener != null)
		{
			this.statusListener.endpointStatusUpdated(this.url, status);
		}
		this.status = status;
	}
	
	public synchronized String getStatus(){
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
		boolean result = false;
		//if there is a stream in the output format context, try to push
		if (getStreamCount() > 0) {
			this.vertx.executeBlocking(() -> {
				if (openIO()) {
					if (bsfFilterContextList.isEmpty()) {
						writeHeader();
						return null;
					}
					isRunning.set(true);
					setStatus(IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING);
				} else {
					clearResource();
					setStatus(IAntMediaStreamHandler.BROADCAST_STATUS_FAILED);
					logger.error("Cannot initializeOutputFormatContextIO for rtmp endpoint:{}", url);
				}
				return null;
			}, false);

			result = true;
		} else {
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
		writeSuperHeader();
		long diff = System.currentTimeMillis() - startTime;
		logger.info("write header takes {} for rtmp:{} the bitstream filter name is {}", diff, getOutputURL(), getBitStreamFilter());

		headerWritten = true;

		// --- START WORKER THREAD ---
		this.startWorkerThread();

		setStatus(IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING);
		return true;
	}

	@Override
	public synchronized void writeTrailer() {
		if (!headerWritten) {
			logger.info("Not writing trailer because header is not written yet");
			return;
		}

		if (isWorkerRunning) {
			// Queue the Poison Pill. The worker will call the native trailer write.
			packetQueue.offer(POISON_PILL);
			isWorkerRunning = false;

			// REMAINDER: This writeTrailer method will be called once again, after thread has completed, and enter '!trailerWritten' condition.
		} else if (!trailerWritten) {
			superWriteTrailer();
			trailerWritten = true;
		}

		setStatus(IAntMediaStreamHandler.BROADCAST_STATUS_FINISHED);
	}

	@Override
	public synchronized void clearResource() {
		if (isWorkerRunning) {
			isWorkerRunning = false;
			packetQueue.offer(POISON_PILL);
		}

		try {
			if (workerThread != null) {
				// Wait for worker to finish writing buffer/trailer.
				workerThread.join(1000);
			}
		} catch (InterruptedException e) {
			logger.warn("RtmpMuxer interrupted waiting for close: {}", url);
			Thread.currentThread().interrupt();
		}

		super.clearResource();
	}

	@Override
	public synchronized boolean addVideoStream(int width, int height, AVRational timebase, int codecId, int streamIndex, boolean isAVC, AVCodecParameters codecpar) {
		
		boolean result = super.addVideoStream(width, height, timebase, codecId, streamIndex, isAVC, codecpar);
		if (result) 
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
		if (context.streams(pkt.stream_index()).codecpar().codec_type() ==  AVMEDIA_TYPE_AUDIO && !headerWritten) {
			logger.info("Not writing audio packet to muxer because header is not written yet for {}", url);
			return;
		}
		writeFrameInternal(pkt, inputTimebase, outputTimebase, context, codecType);
	}

	public void startWorkerThread() {
		isWorkerRunning = true;
		workerThread = new Thread(this::processQueue, "RtmpMuxerWorker-" + System.currentTimeMillis());
		workerThread.start();
		logger.info("RtmpMuxer worker thread started for: {}", url);
	}

	/**
	 * This method runs on the MAIN VERT.X THREAD.
	 * Handles timestamps and BSF. Must not block on I/O.
	 */
	private synchronized void writeFrameInternal(AVPacket pkt, AVRational inputTimebase, AVRational outputTimebase,
												 AVFormatContext context, int codecType) {

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
				logger.error("Cannot copy packet for {}", url);
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

				while (av_bsf_receive_packet(bsfFilterContextList.get(0), getTmpPacket()) == 0)
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

					if (headerWritten) {
						enqueuePacket(getTmpPacket(), context);
					} else {
						setStatus(IAntMediaStreamHandler.BROADCAST_STATUS_ERROR);
						logger.warn("Header is not written yet for writing video packet for stream: {}", url);
					}
				}
			} else {
				enqueuePacket(pkt, context);
			}
			av_packet_unref(getTmpPacket());
		} else if (codecType == AVMEDIA_TYPE_AUDIO && headerWritten) {
			av_packet_ref(getTmpPacket() , pkt);
			enqueuePacket(getTmpPacket(), context);
			setStatus(IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING);
			av_packet_unref(getTmpPacket());
		}

		// Restore timestamps for caller
		pkt.pts(pts);
		pkt.dts(dts);
		pkt.duration(duration);
		pkt.pos(pos);
	}


	/**
	 * Logic Flow:
	 * 1. Atomic GOP Recovery: If we previously dropped a P-frame due to congestion, we reject ALL
	 * subsequent P-frames until a new Keyframe arrives. This prevents visual artifacts ("glitching").
	 * <p>
	 * 2. Normal Operation: Try to add packet to queue. If successful, return.
	 * <p>
	 * 3. Congestion Handling (Queue Full For a longer time, when destination constantly can't keep up):
	 * - Case A (P-Frame): Drop the new packet (Tail Drop). Set 'droppingPframes' flag.
	 * - Case B (Keyframe/Audio): We must insert this VIP packet. We make room by dropping from Head.
	 * - If Head is a Keyframe: Dropping it corrupts the buffered GOP. We "Smart Seek" by draining
	 * packets from the Head until we reach the *next* Keyframe, performing a clean time-skip.
	 * - If Head is P-Frame/Audio: Simple Head Drop.
	 */
	private void enqueuePacket(AVPacket pkt, AVFormatContext context) {
		if (!isWorkerRunning) return;

		boolean isVideo = (context.streams(pkt.stream_index()).codecpar().codec_type() == AVMEDIA_TYPE_VIDEO);
		boolean isKeyFrame = (pkt.flags() & AV_PKT_FLAG_KEY) != 0;

		if (isVideo) {
			addExtradataIfRequired(pkt, isKeyFrame);
		}

		// 1. ATOMIC GOP RECOVERY Check
		if (isVideo && droppingPframes) {
			if (isKeyFrame) {
				droppingPframes = false; // Found a restart point
				logger.info("RtmpMuxer: Keyframe received. Recovering stream for {}", url);
			} else {
				// Drop P-frame to avoid glitches
				return;
			}
		}

		AVPacket clone = av_packet_clone(pkt);
		if (clone == null) return;

		// 2. TRY NORMAL INSERT
		if (packetQueue.offer(clone)) {
			return; // Success
		}

		// 3. QUEUE FULL LOGIC

		// Case A: Tail Drop (P-Frames)
		if (isVideo && !isKeyFrame) {
			av_packet_free(clone);
			droppingPframes = true; // Start dropping P-frames until next Keyframe
			return;
		}

		// Case B: Force Insert (Audio or Video Keyframe)
		// We need to make room at the HEAD.

		Object head = packetQueue.peek();
		boolean headIsVideoKeyFrame = false;

		if (head instanceof AVPacket) {
			AVPacket headPkt = (AVPacket) head;
			if (context.streams(headPkt.stream_index()).codecpar().codec_type() == AVMEDIA_TYPE_VIDEO &&
				(headPkt.flags() & AV_PKT_FLAG_KEY) != 0
			) {
				headIsVideoKeyFrame = true;
			}
		}

		if (headIsVideoKeyFrame) {
			// SMART SEEK: Drop Head Keyframe AND all subsequent packets until next Keyframe
			logger.warn("RtmpMuxer: Queue Full. Smart Seeking to next Keyframe.");

			// Drop the head keyframe
			Object dropped = packetQueue.poll();
			if (dropped instanceof AVPacket) av_packet_free((AVPacket)dropped);

			// Drain until next Keyframe
			while (true) {
				Object next = packetQueue.peek();
				if (next == null) break; // Queue emptied

				boolean nextIsKey = false;
				if (next instanceof AVPacket) {
					AVPacket nextPkt = (AVPacket) next;
					if (context.streams(nextPkt.stream_index()).codecpar().codec_type() == AVMEDIA_TYPE_VIDEO) {
						if ((nextPkt.flags() & AV_PKT_FLAG_KEY) != 0) {
							nextIsKey = true;
						}
					}
				}

				if (nextIsKey) {
					break; // Found valid restart point
				}

				// Drop intermediate packet (Audio or P-frame)
				Object removed = packetQueue.poll();
				if (removed instanceof AVPacket) av_packet_free((AVPacket)removed);
			}
		} else {
			// SIMPLE HEAD DROP: Head is Audio or P-frame. Safe to drop.
			Object dropped = packetQueue.poll();
			if (dropped instanceof AVPacket) av_packet_free((AVPacket)dropped);
		}

		// Insert VIP packet
		if (!packetQueue.offer(clone)) {
			av_packet_free(clone);
			logger.error("RtmpMuxer: Failed to force insert VIP packet.");
		}
	}

	// --- WORKER THREAD (UNSYNCHRONIZED) ---
	private void processQueue() {
		while (isWorkerRunning) {
			try {
				Object item = packetQueue.poll(50, TimeUnit.MILLISECONDS);
				if (item == null) continue;

				if (item == POISON_PILL || !isWorkerRunning) {
					this.writeTrailer();
					break;
				}

				if (!(item instanceof AVPacket)) {
					continue;
				}

				AVPacket pkt = (AVPacket) item;
				if (outputFormatContext != null && outputFormatContext.pb() != null) {
					// CHANGE: Use raw write to bypass internal buffers
					int ret = av_write_frame(outputFormatContext, pkt);
					if (ret < 0) {
						if (logger.isDebugEnabled()) {
							logPacketIssue("Cannot write video packet for stream:{} and url:{}. Packet pts:{}, dts:{} Error is {}", streamId, getOutputURL(), pkt.pts(), pkt.dts(),  getErrorDefinition(ret));
						}

						setStatus(IAntMediaStreamHandler.BROADCAST_STATUS_ERROR);
					} else if (!IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING.equals(this.status)) {
						setStatus(IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING);
					}
				}

				av_packet_free(pkt);
			} catch (Exception e) {
				logger.error("RtmpMuxer worker error", e);
			}
		}

		if (!trailerWritten) {
			this.writeTrailer();
		}
	}

	@Override
	public synchronized void writeVideoBuffer(ByteBuffer encodedVideoFrame, long dts, int frameRotation, int streamIndex,
											  boolean isKeyFrame,long firstFrameTimeStamp, long pts)
	{

		if (!isRunning.get() || !registeredStreamIndexList.contains(streamIndex)) {
			return;
		}

		if (!keyFrameReceived && isKeyFrame) {
			keyFrameReceived = true;
			logger.info("Key frame is received to start for rtmp:{}", url);
		}

		if (keyFrameReceived) {
			super.writeVideoBuffer(encodedVideoFrame, dts, frameRotation, streamIndex, isKeyFrame, firstFrameTimeStamp, pts);
		}
	}

	@Override
	public boolean isCodecSupported(int codecId) {
		return (codecId == AV_CODEC_ID_H264 || codecId == AV_CODEC_ID_AAC);
	}

	public int avFormatAllocOutputContext2Wrapper() {
		// We need this ugly wrapper, for testing purposes
		return avformat_alloc_output_context2(outputFormatContext, null, format, null);
	}

	public int getStreamCount() {
		// A one more ugly wrapper for native method, for testing purposes, since those can't be mocked :(
		return getOutputFormatContext().nb_streams();
	}

	public boolean writeSuperHeader() {
		// Also ugly wrapper for testing only
		return super.writeHeader();
	}

	public void superWriteTrailer() {
		super.writeTrailer();
	}
}