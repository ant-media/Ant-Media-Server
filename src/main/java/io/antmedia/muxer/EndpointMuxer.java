package io.antmedia.muxer;


import static org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_AAC;
import static org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_H264;
import static org.bytedeco.ffmpeg.global.avcodec.AV_INPUT_BUFFER_PADDING_SIZE;
import static org.bytedeco.ffmpeg.global.avcodec.AV_PKT_DATA_NEW_EXTRADATA;
import static org.bytedeco.ffmpeg.global.avcodec.AV_PKT_FLAG_KEY;
import static org.bytedeco.ffmpeg.global.avcodec.av_bsf_receive_packet;
import static org.bytedeco.ffmpeg.global.avcodec.av_bsf_send_packet;
import static org.bytedeco.ffmpeg.global.avcodec.av_packet_ref;
import static org.bytedeco.ffmpeg.global.avcodec.av_packet_unref;
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_parameters_copy;
import static org.bytedeco.ffmpeg.global.avformat.av_interleaved_write_frame;
import static org.bytedeco.ffmpeg.global.avformat.avformat_alloc_output_context2;
import static org.bytedeco.ffmpeg.global.avutil.AVMEDIA_TYPE_AUDIO;
import static org.bytedeco.ffmpeg.global.avutil.AVMEDIA_TYPE_VIDEO;
import static org.bytedeco.ffmpeg.global.avutil.AV_ROUND_NEAR_INF;
import static org.bytedeco.ffmpeg.global.avutil.AV_ROUND_PASS_MINMAX;
import static org.bytedeco.ffmpeg.global.avutil.av_rescale_q;
import static org.bytedeco.ffmpeg.global.avutil.av_rescale_q_rnd;
import org.bytedeco.ffmpeg.global.avutil;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bytedeco.ffmpeg.avcodec.AVBSFContext;
import org.bytedeco.ffmpeg.avcodec.AVCodec;
import org.bytedeco.ffmpeg.avcodec.AVCodecContext;
import org.bytedeco.ffmpeg.avcodec.AVCodecParameters;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.avformat.AVStream;
import org.bytedeco.ffmpeg.avutil.AVRational;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.SizeTPointer;

import io.vertx.core.Vertx;

public class EndpointMuxer extends Muxer {

	private String url;
	private volatile boolean trailerWritten = false;
	private IEndpointStatusListener statusListener;

	private BytePointer allocatedExtraDataPointer = null;

	private String status = IAntMediaStreamHandler.BROADCAST_STATUS_CREATED;

	boolean keyFrameReceived = false;

	private AtomicBoolean preparedIO = new AtomicBoolean(false);
	private AtomicBoolean cancelOpenIO = new AtomicBoolean(false);

	public String muxerType = null;

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
		if(url == null)
			return;
		if(url.startsWith("rtmp")) {
			format = "flv";
			muxerType = "rtmp";
			// check if app name is present in the URL rtmp://Domain.com/AppName/StreamId
			String regex = "rtmp(s)?://[a-zA-Z0-9\\.-]+(:[0-9]+)?/([^/]+)/.*";

			Pattern rtmpAppName = Pattern.compile(regex);
			Matcher checkAppName = rtmpAppName.matcher(url);

			if (!checkAppName.matches()) {
				//this is the fix to send stream for urls without app
				setOption("rtmp_app", "");
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
	public void setStatus(String status)
	{

		if (!this.status.equals(status) && this.statusListener != null)
		{
			this.statusListener.endpointStatusUpdated(this.url, status);
		}
		this.status = status;
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
			logger.info("write header takes {} for rtmp:{} the bitstream filter name is {}", diff, getOutputURL(), getBitStreamFilter());
			
			headerWritten = true;
			setStatus(IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING);

			return true;
		}
		else{
			logger.warn("Trying to write header after writing trailer");
			return false;
		}
	}

	/**
	 * {@inheritDoc}
	 * Look at the comments {@code writeHeader}
	 */
	@Override
	public synchronized void writeTrailer() {
		cancelOpenIO.set(true);
		if(headerWritten){
			super.writeTrailer();
			trailerWritten = true;
		}
		else{
			logger.info("Not writing trailer because header is not written yet");
			clearResource();
		}
		setStatus(IAntMediaStreamHandler.BROADCAST_STATUS_FINISHED);
	}

	@Override
	public synchronized void clearResource() {
		super.clearResource();
		if (!headerWritten) {
			preparedIO.set(false);
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
		if (context.streams(pkt.stream_index()).codecpar().codec_type() ==  AVMEDIA_TYPE_AUDIO && !headerWritten) {
			//Opening the RTMP muxer may take some time and don't make audio queue increase
			logger.info("Not writing audio packet to muxer because header is not written yet for {}", url);
			return;
		}
		writeFrameInternal(pkt, inputTimebase, outputTimebase, context, codecType);
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
						
						avWriteFrame(pkt, context);
					}
					else {
						setStatus(IAntMediaStreamHandler.BROADCAST_STATUS_ERROR);
						logger.warn("Header is not written yet for writing video packet for stream: {}", file.getName());
					}
				}
			}
			else
			{
				avWriteFrame(pkt, context);
			}
			av_packet_unref(getTmpPacket());
		}
		else if (codecType == AVMEDIA_TYPE_AUDIO && headerWritten)
		{
			av_packet_ref(getTmpPacket() , pkt);
			ret = av_interleaved_write_frame(context, getTmpPacket());
			if (ret < 0 && logger.isInfoEnabled())
			{
				setStatus(IAntMediaStreamHandler.BROADCAST_STATUS_ERROR);
				logPacketIssue("Cannot write audio packet for stream:{} and url:{}. Packet pts:{} dts:{} and Error is {}", streamId, getOutputURL(), pkt.pts(), pkt.dts(), getErrorDefinition(ret));
			}
			else {
				setStatus(IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING);
				logPacketIssue("Write audio packet for stream:{} and url:{}. Packet pts:{} dts:{}", streamId, getOutputURL(), pkt.pts(), pkt.dts());

			}
			av_packet_unref(getTmpPacket());
		}

		pkt.pts(pts);
		pkt.dts(dts);
		pkt.duration(duration);
		pkt.pos(pos);
	}

	public void avWriteFrame(AVPacket pkt, AVFormatContext context) {
		int ret = 0;
		boolean isKeyFrame = false;
		if ((pkt.flags() & AV_PKT_FLAG_KEY) == 1) {
			isKeyFrame = true;
		}
		addExtradataIfRequired(pkt, isKeyFrame);
		
		ret = av_interleaved_write_frame(context, getTmpPacket());
		if (ret < 0 && logger.isInfoEnabled()) 
		{
			setStatus(IAntMediaStreamHandler.BROADCAST_STATUS_ERROR);
			logPacketIssue("Cannot write video packet for stream:{} and url:{}. Packet pts:{}, dts:{} Error is {}", streamId, getOutputURL(), pkt.pts(), pkt.dts(),  getErrorDefinition(ret));
			
		}
		else {
			logPacketIssue("Write video packet for stream:{} and url:{}. Packet pts:{}, dts:{}", streamId, getOutputURL(), pkt.pts(), pkt.dts());

			setStatus(IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING);
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
	

}
