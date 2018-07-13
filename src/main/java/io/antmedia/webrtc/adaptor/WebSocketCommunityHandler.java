package io.antmedia.webrtc.adaptor;

import java.io.IOException;

import javax.annotation.Nonnull;
import javax.websocket.EndpointConfig;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.bytedeco.javacpp.avcodec;
import org.bytedeco.javacpp.avutil;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;
import org.webrtc.SessionDescription.Type;

import io.antmedia.recorder.FFmpegFrameRecorder;
import io.antmedia.recorder.FrameRecorder;
import io.antmedia.websocket.IWebSocketListener;

public abstract class WebSocketCommunityHandler {

	private static Logger logger = LoggerFactory.getLogger(WebSocketCommunityHandler.class);

	private JSONParser jsonParser = new JSONParser();

	@OnOpen
	public void onOpen(Session session, EndpointConfig config)
	{

	}

	@OnClose
	public void onClose(Session session) {
		RTMPAdaptor connectionContext = (RTMPAdaptor) session.getUserProperties().get(session.getId());
		if (connectionContext != null) {
			connectionContext.stop();
		}
	}

	@OnError
	public void onError(Session session, Throwable throwable) {

	}
	
	@Nonnull
	public abstract ApplicationContext getAppContext();

	@OnMessage
	public void onMessage(Session session, String message) {
		try {

			if (message == null) {
				logger.error("Received message null for session id: {}" , session.getId());
				return;
			}

			JSONObject jsonObject = (JSONObject) jsonParser.parse(message);

			String cmd = (String) jsonObject.get(IWebSocketListener.COMMAND);
			if (cmd == null) {
				logger.error("Received message does not contain any command for session id: {}" , session.getId());
				return;
			}

			final String streamId = (String) jsonObject.get(IWebSocketListener.STREAM_ID);
			if (streamId == null || streamId.isEmpty()) 
			{
				sendNoStreamIdSpecifiedError(session);
				return;
			}

			if (cmd.equals(IWebSocketListener.PUBLISH_COMMAND)) 
			{

				String outputURL = "rtmp://127.0.0.1/WebRTCApp/" + streamId;

				RTMPAdaptor connectionContext = new RTMPAdaptor(getNewRecorder(outputURL));

				session.getUserProperties().put(session.getId(), connectionContext);

				connectionContext.setSession(session);
				connectionContext.setStreamId(streamId);

				connectionContext.start();

			}
			else if (cmd.equals(IWebSocketListener.TAKE_CONFIGURATION_COMMAND))  
			{

				RTMPAdaptor connectionContext = (RTMPAdaptor) session.getUserProperties().get(session.getId());
				if (connectionContext != null) {
					String typeString = (String)jsonObject.get(IWebSocketListener.TYPE);
					String sdpDescription = (String)jsonObject.get(IWebSocketListener.SDP);

					SessionDescription.Type type;
					if (typeString.equals("offer")) {
						type = Type.OFFER;
						logger.info("received sdp type is offer");
					}
					else {
						type = Type.ANSWER;
						logger.info("received sdp type is answer");
					}
					SessionDescription sdp = new SessionDescription(type, sdpDescription);
					connectionContext.setRemoteDescription(sdp);
				}
				else {
					logger.warn("Connection context is null. Wrong message order for stream: {}", streamId);
				}
			}
			else if (cmd.equals(IWebSocketListener.TAKE_CANDIDATE_COMMAND)) {

				RTMPAdaptor connectionContext = (RTMPAdaptor) session.getUserProperties().get(session.getId());
				if (connectionContext != null) {
					String sdpMid = (String) jsonObject.get(IWebSocketListener.CANDIDATE_ID);
					String sdp = (String) jsonObject.get(IWebSocketListener.CANDIDATE_SDP);
					long sdpMLineIndex = (long)jsonObject.get(IWebSocketListener.CANDIDATE_LABEL);

					IceCandidate iceCandidate = new IceCandidate(sdpMid, (int)sdpMLineIndex, sdp);

					connectionContext.addIceCandidate(iceCandidate);
				}
				else {
					logger.warn("Connection context is null for take candidate. Wrong message order for stream: {}", streamId);
				}

			}
			else if (cmd.equals(IWebSocketListener.STOP_COMMAND)) {
				RTMPAdaptor connectionContext = (RTMPAdaptor) session.getUserProperties().get(session.getId());
				if (connectionContext != null) {
					connectionContext.stop();
				}
				else {
					logger.warn("Connection context is null for stop. Wrong message order for stream: {}", streamId);
					
				}
			}


		}
		catch (Exception e) {
			logger.error(ExceptionUtils.getStackTrace(e));
		}

	}


