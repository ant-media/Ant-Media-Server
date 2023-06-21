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
import static org.bytedeco.ffmpeg.global.avformat.av_write_frame;
import static org.bytedeco.ffmpeg.global.avformat.avformat_alloc_output_context2;
import static org.bytedeco.ffmpeg.global.avutil.AVMEDIA_TYPE_AUDIO;
import static org.bytedeco.ffmpeg.global.avutil.AVMEDIA_TYPE_VIDEO;
import static org.bytedeco.ffmpeg.global.avutil.AV_ROUND_NEAR_INF;
import static org.bytedeco.ffmpeg.global.avutil.AV_ROUND_PASS_MINMAX;
import static org.bytedeco.ffmpeg.global.avutil.av_rescale_q;
import static org.bytedeco.ffmpeg.global.avutil.av_rescale_q_rnd;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

import org.bytedeco.ffmpeg.avcodec.AVBSFContext;
import org.bytedeco.ffmpeg.avcodec.AVCodec;
import org.bytedeco.ffmpeg.avcodec.AVCodecContext;
import org.bytedeco.ffmpeg.avcodec.AVCodecParameters;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.avformat.AVStream;
import org.bytedeco.ffmpeg.avutil.AVRational;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.SizeTPointer;

import io.vertx.core.Vertx;

public class RtmpMuxer extends Muxer {

	private String url;
	private volatile boolean trailerWritten = false;
	private IEndpointStatusListener statusListener;

	private BytePointer allocatedExtraDataPointer = null;

	private String status = IAntMediaStreamHandler.BROADCAST_STATUS_CREATED;

	boolean keyFrameReceived = false;

	private AtomicBoolean preparedIO = new AtomicBoolean(false);

	public RtmpMuxer(String url, Vertx vertx) {
		super(vertx);
		format = "flv";
		this.url = url;
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
		boolean result = false;
		//if there is a stream in the output format context, try to push
		if (getOutputFormatContext().nb_streams() > 0) 
		{
			this.vertx.executeBlocking(b ->
			{
	
				if (openIO())
				{
					if (videoBsfFilterContext == null)
					{
						writeHeader();
						return;
					}
					isRunning.set(true);
					setStatus(IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING);
				}
				else
				{
					clearResource();
					setStatus(IAntMediaStreamHandler.BROADCAST_STATUS_FAILED);
					logger.error("Cannot initializeOutputFormatContextIO for rtmp endpoint:{}", url);
				}
			}, null);
			
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
			logger.info("write header takes {} for rtmp:{}", diff, getOutputURL());
			
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
		if(headerWritten){
			super.writeTrailer();
			trailerWritten = true;
		}
		else{
			logger.info("Not writing trailer because header is not written yet");
		}
		setStatus(IAntMediaStreamHandler.BROADCAST_STATUS_FINISHED);
	}

	@Override
	public synchronized void clearResource() {
		super.clearResource();
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
	 * {@inheritDoc}
	 */
	@Override
	public synchronized boolean addVideoStream(int width, int height, AVRational timebase, int codecId, int streamIndex, boolean isAVC, AVCodecParameters codecpar) {
		
		boolean result = super.addVideoStream(width, height, timebase, codecId, streamIndex, isAVC, codecpar);
		if (result) 
		{
			AVStream outStream = getOutputFormatContext().streams(inputOutputStreamIndexMap.get(streamIndex));
			
			setBitstreamFilter("extract_extradata");
			
			AVBSFContext avbsfContext = initVideoBitstreamFilter(outStream.codecpar(), inputTimeBaseMap.get(streamIndex));
			
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

	private synchronized void writeFrameInternal(AVPacket pkt, AVRational inputTimebase, AVRational outputTimebase,
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
			if (videoBsfFilterContext != null)
			{
				ret = av_bsf_send_packet(videoBsfFilterContext, getTmpPacket());
				if (ret < 0) {
					setStatus(IAntMediaStreamHandler.BROADCAST_STATUS_ERROR);
					logger.warn("cannot send packet to the filter");
					return;
				}

				while (av_bsf_receive_packet(videoBsfFilterContext, getTmpPacket()) == 0)
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
			ret = av_write_frame(context, pkt);
			if (ret < 0 && logger.isInfoEnabled())
			{
				setStatus(IAntMediaStreamHandler.BROADCAST_STATUS_ERROR);
				logPacketIssue("Cannot write audio packet for stream:{} and url:{}. Error is {}", streamId, getOutputURL(), getErrorDefinition(ret));
			}
			else {
				setStatus(IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING);
			}
		}

		pkt.pts(pts);
		pkt.dts(dts);
		pkt.duration(duration);
		pkt.pos(pos);
	}

	public void avWriteFrame(AVPacket pkt, AVFormatContext context) {
		int ret;
		boolean isKeyFrame = false;
		if ((pkt.flags() & AV_PKT_FLAG_KEY) == 1) {
			isKeyFrame = true;
		}
		addExtradataIfRequired(pkt, isKeyFrame);
		
		ret = av_write_frame(context, getTmpPacket());
		if (ret < 0 && logger.isInfoEnabled()) 
		{
			setStatus(IAntMediaStreamHandler.BROADCAST_STATUS_ERROR);
			logPacketIssue("Cannot write video packet for stream:{} and url:{}. Error is {}", streamId, getOutputURL(), getErrorDefinition(ret));
			
		}
		else {
			setStatus(IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING);
		}
	}


	@Override
	public synchronized void writeVideoBuffer(ByteBuffer encodedVideoFrame, long dts, int frameRotation, int streamIndex,
			boolean isKeyFrame,long firstFrameTimeStamp, long pts)
	{

		if (!isRunning.get() || !registeredStreamIndexList.contains(streamIndex)) {
			logPacketIssue("Not writing to RTMP muxer because it's not started for {}", url);
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
	

}
