package io.antmedia.websocket;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.AppSettings;
import io.antmedia.StreamIdValidator;
import io.antmedia.datastore.db.DataStore;
import io.antmedia.datastore.db.DataStoreFactory;
import io.antmedia.datastore.db.IDataStoreFactory;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.muxer.IAntMediaStreamHandler;
import io.antmedia.webrtc.adaptor.RTMPAdaptor;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;
import org.webrtc.VideoCodecInfo;

import javax.annotation.Nonnull;
import javax.websocket.Session;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

import static io.antmedia.muxer.IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING;

public class WebSocketSignalingHandler extends WebSocketCommunityHandler {

    public static final String WEBRTC_VERTX_BEAN_NAME = "webRTCVertx";

    private static Logger logger = LoggerFactory.getLogger(WebSocketCommunityHandler.class);

    private JSONParser jsonParser = new JSONParser();

    private AppSettings appSettings;

    private static Map<String,Session> availableOrigins = new ConcurrentHashMap<>();

    private DataStore datastore;

    private AntMediaApplicationAdapter appAdaptor;

    protected String userAgent = "N/A";

    public static final String USER_STREAM_ID = "USER_STREAM_ID";

    public static final String USER_ROOM_ID = "USER_ROOM_ID";

    protected static Map<String, Session> streamIDtoServerSessionMap = new Hashtable<>();;

    protected static Map<String, Session> streamIDtoClientSessionMap = new Hashtable<>();;

    protected static Map<String, Session> availableOriginMap = new Hashtable<>();

    public WebSocketSignalingHandler(ApplicationContext appContext, Session session) {
        super(appContext, session);
        appSettings = (AppSettings) getAppContext().getBean(AppSettings.BEAN_NAME);

        //DataStoreFactory dataStoreFactory = (DataStoreFactory) getAppContext().getBean(IDataStoreFactory.BEAN_NAME);
        //setDatastore(dataStoreFactory.getDataStore());
    }

    @Override
    public void onClose(Session session) {
        RTMPAdaptor connectionContext = (RTMPAdaptor) session.getUserProperties().get(session.getId());
        if (connectionContext != null) {
            connectionContext.stop();
        }
    }

    @Override
    public void onError(Session session, Throwable throwable) {
        //not used for now
    }

