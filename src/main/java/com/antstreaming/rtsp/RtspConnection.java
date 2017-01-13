package com.antstreaming.rtsp;

import static org.bytedeco.javacpp.avcodec.AV_CODEC_FLAG_GLOBAL_HEADER;
import static org.bytedeco.javacpp.avcodec.av_packet_unref;
import static org.bytedeco.javacpp.avcodec.avcodec_parameters_from_context;
import static org.bytedeco.javacpp.avcodec.avcodec_parameters_to_context;
import static org.bytedeco.javacpp.avformat.*;
import static org.bytedeco.javacpp.avutil.AV_ROUND_NEAR_INF;
import static org.bytedeco.javacpp.avutil.AV_ROUND_PASS_MINMAX;
import static org.bytedeco.javacpp.avutil.AV_TIME_BASE;
import static org.bytedeco.javacpp.avutil.av_dict_set;
import static org.bytedeco.javacpp.avutil.av_rescale_q;
import static org.bytedeco.javacpp.avutil.av_rescale_q_rnd;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.bytedeco.javacpp.PointerPointer;
import org.bytedeco.javacpp.avformat;
import org.bytedeco.javacpp.avutil;
import org.bytedeco.javacpp.avcodec.AVCodecParameters;
import org.bytedeco.javacpp.avcodec.AVPacket;
import org.bytedeco.javacpp.avformat.AVFormatContext;
import org.bytedeco.javacpp.avformat.AVIOContext;
import org.bytedeco.javacpp.avformat.AVOutputFormat;
import org.bytedeco.javacpp.avformat.AVStream;
import org.bytedeco.javacpp.avutil.AVDictionary;
import org.bytedeco.javacpp.avutil.AVRational;
import org.red5.server.BaseConnection;
import org.red5.server.api.IConnection;
import org.red5.server.api.IContext;
import org.red5.server.api.IServer;
import org.red5.server.api.scope.IBroadcastScope;
import org.red5.server.api.scope.IGlobalScope;
import org.red5.server.api.scope.IScope;
import org.red5.server.api.service.IPendingServiceCallback;
import org.red5.server.api.service.IServiceCall;
import org.red5.server.api.service.IServiceCapableConnection;
import org.red5.server.api.stream.IClientBroadcastStream;
import org.red5.server.api.stream.IClientStream;
import org.red5.server.api.stream.IPlaylistSubscriberStream;
import org.red5.server.api.stream.ISingleItemSubscriberStream;
import org.red5.server.api.stream.IStreamCapableConnection;
import org.red5.server.api.stream.IStreamFilenameGenerator;
import org.red5.server.api.stream.IStreamService;
import org.red5.server.net.rtmp.IReceivedMessageTaskQueueListener;
import org.red5.server.net.rtmp.RTMPConnection;
import org.red5.server.net.rtmp.RTMPMinaConnection;
import org.red5.server.net.rtmp.ReceivedMessageTaskQueue;
import org.red5.server.net.rtmp.message.Packet;
import org.red5.server.net.rtmp.status.Status;
import org.red5.server.api.stream.IStreamFilenameGenerator.GenerationType;
import org.red5.server.stream.AbstractClientStream;
import org.red5.server.stream.ClientBroadcastStream;
import org.red5.server.stream.DefaultStreamFilenameGenerator;
import org.red5.server.stream.IProviderService;
import org.red5.server.stream.IProviderService.INPUT_TYPE;
import org.red5.server.stream.ProviderService;
import org.red5.server.stream.StreamService;
import org.red5.server.util.ScopeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import com.antstreaming.rtsp.protocol.RTSPTransport;
import com.antstreaming.rtsp.protocol.RtspCode;
import com.antstreaming.rtsp.protocol.RtspHeaderCode;
import com.antstreaming.rtsp.protocol.RtspRequest;
import com.antstreaming.rtsp.protocol.RtspResponse;
import com.antstreaming.rtsp.protocol.RTSPTransport.LowerTransport;
import com.antstreaming.rtsp.protocol.RtspRequest.Verb;
import com.antstreaming.rtsp.session.DateUtil;

public class RtspConnection  extends RTMPMinaConnection {

	private static final int UDP_PORT_MIN = 5000;

	private static final int UDP_PORT_MAX = 65000;

	private Logger logger = LoggerFactory.getLogger(RtspConnection.class);

	protected static final String SLASH = "/";
	private static final String CSEQ = "cseq";
	private static final String REQUIRE_VALUE_NGOD_R2 = "com.comcast.ngod.r2";
	private static final String REQUIRE_VALUE_NGOD_C1 = "com.comcast.ngod.c1";

	private ThreadPoolTaskScheduler mTaskScheduler;
	private IoSession mSession;

	private IServer mServer;

	protected static AtomicInteger portNum = new AtomicInteger(5000);

	private AVFormatContext inputFormatContext;
	private AVFormatContext[] outputFormatContext;

	private ScheduledFuture<?> mPacketSendScheduledFuture;

	private String mSessionKey;

	private int[][] serverPort;

	private int[][] clientPort;

	private String remoteAddress;

	private StringBuffer liveStreamSdpDef;

	private ApplicationContext mApplicationContext;

	private File streamFile;

	private File sdpFile;

	private ScheduledFuture<?> frameReceiverScheduledFuture;

