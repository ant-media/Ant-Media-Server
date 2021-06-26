package io.antmedia.muxer;


import static org.bytedeco.ffmpeg.global.avcodec.AV_INPUT_BUFFER_PADDING_SIZE;
import static org.bytedeco.ffmpeg.global.avcodec.AV_PKT_DATA_NEW_EXTRADATA;
import static org.bytedeco.ffmpeg.global.avcodec.AV_PKT_FLAG_KEY;
import static org.bytedeco.ffmpeg.global.avcodec.av_bsf_alloc;
import static org.bytedeco.ffmpeg.global.avcodec.av_bsf_free;
import static org.bytedeco.ffmpeg.global.avcodec.av_bsf_get_by_name;
import static org.bytedeco.ffmpeg.global.avcodec.av_bsf_init;
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
import static org.bytedeco.ffmpeg.global.avformat.avformat_free_context;
import static org.bytedeco.ffmpeg.global.avformat.avformat_new_stream;
import static org.bytedeco.ffmpeg.global.avformat.avformat_write_header;
import static org.bytedeco.ffmpeg.global.avformat.avio_closep;
import static org.bytedeco.ffmpeg.global.avutil.AVMEDIA_TYPE_AUDIO;
import static org.bytedeco.ffmpeg.global.avutil.AVMEDIA_TYPE_DATA;
import static org.bytedeco.ffmpeg.global.avutil.AVMEDIA_TYPE_VIDEO;
import static org.bytedeco.ffmpeg.global.avutil.AV_PIX_FMT_YUV420P;
import static org.bytedeco.ffmpeg.global.avutil.AV_ROUND_NEAR_INF;
import static org.bytedeco.ffmpeg.global.avutil.AV_ROUND_PASS_MINMAX;
import static org.bytedeco.ffmpeg.global.avutil.av_dict_free;
import static org.bytedeco.ffmpeg.global.avutil.av_dict_set;
import static org.bytedeco.ffmpeg.global.avutil.av_rescale_q;
import static org.bytedeco.ffmpeg.global.avutil.av_rescale_q_rnd;
import static org.bytedeco.ffmpeg.global.avutil.av_strerror;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.bytedeco.ffmpeg.avcodec.AVBSFContext;
import org.bytedeco.ffmpeg.avcodec.AVBitStreamFilter;
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
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.IntPointer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RtmpMuxer extends Muxer {

	protected static Logger logger = LoggerFactory.getLogger(RtmpMuxer.class);
	
	private String url;
	private AVPacket videoPkt;
	private Map<Integer, AVRational> codecTimeBaseMap = new HashMap<>();
	private AVBSFContext bsfExtractdataContext = null;
	private AVPacket tmpPacket;
	private volatile boolean headerWritten = false;
	private volatile boolean trailerWritten = false;
	private IEndpointStatusListener statusListener;


	private BytePointer allocatedExtraDataPointer = null;

	private String status = IAntMediaStreamHandler.BROADCAST_STATUS_CREATED;
	
	boolean keyFrameReceived = false;
	private int audioIndex;
	private int videoIndex;

	public RtmpMuxer(String url) {
		super(null);
		format = "flv";
		this.url = url;

		videoPkt = avcodec.av_packet_alloc();
		av_init_packet(videoPkt);

		tmpPacket = avcodec.av_packet_alloc();
		av_init_packet(tmpPacket);
	}

	public String getURL() {
		return url;
	}



	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized boolean addStream(AVCodec codec, AVCodecContext codecContext, int streamIndex) {

		AVFormatContext outputContext = getOutputFormatContext();

		if (outputContext == null) {
			setStatus(IAntMediaStreamHandler.BROADCAST_STATUS_FAILED);
			return false;
		}
		registeredStreamIndexList.add(streamIndex);
		AVStream outStream = avformat_new_stream(outputContext, codec);		
		outStream.time_base(codecContext.time_base());

		int ret = avcodec_parameters_from_context(outStream.codecpar(), codecContext);

		if (ret < 0) {
			setStatus(IAntMediaStreamHandler.BROADCAST_STATUS_FAILED);
			logger.info("codec context cannot be copied for url: {}", url);
		}
		outStream.codecpar().codec_tag(0);
		codecTimeBaseMap.put(streamIndex, codecContext.time_base());
		logger.info("Adding stream index:{} for stream:{} codec type:{}", streamIndex, url, codecContext.codec_type());
		setStatus(IAntMediaStreamHandler.BROADCAST_STATUS_PREPARING);
		return true;

	}
	public void setStatusListener(IEndpointStatusListener listener){
		this.statusListener = listener;
	}

	private AVFormatContext getOutputFormatContext() {
		if (outputFormatContext == null) {
			logger.info("Filling outputFormatContext");
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
	public  boolean prepareIO() 
	{
		/*
		 * extradata context is created if addVideoStream is called from WebRTC Forwarder 
		 */
		AVFormatContext context = getOutputFormatContext();
		if (context != null && context.pb() != null) {
			//return false if it is already prepared
			return false;
		}

		AVIOContext pb = new AVIOContext(null);

		long startTime = System.currentTimeMillis();
		logger.info("rtmp muxer opening: {} time:{}" , url, System.currentTimeMillis());
		int ret = avformat.avio_open(pb,  url, AVIO_FLAG_WRITE);
		if (ret < 0) {
			setStatus(IAntMediaStreamHandler.BROADCAST_STATUS_FAILED);
			logger.error("Could not open output file for rtmp url {}", url);
			return false;
		}
		context.pb(pb);
		long diff = System.currentTimeMillis() - startTime;
		logger.info("avio open takes {}", diff);


		if (bsfExtractdataContext == null)  
		{	
			return writeHeader(); 
		}
		isRunning.set(true);
		setStatus(IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING);
		return true;
	}

	/**
	 * If the broadcast is stopped while the muxer is writing the header
	 * it cannot complete writing the header
	 * Then writeTrailer causes crash because of memory problem.
	 * We need to control if header is written before trying to write Trailer and synchronize them.
	 */
	private synchronized boolean writeHeader() {
		if(!trailerWritten) {
			long startTime = System.currentTimeMillis();
			AVDictionary optionsDictionary = null;

			if (!options.isEmpty()) {
				optionsDictionary = new AVDictionary();
				Set<String> keySet = options.keySet();
				for (String key : keySet) {
					av_dict_set(optionsDictionary, key, options.get(key), 0);
				}
			}

			logger.info("before writing rtmp muxer header to {}", url);
			int ret = avformat_write_header(getOutputFormatContext(), optionsDictionary);
			if (ret < 0) {
				setStatus(IAntMediaStreamHandler.BROADCAST_STATUS_FAILED);
				logger.warn("could not write header to rtmp url {}", url);

				clearResource();
				return false;
			}
			if (optionsDictionary != null) {
				av_dict_free(optionsDictionary);
				optionsDictionary = null;
			}
			long diff = System.currentTimeMillis() - startTime;
			logger.info("write header takes {}", diff);
			headerWritten = true;
			isRunning.set(true);
			setStatus(IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING);

			return true;
		}
		else{
			logger.info("Trying to write header after writing trailer");
			return false;
		}
	}

	/**
	 * {@inheritDoc}
	 * Look at the comments {@code writeHeader}
	 */
	@Override
	public synchronized void writeTrailer() {
		if (!isRunning.get() || outputFormatContext == null || outputFormatContext.pb() == null) {
			//return if it is already null
			logger.info("RTMPMuxer is not running or output context is null for stream: {}", url);
			return;
		}

		if(headerWritten){
			logger.info("Writing trailer for stream id: {}", url);
			isRunning.set(false);

			av_write_trailer(outputFormatContext);
			clearResource();
			setStatus(IAntMediaStreamHandler.BROADCAST_STATUS_FINISHED);
			isRecording = false;
			trailerWritten = true;
		}
		else{
			logger.info("Not writing trailer because header is not written yet");
		}
	}


	private void clearResource() {
		/* close output */
		if ((outputFormatContext.flags() & AVFMT_NOFILE) == 0) {
			avio_closep(outputFormatContext.pb());
		}

		if (videoPkt != null) {
			av_packet_free(videoPkt);
			videoPkt = null;
		}

		if (tmpPacket != null) {
			av_packet_free(tmpPacket);
			tmpPacket = null;
		}

		if (audioPkt != null) {
			av_packet_free(audioPkt);
			audioPkt = null;
		}

		if (bsfExtractdataContext != null) {
			av_bsf_free(bsfExtractdataContext);
			bsfExtractdataContext = null;
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
		avformat_free_context(outputFormatContext);
		outputFormatContext.close();
		outputFormatContext = null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean addVideoStream(int width, int height, AVRational timebase, int codecId, int streamIndex, boolean isAVC, AVCodecParameters codecpar) {
		boolean result = false;
		AVFormatContext outputContext = getOutputFormatContext();
		if (outputContext != null) 
		{
			registeredStreamIndexList.add(streamIndex);
			AVStream outStream = avformat_new_stream(outputContext, null);
			outStream.codecpar().width(width);
			outStream.codecpar().height(height);
			outStream.codecpar().codec_id(codecId);
			outStream.codecpar().codec_type(AVMEDIA_TYPE_VIDEO);
			outStream.codecpar().format(AV_PIX_FMT_YUV420P);
			outStream.codecpar().codec_tag(0);
			outStream.codec().codec_tag(0);

			AVRational timeBase = new AVRational();
			timeBase.num(1).den(1000);


			AVBitStreamFilter h264bsfc = av_bsf_get_by_name("extract_extradata");
			bsfExtractdataContext = new AVBSFContext(null);

			int ret = av_bsf_alloc(h264bsfc, bsfExtractdataContext);
			if (ret < 0) {
				logger.info("cannot allocate bsf context for {}", file.getName());
				setStatus(IAntMediaStreamHandler.BROADCAST_STATUS_FAILED);
				outStream.close();
				timeBase.close();
				return false;
			}

			ret = avcodec_parameters_copy(bsfExtractdataContext.par_in(), outStream.codecpar());
			if (ret < 0) {
				logger.info("cannot copy input codec parameters for {}", file.getName());
				setStatus(IAntMediaStreamHandler.BROADCAST_STATUS_FAILED);
				outStream.close();
				timeBase.close();
				h264bsfc.close();
				return false;
			}
			bsfExtractdataContext.time_base_in(timeBase);

			ret = av_bsf_init(bsfExtractdataContext);
			if (ret < 0) {
				logger.info("cannot init bit stream filter context for {}", file.getName());
				setStatus(IAntMediaStreamHandler.BROADCAST_STATUS_FAILED);
				outStream.close();
				timeBase.close();
				h264bsfc.close();
				return false;
			}

			ret = avcodec_parameters_copy(outStream.codecpar(), bsfExtractdataContext.par_out());
			if (ret < 0) {
				logger.info("cannot copy codec parameters to output for {}", file.getName());
				setStatus(IAntMediaStreamHandler.BROADCAST_STATUS_FAILED);
				outStream.close();
				timeBase.close();
				h264bsfc.close();
				return false;
			}
			outStream.time_base(bsfExtractdataContext.time_base_out());

			codecTimeBaseMap.put(streamIndex, timeBase);
			logger.info("Adding video stream index:{} for stream:{}", streamIndex, url);
			result = true;
		}

		return result;
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized void writePacket(AVPacket pkt, AVStream stream) {
		if (!isRunning.get() || !registeredStreamIndexList.contains(pkt.stream_index()))  {
			if (time2log % 100 == 0) {
				logger.warn("not registered stream index {}", file.getName());
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
			logger.error("Undefined codec type for stream: {} ", url);
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
		if (!isRunning.get() || !registeredStreamIndexList.contains(pkt.stream_index()))  {
			if (time2log % 100 == 0) {
				logger.warn("not registered stream index {}", file.getName());
				time2log = 0;
			}
			time2log++;
			return;
		}
		
		AVStream outStream = outputFormatContext.streams(pkt.stream_index());
		AVRational codecTimebase = codecTimeBaseMap.get(pkt.stream_index());
		writePacket(pkt, codecTimebase,  outStream.time_base(), outStream.codecpar().codec_type()); 
	}


	private void writePacket(AVPacket pkt, final AVRational inputTimebase, final AVRational outputTimebase, int codecType) 
	{
		final AVFormatContext context = getOutputFormatContext();
		if (context.streams(pkt.stream_index()).codecpar().codec_type() ==  AVMEDIA_TYPE_AUDIO && !headerWritten) {
			//Opening the RTMP muxer may take some time and don't make audio queue increase
			logger.info("Not writing audio packet to muxer because header is not written yet for {}", url);
			return;
		}
		writeFrameInternal(pkt, inputTimebase, outputTimebase, context, codecType);
	}

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

		if (codecType == AVMEDIA_TYPE_VIDEO) {
			ret = av_packet_ref(tmpPacket , pkt);
			if (ret < 0) {
				setStatus(IAntMediaStreamHandler.BROADCAST_STATUS_ERROR);
				logger.error("Cannot copy packet for {}", file.getName());
				return;
			}
			if (bsfExtractdataContext != null) {

				ret = av_bsf_send_packet(bsfExtractdataContext, tmpPacket);
				if (ret < 0) {
					setStatus(IAntMediaStreamHandler.BROADCAST_STATUS_ERROR);
					logger.warn("cannot send packet to the filter");
					return;
				}

				while (av_bsf_receive_packet(bsfExtractdataContext, tmpPacket) == 0) 
				{
					if (!headerWritten) 
					{
						IntPointer size = new IntPointer(1);
						BytePointer extradataBytePointer = avcodec.av_packet_get_side_data(tmpPacket, AV_PKT_DATA_NEW_EXTRADATA,  size);
						if (size.get() != 0) 
						{
							allocatedExtraDataPointer = new BytePointer(avutil.av_malloc(size.get() + AV_INPUT_BUFFER_PADDING_SIZE)).capacity(size.get() + AV_INPUT_BUFFER_PADDING_SIZE);
							byte[] extraDataArray = new byte[size.get()];
							extradataBytePointer.get(extraDataArray, 0, extraDataArray.length);
							allocatedExtraDataPointer.put(extraDataArray, 0, extraDataArray.length);
							logger.info("extradata size:{} extradata: {} allocated pointer: {}", size.get(), extradataBytePointer, allocatedExtraDataPointer);
							context.streams(pkt.stream_index()).codecpar().extradata(allocatedExtraDataPointer);
							context.streams(pkt.stream_index()).codecpar().extradata_size(size.get());
							writeHeader();
							setStatus(IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING);
						}
					}

					if (headerWritten) {
						ret = av_write_frame(context, tmpPacket);
						if (ret < 0 && logger.isInfoEnabled()) {
							byte[] data = new byte[128];
							av_strerror(ret, data, data.length);
							setStatus(IAntMediaStreamHandler.BROADCAST_STATUS_ERROR);
							logger.info("cannot write video frame to muxer. Error: {} stream: {} pkt.dts: {}", new String(data, 0, data.length), file != null ? file.getName() : " no name", tmpPacket.dts());
						}
						else{
							setStatus(IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING);
						}
					}
					else {
						setStatus(IAntMediaStreamHandler.BROADCAST_STATUS_ERROR);
						logger.info("Header is not written yet for writing video packet for stream: {}", file.getName());
					}
				}
			}
			else 
			{
				ret = av_write_frame(context, tmpPacket);
				if (ret < 0 && logger.isInfoEnabled()) {
					byte[] data = new byte[128];
					av_strerror(ret, data, data.length);
					setStatus(IAntMediaStreamHandler.BROADCAST_STATUS_ERROR);
					logger.info("cannot write video frame to muxer. Error: {} stream: {} codec type: {} index: {} pkt.dts:{}", new String(data, 0, data.length), file != null ? file.getName() : "no name", codecType, pkt.stream_index(), tmpPacket.dts());
				}
			}
			av_packet_unref(tmpPacket);
		}
		else 
		{
			if (headerWritten) 
			{
				ret = av_write_frame(context, pkt);
				if (ret < 0 && logger.isInfoEnabled()) {
					byte[] data = new byte[128];
					av_strerror(ret, data, data.length);
					setStatus(IAntMediaStreamHandler.BROADCAST_STATUS_ERROR);
					logger.info("cannot write frame to muxer(not video). Error: {} stream: {} codec type: {} index: {} pkt.dts:{}", new String(data, 0, data.length), file != null ? file.getName() : "no name", codecType, pkt.stream_index(), pkt.dts());
				}
			}
		}

		pkt.pts(pts);
		pkt.dts(dts);
		pkt.duration(duration);
		pkt.pos(pos);
	}

	@Override
	public synchronized void writeVideoBuffer(ByteBuffer encodedVideoFrame, long dts, int frameRotation, int streamIndex,
			boolean isKeyFrame,long firstFrameTimeStamp, long pts) 
	{

		if (!isRunning.get() || !registeredStreamIndexList.contains(streamIndex)) {
			logger.info("Not writing to muxer because it's not started for {}", url);
			return;
		}

		if (!keyFrameReceived) {

			if (isKeyFrame) {
				keyFrameReceived = true;
				logger.info("Key frame is received to start");
			}
		}

		if (keyFrameReceived) {
			videoPkt.stream_index(streamIndex);
			videoPkt.pts(pts);
			videoPkt.dts(dts);

			encodedVideoFrame.rewind();
			if (isKeyFrame) {
				videoPkt.flags(videoPkt.flags() | AV_PKT_FLAG_KEY);
			}
			videoPkt.data(new BytePointer(encodedVideoFrame));
			videoPkt.size(encodedVideoFrame.limit());
			videoPkt.position(0);

			AVStream outStream = outputFormatContext.streams(videoPkt.stream_index());
			AVRational codecTimebase = codecTimeBaseMap.get(videoPkt.stream_index());
			writePacket(videoPkt, codecTimebase,  outStream.time_base(), outStream.codecpar().codec_type()); 

			av_packet_unref(videoPkt);
		}
	}


	@Override
	public boolean addStream(AVCodecParameters codecParameters, AVRational timebase, int streamIndex) {
		boolean result = false;
		AVFormatContext outputContext = getOutputFormatContext();
		if (outputContext != null 
				&& (codecParameters.codec_type() == AVMEDIA_TYPE_AUDIO || codecParameters.codec_type() == AVMEDIA_TYPE_VIDEO)) 
		{
			AVStream outStream = avformat_new_stream(outputContext, null);
			
			avcodec_parameters_copy(outStream.codecpar(), codecParameters);
			outStream.time_base(timebase);
			codecTimeBaseMap.put(outStream.index(), timebase);
			registeredStreamIndexList.add(streamIndex);
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

		return result;
	}
	

	
	

	@Override
	public void writeAudioBuffer(ByteBuffer audioFrame, int streamIndex, long timestamp) {

		if (!isRunning.get()) {
			if (time2log  % 100 == 0) {
				logger.warn("Not writing AudioBuffer for {} because Is running:{}", url, isRunning.get());
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
	


}