    @Override
    public void onMessage(Session session, String message) {

        if (message == null) {
            logger.error("Received message null for session id: {}" , session.getId());
            return;
        }

        //TODO: SEND DATASTORE NOT AVAILABLE IN CASE

        try {
            logger.debug("Received message: {} session id: {}" , message, session.getId());

            JSONObject jsonObject = (JSONObject) jsonParser.parse(message);

            String cmd = (String) jsonObject.get(WebSocketConstants.COMMAND);

            if (cmd == null) {
                logger.error("Received message does not contain any command for session id: {}, message:{}" , session.getId(), message);
                return;
            }

            final String streamId = (String) jsonObject.get(WebSocketConstants.STREAM_ID);

            if ((streamId == null || streamId.isEmpty()) &&
                    !cmd.equals(WebSocketConstants.REGISTER_ORIGIN_SERVER) &&
                    !cmd.equals(WebSocketConstants.LEAVE_THE_ROOM) &&
                    !cmd.equals(WebSocketConstants.PING_COMMAND)
            )
            {
                //join room command does not, do nothing
                sendNoStreamIdSpecifiedError(session);
                return;
            }

            if(!StreamIdValidator.isStreamIdValid(streamId)) {
                sendInvalidStreamNameError(streamId, session);
                return;
            }
            if (cmd.equals(WebSocketConstants.PUBLISH_COMMAND)) {

                //TODO: SEND LICENSE SUSPENDED, SEND HIGH RESOURCE USAGE

                //TODO: GET TOKENID, SUBSCRIBERID, SUBSCRIBER CODE, STREAMNAME, MAINTRACK, MetaData from json
                //NOT SURE IF I NEED THIS
                String tokenId = null;
                String subscriberId = null;
                String subscriberCode = null;
                String streamName = null;
                String mainTrack = null;
                String metaData = null;

                if(jsonObject.containsKey(WebSocketConstants.TOKEN)) {
                    tokenId = (String) jsonObject.get(WebSocketConstants.TOKEN);
                    logger.info("received token:{}",tokenId );
                }

                if(jsonObject.containsKey(WebSocketConstants.SUBSCRIBER_ID)) {
                    subscriberId = (String) jsonObject.get(WebSocketConstants.SUBSCRIBER_ID);
                    logger.info("received subscriber id :{}",subscriberId );
                }

                if(jsonObject.containsKey(WebSocketConstants.SUBSCRIBER_CODE)) {
                    subscriberCode = (String) jsonObject.get(WebSocketConstants.SUBSCRIBER_CODE);
                    logger.info("received subscriber code :{}",subscriberCode );
                }

                if(jsonObject.containsKey(WebSocketConstants.STREAM_NAME) &&  !((String) jsonObject.get(WebSocketConstants.STREAM_NAME)).isEmpty()) {
                    streamName = (String) jsonObject.get(WebSocketConstants.STREAM_NAME);
                    logger.info("received stream name:{}",streamName );
                }

                if(jsonObject.containsKey(WebSocketConstants.MAIN_TRACK) &&  !((String) jsonObject.get(WebSocketConstants.MAIN_TRACK)).isEmpty()) {
                    mainTrack = (String) jsonObject.get(WebSocketConstants.MAIN_TRACK);
                    logger.info("received mainTrack:{}",mainTrack );
                }

                if(jsonObject.containsKey(WebSocketConstants.META_DATA) &&  !((String) jsonObject.get(WebSocketConstants.META_DATA)).isEmpty()) {
                    metaData = (String) jsonObject.get(WebSocketConstants.META_DATA);
                    logger.info("received metaData:{}",metaData);
                }

                //default value is true
                boolean enableVideo = jsonObject.containsKey(WebSocketConstants.VIDEO) ? (boolean) jsonObject.get(WebSocketConstants.VIDEO) : true;
                //default value is true
                boolean enableAudio = jsonObject.containsKey(WebSocketConstants.AUDIO) ? (boolean) jsonObject.get(WebSocketConstants.AUDIO) : true;

                logger.info("1111111111111111111111111111111");

                processPublishCommand(streamId, enableVideo, enableAudio, tokenId, subscriberId, subscriberCode, streamName, mainTrack, metaData);
            }
            else if (cmd.equals(WebSocketConstants.TAKE_CONFIGURATION_COMMAND))
            {
                processTakeConfigurationCommand(jsonObject, session.getId(), streamId);
            }
            else if (cmd.equals(WebSocketConstants.TAKE_CANDIDATE_COMMAND))
            {
                logger.info("RECEIVED 0000000000000000000000000000000000000000000000000000p");
                processTakeCandidateCommand(jsonObject, session.getId(), streamId);
            }
            else if (cmd.equals(WebSocketConstants.REGISTER_ORIGIN_SERVER)){
                //TODO:
                registerOriginServerToMap(session);
            }
        }catch (ParseException e) {
            logger.info("Received message: {} session id: {}" , message, session.getId());
            logger.error(ExceptionUtils.getStackTrace(e));

        }
    }

    public void registerOriginServerToMap(Session session){
        String originId = String.valueOf(availableOriginMap.size() + 1);
        logger.info("***************** = " + session.getBasicRemote()+ " - " + originId);
        availableOriginMap.put(originId, session);
        logger.info("" + availableOriginMap.get(originId));
        //getDatastore().saveOriginForSignaling(session.getRequestURI());
    }

