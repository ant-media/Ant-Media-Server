package com.antstreaming.rtsp;

import static org.bytedeco.javacpp.avcodec.AV_CODEC_FLAG_GLOBAL_HEADER;
import static org.bytedeco.javacpp.avcodec.av_packet_unref;
import static org.bytedeco.javacpp.avcodec.avcodec_parameters_copy;
import static org.bytedeco.javacpp.avformat.AVFMT_GLOBALHEADER;
import static org.bytedeco.javacpp.avformat.AVFMT_NOFILE;
import static org.bytedeco.javacpp.avformat.AVIO_FLAG_WRITE;
import static org.bytedeco.javacpp.avformat.av_find_input_format;
import static org.bytedeco.javacpp.avformat.av_interleaved_write_frame;
import static org.bytedeco.javacpp.avformat.av_read_frame;
import static org.bytedeco.javacpp.avformat.av_write_trailer;
import static org.bytedeco.javacpp.avformat.avformat_alloc_output_context2;
import static org.bytedeco.javacpp.avformat.avformat_close_input;
import static org.bytedeco.javacpp.avformat.avformat_find_stream_info;
import static org.bytedeco.javacpp.avformat.avformat_free_context;
import static org.bytedeco.javacpp.avformat.avformat_new_stream;
import static org.bytedeco.javacpp.avformat.avformat_open_input;
import static org.bytedeco.javacpp.avformat.avformat_write_header;
import static org.bytedeco.javacpp.avformat.avio_closep;
import static org.bytedeco.javacpp.avutil.AV_ROUND_NEAR_INF;
import static org.bytedeco.javacpp.avutil.AV_ROUND_PASS_MINMAX;
import static org.bytedeco.javacpp.avutil.av_dict_set;
import static org.bytedeco.javacpp.avutil.av_rescale_q;
import static org.bytedeco.javacpp.avutil.av_rescale_q_rnd;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Date;

import org.apache.mina.core.session.IoSession;
import org.bytedeco.javacpp.PointerPointer;
import org.bytedeco.javacpp.avcodec.AVPacket;
import org.bytedeco.javacpp.avformat;
import org.bytedeco.javacpp.avformat.AVFormatContext;
import org.bytedeco.javacpp.avformat.AVIOContext;
import org.bytedeco.javacpp.avformat.AVInputFormat;
import org.bytedeco.javacpp.avformat.AVStream;
import org.bytedeco.javacpp.avutil.AVDictionary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import com.antstreaming.rtsp.protocol.RtspCode;
import com.antstreaming.rtsp.protocol.RtspHeaderCode;
import com.antstreaming.rtsp.protocol.RtspResponse;
import com.antstreaming.rtsp.session.DateUtil;

public class PacketReceiverRunnable implements Runnable {

	private AVFormatContext inputFormatCtx;
	private long[] lastDTS;
	private AVFormatContext outputRTMPFormatContext;
	private boolean closeRequest = false;
	private File sdpFile;

	private Logger logger = LoggerFactory.getLogger(PacketReceiverRunnable.class);
	private String url;
	private StringBuilder liveStreamSdpDef;
	private String announcedStreamName;
	private IoSession session;
	private String sessionKey;
	private String cseq;
	private ThreadPoolTaskScheduler mTaskScheduler;


	public PacketReceiverRunnable(ThreadPoolTaskScheduler mTaskScheduler, String cseq, String sessionKey,
			IoSession session, String announcedStreamName, StringBuilder liveStreamSdpDef, String url) {
		this.mTaskScheduler = mTaskScheduler;
		this.cseq = cseq;
		this.sessionKey = sessionKey;
		this.session = session;
		this.announcedStreamName = announcedStreamName;
		this.liveStreamSdpDef = liveStreamSdpDef;
		this.url = url;
	}