	private FrameReceiverRunnable frameReceiver;

	private IScope scope;

	//private ClientBroadcastStream bs;


	public void handleMessage(IoSession session, Object message) {
		//get RTSP connection in here and let rtsp connection receive this message
		//rtsp connection should receive this message from a task queue
		//do not forget to add this job to a task queue as rtmp conn does
		logger.debug("RTSP Server Receive Message: \n{}", message);

		RtspRequest request = (RtspRequest) message;
		String cseq = request.getHeader(RtspHeaderCode.CSeq);
		session.setAttribute(CSEQ, cseq);
		switch (request.getVerb()) {
		case OPTIONS:
			onRequestOptions(session, request);
			break;
		case DESCRIBE:
			onRequestDescribe(session, request);
			break;
		case SETUP:
			onRequestSetup(session, request);
			break;
		case TEARDOWN:
			onRequestTeardown(session, request);
			break;
		case PLAY:
			onRequestPlay(session, request);
			break;
		case PAUSE:
			onRequestPause(session, request);
			break;
		case GET_PARAMETER:
			onRequestGP(session, request);
			break;
		case ANNOUNCE:
			onRequestAnnounce(session, request);
			break;
		case RECORD:
			onRequestRecord(session, request);
			break;
		default:
			onDefaultRequest(session, request, request.getHeader(RtspHeaderCode.CSeq));
		}
	}


	private void onRequestRecord(IoSession session, RtspRequest request) {
		String cseq = request.getHeader(RtspHeaderCode.CSeq);
		if (cseq == null || "".equals(cseq)) {
			cseq = "0";
		}
		//bs.start();
		//bs.startPublishing();

		prepare_input_context(session, cseq, request);
		logger.debug("sdp content is:" + liveStreamSdpDef);
		
	}



