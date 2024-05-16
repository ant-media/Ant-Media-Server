package io.antmedia.websocket;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;
import org.webrtc.SessionDescription.Type;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.AppSettings;
import io.antmedia.StreamIdValidator;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.muxer.IAntMediaStreamHandler;
import io.antmedia.settings.ServerSettings;
import io.antmedia.webrtc.adaptor.RTMPAdaptor;
import jakarta.websocket.Session;

public class WebSocketCommunityHandler {

	public static final String WEBRTC_VERTX_BEAN_NAME = "webRTCVertx";
	
	private static Logger logger = LoggerFactory.getLogger(WebSocketCommunityHandler.class);

	protected AppSettings appSettings;

	private ApplicationContext appContext;

	protected Session session;

	private String appName;

	private AntMediaApplicationAdapter appAdaptor;
	
	protected String userAgent = "N/A";
	
	public WebSocketCommunityHandler(ApplicationContext appContext, Session session) {
		this.appContext = appContext;
		this.session = session;
		appSettings = (AppSettings) getAppContext().getBean(AppSettings.BEAN_NAME);
		appAdaptor = ((AntMediaApplicationAdapter)appContext.getBean(AntMediaApplicationAdapter.BEAN_NAME));
		
		appName = appAdaptor.getScope().getName();
	}
	
	public void onClose(Session session) {
		RTMPAdaptor connectionContext = (RTMPAdaptor) session.getUserProperties().get(session.getId());
		if (connectionContext != null) {
			connectionContext.stop();
		}
	}

	public void onError(Session session, Throwable throwable) {
		//not used for now
	}

	public void onMessage(Session session, String message) {
		//json parser is not thread-safe
		JSONParser jsonParser = new JSONParser();
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
			if ((streamId == null || streamId.isEmpty())
					&& !cmd.equals(WebSocketConstants.PING_COMMAND)) 
			{
				sendNoStreamIdSpecifiedError(session);
				return;
			}
			
			if(!StreamIdValidator.isStreamIdValid(streamId)) {
				sendInvalidStreamNameError(streamId, session);
				return;
			}

			if (cmd.equals(WebSocketConstants.PUBLISH_COMMAND)) 
			{
				Broadcast broadcast = appAdaptor.getDataStore().get(streamId);
				if (broadcast != null) {
					String status = broadcast.getStatus();
					if (status.endsWith(IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING)
							||
							status.endsWith(IAntMediaStreamHandler.BROADCAST_STATUS_PREPARING)) 
					{
						logger.error("Sending stream id in use error for stream:{} session:{}", streamId, session.getId());
						sendStreamIdInUse(streamId, session);
						return;
					}
				}
				
				//Get if enableVideo is true or false
				boolean enableVideo = jsonObject.containsKey(WebSocketConstants.VIDEO) ? (boolean) jsonObject.get(WebSocketConstants.VIDEO) : true;
				//audio is by default true 
				
				//get scope and use its name
				startRTMPAdaptor(session, streamId, enableVideo);
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

				
				addICECandidate(streamId, connectionContext, ((sdpMid != null) ? sdpMid : "0"), sdp, sdpMLineIndex);

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
			else if (cmd.equals(WebSocketConstants.PING_COMMAND)) {
				sendPongMessage(session);
			}
			else if (cmd.equals(WebSocketConstants.GET_STREAM_INFO_COMMAND) || cmd.equals(WebSocketConstants.PLAY_COMMAND)) 
			{
				sendNotFoundJSON(streamId, session);
			}
			


		}
		catch (Exception e) {
			logger.error(ExceptionUtils.getStackTrace(e));
		}

	}
		