	public boolean prepare_input_context(ThreadPoolTaskScheduler mTaskScheduler, final String cseq, final String sessionKey, final IoSession session, String announcedStreamName, StringBuilder liveStreamSdpDef, String streamUrl) {
		
		try {
			sdpFile = new File(announcedStreamName + ".sdp");
			
			try (OutputStream fos = new FileOutputStream(sdpFile)) {
				fos.write(liveStreamSdpDef.toString().getBytes());
			}

			inputFormatCtx = new AVFormatContext(null);
			

			AVDictionary options = new AVDictionary();
			int ret = av_dict_set(options, "protocol_whitelist", "file,crypto,udp,rtp", 0);
			if (ret < 0) {
				logger.debug("cannot set protocol_whitelist");
				return false;
			}
			
			AVInputFormat sdpFormat = av_find_input_format("sdp");


			if (avformat_open_input(inputFormatCtx, sdpFile.getAbsolutePath(), sdpFormat, options) != 0) {
				logger.debug("Could not open rtp for demuxing");
				return false;
			}
			
			mTaskScheduler.schedule(new Runnable() {
				@Override
				public void run() {
					RtspResponse response = new RtspResponse();
					response.setCode(RtspCode.OK);
					response.setHeader(RtspHeaderCode.CSEQ, cseq);
					response.setHeader(RtspHeaderCode.DATE, DateUtil.getGmtDate());
					response.setHeader(RtspHeaderCode.SESSION, sessionKey);
					session.write(response);
				}
			}, new Date());

			if (avformat_find_stream_info(inputFormatCtx, (PointerPointer<?>)null) < 0) {
				logger.debug("Could not get stream info");
				return false;
			}


			outputRTMPFormatContext = new AVFormatContext(null);

			ret = avformat_alloc_output_context2(outputRTMPFormatContext, null, "flv", null);

			lastDTS = new long[this.inputFormatCtx.nb_streams()];
			for (int i=0; i < inputFormatCtx.nb_streams(); i++) {
				AVStream inStream = inputFormatCtx.streams(i);
				AVStream outStream = avformat_new_stream(outputRTMPFormatContext, inStream.codec().codec());

				
				ret = avcodec_parameters_copy(outStream.codecpar(), inStream.codecpar());
				
				if (ret < 0) {
					logger.debug("Cannot get codec parameters\n");
					return false;
				}

				outStream.codec().codec_tag(0);
				
				if ((outputRTMPFormatContext.oformat().flags() & AVFMT_GLOBALHEADER) != 0) {
					outStream.codec().flags(outStream.codec().flags() | AV_CODEC_FLAG_GLOBAL_HEADER);
				}
				
				//initialize last decoding time stamp reference value
				lastDTS[i] = -1;
			}


			if ((outputRTMPFormatContext.flags() & AVFMT_NOFILE) == 0) 
			{
				AVIOContext pb = new AVIOContext(null);

				URI url = new URI(streamUrl);
				String urlStr = "rtmp://" + url.getHost() + "/" + url.getPath();
				//				logger.debug("rtmp url: " + urlStr);
				
				ret = avformat.avio_open(pb,  urlStr, AVIO_FLAG_WRITE);
				outputRTMPFormatContext.pb(pb);

				ret = avformat_write_header(outputRTMPFormatContext, (AVDictionary)null);
				if (ret < 0) {
					logger.debug("Cannot write header to rtmp\n");
					return false;
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		

		return true;
	}
	
	//TODO: check that if process interrupts soomehow, there is no memory leakage

	@Override
	public void run() {


		if (!prepare_input_context(mTaskScheduler, cseq, sessionKey, session, announcedStreamName, liveStreamSdpDef, url)) {
			closeInternal();
			return;
		}
		
		while(true) {
			//TODO check garbage collection and memory allocations on jvisualvm
			AVPacket pkt = new AVPacket();

			int ret = av_read_frame(inputFormatCtx, pkt);
			if (ret<0) {
				closeInternal();
				break;
			}
			int packetIndex = pkt.stream_index();

			AVStream in_stream = inputFormatCtx.streams(packetIndex);
			AVStream out_stream2 = outputRTMPFormatContext.streams(packetIndex);
			
		//	if (pkt.dts() != avutil.AV_NOPTS_VALUE && lastDTS[packetIndex] == -1) {
		//		lastDTS[packetIndex] = pkt.dts();
		//	}
			if (pkt.dts() < 0) {
				continue;
			}
			
			if (lastDTS[packetIndex] >= pkt.dts()) {
				logger.warn("dts timestamps are not in correct order last dts:"  + lastDTS[packetIndex] 
						+ " current dts:" + pkt.dts() + " fixing problem by adding offset");

				pkt.dts(lastDTS[packetIndex] + 1);
			}

			lastDTS[packetIndex] = pkt.dts();
			if (pkt.dts() > pkt.pts()) {
				pkt.pts(pkt.dts());
			}

			//sending rtmp
			pkt.pts(av_rescale_q_rnd(pkt.pts(), in_stream.time_base(), out_stream2.time_base(), AV_ROUND_NEAR_INF|AV_ROUND_PASS_MINMAX));
			pkt.dts(av_rescale_q_rnd(pkt.dts(), in_stream.time_base(), out_stream2.time_base(), AV_ROUND_NEAR_INF|AV_ROUND_PASS_MINMAX));
			pkt.duration(av_rescale_q(pkt.duration(), in_stream.time_base(), out_stream2.time_base()));
			pkt.pos(-1);


			ret = av_interleaved_write_frame(outputRTMPFormatContext, pkt);
			if (ret < 0) {
				logger.debug("Error muxing rtmp packet\n");
			}

			av_packet_unref(pkt);

			if (closeRequest) {
				closeInternal();
				break;
			}
		}
	}

	public void closeInternal() {
		logger.warn("closing rtmp format context");
		av_write_trailer(outputRTMPFormatContext);


		logger.warn("closing rtsp input format context");
		// Close the video file
		if (inputFormatCtx != null) {
			avformat_close_input(inputFormatCtx);
		}


		// close output 
		if (outputRTMPFormatContext != null && ((outputRTMPFormatContext.oformat().flags() & AVFMT_NOFILE) == 0)) {
			logger.warn("closing rtmp format context pb");
			avio_closep(outputRTMPFormatContext.pb());
		}
		avformat_free_context(outputRTMPFormatContext);


		logger.debug("deleting sdpfile");
		if (sdpFile != null && sdpFile.exists()) {
			sdpFile.delete();
			sdpFile = null;
		}
	}

	public synchronized void closeMuxer() {
		closeRequest = true;

	}

}