    //v1.0 will only publish regardless of tokens etc.
    public void processPublishCommand(String streamId, boolean enableVideo, boolean enableAudio, String tokenId, String subscriberId, String subscriberCodeText, String streamName, String mainTrack, String metaData)
    {
        //String roomId = (String)session.getUserProperties().get(USER_ROOM_ID);

        //TODO: SEND ALREADY PUBLISHING, SEND UNAUTHORIZED

        //TODO: SEND STREAM ID IN USE IF IT IS, POSSIBLY CREATE A DATABASE FOR KEEPING WHICH STREAM ID IS WHERE

        //TODO: CONTROL STREAM TIME

        //TODO: CHECK IF STREAM IS ALLOWED sendNoAllowUnRegisteredStream()

        //TODO: ZOMBI STREAMS?

        //TODO: IS IT POSSIBLE TO DO ALL OF THIS BY JUST PASSING THE INCOMING MESSAGE TO THE CLIENT?

        //logger.debug("Enable audio {}  enable video {} for stream: {}, to session:{}", enableAudio, enableVideo, streamId, session.getId());
        logger.info(":++++++++++++++++++++++++++++++++");
        session.getUserProperties().put(WebSocketConstants.ATTR_STREAM_NAME, streamId);
        logger.info(":DDDDDDDDDDDDDDDDDDDDDDDDDDDD");


        //TODO: REPLACE CANDIDATES WITH SERVER ADDRESS PARAMETER, DISCUSS THIS
        //encoderAdaptor.setReplaceCandidateAddressWithServerAddress(appSettings.isReplaceCandidateAddrWithServerAddr());
        //encoderAdaptor.setServerAddress(serverSettings.getServerName());

        //applicationAdaptor.getPublisherAdaptorList().put(session.getId(), encoderAdaptor);

        sendPublishMessageToOrigin(streamId, enableVideo, enableAudio, tokenId, subscriberId, subscriberCodeText, streamName, mainTrack, metaData);
        //TODO: AFTER START MESSAGE IS SENT, IT NEEDS TO TAKE CONFIGURATION FROM TARGET HOST WHICH WILL ESTABLISH P2P


    }

    private void processTakeCandidateCommand(JSONObject jsonObject, String sessionId, String streamId) {
        String sdpMid = (String) jsonObject.get(WebSocketConstants.CANDIDATE_ID);
        String sdp = (String) jsonObject.get(WebSocketConstants.CANDIDATE_SDP);
        long sdpMLineIndex = (long)jsonObject.get(WebSocketConstants.CANDIDATE_LABEL);

        IceCandidate iceCandidate = new IceCandidate(sdpMid, (int)sdpMLineIndex, sdp);

        Session targetSession;
        if(streamIDtoClientSessionMap.get(streamId).getId().equals(session.getId())){
            targetSession = streamIDtoServerSessionMap.get(streamId);
        }
        else{
            targetSession = streamIDtoClientSessionMap.get(streamId);
        }

        sendTakeCandidateMessage(sdpMLineIndex, sdpMid, sdp, streamId, targetSession);

    }

    public void processTakeConfigurationCommand(JSONObject jsonObject, String sessionId, String streamId) {
        String typeString = (String)jsonObject.get(WebSocketConstants.TYPE);
        String sdpDescription = (String)jsonObject.get(WebSocketConstants.SDP);

        //TODO: MAKE IT DYNAMIC, DATABASEDE TUT
        String hostDestination = "ovh36.antmedia.io";

        SessionDescription.Type type;
        if (typeString.equals("offer")) {
            type = SessionDescription.Type.OFFER;
        }
        else {
            type = SessionDescription.Type.ANSWER;
        }

        if (type == SessionDescription.Type.OFFER)
        {
            //if it's offer, it means this is publishing
            //When it is offer, we already have the publisher SDP, so we will require server's SDP,
            //Therefore send take configuration to the target server and send what comes as the answer
            logger.info("received type: {} sdp: {}", type, sdpDescription);

            // webrtc publish
            SessionDescription sdp = new SessionDescription(type, sdpDescription);
            Session session = getOriginSession(streamId);
            logger.info("44444444444444444444444444444444 = " + session.getRequestURI());
            takeConfigurationFromTargetHost(sdpDescription, type.toString(), streamId, session);

        }

        //TODO: IMPLEMENT PLAY SCENARIO, TYPE WILL BE ANSWER
        /*else
        {
            //if it's answer it means this webrtc client

            //if it is a viewer as well,
            //user may publish/play stream at the same time
            Map<String, Queue<WebRTCClient>> webRTCClientsMap = applicationAdaptor.getWebRTCClientsMap();
            Queue<WebRTCClient> webRTCClientList = webRTCClientsMap.get(sessionId);
            if (webRTCClientList != null)
            {
                //webrtc play
                for (WebRTCClient webRTCClient : webRTCClientList) {
                    if (webRTCClient.getStreamId().equals(streamId)) {
                        SessionDescription sdp = new SessionDescription(type, sdpDescription);
                        webRTCClient.setRemoteDescription(sdp);
                        break;
                    }
                }
            }
        }*/
    }

