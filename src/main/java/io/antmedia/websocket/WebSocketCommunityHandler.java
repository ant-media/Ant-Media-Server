package io.antmedia.websocket;

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
import io.antmedia.webrtc.adaptor.RTMPAdaptor;

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

			String cmd = (String) jsonObject.get(WebSocketConstants.COMMAND);
			if (cmd == null) {
				logger.error("Received message does not contain any command for session id: {}" , session.getId());
				return;
			}

			final String streamId = (String) jsonObject.get(WebSocketConstants.STREAM_ID);
			if (streamId == null || streamId.isEmpty()) 
			{
				sendNoStreamIdSpecifiedError(session);
				return;
			}

			if (cmd.equals(WebSocketConstants.PUBLISH_COMMAND)) 
			{
				//get scope and use its name
				startRTMPAdaptor(session, streamId);
			}
			else if (cmd.equals(WebSocketConstants.TAKE_CONFIGURATION_COMMAND))  
			{

				RTMPAdaptor connectionContext = (RTMPAdaptor) session.getUserProperties().get(session.getId());
				String typeString = (String)jsonObject.get(WebSocketConstants.TYPE);
				String sdpDescription = (String)jsonObject.get(WebSocketConstants.SDP);
				setRemoteDescription(connectionContext, typeString, sdpDescription, streamId);

			}
			else if (cmd.equals(WebSocketConstants.TAKE_CANDIDATE_COMMAND)) {

				RTMPAdaptor connectionContext = (RTMPAdaptor) session.getUserProperties().get(session.getId());
				String sdpMid = (String) jsonObject.get(WebSocketConstants.CANDIDATE_ID);
				String sdp = (String) jsonObject.get(WebSocketConstants.CANDIDATE_SDP);
				long sdpMLineIndex = (long)jsonObject.get(WebSocketConstants.CANDIDATE_LABEL);

				addICECandidate(streamId, connectionContext, sdpMid, sdp, sdpMLineIndex);

			}
			else if (cmd.equals(WebSocketConstants.STOP_COMMAND)) {
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

	private void startRTMPAdaptor(Session session, final String streamId) {
		
		//get scope and use its name
		String outputURL = "rtmp://127.0.0.1/WebRTCApp/" + streamId;

		RTMPAdaptor connectionContext = getNewRTMPAdaptor(outputURL);

		session.getUserProperties().put(session.getId(), connectionContext);

		connectionContext.setSession(session);
		connectionContext.setStreamId(streamId);

		connectionContext.start();
	}

	public RTMPAdaptor getNewRTMPAdaptor(String outputURL) {
		return new RTMPAdaptor(getNewRecorder(outputURL), this);
	}

	public void addICECandidate(final String streamId, RTMPAdaptor connectionContext, String sdpMid, String sdp,
			long sdpMLineIndex) {
		if (connectionContext != null) {
			IceCandidate iceCandidate = new IceCandidate(sdpMid, (int)sdpMLineIndex, sdp);

			connectionContext.addIceCandidate(iceCandidate);
		}
		else {
			logger.warn("Connection context is null for take candidate. Wrong message order for stream: {}", streamId);
		}
	}


	private void setRemoteDescription(RTMPAdaptor connectionContext, String typeString, String sdpDescription, String streamId) {
		if (connectionContext != null) {
			SessionDescription.Type type;
			if (typeString.equals("offer")) {
				type = Type.OFFER;
				logger.info("received sdp type is offer {}", streamId);
			}
			else {
				type = Type.ANSWER;
				logger.info("received sdp type is answer {}", streamId);
			}
			SessionDescription sdp = new SessionDescription(type, sdpDescription);
			connectionContext.setRemoteDescription(sdp);
		}
		else {
			logger.warn("Connection context is null. Wrong message order for stream: {}", streamId);
		}

	}

	@SuppressWarnings("unchecked")
	public  void sendSDPConfiguration(String description, String type, String streamId, Session session) {
		JSONObject jsonResponseObject = new JSONObject();
		jsonResponseObject.put(WebSocketConstants.COMMAND, WebSocketConstants.TAKE_CONFIGURATION_COMMAND);
		jsonResponseObject.put(WebSocketConstants.SDP, description);
		jsonResponseObject.put(WebSocketConstants.TYPE, type);
		jsonResponseObject.put(WebSocketConstants.STREAM_ID, streamId);
		sendMessage(jsonResponseObject.toJSONString(), session);
	}

	@SuppressWarnings("unchecked")
	public  void sendPublishStartedMessage(String streamId, Session session) {
		JSONObject jsonObj = new JSONObject();
		jsonObj.put(WebSocketConstants.COMMAND, WebSocketConstants.NOTIFICATION_COMMAND);
		jsonObj.put(WebSocketConstants.DEFINITION, WebSocketConstants.PUBLISH_STARTED);
		jsonObj.put(WebSocketConstants.STREAM_ID, streamId);

		sendMessage(jsonObj.toJSONString(), session);
	}

	@SuppressWarnings("unchecked")
	public  void sendPublishFinishedMessage(String streamId, Session session) {
		JSONObject jsonObject = new JSONObject();
		jsonObject.put(WebSocketConstants.COMMAND, WebSocketConstants.NOTIFICATION_COMMAND);
		jsonObject.put(WebSocketConstants.DEFINITION,  WebSocketConstants.PUBLISH_FINISHED);
		jsonObject.put(WebSocketConstants.STREAM_ID, streamId);

		sendMessage(jsonObject.toJSONString(), session);
	}

	@SuppressWarnings("unchecked")
	public  void sendStartMessage(String streamId, Session session) 
	{
		JSONObject jsonObject = new JSONObject();
		jsonObject.put(WebSocketConstants.COMMAND, WebSocketConstants.START_COMMAND);
		jsonObject.put(WebSocketConstants.STREAM_ID, streamId);

		sendMessage(jsonObject.toJSONString(), session);
	}


	public static FFmpegFrameRecorder getNewRecorder(String outputURL) {

		FFmpegFrameRecorder recorder = initRecorder(outputURL);

		try {
			recorder.start();
		} catch (FrameRecorder.Exception e) {
			logger.error(ExceptionUtils.getStackTrace(e));
		}

		return recorder;
	}

	public static FFmpegFrameRecorder initRecorder(String outputURL) {
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
		return recorder;
	}

	@SuppressWarnings("unchecked")
	protected  final  void sendNoStreamIdSpecifiedError(Session session)  {
		JSONObject jsonResponse = new JSONObject();
		jsonResponse.put(WebSocketConstants.COMMAND, WebSocketConstants.ERROR_COMMAND);
		jsonResponse.put(WebSocketConstants.DEFINITION, WebSocketConstants.NO_STREAM_ID_SPECIFIED);
		sendMessage(jsonResponse.toJSONString(), session);	
	}

	@SuppressWarnings("unchecked")
	public void sendTakeCandidateMessage(long sdpMLineIndex, String sdpMid, String sdp, String streamId, Session session)
	{
		JSONObject jsonObject = new JSONObject();
		jsonObject.put(WebSocketConstants.COMMAND,  WebSocketConstants.TAKE_CANDIDATE_COMMAND);
		jsonObject.put(WebSocketConstants.CANDIDATE_LABEL, sdpMLineIndex);
		jsonObject.put(WebSocketConstants.CANDIDATE_ID, sdpMid);
		jsonObject.put(WebSocketConstants.CANDIDATE_SDP, sdp);
		jsonObject.put(WebSocketConstants.STREAM_ID, streamId);

		sendMessage(jsonObject.toJSONString(), session);
	}


	@SuppressWarnings("unchecked")
	public void sendMessage(String message, final Session session) {
		synchronized (this) {
			if (session.isOpen()) {
				try {
					session.getBasicRemote().sendText(message);
				} catch (IOException e) {
					logger.error(ExceptionUtils.getStackTrace(e));
				}
			}
		}
	}



}
