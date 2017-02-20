package com.antstreaming.rtsp;

import static org.bytedeco.javacpp.avcodec.AV_CODEC_FLAG_GLOBAL_HEADER;
import static org.bytedeco.javacpp.avcodec.av_packet_unref;
import static org.bytedeco.javacpp.avcodec.avcodec_parameters_from_context;
import static org.bytedeco.javacpp.avcodec.avcodec_parameters_to_context;
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
import java.net.URI;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.mina.core.session.IoSession;
import org.bytedeco.javacpp.PointerPointer;
import org.bytedeco.javacpp.avformat;
import org.bytedeco.javacpp.avcodec.AVCodecParameters;
import org.bytedeco.javacpp.avcodec.AVPacket;
import org.bytedeco.javacpp.avformat.AVFormatContext;
import org.bytedeco.javacpp.avformat.AVIOContext;
import org.bytedeco.javacpp.avformat.AVInputFormat;
import org.bytedeco.javacpp.avformat.AVStream;
import org.bytedeco.javacpp.avutil;
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
	private StringBuffer liveStreamSdpDef;
	private String announcedStreamName;
	private IoSession session;
	private String sessionKey;
	private String cseq;
	private ThreadPoolTaskScheduler mTaskScheduler;


	public PacketReceiverRunnable(ThreadPoolTaskScheduler mTaskScheduler, String cseq, String sessionKey,
			IoSession session, String announcedStreamName, StringBuffer liveStreamSdpDef, String url) {
		this.mTaskScheduler = mTaskScheduler;
		this.cseq = cseq;
		this.sessionKey = sessionKey;
		this.session = session;
		this.announcedStreamName = announcedStreamName;
		this.liveStreamSdpDef = liveStreamSdpDef;
		this.url = url;
	}

	public boolean prepare_input_context(ThreadPoolTaskScheduler mTaskScheduler, final String cseq, final String sessionKey, final IoSession session, String announcedStreamName, StringBuffer liveStreamSdpDef, String streamUrl) {
		try {
			sdpFile = new File(announcedStreamName + ".sdp");
			FileOutputStream fos = new FileOutputStream(sdpFile);
			fos.write(liveStreamSdpDef.toString().getBytes());
			fos.close();

			inputFormatCtx = new AVFormatContext(null);
			

			AVDictionary options = new AVDictionary();
			int ret = av_dict_set(options, "protocol_whitelist", "file,crypto,udp,rtp", 0);
			if (ret < 0) {
				logger.debug("cannot set protocol_whitelist");
				return false;
			}
			ret = av_dict_set(options, "reorder_queue_size", "100400", 0);
			if (ret < 0) {
				logger.debug("cannot set reorder queue size");
				return false;
			}
			
			ret = av_dict_set(options, "buffer_size", "102400", 0);
			if (ret < 0) {
				logger.debug("cannot set buffer size");
				return false;
			}
			
			AVInputFormat sdpFormat = av_find_input_format("sdp");

			mTaskScheduler.schedule(new Runnable() {
				@Override
				public void run() {
					RtspResponse response = new RtspResponse();
					response.setCode(RtspCode.OK);
					response.setHeader(RtspHeaderCode.CSeq, cseq);
					response.setHeader(RtspHeaderCode.Date, DateUtil.getGmtDate());
					response.setHeader(RtspHeaderCode.Session, sessionKey);
					session.write(response);

				}
			}, new Date());


			if (avformat_open_input(inputFormatCtx, sdpFile.getAbsolutePath(), sdpFormat, options) != 0) {
				logger.debug("Could not open rtp for demuxing");
				return false;
			}

			if (avformat_find_stream_info(inputFormatCtx, (PointerPointer)null) < 0) {
				logger.debug("Could not get stream info");
				return false;
			}


			outputRTMPFormatContext = new AVFormatContext(null);

			ret = avformat_alloc_output_context2(outputRTMPFormatContext, null, "flv", null);

			lastDTS = new long[this.inputFormatCtx.nb_streams()];
			for (int i=0; i < inputFormatCtx.nb_streams(); i++) {
				AVStream in_stream = inputFormatCtx.streams(i);
				AVStream out_stream = avformat_new_stream(outputRTMPFormatContext, in_stream.codec().codec());
				AVCodecParameters avCodecParameters = new AVCodecParameters();

				ret = avcodec_parameters_from_context(avCodecParameters,  in_stream.codec());
				if (ret < 0) {
					logger.debug("Cannot get codec parameters\n");
					return false;
				}
				ret  = avcodec_parameters_to_context(out_stream.codec(), avCodecParameters);
				if (ret < 0) {
					logger.debug("Cannot set codec parameters\n");
					return false;
				}
				out_stream.codec().codec_tag(0);
				//initialize last decoding time stamp reference value
				lastDTS[i] = -1;
			}


			if ((outputRTMPFormatContext.oformat().flags() & AVFMT_GLOBALHEADER) != 0) {
				//out_stream->codec->flags |= AV_CODEC_FLAG_GLOBAL_HEADER;
				outputRTMPFormatContext.oformat().flags(outputRTMPFormatContext.oformat().flags() | AV_CODEC_FLAG_GLOBAL_HEADER);
			}


			if ((outputRTMPFormatContext.flags() & AVFMT_NOFILE) == 0) 
			{
				AVIOContext pb = new AVIOContext(null);

				URI url = new URI(streamUrl);
				String urlStr = "rtmp://" + url.getHost() + "/" + url.getPath();
				//				logger.debug("rtmp url: " + urlStr);
				//
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

			//logger.warn("timestamps pts: " +  pkt.pts() + " dts:" + pkt.dts() + " duration:" + pkt.duration());
			
			/*
			in_stream.codec().max_b_frames();
	
			
			int delay = in_stream.codecpar().video_delay();
			if (delay == 0 && in_stream.codec().max_b_frames() > 0) {
				delay = 1;
			}
			

			if (pkt.pts() == avutil.AV_NOPTS_VALUE || pkt.dts() == avutil.AV_NOPTS_VALUE) {
				logger.warn("unset timestamps pts: " +  pkt.pts() + " dts:" + pkt.dts() + " duration:" + pkt.duration() + " stream index: " + pkt.stream_index());
				if (pkt.dts() != avutil.AV_NOPTS_VALUE && delay == 0) {
					pkt.pts(pkt.dts());
				}
			}
			
//			 if ((pkt.pts() == 0 || pkt.pts() == avutil.AV_NOPTS_VALUE) && pkt.dts() == avutil.AV_NOPTS_VALUE && delay == 0) {
//			        pkt.pts(in_stream.priv_pts().)
//			        pkt->dts() =
////			        pkt->pts= st->cur_dts;
//			            pkt->pts = st->priv_pts->val;
//			    }
			
			
		    //calculate dts from pts
		    if (pkt.pts() != avutil.AV_NOPTS_VALUE && pkt.dts() == avutil.AV_NOPTS_VALUE && 
		    		delay <= avformat.AVStream.MAX_REORDER_DELAY) {
		    	in_stream.pts_buffer(0, pkt.pts());  // st->pts_buffer[0] = pkt->pts;
		    	
		        for (int i = 1; i < delay + 1 && in_stream.pts_buffer(i) == avutil.AV_NOPTS_VALUE; i++)
		            in_stream.pts_buffer(i, pkt.pts() + (i - delay - 1) * pkt.duration());  //st->pts_buffer[i] = pkt->pts + (i - delay - 1) * pkt->duration;
		        for (int i = 0; i<delay && in_stream.pts_buffer(i) > in_stream.pts_buffer(i + 1); i++) {
		        	long pts_buffer = in_stream.pts_buffer(i);
		        	in_stream.pts_buffer(i, in_stream.pts_buffer(i+1));
		        	in_stream.pts_buffer(i+1, pts_buffer);
		        	//FFSWAP(int64_t, st->pts_buffer[i], st->pts_buffer[i + 1]);
		        }
		            

		        pkt.dts(in_stream.pts_buffer(0));
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
			*/
			
			 logger.warn("pkt.pts: " + pkt.pts() + " pkt.dts:" + pkt.dts() + " stream index:" + pkt.stream_index());


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
		logger.debug("closing rtmp format context");
		av_write_trailer(outputRTMPFormatContext);


		logger.debug("closing rtsp input format context");
		// Close the video file
		if (inputFormatCtx != null) {
			avformat_close_input(inputFormatCtx);
		}


		// close output 
		if (outputRTMPFormatContext != null && ((outputRTMPFormatContext.oformat().flags() & AVFMT_NOFILE) == 0)) {
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