	private boolean prepare_input_context(final IoSession session, final String cseq, RtspRequest request) {
		try {
			sdpFile = new File(streamFile.getName() + ".sdp");
			FileOutputStream fos = new FileOutputStream(sdpFile);
			fos.write(liveStreamSdpDef.toString().getBytes());
			fos.close();


			AVFormatContext inputFormatCtx = new AVFormatContext(null);

			AVDictionary options = new AVDictionary();
			int ret = av_dict_set(options, "protocol_whitelist", "file,crypto,udp,rtp", 0);

			AVInputFormat sdpFormat = av_find_input_format("sdp");

			mTaskScheduler.schedule(new Runnable() {

				@Override
				public void run() {
					RtspResponse rtspResponse = new RtspResponse();
					rtspResponse.setHeader(RtspHeaderCode.CSeq, cseq);
					session.write(rtspResponse);
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

			logger.debug("1");
			AVFormatContext outputFormatContext = new AVFormatContext(null);

			ret = avformat_alloc_output_context2(outputFormatContext, null, "mp4", streamFile.getAbsolutePath());
			
			AVFormatContext outputRTMPFormatContext = new AVFormatContext(null);
			
			ret = avformat_alloc_output_context2(outputRTMPFormatContext, null, "flv", null);

			logger.debug("2");
			for (int i=0; i < inputFormatCtx.nb_streams(); i++) {
				AVStream in_stream = inputFormatCtx.streams(i);
				AVStream out_stream = avformat_new_stream(outputFormatContext, in_stream.codec().codec());
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
				
				
				out_stream = avformat_new_stream(outputRTMPFormatContext, in_stream.codec().codec());
				ret  = avcodec_parameters_to_context(out_stream.codec(), avCodecParameters);
				out_stream.codec().codec_tag(0);
			}
			
			logger.debug("3");


			if ((outputFormatContext.oformat().flags() & AVFMT_GLOBALHEADER) != 0) {
				//out_stream->codec->flags |= AV_CODEC_FLAG_GLOBAL_HEADER;
				outputFormatContext.oformat().flags(outputFormatContext.oformat().flags() | AV_CODEC_FLAG_GLOBAL_HEADER);
			}
			
			
			if ((outputRTMPFormatContext.oformat().flags() & AVFMT_GLOBALHEADER) != 0) {
				//out_stream->codec->flags |= AV_CODEC_FLAG_GLOBAL_HEADER;
				outputRTMPFormatContext.oformat().flags(outputRTMPFormatContext.oformat().flags() | AV_CODEC_FLAG_GLOBAL_HEADER);
			}
			
			logger.debug("4");

			if ((outputFormatContext.flags() & AVFMT_NOFILE) == 0) 
			{
				AVIOContext pb = new AVIOContext(null);
				ret = avformat.avio_open(pb,  streamFile.getAbsolutePath(), AVIO_FLAG_WRITE);
				outputFormatContext.pb(pb);
				AVDictionary optionsFastStart = new AVDictionary();
				av_dict_set(optionsFastStart, "movflags", "faststart+rtphint", 0);
				ret = avformat_write_header(outputFormatContext, optionsFastStart);
				if (ret < 0) {
					logger.debug("Cannot write header to mp4\n");
					return false;
				}
			}
			
			logger.debug("5");
			
			if ((outputRTMPFormatContext.flags() & AVFMT_NOFILE) == 0) 
			{
				AVIOContext pb = new AVIOContext(null);
				
				URI url = new URI(request.getUrl());
				String urlStr = "rtmp://" + url.getHost() + "/" + url.getPath();
				logger.debug("rtmp url: " + urlStr);
				
				ret = avformat.avio_open(pb,  urlStr, AVIO_FLAG_WRITE);
				outputRTMPFormatContext.pb(pb);
				
				logger.debug("51");
				ret = avformat_write_header(outputRTMPFormatContext, (AVDictionary)null);
				if (ret < 0) {
					logger.debug("Cannot write header to rtmp\n");
					return false;
				}
				logger.debug("52");
			}
			
			logger.debug("6");


			//start frame receiver
			frameReceiver = new FrameReceiverRunnable(inputFormatCtx, outputFormatContext, outputRTMPFormatContext);
			frameReceiverScheduledFuture = mTaskScheduler.scheduleAtFixedRate(frameReceiver, 10);
			
			logger.debug("7");
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return true;
	}




	private void onRequestAnnounce(IoSession session, RtspRequest request) {
		// TODO: execute scope publish security function

		String contentType = request.getHeader(RtspHeaderCode.ContentType);
		String cseq = request.getHeader(RtspHeaderCode.CSeq);
		if (cseq == null || "".equals(cseq)) {
			cseq = "0";
		}
		if (contentType.indexOf("application/sdp") == -1) {
			handleError(session, cseq, RtspCode.UnsupportedMediaType);
			return;
		}

		//logger.debug(request.toString());
		URI url;
		try {
			url = new URI(request.getUrl());

			String streamName = getStreamName(url.getPath());
			String app = getAppName(url.getPath());

			IScope scope = getScope(url, app);
			if (scope == null) {
				handleError(session, cseq, RtspCode.DestinationUnreachable);
				return;
			}
/*
			INPUT_TYPE type = getProviderService(scope).lookupProviderInput(scope, streamName, -2);
			//TODO: test case try to resend the same  file and check that server returns error
			logger.debug("input type: " + type , " live input:" + INPUT_TYPE.LIVE + " vod type:" + INPUT_TYPE.VOD);
			if (type == INPUT_TYPE.LIVE || type == INPUT_TYPE.VOD) {
				handleError(session, cseq, RtspCode.NotAcceptable);
				return;
			}
			else if (streamName.indexOf(".") != -1) {
				//TODO we may need to check webm file as well
				type = getProviderService(scope).lookupProviderInput(scope, streamName+".mp4", -2);
				logger.debug("input type2: " + type , " live input:" + INPUT_TYPE.LIVE + " vod type:" + INPUT_TYPE.VOD);
				if (type == INPUT_TYPE.LIVE || type == INPUT_TYPE.VOD) {
					handleError(session, cseq, RtspCode.NotAcceptable);
					return;
				}
			}
*/
			IStreamFilenameGenerator generator = (IStreamFilenameGenerator) ScopeUtils.getScopeService(scope, IStreamFilenameGenerator.class, DefaultStreamFilenameGenerator.class);
			String fileName = generator.generateFilename(scope, streamName, ".mp4", GenerationType.RECORD);


			streamFile = scope.getContext().getResource(fileName).getFile();

			liveStreamSdpDef = request.getBuffer();

			int streamCount = 0;
			if (liveStreamSdpDef.indexOf("m=video") != -1) {
				streamCount++;
			}

			if (liveStreamSdpDef.indexOf("m=audio") != -1) {
				streamCount++;
			}
			
			/*
			{
				IBroadcastScope bsScope = scope.getBroadcastScope(streamName); 
				connect(scope);
				reserveStreamId(session.getId());
				bs = (ClientBroadcastStream) newBroadcastStream(session.getId());
				bs.setPublishedName(streamName);
				
				// set stream parameters if they exist
				IContext context = scope.getContext();
				
				ProviderService providerService = (ProviderService) mApplicationContext.getBean(IProviderService.BEAN_NAME);
				
				// TODO handle registration failure
				if (providerService.registerBroadcastStream(scope, streamName, bs)) {
					bsScope = scope.getBroadcastScope(streamName); 
					bsScope.setClientBroadcastStream(bs);
					// if (conn instanceof BaseConnection) 
					{
						registerBasicScope(bsScope);
					}
				}
			}
			*/
			
			


			serverPort = new int[streamCount][2];
			clientPort = new int[streamCount][2];
			RtspResponse rtspResponse = new RtspResponse();
			rtspResponse.setHeader(RtspHeaderCode.CSeq, cseq);
			session.write(rtspResponse);

		} catch (URISyntaxException e) {

			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}


	private IScope getScope(URI url, String app) {
		if (scope == null) {
			IGlobalScope global = mServer.lookupGlobal(url.getHost(), app);
			if (global != null) {
				IContext context = global.getContext();
				if (context != null) {
					scope = context.resolveScope(global, app);
				}
			}
		}
		return scope;
	}


	private void onRequestPlay(IoSession session, RtspRequest request) {
		// get cesq
		String cseq = request.getHeader(RtspHeaderCode.CSeq);
		if (null == cseq || "".equals(cseq)) {
			logger.error("cesq is null...................");
			handleError(session, "0", RtspCode.HeaderFieldNotValidForResource);
			return;
		}
		// getsessionKey
		String sessionKey = request.getHeader(RtspHeaderCode.Session);
		if (null == sessionKey || "".equals(sessionKey) || !mSessionKey.equals(sessionKey)) {
			logger.error("sessionKey is null...................");
			handleError(session, cseq, RtspCode.SessionNotFound);
			return;
		}

		RtspResponse response = new RtspResponse();
		response.setCode(RtspCode.OK);
		response.setHeader(RtspHeaderCode.CSeq, cseq);
		response.setHeader(RtspHeaderCode.Date, DateUtil.getGmtDate());
		response.setHeader(RtspHeaderCode.Session, sessionKey);


		String rangeValue = request.getHeader(RtspHeaderCode.Range);
		float durationInSeconds =  inputFormatContext.duration()/1000000;


		long seekTime = 0; //should in micro seconds
		if (null != rangeValue) {
			String[] rangeValues = rangeValue.split("=");
			String startTime = "0.000";
			if (rangeValues.length >=2) {
				startTime = rangeValues[1].substring(0, rangeValues[1].indexOf("-"));
				seekTime = (long)(Float.parseFloat(startTime) * 1000000);
				logger.debug("seek time :" + seekTime);
				if (seekTime <= inputFormatContext.duration()) {
					int ret = avformat.av_seek_frame(inputFormatContext, -1, seekTime, avformat.AVSEEK_FLAG_FRAME);
					if (ret < 0) {
						logger.debug("cannot seek the file to specified timestamp" + seekTime);
						response.setCode(RtspCode.InternalServerError);
					}
				}
				else {
					response.setCode(RtspCode.InvalidRange);
				}
			}
			response.setHeader(RtspHeaderCode.Range, "npt=" + startTime + "-" + durationInSeconds);
		} else {
			response.setHeader(RtspHeaderCode.Range, "npt=0.000-" + durationInSeconds);
		}


		String scale = request.getHeader(RtspHeaderCode.Scale);
		if (null != scale) {
			response.setHeader(RtspHeaderCode.Scale, scale);
		} else {
			response.setHeader(RtspHeaderCode.Scale, "1.00");
		}

		session.write(response);

		if (response.getCode() == RtspCode.OK) {
			logger.debug("starting to play");
			mPacketSendScheduledFuture = mTaskScheduler.scheduleAtFixedRate(new FrameSenderRunnable(), 10);
			//convert seektime from micro seconds to nano seconds
		}
		else {
			logger.debug("not starting to play");
		}

	}


	public class FrameReceiverRunnable implements Runnable {

		private AVFormatContext inputFormatCtx;
		//private AVFormatContext outputFormatContext;
		private AtomicBoolean isRunning = new AtomicBoolean(false);
		private long[] lastDTS;
		private AVFormatContext outputRTMPFormatContext;

		public FrameReceiverRunnable(AVFormatContext inputFormatCtx, AVFormatContext outputFormatContext, AVFormatContext outputRTMPFormatContext) {
			this.inputFormatCtx = inputFormatCtx;
		//	this.outputFormatContext = outputFormatContext;
			this.outputRTMPFormatContext = outputRTMPFormatContext;
			lastDTS = new long[this.inputFormatCtx.nb_streams()];
		}

		@Override
		public void run() {

			if (!isRunning.compareAndSet(false, true)) {
				return;
			}

			AVPacket pkt = new AVPacket();

			int ret = av_read_frame(inputFormatCtx, pkt);
			if (ret<0) {
				logger.debug("close file...");
				closeMuxer();
				return;
			}
			int packetIndex = pkt.stream_index();
			AVStream in_stream = inputFormatCtx.streams(packetIndex);
			//AVStream out_stream = outputFormatContext.streams(packetIndex);
			AVStream out_stream2 = outputRTMPFormatContext.streams(packetIndex);

			if (lastDTS[packetIndex] > pkt.dts()) {
				logger.warn("dts timestamps are not in correct order last dts:"  + lastDTS[packetIndex] 
						+ " current dts:" + pkt.dts() + " fixing problem by adding offset");
				long newDTS = lastDTS[packetIndex] + 1;
				if (newDTS > pkt.pts()) {
					pkt.pts(newDTS);
				}
				pkt.dts(newDTS);
			}
			lastDTS[packetIndex] = pkt.dts();

			/*
			logger.trace("before packet index:"+ packetIndex +" packet dts:" + pkt.dts() + " paket pts:" + pkt.pts());

			pkt.pts(av_rescale_q_rnd(pkt.pts(), in_stream.time_base(), out_stream.time_base(), AV_ROUND_NEAR_INF|AV_ROUND_PASS_MINMAX));
			pkt.dts(av_rescale_q_rnd(pkt.dts(), in_stream.time_base(), out_stream.time_base(), AV_ROUND_NEAR_INF|AV_ROUND_PASS_MINMAX));
			pkt.duration(av_rescale_q(pkt.duration(), in_stream.time_base(), out_stream.time_base()));
			pkt.pos(-1);
			//bs.dispatchEvent(event);

			//System.out.println("packet dts 2:" + pkt.dts());

			ret = av_interleaved_write_frame(outputFormatContext, pkt);
			if (ret < 0) {
				logger.debug("Error muxing packet\n");
			}
			*/
			
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

			isRunning.compareAndSet(true, false);


		}

		public synchronized void closeMuxer() {

			/*
			if (outputFormatContext != null) {
				av_write_trailer(outputFormatContext);
				// close output 
				if (outputFormatContext != null && ((outputFormatContext.oformat().flags() & AVFMT_NOFILE) == 0)) {
					avio_closep(outputFormatContext.pb());
				}
				avformat_free_context(outputFormatContext);
				outputFormatContext = null;
			}
			*/
			
			if (outputRTMPFormatContext != null) {
				av_write_trailer(outputRTMPFormatContext);
				/* close output */
				if (outputRTMPFormatContext != null && ((outputRTMPFormatContext.oformat().flags() & AVFMT_NOFILE) == 0)) {
					avio_closep(outputRTMPFormatContext.pb());
				}
				avformat_free_context(outputRTMPFormatContext);
				outputRTMPFormatContext = null;
			}

			// Close the video file
			if (inputFormatContext == null) {
				avformat_close_input(inputFormatCtx);
				inputFormatContext = null;
			}

			if (sdpFile != null && sdpFile.exists()) {
				sdpFile.delete();
				sdpFile = null;
			}
		}

	}


	public class FrameSenderRunnable implements Runnable {

		AVPacket pkt = new AVPacket();
		int packetIndex = 0;

		long packetSentTime = 0;
		long firstPacketSentTime = 0;

		long startTime = 0;
		boolean bufferFree = true;
		boolean endOfFile = false;
		private AtomicBoolean isRunning = new AtomicBoolean(false);

		@Override
		public void run() {
			try {
				if (!isRunning.compareAndSet(false, true)) {
					return;
				}

				AVStream in_stream, out_stream;

				if (bufferFree) 
				{
					int ret = av_read_frame(inputFormatContext, pkt);
					if (ret < 0) {
						closeMuxer(true);
						return;
					}
					packetIndex = pkt.stream_index();
					in_stream  = inputFormatContext.streams(packetIndex);
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

					logger.debug("packet index:"+ pkt.stream_index() + " packet.pts:" + pkt.dts() + " packet.pts:" + pkt.pts()  
					+" in_stream time base:"+ in_stream.time_base().num() +"/" + in_stream.time_base().den() 
					+" out_stream time base:"+ out_stream.time_base().num() +"/" + out_stream.time_base().den() 

					+ " packet duration: " + pkt.duration());

					//stream index is always zero because rtp can contain one stream
					pkt.stream_index(0);

				}


				if (startTime == 0) {
					startTime = System.currentTimeMillis(); 
					//logger.debug("start time :" + startTime + " offset:" + offset);
				}

				long timeDiff = (System.currentTimeMillis() - startTime) * 1000; // convert milli seconds to micro seconds

				//long time = (System.nanoTime() - startTime + offset);

				//logger.debug("packet sent time:" + packetSentTime + " time:" + time);

				if (packetSentTime <= (firstPacketSentTime + timeDiff)) 
				{

					int ret = av_write_frame(outputFormatContext[packetIndex], pkt);
					if (ret < 0) {
						System.out.println("Error muxing packet\n");
					}
					av_packet_unref(pkt);
					bufferFree = true;
				}
				else {
					//logger.debug("waiting to send");
					bufferFree = false;
				}	
			}
			catch (Exception e) {
				e.printStackTrace();
			}finally {
				isRunning.compareAndSet(true, false);
			}
		}

	}


	private void closeMuxer(boolean finishProcess) {

		synchronized (this) {
			//this is end of file or a problem
			//close the muxers
			if (mPacketSendScheduledFuture != null && !mPacketSendScheduledFuture.isDone()) {
				mPacketSendScheduledFuture.cancel(false);
				mPacketSendScheduledFuture = null;
			}

			if (outputFormatContext != null) {
				for (AVFormatContext avFormatContext : outputFormatContext) {
					if (avFormatContext == null) {
						continue;
					}
					//if (finishProcess) 
					{
						av_write_trailer(avFormatContext);
					}
					/* close output */
					if (avFormatContext != null && ((avFormatContext.oformat().flags() & AVFMT_NOFILE) == 0)) {

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
		}

	}


	private void onRequestOptions(IoSession session, RtspRequest request) {
		RtspResponse response = new RtspResponse();
		response.setHeader(RtspHeaderCode.CSeq, request.getHeader(RtspHeaderCode.CSeq));
		response.setHeader(RtspHeaderCode.Public, "DESCRIBE, SETUP, TEARDOWN, PLAY, PAUSE, ANNOUNCE");
		response.setHeader(RtspHeaderCode.Server, "RtspServer");
		response.setHeader(RtspHeaderCode.ContentLength, "0");
		session.write(response);
	}


	private void onRequestDescribe(IoSession session, RtspRequest request) {
		//TODO: call context playback security here
		RtspResponse response = new RtspResponse();
		String cseq = request.getHeader(RtspHeaderCode.CSeq);
		if (null != cseq && !"".equals(cseq)) {
			response.setHeader(RtspHeaderCode.CSeq, cseq);
		}

		URI url;
		try {
			url = new URI(request.getUrl());

			logger.debug("on request describe host: " + url.getHost() + " path:" + url.getPath());

			String streamName = getStreamName(url.getPath());
			String app = getAppName(url.getPath());

			logger.debug("app:"  + app + " streamName:" + streamName);

			IScope scope = getScope(url, app);

			IStreamService streamService = (IStreamService) ScopeUtils.getScopeService(scope, IStreamService.class, StreamService.class);

			IProviderService providerService = getProviderService(scope);


			streamFile = providerService.getVODProviderFile(scope, streamName);


			if (streamFile != null) {
				logger.debug("requested file is " + streamFile.getName());
				response.setHeader(RtspHeaderCode.Date, DateUtil.getGmtDate());
				response.setHeader(RtspHeaderCode.ContentType, "application/sdp");
				response.setHeader(RtspHeaderCode.Server, "RtspServer");

				//TODO: do this job with a task in an executor
				inputFormatContext = new AVFormatContext(null);
				int ret = avformat_open_input(inputFormatContext, streamFile.getAbsolutePath(), null, null);
				if (ret == 0) {

					byte[] sdpData = new byte[16384];
					//pFormatCtx.nb_streams()
					if (av_sdp_create(inputFormatContext, inputFormatContext.nb_streams(), sdpData, sdpData.length) == 0) {
						if (avformat_find_stream_info(inputFormatContext, (PointerPointer)null) >= 0) 
						{
							serverPort = new int[inputFormatContext.nb_streams()][2];
							clientPort = new int[inputFormatContext.nb_streams()][2];
							StringBuffer sdp = new StringBuffer();
							sdp.append(new String(sdpData));
							response.setHeader(RtspHeaderCode.ContentLength, String.valueOf(sdp.length()));
							response.setBuffer(sdp);
						}
						else {
							response.setCode(RtspCode.InternalServerError);
							logger.debug("cannot find stream info in the context");
						}
					}
					else {
						response.setCode(RtspCode.InternalServerError);
						logger.debug("could not get sdp info of " + streamFile.getAbsolutePath());
					}


				}
				else {
					byte[] data = new byte[4096];
					avutil.av_strerror(ret, data, data.length);
					logger.warn("could not opened file " + streamFile.getAbsolutePath() + " error code:" + ret
							+ " description: " + new String(data));
					response.setCode(RtspCode.InternalServerError);
				}
			}
			else {
				response.setCode(RtspCode.NotFound);
			}
			session.write(response);


		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
	}


	private IProviderService getProviderService(IScope scope) {
		IContext ctx = scope.getContext();
		IProviderService providerService = null;
		if (ctx.hasBean(IProviderService.BEAN_NAME)) {
			providerService  = (IProviderService) ctx.getBean(IProviderService.BEAN_NAME);
		} else {
			//try the parent
			providerService = (IProviderService) scope.getParent().getContext().getBean(IProviderService.BEAN_NAME);
		}
		return providerService;
	}


	private String getAppName(String path) {
		String app = "/";
		int slashIndex = path.indexOf(SLASH);
		if ( slashIndex != -1) {
			int secondIndex = path.indexOf(SLASH, slashIndex+1);
			if (secondIndex != -1) {
				app =  path.substring(slashIndex+1, secondIndex);
			}

		}
		return app;
	}


	private String getStreamName(String path) {
		String streamName = null;
		int lastIndexOf = path.lastIndexOf("/");
		if (lastIndexOf != -1) {
			streamName = path.substring(path.lastIndexOf("/") + 1);
		}
		return streamName;
	}


	private void onRequestSetup(IoSession session, RtspRequest request) 
	{

		RtspResponse response = new RtspResponse();
		// get cesq
		String cseq = request.getHeader(RtspHeaderCode.CSeq);
		if (null == cseq || "".equals(cseq)) {
			logger.error("cesq is null.....");
			handleError(session, "0", RtspCode.HeaderFieldNotValidForResource);
			return;
		}
		else {
			response.setHeader(RtspHeaderCode.CSeq, cseq);
		}

		String streamIdText = "streamid=";
		int streamIdIndex = request.getUrl().indexOf(streamIdText);
		int startIndex = streamIdIndex + streamIdText.length();
		int streamId = Integer.valueOf(request.getUrl().substring(startIndex, startIndex+1)); 

		// get Transport
		String transport = request.getHeader(RtspHeaderCode.Transport);
		RTSPTransport rtspTransport = new RTSPTransport(transport);

		if (rtspTransport.getLowerTransport() != LowerTransport.UDP) {
			handleError(session, cseq, RtspCode.UnsupportedTransport);
			return;
		}
		clientPort[streamId] = rtspTransport.getClientPort();
		String mode = rtspTransport.getMode();

		if (mode != null && mode.equals("record")) {
			//TODO check the url and do this operation according to the control parameter in the sdp

			int portNo = portNum.getAndAdd(2);
			serverPort[streamId][0] = portNo;
			serverPort[streamId][1] = portNo + 1;

			if (streamId == 0) {
				String videoDef = "m=video 0";
				liveStreamSdpDef.replace(liveStreamSdpDef.indexOf(videoDef) + videoDef.length() -1 , liveStreamSdpDef.indexOf(videoDef) + videoDef.length(), String.valueOf(portNo));
			}
			else if (streamId == 1) {
				String audioDef = "m=audio 0";
				liveStreamSdpDef.replace(liveStreamSdpDef.indexOf(audioDef) + audioDef.length() -1 , liveStreamSdpDef.indexOf(audioDef) + audioDef.length(), String.valueOf(portNo));
			}

			rtspTransport.setServerPort(serverPort[streamId]);
			response.setHeader(RtspHeaderCode.Session, mSessionKey);
			rtspTransport.setLowerTransport(LowerTransport.UDP);

			response.setHeader(RtspHeaderCode.Transport, rtspTransport.toString());
		}
		else {
			//then assume that it is a play request

			if (streamFile == null) {
				handleError(session, cseq, RtspCode.BadRequest);
				return;
			}


			if (mSessionKey == null) {
				mSessionKey = RandomStringUtils.randomAlphanumeric(17).toUpperCase();
			}

			SocketAddress remote = session.getRemoteAddress();
			if (remote instanceof InetSocketAddress) {
				remoteAddress = ((InetSocketAddress) remote).getAddress().getHostAddress();
			} else {
				remoteAddress = remote.toString();
			}


			if (prepare_output_context(streamId, remoteAddress, clientPort[streamId], serverPort[streamId])) {
				response.setCode(RtspCode.OK);
				response.setHeader(RtspHeaderCode.CSeq, cseq);
				response.setHeader(RtspHeaderCode.Session, mSessionKey);

				rtspTransport.setServerPort(serverPort[streamId]);
				rtspTransport.setLowerTransport(LowerTransport.UDP);

				response.setHeader(RtspHeaderCode.Transport, rtspTransport.toString());

				//avformat_write_header(outputFormatContext[streamId], (PointerPointer)null);

				logger.debug("out_stream time base after header:" + outputFormatContext[streamId].streams(0).time_base().num() +"/" + outputFormatContext[streamId].streams(0).time_base().den());

			}
			else {
				logger.debug("Failed to copy context from input to output stream codec context\n");
				response.setCode(RtspCode.InternalServerError);
			}
		}
		session.write(response);
	}


	private boolean prepare_output_context(int streamId, String remoteAddress, int[] clientPort, int[] serverPort) {
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
		AVCodecParameters avCodecParameters = new AVCodecParameters();

		avcodec_parameters_from_context(avCodecParameters,  in_stream.codec());
		ret  = avcodec_parameters_to_context(out_stream.codec(), avCodecParameters);

		logger.debug("out_stream time base:" + out_stream.time_base().num() +"/" + out_stream.time_base().den());

		if (ret<0){
			logger.debug("Failed to copy context from input to output stream codec context\n");
			return false;
		}

		out_stream.codec().codec_tag(0);
		if ((outputFormatContext[streamId].oformat().flags() & AVFMT_GLOBALHEADER) != 0) {
			//out_stream->codec->flags |= AV_CODEC_FLAG_GLOBAL_HEADER;
			outputFormatContext[streamId].oformat().flags(outputFormatContext[streamId].oformat().flags() | AV_CODEC_FLAG_GLOBAL_HEADER);
		}

		if ((ofmt.flags() & AVFMT_NOFILE) == 0) {
			AVIOContext pb = new AVIOContext(null);

			String rtpUrl = null;
			if (serverPort[0] == 0) 
			{
				while (portNum.get() <= UDP_PORT_MAX) 
				{
					serverPort[0] = portNum.getAndAdd(2);
					// need to +2 because rtp port must be even and rtcp port must be odd
					//rtcp port is automatically set to rtp port + 1

					rtpUrl = "rtp://"+ remoteAddress +":" + clientPort[0] + "?localrtpport=" + serverPort[0]; // + "&localrtcpport=" + rtcpLocalPort; // + "&connect=1";

					ret = avformat.avio_open(pb, rtpUrl, AVIO_FLAG_WRITE);

					if (ret < 0) {
						logger.debug("Could not open url " + rtpUrl);
						continue;
					}
					else {
						logger.debug("Opened url " + rtpUrl);
						outputFormatContext[streamId].pb(pb);
						logger.debug("out_stream time base 1:" + out_stream.time_base().num() +"/" + out_stream.time_base().den());
						break;
					}
				}
				if (portNum.get() >= UDP_PORT_MAX) {
					portNum.set(UDP_PORT_MIN);
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
		avformat_write_header(outputFormatContext[streamId], (PointerPointer)null);
		return true;
	}


	private void onRequestPause(IoSession session, RtspRequest request) {
		// get cesq
		String cseq = request.getHeader(RtspHeaderCode.CSeq);
		if (null == cseq || "".equals(cseq)) {
			logger.error("cesq is null...................");
			handleError(session, "0", RtspCode.HeaderFieldNotValidForResource);
			return;
		}

		// get sessionKey
		String sessionKey = request.getHeader(RtspHeaderCode.Session);
		if (null == sessionKey || "".equals(sessionKey) || mSessionKey == null || !mSessionKey.equals(sessionKey)) {
			logger.error("sessionKey is null...................");
			handleError(session, cseq, RtspCode.SessionNotFound);
			return;
		}
		else {
			RtspResponse response = new RtspResponse();
			response.setCode(RtspCode.OK);
			response.setHeader(RtspHeaderCode.CSeq, cseq);
			//response.setHeader(RtspHeaderCode.Require, "HFC.Delivery.Profile.1.0");
			response.setHeader(RtspHeaderCode.Date, DateUtil.getGmtDate());
			response.setHeader(RtspHeaderCode.Session, sessionKey);
			response.setHeader(RtspHeaderCode.Scale, "1.00");

			closeMuxer(false);

			for (int i = 0; i < inputFormatContext.nb_streams(); i++) {
				if (!prepare_output_context(i, remoteAddress, clientPort[i], serverPort[i])) {
					logger.debug("prepare output context failed...");
				}
				else {
					logger.debug("prepare output context is ok");
				}
			}
			//mPacketSendScheduledFuture.cancel(true);
			/*
			String rangeValue = request.getHeader("Range");
			if (null != rangeValue) {
				String[] rangeValues = rangeValue.split("=");
				response.setHeader(RtspHeaderCode.Range, "npt=" + rangeValues[1] + "233.800");
			} else {
				response.setHeader(RtspHeaderCode.Range, "npt=0.000-");
			}
			 */



			session.write(response);

		}
	}

	private void onRequestGP(IoSession session, RtspRequest request) {
		// get cesq
		String cseq = request.getHeader(RtspHeaderCode.CSeq);
		if (null == cseq || "".equals(cseq)) {
			logger.error("cesq is null...................");
			handleError(session, "0", RtspCode.HeaderFieldNotValidForResource);
			return;
		}

		// get require
		String requireValue = request.getHeader(RtspHeaderCode.Require);
		if (null == requireValue || "".equals(requireValue)
				|| (!requireValue.equals(REQUIRE_VALUE_NGOD_C1))) {
			logger.error("require value ==> {} ", requireValue);
			handleError(session, "0", RtspCode.HeaderFieldNotValidForResource);
			return;
		}

		// get sessionKey
		String sessionKey = request.getHeader(RtspHeaderCode.Session);
		if (null == sessionKey || "".equals(sessionKey) || mSessionKey == null || !mSessionKey.equals(sessionKey)) {
			logger.debug("sessionKey is null...................");
			handleError(session, cseq, RtspCode.SessionNotFound);
		}
		else {
			// sdp
			StringBuffer sdp = new StringBuffer();
			sdp.append("position: 22\r\n");
			sdp.append("presentation state: play\r\n\r\n");
			sdp.append("scale: 1\r\n");

			RtspResponse response = new RtspResponse();
			response.setCode(RtspCode.OK);
			response.setHeader(RtspHeaderCode.CSeq, cseq);
			response.setHeader(RtspHeaderCode.Date, DateUtil.getGmtDate());
			response.setHeader(RtspHeaderCode.Session, sessionKey);
			response.setHeader(RtspHeaderCode.ContentLength, String.valueOf(sdp.length()));
			response.appendToBuffer(sdp);
			session.write(response);
		}
	}

	private void onDefaultRequest(IoSession session, RtspRequest request, String cseq) {
		RtspResponse response = new RtspResponse();
		response.setCode(RtspCode.BadRequest);
		response.setHeader(RtspHeaderCode.CSeq, cseq);
		session.write(response);
	}

	private void handleError(IoSession session, String cseq, RtspCode code) {
		RtspResponse response = new RtspResponse();
		response.setCode(code);
		response.setHeader(RtspHeaderCode.CSeq, cseq);
		session.write(response);
	}


	private void onRequestTeardown(IoSession session, RtspRequest request) {

		// get cesq
		String cseq = request.getHeader(RtspHeaderCode.CSeq);
		if (null == cseq || "".equals(cseq)) {
			logger.error("cesq is null...................");
			handleError(session, "0", RtspCode.HeaderFieldNotValidForResource);
			return;
		}

		// get sessionKey
		String sessionKey = request.getHeader(RtspHeaderCode.Session);
		if (null == sessionKey || "".equals(sessionKey) || mSessionKey == null || !mSessionKey.equals(sessionKey)) {
			logger.error("sessionKey is null...................");
			if (frameReceiverScheduledFuture != null && frameReceiverScheduledFuture.isDone() == false) {
				frameReceiverScheduledFuture.cancel(true);
				if (frameReceiver != null) {
					logger.debug("closing framereceiver muxer");
					frameReceiver.closeMuxer();
				}
				RtspResponse response = new RtspResponse();
				response.setHeader(RtspHeaderCode.CSeq, cseq);
				session.write(response);
				RtspConnectionManager.getInstance().removeConnection(sessionId);
			}
			else {
				handleError(session, cseq, RtspCode.SessionNotFound);
			}

		}
		else {
			close();

			RtspConnectionManager.getInstance().removeConnection(sessionId);

			RtspResponse response = new RtspResponse();
			response.setCode(RtspCode.OK);
			response.setHeader(RtspHeaderCode.CSeq, cseq);
			response.setHeader(RtspHeaderCode.Date, DateUtil.getGmtDate());
			response.setHeader(RtspHeaderCode.Session, sessionKey);
			session.write(response);

		}
	}

	public ThreadPoolTaskScheduler getScheduler() {
		return mTaskScheduler;
	}

	public void setScheduler(ThreadPoolTaskScheduler taskScheduler) {
		this.mTaskScheduler = taskScheduler;
		logger.debug("setting task scheduler");
	}

	public void setSession(IoSession session) {
		this.mSession = session;
	}

	public IoSession getSession() {
		return mSession;
	}

	public void close() {
		closeMuxer(true);
	}


	public void setServer(IServer server) {
		this.mServer = server;
	}


	public void setApplicationContext(ApplicationContext applicationContext) {
		this.mApplicationContext = applicationContext;
	}







}