	@SuppressWarnings("unchecked")
	public static void sendSDPConfiguration(String description, String type, String streamId, Session session) {
		JSONObject jsonResponseObject = new JSONObject();
		jsonResponseObject.put(IWebSocketListener.COMMAND, IWebSocketListener.TAKE_CONFIGURATION_COMMAND);
		jsonResponseObject.put(IWebSocketListener.SDP, description);
		jsonResponseObject.put(IWebSocketListener.TYPE, type);
		jsonResponseObject.put(IWebSocketListener.STREAM_ID, streamId);
		sendMessage(jsonResponseObject.toJSONString(), session);
	}

	@SuppressWarnings("unchecked")
	public static void sendPublishStartedMessage(String streamId, Session session) {
		JSONObject jsonObj = new JSONObject();
		jsonObj.put(IWebSocketListener.COMMAND, IWebSocketListener.NOTIFICATION_COMMAND);
		jsonObj.put(IWebSocketListener.DEFINITION, IWebSocketListener.PUBLISH_STARTED);
		jsonObj.put(IWebSocketListener.STREAM_ID, streamId);

		sendMessage(jsonObj.toJSONString(), session);
	}

	@SuppressWarnings("unchecked")
	public static void sendPublishFinishedMessage(String streamId, Session session) {
		JSONObject jsonObject = new JSONObject();
		jsonObject.put(IWebSocketListener.COMMAND, IWebSocketListener.NOTIFICATION_COMMAND);
		jsonObject.put(IWebSocketListener.DEFINITION,  IWebSocketListener.PUBLISH_FINISHED);
		jsonObject.put(IWebSocketListener.STREAM_ID, streamId);

		sendMessage(jsonObject.toJSONString(), session);
	}

	@SuppressWarnings("unchecked")
	public static void sendStartMessage(String streamId, Session session) 
	{
		JSONObject jsonObject = new JSONObject();
		jsonObject.put(IWebSocketListener.COMMAND, IWebSocketListener.START_COMMAND);
		jsonObject.put(IWebSocketListener.STREAM_ID, streamId);

		sendMessage(jsonObject.toJSONString(), session);
	}


	public static FFmpegFrameRecorder getNewRecorder(String outputURL) {

		FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(outputURL, 640, 480, 1);
		recorder.setFormat("flv");
		recorder.setSampleRate(44100);
		// Set in the surface changed method
		recorder.setFrameRate(30);
		recorder.setPixelFormat(avutil.AV_PIX_FMT_YUV420P);
		recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
		recorder.setAudioCodec(avcodec.AV_CODEC_ID_AAC);
		recorder.setAudioChannels(2);
		recorder.setGopSize(20);

		try {
			recorder.start();
		} catch (FrameRecorder.Exception e) {
			e.printStackTrace();
		}

		return recorder;
	}

	@SuppressWarnings("unchecked")
	protected static final  void sendNoStreamIdSpecifiedError(Session session)  {
		JSONObject jsonResponse = new JSONObject();
		jsonResponse.put(IWebSocketListener.COMMAND, IWebSocketListener.ERROR_COMMAND);
		jsonResponse.put(IWebSocketListener.DEFINITION, IWebSocketListener.NO_STREAM_ID_SPECIFIED);
		sendMessage(jsonResponse.toJSONString(), session);	
	}

	@SuppressWarnings("unchecked")
	public static void sendTakeCandidateMessage(long sdpMLineIndex, String sdpMid, String sdp, String streamId, Session session)
	{
		JSONObject jsonObject = new JSONObject();
		jsonObject.put(IWebSocketListener.COMMAND,  IWebSocketListener.TAKE_CANDIDATE_COMMAND);
		jsonObject.put(IWebSocketListener.CANDIDATE_LABEL, sdpMLineIndex);
		jsonObject.put(IWebSocketListener.CANDIDATE_ID, sdpMid);
		jsonObject.put(IWebSocketListener.CANDIDATE_SDP, sdp);
		jsonObject.put(IWebSocketListener.STREAM_ID, streamId);

		sendMessage(jsonObject.toJSONString(), session);
	}


	public static void sendMessage(String message, Session session) {
		synchronized (session) {
			if (session.isOpen()) {
				try {
					session.getBasicRemote().sendText(message);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}



}