    //TODO: Make it dynamic about the origin ID
    public Session getOriginSession(String streamId){
        return availableOriginMap.get("1");
    }
    public void takeConfigurationFromTargetHost(String sdpDescription, String typeString, String streamId, Session session){
        sendSDPConfiguration(sdpDescription, typeString, streamId, session, null);
    }

    public boolean processSignallingTakeConf(String streamId, @Nonnull List<Session> webSocketSessions, String typeString, String sdpDescription)  {
        //if it is in signalling mode,
        boolean result = false;
        if (webSocketSessions.size() == 2)
        {
            for (Session sessionTmp : webSocketSessions) {
                if (!sessionTmp.getId().equals(this.session.getId())) {
                    sendSDPConfiguration(sdpDescription, typeString, streamId, sessionTmp, null);
                    result = true;
                }
            }
        }
        else {
            JSONObject jsonResponse = new JSONObject();
            jsonResponse.put(WebSocketConstants.COMMAND, WebSocketConstants.ERROR_COMMAND);
            jsonResponse.put(WebSocketConstants.DEFINITION, WebSocketConstants.NO_PEER_ASSOCIATED);
            sendMessage(jsonResponse.toJSONString(), this.session);
        }
        return result;

    }

    //TODO: MAKE IT DYNAMIC ABOUT ORIGIN ID
    public void sendPublishMessageToOrigin(String streamId, boolean enableVideo, boolean enableAudio, String tokenId, String subscriberId, String subscriberCodeText, String streamName, String mainTrack, String metaData){
        logger.info("333333333333333333");
        Session originSession = getOriginSession(streamId);
        streamIDtoServerSessionMap.put(streamId, originSession);
        streamIDtoClientSessionMap.put(streamId, session);
        logger.info("333333333333333333");

        JSONObject jsonResponse = new JSONObject();
        jsonResponse.put(WebSocketConstants.COMMAND, WebSocketConstants.PUBLISH_COMMAND);
        jsonResponse.put(WebSocketConstants.STREAM_ID, streamId);
        jsonResponse.put(WebSocketConstants.SUBSCRIBER_ID, subscriberId);
        jsonResponse.put(WebSocketConstants.SUBSCRIBER_CODE, subscriberCodeText);
        jsonResponse.put(WebSocketConstants.STREAM_NAME, streamName);
        jsonResponse.put(WebSocketConstants.VIDEO, enableVideo);
        jsonResponse.put(WebSocketConstants.AUDIO, enableAudio);
        jsonResponse.put(WebSocketConstants.META_DATA, metaData);
        Session sessionTmp = getOriginSession(streamId);
        sendMessage(jsonResponse.toJSONString(), sessionTmp);

        sendStartMessage(streamId, session);
    }

    /**
     * It will return the configuration (SDP) from the target server which peer needs to connect.
     *
    public SessionDescription takeConfigurationFromTargetHost(SessionDescription offerSdp, String destinationHost){

    }*/