	private void startRTMPAdaptor(Session session, final String streamId, boolean enableVideo) {
		int rtmpPort = appAdaptor.getServerSettings().getRtmpPort();
		//get scope and use its name
		String outputURL = "rtmp://127.0.0.1" + ":" + rtmpPort +"/"+ appName +"/" + streamId;

		RTMPAdaptor connectionContext = getNewRTMPAdaptor(outputURL, appSettings.getHeightRtmpForwarding());

		session.getUserProperties().put(session.getId(), connectionContext);

		connectionContext.setSession(session);
		connectionContext.setStreamId(streamId);
		connectionContext.setPortRange(appSettings.getWebRTCPortRangeMin(), appSettings.getWebRTCPortRangeMax());
		connectionContext.setStunServerUri(appSettings.getStunServerURI(), appSettings.getTurnServerUsername(), appSettings.getTurnServerCredential());
		connectionContext.setTcpCandidatesEnabled(appSettings.isWebRTCTcpCandidatesEnabled());
		connectionContext.setEnableVideo(enableVideo);	
		connectionContext.start();
	}

	public RTMPAdaptor getNewRTMPAdaptor(String outputURL, int height) {
		return new RTMPAdaptor(outputURL, this, height, "flv");
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
			if ("offer".equals(typeString)) {
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
	public  void sendSDPConfiguration(String description, String type, String streamId, Session session, Map<String, String> midSidMap, String linkedSessionForSignaling, String subscriberId) {

		sendMessage(getSDPConfigurationJSON (description, type,  streamId, midSidMap, linkedSessionForSignaling, subscriberId).toJSONString(), session);
	}

	@SuppressWarnings("unchecked")
	public  void sendPublishStartedMessage(String streamId, Session session, String roomName, String subscriberId) {
		
		JSONObject jsonObj = new JSONObject();
		jsonObj.put(WebSocketConstants.COMMAND, WebSocketConstants.NOTIFICATION_COMMAND);
		jsonObj.put(WebSocketConstants.DEFINITION, WebSocketConstants.PUBLISH_STARTED);
		jsonObj.put(WebSocketConstants.STREAM_ID, streamId);

		if(roomName != null) {
			jsonObj.put(WebSocketConstants.ATTR_ROOM_NAME, roomName); //keep it for compatibility
			jsonObj.put(WebSocketConstants.ROOM, roomName);
		}
		jsonObj.put(WebSocketConstants.SUBSCRIBER_ID, subscriberId);

		sendMessage(jsonObj.toJSONString(), session);
	}
	
	public void sendStreamIdInUse(String streamId, Session session) {
		JSONObject jsonResponse = new JSONObject();
		jsonResponse.put(WebSocketConstants.COMMAND, WebSocketConstants.ERROR_COMMAND);
		jsonResponse.put(WebSocketConstants.DEFINITION, WebSocketConstants.STREAM_ID_IN_USE);
		jsonResponse.put(WebSocketConstants.STREAM_ID, streamId);
		sendMessage(jsonResponse.toJSONString(), session);
	}
	
	@SuppressWarnings("unchecked")
	public void sendPongMessage(Session session) {
		JSONObject jsonResponseObject = new JSONObject();
		jsonResponseObject.put(WebSocketConstants.COMMAND, WebSocketConstants.PONG_COMMAND);
		sendMessage(jsonResponseObject.toJSONString(), session);
	}
	

	@SuppressWarnings("unchecked")
	public  void sendPublishFinishedMessage(String streamId, Session session, String subscriberId) {
		JSONObject jsonObject = new JSONObject();
		jsonObject.put(WebSocketConstants.COMMAND, WebSocketConstants.NOTIFICATION_COMMAND);
		jsonObject.put(WebSocketConstants.DEFINITION,  WebSocketConstants.PUBLISH_FINISHED);
		jsonObject.put(WebSocketConstants.STREAM_ID, streamId);
		jsonObject.put(WebSocketConstants.SUBSCRIBER_ID, subscriberId);
		sendMessage(jsonObject.toJSONString(), session);
	}

	@SuppressWarnings("unchecked")
	public  void sendStartMessage(String streamId, Session session, String subscriberId) 
	{
		JSONObject jsonObject = new JSONObject();
		jsonObject.put(WebSocketConstants.COMMAND, WebSocketConstants.START_COMMAND);
		jsonObject.put(WebSocketConstants.STREAM_ID, streamId);
		jsonObject.put(WebSocketConstants.SUBSCRIBER_ID, subscriberId);

		sendMessage(jsonObject.toJSONString(), session);
	}


	@SuppressWarnings("unchecked")
	public  final  void sendNoStreamIdSpecifiedError(Session session)  {
		JSONObject jsonResponse = new JSONObject();
		jsonResponse.put(WebSocketConstants.COMMAND, WebSocketConstants.ERROR_COMMAND);
		jsonResponse.put(WebSocketConstants.DEFINITION, WebSocketConstants.NO_STREAM_ID_SPECIFIED);
		sendMessage(jsonResponse.toJSONString(), session);
	}
	
	@SuppressWarnings("unchecked")
	public void sendTakeCandidateMessage(long sdpMLineIndex, String sdpMid, String sdp, String streamId, Session session, String linkedSessionForSignaling, String subscriberId)
	{

		sendMessage(getTakeCandidateJSON(sdpMLineIndex, sdpMid, sdp, streamId, linkedSessionForSignaling, subscriberId).toJSONString(), session);
	}


	@SuppressWarnings("unchecked")
	public void sendMessage(String message, final Session session) {
		synchronized (this) {
			if (session.isOpen()) {
				try {
					session.getBasicRemote().sendText(message);
				} 
				catch (Exception e) { 
					//capture all exceptions because some unexpected events may happen it causes some internal errors
					String exceptioMessage = e.getMessage();
					
					//ignore following messages
					if (exceptioMessage == null || !exceptioMessage.contains("WebSocket session has been closed")) 
					{
						logger.error(ExceptionUtils.getStackTrace(e));
					}
					
				}
			}
		}
	}
	

	
	@SuppressWarnings("unchecked")
	public void sendRoomNotActiveInformation(String roomId) {
		JSONObject jsonResponse = new JSONObject();
		jsonResponse.put(WebSocketConstants.COMMAND, WebSocketConstants.ERROR_COMMAND);
		jsonResponse.put(WebSocketConstants.DEFINITION, WebSocketConstants.ROOM_NOT_ACTIVE);
		jsonResponse.put(WebSocketConstants.ROOM, roomId);
		sendMessage(jsonResponse.toJSONString(), session);
	}
	
	/**
	 * 
	 * @param streamIdNameMap this is the map that keys are stream ids and values are stream names
	 * @param roomId is the id of the room
	 * @param subscriberId 
	 */
	public void sendRoomInformation(Map<String,String> streamIdNameMap , String roomId) 
	{
		JSONObject jsObject = new JSONObject();
		JSONArray jsonStreamIdArray = new JSONArray();
		JSONArray jsonStreamListArray = new JSONArray();
		
		prepareStreamListJSON(streamIdNameMap, jsonStreamIdArray, jsonStreamListArray, new HashMap<String, String>());
        
		jsObject.put(WebSocketConstants.COMMAND, WebSocketConstants.ROOM_INFORMATION_NOTIFICATION);
		jsObject.put(WebSocketConstants.STREAMS_IN_ROOM, jsonStreamIdArray);
		//This field is deprecated. Use STREAM_LIST_IN_ROOM 
		jsObject.put(WebSocketConstants.STREAM_LIST_IN_ROOM, jsonStreamListArray);	
		jsObject.put(WebSocketConstants.ATTR_ROOM_NAME, roomId);
		jsObject.put(WebSocketConstants.ROOM, roomId);
		String jsonString = jsObject.toJSONString();
		sendMessage(jsonString, session);
	}

	private void prepareStreamListJSON(Map<String, String> streamIdNameMap, JSONArray jsonStreamIdArray,
			JSONArray jsonStreamListArray, HashMap<String, String> streamMetaDataMap) {
		if(streamIdNameMap != null) {
			for (Map.Entry<String, String> e : streamIdNameMap.entrySet()) {
				jsonStreamIdArray.add(e.getKey());
				JSONObject jsStreamObject = new JSONObject();
				jsStreamObject.put(WebSocketConstants.STREAM_ID, e.getKey());
				jsStreamObject.put(WebSocketConstants.STREAM_NAME, e.getValue());
				jsStreamObject.put(WebSocketConstants.META_DATA, streamMetaDataMap.get(e.getKey()));
				jsonStreamListArray.add(jsStreamObject);
			}
		}
	}
	
	public void sendJoinedRoomMessage(String room, String newStreamId, Map<String,String> streamIdNameMap, HashMap<String, String> streamMetaDataMap ) {
		JSONObject jsonResponse = new JSONObject();
		JSONArray jsonStreamIdArray = new JSONArray();
		JSONArray jsonStreamListArray = new JSONArray();
		
		prepareStreamListJSON(streamIdNameMap, jsonStreamIdArray, jsonStreamListArray, streamMetaDataMap);
		jsonResponse.put(WebSocketConstants.COMMAND, WebSocketConstants.NOTIFICATION_COMMAND);
		jsonResponse.put(WebSocketConstants.DEFINITION, WebSocketConstants.JOINED_THE_ROOM);
		jsonResponse.put(WebSocketConstants.STREAM_ID, newStreamId);
		//This field is deprecated. Use STREAM_LIST_IN_ROOM 
		jsonResponse.put(WebSocketConstants.STREAMS_IN_ROOM, jsonStreamIdArray);	
		jsonResponse.put(WebSocketConstants.STREAM_LIST_IN_ROOM, jsonStreamListArray);	
		jsonResponse.put(WebSocketConstants.ATTR_ROOM_NAME, room);	
		jsonResponse.put(WebSocketConstants.ROOM, room);	
		jsonResponse.put(WebSocketConstants.MAX_TRACK_COUNT, appSettings.getMaxVideoTrackCount());	
		
		sendMessage(jsonResponse.toJSONString(), session);
	}


	public static JSONObject getTakeCandidateJSON(long sdpMLineIndex, String sdpMid, String sdp, String streamId, String linkedSessionForSignaling, String subscriberId) {

		JSONObject jsonObject = new JSONObject();
		jsonObject.put(WebSocketConstants.COMMAND,  WebSocketConstants.TAKE_CANDIDATE_COMMAND);
		jsonObject.put(WebSocketConstants.CANDIDATE_LABEL, sdpMLineIndex);
		jsonObject.put(WebSocketConstants.CANDIDATE_ID, sdpMid);
		jsonObject.put(WebSocketConstants.CANDIDATE_SDP, sdp);
		jsonObject.put(WebSocketConstants.STREAM_ID, streamId);
		jsonObject.put(WebSocketConstants.LINK_SESSION, linkedSessionForSignaling);
		jsonObject.put(WebSocketConstants.SUBSCRIBER_ID, subscriberId);

		return jsonObject;
	}

	public static JSONObject getSDPConfigurationJSON(String description, String type, String streamId, Map<String, String> midSidMap, String linkedSessionForSignaling, String subscriberId) {

		JSONObject jsonResponseObject = new JSONObject();
		jsonResponseObject.put(WebSocketConstants.COMMAND, WebSocketConstants.TAKE_CONFIGURATION_COMMAND);
		jsonResponseObject.put(WebSocketConstants.SDP, description);
		jsonResponseObject.put(WebSocketConstants.TYPE, type);
		jsonResponseObject.put(WebSocketConstants.STREAM_ID, streamId);
		jsonResponseObject.put(WebSocketConstants.LINK_SESSION, linkedSessionForSignaling);
		jsonResponseObject.put(WebSocketConstants.SUBSCRIBER_ID, subscriberId);
		
		if(midSidMap != null) {
			JSONObject jsonIdMappingObject = new JSONObject();

			for (Entry<String, String> entry : midSidMap.entrySet()) {
				jsonIdMappingObject.put(entry.getKey(), entry.getValue());
			}
			jsonResponseObject.put(WebSocketConstants.ID_MAPPING, jsonIdMappingObject);
		}


		return jsonResponseObject;
	}

	@SuppressWarnings("unchecked")
	public void sendInvalidStreamNameError(String streamId, Session session)  {
		JSONObject jsonResponse = new JSONObject();
		jsonResponse.put(WebSocketConstants.COMMAND, WebSocketConstants.ERROR_COMMAND);
		jsonResponse.put(WebSocketConstants.DEFINITION, WebSocketConstants.INVALID_STREAM_NAME);
		jsonResponse.put(WebSocketConstants.STREAM_ID, streamId);
		sendMessage(jsonResponse.toJSONString(), session);	
	}

	public ApplicationContext getAppContext() {
		return appContext;
	}

	public void setAppContext(ApplicationContext appContext) {
		this.appContext = appContext;
	}
	
	public void setAppAdaptor(AntMediaApplicationAdapter appAdaptor) {
		this.appAdaptor = appAdaptor;
	}
	
	public void sendRemoteDescriptionSetFailure(Session session, String streamId, String subscriberId) {
		JSONObject jsonObject = new JSONObject();
		jsonObject.put(WebSocketConstants.COMMAND, WebSocketConstants.ERROR_COMMAND);
		jsonObject.put(WebSocketConstants.DEFINITION, WebSocketConstants.NOT_SET_REMOTE_DESCRIPTION);
		jsonObject.put(WebSocketConstants.STREAM_ID, streamId);
		jsonObject.put(WebSocketConstants.SUBSCRIBER_ID, subscriberId);
		sendMessage(jsonObject.toJSONString(), session);
	}
	
	public void sendLocalDescriptionSetFailure(Session session, String streamId, String subscriberId) {
		JSONObject jsonObject = new JSONObject();
		jsonObject.put(WebSocketConstants.COMMAND, WebSocketConstants.ERROR_COMMAND);
		jsonObject.put(WebSocketConstants.DEFINITION, WebSocketConstants.NOT_SET_LOCAL_DESCRIPTION);
		jsonObject.put(WebSocketConstants.STREAM_ID, streamId);
		jsonObject.put(WebSocketConstants.SUBSCRIBER_ID, subscriberId);
		
		sendMessage(jsonObject.toJSONString(), session);
	}
	
	@SuppressWarnings("unchecked")
	public void sendNotFoundJSON(String streamId, Session session) {
		JSONObject jsonResponse = new JSONObject();
		jsonResponse.put(WebSocketConstants.COMMAND, WebSocketConstants.ERROR_COMMAND);
		jsonResponse.put(WebSocketConstants.ERROR_CODE, "404");
		jsonResponse.put(WebSocketConstants.DEFINITION, WebSocketConstants.NO_STREAM_EXIST);
		jsonResponse.put(WebSocketConstants.STREAM_ID, streamId);
		sendMessage(jsonResponse.toJSONString(), session);
	}

	public void sendServerError(String streamId, Session session) {
		JSONObject jsonResponse = new JSONObject();
		jsonResponse.put(WebSocketConstants.COMMAND, WebSocketConstants.ERROR_COMMAND);
		jsonResponse.put(WebSocketConstants.DEFINITION, WebSocketConstants.SERVER_ERROR_CHECK_LOGS);
		jsonResponse.put(WebSocketConstants.STREAM_ID, streamId);
		sendMessage(jsonResponse.toJSONString(), session);
		
	}

	public void setSession(Session session) {
		this.session = session;
		
	}

	public String getUserAgent() {
		return userAgent;
	}

	public void setUserAgent(String userAgent) {
		this.userAgent = userAgent;
	}
}
