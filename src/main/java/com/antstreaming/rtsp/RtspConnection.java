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
import org.red5.server.messaging.IMessageInput;
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

	private Logger logger = LoggerFactory.getLogger(RtspConnection.class);

	protected static final String SLASH = "/";
	private static final String CSEQ = "cseq";
	private static final String REQUIRE_VALUE_NGOD_R2 = "com.comcast.ngod.r2";
	private static final String REQUIRE_VALUE_NGOD_C1 = "com.comcast.ngod.c1";

	public static final int UDP_PORT_MIN = 5000;

	public static final int UDP_PORT_MAX = 65000;

	public static AtomicInteger PORT_NUMBER = new AtomicInteger(UDP_PORT_MIN);


	private ThreadPoolTaskScheduler mTaskScheduler;
	private IoSession mSession;

	private IServer mServer;

	private ScheduledFuture<?> mPacketSenderScheduledFuture;

	private String mSessionKey = RandomStringUtils.randomAlphanumeric(17).toUpperCase();;

	private int[][] serverPort;

	private int[][] clientPort;

	private String remoteAddress;

	private StringBuffer liveStreamSdpDef;

	private ApplicationContext mApplicationContext;

	//	private File streamFile;

	private ScheduledFuture<?> mPacketReceiverScheduledFuture;

	private PacketReceiverRunnable frameReceiver;

	private PacketSenderRunnable frameSender;

	private IScope scope;

	private String announcedStreamName;

	private String mode;

	//private ClientBroadcastStream bs;


	public void handleMessage(IoSession session, Object message) {
		//get RTSP connection in here and let rtsp connection receive this message
		//rtsp connection should receive this message from a task queue
		//do not forget to add this job to a task queue as rtmp conn does

		RtspRequest request = (RtspRequest) message;
		String cseq = request.getHeader(RtspHeaderCode.CSeq);
		session.setAttribute(CSEQ, cseq);
		switch (request.getVerb()) {
		case OPTIONS:
			onOptions(session, request);
			break;
		case DESCRIBE:
			onDescribe(session, request);
			break;
		case SETUP:
			onSetup(session, request);
			break;
		case TEARDOWN:
			onTeardown(session, request);
			break;
		case PLAY:
			onPlay(session, request);
			break;
		case PAUSE:
			onPause(session, request);
			break;
		case GET_PARAMETER:
			onGP(session, request);
			break;
		case ANNOUNCE:
			onAnnounce(session, request);
			break;
		case RECORD:
			onRecord(session, request);
			break;
		default:
			onDefaultRequest(session, request, request.getHeader(RtspHeaderCode.CSeq));
		}
	}

	private void onRecord(IoSession session, RtspRequest request) {
		String cseq = request.getHeader(RtspHeaderCode.CSeq);
		if (cseq == null || "".equals(cseq)) {
			cseq = "0";
		}

		String sessionKey = request.getHeader(RtspHeaderCode.Session);
		if (null == sessionKey || "".equals(sessionKey) || mSessionKey == null || !mSessionKey.equals(sessionKey)) {
			logger.error("sessionKey is null...................");
			handleError(session, cseq, RtspCode.SessionNotFound);
			return;
		}

		
		frameReceiver = new PacketReceiverRunnable(mTaskScheduler, cseq, sessionKey, session, announcedStreamName, liveStreamSdpDef, request.getUrl());
		mPacketReceiverScheduledFuture = mTaskScheduler.schedule(frameReceiver, new Date());

	}

	private void onAnnounce(IoSession session, RtspRequest request) {
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

			announcedStreamName = getStreamName(url.getPath());
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
			String fileName = generator.generateFilename(scope, announcedStreamName, ".mp4", GenerationType.RECORD);


			//		streamFile = scope.getContext().getResource(fileName).getFile();

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


	private void onPlay(IoSession session, RtspRequest request) {
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
		long duration = frameSender.getDuration();
		float durationInSeconds =  duration/1000000;


		long seekTime = 0; //should in micro seconds
		if (null != rangeValue) {
			String[] rangeValues = rangeValue.split("=");
			String startTime = "0.000";
			if (rangeValues.length >=2) {
				startTime = rangeValues[1].substring(0, rangeValues[1].indexOf("-"));
				seekTime = (long)(Float.parseFloat(startTime) * 1000000);
				logger.debug("seek time :" + seekTime);
				if (durationInSeconds < 0) {
					response.setHeader(RtspHeaderCode.Range, "npt=0.000-");
				}
				else if (seekTime <= duration) {
					//duration in 
					int ret = frameSender.seek(seekTime);
					if (ret >= 0) {
						response.setHeader(RtspHeaderCode.Range, "npt=" + startTime + "-" + durationInSeconds);
					}
					else {
						logger.debug("cannot seek the file to specified timestamp" + seekTime);
						response.setCode(RtspCode.InternalServerError);
					}

				}
				else {
					response.setCode(RtspCode.InvalidRange);
				}
			}

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
			mPacketSenderScheduledFuture = mTaskScheduler.scheduleAtFixedRate(frameSender, 10);
			//convert seektime from micro seconds to nano seconds
		}
		else {
			logger.debug("not starting to play");
		}

	}


	private void onOptions(IoSession session, RtspRequest request) {
		RtspResponse response = new RtspResponse();
		response.setHeader(RtspHeaderCode.CSeq, request.getHeader(RtspHeaderCode.CSeq));
		response.setHeader(RtspHeaderCode.Public, "DESCRIBE, SETUP, TEARDOWN, PLAY, PAUSE, ANNOUNCE");
		response.setHeader(RtspHeaderCode.Server, "RtspServer");
		response.setHeader(RtspHeaderCode.ContentLength, "0");
		session.write(response);
	}


	private void onDescribe(IoSession session, RtspRequest request) {
		//TODO: call context playback security here
		RtspResponse response = new RtspResponse();
		String cseq = request.getHeader(RtspHeaderCode.CSeq);
		if (null != cseq && !"".equals(cseq)) {
			response.setHeader(RtspHeaderCode.CSeq, cseq);
		}

		URI url;
		try {
			url = new URI(request.getUrl());

			String rtmpUrl = "rtmp://" + url.getHost() + "/" + url.getPath();

			logger.debug("rtmp url is: " + rtmpUrl);
			logger.debug("on request describe host: " + url.getHost() + " path:" + url.getPath());


			String streamName = getStreamName(url.getPath());
			String app = getAppName(url.getPath());

			logger.debug("app:"  + app + " streamName:" + streamName);

			IScope scope = getScope(url, app);

			//	IStreamService streamService = (IStreamService) ScopeUtils.getScopeService(scope, IStreamService.class, StreamService.class);

			IProviderService providerService = getProviderService(scope);


			//		streamFile = providerService.getVODProviderFile(scope, streamName);

			INPUT_TYPE input = providerService.lookupProviderInput(scope, streamName, -2);
			logger.debug("input type is " + input + " live wait is:" + INPUT_TYPE.LIVE_WAIT);

			if (input != INPUT_TYPE.LIVE_WAIT) 
			{
				response.setHeader(RtspHeaderCode.Date, DateUtil.getGmtDate());
				response.setHeader(RtspHeaderCode.ContentType, "application/sdp");
				response.setHeader(RtspHeaderCode.Server, "RtspServer");

				//TODO: do this job with a task in an executor
				AVFormatContext inputFormatContext = new AVFormatContext(null);
				int ret = avformat_open_input(inputFormatContext, rtmpUrl, null, null);
				if (ret == 0) {
					byte[] sdpData = new byte[16384];
					//pFormatCtx.nb_streams()
					if (avformat_find_stream_info(inputFormatContext, (PointerPointer)null) >= 0) {
						if (av_sdp_create(inputFormatContext, inputFormatContext.nb_streams(), sdpData, sdpData.length) == 0) 
						{
							serverPort = new int[inputFormatContext.nb_streams()][2];
							clientPort = new int[inputFormatContext.nb_streams()][2];
							StringBuffer sdp = new StringBuffer();
							sdp.append(new String(sdpData));
							response.setHeader(RtspHeaderCode.ContentLength, String.valueOf(sdp.length()));
							response.setBuffer(sdp);
							frameSender = new PacketSenderRunnable(inputFormatContext);
						}
						else {
							response.setCode(RtspCode.InternalServerError);
							logger.debug("cannot find stream info in the context");
						}
					}
					else {
						response.setCode(RtspCode.InternalServerError);
						logger.debug("could not get sdp info of " + rtmpUrl);
					}
				}
				else {
					byte[] data = new byte[4096];
					avutil.av_strerror(ret, data, data.length);
					logger.warn("could not opened file " + rtmpUrl + " error code:" + ret
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


	private void onSetup(IoSession session, RtspRequest request) 
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
		mode = rtspTransport.getMode();

		if (mode != null && mode.equals("record")) {
			//TODO check the url and do this operation according to the control parameter in the sdp

			int portNo = PORT_NUMBER.getAndAdd(2);
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

			SocketAddress remote = session.getRemoteAddress();
			if (remote instanceof InetSocketAddress) {
				remoteAddress = ((InetSocketAddress) remote).getAddress().getHostAddress();
			} else {
				remoteAddress = remote.toString();
			}


			if (frameSender.prepare_output_context(streamId, remoteAddress, clientPort[streamId], serverPort[streamId])) {
				response.setCode(RtspCode.OK);
				response.setHeader(RtspHeaderCode.CSeq, cseq);
				response.setHeader(RtspHeaderCode.Session, mSessionKey);

				rtspTransport.setServerPort(serverPort[streamId]);
				rtspTransport.setLowerTransport(LowerTransport.UDP);

				response.setHeader(RtspHeaderCode.Transport, rtspTransport.toString());

			}
			else {
				logger.debug("Failed to copy context from input to output stream codec context\n");
				response.setCode(RtspCode.InternalServerError);
			}
		}
		session.write(response);
	}





	private void onPause(IoSession session, RtspRequest request) {
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


			frameSender.closeMuxer(false);
			mPacketSenderScheduledFuture.cancel(true);
			int streamCount = frameSender.getStreamCount();
			for (int i = 0; i < streamCount; i++) {
				if (!frameSender.prepare_output_context(i, remoteAddress, clientPort[i], serverPort[i])) {
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

	private void onGP(IoSession session, RtspRequest request) {
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


	private void onTeardown(IoSession session, RtspRequest request) {

		// get cesq
		String cseq = request.getHeader(RtspHeaderCode.CSeq);
		if (null == cseq || "".equals(cseq)) {
			logger.error("cesq is null...................");
			handleError(session, "0", RtspCode.HeaderFieldNotValidForResource);
			return;
		}

		// get sessionKey
		String sessionKey = request.getHeader(RtspHeaderCode.Session);
		if (null == sessionKey || "".equals(sessionKey) || mSessionKey == null || !mSessionKey.equals(sessionKey))
		{
			handleError(session, "0", RtspCode.SessionNotFound);
			return;
		}


		
		RtspResponse response = new RtspResponse();
		response.setCode(RtspCode.OK);
		response.setHeader(RtspHeaderCode.CSeq, cseq);
		response.setHeader(RtspHeaderCode.Date, DateUtil.getGmtDate());
		response.setHeader(RtspHeaderCode.Session, sessionKey);
		session.write(response);
		logger.debug("tear down called");
		close();
		
		/*
		if (mode != null && mode.equals("record")) 
		{
			if (mPacketReceiverScheduledFuture != null && mPacketReceiverScheduledFuture.isDone() == false) {
				mPacketReceiverScheduledFuture.cancel(false);
				if (frameReceiver != null) {
					logger.debug("closing framereceiver muxer");
					frameReceiver.closeMuxer();
				}
				RtspResponse response = new RtspResponse();
				response.setHeader(RtspHeaderCode.CSeq, cseq);
				session.write(response);
			}
			else {
				handleError(session, cseq, RtspCode.SessionNotFound);
			}

		}
		else {


			RtspResponse response = new RtspResponse();
			response.setCode(RtspCode.OK);
			response.setHeader(RtspHeaderCode.CSeq, cseq);
			response.setHeader(RtspHeaderCode.Date, DateUtil.getGmtDate());
			response.setHeader(RtspHeaderCode.Session, sessionKey);
			session.write(response);

		}
		 */
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

		if (frameSender != null) {
			frameSender.closeMuxer(true);
			frameSender = null;
		}
		
		if (mPacketSenderScheduledFuture != null && mPacketSenderScheduledFuture.isDone() == false) {
			logger.debug("cancelling packet sender scheduledFuture");
			mPacketSenderScheduledFuture.cancel(false);
		}
	

		if (frameReceiver != null) {
			frameReceiver.closeMuxer();
			frameReceiver = null;
		}

		if (mPacketReceiverScheduledFuture != null && mPacketReceiverScheduledFuture.isDone() == false) {
			logger.debug("cancelling packet receiver scheduledFuture");
			mPacketReceiverScheduledFuture.cancel(false);
		}

	}


	public void setServer(IServer server) {
		this.mServer = server;
	}


	public void setApplicationContext(ApplicationContext applicationContext) {
		this.mApplicationContext = applicationContext;
	}







}
