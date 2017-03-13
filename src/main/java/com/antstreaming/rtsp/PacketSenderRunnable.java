package com.antstreaming.rtsp;

import static org.bytedeco.javacpp.avformat.AVFMT_GLOBALHEADER;
import static org.bytedeco.javacpp.avformat.AVFMT_NOFILE;
import static org.bytedeco.javacpp.avformat.AVIO_FLAG_WRITE;
import static org.bytedeco.javacpp.avformat.av_interleaved_write_frame;
import static org.bytedeco.javacpp.avformat.av_read_frame;
import static org.bytedeco.javacpp.avformat.av_register_all;
import static org.bytedeco.javacpp.avformat.av_sdp_create;
import static org.bytedeco.javacpp.avformat.av_write_frame;
import static org.bytedeco.javacpp.avformat.av_write_trailer;
import static org.bytedeco.javacpp.avformat.avformat_alloc_output_context2;
import static org.bytedeco.javacpp.avformat.avformat_close_input;
import static org.bytedeco.javacpp.avformat.avformat_find_stream_info;
import static org.bytedeco.javacpp.avformat.avformat_free_context;
import static org.bytedeco.javacpp.avformat.avformat_network_init;
import static org.bytedeco.javacpp.avformat.avformat_new_stream;
import static org.bytedeco.javacpp.avformat.avformat_open_input;
import static org.bytedeco.javacpp.avformat.avformat_write_header;
import static org.bytedeco.javacpp.avformat.avio_closep;
import static org.bytedeco.javacpp.avutil.AV_ROUND_NEAR_INF;
import static org.bytedeco.javacpp.avutil.AV_ROUND_PASS_MINMAX;
import static org.bytedeco.javacpp.avutil.AV_TIME_BASE;
import static org.bytedeco.javacpp.avutil.av_rescale_q;
import static org.bytedeco.javacpp.avutil.av_rescale_q_rnd;
import static org.bytedeco.javacpp.avcodec.*;