    public  void sendStartMessage(String streamId, Session session)
    {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(WebSocketConstants.COMMAND, WebSocketConstants.START_COMMAND);
        jsonObject.put(WebSocketConstants.STREAM_ID, streamId);
        logger.info("**********");
        sendMessage(jsonObject.toJSONString(), session);
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

    public void sendTakeCandidateMessage(long sdpMLineIndex, String sdpMid, String sdp, String streamId, Session session)
    {
        sendMessage(getTakeCandidateJSON(sdpMLineIndex, sdpMid, sdp, streamId).toJSONString(), session);
    }

    private void setRemoteDescription(RTMPAdaptor connectionContext, String typeString, String sdpDescription, String streamId) {
        if (connectionContext != null) {
            SessionDescription.Type type;
            if ("offer".equals(typeString)) {
                type = SessionDescription.Type.OFFER;
                logger.info("received sdp type is offer {}", streamId);
            }
            else {
                type = SessionDescription.Type.ANSWER;
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
    public  void sendSDPConfiguration(String description, String type, String streamId, Session session, Map<String, String> midSidMap) {

        sendMessage(getSDPConfigurationJSON (description, type,  streamId, midSidMap).toJSONString(), session);
    }

    public static JSONObject getTakeCandidateJSON(long sdpMLineIndex, String sdpMid, String sdp, String streamId) {

        JSONObject jsonObject = new JSONObject();
        jsonObject.put(WebSocketConstants.COMMAND,  WebSocketConstants.TAKE_CANDIDATE_COMMAND);
        jsonObject.put(WebSocketConstants.CANDIDATE_LABEL, sdpMLineIndex);
        jsonObject.put(WebSocketConstants.CANDIDATE_ID, sdpMid);
        jsonObject.put(WebSocketConstants.CANDIDATE_SDP, sdp);
        jsonObject.put(WebSocketConstants.STREAM_ID, streamId);

        return jsonObject;
    }

    public static JSONObject getSDPConfigurationJSON(String description, String type, String streamId, Map<String, String> midSidMap) {

        JSONObject jsonResponseObject = new JSONObject();
        jsonResponseObject.put(WebSocketConstants.COMMAND, WebSocketConstants.TAKE_CONFIGURATION_COMMAND);
        jsonResponseObject.put(WebSocketConstants.SDP, description);
        jsonResponseObject.put(WebSocketConstants.TYPE, type);
        jsonResponseObject.put(WebSocketConstants.STREAM_ID, streamId);

        if(midSidMap != null) {
            JSONObject jsonIdMappingObject = new JSONObject();

            for (Map.Entry<String, String> entry : midSidMap.entrySet()) {
                jsonIdMappingObject.put(entry.getKey(), entry.getValue());
            }
            jsonResponseObject.put(WebSocketConstants.ID_MAPPING, jsonIdMappingObject);
        }


        return jsonResponseObject;
    }

    public void sendMessage(String message, final Session session) {
        logger.info("SENDING = " + message + " - " + session.isOpen());
        synchronized (this) {
            if (session.isOpen()) {
                logger.info("SENDING = " + message);
                try {
                    session.getBasicRemote().sendText(message);
                } catch (Exception e) {
                    //capture all exceptions because some unexpected events may happen it causes some internal errors
                    logger.error(ExceptionUtils.getStackTrace(e));
                }
            }
        }
    }

    public void setAppAdaptor(AntMediaApplicationAdapter appAdaptor) {
        this.appAdaptor = appAdaptor;
    }

    public void sendRemoteDescriptionSetFailure(Session session, String streamId) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(WebSocketConstants.COMMAND, WebSocketConstants.ERROR_COMMAND);
        jsonObject.put(WebSocketConstants.DEFINITION, WebSocketConstants.NOT_SET_REMOTE_DESCRIPTION);
        jsonObject.put(WebSocketConstants.STREAM_ID, streamId);
        sendMessage(jsonObject.toJSONString(), session);
    }

    public void sendInvalidStreamNameError(String streamId, Session session)  {
        JSONObject jsonResponse = new JSONObject();
        jsonResponse.put(WebSocketConstants.COMMAND, WebSocketConstants.ERROR_COMMAND);
        jsonResponse.put(WebSocketConstants.DEFINITION, WebSocketConstants.INVALID_STREAM_NAME);
        jsonResponse.put(WebSocketConstants.STREAM_ID, streamId);
        sendMessage(jsonResponse.toJSONString(), session);
    }

    public void sendLocalDescriptionSetFailure(Session session, String streamId) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(WebSocketConstants.COMMAND, WebSocketConstants.ERROR_COMMAND);
        jsonObject.put(WebSocketConstants.DEFINITION, WebSocketConstants.NOT_SET_LOCAL_DESCRIPTION);
        jsonObject.put(WebSocketConstants.STREAM_ID, streamId);
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

    public DataStore getDatastore() {
        return datastore;
    }

    public void setDatastore(DataStore datastore) {
        this.datastore = datastore;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

}