import java.lang.annotation.Annotation;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.PointerPointer;
import org.bytedeco.javacpp.avformat;
import org.bytedeco.javacpp.avutil;
import org.bytedeco.javacpp.annotation.ByPtrPtr;
import org.bytedeco.javacpp.avcodec.AVCodecParameters;
import org.bytedeco.javacpp.avcodec.AVPacket;
import org.bytedeco.javacpp.avformat.AVFormatContext;
import org.bytedeco.javacpp.avformat.AVIOContext;
import org.bytedeco.javacpp.avformat.AVOutputFormat;
import org.bytedeco.javacpp.avformat.AVStream;
import org.bytedeco.javacpp.avutil.AVDictionary;
import org.bytedeco.javacpp.avutil.AVRational;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class PacketSenderRunnable implements Runnable {


	AVPacket pkt = new AVPacket();

	int packetIndex = 0;

	long packetSentTime = 0;
	long firstPacketSentTime = 0;

	long startTime = 0;
	boolean bufferFree = true;
	boolean endOfFile = false;
	private AtomicBoolean isRunning = new AtomicBoolean(false);
	private AVFormatContext inputFormatContext;
	private AVFormatContext[] outputFormatContext;
	private static Logger logger = LoggerFactory.getLogger(PacketSenderRunnable.class);
	private boolean closeRequest = false;
	private boolean finish;
	private String rtmpUrl;

	private IMuxerListener muxerListener;


	public PacketSenderRunnable(IMuxerListener muxerListener) {
		this.muxerListener = muxerListener;
	}

	public int seek(long seekTime) {
		return avformat.av_seek_frame(inputFormatContext, -1, seekTime, avformat.AVSEEK_FLAG_FRAME);
	}


	public long getDuration() {
		return inputFormatContext.duration();
	}

	public boolean prepare_output_context(int streamId, String remoteAddress, int[] clientPort, int[] serverPort) {
		if (outputFormatContext == null) {
			outputFormatContext = new AVFormatContext[inputFormatContext.nb_streams()];
		}
		outputFormatContext[streamId] = new AVFormatContext(null);

		int ret = avformat_alloc_output_context2(outputFormatContext[streamId], null, "rtp", null);
		if (ret < 0) {
			logger.debug("Could not create output context\n");
			return false;
		}
		AVOutputFormat ofmt = outputFormatContext[streamId].oformat();
		AVStream in_stream = inputFormatContext.streams(streamId);
		AVStream out_stream = avformat_new_stream(outputFormatContext[streamId], in_stream.codec().codec());

		ret = avcodec_parameters_copy(out_stream.codecpar(), in_stream.codecpar());

		if (ret<0){
			logger.warn("Failed to copy context from input to output stream codec context\n");
			return false;
		}

		out_stream.codec().codec_tag(0);
		if ((outputFormatContext[streamId].oformat().flags() & AVFMT_GLOBALHEADER) != 0) {
			//out_stream->codec->flags |= AV_CODEC_FLAG_GLOBAL_HEADER;
			out_stream.codec().flags(out_stream.codec().flags() | AV_CODEC_FLAG_GLOBAL_HEADER);
		}

		if ((ofmt.flags() & AVFMT_NOFILE) == 0) {
			AVIOContext pb = new AVIOContext(null);

			String rtpUrl = null;
			if (serverPort[0] == 0) 
			{
				while (RtspConnection.PORT_NUMBER.get() <= RtspConnection.UDP_PORT_MAX) 
				{
					serverPort[0] = RtspConnection.PORT_NUMBER.getAndAdd(2);
					serverPort[1] = serverPort[0] + 1;
					// need to +2 because rtp port must be even and rtcp port must be odd
					//rtcp port is automatically set to rtp port + 1

					rtpUrl = "rtp://"+ remoteAddress +":" + clientPort[0] + "?localrtpport=" + serverPort[0]; // + "&localrtcpport=" + rtcpLocalPort; // + "&connect=1";

					ret = avformat.avio_open(pb, rtpUrl, AVIO_FLAG_WRITE);

					if (ret < 0) {
						logger.debug("Could not open url " + rtpUrl);
						continue;
					}
					else {
						logger.warn("Opened url " + rtpUrl);
						outputFormatContext[streamId].pb(pb);
						break;
					}
				}
				if (RtspConnection.PORT_NUMBER.get() >= RtspConnection.UDP_PORT_MAX) {
					RtspConnection.PORT_NUMBER.set(RtspConnection.UDP_PORT_MIN);
				}
			}
			else {
				rtpUrl = "rtp://"+ remoteAddress +":" + clientPort[0] + "?localrtpport=" + serverPort[0]; // + "&localrtcpport=" + rtcpLocalPort; // + "&connect=1";
				ret = avformat.avio_open(pb, rtpUrl, AVIO_FLAG_WRITE);
				if (ret < 0) {
					logger.debug("Could not open url returning " + rtpUrl);
					return false;
				}
				outputFormatContext[streamId].pb(pb);
			}
		}
		ret = avformat_write_header(outputFormatContext[streamId], (PointerPointer)null);
		if (ret < 0) {
			logger.warn("cannot write header with error: " + ret);
			return false;
		}
		return true;
	}
	/*
	public void getRtspStream() {

		AVFormatContext pFormatCtx = new AVFormatContext(null);

		String sdpFile = "rtmp://127.0.0.1//vod/bug_test2.mp4";
		//"rtp://127.0.0.1:5577"
		;
		//AVDictionary options = new AVDictionary();
		//int ret = av_dict_set(options, "protocol_whitelist", "file,crypto,udp,rtp", 0);
		//System.out.println("options dict ret:" + ret);

		//ByteBuffer wrap = ByteBuffer.wrap("file,crypto,rtp,udp".getBytes());
		//pFormatCtx.protocol_whitelist(new BytePointer(wrap));

		if (avformat_open_input(pFormatCtx, sdpFile, null, null) != 0) {
			System.out.println("Could not open rtp for demuxing");
			return;
		}


		System.out.println("waiting for stream ..." + pFormatCtx.nb_streams());
//		if (avformat_find_stream_info(pFormatCtx, (PointerPointer)null) < 0) {
//			System.out.println("Could not get stream info");
//			return;
//		}

//		byte[] sdpData = new byte[16384];
//		if (av_sdp_create(pFormatCtx, pFormatCtx.nb_streams(), sdpData, sdpData.length) != 0)  {
//			System.out.println("Could not get sdp description");
//			return;
//		}
		System.out.println("get stream info... stream count:" + pFormatCtx.nb_streams());

		//AVFormatContext outputFormatContext = new AVFormatContext(null);

		//int ret = avformat_alloc_output_context2(outputFormatContext, null, "rtp", null);

		//if (ret < 0) {
		//	System.out.println("Could not create output context\n");
		//	return;
		//}

//		for (int i=0; i < pFormatCtx.nb_streams(); i++) {
//			AVStream in_stream = pFormatCtx.streams(i);
//			AVStream out_stream = avformat_new_stream(outputFormatContext[0], in_stream.codec().codec());
//
//
//			ret = avcodec_parameters_copy(out_stream.codecpar(), in_stream.codecpar());
//			
//			logger.debug("getRtspStream time base:" + out_stream.time_base().num() +"/" + out_stream.time_base().den());
//
//			if (ret < 0) {
//				System.out.println("Cannot set codec parameters\n");
//				return;
//			}
//			out_stream.codec().codec_tag(0);
//		}
//		
//		
//		
//
//
//		if ((outputFormatContext.oformat().flags() & AVFMT_GLOBALHEADER) != 0) {
//			//out_stream->codec->flags |= AV_CODEC_FLAG_GLOBAL_HEADER;
//			outputFormatContext.oformat().flags(outputFormatContext.oformat().flags() | AV_CODEC_FLAG_GLOBAL_HEADER);
//		}
//
//		if ((outputFormatContext.flags() & AVFMT_NOFILE) == 0) {
//			AVIOContext pb = new AVIOContext(null);
//			ret = avformat.avio_open(pb, "rtp://127.0.0.1:56442", AVIO_FLAG_WRITE);
//			outputFormatContext.pb(pb);
//
//			ret = avformat_write_header(outputFormatContext, (PointerPointer)null);
//			if (ret < 0) {
//				System.out.println("Cannot write header\n");
//				return;
//			}
//		}

		AVPacket pkt = new AVPacket();

		while (true) {
			logger.warn("av read frame 1");
			int ret = av_read_frame(pFormatCtx, pkt);
			if (ret<0) {
				System.out.println("close file...");
				break;
			}
			logger.warn("av read frame 2");
			int packetIndex = pkt.stream_index();
			AVStream in_stream = pFormatCtx.streams(packetIndex);
			AVStream out_stream = outputFormatContext[0].streams(packetIndex);

			System.out.println("packet dts:" + pkt.dts() + " paket pts:" + pkt.pts());

			pkt.pts(av_rescale_q_rnd(pkt.pts(), in_stream.time_base(), out_stream.time_base(), AV_ROUND_NEAR_INF|AV_ROUND_PASS_MINMAX));
			pkt.dts(av_rescale_q_rnd(pkt.dts(), in_stream.time_base(), out_stream.time_base(), AV_ROUND_NEAR_INF|AV_ROUND_PASS_MINMAX));
			pkt.duration(av_rescale_q(pkt.duration(), in_stream.time_base(), out_stream.time_base()));
			pkt.pos(-1);

			//System.out.println("packet dts 2:" + pkt.dts());

			ret = av_interleaved_write_frame(outputFormatContext[0], pkt);
			if (ret < 0) {
				System.out.println("Error muxing packet\n");
			}
			av_packet_unref(pkt);
		}

		av_write_trailer(outputFormatContext[0]);
		// close output 
		if (outputFormatContext != null && ((outputFormatContext[0].oformat().flags() & AVFMT_NOFILE) == 0)) {
			avio_closep(outputFormatContext[0].pb());
		}
		avformat_free_context(outputFormatContext[0]);

		// Close the video file
		avformat_close_input(pFormatCtx);
	}
	 */

	@Override
	public void run() {
		try {
			if (!isRunning.compareAndSet(false, true) || inputFormatContext == null) {
				return;
			}

			synchronized (this) {

				AVStream in_stream, out_stream;
				if (bufferFree) 
				{
					int ret = av_read_frame(inputFormatContext, pkt);
					if (ret < 0) {
						byte[] data = new byte[4096];
						avutil.av_strerror(ret, data, data.length);
						logger.warn("cannot read frame, closing muxer. Error: " + new String(data));

						closeMuxer(true);
						return;
					}
					packetIndex = pkt.stream_index();

					in_stream  = inputFormatContext.streams(packetIndex);

					if (outputFormatContext[packetIndex] == null) {
						//pass this packet, stream is likely not supported in rtp
						return;
					}
					out_stream = outputFormatContext[packetIndex].streams(0);

					/* copy packet */
					AVRational avRational = new AVRational();
					avRational.num(1);
					avRational.den(AV_TIME_BASE);
					packetSentTime = av_rescale_q(pkt.dts(), in_stream.time_base(), avRational); // + pkt.duration(); 
					if (firstPacketSentTime == 0) {
						firstPacketSentTime = packetSentTime;
					}

					//pkt.dts() + pkt.duration();
					pkt.pts(av_rescale_q_rnd(pkt.pts(), in_stream.time_base(), out_stream.time_base(), AV_ROUND_NEAR_INF|AV_ROUND_PASS_MINMAX));
					pkt.dts(av_rescale_q_rnd(pkt.dts(), in_stream.time_base(), out_stream.time_base(), AV_ROUND_NEAR_INF|AV_ROUND_PASS_MINMAX));
					pkt.duration(av_rescale_q(pkt.duration(), in_stream.time_base(), out_stream.time_base()));
					pkt.pos(-1);

					//stream index is always zero because rtp can contain one stream
					pkt.stream_index(0);

				}


				if (startTime == 0) {
					startTime = System.currentTimeMillis(); 
					//logger.debug("start time :" + startTime + " offset:" + offset);
				}

				long timeDiff = (System.currentTimeMillis() - startTime) * 1000; // convert milli seconds to micro seconds

				if (packetSentTime <= (firstPacketSentTime + timeDiff)) 
				{
					int ret = av_write_frame(outputFormatContext[packetIndex], pkt);
					if (ret < 0) {
						logger.warn("Error muxing packet with error: " + ret);
					}
					av_packet_unref(pkt);
					bufferFree = true;
				}
				else {
					//logger.debug("waiting to send");
					bufferFree = false;
				}

				if (closeRequest) {
					closeInternal(finish);
					closeRequest = false;
				}
			}
		}
		catch (Exception e) {
			logger.warn("exception");
			e.printStackTrace();
		}finally {
			isRunning.compareAndSet(true, false);
		}
	}

	public void closeInternal(boolean finishProcess) 
	{
		logger.warn("closeInternal called.");
		if (outputFormatContext != null) {
			for (AVFormatContext avFormatContext : outputFormatContext) {
				if (avFormatContext == null) {
					continue;
				}
				//if (finishProcess) 
				{
					logger.warn("close internal av_write_trailer(avFormatContext);");
					av_write_trailer(avFormatContext);
				}
				/* close output */
				if (avFormatContext != null && ((avFormatContext.oformat().flags() & AVFMT_NOFILE) == 0)) {
					logger.warn(" avio_closep(avFormatContext.pb()); ");
					avio_closep(avFormatContext.pb());
				}
				avformat_free_context(avFormatContext);
				avFormatContext = null;
			}
			outputFormatContext = null;
		}
		if (inputFormatContext != null && finishProcess) {
			avformat_close_input(inputFormatContext);
			inputFormatContext = null;
		}
		if (this.muxerListener != null) {
			this.muxerListener.muxingFinished(this);
		}
	}

	public void closeMuxer(boolean finishProcess) {
		synchronized (this) {
			logger.warn("close muxer called with finish process: " + finishProcess);
			closeRequest  = true;
			finish = finishProcess;
			closeInternal(finishProcess);
			closeRequest = false;
			//this is end of file or a problem
			//close the muxers
		}

	}


	public int getStreamCount() {
		return inputFormatContext.nb_streams();
	}

	public String getSdpDescription(String rtmpUrl) {
		return getSdpDescription(rtmpUrl, true);
	}

	public String getSdpDescription(String rtmpUrl, boolean createSDP) {
		this.rtmpUrl = rtmpUrl;
		inputFormatContext = avformat.avformat_alloc_context();
		int ret = avformat_open_input(inputFormatContext, rtmpUrl, null, null);
		if (ret < 0) {
			byte[] data = new byte[4096];
			avutil.av_strerror(ret, data, data.length);
			logger.warn("could not open input " + rtmpUrl + " error code:" + ret
					+ " description: " + new String(data));
			return null;
		}


		ret = avformat_find_stream_info(inputFormatContext, (PointerPointer)null); 
		if (ret < 0) {
			byte[] data = new byte[4096];
			avutil.av_strerror(ret, data, data.length);
			logger.warn("could not find stream info " + rtmpUrl + " error code:" + ret
					+ " description: " + new String(data));
			return null;
		}

		if (createSDP) {
			byte[] sdpData = new byte[16384];

			ret = av_sdp_create(inputFormatContext, 1, sdpData, 2048) ;
			if (ret < 0) {
				byte[] data = new byte[4096];
				avutil.av_strerror(ret, data, data.length);
				logger.warn("could not create sdp " + rtmpUrl + " error code:" + ret
						+ " description: " + new String(data));
				return null;
			}

			String sdpString = new String(sdpData);

			if (sdpString.indexOf("rtpmap") == -1) {
				logger.warn("sdp description does not have rtpmap field");
				return null;
			}

			return sdpString.trim();
		}
		return null;
	}

	public void reinitialize() {
		if (inputFormatContext != null) {
			avformat_close_input(inputFormatContext);
			inputFormatContext = null;
		}
		getSdpDescription(rtmpUrl, false);

	}

}